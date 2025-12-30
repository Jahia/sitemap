import { configureSitemap } from '../../utils/configureSitemap'
import { removeSitemapConfiguration } from '../../utils/removeSitemapConfiguration'

const siteKey = 'digitall'
const sitePath = '/sites/' + siteKey
const langDe = 'de'
const langEn = 'en'
const siteMapRootUrl = Cypress.config().baseUrl

describe('Check sitemap-lang.xml file on digitall', () => {
    before('Configure sitemap for the test', () => {
        // Set current server name as digitall j:serverName
        const host = new URL(Cypress.config().baseUrl).host
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                propertyName: 'j:serverName',
                propertyValue: host.includes(':') ? host.substring(0, host.indexOf(':')) : host,
            },
            mutationFile: 'graphql/jcrAddProperty.graphql',
        })
        configureSitemap(sitePath, siteMapRootUrl)
    })

    after('Remove sitemap configuration via GraphQL', () => {
        removeSitemapConfiguration(sitePath)
        // Set back localhost as Digitall j:serverName
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                propertyName: 'j:serverName',
                propertyValue: 'localhost',
            },
            mutationFile: 'graphql/jcrAddProperty.graphql',
        })
    })

    afterEach('Be sure to remove any invalidated languages', () => {
        // Remove edit only language
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                properties: [{ name: 'j:inactiveLiveLanguages', values: [], language: langEn }],
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })
    })

    it('should have only live languages as root url', () => {
        // Check all siteRoots available
        cy.request(Cypress.config().baseUrl + '/sitemap.xml')
            .its('body')
            .should('include', Cypress.config().baseUrl + '/sitemap-lang.xml')
            .should('include', Cypress.config().baseUrl + '/fr/sitemap-lang.xml')
            .should('include', Cypress.config().baseUrl + '/de/sitemap-lang.xml')
        // Add edit only language to the site
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                properties: [{ name: 'j:inactiveLiveLanguages', values: [langDe], language: langEn }],
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })
        // Check only 2 siteRoots available
        cy.request(Cypress.config().baseUrl + '/sitemap.xml')
            .its('body')
            .should('include', Cypress.config().baseUrl + '/sitemap-lang.xml')
            .should('include', Cypress.config().baseUrl + '/fr/sitemap-lang.xml')
            .should('not.include', Cypress.config().baseUrl + '/de/sitemap-lang.xml')
    })

    it('Should generate valid links', () => {
        cy.requestFindXMLElementByTagName(Cypress.config().baseUrl + '/sitemap.xml', 'sitemap').then((urls) => {
            Cypress.$(urls).each(($idx, $list) => {
                cy.request($list.getElementsByTagName('loc')[0].textContent)
            })
        })
    })
})
