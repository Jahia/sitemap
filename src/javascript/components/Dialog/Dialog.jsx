import React from 'react';
import PropTypes from 'prop-types';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

import {useTranslation} from 'react-i18next';

import {Button, Separator} from '@jahia/moonstone';

export const DialogComponent = ({
    isOpen,
    handleClose,
    handleSubmit,
    submitButtonText,
    title,
    subtitle,
    children
}) => {
    const {t} = useTranslation('sitemap');
    return (
        <>
            <Dialog fullWidth maxWidth="md" open={isOpen} onClose={handleClose}>
                <DialogTitle>{title}</DialogTitle>
                <DialogContent>
                    <DialogContentText>{subtitle}</DialogContentText>
                    {children}
                </DialogContent>
                <Separator spacing="none"/>
                <DialogActions>
                    <Button key="cancelButton" data-sel-role="sitemapDialogCancelButton" size="big" label={t('labels.dialog.buttonCancel')} onClick={handleClose}/>
                    <Button key="submitButton" data-sel-role="sitemapDialogSubmitButton" color="accent" size="big" label={submitButtonText} onClick={handleSubmit}/>
                </DialogActions>
            </Dialog>
        </>
    );
};

DialogComponent.propTypes = {
    isOpen: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    handleSubmit: PropTypes.func.isRequired,
    submitButtonText: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    subtitle: PropTypes.string,
    children: PropTypes.element
};
