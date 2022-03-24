export const deleteSitemapCache = (siteKey: string) => {
    cy.log(`Delete sitemap cache for siteKey: ${siteKey}`)
    cy.apollo({
        variables: {
            siteKey: siteKey,
        },
        mutationFile: 'graphql/deleteSitemapCache.graphql',
    })
}
