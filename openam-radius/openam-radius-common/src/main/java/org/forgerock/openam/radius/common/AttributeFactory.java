/*
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
 * Portions Copyrighted 2011 ForgeRock AS.
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common;

import org.forgerock.openam.radius.common.packet.CHAPChallengeAttribute;
import org.forgerock.openam.radius.common.packet.CHAPPasswordAttribute;
import org.forgerock.openam.radius.common.packet.CallbackIdAttribute;
import org.forgerock.openam.radius.common.packet.CallbackNumberAttribute;
import org.forgerock.openam.radius.common.packet.CallerStationIdAttribute;
import org.forgerock.openam.radius.common.packet.CallingStationIdAttribute;
import org.forgerock.openam.radius.common.packet.FilterIdAttribute;
import org.forgerock.openam.radius.common.packet.FramedAppleTalkLinkAttribute;
import org.forgerock.openam.radius.common.packet.FramedAppleTalkNetworkAttribute;
import org.forgerock.openam.radius.common.packet.FramedAppleTalkZoneAttribute;
import org.forgerock.openam.radius.common.packet.FramedCompressionAttribute;
import org.forgerock.openam.radius.common.packet.FramedIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.FramedIPNetmaskAttribute;
import org.forgerock.openam.radius.common.packet.FramedIPXNetworkAttribute;
import org.forgerock.openam.radius.common.packet.FramedMTUAttribute;
import org.forgerock.openam.radius.common.packet.FramedProtocolAttribute;
import org.forgerock.openam.radius.common.packet.FramedRouteAttribute;
import org.forgerock.openam.radius.common.packet.FramedRoutingAttribute;
import org.forgerock.openam.radius.common.packet.IdleTimeoutAttribute;
import org.forgerock.openam.radius.common.packet.LoginIPHostAttribute;
import org.forgerock.openam.radius.common.packet.LoginLATGroupAttribute;
import org.forgerock.openam.radius.common.packet.LoginLATNodeAttribute;
import org.forgerock.openam.radius.common.packet.LoginLATPortAttribute;
import org.forgerock.openam.radius.common.packet.LoginLATServiceAttribute;
import org.forgerock.openam.radius.common.packet.LoginServiceAttribute;
import org.forgerock.openam.radius.common.packet.LoginTCPPortAttribute;
import org.forgerock.openam.radius.common.packet.NASClassAttribute;
import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASIdentifierAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;
import org.forgerock.openam.radius.common.packet.NASPortTypeAttribute;
import org.forgerock.openam.radius.common.packet.PortLimitAttribute;
import org.forgerock.openam.radius.common.packet.ProxyStateAttribute;
import org.forgerock.openam.radius.common.packet.ServiceTypeAttribute;
import org.forgerock.openam.radius.common.packet.SessionTimeoutAttribute;
import org.forgerock.openam.radius.common.packet.TerminationActionAttribute;
import org.forgerock.openam.radius.common.packet.UnknownAttribute;
import org.forgerock.openam.radius.common.packet.VendorSpecificAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates the attribute type code held in the first octet of the on-the-wire representation of a specific radius
 * attribute into the corresponding java object used to interact with attribute fields of that type.
 */
public final class AttributeFactory {

    private static Logger logger = LoggerFactory.getLogger(RadiusCommonConstants.RADIUS_COMMON_LOGGER);

    /**
     * Private constructor so this utility class can not be instantiated.
     */
    private AttributeFactory() {

    }

