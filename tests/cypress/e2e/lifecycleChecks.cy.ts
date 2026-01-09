import { configureSitemap } from '../utils/configureSitemap'
import { removeSitemapConfiguration } from '../utils/removeSitemapConfiguration'
import { waitForSitemap } from '../utils/generateSitemap'
import { enableModule, getNodeByPath } from '@jahia/cypress'
import { switchToBrowsingApolloClient, switchToProcessingApolloClient } from '../utils/apollo'
import { jahiaProcessingConfig } from '../utils/serversConfig'
import { waitUntilSyncIsComplete } from '../utils/sync'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`

const stopSitemap = (): void => {
    cy.runProvisioningScript({
        script: [{ value: '- stopBundle: "sitemap"' }],
        jahiaServer: {
            url: Cypress.env('JAHIA_PROCESSING_URL'),
            password: Cypress.env('SUPER_USER_PASSWORD'),
            username: 'root',
        },
    })
    waitUntilSyncIsComplete()
}

const startSitemap = (): void => {
    cy.runProvisioningScript({
        script: [{ value: '- startBundle: "sitemap"' }],
        jahiaServer: {
            url: Cypress.env('JAHIA_PROCESSING_URL'),
            password: Cypress.env('SUPER_USER_PASSWORD'),
            username: 'root',
        },
    })
    waitUntilSyncIsComplete()
}

const disableSitemapFromSite = () => {
    cy.apolloProcessing({
        variables: {
            pathOrId: '/sites/digitall',
            propertyName: 'j:installedModules',
            propertyValue: 'sitemap',
        },
        mutationFile: 'graphql/jcrRemoveValue.graphql',
    })
}

describe('Test behaviour when module state are changing', () => {
    after('Remove sitemap configuration via GraphQL', () => {
        removeSitemapConfiguration(sitePath)
    })

    beforeEach(() => {
        startSitemap()
        configureSitemap(sitePath, siteMapRootUrl)
        waitForSitemap()
    })

    afterEach(() => {
        enableModule('sitemap', siteKey, jahiaProcessingConfig)
    })

    it('Verify that sitemap nodes still existing when stopping the module', () => {
        stopSitemap()

        switchToProcessingApolloClient()
        getNodeByPath(`${sitePath}/sitemapSettings`).then(({ data }) => {
            expect(data.jcr.nodeByPath.uuid).exist
        })
        switchToBrowsingApolloClient()
    })

    it('Verify that sitemap nodes not present anymore when disable sitemap on a site', () => {
        disableSitemapFromSite()

        switchToProcessingApolloClient()
        cy.waitUntil(
            () =>
                getNodeByPath(`${sitePath}/sitemapSettings`).then(({ data }) => {
                    return !data
                }),
            {
                errorMsg: `${sitePath}/sitemapSettings is still existing but should not`,
                timeout: 60000,
                interval: 1000,
            },
        )
        switchToBrowsingApolloClient()
    })

    it('Verify that there is no error with the job when the module is enabled but not fully configured', () => {
        removeSitemapConfiguration(sitePath)
        enableModule('sitemap', siteKey, jahiaProcessingConfig)
        cy.runProvisioningScript({
            script: {
                fileContent: '- enable: "sitemap"\n  site: "' + siteKey + '"',
                type: 'application/yaml',
            },
            jahiaServer: {
                url: Cypress.env('JAHIA_PROCESSING_URL'),
                password: Cypress.env('SUPER_USER_PASSWORD'),
                username: 'root',
            },
        })
        waitUntilSyncIsComplete()
        // force the reactivation of the module by stopping/starting it
        stopSitemap()
        startSitemap()
        waitForSitemap()

        // check the last job execution is successful
        switchToProcessingApolloClient()
        cy.apollo({
            fetchPolicy: 'no-cache',
            queryFile: 'graphql/getJobsWithStatus.graphql',
        }).then((response) => {
            const jobs = response?.data?.admin?.jahia?.scheduler?.jobs
            const sitemapJobs = jobs.filter((job) => job.group === 'SitemapCreationJob')
            return sitemapJobs.every((job) => job.jobState === 'FINISHED')
        })
        switchToBrowsingApolloClient()
    })
})
