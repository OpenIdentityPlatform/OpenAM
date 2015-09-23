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
 * Class representing the structure of the Class attribute as specified in section 5.25 of RFC 2865 and able to be
 * instantiated from the on-the-wire bytes or model objects.
 */
public class NASClassAttribute extends Attribute {
    /**
     * An opaque string value that can be passed to the client and from thence to the accounting server if accounting is
     * supported.
     */
    private String theClass = null;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public NASClassAttribute(byte[] octets) {
        super(octets);
        theClass = new String(octets, 2, octets.length - 2);
    }

    /**
     * Create an instance with the opaque string that the client can pass to the accounting server if supported.
     *
     * @param theClass the opaque string that the client can pass to the accounting server if supported.
     */
    public NASClassAttribute(String theClass) {
        super(OctetUtils.toOctets(AttributeType.NAS_CLASS, theClass));
        this.theClass = new String(super.getOctets(), 2, super.getOctets().length - 2);
    }

    /**
     * Returns the client can pass to the accounting server if supported.
     *
     * @return the client can pass to the accounting server if supported.
     */
    public String getTheClass() {
        return theClass;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return theClass;
    }
}
