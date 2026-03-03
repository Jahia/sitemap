import { configureSitemap } from '../../utils/configureSitemap'
import { removeSitemapConfiguration } from '../../utils/removeSitemapConfiguration'
import { waitForSitemap } from '../../utils/generateSitemap'
import { switchToBrowsingApolloClient, switchToProcessingApolloClient } from '../../utils/apollo'
import { enableModule } from '@jahia/cypress'
import { jahiaProcessingConfig } from '../../utils/serversConfig'
import { waitUntilSyncIsComplete } from '../../utils/sync'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`
const sitemapUrl = `${siteMapRootUrl}/sitemap.xml`

describe('Testing sitemap configuration via GraphQL API', () => {
    after('Remove sitemap configuration via GraphQL', () => {
        removeSitemapConfiguration(sitePath)
    })

    beforeEach(() => {
        waitForSitemap()
    })

    before('Verify Sitemap is configured properly for digitall', () => {
        enableModule('sitemap', siteKey, jahiaProcessingConfig)
        waitUntilSyncIsComplete()
        configureSitemap(sitePath, siteMapRootUrl)
        cy.apolloProcessing({
            variables: {
                pathOrId: sitePath,
                mixinsFilter: { filters: [{ fieldName: 'name', value: 'jseomix:sitemap' }] },
                propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration'],
            },
            queryFile: 'graphql/jcrGetSitemapConfig.graphql',
        }).then((response) => {
            const r = response?.data?.jcr?.nodeByPath
            cy.log(JSON.stringify(r))
            expect(r.id).not.to.be.null
            expect(r.mixinTypes[0].name).to.equal('jseomix:sitemap')
            expect(r.name).to.equal(siteKey)
        })
    })

    // By default, digitall should have some URLs
    it('Verify that the sitemap does contain some pages', function () {
        cy.task('parseSitemap', { url: sitemapUrl }).then((urls: Array<string>) => {
            cy.log(`Sitemap contains: ${urls.length} URLs`)
            expect(urls.length).to.be.greaterThan(20)
        })
    })

    it('Should display debug info in XML when debug enabled', function () {
        ;['true', 'false'].forEach((debug) => {
            switchToProcessingApolloClient()
            cy.log(`Verifying debug info in XML when debug is ${debug}`)
            cy.apollo({
                variables: {
                    debug,
                },
                mutationFile: 'graphql/enabledDebug.graphql',
            })
            waitUntilSyncIsComplete()
            waitForSitemap()
            switchToBrowsingApolloClient()

            // Retry mechanism to ensure OSGI service has taken the configuration into account and fix flakiness of the test
            const maxRetries = 4
            const waitTime = 5000 // 5 seconds
            let retryCount = 0

            const attemptRequest = (): Cypress.Chainable => {
                return cy.request('en/sites/digitall/sitemap-lang.xml').then((response) => {
                    const debugExpected = debug === 'true'
                    const hasDebugInfo = response.body.includes('<!-- nodePath:')
                    const isValid = debugExpected === hasDebugInfo

                    if (!isValid && retryCount < maxRetries - 1) {
                        retryCount++
                        cy.log(
                            `Attempt ${retryCount}: Debug config mismatch detected. Expected debug=${debugExpected}, ` +
                                `hasDebugInfo=${hasDebugInfo}. Retrying in ${waitTime}ms...`,
                        )
                        return cy.wait(waitTime).then(() => attemptRequest())
                    }

                    // Final validation
                    if (debugExpected) {
                        expect(response.body, 'Should contain comment tags in debug').to.contains('<!-- nodePath:')
                    } else {
                        expect(response.body, 'Should not contain comment tags in non debug').not.to.contains('<!-- nodePath:')
                    }
                })
            }

            attemptRequest()
        })
    })
})
