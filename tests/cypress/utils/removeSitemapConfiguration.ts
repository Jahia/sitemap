export const removeSitemapConfiguration = (sitePath: string) => {
    cy.log(`Removing sitemap configuration: Verifying if a configuration is present for ${sitePath}`)
    cy.apollo({
        variables: {
            pathOrId: sitePath,
            mixinsFilter: { filters: [{ fieldName: 'name', value: 'jseomix:sitemap' }] },
            propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration'],
        },
        queryFile: 'graphql/jcrGetSitemapConfig.graphql',
    }).then((response) => {
        const r = response?.data?.jcr?.nodeByPath
        cy.log(JSON.stringify(r))

        if (r.properties.map((p) => p.name).includes('sitemapIndexURL')) {
            cy.log(`Removing sitemap configuration: Property sitemapIndexURL is present for ${sitePath}, removing it`)
            cy.apollo({
                variables: {
                    pathOrId: sitePath,
                    propertyName: 'sitemapIndexURL',
                },
                mutationFile: 'graphql/jcrDeleteProperty.graphql',
            })
        } else {
            cy.log(
                `Removing sitemap configuration: Property sitemapIndexURL was not present for ${sitePath}, doing nothing`,
            )
        }

        if (r.properties.map((p) => p.name).includes('sitemapCacheDuration')) {
            cy.log(
                `Removing sitemap configuration: Property sitemapCacheDuration is present for ${sitePath}, removing it`,
            )
            cy.apollo({
                variables: {
                    pathOrId: sitePath,
                    propertyName: 'sitemapCacheDuration',
                },
                mutationFile: 'graphql/jcrDeleteProperty.graphql',
            })
        } else {
            cy.log(
                `Removing sitemap configuration: Property sitemapCacheDuration was not present for ${sitePath}, doing nothing`,
            )
        }

        if (r.mixinTypes.map((m) => m.name).includes('jseomix:sitemap')) {
            cy.log(`Removing sitemap configuration: Mixin jseomix:sitemap is present for ${sitePath}, removing it`)
            cy.apollo({
                variables: {
                    pathOrId: sitePath,
                    mixins: ['jseomix:sitemap'],
                },
                mutationFile: 'graphql/jcrDeleteSitemapMixin.graphql',
            })
        } else {
            cy.log(
                `Removing sitemap configuration: Property sitemapCacheDuration was not present for ${sitePath}, doing nothing`,
            )
        }
    })

    cy.log(`Removing sitemap configuration: Configuration removed for ${sitePath}`)
    cy.apollo({
        variables: {
            pathOrId: sitePath,
            mixinsFilter: { filters: [{ fieldName: 'name', value: 'jseomix:sitemap' }] },
            propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration'],
        },
        queryFile: 'graphql/jcrGetSitemapConfig.graphql',
    }).should((response) => {
        const r = response?.data?.jcr?.nodeByPath
        cy.log(JSON.stringify(r))
        expect(r.mixinTypes.filter((m) => m.name === 'jseomix:sitemap')).to.be.empty
        expect(r.properties.filter((p) => p.name === 'sitemapIndexURL')).to.be.empty
        expect(r.properties.filter((p) => p.name === 'sitemapCacheDuration')).to.be.empty
    })
}
