import React from 'react';
import PropTypes from 'prop-types';
import {Card} from '@material-ui/core';
import {Button, File, OpenInNew, Typography} from '@jahia/moonstone';
import styles from './SitemapIndexLink.scss';
import {useGetSitemapUrl} from '~/hooks/graphql';

/**
 * Get hostname part of the input URL
 * @param inputUrl e.g. "https://yahoo.com:8080/path/to/url"
 * @returns {string|string} hostname part - e.g. "https://yahoo.com:8080"
 */
const getHostname = inputUrl => {
    let hostname = inputUrl;
    try {
        const url = new URL(inputUrl);
        hostname = url.origin;
    } catch (_) {
        // Ignore if unparseable
    }

    return hostname;
};

const SitemapIndexLink = ({inputUrl, siteKey, t}) => {
    const [siteUrl, isSeoRulesEnabled] = useGetSitemapUrl(siteKey);
    const indexUrl = (isSeoRulesEnabled || !siteUrl) ?
        `${inputUrl}${window.contextJsParameters.contextPath}/sitemap.xml` :
        `${getHostname(inputUrl)}${siteUrl}/sitemap.xml`;

    return (
        <>
            <Typography className={styles.sitemapIndexFileTitle} component="h3">
                {t('labels.settingSection.sitemapIndexFileSection.title')}
            </Typography>
            <Typography className={styles.sitemapIndexFileDescription} component="p">
                {t('labels.settingSection.sitemapIndexFileSection.description')}
            </Typography>

            <Card>
                <div className={styles.sitemapIndexFileCardArea}>
                    {
                        (inputUrl) ? (
                            <>
                                <File className={styles.sitemapIndexFileIconEnabled} size="big"/>
                                <Typography className={styles.sitemapIndexFileName} component="p">
                                    {indexUrl}
                                </Typography>
                                <Button variant="ghost"
                                        data-sel-role="sitemapIndexLinkButton"
                                        className={styles.sitemapIndexFileButton}
                                        icon={<OpenInNew size="big"/>}
                                        onClick={() => window.open(indexUrl, '_blank')}/>
                            </>
                        ) : (
                            <Typography className={`${styles.sitemapIndexFileName} ${styles.disabled}`} component="p">
                                {t('labels.settingSection.sitemapIndexFileSection.missing')}
                            </Typography>
                        )
                    }
                </div>
            </Card>
        </>
    );
};

SitemapIndexLink.propTypes = {
    inputUrl: PropTypes.string.isRequired,
    siteKey: PropTypes.string.isRequired,
    t: PropTypes.func
};

export default SitemapIndexLink;
