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
 * $Id: AttributeFactory.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public class AttributeFactory
{
	public static Attribute createAttribute(byte data[])
		throws IOException
	{
		switch (data[0] & 0xFF)
		{
		case Attribute.USER_NAME: // 1
			return new UserNameAttribute(data);
		case Attribute.USER_PASSWORD: // 2
			return new UserPasswordAttribute(data);
		case Attribute.NAS_IP_ADDRESS: // 4
			return new NASIPAddressAttribute(data);
		case Attribute.NAS_PORT: // 5
			return new NASPortAttribute(data);
		case Attribute.CHAP_PASSWORD: // 3
			return new CHAPPasswordAttribute(data);
		case Attribute.SERVICE_TYPE: // 6
			return new ServiceTypeAttribute(data);
		case Attribute.FRAMED_PROTOCOL: // 7
			return new FramedProtocolAttribute(data);
		case Attribute.FRAMED_IP_ADDRESS: // 8
			return new FramedIPAddressAttribute(data);
		case Attribute.FRAMED_IP_NETMASK: // 9
			return new FramedIPNetmaskAttribute(data);
		case Attribute.FRAMED_ROUTING: // 10 
			return new FramedRoutingAttribute(data);
		case Attribute.FILTER_ID: // 11 
			return new FilterIdAttribute(data);
		case Attribute.FRAMED_MTU: // 12 
			return new FramedMTUAttribute(data);
		case Attribute.FRAMED_COMPRESSION: // 13
			return new FramedCompressionAttribute(data);
		case Attribute.LOGIN_IP_HOST: // 14
			return new LoginIPHostAttribute(data);
		case Attribute.LOGIN_SERVICE: // 15
			return new LoginServiceAttribute(data);
		case Attribute.LOGIN_TCP_PORT: // 16
			return new LoginTCPPortAttribute(data);
		case Attribute.REPLY_MESSAGE: // 18
			return new ReplyMessageAttribute(data);
		case Attribute.CALLBACK_NUMBER: // 19
			return new CallbackNumberAttribute(data);
		case Attribute.CALLBACK_ID: // 20 
			return new CallbackIdAttribute(data);
		case Attribute.FRAMED_ROUTE: // 22 
			return new FramedRouteAttribute(data);
		case Attribute.FRAMED_IPX_NETWORK: // 23 
			return new FramedIPXNetworkAttribute(data);
		case Attribute.STATE: // 24 
			return new StateAttribute(data);
		case Attribute.NAS_CLASS: // 25 
			return new NASClassAttribute(data);
		case Attribute.VENDOR_SPECIFIC: // 26 
			return new VendorSpecificAttribute(data);
		case Attribute.SESSION_TIMEOUT: // 27 
			return new SessionTimeoutAttribute(data);
		case Attribute.IDLE_TIMEOUT: // 28 
			return new IdleTimeoutAttribute(data);
		case Attribute.TERMINATION_ACTION: // 29 
			return new TerminationActionAttribute(data);
		case Attribute.CALLER_STATION_ID: // 30 
			return new CallerStationIdAttribute(data);
		case Attribute.CALLING_STATION_ID: // 31 
			return new CallingStationIdAttribute(data);
		case Attribute.NAS_IDENTIFIER: // 32 
			return new NASIdentifierAttribute(data);
		case Attribute.PROXY_STATE: // 33 
			return new ProxyStateAttribute(data);
		case Attribute.LOGIN_LAT_SERVICE: // 34 
			return new LoginLATServiceAttribute(data);
		case Attribute.LOGIN_LAT_NODE: // 35 
			return new LoginLATNodeAttribute(data);
		case Attribute.LOGIN_LAT_GROUP: // 36 
			return new LoginLATGroupAttribute(data);
		case Attribute.FRAMED_APPLETALK_LINK: // 37 
			return new FramedAppleTalkLinkAttribute(data);
		case Attribute.FRAMED_APPLETALK_NETWORK: // 38 
			return new FramedAppleTalkNetworkAttribute(data);
		case Attribute.FRAMED_APPLETALK_ZONE: // 39 
			return new FramedAppleTalkZoneAttribute(data);
		case Attribute.CHAP_CHALLENGE: // 60 
			return new CHAPChallengeAttribute(data);
		case Attribute.NAS_PORT_TYPE: // 61 
			return new NASPortTypeAttribute(data);
		case Attribute.PORT_LIMIT: // 62 
			return new PortLimitAttribute(data);
		case Attribute.LOGIN_LAT_PORT: // 63 
			return new LoginLATPortAttribute(data);
		default:
			return new GenericAttribute(data);
			// throw new IOException("Unknown attribute " + (data[0] & 0xFF));
		}
	}
}
