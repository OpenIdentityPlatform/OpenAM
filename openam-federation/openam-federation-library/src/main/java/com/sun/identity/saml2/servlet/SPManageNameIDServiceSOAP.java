/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: SPManageNameIDServiceSOAP.java,v 1.3 2009/06/12 22:21:41 mallas Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.saml2.servlet;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.profile.DoManageNameID;
import com.sun.identity.saml.common.SAMLUtils;
import java.io.IOException;
import java.util.Map; 
import java.util.HashMap; 
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.ServletException;
import jakarta.xml.soap.SOAPException;


/**
 * This class <code>SPManageNameIDServiceSOAP</code> receives and processes 
 * Manage NameID request using SOAP binding on SP side.
 */
public class SPManageNameIDServiceSOAP extends HttpServlet {

    public void init() throws ServletException {
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        try {
            Map paramsMap = new HashMap();
            paramsMap.put(SAML2Constants.ROLE, SAML2Constants.SP_ROLE);
            DoManageNameID.processSOAPRequest(request, response, paramsMap);
        } catch (SAML2Exception ex) {
            SAML2Utils.debug.error("SPManageNameIDServiceSOAP.doPost:", ex);
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "requestProcessingMNIError", ex.getMessage());
            return;
        } catch (SOAPException se) {
            SAML2Utils.debug.error("SPManageNameIDServiceSOAP.doPost:", se);
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "requestProcessingMNIError", se.getMessage());
            return;
        }
    }
}
