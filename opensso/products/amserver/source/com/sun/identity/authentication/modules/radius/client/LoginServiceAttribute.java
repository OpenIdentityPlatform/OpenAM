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
 * $Id: LoginServiceAttribute.java,v 1.2 2008/06/25 05:42:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public class LoginServiceAttribute extends Attribute
{
	public static final int TELNET = 0;
	public static final int RLOGIN = 1;
	public static final int TCP_CLEAR = 2;
	public static final int PORTMASTER = 3;
	public static final int LAT = 4;
	public static final int X25_PAD = 5;
	public static final int X25_T3POS = 6;
	public static final int TCP_CLEAR_QUIET = 8;

	private byte _value[] = null;
    private int _type = 0;

	public LoginServiceAttribute(byte value[])
	{
		super();
		_t = LOGIN_SERVICE;
        _type = value[5] & 0xFF;
        _type |= ((value[4] << 8) & 0xFF00);
        _type |= ((value[3] << 16) & 0xFF0000);
        _type |= ((value[2] << 24) & 0xFF000000);
		_value = value;
	}

    public int getType()
    {
        return _type;
    }

	public byte[] getValue() throws IOException
	{
        byte[] p = new byte[4];

        p[0] = (byte) ((_type >>> 24) & 0xFF);
        p[1] = (byte) ((_type >>> 16) & 0xFF);
        p[2] = (byte) ((_type >>> 8) & 0xFF);
        p[3] = (byte) (_type & 0xFF);
        return p;
	}
}
