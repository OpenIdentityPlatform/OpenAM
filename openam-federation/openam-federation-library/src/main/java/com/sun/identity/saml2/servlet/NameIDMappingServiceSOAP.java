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
 * $Id: NameIDMappingServiceSOAP.java,v 1.6 2009/10/14 23:59:44 exu Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.saml2.servlet;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.NameIDMapping;
import com.sun.identity.saml2.protocol.NameIDMappingRequest;
import com.sun.identity.saml2.protocol.NameIDMappingResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml.common.SAMLUtils;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.ServletException;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.w3c.dom.Element;


/**
 * This class <code>NameIDMappingServiceSOAP</code> receives and processes 
 * Name ID mapping request using SOAP binding on IDP side.
 */
public class NameIDMappingServiceSOAP extends HttpServlet {

    public void init() throws ServletException {
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
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
                realm, idpEntityID, SAML2Constants.NAMEID_MAPPING_SERVICE,
                SAML2Constants.SOAP))
            {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "unsuppoprtedBinding"));
            }

            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("NameIDMappingServiceSOAP.doPost : " +
                    "uri = " + req.getRequestURI() + ", idpMetaAlias = " +
                    idpMetaAlias + ", idpEntityID = " + idpEntityID);
            }

            SOAPMessage msg = SOAPCommunicator.getInstance().getSOAPMessage(req);
            Element reqElem = SOAPCommunicator.getInstance().getSamlpElement(msg,
                    SAML2Constants.NAME_ID_MAPPING_REQUEST);

            NameIDMappingRequest nimRequest = ProtocolFactory.getInstance()
                .createNameIDMappingRequest(reqElem);

            NameIDMappingResponse nimResponse =
                NameIDMapping.processNameIDMappingRequest(nimRequest, realm,
               idpEntityID);

            SOAPMessage reply = SOAPCommunicator.getInstance().createSOAPMessage(
                    nimResponse.toXMLString(true, true), false);

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
                OutputStream os = resp.getOutputStream();
                reply.writeTo(os);
                os.flush();
            }
        } catch (SAML2Exception ex) {
            SAML2Utils.debug.error("NameIDMappingServiceSOAP", ex);
            SAMLUtils.sendError(req, resp, 
                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                 "nameIDMappingFailed", ex.getMessage());
            return;
        } catch (SOAPException soap) {
            SAML2Utils.debug.error("NameIDMappingServiceSOAP", soap);
            SAMLUtils.sendError(req, resp, 
                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                 "nameIDMappingFailed", soap.getMessage());
            return;
        }
    }
}
