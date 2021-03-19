import { BasePage } from './base.page'
import { siteHomePage } from './site.home.page'

class WorkflowDashboardPage extends BasePage {
    elements = {
        publishAll: "[class *= 'bbar'] [class*='button-bypassworkflow']",
    }

    clickPublishAll() {
        this.getIframeBody().find(this.elements.publishAll).click()
        return siteHomePage.waitForPageLoad()
    }
}

export const workflowDashboard = new WorkflowDashboardPage()
