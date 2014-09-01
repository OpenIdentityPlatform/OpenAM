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
 * $Id: ConfigManagerFatalException.java,v 1.2 2008/06/25 05:41:44 qcheng Exp $
 *
 */

package com.iplanet.ums;

// Imports

/**
 * This is a specific typed exception used to indicate some sort of fatal
 * ConfigManager issue. When the ConfigManager encounters any sort of situation
 * from which it cannot recover this, or a subclass of this, exception will be
 * thrown. This is the super class of all ConfigManager specific fatal
 * exceptions.
 * 
 * @see java.lang.RuntimeException
 * @see java.lang.Throwable
 */
public class ConfigManagerFatalException extends RuntimeException {
    /**
     * Constructs an <code>ConfigManagerFatalException</code> with no
     * specified detail message.
     */
    public ConfigManagerFatalException() {
        super();
    }

    /**
     * Constructs an <code>ConfigManagerFatalException</code> with the
     * specified detail message.
     * 
     * @param s
     *            the detail message.
     */
    public ConfigManagerFatalException(String s) {
        super(s);
    }
}
