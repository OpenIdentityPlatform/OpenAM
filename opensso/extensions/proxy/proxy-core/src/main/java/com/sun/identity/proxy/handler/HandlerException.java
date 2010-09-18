/* The contents of this file are subject to the terms
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
 * $Id: HandlerException.java,v 1.2 2009/10/09 07:38:37 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.handler;

/**
 * An exception that a {@link Handler} can throw when it cannot handle a
 * message exchange.
 *
 * @author Paul C. Bryan
 */
public class HandlerException extends Exception
{
    /**
     * Creates a new handler exception.
     */
    public HandlerException() {
        super();
    }
    
    /**
     * Creates a new handler exception with a specified message.
     *
     * @param message the text of the exception message.
     */
    public HandlerException(String message) {
        super(message);
    }
    
    /**
     * Creates a new handler exception with a specified root cause.
     *
     * @param cause the exception that interfered with the handler's operation.
     */
    public HandlerException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new handler exception with a specified message and root cause.
     *
     * @param message the text of the exception message.
     * @param cause the exception that interfered with the handler's operation.
     */
    public HandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}

