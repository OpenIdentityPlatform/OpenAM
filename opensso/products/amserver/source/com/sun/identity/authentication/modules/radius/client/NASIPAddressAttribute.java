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
 * $Id: NASIPAddressAttribute.java,v 1.2 2008/06/25 05:42:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.net.*;
import java.io.*;

public class NASIPAddressAttribute extends Attribute
{
	private InetAddress _ip = null;
	private byte _value[] = null;

    public NASIPAddressAttribute(byte value[])
    {
        super();
        _t = NAS_IP_ADDRESS;
        _value = value;
    }

	public NASIPAddressAttribute(InetAddress ip)
	{
		super(NAS_IP_ADDRESS);
		_ip = ip;
	}

	public byte[] getValue() throws IOException
	{
		return _ip.getAddress();
	}
}
