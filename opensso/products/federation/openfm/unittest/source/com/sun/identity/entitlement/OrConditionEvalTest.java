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
 * $Id: OrConditionEvalTest.java,v 1.1 2009/09/05 00:24:03 veiming Exp $
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class OrConditionEvalTest {
    private static final String PRIVILEGE_NAME = "OrConditionEvalTest";
    private static final String ROOT_RESOURCE_NAME = 
            "http://www.OrConditionEvalTest.com";
    private static final String START_IP =  "100.100.100.100";
    private static final String END_IP =  "200.200.200.200";
    private static final String DNS_MASK =  "*.OrConditionEvalTest.com";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = EntitlementConfiguration.getInstance(
        adminSubject, "/").migratedToEntitlementService();

    @BeforeClass
    public void setup() throws Exception {
        if (migrated) {
            Map<String, Boolean> actions = new HashMap<String, Boolean>();
            actions.put("GET", Boolean.TRUE);
            Entitlement ent = new Entitlement(
                    ApplicationTypeManager.URL_APPLICATION_TYPE_NAME,
                    ROOT_RESOURCE_NAME + "/*", actions);
            OrCondition cond = new OrCondition();
            Set<EntitlementCondition> conditions = new 
                    HashSet<EntitlementCondition>();
            conditions.add(new DNSNameCondition(DNS_MASK));
            conditions.add(new IPCondition(START_IP, END_IP));
            cond.setEConditions(conditions);

            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME);
            privilege.setEntitlement(ent);
            privilege.setSubject(new AnyUserSubject());
            privilege.setCondition(cond);

            PrivilegeManager pm = PrivilegeManager.getInstance("/",
                    adminSubject);
            pm.addPrivilege(privilege);
            Thread.sleep(1000);
        }
    }

    @AfterClass
    public void clean() throws EntitlementException {
        if (migrated) {
            PrivilegeManager pm = PrivilegeManager.getInstance("/",
                    adminSubject);
            pm.removePrivilege(PRIVILEGE_NAME);
        }
    }

    @Test
    public void positiveTest() throws Exception {
        Map<String, Set<String>> environment = getEnvironment(
                "100.100.100.200", "www.OrConditionEvalTest.com");

        Evaluator evaluator = new Evaluator(adminSubject,
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        List<Entitlement> entitlements = evaluator.evaluate("/", adminSubject,
                ROOT_RESOURCE_NAME + "/index.html", environment, false);

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception(
                    "OrConditionEvalTest.positiveTest: no entitlements returned");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");

        if ((result == null) || !result) {
            throw new Exception(
                    "OrConditionEvalTest.positiveTest: incorrect decision");
        }

        Map<String, Set<String>> advice = ent.getAdvices();
        if ((advice != null) && !advice.isEmpty()) {
            throw new Exception(
                    "OrConditionEvalTest.positiveTest: do not expect advices.");
        }
    }
    
    @Test
    public void negativeTest() throws Exception {
        Map<String, Set<String>> environment = getEnvironment(
                "210.100.100.200", "www.OrConditionEvalTest1.com");

        Evaluator evaluator = new Evaluator(adminSubject,
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        List<Entitlement> entitlements = evaluator.evaluate("/", adminSubject,
                ROOT_RESOURCE_NAME + "/index.html", environment, false);

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception(
                    "OrConditionEvalTest.negativeTest: no entitlements returned");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");

        if ((result != null) && result) {
            throw new Exception(
                    "OrConditionEvalTest.negativeTest: incorrect decision");
        }

        Map<String, Set<String>> advices = ent.getAdvices();
        if ((advices == null) || advices.isEmpty()) {
            throw new Exception(
                    "OrConditionEvalTest.negativeTest: no advices.");
        }

        Set<String> ipAdvice = advices.get(IPCondition.class.getName());
        if ((ipAdvice == null) || ipAdvice.isEmpty()) {
            throw new Exception(
                    "OrConditionEvalTest.negativeTest: no advice for IPCondition.");
        }

        String adv = ipAdvice.iterator().next();
        if (!adv.equals(IPCondition.REQUEST_IP + "=" + START_IP + "-" + END_IP)
        ) {
            throw new Exception(
                    "OrConditionEvalTest.negativeTest: incorrect decision for IPCondition");
        }

        Set<String> dsnAdvice = advices.get(DNSNameCondition.class.getName());
        if ((dsnAdvice == null) || dsnAdvice.isEmpty()) {
            throw new Exception(
                    "OrConditionEvalTest.negativeTest: no advice for DNSNameCondition.");
        }

        String dnsAdv = dsnAdvice.iterator().next();
        if (!dnsAdv.equals(DNSNameCondition.REQUEST_DNS_NAME + "=" + DNS_MASK)){
            throw new Exception(
                    "OrConditionEvalTest.negativeTest: incorrect decision for DNSNameCondition");
        }
    }

    private Map<String, Set<String>> getEnvironment(String ipAddr, String dns) {
        Map<String, Set<String>> environment =
                new HashMap<String, Set<String>>();
        Set<String> dnsMask = new HashSet<String>();
        dnsMask.add(dns);
        environment.put(DNSNameCondition.REQUEST_DNS_NAME, dnsMask);

        Set<String> ip = new HashSet<String>();
        ip.add(ipAddr);
        environment.put(IPCondition.REQUEST_IP, ip);

        return environment;
    }
}
