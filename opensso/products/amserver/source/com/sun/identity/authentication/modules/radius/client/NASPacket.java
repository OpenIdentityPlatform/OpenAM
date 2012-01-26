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
 * $Id: NASPacket.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public abstract class NASPacket extends Packet
{
	public NASPacket(int c, short id, Authenticator auth)
	{
		super(c, id, auth);
	}

	public byte[] getData() throws IOException
	{
		// prepare the attributes first
		ByteArrayOutputStream attrsOS = new ByteArrayOutputStream();
		for (int i = 0; i < _attrs.size(); i++) {
			Attribute attr = (Attribute)getAttributeAt(i);
			attrsOS.write(attr.getData());
		}
		byte attrsData[] = attrsOS.toByteArray();

		ByteArrayOutputStream dataOS = new ByteArrayOutputStream();
		dataOS.write(_c); // code
		dataOS.write(_id); // identifier
		int len = attrsData.length + 20;
		dataOS.write((len >>> 8) & 0xFF);
		dataOS.write(len & 0xFF);
		dataOS.write(_auth.getData());
		dataOS.write(attrsData);

		return dataOS.toByteArray();
	}
}
