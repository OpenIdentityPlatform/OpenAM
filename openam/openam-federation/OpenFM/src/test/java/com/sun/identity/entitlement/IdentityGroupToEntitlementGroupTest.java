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
 * $Id: IdentityGroupToEntitlementGroupTest.java,v 1.2 2009/11/12 18:37:39 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.PrivilegeUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.SubjectTypeManager;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class IdentityGroupToEntitlementGroupTest {

    private static final String GROUP_NAME1 =
        "IdentityGroupToEntitlementGroupTestGroup1";
    private static final String GROUP_NAME2 =
        "IdentityGroupToEntitlementGroupTestGroup2";
    private AMIdentity group1;
    private AMIdentity group2;

    @BeforeClass
    public void setup() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        group1 = amir.createIdentity(IdType.GROUP, GROUP_NAME1,
            Collections.EMPTY_MAP);
        group2 = amir.createIdentity(IdType.GROUP, GROUP_NAME2,
            Collections.EMPTY_MAP);
    }

    @AfterClass
    public void cleanup() throws Exception {
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(group1);
        identities.add(group2);
        IdRepoUtils.deleteIdentities("/", identities);
    }

    @Test
    public void test() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        PolicyManager pm = new PolicyManager(adminToken, "/");
        Policy policy = new Policy("IdentityGroupToEntitlementGroupTest",
            "desc", false);
        policy.addRule(createRule());
        policy.addSubject("subject", createSubject(pm));

        Set<IPrivilege> privileges =
            PrivilegeUtils.policyObjectToPrivileges(policy);
        if ((privileges == null) || privileges.isEmpty()) {
            throw new Exception(
                "IdentityGroupToEntitlementGroupTest, set is empty");
        }

        Privilege p = (Privilege)privileges.iterator().next();
        //uncomment after the groupsubject mapping is done
/*        EntitlementSubject subject = p.getSubject();
        if (!(subject instanceof OrSubject)) {
            throw new Exception(
                "IdentityGroupToEntitlementGroupTest, orSubject not found");
        }

        OrSubject orSubject = (OrSubject)subject;
        Set<EntitlementSubject> subjects = orSubject.getESubjects();
        if ((subjects == null) || (subjects.size() != 2)) {
            throw new Exception(
                "IdentityGroupToEntitlementGroupTest, subjects collection is incorrect");
        }

        for (EntitlementSubject s : subjects) {
            if (!(s instanceof GroupSubject)) {
                throw new Exception(
                    "IdentityGroupToEntitlementGroupTest, no group subject");
            }
        }*/
    }

    private Rule createRule() throws PolicyException {
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
            "dummy", actionValues);
    }

    private Subject createSubject(PolicyManager pm) throws PolicyException {
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> set = new HashSet<String>();
        set.add(group1.getUniversalId());
        set.add(group2.getUniversalId());
        subject.setValues(set);
        return subject;
    }

}
