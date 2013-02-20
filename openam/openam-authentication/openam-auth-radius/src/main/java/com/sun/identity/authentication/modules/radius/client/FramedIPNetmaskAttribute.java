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
 * $Id: FramedIPNetmaskAttribute.java,v 1.2 2008/06/25 05:42:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public class FramedIPNetmaskAttribute extends Attribute
{
	private byte _value[] = null;
    private byte _mask[] = new byte[4];

	public FramedIPNetmaskAttribute(byte value[])
	{
		super();
		_t = FRAMED_IP_NETMASK;
        _mask[0] = value[2];
        _mask[1] = value[3];
        _mask[2] = value[4];
        _mask[3] = value[5];
		_value = value;
	}

	public byte[] getValue() throws IOException
	{
		return _mask;
	}
}
