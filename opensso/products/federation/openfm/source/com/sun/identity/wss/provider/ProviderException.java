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
 * $Id: ProviderException.java,v 1.4 2008/08/27 19:05:51 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.provider;

/**
 * This class <code>ProviderException</code> represents the Exception
 * that can be thrown for any errors in accessing any provider configuration 
 * (Web Services Client, Web Services Provider, STS client or Discovery client).
 * @supported.all.api
 */

public class ProviderException extends Exception {

    /**
     * Create an <code>ProviderException</code> with no message.
     */
    public ProviderException() {
	super();
    }

    /**
     * Create an <code>ProviderException</code> with a message.
     * @param s message for the exception
     */
    public ProviderException(String s) {
	super(s);
    }
}

