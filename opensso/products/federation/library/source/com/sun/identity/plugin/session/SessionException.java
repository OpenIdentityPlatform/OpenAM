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
 * $Id: SessionException.java,v 1.3 2008/06/25 05:47:28 qcheng Exp $
 *
 */

package com.sun.identity.plugin.session;

import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * This class is to handle Session related exceptions.
 *
 * @supported.all.api
 */
public class SessionException extends L10NMessageImpl {


    /**
     * Error codes.
     */
    public static int AUTH_ERROR_NOT_DEFINED = -1;
    public static int AUTH_USER_INACTIVE = 1;
    public static int AUTH_USER_LOCKED = 2;
    public static int AUTH_ACCOUNT_EXPIRED = 3;

    private int code = AUTH_ERROR_NOT_DEFINED;

    /**
     * Constructs a <code>SessionException</code> with a detailed
     * message.
     *
     * @param message Detailed message for this exception.
     */
    public SessionException(String message) {
        super(message);
    }
    
    /**
     * Constructs a <code>SessionException</code> with
     * an embedded exception.
     *
     * @param rootCause An embedded exception
     */
    public SessionException(Throwable rootCause) {
        super(rootCause);
    }

    /**
     * Constructs a <code>SessionException</code> with an exception.
     *
     * @param ex an exception
     */
    public SessionException(Exception ex) {
       super(ex);
    }

    /**
     * Constructs a new <code>SessionException</code> without a nested
     * <code>Throwable</code>.
     * @param rbName Resource Bundle Name to be used for getting
     *  localized error message.
     * @param messageKey Key to resource bundle. You can use
     * <pre>
     * ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     * String localizedStr = rb.getString(messageCode);
     * </pre>
     * @param args arguments to message. If it is not present pass them
     *  as null
     *
     */
    public SessionException(String rbName, String messageKey, Object[] args) {
        super(rbName, messageKey, args);
    }

    /**
     * Returns the error code for the caller of a <code>SessionProvider</code>
     * method.
     * @return Error code.
     */
    public int getErrCode() {
        return code;
    }

    /**
     * Sets an error code by an implementation of the
     * <code>SessionProvider</code> to indicate a specific error condition
     * which could be retrieved by the caller of a SessionProvider method.
     *
     * @param errorCode the error code to be set.
     */
    public void setErrCode(int errorCode) {
        code = errorCode;
    }
}
