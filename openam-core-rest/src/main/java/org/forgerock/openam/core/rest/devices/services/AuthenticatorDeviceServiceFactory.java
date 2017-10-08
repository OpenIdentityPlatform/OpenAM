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
package org.forgerock.openam.core.rest.devices.services;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Generic implementation of a service factory used to produce services for realms
 * which will cache the service and attach a config listener which will update on
 * config modifications.
 *
 * @param <T> The DeviceService type that this factory will produce.
 */
public class AuthenticatorDeviceServiceFactory<T extends DeviceService> {

    private final ConcurrentMap<String, T> serviceCache = new ConcurrentHashMap<>();
    private final Debug debug;
    private final ServiceConfigManager serviceConfigManager;

    private final DeviceServiceFactory<T> factory;

    /**
     * Generates a new AuthenticatorDeviceServiceFactory, configuring a listener on the provided
     * ServiceConfigManager to listen to changes in its settings.
     *
     * @param debug For writing debug messages.
     * @param serviceConfigManager To communicate with the data store.
     * @param factory For producing DeviceServices.
     */
    public AuthenticatorDeviceServiceFactory(Debug debug, ServiceConfigManager serviceConfigManager,
                                             DeviceServiceFactory<T> factory) {
        this.debug = debug;
        this.factory = factory;
        this.serviceConfigManager = serviceConfigManager;

        if (serviceConfigManager != null) {
            serviceConfigManager.addListener(new AuthenticatorDeviceServiceFactoryServiceListener());
        }
    }

    /**
     * Generate a new instance of the DeviceService for this factory in the given realm.
     *
     * @param realm The realm the new instance will serve.
     * @return A new instance of the service this factory produces.
     * @throws SMSException In case of errors talking to the config store.
     * @throws SSOException In case of permission errors.
     */
    public T create(String realm) throws SMSException, SSOException {

        T service = serviceCache.get(realm);

        if (service == null) {
            service = factory.create(serviceConfigManager, realm);
            serviceCache.putIfAbsent(realm, service);
        }

        return serviceCache.get(realm);
    }

    /**
     * Our service config change listener.
     */
    private final class AuthenticatorDeviceServiceFactoryServiceListener implements ServiceListener {

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
                        serviceCache.put(DNMapper.orgNameToRealmName(orgName),
                                factory.create(serviceConfigManager, orgName));
                    } catch (SMSException | SSOException e) {
                        debug.error("Unknown function requested on to update preferences for organization {}", type);
                    }
                    break;
                case REMOVED:
                    serviceCache.remove(DNMapper.orgNameToRealmName(orgName));
                default:
                    debug.error("Unknown function requested on to update preferences for organization {}", type);
            }
        }
    }

}
