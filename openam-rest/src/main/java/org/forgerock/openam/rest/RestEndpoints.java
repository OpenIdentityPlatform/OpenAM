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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.VersionSelector;
import org.forgerock.openam.core.CoreWrapper;
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
import org.forgerock.openam.forgerockrest.entitlements.SubjectAttributesResourceV1;
import org.forgerock.openam.forgerockrest.entitlements.SubjectTypesResource;
import org.forgerock.openam.forgerockrest.server.ServerInfoResource;
import org.forgerock.openam.forgerockrest.session.SessionResource;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.rest.authz.CoreTokenResourceAuthzModule;
import org.forgerock.openam.rest.authz.PrivilegeAuthzModule;
import org.forgerock.openam.rest.authz.SessionResourceAuthzModule;
import org.forgerock.openam.rest.dashboard.DashboardResource;
import org.forgerock.openam.rest.dashboard.TrustedDevicesResource;
import org.forgerock.openam.rest.fluent.FluentRealmRouter;
import org.forgerock.openam.rest.fluent.FluentRouter;
import org.forgerock.openam.rest.fluent.LoggingFluentRouter;
import org.forgerock.openam.rest.resource.CrestRouter;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.router.VersionBehaviourConfigListener;
import org.forgerock.openam.rest.service.ServiceRouter;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;

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

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param versionSelector An instance of the VersionSelector.
     */
    @Inject
    public RestEndpoints(RestRealmValidator realmValidator, VersionSelector versionSelector, CoreWrapper coreWrapper) {
        this.realmValidator = realmValidator;
        this.versionSelector = versionSelector;
        this.coreWrapper = coreWrapper;

        this.resourceRouter = createResourceRouter();
        this.jsonServiceRouter = createJSONServiceRouter();
        this.xacmlServiceRouter = createXACMLServiceRouter();
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
     * Constructs a new {@link org.forgerock.openam.rest.resource.CrestRealmRouter} with routes to each of the CREST
     * resource endpoints.
     *
     * @return A {@code RealmRouter}.
     */
    private CrestRouter createResourceRouter() {

        FluentRouter rootRealmRouter = InjectorHolder.getInstance(LoggingFluentRouter.class);
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

        rootRealmRouter.route("/applicationtypes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(ApplicationTypesResource.class);

        rootRealmRouter.route("/decisioncombiners")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(DecisionCombinersResource.class);

        rootRealmRouter.route("/conditiontypes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(ConditionTypesResource.class);

        rootRealmRouter.route("/subjecttypes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(SubjectTypesResource.class);

        rootRealmRouter.route("/subjectattributes")
                .through(PrivilegeAuthzModule.class, PrivilegeAuthzModule.NAME)
                .forVersion("1.0").to(SubjectAttributesResourceV1.class);

        rootRealmRouter.route("/tokens")
                .through(CoreTokenResourceAuthzModule.class, CoreTokenResourceAuthzModule.NAME)
                .forVersion("1.0").to(CoreTokenResource.class);

        VersionBehaviourConfigListener.bindToServiceConfigManager(rootRealmRouter);
        VersionBehaviourConfigListener.bindToServiceConfigManager(dynamicRealmRouter);

        return rootRealmRouter;
    }

    /**
     * Constructs a new {@link ServiceRouter} with routes to each of the Restlet service endpoints.
     *
     * @return A {@code ServiceRouter}.
     */
    private ServiceRouter createJSONServiceRouter() {

        ServiceRouter router = new ServiceRouter(realmValidator, versionSelector, coreWrapper);

        router.addRoute("/authenticate")
                .addVersion("1.1", wrap(AuthenticationServiceV1.class))
                .addVersion("2.0", wrap(AuthenticationServiceV2.class));

        VersionBehaviourConfigListener.bindToServiceConfigManager(router);

        return router;
    }

    /**
     * Constructs a new {@link ServiceRouter} with routes to each of the Restlet service endpoints.
     *
     * @return A {@code ServiceRouter}.
     */
    private ServiceRouter createXACMLServiceRouter() {

        ServiceRouter router = new ServiceRouter(realmValidator, versionSelector, coreWrapper);

        router.addRoute("/policies")
                .addVersion("1.0", wrap(XacmlService.class));

        VersionBehaviourConfigListener.bindToServiceConfigManager(router);

        return router;
    }

    private Restlet wrap(final Class<? extends ServerResource> resource) {
        return new Finder() {
            @Override
            public ServerResource create(Request request, Response response) {
                return InjectorHolder.getInstance(resource);
            }
        };
    }

}
