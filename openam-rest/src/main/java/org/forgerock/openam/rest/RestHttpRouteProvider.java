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

import static org.forgerock.authz.filter.crest.AuthorizationFilters.createFilter;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.RouteMatchers.requestResourceApiVersionMatcher;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;
import static org.forgerock.openam.http.HttpRoute.newHttpRoute;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
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
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.Resources;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.forgerockrest.IdentityResourceV1;
import org.forgerock.openam.forgerockrest.IdentityResourceV2;
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
import org.forgerock.openam.rest.authz.LoggingAuthzModule;
import org.forgerock.openam.rest.authz.PrivilegeAuthzModule;
import org.forgerock.openam.rest.authz.ResourceOwnerOrSuperUserAuthzModule;
import org.forgerock.openam.rest.authz.SessionResourceAuthzModule;
import org.forgerock.openam.rest.batch.BatchResource;
import org.forgerock.openam.rest.dashboard.DashboardResource;
import org.forgerock.openam.rest.devices.OathDevicesResource;
import org.forgerock.openam.rest.devices.TrustedDevicesResource;
import org.forgerock.openam.rest.fluent.AuditEndpointAuditFilter;
import org.forgerock.openam.rest.fluent.AuditFilter;
import org.forgerock.openam.rest.fluent.AuditFilterWrapper;
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
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

public class RestHttpRouteProvider implements HttpRouteProvider {

    private SmsRequestHandlerFactory smsRequestHandlerFactory;
    private Set<String> invalidRealms = new HashSet<>();
    private Provider<AuthenticationFilter> authenticationFilterProvider;
    private Router rootRouter;
    private Router realmRouter;
    private org.forgerock.json.resource.Router crestRootRouter;
    private org.forgerock.json.resource.Router crestRealmRouter;
    private Filter crestLoggingFilter;
    private Handler restHandler;
    private Filter contextFilter;

    @Inject
    public void setSmsRequestHandlerFactory(SmsRequestHandlerFactory smsRequestHandlerFactory) {
        this.smsRequestHandlerFactory = smsRequestHandlerFactory;
    }

    @Inject
    public void setInvalidRealms(@Named("InvalidRealmNames") Set<String> invalidRealms) {
        this.invalidRealms = invalidRealms;
    }

    @Inject
    public void setAuthenticationFilterProvider(
            @Named("RestAuthenticationFilter") Provider<AuthenticationFilter> authenticationFilterProvider) {
        this.authenticationFilterProvider = authenticationFilterProvider;
    }

    @Inject
    public void setRootRouter(@Named("RestRootRouter") Router rootRouter) {
        this.rootRouter = rootRouter;
    }

    @Inject
    public void setRealmRouter(@Named("RestRealmRouter") Router realmRouter) {
        this.realmRouter = realmRouter;
    }

    @Inject
    public void setCrestRootRouter(@Named("CrestRootRouter") org.forgerock.json.resource.Router crestRootRouter) {
        this.crestRootRouter = crestRootRouter;
    }

    @Inject
    public void setCrestRealmRouter(@Named("CrestRealmRouter") org.forgerock.json.resource.Router crestRealmRouter) {
        this.crestRealmRouter = crestRealmRouter;
    }

    @Inject
    public void setCrestLoggingFilter(@Named("LoggingFilter") Filter crestLoggingFilter) {
        this.crestLoggingFilter = crestLoggingFilter;
    }

    @Inject
    public void setRestHandler(@Named("RestHandler") Handler restHandler) {
        this.restHandler = restHandler;
    }
    
    @Inject
    public void setCrestContextFilter(@Named("ContextFilter") Filter contextFilter) {
        this.contextFilter = contextFilter;
    }

    @Override
    public Set<HttpRoute> get() {
        addJsonRoutes(invalidRealms);
        return Collections.singleton(
                newHttpRoute(STARTS_WITH, "json", new Function<Void, Handler, NeverThrowsException>() {
                    @Override
                    public Handler apply(Void value) {
                        return restHandler;
                    }
                }));
    }

