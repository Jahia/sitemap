package org.jahia.modules.sitemap.listeners;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.services.content.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component(service = DefaultEventListener.class, immediate = true)
public class SitemapSiteListener extends DefaultEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SitemapSiteListener.class);

    private static final String[] NODE_TYPES = {Constants.JAHIANT_VIRTUALSITE};

    private SitemapService sitemapService;

    @Reference
    public void setSitemapService(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @Override
    public int getEventTypes() {
        return Event.NODE_REMOVED + Event.PROPERTY_CHANGED + Event.PROPERTY_ADDED + Event.PROPERTY_REMOVED;
    }

    @Override
    public String[] getNodeTypes() {
        return NODE_TYPES;
    }

    @Override
    public void onEvent(EventIterator events) {
        Map<String, SitemapJobBuilder> jobsToBuild = new HashMap<>();
        while (events.hasNext()) {
            JCRObservationManager.EventWrapper event = (JCRObservationManager.EventWrapper) events.next();
            try {
                String eventPath = event.getPath();
                String siteKey = StringUtils.substringBetween(eventPath, "/sites/", "/");
                // In case the path is the site itself
                if (siteKey == null) {
                    siteKey = StringUtils.substringAfter(eventPath, "/sites/");
                }
                // Create job only if the "sitemapCacheDuration" property is set.
                final JCRSessionWrapper session = ((JCREventIterator) events).getSession();
                if (eventPath.equals("/sites/" + siteKey + "/sitemapCacheDuration") && session.nodeExists(eventPath)) {
                    JCRPropertyWrapper prop = (JCRPropertyWrapper) session.getItem(eventPath);
                    final String sitemapCacheDuration = prop.getParent().getPropertyAsString("sitemapCacheDuration");
                    // add a job in case the sitemap mixin is set and the job to create is new (or set as create already)
                    SitemapJobBuilder jobBuilder = jobsToBuild.get(siteKey);
                    final boolean createJob = jobBuilder == null || jobBuilder.createJob;
                    if (prop.getParent().isNodeType("jseomix:sitemap") && createJob && StringUtils.isNotEmpty(sitemapCacheDuration)) {
                        SitemapJobBuilder builder = jobBuilder == null ? new SitemapJobBuilder() : jobBuilder;
                        builder.createJob = true;
                        builder.cacheDuration = sitemapCacheDuration;
                        jobsToBuild.put(siteKey, builder);
                    }
                }
                // Site node removed
                if (event.getType() == Event.NODE_REMOVED && event.getPath().equals("/sites/" + siteKey)) {
                    SitemapJobBuilder builder = new SitemapJobBuilder();
                    builder.createJob = false;
                    jobsToBuild.put(siteKey, builder);
                }
                // Mixin removed on site node
                if (event.getPath().equals("/sites/" + siteKey + "/" + Constants.JCR_MIXINTYPES)) {
                    JCRPropertyWrapper prop = (JCRPropertyWrapper) session.getItem(eventPath);
                    // if property added without the mixin, it means the mixin was not here (in case of site import) => skip
                    if (event.getType() != Event.PROPERTY_ADDED && Arrays.stream(prop.getValues()).noneMatch(value -> {
                        try {
                            return value.getString().equals("jseomix:sitemap");
                        } catch (RepositoryException e) {
                            throw new RuntimeException(e);
                        }
                    })) {
                        SitemapJobBuilder builder = new SitemapJobBuilder();
                        builder.createJob = false;
                        jobsToBuild.put(siteKey, builder);
                    };
                }
            } catch (RepositoryException e) {
                logger.error("Error while processing JCR event");
            }
        }

        jobsToBuild.forEach((siteKey, config) -> config.build(siteKey, sitemapService));
    }

    private static class SitemapJobBuilder {
        String cacheDuration;
        boolean createJob;

        void build(String siteKey, SitemapService sitemapService) {
            try {
                if (createJob) {
                    logger.info("Sitemap job for site {} has been set", siteKey);
                    sitemapService.scheduleSitemapJob(siteKey, cacheDuration);
                } else {
                    if (sitemapService.deleteSitemapJob(siteKey)) {
                        logger.info("Sitemap job for site {} has been removed", siteKey);
                    }
                }
            } catch (SchedulerException e) {
                logger.error("Unable to set sitemap job for site {}", siteKey);
            }
        }
    }
}
