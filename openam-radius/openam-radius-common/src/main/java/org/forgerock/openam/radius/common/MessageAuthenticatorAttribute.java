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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.radius.common;

/**
 * Represents the Message-Authenticator attribute (type 80) defined in section 3.2 of RFC 3579
 * (originally introduced by RFC 2869). The value is a 16-octet HMAC-MD5 of the entire RADIUS
 * packet computed with the shared secret as the key, with the value field of this attribute
 * being treated as 16 zero octets during the computation. For responses, the response
 * authenticator field of the packet is replaced by the corresponding request authenticator
 * during the HMAC computation as required by RFC 3579.
 */
public class MessageAuthenticatorAttribute extends Attribute {

    /** The fixed length on the wire (type + length + 16 octets). */
    public static final int WIRE_LENGTH = 18;

    /**
     * Create an attribute initialized with 16 zero octets, suitable for inclusion in an
     * outbound packet prior to HMAC-MD5 computation.
     */
    public MessageAuthenticatorAttribute() {
        super(emptyOctets());
    }

    /**
     * Create an attribute from on-the-wire octets.
     *
     * @param octets the on-the-wire bytes including type and length prefix.
     */
    public MessageAuthenticatorAttribute(byte[] octets) {
        super(octets);
    }

    /**
     * Create an attribute carrying the supplied 16-octet HMAC value.
     *
     * @param hmac the 16-octet HMAC-MD5 value.
     */
    public MessageAuthenticatorAttribute(byte[] hmac, boolean copy) {
        super(buildOctets(hmac));
    }

    private static byte[] emptyOctets() {
        return buildOctets(new byte[16]);
    }

    private static byte[] buildOctets(byte[] hmac) {
        if (hmac == null || hmac.length != 16) {
            throw new IllegalArgumentException("Message-Authenticator value must be 16 octets");
        }
        final byte[] o = new byte[WIRE_LENGTH];
        o[0] = (byte) AttributeType.MESSAGE_AUTHENTICATOR.getTypeCode();
        o[1] = (byte) WIRE_LENGTH;
        System.arraycopy(hmac, 0, o, 2, 16);
        return o;
    }

    /**
     * Returns a copy of the 16-octet HMAC value carried by this attribute.
     *
     * @return the 16-octet HMAC-MD5 value.
     */
    public byte[] getHmac() {
        final byte[] o = getOctets();
        final byte[] v = new byte[16];
        System.arraycopy(o, 2, v, 0, 16);
        return v;
    }

    @Override
    public String toStringImpl() {
        return "*******"; // never log raw HMAC bytes
    }
}

