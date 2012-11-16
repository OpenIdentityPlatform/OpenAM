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
 * $Id: SetApplicationTest.java,v 1.1 2009/08/21 22:27:55 veiming Exp $
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


public class SetApplicationTest extends CLITestImpl {
    private static final String APPL_NAME = "SetApplicationTestAppl";

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
    public void modifyApp() throws CLIException {
        String[] args = new String[16];
        args[0] = "set-appl";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            CreateApplication.PARAM_APPL_NAME;
        args[4] = APPL_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.ATTRIBUTE_VALUES;
        args[6] = CreateApplication.ATTR_ACTIONS + "=GET=true";
        args[7] = CreateApplication.ATTR_ACTIONS + "=POST=true";
        args[8] = CreateApplication.ATTR_CONDITIONS +
            "=com.sun.identity.admin.model.DateRangeCondition";
        args[9] = CreateApplication.ATTR_SUBJECTS +
            "=com.sun.identity.admin.model.IdRepoGroupViewSubject";
        args[10] = CreateApplication.ATTR_RESOURCES +
            "=https://";
        args[11] = CreateApplication.ATTR_SUBJECT_ATTRIBUTE_NAMES +
            "=uid";
        args[12] = CreateApplication.ATTR_ENTITLEMENT_COMBINER +
            "=com.sun.identity.entitlement.DenyOverride";
        args[13] = CreateApplication.ATTR_RESOURCE_COMPARATOR +
            "=com.sun.identity.entitlement.URLResourceName";
        args[14] = CreateApplication.ATTR_SAVE_INDEX +
            "=com.sun.identity.entitlement.util.ResourceNameIndexGenerator";
        args[15] = CreateApplication.ATTR_SEARCH_INDEX +
            "=com.sun.identity.entitlement.util.ResourceNameSplitter";

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
    }
}
