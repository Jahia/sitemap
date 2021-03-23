// Load type definitions that come with Cypress module
/// <reference types="cypress" />

// eslint-disable-next-line @typescript-eslint/no-namespace
declare namespace Cypress {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface Chainable {
        /**
         * Custom command to navigate to url with default authentication
         * @example cy.goTo('/start')
         */
        goTo(value: string): Chainable<Element>

        clickAttached(): Chainable<Element>
        requestFindNodeInnerHTMLByName(url: string, nodeName: string): Chainable
        requestFindXMLElementByTagName(url: string, tagName: string): Chainable
    }
}

Cypress.Commands.add('goTo', function (url: string) {
    cy.visit(url, {
        auth: {
            username: Cypress.env('JAHIA_USERNAME'),
            password: Cypress.env('JAHIA_PASSWORD'),
        },
    })
})

Cypress.Commands.add('clickAttached', { prevSubject: 'element' }, (subject) => {
    cy.wrap(subject).should(($el) => {
        expect(Cypress.dom.isDetached($el)).to.be.false // Ensure the element is attached

        // Using Jquery .click() here so no queuing from cypress side and not chance for the element to detach
        $el.click()
    })
})

Cypress.Commands.add('requestFindNodeInnerHTMLByName', function (url: string, nodeName: string) {
    return cy.request(url).then((response) => {
        const nodes = Cypress.$(response.body)
            .find(nodeName)
            .toArray()
            .map((el) => el.innerText)
        return nodes
    })
})

Cypress.Commands.add('requestFindXMLElementByTagName', function (url: string, tagName: string) {
    return cy.request(url).then((response) => {
        // Convert the response to an XML
        const xml: XMLDocument = Cypress.$.parseXML(response.body)
        // Get the node group by tagName
        const nodeGroup = xml.getElementsByTagName(tagName)
        return nodeGroup
    })
})
