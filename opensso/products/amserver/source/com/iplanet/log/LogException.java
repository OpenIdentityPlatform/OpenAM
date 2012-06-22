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
 * $Id: LogException.java,v 1.3 2008/06/25 05:41:32 qcheng Exp $
 *
 */

package com.iplanet.log;

/** 
 * The applications may encounter errors when they are denied for 
 * a log access because they don't have the access privileges or
 * when they do not have a valid session. The applications may also
 * encounter errors when the logging server is not able response to
 * the applications' request or for some other reasons. This 
 * LogException class provides the method for detecting such errors.
 *
 * @supported.all.api
 */
public class LogException extends Exception {

    /**
     * Invalid session.
     */
    static public final int INVALID_SESSION = 500;

    /**
     * Log already exists.
     */
    static public final int ALREADY_EXISTS = 501;

    /**
     * Log is in inactive state, preventing log submission.
     */
    static public final int INACTIVE = 502;

    /**
     * Log handler error.
     */
    static public final int LOG_HANDLER_ERROR = 503;

    /**
     * Log submission error.
     */
    static public final int WRITE_ERROR = 504;

    /**
     * Log Retrieval error.
     */
    static public final int READ_ERROR = 505;

    /**
     * Log deletion error.
     */
    static public final int DELETE_ERROR = 506;

    /**
     * Log list does not exist.
     */
    static public final int LIST_NOT_EXISTS = 507;

    /**
     * Log type error.
     */
    static public final int TYPE_ERROR = 508;

    /**
     * Log creation privilege is denied.
     */
    static public final int CREATE_ACCESS_DENIED = 509;

    /**
     * Log submition privilege is denied.
     */
    static public final int WRITE_ACCESS_DENIED = 510;

    /**
     * Log retrieval privilege is denied.
     */
    static public final int READ_ACCESS_DENIED = 511;

    /**
     * Log listing privilege is denied.
     */
    static public final int LIST_ACCESS_DENIED = 512;

    /**
     * Log profile error.
     */
    static public final int PROFILE_ERROR = 513;

    /**
     * No such log exists.
     */
    static public final int LOG_NOT_FOUND = 514;

    /**
     * No such log segment exists.
     */
    static public final int NO_SUCH_SEGMENT_EXISTS = 515;

    /**
     * Log deletion privilege is denied.
     */
    static public final int DELETE_ACCESS_DENIED = 516;

    /**
     * Log name is in valid.
     */
    static public final int INVALID_LOG_NAME = 517;

    /**
     * Log retrieval size (in bytes) exceeds the maximum allowed.
     */
    static public final int READ_EXCEEDS_MAX = 518;

    /**
     * Log JDBC driver loading failed.
    static public final int DRIVER_LOAD_FAILED = 519;

    /**
     * Log JDBC driver null location.
     */
    static public final int NULL_LOCATION = 520;

    /**
     * Log JDBC driver connection failed.
     */
    static public final int CONNECTION_FALIED = 521;

    /**
     * Log JDBC null pointer.
     */
    static public final int NULL_POINTER = 522;

    /**
     * Log SQL error
    * /
    static public final int SQL_ERROR = 523;
 
    /**
     * Other unknown fatal error.
     */
    static public final int FATAL_ERROR = 699;

    private int excep_type;

    /**
     * Constructs a log exception.
     */
    public LogException() {
        super();
    }

    /**
     * Constructs a log exception. 
     *
     * @param msg Log exception message.
     */
    public LogException(String msg) {
        super(msg);
        excep_type = 0;
    }

    /**
     * Constructs a log exception.
     *
     * @param msg Exception message.
     * @param type Log exception type.
     */
    public LogException(String msg, int type) {
        super(msg);
        excep_type = type;
    }

    /**
     * Returns log exception type. 
     *
     * @return Log exception type. 
     */
    public int getType() {
        return excep_type;
    }
}

