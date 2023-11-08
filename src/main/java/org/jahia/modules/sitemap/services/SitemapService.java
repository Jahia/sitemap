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
import org.quartz.SchedulerException;

import javax.jcr.RepositoryException;

public interface SitemapService {

    Boolean sendSitemapXMLUrlPathToSearchEngines(String sitemapIndexXMLUrlPath) throws SitemapException;

    /**
     * Generate the sitemap for the given siteKey
     * @param siteKey site key
     */
    void generateSitemap(String siteKey);

    /**
     * Adds sitemap cache entry.
     * @param siteKey (mandatory)
     * @param key (mandatory)
     * @param sitemap (mandatory)
     * @throws RepositoryException
     */
    void addSitemap(String siteKey, String key, String sitemap)  throws RepositoryException;

    /**
     * Gets sitemap entry cache value for a giving sitemap cache key.
     * @param siteKey (mandatory)
     * @param key (mandatory)
     * @return sitemap cache content as String.
     */
    String getSitemap(String siteKey, String key);

    /**
     * Set up sitemap job generation for the given site
     * @param siteKey targeted site
     * @param repeatInterval String representation in hours of time between each job execution in ms.
     * @throws SchedulerException
     */
    void scheduleSitemapJob(String siteKey, String repeatInterval) throws SchedulerException;

    /**
     * remove sitemap job generation for all or one given site
     * @param siteKey siteKey of the site, if null unSchedule all sitemaps generation
     * @return true if a job has been deleted
     */
    boolean deleteSitemapJob(String siteKey);

    /**
     * remove sitemap for one given site
     * @param siteKey siteKey of the site
     */
    void removeSitemap(String siteKey);

}
