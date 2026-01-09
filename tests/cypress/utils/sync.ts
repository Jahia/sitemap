import { waitUntilJournalSync } from '@jahia/cypress'
import { switchToBrowsingApolloClient, switchToProcessingApolloClient } from './apollo'

export const waitUntilSyncIsComplete = (): void => {
    if (isInCluster()) {
        cy.log(`Waiting for journal sync on processing server (${jahiaProcessing})...`)
        switchToProcessingApolloClient()
        waitUntilJournalSync()
        switchToBrowsingApolloClient()
        cy.log('Journal sync completed')
    } else {
        cy.log(`Journal sync not needed (processing server and browsing server are the same: ${jahiaProcessing})`)
    }
}

const jahiaProcessing = Cypress.env('JAHIA_PROCESSING_URL')
const jahiaBrowsing = Cypress.env('JAHIA_URL')

export const isInCluster = () => {
    // consider Jahia in cluster mode if processing and browsing servers are different
    return jahiaProcessing !== jahiaBrowsing
}
