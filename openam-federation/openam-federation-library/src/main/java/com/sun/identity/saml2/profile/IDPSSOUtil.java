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
 * $Id: IDPSSOUtil.java,v 1.56 2009/11/24 21:53:28 madan_ranganath Exp $
 *
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */

package com.sun.identity.saml2.profile;

import com.sun.identity.saml2.common.*;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.COTException;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.ecp.ECPFactory;
import com.sun.identity.saml2.ecp.ECPResponse;
import com.sun.identity.saml2.idpdiscovery.IDPDiscoveryConstants;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.ArtifactResolutionServiceElement;
import com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.key.EncInfo;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.plugins.IDPAttributeMapper;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.plugins.IDPECPSessionMapper;
import com.sun.identity.saml2.protocol.Artifact;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.PrivateKey;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

/**
 * The utility class is used by the identity provider to process
 * the authentication request from a service provider and send back
 * a proper response.
 * The identity provider can also send unsolicited response to a service
 * provider to do single sign on and/or federation.
 */
public class IDPSSOUtil {
    // key name for name id format on SSOToken
    public static final String NAMEID_FORMAT = "SAML2NameIDFormat";
    public static final String NULL = "null";
    public static SAML2MetaManager metaManager = null;
    public static CircleOfTrustManager cotManager = null;
    static IDPSessionListener sessionListener = new IDPSessionListener();
    static SessionProvider sessionProvider = null;

    private static FedMonAgent agent;
    private static FedMonSAML2Svc saml2Svc;

    static {
        try {
            metaManager = new SAML2MetaManager();
            cotManager = new CircleOfTrustManager();
        } catch (COTException ce) {
            SAML2Utils.debug.error("Error retrieving circle of trust");
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error("Error retrieving metadata", sme);
        }
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            SAML2Utils.debug.error(
                    "IDPSSOUtil static block: Error getting SessionProvider.",
                    se);
        }

