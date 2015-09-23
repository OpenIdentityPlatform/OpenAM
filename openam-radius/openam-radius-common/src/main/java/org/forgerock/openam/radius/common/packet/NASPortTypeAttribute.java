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
 * Class representing the structure of the NAS-Port-Type field as specified in section 5.41 of RFC 2865 and able to be
 * instantiated from the on-the-wire bytes or model objects.
 */
public class NASPortTypeAttribute extends Attribute {
    /**
     * The Async type.
     */
    public static final int ASYNC = 0;
    /**
     * The Sync type.
     */
    public static final int SYNC = 1;
    /**
     * The ISDN Sync type.
     */
    public static final int ISDN_SYNC = 2;
    /**
     * The ISDN Async V.120 type.
     */
    public static final int ISDN_ASYNC_V120 = 3;
    /**
     * The ISDN Async V.110 type.
     */
    public static final int ISDN_ASYNC_V110 = 4;
    /**
     * The Virtual type.
     */
    public static final int VIRTUAL = 5;
    /**
     * The PIAFS type.
     */
    public static final int PIAFS = 6;
    /**
     * The HDLC Clear Channel type.
     */
    public static final int HDLC = 7;
    /**
     * The X.25 type.
     */
    public static final int X_25 = 8;
    /**
     * The X.75 type.
     */
    public static final int X_75 = 9;
    /**
     * The G.3 Fax type.
     */
    public static final int G3_FAX = 10;
    /**
     * The SDSL - Symmetric DSL type.
     */
    public static final int SDSL = 11;
    /**
     * The ADSL-CAP - Asymmetric DSL, Carrierless Amplitude Phase Modulation type.
     */
    public static final int ADSL_CAP = 12;
    /**
     * The ADSL-DMT - Asymmetric DSL, Discrete Multi-Tone type.
     */
    public static final int ADSL_DMT = 13;
    /**
     * The IDSL - ISDN Digital Subscriber Line type.
     */
    public static final int IDSL = 14;
    /**
     * The Ethernet type.
     */
    public static final int ETHERNET = 15;
    /**
     * The xDSL - Digital Subscriber Line of unknown type type.
     */
    public static final int XDSL = 16;
    /**
     * The Cable type.
     */
    public static final int CABLE = 17;
    /**
     * The Wireless - Other type.
     */
    public static final int WIRELESS_OTHER = 18;
    /**
     * The Wireless - IEEE 802.11 type.
     */
    public static final int WIRELESS_IEEE_802_11 = 19;

    /**
     * The port type number of the NAS which is authenticating the user.
     */
    private int portType = 0;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public NASPortTypeAttribute(byte[] octets) {
        super(octets);
        portType = OctetUtils.toIntVal(octets);
    }

    /**
     * Constructs a new instance from the port type of the NAS.
     *
     * @param portType the port type of the NAS.
     */
    public NASPortTypeAttribute(int portType) {
        super(OctetUtils.toOctets(AttributeType.NAS_PORT_TYPE, portType));
        this.portType = portType;
    }

    /**
     * Returns the port type of the NAS.
     *
     * @return the port type of the NAS.
     */
    public int getPortType() {
        return portType;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return new StringBuilder().append(portType).toString();
    }
}
