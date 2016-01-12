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

import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import static org.fest.assertions.Assertions.*;

import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.MapHelper;
import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.RequestContext;
import org.forgerock.opendj.ldap.RequestHandler;
import org.forgerock.opendj.ldap.ResultCode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Contains test cases against an AD where the authentication and user lookup occurs based on the mail attribute.
 */
public class ADMailBasedRepoTest extends IdRepoTestBase {

    private static final String AD_MAIL_SETTINGS = "/config/admailsettings.properties";
    private static final String AD_MAIL_LDIF = "/ldif/admail.ldif";
    private static final String DEMO_MAIL = "demo@example.com";

    @BeforeClass
    public void setUp() throws Exception {
        idrepo.initialize(MapHelper.readMap(AD_MAIL_SETTINGS));
        idrepo.addListener(null, idRepoListener);
    }

    @Override
    protected RequestHandler<RequestContext> decorateBackend(MemoryBackend memoryBackend) {
        return new ADBackend(memoryBackend);
    }

    @Override
    protected String getLDIFPath() {
        return AD_MAIL_LDIF;
    }

    @Test(description = "OPENAM-3428")
    public void canAuthenticateWithMailAttribute() throws Exception {
        assertThat(idrepo.authenticate(getCredentials(DEMO_MAIL, "changeit"))).isTrue();
        //simulate profile lookup
        CrestQuery crestQuery = new CrestQuery(DEMO_MAIL);
        RepoSearchResults results =
                idrepo.search(null, IdType.USER, crestQuery, 0, 0, null, true, IdRepo.OR_MOD, null, true);
        assertThat(results.getErrorCode()).isEqualTo(ResultCode.SUCCESS.intValue());
        assertThat(results.getType()).isEqualTo(IdType.USER);
        assertThat(results.getSearchResults()).isNotEmpty().hasSize(1).containsOnly(DEMO_MAIL);
    }

    @Test(description = "OPENAM-3428")
    public void searchReturnsSearchAttributeValues() throws Exception {
        CrestQuery crestQuery = new CrestQuery(DEMO_MAIL);
        RepoSearchResults results =
                idrepo.search(null, IdType.USER, crestQuery, 0, 0, null, true, IdRepo.OR_MOD, null, true);
        assertThat(results.getErrorCode()).isEqualTo(ResultCode.SUCCESS.intValue());
        assertThat(results.getType()).isEqualTo(IdType.USER);
        assertThat(results.getSearchResults()).isNotEmpty().hasSize(1).containsOnly(DEMO_MAIL);
    }
}
