package org.jahia.modules.sitemap.services;

/**
 * Service that send simple notification across cluster nodes
 */
public interface SimpleNotificationService {

    /**
     * Send a notification event to ohter cluster nodes
     */
    public void notifyNodes();

    /**
     * Clean up everything
     */
    public void unregister();

    /**
     * Inner callback interface
     */
    public interface NotificationCallback {
        public void execute();
    }


}
