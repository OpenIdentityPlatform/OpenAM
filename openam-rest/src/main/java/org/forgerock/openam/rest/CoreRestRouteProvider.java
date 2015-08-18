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

import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.openam.audit.AuditConstants.Component.DEVICES;
import static org.forgerock.openam.audit.AuditConstants.Component.POLICY_AGENT;
import static org.forgerock.openam.rest.Routers.ssoToken;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.openam.forgerockrest.IdentityResourceV1;
import org.forgerock.openam.forgerockrest.IdentityResourceV2;
import org.forgerock.openam.forgerockrest.cts.CoreTokenResource;
import org.forgerock.openam.forgerockrest.server.ServerInfoResource;
import org.forgerock.openam.forgerockrest.session.SessionResource;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.rest.authz.CoreTokenResourceAuthzModule;
import org.forgerock.openam.rest.authz.ResourceOwnerOrSuperUserAuthzModule;
import org.forgerock.openam.rest.authz.SessionResourceAuthzModule;
import org.forgerock.openam.rest.batch.BatchResource;
import org.forgerock.openam.rest.dashboard.DashboardResource;
import org.forgerock.openam.rest.devices.OathDevicesResource;
import org.forgerock.openam.rest.devices.TrustedDevicesResource;
import org.forgerock.openam.rest.record.RecordConstants;
import org.forgerock.openam.rest.record.RecordResource;

/**
 * A {@link RestRouteProvider} that add routes for all the core endpoints.
 *
 * @since 13.0.0
 */
public class CoreRestRouteProvider implements RestRouteProvider {

    @Override
    public void addRoutes(RestRouter rootRouter, RestRouter realmRouter) {

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

        realmRouter.route("batch")
                .auditAs(BATCH)
                .authorizeWith(AdminOnlyAuthzModule.class)
                .toCollection(BatchResource.class);

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
}
