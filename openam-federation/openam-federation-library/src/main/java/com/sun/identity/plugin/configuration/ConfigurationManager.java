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
 * $Id: ConfigurationManager.java,v 1.3 2009/10/28 23:58:57 exu Exp $
 *
 */

package com.sun.identity.plugin.configuration;

import com.sun.identity.common.SystemConfigurationUtil;

/**
 * An <code>ConfigurationManager</code> provides a method get configuration
 * instances. 
 */
public final class ConfigurationManager {   

    private final static String PROP_CONFIG_IMPL_CLASS =
        "com.sun.identity.plugin.configuration.class";
    private final static String RESOURCE_BUNDLE = "libConfigurationManager";

    private static String configClass =
        SystemConfigurationUtil.getProperty(PROP_CONFIG_IMPL_CLASS);

    /**
     * Gets a configuration instance.
     * @param componentName Name of the components, e.g. SAML1, SAML2, ID-FF
     * @return a configuration instance
     */
    public static ConfigurationInstance getConfigurationInstance(
        String componentName) throws ConfigurationException {
        return getConfigurationInstance(componentName, null);
    }

    /**
     * Gets a configuration instance.
     * @param componentName Name of the components, e.g. SAML1, SAML2, ID-FF
     * @return a configuration instance
     */
    public static ConfigurationInstance getConfigurationInstance(
        String componentName,
        Object callerToken 
    ) throws ConfigurationException {
        try {
            if (configClass == null) {
                return null;
            }
            ConfigurationInstance config = (ConfigurationInstance)
                Class.forName(configClass).newInstance();
            config.init(componentName, callerToken);
            return config;
        } catch (IllegalAccessException iae) {
            Object[] objs = { configClass };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "FailedCreatingConfigInstance", objs);
        } catch (InstantiationException ie) {
            Object[] objs = { configClass };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "FailedCreatingConfigInstance", objs);
        } catch (ClassNotFoundException cnfe) {
            Object[] objs = { configClass };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "ConfigClassNotFound", objs);
        }
    }
}

