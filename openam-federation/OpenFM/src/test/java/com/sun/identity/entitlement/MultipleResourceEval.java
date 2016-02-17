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
 * $Id: MultipleResourceEval.java,v 1.1 2009/09/10 16:35:38 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MultipleResourceEval {
    private static final String APPL_NAME =
        ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;
    private static final String PRIVILEGE_NAME = "MultipleResourceEval";
    private static final String URL = "http://www.MultipleResourceEval.com:80";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    @BeforeClass
    public void setup() throws Exception {
        if (!migrated) {
            return;
        }
      
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        {
            Map<String, Boolean> actions = new HashMap<String, Boolean>();
            actions.put("GET", Boolean.TRUE);
            Entitlement ent = new Entitlement(APPL_NAME, URL + "/*", actions);
            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME + "1");
            privilege.setEntitlement(ent);
            privilege.setSubject(new AnyUserSubject());
            pm.add(privilege);
        }
        {
            Map<String, Boolean> actions = new HashMap<String, Boolean>();
            actions.put("GET", Boolean.FALSE);
            Entitlement ent = new Entitlement(APPL_NAME, URL + "/index.html",
                actions);
            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME + "2");
            privilege.setEntitlement(ent);
            privilege.setSubject(new AnyUserSubject());
            pm.add(privilege);
        }

        Thread.sleep(1000);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        
        PrivilegeManager pm = PrivilegeManager.getInstance("/", adminSubject);
        pm.remove(PRIVILEGE_NAME + "1");
        pm.remove(PRIVILEGE_NAME + "2");
    }

    @Test
    public void postiveTest()
        throws Exception {
        if (!migrated) {
            return;
        }
        Set<String> res = new HashSet<String>();
        res.add(URL + "/a");
        res.add(URL + "/index.html");
        Evaluator evaluator = new Evaluator(
            SubjectUtils.createSubject(adminToken), APPL_NAME);
        List<Entitlement> entitlements = evaluator.evaluate(
            "/", adminSubject, res, Collections.EMPTY_MAP);

        if ((entitlements == null) || entitlements.isEmpty()) {
            throw new Exception("MultipleResourceEval.postiveTest failed");
        }

        for (Entitlement e : entitlements) {
            String r = e.getResourceName();
            Boolean result = e.getActionValue("GET");

            if (r.equals(URL + "/a")) {
                if (!result) {
                    throw new Exception(
                        "MultipleResourceEval.postiveTest: result should be true for /a");
                }
            } else if (r.equals(URL + "/index.html")) {
                if (result) {
                    throw new Exception(
                        "MultipleResourceEval.postiveTest: result should be false for /index.html");
                }
            }
        }
    }
}
