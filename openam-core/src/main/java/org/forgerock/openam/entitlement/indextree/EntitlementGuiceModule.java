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
package org.forgerock.openam.entitlement.indextree;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ServiceManagementDAO;
import com.sun.identity.sm.ServiceManagementDAOWrapper;
import org.forgerock.openam.entitlement.indextree.IndexTreeServiceImpl.DNWrapper;

import javax.inject.Singleton;
import java.security.PrivilegedAction;

/**
 * Provides object wiring through the use of DI for entitlements.
 *
 * @author apforrest
 */
public class EntitlementGuiceModule extends AbstractModule {

    private static final AdminTokenType ADMIN_TOKEN_TYPE;
    private static final AdminTokenProvider ADMIN_TOKEN_PROVIDER;

    static {
        ADMIN_TOKEN_TYPE = new AdminTokenType();
        ADMIN_TOKEN_PROVIDER = new AdminTokenProvider();
    }

    @Override
    protected void configure() {
        bind(ADMIN_TOKEN_TYPE).toProvider(ADMIN_TOKEN_PROVIDER).in(Singleton.class);
        bind(ServiceManagementDAO.class).to(ServiceManagementDAOWrapper.class).in(Singleton.class);
        bind(DNWrapper.class).in(Singleton.class);
        bind(IndexTreeService.class).to(IndexTreeServiceImpl.class).in(Singleton.class);
    }

    // Implementation exists to capture the generic type of the PrivilegedAction.
    private static class AdminTokenType extends TypeLiteral<PrivilegedAction<SSOToken>> {
    }

    // Simple provide implementation to return the static instance of AdminTokenAction.
    private static class AdminTokenProvider implements Provider<PrivilegedAction<SSOToken>> {

        @Override
        public PrivilegedAction<SSOToken> get() {
            return AdminTokenAction.getInstance();
        }

    }

}
