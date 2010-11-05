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
 * $Id: AuthenticationServiceNameProvider.java,v 1.2 2008/06/25 05:44:03 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import java.util.Set;

/**
 * Allows the service management subsystem to identify the names of
 * authentication module services loaded by default.
 */
public interface AuthenticationServiceNameProvider {

    /**
     * Provides a collection of authentication module service names that are
     * loaded by default.
     * 
     * @return a <code>Set</code> of authentication module service names.
     */
    public Set getAuthenticationServiceNames();
}
