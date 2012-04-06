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
 * $Id: ISystemProperties.java,v 1.5 2008/06/25 05:53:00 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.shared.configuration;

import java.net.URL;
import java.util.Collection;
import java.util.Properties;

/**
 * This interface provides method to get System configuration information.
 */
public interface ISystemProperties {
    /**
     * Returns system properties.
     *
     * @param key Key to the properties.
     */
    String get(String key);
    
    /**
     * Returns server list.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    Collection getServerList()
        throws Exception;

    /**
     * Returns server all urls.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    Collection getServiceAllURLs(String serviceName)
        throws Exception;
    
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
    URL getServiceURL(
        String serviceName, 
        String protocol,
        String hostname,
        int port,
        String uri)
        throws Exception;

    /**
     * Initializes the properties map.
     *
     * @param properties Map of new properties.
     */
    void initializeProperties(Properties properties);

    /**
     * Initializes the properties map.
     *
     * @param propertyName Name of properties.
     * @param propertyValue Value of properties.
     */
    void initializeProperties(String propertyName, String propertyValue);
}
