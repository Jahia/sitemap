import { BasePage } from './base.page'
import { editPage } from './edit.page'
import { workflowDashboard } from './worflow.dashboard.page'

class SiteHomePage extends BasePage {
    elements = {
        iframePageComposerFrame: 'iframe[id="page-composer-frame"]',
        iframeNestedSrcEditFrame: 'iframe[src*="editframe"]',
        divRoleRow: 'div[role="row"]',
        imgVirtualSite: 'img[src*="jnt_virtualsite"]',

        publishSite: '[class*="publishsite-sitenode"]',
        publishSiteinAllLang: "[class*='publishsiteinalllanguages']",

        editSite: "[class*='editcontentroot']",
        cacheButton: '.edit-menu-cache',
        flushAll: "[class*='flushall']",
    }

    goTo(siteHomeUrl: string) {
        cy.goTo(siteHomeUrl)
        this.waitForPageLoad()
        return this
    }

    waitForPageLoad() {
        this.getIframeElement(this.elements.iframePageComposerFrame, this.elements.iframeNestedSrcEditFrame)

        this.getSiteIframeBody(this.elements.iframeNestedSrcEditFrame, '0.contentDocument.body')
            .find('.editmodeArea')
            .should('be.visible')
        return this
    }

    editPage(page: string) {
        this.getIframeElement(this.elements.iframePageComposerFrame, this.elements.imgVirtualSite)

        this.getIframeBody()
            .contains(this.elements.divRoleRow, page)
            .trigger('mouseover') // Stabilize portion right before the right-click so it hover over the right element
            .rightclick()
            .should('have.class', 'context-menu-open')

        this.getIframeBody().find(this.elements.editSite).click()
        return editPage
    }

    publishSite(site: string) {
        return this.clickContextMenuItem(site, this.elements.publishSite)
    }
    publishAllSite(site: string) {
        return this.clickContextMenuItem(site, this.elements.publishSiteinAllLang)
    }

    clickContextMenuItem(site: string, itemSelector: string) {
        this.getIframeBody()
            .contains(this.elements.divRoleRow, site)
            .trigger('mouseover') // Stabilize portion right before the right-click so it hover over the right element
            .rightclick()
            .should('have.class', 'context-menu-open')
        this.getIframeBody().find(itemSelector).click()
        return workflowDashboard
    }

    flushCache() {
        this.getIframeBody().find(this.elements.cacheButton).click()
        this.getIframeBody().find(this.elements.flushAll).click()
        // make sure we get success message before continuing
        this.containPageComposerIframeElement('.x-info-body', 'All caches have been flushed')
        return this
    }
}

export const siteHomePage = new SiteHomePage()
