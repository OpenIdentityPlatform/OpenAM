/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RestException.java,v 1.1 2009/11/12 18:37:35 veiming Exp $
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.rest;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Entitlement related exception.
 */
public class RestException extends Exception {
    private static final String RES_BUNDLE_NAME = "RestException";

    private int errorCode;
    private String message;
    private Object[] params;

    
    /**
     * Creates an entitlement exception.
     *
     * @param errorCode Error code.
     */
    public RestException(int errorCode) {
        this.errorCode = errorCode;
        this.message = getLocalizedMessage(Locale.getDefault());
    }

    /**
     * Creates an entitlement exception.
     * 
     * @param errorCode Error code.
     * @param params Parameters for formatting the message string.
     */
    public RestException(int errorCode, Object[] params) {
        this.errorCode = errorCode;
        this.params = params;
        this.message = getLocalizedMessage(Locale.getDefault());
    }

    /**
     * Creates an entitlement exception.
     *
     * @param errorCode Error code.
     * @param cause Root cause.
     */
    public RestException(int errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
        this.message = getLocalizedMessage(Locale.getDefault());
    }

    /**
     * Creates an entitlement exception.
     *
     * @param errorCode Error code.
     * @param params Parameters for formatting the message string.
     * @param cause Root cause.
     */
    public RestException(int errorCode, Object[] params, Throwable cause)
    {
        super(cause);
        this.errorCode = errorCode;
        this.params = params;
        this.message = getLocalizedMessage(Locale.getDefault());
    }

    /**
     * Returns error code.
     * 
     * @return error code.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns exception message.
     *
     * @return exception message.
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Returns localized exception message.
     *
     * @return localized exception message.
     */
    @Override
    public String getLocalizedMessage() {
        return message;
    }

    /**
     * Returns localized exception message.
     *
     * @param locale Locale of the message.
     * @return localized exception message.
     */
    public String getLocalizedMessage(Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(RES_BUNDLE_NAME, locale);
        String msg = rb.getString(Integer.toString(errorCode));
        return (params != null) ? MessageFormat.format(msg, params) :
            msg;
    }
}