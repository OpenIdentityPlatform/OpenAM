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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestUserPasswordAttribute {

    @Test
    public void testEncDec() throws IOException {
        final String spaceHexAuthnctr = "0f 40 3f 94 73 97 80 57 bd 83 d5 cb 98 f4 22 7a"; // from rfc 2865 ex 7.1
        final RequestAuthenticator authnctr = new RequestAuthenticator(Utils.toByteArray(spaceHexAuthnctr));
        final UserPasswordAttribute upa = new UserPasswordAttribute(authnctr, Rfc2865Examples.secret,
                Rfc2865Examples.password);
        final byte[] data = upa.getOctets();
        System.out.println("on-the-wire attribute sequence: " + Utils.toSpacedHex(ByteBuffer.wrap(data)));

        // now reverse and get password back out
        final UserPasswordAttribute upa2 = new UserPasswordAttribute(data);
        final String pwd = upa2.extractPassword(authnctr, Rfc2865Examples.secret);
        Assert.assertEquals(pwd, Rfc2865Examples.password, "passwords should be recoverable");
    }

    @Test
    public void testLongerPwd() throws IOException {
        final String spaceHexAuthnctr = "4d 83 19 3b 80 31 9b b0 43 a5 b6 94 a5 12 43 5b";
        final RequestAuthenticator authnctr = new RequestAuthenticator(Utils.toByteArray(spaceHexAuthnctr));
        final String wireSeq = "02 12 f0 82 69 17 e4 ef 18 e5 44 e1 53 c0 06 b0 43 df"; // depends on client secret and
                                                                                  // authntctr
        final byte[] wireBytes = Utils.toByteArray(wireSeq);
        final String clientSecret = "don't tell";
        final UserPasswordAttribute upa = new UserPasswordAttribute(wireBytes);
        final String pwd = upa.extractPassword(authnctr, clientSecret);
        Assert.assertEquals(pwd, "secret", "passwords should be 'secret'");
    }

    SecureRandom rand = new SecureRandom();
    String secret = "my-secret";

    /**
     * Tests the first boundary incurrence meaning the password length is the same length as the 16 byte hash used for
     * XOR'ing.
     *
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     */
    @Test
    public void test15charPwd() throws NoSuchAlgorithmException, IOException {
        final String pwd15 = "123456789_12345";
        final SecureRandom rand = new SecureRandom();
        final RequestAuthenticator ra = new RequestAuthenticator(rand, secret);
        final UserPasswordAttribute upa = new UserPasswordAttribute(ra, secret, pwd15);
        final byte[] bytes = upa.getOctets();
        final UserPasswordAttribute upa2 = new UserPasswordAttribute(bytes);
        final String pwd = upa2.extractPassword(ra, secret);
        Assert.assertEquals(pwd, pwd15, "15 character password should be the same after decoding.");
    }

    /**
     * Tests the first boundary incurrence meaning the password length is one char more than the 16 byte hash used for
     * XOR'ing.
     *
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     */
    @Test
    public void test16charPwd() throws NoSuchAlgorithmException, IOException {
        final String pwd = "123456789_123456";
        final SecureRandom rand = new SecureRandom();
        final RequestAuthenticator ra = new RequestAuthenticator(rand, secret);
        final UserPasswordAttribute upa = new UserPasswordAttribute(ra, secret, pwd);
        final byte[] bytes = upa.getOctets();
        final UserPasswordAttribute upa2 = new UserPasswordAttribute(bytes);
        final String pwd2 = upa2.extractPassword(ra, secret);
        Assert.assertEquals(pwd2, pwd, "16 character password should be the same after decoding.");
    }

    @Test
    public void test36charPwd() throws NoSuchAlgorithmException, IOException {
        final String pwd = "123456789_123456789_123456";
        final SecureRandom rand = new SecureRandom();
        final RequestAuthenticator ra = new RequestAuthenticator(rand, secret);
        final UserPasswordAttribute upa = new UserPasswordAttribute(ra, secret, pwd);
        final byte[] bytes = upa.getOctets();
        final UserPasswordAttribute upa2 = new UserPasswordAttribute(bytes);
        final String pwd2 = upa2.extractPassword(ra, secret);
        Assert.assertEquals(pwd2, pwd, "36 character password should be the same after decoding.");
    }

    @Test
    public void testMultiByteCharPwd() throws NoSuchAlgorithmException, IOException {
        // my poor attempt at "software architect" in japanese
        final String pwd = "\u30BD\u30D5\u30C8\u30A6\u30A7\u30A2\u5EFA\u7BC9\u5BB6";
        System.out.println("- " + pwd);
        final SecureRandom rand = new SecureRandom();
        final RequestAuthenticator ra = new RequestAuthenticator(rand, secret);
        final UserPasswordAttribute upa = new UserPasswordAttribute(ra, secret, pwd);
        final byte[] bytes = upa.getOctets();
        final UserPasswordAttribute upa2 = new UserPasswordAttribute(bytes);
        final String pwd2 = upa2.extractPassword(ra, secret);
        Assert.assertEquals(pwd2, pwd, "multibyte character password should be the same after decoding.");
    }

}
