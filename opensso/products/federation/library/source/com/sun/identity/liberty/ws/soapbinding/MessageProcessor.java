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
 * $Id: MessageProcessor.java,v 1.3 2008/06/25 05:47:22 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.soapbinding;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPException;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.liberty.ws.security.SecurityUtils;
import com.sun.identity.liberty.ws.security.SecurityTokenManager;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.disco.DiscoveryClient;
import com.sun.identity.liberty.ws.disco.QueryResponse;
import com.sun.identity.liberty.ws.disco.Description;
import com.sun.identity.liberty.ws.disco.ServiceInstance;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The class <code>MessageProcessor</code> is used to process the 
 * <code>SOAPMessage</code> for the JSR 196 providers. This API will
 * be used by both the client and server providers for securing
 * and validating the request/responses between the web services
 * applications.
 */
public class MessageProcessor {


    private SOAPProviderConfig _config = null;
    private String correlationId = null;

    private MessageProcessor() {}

    /**
     * Constructor
     *
     * @param config SOAPConfiguration that will be used to validate
     *               the SOAPMessage.
     */
    public MessageProcessor(SOAPProviderConfig config) {
        this._config = config;
    }
    
    /**
     * This method is used to validate the SOAP Message Request by the
     * processing rules of Liberty SOAPBinding specifications.
     *
     * @param soapMessage SOAPMessage that needs to be validated.
     * @param subject Subject that may be used to populate the authenticated
     *        entity/user principal and any other credential information.
     * @param sharedData that may be used to store any data needed between
     *        the request and response.
     * @param httpRequest HttpServletRequest associated with this SOAP
     *        Message request.
     * @return Object Credential object after successful validation.
     * @throws SOAPBindingException for any error occured during validation.
     */
    public Object validateRequest(SOAPMessage soapMessage,Subject subject,
            Map sharedData,HttpServletRequest httpRequest)
            throws SOAPBindingException {
        Utils.debug.message("SOAPProvider.validateRequest : Init");
        Message req = null;
        try {
            req = new Message(soapMessage);
            sharedData.put(SOAPBindingConstants.LIBERTY_REQUEST, req);
            if(req.getSecurityProfileType() != Message.ANONYMOUS &&
                    !SecurityUtils.verifyMessage(req)) {
                Utils.debug.error("MessageProcessor.validateRequest: Signature"+
                        "Verification failed.");
                throw new SOAPBindingException(
                        Utils.bundle.getString("cannotVerifySignature"));
            }
            
            Utils.enforceProcessingRules(req, null, true);
            
            if(_config != null) {
                String authMech = req.getAuthenticationMechanism();
                if(authMech == null ||
                        !_config.getSupportedAuthenticationMechanisms()
                        .contains(authMech)) {
                    
                    throw new SOAPBindingException(
                            Utils.bundle.getString("unsupportedAuthMech"));
                }
            } else {
                throw new SOAPBindingException(
                        Utils.bundle.getString("nullConfiguration"));
            }
            
            return _config.getAuthenticator().authenticate(
                    req, subject, sharedData, httpRequest);
        } catch (SOAPBindingException sbe) {
            Utils.debug.error("MessageProcessor.validateRequest: Request" +
                    "Validation has failed.", sbe);
            throw sbe;
            
        } catch (SOAPFaultException sfe) {
            Utils.debug.error("MessageProcessor.validateRequest: SOAPFault" +
                    "Exception.", sfe);
            throw new SOAPBindingException(
                    Utils.bundle.getString("soapFaultException"));
        }
    }

