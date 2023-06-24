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
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.sitemap.config.SitemapConfigService;
import org.jahia.modules.sitemap.exceptions.SitemapException;
import org.jahia.modules.sitemap.job.SitemapCreationJob;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static org.quartz.SimpleTrigger.REPEAT_INDEFINITELY;

@Component(immediate = true, service = SitemapService.class)
public class SitemapServiceImpl implements SitemapService {

    private static final Logger logger = LoggerFactory.getLogger(SitemapServiceImpl.class);
    private static final String ERROR_IO_EXCEPTION_WHEN_SENDING_URL_PATH = "Error IO exception when sending url path";
    private static final String SITEMAP_CACHE_NAME = "sitemapCache";
    private static final long SITEMAP_DEFAULT_CACHE_DURATION_IN_SECONDS = 14400;
    private static final String JOB_GROUP_NAME = BackgroundJob.getGroupName(SitemapCreationJob.class);
    private static final JahiaUser ROOT_USER = JahiaUserManagerService.getInstance().lookupRootUser().getJahiaUser();

    private SitemapConfigService configService;
    private SchedulerService schedulerService;

    @Activate
    public void activate() throws RepositoryException {
        logger.info("Sitemap service started (debug {})", configService != null && configService.isDebug() ? "ENABLED" : "DISABLED");
        JCRSessionFactory.getInstance().setCurrentUser(ROOT_USER);
        // Set up job to trigger cache
        JCRTemplate.getInstance().doExecuteWithSystemSession( session -> {
            try {
                for (JCRSiteNode siteNode : JahiaSitesService.getInstance().getSitesNodeList(session)) {
                    if (siteNode.getInstalledModules().contains("sitemap")) {
                        scheduleSitemapJob(siteNode.getSiteKey(), siteNode.getPropertyAsString("sitemapCacheDuration"));
                    }
                }
            } catch (Exception e) {
                logger.error("Unable to read site nodes to start sitemap generation jobs", e);
            }
            return null;
        });
    }

    @Reference(service = SitemapConfigService.class)
    public void setConfigService(SitemapConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void scheduleSitemapJob(String siteKey, String repeatInterval) throws SchedulerException {
        deleteSitemapJob(siteKey);
        final JobDetail sitemapJob = BackgroundJob.createJahiaJob("sitemap", SitemapCreationJob.class);
        sitemapJob.getJobDataMap().put("debug", configService.isDebug());
        sitemapJob.setName(siteKey);
        Trigger trigger = new SimpleTrigger(siteKey, "     sitemapTrigger", REPEAT_INDEFINITELY, getSitemapCacheExpirationInSeconds(repeatInterval) * 1000);
        schedulerService.getScheduler().scheduleJob(sitemapJob, trigger);
    }

    @Override
    public boolean deleteSitemapJob(String siteKey) {
        try {
            boolean isdeleted = schedulerService.getScheduler().deleteJob(siteKey, JOB_GROUP_NAME);

            removeSitemap(siteKey);
            return isdeleted;
        } catch (SchedulerException e) {
            logger.error("An error happen while trying to unSchedule job for siteKey {}", siteKey, e);
        }
        return false;
    }

    private void deleteSitemapJobs() {
        try {
            for (String jobName : schedulerService.getScheduler().getJobNames(JOB_GROUP_NAME)) {
                deleteSitemapJob(jobName);
            }
        } catch (SchedulerException e) {
            logger.error("An error happen while trying to unSchedule jobs", e);
        }

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
    public void generateSitemap(String siteKey) {
        try {
            schedulerService.getScheduler().triggerJob(siteKey, JOB_GROUP_NAME);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSitemap(String key) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession( session -> session.getNode("/settings/sitemapSettings/sitemapCache/" + key).getProperty("sitemap").getString());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void addSitemap(String siteKey, String key, String sitemap) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession( session -> {
            JCRNodeWrapper settingsNode = JCRContentUtils.getOrAddPath(session, session.getNode("/settings"), "sitemapSettings", "jnt:sitemapSettings");
            JCRNodeWrapper cacheRoot = JCRContentUtils.getOrAddPath(session, settingsNode, "sitemapCache", "jnt:sitemapRootCacheEntries");
            JCRNodeWrapper cacheNode = JCRContentUtils.getOrAddPath(session, cacheRoot, key, "jnt:sitemapEntry");
            cacheNode.setProperty("sitemap", sitemap);
            cacheNode.setProperty("siteKey", siteKey);
            session.save();
            return null;
        });
    }

    private void removeSitemaps() {
        // Clean up cache
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession( session -> {
                if (session.nodeExists("/settings/sitemapSettings/sitemapCache")) {
                    session.getNode("/settings/sitemapSettings/sitemapCache").remove();
                    session.save();
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeSitemap(String siteKey) {
        // Clean up cache
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession( session -> {
                if (session.nodeExists("/settings/sitemapSettings/sitemapCache")) {
                    for (JCRNodeWrapper sitemapCacheNode : session.getNode("/settings/sitemapSettings/sitemapCache").getNodes()) {
                        if (sitemapCacheNode.hasProperty("siteKey") && StringUtils.equals(siteKey, sitemapCacheNode.getPropertyAsString("siteKey"))) {
                            sitemapCacheNode.remove();
                        }
                    }
                    session.save();
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Deactivate
    public void deactivate() {
        // Clean all jobs
        try {
            deleteSitemapJobs();
            removeSitemaps();
        } catch (Exception e) {
            logger.error("An error happen while trying to unSchedule jobs or remove cached sitemaps", e);
        }
    }

    /**
     * Retrieves cache expiration time based on JCR sitemapCacheDuration property
     *
     * @param sitemapCacheDurationPropertyValue (JCR property string value)
     * @return int expiration date in seconds (default value 144000 seconds = 4h).
     */
    private long getSitemapCacheExpirationInSeconds(String sitemapCacheDurationPropertyValue) {
        if (StringUtils.isNotEmpty(sitemapCacheDurationPropertyValue)) {
            // to retro compatibility with older version of sitemap
            if (sitemapCacheDurationPropertyValue.endsWith("h")) {
                sitemapCacheDurationPropertyValue = sitemapCacheDurationPropertyValue.replace("h", "");
            }
            // property value is in hours we need seconds for the cache expiration
            return Long.parseLong(sitemapCacheDurationPropertyValue) * 3600; // in seconds
        }

        return SITEMAP_DEFAULT_CACHE_DURATION_IN_SECONDS;
    }

    @Reference
    public void setSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }
}
