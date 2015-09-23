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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;

/**
 * Class representing the structure of the NAS-IP-Address attribute as specified in section 5.4 of RFC 2865 and able to
 * be instantiated from the on-the-wire bytes or model objects.
 */
public class NASIPAddressAttribute extends Attribute {
    /**
     * The identifying IP Address of the NAS which is requesting authentication of the user.
     */
    private InetAddress ip = null;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public NASIPAddressAttribute(byte[] octets) {
        super(octets);
        // fix lack of ip being filled in ip when instantiated from on-the-wire bits
        byte[] addr = new byte[octets.length - 2];
        System.arraycopy(octets, 2, addr, 0, addr.length);
        try {
            ip = InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            e.printStackTrace(); // TODO
        }
    }

    /**
     * Custom implementation of toOctets.
     *
     * @param ip the IP address of the NAS originating the request.
     * @return the on-the-wire octets representing this attribute instance.
     */
    private static final byte[] toOctets(InetAddress ip) {
        byte[] octets = new byte[6];
        octets[0] = (byte) AttributeType.NAS_IP_ADDRESS.getTypeCode();
        octets[1] = 6;
        byte[] addr = ip.getAddress();
        octets[2] = addr[0];
        octets[3] = addr[1];
        octets[4] = addr[2];
        octets[5] = addr[3];
        return octets;
    }

    /**
     * Construct an instance from the IP address of the NAS originating the request.
     *
     * @param ip the IP address of the NAS originating the request.
     */
    public NASIPAddressAttribute(InetAddress ip) {
        super(NASIPAddressAttribute.toOctets(ip));
        this.ip = ip;
    }

    /**
     * Returns the IP address of the NAS originating the request.
     *
     * @return the IP address of the NAS originating the request.
     */
    public InetAddress getIpAddress() {
        return ip;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return ip.toString();
    }
}
