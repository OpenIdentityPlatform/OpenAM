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
 * $Id: OrgAliasReferralTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOIndexStore;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class OrgAliasReferralTest {
    private static final String SUB_REALM1 = "/OrgAliasReferralTestSub1";
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean orgAliasEnabled =
        OpenSSOIndexStore.isOrgAliasMappingResourceEnabled(adminToken);

    @BeforeClass
    public void setup() throws Exception {
        if (orgAliasEnabled) {
            createOrgs();
            createPrivilege();
        }
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (orgAliasEnabled) {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminToken, "/");
            ocm.deleteSubOrganization(SUB_REALM1.substring(1), true);
        }
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
        set.add("www.OrgAliasReferralTest.com");
        idRepoService.put(PolicyManager.ORG_ALIAS, set);
        map.put(PolicyManager.ID_REPO_SERVICE, idRepoService);

        ocm.createSubOrganization(subRealm, map);
    }

    private void createPrivilege()
        throws EntitlementException {
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", true);
        Entitlement e1 = new Entitlement(
            "http://www.OrgAliasReferralTest.com:80/*.*",
            actionValues);
        EntitlementSubject sbj = new AuthenticatedUsers();

        Privilege p1 = Privilege.getNewInstance();
        p1.setName("OrgAliasReferralTest");
        p1.setEntitlement(e1);
        p1.setSubject(sbj);
        PrivilegeManager mgr = PrivilegeManager.getInstance(SUB_REALM1,
            adminSubject);
        mgr.add(p1);
    }

    @Test
    public void test() throws Exception {
        if (orgAliasEnabled) {
            Evaluator eval = new Evaluator(adminSubject);
            Set<String> actions = new HashSet<String>();
            actions.add("GET");
            Entitlement e = new Entitlement(
                "http://www.OrgAliasReferralTest.com/test.html", actions);
            if (!eval.hasEntitlement("/", adminSubject, e,
                Collections.EMPTY_MAP)) {
                cleanup();
                throw new Exception("OrgAliasReferralTest.test failed.");
            }
        }
    }
}
