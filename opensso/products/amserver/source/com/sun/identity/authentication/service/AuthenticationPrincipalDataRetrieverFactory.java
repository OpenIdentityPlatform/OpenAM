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
 * $Id: AuthenticationPrincipalDataRetrieverFactory.java,v 1.3 2008/06/25 05:42:04 qcheng Exp $
 *
 */


package com.sun.identity.authentication.service;

import com.iplanet.am.util.SystemProperties;

/**
 * A factory to access <code>AuthenticationPrincipalDataRetriever</code> 
 * instance.
 * This factory uses the configuration key
 * <code>com.sun.identity.authentication.principalDataRetriever</code> to 
 * identify the implementation of 
 * <code>AuthenticationPrincipalDataRetriever</code> interface; 
 * instantiates this class; and returns the instance for retrieving 
 * Authentication module <code>Principal</code> data, to be populated in
 * successful user authentication session.
 */
public class AuthenticationPrincipalDataRetrieverFactory {

    /**
     * The configuration key used for identifying the implemenation class of
     * <code>AuthenticationPrincipalDataRetriever</code> interface.
     */
    public static final String CONFIG_PRINCIPAL_DATA_RETRIEVER =
        "com.sun.identity.authentication.principalDataRetriever";

    /**
     * The default implementation to be used in case no value is specified in
     * the configuration.
     */
    public static final String DEFAULT_PRINCIPAL_DATA_RETRIEVER =
        "com.sun.identity.authentication.service.AuthenticationPrincipalDataRetrieverImpl";

    /**
     * Singleton instance of <code>AuthenticationPrincipalDataRetriever</code>.
     */
     private static AuthenticationPrincipalDataRetriever principalDataRetriever;

     static {
        String className = SystemProperties.get(CONFIG_PRINCIPAL_DATA_RETRIEVER,
            DEFAULT_PRINCIPAL_DATA_RETRIEVER);
            
        try {
            principalDataRetriever = 
                (AuthenticationPrincipalDataRetriever)Class.forName(className)
                .newInstance();
        } catch (Exception e) {
            if (AuthD.debug.messageEnabled()) {
                AuthD.debug.message("Failed to instantiate : " + className 
                    + e.toString());
            }
        } 
     }

     private AuthenticationPrincipalDataRetrieverFactory() {
     }

    /**
     * Returns an instance of <code>AuthenticationPrincipalDataRetriever</code>.
     * This instance is instantiated during static initialization of this
     * factory and is kept as a singleton throughout its lifecycle.
     *
     * @return an instance of <code>AuthenticationPrincipalDataRetriever</code>.
     */
    public static AuthenticationPrincipalDataRetriever
        getPrincipalDataRetriever() {
        return principalDataRetriever;
    }
}
