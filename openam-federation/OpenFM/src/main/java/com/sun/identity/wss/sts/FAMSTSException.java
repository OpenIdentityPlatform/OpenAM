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
 * $Id: FAMSTSException.java,v 1.3 2008/08/27 19:05:53 mrudul_uchil Exp $
 *
 */


package com.sun.identity.wss.sts;

/**
 * This class <code>FAMSTSException</code> is used to generate the
 * exceptions during failures for generating or handling Security
 * tokens via Security Token Service.
 *
 * @supported.all.api
 */

public class FAMSTSException extends Exception {

    /**
     * Create an <code>SecurityException</code> with no message.
     */
    public FAMSTSException() {
	super();
    }

    /**
     * Create an <code>SecurityException</code> with a message.
     * @param message message for the exception
     */
    public FAMSTSException(String message) {
	super(message);
    }
}

