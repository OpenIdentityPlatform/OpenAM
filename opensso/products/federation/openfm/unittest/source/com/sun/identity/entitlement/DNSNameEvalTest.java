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
 * $Id: DNSNameEvalTest.java,v 1.1 2009/09/05 00:24:03 veiming Exp $
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

public class DNSNameEvalTest {
    private static final String PRIVILEGE_NAME = "DNSNameEvalTest";
    private static final String ROOT_RESOURCE_NAME = 
            "http://www.DNSNameEvalTest.com";
    private static final String DNS_MASK =  "*.dnsnameevaltest.com";

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

            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME);
            privilege.setEntitlement(ent);
            privilege.setSubject(new AnyUserSubject());
            privilege.setCondition(new DNSNameCondition(DNS_MASK));

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
                "www.dnsnameevaltest.com");

        Evaluator evaluator = new Evaluator(adminSubject,
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        List<Entitlement> entitlements = evaluator.evaluate("/", adminSubject,
                ROOT_RESOURCE_NAME + "/index.html", environment, false);

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception(
                    "DNSNameEvalTest.positiveTest: no entitlements returned");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");

        if ((result == null) || !result) {
            throw new Exception(
                    "DNSNameEvalTest.positiveTest: incorrect decision");
        }

        Map<String, Set<String>> advice = ent.getAdvices();
        if ((advice != null) && !advice.isEmpty()) {
            throw new Exception(
                    "DNSNameEvalTest.positiveTest: do not expect advices.");
        }
    }
    
    @Test
    public void negativeTest() throws Exception {
        Map<String, Set<String>> environment = getEnvironment(
                "www.dnsnameevaltest1.com");

        Evaluator evaluator = new Evaluator(adminSubject,
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        List<Entitlement> entitlements = evaluator.evaluate("/", adminSubject,
                ROOT_RESOURCE_NAME + "/index.html", environment, false);

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception(
                    "DNSNameEvalTest.negativeTest: no entitlements returned");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");

        if ((result != null) && result) {
            throw new Exception(
                    "DNSNameEvalTest.negativeTest: incorrect decision");
        }

        Map<String, Set<String>> advices = ent.getAdvices();
        if ((advices == null) || advices.isEmpty()) {
            throw new Exception(
                    "DNSNameEvalTest.negativeTest: no advices.");
        }

        Set<String> advice = advices.get(DNSNameCondition.class.getName());
        if ((advice == null) || advice.isEmpty()) {
            throw new Exception(
                    "DNSNameEvalTest.negativeTest: no advice for DNSNameCondition.");
        }

        String adv = advice.iterator().next();
        if (!adv.equals(DNSNameCondition.REQUEST_DNS_NAME + "=" + DNS_MASK)) {
            throw new Exception(
                    "DNSNameEvalTest.negativeTest: incorrect decision");
        }
    }

    private Map<String, Set<String>> getEnvironment(String dns) {
        Map<String, Set<String>> environment =
                new HashMap<String, Set<String>>();
        Set<String> ip = new HashSet<String>();
        ip.add(dns);
        environment.put(DNSNameCondition.REQUEST_DNS_NAME, ip);
        return environment;
    }
}
