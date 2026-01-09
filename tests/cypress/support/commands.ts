// Load type definitions that come with Cypress module
/// <reference types="cypress" />

import { switchToBrowsingApolloClient, switchToProcessingApolloClient } from '../utils/apollo'
import { ApolloOptions } from '@jahia/cypress/src/support/apollo/apollo'
import { waitUntilSyncIsComplete } from '../utils/sync'
import { ApolloQueryResult, FetchResult } from '@apollo/client/core'

// eslint-disable-next-line @typescript-eslint/no-namespace
declare namespace Cypress {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface Chainable<Subject> {
        clickAttached(): Chainable<Element>
        requestFindNodeInnerHTMLByName(url: string, nodeName: string): Chainable<never>
        requestFindXMLElementByTagName(url: string, tagName: string): Chainable<never>

        /**
         * Execute an Apollo mutation/query on the processing server and wait for sync
         * @example cy.apolloProcessing({ variables: {...}, mutationFile: '...' })
         */
        apolloProcessing(options: ApolloOptions): Chainable<ApolloQueryResult<never> | FetchResult>
    }
}

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

Cypress.Commands.add('apolloProcessing', function (options) {
    switchToProcessingApolloClient()
    cy.apollo(options).then((result) => {
        waitUntilSyncIsComplete()
        switchToBrowsingApolloClient()
        cy.wrap(result)
    })
})
