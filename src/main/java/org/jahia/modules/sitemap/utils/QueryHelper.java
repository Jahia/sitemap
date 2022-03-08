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
import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.JahiaService;
import org.jahia.services.content.*;
import org.jahia.services.render.RenderContext;
import org.jahia.services.usermanager.JahiaUser;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility helper class for Sitemap
 */
public final class QueryHelper {

    private static final String DEDICATED_SITEMAP_MIXIN = "jseomix:sitemapResource";

    private QueryHelper() {}

    /** Check if node is a descendant of the list of parent excluded nodes */
    public static boolean excludeNode(JCRNodeWrapper node, NodeIterator excludeNodesIter) {
        String nodePath = node.getPath();
        while (excludeNodesIter.hasNext()) {
            String path = ((JCRNodeWrapper) excludeNodesIter.nextNode()).getPath();
            if (nodePath.equals(path) && nodePath.startsWith(path + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return sitemap entries that are publicly accessible
     */
    public static Set<String> getSitemapEntries(String rootPath, String nodeType, Locale locale) throws RepositoryException {
        String query = String.format("SELECT * FROM [%s] as sel WHERE ISDESCENDANTNODE(sel, '%s')", nodeType, rootPath);
        final Set<String> result = new HashSet<>();
        List<String> excludedPath = new ArrayList<>();
        JahiaUser guestUser = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser(Constants.GUEST_USERNAME).getJahiaUser();
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(guestUser, Constants.LIVE_WORKSPACE, locale, session -> {
            QueryResult queryResult = getQuery(session, query);
            for (NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
                JCRNodeWrapper node = (JCRNodeWrapper) iter.nextNode();
                if (node.isNodeType(DEDICATED_SITEMAP_MIXIN) && !node.getPath().equals(rootPath)) {
                    excludedPath.add(node.getPath());
                }
                result.add(node.getPath());
            }
            return null;
        });
        // Filter out excluded path
        Set<String> collect = result.stream().filter(nodePath -> !excludedPath.stream().anyMatch(path -> nodePath.startsWith(path + "/") || nodePath.equals(path))).collect(Collectors.toSet());
        return collect;
    }

    public static QueryResult getQuery(JCRSessionWrapper session, String query) throws RepositoryException {
        return session.getWorkspace().getQueryManager()
                .createQuery(query, Query.JCR_SQL2).execute();
    }

}
