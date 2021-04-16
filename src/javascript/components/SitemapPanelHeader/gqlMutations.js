import gql from 'graphql-tag';

const deleteSitemapCache = gql`
    mutation deleteSitemapCache($expirationTimeDifference: Long!) {
        admin {
            sitemap {
                deleteSitemapCache(expirationTimeDifference: $expirationTimeDifference)
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
