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
 * $Id: FedSystemProperties.java,v 1.9 2009/06/22 23:26:26 rh221556 Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.configuration;

import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.common.AttributeStruct;
import com.sun.identity.common.PropertiesFinder;
import com.sun.identity.common.SystemConfigurationUtil;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * This is the adapter class for Federation Manager to the shared library.
 * Mainly to provide system configuration information.
  */
public class FedSystemProperties extends FedLibSystemProperties {
    private static Map attributeMap = new HashMap();

    static {
        initAttributeMapping();        
    }
    
    private static void initAttributeMapping() {
        if (systemConfigProps != null) {
            systemConfigProps.clear();
        } 
        ResourceBundle rb = ResourceBundle.getBundle("serverAttributeMap");
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String propertyName = (String)e.nextElement();
            attributeMap.put(propertyName, new AttributeStruct(
                rb.getString(propertyName)));
        }
    }
    
    /**
     * Creates a new instance of <code>FedSystemProperties</code>
     */
    public FedSystemProperties() {
    }
    
    /**
     * Returns system properties.
     *
     * @param key Key to the properties.
     */
    @Override
    public String get(String key) {
        String value = null;
        if (isServerMode()) {                    
           AttributeStruct ast = (AttributeStruct)attributeMap.get(key);
           if (ast != null) {
                value = PropertiesFinder.getProperty(key, ast);
            }
        }        
        return (value != null) ? value : getPropertyValue(key);
       
    }
    
    private boolean isServerMode() {
        return Boolean.valueOf(com.iplanet.am.util.SystemProperties.get(
                Constants.SERVER_MODE)).booleanValue();
        
    }
    
    private String getPropertyValue(String key) {
        String value = super.get(key);
        if ((value != null) && (value.trim().length() > 0)) {
            return value;
        }
        if (key.equals(SystemConfigurationUtil.PROP_SERVER_MODE)) {
            return com.iplanet.am.util.SystemProperties.get(
                Constants.SERVER_MODE);
        } else {
            return com.iplanet.am.util.SystemProperties.get(key);
        }
    }

    /**
     * Returns server list.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    @Override
    public Collection getServerList()
        throws Exception {
        return WebtopNaming.getPlatformServerList(false);
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
    @Override
    public URL getServiceURL(
        String serviceName, 
        String protocol,
        String hostname,
        int port,
        String uri
    ) throws Exception {
        return WebtopNaming.getServiceURL(
            serviceName, protocol, hostname, "" + port, uri);
    }

    /**
     * Returns service all url list.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    @Override
    public Collection getServiceAllURLs(String serviceName)
        throws Exception {
        return WebtopNaming.getServiceAllURLs(serviceName);
    }
}
