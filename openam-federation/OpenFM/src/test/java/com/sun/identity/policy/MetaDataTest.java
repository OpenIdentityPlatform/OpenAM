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
 * $Id: MetaDataTest.java,v 1.3 2009/10/14 03:18:42 veiming Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */

package com.sun.identity.policy;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class MetaDataTest {
    private final static String TEST_USER_NAME = "MetaDataTestTestUser";
    private final static String POLICY_NAME = "MetaDataTestPolicy";
    private final static String RES_NAME =
        "http://whatever.metadatatest.com:80";
    private AMIdentity testUser;
    private Policy policy;

    @BeforeClass
    public void setup() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        testUser = createUser(adminToken);
        PolicyManager pm = new PolicyManager(adminToken, "/");
        policy = new Policy(POLICY_NAME, "", false, true);
        policy.addRule(createRule());
        policy.addSubject("subjectName", createSubject(pm));
        pm.addPolicy(policy);
        Thread.sleep(1000);
    }

    @AfterClass
    public void cleanup() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        deleteUser(adminToken);
        PolicyManager pm = new PolicyManager(adminToken, "/");
        pm.removePolicy(POLICY_NAME);
    }

    @Test
    public void test() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        PolicyManager pm = new PolicyManager(adminToken, "/");
        Policy testPolicy = pm.getPolicy(POLICY_NAME);
        long creationDate = testPolicy.getCreationDate();
        String createdBy = testPolicy.getCreatedBy();

        testPolicy.addCondition("cond", createIPCondition(pm));
        pm.replacePolicy(testPolicy);
        Thread.sleep(1000);

        long lastModifiedDate = testPolicy.getLastModifiedDate();
        String lastModifiedBy = testPolicy.getLastModifiedBy();

        if (creationDate > lastModifiedDate) {
            throw new Exception("MetaDataTest.test: last modified date is wrong");
        }
        if (!createdBy.equals(lastModifiedBy)) {
            throw new Exception("MetaDataTest.test: last modified user Id is wrong");
        }

        PrivilegeManager privilegMgr = PrivilegeManager.getInstance("/",
            SubjectUtils.createSubject(adminToken));

        test(privilegMgr, SearchFilter.Operator.EQUALS_OPERATOR, creationDate,
            "equals test", false);
        test(privilegMgr, SearchFilter.Operator.GREATER_THAN_OPERATOR,
            creationDate -1, "greater than test", true);
        test(privilegMgr, SearchFilter.Operator.LESS_THAN_OPERATOR,
            creationDate +1, "lesser than test", true);
    }

    private void test(
        PrivilegeManager privilegMgr,
        SearchFilter.Operator operator,
        long value,
        String desc,
        boolean containCheckOnly
    ) throws Exception {
        Set<SearchFilter> filter =
            new HashSet<SearchFilter>();
        filter.add(new SearchFilter(Privilege.CREATION_DATE_SEARCH_ATTRIBUTE, value, operator));
        Set<String> privilegeNames = privilegMgr.searchNames(filter);

        if (!containCheckOnly) {
            if ((privilegeNames == null) || (privilegeNames.size() != 1)) {
                throw new Exception("MetaDataTest.test: (" + desc +
                    ") search privilege names failed");
            }
        }
        if (!privilegeNames.contains(POLICY_NAME)) {
            throw new Exception("MetaDataTest.test: (" + desc +
                ") search privilege names failed, Got the wrong privilege");
        }
    }

    private Rule createRule() throws PolicyException {
        Map<String, Set<String>> actionValues = new
            HashMap<String, Set<String>>();
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
            RES_NAME, actionValues);
    }

    private Subject createSubject(PolicyManager pm) throws PolicyException {
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> set = new HashSet<String>();
        set.add(testUser.getUniversalId());
        subject.setValues(set);
        return subject;
    }

    private Condition createIPCondition(PolicyManager pm)
        throws PolicyException {
        Map<String, Set<String>> ipConditionEnvMap = new
            HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add("whatever.whatever");
        ipConditionEnvMap.put(Condition.DNS_NAME, set);

        ConditionTypeManager mgr = pm.getConditionTypeManager();
        Condition cond = mgr.getCondition("IPCondition");
        cond.setProperties(ipConditionEnvMap);
        return cond;
    }

    private AMIdentity createUser(SSOToken adminToken)
        throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Map<String, Set<String>> attrValues =new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(TEST_USER_NAME);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);
        return amir.createIdentity(IdType.USER, TEST_USER_NAME,
            attrValues);
    }

    private void deleteUser(SSOToken adminToken)
        throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(testUser);
        amir.deleteIdentities(identities);
    }
}
