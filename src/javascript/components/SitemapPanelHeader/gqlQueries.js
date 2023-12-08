import gql from 'graphql-tag';
import {PredefinedFragments} from '@jahia/data-helper';

const getJobsStatus = gql`
    query getJobsStatus($path: String!) {
         jcr {
            nodeByPath(path:$path) {
                property(name:"isSitemapJobTriggered") {
                    isSitemapJobTriggered: booleanValue
                }
                ...NodeCacheRequiredFields
            }
        }
        admin {
            jahia {
                scheduler {
                    jobs {
                        name
                        group
                        jobState
                        jobStatus
                        duration
                    }
                }
            }
        }
    }
    ${PredefinedFragments.nodeCacheRequiredFields.gql}
`;

export {getJobsStatus};
