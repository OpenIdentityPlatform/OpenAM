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

import static org.forgerock.openam.rest.service.RestletUtils.*;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

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
import org.forgerock.openam.forgerockrest.AuditHistory;
import org.forgerock.openam.forgerockrest.IdentityResourceV1;
import org.forgerock.openam.forgerockrest.IdentityResourceV2;
import org.forgerock.openam.forgerockrest.RealmResource;
import org.forgerock.openam.forgerockrest.XacmlService;
import org.forgerock.openam.forgerockrest.authn.restlet.AuthenticationServiceV1;
import org.forgerock.openam.forgerockrest.authn.restlet.AuthenticationServiceV2;
import org.forgerock.openam.forgerockrest.cts.CoreTokenResource;
import org.forgerock.openam.forgerockrest.entitlements.ApplicationTypesResource;
import org.forgerock.openam.forgerockrest.entitlements.ApplicationsResource;
import org.forgerock.openam.forgerockrest.entitlements.ConditionTypesResource;
import org.forgerock.openam.forgerockrest.entitlements.DecisionCombinersResource;
import org.forgerock.openam.forgerockrest.entitlements.PolicyResource;
import org.forgerock.openam.forgerockrest.entitlements.ReferralsResourceV1;
import org.forgerock.openam.forgerockrest.entitlements.ResourceTypesResource;
import org.forgerock.openam.forgerockrest.entitlements.SubjectAttributesResourceV1;
import org.forgerock.openam.forgerockrest.entitlements.SubjectTypesResource;
import org.forgerock.openam.forgerockrest.server.ServerInfoResource;
import org.forgerock.openam.forgerockrest.session.SessionResource;
import org.forgerock.openam.rest.authz.*;
import org.forgerock.openam.rest.dashboard.DashboardResource;
import org.forgerock.openam.rest.dashboard.TrustedDevicesResource;
import org.forgerock.openam.rest.fluent.FluentRealmRouter;
import org.forgerock.openam.rest.fluent.FluentRoute;
import org.forgerock.openam.rest.fluent.FluentRouter;
import org.forgerock.openam.rest.fluent.LoggingFluentRouter;
import org.forgerock.openam.rest.oauth2.ResourceSetResource;
import org.forgerock.openam.rest.resource.CrestRouter;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.router.VersionBehaviourConfigListener;
import org.forgerock.openam.rest.scripting.ScriptResource;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.rest.service.ServiceRouter;
import org.forgerock.openam.rest.uma.UmaPolicyResource;
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

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.sm.InvalidRealmNameManager;

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
    private final CrestRouter resourceRouter;
    private final ServiceRouter jsonServiceRouter;
    private final ServiceRouter xacmlServiceRouter;
    private final Router umaServiceRouter;
    private final Router oauth2ServiceRouter;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param versionSelector An instance of the VersionSelector.
     */
    @Inject
    public RestEndpoints(RestRealmValidator realmValidator, VersionSelector versionSelector, CoreWrapper coreWrapper) {
        this(realmValidator, versionSelector, coreWrapper, InvalidRealmNameManager.getInvalidRealmNames());
    }

    RestEndpoints(RestRealmValidator realmValidator, VersionSelector versionSelector, CoreWrapper coreWrapper,
                  Set<String> invalidRealmNames) {
        this.realmValidator = realmValidator;
        this.versionSelector = versionSelector;
        this.coreWrapper = coreWrapper;

        this.resourceRouter = createResourceRouter(invalidRealmNames);
        this.jsonServiceRouter = createJSONServiceRouter(invalidRealmNames);
        this.xacmlServiceRouter = createXACMLServiceRouter(invalidRealmNames);
        this.umaServiceRouter = createUMAServiceRouter();
        this.oauth2ServiceRouter = createOAuth2Router();
    }

    /**
     * Gets the CREST resource router.
     * @return The router.
     */
    public CrestRouter getResourceRouter() {
        return resourceRouter;
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
     * Constructs a new {@link org.forgerock.openam.rest.resource.CrestRealmRouter} with routes to each of the CREST
     * resource endpoints.
     *
     * @return A {@code RealmRouter}.
     */
    private CrestRouter createResourceRouter(final Set<String> invalidRealmNames) {

        FluentRouter rootRealmRouterDelegate = InjectorHolder.getInstance(LoggingFluentRouter.class);

        // Ensure all routes are added to the realm name blacklist
        FluentRouter rootRealmRouter = new RealmBlackListingFluentRouter(rootRealmRouterDelegate, invalidRealmNames);
        FluentRealmRouter dynamicRealmRouter = rootRealmRouter.dynamically();

        //not protected
        dynamicRealmRouter.route("/dashboard")
                .forVersion("1.0").to(DashboardResource.class);

        dynamicRealmRouter.route("/serverinfo")
                .forVersion("1.1").to(ServerInfoResource.class);

        dynamicRealmRouter.route("/users")
                .forVersion("1.1").to(IdentityResourceV1.class, "UsersResource")
                .forVersion("2.0").to(IdentityResourceV2.class, "UsersResource");

        dynamicRealmRouter.route("/groups")
                .forVersion("1.1").to(IdentityResourceV1.class, "GroupsResource")
                .forVersion("2.0").to(IdentityResourceV2.class, "GroupsResource");

        dynamicRealmRouter.route("/agents")
                .forVersion("1.1").to(IdentityResourceV1.class, "AgentsResource")
                .forVersion("2.0").to(IdentityResourceV2.class, "AgentsResource");

        dynamicRealmRouter.route("/users/{user}/devices/trusted")
                .forVersion("1.0").to(TrustedDevicesResource.class);

        dynamicRealmRouter.route("/users/{user}/oauth2/resourcesets")
                .through(ResourceOwnerOrSuperUserAuthzModule.class, ResourceOwnerOrSuperUserAuthzModule.NAME)
                .forVersion("1.0").to(ResourceSetResource.class);

        dynamicRealmRouter.route("/users/{user}/uma/policies")
                .through(ResourceOwnerOrSuperUserAuthzModule.class, ResourceOwnerOrSuperUserAuthzModule.NAME)
                .forVersion("1.0").to(UmaPolicyResource.class);

        dynamicRealmRouter.route("/users/{user}/uma/auditHistory")
                .forVersion("1.0").to(AuditHistory.class);

        //protected
        dynamicRealmRouter.route("/policies")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(PolicyResource.class);

        dynamicRealmRouter.route("/referrals")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(ReferralsResourceV1.class);

        dynamicRealmRouter.route("/realms")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(RealmResource.class);

        dynamicRealmRouter.route("/sessions")
                .through(SessionResourceAuthzModule.class, SessionResourceAuthzModule.NAME)
                .forVersion("1.1").to(SessionResource.class);

        dynamicRealmRouter.route("/applications")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(ApplicationsResource.class);

        dynamicRealmRouter.route("/subjectattributes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(SubjectAttributesResourceV1.class);

        rootRealmRouter.route("/applicationtypes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(ApplicationTypesResource.class);

        dynamicRealmRouter.route("/resourcetypes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(ResourceTypesResource.class);

        rootRealmRouter.route("/decisioncombiners")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(DecisionCombinersResource.class);

        rootRealmRouter.route("/conditiontypes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(ConditionTypesResource.class);

        rootRealmRouter.route("/subjecttypes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(SubjectTypesResource.class);

        rootRealmRouter.route("/tokens")
                .through(CoreTokenResourceAuthzModule.class, CoreTokenResourceAuthzModule.NAME)
                .forVersion("1.0").to(CoreTokenResource.class);

        dynamicRealmRouter.route("/scripts")
                .through(AdminOnlyAuthzModule.class, AdminOnlyAuthzModule.NAME)
                .forVersion("1.0").to(ScriptResource.class);

        VersionBehaviourConfigListener.bindToServiceConfigManager(rootRealmRouter);
        VersionBehaviourConfigListener.bindToServiceConfigManager(dynamicRealmRouter);

        return rootRealmRouterDelegate;
    }

    /**
     * Constructs a new {@link ServiceRouter} with routes to each of the Restlet service endpoints.
     *
     * @return A {@code ServiceRouter}.
     */
    private ServiceRouter createJSONServiceRouter(final Set<String> invalidRealmNames) {

        ServiceRouter router = new ServiceRouter(realmValidator, versionSelector, coreWrapper);

        router.addRoute("/authenticate")
                .addVersion("1.1", wrap(AuthenticationServiceV1.class))
                .addVersion("2.0", wrap(AuthenticationServiceV2.class));
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

    /**
     * Decorator realm router that ensures that any REST endpoint route names are automatically added to the
     * realm name black-list to prevent clashes.
     */
    private static class RealmBlackListingFluentRealmRouter implements FluentRealmRouter {
        private final FluentRealmRouter delegate;
        private final Set<String> invalidRealmNames;

        RealmBlackListingFluentRealmRouter(final FluentRealmRouter delegate, final Set<String> invalidRealmNames) {
            this.delegate = delegate;
            this.invalidRealmNames = invalidRealmNames;
        }

        @Override
        public FluentRoute route(final String uriTemplate) {
            invalidRealmNames.add(firstPathSegment(uriTemplate));
            return delegate.route(uriTemplate);
        }

        @Override
        public FluentRealmRouter setVersioning(final DefaultVersionBehaviour behaviour) {
            delegate.setVersioning(behaviour);
            return this;
        }

        @Override
        public FluentRealmRouter setHeaderWarningEnabled(final boolean warningEnabled) {
            delegate.setHeaderWarningEnabled(warningEnabled);
            return this;
        }

    }
    /**
     * Decorator router that ensures that any REST endpoint route names are automatically added to the
     * realm name black-list to prevent clashes.
     */
    private static class RealmBlackListingFluentRouter extends FluentRouter {
        private final FluentRouter delegate;
        private final Set<String> invalidRealmNames;

        public RealmBlackListingFluentRouter(final FluentRouter delegate, final Set<String> invalidRealmNames) {
            this.delegate = delegate;
            this.invalidRealmNames = invalidRealmNames;
        }

        @Override
        public FluentRoute route(String uriTemplate) {
            invalidRealmNames.add(firstPathSegment(uriTemplate));
            return delegate.route(uriTemplate);
        }

        @Override
        public FluentRealmRouter dynamically() {
            return new RealmBlackListingFluentRealmRouter(delegate.dynamically(), invalidRealmNames);
        }
    }

    /**
     * Returns the first path segment from a uri template. For example {@code /foo/bar} becomes {@code foo}.
     *
     * @param path the full uri template path.
     * @return the first non-empty path segment.
     * @throws IllegalArgumentException if the path contains no non-empty segments.
     */
    private static String firstPathSegment(final String path) {
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                return part;
            }
        }
        throw new IllegalArgumentException("uriTemplate " + path + " is invalid");
    }
}
