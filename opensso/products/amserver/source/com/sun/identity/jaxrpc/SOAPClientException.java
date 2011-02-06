/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SOAPClientException.java,v 1.4 2008/06/25 05:43:34 qcheng Exp $
 *
 */

package com.sun.identity.jaxrpc;

/**
 * An <code>SOAPClientException</code> is thrown when there are errors related
 * to JAXRPC and SOAP methods.
 *
 * @supported.all.api
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.jaxrpc.SOAPClientException}
 */
public class SOAPClientException extends Exception {

    private String message = null;

    private String className = null;

    /**
     * Create <code>SOAPClientException</code> with no message.
     */
    public SOAPClientException() {
        super();
    }

    /**
     * Create <code>SOAPClientException</code> with a message.
     * 
     * @param className
     *            The name of the class associated with exception.
     */
    public SOAPClientException(String className) {
        this.className = className;
    }

    /**
     * Create <code>SOAPClientException</code> with a class name and message.
     * 
     * @param className
     *            The name of the class associated with exception.
     * @param exceptionMessage
     *            The message associated with exception.
     */
    public SOAPClientException(String className, String exceptionMessage) {
        super(exceptionMessage);
        this.className = className;
        message = exceptionMessage;
    }

    /**
     * Method to obtain the class name.
     * 
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Method to obtain the message.
     * 
     * @return message
     */
    public String getMessage() {
        return message;
    }

}
