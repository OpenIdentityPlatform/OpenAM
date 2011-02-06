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
 * $Id: CreateApplicationTest.java,v 1.1 2009/08/21 22:27:55 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.IArgument;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;


public class CreateApplicationTest extends CLITestImpl {
    private static final String APPL_NAME = "CreateApplicationTestAppl";

    @AfterClass
    public void cleanup() throws EntitlementException {
        ApplicationManager.deleteApplication(SubjectUtils.createSubject(
            adminToken), "/", APPL_NAME);
    }

    @Test
    public void createApp() throws CLIException {
        String[] args = new String[18];
        args[0] = "create-appl";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            CreateApplication.PARAM_APPL_NAME;
        args[4] = APPL_NAME;
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            CreateApplication.PARAM_APPL_TYPE_NAME;
        args[6] = ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;

        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.ATTRIBUTE_VALUES;
        args[8] = CreateApplication.ATTR_ACTIONS + "=GET=true";
        args[9] = CreateApplication.ATTR_ACTIONS + "=POST=true";
        args[10] = CreateApplication.ATTR_CONDITIONS +
            "=com.sun.identity.admin.model.DateRangeCondition";
        args[11] = CreateApplication.ATTR_SUBJECTS +
            "=com.sun.identity.admin.model.IdRepoGroupViewSubject";
        args[12] = CreateApplication.ATTR_RESOURCES +
            "=https://";
        args[13] = CreateApplication.ATTR_SUBJECT_ATTRIBUTE_NAMES +
            "=uid";
        args[14] = CreateApplication.ATTR_ENTITLEMENT_COMBINER +
            "=com.sun.identity.entitlement.DenyOverride";
        args[15] = CreateApplication.ATTR_RESOURCE_COMPARATOR +
            "=com.sun.identity.entitlement.URLResourceName";
        args[16] = CreateApplication.ATTR_SAVE_INDEX +
            "=com.sun.identity.entitlement.util.ResourceNameIndexGenerator";
        args[17] = CreateApplication.ATTR_SEARCH_INDEX +
            "=com.sun.identity.entitlement.util.ResourceNameSplitter";

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
    }
}
