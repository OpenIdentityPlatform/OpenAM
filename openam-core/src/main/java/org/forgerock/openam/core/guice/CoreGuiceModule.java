/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.core.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceManagementDAO;
import com.sun.identity.sm.ServiceManagementDAOWrapper;
import org.forgerock.openam.entitlement.indextree.IndexChangeHandler;
import org.forgerock.openam.entitlement.indextree.IndexChangeManager;
import org.forgerock.openam.entitlement.indextree.IndexChangeManagerImpl;
import org.forgerock.openam.entitlement.indextree.IndexChangeMonitor;
import org.forgerock.openam.entitlement.indextree.IndexChangeMonitorImpl;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeObservable;
import org.forgerock.openam.entitlement.indextree.IndexTreeService;
import org.forgerock.openam.entitlement.indextree.IndexTreeServiceImpl;
import org.forgerock.openam.guice.AMGuiceModule;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.requests.Requests;

import javax.inject.Singleton;
import java.security.PrivilegedAction;

/**
 * Guice Module for configuring bindings for the OpenAM Core classes.
 *
 * @author apforrest
 */
@AMGuiceModule
public class CoreGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new AdminTokenType()).toProvider(new AdminTokenProvider()).in(Singleton.class);
        bind(ServiceManagementDAO.class).to(ServiceManagementDAOWrapper.class).in(Singleton.class);
        bind(DNWrapper.class).in(Singleton.class);
        bind(ConnectionFactory.class).toProvider(new ConnectionFactoryProvider()).in(Singleton.class);
        bind(IndexChangeObservable.class).in(Singleton.class);
        bind(ShutdownManagerWrapper.class).in(Singleton.class);
        bind(SearchResultHandler.class).to(IndexChangeHandler.class).in(Singleton.class);
        bind(IndexChangeManager.class).to(IndexChangeManagerImpl.class).in(Singleton.class);
        bind(IndexChangeMonitor.class).to(IndexChangeMonitorImpl.class).in(Singleton.class);
        bind(IndexTreeService.class).to(IndexTreeServiceImpl.class).in(Singleton.class);
    }

    // Implementation exists to capture the generic type of the PrivilegedAction.
    private static class AdminTokenType extends TypeLiteral<PrivilegedAction<SSOToken>> {
    }

    // Simple provider implementation to return the static instance of AdminTokenAction.
    private static class AdminTokenProvider implements Provider<PrivilegedAction<SSOToken>> {

        @Override
        public PrivilegedAction<SSOToken> get() {
            // Provider used over bind(..).getInstance(..) to enforce a lazy loading approach.
            return AdminTokenAction.getInstance();
        }

    }

    // Simple provider implementation to return a connection factory.
    private static class ConnectionFactoryProvider implements Provider<ConnectionFactory> {

        @Override
        public ConnectionFactory get() {
            // TODO: This needs to delegate to the new connection utils class.
            DSConfigMgr dsCfg = null;

            try {
                dsCfg = DSConfigMgr.getDSConfigMgr();
            } catch (LDAPServiceException e) {
                throw new RuntimeException(e);
            }

            ServerGroup sg = dsCfg.getServerGroup("sms");
            ServerInstance svrCfg = sg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            String connDN = svrCfg.getAuthID();
            String connPWD = svrCfg.getPasswd();
            String hostString = dsCfg.getHostName("sms");
            int pos = hostString.indexOf(":");
            String hostName = hostString.substring(0, pos);
            int port = Integer.parseInt(hostString.substring(pos + ":".length()));
            ConnectionFactory factory = new LDAPConnectionFactory(hostName, port);
            BindRequest bindRequest = Requests.newSimpleBindRequest(connDN, connPWD.toCharArray());
            return Connections.newAuthenticatedConnectionFactory(factory, bindRequest);
        }

    }

    /**
     * Wrapper class to remove coupling to DNMapper static methods.
     * <p/>
     * Until DNMapper is refactored, this class can be used to assist with DI.
     */
    public static class DNWrapper {

        /**
         * @see com.sun.identity.sm.DNMapper#orgNameToDN(String)
         */
        public String orgNameToDN(String orgName) {
            return DNMapper.orgNameToDN(orgName);
        }

        /**
         * @see DNMapper#orgNameToRealmName(String)
         */
        public String orgNameToRealmName(String orgName) {
            return DNMapper.orgNameToRealmName(orgName);
        }

    }

    /**
     * Wrap class to remove coupling to ShutdownManager static methods.
     * <p/>
     * Until ShutdownManager is refactored, this class can be used to assist with DI.
     */
    public static class ShutdownManagerWrapper {

        /**
         * @see com.sun.identity.common.ShutdownManager#addShutdownListener(com.sun.identity.common.ShutdownListener)
         */
        public void addShutdownListener(ShutdownListener listener) {
            ShutdownManager shutdownManager = ShutdownManager.getInstance();

            try {
                if (shutdownManager.acquireValidLock()) {
                    // Add the listener.
                    shutdownManager.addShutdownListener(listener);
                }
            } finally {
                shutdownManager.releaseLockAndNotify();
            }
        }

    }

}
