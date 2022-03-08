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

import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for simple conversions
 */
public class ConversionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConversionUtils.class);

    /**
     * Converting time to milliseconds
     * If the time is null, empty, or length <= 1 will just return default value
     * @param time          [String] Format in "[time][unit]" ex: 4h
     * @param defaultValue
     * @return
     */
    public static long toMilliSecondsLong(String time, long defaultValue) {
        if (time == null || time.isEmpty() || time.length() <= 1) return defaultValue;
        String timeValue = time.substring(0, time.length() - 1);
        String unit = time.substring(time.length() - 1);

        try {
            switch (unit) {
                case "h":
                    return convertFromHour(Long.parseLong(timeValue));
                default:
                    return defaultValue;
            }
        } catch (Exception e) {
            logger.error("Unable to convert timeValue will default to default value.");
            return defaultValue;
        }
    }

    public static long convertFromHour(long hours) {
        return TimeUnit.HOURS.toMillis(hours);
    }
}
