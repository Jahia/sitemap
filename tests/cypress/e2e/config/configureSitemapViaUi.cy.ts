import { SitemapPage } from '../../page-object/sitemap.page'

import { waitUntilRefresh } from '../../utils/waitUntilRefresh'
import { removeSitemapConfiguration } from '../../utils/removeSitemapConfiguration'
import { switchToBrowsingApolloClient, switchToProcessingApolloClient } from '../../utils/apollo'
import { waitUntilSyncIsComplete } from '../../utils/sync'
import { enableModule } from '@jahia/cypress'
import { jahiaProcessingConfig } from '../../utils/serversConfig'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`
const sitemapUrl = `${siteMapRootUrl}/sitemap.xml`
const langEn = 'en'

describe('Testing sitemap configuration via Jahia Admin UI', () => {
    after('Remove sitemap configuration via GraphQL', () => {
        removeSitemapConfiguration(sitePath)
    })

    before('Verify Sitemap is configured properly for digitall', () => {
        cy.log('AAA')
        enableModule('sitemap', siteKey, jahiaProcessingConfig)
        cy.log('AAA1')
        // Save the root sitemap URL and Flush sitemap cache
        switchToProcessingApolloClient()
        cy.log('AAA2')
        const siteMapPage = SitemapPage.visit(siteKey, langEn)
        cy.log('AAA3')
        siteMapPage.inputSitemapRootURL(siteMapRootUrl)
        siteMapPage.clickOnSave()
        siteMapPage.clickTriggerSitemapJob()
        cy.log('AAA4')

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
            expect(r.id).not.to.be.null
            expect(r.mixinTypes[0].name).to.equal('jseomix:sitemap')
            expect(r.name).to.equal(siteKey)
        })
        waitUntilSyncIsComplete()
        switchToBrowsingApolloClient()
    })

    // By default, digitall should have some URLs
    it('Verify that the sitemap does contain some pages', function () {
        // Wait for the sitemap to be initialized with some urls
        waitUntilRefresh(sitemapUrl, [])

        cy.task('parseSitemap', { url: sitemapUrl }).then((urls: Array<string>) => {
            cy.log(`Sitemap contains: ${urls.length} URLs`)
            expect(urls.length).to.be.greaterThan(20)
        })
    })
})
