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
 * $Id: DelegationPrivilegeIdRepoAccessTest.java,v 1.3 2009/12/18 21:56:56 veiming Exp $
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
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;

import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

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
public class DelegationPrivilegeIdRepoAccessTest {
    private static final String SUB_REALM =
        "/DelegationPrivilegeIdRepoAccessTestDelegateRealm";
    private static final String SUB_SUB_REALM = "sub";
    private static final String APPLICATION_NAME =
        "DelegationPrivilegeIdRepoAccessTestDelegateApp";
    private static final String REFERRAL_NAME =
        "DelegationPrivilegeIdRepoAccessTestDelegateReferral";
    private static final String DELEGATE_PRIVILEGE_NAME =
        "DelegationPrivilegeIdRepoAccessTestDelegatePrivilege";
    private static final String DELEGATED_RESOURCE =
            "http://www.delegationprivilegeidrepoaccesstest.com/*";
    private static final String DELEGATED_USER =
        "DelegationPrivilegeIdRepoAccessTestDelegatedUser";
    private static final String DELEGATED_USER1 =
        "DelegationPrivilegeIdRepoAccessTestDelegatedUser1";

    protected SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private AMIdentity delegatedUser;
    private AMIdentity delegatedUser1;

    public DelegationPrivilegeIdRepoAccessTest() {
    }

    @BeforeTest
    public void setup() throws Exception {
        OrganizationConfigManager orgMgr = new OrganizationConfigManager(
            adminToken, "/");
        orgMgr.createSubOrganization(SUB_REALM.substring(1),
            Collections.EMPTY_MAP);
        delegatedUser = IdRepoUtils.createUser(SUB_REALM, DELEGATED_USER);
        delegatedUser1 = IdRepoUtils.createUser(SUB_REALM, DELEGATED_USER1);

        orgMgr = new OrganizationConfigManager(
            adminToken, SUB_REALM);
        orgMgr.createSubOrganization(SUB_SUB_REALM, Collections.EMPTY_MAP);

        Application appl = EntitlementUtils.newApplication(APPLICATION_NAME, ApplicationTypeManager.getAppplicationType(
                PrivilegeManager.superAdminSubject, ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));
        // Test disabled, unable to make model change.
        // Set<String> resources = new HashSet<String>();
        // resources.add(DELEGATED_RESOURCE);
        // appl.setResources(resources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationServiceTestHelper.saveApplication(
                SubjectUtils.createSuperAdminSubject(), SUB_REALM, appl);
    }

    @AfterTest
    public void cleanup() throws Exception {
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(delegatedUser);
        identities.add(delegatedUser1);
        IdRepoUtils.deleteIdentities(SUB_REALM, identities);

        ApplicationServiceTestHelper.deleteApplication(
                SubjectUtils.createSuperAdminSubject(), SUB_REALM, APPLICATION_NAME);

        OrganizationConfigManager orgMgr = new OrganizationConfigManager(
            adminToken, "/");
        orgMgr.deleteSubOrganization(SUB_REALM, true);
    }

    @Test
    public void test() throws Exception {
        try {
            addPrivilege();
            SSOToken token = AuthUtils.authenticate(
                SUB_REALM, DELEGATED_USER, DELEGATED_USER);
            testIdRepoAccess(token);
            addUserToPrivilege();
            testIdRepoAccess(token);

            SSOToken token1 = AuthUtils.authenticate(
                SUB_REALM, DELEGATED_USER1, DELEGATED_USER1);
            testIdRepoAccess(token1);
        } catch (EntitlementException e) {
            throw e;
        } finally {
            removePrivilege();
        }
    }

    private void testIdRepoAccess(SSOToken token) throws Exception {
        try {
            AMIdentityRepository idrepo = new AMIdentityRepository(token, "/");
            IdSearchResults result = idrepo.searchIdentities(
                IdType.USER, "*", new IdSearchControl());
            result.getSearchResults();
        } catch (IdRepoException e) {
            // permission denied
            }

        // ok to search current realm
        AMIdentityRepository idrepo = new AMIdentityRepository(token,
            SUB_REALM);
        IdSearchResults result = idrepo.searchIdentities(
            IdType.USER, "*", new IdSearchControl());
        result.getSearchResults();

        // ok to search sub realm
        idrepo = new AMIdentityRepository(token,
            SUB_REALM + "/" + SUB_SUB_REALM);
        result = idrepo.searchIdentities(IdType.USER, "*",
            new IdSearchControl());
        result.getSearchResults();
    }

    private void removePrivilege() throws Exception {
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(SUB_REALM,
            PrivilegeManager.superAdminSubject);
        mgr.removePrivilege(DELEGATE_PRIVILEGE_NAME);
    }

    private void addUserToPrivilege() throws EntitlementException  {
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(SUB_REALM,
            PrivilegeManager.superAdminSubject);
        ApplicationPrivilege ap = mgr.getPrivilege(DELEGATE_PRIVILEGE_NAME);
        Set<SubjectImplementation> eSubjects = new
            HashSet<SubjectImplementation>();
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID(delegatedUser.getUniversalId());
        eSubjects.add(sbj);
        OpenSSOUserSubject sbj1 = new OpenSSOUserSubject();
        sbj1.setID(delegatedUser1.getUniversalId());
        eSubjects.add(sbj1);
        ap.setSubject(eSubjects);
        mgr.replacePrivilege(ap);
    }

    private void addPrivilege() throws EntitlementException  {
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(SUB_REALM,
            PrivilegeManager.superAdminSubject);
        ApplicationPrivilege ap = new ApplicationPrivilege(
            DELEGATE_PRIVILEGE_NAME);
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID(delegatedUser.getUniversalId());
        Set<SubjectImplementation> subjects = new
            HashSet<SubjectImplementation>();
        subjects.add(sbj);
        ap.setSubject(subjects);

        Map<String, Set<String>> appRes = new HashMap<String, Set<String>>();
        Set<String> res = new HashSet<String>();
        appRes.put(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, res);
        res.add(DELEGATED_RESOURCE);
        ap.setApplicationResources(appRes);
        ap.setActionValues(
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE);
        mgr.addPrivilege(ap);
    }

}