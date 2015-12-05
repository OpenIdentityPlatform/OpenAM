/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.saml2.common;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;

/**
 * The SOAPCommunicator is a utility package to assist in SAML2 communication over SOAP.
 */
public class SOAPCommunicator {

    private static final Debug debug = Debug.getInstance("libSAML2");
    private SOAPConnectionFactory soapConnectionFactory; // TODO: use Guice
    private MessageFactory messageFactory; // TODO: use Guice

    private static SOAPCommunicator instance = new SOAPCommunicator(); // TODO: use Guice

    private SOAPCommunicator() {
        try {
            soapConnectionFactory = SOAPConnectionFactory.newInstance();
            messageFactory = MessageFactory.newInstance();
        } catch (SOAPException se) {
            debug.error("SOAPCommunicator: Unable to create SOAP MessageFactory", se);
        }
    }

    /**
     * Gets the singleton instance of the SOAPCommunicator.
     *
     * @return the SOAPCommunicator instance.
     */
    public static SOAPCommunicator getInstance() {
        return instance;
    }

    /**
     * Opens a SOAP Connection.
     *
     * @return a new <code>SOAPConnection</code>
     * @throws if there was an exception creating the
     *            <code>SOAPConnection</code> object.
     */
    public SOAPConnection openSOAPConnection() throws SOAPException {
        return soapConnectionFactory.createConnection();
    }

    /**
     * Creates <code>SOAPMessage</code> with the input XML String
     * as message body.
     *
     * @param xmlString       XML string to be put into <code>SOAPMessage</code> body.
     * @param isClientMessage true if the message is sent from SOAP client to
     *                        server.
     * @return newly created <code>SOAPMessage</code>.
     * @throws SOAPException if it cannot create the
     *                       <code>SOAPMessage</code>.
     */
    public SOAPMessage createSOAPMessage(final String xmlString,
                                         final boolean isClientMessage)
            throws SOAPException, SAML2Exception {
        return createSOAPMessage(null, xmlString, isClientMessage);
    }

    /**
     * Creates <code>SOAPMessage</code> with the input XML String
     * as message header and body.
     *
     * @param header          XML string to be put into <code>SOAPMessage</code> header.
     * @param body            XML string to be put into <code>SOAPMessage</code> body.
     * @param isClientMessage true if the message is sent from SOAP client to
     *                        server.
     * @return newly created <code>SOAPMessage</code>.
     * @throws SOAPException if it cannot create the <code>SOAPMessage</code>.
     */
    public SOAPMessage createSOAPMessage(final String header,
                                         final String body,
                                         final boolean isClientMessage)
            throws SOAPException, SAML2Exception {

        try {
            MimeHeaders mimeHeaders = new MimeHeaders();
            mimeHeaders.addHeader("Content-Type", "text/xml");
            if (isClientMessage) {
                mimeHeaders.addHeader("SOAPAction", "\"\"");
            }
            if (debug.messageEnabled()) {
                debug.message("SOAPCommunicator.createSOAPMessage: header = " +
                        header + ", body = " + body);
            }

            StringBuilder sb = new StringBuilder(500);
            sb.append("<").append(SAMLConstants.SOAP_ENV_PREFIX)
                    .append(":Envelope").append(SAMLConstants.SPACE)
                    .append("xmlns:").append(SAMLConstants.SOAP_ENV_PREFIX)
                    .append("=\"").append(SAMLConstants.SOAP_URI).append("\">");
            if (header != null) {
                sb.append("<")
                        .append(SAMLConstants.SOAP_ENV_PREFIX).append(":Header>")
                        .append(header)
                        .append(SAMLConstants.START_END_ELEMENT)
                        .append(SAMLConstants.SOAP_ENV_PREFIX)
                        .append(":Header>");
            }
            if (body != null) {
                sb.append("<")
                        .append(SAMLConstants.SOAP_ENV_PREFIX).append(":Body>")
                        .append(body)
                        .append(SAMLConstants.START_END_ELEMENT)
                        .append(SAMLConstants.SOAP_ENV_PREFIX)
                        .append(":Body>");
            }
            sb.append(SAMLConstants.START_END_ELEMENT)
                    .append(SAMLConstants.SOAP_ENV_PREFIX)
                    .append(":Envelope>").append(SAMLConstants.NL);

            if (debug.messageEnabled()) {
                debug.message("SOAPCommunicator.createSOAPMessage: soap message = " +
                        sb.toString());
            }

            return messageFactory.createMessage(mimeHeaders, new ByteArrayInputStream(
                    sb.toString().getBytes(SAML2Constants.DEFAULT_ENCODING)));
        } catch (IOException io) {
            debug.error("SOAPCommunicator.createSOAPMessage: IOE", io);
            throw new SAML2Exception(io.getMessage());
        }
    }

