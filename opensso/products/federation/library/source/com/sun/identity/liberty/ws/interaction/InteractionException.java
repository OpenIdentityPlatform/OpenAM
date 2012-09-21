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
 * $Id: InteractionException.java,v 1.2 2008/06/25 05:47:17 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.interaction;

import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * Base class for exceptions that could be thrown from
 * <code>InteractionService</code> framework.
 *
 * @supported.all.api
 */
public class InteractionException extends L10NMessageImpl {

    /**
     * Constructor
     * @param message message for the exception
     */
    public InteractionException(String message) {
        super(message);
    }

    /**
     * Constructor
     * @param nestedException <code>Throwable</code> nested in this exception.
     */
    public InteractionException(Throwable nestedException) {
        super(nestedException);
    }

    /**
     * Constructor
     * Constructs an instance of <code> InteractionException </code> to 
     * pass the localized error message
     *
     * At this level, the locale of the caller is not known and it is
     * not possible to throw localized error message.
     * Instead this constructor provides Resource Bundle name and error code
     * for correctly locating the error message. The default
     * <code>getMessage()</code> will always return English messages only. This
     * is consistent with current JRE.
     *
     * @param rbName Resource Bundle Name to be used for getting 
     * localized error message.
     *
     * @param errorCode  Key to resource bundle. You can use 
     * <pre>
     * ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     * String localizedStr = rb.getString(errorCode);
     * </pre>
     *
     * @param args  arguments to message. If it is not present pass them
     * as null 
     */
    public InteractionException(String rbName, String errorCode,
            Object[] args) {
        super(rbName, errorCode, args);
    }

}
