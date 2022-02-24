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

import net.htmlparser.jericho.Source;
import org.jahia.modules.sitemap.exceptions.SitemapException;
import org.jahia.modules.sitemap.utils.CacheUtils;
import org.jahia.modules.sitemap.utils.ConversionUtils;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import org.jahia.modules.sitemap.config.ConfigService;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

@Component(immediate = true, service = SitemapService.class)
public class SitemapServiceImpl implements SitemapService {

    private static Logger logger = LoggerFactory.getLogger(SitemapServiceImpl.class);
    private static String CHAR_ENCODING="ISO-8859-1";

    private static final String ERROR_IO_EXCEPTION_WHEN_SENDING_URL_PATH = "Error IO exception when sending url path";

    private ConfigService configService;

    @Activate
    public void activate(Map<String, Object> props) {
        logger.info("Activator started for sitemap...");

        // Flushing the sitemap caches on activate
        flushSitemapCaches();
        logger.info("Activator completed for sitemap...");
    }

    @Reference(service = ConfigService.class)
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Boolean sendSitemapXMLUrlPathToSearchEngines(String sitemapUrlPath) throws SitemapException {
        final List<String> searchEngines = configService.getSearchEngines();
        if (searchEngines.isEmpty()) {
            logger.warn("There are not entries found in the configuration: sitemap.search-engines");
            return false;
        }
        if (!sitemapUrlPath.isEmpty() && !searchEngines.isEmpty()) {
            for (String s : searchEngines) {
                try {
                    URL url = new URL(s + sitemapUrlPath);
                    logger.debug("Calling {}", url.toExternalForm());
                    URLConnection urlConnection = url.openConnection();
                    Source source = new Source(urlConnection);
                    logger.debug(source.getTextExtractor().toString());
                    logger.info(source.getTextExtractor().toString());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    throw new SitemapException(ERROR_IO_EXCEPTION_WHEN_SENDING_URL_PATH, e);
                }
            }
        }
        return true;
    }

    private void flushSitemapCaches() {
        JahiaSitesService jahiaSitesService = JahiaSitesService.getInstance();
        List<JCRSiteNode> siteList = null;
        try {
            siteList = jahiaSitesService.getSitesNodeList();

            for (int i = 0; i < siteList.size(); i++) {
                try {
                    String siteKey = siteList.get(i).getSiteKey();
                    logger.info("Site " + siteKey + " flush in progress...");
                    Long expirationTimeDifference = -1L;
                    // We are refreshing just the sitemap cache
                    // TODO expiration unit is wrong hours instead of ms, anyway it's stupid to pass expiration when we want to flush the cache
                    CacheUtils.refreshSitemapCache(ConversionUtils.longVal(expirationTimeDifference, ConversionUtils.convertFromHour(4L)),
                            siteKey);
                } catch (Exception e) {
                    // If breaks for one site will skip for now
                }
            }
        } catch (RepositoryException e) {
            // Skip if we cannot get the list of site nodes
        }
    }
}