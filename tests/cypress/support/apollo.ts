import { ApolloClient, InMemoryCache, ApolloLink, from, NormalizedCacheObject } from '@apollo/client/core'
import { setContext } from '@apollo/client/link/context'
import { onError } from '@apollo/client/link/error'
import { BatchHttpLink } from '@apollo/client/link/batch-http'

interface authMethod {
    token?: string
    username?: string
    password?: string
    jsessionid?: string
}

interface httpHeaders {
    authorization?: string
    Cookie?: string
}

export const apolloClient = (authMethod?: authMethod, baseUrl?: string): ApolloClient<NormalizedCacheObject> => {
    const testBaseUrl = baseUrl === undefined ? Cypress.config().baseUrl : baseUrl

    const httpLink = new BatchHttpLink({
        uri: `${testBaseUrl}/modules/graphql`,
    })

    const authHeaders: httpHeaders = {}
    if (authMethod === undefined) {
        authHeaders.authorization = `Basic ${btoa(Cypress.env('JAHIA_USERNAME') + ':' + Cypress.env('JAHIA_PASSWORD'))}`
    } else if (authMethod.token !== undefined) {
        authHeaders.authorization = `APIToken ${authMethod.token}`
    } else if (authMethod.username !== undefined && authMethod.password !== undefined) {
        authHeaders.authorization = `Basic ${btoa(authMethod.username + ':' + authMethod.password)}`
    } else if (authMethod.jsessionid !== undefined) {
        authHeaders.Cookie = 'JSESSIONID=' + authMethod.jsessionid
    }

    const authLink = setContext((_, { headers }) => {
        return {
            headers: {
                ...headers,
                ...authHeaders,
            },
        }
    })

    const errorLink = onError(({ graphQLErrors, networkError }) => {
        if (graphQLErrors)
            graphQLErrors.map(({ message, locations, path }) =>
                console.log(`[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`),
            )
        if (networkError) console.log(`[Network error]: ${networkError}`)
    })

    // Otherwise, no headers are sent and user is considered guest (i.e. apolloClient({}))
    // cy.log(`HTTP Headers ${JSON.stringify(authHeaders)}`)
    return new ApolloClient({
        cache: new InMemoryCache(),
        link: from([(authLink as unknown) as ApolloLink, errorLink, httpLink]),
        defaultOptions: {
            query: {
                errorPolicy: 'ignore',
            },
        },
    })
}
