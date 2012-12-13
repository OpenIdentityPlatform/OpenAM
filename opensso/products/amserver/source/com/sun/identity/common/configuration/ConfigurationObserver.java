/**
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
 * $Id: ConfigurationObserver.java,v 1.5 2008/06/25 05:42:27 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
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

/**
 * This class listens to changes in configuration changes
 */
public class ConfigurationObserver implements ServiceListener {
    private static ConfigurationObserver instance = new ConfigurationObserver();
    private static int PARENT_LEN = ConfigurationBase.CONFIG_SERVERS.length()
    + 2;
    private Set migratedServiceNames = new HashSet();
    private Set listeners = new HashSet();
    private static boolean hasRegisteredListeners;
    
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
        if (SystemProperties.isServerMode() &&
            !ServerConfiguration.isLegacy()
        ) {
            Map attributeMap = SystemProperties.getAttributeMap();
            for (Iterator i = attributeMap.values().iterator(); i.hasNext(); ) {
                AttributeStruct a = (AttributeStruct)i.next();
                migratedServiceNames.add(a.getServiceName());
            }
        }
    }
    
    private synchronized void registerListeners() {
        if (!hasRegisteredListeners) {
            SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            for (Iterator i = migratedServiceNames.iterator(); i.hasNext(); ) {
                try {
                    ServiceConfigManager scm = new ServiceConfigManager(
                        (String)i.next(), adminToken);
                    scm.addListener(instance);
                } catch (SSOException ex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "ConfigurationObserver.registeringListeners", ex);
                } catch (SMSException ex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "ConfigurationObserver.registeringListeners", ex);
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
    public void globalConfigChanged(
        String serviceName, 
        String version,
        String groupName, 
        String serviceComponent, 
        int type
    ) {
        if (serviceName.equals(Constants.SVC_NAME_PLATFORM)) {
            if (serviceComponent.startsWith("/" +
                    ConfigurationBase.CONFIG_SERVERS + "/")) {
                String serverName = serviceComponent.substring(PARENT_LEN);

                if (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)
                    ||
                    serverName.equals(SystemProperties.getServerInstanceName())
                 ) {
                    // always use the server instance name with initialising properties
                    if (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)) {
                        serverName = SystemProperties.getServerInstanceName();
                    }

                    SSOToken adminToken = (SSOToken)
                        AccessController.doPrivileged(
                            AdminTokenAction.getInstance());
                    try {
                        Properties newProp = 
                            ServerConfiguration.getServerInstance(
                                adminToken, serverName);
                        SystemProperties.initializeProperties(
                            newProp, true, true);
                        notifies();
                    } catch (SSOException ex) {
                    //ingored
                    } catch (IOException ex) {
                    //ingored
                    } catch (SMSException ex) {
                    //ingored
                    }
                }
            } else {
                notifies();
            }
        } else {
            /* need to do this else bcoz some of the property have been moved
             * to services
             */
            notifies();
        }
    }
    
    /**
     * Adds listeners
     *
     * @param l Listener to be added
     */
    public void addListener(ConfigurationListener l) {
        listeners.add(l);
    }

    private void notifies() {
        for (Iterator i = listeners.iterator(); i.hasNext(); ) {
            ConfigurationListener l = (ConfigurationListener)i.next();
            l.notifyChanges();
        }
    }

    /**
     * This method will be invoked when a service's organization configuration
     * data has been changed. The parameters <code>orgName</code>,
     * <code>groupName</code> and <code>serviceComponent</code> denotes the
     * organization name, configuration grouping name and service's
     * sub-component that are changed respectively.
     * 
     * @param serviceName Name of the service.
     * @param version Version of the service.
     * @param orgName Organization name as DN.
     * @param groupName Name of the configuration grouping
     * @param serviceComponent Name of the service components that changed.
     * @param type Change type, i.e., ADDED, REMOVED or MODIFIED
     */
    public void organizationConfigChanged(
        String serviceName, 
        String version,
        String orgName, 
        String groupName, 
        String serviceComponent,
        int type
     ) {
        // no-op
     }
}
