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
package org.jahia.modules.sitemap.job;

import net.htmlparser.jericho.Source;
import org.jahia.modules.sitemap.config.ConfigService;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static org.jahia.modules.sitemap.constant.SitemapConstant.SEARCH_ENGINES;
import static org.jahia.modules.sitemap.constant.SitemapConstant.SITEMAP_URLS;

/**
 * Short description of the class
 *
 * @author nonico
 */
@Component(immediate = true)
public class SitemapBackgroundJob extends BackgroundJob {

    private static Logger logger = LoggerFactory.getLogger(SitemapBackgroundJob.class);
    private static String CHAR_ENCODING="UTF-8";
    private SchedulerService schedulerService;
    private JobDetail jobDetail;
    private ConfigService configService;

    @Activate public void start() throws Exception {
        final List<String> searchEngines = configService.getSearchEngines();
        final List<String> siteMapUrls = configService.getSitemapUrls();
        final JobDataMap jobDataMap = new JobDataMap();
        jobDetail = BackgroundJob.createJahiaJob("Simple background job made declared with OSGi", SitemapBackgroundJob.class);
        jobDataMap.put(SEARCH_ENGINES, new ArrayList<>(searchEngines));
        logger.debug("Search engines: {}", searchEngines);
        jobDataMap.put(SITEMAP_URLS, new ArrayList<>(siteMapUrls));
        logger.debug("Sitemap urls: {}", siteMapUrls);
        if (schedulerService.getAllJobs(jobDetail.getGroup()).isEmpty() && SettingsBean.getInstance().isProcessingServer()) {
            Trigger trigger = new SimpleTrigger("sitemapJob_trigger", jobDetail.getGroup(), SimpleTrigger.REPEAT_INDEFINITELY,
                    configService.getJobFrequency());
            jobDetail.setJobDataMap(jobDataMap);
            schedulerService.getScheduler().scheduleJob(jobDetail, trigger);
        }
    }

    @Deactivate public void stop() throws Exception {
        if (!schedulerService.getAllJobs(jobDetail.getGroup()).isEmpty() && SettingsBean.getInstance().isProcessingServer()) {
            schedulerService.getScheduler().deleteJob(jobDetail.getName(), jobDetail.getGroup());
        }
    }

    @Override public void executeJahiaJob(JobExecutionContext jobExecutionContext) {
        final JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        final List<String> searchEngines = (List<String>) jobDataMap.get(SEARCH_ENGINES);
        final List<String> siteMaps = (List<String>) jobDataMap.get(SITEMAP_URLS);
        logger.debug("Search engines: {}", searchEngines);
        logger.debug("Sitemap urls: {}", siteMaps);
        for (String siteMap : siteMaps) {
            for (String s : searchEngines) {
                try {
                    URL url = new URL(s + URLEncoder.encode(siteMap, CHAR_ENCODING));
                    logger.debug("Calling {}", url.toExternalForm());
                    URLConnection urlConnection = url.openConnection();
                    Source source = new Source(urlConnection);
                    logger.debug(source.getTextExtractor().toString());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Reference public void setSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Reference public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

}
