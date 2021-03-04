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
package org.jahia.modules.sitemap.utils;

import org.jahia.modules.sitemap.config.ConfigService;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import java.util.List;

/**
 * Utility functions for getting information from the configuration
 *
 * @author nonico
 */
public final class ConfigServiceUtils {
    public static List<String> getSearchEngines() {
        return getConfigService().getSearchEngines();
    }

    public static List<String> getIncludedContentTypes() {
        return getConfigService().getIncludeContentTypes();
    }

    public static List<String> getSitemapUrls() {
        return getConfigService().getSitemapUrls();
    }

    public static long getJobFrequency() {
        return getConfigService().getJobFrequency();
    }

    private static ConfigService getConfigService() {
        final BundleContext bundleContext = FrameworkUtil.getBundle(ConfigService.class).getBundleContext();
        final ServiceReference<ConfigService> serviceReference = FrameworkService.getBundleContext()
                .getServiceReference(ConfigService.class);
        return bundleContext.getService(serviceReference);
    }
}
