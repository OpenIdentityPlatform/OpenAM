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
 * $Id: IDPECPSessionMapper.java,v 1.3 2008/12/03 00:34:10 hengming Exp $
 *
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Exception;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interface <code>IDPECPSessionMapper</code> is used to find a valid
 * session from HTTP servlet request on IDP with ECP profile.
 *
 * @supported.all.api
 */ 
public interface IDPECPSessionMapper {

    /**
     * Returns user valid session.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return a vaild user session or null if not found
     * @exception SAML2Exception if error occurs. 
     */
    public Object getSession(HttpServletRequest request,
        HttpServletResponse response) throws SAML2Exception;
}
