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

import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.VersionSelector;
import org.forgerock.openam.forgerockrest.IdentityResource;
import org.forgerock.openam.forgerockrest.RealmResource;
import org.forgerock.openam.forgerockrest.authn.restlet.AuthenticationService;
import org.forgerock.openam.forgerockrest.cts.CoreTokenResource;
import org.forgerock.openam.forgerockrest.entitlements.ApplicationTypesResource;
import org.forgerock.openam.forgerockrest.entitlements.ApplicationsResource;
import org.forgerock.openam.forgerockrest.entitlements.ConditionTypesResource;
import org.forgerock.openam.forgerockrest.entitlements.DecisionCombinersResource;
import org.forgerock.openam.forgerockrest.entitlements.PolicyResource;
import org.forgerock.openam.forgerockrest.entitlements.SubjectTypesResource;
import org.forgerock.openam.forgerockrest.server.ServerInfoResource;
import org.forgerock.openam.forgerockrest.session.SessionResource;
import org.forgerock.openam.rest.dashboard.DashboardResource;
import org.forgerock.openam.rest.dashboard.TrustedDevicesResource;
import org.forgerock.openam.rest.resource.CrestRealmRouter;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.router.VersionBehaviourConfigListener;
import org.forgerock.openam.rest.router.VersionRouter;
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
    private final VersionRouter resourceRouter;
    private final ServiceRouter serviceRouter;

    /**
     * Constructs a new RestEndpoints instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param versionSelector An instance of the VersionSelector.
     */
    @Inject
    public RestEndpoints(RestRealmValidator realmValidator, VersionSelector versionSelector) {
        this.realmValidator = realmValidator;
        this.versionSelector = versionSelector;

        this.resourceRouter = createResourceRouter();
        this.serviceRouter = createServiceRouter();
    }

    /**
     * Gets the CREST resource router.
     * @return The router.
     */
    public VersionRouter getResourceRouter() {
        return resourceRouter;
    }

    /**
     * Gets the restlet service router.
     * @return The router.
     */
    public ServiceRouter getServiceRouter() {
        return serviceRouter;
    }

    /**
     * Constructs a new {@link org.forgerock.openam.rest.resource.CrestRealmRouter} with routes to each of the CREST
     * resource endpoints.
     *
     * @return A {@code RealmRouter}.
     */
    private VersionRouter createResourceRouter() {

        final CrestRealmRouter cr = getRealmRouter();
        final VersionRouter vr = getRootRouter();

        //Initial RequestHandler for OpenAM, which ensures an SSOTokenContext is accessible
        //and advertises the routes of its two chained routers
        VersionRouter openAMRequestHandler = new VersionRouter() {

            protected ServerContext transformContext(ServerContext context) throws BadRequestException {
                return new SSOTokenContext(context);
            }

            public Set<String> getRoutes() {
                final HashSet<String> myRoutes = new HashSet<String>();
                myRoutes.addAll(vr.getRoutes());
                myRoutes.addAll(cr.getRoutes());
                return myRoutes;
            }
        };

        //if the openAMRequestHandler can't handle them, they will fall through to here
        vr.setDefaultRoute(cr);

        //all requests will default to the openAMRequestHandler first
        openAMRequestHandler.setDefaultRoute(vr);

        //no need to register the anonymous handler as a listener
        return openAMRequestHandler;
    }

    /**
     * Provides a realm-aware router with endpoints registered which
     */
    private CrestRealmRouter getRealmRouter() {
        final CrestRealmRouter cr = new CrestRealmRouter(realmValidator);

        cr.addRoute("/users")
                .addVersion("1.0", get(Key.get(IdentityResource.class, Names.named("UsersResource"))));

        cr.addRoute("/users/{user}/devices/trusted")
                .addVersion("1.0", get(TrustedDevicesResource.class));

        cr.addRoute("/groups")
                .addVersion("1.0", get(Key.get(IdentityResource.class, Names.named("GroupsResource"))));

        cr.addRoute("/agents")
                .addVersion("1.0", get(Key.get(IdentityResource.class, Names.named("AgentsResource"))));

        cr.addRoute("/realms")
                .addVersion("1.0", get(RealmResource.class));

        cr.addRoute("/dashboard")
                .addVersion("1.0", get(DashboardResource.class));

        cr.addRoute("/serverinfo")
                .addVersion("1.0", get(ServerInfoResource.class));

        cr.addRoute("/policies")
                .addVersion("1.0", get(PolicyResource.class));

        cr.addRoute("/applications")
                .addVersion("1.0", get(ApplicationsResource.class));

        VersionBehaviourConfigListener.bindToServiceConfigManager(cr);

        return cr;
    }

    /**
     * Provide a router which only handles requests for root-resources, and does not fancy realm-mapping
     */
    private VersionRouter getRootRouter() {

        final VersionRouter vr = new VersionRouter();

        vr.addRoute("/applicationtypes")
                .addVersion("1.0", get(ApplicationTypesResource.class));

        vr.addRoute("/conditiontypes")
                .addVersion("1.0", get(ConditionTypesResource.class));

        vr.addRoute("/subjecttypes")
                .addVersion("1.0", get(SubjectTypesResource.class));

        vr.addRoute("/decisioncombiners")
                .addVersion("1.0", get(DecisionCombinersResource.class));

        vr.addRoute("/tokens")
                .addVersion("1.0", get(CoreTokenResource.class));

        vr.addRoute("/sessions")
                .addVersion("1.0", get(SessionResource.class));

        VersionBehaviourConfigListener.bindToServiceConfigManager(vr);

        return vr;
    }

    /**
     * Constructs a new {@link ServiceRouter} with routes to each of the Restlet service endpoints.
     *
     * @return A {@code ServiceRouter}.
     */
    private ServiceRouter createServiceRouter() {

        ServiceRouter router = new ServiceRouter(realmValidator, versionSelector);

        router.addRoute("/authenticate")
                .addVersion("1.0", wrap(AuthenticationService.class));

        VersionBehaviourConfigListener.bindToServiceConfigManager(router);

        return router;
    }

    private <T> T get(Class<T> resourceClass) {
        return InjectorHolder.getInstance(resourceClass);
    }

    private <T> T get(Key<T> resourceKey) {
        return InjectorHolder.getInstance(resourceKey);
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
