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
 * Class representing the structure of the Framed-IP-Netmask attribute as specified in section 5.9 of RFC 2865 and
 * able to be instantiated from the on-the-wire bytes.
 */
public class FramedIPNetmaskAttribute extends Attribute {
    /**
     * The net mask.
     */
    private byte[] mask = new byte[4];

    /**
     * Construct an instance from the set of bytes for each address. We can't use a byte[] or it collides with the
     * footprint of the other constructor that uses the raw octets. The ordering is significant. The first parameter
     * is the most significant byte. The last is the least significant byte. So for a mask of 255.255.255.0 we would
     * perform:
     * <pre>
     *    new FramedIPNetmaskAttribute(255,255,255,0);
     * </pre>
     *
     * @param msb the most significant bit of the Framed IPX network address
     * @param msb2 the 2nd most significant byte of the IPX network address
     * @param msb3 the 3rd most significant byte of the IPX network address
     * @param msb4 the 4th most significant bit of hte Framed IPX network address
     */
    public FramedIPNetmaskAttribute(int msb, int msb2, int msb3, int msb4) {
        super(OctetUtils.toOctets(AttributeType.FRAMED_IP_NETMASK, msb, msb2, msb3, msb4));
        this.mask = new FramedIPNetmaskAttribute(super.getOctets()).getMask();
    }

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public FramedIPNetmaskAttribute(byte[] octets) {
        super(octets);
        mask[0] = octets[2];
        mask[1] = octets[3];
        mask[2] = octets[4];
        mask[3] = octets[5];
    }

    /**
     * Get the mask.
     *
     * @return the mask
     */
    public byte[] getMask() {
        return mask;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {

        return new StringBuilder().append(mask[0])
                .append(".").append(mask[1])
                .append(".").append(mask[2])
                .append(".").append(mask[3])
                .toString();
    }
}
