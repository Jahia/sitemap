import {useEffect, useState} from 'react';
import {useQuery} from '@apollo/react-hooks';
import {GetSitemapUrl} from '../../components/gqlQueries';

const useGetSitemapUrl = siteKey => {
    const [siteUrl, setSiteUrl] = useState(null);

    const {loading, data, error} = useQuery(GetSitemapUrl, {
        variables: {siteKey}
    });

    // Trigger change when data is ready
    useEffect(() => {
        if (error) {
            setSiteUrl(null);
            console.error('Unable to fetch site URL');
        } else if (!loading && data) {
            const {siteUrl} = data.admin?.sitemap;
            setSiteUrl(siteUrl);
        }
    }, [loading, data, error]);

    return [siteUrl];
};

export default useGetSitemapUrl;
