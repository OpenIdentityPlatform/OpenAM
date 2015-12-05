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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common;

import java.util.HashMap;
import java.util.Map;

/**
 * The packet types in RADIUS rfc 2865 and 2866.
 * <p/>
 */
public enum PacketType {

    /**
     * Enum representing the Access-Request packet type specified in section 4.1 of RFC 2865.
     */
    ACCESS_REQUEST(1),

    /**
     * Enum representing the Access-Accept packet type specified in section 4.2 of RFC 2865.
     */
    ACCESS_ACCEPT(2),

    /**
     * Enum representing the Access-Reject packet type specified in section 4.3 of RFC 2865.
     */
    ACCESS_REJECT(3),

    /**
     * Enum representing the Accounting-Request packet type specified in section 4.1 of RFC 2866.
     */
    ACCOUNTING_REQUEST(4),

    /**
     * Enum representing the Accounting-Response packet type specified in section 4.2 of RFC 2866.
     */
    ACCOUNTING_RESPONSE(5),

    /**
     * Enum representing the Access-Challenge packet type specified in section 4.4 of RFC 2865.
     */
    ACCESS_CHALLENGE(11),

    /**
     * Enum representing the reserved packet type specified in section 3 of RFC 2865.
     */
    RESERVED(255),

    /**
     * Enum used to represent unrecognized and hence unsupported packet type codes. This is solely for use in this
     * library and is not representative of any RFC specification.
     */
    UNKNOWN(Integer.MIN_VALUE);

    /**
     * The integer code indicating the type of the packet.
     */
    private int type;

    /**
     * Lookup map by type code.
     */
    private static Map<Integer, PacketType> types;

    /**
     * Create a PacketType associated with the given code from rfc 2865.
     *
     * @param type The on-the-wire integer type code for the packet.
     */
    PacketType(int type) {
        this.type = type;
        addTypeToMap(this);
    }

    /**
     * Builds the lookup map for use in getPacketType static method.
     *
     * @param t Adds an instance of this enum to a map for quickly looking up that enum given its integer type code.
     */
    private void addTypeToMap(PacketType t) {
        if (types == null) {
            types = new HashMap<Integer, PacketType>();
        }
        types.put(t.getTypeCode(), t);
    }

    /**
     * Get the type code for a given PacketType.
     *
     * @return the type code for the packet.
     */
    public int getTypeCode() {
        return type;
    }

    /**
     * Get the PacketType corresponding to a given code from an incoming packet.
     *
     * @param code the integer type code of the packet
     * @return the PacketType representing the corresponding on-the-wire type code or UNKNOWN if the integer code is for
     * a packet type that is not yet supported.
     */
    public static final PacketType getPacketType(int code) {
        final PacketType pt = types.get(code);

        if (pt == null) {
            return UNKNOWN;
        }
        return pt;
    }
}
