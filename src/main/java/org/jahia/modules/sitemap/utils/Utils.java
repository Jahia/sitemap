/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.sitemap.utils;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.beans.SitemapEntry;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.render.RenderContext;
import org.jahia.services.seo.urlrewrite.ServerNameToSiteMapper;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility helper class for Sitemap
 */
public final class Utils {

    private static final String DEDICATED_SITEMAP_MIXIN = "jseomix:sitemapResource";
    private static final String NO_INDEX_MIXIN = "jseomix:noIndex";

    private Utils() {
    }

    public static String encodeSitemapLink(String URIPath, boolean shouldBeDecodedFirst) throws URIException, UnsupportedEncodingException {
        String encodedURIPath = URIPath;
        if (shouldBeDecodedFirst) {
            // example: /cms/render/live/fr/sites/digitall/home/test-parent/test2%3c%c3%bc.html
            // First we decode it, since the node.getUrl(); is encoding the path using JackRabbit Text.escapePath();
            encodedURIPath = URLDecoder.decode(encodedURIPath, "UTF-8");
        }

        // example is now: /cms/render/live/fr/sites/digitall/home/test-parent/test2<Ã¼.html
        // Now we need first to escape XML entities to follow google recommendations -> XML 1.0 is used here
        encodedURIPath = StringEscapeUtils.escapeXml10(encodedURIPath);

        // example is now: /cms/render/live/fr/sites/digitall/home/test-parent/test2&lt;Ã¼.html
        // Now we just need to re encode into UTF-8
        encodedURIPath = URIUtil.encodePath(encodedURIPath, "UTF-8");

        // example is now: /cms/render/live/fr/sites/digitall/home/test-parent/test2&lt;%c3%bc.html
        // we have correctly encoded XML entity: (> into &lt;) and correctly encoded character: (Ã¼ into %c3%bc)
        return encodedURIPath;
    }

    public static Set<String> getSitemapRoots(RenderContext renderContext, String locale) throws RepositoryException {
        Set<String> results = new HashSet<>();
        JahiaUser guestUser = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser(Constants.GUEST_USERNAME).getJahiaUser();
        // Add site node to results
        if (renderContext.getSite().getActiveLiveLanguages().contains(locale)) {
            results.add(renderContext.getSite().getPath());
        }
        JCRTemplate.getInstance().doExecute(guestUser, Constants.LIVE_WORKSPACE, LanguageCodeConverters.languageCodeToLocale(locale), session -> {

            String query = String.format("SELECT * FROM [jseomix:sitemapResource] as sel WHERE ISDESCENDANTNODE(sel, '%s')", renderContext.getSite().getPath());
            QueryResult queryResult = getQuery(session, query);
            NodeIterator ni = queryResult.getNodes();
            while (ni.hasNext()) {
                JCRNodeWrapper n = (JCRNodeWrapper) ni.nextNode();
                if (isValidEntry(n, renderContext)) {
                    results.add(n.getPath());
                }
            }
            return null;
        });
        return results;
    }

    /**
     * @return sitemap entries that are publicly accessible
     */
    public static Set<SitemapEntry> getSitemapEntries(RenderContext renderContext, String rootPath, List<String> nodeTypes, Locale locale) throws RepositoryException {
        final Set<SitemapEntry> result = new LinkedHashSet<>();
        List<String> excludedPath = new ArrayList<>();
        JahiaUser guestUser = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser(Constants.GUEST_USERNAME).getJahiaUser();
        JCRTemplate.getInstance().doExecute(guestUser, Constants.LIVE_WORKSPACE, locale, session -> {
            // add root node into results
            JCRNodeWrapper rootNode = session.getNode(rootPath);
            if (isValidEntry(rootNode, renderContext)) {
                result.add(buildSiteMapEntry(rootNode, locale, guestUser, renderContext));
            }
            // look for sub nodes
            for (String nodeType : nodeTypes) {
                String query = String.format("SELECT * FROM [%s] as sel WHERE ISDESCENDANTNODE(sel, '%s')", nodeType, rootPath);
                QueryResult queryResult = getQuery(session, query);
                for (NodeIterator iter = queryResult.getNodes(); iter.hasNext(); ) {
                    JCRNodeWrapper node = (JCRNodeWrapper) iter.nextNode();
                    if (node.isNodeType(DEDICATED_SITEMAP_MIXIN) && !node.getPath().equals(rootPath)) {
                        excludedPath.add(node.getPath());
                    } else if (isValidEntry(node, renderContext)) {
                        result.add(buildSiteMapEntry(node, locale, guestUser, renderContext));
                    }

                }
            }
            return null;
        });
        // Filter out excluded path
        return result.stream().filter(sitemapEntry -> excludedPath.stream().noneMatch(path -> sitemapEntry.getPath().startsWith(path + "/") || sitemapEntry.getPath().equals(path))).collect(Collectors.toSet());
    }

