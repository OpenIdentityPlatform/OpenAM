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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * The Request Authenticator implementation as outlined in section 3 of RFC 2865. This class is used in both radius
 * client functionality such as the RADIUS authentication module and in radius server functionality when receiving
 * access-requests and then sending responses.
 */
public class RequestAuthenticator implements Authenticator {
    /**
     * The on-the-wire representation of this authenticator.
     */
    private byte[] octets = null;

    /**
     * Generates a request authenticator field consisting of a 16 octet random number per section 3 of RFC 2865.
     *
     * @param rand   in instance of SecureRandom as the source of randomness.
     * @param secret the secret shared between a radius client and server.
     * @throws NoSuchAlgorithmException if the MD5 algorithm is not available.
     * @throws UnsupportedEncodingException 
     */
    public RequestAuthenticator(SecureRandom rand, String secret)
            throws NoSuchAlgorithmException {
        final byte[] authenticator = new byte[16];
        rand.nextBytes(authenticator);

        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(authenticator);
        md5.update(secret.getBytes(StandardCharsets.UTF_8));
        octets = md5.digest();
    }

    /**
     * Generates a request authenticator field consisting of the 16 octets from the on-the-wire form of a packet.
     *
     * @param octets the on-the-wire bytes.
     */
    public RequestAuthenticator(byte[] octets) {
        this.octets = octets;
    }

    /**
     * Returns the contained on-the-wire bytes of the authenticator.
     *
     * @return the on-the-wire representation of this authenticator.
     */
    @Override
    public byte[] getOctets() {
        return octets;
    }
}
