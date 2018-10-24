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
* Copyright 2015-2016 ForgeRock AS.
*/
package org.forgerock.openam.saml2;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml2.common.QuerySignatureUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SingleSignOnServiceElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.plugins.IDPECPSessionMapper;
import com.sun.identity.saml2.profile.CacheObject;
import com.sun.identity.saml2.profile.ClientFaultException;
import com.sun.identity.saml2.profile.FederatedSSOException;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPProxyUtil;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.profile.IDPSession;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.profile.SPSSOFederate;
import com.sun.identity.saml2.profile.ServerFaultException;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * An implementation of A SAMLAuthenticator that uses the Util classes to make the federation connection.
 */
public class UtilProxySAMLAuthenticator extends SAMLBase implements SAMLAuthenticator {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private final IDPSSOFederateRequest data;
    private final PrintWriter out;

    private final boolean isFromECP;

    /**
     * Creates a new UtilProxySAMLAuthenticator using the detail provided.
     *
     * @param data      the request containing the details of the Federate request.
     * @param request   the Http request object.
     * @param response  the Http response object.
     * @param out       the print out.
     * @param isFromECP true if this request was made by an ECP.
     */
    public UtilProxySAMLAuthenticator(final IDPSSOFederateRequest data,
                                      final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final PrintWriter out,
                                      final boolean isFromECP) {
        this.data = data;
        this.out = out;
        this.request = request;
        this.response = response;
        this.isFromECP = isFromECP;
    }

    @Override
    public void authenticate() throws FederatedSSOException, IOException {

        final String classMethod = "UtilProxySAMLAuthenticator.authenticate: ";

        SPSSODescriptorElement spSSODescriptor = null;
        String preferredIDP;

        // There is no reqID, this is the first time that we pass here.
        String binding = SAML2Constants.HTTP_REDIRECT;
        if (request.getMethod().equals("POST")) {
            binding = SAML2Constants.HTTP_POST;
        }

        data.setAuthnRequest(getAuthnRequest(request, isFromECP, binding));
        if (data.getAuthnRequest() == null) {
            throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
        }
        data.getEventAuditor().setRequestId(data.getRequestID());

        data.setSpEntityID(data.getAuthnRequest().getIssuer().getValue());

        try {
            logAccess(isFromECP ? LogUtil.RECEIVED_AUTHN_REQUEST_ECP : LogUtil.RECEIVED_AUTHN_REQUEST, Level.INFO,
                    data.getSpEntityID(), data.getIdpMetaAlias(), data.getAuthnRequest().toXMLString());
        } catch (SAML2Exception saml2ex) {
            SAML2Utils.debug.error(classMethod, saml2ex);
            throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST, saml2ex.getMessage());
        }

        if (!SAML2Utils.isSourceSiteValid(data.getAuthnRequest().getIssuer(), data.getRealm(), data.getIdpEntityID())) {
            SAML2Utils.debug.warning("{} Issuer in Request is not valid.", classMethod);
            throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
        }

