import { BasePage } from './base.page'
import { siteHomePage } from './site.home.page'

class WorkflowDashboardPage extends BasePage {
    elements = {
        publishAll: "[class *= 'bbar'] [class*='button-bypassworkflow']",
    }

    clickPublishAll() {
        this.getIframeBody().find(this.elements.publishAll).click()
        // make sure we get success message before continuing
        this.containPageComposerIframeElement('.x-info-body', 'Content published')
        return siteHomePage.waitForPageLoad()
    }
}

export const workflowDashboard = new WorkflowDashboardPage()
