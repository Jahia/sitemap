import { BasePage } from './base.page'
import { editPage } from './edit.page'
import { workflowDashboard } from './worflow.dashboard.page'

class DigitallHomePage extends BasePage {
    elements = {
        
    }

    goTo() {
        cy.goTo('/sites/digitall/sitemap.xml')
        return this
    }

}

export const digitall = new DigitallHomePage()
