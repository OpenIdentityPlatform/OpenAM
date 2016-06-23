/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2016 ForgeRock AS.
*/

package org.forgerock.openam.services.push;

import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceListener;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.MessageDispatcherFactory;
import org.forgerock.openam.services.push.dispatch.Predicate;

/**
 * The PushNotificationManager holds a map of instantiated PushNotificationDelegates to their realm in which they exist.
 * The PushNotificationManager produces PushNotificationDelegates in accordance with the PushNotificationDelegate
 * interface, using a provided PushNotificationDelegateFactory per realm's instantiated service.
 *
 * PushNotificationDelegateFactories are stored in a cache, so as to only load each factory class once.
 *
 * Creating a service config will not update the PushNotificationService unless it has been previously instantiated
 * by another component in the system. Therefore each time the service is called upon to perform a task such as
 * sending a message it checks that there exists a delegate to handle that request.
 *
 * If no delegate has been configured, the service will attempt to load the config for that realm before accessing
 * the delegate instance. Updating the service config via the service interface (after the service has been
 * instantiated) also causes the attempt to load the config & mint a delegate.
 *
 * Later changes in the config will update the delegateFactory and depending upon the delegate's implementation of
 * the isRequireNewDelegate(PushNotificationServiceConfig) method may require the generation of a new delegate. If
 * no new delegate is required, it may be appropriate to update the existing delegate's (non-connection-relevant)
 * configuration parameters. It may also be appropriate to leave the delegate exactly as it was if no configuration
 * option has been altered -- there's no point in tearing down the listeners and services in the case we're
 * re-creating the same delegate.
 */
@Singleton
public class PushNotificationService {

    /** Holds a map of the current PushNotificationDelegate for a given realm. */
    private final ConcurrentMap<String, PushNotificationDelegate> pushRealmMap;

    /** Holds a cache of all pushNotificationDelegateFactories we have used for quick loading. */
    private final ConcurrentMap<String, PushNotificationDelegateFactory> pushFactoryMap;

    private final Debug debug;

    private final PushNotificationServiceConfigHelperFactory configHelperFactory;

    private final PushNotificationDelegateUpdater delegateUpdater;

    private final MessageDispatcherFactory messageDispatcherFactory;

    /**
     * Constructor (called by Guice), registers a listener for this class against all
     * PushNotificationService changes in a realm.
     * @param debug A debugger for logging.
     * @param pushRealmMap Map holding all delegates mapped to the realm in which they belong.
     * @param pushFactoryMap Map holding all factories registered during the lifetime of this service.
     * @param configHelperFactory Produces config helpers for the appropriate realms.
     * @param messageDispatcherFactory Produces MessageDispatchers according to the configured options.
     */
    @Inject
    public PushNotificationService(@Named("frPush") Debug debug,
                                   ConcurrentMap<String, PushNotificationDelegate> pushRealmMap,
                                   ConcurrentMap<String, PushNotificationDelegateFactory> pushFactoryMap,
                                   PushNotificationServiceConfigHelperFactory configHelperFactory,
                                   MessageDispatcherFactory messageDispatcherFactory) {
        this.debug = debug;
        this.pushRealmMap = new ConcurrentHashMap<>(pushRealmMap);
        this.pushFactoryMap = new ConcurrentHashMap<>(pushFactoryMap);
        this.delegateUpdater = new PushNotificationDelegateUpdater();
        this.configHelperFactory = configHelperFactory;
        this.messageDispatcherFactory = messageDispatcherFactory;
    }

    /**
     * Registers the service listener to ensure that updates are caught and responded to.
     */
    public void registerServiceListener() {
        configHelperFactory.addListener(new PushNotificationServiceListener());
    }

    /**
     * Primary method of this class. Used to communicate via the appropriate delegate for this realm out
     * to a Push communication service such as SNS.
     *
     * @param message the message to transmit.
     * @param realm the realm from which to transmit the message.
     * @throws PushNotificationException if there are problems initialising the service or sending the notification.
     */
    public void send(PushMessage message, String realm) throws PushNotificationException {

        PushNotificationDelegate delegate = getDelegateForRealm(realm);

        if (delegate == null) {
            throw new PushNotificationException("No delegate for supplied realm. Check service exists and init has "
                    + "been called.");
        }

        delegate.send(message);
    }

