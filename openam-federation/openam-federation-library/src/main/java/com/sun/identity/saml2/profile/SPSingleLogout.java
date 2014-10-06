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
 * $Id: SPSingleLogout.java,v 1.29 2009/11/24 21:53:28 madan_ranganath Exp $
 *
 * Portions Copyrighted 2013-2014 ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import javax.xml.soap.SOAPMessage;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.plugins.FedletAdapter;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap; 
import java.util.StringTokenizer;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class reads the required data from HttpServletRequest and
 * initiates the <code>LogoutRequest</code> from SP to IDP.
 */

public class SPSingleLogout {

    static SAML2MetaManager sm = null;
    static AssertionFactory af = AssertionFactory.getInstance();
    static Debug debug = SAML2Utils.debug;
    static final Status SUCCESS_STATUS =
            SAML2Utils.generateStatus(SAML2Constants.SUCCESS,
                                SAML2Utils.bundle.getString("requestSuccess"));
    static final Status PARTIAL_LOGOUT_STATUS =
            SAML2Utils.generateStatus(SAML2Constants.RESPONDER,
                                SAML2Utils.bundle.getString("partialLogout"));
    static SessionProvider sessionProvider = null;

    private static FedMonAgent agent;
    private static FedMonSAML2Svc saml2Svc;
    
