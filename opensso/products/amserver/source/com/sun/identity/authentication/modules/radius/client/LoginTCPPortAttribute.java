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
 * $Id: LoginTCPPortAttribute.java,v 1.2 2008/06/25 05:42:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public class LoginTCPPortAttribute extends Attribute
{
	private int _port = 0;

    public LoginTCPPortAttribute(byte value[])
    {
        super();
        _t = LOGIN_TCP_PORT;
        _port = value[5] & 0xFF;
        _port |= ((value[4] << 8) & 0xFF00);
        _port |= ((value[3] << 16) & 0xFF0000);
        _port |= ((value[2] << 24) & 0xFF000000);
    }


	public LoginTCPPortAttribute(int port)
	{
		super(LOGIN_TCP_PORT);
		_port = port;
	}

	public int getPort()
	{
		return _port;
	}

	public byte[] getValue() throws IOException
	{
		byte[] p = new byte[4]; 

		p[0] = (byte) ((_port >>> 24) & 0xFF); 
		p[1] = (byte) ((_port >>> 16) & 0xFF); 
		p[2] = (byte) ((_port >>> 8) & 0xFF); 
		p[3] = (byte) (_port & 0xFF); 
		return p;
	}
}
