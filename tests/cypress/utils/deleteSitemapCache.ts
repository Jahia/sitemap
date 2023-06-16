export const waitForSitemap = () => {
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
    // Wait 0.5 seconds for server sync after publication
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(1000)
}

export const deleteSitemapCache = (siteKey: string): void => {
    cy.log(`Delete sitemap cache for siteKey: ${siteKey}`)
    cy.apollo({
        variables: {
            siteKey: siteKey,
        },
        mutationFile: 'graphql/deleteSitemapCache.graphql',
    })
    waitForSitemap()
}
