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

package org.forgerock.openam.rest;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.openam.http.HttpRoute.newHttpRoute;
import static org.forgerock.openam.rest.Routers.ssoToken;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.sm.SchemaType;
import org.forgerock.audit.AuditService;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Handler;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.http.routing.Router;
import org.forgerock.openam.forgerockrest.AuditHistory;
import org.forgerock.openam.forgerockrest.IdentityResourceV1;
import org.forgerock.openam.forgerockrest.IdentityResourceV2;
import org.forgerock.openam.forgerockrest.RealmResource;
import org.forgerock.openam.forgerockrest.UmaLabelResource;
import org.forgerock.openam.forgerockrest.authn.http.AuthenticationServiceV1;
import org.forgerock.openam.forgerockrest.authn.http.AuthenticationServiceV2;
import org.forgerock.openam.forgerockrest.cts.CoreTokenResource;
import org.forgerock.openam.forgerockrest.entitlements.ApplicationTypesResource;
import org.forgerock.openam.forgerockrest.entitlements.ApplicationV1Filter;
import org.forgerock.openam.forgerockrest.entitlements.ApplicationsResource;
import org.forgerock.openam.forgerockrest.entitlements.ConditionTypesResource;
import org.forgerock.openam.forgerockrest.entitlements.DecisionCombinersResource;
import org.forgerock.openam.forgerockrest.entitlements.PolicyResource;
import org.forgerock.openam.forgerockrest.entitlements.PolicyV1Filter;
import org.forgerock.openam.forgerockrest.entitlements.ReferralsResourceV1;
import org.forgerock.openam.forgerockrest.entitlements.ResourceTypesResource;
import org.forgerock.openam.forgerockrest.entitlements.SubjectAttributesResourceV1;
import org.forgerock.openam.forgerockrest.entitlements.SubjectTypesResource;
import org.forgerock.openam.forgerockrest.server.ServerInfoResource;
import org.forgerock.openam.forgerockrest.session.SessionResource;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.http.annotations.Endpoints;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.rest.authz.AgentOnlyAuthzModule;
import org.forgerock.openam.rest.authz.CoreTokenResourceAuthzModule;
import org.forgerock.openam.rest.authz.PrivilegeAuthzModule;
import org.forgerock.openam.rest.authz.ResourceOwnerOrSuperUserAuthzModule;
import org.forgerock.openam.rest.authz.SessionResourceAuthzModule;
import org.forgerock.openam.rest.batch.BatchResource;
import org.forgerock.openam.rest.dashboard.DashboardResource;
import org.forgerock.openam.rest.devices.OathDevicesResource;
import org.forgerock.openam.rest.devices.TrustedDevicesResource;
import org.forgerock.openam.rest.oauth2.ResourceSetResource;
import org.forgerock.openam.rest.record.RecordConstants;
import org.forgerock.openam.rest.record.RecordResource;
import org.forgerock.openam.rest.scripting.ScriptResource;
import org.forgerock.openam.rest.sms.SmsRequestHandlerFactory;
import org.forgerock.openam.rest.sms.SmsServerPropertiesResource;
import org.forgerock.openam.rest.uma.PendingRequestResource;
import org.forgerock.openam.rest.uma.UmaConfigurationResource;
import org.forgerock.openam.rest.uma.UmaEnabledFilter;
import org.forgerock.openam.rest.uma.UmaPolicyResource;
import org.forgerock.openam.rest.uma.UmaPolicyResourceAuthzFilter;

public class RestHttpRouteProvider implements HttpRouteProvider {

    private SmsRequestHandlerFactory smsRequestHandlerFactory;
    private Set<String> invalidRealms = new HashSet<>();
    private RestRouter rootRouter;
    private RestRouter realmRouter;
    private Router chfRealmRouter;

    @Inject
    public void setSmsRequestHandlerFactory(SmsRequestHandlerFactory smsRequestHandlerFactory) {
        this.smsRequestHandlerFactory = smsRequestHandlerFactory;
    }

    @Inject
    public void setInvalidRealms(@Named("InvalidRealmNames") Set<String> invalidRealms) {
        this.invalidRealms = invalidRealms;
    }

    @Inject
    public void setRealmRouter(@Named("RestRealmRouter") Router realmRouter) {
        this.chfRealmRouter = realmRouter;
    }

    @Inject
    public void setRouters(@Named("RestRouter") DynamicRealmRestRouter router) {
        this.rootRouter = router;
        this.realmRouter = router.dynamically();
    }

    @Override
    public Set<HttpRoute> get() {
        addJsonRoutes(invalidRealms);
        return Collections.singleton(
                newHttpRoute(STARTS_WITH, "json", Key.get(Handler.class, Names.named("RestHandler"))));
    }

