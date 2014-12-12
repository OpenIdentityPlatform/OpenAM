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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.forgerockrest;

import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.forgerockrest.XacmlService;
import org.forgerock.openam.forgerockrest.utils.RestLog;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Allow testing of XacmlService without the unpleasantness of mocking a dozen static functions and classes.
 */
public class XacmlServiceTestWrapper extends XacmlService {

    /**
     * Constructor with dependencies exposed for unit testing.
     *
     * @param importExport Non null utility functions.
     * @param adminTokenAction Non null admin action function.
     * @param debug The debug instance for logging.
     */
    @Inject
    public XacmlServiceTestWrapper(XACMLExportImport importExport,
                                   AdminTokenAction adminTokenAction,
                                   @Named("frRest") Debug debug,
                                   RestLog restLog,
                                   DelegationEvaluatorImpl evaluator) {
        super(importExport, adminTokenAction, debug, restLog, evaluator);
    }

    @Override
    protected boolean checkPermission(String ignored) {
        return true;
    }
}
