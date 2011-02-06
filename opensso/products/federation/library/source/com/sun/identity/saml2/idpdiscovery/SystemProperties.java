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
 * $Id: SystemProperties.java,v 1.5 2008/06/25 05:47:48 qcheng Exp $
 *
 */


package com.sun.identity.saml2.idpdiscovery;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.MissingResourceException;

/** 
 * This class provides functionality that allows single-point-of-access
 * to all related system properties.
 * 
 * The class tries to retrieve IDP discovery related properties in services,
 * if not exists, find a file <code>IDPDiscoveryConfig.properties</code> in
 * the CLASSPATH accessible to this code.
 *
 * If multiple servers are running, each may have their own configuration file.
 * The naming convention for such scenarios is 
 * <code>IDPDiscoveryConfig_serverName</code>.
 */
public class SystemProperties {
    private static Properties properties = new Properties();

    /** 
     * Initializes the properties.
     * @param fileName name of file containing the properties to be initialized.
     */
    public static synchronized void initializeProperties(String fileName) 
        throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        try { 
            if ((fileName != null) && (fileName.length() != 0)) {
                fis = new FileInputStream(fileName);
                properties.load(fis);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
     * Returns system property of a given key. The method will get the property
     * using SystemPropertiesManager first (server mode), if not found,  
     * get it from the locale file (IDP discovery WAR only mode). 
     *
     * @param key the key whose value to be returned.
     * @return the value if the key exists; otherwise returns <code>null</code>.
    */
    public static String get(String key) {
        String val = SystemPropertiesManager.get(key);
        if (val == null) {
            return (String) properties.get(key);
        } else {
            return val;
        }
    }
}
