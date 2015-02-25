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
 * $Id: HttpStarEvaluationTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class HttpStarEvaluationTest {
    private static final String POLICY_NAME =
        "HttpStarEvaluationTestPolicy";
    private Subject adminSubject;

    @BeforeClass
    public void setup() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        adminSubject = SubjectUtils.createSubject(adminToken);
        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(POLICY_NAME);

        Map<String, Boolean> actions = new HashMap<String, Boolean>();
        actions.put("findAll", true);
        Entitlement entitlement = new Entitlement(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, "http://*",
            actions);
        privilege.setEntitlement(entitlement);
        privilege.setSubject(new AuthenticatedUsers());
        pm.add(privilege);
    }

    @AfterClass
    public void cleanup() throws EntitlementException {
        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        pm.remove(POLICY_NAME);
    }

    @Test
    public void testEvaluation()
        throws Exception {
        Set actions = new HashSet();
        actions.add("findAll");
        Evaluator evaluator = new Evaluator(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        boolean allow = evaluator.hasEntitlement("/", adminSubject,
            new Entitlement("http://www.httpstarevaluationtest.com/index.html",
            actions),Collections.EMPTY_MAP);
        if (!allow) {
            throw new Exception(
                "HttpStarEvaluationTest.testEvaluation: incorrect policy decision.");
        }
    }
}
