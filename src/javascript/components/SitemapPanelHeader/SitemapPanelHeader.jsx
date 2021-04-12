import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {Book, Button, Delete, Header, Save, Upload} from '@jahia/moonstone';
import styles from './SitemapPanelHeader.scss';
import {DialogComponent} from '../Dialog/Dialog';
import {useTranslation} from 'react-i18next';

export const SitemapPanelHeaderComponent = ({
    formik,
    isSitemapMixinEnabled,
    siteKey
}) => {
    const {t} = useTranslation('sitemap');
    const [dialogIsOpen, setDialogIsOpen] = useState(false);
    const [dialogInfo, setDialogInfo] = useState(null);

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

    const onAcademyButtonClick = () => {
        window.open('https://academy.jahia.com/documentation/enduser/jahia/8/advanced-authoring/seo/sitemap', '_blank');
    };

    return (
        <>
            <Header
                className={styles.header}
                title={t('labels.header.title', {siteName: siteKey})}
                mainActions={[
                    <Button key="submitButton"
                            color="accent"
                            icon={<Save/>}
                            label={(isSitemapMixinEnabled) ? t('labels.header.save') : t('labels.header.activate')}
                            size="big"
                            disabled={formik.values.sitemapIndexURL === '' || !formik.dirty}
                            type="submit"
                            onClick={() => {}}
                    />
                ]}
                toolbarLeft={[
                    <Button key="flushCacheButton"
                            variant="ghost"
                            label={t('labels.header.flushCacheButtonLabel')}
                            icon={<Delete/>}
                            disabled={formik.values.sitemapIndexURL === '' || !isSitemapMixinEnabled}
                            onClick={() => handleDialogOpen(t('labels.dialog.flushCache.title'), t('labels.dialog.flushCache.description'), t('labels.dialog.flushCache.buttonFlushCacheText'))}/>,
                    <Button key="submitToGoogleButton"
                            variant="ghost"
                            label={t('labels.header.submitToGoogleButtonLabel')}
                            icon={<Upload/>}
                            disabled={formik.values.sitemapIndexURL === '' || !isSitemapMixinEnabled}
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
        </>
    );
};

SitemapPanelHeaderComponent.propTypes = {
    formik: PropTypes.object.isRequired,
    isSitemapMixinEnabled: PropTypes.bool.isRequired,
    siteKey: PropTypes.string.isRequired
};
