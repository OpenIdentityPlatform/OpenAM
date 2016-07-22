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

package org.forgerock.openam.core.rest;

import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.openam.rest.Routers.*;

import com.google.inject.Key;
import com.google.inject.name.Names;

import org.forgerock.http.routing.RoutingMode;
import org.forgerock.openam.core.rest.authn.http.AuthenticationServiceV1;
import org.forgerock.openam.core.rest.authn.http.AuthenticationServiceV2;
import org.forgerock.openam.core.rest.cts.CoreTokenResource;
import org.forgerock.openam.core.rest.cts.CoreTokenResourceAuthzModule;
import org.forgerock.openam.core.rest.dashboard.DashboardResource;
import org.forgerock.openam.core.rest.devices.oath.OathDevicesResource;
import org.forgerock.openam.core.rest.devices.push.PushDevicesResource;
import org.forgerock.openam.core.rest.devices.deviceprint.TrustedDevicesResource;
import org.forgerock.openam.core.rest.docs.api.ApiDocsService;
import org.forgerock.openam.core.rest.record.RecordConstants;
import org.forgerock.openam.core.rest.record.RecordResource;
import org.forgerock.openam.core.rest.server.ServerInfoResource;
import org.forgerock.openam.core.rest.server.ServerVersionResource;
import org.forgerock.openam.core.rest.session.AnyOfAuthzModule;
import org.forgerock.openam.core.rest.session.SessionResource;
import org.forgerock.openam.http.authz.HttpContextFilter;
import org.forgerock.openam.http.authz.HttpPrivilegeAuthzModule;
import org.forgerock.openam.core.rest.session.SessionResourceV2;
import org.forgerock.openam.rest.AbstractRestRouteProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.ServiceRouter;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.rest.authz.CrestPrivilegeAuthzModule;
import org.forgerock.openam.rest.authz.ResourceOwnerOrSuperUserAuthzModule;
import org.forgerock.openam.services.MailService;

/**
 * A {@link RestRouteProvider} that add routes for all the core endpoints.
 *
 * @since 13.0.0
 */
public class CoreRestRouteProvider extends AbstractRestRouteProvider {

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {
        realmRouter.route("dashboard")
                .auditAs(DASHBOARD)
                .toAnnotatedCollection(DashboardResource.class);

        realmRouter.route("serverinfo")
                .authenticateWith(ssoToken().exceptRead())
                .auditAs(SERVER_INFO)
                .forVersion(1, 1)
                .toCollection(ServerInfoResource.class);

        realmRouter.route("serverinfo/version")
                .authenticateWith(ssoToken().exceptRead())
                .auditAs(SERVER_INFO)
                .authorizeWith(CrestPrivilegeAuthzModule.class)
                .toAnnotatedSingleton(ServerVersionResource.class);

        realmRouter.route("users")
                .authenticateWith(ssoToken().exceptActions("register", "confirm", "forgotPassword",
                        "forgotPasswordReset", "anonymousCreate"))
                .auditAs(USERS)
                .forVersion(1, 2)
                .toCollection(Key.get(IdentityResourceV1.class, Names.named("UsersResource")))
                .forVersion(2, 1)
                .toCollection(Key.get(IdentityResourceV2.class, Names.named("UsersResource")))
                .forVersion(3, 0)
                .toCollection(Key.get(IdentityResourceV3.class, Names.named("UsersResource")));

        realmRouter.route("groups")
                .auditAs(GROUPS)
                .forVersion(1, 2)
                .toCollection(Key.get(IdentityResourceV1.class, Names.named("GroupsResource")))
                .forVersion(2, 1)
                .toCollection(Key.get(IdentityResourceV2.class, Names.named("GroupsResource")))
                .forVersion(3, 0)
                .toCollection(Key.get(IdentityResourceV3.class, Names.named("GroupsResource")));

        realmRouter.route("agents")
                .auditAs(POLICY_AGENT)
                .forVersion(1, 2)
                .toCollection(Key.get(IdentityResourceV1.class, Names.named("AgentsResource")))
                .forVersion(2, 1)
                .toCollection(Key.get(IdentityResourceV2.class, Names.named("AgentsResource")))
                .forVersion(3, 0)
                .toCollection(Key.get(IdentityResourceV3.class, Names.named("AgentsResource")));

        realmRouter.route("users/{user}/devices/trusted")
                .auditAs(DEVICES)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .toCollection(TrustedDevicesResource.class);

        realmRouter.route("users/{user}/devices/2fa/oath")
                .auditAs(DEVICES)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .toCollection(OathDevicesResource.class);

        realmRouter.route("users/{user}/devices/push")
                .auditAs(DEVICES)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .toCollection(PushDevicesResource.class);

        realmRouter.route("sessions")
                .authenticateWith(ssoToken().exceptActions("validate"))
                .auditAs(SESSION)
                .authorizeWith(AnyOfAuthzModule.class)
                .forVersion(1, 2)
                .toCollection(SessionResource.class)
                .forVersion(2, 0)
                .toCollection(SessionResourceV2.class);


        rootRouter.route("tokens")
                .auditAs(CTS)
                .authorizeWith(CoreTokenResourceAuthzModule.class)
                .toAnnotatedCollection(CoreTokenResource.class);

        rootRouter.route(RecordConstants.RECORD_REST_ENDPOINT)
                .auditAs(RECORD)
                .authorizeWith(AdminOnlyAuthzModule.class)
                .toCollection(RecordResource.class);
    }

    @Override
    public void addInternalRoutes(ResourceRouter internalRouter) {
        internalRouter
                .route("email")
                .toRequestHandler(RoutingMode.STARTS_WITH, MailService.class);
    }

    @Override
    public void addServiceRoutes(ServiceRouter rootRouter, ServiceRouter realmRouter) {
        realmRouter.route("authenticate")
                .auditAs(AUTHENTICATION)
                .forVersion(1, 2)
                .toService(EQUALS, AuthenticationServiceV1.class)
                .forVersion(2, 1)
                .toService(EQUALS, AuthenticationServiceV2.class);

        realmRouter.route("docs/api")
                .auditAs(DOCUMENTATION)
                .through(HttpContextFilter.class)
                .authorizeWith(HttpPrivilegeAuthzModule.class)
                .forVersion(1, 0)
                .toService(EQUALS, ApiDocsService.class);
    }
}
