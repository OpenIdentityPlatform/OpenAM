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
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.RouteMatchers.*;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;
import static org.forgerock.openam.http.HttpRoute.newHttpRoute;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.sm.SchemaType;
import org.forgerock.audit.AuditService;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.forgerockrest.IdentityResourceV1;
import org.forgerock.openam.forgerockrest.IdentityResourceV2;
import org.forgerock.openam.forgerockrest.UmaLabelResource;
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
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
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
import org.forgerock.openam.rest.fluent.CrestLoggingFilter;
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

    @Inject
    public void setSmsRequestHandlerFactory(SmsRequestHandlerFactory smsRequestHandlerFactory) {
        this.smsRequestHandlerFactory = smsRequestHandlerFactory;
    }

    @Inject
    public void setInvalidRealms(Set<String> invalidRealms) {
        this.invalidRealms = invalidRealms;
    }

    @Override
    public Set<HttpRoute> get() {
        return Collections.singleton(
                newHttpRoute(STARTS_WITH, "json", newHttpHandler(createResourceRouter(invalidRealms))));
    }

    private RequestHandler createResourceRouter(final Set<String> invalidRealmNames) {
        org.forgerock.json.resource.Router realmRouter = new org.forgerock.json.resource.Router();
        RealmContextFilter realmContextFilter = InjectorHolder.getInstance(RealmContextFilter.class);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "{realm}"),
                new FilterChain(realmRouter, realmContextFilter));

        org.forgerock.json.resource.Router rootRouter = new org.forgerock.json.resource.Router();
        rootRouter.setDefaultRoute(realmRouter);

        // ------------------
        // Realm based routes
        // ------------------
        //not protected
        org.forgerock.json.resource.Router dashboardVersionRouter = new org.forgerock.json.resource.Router();
        dashboardVersionRouter.addRoute(version(1), InjectorHolder.getInstance(DashboardResource.class));
        AuditFilterWrapper dashboardAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.DASHBOARD);
        FilterChain dashboardFilterChain = new FilterChain(dashboardVersionRouter, dashboardAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "dashboard"), dashboardFilterChain);
        invalidRealmNames.add(firstPathSegment("dashboard"));

        org.forgerock.json.resource.Router serverInfoVersionRouter = new org.forgerock.json.resource.Router();
        serverInfoVersionRouter.addRoute(version(1, 1), InjectorHolder.getInstance(ServerInfoResource.class));
        AuditFilterWrapper serverInfoAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.SERVER_INFO);
        FilterChain serverInfoFilterChain = new FilterChain(serverInfoVersionRouter, serverInfoAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "serverinfo"), serverInfoFilterChain);
        invalidRealmNames.add(firstPathSegment("serverinfo"));

        org.forgerock.json.resource.Router umaServerInfoVersionRouter = new org.forgerock.json.resource.Router();
        umaServerInfoVersionRouter.addRoute(version(1), InjectorHolder.getInstance(UmaConfigurationResource.class));
        AuditFilterWrapper umaServerInfoAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.UMA);
        FilterChain umaServerInfoFilterChain = new FilterChain(umaServerInfoVersionRouter, umaServerInfoAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "serverinfo/uma"), umaServerInfoFilterChain);
        invalidRealmNames.add(firstPathSegment("serverinfo/uma"));

        org.forgerock.json.resource.Router usersVersionRouter = new org.forgerock.json.resource.Router();
        usersVersionRouter.addRoute(version(1, 2), InjectorHolder.getInstance(Key.get(IdentityResourceV1.class, Names.named("UsersResource"))));
        usersVersionRouter.addRoute(version(2, 1), InjectorHolder.getInstance(Key.get(IdentityResourceV2.class, Names.named("UsersResource"))));
        AuditFilterWrapper usersAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.USERS);
        FilterChain usersFilterChain = new FilterChain(usersVersionRouter, usersAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users"), usersFilterChain);
        invalidRealmNames.add(firstPathSegment("users"));

        org.forgerock.json.resource.Router groupsVersionRouter = new org.forgerock.json.resource.Router();
        groupsVersionRouter.addRoute(version(1, 2), InjectorHolder.getInstance(Key.get(IdentityResourceV1.class, Names.named("GroupsResource"))));
        groupsVersionRouter.addRoute(version(2, 1), InjectorHolder.getInstance(Key.get(IdentityResourceV2.class, Names.named("GroupsResource"))));
        AuditFilterWrapper groupsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.GROUPS);
        FilterChain groupsFilterChain = new FilterChain(groupsVersionRouter, groupsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "groups"), groupsFilterChain);
        invalidRealmNames.add(firstPathSegment("groups"));

        org.forgerock.json.resource.Router agentsVersionRouter = new org.forgerock.json.resource.Router();
        agentsVersionRouter.addRoute(version(1, 2), InjectorHolder.getInstance(Key.get(IdentityResourceV1.class, Names.named("AgentsResource"))));
        agentsVersionRouter.addRoute(version(2, 1), InjectorHolder.getInstance(Key.get(IdentityResourceV2.class, Names.named("AgentsResource"))));
        AuditFilterWrapper agentsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY_AGENT);
        FilterChain agentsFilterChain = new FilterChain(agentsVersionRouter, agentsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "agents"), agentsFilterChain);
        invalidRealmNames.add(firstPathSegment("agents"));

        org.forgerock.json.resource.Router trustedDevicesVersionRouter = new org.forgerock.json.resource.Router();
        trustedDevicesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(TrustedDevicesResource.class));
        AuditFilterWrapper trustedDevicesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.DEVICES);
        FilterChain trustedDevicesFilterChain = new FilterChain(trustedDevicesVersionRouter, trustedDevicesAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/devices/trusted"), trustedDevicesFilterChain);
        invalidRealmNames.add(firstPathSegment("users/{user}/devices/trusted"));

        org.forgerock.json.resource.Router oathDevicesVersionRouter = new org.forgerock.json.resource.Router();
        oathDevicesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(OathDevicesResource.class));
        AuditFilterWrapper oathDevicesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.DEVICES);
        FilterChain oathDevicesFilterChain = new FilterChain(oathDevicesVersionRouter, oathDevicesAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/devices/2fa/oath"), oathDevicesFilterChain);
        invalidRealmNames.add(firstPathSegment("users/{user}/devices/2fa/oath"));

        org.forgerock.json.resource.Router oauth2ResourceSetsVersionRouter = new org.forgerock.json.resource.Router();
        oauth2ResourceSetsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ResourceSetResource.class));
        FilterChain oauth2ResourceSetsAuthzFilter = createFilter(oauth2ResourceSetsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(UmaPolicyResourceAuthzFilter.class), UmaPolicyResourceAuthzFilter.NAME));
        AuditFilterWrapper oauth2ResourceSetsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.OAUTH2);
        FilterChain oauth2ResourceSetsFilterChain = new FilterChain(oauth2ResourceSetsAuthzFilter, InjectorHolder.getInstance(UmaEnabledFilter.class), oauth2ResourceSetsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/oauth2/resources/sets"), oauth2ResourceSetsFilterChain);
        invalidRealmNames.add(firstPathSegment("users/{user}/oauth2/resources/sets"));

        org.forgerock.json.resource.Router umaPoliciesVersionRouter = new org.forgerock.json.resource.Router();
        umaPoliciesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(UmaPolicyResource.class));
        FilterChain umaPoliciesAuthzFilter = createFilter(umaPoliciesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(UmaPolicyResourceAuthzFilter.class), UmaPolicyResourceAuthzFilter.NAME));
        AuditFilterWrapper umaPoliciesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.UMA);
        FilterChain umaPoliciesFilterChain = new FilterChain(umaPoliciesAuthzFilter, InjectorHolder.getInstance(UmaEnabledFilter.class), umaPoliciesAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/uma/policies"), umaPoliciesFilterChain);
        invalidRealmNames.add(firstPathSegment("users/{user}/uma/policies"));

        org.forgerock.json.resource.Router umaAuditHistoryVersionRouter = new org.forgerock.json.resource.Router();
        umaAuditHistoryVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ResourceSetResource.class));
        FilterChain umaAuditHistoryAuthzFilter = createFilter(umaAuditHistoryVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(ResourceOwnerOrSuperUserAuthzModule.class), ResourceOwnerOrSuperUserAuthzModule.NAME));
        AuditFilterWrapper umaAuditHistoryAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.UMA);
        FilterChain umaAuditHistoryFilterChain = new FilterChain(umaAuditHistoryAuthzFilter, InjectorHolder.getInstance(UmaEnabledFilter.class), umaAuditHistoryAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/uma/auditHistory"), umaAuditHistoryFilterChain);
        invalidRealmNames.add(firstPathSegment("users/{user}/uma/auditHistory"));

        org.forgerock.json.resource.Router umaPendingRequestsVersionRouter = new org.forgerock.json.resource.Router();
        umaPendingRequestsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(PendingRequestResource.class));
        FilterChain umaPendingRequestsAuthzFilter = createFilter(umaPendingRequestsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(ResourceOwnerOrSuperUserAuthzModule.class), ResourceOwnerOrSuperUserAuthzModule.NAME));
        AuditFilterWrapper umaPendingRequestsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.UMA);
        FilterChain umaPendingRequestsFilterChain = new FilterChain(umaPendingRequestsAuthzFilter, InjectorHolder.getInstance(UmaEnabledFilter.class), umaPendingRequestsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/uma/pendingrequests"), umaPendingRequestsFilterChain);
        invalidRealmNames.add(firstPathSegment("users/{user}/uma/pendingrequests"));

        org.forgerock.json.resource.Router umaLabelsVersionRouter = new org.forgerock.json.resource.Router();
        umaLabelsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(UmaLabelResource.class));
        FilterChain umaLabelsAuthzFilter = createFilter(umaLabelsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(ResourceOwnerOrSuperUserAuthzModule.class), ResourceOwnerOrSuperUserAuthzModule.NAME));
        AuditFilterWrapper umaLabelsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.OAUTH2);
        FilterChain umaLabelsFilterChain = new FilterChain(umaLabelsAuthzFilter, umaLabelsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "users/{user}/resources/labels"), umaLabelsFilterChain);
        invalidRealmNames.add(firstPathSegment("users/{user}/resources/labels"));


        //protected
        org.forgerock.json.resource.Router policiesVersionRouter = new org.forgerock.json.resource.Router();
        FilterChain policiesVersionOneFilterChain = new FilterChain(Resources.newCollection(InjectorHolder.getInstance(PolicyResource.class)), InjectorHolder.getInstance(PolicyV1Filter.class));
        policiesVersionRouter.addRoute(requestResourceApiVersionMatcher(version(1)), policiesVersionOneFilterChain);
        policiesVersionRouter.addRoute(version(2), InjectorHolder.getInstance(PolicyResource.class));
        FilterChain policiesAuthzFilter = createFilter(policiesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper policiesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain policiesFilterChain = new FilterChain(policiesAuthzFilter, policiesAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "policies"), policiesFilterChain);
        invalidRealmNames.add(firstPathSegment("policies"));

        org.forgerock.json.resource.Router referralsVersionRouter = new org.forgerock.json.resource.Router();
        referralsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ReferralsResourceV1.class));
        FilterChain referralsAuthzFilter = createFilter(referralsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper referralsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain referralsFilterChain = new FilterChain(referralsAuthzFilter, referralsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "referrals"), referralsFilterChain);
        invalidRealmNames.add(firstPathSegment("referrals"));

        org.forgerock.json.resource.Router realmsVersionRouter = new org.forgerock.json.resource.Router();
        realmsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ReferralsResourceV1.class));
        FilterChain realmsAuthzFilter = createFilter(realmsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper realmsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.REALMS);
        FilterChain realmsFilterChain = new FilterChain(realmsAuthzFilter, realmsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "realms"), realmsFilterChain);
        invalidRealmNames.add(firstPathSegment("realms"));

        org.forgerock.json.resource.Router sessionsVersionRouter = new org.forgerock.json.resource.Router();
        sessionsVersionRouter.addRoute(version(1, 1), InjectorHolder.getInstance(ReferralsResourceV1.class));
        FilterChain sessionsAuthzFilter = createFilter(sessionsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(SessionResourceAuthzModule.class), SessionResourceAuthzModule.NAME));
        AuditFilterWrapper sessionsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.SESSION);
        FilterChain sessionsFilterChain = new FilterChain(sessionsAuthzFilter, sessionsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "sessions"), sessionsFilterChain);
        invalidRealmNames.add(firstPathSegment("sessions"));

        org.forgerock.json.resource.Router applicationsVersionRouter = new org.forgerock.json.resource.Router();
        FilterChain applicationsVersionOneFilterChain = new FilterChain(Resources.newCollection(InjectorHolder.getInstance(ApplicationsResource.class)), InjectorHolder.getInstance(ApplicationV1Filter.class));
        applicationsVersionRouter.addRoute(requestResourceApiVersionMatcher(version(1)), applicationsVersionOneFilterChain);
        applicationsVersionRouter.addRoute(version(2), InjectorHolder.getInstance(ApplicationsResource.class));
        FilterChain applicationsAuthzFilter = createFilter(applicationsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper applicationsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain applicationsFilterChain = new FilterChain(applicationsAuthzFilter, applicationsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "applications"), applicationsFilterChain);
        invalidRealmNames.add(firstPathSegment("applications"));

        org.forgerock.json.resource.Router subjectAttributesVersionRouter = new org.forgerock.json.resource.Router();
        subjectAttributesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(SubjectAttributesResourceV1.class));
        FilterChain subjectAttributesAuthzFilter = createFilter(subjectAttributesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(SessionResourceAuthzModule.class), SessionResourceAuthzModule.NAME));
        AuditFilterWrapper subjectAttributesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain subjectAttributesFilterChain = new FilterChain(subjectAttributesAuthzFilter, subjectAttributesAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "subjectattributes"), subjectAttributesFilterChain);
        invalidRealmNames.add(firstPathSegment("subjectattributes"));

        org.forgerock.json.resource.Router applicationTypesVersionRouter = new org.forgerock.json.resource.Router();
        applicationTypesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ApplicationTypesResource.class));
        FilterChain applicationTypesAuthzFilter = createFilter(applicationTypesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper applicationTypesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain applicationTypesFilterChain = new FilterChain(applicationTypesAuthzFilter, applicationTypesAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "applicationtypes"), applicationTypesFilterChain);
        invalidRealmNames.add(firstPathSegment("applicationtypes"));

        org.forgerock.json.resource.Router resourceTypesVersionRouter = new org.forgerock.json.resource.Router();
        resourceTypesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ResourceTypesResource.class));
        FilterChain resourceTypesAuthzFilter = createFilter(resourceTypesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(SessionResourceAuthzModule.class), SessionResourceAuthzModule.NAME));
        AuditFilterWrapper resourceTypesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain resourceTypesFilterChain = new FilterChain(resourceTypesAuthzFilter, resourceTypesAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "resourcetypes"), resourceTypesFilterChain);
        invalidRealmNames.add(firstPathSegment("resourcetypes"));

        org.forgerock.json.resource.Router scriptsVersionRouter = new org.forgerock.json.resource.Router();
        scriptsVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ScriptResource.class));
        FilterChain scriptsAuthzFilter = createFilter(scriptsVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper scriptsAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.SCRIPT);
        FilterChain scriptsFilterChain = new FilterChain(scriptsAuthzFilter, scriptsAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "scripts"), scriptsFilterChain);
        invalidRealmNames.add(firstPathSegment("scripts"));

        org.forgerock.json.resource.Router realmConfigVersionRouter = new org.forgerock.json.resource.Router();
        realmConfigVersionRouter.addRoute(version(1), smsRequestHandlerFactory.create(SchemaType.ORGANIZATION));
        FilterChain realmConfigAuthzFilter = createFilter(realmConfigVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper realmConfigAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.CONFIG);
        FilterChain realmConfigFilterChain = new FilterChain(realmConfigAuthzFilter, realmConfigAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "realm-config"), realmConfigFilterChain);
        invalidRealmNames.add(firstPathSegment("realm-config"));

        org.forgerock.json.resource.Router batchVersionRouter = new org.forgerock.json.resource.Router();
        batchVersionRouter.addRoute(version(1), InjectorHolder.getInstance(BatchResource.class));
        FilterChain batchAuthzFilter = createFilter(batchVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper batchAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.BATCH);
        FilterChain batchFilterChain = new FilterChain(batchAuthzFilter, batchAuditFilter);
        realmRouter.addRoute(requestUriMatcher(STARTS_WITH, "batch"), batchFilterChain);
        invalidRealmNames.add(firstPathSegment("batch"));


        // ------------------
        // Global routes
        // ------------------
        org.forgerock.json.resource.Router decisionCombinersVersionRouter = new org.forgerock.json.resource.Router();
        decisionCombinersVersionRouter.addRoute(version(1), InjectorHolder.getInstance(DecisionCombinersResource.class));
        FilterChain decisionCombinersAuthzFilter = createFilter(decisionCombinersVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper decisionCombinersAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain decisionCombinersFilterChain = new FilterChain(decisionCombinersAuthzFilter, decisionCombinersAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "decisioncombiners"), decisionCombinersFilterChain);
        invalidRealmNames.add(firstPathSegment("decisioncombiners"));

        org.forgerock.json.resource.Router conditionTypesVersionRouter = new org.forgerock.json.resource.Router();
        conditionTypesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(ConditionTypesResource.class));
        FilterChain conditionTypesAuthzFilter = createFilter(conditionTypesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper conditionTypesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain conditionTypesFilterChain = new FilterChain(conditionTypesAuthzFilter, conditionTypesAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "conditiontypes"), conditionTypesFilterChain);
        invalidRealmNames.add(firstPathSegment("conditiontypes"));

        org.forgerock.json.resource.Router subjectTypesVersionRouter = new org.forgerock.json.resource.Router();
        subjectTypesVersionRouter.addRoute(version(1), InjectorHolder.getInstance(SubjectTypesResource.class));
        FilterChain subjectTypesAuthzFilter = createFilter(subjectTypesVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(PrivilegeAuthzModule.class), PrivilegeAuthzModule.NAME));
        AuditFilterWrapper subjectTypesAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.POLICY);
        FilterChain subjectTypesFilterChain = new FilterChain(subjectTypesAuthzFilter, subjectTypesAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "subjecttypes"), subjectTypesFilterChain);
        invalidRealmNames.add(firstPathSegment("subjecttypes"));

        org.forgerock.json.resource.Router tokensVersionRouter = new org.forgerock.json.resource.Router();
        tokensVersionRouter.addRoute(version(1), InjectorHolder.getInstance(CoreTokenResource.class));
        FilterChain tokensAuthzFilter = createFilter(tokensVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(CoreTokenResourceAuthzModule.class), CoreTokenResourceAuthzModule.NAME));
        AuditFilterWrapper tokensAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.CTS);
        FilterChain tokensFilterChain = new FilterChain(tokensAuthzFilter, tokensAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "tokens"), tokensFilterChain);
        invalidRealmNames.add(firstPathSegment("tokens"));

        org.forgerock.json.resource.Router globalConfigVersionRouter = new org.forgerock.json.resource.Router();
        globalConfigVersionRouter.addRoute(requestResourceApiVersionMatcher(version(1)), smsRequestHandlerFactory.create(SchemaType.GLOBAL));
        FilterChain globalConfigAuthzFilter = createFilter(globalConfigVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper globalConfigAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.CONFIG);
        FilterChain globalConfigFilterChain = new FilterChain(globalConfigAuthzFilter, globalConfigAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "global-config"), globalConfigFilterChain);
        invalidRealmNames.add(firstPathSegment("global-config"));

        org.forgerock.json.resource.Router globalConfigServersVersionRouter = new org.forgerock.json.resource.Router();
        globalConfigServersVersionRouter.addRoute(version(1), InjectorHolder.getInstance(SmsServerPropertiesResource.class));
        FilterChain globalConfigServersAuthzFilter = createFilter(globalConfigServersVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper globalConfigServersAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.CONFIG);
        FilterChain globalConfigServersFilterChain = new FilterChain(globalConfigServersAuthzFilter, globalConfigServersAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "global-config/servers/{serverName}/properties/{tab}"), globalConfigServersFilterChain);
        invalidRealmNames.add(firstPathSegment("global-config/servers/{serverName}/properties/{tab}"));

        org.forgerock.json.resource.Router auditVersionRouter = new org.forgerock.json.resource.Router();
        auditVersionRouter.addRoute(version(1), InjectorHolder.getInstance(AuditService.class));
        FilterChain auditAuthzFilter = createFilter(auditVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AgentOnlyAuthzModule.class), AgentOnlyAuthzModule.NAME));
        AuditFilterWrapper auditAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditEndpointAuditFilter.class),
                AuditConstants.Component.AUDIT);
        FilterChain auditFilterChain = new FilterChain(auditAuthzFilter, auditAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "audit"), auditFilterChain);
        invalidRealmNames.add(firstPathSegment("audit"));


        org.forgerock.json.resource.Router recordVersionRouter = new org.forgerock.json.resource.Router();
        recordVersionRouter.addRoute(version(1), InjectorHolder.getInstance(RecordResource.class));
        FilterChain recordAuthzFilter = createFilter(recordVersionRouter, new LoggingAuthzModule(InjectorHolder.getInstance(AdminOnlyAuthzModule.class), AdminOnlyAuthzModule.NAME));
        AuditFilterWrapper recordAuditFilter = new AuditFilterWrapper(InjectorHolder.getInstance(AuditFilter.class),
                AuditConstants.Component.RECORD);
        FilterChain recordFilterChain = new FilterChain(recordAuthzFilter, recordAuditFilter);
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, RecordConstants.RECORD_REST_ENDPOINT), recordFilterChain);
        invalidRealmNames.add(firstPathSegment(RecordConstants.RECORD_REST_ENDPOINT));

        VersionBehaviourConfigListener behaviourManager = new VersionBehaviourConfigListener();
        Filter apiVersionFilter = resourceApiVersionContextFilter(behaviourManager);
        Filter contextFilter = new ContextFilter();
        Filter loggingFilter = InjectorHolder.getInstance(CrestLoggingFilter.class);
        return new FilterChain(rootRouter, apiVersionFilter, contextFilter, loggingFilter);
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
