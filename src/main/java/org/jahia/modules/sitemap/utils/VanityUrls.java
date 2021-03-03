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

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.query.QueryResultWrapper;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.VanityUrlManager;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility functions for getting information on Vanity URLs
 */
public final class VanityUrls {

    static VanityUrlManager vanityUrlManager = new VanityUrlManager();

    private VanityUrls() {}


    public static boolean hasActiveUrl(JCRNodeWrapper node) throws RepositoryException {
        return StringUtils.isNotEmpty(getActiveUrl(node));
    }

    public static String getActiveUrl(JCRNodeWrapper node) throws RepositoryException {
        return getActiveUrl(node, node.getSession().getLocale().toString());
    }

    public static String getActiveUrl(JCRNodeWrapper node, String langCode) throws RepositoryException {
        List<VanityUrl> vanityUrls = vanityUrlManager.getVanityUrls(node, langCode, node.getSession());
        return getActiveUrl(vanityUrls);
    }

    private static String getActiveUrl(List<VanityUrl> urls) {
        Optional<VanityUrl> url = urls.stream().filter(v -> v.isActive()).findFirst();
        return (url.isPresent()) ? url.get().getUrl() : null;
    }

}
