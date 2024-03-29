export const waitForSitemap = (): void => {
    cy.waitUntil(
        () =>
            cy
                .apollo({
                    fetchPolicy: 'no-cache',
                    queryFile: 'graphql/getJobsWithStatus.graphql',
                })
                .then((response) => {
                    const jobs = response?.data?.admin?.jahia?.scheduler?.jobs
                    const sitemapJobs = jobs.filter((job) => job.group === 'SitemapCreationJob')
                    const hasActiveJobs = sitemapJobs.some((job) => job.jobStatus === 'EXECUTING')
                    return !hasActiveJobs
                }),
        {
            errorMsg: 'Jobs are still running before the end of timeout',
            timeout: 60000,
            verbose: true,
            interval: 500,
        },
    )
    // Wait 2 second for server sync after publication
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(2000)
}

export const generateSitemap = (siteKey: string): void => {
    cy.log(`Delete sitemap cache for siteKey: ${siteKey}`)
    cy.apollo({
        variables: {
            siteKey: siteKey,
        },
        mutationFile: 'graphql/deleteSitemapCache.graphql',
    })
    // Wait for cluster sync
    waitForSitemap()
}
