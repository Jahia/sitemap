import gql from 'graphql-tag';

const getJobsStatus = gql`
    query getJobsStatus {
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
`;

export {getJobsStatus};
