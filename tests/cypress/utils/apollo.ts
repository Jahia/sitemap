import { switchApolloClient } from '@jahia/cypress'
import { jahiaBrowsingConfig, jahiaProcessingConfig } from './serversConfig'
import { isInCluster } from './sync'

export const switchToProcessingApolloClient = (): void => {
    if (isInCluster()) {
        cy.log('Switching to Apollo client on Jahia Processing server (URL: ' + jahiaProcessingConfig.url + ')')
        switchApolloClient(jahiaProcessingConfig)
    }
}

export const switchToBrowsingApolloClient = (): void => {
    if (isInCluster()) {
        cy.log('Switching to Apollo client on Jahia Browsing server(s) (URL: ' + jahiaBrowsingConfig.url + ')')
        switchApolloClient(jahiaBrowsingConfig)
    }
}
