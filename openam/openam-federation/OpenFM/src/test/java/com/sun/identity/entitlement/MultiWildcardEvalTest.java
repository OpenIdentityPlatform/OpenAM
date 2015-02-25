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
 * $Id: MultiWildcardEvalTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
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


public class MultiWildcardEvalTest {
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
        AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);

    @BeforeClass
    public void setup() throws Exception {
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", true);
        Entitlement e1 = new Entitlement(
            "http://sun.com/a/*/b/*/c", actionValues);
        EntitlementSubject sbj = new AuthenticatedUsers();

        Privilege p1 = Privilege.getNewInstance();
        p1.setName("MultiWildcardEvalTest");
        p1.setEntitlement(e1);
        p1.setSubject(sbj);
        PrivilegeManager mgr = PrivilegeManager.getInstance("/",
            adminSubject);
        mgr.add(p1);
    }

    @AfterClass
    public void cleanup() throws Exception {
        PrivilegeManager mgr = PrivilegeManager.getInstance("/",
            adminSubject);
        mgr.remove("MultiWildcardEvalTest");
    }

    @Test
    public void test() throws Exception {
        Evaluator eval = new Evaluator(adminSubject);
        Set<String> actions = new HashSet<String>();
        actions.add("GET");
        Entitlement e = new Entitlement(
            "http://sun.com/a/a1/b/b1/c", actions);
        if (!eval.hasEntitlement("/", adminSubject, e,
            Collections.EMPTY_MAP)) {
            cleanup();
            throw new Exception("OrgAliasReferralTest.test failed.");
        }
    }
}
