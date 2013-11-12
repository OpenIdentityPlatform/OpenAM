/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAML2ConfigService.java,v 1.6 2009/06/12 22:21:40 mallas Exp $
 *
 */


package com.sun.identity.saml2.common; 

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class provides methods to retrieve SAML2 configuration
 * from the data store. 
 */
public class SAML2ConfigService implements ConfigurationListener {
    static final String CONFIG_NAME = "SAML2_CONFIG";
    static final String SERVICE_NAME = "sunFAMSAML2Configuration";
    static Debug debug = Debug.getInstance("libSAML2");
    static ConfigurationInstance ci = null;
    static final String SAML2_FAILOVER_ATTR = "failOverEnabled"; 
    static final String SAML2_BUFFER_LENGTH = "bufferLength";     
    private static Map attributes = new HashMap();

    static {
        try {
            ci = ConfigurationManager.getConfigurationInstance(CONFIG_NAME);
            SAML2ConfigService saml2ConfigService = new SAML2ConfigService();
            if ( (saml2ConfigService != null) && (ci != null) )
                { ci.addListener(saml2ConfigService); }
            setValues();
        } catch (ConfigurationException ce) {
            debug.error("SAML2ConfigService.static:", ce);
        }
    }

    /**
     * Default Constructor.
     */
    private SAML2ConfigService() {
    }

    /**
     * This method will be invoked when a component's 
     * configuration data has been changed. The parameters componentName,
     * realm and configName denotes the component name,
     * organization and configuration instance name that are changed 
     * respectively.
     *
     * @param e Configuration action event, like ADDED, DELETED, MODIFIED etc.
     */
    public void configChanged(ConfigurationActionEvent e) {
        if (debug.messageEnabled()) {
            debug.message("SAML2ConfigService: configChanged");
        }
        setValues();
    }

    /**
     * This method reads values from service schema.
     */
    static private synchronized void setValues() {
        if (ci == null) {
            attributes.put(SAML2_FAILOVER_ATTR, "false");
            attributes.put(SAML2_BUFFER_LENGTH, "2048");
            debug.warning("ConfigurationInstance is null, so default values for " +
                    "failover (false) and buffer length (2048) will be set.");
        } else {
            Map attrMap = null; 
            try {
               attrMap = ci.getConfiguration(null, null);
            } catch (ConfigurationException ce) {
               debug.error("Exception caught obtaining updated configuration. " + SAML2_FAILOVER_ATTR + " and " +
                       SAML2_BUFFER_LENGTH + " will not be updated. Exception: " + ce, ce);
               return;
            }
            Map newAttributes = new HashMap();
            if (attrMap != null) {
                if (debug.messageEnabled()) {
                    debug.message("The updated configuration: " + attrMap);
                }
                Set values = (Set)attrMap.get(SAML2_FAILOVER_ATTR);
                String value = "false" ;
                if ((values != null) && (values.size() == 1)) {
                     value = (String) values.iterator().next(); 
                } else {
                    debug.warning("Value for " + SAML2_FAILOVER_ATTR + " null or size!=1. Defaulting to false.");
                }
                newAttributes.put(SAML2_FAILOVER_ATTR, value); 
                values = (Set)attrMap.get(SAML2_BUFFER_LENGTH);
                value = "2048" ; 
                if ((values != null) && (values.size() == 1)) {
                     value = (String) values.iterator().next(); 
                } else {
                    debug.warning("Value for " + SAML2_BUFFER_LENGTH + " null or size!=1. Defaulting to 2048.");
                }
                newAttributes.put(SAML2_BUFFER_LENGTH, value);                 
            } else {
                debug.warning("Attribute map returned from ConfigurationInstance for the SAML2 config is null! " +
                        "Default values for failover (false) and buffer length (2048) will be set.");
            }
            attributes = newAttributes;
        } 
        if (debug.messageEnabled()) {
            debug.message("Attributes in SAML2ConfigService updated to: "
                + attributes);
        }
    }
    
    /**
     * Retrieves current value of an AttributeSchema in the SAML2Config
     * ServiceSchema.
     * @param attributeName the name of the attributeSchema.
     * @return the value of the attribute schema. It could return null if
     *      input attibuteName is null, or the attributeName can not be
     *      found in the service schema.
     */
    public static Object getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }  
}
