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

import net.sf.ehcache.Ehcache;
import org.jahia.modules.sitemap.exceptions.SitemapException;

public interface SitemapService {

    Boolean sendSitemapXMLUrlPathToSearchEngines(String sitemapIndexXMLUrlPath) throws SitemapException;

    /**
     * Gets sitemap ehcache.
     * @return sitemap Ehcache.
     */
    Ehcache getSitemapEhCache();

    /**
     * Flush sitemap Ehcache.
     */
    void flushSitemapEhCache();

    /**
     * Retrieves cache expiration time based on JCR sitemapCacheDuration property
     * @param sitemapCacheDurationPropertyValue (JCR property string value)
     * @return int expiration date in seconds (default value 144000 seconds = 4h).
     */
    int getSitemapCacheExpirationInSeconds(String sitemapCacheDurationPropertyValue);

}
