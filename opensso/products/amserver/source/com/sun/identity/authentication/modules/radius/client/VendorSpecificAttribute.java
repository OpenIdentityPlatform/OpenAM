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
 * $Id: VendorSpecificAttribute.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public class VendorSpecificAttribute extends Attribute
{
	private byte _value[] = null;
	private String _id = null;
	private String _str = null;

	public VendorSpecificAttribute(byte value[])
	{
		super();
		_t = VENDOR_SPECIFIC;
		_id = new String(value, 2, 4);
		_str = new String(value, 6, value.length - 6);
		_value = value;
	}

	public String getId()
	{
		return _id;
	}

	public String getString()
	{
		return _str;
	}

	public byte[] getValue() throws IOException
	{
		byte v[] = new byte[_id.length() + _str.length()];
		byte idData[] = _id.getBytes();
		byte strData[] = _str.getBytes();
		System.arraycopy(idData, 0, v, 0, _id.length());
		System.arraycopy(strData, 0, v, _id.length(), _str.length());
		return v;
	}
}