        agent = MonitorManager.getAgent();
        saml2Svc = MonitorManager.getSAML2Svc();
    }

    /**
     * Does SSO with existing federation or new federation
     *
     * @param request      the <code>HttpServletRequest</code> object
     * @param response     the <code>HttpServletResponse</code> object
     * @param out          the print writer for writing out presentation
     * @param authnReq     the <code>AuthnRequest</code> object
     * @param spEntityID   the entity id of the service provider
     * @param idpMetaAlias the meta alias of the identity provider
     * @param nameIDFormat the <code>NameIDFormat</code>
     * @param relayState   the relay state
     * @throws SAML2Exception if the operation is not successful
     */
    public static void doSSOFederate(HttpServletRequest request,
                                     HttpServletResponse response,
                                     PrintWriter out,
                                     AuthnRequest authnReq,
                                     String spEntityID,
                                     String idpMetaAlias,
                                     String nameIDFormat,
                                     String relayState)
            throws SAML2Exception {
        doSSOFederate(request, response, out, authnReq,
                spEntityID, idpMetaAlias, nameIDFormat,
                relayState, null);
    }

    /**
     * Does SSO with existing federation or new federation
     *
     * @param request      the <code>HttpServletRequest</code> object
     * @param response     the <code>HttpServletResponse</code> object
     * @param out          the print writer for writing out presentation
     * @param authnReq     the <code>AuthnRequest</code> object
     * @param spEntityID   the entity id of the service provider
     * @param idpMetaAlias the meta alias of the identity provider
     * @param nameIDFormat the <code>NameIDFormat</code>
     * @param relayState   the relay state
     * @param newSession   Session used in IDP Proxy Case
     * @throws SAML2Exception if the operation is not successful
     */
    public static void doSSOFederate(HttpServletRequest request,
                                     HttpServletResponse response,
                                     PrintWriter out,
                                     AuthnRequest authnReq,
                                     String spEntityID,
                                     String idpMetaAlias,
                                     String nameIDFormat,
                                     String relayState,
                                     Object newSession)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.doSSOFederate: ";

        Object session = null;
        if (newSession != null) {
            session = newSession;
        } else {
            try {
                session = sessionProvider.getSession(request);
            } catch (SessionException se) {
                if (SAML2Utils.debug.warningEnabled()) {
                    SAML2Utils.debug.warning(
                            classMethod + "No session yet.");
                }
            }
        }

        // log the authnRequest       
        String authnRequestStr = null;
        if (authnReq != null) {
            authnRequestStr = authnReq.toXMLString();
        }
        String[] logdata = {spEntityID, idpMetaAlias, authnRequestStr};
        LogUtil.access(Level.INFO,
                LogUtil.RECEIVED_AUTHN_REQUEST, logdata, session);

        // retrieve IDP entity id from meta alias
        String idpEntityID = null;
        String realm = null;
        try {
            if (metaManager == null) {
                SAML2Utils.debug.error(classMethod +
                        "Unable to get meta manager.");
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("errorMetaManager"));
            }
            idpEntityID = metaManager.getEntityByMetaAlias(idpMetaAlias);
            if ((idpEntityID == null)
                    || (idpEntityID.trim().length() == 0)) {
                SAML2Utils.debug.error(classMethod +
                        "Unable to get IDP Entity ID from meta.");
                String[] data = {idpEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.INVALID_IDP, data, session);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
            realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get IDP Entity ID from meta.");
            String[] data = {idpMetaAlias};
            LogUtil.error(Level.INFO,
                    LogUtil.IDP_METADATA_ERROR, data, session);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        // check if the remote provider is valid
        if (authnReq == null) {
            Issuer issuer = AssertionFactory.getInstance().createIssuer();
            issuer.setValue(spEntityID);
            if (!SAML2Utils.isSourceSiteValid(issuer, realm, idpEntityID)) {
                if (SAML2Utils.debug.warningEnabled()) {
                    SAML2Utils.debug.warning(classMethod +
                            "The remote provider is not valid.");
                }
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidReceiver"));
            }
        }

        // Validate the RelayState URL.
        SAML2Utils.validateRelayStateURL(realm,
                idpEntityID,
                relayState,
                SAML2Constants.IDP_ROLE);

        if ((authnReq == null) && (session == null)) {
            // idp initiated and not logged in yet, need to authenticate
            try {
                redirectAuthentication(request, response, authnReq,
                        null, realm, idpEntityID, spEntityID);
            } catch (IOException ioe) {
                SAML2Utils.debug.error(classMethod +
                        "Unable to redirect to authentication.", ioe);
                SAMLUtils.sendError(request, response,
                        response.SC_INTERNAL_SERVER_ERROR,
                        "UnableToRedirectToAuth",
                        SAML2Utils.bundle.getString("UnableToRedirectToAuth"));
            }
            return;
        }

        // Invoke the IDP Adapter
        try {
            SAML2Utils.debug.message(classMethod + " Invoking the "
                    + "IDP Adapter");
            SAML2IdentityProviderAdapter idpAdapter =
                    IDPSSOUtil.getIDPAdapterClass(realm, idpEntityID);
            if (idpAdapter != null) {
                // If the preSendResponse returns true we end here
                if (idpAdapter.preSendResponse(authnReq, idpEntityID,
                        realm, request, response, session, null, relayState)) {
                    return;
                }  // else we continue with the logic. Beware of loops
            }
        } catch (SAML2Exception se2) {
            SAML2Utils.debug.error(classMethod + " There was a problem when invoking"
                    + "the preSendResponse of the IDP Adapter: ", se2);
        }
        // End of invocation

        sendResponseToACS(request, response, out, session, authnReq, spEntityID,
                idpEntityID, idpMetaAlias, realm, nameIDFormat, relayState, null);
    }

    /**
     * Sends <code>Response</code> containing an <code>Assertion</code>
     * back to the requesting service provider
     *
     * @param request              the <code>HttpServletRequest</code> object
     * @param response             the <code>HttpServletResponse</code> object
     * @param out                  the print writer for writing out presentation
     * @param session              user session
     * @param authnReq             the <code>AuthnRequest</code> object
     * @param spEntityID           the entity id of the service provider
     * @param idpEntityID          the entity id of the identity provider
     * @param idpMetaAlias         the meta alias of the identity provider
     * @param realm                the realm
     * @param nameIDFormat         the <code>NameIDFormat</code>
     * @param relayState           the relay state
     * @param matchingAuthnContext the <code>AuthnContext</code> used to find
     *                             authentication type and scheme.
     */
    public static void sendResponseToACS(HttpServletRequest request, HttpServletResponse response, PrintWriter out,
                                         Object session, AuthnRequest authnReq,
                                         String spEntityID, String idpEntityID, String idpMetaAlias,
                                         String realm, String nameIDFormat, String relayState,
                                         AuthnContext matchingAuthnContext)
            throws SAML2Exception {

        StringBuffer returnedBinding = new StringBuffer();
        String acsURL = IDPSSOUtil.getACSurl(
                spEntityID, realm, authnReq, request, returnedBinding);
        String acsBinding = returnedBinding.toString();

        if ((acsURL == null) || (acsURL.trim().length() == 0)) {
            SAML2Utils.debug.error("IDPSSOUtil.sendResponseToACS:" +
                    " no ACS URL found.");
            String[] data = {idpMetaAlias};
            LogUtil.error(Level.INFO,
                    LogUtil.NO_ACS_URL, data, session);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnableTofindACSURL"));
        }
        if ((acsBinding == null) || (acsBinding.trim().length() == 0)) {
            SAML2Utils.debug.error("IDPSSOUtil.sendResponseToACS:" +
                    " no return binding found.");
            String[] data = {idpMetaAlias};
            LogUtil.error(Level.INFO,
                    LogUtil.NO_RETURN_BINDING, data, session);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnableTofindBinding"));
        }


        String affiliationID = request.getParameter(
                SAML2Constants.AFFILIATION_ID);

        //check first if there is already an existing sessionindex associated with this SSOToken, if there is, then
        //we need to redirect the request internally to the holder of the idpsession.
        //The remoteServiceURL will be null if there is no sessionindex for this SSOToken, or there is, but it's
        //local. If the remoteServiceURL is not null, we can start to send the request to the original server.
        String remoteServiceURL = SAML2Utils.getRemoteServiceURL(getSessionIndex(session));
        if (remoteServiceURL != null) {
            remoteServiceURL += SAML2Utils.removeDeployUri(request.getRequestURI()) + "?" + request.getQueryString();
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SessionIndex for this SSOToken is not local, forwarding the request to: "
                        + remoteServiceURL);
            }
            String redirectUrl = null;
            String outputData = null;
            String responseCode = null;
            HashMap<String, String> remoteRequestData =
                    SAML2Utils.sendRequestToOrigServer(request, response, remoteServiceURL);
            if (remoteRequestData != null && !remoteRequestData.isEmpty()) {
                redirectUrl = remoteRequestData.get(SAML2Constants.AM_REDIRECT_URL);
                outputData = remoteRequestData.get(SAML2Constants.OUTPUT_DATA);
                responseCode = remoteRequestData.get(SAML2Constants.RESPONSE_CODE);
            }

            try {
                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    response.sendRedirect(redirectUrl);
                } else {
                    if (responseCode != null) {
                        response.setStatus(Integer.valueOf(responseCode));
                    }
                    // no redirect, perhaps an error page, return the content
                    if (outputData != null && !outputData.isEmpty()) {
                        SAML2Utils.debug.message("Printing the forwarded response");
                        response.setContentType("text/html; charset=UTF-8");
                        out.println(outputData);
                        return;
                    }
                }
            } catch (IOException ioe) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("IDPSSOUtil.sendResponseToACS() error in Request Routing", ioe);
                }
            }
            return;
        }
        //end of request proxy

        // generate a response for the authn request
        Response res = getResponse(session, authnReq, spEntityID, idpEntityID,
                idpMetaAlias, realm, nameIDFormat, acsURL, affiliationID,
                matchingAuthnContext);

        if (res == null) {
            SAML2Utils.debug.error("IDPSSOUtil.sendResponseToACS:" +
                    " response is null");
            String errorMsg =
                    SAML2Utils.bundle.getString("UnableToCreateAssertion");
            if (authnReq == null) {
                //idp initiated case, will not send error response to sp
                throw new SAML2Exception(errorMsg);
            }
            res = SAML2Utils.getErrorResponse(authnReq,
                    SAML2Constants.RESPONDER, null, errorMsg, idpEntityID);
        } else {
            try {
                String[] values = {idpMetaAlias};
                sessionProvider.setProperty(
                        session, SAML2Constants.IDP_META_ALIAS, values);
            } catch (SessionException e) {
                SAML2Utils.debug.error("IDPSSOUtil.sendResponseToACS:" +
                        " error setting idpMetaAlias into the session: ", e);
            }
        }

        if (res != null) {
            // call multi-federation protocol to set the protocol
            MultiProtocolUtils.addFederationProtocol(session,
                    SingleLogoutManager.SAML2);
            // check if the COT cookie needs to be set
            if (setCOTCookie(request, response, acsBinding, spEntityID,
                    idpEntityID, idpMetaAlias, realm, relayState, acsURL, res,
                    session)) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("IDPSSOUtil.sendResponseToACS:" +
                            " Redirected to set COT cookie.");
                }
                return;
            }
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("IDPSSOUtil.sendResponseToACS:" +
                        " Doesn't set COT cookie.");
                SAML2Utils.debug.message("IDPSSOUtil.sendResponseToACS:" +
                        " Response is:  " + res.toXMLString());
            }
            try {
                SAML2Utils.debug.message("IDPSSOUtil.sendResponseToACS: Invoking the IDP Adapter");
                SAML2IdentityProviderAdapter idpAdapter = IDPSSOUtil.getIDPAdapterClass(realm, idpEntityID);
                if (idpAdapter != null) {
                    idpAdapter.preSignResponse(authnReq, res, idpEntityID, realm, request, session, relayState);
                }
            } catch (SAML2Exception se) {
                SAML2Utils.debug.error("IDPSSOUtil.sendResponseToACS: There was a problem when invoking the "
                        + "preSendResponse of the IDP Adapter: ", se);
            }
            sendResponse(request, response, acsBinding, spEntityID, idpEntityID,
                    idpMetaAlias, realm, relayState, acsURL, res, session);
        } else {
            SAML2Utils.debug.error("IDPSSOUtil.sendResponseToACS:" +
                    " error response is null");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnableToCreateErrorResponse"));
        }

    }

    private static boolean setCOTCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String acsBinding,
            String spEntityID,
            String idpEntityID,
            String idpMetaAlias,
            String realm,
            String relayState,
            String acsURL,
            Response res,
            Object session) {

        String classMethod = "IDPSSOUtil.setCOTCookie: ";

        String writerURL = getWriterURL(realm, idpEntityID, spEntityID);
        if (writerURL == null) {
            // could not find the writer URL, do not set the COT cookie
            return false;
        }
        // save the needed info into cache so they can be used later
        // when it is redirected back
        ArrayList cacheList = new ArrayList(9);
        cacheList.add(0, acsBinding);
        cacheList.add(1, spEntityID);
        cacheList.add(2, idpEntityID);
        cacheList.add(3, idpMetaAlias);
        cacheList.add(4, realm);
        cacheList.add(5, relayState);
        cacheList.add(6, acsURL);
        cacheList.add(7, res);
        cacheList.add(8, session);
        String cachedResID = SAML2Utils.generateIDWithServerID();
        IDPCache.responseCache.put(cachedResID, cacheList);

        // construct redirect URL
        StringBuffer retURLSB = new StringBuffer(100);
        retURLSB.append(request.getScheme()).append("://")
                .append(request.getServerName()).append(":")
                .append(request.getServerPort())
                .append(request.getRequestURI())
                .append("?")
                .append(SAML2Constants.RES_INFO_ID)
                .append("=")
                .append(cachedResID);
        String retURL = URLEncDec.encode(retURLSB.toString());
        StringBuffer redirectURLSB = new StringBuffer(200);
        redirectURLSB.append(writerURL);
        if (writerURL.indexOf("?") > 0) {
            redirectURLSB.append("&");
        } else {
            redirectURLSB.append("?");
        }
        redirectURLSB.append(IDPDiscoveryConstants.SAML2_COOKIE_NAME)
                .append("=")
                .append(idpEntityID)
                .append("&")
                .append(SAML2Constants.RELAY_STATE)
                .append("=")
                .append(retURL);
        String redirectURL = redirectURLSB.toString();
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod +
                    "Writer redirect URL: " + redirectURL);
        }
        try {
            response.sendRedirect(redirectURL);
            return true;
        } catch (IOException ioe) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to send redirect: ", ioe);
        }
        return false;
    }

    /**
     * A convenience method to construct NoPassive SAML error responses for SAML passive authentication requests.
     *
     * @param request The servlet request.
     * @param response The servlet response.
     * @param idpMetaAlias The IdP's metaAlias.
     * @param idpEntityID The IdP's entity ID.
     * @param realm The realm where the IdP belongs to.
     * @param authnReq The SAML AuthnRequest sent by the SP.
     * @param relayState The RelayState value.
     * @param spEntityID The SP's entity ID.
     * @throws SAML2Exception If there was an error while creating or sending the response back to the SP.
     */
    public static void sendNoPassiveResponse(HttpServletRequest request, HttpServletResponse response,
            String idpMetaAlias, String idpEntityID, String realm, AuthnRequest authnReq, String relayState,
            String spEntityID) throws SAML2Exception {
        Response res = SAML2Utils.getErrorResponse(authnReq, SAML2Constants.RESPONDER, SAML2Constants.NOPASSIVE, null,
                idpEntityID);
        StringBuffer returnedBinding = new StringBuffer();
        String acsURL = IDPSSOUtil.getACSurl(spEntityID, realm, authnReq, request, returnedBinding);
        String acsBinding = returnedBinding.toString();
        sendResponse(request, response, acsBinding, spEntityID, idpEntityID, idpMetaAlias, realm, relayState,
                acsURL, res, null);
    }

    /**
     * Sends a response to service provider
     *
     * @param response    the <code>HttpServletResponse</code> object
     * @param cachedResID the key used to retrieve response information
     *                    from the response information cache
     * @throws SAML2Exception if the operation is not successful
     */
    public static void sendResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String cachedResID)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.sendResponse: ";
        ArrayList cacheList =
                (ArrayList) IDPCache.responseCache.remove(cachedResID);
        if ((cacheList != null) && (cacheList.size() == 9)) {
            String acsBinding = (String) cacheList.get(0);
            String spEntityID = (String) cacheList.get(1);
            String idpEntityID = (String) cacheList.get(2);
            String idpMetaAlias = (String) cacheList.get(3);
            String realm = (String) cacheList.get(4);
            String relayState = (String) cacheList.get(5);
            String acsURL = (String) cacheList.get(6);
            Response res = (Response) cacheList.get(7);
            Object session = cacheList.get(8);
            sendResponse(request, response, acsBinding, spEntityID, idpEntityID,
                    idpMetaAlias, realm, relayState, acsURL, res, session);
        } else {
            SAML2Utils.debug.error(classMethod +
                    "unable to get response information from cache.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString(
                            "UnableToGetResponseInfoFromCache"));
        }
    }

    /**
     * Sends a response to service provider
     *
     * @param response     the <code>HttpServletResponse</code> object
     * @param acsBinding   the assertion consumer service binding
     * @param spEntityID   the entity id of the service provider
     * @param idpEntityID  the entity id of the identity provider
     * @param idpMetaAlias the meta alias of the identity provider
     * @param realm        the realm name
     * @param relayState   the relay state
     * @param acsURL       the assertion consumer service <code>url</code>
     * @param res          the <code>SAML Response</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    public static void sendResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String acsBinding,
            String spEntityID,
            String idpEntityID,
            String idpMetaAlias,
            String realm,
            String relayState,
            String acsURL,
            Response res,
            Object session)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.sendResponse: ";

        String nameIDString = SAML2Utils.getNameIDStringFromResponse(res);
        Map props = new HashMap();
        props.put(LogUtil.NAME_ID, nameIDString);

        // send the response back through HTTP POST or Artifact 
        if (acsBinding.equals(SAML2Constants.HTTP_POST)) {
            // check if response needs to be signed.
            // if response is signed then assertion
            // will not be signed for POST Profile
            boolean signAssertion = true;
            boolean signResponse =
                    SAML2Utils.wantPOSTResponseSigned(realm,
                            spEntityID, SAML2Constants.SP_ROLE);
            // signing assertion is a must for POST profile if
            // response signing is not enabled.
            // encryption is optional based on SP config settings.
            signAndEncryptResponseComponents(
                    realm, spEntityID, idpEntityID, res, signAssertion);

            if (signResponse) {
                signResponse(realm, idpEntityID, res);
            }

            String resMsg = res.toXMLString(true, true);
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                        "SAML Response content :\n" + resMsg);
            }
            String encodedResMsg = SAML2Utils.encodeForPOST(resMsg);

            String[] logdata1 = {spEntityID, idpMetaAlias, resMsg};
            LogUtil.access(Level.INFO,
                    LogUtil.POST_RESPONSE, logdata1, session, props);
            try {
                SAML2Utils.postToTarget(request, response, "SAMLResponse",
                        encodedResMsg, "RelayState", relayState, acsURL);
            } catch (SAML2Exception saml2E) {
                String[] data = {acsURL};
                LogUtil.error(Level.INFO, LogUtil.POST_TO_TARGET_FAILED, data, session, props);
                throw saml2E;
            }
        } else if (acsBinding.equals(SAML2Constants.HTTP_ARTIFACT)) {
            IDPSSOUtil.sendResponseArtifact(request, response, idpEntityID,
                    spEntityID, realm, acsURL, relayState, res, session, props);
        } else if (acsBinding.equals(SAML2Constants.PAOS)) {
            // signing assertion is a must for ECP profile.
            // encryption is optional based on SP config settings.
            signAndEncryptResponseComponents(
                    realm, spEntityID, idpEntityID, res, true);
            IDPSSOUtil.sendResponseECP(request, response, idpEntityID,
                    realm, acsURL, res);
        } else {
            SAML2Utils.debug.error(classMethod +
                    "unsupported return binding.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnSupportedReturnBinding"));
        }
    }

    /**
     * Returns a <code>SAML Response</code> object
     *
     * @param session              the user's session object
     * @param authnReq             the <code>AuthnRequest</code> object
     * @param recipientEntityID    the entity id of the response recipient
     * @param idpEntityID          the entity id of the identity provider
     * @param realm                the realm name
     * @param nameIDFormat         the <code>NameIDFormat</code>
     * @param acsURL               the <code>ACS</code> service <code>url</code>
     * @param affiliationID        affiliationID for IDP initiated SSO
     * @param matchingAuthnContext the <code>AuthnContext</code> used to find
     *                             authentication type and scheme.
     * @return the <code>SAML Response</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    public static Response getResponse(
            Object session,
            AuthnRequest authnReq,
            String recipientEntityID,
            String idpEntityID,
            String idpMetaAlias,
            String realm,
            String nameIDFormat,
            String acsURL,
            String affiliationID,
            AuthnContext matchingAuthnContext)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getResponse: ";

        Response res = ProtocolFactory.getInstance().createResponse();
        Status status = ProtocolFactory.getInstance().createStatus();
        if (status == null) {
            return null;
        }
        StatusCode statusCode = ProtocolFactory.getInstance().
                createStatusCode();
        if (statusCode == null) {
            return null;
        }
        try {
            List assertionList = new ArrayList();

            Assertion assertion = getAssertion(session, authnReq,
                    recipientEntityID, idpEntityID, idpMetaAlias, realm,
                    nameIDFormat, acsURL, affiliationID, matchingAuthnContext);

            if (assertion == null) {
                SAML2Utils.debug.error(
                        classMethod + "Unable to get Assertion.");
                return null;
            }

            assertionList.add(assertion);
            res.setAssertion(assertionList);

            statusCode.setValue(SAML2Constants.SUCCESS);
        } catch (SAML2InvalidNameIDPolicyException se) {
            statusCode.setValue(SAML2Constants.REQUESTER);
            StatusCode subStatusCode = ProtocolFactory.getInstance().
                    createStatusCode();
            subStatusCode.setValue(SAML2Constants.INVALID_NAME_ID_POLICY);
            statusCode.setStatusCode(subStatusCode);
            status.setStatusMessage(se.getMessage());
        }
        status.setStatusCode(statusCode);
        res.setStatus(status);

        if (authnReq != null) {
            // sp initiated case, need to set InResponseTo attribute
            res.setInResponseTo(authnReq.getID());
        }
        res.setVersion(SAML2Constants.VERSION_2_0);
        res.setIssueInstant(new Date());
        res.setID(SAML2Utils.generateID());

        // set the idp entity id as the response issuer
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(idpEntityID);
        res.setIssuer(issuer);
        res.setDestination(XMLUtils.escapeSpecialCharacters(acsURL));
        return res;
    }


    /**
     * Returns a <code>SAML Assertion</code> object
     *
     * @param session              the user's session object
     * @param authnReq             the <code>AuthnRequest</code> object
     * @param recipientEntityID    the entity id of the response recipient
     * @param idpEntityID          the entity id of the identity provider
     * @param realm                the realm name
     * @param nameIDFormat         the <code>NameIDFormat</code>
     * @param acsURL               the <code>ACS</code> service <code>url</code>
     * @param affiliationID        affiliationID for IDP initiated SSO
     * @param matchingAuthnContext the <code>AuthnContext</code> used to find
     *                             authentication type and scheme.
     * @return the <code>SAML Assertion</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    private static Assertion getAssertion(
            Object session,
            AuthnRequest authnReq,
            String recipientEntityID,
            String idpEntityID,
            String idpMetaAlias,
            String realm,
            String nameIDFormat,
            String acsURL,
            String affiliationID,
            AuthnContext matchingAuthnContext)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getAssertion: ";
        Assertion assertion = AssertionFactory.getInstance().createAssertion();
        String assertionID = SAML2Utils.generateID();
        assertion.setID(assertionID);
        assertion.setVersion(SAML2Constants.VERSION_2_0);
        assertion.setIssueInstant(new Date());
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(idpEntityID);

        assertion.setIssuer(issuer);

        List statementList = new ArrayList();

        NewBoolean isNewSessionIndex = new NewBoolean();
        AuthnStatement authnStatement = null;
        IDPSession idpSession = null;
        String sessionIndex = null;
        String sessionID = sessionProvider.getSessionID(session);
        synchronized (sessionID) {
            authnStatement = getAuthnStatement(
                session, isNewSessionIndex, authnReq, idpEntityID, realm,
                matchingAuthnContext);
            if (authnStatement == null) {
                return null;
            }

            sessionIndex = authnStatement.getSessionIndex();
            if (isNewSessionIndex.getValue()) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                        "This is a new IDP session with sessionIndex=" +
                        sessionIndex + ", and sessionID=" +
                        sessionID);
                }
                idpSession = (IDPSession) IDPCache.idpSessionsBySessionID.
                    get(sessionProvider.getSessionID(session));
                if (idpSession == null) {
                    idpSession = new IDPSession(session);
                }
                // Set the metaAlias in the IDP session object
                idpSession.setMetaAlias(idpMetaAlias);

                IDPCache.idpSessionsByIndices.put(sessionIndex, idpSession);

                if ((agent != null) && agent.isRunning() && (saml2Svc != null)) {
                    saml2Svc.setIdpSessionCount(
    		    (long)IDPCache.idpSessionsByIndices.size());
                }
            } else {
                idpSession = (IDPSession)IDPCache.idpSessionsByIndices.
                                                  get(sessionIndex);
            }
		}
		if (isNewSessionIndex.getValue()) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                    "a new IDP session has been saved in cache, " +
                    "with sessionIndex=" + sessionIndex);
            }
            try {
                sessionProvider.addListener(session, sessionListener);
            } catch (SessionException e) {
                SAML2Utils.debug.error(classMethod +
                    "Unable to add session listener.");
            }
		} else {
            if (idpSession == null && SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                // Read from SAML2 Token Repository
                IDPSessionCopy idpSessionCopy = null;
                try {
                    idpSessionCopy = (IDPSessionCopy) SAML2FailoverUtils.retrieveSAML2Token(sessionIndex);
                } catch (SAML2TokenRepositoryException se) {
                    SAML2Utils.debug.error(classMethod +
                            "Unable to obtain IDPSessionCopy from the SAML2 Token Repository for sessionIndex:"
                            + sessionIndex, se);
                }
                // Copy back to IDPSession
                if (idpSessionCopy != null) {
                    idpSession = new IDPSession(idpSessionCopy);
                } else {
                    SAML2Utils.debug.error("IDPSessionCopy is null");
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("IDPSessionIsNULL"));
                }
            } else if ((idpSession == null) &&
                    (!SAML2FailoverUtils.isSAML2FailoverEnabled())) {
                SAML2Utils.debug.error("IDPSession is null; SAML2 failover" +
                        "is disabled");
                throw new SAML2Exception(
                   SAML2Utils.bundle.getString("IDPSessionIsNULL"));
            } else {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                        "This is an existing IDP session with sessionIndex="
                        + sessionIndex + ", and sessionID=" +
                        sessionProvider.getSessionID(idpSession.getSession()));
                }
            }
        }
        statementList.add(authnStatement);

        AttributeStatement attrStatement = getAttributeStatement(
                session, idpEntityID, recipientEntityID, realm);
        if (attrStatement != null) {
            List attrStatementList = new ArrayList();
            attrStatementList.add(attrStatement);
            assertion.setAttributeStatements(attrStatementList);
        }

        // get the assertion effective time (in seconds)
        int effectiveTime = getEffectiveTime(realm, idpEntityID);

        // get the NotBefore skew (in seconds)
        int notBeforeSkewTime = getNotBeforeSkewTime(realm, idpEntityID);

        // get the subject element
        NewBoolean isNewFederation = new NewBoolean();
        Subject subject = getSubject(session, authnReq, acsURL,
                nameIDFormat, isNewFederation, realm, idpEntityID,
                recipientEntityID, effectiveTime, affiliationID);

        // register (spEntityID, nameID) with the sso token
        // for later logout use 
        String spEntityID = null;
        if (authnReq != null) {
            spEntityID = authnReq.getIssuer().getValue();
        } else {
            spEntityID = recipientEntityID;
        }
        NameIDandSPpair pair = new NameIDandSPpair(
                (NameID) subject.getNameID(), spEntityID);

        synchronized (IDPCache.idpSessionsByIndices) {
            List list = (List) idpSession.getNameIDandSPpairs();
            if (isNewFederation.getValue()) { // new federation case
                list.add(pair);
            } else {  // existing federation case
                String id = null;
                if (authnReq != null) {
                    id = authnReq.getIssuer().getValue();
                } else {
                    id = spEntityID;
                }
                int n = list.size();
                NameIDandSPpair p = null;
                for (int i = 0; i < n; i++) {
                    p = (NameIDandSPpair) list.get(i);
                    if (p.getSPEntityID().equals(id)) {
                        break;
                    }
                    p = null;
                }
                if (p == null) {
                    list.add(pair);
                }
            }
        }

        assertion.setAuthnStatements(statementList);
        assertion.setSubject(subject);
        Conditions conditions = getConditions(recipientEntityID,
                notBeforeSkewTime, effectiveTime);
        assertion.setConditions(conditions);

        String discoBootstrapEnabled = getAttributeValueFromIDPSSOConfig(
                realm, idpEntityID, SAML2Constants.DISCO_BOOTSTRAPPING_ENABLED);

        if ((discoBootstrapEnabled != null) &&
                discoBootstrapEnabled.equalsIgnoreCase("true")) {

            List attrStatementList = assertion.getAttributeStatements();
            if (attrStatementList == null) {
                attrStatementList = new ArrayList();
                assertion.setAttributeStatements(attrStatementList);
            }

            DiscoveryBootstrap bootstrap = new DiscoveryBootstrap(session,
                    subject,
                    authnStatement.getAuthnContext().getAuthnContextClassRef(),
                    spEntityID, realm);
            attrStatementList.add(bootstrap.getBootstrapStatement());
            assertion.setAdvice(bootstrap.getCredentials());
        }


        if (assertionCacheEnabled(realm, idpEntityID)) {
            String userName = null;
            try {
                userName = sessionProvider.getPrincipalName(session);
            } catch (SessionException se) {
                SAML2Utils.debug.error(classMethod +
                        "Unable to get principal name from the session.", se);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidSSOToken"));
            }

            String cacheKey = userName.toLowerCase();

            List assertions = (List) IDPCache.assertionCache.get(cacheKey);
            if (assertions == null) {
                synchronized (IDPCache.assertionCache) {
                    assertions = (List) IDPCache.assertionCache.get(cacheKey);
                    if (assertions == null) {
                        assertions = new ArrayList();
                        IDPCache.assertionCache.put(cacheKey, assertions);
                    }
                }
            }
            synchronized (assertions) {
                assertions.add(assertion);
            }

            IDPCache.assertionByIDCache.put(assertionID, assertion);
            if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                try {
                    SAML2FailoverUtils.saveSAML2Token(assertionID, cacheKey,
                            assertion.toXMLString(true, true),
                            conditions.getNotOnOrAfter().getTime() / 1000);
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod +
                                "Saving Assertion to SAML2 Token Repository. ID = " + assertionID);
                    }
                } catch (SAML2TokenRepositoryException se) {
                    SAML2Utils.debug.error(classMethod + "Unable to save Assertion to the SAML2 Token Repository", se);
                }
            }
        }
        //  Save to SAML2 Token Repository
        try {
            if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                long sessionExpireTime = System.currentTimeMillis() / 1000 + (sessionProvider.getTimeLeft(session));
                SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(sessionIndex, new IDPSessionCopy(idpSession), sessionExpireTime);
            }
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod + "SAVE IDPSession!");
            }
        } catch (SessionException se) {
            SAML2Utils.debug.error(classMethod + "Unable to get left-time from the session.", se);
            throw new SAML2Exception(SAML2Utils.bundle.getString("invalidSSOToken"));
        } catch (SAML2TokenRepositoryException se) {
            SAML2Utils.debug.error(classMethod + "Unable to save IDPSession to the SAML2 Token Repository", se);
        }
        return assertion;
    }


    /**
     * Returns a <code>SAML AuthnStatement</code> object
     *
     * @param session              the user's session
     * @param isNewSessionIndex    a returned flag from which the caller
     *                             knows if the session index in the returned
     *                             <code>AuthnStatement</code> is a new session index
     * @param authnReq             the <code>AuthnRequest</code> object
     * @param idpEntityID          the entity id of the identity provider
     * @param realm                the realm name
     * @param matchingAuthnContext the <code>AuthnContext</code> used to find
     *                             authentication type and scheme.
     * @return the <code>SAML AuthnStatement</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    private static AuthnStatement getAuthnStatement(
            Object session,
            NewBoolean isNewSessionIndex,
            AuthnRequest authnReq,
            String idpEntityID,
            String realm,
            AuthnContext matchingAuthnContext)
            throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getAuthnStatement: ";

        AuthnStatement authnStatement =
                AssertionFactory.getInstance().createAuthnStatement();

        Date authInstant = null;
        // will be used when we add SubjectLocality to the statement
        try {
            String[] values = sessionProvider.getProperty(
                    session, SessionProvider.AUTH_INSTANT);
            if (values != null && values.length != 0 &&
                    values[0] != null && values[0].length() != 0) {
                authInstant = DateUtils.stringToDate(values[0]);
            }
        } catch (Exception e) {
            SAML2Utils.debug.error(classMethod +
                    "exception retrieving info from the session: ", e);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorGettingAuthnStatement"));
        }
        if (authInstant == null) {
            authInstant = new Date();
        }
        authnStatement.setAuthnInstant(authInstant);

        AuthnContext authnContext = matchingAuthnContext;
        if (authnContext == null) {
            String authLevel = null;
            try {
                String[] values = sessionProvider.getProperty(
                        session, SessionProvider.AUTH_LEVEL);
                if (values != null && values.length != 0 &&
                        values[0] != null && values[0].length() != 0) {
                    authLevel = values[0];
                }
            } catch (Exception e) {
                SAML2Utils.debug.error(classMethod +
                        "exception retrieving auth level info from the session: ",
                        e);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("errorGettingAuthnStatement"));
            }

            IDPAuthnContextMapper idpAuthnContextMapper =
                    getIDPAuthnContextMapper(realm, idpEntityID);

            authnContext =
                    idpAuthnContextMapper.getAuthnContextFromAuthLevel(
                            authLevel, realm, idpEntityID);
        }

        authnStatement.setAuthnContext(authnContext);

        String sessionIndex = getSessionIndex(session);

        if (sessionIndex == null) { // new sessionIndex
            sessionIndex = SAML2Utils.generateIDWithServerID();
            try {
                String[] values = {sessionIndex};
                sessionProvider.setProperty(
                        session,
                        SAML2Constants.IDP_SESSION_INDEX,
                        values);
            } catch (SessionException e) {
                SAML2Utils.debug.error(classMethod +
                        "error setting session index into the session: ", e);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("errorGettingAuthnStatement"));
            }
            isNewSessionIndex.setValue(true);
        } else {
            isNewSessionIndex.setValue(false);
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod +
                    "SessionIndex (in AuthnStatement) =" + sessionIndex);
        }
        if (sessionIndex != null) {
            Set authContextSet = (HashSet)
                    IDPCache.authnContextCache.get(sessionIndex);
            if (authContextSet == null || authContextSet.isEmpty()) {
                authContextSet = new HashSet();
            }
            authContextSet.add(authnContext);
            // cache the AuthContext to use in the case of session upgrade.
            IDPCache.authnContextCache.put(sessionIndex, authContextSet);
            authnStatement.setSessionIndex(sessionIndex);
        }
        return authnStatement;
    }


    /**
     * Returns a <code>SAML AttributeStatement</code> object
     *
     * @param session           the user's session
     * @param idpEntityID       the entity id of the identity provider
     * @param recipientEntityID the entity id of the response recipient
     * @param realm             the realm name
     * @return the <code>SAML AttributeStatement</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    private static AttributeStatement getAttributeStatement(
            Object session,
            String idpEntityID,
            String recipientEntityID,
            String realm)
            throws SAML2Exception {

        IDPAttributeMapper idpAttrMapper =
                getIDPAttributeMapper(realm, idpEntityID);

        List attributes = idpAttrMapper.getAttributes(
                session, idpEntityID, recipientEntityID, realm);

        if ((attributes == null) || (attributes.isEmpty())) {
            return null;
        }

        AttributeStatement attrStatement =
                AssertionFactory.getInstance().createAttributeStatement();

        attrStatement.setAttribute(attributes);
        return attrStatement;
    }

    /**
     * Returns an <code>IDPAttributeMapper</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>IDPAttributeMapper</code>
     * @throws SAML2Exception if the operation is not successful
     */
    static IDPAttributeMapper getIDPAttributeMapper(
            String realm, String idpEntityID)
            throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getIDPAttributeMapper: ";
        String idpAttributeMapperName = null;
        IDPAttributeMapper idpAttributeMapper = null;
        try {
            idpAttributeMapperName = getAttributeValueFromIDPSSOConfig(
                    realm, idpEntityID, SAML2Constants.IDP_ATTRIBUTE_MAPPER);
            if (idpAttributeMapperName == null) {
                idpAttributeMapperName =
                        SAML2Constants.DEFAULT_IDP_ATTRIBUTE_MAPPER_CLASS;
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod + "use " +
                            SAML2Constants.DEFAULT_IDP_ATTRIBUTE_MAPPER_CLASS);
                }
            }
            idpAttributeMapper = (IDPAttributeMapper)
                    IDPCache.idpAttributeMapperCache.get(
                            idpAttributeMapperName);
            if (idpAttributeMapper == null) {
                idpAttributeMapper = (IDPAttributeMapper)
                        Class.forName(idpAttributeMapperName).newInstance();
                IDPCache.idpAttributeMapperCache.put(
                        idpAttributeMapperName, idpAttributeMapper);
            } else {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                            "got the IDPAttributeMapper from cache");
                }
            }
        } catch (Exception ex) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get IDP Attribute Mapper.", ex);
            throw new SAML2Exception(ex);
        }

        return idpAttributeMapper;
    }


    /**
     * Returns an <code>IDPAuthnContextMapper</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>IDPAuthnContextMapper</code>
     * @throws SAML2Exception if the operation is not successful
     */
    static IDPAuthnContextMapper getIDPAuthnContextMapper(
            String realm, String idpEntityID)
            throws SAML2Exception {
        String classMethod = "IDPSSOUtil.getIDPAuthnContextMapper: ";
        String idpAuthnContextMapperName = null;
        IDPAuthnContextMapper idpAuthnContextMapper = null;
        try {
            idpAuthnContextMapperName = getAttributeValueFromIDPSSOConfig(
                    realm, idpEntityID,
                    SAML2Constants.IDP_AUTHNCONTEXT_MAPPER_CLASS);
            if (idpAuthnContextMapperName == null) {
                idpAuthnContextMapperName =
                        SAML2Constants.DEFAULT_IDP_AUTHNCONTEXT_MAPPER_CLASS;
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod + "use " +
                            SAML2Constants.DEFAULT_IDP_AUTHNCONTEXT_MAPPER_CLASS);
                }
            }
            idpAuthnContextMapper = (IDPAuthnContextMapper)
                    IDPCache.idpAuthnContextMapperCache.get(
                            idpAuthnContextMapperName);
            if (idpAuthnContextMapper == null) {
                idpAuthnContextMapper = (IDPAuthnContextMapper)
                        Class.forName(idpAuthnContextMapperName).newInstance();
                IDPCache.idpAuthnContextMapperCache.put(
                        idpAuthnContextMapperName, idpAuthnContextMapper);
            } else {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                            "got the IDPAuthnContextMapper from cache");
                }
            }
        } catch (Exception ex) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get IDP AuthnContext Mapper.", ex);
            throw new SAML2Exception(ex);
        }

        return idpAuthnContextMapper;
    }

    /**
     * Returns an <code>IDPECPSessionMapper</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>IDPECPSessionMapper</code>
     * @throws SAML2Exception if the operation is not successful
     */
    static IDPECPSessionMapper getIDPECPSessionMapper(String realm,
                                                      String idpEntityID) throws SAML2Exception {

        String idpECPSessionMapperName = null;
        IDPECPSessionMapper idpECPSessionMapper = null;
        try {
            idpECPSessionMapperName = getAttributeValueFromIDPSSOConfig(realm,
                    idpEntityID, SAML2Constants.IDP_ECP_SESSION_MAPPER_CLASS);
            if (idpECPSessionMapperName == null) {
                idpECPSessionMapperName =
                        SAML2Constants.DEFAULT_IDP_ECP_SESSION_MAPPER_CLASS;
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                            "IDPSSOUtil.getIDPECPSessionMapper: use " +
                                    SAML2Constants.DEFAULT_IDP_ECP_SESSION_MAPPER_CLASS);
                }
            }
            idpECPSessionMapper = (IDPECPSessionMapper)
                    IDPCache.idpECPSessionMapperCache.get(
                            idpECPSessionMapperName);
            if (idpECPSessionMapper == null) {
                idpECPSessionMapper = (IDPECPSessionMapper)
                        Class.forName(idpECPSessionMapperName).newInstance();
                IDPCache.idpECPSessionMapperCache.put(
                        idpECPSessionMapperName, idpECPSessionMapper);
            } else {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                            "IDPSSOUtil.getIDPECPSessionMapper: " +
                                    "got the IDPECPSessionMapper from cache");
                }
            }
        } catch (Exception ex) {
            SAML2Utils.debug.error("IDPSSOUtil.getIDPECPSessionMapper: " +
                    "Unable to get IDPECPSessionMapper.", ex);
            throw new SAML2Exception(ex);
        }

        return idpECPSessionMapper;
    }

    /**
     * Returns a <code>SAML Subject</code> object
     *
     * @param session           the user's session
     * @param authnReq          the <code>AuthnRequest</code> object
     * @param acsURL            the <code>ACS</code> service <code>url</code>
     * @param nameIDFormat      the <code>NameIDFormat</code>
     * @param isNewFederation   a returned flag from which the caller
     *                          knows if this is a new federation case
     * @param realm             The realm name
     * @param idpEntityID       the entity id of the identity provider
     * @param recipientEntityID the entity id of the response recipient
     * @param effectiveTime     the effective time of the assertion
     * @param affiliationID     affiliationID for IDP initiated SSO
     * @return the <code>SAML Subject</code> object
     * @throws SAML2Exception if the operation is not successful
     */

    private static Subject getSubject(Object session,
                                      AuthnRequest authnReq,
                                      String acsURL,
                                      String nameIDFormat,
                                      NewBoolean isNewFederation,
                                      String realm,
                                      String idpEntityID,
                                      String recipientEntityID,
                                      int effectiveTime,
                                      String affiliationID)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getSubject: ";

        Subject subject = AssertionFactory.getInstance().createSubject();
        boolean ignoreProfile = false;
        String userName = null;
        try {
            userName = sessionProvider.getPrincipalName(session);
            ignoreProfile = SAML2Utils.isIgnoreProfileSet(session);
        } catch (SessionException se) {
            SAML2Utils.debug.error(classMethod +
                    "There was a problem with the session.", se);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidSSOToken"));
        }
        boolean allowCreate = true; // allow create is the default
        String remoteEntityID = null;
        String spNameQualifier = null;
        boolean isAffiliation = false;
        if (authnReq != null) {
            remoteEntityID = authnReq.getIssuer().getValue();
            NameIDPolicy nameIDPolicy = authnReq.getNameIDPolicy();
            if (nameIDPolicy != null) {
                // this will take care of affiliation
                allowCreate = nameIDPolicy.isAllowCreate();
                spNameQualifier = nameIDPolicy.getSPNameQualifier();
                if (spNameQualifier != null && !spNameQualifier.isEmpty()) {
                    AffiliationDescriptorType affiDesc = metaManager.getAffiliationDescriptor(realm, spNameQualifier);

                    if (affiDesc != null) {
                        if (affiDesc.getAffiliateMember().contains(remoteEntityID)) {
                            isAffiliation = true;
                            remoteEntityID = spNameQualifier;
                        } else {
                            throw new SAML2Exception(SAML2Utils.bundle.getString("spNotAffiliationMember"));
                        }
                    }
                } else {
                    spNameQualifier = recipientEntityID;
                }
            }
        } else {
            // IDP initialted SSO
            if (affiliationID != null) {
                AffiliationDescriptorType affiDesc = metaManager.
                        getAffiliationDescriptor(realm, affiliationID);
                if (affiDesc == null) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                            "affiliationNotFound"));
                }
                if (affiDesc.getAffiliateMember().contains(recipientEntityID)) {
                    isAffiliation = true;
                    remoteEntityID = affiliationID;
                    spNameQualifier = affiliationID;
                } else {
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                            "spNotAffiliationMember"));
                }
            } else {
                remoteEntityID = recipientEntityID;
                spNameQualifier = recipientEntityID;
            }
        }

        SPSSODescriptorElement spsso = metaManager.getSPSSODescriptor(
                realm, recipientEntityID);
        if (spsso == null) {
            String[] data = {recipientEntityID};
            LogUtil.error(Level.INFO, LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "metaDataError"));
        }

        IDPSSODescriptorElement idpsso =
                metaManager.getIDPSSODescriptor(realm, idpEntityID);
        if (idpsso == null) {
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "metaDataError"));
        }

        nameIDFormat = SAML2Utils.verifyNameIDFormat(nameIDFormat, spsso,
                idpsso);

        // Even if the user profile is set to ignore, we must attempt to persist
        // if the NameIDFormat is set to persistent.
        if (ignoreProfile && SAML2Constants.PERSISTENT.equals(nameIDFormat)) {
            ignoreProfile = false;
            SAML2Utils.debug.warning(classMethod
                    + "ignoreProfile was true but NameIDFormat is Persistent => setting ignoreProfile to false");
        }

        NameIDInfo nameIDInfo = null;
        NameID nameID = null;
        boolean isTransient = nameIDFormat.equals(
                SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        if (!isTransient) {
            String userID = null;
            try {
                userID = sessionProvider.getPrincipalName(session);
            } catch (SessionException se) {
                SAML2Utils.debug.error(classMethod +
                        "Unable to get principal name from the session.", se);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidSSOToken"));
            }

            if (!ignoreProfile) {
                nameIDInfo = AccountUtils.getAccountFederation(userID,
                        idpEntityID, remoteEntityID);
            }

            if (nameIDInfo != null) {
                nameID = nameIDInfo.getNameID();

                if (nameIDFormat.equals(nameID.getFormat())) {
                    // existing federation
                    isNewFederation.setValue(false);
                } else {
                    AccountUtils.removeAccountFederation(nameIDInfo, userID);
                    DoManageNameID.removeIDPFedSession(remoteEntityID,
                            nameID.getValue());
                    nameID = null;
                }
            }
        }

        if (nameID == null) {
            if (!allowCreate &&
                    nameIDFormat.equals(SAML2Constants.PERSISTENT)) {
                throw new SAML2InvalidNameIDPolicyException(
                        SAML2Utils.bundle.getString("cannotCreateNameID"));
            }

            IDPAccountMapper idpAccountMapper =
                    SAML2Utils.getIDPAccountMapper(realm, idpEntityID);
            nameID = idpAccountMapper.getNameID(session, idpEntityID, spNameQualifier, realm, nameIDFormat);

            // If the IdP has received a request from a remote SP for which it has
            // been configured not to persist the Federation if unspecified NameID
            // Format has been set
            boolean spDoNotWriteFedInfoInIdP = isSPDoNotWriteFedInfoInIdP(realm,
                    remoteEntityID, metaManager) &&
                    SAML2Constants.UNSPECIFIED.equals(nameIDFormat);
            boolean writeFedInfo = !ignoreProfile && !isTransient && !spDoNotWriteFedInfoInIdP;
            SAML2Utils.debug.message(classMethod + " writeFedInfo = " + writeFedInfo);

            if (writeFedInfo && allowCreate) {
                // write federation info the into persistent datastore
                if (SAML2Utils.isDualRole(idpEntityID, realm)) {
                    nameIDInfo = new NameIDInfo(idpEntityID, remoteEntityID,
                            nameID, SAML2Constants.DUAL_ROLE, false);
                } else {
                    nameIDInfo = new NameIDInfo(idpEntityID, remoteEntityID,
                            nameID, SAML2Constants.IDP_ROLE, isAffiliation);
                }
                AccountUtils.setAccountFederation(nameIDInfo, userName);
            }
            if (writeFedInfo) {
                isNewFederation.setValue(true);
            } else {
                isNewFederation.setValue(false);
            }
        }

        subject.setNameID(nameID);

        if (isTransient) {
            IDPCache.userIDByTransientNameIDValue.put(nameID.getValue(),
                    userName);
        }

        String inResponseTo = null;
        if (authnReq != null) {
            inResponseTo = authnReq.getID();
        }
        SubjectConfirmation sc = getSubjectConfirmation(
                inResponseTo, acsURL, effectiveTime);
        if (sc == null) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get subject confirmation");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("noSubjectConfirmation"));
        }

        List list = new ArrayList();
        list.add(sc);
        subject.setSubjectConfirmation(list);

        return subject;
    }


    /**
     * Returns a <code>SAML SubjectConfirmation</code> object
     *
     * @param inResponseTo  the request id of the <code>AuthnRequest</code>
     * @param acsURL        the <code>ACS</code> service <code>url</code>
     * @param effectiveTime the effective time of the assertion
     * @return the <code>SAML SubjectConfirmation</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    private static SubjectConfirmation getSubjectConfirmation(
            String inResponseTo, String acsURL, int effectiveTime)
            throws SAML2Exception {

        SubjectConfirmation sc = AssertionFactory.getInstance().
                createSubjectConfirmation();

        sc.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);

        SubjectConfirmationData scd = AssertionFactory.getInstance().
                createSubjectConfirmationData();

        scd.setRecipient(XMLUtils.escapeSpecialCharacters(acsURL));

        if (inResponseTo != null) {
            scd.setInResponseTo(inResponseTo);
        }

        Date date = new Date();
        date.setTime(date.getTime() + effectiveTime * 1000);
        scd.setNotOnOrAfter(date);
        sc.setSubjectConfirmationData(scd);

        return sc;
    }


    /**
     * Returns a <code>SAML Conditions</code> object
     *
     * @param audienceEntityID the entity id of the audience
     * @param effectiveTime    the effective time of the assertion
     * @return the <code>SAML Conditions</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    protected static Conditions getConditions(String audienceEntityID,
                                              int notBeforeSkewTime, int effectiveTime) throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getConditions: ";

        Conditions conditions = AssertionFactory.getInstance().
                createConditions();
        Date date = new Date();
        date.setTime(date.getTime() - notBeforeSkewTime * 1000);
        conditions.setNotBefore(date);

        date = new Date();
        date.setTime(date.getTime() + effectiveTime * 1000);
        conditions.setNotOnOrAfter(date);

        List list = new ArrayList();
        AudienceRestriction ar = getAudienceRestriction(audienceEntityID);
        if (ar == null) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get Audience Restriction");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("noAudienceRestriction"));
        }
        list.add(ar);

        conditions.setAudienceRestrictions(list);
        return conditions;
    }


    /**
     * Returns a <code>SAML AudienceRestriction</code> object
     *
     * @param audienceEntityID the entity id of the audience
     * @return the <code>SAML AudienceRestriction</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    private static AudienceRestriction getAudienceRestriction(
            String audienceEntityID) throws SAML2Exception {

        AudienceRestriction ar = AssertionFactory.getInstance().
                createAudienceRestriction();
        if (audienceEntityID != null) {
            List list = new ArrayList();
            list.add(audienceEntityID);
            ar.setAudience(list);
        }
        return ar;
    }


    /**
     * Returns the assertion consumer service <code>URL</code>
     *
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name of the identity provider
     * @param authnReq   the <code>AuthnRequest</code> object
     * @param request    the <code>HttpServletRequest</code> object
     * @param rBinding   the binding used to send back <code>Response</code>
     * @return the assertion consumer service <code>URL</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getACSurl(String spEntityID,
            String realm,
            AuthnRequest authnReq,
            HttpServletRequest request,
            StringBuffer rBinding)
            throws SAML2Exception {
        String acsURL = null;
        String acsBinding;
        Integer acsIndex = null;
        if (authnReq != null) {
            acsURL = authnReq.getAssertionConsumerServiceURL();
            acsBinding = authnReq.getProtocolBinding();
            acsIndex = authnReq.getAssertionConsumerServiceIndex();
        } else {
            acsBinding = request.getParameter(SAML2Constants.BINDING);
        }
        return getACSurl(spEntityID, realm, acsURL, acsBinding, acsIndex, request, rBinding);
    }

    /**
     * Returns the assertion consumer service <code>URL</code>.
     *
     * @param spEntityID The entity id of the service provider.
     * @param realm The realm name of the identity provider.
     * @param acsURL AssertionConsumerServiceURL in AuthnRequest.
     * @param binding ProtocolBinding in AuthnRequest.
     * @param index AssertionConsumerServiceIndex in AuthnRequest.
     * @param request The <code>HttpServletRequest</code> object.
     * @param rBinding The binding used to send back <code>Response</code>.
     * @return The assertion consumer service <code>URL</code>.
     * @throws SAML2Exception if the operation is not successful.
     */
    public static String getACSurl(String spEntityID, String realm, String acsURL, String binding, Integer index,
            HttpServletRequest request, StringBuffer rBinding) throws SAML2Exception {
        if (binding != null && !binding.trim().isEmpty()
                && !binding.startsWith(SAML2Constants.BINDING_PREFIX)) {
            // convert short format binding to long format
            binding = SAML2Constants.BINDING_PREFIX + binding;
        }
        if (acsURL == null || acsURL.length() == 0) {
            StringBuffer returnedBinding = new StringBuffer();
            if ((binding != null) && (binding.trim().length() != 0)) {
                acsURL = IDPSSOUtil.getACSurlFromMetaByBinding(spEntityID, realm, binding, returnedBinding);
            } else {
                int acsIndex;
                if (index == null) {
                    acsURL = getDefaultACSurl(spEntityID, realm, returnedBinding);
                } else {
                    acsIndex = index.intValue();
                    if (acsIndex < 0 || acsIndex > 65535) {
                        acsIndex = 0;
                    }
                    acsURL = IDPSSOUtil.getACSurlFromMetaByIndex(spEntityID, realm, acsIndex, returnedBinding);
                }
            }
            binding = returnedBinding.toString();
        } else {
            if (isACSurlValidInMetadataSP(acsURL, spEntityID, realm)) {
                if (binding == null || binding.isEmpty()) {
                    binding = getBindingForAcsUrl(spEntityID, realm, acsURL);
                }
            } else {
                String[] args = {acsURL, spEntityID};
                throw new SAML2Exception("libSAML2", "invalidAssertionConsumerServiceURL", args);
            }
        }
        rBinding.append(binding);
        return acsURL;
    }

    /**
     * Returns the default assertion consumer service url and binding
     * from the metadata.
     *
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name of the identity provider
     * @return the assertion consumer service url with returned binding.
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getDefaultACSurl(
            String spEntityID,
            String realm,
            StringBuffer returnedBinding) throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getDefaultACSurl: ";
        SPSSODescriptorElement spSSODescriptorElement = null;
        if (metaManager == null) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get meta manager.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorMetaManager"));
        }

        try {
            spSSODescriptorElement = metaManager.getSPSSODescriptor(
                    realm, spEntityID);
            if (spSSODescriptorElement == null) {
                SAML2Utils.debug.error(classMethod
                        + "Unable to get SP SSO Descriptor from meta.");
                String[] data = {spEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.SP_METADATA_ERROR, data, null);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get SP SSO Descriptor from meta.");
            String[] data = {spEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }
        List acsList = spSSODescriptorElement.getAssertionConsumerService();
        AssertionConsumerServiceElement acs = null;
        String acsURL = null;
        String binding = null;
        String firstAcsURL = null;
        String firstBinding = null;
        for (int i = 0; i < acsList.size(); i++) {
            acs = (AssertionConsumerServiceElement) acsList.get(i);
            if (acs.isIsDefault()) {
                acsURL = acs.getLocation();
                binding = acs.getBinding();
            }
            if (i == 0) {
                firstAcsURL = acs.getLocation();
                firstBinding = acs.getBinding();
            }
        }

        if (acsURL == null) {
            acsURL = firstAcsURL;
            binding = firstBinding;
        }

        if (binding != null) {
            returnedBinding.append(binding);
        }
        return acsURL;
    }

    /**
     * Returns the assertion consumer service url binding from
     * the metadata.
     *
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name of the identity provider
     * @return the assertion consumer service url binding
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getBindingForAcsUrl(
            String spEntityID,
            String realm,
            String acsURL) throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getBindingForAcsUrl: ";
        SPSSODescriptorElement spSSODescriptorElement = null;
        if (metaManager == null) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get meta manager.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorMetaManager"));
        }

        try {
            spSSODescriptorElement = metaManager.getSPSSODescriptor(
                    realm, spEntityID);
            if (spSSODescriptorElement == null) {
                SAML2Utils.debug.error(classMethod
                        + "Unable to get SP SSO Descriptor from meta.");
                String[] data = {spEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.SP_METADATA_ERROR, data, null);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get SP SSO Descriptor from meta.");
            String[] data = {spEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }
        List acsList = spSSODescriptorElement.getAssertionConsumerService();
        AssertionConsumerServiceElement acs = null;
        String binding = null;
        for (int i = 0; i < acsList.size(); i++) {
            acs = (AssertionConsumerServiceElement) acsList.get(i);
            String location = acs.getLocation();
            if (location != null && location.equals(acsURL)) {
                return acs.getBinding();
            }
        }
        return null;
    }

    /**
     * Returns the assertion consumer service <code>URL</code> from
     * meta data by binding
     *
     * @param spEntityID      the entity id of the service provider
     * @param realm           the realm name of the identity provider
     * @param desiredBinding  the desired binding
     * @param returnedBinding the binding used to send back
     *                        <code>Response</code>
     * @return the assertion consumer service <code>URL</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getACSurlFromMetaByBinding(
            String spEntityID,
            String realm,
            String desiredBinding,
            StringBuffer returnedBinding)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getACSurlFromMetaByBinding: ";
        SPSSODescriptorElement spSSODescriptorElement = null;
        if (metaManager == null) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get meta manager.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorMetaManager"));
        }

        try {
            spSSODescriptorElement = metaManager.getSPSSODescriptor(
                    realm, spEntityID);
            if (spSSODescriptorElement == null) {
                SAML2Utils.debug.error(classMethod
                        + "Unable to get SP SSO Descriptor from meta.");
                String[] data = {spEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.SP_METADATA_ERROR, data, null);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get SP SSO Descriptor from meta.");
            String[] data = {spEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        List acsList = spSSODescriptorElement.getAssertionConsumerService();
        String acsURL = null;
        String binding = null;
        String defaultAcsURL = null;
        String defaultBinding = null;
        String firstAcsURL = null;
        String firstBinding = null;
        AssertionConsumerServiceElement acs = null;
        for (int i = 0; i < acsList.size(); i++) {
            acs = (AssertionConsumerServiceElement) acsList.get(i);

            binding = acs.getBinding();
            if (binding.equals(desiredBinding)) {
                acsURL = acs.getLocation();
                break;
            }
            if (acs.isIsDefault()) {
                defaultAcsURL = acs.getLocation();
                defaultBinding = acs.getBinding();
            }
            if (i == 0) {
                firstAcsURL = acs.getLocation();
                firstBinding = acs.getBinding();
            }
        }
        if (acsURL == null || acsURL.length() == 0) {
            acsURL = defaultAcsURL;
            if (acsURL == null || acsURL.length() == 0) {
                acsURL = firstAcsURL;
                if (acsURL == null || acsURL.length() == 0) {
                    acsURL = null;
                    SAML2Utils.debug.error(classMethod +
                            "Unable to get valid Assertion " +
                            "Consumer Service URL");
                    return null;
                }
                returnedBinding.append(firstBinding);
            } else {
                returnedBinding.append(defaultBinding);
            }
        } else {
            returnedBinding.append(binding);
        }
        return acsURL;
    }


    /**
     * Returns the assertion consumer service <code>URL</code> from
     * meta data by binding
     *
     * @param spEntityID      the entity id of the service provider
     * @param realm           the realm name of the identity provider
     * @param acsIndex        the <code>ACS</code> index
     * @param returnedBinding the binding used to send back
     *                        <code>Response</code>
     * @return the assertion consumer service <code>URL</code>
     * @throws SAML2Exception if the operation is not successful
     */
    public static String getACSurlFromMetaByIndex(
            String spEntityID,
            String realm,
            int acsIndex,
            StringBuffer returnedBinding)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.getACSurlFromMetaByIndex: ";

        SPSSODescriptorElement spSSODescriptorElement = null;
        if (metaManager == null) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get meta manager.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorMetaManager"));
        }

        try {
            spSSODescriptorElement = metaManager.getSPSSODescriptor(
                    realm, spEntityID);
            if (spSSODescriptorElement == null) {
                SAML2Utils.debug.error(classMethod
                        + "Unable to get SP SSO Descriptor from meta.");
                String[] data = {spEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.SP_METADATA_ERROR, data, null);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get SP SSO Descriptor from meta.");
            String[] data = {spEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        List acsList = spSSODescriptorElement.getAssertionConsumerService();
        int index;
        String acsURL = null;
        String binding = null;
        String defaultAcsURL = null;
        String defaultBinding = null;
        String firstAcsURL = null;
        String firstBinding = null;
        AssertionConsumerServiceElement acs = null;
        for (int i = 0; i < acsList.size(); i++) {
            acs = (AssertionConsumerServiceElement) acsList.get(i);

            index = acs.getIndex();
            binding = acs.getBinding();
            if (index == acsIndex) {
                acsURL = acs.getLocation();
                binding = acs.getBinding();
                break;
            }
            if (acs.isIsDefault()) {
                defaultAcsURL = acs.getLocation();
                defaultBinding = acs.getBinding();
            }
            if (i == 0) {
                firstAcsURL = acs.getLocation();
                firstBinding = acs.getBinding();
            }
        }
        if (acsURL == null || acsURL.length() == 0) {
            acsURL = defaultAcsURL;
            if (acsURL == null || acsURL.length() == 0) {
                acsURL = firstAcsURL;
                if (acsURL == null || acsURL.length() == 0) {
                    acsURL = null;
                    SAML2Utils.debug.error(classMethod +
                            "Unable to get valid Assertion " +
                            "Consumer Service URL");
                    return null;
                }
                returnedBinding.append(firstBinding);
            } else {
                returnedBinding.append(defaultBinding);
            }
        } else {
            returnedBinding.append(binding);
        }
        return acsURL;
    }

    /**
     * This method opens a URL connection to the target specified and
     * sends artifact response to it using the
     * <code>HttpServletResponse</code> object.
     *
     * @param response    the <code>HttpServletResponse</code> object
     * @param idpEntityID the entity id of the identity provider
     * @param realm       the realm name of the identity provider
     * @param acsURL      the assertion consumer service <code>URL</code>
     * @param relayState  the value of the <code>RelayState</code>
     * @param res         the <code>SAML Response</code> object
     * @param session     user session
     * @param props       property map including nameIDString for logging
     * @throws SAML2Exception if the operation is not successful
     */
    public static void sendResponseArtifact(HttpServletRequest request,
                                            HttpServletResponse response,
                                            String idpEntityID, String spEntityID, String realm, String acsURL,
                                            String relayState, Response res, Object session, Map props)
            throws SAML2Exception {

        String classMethod = "IDPSSOUtil.sendResponseArtifact: ";

        IDPSSODescriptorElement idpSSODescriptorElement = null;
        try {
            idpSSODescriptorElement = metaManager.getIDPSSODescriptor(
                    realm, idpEntityID);
            if (idpSSODescriptorElement == null) {
                SAML2Utils.debug.error(classMethod
                        + "Unable to get IDP SSO Descriptor from meta.");
                String[] data = {idpEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.IDP_METADATA_ERROR, data, session, props);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get IDP SSO Descriptor from meta.");
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.IDP_METADATA_ERROR, data, session, props);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        ArtifactResolutionServiceElement ars =
                (ArtifactResolutionServiceElement)
                        idpSSODescriptorElement.getArtifactResolutionService().get(0);
        if (ars == null) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get ArtifactResolutionServiceElement from meta.");
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.IDP_METADATA_ERROR, data, session, props);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        Artifact art = null;
        try {
            art = ProtocolFactory.getInstance().createArtifact(
                    null,
                    ars.getIndex(),
                    SAML2Utils.generateSourceID(idpEntityID),
                    SAML2Utils.generateMessageHandleWithServerID()
            );
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to create artifact: ", se);
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.CANNOT_CREATE_ARTIFACT, data, session, props);
            SAMLUtils.sendError(request, response,
                    response.SC_INTERNAL_SERVER_ERROR, "errorCreateArtifact",
                    SAML2Utils.bundle.getString("errorCreateArtifact"));
            return;
        }
        String artStr = art.getArtifactValue();
        try {
            IDPCache.responsesByArtifacts.put(artStr, res);
            if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                try {
                    long expireTime = getValidTimeofResponse(realm, idpEntityID, res) / 1000;
                    SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(artStr, res.toXMLString(true, true), expireTime);
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod + "Saved Response to SAML2 Token Repository using key "
                                + artStr);
                    }
                } catch (SAML2TokenRepositoryException se) {
                    SAML2Utils.debug.error(classMethod + "Unable to save Response to the SAML2 Token Repository", se);
                }
            }

            String messageEncoding = SAML2Utils.getAttributeValueFromSSOConfig(
                    realm, spEntityID, SAML2Constants.SP_ROLE,
                    SAML2Constants.RESPONSE_ARTIFACT_MESSAGE_ENCODING);

            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                        "messageEncoding = " + messageEncoding);
                SAML2Utils.debug.message(classMethod +
                        "artStr = " + artStr);
            }

            if ((messageEncoding != null) &&
                    (messageEncoding.equals(SAML2Constants.FORM_ENCODING))) {

                String[] logdata = {idpEntityID, realm, acsURL};
                LogUtil.access(Level.INFO, LogUtil.SEND_ARTIFACT, logdata,
                        session, props);

                SAML2Utils.postToTarget(request, response, SAML2Constants.SAML_ART,
                        artStr, "RelayState", relayState, acsURL);
            } else {
                String redirectURL = acsURL +
                        (acsURL.contains("?") ? "&" : "?") + "SAMLart=" +
                        URLEncDec.encode(artStr);
                if ((relayState != null) && (relayState.trim().length() != 0)) {
                    redirectURL += "&RelayState=" +
                            URLEncDec.encode(relayState);
                }
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                            "Redirect URL = " + redirectURL);
                }

                String[] logdata = {idpEntityID, realm, redirectURL};
                LogUtil.access(Level.INFO, LogUtil.SEND_ARTIFACT, logdata,
                        session, props);
                response.sendRedirect(redirectURL);
            }
        } catch (IOException ioe) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to send redirect: ", ioe);
        }
    }

    /**
     * This method sends SAML Response back to ECP.
     *
     * @param response    the <code>HttpServletResponse</code> object
     * @param idpEntityID the entity id of the identity provider
     * @param realm       the realm name of the identity provider
     * @param acsURL      the assertion consumer service <code>URL</code>
     * @param res         the <code>SAML Response</code> object
     * @throws SAML2Exception if the operation is not successful
     */
    public static void sendResponseECP(HttpServletRequest request,
                                       HttpServletResponse response,
                                       String idpEntityID, String realm, String acsURL,
                                       Response res) throws SAML2Exception {

        ECPFactory ecpFactory = ECPFactory.getInstance();
        ECPResponse ecpResponse = ecpFactory.createECPResponse();
        ecpResponse.setMustUnderstand(Boolean.TRUE);
        ecpResponse.setActor(SAML2Constants.SOAP_ACTOR_NEXT);
        ecpResponse.setAssertionConsumerServiceURL(acsURL);

        String header = ecpResponse.toXMLString(true, true);
        String body = res.toXMLString(true, true);

        try {
            SOAPMessage reply = SAML2Utils.createSOAPMessage(header, body,
                    false);

            String[] logdata = {idpEntityID, realm, acsURL, ""};
            if (LogUtil.isAccessLoggable(Level.FINE)) {
                logdata[3] = SAML2Utils.soapMessageToString(reply);
            }
            LogUtil.access(Level.INFO, LogUtil.SEND_ECP_RESPONSE, logdata,
                    null);

            // Need to call saveChanges because we're
            // going to use the MimeHeaders to set HTTP
            // response information. These MimeHeaders
            // are generated as part of the save.
            if (reply.saveRequired()) {
                reply.saveChanges();
            }

            response.setStatus(HttpServletResponse.SC_OK);
            SAML2Utils.putHeaders(reply.getMimeHeaders(), response);

            // Write out the message on the response stream
            OutputStream os = response.getOutputStream();
            reply.writeTo(os);
            os.flush();
        } catch (Exception ex) {
            SAML2Utils.debug.error("IDPSSOUtil.sendResponseECP", ex);
            String[] data = {idpEntityID, realm, acsURL};
            LogUtil.error(Level.INFO, LogUtil.SEND_ECP_RESPONSE_FAILED, data,
                    null);
            SAMLUtils.sendError(request, response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "failedToSendECPResponse", ex.getMessage());
            return;
        }
    }

    /**
     * Returns the session index of an <code>IDPSession</code>
     *
     * @param session the session corresponding to the <code>IDPSession</code>
     * @return the session index string
     */
    public static String getSessionIndex(Object session) {

        String classMethod = "IDPSSOUtil.getSessionIndex: ";
        if (session == null) {
            return null;
        }
        String[] values = null;
        try {
            values = sessionProvider.getProperty(
                    session, SAML2Constants.IDP_SESSION_INDEX);
        } catch (SessionException e) {
            SAML2Utils.debug.error(classMethod +
                    "error retrieving session index from the session: ", e);
            values = null;
        }
        if (values == null || values.length == 0) {
            return null;
        }
        String index = values[0];
        if (index == null || index.length() == 0) {
            return null;
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                    classMethod + "Returning sessionIndex=" + index);
        }
        return index;
    }


    /**
     * Returns the authentication service <code>URL</code> of the
     * identity provider
     *
     * @param realm        the realm name of the identity provider
     * @param hostEntityId the entity id of the identity provider
     * @param request      the <code>HttpServletRequest</code> object
     * @return the authentication service <code>URL</code> of the
     *         identity provider
     */

    public static String getAuthenticationServiceURL(
            String realm,
            String hostEntityId,
            HttpServletRequest request) {
        String classMethod = "IDPSSOUtil.getAuthenticationServiceURL: ";
        String authUrl = getAttributeValueFromIDPSSOConfig(
                realm, hostEntityId, SAML2Constants.AUTH_URL);
        if ((authUrl == null) || (authUrl.trim().length() == 0)) {
            // need to get it from the request
            String uri = request.getRequestURI();
            String deploymentURI = uri;
            int firstSlashIndex = uri.indexOf("/");
            int secondSlashIndex = uri.indexOf("/", firstSlashIndex + 1);
            if (secondSlashIndex != -1) {
                deploymentURI = uri.substring(0, secondSlashIndex);
            }
            StringBuffer sb = new StringBuffer(100);
            sb.append(request.getScheme()).append("://")
                    .append(request.getServerName()).append(":")
                    .append(request.getServerPort())
                    .append(deploymentURI)
                    .append("/UI/Login?realm=").append(realm);
            authUrl = sb.toString();
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod + "auth url=:" + authUrl);
        }
        return authUrl;
    }

    public static String getAttributeValueFromIDPSSOConfig(
            String realm,
            String hostEntityId,
            String attrName) {
        String classMethod = "IDPSSOUtil.getAttributeValueFromIDPSSOConfig: ";
        String result = null;
        try {
            IDPSSOConfigElement config = metaManager.getIDPSSOConfig(
                    realm, hostEntityId);
            Map attrs = SAML2MetaUtils.getAttributes(config);
            List value = (List) attrs.get(attrName);
            if (value != null && value.size() != 0) {
                result = (String) value.get(0);
            }
        } catch (SAML2MetaException sme) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                        "get IDPSSOConfig failed:", sme);
            }
            result = null;
        }
        return result;
    }

    /**
     * Redirects to authenticate service
     *
     * @param request     the <code>HttpServletRequest</code> object
     * @param response    the <code>HttpServletResponse</code> object
     * @param authnReq    the <code>AuthnRequest</code> object
     * @param reqID       the <code>AuthnRequest ID</code>
     * @param realm       the realm name of the identity provider
     * @param idpEntityID the entity id of the identity provider
     * @param spEntityID  the entity id of the service provider
     */
    static void redirectAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnReq,
            String reqID,
            String realm,
            String idpEntityID,
            String spEntityID)
            throws SAML2Exception, IOException {
        String classMethod = "IDPSSOUtil.redirectAuthentication: ";
        // get the authentication service url 
        StringBuffer newURL = new StringBuffer(
                IDPSSOUtil.getAuthenticationServiceURL(
                        realm, idpEntityID, request));

        // Pass spEntityID to IdP Auth Module
        if (spEntityID != null) {
            if (newURL.indexOf("?") == -1) {
                newURL.append("?");
            } else {
                newURL.append("&");
            }
            newURL.append(SAML2Constants.SPENTITYID);
            newURL.append("=");
            newURL.append(URLEncDec.encode(spEntityID));
        }

        // find out the authentication method, e.g. module=LDAP, from
        // authn context mapping 
        IDPAuthnContextMapper idpAuthnContextMapper =
                getIDPAuthnContextMapper(realm, idpEntityID);

        IDPAuthnContextInfo info =
                idpAuthnContextMapper.getIDPAuthnContextInfo(
                        authnReq, idpEntityID, realm);
        Set authnTypeAndValues = info.getAuthnTypeAndValues();
        if ((authnTypeAndValues != null)
                && (!authnTypeAndValues.isEmpty())) {
            Iterator iter = authnTypeAndValues.iterator();
            StringBuffer authSB = new StringBuffer((String) iter.next());
            while (iter.hasNext()) {
                authSB.append("&");
                authSB.append((String) iter.next());
            }
            if (newURL.indexOf("?") == -1) {
                newURL.append("?");
            } else {
                newURL.append("&");
            }
            newURL.append(authSB.toString());
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                        "authString=" + authSB.toString());
            }
        }
        if (newURL.indexOf("?") == -1) {
            newURL.append("?goto=");
        } else {
            newURL.append("&goto=");
        }

        String gotoURL = request.getRequestURL().toString();
        String gotoQuery = request.getQueryString();
        if (gotoQuery != null) {
            gotoURL += "?" + gotoQuery;
            if (reqID != null) {
                gotoURL += "&ReqID=" + reqID;
            }
        } else {
            if (reqID != null) {
                gotoURL += "?ReqID=" + reqID;
            }
        }

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod +
                    "gotoURL=" + gotoURL);
        }

        newURL.append(URLEncDec.encode(gotoURL));
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod +
                    "New URL for authentication: " + newURL.toString());
        }
        // TODO: here we should check if the new URL is one
        //       the same web container, if yes, forward,
        //       if not, redirect
        response.sendRedirect(newURL.toString());

        return;
    }

    /**
     * Signs an <code>Assertion</code>
     *
     * @param realm       the realm name of the identity provider
     * @param idpEntityID the entity id of the identity provider
     * @param assertion   The <code>Assertion</code> to be signed
     */
    static void signAssertion(String realm,
                              String idpEntityID,
                              Assertion assertion)
            throws SAML2Exception {
        String classMethod = "IDPSSOUtil.signAssertion: ";

        KeyProvider kp = KeyUtil.getKeyProviderInstance();
        if (kp == null) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get a key provider instance.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nullKeyProvider"));
        }
        String idpSignCertAlias = SAML2Utils.getSigningCertAlias(
                realm, idpEntityID, SAML2Constants.IDP_ROLE);
        if (idpSignCertAlias == null) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get the hosted IDP signing certificate alias.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }

        String encryptedKeyPass =
                SAML2Utils.getSigningCertEncryptedKeyPass(realm, idpEntityID, SAML2Constants.IDP_ROLE);
        PrivateKey key;
        if (encryptedKeyPass == null || encryptedKeyPass.isEmpty()) {
            key = kp.getPrivateKey(idpSignCertAlias);
        } else {
            key = kp.getPrivateKey(idpSignCertAlias, encryptedKeyPass);
        }

        assertion.sign(key, kp.getX509Certificate(idpSignCertAlias));
    }

    /**
     * Signs and encrypts the components of a <code>SAML Response</code>
     * based on the service provider meta data. If the flag of
     * encrypting <code>Assertion</code> is on, then the embedded
     * <code>Assertion</code> object will be encrypted; if the flag
     * of encrypting <code>Assertion</code> is off and the flag of
     * encrypting <code>NameID</code> is on, then the <code>NameID</code>
     * embedded in the <code>Assertion</code> will be encrypted; if the
     * flag of encrypting <code>Assertion</code> is off and the flag of
     * encrypting <code>Attribute</code> is on, then the
     * <code>Attribute</code> embedded in the <code>Assertion</code>
     * will be encrypted. If the flag signAssertion is on, then the
     * <code>Assertion</code> will be signed. It will be signed before
     * it is encrypted and after its embedded <code>NameID</code> or
     * <code>Attribute</code> is encrypted.
     *
     * @param realm         the realm name of the identity provider
     * @param spEntityID    the entity id of the service provider
     * @param idpEntityID   the entity id of the identity provider
     * @param res           The <code>Response</code> whose components may be
     *                      encrypted based on the service provider meta data setting
     * @param signAssertion A flag to indicate if <code>Assertion</code>
     *                      signing is required
     */
    static void signAndEncryptResponseComponents(String realm,
                                                 String spEntityID,
                                                 String idpEntityID,
                                                 Response res,
                                                 boolean signAssertion)
            throws SAML2Exception {
        String classMethod = "IDPSSOUtil.signAndEncryptResponseComponents: ";
        boolean toEncryptAssertion = false;
        boolean toEncryptNameID = false;
        boolean toEncryptAttribute = false;

        if (res == null) {
            return;
        }

        List assertions = res.getAssertion();
        if ((assertions == null) || (assertions.size() == 0)) {
            return;
        }

        Assertion assertion = (Assertion) assertions.get(0);

        // get the encryption related flags from the SP Entity Config
        String wantAssertionEncrypted =
                SAML2Utils.getAttributeValueFromSSOConfig(
                        realm, spEntityID, SAML2Constants.SP_ROLE,
                        SAML2Constants.WANT_ASSERTION_ENCRYPTED);
        toEncryptAssertion = (wantAssertionEncrypted != null)
                && (wantAssertionEncrypted.equals(SAML2Constants.TRUE));
        if (!toEncryptAssertion) {
            String wantNameIDEncrypted =
                    SAML2Utils.getAttributeValueFromSSOConfig(
                            realm, spEntityID, SAML2Constants.SP_ROLE,
                            SAML2Constants.WANT_NAMEID_ENCRYPTED);
            toEncryptNameID = (wantNameIDEncrypted != null)
                    && (wantNameIDEncrypted.equals(SAML2Constants.TRUE));

            String wantAttributeEncrypted =
                    SAML2Utils.getAttributeValueFromSSOConfig(
                            realm, spEntityID, SAML2Constants.SP_ROLE,
                            SAML2Constants.WANT_ATTRIBUTE_ENCRYPTED);
            toEncryptAttribute = (wantAttributeEncrypted != null)
                    && (wantAttributeEncrypted.equals(SAML2Constants.TRUE));
        }

        if ((!toEncryptAssertion) && (!toEncryptNameID)
                && (!toEncryptAttribute)) {
            // all encryption flags are off, no encryption needed
            if (signAssertion) {
                signAssertion(realm, idpEntityID, assertion);
                List assertionList = new ArrayList();
                assertionList.add(assertion);
                res.setAssertion(assertionList);
            }
            return;
        }

        SPSSODescriptorElement spSSODescriptorElement = null;
        if (metaManager == null) {
            SAML2Utils.debug.error(classMethod + "Unable to get meta manager.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorMetaManager"));
        }

        try {
            spSSODescriptorElement = metaManager.getSPSSODescriptor(
                    realm, spEntityID);
            if (spSSODescriptorElement == null) {
                SAML2Utils.debug.error(classMethod
                        + "Unable to get SP SSO Descriptor from meta.");
                String[] data = {spEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.SP_METADATA_ERROR, data, null);
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get SP SSO Descriptor from meta.");
            String[] data = {spEntityID};
            LogUtil.error(Level.INFO,
                    LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }
        // get the encryption information 
        EncInfo encInfo = KeyUtil.getEncInfo(spSSODescriptorElement,
                spEntityID, SAML2Constants.SP_ROLE);
        if (encInfo == null) {
            SAML2Utils.debug.error(classMethod +
                    "failed to get service provider encryption key info.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("UnableToFindEncryptKeyInfo"));
        }
        if (toEncryptAssertion) {
            // sign assertion first, then encrypt the assertion
            if (signAssertion) {
                signAssertion(realm, idpEntityID, assertion);
            }
            // we only encrypt the Assertion
            EncryptedAssertion encryptedAssertion = assertion.encrypt(
                    encInfo.getWrappingKey(), encInfo.getDataEncAlgorithm(),
                    encInfo.getDataEncStrength(), spEntityID);
            if (encryptedAssertion == null) {
                SAML2Utils.debug.error(classMethod +
                        "failed to encrypt the assertion.");
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("FailedToEncryptAssertion"));
            }
            List assertionList = new ArrayList();
            assertionList.add(encryptedAssertion);
            res.setEncryptedAssertion(assertionList);
            res.setAssertion(new ArrayList()); // reset assertion list
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod + "Assertion encrypted.");
            }
        } else {
            // we only encrypt NameID and/or Attribute.
            // encrypt NameID and/or Attribute first, then sign the 
            // assertion if applicable
            if (toEncryptNameID) {
                // we need to encrypt the NameID            
                Subject subject = assertion.getSubject();
                if (subject == null) {
                    return;
                }
                NameID nameID = subject.getNameID();
                if (nameID == null) {
                    return;
                }
                EncryptedID encryptedNameID = nameID.encrypt(
                        encInfo.getWrappingKey(),
                        encInfo.getDataEncAlgorithm(),
                        encInfo.getDataEncStrength(), spEntityID);
                if (encryptedNameID == null) {
                    SAML2Utils.debug.error(classMethod +
                            "failed to encrypt the NameID.");
                    throw new SAML2Exception(
                            SAML2Utils.bundle.getString(
                                    "FailedToEncryptNameID"));
                }
                subject.setEncryptedID(encryptedNameID);
                subject.setNameID(null); // reset NameID
                assertion.setSubject(subject);
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod + "NameID encrypted.");
                }
            }

            if (toEncryptAttribute) {
                // we need to encrypt the Attribute
                List attributeStatements = assertion.getAttributeStatements();
                if ((attributeStatements != null)
                        && (attributeStatements.size() > 0)) {
                    int asSize = attributeStatements.size();
                    // to hold all the AttributeStatements
                    List stmts = new ArrayList();
                    for (int i = 0; i < asSize; i++) {
                        AttributeStatement attributeStatement =
                                (AttributeStatement) attributeStatements.get(i);
                        List attributes = attributeStatement.getAttribute();
                        if ((attributes == null) || (attributes.size() == 0)) {
                            continue;
                        }
                        int aSize = attributes.size();
                        // holds all the encrypted Attributes in this statement
                        List eaList = new ArrayList();
                        for (int j = 0; j < aSize; j++) {
                            Attribute attribute = (Attribute) attributes.get(j);
                            EncryptedAttribute encryptedAttribute =
                                    attribute.encrypt(
                                            encInfo.getWrappingKey(),
                                            encInfo.getDataEncAlgorithm(),
                                            encInfo.getDataEncStrength(), spEntityID);
                            if (encryptedAttribute == null) {
                                SAML2Utils.debug.error(classMethod +
                                        "failed to encrypt the Attribute.");
                                throw new SAML2Exception(
                                        SAML2Utils.bundle.getString(
                                                "FailedToEncryptAttribute"));
                            }
                            eaList.add(encryptedAttribute);
                        }
                        attributeStatement.setEncryptedAttribute(eaList);
                        attributeStatement.setAttribute(new ArrayList());
                        stmts.add(attributeStatement);
                    }
                    assertion.setAttributeStatements(stmts);
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod +
                                "Attribute encrypted.");
                    }
                }
            }
            if (signAssertion) {
                signAssertion(realm, idpEntityID, assertion);
            }
            List assertionList = new ArrayList();
            assertionList.add(assertion);
            res.setAssertion(assertionList);
        }
    }

    private static String getWriterURL(String realm,
                                       String idpEntityID,
                                       String spEntityID) {

        String classMethod = "IDPSSOUtil.getWriterURL: ";
        String writerURL = null;
        try {
            // get cot list of the idp
            IDPSSOConfigElement idpEntityCfg =
                    metaManager.getIDPSSOConfig(realm, idpEntityID);
            Map idpConfigAttrsMap = null;
            if (idpEntityCfg != null) {
                idpConfigAttrsMap = SAML2MetaUtils.getAttributes(idpEntityCfg);
            }
            if ((idpConfigAttrsMap == null) || (idpConfigAttrsMap.size() == 0)) {
                return null;
            }
            List idpCOTList =
                    (List) idpConfigAttrsMap.get(SAML2Constants.COT_LIST);
            if ((idpCOTList == null) || (idpCOTList.size() == 0)) {
                return null;
            }

            // get cot list of the sp
            SPSSOConfigElement spEntityCfg =
                    metaManager.getSPSSOConfig(realm, spEntityID);
            Map spConfigAttrsMap = null;
            if (spEntityCfg != null) {
                spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
            }
            if ((spConfigAttrsMap == null) || (spConfigAttrsMap.size() == 0)) {
                return null;
            }
            List spCOTList = (List) spConfigAttrsMap.get(SAML2Constants.COT_LIST);
            if ((spCOTList == null) || (spCOTList.size() == 0)) {
                return null;
            }

            // retain in the idpCOTList the intersection of two lists
            idpCOTList.retainAll(spCOTList);
            for (int i = 0; i < idpCOTList.size(); i++) {
                String cotName = (String) idpCOTList.get(i);

                CircleOfTrustDescriptor cotDescriptor =
                        cotManager.getCircleOfTrust(realm, cotName);
                writerURL = cotDescriptor.getSAML2WriterServiceURL();
                if ((writerURL != null) && (writerURL.trim().length() != 0)) {
                    break;
                }
            }
        } catch (COTException ce) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod + "Error retreiving of "
                        + "circle of trust", ce);
            }
        } catch (SAML2Exception se) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                        "Not able to getting writer URL : ", se);
            }
        } catch (Exception e) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                        "Not able to getting writer URL : ", e);
            }
        }
        return writerURL;
    }

    /**
     * Returns the effective time from the IDP
     * extended metadata . If the attreibute is not
     * defined in the metadata then defaults to
     * a value of 600 seconds (5 minutes).
     *
     * @return the effective time value in seconds.
     */
    protected static int getEffectiveTime(String realm, String idpEntityID) {
        int effectiveTime = SAML2Constants.ASSERTION_EFFECTIVE_TIME;
        String effectiveTimeStr = getAttributeValueFromIDPSSOConfig(
                realm, idpEntityID,
                SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE);
        if (effectiveTimeStr != null) {
            try {
                effectiveTime = Integer.parseInt(effectiveTimeStr);
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("IDPSSOUtil.getEffectiveTime: " +
                            "got effective time from config:" + effectiveTime);
                }
            } catch (NumberFormatException nfe) {
                SAML2Utils.debug.error("IDPSSOUtil.getEffectiveTime: " +
                        "Failed to get assertion effective time from " +
                        "IDP SSO config: ", nfe);
                effectiveTime = SAML2Constants.ASSERTION_EFFECTIVE_TIME;
            }
        }
        return effectiveTime;
    }

    /**
     * Returns the NotBefore skew time from the IDP
     * extended metadata . If the attreibute is not
     * defined in the metadata then defaults to
     * a value of 600 seconds (5 minutes).
     *
     * @return the NotBefore skew value in seconds.
     */
    protected static int getNotBeforeSkewTime(String realm,
                                              String idpEntityID) {
        String classMethod = "IDPSSOUtil.getNotBeforeSkewTime:";
        int notBeforeSkewTime =
                SAML2Constants.NOTBEFORE_ASSERTION_SKEW_DEFAULT;
        // get the assertion effective time (in seconds)
        String skewTimeStr = getAttributeValueFromIDPSSOConfig(
                realm, idpEntityID,
                SAML2Constants.ASSERTION_NOTBEFORE_SKEW_ATTRIBUTE);
        if (skewTimeStr != null) {
            try {
                notBeforeSkewTime = Integer.parseInt(skewTimeStr);
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                            "got NotBefore skew time from config:"
                            + notBeforeSkewTime);
                }
            } catch (NumberFormatException nfe) {
                SAML2Utils.debug.error(classMethod + "IDP SSO config: ", nfe);
                notBeforeSkewTime =
                        SAML2Constants.NOTBEFORE_ASSERTION_SKEW_DEFAULT;
            }
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod + "NotBefore Skew time :" +
                    notBeforeSkewTime);
        }
        return notBeforeSkewTime;
    }

    private static boolean assertionCacheEnabled(String realm,
                                                 String idpEntityID) {

        String enabled = SAML2Utils.getAttributeValueFromSSOConfig(realm,
                idpEntityID, SAML2Constants.IDP_ROLE,
                SAML2Constants.ASSERTION_CACHE_ENABLED);

        return "true".equalsIgnoreCase(enabled) ? true : false;
    }

    public static byte[] stringToByteArray(String input) {
        char chars[] = input.toCharArray();
        byte bytes[] = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    public static long getValidTimeofResponse(
            String realm, String idpEntityID, Response response)
            throws SAML2Exception {
        // in seconds
        int timeskew = SAML2Constants.ASSERTION_TIME_SKEW_DEFAULT;
        String timeskewStr = getAttributeValueFromIDPSSOConfig(
                realm,
                idpEntityID,
                SAML2Constants.ASSERTION_TIME_SKEW);
        if (timeskewStr != null && timeskewStr.trim().length() > 0) {
            timeskew = Integer.parseInt(timeskewStr);
            if (timeskew < 0) {
                timeskew = SAML2Constants.ASSERTION_TIME_SKEW_DEFAULT;
            }
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("timeskew = " + timeskew);
        }

        List assertions = response.getAssertion();
        if ((assertions == null) || (assertions.size() == 0)) {
            // failed case
            return (System.currentTimeMillis()
                    + getEffectiveTime(realm, idpEntityID)
                    + timeskew * 1000);
        }

        Assertion assertion = (Assertion) assertions.get(0);
        Conditions cond = assertion.getConditions();
        if (cond == null) {
            throw new SAML2Exception("nullConditions");
        }
        Date notOnOrAfter = cond.getNotOnOrAfter();

        long ret = notOnOrAfter.getTime() + timeskew * 1000;
        if (notOnOrAfter == null ||
                (ret < System.currentTimeMillis())) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("Time in Assertion "
                        + " is invalid.");
            }
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidTimeOnResponse"));
        }
        return ret;
    }

    /**
     * Signs SAMLv2 Response.
     *
     * @param realm       the realm name.
     * @param idpEntityID the identity provider entity identifier
     * @param response    the SAMLv2 <code>Response</code>
     * @throws <code>SAML2Exception</code> if there is an
     *                                     error signing the response.
     */
    private static void signResponse(String realm, String idpEntityID,
                                     Response response) throws SAML2Exception {
        String classMethod = "IDPSSOUtil:signResponse";
        KeyProvider kp = KeyUtil.getKeyProviderInstance();
        if (kp == null) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get a key provider instance.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nullKeyProvider"));

        }
        String idpSignCertAlias = SAML2Utils.getSigningCertAlias(
                realm, idpEntityID, SAML2Constants.IDP_ROLE);
        if (idpSignCertAlias == null) {
            SAML2Utils.debug.error(classMethod +
                    "Unable to get the hosted IDP signing certificate alias.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("missingSigningCertAlias"));

        }

        String encryptedKeyPass =
                SAML2Utils.getSigningCertEncryptedKeyPass(realm, idpEntityID, SAML2Constants.IDP_ROLE);
        PrivateKey key;
        if (encryptedKeyPass == null || encryptedKeyPass.isEmpty()) {
            key = kp.getPrivateKey(idpSignCertAlias);
        } else {
            key = kp.getPrivateKey(idpSignCertAlias, encryptedKeyPass);
        }

        response.sign(key, kp.getX509Certificate(idpSignCertAlias));
    }

    /**
     * Returns a <code>SAML2IdentityProviderAdapter</code>
     *
     * @param realm       the realm name
     * @param idpEntityID the entity id of the identity provider
     * @return the <code>SAML2IdenityProviderAdapter</code>
     * @throws SAML2Exception if the operation is not successful
     */
    static SAML2IdentityProviderAdapter getIDPAdapterClass(String realm, String idpEntityID)
            throws SAML2Exception {
        return SAML2Utils.getIDPAdapterClass(realm, idpEntityID);
    }

    /**
     * Returns  <code>true</code> or <code>false</code>
     * depending if the flag  spDoNotWriteFederationInfo is set in the
     * SP Extended metadata
     *
     * @param realm       the realm name
     * @param spEntityID  the entity id of the Service Provider
     * @param metaManager the SAML2MetaMAnager used to read the extendede metadata
     * @return the <code>true/false</code>
     * @throws SAML2Exception if the operation is not successful
     */
    private static Boolean isSPDoNotWriteFedInfoInIdP(
            String realm, String spEntityID, SAML2MetaManager metaManager)
            throws SAML2Exception {
        String methodName = "isSPDoNotWriteFedInfoInIdp";

        Boolean isSPDoNotWriteFedInfoEnabled = false;
        SAML2Utils.debug.message("IDPSSOUtil." + methodName + "Entering");

        try {
            String SPDoNotWriteFedInfo = getAttributeValueFromSPSSOConfig(realm,
                    spEntityID, metaManager,
                    SAML2Constants.SP_DO_NOT_WRITE_FEDERATION_INFO);

            if (SPDoNotWriteFedInfo != null && !SPDoNotWriteFedInfo.isEmpty()) {
                SAML2Utils.debug.message("IDPSSOUtil." + methodName +
                        ": SPDoNotWriteFedInfo is: " + SPDoNotWriteFedInfo);
                isSPDoNotWriteFedInfoEnabled = SPDoNotWriteFedInfo.equalsIgnoreCase("true");
            } else {
                SAML2Utils.debug.message("IDPSSOUtil." + methodName +
                        ": SPDoNotWriteFedInfo is: not configured");
                isSPDoNotWriteFedInfoEnabled = false;
            }
        } catch (Exception ex) {
            SAML2Utils.debug.error("IDPSSOUtil." + methodName +
                    "Unable to get the spDoNotWriteFedInfo flag.", ex);
            throw new SAML2Exception(ex);
        }

        return isSPDoNotWriteFedInfoEnabled;
    }

    /**
     * Retrieves attribute value for a given attribute name from
     * <code>SPSSOConfig</code>.
     *
     * @param orgName      realm or organization name the service provider resides in
     * @param hostEntityId hosted service provider's Entity ID.
     * @param sm           <code>SAML2MetaManager</code> instance to perform meta
     *                     operations.
     * @param attrName     name of the attribute whose value ot be retrived.
     * @return value of the attribute; or <code>null</code> if the attribute
     *         if not configured, or an error occured in the process.
     */
    private static String getAttributeValueFromSPSSOConfig(String orgName,
                                                           String hostEntityId,
                                                           SAML2MetaManager sm,
                                                           String attrName) {
        String result = null;
        try {
            SPSSOConfigElement config = sm.getSPSSOConfig(orgName,
                    hostEntityId);
            if (config == null) {
                return null;
            }
            Map attrs = SAML2MetaUtils.getAttributes(config);
            List value = (List) attrs.get(attrName);
            if (value != null && value.size() != 0) {
                result = ((String) value.iterator().next()).trim();
            }
        } catch (SAML2MetaException sme) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("IDPSSOUtil.getAttributeValueFromSPSSO"
                        + " Config:", sme);
            }
            result = null;
        }
        return result;
    }

    /**
     * Validates if the Assertion Consumer Service URL acsURL exists in the
     * metadata of the Service Provider spEntityID
     *
     * @param acsURL     the assertion consumer service <code>URL</code>
     * @param spEntityID the entity id of the service provider
     * @param realm      the realm name of the identity provider
     * @return true if the assertion consumer service URL was found
     *         false otherwise
     */
    private static boolean isACSurlValidInMetadataSP(String acsURL,
                                                     String spEntityID, String realm)
            throws SAML2Exception {

        boolean isValidACSurl = false;
        String classMethod = "IDPSSOUtil.isACSurlValidInMetadataSP: ";

        SPSSODescriptorElement spSSODescriptorElement = null;

        if (metaManager == null) {
            SAML2Utils.debug.error(classMethod + "Unable to get meta manager.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorMetaManager"));
        }

        try {
            spSSODescriptorElement = metaManager.getSPSSODescriptor(
                    realm, spEntityID);
            if (spSSODescriptorElement == null) {
                SAML2Utils.debug.error(classMethod
                        + "Unable to get SP SSO Descriptor from meta.");
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("metaDataError"));
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod
                    + "Unable to get SP SSO Descriptor from meta.");
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
        }

        List acsList = spSSODescriptorElement.getAssertionConsumerService();

        AssertionConsumerServiceElement acs = null;

        for (int i = 0; i < acsList.size(); i++) {
            acs = (AssertionConsumerServiceElement) acsList.get(i);
            String acsInMeta = acs.getLocation();
            if (acsInMeta.equalsIgnoreCase(acsURL)) {
                isValidACSurl = true;
                SAML2Utils.debug.message(classMethod + " acsURL=" + acsURL +
                        "Found in the metadata");
                break;
            }
        }

        return isValidACSurl;
    }
}
