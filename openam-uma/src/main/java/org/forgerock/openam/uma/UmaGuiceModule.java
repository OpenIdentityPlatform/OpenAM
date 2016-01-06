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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import static org.forgerock.openam.rest.service.RestletUtils.wrap;
import static org.forgerock.openam.uma.UmaConstants.UMA_BACKEND_POLICY_RESOURCE_HANDLER;

import org.forgerock.openam.auditors.SMSAuditFilter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.security.auth.Subject;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.idm.IdRepoCreationListener;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenIntrospectionHandler;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.restlet.resources.ResourceSetRegistrationHook;
import org.forgerock.openam.core.rest.UiRolePredicate;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.entitlement.rest.PolicyResource;
import org.forgerock.openam.oauth2.AccessTokenProtectionFilter;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaAuditEntry;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.rest.UmaIdRepoCreationListener;
import org.forgerock.openam.uma.rest.UmaPolicyEvaluatorFactory;
import org.forgerock.openam.uma.rest.UmaPolicyServiceImpl;
import org.forgerock.openam.uma.rest.UmaResourceSetRegistrationHook;
import org.forgerock.openam.uma.rest.UmaRouterProvider;
import org.forgerock.openam.utils.Config;
import org.forgerock.services.context.RootContext;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.routing.Router;

@GuiceModule
public class UmaGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Key.get(Router.class, Names.named("UMARouter"))).toProvider(UmaRouterProvider.class)
                .in(Singleton.class);
        bind(UmaPolicyService.class).to(UmaPolicyServiceImpl.class);

        Multibinder.newSetBinder(binder(), IdRepoCreationListener.class)
                .addBinding().to(UmaIdRepoCreationListener.class);

        Multibinder.newSetBinder(binder(), ResourceSetRegistrationHook.class)
                .addBinding().to(UmaResourceSetRegistrationHook.class);

        install(new FactoryModuleBuilder()
                .implement(UmaTokenStore.class, UmaTokenStore.class)
                .build(UmaTokenStoreFactory.class));

        Multibinder.newSetBinder(binder(), TokenIntrospectionHandler.class)
                .addBinding().to(UmaTokenIntrospectionHandler.class);

        install(new FactoryModuleBuilder()
                .implement(UmaSettings.class, UmaSettingsImpl.class)
                .build(UmaSettingsFactory.class));

        MapBinder.newMapBinder(binder(), String.class, ClaimGatherer.class)
                .addBinding(IdTokenClaimGatherer.FORMAT).to(IdTokenClaimGatherer.class);

        Multibinder.newSetBinder(binder(), SMSAuditFilter.class)
                .addBinding().to(UmaAuditFilter.class);

        Multibinder.newSetBinder(binder(), UiRolePredicate.class)
                .addBinding().to(UmaUserUiRolePredicate.class);
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
    UmaPolicyEvaluatorFactory getUmaPolicyEvaluatorFactory() {
        return new UmaPolicyEvaluatorFactory() {
            @Override
            public Evaluator getEvaluator(Subject subject, String application) throws EntitlementException {
                return new Evaluator(subject, application);
            }
        };
    }

    @Provides
    @Inject
    @Singleton
    @Named(UMA_BACKEND_POLICY_RESOURCE_HANDLER)
    RequestHandler getPolicyResource(PolicyResource policyResource) {
        return Resources.newCollection(policyResource);
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
    public JavaBeanAdapter<UmaPendingRequest> getPendingRequestAdapter(TokenIdGenerator idFactory) {
        return new JavaBeanAdapter<>(UmaPendingRequest.class, idFactory);
    }

    @Provides
    @Inject
    @Singleton
    @Named(UmaConstants.PERMISSION_REQUEST_ENDPOINT)
    public Restlet createPermissionRequestEndpoint(TokenStore store, OAuth2RequestFactory<?, Request> requestFactory) {
        return new AccessTokenProtectionFilter(UmaConstants.PAT_SCOPE, store, requestFactory,
                        wrap(PermissionRequestEndpoint.class));
    }

    @Provides
    @Inject
    @Singleton
    @Named(UmaConstants.AUTHORIZATION_REQUEST_ENDPOINT)
    public Restlet createAuthorizationRequestEndpoint(TokenStore store, OAuth2RequestFactory<?, Request> requestFactory) {
        return new AccessTokenProtectionFilter(UmaConstants.AAT_SCOPE, store, requestFactory,
                        wrap(AuthorizationRequestEndpoint.class));
    }

    @Provides
    @Named("UMA")
    Client getHttpClient() {
        try {
            return new Client(new HttpClientHandler(), new RootContext());
        } catch (HttpApplicationException e) {
            throw new RuntimeException("Failed to create HTTP Client. "
                    + "Is the HTTP Client binding present on the classpath?", e);
        }
    }
}
