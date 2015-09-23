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
 * Class representing the structure of the Framed-Route attribute as specified in section 5.35 of RFC 2865 and able to
 * be instantiated from the on-the-wire bytes or model objects.
 */
public class LoginLATNodeAttribute extends Attribute {
    /**
     * The identity of the LAT Node to connect the user to.
     */
    private String latNode = null;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public LoginLATNodeAttribute(byte[] octets) {
        super(octets);
        latNode = new String(octets, 2, octets.length - 2);
    }

    /**
     * Create an instance with the specified LAT node identity.
     *
     * @param latNode the LAT group codes held by the instance
     */
    public LoginLATNodeAttribute(String latNode) {
        super(OctetUtils.toOctets(AttributeType.LOGIN_LAT_NODE, latNode));
        this.latNode = new String(super.getOctets(), 2, super.getOctets().length - 2);
    }

    /**
     * Returns the identity of the LAT Node to connect the user to.
     * @return The identity of the LAT Node to connect the user to.
     */
    public String getLatNode() {
        return latNode;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return latNode;
    }
}
