/**
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.resources;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.key.EncInfo;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.soapbinding.RequestHandler;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.ContextFactory;

import com.sun.identity.xacml.saml2.XACMLAuthzDecisionQuery;

import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;

import org.forgerock.identity.openam.xacml.v3.model.XACMLRequestInformation;
import org.forgerock.identity.openam.xacml.v3.services.XacmlContentHandlerService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;

/**
 * XACML PDP Resource
 * <p/>
 * Policy decision point (PDP)
 * The system entity that evaluates applicable policy and renders an authorization decision.
 * This term is defined in a joint effort by the IETF Policy Framework Working Group
 * and the Distributed Management Task Force (DMTF)/Common Information Model (CIM) in [RFC3198].
 * This term corresponds to "Access Decision Function" (ADF) in [ISO10181-3].
 * <p/>
 * Provides main methods for all XACML 3 PDP requests, either SOAP or REST based.
 * This code was originally used from the @see com.sun.identity.saml2.soapbinding.QueryHandlerServlet.
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class XacmlPDPResource implements XACML3Constants {
    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug debug = Debug.getInstance("amXACML");

    /**
     * Do not allow instantiation of this Utility Resource.
     */
    private XacmlPDPResource() {
    }

    /**
     * Processes XML Requests from the PEP Request for the PDP.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public static void processPDP_XMLRequest(XACMLRequestInformation xacmlRequestInformation,
                                             HttpServletRequest request,
                                             HttpServletResponse response)
            throws ServletException, IOException {
        String classMethod = "XacmlContentHandlerService:processPDP_XMLRequest";
        // Get all the headers from the HTTP request
        MimeHeaders headers = SAML2Utils.getHeaders(request);

        // Check Original Content
        if (xacmlRequestInformation.getOriginalContent() == null) {
                // Bad or no content.
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);   // 204 // Fix this Return.
        }

            debug.error("XACML Incoming Request:[" + xacmlRequestInformation.getOriginalContent() + "], " +
                    "Length:[" + xacmlRequestInformation.getOriginalContent().length() + "]");

            // ********************************************
            // Process the PDP Request from the PEP.
            String pepEntityID = null;    // TODO Need to resolve this.

            try {
                Document requestDocument = (Document) xacmlRequestInformation.getContent();

                Response samlResponse =
                        processSAML4XACMLRequest(xacmlRequestInformation,
                                ((Node) xacmlRequestInformation.getAuthenticationContent()),
                                request,
                                requestDocument.getDocumentElement());
                if (samlResponse != null) {
                    // TODO -- Determine response...
                    //xacmlRequestInformation.setXacmlStringResponse(samlResponse.toXMLString(true, true));

                    // *******************************************
                    // Set our Response Status per specification.
                    response.setStatus(HttpServletResponse.SC_OK);   // 200
                    xacmlRequestInformation.setRequestProcessed(true);
                    return;

                } else {
                    // Bad or no content.
                    xacmlRequestInformation.setAuthenticated(false);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);   // 204 // Fix this Return.
                }
            } catch (Exception se) {
                debug.error(classMethod + "XACML Processing Exception", se);
                // TODO Handle invalid.
                // Bad or no content.
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);   // 204 // Fix this Return.
            }


        // TODO Handle invalid.
        SAML2Utils.putHeaders(headers, response);

    }

    /**
     * Processes JSON Requests from the PEP Request for the PDP.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public static void processPDP_JSONRequest(XACMLRequestInformation xacmlRequestInformation,
                                              HttpServletRequest request,
                                              HttpServletResponse response)
            throws ServletException, IOException {
        String classMethod = "XacmlContentHandlerService:processPDP_JSONRequest";
        // Get all the headers from the HTTP request
        MimeHeaders headers = SAML2Utils.getHeaders(request);

        // Ready our Response Object...
        com.sun.identity.entitlement.xacml3.core.Result xacmlResult = new com.sun.identity.entitlement.xacml3.core.Result();
        xacmlRequestInformation.getXacmlResponse().getResult().add(xacmlResult);
        // Check Original Content
        if (xacmlRequestInformation.getOriginalContent() == null) {
            // Bad or no content.
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);   // 204 // Fix this Return.
        }

        debug.error("XACML Incoming Request:[" + xacmlRequestInformation.getOriginalContent() + "], " +
                "Length:[" + xacmlRequestInformation.getOriginalContent().length() + "]");

        // ********************************************
        // Process the PDP Request from the PEP.
        String pepEntityID = null;    // TODO Need to resolve this.

        // TODO -- we  build up a Request Object from our JSON Map and send to normal XACML Processing.

        // TODO ....




    }

    /**
     * Returns the SAMLv2 <code>Response</code> received in response to
     * the Request.
     *
     * @param realm       the realm of the entity.
     * @param pdpEntityID entity identifier of the Policy Decision Point.
     * @param reqAbs      the Document Element object.
     * @param request     the <code>HttpServletRequest</code> object.
     * @param soapMsg     the <code>SOAPMessage</code> object
     * @return the <code>Response</code> object.
     * @throws <code>SAML2Exception</code> if there is an error processing
     *                                     the request.
     */
    static Response processSAMLSOAPRequest(String realm, String pdpEntityID, Element reqAbs,
                                           HttpServletRequest request, SOAPMessage soapMsg)
            throws SAML2Exception {
        String classMethod = "XacmlPDPResource:processSAMLSOAPRequest";
        Response samlResponse = null;
        if (reqAbs != null) {
            String xsiType = reqAbs.getAttribute(XSI_TYPE_ATTR);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "xsi type is : " + xsiType);
            }
            if (xsiType != null && xsiType.contains(XACML_AUTHZ_QUERY)) {
                RequestAbstract samlRequest =
                        ContextFactory.getInstance()
                                .createXACMLAuthzDecisionQuery(reqAbs);
                String requestStr = samlRequest.toXMLString(true, true);
                String[] data = {requestStr, pdpEntityID};
                LogUtil.access(Level.FINE, LogUtil.REQUEST_MESSAGE, data);

                Issuer issuer = samlRequest.getIssuer();
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
                            isTrustedXACMLProvider(realm, pdpEntityID, pepEntityID,
                                    SAML2Constants.PDP_ROLE);
                } catch (SAML2MetaException sme) {
                    debug.error("Error retreiving meta", sme);
                }
                if (!isTrusted) {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "Issuer in Request is not valid." + pepEntityID);
                    }
                    String[] args = {realm, pepEntityID, pdpEntityID};
                    LogUtil.error(Level.INFO,
                            LogUtil.INVALID_ISSUER_IN_PEP_REQUEST,
                            args);
                    throw new SAML2Exception("invalidIssuerInRequest");
                }
                samlResponse =
                        processXACMLSOAPResponse(realm, pdpEntityID, samlRequest, request,
                                soapMsg);

            }
        }
        return samlResponse;
    }

    /**
     * Returns the SAMLv2 <code>Response</code> received in response to
     * the Request.
     *
     * @param xacmlRequestInformation the request information object.
     * @param reqAbs                  the Document Element object.
     * @param request                 the <code>HttpServletRequest</code> object.
     * @param requestBodyNode         the <code>Element</code> object
     * @return the <code>Response</code> object.
     * @throws <code>SAML2Exception</code> if there is an error processing
     *                                     the request.
     */
    private static Response processSAML4XACMLRequest(XACMLRequestInformation xacmlRequestInformation, Node reqAbs,
                                                     HttpServletRequest request, Node requestBodyNode)
            throws SAML2Exception {
        String classMethod = "XacmlContentHandlerService:processSAML4XACMLRequest";
        Response samlResponse = null;

        // TODO -------- Hook In!


        if ((reqAbs != null) && (reqAbs.hasAttributes())) {
            RequestAbstract samlRequest =
                    createXACML3AuthzDecisionQuery(reqAbs);
            if (samlRequest == null) {
                debug.error("Unable to Obtain SAML4XACML XacmlAuthzDecisionQuery!");
                return null;
            }

            // TODO Verify
            String requestStr = samlRequest.toXMLString(true, true);
            String[] data = {requestStr, xacmlRequestInformation.getPdpEntityID()};
            LogUtil.access(Level.FINE, LogUtil.REQUEST_MESSAGE, data);

            Issuer issuer = samlRequest.getIssuer();
            String pepEntityID = null;
            if (issuer != null) {
                pepEntityID = issuer.getValue().trim();
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Issuer is:" + pepEntityID);
            }
            boolean isTrusted = false;
            try {
                SAML2MetaManager saml2MetaManager = SAML2Utils.getSAML2MetaManager();
                if (saml2MetaManager == null) {
                    // We have no SAML2Manager to determine Trust Between PDP and PEP.
                    debug.error("Unable to obtain SAML2Manger Object to determine Trust Between " +
                            "PDP:[" + xacmlRequestInformation.getPdpEntityID() + "] and " +
                            "PEP:[" + pepEntityID + "], " +
                            "assuming no Trust.");
                } else {
                    // Check for Trust between PDP (OpenAM) and the PEP.
                    isTrusted = saml2MetaManager.
                            isTrustedXACMLProvider(xacmlRequestInformation.getRealm(),
                                    xacmlRequestInformation.getPdpEntityID(), pepEntityID, SAML2Constants.PDP_ROLE);
                }
            } catch (SAML2MetaException sme) {
                debug.error("Error retreiving meta", sme);
            }
            // Set our Authentication Indicator.
            // If false, will trigger caller to send back a 403 Forbidden.
            xacmlRequestInformation.setAuthenticated(isTrusted);
            if (!isTrusted) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                            "Issuer in Request is not valid, PEP ID:[" + pepEntityID + "], no Trust established.");
                }
                xacmlRequestInformation.setAuthenticated(false);
                String[] args = {xacmlRequestInformation.getRealm(), pepEntityID, xacmlRequestInformation.getPdpEntityID()};
                LogUtil.error(Level.INFO,
                        LogUtil.INVALID_ISSUER_IN_PEP_REQUEST,
                        args);
                throw new SAML2Exception("invalidIssuerInRequest");
            }
            // Process the XACML Request.
            samlResponse =
                    processXACML3Response(xacmlRequestInformation.getRealm(),
                            xacmlRequestInformation.getPdpEntityID(), samlRequest, request,
                            requestBodyNode.getOwnerDocument().getDocumentElement());  // TODO Verify...

        }
        return samlResponse;
    }


    /**
     * Returns a new instance of <code>XACMLAuthzDecisionQuery</code>.
     * The return object is immutable.
     *
     * @param node an <code>Element</code> representation of
     *             <code>XACMLAuthzDecisionQuery</code>.
     * @return a new instance of <code>XACMLAuthzDecisionQuery</code>.
     * @throws com.sun.identity.xacml.common.XACMLException
     *                        if error occurs while processing the
     *                        <code>Element</code>.
     * @throws SAML2Exception if not able to create the base saml
     *                        <code>RequestAbstract</code>
     */
    private static XACMLAuthzDecisionQuery createXACML3AuthzDecisionQuery(Node node)
            throws SAML2Exception {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.XACML_AUTHZ_DECISION_QUERY,
                                node.getOwnerDocument().getDocumentElement());
        if (obj == null) {
            // TODO Fix.....
            return null;
            //return new XACML3AuthzDecisionQueryImpl(node.getOwnerDocument().getDocumentElement());
        } else {
            return (XACMLAuthzDecisionQuery) obj;
        }
    }

    /**
     * Returns the received Response to the Requester.
     * Validates the message signature if signed and invokes the
     * Request Handler to pass the request for further processing.
     *
     * @param realm              realm of the entity.
     * @param pdpEntityID        entity identifier of Policy Decision Point (PDP).
     * @param samlRequest        the <code>RequestAbstract</code> object.
     * @param request            the <code>HttpServletRequest</code> object.
     * @param requestBodyElement the <code>Element</code> object.
     * @throws <code>SAML2Exception</code> if there is an error processing
     *                                     the request and returning a  response.
     */
    static Response processXACML3Response(String realm, String pdpEntityID,
                                          RequestAbstract samlRequest, HttpServletRequest request,
                                          Element requestBodyElement) throws SAML2Exception {

        String classMethod = "XacmlContentHandlerService:processXACMLResponse";
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
                        pdpEntityID, SAML2Constants.PDP_ROLE);

        if (debug.messageEnabled()) {
            debug.message(classMethod + "PDP wantAuthzQuerySigned:" +
                    pdpWantAuthzQuerySigned);
        }
        if (pdpWantAuthzQuerySigned) {
            if (samlRequest.isSigned()) {
                XACMLAuthzDecisionQueryDescriptorElement pep =
                        SAML2Utils.getSAML2MetaManager().
                                getPolicyEnforcementPointDescriptor(
                                        realm, pepEntityID);
                X509Certificate cert =
                        KeyUtil.getPEPVerificationCert(pep, pepEntityID);
                if (cert == null ||
                        !samlRequest.isSignatureValid(cert)) {
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

        // TODO -- Was from SOAP Method originally....
        //getRequestHandlerClass
        RequestHandler handler =
                (RequestHandler) XacmlContentHandlerService.getHandlers().get(key);  // TODO -- This was referencing
        // handlers in the SOAPBindingService class.
        if (handler != null) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Found handler");
            }

            // TODO Figure out how to implement this.
            //samlResponse = handler.handleQuery(pdpEntityID, pepEntityID,
            //        samlRequest, requestBodyElement);

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
                            realm, SAML2Constants.PEP_ROLE,
                            pepEntityID,
                            SAML2Constants.WANT_ASSERTION_ENCRYPTED);


            XACMLAuthzDecisionQueryDescriptorElement
                    pepDescriptor = SAML2Utils.
                    getSAML2MetaManager().
                    getPolicyEnforcementPointDescriptor(realm,
                            pepEntityID);

            EncInfo encInfo = null;
            boolean wantAssertionSigned = pepDescriptor.isWantAssertionsSigned();

            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        " wantAssertionSigned :" + wantAssertionSigned);
            }
            if (wantAssertionSigned) {
                signAssertion(realm, pdpEntityID, assertion);
            }

            if (wantAssertionEncrypted != null
                    && wantAssertionEncrypted.equalsIgnoreCase
                    (SAML2Constants.TRUE)) {
                encInfo = KeyUtil.getPEPEncInfo(pepDescriptor, pepEntityID);

                // encrypt the Assertion
                EncryptedAssertion encryptedAssertion =
                        assertion.encrypt(
                                encInfo.getWrappingKey(),
                                encInfo.getDataEncAlgorithm(),
                                encInfo.getDataEncStrength(),
                                pepEntityID);
                if (encryptedAssertion == null) {
                    debug.error(classMethod + "Assertion encryption failed.");
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
            signResponse(samlResponse, realm, pepEntityID, pdpEntityID);

        } else {
            // error -  missing request handler.
            debug.error(classMethod + "RequestHandler not found");
            throw new SAML2Exception("missingRequestHandler");
        }
        return samlResponse;
    }


    /**
     * Returns the received Response to the Requester.
     * Validates the message signature if signed and invokes the
     * Request Handler to pass the request for further processing.
     *
     * @param realm       realm of the entity.
     * @param pdpEntityID entity identifier of Policy Decision Point (PDP).
     * @param samlRequest the <code>RequestAbstract</code> object.
     * @param request     the <code>HttpServletRequest</code> object.
     * @param soapMsg     the <code>SOAPMessage</code> object.
     * @throws <code>SAML2Exception</code> if there is an error processing
     *                                     the request and returning a  response.
     */
    static Response processXACMLSOAPResponse(String realm, String pdpEntityID,
                                             RequestAbstract samlRequest, HttpServletRequest request,
                                             SOAPMessage soapMsg) throws SAML2Exception {

        String classMethod = "XacmlPDPResource:processXACMLSOAPResponse";
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
                        pdpEntityID, SAML2Constants.PDP_ROLE);

        if (debug.messageEnabled()) {
            debug.message(classMethod + "PDP wantAuthzQuerySigned:" +
                    pdpWantAuthzQuerySigned);
        }
        if (pdpWantAuthzQuerySigned) {
            if (samlRequest.isSigned()) {
                XACMLAuthzDecisionQueryDescriptorElement pep =
                        SAML2Utils.getSAML2MetaManager().
                                getPolicyEnforcementPointDescriptor(
                                        realm, pepEntityID);
                X509Certificate cert =
                        KeyUtil.getPEPVerificationCert(pep, pepEntityID);
                if (cert == null ||
                        !samlRequest.isSignatureValid(cert)) {
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
        RequestHandler handler = null; // TODO This is broke.....
        if (handler != null) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Found handler");
            }

            samlResponse = handler.handleQuery(pdpEntityID, pepEntityID,
                    samlRequest, soapMsg);
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
                            realm, SAML2Constants.PEP_ROLE,
                            pepEntityID,
                            SAML2Constants.WANT_ASSERTION_ENCRYPTED);


            XACMLAuthzDecisionQueryDescriptorElement
                    pepDescriptor = SAML2Utils.
                    getSAML2MetaManager().
                    getPolicyEnforcementPointDescriptor(realm,
                            pepEntityID);

            EncInfo encInfo = null;
            boolean wantAssertionSigned = pepDescriptor.isWantAssertionsSigned();

            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        " wantAssertionSigned :" + wantAssertionSigned);
            }
            if (wantAssertionSigned) {
                signAssertion(realm, pdpEntityID, assertion);
            }

            if (wantAssertionEncrypted != null
                    && wantAssertionEncrypted.equalsIgnoreCase
                    (SAML2Constants.TRUE)) {
                encInfo = KeyUtil.getPEPEncInfo(pepDescriptor, pepEntityID);

                // encrypt the Assertion
                EncryptedAssertion encryptedAssertion =
                        assertion.encrypt(
                                encInfo.getWrappingKey(),
                                encInfo.getDataEncAlgorithm(),
                                encInfo.getDataEncStrength(),
                                pepEntityID);
                if (encryptedAssertion == null) {
                    debug.error(classMethod + "Assertion encryption failed.");
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
            signResponse(samlResponse, realm, pepEntityID, pdpEntityID);

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
     * @param response    the <code>Response<code> object.
     * @param realm       the realm of the entity.
     * @param pepEntityID Policy Enforcement Point Entity Identitifer.
     * @param pdpEntityID Policy Decision Point Entity Identifier.
     * @throws <code>SAML2Exception</code> if there is an exception.
     */
    static void signResponse(Response response,
                             String realm, String pepEntityID,
                             String pdpEntityID)
            throws SAML2Exception {
        String classMethod = "signResponse : ";
        String attrName = "wantXACMLAuthzDecisionResponseSigned";
        String wantResponseSigned =
                SAML2Utils.getAttributeValueFromXACMLConfig(realm,
                        SAML2Constants.PEP_ROLE, pepEntityID, attrName);

        if (wantResponseSigned == null ||
                wantResponseSigned.equalsIgnoreCase("false")) {
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "Response doesn't need to be signed.");
            }
        } else {
            String pdpSignCertAlias =
                    SAML2Utils.getAttributeValueFromXACMLConfig(
                            realm, SAML2Constants.PDP_ROLE, pdpEntityID,
                            SAML2Constants.SIGNING_CERT_ALIAS);
            if (pdpSignCertAlias == null) {
                debug.error(classMethod + "PDP certificate alias is null.");
                String[] data = {realm, pdpEntityID};
                LogUtil.error(Level.INFO, LogUtil.NULL_PDP_SIGN_CERT_ALIAS, data);
                throw new SAML2Exception("missingSigningCertAlias");
            }

            if (debug.messageEnabled()) {
                debug.message(classMethod + "realm is : " + realm);
                debug.message(classMethod + "pepEntityID is :" + pepEntityID);
                debug.message(classMethod + "pdpEntityID : " + pdpEntityID);
                debug.message(classMethod + "wantResponseSigned" +
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

    /**
     * Signs an <code>Assertion</code>.
     *
     * @param realm       the realm name of the Policy Decision Point (PDP).
     * @param pdpEntityID the entity id of the policy decision provider.
     * @param assertion   the <code>Assertion</code> to be signed.
     * @throws <code>SAML2Exception</code> it there is an error signing
     *                                     the assertion.
     */
    static void signAssertion(String realm, String pdpEntityID,
                              Assertion assertion) throws SAML2Exception {
        String classMethod = "XacmlPDPResource.signAssertion: ";

        // Don't load the KeyProvider object in static block as it can
        // cause issues when doing a container shutdown/restart.
        KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();
        if (keyProvider == null) {
            debug.error(classMethod +
                    "Unable to get a key provider instance.");
            throw new SAML2Exception("nullKeyProvider");
        }
        String pdpSignCertAlias = SAML2Utils.getAttributeValueFromXACMLConfig(
                realm, SAML2Constants.PDP_ROLE, pdpEntityID,
                SAML2Constants.SIGNING_CERT_ALIAS);
        if (pdpSignCertAlias == null) {
            debug.error(classMethod +
                    "Unable to get the hosted PDP signing certificate alias.");
            String[] data = {realm, pdpEntityID};
            LogUtil.error(Level.INFO, LogUtil.NULL_PDP_SIGN_CERT_ALIAS, data);
            throw new SAML2Exception("missingSigningCertAlias");
        }
        assertion.sign(keyProvider.getPrivateKey(pdpSignCertAlias),
                keyProvider.getX509Certificate(pdpSignCertAlias));
    }


}

