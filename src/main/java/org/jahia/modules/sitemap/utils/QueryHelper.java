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

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Utility helper class for Sitemap
 */
public final class QueryHelper {

    private QueryHelper() {}

    /** Check if node is a descendant of the list of parent excluded nodes */
    public static boolean excludeNode(JCRNodeWrapper node, NodeIterator excludeNodesIter) {
        String nodePath = node.getPath();
        while (excludeNodesIter.hasNext()) {
            JCRNodeWrapper p = (JCRNodeWrapper) excludeNodesIter.nextNode();
            if (nodePath.startsWith(p.getPath())) return true;
        }
        return false;
    }

    /**
     * @return sitemap entries that are publicly accessible
     */
    public static QueryResult getSitemapEntries(RenderContext ctx, String rootPath, String nodeType) throws RepositoryException {
        String query = "SELECT * FROM [%s] WHERE ISDESCENDANTNODE('%s') and ([createSitemap] IS NULL or [createSitemap]=false)";
        return getQuery(ctx.getSite().getSession(), String.format(query, nodeType, rootPath));
    }

    private static QueryResult getQuery(JCRSessionWrapper session, String query) {
        try {
            return session.getWorkspace().getQueryManager()
                    .createQuery(query, Query.JCR_SQL2).execute();
        } catch (RepositoryException e) {
            return null;
        }
    }

}
