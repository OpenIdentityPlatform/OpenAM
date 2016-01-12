/*
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
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 */

package com.sun.identity.saml2.servlet;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.profile.FederatedSSOException;
import com.sun.identity.saml2.profile.IDPSSOFederate;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

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
        try {
            IDPSSOFederate.doSSOFederate(req, resp, resp.getWriter(), true, SAML2Constants.SOAP, null);
        } catch (FederatedSSOException e) {
            sendError(resp, e.getFaultCode(), e.getMessageCode(), e.getDetail());
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
            
        // handle DOS attack
        SAMLUtils.checkHTTPContentLength(req);
        try {
            IDPSSOFederate.doSSOFederate(req, resp, resp.getWriter(), true, SAML2Constants.SOAP, null);
        } catch (FederatedSSOException e) {
            sendError(resp, e.getFaultCode(), e.getMessageCode(), e.getDetail());
        }
    }

    private void sendError(HttpServletResponse response, String faultCode, String rbKey, String detail)
            throws IOException {
        try {
            SOAPMessage soapFault = SOAPCommunicator.getInstance().createSOAPFault(faultCode, rbKey, detail);
            if (soapFault != null) {
                //  Need to call saveChanges because we're
                // going to use the MimeHeaders to set HTTP
                // response information. These MimeHeaders
                // are generated as part of the save.
                if (soapFault.saveRequired()) {
                    soapFault.saveChanges();
                }
                response.setStatus(HttpServletResponse.SC_OK);
                SAML2Utils.putHeaders(soapFault.getMimeHeaders(), response);
                // Write out the message on the response stream
                try (OutputStream os = response.getOutputStream()) {
                    soapFault.writeTo(os);
                    os.flush();
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (SOAPException ex) {
            SAML2Utils.debug.error("IDPSingleSignOnServiceSOAP.sendError:" , ex);
        }

    }
}
