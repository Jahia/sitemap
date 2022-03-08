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
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.Date;

/** Utility functions for sitemap cache */
public class CacheUtils {

    /** Node name prefix for sitemap file caches */
    public static final String CACHE_NAME = "sitemap-cache";


    public static boolean isExpired(JCRNodeWrapper cacheNode, long expiration) {
        if (expiration < 0) return true; // If expiration is negative then is expired
        Date lastModified = cacheNode.getContentLastModifiedAsDate();
        long expirationInMs = lastModified.getTime() + expiration;
        return System.currentTimeMillis() > expirationInMs;
    }

    /**
     * Delete all sitemap cache nodes
     * @param siteKey
     * @throws RepositoryException
     */
    public static void flushCache(String siteKey) throws RepositoryException {
        // This will be reworked
        String subSite = (siteKey == null || siteKey.isEmpty()) ? "" : ("/" + siteKey);

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null,
                Constants.LIVE_WORKSPACE, null, new JCRCallback<Object>() {
            @Override public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                QueryResult result = Utils.getQuery(session, String.format("SELECT * from [jseont:sitemap] WHERE ISDESCENDANTNODE"
                        + "('/sites%s')", subSite));
                if (result == null) return null;

                for (NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
                    JCRNodeWrapper sitemapNode = (JCRNodeWrapper) iter.nextNode();
                    // Flush the sitemap node per the path
                    CacheHelper.flushOutputCachesForPath(sitemapNode.getPath(), false);
                    refreshExpiredCache(sitemapNode);

                    // get all caches for sitemap resource
                    String sitePath = sitemapNode.getParent().getPath();
                    String query = "SELECT * from [jseont:sitemapResource] WHERE ISDESCENDANTNODE('%s')";
                    QueryResult subResult = Utils.getQuery(session, String.format(query, sitePath));
                    if (subResult == null) continue;

                    for (NodeIterator iter2 = subResult.getNodes(); iter2.hasNext(); ) {
                        JCRNodeWrapper sitemapResourceNode = (JCRNodeWrapper) iter2.nextNode();
                        // Flush the sitemap resource node per the path
                        CacheHelper.flushOutputCachesForPath(sitemapResourceNode.getPath(), false);
                        refreshExpiredCache(sitemapResourceNode);
                    }
                }

                session.save();
                return null;
            }
        });
    }

    /** Delete all expired sitemap cache nodes for a given sitemap node */
    private static void refreshExpiredCache(JCRNodeWrapper sitemapNode) throws RepositoryException {
        // safety check to make sure we're only dealing with sitemap nodes since we're dealing with delete operation
        if (!sitemapNode.isNodeType("jseont:sitemap") && !sitemapNode.isNodeType("jseont:sitemapResource")) return;

        // assumption: only file caches under the sitemap node
        for (NodeIterator iter = sitemapNode.getNodes(); iter.hasNext(); ) {
            JCRNodeWrapper cacheNode = (JCRNodeWrapper) iter.nextNode();
            cacheNode.remove();
        }
    }


}
