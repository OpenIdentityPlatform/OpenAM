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
 * $Id: SAMLSOAPReceiver.java,v 1.3 2009/06/12 22:21:39 mallas Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.saml.servlet;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import org.forgerock.openam.utils.ClientUtils;

import com.sun.identity.saml.AssertionManager;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.saml.assertion.Statement;

import com.sun.identity.saml.common.LogUtils;
import com.sun.identity.saml.common.SAMLCertUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequestVersionTooHighException;
import com.sun.identity.saml.common.SAMLRequestVersionTooLowException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLServiceManager;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.saml.protocol.Query;
import com.sun.identity.saml.protocol.Request;
import com.sun.identity.saml.protocol.Response;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class defines a SOAP Receiver which supports SOAP over HTTP binding.
 * It's the receiver of SAML requests from external parties for any 
 * authentication ,authorization or attribute information for a 
 * <code>Subject</code>. 
 * Its response which is an <code>Assertion</code> is used by the caller to 
 * enable an SSO solution or allow access to resource based on the 
 * <code>Statement</code> contained in the assertion sent by it. 
 * It  supports the following functions:
 * <pre>
 * 1- Accepts an artifact request and returns corresponding assertion.
 * 2- Accepts assertion id request and returns corresponding assertion.
 * 3- accept the authentication query and return authentication assertion.
 * 4- Accept authorization query and return authorization decision assertion.
 * 5- Accept attribute query and return attribute assertion.
 * </pre>
 **/ 
public class SAMLSOAPReceiver extends HttpServlet {

   /**
    * The <code>MessageFactory</code> object that will be used internally
    * to create the <code>SOAPMessage</code> object which will be passed to the
    * method <code>onMessage</code>. This new message will contain the data
    * from the message that was posted to the servlet.
    */
    private static MessageFactory msgFactory = null;

    private static SAMLConstants sc = null;

    /**
     * When initialized it represents ID of the local server. It has the format
     * of server_protocol://server_host:server_port.
     * It is used to identify whether the system is in server mode or client
     * mode.
     */
    public static String localSAMLServiceID = null;

    /**
     * Initializes the servlet.
     *
     * @param config <code>ServletConfig</code> object.
     * @throws ServletException if error occurred during initialization.
     */
    public void init(ServletConfig config) throws ServletException {
        String localServerProtocol =
            SystemConfigurationUtil.getProperty(
            SAMLConstants.SERVER_PROTOCOL);
        String localServer    = 
            SystemConfigurationUtil.getProperty(
            SAMLConstants.SERVER_HOST);
        String localServerPort= 
            SystemConfigurationUtil.getProperty(
            SAMLConstants.SERVER_PORT);

        localSAMLServiceID = localServerProtocol + "://" + localServer + 
                             ":" + localServerPort;
        super.init(config);                 
        // initializing the msgFactory field with a default
        // MessageFactory object.
        try {
            // Initialize it to the default.
            msgFactory = MessageFactory.newInstance();
        } catch (SOAPException ex) {
            String message = SAMLUtils.bundle.getString(
                    "missingSoapMessageFactory");
            SAMLUtils.debug.error(message, ex);
            String[] data = {SAMLUtils.bundle.getString(
                "missingSoapMessageFactory")};
            LogUtils.error(java.util.logging.Level.INFO, 
                LogUtils.SOAP_MESSAGE_FACTORY_ERROR, data);
            throw new ServletException(ex.getMessage());
        }
    }
    
