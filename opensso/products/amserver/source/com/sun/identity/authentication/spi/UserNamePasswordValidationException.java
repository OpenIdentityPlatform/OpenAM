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
 * $Id: UserNamePasswordValidationException.java,v 1.2 2008/06/25 05:42:06 qcheng Exp $
 *
 */


package com.sun.identity.authentication.spi;

/**
 * This class is for handling Exception that is thrown when the user name
 * password validation plugin is failed or any invalid characters detected in
 * user name.
 *
 * @supported.all.api
 */
public class UserNamePasswordValidationException extends AuthLoginException {
    /**
     * Creates <code>UserNamePasswordValidationException</code> with the 
     * message from the given resource bundle, for a given error message key 
     * (<code>errorCode</code>) and using arguments to the message.
     * @param rbName Resource bundle name for the error message.
     * @param errorCode Key to the message in resource bundle.
     * @param args Arguments to the message.
     */
    public UserNamePasswordValidationException(
        String rbName,
        String errorCode,
        Object[] args) {
        super(rbName, errorCode, args);
    }
    
    /**
     * Creates <code>UserNamePasswordValidationException</code> with 
     * given message.
     * @param message English message for the exception.
     */
    public UserNamePasswordValidationException(String message) {
        super(message);
    }
    
    /**
     * Creates <code>UserNamePasswordValidationException</code> with given 
     * <code>Throwable</code>.
     * @param t the root cause of the exception
     */
    public UserNamePasswordValidationException(Throwable t) {
        super(t);
        
    }
}
