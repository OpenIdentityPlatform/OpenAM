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

/**
 * Class representing the structure of the User-Name attribute as specified in section 5.1 of RFC 2865 and able to be
 * instantiated from the on-the-wire bytes or model objects.
 */
public class UserNameAttribute extends Attribute {
    /**
     * The username of the user. Also known as login name.
     */
    private String username = null;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets
     *            the on-the-wire bytes from which to construct this instance
     */
    public UserNameAttribute(byte[] octets) {
        super(octets);
        this.username = new String(octets, 2, octets.length - 2);
    }

    /**
     * Constructs an instance with the indicated name embedded.
     *
     * @param name
     *            the username.
     */
    public UserNameAttribute(String name) {
        super(OctetUtils.toOctets(AttributeType.USER_NAME, name));
        // handles truncation if it doesn't fit in the max attribute size. should never happen.
        final byte[] octets = super.getOctets();
        this.username = new String(octets, 2, octets.length - 2);
    }

    /**
     * Get the nested username value.
     *
     * @return the username.
     */
    public String getName() {
        return username;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    @Override
    public String toStringImpl() {
        return username;
    }
}
