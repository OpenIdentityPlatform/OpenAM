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
 * $Id: DataConstraintException.java,v 1.3 2008/06/25 05:41:48 qcheng Exp $
 *
 */

package com.iplanet.ums.validation;

import com.iplanet.ums.UMSException;

/**
 * Exception thrown if data validation fails. The validating data passed to
 * validating functions is checked against optional rules and validating rules
 * specific to the validator classes and this exception is thrown when it
 * doesnot satify those rules.
 *
 * @supported.all.api
 */
public class DataConstraintException extends UMSException {

    /**
     * Constructor to constuct the exception without any message
     */
    public DataConstraintException() {
    }

    /**
     * Constructor to construct the exception with a message
     * 
     * @param msg
     *            message describing the cause of the exception
     */
    public DataConstraintException(String msg) {
        super(msg);
    }

    /**
     * Constructs the exception with a message and an embedded exception
     * 
     * @param msg
     *            the message explaining the exception
     * @param rootCause
     *            the embedded exception, the exception that led to this
     *            exception.
     */
    public DataConstraintException(String msg, Throwable rootCause) {
        super(msg, rootCause);
    }
}
