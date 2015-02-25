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
 * $Id: SPSingleLogoutServiceSOAP.java,v 1.9 2009/10/14 23:59:45 exu Exp $
 *
 */


package com.sun.identity.saml2.servlet;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.LogoutUtil;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.profile.SPSingleLogout;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml.common.SAMLUtils;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Element;

/**
 * This class <code>SPSingleLogoutServiceSOAP</code> receives and processes 
 * single logout request using SOAP binding on SP side.
 */
public class SPSingleLogoutServiceSOAP extends HttpServlet {

    public void init() throws ServletException {
    }

    public void doPost(
        HttpServletRequest req,
        HttpServletResponse resp)
        throws ServletException, IOException {
            
        try {
            // handle DOS attack
            SAMLUtils.checkHTTPContentLength(req);
            // Get SP entity ID
            String spMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
                req.getRequestURI());
            if (SPCache.isFedlet) {
                if ((spMetaAlias ==  null) || (spMetaAlias.length() == 0)) {
                    // pick the first available one
                    List spMetaAliases = SAML2Utils.getSAML2MetaManager().
                        getAllHostedServiceProviderMetaAliases("/");
                    if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                        // get first one
                        spMetaAlias = (String) spMetaAliases.get(0);
                    }
                }
            }
            String spEntityID = SAML2Utils.getSAML2MetaManager().
                getEntityByMetaAlias(spMetaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(spMetaAlias);
            if (!SAML2Utils.isSPProfileBindingSupported(
                realm, spEntityID, SAML2Constants.SLO_SERVICE,
                SAML2Constants.SOAP))
            {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("unsupportedBinding"));
            }
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPSLOSOAP.doPost : uri =" +
                    req.getRequestURI() +", spMetaAlias=" + spMetaAlias
                    + ", spEntityID=" + spEntityID);
            }
            
            // Get all the headers from the HTTP request
            MimeHeaders headers = SAML2Utils.getHeaders(req);
            // Get the body of the HTTP request
            InputStream is = req.getInputStream();
            // Now internalize the contents of a HTTP request
            // and create a SOAPMessage
            SOAPMessage msg =
                SAML2Utils.mf.createMessage(headers, is);
            SOAPMessage reply = null;
            reply = onMessage(msg, req, resp, realm, spEntityID);
            if (reply != null) {
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
            } else {
                // Form SOAP fault
                resp.setStatus( HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (SAML2Exception ex) {
            SAML2Utils.debug.error("SPSingleLogoutServiceSOAP", ex);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "singleLogoutFailed", ex.getMessage());
            return;
        } catch (SOAPException soap) {
            SAML2Utils.debug.error("SPSingleLogoutServiceSOAP", soap);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "singleLogoutFailed", soap.getMessage());
            return;
        }
    }

    /**
     * Process the incoming SOAP message containing the LogoutRequest and
     * generates outgoing SOAP message containing the LogoutResponse on SP side.
     * @param message incoming SOAP message.
     * @param request HTTP servlet request.
     * @param response HTTP servlet response.
     * @param realm realm of the hosted SP.
     * @param spEntityID Entity ID of the hosted SP.
     * @return SOAP message containing the outgoing LogoutResponse.
     */
    public SOAPMessage onMessage(
        SOAPMessage message,
        HttpServletRequest request,
        HttpServletResponse response,
        String realm,
        String spEntityID) {
        
        SAML2Utils.debug.message("SPSLOServiceSOAP.onMessage: starting");
        LogoutRequest logoutReq = null;
        String tmpStr = request.getParameter("isLBReq");
        boolean isLBReq = (tmpStr == null || !tmpStr.equals("false"));
        try {
            Element reqElem = SAML2Utils.getSamlpElement(message, 
                "LogoutRequest");
            logoutReq = 
                ProtocolFactory.getInstance().createLogoutRequest(reqElem);
            // delay the signature validation until it finds the session
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("SPSingleLogoutServiceSOAP.onMessage: " +
                "unable to get LogoutRequest from message", se);
            return SAML2Utils.createSOAPFault(SAML2Constants.CLIENT_FAULT,
                "errorLogoutRequest", se.getMessage());
        }

        if (logoutReq == null) {
            SAML2Utils.debug.error("SPSLOServiceSOAP.onMessage: null request");
            return SAML2Utils.createSOAPFault(SAML2Constants.CLIENT_FAULT,
                "nullLogoutRequest", null);
        }

        // process LogoutRequestElement
        LogoutResponse loRes = 
            SPSingleLogout.processLogoutRequest(logoutReq, spEntityID, realm,
                request, response, isLBReq, SAML2Constants.SOAP, false);

        if (loRes == null) {
            SAML2Utils.debug.error("SPSLOSOAP.onMessage: null LogoutResponse");
            return SAML2Utils.createSOAPFault(SAML2Constants.SERVER_FAULT,
                "errorLogoutResponse", null);
        }
    
        SOAPMessage msg = null;
        try {
            LogoutUtil.signSLOResponse(loRes, realm, spEntityID, 
                SAML2Constants.SP_ROLE, logoutReq.getIssuer().getValue());
            msg = SAML2Utils.createSOAPMessage(loRes.toXMLString(true, true),
                false);
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("SPSingleLogoutServiceSOAP.onMessage: " +
                "Unable to create SOAP message:", se);
            return SAML2Utils.createSOAPFault(SAML2Constants.SERVER_FAULT,
                "errorLogoutResponseSOAP", se.getMessage());
        } catch (SOAPException ex) {
            SAML2Utils.debug.error("SPSingleLogoutServiceSOAP.onMessage: " +
                "Unable to create SOAP message:", ex);
            return SAML2Utils.createSOAPFault(SAML2Constants.SERVER_FAULT,
                "errorLogoutResponseSOAP", ex.getMessage());
        }

        return msg;
    }
}
