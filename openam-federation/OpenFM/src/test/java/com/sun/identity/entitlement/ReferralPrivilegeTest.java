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
 * $Id: ReferralPrivilegeTest.java,v 1.1 2009/12/07 19:46:50 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.AuthUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
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
 * One referral privilege in root realm pointing to two sub realm.
 */
public class ReferralPrivilegeTest {
    private static final String SUB_REALM1 = "/ReferredPrivilegeTest1";
    private static final String SUB_REALM2 = "/ReferredPrivilegeTest2";
    private static final String REFERRAL_NAME = 
        "ReferredPrivilegeTestReferral";
    private static final String USER_NAME =  "ReferredPrivilegeTestUser";
    private static final String PRIVILEGE_NAME =
        "ReferredPrivilegeTestPrivilege";
    private static final String BASE_URL =
        "http://www.ReferralPrivilegeTest.com";

    private SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;
    private AMIdentity user;

    @BeforeClass
    public void setup() throws Exception {
        if (!migrated) {
            return;
        }

        createOrgs();
        user = IdRepoUtils.createUser("/", USER_NAME);
        createReferral1();
        createPrivilege();
    }

    private void createOrgs() throws Exception {
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM1.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);
        subRealm = SUB_REALM2.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);
    }

    private void createReferral1()
        throws EntitlementException {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(BASE_URL + "/sub/*");
        map.put(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, set);
        Set<String> realms = new HashSet<String>();
        realms.add(SUB_REALM1);
        realms.add(SUB_REALM2);
        ReferralPrivilege r1 = new ReferralPrivilege(REFERRAL_NAME,
            map, realms);
    }

    private void createPrivilege() throws EntitlementException {
        PrivilegeManager pm = PrivilegeManager.getInstance(SUB_REALM2,
            adminSubject);
        Privilege p = Privilege.getNewInstance();
        p.setName(PRIVILEGE_NAME);
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", true);
        Entitlement entitlement = new Entitlement(
            BASE_URL + "/sub/*", actionValues);
        p.setEntitlement(entitlement);
        EntitlementSubject eSubject = new OpenSSOUserSubject(
            user.getUniversalId());
        p.setSubject(eSubject);
        pm.add(p);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        IdRepoUtils.deleteIdentity("/", user);
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        ocm.deleteSubOrganization(SUB_REALM1.substring(1), true);
        ocm.deleteSubOrganization(SUB_REALM2.substring(1), true);
    }

    @Test
    public void test() throws Exception {
        SSOToken token = AuthUtils.authenticate("/", USER_NAME, USER_NAME);
        Evaluator evaluator = new Evaluator(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        Set actions = new HashSet();
        actions.add("GET");
        boolean result = evaluator.hasEntitlement("/",
            SubjectUtils.createSubject(token),
            new Entitlement(BASE_URL + "/sub/test.jsp", actions),
            Collections.EMPTY_MAP);

        if (!result) {
            throw new Exception("ReferralPrivilegeTest.test: failed");
        }
    }
}