    /**
     * Processes request coming from SOAP.
     *
     * @param req <code>HttpServletRequest</code> object.
     * @param resp <code>HttpServletResponse</code> object.
     * @throws ServletException if there is an error.
     * @throws IOException if there is an error.
     */
    public void doPost(HttpServletRequest req,
                       HttpServletResponse resp)
                   throws ServletException, java.io.IOException {
        // avoid the DOS attack for SOAP messaging 
        // In case SAML inter-op with other SAML vendor who may not provide 
        // contentlength in HttpServletRequest. We decide to support no length 
        // restriction for SOAP messaging. Here, we use a special value 
        // (e.g. 0) to indicate that no enforcement is required.
        
            if (SAMLUtils.getMaxContentLength() != 0) {
                int length =  req.getContentLength();
            if (length == -1) {
                throw new ServletException(
                    SAMLUtils.bundle.getString("unknownLength"));
            }
            if (length > SAMLUtils.getMaxContentLength()) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("content length too large" +length);
                }
                throw new ServletException(
                    SAMLUtils.bundle.getString("largeContentLength"));
             }
        }    

        String remoteAddr = ClientUtils.getClientIPAddress(req);
        Set partnerSourceID = null;
        if ((partnerSourceID = checkCaller(req, resp)) != null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(" got request from a trusted server, "
                    + "processing it now..");
            }
            try {
                MimeHeaders mimeHeaders = SAMLUtils.getMimeHeaders(req);
                ServletInputStream sInputStream = req.getInputStream();

                //Create the SOAPMessage from the reply
                SOAPMessage soapMessage = msgFactory.createMessage(
                                                    mimeHeaders,sInputStream);
                SOAPMessage soapMessageReply = null;
                soapMessageReply = this.onMessage(req, resp,soapMessage, 
                    partnerSourceID );
                if(soapMessageReply != null){
                    if(soapMessageReply.saveRequired())
                        soapMessageReply.saveChanges();
                        //Check to see if presence of SOAPFault
                        if(containsFault(soapMessageReply)){
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message(
                                    "Contains a SOAPFault!"); 
                            }
                            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
                        } else {
                            resp.setStatus(resp.SC_OK);
                        }
                        //Send the response back to the senderby placing
                        //the mime headers into the response, and
                        //externalizing the soapmessage onto the response object

                        SAMLUtils.setMimeHeaders(
                             soapMessageReply.getMimeHeaders(), resp);
                        ServletOutputStream sOutputStream = 
                                                    resp.getOutputStream();
                        soapMessageReply.writeTo(sOutputStream);
                        sOutputStream.flush();
                }    
            } catch(Exception e){
                 throw new ServletException(e); 
            }
        } else {
            // its not trusted site
            SAMLUtils.debug.error("Error message from SOAP Receiver:"
                                +remoteAddr+ " is untrusted site"); 
            String[] data = {SAMLUtils.bundle.getString("untrustedSite"), 
                remoteAddr};
            LogUtils.error(java.util.logging.Level.INFO, 
                LogUtils.UNTRUSTED_SITE, data);
            SOAPMessage faultReply = FormSOAPError(
                resp, "Server", "untrustedSite", null);
            SAMLUtils.setMimeHeaders(faultReply.getMimeHeaders(), resp);
            ServletOutputStream sOutputStream = resp.getOutputStream();
            try {
                faultReply.writeTo(sOutputStream);
            } catch (SOAPException se) {
                throw new ServletException(se);
            }
            sOutputStream.flush();
        }
    }

    /**
     * containsFault is a utiltiy method to see if msg contains a soapfault.
     */
    private boolean containsFault(SOAPMessage msg){
        try{
            SOAPPart sp = msg.getSOAPPart();
            SOAPEnvelope se = sp.getEnvelope();
            SOAPBody sb = se.getBody();
            return (sb.hasFault());
        }catch(Exception e){
            if (SAMLUtils.debug.messageEnabled()){
                SAMLUtils.debug.message("Error in containFault!"); 
            }
            return false;
        }
    }

    /**
     * Retrieves the SAML <code>Request</code> from the
     * <code>SOAPMessage</code>, calls internal methods to parse the 
     * <code>Request</code> generate SAML <code>Response</code> and send it 
     * back to the requestor again using SOAP binding over HTTP.
     */
    private SOAPMessage onMessage(HttpServletRequest req, HttpServletResponse
        servletResp, SOAPMessage message, Set partnerSourceID ) {

        // first check if there was any soap error during verifyHost()
        try {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("OnMessage called in receiving "
                    + "servlet");
            }

            // check the SOAP message for any SOAP
            // related errros before passing control to SAML processor
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            message.writeTo(bop);
            ByteArrayInputStream bin =
                    new ByteArrayInputStream(bop.toByteArray());
            Document doc = XMLUtils.toDOMDocument(bin, SAMLUtils.debug);
            Element root= doc.getDocumentElement();
            String rootName  = doc.getDocumentElement().getLocalName();
            if ((rootName == null) || (rootName.length() == 0)) {
                SAMLUtils.debug.error("Local name of the SOAPElement in  the"
                        + " SOAPMessage passed seems to be missing");
                return FormSOAPError(servletResp, "Client", "nullInput", 
                    "LocalNameMissing");
            }
            if (!(rootName.equals("Envelope")) ||
               (!(root.getNamespaceURI().equals(sc.SOAP_URI)))) {
                SAMLUtils.debug.error("SOAPReceiver: Could not parse "
                    + "SOAPMessage, either root element is not Envelope"
                    + " or invalid name space or prefix");
                return FormSOAPError(servletResp, "Client", "invalidElement", 
                        "envelopeInvalid");
            }
            NodeList nl = doc.getChildNodes();
            int length = nl.getLength();
            if (length <=0 ) {
                SAMLUtils.debug.error("SOAPReceiver: Message does not have "
                    + "body");
                return FormSOAPError(servletResp, "Client", "missingBody",null);
            }
            Node child = null;
            for (int i = 0; i < length; i++) {
                child = (Node)nl.item(i);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                String childName = child.getLocalName();
                if (childName.equals("Body")) {
                    break; // found the message body
                }
            }
            Element body = (Element)child;
            Response resp = extractProcessRequest(req, body, partnerSourceID);
            if (((Boolean) SAMLServiceManager.getAttribute(
                SAMLConstants.SIGN_RESPONSE)).booleanValue())
            {
                resp.signXML();
            }
            return FormMessageResponse(servletResp, resp);
        } catch(Exception e) {
            SAMLUtils.debug.error("Error in processing Request", e);
            return FormSOAPError(servletResp, "Server", 
                "cannotProcessRequest", null);
        }
    }

    /**
     * Extracts the Request object from the SOAPMessage return corresponding
     * response.
     */
    private Response extractProcessRequest( HttpServletRequest servletReq, 
        org.w3c.dom.Element  body, Set partnerSourceID) {
        
        Response retResponse = null;        
        String respID= SAMLUtils.generateID();
        String inResponseTo=null;
        List contents = new ArrayList();
        String message = null;
        Status status;
        String remoteAddr = ClientUtils.getClientIPAddress(servletReq);
        String recipient= remoteAddr;
        String invalidRespPrefix = SAMLUtils.bundle.getString(
            "invalidRequestLogMessage")+" "+remoteAddr+": ";
        String respPrefix = SAMLUtils.bundle.getString("responseLogMessage")+" "
            +remoteAddr+": ";
        NodeList nl  = body.getElementsByTagNameNS(sc.PROTOCOL_NAMESPACE_URI, 
                        "Request");
        int length = nl.getLength();
        if (length == 0 ) {
            SAMLUtils.debug.error("SOAPReceiver: Body does not have a Request");
            message = SAMLUtils.bundle.getString("missingRequest");
            try {
                status = new Status(new StatusCode("samlp:Requester"), 
                        message, null);
                retResponse = 
                    new Response(respID, inResponseTo, status, recipient, 
                    contents);
            } catch ( SAMLException se ) {
                SAMLUtils.debug.error("SOAPReceiver:Fatal error, cannot "
                    + "create status or response:"+ se.getMessage());
            }
            String[] data = {invalidRespPrefix, retResponse.toString()};
            LogUtils.error(java.util.logging.Level.INFO, 
                LogUtils.INVALID_REQUEST, data); 
            return retResponse;
        }
        boolean foundRequest = false;
        Request req = null;
        for (int i = 0; i < length; i++) {
            Node child = (Node)nl.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (child.getLocalName().equals("Request")) {
                try {
                    req = new Request((Element)child);
                    SAMLUtils.debug.message("found request ");
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message(" Received Request:"
                            + req.toString());
                    }
                    String[] data = {SAMLUtils.bundle.getString(
                      "requestLogMessage") + " " + remoteAddr, req.toString()};
                    LogUtils.access(java.util.logging.Level.FINE, 
                      LogUtils.SOAP_REQUEST_MESSAGE, data);
                    inResponseTo = req.getRequestID();
                    foundRequest = true;
                    break;
                } catch ( SAMLRequesterException ss) {
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("SOAPReceiver:setting "
                            + "status to samlp:Requester"+" "+ss.getMessage());
                    }
                    message = new String(ss.getMessage());
                    try {
                        status = new Status(new StatusCode("samlp:Requester"), 
                            message, null);
                            retResponse =  
                            new Response(respID, inResponseTo, status, 
                            recipient,contents);
                    } catch ( SAMLException se ) {
                        SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                            + "cannot create status or response:" 
                            + se.getMessage());
                        }
                    String[] data = {invalidRespPrefix, retResponse.toString()};
                        LogUtils.error(java.util.logging.Level.INFO, 
                        LogUtils.INVALID_REQUEST, data);
                    return retResponse;
                } catch ( SAMLRequestVersionTooHighException sv) {
                    String mesg = new String(sv.getMessage());
                    StringTokenizer tok1 = new StringTokenizer(mesg, "|");
                    inResponseTo = tok1.nextToken();
                    message = tok1.nextToken(); 
                    if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("SOAPReceiver:setting "
                            + "status to samlp:VersionMismatch"+" "+message);
                    }           
                    try {
                        status = new Status(new StatusCode(
                                "samlp:RequestVersionTooHigh"), message, null);
                            retResponse =  
                            new Response(respID,inResponseTo, status, recipient,
                                contents);
                    } catch ( SAMLException se ) {
                        SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                            + "cannot create status or response:" 
                            + se.getMessage());
                        }
                    String[] data = {invalidRespPrefix, retResponse.toString()};
                        LogUtils.error(java.util.logging.Level.INFO, 
                        LogUtils.INVALID_REQUEST, data);
                    return retResponse;
                } catch ( SAMLRequestVersionTooLowException sv) {
                        String mesg = new String(sv.getMessage());
                    StringTokenizer tok1 = new StringTokenizer(mesg, "|");
                    inResponseTo = tok1.nextToken();
                    message = tok1.nextToken(); 
                    if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("SOAPReceiver:setting "
                            + "status to samlp:VersionMismatch"+" "+message);
                    }
                    try {
                        status = new Status(new StatusCode(
                                "samlp:RequestVersionTooLow"), message, null);
                            retResponse =  
                            new Response(respID,inResponseTo, status, recipient,
                                contents);
                    } catch ( SAMLException se ) {
                        SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                            + "cannot create status or response:" 
                            + se.getMessage());
                        }
                    String[] data = {invalidRespPrefix, retResponse.toString()};
                        LogUtils.error(java.util.logging.Level.INFO, 
                        LogUtils.INVALID_REQUEST, data);
                    return retResponse;
                } catch (Exception e ) {
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("SOAPReceiver:setting "
                            + "status to samlp:Responder"+" "+ e.getMessage());
                    }
                    message = new String(e.getMessage());
                    try {
                        status = new Status(new StatusCode("samlp:Responder"), 
                                message, null);
                            retResponse =  new Response(respID,inResponseTo, 
                                    status, recipient, contents);
                    } catch ( SAMLException se ) {
                        SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                            + "cannot create status or response:" 
                            + se.getMessage());
                        }
                    String[] data = {invalidRespPrefix, retResponse.toString()};
                        LogUtils.error(java.util.logging.Level.INFO, 
                        LogUtils.INVALID_REQUEST, data);
                    return retResponse;
                }
            }
        }
        if (!(foundRequest)) {
            SAMLUtils.debug.error("SOAPReceiver: Body does not have a Request");
            message = SAMLUtils.bundle.getString("missingRequest");
            try {
                status = new Status(new StatusCode("samlp:Requester"),
                        message, null);
                retResponse =  new Response(respID, inResponseTo, status, 
                                    recipient, contents);
            } catch ( SAMLException se ) {
                SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                    + "cannot create status or response:" 
                    + se.getMessage());
            }
            String[] data = {invalidRespPrefix, retResponse.toString()};
            LogUtils.error(java.util.logging.Level.INFO, 
                LogUtils.INVALID_REQUEST, data);
            return retResponse;
        } else { // found request now process it
            if (!req.isSignatureValid()) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("SOAPReceiver: couldn't verify "
                        + "the signature on Request.");
                }
                message = SAMLUtils.bundle.getString("cannotVerifyRequest");
                try {
                    status = new Status(new StatusCode("samlp:Requester"),
                                message, null);
                    retResponse = new Response(respID, inResponseTo,
                                status, recipient, contents);
                    retResponse.setMajorVersion(req.getMajorVersion()); 
                    retResponse.setMinorVersion(req.getMinorVersion()); 
                } catch ( SAMLException se ) {
                    SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                        + "cannot create status or response", se);
                    String[] data = {SAMLUtils.bundle.getString(
                        "cannotBuildResponse")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.BUILD_RESPONSE_ERROR, data);
                    }
                String[] data = {respPrefix, retResponse.toString()};
                LogUtils.access(java.util.logging.Level.INFO, 
                    LogUtils.SENDING_RESPONSE, data);
                return retResponse;
            }
                                
            int reqType = req.getContentType();                
            if (reqType == Request.NOT_SUPPORTED) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("SOAPReceiver:Found "
                        + "element in the request which are not supported");
                }
                message = SAMLUtils.bundle.getString("unsupportedElement");
                try {
                    status = new Status(new StatusCode("samlp:Responder") , 
                                message, null);
                    retResponse =  new Response(respID, inResponseTo, 
                                status, recipient, contents);
                    retResponse.setMajorVersion(req.getMajorVersion()); 
                    retResponse.setMinorVersion(req.getMinorVersion()); 
                } catch ( SAMLException se ) {
                    SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                        + "cannot create status or response", se); 
                    String[] data = {SAMLUtils.bundle.getString(
                        "cannotBuildResponse")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.BUILD_RESPONSE_ERROR, data);
                    }
                String[] data = {respPrefix, retResponse.toString()};
                LogUtils.access(java.util.logging.Level.INFO, 
                    LogUtils.SENDING_RESPONSE, data);
                return retResponse;
            }
            List respondWith = req.getRespondWith();
            if (!parseRespondWith(respondWith)) {
                SAMLUtils.debug.error("SOAPReceiver:Supported statements "
                    + "are not present in the RespondWith element.");
                message = SAMLUtils.bundle.getString("unsupportedStatement");
                try {
                    status = new Status(new StatusCode("samlp:Responder"),
                        message, null);
                    retResponse =  new Response(respID, inResponseTo, 
                        status, recipient, contents);
                    retResponse.setMajorVersion(req.getMajorVersion()); 
                    retResponse.setMinorVersion(req.getMinorVersion());   
                    } catch ( SAMLException se ) {
                    SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                        + "cannot create status or response", se); 
                    String[] data = {SAMLUtils.bundle.getString(
                        "cannotBuildResponse")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.BUILD_RESPONSE_ERROR, data);
                    }
                String[] data = {respPrefix, retResponse.toString()};
                LogUtils.access(java.util.logging.Level.INFO, 
                    LogUtils.SENDING_RESPONSE, data);
                return retResponse;
            }
            AssertionManager am = null;
            try {
                am = AssertionManager.getInstance();
            } catch (SAMLException se ) {
                       if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("SOAPReceiver: Cannot"
                    + " instantiate AssertionManager");
                }
                message = se.getMessage();
                try {
                    status = new Status(new StatusCode("samlp:Responder"),
                             message, null);
                    retResponse =  new Response(respID,inResponseTo, 
                            status, recipient, contents);
                    retResponse.setMajorVersion(req.getMajorVersion()); 
                    retResponse.setMinorVersion(req.getMinorVersion()); 
                } catch ( SAMLException sse ) {
                    SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                        + "cannot create status or response", sse); 
                    String[] data = {SAMLUtils.bundle.getString(
                        "cannotBuildResponse")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.BUILD_RESPONSE_ERROR, data);
                }
                String[] data = {respPrefix, retResponse.toString()};
                LogUtils.access(java.util.logging.Level.INFO, 
                    LogUtils.SENDING_RESPONSE, data);
                return retResponse;
            } 
            List artifacts = null;
            List assertions = new ArrayList();
            if (reqType == Request.ASSERTION_ARTIFACT) {
                artifacts = req.getAssertionArtifact();
                length = artifacts.size();
                // ensure that all the artifacts have this site's sourceID
                for (int j = 0; j < length; j++) {
                    AssertionArtifact art = (AssertionArtifact)artifacts.get(j);
                    if (!isThisSiteID(art.getSourceID())) {
                               if (SAMLUtils.debug.messageEnabled()) {
                                 SAMLUtils.debug.message("SOAPReceiver:Artifact"
                                    + " has invalid SourceID");
                        }
                        message = SAMLUtils.bundle.getString(
                            "mismatchSourceID");
                        try {
                                status = new Status(new StatusCode(
                                "samlp:Requester"), message, null);
                                retResponse =  new Response(respID,inResponseTo,
                                status, recipient, contents);
                            retResponse.setMajorVersion(req.getMajorVersion()); 
                            retResponse.setMinorVersion(req.getMinorVersion()); 
                        } catch ( SAMLException ex ) {
                                SAMLUtils.debug.error("SOAPReceiver:"
                                + "Fatal error, "
                                + "cannot create status or response", ex); 
                            String[] data = {SAMLUtils.bundle.getString(
                                "cannotBuildResponse")};
                            LogUtils.error(java.util.logging.Level.INFO,
                                LogUtils.BUILD_RESPONSE_ERROR, data);
                        }
                        String[] data = {respPrefix, retResponse.toString()};
                        LogUtils.access(java.util.logging.Level.INFO, 
                            LogUtils.SENDING_RESPONSE, data);
                        return retResponse;
                    }
                } // for loop to go through artifacts to check for sourceID
                for (int i = 0; i < length; i++) {
                    AssertionArtifact artifact = (AssertionArtifact)
                                                    artifacts.get(i);
                    Assertion assertion = null;
                    try {
                        assertion = am.getAssertion(artifact,partnerSourceID );
                    } catch (SAMLException se ) {
                        if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("SOAPReceiver:"
                            + " could not find matching assertion");
                            }
                        message = se.getMessage();
                        try {
                            status = new Status(new StatusCode("samlp:Success"),
                                message, null);
                            retResponse =  new Response(respID,inResponseTo, 
                                status, recipient, contents);
                            retResponse.setMajorVersion(req.getMajorVersion()); 
                            retResponse.setMinorVersion(req.getMinorVersion()); 
                        } catch ( SAMLException sse ) {
                            SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                                + "cannot create status or response", sse); 
                            String[] data = {SAMLUtils.bundle.getString(
                                "cannotBuildResponse")};
                            LogUtils.error(java.util.logging.Level.INFO,
                                LogUtils.BUILD_RESPONSE_ERROR, data);
                            }
                        String[] data = {respPrefix, retResponse.toString()};
                        LogUtils.access(java.util.logging.Level.INFO, 
                            LogUtils.SENDING_RESPONSE, data);
                        return retResponse;
                    }
                    if (assertion != null) {
                        assertions.add(i,assertion);
                    }
                }
            } else if (reqType == Request.ASSERTION_ID_REFERENCE) {
                List assertionIdRefs = req.getAssertionIDReference();
                length = assertionIdRefs.size();
                for (int i = 0; i < length; i++) {
                    AssertionIDReference aidRef = 
                        (AssertionIDReference)assertionIdRefs.get(i);
                    Assertion assertion = null;
                    try {
                        assertion = am.getAssertion(aidRef, partnerSourceID);
                    } catch (SAMLException se ) {
                        if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("SOAPReceiver:"
                            + " could not find matching assertion");
                            }
                        message = se.getMessage();
                        try {
                            status = new Status(new StatusCode("samlp:Success"),
                                message, null);
                            retResponse =  new Response(respID,inResponseTo, 
                                status, recipient, contents);
                            retResponse.setMajorVersion(req.getMajorVersion()); 
                            retResponse.setMinorVersion(req.getMinorVersion()); 
                        } catch ( SAMLException sse ) {
                            SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                                + "cannot create status or response", sse); 
                            String[] data = {SAMLUtils.bundle.getString(
                                "cannotBuildResponse")};
                            LogUtils.error(java.util.logging.Level.INFO,
                                LogUtils.BUILD_RESPONSE_ERROR, data);
                            }
                        String[] data = {respPrefix, retResponse.toString()};
                        LogUtils.access(java.util.logging.Level.INFO, 
                            LogUtils.SENDING_RESPONSE, data);
                        return retResponse;
                    }
                    if (assertion != null) {
                        assertions.add(i,assertion);
                    }
                }
            } else if ((reqType == Request.AUTHENTICATION_QUERY) ||
                       (reqType == Request.AUTHORIZATION_DECISION_QUERY) ||
                        (reqType == Request.ATTRIBUTE_QUERY))
            {
                Query query = req.getQuery();
                if (query != null) {
                    Assertion assertion = null;
                    try { 
                        // if we come here, partnerSourceID is not empty
                        // always pass the first matching sourceID in
                        // need to find solution to handle multiple matches:TBD
                        assertion = am.getAssertion(query, (String)
                            ((Iterator) partnerSourceID.iterator()).next());
                    } catch (SAMLException se ) {
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("SOAPReceiver:"
                            + " could not find matching assertion");
                        }
                        message = se.getMessage();
                        try {
                            status = new Status(new StatusCode("samlp:Success"),
                                message, null);
                            retResponse =  new Response(respID,inResponseTo,
                                status, recipient, contents);
                            retResponse.setMajorVersion(req.getMajorVersion()); 
                            retResponse.setMinorVersion(req.getMinorVersion()); 
                        } catch ( SAMLException sse ) {
                            SAMLUtils.debug.error("SOAPReceiver:Fatal "
                                  + " error, cannot create status or "
                                + " response", sse);
                            String[] data = {SAMLUtils.bundle.getString(
                                "cannotBuildResponse")};
                            LogUtils.error(java.util.logging.Level.INFO,
                                LogUtils.BUILD_RESPONSE_ERROR, data);
                        }
                        String[] data = {respPrefix, retResponse.toString()};
                        LogUtils.access(java.util.logging.Level.INFO, 
                            LogUtils.SENDING_RESPONSE, data);
                        return retResponse;
                    }
                    if (assertion != null) {
                        assertions.add(assertion);
                    }
                }
            } else { //
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("SOAPReceiver:Request "
                        + "contents has element which is not supported at this"
                        + " time");
                }
                message = SAMLUtils.bundle.getString(
                        "unsupportedElement");
                try {
                    status = new Status(new StatusCode("samlp:Responder"),
                                message, null);
                    retResponse = new Response(respID, inResponseTo, 
                                     status, recipient, contents);
                    retResponse.setMajorVersion(req.getMajorVersion()); 
                    retResponse.setMinorVersion(req.getMinorVersion()); 
                } catch ( SAMLException se ) {
                    SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                        + "cannot create status or response", se); 
                    String[] data = {SAMLUtils.bundle.getString(
                        "cannotBuildResponse")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.BUILD_RESPONSE_ERROR, data);
                    }
                String[] data = {respPrefix, retResponse.toString()};
                LogUtils.access(java.util.logging.Level.INFO, 
                    LogUtils.SENDING_RESPONSE, data);
                return retResponse;
            }
            int assertionSize = assertions.size();
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("found "+assertionSize+ " assertions.");
            }
            // check to see of the statements inside the assertion are not 
            // different from what exists in the RespondWith element of the 
            // Request received. 
            for (int i = 0; i < assertionSize; i++) {
                Response resp = validateStatements((Assertion)assertions.get(i),
                        respondWith, contents, i, respID, inResponseTo, 
                          recipient);
                if (resp != null) {
                    String[] data = {respPrefix, retResponse.toString()};
                    LogUtils.access(java.util.logging.Level.INFO, 
                        LogUtils.SENDING_RESPONSE, data);
                    retResponse.setMajorVersion(req.getMajorVersion()); 
                    retResponse.setMinorVersion(req.getMinorVersion()); 
                    return resp;
                } // else there was no mismatch with respondWith element
            }
            if (reqType == Request.ASSERTION_ARTIFACT) {
                if (contents.size() == artifacts.size()) {
                    message = null;
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("SOAPReceiver: Matching "
                            + "Assertion found");
                    }
                    try {
                        status = new Status(new StatusCode("samlp:Success"),
                                message, null);
                        retResponse = new Response(respID, inResponseTo, 
                            status, recipient, contents);
                        retResponse.setMajorVersion(req.getMajorVersion()); 
                        retResponse.setMinorVersion(req.getMinorVersion()); 
                    } catch ( SAMLException se ) {
                        SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                            + "cannot create status or response", se); 
                        String[] data = {SAMLUtils.bundle.getString(
                            "cannotBuildResponse")};
                        LogUtils.error(java.util.logging.Level.INFO,
                            LogUtils.BUILD_RESPONSE_ERROR, data);
                        }
                    String[] data = {respPrefix, retResponse.toString()};
                    LogUtils.access(java.util.logging.Level.FINE, 
                        LogUtils.SENDING_RESPONSE, data);
                    return retResponse;
                } else {
                    message = SAMLUtils.bundle.getString("unequalMatch");
                    try {
                        status = new Status(new StatusCode("samlp:Success"),
                                     message, null);
                        //contents = null;
                        retResponse  = new Response(respID, inResponseTo, 
                                status, recipient, contents);
                        retResponse.setMajorVersion(req.getMajorVersion()); 
                        retResponse.setMinorVersion(req.getMinorVersion()); 
                    } catch ( SAMLException se ) {
                        SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                            + "cannot create status or response", se); 
                        String[] data = {SAMLUtils.bundle.getString(
                            "cannotBuildResponse")};
                        LogUtils.error(java.util.logging.Level.INFO,
                            LogUtils.BUILD_RESPONSE_ERROR, data);
                        }
                    String[] data = {respPrefix, retResponse.toString()};
                    LogUtils.access(java.util.logging.Level.INFO, 
                        LogUtils.SENDING_RESPONSE, data);
                    return retResponse;
                }
            } else { // build response for all the other type of request
                 try {
                    status = new Status(new StatusCode("samlp:Success"),
                               message, null);
                    retResponse = new Response(respID, inResponseTo, 
                        status, recipient, contents);
                    retResponse.setMajorVersion(req.getMajorVersion()); 
                    retResponse.setMinorVersion(req.getMinorVersion()); 
                } catch ( SAMLException se ) {
                    SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                        + "cannot create status or response", se); 
                    String[] data = {SAMLUtils.bundle.getString(
                        "cannotBuildResponse")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.BUILD_RESPONSE_ERROR, data);
                    }
            }
        } // end of else found request
        if (LogUtils.isAccessLoggable(
            java.util.logging.Level.FINER)) {
            String[] data = {respPrefix, retResponse.toString()};
            LogUtils.access(java.util.logging.Level.FINER, 
                LogUtils.SENDING_RESPONSE, data);
        } else {
            String[] data = {respPrefix, retResponse.getResponseID()};
            LogUtils.access(java.util.logging.Level.INFO, 
                LogUtils.SENDING_RESPONSE, data);
        }
        return retResponse;
    }
   
    /**
     * This method forms  a SOAP Fault and puts it in the SOAP Message's
     * Body.
     */        
    private SOAPMessage FormSOAPError( HttpServletResponse resp, 
        String faultCode, String faultString, String detail) {
        SOAPMessage msg = null ;
        SOAPEnvelope envelope = null;
        SOAPFault sf = null;
        SOAPBody body = null;
        SOAPElement se = null;
        try {
            msg = msgFactory.createMessage();
            envelope = msg.getSOAPPart().getEnvelope();
            body = envelope.getBody();
            sf = body.addFault();
            Name qName = envelope.createName(faultCode,null,
                            SOAPConstants.URI_NS_SOAP_ENVELOPE);
            sf.setFaultCode(qName);
            sf.setFaultString(SAMLUtils.bundle.getString(faultString));
            if ((detail != null) && !(detail.length() == 0)) {
                Detail det = sf.addDetail();
                se = (SOAPElement)det.addDetailEntry(envelope.createName(
                                "Problem"));
                se.addAttribute(envelope.createName("details"), SAMLUtils.
                                bundle.getString(detail));
            }
        } catch ( SOAPException e ) {
               SAMLUtils.debug.error("FormSOAPError:", e); 
            String[] data = {SAMLUtils.bundle.getString("soapFaultError")};
            LogUtils.error(java.util.logging.Level.INFO, 
                LogUtils.SOAP_FAULT_ERROR, data);
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        }
        return msg;
    }

    /**
     * This message forms the SAML Response and puts it in the 
     * SOAPMessage's Body.
     */
    private SOAPMessage FormMessageResponse(HttpServletResponse servletResp, 
        Response resp) {
        SOAPMessage msg = null;
        MimeHeaders mimeHeaders = new MimeHeaders();
        mimeHeaders.addHeader("Content-Type","text/xml");
        StringBuffer envBegin = new StringBuffer(100);
        envBegin.append("<").append(sc.SOAP_ENV_PREFIX).append(":Envelope").
                append(sc.SPACE).append("xmlns:").append(sc.SOAP_ENV_PREFIX).
                append("=\"").append(sc.SOAP_URI).append("\">").append(sc.NL);
        envBegin.append("<").append(sc.SOAP_ENV_PREFIX).append(":Body>").
                     append(sc.NL);

        StringBuffer envEnd = new StringBuffer(100);
        envEnd.append(sc.START_END_ELEMENT).append(sc.SOAP_ENV_PREFIX).
               append(":Body>").append(sc.NL);
        envEnd.append(sc.START_END_ELEMENT).append(sc.SOAP_ENV_PREFIX).
               append(":Envelope>").append(sc.NL);
        try {
            StringBuffer sb = new StringBuffer(300);
            sb.append(envBegin).append(resp.toString()).append(envEnd);
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("response created is: "+sb.toString());
            }
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            bop.write(sb.toString().getBytes(sc.DEFAULT_ENCODING));
            msg = msgFactory.createMessage(
                mimeHeaders, new ByteArrayInputStream(bop.toByteArray()));
        } catch (Exception e ) {
            SAMLUtils.debug.error("could not build response:"+e.getMessage());
            servletResp.setStatus(servletResp.SC_INTERNAL_SERVER_ERROR);
            return FormSOAPError(servletResp, "Server", "cannotBuildResponse", 
                "cannotVerifyIdentity");
        }
        return msg; 
    }

    /**
     * This method validates the assertion to see that the statements it 
     * contains are what is present in the RespondWith element of the
     * corresponsing Request.  If valid adds the passed assertion in the
     * passed contents, which is a List, at the specified index.
     */
    private Response validateStatements(Assertion assertion, List respondWith, 
        List contents,int index, String respID, String inResponseTo, 
        String recipient) {  
        String message = null;
        Set statements = assertion.getStatement();
        int length = statements.size();
        Response retResponse = null;
        Status status = null;
        if ((statements.isEmpty()) || (length == 0)) {
            SAMLUtils.debug.error("SOAPReceiver: Assertion found does not have"
                + " any statements in it");
            message = SAMLUtils.bundle.getString("missingStatement");
            try {
                status = new Status(new StatusCode("samlp:Responder"),
                        message, null);
                retResponse = 
                    new Response(respID, inResponseTo, status, recipient, 
                        contents);
            } catch ( SAMLException se ) {
                SAMLUtils.debug.error("SOAPReceiver:Fatal error, cannot "
                    + "create status or response", se);
                String[] data = {SAMLUtils.bundle.getString(
                    "cannotBuildResponse")};
                LogUtils.error(java.util.logging.Level.INFO,
                    LogUtils.BUILD_RESPONSE_ERROR, data);
            }
            return retResponse;
        } else { // statements not empty
            boolean mismatchError = false; // would be true if there is any
                                   // mismatch with RespondWith contents.
            if (respondWith.size() == 0 ) {
                contents.add(index,assertion);
            } else {
                mismatchError = !checkAgainstRespondWith(respondWith,
                    statements);
                if (!mismatchError) {
                    contents.add(index, assertion);
                }
            } // end of else respondWith size > 0
            if (mismatchError) {
                SAMLUtils.debug.error("SOAPReceiver: Assertion does not "
                       + " meet respondWith criteria in the received Request");
                message = SAMLUtils.bundle.getString(
                   "mismatchRespondWith");
                try {
                    //contents = null;
                    status = new Status(new StatusCode("samlp:Success"),
                        message, null);
                    return new Response(respID, inResponseTo, status, 
                        recipient, contents);
                } catch ( SAMLException se ) {
                    SAMLUtils.debug.error("SOAPReceiver:Fatal error, "
                        + " cannot create status or response", se);
                    String[] data = {SAMLUtils.bundle.getString(
                        "cannotBuildResponse")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.BUILD_RESPONSE_ERROR, data);
                }
            }
        }  // end of else statements not empty
        return null; // reached here, so there was no error in validation
    }

    /**
     * This method takes in the List of respondWith and returns true or
     * false based on if the contents of the respondWith element 
     * contains Statement types supported by the Receiver
     */
    private boolean parseRespondWith(List respondWith) {
        Iterator it = respondWith.iterator();
        while (it.hasNext()) {
            String respWith = (String)it.next();
            int index = respWith.indexOf(":");
            if ((index == -1) || (index == 0)) {
                return false;
            } else {
                if (!(respWith.endsWith(":AuthenticationStatement")) &&
                   (!(respWith.endsWith(":AuthorizationDecisionStatement"))) &&
                   (!(respWith.endsWith(":AttributeStatement")))) 
                {
                    return false;
                }
            }
        }
        return true;
    }
   
    /**
     * This method takes in a RespondWith element ( List) and a statement List
     * and returns true or false depending on whether the all statement types 
     * exist in the RespondWith List
     */
    private boolean checkAgainstRespondWith(List respondWith, Set statements) {
        boolean stFound= false;
        Iterator itSt = statements.iterator(); 
        while (itSt.hasNext()) { // it iterates over statements
            stFound = false;
            Statement st = (Statement)itSt.next();
            Iterator it = respondWith.iterator();
            while (it.hasNext()) {
                String respWith = (String)it.next();
                SAMLUtils.debug.message("matching respondWith element:"
                    +respWith);
                switch (st.getStatementType()) {
                    case Statement.AUTHENTICATION_STATEMENT:
                        if (respWith.endsWith(":AuthenticationStatement")) {
                            SAMLUtils.debug.message("matching auth st");
                            stFound = true; 
                        }
                        break;
                    case Statement.AUTHORIZATION_DECISION_STATEMENT:
                        if (respWith.endsWith(
                            ":AuthorizationDecisionStatement")) {
                            SAMLUtils.debug.message("matching authz st");
                            stFound = true; 
                        }
                        break;
                    case Statement.ATTRIBUTE_STATEMENT:
                        if (respWith.endsWith(":AttributeStatement")) {
                            SAMLUtils.debug.message("matching attrib st");
                            stFound = true; 
                        }
                        break;
                }
                   if (stFound) {
                    SAMLUtils.debug.message("match found");
                    break;
                }
            }
            if (!stFound) {
                SAMLUtils.debug.message("mismatch found");
                return false;
            }
        }
        return true;
    }

    /**
     * Protected method to check the caller to the servlet.
     * Returns a set of sourceid whose hostlist contains the passed in
     * certificate's nick name or HttpServletRequest's remote IP address if
     * valid. Else null is returned.
     */
    protected static Set checkCaller(HttpServletRequest req,
        HttpServletResponse resp) throws ServletException {
        String certOrIP = null;
        Set partnerSourceID = null;
        String remoteAddr = ClientUtils.getClientIPAddress(req);
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("caller's IP:"+remoteAddr);
        }
        // first see if there is a cert being presented
        // if so establish trust with cert, then dont bother about IP addresses
        java.security.cert.X509Certificate[] allCerts = null;
        // check if container is secure or not before calling get certificate
        // to avoid error in web server log for WS 6.1sp2
        if (req.isSecure()) {
            try {
                allCerts = (java.security.cert.X509Certificate[])
                    req.getAttribute("javax.servlet.request.X509Certificate");
            } catch (Exception e ) {
                    SAMLUtils.debug.error("SAMLSOAPReceiver: Exception", e);
            }
        }
        if (allCerts == null || allCerts.length == 0) {
            // no cert so use IP address
            certOrIP = remoteAddr;
        } else { // cert is present, so verify cert
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLSOAPReceiver: got all certs from "
                     +"HttpServletRequest =" + allCerts.length);
            }
            java.security.cert.X509Certificate thecert = allCerts[0];
            SAMLCertUtils.CertEntry foundCertEntry = null;
            if ((foundCertEntry = SAMLCertUtils.getMatchingCertEntry(
                thecert)) != null) {
                certOrIP = foundCertEntry.getNickName();
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("Found matching nickname:"
                        +certOrIP);
                }
            }
        }
        partnerSourceID = getPartnerSourceID(certOrIP);
        if (partnerSourceID == null || partnerSourceID.isEmpty()) {
            return (null);
        }
        return (partnerSourceID);
    }

    /**
     * This is a private function which goes through the partner URLS and
     * locates a set of source ID based on the passed string which is either the
     * remote IP address ( if no cert is presented) or is a cert alias if
     * SSL with Client auth is on. If there is no match, return null.
     */
    private static Set getPartnerSourceID(String certOrIP) {
        Map partnerMap = (Map) SAMLServiceManager.getAttribute(
                                        SAMLConstants.PARTNER_URLS);
        if (partnerMap != null) {
            Set sidSet = new HashSet();
            Set partnerSet = partnerMap.entrySet();
            Iterator it = partnerSet.iterator();
            Set hostSet = null;
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                SAMLServiceManager.SOAPEntry partnerEntry = 
                    (SAMLServiceManager.SOAPEntry)entry.getValue();
                hostSet = partnerEntry.getHostSet();
                if ((hostSet != null) && (hostSet.contains(certOrIP))) {
                    sidSet.add((String) entry.getKey());
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("getPartnerSourceID: found " 
                            + "a matching sid=" + Base64.encode(
                SAMLUtils.stringToByteArray((String)entry.getKey())));
                    }
                }
            }
            return sidSet;
        }
        return (null);
    } 

    /**
     * Verifies if the sourceID passed in the parameter is same
     * as this site's site ID.
     */
    private boolean isThisSiteID(String sourceID) {
        return ((String) SAMLServiceManager.getAttribute(
            SAMLConstants.SITE_ID)).equals(sourceID) ? true:false;
    }
}
