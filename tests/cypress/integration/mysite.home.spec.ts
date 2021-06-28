import { siteHomePage } from '../page-object/site.home.page'
import { webProjectSettings } from '../page-object/webProjectSettings.page'
import { manageModules } from '../page-object/manageModules.page'
import { sitemapPage } from '../page-object/sitemap.page'
import { getUrlInfoWithHost } from '../support/utilites'

describe('Enable sitemap on MySite', () => {
    beforeEach(() => {
        webProjectSettings.goTo().deleteProject('My Site')
        // Create the site and enable the module
        webProjectSettings.goTo().createProject('My Site', 'mySite')
        manageModules.goTo().selectModule('Jahia Sitemap', ['mySite'])
    })

    it('gets success message when sitemap is enabled', function () {
        const siteUrlHome = '/jahia/page-composer/default/en/sites/mySite/home.html'
        siteHomePage.goTo(siteUrlHome).editPage('My Site').clickOnSitemap().clickOnSave().validateSucessMessage()
    })

    it('Create sitemap for a site', function () {
        // Testcase C3218086 "Create sitemap for a site"
        const siteUrlHome = '/jahia/page-composer/default/en/sites/mySite/home.html'
        // Loading the fixtures
        cy.fixture('C3218086/sitemap.index.urlpath.json').then((json) => {
            this.fixture = json // Recommended from Cypress as no real good way to get promise variable out (use this variable)
        })

        // Step #1 the standard enable
        siteHomePage
            .goTo(siteUrlHome)
            .editPage('My Site')
            .clickOnSitemap()
            .clickOnSave()
            .validateSucessMessage()
            .clickBack()
        // Step #2 publishing the site and flush the cache
        siteHomePage.publishSite('My Site').clickPublishAll().flushCache()

        // Save the root URL
        sitemapPage.goTo().inputSitemapRootURL(Cypress.config().baseUrl).clickOnSave().clickFlushCache()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500)

        // Getting the response from sitemap.index.xml
        // according to the sitemap.xml spec,
        // the url value should reside in a <loc /> node
        // https://www.google.com/sitemaps/protocol.html
        cy.requestFindNodeInnerHTMLByName('sites/mySite/sitemap.xml', 'loc').then((urls) => {
            urls.forEach((url) => {
                const regexArray = getUrlInfoWithHost(String(url)) // Typescript issue that need to explicitly convert to String
                if (regexArray != null && regexArray.length > 4) {
                    const path = regexArray[5]
                    expect(path).to.equal(this.fixture.siteMapPath)
                }

                cy.requestFindXMLElementByTagName(String(url), 'url').then((urlGroups) => {
                    expect(urlGroups.length).to.be.greaterThan(0)
                    Cypress.$(urlGroups).each(($idx, $list) => {
                        const pageUrl = $list.getElementsByTagName('loc')
                        const lastMod = $list.getElementsByTagName('lastmod')
                        expect(pageUrl.length).to.be.equal(1)
                        const siteUrlArray = getUrlInfoWithHost(pageUrl[0].innerHTML)
                        expect(siteUrlArray.length).to.be.greaterThan(4)
                        expect(this.fixture.mySitePaths).to.include(siteUrlArray[5])
                        expect(lastMod.length).to.be.equal(1)
                    })
                })
            })
        })
    })

    it('Generate sub-sitemap', function () {
        // Testcase C3218088 "Generate sub-sitemap"
        const siteUrlHome = '/jahia/page-composer/default/en/sites/mySite/home.html'
        // Loading the fixtures
        cy.fixture('C3218088/sitemap.index.urlpath.json').then((json) => {
            this.fixture = json // Recommended from Cypress as no real good way to get promise variable out (use this variable)
        })

        // Step #1 the standard enable
        siteHomePage
            .goTo(siteUrlHome)
            .editPage('My Site')
            .clickOnSitemap()
            .clickOnSave()
            .validateSucessMessage()
            .clickBack()
        // Step #2 publishing the site and flush the cache
        siteHomePage.publishSite('My Site').clickPublishAll().flushCache()

        // Need to enable SEO on "Search Result"
        siteHomePage
            .editPage('Search Results')
            .clickOnDedicatedSitemap()
            .clickOnSave()
            .validateSucessMessage()
            .clickBack()
        siteHomePage.publishSite('My Site').clickPublishAll().flushCache() // publish the whole site and flush

        // Save the root URL
        sitemapPage.goTo().inputSitemapRootURL(Cypress.config().baseUrl).clickOnSave().clickFlushCache()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500)

        // Getting the response from sitemap.index.xml
        // according to the sitemap.xml spec,
        // the url value should reside in a <loc /> node
        // https://www.google.com/sitemaps/protocol.html
        cy.requestFindNodeInnerHTMLByName('sites/mySite/sitemap.xml', 'loc').then((urls) => {
            expect(urls.length).to.be.equal(2)
            urls.forEach((url) => {
                const regexArray = getUrlInfoWithHost(String(url)) // Typescript issue that need to explicitly convert to String
                if (regexArray != null && regexArray.length > 4) {
                    const path = regexArray[5]
                    expect(this.fixture.siteMapPath).to.include(path)
                }

                cy.requestFindXMLElementByTagName(String(url), 'url').then((urlGroups) => {
                    expect(urlGroups.length).to.be.equal(1) // Only one sitemap url entry in each
                    Cypress.$(urlGroups).each(($idx, $list) => {
                        const pageUrl = $list.getElementsByTagName('loc')
                        const lastMod = $list.getElementsByTagName('lastmod')
                        expect(pageUrl.length).to.be.equal(1)
                        const siteUrlArray = getUrlInfoWithHost(pageUrl[0].innerHTML)
                        expect(siteUrlArray.length).to.be.greaterThan(4)
                        expect(lastMod.length).to.be.equal(1)
                    })
                })
            })
        })
    })

    it('Exclude pages from sitemap', function () {
        // Testcase C3218089 "Exclude pages from sitemap"
        const siteUrlHome = '/jahia/page-composer/default/en/sites/mySite/home.html'
        // Loading the fixtures
        cy.fixture('C3218089/sitemap.index.urlpath.json').then((json) => {
            this.fixture = json // Recommended from Cypress as no real good way to get promise variable out (use this variable)
        })

        // Step #1 the standard enable
        siteHomePage
            .goTo(siteUrlHome)
            .editPage('My Site')
            .clickOnSitemap()
            .clickOnSave()
            .validateSucessMessage()
            .clickBack()
        // Step #2 publishing the site and flush the cache
        siteHomePage.publishSite('My Site').clickPublishAll().flushCache()

        // Need to enable SEO on "Search Result"
        siteHomePage
            .editPage('Search Results')
            .clickOnNoIndexSitemap()
            .clickOnSave()
            .validateSucessMessage()
            .clickBack()
        siteHomePage.publishSite('My Site').clickPublishAll().flushCache() // publish the whole site and flush

        // Save the root URL
        sitemapPage.goTo().inputSitemapRootURL(Cypress.config().baseUrl).clickOnSave().clickFlushCache()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(500)

        // Getting the response from sitemap.index.xml
        // according to the sitemap.xml spec,
        // the url value should reside in a <loc /> node
        // https://www.google.com/sitemaps/protocol.html
        cy.requestFindNodeInnerHTMLByName('sites/mySite/sitemap.xml', 'loc').then((urls) => {
            expect(urls.length).to.be.equal(1)
            urls.forEach((url) => {
                const regexArray = getUrlInfoWithHost(String(url)) // Typescript issue that need to explicitly convert to String
                if (regexArray != null && regexArray.length > 4) {
                    const path = regexArray[5]
                    expect(path).to.equal(this.fixture.siteMapPath)
                }

                cy.requestFindXMLElementByTagName(String(url), 'url').then((urlGroups) => {
                    expect(urlGroups.length).to.be.equal(1) // Only one sitemap url entry as other is excluded
                    Cypress.$(urlGroups).each(($idx, $list) => {
                        const pageUrl = $list.getElementsByTagName('loc')
                        const lastMod = $list.getElementsByTagName('lastmod')
                        expect(pageUrl.length).to.be.equal(1)
                        const siteUrlArray = getUrlInfoWithHost(pageUrl[0].innerHTML)
                        expect(siteUrlArray.length).to.be.greaterThan(4)
                        expect(lastMod.length).to.be.equal(1)
                    })
                })
            })
        })
    })
})
