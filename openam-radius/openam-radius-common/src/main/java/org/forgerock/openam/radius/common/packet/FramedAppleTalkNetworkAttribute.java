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
 * Class representing the structure of the Framed-AppleTalk-Link attribute as specified in section 5.37 of RFC 2865.
 */
public class FramedAppleTalkNetworkAttribute extends Attribute {
    /**
     * The special link value that indicates an unnumbered link.
     */
    public static final int UN_NUMBERED = 0;

    /**
     * The network number value between 0 and 65535.
     */
    private int networkNumber = 0;

    /**
     * Construct a new instance from the network number it should represent between 0 and 65535 notwithstanding use
     * of an integer. The int type is used since short types in java are signed and hence can't represent an unsigned
     * value of 32786 or greater. A value of 0 indicates that the NAS should assign a network for the user.
     *
     * @param networkNumber the network number that should be between 0 and 65535 inclusive.
     */
    public FramedAppleTalkNetworkAttribute(int networkNumber) {
        super(OctetUtils.toOctets(AttributeType.FRAMED_APPLETALK_NETWORK, networkNumber));
        this.networkNumber = networkNumber;
    }

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public FramedAppleTalkNetworkAttribute(byte[] octets) {
        super(octets);
        networkNumber = OctetUtils.toIntVal(octets);
    }

    /**
     * Returns the apple talk network number between 0 and 65535 to probe to allocate a node for the user.
     *
     * @return the apple talk network number.
     */
    public int getNetworkNumber() {
        return networkNumber;
    }

    /**
     * Indicates if the NAS should assign a network for the user.
     *
     * @return true if the NAS should assign a network for the user.
     */
    public boolean isNasAssigned() {
        return networkNumber == UN_NUMBERED;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {

        return new StringBuilder().append(networkNumber).toString();
    }
}
