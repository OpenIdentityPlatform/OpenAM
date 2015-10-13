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
 * Portions Copyrighted [2011] [ForgeRock AS]
 * Portions Copyrighted [2015] [Intellectual Reserve, Inc (IRI)]
 */
package org.forgerock.openam.radius.common.packet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;

/**
 * Class representing the structure of the CHAP-Password attribute as specified in section 5.3 of RFC 2865 and able to
 * be instantiated from the on-the-wire bytes. This attribute is only included in access-requests.
 */
public class CHAPPasswordAttribute extends Attribute {
    /**
     * The chap identifier code from the user's CHAP response.
     */
    private int ident = 0;

    /**
     * The response value provided by a PPP Challenge-Handshake Authentication Protocol (CHAP) user in response to the
     * challenge.
     */
    private String password = null;

    /**
     * Construct a new instance from the password string and chap identifier code from the CHAP response. RFC 2865 isn't
     * very clear on the "password" value other than saying it is  16 octets in length. For clarity we must read RFC
     * 1994 section 4.1 that indicates the CHAP-Password hold a 16 byte hash value created as noted in the RFC.
     * Therefore, we ensure that the resulting bytes are 16 in length and if less we pad them with 0 values.
     * Additionally, the integer value should be less than 256 since it gets truncated to a single octet.
     *
     * @param password   the CHAP response from the user
     * @param identifier the CHAP identifier
     */
    public CHAPPasswordAttribute(String password, int identifier) {
        super(CHAPPasswordAttribute.toOctets(password, identifier));
        // ensure password too long gets trimmed
        this.password = new String(super.getOctets(), 3, super.getOctets().length - 3);
        this.ident = identifier;
    }

    /**
     * Custom implementation of toOctets.
     *
     * @param password   the CHAP response from the user
     * @param identifier the CHAP identifier
     * @return the octets for the on-the-wire bytes
     */
    private static final byte[] toOctets(String password, int identifier) {
        byte[] octets = new byte[19];
        byte[] s = password.getBytes(StandardCharsets.UTF_8);

        // this is not part of rfc 2865 but added to for consistency rather than leaving random values in the unused
        // portion of the array and to prevent an array index out of bounds exception
        if (s.length < 16) {
            byte[] s2 = new byte[16];
            System.arraycopy(s, 0, s2, 0, s.length);
            for (int i = s.length; i < 16; i++) {
                s2[i] = 0;
            }
            s = s2;
        }
        octets[0] = (byte) AttributeType.CHAP_PASSWORD.getTypeCode();
        octets[1] = 19;
        octets[2] = (byte) identifier;
        System.arraycopy(s, 0, octets, 3, 16);
        return octets;
    }

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public CHAPPasswordAttribute(byte[] octets) {
        super(octets);
        ident = octets[2];
        password = new String(octets, 3, 16, Charset.forName("utf-8"));
    }

    /**
     * Returns the chap identifier code from the user's CHAP response.
     *
     * @return the identifier
     */
    public int getIdentifier() {
        return ident;
    }

    /**
     * Returns the CHAP password.
     *
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return new StringBuilder().append(ident).append(", *******").toString(); // we don't log passwords
    }
}
