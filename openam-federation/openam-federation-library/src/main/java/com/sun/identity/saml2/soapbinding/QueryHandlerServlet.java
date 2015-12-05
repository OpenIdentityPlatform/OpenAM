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
 * $Id: QueryHandlerServlet.java,v 1.9 2009/09/22 22:49:28 madan_ranganath Exp $
 *
 * Portions Copyrighted 2012-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.soapbinding;

import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.key.EncInfo;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Element;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.shared.xml.XMLUtils;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * This class <code>QueryHandlerServlet</code> receives and processes
 * SAMLv2 Queries.
 */
public class QueryHandlerServlet extends HttpServlet {
    
    static final String REQUEST_ABSTRACT = "RequestAbstract";
    static final String  XSI_TYPE_ATTR = "xsi:type";
    static final String XACML_AUTHZ_QUERY = "XACMLAuthzDecisionQuery";
    static final String METAALIAS_KEY = "/metaAlias" ;
    static Debug debug = Debug.getInstance("libSAML2");

    
    public void init() throws ServletException {
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @exception ServletException if the request could not be
     *         handled.
     * @exception IOException if an input or output error occurs.
     */
    
    public void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        
        processRequest(request,response);
    }
    
    /**
     * Processes the <code>HttppServletRequest</code>.
     */
    
