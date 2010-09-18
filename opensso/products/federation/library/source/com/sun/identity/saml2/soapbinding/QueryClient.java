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
 * $Id: QueryClient.java,v 1.9 2009/10/29 00:19:21 madan_ranganath Exp $
 *
 */

package com.sun.identity.saml2.soapbinding;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLAuthzDecisionQueryConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLPDPConfigElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzServiceElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.impl.ResponseImpl;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionQuery;
import javax.xml.soap.SOAPException;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.soap.SOAPConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>QueryClient</code> class provides Query Requester clients with
 * a method to send requests using SOAP connection to SOAP endpoint.
 *
 */

public class QueryClient {
    public static Debug debug = Debug.getInstance("libSAML2");
    private static SAML2MetaManager saml2MetaManager = null;
    static {
        try {
            saml2MetaManager =
                    new SAML2MetaManager();
        } catch (SAML2MetaException sme) {
            debug.error("Error retreiving metadata",sme);
        }
    }
    
    private QueryClient() {}
    
    /**
     * Returns SAMLv2 <code>Response</code>.
     * SAMLv2 request is sent enclosed in the body of a  SOAP Message
     * to a SOAP endpoint.
     * Prior to sending the request query, attributes required for completeness
     * of the SAMLv2 Request will be set (eg. Issuer) if not already set.
     * Message will be signed if signing is enabled.
     * SAMLv2 Query Request will be enclosed in the SOAP Body to create a SOAP
     * message to send to the server.
     *
     * @param request the SAMLv2 <code>RequestAbstract</code> object.
     * @param pepEntityID entity identifier of the hosted query requester.
     * @param pdpEntityID entity identifier of the remote server.
     * @return SAMLv2 <code>Response</code> received from the
     *         Query Responder.
     * @throws SAML2Exception if there is an error processing the query.
     */
    public static Response processXACMLQuery(RequestAbstract request,
            String pepEntityID,
            String pdpEntityID)
            throws SAML2Exception {
        String classMethod = "QueryClient:processXACMLQuery";
        String realm = "/";
        Response samlResponse = null;
        Response response = null;
        // retreive pepEntityID metadata
        if (pepEntityID == null || pepEntityID.length() == 0 ) {
            debug.error(classMethod + "PEP Identifier is null");
            String[] data = { pepEntityID };
            LogUtil.error(Level.INFO,LogUtil.INVALID_PEP_ID,data);
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("nullPEP"));
        }
        // retreive pdpEntityID metadata
        if (pdpEntityID == null || pdpEntityID.length() == 0) {
            debug.error(classMethod + "PDP Identifier is null");
            String[] data = { pdpEntityID };
            LogUtil.error(Level.INFO,LogUtil.INVALID_PDP_ID,data);
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("nullPDP"));
        }
        
        if (request != null ) {
            // set properties in the request.
            XACMLAuthzDecisionQuery xacmlQuery =
                    (XACMLAuthzDecisionQuery) request;
            if (xacmlQuery != null) {
                // set Issuer
                Issuer issuer = createIssuer(pepEntityID);
                xacmlQuery.setIssuer(issuer);
                //generate ID
                String requestID = SAML2SDKUtils.generateID();
                xacmlQuery.setID(requestID);
                xacmlQuery.setVersion(SAML2Constants.VERSION_2_0);
                xacmlQuery.setIssueInstant(new Date());

                XACMLPDPConfigElement pdpConfig = getPDPConfig(realm,
                                                               pdpEntityID);
                if (pdpConfig != null) {
                    String wantQuerySigned = getAttributeValueFromPDPConfig(
                                             pdpConfig,
                                            "wantXACMLAuthzDecisionQuerySigned");
                    if (wantQuerySigned != null &&
                        wantQuerySigned.equals("true")) {
                        signAttributeQuery(xacmlQuery,realm,pepEntityID, false);
                    }
                }
                
                String xmlString = xacmlQuery.toXMLString(true,true);
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "XACML Query XML String :"
                            + xmlString);
                }
                // retrieve endpoint from meta data
                String endPoint = null;
                XACMLAuthzDecisionQueryConfigElement pepConfig =
                            getPEPConfig(realm,pepEntityID);
                endPoint = getPDPEndPoint(pdpEntityID);
                if (debug.messageEnabled()) {
                   debug.message(classMethod + " ResponseLocation is :" +
                                endPoint);
                }
                // create SOAP message
                try {
                   String soapMessage =
                       SAML2SDKUtils.createSOAPMessageString(xmlString);
                        
                   endPoint =
                                SAML2SDKUtils.fillInBasicAuthInfo(
                                pepConfig,endPoint);
                        String[] urls = { endPoint };
                        SOAPClient soapClient = new SOAPClient(urls);
                        if (debug.messageEnabled()) {
                            debug.message(classMethod + "soapMessage :" +
                                    soapMessage);
                        }
                        InputStream soapIn =
                                soapClient.call(soapMessage,null,null);
                        StringBuffer reply = new StringBuffer();
                        String line;
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(soapIn, "UTF-8"));
                        while ((line = reader.readLine()) != null) {
                            reply.append(line).append("\n");
                        }
                        // check the SOAP message for any SOAP related errors
                        // before passing control to SAML processor
                        xmlString = reply.toString();
                        if (debug.messageEnabled()) {
                            debug.message("Response Message:\n"+ xmlString);
                        }
                        
                        samlResponse = getSAMLResponse(xmlString);
                        
                        issuer  = samlResponse.getIssuer();
                        String issuerID = null;
                        if (issuer != null) {
                            issuerID = issuer.getValue().trim();
                        }
                        boolean isTrusted = verifyResponseIssuer(realm,
                                pepEntityID,issuerID);
                        
                        if (!isTrusted) {
                            if (debug.messageEnabled()) {
                                debug.message(classMethod +
                                        "Issuer in Request is not valid.");
                            }
                            String[] args = {realm, pepEntityID, pdpEntityID};
                            
                            LogUtil.error(Level.INFO,
                                    LogUtil.INVALID_ISSUER_IN_PEP_REQUEST,
                                    args);
                            throw new SAML2Exception("invalidIssuerInRequest");
                        }
                        if (samlResponse != null) {
                            xmlString =samlResponse.toXMLString(true,true);
                            if (debug.messageEnabled()) {
                                debug.message(classMethod + "Response: "
                                        + xmlString);
                            }
                            response =
                                verifyResponse(realm,pepEntityID,samlResponse);
                            if (debug.messageEnabled()) {
                                debug.message(classMethod +
                                        "Response with decrypted Assertion: "
                                        + response.toXMLString(true,true));
                            }
                        }
                        
                    } catch (SOAPException soae) {
                        if (debug.messageEnabled()) {
                            debug.message(classMethod + "SOAPException :",soae);
                        }
                        throw new SAML2Exception(soae.getMessage());
                    } catch (Exception e) {
                        if (debug.messageEnabled()) {
                            debug.message(classMethod + "Exception " , e);
                        }
                        throw new SAML2Exception(e.getMessage());
                    }
                }
            }
        return response;
    }
    
    /**
     * Returns <code>Issuer</code> for the entity identifier.
     *
     * @param entityID entity identifier.
     * @return the <code>Issuer</code> object.
     * @exception <code>SAML2Exception</code> if there is an error creating
     *            the issuer.
     */
    private static Issuer createIssuer(String entityID)
    throws SAML2Exception {
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(entityID);
        return issuer;
    }
    
    /**
     * Returns SAMLv2 <code>Response</code> object. The <code>Response</code>
     * object is created from the String representation of the object.
     *
     * @param xmlString the String representation of the object.
     * @return the <code>Response</code> object.
     * @exception <code>IOException</code> if there is an error processing the
     *            response.
     * @exception <code>SAML2Exception</code> if there is an error processing
     *            the response.
     */
    private static Response getSAMLResponse(String xmlString)
    throws IOException,SAML2Exception {
        String classMethod="QueryClient:getSAMLResponse";
        if (xmlString == null || xmlString.length() == 0) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("nullResponse"));
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Response String : " + xmlString);
        }
        Response samlResponse = null;
        Document doc = XMLUtils.toDOMDocument(xmlString, debug);
        Element root= doc.getDocumentElement();
        String rootName  = root.getLocalName();
        if (!(rootName.equals("Envelope")) ||
                (!(root.getNamespaceURI().equals(
                SOAPConstants.URI_NS_SOAP_ENVELOPE)))) {
            SAML2SDKUtils.debug.error("Wrong Envelope tag or namespace.");
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("serverError"));
        }
        //examine the child element of <SOAP-ENV:Envelope>
        NodeList  nodes = root.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount <= 0) {
            SAML2SDKUtils.debug.error("Envelope does not contain a SOAP body.");
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("missingSOAPBody"));
        }
        String tagName = null;
        String ctagName = null;
        Node currentNode = null;
        Node cnode = null;
        for (int i = 0; i < nodeCount; i++) {
            currentNode = nodes.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                tagName = currentNode.getLocalName();
                if ((tagName == null) || tagName.length() == 0) {
                    SAML2SDKUtils.debug.error(classMethod +
                            "Child element is missing tag name");
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("missingChildTagName"));
                }
                if (tagName.equals("Body")) {
                    NodeList cNodes = currentNode.getChildNodes();
                    int cnodeCount = cNodes.getLength();
                    for (int j = 0; j < cnodeCount; j++) {
                        cnode = cNodes.item(j);
                        if (cnode.getNodeType() == Node.ELEMENT_NODE){
                            ctagName = cnode.getLocalName();
                            if ((ctagName == null) || ctagName.length() == 0) {
                                SAML2SDKUtils.debug.error("Missing tag name " +
                                        "of child element of <SOAP-ENV:Body>");
                                throw new SAML2Exception(
                                        SAML2SDKUtils.bundle.getString(
                                        "missingChildTagName"));
                            }
                            if (ctagName.equals("Fault")) {
                                SAML2SDKUtils.debug.error("SOAPFault error.");
                                throw new SAML2Exception(
                                        XMLUtils.print(cnode));
                            } else if (ctagName.equals("Response")) {
                                samlResponse = ProtocolFactory.getInstance()
                                .createResponse((Element)cnode);
                                break;
                            } else {
                                SAML2SDKUtils.debug.error("Invalid child  " +
                                        "element in SOAPBody");
                                throw new SAML2Exception(
                                        SAML2SDKUtils.bundle.getString(
                                        "invalidSOAPBody"));
                            }
                        }
                    } // end of for(int j=0; j <cnodeCount; j++)
                } else if (tagName.equals("Header")) {
                    if (SAML2SDKUtils.debug.messageEnabled()) {
                        SAML2SDKUtils.debug.message("SOAP Header in Response");
                    }
                } else {
                    SAML2SDKUtils.debug.error("Invalid element in Envelope");
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidSOAPElement"));
                }
            } // end of if (currentNode.getNodeType() == Node.ELEMENT_NODE)
        } // end of for (int i = 0; i < nodeCount; i++)
        return samlResponse;
    }
    
    /**
     * Returns true if the PEP and PDP entities are in the same Circle of
     * Trust. Verifies <code>Issuer</code> in <code>Response</code> to
     * retreive the PDP entity identity.
     *
     * @param realm realm of the entity.
     * @param pepEntityID  PEP entity identifier
     * @param pdpEntityID  <code>Response</code> issuer identifier.
     * @return true if issuer is valid.
     * @throws SAML2Exception if there is an error during verification.
     */
    private static boolean verifyResponseIssuer(String realm,String pepEntityID,
            String pdpEntityID) throws SAML2Exception {
        boolean isTrusted = false;
        try {
            isTrusted = saml2MetaManager.
                    isTrustedXACMLProvider(realm,pepEntityID,
                    pdpEntityID,SAML2Constants.PEP_ROLE);
        } catch (SAML2MetaException sme) {
            debug.error("Error retreiving meta",sme);
        }
        return isTrusted;
    }
    
    /**
     * Returns the Policy Decision Point End Point (PDP) URL.
     *
     * @param pdpEntityID entity Identifier of the PDP.
     * @return the PDP endpoint URL.
     * @exception if there is an error retreiving the endpoint from the
     *            configuration.
     */
    private static String getPDPEndPoint(String pdpEntityID)
    throws SAML2Exception {
        String endPoint = null;
        String classMethod = "QueryClient:getPDPEndPoint";
        if (saml2MetaManager != null)  {
            try {
                XACMLPDPDescriptorElement pdpDescriptor =
                        saml2MetaManager.
                        getPolicyDecisionPointDescriptor(null,
                        pdpEntityID);
                if (pdpDescriptor != null) {
                    List xacmlPDP = pdpDescriptor.getXACMLAuthzService();
                    if (xacmlPDP != null) {
                        Iterator i = xacmlPDP.iterator();
                        while (i.hasNext()) {
                            Object o = (Object) i.next();
                            if (o instanceof XACMLAuthzServiceElement) {
                                XACMLAuthzServiceElement xType =
                                        (XACMLAuthzServiceElement) o;
                                endPoint = xType.getLocation();
                                if (debug.messageEnabled()) {
                                    debug.message(classMethod +
                                            "EndPoint :" + endPoint);
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (SAML2MetaException sme) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod+"Error retreiving PDP Meta",sme);
                }
                String[] args = { pdpEntityID };
                LogUtil.error(Level.INFO,LogUtil.PDP_METADATA_ERROR,args);
                throw new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME,
                        "pdpMetaRetreivalError",args);
            }
        }
        return endPoint;
    }
    
    /**
     * Returns the extended Policy Enforcement Point Configuration.
     *
     * @param realm the realm of the entity.
     * @param pepEntityId identifier of the PEP.
     * @return the <code>XACMLAuthzDecisionQueryConfigElement</code> object.
     * @exception <code>SAML2Exception</code> if there is an error retreiving
     *            the extended configuration.
     */
    private static XACMLAuthzDecisionQueryConfigElement getPEPConfig(
            String realm,
            String pepEntityID) throws SAML2Exception {
        XACMLAuthzDecisionQueryConfigElement pepConfig = null;
        String classMethod = "QueryClient:getPEPConfig";
        if (saml2MetaManager != null)  {
            try {
                pepConfig =
                        saml2MetaManager.
                        getPolicyEnforcementPointConfig(realm,pepEntityID);
            } catch (SAML2MetaException sme) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod+"Error retreiving PEP meta",sme);
                }
                String[] args = { pepEntityID };
                LogUtil.error(Level.INFO,LogUtil.PEP_METADATA_ERROR,args);
                throw new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME,
                        "pepMetaRetreivalError",args);
                
            }
        }
        return pepConfig;
    }

    /**
     * Returns the extended Policy Decision Point Configuration.
     *
     * @param realm the realm of the entity.
     * @param pdpEntityId identifier of the PDP.
     * @return the <code>XACMLPDPConfigElement</code> object.
     * @exception <code>SAML2Exception</code> if there is an error retreiving
     *            the extended configuration.
     */
    private static XACMLPDPConfigElement getPDPConfig(
            String realm,
            String pdpEntityID) throws SAML2Exception {
        
        XACMLPDPConfigElement pdpConfig = null;
        String classMethod = "QueryClient:getPDPConfig";
        if (saml2MetaManager != null)  {
            try {
                pdpConfig =
                        saml2MetaManager.getPolicyDecisionPointConfig(realm,
                                                                   pdpEntityID);
            } catch (SAML2MetaException sme) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod+"Error retreiving PDP meta",sme);
                }
                String[] args = { pdpEntityID };
                LogUtil.error(Level.INFO,LogUtil.PEP_METADATA_ERROR,args);
                throw new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME,
                        "pdpMetaRetreivalError",args);

            }
        }
        return pdpConfig;
    }

    /**
     * Returns SAMLv2 <code>Response</code> after validation of the
     * response. A new <code>Response</code> object is created which
     * contains decrypted assertion if the assertions were encrypted.
     *
     * @param realm the realm of the entity.
     * @param pepEntityID entity identifier of the PEP.
     * @param samlResponse the <code>Response</code>.
     * @exception <code>SAML2Exception</code> if there is an error.
     */
    private static Response verifyResponse(String realm,String pepEntityID,
            Response samlResponse) throws SAML2Exception {
        
        Response response = samlResponse;
        String classMethod = "QueryClient:verifyResponse";
        if (samlResponse != null) {
            //validate issuer trust.
            Issuer issuer  = samlResponse.getIssuer();
            String issuerID = null;
            if (issuer != null) {
                issuerID = issuer.getValue().trim();
            }
            String pdpEntityID = issuerID;
            boolean isTrusted =
                    verifyResponseIssuer(realm,pepEntityID,issuerID);
            
            if (!isTrusted) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                            "Issuer in Request is not valid.");
                }
                String[] args = {realm, pepEntityID, issuerID};
                
                LogUtil.error(Level.INFO,
                        LogUtil.INVALID_ISSUER_IN_PEP_REQUEST,
                        args);
                throw new SAML2Exception(
                        SAML2SDKUtils.BUNDLE_NAME,"invalidIssuer",args);
            }
            // verify signed response
            verifySignedResponse(pepEntityID,pdpEntityID, samlResponse);
            try {
                // check if assertion needs to be encrypted,signed.
                XACMLAuthzDecisionQueryConfigElement pepConfig =
                        saml2MetaManager.getPolicyEnforcementPointConfig(realm,
                        pepEntityID);
                String assertionEncrypted = getAttributeValueFromPEPConfig(
                        pepConfig,SAML2Constants.WANT_ASSERTION_ENCRYPTED);
                boolean wantAssertionEncrypted = (assertionEncrypted != null &&
                        assertionEncrypted.equalsIgnoreCase("true"))
                        ? true:false;
                boolean wantAssertionSigned =
                        wantAssertionSigned(realm,pepEntityID);
                
                String respID = samlResponse.getID();
                List assertions = samlResponse.getAssertion();
                if (wantAssertionEncrypted &&  (assertions != null
                        && (assertions.size() != 0))) {
                    String[] data = { issuerID , respID  };
                    LogUtil.error(Level.INFO,
                            LogUtil.ASSERTION_FROM_PDP_NOT_ENCRYPTED,
                            data);
                    throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString(
                            "assertionNotEncrypted"));
                }
                PrivateKey decryptionKey = null;
                List encAssertions = samlResponse.getEncryptedAssertion();
                List decAssertions = null;
                if (encAssertions != null) {
                    Iterator encIter = encAssertions.iterator();
                    while (encIter.hasNext()) {
                        if (decryptionKey == null) {
                            decryptionKey = KeyUtil.getDecryptionKey(pepConfig);
                        }
                        Assertion assertion =
                                ((EncryptedAssertion) encIter.next()).
                                decrypt(decryptionKey);
                        if (assertions == null) {
                            assertions = new ArrayList();
                        }
                        assertions.add(assertion);
                    }
                }
                
                if (assertions == null || assertions.size() == 0) {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "no assertion in the Response.");
                    }
                    String[] data = {issuerID , respID};
                    LogUtil.error(Level.INFO,
                            LogUtil.MISSING_ASSERTION_IN_PDP_RESPONSE,data);
                    throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("missingAssertion"));
                }
                
                // validate Issuer  in Assertion
                Iterator assertionIter = assertions.iterator();
                X509Certificate cert = null;
                XACMLPDPDescriptorElement pdpDesc = null;
                if (wantAssertionSigned) {
                    pdpDesc =
                            saml2MetaManager.getPolicyDecisionPointDescriptor(
                            realm,pdpEntityID);
                    cert = KeyUtil.getPDPVerificationCert(pdpDesc,pdpEntityID);
                }
                
                while (assertionIter.hasNext()) {
                    Assertion assertion = (Assertion) assertionIter.next();
                    String assertionID = assertion.getID();
                    String assertionIssuer =
                            assertion.getIssuer().getValue().trim();
                    isTrusted =
                            verifyResponseIssuer(
                            realm, pepEntityID,assertionIssuer);
                    if (!isTrusted) {
                        debug.error(classMethod +
                                "Assertion's source site is not valid.");
                        String[] data = {assertionIssuer,assertionID};
                        LogUtil.error(Level.INFO,
                            LogUtil.INVALID_ISSUER_IN_ASSERTION_FROM_PDP,data);
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                                "invalidIssuerInAssertion"));
                    }
                    String respIssuer =
                            samlResponse.getIssuer().getValue().trim();
                    if (!respIssuer.equals(assertionIssuer)) {
                        if (debug.messageEnabled()) {
                            debug.message(classMethod + "Issuer in Assertion "+
                                    assertionIssuer
                                    + "doesn't match the Issuer in Response."
                                    + respIssuer);
                        }
                        String[] data = {pdpEntityID , assertionIssuer};
                        LogUtil.error(Level.INFO,
                            LogUtil.MISMATCH_ISSUER_IN_ASSERTION_FROM_PDP,data);
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("mismatchIssuer"));
                    }
                    if (wantAssertionSigned) {
                        if (debug.messageEnabled()) {
                            debug.message(classMethod + "wantAssertionSigned "
                                    + wantAssertionSigned);
                        }
                        if (!assertion.isSigned() ||
                                !assertion.isSignatureValid(cert)){
                            debug.error(classMethod +
                                    "Assertion is not signed or signature " +
                                    "is not valid.");
                            String[] data = {assertionIssuer , assertionID};
                            LogUtil.error(Level.INFO,
                                LogUtil.INVALID_SIGNATURE_ASSERTION_FROM_PDP,
                                data);
                            throw new SAML2Exception(
                                    SAML2SDKUtils.bundle.getString(
                                    "invalidSignatureOnAssertion"));
                        }
                    }
                } //end while
                if (wantAssertionEncrypted) {
                    response = createResponse(samlResponse,assertions);
                }
                if (debug.messageEnabled()) {
                    debug.message(classMethod + " Response : " +
                            response.toXMLString(true,true));
                }
            } catch (SAML2MetaException sme) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Error retreiving meta",sme);
                }
                throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("metaDataError"));
            }
        }
        return response;
        
    }
    
    /**
     * Returns SAMLv2 <code>Response</code> object.
     * A new <code>Response</code> object is created from
     * the received response to include decrypted assertion
     * if the response  contained encrypted assertion.
     *
     * @param samlResponse the <code>Response</code> object.
     * @return <code>Response</code> object.
     * @exception <code>SAML2Exception</code> if there is an error creating
     *            the response.
     */
    private static Response createResponse(Response samlResponse,
            List assertions) throws SAML2Exception  {
        
        Response response = new ResponseImpl();
        response.setVersion(samlResponse.getVersion()) ;
        response.setIssueInstant(samlResponse.getIssueInstant());
        response.setID(samlResponse.getID());
        response.setInResponseTo(samlResponse.getInResponseTo());
        response.setIssuer(samlResponse.getIssuer());
        response.setDestination(samlResponse.getDestination());
        response.setExtensions(samlResponse.getExtensions());
        response.setConsent(samlResponse.getConsent());
        response.setStatus(samlResponse.getStatus());
        response.setAssertion(assertions);
        
        return response;
    }
    
    /**
     * Returns value of an attribute in the PEP extended configuration.
     *
     * @param pepConfig the PEP extended configuration object.
     * @param attrName the attribute name whose value is to be retreived.
     * @return value of the attribute.
     * @exception <code>SAML2MetaException</code> if there is an error
     *             retreiving the value.
     */
    private static String getAttributeValueFromPEPConfig(
            XACMLAuthzDecisionQueryConfigElement pepConfig,String attrName)
            throws SAML2MetaException {
        String classMethod = "QueryClient:getAttributeValueFromPEPConfig:";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "attrName : " + attrName);
        }
        String result = null;
        
        Map attrs = SAML2MetaUtils.getAttributes(pepConfig);
        
        if (attrs != null) {
            List value = (List) attrs.get(attrName);
            if (value != null && value.size() != 0) {
                result = (String) value.get(0);
            }
        }
        
        if (debug.messageEnabled()) {
            debug.message(classMethod + "Attribute value is : " + result);
        }
        return result;
    }

   /**
     * Returns value of an attribute in the PDP extended configuration.
     *
     * @param pdpConfig the PDP extended configuration object.
     * @param attrName the attribute name whose value is to be retreived.
     * @return value of the attribute.
     * @exception <code>SAML2MetaException</code> if there is an error
     *             retreiving the value.
     */
    private static String getAttributeValueFromPDPConfig(
            XACMLPDPConfigElement pdpConfig,String attrName)
            throws SAML2MetaException {

        String classMethod = "QueryClient:getAttributeValueFromPDPConfig:";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "attrName : " + attrName);
        }
        String result = null;

        Map attrs = SAML2MetaUtils.getAttributes(pdpConfig);

        if (attrs != null) {
            List value = (List) attrs.get(attrName);
            if (value != null && value.size() != 0) {
                result = (String) value.get(0);
            }
        }

        if (debug.messageEnabled()) {
            debug.message(classMethod + "Attribute value is : " + result);
        }
        return result;
    }

    /**
     * Returns true if the assertion is to be signed.
     * The PEP Standard metdata configuration is retreived to
     * get the value of the attribute WantAssertionsSigned.
     *
     * @param realm the entity's realm.
     * @param pepEntityID entity identifier of the PEP.
     * @return true if the value of the attribute is true.
     * @exception <code>SAML2MetaException</code> if there is an error
     * retreiving the configuration.
     */
    private static boolean wantAssertionSigned(String realm,String pepEntityID)
    throws SAML2MetaException {
        XACMLAuthzDecisionQueryDescriptorElement
                pepDescriptor  =
                saml2MetaManager.
                getPolicyEnforcementPointDescriptor(realm,
                pepEntityID);
        
        return pepDescriptor.isWantAssertionsSigned();
    }


    /**
     * 
     * @param xacmlQuery XACML Query
     * @param realm the entity's realm.
     * @param pepEntityID entity identifier of PEP.
     * @param pdpEntityID entity identifier of PDP.
     * @throws <code>SAML2Exception</code> if error in verifying
     *         the signature.
     */
    private static void signAttributeQuery(XACMLAuthzDecisionQuery xacmlQuery,
        String realm, String pepEntityID, boolean includeCert) throws
        SAML2Exception {

        KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();
        XACMLAuthzDecisionQueryConfigElement pepConfig =
                              getPEPConfig(realm,pepEntityID);

        String alias =
                getAttributeValueFromPEPConfig(pepConfig,
                "signingCertAlias");

        PrivateKey signingKey = keyProvider.getPrivateKey(alias);
        if (signingKey == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }

        X509Certificate signingCert = null;
        if (includeCert) {
            signingCert = keyProvider.getX509Certificate(alias);
        }

        if (signingKey != null) {
            xacmlQuery.sign(signingKey, signingCert);
        }
    }

    /**
     * Verify the signature in <code>Response</code>.
     *
     * @param pepEntityID entity identifier of PEP.
     * @param pdpEntityID entity identifier of PDP.
     * @param response <code>Response</code> to be verified
     * @return true if signature is valid.
     * @throws <code>SAML2Exception</code> if error in verifying
     *         the signature.
     */
    public static boolean verifySignedResponse(String pepEntityID,
            String pdpEntityID, Response response) throws SAML2Exception {
        String classMethod = "QueryClient:verifySignedResponse: ";
        
        String realm = "/";
        XACMLAuthzDecisionQueryConfigElement pepConfig =
                getPEPConfig(realm,pepEntityID);
        
        String wantResponseSigned =
                getAttributeValueFromPEPConfig(pepConfig,
                "wantXACMLAuthzDecisionResponseSigned");
        
        boolean valid = false;
        if (wantResponseSigned != null &&
                wantResponseSigned.equalsIgnoreCase("true")) {
            XACMLPDPDescriptorElement
                    pdpDescriptor  =
                    saml2MetaManager.
                    getPolicyDecisionPointDescriptor(null, pdpEntityID);
            X509Certificate signingCert =
                    KeyUtil.getPDPVerificationCert(pdpDescriptor,pdpEntityID);
            if (signingCert != null) {
                valid = response.isSignatureValid(signingCert);
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Signature is valid :" + valid);
                }
            } else {
                debug.error(classMethod +
                        "Incorrect configuration for Signing Certificate.");
                throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("metaDataError"));
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "Response doesn't need to be verified.");
            }
            valid = true;
        }
        return valid;
    }
}
