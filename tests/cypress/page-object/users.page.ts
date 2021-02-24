import { BasePage } from './base.page'

class UsersPage extends BasePage {
    elements = {
        newUser: "[onclick*='addUser']",
        bulkCreateUsers: "[onclick*='bulkAddUser']",
        removeSelectedUsers: "[onclick*='bulkDeleteUser']",
    }

    goTo() {
        cy.goTo('/cms/adminframe/default/en/settings.manageUsers.html')
        // The next click is necessary to reload the css. it doesn't do anything besides that
        this.clickOnCreateNewUser()
        return this
    }

    clickOnCreateNewUser() {
        cy.get(this.elements.newUser).click()
        // Should return new class here
    }

    editUser(username: string) {
        this.getByText('a', username).click()
    }
}

export const users = new UsersPage()
