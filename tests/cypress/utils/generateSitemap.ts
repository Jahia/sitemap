import { switchToBrowsingApolloClient, switchToProcessingApolloClient } from './apollo'
import { waitUntilSyncIsComplete } from './sync'

export const waitForSitemap = (): void => {
    cy.log('Wait for sitemap generation')
    switchToProcessingApolloClient()
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
    waitUntilSyncIsComplete()
    switchToBrowsingApolloClient()
    // Wait 2 second for server sync after publication
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(2000)
    cy.log('Sitemap generation completed')
}

export const generateSitemap = (siteKey: string): void => {
    cy.log(`Delete sitemap cache for siteKey: ${siteKey}`)
    cy.apolloProcessing({
        variables: {
            siteKey: siteKey,
        },
        mutationFile: 'graphql/deleteSitemapCache.graphql',
    })
    // Wait for cluster sync
    waitForSitemap()
}
