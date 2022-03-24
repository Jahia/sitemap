import { SitemapPage } from '../../page-object/sitemap.page'
import { waitUntilRefresh } from '../../utils/waitUntilRefresh'
const langEn = 'en'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const homePagePath = `${sitePath}/home`
const testPageName = 'publish_unpublish_test'
const testPagePath = `${homePagePath}/${testPageName}`
const languages = ['en', 'de', 'fr']
const defaultLanguage = 'en'
const sitemapUrl = `${Cypress.config().baseUrl}/sites/digitall/sitemap.xml`

describe('Testing publishing and unpublishing of pages and languages', () => {
    before('Create test data in 3 languages', () => {
        // START - Added this for consistency wtih other tests
        // Save the root sitemap URL and Flush sitemap cache
        const siteMapPage = SitemapPage.visit(siteKey, langEn)
        siteMapPage.inputSitemapRootURL(`${Cypress.config().baseUrl}${sitePath}`)
        siteMapPage.clickOnSave()
        siteMapPage.clickFlushCache()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500)
        // END

        // Creates the test page with content in all 3 languages
        cy.apollo({
            variables: {
                parentPathOrId: homePagePath,
                name: testPageName,
                template: 'simple',
                language: languages.join(),
            },
            mutationFile: 'graphql/jcrAddPagePublishTest.graphql',
        })
    })

    after('Cleanup test data', () => {
        // Remove the page that was created for the test
        cy.apollo({
            variables: {
                pathOrId: testPagePath,
            },
            mutationFile: 'graphql/jcrDeleteNode.graphql',
        })

        // Publish home to remove the pages from the sitemap
        cy.apollo({
            variables: {
                pathOrId: homePagePath,
                languages: languages,
                publishSubNodes: true,
                includeSubTree: true,
            },
            mutationFile: 'graphql/jcrPublishNode.graphql',
        })
    })

    // The test page created in before() has not been published. Therefore there
    // should be no mention of the test page in any of the sitemaps.
    it('Verify that the sitemap does not contain the empty page', function () {
        cy.task('parseSitemap', { url: sitemapUrl }).then((urls: Array<string>) => {
            cy.log(`Sitemap contains: ${urls.length} URLs`)
            cy.log(`Sitemap should not contain any url with path: ${testPageName}`)
            expect(urls.length).to.be.greaterThan(0)
            const pageUrls = urls.filter((u) => u.includes(testPageName))
            expect(pageUrls.length).to.equal(0)
        })
    })

    // This test publish the test page created in before(). It verified that
    // after publication, the page is available in the sitemaps in all three
    // languages.
    it('Publish page in all languages and check sitemap', function () {
        cy.task('parseSitemap', { url: sitemapUrl }).then((originalSitemapUrls: Array<string>) => {
            cy.log(`Starting with a sitemap containing ${originalSitemapUrls.length} URLs`)

            // Publish the page in all languages with subnodes
            cy.log(`Publish the page in all languages with subnodes`)
            cy.apollo({
                variables: {
                    pathOrId: testPagePath,
                    languages: languages,
                    publishSubNodes: true,
                    includeSubTree: true,
                },
                mutationFile: 'graphql/jcrPublishNode.graphql',
            })

            // Wait until the sitemap is modified
            waitUntilRefresh(sitemapUrl, originalSitemapUrls)

            // Fetch the new sitemaps again and test the result
            cy.task('parseSitemap', { url: sitemapUrl }).then((newSitemapUrls: Array<string>) => {
                cy.log(`The updated sitemap contains ${newSitemapUrls.length} URLs`)
                const difference = originalSitemapUrls
                    .filter((u) => !newSitemapUrls.includes(u))
                    .concat(newSitemapUrls.filter((u) => !originalSitemapUrls.includes(u)))

                cy.log(`The following URLs are different: ${JSON.stringify(difference)}`)

                expect(difference.filter((u) => u.includes(testPageName)).length).to.equal(3)
            })
        })
    })

    // Unpublish a lang and make sure only that lang is removed from publication
    // The sitemap should refresh automatically after publication
    for (const lang of languages) {
        it(`Unpublishing page content in language: ${lang} and check sitemap for absence of this link`, function () {
            // Execute the test
            cy.task('parseSitemap', { url: sitemapUrl }).then((originalSitemapUrls: Array<string>) => {
                cy.log(`Starting with a sitemap containing ${originalSitemapUrls.length} URLs`)

                // Before any changes are made, there should be 3 URLs for the page name
                expect(originalSitemapUrls.filter((u) => u.includes(testPageName)).length).to.equal(3)

                // Unpublish the page in that particular language
                cy.log(`Unpublish content in ${lang}`)
                cy.apollo({
                    variables: {
                        pathOrId: testPagePath,
                        languages: [lang],
                    },
                    mutationFile: 'graphql/jcrUnpublishNode.graphql',
                })

                // Wait until the sitemap is modified
                waitUntilRefresh(sitemapUrl, originalSitemapUrls)

                // Fetch the new sitemap again and test the result
                cy.task('parseSitemap', { url: sitemapUrl }).then((newSitemapUrls: Array<string>) => {
                    cy.log(`The updated sitemap contains ${newSitemapUrls.length} URLs`)
                    const removedUrl = originalSitemapUrls.filter((x) => !newSitemapUrls.includes(x))
                    cy.log(`The following URLs have been removed in the updated sitemap: ${JSON.stringify(removedUrl)}`)

                    // In that case, the url does not contain the language in the URL
                    expect(removedUrl.length).to.equal(1)

                    if (lang === defaultLanguage) {
                        // Find the default language URL by substracting the other languages from the original sitemap
                        const otherLanguages = languages.filter((l) => !l.includes(lang))
                        const defaultUrl = originalSitemapUrls
                            .filter((u) => u.includes(testPageName))
                            .find((u) => {
                                for (const l of otherLanguages) {
                                    if (u.includes(`/${l}/`)) {
                                        return false
                                    }
                                }
                                return true
                            })
                        // The single URL that was removed shoudl be the default URL
                        expect(removedUrl[0]).to.equal(defaultUrl)
                    } else {
                        expect(removedUrl.filter((u) => u.includes(`/${lang}${sitePath}`)).length).to.equal(1)
                    }
                })

                // Cleanup after the test
                cy.task('parseSitemap', { url: sitemapUrl }).then((originalSitemapUrls: Array<string>) => {
                    // Publish all pages in all languages to restore the page that was just unpublished
                    cy.log(`Publish the page in all languages with subnodes`)
                    cy.apollo({
                        variables: {
                            pathOrId: testPagePath,
                            languages: languages,
                            publishSubNodes: true,
                            includeSubTree: true,
                        },
                        mutationFile: 'graphql/jcrPublishNode.graphql',
                    })

                    // Wait until the sitemap is modified
                    waitUntilRefresh(sitemapUrl, originalSitemapUrls)
                })
            })
        })
    }

    it('Unpublish the page in all languages and verify its not in the sitemap', function () {
        cy.task('parseSitemap', { url: sitemapUrl }).then((originalSitemapUrls: Array<string>) => {
            cy.log(`Starting with a sitemap containing ${originalSitemapUrls.length} URLs`)

            // Before any changes are made, there should be 3 URLs for the page name
            expect(originalSitemapUrls.filter((u) => u.includes(testPageName)).length).to.equal(3)

            // Unpublish the page in that particular language
            cy.log(`Unpublish content in all languages (${JSON.stringify(languages)}) `)
            cy.apollo({
                variables: {
                    pathOrId: testPagePath,
                    languages: languages,
                },
                mutationFile: 'graphql/jcrUnpublishNode.graphql',
            })

            // Wait until the sitemap is modified
            waitUntilRefresh(sitemapUrl, originalSitemapUrls)

            // Verify the test page is not included in any of the sitemaps
            cy.task('parseSitemap', { url: sitemapUrl }).then((urls: Array<string>) => {
                cy.log(`Sitemap contains: ${urls.length} URLs`)
                expect(urls.length).to.be.greaterThan(0)
                cy.log(`Sitemap should not contain any url with path: ${testPageName}`)
                const pageUrls = urls.filter((u) => u.includes(testPageName))
                expect(pageUrls.length).to.equal(0)
            })
        })
    })
})
