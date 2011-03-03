/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AgentException.java,v 1.2 2008/06/25 05:51:35 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The basic exception type used within the Agent SDK to indicate the
 * occurrence of error conditions. It provides the necessary means and
 * functionality to preserve the root cause exception by allowing the
 * chaining of exceptions where needed. 
 */
public class AgentException extends Exception {

    /**
     * This constructor takes a message indicating the nature of failure, 
     * along with the exception that originally caused the failure.
     *
     * @param ex The exception that originally caused the failure.
     * @param msg The message indicating the nature of the failure.
     */
    public AgentException(String msg, Exception ex) {
        setInnerException(ex);
        setMessage(msg);
    }

    /**
     * This constructor takes a message indicating the nature of the failure.
     *
     * @param msg The message indicating the nature of the failure.
     */
    public AgentException(String msg) {
        this(msg, null);
    }

    /**
     * This constructor takes a <code>Exception</code> which indicates the
     * root cause of this failure condition.
     *
     * @param ex The <code>Exception</code> which caused this failure.
     */
    public AgentException(Exception ex) {
        this(ex.getMessage(), ex);
    }

    /**
     * Returns the message that was supplied to the constructor at the time 
     * when this exception was created.
     *
     * @return The message string.
     */
    public String getMessage() {
        return _message;
    }

    /**
     * Prints the stack trace of this exception to the specified
     * <code>PrintWriter</code>.
     *
     * @param err The <code>PrintWriter</code> to which the stack trace will 
     * be written.
     */
    public void printStackTrace(PrintWriter err) {

        err.println("[AgentException Stack]");
        super.printStackTrace(err);

        Throwable ex = getInnerException();

        while(ex != null) {
            if(ex instanceof AgentException) {
                AgentException ae = (AgentException) ex;

                err.println("--------");
                ae.printStackTrace(err);

                ex = ae.getInnerException();
            } else {
                err.println("--------");
                ex.printStackTrace(err);

                ex = null;
            }
        }
    }

    /**
     * Prints the stack trace of this exception to the specified
     * <code>PrintStream</code>.
     *
     * @param pstream The <code>PrintStream</code> to which the stack trace 
     * will be written.
     */
    public void printStackTrace(PrintStream pstream) {
        printStackTrace(new PrintWriter(pstream, true));
    }

    /**
     * Returns the exception that was supplied to the constructor at the 
     * time when this exception was created.
     *
     * @return The inner exception.
     */
    public Throwable getInnerException() {
        return _exception;
    }
    
    private void setInnerException(Throwable exception) {
        _exception = exception;
    }
    
    private void setMessage(String message) {
        _message = message;
    }

    private Throwable _exception;
    private String    _message;
}
