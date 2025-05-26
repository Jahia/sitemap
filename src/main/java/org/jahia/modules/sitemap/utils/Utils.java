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

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.beans.SitemapEntry;
import org.jahia.modules.sitemap.config.SitemapConfigService;
import org.jahia.modules.sitemap.job.HttpServletRequestMock;
import org.jahia.modules.sitemap.job.HttpServletResponseMock;
import org.jahia.osgi.BundleUtils;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.query.ScrollableQuery;
import org.jahia.services.query.ScrollableQueryCallback;
import org.jahia.services.render.RenderContext;
import org.jahia.services.seo.urlrewrite.ServerNameToSiteMapper;
import org.jahia.services.seo.urlrewrite.UrlRewriteService;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.LanguageCodeConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.touk.throwing.ThrowingConsumer;
import pl.touk.throwing.ThrowingPredicate;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility helper class for Sitemap
 */
public final class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private static final String NO_INDEX_MIXIN = "jseomix:noIndex";
    private static final String[] ENTITIES = new String[]{"&amp;", "&apos;", "&quot;", "&gt;", "&lt;"};
    private static final String[] ENCODED_ENTITIES = new String[]{"_-amp-_", "_-apos-_", "_-quot-_", "_-gt-_", "_-lt-_"};

    private static final UrlRewriteService urlRewriteService = BundleUtils.getOsgiService(UrlRewriteService.class, null);

    public static final String DEDICATED_SITEMAP_MIXIN = "jseomix:sitemapResource";
    
    private Utils() {
    }

    /**
     * Try to encode and rewrite the provided path, return the initial path if anything wrong happen
     * @param URIPath path to transform
     * @return the encoded path
     */
    public static String encodeSitemapLink(String URIPath) {
        try {
            final JCRSiteNode siteNode = JahiaSitesService.getInstance().getSiteByKey(StringUtils.substringBetween(URIPath, "/sites/", "/"), JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.LIVE_WORKSPACE, Locale.ENGLISH, Locale.ENGLISH));
            return encodeSitemapLink(URIPath, false, getCustomRenderContext(siteNode), true);
        } catch (Exception e) {
            logger.error("Something wrong happen while encoding the following Path {}", URIPath);
            logger.debug("Detailed message", e);
        }
        return URIPath;
    }

    private static RenderContext getCustomRenderContext(JCRSiteNode siteNode) throws MalformedURLException {
        String hostName = getHostName(siteNode);
        URL serverUrl = new URL(hostName);
        final HttpServletRequestMock request = new HttpServletRequestMock(new HashMap<>(), serverUrl.getHost(), serverUrl.getPath());
        final HttpServletResponseMock response = new HttpServletResponseMock(new StringWriter());
        request.setAttribute("jahiaHostname", hostName);
        RenderContext customRenderContext = new RenderContext(request, response, JCRSessionFactory.getInstance().getCurrentUser());
        customRenderContext.setSite(siteNode);
        return customRenderContext;
    }

    public static String encodeSitemapLink(String URIPath, boolean shouldBeDecodedFirst,RenderContext renderContext,  boolean removeContextPath) throws IOException, ServletException, InvocationTargetException, URISyntaxException {
        String encodedURIPath = urlRewriteService.rewriteOutbound(URIPath, renderContext.getRequest(), renderContext.getResponse());

        if (removeContextPath) {
            encodedURIPath =  RegExUtils.replaceFirst(encodedURIPath, renderContext.getRequest().getContextPath(), "");
        }

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
        encodedURIPath = new URI(null, null, encodedURIPath, null).toASCIIString();;

        // example is now: /cms/render/live/fr/sites/digitall/home/test-parent/test2&lt;%c3%bc.html
        // we have correctly encoded XML entity: (> into &lt;) and correctly encoded character: (Ã¼ into %c3%bc)
        return  encodedURIPath;
    }

    public static Set<String> getSitemapRoots(RenderContext renderContext, String locale) throws RepositoryException {
        Set<String> results = new HashSet<>();
        JahiaUser guestUser = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser(Constants.GUEST_USERNAME).getJahiaUser();
        JCRTemplate.getInstance().doExecute(guestUser, Constants.LIVE_WORKSPACE, LanguageCodeConverters.languageCodeToLocale(locale), session -> {
            // Add site node to results
            results.add(renderContext.getSite().getPath());
            String query = String.format("SELECT * FROM [jseomix:sitemapResource] as sel WHERE ISDESCENDANTNODE(sel, '%s')", renderContext.getSite().getPath());
            QueryResult queryResult = getQuery(session, query);
            NodeIterator ni = queryResult.getNodes();
            while (ni.hasNext()) {
                JCRNodeWrapper n = (JCRNodeWrapper) ni.nextNode();
                results.add(n.getPath());
            }
            return null;
        });
        return results;
    }

    public static String getHostName(JCRSiteNode siteNode) {
        String hostName;
        try {
            String sitemapIndexURL = siteNode.getPropertyAsString("sitemapIndexURL");
            URL serverUrl = new URL(sitemapIndexURL);
            hostName = StringUtils.substringBeforeLast(sitemapIndexURL, serverUrl.getPath());
            return hostName;
        } catch (MalformedURLException e) {
            logger.error("Something wrong happen while retrieving the hostname for the site {}, Sitemap generation won't happen", siteNode.getPath());
            logger.debug("Detailed message", e);
        }
        return "";
    }

    /**
     * generate sitemap entries that are publicly accessible, store them in entriesByLocale and entriesByPath parameters
     */
    public static void generateSitemapEntries(Map<Locale, JCRSessionWrapper> sessionPerLocale, RenderContext renderContext, String rootPath, Set<String> sitemapRoots, Map<Locale, Set<SitemapEntry>> entriesByLocale, Map<String, Set<SitemapEntry>> entriesByPath) throws RepositoryException {
         SitemapConfigService config = BundleUtils.getOsgiService(SitemapConfigService.class, null);
        if (config == null) {
            logger.error("Configuration service SitemapConfigService not revolved, check OSGi services status");
            return;
        }

        JCRSessionWrapper session = sessionPerLocale.get(null);
        // add root node into results
        JCRNodeWrapper rootNode = session.getNode(rootPath);
        if (isValidEntry(rootNode, renderContext)) {
            buildSiteMapEntriesForNode(rootNode, sessionPerLocale, renderContext, entriesByLocale, entriesByPath);
        }
        // Compute other rootPath
        Set<String> filteredRootPath = sitemapRoots.stream().filter(ThrowingPredicate.unchecked(path -> !path.equals(rootPath) && !path.equals(rootNode.getResolveSite().getPath()))).collect(Collectors.toSet());

        // look for sub nodes
        for (String nodeType : config.getIncludeContentTypes()) {
            String queryFrom = String.format("select * FROM [%s] as sel WHERE ISDESCENDANTNODE(sel, '%s')", nodeType, StringUtils.replace(rootPath, "'", "''"));
            new ScrollableQuery(500, session.getWorkspace().getQueryManager()
                    .createQuery(queryFrom, Query.JCR_SQL2)).execute(
                    new ScrollableQueryCallback<ScrollableQuery>() {
                        @Override
                        public boolean scroll() throws RepositoryException {
                            for (NodeIterator iter = stepResult.getNodes(); iter.hasNext(); ) {
                                JCRNodeWrapper node = (JCRNodeWrapper) iter.nextNode();
                                if (node != null && filteredRootPath.stream().noneMatch(root -> node.getPath().startsWith(root + "/") || node.getPath().equals(root)) && isValidEntry(node, renderContext)) {
                                    buildSiteMapEntriesForNode(node, sessionPerLocale, renderContext, entriesByLocale, entriesByPath);
                                }
                            }
                            // Clean cache of all sessions
                            JCRSessionFactory.getInstance().getAllOpenUserSessions().forEach(ThrowingConsumer.unchecked(s -> s.refresh(false)));
                            return true;
                        }

                        @Override
                        protected ScrollableQuery getResult() {
                            return null;
                        }
                    }
            );
        }
    }

    private static void buildSiteMapEntriesForNode(JCRNodeWrapper node, Map<Locale, JCRSessionWrapper> sessionPerLocale, RenderContext renderContext, Map<Locale, Set<SitemapEntry>> entriesByLocale, Map<String, Set<SitemapEntry>> entriesByPath) throws RepositoryException {

        // look for other languages
        Set<SitemapEntry> sitemapEntries = new HashSet<>();
        for (Locale locale : node.getResolveSite().getActiveLiveLanguagesAsLocales()) {
            // The "main" node existing, we use doExecuteWithSystemSessionAsUser to retrieve the I18n nodes to prevent map explosion (cf https://jira.jahia.org/browse/QA-14850 )
            // The "main" node being restrieved by a guest session, we have no security issue when retrieving i18ns nodes with system session
            JCRSessionWrapper localizedSession = sessionPerLocale.get(locale);
            if (!localizedSession.nodeExists(node.getPath())) {
                continue;
            }
            JCRNodeWrapper nodeInOtherLocale = localizedSession.getNode(node.getPath());
            if (nodeInOtherLocale != null && isValidEntry(nodeInOtherLocale, renderContext)) {
                String link = null;
                try {
                    link = renderContext.getRequest().getAttribute("jahiaHostname") + encode(nodeInOtherLocale.getUrl(), renderContext);
                } catch (Throwable e) {
                    logger.warn("Unable to rewrite link for url {}", nodeInOtherLocale.getUrl());
                }
                final SitemapEntry sitemapEntry = new SitemapEntry(nodeInOtherLocale.getPath(), link, new SimpleDateFormat("yyyy-MM-dd").format(node.getLastModifiedAsDate()), locale, nodeInOtherLocale.getPrimaryNodeTypeName(), nodeInOtherLocale.getIdentifier());
                sitemapEntries.add(sitemapEntry);
                final Set<SitemapEntry> entries = entriesByLocale.getOrDefault(locale, new HashSet<>());
                entries.add(sitemapEntry);
                entriesByLocale.put(locale, entries);
            }
        }
        entriesByPath.put(node.getPath(), sitemapEntries);
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

    /**
     * Simple encoding function that encodes xml entities.
     *
     * @param uri to encode
     * @return an encoded uri
     * @throws UnsupportedEncodingException
     */
    public static String encode(String uri, RenderContext renderContext) throws IOException, ServletException, InvocationTargetException, URISyntaxException {
        return org.apache.commons.lang.StringUtils.replaceEach(Utils.encodeSitemapLink(uri, true, renderContext, false), ENTITIES, ENCODED_ENTITIES);
    }


    /**
     * Same as encode, but the way around
     *
     * @param xml to decode
     * @return a decoded xml
     */
    public static String decode(String xml) {
        return org.apache.commons.lang.StringUtils.replaceEach(xml, ENCODED_ENTITIES, ENTITIES);
    }

}
