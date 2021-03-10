import { BasePage } from './base.page'
import { digitall } from './digitall.home.page'

class EditPage extends BasePage {
    elements = {
        sitemap: "[id='jseomix:sitemap']",
        save: "[data-sel-role='submitSave']",
        message: '#message-id',
        back: "[data-sel-role='backButton']",
    }

    clickOnSitemap() {
        cy.get(this.elements.sitemap).click()
        cy.get(this.elements.sitemap).should('be.checked')
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
        this.getSiteIframeBody()
        return digitall
    }
}

export const editPage = new EditPage()
