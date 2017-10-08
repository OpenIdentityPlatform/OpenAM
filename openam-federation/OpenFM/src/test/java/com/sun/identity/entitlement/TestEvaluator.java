/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TestEvaluator.java,v 1.2 2009/11/12 18:37:40 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
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
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
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

public class TestEvaluator {
    private static final String APPL_NAME = "TestEvaluatorAppl";
    private static final String SUB_REALM = "/TestEvaluator";
    private static final String REFERRAL_NAME = "testEvaluatorReferral";
    private static final String PRIVILEGE1_NAME = "entitlementPrivilege1";
    private static final String USER1_NAME = "privilegeEvalTestUser1";
    private static final String USER2_NAME = "privilegeEvalTestUser2";
    private static final String URL1 = "http://www.testevaluator.com:80/private";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    private AMIdentity user1;
    private AMIdentity user2;
    

    @BeforeClass
    public void setup() throws Exception {
        if (!migrated) {
            return;
        }
      
        Application appl = new Application(APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));

        // Test disabled, unable to fix model change
        // Set<String> avaliableResources = new HashSet<String>();
        // avaliableResources.add("http://www.testevaluator.com:80/*");
        // appl.addResources(avaliableResources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);

        createReferral(adminToken, adminSubject);
    }

    private void createReferral(SSOToken adminToken, Subject adminSubject)
        throws SMSException, EntitlementException, SSOException, IdRepoException,
        InterruptedException {
        OrganizationConfigManager orgMgr = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM.substring(1);
        orgMgr.createSubOrganization(subRealm, Collections.EMPTY_MAP);


        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        map.put(APPL_NAME, set);
        set.add("http://www.testevaluator.com:80/*");

        Set<String> realms = new HashSet<String>();
        realms.add(SUB_REALM);
        
        ReferralPrivilege referral =
            new ReferralPrivilege(REFERRAL_NAME, map, realms);

        PrivilegeManager pm = PrivilegeManager.getInstance(SUB_REALM,
            adminSubject);
        Map<String, Boolean> actions = new HashMap<String, Boolean>();
        actions.put("GET", Boolean.TRUE);
        Entitlement ent = new Entitlement(APPL_NAME, URL1, actions);
        user1 = IdRepoUtils.createUser("/", USER1_NAME);
        user2 = IdRepoUtils.createUser("/", USER2_NAME);
        Set<EntitlementSubject> esSet = new HashSet<EntitlementSubject>();
        EntitlementSubject es1 = new OpenSSOUserSubject(user1.getUniversalId());
        EntitlementSubject es2 = new OpenSSOUserSubject(user2.getUniversalId());
        esSet.add(es1);
        esSet.add(es2);

        EntitlementSubject eSubject = new OrSubject(esSet);
        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(PRIVILEGE1_NAME);
        privilege.setEntitlement(ent);
        privilege.setSubject(eSubject);
        pm.add(privilege);
        Thread.sleep(1000);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        
        PrivilegeManager pm = PrivilegeManager.getInstance(SUB_REALM,
            adminSubject);
        pm.remove(PRIVILEGE1_NAME);

        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(user1);
        identities.add(user2);
        IdRepoUtils.deleteIdentities("/", identities);

        ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);

        OrganizationConfigManager orgMgr = new OrganizationConfigManager(
            adminToken, "/");
        orgMgr.deleteSubOrganization(SUB_REALM, true);
    }

    @Test
    public void postiveTest()
        throws Exception {
        if (!migrated) {
            return;
        }
        Thread.sleep(1000);
        if (!evaluate(URL1)) {
            throw new Exception("TestEvaluator.postiveTest failed");
        }
    }

    private boolean evaluate(String res)
        throws EntitlementException {
        Subject subject = AuthUtils.createSubject(user1.getUniversalId());
        Set actions = new HashSet();
        actions.add("GET");
        Evaluator evaluator = new Evaluator(
            SubjectUtils.createSubject(adminToken),
            APPL_NAME);
        return evaluator.hasEntitlement("/", subject,
            new Entitlement(res, actions), Collections.EMPTY_MAP);
    }
}
