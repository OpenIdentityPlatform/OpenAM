/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.auditors;

import static org.fest.assertions.Assertions.assertThat;

import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test class for the SMS auditor
 */
public class SMSAuditorTest {

    @DataProvider(name = "DNprovider")
    public Object[][] createData1() {
        return new Object[][] {
                { "ou=default,ou=GlobalConfig,ou=1.0,ou=AuditService,ou=services,dc=openam,dc=openidentityplatform,dc=org", null },
                { "ou=default,ou=OrganizationConfig,ou=1.0,ou=AuditService,ou=services,dc=openam,dc=openidentityplatform,dc=org", "/"},
                { "ou=default,ou=OrganizationConfig,ou=1.0,ou=AuditService,ou=services,o=subrealm,ou=services,dc=openam,dc=openidentityplatform,dc=org", "/subrealm"},
                { "ou=default,ou=OrganizationConfig,ou=1.0,ou=AuditService,ou=services,o=subsubrealm,o=subrealm,ou=services,dc=openam,dc=openidentityplatform,dc=org", "/subrealm/subsubrealm"},

        };
    }


    @Test(dataProvider = "DNprovider")
    public void testGetRealmFromDN(String dn, String expectedRealm) throws Exception {
        Reporter.log(SMSAuditor.getRealmFromDN(dn) + " - " + expectedRealm);
        assertThat(SMSAuditor.getRealmFromDN(dn)).isEqualTo(expectedRealm);
    }
}