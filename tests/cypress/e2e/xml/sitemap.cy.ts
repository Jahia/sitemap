import { configureSitemap } from '../../utils/configureSitemap'
import { removeSitemapConfiguration } from '../../utils/removeSitemapConfiguration'

const siteKey = 'digitall'
const sitePath = '/sites/' + siteKey
const homePagePath = sitePath + '/home'
const searchResultsPageName = 'search-results'
const searchResultsPagePath = homePagePath + '/' + searchResultsPageName
const sitemapRootPath = sitePath + '/sitemap.xml'
const dedicatedSitemapMixin = 'jseomix:sitemapResource'
const siteMapRootUrl = Cypress.env('JAHIA_PROCESSING_URL') + sitePath

describe('Check sitemap.xml root file on digitall', () => {
    beforeEach('Configure sitemap for the test', () => {
        configureSitemap(sitePath, siteMapRootUrl)
    })

    afterEach('Cleanup test data', () => {
        removeSitemapConfiguration(sitePath)

        // remove the previous sitemapResource mixin added during the test to the digital page search-results
        cy.apolloProcessing({
            variables: {
                pathOrId: searchResultsPagePath,
                mixinsToRemove: dedicatedSitemapMixin,
                workspace: 'LIVE',
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })
    })

    it('Generate dedicated sitemap', function () {
        // check that the sitemap root only contains the default sitemap length value
        cy.requestFindNodeInnerHTMLByName(sitemapRootPath, 'loc').then((urls) => {
            expect(urls.length).to.be.equal(3)
        })

        // add sitemapResource mixin to the digital page search-results to add dedicated sitemap
        cy.apolloProcessing({
            variables: {
                pathOrId: searchResultsPagePath,
                mixinsToAdd: dedicatedSitemapMixin,
                workspace: 'LIVE',
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

        // check that the sitemap root now contain 3 more entries (one for each active live language on digitall)
        cy.requestFindNodeInnerHTMLByName(sitemapRootPath, 'loc').then((urls) => {
            // we expect 3 new entries
            expect(urls.length).to.be.equal(6)
            let nodeItems = 0
            Cypress.$(urls).each(($idx, $list) => {
                if ($list.indexOf(searchResultsPagePath) > 0) {
                    nodeItems++
                }
            })
            // we expect that 3 new entries are well related to our dedicated sitemap page
            expect(nodeItems).to.be.equal(3)

            // Check generated sitemaps
            urls.forEach((url) => {
                if (url.indexOf(searchResultsPagePath) > 0) {
                    // Use waitUntil to ensure the dedicated sitemap is fully generated and available
                    cy.waitUntil(
                        () =>
                            cy
                                .request({
                                    url: url,
                                    failOnStatusCode: false,
                                })
                                .then((response) => {
                                    return (
                                        response.status === 200 &&
                                        response.headers['content-type'].includes('text/xml') &&
                                        response.body.includes(
                                            '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"',
                                        ) &&
                                        response.body.includes('<url>')
                                    )
                                }),
                        {
                            timeout: 10000, // Wait up to 10 seconds
                            interval: 500, // Check every 500ms
                            errorMsg: `Sitemap at ${url} was not properly generated within the timeout period`,
                        },
                    ).then(() => {
                        cy.log(`✓ Sitemap at ${url} is properly generated`)

                        // Now validate the structure and content of the sitemap
                        validateSitemapStructure(url)
                    })
                }
            })
        })
    })

    const validateSitemapStructure = (url) => {
        cy.log(`get ${url}`)
        // Use requestFindNodeInnerHTMLByName to get URL elements
        cy.requestFindXMLElementByTagName(url, 'xhtml:link').then((links) => {
            expect(links.length).to.be.greaterThan(0, 'URL should have alternate language links')

            Array.from(links).forEach((link) => {
                expect(link.getAttribute('rel')).to.equal('alternate')
                expect(link.getAttribute('hreflang')).to.be.oneOf(['en', 'fr', 'de', 'x-default'])
                expect(link.getAttribute('href')).to.include('http')
            })
        })
    }
})
