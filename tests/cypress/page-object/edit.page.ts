import { BasePage } from './base.page'

class EditPage extends BasePage {
    elements = {
        sitemap: "[id='jseomix:sitemap']",
        sitemapspan: "[data-sel-role-dynamic-fieldset='jseomix:sitemap']",
        save: "[data-sel-role='submitSave']",
        message: '#message-id',
    }

    clickOnSitemap() {
        cy.get(this.elements.sitemap).click()
        // Known issue that transformX render to matrix Cypress cannot handle properly (directly use matrix for now)
        cy.get(this.elements.sitemapspan).should('have.css', 'transform', 'matrix(1, 0, 0, 1, 14, 0)')
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
