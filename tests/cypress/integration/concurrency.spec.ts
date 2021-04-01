import { siteHomePage } from '../page-object/site.home.page'

describe('Sitemap 4.0.0', () => {
    it('Create sitemap for a site', function () {
        for (let i = 0; i < 10; i++) {
            siteHomePage
                .goTo('/jahia/page-composer/default/en/sites/digitall/home.html')
                .addPage('Digitall')
                .fillPageAndSave()
                .publishSite('Digitall')
                .clickPublishAll()
                .flushCache()
        }
    })
})
