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
 * $Id: LDAPServiceException.java,v 1.3 2009/01/28 05:34:49 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

import com.sun.identity.shared.ldap.LDAPException;

/**
 * This exception class captures expcetions that occurs in the
 * com.iplanet.services.adal package.
 */
public class LDAPServiceException extends Exception {
    /**
     * SERVER_DOWN(4) Indicates that the server was reported down.
     */
    public final static int SERVER_DOWN = 4;

    /**
     * INVALID_OPERATION(5) Reported an invalid operation. Doesn't say much.
     * does it? Well, it means that something really bad is going on and I for
     * one, do not know what it is.
     */
    public final static int INVALID_OPERATION = 5;

    /**
     * INCORRECT_SERVER_PARAMS(6) Incorrect server parameters passed in the
     * configuration file. Typically, each server specified must have a
     * server_id, server_name and server_type. If either of these values are
     * null, an instance of LDAPServiceException is thrown with this error code
     * set.
     */
    public final static int INCORRECT_SERVER_PARAMS = 6;

    /**
     * INCORRECT_LDAP_PARAMS(7) If the server_type is given as LDAP, then one or
     * more of the ldap parameters are not correct. The possibilities are: 1.
     * The sever_port, auth_id or auth_passwd any one of them are null. 2. Port
     * number is not an integer value or it is not in the range of 0 and 65535.
     */
    public final static int INCORRECT_LDAP_PARAMS = 7;

    /**
     * INCORRECT_CP_PARAMS(8) One or more connection pool parameters incorrect.
     * The possibilities are: 1. The init_conn_pool_length or
     * max_conn_pool_length or conn_pool_load_factor are not integers or are
     * less than or equal to zero. 2. The max_conn_pool_length is less than
     * init_conn_pool_length.
     */
    public final static int INCORRECT_CP_PARAMS = 8;

    /**
     * INCORRECT_LDAP_REBIND_PARAMS(9) The rebind parameters specified are not
     * correct. That is the rebind_id is specified, but rebind_passwd is not
     * specified. The rebind_passwd, if rebind_id is specified, at the least
     * must be "".
     */
    public final static int INCORRECT_LDAP_REBIND_PARAMS = 9;

    /**
     * MUST_HAVE_SERVICE_NAME(10) Simple, the any ips_core:service specified in
     * the xml must have a name.
     */
    public final static int MUST_HAVE_SERVICE_NAME = 10;

    /**
     * FILTER_IS_REQUIRED(11) For a search operation, filter is required.
     */
    public final static int FILTER_IS_REQD = 11;

    /**
     * INVALID_SEARCH_CONSTRAINTS(12) If the constraints specified are not
     * comprehendable, then this error is returned. I'll try to keep this list
     * updated. The possibilities are: 1. The attribute list is not specified in
     * a String[].
     */
    public final static int INVALID_SEARCH_CONSTRAINTS = 12;

    /**
     * SERVICE_NOT_FOUND(13) When called
     * DataAccessProvider.getConnection(String), it tries to look for the type
     * of service it is given to provide. If no such service exits, the server
     * returns with this exception code.
     */
    public final static int SERVICE_NOT_PRESENT = 13;

    /**
     * NO_DEFAULT_SERVICE_SPECIFIED(14) It could happen if you call
     * DataAccessProvider.getConnection() and there was no "common" service
     * defined in the serverconfig.xml. That's when this exception code gets
     * returned.
     */
    public final static int NO_COMMON_SERVICE_SPECIFIED = 14;

    /**
     * SERVER_NOT_PRESENT(15) If you call getConnection, with or without the
     * service name and there is no Server specified for that service, this
     * exception gets thrown.
     */
    public final static int SERVER_NOT_PRESENT = 15;

    /**
     * INVALID_ENTRY_ID(16) Typically, if an invalid ID is passed to Entry's
     * constructor, this error is thrown.
     */
    public final static int INVALID_ENTRY_ID = 16;

    /**
     * INCOMPATIBLE_DB_TYPE(17) E.g. If a function takes Connector as a
     * parameter, it may implicitly expect LDAPConnector, if the current scope
     * of the object is LDAP specific. In such case, if another derivation of
     * Connector is passed, this exception will be thrown.
     */
    public final static int INCOMPATIBLE_DB_TYPE = 17;

    /**
     * CACHE_OP_NOT_PERMITTED(18) If the particular server does not support
     * entry change notifications, the setupCache will return this exception.
     */
    public final static int CACHE_OP_NOT_SUPPORTED = 18;

    /**
     * FILE_NOT_FOUND(19) This exception is thrown if ADAL tries to open a file
     * and get a java.io.FileNotFoundException
     */
    public final static int FILE_NOT_FOUND = 19;

    /**
     * UNKNOWN_ERROR(20) This error is thrown when ADAL does not know what on
     * earth happened. e.g. WebTopParser throws and Exception object, which is
     * so generic that we don't know what to do with it.
     */
    public final static int UNKNOWN_ERROR = 20;

    // /////////////////////METHODS////////////////////////////
    /**
     * Constructor
     * 
     * @param code
     *            The error code that represents the error that occured while
     *            performing an operation.
     */
    public LDAPServiceException(int code) {
        super();
        exceptionCode = code;
    }

    /**
     * Get the exception string.
     * 
     * @return String The exception string.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (rootCause != null) {
            buf.append(rootCause.toString());
        }

        buf.append('\n');
        buf.append(getMessage());

        return buf.toString();
    }

    public String getMessage() {
        String str = "Got LDAPServiceException code=" + exceptionCode;
        return str;
    }

    /**
     * The constructor.
     * 
     * @param code
     *            The error code that represents the error that occured while
     *            performing an operation.
     * @param errormsg
     *            A string description of the error that occured.
     */
    public LDAPServiceException(int code, String errormsg) {
        super(errormsg);
        exceptionCode = code;
    }

    /**
     * The constructor.
     * 
     * @param errormsg
     *            A string description of the error that occured.
     */
    public LDAPServiceException(String errormsg) {
        super(errormsg);
    }

    /**
     * The constructor.
     * 
     * @param errormsg
     *            A string description of the error that occured.
     */
    public LDAPServiceException(String errormsg, Throwable t) {
        super(errormsg);
        rootCause = t;
    }

    /**
     * Gets LDAPException error code.
     * 
     * @return LDAPException error code or -1 if not a LDAPException
     */
    public int getLDAPExceptionErrorCode() {
        if (rootCause == null) {
            return -1;
        } else if (rootCause instanceof LDAPException) {
            return ((LDAPException) rootCause).getLDAPResultCode();
        } else if (rootCause instanceof LDAPServiceException) {
            return ((LDAPServiceException) rootCause)
                    .getLDAPExceptionErrorCode();
        }

        return -1;
    }

    /**
     * The variable contains the error that has occured.
     */
    int exceptionCode = -1;

    Throwable rootCause;
}