    static {
        try {
            sm = new SAML2MetaManager();
        } catch (SAML2MetaException sme) {
            debug.error("Error retrieving metadata.", sme);
        }
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            debug.error("Error retrieving session provider.", se);
        }
        agent = MonitorManager.getAgent();
        saml2Svc = MonitorManager.getSAML2Svc();
    }

    /**
     * Parses the request parameters and initiates the Logout
     * Request to be sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param binding binding used for this request.
     * @param paramsMap Map of all other parameters.
     *       Following parameters names with their respective
     *       String values are allowed in this paramsMap.
     *       "RelayState" - the target URL on successful Single Logout
     *       "Destination" - A URI Reference indicating the address to
     *                       which the request has been sent.
     *       "Consent" - Specifies a URI a SAML defined identifier
     *                   known as Consent Identifiers.
     *       "Extension" - Specifies a list of Extensions as list of
     *                   String objects.
     * @throws SAML2Exception if error initiating request to IDP.
     */
    public static void initiateLogoutRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        String binding,
        Map paramsMap) 
    throws SAML2Exception {
        initiateLogoutRequest(request, response, binding,
            paramsMap, null, null, null);
    }

    /**
     * Parses the request parameters and initiates the Logout
     * Request to be sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param binding binding used for this request.
     * @param paramsMap Map of all other parameters.
     *       Following parameters names with their respective
     *       String values are allowed in this paramsMap.
     *       "RelayState" - the target URL on successful Single Logout
     *       "Destination" - A URI Reference indicating the address to
     *                       which the request has been sent.
     *       "Consent" - Specifies a URI a SAML defined identifier
     *                   known as Consent Identifiers.
     *       "Extension" - Specifies a list of Extensions as list of
     *                   String objects.
     * @param origLogoutRequest original LogoutRequest
     * @param msg SOAPMessage 
     * @param  newSession Session object for IDP Proxy
     * @throws SAML2Exception if error initiating request to IDP.
     */
    public static void initiateLogoutRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        String binding,
        Map paramsMap, 
        LogoutRequest origLogoutRequest, 
        SOAPMessage msg, 
        Object newSession)
        throws SAML2Exception {

        if (debug.messageEnabled()) {
            debug.message("SPSingleLogout:initiateLogoutRequest");
            debug.message("binding : " + binding);
            debug.message("paramsMap : " + paramsMap);
        }

        String metaAlias = (String)paramsMap.get(SAML2Constants.SP_METAALIAS);
        try {
            Object session = null; 
            if (newSession != null) {
               session = newSession; 
            } else {
                session = sessionProvider.getSession(request);
            }
            if (!SPCache.isFedlet) {
                if (session == null) {
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("nullSSOToken"));
                }
            }
            if (metaAlias == null) {
                if (!SPCache.isFedlet) {
                    String[] values =
                        sessionProvider.getProperty(
                            session, SAML2Constants.SP_METAALIAS);
                    if (values != null && values.length > 0) {
                        metaAlias = values[0];
                    }
                } else {
                    List spMetaAliases =
                        sm.getAllHostedServiceProviderMetaAliases("/");
                    if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                        // get first one
                        metaAlias = (String) spMetaAliases.get(0);
                    }
                }
            }
            
            if (metaAlias == null) {
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("nullSPMetaAlias"));
            }
            
            paramsMap.put(SAML2Constants.METAALIAS, metaAlias);
            String realm = SAML2Utils.
                getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
            debug.message("realm : " + realm);
            String spEntityID = sm.getEntityByMetaAlias(metaAlias);
            if (spEntityID == null) {
                debug.error("Service Provider ID is missing");
                String[] data = {spEntityID};
                LogUtil.error(
                    Level.INFO,LogUtil.INVALID_SP,data,null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nullSPEntityID"));
            }
            debug.message("spEntityID : " + spEntityID);

            // clean up session index
            String tokenID = sessionProvider.getSessionID(session);
            String infoKeyString = null;            
            if (SPCache.isFedlet) {
                infoKeyString = SAML2Utils.getParameter(paramsMap,
                    SAML2Constants.INFO_KEY);
            } else {
                try {
                    String[] values = sessionProvider.getProperty(
                        session, AccountUtils.getNameIDInfoKeyAttribute());
                    if (values != null && values.length > 0) {
                        infoKeyString = values[0];
                    }
                } catch (SessionException se) {
                    debug.error("Unable to get infoKeyString from " +
                        "session.", se);
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("errorInfoKeyString"));
                }
            }
            if (debug.messageEnabled()) {
                debug.message("tokenID : " + tokenID);
                debug.message("infoKeyString : " + infoKeyString);
            }

            // get SPSSODescriptor
            SPSSODescriptorElement spsso =
                sm.getSPSSODescriptor(realm,spEntityID);

            if (spsso == null) {
                String[] data = {spEntityID};
                LogUtil.error(Level.INFO,LogUtil.SP_METADATA_ERROR,data,
                    null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
            }
            List extensionsList = LogoutUtil.getExtensionsList(paramsMap);

            String relayState = SAML2Utils.getParameter(paramsMap,
                SAML2Constants.RELAY_STATE);
            
            if (relayState == null || relayState.equals("")) {
                relayState = SAML2Utils.getAttributeValueFromSSOConfig(realm,
                    spEntityID, SAML2Constants.SP_ROLE,
                    SAML2Constants.DEFAULT_RELAY_STATE);
            }

            // Validate the RelayState URL.
            SAML2Utils.validateRelayStateURL(realm,
                                             spEntityID,
                                             relayState,
                                             SAML2Constants.SP_ROLE);

            if (infoKeyString == null) {
                // termination case, do local logout only and send to
                // relay state if any
                debug.warning("SPSingleLogout.initiateLogoutRequest : Unable to get infoKeyString from session.");
                sessionProvider.invalidateSession(session, request, response);
                if ((relayState != null) && !relayState.equals("")) {
                    try {
                        response.sendRedirect(relayState);
                    } catch (IOException e) {
                        debug.error("SPSingleLogout.initiateLogoutRequest: "
                            + "Error in send redirect to " + relayState, e);
                    }
                } else {
                    RequestDispatcher dispatcher = request.getRequestDispatcher(
                        "saml2/jsp/default.jsp?message=spSloSuccess");
                    try {
                        dispatcher.forward(request, response);
                    } catch (IOException e) {
                        debug.error("SPSingleLogout.initiateLogoutRequest: "
                            + "Error in forwarding to default.jsp", e);
                    } catch (ServletException e) {
                        debug.error("SPSingleLogout.initiateLogoutRequest: "
                            + "Error in forwarding to default.jsp", e);
                    }
                }
                return;
            }
            StringTokenizer st = new StringTokenizer(infoKeyString, SAML2Constants.SECOND_DELIM);
            String requestID = null; 
            while (st.hasMoreTokens()) {
                String tmpInfoKeyString = st.nextToken();
                NameIDInfoKey nameIdInfoKey = NameIDInfoKey.parse(tmpInfoKeyString);
                //only try to perform the logout for the SP entity who is currently assigned to the session, this is
                //to cover the case when there are multiple hosted SPs authenticating against the same IdP. In this
                //scenario the sp metaalias will always be the SP who authenticated last, so we must ensure that we
                //send out the LogoutRequest to the single IdP correctly. Once that's done the IdP will send the
                //logout request to the other SP instance, invalidating the session for both SPs.
                if (nameIdInfoKey.getHostEntityID().equals(spEntityID)) {
                    requestID = prepareForLogout(realm, tokenID, metaAlias, extensionsList, binding, relayState, request,
                            response, paramsMap, tmpInfoKeyString, origLogoutRequest, msg);
                }
            }
            // IDP Proxy 
            SOAPMessage soapMsg = (SOAPMessage) 
                IDPCache.SOAPMessageByLogoutRequestID.get(
                requestID); 
            if (soapMsg != null) {   
                IDPProxyUtil.sendProxyLogoutResponseBySOAP(
                    soapMsg,response);  
            }     
            // local log out for SOAP. For HTTP case, session will be destroyed 
            // when SAML Response reached the SP side.
            if (binding.equals(SAML2Constants.SOAP) || (requestID == null)) {
                sessionProvider.invalidateSession(session, request, response); 
            }
        } catch (SAML2MetaException sme) {
            debug.error("Error retreiving metadata",sme);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        } catch (SessionException ssoe) {
            debug.error("Session exception: ",ssoe);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        }
    }

    private static String prepareForLogout(String realm,
        String tokenID,
        String metaAlias,
        List extensionsList,
        String binding,
        String relayState,
        HttpServletRequest request,
        HttpServletResponse response,
        Map paramsMap,
        String infoKeyString,
        LogoutRequest origLogoutRequest, 
        SOAPMessage msg) throws SAML2Exception, SessionException {

        NameIDInfoKey nameIdInfoKey = NameIDInfoKey.parse(infoKeyString);
        String sessionIndex = null;
        NameID nameID = null;
        if (SPCache.isFedlet) {
            sessionIndex = SAML2Utils.getParameter(paramsMap,
                SAML2Constants.SESSION_INDEX);
             nameID = AssertionFactory.getInstance().createNameID();
             nameID.setValue(nameIdInfoKey.getNameIDValue());
             nameID.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
             nameID.setNameQualifier(nameIdInfoKey.getRemoteEntityID());
             nameID.setSPNameQualifier(nameIdInfoKey.getHostEntityID());
        } else {
            SPFedSession fedSession = null;
        
            List list =
                (List)SPCache.fedSessionListsByNameIDInfoKey.get(infoKeyString);
            if (list != null) {
                synchronized (list) {
                    ListIterator iter = list.listIterator();
                    while (iter.hasNext()) {
                        fedSession = (SPFedSession)iter.next();
                        if (tokenID.equals(fedSession.spTokenID)) {
                            iter.remove();
                            if ((agent != null) &&
                                agent.isRunning() && (saml2Svc != null))
                            {
                                saml2Svc.setFedSessionCount(
		                    (long)SPCache.
					fedSessionListsByNameIDInfoKey.size());
                            }
                            if (list.size() == 0) {
                                SPCache.fedSessionListsByNameIDInfoKey.
                                    remove(infoKeyString);
                            }
                            break;
                        }
                        fedSession = null;
                    }
                }   
            }

            if (fedSession == null) {
                // just do local logout
                if (debug.messageEnabled()) {
                    debug.message(
                        "No session partner, just do local logout.");
                }
                return null;
            }
            sessionIndex = fedSession.idpSessionIndex;
            nameID = fedSession.info.getNameID();
        }


        // get IDPSSODescriptor
        IDPSSODescriptorElement idpsso =
            sm.getIDPSSODescriptor(realm,nameIdInfoKey.getRemoteEntityID());

        if (idpsso == null) {
            String[] data = {nameIdInfoKey.getRemoteEntityID()};
            LogUtil.error(Level.INFO,LogUtil.IDP_METADATA_ERROR,data,
                null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        }

        List slosList = idpsso.getSingleLogoutService();
        if (slosList == null) {
            String[] data = {nameIdInfoKey.getRemoteEntityID()};
            LogUtil.error(Level.INFO,LogUtil.SLO_NOT_FOUND,data,
                null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("sloServiceListNotfound"));
        }
        // get IDP entity config in case of SOAP, for basic auth info
        IDPSSOConfigElement idpConfig = null;
        if (binding.equals(SAML2Constants.SOAP)) {
            idpConfig = sm.getIDPSSOConfig(
                realm,
                nameIdInfoKey.getRemoteEntityID()
            );
        }
        
        StringBuffer requestID = LogoutUtil.doLogout(
            metaAlias,
            nameIdInfoKey.getRemoteEntityID(),
            slosList,
            extensionsList,
            binding,
            relayState,
            sessionIndex,
            nameID,
            request,
            response,
            paramsMap,
            idpConfig);

        String requestIDStr = requestID.toString();
        if (debug.messageEnabled()) {
            debug.message(
                "\nSPSLO.requestIDStr = " + requestIDStr +
                "\nbinding = " + binding);
        }
         
        if ((requestIDStr != null) && (requestIDStr.length() != 0) &&
            (binding.equals(SAML2Constants.HTTP_REDIRECT) ||
            binding.equals(SAML2Constants.HTTP_POST)) && 
            (origLogoutRequest != null)) {
             IDPCache.proxySPLogoutReqCache.put(requestIDStr, 
                 origLogoutRequest);
        } else if ((requestIDStr != null) && (requestIDStr.length() != 0) &&
            binding.equals(SAML2Constants.SOAP) && (msg != null)) { 
            IDPCache.SOAPMessageByLogoutRequestID.put(requestIDStr, msg);
        }
        return requestIDStr;
    }

    /**
     * Gets and processes the Single <code>LogoutResponse</code> from IDP,
     * destroys the local session, checks response's issuer
     * and inResponseTo.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param samlResponse <code>LogoutResponse</code> in the
     *          XML string format.
     * @param relayState the target URL on successful
     * <code>LogoutResponse</code>.
     * @throws SAML2Exception if error processing
     *          <code>LogoutResponse</code>.
     * @throws SessionException if error processing
     *          <code>LogoutResponse</code>.
     */
    public static Map processLogoutResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        String samlResponse,
        String relayState) throws SAML2Exception, SessionException  {
        String method = "SPSingleLogout:processLogoutResponse : ";
        if (debug.messageEnabled()) {
            debug.message(method + "samlResponse : " + samlResponse);
            debug.message(method + "relayState : " + relayState);
        }

        String rmethod = request.getMethod();
        String binding = SAML2Constants.HTTP_REDIRECT;
        if (rmethod.equals("POST")) {
            binding = SAML2Constants.HTTP_POST;
        }
        String metaAlias =
                SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI()) ;
        if ((SPCache.isFedlet) && 
            ((metaAlias ==  null) || (metaAlias.length() == 0))) 
        {
            List spMetaAliases =
                sm.getAllHostedServiceProviderMetaAliases("/");
            if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                // get first one
                metaAlias = (String) spMetaAliases.get(0);
            }
        }
        if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullSPEntityID"));
        }
        String realm = SAML2Utils.
                getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
        String spEntityID = sm.getEntityByMetaAlias(metaAlias);
        if (!SAML2Utils.isSPProfileBindingSupported(
            realm, spEntityID, SAML2Constants.SLO_SERVICE, binding))
        {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }

        // Validate the RelayState URL.
        SAML2Utils.validateRelayStateURL(realm,
                                         spEntityID,
                                         relayState,
                                         SAML2Constants.SP_ROLE);

        LogoutResponse logoutRes = null;
        if (rmethod.equals("POST")) {
                logoutRes = LogoutUtil.getLogoutResponseFromPost(samlResponse,
                response);
        } else if (rmethod.equals("GET")) {
            String decodedStr = SAML2Utils.decodeFromRedirect(samlResponse);
            if (decodedStr == null) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullDecodedStrFromSamlResponse"));
            }
            logoutRes = 
                ProtocolFactory.getInstance().createLogoutResponse(decodedStr);
        }

        if (logoutRes == null) {
            if (debug.messageEnabled()) {
                debug.message("SSingleLogout:processLogoutResponse: logoutRes " +
                       "is null");
            }
            return null;
        }

        String idpEntityID = logoutRes.getIssuer().getValue();
        Issuer resIssuer = logoutRes.getIssuer();
        String inResponseTo = logoutRes.getInResponseTo();
        LogoutRequest logoutReq =  (LogoutRequest)
            SPCache.logoutRequestIDHash.remove(inResponseTo);

        // invoke SPAdapter preSingleLogoutProcess
        String userId = null;
        if (!SPCache.isFedlet) {
            userId = preSingleLogoutProcess(spEntityID, realm, request, 
                response, null, logoutReq, logoutRes, binding);
        }
 
        SAML2Utils.verifyResponseIssuer(
            realm, spEntityID, resIssuer, inResponseTo);
        boolean needToVerify = 
            SAML2Utils.getWantLogoutResponseSigned(realm, spEntityID, 
                             SAML2Constants.SP_ROLE);
        if (debug.messageEnabled()) {
            debug.message(method + "metaAlias : " + metaAlias);
            debug.message(method + "realm : " + realm);
            debug.message(method + "idpEntityID : " + idpEntityID);
            debug.message(method + "spEntityID : " + spEntityID);
        }
        Map infoMap = new HashMap(); 
        infoMap.put("entityid", spEntityID);  
 
        if (needToVerify) {
            boolean valid = false;
            if (rmethod.equals("GET")) {
                String queryString = request.getQueryString();
                valid = SAML2Utils.verifyQueryString(queryString, realm,
                    SAML2Constants.SP_ROLE, idpEntityID);
            } else {
                valid = LogoutUtil.verifySLOResponse(logoutRes, realm,
                    idpEntityID, spEntityID, SAML2Constants.SP_ROLE);
            }

            if (!valid) {
                debug.error("SPSingleLogout.processLogoutResponse: " +
                    "Invalid signature in SLO Response.");
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "invalidSignInResponse"));
            }
            SPSSODescriptorElement spsso =
                sm.getSPSSODescriptor(realm, spEntityID);
            String loc = getSLOResponseLocationOrLocation(spsso, binding); 
            if (!SAML2Utils.verifyDestination(logoutRes.getDestination(),
                loc)) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidDestination"));
            }    
        }

        if (inResponseTo == null ||
            inResponseTo.length() == 0) {
            if (debug.messageEnabled()) {
                debug.message(
                    "LogoutResponse inResponseTo is null");
            }
            throw new SAML2Exception(
                SAML2Utils.bundle.getString(
                "nullInResponseToFromSamlResponse"));
        }

        if (logoutReq != null) {
            if (debug.messageEnabled()) {
                debug.message(
                    "LogoutResponse inResponseTo matches "+
                    "LogoutRequest ID.");
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message(
                    "LogoutResponse inResponseTo does not match " +
                    "LogoutRequest ID.");
            }
            throw new SAML2Exception(
                SAML2Utils.bundle.getString(
                "LogoutRequestIDandInResponseToDoNotMatch"));
        }
        
        infoMap.put("inResponseTo" , inResponseTo);                         
        infoMap.put(SAML2Constants.RELAY_STATE, relayState);
        // destroy session
        try {
            Object session = sessionProvider.getSession(request);
            if ((session != null) && sessionProvider.isValid(session)) {
                sessionProvider.invalidateSession(session, request, response);
            }
        } catch (SessionException se) {
            debug.message("SPSingleLogout.processLogoutResponse() : Unable to invalidate session: " + se.getMessage());
        }
        if (!SPCache.isFedlet) {
            if (isSuccess(logoutRes)) {
                // invoke SPAdapter postSingleLogoutSucces
                postSingleLogoutSuccess(spEntityID, realm, request, response, 
                    userId, logoutReq, logoutRes, binding); 
            } else {
                throw new SAML2Exception(SAML2Utils.BUNDLE_NAME, "sloFailed", null);
            }
        } else {
            // obtain fedlet adapter
            FedletAdapter fedletAdapter = 
                SAML2Utils.getFedletAdapterClass(spEntityID, realm);
            if (fedletAdapter != null) {
                if (isSuccess(logoutRes)) {
                    fedletAdapter.onFedletSLOSuccess(
                        request, response, logoutReq, logoutRes,
                        spEntityID, idpEntityID, binding);
                } else {
                    fedletAdapter.onFedletSLOFailure(
                        request, response, logoutReq, logoutRes,
                        spEntityID, idpEntityID, binding);
                    throw new SAML2Exception(SAML2Utils.BUNDLE_NAME, "sloFailed", null);
                }
            }
        }
        return infoMap; 
    }


    static String preSingleLogoutProcess(String hostedEntityID,
        String realm, HttpServletRequest request, HttpServletResponse response,
        String userID, LogoutRequest logoutRequest, 
        LogoutResponse logoutResponse, String binding) throws SAML2Exception {
      
        SAML2ServiceProviderAdapter spAdapter = null;
        try {
            spAdapter = SAML2Utils.getSPAdapterClass(hostedEntityID, realm);
        } catch (SAML2Exception e) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SPACSUtils.invokeSPAdapterForPreSLOProcess", e);
            }
        }
        if (spAdapter != null) {
            if (userID == null) {
                try {
                    Object session = sessionProvider.getSession(request);
                    if (session != null) {
                        userID = sessionProvider.getPrincipalName(session);
                    }
                } catch (SessionException ex) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(
                            "SPACSUtils.invokeSPAdapterForPreSLOProcess2", ex);
                    }
                }
            }
            spAdapter.preSingleLogoutProcess(hostedEntityID, realm, request, 
                response, userID, logoutRequest, logoutResponse, binding);
        }
        return userID;
    }

    static void postSingleLogoutSuccess(String hostedEntityID,
        String realm, HttpServletRequest request, HttpServletResponse response,
        String userID, LogoutRequest logoutRequest, 
        LogoutResponse logoutResponse, String binding) {
      
        SAML2ServiceProviderAdapter spAdapter = null;
        try {
            spAdapter = SAML2Utils.getSPAdapterClass(hostedEntityID, realm);
        } catch (SAML2Exception e) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SPACSUtils.invokeSPAdapterForPostSLOProcess", e);
            }
        }
        if (spAdapter != null) {
            spAdapter.postSingleLogoutSuccess(hostedEntityID, realm, request, 
                response, userID, logoutRequest, logoutResponse, binding);
        }
    }

    /**
     * Gets and processes the Single <code>LogoutRequest</code> from IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param out the print writer for writing out presentation
     * @param samlRequest <code>LogoutRequest</code> in the
     *          XML string format.
     * @param relayState the target URL on successful
     * <code>LogoutRequest</code>.
     * @throws SAML2Exception if error processing
     *          <code>LogoutRequest</code>.
     * @throws SessionException if error processing
     *          <code>LogoutRequest</code>.
     */
    public static void processLogoutRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        PrintWriter out,
        String samlRequest,
        String relayState) throws SAML2Exception, SessionException {
        String method = "processLogoutRequest : ";
        if (debug.messageEnabled()) {
            debug.message(method + "samlRequest : " + samlRequest);
            debug.message(method + "relayState : " + relayState);
        }
        String rmethod = request.getMethod();
        String binding = SAML2Constants.HTTP_REDIRECT;
        if (rmethod.equals("POST")) {
            binding = SAML2Constants.HTTP_POST;
        }
    
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(
            request.getRequestURI()) ;

        if ((SPCache.isFedlet) && 
            ((metaAlias == null) || (metaAlias.length() == 0)))
        {
            List spMetaAliases =
                sm.getAllHostedServiceProviderMetaAliases("/");
            if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                // get first one
                metaAlias = (String) spMetaAliases.get(0);
            }
            if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nullSPEntityID"));
            }
        }
        String realm = SAML2Utils.getRealm(
            SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
        String spEntityID = sm.getEntityByMetaAlias(metaAlias);
        if (!SAML2Utils.isSPProfileBindingSupported(
            realm, spEntityID, SAML2Constants.SLO_SERVICE, binding))
        {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }

        LogoutRequest logoutReq = null;
        if (rmethod.equals("POST")) {
            logoutReq = LogoutUtil.getLogoutRequestFromPost(samlRequest,
                response);
        } else if (rmethod.equals("GET")) {
            String decodedStr = SAML2Utils.decodeFromRedirect(samlRequest);
            if (decodedStr == null) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "nullDecodedStrFromSamlRequest"));
            }
            logoutReq = 
                ProtocolFactory.getInstance().createLogoutRequest(decodedStr);
        }

        if (logoutReq == null) {
            if (debug.messageEnabled()) {
                debug.message("SPSingleLogout:processLogoutRequest: logoutReq " +
                       "is null");
            }
            return;
        }
        String location = null;
        String idpEntityID = logoutReq.getIssuer().getValue();

        // invoke SPAdapter preSingleLogoutProcess : IDP initiated HTTP
        //String userId = preSingleLogoutProcess(spEntityID, realm, request, 
        //    response, null, logoutReq, null, SAML2Constants.HTTP_REDIRECT); 
        
        boolean needToVerify = 
            SAML2Utils.getWantLogoutRequestSigned(realm, spEntityID, 
                            SAML2Constants.SP_ROLE);
        if (debug.messageEnabled()) {
                debug.message(method + "metaAlias : " + metaAlias);
                debug.message(method + "realm : " + realm);
                debug.message(method + "idpEntityID : " + idpEntityID);
                debug.message(method + "spEntityID : " + spEntityID);
        }
        
        if (needToVerify == true) {
            boolean valid = false;
            if (rmethod.equals("POST")) {
                valid = LogoutUtil.verifySLORequest(logoutReq, realm,
                    idpEntityID, spEntityID, SAML2Constants.SP_ROLE); 
            } else {
                String queryString = request.getQueryString();
                valid = SAML2Utils.verifyQueryString(queryString, realm,
                    SAML2Constants.SP_ROLE, idpEntityID);
            }
            if (!valid) {
                debug.error("SPSingleLogout.processLogoutRequest: " +
                    "Invalid signature in SLO Request.");
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "invalidSignInRequest"));
            }
            SPSSODescriptorElement spsso =
                sm.getSPSSODescriptor(realm, spEntityID);
            String loc = getSLOResponseLocationOrLocation(spsso, binding);
            if (!SAML2Utils.verifyDestination(logoutReq.getDestination(),
                loc)) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidDestination"));
            } 
        }
        
        // get IDPSSODescriptor
        IDPSSODescriptorElement idpsso =
            sm.getIDPSSODescriptor(realm,idpEntityID);
        
        if (idpsso == null) {
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,LogUtil.IDP_METADATA_ERROR,data,
                          null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        }
        
        List slosList = idpsso.getSingleLogoutService();
        if (slosList == null) {
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,LogUtil.SLO_NOT_FOUND,data,
                          null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("sloServiceListNotfound"));
        }
        
        location = LogoutUtil.getSLOResponseServiceLocation(
            slosList, binding);
        if (location == null || location.length() == 0) {
            location = LogoutUtil.getSLOServiceLocation(slosList, binding);

            if (location == null || location.length() == 0) {
                debug.error(
                    "Unable to find the IDP's single logout "+
                    "response service with the HTTP-Redirect binding");
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString(
                        "sloResponseServiceLocationNotfound"));
            } else {
                if (debug.messageEnabled()) {
                    debug.message(
                        "SP's single logout response service location = "+
                        location);
                }
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message(
                    "IDP's single logout response service location = "+
                    location);
            }
        }
        List partners = IDPProxyUtil.getSPSessionPartners(request);
        
        //IDP Proxy Case
        if (partners != null && !partners.isEmpty()) {
            LogoutResponse logoutRespon =
                processLogoutRequest(logoutReq, spEntityID, realm,
                request, response, false, false, binding, true);
            logoutRespon.setDestination(XMLUtils.escapeSpecialCharacters(
                location));
            IDPProxyUtil.sendIDPInitProxyLogoutRequest(request, response, out,
                 logoutRespon, location, spEntityID, idpEntityID, binding);
        } else {
            LogoutResponse logoutRes = processLogoutRequest(
                logoutReq, spEntityID, realm,
                request, response, true, binding, true);
            logoutRes.setDestination(XMLUtils.escapeSpecialCharacters(
                location));

            LogoutUtil.sendSLOResponse(response, request, logoutRes, location,
                relayState, realm, spEntityID, SAML2Constants.SP_ROLE,
                idpEntityID, binding);
        }
    }

    /**
     * Gets and processes the Single <code>LogoutRequest</code> from IDP
     * and return <code>LogoutResponse</code>.
     *
     * @param logoutReq <code>LogoutRequest</code> from IDP
     * @param spEntityID name of host entity ID.
     * @param realm name of host entity.
     * @param request HTTP servlet request.
     * @param response HTTP servlet response.
     * @param isLBReq true if the request is for load balancing.
     * @param binding value of <code>SAML2Constants.HTTP_REDIRECT</code> or
     *        <code>SAML2Constants.SOAP</code>.
     * @param isVerified true if the request is verified already.
     * @return LogoutResponse the target URL on successful
     * <code>LogoutRequest</code>.
     */
    public static LogoutResponse processLogoutRequest(
        LogoutRequest logoutReq, String spEntityID, String realm,
        HttpServletRequest request, HttpServletResponse response,
        boolean isLBReq, String binding, boolean isVerified) {
            return processLogoutRequest(logoutReq, spEntityID, realm,
            request, response, isLBReq, true, binding, isVerified);        
     }    
 
    /**
     * Gets and processes the Single <code>LogoutRequest</code> from IDP
     * and return <code>LogoutResponse</code>.
     *
     * @param logoutReq <code>LogoutRequest</code> from IDP
     * @param spEntityID name of host entity ID.
     * @param realm name of host entity.
     * @param request HTTP servlet request.
     * @param response HTTP servlet response.
     * @param isLBReq true if the request is for load balancing.
     * @param binding value of <code>SAML2Constants.HTTP_REDIRECT</code> or
     *        <code>SAML2Constants.SOAP</code>.
     * @param isVerified true if the request is verified already.
     * @return LogoutResponse the target URL on successful
     * <code>LogoutRequest</code>.
     */
    public static LogoutResponse processLogoutRequest(
        LogoutRequest logoutReq, String spEntityID, String realm,
        HttpServletRequest request, HttpServletResponse response,
        boolean isLBReq, boolean destroySession, String binding,
        boolean isVerified) {
        final String method = "processLogoutRequest : "; 
        NameID nameID = null;
        Status status = null;
        Issuer issuer = null;
        String idpEntity = logoutReq.getIssuer().getValue();
        String userId = null;
        
        try {
            do {
                 // TODO: check the NotOnOrAfter attribute of LogoutRequest
                issuer = logoutReq.getIssuer();
                String requestId = logoutReq.getID();
                SAML2Utils.verifyRequestIssuer(
                                    realm, spEntityID, issuer, requestId);
                    
                issuer = SAML2Utils.createIssuer(spEntityID);
                // get SessionIndex and NameID form LogoutRequest
                List siList = logoutReq.getSessionIndex();
                int numSI = 0;
                if (siList != null) {
                    numSI = siList.size();
                    if (debug.messageEnabled()) {
                        debug.message(method +
                        "Number of session indices in the logout request is "
                        + numSI);
                    }
                }
            
                nameID = LogoutUtil.getNameIDFromSLORequest(logoutReq, realm, 
                                spEntityID, SAML2Constants.SP_ROLE);
                
                if (nameID == null) {
                    debug.error(method +
                                    "LogoutRequest does not contain Name ID");
                    status = SAML2Utils.generateStatus(
                            SAML2Constants.RESPONDER, 
                            SAML2Utils.bundle.
                            getString("missing_name_identifier"));
                    break;
                }

                String infoKeyString = null; 
                infoKeyString = (new NameIDInfoKey(nameID.getValue(), 
                         spEntityID, idpEntity)).toValueString(); 
                if (debug.messageEnabled()) {
                    debug.message(method + "infokey=" + infoKeyString);
                }
                if (SPCache.isFedlet) {
                    // verify request
                    if(!isVerified &&
                        !LogoutUtil.verifySLORequest(logoutReq, realm,
                        idpEntity, spEntityID, SAML2Constants.SP_ROLE)) 
                    {
                        throw new SAML2Exception(
                           SAML2Utils.bundle.getString("invalidSignInRequest"));
                    }

                    // obtain fedlet adapter
                    FedletAdapter fedletAdapter = 
                        SAML2Utils.getFedletAdapterClass(spEntityID, realm);
                    boolean result = false;
                    if (fedletAdapter != null) {
                        // call adapter to do real logout
                        result = fedletAdapter.doFedletSLO(request, response,
                            logoutReq, spEntityID, idpEntity, siList,
                            nameID.getValue(), binding);
                    }
                    if (result) {
                        status = SUCCESS_STATUS;
                    } else {
                        status = SAML2Utils.generateStatus(
                            SAML2Constants.RESPONDER,
                            SAML2Utils.bundle.getString("appLogoutFailed"));
                    }
                    break;
                }
                List list = (List)SPCache.fedSessionListsByNameIDInfoKey
                                         .get(infoKeyString);
                if (debug.messageEnabled()) {
                    debug.message(method + "SPFedsessions=" + list);
                }

                if ((list == null) || list.isEmpty()) {
                    String spQ = nameID.getSPNameQualifier();
                    if ((spQ == null) || (spQ.length() == 0)) {
                        infoKeyString = (new NameIDInfoKey(nameID.getValue(), 
                            spEntityID,
                            nameID.getNameQualifier())).toValueString(); 
                        list = (List)SPCache.fedSessionListsByNameIDInfoKey
                            .get(infoKeyString);

                    }
                }

                boolean foundPeer = false;
                List remoteServiceURLs = null;
                if (isLBReq) {
                    remoteServiceURLs = FSUtils.getRemoteServiceURLs(request);
                    foundPeer = remoteServiceURLs != null &&
                                !remoteServiceURLs.isEmpty();
                }

                if (debug.messageEnabled()) {
                    debug.message(method + "isLBReq = " + isLBReq +
                                 ", foundPeer = " + foundPeer);
                }

                if (list == null || list.isEmpty()) {
                    if (foundPeer) {
                        boolean peerError = false;
                        for(Iterator iter = remoteServiceURLs.iterator();
                            iter.hasNext();) {

                            String remoteLogoutURL = getRemoteLogoutURL(
                                (String)iter.next(), request);
                            LogoutResponse logoutRes =
                                LogoutUtil.forwardToRemoteServer(
                                    logoutReq, remoteLogoutURL);
                            if ((logoutRes != null) &&
                                !isNameNotFound(logoutRes)) {
                                if (isSuccess(logoutRes)) {
                                    if (numSI > 0) {
                                       siList =
                                         LogoutUtil.getSessionIndex(logoutRes);
                                       if (siList == null || siList.isEmpty()){
                                           peerError = false;
                                           break;
                                       }
                                    }
                                } else { 
                                    peerError = true;
                                }
                            }

                        }
                        if (peerError ||
                            (siList != null && siList.size() > 0)) {
                            status = PARTIAL_LOGOUT_STATUS;
                        } else {
                            status = SUCCESS_STATUS;
                        }
                    } else {
                        debug.error(method + "invalid Name ID received");
                        status = SAML2Utils
                               .generateStatus(SAML2Constants.RESPONDER, 
                                SAML2Utils.bundle
                                        .getString("invalid_name_identifier"));
                    }
                    break;
                } else {
                    // find the session, do signature validation
                    if(!isVerified &&
                        !LogoutUtil.verifySLORequest(logoutReq, realm,
                        logoutReq.getIssuer().getValue(),
                        spEntityID, SAML2Constants.SP_ROLE)) {
                        throw new SAML2Exception(
                           SAML2Utils.bundle.getString("invalidSignInRequest"));
                    }

                    // invoke SPAdapter for preSingleLogoutProcess
                    try {                     
                        String tokenId = 
                            ((SPFedSession) list.iterator().next()).spTokenID;
                        Object token = sessionProvider.getSession(tokenId);
                        userId = sessionProvider.getPrincipalName(token);
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message("SPSingleLogout." +
                                "processLogoutRequest, user = " + userId);
                        }
                    } catch (SessionException ex) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message("SPSingleLogout." +
                                "processLogoutRequest", ex);
                        }
                    }
                    userId = preSingleLogoutProcess(spEntityID, realm, request, 
                        response, userId, logoutReq, null, binding);
                }

                // get application logout URL 
                BaseConfigType spConfig = SAML2Utils.getSAML2MetaManager()
                    .getSPSSOConfig(realm, spEntityID);
                List appLogoutURL = (List) SAML2MetaUtils.getAttributes(
                    spConfig).get(SAML2Constants.APP_LOGOUT_URL);
                if (debug.messageEnabled()) {
                    debug.message("IDPLogoutUtil.processLogoutRequest: " +
                        "external app logout URL= " + appLogoutURL);
                }
 
                if (numSI == 0) {
                    // logout all fed sessions for this user
                    // between this SP and the IDP
                    List tokenIDsToBeDestroyed = new ArrayList();
                    synchronized (list) {
                        Iterator iter = list.listIterator();
                        while (iter.hasNext()) {
                            SPFedSession fedSession =(SPFedSession) iter.next();
                            tokenIDsToBeDestroyed.add(fedSession.spTokenID);
                            iter.remove();
                            if ((agent != null) &&
                                agent.isRunning() &&
                                (saml2Svc != null))
                            {
                                saml2Svc.setFedSessionCount(
		                    (long)SPCache.
					fedSessionListsByNameIDInfoKey.size());
                            }
                        }
                    }
                   
                    for (Iterator iter = tokenIDsToBeDestroyed.listIterator();
                        iter.hasNext();) {                          
                        String tokenID =(String) iter.next();
                        Object token = null; 
                        try {
                            token = sessionProvider.getSession(tokenID);
                        } catch (SessionException se) {
                            debug.error(method
                                + "Could not create session from token ID = " +
                                tokenID);
                            continue;    
                        }
                        if (debug.messageEnabled()) {
                            debug.message(method
                                + "destroy token " + tokenID);
                        } 
                        // handle external application logout if configured
                        if ((appLogoutURL != null) && 
                            (appLogoutURL.size() != 0)) {
                            SAML2Utils.postToAppLogout(request,
                                (String) appLogoutURL.get(0), token);
                        }
                        if (destroySession) {
                            sessionProvider.invalidateSession(token, request,
                            response);
                        }    
                    }
                    if (foundPeer) {
                        boolean peerError = false;
                        for(Iterator iter = remoteServiceURLs.iterator();
                            iter.hasNext();) {

                            String remoteLogoutURL = getRemoteLogoutURL(
                                (String)iter.next(), request);
                            LogoutResponse logoutRes =
                                 LogoutUtil.forwardToRemoteServer(logoutReq,
                                 remoteLogoutURL);
                            if ((logoutRes == null) || !(isSuccess(logoutRes) ||
                                  isNameNotFound(logoutRes))) {
                                peerError = true;
                            }
                        }
                        if (peerError) {
                            status = PARTIAL_LOGOUT_STATUS;
                        } else {
                            status = SUCCESS_STATUS;
                        }
                     }
                } else {
                    // logout only those fed sessions specified
                    // in logout request session list
                    String sessionIndex = null;
                    List siNotFound = new ArrayList();
                    for (int i = 0; i < numSI; i++) {
                        sessionIndex = (String)siList.get(i);
                       
                        String tokenIDToBeDestroyed = null;
                        synchronized (list) {
                            Iterator iter = list.listIterator();
                            while (iter.hasNext()) {
                                SPFedSession fedSession = 
                                    (SPFedSession) iter.next();
                                if (sessionIndex
                                          .equals(fedSession.idpSessionIndex)) {
                                    if (debug.messageEnabled()) {
                                        debug.message(method + " found si + " +
                                            sessionIndex);
                                    }
                                    tokenIDToBeDestroyed = fedSession.spTokenID;
                                    iter.remove();
                                    if ((agent != null) &&
                                        agent.isRunning() &&
                                        (saml2Svc != null))
                                    {
                                        saml2Svc.setFedSessionCount(
		                            (long)SPCache.
						fedSessionListsByNameIDInfoKey.
						   size());
                                    }
                                    break;
                                }
                            }   
                        }
                        
                        if (tokenIDToBeDestroyed != null) {      
                            try {
                                 Object token = sessionProvider.getSession(
                                        tokenIDToBeDestroyed);
                                 if (debug.messageEnabled()) {
                                     debug.message(method 
                                         + "destroy token (2) " 
                                         + tokenIDToBeDestroyed);
                                 }
                                 // handle external application logout 
                                 if ((appLogoutURL != null) && 
                                     (appLogoutURL.size() != 0)) {
                                     SAML2Utils.postToAppLogout(request,
                                         (String) appLogoutURL.get(0), token);
                                 }
                                 if (destroySession) {
                                     sessionProvider.invalidateSession(
                                        token, request, response);
                                 }   
                            } catch (SessionException se) {
                                debug.error(method + "Could not create " +
                                    "session from token ID = " +
                                    tokenIDToBeDestroyed);
                            }
                        } else {
                            siNotFound.add(sessionIndex);
                        }
                    }

                    if (isLBReq) {
                        if (foundPeer && !siNotFound.isEmpty()) {
                            boolean peerError = false;
                            LogoutRequest lReq = copyAndMakeMutable(logoutReq);
                            for(Iterator iter = remoteServiceURLs.iterator();
                                iter.hasNext();) {

                                lReq.setSessionIndex(siNotFound);
                                String remoteLogoutURL = getRemoteLogoutURL(
                                    (String)iter.next(), request);
                                LogoutResponse logoutRes =
                                    LogoutUtil.forwardToRemoteServer(lReq, 
                                        remoteLogoutURL);
                                if ((logoutRes != null) &&
                                    !isNameNotFound(logoutRes)) {
                                    if (isSuccess(logoutRes)) {
                                        siNotFound =
                                         LogoutUtil.getSessionIndex(logoutRes);
                                    } else { 
                                        peerError = true;
                                    }
                                }

                                if (debug.messageEnabled()) {
                                    debug.message(method 
                                         + "siNotFound = " 
                                         + siNotFound);
                                }
                                if (siNotFound == null ||
                                    siNotFound.isEmpty()) {
                                    peerError = false;
                                    break;
                                }
                            }
                            if (peerError ||
                                (siNotFound != null && !siNotFound.isEmpty())){
                                status = PARTIAL_LOGOUT_STATUS;
                            } else {
                                status = SUCCESS_STATUS;
                            }
                        } else {
                            status = SUCCESS_STATUS;
                        }
                    } else {
                        if (siNotFound.isEmpty()) {
                            status = SUCCESS_STATUS;
                        } else {
                            status = SAML2Utils.generateStatus(
                                SAML2Constants.SUCCESS,
                                SAML2Utils.bundle.getString("requestSuccess"));
                            LogoutUtil.setSessionIndex(status, siNotFound);
                        }
                    }
                }
            } while (false);
        } catch (SessionException se) {
            debug.error("processLogoutRequest: ", se);
            status = 
                    SAML2Utils.generateStatus(SAML2Constants.RESPONDER, 
                        se.toString());
        } catch (SAML2Exception e) {
            debug.error("processLogoutRequest: " + 
                "failed to create response", e);
            status = SAML2Utils.generateStatus(SAML2Constants.RESPONDER, 
                            e.toString());
        }
        
        // create LogoutResponse
        if (spEntityID == null) {
            spEntityID = nameID.getSPNameQualifier();
        }
        
        LogoutResponse logResponse =  LogoutUtil.generateResponse(
                status, logoutReq.getID(), issuer,
                realm, SAML2Constants.SP_ROLE, idpEntity);
       
        if (isSuccess(logResponse)) {
            // invoke SPAdapter for postSingleLogoutSuccess
            postSingleLogoutSuccess(spEntityID, realm, request, 
                response, userId, logoutReq, logResponse, binding);
        }

        return logResponse;
    }

    static boolean isSuccess(LogoutResponse logoutRes) {
        return logoutRes.getStatus().getStatusCode().getValue()
                        .equals(SAML2Constants.SUCCESS);
    }

    static boolean isNameNotFound(LogoutResponse logoutRes) {
        Status status = logoutRes.getStatus();
        String  statusMessage = status.getStatusMessage();

        return (status.getStatusCode().getValue()
                     .equals(SAML2Constants.RESPONDER) &&
                statusMessage != null &&
                statusMessage.equals(
                     SAML2Utils.bundle.getString("invalid_name_identifier")));
    }

    private static LogoutRequest copyAndMakeMutable(LogoutRequest src) {
        LogoutRequest dest = ProtocolFactory.getInstance()
                                            .createLogoutRequest();
        try {
            dest.setNotOnOrAfter(src.getNotOnOrAfter());
            dest.setReason(src.getReason());
            dest.setEncryptedID(src.getEncryptedID());
            dest.setNameID(src.getNameID());
            dest.setBaseID(src.getBaseID());
            dest.setSessionIndex(src.getSessionIndex());
            dest.setIssuer(src.getIssuer());
            dest.setExtensions(src.getExtensions());
            dest.setID(src.getID());
            dest.setVersion(src.getVersion());
            dest.setIssueInstant(src.getIssueInstant());        
            dest.setDestination(XMLUtils.escapeSpecialCharacters(
                src.getDestination()));
            dest.setConsent(src.getConsent());
        } catch(SAML2Exception ex) {
            debug.error("SPLogoutUtil.copyAndMakeMutable:", ex);
        }
        return dest;
    }
   
    private static String getSLOResponseLocationOrLocation(
        SPSSODescriptorElement spsso, String binding) {
        String location = null;
        if (spsso != null) {
            List sloList = spsso.getSingleLogoutService();
            if (sloList != null && !sloList.isEmpty()) {
                location = LogoutUtil.getSLOResponseServiceLocation(
                           sloList, binding);
                if (location == null || (location.length() == 0)) {
                    location = LogoutUtil.getSLOServiceLocation(
                          sloList, binding);
                }
            }
        }
        return location;
    }

    private static String getRemoteLogoutURL(String serverURL,
        HttpServletRequest request) {
        if (serverURL == null || request == null) {
            return null;
        }
        String queryString = request.getQueryString();
        if (queryString == null) {
            return serverURL + SAML2Utils.removeDeployUri(
                request.getRequestURI()) +  "?isLBReq=false";
        } else {
            return serverURL + SAML2Utils.removeDeployUri(
                request.getRequestURI()) + "?" + queryString + "&isLBReq=false";
        }
    }
}

