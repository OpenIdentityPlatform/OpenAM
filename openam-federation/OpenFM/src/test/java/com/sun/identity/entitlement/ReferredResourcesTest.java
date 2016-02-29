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
 * $Id: ReferredResourcesTest.java,v 1.2 2009/11/05 21:13:46 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOIndexStore;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.PolicyManager;
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


public class ReferredResourcesTest {
    private static final String APPL_NAME = "ReferredResourcesTestAppl";
    private static final String REFERRAL_NAME1 = "ReferredResourcesTestR1";
    private static final String REFERRAL_NAME2 = "ReferredResourcesTestR2";
    private static final String SUB_REALM1 = "/ReferredResourcesTest1";
    private static final String SUB_REALM2 = "/ReferredResourcesTest2";
    private static final String SUB_REALM3 = "/ReferredResourcesTest3";
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
        
        createOrgs();
        createAppl(adminSubject);
        createReferral1(adminSubject);
        createReferral2(adminSubject);
    }

    private void createOrgs() throws Exception {
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM1.substring(1);

        Map<String, Map<String, Set<String>>> map = new
            HashMap<String, Map<String, Set<String>>>();
        Map<String, Set<String>> idRepoService = new
            HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add("a.com");
        idRepoService.put(PolicyManager.ORG_ALIAS, set);
        map.put(PolicyManager.ID_REPO_SERVICE, idRepoService);

        ocm.createSubOrganization(subRealm, map);
        subRealm = SUB_REALM2.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);
        subRealm = SUB_REALM3.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);
    }
    
    private void createAppl(Subject adminSubject) throws EntitlementException,
        InstantiationException, IllegalAccessException {
        Application appl = new Application(APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));

        // Test disabled, unable to fix model change
        // Set<String> appResources = new HashSet<String>();
        // appResources.add("http://www.ReferredResourcesTest.com/*");
        // appl.addResources(appResources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);
    }

    private void createReferral1(Subject adminSubject)
        throws EntitlementException {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add("http://www.ReferredResourcesTest.com/1/");
        map.put(APPL_NAME, set);
        Set<String> realms = new HashSet<String>();
        realms.add(SUB_REALM1);
        realms.add(SUB_REALM2);
        ReferralPrivilege r1 = new ReferralPrivilege(REFERRAL_NAME1,
            map, realms);
    }

    private void createReferral2(Subject adminSubject)
        throws EntitlementException {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add("http://www.ReferredResourcesTest.com/1/2");
        map.put(APPL_NAME, set);
        Set<String> realms = new HashSet<String>();
        realms.add(SUB_REALM1);
        ReferralPrivilege r1 = new ReferralPrivilege(REFERRAL_NAME2, map,
            realms);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM1.substring(1);
        ocm.deleteSubOrganization(subRealm, true);
        subRealm = SUB_REALM2.substring(1);
        ocm.deleteSubOrganization(subRealm, true);
        subRealm = SUB_REALM3.substring(1);
        ocm.deleteSubOrganization(subRealm, true);
    }

    @Test
    public void test() throws EntitlementException, Exception {
        if (!migrated) {
            return;
        }
        Set<String> resources = ApplicationServiceTestHelper.getReferredResources(
                PrivilegeManager.superAdminSubject, SUB_REALM1, ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        if (OpenSSOIndexStore.isOrgAliasMappingResourceEnabled(adminToken)) {
            if (resources.size() != 4) {
                throw new Exception(
                    "ReferredResourcesTest.test: failed incorrect number of" +
                    "resources");
            }
        } else {
            if (resources.size() != 2) {
                throw new Exception(
                    "ReferredResourcesTest.test: failed incorrect number of" +
                    "resources");
            }
        }

        if (!resources.contains("http://www.ReferredResourcesTest.com/1/")) {
            throw new Exception("ReferredResourcesTest.test: referred from parent realm failed");
        }
        if (!resources.contains("http://www.ReferredResourcesTest.com/1/2")) {
            throw new Exception("ReferredResourcesTest.test: referred from peer realm failed");
        }

    }
}
