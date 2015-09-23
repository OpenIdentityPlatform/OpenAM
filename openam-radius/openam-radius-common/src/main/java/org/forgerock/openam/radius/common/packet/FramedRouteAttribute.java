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
 * Class representing the structure of the Framed-Route attribute as specified in section 5.22 of RFC 2865 and
 * able to be instantiated from the on-the-wire bytes or model objects.
 */
public class FramedRouteAttribute extends Attribute {
    /**
     * The route string.
     */
    private String routeString = null;

    /**
     * Create an instance with the specified route string.
     *
     * @param routeString the route string held by the instance
     */
    public FramedRouteAttribute(String routeString) {
        super(OctetUtils.toOctets(AttributeType.FRAMED_ROUTE, routeString));
        this.routeString = new String(super.getOctets(), 2, super.getOctets().length - 2);
    }

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public FramedRouteAttribute(byte[] octets) {
        super(octets);
        routeString = new String(octets, 2, octets.length - 2);
    }

    /**
     * Return the route string.
     * @return the route string.
     */
    public String getRouteString() {
        return routeString;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return routeString;
    }
}
