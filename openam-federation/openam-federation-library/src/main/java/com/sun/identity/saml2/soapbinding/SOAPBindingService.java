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
 * $Id: SOAPBindingService.java,v 1.4 2008/08/06 17:28:19 exu Exp $
 *
 */


package com.sun.identity.saml2.soapbinding; 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;


/**
 * This class provides methods to retrieve SOAP configuration
 * from the data store. 
 */
public class SOAPBindingService implements ConfigurationListener {
    static final String CONFIG_NAME = "SAML2_SOAP_BINDING";
    static final String SERVICE_NAME = "sunfmSAML2SOAPBindingService";
    static final String REQUEST_HANDLER_LIST_ATTR="sunSAML2RequestHandlerList";
    static Debug debug = Debug.getInstance("libSAML2");
    static ConfigurationInstance ci = null;

    static HashMap handlers = new HashMap();

    static {
        try {
            ci = ConfigurationManager.getConfigurationInstance(CONFIG_NAME);
            ci.addListener(new SOAPBindingService());
            setValues();
        } catch (ConfigurationException ce) {
             debug.error("SOAPBindingService.static:", ce);
        }
    }

    /**
     * Default Constructor.
     */
    private SOAPBindingService() {
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
            debug.message("SOAPBindingService: configChanged");
        }
        setValues();
    }

    /**
     * This method reads values from service schema.
     */
    static private void setValues() {
        String classMethod = "SOAPBindingService.setValues:";
        Map attrMap = null;
        try {
            attrMap = ci.getConfiguration(null, null);
        } catch (ConfigurationException ce) {
            debug.error(classMethod, ce);
            return;
        }
        handlers.clear();
        Set values = (Set)attrMap.get(REQUEST_HANDLER_LIST_ATTR);
        for (Iterator iter = values.iterator(); iter.hasNext();) {
            String value = (String)iter.next();
            StringTokenizer stz = new StringTokenizer(value, "|");
            String key = null;
            String handlerClass = null;
            while(stz.hasMoreTokens()) {
                String token = stz.nextToken();
                if (token.startsWith("key=")) {
                    key = token.substring(4);
                } else if (token.startsWith("class=")) {
                    handlerClass = token.substring(6);
                } 
            }
            if (key != null && handlerClass != null) {
                try {
                    handlers.put(key,Class.forName(handlerClass).newInstance());
                } catch (Throwable t) {
                    debug.error(classMethod+ "class="+handlerClass,t);
                }
            } else {
                if (debug.warningEnabled()) {
                    debug.warning(classMethod + "Invalid syntax for " +
                        "Request Handler List: " + value);
                }
            }
        }
    }
}
