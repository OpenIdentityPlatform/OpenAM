/*
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
 * $Id: ApplicationMetaData.java,v 1.1 2009/09/25 05:52:56 veiming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import static com.sun.identity.entitlement.Application.NAME_ATTRIBUTE;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.Set;
import javax.security.auth.Subject;

import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

/**
 *
 * @author dillidorai
 */
public class ApplicationMetaData {
    private static final String APPL_NAME = "ApplicationMetaDataAppl";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    @BeforeClass
    public void setup() 
        throws SSOException, IdRepoException, EntitlementException,
        SMSException, InstantiationException, IllegalAccessException {

        if (!migrated) {
            return;
        }
        Application appl = new Application(APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));

        // Test disabled, unable to fix model change
        // Set<String> appResources = new HashSet<String>();
        // appResources.add("http://www.applicationmetadata.com");
        // appl.addResources(appResources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }

        ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);
    }

    @Test
    public void test() throws Exception {
        if (!migrated) {
            return;
        }
        Application appl = ApplicationServiceTestHelper.getApplication(
                adminSubject, "/", APPL_NAME);
        String createdBy = appl.getCreatedBy();
        long creationTime = appl.getCreationDate();
        long modifiedTime = appl.getLastModifiedDate();

        if (creationTime != modifiedTime) {
            throw new Exception(
                "ApplicationMetaData.test: modified and creation time diff");
        }
        ApplicationServiceTestHelper.saveApplication(adminSubject, "/", appl);
        creationTime = appl.getCreationDate();
        modifiedTime = appl.getLastModifiedDate();

        if (creationTime == modifiedTime) {
            throw new Exception(
                "ApplicationMetaData.test: modified and creation time unchanged");
        }

        if (!createdBy.equals(appl.getLastModifiedBy())) {
            throw new Exception(
                "ApplicationMetaData.test: createdBy and modifiedBy should be the same");
        }

        Set<Application> results = ApplicationServiceTestHelper.search(
                adminSubject, "/", QueryFilter.equalTo(NAME_ATTRIBUTE, APPL_NAME));
        if (!results.isEmpty()) {
            throw new Exception(
                "ApplicationMetaData.test: search fails");
        }
    }
}
