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
 * $Id: IDPSingleSignOnServiceSOAP.java,v 1.3 2009/10/14 23:59:44 exu Exp $
 *
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.saml2.servlet;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.profile.IDPSSOFederate;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

/**
 * This class <code>SPSingleSignOnServiceSOAP</code> receives and processes 
 * AuthnRequest using SOAP binding on IDP side.
 */
public class IDPSingleSignOnServiceSOAP extends HttpServlet {

    public void init() throws ServletException {
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
            
        // handle DOS attack
        SAMLUtils.checkHTTPContentLength(req);
        IDPSSOFederate.doSSOFederate(req, resp, resp.getWriter(), true, SAML2Constants.SOAP);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
            
        // handle DOS attack
        SAMLUtils.checkHTTPContentLength(req);
        IDPSSOFederate.doSSOFederate(req, resp, resp.getWriter(), true, SAML2Constants.SOAP);
    }
}
