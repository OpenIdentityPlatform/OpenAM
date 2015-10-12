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

package org.forgerock.openam.oauth2.rest;

import static org.forgerock.openam.audit.AuditConstants.OAUTH2_AUDIT_CONTEXT_PROVIDERS;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.restlet.AccessTokenFlowFinder;
import org.forgerock.oauth2.restlet.AuthorizeEndpointFilter;
import org.forgerock.oauth2.restlet.AuthorizeResource;
import org.forgerock.oauth2.restlet.DeviceCodeVerificationResource;
import org.forgerock.oauth2.restlet.DeviceCodeResource;
import org.forgerock.oauth2.restlet.DeviceTokenResource;
import org.forgerock.oauth2.restlet.TokenEndpointFilter;
import org.forgerock.oauth2.restlet.TokenIntrospectionResource;
import org.forgerock.oauth2.restlet.ValidationServerResource;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.audit.OAuth2AccessAuditFilter;
import org.forgerock.openam.rest.audit.OAuth2AuditContextProvider;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openidconnect.restlet.ConnectClientRegistration;
import org.forgerock.openidconnect.restlet.EndSession;
import org.forgerock.openidconnect.restlet.OpenIDConnectConfiguration;
import org.forgerock.openidconnect.restlet.OpenIDConnectJWKEndpoint;
import org.forgerock.openidconnect.restlet.UserInfo;
import org.restlet.Restlet;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;

import java.util.Set;

/**
 * Guice Provider from getting the OAuth2 HTTP router.
 *
 * @since 13.0.0
 */
public class OAuth2RouterProvider implements Provider<Router> {

    private final RestRealmValidator realmValidator;
    private final CoreWrapper coreWrapper;
    private final AuditEventPublisher eventPublisher;
    private final AuditEventFactory eventFactory;
    private final Set<OAuth2AuditContextProvider> contextProviders;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param coreWrapper An instance of the CoreWrapper.
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     * @param contextProviders The OAuth2 audit context providers, responsible for finding details which can
     *                         be audit logged from various tokens which may be attached to requests and/or responses.
     */
    @Inject
    public OAuth2RouterProvider(RestRealmValidator realmValidator, CoreWrapper coreWrapper,
            AuditEventPublisher eventPublisher, AuditEventFactory eventFactory,
            @Named(OAUTH2_AUDIT_CONTEXT_PROVIDERS) Set<OAuth2AuditContextProvider> contextProviders) {
        this.realmValidator = realmValidator;
        this.coreWrapper = coreWrapper;
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
        this.contextProviders = contextProviders;
    }

    @Override
    public Router get() {
        final Router router = new RestletRealmRouter(realmValidator, coreWrapper);

        // Standard OAuth2 endpoints

        router.attach("/authorize", auditWithOAuthFilter(new AuthorizeEndpointFilter(wrap(AuthorizeResource.class))));
        router.attach("/access_token", auditWithOAuthFilter(new TokenEndpointFilter(new AccessTokenFlowFinder())));
        router.attach("/tokeninfo", auditWithOAuthFilter(wrap(ValidationServerResource.class)));

        // OAuth 2.0 Token Introspection Endpoint

        router.attach("/introspect", auditWithOAuthFilter(wrap(TokenIntrospectionResource.class)));

        // OpenID Connect endpoints

        router.attach("/connect/register", auditWithOAuthFilter(wrap(ConnectClientRegistration.class)));
        router.attach("/userinfo", auditWithOAuthFilter(wrap(UserInfo.class)));
        router.attach("/connect/endSession", auditWithOAuthFilter(wrap(EndSession.class)));
        router.attach("/connect/jwk_uri", auditWithOAuthFilter(wrap(OpenIDConnectJWKEndpoint.class)));

        // Resource Set Registration

        Restlet resourceSetRegistrationEndpoint = getRestlet(OAuth2Constants.Custom.RSR_ENDPOINT);
        router.attach("/resource_set/{rsid}", auditWithOAuthFilter(resourceSetRegistrationEndpoint));
        router.attach("/resource_set", auditWithOAuthFilter(resourceSetRegistrationEndpoint));
        router.attach("/resource_set/", auditWithOAuthFilter(resourceSetRegistrationEndpoint));

        // OpenID Connect Discovery

        router.attach("/.well-known/openid-configuration", auditWithOAuthFilter(wrap(OpenIDConnectConfiguration.class)));

        // OAuth 2 Device Flow

        router.attach("/device/user", wrap(DeviceCodeVerificationResource.class));
        router.attach("/device/code", wrap(DeviceCodeResource.class));
        router.attach("/device/token", wrap(DeviceTokenResource.class));

        return router;
    }

    private Restlet getRestlet(String name) {
        return InjectorHolder.getInstance(Key.get(Restlet.class, Names.named(name)));
    }

    private Filter auditWithOAuthFilter(Restlet restlet) {
        return new OAuth2AccessAuditFilter(restlet, eventPublisher, eventFactory, contextProviders);
    }
}
