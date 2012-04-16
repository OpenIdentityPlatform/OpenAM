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
 * $Id: AssertionIDRequestServiceURI.java,v 1.5 2009/10/14 23:59:44 exu Exp $
 *
 */

package com.sun.identity.saml2.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.AssertionIDRequestUtil;

/**
 * This class <code>AssertionIDRequestServiceURI</code> receives and processes
 * assertion ID request using URI binding.
 */
public class AssertionIDRequestServiceURI extends HttpServlet {

    public void init() throws ServletException {
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doGetPost(req, resp);
    }
            
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doGetPost(req, resp);
    }

    private void doGetPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "AssertionIDRequestServiceSOAP.doGetPost: " +
                    "pathInfo is null.");
            }
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "nullPathInfo", SAML2Utils.bundle.getString("nullPathInfo"));
            return;
        }

        String role = null;
        int index = pathInfo.indexOf(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        if (index > 2) {
            role = pathInfo.substring(1, index -1);
        }

        String samlAuthorityMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
            req.getRequestURI());


        String samlAuthorityEntityID = null;
        String realm = null;

        try {
            samlAuthorityEntityID =
                SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(
                samlAuthorityMetaAlias);

            realm = SAML2MetaUtils.getRealmByMetaAlias(samlAuthorityMetaAlias);
        } catch (SAML2Exception sme) {
            SAML2Utils.debug.error("AssertionIDRequestServiceSOAP.doGetPost",
                sme);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "invalidMetaAlias", sme.getMessage());
            return;
        }

        if (!SAML2Utils.isIDPProfileBindingSupported(
            realm, samlAuthorityEntityID,
            SAML2Constants.ASSERTION_ID_REQUEST_SERVICE, SAML2Constants.URI))
	{
            SAML2Utils.debug.error(
		"AssertionIDRequestServiceURI:Assertion ID request" +
		" service URI binding is not supported for " +
		samlAuthorityEntityID);
            SAMLUtils.sendError(req, resp,
		HttpServletResponse.SC_BAD_REQUEST,
		"unsupportedBinding",
		SAML2Utils.bundle.getString("unsupportedBinding"));
            return;
	}

        AssertionIDRequestUtil.processAssertionIDRequestURI(req, resp,
            samlAuthorityEntityID, role, realm);
    }
}
