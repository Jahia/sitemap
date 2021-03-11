import { BasePage } from './base.page'
import { editPage } from './edit.page'

class SiteHomePage extends BasePage {
    elements = {
        iframePageComposerFrame: 'iframe[id="page-composer-frame"]',
        iframeNestedSrcEditFrame: 'iframe[src*="editframe"]',

        divRoleRow: 'div[role="row"]',
        imgVirtualSite: 'img[src*="jnt_virtualsite"]',

        textEdit: 'Edit',
    }

    goTo(siteHomeUrl: string) {
        cy.goTo(siteHomeUrl)
        // Stabilize the nested iframe loading
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

        this.getIframeBody().contains(this.htmlElements.span, this.elements.textEdit).click()
        return editPage
    }
}

export const siteHomePage = new SiteHomePage()
