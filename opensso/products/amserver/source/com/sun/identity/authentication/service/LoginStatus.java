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
 * $Id: LoginStatus.java,v 1.2 2008/06/25 05:42:05 qcheng Exp $
 *
 */


package com.sun.identity.authentication.service;

/**
 * Class is representing status code for different login status
 */ 
public class LoginStatus {
    // possible values for LoginStatus NOT_STARTED,IN_PROGRESS,
    // SUCCESS , FAILURE

    /**
     *  Authentication not started
     */
    public static final int  AUTH_NOT_STARTED = 0;

    /**
     *  Authentication not started
     */
    public static final int AUTH_IN_PROGRESS = 2;

    /**
     *  Authentication in progress
     */
    public static final int AUTH_SUCCESS = 3;

    /**
     *  Authentication Failed
     */
    public static final int AUTH_FAILED = 4;

    /**
     *  Authentication completed - logout
     */
    public static final int AUTH_COMPLETED = 5;

    /**
     * Authentication failure  - error in service , module not setting token
     */
    public static final int AUTH_ERROR = 6;
    
    /**
     *  Authentication is reset
     */
    public static final int AUTH_RESET = 7;
    
    /**
     *  Organization mismatch
     */
    public static final int AUTH_ORG_MISMATCH = 8;

    /**
     *  Account Expired 
     */
    public static final int AUTH_ACCOUNT_EXPIRED = 9;

    /**
     *  Authentication not started
     */
    public int loginStatus = LoginStatus.AUTH_NOT_STARTED;

    /**
     * Returns Login Status.
     *
     * @return Login Status.
     */
    public int getStatus() {
        return loginStatus;
    }

    /**
     * Sets Login Status.
     *
     * @param status Login Status.
     */
    public void setStatus(int status) {
        loginStatus = status;
    }
}
