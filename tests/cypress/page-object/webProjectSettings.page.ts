import { BasePage } from './base.page'

class WebProjectSettings extends BasePage {
    elements = {
        iframeWebProjectSettings: "iframe[src*='webProjectSettings']",
        createSiteAnchor: "[id='createSite']",
        createSiteForm: "form[id='createSiteForm']",
        createYourProjectNextButton: "button[name='_eventId_next']",

        titleInput: "input[id='title']",
        siteKeyInput: "input[id='siteKey']",
        serverNameInput: "input[id='serverName']",

        textProject: 'Project',
        textPleaseChooseATemplateSet: 'Please choose a template set',
        textDoYouWantToContinue: 'Do you want to continue?',
        textPleaseBeCareful: 'Please be careful: this will permanently delete the Project from your server.',

        deleteSiteAnchor: "a[onclick*='deleteSites']",
        deleteSiteConfirmationButton: "button[name='_eventId_deleteSitesConfirmed']",
    }

    goTo() {
        cy.goTo('/jahia/administration/webProjectSettings')

        // Making sure that the siteSettings inside the the iframe is available
        // this.getIframeBodySelector(this.elements.iframeWebProjectSettings)
        this.getIframeElement(this.elements.iframeWebProjectSettings, this.htmlElements.body)
        return this
    }

    createProject(name = 'My Site', siteKey = 'mySite', serverName = 'localhost') {
        this.containIframeElement(
            this.elements.iframeWebProjectSettings,
            this.htmlElements.h2,
            this.elements.textProject,
        ).should('be.visible')

        this.getIframeBodySelector(this.elements.iframeWebProjectSettings).find(this.elements.createSiteAnchor).click()

        this.getIframeElement(this.elements.iframeWebProjectSettings, this.elements.titleInput)
        this.getIframeElement(this.elements.iframeWebProjectSettings, this.elements.siteKeyInput)
        this.getIframeElement(this.elements.iframeWebProjectSettings, this.elements.serverNameInput).should(
            'be.visible',
        )

        this.getIframeBodySelector(this.elements.iframeWebProjectSettings)
            .find(this.elements.titleInput)
            .clear()
            .type(name)
        this.getIframeBodySelector(this.elements.iframeWebProjectSettings)
            .find(this.elements.siteKeyInput)
            .clear()
            .type(siteKey)
        this.getIframeBodySelector(this.elements.iframeWebProjectSettings)
            .find(this.elements.serverNameInput)
            .clear()
            .type(serverName)

        this.getIframeBodySelector(this.elements.iframeWebProjectSettings)
            .find(this.elements.createYourProjectNextButton)
            .clickAttached()

        this.containIframeElement(
            this.elements.iframeWebProjectSettings,
            this.htmlElements.h4,
            this.elements.textPleaseChooseATemplateSet,
        ).should('be.visible')

        this.getIframeBodySelector(this.elements.iframeWebProjectSettings)
            .find(this.elements.createYourProjectNextButton)
            .clickAttached()

        this.containIframeElement(this.elements.iframeWebProjectSettings, this.htmlElements.td, name).should(
            'be.visible',
        )
        this.containIframeElement(this.elements.iframeWebProjectSettings, this.htmlElements.td, siteKey).should(
            'be.visible',
        )
        this.containIframeElement(this.elements.iframeWebProjectSettings, this.htmlElements.td, serverName).should(
            'be.visible',
        )

        this.getIframeBodySelector(this.elements.iframeWebProjectSettings)
            .find(this.elements.createYourProjectNextButton)
            .clickAttached()

        return this
    }

    deleteProject(name = 'My Site') {
        this.containIframeElement(
            this.elements.iframeWebProjectSettings,
            this.htmlElements.h2,
            this.elements.textProject,
        ).should('be.visible')

        this.getIframeBodySelector(this.elements.iframeWebProjectSettings).then(($iframe) => {
            if ($iframe.contents().find(`${this.htmlElements.td}:contains(${name})`).length > 0) {
                this.getIframeBodySelector(this.elements.iframeWebProjectSettings)
                    .contains(this.htmlElements.td, name)
                    .siblings()
                    .find(this.elements.deleteSiteAnchor)
                    .click()

                this.containIframeElement(
                    this.elements.iframeWebProjectSettings,
                    this.htmlElements.h2,
                    this.elements.textDoYouWantToContinue,
                ).should('be.visible')
                this.containIframeElement(
                    this.elements.iframeWebProjectSettings,
                    this.htmlElements.h4,
                    this.elements.textPleaseBeCareful,
                ).should('be.visible')

                this.getIframeElement(
                    this.elements.iframeWebProjectSettings,
                    this.elements.deleteSiteConfirmationButton,
                ).click()

                this.containIframeElement(this.elements.iframeWebProjectSettings, this.htmlElements.td, name).should(
                    'not.exist',
                )
            }
        })

        return this
    }
}

export const webProjectSettings = new WebProjectSettings()
