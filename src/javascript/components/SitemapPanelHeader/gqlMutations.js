import gql from 'graphql-tag';

const deleteSitemapCache = gql`
    mutation deleteSitemapCache($siteKey: String) {
        admin {
            sitemap {
                deleteSitemapCache(siteKey: $siteKey)
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
