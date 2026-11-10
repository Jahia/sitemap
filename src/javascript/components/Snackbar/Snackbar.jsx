import React from 'react';
import PropTypes from 'prop-types';
import {Snackbar} from '@material-ui/core';
import {Button, Close} from '@jahia/moonstone';
import {useTranslation} from 'react-i18next';

import styles from './Snackbar.scss';

export const SnackbarComponent = ({
    isOpen,
    message,
    autoHideDuration,
    handleClose
}) => {
    const {t} = useTranslation('sitemap');
    return (
        <>
            <div className={styles.snackbarContainer}>
                <Snackbar
                    classes={{
                        anchorOriginBottomCenter: styles.snackbar
                    }}
                    open={isOpen}
                    autoHideDuration={autoHideDuration}
                    message={message}
                    action={
                        <React.Fragment>
                            <Button className={styles.snackbarMessageButton} variant="ghost" icon={<Close size="big"/>} aria-label={t('labels.snackbar.close')} onClick={handleClose}/>
                        </React.Fragment>
                    }
                    onClose={handleClose}
                />
            </div>
        </>
    );
};

SnackbarComponent.propTypes = {
    isOpen: PropTypes.bool.isRequired,
    message: PropTypes.node.isRequired,
    autoHideDuration: PropTypes.number.isRequired,
    handleClose: PropTypes.func.isRequired
};
