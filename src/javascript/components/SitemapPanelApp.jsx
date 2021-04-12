import React, {useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import classnames from 'clsx';
import {Typography, Button, Chip} from '@jahia/moonstone';
import {Dropdown, Input, File, OpenInNew, Check} from '@jahia/moonstone';

import styles from './SitemapPanel.scss';

import {Card} from '@material-ui/core';

import * as compose from 'lodash.flowright';
import {withApollo} from 'react-apollo';
import {withTranslation} from 'react-i18next';
import {useQuery} from '@apollo/react-hooks';

import * as gqlMutations from './gqlMutations';
import * as gqlQueries from './gqlQueries';
import * as gqlUtilities from '../utils/gqlUtilities';

import {SnackbarComponent} from './Snackbar/Snackbar';
import {SitemapPanelHeaderComponent} from './SitemapPanelHeader/SitemapPanelHeader';
import {useFormik} from 'formik';

const SitemapPanelApp = ({client, dxContext, t}) => {
    const [sitemapMixinEnabled, setSitemapMixinEnabled] = useState(false);
    const [sitemapIndexURL, setSitemapIndexURL] = useState(null);
    const [sitemapCacheDuration, setSitemapCacheDuration] = useState(null);

    const {data, error, loading} = useQuery(gqlQueries.GetNodeSitemapInfo, {
        variables: {
            pathOrId: `/sites/${dxContext.siteKey}`,
            mixinsFilter: {
                filters: [
                    {fieldName: 'name', value: 'jseomix:sitemap'}
                ]
            },
            propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration']
        },
        fetchPolicy: 'no-cache'
    });

    const [snackbarIsOpen, setSnackbarIsOpen] = useState(false);

    useEffect(() => {
        if (data) {
            if (data?.jcr?.nodeByPath?.mixinTypes?.length > 0) {
                setSitemapMixinEnabled(true);
            }

            const properties = data?.jcr?.nodeByPath?.properties;
            if (properties.length > 0) {
                properties.forEach(property => {
                    if (property.name === 'sitemapIndexURL') {
                        setSitemapIndexURL(property.value);
                    } else if (property.name === 'sitemapCacheDuration') {
                        setSitemapCacheDuration(property.value);
                    }
                });
            }
        }
    }, [data, dxContext.siteKey]);

    const dropdownData = [{
        label: `4 ${t('labels.settingSection.updateIntervalSection.hours')}`,
        value: '4h'
    }, {
        label: `8 ${t('labels.settingSection.updateIntervalSection.hours')}`,
        value: '8h'
    }, {
        label: `24 ${t('labels.settingSection.updateIntervalSection.hours')}`,
        value: '24h'
    }, {
        label: `48 ${t('labels.settingSection.updateIntervalSection.hours')}`,
        value: '48h'
    }];

    const convertCacheDurationToLabel = durationValue => {
        switch (durationValue) {
            case dropdownData[1].value:
                return dropdownData[1].label;
            case dropdownData[2].value:
                return dropdownData[2].label;
            case dropdownData[3].value:
                return dropdownData[3].label;
            default:
                return dropdownData[0].label;
        }
    };

    const formik = useFormik({
        initialValues: {
            sitemapIndexURL: sitemapIndexURL,
            sitemapCacheDuration: ((sitemapCacheDuration) ? sitemapCacheDuration : dropdownData[0].value)
        },
        enableReinitialize: true,
        // eslint-disable-next-line no-unused-vars
        onSubmit: values => {
            if (!sitemapMixinEnabled) {
                gqlUtilities.gqlMutate(client, gqlMutations.AddMixin, {
                    pathOrId: `/sites/${dxContext.siteKey}`,
                    mixins: ['jseomix:sitemap']
                });
                setSitemapMixinEnabled(prevState => !prevState);
                setSnackbarIsOpen(true);
            }

            gqlUtilities.gqlMutate(client, gqlMutations.mutateProperty, {
                pathOrId: `/sites/${dxContext.siteKey}`,
                propertyName: 'sitemapIndexURL',
                propertyValue: formik.values.sitemapIndexURL
            });
            gqlUtilities.gqlMutate(client, gqlMutations.mutateProperty, {
                pathOrId: `/sites/${dxContext.siteKey}`,
                propertyName: 'sitemapCacheDuration',
                propertyValue: formik.values.sitemapCacheDuration
            });
        }
    });

    const onOpenSitemapXMLButtonClick = url => {
        window.open(url, '_blank');
    };

    const handleSnackBarClose = () => {
        setSnackbarIsOpen(false);
    };

    if (loading) {
        return 'Loading ...';
    }

    if (error) {
        return 'There was an error reading sitemap configuration(s)/properties';
    }

    return (
        <form onSubmit={formik.handleSubmit}>
            <main className={classnames(styles.main, 'flexCol')}>
                <SitemapPanelHeaderComponent
                    formik={formik}
                    isSitemapMixinEnabled={sitemapMixinEnabled}
                    siteKey={dxContext.siteKey}
                />
                <div className={classnames(styles.content, 'flexCol')}>
                    <div className={styles.section}>
                        <section>
                            <div className={styles.subsection}>
                                <Typography className={styles.sitemapIndexFileTitle} component="h3">
                                    {t('labels.settingSection.sitemapIndexFileSection.title')}
                                </Typography>
                                <Typography className={styles.sitemapIndexFileDescription} component="p">{t('labels.settingSection.sitemapIndexFileSection.description')}</Typography>
                                <Card>
                                    <div className={styles.sitemapIndexFileCardArea}>
                                        <File className={(!formik.values.sitemapIndexURL || !sitemapMixinEnabled) ? styles.sitemapIndexFileIconDisable : styles.sitemapIndexFileIconEnabled} size="big"/>
                                        <Typography className={(!formik.values.sitemapIndexURL || !sitemapMixinEnabled) ? styles.sitemapIndexFileNameDisabled : styles.sitemapIndexFileNameEnabled} component="p">{t('labels.settingSection.sitemapIndexFileSection.sitemapIndexXML')}</Typography>
                                        <Button className={styles.sitemapIndexFileButton} variant="ghost" icon={<OpenInNew size="big"/>} disabled={!formik.values.sitemapIndexURL || !sitemapMixinEnabled} onClick={() => onOpenSitemapXMLButtonClick(formik.values.sitemapIndexURL)}/>
                                    </div>
                                </Card>
                            </div>
                            <div className={styles.subsection}>
                                <Typography className={styles.sitemapIndexURLTitle} component="h3">
                                    {t('labels.settingSection.sitemapIndexURLSection.title')}
                                    <Chip color="accent" className={styles.sitemapIndexURLChip} label={t('labels.settingSection.sitemapIndexURLSection.chipLabel')}/>
                                </Typography>
                                <Typography className={styles.sitemapIndexURLDescription} component="p">{t('labels.settingSection.sitemapIndexURLSection.description')}</Typography>
                                <Input
                                    required
                                    id="sitemapIndexURL"
                                    name="sitemapIndexURL"
                                    placeholder="http://path/of/my/sitemap-index"
                                    value={formik.values.sitemapIndexURL}
                                    className={styles.sitemapIndexURLInput}
                                    onChange={formik.handleChange}
                                />
                            </div>
                            <div className={styles.subsection}>
                                <Typography className={styles.updateIntervalTitle} component="h3">{t('labels.settingSection.updateIntervalSection.title')}</Typography>
                                <Typography className={styles.updateIntervalDescription} component="p">{t('labels.settingSection.updateIntervalSection.description')}</Typography>

                                <Dropdown
                                    id="intervalDuration"
                                    name="intervalDuration"
                                    isDisabled={false}
                                    variant="outlined"
                                    label={convertCacheDurationToLabel(formik.values.sitemapCacheDuration)}
                                    value={formik.values.sitemapCacheDuration}
                                    data={dropdownData}
                                    onChange={(e, item) => {
                                        // FIXME existing issue with moonstone that uses item not event for the target value
                                        formik.setFieldValue('sitemapCacheDuration', item.value);
                                    }}
                                />
                            </div>
                        </section>
                    </div>
                </div>
                <SnackbarComponent
                    open={snackbarIsOpen}
                    autoHideDuration={2000}
                    message={
                        <div className={styles.snackbarMessageDiv}>
                            <Check className={styles.snackbarMessageCheckIcon}/>
                            <Typography className={styles.snackbarMessageTypography} component="p">
                                {t('labels.snackbar.successActivation')}
                            </Typography>
                        </div>
                    }
                    handleClose={handleSnackBarClose}
                />
            </main>
        </form>
    );
};

SitemapPanelApp.propTypes = {
    client: PropTypes.object.isRequired,
    dxContext: PropTypes.object.isRequired,
    t: PropTypes.func.isRequired
};

export default compose(
    withTranslation('sitemap'),
    withApollo
)(SitemapPanelApp);
