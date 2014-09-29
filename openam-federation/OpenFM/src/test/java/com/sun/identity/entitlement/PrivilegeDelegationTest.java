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
 * $Id: PrivilegeDelegationTest.java,v 1.4 2009/11/12 18:37:39 veiming Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.AuthUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PrivilegeDelegationTest {
    private static final String DELEG_PRIVILEGE_NAME =
        "PrivilegeDelegationTestDelegationPrivilege";
    private static final String PRIVILEGE_NAME =
        "PrivilegeDelegationTestPrivilege";
    private static final String RESOURCE =
        "http://www.privilegedelegationtest.com";
    private static final String USER_NAME = "PrivilegeDelegationTestUser";
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
        AdminTokenAction.getInstance());
    private AMIdentity user;
    private String realm = "/";
    private Privilege privilegeObject;
    private SSOToken token;

    @BeforeClass
    public void setup() throws Exception {
        user = IdRepoUtils.createUser(realm, USER_NAME);
        PrivilegeManager pm = PrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(adminToken));
        Map<String, Boolean> actions = new HashMap<String, Boolean>();
        actions.put("GET", Boolean.TRUE);
        Entitlement ent = new Entitlement(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME,
            RESOURCE + "/index.html", actions);
        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(PRIVILEGE_NAME);
        privilege.setEntitlement(ent);
        privilege.setSubject(new AnyUserSubject());
        pm.add(privilege);

        privilegeObject = pm.findByName(PRIVILEGE_NAME);
        token = AuthUtils.authenticate(realm, USER_NAME, USER_NAME);
    }

    @AfterClass
    public void cleanup() throws Exception {
        IdRepoUtils.deleteIdentity(realm, user);

        PrivilegeManager pm = PrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(adminToken));
        pm.remove(PRIVILEGE_NAME);
    }

    @Test
    public void negativeTest() throws Exception {
        PrivilegeManager mgr = PrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(token));
        try {
            mgr.add(privilegeObject);
            throw new Exception(
                "PrivilegeDelegationTest.negativeTest: modify privilege should be deny");
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 326) {
                throw e;
            }
        }
        try {
            mgr.modify(privilegeObject);
            throw new Exception(
                "PrivilegeDelegationTest.negativeTest: modify privilege should be deny");
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 326) {
                throw e;
            }
        }
        try {
            mgr.modify(privilegeObject);
            throw new Exception(
                "PrivilegeDelegationTest.negativeTest: modify privilege should be deny");
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 326) {
                throw e;
            }
        }
        try {
            mgr.remove(PRIVILEGE_NAME);
            throw new Exception(
                "PrivilegeDelegationTest.negativeTest: remove privilege should be deny");
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 326) {
                throw e;
            }
        }

    }

    @Test (dependsOnMethods = {"negativeTest"})
    public void positiveGetTest() throws Exception {
        ApplicationPrivilegeManager apm = createApplicationPrivilege(
            ApplicationPrivilege.PossibleAction.READ);
        PrivilegeManager mgr = PrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(token));
        mgr.findByName(PRIVILEGE_NAME);
        try {
            mgr.modify(privilegeObject);
            throw new Exception(
                "PrivilegeDelegationTest.positiveGetTest: modify privilege should be deny");
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 326) {
                throw e;
            }
        }
        apm.removePrivilege(DELEG_PRIVILEGE_NAME);
    }

    @Test (dependsOnMethods = {"positiveGetTest"})
    public void positiveModifyTest() throws Exception {
        ApplicationPrivilegeManager apm = createApplicationPrivilege(
            ApplicationPrivilege.PossibleAction.READ_MODIFY);
        PrivilegeManager mgr = PrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(token));
        mgr.findByName(PRIVILEGE_NAME);
        mgr.modify(privilegeObject);
        apm.removePrivilege(DELEG_PRIVILEGE_NAME);
    }

    private ApplicationPrivilegeManager createApplicationPrivilege(
        ApplicationPrivilege.PossibleAction actions) 
        throws EntitlementException {
        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance(
            realm, SubjectUtils.createSubject(adminToken));
        ApplicationPrivilege ap = new ApplicationPrivilege(
            DELEG_PRIVILEGE_NAME);
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID(user.getUniversalId());
        Set<SubjectImplementation> subjects = new
            HashSet<SubjectImplementation>();
        subjects.add(sbj);
        ap.setSubject(subjects);

        Set<String> res = new HashSet<String>();
        Map<String, Set<String>> appRes = new HashMap<String, Set<String>>();
        appRes.put(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, res);
        res.add(RESOURCE + "/*");
        ap.setApplicationResources(appRes);
        ap.setActionValues(actions);
        apm.addPrivilege(ap);
        return apm;
    }
}
