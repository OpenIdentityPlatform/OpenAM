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
 * $Id: LDAPSocketFactory.java,v 1.3 2009/01/28 05:35:02 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.protocol;

import com.sun.identity.shared.ldap.factory.JSSESocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.sun.identity.shared.debug.Debug;

/**
 * Generate SSLContext can be used to create an SSL socket connection 
 * to ldap server. 
 * It is using the JSSE package and extends 
 * the <CODE>com.sun.identity.shared.ldap.factory.JSSESocketFactory</CODE> class.
 */

public class LDAPSocketFactory extends JSSESocketFactory
{
    static private SSLSocketFactory sf = null;
    static private Debug debug = Debug.getInstance("amJSSE");
	
    static {
    	try {
    	    sf = SSLSocketFactoryManager.getSocketFactory();
	} catch (Exception e) {
	    debug.error("Exception in LDAPSocketFactory.init()" + e.toString());
	}
    }

    /**
     * Constructs a new <CODE>LDAPSocketFactory</CODE>.
     * If the JSSE SSL Context is not already initialized, initialize it
     * using the default and configured value. 
     */
    public LDAPSocketFactory() {
    	super(null, sf);
    }
}
