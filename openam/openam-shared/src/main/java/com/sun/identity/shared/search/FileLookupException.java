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
 * $Id: FileLookupException.java,v 1.2 2008/06/25 05:53:05 qcheng Exp $
 *
 */

package com.sun.identity.shared.search;

/**
 * Exception for failure in file lookup.
 */
public class FileLookupException extends Exception {
    /**
     * Default Constructor.
     */
    public FileLookupException() {
        super();
    }

    /**
     * Creates an instance of this exception.
     *
     * @param msg Exception message.
     */
    public FileLookupException(String msg) {
        super(msg);
    }
}
