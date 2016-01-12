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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgerock.opendj.ldap.controls.TransactionIdControl;

import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.Entries;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.Request;
import org.forgerock.services.TransactionId;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LDAPRequestsTest {
    private static final TransactionId TRANSACTION_ID = new TransactionId();

    @BeforeClass
    public void setTransactionId() {
        AuditRequestContext.set(new AuditRequestContext(TRANSACTION_ID));
    }

    @AfterClass
    public void clearTransactionId() {
        AuditRequestContext.clear();
    }


    @Test(dataProvider = "supportedRequests")
    public void shouldAddTransactionIdControlToRequests(Request request) throws Exception {
        assertThat(request.containsControl(TransactionIdControl.OID)).isTrue();
        TransactionIdControl control = request.getControl(TransactionIdControl.DECODER, new DecodeOptions());
        assertThat(control.getTransactionId()).startsWith(TRANSACTION_ID.getValue());
    }


    @DataProvider
    public static Object[][] supportedRequests() {
        final String dn = "uid=test,dc=example,dc=com";
        return new Object[][] {
                { LDAPRequests.newSimpleBindRequest(dn, "sekret".toCharArray()) },
                { LDAPRequests.newModifyRequest(dn) },
                { LDAPRequests.newModifyRequest(DN.valueOf(dn)) },
                { LDAPRequests.newSearchRequest(DN.valueOf(dn), SearchScope.BASE_OBJECT,
                        Filter.alwaysTrue(), "test") },
                { LDAPRequests.newSearchRequest(dn, SearchScope.BASE_OBJECT, "(objectclass=*)",
                        "test") },
                { LDAPRequests.newSingleEntrySearchRequest(DN.valueOf(dn), "test") },
                { LDAPRequests.newSingleEntrySearchRequest(dn, "test") },
                { LDAPRequests.newSingleEntrySearchRequest(dn, SearchScope.SUBORDINATES, "(objectclass=*)", "test") },
                { LDAPRequests.newAddRequest(dn) },
                { LDAPRequests.newAddRequest(DN.valueOf(dn)) },
                { LDAPRequests.newAddRequest(Entries.makeEntry("dn: " + dn)) },
                { LDAPRequests.newDeleteRequest(dn) },
                { LDAPRequests.newDeleteRequest(DN.valueOf(dn)) },
                { LDAPRequests.newModifyDNRequest(dn, "uid=test2,dc=example,dc=com") }
        };
    }

}