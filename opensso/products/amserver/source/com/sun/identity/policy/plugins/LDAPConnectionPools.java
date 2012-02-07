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
 * $Id: LDAPConnectionPools.java,v 1.7 2009/01/28 05:35:01 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;

import com.sun.identity.shared.ldap.*; 
import com.sun.identity.shared.debug.Debug;    
import com.sun.identity.shared.ldap.factory.JSSESocketFactory;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResBundleUtils;
import java.util.*; 

/**
 * Provides a pool of <code>LDAPConnection</code>
 * objects per host(s) for the policy service to use.
 */
public class LDAPConnectionPools {

    private static HashMap connectionPools = new HashMap();
    
    private final static int MIN_CONNECTION_POOL_SIZE = 1;
    private final static int MAX_CONNECTION_POOL_SIZE = 10;
    private final static int DEFAULT_PORT = 389;
    private static Debug debug = 
              Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private LDAPConnectionPools() {
    // Do nothing
    }

    /**
     * Create a Ldap Connection Pool for a ldap server
     * @param host the name of the LDAP server host and its port number.
     *        For example, dsame.sun.com:389
     *        Alternatively, this can be a space-delimited list of
     *        host names.
     * @param ssl if the connection is in ssl
     * @param minPoolSize minimal pool size
     * @param maxPoolSize maximum pool size
     */ 
    static void initConnectionPool(String host, 
                                   String authDN,
                                   String authPasswd,
                                   boolean ssl, 
                                   int minPoolSize, 
                                   int maxPoolSize) 
            throws PolicyException {

        if (host.length() < 1) {
            debug.message("Invalid host name");
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_ldap_server_host", null, null);
        }

        LDAPConnectionPool cPool = null;
        try {
            synchronized(connectionPools) {
                cPool = (LDAPConnectionPool)connectionPools.get(host);
    
                if (cPool == null) {
                    if (debug.messageEnabled()) {
                        debug.message("Create LDAPConnectionPool: " + host);
                    }
                    LDAPConnection ldc;
                    if (ssl) {
                        ldc = new LDAPConnection(
                            new JSSESocketFactory(null));
                    }
                    else {
                        ldc = new LDAPConnection();
                    }
    
                    ldc.setOption(LDAPv3.PROTOCOL_VERSION, new Integer(3));
                    ldc.connect(host, DEFAULT_PORT, authDN, authPasswd);
    
                    if (minPoolSize < 1) {
                        minPoolSize = MIN_CONNECTION_POOL_SIZE;
                    }
    
                    if (maxPoolSize < 1) {
                        maxPoolSize = MAX_CONNECTION_POOL_SIZE;
                    }
    
                    if (debug.messageEnabled()) {
                        debug.message(
                            "LDAPConnectionPools.initConnectionPool(): " +
                            "minPoolSize=" + minPoolSize +
                            ", maxPoolSize=" + maxPoolSize);
                    }
                    ShutdownManager shutdownMan = ShutdownManager.getInstance();
                    if (shutdownMan.acquireValidLock()) {
                        try {
                            cPool = new LDAPConnectionPool (host + "-Policy",
                                minPoolSize, maxPoolSize, ldc);
                            if (debug.messageEnabled()) {
                                debug.message(
                                "LDAPConnectionPools.initConnectionPool(): " +
                                    " host: " + host);
                            }
                            final LDAPConnectionPool finalPool = cPool;
                            shutdownMan.addShutdownListener(new
                                ShutdownListener() {

                                    public void shutdown() {
                                        if (finalPool != null) {
                                            finalPool.destroy();
                                        }
                                    }

                                });
                        } finally {
                            shutdownMan.releaseLockAndNotify();
                        }
                    }
                    connectionPools.put(host, cPool);
                }
            }
        }
        catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Unable to create LDAPConnectionPool", e);
            }
            throw new PolicyException(e.getMessage(), e);
        }
    }
    
                
    /**
     * Get LDAPConnectionPool for the ldap server from the pools. 
     * @param host the name of host and its port number.
     *        It can be a space-delimited list of host names.
     * @return LDAPConnectionPool for the ldap server
     */
    static LDAPConnectionPool getConnectionPool(String host)
    {
        if (debug.messageEnabled()) {
            debug.message("LDAPConnectionPools.getConnectionPool(): host: " +
                host);
        }
        synchronized(connectionPools) {
           return (LDAPConnectionPool)(connectionPools.get(host));
        }
    }

}
