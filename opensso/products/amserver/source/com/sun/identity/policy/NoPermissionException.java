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
 * $Id: NoPermissionException.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */


package com.sun.identity.policy;

/**
 * Exception thrown if a policy operation attempted could not be 
 * done due to insufficient permissions
 *
 * @supported.all.api
 */
public class NoPermissionException extends PolicyException {

    /**
     * No argument constructor
     */
    public NoPermissionException() {
        super("");
        fillInStackTrace();
    }

    /**
     * Constructor
     * @param msg exception message
     */
    public NoPermissionException(String msg) {
        super(msg);
        fillInStackTrace();
    }

    /**
     * Constructor
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting. null if no
     * additional arguments are necessary
     */
    public NoPermissionException(String rbName, String errCode,Object[] args) 
    {
        super(rbName, errCode, args,null);
        fillInStackTrace();
    }

    /**
     * Constructor
     * @param rbName Resource bundle name of exception message
     * @param errCode id used with resource bundle to get error message
     * @param args any arguments for message formatting. null if no
     * additional arguments are necessary
     * @param t nested root cause exception
     */
    public NoPermissionException(String rbName, String errCode,Object[] args,
        Throwable t) 
    {
        super(rbName, errCode, args,t);
        fillInStackTrace();
    }

    /**
     * Constructor
     * @param t nested root cause exception
     */
    public NoPermissionException(Throwable t) {
        super(t);
        fillInStackTrace();
    }
}
