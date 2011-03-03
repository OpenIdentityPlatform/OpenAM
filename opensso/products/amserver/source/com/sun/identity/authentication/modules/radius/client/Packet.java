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
 * $Id: Packet.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

public abstract class Packet
{
	public static final int ACCESS_REQUEST = 1; 
	public static final int ACCESS_ACCEPT = 2; 
	public static final int ACCESS_REJECT = 3; 
	// public static final int ACCOUNTING_REQUEST = 4; 
	// public static final int ACCOUNTING_RESPONSE = 5; 
	public static final int ACCESS_CHALLENGE = 11;
	public static final int RESERVED = 255; 

	protected int _c = 0;
	protected short _id = 0;
	protected Authenticator _auth = null;
	protected AttributeSet _attrs = new AttributeSet();

	public Packet()
	{
	}

	public Packet(int c, short id, Authenticator auth)
	{
		_c = c;
		_id = id;
		_auth = auth;
	}

	public int getCode()
	{
		return _c;
	}

	public short getIdentifier()
	{
		return _id;
	}

	public Authenticator getAuthenticator()
	{
		return _auth;
	}

	public void addAttribute(Attribute attr)
	{
		_attrs.addAttribute(attr);
	}

	public AttributeSet getAttributeSet()
	{
		return _attrs;
	}

	public Attribute getAttributeAt(int pos)
	{
		return _attrs.getAttributeAt(pos);
	}

	public String toString()
	{
		return "Packet [code=" + _c + ",id=" + (_id & 0xFF) + "]";
	}
}