    /**
     * Secures the SOAP Message response by adding necessary headers to the
     * given SOAP Message and also signs the message if it is required.
     * @param soapMessage SOAP Message that needs to be secured.
     * @param sharedData Any shared data that may be needed between the request
     *                   and response.
     * @return SOAPMessage Secured SOAP Message by adding liberty headers
     *         and also signs the message if configured.
     * @throws SOAPBindingException for any failure.
     */
    public SOAPMessage secureResponse (SOAPMessage soapMessage, Map sharedData)
                                       throws SOAPBindingException {
         
         Utils.debug.message("MessageProcessor.secureResponse : Init");

         try {
             Message req = (Message)sharedData.get(
                 SOAPBindingConstants.LIBERTY_REQUEST);
             addCorrelationHeader(soapMessage, req);

             if(_config.isResponseSignEnabled()) {
                soapMessage = signMessage(soapMessage, null, null);
             }

             if(Utils.debug.messageEnabled()) {
                Utils.debug.message("MessageProcessor.secureResponse: " +
                    XMLUtils.print(soapMessage.getSOAPPart().getEnvelope()));
              }

              return soapMessage;
         } catch (Exception ex) {
              Utils.debug.error("MessageProcessor.secureResponse: " +
                  "Failed in securing the response", ex);             
              throw new SOAPBindingException(
                 Utils.bundle.getString("secureResponseFailed"));
         }
    }

    /**
     * Secures the request by getting the credential from the discovery
     * service.
     *
     * @param offering Resource Offering of the discovery service.
     * @param credentials List of credentials that are required to access
     *        the discovery service.
     * @param serviceType Service Type that the discovery service should
     *        need to look for.
     * @param soapMessage SOAPMessage that needs to be secured.
     * @param sharedData Any shared data that may be used between the request
     *        and the response.
     * @return SOAPMessage Secured SOAP Message.
     * @throws SOAPBindingException for any failure.
     */
    public SOAPMessage secureRequest(ResourceOffering offering,List credentials,
            String serviceType,SOAPMessage
            soapMessage,Map sharedData)
            throws SOAPBindingException {
        
        Utils.debug.message("MessageProcessor.secureRequest:Init");
        try {
            SOAPHeader header = addCorrelationHeader(soapMessage, null);
            QueryResponse discoResponse =
                    getWebserviceOffering(offering, credentials, serviceType);
            
            if(Utils.debug.messageEnabled()) {
                Utils.debug.message("MessageProcessor.secureRequest: " +
                        "Discovery Response: " + discoResponse.toString());
            }
            
            ResourceOffering serviceOffering =
                    (ResourceOffering)discoResponse.getResourceOffering().get(0);
            
            List creds = discoResponse.getCredentials();
            
            String securityProfile = processResourceOffering(serviceOffering);
            
            SecurityAssertion securityAssertion = null;
            // If the security profile is of SAML or Bearer insert a
            // security token for this profile.
            if (securityProfile.equals(Message.NULL_SAML) ||
                securityProfile.equals(Message.TLS_SAML) ||
                securityProfile.equals(Message.CLIENT_TLS_SAML) ||
                securityProfile.equals(Message.NULL_BEARER) ||
                securityProfile.equals(Message.TLS_BEARER) ||
                securityProfile.equals(Message.CLIENT_TLS_BEARER) ||
                securityProfile.equals(Message.NULL_SAML_WSF11) ||
                securityProfile.equals(Message.TLS_SAML_WSF11) ||
                securityProfile.equals(Message.CLIENT_TLS_SAML_WSF11) ||
                securityProfile.equals(Message.NULL_BEARER_WSF11) ||
                securityProfile.equals(Message.TLS_BEARER_WSF11) ||
                securityProfile.equals(Message.CLIENT_TLS_BEARER_WSF11)) {
                
                if(creds != null && creds.size() != 0) {
                    securityAssertion = (SecurityAssertion)creds.get(0);
                    securityAssertion.addToParent(header);
                }
            }
            
            if (securityProfile.equals(Message.NULL_SAML) ||
                securityProfile.equals(Message.TLS_SAML) ||
                securityProfile.equals(Message.CLIENT_TLS_SAML) ||
                securityProfile.equals(Message.NULL_X509) ||
                securityProfile.equals(Message.TLS_X509) ||
                securityProfile.equals(Message.CLIENT_TLS_X509) ||
                securityProfile.equals(Message.NULL_SAML_WSF11) ||
                securityProfile.equals(Message.TLS_SAML_WSF11) ||
                securityProfile.equals(Message.CLIENT_TLS_SAML_WSF11) ||
                securityProfile.equals(Message.NULL_X509_WSF11) ||
                securityProfile.equals(Message.TLS_X509_WSF11) ||
                securityProfile.equals(Message.CLIENT_TLS_X509_WSF11)) {
                
                soapMessage = signMessage(soapMessage, securityProfile,
                    securityAssertion);
            }
            
            if (Utils.debug.messageEnabled()) {
                Utils.debug.message("MessageProcessor.secureRequest: " +
                    XMLUtils.print(soapMessage.getSOAPPart().getEnvelope()));
            }
            
            return soapMessage;
            
        } catch (Exception ex) {
            Utils.debug.error("MessageProcessor.secureRequest: Failure in " +
                    "Securing the request.", ex);
            throw new SOAPBindingException(
                    Utils.bundle.getString("secureRequestFailed"));
        }
        
    }
    
