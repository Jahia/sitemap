import { SitemapPage } from '../../page-object/sitemap.page'

import { waitUntilRefresh } from '../../utils/waitUntilRefresh'
import { removeSitemapConfiguration } from '../../utils/removeSitemapConfiguration'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`
const sitemapUrl = `${siteMapRootUrl}/sitemap.xml`
const langEn = 'en'

describe('Testing sitemap configuration via Jahia Admin UI', () => {
    after('Remove sitemap configuration via GraphQL', () => {
        removeSitemapConfiguration(sitePath)
    })

    // Before running the other tests, verify Sitemap is configured properly for digitall
    it(`Apply sitemap configuration for site ${sitePath}`, function () {
        // Save the root sitemap URL and Flush sitemap cache
        const siteMapPage = SitemapPage.visit(siteKey, langEn)
        siteMapPage.inputSitemapRootURL(siteMapRootUrl)
        siteMapPage.clickOnSave()
        siteMapPage.clickFlushCache()

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
            expect(r.id).not.to.be.null
            expect(r.mixinTypes[0].name).to.equal('jseomix:sitemap')
            expect(r.name).to.equal(siteKey)
        })
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
