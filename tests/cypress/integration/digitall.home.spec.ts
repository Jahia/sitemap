import { digitall } from '../page-object/digitall.home.page'

describe('Enable sitemap on digitall', () => {
    it('gets success message when sitemap is enabled', function () {
        digitall
            .goTo()
            .editPage('Digitall')
            .clickOnSitemap()
            .validateSucessMessage()
            .clickBack()
            .publishSite()
            .clickPublishAll()
            .flushCache()

        // getch the sitemap content
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
                    const responseBody = Cypress.$(response.body)
                    const pageUrls = responseBody
                        .find('loc')
                        .toArray()
                        .map((el) => el.innerText)
                    assert.equal(pageUrls.length, 14, 'There should be 14 urls, one for each page')
                    pageUrls.forEach((pageUrl) => expect(pageUrl).to.contain('html'))

                    const lastMods = responseBody
                        .find('lastmod')
                        .toArray()
                        .map((el) => el.innerText)
                    assert.equal(lastMods.length, 14, 'There should be 14 modification dates, one for each page')
                    // Need to continue First approach for xhtml:link
                })
            })
        })
    })
})
