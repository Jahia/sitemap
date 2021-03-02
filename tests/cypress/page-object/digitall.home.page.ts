import { BasePage } from './base.page'
import { editPage } from './edit.page'

class DigitallHomePage extends BasePage {
    elements = {
        newUser: "[onclick*='addUser']",
        bulkCreateUsers: "[onclick*='bulkAddUser']",
        removeSelectedUsers: "[onclick*='bulkDeleteUser']",
    }

    goTo() {
        cy.goTo('/jahia/page-composer/default/en/sites/digitall/home.html')
        return this
    }

    editPage(page: string) {
        this.getIframeBody().contains('div', page).rightclick({ force: true })
        this.getIframeBody().contains('span', 'Edit').click()
        return editPage
    }
}

export const digitall = new DigitallHomePage()
