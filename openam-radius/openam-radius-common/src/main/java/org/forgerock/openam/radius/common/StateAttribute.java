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
 * Portions Copyrighted 2011 ForgeRock AS
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common;

import java.nio.charset.Charset;

/**
 * Class representing the structure of the State attribute as specified in section 5.24 of RFC 2865 and able to be
 * instantiated from the on-the-wire bytes.
 */
public class StateAttribute extends Attribute {
    /**
     * The opaque state string.
     */
    private String state = null;

    /**
     * Creates an instance to contain the given string.
     *
     * @param str
     *            the opaque state string.
     */
    public StateAttribute(String str) {
        super(OctetUtils.toOctets(AttributeType.STATE, str));
        state = new String(super.getOctets(), 2, super.getOctets().length - 2, Charset.forName("utf-8"));
    }

    /**
     * Creates a ReplyMessageAttribute from the on-the-wire octets.
     *
     * @param octets
     *            the on-the-wire bytes.
     */
    public StateAttribute(byte[] octets) {
        super(octets);
        state = new String(octets, 2, octets.length - 2, Charset.forName("utf-8"));
    }

    /**
     * Returns the contained state string.
     *
     * @return the contained state string.
     */
    public String getState() {
        return state;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return state;
    }
}