    private static SitemapEntry buildSiteMapEntry(JCRNodeWrapper node, Locale currentLocale, JahiaUser guestUser, RenderContext renderContext) throws RepositoryException {
        // look for other languages
        List<SitemapEntry> linksInOtherLanguages = new ArrayList<>();
        for (Locale otherLocale : node.getResolveSite().getActiveLiveLanguagesAsLocales()) {
            JCRTemplate.getInstance().doExecute(guestUser, Constants.LIVE_WORKSPACE, otherLocale, sessionInOtherLocale -> {
                if (!sessionInOtherLocale.nodeExists(node.getPath())) {
                    return null;
                }
                JCRNodeWrapper nodeInOtherLocale = sessionInOtherLocale.getNode(node.getPath());
                if (nodeInOtherLocale != null && isValidEntry(nodeInOtherLocale, renderContext)) {
                    linksInOtherLanguages.add(new SitemapEntry(nodeInOtherLocale.getPath(), nodeInOtherLocale.getUrl(), new SimpleDateFormat("yyyy-MM-dd").format(node.getLastModifiedAsDate()), otherLocale, null));
                }
                return null;
            });
        }
        return new SitemapEntry(node.getPath(), node.getUrl(), new SimpleDateFormat("yyyy-MM-dd").format(node.getLastModifiedAsDate()), currentLocale, linksInOtherLanguages);
    }

    private static boolean isValidEntry(JCRNodeWrapper node, RenderContext renderContext) throws RepositoryException {
        // node displayable
        return !node.isNodeType(NO_INDEX_MIXIN) && JCRContentUtils.isADisplayableNode(node, renderContext);
    }

    public static QueryResult getQuery(JCRSessionWrapper session, String query) throws RepositoryException {
        return session.getWorkspace().getQueryManager()
                .createQuery(query, Query.JCR_SQL2).execute();
    }

    public static void addRequestAttributes(ServletRequest request) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        JahiaSitesService jahiaSitesService = ServicesRegistry.getInstance().getJahiaSitesService();
        // resolve site key from hostname
        String siteDefaultLanguage = null;
        String siteKey = null;
        try {
            siteKey = ServerNameToSiteMapper.getSiteKeyByServerName(httpServletRequest);
            if (StringUtils.isEmpty(siteKey)) {
                // If not set, look into the url for any "sites"
                siteKey = StringUtils.substringBetween(httpServletRequest.getRequestURI(), "/sites/", "/");
                // At last, get default site.
                if (StringUtils.isEmpty(siteKey) || jahiaSitesService.getSiteByKey(siteKey) == null) {
                    siteKey = jahiaSitesService.getDefaultSite().getSiteKey();
                }
            }
            // Set language
            siteDefaultLanguage = jahiaSitesService.getSiteDefaultLanguage(siteKey);
        } catch (Exception e) {
            // set language and siteKey if not set
            if (StringUtils.isEmpty(siteDefaultLanguage)) {
                siteDefaultLanguage = SettingsBean.getInstance().getDefaultLanguageCode();
            }
            if (StringUtils.isEmpty(siteKey)) {
                siteKey = jahiaSitesService.getDefaultSite().getSiteKey();
            }
        }
        request.setAttribute("jahiaSitemapSiteKey", siteKey);
        request.setAttribute("jahiaSitemapSiteLanguage", siteDefaultLanguage);
    }

}
