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
 * $Id: FSAccountMgmtException.java,v 1.3 2008/06/25 05:46:39 qcheng Exp $
 *
 */

package com.sun.identity.federation.accountmgmt;

import com.sun.identity.federation.common.FSException;

/**
 * This exception class handles Account Mangement errors.
 */
public class FSAccountMgmtException extends FSException {
    /**
     * Constructor
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     */
    public FSAccountMgmtException(String errorCode, Object[] args) {
        super(errorCode, args);
    }

    /**
     * Constructor
     *
     * This constructor builds an <code>FSAccountMgmtException</code> with
     * the specified detail message.
     * @param msg the detail message.
     */
    public FSAccountMgmtException(String msg) {
        super(msg);
    }
    
}
