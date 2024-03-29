import { configureSitemap } from '../../utils/configureSitemap'
import { removeSitemapConfiguration } from '../../utils/removeSitemapConfiguration'
import { waitForSitemap } from '../../utils/generateSitemap'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`
const sitemapUrl = `${siteMapRootUrl}/sitemap.xml`

describe('Testing sitemap configuration via GraphQL API', () => {
    after('Remove sitemap configuration via GraphQL', () => {
        removeSitemapConfiguration(sitePath)
    })

    beforeEach(() => {
        waitForSitemap()
    })

    // Before running the other tests, verify Sitemap is configured properly for digitall
    it(`Apply sitemap configuration for site ${sitePath}`, function () {
        configureSitemap(sitePath, siteMapRootUrl)

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
        cy.task('parseSitemap', { url: sitemapUrl }).then((urls: Array<string>) => {
            cy.log(`Sitemap contains: ${urls.length} URLs`)
            expect(urls.length).to.be.greaterThan(20)
        })
    })

    it('Should display debug info in XML when debug enabled', function () {
        ;['true', 'false'].forEach((debug) => {
            cy.apollo({
                variables: {
                    debug,
                },
                mutationFile: 'graphql/enabledDebug.graphql',
            })
            // wait for sync of config
            // eslint-disable-next-line cypress/no-unnecessary-waiting
            cy.wait(1000)
            waitForSitemap()
            cy.request('en/sites/digitall/sitemap-lang.xml').then((response) => {
                if (debug === 'true') {
                    expect(response.body).to.contains('<!-- nodePath:')
                } else {
                    expect(response.body).not.to.contains('<!-- nodePath:')
                }
            })
        })
    })
})
