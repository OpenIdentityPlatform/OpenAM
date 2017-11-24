/**
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
 * $Id: SecureSOAPMessage.java,v 1.30 2010/01/23 00:20:27 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPPart;

import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.security.SecurityMechanism;
import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.security.SecurityToken;
import com.sun.identity.wss.security.AssertionToken;
import com.sun.identity.wss.security.SecurityPrincipal;
import com.sun.identity.wss.security.BinarySecurityToken;
import com.sun.identity.wss.security.UserNameToken;
import com.sun.identity.wss.security.SAML2Token;
import com.sun.identity.wss.security.SAML2TokenUtils;
import com.sun.identity.wss.logging.LogUtil;

import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.PublicKey;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.Constants;

import com.sun.identity.xmlenc.XMLEncryptionManager;
import com.sun.identity.xmlenc.EncryptionConstants;
import com.sun.identity.xmlenc.EncryptionException;
import com.sun.identity.shared.DateUtils;
import java.text.ParseException;


/**
 * This class <code>SecureSOAPMessage</code> constructs the secured 
 * <code>SOAPMessage</code> for the given security mechanism token.
 * @supported.all.api
 */
public class SecureSOAPMessage {

     private SOAPMessage soapMessage = null;
     private SecurityToken securityToken = null;
     private SecurityMechanism securityMechanism = null;
     private boolean create = false;
     private Element wsseHeader = null;
     private X509Certificate messageCertificate = null;
     private static Debug debug = WSSUtils.debug;
     private static ResourceBundle bundle = WSSUtils.bundle;

     private String server_proto =
     SystemConfigurationUtil.getProperty(Constants.AM_SERVER_PROTOCOL);
     private String server_host  =
     SystemConfigurationUtil.getProperty(Constants.AM_SERVER_HOST);
     private String server_port  =
     SystemConfigurationUtil.getProperty(Constants.AM_SERVER_PORT);
     private List signingIds = new ArrayList();
     private String messageID = null;
     private long msgTimestamp = 0;
     private SecurityContext securityContext = null;
     private String clientDnsClaim = null;
     private List signedElements = new ArrayList();

     /**
      * Constructor to create secure SOAP message.
      *
      * @param soapMessage the SOAP message to be secured.
      *
      * @param create if true, creates a new secured SOAP message by adding
      *               security headers.
      *               if false, parses the secured SOAP message.
      *
      * @exception SecurityException if failed in creating or parsing the
      *            new secured SOAP message.
      */
     public SecureSOAPMessage(SOAPMessage soapMessage, boolean create)
             throws SecurityException {
         this(soapMessage, create, new ArrayList());
     }

     /**
      * Constructor to create secure SOAP message. 
      *
      * @param soapMessage the SOAP message to be secured.
      *
      * @param create if true, creates a new secured SOAP message by adding
      *               security headers.
      *               if false, parses the secured SOAP message.
      * @param signedElements list of signed elements
      *
      * @exception SecurityException if failed in creating or parsing the
      *            new secured SOAP message. 
      */
     public SecureSOAPMessage(SOAPMessage soapMessage, boolean create,
             List signedElements) throws SecurityException {
          
         this.soapMessage = soapMessage;
         this.create = create;
         this.signedElements = signedElements;

         if(debug.messageEnabled()) {
            debug.message("SecureSOAPMessage.Input SOAP message : " + 
            WSSUtils.print(soapMessage.getSOAPPart()));
         }

         if(!create) {
             parseSOAPMessage(soapMessage);
         } else {
             ((Node) soapMessage.getSOAPPart()).normalize();

             if(debug.messageEnabled()) {
                debug.message("SecureSOAPMessage.Input SOAP message After " + 
                "normalization: "+ WSSUtils.print(soapMessage.getSOAPPart()));
             }
             addNameSpaces();
             addSecurityHeader();
             
         }
         
         if (debug.messageEnabled()) {
             debug.message("SecureSOAPMessage.Output SOAP message: " + WSSUtils.print(soapMessage.getSOAPPart()));
         }
     }

     /**
      * Returns the Security Header Element.
      *
      * @return the Security Header Element.
      */
     public Element getSecurityHeaderElement() {
         return this.wsseHeader;
     }

     /**
      * Returns the secured SOAP message.
      *
      * @return the secured SOAP message.
      */
     public SOAPMessage getSOAPMessage() {
         return this.soapMessage;
     }

     /**
      * Sets the secured SOAP message.
      *
      * @param inSoapMessage the input secured SOAP message.
      */
     public void setSOAPMessage(SOAPMessage inSoapMessage) {
         this.soapMessage = inSoapMessage;
     }

     /**
      * Parses the secured SOAP message.
      * @param soapMessage the secured SOAP message which needs to be parsed.
      *
      * @exception SecurityException if there is any failure in parsing.
      */
     private void parseSOAPMessage(SOAPMessage soapMessage) 
                throws SecurityException {
         try {
             SOAPHeader header = 
                   soapMessage.getSOAPPart().getEnvelope().getHeader();   
             if(header == null) {
                if(debug.messageEnabled()) {
                   debug.message("SecureSOAPMessage.parseSOAPMessage: " +
                     "No SOAP header found.");
                }
             }
             NodeList headerChildNodes = header.getChildNodes();
             if((headerChildNodes == null) || 
                        (headerChildNodes.getLength() == 0)) {
                if(debug.messageEnabled()) { 
                   debug.message("SecureSOAPMessage.parseSOAPMessage: " +
                     "No security header found.");
                }
             }
             for(int i=0; i < headerChildNodes.getLength(); i++) {

                 Node currentNode = headerChildNodes.item(i);
                 if(currentNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                 }
                 
                 String nodeName = currentNode.getLocalName();
                 String nodeNS = currentNode.getNamespaceURI();
                 
                 if((WSSConstants.WSSE_SECURITY_LNAME.equals(nodeName)) &&                       
                    (WSSConstants.WSSE_NS.equals(nodeNS))) {                       
                    wsseHeader = (Element) currentNode;
                 }
                 
                 if((WSSConstants.wsaMessageID.equals(nodeName)) &&
                         (WSSConstants.wsaNS.equals(nodeNS))) {
                     messageID = XMLUtils.getElementValue((Element)currentNode);       
                 }
                 
                 if(("From".equals(nodeName)) &&
                         (WSSConstants.wsaNS.equals(nodeNS))) {
                     Element fromElement = (Element)currentNode;
                     NodeList nodeList =
                             fromElement.getElementsByTagNameNS(
                             WSSConstants.WSID_NS, WSSConstants.DNS_CLAIM);
                     if(nodeList == null || nodeList.getLength() == 0) {
                        continue;
                     }
                     clientDnsClaim = XMLUtils.getElementValue(
                             (Element)nodeList.item(0));
                 }
             }            
         } catch (SOAPException se) {
             debug.error("SecureSOAPMessage.parseSOAPMessage: SOAP" +
             "Exception in parsing the headers.", se);
             String[] data = {se.getLocalizedMessage()};
             LogUtil.error(Level.INFO,
                        LogUtil.ERROR_PARSING_SOAP_HEADERS,
                        data,
                        null);
             throw new SecurityException(se.getMessage());
         }
     }

