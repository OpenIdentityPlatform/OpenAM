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
 * $Id: AMConfigurationException.java,v 1.4 2008/06/25 05:41:52 qcheng Exp $
 *
 */

package com.sun.identity.authentication.config;

import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * Exception that is thrown when there are error in manipulating authentication
 * configuration.
 *
 */
public class AMConfigurationException extends L10NMessageImpl {

    /**
     * Constructor.
     *
     * @param msg message of the exception
     */
    public AMConfigurationException(String msg) {
        super(msg);
    }

    /**
     * Constructor.
     *
     * @param rbName Resource bundle name of the error message.
     * @param errorCode Key of the error message in the resource bundle.
     */ 
    public AMConfigurationException(String rbName, String errorCode) {
        super(rbName, errorCode, (Object[])null);
    }

    /**
     * Constructor.
     *
     * @param rbName Resource bundle name of the error message.
     * @param errorCode Key of the error message in the resource bundle.
     * @param params parameters to the error message.
     */ 
    public AMConfigurationException(String rbName, String errorCode, 
        Object[] params) {
        super(rbName, errorCode, params);
    }
 
    /**
     * Constructor.
     *
     * @param t Root cause of this exception.
     */
    public AMConfigurationException(Throwable t) {
        super(t);
    }
}
