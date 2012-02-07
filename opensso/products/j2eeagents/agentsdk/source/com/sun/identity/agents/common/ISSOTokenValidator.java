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
 * $Id: ISSOTokenValidator.java,v 1.3 2008/07/02 18:27:10 leiming Exp $
 *
 */

package com.sun.identity.agents.common;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.AgentException;

/**
 * The interface for SSOTokenValidator
 */
public interface ISSOTokenValidator {
    public abstract void initialize()
            throws AgentException;

    /**
     * Validates the given transport string and returns a SSOValidationResult
     * instance indicating the outcome of the validation process.
     *
     *
     * @param transportString that carries all the necessary information such
     * as SSO Token String, remote client address etc.
     *
     * @return a SSOValidationResult object indicating the outcome of the 
     * validation process.
     *
     * @throws AgentException in case of an unexpected error condition that
     * hinders the evaluation of the given transport string.
     */
    public abstract SSOValidationResult validate(String transportString)
            throws AgentException;

    public abstract SSOValidationResult validate(HttpServletRequest request);
    
    public SSOValidationResult validate(String ssoTokenID, 
            HttpServletRequest request); 

    public abstract String getSSOTokenValue(HttpServletRequest request);
    
    public abstract String getClientIPAddress(HttpServletRequest request);
    
    public abstract String getClientHostName(HttpServletRequest request);
    
    public abstract String getAppName(HttpServletRequest request);
    
    public static final String ATTRIBUTE_APPLICATION_NAME = "appname";
    public static final String ATTRIBUTE_HEADER_VALUE = "headerval";
    public static final String ATTRIBUTE_CLIENT_HOST = "clienthost";
}
