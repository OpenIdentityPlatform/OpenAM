/*
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
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */
package com.sun.identity.saml2.common;

import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.common.SystemConfigurationException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.idpdiscovery.IDPDiscoveryConstants;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLAuthzDecisionQueryConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLPDPConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.DefaultSPAuthnContextMapper;
import com.sun.identity.saml2.plugins.FedletAdapter;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.plugins.SAML2IDPFinder;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.plugins.SPAccountMapper;
import com.sun.identity.saml2.plugins.SPAttributeMapper;
import com.sun.identity.saml2.plugins.SPAuthnContextMapper;
import com.sun.identity.saml2.profile.AuthnRequestInfo;
import com.sun.identity.saml2.profile.AuthnRequestInfoCopy;
import com.sun.identity.saml2.profile.CacheCleanUpScheduler;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;
import com.sun.identity.security.cert.CRLValidator;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.SAML2Store;
import org.forgerock.openam.saml2.plugins.ValidRelayStateExtractor;
import org.forgerock.openam.saml2.plugins.ValidRelayStateExtractor.SAMLEntityInfo;
import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;
import org.owasp.esapi.ESAPI;

/**
 * The <code>SAML2Utils</code> contains utility methods for SAML 2.0
 * implementation.
 */
public class SAML2Utils extends SAML2SDKUtils {


    private static SAML2MetaManager saml2MetaManager = null;
    private static CircleOfTrustManager cotManager = null;
    private static KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();

