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

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.openam.audit.AuditConstants.Component.POLICY;

import org.forgerock.openam.rest.AbstractRestRouteProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.authz.PrivilegeAuthzModule;
import org.forgerock.openam.rest.authz.PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule;
import org.forgerock.openam.rest.fluent.PoliciesAuditFilter;

/**
 * A {@link RestRouteProvider} that adds routes for all the entitlement (policy)
 * endpoints.
 *
 * @since 13.0.0
 */
public class EntitlementsRestRouteProvider extends AbstractRestRouteProvider {

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {
        realmRouter.route("policies")
                .auditAs(POLICY, PoliciesAuditFilter.class)
                .authorizeWith(PrivilegeAuthzModule.class)
                .forVersion(1)
                .through(PolicyV1Filter.class)
                .toCollection(PolicyResource.class)
                .forVersion(2)
                .toCollection(PolicyResource.class)
                .forVersion(2, 1)
                .toCollection(PolicyResourceWithCopyMoveSupport.class);

        realmRouter.route("applications")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .forVersion(1)
                .through(ApplicationV1Filter.class)
                .toCollection(ApplicationsResource.class)
                .forVersion(2, 1)
                .toCollection(ApplicationsResource.class);

        realmRouter.route("subjectattributes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(SubjectAttributesResourceV1.class);

        realmRouter.route("resourcetypes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(ResourceTypesResource.class);

        rootRouter.route("applicationtypes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class)
                .toCollection(ApplicationTypesResource.class);

        rootRouter.route("decisioncombiners")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class)
                .toCollection(DecisionCombinersResource.class);

        rootRouter.route("conditiontypes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class)
                .toCollection(ConditionTypesResource.class);

        rootRouter.route("subjecttypes")
                .auditAs(POLICY)
                .authorizeWith(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class)
                .toCollection(SubjectTypesResource.class);
    }
}
