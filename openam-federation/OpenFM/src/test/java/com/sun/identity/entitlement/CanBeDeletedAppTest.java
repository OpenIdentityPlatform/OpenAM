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
 * $Id: CanBeDeletedAppTest.java,v 1.1 2010/01/08 22:20:46 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dennis
 */
public class CanBeDeletedAppTest {
    private static final String APPL_NAME = "CanBeDeletedAppTestAppl";
    private static final String PRIVILEGE_NAME = "CanBeDeletedAppTestPrivilege";
    private static final String REFERRAL_NAME = "CanBeDeletedAppTestReferralP";
    private static final String DELEGATE_PRIVILEGE_NAME =
        "CanBeDeletedAppTestApplicationPrivilege";
    private static final String SUB_REALM = "/CanBeDeletedAppTestSubRealm";
    private SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    @BeforeClass
    public void setup() throws Exception {
        if (!migrated) {
            return;
        }

        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);

        createAppl();
        createPrivilege();
        createApplicationPrivilege();
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }

        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM.substring(1);
        ocm.deleteSubOrganization(subRealm, true);
    }

    private void createAppl() throws EntitlementException,
        InstantiationException, IllegalAccessException {
        Application appl = new Application(APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));

        // Test disabled, unable to fix model change
        // Set<String> appResources = new HashSet<String>();
        // appResources.add("http://www.CanBeDeletedAppTest.com/*");
        // appl.addResources(appResources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);
    }

    private void createPrivilege() throws EntitlementException {
        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        Privilege p = Privilege.getNewInstance();
        p.setName(PRIVILEGE_NAME);
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", true);
        Entitlement entitlement = new Entitlement(APPL_NAME,
            "http://www.CanBeDeletedAppTest.com/*", actionValues);
        p.setEntitlement(entitlement);
        p.setSubject(new AuthenticatedUsers());
        pm.add(p);
    }

    private void createApplicationPrivilege() throws Exception {
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = new ApplicationPrivilege(
            DELEGATE_PRIVILEGE_NAME);
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID("uid=demo,ou=user,dc=openam,dc=openidentityplatform,dc=org");
        Set<SubjectImplementation> subjects = new
            HashSet<SubjectImplementation>();
        subjects.add(sbj);
        ap.setSubject(subjects);

        Map<String, Set<String>> appRes = new HashMap<String, Set<String>>();
        Set<String> res = new HashSet<String>();
        appRes.put(APPL_NAME, res);
        res.add("http://www.CanBeDeletedAppTest.com");
        ap.setApplicationResources(appRes);
        ap.setActionValues(
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE);
        mgr.addPrivilege(ap);
    }

    @Test
    public void test() throws Exception {
        // at this point, we have privilege, referral privilege and application
        // privilege, so application cannot be deleted.
        try {
            ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 404) {
                throw e;
            }
        }

        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        pm.remove(PRIVILEGE_NAME);

        // at this point, we have referral privilege and application
        // privilege, so application cannot be deleted.
        try {
            ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 404) {
                throw e;
            }
        }

        // at this point, we still have application privilege, so application
        // still cannot be deleted.
        try {
            ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 404) {
                throw e;
            }
        }

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        apm.removePrivilege(DELEGATE_PRIVILEGE_NAME);

        // can delete now
        ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);
    }
}
