package org.jahia.modules.sitemap.activators;

import org.jahia.modules.sitemap.utils.CacheUtils;
import org.jahia.modules.sitemap.utils.ConversionUtils;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.service.component.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

/**
 * Activator for the sitemap
 */
@Component(service = SitemapActivator.class, immediate = true)
public class SitemapActivator {

    private static final Logger logger = LoggerFactory.getLogger(SitemapActivator.class);

    @Activate
    public void activate(Map<String, Object> props) {
        logger.info("Activator started for sitemap...");
        JahiaSitesService jahiaSitesService = JahiaSitesService.getInstance();
        List<JCRSiteNode> siteList = null;
        try {
            siteList = jahiaSitesService.getSitesNodeList();

            for (int i = 0; i < siteList.size(); i++) {
                try {
                    String siteKey = siteList.get(i).getSiteKey();
                    logger.info("Site " + siteKey + " in progress...");
                    Long expirationTimeDifference = -1L;
                    // We are refreshing just the sitemap cache
                    CacheUtils.refreshSitemapCache(ConversionUtils.longVal(expirationTimeDifference, ConversionUtils.convertFromHour(4L)),
                            siteKey);
                } catch (Exception e) {
                    // If breaks for one site will skip for now
                }
            }
        } catch (RepositoryException e) {
            // Skip if we cannot get the list of site nodes
        }
        logger.info("Activator completed for sitemap...");
    }
}
