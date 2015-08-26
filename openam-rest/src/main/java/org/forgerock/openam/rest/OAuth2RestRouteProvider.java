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

import static org.forgerock.openam.audit.AuditConstants.Component.OAUTH2;

import org.forgerock.openam.forgerockrest.UmaLabelResource;
import org.forgerock.openam.rest.authz.ResourceOwnerOrSuperUserAuthzModule;
import org.forgerock.openam.rest.oauth2.ResourceSetResource;
import org.forgerock.openam.rest.uma.UmaEnabledFilter;

/**
 * A {@link RestRouteProvider} that add routes for all the OAuth2 endpoints.
 *
 * @since 13.0.0
 */
public class OAuth2RestRouteProvider implements RestRouteProvider {

    @Override
    public void addRoutes(RestRouter rootRouter, RestRouter realmRouter) {

        realmRouter.route("users/{user}/oauth2/resources/sets")
                .auditAs(OAUTH2)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .through(UmaEnabledFilter.class)
                .toCollection(ResourceSetResource.class);

        realmRouter.route("users/{user}/oauth2/resources/labels")
                .auditAs(OAUTH2)
                .authorizeWith(ResourceOwnerOrSuperUserAuthzModule.class)
                .through(UmaEnabledFilter.class)
                .toCollection(UmaLabelResource.class);
    }
}