     /**
      * Parses for the security header.
      * @param node security header node.
      *
      * @exception SecurityException if there is any error occured.
      */
     public void parseSecurityHeader(Node node) throws SecurityException {
         securityMechanism = SecurityMechanism.WSS_NULL_ANONYMOUS;
         if (node != null) {
             NodeList securityHeaders = node.getChildNodes();
             for(int i=0; i < securityHeaders.getLength(); i++) {
                 Node currentNode =  securityHeaders.item(i);
                 if(currentNode.getNodeType() != Node.ELEMENT_NODE) {
                     continue;
                 }
                 String localName =  currentNode.getLocalName();
                 String nameSpace = currentNode.getNamespaceURI();

                 if( (SAMLConstants.TAG_ASSERTION.equals(localName)) &&
                     (SAMLConstants.assertionSAMLNameSpaceURI.equals(
                         nameSpace)) ) {

                     if(debug.messageEnabled()) {
                         debug.message("SecureSOAPMessage.parseSecurityHeader:: "
                             + "Assertion token found in the security header.");
                     }
                     try {
                         securityToken = 
                             new AssertionToken((Element)currentNode);
                         AssertionToken assertionToken = 
                             (AssertionToken)securityToken;
                         if(assertionToken.isSenderVouches()) {
                             securityMechanism = 
                                 SecurityMechanism.WSS_NULL_SAML_SV;
                         } else {
                             securityMechanism = 
                                 SecurityMechanism.WSS_NULL_SAML_HK;
                         }
                         messageCertificate = 
                             WSSUtils.getCertificate(assertionToken);
                     } catch (SAMLException se) {
                         debug.error("SecureSOAPMessage.parseSecurity" +
                             "Header: unable to parse the token", se);
                         throw new SecurityException(se.getMessage());
                     }
                
                 } else if( (SAMLConstants.TAG_ASSERTION.equals(localName)) &&
                     (SAML2Constants.ASSERTION_NAMESPACE_URI.equals(
                         nameSpace)) ) {

                     if(debug.messageEnabled()) {
                         debug.message("SecureSOAPMessage.parseSecurityHeader:: "
                             + "SAML2 token found in the security header.");
                     }
                     try {
                         securityToken = new SAML2Token((Element)currentNode);
                         SAML2Token saml2Token = (SAML2Token)securityToken;
                         if(saml2Token.isSenderVouches()) {
                             securityMechanism = 
                                 SecurityMechanism.WSS_NULL_SAML2_SV;
                         } else {
                             securityMechanism = 
                                 SecurityMechanism.WSS_NULL_SAML2_HK;
                         }
                         messageCertificate = 
                             SAML2TokenUtils.getCertificate(saml2Token);                         
                     } catch (SAML2Exception se) {
                         debug.error("SecureSOAPMessage.parseSecurity" +
                             "Header: unable to parse the token", se);
                         throw new SecurityException(se.getMessage());
                     }

                 } else if( (WSSConstants.TAG_BINARY_SECURITY_TOKEN.
                         equals(localName)) && 
                        (WSSConstants.WSSE_NS.equals(nameSpace)) ) {

                     if(debug.messageEnabled()) {
                         debug.message("SecureSOAPMessage.parseSecurityHeader:: "
                             + "binary token found in the security header.");
                     }
                     securityToken = 
                         new BinarySecurityToken((Element)currentNode);
                     if(securityToken.getTokenType().equals(
                             securityToken.WSS_KERBEROS_TOKEN)) {
                        securityMechanism = 
                                SecurityMechanism.WSS_NULL_KERBEROS_TOKEN; 
                     } else  {
                        securityMechanism = 
                                SecurityMechanism.WSS_NULL_X509_TOKEN;
                        messageCertificate = 
                                WSSUtils.getCertificate(securityToken);
                     }

                 } else if( (WSSConstants.TAG_USERNAME_TOKEN.equals(localName)) &&
                        (WSSConstants.WSSE_NS.equals(nameSpace)) ) {

                     if(debug.messageEnabled()) {
                         debug.message("SecureSOAPMessage.parseSecurityHeader:: "
                             + "username token found in the security header.");
                     }
                     securityToken = new UserNameToken((Element)currentNode);
                     UserNameToken usernameToken = (UserNameToken)securityToken;
                     String passwordType = usernameToken.getPasswordType(); 
                     if (passwordType != null) {
                         if (passwordType.equals(
                             WSSConstants.PASSWORD_DIGEST_TYPE)) {
                             securityMechanism = 
                                 SecurityMechanism.WSS_NULL_USERNAME_TOKEN;
                         } else if 
                             (passwordType.equals(
                             WSSConstants.PASSWORD_PLAIN_TYPE)) {
                             securityMechanism = 
                                 SecurityMechanism.WSS_NULL_USERNAME_TOKEN_PLAIN;
                         }
                     }

                 } else if((SAMLConstants.XMLSIG_ELEMENT_NAME.equals(localName))
                     && (SAMLConstants.XMLSIG_NAMESPACE_URI.equals(nameSpace))){
                     if(securityToken != null) {
                        continue; 
                     }
                     messageCertificate = 
                             WSSUtils.getMessageCertificate((Element)node);
                     if(messageCertificate != null) {
                        securityMechanism = 
                                SecurityMechanism.WSS_NULL_X509_TOKEN; 
                     }
                 } else if((WSSConstants.TIME_STAMP.equals(localName)) ||
                         (WSSConstants.WSSE_NS.equals(nameSpace))) {
                     if(!validateTimestamp((Element)currentNode)) {
                        throw new SecurityException(
                                bundle.getString("invalidTimestamp")); 
                     }
                 } else {
                     if(debug.messageEnabled()) {
                         debug.message("SecureSOAPMessage.parseSecurityHeader: "
                             + "ignore header element, " + localName);                              
                     }                     
                 }
             }
         }
     }

     /**
      * Returns the security mechanism of the secure soap message.
      *
      * @return SecurityMechanism the security mechanism of the secure
      *         <code>SOAPMessage</code>.
      */
     public SecurityMechanism getSecurityMechanism() {
         return securityMechanism;    
     }

     /**
      * Sets the security mechanism for securing the soap message.
      *
      * @param securityMechanism the security mechanism that will be used
      *        to secure the soap message.
      */
     public void setSecurityMechanism(SecurityMechanism securityMechanism) {
         this.securityMechanism = securityMechanism;
     }

     /**
      * Sets the security token for securing the soap message.
      *
      * @param token the security token that is used to secure the soap message.
      *
      * @exception SecurityException if the security token can not be added
      *       to the security header. 
      */
     public void setSecurityToken(SecurityToken token) 
                  throws SecurityException {

         if(wsseHeader == null) {
            debug.error("SecureSOAPMessage.setSecurityToken:: WSSE security" +
            " Header is not found in the Secure SOAP Message.");
            throw new SecurityException(
                 bundle.getString("securityHeaderNotFound"));
         }
         this.securityToken = token;
         String tokenType = securityToken.getTokenType();
         if(SecurityToken.WSS_USERNAME_TOKEN.equals(tokenType)) {
            UserNameToken userNameToken = (UserNameToken)securityToken;
            if(signedElements.contains(WSSConstants.SECURITY_TOKEN)) {
               signingIds.add(userNameToken.getSigningId());
            }
         } else if(SecurityToken.WSS_X509_TOKEN.equals(tokenType)) {
            BinarySecurityToken binaryToken = 
                    (BinarySecurityToken)securityToken;
            if(signedElements.contains(WSSConstants.SECURITY_TOKEN)) {
               signingIds.add(binaryToken.getSigningId());
            }
         }
         Element tokenE = token.toDocumentElement();
         Node tokenNode = soapMessage.getSOAPPart().importNode(tokenE, true);
         WSSUtils.prependChildElement(wsseHeader, (Element)tokenNode, true, 
                (Document)soapMessage.getSOAPPart());
         try {
             soapMessage.saveChanges();
         } catch (SOAPException se) {
             debug.error("SecureSOAPMessage.setSecurityToken: " +
                     "SOAPException" , se);
             throw new SecurityException(se.getMessage());
         }
     }

