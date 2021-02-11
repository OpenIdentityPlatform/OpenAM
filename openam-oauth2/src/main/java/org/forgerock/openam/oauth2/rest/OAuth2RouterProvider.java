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

package org.forgerock.openam.oauth2.rest;

import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.ACTIVE;
import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.TOKEN_TYPE_HINT;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.CLIENT_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.ResourceSets.NAME;
import static org.forgerock.openam.oauth2.OAuth2Constants.ResourceSets.SCOPES;
import static org.forgerock.openam.oauth2.OAuth2Constants.ShortClientAttributeNames.*;
import static org.forgerock.openam.rest.audit.RestletBodyAuditor.*;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.restlet.AccessTokenFlowFinder;
import org.forgerock.oauth2.restlet.AuthorizeEndpointFilter;
import org.forgerock.oauth2.restlet.AuthorizeResource;
import org.forgerock.oauth2.restlet.DeviceCodeResource;
import org.forgerock.oauth2.restlet.DeviceCodeVerificationResource;
import org.forgerock.oauth2.restlet.TokenEndpointFilter;
import org.forgerock.oauth2.restlet.TokenIntrospectionResource;
import org.forgerock.oauth2.restlet.ValidationServerResource;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.rest.RealmRoutingFactory;
import org.forgerock.openam.rest.audit.OAuth2AccessAuditFilter;
import org.forgerock.openam.rest.audit.RestletBodyAuditor;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openidconnect.restlet.ConnectClientRegistration;
import org.forgerock.openidconnect.restlet.EndSession;
import org.forgerock.openidconnect.restlet.IdTokenInfo;
import org.forgerock.openidconnect.restlet.OpenIDConnectCheckSessionEndpoint;
import org.forgerock.openidconnect.restlet.OpenIDConnectConfiguration;
import org.forgerock.openidconnect.restlet.OpenIDConnectJWKEndpoint;
import org.forgerock.openidconnect.restlet.UserInfo;
import org.restlet.Restlet;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;

/**
 * Guice Provider from getting the OAuth2 HTTP router.
 *
 * @since 13.0.0
 */
public class OAuth2RouterProvider implements Provider<Router> {

    private final AuditEventPublisher eventPublisher;
    private final AuditEventFactory eventFactory;
    private final OAuth2RequestFactory requestFactory;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Constructs a new RestEndpoints instance.
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     * @param requestFactory The factory that provides access to OAuth2Request.
     * @param jacksonRepresentationFactory The factory for {@code JacksonRepresentation} instances.
     */
    @Inject
    public OAuth2RouterProvider(AuditEventPublisher eventPublisher, AuditEventFactory eventFactory,
            OAuth2RequestFactory requestFactory,
            JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
        this.requestFactory = requestFactory;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    @Override
    public Router get() {
        final Router router = new RestletRealmRouter();
        router.attach("/" + RealmRoutingFactory.REALM_ROUTE, new RealmRoutingFactory().createRouter(router));

        // Standard OAuth2 endpoints

        router.attach("/authorize", auditWithOAuthFilter(new AuthorizeEndpointFilter(wrap(AuthorizeResource.class),
                jacksonRepresentationFactory)));
        router.attach("/access_token", auditWithOAuthFilter(new TokenEndpointFilter(new AccessTokenFlowFinder(),
                jacksonRepresentationFactory),
                formAuditor(RESPONSE_TYPE, GRANT_TYPE, CLIENT_ID, USERNAME, SCOPE, REDIRECT_URI),
                jacksonAuditor(SCOPE, TOKEN_TYPE)));
        router.attach("/tokeninfo", auditWithOAuthFilter(wrap(ValidationServerResource.class),
                noBodyAuditor(), jacksonAuditor(SCOPE, TOKEN_TYPE)));

        // OAuth 2.0 Token Introspection Endpoint

        router.attach("/introspect", auditWithOAuthFilter(wrap(TokenIntrospectionResource.class),
                formAuditor(TOKEN_TYPE_HINT),
                jsonAuditor(SCOPE, TOKEN_TYPE, CLIENT_ID, USERNAME, ACTIVE)));

        // OpenID Connect endpoints

        router.attach("/connect/register", auditWithOAuthFilter(wrap(ConnectClientRegistration.class),
                jsonAuditor(CLIENT_NAME.getType(), APPLICATION_TYPE.getType(), REDIRECT_URIS.getType()),
                jacksonAuditor(CLIENT_ID, CLIENT_NAME.getType(), APPLICATION_TYPE.getType(), REDIRECT_URIS.getType())));
        router.attach("/userinfo", auditWithOAuthFilter(wrap(UserInfo.class)));
        router.attach("/idtokeninfo", auditWithOAuthFilter(wrap(IdTokenInfo.class)));
        router.attach("/connect/checkSession", auditWithOAuthFilter(wrap(OpenIDConnectCheckSessionEndpoint.class)));
        router.attach("/connect/endSession", auditWithOAuthFilter(wrap(EndSession.class)));
        router.attach("/connect/jwk_uri", auditWithOAuthFilter(wrap(OpenIDConnectJWKEndpoint.class)));

        // Resource Set Registration

        Restlet resourceSetRegistrationEndpoint = auditWithOAuthFilter(getRestlet(OAuth2Constants.Custom.RSR_ENDPOINT),
                jsonAuditor(NAME, SCOPES),
                jacksonAuditor("_id"));
        router.attach("/resource_set/{rsid}", resourceSetRegistrationEndpoint);
        router.attach("/resource_set", resourceSetRegistrationEndpoint);
        router.attach("/resource_set/", resourceSetRegistrationEndpoint);

        // OpenID Connect Discovery

        router.attach("/.well-known/openid-configuration", auditWithOAuthFilter(wrap(OpenIDConnectConfiguration.class)));

        // OAuth 2 Device Flow

        router.attach("/device/user", auditWithOAuthFilter(wrap(DeviceCodeVerificationResource.class)));
        router.attach("/device/code", auditWithOAuthFilter(wrap(DeviceCodeResource.class),
                formAuditor(RESPONSE_TYPE, GRANT_TYPE, CLIENT_ID, SCOPE), noBodyAuditor()));

        // OAuth2 Token Revocation
        router.attach("/token/revoke", auditWithOAuthFilter(wrap(TokenRevocationResource.class)));

        return router;
    }

    private Restlet getRestlet(String name) {
        return InjectorHolder.getInstance(Key.get(Restlet.class, Names.named(name)));
    }

    private Filter auditWithOAuthFilter(Restlet restlet) {
        return new OAuth2AccessAuditFilter(restlet, eventPublisher, eventFactory, requestFactory,
                noBodyAuditor(), noBodyAuditor());
    }

    private Filter auditWithOAuthFilter(Restlet restlet, RestletBodyAuditor<?> requestDetailCreator,
            RestletBodyAuditor<?> responseDetailCreator) {
        return new OAuth2AccessAuditFilter(restlet, eventPublisher, eventFactory, requestFactory,
                requestDetailCreator, responseDetailCreator);
    }
}
