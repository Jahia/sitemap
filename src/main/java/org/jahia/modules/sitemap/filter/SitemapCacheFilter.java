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

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.sitemap.constant.SitemapConstant;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * Filter that creates sitemap file nodes for caching.
 *
 * Since it can be resource-intensive to go through and generate all sitemap entries,
 * and since the built-in Jahia view caching isn't guaranteed (i.e. depending on traffic, less-used caches can get invalidated at any time)
 * we've added a custom file-based caching layer to the sitemap.xml views that will be invalidated only after expiration has passed
 * (currently set at 4 hours).
 */
@Component(service = RenderFilter.class, immediate = true)
public class SitemapCacheFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(SitemapCacheFilter.class);

    private static final String SITEMAP_FILTER_TARGET_CACHE_KEY_ATTRIBUTE_KEY = "org.jahia.modules.sitemap.filter.targetSitemapCacheKey";
    private static final String SITEMAP_FILTER_IS_CACHED_ATTRIBUTE_KEY = "org.jahia.modules.sitemap.filter.isCached";

    private SitemapService sitemapService;

    @Reference
    public void setSitemapService(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @Activate
    public void activate() {
        setPriority(15f);
        setApplyOnNodeTypes("jseomix:sitemap,jseomix:sitemapResource");
        setApplyOnTemplates("sitemapLang");
        setApplyOnModes("live");
        setDescription("Filter for sitemap caching");
        logger.debug("Activated SitemapCacheFilter");
    }

    /**
     * Stop render if cache exists and not expired
     */
    @Override
    public String prepare(
            RenderContext renderContext,
            Resource resource,
            RenderChain chain
    ) throws Exception {

        String targetSitemapCacheKey = resource.getNode().getPath() + "#" + resource.getNode().getLanguage();

        renderContext.getRequest().setAttribute(SITEMAP_FILTER_TARGET_CACHE_KEY_ATTRIBUTE_KEY, targetSitemapCacheKey );
        String result = sitemapService.getSitemap(targetSitemapCacheKey);
        if (!StringUtils.isEmpty(result)) {
            renderContext.getRequest().setAttribute(SITEMAP_FILTER_IS_CACHED_ATTRIBUTE_KEY, Boolean.TRUE );
            return result;
        }

        return null;
    }

    /**
     * Create cache, if needed, with rendered contents (previousOut) and expiration time.
     */
    @Override
    public String execute(
            String previousOut,
            RenderContext renderContext,
            Resource resource,
            RenderChain chain
    ) throws RepositoryException {

        String targetSitemapCacheKey = renderContext.getRequest().getAttribute(SITEMAP_FILTER_TARGET_CACHE_KEY_ATTRIBUTE_KEY).toString();

        if (renderContext.getRequest().getAttribute(SITEMAP_FILTER_IS_CACHED_ATTRIBUTE_KEY) == null || !((Boolean) renderContext.getRequest().getAttribute(SITEMAP_FILTER_IS_CACHED_ATTRIBUTE_KEY))) {
            // we retrieve the current sitemap cache duration set by user.0
            String sitemapCacheDuration = resource.getNode().getResolveSite().getPropertyAsString(SitemapConstant.SITEMAP_CACHE_DURATION);
            sitemapService.addSitemap(targetSitemapCacheKey, previousOut, sitemapCacheDuration);
        }

        return previousOut;

    }

}
