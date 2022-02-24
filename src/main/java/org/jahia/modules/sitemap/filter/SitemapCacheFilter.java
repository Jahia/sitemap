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
package org.jahia.modules.sitemap.filter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.utils.CacheUtils;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.content.*;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jahia.modules.sitemap.utils.ConversionUtils;
import org.jahia.modules.sitemap.constant.SitemapConstant;

/**
 * Filter that creates sitemap file nodes for caching.
 *
 * Since it can be resource-intensive to go through and generate all sitemap entries,
 * and since the built-in Jahia view caching isn't guaranteed (i.e. depending on traffic, less-used caches can get invalidated at any time)
 * we've added a custom file-based caching layer to the sitemap.xml views that will be invalidated only after expiration has passed
 * (currently set at 4 hours).
 */
@Component(service = RenderFilter.class)
public class SitemapCacheFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(SitemapCacheFilter.class);

    @Activate
    public void activate() {
        setPriority(15f);
        setApplyOnNodeTypes("jseont:sitemapResource,jseont:sitemap");
        setApplyOnModes("live");
        setDescription("Filter for creating sitemap file nodes for caching");
        logger.debug("Activated SitemapCacheFilter");
    }

    /**
     * Serve cache contents if cache exists and is not expired. Otherwise, flush sitemap cache and re-render
     */
    @Override public String prepare(RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        if (!needsCaching(resource)) return null;

        JCRNodeWrapper cacheNode = getCacheNode(resource.getNode());
        InputStream inputStream = null;
        try {
            if (isValidCache(cacheNode)) {
                inputStream = cacheNode.getFileContent().downloadFile();
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        } catch (IOException e) { // something happened; re-render
            logger.error("Unable to read sitemap file cache contents.");
        } finally {
            // TODO this is deprecate please use something else
            IOUtils.closeQuietly(inputStream);
        }

        // re-render by returning null; manually flush jahia cache prior to render
        // TODO why flushing jahia cache here ?
        CacheHelper.flushOutputCachesForPath(resource.getPath(), false);
        return null;
    }

    /**
     * Create/refresh file cache with rendered contents (previousOut) if needed.
     */
    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain)
            throws RepositoryException
    {
        if (!needsCaching(resource)) return previousOut;

        JCRNodeWrapper sitemapNode = resource.getNode();
        JCRNodeWrapper cacheNode = getCacheNode(sitemapNode);

        InputStream inputStream = null;
        try {
            // refresh if invalid cache; cache only if there's contents
            if (!isValidCache(cacheNode) && StringUtils.isNotBlank(previousOut)) {
                inputStream = IOUtils.toInputStream(previousOut, StandardCharsets.UTF_8);
                refreshSitemapCache(sitemapNode, inputStream);
            }
        } finally {
            // TODO this is deprecate please use something else
            IOUtils.closeQuietly(inputStream);
        }

        return previousOut;
    }

    /**
     * Create or refresh file cache for given parent for a given locale
     * Uses parent session to save
     * https://stackoverflow.com/questions/4685959/get-file-out-of-jcr-file-node
     */
    private JCRNodeWrapper refreshSitemapCache(JCRNodeWrapper sitemapNode, InputStream data) throws RepositoryException {
        String cacheName = getCacheName(sitemapNode);
        String nodePath = sitemapNode.getPath();

        // use system session to be able to save file cache in live workspace
        return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null,
                Constants.LIVE_WORKSPACE, null, new JCRCallback<JCRNodeWrapper>() {
            @Override public JCRNodeWrapper doInJCR(JCRSessionWrapper session) throws RepositoryException {
                // TODO very dangerous to use the JCR to cache a binary file.
                // TODO what would happen if two node cluster receive a request at the same time ?
                // TODO they both will try to generate the sitemap and store in the JCR because of JCR cluster sync not fast enough
                // TODO we should consider caching the file on the file system or in memory using EHCache,
                // TODO also we can avoid cluster issue by having a separate cache on each cluster nodes.
                JCRNodeWrapper node = session.getNode(nodePath);
                node.uploadFile(cacheName, data, "application/xml");
                // TODO performing save operation during render is never a good idea
                session.save();
                return node;
            }
        });
    }


    /** Apply file caching only for default template */
    public boolean needsCaching(Resource resource) {
        String templateName = resource.getTemplate();
        return "lang".equalsIgnoreCase(templateName);
    }

    public boolean isValidCache(JCRNodeWrapper cacheNode) {
        long expiration = 4 * 60 * 60 * 1000L; // Default to 4 hours
        if (cacheNode == null) return false;
        try {
            JCRNodeWrapper siteNode = cacheNode.getResolveSite();
            if (siteNode != null) {
                JCRPropertyWrapper cacheDurationProperty = siteNode.getProperty(SitemapConstant.SITEMAP_CACHE_DURATION);
                String propertyValue = ConversionUtils.getValueFromJCRProperty(cacheDurationProperty);
                expiration = ConversionUtils.toMilliSecondsLong(propertyValue, expiration);
            }
        } catch (RepositoryException e) {
            logger.error("Unable to retrieve node information.");
            return false;
        }
        return !CacheUtils.isExpired(cacheNode, expiration);
    }

    public JCRNodeWrapper getCacheNode(JCRNodeWrapper sitemapNode) throws RepositoryException {
        if (sitemapNode == null) return null;
        String cacheName = getCacheName(sitemapNode);
        return (sitemapNode.hasNode(cacheName)) ? sitemapNode.getNode(cacheName) : null;
    }

    public String getCacheName(JCRNodeWrapper sitemapNode) {
        return CacheUtils.CACHE_NAME + "-" + sitemapNode.getLanguage();
    }

}
