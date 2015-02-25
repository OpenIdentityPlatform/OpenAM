/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IDPSingleLogoutServiceSOAP.java,v 1.10 2009/10/14 23:59:44 exu Exp $
 *
 */


package com.sun.identity.saml2.servlet;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.LogoutUtil;
import com.sun.identity.saml2.profile.IDPProxyUtil;
import com.sun.identity.saml2.profile.IDPSession; 
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPSingleLogout;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml.common.SAMLUtils;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List; 
import java.util.ArrayList;
import java.util.Map; 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Element;


/**
 * This class <code>IDPSingleLogoutServiceSOAP</code> receives and processes 
 * single logout request using SOAP binding on IDP side.
 */
public class IDPSingleLogoutServiceSOAP extends HttpServlet {

    public void init() throws ServletException {
    }

    public void doPost(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws ServletException, IOException {

        try {
            // handle DOS attack
            SAMLUtils.checkHTTPContentLength(req);
            // Get IDP entity ID
            String idpMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
                req.getRequestURI());
            String idpEntityID = SAML2Utils.getSAML2MetaManager().
                getEntityByMetaAlias(idpMetaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
            if (!SAML2Utils.isIDPProfileBindingSupported(
                realm, idpEntityID, SAML2Constants.SLO_SERVICE,
                SAML2Constants.SOAP))
            {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "unsupportedBinding"));
            }
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("IDPSLOSOAP.doPost : uri =" + 
                    req.getRequestURI() +", idpMetaAlias=" + idpMetaAlias
                    + ", idpEntityID=" + idpEntityID);
            }
            // Get all the headers from the HTTP request
            MimeHeaders headers = SAML2Utils.getHeaders(req);
            // Get the body of the HTTP request
            InputStream is = req.getInputStream();
            // Now internalize the contents of a HTTP request
            // and create a SOAPMessage
            SOAPMessage msg =
                MessageFactory.newInstance().createMessage(headers, is);
            Map aMap = IDPProxyUtil.getSessionPartners(msg);
            List partners = (List) aMap.get(SAML2Constants.PARTNERS); 
            SOAPMessage reply = null;
            reply = onMessage(msg, req, resp, idpEntityID, realm);
            if (reply != null) {    
                // IDP Proxy case
                if (partners != null &&  (!partners.isEmpty())) {
                    Element reqElem = SAML2Utils.getSamlpElement(msg,
                        "LogoutRequest");
                    LogoutRequest logoutReq =
                        ProtocolFactory.getInstance().createLogoutRequest(
                        reqElem);
                    IDPCache.SOAPMessageByLogoutRequestID.put(
                        logoutReq.getID(), reply); 
                    IDPProxyUtil.sendProxyLogoutRequestSOAP(req, resp,
                        reply, partners, (IDPSession) aMap.get(
                        SAML2Constants.IDP_SESSION));
                } else {
                    //  Need to call saveChanges because we're
                    // going to use the MimeHeaders to set HTTP
                    // response information. These MimeHeaders
                    // are generated as part of the save.
    
                   if (reply.saveRequired()) {
                       reply.saveChanges();
                   }
                   resp.setStatus(HttpServletResponse.SC_OK);
                   SAML2Utils.putHeaders(reply.getMimeHeaders(), resp);
                   // Write out the message on the response stream
                   OutputStream os = resp.getOutputStream();
                   reply.writeTo(os);
                   os.flush();
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (SAML2Exception ex) {
            SAML2Utils.debug.error("IDPSingleLogoutServiceSOAP", ex);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "singleLogoutFailed", ex.getMessage());
            return;
        } catch (SOAPException soap) {
            SAML2Utils.debug.error("IDPSingleLogoutServiceSOAP", soap);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "singleLogoutFailed", soap.getMessage());
            return;
        }
    }

    /**
     * Process the incoming SOAP message containing the LogoutRequest and
     * generates outgoing SOAP message containing the LogoutResponse on IDP 
     * side.
     * @param message incoming SOAP message.
     * @param request HTTP servlet request.
     * @param response HTTP servlet response.
     * @param idpEntityID Entity ID of the hosted IDP.
     * @param realm realm of this hosted IDP.
     * @return SOAP message containing the outgoing LogoutResponse.
     */
    public SOAPMessage onMessage(
        SOAPMessage message,
        HttpServletRequest request,
        HttpServletResponse response,
        String idpEntityID,
        String realm) {
        
        SAML2Utils.debug.message("IDPSingleLogoutServiceSOAP.onMessage: init");
        // get LogoutRequest element from SOAP message
        LogoutRequest logoutReq = null;
        try {
            Element reqElem = SAML2Utils.getSamlpElement(message, 
                "LogoutRequest");
            logoutReq = 
                ProtocolFactory.getInstance().createLogoutRequest(reqElem);
            // delay the signature until this server finds the session
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("IDPSingleLogoutServiceSOAP.onMessage: " + 
                "unable to get LogoutRequest from message", se);
            return SAML2Utils.createSOAPFault(SAML2Constants.CLIENT_FAULT, 
                "errorLogoutRequest", se.getMessage());
        }

        // TODO decrypt LogoutRequest if any
        // TODO, handle signature verification

        if (logoutReq == null) {
            SAML2Utils.debug.error("IDPSingleLogoutServiceSOAP.onMessage: " + 
                "LogoutRequest is null");
            return SAML2Utils.createSOAPFault(SAML2Constants.CLIENT_FAULT, 
                "nullLogoutRequest", null);
        }

        LogoutResponse loRes = null;
        try {
            // process LogoutRequestElement
            loRes = IDPSingleLogout.processLogoutRequest(logoutReq, request, 
                response, SAML2Constants.SOAP, null, idpEntityID, realm, false);
            LogoutUtil.signSLOResponse(loRes, realm, idpEntityID, 
                    SAML2Constants.IDP_ROLE, logoutReq.getIssuer().getValue());
        } catch (SAML2Exception e) {
            SAML2Utils.debug.error("IDPSingleLogoutServiceSOAP.onMessage;", e);
            return SAML2Utils.createSOAPFault(SAML2Constants.SERVER_FAULT,
                "errorLogoutResponse", e.getMessage());
        }

        if (loRes == null) {
            SAML2Utils.debug.error("IDPSingleLogoutServiceSOAP.onMessage: " + 
                "LogoutResponse is null");
            return SAML2Utils.createSOAPFault(SAML2Constants.SERVER_FAULT,
                "errorLogoutResponse", null);
        }

        SOAPMessage msg = null; 
        try {
            msg = SAML2Utils.createSOAPMessage(loRes.toXMLString(true, true),
                false);
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("IDPSingleLogoutServiceSOAP.onMessage: " +
                "Unable to create SOAP message:", se);
            return SAML2Utils.createSOAPFault(SAML2Constants.SERVER_FAULT,
                "errorLogoutResponseSOAP", se.getMessage());
        } catch (SOAPException ex) {
            SAML2Utils.debug.error("IDPSingleLogoutServiceSOAP.onMessage: " +
                "Unable to create SOAP message:", ex);
            return SAML2Utils.createSOAPFault(SAML2Constants.SERVER_FAULT,
                "errorLogoutResponseSOAP", ex.getMessage());
        }
        return msg;
    }
}
