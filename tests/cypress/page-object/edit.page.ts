import { BasePage } from './base.page'
import { siteHomePage } from './site.home.page'

class EditPage extends BasePage {
    elements = {
        sitemap: "[id='jseomix:sitemap']",
        save: "[data-sel-role='submitSave']",
        message: '#message-id',
        back: "[data-sel-role='backButton']",

        seoSitemap: "[id='jseomix:sitemapResource']",
        dedicatedSitemap: "[id='jseomix:sitemapResource_createSitemap']",
        noIndexSitemap: "[id='jseomix:sitemapResource_noIndex']",
    }

    clickOnSitemap() {
        cy.get(this.elements.sitemap).click()
        cy.get(this.elements.sitemap).should('be.checked')
        return this
    }

    clickOnSEOSitemap() {
        cy.get(this.elements.seoSitemap).click()
        cy.get(this.elements.seoSitemap).should('be.checked')
        return this
    }

    clickOnDedicatedSitemap() {
        cy.get(this.elements.dedicatedSitemap).click()
        cy.get(this.elements.dedicatedSitemap).should('be.checked')
        return this
    }

    clickOnNoIndexSitemap() {
        cy.get(this.elements.noIndexSitemap).click()
        cy.get(this.elements.noIndexSitemap).should('be.checked')
        return this
    }

    clickOnSave() {
        cy.get(this.elements.save).should('not.be.disabled')
        cy.get(this.elements.save).clickAttached()
        return this
    }

    validateSucessMessage() {
        cy.get(this.elements.message).should('contain', 'Content successfully saved')
        return this
    }
    clickBack() {
        cy.get(this.elements.back).click()
        return siteHomePage.waitForPageLoad()
    }
}

export const editPage = new EditPage()
