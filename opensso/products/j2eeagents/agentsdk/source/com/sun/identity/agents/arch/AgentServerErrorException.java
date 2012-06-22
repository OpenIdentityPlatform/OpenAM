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
 *
 */

package com.sun.identity.agents.arch;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The exception type used to indicate the occurrence of a server error.
 */
public class AgentServerErrorException extends AgentException {

    /**
     * This constructor takes a message indicating the nature of failure, along with
     * the exception that originally caused the failure.
     *
     * @param ex The exception that originally caused the failure.
     * @param msg The message indicating the nature of the failure.
     */
    public AgentServerErrorException(String msg, Exception ex) {
        super(msg, ex);
    }

    /**
     * This constructor takes a message indicating the nature of the failure.
     *
     * @param msg The message indicating the nature of the failure.
     */
    public AgentServerErrorException(String msg) {
        super(msg);
    }

    /**
     * This constructor takes a <code>Exception</code> which indicates the
     * root cause of this failure condition.
     *
     * @param ex The <code>Exception</code> which caused this failure.
     */
    public AgentServerErrorException(Exception ex) {
        super(ex);
    }
}
