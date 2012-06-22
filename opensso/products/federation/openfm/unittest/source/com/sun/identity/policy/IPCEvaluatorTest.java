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
 * $Id: IPCEvaluatorTest.java,v 1.1 2009/08/21 05:27:00 dillidorai Exp $
 */
package com.sun.identity.policy;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.internal.server.AuthSPrincipal;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.IPCondition;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;

import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.unittest.UnittestLog;

import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

/**
 *
 * @author dilli
 *
 * Unittest to verify the fix for issue 5440 
 * IP Address condition is broken
 */
public class IPCEvaluatorTest {

    private static String PRIVILEGE_NAME1 = "IPCEvaluatorTestP1";
    private static String URL_RESOURCE1
            = "http://www.sample.com:8080/ipc-evaluation.html";
    private static String TEST_USER_NAME = "policyTestUser";
    private static String APPL_NAME = "iPlanetAMWebAgentService";
    private AMIdentity testUser;
    private AuthContext lc;
    private SSOToken userSSOToken;

    @BeforeClass
    public void setup() throws Exception {
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            Subject adminSubject = SubjectUtils.createSubject(adminToken);
            boolean migrated = EntitlementConfiguration.getInstance(
                    adminSubject, "/").migratedToEntitlementService();
            if (!migrated) {
                UnittestLog.logError("IPCEvaluatorTest.setup():"
                        + " not migrated to entitlement service");
                throw new Exception("IPCEvaluatorTest.setup():"
                        + "not migrated to entitlement service");
            }

            testUser = createUser(adminToken);
            userSSOToken = login();

            PrivilegeManager pm = PrivilegeManager.getInstance("/",
                    adminSubject);

            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME1);

            Map<String, Boolean> actionMap = new HashMap<String, Boolean>();
            actionMap.put("GET", Boolean.TRUE);

            Entitlement ent = new Entitlement(APPL_NAME, URL_RESOURCE1,
                    actionMap);
            privilege.setEntitlement(ent);

            EntitlementSubject es = new OpenSSOUserSubject(
                    testUser.getUniversalId());
            privilege.setSubject(es);

            EntitlementCondition ec = new IPCondition(
                    "100.100.100.100", "200.200.200.200");
            privilege.setCondition(ec);

            pm.addPrivilege(privilege);
        } catch (Exception e) {
            UnittestLog.logError("IPCEvaluatorTest.setup():hit exception");
            UnittestLog.logError("Exception STACKTRACE, message:"
                    + e.getMessage());
            UnittestLog.logError("Exception Class:" + e.getClass().getName());
            StackTraceElement[] elems = e.getStackTrace();
            for (StackTraceElement elem : elems) {
                UnittestLog.logMessage(elem.toString());
            }
            UnittestLog.logMessage("END STACKTRACE");
            throw e;
        }
    }

    @AfterClass
    public void cleanup() throws PolicyException, SSOException, 
            IdRepoException,
            EntitlementException {
        try {
            lc.logout();
        } catch (Exception e) {
            //ignore
        }
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());

        Subject adminSubject = SubjectUtils.createSubject(adminToken);
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
                adminSubject);
        pm.removePrivilege(PRIVILEGE_NAME1);

        AMIdentityRepository amir = new AMIdentityRepository(
                adminToken, "/");
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(testUser);
        amir.deleteIdentities(identities);
    }

    @Test
    public void testAllowedWithStringIp() throws Exception {
        PolicyEvaluator pe = new PolicyEvaluator("iPlanetAMWebAgentService");
        Map env = new HashMap();
        env.put("requestIp", "120.120.120.120");
        if (!pe.isAllowed(userSSOToken, URL_RESOURCE1, "GET", env)) {
            throw new Exception("testIsAllowedWithStringIp:" +
                    URL_RESOURCE1 + ", failed");
        }
    }

    @Test
    public void testAllowedWithSetIp() throws Exception {
        PolicyEvaluator pe = new PolicyEvaluator("iPlanetAMWebAgentService");
        Map env = new HashMap();
        Set requestIpSet = new HashSet();
        requestIpSet.add("120.120.120.120");
        env.put("requestIp", requestIpSet);
        if (!pe.isAllowed(userSSOToken, URL_RESOURCE1, "GET", env)) {
            throw new Exception("testIsAllowedWithSetIp:" +
                    URL_RESOURCE1 + ", failed");
        }
    }

    private AMIdentity createUser(SSOToken adminToken)
            throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
                adminToken, "/");
        Map<String, Set<String>> attrValues = new HashMap<String,
                Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(TEST_USER_NAME);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);

        testUser = amir.createIdentity(IdType.USER, TEST_USER_NAME,
                attrValues);
        return testUser;
    }

    private SSOToken login()
            throws Exception {
        lc = new AuthContext("/");
        AuthContext.IndexType indexType
                = AuthContext.IndexType.MODULE_INSTANCE;
        lc.login(indexType, "DataStore");
        Callback[] callbacks = null;

        // get information requested from module
        while (lc.hasMoreRequirements()) {
            callbacks = lc.getRequirements();
            if (callbacks != null) {
                addLoginCallbackMessage(callbacks);
                lc.submitRequirements(callbacks);
            }
        }

        return (lc.getStatus() == AuthContext.Status.SUCCESS) 
                ? lc.getSSOToken() : null;
    }

    private void addLoginCallbackMessage(Callback[] callbacks) {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                ((NameCallback) callbacks[i]).setName(TEST_USER_NAME);
            } else if (callbacks[i] instanceof PasswordCallback) {
                ((PasswordCallback) callbacks[i]).setPassword(
                        TEST_USER_NAME.toCharArray());
            }
        }
    }
}