    /**
     * Validates the SOAP Response from the service and verifies the signature
     * if needed.
     *
     * @param soapMessage SOAPMessage that needs to be validated.
     * @param sharedData Any shared data that may be required between the
     *        request and the response.
     * @return SOAPMessage Validated SOAP Response.
     * @throws SOAPBindingException for any failure.
     */
    public SOAPMessage validateResponse(SOAPMessage soapMessage,Map sharedData)
                       throws SOAPBindingException {
        try {
            Message msg = new Message(soapMessage);
            if(_config.isResponseSignEnabled() &&
                    !SecurityUtils.verifyMessage(msg)) {
                throw new SOAPBindingException(
                        Utils.bundle.getString("cannotVerifySignature"));
                
            }
            Utils.enforceProcessingRules(msg, null, true);
            return soapMessage;
        } catch (Exception ex) {
            Utils.debug.error("MessageProcessor.validateResponse: " +
                    " Response validation failed.", ex);
            throw new SOAPBindingException(
                    Utils.bundle.getString("validateResponseFailed"));
        }
    }

    /**
     * Signs the message.
     * @param soapMessage SOAPMessage that needs to be signed.
     * @param profile Security profile that needs to be used for signing.
     * @param assertion Security Assertion
     * @return SOAPMessage signed SOAPMessage.
     */
    private SOAPMessage signMessage(
         SOAPMessage soapMessage, 
         String profile,
         SecurityAssertion assertion
    )
    throws SOAPBindingException {
        try {
            SOAPHeader soapHeader = 
                    soapMessage.getSOAPPart().getEnvelope().getHeader();
            if(soapHeader == null) {
               soapMessage.getSOAPPart().getEnvelope().addHeader();
            }
            SOAPBody soapBody = 
                   soapMessage.getSOAPPart().getEnvelope().getBody();
            if(soapBody == null) {
               throw new SOAPBindingException(
                     Utils.bundle.getString("nullSOAPBody"));
            }

            String bodyId = SAMLUtils.generateID();
            soapBody.setAttributeNS(WSSEConstants.NS_WSU_WSF11,
                     WSSEConstants.WSU_ID, bodyId);
            List ids = new ArrayList();
            ids.add(bodyId);
            if(correlationId != null) {
               ids.add(correlationId);
            }

            Certificate cert = null;
            Element sigElem = null;
            ByteArrayInputStream bin = null;
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            Document doc = null;
            if(profile == null || 
                      profile.equals(Message.NULL_X509) ||
                      profile.equals(Message.TLS_X509) ||
                      profile.equals(Message.CLIENT_TLS_X509) ||
                      profile.equals(Message.NULL_X509_WSF11) ||
                      profile.equals(Message.TLS_X509_WSF11) ||
                      profile.equals(Message.CLIENT_TLS_X509_WSF11)) {

               BinarySecurityToken binaryToken = addBinaryToken(soapMessage);
               cert = SecurityUtils.getCertificate(binaryToken);
               soapMessage.writeTo(bop);
               bin = new ByteArrayInputStream(bop.toByteArray());
               doc = XMLUtils.toDOMDocument(bin, Utils.debug);
               sigElem = SecurityUtils.getSignatureManager().
                         signWithWSSX509TokenProfile(doc, cert, "", ids, 
                         SOAPBindingConstants.WSF_11_VERSION); 

            } else if(profile.equals(Message.NULL_SAML) ||
                      profile.equals(Message.TLS_SAML) ||
                      profile.equals(Message.CLIENT_TLS_SAML) ||
                      profile.equals(Message.NULL_SAML_WSF11) ||
                      profile.equals(Message.TLS_SAML_WSF11) ||
                      profile.equals(Message.CLIENT_TLS_SAML_WSF11)) {

               cert = SecurityUtils.getCertificate(assertion);
               soapMessage.writeTo(bop);
                      new ByteArrayInputStream(bop.toByteArray());
               bin = new ByteArrayInputStream(bop.toByteArray());
               doc = XMLUtils.toDOMDocument(bin, Utils.debug);
               sigElem = SecurityUtils.getSignatureManager().
                         signWithWSSSAMLTokenProfile(doc, cert, 
                         assertion.getAssertionID(), "", ids, 
                         SOAPBindingConstants.WSF_11_VERSION); 
            }

            if(sigElem == null) {
               Utils.debug.error("MessageProcessor.signMessage: " +
                "SigElement is null");
               throw new SOAPBindingException(
                 Utils.bundle.getString("cannotSignMessage"));
            }

            Element securityHeader = getSecurityHeader(soapMessage);
            securityHeader.appendChild(securityHeader.getOwnerDocument().
                           importNode(sigElem, true));

            return Utils.DocumentToSOAPMessage(sigElem.getOwnerDocument());

        } catch (Exception ex) {
            Utils.debug.error("MessageProcessor.signMessage: " +
               "Signing failed.", ex);
            throw new SOAPBindingException(
                Utils.bundle.getString("cannotSignMessage"));
        }
    }

