/*
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
 * $Id: SMDataLayer.java,v 1.16 2009/01/28 05:35:04 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package com.sun.identity.sm.ldap;

import static com.sun.identity.shared.Constants.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;
import com.sun.identity.shared.debug.Debug;
import java.util.concurrent.TimeUnit;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

/**
 * SMDataLayer (A PACKAGE SCOPE CLASS) to access LDAP or other database
 * 
 * TODO: 1. Needs to subclass and isolate the current implementation of
 * DataLayer as DSLayer for ldap specific operations 2. Improvements needed for
 * _ldapPool: destroy(), initial bind user, tunning for MIN and MAX initial
 * settings etc 3. May choose to extend implementation of _ldapPool from
 * LdapConnectionPool so that there is load balance between connections.Also
 * _ldapPool may be implemented with a HashTable of (host,port) for multiple
 * pools of connections for mulitple (host,port) to DS servers instead of single
 * host and port.
 * 
 */
class SMDataLayer {

    /**
     * Static section to retrieve the debug object.
     */
    private static Debug debug;

    private static SMDataLayer m_instance = null;

    private ConnectionFactory _ldapPool = null;

    /**
     * SMDataLayer constructor
     */
    private SMDataLayer() {
        initLdapPool();
    }

    /**
     * create the singleton SMDataLayer object if it doesn't exist already.
     */
    protected synchronized static SMDataLayer getInstance() {
        // Obtain the Debug instance.
        debug = Debug.getInstance("amSMSLdap");

        // Make sure only one instance of this class is created.
        if (m_instance == null) {
            m_instance = new SMDataLayer();
        }
        return m_instance;
    }

    /**
     * Get connection from pool, not through LDAPProxy. Reauthenticate if
     * necessary
     * 
     * @return connection that is available to use
     */
    protected Connection getConnection() {
        if (_ldapPool == null)
            return null;

        debug.message("SMDataLayer:getConnection() - Invoking _ldapPool.getConnection()");
        Connection conn = null;
        try {
            conn = _ldapPool.getConnection();
            debug.message("SMDataLayer:getConnection() - Got Connection : {}", conn);
        } catch (LdapException e) {
            debug.error("SMDataLayer:getConnection() - Failed to get Connection", e);
        }

        return conn;
    }

    /**
     * Closes all the open ldap connections 
     */
    protected synchronized void shutdown() {
        if (_ldapPool != null) {
            _ldapPool.close();
        }
        _ldapPool = null;
        m_instance = null;
    }

    /**
     * Initialize the pool shared by all SMDataLayer object(s).
     */
    private synchronized void initLdapPool() {
        // Dont' do anything if pool is already initialized
        if (_ldapPool != null)
            return;

        // Initialize the pool with minimum and maximum connections settings
        // retrieved from configuration
        ServerInstance svrCfg;

        try {
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();

            // Get "sms" ServerGroup if present
            ServerGroup sg = dsCfg.getServerGroup("sms");
            final ConnectionFactory baseFactory;
            if (sg != null) {
                baseFactory = dsCfg.getNewConnectionFactory("sms",
                        LDAPUser.Type.AUTH_ADMIN);
                svrCfg = sg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            } else {
                baseFactory = dsCfg.getNewAdminConnectionFactory();
                svrCfg = dsCfg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            }
            if (svrCfg == null) {
                debug.error("SMDataLayer:initLdapPool()-"
                        + "Error getting server config.");
            }

            int poolMin = 1;
            int poolMax = 2;
            // Initialize the Connection Pool size only for the server
            if (SystemProperties.isServerMode()) {
                poolMin = svrCfg.getMinConnections();
                poolMax = svrCfg.getMaxConnections();
            }

            debug.message("SMDataLayer:initLdapPool(): Creating ldap connection pool with: poolMin {} poolMax {}",
                    poolMin, poolMax);

            int idleTimeout = SystemProperties.getAsInt(LDAP_CONN_IDLE_TIME_IN_SECS, 0);
            if (idleTimeout == 0 && StringUtils.isNotBlank(SystemProperties.get(LDAP_CONN_IDLE_TIME_IN_SECS))) {
                debug.error("SMDataLayer: Idle timeout could not be parsed, connection reaping is disabled");
            } else if (idleTimeout == 0) {
                debug.message("SMDataLayer: Idle timeout is set to 0 - connection reaping is disabled");
            }
            _ldapPool = Connections.newCachedConnectionPool(baseFactory, poolMin, poolMax,
                    idleTimeout, TimeUnit.SECONDS);

            ShutdownManager shutdownMan = com.sun.identity.common.ShutdownManager.getInstance();
            shutdownMan.addShutdownListener(
                    new ShutdownListener() {
                        public void shutdown() {
                            if (_ldapPool != null) {
                                _ldapPool.close();
                            }
                        }
                    }
            );

        } catch (LDAPServiceException ex) {
            debug.error("SMDataLayer:initLdapPool()-"
                    + "Error initializing connection pool " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