     /**
      * Returns the security token associated with this secure soap message.
      *
      * @return SecurityToken the security token for this secure soap message.
      */
     public SecurityToken getSecurityToken() {
         return securityToken;
     }
     
     public SecurityContext getSecurityContext() {
         return securityContext;
     }
     
     public void setSecurityContext(SecurityContext securityContext) {
         this.securityContext = securityContext;
     }

     /**
      * Adds the WSSE related name spaces to the SOAP Envelope.
      */
     private void addNameSpaces() throws SecurityException {
         try {
             SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
             envelope.setAttributeNS(WSSConstants.NS_XML,
                       WSSConstants.TAG_XML_WSU,
                       WSSConstants.WSU_NS);

             SOAPBody body = envelope.getBody();
             body.setAttributeNS(WSSConstants.NS_XML,
                       WSSConstants.TAG_XML_WSU,
                       WSSConstants.WSU_NS);
             body.setAttribute(WSSConstants.WSU_ID, SAMLUtils.generateID());
             
         } catch (SOAPException se) {
             debug.error("SecureSOAPMessage.addNameSpaces:: Could not add " + 
             "Name spaces. ", se);
             throw new SecurityException(
                   bundle.getString("nameSpaceAdditionfailure"));
         }
     }

     /**
      * Adds the security header to the SOAP Envelope.
      */
     private void addSecurityHeader() throws SecurityException {

         if(debug.messageEnabled()) {
            debug.message("SecureSOAPMessage.addSecurityHeader:: preparing the"+
            " security header");
         }
         try {
             SOAPPart soapPart = soapMessage.getSOAPPart();
             SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
             SOAPHeader header = envelope.getHeader(); 
             if(header == null) {
                header = envelope.addHeader();
             }
             checkForAddressingHeaders();
             
             wsseHeader = soapPart.createElementNS(
                          WSSConstants.WSSE_NS,
                          WSSConstants.WSSE_TAG + ":" +
                          WSSConstants.WSSE_SECURITY_LNAME);
             wsseHeader.setAttributeNS(
                          WSSConstants.NS_XML,
                          WSSConstants.TAG_XML_WSSE,
                          WSSConstants.WSSE_NS);
             wsseHeader.setAttributeNS(WSSConstants.NS_XML,
                          WSSConstants.TAG_XML_WSU,
                          WSSConstants.WSU_NS);
             wsseHeader.setAttributeNS(
                     WSSConstants.NS_XML,
                     WSSConstants.TAG_XML_WSSE11,
                     WSSConstants.WSSE11_NS);

             String envPrefix = envelope.getPrefix();
             if(envPrefix != null) {
                wsseHeader.setAttribute(envPrefix + ":" +
                          WSSConstants.MUST_UNDERSTAND, "1");
             }

             //Add time stamp
             Element timeStamp = soapPart.createElementNS(
                          WSSConstants.WSU_NS, 
                          WSSConstants.WSU_TAG + ":" +
                          WSSConstants.TIME_STAMP);
             String tsId = SAMLUtils.generateID();
             if(signedElements.contains(WSSConstants.TIME_STAMP)) {
                if(signingIds != null) {
                   signingIds.add(tsId);
                }
             }
             timeStamp.setAttribute(WSSConstants.WSU_ID, tsId);
             wsseHeader.appendChild(timeStamp);
             Element created = soapPart.createElementNS(
                          WSSConstants.WSU_NS,
                          WSSConstants.WSU_TAG + ":" + WSSConstants.CREATED);

             Date createTime = new Date();
             Date expireTime = new Date(); 
             expireTime.setTime(createTime.getTime() + 
                          WSSConstants.INTERVAL * 1000);

             created.appendChild(soapPart.createTextNode(
                          DateUtils.toUTCDateFormat(createTime))); 
             timeStamp.appendChild(created);
 
             Element expires = soapPart.createElementNS(
                          WSSConstants.WSU_NS,
                          WSSConstants.WSU_TAG + ":" + WSSConstants.EXPIRES);
             expires.appendChild(soapPart.createTextNode(
                          DateUtils.toUTCDateFormat(expireTime))); 
             timeStamp.appendChild(expires);
             header.appendChild(wsseHeader);

         } catch (SOAPException se) {
             debug.error("SecureSOAPMessage.addSecurityHeader:: SOAPException"+
             " while adding the security header.", se);
             String[] data = {se.getLocalizedMessage()};
             LogUtil.error(Level.INFO,
                        LogUtil.ERROR_ADDING_SECURITY_HEADER,
                        data,
                        null);
             throw new SecurityException(
                    bundle.getString("addSecurityHeaderFailed"));
         }
     }

     private void checkForAddressingHeaders() throws SecurityException {
         try {
             SOAPHeader header = soapMessage.getSOAPHeader();
             java.util.Iterator childElements = header.getChildElements();
             while(childElements.hasNext()) {
                Element childElement = (Element)childElements.next();
                String localName = childElement.getLocalName();
                String nameSpace = childElement.getNamespaceURI();
                if(WSSConstants.wsaNS.equals(nameSpace)
                        &&( localName.equals("To") ||
                            localName.equals("From") ||
                            localName.equals("MessageID") ||
                            localName.equals("Action"))) {
                   childElement.setAttributeNS(WSSConstants.NS_XML,
                       WSSConstants.TAG_XML_WSU,
                       WSSConstants.WSU_NS);
                   String id = SAMLUtils.generateID();
                   childElement.setAttribute(WSSConstants.WSU_ID, id);
                   if(signedElements.contains(localName)) {
                      signingIds.add(id);
                   }
                }

             }            

         } catch (SOAPException se) {
             debug.error("SecureSOAPMessage.addNameSpaces:: Could not add " +
             "Name spaces. ", se);
             throw new SecurityException(
                   bundle.getString("nameSpaceAdditionfailure"));
         }
     }

