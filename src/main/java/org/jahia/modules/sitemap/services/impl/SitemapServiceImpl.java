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
import org.jahia.modules.sitemap.utils.Utils;
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
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.jahia.modules.sitemap.utils.Utils.markSitemapGenerationAsRunning;
import static org.jahia.modules.sitemap.constant.SitemapConstant.SITEMAP_CACHE_DURATION;
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
        JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
            try {
                for (JCRSiteNode siteNode : JahiaSitesService.getInstance().getSitesNodeList(session)) {
                    if (Utils.isSitemapEnabledAndConfigured(siteNode)) {
                        scheduleSitemapJob(siteNode.getSiteKey(), siteNode.getPropertyAsString(SITEMAP_CACHE_DURATION));
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
            return schedulerService.getScheduler().deleteJob(siteKey, JOB_GROUP_NAME);
        } catch (SchedulerException e) {
            logger.error("An error happen while trying to unSchedule job for siteKey {}", siteKey, e);
        }
        return false;
    }

    private void deleteSitemapJobs() {
        if (schedulerService.getScheduler() == null) {
            // Such case happen when Jahia is stopped.
            // At startup, the job is removed then recreated.
            logger.warn("Sitemap jobs cannot be removed because the scheduler is not available anymore.");
            logger.debug("debug info:", new Exception());
            return;
        }
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
            markSitemapGenerationAsRunning(siteKey);
            schedulerService.getScheduler().triggerJob(siteKey, JOB_GROUP_NAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSitemap(String siteKey, String key) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                try(InputStream is = session.getNode("/sites/" + siteKey + "/sitemapSettings/sitemapCache/" + key).getProperty("sitemapFile").getBinary().getStream()) {
                    // String transformation
                    return new BufferedReader(
                            new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines()
                            .map(Utils::decode)
                            .collect(Collectors.joining("\n"));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            try {
                JobDetail jobDetail = schedulerService.getScheduler().getJobDetail(siteKey, JOB_GROUP_NAME);
                if (jobDetail != null) {
                    logger.info("No entry found for site {} and key {}", siteKey, key);
                    // Check if the job is running, in case not, trigger a run
                    if (!Utils.isSitemapGenerationAsRunning(siteKey)) {
                        // trigger the job
                        generateSitemap(siteKey);
                    }
                    logger.info("Sitemap generation in progress");
                } else {
                    logger.error("Sitemap generation job is not running, the sitemap will not be available", e);
                }
            } catch (SchedulerException | RepositoryException ex) {
                logger.error("Error while getting jobDetail for job name {} and group {}", siteKey, JOB_GROUP_NAME, ex);
            }
            return null;
        }
    }

    @Override
    public void addSitemap(String siteKey, String key, Path sitemap) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession( session -> {
            JCRNodeWrapper settingsNode = JCRContentUtils.getOrAddPath(session, session.getNode("/sites/" + siteKey), "sitemapSettings", "jnt:sitemapSettings");
            JCRNodeWrapper cacheRoot = JCRContentUtils.getOrAddPath(session, settingsNode, "sitemapCache", "jnt:sitemapRootCacheEntries");
            JCRNodeWrapper cacheNode = JCRContentUtils.getOrAddPath(session, cacheRoot, key, "jnt:sitemapEntry");
            try(FileInputStream stream = new FileInputStream(sitemap.toFile())) {
                cacheNode.setProperty("sitemapFile", session.getValueFactory().createBinary(stream));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            session.save();
            return null;
        });
    }

    @Override
    public void removeSitemap(String siteKey) {
        // Clean up cache
        String sitemapSettingsPath = "/sites/" + siteKey + "/sitemapSettings";
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                if (session.nodeExists(sitemapSettingsPath)) {
                    session.removeItem(sitemapSettingsPath);
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
