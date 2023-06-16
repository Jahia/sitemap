import {waitUntilRefresh} from '../../utils/waitUntilRefresh'
import {configureSitemap} from '../../utils/configureSitemap'
import {removeSitemapConfiguration} from '../../utils/removeSitemapConfiguration'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`
const sitemapUrl = `${siteMapRootUrl}/sitemap.xml`

describe('Testing sitemap configuration via GraphQL API', () => {
    after('Remove sitemap configuration via GraphQL', () => {
        removeSitemapConfiguration(sitePath)
    })

    // Before running the other tests, verify Sitemap is configured properly for digitall
    it(`Apply sitemap configuration for site ${sitePath}`, function () {
        configureSitemap(sitePath, siteMapRootUrl, Cypress.config().baseUrl)

        cy.apollo({
            variables: {
                pathOrId: sitePath,
                mixinsFilter: { filters: [{ fieldName: 'name', value: 'jseomix:sitemap' }] },
                propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration', 'sitemapHostname'],
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