    /**
     * Signs the <code>SOAPMessage</code>  for the given security profile.
     *
     * @exception SecurityException if there is any failure in signing.
     */
     public void sign() 
             throws SecurityException {

         if(debug.messageEnabled()) {
            debug.message("SecureSOAPMessage.sign:: Before Signing : "+
            WSSUtils.print(soapMessage.getSOAPPart()));
         }
         
         Document doc = toDocument();
         String tokenType = null;
         // securityToken=null if the secmech is Anonymous
         if (securityToken!=null) {
             tokenType = securityToken.getTokenType();
         } else {
            if((securityMechanism != null) && 
                   (securityMechanism.getURI().equals(
                              SecurityMechanism.WSS_NULL_X509_TOKEN_URI))) {
                tokenType = SecurityToken.WSS_X509_TOKEN;
            }
                    
         }

         if(SecurityToken.WSS_SAML_TOKEN.equals(tokenType) ||
                 SecurityToken.WSS_SAML2_TOKEN.equals(tokenType)) {
            signWithAssertion(doc);
         } else if(SecurityToken.WSS_X509_TOKEN.equals(tokenType)) {
            signWithBinaryToken(doc, securityContext.getSigningCertAlias(),
                    securityContext.getSigningRef());
            // treat Anonymous secmech (securityToken=null) same as UserName
            // Token
         } else if ((SecurityToken.WSS_USERNAME_TOKEN.equals(tokenType)) ||
                 (null==securityToken)){
             signWithUNToken(doc, securityContext.getSigningCertAlias());
         } else if ((SecurityToken.WSS_KERBEROS_TOKEN.equals(tokenType))) {
             signWithKerberosToken(doc);
         } else {
            debug.error("SecureSOAPMessage.sign:: Invalid token type for" +
            " XML signing.");
         }

         if(debug.messageEnabled()) {
            debug.message("SecureSOAPMessage.sign:: After Signing : "+
            WSSUtils.print(soapMessage.getSOAPPart()));
         }
     }

