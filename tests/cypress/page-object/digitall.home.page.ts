import { BasePage } from './base.page'
import { editPage } from './edit.page'

class DigitallHomePage extends BasePage {
    elements = {}

    goTo() {
        cy.goTo('/jahia/page-composer/default/en/sites/digitall/home.html')
        this.getSiteIframeBody()
        return this
    }

    editPage(page: string) {
        this.getIframeBody()
            .contains('div[role="row"]', page)
            .rightclick({ force: true })
            .should('have.class', 'context-menu-open')
        this.getIframeBody().contains('span', 'Edit').click()
        return editPage
    }
}

export const digitall = new DigitallHomePage()
