import { BasePage } from './base.page'
import { editPage } from './edit.page'

class SiteHomePage extends BasePage {
    elements = {
        iframePageComposerFrame: 'iframe[id="page-composer-frame"]',
        iframeNestedSrcEditFrame: 'iframe[src*="editframe"]',

        divRoleRow: 'div[role="row"]',

        textEdit: 'Edit',
    }

    goTo(siteHomeUrl: string) {
        cy.goTo(siteHomeUrl)

        this.getIframeElement(this.elements.iframePageComposerFrame, this.elements.iframeNestedSrcEditFrame)
        return this
    }

    editPage(page: string) {
        this.getIframeBody()
            .contains(this.elements.divRoleRow, page)
            .rightclick()
            .should('have.class', 'context-menu-open')
        this.getIframeBody().contains(this.htmlElements.span, this.elements.textEdit).click()
        return editPage
    }
}

export const siteHomePage = new SiteHomePage()