    private void processRequest(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        String classMethod = "QueryHandlerServlet:processRequest";
        try {
            // handle DOS attack
            SAMLUtils.checkHTTPContentLength(request);
            // Get PDP entity ID
            String requestURI = request.getRequestURI();
            String queryMetaAlias =
                    SAML2MetaUtils.getMetaAliasByUri(requestURI);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "queryMetaAlias is :"
                        + queryMetaAlias);
            }
            String pdpEntityID = 
                    SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(
                                                                queryMetaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(queryMetaAlias);
            
            if (debug.messageEnabled()) {
                debug.message(classMethod + "uri : " + requestURI
                        + ",queryMetaAlias=" + queryMetaAlias
                        + ", pdpEntityID=" + pdpEntityID);
            }

            SOAPMessage soapMsg = SOAPCommunicator.getInstance().getSOAPMessage(request);
            Element soapBody = SOAPCommunicator.getInstance().getSOAPBody(soapMsg);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "SOAPMessage received.:"
                        + XMLUtils.print(soapBody));
            }
            SOAPMessage reply = null;
            reply = onMessage(soapMsg,request,response,realm,pdpEntityID);
            if (reply != null) {
                if (reply.saveRequired()) {
                    reply.saveChanges();
                }
                response.setStatus(HttpServletResponse.SC_OK);
                SAML2Utils.putHeaders(reply.getMimeHeaders(), response);
            } else {
                // Error
               debug.error(classMethod + "SOAPMessage is null");
               response.setStatus(HttpServletResponse.SC_NO_CONTENT);
               reply = SOAPCommunicator.getInstance().createSOAPFault(
                       SAML2Constants.SERVER_FAULT, "invalidQuery", null);
            }
             // Write out the message on the response stream
            OutputStream os = response.getOutputStream();
            reply.writeTo(os);
            os.flush();
        } catch (SAML2Exception ex) {
            debug.error(classMethod, ex);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "failedToProcessRequest", ex.getMessage());
            return;
        } catch (SOAPException soap) {
            debug.error(classMethod, soap);
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "failedToProcessRequest", soap.getMessage());
            return;
        }
    }
    
    /**
     * Process the incoming SOAP message containing the Query Request and
     * generates outgoing SOAP message containing the Query Response.
     *
     * @param soapMsg incoming SOAP message.
     * @param request HTTP servlet request.
     * @param response HTTP servlet response.
     * @param realm realm of the Policy Decision Point (PDP).
     * @param pdpEntityID Entity ID of the Policy Decision Point (PDP).
     * @return SOAP message containing the outgoing Response.
     */
    public SOAPMessage onMessage(
            SOAPMessage soapMsg,
            HttpServletRequest request,
            HttpServletResponse response,
            String realm,
            String pdpEntityID) throws SOAPException {
        
        String classMethod = "QueryHandlerServlet:onMessage:";
        SOAPMessage soapMessage = null;
        String pepEntityID = null;
        try {
            Element soapBody = SOAPCommunicator.getInstance().getSOAPBody(soapMsg);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "SOAPMessage recd. :"
                        + XMLUtils.print(soapBody));
            }
            Element reqAbs = SOAPCommunicator.getInstance().getSamlpElement(soapMsg,
                    REQUEST_ABSTRACT);
            
            Response samlResponse = 
                processSAMLRequest(realm,pdpEntityID,reqAbs,request,soapMsg);
            soapMessage = SOAPCommunicator.getInstance().createSOAPMessage(
                    samlResponse.toXMLString(true, true), false);
        } catch (SAML2Exception se) {
            debug.error(classMethod + "XACML Response Error SOAP Fault", se);
            soapMessage = SOAPCommunicator.getInstance().createSOAPFault(
                    SAML2Constants.SERVER_FAULT, "invalidQuery", se.getMessage());
        }
        return soapMessage;
    }
    
    /**
     * Signs an <code>Assertion</code>.
     *
     * @param realm the realm name of the Policy Decision Point (PDP).
     * @param pdpEntityID the entity id of the policy decision provider.
     * @param assertion the <code>Assertion</code> to be signed.
     * @exception <code>SAML2Exception</code> it there is an error signing
     *            the assertion.
     */
    static void signAssertion(String realm,String pdpEntityID,
                            Assertion assertion) throws SAML2Exception {
        String classMethod = "QueryHandlerServlet.signAssertion: ";

        // Don't load the KeyProvider object in static block as it can
        // cause issues when doing a container shutdown/restart.
        KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();
        if (keyProvider == null) {
            debug.error(classMethod +
                    "Unable to get a key provider instance.");
            throw new SAML2Exception("nullKeyProvider");
        }
        String pdpSignCertAlias = SAML2Utils.getAttributeValueFromXACMLConfig(
                realm, SAML2Constants.PDP_ROLE,pdpEntityID,
                SAML2Constants.SIGNING_CERT_ALIAS);
        if (pdpSignCertAlias == null) {
            debug.error(classMethod +
                    "Unable to get the hosted PDP signing certificate alias.");
            String[] data = { realm , pdpEntityID };
            LogUtil.error(Level.INFO,LogUtil.NULL_PDP_SIGN_CERT_ALIAS,data);
            throw new SAML2Exception("missingSigningCertAlias");
        }
        assertion.sign(keyProvider.getPrivateKey(pdpSignCertAlias),
                keyProvider.getX509Certificate(pdpSignCertAlias));
    }
    
    /**
     * Returns the SAMLv2 <code>Response</code> received in response to
     * the Request.
     *
     * @param realm the realm of the entity.
     * @param pdpEntityID entity identifier of the Policy Decision Point.
     * @param reqAbs the Document Element object.
     * @param request the <code>HttpServletRequest</code> object.
     * @param soapMsg the <code>SOAPMessage</code> object
     * @return the <code>Response</code> object.
     * @exception <code>SAML2Exception</code> if there is an error processing
     *            the request.
     */
    Response processSAMLRequest(String realm,String pdpEntityID,Element reqAbs,
                                HttpServletRequest request,SOAPMessage soapMsg)
                                throws SAML2Exception {
        String classMethod = "QueryHandlerServlet:processSAMLRequest";
        Response samlResponse = null;
        if (reqAbs != null) {
            String xsiType = reqAbs.getAttribute(XSI_TYPE_ATTR);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "xsi type is : " + xsiType);
            }
            if (xsiType != null && xsiType.indexOf(XACML_AUTHZ_QUERY) != -1) {
                RequestAbstract samlRequest =
                        ContextFactory.getInstance()
                        .createXACMLAuthzDecisionQuery(reqAbs);
                String requestStr = samlRequest.toXMLString(true,true);
                String[] data = { requestStr , pdpEntityID };
                LogUtil.access(Level.FINE,LogUtil.REQUEST_MESSAGE,data);
                
                Issuer issuer  = samlRequest.getIssuer();
                String pepEntityID = null;
                if (issuer != null) {
                    pepEntityID = issuer.getValue().trim();
                }
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Issuer is:" + pepEntityID);
                }
                boolean isTrusted = false;
                try {
                    isTrusted = SAML2Utils.getSAML2MetaManager().
                        isTrustedXACMLProvider(realm,pdpEntityID,pepEntityID,
                                                SAML2Constants.PDP_ROLE);
                } catch (SAML2MetaException sme) {
                    debug.error("Error retreiving meta",sme);
                }
                if (!isTrusted) {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "Issuer in Request is not valid."+ pepEntityID);
                    }
                    String[] args = { realm, pepEntityID, pdpEntityID};
                    LogUtil.error(Level.INFO,
                            LogUtil.INVALID_ISSUER_IN_PEP_REQUEST,
                                  args);
                    throw new SAML2Exception("invalidIssuerInRequest");
                }
                samlResponse =
                    processXACMLResponse(realm,pdpEntityID,samlRequest,request,
                        soapMsg);
                
            }
        }
        return samlResponse;
    }
    
    /**
     * Returns the received Response to the Requester.
     * Validates the message signature if signed and invokes the
     * Request Handler to pass the request for futher processing.
     *
     * @param realm realm of the entity.
     * @param pdpEntityID entity identifier of Policy Decision Point (PDP).
     * @param samlRequest the <code>RequestAbstract</code> object.
     * @param request the <code>HttpServletRequest</code> object.
     * @param soapMsg the <code>SOAPMessage</code> object.
     * @exception <code>SAML2Exception</code> if there is an error processing
     *            the request and returning a  response.
     */
    Response processXACMLResponse(String realm,String pdpEntityID,
            RequestAbstract samlRequest,HttpServletRequest request,
            SOAPMessage soapMsg) throws SAML2Exception {
        
        String classMethod = "QueryHandlerServlet:processXACMLResponse";
        Response samlResponse = null;
        String path = request.getPathInfo();
        String key = path.substring(path.indexOf(METAALIAS_KEY) + 10);
        String pepEntityID = samlRequest.getIssuer().getValue();
        if (debug.messageEnabled()) {
            debug.message(classMethod + "SOAPMessage KEY . :" + key);
            debug.message(classMethod + "pepEntityID is :" + pepEntityID);
        }
        //Retreive metadata
        boolean pdpWantAuthzQuerySigned =
                SAML2Utils.getWantXACMLAuthzDecisionQuerySigned(realm,
                pdpEntityID,SAML2Constants.PDP_ROLE);
        
        if (debug.messageEnabled()) {
            debug.message(classMethod + "PDP wantAuthzQuerySigned:" +
                    pdpWantAuthzQuerySigned);
        }
        if (pdpWantAuthzQuerySigned) {                        
            if (samlRequest.isSigned()) {
                XACMLAuthzDecisionQueryDescriptorElement pep =
                        SAML2Utils.getSAML2MetaManager().
                        getPolicyEnforcementPointDescriptor(
                        realm,pepEntityID);
                Set<X509Certificate> verificationCerts = KeyUtil.getPEPVerificationCerts(pep, pepEntityID);
                if (verificationCerts.isEmpty() || !samlRequest.isSignatureValid(verificationCerts)) {
                    // error
                    debug.error(classMethod + "Invalid signature in message");
                    throw new SAML2Exception("invalidQuerySignature");
                } else {
                    debug.message(classMethod + "Valid signature found");                    
                }
            } else {
                debug.error("Request not signed");
                throw new SAML2Exception("nullSig");
            }
        }
        
        //getRequestHandlerClass
        RequestHandler handler =
                (RequestHandler)SOAPBindingService.handlers.get(key);
        if (handler != null) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Found handler");
            }
            
            samlResponse = handler.handleQuery(pdpEntityID,pepEntityID,
                    samlRequest,soapMsg);
            // set response attributes
            samlResponse.setID(SAML2Utils.generateID());
            samlResponse.setVersion(SAML2Constants.VERSION_2_0);
            samlResponse.setIssueInstant(new Date());
            Issuer issuer = AssertionFactory.getInstance().createIssuer();
            issuer.setValue(pdpEntityID);
            samlResponse.setIssuer(issuer);
            // end set Response Attributes
            
            //set Assertion attributes
            List assertionList = samlResponse.getAssertion();
            Assertion assertion = (Assertion) assertionList.get(0);
            
            assertion.setID(SAML2Utils.generateID());
            assertion.setVersion(SAML2Constants.VERSION_2_0);
            assertion.setIssueInstant(new Date());
            assertion.setIssuer(issuer);
            // end assertion set attributes
            
            // check if assertion needs to be encrypted,signed.
            String wantAssertionEncrypted =
                    SAML2Utils.getAttributeValueFromXACMLConfig(
                    realm,SAML2Constants.PEP_ROLE,
                    pepEntityID,
                    SAML2Constants.WANT_ASSERTION_ENCRYPTED);
            
            
            XACMLAuthzDecisionQueryDescriptorElement
                    pepDescriptor  = SAML2Utils.
                    getSAML2MetaManager().
                    getPolicyEnforcementPointDescriptor(realm,
                    pepEntityID);
            
            EncInfo encInfo = null;
            boolean wantAssertionSigned=pepDescriptor.isWantAssertionsSigned();
            
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        " wantAssertionSigned :" + wantAssertionSigned);
            }
            if (wantAssertionSigned) {
                signAssertion(realm,pdpEntityID,assertion);
            }
            
            if (wantAssertionEncrypted != null
                    && wantAssertionEncrypted.equalsIgnoreCase
                    (SAML2Constants.TRUE)) {
                encInfo = KeyUtil.getPEPEncInfo(pepDescriptor,pepEntityID);
                
                // encrypt the Assertion
                EncryptedAssertion encryptedAssertion =
                        assertion.encrypt(
                        encInfo.getWrappingKey(),
                        encInfo.getDataEncAlgorithm(),
                        encInfo.getDataEncStrength(),
                        pepEntityID);
                if (encryptedAssertion == null) {
                    debug.error(classMethod+"Assertion encryption failed.");
                    throw new SAML2Exception("FailedToEncryptAssertion");
                }
                assertionList = new ArrayList();
                assertionList.add(encryptedAssertion);
                samlResponse.setEncryptedAssertion(assertionList);
                //reset Assertion list
                samlResponse.setAssertion(new ArrayList());
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Assertion encrypted.");
                }
            } else {
                List assertionsList = new ArrayList();
                assertionsList.add(assertion);
                samlResponse.setAssertion(assertionsList);
            }
            signResponse(samlResponse,realm,pepEntityID,pdpEntityID);
            
        } else {
            // error -  missing request handler.
            debug.error(classMethod + "RequestHandler not found");
            throw new SAML2Exception("missingRequestHandler");
        }
        return samlResponse;
    }
    
    /**
     * Signs the <code>Response</code>.
     *
     * @param response the <code>Response<code> object.
     * @param realm the realm of the entity.
     * @param pepEntityID Policy Enforcement Point Entity Identitifer.
     * @param pdpEntityID Policy Decision Point Entity Identifier.
     * @exception <code>SAML2Exception</code> if there is an exception.
     */
    static void signResponse(Response response,
                               String realm, String pepEntityID,
                               String pdpEntityID)
        throws SAML2Exception {
        String classMethod = "signResponse : ";
        String attrName = "wantXACMLAuthzDecisionResponseSigned";
        String wantResponseSigned = 
            SAML2Utils.getAttributeValueFromXACMLConfig(realm,
                SAML2Constants.PEP_ROLE,pepEntityID,attrName);        

        if (wantResponseSigned == null || 
                wantResponseSigned.equalsIgnoreCase("false")) {
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                              "Response doesn't need to be signed.");
            }
        } else {
            String pdpSignCertAlias = 
                SAML2Utils.getAttributeValueFromXACMLConfig(
                    realm, SAML2Constants.PDP_ROLE,pdpEntityID,
                    SAML2Constants.SIGNING_CERT_ALIAS);
            if (pdpSignCertAlias == null) {
                debug.error(classMethod + "PDP certificate alias is null.");
                String[] data = { realm , pdpEntityID };
                LogUtil.error(Level.INFO,LogUtil.NULL_PDP_SIGN_CERT_ALIAS,data);
                throw new SAML2Exception("missingSigningCertAlias");
            }
            
            if (debug.messageEnabled()) {
                debug.message(classMethod + "realm is : "+ realm);
                debug.message(classMethod + "pepEntityID is :" + pepEntityID);
                debug.message(classMethod + "pdpEntityID : " + pdpEntityID);
                debug.message(classMethod+ "wantResponseSigned" +
                                                wantResponseSigned);
                debug.message(classMethod + "Cert Alias:" + pdpSignCertAlias);
            }
            // Don't load the KeyProvider object in static block as it can
            // cause issues when doing a container shutdown/restart.
            KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();
            if (keyProvider == null) {
                debug.error(classMethod +
                        "Unable to get a key provider instance.");
                throw new SAML2Exception("nullKeyProvider");
            }
            PrivateKey signingKey = keyProvider.getPrivateKey(pdpSignCertAlias);
            X509Certificate signingCert = 
                keyProvider.getX509Certificate(pdpSignCertAlias);
            
            if (signingKey != null) {
                response.sign(signingKey, signingCert);
            } else {
                debug.error("Incorrect configuration for Signing Certificate.");
                throw new SAML2Exception("metaDataError");
            }
        }
    }
}
