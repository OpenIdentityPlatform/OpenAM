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
 * $Id: FSMsgException.java,v 1.2 2008/06/25 05:46:47 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message.common;

import com.sun.identity.federation.common.FSException;

/**
 * This exception is thrown when there are failures
 * during the federation of an identity.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSMsgException extends FSException {
    
    /**
     * Constructor
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     */
    public FSMsgException(String errorCode, Object[] args) {
        super(errorCode, args);
    }
    
    /**
     * Constructor
     *
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     * @param rootCause  An embedded exception
     */
    public FSMsgException(String errorCode,Object[] args,Throwable rootCause) {
        super(errorCode, args, rootCause);
    }
    
     /**
     * Constructor
     *
     * @param rootCause  An embedded exception
     * @param s the exception message.
     */
    public FSMsgException(Throwable rootCause,String s) {
        super(rootCause, s);
    }
    
    /**
     * Constructor
     *
     * @param s the exception message.
     */
    public FSMsgException(String s) {
        super(s);
    }
}
