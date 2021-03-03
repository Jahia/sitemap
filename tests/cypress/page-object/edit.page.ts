import { BasePage } from './base.page'

class EditPage extends BasePage {
    elements = {
        sitemap: "[id='jmix:sitemap']",
        save: "[data-sel-role='submitSave']",
        message: '#message-id',
    }

    clickOnSitemap() {
        cy.get(this.elements.sitemap).click()
        cy.get(this.elements.save).click()
        return this
    }

    validateSucessMessage() {
        cy.get(this.elements.message).should('contain', 'Content successfully saved')
        return this
    }
}

export const editPage = new EditPage()
