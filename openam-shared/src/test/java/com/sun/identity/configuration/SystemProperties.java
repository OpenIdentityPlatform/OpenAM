/**
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
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.configuration;


import com.sun.identity.shared.configuration.ISystemProperties;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Due to the hidden dependency in "com.sun.identity.shared.configuration.SystemPropertiesManager",
 * this class substitute the one in openam-core.
 * This class will be loaded and used to store the system properties.
 */
public class SystemProperties implements ISystemProperties {

    private Map<String, String> propertiesMap = new HashMap<String, String>();

    public String get(String key) {
        return propertiesMap.get(key);
    }

    public Collection getServerList() throws Exception {
        return null;
    }

    public Collection getServiceAllURLs(String serviceName) throws Exception {
        return null;
    }

    public URL getServiceURL(String serviceName, String protocol, String hostname, int port,
                             String uri) throws Exception {
        return null;
    }

    public void initializeProperties(Properties properties) {

    }

    public void initializeProperties(String propertyName, String propertyValue) {
        propertiesMap.put(propertyName, propertyValue);
    }
}
