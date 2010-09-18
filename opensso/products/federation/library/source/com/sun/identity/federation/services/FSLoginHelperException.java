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
 * $Id: FSLoginHelperException.java,v 1.2 2008/06/25 05:46:55 qcheng Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.federation.common.FSException;

/**
 * This class handles all preLogin & postLogin exceptions.
 *
 * @see com.sun.identity.federation.common.FSException
 */
public class FSLoginHelperException extends FSException {

    /**
     * Constructor.
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     */
    public FSLoginHelperException(String errorCode, Object[] args) {
        super(errorCode, args);
    }

    /**
     * Constructs an <code>FSLoginHelperException</code> with a 
     * detailed message.
     *
     * @param msg Detailed message for this exception.
     */
    public FSLoginHelperException(String msg) {
        super(msg);
    }

    /**
     * Constructs an <code>FSLoginHelperException</code> with a message and
     * an embedded exception.
     *
     * @param msg  Detailed message for this exception.
     * @param t An embedded exception
     */
    public FSLoginHelperException(String msg, Throwable t) {
        super(t, msg);
    }

}
