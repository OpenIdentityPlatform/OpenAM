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
 * $Id: FSSignatureException.java,v 1.2 2008/06/25 05:47:05 qcheng Exp $
 *
 */

package com.sun.identity.federation.services.util;

import com.sun.identity.federation.common.FSException;

/**
 * ID-FF signature related exceptions.
 */
public class FSSignatureException extends FSException {

    /**
     * Constructor.
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     */
    public FSSignatureException(String errorCode, Object[] args) {
        super(errorCode, args);
    }


    /**
     * Creates a <code>FSSignatureException</code> with a message.
     * @param s exception message
     */
    public FSSignatureException (String s) {
        super(s);
    }
    
    /**
     * Constructs a <code>FSSignatureException</code> with a message and
     * an embedded exception.
     *
     * @param rootCause  An embedded exception
     * @param s  Detailed message for this exception.
     */
    public FSSignatureException (Throwable rootCause, String s) {
        super(rootCause, s);
    }
}
