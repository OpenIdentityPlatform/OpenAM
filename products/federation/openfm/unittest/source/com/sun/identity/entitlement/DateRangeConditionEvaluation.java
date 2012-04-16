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
 * $Id: DateRangeConditionEvaluation.java,v 1.2 2009/10/13 22:37:53 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DateRangeConditionEvaluation {
    private static final String PRIVILEGE_NAME =
        "DateRangeConditionEvaluationPrivilege";
    private static final String URL = 
        "http://www.daterangeconditionevaluation.com";
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
        tc.setStartDate("2008:01:01");
        tc.setEndDate("2010:01:01");
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

    @Test
    public void positiveTest() throws Exception {
        boolean result = evaluate();
        if (!result) {
            throw new Exception("DateRangeConditionEvaluation.positiveTest fails");
        }
    }

    private boolean evaluate()
        throws Exception {
        Set actions = new HashSet();
        actions.add("GET");
        Evaluator evaluator = new Evaluator(
            SubjectUtils.createSubject(adminToken),
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        Map<String, Set<String>> conditions = new
            HashMap<String, Set<String>>();
        Set<String> setTime = new HashSet<String>();
        // 7/31/09 12:05 AM
        setTime.add("1249027551534");
        conditions.put(TimeCondition.REQUEST_TIME, setTime);
        return evaluator.hasEntitlement("/", null,
            new Entitlement(URL, actions), conditions);

    }

    @Test (dependsOnMethods = {"positiveTest"})
    public void negativeTest() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        Privilege p = pm.getPrivilege(PRIVILEGE_NAME);
        TimeCondition tc = (TimeCondition)p.getCondition();
        tc.setEndDate("2008:01:02");
        pm.modifyPrivilege(p);
        boolean result = evaluate();
        if (result) {
            throw new Exception("DateRangeConditionEvaluation.negativeTest fails");
        }
    }

    @Test (dependsOnMethods = {"negativeTest"})
    public void indefiniteEndDate() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        Privilege p = pm.getPrivilege(PRIVILEGE_NAME);
        TimeCondition tc = (TimeCondition)p.getCondition();
        tc.setEndDate(null);
        pm.modifyPrivilege(p);
        boolean result = evaluate();
        if (!result) {
            throw new Exception("DateRangeConditionEvaluation.indefiniteEndDate fails");
        }
    }

}

