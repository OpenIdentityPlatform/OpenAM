/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FedLibSystemProperties.java,v 1.7 2008/06/25 05:46:37 qcheng Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */

package com.sun.identity.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.configuration.ISystemProperties;

/**
 * This is the adapter class for Federation Library to the shared library.
 * Mainly to provide system configuration information.
  */
public class FedLibSystemProperties implements ISystemProperties {
    private static final String SYSTEM_CONFIG_PROPERTY_NAME ="FederationConfig";
    private static final String FEDLET_HOME_TAG = "@FEDLET_HOME@";
    private static final String FEDLET_HOME_DIR = 
        "com.sun.identity.fedlet.home";
    protected static Properties systemConfigProps = null;

    static {
        initProperties();
    }

    /**
     * Creates a new instance of <code>FedLibSystemProperties</code>
     */
    public FedLibSystemProperties() {
    }

    /**
     * Returns system properties.
     *
     * @param key Key to the properties.
     */
    public String get(String key) {
        return systemConfigProps.getProperty(key);
    }

    @Override
    public String getOrDefault(final String key, final String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Initializes the properties to be used by Open Federation Library.
     * Ideally this must be called first before any other method is called
     * within Open Federation Library. This method provides a programmatic way
     * to set the properties, and will override similar properties if loaded
     * for a properties file.
     *
     * @param properties  properties for Open Federation Library
     */
    public void initializeProperties(Properties properties){
        Properties newProps = new Properties();
        newProps.putAll(systemConfigProps);
        newProps.putAll(properties);
        systemConfigProps = newProps;
    }

    private static void initProperties() {
        systemConfigProps = new Properties();
        try {
            ResourceBundle bundle =
                ResourceBundle.getBundle(SYSTEM_CONFIG_PROPERTY_NAME);
            Enumeration e = bundle.getKeys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                systemConfigProps.put(key, bundle.getString(key));
            }
        } catch (MissingResourceException mrex) {
            // check if this is the fedlet case, find fedlet home first
            String fedletHomeDir = System.getProperty(FEDLET_HOME_DIR);
            if ((fedletHomeDir == null) || 
                (fedletHomeDir.trim().length() == 0)) {
                    fedletHomeDir = System.getProperty("user.home") +
                        File.separator + "fedlet";
            }
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fedletHomeDir + File.separator +
                    SYSTEM_CONFIG_PROPERTY_NAME + ".properties");
                Properties props = new Properties();
                props.load(fis);
                Iterator it = props.keySet().iterator();
                while (it.hasNext()) {
                     String key = (String) it.next();
                     String value = (String) props.get(key);
                     int loc = value.indexOf(FEDLET_HOME_TAG);
                     if (loc == 0) {
                         // dynamically replace TAG
                         value = fedletHomeDir.trim() + 
                             value.substring(FEDLET_HOME_TAG.length());
                     } else if (loc != -1) {
                         value = value.substring(0, loc) + 
                             fedletHomeDir.trim() + 
                             value.substring(loc + FEDLET_HOME_TAG.length());
                     }
                     systemConfigProps.put(key, value);
                }
            } catch (MissingResourceException ex) {
            } catch (FileNotFoundException ffe) {
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * Returns server list.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    public Collection getServerList()
        throws Exception {
        return SystemConfigurationUtil.getServerList();
    }

    /**
     * Returns service all url list.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    public Collection getServiceAllURLs(String serviceName)
        throws Exception {
        return SystemConfigurationUtil.getServiceAllURLs(serviceName);
    }
    
    /**
     * Returns the URL of the specified service on the specified host.
     *
     * @param serviceName The name of the service.
     * @param protocol The service protocol.
     * @param hostname The service host name.
     * @param port The service listening port.
     * @param uri The service URI.
     * @return The URL of the specified service on the specified host.
     * @throws Exception if the URL could not be found.
     */
    public URL getServiceURL(
        String serviceName, 
        String protocol,
        String hostname,
        int port,
        String uri
    ) throws Exception {
        return SystemConfigurationUtil.getServiceURL(
            serviceName, protocol, hostname, port, uri);
    }

    /**
     * Initializes the properties map.
     *
     * @param propertyName Name of properties.
     * @param propertyValue Value of properties.
     */
    public void initializeProperties(String propertyName, String propertyValue){
        Properties newProps = new Properties();
        newProps.putAll(systemConfigProps);
        newProps.put(propertyName, propertyValue);
        systemConfigProps = newProps;
    }
}
