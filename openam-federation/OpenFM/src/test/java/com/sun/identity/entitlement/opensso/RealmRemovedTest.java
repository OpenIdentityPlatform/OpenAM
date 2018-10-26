/**
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
 * $Id: RealmRemovedTest.java,v 1.1 2010/01/11 20:15:45 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectImplementation;
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
public class RealmRemovedTest {
    private static final String REFERRAL_NAME1 = "RealmRemovedTestReferralP1";
    private static final String REFERRAL_NAME2 = "RealmRemovedTestReferralP2";
    private static final String APP_PRIVILEGE_NAME =
        "RealmRemovedTestApplicationP";
    private static final String SUB_REALM1 = "/RealmRemovedTestSubRealm1";
    private static final String SUB_REALM2 = "/RealmRemovedTestSubRealm2";
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
        String subRealm = SUB_REALM1.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);
        subRealm = SUB_REALM2.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);

        createApplicationPrivilege();
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM2.substring(1);
        ocm.deleteSubOrganization(subRealm, true);
    }

    private void createApplicationPrivilege() throws EntitlementException {
        ApplicationPrivilegeManager mgr =
            ApplicationPrivilegeManager.getInstance(SUB_REALM1,
            SubjectUtils.createSubject(adminToken));
        ApplicationPrivilege ap = new ApplicationPrivilege(
            APP_PRIVILEGE_NAME);
        OpenSSOUserSubject sbj = new OpenSSOUserSubject();
        sbj.setID("ou=dummy,ou=user,dc=openam,dc=openidentityplatform,dc=org");
        Set<SubjectImplementation> subjects = new
            HashSet<SubjectImplementation>();
        subjects.add(sbj);
        ap.setSubject(subjects);

        Map<String, Set<String>> appRes = new HashMap<String, Set<String>>();
        Set<String> res = new HashSet<String>();
        appRes.put(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, res);
        res.add("http://www.RealmRemovedTest.com/*");
        ap.setApplicationResources(appRes);
        ap.setActionValues(
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE);
        mgr.addPrivilege(ap);
    }


    @Test
    public void test() throws Exception {
        if (!migrated) {
            return;
        }

        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM1.substring(1);
        ocm.deleteSubOrganization(subRealm, true);

        // application privilege should be removed.
        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        try {
            apm.getPrivilege(APP_PRIVILEGE_NAME);
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 325) {
                throw e;
            }
        }
    }
}
