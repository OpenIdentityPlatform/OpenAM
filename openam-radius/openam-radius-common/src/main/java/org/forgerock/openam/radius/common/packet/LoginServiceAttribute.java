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
 * Class representing the structure of the Framed-Route attribute as specified in section 5.15 of RFC 2865 and able to
 * be instantiated from the on-the-wire bytes or model objects.
 */
public class LoginServiceAttribute extends Attribute {
    /**
     * Use Telnet.
     */
    public static final int TELNET = 0;
    /**
     * Use Rlogin.
     */
    public static final int RLOGIN = 1;
    /**
     * Use TCP Clear.
     */
    public static final int TCP_CLEAR = 2;
    /**
     * Use PortMaster which is some proprietary protocol.
     */
    public static final int PORTMASTER = 3;
    /**
     * Use LAT.
     */
    public static final int LAT = 4;
    /**
     * Use X25-PAD.
     */
    public static final int X25_PAD = 5;
    /**
     * Use X25_T3POS.
     */
    public static final int X25_T3POS = 6;
    /**
     * Use TCP Clear Quiet (suppresses any NAS-generated connect string).
     */
    public static final int TCP_CLEAR_QUIET = 8;

    /**
     * The service to use to connect the user to the login host.
     */
    private int service = 0;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public LoginServiceAttribute(byte[] octets) {
        super(octets);
        service = OctetUtils.toIntVal(octets);
    }

    /**
     * Constructs a new instance from the service to use to connect the user to the login host.
     *
     * @param service The service to use to connect the user to the login host.
     */
    public LoginServiceAttribute(int service) {
        super(OctetUtils.toOctets(AttributeType.LOGIN_SERVICE, service));
        this.service = service;
    }

    /**
     * Returns the service to use to connect the user to the login host.
     * @return the service to use to connect the user to the login host.
     */
    public int getService() {
        return service;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {

        return new StringBuilder().append(service).toString();
    }
}
