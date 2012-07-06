/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WSTException.java,v 1.1 2008/09/19 16:00:56 mallas Exp $
 *
 */

package com.sun.identity.wss.trust;

/**
 * The class <code>WSTException</code> is used to throw exceptions for 
 * ws-trust protocol level related errors.
 */
public class WSTException extends Exception {
    
    /**
     * Create an <code>WSTException</code> with no message.
     */
    public WSTException() {
	super();
    }

    /**
     * Create an <code>WSTException</code> with a message.
     * @param message message for the exception
     */
    public WSTException(String message) {
	super(message);
    }

}
