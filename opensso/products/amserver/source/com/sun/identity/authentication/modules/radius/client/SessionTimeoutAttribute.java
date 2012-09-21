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
 * $Id: SessionTimeoutAttribute.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public class SessionTimeoutAttribute extends Attribute
{
	private int _timeout = 0;

    public SessionTimeoutAttribute(byte value[])
    {
        super();
        _t = SESSION_TIMEOUT;
        _timeout = value[5] & 0xFF;
        _timeout |= ((value[4] << 8) & 0xFF00);
        _timeout |= ((value[3] << 16) & 0xFF0000);
        _timeout |= ((value[2] << 24) & 0xFF000000);
    }

	public SessionTimeoutAttribute(int timeout)
	{
		super(SESSION_TIMEOUT);
		_timeout = timeout;
	}

	public byte[] getValue() throws IOException
	{
		byte[] p = new byte[4]; 

		p[0] = (byte) ((_timeout >>> 24) & 0xFF); 
		p[1] = (byte) ((_timeout >>> 16) & 0xFF); 
		p[2] = (byte) ((_timeout >>> 8) & 0xFF); 
		p[3] = (byte) (_timeout & 0xFF); 
		return p;
	}
}
