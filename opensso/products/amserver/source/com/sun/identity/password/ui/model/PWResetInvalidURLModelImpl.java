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
 * $Id: PWResetInvalidURLModelImpl.java,v 1.2 2008/06/25 05:43:42 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.password.ui.model;

/**
 * <code>PWResetInvalidURLModelImpl</code> contains a set of methods required by
 * <code>PWResetInvalidURLViewBean</code>.
 */
public class PWResetInvalidURLModelImpl extends PWResetModelImpl
    implements PWResetInvalidURLModel {

    /**
     * Creates a invalid URL model implementation object.
     *
     */
    public PWResetInvalidURLModelImpl() {
        super();
    }

    /**
     * Returns invalid URL title.
     *
     * @return invalid URL title.
     */
    public String getInvalidURLTitle() {
        return getLocalizedString("invalidURL.title");
    }

    /**
     * Returns invalid URL message.
     *
     * @return invalid URL message.
     */
    public String getInvalidURLMessage() {
        return getLocalizedString("invalidURL.message");
    }
}
