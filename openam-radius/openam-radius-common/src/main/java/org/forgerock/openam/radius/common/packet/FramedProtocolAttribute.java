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
 * Class representing the structure of the Framed-MTU attribute as specified in section 5.7 of RFC 2865 and
 * able to be instantiated from the on-the-wire bytes or from model objects.
 */
public class FramedProtocolAttribute extends Attribute {
    /**
     * Framing value for specifying PPP framing.
     */
    public static final int PPP = 1;
    /**
     * Framing value for specifying SLIP framing.
     */
    public static final int SLIP = 2;
    /**
     * Framing value for specifying ARAP framing.
     */
    public static final int ARAP = 3;
    /**
     * Framing value for specifying GANDALF framing.
     */
    public static final int GANDALF = 4;
    /**
     * Framing value for specifying XYLOGICS framing.
     */
    public static final int XYLOGICS = 5;
    /**
     * Framing value for specifying X_75 framing.
     */
    public static final int X_75 = 6;

    /**
     * The framing to be used for framed access.
     */
    private int framing = 0;

    /**
     * Constructs an instance from the on-the-wire octets.
     *
     * @param octets the on-the-wire octets
     */
    public FramedProtocolAttribute(byte[] octets) {
        super(octets);
        framing = OctetUtils.toIntVal(octets);
    }

    /**
     * Constructs an instance representing the desired framing.
     *
     * @param framing the framing indicator
     */
    public FramedProtocolAttribute(int framing) {
        super(OctetUtils.toOctets(AttributeType.FRAMED_PROTOCOL, framing));
        this.framing = framing;
    }


    /**
     * Returns the desired framing represented by this instance.
     *
     * @return the framing value
     */
    public int getFraming() {
        return framing;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return new StringBuilder().append(framing).toString();
    }
}
