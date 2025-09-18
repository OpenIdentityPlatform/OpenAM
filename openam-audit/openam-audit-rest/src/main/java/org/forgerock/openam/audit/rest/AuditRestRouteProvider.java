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
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.audit.rest;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.openam.audit.AuditConstants.Component.AUDIT;

import jakarta.inject.Inject;

import org.forgerock.openam.audit.AuditServiceProvider;
import org.forgerock.openam.rest.AbstractRestRouteProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.authz.SpecialOrAdminOrAgentAuthzModule;
import org.forgerock.openam.rest.fluent.AuditEndpointAuditFilter;

/**
 * A {@link RestRouteProvider} that add routes for the audit endpoint.
 *
 * @since 13.0.0
 */
public class AuditRestRouteProvider extends AbstractRestRouteProvider {
    private AuditServiceProvider auditServiceProvider;

    /**
     * Inject the service provider.
     * @param auditServiceProvider The provider.
     */
    @Inject
    public void setProvider(AuditServiceProvider auditServiceProvider) {
        this.auditServiceProvider = auditServiceProvider;
    }

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {
        rootRouter.route("global-audit")
                .auditAs(AUDIT, AuditEndpointAuditFilter.class)
                .authorizeWith(SpecialOrAdminOrAgentAuthzModule.class)
                .forVersion(1)
                .toRequestHandler(STARTS_WITH, auditServiceProvider.getDefaultAuditService());

        realmRouter.route("realm-audit")
                .auditAs(AUDIT, AuditEndpointAuditFilter.class)
                .authorizeWith(SpecialOrAdminOrAgentAuthzModule.class)
                .forVersion(1)
                .toRequestHandler(STARTS_WITH, RealmAuditRequestHandler.class);
    }
}
