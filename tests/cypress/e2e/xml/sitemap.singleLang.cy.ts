import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding,
    setNodeProperty,
    unpublishNode,
} from '@jahia/cypress'
import { configureSitemap } from '../../utils/configureSitemap'
import { generateSitemap } from '../../utils/generateSitemap'
import { switchToBrowsingApolloClient, switchToProcessingApolloClient } from '../../utils/apollo'
import { jahiaProcessingConfig } from '../../utils/serversConfig'
import { waitUntilSyncIsComplete } from '../../utils/sync'

const siteKey = 'singleLanguageSite'
const path = `/sites/${siteKey}`

describe('Test site with one language', () => {
    beforeEach(() => {
        // Set up a site with a single language
        switchToProcessingApolloClient()
        deleteSite(siteKey)
        createSite(
            siteKey,
            {
                languages: 'en, fr',
                templateSet: 'dx-demo-templates',
                serverName: 'localhost',
                locale: 'en',
            },
            jahiaProcessingConfig,
        )
        // Set title of home in French
        setNodeProperty(path + '/home', 'jcr:title', 'home FR', 'fr')
        // Enable sitemap module and configure it
        enableModule('sitemap', siteKey, jahiaProcessingConfig)
        configureSitemap(path, Cypress.config().baseUrl + path)
        // Publish the site
        switchToProcessingApolloClient()
        publishAndWaitJobEnding(path, ['en', 'fr'])
        waitUntilSyncIsComplete()
        switchToBrowsingApolloClient()
    })

    afterEach(() => {
        switchToProcessingApolloClient()
        deleteSite(siteKey)
        waitUntilSyncIsComplete()
        switchToBrowsingApolloClient()
    })

    it('Should generate a simplified sitemap for single language site', () => {
        // Generate the sitemap
        generateSitemap(siteKey)

        // Wait for the sitemap to be properly generated
        cy.waitUntil(
            () =>
                cy
                    .request({
                        url: Cypress.config().baseUrl + path + '/sitemap-lang.xml',
                        failOnStatusCode: false,
                    })
                    .then((response) => {
                        return (
                            response.status === 200 &&
                            response.body.includes('<urlset') &&
                            response.body.includes('<url>')
                        )
                    }),
            {
                timeout: 10000,
                interval: 500,
                errorMsg: 'Sitemap was not properly generated within the timeout period',
            },
        )

        // Verify the content of the sitemap
        cy.requestFindXMLElementByTagName(Cypress.config().baseUrl + path + '/sitemap-lang.xml', 'url').then((urls) => {
            // Ensure we have at least one URL entry
            expect(urls).to.have.length.at.least(1, 'Sitemap should contain at least one URL entry')
        })

        // Check that multilang behavior is working
        // verify that xhtml:link elements exist
        cy.requestFindXMLElementByTagName(Cypress.config().baseUrl + path + '/sitemap-lang.xml', 'xhtml:link').then(
            (links) => {
                expect(links).to.not.be.empty
            },
        )

        // Unpublish french
        switchToProcessingApolloClient()
        unpublishNode(path + '/home', 'fr')
        waitUntilSyncIsComplete()
        switchToBrowsingApolloClient()

        // Generate sitemap
        generateSitemap(siteKey)

        // Verify the content of the sitemap
        cy.requestFindXMLElementByTagName(Cypress.config().baseUrl + path + '/sitemap-lang.xml', 'url').then((urls) => {
            // Ensure we have at least one URL entry
            expect(urls).to.have.length.at.least(1, 'Sitemap should contain at least one URL entry')
        })

        // verify that xhtml:link elements do not exist
        cy.requestFindXMLElementByTagName(Cypress.config().baseUrl + path + '/sitemap-lang.xml', 'xhtml:link').then(
            (links) => {
                expect(links).to.be.empty
            },
        )
    })
})
