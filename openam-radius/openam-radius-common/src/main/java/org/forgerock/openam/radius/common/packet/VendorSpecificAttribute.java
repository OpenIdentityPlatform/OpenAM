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

import java.nio.charset.StandardCharsets;

import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.OctetUtils;

/**
 * Class representing the structure of the Vendor-Specific attribute as specified in section 5.26 of RFC 2865 and able
 * to be instantiated from the on-the-wire bytes. See
 * http://www.iana.org/assignments/enterprise-numbers/enterprise-numbers
 * for obtaining the Vendor's enterprise code that is used as the identifier for this attribute.
 */
public class VendorSpecificAttribute extends Attribute {
    /**
     * The SMI Network Management Private Enterprise Code of the Vendor. See
     * http://www.iana.org/assignments/enterprise-numbers/enterprise-numbers.
     */
    private int id = 0; // zero is reserved and does not indicate a vendor number
    /**
     * A site or application specific String value.
     */
    private String str = null;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public VendorSpecificAttribute(byte[] octets) {
        super(octets);
        id = OctetUtils.toIntVal(octets);
        str = new String(octets, 6, octets.length - 6, StandardCharsets.UTF_8);
    }

    /**
     * Constructs an instance to hold the vendor identifier and nested string.
     *
     * @param identifier SMI Network Management Private Enterprise Code.
     * @param text the textual content.
     */
    public VendorSpecificAttribute(int identifier, String text) {
        super(VendorSpecificAttribute.toOctets(identifier, text));
        id = identifier;
        str = new String(super.getOctets(), 6, super.getOctets().length - 6, StandardCharsets.UTF_8);
    }

    /**
     * Custom implementation of toOctets to ensure a string value too long will be truncated properly in view of the
     * space needed by the type, length, and identifier.
     *
     * @param identifier the vendor identifier
     * @param text       the value of the attribute
     * @return the octets for the on-the-wire bytes
     */
    private static final byte[] toOctets(int identifier, String text) {
        byte[] s = text.getBytes(StandardCharsets.UTF_8);
        // use utility code to make it easier for us and copy in those pieces
        byte[] base = OctetUtils.toOctets(AttributeType.VENDOR_SPECIFIC, identifier);
        int maxTextSpace = Attribute.MAX_ATTRIBUTE_LENGTH - base.length;
        byte[] octets;

        if (s.length > maxTextSpace) {
            octets = new byte[Attribute.MAX_ATTRIBUTE_LENGTH];
            System.arraycopy(s, 0, octets, base.length, Attribute.MAX_ATTRIBUTE_LENGTH - base.length);
        } else {
            octets = new byte[base.length + s.length];
            System.arraycopy(s, 0, octets, base.length, s.length);
        }
        System.arraycopy(base, 0, octets, 0, base.length);
        // now adjust length appropriately
        octets[1] = (byte) octets.length;
        return octets;
    }

    /**
     * Get the The SMI Network Management Private Enterprise Code of the Vendor.
     * @return the enterprise code
     */
    public int getId() {
        return id;
    }

    /**
     * The vendor specific value text.
     * @return the text value.
     */
    public String getString() {
        return str;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return new StringBuilder().append(id).append(", ").append(str).toString();
    }
}
