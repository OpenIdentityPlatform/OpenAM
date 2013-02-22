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
 * $Id: SOAPReceiver.java,v 1.3 2008/06/25 05:47:23 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.liberty.ws.soapbinding; 

import java.io.IOException;

import java.util.List;
import java.util.logging.Level;

import java.security.cert.X509Certificate;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.liberty.ws.security.SecurityUtils;
import com.sun.identity.liberty.ws.common.LogUtil;
import com.sun.identity.saml.common.SAMLUtils;
import org.forgerock.openam.utils.ClientUtils;

/**
 * The <code>SOAPReceiver</code> class defines a SOAP Receiver which supports
 * SOAP over HTTP binding. It receives requests sent from <code>Client</code>.
 * During the startup, it will read <code>RequestHandler</code> from SOAP
 * binding SM schema and store in a static HashMap. Each 
 * <code>RequestHandler</code> is associated with a unique key. When a web
 * service client uses <code>Client</code> to send request, the SOAP URL must
 * be in the format of 'this_servlet_URL>/key'. The <code>SOAPReceiver</code>
 * will parse the SOAP URL to get the key and use it to find corresponding
 * <code>RequestHandler</code>. After it is done processing, it will invoke
 * <code>RequestHandler.processRequest</code> to let web service to do further
 * processing.
 */

public class SOAPReceiver extends HttpServlet {

     private static MessageFactory msgFactory = null;

     /**
      * Initializes the Servlet.
      *
      * @param config the <code>ServletConfig</code>.
      * @throws ServletException if there is any error.
      */ 
     @Override
     public void init(ServletConfig config) throws ServletException {
         super.init(config);
         try {
             msgFactory = MessageFactory.newInstance();
         } catch (SOAPException ex) {
             Utils.debug.error("SOAPReceiver.init: create message " +
                     "factory failed.");
             throw new ServletException(ex.getMessage());
         }
     }

