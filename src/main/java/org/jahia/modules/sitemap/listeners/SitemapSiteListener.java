package org.jahia.modules.sitemap.listeners;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCREventIterator;
import org.jahia.services.content.JCRObservationManager;
import org.jahia.services.content.JCRPropertyWrapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

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
        String siteKey = null;
        String duration = null;
        Boolean createJob = null;
        while (events.hasNext()) {
            try {
                JCRObservationManager.EventWrapper event = (JCRObservationManager.EventWrapper) events.next();
                String eventPath = event.getPath();
                siteKey = StringUtils.substringBetween(eventPath, "/sites/", "/");
                if (eventPath.endsWith("/sitemapHostname") || eventPath.endsWith("/sitemapCacheDuration")) {
                     createJob = event.getType() != Event.PROPERTY_REMOVED;
                    if (createJob) {
                        JCRPropertyWrapper prop = (JCRPropertyWrapper) ((JCREventIterator) events).getSession().getItem(eventPath);
                        if (prop.getParent().hasProperty("sitemapCacheDuration")) {
                            duration = prop.getParent().getPropertyAsString("sitemapCacheDuration");
                        }
                    }
                } else if (event.getType() == Event.NODE_REMOVED) {
                    siteKey = StringUtils.substringAfter(eventPath, "/sites/");
                    createJob = Boolean.FALSE;
                }
            } catch (RepositoryException e) {
                logger.error("Error while processing JCR event", e);
            }
        }
        if (createJob != null &&  StringUtils.isNotBlank(siteKey)) {
            try {
                if (createJob) {
                    sitemapService.scheduleSitemapJob(siteKey, duration);
                } else {
                    if (sitemapService.deleteSitemapJob(siteKey)) {
                        logger.info("Sitemap job for site {} has been removed", siteKey);
                    };
                }
            } catch (SchedulerException e) {
                logger.error("Unable to set sitemap job for site {}", siteKey);
            }
        }
    }
}
