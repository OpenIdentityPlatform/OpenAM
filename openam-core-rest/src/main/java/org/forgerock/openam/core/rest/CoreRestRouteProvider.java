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

package org.forgerock.openam.core.rest;

import static org.forgerock.http.handler.Handlers.chainOf;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.openam.rest.Routers.ssoToken;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.http.Handler;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.http.routing.Router;
import org.forgerock.openam.core.rest.authn.http.AuthenticationServiceV1;
import org.forgerock.openam.core.rest.authn.http.AuthenticationServiceV2;
import org.forgerock.openam.core.rest.cts.CoreTokenResource;
import org.forgerock.openam.core.rest.dashboard.DashboardResource;
import org.forgerock.openam.core.rest.server.ServerInfoResource;
import org.forgerock.openam.core.rest.session.SessionResource;
import org.forgerock.openam.http.annotations.Endpoints;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.RestRouter;
import org.forgerock.openam.core.rest.authn.HttpAccessAuditFilterFactory;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.core.rest.cts.CoreTokenResourceAuthzModule;
import org.forgerock.openam.rest.authz.ResourceOwnerOrSuperUserAuthzModule;
import org.forgerock.openam.core.rest.session.SessionResourceAuthzModule;
import org.forgerock.openam.core.rest.devices.OathDevicesResource;
import org.forgerock.openam.core.rest.devices.TrustedDevicesResource;
import org.forgerock.openam.core.rest.record.RecordConstants;
import org.forgerock.openam.core.rest.record.RecordResource;

/**
 * A {@link RestRouteProvider} that add routes for all the core endpoints.
 *
 * @since 13.0.0
 */
public class CoreRestRouteProvider implements RestRouteProvider {

    private Set<String> invalidRealms = new HashSet<>();
    private Router chfRealmRouter;
    private HttpAccessAuditFilterFactory httpAuditFactory;

    private Handler createAuthenticateHandler() {
        Router authenticateVersionRouter = new Router();
        Handler authenticateHandlerV1 = Endpoints.from(AuthenticationServiceV1.class);
        Handler authenticateHandlerV2 = Endpoints.from(AuthenticationServiceV2.class);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(1, 1)), authenticateHandlerV1);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(2)), authenticateHandlerV2);
        return chainOf(authenticateVersionRouter, httpAuditFactory.createFilter(AUTHENTICATION));
    }

    @Override
    public void addRoutes(RestRouter rootRouter, RestRouter realmRouter) {
        chfRealmRouter.addRoute(RouteMatchers.requestUriMatcher(EQUALS, "authenticate"), createAuthenticateHandler());
        invalidRealms.add(firstPathSegment("authenticate"));

        realmRouter.route("dashboard")
                .auditAs(DASHBOARD)
                .toCollection(DashboardResource.class);

        realmRouter.route("serverinfo")
                .authenticateWith(ssoToken().exceptRead())
                .auditAs(SERVER_INFO)
                .forVersion(1, 1)
                .toCollection(ServerInfoResource.class);

        realmRouter.route("users")
                .authenticateWith(ssoToken().exceptActions("register", "confirm", "forgotPassword",
                        "forgotPasswordReset", "anonymousCreate"))
                .auditAs(USERS)
                .forVersion(1, 2)
                .toCollection(Key.get(IdentityResourceV1.class, Names.named("UsersResource")))
                .forVersion(2, 1)
                .toCollection(Key.get(IdentityResourceV2.class, Names.named("UsersResource")));

        realmRouter.route("groups")
                .auditAs(GROUPS)
                .forVersion(1, 2)
                .toCollection(Key.get(IdentityResourceV1.class, Names.named("GroupsResource")))
                .forVersion(2, 1)
                .toCollection(Key.get(IdentityResourceV2.class, Names.named("GroupsResource")));

        realmRouter.route("agents")
                .auditAs(POLICY_AGENT)
                .forVersion(1, 2)
                .toCollection(Key.get(IdentityResourceV1.class, Names.named("AgentsResource")))
                .forVersion(2, 1)
                .toCollection(Key.get(IdentityResourceV2.class, Names.named("AgentsResource")));

        realmRouter.route("users/{user}/devices/trusted")
                .auditAs(DEVICES)
                .toCollection(TrustedDevicesResource.class);

        realmRouter.route("users/{user}/devices/2fa/oath")
                .auditAs(DEVICES)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .toCollection(OathDevicesResource.class);

        realmRouter.route("sessions")
                .authenticateWith(ssoToken().exceptActions("validate"))
                .auditAs(SESSION)
                .authorizeWith(SessionResourceAuthzModule.class)
                .forVersion(1, 1)
                .toCollection(SessionResource.class);

        rootRouter.route("tokens")
                .auditAs(CTS)
                .authorizeWith(CoreTokenResourceAuthzModule.class)
                .toCollection(CoreTokenResource.class);

        rootRouter.route(RecordConstants.RECORD_REST_ENDPOINT)
                .auditAs(RECORD)
                .authorizeWith(AdminOnlyAuthzModule.class)
                .toCollection(RecordResource.class);
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

    @Inject
    public void setInvalidRealms(@Named("InvalidRealmNames") Set<String> invalidRealms) {
        this.invalidRealms = invalidRealms;
    }

    @Inject
    public void setChfRealmRouter(@Named("RestRealmRouter") Router chfRealmRouter) {
        this.chfRealmRouter = chfRealmRouter;
    }

    @Inject
    public void setHttpAuditFactory(HttpAccessAuditFilterFactory httpAuditFactory) {
        this.httpAuditFactory = httpAuditFactory;
    }
}
