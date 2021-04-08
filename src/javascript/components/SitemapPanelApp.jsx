import React, {useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import classnames from 'clsx';
import {Header, Typography, Button, Chip} from '@jahia/moonstone';
import {Upload, Book, Dropdown, Input, Delete, Save, File, OpenInNew, Check} from '@jahia/moonstone';

import styles from './SitemapPanel.scss';

import {Card} from '@material-ui/core';

import * as compose from 'lodash.flowright';
import {withApollo} from 'react-apollo';
import {withTranslation} from 'react-i18next';

import * as gqlMutations from './gqlMutations';
import * as gqlQueries from './gqlQueries';
import * as gqlUtilities from '../utils/gqlUtilities';

import {DialogComponent} from './Dialog/Dialog';
import {SnackbarComponent} from './Snackbar/Snackbar';
import {useFormik} from 'formik';

const SitemapPanelApp = ({client, dxContext, t}) => {
    const [sitemapMixinEnabled, setSitemapMixinEnabled] = useState(false);
    const [sitemapIndexURL, setSitemapIndexURL] = useState(null);
    const [sitemapCacheDuration, setSitemapCacheDuration] = useState(null);

    const [dialogIsOpen, setDialogIsOpen] = useState(false);
    const [dialogInfo, setDialogInfo] = useState(null);

    const [snackbarIsOpen, setSnackbarIsOpen] = useState(false);

    const dropdownData = [{
        label: '4 hours',
        value: '4h'
    }, {
        label: '8 hours',
        value: '8h'
    }, {
        label: '24 hours',
        value: '24h'
    }, {
        label: '48 hours',
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

    useEffect(() => {
        gqlUtilities.gqlQuery(client, gqlQueries.GetNodeMixin, {pathOrId: `/sites/${dxContext.siteKey}`,
            mixinsFilter: {
                filters: [
                    {fieldName: 'name', value: 'jseomix:sitemap'}
                ]
            }
        }).then(data => {
            if (data?.data?.jcr?.nodeByPath?.mixinTypes?.length > 0) {
                setSitemapMixinEnabled(true);
            }
        });

        gqlUtilities.gqlQuery(client, gqlQueries.GetProperties, {pathOrId: `/sites/${dxContext.siteKey}`,
            propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration']
        }).then(data => {
            const properties = data?.data?.jcr?.nodeByPath?.properties;
            if (properties.length > 0) {
                properties.forEach(property => {
                    if (property.name === 'sitemapIndexURL') {
                        setSitemapIndexURL(property.value);
                    } else if (property.name === 'sitemapCacheDuration') {
                        setSitemapCacheDuration(property.value);
                    }
                });
            }
        });
    }, [client, dxContext.siteKey]);

    const onAcademyButtonClick = () => {
        window.open('https://academy.jahia.com/documentation/enduser/jahia/8/advanced-authoring/seo/sitemap', '_blank');
    };

    const onOpenSitemapXMLButtonClick = url => {
        window.open(url, '_blank');
    };

    const handleDialogOpen = (title, text, submitText) => {
        setDialogInfo({
            title: title,
            text: text,
            submitText: submitText
        });
        setDialogIsOpen(true);
    };

    const handleDialogClose = () => {
        setDialogInfo(null);
        setDialogIsOpen(false);
    };

    const handleSnackBarClose = () => {
        setSnackbarIsOpen(false);
    };

    return (
        <form onSubmit={formik.handleSubmit}>
            <main className={classnames(styles.main, 'flexCol')}>
                <Header
                    className={styles.header}
                    title={t('labels.header.title', {siteName: dxContext.siteKey})}
                    mainActions={[
                        <Button key="submitButton"
                                color="accent"
                                icon={<Save/>}
                                label={(sitemapMixinEnabled) ? t('labels.header.save') : t('labels.header.activate')}
                                size="big"
                                disabled={!formik.values.sitemapIndexURL || !formik.dirty}
                                type="submit"
                                onClick={() => {}}
                        />
                    ]}
                    toolbarLeft={[
                        <Button key="flushCacheButton"
                                variant="ghost"
                                label={t('labels.header.flushCacheButtonLabel')}
                                icon={<Delete/>}
                                disabled={!formik.values.sitemapIndexURL || !sitemapMixinEnabled}
                                onClick={() => handleDialogOpen(t('labels.dialog.flushCache.title'), t('labels.dialog.flushCache.description'), t('labels.dialog.flushCache.buttonFlushCacheText'))}/>,
                        <Button key="submitToGoogleButton"
                                variant="ghost"
                                label={t('labels.header.submitToGoogleButtonLabel')}
                                icon={<Upload/>}
                                disabled={!formik.values.sitemapIndexURL || !sitemapMixinEnabled}
                                onClick={() => handleDialogOpen(t('labels.dialog.submitToGoogle.title'), t('labels.dialog.submitToGoogle.description'), t('labels.dialog.submitToGoogle.buttonSubmitText'))}/>
                    ]}
                    toolbarRight={[<Button key="academyLinkIcon" variant="ghost" label={t('labels.header.academy')} icon={<Book/>} onClick={onAcademyButtonClick}/>]}
                />
                {dialogInfo !== null &&
                    <DialogComponent
                        isOpen={dialogIsOpen}
                        handleClose={handleDialogClose}
                        handleSubmit={handleDialogClose} // TODO add the action per each dialog
                        title={dialogInfo.title}
                        subtitle={dialogInfo.text}
                        submitButtonText={dialogInfo.submitText}
                    />}

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
