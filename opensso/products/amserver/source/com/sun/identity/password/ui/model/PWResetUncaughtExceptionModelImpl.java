/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PWResetUncaughtExceptionModelImpl.java,v 1.2 2008/06/25 05:43:43 qcheng Exp $
 *
 */

package com.sun.identity.password.ui.model;

/**
 * <code>PWResetUncaughtExceptionModelImpl</code> contains a set of methods 
 * required by <code>PWResetUncaughtExceptionViewBean</code>.
 */
public class PWResetUncaughtExceptionModelImpl extends PWResetModelImpl
    implements PWResetUncaughtExceptionModel {

    /**
     * Creates a uncaught exception model implementation object
     *
     */
    public PWResetUncaughtExceptionModelImpl() {
        super();
    }

    /**
     * Returns uncaught exception title.
     *
     * @return uncaught exception title.
     */
    public String getErrorTitle() {
        return getLocalizedString("uncaughtException.title");
    }

    /**
     * Returns uncaught exception message.
     *
     * @return uncaught exception message.
     */
    public String getErrorMessage() {
        return getLocalizedString("uncaughtException.message");
    }
}
