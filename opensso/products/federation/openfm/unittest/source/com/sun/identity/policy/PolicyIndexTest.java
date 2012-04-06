/*
 * 
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
 * $Id: PolicyIndexTest.java,v 1.1 2009/08/19 05:41:02 veiming Exp $
 */
package com.sun.identity.policy;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test the policy indexing. store and retrieving of policy object.
 * @author dennis
 */
public class PolicyIndexTest {

    private static final String URL_RESOURCE = "http://www.sun.com:8080/private";
    private static final String POLICY_NAME = "policyIndexTest";

    @BeforeClass
    public void setup()
            throws SSOException, PolicyException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        PolicyManager pm = new PolicyManager(adminToken, "/");
        Policy policy = new Policy(POLICY_NAME, "test - discard",
                false, true);
        policy.addRule(createRule());
        policy.addSubject("group", createSubject(pm));
        pm.addPolicy(policy);
    }

    @AfterClass
    public void cleanup()
            throws SSOException, PolicyException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        PolicyManager pm = new PolicyManager(adminToken, "/");
        pm.removePolicy(POLICY_NAME);
    }

    @Test
    public void storeAndRetrieve()
            throws SSOException, PolicyException, EntitlementException, Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        PolicyManager pm = new PolicyManager(adminToken, "/");

        Set<String> hostIndexes = new HashSet<String>();
        Set<String> pathIndexes = new HashSet<String>();
        Set<String> parentPathIndexes = new HashSet<String>();
        hostIndexes.add("http://www.sun.com");
        pathIndexes.add("/private");
        parentPathIndexes.add("/");
        ResourceSearchIndexes indexes = new ResourceSearchIndexes(
                hostIndexes, pathIndexes, parentPathIndexes);
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
            SubjectUtils.createSubject(adminToken), "/");
        for (Iterator<IPrivilege> i = pis.search("/", indexes,
            Collections.EMPTY_SET, false); i.hasNext();
        ) {
            IPrivilege eval = i.next();
            if (!(eval instanceof Privilege)) {
                throw new Exception(
                    "incorrect deserialized policy, wrong type");
            }
            Privilege p = (Privilege)eval;
            if (!p.getEntitlement().getResourceName().equals(URL_RESOURCE)) {
                throw new Exception("incorrect deserialized policy");
            }
        }
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
            set.add("allow");
            actionValues.put("POST", set);
        }

        return new Rule("rule1", "iPlanetAMWebAgentService",
                URL_RESOURCE, actionValues);
    }

    private Subject createSubject(PolicyManager pm) throws PolicyException {
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> set = new HashSet<String>();
        set.add("testgroup");
        subject.setValues(set);
        return subject;
    }
}
