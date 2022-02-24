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
package org.jahia.modules.sitemap.graphql.api;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;

import javax.inject.Inject;

import javax.jcr.RepositoryException;

import org.jahia.modules.sitemap.exceptions.SitemapException;
import org.jahia.modules.sitemap.services.SitemapService;

import org.jahia.modules.sitemap.utils.CacheUtils;
import org.jahia.modules.sitemap.utils.ConversionUtils;
import org.jahia.services.cache.CacheHelper;

public class GqlSitemapMutation {

    @Inject
    @GraphQLOsgiService
    private SitemapService sitemapService;

    @GraphQLField
    @GraphQLDescription("Sending sitemap(s) based on either sitemap index XML or sitemap XML URL to search engine URL(s) specified in CFG")
    public Boolean sendSitemapToSearchEngine(
            @GraphQLName("sitemapURL") @GraphQLDescription("Sitemap index XML or sitemap XML URL") @GraphQLNonNull String sitemapURL
            ){
        try {
            return sitemapService.sendSitemapXMLUrlPathToSearchEngines(sitemapURL);
        } catch (SitemapException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Delete existing sitemap cache if exists before expiration time difference")
    public Boolean deleteSitemapCache(
            @GraphQLName("expirationTimeDifference") @GraphQLDescription("The expiration time difference in (ms)") @GraphQLNonNull Long expirationTimeDifference,
            @GraphQLName("siteKey") @GraphQLDescription("Site key") String siteKey
    ){
        try {
            // TODO this is completely useless to calculate the expiration here we want to delete all the caches,
            // TODO so why calculating an expiration ?
            // TODO also there is a big misconception here the unity is ms but we pass hours ...
            CacheUtils.refreshSitemapCache(ConversionUtils.longVal(expirationTimeDifference,
                    ConversionUtils.convertFromHour(4L)), siteKey);
            // TODO I dont understand why we need to flush jahia output cache, the sitemap is not rendering pages ?
            CacheUtils.flushJntPages(siteKey); // Flushing specific jnt pages
            return true;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}