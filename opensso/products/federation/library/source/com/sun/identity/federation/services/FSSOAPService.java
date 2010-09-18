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
 * $Id: FSSOAPService.java,v 1.4 2008/11/10 22:56:58 veiming Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
    
/**
 * Utils to handle SOAP profiles.
 */
public class FSSOAPService {
    private static FSSOAPService instance = null;
    private static MessageFactory fac = null;
    private static SOAPConnectionFactory scf = null;

    static {
        try {
            fac = MessageFactory.newInstance();
        } catch(Exception ex) {
            FSUtils.debug.error(
                FSUtils.bundle.getString( "missingSOAPMessageFactory"), ex);
        }

        try {
            scf = SOAPConnectionFactory.newInstance();
        } catch(SOAPException e) {
            FSUtils.debug.error("FSSOAPService", e);
        }
    };
    
    /* do nothing constructor */
    private FSSOAPService() {
    }
    
    /*
     * Binds the passed object xml string to SOAP message.
     * The SOAP Message will then be sent across to the remote
     * provider for processing.
     * @param xmlString the request/response xml string to be bound
     * @return SOAPMessage constructed from the xml string
     */
    public SOAPMessage bind(String xmlString) {
        SOAPMessage msg = null;
        MimeHeaders mimeHeaders = new MimeHeaders();
        mimeHeaders.addHeader("Content-Type","text/xml");
        StringBuffer envBegin = new StringBuffer(100);
        envBegin.append("<").append(IFSConstants.SOAP_ENV_PREFIX).
            append(":Envelope").append(IFSConstants.SPACE).append("xmlns:").
            append(IFSConstants.SOAP_ENV_PREFIX).append("=\"").
            append(IFSConstants.SOAP_URI).append("\">").append(IFSConstants.NL).
            append("<").append(IFSConstants.SOAP_ENV_PREFIX).
            append(":Body>").append(IFSConstants.NL);
        
        StringBuffer envEnd = new StringBuffer(100);
        envEnd.append(IFSConstants.START_END_ELEMENT).
            append(IFSConstants.SOAP_ENV_PREFIX).append(":Body>").
            append(IFSConstants.NL).
            append(IFSConstants.START_END_ELEMENT).
            append(IFSConstants.SOAP_ENV_PREFIX).
            append(":Envelope>").append(IFSConstants.NL);
        try {
            StringBuffer sb = new StringBuffer(300);
            sb.append(envBegin).append(xmlString).append(envEnd);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("response created is: "+sb.toString() +
                        "\n--------------------");
            }
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            bop.write(sb.toString().getBytes(IFSConstants.DEFAULT_ENCODING));
            msg = fac.createMessage(mimeHeaders, new ByteArrayInputStream(
                bop.toByteArray()));
        } catch(Exception e ) {
            FSUtils.debug.error("could not build response:", e);
            return null;
        }
        return msg;
    }
    
    /*
     * Sends the passed SOAPMessage to the SOAPEndpoint URL
     * that is passed.
     * @param msg the <code>SOAPMessage</code> to be sent
     * @param soapEndPoint the SOAPEndpoint URL of remote provider
     * @return SOAPMessage response message from remote provider
     * @exception IOException, SOAPException if error occurrs
     */
    public SOAPMessage sendMessage(
        SOAPMessage msg,
        String soapEndPoint
    ) throws IOException, SOAPException 
    {
        try {
            FSUtils.debug.message("just started in func sendMessage");
            if (soapEndPoint == null) {
                FSUtils.debug.error("createSOAPReceiverURL Error!");
                String[] data = { soapEndPoint };
                LogUtil.error(
                    Level.INFO,
                    LogUtil.FAILED_SOAP_URL_END_POINT_CREATION,
                    data);
                return null;
            }
            // Send the message to the provider using the connection.
            ByteArrayOutputStream output  = new ByteArrayOutputStream();
            msg.writeTo(output);
            String xmlString = output.toString(IFSConstants.DEFAULT_ENCODING);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("SENDING message: \n " + xmlString +
                    "\nURLEndpoint :" + soapEndPoint + "\nSOAP CALL");
            }
            SOAPConnection con = scf.createConnection();
            SOAPMessage  reply = con.call(msg, soapEndPoint);
            FSUtils.debug.message("SOAP CALL COMPLETED");
            if (reply == null) {
                return null;
            }
            // check the SOAP message for any SOAP related errors
            // before passing control to SAML processor
            output  = new ByteArrayOutputStream();
            reply.writeTo(output);
            xmlString = output.toString(IFSConstants.DEFAULT_ENCODING);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("REPLIED message: \n " + xmlString);
            }
            return reply;
        } catch(Exception e){
            FSUtils.debug.error("In catch of sendMessage" , e);
            return null;
        }
    }
    
    /*
     * Method to send the passed SOAPMessage to the SOAPEndpoint URL
     * that is passed. The SOAP Message will then be sent across to the remote
     * provider in order to perform federation termination.
     * @param msg the <code>FSFederationTerminationNotification</code> 
     *  SOAPMesage to be sent
     * @param soapEndPoint the SOAPEndpoint URL of remote provider
     * @return boolean true if successful else false
     */
    public boolean sendTerminationMessage(
        SOAPMessage msg, String soapEndPoint) 
    {
        try {
            FSUtils.debug.message("started in func sendTerminationMessage");
            if(soapEndPoint == null) {
                FSUtils.debug.error("createSOAPReceiverURL Error!");
                String[] data =
                    { FSUtils.bundle.getString("failCreateURLEndpoint") };
                LogUtil.error(Level.INFO,
                              LogUtil.FAILED_SOAP_URL_END_POINT_CREATION,
                              data);
                return false;
            }
            // Send the message to the provider using the connection.
            ByteArrayOutputStream output  = new ByteArrayOutputStream();
            msg.writeTo(output);
            if (FSUtils.debug.messageEnabled()) {
                String xmlString = output.toString(
                    IFSConstants.DEFAULT_ENCODING);
                FSUtils.debug.message("SENDING message: \n " + xmlString);
                FSUtils.debug.message("URLEndpoint :" + soapEndPoint);
            }
            SOAPConnection con = scf.createConnection();
            SOAPMessage  reply = con.call(msg, soapEndPoint);
            FSUtils.debug.message("SOAP CALL COMPLETED");
            return true;
        } catch(Exception e){
            if(FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("In catch of sendTerminationMessage", e);
            }
            return false;
        }
    }

    /*
     * Parses the SOAPMessage and return the Element
     * corresponding to the liberty message(request/response).
     * @param message the <code>SOAPMessage</code> to be parsed
     * @return Element corresponding to liberty request/response
     */
    public Element parseSOAPMessage(SOAPMessage message) {
        FSUtils.debug.message("FSSOAPService.parseSOAPMessage: Called");
        ByteArrayOutputStream bop = null;
        String xmlString = null;
        
        // first check if there was any soap error during verifyHost()
        try {
            // check the SOAP message for any SOAP
            // related errros before passing control to SAM processor
            bop = new ByteArrayOutputStream();
            message.writeTo(bop);
            xmlString = bop.toString(IFSConstants.DEFAULT_ENCODING);
            Document doc = XMLUtils.toDOMDocument(xmlString, FSUtils.debug);
            Element root = doc.getDocumentElement();
            String rootName  = root.getLocalName();
            if ((rootName == null) ||(rootName.length() == 0)) {
                FSUtils.debug.error("FSSOAPService.parseSOAPMessage: " +
                    "Local name of the SOAPElement in  the" +
                    " SOAPMessage passed seems to be missing");
                return null;
            }
            if (!(rootName.equals("Envelope")) ||
                (!(root.getNamespaceURI().equals(IFSConstants.SOAP_URI)))) 
            {
                FSUtils.debug.error(
                    "FSSOAPService.parseSOAPMessage: Could not" +
                    "parse SOAPMessage, either root element is not Envelope" +
                    " or invalid name space");
                return null;
            }
            NodeList nlbody = root.getChildNodes();
            int blength = nlbody.getLength();
            if (blength <=0 ) {
                FSUtils.debug.error("FSSOAPService.parseSOAPMessage: Message " +
                    "does not have body");
                return null;
            }
            Node child = null;
            boolean found = false;
            for (int i = 0; i < blength; i++) {
                child =(Node)nlbody.item(i);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                String childName = child.getLocalName();
                if (childName.equals("Body")) {
                    found = true;
                    break; // found the message body
                }
            }
            if (!found) {
                FSUtils.debug.error("FSSOAPService.parseSOAPMessage: Message " +
                    "does not have body1");
                return null;
            }
            Element body =(Element)child;
            //Is soap-env:Fault
            NodeList nl  = body.getElementsByTagNameNS(
                IFSConstants.SOAP_URI, "Fault");
            int length = nl.getLength();
            
            if (length > 1) {
                return null;
            }
            if (length != 0 ) {
                child =(Node)nl.item(0);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                        return null;
                }
                if (child.getLocalName().equalsIgnoreCase("Fault")) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSSOAPService." +
                            " parseSOAPMessage soap-env:Fault found in the " +
                            "SOAPMessage");
                    }
                    return(Element)child;
                }
            }

            try {
                //Is samlp:Request
                child = getSAMLElement(body, "Request");
                if (child != null) {
                   return (Element) child;
                }
                //Is samlp:Response
                child = getSAMLElement(body, "Response");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:AuthnResponseEnvelope
                child = getFederationElement(body, "AuthnResponseEnvelope");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:AuthnRequest
                child = getFederationElement(body, "AuthnRequest");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:RegisterNameIdentifierRequest
                child = getFederationElement(body,
                    "RegisterNameIdentifierRequest");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:RegisterNameIdentifierResponse
                child = getFederationElement(body,
                    "RegisterNameIdentifierResponse");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:FederationTerminationNotification
                child = getFederationElement(body,
                    "FederationTerminationNotification");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:LogoutRequest
                child = getFederationElement(body, "LogoutRequest");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:LogoutResponse
                child = getFederationElement(body, "LogoutResponse");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:NameIdentifierMappingRequest
                child = getFederationElement(body, 
                    "NameIdentifierMappingRequest");
                if (child != null) {
                   return (Element) child;
                }
                //Is lib:NameIdentifierMappingResponse
                child = getFederationElement(body, 
                    "NameIdentifierMappingResponse");
                if (child != null) {
                   return (Element) child;
                }
                FSUtils.debug.error(
                    "FSSOAPService.parseMessage:Invalid message type.");
            } catch (FSException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("SOAPService.parseSOAPMessage: "
                        + "Couldn't object protocol message element.");
                }
            }
            return null;
        } catch(Exception e){
            FSUtils.debug.error("FSSOAPService.parseSOAPMessage: Exception ",e);
            return null;
        }
    }
    
    private Node getSAMLElement(Element body, String nodeName) 
        throws FSException
    {
        NodeList nl = body.getElementsByTagNameNS(
             IFSConstants.PROTOCOL_NAMESPACE_URI, nodeName);
        int length = nl.getLength();
        if (length > 1) {
            throw new FSException((String) null);
        }
        if (length != 0 ) {
            return (Node)nl.item(0);
        }
        return null;
    }

    private Node getFederationElement(Element body, String nodeName) 
        throws FSException
    {
        NodeList nl = body.getElementsByTagNameNS(
             IFSConstants.LIB_NAMESPACE_URI, nodeName);
        int length = nl.getLength();
        if (length == 0) {
             nl = body.getElementsByTagNameNS(
                 IFSConstants.FF_12_XML_NS, nodeName);
             length = nl.getLength();
        }
        if (length > 1) {
            throw new FSException((String) null);
        }
        if (length != 0 ) {
            return (Node)nl.item(0);
        }
        return null;
    }

    /*
     * Sends a synchronous SOAPMessage to remote provider.
     * @param response the http response object
     * @param msg the <code>SOAPMessage</code> to be sent
     * @param partnerDecriptor the remote provider meta descriptor
     * @param needAuthn determines forced authn
     * @return <code>SOAPMessage</code> corresponding to liberty 
     *  request/response message
     * @exception IOException, SOAPException if error occurrs
     */
    public SOAPMessage doSyncCall(
        HttpServletResponse response,
        SOAPMessage msg,
        ProviderDescriptorType partnerDecriptor,
        boolean needAuthn
    ) throws IOException, SOAPException 
    {
        FSUtils.debug.message("FSSOAPService.doSyncCall: Called");

        String soapURL = createSOAPReceiverUrl(
            response, partnerDecriptor, false);
        if (soapURL == null) {
            FSUtils.debug.error("FSSOAPService.doSyncCall: " +
                "createSOAPReceiverURL Error!");
            String[] data = 
                { FSUtils.bundle.getString("failCreateURLEndpoint") };
            LogUtil.error(Level.INFO,
                        LogUtil.FAILED_SOAP_URL_END_POINT_CREATION,
                        data);
            return null;
        }
        // Send the message to the provider using the connection.
        ByteArrayOutputStream output  = new ByteArrayOutputStream();
        msg.writeTo(output);
        String xmlString = output.toString(IFSConstants.DEFAULT_ENCODING);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSSOAPService.doSyncCall: SENDING message\n" + xmlString);
        }
        SOAPConnection con = scf.createConnection();
        SOAPMessage  reply = con.call(msg, soapURL);
        if (reply == null) {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("noReplyfromSOAPReceiver"));
            return null;
        }
        
        // check the SOAP message for any SOAP related errors
        // before passing control to SAML processor
        output  = new ByteArrayOutputStream();
        reply.writeTo(output);
        xmlString = output.toString(IFSConstants.DEFAULT_ENCODING);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSSOAPService.doSyncCall: REPLIED message:\n" + xmlString);
        }
        return reply;
    }
    
    /*
     * Method to construct the URLEndpoint depending on whether basic
     * authentication of one provider to another is to be done. Otherwise the
     * SOAPEndpoint of the remote provider is returned
     * @param response the response object
     * @param partnerDecriptor the remote provider descriptor
     * @param needAuthn determines forced authn
     * @return Element corresponding to liberty request/response
     */
    public String createSOAPReceiverUrl(
        HttpServletResponse response,
        ProviderDescriptorType partnerDecriptor,
        boolean needAuthn
    ) throws IOException 
    {
        // TODO: need to handle needAuthn correctly
        // TODO: need to retrieve auth type, user name and password from meta
        //basic authentication
        String username = null;
        String password = null;
        String to = partnerDecriptor.getSoapEndpoint();
        String authtype = null;
        String soapURL  = null;
        if (needAuthn) {
            int idnx = -1;
            if ((idnx = to.indexOf("//")) == -1) {
                FSUtils.debug.error("FSSOAPService.createSOAPReceiverUrl: " +
                    "createSOAPReceiverUrl: SOAP-Receiver-URL illegal format.");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("illegalFormatSOAPUrl"));
                return null;
            }
            String protocol = to.substring(0, idnx-1);
            if (authtype.equalsIgnoreCase(IFSConstants.BASICAUTH) ||
                authtype.equalsIgnoreCase(IFSConstants.NOAUTH)) 
            {
                if (!protocol.equals(IFSConstants.HTTP)) {
                    String[] data = { protocol , authtype }; 
                    LogUtil.error(
                        Level.INFO,
                        LogUtil.MISMATCH_AUTH_TYPE_AND_PROTOCOL,
                        data);
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                        FSUtils.bundle.getString(
                            "mismatchAuthTypeandProtocol"));
                    return null;
                }
            } else if(authtype.equalsIgnoreCase(IFSConstants.SSLWITHBASICAUTH)||
                authtype.equalsIgnoreCase(IFSConstants.SSL)) 
            {
                if (!protocol.equals(IFSConstants.HTTPS)) {
                    String[] data = { protocol, authtype };
                    LogUtil.error(Level.INFO,
                        LogUtil.MISMATCH_AUTH_TYPE_AND_PROTOCOL,
                        data);
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                        FSUtils.bundle.getString(
                            "mismatchAuthTypeandProtocol"));
                    return null;
                }
            } else {
                String[] data = { authtype };
                LogUtil.error(Level.INFO,LogUtil.WRONG_AUTH_TYPE,data);
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("wrongAuthType"));
                return null;
            }
            
            if (authtype.equalsIgnoreCase(IFSConstants.BASICAUTH) ||
                authtype.equalsIgnoreCase(IFSConstants.SSLWITHBASICAUTH)) 
            {
                Map userMap = null; //partnerDecriptor.getAuthType();
                username =(String) userMap.get(IFSConstants.USER);
                password =(String) userMap.get(IFSConstants.PASSWORD);
                if (username == null || password == null) {
                    FSUtils.debug.error(
                        "FSSOAPService.createSOAPReceiverUrl: " +
                        "PartnerSite required basic authentication. But " +
                        "the user name used for authentication is null.");
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                        FSUtils.bundle.getString("wrongConfigBasicAuth"));
                    return null;
                }
                StringBuffer toSOAP = new StringBuffer(100);
                toSOAP.append(to.substring(0, idnx+2)).append(username).
                    append(":").append(password).append("@").
                    append(to.substring(idnx+2));
                soapURL  = toSOAP.toString();
            }
            return null;
        } else {
            soapURL = to;
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSSOAPService.createSOAPReceiverUrl: Sending message to URL: "
                + soapURL);
        }
        String[] data = { soapURL };
        LogUtil.access(Level.FINER,"SOAP_RECEIVER_URL", data);
        return soapURL;
    }
    
    /**
     * Forms a SOAP Fault and puts it in the SOAP Message's Body.
     *
     * @param faultcode fault code to be set in SOAPMEssage
     * @param faultString fault string
     * @param detail the details of the fault condition
     * @return <code>SOAPMessage</code> containing the SOAP fault
     */
    public SOAPMessage formSOAPError(
        String faultcode,
        String faultString,
        String detail
    ) {
        SOAPMessage msg = null ;
        SOAPEnvelope envelope = null;
        SOAPFault sf = null;
        SOAPBody body = null;
        SOAPElement se = null;
        try {
            msg = fac.createMessage();
            envelope = msg.getSOAPPart().getEnvelope();
            body = envelope.getBody();
            sf = body.addFault();
            Name qname = envelope.createName(
                faultcode, null, IFSConstants.SOAP_URI);
            sf.setFaultCode(qname);
            sf.setFaultString(FSUtils.bundle.getString(faultString));
            if ((detail != null) && !(detail.length() == 0)) {
                Detail det = sf.addDetail();
                se =(SOAPElement)det.addDetailEntry(envelope.createName(
                    "Problem"));
                se.addAttribute(envelope.createName("details"), 
                    FSUtils.bundle.getString(detail));
            }
        } catch( SOAPException e ) {
            FSUtils.debug.error(
                "FSSOAPService.formSOAPError:", e);
            return null;
        }
        return msg;
    }
    
    /**
     * Returns an instance of <code>FSSOAPService</code> instance.
     *
     * @return an instance of <code>FSSOAPService</code> instance.
     */
    public static FSSOAPService getInstance() {
        FSUtils.debug.message("FSSOAPService.getInstance: Called");
        synchronized(FSServiceManager.class) {
            if (instance == null){
                FSUtils.debug.message(
                    "Constructing a new instance of FSSOAPService");
                instance = new FSSOAPService();
            }
            return instance;
        }
    }
}
