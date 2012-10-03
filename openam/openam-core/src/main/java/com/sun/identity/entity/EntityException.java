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
 * $Id: EntityException.java,v 1.2 2008/06/25 05:43:25 qcheng Exp $
 *
 */

package com.sun.identity.entity;

/**
 * The <code>EntityException</code> is thrown whenever an error is is
 * encountered while performing an operation on the data store.
 */
public class EntityException extends Exception {
    private String localizedMsg = null;

    private String errorCode = null;

    private Object args[] = null;

    private String ldapErrCode = null;

    /**
     * Constructs a new <code>EntityException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     */
    public EntityException(String msg, String errorCode) {
        super(msg);
        this.localizedMsg = msg;
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new <code>EntityException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>
     * @param args
     *            if the error message needs specific values to be set
     */
    public EntityException(String msg, String errorCode, Object[] args) {
        super(msg);
        this.localizedMsg = msg;
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * Method to obtain the error code. This error code can be used with the
     * arguments to construct a localized message.
     * 
     * @return the error code which can be used to map the message to a user
     *         specific locale.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Method to obtain the arguments corresponding to the error code.
     * 
     * @return the arguments corresponding to the error code or null if no
     *         arguments are need to construct the message
     */
    public Object[] getMessageArgs() {
        return args;
    }

    /**
     * Overriding the default <code>getMessage()</code> method of super class
     * Exception.
     * 
     * @return The error message string
     */
    public String getMessage() {
        return localizedMsg;
    }

    /**
     * Method to obtain the LDAP error code.
     * 
     * @return The error code, which can be used to map the message to a
     *         specific locale. returns a null, if not an LDAP error
     */
    public String getLDAPErrorCode() {
        return ldapErrCode;
    }

}
