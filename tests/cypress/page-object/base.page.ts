export class BasePage {
    htmlElements = {
        td: 'td',
        h2: 'h2',
        h4: 'h4',
        body: 'body',
        span: 'span',
    }

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

    /**
     * waits for the body inside the iframe to appear
     * returns the body of the iframe
     */
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

    /**
     * Waits for the body inside the iframe to appear for sites (nested frames)
     * returns the body of the iframe
     * @param iframeSelector    - src attribute of the iframe
     * @param itsPropertyPath   - property path to check again EX: '0.contentDocument.body'
     */
    getSiteIframeBody(iframeSelector: string, itsPropertyPath?: string): Cypress.Chainable {
        // get the iframe > document > body
        // and retry until the body element is not empty
        return (
            this.getIframeBody()
                .find(iframeSelector)
                .its(itsPropertyPath)
                .should('not.be.empty')
                // wraps "body" DOM element to allow
                // chaining more Cypress commands, like ".find(...)"
                // https://on.cypress.io/wrap
                .then(cy.wrap)
        )
    }

    /**
     * Waits for the body inside the iframe to appear for sites
     * @param selector  - HTML selector
     * @param options   - cypress get options
     */
    getIframeBodySelector(
        selector: string,
        options?: Partial<Cypress.Loggable & Cypress.Timeoutable & Cypress.Withinable & Cypress.Shadow>,
    ): Cypress.Chainable {
        return cy.get(selector, options).its('0.contentDocument.body').should('not.be.empty').then(cy.wrap)
    }

    /**
     * Waits to "find" specific elements inside the iframe
     * NOTE: We are using direct JQUERY inside
     * REASON: limitation with existing Cypress framework
     * @param iframeSelector    - iframe selector
     * @param elementSelector   - HTML element selector
     * @param timeout           - timeout for looping checks
     */
    getIframeElement(iframeSelector: string, elementSelector: string, timeout = 60000): Cypress.Chainable {
        return cy
            .get(iframeSelector, { timeout: timeout })
            .should(($iframe) => {
                expect($iframe.contents().find(elementSelector)).to.exist
            })
            .then(($iframe) => {
                return cy.wrap($iframe.contents().find(elementSelector))
            })
    }

    /**
     * Waits to if "contains" specific elements inside the iframe
     * NOTE: We are using direct JQUERY inside
     * REASON: limitation with existing Cypress framework
     * @param iframeSelector    - iframe selector
     * @param elementSelector   - HTML element selector
     * @param text              - text to select from
     * @param timeout           - timeout for looping checks
     */
    containIframeElement(
        iframeSelector: string,
        elementSelector: string,
        text: string,
        timeout = 60000,
    ): Cypress.Chainable {
        return cy
            .get(iframeSelector, { timeout: timeout })
            .should(($iframe) => {
                expect($iframe.contents().find(`${elementSelector}:contains(${text})`)).to.exist
            })
            .then(($iframe) => {
                return cy.wrap($iframe.contents().find(`${elementSelector}:contains(${text})`))
            })
    }

    containPageComposerIframeElement(elementSelector: string, text: string): Cypress.Chainable {
        return this.getIframeBody()
            .should(($iframe) => {
                expect($iframe.contents().find(`${elementSelector}:contains(${text})`)).to.exist
            })
            .then(($iframe) => {
                return cy.wrap($iframe.contents().find(`${elementSelector}:contains(${text})`))
            })
    }
}
