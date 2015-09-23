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
 * Class representing the structure of the Framed-IPX Address attribute as specified in section 5.24 of RFC 2865.
 */
public class FramedIPXNetworkAttribute extends Attribute {
    /**
     * The IPX Network number.
     */
    private byte [] net = new byte[4];

    /**
     * Constructs an instance of the specified type from the on-the-wire bytes.
     *
     * @param octets the FramedIPX network packet
     */
    public FramedIPXNetworkAttribute(byte[] octets) {

        super(octets);
        net[0] = octets[2];
        net[1] = octets[3];
        net[2] = octets[4];
        net[3] = octets[5];

    }

    /**
     * Constructs an instance from its address model.
     *
     * @param msb the most significant bit of the Framed IPX network address
     * @param msb2 the 2nd most significant byte of the IPX network address
     * @param msb3 the 3rd most significant byte of the IPX network address
     * @param msb4 the 4th most significant bit of hte Framed IPX network address
     */
    public FramedIPXNetworkAttribute(int msb, int msb2, int msb3, int msb4) {
        super(OctetUtils.toOctets(AttributeType.FRAMED_IPX_NETWORK, msb, msb2, msb3, msb4));
        this.net = new FramedIPXNetworkAttribute(super.getOctets()).getIPXNetworkAddress();
    }

    /**
     * Returns the IPX network address.
     *
     * @return the IPX network address
     */
    public byte[] getIPXNetworkAddress() {
        return net;
    }
}
