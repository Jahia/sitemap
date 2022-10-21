export const publishAndWaitJobEnding = (path: string) => {
    cy.apollo({
        variables: {
            pathOrId: path,
            languages: ["en"],
            publishSubNodes: true,
            includeSubTree: true
        },
        mutationFile: 'graphql/jcrPublishNode.graphql',
    })

    cy.waitUntil(
        () =>
            cy
                .apollo({
                    fetchPolicy: 'no-cache',
                    variables: {
                        path: path,
                    },
                    queryFile: 'graphql/jcrPublicationStatus.graphql',
                })
                .then((response) => {
                    const publicationStatus =
                        response?.data?.jcr?.nodeByPath?.aggregatedPublicationInfo?.publicationStatus
                    return publicationStatus && publicationStatus === 'PUBLISHED'
                }),
        {
            errorMsg: 'Publication timeout for node: ' + path, // overrides the default error message
            timeout: 60000, // timeout to 1min
            verbose: true,
            interval: 1000, // performs the check every 1 sec, default to 200
        },
    )
    // Wait 2 seconds for server sync after publication
    cy.wait(2000)
}