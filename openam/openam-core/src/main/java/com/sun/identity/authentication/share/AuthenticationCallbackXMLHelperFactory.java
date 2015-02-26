/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthenticationCallbackXMLHelperFactory.java,v 1.3 2008/06/25 05:42:05 qcheng Exp $
 *
 */


package com.sun.identity.authentication.share;

import com.iplanet.am.util.SystemProperties;

/**
 * A factory to access <code>AuthenticationCallbackXMLHelper</code> instance.
 * This factory uses the configuration key
 * <code>com.sun.identity.authentication.callbackXMLHelper</code> to identify 
 * the implementation of <code>AuthenticationCallbackXMLHelper</code> interface;
 * instantiates this class; and returns the instance for XML processing of 
 * Authentication Callback during Authentication remote SDK execution.
 */
public class AuthenticationCallbackXMLHelperFactory {

    /**
     * The configuration key used for identifying the implemenation class of
     * <code>AuthenticationCallbackXMLHelper</code> interface.
     */
    public static final String CONFIG_CALLBACK_XML_HELPER =
        "com.sun.identity.authentication.callbackXMLHelper";

    /**
     * The default implementation to be used in case no value is specified in
     * the configuration.
     */
    public static final String DEFAULT_CALLBACK_XML_HELPER =
    "com.sun.identity.authentication.share.AuthenticationCallbackXMLHelperImpl";

    /**
     * Singleton instance of <code>AuthenticationCallbackXMLHelper</code>.
     */
     private static AuthenticationCallbackXMLHelper callbackXMLHelper;

     static {
        String className = SystemProperties.get(CONFIG_CALLBACK_XML_HELPER,
            DEFAULT_CALLBACK_XML_HELPER);
            
        try {
            callbackXMLHelper = 
                (AuthenticationCallbackXMLHelper)Class.forName(className)
                .newInstance();
        } catch (Exception e) {
            if (AuthXMLUtils.debug.messageEnabled()) {
                AuthXMLUtils.debug.message("Failed to instantiate : " 
                    + className + e.toString());
            }
        } 
     }

     private AuthenticationCallbackXMLHelperFactory() {
     }

     /**
      * Returns an instance of <code>AuthenticationCallbackXMLHelper</code>.
      * This instance is instantiated during static initialization of this
      * factory and is kept as a singleton throughout its lifecycle.
      *
      * @return an instance of <code>AuthenticationCallbackXMLHelper</code>.
      */
    public static AuthenticationCallbackXMLHelper getCallbackXMLHelper() {
        return callbackXMLHelper;
    }
}
