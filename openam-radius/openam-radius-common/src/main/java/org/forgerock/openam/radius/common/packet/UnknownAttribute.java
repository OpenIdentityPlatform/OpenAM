/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions Copyrighted [2015] [ForgeRock AS]
 * Portions Copyrighted [2015] [Intellectual Reserve, Inc (IRI)]
 */
package org.forgerock.openam.radius.common.packet;

import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;

/**
 * Implements a special version of Attribute for handling attribute types received across the wire but not yet
 * implemented but able to be received and logged.
 */
public class UnknownAttribute extends Attribute {

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public UnknownAttribute(byte[] octets) {
        super(UnknownAttribute.toOctets(octets));
        super.setOctets(octets);
    }

    /**
     * Swaps the type code from the originating octets to the Unknown type so that the super class can find the
     * corresponding type and toString() in that class can generate output that looks like "UNKNOWN TYPE : " followed
     * by the hexadecimal representation of the original unchanged set of octets.
     *
     * @param orig the original octets
     * @return a clone of the same octets but with the type code set to the Unknown value of 0.
     */
    private static final byte[] toOctets(byte[] orig) {
        byte[] bytes = new  byte[orig.length];
        System.arraycopy(orig, 0, bytes, 0, orig.length);
        bytes[0] = (byte) AttributeType.UNKNOWN.getTypeCode();
        return bytes;
    }
}

