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
 * $Id: RestrictedTokenAction.java,v 1.2 2008/06/25 05:43:59 qcheng Exp $
 *
 */

package com.sun.identity.session.util;

/**
 * Utility interface to be used with RestrictedTokenContext.doUsing() for
 * performing actions with restricted tokens in a specific context used for
 * token restriction checking
 */

public interface RestrictedTokenAction {
    /**
     * Perform an arbitrary action which involves a restricted token in a
     * context provided by RestrictedTokenContext.doUsing()
     * 
     * @return a class-dependent value that may represent the results of the
     *         computation
     * @throws Exception
     *             an exceptional condition has occured
     */
    public Object run() throws Exception;
}
