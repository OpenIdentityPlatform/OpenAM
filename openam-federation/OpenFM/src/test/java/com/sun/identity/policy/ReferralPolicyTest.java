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
 * $Id: ReferralPolicyTest.java,v 1.2 2009/09/25 05:52:56 veiming Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.policy;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class ReferralPolicyTest {
    private static final String REFERRAL_POLICY_NAME1 =
        "ReferralPolicyTestReferralPolicy1";
    private static final String REFERRAL_POLICY_NAME2 =
        "ReferralPolicyTestReferralPolicy2";
    private static final String POLICY_NAME =
        "ReferralPolicyTestPolicy";
    private static final String SUB_REALM1 = "/ReferralPolicyTestSubRealm1";
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
        AdminTokenAction.getInstance());


    @BeforeClass
    public void setup() throws Exception {
        createOrgs();
        createReferralPolicy1();
        createPolicy();
    }

    private void createPolicy() throws Exception {
        Policy policy = new Policy(POLICY_NAME, "", false);
        PolicyManager pm = new PolicyManager(adminToken, SUB_REALM1);
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AuthenticatedUsers");
        Map<String, Set<String>> actionValues =
            new HashMap<String, Set<String>>();
        {
            Set<String> set = new HashSet<String>();
            set.add("allow");
            actionValues.put("GET", set);
        }
        {
            Set<String> set = new HashSet<String>();
            set.add("allow");
            actionValues.put("POST", set);
        }
        policy.addRule(new Rule("rule", "iPlanetAMWebAgentService",
            "http://www.ReferredResourcesTest.com/1/2", actionValues));
        policy.addSubject("subject", subject);
        pm.addPolicy(policy);
    }

    private void createReferralPolicy1() throws Exception {
        Policy policy = new Policy(REFERRAL_POLICY_NAME1, "", true);
        PolicyManager pm = new PolicyManager(adminToken, "/");

        ReferralTypeManager rm = pm.getReferralTypeManager();
        Referral referral = rm.getReferral("SubOrgReferral");
        Set<String> tmp = new HashSet<String>();
        tmp.add(SUB_REALM1);
        referral.setValues(tmp);
        policy.addReferral("referral", referral);

        Rule rule = new Rule("iPlanetAMWebAgentService",
            Collections.EMPTY_MAP);
        Set<String> set = new HashSet<String>();
        set.add("http://www.ReferredResourcesTest.com/1/*");
        rule.setResourceNames(set);
        policy.addRule(rule);

        pm.addPolicy(policy);
    }


    private void createOrgs() throws Exception {
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM1.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);
    }

    @Test
    private void createReferralPolicyWithoutRule() throws Exception {
        Policy policy = new Policy(REFERRAL_POLICY_NAME2, 
                "",  // description
                true, // referral?
                true // active?
                ); 
        PolicyManager pm = new PolicyManager(adminToken, "/");

        pm.addPolicy(policy);
    }

    @Test(dependsOnMethods={"createReferralPolicyWithoutRule"})
    private void addRuleToReferralPolicy() throws Exception {
        PolicyManager pm = new PolicyManager(adminToken, "/");
        Policy policy = pm.getPolicy(REFERRAL_POLICY_NAME2);

        Rule rule = new Rule("iPlanetAMWebAgentService",
            Collections.EMPTY_MAP);
        Set<String> set = new HashSet<String>();
        set.add("http://www.ReferredResourcesTest.com/1/*");
        rule.setResourceNames(set);
        policy.addRule(rule);

        pm.replacePolicy(policy);
    }

    @AfterClass
    public void cleanup() throws Exception {
        PolicyManager pm = new PolicyManager(adminToken, SUB_REALM1);
        pm.removePolicy(POLICY_NAME);
        pm = new PolicyManager(adminToken, "/");
        pm.removePolicy(REFERRAL_POLICY_NAME1);
        pm.removePolicy(REFERRAL_POLICY_NAME2);

        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM1.substring(1);
        ocm.deleteSubOrganization(subRealm, true);
    }

    @Test
    public void getReferralInRootRealm() throws Exception {
        javax.security.auth.Subject adminSubject = SubjectUtils.createSubject(
            adminToken);
        ReferralPrivilegeManager rfm = new ReferralPrivilegeManager("/",
            adminSubject);
        SearchFilter f = new SearchFilter(
            Privilege.NAME_ATTRIBUTE, "*");
        ReferralPrivilege ref = rfm.findByName(REFERRAL_POLICY_NAME1);
        Map<String, Set<String>> map = ref.getMapApplNameToResources();
        if ((map == null) || map.isEmpty()) {
            throw new Exception("ReferralPolicyTest.getReferralInRootRealm: " +
                "cannot get mapping of application to resource");
        }

        Set<String> res = map.get("iPlanetAMWebAgentService");
        if ((res == null) || res.isEmpty()) {
            throw new Exception("ReferralPolicyTest.getReferralInRootRealm: " +
                "cannot referred resources");
        }

        if (!res.contains("http://www.ReferredResourcesTest.com/1/*")) {
            throw new Exception("ReferralPolicyTest.getReferralInRootRealm: " +
                "referred resources does not match");
        }
    }

}
