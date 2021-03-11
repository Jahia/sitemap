import { BasePage } from './base.page'
import { digitall } from './digitall.home.page'

class WorkflowDashboardPage extends BasePage {
    elements = {
        publishAll: "[class *= 'bbar'] [class*='button-bypassworkflow']",
    }

    clickPublishAll() {
        this.getIframeBody().find(this.elements.publishAll).click()
        return digitall
    }
}

export const workflowDashboard = new WorkflowDashboardPage()
