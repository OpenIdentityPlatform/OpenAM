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
 * $Id: AMSecurityPropertiesException.java,v 1.0 2006/12/21 21:21:14
 * manish_rustagi Exp $
 *
 */

package com.sun.identity.security;

/**
 * Exception that is thrown when AMConfig.properties does not contain
 * com.sun.identity.agents.app.username and com.iplanet.am.service.password
 */

public class AMSecurityPropertiesException extends RuntimeException {

    /**
     * Constructs an <code>AMSecurityPropertiesException</code> object.
     * @param message English message for the exception.
     */
    public AMSecurityPropertiesException(String message) {
        super(message);
    }
}
