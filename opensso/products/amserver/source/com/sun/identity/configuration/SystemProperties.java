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
 * $Id: SystemProperties.java,v 1.6 2008/10/04 03:32:55 hengming Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.configuration;

import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.configuration.ISystemProperties;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * This is the adapter class for <code>amserver</code> to the shared library.
 * Mainly to provide system configuration information.
  */
public class SystemProperties implements ISystemProperties {
    
    /**
     * Creates a new instance of <code>SystemProperties</code>
     */
    public SystemProperties() {
    }
    
    /**
     * Returns system properties.
     *
     * @param key Key to the properties.
     */
    public String get(String key) {
        return com.iplanet.am.util.SystemProperties.get(key);
    }
    
    /**
     * Returns server list.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    public Collection getServerList()
        throws Exception {
        return WebtopNaming.getPlatformServerList(false);
    }


    /**
     * Returns server all urls.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    public Collection getServiceAllURLs(String serviceName) 
        throws Exception {
        return new ArrayList(WebtopNaming.getServiceAllURLs(serviceName));
    }

    
    /**
     * Returns the URL of the specified service on the specified host.
     *
     * @param serviceName The name of the service.
     * @param protocol The service protocol.
     * @param hostname The service host name.
     * @param port The service listening port.
     * @param uri The service deployment URI.
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
        return WebtopNaming.getServiceURL(serviceName, protocol, hostname,
            Integer.toString(port), uri);
    }

    /**
     * Initializes the properties map.
     *
     * @param properties Map of new properties.
     */
    public void initializeProperties(Properties properties) {
        com.iplanet.am.util.SystemProperties.initializeProperties(properties);
    }

    /**
     * Initializes the properties map.
     *
     * @param propertyName Name of properties.
     * @param propertyValue Value of properties.
     */
    public void initializeProperties(String propertyName, String propertyValue){
        com.iplanet.am.util.SystemProperties.initializeProperties(
            propertyName, propertyValue);
    }
}
