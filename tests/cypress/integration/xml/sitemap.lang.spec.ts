import { SitemapPage } from '../../page-object/sitemap.page'
const siteKey = 'digitall'
const sitePath = '/sites/' + siteKey
const homePagePath = sitePath + '/home'
const historyPagePath = '/sites/digitall/home/about/history'
const searchResultsPageName = 'search-results'
const searchResultsPagePath = homePagePath + '/' + searchResultsPageName
const noIndexSitemapMixin = 'jseomix:noIndex'
const sitemapLangFilePath = sitePath + '/sitemap-lang.xml'
const langDe = 'de'
const langEn = 'en'
const langFr = 'fr'
const langAll = 'de,en,fr'
const siteMapRootUrl = Cypress.config().baseUrl + sitePath

describe('Check sitemap-lang.xml file on digitall', () => {

    beforeEach('Create content for test', () => {
        // Save the root sitemap URL and Flush sitemap cache
        const siteMapPage = SitemapPage.visit(siteKey, langEn)
        siteMapPage.inputSitemapRootURL(siteMapRootUrl)
        siteMapPage.clickOnSave()
        siteMapPage.clickFlushCache()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500)
    })

    afterEach('Cleanup test data', () => {
        // deactivate the current sitemap config by removing the mixin
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                mixinsToRemove: ['jseomix:sitemap'],
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })
    })

    it('alternate url should not contains the default language', () => {
        cy.requestFindXMLElementByTagName(langEn + sitemapLangFilePath, 'url').then((urls) => {
            Cypress.$(urls).each(($idx, $list) => {
                const nodeItems = $list.getElementsByTagName('xhtml:link')
                if (nodeItems.length > 0) {
                    for (const c of nodeItems) {
                        cy.wrap(c.getAttribute('hreflang')).should('not.contain', langEn)
                    }
                }
            })
        })
    })

    it('alternate url should not contains invalid language', () => {

        // update history page to invalid 'de' language
        cy.apollo({
            variables: {
                pathOrId: historyPagePath,
                properties: [{ name: 'j:invalidLanguages', values: [langDe], language: langEn }],
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

        // publish history page in all wanted languages
        cy.apollo({
            variables: {
                pathOrId: historyPagePath,
                language: langAll,
                publishSubNodes: false,
                includeSubTree: false,
            },
            mutationFile: 'graphql/jcrPublishNode.graphql',
        })

        const siteMapPage = SitemapPage.visit(siteKey, langEn)
        siteMapPage.clickFlushCache()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500)

        cy.requestFindXMLElementByTagName(langFr + sitemapLangFilePath, 'url').then((urls) => {
            Cypress.$(urls).each(($idx, $list) => {
                const pageUrl = $list.getElementsByTagName('loc')
                const siteUrl = pageUrl[0].innerHTML
                if (siteUrl.indexOf('history.html') > 0) {
                    const nodeItems = $list.getElementsByTagName('xhtml:link')
                    if (nodeItems.length > 0) {
                        for (const c of nodeItems) {
                            if (c.getAttribute('hreflang')) {
                                cy.wrap(c.getAttribute('hreflang')).should('not.contain', langDe)
                            }
                        }
                    }
                }
            })
        })

        cy.apollo({
            variables: {
                pathOrId: historyPagePath,
                properties: [{ name: 'j:invalidLanguages', values: [], language: langEn }]
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

        // publish history page in all wanted languages
        cy.apollo({
            variables: {
                pathOrId: historyPagePath,
                language: langAll,
                publishSubNodes: false,
                includeSubTree: false,
            },
            mutationFile: 'graphql/jcrPublishNode.graphql',
        })

    })

    it('Exclude page from sitemap', function () {

        // check that the page we want to exclude currently exists in the sitemap
        cy.requestFindXMLElementByTagName(langEn + sitemapLangFilePath, 'url').then((urls) => {
            let isTestPassed = false
            Cypress.$(urls).each(($idx, $list) => {
                const pageUrl = $list.getElementsByTagName('loc')
                const siteUrl = pageUrl[0].innerHTML
                if (siteUrl.indexOf(searchResultsPagePath) > 0) {
                    isTestPassed = true
                }
            })
            expect(isTestPassed).to.be.equal(true)
        })

        // add noIndex mixin to the page search-results to exclude from sitemap
        cy.apollo({
            variables: {
                pathOrId: searchResultsPagePath,
                mixinsToAdd: noIndexSitemapMixin,
                workspace: 'LIVE'
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

        // Save the root sitemap URL and Flush sitemap cache
        const siteMapPage = SitemapPage.visit(siteKey, langEn)
        siteMapPage.clickFlushCache()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500)

        // check that the page we want to exclude no longer exist in the sitemap
        cy.requestFindXMLElementByTagName(langEn + sitemapLangFilePath, 'url').then((urls) => {
            let isTestPassed = true
            Cypress.$(urls).each(($idx, $list) => {
                const pageUrl = $list.getElementsByTagName('loc')
                const siteUrl = pageUrl[0].innerHTML
                if (siteUrl.indexOf(searchResultsPagePath) > 0) {
                    isTestPassed = false
                }
            })
            expect(isTestPassed).to.be.equal(true)
        })

        cy.apollo({
            variables: {
                pathOrId: searchResultsPagePath,
                mixinsToRemove: noIndexSitemapMixin,
                workspace: 'LIVE'
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

    })

})
