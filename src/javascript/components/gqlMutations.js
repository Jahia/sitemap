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

const mutateProperty = gql`
    mutation addProperty($pathOrId: String!, $propertyName: String!, $propertyValue: String!) {
        jcr {
            mutateNode(pathOrId: $pathOrId) {
                mutateProperty(name: $propertyName) {
                    setValue(value: $propertyValue)
                }
            }
        }
    }
`;

export {AddMixin, RemoveMixin, mutateProperty};

