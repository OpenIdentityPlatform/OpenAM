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
 * $Id: ConnectionFactoryProviderFactory.java,v 1.3 2008/07/22 18:12:03 weisun2 Exp $
 *
 */

package com.sun.identity.ha.jmqdb;

import com.sun.identity.ha.FAMRecordUtils;
/**
 * A factory to access <code>ConnectionFactoryProvider</code> instance at
 * runtime. This factory uses the configuration key
 * <code>com.sun.identity.session.connectionfactory.provider</code> to
 * identify an implementation of <code>ConnectionFactoryProvider</code>
 * interface at runtime.
 */
@Deprecated
public class ConnectionFactoryProviderFactory {

    /**
     * The configuration key used for identifying the class that implements the
     * <code>ConnectionFactoryProvider</code> interface.
     */
    public static final String CONFIG_CONNECTION_FACTORY_PROVIDER_FACTORY = 
        "com.sun.identity.session.connectionfactory.provider";

    /**
     * The default implementation to be used in case no value is specified in
     * the configuration.
     */
    public static final String DEFAULT_CONNECTION_FACTORY_PROVIDER_FACTORY = 
        "com.sun.identity.ha.jmqdb.ConnectionFactoryProviderImpl";

    /**
     * The singleton instance of <code>ConnectionFactoryProvider</code> to be
     * used.
     */
    private static ConnectionFactoryProvider connectionFactoryProvider;

    /**
     * Provides access to the configured implementation of
     * <code>ConnectionFactoryProvider</code> interface. An instance of this
     * implementation is constructed during static initialization of this
     * factory and is kept as a singleton throughout its lifecycle.
     * 
     * @return the configured implementation of
     *         <code>ConnectionFactoryProvider</code>.
     */
    public static ConnectionFactoryProvider getProvider() {
        return connectionFactoryProvider;
    }

    static {
        try {    
            String className = System.getProperty(
                    CONFIG_CONNECTION_FACTORY_PROVIDER_FACTORY,
                    DEFAULT_CONNECTION_FACTORY_PROVIDER_FACTORY);                    

            connectionFactoryProvider = (ConnectionFactoryProvider) Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();
            
        } catch (Exception ex) {
            throw new RuntimeException(FAMRecordUtils.bundle.getString(
               "failedInitialize"), ex);
        }
    }
}
