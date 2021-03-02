export class BasePage {
    /**
     * Get any element of given type that contain given text
     * It does not require to be the direct element containing text
     * example: <span><div>mytext</div></span> getByText("span", "myText") will work
     * @param type of content to find
     * @param text to find
     */
    getByText(type: string, text: string): Cypress.Chainable {
        return cy.contains(type, text)
    }

    getIframeBody(): Cypress.Chainable {
        // get the iframe > document > body
        // and retry until the body element is not empty
        return (
            cy
                .get('iframe#page-composer-frame')
                .its('0.contentDocument.body')
                .should('not.be.empty')
                // wraps "body" DOM element to allow
                // chaining more Cypress commands, like ".find(...)"
                // https://on.cypress.io/wrap
                .then(cy.wrap)
        )
    }
}
