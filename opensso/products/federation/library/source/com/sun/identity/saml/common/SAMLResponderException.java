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
 * $Id: SAMLResponderException.java,v 1.2 2008/06/25 05:47:34 qcheng Exp $
 *
 */


package com.sun.identity.saml.common;

/**
 * This exception is thrown when the request could not be performed
 * due to an error at the receiving end.
 *
 * @supported.all.api
 */
public class SAMLResponderException extends SAMLException {

    /**
     * Constructs an <code>SAMLResponderException</code> with a message.
     *
     * @param s exception message.
     */
    public SAMLResponderException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>SAMLResponderException</code> with given
     * <code>Throwable</code>.
     *
     * @param t Exception nested in the new exception.
     *
     */
    public SAMLResponderException(Throwable t) {
        super(t);
    }

    /**
     * Constructs a new <code>SAMLResponderException</code> without a nested
     * <code>Throwable</code>.
     * @param rbName Resource Bundle Name to be used for getting
     * localized error message.
     * @param errorCode Key to resource bundle. You can use
     * <pre>
     * ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     * String localizedStr = rb.getString(errorCode);
     * </pre>
     * @param args arguments to message. If it is not present pass them
     * as null
     *
     */
    public SAMLResponderException(String rbName,
                                String errorCode,
                                Object[] args)
    {
        super(rbName, errorCode, args);
    }
}
