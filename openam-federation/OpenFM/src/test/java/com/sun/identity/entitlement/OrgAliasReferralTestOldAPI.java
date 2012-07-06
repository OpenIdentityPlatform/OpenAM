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
 * $Id: OrgAliasReferralTestOldAPI.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOIndexStore;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.SubjectTypeManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
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

public class OrgAliasReferralTestOldAPI {
    private static final String SUB_REALM1 = "/OrgAliasReferralTestOldAPISub1";
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private boolean orgAliasEnabled =
        OpenSSOIndexStore.isOrgAliasMappingResourceEnabled(adminToken);
    private static final String URL_RESOURCE1 =
        "http://www.OrgAliasReferralTestOldAPI.com:80/*.*";

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
        set.add("www.OrgAliasReferralTestOldAPI.com");
        idRepoService.put(PolicyManager.ORG_ALIAS, set);
        map.put(PolicyManager.ID_REPO_SERVICE, idRepoService);

        ocm.createSubOrganization(subRealm, map);
    }

    private void createPrivilege()
        throws Exception {
        Policy policy = new Policy("OrgAliasReferralTestOldAPI", "", false);
        PolicyManager pm = new PolicyManager(adminToken, SUB_REALM1);
        SubjectTypeManager sm = pm.getSubjectTypeManager();
        policy.addSubject("s", sm.getSubject("AuthenticatedUsers"));
        policy.addRule(createRule1());
        pm.addPolicy(policy);
    }

    private Rule createRule1() throws PolicyException {
        Map<String, Set<String>> actionValues =
            new HashMap<String, Set<String>>();
        {
            Set<String> set = new HashSet<String>();
            set.add("allow");
            actionValues.put("GET", set);
        }
        {
            Set<String> set = new HashSet<String>();
            set.add("deny");
            actionValues.put("POST", set);
        }

        return new Rule("rule1", "iPlanetAMWebAgentService",
            URL_RESOURCE1, actionValues);
    }

    @Test
    public void test() throws Exception {
        if (orgAliasEnabled) {
            Subject adminSubject = SubjectUtils.createSubject(adminToken);
            Evaluator eval = new Evaluator(adminSubject);
            Set<String> actions = new HashSet<String>();
            actions.add("GET");
            Entitlement e = new Entitlement(
                "http://www.OrgAliasReferralTestOldAPI.com/test.html", actions);
            if (!eval.hasEntitlement("/",
                adminSubject, e,
                Collections.EMPTY_MAP)) {
                cleanup();
                throw new Exception("OrgAliasReferralTestOldAPI.test failed.");
            }
        }
    }
}
