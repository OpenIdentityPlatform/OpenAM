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
 * Legacy class from before enum types were supported and holder of constants of all defined radius attribute type codes
 * outlined in RFC 2865. If support for new types is added we should just be able to add them to the
 * {@link org.forgerock.openam.radius.common.AttributeType} class without adding them here.
 */
public abstract class Attribute {

    /**
     * The maximum number of bytes in the on-the-wire format of an attribute since RFC 2865 gives the length field a
     * single octet in the on-the-wire format.
     */
    public static final int MAX_ATTRIBUTE_LENGTH = 255; // since attribute

    /**
     * Since the on-the-wire format has a single type octet followed by a single length octet the maximum length of an
     * attribute's value is 255 - 2 which is 253.
     */
    public static final int MAX_ATTRIBUTE_VALUE_LENGTH = MAX_ATTRIBUTE_LENGTH - 2;

    /**
     * The type of this attribute instance.
     */
    private AttributeType type = AttributeType.UNKNOWN;

    /**
     * The on-the-wire bytes representing this attribute.
     */
    private byte[] octets;

    /**
     * Constructor using the on-the-wire octets for the attribute.
     *
     * @param octets
     *            the on-the-wire octets
     */
    public Attribute(byte[] octets) {
        this.octets = octets;
        type = AttributeType.getType(octets[0]);
    }

    /**
     * Returns the type code of the attribute.
     *
     * @return code indicative of the type of radius attribute.
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * Sets the array of octets representing the format of a given attribute on-the-wire including prefixed type and
     * length octets as defined in section 3 of RFC 2865. Subclasses should call this method in their constructors for
     * injecting the proper set of octest to represent this attribute instance in the on-the-wire format.
     *
     * @param octets
     *            the octets
     */
    protected void setOctets(byte[] octets) {
        this.octets = octets;
    }

    /**
     * Returns a byte array with on-the-wire attribute format including the two byte prefix consisting of the type octet
     * and length octet defined in section 3 of RFC 2865.
     *
     * @return the on-the-wire byte representation of this attribute including preceding type and length octets.
     */
    public byte[] getOctets() {
        return this.octets;
    }

    /**
     * Shows a String representation of the contents of a given field. Used for logging packet traffic.
     *
     * @return the representation of the attribute when traffic logging is enabled
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();

        if (type == AttributeType.UNKNOWN) {
            s.append("UNKNOWN TYPE : ").append(Utils.toHexAndPrintableChars(this.octets)).toString();
        } else {
            s.append(type.name());
        }
        final String content = this.toStringImpl();

        if (!"".equals(content)) {
            s.append(" : ").append(content);
        }
        return s.toString();
    }

    /**
     * Method expected to be overridden by subclasses to append detail beyond attribute type name. Used by logging to
     * portray attribute field structure when logging packet traffic if special handling is required for a given field
     * distinct from that provided by the super class.
     *
     * @return the string representation of this attribute which is an empty string by default.
     */
    public String toStringImpl() {
        return "";
    }
}
