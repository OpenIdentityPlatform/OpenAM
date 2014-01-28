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
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.idm;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.ldap.util.DN;
import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;
import org.testng.annotations.Test;

public class AMIdentityTest {

    @Test
    public void regularCharactersWorkAsExpectedWithToken() throws Exception {
        SSOToken token = mock(SSOToken.class);
        String uuid = "id=badger,ou=user,dc=config";
        when(token.getProperty(eq(Constants.UNIVERSAL_IDENTIFIER))).thenReturn(uuid);
        AMIdentity identity = new AMIdentity(token);
        assertThat(identity.getName()).isEqualTo("badger");
        assertThat(identity.getType()).isEqualTo(IdType.USER);
        assertThat(identity.getUniversalId()).isEqualTo(uuid);
        assertThat(identity.getRealm()).isEqualTo("dc=config");
        assertThat(identity.getDN()).isNull();
    }

    @Test
    public void regularCharactersWorkAsExpectedWithTokenAndUUID() throws Exception {
        String uuid = "id=badger,ou=user,dc=config";
        AMIdentity identity = new AMIdentity(null, uuid);
        assertThat(identity.getName()).isEqualTo("badger");
        assertThat(identity.getType()).isEqualTo(IdType.USER);
        assertThat(identity.getUniversalId()).isEqualTo(uuid);
        assertThat(identity.getRealm()).isEqualTo("dc=config");
        assertThat(identity.getDN()).isNull();
    }

    @Test
    public void regularCharactersWorkAsExpectedWithTokenAndDN() throws Exception {
        String uuid = "id=badger,ou=user,dc=config";
        AMIdentity identity = new AMIdentity(new DN(uuid), null);
        assertThat(identity.getName()).isEqualTo("badger");
        assertThat(identity.getType()).isEqualTo(IdType.USER);
        assertThat(identity.getUniversalId()).isEqualTo(uuid);
        assertThat(identity.getRealm()).isEqualTo("dc=config");
        assertThat(identity.getDN()).isNull();
    }

    @Test
    public void specialCharactersAreHandledConsistently() throws Exception {
        String uuid = "id=hello\\+world,ou=user,dc=config";
        AMIdentity identity = new AMIdentity(new DN(uuid), null);
        assertThat(identity.getName()).isEqualTo("hello+world");
        assertThat(identity.getType()).isEqualTo(IdType.USER);
        assertThat(identity.getUniversalId()).isEqualTo(uuid);
        assertThat(identity.getRealm()).isEqualTo("dc=config");
        assertThat(identity.getDN()).isNull();
    }

    @Test(expectedExceptions = IdRepoException.class)
    public void emtpyStringResultsInException() throws Exception {
        new AMIdentity(null, "");
    }

    @Test(expectedExceptions = IdRepoException.class)
    public void nullUniversalIdResultsInException() throws Exception {
        new AMIdentity(null, (String) null);
    }
}
