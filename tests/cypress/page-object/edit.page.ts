import { BasePage } from './base.page'

class EditPage extends BasePage {
    elements = {
        sitemap: "[id='jseomix:sitemap']",
        save: "[data-sel-role='submitSave']",
        message: '#message-id',
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
}

export const editPage = new EditPage()
