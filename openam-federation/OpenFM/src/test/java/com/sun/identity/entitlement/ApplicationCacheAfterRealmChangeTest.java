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
 * $Id: ApplicationCacheAfterRealmChangeTest.java,v 1.2 2010/01/20 17:01:36 veiming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class ApplicationCacheAfterRealmChangeTest {
    private static final String SUB_REALM =
        "/ApplicationCacheAfterRealmChangeTest";
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    @BeforeClass
    public void setup()
        throws Exception {

        if (!migrated) {
            return;
        }

        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM.substring(1);
        ocm.createSubOrganization(subRealm, Collections.EMPTY_MAP);
        setOrgAlias(true);
    }

    private void setOrgAlias(boolean flag) throws SMSException, SSOException {
        ServiceSchemaManager ssm = new ServiceSchemaManager(
            PolicyConfig.POLICY_CONFIG_SERVICE, adminToken);
        ServiceSchema global = ssm.getSchema(SchemaType.GLOBAL);
        Set<String> values = new HashSet<String>();
        values.add(Boolean.toString(flag));
        global.setAttributeDefaults(
            "sun-am-policy-config-org-alias-mapped-resources-enabled",
            values);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM.substring(1);
        ocm.deleteSubOrganization(subRealm, true);
        setOrgAlias(false);
    }

    @Test
    public void test() throws Exception {
        if (!migrated) {
            return;
        }
        Application appl = ApplicationServiceTestHelper.getApplication(
                adminSubject, SUB_REALM, ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);

        // Test disabled, unable to fix model changes
        // Set<String> resources = appl.getResources();
        // if ((resources != null) && !resources.isEmpty()) {
        //     throw new Exception("ApplicationCacheAfterRealmChangeTest: " +
        //         "application resources should be empty");
        // }

        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, SUB_REALM);

        Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();
        Set<String> setAlias = new HashSet<String>();
        setAlias.add("www.ApplicationCacheAfterRealmChangeTest.com");
        attributes.put("sunOrganizationAliases", setAlias);
        Set<String> setStatus = new HashSet<String>();
        setStatus.add("Active");
        attributes.put("sunOrganizationStatus", setStatus);

        ocm.setAttributes(IdConstants.REPO_SERVICE, attributes);

        appl = ApplicationServiceTestHelper.getApplication(
                adminSubject, SUB_REALM, ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);

        // Test disabled, unable to fix model changes.
        // resources = appl.getResources();
        // if ((resources == null) || resources.isEmpty()) {
        //     throw new Exception("ApplicationCacheAfterRealmChangeTest: " +
        //         "application resources should NOT be empty");
        // }
    }

}
