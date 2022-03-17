export class SitemapPage {
    elements = {
        saveButton: "button[data-sel-role='sitemapSubmitButton']",
        sitemapRootUrlInput: "input[data-sel-role='sitemapIndexURL']",
        headerFlushCacheSpan: "button[data-sel-role='sitemapFlushCacheButton']",
        dialogFlushCacheSpan: "button[data-sel-role='sitemapDialogSubmitButton']",
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    static visit(siteKey = 'mySite', lang = 'en') {
        cy.goTo(`/jahia/jcontent/${siteKey}/${lang}/apps/siteSettingsSeo/sitemap`)
        return new SitemapPage()
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    inputSitemapRootURL(serverName = 'http://localhost:8080') {
        cy.get(this.elements.sitemapRootUrlInput).clear().type(serverName)
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    clickOnSave() {
        cy.get(this.elements.saveButton).should('not.be.disabled')
        cy.get(this.elements.saveButton).click()
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    clickFlushCache() {
        cy.get(this.elements.headerFlushCacheSpan, { timeout: 10000 }).click()
        cy.get(this.elements.dialogFlushCacheSpan, { timeout: 10000 }).click()
    }
}
