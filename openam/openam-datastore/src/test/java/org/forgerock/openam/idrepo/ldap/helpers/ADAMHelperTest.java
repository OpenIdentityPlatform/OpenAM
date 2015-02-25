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
package org.forgerock.openam.idrepo.ldap.helpers;

import com.sun.identity.idm.IdType;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.*;
import org.forgerock.openam.utils.CollectionUtils;
import static org.forgerock.openam.utils.CollectionUtils.*;

public class ADAMHelperTest {

    private static final String PASSWORD = "secret123";
    private static final String QUOTED_PASSWORD = "\"secret123\"";
    private static final String ENCODING = "UTF-16LE";
    private final ADAMHelper helper = new ADAMHelper();

    @Test
    public void nullPasswordIsNotEncoded() {
        assertThat(helper.encodePassword(null)).isNull();
        assertThat(helper.encodePassword(IdType.USER, (byte[][]) null)).isNull();
    }

    @Test
    public void passwordsAreOnlyEncodedForUsers() {
        assertThat(helper.encodePassword(IdType.AGENT, asSet(PASSWORD))).isNull();
        assertThat(helper.encodePassword(IdType.AGENTGROUP, asSet(PASSWORD))).isNull();
        assertThat(helper.encodePassword(IdType.AGENTONLY, asSet(PASSWORD))).isNull();
        assertThat(helper.encodePassword(IdType.FILTEREDROLE, asSet(PASSWORD))).isNull();
        assertThat(helper.encodePassword(IdType.GROUP, asSet(PASSWORD))).isNull();
        assertThat(helper.encodePassword(IdType.REALM, asSet(PASSWORD))).isNull();
        assertThat(helper.encodePassword(IdType.ROLE, asSet(PASSWORD))).isNull();
        assertThat(helper.encodePassword(IdType.USER, asSet(PASSWORD))).isNotNull();
    }
    @Test
    public void passwordIsCorrectlyEncodedWithEnclosingDoubleQuotes() throws Exception {
        assertThat(helper.encodePassword(PASSWORD)).isEqualTo(("\"" + PASSWORD + "\"").getBytes(ENCODING));
    }

    @Test
    public void decodedPasswordHasDoubleQuotes() throws Exception {
        assertThat(new String(helper.encodePassword(PASSWORD), ENCODING)).isEqualTo(QUOTED_PASSWORD);
    }

    @Test
    public void passwordSetIsCorrectlyEncoded() throws Exception {
        assertThat(helper.encodePassword(IdType.USER, CollectionUtils.<String>asSet())).isNull();
        assertThat(helper.encodePassword(IdType.USER, asSet(PASSWORD))).isNotNull()
                .isEqualTo(QUOTED_PASSWORD.getBytes(ENCODING));
        assertThat(helper.encodePassword(IdType.USER, asSet(PASSWORD, QUOTED_PASSWORD))).isNotNull()
                .isEqualTo(QUOTED_PASSWORD.getBytes(ENCODING));
    }

    @Test
    public void passwordInByteArrayIsNotModified() throws Exception {
        assertThat(helper.encodePassword(IdType.USER, getPasswordInByteArray(PASSWORD))).isNotNull()
                .isEqualTo(PASSWORD.getBytes("UTF-8"));
    }

    private byte[][] getPasswordInByteArray(String input) throws Exception {
        byte[][] ret = new byte[1][];
        ret[0] = input.getBytes("UTF-8");
        return ret;
    }
}
