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
 * $Id: IDPManageNameIDServiceSOAP.java,v 1.3 2009/06/12 22:21:41 mallas Exp $
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.xml.soap.SOAPException;


/**
 * This class <code>IDPManageNameIDServiceSOAP</code> receives and processes 
 * Manage NameID request using SOAP binding on IDP side.
 */
public class IDPManageNameIDServiceSOAP extends HttpServlet {

    public void init() throws ServletException {
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        try {
            Map paramsMap = new HashMap();
            paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);
            DoManageNameID.processSOAPRequest(request, response, paramsMap);
        } catch (SAML2Exception ex) {
            SAML2Utils.debug.error("IDPManageNameIDServiceSOAP.doPost:", ex);
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "requestProcessingMNIError", ex.getMessage());
            return;
        } catch (SOAPException se) {
            SAML2Utils.debug.error("IDPManageNameIDServiceSOAP.doPost:", se);
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "requestProcessingMNIError", se.getMessage());
            return;
        }
    }
}
