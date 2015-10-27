/*
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
 * $Id: AuthnQueryUtil.java,v 1.8 2008/12/03 00:32:31 hengming Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.w3c.dom.Element;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.AuthnAuthorityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AuthnAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.AuthnQueryServiceElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.protocol.AuthnQuery;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;

/**
 * This class provides methods to send or process <code>AuthnQuery</code>.
 *
 * @supported.api
 */

public class AuthnQueryUtil {

    static KeyProvider keyProvider = KeyUtil.getKeyProviderInstance(); 
    static SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();

    private AuthnQueryUtil() {
    }

    /**
     * This method sends the <code>AuthnQuery</code> to specifiied
     * authentication authority and returns <code>Response</code> coming
     * from the authentication authority.
     *
     * @param authnQuery the <code>AuthnQuery</code> object
     * @param authnAuthorityEntityID entity ID of authentication authority
     * @param realm the realm of hosted entity
     * @param binding the binding
     *
     * @return the <code>Response</code> object
     * @exception SAML2Exception if the operation is not successful
     *
     * @supported.api
     */
    public static Response sendAuthnQuery(AuthnQuery authnQuery,
        String authnAuthorityEntityID, String realm, String binding)
        throws SAML2Exception {

        SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
        AuthnAuthorityDescriptorElement aad = null;
        try {
            aad = metaManager.getAuthnAuthorityDescriptor(realm,
                authnAuthorityEntityID);
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error("AttributeService.sendAuthnQuery:",
                sme);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        }

        if (aad == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("authnAuthorityNotFound"));
        }

        if (binding == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }

        String location = null;
        List authnService = aad.getAuthnQueryService();
        for(Iterator iter = authnService.iterator(); iter.hasNext(); ) {
            AuthnQueryServiceElement authnService1 =
                (AuthnQueryServiceElement)iter.next();
            if (binding.equalsIgnoreCase(authnService1.getBinding())) {
                location = authnService1.getLocation();
                break;
            }
        }
        if (location == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }                

