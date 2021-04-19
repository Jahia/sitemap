import gql from 'graphql-tag';

const deleteSitemapCache = gql`
    mutation deleteSitemapCache($expirationTimeDifference: Long!, $siteKey: String) {
        admin {
            sitemap {
                deleteSitemapCache(expirationTimeDifference: $expirationTimeDifference, siteKey: $siteKey)
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

export {deleteSitemapCache, sendSitemapToSearchEngine};
