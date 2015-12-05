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

package org.forgerock.openam.selfservice;

import static org.forgerock.openam.audit.AuditConstants.Component.USERS;
import static org.forgerock.openam.rest.Routers.ssoToken;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.openam.rest.AbstractRestRouteProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.selfservice.config.beans.ForgottenPasswordConsoleConfig;
import org.forgerock.openam.selfservice.config.beans.ForgottenUsernameConsoleConfig;
import org.forgerock.openam.selfservice.config.beans.UserRegistrationConsoleConfig;
import org.forgerock.selfservice.core.UserUpdateService;

/**
 * Provides routes for the user self service services.
 *
 * @since 13.0.0
 */
public final class SelfServiceRestRouteProvider extends AbstractRestRouteProvider {

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {
        realmRouter
                .route("/selfservice/userRegistration")
                .authenticateWith(
                        ssoToken()
                                .exceptRead()
                                .exceptActions("submitRequirements"))
                .toRequestHandler(RoutingMode.STARTS_WITH, Key
                        .get(new TypeLiteral<SelfServiceRequestHandler<UserRegistrationConsoleConfig>>() { }));

        realmRouter
                .route("/selfservice/forgottenPassword")
                .authenticateWith(
                        ssoToken()
                                .exceptRead()
                                .exceptActions("submitRequirements"))
                .toRequestHandler(RoutingMode.STARTS_WITH, Key
                        .get(new TypeLiteral<SelfServiceRequestHandler<ForgottenPasswordConsoleConfig>>() { }));

        realmRouter
                .route("/selfservice/forgottenUsername")
                .authenticateWith(
                        ssoToken()
                                .exceptRead()
                                .exceptActions("submitRequirements"))
                .toRequestHandler(RoutingMode.STARTS_WITH, Key
                        .get(new TypeLiteral<SelfServiceRequestHandler<ForgottenUsernameConsoleConfig>>() { }));

        realmRouter
                .route("/selfservice/user")
                .auditAs(USERS)
                .authenticateWith(ssoToken())
                .toCollection(UserUpdateService.class);

        realmRouter
                .route("selfservice/kba")
                .authenticateWith(ssoToken())
                .toSingleton(KbaResource.class);
    }

}
