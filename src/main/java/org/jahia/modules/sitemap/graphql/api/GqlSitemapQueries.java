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
package org.jahia.modules.sitemap.graphql.api;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.RepositoryException;
import java.util.Locale;

public class GqlSitemapQueries {

    @GraphQLField
    @GraphQLDescription("Get site URL with only server domain appended from sitemap URL")
    public String getSiteUrl(
            @GraphQLName("siteKey") @GraphQLDescription("Sitemap index XML or sitemap XML URL") @GraphQLNonNull String siteKey) {

        try {
            Locale defaultLang = getDefaultLanguage(siteKey);
            JCRSessionWrapper session = getLiveSession(defaultLang);
            String siteUrl = session.getNode("/sites/" + siteKey).getUrl();
            return siteUrl.replace(".html", "");
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private JCRSessionWrapper getLiveSession() throws RepositoryException {
        return getLiveSession(null);
    }

    private JCRSessionWrapper getLiveSession(Locale locale) throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(Constants.LIVE_WORKSPACE, locale);
    }

    private Locale getDefaultLanguage(String siteKey) throws RepositoryException {
        JCRSessionWrapper session = getLiveSession();
        JCRSiteNode site = (JCRSiteNode) session.getNode("/sites/" + siteKey);

        String langCode = site.getDefaultLanguage();
        return LanguageCodeConverters.getLocaleFromCode(langCode);
    }

}
