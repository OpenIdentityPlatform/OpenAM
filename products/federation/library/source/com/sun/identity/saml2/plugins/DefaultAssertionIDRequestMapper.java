/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DefaultAssertionIDRequestMapper.java,v 1.2 2008/06/25 05:47:50 qcheng Exp $
 *
 */

package com.sun.identity.saml2.plugins;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;

/**
/**
 * This class <code>DefaultAssertonIDRequestMapper</code> is the default
 * implementation of the <code>AssertonIDRequestMapper</code> that is used by
 * asseriton ID request service to process assertion ID request.
 */ 
public class DefaultAssertionIDRequestMapper
    implements AssertionIDRequestMapper {

    /**
     * Checks if the assertion requester using URI binding is valid.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param samlAuthorityEntityID entity ID of SAML authority
     * @param role SAML authority role
     * @param realm the realm of hosted entity
     *
     * @exception SAML2Exception if the request is not valid. 
     */
    public void authenticateRequesterURI(HttpServletRequest request,
        HttpServletResponse response, String samlAuthorityEntityID,
        String role, String realm) throws SAML2Exception {

        // unsupported by default
        throw new SAML2Exception(SAML2Utils.bundle.getString(
            "invalidAssertionRequester"));
    }
}
