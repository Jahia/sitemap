import { BasePage } from './base.page'

class ManageModules extends BasePage {
    elements = {
        iframeManageModules: "iframe[src*='manageModules']",

        searchInput: "input[type='search']",
        siteEnableCheckBoxToggle: "input[name='_eventId_enable']",
    }

    goTo() {
        cy.goTo('/jahia/administration/manageModules')

        // Making sure that the siteSettings inside the the iframe is available
        // this.getIframeBodySelector(this.elements.iframeManageModules)
        this.getIframeElement(this.elements.iframeManageModules, this.htmlElements.body)
        return this
    }

    selectModule(moduleName: string, siteNames: string[]) {
        this.getIframeBodySelector(this.elements.iframeManageModules)
            .find(this.elements.searchInput)
            .clear()
            .type(moduleName)
        this.getIframeBodySelector(this.elements.iframeManageModules).contains(this.htmlElements.td, moduleName).click()

        this.containIframeElement(this.elements.iframeManageModules, this.htmlElements.h2, moduleName).should(
            'be.visible',
        )

        siteNames.forEach((siteName) => {
            // Issue with the input is of size 0x0 (hacky hiding)
            this.getIframeBodySelector(this.elements.iframeManageModules)
                .contains(this.htmlElements.td, siteName)
                .siblings()
                .find(this.elements.siteEnableCheckBoxToggle)
                .clickAttached()
            this.getIframeBodySelector(this.elements.iframeManageModules)
                .contains(this.htmlElements.td, siteName)
                .siblings()
                .find(this.elements.siteEnableCheckBoxToggle)
                .should('be.checked')
        })

        return this
    }
}

export const manageModules = new ManageModules()
