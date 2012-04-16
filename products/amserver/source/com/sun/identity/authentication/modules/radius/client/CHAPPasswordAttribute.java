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
 * $Id: CHAPPasswordAttribute.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public class CHAPPasswordAttribute extends Attribute
{
	private byte _value[] = null;
	private int _ident = 0;
	private String _str = null;

	public CHAPPasswordAttribute(String s)
	{
		_str = s;
	}

	public CHAPPasswordAttribute(byte value[])
	{
		super();
		_t = CHAP_PASSWORD;
		_ident = value[2];
		_str = new String(value, 2, 16);
		_value = value;
	}

	public int getIdent()
	{
		return _ident;
	}

	public String getString()
	{
		return _str;
	}

	public byte[] getValue() throws IOException
	{
		byte val[] = new byte[1 + _str.length()];
		byte s[] = _str.getBytes();
		val[0] = (byte)_ident;
		System.arraycopy(s, 0, val, 1, s.length);
		return val;
	}
}
