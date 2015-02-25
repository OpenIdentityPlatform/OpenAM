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
 * $Id: ValidationException.java,v 1.2 2008/06/25 05:53:07 qcheng Exp $
 *
 */

package com.sun.identity.shared.validation;

import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * Exception for violating data format.
 */
public class ValidationException
    extends L10NMessageImpl
{
    /**
     * Creates an instance of Validation Exception.
     * @param msg message of the exception
     */
    public ValidationException(String msg) {
        super(msg);
    }

    /**
     * Creates an instance of Validation Exception.
     * @param rbName Resource bundle name of the error message.
     * @param errorCode Key of the error message in the resource bundle.
     */
    public ValidationException(String rbName, String errorCode) {
        super(rbName, errorCode, (Object[])null);
    }
                                                                                
    /**
     * Creates an instance of Validation Exception.
     *
     * @param t Root cause of this exception.
     */
    public ValidationException(Throwable t) {
        super(t);
    }
}
