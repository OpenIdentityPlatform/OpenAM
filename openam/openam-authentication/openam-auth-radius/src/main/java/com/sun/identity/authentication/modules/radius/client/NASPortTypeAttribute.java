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
 * $Id: NASPortTypeAttribute.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public class NASPortTypeAttribute extends Attribute
{
	public static final int ASYNC = 0;
	public static final int SYNC = 1;
	public static final int ISDN_SYNC = 2;
	public static final int ISDN_ASYNC_V120 = 3;
	public static final int ISDN_ASYNC_V110 = 4;
	public static final int VIRTUAL = 5;
	public static final int PIAFS = 6;
	public static final int HDLC = 7;
	public static final int X_25 = 8;
	public static final int X_75 = 9;
	public static final int G3_FAX = 10;
	public static final int SDSL = 11;
	public static final int ADSL_CAP = 12;
	public static final int ADSL_DMT = 13;
	public static final int IDSL = 14;
	public static final int ETHERNET = 15;
	public static final int XDSL = 16;
	public static final int CABLE = 17;
	public static final int WIRELESS_OTHER = 18;
	public static final int WIRELESS_IEEE_802_11 = 19;

	private byte _value[] = null;

	public NASPortTypeAttribute(byte value[])
	{
		super();
		_t = NAS_PORT_TYPE;
		_value = value;
	}

	public byte[] getValue() throws IOException
	{
		return _value;
	}
}
