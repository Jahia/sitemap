import { configureSitemap } from '../../utils/configureSitemap'
import { removeSitemapConfiguration } from '../../utils/removeSitemapConfiguration'
import { generateSitemap } from '../../utils/generateSitemap'

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
const languages = ['de', 'en', 'fr']
const siteMapRootUrl = Cypress.config().baseUrl + sitePath

describe('Check sitemap-lang.xml file on digitall', () => {
    before('Configure sitemap for the test', () => {
        console.log(Cypress.config().baseUrl)
        configureSitemap(sitePath, siteMapRootUrl)
    })

    after('Remove sitemap configuration via GraphQL', () => {
        removeSitemapConfiguration(sitePath)
    })

    it('alternate url should not contains all three languages', () => {
        cy.requestFindXMLElementByTagName(langEn + sitemapLangFilePath, 'url').then((urls) => {
            Cypress.$(urls).each(($idx, $list) => {
                const nodeItems = $list.getElementsByTagName('xhtml:link')
                cy.visit($list.getElementsByTagName('loc')[0].textContent)
                cy.get('ul[class~="languages"] li').should('have.length.at.most', nodeItems.length)
            })
        })
    })

    it('alternate url should not contains invalid language', () => {
        // update history page to invalid 'de' language
        cy.apolloProcessing({
            variables: {
                pathOrId: historyPagePath,
                properties: [{ name: 'j:invalidLanguages', values: [langDe], language: langEn }],
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

        // publish history page in all wanted languages
        cy.apolloProcessing({
            variables: {
                pathOrId: historyPagePath,
                languages: languages,
                publishSubNodes: false,
                includeSubTree: false,
            },
            mutationFile: 'graphql/jcrPublishNode.graphql',
        })

        generateSitemap(siteKey)

        cy.requestFindXMLElementByTagName(langFr + sitemapLangFilePath, 'url').then((urls) => {
            Cypress.$(urls).each(($idx, $list) => {
                const pageUrl = $list.getElementsByTagName('loc')
                const siteUrl = pageUrl[0].innerHTML
                if (siteUrl.indexOf('history.html') > 0) {
                    const nodeItems = $list.getElementsByTagName('xhtml:link')
                    if (nodeItems.length > 0) {
                        for (const c of nodeItems) {
                            if (c.getAttribute('hreflang') && c.getAttribute('hreflang') !== 'x-default') {
                                cy.wrap(c.getAttribute('hreflang')).should('not.contain', langDe)
                            }
                        }
                    }
                }
            })
        })

        cy.apolloProcessing({
            variables: {
                pathOrId: historyPagePath,
                properties: [{ name: 'j:invalidLanguages', values: [], language: langEn }],
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

        // publish history page in all wanted languages
        cy.apolloProcessing({
            variables: {
                pathOrId: historyPagePath,
                languages: languages,
                publishSubNodes: false,
                includeSubTree: false,
            },
            mutationFile: 'graphql/jcrPublishNode.graphql',
        })
    })

    it('Check x-default is matching all default (en) locale links', () => {
        cy.requestFindXMLElementByTagName(langEn + sitemapLangFilePath, 'url').then((urls) => {
            Cypress.$(urls).each((idx, url) => {
                // Find all xhtml:link elements in the current URL
                const linkElements = url.getElementsByTagName('xhtml:link')

                // Find the href values for English and x-default
                let englishHref = ''
                let xDefaultHref = ''
                // Extract href values for both locales
                Cypress.$(linkElements).each((idx, link) => {
                    const hreflang = link.getAttribute('hreflang')
                    const href = link.getAttribute('href')
                    if (hreflang === 'en') {
                        englishHref = href
                    } else if (hreflang === 'x-default') {
                        xDefaultHref = href
                    }
                })
                // Verify both href values exist and match
                expect(englishHref).to.not.be.empty
                expect(xDefaultHref).to.not.be.empty
                expect(xDefaultHref).to.equal(englishHref, 'x-default link should match the English link')
            })
        })
    })

    it('Exclude page from sitemap', function () {
        // Clean up any mixin
        cy.apolloProcessing({
            variables: {
                pathOrId: searchResultsPagePath,
                mixinsToRemove: noIndexSitemapMixin,
                workspace: 'LIVE',
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })
        generateSitemap(siteKey)

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
        cy.apolloProcessing({
            variables: {
                pathOrId: searchResultsPagePath,
                mixinsToAdd: noIndexSitemapMixin,
                workspace: 'LIVE',
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

        generateSitemap(siteKey)

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

        cy.apolloProcessing({
            variables: {
                pathOrId: searchResultsPagePath,
                mixinsToRemove: noIndexSitemapMixin,
                workspace: 'LIVE',
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })
    })
})
