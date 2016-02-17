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
 * $Id: DelegationPrivilegeSubResourceTest.java,v 1.1 2009/12/22 18:00:24 veiming Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.AuthUtils;
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
public class DelegationPrivilegeSubResourceTest {
    private static final String USER1 =
        "DelegationPrivilegeSubResourceTestUser1";
    private static final String USER2 =
        "DelegationPrivilegeSubResourceTestUser2";

    private static final String DELEGATE_PRIVILEGE_NAME =
        "DelegationPrivilegeSubResourceTestDelegationPrivilege";
    private static final String DELEGATE_PRIVILEGE_NAME1 =
        "DelegationPrivilegeSubResourceTestDelegationPrivilege1";
    private static final String DELEGATE_PRIVILEGE_NAME2 =
        "DelegationPrivilegeSubResourceTestDelegationPrivilege2";

    private static final String DELEGATED_RESOURCE_BASE =
        "http://www.delegationprivilegesubresourceTest.com:8080/hr";
    private static final String DELEGATED_RESOURCE = DELEGATED_RESOURCE_BASE +
        "/sub";
    private static final String OTHER_DELEGATED_RESOURCE =
        "http://www.delegationprivilegesubresourceTest.com:8080/engr";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;
    private AMIdentity user1;
    private AMIdentity user2;

    @BeforeClass
    public void setup()
        throws Exception {

        if (!migrated) {
            return;
        }

        user1 = IdRepoUtils.createUser("/", USER1);
        user2 = IdRepoUtils.createUser("/", USER2);
        createDelegationPrivilege(DELEGATE_PRIVILEGE_NAME,
            user1.getUniversalId(), DELEGATED_RESOURCE_BASE,
            ApplicationPrivilege.PossibleAction.READ_DELEGATE);
        createDelegationPrivilege(DELEGATE_PRIVILEGE_NAME1,
            user2.getUniversalId(), DELEGATED_RESOURCE,
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE);
        createDelegationPrivilege(DELEGATE_PRIVILEGE_NAME2,
            user2.getUniversalId(), OTHER_DELEGATED_RESOURCE,
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        apm.removePrivilege(DELEGATE_PRIVILEGE_NAME);
        apm.removePrivilege(DELEGATE_PRIVILEGE_NAME1);
        apm.removePrivilege(DELEGATE_PRIVILEGE_NAME2);

        IdRepoUtils.deleteIdentity("/", user1);
        IdRepoUtils.deleteIdentity("/", user2);
    }

    private void createDelegationPrivilege(
        String dpName,
        String uuid,
        String resource,
        ApplicationPrivilege.PossibleAction actions)
        throws SMSException, EntitlementException, SSOException,
        IdRepoException,
        InterruptedException {

        ApplicationPrivilege ap = new ApplicationPrivilege(dpName);
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID(uuid);
        Set<SubjectImplementation> subjects = new HashSet<SubjectImplementation>();
        subjects.add(sbj);
        ap.setSubject(subjects);

        Map<String, Set<String>> appRes = new HashMap<String, Set<String>>();
        Set<String> res = new HashSet<String>();
        appRes.put(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, res);
        res.add(resource);
        ap.setApplicationResources(appRes);
        ap.setActionValues(actions);

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        apm.addPrivilege(ap);
    }

    @Test
    public void test() throws Exception {
        SSOToken ssoToken = AuthUtils.authenticate("/", USER1, USER1);
        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/",
            SubjectUtils.createSubject(ssoToken));

        Set<String> applNames = apm.getApplications(
            ApplicationPrivilege.Action.READ);
        if ((applNames.size() != 1) ||
            !applNames.contains(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME)
        ) {
            throw new Exception("DelegationPrivilegeSubResourceTest.test: " +
                "application names for READ action is inccorect");
        }

        applNames = apm.getApplications(ApplicationPrivilege.Action.DELEGATE);
        if ((applNames.size() != 1) ||
            !applNames.contains(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME)
        ) {
            throw new Exception("DelegationPrivilegeSubResourceTest.test: " +
                "application names for DELEGATE action is inccorect");
        }

        applNames = apm.getApplications(ApplicationPrivilege.Action.MODIFY);
        if (!applNames.isEmpty()) {
            throw new Exception("DelegationPrivilegeSubResourceTest.test: " +
                "application names for MODIFY action is inccorect");
        }

        Set<String> resources = apm.getResources(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME,
            ApplicationPrivilege.Action.READ);
        if ((resources.size() != 2) ||
            !resources.contains(DELEGATED_RESOURCE_BASE) ||
            !resources.contains(DELEGATED_RESOURCE)
            ) {
            throw new Exception("DelegationPrivilegeSubResourceTest.test: " +
                "resource names for READ action is inccorect");
        }

        resources = apm.getResources(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME,
            ApplicationPrivilege.Action.DELEGATE);
        if ((resources.size() != 2) ||
            !resources.contains(DELEGATED_RESOURCE_BASE) ||
            !resources.contains(DELEGATED_RESOURCE)
            ) {
            throw new Exception("DelegationPrivilegeSubResourceTest.test: " +
                "resource names for DELEGATE action is inccorect");
        }
    }
}