    /**
     * Returns SOAP body as DOM Element from SOAPMessage.
     *
     * @param message SOAPMessage object.
     * @return SOAP body, return null if unable to get the SOAP body element.
     */
    public Element getSOAPBody(final SOAPMessage message)
            throws SAML2Exception {
        debug.message("SOAPCommunicator.getSOAPBody : start");

        // check the SOAP message for any SOAP
        // related errors before passing control to SAML processor
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        try {
            message.writeTo(byteArrayOut);
        } catch (IOException ie) {
            debug.error("SOAPCommunicator.getSOAPBody : writeTo IO", ie);
            throw new SAML2Exception(ie.getMessage());
        } catch (SOAPException se) {
            debug.error("SOAPCommunicator.getSOAPBody : writeTo SOAP", se);
            throw new SAML2Exception(se.getMessage());
        }
        ByteArrayInputStream byteArrayIn = new ByteArrayInputStream(byteArrayOut.toByteArray());
        Document doc = XMLUtils.toDOMDocument(byteArrayIn, debug);
        Element root = doc.getDocumentElement();
        if (debug.messageEnabled()) {
            debug.message("SOAPCommunicator.getSOAPBody : soap body =\n"
                    + XMLUtils.print((Node) root));
        }
        String rootName = doc.getDocumentElement().getLocalName();
        if (StringUtils.isEmpty(rootName)) {
            debug.error("SOAPCommunicator.getSOAPBody : no local name");
            throw new SAML2Exception(SAML2Utils.bundle.getString("missingLocalName"));
        }
        if (!(rootName.equals("Envelope")) ||
                (!(SAMLConstants.SOAP_URI.equals(root.getNamespaceURI())))) {
            debug.error("SOAPCommunicator.getSOAPBody : either root " +
                    "element is not Envelope or invalid name space or prefix");
            throw new SAML2Exception(SAML2Utils.bundle.getString("invalidSOAPElement"));
        }
        NodeList nodeList = root.getChildNodes();
        int length = nodeList.getLength();
        if (length <= 0) {
            debug.error("SOAPCommunicator.getSOAPBody: no msg body");
            throw new SAML2Exception(SAML2Utils.bundle.getString("missingSOAPBody"));
        }
        for (int i = 0; i < length; i++) {
            Node child = nodeList.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                debug.message("SOAPCommunicator.getSOAPBody: " + child);
                continue;
            }
            String childName = child.getLocalName();
            if (debug.messageEnabled()) {
                debug.message("SOAPCommunicator.getSOAPBody: local name= " + childName);
            }
            if (childName.equals("Body") && SAMLConstants.SOAP_URI.equals(child.getNamespaceURI())) {
                // found the Body element
                return (Element) child;
            }
        }
        throw new SAML2Exception(SAML2Utils.bundle.getString(
                "missingSOAPBody"));
    }

    /**
     * Forms a SOAP Fault and puts it in the SOAP Message Body.
     *
     * @param faultCode   Fault code.
     * @param faultString Fault string.
     * @param detail      Fault details.
     * @return SOAP Fault in the SOAP Message Body or null if unable to generate the message.
     */
    public SOAPMessage createSOAPFault(final String faultCode,
                                       final String faultString,
                                       final String detail) {
        try {
            SOAPMessage message = messageFactory.createMessage();
            SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
            SOAPFault fault = envelope.getBody().addFault();
            fault.setFaultCode(envelope.createName(faultCode, null, SOAPConstants.URI_NS_SOAP_ENVELOPE));
            fault.setFaultString(SAML2Utils.bundle.getString(faultString));
            if (StringUtils.isNotEmpty(detail)) {
                Detail faultDetail = fault.addDetail();
                SOAPElement faultDetailEntry = (SOAPElement) faultDetail.addDetailEntry(envelope.createName(
                        "Problem"));
                faultDetailEntry.addAttribute(envelope.createName("details"), SAML2Utils.bundle.getString(detail));
            }
            return message;
        } catch (SOAPException e) {
            debug.error("createSOAPFault:", e);
            return null;
        }
    }

    /**
     * Returns SOAP Message from <code>HttpServletRequest</code>.
     *
     * @param request <code>HttpServletRequest</code> includes SOAP Message.
     * @return SOAPMessage if request include any soap message in the header.
     * @throws IOException   if error in creating input stream.
     * @throws SOAPException if error in creating soap message.
     */
    public SOAPMessage getSOAPMessage(final HttpServletRequest request)
            throws IOException, SOAPException {
        // Get all the headers from the HTTP request
        MimeHeaders headers = getHeaders(request);
        // Get the body of the HTTP request
        InputStream is = request.getInputStream();

        // Create a SOAPMessage
        return messageFactory.createMessage(headers, is);
    }

    /**
     * Send SOAP Message to specified url and returns message from peer.
     *
     * @param xmlMessage      <code>String</code> will be sent.
     * @param soapUrl         URL the mesaage send to.
     * @param isClientMessage true if the message is sent from SOAP client to
     *                        server.
     * @return SOAPMessage if the peer send back any reply.
     * @throws SOAPException  if error in creating soap message.
     * @throws SAML2Exception if error in creating soap message.
     */
    public SOAPMessage sendSOAPMessage(final String xmlMessage,
                                       final String soapUrl,
                                       final boolean isClientMessage)
            throws SOAPException, SAML2Exception {
        SOAPConnection con = soapConnectionFactory.createConnection();
        SOAPMessage msg = createSOAPMessage(xmlMessage, isClientMessage);
        return con.call(msg, soapUrl);
    }

    /**
     * Converts a <code>SOAPMessage</code> to a <code>String</code>.
     *
     * @param message SOAPMessage object.
     * @return the <code>String</code> converted from the
     * <code>SOAPMessage</code> or null if an error ocurred.
     */
    public String soapMessageToString(final SOAPMessage message) {
        try {
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            message.writeTo(bop);
            return new String(bop.toByteArray());
        } catch (IOException ie) {
            debug.error("SOAPCommunicator.soapMessageToString:", ie);
            return null;
        } catch (SOAPException soapex) {
            debug.error("SOAPCommunicator.soapMessageToString:", soapex);
            return null;
        }
    }

    /**
     * Returns first Element with given local name in samlp name space inside
     * SOAP message.
     *
     * @param message   SOAP message.
     * @param localName local name of the Element to be returned.
     * @return first Element matching the local name.
     * @throws SAML2Exception if the Element could not be found or there is
     *                        SOAP Fault present.
     */
    public Element getSamlpElement(final SOAPMessage message,
                                   final String localName) throws SAML2Exception {

        Element body = getSOAPBody(message);
        NodeList bodyChildNodes = body.getChildNodes();

        int childNodeLength = bodyChildNodes.getLength();
        if (childNodeLength <= 0) {
            debug.error("SOAPCommunicator.getSamlpElement: empty body");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missingBody"));
        }
        Element returnElement = null;
        Node node;
        for (int i = 0; i < childNodeLength; i++) {
            node = bodyChildNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String nlName = node.getLocalName();
            if (debug.messageEnabled()) {
                debug.message("SOAPCommunicator.getSamlpElement: node={}, nsURI={}",
                        nlName, node.getNamespaceURI());
            }
            if (nlName.equals("Fault")) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "soapFaultInSOAPResponse"));
            } else if (nlName.equals(localName) &&
                    SAML2Constants.PROTOCOL_NAMESPACE.equals(
                            node.getNamespaceURI())) {
                returnElement = (Element) node;
                break;
            }
        }
        if (returnElement == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("elementNotFound") +
                    localName);
        }
        return returnElement;
    }

    /**
     * Returns mime headers in HTTP servlet request.
     *
     * @param req HTTP servlet request.
     * @return mime headers in HTTP servlet request.
     */
    public MimeHeaders getHeaders(final HttpServletRequest req) {
        Enumeration<String> e = req.getHeaderNames();
        MimeHeaders headers = new MimeHeaders();
        while (e.hasMoreElements()) {
            String headerName = e.nextElement();
            String headerValue = req.getHeader(headerName);

            debug.message("SOAPCommunicator.getHeaders: Header name={}, value={}",
                    headerName, headerValue);
            StringTokenizer values =
                    new StringTokenizer(headerValue, ",");
            while (values.hasMoreTokens()) {
                headers.addHeader(
                        headerName, values.nextToken().trim());
            }
        }

        if (debug.messageEnabled()) {
            debug.message("SOAPCommunicator.getHeaders: Header=" + headers.toString());
        }
        return headers;
    }
}
