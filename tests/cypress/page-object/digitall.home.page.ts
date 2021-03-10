import { BasePage } from './base.page'
import { editPage } from './edit.page'
import { workflowDashboard } from './worflow.dashboard.page'

class DigitallHomePage extends BasePage {
    elements = {
        publishSiteinAllLang: "[class*='publishsiteinalllanguages']",
        editSite: "[class*='editcontentroot']",
    }

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
        this.getIframeBody().find(this.elements.editSite).click()
        return editPage
    }

    publishSite() {
        this.getIframeBody()
            .contains('div[role="row"]', 'Digitall')
            .rightclick({ force: true })
            .should('have.class', 'context-menu-open')
        this.getIframeBody().find(this.elements.publishSiteinAllLang).click()
        return workflowDashboard
    }
}

export const digitall = new DigitallHomePage()
