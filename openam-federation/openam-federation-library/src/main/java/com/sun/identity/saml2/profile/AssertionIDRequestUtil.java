/**
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
 * $Id: AssertionIDRequestUtil.java,v 1.8 2009/06/12 22:21:40 mallas Exp $
 *
 * Portions Copyrighted 2013-2014 ForgeRock AS.
 */

/*
 * Portions Copyrighted 2013-2014 ForgeRock AS
 */

package com.sun.identity.saml2.profile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import com.sun.identity.saml2.common.SAML2FailoverUtils;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import com.sun.identity.common.HttpURLConnectionManager;
import org.w3c.dom.Element;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AssertionIDRef;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.metadata.AttributeAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.AssertionIDRequestServiceElement;
import com.sun.identity.saml2.jaxb.metadata.AuthnAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.plugins.AssertionIDRequestMapper;
import com.sun.identity.saml2.protocol.AssertionIDRequest;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;

/**
 * This class provides methods to send or process
 * <code>AssertionIDRequest</code>.
 *
 * @supported.api
 */
public class AssertionIDRequestUtil {

    static KeyProvider keyProvider = KeyUtil.getKeyProviderInstance(); 
    static SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
    static Hashtable assertionIDRequestMapperCache = new Hashtable(); 
    static final String MIME_TYPE_ASSERTION = "application/samlassertion+xml";

    private AssertionIDRequestUtil() {
    }

    /**
     * Sends the <code>AssertionIDRequest</code> to specifiied Assertion ID
     * Request Service and returns <code>Response</code> coming from the
     * Assertion ID Request Service.
     *
     * @param assertionIDRequest the <code>AssertionIDRequest</code> object
     * @param samlAuthorityEntityID entity ID of SAML authority
     * @param role SAML authority role, for example,
     * <code>SAML2Constants.ATTR_AUTH_ROLE</code>, 
     * <code>SAML2Constants.AUTHN_AUTH_ROLE</code> or
     * <code>SAML2Constants.IDP_ROLE</code>
     * @param realm the realm of hosted entity
     * @param binding the binding
     *
     * @return the <code>Response</code> object
     * @exception SAML2Exception if the operation is not successful
     *
     * @supported.api
     */
    public static Response sendAssertionIDRequest(
        AssertionIDRequest assertionIDRequest, String samlAuthorityEntityID,
        String role, String realm, String binding) throws SAML2Exception {

        StringBuffer location = new StringBuffer();
        RoleDescriptorType roled = getRoleDescriptorAndLocation(
            samlAuthorityEntityID, role, realm, binding, location);

        if (binding.equalsIgnoreCase(SAML2Constants.SOAP)) {
            signAssertionIDRequest(assertionIDRequest, realm, false);
            return sendAssertionIDRequestBySOAP(assertionIDRequest,
                location.toString(), realm, samlAuthorityEntityID, role, roled);
        } else {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }
    }

