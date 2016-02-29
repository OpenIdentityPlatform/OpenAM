/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ApplicationCreatorNDateTest.java,v 1.1 2010/01/11 20:19:06 veiming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class ApplicationCreatorNDateTest {
    private static final String APPL_NAME = "ApplicationCreatorNDateTest";
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    @BeforeClass
    public void setup()
        throws Exception {

        if (!migrated) {
            return;
        }

        Application appl = new Application(APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));

        // Test disabled, unable to fix model change
        // Set<String> appResources = new HashSet<String>();
        // appResources.add("http://www.ApplicationCreatorNDateTest.com/*");
        // appl.addResources(appResources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }

        ApplicationServiceTestHelper.deleteApplication(
                adminSubject, "/", APPL_NAME);
    }

    @Test
    public void test() throws Exception {
        Application appl = ApplicationServiceTestHelper.getApplication(
                adminSubject, "/", APPL_NAME);

        long creationDate = appl.getCreationDate();
        String createdBy = appl.getCreatedBy();

        //reset createdBy and creationDate
        appl.setCreatedBy(null);
        appl.setCreationDate(-1);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);

        appl = ApplicationServiceTestHelper.getApplication(
                adminSubject, "/", APPL_NAME);

        if (!appl.getCreatedBy().equals(createdBy)) {
            throw new Exception(
                "ApplicationCreatorNDateTest: createdBy is reset");
        }

        if (appl.getCreationDate() != creationDate) {
            throw new Exception(
                "ApplicationCreatorNDateTest: creation date is reset");
        }
    }
}
