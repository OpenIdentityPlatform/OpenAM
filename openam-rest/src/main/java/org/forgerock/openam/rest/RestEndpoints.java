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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.rest;

import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.sm.InvalidRealmNameManager;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.VersionSelector;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.restlet.AccessTokenFlowFinder;
import org.forgerock.oauth2.restlet.AuthorizeEndpointFilter;
import org.forgerock.oauth2.restlet.AuthorizeResource;
import org.forgerock.oauth2.restlet.TokenEndpointFilter;
import org.forgerock.oauth2.restlet.TokenIntrospectionResource;
import org.forgerock.oauth2.restlet.ValidationServerResource;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.forgerockrest.XacmlService;
import org.forgerock.openam.rest.audit.RestletAccessAuditFilterFactory;
import org.forgerock.openam.forgerockrest.authn.restlet.AuthenticationServiceV1;
import org.forgerock.openam.forgerockrest.authn.restlet.AuthenticationServiceV2;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.uma.UmaConstants;
import org.forgerock.openam.uma.UmaExceptionFilter;
import org.forgerock.openam.uma.UmaWellKnownConfigurationEndpoint;
import org.forgerock.openidconnect.restlet.ConnectClientRegistration;
import org.forgerock.openidconnect.restlet.EndSession;
import org.forgerock.openidconnect.restlet.OpenIDConnectConfiguration;
import org.forgerock.openidconnect.restlet.OpenIDConnectJWKEndpoint;
import org.forgerock.openidconnect.restlet.UserInfo;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Singleton class which contains both the routers for CREST resources and Restlet service endpoints.
 *
 * @since 12.0.0
 */
@Singleton
public class RestEndpoints {

    private final RestRealmValidator realmValidator;
    private final VersionSelector versionSelector;
    private final CoreWrapper coreWrapper;
    private final ServiceRouter jsonServiceRouter;
    private final ServiceRouter xacmlServiceRouter;
    private final Router umaServiceRouter;
    private final Router oauth2ServiceRouter;
    private final RestletAccessAuditFilterFactory restletAuditFactory;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param versionSelector An instance of the VersionSelector.
     */
    @Inject
    public RestEndpoints(RestRealmValidator realmValidator, VersionSelector versionSelector, CoreWrapper coreWrapper,
            RestletAccessAuditFilterFactory restletAuditFactory) {
        this(realmValidator, versionSelector, coreWrapper, restletAuditFactory,
                InvalidRealmNameManager.getInvalidRealmNames());
    }

    RestEndpoints(RestRealmValidator realmValidator, VersionSelector versionSelector, CoreWrapper coreWrapper,
            RestletAccessAuditFilterFactory restletAuditFactory, Set<String> invalidRealmNames) {
        this.realmValidator = realmValidator;
        this.versionSelector = versionSelector;
        this.coreWrapper = coreWrapper;
        this.restletAuditFactory = restletAuditFactory;

        this.jsonServiceRouter = createJSONServiceRouter(invalidRealmNames);
        this.xacmlServiceRouter = createXACMLServiceRouter(invalidRealmNames);
        this.umaServiceRouter = createUMAServiceRouter();
        this.oauth2ServiceRouter = createOAuth2Router();
    }

    /**
     * Gets the JSON restlet service router.
     * @return The router.
     */
    public ServiceRouter getJSONServiceRouter() {
        return jsonServiceRouter;
    }

    /**
     * Gets the XACML restlet service router.
     * @return The router.
     */
    public ServiceRouter getXACMLServiceRouter() {
        return xacmlServiceRouter;
    }

    /**
     * Gets the UMA restlet service router.
     * @return The router.
     */
    public Router getUMAServiceRouter() {
        return umaServiceRouter;
    }

    /**
     * Gets the OAuth2 restlet service router.
     * @return The router.
     */
    public Router getOAuth2ServiceRouter() {
        return oauth2ServiceRouter;
    }

    /**
     * Constructs a new {@link ServiceRouter} with routes to each of the Restlet service endpoints.
     *
     * @return A {@code ServiceRouter}.
     */
    private ServiceRouter createJSONServiceRouter(final Set<String> invalidRealmNames) {

        ServiceRouter router = new ServiceRouter(realmValidator, versionSelector, coreWrapper);

        router.addRoute("/authenticate")
                .addVersion("1.1",
                        restletAuditFactory.createFilter(Component.AUTHENTICATION, wrap(AuthenticationServiceV1.class)))
                .addVersion("2.0",
                        restletAuditFactory.createFilter(Component.AUTHENTICATION, wrap(AuthenticationServiceV2.class)));
        invalidRealmNames.add("authenticate");

        VersionBehaviourConfigListener.bindToServiceConfigManager(router);

        return router;
    }

    /**
     * Constructs a new {@link ServiceRouter} with routes to each of the Restlet service endpoints.
     *
     * @return A {@code ServiceRouter}.
     */
    private ServiceRouter createXACMLServiceRouter(final Set<String> invalidRealmNames) {

        ServiceRouter router = new ServiceRouter(realmValidator, versionSelector, coreWrapper);

        router.addRoute("/policies")
                .addVersion("1.0", wrap(XacmlService.class));
        invalidRealmNames.add("policies");

        VersionBehaviourConfigListener.bindToServiceConfigManager(router);

        return router;
    }

    private Router createUMAServiceRouter() {

        Router router = new RestletRealmRouter(realmValidator, coreWrapper);

        router.attach("/permission_request", getRestlet(UmaConstants.PERMISSION_REQUEST_ENDPOINT));
        router.attach("/authz_request", getRestlet(UmaConstants.AUTHORIZATION_REQUEST_ENDPOINT));

        // Well-Known Discovery

        router.attach("/.well-known/uma-configuration",
                new UmaExceptionFilter(wrap(UmaWellKnownConfigurationEndpoint.class)));

        return router;
    }

    private Restlet getRestlet(String name) {
        return InjectorHolder.getInstance(Key.get(Restlet.class, Names.named(name)));
    }

    private Router createOAuth2Router() {
        final Router router = new RestletRealmRouter(realmValidator, coreWrapper);

        // Standard OAuth2 endpoints

        router.attach("/authorize", new AuthorizeEndpointFilter(wrap(AuthorizeResource.class)));
        router.attach("/access_token", new TokenEndpointFilter(new AccessTokenFlowFinder()));
        router.attach("/tokeninfo", wrap(ValidationServerResource.class));

        // OAuth 2.0 Token Introspection Endpoint
        router.attach("/introspect", wrap(TokenIntrospectionResource.class));

        // OpenID Connect endpoints

        router.attach("/connect/register", wrap(ConnectClientRegistration.class));
        router.attach("/userinfo", wrap(UserInfo.class));
        router.attach("/connect/endSession", wrap(EndSession.class));
        router.attach("/connect/jwk_uri", wrap(OpenIDConnectJWKEndpoint.class));

        // Resource Set Registration

        Restlet resourceSetRegistrationEndpoint = getRestlet(OAuth2Constants.Custom.RSR_ENDPOINT);
        router.attach("/resource_set/{rsid}", resourceSetRegistrationEndpoint);
        router.attach("/resource_set", resourceSetRegistrationEndpoint);
        router.attach("/resource_set/", resourceSetRegistrationEndpoint);

        // OpenID Connect Discovery

        router.attach("/.well-known/openid-configuration", wrap(OpenIDConnectConfiguration.class));

        return router;
    }
}
