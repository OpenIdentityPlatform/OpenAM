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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.config.user;

import org.forgerock.openam.sts.AMSTSConstants;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class KeystoreConfigTest {
    @Test
    public void testEquals() throws UnsupportedEncodingException {
        KeystoreConfig kc1 = KeystoreConfig.builder()
                .encryptionKeyAlias("a")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();

        KeystoreConfig kc2 = KeystoreConfig.builder()
                .encryptionKeyAlias("a")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();

        assertEquals(kc1, kc2);
        assertEquals(kc1.hashCode(), kc2.hashCode());
    }

    @Test
    public void testNotEquals() throws UnsupportedEncodingException {
        KeystoreConfig kc1 = KeystoreConfig.builder()
                .encryptionKeyAlias("a")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
        KeystoreConfig kc2 = KeystoreConfig.builder()
                .encryptionKeyAlias("aa")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
        assertNotEquals(kc1, kc2);
        assertNotEquals(kc1.hashCode(), kc2.hashCode());

        KeystoreConfig kc3 = KeystoreConfig.builder()
                .encryptionKeyAlias("a")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();

        KeystoreConfig kc4 = KeystoreConfig.builder()
                .encryptionKeyAlias("a")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("ff".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
        assertNotEquals(kc3, kc4);
        assertNotEquals(kc3.hashCode(), kc4.hashCode());
        assertNotEquals(kc3, "bobo");
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void testNullRejected() throws UnsupportedEncodingException {
        KeystoreConfig.builder()
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
    }

    @Test
    public void testSetting() throws UnsupportedEncodingException {
        KeystoreConfig kc1 = KeystoreConfig.builder()
                .encryptionKeyAlias("a")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();

        assertEquals("b", new String(kc1.getEncryptionKeyPassword()));
        assertEquals("f", new String(kc1.getSignatureKeyPassword()));
    }

    @Test
    public void testJsonRountTrip() throws UnsupportedEncodingException {
        KeystoreConfig kc1 = KeystoreConfig.builder()
                .encryptionKeyAlias("a")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
        assertEquals(kc1, KeystoreConfig.fromJson(kc1.toJson()));
    }

    @Test
    public void testAttributeMappingRoundTrip() throws UnsupportedEncodingException {
        KeystoreConfig kc1 = KeystoreConfig.builder()
                .encryptionKeyAlias("a")
                .encryptionKeyPassword("b".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .fileName("c")
                .password("d".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .signatureKeyAlias("e")
                .signatureKeyPassword("f".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();

        assertEquals(kc1, KeystoreConfig.marshalFromAttributeMap(kc1.marshalToAttributeMap()));

    }
}
