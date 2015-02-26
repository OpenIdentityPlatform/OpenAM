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
 * $Id: AuthenticationServiceNameProviderFactory.java,v 1.2 2008/06/25 05:44:03 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import com.iplanet.am.util.SystemProperties;

/**
 * A factory to access <code>AuthenticationServiceNameProvider</code> instance
 * at runtime. This factory uses the configuration key
 * <code>com.sun.identity.sm.authservicename.provider</code> to identify an
 * implementation of <code>AuthenticationServiceNameProvider</code> interface
 * at runtime.
 */
public class AuthenticationServiceNameProviderFactory {

    /**
     * The configuration key used for identifying the class that implements the
     * <code>AuthenticationServiceNameProvider</code> interface.
     */
    public static final String CONFIG_AUTH_SERVICE_NAME_PROVIDER = 
        "com.sun.identity.sm.authservicename.provider";

    /**
     * The default implementation to be used in case no value is specified in
     * the configuration.
     */
    public static final String DEFAULT_AUTH_SERVICE_NAME_PROVIDER = 
        "com.sun.identity.sm.AuthenticationServiceNameProviderImpl";

    /**
     * The singleton instance of <code>AuthenticationServiceNameProvider</code>
     * to be used.
     */
    private static AuthenticationServiceNameProvider authServiceNameProvider;

    /**
     * Provides access to the configured implementation of
     * <code>AuthenticationServiceNameProvider</code> interface. An instance
     * of this implementation is constructed during static initialization of
     * this factory and is kept as a singleton throughout its lifecycle.
     * 
     * @return the configured implementation of
     *         <code>AuthenticationServiceNameProvider</code>.
     */
    public static AuthenticationServiceNameProvider getProvider() {
        return authServiceNameProvider;
    }

    static {
        try {
            String className = SystemProperties.get(
                    CONFIG_AUTH_SERVICE_NAME_PROVIDER,
                    DEFAULT_AUTH_SERVICE_NAME_PROVIDER);

            authServiceNameProvider = (AuthenticationServiceNameProvider) Class
                    .forName(className).newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("Initialization Failed", ex);
        }
    }

}
