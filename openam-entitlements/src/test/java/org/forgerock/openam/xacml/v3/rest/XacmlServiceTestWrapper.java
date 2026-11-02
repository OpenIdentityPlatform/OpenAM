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

import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;

/**
 * Allow testing of XacmlService without the unpleasantness of mocking a dozen static functions and classes.
 */
public class XacmlServiceTestWrapper extends XacmlService {

    private final boolean permitted;
    private String lastAction;

    /**
     * Constructor with dependencies exposed for unit testing.
     *
     * @param importExport Non null utility functions.
     * @param adminTokenAction Non null admin action function.
     * @param debug The debug instance for logging.
     * @param restLog The REST audit logger.
     * @param evaluator The delegation evaluator.
     * @param permitted What the permission check answers.
     */
    public XacmlServiceTestWrapper(XACMLExportImport importExport, AdminTokenAction adminTokenAction, Debug debug,
            RestLog restLog, DelegationEvaluator evaluator, boolean permitted) {
        super(importExport, adminTokenAction, debug, restLog, evaluator);
        this.permitted = permitted;
    }

    @Override
    boolean checkPermission(Context context, String action) {
        lastAction = action;
        return permitted;
    }

    /**
     * @return The action of the last permission check, or null if none was made.
     */
    String getLastAction() {
        return lastAction;
    }
}
