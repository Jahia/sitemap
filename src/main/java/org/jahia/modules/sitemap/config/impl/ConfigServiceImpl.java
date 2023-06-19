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

import org.jahia.modules.sitemap.config.SitemapConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.jahia.modules.sitemap.constant.SitemapConstant.*;

/**
 * A service class that retrieves value from the cfg file
 *
 * @author nonico
 */
@Component(service = SitemapConfigService.class)
public class ConfigServiceImpl implements SitemapConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceImpl.class);
    private static final String PROP_FORMAT="%s%s%s";
    private static final String EMPTY_STRING="";
    private Map<String, String> properties;

    public ConfigServiceImpl() {
        properties = new HashMap<>();
    }

    @Activate
    public void activate(Map<String, ?> props) {
        properties = props.keySet().stream()
                .filter(propsKey -> !props.get(propsKey).toString().isEmpty())
                .collect(Collectors.toMap(propsKey -> propsKey, propsKey -> props.get(propsKey).toString(), (a, b) -> b));
        logger.info("Sitemap configuration activated");
    }

    @Deactivate
    public void deactivate() {
        logger.info("Sitemap configuration deactivated");
    }

    @Override
    public List<String> getSearchEngines() {
        final String searchEnginesStr = properties.getOrDefault(String.format(PROP_FORMAT, SITEMAP_PARENT_PROPERTY, DOT, SEARCH_ENGINES),
                EMPTY_STRING);
        return new ArrayList<>(Arrays.asList(searchEnginesStr.split(",")));
    }

    @Override
    public List<String> getIncludeContentTypes() {
        final String includedContentTypes = properties.getOrDefault(String.format(PROP_FORMAT, SITEMAP_PARENT_PROPERTY, DOT, INCLUDED_CONTENT_TYPES),
                EMPTY_STRING);
        return new ArrayList<>(Arrays.asList(includedContentTypes.split(",")));
    }
}