    private Handler createAuthenticateHandler() {
        Router authenticateVersionRouter = new Router();
        Handler authenticateHandlerV1 = Endpoints.from(AuthenticationServiceV1.class);
        Handler authenticateHandlerV2 = Endpoints.from(AuthenticationServiceV2.class);
        // TODO need to do auditing
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(1, 1)), authenticateHandlerV1);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(2)), authenticateHandlerV2);
        //TODO authentication filter?
        return authenticateVersionRouter;
    }

    private void addJsonRoutes(final Set<String> invalidRealmNames) {

        // ------------------
        // Realm based routes
        // ------------------
        //not protected
        chfRealmRouter.addRoute(RouteMatchers.requestUriMatcher(EQUALS, "authenticate"), createAuthenticateHandler());
        invalidRealmNames.add(firstPathSegment("authenticate"));

        realmRouter.route("dashboard")
                .auditAs(DASHBOARD)
                .toCollection(DashboardResource.class);

        realmRouter.route("serverinfo")
                .authenticateWith(ssoToken().exceptRead())
                .auditAs(SERVER_INFO)
                .forVersion(1, 1)
                .toCollection(ServerInfoResource.class);

        realmRouter.route("serverinfo/uma")
                .auditAs(UMA)
                .toSingleton(UmaConfigurationResource.class);

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
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
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

        realmRouter.route("users/{user}/oauth2/resources/sets")
                .auditAs(OAUTH2)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .through(UmaEnabledFilter.class)
                .toCollection(ResourceSetResource.class);

        realmRouter.route("users/{user}/uma/policies")
                .auditAs(UMA)
                .authorizeWith(UmaPolicyResourceAuthzFilter.class)
                .through(UmaEnabledFilter.class)
                .toCollection(UmaPolicyResource.class);

        realmRouter.route("users/{user}/uma/auditHistory")
                .auditAs(UMA)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .through(UmaEnabledFilter.class)
                .toCollection(AuditHistory.class);

        realmRouter.route("users/{user}/uma/pendingrequests")
                .auditAs(UMA)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .through(UmaEnabledFilter.class)
                .toCollection(PendingRequestResource.class);

        realmRouter.route("users/{user}/oauth2/resources/labels")
                .auditAs(OAUTH2)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .toCollection(UmaLabelResource.class);


        //protected
        realmRouter.route("policies")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .forVersion(1)
                .through(PolicyV1Filter.class)
                .toCollection(PolicyResource.class)
                .forVersion(2)
                .toCollection(PolicyResource.class);

        realmRouter.route("referrals")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(ReferralsResourceV1.class);

        realmRouter.route("realms")
                .auditAs(REALMS)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(RealmResource.class);

        realmRouter.route("sessions")
                .authenticateWith(ssoToken().exceptActions("validate"))
                .auditAs(SESSION)
                .authorizeWith(SessionResourceAuthzModule.class)
                .forVersion(1, 1)
                .toCollection(SessionResource.class);

        realmRouter.route("applications")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .forVersion(1)
                .through(ApplicationV1Filter.class)
                .toCollection(ApplicationsResource.class)
                .forVersion(2)
                .toCollection(ApplicationsResource.class);

        realmRouter.route("subjectattributes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(SubjectAttributesResourceV1.class);

        rootRouter.route("applicationtypes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(ApplicationTypesResource.class);

        realmRouter.route("resourcetypes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(ResourceTypesResource.class);

        realmRouter.route("scripts")
                .auditAs(SCRIPT)
                .authorizeWith(AdminOnlyAuthzModule.class)
                .toCollection(ScriptResource.class);

        realmRouter.route("realm-config")
                .auditAs(CONFIG)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toRequestHandler(STARTS_WITH, smsRequestHandlerFactory.create(SchemaType.ORGANIZATION));

        realmRouter.route("batch")
                .auditAs(BATCH)
                .authorizeWith(AdminOnlyAuthzModule.class)
                .toCollection(BatchResource.class);


        // ------------------
        // Global routes
        // ------------------
        rootRouter.route("decisioncombiners")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(DecisionCombinersResource.class);

        rootRouter.route("conditiontypes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(ConditionTypesResource.class);

        rootRouter.route("subjecttypes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(SubjectTypesResource.class);

        rootRouter.route("tokens")
                .auditAs(CTS)
                .authorizeWith(CoreTokenResourceAuthzModule.class)
                .toCollection(CoreTokenResource.class);

        rootRouter.route("global-config")
                .auditAs(CONFIG)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toRequestHandler(STARTS_WITH, smsRequestHandlerFactory.create(SchemaType.GLOBAL));

        rootRouter.route("global-config/servers/{serverName}/properties/{tab}")
                .auditAs(CONFIG)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toSingleton(SmsServerPropertiesResource.class);

        rootRouter.route("audit")
                .auditAs(AUDIT)
                .authorizeWith(AgentOnlyAuthzModule.class)
                .toRequestHandler(STARTS_WITH, AuditService.class);

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
}
