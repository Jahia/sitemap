import { SitemapPage } from '../../page-object/sitemap.page'
const siteKey = 'digitall'
const sitePath = '/sites/' + siteKey
const homePagePath = sitePath + '/home'
const testPageName = 'page1'
const testPagePath = homePagePath + '/' + testPageName
const sitemapLangFilePath = sitePath + '/sitemap-lang.xml'
const langDe = 'de'
const langEn = 'en'
const langFr = 'fr'
const langAll = 'de,en,fr'
const siteMapRootUrl = Cypress.config().baseUrl + sitePath

describe('Check sitemap-lang.xml file on MySite', () => {
    beforeEach('Create content for test', () => {
        // create a test page
        cy.apollo({
            variables: {
                parentPathOrId: homePagePath,
                name: testPageName,
                template: 'simple',
                language: langEn,
            },
            mutationFile: 'graphql/jcrAddPage.graphql',
        })

        // update test page title in all wanted languages
        cy.apollo({
            variables: {
                pathOrId: testPagePath,
                properties: [
                    { name: 'jcr:title', value: 'Page1' + langDe, language: langDe },
                    { name: 'jcr:title', value: 'Page1' + langEn, language: langEn },
                    { name: 'jcr:title', value: 'Page1' + langFr, language: langFr },
                ],
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })

        // publish test page in all wanted languages
        cy.apollo({
            variables: {
                pathOrId: testPagePath,
                language: langAll,
                publishSubNodes: false,
                includeSubTree: false,
            },
            mutationFile: 'graphql/jcrPublishNode.graphql',
        })

        // Publish the site in all wanted languages
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                language: langAll,
                publishSubNodes: true,
                includeSubTree: true,
            },
            mutationFile: 'graphql/jcrPublishNode.graphql',
        })

        // Save the root sitemap URL and Flush sitemap cache
        const siteMapPage = SitemapPage.visit(siteKey, langEn)
        siteMapPage.inputSitemapRootURL(siteMapRootUrl)
        siteMapPage.clickOnSave()
        siteMapPage.clickFlushCache()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500)
    })

    afterEach('Cleanup test data', () => {
        cy.apollo({
            variables: {
                pathOrId: testPagePath,
            },
            mutationFile: 'graphql/jcrDeleteNode.graphql',
        })

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
})
