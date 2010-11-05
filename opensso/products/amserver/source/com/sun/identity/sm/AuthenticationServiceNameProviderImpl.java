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
 * $Id: AuthenticationServiceNameProviderImpl.java,v 1.3 2008/06/25 05:44:03 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A concrete implementation of <code>AuthenticationServiceNameProvider</code>
 * that uses the 
 * <code>com.sun.identity.authentication.config.AMAuthenticationManager</code>
 * to retrieve the names of authentication module services that are loaded by
 * default.
 */
public class AuthenticationServiceNameProviderImpl implements
        AuthenticationServiceNameProvider {
    
    private static boolean initialized;
    private static HashSet authNmodules = new HashSet();
    private static Debug debug = SMSEntry.debug;
    
    /**
     * Provides a collection of authentication module service names that are
     * loaded by default. This implementation uses the authentication 
     * service specific configuration to retrieve the relevant 
     * module service name information.
     * 
     * @return a <code>Set</code> of authentication module service names.
     */
    public Set getAuthenticationServiceNames() {
        if (initialized) {
            return authNmodules;
        }
        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            ServiceSchemaManager scm = new ServiceSchemaManager(
                ISAuthConstants.AUTH_SERVICE_NAME, token);
            ServiceSchema schema = scm.getGlobalSchema();
            Set authenticators = (Set) schema.getAttributeDefaults().get(
                ISAuthConstants.AUTHENTICATORS);
            for (Iterator it = authenticators.iterator(); it.hasNext();) {
                String module = (String) it.next();
                int index = module.lastIndexOf(".");
                if (index != -1) {
                    module = module.substring(index + 1);
                }
                String serviceName = "iPlanetAMAuth" + module + "Service";
                // Check if the service name exisits with organization schema
                try {
                    ServiceSchemaManager ssm = new ServiceSchemaManager(
                        serviceName, token);
                    if (ssm.getOrganizationSchema() != null) {
                        authNmodules.add(serviceName);
                    }
                } catch (Exception e) {
                    // Try with "sunAMAuth"
                    serviceName = "sunAMAuth" + module + "Service";
                    try {
                        ServiceSchemaManager ssm = new ServiceSchemaManager(
                            serviceName, token);
                        if (ssm.getOrganizationSchema() != null) {
                            authNmodules.add(serviceName);
                        }
                    } catch (Exception ee) {
                        // Ignore the Exception and donot add to authmodules
                        // 1) Service does not exisit
                        // 2) OrganizationSchema does not exisit
                    }
                }
            }
            initialized = true;
        } catch (SMSException ex) {
            debug.error("AuthenticationServiceNameProviderImpl error", ex);
        } catch (SSOException ex) {
            debug.error("AuthenticationServiceNameProviderImpl error", ex);
        }
        return authNmodules;
    }
}
