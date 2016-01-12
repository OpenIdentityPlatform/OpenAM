/*
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
 * $Id: AuthnQueryServiceSOAP.java,v 1.5 2009/06/12 22:21:41 mallas Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.saml2.servlet;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import com.sun.identity.saml2.common.SOAPCommunicator;
import org.w3c.dom.Element;


import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.AuthnQueryUtil;
import com.sun.identity.saml2.protocol.AuthnQuery;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;

/**
 * This class <code>AuthnQueryServiceSOAP</code> receives and processes 
 * authentication query request using SOAP binding.
 */
public class AuthnQueryServiceSOAP extends HttpServlet {

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

        // handle DOS attack
        SAMLUtils.checkHTTPContentLength(req);

        AuthnQuery authnQuery = null;

        try {
            SOAPMessage msg = SOAPCommunicator.getInstance().getSOAPMessage(req);
            Element elem = SOAPCommunicator.getInstance().getSamlpElement(msg,
                    SAML2Constants.AUTHN_QUERY);
            authnQuery =
                ProtocolFactory.getInstance().createAuthnQuery(elem);
        } catch (Exception ex) {
            SAML2Utils.debug.error(
                "AuthnQueryServiceSOAP.doGetPost:",  ex);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "failedToCreateAttributeQuery", ex.getMessage());
            return;
        }

        String authnAuthorityMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
            req.getRequestURI());


        String authnAuthorityEntityID = null;
        String realm = null;

        try {
            authnAuthorityEntityID =
                SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(
                authnAuthorityMetaAlias);

            realm = SAML2MetaUtils.getRealmByMetaAlias(authnAuthorityMetaAlias);
        } catch (SAML2Exception sme) {
            SAML2Utils.debug.error("AuthnQueryServiceSOAP.doGetPost", sme);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "invalidMetaAlias", sme.getMessage());
            return;
        }

        SOAPMessage replymsg = null;
        try {
            Response samlResp = AuthnQueryUtil.processAuthnQuery(
                authnQuery, req, resp, authnAuthorityEntityID, realm);
            replymsg = SOAPCommunicator.getInstance().createSOAPMessage(
                    samlResp.toXMLString(true, true), false);
        } catch (Throwable t) {
            SAML2Utils.debug.error("AuthnQueryServiceSOAP.doGetPost: " +
                "Unable to create SOAP message:", t);
            replymsg = SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.SERVER_FAULT,
                    "unableToCreateSOAPMessage", null);
        }

        try {
            if (replymsg.saveRequired()) {
                replymsg.saveChanges();
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            SAML2Utils.putHeaders(replymsg.getMimeHeaders(), resp);
            OutputStream os = resp.getOutputStream();
            replymsg.writeTo(os);
            os.flush();
        } catch (SOAPException soap) {
            SAML2Utils.debug.error("AuthnQueryServiceSOAP.doGetPost", soap);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "soapError", soap.getMessage());
            return;
        }
    }
}
