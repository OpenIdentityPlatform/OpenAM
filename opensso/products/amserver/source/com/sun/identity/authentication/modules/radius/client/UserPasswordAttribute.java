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
 * $Id: UserPasswordAttribute.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.security.*;
import java.io.*;

public class UserPasswordAttribute extends Attribute
{
	private Authenticator _ra = null;
	private String _secret = null;
	private String _password = null;

	public UserPasswordAttribute(byte value[])
	{
		//
	}

	public UserPasswordAttribute(Authenticator ra, String secret, String password)
	{
		super(USER_PASSWORD);
		_ra = ra;
		_secret = secret;
		_password = password;
	}

	public byte[] getValue() throws IOException
	{
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5"); 
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage());
		}
		md5.update(_secret.getBytes());
        md5.update(_ra.getData());
        byte sum[] = md5.digest();

        byte up[] = _password.getBytes();
		int oglen = (up.length/16) + 1;
        byte ret[] = new byte[oglen * 16];
        for (int i = 0; i < ret.length; i++) {
			if ((i % 16) == 0) {
				md5.reset();	
				md5.update(_secret.getBytes());
			}
            if (i < up.length) {
                ret[i] = (byte)(sum[i%16] ^ up[i]);
            } else {
                ret[i] = (byte)(sum[i%16] ^ 0);
			}
			md5.update(ret[i]);
			if ((i % 16) == 15) {
				sum = md5.digest();
			}
        }
		return ret;
	}
}
