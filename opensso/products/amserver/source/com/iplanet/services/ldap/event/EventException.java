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
 * $Id: EventException.java,v 1.3 2008/06/25 05:41:38 qcheng Exp $
 *
 */

package com.iplanet.services.ldap.event;

import java.io.PrintStream;
import java.io.PrintWriter;

import com.iplanet.services.ldap.LDAPServiceException;

/**
 * Exception occurs while setting an event request or when trigering the
 * "entryChanged()" method after a persistent search results are received from
 * the Directory Server.
 * @supported.api
 */
public class EventException extends LDAPServiceException {

    /**
     * Holds the message assocuated with this exception, if present.
     */
    private String _message = "";

    /**
     * Constructs a EventException with no detail message and no nested
     * exception. public EventException() { super(); }
     */

    /**
     * Constructs a EventException with a detail message.
     * 
     * @param msg
     *            Message string for the exception 
     * @supported.api
     */
    public EventException(String msg) {
        super(msg);
        _message = msg;
    }

    /**
     * Constructor with message string and an embedded exception Constructs a
     * EventException with the given detail message and nested exception.
     * 
     * @param msg
     *            Message string
     * @param t
     *            The embedded exception 
     * @supported.api
     */
    public EventException(String msg, Throwable t) {
        super(msg, t);
        _message = msg;
    }

    /**
     * Returns a string representation of this EventException, including the
     * detail message (if present);
     * 
     * @return a string representation of this EventException, including its
     *         detail message, if present.
     * @supported.api
     */
    public String toString() {
        return super.toString()
                + (_message == null ? "" : " [message: " + _message + "]");
    }

    /**
     * Returns a string representation of the message in the this EventException
     * (if present)
     * 
     * @return a string representation of the detailed message, if present.
     *        
     * @supported.api
     */
    public String getMessage() {
        return _message;
    }

    /**
     * Prints a stack trace for this EventException to System.out;
     *
     * @supported.api
     */
    public void printStackTrace() {
        super.printStackTrace();
    }

    /**
     * Prints a stack trace for this EventException to the given PrintStream;
     * 
     * @param printStream
     *            a PrintStream to print the stack trace out to.
     *           
     * @supported.api
     */
    public void printStackTrace(PrintStream printStream) {
        super.printStackTrace(printStream);
    }

    /**
     * Prints a stack trace for this EventException to the given PrintWriter;
     * 
     * @param printWriter
     *            a PrintWriter to print the stack trace out to.
     *           
     * @supported.api
     */
    public void printStackTrace(PrintWriter printWriter) {
        super.printStackTrace(printWriter);
    }
}
