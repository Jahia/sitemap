import { siteHomePage } from '../page-object/site.home.page'

describe('Sitemap 4.0.0', () => {
    it('Create sitemap for a site', function () {
        siteHomePage
            .goTo()
            .editPage('Digitall')
            .clickOnSitemap()
            .validateSucessMessage()
            .clickBack()
            .publishSite()
            .clickPublishAll()
            .flushCache()

        // fetch the sitemap content
        cy.request('sites/digitall/sitemap.index.xml').then((response) => {
            // convert sitemap xml body to an array of urls
            const languageUrls = Cypress.$(response.body)
                .find('loc')
                // map to a js array
                .toArray()
                // get the text of the <loc /> node
                .map((el) => el.innerText)
            assert.equal(languageUrls.length, 3, 'There should be 3 urls, one for each language')

            languageUrls.forEach((url) => {
                cy.request(url).then((response) => {
                    // Convert the response to an XML
                    const xml: XMLDocument = Cypress.$.parseXML(response.body)
                    // Get the node group under 'url'
                    const urlGroups = xml.getElementsByTagName('url')
                    // Go over each one and make assertions
                    assert.equal(urlGroups.length, 14, 'There should be 14 urls, one for each page')
                    Cypress.$(urlGroups).each(function () {
                        const pageUrl = this.getElementsByTagName('loc')
                        assert.equal(pageUrl.length, 1, 'There should be 1 url for each group')
                        expect(pageUrl.item(0)).to.contain('html')
                        const lastMod = this.getElementsByTagName('lastmod')
                        assert.equal(lastMod.length, 1, 'There should be 1 lastmod for each group')
                        expect(lastMod.item(0)).to.contain('-')
                        const links = this.getElementsByTagName('xhtml:link')
                        assert.equal(links.length, 3, 'There should be 3 alternative links for each group')
                        // go over each xhtml:link and make assertions
                        Cypress.$(links).each(function () {
                            expect(this.getAttribute('rel')).to.eq('alternate')
                            expect(['en', 'fr', 'de']).to.be.contain(this.getAttribute('hreflang'))
                            expect(this.getAttribute('href')).to.contain('html')
                        })
                    })
                })
            })
        })
    })
})
