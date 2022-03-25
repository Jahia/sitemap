package org.jahia.modules.sitemap.services.impl;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.jahia.modules.sitemap.services.SimpleNotificationService;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class HazelcastSimpleNotificationServiceImpl implements SimpleNotificationService {

    private static final String TOPIC_NAME = "sitemapTopic";

    private final ServiceTracker<Object, Object> serviceTracker;

    private ITopic<Boolean> topic;
    private String listenerId;

    public HazelcastSimpleNotificationServiceImpl(NotificationCallback callback) {
        BundleContext bundleContext = FrameworkService.getBundleContext();
        // Wait for hazelcast to be available.
        serviceTracker = new ServiceTracker<>(bundleContext, "com.hazelcast.core.HazelcastInstance", new ServiceTrackerCustomizer<Object, Object>() {
            @Override
            public HazelcastInstance addingService(ServiceReference serviceReference) {
                HazelcastInstance hazelcastInstance = (HazelcastInstance) bundleContext.getService(serviceReference);
                topic = hazelcastInstance.getTopic(TOPIC_NAME);
                registerCallback(callback);
                return hazelcastInstance;
            }

            @Override
            public void modifiedService(ServiceReference serviceReference, Object o) {
                if (listenerId != null) {
                    topic.removeMessageListener(listenerId);
                }
                HazelcastInstance hazelcastInstance = (HazelcastInstance) bundleContext.getService(serviceReference);
                topic = hazelcastInstance.getTopic(TOPIC_NAME);
                registerCallback(callback);
            }

            @Override
            public void removedService(ServiceReference serviceReference, Object o) {
                topic.removeMessageListener(listenerId);
            }
        });
        serviceTracker.open();
    }

    private void registerCallback(NotificationCallback callback) {
        listenerId = topic.addMessageListener(message -> {
            if (message.getMessageObject()) {
                callback.execute();
            }
        });

    }

    @Override
    public void notifyNodes() {
        if (topic == null) {
            return;
        }
        topic.publish(true);
    }

    @Override
    public void unregister() {
        if (listenerId != null) {
            topic.removeMessageListener(listenerId);
        }
        serviceTracker.close();
    }
}