    /**
     * Adds the correlation header.
     * @param msg SOAP Message that needs to be added with Correlation header. 
     * @param req Message Request, if present adds the correlation header 
     *             reference.
     * @return SOAPHeader SOAP Header with Correlation header.
     * @throws SOAPBindingException if there is an error.
     */
    private SOAPHeader addCorrelationHeader(SOAPMessage msg, Message req)
                       throws SOAPBindingException {
        try {
            SOAPHeader header =
                    msg.getSOAPPart().getEnvelope().getHeader();
            
            if(header == null) {
                header = msg.getSOAPPart().getEnvelope().addHeader();
            }
            
            CorrelationHeader cHeader = new CorrelationHeader();
            correlationId = cHeader.getId();
            if(req != null) {
                cHeader.setRefToMessageID(
                        req.getCorrelationHeader().getMessageID());
            }
            cHeader.addToParent(header);
            return header;
        } catch (Exception ex) {
            Utils.debug.error("MessageProcessor.addCorrealtionHeader: " +
                    "Could not add correlation header", ex);
            throw new SOAPBindingException(
                    Utils.bundle.getString("cannotAddCorrelationHeader"));
        }
    }

    /**
     * Adds binary token to the security header.
     */
    private BinarySecurityToken addBinaryToken(
         SOAPMessage msg
    ) throws SOAPBindingException {
        try { 
            SOAPHeader header =
                   msg.getSOAPPart().getEnvelope().getHeader();
            if(header == null) {
               header = msg.getSOAPPart().getEnvelope().addHeader();
            }
            SecurityTokenManager manager = new SecurityTokenManager(null);
            BinarySecurityToken binaryToken = 
                     manager.getX509CertificateToken();
            binaryToken.setWSFVersion(SOAPBindingConstants.WSF_11_VERSION);
            binaryToken.addToParent(header);
            return binaryToken;
        } catch (Exception ex) {
            Utils.debug.error("MessageProcessor.addBinaryToken: " +
             "Could not add binary security token", ex);
            throw new SOAPBindingException(
                Utils.bundle.getString("cannotAddCorrelationHeader"));
        }
    }