    /**
     * Performs the translation based upon the first octed in the passed in on-the-wire representation.
     *
     * @param data
     *            the raw octets received in a radius packet on the wire for the attribute including leading attribute
     *            type code octet and length octet.
     * @return the corresponding subclass of the {@link org.forgerock.openam.radius.common.Attribute} class or an
     *         instance of the {@link com.sun.identity.authentication.modules.radius.client.UnknownAttribute} class if
     *         the field type is unrecognized and hence not supported.
     */
    public static Attribute createAttribute(byte[] data) {
        logger.trace("Entering AttributeFactory.createAttribute()");
        final int attributeTypeInt = data[0] & 0xFF;
        final AttributeType type = AttributeType.getType(attributeTypeInt);
        logger.trace("AttributeType in is " + attributeTypeInt + ", which is type "
                + ((type == null) ? "null" : type.toString()));
        if (type != null) {
            switch (type) {
            case USER_NAME: // 1
                return new UserNameAttribute(data);
            case USER_PASSWORD: // 2
                return new UserPasswordAttribute(data);
            case NAS_IP_ADDRESS: // 4
                return new NASIPAddressAttribute(data);
            case NAS_PORT: // 5
                return new NASPortAttribute(data);
            case CHAP_PASSWORD: // 3
                return new CHAPPasswordAttribute(data);
            case SERVICE_TYPE: // 6
                return new ServiceTypeAttribute(data);
            case FRAMED_PROTOCOL: // 7
                return new FramedProtocolAttribute(data);
            case FRAMED_IP_ADDRESS: // 8
                return new FramedIPAddressAttribute(data);
            case FRAMED_IP_NETMASK: // 9
                return new FramedIPNetmaskAttribute(data);
            case FRAMED_ROUTING: // 10
                return new FramedRoutingAttribute(data);
            case FILTER_ID: // 11
                return new FilterIdAttribute(data);
            case FRAMED_MTU: // 12
                return new FramedMTUAttribute(data);
            case FRAMED_COMPRESSION: // 13
                return new FramedCompressionAttribute(data);
            case LOGIN_IP_HOST: // 14
                return new LoginIPHostAttribute(data);
            case LOGIN_SERVICE: // 15
                return new LoginServiceAttribute(data);
            case LOGIN_TCP_PORT: // 16
                return new LoginTCPPortAttribute(data);
            case REPLY_MESSAGE: // 18
                return new ReplyMessageAttribute(data);
            case CALLBACK_NUMBER: // 19
                return new CallbackNumberAttribute(data);
            case CALLBACK_ID: // 20
                return new CallbackIdAttribute(data);
            case FRAMED_ROUTE: // 22
                return new FramedRouteAttribute(data);
            case FRAMED_IPX_NETWORK: // 23
                return new FramedIPXNetworkAttribute(data);
            case STATE: // 24
                return new StateAttribute(data);
            case NAS_CLASS: // 25
                return new NASClassAttribute(data);
            case VENDOR_SPECIFIC: // 26
                return new VendorSpecificAttribute(data);
            case SESSION_TIMEOUT: // 27
                return new SessionTimeoutAttribute(data);
            case IDLE_TIMEOUT: // 28
                return new IdleTimeoutAttribute(data);
            case TERMINATION_ACTION: // 29
                return new TerminationActionAttribute(data);
            case CALLER_STATION_ID: // 30
                return new CallerStationIdAttribute(data);
            case CALLING_STATION_ID: // 31
                return new CallingStationIdAttribute(data);
            case NAS_IDENTIFIER: // 32
                return new NASIdentifierAttribute(data);
            case PROXY_STATE: // 33
                return new ProxyStateAttribute(data);
            case LOGIN_LAT_SERVICE: // 34
                return new LoginLATServiceAttribute(data);
            case LOGIN_LAT_NODE: // 35
                return new LoginLATNodeAttribute(data);
            case LOGIN_LAT_GROUP: // 36
                return new LoginLATGroupAttribute(data);
            case FRAMED_APPLETALK_LINK: // 37
                return new FramedAppleTalkLinkAttribute(data);
            case FRAMED_APPLETALK_NETWORK: // 38
                return new FramedAppleTalkNetworkAttribute(data);
            case FRAMED_APPLETALK_ZONE: // 39
                return new FramedAppleTalkZoneAttribute(data);
            case CHAP_CHALLENGE: // 60
                return new CHAPChallengeAttribute(data);
            case NAS_PORT_TYPE: // 61
                return new NASPortTypeAttribute(data);
            case PORT_LIMIT: // 62
                return new PortLimitAttribute(data);
            case LOGIN_LAT_PORT: // 63
                return new LoginLATPortAttribute(data);
            default:
                return new UnknownAttribute(data);
            }
        } else {
            logger.debug("Unknown attribute type.");
            return new UnknownAttribute(data);
        }
    }
}
