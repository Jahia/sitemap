import {useEffect, useState} from 'react';
import {useLazyQuery} from '@apollo/react-hooks';
import {GetSitemapUrl} from '../../components/gqlQueries';

const useGetSitemapUrl = siteKey => {
    const [siteUrl, setSiteUrl] = useState(null);
    const [isSeoRulesEnabled, setSeoRulesEnabled] = useState(true);
    const [fetchSiteUrl, {loading, data}] = useLazyQuery(GetSitemapUrl);

    // Call graphql query only if siteKey changes
    useEffect(() => {
        if (siteKey) {
            fetchSiteUrl({
                variables: {siteKey}
            });
        }
    }, [siteKey, fetchSiteUrl]);

    // Trigger change when data is ready
    useEffect(() => {
        if (!loading && data) {
            const {urlRewriteSeoRulesEnabled, siteUrl} = data.admin?.sitemap;
            // Default to true if it has no value
            setSeoRulesEnabled((typeof urlRewriteSeoRulesEnabled === 'boolean') ?
                urlRewriteSeoRulesEnabled : true);
            setSiteUrl(siteUrl);
        }
    }, [loading, data]);

    return [siteUrl, isSeoRulesEnabled];
};

export default useGetSitemapUrl;