    /**
     * Returns web service offering by making a discovery query.
     */
    private QueryResponse getWebserviceOffering(ResourceOffering offering,
                                           List credentials,String serviceType)
                                           throws SOAPBindingException {

         List list = new ArrayList();
         list.add(serviceType);
          
         try {
             DiscoveryClient client = new DiscoveryClient(
                     offering, null, null, credentials);
             return client.getResourceOffering(list);
         } catch (Exception ex) {
             Utils.debug.error("MessageProcessor.getWebserviceOffering : " +
             "Failed in discovery query.", ex);
            throw new SOAPBindingException(
               Utils.bundle.getString("discoveryQueryFailed"));
         }
    }

    /**
     * Returns security profile after parsing the resource offering.
     */
    private String processResourceOffering(ResourceOffering offering)
                   throws SOAPBindingException {
        
        try {
            ServiceInstance si = offering.getServiceInstance();
            List descriptions = si.getDescription();
            
            if(descriptions == null || descriptions.isEmpty()) {
                Utils.debug.error("MessageProcessor:processResourceOffering: " +
                        "descriptions are null.");
                throw new SOAPBindingException(
                        Utils.bundle.getString("noDescriptions"));
            }
            
            Iterator iter = descriptions.iterator();
            while(iter.hasNext()) {
                Description desc = (Description)iter.next();
                List secMechIDs = desc.getSecurityMechID();
                if (secMechIDs == null || secMechIDs.isEmpty()) {
                    Utils.debug.error(
                            "MessageProcessor.processResourceOffering:"
                            + " security Mechs are empty");
                    throw new SOAPBindingException(
                            Utils.bundle.getString("noSecurityMechs"));
                }
                return (String)secMechIDs.iterator().next();
            }
            
            //It should not come over here.
            throw new SOAPBindingException(
                    Utils.bundle.getString("noSecurityMechs"));
            
        } catch (Exception ex) {
            Utils.debug.error("MessageProcessor.processResourceOffering: " +
                    "Failed in processing the resource offering.", ex);
            throw new SOAPBindingException(
                    Utils.bundle.getString("processOfferingFailed"));
        }
    }

    /**
     * Returns the security header element.
     */
    private Element getSecurityHeader(SOAPMessage soapMessage) 
                 throws SOAPBindingException {
        try {
            SOAPHeader header = 
                 soapMessage.getSOAPPart().getEnvelope().getHeader();
            NodeList headerChildNodes = header.getChildNodes();
            if((headerChildNodes == null) ||
                        (headerChildNodes.getLength() == 0)) {
               throw new SOAPBindingException(
                     Utils.bundle.getString("noSecurityHeader"));
            }
            for(int i=0; i < headerChildNodes.getLength(); i++) {

                 Node currentNode = headerChildNodes.item(i);
                 if(currentNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                 }
                 if((WSSEConstants.TAG_SECURITYT.equals(
                       currentNode.getLocalName())) &&
                    (WSSEConstants.NS_WSSE_WSF11.equals(
                       currentNode.getNamespaceURI()))) {
                    return (Element)currentNode;
                 }
             }
             return null;
        } catch (SOAPException se) {
             Utils.debug.error("MessageProcess.getSecurityHeader:: " +
             "SOAPException", se);
             throw new SOAPBindingException(
             Utils.bundle.getString("noSecurityHeader"));
        }
    }
}