        // verify the signature of the query string if applicable
        IDPSSODescriptorElement idpSSODescriptor;
        try {
            idpSSODescriptor = IDPSSOUtil.metaManager.getIDPSSODescriptor(data.getRealm(), data.getIdpEntityID());
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod + "Unable to get IDP SSO Descriptor from meta.");
            throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
        }

        try {
            spSSODescriptor = IDPSSOUtil.metaManager.getSPSSODescriptor(data.getRealm(), data.getSpEntityID());
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod + "Unable to get SP SSO Descriptor from meta.");
            SAML2Utils.debug.error(classMethod, sme);
        }

        if (idpSSODescriptor.isWantAuthnRequestsSigned()
                || (spSSODescriptor != null && spSSODescriptor.isAuthnRequestsSigned())) {
            // need to verify the query string containing authnRequest
            if (StringUtils.isBlank(data.getSpEntityID())) {
                throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
            }

            if (spSSODescriptor == null) {
                SAML2Utils.debug.error(classMethod + "Unable to get SP SSO Descriptor from meta.");
                throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
            }

            Set<X509Certificate> certificates = KeyUtil.getVerificationCerts(spSSODescriptor, data.getSpEntityID(),
                    SAML2Constants.SP_ROLE);

            try {
                boolean isSignatureOK;
                if (isFromECP) {
                    isSignatureOK = data.getAuthnRequest().isSignatureValid(certificates);
                } else {
                    if ("POST".equals(request.getMethod())) {
                        isSignatureOK = data.getAuthnRequest().isSignatureValid(certificates);
                    } else {
                        isSignatureOK = QuerySignatureUtil.verify(request.getQueryString(), certificates);
                    }
                }
                if (!isSignatureOK) {
                    SAML2Utils.debug.error(classMethod + "authn request verification failed.");
                    throw new ClientFaultException(data.getIdpAdapter(), "invalidSignInRequest");
                }

                // In ECP profile, sp doesn't know idp.
                if (!isFromECP) {
                    // verify Destination
                    List<SingleSignOnServiceElement> ssoServiceList = idpSSODescriptor.getSingleSignOnService();
                    SingleSignOnServiceElement  endPoint = SPSSOFederate.getSingleSignOnServiceEndpoint(ssoServiceList, binding);
                    if (endPoint == null || StringUtils.isEmpty(endPoint.getLocation())) {
                        SAML2Utils.debug
                                .error("{} authn request unable to get endpoint location for IdpEntity: {}  MetaAlias: {} ",
                                        classMethod, data.getIdpEntityID(), data.getIdpMetaAlias());
                        throw new ClientFaultException(data.getIdpAdapter(), "invalidDestination");
                    }
                    if (!SAML2Utils
                            .verifyDestination(data.getAuthnRequest().getDestination(), endPoint.getLocation())) {
                        SAML2Utils.debug
                                .error("{} authn request destination verification failed for IdpEntity: {}  MetaAlias: {} Destination: {}  Location: {}",
                                        classMethod, data.getIdpEntityID(), data.getIdpMetaAlias(),
                                        data.getAuthnRequest().getDestination(), endPoint.getLocation());
                        throw new ClientFaultException(data.getIdpAdapter(), "invalidDestination");
                    }
                }
            } catch (SAML2Exception se) {
                SAML2Utils.debug.error(classMethod + "authn request verification failed.", se);
                throw new ClientFaultException(data.getIdpAdapter(), "invalidSignInRequest");
            }

            SAML2Utils.debug.message("{} authn request signature verification is successful.", classMethod);
        }

        SAML2Utils.debug.message("{} request id= {}", classMethod, data.getRequestID());

        if (data.getRequestID() == null) {
            SAML2Utils.debug.error(classMethod + "Request id is null");
            throw new ClientFaultException(data.getIdpAdapter(), "InvalidSAMLRequestID");
        }

        if (isFromECP) {
            try {
                IDPECPSessionMapper idpECPSessonMapper =
                        IDPSSOUtil.getIDPECPSessionMapper(data.getRealm(), data.getIdpEntityID());
                data.setSession(idpECPSessonMapper.getSession(request, response));
            } catch (SAML2Exception se) {
                SAML2Utils.debug.message("Unable to retrieve user session.", classMethod);
            }
        } else {
            // get the user sso session from the request
            try {
                data.setSession(SessionManager.getProvider().getSession(request));
            } catch (SessionException se) {
                SAML2Utils.debug.message("{} Unable to retrieve user session.", classMethod);
            }
        }
        if (null != data.getSession()) {
            data.getEventAuditor().setAuthTokenId(data.getSession());
        }

        // preSingleSignOn adapter hook
        // NB: This method is not called in IDPSSOUtil.doSSOFederate(...) so proxy requests or idp init sso
        // will not trigger this adapter call
        if (preSingleSignOn(request, response, data)) {
            return;
        }
        // End of adapter invocation

        IDPAuthnContextMapper idpAuthnContextMapper = null;
        try {
            idpAuthnContextMapper = IDPSSOUtil.getIDPAuthnContextMapper(data.getRealm(), data.getIdpEntityID());
        } catch (SAML2Exception sme) {
            SAML2Utils.debug.error(classMethod, sme);
        }
        if (idpAuthnContextMapper == null) {
            SAML2Utils.debug.error(classMethod + "Unable to get IDPAuthnContextMapper from meta.");
            throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
        }

        IDPAuthnContextInfo idpAuthnContextInfo = null;
        try {
            idpAuthnContextInfo = idpAuthnContextMapper.getIDPAuthnContextInfo(data.getAuthnRequest(),
                    data.getIdpEntityID(), data.getRealm());
        } catch (SAML2Exception sme) {
            SAML2Utils.debug.error(classMethod, sme);
        }

        if (idpAuthnContextInfo == null) {
            SAML2Utils.debug.message("{} Unable to find valid AuthnContext. Sending error Response.", classMethod);
            try {
                Response res = SAML2Utils.getErrorResponse(data.getAuthnRequest(), SAML2Constants.REQUESTER,
                        SAML2Constants.NO_AUTHN_CONTEXT, null, data.getIdpEntityID());
                StringBuffer returnedBinding = new StringBuffer();
                String acsURL = IDPSSOUtil.getACSurl(data.getSpEntityID(), data.getRealm(),
                        data.getAuthnRequest(), request, returnedBinding);
                String acsBinding = returnedBinding.toString();
                IDPSSOUtil.sendResponse(request, response, out, acsBinding, data.getSpEntityID(), data.getIdpEntityID(), data.getIdpMetaAlias(), data.getRealm(),
                        data.getRelayState(), acsURL, res, data.getSession());
            } catch (SAML2Exception sme) {
                SAML2Utils.debug.error(classMethod, sme);
                throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
            }
            return;
        }

        // get the relay state query parameter from the request
        data.setRelayState(request.getParameter(SAML2Constants.RELAY_STATE));
        data.setMatchingAuthnContext(idpAuthnContextInfo.getAuthnContext());

        if (data.getSession() == null) {
            // the user has not logged in yet, redirect to auth
            redirectToAuth(spSSODescriptor, binding, idpAuthnContextInfo, data);
        } else {
            SAML2Utils.debug.message("{} There is an existing session", classMethod);

            // Let's verify that the realm is the same for the user and the IdP
            boolean isValidSessionInRealm = IDPSSOUtil.isValidSessionInRealm(data.getRealm(), data.getSession());
            String sessionIndex = IDPSSOUtil.getSessionIndex(data.getSession());
            boolean sessionUpgrade = false;
            if (isValidSessionInRealm) {
                sessionUpgrade = isSessionUpgrade(idpAuthnContextInfo, data.getSession());
                SAML2Utils.debug.message("{} IDP Session Upgrade is : {}", classMethod, sessionUpgrade);
            }
            // Holder for any exception encountered while redirecting for authentication:
            FederatedSSOException redirectException = null;
            if (sessionUpgrade || !isValidSessionInRealm ||
                    ((Boolean.TRUE.equals(data.getAuthnRequest().isForceAuthn())) &&
                            (!Boolean.TRUE.equals(data.getAuthnRequest().isPassive())))) {

                // If there was no previous SAML2 session, there will be no
                // sessionIndex
                if (sessionIndex != null && sessionIndex.length() != 0) {
                    // Save the original IDP Session
                    IDPSession oldIDPSession = IDPCache.idpSessionsByIndices.get(sessionIndex);
                    if (oldIDPSession != null) {
                        IDPCache.oldIDPSessionCache.put(data.getRequestID(), oldIDPSession);
                    } else {
                        SAML2Utils.debug.error(classMethod + "The old SAML2 session  was not found in the idp session " +
                                "by indices cache");
                    }
                }

                // Save the new requestId and AuthnRequest
                IDPCache.authnRequestCache.put(data.getRequestID(), new CacheObject(data.getAuthnRequest()));
                // Save the new requestId and AuthnContext
                IDPCache.idpAuthnContextCache.put(data.getRequestID(), new CacheObject(data.getMatchingAuthnContext()));
                // save if the request was an Session Upgrade case.
                IDPCache.isSessionUpgradeCache.add(data.getRequestID());

                // save the relay state in the IDPCache so that it can
                // be retrieved later when the user successfully
                // authenticates
                if (StringUtils.isNotBlank(data.getRelayState())) {
                    IDPCache.relayStateCache.put(data.getRequestID(), data.getRelayState());
                }

                //IDP Proxy: Initiate proxying when session upgrade is requested
                // Session upgrade could be requested by asking a greater AuthnContext
                if (isValidSessionInRealm) {
                    try {
                        boolean isProxy = IDPProxyUtil.isIDPProxyEnabled(data.getAuthnRequest(), data.getRealm());
                        if (isProxy) {
                            preferredIDP = IDPProxyUtil.getPreferredIDP(data.getAuthnRequest(), data.getIdpEntityID(),
                                    data.getRealm(), request, response);
                            if (preferredIDP != null) {
                                if ((SPCache.reqParamHash != null)
                                        && (!(SPCache.reqParamHash.containsKey(preferredIDP)))) {
                                    // IDP Proxy with configured proxy list
                                    SAML2Utils.debug.message("{} IDP to be proxied {}", classMethod, preferredIDP);
                                    IDPProxyUtil.sendProxyAuthnRequest(data.getAuthnRequest(), preferredIDP, spSSODescriptor,
                                            data.getIdpEntityID(), request, response, data.getRealm(),
                                            data.getRelayState(), binding);
                                    return;
                                } else {
                                    // IDP proxy with introduction cookie
                                    Map paramsMap = (Map) SPCache.reqParamHash.get(preferredIDP);
                                    paramsMap.put("authnReq", data.getAuthnRequest());
                                    paramsMap.put("spSSODescriptor", spSSODescriptor);
                                    paramsMap.put("idpEntityID", data.getIdpEntityID());
                                    paramsMap.put("realm", data.getRealm());
                                    paramsMap.put("relayState", data.getRelayState());
                                    paramsMap.put("binding", binding);
                                    SPCache.reqParamHash.put(preferredIDP, paramsMap);
                                    return;
                                }
                            }
                        }
                        //else continue for the local authentication.
                    } catch (SAML2Exception re) {
                        SAML2Utils.debug.message("{} Redirecting for the proxy handling error: {}", classMethod,
                                re.getMessage());
                        redirectException = new ServerFaultException(data.getIdpAdapter(),
                                "UnableToRedirectToPreferredIDP", re.getMessage());
                    }
                    // End of IDP Proxy: Initiate proxying when session upgrade is requested

                }

                // Invoke the IDP Adapter before redirecting to authn
                if (preAuthenticationAdapter(request, response, data)) {
                    return;
                }
                // End of block for IDP Adapter invocation

                //we don't have a session
                try { //and they want to authenticate
                    if (!Boolean.TRUE.equals(data.getAuthnRequest().isPassive())) {
                        redirectAuthentication(request, response, idpAuthnContextInfo, data, true);
                        return;
                    } else {
                        try { //and they want to get into the system with passive auth - response no passive
                            IDPSSOUtil.sendResponseWithStatus(request, response, out, data.getIdpMetaAlias(),
                                    data.getIdpEntityID(), data.getRealm(), data.getAuthnRequest(),
                                    data.getRelayState(), data.getSpEntityID(), SAML2Constants.RESPONDER,
                                    SAML2Constants.NOPASSIVE);
                        } catch (SAML2Exception sme) {
                            SAML2Utils.debug.error(classMethod, sme);
                            redirectException = new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
                        }
                    }
                } catch (IOException | SAML2Exception e) {
                    SAML2Utils.debug.error(classMethod + "Unable to redirect to authentication.", e);
                    sessionUpgrade = false;
                    cleanUpCache(data.getRequestID());
                    redirectException = new ServerFaultException(data.getIdpAdapter(), "UnableToRedirectToAuth",
                            e.getMessage());
                }
            }

            // comes here if either no session upgrade or error redirecting to authentication url.
            // generate assertion response
            if (!sessionUpgrade && isValidSessionInRealm) {
                generateAssertionResponse(data);
            }

            if (redirectException != null) {
                throw redirectException;
            }
        }

    }


    /**
     * Iterates through the RequestedAuthnContext from the Service Provider and
     * check if user has already authenticated with a sufficient authentication
     * level.
     * <p/>
     * If RequestAuthnContext is not found in the authenticated AuthnContext
     * then session upgrade will be done .
     *
     * @return true if the requester requires to reauthenticate
     */
    private static boolean isSessionUpgrade(IDPAuthnContextInfo idpAuthnContextInfo, Object session) {

        String classMethod = "UtilProxySAMLAuthenticator.isSessionUpgrade: ";

        if (session != null) {
            // Get the Authentication Context required
            String authnClassRef = idpAuthnContextInfo.getAuthnContext().
                    getAuthnContextClassRef();
            // Get the AuthN level associated with the Authentication Context
            int authnLevel = idpAuthnContextInfo.getAuthnLevel();

            SAML2Utils.debug.message(classMethod + "Requested AuthnContext: authnClassRef=" + authnClassRef +
                    " authnLevel=" + authnLevel);

            int sessionAuthnLevel = 0;

            try {
                final String strAuthLevel = SessionManager.getProvider().getProperty(session,
                        SAML2Constants.AUTH_LEVEL)[0];
                if (strAuthLevel.contains(":")) {
                    String[] realmAuthLevel = strAuthLevel.split(":");
                    sessionAuthnLevel = Integer.parseInt(realmAuthLevel[1]);
                } else {
                    sessionAuthnLevel = Integer.parseInt(strAuthLevel);
                }
                SAML2Utils.debug.message(classMethod + "Current session Authentication Level: " + sessionAuthnLevel);
            } catch (SessionException sex) {
                SAML2Utils.debug.error(classMethod + " Couldn't get the session Auth Level", sex);
            }
            
            return authnLevel > sessionAuthnLevel;
        } else {
            return true;
        }
    }

    private void generateAssertionResponse(IDPSSOFederateRequest data) throws ServerFaultException {

        final String classMethod = "UtilProxySAMLAuthenticator.generateAssertionResponse";

        // IDP Adapter invocation, to be sure that we can execute the logic
        // even if there is a new request with the same session

        // save the AuthnRequest in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        synchronized (IDPCache.authnRequestCache) {
            IDPCache.authnRequestCache.put(data.getRequestID(), new CacheObject(data.getAuthnRequest()));
        }

        // save the AuthnContext in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        synchronized (IDPCache.idpAuthnContextCache) {
            IDPCache.idpAuthnContextCache.put(data.getRequestID(), new CacheObject(data.getMatchingAuthnContext()));
        }

        // save the relay state in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        if (StringUtils.isNotBlank(data.getRelayState())) {
            IDPCache.relayStateCache.put(data.getRequestID(), data.getRelayState());
        }

        if (preSendResponse(request, response, data)) {
            return;
        }
        // preSendResponse IDP adapter invocation ended

        // call multi-federation protocol to set the protocol
        MultiProtocolUtils.addFederationProtocol(data.getSession(), SingleLogoutManager.SAML2);
        NameIDPolicy policy = data.getAuthnRequest().getNameIDPolicy();
        String nameIDFormat = (policy == null) ? null : policy.getFormat();
        try {
            IDPSSOUtil.sendResponseToACS(request, response, out, data.getSession(), data.getAuthnRequest(),
                    data.getSpEntityID(), data.getIdpEntityID(), data.getIdpMetaAlias(), data.getRealm(),
                    nameIDFormat, data.getRelayState(), data.getMatchingAuthnContext());
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error(classMethod + "Unable to do sso or federation.", se);
            throw new ServerFaultException(data.getIdpAdapter(), SSO_OR_FEDERATION_ERROR, se.getMessage());
        }

    }

    private void redirectToAuth(SPSSODescriptorElement spSSODescriptor, String binding,
                                IDPAuthnContextInfo idpAuthnContextInfo, IDPSSOFederateRequest data)
            throws IOException, ServerFaultException {

        String classMethod = "UtilProxySAMLAuthenticator.redirectToAuth";
        String preferredIDP;

        // TODO: need to verify the signature of the AuthnRequest

        // save the AuthnRequest in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        synchronized (IDPCache.authnRequestCache) {
            IDPCache.authnRequestCache.put(data.getRequestID(), new CacheObject(data.getAuthnRequest()));
        }

        // save the AuthnContext in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        synchronized (IDPCache.idpAuthnContextCache) {
            IDPCache.idpAuthnContextCache.put(data.getRequestID(), new CacheObject(data.getMatchingAuthnContext()));
        }

        // save the relay state in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        if (StringUtils.isNotBlank(data.getRelayState())) {
            IDPCache.relayStateCache.put(data.getRequestID(), data.getRelayState());
        }

        //IDP Proxy: Initiate proxying
        try {
            boolean isProxy = IDPProxyUtil.isIDPProxyEnabled(data.getAuthnRequest(), data.getRealm());
            if (isProxy) {
                preferredIDP = IDPProxyUtil.getPreferredIDP(data.getAuthnRequest(), data.getIdpEntityID(),
                        data.getRealm(), request, response);
                if (preferredIDP != null) {
                    if ((SPCache.reqParamHash != null) && (!(SPCache.reqParamHash.containsKey(preferredIDP)))) {
                        // IDP Proxy with configured proxy list
                        SAML2Utils.debug.message("{} IDP to be proxied {} ", classMethod, preferredIDP);
                        IDPProxyUtil.sendProxyAuthnRequest(data.getAuthnRequest(), preferredIDP, spSSODescriptor,
                                data.getIdpEntityID(), request, response, data.getRealm(),
                                data.getRelayState(), binding);
                        return;
                    } else {
                        // IDP proxy with introduction cookie
                        Map paramsMap = (Map) SPCache.reqParamHash.get(preferredIDP);
                        paramsMap.put("authnReq", data.getAuthnRequest());
                        paramsMap.put("spSSODescriptor", spSSODescriptor);
                        paramsMap.put("idpEntityID", data.getIdpEntityID());
                        paramsMap.put("realm", data.getRealm());
                        paramsMap.put("relayState", data.getRelayState());
                        paramsMap.put("binding", binding);
                        SPCache.reqParamHash.put(preferredIDP, paramsMap);
                        return;
                    }
                }
            }
            //else continue for the local authentication.
        } catch (SAML2Exception re) {
            SAML2Utils.debug.message("{} Redirecting for the proxy handling error: {}", classMethod, re.getMessage());
            throw new ServerFaultException(data.getIdpAdapter(), "UnableToRedirectToPreferredIDP", re.getMessage());
        }

        // preAuthentication adapter hook
        if (preAuthenticationAdapter(request, response, data)) {
            return;
        }
        // End of adapter invocation

        // redirect to the authentication service
        try {
            if (!Boolean.TRUE.equals(data.getAuthnRequest().isPassive())) {
                redirectAuthentication(request, response, idpAuthnContextInfo, data, false);
            } else {
                try {
                    IDPSSOUtil.sendResponseWithStatus(request, response, out, data.getIdpMetaAlias(),
                            data.getIdpEntityID(), data.getRealm(), data.getAuthnRequest(), data.getRelayState(),
                            data.getSpEntityID(), SAML2Constants.RESPONDER, SAML2Constants.NOPASSIVE);
                } catch (SAML2Exception sme) {
                    SAML2Utils.debug.error(classMethod, sme);
                    throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
                }
            }
        } catch (IOException | SAML2Exception e) {
            SAML2Utils.debug.error(classMethod + "Unable to redirect to authentication.", e);
            throw new ServerFaultException(data.getIdpAdapter(), "UnableToRedirectToAuth", e.getMessage());
        }

    }

    /**
     * Returns the <code>AuthnRequest</code> from saml request string
     */
    private static AuthnRequest getAuthnRequest(String compressedReq) {

        AuthnRequest authnReq = null;
        String outputString = SAML2Utils.decodeFromRedirect(compressedReq);
        if (outputString != null) {
            try {
                authnReq = ProtocolFactory.getInstance().createAuthnRequest(outputString);
            } catch (SAML2Exception se) {
                SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest(): cannot construct a AuthnRequest "
                        + "object from the SAMLRequest value:", se);
            }
        }
        return authnReq;
    }

    /**
     * Returns the <code>AuthnRequest</code> from HttpServletRequest
     */
    private static AuthnRequest getAuthnRequest(HttpServletRequest request, boolean isFromECP, String binding) {

        if (isFromECP) {
            try {
                SOAPMessage msg = SOAPCommunicator.getInstance().getSOAPMessage(request);
                Element elem = SOAPCommunicator.getInstance().getSamlpElement(msg, SAML2Constants.AUTHNREQUEST);
                return ProtocolFactory.getInstance().createAuthnRequest(elem);
            } catch (Exception ex) {
                SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest:", ex);
            }
            return null;
        } else {
            String samlRequest = request.getParameter(SAML2Constants.SAML_REQUEST);
            if (samlRequest == null) {
                SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest: SAMLRequest is null");
                return null;
            }
            if (binding.equals(SAML2Constants.HTTP_REDIRECT)) {
                SAML2Utils.debug.message("UtilProxySAMLAuthenticator.getAuthnRequest: saml request = {}", samlRequest);
                return getAuthnRequest(samlRequest);
            } else if (binding.equals(SAML2Constants.HTTP_POST)) {
                ByteArrayInputStream bis = null;
                AuthnRequest authnRequest = null;
                try {
                    byte[] raw = Base64.decode(samlRequest);
                    if (raw != null) {
                        bis = new ByteArrayInputStream(raw);
                        Document doc = XMLUtils.toDOMDocument(bis, SAML2Utils.debug);
                        if (doc != null) {
                            SAML2Utils.debug.message("UtilProxySAMLAuthenticator.getAuthnRequest: decoded SAML2 Authn "
                                    + "Request: {}",
                                    XMLUtils.print(doc.getDocumentElement()));
                            authnRequest = ProtocolFactory.getInstance().createAuthnRequest(doc.getDocumentElement());
                        } else {
                            SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest: Unable to parse "
                                    + "SAMLRequest: " + samlRequest);
                        }
                    }
                } catch (Exception ex) {
                    SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest:", ex);
                    return null;
                } finally {
                    IOUtils.closeIfNotNull(bis);
                }
                return authnRequest;
            }
            return null;
        }
    }

    private static StringBuilder getAppliRootUrl(HttpServletRequest request) {
        return new StringBuilder(request.getScheme()).append("://")
                .append(request.getServerName()).append(":")
                .append(request.getServerPort()).append(request.getContextPath());
    }

    private static String getRelativePath(String absUrl, String appliRootUrl) {
        return absUrl.substring(appliRootUrl.length(), absUrl.length());
    }

    /**
     * Redirect to authenticate service
     * If authentication service and federation code are
     * is the same j2ee container do a forward instead of
     * a redirection
     */
    private static void redirectAuthentication(HttpServletRequest request, HttpServletResponse response,
                                               IDPAuthnContextInfo info, IDPSSOFederateRequest data,
                                               boolean isSessionUpgrade)
            throws SAML2Exception, IOException {

        String classMethod = "UtilProxySAMLAuthenticator.redirectAuthentication: ";
        // get the authentication service url
        String authService = IDPSSOUtil.getAuthenticationServiceURL(data.getRealm(), data.getIdpEntityID(), request);
        StringBuilder appliRootUrl = getAppliRootUrl(request);
        boolean forward;
        StringBuffer newURL;

        // build newUrl to auth service and test if redirect or forward
        if (FSUtils.isSameContainer(request, authService)) {
            forward = true;
            String relativePath = getRelativePath(authService, appliRootUrl.toString());
            // in this case continue to forward to SSORedirect after login
            newURL = new StringBuffer(relativePath).append("&forward=true");
        } else {
            // cannot forward so redirect
            forward = false;
            newURL = new StringBuffer(authService);
        }

        // Pass spEntityID to IdP Auth Module
        if (data.getSpEntityID() != null) {
            if (newURL.indexOf("?") == -1) {
                newURL.append("?");
            } else {
                newURL.append("&");
            }

            newURL.append(SAML2Constants.SPENTITYID).append("=").append(URLEncDec.encode(data.getSpEntityID()));
        }

        Set<String> authnTypeAndValues = info.getAuthnTypeAndValues();
        if (CollectionUtils.isNotEmpty(authnTypeAndValues)) {
            boolean isFirst = true;
            StringBuilder authSB = new StringBuilder();

            for (String authnTypeAndValue : authnTypeAndValues) {
                int index = authnTypeAndValue.indexOf("=");
                if (index != -1) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        authSB.append("&");
                    }
                    authSB.append(authnTypeAndValue.substring(0, index + 1))
                            .append(URLEncDec.encode(authnTypeAndValue.substring(index + 1)));
                }
            }

            if (newURL.indexOf("?") == -1) {
                newURL.append("?");
            } else {
                newURL.append("&");
            }

            newURL.append(authSB.toString());

            SAML2Utils.debug.message("{} authString= {}", classMethod, authSB.toString());
        }

        if (newURL.indexOf("?") == -1) {
            if (isSessionUpgrade) {
                newURL.append("?ForceAuth=true&goto=");
            } else {
                newURL.append("?goto=");
            }

        } else {
            if (isSessionUpgrade) {
                newURL.append("&ForceAuth=true");
            }
            newURL.append("&goto=");
        }

        // compute gotoURL differently in case of forward or in case
        // of redirection, forward needs a relative URI.
        StringBuffer gotoURL;
        if (forward) {
            //gotoURL = new StringBuffer(getRelativePath(request.getRequestURI(), request.getContextPath()));
        	gotoURL = new StringBuffer(request.getRequestURI());
        } else {
            String rpUrl = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(data.getRealm(),
                    data.getIdpEntityID(), SAML2Constants.RP_URL);
            if (StringUtils.isNotEmpty(rpUrl)) {
                gotoURL = new StringBuffer(rpUrl);
                gotoURL.append(getRelativePath(request.getRequestURI(), request.getContextPath()));
            } else {
                gotoURL = request.getRequestURL();
            }
        }

        //adding these extra parameters will ensure that we can send back SAML error response to the SP even when the
        //originally received AuthnRequest gets lost.
        gotoURL.append("?ReqID=").append(data.getAuthnRequest().getID()).append('&')
                .append(INDEX).append('=').append(data.getAuthnRequest().getAssertionConsumerServiceIndex()).append('&')
                .append(ACS_URL).append('=').append(URLEncDec.encode(data.getAuthnRequest().getAssertionConsumerServiceURL())).append('&')
                .append(SP_ENTITY_ID).append('=').append(URLEncDec.encode(data.getAuthnRequest().getIssuer().getValue())).append('&')
                .append(BINDING).append('=').append(URLEncDec.encode(data.getAuthnRequest().getProtocolBinding()));

        newURL.append(URLEncDec.encode(gotoURL.toString()));

        SAML2Utils.debug.message("{} New URL for authentication: {}", classMethod, newURL.toString());

        // do forward if we are in the same container ,
        // else redirection
        if (forward) {
            newURL.append('&').append(SystemPropertiesManager.get(Constants.AM_AUTH_COOKIE_NAME, "AMAuthCookie"));
            newURL.append('=');

            SAML2Utils.debug.message("{} Forward to {}", classMethod, newURL.toString());
            try {
                request.setAttribute(Constants.FORWARD_PARAM, Constants.FORWARD_YES_VALUE);
                request.getRequestDispatcher(newURL.toString()).forward(request, response);
            } catch (ServletException se) {
                SAML2Utils.debug.error("{} Exception Bad Forward URL: {}", classMethod, newURL.toString());
            }
        } else {
            response.sendRedirect(newURL.toString());
        }
    }

    /**
     * clean up the cache created for session upgrade.
     */
    private static void cleanUpCache(String reqID) {
        IDPCache.oldIDPSessionCache.remove(reqID);
        IDPCache.authnRequestCache.remove(reqID);
        IDPCache.idpAuthnContextCache.remove(reqID);
        IDPCache.isSessionUpgradeCache.remove(reqID);
    }
}