    private Handler createAuthenticateHandler() {
        Router authenticateVersionRouter = new Router();
        Handler authenticateHandlerV1 = Endpoints.from(InjectorHolder.getInstance(AuthenticationServiceV1.class));
        Handler authenticateHandlerV2 = Endpoints.from(InjectorHolder.getInstance(AuthenticationServiceV2.class));
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(1)), authenticateHandlerV1);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(2)), authenticateHandlerV2);
        //TODO authentication filter?
        return authenticateVersionRouter;
    }

    private void addJsonRoutes(final Set<String> invalidRealmNames) {

        AuthenticationFilter defaultAuthenticationFilter = authenticationFilterProvider.get();

        // ------------------
        // Realm based routes
        // ------------------
        //not protected
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(EQUALS, "authenticate"), createAuthenticateHandler());
        invalidRealmNames.add(firstPathSegment("authenticate"));

        org.forgerock.json.resource.Router dashboardVersionRouter = new org.forgerock.json.resource.Router();
        dashboardVersionRouter.addRoute(version(1), InjectorHolder.getInstance(DashboardResource.class));
        AuditFilterWrapper dashboardAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.DASHBOARD);
        FilterChain dashboardFilterChain = new FilterChain(dashboardVersionRouter, defaultAuthenticationFilter, dashboardAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "dashboard"), newHttpHandler(new FilterChain(dashboardFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "dashboard"), new FilterChain(dashboardFilterChain, contextFilter, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("dashboard"));

        org.forgerock.json.resource.Router serverInfoVersionRouter = new org.forgerock.json.resource.Router();
        serverInfoVersionRouter.addRoute(version(1, 1), InjectorHolder.getInstance(ServerInfoResource.class));
        AuditFilterWrapper serverInfoAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.SERVER_INFO);
        Filter serverInfoAuthnFilter = authenticationFilterProvider.get().exceptRead();
        FilterChain serverInfoFilterChain = new FilterChain(serverInfoVersionRouter, serverInfoAuthnFilter, serverInfoAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "serverinfo"), newHttpHandler(new FilterChain(serverInfoFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "serverinfo"), new FilterChain(serverInfoFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("serverinfo"));

        org.forgerock.json.resource.Router umaServerInfoVersionRouter = new org.forgerock.json.resource.Router();
        umaServerInfoVersionRouter.addRoute(version(1), InjectorHolder.getInstance(UmaConfigurationResource.class));
        AuditFilterWrapper umaServerInfoAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.UMA);
        FilterChain umaServerInfoFilterChain = new FilterChain(umaServerInfoVersionRouter, defaultAuthenticationFilter, umaServerInfoAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "serverinfo/uma"), newHttpHandler(new FilterChain(umaServerInfoFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "serverinfo/uma"), new FilterChain(umaServerInfoFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("serverinfo/uma"));

        org.forgerock.json.resource.Router usersVersionRouter = new org.forgerock.json.resource.Router();
        usersVersionRouter.addRoute(version(1, 2), InjectorHolder.getInstance(Key.get(IdentityResourceV1.class, Names.named("UsersResource"))));
        usersVersionRouter.addRoute(version(2, 1), InjectorHolder.getInstance(Key.get(IdentityResourceV2.class, Names.named("UsersResource"))));
        AuditFilterWrapper usersAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.USERS);
        Filter usersAuthnFilter = authenticationFilterProvider.get().exceptActions("register", "confirm", "forgotPassword", "forgotPasswordReset", "anonymousCreate");
        FilterChain usersFilterChain = new FilterChain(usersVersionRouter, usersAuthnFilter, usersAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "users"), newHttpHandler(new FilterChain(usersFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users"), new FilterChain(usersFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("users"));

        org.forgerock.json.resource.Router groupsVersionRouter = new org.forgerock.json.resource.Router();
        groupsVersionRouter.addRoute(version(1, 2), InjectorHolder.getInstance(Key.get(IdentityResourceV1.class, Names.named("GroupsResource"))));
        groupsVersionRouter.addRoute(version(2, 1), InjectorHolder.getInstance(Key.get(IdentityResourceV2.class, Names.named("GroupsResource"))));
        AuditFilterWrapper groupsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.GROUPS);
        FilterChain groupsFilterChain = new FilterChain(groupsVersionRouter, defaultAuthenticationFilter, groupsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "groups"), newHttpHandler(new FilterChain(groupsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "groups"), new FilterChain(groupsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("groups"));

        org.forgerock.json.resource.Router agentsVersionRouter = new org.forgerock.json.resource.Router();
        agentsVersionRouter.addRoute(version(1, 2), InjectorHolder.getInstance(Key.get(IdentityResourceV1.class, Names.named("AgentsResource"))));
        agentsVersionRouter.addRoute(version(2, 1), InjectorHolder.getInstance(Key.get(IdentityResourceV2.class, Names.named("AgentsResource"))));
        AuditFilterWrapper agentsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY_AGENT);
        FilterChain agentsFilterChain = new FilterChain(agentsVersionRouter, defaultAuthenticationFilter, agentsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "agents"), newHttpHandler(new FilterChain(agentsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "agents"), new FilterChain(agentsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("agents"));

        org.forgerock.json.resource.Router trustedDevicesVersionRouter = new org.forgerock.json.resource.Router();
        trustedDevicesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(TrustedDevicesResource.class));
        AuditFilterWrapper trustedDevicesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.DEVICES);
        FilterChain trustedDevicesFilterChain = new FilterChain(trustedDevicesVersionRouter, defaultAuthenticationFilter, trustedDevicesAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "users/{user}/devices/trusted"), newHttpHandler(new FilterChain(trustedDevicesFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/devices/trusted"), new FilterChain(trustedDevicesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("users/{user}/devices/trusted"));

        org.forgerock.json.resource.Router oathDevicesVersionRouter = new org.forgerock.json.resource.Router();
        oathDevicesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(OathDevicesResource.class));
        AuditFilterWrapper oathDevicesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.DEVICES);
        FilterChain oathDevicesFilterChain = new FilterChain(oathDevicesVersionRouter, defaultAuthenticationFilter, oathDevicesAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "users/{user}/devices/2fa/oath"), newHttpHandler(new FilterChain(oathDevicesFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/devices/2fa/oath"), new FilterChain(oathDevicesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("users/{user}/devices/2fa/oath"));

        org.forgerock.json.resource.Router oauth2ResourceSetsVersionRouter = new org.forgerock.json.resource.Router();
        oauth2ResourceSetsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ResourceSetResource.class));
        FilterChain oauth2ResourceSetsAuthzFilter = createFilter(oauth2ResourceSetsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(UmaPolicyResourceAuthzFilter.class), UmaPolicyResourceAuthzFilter.NAME));
        AuditFilterWrapper oauth2ResourceSetsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.OAUTH2);
        FilterChain oauth2ResourceSetsFilterChain = new FilterChain(oauth2ResourceSetsAuthzFilter, defaultAuthenticationFilter, InjectorHolder.getInstance(UmaEnabledFilter.class), oauth2ResourceSetsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "users/{user}/oauth2/resources/sets"), newHttpHandler(new FilterChain(oauth2ResourceSetsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/oauth2/resources/sets"), new FilterChain(oauth2ResourceSetsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("users/{user}/oauth2/resources/sets"));

        org.forgerock.json.resource.Router umaPoliciesVersionRouter = new org.forgerock.json.resource.Router();
        umaPoliciesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(UmaPolicyResource.class));
        FilterChain umaPoliciesAuthzFilter = createFilter(umaPoliciesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(UmaPolicyResourceAuthzFilter.class), UmaPolicyResourceAuthzFilter.NAME));
        AuditFilterWrapper umaPoliciesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.UMA);
        FilterChain umaPoliciesFilterChain = new FilterChain(umaPoliciesAuthzFilter, defaultAuthenticationFilter, InjectorHolder.getInstance(UmaEnabledFilter.class), umaPoliciesAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "users/{user}/uma/policies"), newHttpHandler(new FilterChain(umaPoliciesFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/uma/policies"), new FilterChain(umaPoliciesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("users/{user}/uma/policies"));

        org.forgerock.json.resource.Router umaAuditHistoryVersionRouter = new org.forgerock.json.resource.Router();
        umaAuditHistoryVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ResourceSetResource.class));
        FilterChain umaAuditHistoryAuthzFilter = createFilter(umaAuditHistoryVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(ResourceOwnerOrSuperUserAuthzModule.class), ResourceOwnerOrSuperUserAuthzModule.NAME));
        AuditFilterWrapper umaAuditHistoryAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.UMA);
        FilterChain umaAuditHistoryFilterChain = new FilterChain(umaAuditHistoryAuthzFilter, defaultAuthenticationFilter, InjectorHolder.getInstance(UmaEnabledFilter.class), umaAuditHistoryAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "users/{user}/uma/auditHistory"), newHttpHandler(new FilterChain(umaAuditHistoryFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/uma/auditHistory"), new FilterChain(umaAuditHistoryFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("users/{user}/uma/auditHistory"));

        org.forgerock.json.resource.Router umaPendingRequestsVersionRouter = new org.forgerock.json.resource.Router();
        umaPendingRequestsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(PendingRequestResource.class));
        FilterChain umaPendingRequestsAuthzFilter = createFilter(umaPendingRequestsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(ResourceOwnerOrSuperUserAuthzModule.class), ResourceOwnerOrSuperUserAuthzModule.NAME));
        AuditFilterWrapper umaPendingRequestsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.UMA);
        FilterChain umaPendingRequestsFilterChain = new FilterChain(umaPendingRequestsAuthzFilter, defaultAuthenticationFilter, InjectorHolder.getInstance(UmaEnabledFilter.class), umaPendingRequestsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "users/{user}/uma/pendingrequests"), newHttpHandler(new FilterChain(umaPendingRequestsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/uma/pendingrequests"), new FilterChain(umaPendingRequestsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("users/{user}/uma/pendingrequests"));

        org.forgerock.json.resource.Router umaLabelsVersionRouter = new org.forgerock.json.resource.Router();
        umaLabelsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(UmaLabelResource.class));
        FilterChain umaLabelsAuthzFilter = createFilter(umaLabelsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(ResourceOwnerOrSuperUserAuthzModule.class), ResourceOwnerOrSuperUserAuthzModule.NAME));
        AuditFilterWrapper umaLabelsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.OAUTH2);
        FilterChain umaLabelsFilterChain = new FilterChain(umaLabelsAuthzFilter, defaultAuthenticationFilter, umaLabelsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "users/{user}/resources/labels"), newHttpHandler(new FilterChain(umaLabelsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/resources/labels"), new FilterChain(umaLabelsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("users/{user}/resources/labels"));


        //protected
        org.forgerock.json.resource.Router policiesVersionRouter = new org.forgerock.json.resource.Router();
        FilterChain policiesVersionOneFilterChain = new FilterChain(Resources.newCollection(InjectorHolder.getInstance(PolicyResource.class)), InjectorHolder.getInstance(PolicyV1Filter.class));
        policiesVersionRouter.addRoute(requestResourceApiVersionMatcher(version(1)), policiesVersionOneFilterChain);
        policiesVersionRouter.addRoute(version(2), InjectorHolder.getInstance(PolicyResource.class));
        FilterChain policiesAuthzFilter = createFilter(policiesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper policiesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain policiesFilterChain = new FilterChain(policiesAuthzFilter, defaultAuthenticationFilter, policiesAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "policies"), newHttpHandler(new FilterChain(policiesFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "policies"), new FilterChain(policiesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("policies"));

        org.forgerock.json.resource.Router referralsVersionRouter = new org.forgerock.json.resource.Router();
        referralsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ReferralsResourceV1.class));
        FilterChain referralsAuthzFilter = createFilter(referralsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper referralsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain referralsFilterChain = new FilterChain(referralsAuthzFilter, defaultAuthenticationFilter, referralsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "referrals"), newHttpHandler(new FilterChain(referralsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "referrals"), new FilterChain(referralsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("referrals"));

        org.forgerock.json.resource.Router realmsVersionRouter = new org.forgerock.json.resource.Router();
        realmsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ReferralsResourceV1.class));
        FilterChain realmsAuthzFilter = createFilter(realmsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper realmsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.REALMS);
        FilterChain realmsFilterChain = new FilterChain(realmsAuthzFilter, defaultAuthenticationFilter, realmsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "realms"), newHttpHandler(new FilterChain(realmsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "realms"), new FilterChain(realmsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("realms"));

        org.forgerock.json.resource.Router sessionsVersionRouter = new org.forgerock.json.resource.Router();
        sessionsVersionRouter.addRoute(version(1, 1), InjectorHolder.getInstance(SessionResource.class));
        FilterChain sessionsAuthzFilter = createFilter(sessionsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(SessionResourceAuthzModule.class), SessionResourceAuthzModule.NAME));
        AuditFilterWrapper sessionsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.SESSION);
        Filter sessionsAuthnFilter = authenticationFilterProvider.get().exceptActions("validate");
        FilterChain sessionsFilterChain = new FilterChain(sessionsAuthzFilter, sessionsAuthnFilter, sessionsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "sessions"), newHttpHandler(new FilterChain(sessionsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "sessions"), new FilterChain(sessionsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("sessions"));

        org.forgerock.json.resource.Router applicationsVersionRouter = new org.forgerock.json.resource.Router();
        FilterChain applicationsVersionOneFilterChain = new FilterChain(Resources.newCollection(InjectorHolder.getInstance(ApplicationsResource.class)), InjectorHolder.getInstance(ApplicationV1Filter.class));
        applicationsVersionRouter.addRoute(requestResourceApiVersionMatcher(version(1)), applicationsVersionOneFilterChain);
        applicationsVersionRouter.addRoute(version(2), InjectorHolder.getInstance(ApplicationsResource.class));
        FilterChain applicationsAuthzFilter = createFilter(applicationsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper applicationsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain applicationsFilterChain = new FilterChain(applicationsAuthzFilter, defaultAuthenticationFilter, applicationsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "applications"), newHttpHandler(new FilterChain(applicationsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "applications"), new FilterChain(applicationsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("applications"));

        org.forgerock.json.resource.Router subjectAttributesVersionRouter = new org.forgerock.json.resource.Router();
        subjectAttributesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(SubjectAttributesResourceV1.class));
        FilterChain subjectAttributesAuthzFilter = createFilter(subjectAttributesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(SessionResourceAuthzModule.class), SessionResourceAuthzModule.NAME));
        AuditFilterWrapper subjectAttributesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain subjectAttributesFilterChain = new FilterChain(subjectAttributesAuthzFilter, defaultAuthenticationFilter, subjectAttributesAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "subjectattributes"), newHttpHandler(new FilterChain(subjectAttributesFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "subjectattributes"), new FilterChain(subjectAttributesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("subjectattributes"));

        org.forgerock.json.resource.Router applicationTypesVersionRouter = new org.forgerock.json.resource.Router();
        applicationTypesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ApplicationTypesResource.class));
        FilterChain applicationTypesAuthzFilter = createFilter(applicationTypesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper applicationTypesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain applicationTypesFilterChain = new FilterChain(applicationTypesAuthzFilter, defaultAuthenticationFilter, applicationTypesAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "applicationtypes"), newHttpHandler(new FilterChain(applicationTypesFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, "applicationtypes"), new FilterChain(applicationTypesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("applicationtypes"));

        org.forgerock.json.resource.Router resourceTypesVersionRouter = new org.forgerock.json.resource.Router();
        resourceTypesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ResourceTypesResource.class));
        FilterChain resourceTypesAuthzFilter = createFilter(resourceTypesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(SessionResourceAuthzModule.class), SessionResourceAuthzModule.NAME));
        AuditFilterWrapper resourceTypesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain resourceTypesFilterChain = new FilterChain(resourceTypesAuthzFilter, defaultAuthenticationFilter, resourceTypesAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "resourcetypes"), newHttpHandler(new FilterChain(resourceTypesFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "resourcetypes"), new FilterChain(resourceTypesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("resourcetypes"));

        org.forgerock.json.resource.Router scriptsVersionRouter = new org.forgerock.json.resource.Router();
        scriptsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ScriptResource.class));
        FilterChain scriptsAuthzFilter = createFilter(scriptsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper scriptsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.SCRIPT);
        FilterChain scriptsFilterChain = new FilterChain(scriptsAuthzFilter, defaultAuthenticationFilter, scriptsAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "scripts"), newHttpHandler(new FilterChain(scriptsFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "scripts"), new FilterChain(scriptsFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("scripts"));

        org.forgerock.json.resource.Router realmConfigVersionRouter = new org.forgerock.json.resource.Router();
        realmConfigVersionRouter.addRoute(version(1), smsRequestHandlerFactory.create(SchemaType.ORGANIZATION));
        FilterChain realmConfigAuthzFilter = createFilter(realmConfigVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper realmConfigAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.CONFIG);
        FilterChain realmConfigFilterChain = new FilterChain(realmConfigAuthzFilter, defaultAuthenticationFilter, realmConfigAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "realm-config"), newHttpHandler(new FilterChain(realmConfigFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "realm-config"), new FilterChain(realmConfigFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("realm-config"));

        org.forgerock.json.resource.Router batchVersionRouter = new org.forgerock.json.resource.Router();
        batchVersionRouter.addRoute(version(1), InjectorHolder.getInstance(BatchResource.class));
        FilterChain batchAuthzFilter = createFilter(batchVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper batchAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.BATCH);
        FilterChain batchFilterChain = new FilterChain(batchAuthzFilter, defaultAuthenticationFilter, batchAuditFilter);
        realmRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "batch"), newHttpHandler(new FilterChain(batchFilterChain, contextFilter, crestLoggingFilter)));
        crestRealmRouter.addRoute(requestUriMatcher(STARTS_WITH, "batch"), new FilterChain(batchFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("batch"));


        // ------------------
        // Global routes
        // ------------------
        org.forgerock.json.resource.Router decisionCombinersVersionRouter = new org.forgerock.json.resource.Router();
        decisionCombinersVersionRouter.addRoute(version(1), InjectorHolder.getInstance(DecisionCombinersResource.class));
        FilterChain decisionCombinersAuthzFilter = createFilter(decisionCombinersVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper decisionCombinersAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain decisionCombinersFilterChain = new FilterChain(decisionCombinersAuthzFilter, defaultAuthenticationFilter, decisionCombinersAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "decisioncombiners"), newHttpHandler(new FilterChain(decisionCombinersFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, "decisioncombiners"), new FilterChain(decisionCombinersFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("decisioncombiners"));

        org.forgerock.json.resource.Router conditionTypesVersionRouter = new org.forgerock.json.resource.Router();
        conditionTypesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ConditionTypesResource.class));
        FilterChain conditionTypesAuthzFilter = createFilter(conditionTypesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper conditionTypesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain conditionTypesFilterChain = new FilterChain(conditionTypesAuthzFilter, defaultAuthenticationFilter, conditionTypesAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "conditiontypes"), newHttpHandler(new FilterChain(conditionTypesFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, "conditiontypes"), new FilterChain(conditionTypesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("conditiontypes"));

        org.forgerock.json.resource.Router subjectTypesVersionRouter = new org.forgerock.json.resource.Router();
        subjectTypesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(SubjectTypesResource.class));
        FilterChain subjectTypesAuthzFilter = createFilter(subjectTypesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper subjectTypesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain subjectTypesFilterChain = new FilterChain(subjectTypesAuthzFilter, defaultAuthenticationFilter, subjectTypesAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "subjecttypes"), newHttpHandler(new FilterChain(subjectTypesFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, "subjecttypes"), new FilterChain(subjectTypesFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("subjecttypes"));

        org.forgerock.json.resource.Router tokensVersionRouter = new org.forgerock.json.resource.Router();
        tokensVersionRouter.addRoute(version(1), InjectorHolder.getInstance(CoreTokenResource.class));
        FilterChain tokensAuthzFilter = createFilter(tokensVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(CoreTokenResourceAuthzModule.class), CoreTokenResourceAuthzModule.NAME));
        AuditFilterWrapper tokensAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.CTS);
        FilterChain tokensFilterChain = new FilterChain(tokensAuthzFilter, defaultAuthenticationFilter, tokensAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "tokens"), newHttpHandler(new FilterChain(tokensFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, "tokens"), new FilterChain(tokensFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("tokens"));

        org.forgerock.json.resource.Router globalConfigVersionRouter = new org.forgerock.json.resource.Router();
        globalConfigVersionRouter.addRoute(requestResourceApiVersionMatcher(version(1)), smsRequestHandlerFactory.create(SchemaType.GLOBAL));
        FilterChain globalConfigAuthzFilter = createFilter(globalConfigVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper globalConfigAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.CONFIG);
        FilterChain globalConfigFilterChain = new FilterChain(globalConfigAuthzFilter, defaultAuthenticationFilter, globalConfigAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "global-config"), newHttpHandler(new FilterChain(globalConfigFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, "global-config"), new FilterChain(globalConfigFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("global-config"));

        org.forgerock.json.resource.Router globalConfigServersVersionRouter = new org.forgerock.json.resource.Router();
        globalConfigServersVersionRouter.addRoute(version(1), InjectorHolder.getInstance(SmsServerPropertiesResource.class));
        FilterChain globalConfigServersAuthzFilter = createFilter(globalConfigServersVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper globalConfigServersAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.CONFIG);
        FilterChain globalConfigServersFilterChain = new FilterChain(globalConfigServersAuthzFilter, defaultAuthenticationFilter, globalConfigServersAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "global-config/servers/{serverName}/properties/{tab}"), newHttpHandler(new FilterChain(globalConfigServersFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, "global-config/servers/{serverName}/properties/{tab}"), new FilterChain(globalConfigServersFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("global-config/servers/{serverName}/properties/{tab}"));

        org.forgerock.json.resource.Router auditVersionRouter = new org.forgerock.json.resource.Router();
        auditVersionRouter.addRoute(version(1), InjectorHolder.getInstance(AuditService.class));
        FilterChain auditAuthzFilter = createFilter(auditVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AgentOnlyAuthzModule.class), AgentOnlyAuthzModule.NAME));
        AuditFilterWrapper auditAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditEndpointAuditFilter.class),
                AuditConstants.Component.AUDIT);
        FilterChain auditFilterChain = new FilterChain(auditAuthzFilter, defaultAuthenticationFilter,auditAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, "audit"), newHttpHandler(new FilterChain(auditFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, "audit"), new FilterChain(auditFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment("audit"));

        org.forgerock.json.resource.Router recordVersionRouter = new org.forgerock.json.resource.Router();
        recordVersionRouter.addRoute(version(1), InjectorHolder.getInstance(RecordResource.class));
        FilterChain recordAuthzFilter = createFilter(recordVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper recordAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.RECORD);
        FilterChain recordFilterChain = new FilterChain(recordAuthzFilter, defaultAuthenticationFilter, recordAuditFilter);
        rootRouter.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, RecordConstants.RECORD_REST_ENDPOINT), newHttpHandler(new FilterChain(recordFilterChain, contextFilter, crestLoggingFilter)));
        crestRootRouter.addRoute(requestUriMatcher(STARTS_WITH, RecordConstants.RECORD_REST_ENDPOINT), new FilterChain(recordFilterChain, contextFilter, crestLoggingFilter));
        invalidRealmNames.add(firstPathSegment(RecordConstants.RECORD_REST_ENDPOINT));
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
