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
 * $Id: ApplicationPrivilegeMetaTest.java,v 1.1 2009/11/19 00:08:52 veiming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class ApplicationPrivilegeMetaTest {
    private static final String APPL_NAME = "ApplicationPrivilegeMetaTest";
    private static final String DELEGATE_PRIVILEGE_NAME =
        "ApplicationPrivilegeMetaTestPrivilege";
    private static final String DELEGATED_RESOURCE_BASE =
        "http://www.applicationdelegationtest.com";
    private static final String DELEGATED_RESOURCE =
        "http://www.applicationdelegationtest.com/user";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;
    private AMIdentity user1;

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
        // appResources.add(DELEGATED_RESOURCE_BASE);
        // appl.addResources(appResources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);
        createDelegationPrivilege();
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        apm.removePrivilege(DELEGATE_PRIVILEGE_NAME);

        IdRepoUtils.deleteIdentity("/", user1);
        ApplicationServiceTestHelper.deleteApplication(
                adminSubject, "/", APPL_NAME);
    }

    private void createDelegationPrivilege()
        throws SMSException, EntitlementException, SSOException,
        IdRepoException,
        InterruptedException {

        ApplicationPrivilege ap = new ApplicationPrivilege(
            DELEGATE_PRIVILEGE_NAME);
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID("id=dummy,dc=openam,dc=openidentityplatform,dc=org");
        Set<SubjectImplementation> subjects = new HashSet<SubjectImplementation>();
        subjects.add(sbj);
        ap.setSubject(subjects);

        Map<String, Set<String>> appRes = new HashMap<String, Set<String>>();
        Set<String> res = new HashSet<String>();
        appRes.put(APPL_NAME, res);
        res.add(DELEGATED_RESOURCE);
        ap.setApplicationResources(appRes);
        ap.setActionValues(ApplicationPrivilege.PossibleAction.READ);

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        apm.addPrivilege(ap);
    }

    @Test
    public void test() throws Exception {
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(
            "/", PrivilegeManager.superAdminSubject);
        ApplicationPrivilege ap = mgr.getPrivilege(DELEGATE_PRIVILEGE_NAME);
        long modifiedTime = ap.getLastModifiedDate();

        ap.setDescription("123");
        mgr.replacePrivilege(ap);
        ap = mgr.getPrivilege(DELEGATE_PRIVILEGE_NAME);
        long modifiedTime2 = ap.getLastModifiedDate();

        if (modifiedTime == modifiedTime2) {
            throw new Exception(
                "ApplicationPrivilegeMetaTest.test: modified time remains unchanged");
        }
    }
}
