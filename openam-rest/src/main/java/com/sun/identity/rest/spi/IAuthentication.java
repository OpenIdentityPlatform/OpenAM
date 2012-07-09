/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IAuthentication.java,v 1.1 2009/10/21 01:11:02 veiming Exp $
 *
 */

package com.sun.identity.rest.spi;

import javax.servlet.Filter;

/**
 * Implements this interface to do REST permission check.
 */
public interface IAuthentication extends Filter {
    /**
     * Query parameter for passing administrator (caller) Single Sign-On
     * token string.
     */
    String PARAM_ADMIN = "admin";

    /**
     * Returns the accept authentication method
     *
     * @return the accept authentication method
     */
    String[] accept();
}
