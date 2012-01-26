/**
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
 * $Id: SystemPropertiesManager.java,v 1.3 2008/06/25 05:53:01 qcheng Exp $
 *
 */

package com.sun.identity.shared.configuration;

import java.util.Properties;

/**
 * This manages the system configuration class. The order for locating
 * the implementation class for ISystemProperties is
 * <ol>
 * <li>Instantiate the class that is defined in System parameter,
 *     <code>com.sun.identity.systemconfiguration</code> e.g.
 *     <code>java -D"com.sun.identity.systemconfiguration=mypkg.myconfig"</code>
 *     </li>
 * <li>Instantiate <code>com.sun.identity.common.SystemConfigurationUtil</code>
 * <li>Instantiate <code>com.sun.identity.configuration.SystemProperties</code>
 * </ol>
 */
public final class SystemPropertiesManager {
    private static ISystemProperties systemProperties;
    private static final String DEBUG_SYSTEM_CONFIG =
        "systempropertiesmanager";
    private static final String PARAM_SYS_CONFIG =
        "com.sun.identity.systemconfiguration";
    
    static {
        if (instantiateDefinedProvider()) {
            // may be that PARAM_SYS_CONFIG is not defined
            if (systemProperties == null) {
                if (instantiateProvider(
                    "com.sun.identity.configuration.FedSystemProperties") &&
                    (systemProperties == null)
                ) {
                    // may be it is not a federation setup.
                    if (instantiateProvider(
                        "com.sun.identity.configuration.SystemProperties") &&
                        (systemProperties == null)
                    ) {
                        instantiateProvider(
                      "com.sun.identity.configuration.FedLibSystemProperties");
                    }
                }
            }
        }
    }
    
    
    private static boolean instantiateDefinedProvider() {
        boolean succeeded = true;
        String param = System.getProperty(PARAM_SYS_CONFIG);
        if (param != null) {
            try {
                Class clazz = Class.forName(param);
                systemProperties = (ISystemProperties)clazz.newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                succeeded = false;
            } catch (InstantiationException e) {
                e.printStackTrace();
                succeeded = false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                succeeded = false;
            } catch (ClassCastException e) {
                e.printStackTrace();
                succeeded = false;
            }
        }
        return succeeded;
    }
    
    private static boolean instantiateProvider(String providerClassName) {
        boolean succeeded = false;
        try {
            Class clazz = Class.forName(providerClassName);
            systemProperties = (ISystemProperties)clazz.newInstance();
            succeeded = true;
        } catch (ClassNotFoundException e) {
            succeeded = true;
            // ok if it is not found.
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return succeeded;
    }
    
    private SystemPropertiesManager() {
    }
    
    /**
     * Returns system properties implementation class.
     *
     * @return system properties implementation class.
     */
    public static ISystemProperties getSystemProperties() {
        return systemProperties;
    }
    
    /**
     * Returns property string.
     *
     * @param key Key of the property.
     * @return property string.
     */
    public static String get(String key) {
        return (systemProperties != null) ? systemProperties.get(key) : null;
    }
    
    /**
     * Returns property string.
     *
     * @param key Key of the property.
     * @param defaultValue Default value if the property is not found.
     * @return property string.
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return ((value != null) && (value.trim().length() > 0)) 
            ? value : defaultValue;
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
    public static void initializeProperties(Properties properties) {
        if (systemProperties != null) {
            systemProperties.initializeProperties(properties);
        }
    }

    /**
     * Initializes the properties map.
     *
     * @param propertyName Name of properties.
     * @param propertyValue Value of properties.
     */
    public static void initializeProperties(
        String propertyName,
        String propertyValue
    ) {
        if (systemProperties != null) {
            systemProperties.initializeProperties(propertyName, propertyValue);
        }
    }
}
