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
 * $Id: Attribute.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;
import java.io.*;

public abstract class Attribute
{
	public static final int USER_NAME = 1; 
	public static final int USER_PASSWORD = 2;
	public static final int CHAP_PASSWORD = 3;
	public static final int NAS_IP_ADDRESS = 4;
	public static final int NAS_PORT = 5;
	public static final int SERVICE_TYPE = 6;
	public static final int FRAMED_PROTOCOL = 7;
	public static final int FRAMED_IP_ADDRESS = 8;
	public static final int FRAMED_IP_NETMASK = 9;
	public static final int FRAMED_ROUTING = 10;
	public static final int FILTER_ID = 11;
	public static final int FRAMED_MTU = 12;
	public static final int FRAMED_COMPRESSION = 13;
	public static final int LOGIN_IP_HOST = 14;
	public static final int LOGIN_SERVICE = 15;
	public static final int LOGIN_TCP_PORT = 16;
	// 17 HAS NOT BEEN ASSIGNED
	public static final int REPLY_MESSAGE = 18;
	public static final int CALLBACK_NUMBER = 19;
	public static final int CALLBACK_ID = 20;
	// 21 HAS NOT BEEN ASSIGNED
	public static final int FRAMED_ROUTE = 22;
	public static final int FRAMED_IPX_NETWORK = 23;
	public static final int STATE = 24;
	public static final int NAS_CLASS = 25;
	public static final int VENDOR_SPECIFIC = 26;
	public static final int SESSION_TIMEOUT = 27;
	public static final int IDLE_TIMEOUT = 28;
	public static final int TERMINATION_ACTION = 29;
	public static final int CALLER_STATION_ID = 30;
	public static final int CALLING_STATION_ID = 31;
	public static final int NAS_IDENTIFIER = 32;
	public static final int PROXY_STATE = 33;
	public static final int LOGIN_LAT_SERVICE = 34;
	public static final int LOGIN_LAT_NODE = 35;
	public static final int LOGIN_LAT_GROUP = 36;
	public static final int FRAMED_APPLETALK_LINK = 37;
	public static final int FRAMED_APPLETALK_NETWORK = 38;
	public static final int FRAMED_APPLETALK_ZONE = 39;
	// 40-59 HAS NOT BEEN ASSIGNED
	public static final int CHAP_CHALLENGE = 60;
	public static final int NAS_PORT_TYPE = 61;
	public static final int PORT_LIMIT = 62;
	public static final int LOGIN_LAT_PORT = 63;

	protected int _t = 0;

	public Attribute()
	{
	}

	public Attribute(int t)
	{
		_t = t;
	}

	public int getType()
	{
		return _t;
	}

	public abstract byte[] getValue() 
		throws IOException;

	public byte[] getData() 
		throws IOException
	{
		ByteArrayOutputStream attrOS = new ByteArrayOutputStream();		
		attrOS.write(_t); // type
		byte value[] = getValue();
		attrOS.write(value.length + 2); // length
		attrOS.write(value);

		return attrOS.toByteArray();
	}
}
