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
 * $Id: ServiceNotFoundException.java,v 1.3 2008/06/25 05:44:05 qcheng Exp $
 *
 */

package com.sun.identity.sm;

/**
 * The <code>ServiceNotFoundException</code> is thrown if the service does not
 * exist.
 * 
 * @see java.lang.Exception
 * @see java.lang.Throwable
 *
 * @supported.all.api
 */
public class ServiceNotFoundException extends SMSException {
    /**
     * Constructs an <code>ServiceNotFoundException</code> with no specified
     * detail message.
     */
    public ServiceNotFoundException() {
        super();
    }

    /**
     * Constructs an <code>ServiceNotFoundException</code> with the specified
     * detail message.
     * 
     * @param s
     *            the detail message.
     */
    public ServiceNotFoundException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>ServiceNotFoundException</code> with the specified
     * error code. It can be used to pass localized error message.
     * 
     * @param rbName
     *            Resource Bundle name where localized error message is located.
     * @param errCode
     *            error code or message ID to be used for
     *            <code>ResourceBundle.getString()</code> to locate error
     *            message
     * @param args
     *            any arguments to be used for error message formatting
     *            <code>getMessage()</code> will construct error message using
     *            English resource bundle.
     */
    public ServiceNotFoundException(String rbName, String errCode,
            Object[] args) 
    {
        super(rbName, errCode, args);
    }
}
