/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ReferralPrivilegeWithRemovedRealmTest.java,v 1.1 2010/01/08 23:59:31 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dennis
 */
public class ReferralPrivilegeWithRemovedRealmTest {
    private static final String REFERRAL_NAME =
        "ReferralPrivilegeWithRemovedRealmTestReferralP";
    private static final String SUB_REALM =
        "/ReferralPrivilegeWithRemovedRealmTestSubRealm";
    private SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    @BeforeClass
    public void setup() throws Exception {
        if (!migrated) {
            return;
        }

        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);

        createReferral();
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
    }

    private void createReferral()
        throws EntitlementException {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add("http://www.ReferralPrivilegeWithRemovedRealmTest.com/*");
        map.put(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, set);
        Set<String> realms = new HashSet<String>();
        realms.add(SUB_REALM);
        ReferralPrivilege r1 = new ReferralPrivilege(REFERRAL_NAME,
            map, realms);
    }

    @Test
    public void test() throws Exception {
        if (!migrated) {
            return;
        }

        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM.substring(1);
        ocm.deleteSubOrganization(subRealm, true);

        Evaluator evaluator = new Evaluator(
            adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
                Set actions = new HashSet();
        actions.add("GET");

        //should not result in NPE.
        evaluator.hasEntitlement("/", adminSubject,
            new Entitlement(
            "http://www.ReferralPrivilegeWithRemovedRealmTest.com/index.html",
            actions), Collections.EMPTY_MAP);
    }
}
