import gql from 'graphql-tag';

const GetNodeMixin = gql`
    query findMixin($pathOrId: String!, $mixinsFilter: InputFieldFiltersInput) {
        jcr {
            nodeByPath(path: $pathOrId) {
                id: uuid
                mixinTypes(fieldFilter: $mixinsFilter) {
                    name
                }
            }
        }
    }
`;

const GetNodeSitemapInfo = gql`
    query findNodeSitemapInfo($pathOrId: String!, $mixinsFilter: InputFieldFiltersInput, $propertyNames: [String!]!) {
        jcr {
            nodeByPath(path: $pathOrId) {
                id: uuid
                mixinTypes(fieldFilter: $mixinsFilter) {
                    name
                }
                properties(names: $propertyNames) {
                    name
                    value
                }
            }
        }
    }
`;

const GetSitemapUrl = gql`
    query getSitemapUrl($siteKey: String!) {
        admin {
            sitemap {
              siteUrl(siteKey: $siteKey)
            }
        }
    }
`;

export {
    GetNodeMixin,
    GetNodeSitemapInfo,
    GetSitemapUrl
};
