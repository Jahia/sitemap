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

import javax.jcr.RepositoryException;

public interface SitemapService {

    /**
     * TODO add java doc
     * @param sitemapIndexXMLUrlPath
     * @return
     * @throws SitemapException
     */
    Boolean sendSitemapXMLUrlPathToSearchEngines(String sitemapIndexXMLUrlPath) throws SitemapException;

    /**
     * TODO add java doc
     * @param siteKey
     * @throws RepositoryException
     */
    void flushCache(String siteKey) throws RepositoryException;
}