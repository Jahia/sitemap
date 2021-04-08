import React from 'react';
import PropTypes from 'prop-types';
import {Snackbar} from '@material-ui/core';
import {Button, Close} from '@jahia/moonstone';

import styles from './Snackbar.scss';

export const SnackbarComponent = ({
    open,
    message,
    autoHideDuration,
    handleClose
}) => {
    return (
        <>
            <div className={styles.snackbarContainer}>
                <Snackbar
                    classes={{
                        anchorOriginBottomCenter: styles.snackbar
                    }}
                    open={open}
                    autoHideDuration={autoHideDuration}
                    message={message}
                    action={
                        <React.Fragment>
                            <Button className={styles.snackbarMessageButton} variant="ghost" icon={<Close size="big"/>} onClick={handleClose}/>
                        </React.Fragment>
                    }
                    onClose={handleClose}
                />
            </div>
        </>
    );
};

SnackbarComponent.propTypes = {
    open: PropTypes.func.isRequired,
    message: PropTypes.node.isRequired,
    autoHideDuration: PropTypes.number.isRequired,
    handleClose: PropTypes.func.isRequired
};
