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
 * $Id: LogException.java,v 1.2 2008/06/25 05:47:27 qcheng Exp $
 *
 */

package com.sun.identity.plugin.log;

import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * This class is to handle Log related exceptions.
 *
 * @supported.all.api
 */

public class LogException extends L10NMessageImpl {
    
    /**
     * Constructs a <code>LogException</code> with a detailed
     * message.
     *
     * @param message detailed message for this exception.
     */
    public LogException(String message) {
        super(message);
    }
    
    /**
     * Constructs a <code>LogException</code> with
     * an embedded exception.
     *
     * @param rootCause An embedded exception
     */
    public LogException(Throwable rootCause) {
        super(rootCause);
    }
    
    /**
     * Constructs a <code>LogException</code> with an exception.
     *
     * @param ex an exception
     */
    public LogException(Exception ex) {
        super(ex);
    }
    
    /**
     * Constructs a new <code>LogException</code> without a nested
     * <code>Throwable</code>.
     * @param rbName Resource Bundle Name to be used for getting
     *  localized error message.
     * @param errorCode Key to resource bundle. You can use
     * <pre>
     * ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     * String localizedStr = rb.getString(errorCode);
     * </pre>
     * @param args arguments to message. If it is not present pass them
     *  as null
     *
     */
    public LogException(String rbName,String errorCode,Object[] args) {
        super(rbName, errorCode, args);
    }
}
