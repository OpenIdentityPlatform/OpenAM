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
 * $Id: DelegationPrivilegeTest.java,v 1.5 2009/12/17 18:03:51 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import org.testng.annotations.Test;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.AuthUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

/**
 *
 * @author dennis
 */
public class DelegationPrivilegeTest {
    protected SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private AMIdentity delegatedUser;
    private AMIdentity nonDelegatedUser;
    protected String realm;
    protected Map<String, String> testParams;

    public DelegationPrivilegeTest() {
        init();
    }

    protected void init() {
        realm = "/";
        testParams = new HashMap<String, String>();
        testParams.put("DELEGATE_PRIVILEGE_NAME",
            "DelegationPrivilegeTestDelegatePrivilege");
        testParams.put("DELEGATED_RESOURCE",
            "http://www.delegationprivilegetest.com/*");
        testParams.put("DELEGATED_SUB_RESOURCE",
            "http://www.delegationprivilegetest.com/sub/*");
        testParams.put("DELEGATED_USER",
            "DelegationPrivilegeTestDelegatedUser");
        testParams.put("NON_DELEGATED_USER",
            "DelegationPrivilegeTestNonDelegatedUser");
    }

    @BeforeTest
    public void setup() throws Exception {
        delegatedUser = createUser(testParams.get("DELEGATED_USER"));
        nonDelegatedUser = createUser(testParams.get("NON_DELEGATED_USER"));
    }

    @AfterTest
    public void cleanup() throws Exception {
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(nonDelegatedUser);
        identities.add(delegatedUser);
        IdRepoUtils.deleteIdentities(realm, identities);
    }

    @Test
    public void testAdd() throws Exception {
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(adminToken));
        ApplicationPrivilege ap = new ApplicationPrivilege(
            testParams.get("DELEGATE_PRIVILEGE_NAME"));
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID(delegatedUser.getUniversalId());
        Set<SubjectImplementation> subjects = new
            HashSet<SubjectImplementation>();
        subjects.add(sbj);
        ap.setSubject(subjects);

        String delResource = testParams.get("DELEGATED_RESOURCE");
        Map<String, Set<String>> appRes = new HashMap<String, Set<String>>();
        Set<String> res = new HashSet<String>();
        appRes.put(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, res);
        res.add(delResource);
        ap.setApplicationResources(appRes);
        ap.setActionValues(
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE);
        mgr.addPrivilege(ap);

        Application app = ApplicationServiceTestHelper.getApplication(
                PrivilegeManager.superAdminSubject, realm, ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);

        // Test disabled, unable to fix model change.
        // if (app.getResources().contains(delResource)) {
        //     throw new Exception("DelegationPrivilegeTest.testAdd:" +
        //         "application resources should not have delegated resource");
        // }
    }

    @Test (dependsOnMethods = {"testAdd"})
    public void testModify() throws Exception {
        SSOToken userSSOToken = AuthUtils.authenticate("/",
            testParams.get("DELEGATED_USER"),
            testParams.get("DELEGATED_USER"));
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(userSSOToken));

        Set<SearchFilter> filters = new HashSet<SearchFilter>();
        String privilegeName = testParams.get("DELEGATE_PRIVILEGE_NAME");
        filters.add(new SearchFilter(Privilege.NAME_SEARCH_ATTRIBUTE, privilegeName));

        Set<String> names = mgr.search(filters);
        if ((names == null) || names.isEmpty()) {
            throw new Exception(
                "DelegationPrivilegeTest.testModify: search failed");
        }
        ApplicationPrivilege ap = mgr.getPrivilege(privilegeName);
        Set<String> appRes = ap.getResourceNames(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        appRes.add(testParams.get("DELEGATED_SUB_RESOURCE"));
        mgr.replacePrivilege(ap);
    }

    @Test (dependsOnMethods = {"testModify"})
    public void testModifyNegative() throws Exception {
        SSOToken userSSOToken = AuthUtils.authenticate("/",
            testParams.get("NON_DELEGATED_USER"),
            testParams.get("NON_DELEGATED_USER"));
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(userSSOToken));

        Set<SearchFilter> filters = new HashSet<SearchFilter>();
        String privilegeName = testParams.get("DELEGATE_PRIVILEGE_NAME");
        filters.add(new SearchFilter(Privilege.NAME_SEARCH_ATTRIBUTE, privilegeName));
        Set<String> privilegeNames = mgr.search(filters);

        if ((privilegeNames != null) && !privilegeNames.isEmpty()) {
            throw new Exception("DelegationPrivilegeTest.testModifyNegative" +
                "privilegeNames should be empty");
        }

        try {
            ApplicationPrivilege ap = mgr.getPrivilege(
                testParams.get("DELEGATE_PRIVILEGE_NAME"));
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 325) {
                throw e;
            }
        }
    }

    @Test (dependsOnMethods = {"testModifyNegative"})
    public void testRemove() throws Exception {
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(realm,
            SubjectUtils.createSubject(adminToken));
        mgr.removePrivilege(
            testParams.get("DELEGATE_PRIVILEGE_NAME"));
    }

    private AMIdentity createUser(String name)
        throws SSOException, IdRepoException {
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, realm);
        Map<String, Set<String>> attrValues =new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(name);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);
        return amir.createIdentity(IdType.USER, name, attrValues);
    }
}