     /**
      * Signs the SOAP Message with SAML Assertion.
      */
     private void signWithAssertion(Document doc) throws SecurityException {
         
         XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
         KeyProvider keyProvider = sigManager.getKeyProvider();
         Certificate cert = null;
         String uri =  securityMechanism.getURI(); 

         boolean symmetricKey =   SecurityContext.SYMMETRIC_KEY.equals(
                 securityContext.getKeyType()) ? true : false;
         
         if( (SecurityMechanism.WSS_NULL_SAML_HK_URI.equals(uri)) ||
             (SecurityMechanism.WSS_TLS_SAML_HK_URI.equals(uri)) ||
             (SecurityMechanism.WSS_CLIENT_TLS_SAML_HK_URI.equals(uri))) {
             if(!symmetricKey) {
                cert = WSSUtils.getCertificate(securityToken);
             }
             
         } else if( (SecurityMechanism.WSS_NULL_SAML2_HK_URI.equals(uri)) ||
             (SecurityMechanism.WSS_TLS_SAML2_HK_URI.equals(uri)) ||
             (SecurityMechanism.WSS_CLIENT_TLS_SAML2_HK_URI.equals(uri))) {
             if(!symmetricKey) {
                cert = SAML2TokenUtils.getCertificate(securityToken);
             }
             
         } else if( (SecurityMechanism.WSS_NULL_SAML_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_TLS_SAML_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_CLIENT_TLS_SAML_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_NULL_SAML2_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_TLS_SAML2_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_CLIENT_TLS_SAML2_SV_URI.equals(uri))) {             
             cert =  keyProvider.getX509Certificate(
                     securityContext.getSigningCertAlias());
             
         } else if (SecurityMechanism.STS_SECURITY_URI.equals(uri)) {
             if(SecurityToken.WSS_SAML_TOKEN.equals(
                 securityToken.getTokenType())) {
                 if(!symmetricKey) {
                    cert = WSSUtils.getCertificate(securityToken);
                 }
             } else if(SecurityToken.WSS_SAML2_TOKEN.equals(
                 securityToken.getTokenType())) {
                 if(!symmetricKey) {
                    cert = SAML2TokenUtils.getCertificate(securityToken);
                 }
             } 
             if (cert == null) {
                 cert =  keyProvider.getX509Certificate(
                         securityContext.getSigningCertAlias());
             }
             
         } else {
             debug.error("SecureSOAPMessage.signWithSAMLAssertion:: " +
              "Unknown security mechanism");
             throw new SecurityException(
                   bundle.getString("unknownSecurityMechanism"));
         }
 
         Element sigElement = null;
         try {
             String assertionID = null;
             if(securityToken instanceof AssertionToken) {
                AssertionToken assertionToken = (AssertionToken)securityToken;
                assertionID = assertionToken.getAssertion().getAssertionID();
             } else if (securityToken instanceof SAML2Token) {
                SAML2Token saml2Token = (SAML2Token)securityToken;
                assertionID = saml2Token.getAssertion().getID();
             }
             Key signingKey = securityContext.getSigningKey();
             if(signingKey == null) {
                if(cert != null) {
                   String signAlias = keyProvider.getCertificateAlias(cert);
                   signingKey = keyProvider.getPrivateKey(signAlias);
                }               
             }
             
             Key encryptionKey = securityContext.getEncryptionKey();
             Certificate encryptCert = null;
             if(encryptionKey == null) {
                String encryptAlias = securityContext.getEncryptionKeyAlias();
                encryptCert = keyProvider.getX509Certificate(encryptAlias);
             } else {
                encryptCert = keyProvider.getCertificate(
                        (PublicKey)encryptionKey); 
             }
             
             sigElement = sigManager.signWithSAMLToken(doc,
                   signingKey, symmetricKey, cert, encryptCert, 
                   assertionID, "", getSigningIds()); 
            

         } catch (XMLSignatureException se) {
             debug.error("SecureSOAPMessage.signWithAssertion:: " +
                "signing failed", se);
            String[] data = {se.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_SIGN,
                        data,
                        null);
             throw new SecurityException(
                   bundle.getString("unabletoSign"));
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.signWithAssertion:: " +
                "signing failed", ex);
            String[] data = {ex.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_SIGN,
                        data,
                        null);
             throw new SecurityException(
                   bundle.getString("unabletoSign"));
         }
         wsseHeader.appendChild(
                 soapMessage.getSOAPPart().importNode(sigElement, true));
         try {
             this.soapMessage.saveChanges();
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.signWithAssertion:: " +
                "SOAP message save failed : ", ex);
         }
     }


     /**
      * Signs the document with binary security token.
      */
     private void signWithBinaryToken(Document doc, String certAlias,
             String refType) throws SecurityException {

         Certificate cert = null;
         Element sigElement = null;
         XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
         KeyProvider keyProvider = sigManager.getKeyProvider();
         try {
             cert =  keyProvider.getX509Certificate(certAlias);
             sigElement = sigManager.signWithBinarySecurityToken(
                doc, cert, "", getSigningIds(), refType);
         } catch (XMLSignatureException se) {
            debug.error("SecureSOAPMessage.signWithBinaryToken:: Signature " +
            "Exception.", se);
            String[] data = {se.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_SIGN,
                        data,
                        null);
            throw new SecurityException(
                   bundle.getString("unabletoSign"));
         } catch (Exception ex) {
            debug.error("SecureSOAPMessage.signWithBinaryToken:: " +
                "signing failed", ex);
            String[] data = {ex.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_SIGN,
                        data,
                        null);
            throw new SecurityException(
                   bundle.getString("unabletoSign"));
         }
         wsseHeader.appendChild(
                 soapMessage.getSOAPPart().importNode(sigElement, true));
         try {
             this.soapMessage.saveChanges();
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.signWithBinaryToken:: " +
                "SOAP message save failed : ", ex);
         }

     }
     
     /**
      * Signs the document with kerberos security token.
      */
     private void signWithKerberosToken(Document doc) 
           throws SecurityException {
         
         Element sigElement = null;
         XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();         
         try {
             BinarySecurityToken bst = (BinarySecurityToken)securityToken;             
             sigElement =  sigManager.signWithKerberosToken(
                doc, bst.getSecretKey(), SAMLConstants.ALGO_ID_MAC_HMAC_SHA1, 
                getSigningIds());
         } catch (XMLSignatureException se) {
            debug.error("SecureSOAPMessage.signWithBinaryToken:: Signature " +
            "Exception.", se);
            String[] data = {se.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_SIGN,
                        data,
                        null);
            throw new SecurityException(
                   bundle.getString("unabletoSign"));
         } catch (Exception ex) {
            debug.error("SecureSOAPMessage.signWithBinaryToken:: " +
                "signing failed", ex);
            String[] data = {ex.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_SIGN,
                        data,
                        null);
            throw new SecurityException(
                   bundle.getString("unabletoSign"));
         }
         wsseHeader.appendChild(
                 soapMessage.getSOAPPart().importNode(sigElement, true));
         try {
             this.soapMessage.saveChanges();
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.signWithBinaryToken:: " +
                "SOAP message save failed : ", ex);
         }
     }

     /**
      * Signs the document with binary security token.
      */
     private void signWithUNToken(Document doc, String certAlias) 
           throws SecurityException {

         Certificate cert = null;
         Element sigElement = null;
         XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
         KeyProvider keyProvider = sigManager.getKeyProvider();
         try {
             cert =  keyProvider.getX509Certificate(certAlias);
             sigElement = sigManager.signWithUserNameToken(
                doc, cert, "", getSigningIds());
         } catch (XMLSignatureException se) {
            debug.error("SecureSOAPMessage.signWithUNToken:: Signature " +
            "Exception.", se);
            String[] data = {se.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_SIGN,
                        data,
                        null);
            throw new SecurityException(
                   bundle.getString("unabletoSign"));
         } catch (Exception ex) {
            debug.error("SecureSOAPMessage.signWithUNToken:: " +
                "signing failed", ex);
            String[] data = {ex.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_SIGN,
                        data,
                        null);
            throw new SecurityException(
                   bundle.getString("unabletoSign"));
         }
         wsseHeader.appendChild(
                 soapMessage.getSOAPPart().importNode(sigElement, true));
         try {
             this.soapMessage.saveChanges();
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.signWithUNToken:: " +
                "SOAP message save failed : ", ex);
         }
     }

     /**
      * Returns the list of signing ids.
      */
     private List getSigningIds() throws Exception {
        if(signingIds == null) {
           signingIds = new ArrayList();
        }
        SOAPBody body = soapMessage.getSOAPBody();
        String id  = body.getAttribute(WSSConstants.WSU_ID);
        if(signedElements.isEmpty() ||
                signedElements.contains(WSSConstants.BODY_LNAME)) {
           signingIds.add(id);
        }
        return signingIds;
     }

     /**
      * Returns the messageID from the <wsa:Addressing> header.
      * @return the messageID from the <wsa:Addressing> header.
      */
     public String getMessageID() {
         return messageID;
     }
     
     /**
      * Retruns the message timestamp.
      * @return the message timestamp.
      */
     public long getMessageTimestamp() {
         return msgTimestamp;
     }
     /**
      * Verifies the signature of the SOAP message.
      * @return true if the signature verification is successful.
      * @exception SecurityException if there is any failure in validation. 
      */
     public boolean verifySignature() throws SecurityException {

        try {
            Document doc = toDocument();
            Key verificationKey = null;
            XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
            String tokenType = null;
            if(securityToken != null) {
               tokenType = securityToken.getTokenType(); 
            } else {
               tokenType = SecurityMechanism.WSS_NULL_ANONYMOUS_URI; 
            }
            
            if(tokenType.equals(SecurityToken.WSS_SAML2_TOKEN) ||
                    tokenType.equals(SecurityToken.WSS_SAML_TOKEN)) {
                
               String issuer = null;
               if(tokenType.equals(SecurityToken.WSS_SAML2_TOKEN)) {
                  SAML2Token saml2Token = (SAML2Token)securityToken;
                  issuer = saml2Token.getAssertion().getIssuer().getValue();                  
               } else if (tokenType.equals(SecurityToken.WSS_SAML_TOKEN)) {
                  AssertionToken assertionToken = (AssertionToken)securityToken;
                  issuer = assertionToken.getAssertion().getIssuer();
               }
               String issuerAlias = WSSUtils.getCertAlias(issuer);
               if(issuerAlias == null) {
                  WSSUtils.debug.message("SecureSOAPMessage.verifySignature: "
                          + " issuer alias does not present in the trusted ca" +
                          " alias list");
                  return false; 
               }
               Element assertionE = securityToken.toDocumentElement();
               Document document = XMLUtils.newDocument();
               document.appendChild(document.importNode(assertionE, true));
               if(WSSUtils.debug.messageEnabled()) {
                  WSSUtils.debug.message("SecureSOAPMessage.verifySignature "+
                     " Assertion to be verified" + XMLUtils.print(assertionE));
               }
               if(!sigManager.verifyXMLSignature(document, issuerAlias)){
                  if(WSSUtils.debug.messageEnabled()) {
                     WSSUtils.debug.message("SecureSOAPMessage.verifySignature:"
                         + " Signature verification for the assertion failed"); 
                  }
                  return false; 
               } else {
                 if(WSSUtils.debug.messageEnabled()) {
                    WSSUtils.debug.message("SecureSOAPMessage.verifySignature:"
                       + "Signature verification successful for the assertion");
                 }
               }
               
            }
                        
            if(messageCertificate != null) {
               String alias = sigManager.getKeyProvider().
                           getCertificateAlias(messageCertificate);
               verificationKey = sigManager.getKeyProvider().getPublicKey(alias);
            } else {
                // check if this symmetric encrypted key
                if(tokenType.equals(SecurityToken.WSS_SAML2_TOKEN)) {
                   verificationKey = SAML2TokenUtils.getSecretKey(securityToken, 
                        securityContext.getDecryptionAlias());
                } else if (tokenType.equals(SecurityToken.WSS_SAML_TOKEN)) {
                   verificationKey = WSSUtils.getSecretKey(securityToken, 
                        securityContext.getDecryptionAlias());
                }
            }
                                
            return sigManager.verifyWSSSignature(doc, verificationKey, 
                        securityContext.getVerificationCertAlias(), 
                        securityContext.getDecryptionAlias());              
                        
        } catch (SAMLException se) {
            debug.error("SecureSOAPMessage.verify:: Signature validation " +
                   "failed", se);
            String[] data = {se.getLocalizedMessage()};
            LogUtil.error(Level.INFO,
                        LogUtil.SIGNATURE_VALIDATION_FAILED,
                        data,
                        null);
            throw new SecurityException(
                bundle.getString("signatureValidationFailed"));
        } catch (Exception ex) {
            debug.error("SecureSOAPMessage.verify:: Signature validation " +
                   "failed", ex);
            throw new SecurityException(
                bundle.getString("signatureValidationFailed"));
        }        
     }
     
     /**
      * Verifies the signature of the SOAP message that has kerberos key.
      * @param secretKey the secret key that is used for signature verification.
      * @return true if the signature verification is successful.
      * @exception SecurityException if there is any failure in validation. 
      */
     public boolean verifyKerberosTokenSignature(java.security.Key secretKey) 
             throws SecurityException {
        try {
            Document doc = toDocument();
            XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
            return sigManager.verifyWSSSignature(doc, secretKey);            
        } catch (SAMLException se) {
            debug.error("SecureSOAPMessage.verify:: Signature validation " +
                   "failed", se);
            throw new SecurityException(
                bundle.getString("signatureValidationFailed"));
        }         
     }

     /**
      * Converts the SOAP Message into an XML document.
      */
     private Document toDocument() throws SecurityException {
        try {
            
            /* make sure changes to the soapMessage are persisted before
             * using writeTo() ... possibly SAAJ bug as 'writeTo()' should always
             * save the changes first
             */
            soapMessage.saveChanges();              

           
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            
            soapMessage.writeTo(bop);
            
            if (debug.messageEnabled()) {
                debug.message("SecureSOAPMessage.toDocument:: message used: " + bop.toString());
            }
            
            ByteArrayInputStream bin =
                    new ByteArrayInputStream(bop.toByteArray());
            Document doc = XMLUtils.toDOMDocument(bin, WSSUtils.debug);
            if (debug.messageEnabled()) {
                debug.message("SecureSOAPMessage.toDocument: Converted SOAPMessage: " + XMLUtils.print(doc));
            }
            return doc;
        } catch (Exception ex) {
            debug.error("SecureSOAPMessage.toDocument: Could not" +
            " Convert the SOAP Message to XML document.", ex); 
            throw new SecurityException(ex.getMessage());
        }
     }

     /**
      * Returns the <code>X509Certificate</code> that is used to secure
      * the <code>SOAPMessage</code>.
      *
      * @return the X509 certificate. 
      */
     public X509Certificate getMessageCertificate() {
         return messageCertificate;
     }

    /**
     * Encrypts the <code>SOAPMessage</code> for the given security profile.
     *
     * @param certAlias the certificate alias
     * @param encryptBody boolean flag to encrypt Body
     * @param encryptHeader boolean flag to encrypt Security header
     *
     * @exception SecurityException if there is any failure in encryption.
     */
     public void encrypt(String certAlias,
                  String encryptionAlgorithm,
                  int encryptionKeyStrength,
                  boolean encryptBody, 
                  boolean encryptHeader) throws SecurityException {
     
         Document doc = toDocument();
         String tokenType = null;
         // securityToken=null if the secmech is Anonymous
         if (securityToken == null) {
             tokenType = SecurityToken.WSS_X509_TOKEN;
         } else {
             tokenType = securityToken.getTokenType();
         }
         Map elmMap = new HashMap();
         Document encryptedDoc = null;
         String searchType = null;
         String searchNS = WSSConstants.WSSE_NS;

         try {
             if (encryptBody) {
                 Element elmDoc = (Element) doc.getDocumentElement();
                 String nameSpaceURI = elmDoc.getNamespaceURI();
                 Element bodyElement = 
                     (Element) elmDoc.getElementsByTagNameNS(nameSpaceURI, 
                     WSSConstants.BODY_LNAME).item(0);
                 String bodyId  = bodyElement.getAttribute(WSSConstants.WSU_ID);
                 Node firstNodeInsideBody = bodyElement.getFirstChild();
                 elmMap.put((Element)firstNodeInsideBody, bodyId);
             }

             if (encryptHeader) {
                 String tokenId = null;
                 
                 if (SecurityToken.WSS_X509_TOKEN.equals(tokenType)) {
                     searchType = SAMLConstants.BINARYSECURITYTOKEN;
                 } else if (SecurityToken.WSS_USERNAME_TOKEN.equals(tokenType)) {
                     searchType = WSSConstants.TAG_USERNAME_TOKEN;
                 } else if (SecurityToken.WSS_SAML_TOKEN.equals(tokenType)) {
                     searchType = SAMLConstants.TAG_ASSERTION;
                     AssertionToken assertionToken = 
                         (AssertionToken)securityToken;
                     tokenId = assertionToken.getAssertion().getAssertionID();
                     searchNS = SAMLConstants.assertionSAMLNameSpaceURI;
                 } else if (SecurityToken.WSS_SAML2_TOKEN.equals(tokenType)) {
                     searchType = SAMLConstants.TAG_ASSERTION;
                     SAML2Token saml2Token = (SAML2Token)securityToken;
                     tokenId = saml2Token.getAssertion().getID();
                     searchNS = SAML2Constants.ASSERTION_NAMESPACE_URI;
                 }

                 Element secHeaderElement = (Element) doc.getDocumentElement().
                     getElementsByTagNameNS(WSSConstants.WSSE_NS,
                     WSSConstants.WSSE_SECURITY_LNAME).item(0);

                 if(debug.messageEnabled()) {
                     debug.message("SecureSOAPMessage.encrypt:: Security " + 
                         "Header : " + WSSUtils.print(secHeaderElement));
                 }
                 if (secHeaderElement != null) {
                     Element token = 
                         (Element)secHeaderElement.getElementsByTagNameNS(
                         searchNS,searchType).item(0);
                     
                     if ((token != null) && (tokenId == null)) {        
                         tokenId = token.getAttributeNS(WSSConstants.WSU_NS,
                             SAMLConstants.TAG_ID);     
                     }
                     elmMap.put(token, tokenId);
                 }
             }

             XMLEncryptionManager encManager = 
                                  WSSUtils.getXMLEncryptionManager();                 
             encryptedDoc = encManager.encryptAndReplaceWSSElements(
                 doc, 
                 elmMap,
                 encryptionAlgorithm,
                 encryptionKeyStrength,
                 certAlias,
                 0,
                 tokenType,
                 server_proto + "://" + server_host + ":" + server_port);

         } catch (EncryptionException ee) {
             debug.error("SecureSOAPMessage.encrypt:: Encryption " +
                 "Exception : ", ee);
             String[] data = {ee.getLocalizedMessage()};
             LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_ENCRYPT,
                        data,
                        null);
             throw new SecurityException(
                 bundle.getString("unabletoEncrypt"));        
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.encrypt:: " +
                 "encryption failed : ", ex);
             String[] data = {ex.getLocalizedMessage()};
             LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_ENCRYPT,
                        data,
                        null);
             throw new SecurityException(
                 bundle.getString("unabletoEncrypt"));
         }
         
         try {
             Element encryptedKeyElem = null;
             
             NodeList nl = encryptedDoc.getElementsByTagNameNS(
                 EncryptionConstants.ENC_XML_NS, "EncryptedKey");
             for(int i=0; i < nl.getLength(); i++ ) {                 
                 Element elem = (Element)nl.item(i);
                 if(elem.getParentNode().getParentNode().getLocalName()
                         .equals("Signature")) {
                    continue; 
                 }
                 encryptedKeyElem = elem;
                 break;
             }
             if(debug.messageEnabled()) {
                 debug.message("SecureSOAPMessage.encrypt:EncryptedKey DOC : " 
                     + WSSUtils.print(encryptedKeyElem)); 
             }
             // Append EncryptedKey element in the Security header
             Node encKeyNode = 
                 (soapMessage.getSOAPPart()).importNode(encryptedKeyElem, 
                                                        true);
             wsseHeader.appendChild(encKeyNode);
             soapMessage.saveChanges();
             // Append some how does not work for webshpere SOAP runtime
             // So, if it's not added for whatever reason, get the enveloped
             // and add it.
             NodeList nodeList = soapMessage.getSOAPPart().
                      getElementsByTagNameNS(EncryptionConstants.ENC_XML_NS,
                      "EncryptedKey");
             if(nodeList == null || nodeList.getLength() == 0) {
                soapMessage.getSOAPPart().getEnvelope().getHeader().
                        appendChild(encKeyNode);
             }

             if(debug.messageEnabled()) {
                 debug.message("SecureSOAPMessage.encrypt:: wsseHeader: " 
                     + "after Encrypt : " 
                     + WSSUtils.print(soapMessage.getSOAPPart()));
             }
             // EncryptedData elements
             NodeList nodes = encryptedDoc.getElementsByTagNameNS(
                 EncryptionConstants.ENC_XML_NS, "EncryptedData");
             int length = nodes.getLength();

             for (int i=0; i < length; i++) {
                 Element encryptedDataElem = (Element)nodes.item(i);
             
                 if(debug.messageEnabled()) {
                     debug.message("SecureSOAPMessage.encrypt:" + 
                                   "EncryptedData DOC (" + i + ") : " 
                                   + WSSUtils.print(encryptedDataElem));
                 }
                 // Replace first child of Body element or Security
                 // header element with the EncryptedData element 
                 Node encDataNode = (soapMessage.getSOAPPart()).
                     importNode(encryptedDataElem,true);
                 
                 if( (WSSConstants.BODY_LNAME.equals(
                     ((Node)encryptedDataElem).getParentNode().getLocalName()))) {
                     Node firstNodeInsideBody = 
                         soapMessage.getSOAPPart().getEnvelope().getBody().
                             getFirstChild();
                     soapMessage.getSOAPPart().getEnvelope().getBody().
                         replaceChild(encDataNode, firstNodeInsideBody);
                 } else if (encryptHeader) {
                     Element token = 
                         (Element)wsseHeader.getElementsByTagNameNS(
                         searchNS,searchType).item(0);

                     if(debug.messageEnabled()) {
                         debug.message("SecureSOAPMessage.encrypt:: wsseHeader: " 
                             + "token element : " + WSSUtils.print(token));
                     }
                     
                     if (token != null) {
                         wsseHeader.removeChild(encKeyNode);
                         wsseHeader.insertBefore(encKeyNode,(Node)token);

                         wsseHeader.replaceChild(encDataNode, (Node)token);
                     }                      
                 }
             }

                                       
             this.soapMessage.saveChanges();
             

             if(debug.messageEnabled()) {
                 debug.message("SecureSOAPMessage.encrypt:*** SOAP PART ***"); 
                 debug.message(WSSUtils.print(soapMessage.getSOAPPart()));
             }
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.encrypt:: " +
                 "encryption failed : ", ex);
             throw new SecurityException(
                 bundle.getString("unabletoGetFinalSoapMessage"));
         }
         
     }
     
    /**
     * Decrypts the <code>SOAPMessage</code> for the given security profile.
     * @param keyAlias private key alias that is used to decrypt.
     * @param decryptBody boolean flag to decrypt Body
     * @param decryptHeader boolean flag to decrypt Security header     
     * @exception SecurityException if there is any failure in decryption.
     */
     public void decrypt(String keyAlias, 
             boolean decryptBody,
             boolean decryptHeader)             
         throws SecurityException {

         Document decryptedDoc = null; 
         try {
             Document doc = toDocument();

             NodeList nodes = doc.getElementsByTagNameNS(
                 EncryptionConstants.ENC_XML_NS, "EncryptedData");
             if((nodes == null) || (nodes.getLength() == 0)) {
                 debug.error("SecureSOAPMessage.decrypt:: Request " +
                     "is not encrypted.");
                 throw new SecurityException(
                     bundle.getString("decryptEncryptionFailed"));
             }

             XMLSignatureManager sigManager = 
                 XMLSignatureManager.getInstance();
             XMLEncryptionManager encManager = 
                     WSSUtils.getXMLEncryptionManager();                 
             String certAlias = null;
             if(messageCertificate != null) {
                 certAlias = sigManager.getKeyProvider().
                             getCertificateAlias(messageCertificate);
             } else {
                 certAlias = keyAlias;
             }
             decryptedDoc = encManager.decryptAndReplace(doc,certAlias);
         } catch (EncryptionException ee) {
             debug.error("SecureSOAPMessage.decrypt:: Decrypt " +
                "encryption failed : ", ee);
             String[] data = {ee.getLocalizedMessage()};
             LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_DECRYPT,
                        data,
                        null);
             throw new SecurityException(
                    bundle.getString("decryptEncryptionFailed"));
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.decrypt:: " +
                 "exception : ", ex);
             String[] data = {ex.getLocalizedMessage()};
             LogUtil.error(Level.INFO,
                        LogUtil.UNABLE_TO_DECRYPT,
                        data,
                        null);
             throw new SecurityException(
                 bundle.getString("unabletoDecrypt"));
         }

         try {
             if (decryptBody) {
                 // Decrypted Body element replacement
                 Element elmDoc = (Element) decryptedDoc.getDocumentElement();
                 String nameSpaceURI = elmDoc.getNamespaceURI();
                 Element decryptedBodyElem = 
                     (Element)decryptedDoc.getElementsByTagNameNS(
                     nameSpaceURI, WSSConstants.BODY_LNAME).item(0);
             
                 Element bodyElement = 
                     (Element)soapMessage.getSOAPPart().getEnvelope().getBody();

                 if(debug.messageEnabled()) {
                     debug.message("SecureSOAPMessage.decrypt::decrypted " + 
                         "Body element : " + WSSUtils.print(decryptedBodyElem));
                     debug.message("SecureSOAPMessage.decrypt::SOAP " + 
                         "Body element : " + WSSUtils.print(bodyElement));
                 }
         
                 // Replace first child of Body element with the 
                 // first child of Decrypted Body element
                 Node decDataNode = 
                     (soapMessage.getSOAPPart()).importNode(decryptedBodyElem, 
                                                        true);                 
                 NodeList nl = soapMessage.getSOAPPart().getEnvelope().
                         getBody().getChildNodes();
                 for (int i=0; i < nl.getLength(); i++) {
                     Node node = nl.item(i);
                     if(node.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                     }
                     soapMessage.getSOAPPart().getEnvelope().getBody().
                             removeChild(node);
                     soapMessage.getSOAPPart().getEnvelope().getBody().
                             appendChild(decDataNode.getFirstChild());
                     break;
                 }
             }

             // Decrypted Security token element replacement
             if (decryptHeader) {
                 Element decryptedSecHeaderElement = (Element) decryptedDoc.
                     getElementsByTagNameNS(WSSConstants.WSSE_NS,
                         WSSConstants.WSSE_SECURITY_LNAME).item(0);
                 Node decSecHeaderDataNode = 
                     (soapMessage.getSOAPPart()).importNode(
                         decryptedSecHeaderElement, true);

                 Node tokenNode = getTokenNode(decSecHeaderDataNode);
                 Element token = (Element)wsseHeader.getElementsByTagNameNS(
                     EncryptionConstants.ENC_XML_NS, "EncryptedData").item(0);

                 if(debug.messageEnabled()) {
                     debug.message("SecureSOAPMessage.decrypt: decrypted " + 
                         "Security Header doc : " + 
                         WSSUtils.print(decryptedSecHeaderElement));
                     debug.message("SecureSOAPMessage.decrypt: " + 
                         "SOAP HEADER DOC : " + WSSUtils.print(wsseHeader));
                     debug.message("SecureSOAPMessage.decrypt: tokenNode " + 
                         "from decrypted Security header doc: " + 
                         WSSUtils.print(tokenNode));
                     debug.message("SecureSOAPMessage.decrypt: token " + 
                         "from current SOAP wsseHeader : " + 
                         WSSUtils.print(token));
                 }

                 if (token != null) {
                     Element encKey = (Element)wsseHeader.getElementsByTagNameNS(
                         EncryptionConstants.ENC_XML_NS, "EncryptedKey").item(0);
                     wsseHeader.removeChild((Node) encKey);
                     wsseHeader.appendChild((Node) encKey);

                     wsseHeader.replaceChild(tokenNode, (Node) token);
                 }

             }
             
             this.soapMessage.saveChanges();             

             if(debug.messageEnabled()) {
                 debug.message("SecureSOAPMessage.decrypt:*** SOAP PART ***"); 
                 debug.message(WSSUtils.print(soapMessage.getSOAPPart()));
             }
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.decrypt:: " +
                 "decryption failed : ", ex);
             throw new SecurityException(
                 bundle.getString("unabletoGetFinalSoapMessage"));
         }
         
     }


     /**
      * Returns the token Node for corresponding Security token.
      */
     private Node getTokenNode(Node secHeaderNode) throws Exception {
         Node tokenNode = null;
         String searchType = null;
         NodeList securityHeaders = secHeaderNode.getChildNodes();
         for(int i=0; i < securityHeaders.getLength(); i++) {
             Node currentNode =  securityHeaders.item(i);
             String localName =  currentNode.getLocalName();
             String nameSpace = currentNode.getNamespaceURI();

             if( (SAMLConstants.TAG_ASSERTION.equals(localName)) &&
                 (SAMLConstants.assertionSAMLNameSpaceURI.equals(nameSpace))) {
                 searchType = SAMLConstants.TAG_ASSERTION;
             } else if( (SAMLConstants.TAG_ASSERTION.equals(localName)) &&
                 (SAML2Constants.ASSERTION_NAMESPACE_URI.equals(nameSpace))) {
                 searchType = SAMLConstants.TAG_ASSERTION;
             } else if( (WSSConstants.TAG_BINARY_SECURITY_TOKEN.
                         equals(localName)) && 
                        (WSSConstants.WSSE_NS.equals(nameSpace)) ) {
                 searchType = SAMLConstants.BINARYSECURITYTOKEN;
             } else if( (WSSConstants.TAG_USERNAME_TOKEN.equals(localName)) &&
                        (WSSConstants.WSSE_NS.equals(nameSpace)) ) {
                 searchType = WSSConstants.TAG_USERNAME_TOKEN;
             }
             if (searchType != null) {
                 tokenNode = currentNode;
                 break;
             }
         }
         return tokenNode;
     }
     
     private boolean validateTimestamp(Element tsElement) {
         
         String created = null;
         String expires = null;
         NodeList nl = tsElement.getChildNodes();
         for (int i=0; i < nl.getLength(); i++) {
             Node child = nl.item(i);
             if(child.getNodeType() != Node.ELEMENT_NODE) {
                continue; 
             }
             
             String childName = child.getLocalName();
             if(WSSConstants.CREATED.equals(childName)) {                 
                created = XMLUtils.getElementValue((Element)child);
             } else if(WSSConstants.EXPIRES.equals(childName)) {
                expires = XMLUtils.getElementValue((Element)child); 
             }
         }
         try {
             msgTimestamp = DateUtils.stringToDate(created).getTime();
             long createdTS = msgTimestamp - WSSUtils.getTimeSkew();
             long expiresTS = DateUtils.stringToDate(expires).getTime();
             long now = new Date().getTime();
             if (created == null ) {
                 if (expires == null) {
                     return false;
                 } else {
                    if (now < expiresTS) {
                        return true;
                    }
                 }
             } else if (expires == null ) {
                 if (now >= createdTS) {
                     return true;
                 }
             } else if ((now >= createdTS) && 
                  (now < expiresTS)) {       
                 return true; 
             }
             return false;
         } catch (java.text.ParseException pe) {
             WSSUtils.debug.error("SecureSOAPMessage.validateTimestamp: " +
                    "parsing exception", pe);
             return false; 
         }
         
     }

     public void setSenderIdentity(String dnsName) {
         try {
             SOAPPart soapPart = soapMessage.getSOAPPart();
             SOAPHeader header = soapPart.getEnvelope().getHeader();
             if(header == null) {
                return;
             }
             NodeList nl = header.getElementsByTagNameNS(WSSConstants.wsaNS,
                     "Action");
             if(nl == null || nl.getLength() == 0) {
                return;
             }
             NodeList childNodes =
                     header.getElementsByTagNameNS(WSSConstants.wsaNS, "From");
             Element fromElement = null;
             if(childNodes == null || childNodes.getLength() == 0) {
                fromElement = soapPart.createElementNS(
                        WSSConstants.wsaNS, "From");
                fromElement.setAttributeNS(WSSConstants.NS_XML,
                       WSSConstants.TAG_XML_WSU,
                       WSSConstants.WSU_NS);
                String id = SAMLUtils.generateID();
                fromElement.setAttribute(WSSConstants.WSU_ID, id);
                if(signedElements.contains(WSSConstants.FROM)) {
                   signingIds.add(id);
                }
             } else {
                fromElement = (Element)childNodes.item(0);
             }

             Element identityE = soapPart.createElementNS(
                     WSSConstants.WSID_NS,
                     WSSConstants.TAG_IDENTITY);

             identityE.setAttributeNS(
                          WSSConstants.NS_XML,
                          WSSConstants.TAG_XML_WSID,
                          WSSConstants.WSID_NS);

             Element dnsClaim = soapPart.createElementNS(
                      WSSConstants.WSID_NS,
                      WSSConstants.TAG_DNSCLAIM);

             org.w3c.dom.Text textNode = soapPart.createTextNode(dnsName);
             dnsClaim.appendChild(textNode);
             identityE.appendChild(dnsClaim);
             fromElement.appendChild(identityE);
             Element replyTo = 
                         (Element)header.getElementsByTagNameNS(
                         WSSConstants.wsaNS, "ReplyTo").item(0);
             header.insertBefore(fromElement, replyTo);
         } catch (SOAPException se) {
             WSSUtils.debug.error("SecureSOAPMessage.setSenderIdentity: " +
                     "SOAP Exception", se);
         }
     }
     
     public String getClientDnsClaim() {
         return clientDnsClaim;
     }
     
     public void setSignedElements(List elements) {
         this.signedElements = elements;
     }

}
