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
 * $Id: TimeConditionEvaluation.java,v 1.3 2009/10/13 22:37:53 veiming Exp $
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

public class TimeConditionEvaluation {
    private static final String PRIVILEGE_NAME =
        "TimeConditionEvaluationPrivilege";
    private static final String URL = "http://www.timeconditionevaluation.com";
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = EntitlementConfiguration.getInstance(
        adminSubject, "/").migratedToEntitlementService();

    @BeforeClass
    public void setup() throws Exception {
        if (!migrated) {
            return;
        }

        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        Map<String, Boolean> actions = new HashMap<String, Boolean>();
        actions.put("GET", Boolean.TRUE);
        Entitlement ent = new Entitlement(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, URL, actions);

        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(PRIVILEGE_NAME);
        privilege.setEntitlement(ent);
        privilege.setSubject(new AnyUserSubject());

        TimeCondition tc = new TimeCondition();
        tc.setStartTime("15:00");
        tc.setEndTime("16:00");

        privilege.setCondition(tc);
        pm.addPrivilege(privilege);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        pm.removePrivilege(PRIVILEGE_NAME);
    }

    private List<Entitlement> evaluate(String time) throws Exception {
        Evaluator evaluator = new Evaluator(
            SubjectUtils.createSubject(adminToken),
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        return evaluator.evaluate("/", null, URL, envTime(time),
            false);
    }

    @Test
    public void positiveTest()
        throws Exception {
        List<Entitlement> entitlements = evaluate("1251847000000");

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception("TimeConditionEvaluation.positiveTest: no entitlements");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");
        if ((result == null) || !result) {
            throw new Exception("TimeConditionEvaluation.positiveTest: incorrect result");
        }

        Map<String, Set<String>> advices = ent.getAdvices();
        if ((advices == null) || advices.isEmpty()) {
            throw new Exception("TimeConditionEvaluation.positiveTest: no advices");
        }

        Set<String> set = advices.get(ConditionDecision.MAX_TIME);

        if ((set == null) || set.isEmpty()) {
            throw new Exception("TimeConditionEvaluation.positiveTest: no advices for MAX_TIME");
        }

        if (!set.contains("1251849600000")) {
            throw new Exception("TimeConditionEvaluation.positiveTest: incorrect advices for MAX_TIME");
        }
    }


    @Test (dependsOnMethods={"positiveTest"})
    public void negativeLowerLimitTest()
        throws Exception {
        List<Entitlement> entitlements = evaluate("1248994000000");

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception("TimeConditionEvaluation.negativeLowerLimitTest: no entitlements");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");
        if ((result != null) && result) {
            throw new Exception("TimeConditionEvaluation.negativeLowerLimitTest: incorrect result");
        }

        Map<String, Set<String>> advices = ent.getAdvices();
        if ((advices == null) || advices.isEmpty()) {
            throw new Exception("TimeConditionEvaluation.negativeLowerLimitTest: no advices");
        }

        Set<String> set = advices.get(ConditionDecision.MAX_TIME);

        if ((set == null) || set.isEmpty()) {
            throw new Exception("TimeConditionEvaluation.negativeLowerLimitTest: no advices for MAX_TIME");
        }

        if (!set.contains("1248994800000")) {
            throw new Exception("TimeConditionEvaluation.negativeLowerLimitTest: incorrect advices for MAX_TIME");
        }
    }

    @Test (dependsOnMethods={"negativeLowerLimitTest"})
    public void negativeUpperLimitTest()
        throws Exception {
        List<Entitlement> entitlements = evaluate("128999400000");

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception("TimeConditionEvaluation.negativeUpperLimitTest: no entitlements");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");
        if ((result != null) && result) {
            throw new Exception("TimeConditionEvaluation.negativeUpperLimitTest: incorrect result");
        }

        Map<String, Set<String>> advices = ent.getAdvices();
        if ((advices != null) && !advices.isEmpty()) {
            throw new Exception("TimeConditionEvaluation.negativeUpperLimitTest: no advices");
        }
    }

    @Test (dependsOnMethods={"negativeUpperLimitTest"})
    public void indefiniteEndTimeTest() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        Privilege p = pm.getPrivilege(PRIVILEGE_NAME);
        TimeCondition tc = (TimeCondition)p.getCondition();
        tc.setEndTime(null);
        pm.modifyPrivilege(p);

        List<Entitlement> entitlements = evaluate("128999400000");
        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception(
                "TimeConditionEvaluation.indefiniteEndTimeTest: no entitlements");
        }

        Entitlement ent = entitlements.get(0);
        Boolean result = ent.getActionValue("GET");
        if ((result == null) || !result) {
            throw new Exception(
                "TimeConditionEvaluation.indefiniteEndTimeTest: incorrect result");
        }
    }

    private Map<String, Set<String>> envTime(String time) {
        Map<String, Set<String>> conditions = new
            HashMap<String, Set<String>>();
        Set<String> setTime = new HashSet<String>();
        setTime.add(time);
        conditions.put(TimeCondition.REQUEST_TIME, setTime);
        return conditions;
    }
}

