/*
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
 * $Id: ApplicationTypeTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ApplicationTypeTest {
    private static final String APPL_NAME = "ApplicationTypeTestApp";
    private static final String DESC = "doh";
    private Subject adminSubject;

    @BeforeClass
    public void setup() throws EntitlementException, InstantiationException,
        IllegalAccessException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        adminSubject = SubjectUtils.createSubject(adminToken);
        Application appl = new Application(APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));
        // Test disabled, unable to fix model change
        // Set<String> resources = new HashSet<String>();
        // resources.add("http://*");
        // appl.addResources(resources);
        appl.setEntitlementCombiner(DenyOverride.class);
        appl.setDescription(DESC);
        
        Set<String> subjects = new HashSet<String>();
        subjects.add("com.sun.identity.admin.model.OrViewSubject");
        appl.setSubjects(subjects);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);
    }

    @AfterClass
    public void cleanup() throws EntitlementException {
        ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);
    }

    @Test
    public void testApplicationType() throws Exception {
        ApplicationType appType = ApplicationTypeManager.getAppplicationType(
            adminSubject, ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        if (appType == null) {
            throw new Exception("ApplicationTypeTest.testApplicationType cannot get application type");
        }
        ApplicationTypeManager.saveApplicationType(adminSubject, appType);
        appType = ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        if (appType == null) {
            throw new Exception("ApplicationTypeTest.testApplicationType application type lost");
        }
    }
    
    @Test
    public void testApplication() throws Exception {
        Application app = ApplicationServiceTestHelper.getApplication(
                adminSubject, "/", APPL_NAME);
        if (app == null) {
            throw new Exception("ApplicationTypeTest.testApplication cannot get application");
        }

        // Test disabled, unable to fix model change.
        // Map<String, Boolean> actions = new HashMap<String, Boolean>();
        // actions.put("action", true);
        // app.setActions(actions);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", app);

        app = ApplicationServiceTestHelper.getApplication(
                adminSubject, "/", APPL_NAME);
        if (app == null) {
            throw new Exception("ApplicationTypeTest.testApplication application lost");
        }

        // Test disabled, unable to fix model change.
        // actions = app.getActions();
        // if ((actions == null) || (actions.size()!= 1)) {
        //     throw new Exception("ApplicationTypeTest.testApplication action is not saved");
        // }

        // Boolean actionVal = actions.get("action");
        // if ((actionVal == null) || !actionVal) {
        //     throw new Exception("ApplicationTypeTest.testApplication \"action\" is not saved");
        // }

        Set<String> subjects = app.getSubjects();
        if (!subjects.contains("com.sun.identity.admin.model.OrViewSubject")) {
            throw new Exception(
                "ApplicationTypeTest.testApplication, subject test fails");
        }

        String desc = app.getDescription();
        if ((desc == null) || !desc.equals(DESC)) {
            throw new Exception(
                "ApplicationTypeTest.testApplication, description test fails");
        }
    }
}
