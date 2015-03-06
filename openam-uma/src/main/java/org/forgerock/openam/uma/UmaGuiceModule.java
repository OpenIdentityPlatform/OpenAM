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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;


import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenIntrospectionHandler;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.oauth2.AccessTokenProtectionFilter;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaAuditEntry;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.utils.Config;
import org.restlet.Request;
import org.restlet.Restlet;

@GuiceModule
public class UmaGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(UmaTokenStore.class, UmaTokenStore.class)
                .build(UmaTokenStoreFactory.class));

        Multibinder.newSetBinder(binder(), TokenIntrospectionHandler.class)
                .addBinding().to(UmaTokenIntrospectionHandler.class);

        install(new FactoryModuleBuilder()
                .implement(UmaSettings.class, UmaSettingsImpl.class)
                .build(UmaSettingsFactory.class));
    }

    @Provides
    Config<UmaAuditLogger> getUmaAuditLogger() {
        return new Config<UmaAuditLogger>() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public UmaAuditLogger get() {
                return InjectorHolder.getInstance(UmaAuditLogger.class);
            }
        };
    }

    @Provides
    @Inject
    public JavaBeanAdapter<RequestingPartyToken> getRPTAdapter(TokenIdGenerator idFactory) {
        return new JavaBeanAdapter<RequestingPartyToken>(RequestingPartyToken.class, idFactory);
    }

    @Provides
    @Inject
    public JavaBeanAdapter<PermissionTicket> getPermissionTicketAdapter(TokenIdGenerator idFactory) {
        return new JavaBeanAdapter<PermissionTicket>(PermissionTicket.class, idFactory);
    }

    @Provides
    @Inject
    public JavaBeanAdapter<UmaAuditEntry> getAuditEntryAdapter(TokenIdGenerator idFactory) {
        return new JavaBeanAdapter<UmaAuditEntry>(UmaAuditEntry.class, idFactory);
    }

    @Provides
    @Inject
    @Singleton
    @Named(UmaConstants.PERMISSION_REQUEST_ENDPOINT)
    public Restlet createPermissionRequestEndpoint(TokenStore store, OAuth2RequestFactory<Request> requestFactory) {
        return new UmaExceptionFilter(
                new AccessTokenProtectionFilter(UmaConstants.PAT_SCOPE, store, requestFactory,
                        wrap(PermissionRequestEndpoint.class)));
    }

    @Provides
    @Inject
    @Singleton
    @Named(UmaConstants.AUTHORIZATION_REQUEST_ENDPOINT)
    public Restlet createAuthorizationRequestEndpoint(TokenStore store, OAuth2RequestFactory<Request> requestFactory) {
        return new UmaExceptionFilter(
                new AccessTokenProtectionFilter(UmaConstants.AAT_SCOPE, store, requestFactory,
                        wrap(AuthorizationRequestEndpoint.class)));
    }

}
