import { siteHomePage } from '../page-object/site.home.page'
import { webProjectSettings } from '../page-object/webProjectSettings.page'
import { manageModules } from '../page-object/manageModules.page'

describe('Enable sitemap on MySite', () => {
    beforeEach(() => {
        // Create the site and enable the module
        webProjectSettings.goTo().createProject('My Site', 'mySite')
        manageModules.goTo().selectModule('Jahia Sitemap', ['mySite'])
    })

    it('gets success message when sitemap is enabled', function () {
        const siteUrlHome = '/jahia/page-composer/default/en/sites/mySite/home.html'
        siteHomePage.goTo(siteUrlHome).editPage('My Site').clickOnSitemap().validateSucessMessage()
    })

    afterEach(() => {
        // Delete the site perform cleanup
        webProjectSettings.goTo().deleteProject('My Site')
    })
})