    /**
     * Initializes the PushNotification system for this realm. If the system is already up-to-date and
     * operational in the realm, this call will make no changes.
     *
     * @param realm Realm in which this PushNotification system exists.
     * @throws PushNotificationException If there were issues configuring the Push system.
     */
    public void init(String realm) throws PushNotificationException {
        if (!pushRealmMap.containsKey(realm)) {
            synchronized (pushRealmMap) { //wait here for the thread with first access to update
                if (!pushRealmMap.containsKey(realm)) {
                    updatePreferences(realm);
                    if (!pushRealmMap.containsKey(realm)) {
                        debug.warning("No Push Notification Delegate configured for realm {}", realm);
                        throw new PushNotificationException("No Push Notification Delegate configured for this realm.");
                    }
                }
            }
        }
    }

    /**
     * Returns the relative location of the registration service endpoint in this realm.
     * @param realm The realm of the service to check.
     * @return The relative location of the service.
     * @throws PushNotificationException if the is not service available for that realm.
     */
    public String getRegServiceAddress(String realm) throws PushNotificationException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getRegServiceLocation();
        }

        throw new PushNotificationException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Retrieve the MessageDispatcher message return-route object for this realm's delegate.
     *
     * @param realm The realm whose delegate's MessageDispatcher to return.
     * @return The MessageDispatcher belonging to the delegate associated with the realm requested.
     * @throws NotFoundException If there was no delegate configured for the given realm.
     */
    public MessageDispatcher getMessageDispatcher(String realm) throws NotFoundException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getMessageDispatcher();
        }

        throw new NotFoundException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Returns the relative location of the authentication service endpoint in this realm.
     * @param realm The realm of the service to check.
     * @return The relative location of the service.
     * @throws PushNotificationException if the is not service available for that realm.
     */
    public String getAuthServiceAddress(String realm) throws PushNotificationException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getAuthServiceLocation();
        }

        throw new PushNotificationException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Get the current delegate for the provided realm.
     * @param realm Realm whose delegate you wish to retrieve.
     * @return The current mapping for that realm, or null if one does not exist.
     */
    private PushNotificationDelegate getDelegateForRealm(String realm) {
        return pushRealmMap.get(realm);
    }

    private void updatePreferences(String realm) throws PushNotificationException {
        PushNotificationServiceConfigHelper configHelper = getConfigHelper(realm);
        String factoryClass = configHelper.getFactoryClass();
        validateFactoryExists(factoryClass);
        PushNotificationServiceConfig config = configHelper.getConfig();
        PushNotificationDelegate pushNotificationDelegate = pushFactoryMap.get(factoryClass)
                .produceDelegateFor(config, realm, buildDispatcher(configHelper));

        if (pushNotificationDelegate == null) {
            throw new PushNotificationException("PushNotificationFactory produced a null delegate. Aborting update.");
        }

        delegateUpdater.replaceDelegate(realm, pushNotificationDelegate, config);
        init(realm);
    }

    private MessageDispatcher buildDispatcher(PushNotificationServiceConfigHelper configHelper)
            throws PushNotificationException {
        long maxSize = configHelper.getConfig().getMessageDispatcherSize();
        int concurrency = configHelper.getConfig().getMessageDispatcherConcurrency();
        long duration = configHelper.getConfig().getMessageDispatcherDuration();

        return messageDispatcherFactory.build(maxSize, concurrency, duration, debug);
    }

    private void deleteService(String realm) throws PushNotificationException {

        delegateUpdater.deleteDelegate(realm);

        PushNotificationServiceConfigHelper configHelper = getConfigHelper(realm);
        String factoryClass = configHelper.getFactoryClass();
        validateFactoryExists(factoryClass);
        pushFactoryMap.remove(factoryClass);

    }

    private void validateFactoryExists(String factoryClass) throws PushNotificationException {
        try {
            pushFactoryMap.putIfAbsent(factoryClass, createFactory(factoryClass));
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            debug.error("Unable to instantiate PushNotificationDelegateFactory class.", e);
            throw new PushNotificationException("Unable to instantiate PushNotificationDelegateFactory class.", e);
        }

    }

    private PushNotificationServiceConfigHelper getConfigHelper(String realm) throws PushNotificationException {
        try {
            return configHelperFactory.getConfigHelperFor(realm);
        } catch (SSOException | SMSException e) {
            debug.warning("Unable to read config for PushNotificationServiceConfig in realm {}", realm);
            throw new PushNotificationException("Unable to find config for PushNotificationServiceConfig.", e);
        }

    }

    private PushNotificationDelegateFactory createFactory(String factoryClass)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (PushNotificationDelegateFactory) Class.forName(factoryClass).newInstance();
    }

    /**
     * Retrieve the message predicates (if any) specific to the delegate implementation for the given realm,
     * which must be passed during registration message(s).
     *
     * @param realm The realm in which the service (should) exist.
     * @return A set of predicates necessary for messages returning from that specific service to be valid.
     * @throws PushNotificationException if the service is not available for that realm.
     */
    public Set<Predicate> getRegistrationMessagePredicatesFor(String realm) throws PushNotificationException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getRegistrationMessagePredicates();
        }

        throw new PushNotificationException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Retrieve the message predicates (if any) specific to the delegate implementation for the given realm,
     * which must be passed during authentication message(s).
     *
     * @param realm The realm in which the service (should) exist.
     * @return A set of predicates necessary for messages returning from that specific service to be valid.
     * @throws PushNotificationException if the service is not available for that realm.
     */
    public Set<Predicate> getAuthenticationMessagePredicatesFor(String realm) throws PushNotificationException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getAuthenticationMessagePredicates();
        }

        throw new PushNotificationException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Our service config change listener.
     */
    private final class PushNotificationServiceListener implements ServiceListener {

        /**
         * No-op for this impl.
         */
        @Override
        public void schemaChanged(String serviceName, String version) {
            //This section intentionally left blank
        }

        /**
         * No-op for this impl.
         */
        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName,
                                        String serviceComponent, int type) {
            //This section intentionally left blank
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                              String serviceComponent, int type) {
            switch (type) {
            case ADDED: // OR
            case MODIFIED:
                try {
                    if (SERVICE_NAME.equals(serviceName) && SERVICE_VERSION.equals(version)) {
                        synchronized (pushRealmMap) { //wait here for the thread with first access to update
                            updatePreferences(DNMapper.orgNameToRealmName(orgName));
                        }
                    }
                } catch (PushNotificationException e) {
                    debug.error("Unable to update preferences for organization {}", orgName, e);
                }
                break;
            case REMOVED:
                try {
                    deleteService(DNMapper.orgNameToRealmName(orgName));
                } catch (PushNotificationException e) {
                    debug.error("Unable to update preferences for organization {}", orgName, e);
                }
                break;
            default:
                debug.error("Unknown function requested on to update preferences for organization {}", type);
            }
        }
    }

    /**
     * Our delegate updater.
     */
    @VisibleForTesting
    final class PushNotificationDelegateUpdater {

        void replaceDelegate(String realm, PushNotificationDelegate newDelegate,
                         PushNotificationServiceConfig config) throws PushNotificationException {
            try {
                PushNotificationDelegate oldDelegate = pushRealmMap.get(realm);

                if (oldDelegate == null) {
                    start(realm, newDelegate);
                } else {
                    if (oldDelegate.isRequireNewDelegate(config)) {
                        pushRealmMap.remove(realm);
                        oldDelegate.close();
                        start(realm, newDelegate);
                    } else {
                        oldDelegate.updateDelegate(config);
                    }
                }
            } catch (IOException e) {
                debug.error("Unable to call close on the old delegate having removed it from the realmPushMap.", e);
                throw new PushNotificationException("Error calling close on the previous delegate instance.", e);
            }
        }

        private void start(String realm, PushNotificationDelegate delegate) throws PushNotificationException {
            delegate.startServices();
            pushRealmMap.put(realm, delegate);
        }

        private void deleteDelegate(String realm) throws PushNotificationException {
            PushNotificationDelegate removedDelegate = pushRealmMap.remove(realm);
            try {
                removedDelegate.close();
            } catch (IOException e) {
                throw new PushNotificationException("Error Deleting Service " + SERVICE_NAME + " for realm: " + realm);
            }
        }
    }

}
