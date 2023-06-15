package org.jahia.modules.sitemap.listeners;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCREventIterator;
import org.jahia.services.content.JCRObservationManager;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.service.component.annotations.Reference;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

public class SiteListener extends DefaultEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SiteListener.class);

    private static final String[] NODE_TYPES = {Constants.JAHIANT_VIRTUALSITE};

    private SitemapService sitemapService;

    @Reference
    public void setSitemapService(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @Override
    public int getEventTypes() {
        return Event.PROPERTY_CHANGED + Event.PROPERTY_ADDED + Event.PROPERTY_REMOVED;
    }

    @Override
    public String[] getNodeTypes() {
        return NODE_TYPES;
    }

    @Override
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            try {
                JCRObservationManager.EventWrapper event = (JCRObservationManager.EventWrapper) events.next();
                String eventPath = event.getPath();
                if (eventPath.endsWith("/sitemapHostname") || eventPath.endsWith("/sitemapCacheDuration")) {
                    JCRPropertyWrapper prop = (JCRPropertyWrapper) ((JCREventIterator) events).getSession().getItem(event.getPath());
                    String siteKey = prop.getParent().getName();
                    // do not react on system site (special handling)
                    // do not react if the current node is not a site node
                    if (!siteKey.equals(JahiaSitesService.SYSTEM_SITE_KEY) && StringUtils.isNotEmpty(siteKey) && !StringUtils.contains(siteKey, "/")) {
                        if (prop.getParent().hasProperty("sitemapCacheDuration")) {
                            sitemapService.scheduleSitemapJob(siteKey, prop.getParent().getPropertyAsString("sitemapCacheDuration"));
                        } else {
                            // Clean up jobs
                            sitemapService.deleteSitemapJob(siteKey);
                        }

                    }
                }
            } catch (RepositoryException | SchedulerException e) {
                logger.error("Error while processing JCR event", e);
            }
        }
    }
}
