import {waitForSitemap} from './deleteSitemapCache'

export const configureSitemap = (sitePath: string, siteMapRootUrl: string, sitemapHostname: string): void => {
    cy.log(`Configuring sitemap: Verifying if a configuration is present for ${sitePath}`)
    cy.apollo({
        variables: {
            pathOrId: sitePath,
            mixinsFilter: { filters: [{ fieldName: 'name', value: 'jseomix:sitemap' }] },
            propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration', 'sitemapHostname'],
        },
        queryFile: 'graphql/jcrGetSitemapConfig.graphql',
    }).then((response) => {
        const r = response?.data?.jcr?.nodeByPath
        expect(r.id).not.to.be.null

        if (r.mixinTypes?.length > 0) {
            cy.log(`Configuring sitemap: Mixin jseomix:sitemap is already present for ${sitePath}`)
        } else {
            cy.log(`Configuring sitemap: Mixin jseomix:sitemap is not present for ${sitePath}`)
            cy.apollo({
                variables: {
                    pathOrId: sitePath,
                    mixins: ['jseomix:sitemap'],
                },
                mutationFile: 'graphql/jcrAddSitemapMixin.graphql',
            })
        }

        cy.apollo({
            variables: {
                pathOrId: sitePath,
                propertyName: 'sitemapIndexURL',
                propertyValue: siteMapRootUrl,
            },
            mutationFile: 'graphql/jcrAddProperty.graphql',
        })
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                propertyName: 'sitemapHostname',
                propertyValue: sitemapHostname,
            },
            mutationFile: 'graphql/jcrAddProperty.graphql',
        })
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                propertyName: 'sitemapCacheDuration',
                propertyValue: '4',
            },
            mutationFile: 'graphql/jcrAddProperty.graphql',
        })
        // Wait 0.5 seconds for server sync after publication
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(1000)
        waitForSitemap()
    })
}
