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
 * $Id: ServerPacket.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.io.*;

public abstract class ServerPacket extends Packet
{
	public ServerPacket(byte data[]) throws IOException
	{
		super();
        _c = data[0];
        _id = data[1];
		int datalen = data[3] & 0xFF;
        datalen |= ((data[2] << 8) & 0xFF00);
        byte authData[] = new byte[16];
        System.arraycopy(data, 4, authData, 0, 16);
        _auth = new ResponseAuthenticator(authData);

		// building attributes
		int startp = 20;
		while (startp != datalen) {
        	int attrLen = (data[startp+1] & 0xFF);
        	byte attrData[] = new byte[attrLen];
			System.arraycopy(data, startp, attrData, 0, attrData.length); 
			addAttribute(AttributeFactory.createAttribute(attrData));	
			startp += attrData.length;
		}
	}
}
