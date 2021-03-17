import { BasePage } from './base.page'

class DigitallHomePage extends BasePage {
    elements = {
        goTo() {
            cy.goTo('/sites/digitall/sitemap.xml')
            return this
        },
    }
}

export const digitall = new DigitallHomePage()
