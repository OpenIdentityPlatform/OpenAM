/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AccessAccept.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class representing the User Password Attribute as defined in section 5.2 of RFC 2865. It's use follows one of two
 * patterns. When readying an access request packet to be sent this class is instantiated via by passing the clear text
 * password, request authenticator, and shared secret and the on-the-wire bytes are extracted via the super class's
 * getOctets() method. When receiving an access request packet this class is instantiated with the full on-the-wire
 * octets and the password is then extracted using the extractPassword method passing the received request authenticator
 * and shared secret.
 */
public class UserPasswordAttribute extends Attribute {
    /**
     * Indicate in which direction we are processing since the algorithm is mostly the same save for building the md5
     * hash blocks after the initial one. For encrypting it pulls from the resultant cipher text. For decrypting, the
     * cipher text is what is being decrypted (via XOR'ing again with the same hashes) so the hashes must be generated
     * using the cipher texts so that they are the same else XOR'ing's restorative nature won't work resulting in all
     * characters past the first 16 character block being garbled.
     */
    private static enum Direction {
        /**
         * Indicates we are encrypting the password.
         */
        ENCRYPT,
        /**
         * Indicates that we are decrypting the password.
         */
        DECRYPT;
    }

    /**
     * Instantiates attribute with on-the-wire bytes for decrypting cypher text to restore clear text password.
     *
     * @param octets
     *            the on-the-wire bytes representing the attribute
     */
    public UserPasswordAttribute(byte[] octets) {
        super(octets);
    }

    /**
     * Instantiates attribute with clear text password for creation of cipher text.
     *
     * @param ra
     *            the Request Authenticator used for the encrypting the first 16 characters of the password.
     * @param secret
     *            the shared secret between the client and the server used for the creation of the cipher text.
     * @param password
     *            The plain text password.
     */
    public UserPasswordAttribute(Authenticator ra, String secret, String password) {
        super(UserPasswordAttribute.toOctets(ra, secret, password));
    }

    /**
     * Custom toOctets implementation that takes a clear text password, shared secret, and authenticator and prepares
     * the on-the-wire otets for passing to the super class constructor.
     *
     * @param ra
     *            the request authenticator
     * @param secret
     *            the shared secret
     * @param password
     *            the clear text password
     * @return the cipher text of the encrypted password
     */
    private static final byte[] toOctets(Authenticator ra, String secret, String password) {
        byte[] bytes;
        try {
            bytes = convert(password.getBytes(StandardCharsets.UTF_8), Direction.ENCRYPT, secret, ra);
        } catch (final IOException e) {
            return new byte[] { (byte) AttributeType.USER_PASSWORD.getTypeCode(), 2 }; // empty string password
        }
        final byte[] octets = new byte[bytes.length + 2];
        octets[0] = (byte) AttributeType.USER_PASSWORD.getTypeCode();
        octets[1] = (byte) octets.length;
        System.arraycopy(bytes, 0, octets, 2, bytes.length);
        return octets;
    }

    /**
     * Converts the plain text password to cipher text, or cipher text to plain text according following the algorithm
     * specified in section 5.2 of RFC 2865.
     *
     * @param value
     *            the cipher text or plain text of the password depending on the direction value
     * @param direction
     *            which direction operation will take place either ENCRYPT or DECRYPT
     * @param secret
     *            the secret shared between client and server
     * @param ra
     *            the authenticator instance
     * @return the cipher text or plain text of the password depending on direction
     * @throws IOException
     *             upon invalid Request Authenticator object or missing MD5 implementation
     */
    private static final byte[] convert(byte[] value, Direction direction, String secret, Authenticator ra)
            throws IOException {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        }
        md5.update(secret.getBytes(StandardCharsets.UTF_8));
        md5.update(ra.getOctets());
        byte[] sum = md5.digest();

        final byte[] up = value;
        int oglen = (up.length / 16);

        // increase number of blocks in output array if we don't have a multiple of 16 bytes in value
        if (up.length % 16 != 0) {
            oglen = oglen + 1;
        }
        final byte[] ret = new byte[oglen * 16];
        for (int i = 0; i < ret.length; i++) {
            if ((i % 16) == 0) {
                md5.reset();
                md5.update(secret.getBytes(StandardCharsets.UTF_8));
            }
            if (i < up.length) {
                ret[i] = (byte) (sum[i % 16] ^ up[i]);
            } else {
                ret[i] = (byte) (sum[i % 16] ^ 0);
            }

            // always use the cipher bytes for updating the md5 hashes otherwise all blocks after the first will be
            // garbled upon decrypting rendering all passwords greater than 16 characters useless since they will be
            // incorrect.
            if (direction == Direction.ENCRYPT) {
                md5.update(ret[i]);
            } else {
                md5.update(up[i]);
            }
            if ((i % 16) == 15) {
                sum = md5.digest();
            }
        }
        return ret;
    }

    /**
     * Extracts the plain text password from the cipher text for instances created from on-the-wire octets.
     *
     * @param a
     *            pseudo-random Request Authenticator
     * @param secret
     *            the shared secret between the client and server used for cipher text generation.
     * @return Clear text password
     * @throws IOException
     *             if unable to correctly determine secret, authenticator, or if invalid values are given for cipher
     *             text.
     */
    public String extractPassword(Authenticator a, String secret) throws IOException {
        // trim off sign extension bits, and subtract type and length prefix
        final int valLen = ((super.getOctets()[1]) & 0xFF) - 2;
        // octets
        final byte[] cipherText = new byte[valLen];
        System.arraycopy(super.getOctets(), 2, cipherText, 0, valLen);
        final byte[] clearText = UserPasswordAttribute.convert(cipherText, Direction.DECRYPT, secret, a);

        // trim off any null padding
        int i = 0;
        for (; i < clearText.length; i++) {
            if (clearText[i] == 0) {
                break;
            }
        }
        return new String(clearText, 0, i, StandardCharsets.UTF_8);
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    @Override
    public String toStringImpl() {
        return "*******"; // don't dump password to logs
    }
}
