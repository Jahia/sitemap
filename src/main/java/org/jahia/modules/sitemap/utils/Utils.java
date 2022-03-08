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

import org.jahia.api.Constants;
import org.jahia.modules.sitemap.beans.SitemapEntry;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.render.RenderContext;
import org.jahia.services.usermanager.JahiaUser;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility helper class for Sitemap
 */
public final class Utils {

    private static final String DEDICATED_SITEMAP_MIXIN = "jseomix:sitemapResource";

    private Utils() {}

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
            if (otherLocale.equals(currentLocale)) {
                continue;
            }
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

    private static boolean isValidEntry(JCRNodeWrapper node, RenderContext renderContext) {
        // node displayable
        return JCRContentUtils.isADisplayableNode(node, renderContext);
    }

    public static QueryResult getQuery(JCRSessionWrapper session, String query) throws RepositoryException {
        return session.getWorkspace().getQueryManager()
                .createQuery(query, Query.JCR_SQL2).execute();
    }

}
