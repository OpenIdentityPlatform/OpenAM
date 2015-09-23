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

import java.util.Map;
import java.util.TreeMap;

/**
 * Attribute types corresponding to attribute type codes defined in rfc 2865 and extension RFCs.
 */
public enum AttributeType {
    /**
     * type value not specified by RFC 2865 but included for initializing variables of this type and for handling of
     * type codes received across the wire for unknown types.
     */
    UNKNOWN(0),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the User-Name field as
     * specified in section 5.1 of RFC 2865.
     */
    USER_NAME(1),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the User-Password field as
     * specified in section 5.2 of RFC 2865.
     */
    USER_PASSWORD(2),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the CHAP-Password field as
     * specified in section 5.3 of RFC 2865.
     */
    CHAP_PASSWORD(3),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the NAS-IP-Address field as
     * specified in section 5.4 of RFC 2865.
     */
    NAS_IP_ADDRESS(4),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the NAS-Port field as
     * specified in section 5.5 of RFC 2865.
     */
    NAS_PORT(5),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Service-Type field as
     * specified in section 5.6 of RFC 2865.
     */
    SERVICE_TYPE(6),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-Protocol field as
     * specified in section 5.7 of RFC 2865.
     */
    FRAMED_PROTOCOL(7),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-IP-Address field as
     * specified in section 5.8 of RFC 2865.
     */
    FRAMED_IP_ADDRESS(8),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-IP-Netmask field as
     * specified in section 5.9 of RFC 2865.
     */
    FRAMED_IP_NETMASK(9),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-Routing field as
     * specified in section 5.10 of RFC 2865.
     */
    FRAMED_ROUTING(10),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Filter-Id field as
     * specified in section 5.11 of RFC 2865.
     */
    FILTER_ID(11),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-MTU field as
     * specified in section 5.12 of RFC 2865.
     */
    FRAMED_MTU(12),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-Compression field as
     * specified in section 5.13 of RFC 2865.
     */
    FRAMED_COMPRESSION(13),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Login-IP-Host field as
     * specified in section 5.14 of RFC 2865.
     */
    LOGIN_IP_HOST(14),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Login-Service field as
     * specified in section 5.15 of RFC 2865.
     */
    LOGIN_SERVICE(15),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Login-TCP-Port field as
     * specified in section 5.16 of RFC 2865.
     */
    LOGIN_TCP_PORT(16),

    // 17 HAS NOT BEEN ASSIGNED

    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Reply-Message field as
     * specified in section 5.18 of RFC 2865.
     */
    REPLY_MESSAGE(18),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Callback-Number field as
     * specified in section 5.19 of RFC 2865.
     */
    CALLBACK_NUMBER(19),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Callback-Id field as
     * specified in section 5.20 of RFC 2865.
     */
    CALLBACK_ID(20),

    // 21 HAS NOT BEEN ASSIGNED

    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-Route field as
     * specified in section 5.22 of RFC 2865.
     */
    FRAMED_ROUTE(22),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-IPX-Network field as
     * specified in section 5.23 of RFC 2865.
     */
    FRAMED_IPX_NETWORK(23),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the State field as
     * specified in section 5.24 of RFC 2865.
     */
    STATE(24),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Class field as
     * specified in section 5.25 of RFC 2865.
     */
    NAS_CLASS(25),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Vendor-Specific field as
     * specified in section 5.26 of RFC 2865.
     */
    VENDOR_SPECIFIC(26),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Session-Timeout field as
     * specified in section 5.27 of RFC 2865.
     */
    SESSION_TIMEOUT(27),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Idle-Timeout field as
     * specified in section 5.28 of RFC 2865.
     */
    IDLE_TIMEOUT(28),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Termination-Action field as
     * specified in section 5.29 of RFC 2865.
     */
    TERMINATION_ACTION(29),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Called-Station-Id field as
     * specified in section 5.30 of RFC 2865.
     */
    CALLER_STATION_ID(30),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Calling-Station-Id field as
     * specified in section 5.31 of RFC 2865.
     */
    CALLING_STATION_ID(31),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the NAS-Identifier field as
     * specified in section 5.32 of RFC 2865.
     */
    NAS_IDENTIFIER(32),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Proxy-State field as
     * specified in section 5.33 of RFC 2865.
     */
    PROXY_STATE(33),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Login-LAT-Service field as
     * specified in section 5.34 of RFC 2865.
     */
    LOGIN_LAT_SERVICE(34),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Login-LAT-Node field as
     * specified in section 5.35 of RFC 2865.
     */
    LOGIN_LAT_NODE(35),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Login-LAT-Group field as
     * specified in section 5.36 of RFC 2865.
     */
    LOGIN_LAT_GROUP(36),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-AppleTalk-Link
     * field as specified in section 5.37 of RFC 2865.
     */
    FRAMED_APPLETALK_LINK(37),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-AppleTalk-Network
     * field as specified in section 5.38 of RFC 2865.
     */
    FRAMED_APPLETALK_NETWORK(38),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Framed-AppleTalk-Zone field
     * as specified in section 5.39 of RFC 2865.
     */
    FRAMED_APPLETALK_ZONE(39),

    // 40-59 HAS NOT BEEN ASSIGNED

    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the CHAP-Challenge field as
     * specified in section 5.40 of RFC 2865.
     */
    CHAP_CHALLENGE(60),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the NAS-Port-Type field as
     * specified in section 5.41 of RFC 2865.
     */
    NAS_PORT_TYPE(61),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Port-Limit field as
     * specified in section 5.42 of RFC 2865.
     */
    PORT_LIMIT(62),
    /**
     * The byte value for the 'type' field of the wire protocol indicating the field is the Login-LAT-Port field as
     * specified in section 5.43 of RFC 2865.
     */
    LOGIN_LAT_PORT(63);

    private static Map<Integer, AttributeType> atts;

    /**
     * The attribute type code from rfc 2865 et al.
     */
    private final int typeCode;

    /**
     * Constructs an instance of AttributeType for a given field type.
     * @param typeCode
     */
    AttributeType(int typeCode) {
        this.typeCode = typeCode;
        addToIndex(this);
    }

    /**
     * Adds the specified instance to the index for obtaining an instance for a given type code.
     *
     * @param att
     */
    private void addToIndex(AttributeType att) {
        if (atts == null) {
            atts = new TreeMap<Integer, AttributeType>();
        }
        atts.put(att.typeCode, att);
    }

    /**
     * Returns the AttributeType instance for a given field type code.
     *
     * @param typeCode the type code for this attribute type
     * @return returns the Attributetype instance for the given type code or null if not found or supported.
     */
    public static final AttributeType getType(int typeCode) {
        return atts.get(typeCode);
    }

    /**
     * Get the attribute type code as defined in rfc 2865 or extension RFCs.
     *
     * @return the type code for this attribute type.
     */
    public int getTypeCode() {
        return typeCode;
    }

}
