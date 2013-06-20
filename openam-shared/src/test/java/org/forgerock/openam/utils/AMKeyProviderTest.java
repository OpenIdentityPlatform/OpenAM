/*
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */

package org.forgerock.openam.utils;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.security.EncodeAction;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.security.AccessController;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;


public class AMKeyProviderTest {

    private static final String KEY_STORE_FILE = ClassLoader.getSystemResource("keystore.jks").getFile();
    private static final String KEY_STORE_TYPE = "JKS";
    private static final String KEY_STORE_PASS = "testcase";
    private static final String DEFAULT_PRIVATE_KEY_PASS = "testcase";
    private static final String PRIVATE_KEY_PASS = "keypass";
    private static final String DEFAULT_PRIVATE_KEY_ALIAS = "defaultkey";
    private static final String PRIVATE_KEY_ALIAS = "privatekey";

    KeyProvider amKeyProvider;

    @BeforeClass
    public void setUp() {

        amKeyProvider =
                new AMKeyProvider(true, KEY_STORE_FILE, KEY_STORE_PASS, KEY_STORE_TYPE, DEFAULT_PRIVATE_KEY_PASS);
    }

    @Test
    public void getDefaultPublicKey() {

        PublicKey key = amKeyProvider.getPublicKey(DEFAULT_PRIVATE_KEY_ALIAS);
        Assert.assertNotNull(key);
    }

    @Test
    public void getDefaultX509Certificate() {

        X509Certificate certificate = amKeyProvider.getX509Certificate(DEFAULT_PRIVATE_KEY_ALIAS);
        Assert.assertNotNull(certificate);
    }

    @Test
    public void getPublicKey() {

        PublicKey key = amKeyProvider.getPublicKey(PRIVATE_KEY_ALIAS);
        Assert.assertNotNull(key);
    }

    @Test
    public void getX509Certificate() {

        X509Certificate certificate = amKeyProvider.getX509Certificate(PRIVATE_KEY_ALIAS);
        Assert.assertNotNull(certificate);
    }

    @Test
    public void getDefaultPrivateKeyUsingDefaultPassword() {

        PrivateKey key = amKeyProvider.getPrivateKey(DEFAULT_PRIVATE_KEY_ALIAS);
        Assert.assertNotNull(key);
    }

    @Test
    public void getPrivateKeyUsingProvidedPassword() {

        String encodedPrivatePass = AccessController.doPrivileged(new EncodeAction(PRIVATE_KEY_PASS));

        PrivateKey key = amKeyProvider.getPrivateKey(PRIVATE_KEY_ALIAS, encodedPrivatePass);
        Assert.assertNotNull(key);
    }

    @Test
    public void getPrivateKeyUsingNullPassword() {

        // Trying to get a private key with its own password and passing null should return null
        PrivateKey key = amKeyProvider.getPrivateKey(PRIVATE_KEY_ALIAS, null);
        Assert.assertNull(key);
    }

    @Test
    public void getPrivateKeyUsingDefaultPassword() {

        // Trying to get a private key with its own password will make use of the default password and should return null
        PrivateKey key = amKeyProvider.getPrivateKey(PRIVATE_KEY_ALIAS);
        Assert.assertNull(key);
    }
}
