import gql from 'graphql-tag';

const triggerSitemapJob = gql`
    mutation triggerSitemapJob($siteKey: String) {
        admin {
            sitemap {
                triggerSitemapJob(siteKey: $siteKey)
            }
        }
    }
`;

const sendSitemapToSearchEngine = gql`
    mutation sendSitemapToSearchEngine($sitemapURL: String!) {
        admin {
            sitemap {
                sendSitemapToSearchEngine(sitemapURL: $sitemapURL)
            }
        }
    }
`;

export {triggerSitemapJob, sendSitemapToSearchEngine};
