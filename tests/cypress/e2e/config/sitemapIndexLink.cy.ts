import { enableModule } from '@jahia/cypress'
import { SitemapPage } from '../../page-object/sitemap.page'
import { removeSitemapConfiguration } from '../../utils/removeSitemapConfiguration'
import { switchToBrowsingApolloClient, switchToProcessingApolloClient } from '../../utils/apollo'
import { waitUntilSyncIsComplete } from '../../utils/sync'
import { jahiaProcessingConfig } from '../../utils/serversConfig'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const indexLinkButton = "button[data-sel-role='sitemapIndexLinkButton']"

const setSitemapIndexUrl = (value: string): void => {
    switchToProcessingApolloClient()
    cy.apollo({
        variables: { pathOrId: sitePath, mixins: ['jseomix:sitemap'] },
        mutationFile: 'graphql/jcrAddSitemapMixin.graphql',
    })
    cy.apollo({
        variables: { pathOrId: sitePath, propertyName: 'sitemapIndexURL', propertyValue: value },
        mutationFile: 'graphql/jcrAddProperty.graphql',
    })
    waitUntilSyncIsComplete()
    switchToBrowsingApolloClient()
}

const clickIndexLink = (): void => {
    SitemapPage.visit(siteKey, 'en')
    cy.get(indexLinkButton).should('be.visible')
    cy.window().then((win) => {
        cy.stub(win, 'open').as('windowOpen')
    })
    cy.get(indexLinkButton).click()
}

describe('Sitemap index link', () => {
    before(() => {
        enableModule('sitemap', siteKey, jahiaProcessingConfig)
    })

    after(() => {
        removeSitemapConfiguration(sitePath)
    })

    it('opens an http or https sitemap index URL in a new window', () => {
        const indexUrl = "https://www.example.org/l'été&co"
        setSitemapIndexUrl(indexUrl)
        clickIndexLink()
        cy.get('@windowOpen').should('have.been.calledOnce')
        cy.get('@windowOpen').its('firstCall.args.0').should('eq', `${indexUrl}/sitemap.xml`)
    })

    it('opens no window for a sitemap index URL with another scheme', () => {
        setSitemapIndexUrl('ftp://ftp.example.org')
        clickIndexLink()
        cy.get('@windowOpen').should('not.have.been.called')
    })
})
