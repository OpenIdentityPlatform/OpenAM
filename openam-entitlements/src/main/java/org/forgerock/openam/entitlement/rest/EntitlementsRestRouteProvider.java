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

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.openam.audit.AuditConstants.Component.POLICY;

import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.RestRouter;
import org.forgerock.openam.rest.authz.PrivilegeAuthzModule;

/**
 * A {@link RestRouteProvider} that add routes for all the entitlement (policy)
 * endpoints.
 *
 * @since 13.0.0
 */
public class EntitlementsRestRouteProvider implements RestRouteProvider {

    @Override
    public void addRoutes(RestRouter rootRouter, RestRouter realmRouter) {
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
    }
}
