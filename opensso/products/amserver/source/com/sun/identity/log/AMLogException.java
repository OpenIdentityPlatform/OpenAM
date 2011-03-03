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
 * $Id: AMLogException.java,v 1.4 2008/06/25 05:43:35 qcheng Exp $
 *
 */


package com.sun.identity.log;

/**
 * Defines all the exceptions used in Logging service.
 * <code>AMLogException</code> extends the RuntimeException class.
 */
public class AMLogException extends RuntimeException {
    
    public static final String LOG_DB_CONNECT_FAILED =
        "Connection to DB failed";
    public static final String LOG_DB_RECONNECT_FAILED =
        "Reconnection to DB failed";
    public static final String LOG_DB_NO_CONNECTION  = "No Connection to DB";
    public static final String LOG_TO_DB_FAILED  = "Logging to DB failed";
    public static final String LOG_DB_DRIVER = "Could not load DB driver ";
    public static final String LOG_DB_CSTATEMENT = "createStatement failure";
    public static final String LOG_DB_EXECUPDATE = "executeUpdate failure";
    public static final String LOG_DB_TOOMANYRECORDS =
"More than max number of records returned; Increase Max Records in configuration";
    public static final String LOG_WRT_AUTH_FAILED =
        "Log write authorization failure";
    public static final String LOG_RD_AUTH_FAILED =
        "Log read authorization failure";


    /**
     * Constructs an instance of the <code>AMLogException</code> class.
     *
     * @param message The message provided by the object that is throwing the
     * exception.
     */
    public AMLogException (String message) {
        super(message);
    }
}

