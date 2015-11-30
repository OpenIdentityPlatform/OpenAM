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
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.protocol.AuthnRequest;
import org.forgerock.openam.saml2.IDPRequestValidator;
import org.forgerock.openam.saml2.IDPSSOFederateRequest;
import org.forgerock.openam.saml2.SAML2ActorFactory;
import org.forgerock.openam.saml2.SAMLAuthenticator;
import org.forgerock.openam.saml2.SAMLAuthenticatorLookup;
import org.forgerock.openam.saml2.UtilProxyCookieRedirector;
import org.forgerock.openam.saml2.audit.SAML2EventLogger;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * This class handles the federation and/or single sign on request
 * from a service provider. It processes the <code>AuthnRequest</code>
 * sent by the service provider and generates a proper
 * <code>Response</code> that contains an <code>Assertion</code>.
 * It sends back a <code>Response</code> containing error status if
 * something is wrong during the request processing.
 */
public class IDPSSOFederate {

    private static final String REQ_ID = "ReqID";

    private final boolean isFromECP;
    private final FederateCookieRedirector cookieRedirector;
    private final SAML2ActorFactory saml2ActorFactory;
    private SAML2EventLogger auditor;

    private IDPSSOFederate(final boolean isFromECP) throws ServerFaultException, ClientFaultException {
        this.isFromECP = isFromECP;
        this.cookieRedirector = new UtilProxyCookieRedirector();
        this.saml2ActorFactory = new SAML2ActorFactory();
    }

    @VisibleForTesting
    IDPSSOFederate(final boolean isFromECP,
                   final FederateCookieRedirector redirector,
                   final SAML2ActorFactory saml2ActorFactory)
            throws ServerFaultException, ClientFaultException {
        this.isFromECP = isFromECP;
        this.cookieRedirector = redirector;
        this.saml2ActorFactory = saml2ActorFactory;
    }

