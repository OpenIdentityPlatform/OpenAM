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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.fail;

import org.forgerock.openam.utils.MapHelper;
import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.RequestContext;
import org.forgerock.opendj.ldap.RequestHandler;
import org.forgerock.opendj.ldap.ResultCode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * Contains basic test cases in a standard AD setup, where login and lookup occurs based on the CN attribute.
 */
public class ADBasicRepoTest extends IdRepoTestBase {

    private static final String AD_BASIC_SETTINGS = "/config/adbasicsettings.properties";
    private static final String AD_BASIC_LDIF = "/ldif/adbasic.ldif";

    @BeforeClass
    public void setUp() throws Exception {
        idrepo.initialize(MapHelper.readMap(AD_BASIC_SETTINGS));
        idrepo.addListener(null, idRepoListener);
    }

    @Override
    protected RequestHandler<RequestContext> decorateBackend(MemoryBackend memoryBackend) {
        return new ADBackend(memoryBackend);
    }

    @Override
    protected String getLDIFPath() {
        return AD_BASIC_LDIF;
    }

    @Test
    public void referenceShouldNotCauseExceptionDuringLookup() throws Exception {
        String fqn = idrepo.getFullyQualifiedName(null, IdType.USER, DEMO);
        assertThat(fqn).isNotNull().isEqualTo("[localhost:389]/cn=Demo,cn=users,dc=openam,dc=openidentityplatform,dc=org");
    }

    @Test
    public void noMatchingUserResultsInException() throws Exception {
        try {
            //using getAttributes to test the error case as getFullyQualifiedName should return with null if the user
            //does not exist
            idrepo.getAttributes(null, IdType.USER, "badger");
            fail();
        } catch (IdentityNotFoundException infe) {
            assertThat(infe).hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.TYPE_NOT_FOUND, "badger", IdType.USER.getName()));
            assertThat(infe.getLDAPErrorCode()).isNotNull().isEqualTo(
                    String.valueOf(ResultCode.CLIENT_SIDE_NO_RESULTS_RETURNED.intValue()));
        }
    }

    @Test
    public void moreThanOneMatchResultsInException() throws Exception {
        try {
            idrepo.getFullyQualifiedName(null, IdType.USER, "duplicate");
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire).hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.LDAP_EXCEPTION_OCCURRED,
                    DJLDAPv3Repo.class.getName(),
                    ResultCode.CLIENT_SIDE_UNEXPECTED_RESULTS_RETURNED.intValue()));
            assertThat(ire.getLDAPErrorCode()).isNotNull().isEqualTo(
                    String.valueOf(ResultCode.CLIENT_SIDE_UNEXPECTED_RESULTS_RETURNED.intValue()));
        }
    }
}
