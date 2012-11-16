/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: DeleteApplicationsTest.java,v 1.1 2009/08/21 22:27:55 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.IArgument;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementException;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class DeleteApplicationsTest extends CLITestImpl {
    private static final String APPL_NAME = "DeleteApplicationTestAppl";

    @BeforeClass
    @Override
    public void setup() throws Exception {
        super.setup();
        Application appl = ApplicationManager.newApplication("/", APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));

        Map<String, Boolean> actions = new HashMap<String, Boolean>();
        actions.put("GET", true);
        appl.setActions(actions);
        appl.setEntitlementCombiner(DenyOverride.class);

        ApplicationManager.saveApplication(adminSubject, "/", appl);
    }

    @AfterClass
    public void cleanup() throws EntitlementException {
        ApplicationManager.deleteApplication(adminSubject, "/", APPL_NAME);
    }

    @Test
    public void deleteApp() throws CLIException {
        String[] args = new String[5];
        args[0] = "delete-appls";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            CreateApplication.PARAM_APPL_NAMES;
        args[4] = APPL_NAME;

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
    }
}
