import React from 'react';
import PropTypes from 'prop-types';
import {Card} from '@material-ui/core';
import {Button, File, OpenInNew, Typography} from '@jahia/moonstone';
import styles from './SitemapIndexLink.scss';

const SitemapIndexLink = ({inputUrl, t}) => {
    const indexUrl = `${inputUrl}${window.contextJsParameters.contextPath}/sitemap.xml`;
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
    inputUrl: PropTypes.string,
    t: PropTypes.func
};

export default SitemapIndexLink;
