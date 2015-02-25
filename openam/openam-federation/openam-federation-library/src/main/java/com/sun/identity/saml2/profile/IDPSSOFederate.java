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
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.common.QuerySignatureUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.plugins.IDPECPSessionMapper;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.forgerock.openam.utils.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class handles the federation and/or single sign on request
 * from a service provider. It processes the <code>AuthnRequest</code>
 * sent by the service provider and generates a proper 
 * <code>Response</code> that contains an <code>Assertion</code>.
 * It sends back a <code>Response</code> containing error status if
 * something is wrong during the request processing.
 */

public class IDPSSOFederate {

    private static final String INDEX = "index";
    private static final String ACS_URL = "acsURL";
    private static final String SP_ENTITY_ID = "spEntityID";
    private static final String BINDING = "binding";
    private static final String REQ_ID = "ReqID";

    private static FedMonAgent agent;
    private static FedMonSAML2Svc saml2Svc;
  
    static {
        agent = MonitorManager.getAgent();
        saml2Svc = MonitorManager.getSAML2Svc();
    }

    private IDPSSOFederate() {
    }
 
    /**
     * This method processes the <code>AuthnRequest</code> coming 
     * from a service provider via HTTP Redirect.
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param out the print writer for writing out presentation
     */
    public static void doSSOFederate(HttpServletRequest request,
                                     HttpServletResponse response,
                                     PrintWriter out,
                                     String reqBinding) {
        doSSOFederate(request, response, out, false, reqBinding);
    }
    /**
     * This method processes the <code>AuthnRequest</code> coming 
     * from a service provider via HTTP Redirect.
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param out the print writer for writing out presentation
     * @param isFromECP true if the request comes from ECP
     *
     */
    public static void doSSOFederate(HttpServletRequest request,
        HttpServletResponse response, PrintWriter out, boolean isFromECP, String reqBinding) {

        String classMethod = "IDPSSOFederate.doSSOFederate: ";

        if (FSUtils.needSetLBCookieAndRedirect(request, response, true)) {
            return;
        }
        String preferredIDP = null;
        Map paramsMap = new HashMap(); 
        //IDP Proxy with introduction cookie case. 
        //After reading the introduction cookie, it redirects to here. 
        String requestID = request.getParameter("requestID");
        Object session = null;
        SPSSODescriptorElement spSSODescriptor = null;
        try { 
            if (requestID != null) {
                //get the preferred idp
                preferredIDP = SAML2Utils.getPreferredIDP(request);
                paramsMap = (Map)SPCache.reqParamHash.get(requestID);
                if (preferredIDP != null) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod +
                            "IDP to be proxied " +  preferredIDP);              
                    }
                    try {
                        IDPProxyUtil.sendProxyAuthnRequest(
                            (AuthnRequest) paramsMap.get("authnReq"),
                            preferredIDP,
                            (SPSSODescriptorElement) paramsMap.get(
                            "spSSODescriptor"),
                            (String) paramsMap.get("idpEntityID"),
                            request,
                            response,
                            (String) paramsMap.get("realm"),
                            (String) paramsMap.get("relayState"),
                            (String) paramsMap.get("binding"));  
                            SPCache.reqParamHash.remove(requestID);
                            return;     
                    } catch (SAML2Exception re) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod +
                                "Redirecting for the proxy handling error:"
                                 + re.getMessage());
                        }
                        sendError(request, response, 
                            SAML2Constants.SERVER_FAULT,
                            "UnableToRedirectToPreferredIDP",
                            re.getMessage(), isFromECP, null);
                        return;
                   }
               } 
            } // end of IDP Proxy case 
 
            String idpMetaAlias = request.getParameter(
                SAML2MetaManager.NAME_META_ALIAS_IN_URI);
            if ((idpMetaAlias == null) || (idpMetaAlias.trim().length() == 0)) {
                idpMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
                    request.getRequestURI());
            }
            if ((idpMetaAlias == null) || (idpMetaAlias.trim().length() == 0)) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                        "unable to get IDP meta alias from request.");
                }
                sendError(request, response, SAML2Constants.CLIENT_FAULT,
                    "IDPMetaAliasNotFound", null, isFromECP, null);
                return;
            }
      
            // retrieve IDP entity id from meta alias
            String idpEntityID = null;
            String realm = null;
            try {
                if (IDPSSOUtil.metaManager == null) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to get meta manager.");
                    sendError(request, response, SAML2Constants.SERVER_FAULT,
                        "errorMetaManager", null, isFromECP, null);
                    return;
                }
                idpEntityID = IDPSSOUtil.metaManager.getEntityByMetaAlias(
                    idpMetaAlias);
                if ((idpEntityID == null) ||
                    (idpEntityID.trim().length() == 0)) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to get IDP Entity ID from meta.");
                    String[] data = { idpEntityID };
                    LogUtil.error(Level.INFO, LogUtil.INVALID_IDP, data, null);
                    sendError(request, response, SAML2Constants.CLIENT_FAULT,
                        "nullIDPEntityID", null, isFromECP, null);
                    return;
                }
                realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
                if (isFromECP) {
                    reqBinding = SAML2Constants.SOAP;
                }
                boolean profileEnabled = 
                    SAML2Utils.isIDPProfileBindingSupported(
                        realm, idpEntityID,
                        SAML2Constants.SSO_SERVICE, reqBinding);
                if (!profileEnabled) {
                    SAML2Utils.debug.error(classMethod +
                        "SSO binding:" + reqBinding + " is not enabled for " +
                        idpEntityID);
                    String[] data = { idpEntityID, reqBinding };
                    LogUtil.error(
                        Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
                    sendError(request, response, SAML2Constants.CLIENT_FAULT,
                        "unsupportedBinding", null, isFromECP, null);
                    return;
                }
            } catch (SAML2MetaException sme) {
                SAML2Utils.debug.error(classMethod +
                    "Unable to get IDP Entity ID from meta.");
                String[] data = { idpMetaAlias };
                LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data,
                    null);
                sendError(request, response, SAML2Constants.SERVER_FAULT,
                    "nullIDPEntityID", sme.getMessage(), isFromECP, null);
                return;
            }

            // Get the IDP adapter
            SAML2IdentityProviderAdapter idpAdapter = null;
            if (realm != null && idpEntityID != null) {
                try {
                    idpAdapter = IDPSSOUtil.getIDPAdapterClass(realm, idpEntityID);
                } catch (SAML2Exception se2) {
                    SAML2Utils.debug.error(classMethod + " There was a problem instantiating the IDP Adapter: ", se2);
                }
            } else {
                SAML2Utils.debug.error(classMethod + " Unable to find IDP Adapter, no realm/entity ID");
            }

            // get the request id query parameter from the request. If this
            // is the first visit then the request id is not set; if it is 
            // coming back from a successful authentication, then request 
            // id should be there.
            String reqID = request.getParameter(REQ_ID);
            if ((reqID != null) && (reqID.trim().length() == 0)) { 
                reqID = null;
            }

            AuthnRequest authnReq = null;
            String relayState = null;
            if (reqID == null) {  
                // There is no reqID, this is the first time that we pass here.
                String binding = SAML2Constants.HTTP_REDIRECT;
                if (request.getMethod().equals("POST")) {
                    binding = SAML2Constants.HTTP_POST;
                }

                authnReq = getAuthnRequest(request, isFromECP, binding);
                if (authnReq == null) {
                    sendError(request, response, SAML2Constants.CLIENT_FAULT,
                        "InvalidSAMLRequest", null, isFromECP, idpAdapter);
                    return;
                }

                String spEntityID = authnReq.getIssuer().getValue();
                try {
                    String authnRequestStr = authnReq.toXMLString();
                    String[] logdata = { spEntityID, idpMetaAlias,
                        authnRequestStr };
                    String logId = isFromECP ?
                        LogUtil.RECEIVED_AUTHN_REQUEST_ECP :
                        LogUtil.RECEIVED_AUTHN_REQUEST;
                    LogUtil.access(Level.INFO, logId, logdata, null);
                } catch (SAML2Exception saml2ex) {
                    SAML2Utils.debug.error(classMethod, saml2ex);
                    sendError(request, response, SAML2Constants.CLIENT_FAULT,
                        "InvalidSAMLRequest", saml2ex.getMessage(), isFromECP, idpAdapter);
                    return;
                }

                if (!SAML2Utils.isSourceSiteValid(
                    authnReq.getIssuer(), realm, idpEntityID)) {
                    if (SAML2Utils.debug.warningEnabled()) {
                        SAML2Utils.debug.warning(classMethod + 
                            "Issuer in Request is not valid.");
                    }
                    sendError(request, response, SAML2Constants.CLIENT_FAULT,
                        "InvalidSAMLRequest", null, isFromECP, idpAdapter);
                    return;
                }
 
                // verify the signature of the query string if applicable
                IDPSSODescriptorElement idpSSODescriptor = null;
                try {
                    idpSSODescriptor = IDPSSOUtil.metaManager.
                        getIDPSSODescriptor(realm, idpEntityID);
                } catch (SAML2MetaException sme) {
                    SAML2Utils.debug.error(classMethod, sme);
                    idpSSODescriptor = null;
                }
                if (idpSSODescriptor == null) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to get IDP SSO Descriptor from meta.");
                    sendError(request, response, SAML2Constants.SERVER_FAULT,
                        "metaDataError", null, isFromECP, idpAdapter);
                    return;
                } 
                try {
                    spSSODescriptor = IDPSSOUtil.metaManager.
                        getSPSSODescriptor(realm, spEntityID);
                } catch (SAML2MetaException sme) {
                    SAML2Utils.debug.error(classMethod, sme);
                    spSSODescriptor = null;
                }

                if (isFromECP || idpSSODescriptor.isWantAuthnRequestsSigned()
                        || (spSSODescriptor == null ? false : spSSODescriptor.isAuthnRequestsSigned())) {
                    // need to verify the query string containing authnRequest
                    if ((spEntityID == null) || 
                        (spEntityID.trim().length() == 0)) {
                        sendError(request, response, 
                            SAML2Constants.CLIENT_FAULT,
                            "InvalidSAMLRequest", null, isFromECP, idpAdapter);
                        return;
                    }
                
                    if (spSSODescriptor == null) {
                        SAML2Utils.debug.error(classMethod +
                            "Unable to get SP SSO Descriptor from meta.");
                        sendError(request, response, 
                            SAML2Constants.SERVER_FAULT,
                            "metaDataError", null, isFromECP, idpAdapter);
                        return;
                    }
                    X509Certificate spCert = KeyUtil.getVerificationCert(
                        spSSODescriptor, spEntityID, SAML2Constants.SP_ROLE);

                    try {
                        boolean isSignatureOK = false;
                        if (isFromECP) {
                            isSignatureOK = authnReq.isSignatureValid(spCert);
                        } else {
                            String method  = request.getMethod();
                            if (method.equals("POST")) {
                                isSignatureOK = authnReq.isSignatureValid(
                                    spCert);
                            } else {
                                String queryString = request.getQueryString();
                                isSignatureOK = QuerySignatureUtil.verify(
                                    queryString, spCert);
                            }
                        }
                        if (!isSignatureOK) {
                            SAML2Utils.debug.error(classMethod +
                                "authn request verification failed.");
                            sendError(request, response, 
                                SAML2Constants.CLIENT_FAULT,
                                "invalidSignInRequest", null, isFromECP, idpAdapter);
                            return;
                        }

                        // In ECP profile, sp doesn't know idp.
                        if (!isFromECP) {
                            // verify Destination
                            List ssoServiceList =
                                idpSSODescriptor.getSingleSignOnService();
                            String ssoURL =
                                SPSSOFederate.getSSOURL(ssoServiceList,
                                binding);
                            if (!SAML2Utils.verifyDestination(
                                authnReq.getDestination(), ssoURL)) {
                                SAML2Utils.debug.error(classMethod + "authn " +
                                    "request destination verification failed.");
                                sendError(request, response, 
                                    SAML2Constants.CLIENT_FAULT,
                                    "invalidDestination", null, isFromECP, idpAdapter);
                                return;
                            }
                        }
                    } catch (SAML2Exception se) {
                        SAML2Utils.debug.error(classMethod +
                            "authn request verification failed.", se);
                        sendError(request, response, 
                            SAML2Constants.CLIENT_FAULT,
                            "invalidSignInRequest", null, isFromECP, idpAdapter);
                        return;
                    } 
                
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod + "authn " +
                            "request signature verification is successful.");
                    }
                }

                reqID = authnReq.getID();
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                        "request id=" + reqID);
                }
                if (reqID == null) {
                    SAML2Utils.debug.error(classMethod + "Request id is null");
                    sendError(request, response, SAML2Constants.CLIENT_FAULT,
                        "InvalidSAMLRequestID", null, isFromECP, idpAdapter);
                    return;
                }

                if (isFromECP) {
                    try {
                        IDPECPSessionMapper idpECPSessonMapper = 
                            IDPSSOUtil.getIDPECPSessionMapper(realm,
                            idpEntityID);
                        session = idpECPSessonMapper.getSession(request,
                            response);
                    } catch (SAML2Exception se) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod +
                                "Unable to retrieve user session.");
                        }
                    }
                } else {
                    // get the user sso session from the request
                    try {
                        session = SessionManager.getProvider().getSession(
                            request);
                    } catch (SessionException se) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod +
                                "Unable to retrieve user session.");
                        }
                        session = null;
                    }
                }

                // preSingleSignOn adapter hook
                // NB: This method is not called in IDPSSOUtil.doSSOFederate(...) so proxy requests or idp init sso
                // will not trigger this adapter call
                try {
                    if (idpAdapter != null) {
                        SAML2Utils.debug.message("Invoking the IDP Adapter preSingleSignOn hook");
                        // If the preSingleSignOnProcess returns true we end here
                        if (idpAdapter.preSingleSignOn(idpEntityID, realm, request, response, authnReq, reqID)) {
                            return;
                        }  // else we continue with the logic. Beware of loops
                    }
                } catch (SAML2Exception se2) {
                    SAML2Utils.debug.error("Error invoking the IDP Adapter", se2);
                }
                // End of adapter invocation

                IDPAuthnContextMapper idpAuthnContextMapper = null;
                try {
                    idpAuthnContextMapper =
                        IDPSSOUtil.getIDPAuthnContextMapper(realm, idpEntityID);
                } catch (SAML2Exception sme) {
                    SAML2Utils.debug.error(classMethod, sme);
                }
                if (idpAuthnContextMapper == null) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to get IDPAuthnContextMapper from meta.");
                    sendError(request, response, SAML2Constants.SERVER_FAULT,
                        "metaDataError", null, isFromECP, idpAdapter);
                    return;
                } 

                IDPAuthnContextInfo idpAuthnContextInfo = null;
                try {
                    idpAuthnContextInfo =
                        idpAuthnContextMapper.getIDPAuthnContextInfo(authnReq,
                        idpEntityID, realm);
                } catch (SAML2Exception sme) {
                    SAML2Utils.debug.error(classMethod, sme);
                }

                if (idpAuthnContextInfo == null) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod + "Unable to " +
                            "find valid AuthnContext. Sending error Response.");
                    }
                    try {
                        Response res = SAML2Utils.getErrorResponse(authnReq, 
                            SAML2Constants.REQUESTER,
                            SAML2Constants.NO_AUTHN_CONTEXT, null, idpEntityID);
                        StringBuffer returnedBinding = new StringBuffer();
                        String acsURL = IDPSSOUtil.getACSurl(spEntityID, realm,
                            authnReq, request, returnedBinding);
                        String acsBinding = returnedBinding.toString();
                        IDPSSOUtil.sendResponse(request, response, acsBinding,
                            spEntityID, idpEntityID, idpMetaAlias, realm, relayState,
                            acsURL, res, session);
                    } catch (SAML2Exception sme) {
                        SAML2Utils.debug.error(classMethod, sme);
                        sendError(request, response, 
                            SAML2Constants.SERVER_FAULT,
                            "metaDataError", null, isFromECP, idpAdapter);
                    }
                    return;
                }

                // get the relay state query parameter from the request
                relayState = request.getParameter(SAML2Constants.RELAY_STATE);
                AuthnContext matchingAuthnContext = idpAuthnContextInfo.getAuthnContext();
    
                if (session == null) {
                    // the user has not logged in yet, redirect to auth
    
                    // TODO: need to verify the signature of the AuthnRequest

                    // save the AuthnRequest in the IDPCache so that it can be
                    // retrieved later when the user successfully authenticates
                    synchronized (IDPCache.authnRequestCache) { 
                        IDPCache.authnRequestCache.put(reqID,
                            new CacheObject(authnReq));
                    }
    
                    // save the AuthnContext in the IDPCache so that it can be
                    // retrieved later when the user successfully authenticates
                    synchronized (IDPCache.idpAuthnContextCache) { 
                        IDPCache.idpAuthnContextCache.put(reqID,
                            new CacheObject(matchingAuthnContext));
                    }
                    
                    // save the relay state in the IDPCache so that it can be
                    // retrieved later when the user successfully authenticates
                    if (relayState != null && relayState.trim().length() != 0) {
                        IDPCache.relayStateCache.put(reqID, relayState);
                    }
     
                    //IDP Proxy: Initiate proxying
                    try {
                        boolean isProxy = IDPProxyUtil.isIDPProxyEnabled(
                            authnReq, realm);
                        if (isProxy) {    
                            preferredIDP = IDPProxyUtil.getPreferredIDP(
                                authnReq,idpEntityID, realm, request, 
                                response);
                            if (preferredIDP != null) {
                                if ((SPCache.reqParamHash != null) &&
                                   (!(SPCache.reqParamHash.containsKey(preferredIDP)))) {
                                   // IDP Proxy with configured proxy list 
                                   if (SAML2Utils.debug.messageEnabled()) {
                                       SAML2Utils.debug.message(classMethod +
                                           "IDP to be proxied" +  preferredIDP);
                                   } 
                                   IDPProxyUtil.sendProxyAuthnRequest(
                                       authnReq, preferredIDP, spSSODescriptor,
                                       idpEntityID, request, response, realm,
                                       relayState, binding);
                                   return;
                                } else { 
                                     // IDP proxy with introduction cookie  
                                     paramsMap = (Map) 
                                         SPCache.reqParamHash.get(preferredIDP);
                                     paramsMap.put("authnReq", authnReq);
                                     paramsMap.put("spSSODescriptor",
                                         spSSODescriptor); 
                                     paramsMap.put("idpEntityID", idpEntityID); 
                                     paramsMap.put("realm", realm); 
                                     paramsMap.put("relayState", relayState); 
                                     paramsMap.put("binding", binding);   
                                     SPCache.reqParamHash.put(preferredIDP,
                                         paramsMap);
                                     return;    
                               }
                           }              
                       }  
                       //else continue for the local authentication.    
                    } catch (SAML2Exception re) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod +
                                "Redirecting for the proxy handling error: "
                                + re.getMessage());
                        }
                        sendError(request, response, 
                            SAML2Constants.SERVER_FAULT,
                            "UnableToRedirectToPreferredIDP", re.getMessage(),
                            isFromECP, idpAdapter);
                    }

                    // preAuthentication adapter hook
                    try {
                        if (idpAdapter != null) {
                            SAML2Utils.debug.message("Invoking the IDP Adapter preAuthentication hook");
                            // If preAuthentication returns true we end here
                            if (idpAdapter.preAuthentication(idpEntityID, realm, request, response, authnReq, null,
                                    reqID, relayState)) {
                                return;
                            }  // else continue - beware of loops
                        }
                    } catch (SAML2Exception se2) {
                        SAML2Utils.debug.error("Error invoking the IDP Adapter", se2);
                    }
                    // End of adapter invocation

                    // redirect to the authentication service
                    try {
                        if (!Boolean.TRUE.equals(authnReq.isPassive())) {
                            redirectAuthentication(request, response, authnReq,
                                    idpAuthnContextInfo, realm, idpEntityID, spEntityID,
                                    false);
                        } else {
                            try {
                                IDPSSOUtil.sendNoPassiveResponse(request, response, idpMetaAlias, idpEntityID,
                                        realm, authnReq, relayState, spEntityID);
                            } catch (SAML2Exception sme) {
                                SAML2Utils.debug.error(classMethod, sme);
                                sendError(request, response,
                                        SAML2Constants.SERVER_FAULT,
                                        "metaDataError", null, isFromECP, idpAdapter);
                            }
                        }
                    } catch (IOException ioe) {
                        SAML2Utils.debug.error(classMethod +
                            "Unable to redirect to authentication.", ioe);
                        sendError(request, response, 
                            SAML2Constants.SERVER_FAULT,
                            "UnableToRedirectToAuth", ioe.getMessage(),
                            isFromECP, idpAdapter);
                    } catch (SAML2Exception se) {
                        SAML2Utils.debug.error(classMethod +
                            "Unable to redirect to authentication.", se);
                        sendError(request, response, 
                            SAML2Constants.SERVER_FAULT,
                            "UnableToRedirectToAuth", se.getMessage(),
                            isFromECP, idpAdapter);
                    } 
                    return;
                } else {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod + 
                            "There is an existing session");
                    }

                    // Let's verify that the realm is the same for the user and the IdP
                    boolean isValidSessionInRealm = isValidSessionInRealm(
                            realm, session);
                    IDPSession oldIDPSession = null;
                    String sessionIndex = IDPSSOUtil.getSessionIndex(session);
                    boolean sessionUpgrade = false;
                    if (isValidSessionInRealm) {
                        sessionUpgrade = isSessionUpgrade(idpAuthnContextInfo, 
                                session);
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod
                                    + "IDP Session Upgrade is :" + sessionUpgrade);
                        }
                    }
                    if (sessionUpgrade || !isValidSessionInRealm ||
                            ((Boolean.TRUE.equals(authnReq.isForceAuthn())) &&
                            (!Boolean.TRUE.equals(authnReq.isPassive()))) ) {

                        // If there was no previous SAML2 session, there will be no
                        // sessionIndex
                        if (sessionIndex != null && sessionIndex.length() != 0) {
                            // Save the original IDP Session
                            oldIDPSession = (IDPSession) IDPCache.
                                    idpSessionsByIndices.get(sessionIndex);
                            if (oldIDPSession != null) {
                               IDPCache.oldIDPSessionCache.put(reqID, oldIDPSession);
                            } else {
                                SAML2Utils.debug.error(classMethod + "The old SAML2 session "
                                        + " was not found in the idp session by indices cache");
                            }
                        }

                        // Save the new requestId and AuthnRequest
                        IDPCache.authnRequestCache.put(reqID, 
                            new CacheObject(authnReq));
                        // Save the new requestId and AuthnContext
                        IDPCache.idpAuthnContextCache.put(reqID, 
                            new CacheObject(matchingAuthnContext));
                        // save if the request was an Session Upgrade case.
                        IDPCache.isSessionUpgradeCache.add(reqID);

                        // save the relay state in the IDPCache so that it can
                        // be retrieved later when the user successfully
                        // authenticates
                        if ((relayState != null) &&
                            (relayState.trim().length() != 0)) {
                            IDPCache.relayStateCache.put(reqID, relayState);
                        }

                        //IDP Proxy: Initiate proxying when session upgrade is requested
                        // Session upgrade could be requested by asking a greater AuthnContext
                        if (isValidSessionInRealm) {
                            try {
                                boolean isProxy = IDPProxyUtil.isIDPProxyEnabled(
                                        authnReq, realm);
                                if (isProxy) {
                                    preferredIDP = IDPProxyUtil.getPreferredIDP(
                                            authnReq, idpEntityID, realm, request,
                                            response);
                                    if (preferredIDP != null) {
                                        if ((SPCache.reqParamHash != null)
                                                && (!(SPCache.reqParamHash.containsKey(preferredIDP)))) {
                                            // IDP Proxy with configured proxy list
                                            if (SAML2Utils.debug.messageEnabled()) {
                                                SAML2Utils.debug.message(classMethod
                                                        + "IDP to be proxied" + preferredIDP);
                                            }
                                            IDPProxyUtil.sendProxyAuthnRequest(
                                                    authnReq, preferredIDP, spSSODescriptor,
                                                    idpEntityID, request, response, realm,
                                                    relayState, binding);
                                            return;
                                        } else {
                                            // IDP proxy with introduction cookie
                                            paramsMap = (Map) SPCache.reqParamHash.get(preferredIDP);
                                            paramsMap.put("authnReq", authnReq);
                                            paramsMap.put("spSSODescriptor",
                                                    spSSODescriptor);
                                            paramsMap.put("idpEntityID", idpEntityID);
                                            paramsMap.put("realm", realm);
                                            paramsMap.put("relayState", relayState);
                                            paramsMap.put("binding", binding);
                                            SPCache.reqParamHash.put(preferredIDP,
                                                    paramsMap);
                                            return;
                                        }
                                    }
                                }
                                //else continue for the local authentication.
                            } catch (SAML2Exception re) {
                                if (SAML2Utils.debug.messageEnabled()) {
                                    SAML2Utils.debug.message(classMethod
                                            + "Redirecting for the proxy handling error: "
                                            + re.getMessage());
                                }
                                sendError(request, response,
                                        SAML2Constants.SERVER_FAULT,
                                        "UnableToRedirectToPreferredIDP", re.getMessage(),
                                        isFromECP, idpAdapter);
                            }
                            // End of IDP Proxy: Initiate proxying when session upgrade is requested

                        }

                        // Invoke the IDP Adapter before redirecting to authn
                        try {
                            SAML2Utils.debug.message("Invoking IDP Adapter preAuthentication hook");
                            if (idpAdapter != null) {
                                // If the preAuthentication method returns true we end here
                                if (idpAdapter.preAuthentication(idpEntityID, realm, request, response,
                                        authnReq, session, reqID, relayState)) {
                                    return;
                                }  // else continue - beware of loops
                            }
                        } catch (SAML2Exception se2) {
                            SAML2Utils.debug.error("Error invoking the IDP Adapter", se2);
                        }
                        // End of block for IDP Adapter invocation

                        try {
                            if (!Boolean.TRUE.equals(authnReq.isPassive())) {
                                redirectAuthentication(request, response, authnReq,
                                        idpAuthnContextInfo, realm, idpEntityID,
                                        spEntityID, true);
                                return;
                            } else {
                                try {
                                    IDPSSOUtil.sendNoPassiveResponse(request, response, idpMetaAlias,
                                            idpEntityID, realm, authnReq, relayState, spEntityID);
                                } catch (SAML2Exception sme) {
                                    SAML2Utils.debug.error(classMethod, sme);
                                    sendError(request, response,
                                            SAML2Constants.SERVER_FAULT,
                                            "metaDataError", null, isFromECP, idpAdapter);
                                }
                            }
                        } catch (IOException ioe) {
                            SAML2Utils.debug.error(classMethod +
                                 "Unable to redirect to authentication.", ioe);
                            sessionUpgrade = false;
                            cleanUpCache(reqID);
                            sendError(request, response, 
                                SAML2Constants.SERVER_FAULT,
                                "UnableToRedirectToAuth", ioe.getMessage(),
                                isFromECP, idpAdapter);
                        } catch (SAML2Exception se) {
                            SAML2Utils.debug.error(classMethod +
                                "Unable to redirect to authentication.", se);
                            sessionUpgrade = false;
                            cleanUpCache(reqID);
                            sendError(request, response, 
                                SAML2Constants.SERVER_FAULT,
                                "UnableToRedirectToAuth", se.getMessage(),
                                isFromECP, idpAdapter);
                        }
                    } 
                    // comes here if either no session upgrade or error
                    // redirecting to authentication url.
                    // generate assertion response
                    if (!sessionUpgrade && isValidSessionInRealm) {
                        // IDP Adapter invocation, to be sure that we can execute the logic
                        // even if there is a new request with the same session
                        
                        // save the AuthnRequest in the IDPCache so that it can be
                        // retrieved later when the user successfully authenticates                        
                        synchronized (IDPCache.authnRequestCache) {
                            IDPCache.authnRequestCache.put(reqID,
                                    new CacheObject(authnReq));
                        }

                        // save the AuthnContext in the IDPCache so that it can be
                        // retrieved later when the user successfully authenticates
                        synchronized (IDPCache.idpAuthnContextCache) {
                            IDPCache.idpAuthnContextCache.put(reqID,
                                    new CacheObject(matchingAuthnContext));
                        }

                        // save the relay state in the IDPCache so that it can be
                        // retrieved later when the user successfully authenticates
                        if (relayState != null && relayState.trim().length() != 0) {
                            IDPCache.relayStateCache.put(reqID, relayState);
                        }

                        try {
                            SAML2Utils.debug.message("Invoking the IDP Adapter preSendResponse");
                            if (idpAdapter != null) {
                                // If the preSendResponse returns true we end here
                                if (idpAdapter.preSendResponse(authnReq, idpEntityID,
                                        realm, request, response, session, reqID, relayState)) {
                                    return;
                                }  // else continue - beware of loops
                            }
                        } catch (SAML2Exception se2) {
                            SAML2Utils.debug.error("Error invoking the IDP Adapter", se2);
                        }
                        // preSendResponse IDP adapter invocation ended

                        // call multi-federation protocol to set the protocol                       
                        MultiProtocolUtils.addFederationProtocol(session, 
                             SingleLogoutManager.SAML2);
                        NameIDPolicy policy = authnReq.getNameIDPolicy();
                        String nameIDFormat =
                            (policy == null) ? null : policy.getFormat();
                        try {
                            IDPSSOUtil.sendResponseToACS(request, response, out,
                                session, authnReq, spEntityID, idpEntityID,
                                idpMetaAlias, realm, nameIDFormat, relayState,
                                matchingAuthnContext);
                        } catch (SAML2Exception se) {
                            SAML2Utils.debug.error(classMethod +
                                "Unable to do sso or federation.", se);
                            sendError(request, response, 
                                SAML2Constants.SERVER_FAULT,
                                "UnableToDOSSOOrFederation", se.getMessage(),
                                isFromECP, idpAdapter);
                        }
                    }
                }
            } else {
                // the second visit, the user has already authenticated
                // retrieve the cache authn request and relay state

                // We need the session to pass it to the IDP Adapter preSendResponse
                SessionProvider sessionProvider = SessionManager.getProvider();
                try {
                    session = sessionProvider.getSession(request);
                } catch (SessionException se) {
                    SAML2Utils.debug.error("An error occurred while retrieving the session: " + se.getMessage());
                    session = null;
                }

                // Get the cached Authentication Request and Relay State before
                // invoking the IDP Adapter

                CacheObject cacheObj;
                synchronized (IDPCache.authnRequestCache) {
                    cacheObj = (CacheObject) IDPCache.authnRequestCache.get(reqID);
                }
                if (cacheObj != null) {
                    authnReq = (AuthnRequest)cacheObj.getObject();
                }

                relayState = (String) IDPCache.relayStateCache.get(reqID);
                
                // Let's verify if the session belongs to the proper realm
                boolean isValidSessionInRealm = session != null && isValidSessionInRealm(realm, session);

                // There should be a session on the second pass. If this is not the case then provide an error message
                // If there is a session then it must belong to the proper realm
                if (!isValidSessionInRealm) {
                    if (authnReq != null && Boolean.TRUE.equals(authnReq.isPassive())) {
                        // Send an appropriate response to the passive request
                        String spEntityID = authnReq.getIssuer().getValue();
                        try {
                            IDPSSOUtil.sendNoPassiveResponse(request, response, idpMetaAlias, idpEntityID,
                                    realm, authnReq, relayState, spEntityID);
                        } catch (SAML2Exception sme) {
                            SAML2Utils.debug.error(classMethod, sme);
                            sendError(request, response, SAML2Constants.SERVER_FAULT, "metaDataError", null, isFromECP,
                                    idpAdapter);
                        }
                    } else {
                        // No attempt to authenticate now, since it is assumed that that has already been tried
                        String ipAddress = request.getRemoteAddr();
                        String authnReqString = "";
                        try {
                            authnReqString = authnReq == null ? "" : authnReq.toXMLString();
                        } catch (SAML2Exception ex) {
                            SAML2Utils.debug.error(classMethod + "Could not obtain the AuthnReq to be logged");
                        }

                        if (session == null) {
                            String[] data = {"null", realm, idpEntityID, ipAddress, authnReqString};
                            SAML2Utils.debug.error(classMethod + "The IdP has not been able to create a session");
                            LogUtil.error(Level.INFO, LogUtil.SSO_NOT_FOUND, data, session, null);
                        } else if (!isValidSessionInRealm) {
                            String sessionRealm = sessionProvider.getProperty(session, SAML2Constants.ORGANIZATION)[0];
                            String[] data = {sessionRealm, realm, idpEntityID, ipAddress, authnReqString};
                            SAML2Utils.debug.error(classMethod + "The realm of the session"
                                    + " does not correspond to that of the IdP");
                            LogUtil.error(Level.INFO, LogUtil.INVALID_REALM_FOR_SESSION, data, session, null);
                        }

                        sendError(request, response, SAML2Constants.CLIENT_FAULT, "UnableToDOSSOOrFederation", null,
                                isFromECP, idpAdapter);
                        return;
                    }
                }
                // Invoke the IDP Adapter after the user has been authenticated
                try {
                        SAML2Utils.debug.message("Invoking the IDP Adapter preSendResponse hook");
                        if (idpAdapter != null) {
                            // Id adapter returns true we end here
                            if (idpAdapter.preSendResponse(authnReq, idpEntityID,
                                    realm, request, response, session, reqID, relayState)) {
                                return;
                            }  // else continue - beware of loops
                        }           
                } catch (SAML2Exception se2) {
                    SAML2Utils.debug.error("Error invoking the IDP Adapter", se2);
                }
                // End of block for IDP Adapter invocation

                synchronized (IDPCache.authnRequestCache) {
                    cacheObj =
                        (CacheObject)IDPCache.authnRequestCache.remove(reqID);
                }
                if (cacheObj != null) {
                    authnReq = (AuthnRequest)cacheObj.getObject();
                }
                AuthnContext matchingAuthnContext = null;
                synchronized (IDPCache.idpAuthnContextCache) {
                    cacheObj = (CacheObject)
                        IDPCache.idpAuthnContextCache.remove(reqID);
                }
                if (cacheObj != null) {
                    matchingAuthnContext = (AuthnContext)cacheObj.getObject();
                }
                
                relayState = (String)IDPCache.relayStateCache.remove(reqID);
                if (authnReq == null) {
                    //handle the case when the authn request is no longer available in the local cache. This could
                    //happen for multiple reasons:
                    //* the SAML response has been already sent back for this request (i.e. browser back button)
                    //* the second visit reached a different OpenAM server, than the first and SAML SFO is disabled
                    //* the cache interval has passed
                    SAML2Utils.debug.error(classMethod + "Unable to get AuthnRequest from cache, sending error"
                            + " response");
                    try {
                        SAML2Utils.debug.message("Invoking IDP adapter preSendFailureResponse hook");
                        try {
                            if (idpAdapter != null) {
                                idpAdapter.preSendFailureResponse(request, response, SAML2Constants.SERVER_FAULT,
                                        "UnableToGetAuthnReq");
                            }
                        } catch (SAML2Exception se2) {
                            SAML2Utils.debug.error("Error invoking the IDP Adapter", se2);
                        }
                        Response res = SAML2Utils.getErrorResponse(null, SAML2Constants.RESPONDER, null, null,
                                idpEntityID);
                        res.setInResponseTo(reqID);
                        StringBuffer returnedBinding = new StringBuffer();
                        String spEntityID = request.getParameter(SP_ENTITY_ID);
                        String acsURL = request.getParameter(ACS_URL);
                        String binding = request.getParameter(BINDING);
                        Integer index;
                        try {
                            index = Integer.valueOf(request.getParameter(INDEX));
                        } catch (NumberFormatException nfe) {
                            index = null;
                        }
                        acsURL = IDPSSOUtil.getACSurl(spEntityID, realm, acsURL, binding, index, request,
                                returnedBinding);
                        String acsBinding = returnedBinding.toString();
                        IDPSSOUtil.sendResponse(request, response, acsBinding, spEntityID, idpEntityID, idpMetaAlias,
                                realm, relayState, acsURL, res, session);
                    } catch (SAML2Exception sme) {
                        SAML2Utils.debug.error(classMethod + "an error occured while sending error response", sme);
                        sendError(request, response, SAML2Constants.SERVER_FAULT, "UnableToGetAuthnReq", null,
                                isFromECP, idpAdapter);
                    }
                    return;
                }
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod + "RequestID=" +
                        reqID);
                }
                boolean isSessionUpgrade = false;

                if (IDPCache.isSessionUpgradeCache != null 
                    && !IDPCache.isSessionUpgradeCache.isEmpty()) {
                    if (IDPCache.isSessionUpgradeCache.contains(reqID)) {
                        isSessionUpgrade =  true;
                    }
                }
                if (isSessionUpgrade) {
                    IDPSession oldSess = 
                        (IDPSession)IDPCache.oldIDPSessionCache.remove(reqID);
                    String sessionIndex = IDPSSOUtil.getSessionIndex(session);
                    if (sessionIndex != null && (sessionIndex.length() != 0 )) { 
                        IDPCache.idpSessionsByIndices.put(sessionIndex,oldSess);

                        if ((agent != null) &&
                            agent.isRunning() &&
                            (saml2Svc != null))
                        {
                            saml2Svc.setIdpSessionCount(
		                (long)IDPCache.idpSessionsByIndices.size());
                        }
                    }
                }
                if (session != null) {
                    // call multi-federation protocol to set the protocol
                    MultiProtocolUtils.addFederationProtocol(session, 
                        SingleLogoutManager.SAML2);
                }
                
                // generate assertion response
                String spEntityID = authnReq.getIssuer().getValue();
                NameIDPolicy policy = authnReq.getNameIDPolicy();
                String nameIDFormat =
                    (policy == null) ? null : policy.getFormat();
                try {
                    IDPSSOUtil.sendResponseToACS(request, response, out, session,
                        authnReq, spEntityID, idpEntityID, idpMetaAlias, realm,
                        nameIDFormat, relayState, matchingAuthnContext);
                } catch (SAML2Exception se) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to do sso or federation.", se);
                    sendError(request, response, SAML2Constants.SERVER_FAULT,
                        "UnableToDOSSOOrFederation", se.getMessage(),
                        isFromECP, idpAdapter);
                }
            }
        } catch (IOException ioe) {
            SAML2Utils.debug.error(classMethod + "I/O error", ioe);
        } catch (SessionException sso) {
            SAML2Utils.debug.error("SSOException : " , sso);
        } catch (SOAPException soapex) {
            SAML2Utils.debug.error("IDPSSOFederate.doSSOFederate:" , soapex);
        }
    }

    private static void sendError(HttpServletRequest request,
        HttpServletResponse response,
        String faultCode, String rbKey, String detail, boolean isFromECP, SAML2IdentityProviderAdapter idpAdapter)
        throws IOException, SOAPException {

        if (isFromECP) {
            SOAPMessage soapFault = SAML2Utils.createSOAPFault(faultCode,
                rbKey, detail);
            if (soapFault != null) {
                //  Need to call saveChanges because we're
                // going to use the MimeHeaders to set HTTP
                // response information. These MimeHeaders
                // are generated as part of the save.
                if (soapFault.saveRequired()) {
                    soapFault.saveChanges();
                }
                response.setStatus(HttpServletResponse.SC_OK);
                SAML2Utils.putHeaders(soapFault.getMimeHeaders(), response);
                // Write out the message on the response stream
                OutputStream os = response.getOutputStream();
                soapFault.writeTo(os);
                os.flush();
                os.close();
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } else {

            // Invoke the IDP Adapter after the user has been authenticated
            try {
                SAML2Utils.debug.message("Invoking IDP adapter preSendFailureResponse hook");
                if (idpAdapter != null) {
                    idpAdapter.preSendFailureResponse(request, response, faultCode, detail);
                }
            } catch (SAML2Exception se2) {
                SAML2Utils.debug.error("Error invoking the IDP Adapter", se2);
            }
            // End of block for IDP Adapter invocation

            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, rbKey,
                SAML2Utils.bundle.getString(rbKey));
            return;
        }
    }

    /**
     *  Returns the <code>AuthnRequest</code> from saml request string
     */
    private static AuthnRequest getAuthnRequest(String compressedReq) {
    
        AuthnRequest authnReq = null;
        String outputString = SAML2Utils.decodeFromRedirect(compressedReq);
        if (outputString != null) {
            try {
                authnReq = (AuthnRequest)ProtocolFactory.getInstance().
                    createAuthnRequest(outputString);
            } catch (SAML2Exception se) {
                SAML2Utils.debug.error(
                "IDPSSOFederate.getAuthnRequest(): cannot construct "
                + "a AuthnRequest object from the SAMLRequest value:", se);
            }
        }
        return authnReq;
    }

    /**
     *  Returns the <code>AuthnRequest</code> from HttpServletRequest
     */
    private static AuthnRequest getAuthnRequest(HttpServletRequest request, boolean isFromECP, String binding) {

        if (isFromECP) {
            MimeHeaders headers = SAML2Utils.getHeaders(request);
            try {
                InputStream is = request.getInputStream();
                SOAPMessage msg = SAML2Utils.mf.createMessage(headers, is);
                Element elem = SAML2Utils.getSamlpElement(msg, 
                    SAML2Constants.AUTHNREQUEST);
                return ProtocolFactory.getInstance().createAuthnRequest(elem);
	    } catch (Exception ex) {
                SAML2Utils.debug.error("IDPSSOFederate.getAuthnRequest:", ex);
            }
            return null;
        } else {
            String samlRequest = request.getParameter(SAML2Constants.SAML_REQUEST);
            if (samlRequest == null) {
                SAML2Utils.debug.error("IDPSSOFederate.getAuthnRequest: SAMLRequest is null");
                return null;
            }
	        if (binding.equals(SAML2Constants.HTTP_REDIRECT)) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("IDPSSOFederate.getAuthnRequest: " +
                        "saml request = " + samlRequest);
                }
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
                            if (SAML2Utils.debug.messageEnabled()) {
                                SAML2Utils.debug.message("IDPSSOFederate.getAuthnRequest: decoded SAML2 Authn Request: "
                                        + XMLUtils.print(doc.getDocumentElement()));
                            }
                            authnRequest = ProtocolFactory.getInstance().createAuthnRequest(doc.getDocumentElement());
                        } else {
                            SAML2Utils.debug.error("IDPSSOFederate.getAuthnRequest: Unable to parse SAMLRequest: " +
                                    samlRequest);
                        }
                    }
                } catch (Exception ex) {
                    SAML2Utils.debug.error("IDPSSOFederate.getAuthnRequest:", ex);
                    return null;
                } finally {
                    IOUtils.closeIfNotNull(bis);
                }
                return authnRequest;
            }
            return null;
        }
    }

    private static StringBuffer getAppliRootUrl(HttpServletRequest request) {
        StringBuffer result = new StringBuffer();
        String scheme = request.getScheme();             // http
        String serverName = request.getServerName();     // hostname.com
        int serverPort = request.getServerPort();        // 80
        String contextPath = request.getContextPath();   // /mywebapp
        result.append(scheme).append("://").append(serverName).append(":").
                append(serverPort);
        result.append(contextPath);
        return result ;
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
    private static void redirectAuthentication(HttpServletRequest request,
        HttpServletResponse response, AuthnRequest authnReq, IDPAuthnContextInfo info,
        String realm, String idpEntityID, String spEntityID,
        boolean isSessionUpgrade) throws SAML2Exception, IOException {

        String classMethod = "IDPSSOFederate.redirectAuthentication: ";
        // get the authentication service url 
        String authService =
                IDPSSOUtil.getAuthenticationServiceURL(
                realm, idpEntityID, request);
        StringBuffer appliRootUrl = getAppliRootUrl(request);
        boolean forward ;
        StringBuffer newURL;
        // build newUrl to auth service and test if redirect or forward
        if(FSUtils.isSameContainer(request,authService)){
            forward = true;
            String relativePath = getRelativePath(authService, appliRootUrl.
                    toString());
            // in this case continue to forward to SSORedirect after login
            newURL = new StringBuffer(relativePath).append("&forward=true");
        } else {
            // cannot forward so redirect
            forward = false ;
            newURL = new StringBuffer(authService);
        }

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
        
        Set authnTypeAndValues = info.getAuthnTypeAndValues();
        if ((authnTypeAndValues != null) 
            && (!authnTypeAndValues.isEmpty())) { 
            Iterator iter = authnTypeAndValues.iterator();
            boolean isFirst = true;
            StringBuilder authSB = new StringBuilder();
            while (iter.hasNext()) {
                String authnValue = (String) iter.next();
                int index = authnValue.indexOf("=");
                if (index != -1) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        authSB.append("&"); 
                    }
                    authSB.append(authnValue.substring(0, index+1));
                    authSB.append(
                        URLEncDec.encode(authnValue.substring(index+1)));
                }
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
        StringBuffer gotoURL ;
        if(forward){
            gotoURL = new StringBuffer(getRelativePath(request.getRequestURI(),
                   request.getContextPath()));
        } else {
            String rpUrl = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(
                    realm, idpEntityID, SAML2Constants.RP_URL);
            if (rpUrl != null && !rpUrl.isEmpty()) {
                gotoURL = new StringBuffer(rpUrl);
                gotoURL.append(getRelativePath(request.getRequestURI(), request.getContextPath()));
            } else {
                gotoURL = request.getRequestURL();
            }
        }
        gotoURL.append("?ReqID=").append(authnReq.getID()).append('&');
        //adding these extra parameters will ensure that we can send back SAML error response to the SP even when the
        //originally received AuthnRequest gets lost.
        gotoURL.append(INDEX).append('=').append(authnReq.getAssertionConsumerServiceIndex()).append('&');
        gotoURL.append(ACS_URL).append('=')
                .append(URLEncDec.encode(authnReq.getAssertionConsumerServiceURL())).append('&');
        gotoURL.append(SP_ENTITY_ID).append('=').append(URLEncDec.encode(authnReq.getIssuer().getValue())).append('&');
        gotoURL.append(BINDING).append('=').append(URLEncDec.encode(authnReq.getProtocolBinding()));
        newURL.append(URLEncDec.encode(gotoURL.toString()));

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod +
                "New URL for authentication: " + newURL.toString());
        }

        // do forward if we are in the same container ,
        // else redirection
        if (forward) {
            newURL.append('&').append(SystemPropertiesManager.get(Constants.AM_AUTH_COOKIE_NAME, "AMAuthCookie"));
            newURL.append('=');
            SAML2Utils.debug.message("forward to " + newURL.toString());
            try {
                request.setAttribute(Constants.FORWARD_PARAM, Constants.FORWARD_YES_VALUE);
                request.getRequestDispatcher(newURL.toString()).
                        forward(request, response);
            } catch (ServletException se) {
                SAML2Utils.debug.error("Exception Bad Forward URL" +
                        newURL.toString());
            }
        } else {
            response.sendRedirect(newURL.toString());
        }
    }

    /**
     * Iterates through the RequestedAuthnContext from the Service Provider and
     * check if user has already authenticated with a sufficient authentication
     * level.
     *
     * If RequestAuthnContext is not found in the authenticated AuthnContext
     * then session upgrade will be done .
     *
     * @param requestAuthnContext the <code>RequestAuthnContext</code> object.
     * @param sessionIndex the Session Index of the active session.
     * @return true if the requester requires to reauthenticate
     */
    private static boolean isSessionUpgrade(
            IDPAuthnContextInfo idpAuthnContextInfo, Object session) {

        String classMethod = "IDPSSOFederate.isSessionUpgrade: ";

        if (session != null) {
            // Get the Authentication Context required
            String authnClasRef = idpAuthnContextInfo.getAuthnContext().
                    getAuthnContextClassRef();
            // Get the AuthN level associated with the Authentication Context
            int authnLevel = idpAuthnContextInfo.getAuthnLevel();

            SAML2Utils.debug.message(classMethod + "Requested AuthnContext: " +
                    "authnClasRef=" + authnClasRef + " authnLevel=" + authnLevel);

            int sessionAuthnLevel = 0;

            try {
                sessionAuthnLevel = Integer.parseInt(
                        SessionManager.getProvider().getProperty(
                        session, SAML2Constants.AUTH_LEVEL)[0]);
                SAML2Utils.debug.message(classMethod +
                        "Current session Authentication Level: " +
                        sessionAuthnLevel);
            } catch (SessionException sex) {
                SAML2Utils.debug.error(classMethod +
                        " Couldn't get the session Auth Level", sex);
            }


            if (authnLevel > sessionAuthnLevel) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

       
    /**
     * Check that the authenticated session belongs to the same realm where the 
     * IDP is defined.
     *
     *
     * @param realm the realm where the IdP is defined
     * @param session The Session object of the authenticated user
     * @return true if the session was initiated in the same realm as realm
     */
    private static boolean isValidSessionInRealm(String realm, Object session) {

        String classMethod = "IDPSSOFederate.isValidSessionInRealm: ";
        boolean isValidSessionInRealm = false;
        try {
            // A user can only be authenticated in one realm
            String sessionRealm = SessionManager.getProvider().
                    getProperty(session, SAML2Constants.ORGANIZATION)[0];
            if (sessionRealm != null && !sessionRealm.isEmpty()) {
                if (realm.equalsIgnoreCase(sessionRealm)) {
                    isValidSessionInRealm = true;
                } else {
                    if (SAML2Utils.debug.warningEnabled()) {
                        SAML2Utils.debug.warning(classMethod
                                + "Invalid realm for the session:" + sessionRealm +
                                ", while the realm of the IdP is:" + realm);
                    }
                }
            }
        } catch (SessionException ex) {
            SAML2Utils.debug.error(classMethod + "Could not retrieve the session"
                    + " information", ex);
        }
        return isValidSessionInRealm;
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
