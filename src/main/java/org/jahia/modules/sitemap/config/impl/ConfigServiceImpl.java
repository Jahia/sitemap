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
package org.jahia.modules.sitemap.config.impl;

import org.apache.commons.collections4.map.HashedMap;
import org.jahia.modules.sitemap.config.ConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.jahia.modules.sitemap.constant.SitemapConstant.*;

/**
 * A service class that retrieves value from the cfg file
 *
 * @author nonico
 */
@Component(service = ConfigService.class)
public class ConfigServiceImpl implements ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceImpl.class);
    private static final String PROP_FORMAT="%s%s%s";
    private static final String EMPTY_STRING="";
    public static final long MIN_FREQUENCY = 1L;
    private Map<String, String> properties;

    public ConfigServiceImpl() {
        properties = new HashedMap<>();
    }

    @Activate
    public void activate(Map<String, ?> props) {
        properties = initProperties(props);
        logger.info("Sitemap configuration activated");
    }

    @Deactivate
    public void deactivate() {
        logger.info("Sitemap configuration deactivated");
    }

    @Override
    public void setProperties(Map<String,String> properties) {
        this.properties = properties;
    }

    /**
     * Returns an integer. This is the job frequency specified in the cfg file
     *
     * @return int
     */
    @Override
    public long getJobFrequency() {
        final long jobFrequencyHour = Long.parseLong(properties.getOrDefault(String.format(PROP_FORMAT, SITEMAP_PARENT_PROPERTY, DOT,
                JOB_FREQUENCY),String.valueOf(MIN_FREQUENCY)));
        final long jobFrequency = convertFromHour(jobFrequencyHour);
        return Math.max(MIN_FREQUENCY, jobFrequency);
    }

    @Override
    public List<String> getSearchEngines() {
        final String searchEnginesStr = properties.getOrDefault(String.format(PROP_FORMAT, SITEMAP_PARENT_PROPERTY, DOT, SEARCH_ENGINES),
                EMPTY_STRING);
        return new ArrayList<>(Arrays.asList(searchEnginesStr.split(",")));
    }

    @Override
    public List<String> getSitemapUrls() {
        final String siteMapUrlsStr = properties.getOrDefault(String.format(PROP_FORMAT, SITEMAP_PARENT_PROPERTY, DOT, SITEMAP_URLS),
                EMPTY_STRING);
        return new ArrayList<>(Arrays.asList(siteMapUrlsStr.split(",")));
    }

    @Override public List<String> getIncludeContentTypes() {
        final String includedContentTypes = properties.getOrDefault(String.format(PROP_FORMAT, SITEMAP_PARENT_PROPERTY, DOT, INCLUDED_CONTENT_TYPES),
                EMPTY_STRING);
        return new ArrayList<>(Arrays.asList(includedContentTypes.split(",")));
    }

    private long convertFromHour(long jobFrequency) {
        return TimeUnit.HOURS.toMillis(jobFrequency);
    }

    private Map<String, String> initProperties(Map<String,?> props) {
        return props.keySet().stream()
                .filter(propsKey -> !props.get(propsKey).toString().isEmpty())
                .collect(Collectors.toMap(propsKey -> propsKey, propsKey -> props.get(propsKey).toString(), (a, b) -> b));
    }
}
