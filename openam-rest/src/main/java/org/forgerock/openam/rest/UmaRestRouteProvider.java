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

import static org.forgerock.openam.audit.AuditConstants.Component.UMA;

import org.forgerock.openam.forgerockrest.AuditHistory;
import org.forgerock.openam.rest.authz.ResourceOwnerOrSuperUserAuthzModule;
import org.forgerock.openam.rest.uma.PendingRequestResource;
import org.forgerock.openam.rest.uma.UmaConfigurationResource;
import org.forgerock.openam.rest.uma.UmaEnabledFilter;
import org.forgerock.openam.rest.uma.UmaPolicyResource;
import org.forgerock.openam.rest.uma.UmaPolicyResourceAuthzFilter;

/**
 * A {@link RestRouteProvider} that add routes for all the UMA endpoints.
 *
 * @since 13.0.0
 */
public class UmaRestRouteProvider implements RestRouteProvider {

    @Override
    public void addRoutes(RestRouter rootRouter, RestRouter realmRouter) {

        realmRouter.route("serverinfo/uma")
                .auditAs(UMA)
                .toSingleton(UmaConfigurationResource.class);

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
    }
}
