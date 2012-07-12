/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RealmTest.java,v 1.3 2008/06/25 05:44:22 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.test.common.TestBase;
import java.util.Collections;
import java.util.Set;
import org.testng.annotations.Test;

/**
 * Tests for creation and deletion of realms using OrganizationConfigManager.
 */
public class RealmTest extends TestBase {
    
    public RealmTest() {
        super("SMS");
    }
    
    /**
     * Test case for Issue #181. When a realm is deleted and created
     * again, it fails with "Realm already exists".
     */
    @Test(groups = {"api"}) //, expectedExceptions={SMSException.class})
    public void createRealmTwice() throws SMSException, SSOException {
        SSOToken adminToken = getAdminSSOToken();
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, "/");
        String realm = "sm-issue181-create-realm-twice";
        ocm.createSubOrganization(realm, Collections.EMPTY_MAP);
        ocm.deleteSubOrganization(realm, true);
        ocm.createSubOrganization(realm, Collections.EMPTY_MAP);
        ocm.deleteSubOrganization(realm, true);
        ocm.createSubOrganization(realm, Collections.EMPTY_MAP);
        ocm.deleteSubOrganization(realm, true);
    }
    
    /**
     * Test case of creating a new with the same name. Should throw
     * an SMSException that realm already exists.
     */
    @Test(groups = ("api"), expectedExceptions={SMSException.class})
    public void createRealmWithSameName() throws SMSException,
        SSOException {
        OrganizationConfigManager ocm = null;
        String realm = "sm-noissue-create-realm-with-same-name";
        try {
            SSOToken token = getAdminSSOToken();
            ocm = new OrganizationConfigManager(token, "/");
            ocm.createSubOrganization(realm, Collections.EMPTY_MAP);
            ocm.createSubOrganization(realm, Collections.EMPTY_MAP);
        } finally {
            if (ocm != null) {
                ocm.deleteSubOrganization(realm, true);
            }
        }
    }
    
    /**
     * Test case for Issue #230. Unable to get sub realms using remote
     */
    @Test(groups = {"api"})
    public void verifyJAXRPC() throws SMSException, SSOException {
        String realm = "sm-issue-230-for-remote";
        OrganizationConfigManager ocm =
            new OrganizationConfigManager(getAdminSSOToken(), "/");
        try {
            // Create an organization
            ocm.createSubOrganization(realm, Collections.EMPTY_MAP);
            // Search for sub-organizations
            Set realms = ocm.getSubOrganizationNames();
            // Check if the realm exists
            if (!realms.contains(realm)) {
                throw (new SMSException("issue 230 failed"));
            }
        } finally {
            if (ocm != null) {
                ocm.deleteSubOrganization(realm, true);
            }
        }
    }
}
