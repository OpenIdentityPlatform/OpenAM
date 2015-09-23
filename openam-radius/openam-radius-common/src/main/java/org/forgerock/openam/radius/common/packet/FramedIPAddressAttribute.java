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

import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.OctetUtils;

/**
 * Class representing the structure of the Framed-IP-Address attribute as specified in section 5.8 of RFC 2865.
 */
public class FramedIPAddressAttribute extends Attribute {
    /**
     * Indicator of types of FramedIPAddressAttribute to be instantiated.
     */
    public static enum Type {
        /**
         * Indicates if the NAS should allow the user to select an address.
         */
        USER_NEGOTIATED,
        /**
         * Indicates if the NAS should select an address for the user.
         */
        NAS_ASSIGNED,
        /**
         * Indicates that the NAS should use that value as the user's IP address.
         */
        SPECIFIED;
    }

    /**
     * The ip address bytes.
     */
    private byte[] addr = new byte[4];

    /**
     * Constructs an instance of the specified type. For NAS_ASSIGNED and USER_NEGOTIATED other parameters are ignored.
     *
     * @param type the type of instance to create.
     * @param msb the most significant byte of the network address for the specified type
     * @param msb2 the 2nd most significant byte of the network address for the specified type
     * @param msb3 the 3rd most significant byte of the network address for the specified type
     * @param msb4 the 4th most significant byte of the network address for the specified type
     */
    public FramedIPAddressAttribute(Type type, int msb, int msb2, int msb3, int msb4) {
        super(FramedIPAddressAttribute.toOctets(type, msb, msb2, msb3, msb4));
        byte[] octets = super.getOctets();
        addr[0] = octets[2];
        addr[1] = octets[3];
        addr[2] = octets[4];
        addr[3] = octets[5];
    }

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public FramedIPAddressAttribute(byte[] octets) {
        super(octets);
        addr[0] = octets[2];
        addr[1] = octets[3];
        addr[2] = octets[4];
        addr[3] = octets[5];
    }

    /**
     * Custom implementation of toOctets.
     *
     * @param type
     * @param msb the most significant byte of the network address for the specified type
     * @param msb2 the 2nd most significant byte of the network address for the specified type
     * @param msb3 the 3rd most significant byte of the network address for the specified type
     * @param msb4 the 4th most significant byte of the network address for the specified type
     * @return The on-the-wire octets for this attribute.
     */
    private static final byte[] toOctets(Type type, int msb, int msb2, int msb3, int msb4) {
        if (type == Type.NAS_ASSIGNED) {
            return OctetUtils.toOctets(AttributeType.FRAMED_IP_ADDRESS, 255, 255, 255, 254);
        } else if (type == Type.USER_NEGOTIATED) {
            return OctetUtils.toOctets(AttributeType.FRAMED_IP_ADDRESS, 255, 255, 255, 255);
        } else { // is SPECIFIED
            return OctetUtils.toOctets(AttributeType.FRAMED_IP_ADDRESS, msb, msb2, msb3, msb4);
        }
    }

    /**
     * Indicates if the NAS should allow the user to select an address.
     *
     * @return true if the NAS should allow the user to select
     */
    public boolean isUserNegotiated() {
        return (addr[0] == (byte) 255)
                && (addr[1] == (byte) 255)
                && (addr[2] == (byte) 255)
                && (addr[3] == (byte) 255);
    }

    /**
     * Indicates if the NAS should select an address for the user.
     *
     * @return true if the NAS should select the address
     */
    public boolean isNasSelected() {
        /*
        RFC is not clear on how address bytes should be ordered relative to the value indicator. However, javadoc for
         java's InetAddress class, getAddress() method indicates that network byte order is used and hence the
         highest order byte (the left most byte, 192, of a textual representation such as 192.168.10.20) is found in
         getAddress()[0]. Hence the implementation here for testing for 0xFFFFFFFE.
         */
        return (addr[0] == (byte) 255)
                && (addr[1] == (byte) 255)
                && (addr[2] == (byte) 255)
                && (addr[3] == (byte) 254);
    }

    /**
     * Indicates if the NAS should use the ip address indicated in this instance.
     *
     * @return true if the NAS should use the ip address in this instance.
     */
    public boolean isSpecified() {
        return (!this.isNasSelected()) && (!this.isUserNegotiated());
    }

    /**
     * Returns the Ip address.
     * @return the Ip address.
     */
    public byte[] getAddress() {
        return addr;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {

        return new StringBuilder().append(addr[0])
                .append(".").append(addr[1])
                .append(".").append(addr[2])
                .append(".").append(addr[3])
                .toString();
    }
}
