/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted 2012-2016 ForgeRock AS.
 */

package com.sun.identity.common.configuration;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.AttributeStruct;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.forgerock.guava.common.base.Predicate;
import org.forgerock.util.Pair;

/**
 * This class listens to changes in configuration changes
 */
public class ConfigurationObserver implements ServiceListener {
    private static final ConfigurationObserver instance = new ConfigurationObserver();
    private static int PARENT_LEN = ConfigurationBase.CONFIG_SERVERS.length() + 2;
    private Set<String> migratedServiceNames = new HashSet<>();
    private Set<Pair<ConfigurationListener, Predicate<String>>> serviceListeners = new HashSet<>();
    private static boolean hasRegisteredListeners = false;

    private ConfigurationObserver() {
        //always add PlatformService to the "monitored" services list, as this class makes sure that the
        //PlatformService related changes are properly propagated to different components that are involved with
        //server configuration in any way
        //TODO: remove this hack, and create a listener explicitely made for PlatformService changes, and/or
        //review serverAttributeMap.properties altogether
        migratedServiceNames.add(Constants.SVC_NAME_PLATFORM);
        createAttributeMapping();
    }
    
    private void createAttributeMapping() {
        // this does not apply client mode because client's property
        // never get store in SMS services
        if (SystemProperties.isServerMode()) {
            Map attributeMap = SystemProperties.getAttributeMap();
            for (Iterator i = attributeMap.values().iterator(); i.hasNext(); ) {
                AttributeStruct a = (AttributeStruct)i.next();
                migratedServiceNames.add(a.getServiceName());
            }
        }
    }
    
    private synchronized void registerListeners() {
        if (!hasRegisteredListeners) {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            for (String serviceName : migratedServiceNames) {
                try {
                    ServiceConfigManager scm = new ServiceConfigManager(serviceName, adminToken);
                    scm.addListener(this);
                } catch (SSOException | SMSException ex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).error("ConfigurationObserver.registeringListeners", ex);
                }
            }
            hasRegisteredListeners = true;
        }
    }
    
    /**
     * Returns an instance of <code>ConfigurationObserver</code>.
     *
     * @return an instance of <code>ConfigurationObserver</code>.
     */    
    public static ConfigurationObserver getInstance() {
        instance.registerListeners();
        return instance;
    }

    /**
     * This method will be invoked when a service's schema has been changed.
     * 
     * @param serviceName Name of the service.
     * @param version Version of the service.
     */
    public void schemaChanged(String serviceName, String version) {
        //no-op
    }
    
    /**
     * This method will be invoked when a service's global configuration data
     * has been changed. The parameter <code>groupName</code> denote the name
     * of the configuration grouping (e.g. default) and
     * <code>serviceComponent</code> denotes the service's sub-component that
     * changed (e.g. <code>/NamedPolicy</code>, <code>/Templates</code>).
     * 
     * @param serviceName Name of the service.
     * @param version Version of the service.
     * @param groupName Name of the configuration grouping.
     * @param serviceComponent Name of the service components that changed.
     * @param type change type, i.e., ADDED, REMOVED or MODIFIED.
     */
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
            int type) {
        if (serviceName.equals(Constants.SVC_NAME_PLATFORM)) {
            if (serviceComponent.startsWith("/" + ConfigurationBase.CONFIG_SERVERS + "/")) {
                String serverName = serviceComponent.substring(PARENT_LEN);

                if (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)
                        || serverName.equals(SystemProperties.getServerInstanceName())) {
                    // always use the server instance name with initialising properties
                    if (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)) {
                        serverName = SystemProperties.getServerInstanceName();
                    }

                    SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
                    try {
                        Properties newProp = ServerConfiguration.getServerInstance(adminToken, serverName);
                        SystemProperties.initializeProperties(newProp, true, true);
                        notifies(Constants.SVC_NAME_PLATFORM);
                    } catch (SSOException | IOException | SMSException ex) {
                        // ignored
                    }
                }
            } else {
                notifies(Constants.SVC_NAME_PLATFORM);
            }
        } else {
            /* need to do this else bcoz some of the property have been moved
             * to services
             */
            notifies(serviceName);
        }
    }

    /**
     * Adds listener for all services.
     *
     * @param l Listener to be added.
     */
    public void addListener(ConfigurationListener l) {
        serviceListeners.add(Pair.<ConfigurationListener, Predicate<String>>of(l, null));
    }

    /**
     * Adds listeners for service names that match the provided predicate.
     *
     * @param l Listener to be added.
     * @param servicePredicate The predicate.
     */
    public void addServiceListener(ConfigurationListener l, Predicate<String> servicePredicate) {
        serviceListeners.add(Pair.of(l, servicePredicate));
    }

    private void notifies(String serviceName) {
        for (Pair<ConfigurationListener, Predicate<String>> listenerPair : serviceListeners) {
            if (listenerPair.getSecond() == null || listenerPair.getSecond().apply(serviceName)) {
                listenerPair.getFirst().notifyChanges();
            }
        }
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
            String serviceComponent, int type) {
        // no-op
     }
}
