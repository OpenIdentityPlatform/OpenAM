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
 * Portions copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.xacml.v3.rest;

import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.services.context.Context;

import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPermissionFactory;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;

import static org.mockito.Mockito.mock;

/**
 * Allow testing of XacmlService without the unpleasantness of mocking a dozen static functions and classes.
 * Only the session lookup and the final permission evaluation are replaced; the permission is built by the service.
 */
public class XacmlServiceTestWrapper extends XacmlService {

    private final boolean permitted;
    private DelegationPermission lastPermission;
    private String lastResourceName;

    /**
     * Constructor with dependencies exposed for unit testing.
     *
     * @param importExport Non null utility functions.
     * @param adminTokenAction Non null admin action function.
     * @param debug The debug instance for logging.
     * @param restLog The REST audit logger.
     * @param evaluator The delegation evaluator.
     * @param permissionFactory The factory of the delegation permissions.
     * @param permitted What the permission check answers.
     */
    public XacmlServiceTestWrapper(XACMLExportImport importExport, AdminTokenAction adminTokenAction, Debug debug,
            RestLog restLog, DelegationEvaluator evaluator, DelegationPermissionFactory permissionFactory,
            boolean permitted) {
        super(importExport, adminTokenAction, debug, restLog, evaluator, permissionFactory);
        this.permitted = permitted;
    }

    @Override
    SSOToken sessionToken(Context context) {
        return mock(SSOToken.class);
    }

    @Override
    boolean checkPermission(DelegationPermission permissionRequest, SSOToken token, String urlLastSegment) {
        lastPermission = permissionRequest;
        lastResourceName = urlLastSegment;
        return permitted;
    }

    /**
     * @return The permission of the last evaluation, or null if none was made.
     */
    DelegationPermission getLastPermission() {
        return lastPermission;
    }

    /**
     * @return The resource name of the last evaluation, or null if none was made.
     */
    String getLastResourceName() {
        return lastResourceName;
    }
}