        if (binding.equalsIgnoreCase(SAML2Constants.SOAP)) {
            signAuthnQuery(authnQuery, realm, false);
            return sendAuthnQuerySOAP(authnQuery, location,
               authnAuthorityEntityID, realm, aad);
        } else {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }
    }

    /**
     * This method processes the <code>AuthnQuery</code> coming
     * from a requester.
     *
     * @param authnQuery the <code>AuthnQuery</code> object
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param authnAuthorityEntityID entity ID of authentication authority
     * @param realm the realm of hosted entity
     *
     * @return the <code>Response</code> object
     * @exception SAML2Exception if the operation is not successful
     */
    public static Response processAuthnQuery(AuthnQuery authnQuery,
        HttpServletRequest request, HttpServletResponse response,
        String authnAuthorityEntityID, String realm) throws SAML2Exception {

        try {
            verifyAuthnQuery(authnQuery, authnAuthorityEntityID, realm);
        } catch(SAML2Exception se) {
            SAML2Utils.debug.error("AuthnQueryUtil.processAuthnQuery:", se);
            return SAML2Utils.getErrorResponse(authnQuery,
                SAML2Constants.REQUESTER, null, se.getMessage(), null);
        }

        Issuer issuer = authnQuery.getIssuer();
        String spEntityID = issuer.getValue();        
        AuthnAuthorityDescriptorElement aad = null;
        SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
        try {
            aad = metaManager.getAuthnAuthorityDescriptor(realm,
                authnAuthorityEntityID);
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error("AuthnQueryUtil.processAuthnQuery:", sme);
            return SAML2Utils.getErrorResponse(authnQuery,
                SAML2Constants.RESPONDER, null,
                SAML2Utils.bundle.getString("metaDataError"), null);
        } 

        if (aad == null) {
            return SAML2Utils.getErrorResponse(authnQuery,
                SAML2Constants.REQUESTER, null,
                SAML2Utils.bundle.getString("authnAuthorityNotFound"), null);
        }

        NameID nameID = getNameID(authnQuery.getSubject(), realm,
            authnAuthorityEntityID);

        if (nameID == null) {
            return SAML2Utils.getErrorResponse(authnQuery,
                SAML2Constants.REQUESTER, SAML2Constants.UNKNOWN_PRINCIPAL,
                null, null);
        }

        IDPAccountMapper idpAcctMapper = SAML2Utils.getIDPAccountMapper(
            realm, authnAuthorityEntityID);

        String userID = idpAcctMapper.getIdentity(nameID,
            authnAuthorityEntityID, spEntityID, realm);

        if (userID == null) {
            return SAML2Utils.getErrorResponse(authnQuery,
                SAML2Constants.REQUESTER, SAML2Constants.UNKNOWN_PRINCIPAL,
                null, null);
        }

        IDPAuthnContextMapper idpAuthnContextMapper =
            IDPSSOUtil.getIDPAuthnContextMapper(realm, authnAuthorityEntityID);

        // get assertion for matching authncontext using session
        List returnAssertions = new ArrayList();
        String qSessionIndex = authnQuery.getSessionIndex();
        RequestedAuthnContext requestedAC =
            authnQuery.getRequestedAuthnContext();

        List assertions = null;
        String cacheKey = userID.toLowerCase();
        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("AuthnQueryUtil.processAuthnQuery: " +
                    "getting user assertions from DB. user = " + cacheKey);
            }
            List list = null;
            try {
                list = SAML2FailoverUtils.retrieveSAML2TokensWithSecondaryKey(cacheKey);
            } catch(SAML2TokenRepositoryException se) {
                SAML2Utils.debug.error("AuthnQueryUtil.processAuthnQuery: " +
                        "Unable to obtain user assertions from CTS Repository. user = " + cacheKey, se);
            }
            if (list != null && !list.isEmpty()) {
                assertions = new ArrayList();
                for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                    String assertionStr = (String)iter.next();
                    assertions.add(assertionFactory.createAssertion(
                        assertionStr));
                }
            }
        } else {
            assertions = (List)IDPCache.assertionCache.get(cacheKey);
        }

        if ((assertions != null) && (!assertions.isEmpty())) {

            synchronized (assertions) {
                for(Iterator aIter = assertions.iterator(); aIter.hasNext();) {
                    Assertion assertion = (Assertion)aIter.next();

                    if (!assertion.isTimeValid()) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(
                                "AuthnQueryUtil.processAuthnQuery: "  +
                                " assertion " + assertion.getID() +
                                " expired.");
                        }
                        continue;
                    }

                    List authnStmts = assertion.getAuthnStatements();

                    for(Iterator asIter = authnStmts.iterator();
                        asIter.hasNext();){

                        AuthnStatement authnStmt =
                            (AuthnStatement)asIter.next();
                        AuthnContext authnStmtAC = authnStmt.getAuthnContext();
                        String sessionIndex = authnStmt.getSessionIndex();

                        String authnStmtACClassRef =
                            authnStmtAC.getAuthnContextClassRef();
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(
                                "AuthnQueryUtil.processAuthnQuery: " +
                                "authnStmtACClassRef is " +
                                authnStmtACClassRef + ", sessionIndex = " +
                                sessionIndex);
                        }

                        if ((qSessionIndex != null) &&
                            (qSessionIndex.length() != 0) &&
                            (!qSessionIndex.equals(sessionIndex))) {
                            continue;
                        }

                        if (requestedAC != null) {
                            List requestedACClassRefs =
                                requestedAC.getAuthnContextClassRef();
                            String comparison =  requestedAC.getComparison();
 
                            if (idpAuthnContextMapper.isAuthnContextMatching(
                                requestedACClassRefs, authnStmtACClassRef,
                                comparison, realm, authnAuthorityEntityID)) {

                                returnAssertions.add(assertion);
                                break;
                            }
                        } else {
                            returnAssertions.add(assertion);
                            break;
                        }
                    }
                }
            } // end assertion iterator while.
        }

        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
        Response samlResp = protocolFactory.createResponse();
        if (!returnAssertions.isEmpty()) {
            samlResp.setAssertion(returnAssertions);
        }
        samlResp.setID(SAML2Utils.generateID());
        samlResp.setInResponseTo(authnQuery.getID());

        samlResp.setVersion(SAML2Constants.VERSION_2_0);
        samlResp.setIssueInstant(new Date());
    
        Status status = protocolFactory.createStatus();
        StatusCode statusCode = protocolFactory.createStatusCode();
        statusCode.setValue(SAML2Constants.SUCCESS);
        status.setStatusCode(statusCode);
        samlResp.setStatus(status);

        Issuer respIssuer = assertionFactory.createIssuer();
        respIssuer.setValue(authnAuthorityEntityID);
        samlResp.setIssuer(respIssuer);

        signResponse(samlResp, authnAuthorityEntityID, realm, false);

        return samlResp;
    }

    private static void signAuthnQuery(AuthnQuery authnQuery, String realm,
        boolean includeCert) throws SAML2Exception {

        String spEntityID = authnQuery.getIssuer().getValue();
        
        String alias = SAML2Utils.getSigningCertAlias(realm, spEntityID,
            SAML2Constants.SP_ROLE);

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
            authnQuery.sign(signingKey, signingCert);
        }
    }

    private static void verifyAuthnQuery(AuthnQuery authnQuery,
        String authnAuthorityEntityID, String realm) throws SAML2Exception {

        if (!authnQuery.isSigned()) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "authnQueryNotSigned"));
        }

        Issuer issuer = authnQuery.getIssuer();
        String spEntityID = issuer.getValue();

        if (!SAML2Utils.isSourceSiteValid(issuer, realm,
            authnAuthorityEntityID)) {

            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "authnQueryIssuerInvalid"));
        }
        SPSSODescriptorElement spSSODesc = SAML2Utils.getSAML2MetaManager()
            .getSPSSODescriptor(realm, spEntityID);
        if (spSSODesc == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "authnQueryIssuerNotFound"));
        }
        Set<X509Certificate> signingCerts = KeyUtil.getVerificationCerts(spSSODesc, spEntityID, SAML2Constants.SP_ROLE);

        if (!signingCerts.isEmpty()) {
            boolean valid = authnQuery.isSignatureValid(signingCerts);
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "AuthnQueryUtil.verifyAuthnQuery: " +
                    "Signature validity is : " + valid);
            }
            if (!valid) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "invalidSignatureAuthnQuery"));
            }
        } else {
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
    }

    private static void signResponse(Response response,
        String authnAuthorityEntityID, String realm, boolean includeCert)
        throws SAML2Exception {
        
        String alias = SAML2Utils.getSigningCertAlias(realm,
            authnAuthorityEntityID, SAML2Constants.AUTHN_AUTH_ROLE);

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
            response.sign(signingKey, signingCert);
        }
    }

    private static Response sendAuthnQuerySOAP(AuthnQuery authnQuery,
        String authnServiceURL, String authnAuthorityEntityID, String realm,
        AuthnAuthorityDescriptorElement aad) throws SAML2Exception {

        String authnQueryXMLString = authnQuery.toXMLString(true, true);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("AuthnQueryUtil.sendAuthnQuerySOAP: " +
                "authnQueryXMLString = " + authnQueryXMLString);
            SAML2Utils.debug.message("AuthnQueryUtil.sendAuthnQuerySOAP: " +
                "authnServiceURL= " + authnServiceURL);
        }

        AuthnAuthorityConfigElement config =
            metaManager.getAuthnAuthorityConfig(realm, authnAuthorityEntityID);
        authnServiceURL = SAML2Utils.fillInBasicAuthInfo(config,
            authnServiceURL);
        
        SOAPMessage resMsg = null;
        try {
            resMsg = SOAPCommunicator.getInstance().sendSOAPMessage(authnQueryXMLString,
                    authnServiceURL, true);
        } catch (SOAPException se) {
            SAML2Utils.debug.error(
                "AuthnQueryUtil.sendAuthnQuerySOAP: ", se);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("errorSendingAuthnQuery"));
        }
        
        Element respElem = SOAPCommunicator.getInstance().getSamlpElement(resMsg, "Response");
        Response response =
            ProtocolFactory.getInstance().createResponse(respElem);
        
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("AuthnQueryUtil.sendAuthnQuerySOAP: " +
                "response = " + response.toXMLString(true, true));
        }

        verifyResponse(response, authnQuery, authnAuthorityEntityID, realm,
            aad);

        return response;
    }

    private static void verifyResponse(Response response,
        AuthnQuery authnQuery, String authnAuthorityEntityID, String realm,
        AuthnAuthorityDescriptorElement aad) throws SAML2Exception {

        String authnQueryID = authnQuery.getID();
        if ((authnQueryID != null) &&
            (!authnQueryID.equals(response.getInResponseTo()))) {

            throw new SAML2Exception(
                SAML2Utils.bundle.getString("invalidInResponseToAuthnQuery"));
        }

        Issuer respIssuer = response.getIssuer();
        if (respIssuer == null) {
            return;
        }

        if (!authnAuthorityEntityID.equals(respIssuer.getValue())) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "responseIssuerMismatch"));
        }

        if (!response.isSigned()) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "responseNotSigned"));
        }

        Set<X509Certificate> signingCerts = KeyUtil.getVerificationCerts(aad, authnAuthorityEntityID,
                SAML2Constants.AUTHN_AUTH_ROLE);

        if (signingCerts.isEmpty()) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
        boolean valid = response.isSignatureValid(signingCerts);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("AuthnQueryUtil.verifyResponse: " +
                "Signature validity is : " + valid);
        }
        if (!valid) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "invalidSignatureOnResponse"));
        }

        String spEntityID = authnQuery.getIssuer().getValue();

        List<Assertion> assertions = response.getAssertion();
        if (assertions == null) {
            List<EncryptedAssertion> encAssertions = response.getEncryptedAssertion();
            if (encAssertions != null && !encAssertions.isEmpty()) {
                Set<PrivateKey> privateKeys = KeyUtil.getDecryptionKeys(realm, spEntityID, SAML2Constants.SP_ROLE);
                for (EncryptedAssertion eAssertion : encAssertions) {
                    Assertion assertion = eAssertion.decrypt(privateKeys);
                    if (assertions == null) {
                        assertions = new ArrayList<>();
                    }
                    assertions.add(assertion);
                }
            }
        }

        if ((assertions == null) || (assertions.isEmpty())) {
            return;
        }

        signingCerts = KeyUtil.getVerificationCerts(aad, authnAuthorityEntityID, SAML2Constants.IDP_ROLE);

        for(Iterator iter = assertions.iterator(); iter.hasNext(); ) {
            Assertion assertion = (Assertion)iter.next();
            if (assertion.isSigned()) {

                if (signingCerts.isEmpty()) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString("missingSigningCertAlias"));
                }

                valid = assertion.isSignatureValid(signingCerts);
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                        "AuthnQueryUtil.verifyResponse: " +
                        "Signature validity is : " + valid);
                }
                if (!valid) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "invalidSignatureOnAssertion"));
                }
            }
        }
    }

    private static NameID getNameID(Subject subject, String realm, String authnAuthorityEntityID) {
        NameID nameID = subject.getNameID();
        if (nameID == null) {
            EncryptedID encryptedID = subject.getEncryptedID();
            try {
                nameID = encryptedID.decrypt(KeyUtil.getDecryptionKeys(realm, authnAuthorityEntityID,
                        SAML2Constants.AUTHN_AUTH_ROLE));
            } catch (SAML2Exception ex) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("AuthnQueryUtil.getNameID:", ex);
                }
                return null;
            }
        }

        if (!SAML2Utils.isPersistentNameID(nameID)) {
            return null;
        }

        return nameID;
    }
}
