import gql from 'graphql-tag';

const AddMixin = gql`
    mutation addMixin($pathOrId: String!, $mixins: [String!]!) {
        jcr {
            mutateNode(pathOrId: $pathOrId) {
                addMixins(mixins: $mixins)
            }
        }
    }
`;

const RemoveMixin = gql`
    mutation removeMixin($pathOrId: String!, $mixins: [String!]!) {
        jcr {
            mutateNode(pathOrId: $pathOrId) {
                removeMixins(mixins: $mixins)
            }
        }
    }
`;

const setSitemapProperties = gql`
    mutation setSitemapProperties($sitePath: String!, $sitemapIndexURL: String!, $sitemapCacheDuration: String!) {
        jcr {
            mutateNode(pathOrId: $sitePath) {
                sitemapIndexURL: mutateProperty(name: "sitemapIndexURL") {
                    setValue(value: $sitemapIndexURL)
                }
                sitemapCacheDuration: mutateProperty(name: "sitemapCacheDuration") {
                    setValue(value: $sitemapCacheDuration)
                }
            }
        }
    }
`;

export {AddMixin, RemoveMixin, setSitemapProperties};

