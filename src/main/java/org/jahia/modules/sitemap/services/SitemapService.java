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
package org.jahia.modules.sitemap.services;

import org.jahia.modules.sitemap.exceptions.SitemapException;

public interface SitemapService {

    Boolean sendSitemapXMLUrlPathToSearchEngines(String sitemapIndexXMLUrlPath) throws SitemapException;

    /**
     * Flush sitemap Ehcache.
     */
    void flushSitemapEhCache();

    /**
     * Adds sitemap cache entry
     * @param targetSitemapCacheKey (mandatory)
     * @param sitemap (mandatory)
     * @param sitemapCacheDuration (mandatory)
     */
    void addSitemapEhCacheEntry(String targetSitemapCacheKey, String sitemap, String sitemapCacheDuration);

    /**
     * Checks if an entry cache exist for a giving sitemap cache key.
     * @param targetSitemapCacheKey (mandatory)
     * @return true if a sitemap cache key exist.
     */
    boolean isSitemapEhCacheEntryExist(String targetSitemapCacheKey);

    /**
     * Gets sitemap entry cache value for a giving sitemap cache key.
     * @param targetSitemapCacheKey (mandatory)
     * @return sitemap cache content as String.
     */
    String getSitemapEhCacheEntryValue(String targetSitemapCacheKey);

}
