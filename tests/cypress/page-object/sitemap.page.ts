import { BasePage } from './base.page'

class SitemapPage extends BasePage {
    elements = {
        saveButton: "button[type='submit']",
        sitemapRootUrlInput: "input[id='sitemapIndexURL']",
        headerFlushCacheSpan: 'Flush cache',
        dialogFlushCacheSpan: 'flush cache',
    }

    goTo(siteKey = 'mySite', lang = 'en') {
        cy.goTo(`/jahia/jcontent/${siteKey}/${lang}/apps/siteSettingsSeo/sitemap`)
        return this
    }

    inputSitemapRootURL(serverName = 'http://localhost:8080') {
        cy.get(this.elements.sitemapRootUrlInput).clear().type(serverName)
        return this
    }

    clickOnSave() {
        cy.get(this.elements.saveButton).should('not.be.disabled')
        cy.get(this.elements.saveButton).clickAttached()
        return this
    }

    clickFlushCache() {
        cy.contains(this.htmlElements.span, this.elements.headerFlushCacheSpan).clickAttached()
        cy.contains(this.htmlElements.span, this.elements.dialogFlushCacheSpan).clickAttached()
    }
}

export const sitemapPage = new SitemapPage()
