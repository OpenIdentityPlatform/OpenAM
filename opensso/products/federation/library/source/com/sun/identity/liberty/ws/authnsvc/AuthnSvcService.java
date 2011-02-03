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
 * $Id: AuthnSvcService.java,v 1.3 2008/06/25 05:47:06 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.authnsvc; 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.liberty.ws.authnsvc.mechanism.MechanismHandler;
import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;


/**
 * The <code>AuthnSvcService</code> class stores the current values of all the
 * AttributeSchema defined in Authentication service schema. It updates its
 * store by listening to Authentication Service ServiceSchema events.
 */
public class AuthnSvcService implements ConfigurationListener
{
    static final String SERVICE_NAME = "sunIdentityServerAuthnService";
    static final String MECHANISM_HANDLER_LIST_ATTR = "MechanismHandlerList";
    static final String PLAIN_MECHANISM_AUTH_MODULE =
        "PlainMechanismAuthModule";
    static final String CRAMMD5_MECHANISM_AUTH_MODULE =
        "CramMD5MechanismAuthModule";

    static HashMap handlers = new HashMap();
    static String plainMechanismAuthModule = null;
    static String cramMD5MechanismAuthModule = null;

    static ConfigurationInstance ci = null;

    static {
        try {
            ci = ConfigurationManager.getConfigurationInstance("AUTHN_SVC");
            ci.addListener(new AuthnSvcService());
	    setValues();
        } catch (ConfigurationException ce) {
            AuthnSvcUtils.debug.error("AuthnSvcService.static:", ce);
        }
    }

    /**
     * Default constructor.
     */
    private AuthnSvcService() {
    }


    /**
     * Returns MechanismHandler for specified mechanism.
     *
     * @param mechanism mechanism name
     * @return MechanismHandler for specified mechanism
     */
    public static MechanismHandler getMechanismHandler(String mechanism) {
        return (MechanismHandler)handlers.get(mechanism);
    }

    /**
     * Returns authentication module for 'PLAIN' mechanism handler.
     *
     * @return authentication module for 'PLAIN' mechanism handler
     */
    public static String getPlainMechanismAuthenticationModule() {
        return plainMechanismAuthModule;
    }

    /**
     * Returns authentication module for 'CRAM-MD5' mechanism handler.
     *
     * @return authentication module for 'CRAM-MD5' mechanism handler
     */
    public static String getCramMD5MechanismAuthenticationModule() {
        return cramMD5MechanismAuthModule;
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
        if (AuthnSvcUtils.debug.messageEnabled()) {
	    AuthnSvcUtils.debug.message("AuthnSvcService.configChanged");
        }
	setValues();
    }

    /**
     * Reads values from service schema.
     */
    static private void setValues() {

        Map attrMap = null;
        try {
            attrMap = ci.getConfiguration(null, null);
        } catch (ConfigurationException ce) {
            AuthnSvcUtils.debug.error("AuthnSvcService.setValues:", ce);
            return;
        }

        handlers.clear();
        Set values = (Set)attrMap.get(MECHANISM_HANDLER_LIST_ATTR);
        for (Iterator iter = values.iterator(); iter.hasNext();) {
            String value = (String)iter.next();
            StringTokenizer stz = new StringTokenizer(value, "|");
            String key = null;
            String class_ = null;
            while(stz.hasMoreTokens()) {
                String token = stz.nextToken();
                if (token.startsWith("key=")) {
                    key = token.substring(4);
                } else if (token.startsWith("class=")) {
                    class_ = token.substring(6);
                }
            }
            if (key != null && class_ != null) {
                try {
                    handlers.put(key, (MechanismHandler)
                                      Class.forName(class_).newInstance());
                } catch (Throwable t) {
                    AuthnSvcUtils.debug.error(
                            "AuthnSvcService.setValues class = " + class_, t);
                }
            } else {
                if (AuthnSvcUtils.debug.warningEnabled()) {
                    AuthnSvcUtils.debug.warning(
                            "AuthnSvcService.setValues: Invalid syntax " +
                            "for Mechanism Handler List: " +  value);
                }
            }
        }

        values = (Set)attrMap.get(PLAIN_MECHANISM_AUTH_MODULE);
        if ((values == null) || (values.isEmpty())) {
            plainMechanismAuthModule = null;
        } else {
            plainMechanismAuthModule = (String)values.iterator().next();
        }

        values = (Set)attrMap.get(CRAMMD5_MECHANISM_AUTH_MODULE);
        if ((values == null) || (values.isEmpty())) {
            cramMD5MechanismAuthModule = null;
        } else {
            cramMD5MechanismAuthModule = (String)values.iterator().next();
        }
    }
}
