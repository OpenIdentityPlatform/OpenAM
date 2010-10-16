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
 * $Id: MessageLoginException.java,v 1.2 2008/06/25 05:42:06 qcheng Exp $
 *
 */



package com.sun.identity.authentication.spi;

/**
 * Exception that is thrown when enter a Login Module error state which
 * does not contain a error template. The exception message is the
 * value for the header attribute in the Callbacks element
 */

public class MessageLoginException extends AuthLoginException {
    /**
     * Creates <code>MessageLoginException</code> object.
     *
     * @param rbName Resource bundle name for the error message.
     * @param errorCode Key to the message in resource bundle.
     * @param args Arguments to the message.
     */
    public MessageLoginException(
        String rbName,
        String errorCode,
        Object[] args) {
        super(rbName, errorCode, args);
    }

    /**
     * Constructor.
     *
     * @param message English message for the exception.
     */
    public MessageLoginException(String message) {
        super(message);
    }
}
