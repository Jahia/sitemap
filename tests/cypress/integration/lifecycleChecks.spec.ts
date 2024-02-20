import { configureSitemap } from '../utils/configureSitemap'
import { removeSitemapConfiguration } from '../utils/removeSitemapConfiguration'
import { waitForSitemap } from '../utils/generateSitemap'
import { enableModule, getNodeByPath } from '@jahia/cypress'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`

const stopSitemap = (): void => {
    cy.runProvisioningScript({
        fileContent: '- stopBundle: "' + 'sitemap' + '"',
        type: 'application/yaml',
    })
}

const startSitemap = (): void => {
    cy.runProvisioningScript({
        fileContent: '- startBundle: "' + 'sitemap' + '"',
        type: 'application/yaml',
    })
}

const disableSitemapFromSite = () => {
    cy.apollo({
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
        enableModule('sitemap', siteKey)
    })

    it('Verify that sitemap nodes still existing when stopping the module', () => {
        stopSitemap()

        getNodeByPath(`${sitePath}/sitemapSettings`).then(({ data }) => {
            expect(data.jcr.nodeByPath.uuid).exist
        })
    })

    it('Verify that sitemap nodes not present anymore when disable sitemap on a site', () => {
        disableSitemapFromSite()

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
    })
})
