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
 * $Id: ApplicationDelegationTest.java,v 1.2 2009/11/12 18:37:39 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class ApplicationDelegationTest {
    private static final String APPL_NAME = "ApplicationDelegationTest";
    private static final String USER1 = "ApplicationDelegationTestUser1";
    private static final String PRIVILEGE_NAME =
        "ApplicationDelegationTestPrivilege";
    private static final String PRIVILEGE_NAME1 = PRIVILEGE_NAME + "1";
    private static final String PRIVILEGE_NAME2 = PRIVILEGE_NAME + "2";

    private static final String DELEGATE_PRIVILEGE_NAME =
        "ApplicationDelegationTestDelegationPrivilege";
    private static final String DELEGATED_RESOURCE_BASE =
        "http://www.applicationdelegationtest.com";
    private static final String DELEGATED_RESOURCE =
        "http://www.applicationdelegationtest.com/user";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;
    private AMIdentity user1;
    private Subject testUserSubject;

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
        createPrivileges();
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }

        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        pm.remove(PRIVILEGE_NAME1);
        pm.remove(PRIVILEGE_NAME2);

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        apm.removePrivilege(DELEGATE_PRIVILEGE_NAME);

        IdRepoUtils.deleteIdentity("/", user1);
        ApplicationServiceTestHelper.deleteApplication(
                adminSubject, "/", APPL_NAME);
    }

    private void createPrivileges() throws EntitlementException {
        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", Boolean.TRUE);
        Entitlement entitlement = new Entitlement(APPL_NAME,
            DELEGATED_RESOURCE_BASE, actionValues);
        OpenSSOUserSubject subject = new OpenSSOUserSubject(
            "id=dummy,ou=user," + SMSEntry.getRootSuffix());
        Privilege privilege1 = Privilege.getNewInstance();
        privilege1.setName(PRIVILEGE_NAME1);
        privilege1.setEntitlement(entitlement);
        privilege1.setSubject(subject);
        pm.add(privilege1);

        Privilege privilege2 = Privilege.getNewInstance();
        privilege2.setName(PRIVILEGE_NAME2);
        entitlement.setResourceName(DELEGATED_RESOURCE);
        privilege2.setEntitlement(entitlement);
        privilege2.setSubject(subject);
        pm.add(privilege2);
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

    private SSOToken authenticate(String userName, String password)
        throws Exception {
        AuthContext lc = new AuthContext("/");
        lc.login();
        while (lc.hasMoreRequirements()) {
            Callback[] callbacks = lc.getRequirements();
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName(userName);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(password.toCharArray());
                } else {
                    throw new Exception("No callback");
                }
            }
            lc.submitRequirements(callbacks);
        }

        return (lc.getStatus() != AuthContext.Status.SUCCESS) ? null : lc.
            getSSOToken();
    }

    @Test
    public void negativeTest() throws Exception {
        SSOToken ssoToken = authenticate(USER1, USER1);
        testUserSubject = SubjectUtils.createSubject(ssoToken);
        Application appl = ApplicationServiceTestHelper.getApplication(
                testUserSubject, "/", APPL_NAME);
        //should be able to get application but cannot save it
        // because he is not a policy administrator

        try {
            ApplicationServiceTestHelper.saveApplication(testUserSubject, "/", appl);
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 326) {
                throw e;
            }
        }


    }


    @Test (dependsOnMethods={"negativeTest"})
    public void test() throws Exception {
        Application appl = ApplicationServiceTestHelper.getApplication(
                testUserSubject, "/", APPL_NAME);

        // Test disabled, unable to fix model change.
        // Set<String> resources = appl.getResources();
        
        // if ((resources.size() != 1) &&
        //     !resources.contains(DELEGATED_RESOURCE)) {
        //     throw new Exception("ApplicationDelegationTest.test: " +
        //         "incorrect resource in application object");
        // }

        Set<SearchFilter> filter = new HashSet<SearchFilter>();
        filter.add(new SearchFilter(
            Privilege.NAME_SEARCH_ATTRIBUTE, PRIVILEGE_NAME + "*"));
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            testUserSubject);
        Set<String> names = pm.searchNames(filter);

        // there are two privileges created in this test.
        // test user should be able to see only one.
        // the one that he has read permission base on the resource
        // that is delegated to him
        if (names.size() != 1) {
            throw new Exception("ApplicationDelegationTest.test: " +
                "privilege manager is not returning the correct search privilege name");
        }
    }
}
