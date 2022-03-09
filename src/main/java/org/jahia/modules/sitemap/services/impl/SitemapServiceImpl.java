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
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.exceptions.SitemapException;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.modules.sitemap.utils.Utils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import org.jahia.modules.sitemap.config.ConfigService;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

@Component(immediate = true, service = SitemapService.class)
public class SitemapServiceImpl implements SitemapService {

    private static Logger logger = LoggerFactory.getLogger(SitemapServiceImpl.class);

    private static final String ERROR_IO_EXCEPTION_WHEN_SENDING_URL_PATH = "Error IO exception when sending url path";

    private ConfigService configService;

    @Activate
    public void activate(Map<String, Object> props) {
        logger.info("Activator started for sitemap...");
        // TODO could be a good place to init the cache here.
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
    public void flushCache(String siteKey) throws RepositoryException {
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
                            // TODO flush cache for: sitemapNode

                            // get all caches for sitemap resource
                            String sitePath = sitemapNode.getParent().getPath();
                            String query = "SELECT * from [jseont:sitemapResource] WHERE ISDESCENDANTNODE('%s')";
                            QueryResult subResult = Utils.getQuery(session, String.format(query, sitePath));
                            if (subResult == null) continue;

                            for (NodeIterator iter2 = subResult.getNodes(); iter2.hasNext(); ) {
                                JCRNodeWrapper sitemapResourceNode = (JCRNodeWrapper) iter2.nextNode();
                                // Flush the sitemap resource node per the path
                                // TODO flush cache for: sitemapResourceNode
                            }
                        }

                        return null;
                    }
                });
    }
}