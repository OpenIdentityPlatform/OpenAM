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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.federation.common;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.configuration.FedLibSystemProperties;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Installs the deployment's configured identity - this server's URL and the platform server
 * list - through the stores production reads it from, and puts both back afterwards.
 * <p>
 * The server URL goes into the {@code FedLibSystemProperties} store that surefire selects for
 * this module; the server list goes into {@code SystemConfigurationUtil}, which otherwise tries
 * to read it from a naming service that does not exist under test. Both are process-wide, so
 * callers hold {@link FederationStaticsLock}.
 */
public final class DeploymentIdentityFixture {

    private static Properties savedProperties;
    private static boolean propertiesSaved;
    private static Object savedServerList;
    private static Object savedSiteList;
    private static Object savedNamingInitialized;
    private static Object savedConfigClass;
    private static Object savedPlatformConfig;
    private static boolean serverListSaved;

    private DeploymentIdentityFixture() {
    }

    /** Sets this server's configured protocol, host, port and deployment URI. */
    public static void installServer(String protocol, String host, String port, String uri)
        throws Exception {

        if (!(SystemPropertiesManager.getSystemProperties() instanceof FedLibSystemProperties)) {
            throw new IllegalStateException("expected FedLibSystemProperties, found "
                + SystemPropertiesManager.getSystemProperties());
        }
        if (!propertiesSaved) {
            savedProperties = (Properties) propertiesField().get(null);
            propertiesSaved = true;
        }
        Properties properties = new Properties();
        properties.setProperty(Constants.AM_SERVER_PROTOCOL, protocol);
        properties.setProperty(Constants.AM_SERVER_HOST, host);
        properties.setProperty(Constants.AM_SERVER_PORT, port);
        properties.setProperty(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR, uri);
        SystemPropertiesManager.initializeProperties(properties);
    }

    /**
     * Sets the platform server list, in the lower cased form {@code SystemConfigurationUtil}
     * stores it in, for a platform with no sites. <code>null</code> is the state it is in when
     * the naming service could not be read.
     */
    public static void installServerList(String... urls) throws Exception {
        saveServerList();
        serverListField().set(null, lowerCased(urls));
        siteListField().set(null, null);
        namingInitializedField().set(null, Boolean.TRUE);
    }

    private static List<String> lowerCased(String... urls) {
        if (urls == null) {
            return null;
        }
        List<String> list = new ArrayList<String>();
        for (String url : urls) {
            list.add(url.toLowerCase());
        }
        return list;
    }

    /**
     * Sets the platform site list. {@code SystemConfigurationUtil} also carries every site in
     * the server list, so a caller wanting the state it loads puts them in both - this call
     * after {@link #installServerList}, which clears the sites.
     */
    public static void installSiteList(String... urls) throws Exception {
        saveServerList();
        siteListField().set(null, lowerCased(urls));
        namingInitializedField().set(null, Boolean.TRUE);
    }

    /**
     * Leaves the platform server list to be read on first use with no configuration plugin
     * named, as in the library outside the war: {@code SystemConfigurationUtil} then fails on
     * every read with a <code>NullPointerException</code>, and clears its platform configuration
     * on the way.
     */
    public static void installUnreadableServerList() throws Exception {
        saveServerList();
        serverListField().set(null, null);
        namingInitializedField().set(null, Boolean.FALSE);
        configClassField().set(null, null);
    }

    private static void saveServerList() throws Exception {
        if (!serverListSaved) {
            savedServerList = serverListField().get(null);
            savedSiteList = siteListField().get(null);
            savedNamingInitialized = namingInitializedField().get(null);
            savedConfigClass = configClassField().get(null);
            savedPlatformConfig = platformConfigField().get(null);
            serverListSaved = true;
        }
    }

    /** Puts back whatever was installed before. Safe to call when nothing was. */
    public static void restore() throws Exception {
        if (propertiesSaved) {
            propertiesField().set(null, savedProperties);
            propertiesSaved = false;
        }
        if (serverListSaved) {
            serverListField().set(null, savedServerList);
            siteListField().set(null, savedSiteList);
            namingInitializedField().set(null, savedNamingInitialized);
            configClassField().set(null, savedConfigClass);
            platformConfigField().set(null, savedPlatformConfig);
            serverListSaved = false;
        }
    }

    private static Field propertiesField() throws Exception {
        return accessible(FedLibSystemProperties.class.getDeclaredField("systemConfigProps"));
    }

    private static Field serverListField() throws Exception {
        return accessible(SystemConfigurationUtil.class.getDeclaredField("serverList"));
    }

    private static Field siteListField() throws Exception {
        return accessible(SystemConfigurationUtil.class.getDeclaredField("siteList"));
    }

    private static Field namingInitializedField() throws Exception {
        return accessible(
            SystemConfigurationUtil.class.getDeclaredField("platformNamingInitialized"));
    }

    private static Field configClassField() throws Exception {
        return accessible(ConfigurationManager.class.getDeclaredField("configClass"));
    }

    private static Field platformConfigField() throws Exception {
        return accessible(SystemConfigurationUtil.class.getDeclaredField("platformConfig"));
    }

    private static Field accessible(Field field) {
        field.setAccessible(true);
        return field;
    }
}