    /**
     * Processes HTTP request and sends back HTTP response. It gets called
     * internally by the servlet engine.
     *
     * @param request the HTTP request.
     * @param response the HTTP response.
     * @throws IOException if an IO error occurs while processing
                           the request
     * @throws ServletException if an servlet error occurs while processing
     *                          the request
     */
     @Override
     public void doPost(HttpServletRequest request,HttpServletResponse response)
                        throws IOException, ServletException {
         try {
             MimeHeaders mimeHeaders = SAMLUtils.getMimeHeaders(request);
             ServletInputStream sInputStream = request.getInputStream();
             SOAPMessage soapMessage = msgFactory.createMessage(
                     mimeHeaders, sInputStream);
             
             SOAPMessage soapMessageReply = this.onMessage(soapMessage,request);
             
             if (soapMessageReply != null) {
                 SAMLUtils.setMimeHeaders(
                         soapMessageReply.getMimeHeaders(), response);
                 ServletOutputStream sOutputStream = response.getOutputStream();
                 soapMessageReply.writeTo(sOutputStream);
                 sOutputStream.flush();
             } else {
                 response.setStatus(
                         HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
             }
         } catch (SOAPException se) {
             throw new ServletException(se);
         }
     }

    /**
     * Processes the request message and invokes
     * <code>RequestHandler.processRequest</code>.
     *
     * @param message request received from the requestor
     * @param request the HTTP request
     * @return response being sent to the requestor
     */
    public SOAPMessage onMessage(SOAPMessage message,
                                 HttpServletRequest request) {
        if (Utils.debug.messageEnabled()) {
            Utils.debug.message("SOAPReceiver.onMessage:");
        }

        String soapAction =
                    request.getHeader(SOAPBindingConstants.SOAP_ACTION_HEADER);
        if (soapAction != null && soapAction.length() == 0) {
            soapAction = request.getRequestURI();
        }

	String remoteAddr = ClientUtils.getClientIPAddress(request);

        X509Certificate[] allCerts = (X509Certificate[]) 
            request.getAttribute("javax.servlet.request.X509Certificate");
        X509Certificate certificate = null;
        if (allCerts != null && allCerts.length > 0) {
            certificate = allCerts[0];
        }

        String key = request.getPathInfo();
        if (key != null) {
            // remove leading '/'
            key = key.substring(1);

            if (Utils.debug.messageEnabled()) {
                Utils.debug.message("SOAPReceiver.doPost: key = " + key + 
                                    "\nSOAPAction = " + soapAction +
                                    "\nremoteAttr = " + remoteAddr);
            }

            RequestHandler handler =
                    (RequestHandler)SOAPBindingService.handlers.get(key);
            if (handler != null) {
                if (soapAction != null) {
                    List supportedSOAPActions =
                        (List)SOAPBindingService.supportedSOAPActionsMap
                                                .get(key);
                    if (supportedSOAPActions != null &&
                        !supportedSOAPActions.isEmpty() &&
                        !supportedSOAPActions.contains(soapAction)) {

                        return FormSOAPError("Client","unsupportedSOAPAction",
                                             null);
                    }
                }

                Message req = null;
                try {
                    req = new Message(message);
                    Utils.enforceProcessingRules(req, null, true);
                    req.setIPAddress(remoteAddr);
                    req.setPeerCertificate(certificate);
                    req.setProtocol(request.getScheme());
                    if (req.getSecurityProfileType() != Message.ANONYMOUS &&
                        !SecurityUtils.verifyMessage(req)) {

                        return FormSOAPError("Client", "cannotVerifySignature",
                                             req);
                    }
                    String authMech = req.getAuthenticationMechanism();
                    if (Utils.debug.messageEnabled()) {
                        Utils.debug.message("SOAPReceiver.onMessage: " +
                            "authMech = " + authMech +
                            ", isClientAuthentication = " +
                            req.isClientAuthentication());
                    }
                    if (authMech == null ||
                        !SOAPBindingService
                              .getSupportedAuthenticationMechanisms()
                              .contains(authMech)) {
                        return FormSOAPError("Server", "unsupportedAuthMech",
                                             req);
                    }

                    WebServiceAuthenticator wsAuthenticator =
                        SOAPBindingService.getWebServiceAuthenticator();
                    if (wsAuthenticator == null) {
                        return FormSOAPError("Server", "noWSAuthentication",
                            req);
                    }
                    Object token = wsAuthenticator.authenticate(req, null,
                        null,request);
                    req.setToken(token);

		    String[] data = { req.getCorrelationHeader().getMessageID(),
				      key };
                    LogUtil.access(Level.INFO,LogUtil.WS_SUCCESS,data);

                    Message resp = handler.processRequest(req);
                    resp.getCorrelationHeader().setRefToMessageID(
                                   req.getCorrelationHeader().getMessageID());
                    int securityProfileType = resp.getSecurityProfileType();
                    if (securityProfileType == Message.ANONYMOUS ||
                        securityProfileType == Message.BEARER_TOKEN) {
                        return resp.toSOAPMessage();
                    } else {
                        Element sigElem = SecurityUtils.signMessage(resp);
                        if (sigElem == null) {
                            Utils.debug.error("SOAPReceiver.onMessage: " +
                                              "Unable to sign response");
                            return FormSOAPError("Server",
                                                 "cannotSignResponse", req);
                        }
                        Document doc = sigElem.getOwnerDocument();
                        return Utils.DocumentToSOAPMessage(doc);
                    }
                } catch (SOAPFaultException sfe) {
                    Message sfmsg = sfe.getSOAPFaultMessage();
                    if (sfmsg == null) {
                        return FormSOAPError("Server", "unknownError", req);
                    }

                    if (Utils.debug.messageEnabled()) {
                        Utils.debug.message("SOAPReceiver.onMessage: " +
                            "got SOAPFaultException", sfe);
                    }

                    try {
                        if (req != null) {
                            CorrelationHeader corrH =
                                                sfmsg.getCorrelationHeader();
                            if (corrH != null) {
                                corrH.setRefToMessageID(
                                    req.getCorrelationHeader().getMessageID());
                            }
                        }
                        return sfmsg.toSOAPMessage();
                    } catch (Exception ex) {
                        Utils.debug.message("SOAPReceiver.onMessage: ", ex);
                        return FormSOAPError("Server", ex, req);
                    }
                } catch (Throwable t) {
                    Utils.debug.message("SOAPReceiver.onMessage: ", t);
                    return FormSOAPError("Server", t, req);
                }
            } else {
                return FormSOAPError("Server", "missingRequestHandler", null);
            }
        }
        return FormSOAPError("Server", "missingKey", null);
    }

    /**
     * Constructs a SOAPMessage with specified fault code and Throwable.
     * The fault string will be Throwable.getMessage();
     * The fault code will have same namespace of soap envelope.
     *
     * @param faultCode the fault code
     * @param throwable the Throwable
     * @param req the request Message
     * @return the SOAPMessage object
     */
    private SOAPMessage FormSOAPError(String faultCode,Throwable throwable,
                                      Message req)  {
        String faultString = throwable.getMessage();
        if (faultString == null || faultString.length() == 0) {
            faultString = Utils.bundle.getString("unknownError");
        }
        return FormSOAPError(req, faultCode, faultString);
    }

    /**
     * Constructs a SOAPMessage with specified fault code and fault string.
     * The fault code will have same namespace of soap envelope.
     *
     * @param faultCode the fault code
     * @param faultStringKey the fault string key to resource bundle
     * @param req the request Message
     * @return the SOAPMessage object
     */
    private SOAPMessage FormSOAPError(String faultCode,String faultStringKey,
                                      Message req)  {
        String faultString = Utils.bundle.getString(faultStringKey);
        return FormSOAPError(req, faultCode, faultString);
    }

    /**
     * Constructs a SOAPMessage with specified fault code and fault string.
     * The fault code will have same namespace of soap envelope.
     *
     * @param req the request Message
     * @param faultCode the fault code
     * @param faultString the fault string
     * @return the SOAPMessage object
     */
    private SOAPMessage FormSOAPError(Message req, String faultCode,
                                      String faultString ) {
        String logMsg;
        if (req == null) {
            logMsg = faultString;
        } else {
            logMsg = Utils.bundle.getString("messageID") + "=" +
                    req.getCorrelationHeader().getMessageID() + ". " +
                    faultString;
        }
        String[] data = { logMsg };
        LogUtil.error(Level.INFO, LogUtil.WS_FAILURE,data);
        try {
            SOAPFault sf = new SOAPFault(
                    new QName(SOAPBindingConstants.NS_SOAP, faultCode),
                    faultString);
            Message resp = new Message(sf);
            return resp.toSOAPMessage();
        } catch (Exception e ) {
            Utils.debug.error("SOAPReceiver.FormSOAPError: ", e);
        }
        return null;
    }
}