    /**
     * This method processes the <code>AuthnRequest</code> coming
     * from a service provider via HTTP Redirect.
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param out the print writer for writing out presentation
     * @param auditor the auditor for logging SAML2 Events - may be null
     */
    public static void doSSOFederate(HttpServletRequest request,
                                     HttpServletResponse response,
                                     PrintWriter out,
                                     String reqBinding,
                                     SAML2EventLogger auditor) {

        try {
            doSSOFederate(request, response, out, false, reqBinding, auditor);
            auditor.auditAccessSuccess();
        } catch (FederatedSSOException ex) {
            auditor.auditAccessFailure(ex.getFaultCode(), ex.getLocalizedMessage());
            // Invoke the IDP Adapter after the user has been authenticated
            try {
                SAML2Utils.debug.message("Invoking IDP adapter preSendFailureResponse hook");
                final SAML2IdentityProviderAdapter idpAdapter = ex.getIdpAdapter();
                if (idpAdapter != null) {
                    idpAdapter.preSendFailureResponse(request, response, ex.getFaultCode(), ex.getDetail());
                }
            } catch (SAML2Exception se2) {
                SAML2Utils.debug.error("Error invoking the IDP Adapter", se2);
            }

            SAMLUtils.sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessageCode(),
                    SAML2Utils.bundle.getString(ex.getMessageCode()));

        }
    }

    /**
     * This method processes the <code>AuthnRequest</code> coming
     * from a service provider via HTTP Redirect.
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param out the print writer for writing out presentation
     * @param isFromECP true if the request comes from ECP
     * @param auditor the auditor for logging SAML2 Events - may be null
     *
     */
    public static void doSSOFederate(HttpServletRequest request, HttpServletResponse response, PrintWriter out,
                                     boolean isFromECP, String reqBinding, SAML2EventLogger auditor)
            throws FederatedSSOException {
        String classMethod = "IDPSSOFederate.doSSOFederate: ";

        try {
            final IDPSSOFederate idpSsoFederateRequest = new IDPSSOFederate(isFromECP);
            idpSsoFederateRequest.withEventAuditor(auditor);
            idpSsoFederateRequest.process(request, response, out, reqBinding);
        } catch (IOException ioe) {
            SAML2Utils.debug.error(classMethod + "I/O error", ioe);
        } catch (SessionException sso) {
            SAML2Utils.debug.error("SSOException : ", sso);
        }
    }

    private IDPSSOFederate withEventAuditor(SAML2EventLogger auditor) {
        this.auditor = auditor;
        return this;
    }

    /**
     * Having read the requestID, look up the preferred IDP for this request.
     * If matched, send a proxy authentication request.
     * Performs no action if the requestID is null.
     *
     * @param requestID Nullable identifier for the request. May be null.
     * @throws ServerFaultException If we couldn't send the authentication request.
     */
    private boolean idpProxyCase(String requestID, HttpServletRequest request,
                                 HttpServletResponse response) throws ServerFaultException {
        final String classMethod = "IDPSSOFederate.idpProxyCase:";

        final Map paramsMap = (Map) SPCache.reqParamHash.get(requestID);

        if (requestID != null) {
            String preferredIDP = SAML2Utils.getPreferredIDP(request);
            if (preferredIDP != null) {
                SAML2Utils.debug.message("{} IDP to be proxied {}", classMethod, preferredIDP);
                try {
                    IDPProxyUtil.sendProxyAuthnRequest(
                            (AuthnRequest) paramsMap.get("authnReq"),
                            preferredIDP,
                            (SPSSODescriptorElement) paramsMap.get("spSSODescriptor"),
                            (String) paramsMap.get("idpEntityID"),
                            request,
                            response,
                            (String) paramsMap.get("realm"),
                            (String) paramsMap.get("relayState"),
                            (String) paramsMap.get("binding"));
                    SPCache.reqParamHash.remove(requestID);
                    return true;
                } catch (SAML2Exception | IOException e) {
                        SAML2Utils.debug.message(classMethod +
                                "{} Redirecting for the proxy handling error: {}", classMethod, e.getMessage());
                    throw new ServerFaultException("UnableToRedirectToPreferredIDP", e.getMessage());
                }
            }
        }

        return false;
    }

    @VisibleForTesting
    void process(final HttpServletRequest request, final HttpServletResponse response, final PrintWriter out,
                 final String reqBinding) throws FederatedSSOException, IOException, SessionException {
        if (cookieRedirector.needSetLBCookieAndRedirect(request, response, true)) {
            return;
        }

        final IDPRequestValidator validator =
                saml2ActorFactory.getIDPRequestValidator(reqBinding, isFromECP);

        //IDP Proxy with introduction cookie case.
        //After reading the introduction cookie, it redirects to here.
        String requestID = request.getParameter("requestID");

        if (idpProxyCase(requestID, request, response)) {
            return;
        }

        // Fetch a number of properties about the request.
        String idpMetaAlias = validator.getMetaAlias(request);
        String realm = validator.getRealmByMetaAlias(idpMetaAlias);
        String idpEntityID = validator.getIDPEntity(idpMetaAlias, realm);
        SAML2IdentityProviderAdapter idpAdapter = validator.getIDPAdapter(realm, idpEntityID);
        String reqID = request.getParameter(REQ_ID);
        if (null != auditor && StringUtils.isNotEmpty(reqID)) {
            auditor.setRequestId(reqID);
        }
        IDPSSOFederateRequest reqData = new IDPSSOFederateRequest(reqID, realm, idpAdapter, idpMetaAlias, idpEntityID);
        reqData.setEventAuditor(auditor);
        // get the request id query parameter from the request. If this
        // is the first visit then the request id is not set; if it is
        // coming back from a successful authentication, then request
        // id should be there.
        if (StringUtils.isEmpty(reqData.getRequestID())) {
            SAMLAuthenticator samlAuthenticator = saml2ActorFactory.getSAMLAuthenticator(reqData, request, response,
                    out, isFromECP);
            samlAuthenticator.authenticate();
        } else {
            SAMLAuthenticatorLookup samlLookup = saml2ActorFactory.getSAMLAuthenticatorLookup(reqData, request,
                    response, out);
            samlLookup.retrieveAuthenticationFromCache();
        }
    }


}