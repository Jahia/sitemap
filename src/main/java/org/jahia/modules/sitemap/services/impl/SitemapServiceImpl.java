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
package org.jahia.modules.sitemap.services.impl;

import net.htmlparser.jericho.Source;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jahia.modules.sitemap.config.ConfigService;
import org.jahia.modules.sitemap.exceptions.SitemapException;
import org.jahia.modules.sitemap.services.SimpleNotificationService;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

@Component(immediate = true, service = SitemapService.class)
public class SitemapServiceImpl implements SitemapService {

    private static final Logger logger = LoggerFactory.getLogger(SitemapServiceImpl.class);
    private static final String ERROR_IO_EXCEPTION_WHEN_SENDING_URL_PATH = "Error IO exception when sending url path";
    private static final String SITEMAP_CACHE_NAME = "sitemapCache";
    private static final int SITEMAP_DEFAULT_CACHE_DURATION_IN_SECONDS = 14400;

    private Ehcache sitemapCache;
    private ConfigService configService;
    private SimpleNotificationService simpleEventService;
    private boolean isClusterActivated;

    @Activate
    public void activate() {
        logger.info("Sitemap service started");
        SettingsBean settingsBean = SettingsBean.getInstance();
        if (settingsBean.isClusterActivated()) {
            simpleEventService = new HazelcastSimpleNotificationServiceImpl(this::flush);
            isClusterActivated = true;
        }
        EhCacheProvider cacheService = (EhCacheProvider) SpringContextSingleton.getBean("ehCacheProvider");
        sitemapCache = cacheService.getCacheManager().addCacheIfAbsent(SITEMAP_CACHE_NAME);
        // flush in case an old version of the cache is present.
        flush();
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
        if (!sitemapUrlPath.isEmpty()) {
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

    @Override
    public void askForFlush() {
        logger.info("a flush of sitemap cache was requested");
        if (isClusterActivated) {
            // In case of cluster notify only, local flush will happen on each node, including the source node of the flush
            simpleEventService.notifyNodes();
            return;
        }
        flush();
    }

    @Override
    public String getSitemap(String key) {
        return sitemapCache.get(key) == null ?  null : sitemapCache.get(key).getObjectValue().toString();
    }

    @Override
    public void addSitemap(String key, String sitemap, String expiration) {
        Element sitemapCacheElement = new Element(key, sitemap);
        sitemapCacheElement.setEternal(false);
        // we get the desired cache expiration time in seconds
        sitemapCacheElement.setTimeToLive(getSitemapCacheExpirationInSeconds(expiration));
        sitemapCache.put(sitemapCacheElement);

    }

    @Deactivate
    public void deactivate() {
        flush();
        if (isClusterActivated) {
            simpleEventService.unregister();
        }
    }

    /**
     * Retrieves cache expiration time based on JCR sitemapCacheDuration property
     * @param sitemapCacheDurationPropertyValue (JCR property string value)
     * @return int expiration date in seconds (default value 144000 seconds = 4h).
     */
    private int getSitemapCacheExpirationInSeconds(String sitemapCacheDurationPropertyValue) {
        if (sitemapCacheDurationPropertyValue != null) {
            // to retro compatibility with older version of sitemap
            if (sitemapCacheDurationPropertyValue.endsWith("h")) {
                sitemapCacheDurationPropertyValue = sitemapCacheDurationPropertyValue.replace("h", "");
            }
            // property value is in hours we need seconds for the cache expiration
            return Integer.parseInt(sitemapCacheDurationPropertyValue) * 3600; // in seconds
        }

        return SITEMAP_DEFAULT_CACHE_DURATION_IN_SECONDS;
    }

    private void flush() {
        logger.info("a flush of sitemap cache was triggered");
        sitemapCache.flush();
    }

}
