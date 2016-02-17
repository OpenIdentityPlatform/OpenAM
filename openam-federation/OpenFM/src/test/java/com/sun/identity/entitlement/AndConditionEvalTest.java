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
 * $Id: AndConditionEvalTest.java,v 1.2 2010/01/12 21:29:58 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_IP;

public class AndConditionEvalTest {
    private static final String PRIVILEGE_NAME = "AndConditionEvalTest";
    private static final String ROOT_RESOURCE_NAME = 
            "http://www.AndConditionEvalTest.com";
    private static final String START_IP =  "100.100.100.100";
    private static final String END_IP =  "200.200.200.200";
    private static final String DNS_MASK =  "*.AndConditionEvalTest.com";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    @BeforeClass
    public void setup() throws Exception {
        if (migrated) {
            Map<String, Boolean> actions = new HashMap<String, Boolean>();
            actions.put("GET", Boolean.TRUE);
            Entitlement ent = new Entitlement(
                    ApplicationTypeManager.URL_APPLICATION_TYPE_NAME,
                    ROOT_RESOURCE_NAME + "/*", actions);
            AndCondition cond = new AndCondition();
            Set<EntitlementCondition> conditions = new 
                    HashSet<EntitlementCondition>();
            IPv4Condition ipc = new IPv4Condition();
            ipc.setStartIpAndEndIp(START_IP, END_IP);
            conditions.add(ipc);
            cond.setEConditions(conditions);

            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME);
            privilege.setEntitlement(ent);
            privilege.setSubject(new AnyUserSubject());
            privilege.setCondition(cond);

            PrivilegeManager pm = PrivilegeManager.getInstance("/",
                    adminSubject);
            pm.add(privilege);
            Thread.sleep(1000);
        }
    }

    @AfterClass
    public void clean() throws EntitlementException {
        if (migrated) {
            PrivilegeManager pm = PrivilegeManager.getInstance("/",
                    adminSubject);
            pm.remove(PRIVILEGE_NAME);
        }
    }

    @Test
    public void positiveTest() throws Exception {
        Map<String, Set<String>> environment = getEnvironment(
                "100.100.100.200", "www.AndConditionEvalTest.com");

        Evaluator evaluator = new Evaluator(adminSubject,
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        List<Entitlement> entitlements = evaluator.evaluate("/", adminSubject,
                ROOT_RESOURCE_NAME + "/index.html", environment, false);

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception(
                    "AndConditionEvalTest.positiveTest: no entitlements returned");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");

        if ((result == null) || !result) {
            throw new Exception(
                    "AndConditionEvalTest.positiveTest: incorrect decision");
        }

        Map<String, Set<String>> advice = ent.getAdvices();
        if ((advice != null) && !advice.isEmpty()) {
            throw new Exception(
                    "AndConditionEvalTest.positiveTest: do not expect advices.");
        }
    }
    
    @Test
    public void negativeTest() throws Exception {
        Map<String, Set<String>> environment = getEnvironment(
                "210.100.100.200", "www.wrong.com");

        Evaluator evaluator = new Evaluator(adminSubject,
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        List<Entitlement> entitlements = evaluator.evaluate("/", adminSubject,
                ROOT_RESOURCE_NAME + "/index.html", environment, false);

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception(
                    "AndConditionEvalTest.negativeTest: no entitlements returned");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");

        if ((result != null) && result) {
            throw new Exception(
                    "AndConditionEvalTest.negativeTest: incorrect decision");
        }

        Map<String, Set<String>> advices = ent.getAdvices();
        if ((advices == null) || advices.isEmpty()) {
            throw new Exception(
                    "AndConditionEvalTest.negativeTest: no advices.");
        }

        Set<String> advice = advices.get(IPv4Condition.class.getName());
        if ((advice == null) || advice.isEmpty()) {
            throw new Exception(
                    "AndConditionEvalTest.negativeTest: no advice for IPCondition.");
        }

        String adv = advice.iterator().next();
        if (!adv.equals(REQUEST_IP + "=" + START_IP + "-" + END_IP)
        ) {
            throw new Exception(
                "AndConditionEvalTest.negativeTest: advice for IP Condition");
        }

    }

    private Map<String, Set<String>> getEnvironment(String ipAddr, String dns) {
        Map<String, Set<String>> environment =
                new HashMap<String, Set<String>>();
        Set<String> dnsMask = new HashSet<String>();
        dnsMask.add(dns);
        environment.put("requestDnsName", dnsMask);

        Set<String> ip = new HashSet<String>();
        ip.add(ipAddr);
        environment.put(REQUEST_IP, ip);

        return environment;
    }
}
