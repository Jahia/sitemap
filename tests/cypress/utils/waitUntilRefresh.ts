// Fetches a new sitemap until it gets a list of URLs
// different from the originalSitemapUrls array
export const waitUntilRefresh = (sitemapUrl: string, originalSitemapUrls: Array<string>) => {
    cy.waitUntil(
        () =>
            cy.task('parseSitemap', { url: sitemapUrl }).then((newSitemapUrls: Array<string>) => {
                // cy.log(`Fetched sitemap - Contains ${newSitemapUrls.length} URLs`)
                const difference = originalSitemapUrls
                    .filter((u) => !newSitemapUrls.includes(u))
                    .concat(newSitemapUrls.filter((u) => !originalSitemapUrls.includes(u)))
                if (difference.length === 0) {
                    return false
                }
                return true
            }),
        {
            errorMsg: `Unable to detect a difference in sitemap: ${sitemapUrl} within set timeout`, // overrides the default error message
            timeout: 20000,
            verbose: true,
            interval: 1000, // performs the check every 1 sec, default to 200
        },
    )
}
