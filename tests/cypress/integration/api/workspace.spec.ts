// not an actual test. just demonstrates a test using .graphql/.gql files
import { apolloClient } from '../../support/apollo'
import { DocumentNode } from 'graphql'

describe('workspace api', () => {
    let workspace: DocumentNode

    before('load graphql file', function () {
        workspace = require(`graphql-tag/loader!../../fixtures/workspace.graphql`)
    })

    it('queries workspace correctly', async function () {
        const response = await apolloClient().query({
            query: workspace,
            variables: {
                workspace: 'EDIT',
            },
        })
        expect(response.data.jcr.workspace).to.equal('EDIT')
    })
})
