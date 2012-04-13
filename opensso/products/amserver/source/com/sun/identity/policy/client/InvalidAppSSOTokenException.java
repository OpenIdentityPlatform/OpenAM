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
 * $Id: InvalidAppSSOTokenException.java,v 1.2 2008/06/25 05:43:46 qcheng Exp $
 *
 */




package com.sun.identity.policy.client;
import com.sun.identity.policy.PolicyException;

/**
 * Exception thrown if Application <code>SSOToken</code> is invalid.
 * Application SSO Token is used while communicating with policy service 
 * as a means of authenticating the client Policy Enforcement Point
 * @supported.all.api
 */
public class InvalidAppSSOTokenException extends PolicyException {


    /**
     * Constructs <code>InvalidAppSSOTokenException</code>
     * @param msg exception message
     */
    public InvalidAppSSOTokenException(String msg) {
	super(msg);
    }

    /**
     * Constructs <code>InvalidAppSSOTokenException</code>
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting, 
     * could be <code>null</code>
     */
    public InvalidAppSSOTokenException(String rbName, String errCode, 	
	Object[] args) {
        super(rbName, errCode, args,null);
    }

    /**
     * Constructs <code>InvalidAppSSOTokenException</code>
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting,
     * could be <code>null</code>
     * @param t nested root cause exception
     */
    public InvalidAppSSOTokenException(String rbName, String errCode, 	
	Object[] args, Throwable t) 
    {
        super(rbName, errCode, args,t);
    }

}