    /**
     * Sends the Assertion ID to specifiied Assertion ID Request Service and
     * returns <code>Assertion</code> coming from the Assertion ID Request
     * Service.
     *
     * @param assertionID the asssertionID</code> object
     * @param samlAuthorityEntityID entity ID of SAML authority
     * @param role SAML authority role, for example,
     * <code>SAML2Constants.ATTR_AUTH_ROLE</code>, 
     * <code>SAML2Constants.AUTHN_AUTH_ROLE</code> or
     * <code>SAML2Constants.IDP_ROLE</code>
     * @param realm the realm of hosted entity
     *
     * @return the <code>Assertion</code> object
     * @exception SAML2Exception if the operation is not successful
     *
     * @supported.api
     */
    public static Assertion sendAssertionIDRequestURI(
        String assertionID, String samlAuthorityEntityID,
        String role, String realm) throws SAML2Exception {

        StringBuffer locationSB = new StringBuffer();
        getRoleDescriptorAndLocation(samlAuthorityEntityID, role, realm,
            SAML2Constants.URI, locationSB);
        if (locationSB.indexOf("?") == -1) {
            locationSB.append("?");
        } else {
            locationSB.append("&");
        }
        locationSB.append("ID=").append(assertionID);
        String location = fillInBasicAuthInfo(locationSB.toString(), realm,
            samlAuthorityEntityID, role); 

        URL url = null;
        try {
            url = new URL(location);
        } catch (MalformedURLException me) {
            throw new SAML2Exception(me.getMessage());
        }

        try {
            HttpURLConnection conn = HttpURLConnectionManager.getConnection(url);
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setDoOutput(false);
            conn.connect();

            int respCode = conn.getResponseCode();
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "AssertionIDRequestUtil.sendAssertionIDRequestURI: " +
                    "Response code = " + respCode + ", Response message = " +
                    conn.getResponseMessage());
            }
            if (respCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            String contentType = conn.getContentType();
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "AssertionIDRequestUtil.sendAssertionIDRequestURI: " +
                    "Content type = " + contentType);
            }
            if ((contentType == null) ||
                (contentType.indexOf(MIME_TYPE_ASSERTION) == -1)) {

                return null;
            }

            int contentLength = conn.getContentLength();
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "AssertionIDRequestUtil.sendAssertionIDRequestURI: " +
                    "Content length = " + contentLength);
            }

            BufferedInputStream bin =
                new BufferedInputStream(conn.getInputStream());
            StringBuffer contentSB = new StringBuffer();
            byte content[] = new byte[2048];

            if (contentLength != -1) {
                int read = 0, totalRead = 0;
                int left;
                while (totalRead < contentLength) {
                    left = contentLength - totalRead;
                    read = bin.read(content, 0,
                        left < content.length ? left : content.length);
                    if (read == -1) {
                        // We need to close connection !!
                        break;
                    } else {
                        if (read > 0) {
                            totalRead += read;
                            contentSB.append(new String(content, 0, read));
                        }
                    }
                }
            } else {
                int numbytes;
                int totalRead = 0;

                while (true) {
                    numbytes = bin.read(content);
                    if (numbytes == -1) {
                        break;
                    }

                    totalRead += numbytes;
                    contentSB.append(new String(content, 0, numbytes));
                }
            }

            return AssertionFactory.getInstance().createAssertion(
                contentSB.toString());
        } catch (IOException ioex) {
            SAML2Utils.debug.error(
                "AssertionIDRequest.sendAssertionIDRequestURI:", ioex);
            throw new SAML2Exception(ioex.getMessage());
        }
    }

    /**
     * Gets assertion ID from URI and returns assertion if found.
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param samlAuthorityEntityID entity ID of SAML authority
     * @param role SAML authority role
     * @param realm the realm of hosted entity
     *
     * @exception IOException if response can't be sent
     */
    public static void processAssertionIDRequestURI(HttpServletRequest request,
        HttpServletResponse response, String samlAuthorityEntityID,
        String role, String realm) throws IOException {

        String assertionID = request.getParameter("ID");
        if (assertionID == null) {
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_BAD_REQUEST, "nullAssertionID",
                SAML2Utils.bundle.getString("nullAssertionID"));
            return;
        }

        AssertionIDRequestMapper aidReqMapper = null;
        try {
            aidReqMapper = getAssertionIDRequestMapper(realm,
                samlAuthorityEntityID, role);
        } catch (SAML2Exception ex) {
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "failedToGetAssertionIDRequestMapper", ex.getMessage());
            return;
        }

        try {
            aidReqMapper.authenticateRequesterURI(request, response,
                samlAuthorityEntityID, role, realm);
        } catch (SAML2Exception ex) {
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_FORBIDDEN,
                "failedToAuthenticateRequesterURI", ex.getMessage());
            return;
        }

        Assertion assertion = (Assertion)IDPCache.assertionByIDCache.get(
            assertionID);

        if ((assertion == null) || (!assertion.isTimeValid())) {
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_NOT_FOUND,
                "invalidAssertionID",
                SAML2Utils.bundle.getString("invalidAssertionID"));
            return;
        }

        response.setContentType(MIME_TYPE_ASSERTION);
        response.addHeader("Cache-Control", "no-cache, no-store");
        response.addHeader("Pragma", "no-cache");

        String content = null;
        try {
            content = assertion.toXMLString(true, true);
        } catch (SAML2Exception ex) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("AssertionIDRequestUtil." +
                "processAssertionIDRequestURI:", ex);
            }
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "invalidAssertion", ex.getMessage());
            return;
        }

        byte[] bytes = null;
        try {
            bytes = content.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ueex) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("AssertionIDRequestUtil." +
                "processAssertionIDRequestURI:", ueex);
            }
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "unsupportedEncoding", ueex.getMessage());
            return;
        }
        response.setContentLength(bytes.length);

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(response.getOutputStream());
            bos.write(bytes, 0, bytes.length);
        } catch (IOException ioex) {
            SAML2Utils.debug.error("AssertionIDRequestUtil." +
                "processAssertionIDRequestURI:", ioex);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ioex) {
                    SAML2Utils.debug.error("AssertionIDRequestUtil." +
                        "processAssertionIDRequestURI:", ioex);
                }
            }
        }
    }

    /**
     * This method processes the <code>AssertionIDRequest</code> coming
     * from a requester.
     *
     * @param assertionIDRequest the <code>AssertionIDRequest</code> object
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param samlAuthorityEntityID entity ID of SAML authority
     * @param role the role of SAML authority
     * @param realm the realm of SAML authority
     * @return the <code>Response</code> object
     * @exception SAML2Exception if the operation is not successful
     */
    public static Response processAssertionIDRequest(
        AssertionIDRequest assertionIDRequest, HttpServletRequest request, 
        HttpServletResponse response, String samlAuthorityEntityID,
        String role, String realm) throws SAML2Exception {

        try {
            verifyAssertionIDRequest(assertionIDRequest, samlAuthorityEntityID,
                role, realm);
        } catch(SAML2Exception se) {
            SAML2Utils.debug.error("AssertionIDRequestUtil." +
                "processAssertionIDRequest:", se);
            return SAML2Utils.getErrorResponse(assertionIDRequest,
                SAML2Constants.REQUESTER, null, se.getMessage(),
                samlAuthorityEntityID);
        }

        Issuer issuer = assertionIDRequest.getIssuer();
        String spEntityID = issuer.getValue();        

        RoleDescriptorType roled = null;
        try {
            if (SAML2Constants.IDP_ROLE.equals(role)) {
                roled = metaManager.getIDPSSODescriptor(realm,
                    samlAuthorityEntityID);
            } else if (SAML2Constants.AUTHN_AUTH_ROLE.equals(role)) {
                roled = metaManager.getAuthnAuthorityDescriptor(realm,
                    samlAuthorityEntityID);
            } else if (SAML2Constants.ATTR_AUTH_ROLE.equals(role)) {
                roled = metaManager.getAttributeAuthorityDescriptor(realm,
                    samlAuthorityEntityID);
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error("AssertionIDRequestUtil." +
                "processAssertionIDRequest:", sme);
            return SAML2Utils.getErrorResponse(assertionIDRequest,
                SAML2Constants.RESPONDER, null, sme.getMessage(),
                samlAuthorityEntityID);
        }

        if (roled == null) {
            return SAML2Utils.getErrorResponse(assertionIDRequest,
                SAML2Constants.REQUESTER, null, SAML2Utils.bundle.getString(
                "samlAuthorityNotFound"), samlAuthorityEntityID);
        }

        List returnAssertions = null;
        List assertionIDRefs = assertionIDRequest.getAssertionIDRefs();
        for(Iterator iter = assertionIDRefs.iterator(); iter.hasNext();) {
            AssertionIDRef assertionIDRef = (AssertionIDRef)iter.next();
            String assertionID = assertionIDRef.getValue();

            Assertion assertion = (Assertion)IDPCache.assertionByIDCache.get(
                assertionID);
            if ((assertion == null) && (SAML2FailoverUtils.isSAML2FailoverEnabled())) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("AssertionIDRequestUtil.processAssertionIDRequest: " +
                        "reading assertion from the SAML2 Token Repository using assertionID:" + assertionID);
                }
                String assertionStr = null;
                try {
                    assertionStr = (String) SAML2FailoverUtils.retrieveSAML2Token(assertionID);
                } catch (SAML2TokenRepositoryException se) {
                    SAML2Utils.debug.error("AssertionIDRequestUtil.processAssertionIDRequest: " +
                         "There was a problem reading assertion from the SAML2 Token Repository using assertionID:"
                            + assertionID, se);
                }
                if (assertionStr != null) {
                    assertion = AssertionFactory.getInstance().createAssertion(
                        assertionStr);
                }
            }

            if ((assertion != null) && (assertion.isTimeValid())) {
                if (returnAssertions == null) {
                    returnAssertions = new ArrayList();
                }
                returnAssertions.add(assertion);
            }
        }        

        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
        Response samlResp = protocolFactory.createResponse();
        samlResp.setAssertion(returnAssertions);

        samlResp.setID(SAML2Utils.generateID());
        samlResp.setInResponseTo(assertionIDRequest.getID());

        samlResp.setVersion(SAML2Constants.VERSION_2_0);
        samlResp.setIssueInstant(new Date());
    
        Status status = protocolFactory.createStatus();
        StatusCode statusCode = protocolFactory.createStatusCode();
        statusCode.setValue(SAML2Constants.SUCCESS);
        status.setStatusCode(statusCode);
        samlResp.setStatus(status);

        Issuer respIssuer = AssertionFactory.getInstance().createIssuer();
        respIssuer.setValue(samlAuthorityEntityID);
        samlResp.setIssuer(respIssuer);

        signResponse(samlResp, samlAuthorityEntityID, role, realm, false);

        return samlResp;
    }

    private static RoleDescriptorType getRoleDescriptorAndLocation(
        String samlAuthorityEntityID, String role, String realm,
        String binding, StringBuffer location) throws SAML2Exception {

        List aIDReqServices = null;
        RoleDescriptorType roled = null;
        try {
            if (role == null) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "unsupportedRole"));
            } else if (role.equals(SAML2Constants.IDP_ROLE)) {
                IDPSSODescriptorElement idpd =
                    metaManager.getIDPSSODescriptor(realm,
                    samlAuthorityEntityID);
                if (idpd == null) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "idpNotFound"));
                }
                aIDReqServices = idpd.getAssertionIDRequestService();
                roled = idpd;
            } else if (role.equals(SAML2Constants.AUTHN_AUTH_ROLE)) {
                AuthnAuthorityDescriptorElement attrd =
                    metaManager.getAuthnAuthorityDescriptor(realm,
                    samlAuthorityEntityID);
                if (attrd == null) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "authnAuthorityNotFound"));
                }
                aIDReqServices = attrd.getAssertionIDRequestService();
                roled = attrd;
            } else if (role.equals(SAML2Constants.ATTR_AUTH_ROLE)) {
                AttributeAuthorityDescriptorElement aad =
                    metaManager.getAttributeAuthorityDescriptor(realm,
                    samlAuthorityEntityID);
                if (aad == null) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "attrAuthorityNotFound"));
                }
                aIDReqServices = aad.getAssertionIDRequestService();
                roled = aad;
            } else {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "unsupportedRole"));
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(
                "AssertionIDRequest.getRoleDescriptorAndLocation:", sme);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "metaDataError"));
        }

        if (binding == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }

        if ((aIDReqServices == null) || (aIDReqServices.isEmpty())) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("aIDReqServiceNotFound"));
        }

        for(Iterator iter = aIDReqServices.iterator(); iter.hasNext(); ) {
            AssertionIDRequestServiceElement aIDReqService =
                (AssertionIDRequestServiceElement)iter.next();
            if (binding.equalsIgnoreCase(aIDReqService.getBinding())) {
                location.append(aIDReqService.getLocation());
                break;
            }
        }
        if (location.length() == 0) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }

        return roled;
    }

    private static void signAssertionIDRequest(
        AssertionIDRequest assertionIDRequest,
        String realm, boolean includeCert) throws SAML2Exception {

        String spEntityID = assertionIDRequest.getIssuer().getValue();
        
        String alias = SAML2Utils.getSigningCertAlias(realm, spEntityID,
            SAML2Constants.SP_ROLE);

        PrivateKey signingKey = keyProvider.getPrivateKey(alias);
        X509Certificate signingCert = null;
        if (includeCert) {
            signingCert = keyProvider.getX509Certificate(alias);
        }
        
        if (signingKey != null) {
            assertionIDRequest.sign(signingKey, signingCert);
        }
    }

    private static void verifyAssertionIDRequest(
        AssertionIDRequest assertionIDRequest, String samlAuthorityEntityID,
        String role, String realm) throws SAML2Exception {

        Issuer issuer = assertionIDRequest.getIssuer();
        String requestedEntityID = issuer.getValue();

        if (!SAML2Utils.isSourceSiteValid(issuer, realm,
            samlAuthorityEntityID)) {

            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "assertionIDRequestIssuerInvalid"));
        }

        SPSSODescriptorElement spSSODesc = metaManager.getSPSSODescriptor(
            realm, requestedEntityID);
        if (spSSODesc == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "assertionIDRequestIssuerNotFound"));
        }

        X509Certificate signingCert = KeyUtil.getVerificationCert(spSSODesc,
            requestedEntityID, SAML2Constants.SP_ROLE);

        if (signingCert != null) {
            boolean valid = assertionIDRequest.isSignatureValid(signingCert);
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "AssertionIDRequestUtil.verifyAssertionIDRequest: " +
                    "Signature validity is : " + valid);
            }
            if (!valid) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "invalidSignatureAssertionIDRequest"));
            }
        } else {
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
    }

    private static void signResponse(Response response,
        String samlAuthorityEntityID, String role, String realm,
        boolean includeCert) throws SAML2Exception {
        
        String alias = SAML2Utils.getSigningCertAlias(realm, samlAuthorityEntityID, role);

        String encryptedKeyPass = SAML2Utils.getSigningCertEncryptedKeyPass(realm, samlAuthorityEntityID, role);
        PrivateKey signingKey;
        if (encryptedKeyPass == null  || encryptedKeyPass.isEmpty()) {
            signingKey = keyProvider.getPrivateKey(alias);
        } else {
            signingKey = keyProvider.getPrivateKey(alias, encryptedKeyPass);
        }
        X509Certificate signingCert = null;
        if (includeCert) {
            signingCert = keyProvider.getX509Certificate(alias);
        }
        
        if (signingKey != null) {
            response.sign(signingKey, signingCert);
        }
    }

    private static String fillInBasicAuthInfo(String location, String realm,
        String samlAuthorityEntityID, String role) {

        BaseConfigType config = null;
        try {
            if (role.equals(SAML2Constants.IDP_ROLE)) {
                config = metaManager.getIDPSSOConfig(realm,
                    samlAuthorityEntityID);
            } else if (role.equals(SAML2Constants.AUTHN_AUTH_ROLE)) {
                config = metaManager.getAuthnAuthorityConfig(realm,
                    samlAuthorityEntityID);
            } else if (role.equals(SAML2Constants.ATTR_AUTH_ROLE)) {
                config = metaManager.getAttributeAuthorityConfig(realm,
                    samlAuthorityEntityID);
            }
        } catch (SAML2MetaException sme) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "AssertionIDRequestUtil.getSSOConfig:", sme);
            }
        }

        return SAML2Utils.fillInBasicAuthInfo(config, location); 
    }

    private static Response sendAssertionIDRequestBySOAP(
        AssertionIDRequest assertionIDRequest, String location, String realm,
        String samlAuthorityEntityID, String role, RoleDescriptorType roled)
        throws SAML2Exception {

        String aIDReqStr = assertionIDRequest.toXMLString(true, true);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "AssertionIDRequestUtil.sendAssertionIDRequestBySOAP: " +
                "assertionIDRequest = " + aIDReqStr);
            SAML2Utils.debug.message(
                "AssertionIDRequestUtil.sendAssertionIDRequestBySOAP: " +
                "location = " + location);
        }

        location = fillInBasicAuthInfo(location, realm, samlAuthorityEntityID,
            role); 

        SOAPMessage resMsg = null;
        try {
            resMsg = SAML2Utils.sendSOAPMessage(aIDReqStr, location, true);
        } catch (SOAPException se) {
            SAML2Utils.debug.error(
                "AssertionIDRequestUtil.sendAssertionIDRequestBySOAP:", se);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("errorSendingAssertionIDRequest"));
        }
        
        Element respElem = SAML2Utils.getSamlpElement(resMsg, "Response");
        Response response =
            ProtocolFactory.getInstance().createResponse(respElem);
        
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "AssertionIDRequestUtil.sendAssertionIDRequestBySOAP: " +
                "response = " + response.toXMLString(true, true));
        }

        verifyResponse(response, assertionIDRequest, samlAuthorityEntityID,
            role, roled);

        return response;
    }

    private static void verifyResponse(Response response,
        AssertionIDRequest assertionIDRequest, String samlAuthorityEntityID,
        String role, RoleDescriptorType roled) throws SAML2Exception {

        String aIDReqID = assertionIDRequest.getID();
        if ((aIDReqID != null) &&
            (!aIDReqID.equals(response.getInResponseTo()))) {

            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "invalidInResponseToAssertionIDRequest"));
        }

        Issuer respIssuer = response.getIssuer();
        if (respIssuer == null) {
            return;
        }

        if (!samlAuthorityEntityID.equals(respIssuer.getValue())) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "responseIssuerMismatch"));
        }


        X509Certificate signingCert = KeyUtil.getVerificationCert(roled,
            samlAuthorityEntityID, role);

        if (signingCert != null) {
            boolean valid = response.isSignatureValid(signingCert);
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "AssertionIDRequestUtil .verifyResponse: " +
                    "Signature validity is : " + valid);
            }
            if (!valid) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "invalidSignatureOnResponse"));
            }
        } else {
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }

    }

    private static AssertionIDRequestMapper getAssertionIDRequestMapper(
        String realm, String samlAuthorityEntityID, String role)
        throws SAML2Exception {

        String aidReqMapperName = null;
        AssertionIDRequestMapper aidReqMapper = null;
        try {
            aidReqMapperName = SAML2Utils.getAttributeValueFromSSOConfig(realm,
                samlAuthorityEntityID, role,
                SAML2Constants.ASSERTION_ID_REQUEST_MAPPER);

            if (aidReqMapperName == null) {
                aidReqMapperName = 
                    SAML2Constants.DEFAULT_ASSERTION_ID_REQUEST_MAPPER_CLASS;
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                        "AssertionIDRequestUtil.getAssertionIDRequestMapper:" +
                        " use "+ aidReqMapperName);
                }
            }
            aidReqMapper = (AssertionIDRequestMapper)
                assertionIDRequestMapperCache.get(aidReqMapperName);
            if (aidReqMapper == null) {
                aidReqMapper = (AssertionIDRequestMapper)
                    Class.forName(aidReqMapperName).newInstance();
                assertionIDRequestMapperCache.put(aidReqMapperName,
                    aidReqMapper);
            } else {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                        "AssertionIDRequestUtil.getAssertionIDRequestMapper:" +
                        " got the AssertionIDRequestMapper from cache");
                }
            }
        } catch (Exception ex) {
            SAML2Utils.debug.error(
                "AssertionIDRequestUtil.getAssertionIDRequestMapper:", ex);
            throw new SAML2Exception(ex);
        }

        return aidReqMapper;
    }

}
