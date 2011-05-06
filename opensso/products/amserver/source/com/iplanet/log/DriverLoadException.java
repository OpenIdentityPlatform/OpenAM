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
 * $Id: DriverLoadException.java,v 1.3 2008/06/25 05:41:32 qcheng Exp $
 *
 */

package com.iplanet.log;

/**
 * Throw a log exception when loading the JDBC driver fails
 * @supported.all.api
 */
public class DriverLoadException extends LogException {
    /**
     * Constructs a <code>DriverLoadException</code> instance.
     */
    public DriverLoadException() {
        super();
    }

    /**
     * Constructs a <code>DriverLoadException</code> instance.
     *
     * @param msg Log exception message.
     */
    public DriverLoadException(String msg) {
        super(msg);
    }
}

