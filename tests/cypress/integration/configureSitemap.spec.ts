import { waitUntilRefresh } from '../utils/waitUntilRefresh'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`
const sitemapUrl = `${siteMapRootUrl}/sitemap.xml`

describe('Testing sitemap configuration via GraphQL API', () => {
    before('Configure sitemap via GraphQL', () => {
        // Configure sitemap
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                propertyName: 'sitemapIndexURL',
                propertyValue: siteMapRootUrl,
            },
            mutationFile: 'graphql/jcrAddProperty.graphql',
        })
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                propertyName: 'sitemapCacheDuration',
                propertyValue: '4h',
            },
            mutationFile: 'graphql/jcrAddProperty.graphql',
        })
    })

    // Before running the other tests, verify Sitemap is configured properly for digitall
    it('Verify sitemap is configured properly for site', function () {
        cy.apollo({
            variables: {
                pathOrId: '/sites/digitall',
                mixinsFilter: { filters: [{ fieldName: 'name', value: 'jseomix:sitemap' }] },
                propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration'],
            },
            queryFile: 'graphql/jcrGetSitemapConfig.graphql',
        }).should((response: any) => {
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
