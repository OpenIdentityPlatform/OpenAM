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
 * $Id: DelegationIsAllowedSubResourceTest.java,v 1.3 2009/12/22 18:00:24 veiming Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.AuthUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.Collections;
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
public class DelegationIsAllowedSubResourceTest {
    private static final String APPL_NAME =
        "DelegationIsAllowedSubResourceTest";
    private static final String USER1 =
        "DelegationIsAllowedSubResourceTestUser1";
    private static final String PRIVILEGE_NAME =
        "DelegationIsAllowedSubResourceTestPrivilege";
    private static final String PRIVILEGE_NAME1 = PRIVILEGE_NAME + "1";

    private static final String DELEGATE_PRIVILEGE_NAME =
        "DelegationIsAllowedSubResourceTestDelegationPrivilege";
    private static final String DELEGATED_RESOURCE_BASE =
        "http://www.www.delegationisallowedsubresourcetest.com.com";
    private static final String DELEGATED_RESOURCE = DELEGATED_RESOURCE_BASE +
        "/user";

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

        user1 = IdRepoUtils.createUser("/", USER1);
        createDelegationPrivilege();
        createPrivilege();
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }

        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        pm.remove(PRIVILEGE_NAME1);

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        apm.removePrivilege(DELEGATE_PRIVILEGE_NAME);

        IdRepoUtils.deleteIdentity("/", user1);
        ApplicationServiceTestHelper.deleteApplication(
                adminSubject, "/", APPL_NAME);
    }

    private void createPrivilege() throws EntitlementException {
        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", Boolean.TRUE);
        Entitlement entitlement = new Entitlement(APPL_NAME,
            DELEGATED_RESOURCE_BASE, actionValues);
        OpenSSOUserSubject subject = new OpenSSOUserSubject(
            "id=isallowedtestdummy,ou=user," + SMSEntry.getRootSuffix());
        Privilege privilege1 = Privilege.getNewInstance();
        privilege1.setName(PRIVILEGE_NAME1);
        privilege1.setEntitlement(entitlement);
        privilege1.setSubject(subject);
        pm.add(privilege1);
    }

    private void createDelegationPrivilege()
        throws SMSException, EntitlementException, SSOException,
        IdRepoException,
        InterruptedException {

        ApplicationPrivilege ap = new ApplicationPrivilege(
            DELEGATE_PRIVILEGE_NAME);
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID(user1.getUniversalId());
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
        Set<String> actions = new HashSet<String>();
        actions.add("READ");
        SSOToken token = AuthUtils.authenticate("/", USER1, USER1);
        DelegationPermission dp = new DelegationPermission("/",
            "sunEntitlementService", "1.0", "application", 
            "default/application/*",
            actions, null);
        DelegationEvaluator de = new DelegationEvaluatorImpl();
        if (!de.isAllowed(token, dp, Collections.EMPTY_MAP, true)) {
            throw new Exception(
                "DelegationIsAllowedSubResourceTest.test: failed");
        }
    }
}