    private static String serverProtocol =
            SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL);
    private static String serverHost =
            SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
    private static String serverPort =
            SystemPropertiesManager.get(Constants.AM_SERVER_PORT);
    private static String serverUri = SystemPropertiesManager.get(
            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    private static String sessionCookieName = SystemPropertiesManager.get(
            Constants.AM_COOKIE_NAME);
    private static int intServerPort = 0;
    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";
    private static final String LOCATION = "Location";
    private static final char EQUALS = '=';
    private static final char SEMI_COLON = ';';
    private static final char DOUBLE_QUOTE = '"';

    private static String bufferLen = (String)
            ((SAML2ConfigService.getAttribute(SAML2ConfigService.SAML2_BUFFER_LENGTH) == null)
                    ? "8192" : SAML2ConfigService.
                    getAttribute(SAML2ConfigService.SAML2_BUFFER_LENGTH));

    // Dir server info for CRL entry
    private static boolean checkCertStatus = false;
    private static boolean checkCAStatus = false;
    private static final RedirectUrlValidator<SAMLEntityInfo> RELAY_STATE_VALIDATOR =
            new RedirectUrlValidator<SAMLEntityInfo>(new ValidRelayStateExtractor());

    static {
        if (StringUtils.isBlank(serverPort)) {
                serverPort = String.valueOf(SAML2Constants.DEFAULT_SERVER_PORT);
                intServerPort = SAML2Constants.DEFAULT_SERVER_PORT;
        } else {
            try {intServerPort = Integer.parseInt(serverPort);
            } catch (NumberFormatException nfe) {
                debug.error("Unable to parse port " + serverPort, nfe);
                intServerPort = SAML2Constants.DEFAULT_SERVER_PORT;
            }
        }

        /*
         * Setup the LDAP certificate directory service context for
         * use in verification of signing certificates.
         */
        String checkCertStatusStr = SystemConfigurationUtil.getProperty(
                SAML2Constants.CHECK_SAML2_CERTIFICATE_STATUS, null);

        if (checkCertStatusStr != null) {
            checkCertStatus =
                    Boolean.valueOf(checkCertStatusStr).booleanValue();
            checkCAStatus =
                    Boolean.valueOf(SystemConfigurationUtil.getProperty(
                            SAML2Constants.CHECK_SAML2_CA_STATUS, "false"))
                            .booleanValue();
            if (debug.messageEnabled()) {
                debug.message("SAML2 :  CRL check is configured to "
                        + checkCertStatus);
                debug.message("SAML2 :  CRL check for CA is configured to "
                        + checkCAStatus);
            }
        } else {
            checkCertStatus = CRLValidator.isCRLCheckEnabled();
            if (debug.messageEnabled()) {
                debug.message("SAML2 : CRL check is configured " +
                        "with old config style.");
            }
        }
    }


    static {
        try {
            saml2MetaManager =
                    new SAML2MetaManager();
        } catch (SAML2MetaException sme) {
            debug.error("Error retreiving metadata", sme);
        }

        try {
            cotManager = new CircleOfTrustManager();
        } catch (COTException sme) {
            debug.error("Error retreiving COT ", sme);
        }
        // run the scheduler in case of server or Fedlet
        if (SystemConfigurationUtil.isServerMode() ||
                SPCache.isFedlet) {
            CacheCleanUpScheduler.doSchedule();
        }
    }

    private static final AssertionFactory assertionFactory = AssertionFactory.getInstance();
    private static final SecureRandom randomGenerator = new SecureRandom();

    /**
     * Verifies single sign on <code>Response</code> and returns information
     * to SAML2 auth module for further processing. This method is used by
     * SAML2 auth module only.
     *
     * @param httpRequest    HttpServletRequest
     * @param httpResponse   HttpServletResponse
     * @param response       Single Sign On <code>Response</code>.
     * @param orgName        name of the realm or organization the provider is in.
     * @param hostEntityId   Entity ID of the hosted provider.
     * @param profileBinding Profile binding used.
     * @return A Map of information extracted from the Response. The keys of
     * map are:
     * <code>SAML2Constants.SUBJECT</code>,
     * <code>SAML2Constants.POST_ASSERTION</code>,
     * <code>SAML2Constants.ASSERTIONS</code>,
     * <code>SAML2Constants.SESSION_INDEX</code>,
     * <code>SAML2Constants.AUTH_LEVEL</code>,
     * <code>SAML2Constants.MAX_SESSION_TIME</code>.
     * @throws SAML2Exception if the Response is not valid according to the
     *                        processing rules.
     */
    public static Map verifyResponse(
            final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse,
            final Response response,
            final String orgName,
            final String hostEntityId,
            final String profileBinding)
            throws SAML2Exception {
        final String method = "SAML2Utils.verifyResponse:";
        if (response == null || orgName == null || orgName.length() == 0) {
            if (debug.messageEnabled()) {
                debug.message(method + "response or orgName is null.");
            }
            throw new SAML2Exception(bundle.getString("nullInput"));
        }

        String respID = response.getID();
        AuthnRequestInfo reqInfo = null;
        String inRespToResp = response.getInResponseTo();
        if (inRespToResp != null && inRespToResp.length() != 0) {
            reqInfo = (AuthnRequestInfo) SPCache.requestHash.get(inRespToResp);
            if (reqInfo == null) {
                if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                    // Attempt to read AuthnRequestInfoCopy from SAML2 repository
                    AuthnRequestInfoCopy reqInfoCopy = null;
                    try {
                        reqInfoCopy = (AuthnRequestInfoCopy) SAML2FailoverUtils.retrieveSAML2Token(inRespToResp);
                    } catch (SAML2TokenRepositoryException se) {
                        debug.error(method + "AuthnRequestInfoCopy"
                                + " unable to retrieve from SAML2 repository for inResponseTo: " + inRespToResp);
                    }

                    if (reqInfoCopy != null) {
                        // Get back the AuthnRequestInfo
                        reqInfo = reqInfoCopy.getAuthnRequestInfo(httpRequest, httpResponse);
                        if (debug.messageEnabled()) {
                            debug.message(method + "AuthnRequestInfoCopy"
                                    + " retrieved from SAML2 repository for inResponseTo: " + inRespToResp);
                        }
                    } else {
                        debug.error(method + "InResponseTo attribute in Response"
                                + " is invalid: " + inRespToResp + ", SAML2 failover is enabled");
                        String[] data = {respID};
                        LogUtil.error(Level.INFO,
                                LogUtil.INVALID_INRESPONSETO_RESPONSE,
                                data,
                                null);
                        throw new SAML2Exception(bundle.getString(
                                "invalidInResponseToInResponse"));
                    }
                } else {
                    AuthnRequestInfoCopy reqInfoCopy =
                            (AuthnRequestInfoCopy) SAML2Store.getTokenFromStore(inRespToResp);

                    if (reqInfoCopy != null) {
                        // Get back the AuthnRequestInfo
                        reqInfo = reqInfoCopy.getAuthnRequestInfo(httpRequest, httpResponse);
                        if (debug.messageEnabled()) {
                            debug.message(method + "AuthnRequestInfoCopy"
                                    + " retrieved from SAML2 repository for inResponseTo: " + inRespToResp);
                        }
                    } else {
                        debug.error(method + "InResponseTo attribute in Response"
                                + " is invalid: " + inRespToResp + ", SAML2 failover is enabled");
                        String[] data = {respID};
                        LogUtil.error(Level.INFO, LogUtil.INVALID_INRESPONSETO_RESPONSE, data, null);
                        throw new SAML2Exception(bundle.getString("invalidInResponseToInResponse"));
                    }
                }
            }
        }
        // reqInfo can remain null and will do for IDP initiated SSO requests

        // invoke SP Adapter
        SAML2ServiceProviderAdapter spAdapter =
                SAML2Utils.getSPAdapterClass(hostEntityId, orgName);
        if (spAdapter != null) {
            AuthnRequest authnRequest = null;
            if (reqInfo != null) {
                authnRequest = reqInfo.getAuthnRequest();
            }
            spAdapter.preSingleSignOnProcess(hostEntityId, orgName, httpRequest,
                    httpResponse, authnRequest, response, profileBinding);
        }

        String idpEntityId = null;
        Issuer respIssuer = response.getIssuer();
        if (respIssuer != null) { // optional
            if (!isSourceSiteValid(respIssuer, orgName, hostEntityId)) {
                if (debug.messageEnabled()) {
                    debug.message(method + "Issuer in Response is not valid.");
                }
                String[] data = {hostEntityId, orgName, respID};
                LogUtil.error(Level.INFO,
                        LogUtil.INVALID_ISSUER_RESPONSE,
                        data,
                        null);
                throw new SAML2Exception(bundle.getString(
                        "invalidIssuerInResponse"));
            } else {
                idpEntityId = respIssuer.getValue();
            }
        }

        Status status = response.getStatus();
        if (status == null || !status.getStatusCode().getValue().equals(SAML2Constants.SUCCESS)) {
            String statusCode = (status == null) ? "" : status.getStatusCode().getValue();
            if (debug.messageEnabled()) {
                debug.message(method + "Response's status code is not success: " + statusCode);
            }
            String[] data = {respID, ""};
            if (LogUtil.isErrorLoggable(Level.FINE)) {
                data[1] = statusCode;
            }
            LogUtil.error(Level.INFO,
                    LogUtil.WRONG_STATUS_CODE,
                    data,
                    null);
            if (SAML2Constants.RESPONDER.equals(statusCode)) {
                //In case of passive authentication the NoPassive response will be sent using two StatusCode nodes:
                //the outer StatusCode will be Responder and the inner StatusCode will contain the NoPassive URN
                StatusCode secondLevelStatusCode = status.getStatusCode().getStatusCode();
                if (secondLevelStatusCode != null
                        && SAML2Constants.NOPASSIVE.equals(secondLevelStatusCode.getValue())) {
                    throw new SAML2Exception(SAML2Utils.BUNDLE_NAME, "noPassiveResponse", null);
                }
            } else if (SAML2Constants.REQUESTER.equals(statusCode)) {
                // when is AllowCreate=false mode the auth module gets here with a
                // statusCode of urn:oasis:names:tc:SAML:2.0:status:Requester

                StatusCode secondLevelStatusCode = status.getStatusCode().getStatusCode();
                if (secondLevelStatusCode != null
                        && SAML2Constants.INVALID_NAME_ID_POLICY.equals(secondLevelStatusCode.getValue())) {
                    throw new SAML2Exception(SAML2Utils.BUNDLE_NAME, "nameIDMReqInvalidNameIDPolicy", null);
                }
            }

            throw new SAML2Exception(bundle.getString("invalidStatusCodeInResponse"));
        }

        if (saml2MetaManager == null) {
            throw new SAML2Exception(bundle.getString("nullMetaManager"));
        }
        SPSSOConfigElement spConfig = null;
        SPSSODescriptorElement spDesc = null;
        spConfig = saml2MetaManager.getSPSSOConfig(
                orgName, hostEntityId);
        spDesc = saml2MetaManager.getSPSSODescriptor(orgName, hostEntityId);

        if (debug.messageEnabled()) {
            debug.message(method + "binding is :" + profileBinding);
        }
        // SAML spec processing
        //  4.1.4.3   Verify any signatures present on the assertion(s) or the response
        boolean responseIsSigned = false;
        if (response.isSigned()) {
            IDPSSODescriptorElement idpSSODescriptor = null;
            try {
                idpSSODescriptor = saml2MetaManager.getIDPSSODescriptor(orgName, idpEntityId);
            } catch (SAML2MetaException sme) {
                String[] data = { orgName, idpEntityId };
                LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
                throw new SAML2Exception(sme);
            }
            if (idpSSODescriptor != null) {
                Set<X509Certificate> verificationCerts = KeyUtil.getVerificationCerts(idpSSODescriptor, idpEntityId,
                        SAML2Constants.IDP_ROLE);
                if (CollectionUtils.isEmpty(verificationCerts) || !response.isSignatureValid(verificationCerts)) {
                    debug.error(method + "Response is not signed or signature is not valid.");
                    String[] data = { orgName, hostEntityId, idpEntityId };
                    LogUtil.error(Level.INFO, LogUtil.POST_RESPONSE_INVALID_SIGNATURE, data, null);
                    throw new SAML2Exception(bundle.getString("invalidSignInResponse"));
                }
            } else {
                String[] data = { idpEntityId };
                LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
                throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
            }
            responseIsSigned = true;
        }

        if (debug.messageEnabled()) {
            debug.message(method + "responseIsSigned is :" + responseIsSigned);
        }

        // assertion encryption check
        boolean needAssertionEncrypted = false;
        String assertionEncryptedAttr = getAttributeValueFromSPSSOConfig(spConfig,
                SAML2Constants.WANT_ASSERTION_ENCRYPTED);
        needAssertionEncrypted = Boolean.parseBoolean(assertionEncryptedAttr);

        if (debug.messageEnabled()) {
            debug.message(method + "NeedAssertionEncrypted is :" + needAssertionEncrypted);
        }
        List<Assertion> assertions = response.getAssertion();
        if (needAssertionEncrypted && !CollectionUtils.isEmpty(assertions)) {
            String[] data = { respID };
            LogUtil.error(Level.INFO, LogUtil.ASSERTION_NOT_ENCRYPTED, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("assertionNotEncrypted"));
        }

        Set<PrivateKey> decryptionKeys;
        List<EncryptedAssertion> encAssertions = response.getEncryptedAssertion();
        if (encAssertions != null) {
            decryptionKeys = KeyUtil.getDecryptionKeys(spConfig);
            for (EncryptedAssertion encAssertion : encAssertions) {
                Assertion assertion = encAssertion.decrypt(decryptionKeys);
                if (assertions == null) {
                    assertions = new ArrayList<>();
                }
                assertions.add(assertion);
            }
        }

        if (CollectionUtils.isEmpty(assertions)) {
            if (debug.messageEnabled()) {
                debug.message(method + "no assertion in the Response.");
            }
            String[] data = { respID };
            LogUtil.error(Level.INFO, LogUtil.MISSING_ASSERTION, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("missingAssertion"));
        }

        boolean wantAssertionsSigned = spDesc.isWantAssertionsSigned();
        if (debug.messageEnabled()) {
            debug.message(method + "wantAssertionsSigned is :" + wantAssertionsSigned);
        }

        // validate the assertions
        Map smap = null;
        Map bearerMap = null;
        IDPSSODescriptorElement idp = null;
        Set<X509Certificate> verificationCerts = null;
        boolean allAssertionsSigned = true;
        for (Assertion assertion : assertions) {
            String assertionID = assertion.getID();
            Issuer issuer = assertion.getIssuer();
            if (!isSourceSiteValid(issuer, orgName, hostEntityId)) {
                debug.error("assertion's source site is not valid.");
                String[] data = {assertionID};
                LogUtil.error(Level.INFO,
                        LogUtil.INVALID_ISSUER_ASSERTION,
                        data,
                        null);
                throw new SAML2Exception(bundle.getString(
                        "invalidIssuerInAssertion"));
            }
            if (idpEntityId == null) {
                idpEntityId = issuer.getValue();
            } else {
                if (!idpEntityId.equals(issuer.getValue())) {
                    if (debug.messageEnabled()) {
                        debug.message(method + "Issuer in Assertion doesn't "
                                + "match the Issuer in Response or other "
                                + "Assertions in the Response.");
                    }
                    String[] data = {assertionID};
                    LogUtil.error(Level.INFO,
                            LogUtil.MISMATCH_ISSUER_ASSERTION,
                            data,
                            null);
                    throw new SAML2Exception(
                            SAML2Utils.bundle.getString("mismatchIssuer"));
                }
            }
            if (assertion.isSigned()) {
                if (verificationCerts == null) {
                    idp = saml2MetaManager.getIDPSSODescriptor(
                            orgName, idpEntityId);
                    verificationCerts = KeyUtil.getVerificationCerts(idp, idpEntityId, SAML2Constants.IDP_ROLE);
                }
                if (CollectionUtils.isEmpty(verificationCerts) || !assertion.isSignatureValid(verificationCerts)) {
                    debug.error(method +
                            "Assertion is not signed or signature is not valid.");
                    String[] data = {assertionID};
                    LogUtil.error(Level.INFO,
                            LogUtil.INVALID_SIGNATURE_ASSERTION,
                            data,
                            null);
                    throw new SAML2Exception(bundle.getString(
                            "invalidSignatureOnAssertion"));
                }
            } else {
                allAssertionsSigned = false;
            }
            List authnStmts = assertion.getAuthnStatements();
            if (authnStmts != null && !authnStmts.isEmpty()) {
                Subject subject = assertion.getSubject();
                if (subject == null) {
                    continue;
                }
                List subjectConfirms = subject.getSubjectConfirmation();
                if (subjectConfirms == null || subjectConfirms.isEmpty()) {
                    continue;
                }

                bearerMap = isBearerSubjectConfirmation(subjectConfirms,
                        inRespToResp,
                        spDesc,
                        spConfig,
                        assertionID);

                if (!((Boolean) bearerMap.get(SAML2Constants.IS_BEARER))) {
                    continue;
                }

                boolean foundAssertion = false;
                if ((SPCache.assertionByIDCache != null) &&
                        (SPCache.assertionByIDCache.containsKey(assertionID))) {
                    foundAssertion = true;
                }

                if ((!foundAssertion) && SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                    try {
                        if (SAML2FailoverUtils.retrieveSAML2Token(assertionID) != null) {
                            foundAssertion = true;
                        }
                    } catch (SAML2TokenRepositoryException e) {
                        if (debug.messageEnabled()) {
                            debug.message("Session not found in AMTokenSAML2Repository.", e);
                        }
                    }
                }
                if (foundAssertion) {
                    debug.error("Bearer Assertion is one time use only!");
                    throw new SAML2Exception(bundle.getString("usedBearAssertion"));
                }
                checkAudience(assertion.getConditions(),
                        hostEntityId,
                        assertionID);
                if (smap == null) {
                    smap = fillMap(authnStmts,
                            subject,
                            assertion,
                            assertions,
                            reqInfo,
                            inRespToResp,
                            orgName,
                            hostEntityId,
                            idpEntityId,
                            spConfig,
                            (Date) bearerMap.get(SAML2Constants.NOTONORAFTER));
                }
            } // end of having authnStmt
        }

        if (smap == null) {
            debug.error("No Authentication Assertion in Response.");
            throw new SAML2Exception(bundle.getString("missingAuthnAssertion"));
        }
        // 5.3 signature inheritance -  assertion can be considered to inherit the signature from
        // the enclosing element
        if (wantAssertionsSigned && !(responseIsSigned || allAssertionsSigned)){
            debug.error(method + "WantAssertionsSigned is true and response or all assertions are not signed");
            String[] data = { orgName, hostEntityId, idpEntityId };
            LogUtil.error(Level.INFO, LogUtil.INVALID_SIGNATURE_ASSERTION, data, null);
            throw new SAML2Exception(bundle.getString("assertionNotSigned"));
        }

        // 4.1.4.5 POST-Specific Processing Rules each assertion MUST be protected by a digital signature either by
        // signing each individual <Assertion> element or by signing the <Response> element.
        if (profileBinding.equals(SAML2Constants.HTTP_POST) ) {
            boolean wantPostResponseSigned = SAML2Utils
                    .wantPOSTResponseSigned(orgName, hostEntityId, SAML2Constants.SP_ROLE);

            if (debug.messageEnabled()) {
                debug.message(method + "wantPostResponseSigned is :" + wantPostResponseSigned);
            }
            if (wantPostResponseSigned && !responseIsSigned) {
                debug.error(method + "wantPostResponseSigned is true but response is not signed");
                String[] data = { orgName, hostEntityId, idpEntityId };
                LogUtil.error(Level.INFO, LogUtil.POST_RESPONSE_INVALID_SIGNATURE, data, null);
                throw new SAML2Exception(bundle.getString("responseNotSigned"));
            }
            if (!responseIsSigned && !allAssertionsSigned) {
                debug.error(method + "WantAssertionsSigned is true but some or all assertions are not signed");
                String[] data = { orgName, hostEntityId, idpEntityId };
                LogUtil.error(Level.INFO, LogUtil.INVALID_SIGNATURE_ASSERTION, data, null);
                throw new SAML2Exception(bundle.getString("assertionNotSigned"));
            }
        }

        return smap;
    }


    private static Map isBearerSubjectConfirmation(final List subjectConfirms,
                                                   final String inRespToResponse,
                                                   final SPSSODescriptorElement spDesc,
                                                   final SPSSOConfigElement spConfig,
                                                   final String assertionID)
            throws SAML2Exception {
        String method = "SAML2Utils.isBearerSubjectConfirmation:";
        Map retMap = new HashMap();
        boolean hasBearer = false;
        for (Iterator it = subjectConfirms.iterator(); it.hasNext(); ) {
            SubjectConfirmation subjectConfirm =
                    (SubjectConfirmation) it.next();
            if (subjectConfirm == null ||
                    subjectConfirm.getMethod() == null ||
                    !subjectConfirm.getMethod().equals(
                            SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER)) {
                continue;
            }
            // since this is bearer SC, all below must be true
            SubjectConfirmationData subjectConfData =
                    subjectConfirm.getSubjectConfirmationData();
            if (subjectConfData == null) {
                if (debug.messageEnabled()) {
                    debug.message(method + "missing SubjectConfirmationData.");
                }
                String[] data = {assertionID};
                LogUtil.error(Level.INFO,
                        LogUtil.MISSING_SUBJECT_COMFIRMATION_DATA,
                        data,
                        null);
                throw new SAML2Exception(bundle.getString(
                        "missingSubjectConfirmationData"));
            }

            String recipient = subjectConfData.getRecipient();
            if (recipient == null || recipient.length() == 0) {
                if (debug.messageEnabled()) {
                    debug.message(method + "missing Recipient in Assertion.");
                }
                String[] data = {assertionID};
                LogUtil.error(Level.INFO,
                        LogUtil.MISSING_RECIPIENT,
                        data,
                        null);
                throw new SAML2Exception(bundle.getString("missingRecipient"));
            }
            boolean foundMatch = false;
            Iterator acsIter = spDesc.getAssertionConsumerService().iterator();
            while (acsIter.hasNext()) {
                AssertionConsumerServiceElement acs =
                        (AssertionConsumerServiceElement) acsIter.next();
                if (recipient.equals(acs.getLocation())) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                if (debug.messageEnabled()) {
                    debug.message(method + "this sp is not the intended "
                            + "recipient.");
                }
                String[] data = {assertionID, recipient};
                LogUtil.error(Level.INFO,
                        LogUtil.WRONG_RECIPIENT,
                        data,
                        null);
                throw new SAML2Exception(bundle.getString("wrongRecipient"));
            }

            // in seconds
            int timeskew = SAML2Constants.ASSERTION_TIME_SKEW_DEFAULT;
            String timeskewStr = getAttributeValueFromSPSSOConfig(
                    spConfig,
                    SAML2Constants.ASSERTION_TIME_SKEW);
            if (timeskewStr != null && timeskewStr.trim().length() > 0) {
                timeskew = Integer.parseInt(timeskewStr);
                if (timeskew < 0) {
                    timeskew = SAML2Constants.ASSERTION_TIME_SKEW_DEFAULT;
                }
            }
            if (debug.messageEnabled()) {
                debug.message(method + "timeskew = " + timeskew);
            }

            Date notOnOrAfter = subjectConfData.getNotOnOrAfter();
            if (notOnOrAfter == null ||
                    ((notOnOrAfter.getTime() + timeskew * 1000) <
                            System.currentTimeMillis())) {
                if (debug.messageEnabled()) {
                    debug.message(method + "Time in SubjectConfirmationData of "
                            + "Assertion:" + assertionID + " is invalid.");
                }
                String[] data = {assertionID};
                LogUtil.error(Level.INFO,
                        LogUtil.INVALID_TIME_SUBJECT_CONFIRMATION_DATA,
                        data,
                        null);
                throw new SAML2Exception(bundle.getString(
                        "invalidTimeOnSubjectConfirmationData"));
            }
            retMap.put(SAML2Constants.NOTONORAFTER, notOnOrAfter);

            Date notBefore = subjectConfData.getNotBefore();
            if (notBefore != null) {
                if ((notBefore.getTime() + timeskew * 1000) >
                        System.currentTimeMillis()) {
                    if (debug.messageEnabled()) {
                        debug.message(method + "SubjectConfirmationData included "
                                + "NotBefore.");
                    }
                    String[] data = {assertionID};
                    LogUtil.error(Level.INFO, LogUtil.CONTAINED_NOT_BEFORE, data, null);
                    throw new SAML2Exception(bundle.getString(
                            "containedNotBefore"));
                }
            }
            retMap.put(SAML2Constants.NOTBEFORE, notBefore);

            String inRespTo = subjectConfData.getInResponseTo();
            if (inRespTo != null && inRespTo.length() != 0) {
                if (!inRespTo.equals(inRespToResponse)) {
                    if (debug.messageEnabled()) {
                        debug.message(method + "InResponseTo in Assertion is "
                                + "different from the one in Response.");
                    }
                    String[] data = {assertionID};
                    LogUtil.error(Level.INFO,
                            LogUtil.WRONG_INRESPONSETO_ASSERTION,
                            data,
                            null);
                    throw new SAML2Exception(bundle.getString(
                            "wrongInResponseToInAssertion"));
                }
            } else {
                if (inRespToResponse != null && inRespToResponse.length() != 0) {
                    if (debug.messageEnabled()) {
                        debug.message(method + "Assertion doesn't contain "
                                + "InResponseTo, but Response does.");
                    }
                    String[] data = {assertionID};
                    LogUtil.error(Level.INFO,
                            LogUtil.WRONG_INRESPONSETO_ASSERTION,
                            data,
                            null);
                    throw new SAML2Exception(bundle.getString(
                            "wrongInResponseToInAssertion"));
                }
            }

            hasBearer = true;
            break;
        }
        retMap.put(SAML2Constants.IS_BEARER, Boolean.valueOf(hasBearer));
        return retMap;
    }

    private static void checkAudience(final Conditions conds,
                                      final String hostEntityId,
                                      final String assertionID)
            throws SAML2Exception {
        final String method = "SAML2Utils.checkAudience:";
        if (conds == null) {
            if (debug.messageEnabled()) {
                debug.message(method + "Conditions is missing from Assertion.");
            }
            String[] data = {assertionID};
            LogUtil.error(Level.INFO,
                    LogUtil.MISSING_CONDITIONS,
                    data,
                    null);
            throw new SAML2Exception(bundle.getString("missingConditions"));
        }
        List restrictions = conds.getAudienceRestrictions();
        if (restrictions == null) {
            if (debug.messageEnabled()) {
                debug.message(method + "missing AudienceRestriction.");
            }
            String[] data = {assertionID};
            LogUtil.error(Level.INFO,
                    LogUtil.MISSING_AUDIENCE_RESTRICTION,
                    data,
                    null);
            throw new SAML2Exception(bundle.getString(
                    "missingAudienceRestriction"));
        }
        Iterator restIter = restrictions.iterator();
        boolean found = false;
        while (restIter.hasNext()) {
            List audienceList =
                    ((AudienceRestriction) restIter.next()).getAudience();
            if (audienceList.contains(hostEntityId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            if (debug.messageEnabled()) {
                debug.message(method + "This SP is not the intended audience.");
            }
            String[] data = {assertionID};
            LogUtil.error(Level.INFO,
                    LogUtil.WRONG_AUDIENCE,
                    data,
                    null);

            throw new SAML2Exception(bundle.getString("audienceNotMatch"));
        }
    }

    private static Map fillMap(final List authnStmts,
                               final Subject subject,
                               final Assertion assertion,
                               final List assertions,
                               final AuthnRequestInfo reqInfo,
                               final String inRespToResp,
                               final String orgName,
                               final String hostEntityId,
                               final String idpEntityId,
                               final SPSSOConfigElement spConfig,
                               final Date notOnOrAfterTime)
            throws SAML2Exception {
        // use the first AuthnStmt
        AuthnStatement authnStmt = (AuthnStatement) authnStmts.get(0);
        int authLevel = -1;

        String mapperClass = getAttributeValueFromSPSSOConfig(spConfig,
                SAML2Constants.SP_AUTHCONTEXT_MAPPER);
        SPAuthnContextMapper mapper = getSPAuthnContextMapper(orgName,
                hostEntityId, mapperClass);
        RequestedAuthnContext reqContext = null;
        AuthnRequest authnRequest = null;
        if (reqInfo != null) {
            reqContext = (reqInfo.getAuthnRequest()).
                    getRequestedAuthnContext();
            authnRequest = reqInfo.getAuthnRequest();
        }
        authLevel = mapper.getAuthLevel(reqContext,
                authnStmt.getAuthnContext(),
                orgName,
                hostEntityId,
                idpEntityId);

        String sessionIndex = authnStmt.getSessionIndex();
        Date sessionNotOnOrAfter = authnStmt.getSessionNotOnOrAfter();

        Map smap = new HashMap();
        smap.put(SAML2Constants.SUBJECT, subject);
        smap.put(SAML2Constants.POST_ASSERTION, assertion);
        smap.put(SAML2Constants.ASSERTIONS, assertions);
        if (authnRequest != null) {
            smap.put(SAML2Constants.AUTHN_REQUEST, authnRequest);
        }
        String[] data = {assertion.getID(), "", ""};
        if (LogUtil.isAccessLoggable(Level.FINE)) {
            data[1] = subject.toXMLString();
        }
        if (sessionIndex != null && sessionIndex.length() != 0) {
            data[2] = sessionIndex;
            smap.put(SAML2Constants.SESSION_INDEX, sessionIndex);
        }
        if (authLevel >= 0) {
            smap.put(SAML2Constants.AUTH_LEVEL, new Integer(authLevel));
        }
        // SessionNotOnOrAfter
        if (sessionNotOnOrAfter != null) {
            long maxSessionTime = (sessionNotOnOrAfter.getTime() -
                    System.currentTimeMillis()) / 60000;
            if (maxSessionTime > 0) {
                smap.put(SAML2Constants.MAX_SESSION_TIME,
                        new Long(maxSessionTime));
            }
        }
        if (inRespToResp != null && inRespToResp.length() != 0) {
            smap.put(SAML2Constants.IN_RESPONSE_TO, inRespToResp);
        }
        if (debug.messageEnabled()) {
            debug.message("SAML2Utils.fillMap: Found valid authentication "
                    + "assertion.");
        }
        if (notOnOrAfterTime != null) {
            smap.put(SAML2Constants.NOTONORAFTER,
                    new Long(notOnOrAfterTime.getTime()));
        }
        LogUtil.access(Level.INFO,
                LogUtil.FOUND_AUTHN_ASSERTION,
                data,
                null);
        return smap;
    }

    /**
     * Retrieves attribute value for a given attribute name from
     * <code>SPSSOConfig</code>.
     *
     * @param config   <code>SPSSOConfigElement</code> instance.
     * @param attrName name of the attribute whose value ot be retrived.
     * @return value of the attribute; or <code>null</code> if the attribute
     * if not configured, or an error occured in the process.
     */
    public static String getAttributeValueFromSPSSOConfig(
            SPSSOConfigElement config,
            String attrName) {
        String result = null;
        if (config == null) {
            return null;
        }
        Map attrs = SAML2MetaUtils.getAttributes(config);
        List value = (List) attrs.get(attrName);
        if (value != null && value.size() != 0) {
            result = ((String) value.iterator().next()).trim();
        }
        return result;
    }


    /**
     * Gets List of 'String' assertions from the list of 'Assertion' assertions
     *
     * @param assertions A list of Assertions
     * @return a String printout of the list of Assertions
     */
    public static List getStrAssertions(List assertions) {
        List returnAssertions = new ArrayList();
        if (assertions != null) {
            Iterator it = assertions.iterator();
            while (it.hasNext()) {
                Assertion assertion = (Assertion) it.next();
                try {
                    returnAssertions.add(assertion.toXMLString(true, true));
                } catch (SAML2Exception e) {
                    debug.error("Invalid assertion: " + assertion);
                }
            }
        }
        return returnAssertions;
    }


    /**
     * Checks if it is a persistent request or not.
     *
     * @param nameId Name ID object
     * @return true if it is a persistent request, false if not.
     */
    public static boolean isPersistentNameID(final NameID nameId) {
        boolean isPersistent = false;
        if (nameId == null) {
            return isPersistent;
        }
        String id = nameId.getFormat();
        if (id != null) {
            if (id.equalsIgnoreCase(SAML2Constants.PERSISTENT) ||
                    id.equalsIgnoreCase(SAML2Constants.UNSPECIFIED)) {
                isPersistent = true;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("SAML2Utils:isPersistent : " + isPersistent);
        }
        return isPersistent;
    }

    /**
     * Checks if the federation information for the user exists or not.
     *
     * @param userName       user id for which account federation needs to be
     *                       returned.
     * @param hostEntityID   <code>EntityID</code> of the hosted entity.
     * @param remoteEntityId <code>EntityID</code> of the remote entity.
     * @return true if exists, false otherwise.
     */
    public static boolean isFedInfoExists(String userName, String hostEntityID,
                                          String remoteEntityId, NameID nameID) {
        boolean exists = false;
        if ((userName == null) || (hostEntityID == null) ||
                (remoteEntityId == null) || (nameID == null)) {
            return exists;
        }
        try {
            NameIDInfo info = AccountUtils.getAccountFederation(
                    userName, hostEntityID, remoteEntityId);

            if (info != null &&
                    info.getNameIDValue().equals(nameID.getValue())) {
                exists = true;
            }
        } catch (SAML2Exception se) {
            debug.error("Failed to get DataStoreProvider " + se.toString());
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils:isFedInfoExists:Stack : ", se);
            }
        } catch (Exception e) {
            debug.message("SAML2Utils:isFedInfoExists: Exception : ", e);
        }

        if (debug.messageEnabled()) {
            debug.message("SAML2Utils:isFedInfoExists : " + exists);
        }

        return exists;
    }

    /**
     * Returns the <code>NameIDInfoKey</code> key value pair that can
     * be used for searching the user.
     *
     * @param nameID         <code>NameID</code> object.
     * @param hostEntityID   hosted <code>EntityID</code>.
     * @param remoteEntityID remote <code>EntityID</code>.
     * @param hostEntityRole the role of hosted entity.
     * @throws <code>SAML2Exception</code> if any failure.
     */
    public static Map getNameIDKeyMap(final NameID nameID,
                                      final String hostEntityID,
                                      final String remoteEntityID,
                                      final String realm,
                                      final String hostEntityRole)
            throws SAML2Exception {

        if (nameID == null) {
            throw new SAML2Exception(bundle.getString(
                    "nullNameID"));
        }

        NameIDInfoKey infoKey = null;
        String affiliationID = nameID.getSPNameQualifier();
        if (affiliationID != null && !affiliationID.isEmpty()) {
            AffiliationDescriptorType affiDesc =
                    saml2MetaManager.getAffiliationDescriptor(realm, affiliationID);
            if (affiDesc == null) {
                infoKey = new NameIDInfoKey(nameID.getValue(), hostEntityID,
                        remoteEntityID);
            } else {
                if (SAML2Constants.SP_ROLE.equals(hostEntityRole)) {
                    if (!affiDesc.getAffiliateMember().contains(hostEntityID)) {
                        throw new SAML2Exception(SAML2Utils.bundle.getString(
                                "spNotAffiliationMember"));
                    }
                    infoKey = new NameIDInfoKey(nameID.getValue(),
                            affiliationID, remoteEntityID);
                } else {
                    if (!affiDesc.getAffiliateMember().contains(
                            remoteEntityID)) {
                        throw new SAML2Exception(SAML2Utils.bundle.getString(
                                "spNotAffiliationMember"));
                    }
                    infoKey = new NameIDInfoKey(nameID.getValue(),
                            hostEntityID, affiliationID);
                }
            }
        } else {
            infoKey = new NameIDInfoKey(nameID.getValue(), hostEntityID,
                    remoteEntityID);
        }
        HashSet set = new HashSet();
        set.add(infoKey.toValueString());

        Map keyMap = new HashMap();
        keyMap.put(AccountUtils.getNameIDInfoKeyAttribute(), set);

        if (debug.messageEnabled()) {
            debug.message("SAML2Utils.getNameIDKeyMap: " + keyMap);
        }
        return keyMap;
    }

    /**
     * Returns <code>true</code> if <code>Issuer</code> is valid.
     *
     * @param issuer       to be checked <code>Issuer</code> instance.
     * @param orgName      the name of the realm or organization.
     * @param hostEntityId Entity ID of the hosted provider.
     * @return <code>true</code> if the <code>Issuer</code> is trusted;
     * <code>false</code> otherwise.
     */
    public static boolean isSourceSiteValid(final Issuer issuer,
                                            final String orgName,
                                            final String hostEntityId) {
        boolean isValid = false;
        try {
            if (issuer != null) {
                String entityID = issuer.getValue().trim();
                if (entityID != null && entityID.length() != 0) {
                    // Check if entityID is trusted provider
                    isValid = saml2MetaManager.isTrustedProvider(
                            orgName, hostEntityId, entityID);
                }
            }
            return isValid;
        } catch (Exception e) {
            debug.error("SAML2Utils.isSourceSiteValid: " +
                    "Exception : ", e);
            return false;
        }
    }


    /**
     * Returns <code>DataStoreProvider</code> object.
     *
     * @return <code>DataStoreProvider</code> configured for the SAML2 plugin.
     * @throws SAML2Exception if any failure.
     */
    public static DataStoreProvider getDataStoreProvider()
            throws SAML2Exception {
        try {
            DataStoreProviderManager dsManager =
                    DataStoreProviderManager.getInstance();
            return dsManager.getDataStoreProvider(SAML2Constants.SAML2);
        } catch (DataStoreProviderException dse) {
            debug.error("SAML2Utils.getDataStoreProvider: " +
                    "DataStoreProviderException : ", dse);
            throw new SAML2Exception(dse);
        }
    }

    /**
     * Returns the encoded request message. The SAML Request message must be
     * encoded before being transmitted. The Request message is  base-64
     * encoded according to the rules specified in RFC2045.
     *
     * @param str String to be encoded.
     * @return String the encoded String value or null on error.
     */
    public static String encodeForPOST(final String str) {
        try {
            return Base64.encode(str.getBytes("UTF-8"), true);
        } catch (UnsupportedEncodingException uee) {
            debug.error("SAML2Utils.encodeForPOST", uee);
            return null;
        }
    }

    /**
     * Returns the encoded request message.
     * The SAML Request message must be
     * encoded before being transmitted.
     * The Request message is encoded as follows:
     * 1. URL Encoded using the DEFLATE compression method.
     * 2. Then the message is base-64 encoded according to
     * the rules specified in RFC2045.
     *
     * @param str String to be encoded.
     * @return String the encoded String value or null on error.
     */
    public static String encodeForRedirect(final String str) {
        String classMethod = "SAML2Utils.encodeForRedirect: ";

        byte[] input;
        try {
            input = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            debug.error(classMethod + "cannot get byte array: ", uee);
            return null;
        }

        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(out, deflater);
        try {
            deflaterOutputStream.write(input);
        } catch (IOException e) {
            debug.error(classMethod + "There was a problem compressing the input", e);
            return null;
        } finally {
            IOUtils.closeIfNotNull(deflaterOutputStream);
        }

        String encoded = URLEncDec.encode(Base64.encode(out.toByteArray()));
        if (debug.messageEnabled()) {
            debug.message(classMethod + "out string length : " + encoded.length());
            debug.message(classMethod + "out string is ===>" + encoded + "<===");
        }

        return encoded;
    }

    /**
     * Decodes the request message.
     *
     * @param str String to be decoded.
     * @return String the decoded String.
     */
    public static String decodeFromRedirect(final String str) {

        final String classMethod = "SAML2Utils.decodeFromRedirect: ";

        if (StringUtils.isEmpty(str)) {
            debug.error(classMethod + "input is null.");
            return null;
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + "input string length : " + str.length());
            debug.message(classMethod + "input string is ===>" + str + "<===");
        }
        byte[] input = Base64.decode(removeNewLineChars(str));
        if (input == null || input.length == 0) {
            debug.error(classMethod + "Base64 decoded result is null");
            return null;
        }

        // From the Inflater JavaDoc:
        // Note: When using the 'nowrap' option it is also necessary to provide an extra "dummy" byte as input.
        // This is required by the ZLIB native library in order to support certain optimizations.
        input = Arrays.copyOf(input, input.length + 1);

        int bufferLength = 2048;
        try {
            if (bufferLen != null && !bufferLen.isEmpty()) {
                bufferLength = Integer.parseInt(bufferLen);
            }
        } catch (NumberFormatException nfe) {
            debug.error(classMethod + "Unable to parse buffer length.", nfe);
        }
        // Decompress the bytes
        Inflater inflater = new Inflater(true);
        InflaterInputStream inflaterInputStream = new InflaterInputStream(new ByteArrayInputStream(input), inflater);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(bufferLength);
        try {
            int b = inflaterInputStream.read();
            while (b != -1) {
                bout.write(b);
                b = inflaterInputStream.read();
            }
        } catch (IOException e) {
            debug.error(classMethod + "There was a problem reading the compressed input", e);
            return null;
        } finally {
            IOUtils.closeIfNotNull(inflaterInputStream);
        }

        String result;
        try {
            result = bout.toString("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            debug.error(classMethod + "cannot convert byte array to string.", uee);
            return null;
        }

        if (debug.messageEnabled()) {
            debug.message(classMethod + "Return value: \n" + result);
        }

        return result;
    }

    /**
     * Removes new line character from a String.
     *
     * @param string String to remove newline characters from.
     * @return String with newline characters trimmed.
     */
    public static String removeNewLineChars(final String string) {
        if (StringUtils.isBlank(string)) {
            return string;
        }
        // maybe should be getting the newline expression from the system preferences
        String newLineRegularExpression = "(\\n)";
        return string.replaceAll(newLineRegularExpression, "");
    }

    /**
     * Returns an instance of <code>SAML2MetaManger</code>.
     *
     * @return Instance of <code>SAML2MetaManager</code>
     */
    public static SAML2MetaManager getSAML2MetaManager() {
        return saml2MetaManager;
    }

    /**
     * Returns the realm.
     *
     * @param realm Realm object.
     * @return realm if the input is not null or empty, otherwise
     * return the root realm.
     */
    public static String getRealm(final String realm) {
        if (StringUtils.isEmpty(realm)) {
            return "/";
        }
        return realm;
    }

    /**
     * Returns the realm.
     *
     * @param paramsMap a map of parameters
     * @return realm if the input map contains the realm, otherwise
     * return the default realm from AMConfig.properties
     */
    public static String getRealm(final Map paramsMap) {
        return getRealm(getParameter(paramsMap, SAML2Constants.REALM));
    }

    /**
     * Returns the query parameter value for the param specified from the given Map.
     *
     * @param paramsMap     a map of parameters
     * @param attributeName name of the parameter
     * @return the value of this parameter or null if the parameter was not found in the params map
     */
    public static String getParameter(final Map paramsMap, final String attributeName) {
        if (null == paramsMap || paramsMap.isEmpty()) {
            return null;
        }
        return (String) paramsMap.get(attributeName);
    }

    /**
     * Returns a Map of parameters retrieved from the Query parameters
     * in the HttpServletRequest.
     *
     * @param request the <code>HttpServletRequest</code>.
     * @return a Map where the key is the parameter Name and
     * value is of the type List.
     */
    public static Map<String, List<String>> getParamsMap(final HttpServletRequest request) {
        Map<String, List<String>> paramsMap = new HashMap<>();

        String relayState = getRelayState(request);
        insertValue(paramsMap, relayState, SAML2Constants.RELAY_STATE);

        extractAndAddValue(request, paramsMap, SAML2Constants.ISPASSIVE);
        extractAndAddValue(request, paramsMap, SAML2Constants.FORCEAUTHN);
        extractAndAddValue(request, paramsMap, SAML2Constants.ALLOWCREATE);
        extractAndAddValue(request, paramsMap, SAML2Constants.CONSENT);
        extractAndAddValue(request, paramsMap, SAML2Constants.DESTINATION);
        extractAndAddValue(request, paramsMap, SAML2Constants.NAMEID_POLICY_FORMAT);
        extractAndAddValue(request, paramsMap, SAML2Constants.BINDING);
        extractAndAddValue(request, paramsMap, SAML2Constants.ACS_URL_INDEX);
        extractAndAddValue(request, paramsMap, SAML2Constants.ATTR_INDEX);
        extractAndAddValue(request, paramsMap, SAML2Constants.SP_AUTHCONTEXT_COMPARISON);

        String authContextDeclRef
                = request.getParameter(SAML2Constants.AUTH_CONTEXT_DECL_REF);
        if ((authContextDeclRef != null) && authContextDeclRef.length() > 0) {
            List authDeclList = getAuthContextList(authContextDeclRef);
            paramsMap.put(SAML2Constants.AUTH_CONTEXT_DECL_REF, authDeclList);
        }

        String authContextClassRef
                = request.getParameter(SAML2Constants.AUTH_CONTEXT_CLASS_REF);
        if (authContextClassRef != null) {
            List authClassRefList = getAuthContextList(authContextClassRef);
            paramsMap.put(SAML2Constants.AUTH_CONTEXT_CLASS_REF,
                    authClassRefList);
        }

        extractAndAddValueUsingIfNotEmpty(request, paramsMap, SAML2Constants.AUTH_LEVEL);
        extractAndAddValueUsingIfNotEmpty(request, paramsMap, SAML2Constants.AUTH_LEVEL_ADVICE);
        extractAndAddValue(request, paramsMap, SAML2Constants.REQ_BINDING);
        extractAndAddValue(request, paramsMap, SAML2Constants.AFFILIATION_ID);

        String includeContext = request.getParameter(SAML2Constants.INCLUDE_REQUESTED_AUTHN_CONTEXT);
        if (includeContext != null) {
            paramsMap.put(SAML2Constants.INCLUDE_REQUESTED_AUTHN_CONTEXT, Collections.singletonList(includeContext));
        }

        return paramsMap;
    }

    /**
     * Extracts a value from the request and puts it in the map.
     *
     * @param request       the request object to extract the value from
     * @param paramsMap     the Map to add the value to
     * @param parameterName the name of the value to extract
     */
    private static void extractAndAddValue(final HttpServletRequest request,
                                           final Map<String, List<String>> paramsMap, final String parameterName) {
        String parameterValue = request.getParameter(parameterName);
        insertValue(paramsMap, parameterValue, parameterName);
    }

    /**
     * Extracts a value from the request and puts it in the map.
     *
     * @param request       the request object to extract the value from
     * @param paramsMap     the Map to add the value to
     * @param parameterName the name of the value to extract
     */
    private static void extractAndAddValueUsingIfNotEmpty(final HttpServletRequest request,
                                                          final Map<String, List<String>> paramsMap,
                                                          final String parameterName) {
        String parameterValue = request.getParameter(parameterName);
        insertValueUsingIfNotEmpty(paramsMap, parameterValue, parameterName);
    }

    /**
     * Puts the given parameter value into a new List object then adds it to the map.
     *
     * @param paramsMap     the map to add the object to
     * @param paramVal      the value of the parameter
     * @param parameterName the name of the parameter
     */
    private static void insertValue(final Map<String, List<String>> paramsMap,
                                    final String paramVal,
                                    final String parameterName) {
        if (paramVal != null) {
            List<String> list = new ArrayList<>();
            list.add(paramVal);
            paramsMap.put(parameterName, list);
        }
    }

    /**
     * Puts the given parameter value into a new List object then adds it to the map.
     *
     * @param paramsMap     the map to add the object to
     * @param paramVal      the value of the parameter
     * @param parameterName the name of the parameter
     */
    private static void insertValueUsingIfNotEmpty(final Map<String, List<String>> paramsMap,
                                                   final String paramVal,
                                                   final String parameterName) {
        if (StringUtils.isNotEmpty(paramVal)) {
            List<String> list = new ArrayList<>();
            list.add(paramVal);
            paramsMap.put(parameterName, list);
        }
    }


    /**
     * Returns the Authcontext declare/class references
     * as a List. The string passed to this method
     * is a pipe separated value.
     *
     * @param str a pipe separated String to be parsed.
     * @return List contained the parsed values.
     */
    private static List getAuthContextList(String str) {
        List ctxList = new ArrayList();
        StringTokenizer st = new StringTokenizer(str, "|");
        while (st.hasMoreTokens()) {
            ctxList.add(st.nextToken());
        }
        return ctxList;
    }

    /**
     * Generates provider Source ID based on provider Entity ID. The returned
     * is SHA-1 digest string.
     *
     * @param entityID Entity ID for example <code>http://host.sun.com:81</code>
     * @return sourceID string
     */
    public static String generateSourceID(String entityID) {
        if ((entityID == null) || (entityID.length() == 0)) {
            return null;
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            debug.error("SAML2Utils.generateSourceID: ", e);
            return null;
        }
        char chars[] = entityID.toCharArray();
        byte bytes[] = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        md.update(bytes);
        return SAML2Utils.byteArrayToString(md.digest());
    }

    /**
     * Extracts serverID from the specified id.
     *
     * @param id an id.
     * @return the extracted id, or null if the given string is too short or null.
     */
    public static String extractServerId(String id) {
        if (id == null || id.length() < 2) {
            return null;
        }

        return id.substring(id.length() - 2);
    }

    /**
     * Gets remote service URL according to server id embedded in the provided ID.
     *
     * @param id The server's ID or a user's sessionIndex.
     * @return Remote service URL corresponding to the ID, or null if the ID is local, or an error occurred.
     */
    public static String getRemoteServiceURL(String id) {
        if (debug.messageEnabled()) {
            debug.message("SAML2Utils.getRemoteServiceURL: id = " + id);
        }

        if (StringUtils.isEmpty(id)) {
            return null;
        }

        String serverID = extractServerId(id);

        try {
            String localServerID = SystemConfigurationUtil.getServerID(
                    serverProtocol, serverHost, intServerPort, serverUri);
            if (serverID.equals(localServerID)) {
                return null;
            }

            if (SystemConfigurationUtil.isSiteId(serverID)) {
                if (debug.warningEnabled()) {
                    debug.warning("SAML2Utils.getRemoteServiceURL: the given id refers to a site and not a server: " + serverID);
                }
                return null;
            }

            return SystemConfigurationUtil.getServerFromID(serverID);
        } catch (SystemConfigurationException sce) {
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils.getRemoteServiceURL:", sce);
            }
            return null;
        }
    }

    /**
     * Generates ID with server id at the end.
     *
     * @return ID value.
     */
    public static String generateIDWithServerID() {
        if (random == null) {
            return null;
        }
        byte bytes[] = new byte[SAML2Constants.ID_LENGTH];
        random.nextBytes(bytes);
        String id = SAML2ID_PREFIX + byteArrayToHexString(bytes);

        return embedServerID(id);
    }

    /**
     * Generates message handle with server id used in an <code>Artifact</code>.
     *
     * @return String format of 20-byte sequence identifying message.
     */
    public static String generateMessageHandleWithServerID() {
        if (random == null) {
            return null;
        }
        byte bytes[] = new byte[SAML2Constants.ID_LENGTH];
        random.nextBytes(bytes);
        String id = byteArrayToString(bytes);

        return embedServerID(id);
    }


    /**
     * Replaces last 2 chars of specified id with server id.
     *
     * @param id an id
     * @return String with server id at the end.
     */
    private static String embedServerID(String id) {
        String serverId = null;
        try {
            serverId = SystemConfigurationUtil.getServerID(
                    serverProtocol, serverHost, intServerPort, serverUri);

            // serverId is 2 digit string
            if (serverId != null && serverId.length() == 2) {
                id = id.substring(0, id.length() - 2) + serverId;
            } else if (debug.messageEnabled()) {
                debug.message("SAML2Utils.appendServerID: " +
                        "invalid server id = " + serverId);
            }
        } catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils.appendServerID:", ex);
            }
        }

        return id;
    }

    /**
     * Returns the server id of the local server
     */
    public static String getLocalServerID() {
        String serverId = null;

        try {
            serverId = SystemConfigurationUtil.getServerID(serverProtocol,
                    serverHost, intServerPort, serverUri);
        } catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils.getLocalServerID:", ex);
            }
        }

        return serverId;
    }

    /**
     * Sets mime headers in HTTP servlet response.
     *
     * @param headers  mime headers to be set.
     * @param response HTTP servlet response.
     */
    public static void putHeaders(
            MimeHeaders headers, HttpServletResponse response) {
        if (debug.messageEnabled()) {
            debug.message("SAML2Util.putHeaders: Header=" + headers.toString());
        }
        Iterator it = headers.getAllHeaders();
        while (it.hasNext()) {
            MimeHeader header = (MimeHeader) it.next();
            String[] values = headers.getHeader(header.getName());
            if (debug.messageEnabled()) {
                debug.message("SAML2Util.putHeaders: Header name=" +
                        header.getName() + ", value=" + Arrays.toString(values));
            }
            if (values.length == 1) {
                response.setHeader(header.getName(), header.getValue());
            } else {
                StringBuilder concatenatedHeaderValues = new StringBuilder();
                for (int i = 0; i < values.length; i++) {
                    if (i != 0) {
                        concatenatedHeaderValues.append(',');
                    }
                    concatenatedHeaderValues.append(values[i]);
                }
                response.setHeader(header.getName(), concatenatedHeaderValues.toString());
            }
        }
    }

    /**
     * Generates SAMLv2 Status object
     *
     * @param code    Status code value.
     * @param message Status message.
     * @return Status object.
     */
    public static Status generateStatus(String code, String message) {
        return generateStatus(code, null, message);
    }

    /**
     * Generates SAMLv2 Status object
     *
     * @param code    Status code value.
     * @param subCode second-level status code
     * @param message Status message.
     * @return Status object.
     */
    public static Status generateStatus(String code, String subCode,
                                        String message) {

        Status status = null;
        try {
            status = ProtocolFactory.getInstance().createStatus();
            StatusCode statusCode = ProtocolFactory.getInstance()
                    .createStatusCode();
            statusCode.setValue(code);
            status.setStatusCode(statusCode);
            if ((message != null) && (message.length() != 0)) {
                status.setStatusMessage(message);
            }
            if (subCode != null) {
                StatusCode subStatusCode = ProtocolFactory.getInstance()
                        .createStatusCode();
                subStatusCode.setValue(subCode);
                statusCode.setStatusCode(subStatusCode);
            }
        } catch (SAML2Exception e) {
            debug.error("SAML2Utils.generateStatus:", e);
        }
        return status;
    }

    /**
     * Returns a <code>SAML Response</code> object containing error status
     *
     * @param request        the <code>RequestAbstract</code> object
     * @param code           the error code
     * @param subCode        teh second-level error code
     * @param statusMsg      the error message
     * @param issuerEntityID the entity id of the issuer
     * @return the <code>SAML Response</code> object containing error status
     * @throws SAML2Exception if the operation is not successful
     */
    public static Response getErrorResponse(
            RequestAbstract request,
            String code,
            String subCode,
            String statusMsg,
            String issuerEntityID)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getErrorResponse: ";

        Response errResp = ProtocolFactory.getInstance().createResponse();
        errResp.setStatus(generateStatus(code, subCode, statusMsg));

        String responseID = SAML2Utils.generateID();
        if (responseID == null) {
            debug.error("Unable to generate response ID.");
            return null;
        }
        errResp.setID(responseID);

        if (request != null) {
            // sp initiated case, need to set InResponseTo attribute
            errResp.setInResponseTo(request.getID());
        }
        errResp.setVersion(SAML2Constants.VERSION_2_0);
        errResp.setIssueInstant(new Date());

        // set the idp entity id as the response issuer
        if (issuerEntityID != null) {
            Issuer issuer = AssertionFactory.getInstance().createIssuer();
            issuer.setValue(issuerEntityID);
            errResp.setIssuer(issuer);
        }

        if (debug.messageEnabled()) {
            debug.message(classMethod +
                    "Error Response is : " + errResp.toXMLString());
        }
        return errResp;
    }

    /**
     * Returns encryption certificate alias names.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return The list of certificate aliases for encryption.
     */
    public static List<String> getEncryptionCertAliases(String realm, String hostEntityId, String entityRole) {
        if (debug.messageEnabled()) {
            debug.message("getEncryptionCertAliases : realm - {}; hostEntityId - {}; entityRole - {}", realm,
                    hostEntityId, entityRole);
        }
        return getAllAttributeValueFromSSOConfig(realm, hostEntityId, entityRole, SAML2Constants.ENCRYPTION_CERT_ALIAS);
    }

    /**
     * Returns signing certificate alias name.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return alias name of certificate alias for signing.
     */
    public static String getSigningCertAlias(String realm,
                                             String hostEntityId,
                                             String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getSigningCertAlias : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        return getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                SAML2Constants.SIGNING_CERT_ALIAS);
    }

    /**
     * Returns signing certificate key password (encrypted).
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return The encrypted keypass of the private key used for signing.
     */
    public static String getSigningCertEncryptedKeyPass(String realm,
                                                        String hostEntityId,
                                                        String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getSigningCertEncryptedKeyPass : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        return getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                SAML2Constants.SIGNING_CERT_KEYPASS);
    }

    /**
     * Returns true if wantAssertionEncrypted has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantAssertionEncrypted has <code>String</code> true.
     */
    public static boolean getWantAssertionEncrypted(String realm,
                                                    String hostEntityId,
                                                    String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantAssertionEncrypted : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantEncrypted = getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_ASSERTION_ENCRYPTED);
        if (wantEncrypted == null) {
            wantEncrypted = "false";
        }

        return wantEncrypted.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns true if wantAttributeEncrypted has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantAttributeEncrypted has <code>String</code> true.
     */
    public static boolean getWantAttributeEncrypted(String realm,
                                                    String hostEntityId,
                                                    String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantAttributeEncrypted : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantEncrypted = getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_ATTRIBUTE_ENCRYPTED);
        if (wantEncrypted == null) {
            wantEncrypted = "false";
        }

        return wantEncrypted.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns true if wantNameIDEncrypted has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantNameIDEncrypted has <code>String</code> true.
     */
    public static boolean getWantNameIDEncrypted(String realm,
                                                 String hostEntityId,
                                                 String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantNameIDEncrypted : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantEncrypted =
                getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_NAMEID_ENCRYPTED);
        if (wantEncrypted == null) {
            wantEncrypted = "false";
        }

        return wantEncrypted.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns true if wantArtifactResolveSigned has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantArtifactResolveSigned has <code>String</code> true.
     */
    public static boolean getWantArtifactResolveSigned(String realm,
                                                       String hostEntityId,
                                                       String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantArtifactResolveSigned : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantSigned = getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_ARTIFACT_RESOLVE_SIGNED);
        if (wantSigned == null) {
            wantSigned = "false";
        }

        return wantSigned.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns true if wantArtifactResponseSigned has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantArtifactResponseSigned has <code>String</code> true.
     */
    public static boolean getWantArtifactResponseSigned(String realm,
                                                        String hostEntityId,
                                                        String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantArtifactResponseSigned : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantSigned = getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED);
        if (wantSigned == null) {
            wantSigned = "false";
        }

        return wantSigned.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns true if wantLogoutRequestSigned has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantLogoutRequestSigned has <code>String</code> true.
     */
    public static boolean getWantLogoutRequestSigned(String realm,
                                                     String hostEntityId,
                                                     String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantLogoutRequestSigned : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantSigned =
                getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_LOGOUT_REQUEST_SIGNED);
        if (wantSigned == null) {
            wantSigned = "false";
        }

        return wantSigned.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns true if wantLogoutResponseSigned has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantLogoutResponseSigned has <code>String</code> true.
     */
    public static boolean getWantLogoutResponseSigned(String realm,
                                                      String hostEntityId,
                                                      String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantLogoutResponseSigned : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantSigned =
                getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_LOGOUT_RESPONSE_SIGNED);
        if (wantSigned == null) {
            wantSigned = "false";
        }

        return wantSigned.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns true if wantMNIRequestSigned has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantMNIRequestSigned has <code>String</code> true.
     */
    public static boolean getWantMNIRequestSigned(String realm,
                                                  String hostEntityId,
                                                  String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantMNIRequestSigned : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantSigned =
                getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_MNI_REQUEST_SIGNED);
        if (wantSigned == null) {
            wantSigned = "false";
        }

        return wantSigned.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns true if wantMNIResponseSigned has <code>String</code> true.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantMNIResponseSigned has <code>String</code> true.
     */
    public static boolean getWantMNIResponseSigned(String realm,
                                                   String hostEntityId,
                                                   String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantMNIResponseSigned : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantSigned =
                getAttributeValueFromSSOConfig(realm, hostEntityId, entityRole,
                        SAML2Constants.WANT_MNI_RESPONSE_SIGNED);
        if (wantSigned == null) {
            wantSigned = "false";
        }

        return wantSigned.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Returns boolean value of specified attribute from SSOConfig.
     * This method is used for boolean-valued attributes.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @param attrName     attribute name for the value.
     * @return value of specified attribute from SSOConfig.
     */
    public static boolean getBooleanAttributeValueFromSSOConfig(String realm,
                                                                String hostEntityId, String entityRole, String attrName) {

        List value = (List) getAllAttributeValueFromSSOConfig(realm,
                hostEntityId, entityRole, attrName);

        if ((value == null) || (value.isEmpty())) {
            return false;
        }

        return SAML2Constants.TRUE.equalsIgnoreCase((String) value.get(0));
    }

    /**
     * Returns single value of specified attribute from SSOConfig.
     * This method is used for single-valued attributes.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @param attrName     attribute name for the value.
     * @return value of specified attribute from SSOConfig.
     */
    public static String getAttributeValueFromSSOConfig(String realm,
                                                        String hostEntityId,
                                                        String entityRole,
                                                        String attrName) {
        if (debug.messageEnabled()) {
            String method = "getAttributeValueFromSSOConfig : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
            debug.message(method + "attrName - " + attrName);
        }
        List value = (List) getAllAttributeValueFromSSOConfig(realm,
                hostEntityId, entityRole, attrName);
        if (debug.messageEnabled()) {
            debug.message("getAttributeValueFromSSOConfig: values=" + value);
        }
        if ((value != null) && !value.isEmpty()) {
            return (String) value.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns all values of specified attribute from SSOConfig.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @param attrName     attribute name for the value.
     * @return value of specified attribute from SSOConfig.
     */
    public static List<String> getAllAttributeValueFromSSOConfig(String realm,
                                                                 String hostEntityId,
                                                                 String entityRole,
                                                                 String attrName) {
        if (debug.messageEnabled()) {
            String method = "getAllAttributeValueFromSSOConfig : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "hostEntityId - " + hostEntityId);
            debug.message(method + "entityRole - " + entityRole);
            debug.message(method + "attrName - " + attrName);
        }
        try {
            BaseConfigType config = null;
            if (entityRole.equalsIgnoreCase(SAML2Constants.SP_ROLE)) {
                config = saml2MetaManager.getSPSSOConfig(realm, hostEntityId);
            } else if (entityRole.equalsIgnoreCase(SAML2Constants.IDP_ROLE)) {
                config = saml2MetaManager.getIDPSSOConfig(realm, hostEntityId);
            } else if (entityRole.equalsIgnoreCase(
                    SAML2Constants.ATTR_AUTH_ROLE)) {
                config = saml2MetaManager.getAttributeAuthorityConfig(realm,
                        hostEntityId);
            } else if (entityRole.equalsIgnoreCase(
                    SAML2Constants.AUTHN_AUTH_ROLE)) {
                config = saml2MetaManager.getAuthnAuthorityConfig(realm,
                        hostEntityId);
            } else if (entityRole.equalsIgnoreCase(
                    SAML2Constants.ATTR_QUERY_ROLE)) {
                config = saml2MetaManager.getAttributeQueryConfig(realm,
                        hostEntityId);
            }

            if (config == null) {
                return null;
            }
            Map attrs = SAML2MetaUtils.getAttributes(config);
            if (attrs == null) {
                return null;
            }
            return (List) attrs.get(attrName);
        } catch (SAML2MetaException e) {
            debug.message("get SSOConfig failed:", e);
        }
        return null;
    }

    /**
     * Returns the role of host entity.
     *
     * @param paramsMap <code>Map</code> includes parameters.
     * @return role name for hosted entity.
     * @throws SAML2Exception if error in retrieving the parameters.
     */
    public static String getHostEntityRole(Map paramsMap)
            throws SAML2Exception {
        String roleName = getParameter(paramsMap, SAML2Constants.ROLE);
        if (roleName.equalsIgnoreCase(SAML2Constants.SP_ROLE) ||
                roleName.equalsIgnoreCase(SAML2Constants.IDP_ROLE)) {
            return roleName;
        }

        throw new SAML2Exception(
                SAML2Utils.bundle.getString("unknownHostEntityRole"));
    }

    /**
     * Returns true if this entity is acting as both SP and IDP.
     *
     * @param hostEntityId entity ID of the hosted entity.
     * @param realm        the realm the entity resides.
     * @return true if this entity is acting as both SP and IDP,
     * false otherwise.
     */
    public static boolean isDualRole(String hostEntityId, String realm) {
        try {
            SPSSOConfigElement spConfig =
                    saml2MetaManager.getSPSSOConfig(realm, hostEntityId);
            if (spConfig == null) {
                return false;
            }
            IDPSSOConfigElement idpConfig =
                    saml2MetaManager.getIDPSSOConfig(realm, hostEntityId);
            return idpConfig != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns url for redirection.
     *
     * @param request      <code>HttpServletRequest</code> for redirecting.
     * @param response     <code>HttpServletResponse</code> for redirecting.
     * @param realm        realm of hosted entity.
     * @param hostEntityID name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @throws IOException if error in redirecting request.
     */
    public static void redirectAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            String realm,
            String hostEntityID,
            String entityRole) throws IOException {
        String method = "redirectAuthentication: ";
        // get the authentication service url
        String authUrl = SAML2Utils.getAttributeValueFromSSOConfig(
                realm, hostEntityID, entityRole,
                SAML2Constants.AUTH_URL);
        if ((authUrl == null) || (authUrl.trim().length() == 0)) {
            // need to get it from the request
            String uri = request.getRequestURI();
            String deploymentURI = uri;
            int firstSlashIndex = uri.indexOf("/");
            int secondSlashIndex = uri.indexOf("/", firstSlashIndex + 1);
            if (secondSlashIndex != -1) {
                deploymentURI = uri.substring(0, secondSlashIndex);
            }
            StringBuffer sb = new StringBuffer();
            sb.append(request.getScheme()).append("://")
                    .append(request.getServerName()).append(":")
                    .append(request.getServerPort())
                    .append(deploymentURI)
                    .append("/UI/Login?realm=").append(realm);
            authUrl = sb.toString();
        }

        if (authUrl.indexOf("?") == -1) {
            authUrl += "?goto=";
        } else {
            authUrl += "&goto=";
        }

        authUrl += URLEncDec.encode(request.getRequestURL().toString()
                + "?" + request.getQueryString());
        if (debug.messageEnabled()) {
            debug.message(method + "New URL for authentication: " + authUrl);
        }

        FSUtils.forwardRequest(request, response, authUrl);
    }

    /**
     * Returns url for redirection.
     *
     * @param entityID entityID for Issuer.
     * @return Issuer for the specified entityID.
     * @throws SAML2Exception if error in creating Issuer element.
     */
    public static Issuer createIssuer(String entityID)
            throws SAML2Exception {
        String method = "createIssuer: ";
        Issuer issuer = assertionFactory.createIssuer();
        issuer.setValue(entityID);
        if (debug.messageEnabled()) {
            debug.message(method + "Issuer : " + issuer.toXMLString());
        }
        return issuer;
    }

    /**
     * Sign Query string.
     *
     * @param queryString    URL query string that will be signed.
     * @param realm          realm of host entity.
     * @param hostEntity     entityID of host entity.
     * @param hostEntityRole entity role of host entity.
     * @return returns signed query string.
     * @throws SAML2Exception if error in signing the query string.
     */
    public static String signQueryString(String queryString, String realm,
                                         String hostEntity, String hostEntityRole)
            throws SAML2Exception {
        String method = "signQueryString : ";
        if (debug.messageEnabled()) {
            debug.message(method + "queryString :" + queryString);
        }

        String alias = getSigningCertAlias(realm, hostEntity, hostEntityRole);
        String encryptedKeyPass = getSigningCertEncryptedKeyPass(realm, hostEntity, hostEntityRole);

        if (debug.messageEnabled()) {
            debug.message(method + "realm is : " + realm);
            debug.message(method + "hostEntity is : " + hostEntity);
            debug.message(method + "Host Entity role is : " + hostEntityRole);
            debug.message(method + "Signing Cert Alias is : " + alias);
            if (encryptedKeyPass != null && !encryptedKeyPass.isEmpty()) {
                debug.message(method + "Using provided Signing Cert KeyPass");
            }
        }
        PrivateKey signingKey;
        if (encryptedKeyPass == null || encryptedKeyPass.isEmpty()) {
            signingKey = keyProvider.getPrivateKey(alias);
        } else {
            signingKey = keyProvider.getPrivateKey(alias, encryptedKeyPass);
        }

        if (signingKey == null) {
            debug.error("Incorrect configuration for Signing Certificate.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }
        return QuerySignatureUtil.sign(queryString, signingKey);
    }

    /**
     * Verify Signed Query string.
     *
     * @param queryString    URL query string that will be verified.
     * @param realm          realm of host entity.
     * @param hostEntityRole entity role of host entity.
     * @param remoteEntity   entityID of peer entity.
     * @return returns true if sign is valid.
     * @throws SAML2Exception if error in verifying the signature.
     */
    public static boolean verifyQueryString(String queryString, String realm,
                                            String hostEntityRole, String remoteEntity)
            throws SAML2Exception {
        String method = "verifyQueryString : ";
        if (debug.messageEnabled()) {
            debug.message(method + "queryString :" + queryString);
        }

        Set<X509Certificate> signingCerts;
        if (hostEntityRole.equalsIgnoreCase(SAML2Constants.IDP_ROLE)) {
            SPSSODescriptorElement spSSODesc =
                    saml2MetaManager.getSPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(spSSODesc, remoteEntity, SAML2Constants.SP_ROLE);
        } else {
            IDPSSODescriptorElement idpSSODesc =
                    saml2MetaManager.getIDPSSODescriptor(realm, remoteEntity);
            signingCerts = KeyUtil.getVerificationCerts(idpSSODesc, remoteEntity, SAML2Constants.IDP_ROLE);
        }

        if (debug.messageEnabled()) {
            debug.message(method + "realm is : " + realm);
            debug.message(method + "Host Entity role is : " + hostEntityRole);
            debug.message(method + "remoteEntity is : " + remoteEntity);
        }
        if (signingCerts.isEmpty()) {
            debug.error("Incorrect configuration for Signing Certificate.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }
        return QuerySignatureUtil.verify(queryString, signingCerts);
    }

    /**
     * Parses the request parameters and return session object
     * or redirect to login url.
     *
     * @param request   the HttpServletRequest.
     * @param response  the HttpServletResponse.
     * @param metaAlias entityID of hosted entity.
     * @param paramsMap Map of all other parameters.
     * @return session object of <code>HttpServletRequest</code>.
     * @throws SAML2Exception if error initiating request to remote entity.
     */
    public static Object checkSession(
            HttpServletRequest request,
            HttpServletResponse response,
            String metaAlias,
            Map paramsMap) throws SAML2Exception {
        String method = "SAML2Utils.checkSession : ";
        Object session = null;
        try {
            session = SessionManager.getProvider().getSession(request);
        } catch (SessionException se) {
            if (debug.messageEnabled()) {
                debug.message(method, se);
            }
            session = null;
        }
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String hostEntity = null;
        String hostEntityRole = getHostEntityRole(paramsMap);

        if (session == null) {
            if (debug.messageEnabled()) {
                debug.message(method + "session is missing." +
                        "redirect to the authentication service");
            }
            // the user has not logged in yet,
            // redirect to the authentication service
            try {
                hostEntity = saml2MetaManager.getEntityByMetaAlias(metaAlias);
                redirectAuthentication(request, response,
                        realm, hostEntity, hostEntityRole);
            } catch (IOException ioe) {
                debug.error("Unable to redirect to authentication.");
                throw new SAML2Exception(ioe.toString());
            }
        }

        return session;
    }

    /**
     * Returns a Name Identifier
     *
     * @return a String the Name Identifier. Null value
     * is returned if there is an error in
     * generating the Name Identifier.
     */
    public static String createNameIdentifier() {
        String handle = null;
        try {
            byte[] handleBytes = new byte[21];
            randomGenerator.nextBytes(handleBytes);
            handle = Base64.encode(handleBytes);
            if (debug.messageEnabled()) {
                debug.message("createNameIdentifier String: " + handle);
            }
        } catch (Exception e) {
            debug.message("createNameIdentifier:"
                    + " Exception during proccessing request" + e.getMessage());
        }

        return handle;
    }


    /**
     * Returns the Service Provider AuthnContext Mapper Object.
     *
     * @param authnCtxClassName Service Provider AuthnContext Mapper Class Name.
     * @return SPAuthnContextMapper Object.
     */
    public static SPAuthnContextMapper getSPAuthnContextMapper(
            String realm, String hostEntityID,
            String authnCtxClassName) {


        SPAuthnContextMapper spAuthnCtx =
                (SPAuthnContextMapper)
                        SPCache.authCtxObjHash.get(hostEntityID + "|" + realm);

        if (debug.messageEnabled()) {
            debug.message("AuthContext Class Name is :"
                    + authnCtxClassName);
        }
        if ((spAuthnCtx == null) && ((authnCtxClassName != null) &&
                (authnCtxClassName.length() != 0))) {
            try {
                spAuthnCtx =
                        (SPAuthnContextMapper)
                                Class.forName(authnCtxClassName).newInstance();
                SPCache.authCtxObjHash.put(hostEntityID + "|" + realm, spAuthnCtx);
            } catch (ClassNotFoundException ce) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils: Mapper not configured"
                            + " using Default AuthnContext Mapper");
                }
            } catch (InstantiationException ie) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils: Instantiation ");
                    debug.message("SAML2Utils:Error instantiating : "
                            + " using Default AuthnContext Mapper");
                }
            } catch (IllegalAccessException iae) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils: illegalaccess");
                    debug.message("SAML2Utils:Error : "
                            + " using Default AuthnContext Mapper");
                }
            } catch (Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils:Error : "
                            + " using Default AuthnContext Mapper");
                }
            }
        }
        if (spAuthnCtx == null) {
            spAuthnCtx = new DefaultSPAuthnContextMapper();
            SPCache.authCtxObjHash.put(hostEntityID + "|" + realm, spAuthnCtx);
        }

        return spAuthnCtx;
    }

    /**
     * Verifies <code>Issuer</code> in <code>Request</code> and returns
     * true if the Issuer is part of COT
     * SAML2 auth module only.
     *
     * @param realm      realm of hosted entity.
     * @param hostEntity name of hosted entity.
     * @param reqIssuer  <code>Issuer</code> of Request.
     * @param requestId  request ID
     * @return true if issuer is valid.
     * @throws SAML2Exception
     */
    public static boolean verifyRequestIssuer(String realm, String hostEntity,
                                              Issuer reqIssuer, String requestId)
            throws SAML2Exception {
        boolean issuerValid = isSourceSiteValid(reqIssuer, realm, hostEntity);
        if (!issuerValid) {
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils " +
                        "Issuer in Request is not valid.");
            }
            String[] data = {hostEntity, realm, requestId};
            LogUtil.error(Level.INFO,
                    LogUtil.INVALID_ISSUER_REQUEST,
                    data,
                    null);
            throw new SAML2Exception(
                    bundle.getString("invalidIssuerInRequest"));
        }

        return issuerValid;
    }

    /**
     * Verifies <code>Issuer</code> in <code>Response</code> and returns
     * true if the Issuer is part of COT
     *
     * @param realm      realm of hosted entity.
     * @param hostEntity name of hosted entity.
     * @param resIssuer  <code>Issuer</code> of Response.
     * @param requestId  request ID for the response.
     * @return true if issuer is valid.
     * @throws SAML2Exception
     */
    public static boolean verifyResponseIssuer(String realm, String hostEntity,
                                               Issuer resIssuer, String requestId)
            throws SAML2Exception {
        boolean issuerValid = isSourceSiteValid(resIssuer, realm, hostEntity);
        if (!issuerValid) {
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils " +
                        "Issuer in Response is not valid.");
            }
            String[] data = {hostEntity, realm, requestId};
            LogUtil.error(Level.INFO,
                    LogUtil.INVALID_ISSUER_RESPONSE,
                    data,
                    null);
            throw new SAML2Exception(
                    bundle.getString("invalidIssuerInResponse"));
        }

        return issuerValid;
    }

    public static String getReaderURL(String spMetaAlias) {
        // get spExtended
        String classMethod = "SAML2Utils:getReaderURL:";
        String readerURL = null;
        try {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(spMetaAlias);
            String spEntityID =
                    saml2MetaManager.getEntityByMetaAlias(spMetaAlias);

            if (debug.messageEnabled()) {
                debug.message(classMethod + "metaAlias is :" + spMetaAlias);
                debug.message(classMethod + "Realm is :" + realm);
                debug.message(classMethod + "spEntityID is :" + spEntityID);
            }

            SPSSOConfigElement spEntityCfg =
                    saml2MetaManager.getSPSSOConfig(realm, spEntityID);
            Map spConfigAttrsMap = null;
            if (spEntityCfg != null) {
                spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
                List cotList = (List) spConfigAttrsMap.get("cotlist");
                String cotListStr = (String) cotList.iterator().next();
                CircleOfTrustDescriptor cotDesc =
                        cotManager.getCircleOfTrust(realm, cotListStr);
                readerURL = cotDesc.getSAML2ReaderServiceURL();
            }
        } catch (COTException ce) {
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "Error retreiving circle of trust", ce);
            }
        } catch (SAML2Exception s2e) {
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "Error getting reader URL : ", s2e);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "Error getting reader URL : ", e);
            }
        }
        return readerURL;
    }

    /**
     * Returns the Request URL.
     * The getRequestURL does not alway returns the correct url
     * so this method builds the URL by retrieving the protocol,port
     * host name and deploy descriptor.
     *
     * @param request the <code>HttpServletRequest</code>.
     * @return the Request URL string.
     */
    public static String getBaseURL(HttpServletRequest request) {
        String protocol = request.getScheme();
        String host = request.getHeader("Host");
        if (host == null) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        String baseURL = protocol + "://" + host + "/";
        String requestURL = request.getRequestURL().toString();
        String tmpurl = null;
        if (protocol.equals("http")) {
            tmpurl = requestURL.substring(8);
        } else {
            tmpurl = requestURL.substring(9);
        }
        int startIndex = tmpurl.indexOf("/") + 1;
        String deployDesc = tmpurl.substring(startIndex);
        if (deployDesc != null && deployDesc.length() != 0) {
            baseURL += deployDesc;
        }
        return baseURL;
    }

    /**
     * Returns the Identity Provider Entity Identifier.
     * This method retrieves the _saml_idp query parameter
     * from the request and parses it to get the idp entity
     * id. If there are more then one idps then the last
     * one is the preferred idp.
     *
     * @param request the <code>HttpServletRequest</code> .
     * @return the identity provider entity identifier String.
     */
    public static String getPreferredIDP(HttpServletRequest request) {
        String idpList = request.getParameter(
                IDPDiscoveryConstants.SAML2_COOKIE_NAME);
        String idpEntityID = null;
        if ((idpList != null) && (idpList.length() > 0)) {
            idpList = idpList.trim();
            StringTokenizer st = new StringTokenizer(idpList, " ");
            String preferredIDP = null;
            while (st.hasMoreTokens()) {
                preferredIDP = st.nextToken();
            }
            try {
                byte[] byteArray = Base64.decode(preferredIDP);
                idpEntityID = new String(byteArray);
            } catch (Exception e) {
                debug.message("Error decoding : ", e);
            }
        }
        return idpEntityID;
    }


    /**
     * Returns the redirect URL.
     * This methods returns the complete reader redirect url.
     * The RelayState and requestId parameter are appended to
     * the URL to redirection back to the spSSOInit jsp.
     *
     * @param readerURL the readerURL to redirect to.
     * @param requestID the unique identifier to identify the request.
     * @param request   the HttpServletRequest.
     * @return redirectURL the URL to redirect to.
     */
    public static String getRedirectURL(String readerURL, String requestID,
                                        HttpServletRequest request) {
        StringBuilder redirectURLBuilder = new StringBuilder()
                .append(readerURL)
                .append("?RelayState=");
        String baseURL = getBaseURL(request);
        StringBuilder returnURLBuilder = new StringBuilder().append(baseURL);
        if (returnURLBuilder.indexOf("?") == -1) {
            returnURLBuilder.append("?");
        } else {
            returnURLBuilder.append("&");
        }
        returnURLBuilder.append("requestID=").append(requestID);
        String returnURLString = URLEncDec.encode(returnURLBuilder.toString());
        redirectURLBuilder.append(returnURLString);

        return redirectURLBuilder.toString();
    }


    /**
     * Returns an <code>IDPAccountMapper</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>IDPAccountMapper</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static IDPAccountMapper getIDPAccountMapper(
            String realm, String idpEntityID)
            throws SAML2Exception {
        String classMethod = "SAML2Utils.getIDPAccountMapper: ";
        String idpAccountMapperName = null;
        IDPAccountMapper idpAccountMapper = null;
        try {
            idpAccountMapperName = getAttributeValueFromSSOConfig(
                    realm, idpEntityID, SAML2Constants.IDP_ROLE,
                    SAML2Constants.IDP_ACCOUNT_MAPPER);
            if (idpAccountMapperName == null) {
                idpAccountMapperName =
                        SAML2Constants.DEFAULT_IDP_ACCOUNT_MAPPER_CLASS;
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "use " +
                            SAML2Constants.DEFAULT_IDP_ACCOUNT_MAPPER_CLASS);
                }
            }
            idpAccountMapper = (IDPAccountMapper)
                    IDPCache.idpAccountMapperCache.get(
                            idpAccountMapperName);
            if (idpAccountMapper == null) {
                idpAccountMapper = (IDPAccountMapper)
                        Class.forName(idpAccountMapperName).newInstance();
                IDPCache.idpAccountMapperCache.put(
                        idpAccountMapperName, idpAccountMapper);
            } else {
                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                            "got the IDPAccountMapper from cache");
                }
            }
        } catch (Exception ex) {
            debug.error(classMethod +
                    "Unable to get IDP Account Mapper.", ex);
            throw new SAML2Exception(ex);
        }

        return idpAccountMapper;
    }

    /**
     * Returns a <code>SAML2IdentityProviderAdapter</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>SAML2IdentityProviderAdapter</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static SAML2IdentityProviderAdapter getIDPAdapterClass(
            String realm, String idpEntityID)
            throws SAML2Exception {
        String classMethod = "SAML2Utils.getIDPAdapterClass: ";
        String idpAdapterName = null;
        SAML2IdentityProviderAdapter idpAdapter = null;
        try {
            idpAdapterName = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(
                    realm, idpEntityID, SAML2Constants.IDP_ADAPTER_CLASS);
            if (idpAdapterName == null || idpAdapterName.trim().isEmpty()) {
                idpAdapterName = SAML2Constants.DEFAULT_IDP_ADAPTER;
                if (debug.messageEnabled()) {
                    debug.message(classMethod + " uses " + SAML2Constants.DEFAULT_IDP_ADAPTER);
                }
            }

            // Attempt to retrieve the adapter from the cache
            idpAdapter = (SAML2IdentityProviderAdapter) IDPCache.idpAdapterCache.get(realm + "$" + idpEntityID + "$" + idpAdapterName);

            if (idpAdapter == null) {
                // NB: multiple threads may cause several adapter objects to be created
                idpAdapter = (SAML2IdentityProviderAdapter) Class.forName(idpAdapterName).newInstance();
                idpAdapter.initialize(idpEntityID, realm);

                // Add the adapter to the cache after initialization
                IDPCache.idpAdapterCache.put(realm + "$" + idpEntityID + "$" + idpAdapterName, idpAdapter);
            } else {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + " got the IDPAdapter from cache");
                }
            }
        } catch (Exception ex) {
            debug.error(classMethod + " unable to get IDP Adapter.", ex);
            throw new SAML2Exception(ex);
        }

        return idpAdapter;
    }


    /**
     * Returns an <code>SP</code> adapter class
     *
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name
     * @return the <code>SP</code> adapter class
     * @throws SAML2Exception if the operation is not successful
     */
    public static SAML2ServiceProviderAdapter getSPAdapterClass(
            String spEntityID, String realm)
            throws SAML2Exception {
        String classMethod = "SAML2Utils.getSPAdapterClass: ";
        if (debug.messageEnabled()) {
            debug.message(classMethod +
                    "get SPAdapter for " + spEntityID + " under realm " + realm);
        }
        String spAdapterClassName = null;
        SAML2ServiceProviderAdapter spAdapterClass = null;
        try {
            spAdapterClassName = getAttributeValueFromSSOConfig(
                    realm, spEntityID, SAML2Constants.SP_ROLE,
                    SAML2Constants.SP_ADAPTER_CLASS);
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "get SPAdapter class " + spAdapterClassName);
            }
            if ((spAdapterClassName != null) &&
                    (spAdapterClassName.length() != 0)) {
                spAdapterClass = (SAML2ServiceProviderAdapter)
                        SPCache.spAdapterClassCache.get(realm + spEntityID +
                                spAdapterClassName);
                if (spAdapterClass == null) {
                    spAdapterClass = (SAML2ServiceProviderAdapter)
                            Class.forName(spAdapterClassName).newInstance();
                    List env = getAllAttributeValueFromSSOConfig(
                            realm, spEntityID, SAML2Constants.SP_ROLE,
                            SAML2Constants.SP_ADAPTER_ENV);
                    Map map = parseEnvList(env);
                    map.put(SAML2ServiceProviderAdapter.HOSTED_ENTITY_ID,
                            spEntityID);
                    map.put(SAML2ServiceProviderAdapter.REALM, realm);
                    spAdapterClass.initialize(map);
                    SPCache.spAdapterClassCache.put(
                            realm + spEntityID + spAdapterClassName,
                            spAdapterClass);
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "create new SPAdapter " + spAdapterClassName +
                                " for " + spEntityID + " under realm " + realm);
                    }
                } else {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "got the SPAdapter " + spAdapterClassName +
                                " from cache");
                    }
                }
            }
        } catch (InstantiationException ex) {
            debug.error(classMethod +
                    "Unable to get SP Adapter class instance.", ex);
            throw new SAML2Exception(ex);
        } catch (ClassNotFoundException ex) {
            debug.error(classMethod +
                    "SP Adapter class not found.", ex);
            throw new SAML2Exception(ex);
        } catch (IllegalAccessException ex) {
            debug.error(classMethod +
                    "Unable to get SP Adapter class.", ex);
            throw new SAML2Exception(ex);
        }

        return spAdapterClass;
    }

    /**
     * Returns a <code>Fedlet</code> adapter class.
     *
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name
     * @return the <code>Fedlet</code> adapter class
     * @throws SAML2Exception if the operation is not successful
     */
    public static FedletAdapter getFedletAdapterClass(
            String spEntityID, String realm)
            throws SAML2Exception {
        String classMethod = "SAML2Utils.getFedletAdapterClass: ";
        if (debug.messageEnabled()) {
            debug.message(classMethod +
                    "get FedletAdapter for " + spEntityID + " under realm " + realm);
        }
        String fedletAdapterClassName = null;
        FedletAdapter fedletAdapterClass = null;
        try {
            fedletAdapterClassName = getAttributeValueFromSSOConfig(
                    realm, spEntityID, SAML2Constants.SP_ROLE,
                    SAML2Constants.FEDLET_ADAPTER_CLASS);
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "get FedletAdapter class " + fedletAdapterClassName);
            }
            if ((fedletAdapterClassName != null) &&
                    (fedletAdapterClassName.length() != 0)) {
                fedletAdapterClass = (FedletAdapter)
                        SPCache.fedletAdapterClassCache.get(realm + spEntityID +
                                fedletAdapterClassName);
                if (fedletAdapterClass == null) {
                    fedletAdapterClass = (FedletAdapter)
                            Class.forName(fedletAdapterClassName).newInstance();
                    List env = getAllAttributeValueFromSSOConfig(
                            realm, spEntityID, SAML2Constants.SP_ROLE,
                            SAML2Constants.FEDLET_ADAPTER_ENV);
                    Map map = parseEnvList(env);
                    map.put(FedletAdapter.HOSTED_ENTITY_ID,
                            spEntityID);
                    fedletAdapterClass.initialize(map);
                    SPCache.fedletAdapterClassCache.put(
                            realm + spEntityID + fedletAdapterClassName,
                            fedletAdapterClass);
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "create new FedletAdapter " +
                                fedletAdapterClassName +
                                " for " + spEntityID + " under realm " + realm);
                    }
                } else {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "got the FedletAdapter " + fedletAdapterClassName +
                                " from cache");
                    }
                }
            }
        } catch (InstantiationException ex) {
            debug.error(classMethod +
                    "Unable to get Fedlet Adapter class instance.", ex);
            throw new SAML2Exception(ex);
        } catch (ClassNotFoundException ex) {
            debug.error(classMethod +
                    "Fedlet Adapter class not found.", ex);
            throw new SAML2Exception(ex);
        } catch (IllegalAccessException ex) {
            debug.error(classMethod +
                    "Unable to get Fedlet Adapter class.", ex);
            throw new SAML2Exception(ex);
        }

        return fedletAdapterClass;
    }

    /**
     * Returns map based on A/V pair.
     */
    private static Map<String, String> parseEnvList(List<String> list) {
        Map<String, String> map = new HashMap<>();
        if ((list == null) || (list.isEmpty())) {
            return map;
        }

        for (String val : list) {
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils.parseEnvList : processing " + val);
            }
            if ((val == null) || (val.length() == 0)) {
                continue;
            }
            int pos = val.indexOf("=");
            if (pos == -1) {
                if (debug.warningEnabled()) {
                    debug.warning("SAML2Utils.parseEnvList : invalid value : "
                            + val + ". Value must be in key=value format.");
                }
            } else {
                map.put(val.substring(0, pos), val.substring(pos + 1));
            }
        }
        return map;
    }

    /**
     * Returns an <code>SPAccountMapper</code>
     *
     * @param realm      the realm name
     * @param spEntityID the entity id of the service provider
     * @return the <code>SPAccountMapper</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static SPAccountMapper getSPAccountMapper(
            String realm, String spEntityID)
            throws SAML2Exception {
        String classMethod = "SAML2Utils.getSPAccountMapper: ";
        String spAccountMapperName = null;
        SPAccountMapper spAccountMapper = null;
        try {
            spAccountMapperName = getAttributeValueFromSSOConfig(
                    realm, spEntityID, SAML2Constants.SP_ROLE,
                    SAML2Constants.SP_ACCOUNT_MAPPER);
            if (spAccountMapperName == null) {
                spAccountMapperName =
                        SAML2Constants.DEFAULT_SP_ACCOUNT_MAPPER_CLASS;
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "use " +
                            SAML2Constants.DEFAULT_SP_ACCOUNT_MAPPER_CLASS);
                }
            }
            spAccountMapper = (SPAccountMapper)
                    SPCache.spAccountMapperCache.get(spAccountMapperName);
            if (spAccountMapper == null) {
                spAccountMapper = (SPAccountMapper)
                        Class.forName(spAccountMapperName).newInstance();
                SPCache.spAccountMapperCache.put(
                        spAccountMapperName, spAccountMapper);
            } else {
                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                            "got the SPAccountMapper from cache");
                }
            }
        } catch (Exception ex) {
            debug.error(classMethod +
                    "Unable to get SP Account Mapper.", ex);
            throw new SAML2Exception(ex);
        }

        return spAccountMapper;
    }

    /**
     * Returns an <code>SAML2IDPFinder</code> which is used to find a list
     * of IDP's for ECP Request.
     *
     * @param realm      the realm name
     * @param spEntityID the entity id of the service provider
     * @return the <code>SAML2IDPFinder</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static SAML2IDPFinder getECPIDPFinder(String realm,
                                                 String spEntityID) throws SAML2Exception {
        String classMethod = "SAML2Utils.getECPIDPFinder: ";
        String implClassName = null;
        SAML2IDPFinder ecpRequestIDPListFinder = null;
        try {
            implClassName = getAttributeValueFromSSOConfig(
                    realm, spEntityID, SAML2Constants.SP_ROLE,
                    SAML2Constants.ECP_REQUEST_IDP_LIST_FINDER_IMPL);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "use " + implClassName);
            }
            if ((implClassName == null) ||
                    (implClassName.trim().length() == 0)) {
                return null;
            }

            ecpRequestIDPListFinder = (SAML2IDPFinder)
                    SPCache.ecpRequestIDPListFinderCache.get(implClassName);
            if (ecpRequestIDPListFinder == null) {
                ecpRequestIDPListFinder = (SAML2IDPFinder)
                        Class.forName(implClassName).newInstance();
                SPCache.ecpRequestIDPListFinderCache.put(
                        implClassName, ecpRequestIDPListFinder);
            } else {
                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                            "got the ECP Request IDP List Finder from cache");
                }
            }
        } catch (Exception ex) {
            if (debug.warningEnabled()) {
                debug.warning(classMethod +
                        "Unable to get ECP Request IDP List Finder.", ex);
            }
        }

        return ecpRequestIDPListFinder;
    }

    /**
     * Returns the URL to which redirection will happen after
     * Single-Signon / Federation. This methods checks the
     * following parameters to determine the Relay State.
     * 1. The "RelayState" query parameter in the request.
     * 2. The "RelayStateAlias" query parameter in the
     * request which is used in the absence of the
     * RelayState parameter to determine which query parameter
     * to use if no "RelayState" query paramerter is present.
     * 3. The "goto" query parameter if present is the default
     * RelayState in the absence of the above.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @return the value of the URL to which to redirect on
     * successful Single-SignOn  / Federation.
     */
    public static String getRelayState(HttpServletRequest request) {
        String relayState =
                request.getParameter(SAML2Constants.RELAY_STATE);
        if (StringUtils.isEmpty(relayState)) {
            String relayStateAlias = request.getParameter(SAML2Constants.RELAY_STATE_ALIAS);
            if (relayStateAlias != null && relayStateAlias.length() > 0) {
                StringTokenizer st = new StringTokenizer(relayStateAlias, "|");
                while (st.hasMoreTokens()) {
                    String tmp = st.nextToken();
                    relayState = request.getParameter(tmp);
                    if (StringUtils.isNotEmpty(relayState)) {
                        break;
                    }
                }
            }
            if (relayState == null) {
                // check if goto parameter is there.
                relayState = request.getParameter(SAML2Constants.GOTO);
            }
        }
        return relayState;
    }

    /**
     * Compares the destination and location
     *
     * @param destination Destination
     * @param location    the URL from the meta
     * @return <code>true</code> if the input are the same,
     * otherwise, return <code>false</code>
     */
    public static boolean verifyDestination(String destination,
                                            String location) {
        /* Note: 
        Here we assume there is one endpoint per protocol. In future,
        we may support more than one endpoint per protocol. The caller 
        code should change accordingly. 
        */
        return ((location != null) && (location.length() != 0) &&
                (destination != null) && (destination.length() != 0) &&
                (location.equalsIgnoreCase(destination)));
    }

    /**
     * Retrieves SAE related attributes from exended metadata.
     *
     * @param realm    realm the FM provider is in
     * @param entityId the entity ID of the FM provider
     * @param role     Role of the FM provider
     * @param appUrl   application url
     * @return Map containing SAE parameters or null in case of error.
     */
    public static Map getSAEAttrs(
            String realm, String entityId, String role, String appUrl) {
        if (appUrl == null || appUrl.length() == 0) {
            return null;
        }
        try {
            IDPSSOConfigElement idpConfig = null;
            SPSSOConfigElement spConfig = null;
            Map attrs = null;

            if (role.equalsIgnoreCase(SAML2Constants.SP_ROLE)) {
                spConfig =
                        saml2MetaManager.getSPSSOConfig(realm, entityId);
                if (spConfig == null) {
                    return null;
                }
                attrs = SAML2MetaUtils.getAttributes(spConfig);
            } else {
                idpConfig =
                        saml2MetaManager.getIDPSSOConfig(realm, entityId);
                if (idpConfig == null) {
                    debug.message("SAML2Utils.getSAEAttrs: idpconfig is null");
                    return null;
                }
                attrs = SAML2MetaUtils.getAttributes(idpConfig);
            }

            if (attrs == null) {
                debug.message("SAML2Utils.getSAEAttrs: no extended attrs");
                return null;
            }
            List values = (List) attrs.get(SAML2Constants.SAE_APP_SECRET_LIST);
            if (values != null && values.size() != 0) {
                Iterator iter = values.iterator();
                while (iter.hasNext()) {
                    String value = (String) iter.next();
                    if (debug.messageEnabled()) {
                        debug.message("SAML2Utils.getSAEAttrs: value=" + value);
                    }

                    StringTokenizer st = new StringTokenizer(value, "|");
                    HashMap hp = null;
                    while (st.hasMoreTokens()) {
                        String tok = st.nextToken();
                        int idx = tok.indexOf("=");
                        String name = tok.substring(0, idx);
                        String val = tok.substring(idx + 1, tok.length());
                        if (debug.messageEnabled()) {
                            debug.message("SAML2Utils.getSAEAttrs: tok:name="
                                    + name + " val=" + val);
                        }
                        if (SAML2Constants.SAE_XMETA_URL.equals(name)) {
                            if (appUrl.startsWith(val)) {
                                hp = new HashMap();
                            } else {
                                break;
                            }
                        } else if (SAML2Constants.SAE_XMETA_SECRET.equals(name)) {
                            val = SAMLUtilsCommon.decodePassword(val);
                        }
                        hp.put(name, val);
                    }
                    if (hp != null) {
                        String alias = SAML2Utils.getSigningCertAlias(
                                realm, entityId, role);
                        if (alias != null)
                            hp.put(SAML2Constants.SAE_XMETA_PKEY_ALIAS, alias);
                        if (debug.messageEnabled()) {
                            debug.message(
                                    "SAML2Utils.getSAEAttrs: PKEY=" + alias + ":");
                        }
                        return hp;
                    }
                }
            }
        } catch (SAML2MetaException e) {
            debug.message("get SSOConfig failed:", e);
        }
        return null;
    }

    /**
     * Obtains the value of NameID from Response.
     *
     * @param response <code>Response</code> object
     * @return value of the NameID from the first Assertion in the response.
     * null if the response is null, or no assertion in the response, or
     * no NameID in the assertion.
     */
    public static String getNameIDStringFromResponse(Response response) {
        if (response != null) {
            List assertions = response.getAssertion();
            if ((assertions != null) && (assertions.size() > 0)) {
                Assertion assertion = (Assertion) assertions.get(0);
                Subject subject = assertion.getSubject();
                if (subject != null) {
                    NameID nameID = subject.getNameID();
                    if (nameID != null) {
                        return nameID.getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Writes  a log record in SAML2 access log.
     * (fmSAML2.access)
     *
     * @param lvl    indicating log level
     * @param msgid  Message id
     * @param data   string array of dynamic data only known during run time
     * @param tok    Session of authenticated user
     * @param ipaddr IP Address.
     * @param userid User Id.
     * @param org    Organization.
     * @param module Module Name.
     * @param props  log record columns - used if tok is not available
     *               to specify log record columns such as ip address, realm, etc
     */
    public static void logAccess(Level lvl, String msgid,
                                 String[] data, Object tok,
                                 String ipaddr, String userid,
                                 String org, String module, Map props) {
        Map accProps = accumulateLogProps(
                ipaddr, userid, org, module, props);
        LogUtil.access(lvl, msgid, data, tok, accProps);
    }

    /**
     * Writes error occurred in SAML2 component into a log
     * (fmSAML2.error)
     *
     * @param lvl    indicating log level
     * @param msgid  Message id
     * @param data   string array of dynamic data only known during run time
     * @param tok    Session of authenticated user
     * @param ipaddr IP Address
     * @param userid User Id
     * @param org    Organization
     * @param module Module Name
     * @param props  log record columns - used if tok is not available
     *               to specify log record columns such as ip address, realm, etc
     */
    public static void logError(
            Level lvl,
            String msgid,
            String[] data,
            Object tok,
            String ipaddr,
            String userid,
            String org,
            String module,
            Map props) {
        Map accProps = accumulateLogProps(
                ipaddr, userid, org, module, props);
        LogUtil.error(lvl, msgid, data, tok, accProps);
    }

    private static Map accumulateLogProps(
            String ipaddr,
            String userid,
            String org,
            String module,
            Map props) {
        if (props == null) {
            props = new HashMap();
        }
        if (ipaddr != null) {
            props.put(LogUtil.IP_ADDR, ipaddr);
        }
        if (userid != null) {
            props.put(LogUtil.LOGIN_ID, userid);
        }
        if (org != null) {
            props.put(LogUtil.DOMAIN, org);
        }
        if (module != null) {
            props.put(LogUtil.MODULE_NAME, module);
        }
        return props;
    }

    /**
     * Returns the value of attribute from entity configuration.
     *
     * @param realm      the realm of the entity.
     * @param entityRole role of the entity (PEP or PDP).
     * @param entityID   identity of the entity.
     * @param attrName   name of attribute whose value is to be retreived.
     * @return value of the attribute.
     */
    public static String getAttributeValueFromXACMLConfig(
            String realm,
            String entityRole,
            String entityID,
            String attrName
    ) {
        String method = "SAML2Utils:getAttributeValueFromXACMLConfig : ";
        if (debug.messageEnabled()) {
            debug.message(method + "realm - " + realm);
            debug.message(method + "entityRole - " + entityRole);
            debug.message(method + "EntityId - " + entityID);
            debug.message(method + "attrName - " + attrName);
        }
        String result = null;
        try {
            XACMLAuthzDecisionQueryConfigElement pepConfig = null;
            XACMLPDPConfigElement pdpConfig = null;
            Map attrs = null;
            if (entityRole.equalsIgnoreCase(SAML2Constants.PEP_ROLE)) {
                pepConfig = saml2MetaManager.getPolicyEnforcementPointConfig(
                        realm, entityID);
                if (pepConfig != null) {
                    attrs = SAML2MetaUtils.getAttributes(pepConfig);
                }
            } else {
                pdpConfig =
                        saml2MetaManager.getPolicyDecisionPointConfig(realm,
                                entityID);
                if (pdpConfig != null) {
                    attrs = SAML2MetaUtils.getAttributes(pdpConfig);
                }
            }

            if (attrs != null) {
                List value = (List) attrs.get(attrName);
                if (value != null && value.size() != 0) {
                    result = (String) value.get(0);
                }
            }
        } catch (SAML2MetaException e) {
            debug.message("Retreiving XACML Config failed:", e);
        }
        if (debug.messageEnabled()) {
            debug.message("Attribute value is : " + result);
        }
        return result;
    }

    /**
     * Returns true if wantXACMLAuthzDecisionQuerySigned has
     * <code>true</code> true.
     *
     * @param realm      realm of hosted entity.
     * @param entityID   name of hosted entity.
     * @param entityRole role of hosted entity.
     * @return true if wantXACMLAuthzDecisionQuerySigned
     * has <code>String</code> true.
     */
    public static boolean getWantXACMLAuthzDecisionQuerySigned(String realm,
                                                               String entityID,
                                                               String entityRole) {
        if (debug.messageEnabled()) {
            String method = "getWantArtifactResponseSigned : ";
            debug.message(method + "realm - " + realm);
            debug.message(method + "entityID - " + entityID);
            debug.message(method + "entityRole - " + entityRole);
        }
        String wantSigned =
                getAttributeValueFromXACMLConfig(realm, entityRole, entityID,
                        SAML2Constants.WANT_XACML_AUTHZ_DECISION_QUERY_SIGNED);
        if (wantSigned == null) {
            wantSigned = "false";
        }

        return wantSigned.equalsIgnoreCase("true") ? true : false;
    }

    /**
     * Checks certificate validity with configured CRL
     *
     * @param cert x509 certificate
     * @return <code>true</code> if the certificate is not in CRL,
     * otherwise, return <code>false</code>
     */
    public static boolean validateCertificate(X509Certificate cert) {
        String method = "validateCertificate : ";
        boolean certgood = true;
        if (!checkCertStatus) {
            if (debug.messageEnabled()) {
                debug.message(method +
                        " CRL check is not configured. Just return it is good.");
            }
            return certgood;
        }

        certgood =
                CRLValidator.validateCertificate(cert, checkCAStatus);
        if (debug.messageEnabled()) {
            debug.message(method + " certificate is validated to " + certgood);
        }

        return certgood;
    }

    /**
     * Gets the <code>SPAttributeMapper</code>.
     *
     * @param realm      The realm the SP belongs to.
     * @param spEntityID The entity ID of the SP.
     * @return The {@link SPAttributeMapper} defined in the configuration.
     * @throws SAML2Exception if the processing failed.
     */
    public static SPAttributeMapper getSPAttributeMapper(String realm, String spEntityID) throws SAML2Exception {
        String classMethod = "SAML2Utils.getSPAttributeMapper: ";
        String spAttributeMapperName;
        try {
            spAttributeMapperName = getAttributeValueFromSSOConfig(realm, spEntityID, SAML2Constants.SP_ROLE,
                    SAML2Constants.SP_ATTRIBUTE_MAPPER);
            if (spAttributeMapperName == null) {
                spAttributeMapperName = SAML2Constants.DEFAULT_SP_ATTRIBUTE_MAPPER_CLASS;
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod + "using " + spAttributeMapperName);
            }
            return Class.forName(spAttributeMapperName).asSubclass(SPAttributeMapper.class).newInstance();
        } catch (Exception ex) {
            debug.error(classMethod + "Unable to get SP Attribute Mapper.", ex);
            throw new SAML2Exception(ex);
        }
    }

    /**
     * Returns the attribute map by parsing the configured map in hosted
     * provider configuration
     *
     * @param realm        realm name.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @return a map of local attributes configuration map.
     * This map will have a key as the SAML attribute name and the value
     * is the local attribute.
     * @throws <code>SAML2Exception</code> if any failured.
     */
    public static Map getConfigAttributeMap(String realm, String hostEntityID,
                                            String role) throws SAML2Exception {

        if (realm == null) {
            throw new SAML2Exception(bundle.getString("nullRealm"));
        }

        if (hostEntityID == null) {
            throw new SAML2Exception(bundle.getString("nullHostEntityID"));
        }

        if (debug.messageEnabled()) {
            debug.message("SAML2Utils.getConfigAttributeMap: DefaultAttrMapper: relam="
                    + realm + ", entity id="
                    + hostEntityID + ", role=" + role);
        }
        try {
            BaseConfigType config = null;
            if (role.equals(SAML2Constants.SP_ROLE)) {
                config = saml2MetaManager.getSPSSOConfig(realm, hostEntityID);
            } else if (role.equals(SAML2Constants.IDP_ROLE)) {
                config = saml2MetaManager.getIDPSSOConfig(realm, hostEntityID);
            }


            if (config == null) {
                if (debug.warningEnabled()) {
                    debug.warning("SAML2Utils.getConfigAttributeMap: configuration is not defined.");
                }
                return Collections.EMPTY_MAP;
            }

            Map<String, List<String>> attributeConfig = SAML2MetaUtils.getAttributes(config);
            List<String> mappedAttributes = attributeConfig.get(SAML2Constants.ATTRIBUTE_MAP);

            if (mappedAttributes == null || mappedAttributes.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils.getConfigAttributeMap:Attribute map is not defined for entity: " +
                            hostEntityID);
                }
                return Collections.EMPTY_MAP;
            }

            return getMappedAttributes(mappedAttributes);

        } catch (SAML2MetaException sme) {
            debug.error("SAML2Utils.getConfigAttributeMap: ", sme);
            throw new SAML2Exception(sme.getMessage());

        }
    }

    /**
     * For the list of Strings containing mappings, return a map of name value pairs that match the mapping string
     * @param mappedAttributes a non-null list of strings in the form of name=value or name="static value"
     * @return a Map of name value pairs keyed off of the mapping name from the mappedAttributes list
     */
    public static Map<String, String> getMappedAttributes(List<String> mappedAttributes) {

        if (mappedAttributes == null) {
            return Collections.emptyMap();
        }

        Map<String, String> attributeMap = new HashMap<>(mappedAttributes.size());

        for (String entry : mappedAttributes) {
            int equalsLoc = entry.indexOf("=");
            if (equalsLoc == -1) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils.getMappedAttributes: Invalid entry: " + entry);
                }
                continue;
            }

            attributeMap.put(entry.substring(0, equalsLoc), entry.substring(equalsLoc + 1));
        }

        return attributeMap;
    }

    /**
     * Returns the SAML <code>Attribute</code> object.
     *
     * @param name   attribute name.
     * @param values attribute values.
     * @throws SAML2Exception if any failure.
     */
    public static Attribute getSAMLAttribute(final String name, final String[] values)
            throws SAML2Exception {

        if (name == null) {
            throw new SAML2Exception(bundle.getString("nullInput"));
        }

        AssertionFactory factory = AssertionFactory.getInstance();
        Attribute attribute = factory.createAttribute();

        //samlAttribute might be in format: NameFormat|Name
        int pipePos = name.indexOf('|');
        String realName = null;
        String nameFormat = null;
        if (pipePos != -1) {
            if (pipePos < name.length() - 1) {
                nameFormat = name.substring(0, pipePos);
                realName = name.substring(pipePos + 1);
            } else {
                //TO DO: Put the message in the bundle libSAML2_XX.properties
                throw new SAML2Exception("Wrong format of the attribute Name");
            }
        } else {
            realName = name;
            nameFormat = SAML2Constants.BASIC_NAME_FORMAT;
        }
        attribute.setName(realName);
        attribute.setNameFormat(nameFormat);

        if (values != null) {
            List<String> list = new ArrayList<>();
            for (String value : values) {
                list.add(XMLUtils.escapeSpecialCharacters(value));
            }
            attribute.setAttributeValueString(list);
        }
        return attribute;
    }

    public static void postToTarget(HttpServletRequest request, HttpServletResponse response,
                                    String SAMLmessageName, String SAMLmessageValue, String relayStateName,
                                    String relayStateValue, String targetURL) throws SAML2Exception {

        request.setAttribute("TARGET_URL", ESAPI.encoder().encodeForHTML(targetURL));
        request.setAttribute("SAML_MESSAGE_NAME", ESAPI.encoder().encodeForHTML(SAMLmessageName));
        request.setAttribute("SAML_MESSAGE_VALUE", ESAPI.encoder().encodeForHTML(SAMLmessageValue));
        request.setAttribute("RELAY_STATE_NAME", ESAPI.encoder().encodeForHTML(relayStateName));
        request.setAttribute("RELAY_STATE_VALUE", ESAPI.encoder().encodeForHTML(relayStateValue));
        request.setAttribute("SAML_POST_KEY", bundle.getString("samlPostKey"));

        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache,no-store");

        try {
            request.getRequestDispatcher("/saml2/jsp/autosubmitaccessrights.jsp").forward(request, response);
        } catch (ServletException sE) {
            handleForwardException(sE);
        } catch (IOException ioE) {
            handleForwardException(ioE);
        }
    }

    /**
     * Handles any exception when attempting to forward.
     *
     * @param exception Thrown and caught exception
     * @throws SAML2Exception Single general exception that is thrown on
     */
    private static void handleForwardException(Exception exception) throws SAML2Exception {
        debug.error("Failed to forward to auto submitting JSP", exception);
        throw new SAML2Exception(bundle.getString("postToTargetFailed"));
    }

    /**
     * Verifies specified name ID format and returns it. If specified name ID
     * format is empty, returns name ID foramt supported by both IDP and SP.
     *
     * @param nameIDFormat name ID format.
     * @param spsso        SP meta data desciptor.
     * @param idpsso       IDP meta data desciptor.
     * @throws SAML2Exception if name ID format is not supported.
     */
    public static String verifyNameIDFormat(String nameIDFormat,
                                            SPSSODescriptorElement spsso, IDPSSODescriptorElement idpsso)
            throws SAML2Exception {

        List spNameIDFormatList = spsso.getNameIDFormat();

        List idpNameIDFormatList = null;
        // idpsso is null for ECP case
        if (idpsso != null) {
            idpNameIDFormatList = idpsso.getNameIDFormat();
        }

        if ((nameIDFormat == null) || (nameIDFormat.length() == 0)) {

            if ((idpNameIDFormatList == null) ||
                    (idpNameIDFormatList.isEmpty())) {

                if ((spNameIDFormatList == null) ||
                        (spNameIDFormatList.isEmpty())) {

                    return SAML2Constants.PERSISTENT;
                } else {
                    return (String) spNameIDFormatList.get(0);
                }
            } else {
                if ((spNameIDFormatList == null) ||
                        (spNameIDFormatList.isEmpty())) {

                    return (String) idpNameIDFormatList.get(0);
                } else {
                    nameIDFormat = null;
                    for (Iterator iter = spNameIDFormatList.iterator();
                         iter.hasNext(); ) {
                        String spNameIDFormat = (String) iter.next();
                        if (idpNameIDFormatList.contains(spNameIDFormat)) {
                            nameIDFormat = spNameIDFormat;
                            break;
                        }
                    }
                    if (nameIDFormat == null) {
                        throw new SAML2Exception(bundle.getString(
                                "unsupportedNameIDFormatIDPSP"));
                    }
                }
            }
        } else {
            if (nameIDFormat.equals("persistent") ||
                    nameIDFormat.equals("transient")) {
                nameIDFormat = SAML2Constants.NAMEID_FORMAT_NAMESPACE +
                        nameIDFormat;
            }

            if ((spNameIDFormatList != null) && (!spNameIDFormatList.isEmpty())
                    && (!spNameIDFormatList.contains(nameIDFormat))) {

                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils.verifyNameIDFormat: " +
                            "NameIDFormat not supported by SP: " + nameIDFormat);
                }
                Object[] args = {nameIDFormat};
                throw new SAML2Exception(BUNDLE_NAME,
                        "unsupportedNameIDFormatSP", args);
            }

            if ((idpNameIDFormatList != null) &&
                    (!idpNameIDFormatList.isEmpty()) &&
                    (!idpNameIDFormatList.contains(nameIDFormat))) {

                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils.verifyNameIDFormat: " +
                            "NameIDFormat not supported by IDP: " + nameIDFormat);
                }
                Object[] args = {nameIDFormat};
                throw new SAML2Exception(BUNDLE_NAME,
                        "unsupportedNameIDFormatIDP", args);
            }
        }

        return nameIDFormat;
    }

    /**
     * Returns true if the specified AuthnContextClassRef matches a list of
     * requested AuthnContextClassRef.
     *
     * @param requestedACClassRefs a list of requested AuthnContextClassRef's
     * @param acClassRef           AuthnContextClassRef
     * @param comparison           the type of comparison
     * @param acClassRefLevelMap   a AuthnContextClassRef to AuthLevel map. Key
     *                             is AuthnContextClassRef in <code>String</code> and value is
     *                             AuthLevel in <code>Integer</code>
     * @return true if the specified AuthnContextClassRef matches a list of
     * requested AuthnContextClassRef
     */
    public static boolean isAuthnContextMatching(List requestedACClassRefs,
                                                 String acClassRef, String comparison, Map acClassRefLevelMap) {

        Integer levelInt = (Integer) acClassRefLevelMap.get(acClassRef);
        if (levelInt == null) {
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils.isAuthnContextMatching: " +
                        "AuthnContextClassRef " + acClassRef + " is not supported.");
            }
            return false;
        }
        int level = levelInt.intValue();

        if ((comparison == null) || (comparison.length() == 0) ||
                (comparison.equals("exact"))) {
            for (Iterator iter = requestedACClassRefs.iterator();
                 iter.hasNext(); ) {

                String requstedACClassRef = (String) iter.next();
                if (requstedACClassRef.equals(acClassRef)) {
                    return true;
                }
            }
            return false;
        }

        debug.message("SAML2Utils.isAuthnContextMatching: acClassRef = {}, level = {}, comparison = {}",
                acClassRef, level, comparison);
        if (comparison.equals("minimum")) {
            for (Iterator iter = requestedACClassRefs.iterator();
                 iter.hasNext(); ) {

                String requstedACClassRef = (String) iter.next();
                Integer requestedLevelInt =
                        (Integer) acClassRefLevelMap.get(requstedACClassRef);
                int requestedLevel = (requestedLevelInt == null) ?
                        0 : requestedLevelInt.intValue();

                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils." +
                            "isAuthnContextMatching: requstedACClassRef = " +
                            requstedACClassRef + ", level = " + requestedLevel);
                }

                if (level >= requestedLevel) {
                    return true;
                }
            }
            return false;
        } else if (comparison.equals("better")) {
            for (Iterator iter = requestedACClassRefs.iterator();
                 iter.hasNext(); ) {

                String requstedACClassRef = (String) iter.next();
                Integer requestedLevelInt =
                        (Integer) acClassRefLevelMap.get(requstedACClassRef);
                int requestedLevel = (requestedLevelInt == null) ?
                        0 : requestedLevelInt.intValue();

                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils." +
                            "isAuthnContextMatching: requstedACClassRef = " +
                            requstedACClassRef + ", level = " + requestedLevel);
                }

                if (level <= requestedLevel) {
                    return false;
                }
            }
            return true;
        } else if (comparison.equals("maximum")) {
            for (Iterator iter = requestedACClassRefs.iterator();
                 iter.hasNext(); ) {

                String requstedACClassRef = (String) iter.next();
                Integer requestedLevelInt =
                        (Integer) acClassRefLevelMap.get(requstedACClassRef);
                int requestedLevel = (requestedLevelInt == null) ?
                        0 : requestedLevelInt.intValue();

                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils." +
                            "isAuthnContextMatching: requstedACClassRef = " +
                            requstedACClassRef + ", level = " + requestedLevel);
                }

                if (level <= requestedLevel) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    /**
     * Processes logout for external application. This will do a back channel
     * HTTP POST to the external application logout URL with all the cookies
     * and selected session property as HTTP header.
     *
     * @param request      HttpServletRequest
     * @param appLogoutURL external application logout URL
     * @param session      session object of the user
     */
    public static void postToAppLogout(HttpServletRequest request,
                                       String appLogoutURL, Object session) {

        String method = "SAML2Utils.postToAppLogout: ";
        try {
            if ((appLogoutURL == null) || (appLogoutURL.length() == 0)) {
                return;
            }
            // actual application logout URL without the session 
            // property query parameter
            String logoutURL = appLogoutURL;
            // name of the session property
            String sessProp = null;
            // find out session property name from the URL
            int pos = appLogoutURL.indexOf(
                    SAML2Constants.APP_SESSION_PROPERTY + "=");
            if (pos != -1) {
                int endPos = appLogoutURL.indexOf("&", pos);
                if (endPos != -1) {
                    sessProp = appLogoutURL.substring(
                            pos + SAML2Constants.APP_SESSION_PROPERTY.length() + 1,
                            endPos);
                    logoutURL = appLogoutURL.substring(0, pos) +
                            appLogoutURL.substring(endPos + 1);
                } else {
                    sessProp = appLogoutURL.substring(
                            pos + SAML2Constants.APP_SESSION_PROPERTY.length() + 1);
                    logoutURL = appLogoutURL.substring(0, pos - 1);
                }
            }
            if (debug.messageEnabled()) {
                debug.message(method + "appLogoutURL=" + appLogoutURL +
                        ", real logoutURL=" + logoutURL +
                        ", session property name: " + sessProp);
            }

            URL url = new URL(logoutURL);
            HttpURLConnection conn =
                    HttpURLConnectionManager.getConnection(url);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            HttpURLConnection.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);

            // replay cookies
            String strCookies = getCookiesString(request);
            if (strCookies != null) {
                if (debug.messageEnabled()) {
                    debug.message(method + "Sending cookies : " + strCookies);
                }
                conn.setRequestProperty("Cookie", strCookies);
            }
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            // set header & content 
            StringBuffer buffer = new StringBuffer();
            buffer.append("");
            if ((sessProp != null) && (session != null)) {
                String[] values = SessionManager.getProvider().getProperty(
                        session, sessProp);
                if ((values != null) && (values.length != 0)) {
                    int i = 0;
                    while (true) {
                        conn.setRequestProperty(URLEncDec.encode(sessProp),
                                URLEncDec.encode(values[i]));
                        buffer.append(URLEncDec.encode(sessProp)).append('=');
                        buffer.append(URLEncDec.encode(values[i++]));
                        if (i != values.length) {
                            buffer.append('&');
                        } else {
                            break;
                        }
                    }
                }
            }
            if (debug.messageEnabled()) {
                debug.message(method + "Sending content: " + buffer.toString());
            }

            OutputStream outputStream = conn.getOutputStream();
            // Write the request to the HTTP server.
            outputStream.write(buffer.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            // Check response code
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (debug.messageEnabled()) {
                    debug.message(method + "Response code OK");
                }
            } else {
                debug.error(method + "Response code NOT OK: "
                        + conn.getResponseCode());
            }
        } catch (SessionException ex) {
            debug.error(method + " post to external app failed.", ex);
        } catch (IOException ex) {
            debug.error(method + " post to external app failed.", ex);
        }
    }

    // Get cookies string from HTTP request object
    public static String getCookiesString(HttpServletRequest request) {
        String method = "SAML2Utils.getCookiesString: ";
        Cookie cookies[] = request.getCookies();
        StringBuffer cookieStr = null;
        String strCookies = null;
        // Process Cookies
        if (cookies != null) {
            for (int nCookie = 0; nCookie < cookies.length; nCookie++) {
                if (debug.messageEnabled()) {
                    debug.message(method + "Cookie name = " +
                            cookies[nCookie].getName());
                    debug.message(method + " Cookie value = " +
                            cookies[nCookie].getValue());
                }
                if (cookieStr == null) {
                    cookieStr = new StringBuffer();
                } else {
                    cookieStr.append(SEMI_COLON).append(SAMLConstants.SPACE);
                }

                if (cookies[nCookie].getName().equals(sessionCookieName)) {
                    cookieStr.append(cookies[nCookie].getName()).append(EQUALS)
                            .append(DOUBLE_QUOTE)
                            .append(cookies[nCookie].getValue())
                            .append(DOUBLE_QUOTE);
                } else {
                    cookieStr.append(cookies[nCookie].getName()).append(EQUALS)
                            .append(cookies[nCookie].getValue());
                }
            }
        }
        if (cookieStr != null) {
            strCookies = cookieStr.toString();
        }
        return (strCookies);
    }

    /**
     * Returns value of attribute <code>wantPOSTResponseSigned</code>
     * as a boolean value true to false.
     *
     * @param realm        realm of hosted entity.
     * @param hostEntityId name of hosted entity.
     * @param entityRole   role of hosted entity.
     * @return true if wantPOSTResponseSigned has <code>String</code> true,
     * otherwise false.
     */
    public static boolean wantPOSTResponseSigned(String realm,
                                                 String hostEntityId, String entityRole) {
        if (debug.messageEnabled()) {
            String method = "SAML2Utils:getWantPOSTResponseSigned : ";
            debug.message(method + ": realm - " + realm
                    + "/: hostEntityId - " + hostEntityId + ": entityRole - "
                    + entityRole);
        }
        String wantSigned = getAttributeValueFromSSOConfig(realm, hostEntityId,
                entityRole, SAML2Constants.WANT_POST_RESPONSE_SIGNED);
        return "true".equalsIgnoreCase(wantSigned);
    }

    /**
     * Checks if a profile binding is suppported by a SP.
     *
     * @param realm      Realm the SP is in.
     * @param spEntityID SP entity id.
     * @param profile    name of the profile/service
     * @param binding    binding to be checked on
     * @return <code>true</code> if the binding is supported;
     * <code>false</code> otherwise.
     */
    public static boolean isSPProfileBindingSupported(
            String realm, String spEntityID, String profile, String binding) {
        if ((saml2MetaManager == null) ||
                (realm == null) || (spEntityID == null) ||
                (profile == null) || (binding == null)) {
            return false;
        }
        try {
            SPSSODescriptorElement spDescriptor =
                    saml2MetaManager.getSPSSODescriptor(realm, spEntityID);
            List services = null;
            if (SAML2Constants.ACS_SERVICE.equals(profile)) {
                services = spDescriptor.getAssertionConsumerService();
            } else if (SAML2Constants.SLO_SERVICE.equals(profile)) {
                services = spDescriptor.getSingleLogoutService();
            } else if (SAML2Constants.MNI_SERVICE.equals(profile)) {
                services = spDescriptor.getManageNameIDService();
            }
            if ((services != null) && (!services.isEmpty())) {
                Iterator iter = services.iterator();
                while (iter.hasNext()) {
                    EndpointType endpoint = (EndpointType) iter.next();
                    if (binding.equals(endpoint.getBinding())) {
                        return true;
                    }
                }
            }
        } catch (SAML2MetaException me) {
            debug.error("SAML2Utils.isSPProfileBindingSupported:", me);
        }
        return false;
    }

    /**
     * Checks if a profile binding is suppported by an IDP.
     *
     * @param realm       Realm the IDP is in.
     * @param idpEntityID IDP entity id.
     * @param profile     name of the profile/service
     * @param binding     binding to be checked on
     * @return <code>true</code> if the binding is supported;
     * <code>false</code> otherwise.
     */
    public static boolean isIDPProfileBindingSupported(
            String realm, String idpEntityID, String profile, String binding) {
        if ((saml2MetaManager == null) ||
                (realm == null) || (idpEntityID == null) ||
                (profile == null) || (binding == null)) {
            return false;
        }
        try {
            IDPSSODescriptorElement idpDescriptor =
                    saml2MetaManager.getIDPSSODescriptor(realm, idpEntityID);
            List services = null;
            if (SAML2Constants.SSO_SERVICE.equals(profile)) {
                services = idpDescriptor.getSingleSignOnService();
            } else if (SAML2Constants.NAMEID_MAPPING_SERVICE.equals(profile)) {
                services = idpDescriptor.getNameIDMappingService();
            } else if (
                    SAML2Constants.ASSERTION_ID_REQUEST_SERVICE.equals(profile)) {
                services = saml2MetaManager.
                        getAuthnAuthorityDescriptor(realm, idpEntityID).
                        getAssertionIDRequestService();
            } else if (
                    SAML2Constants.ARTIFACT_RESOLUTION_SERVICE.equals(profile)) {
                services = idpDescriptor.getArtifactResolutionService();
            } else if (
                    SAML2Constants.SLO_SERVICE.equals(profile)) {
                services = idpDescriptor.getSingleLogoutService();
            } else if (
                    SAML2Constants.MNI_SERVICE.equals(profile)) {
                services = idpDescriptor.getManageNameIDService();
            }
            if ((services != null) && (!services.isEmpty())) {
                Iterator iter = services.iterator();
                while (iter.hasNext()) {
                    EndpointType endpoint = (EndpointType) iter.next();
                    if (binding.equals(endpoint.getBinding())) {
                        return true;
                    }
                }
            }
        } catch (SAML2MetaException me) {
            debug.error("SAML2Utils.isIDPProfileBindingSupported:", me);
        }
        return false;
    }

    /**
     * Convenience method to validate a SAML2 relay state (goto) URL, often called from a JSP.
     *
     * @param request    Used to help establish the realm and hostEntityID.
     * @param relayState The URL to validate.
     * @param role       The role of the caller.
     * @return <code>true</code> if the relayState is valid.
     */
    public static boolean isRelayStateURLValid(HttpServletRequest request, String relayState, String role) {
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI());
        if (metaAlias == null) {
            //try to acquire the metaAlias from request parameter
            metaAlias = request.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        }
        return isRelayStateURLValid(metaAlias, relayState, role);
    }

    /**
     * Convenience method to validate a SAML2 relay state (goto) URL, often called from a JSP.
     *
     * @param metaAlias  The metaAlias of the hosted entity.
     * @param relayState The URL to validate.
     * @param role       The role of the caller.
     * @return <code>true</code> if the relayState is valid.
     */
    public static boolean isRelayStateURLValid(String metaAlias, String relayState, String role) {
        boolean result = false;

        if (metaAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
            try {
                String hostEntityID = saml2MetaManager.getEntityByMetaAlias(metaAlias);
                if (hostEntityID != null) {
                    validateRelayStateURL(realm, hostEntityID, relayState, role);
                    result = true;
                }
            } catch (SAML2Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2Utils.isRelayStateURLValid(): relayState " + relayState +
                            " for role " + role + " triggered an exception: " + e.getMessage(), e);
                }
                result = false;
            }
        }

        if (debug.messageEnabled()) {
            debug.message("SAML2Utils.isRelayStateURLValid(): relayState " + relayState +
                    " for role " + role + " was valid? " + result);
        }

        return result;
    }

    /**
     * Validates the Relay State URL against a list of valid Relay State
     * URLs created on the hosted service provider.
     *
     * @param orgName      realm or organization name the provider resides in.
     * @param hostEntityId Entity ID of the hosted provider.
     * @param relayState   Relay State URL.
     * @param role         IDP/SP Role.
     * @throws SAML2Exception if the processing failed.
     */
    public static void validateRelayStateURL(
            String orgName,
            String hostEntityId,
            String relayState,
            String role) throws SAML2Exception {

        // Check for the validity of the RelayState URL.
        if (relayState != null && !relayState.isEmpty()) {
            if (!RELAY_STATE_VALIDATOR.isRedirectUrlValid(relayState,
                    SAMLEntityInfo.from(orgName, hostEntityId, role))) {
                throw new SAML2Exception(SAML2Utils.bundle.getString("invalidRelayStateUrl"));
            }
        }
    }

    /**
     * Sends the request to the original Federation server and receives the result
     * data.
     *
     * @param request      HttpServletRequest to be sent
     * @param response     HttpServletResponse to be received
     * @param sloServerUrl URL of the original federation server to be
     *                     connected
     * @return HashMap of the result data from the original server's response
     */
    public static HashMap sendRequestToOrigServer(HttpServletRequest request,
                                                  HttpServletResponse response, String sloServerUrl) {
        HashMap origRequestData = new HashMap();
        String classMethod = "SAML2Utils.sendRequestToOrigServer: ";

        // Print request Headers
        if (debug.messageEnabled()) {
            for (Enumeration<String> requestHeaders = request.getHeaderNames(); requestHeaders.hasMoreElements(); ) {
                String name = requestHeaders.nextElement();
                Enumeration<String> value = request.getHeaders(name);
                debug.message(classMethod + "Header name = " + name + " Value = " + value);
            }
        }

        // Open URL connection
        HttpURLConnection conn = null;
        String strCookies = null;

        try {
            URL sloRoutingURL = new URL(sloServerUrl);

            if (debug.messageEnabled()) {
                debug.message(classMethod + "Connecting to : " + sloRoutingURL);
            }

            conn = HttpURLConnectionManager.getConnection(sloRoutingURL);
            boolean isGET = request.getMethod().equalsIgnoreCase(GET_METHOD);
            if (isGET) {
                conn.setRequestMethod(GET_METHOD);
            } else {
                conn.setDoOutput(true);
                conn.setRequestMethod(POST_METHOD);
            }
            HttpURLConnection.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);

            // replay cookies
            strCookies = getCookiesString(request);

            if (strCookies != null) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Sending cookies : " + strCookies);
                }
                conn.setRequestProperty("Cookie", strCookies);
            }

            conn.setRequestProperty("Host", request.getHeader("host"));
            conn.setRequestProperty(SAMLConstants.ACCEPT_LANG_HEADER, request.getHeader(SAMLConstants.ACCEPT_LANG_HEADER));

            // do the remote connection
            if (isGET) {
                conn.connect();
            } else {
                String data = "";
                Map<String, String[]> params = request.getParameterMap();
                for (Map.Entry<String, String[]> param : params.entrySet()) {
                    data = data + param.getKey() + "=" +
                            URLEncDec.encode(param.getValue()[0]) + "&";
                }
                data = data.substring(0, data.length() - 1);
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "DATA to be SENT: " + data);
                }
                OutputStreamWriter writer = null;
                try {
                    writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(data);
                } catch (IOException ioe) {
                    debug.error(classMethod + "Could not write to the destination", ioe);
                } finally {
                    writer.close();
                }
            }
            // Receiving input from Original Federation server...
            if (debug.messageEnabled()) {
                debug.message(classMethod + "RECEIVING DATA ... ");
                debug.message(classMethod + "Response Code: " + conn.getResponseCode());
                debug.message(classMethod + "Response Message: " + conn.getResponseMessage());
                debug.message(classMethod + "Follow redirect : " + HttpURLConnection.getFollowRedirects());
            }

            // Input from Original servlet...
            StringBuilder in_buf = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            int len;
            char[] buf = new char[1024];

            while ((len = in.read(buf, 0, buf.length)) != -1) {
                in_buf.append(buf, 0, len);
            }

            String in_string = in_buf.toString();

            if (debug.messageEnabled()) {
                debug.message(classMethod + "Received response data : " + in_string);
            }

            origRequestData.put(SAML2Constants.OUTPUT_DATA, in_string);

            String redirect_url = conn.getHeaderField(LOCATION);

            if (redirect_url != null) {
                origRequestData.put(SAML2Constants.AM_REDIRECT_URL, redirect_url);
            }
            origRequestData.put(SAML2Constants.RESPONSE_CODE, Integer.toString(conn.getResponseCode()));

            // retrieves cookies from the response
            Map headers = conn.getHeaderFields();
            processCookies(headers, request, response);
        } catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "send exception : ", ex);
            }
        }

        return origRequestData;
    }

    // parses the cookies from the response header and adds them in
    // the HTTP response.
    // TODO: This is a copy from AuthClientUtils, need to refactor into OpenAM
    // common
    private static void processCookies(Map headers,
                                       HttpServletRequest request, HttpServletResponse response) {
        if (debug.messageEnabled()) {
            debug.message("processCookies : headers : " + headers);
        }

        if (headers == null || headers.isEmpty()) {
            return;
        }

        for (Iterator hrs = headers.entrySet().iterator(); hrs.hasNext(); ) {
            Map.Entry me = (Map.Entry) hrs.next();
            String key = (String) me.getKey();
            if (key != null && (key.equalsIgnoreCase("Set-cookie") ||
                    (key.equalsIgnoreCase("Cookie")))) {
                List list = (List) me.getValue();

                if (list == null || list.isEmpty()) {
                    continue;
                }

                Cookie cookie = null;
                String domain = null;
                String path = null;
                String cookieName = null;
                String cookieValue = null;

                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    String cookieStr = (String) it.next();

                    if (debug.messageEnabled()) {
                        debug.message("processCookies : cookie : "
                                + cookieStr);
                    }

                    StringTokenizer stz = new StringTokenizer(cookieStr, ";");

                    while (stz.hasMoreTokens()) {
                        String nameValue = stz.nextToken();
                        int index = nameValue.indexOf("=");

                        if (index == -1) {
                            continue;
                        }

                        String nameofParam = nameValue.substring(0, index).trim();
                        String nameOfValue = nameValue.substring(index + 1);

                        // we can ignore this cookie
                        if (nameofParam.equalsIgnoreCase("JSESSIONID")) {
                            continue;
                        }

                        /* decode the cookie if it is already URLEncoded,
                         * we have to pass non URLEncoded cookie to 
                         * createCookie method
                         */
                        if (isURLEncoded(nameOfValue)) {
                            try {
                                nameOfValue = URLDecoder.decode(nameOfValue, "UTF-8");
                            } catch (java.io.UnsupportedEncodingException e) {
                                // this would not happen for UTF-8
                            }
                        }

                        if (nameofParam.equalsIgnoreCase("Domain")) {
                            domain = nameOfValue;
                        } else if (nameofParam.equalsIgnoreCase("Expires") ||
                                nameofParam.equalsIgnoreCase("Max-Age") ||
                                nameofParam.equalsIgnoreCase("Version")) {
                            // we don't care about the cookie expiry
                            continue;
                        } else if (nameofParam.equalsIgnoreCase("Path")) {
                            path = nameOfValue;
                        } else {
                            cookieName = nameofParam;
                            cookieValue = nameOfValue;
                        }
                    }

                    cookie = createCookie(cookieName, cookieValue, domain, path);

                    if ("LOGOUT".equals(cookieValue)) {
                        cookie.setMaxAge(0);
                    }

                    if (cookieName.equals(sessionCookieName)) {
                        cookie.setMaxAge(0);
                    }

                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * Checks if the provided <code>String</code> is URLEncoded. Our logic is
     * simple. If the string has % or + character we treat as URL encoded
     * <p/>
     * TODO : Copied from AuthClientUtils, refactor
     *
     * @param s the <code>String</code> we want to check
     * @return <code>true</code> if the provided string is URLEncoded,
     * <code>false</code> otherwise.
     */
    private static boolean isURLEncoded(String s) {
        boolean urlEncoded = false;
        if (s != null) {
            if ((s.indexOf("%") != -1) || (s.indexOf("+") != -1)) {
                urlEncoded = true;
            }
        }
        return urlEncoded;
    }

    /**
     * Creates a Cookie with the <code>cookieName</code>,
     * <code>cookieValue</code> for the cookie domains specified.
     * <p/>
     * TODO: Copied from AuthClientUtils Refactor
     *
     * @param cookieName   is the name of the cookie
     * @param cookieValue  is the value fo the cookie
     * @param cookieDomain Domain for which the cookie is to be set.
     * @param path         The path into which the cookie shall be set
     * @return the cookie object.
     */
    public static Cookie createCookie(String cookieName,
                                      String cookieValue,
                                      String cookieDomain,
                                      String path) {
        if (debug.messageEnabled()) {
            debug.message("cookieName   : " + cookieName);
            debug.message("cookieValue  : " + cookieValue);
            debug.message("cookieDomain : " + cookieDomain);
            debug.message("path : " + path);
        }

        Cookie cookie = null;

        try {
            cookie = CookieUtils.newCookie(cookieName, cookieValue,
                    path, cookieDomain);
        } catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("Error creating cookie. : " + ex.getMessage());
            }
        }

        if (debug.messageEnabled()) {
            debug.message("createCookie Cookie is set : " + cookie);
        }

        return cookie;
    }

    /**
     * Return true if the User for this session has a profile set to Ignore
     *
     * @param session session object of the user
     * @return true if the User for this session has a profile set to Ignore
     */
    public static boolean isIgnoreProfileSet(Object session) throws SessionException {

        boolean result = false;
        String classMethod = "SAML2Utils.isIgnoreProfileSet: ";

        if (session != null) {
            //ISAuthConstants.USER_PROFILE
            String[] values = SessionManager.getProvider().getProperty(session, "UserProfile");
            String profile = "";
            if (values != null && values.length > 0) {
                profile = values[0];
                //ISAuthConstants.IGNORE
                result = "Ignore".equals(profile);
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod
                        + "User profile from session = "
                        + profile + " ignoreProfile = " + result);
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "User session was null");
            }
        }

        return result;
    }

    /**
     * Returns the first value of the session property.
     *
     * @param session      The session object.
     * @param propertyName The property's name that needs to be returned.
     * @return The property value derived from the session object.
     * @throws SessionException If there was a problem while retrieving the session property.
     */
    public static String getSingleValuedSessionProperty(Object session, String propertyName) throws SessionException {
        return SessionManager.getProvider().getProperty(session, propertyName)[0];
    }
}
