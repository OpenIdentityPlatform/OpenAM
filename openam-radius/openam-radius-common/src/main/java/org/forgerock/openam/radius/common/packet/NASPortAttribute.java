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
 * Class representing the structure of the NAS-Port field as specified in section 5.5 of RFC 2865 and able to
 * be instantiated from the on-the-wire bytes or model objects.
 */
public class NASPortAttribute extends Attribute {
    /**
     * The physical port number of the NAS which is authenticating the user.
     */
    private int port = 0;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public NASPortAttribute(byte[] octets) {
        super(octets);
        port = OctetUtils.toIntVal(octets);
    }

    /**
     * Constructs a new instance from the physical port number of the NAS.
     *
     * @param port The port number.
     */
    public NASPortAttribute(int port) {
        super(OctetUtils.toOctets(AttributeType.NAS_PORT, port));
        this.port = port;
    }

    /**
     * Returns the physical port number of the NAS.
     * @return the physical port number of the NAS.
     */
    public int getPort() {
        return port;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return new StringBuilder().append(port).toString();
    }
}
