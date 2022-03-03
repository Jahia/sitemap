import React, {useState, useEffect} from 'react';
import {useSelector} from 'react-redux';
import PropTypes from 'prop-types';
import classnames from 'clsx';
import {Typography, Chip} from '@jahia/moonstone';
import {Dropdown, Input, Check} from '@jahia/moonstone';

import styles from './SitemapPanel.scss';

import * as compose from 'lodash.flowright';
import {withApollo} from 'react-apollo';
import {withTranslation} from 'react-i18next';
import {useQuery} from '@apollo/react-hooks';

import * as gqlMutations from './gqlMutations';
import * as gqlQueries from './gqlQueries';
import {gqlMutate} from '../utils';

import {SnackbarComponent} from './Snackbar/Snackbar';
import {SitemapPanelHeaderComponent} from './SitemapPanelHeader/SitemapPanelHeader';
import {SitemapIndexLink} from './panelSections';
import {useFormik} from 'formik';

const SitemapPanelApp = ({client, t}) => {
    const [sitemapMixinEnabled, setSitemapMixinEnabled] = useState(false);
    const [sitemapIndexURL, setSitemapIndexURL] = useState(null);
    const [sitemapCacheDuration, setSitemapCacheDuration] = useState(null);
    const currentState = useSelector(state => ({site: state.site, language: state.language}));

    const {data, error, loading, refetch} = useQuery(gqlQueries.GetNodeSitemapInfo, {
        variables: {
            pathOrId: `/sites/${currentState.site}`,
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
    const [snackbarInfo, setSnackbarInfo] = useState(null);

    useEffect(() => {
        if (data) {
            if (data?.jcr?.nodeByPath?.mixinTypes?.length > 0) {
                setSitemapMixinEnabled(true);
            } else {
                setSitemapMixinEnabled(false);
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
            } else {
                setSitemapIndexURL(null);
                setSitemapCacheDuration(null);
            }
        }
    }, [data, currentState.site]);

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
            sitemapIndexURL: ((sitemapIndexURL) ? sitemapIndexURL : ''),
            sitemapCacheDuration: ((sitemapCacheDuration) ? sitemapCacheDuration : dropdownData[0].value)
        },
        enableReinitialize: true,
        // eslint-disable-next-line no-unused-vars
        onSubmit: values => {
            if (!sitemapMixinEnabled) {
                gqlMutate(client, gqlMutations.AddMixin, {
                    pathOrId: `/sites/${currentState.site}`,
                    mixins: ['jseomix:sitemap']
                });
                setSitemapMixinEnabled(prevState => !prevState);
                setSnackbarInfo(t('labels.snackbar.successActivation'));
                setSnackbarIsOpen(true);
            }

            gqlMutate(client, gqlMutations.mutateProperty, {
                pathOrId: `/sites/${currentState.site}`,
                propertyName: 'sitemapIndexURL',
                propertyValue: formik.values.sitemapIndexURL
            });
            gqlMutate(client, gqlMutations.mutateProperty, {
                pathOrId: `/sites/${currentState.site}`,
                propertyName: 'sitemapCacheDuration',
                propertyValue: formik.values.sitemapCacheDuration
            });
            refetch();
        }
    });

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
                    siteKey={currentState.site}
                    openSnackBar={setSnackbarIsOpen}
                    snackBarInfo={setSnackbarInfo}
                />
                <div className={classnames(styles.content, 'flexCol')}>
                    <div className={styles.section}>
                        <section>
                            <div className={styles.subsection}>
                                <Typography className={styles.sitemapIndexURLTitle} component="h3">
                                    {t('labels.settingSection.sitemapIndexURLSection.title')}
                                    <Chip color="accent" className={styles.sitemapIndexURLChip} label={t('labels.settingSection.sitemapIndexURLSection.chipLabel')}/>
                                </Typography>
                                <Typography className={styles.sitemapIndexURLDescription} component="p">{t('labels.settingSection.sitemapIndexURLSection.description')}</Typography>
                                <Input
                                    required
                                    id="sitemapIndexURL"
                                    data-sel-role="sitemapIndexURL"
                                    name="sitemapIndexURL"
                                    placeholder="http://your-site-root"
                                    value={formik.values.sitemapIndexURL}
                                    className={styles.sitemapIndexURLInput}
                                    onChange={formik.handleChange}
                                />
                            </div>
                            <div className={styles.subsection}>
                                <SitemapIndexLink
                                    inputUrl={formik.values.sitemapIndexURL}
                                    siteKey={currentState.site}
                                    t={t}/>
                            </div>
                            <div className={styles.subsection}>
                                <Typography className={styles.updateIntervalTitle} component="h3">
                                    {t('labels.settingSection.updateIntervalSection.title')}
                                </Typography>
                                <Typography className={styles.updateIntervalDescription} component="p">
                                    {t('labels.settingSection.updateIntervalSection.description')}
                                </Typography>

                                <Dropdown
                                    id="intervalDuration"
                                    data-sel-role="sitemapIntervalDuration"
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
                    isOpen={snackbarIsOpen}
                    autoHideDuration={2000}
                    message={
                        <div className={styles.snackbarMessageDiv}>
                            <Check className={styles.snackbarMessageCheckIcon}/>
                            <Typography className={styles.snackbarMessageTypography} component="p">
                                {snackbarInfo}
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
    t: PropTypes.func.isRequired
};

export default compose(
    withTranslation('sitemap'),
    withApollo
)(SitemapPanelApp);
