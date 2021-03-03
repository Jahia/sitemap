import { digitall } from '../page-object/digitall.home.page'

describe('Enable sitemap on digitall', () => {
    it('gets success message when sitemap is enabled', function () {
        digitall.goTo().editPage('Our Companies').clickOnSitemap().validateSucessMessage()
    })
})
