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
 * $Id: IDPSingleLogout.java,v 1.28 2009/11/25 01:20:47 madan_ranganath Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SingleLogoutServiceElement;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.utils.StringUtils;


/**
 * This class reads the required data from HttpServletRequest and
 * initiates the <code>LogoutRequest</code> from IDP to SP.
 */

public class IDPSingleLogout {

    static SAML2MetaManager sm = null;
    static Debug debug = SAML2Utils.debug;
    static SessionProvider sessionProvider = null;
    // Status elements
    static final Status SUCCESS_STATUS =
        SAML2Utils.generateStatus(SAML2Constants.SUCCESS,
        SAML2Utils.bundle.getString("requestSuccess"));
    static final Status PARTIAL_LOGOUT_STATUS =
        SAML2Utils.generateStatus(SAML2Constants.RESPONDER,
        SAML2Utils.bundle.getString("partialLogout"));
    static final Status ALREADY_LOGGEDOUT =
        SAML2Utils.generateStatus(SAML2Constants.SUCCESS,
         SAML2Utils.bundle.getString("sloAlreadyLoggedout"));
    private final static String QUESTION_MARK = "?";
    private static FedMonAgent agent;
    private static FedMonSAML2Svc saml2Svc;
    static {
        try {
            sm = new SAML2MetaManager();
        } catch (SAML2MetaException sme) {
            debug.error("Error retreiving metadata",sme);
        }
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            debug.error("Error retreiving session provider.", se);
        }
        agent = MonitorManager.getAgent();
        saml2Svc = MonitorManager.getSAML2Svc();
    }

    private IDPSingleLogout() {
    }

    /**
     * Parses the request parameters and initiates the Logout
     * Request to be sent to the SP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param out the print writer for writing out presentation
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
     * @throws SAML2Exception if error initiating request to SP.
     */
    public static void initiateLogoutRequest(HttpServletRequest request, HttpServletResponse response,
            PrintWriter out, String binding, Map paramsMap) throws SAML2Exception {

        if (debug.messageEnabled()) {
            debug.message("in initiateLogoutRequest");
            debug.message("binding : " + binding);
            debug.message("logoutAll : " + 
                            (String) paramsMap.get(SAML2Constants.LOGOUT_ALL));
            debug.message("paramsMap : " + paramsMap);
        }

        boolean logoutall = false;
        String logoutAllValue = 
                   (String)paramsMap.get(SAML2Constants.LOGOUT_ALL);
        if ((logoutAllValue != null) && 
                        logoutAllValue.equalsIgnoreCase("true")) {
            logoutall = true;
        }

        String metaAlias = (String)paramsMap.get(SAML2Constants.IDP_META_ALIAS);
        try {
            Object session = sessionProvider.getSession(request);
            String sessUser = sessionProvider.getPrincipalName(session);
            if (session == null) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nullSSOToken"));
            }
            if (metaAlias == null) {
                String[] values = sessionProvider.
                    getProperty(session, SAML2Constants.IDP_META_ALIAS);
                if (values != null && values.length != 0) {
                    metaAlias = values[0];
                }
            }
            if (metaAlias == null) {
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("nullIDPMetaAlias"));
            }
            paramsMap.put(SAML2Constants.METAALIAS, metaAlias);

            String realm = SAML2Utils.
                    getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));

            String idpEntityID = sm.getEntityByMetaAlias(metaAlias);
            if (idpEntityID == null) {
                debug.error("Identity Provider ID is missing");
                String[] data = {idpEntityID};
                LogUtil.error(
                    Level.INFO,LogUtil.INVALID_IDP,data,null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nullIDPEntityID"));
            }
            // clean up session index
            String idpSessionIndex = IDPSSOUtil.getSessionIndex(session);
            if (idpSessionIndex == null) {
                if (debug.messageEnabled()) {
                    debug.message("No SP session participant(s)");
                }
                MultiProtocolUtils.invalidateSession(session, request,
                    response, SingleLogoutManager.SAML2);
                return;
            }

            // If request has been misrouted and we don't  have SAML2 Failover
            // then send the request to the original server
            if (!SAML2FailoverUtils.isSAML2FailoverEnabled() &&
                   isMisroutedRequest(request, response, out, session)) {
                   return;
            } else {
               if (debug.messageEnabled()) {
                    debug.message("IDPSingleLogout.initiateLogoutRequest: "
                        + "SAML2 Failover will be attempted. Be sure SFO is "
                            + "properly configured or the attempt will fail");
                }
            }

            IDPSession idpSession = IDPCache.idpSessionsByIndices.get(idpSessionIndex);

            if (idpSession == null) {
                if (debug.messageEnabled()) {
                    debug.message("IDPSLO.initiateLogoutRequest: "
                        + "IDP Session with session index "
                        + idpSessionIndex + " already removed.");
                }
                try {
                    if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                         SAML2FailoverUtils.deleteSAML2Token(idpSessionIndex);
                     }
                } catch (SAML2TokenRepositoryException se) {
                    debug.error("IDPSingleLogout.initiateLogoutReq: Error while deleting token from " +
                            "SAML2 Token Repository for idpSessionIndex:" + idpSessionIndex, se);
                }
                IDPCache.authnContextCache.remove(idpSessionIndex);
                MultiProtocolUtils.invalidateSession(session, request,
                    response, SingleLogoutManager.SAML2);
                return;
            }
            if (debug.messageEnabled()) {
                debug.message("idpSessionIndex=" + idpSessionIndex);
            }
            List<NameIDandSPpair> list = idpSession.getNameIDandSPpairs();
            int n = list.size();
            if (debug.messageEnabled()) {
                debug.message("IDPSingleLogout.initiateLogoutReq:" +
                    " NameIDandSPpairs=" + list + ", size=" + n);
            }

            if (n == 0) {
                if (debug.messageEnabled()) {
                    debug.message("No SP session participant(s)");
                }
                IDPCache.idpSessionsByIndices.remove(idpSessionIndex);
                if ((agent != null) && agent.isRunning() && (saml2Svc != null)){
                    saml2Svc.setIdpSessionCount(
		        (long)IDPCache.idpSessionsByIndices.size());
                }
                try {
                    if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                        SAML2FailoverUtils.deleteSAML2Token(idpSessionIndex);
                    }
                } catch (SAML2TokenRepositoryException se) {
                    debug.error("IDPSingleLogout.initiateLogoutReq: Error while deleting token from " +
                            "SAML2 Token Repository for idpSessionIndex:" + idpSessionIndex, se);
                }
                IDPCache.authnContextCache.remove(idpSessionIndex);
                MultiProtocolUtils.invalidateSession(session, request,
                    response, SingleLogoutManager.SAML2);
                return;
            }

            String relayState =
                (String)paramsMap.get(SAML2Constants.RELAY_STATE);

            // Validate the RelayState URL.
            SAML2Utils.validateRelayStateURL(realm,
                                             idpEntityID,
                                             relayState,
                                             SAML2Constants.IDP_ROLE);

            int soapFailCount = 0;
            idpSession.setOriginatingLogoutRequestBinding(binding);
            for (int i = 0; i < n; i++) {
                NameIDandSPpair pair = list.remove(0);
                removeTransientNameIDFromCache(pair.getNameID());

                String spEntityID = pair.getSPEntityID();
                if (debug.messageEnabled()) {
                    debug.message("IDPSingleLogout.initLogoutReq: processing spEntityID " + spEntityID);
                }

                List extensionsList = LogoutUtil.getExtensionsList(paramsMap);
                List<SingleLogoutServiceElement> slosList = getSPSLOServiceEndpoints(realm, spEntityID);

                // get IDP entity config in case of SOAP, for basic auth info
                SPSSOConfigElement spConfig = sm.getSPSSOConfig(realm, spEntityID);

                if (logoutall == true) {
                    idpSessionIndex = null;
                }

                SingleLogoutServiceElement logoutEndpoint = LogoutUtil.getMostAppropriateSLOServiceLocation(slosList,
                        idpSession.getOriginatingLogoutRequestBinding());
                if (logoutEndpoint == null) {
                    continue;
                }
                StringBuffer requestID = null;
                try {
                    requestID = LogoutUtil.doLogout(metaAlias, spEntityID, extensionsList, logoutEndpoint, relayState,
                        idpSessionIndex, pair.getNameID(), request, response, paramsMap, spConfig);
                } catch (SAML2Exception ex) {
                    if (logoutEndpoint.getBinding().equals(SAML2Constants.SOAP)) {
                        debug.error(
                            "IDPSingleLogout.initiateLogoutRequest:" , ex);
                        soapFailCount++;
                        continue;
                    } else {
                        throw ex;
                    }
                }

                String requestIDStr = requestID.toString();
                String bindingUsed = logoutEndpoint.getBinding();
                if (debug.messageEnabled()) {
                    debug.message("\nIDPSLO.requestIDStr = " + requestIDStr + "\nbinding = " + bindingUsed);
                }

                if (!requestIDStr.isEmpty() && (bindingUsed.equals(SAML2Constants.HTTP_REDIRECT)
                        || bindingUsed.equals(SAML2Constants.HTTP_POST))) {
                    idpSession.setPendingLogoutRequestID(requestIDStr);
                    idpSession.setLogoutAll(logoutall);
                    Map logoutMap = (Map) paramsMap.get("LogoutMap");
                    if (logoutMap != null && !logoutMap.isEmpty()) {
                       IDPCache.logoutResponseCache.put(requestIDStr, (Map) paramsMap.get("LogoutMap"));
                    }
                    return;
                }
            }

            //This code only runs if the logout process didn't redirect away, so either none of the SPs supported the
            //requested binding, or SOAP was used for the logout (or the mixture of this two).
            if (logoutall == true) {
                String userID = sessionProvider.getPrincipalName(idpSession.getSession());
                destroyAllTokenForUser(userID, request, response);
            } else {
                MultiProtocolUtils.invalidateSession(idpSession.getSession(), request, response,
                        SingleLogoutManager.SAML2);
                IDPCache.idpSessionsByIndices.remove(idpSessionIndex);
                if (agent != null && agent.isRunning() && saml2Svc != null) {
                    saml2Svc.setIdpSessionCount((long) IDPCache.idpSessionsByIndices.size());
                }
                IDPCache.authnContextCache.remove(idpSessionIndex);
            }
            //handling the case when the auth was initiated with HTTP-Redirect, but only SOAP or no SLO endpoint was
            //available, and also the case when the whole logout process was using SOAP binding from the beginning
            int logoutStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
            boolean isMultiProtocol = MultiProtocolUtils.isMultipleProtocolSession(request, SingleLogoutManager.SAML2);
            //TODO: would be nice to actually return the correct message in idpSingleLogoutInit.jsp
            if (soapFailCount == n) {
                if (isMultiProtocol) {
                    logoutStatus = SingleLogoutManager.LOGOUT_FAILED_STATUS;
                }
            } else if (soapFailCount > 0) {
                if (isMultiProtocol) {
                    logoutStatus = SingleLogoutManager.LOGOUT_PARTIAL_STATUS;
                }
            }
            // processing multi-federation protocol session
            if (isMultiProtocol) {
                Set set = new HashSet();
                set.add(session);
                boolean isSOAPInitiated =
                    binding.equals(SAML2Constants.SOAP) ? true : false;
                int retStat = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
                try {
                    debug.message("IDPSingleLogout.initLogReq: MP");
                        retStat = SingleLogoutManager.getInstance().
                            doIDPSingleLogout(set, sessUser, request, response,
                            isSOAPInitiated, true, SingleLogoutManager.SAML2,
                            realm, idpEntityID, null, relayState, null, null,
                            logoutStatus);
                } catch (Exception ex) {
                        debug.warning("IDPSingleLogout.initiateLoogutReq: MP", 
                            ex);
                    throw new SAML2Exception(ex.getMessage());
                }
                if (debug.messageEnabled()) {
                        debug.message("IDPSingleLogout.initLogoutRequest: "
                            + "SLOManager return status = " + retStat);
                }
                switch (retStat) {
                    case SingleLogoutManager.LOGOUT_FAILED_STATUS:
                            throw new SAML2Exception(
                                    SAML2Utils.bundle.getString("sloFailed"));
                    case SingleLogoutManager.LOGOUT_PARTIAL_STATUS:
                            throw new SAML2Exception(
                                    SAML2Utils.bundle.getString("partialLogout"));
                    default:
                        break;
                }
            }
        } catch (SAML2MetaException sme) {
            debug.error("Error retreiving metadata",sme);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        } catch (SessionException ssoe) {
            debug.error("SessionException: ",ssoe);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        }
    }

    /**
     * Gets and processes the Single <code>LogoutRequest</code> from SP.
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
        String classMethod = "IDPSingleLogout.processLogoutRequest : ";
        if (debug.messageEnabled()) {
            debug.message(classMethod + "IDPSingleLogout:processLogoutRequest");
            debug.message(classMethod + "samlRequest : " + samlRequest);
            debug.message(classMethod + "relayState : " + relayState);
        }
        String rmethod= request.getMethod();
        String binding = SAML2Constants.HTTP_REDIRECT;
        if (rmethod.equals("POST")) {
            binding = SAML2Constants.HTTP_POST;
        }
        String metaAlias =
                SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI()) ;
        String realm = SAML2Utils.
                getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
        String idpEntityID = sm.getEntityByMetaAlias(metaAlias);
        if (!SAML2Utils.isIDPProfileBindingSupported(
            realm, idpEntityID, SAML2Constants.SLO_SERVICE, binding))
        {
            debug.error(classMethod + "SLO service binding " + binding +
                " is not supported for " + idpEntityID);
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
                debug.message("IDPSingleLogout:processLogoutRequest: logoutReq " +
                              "is null");
            }
           return;
        }

        String spEntityID = logoutReq.getIssuer().getValue();

        boolean needToVerify =
            SAML2Utils.getWantLogoutRequestSigned(realm, idpEntityID,
                            SAML2Constants.IDP_ROLE);
        if (debug.messageEnabled()) {
            debug.message(classMethod + "metaAlias : " + metaAlias);
            debug.message(classMethod + "realm : " + realm);
            debug.message(classMethod + "idpEntityID : " + idpEntityID);
            debug.message(classMethod + "spEntityID : " + spEntityID);
        }

        if (needToVerify) {
            boolean valid = false;
            if (binding.equals(SAML2Constants.HTTP_REDIRECT)) {
                String queryString = request.getQueryString();
                valid = SAML2Utils.verifyQueryString(queryString, realm,
                    SAML2Constants.IDP_ROLE, spEntityID);
            } else {
                valid = LogoutUtil.verifySLORequest(logoutReq, realm,
                    spEntityID, idpEntityID, SAML2Constants.IDP_ROLE);
            }
            if (!valid) {
                    debug.error("Invalid signature in SLO Request.");
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidSignInRequest"));
            }
            IDPSSODescriptorElement idpsso =
                sm.getIDPSSODescriptor(realm, idpEntityID);
            String loc = null;
            if (idpsso != null) {
                List sloList = idpsso.getSingleLogoutService();
                if ((sloList != null) && (!sloList.isEmpty())) {
                    loc = LogoutUtil.getSLOResponseServiceLocation(
                          sloList, binding);
                    if ((loc == null) || (loc.length() == 0)) {
                        loc = LogoutUtil.getSLOServiceLocation(
                             sloList, binding);
                    }
                }
            }
            if (!SAML2Utils.verifyDestination(logoutReq.getDestination(),
                loc)) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidDestination"));
            }
        }

        // Get the local session, if it does not exist send a succesful
        // Logout Response with a status message of "Already Logout"
        Object session = null;
        try {
            session = sessionProvider.getSession(request);
        } catch (SessionException ssoe) {
            sendAlreadyLogedOutResp(response, request, logoutReq, relayState,
                             realm, idpEntityID, spEntityID, binding);
            return;
        }

        // If the request has been misrouted and we don't  have SAML2 Failover
        // then send the request to the original server
        if (session != null && !SAML2FailoverUtils.isSAML2FailoverEnabled()
                && isMisroutedRequest(request, response, out, session)) {
            return;
        } else {
            if (debug.messageEnabled()) {
                debug.message(classMethod
                        + "SAML2 Failover will be attempted. Be sure SFO is "
                        + "properly configured or the attempt will fail");
            }
        }

        LogoutResponse logoutRes = processLogoutRequest(
            logoutReq, request, response, binding, relayState,
            idpEntityID, realm, true);
        if (logoutRes == null) {
            // this is the case where there is more SP session participant
            // and processLogoutRequest() sends LogoutRequest to one of them
            // already
            // through HTTP_Redirect, nothing to do here
            return;
        }

        // this is the case where there is no more SP session
        // participant
        SingleLogoutServiceElement endpoint = getLogoutResponseEndpoint(realm, spEntityID, binding);
        binding = endpoint.getBinding();
        String location = getResponseLocation(endpoint);
        logoutRes.setDestination(XMLUtils.escapeSpecialCharacters(location));

        // call multi-federation protocol processing
        // this is SP initiated HTTP based single logout
        boolean isMultiProtocolSession = false;
        int retStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
        try {
            if ((session != null) && (sessionProvider.isValid(session))
                && MultiProtocolUtils.isMultipleProtocolSession(session,
                    SingleLogoutManager.SAML2)) {
                isMultiProtocolSession = true;
                // call Multi-Federation protocol SingleLogoutManager
                SingleLogoutManager sloManager =
                    SingleLogoutManager.getInstance();
                Set set = new HashSet();
                set.add(session);
                String uid =  sessionProvider.getPrincipalName(session);
                debug.message("IDPSingleLogout.processLogReq: MP/SPinit/Http");
                retStatus = sloManager.doIDPSingleLogout(set, uid, request,
                    response, false, false, SingleLogoutManager.SAML2, realm,
                    idpEntityID, spEntityID, relayState, logoutReq.toString(),
                    logoutRes.toXMLString(), getLogoutStatus(logoutRes));
            }
        } catch (SessionException e) {
            // ignore as session might not be valid
            debug.message("IDPSingleLogout.processLogoutRequest: session",e);
        } catch (Exception e) {
            debug.message("IDPSingleLogout.processLogoutRequest: MP2",e);
            retStatus = SingleLogoutManager.LOGOUT_FAILED_STATUS;
        }

        if (!isMultiProtocolSession ||
            (retStatus != SingleLogoutManager.LOGOUT_REDIRECTED_STATUS)) {
            logoutRes = updateLogoutResponse(logoutRes, retStatus);
            List partners = IDPProxyUtil.getSessionPartners(request);
            if (partners != null &&  !partners.isEmpty()) {
                IDPProxyUtil.sendProxyLogoutRequest(request, response, out,
                    logoutReq, partners, binding, relayState);
            } else {
                LogoutUtil.sendSLOResponse(response, request, logoutRes, location,
                    relayState, realm, idpEntityID, SAML2Constants.IDP_ROLE,
                    spEntityID, binding);
            }
        }
    }

    private static SingleLogoutServiceElement getLogoutResponseEndpoint(String realm, String spEntityID,
            String binding) throws SAML2Exception {
        SingleLogoutServiceElement endpoint =
                LogoutUtil.getMostAppropriateSLOServiceLocation(getSPSLOServiceEndpoints(realm, spEntityID), binding);
        if (endpoint == null) {
            debug.error("Unable to find the SP's single logout response service with " + binding + " binding");
            throw new SAML2Exception(SAML2Utils.bundle.getString("sloResponseServiceLocationNotfound"));
        }
        if (SAML2Constants.SOAP.equals(endpoint.getBinding())) {
            debug.error("Unable to send logout response with SOAP binding");
            throw new SAML2Exception(SAML2Utils.bundle.getString("unsupportedBinding"));
        }
        return endpoint;
    }

    private static String getResponseLocation(SingleLogoutServiceElement endpoint) {
        String location = endpoint.getResponseLocation();
        if (StringUtils.isBlank(location)) {
            location = endpoint.getLocation();
        }
        return location;
    }

    /**
     * Returns single logout location for the service provider.
     */
    public static String getSingleLogoutLocation(String spEntityID, String realm, String binding)
            throws SAML2Exception {
        List<SingleLogoutServiceElement> slosList = getSPSLOServiceEndpoints(realm, spEntityID);

        String location = LogoutUtil.getSLOResponseServiceLocation(slosList, binding);

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
        if (debug.messageEnabled()) {
            debug.message("IDPSingleLogout.getSLOLocation: loc=" + location);
        }
        return location;
    }

    private static int getLogoutStatus(LogoutResponse logoutRes) {
        StatusCode statusCode = logoutRes.getStatus().getStatusCode();
        String code = statusCode.getValue();
        if (code.equals(SAML2Constants.SUCCESS)) {
            return SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
        } else {
            return SingleLogoutManager.LOGOUT_FAILED_STATUS;
        }
    }

    /**
     * Gets and processes the Single <code>LogoutResponse</code> from SP,
     * destroys the local session, checks response's issuer
     * and inResponseTo.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param samlResponse <code>LogoutResponse</code> in the
     *          XML string format.
     * @param relayState the target URL on successful
     * <code>LogoutResponse</code>.
     * @return true if jsp has sendRedirect for relayState, false otherwise
     * @throws SAML2Exception if error processing
     *          <code>LogoutResponse</code>.
     * @throws SessionException if error processing
     *          <code>LogoutResponse</code>.
     */
    public static boolean processLogoutResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        String samlResponse,
        String relayState) throws SAML2Exception, SessionException {
        String method = "processLogoutResponse : ";
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
        String realm = SAML2Utils.
                getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
        String idpEntityID = sm.getEntityByMetaAlias(metaAlias);
        if (!SAML2Utils.isIDPProfileBindingSupported(
            realm, idpEntityID, SAML2Constants.SLO_SERVICE, binding))
        {
            debug.error("SLO service binding " + binding + " is not supported:"+
                idpEntityID);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("unsupportedBinding"));
        }

        LogoutResponse logoutRes = null;
        if (rmethod.equals("POST")) {
            logoutRes = LogoutUtil.getLogoutResponseFromPost(samlResponse,
                response);
        } else if (rmethod.equals("GET")) {
            String decodedStr =
            SAML2Utils.decodeFromRedirect(samlResponse);
            if (decodedStr == null) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "nullDecodedStrFromSamlResponse"));
            }
            logoutRes =
                ProtocolFactory.getInstance().createLogoutResponse(decodedStr);
        }

        if (logoutRes == null) {
            if (debug.messageEnabled()) {
                debug.message("IDPSingleLogout:processLogoutResponse: logoutRes " +
                             "is null");
            }
           return false;
        }

        String spEntityID = logoutRes.getIssuer().getValue();
        Issuer resIssuer = logoutRes.getIssuer();
        String requestId = logoutRes.getInResponseTo();
        SAML2Utils.verifyResponseIssuer(
                            realm, idpEntityID, resIssuer, requestId);

        boolean needToVerify =
             SAML2Utils.getWantLogoutResponseSigned(realm, idpEntityID,
                             SAML2Constants.IDP_ROLE);
        if (debug.messageEnabled()) {
            debug.message(method + "metaAlias : " + metaAlias);
            debug.message(method + "realm : " + realm);
            debug.message(method + "idpEntityID : " + idpEntityID);
            debug.message(method + "spEntityID : " + spEntityID);
        }

        if (needToVerify) {
            boolean valid = false;
            if (rmethod.equals("POST")) {
                valid = LogoutUtil.verifySLOResponse(logoutRes, realm,
                    spEntityID, idpEntityID, SAML2Constants.IDP_ROLE);

            } else {
                String queryString = request.getQueryString();
                valid = SAML2Utils.verifyQueryString(queryString, realm,
                    SAML2Constants.IDP_ROLE, spEntityID);
            }
            if (!valid) {
                debug.error("Invalid signature in SLO Response.");
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidSignInResponse"));
            }
            IDPSSODescriptorElement idpsso =
                sm.getIDPSSODescriptor(realm, idpEntityID);
            String loc = null;
            if (idpsso != null) {
                List sloList = idpsso.getSingleLogoutService();
                if (sloList != null && !sloList.isEmpty()) {
                     loc = LogoutUtil.getSLOResponseServiceLocation(
                          sloList, binding);
                    if (loc == null || (loc.length() == 0)) {
                        loc = LogoutUtil.getSLOServiceLocation(
                             sloList, binding);
                    }
                }
            }
            if (!SAML2Utils.verifyDestination(logoutRes.getDestination(),
                loc)) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidDestination"));
            }
        }

        boolean doRelayState = processLogoutResponse(request, response,
            logoutRes, relayState, metaAlias, idpEntityID, spEntityID, realm,
            binding);

        // IDPProxy
        Map logoutResponseMap = (Map)IDPCache.logoutResponseCache.get(
            requestId);
        if ((logoutResponseMap != null) && (!logoutResponseMap.isEmpty())) {
            LogoutResponse logoutResp = (LogoutResponse)
                logoutResponseMap.get("LogoutResponse");
            String location = (String) logoutResponseMap.get("Location");
            String spEntity = (String) logoutResponseMap.get("spEntityID");
            String idpEntity = (String) logoutResponseMap.get("idpEntityID");
            if (logoutResp != null && location != null &&
                spEntity != null && idpEntity !=null) {
                LogoutUtil.sendSLOResponse(response, request, logoutResp, location,
                    relayState, "/", spEntity, SAML2Constants.SP_ROLE,
                    idpEntity, binding);
                return true;
            }
        }

        return doRelayState;
    }

    static boolean processLogoutResponse(HttpServletRequest request,
        HttpServletResponse response, LogoutResponse logoutRes,
        String relayState, String metaAlias, String idpEntityID,
        String spEntityID, String realm, String binding)
        throws SAML2Exception, SessionException {

        // use the cache to figure out which session index is in question
        // and then use the cache to see if any more SPs to send logout request
        // if yes, send one
        // if no, do local logout and send response back to original requesting
        // SP (this SP name should be remembered in cache)

        Object session = sessionProvider.getSession(request);
        String tokenID = sessionProvider.getSessionID(session);

        String idpSessionIndex = IDPSSOUtil.getSessionIndex(session);

        if (idpSessionIndex == null) {
            if (debug.messageEnabled()) {
                debug.message("No SP session participant(s)");
            }
            MultiProtocolUtils.invalidateSession(session, request,
                response, SingleLogoutManager.SAML2);
            return false;
        }

        IDPSession idpSession = IDPCache.idpSessionsByIndices.get(idpSessionIndex);

        if (idpSession == null) {
            if (debug.messageEnabled()) {
                debug.message("IDPSLO.processLogoutResponse : "
                    + "IDP Session with session index "
                    + idpSessionIndex + " already removed.");
            }
            try {
                if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                    SAML2FailoverUtils.deleteSAML2Token(idpSessionIndex);
                 }
            } catch (SAML2TokenRepositoryException se) {
                debug.error("IDPSingleLogout.processLogoutRequest: Error while deleting token from " +
                        "SAML2 Token Repository for idpSessionIndex:" + idpSessionIndex, se);
            }
            IDPCache.authnContextCache.remove(idpSessionIndex);
            MultiProtocolUtils.invalidateSession(session, request,
                response, SingleLogoutManager.SAML2);
            return false;
        }

        if (debug.messageEnabled()) {
            debug.message("idpSessionIndex=" + idpSessionIndex);
        }

        List<NameIDandSPpair> list = idpSession.getNameIDandSPpairs();
        debug.message("idpSession.getNameIDandSPpairs()=" + list);

        if (list.isEmpty()) {
            return sendLastResponse(idpSession, logoutRes, request, response, idpSessionIndex, session, realm,
                    idpEntityID, relayState);
        } else {
            // send Next Requests
            Iterator<NameIDandSPpair> it = list.iterator();
            while (it.hasNext()) {
                NameIDandSPpair pair = it.next();
                it.remove();
                spEntityID = pair.getSPEntityID();
                removeTransientNameIDFromCache(pair.getNameID());

                Map paramsMap = new HashMap(request.getParameterMap());
                paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);

                List<SingleLogoutServiceElement> slosList = getSPSLOServiceEndpoints(realm, spEntityID);
                List extensionsList = LogoutUtil.getExtensionsList(request.getParameterMap());
                SPSSOConfigElement spConfig = sm.getSPSSOConfig(realm, spEntityID);
                //When processing a logout response we must ensure that we try to use the original logout request
                //binding to make sure asynchronous bindings have precedence over synchronous bindings.
                SingleLogoutServiceElement logoutEndpoint = LogoutUtil.getMostAppropriateSLOServiceLocation(slosList,
                        idpSession.getOriginatingLogoutRequestBinding());
                if (logoutEndpoint == null) {
                    continue;
                }
                StringBuffer requestID = LogoutUtil.doLogout(metaAlias, spEntityID, extensionsList, logoutEndpoint,
                        relayState, idpSessionIndex, pair.getNameID(), request, response, paramsMap, spConfig);
                String bindingUsed = logoutEndpoint.getBinding();
                if (bindingUsed.equals(SAML2Constants.HTTP_REDIRECT) || bindingUsed.equals(SAML2Constants.HTTP_POST)) {
                    String requestIDStr = requestID.toString();
                    if (debug.messageEnabled()) {
                        debug.message("IDPSingleLogout.processLogoutRequest: requestIDStr = " + requestIDStr
                                + "\nbinding = " + bindingUsed);
                    }

                    if (requestIDStr != null && requestIDStr.length() != 0) {
                        idpSession.setPendingLogoutRequestID(requestIDStr);
                    }

                    return true;
                }
            }
            //seems like there were only SOAP endpoints left for SPs, so now we should just send back the logout
            //response.
            return sendLastResponse(idpSession, logoutRes, request, response, idpSessionIndex, session, realm,
                    idpEntityID, relayState);
        }
    }

    /**
     * Gets and processes the Single <code>LogoutRequest</code> from SP
     * and return <code>LogoutResponse</code>.
     *
     * @param logoutReq <code>LogoutRequest</code> from SP
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param binding name of binding will be used for request processing.
     * @param relayState the relay state.
     * @param idpEntityID name of host entity ID.
     * @param realm name of host entity.
     * @param isVerified true if the request is verified already.
     * @return LogoutResponse the target URL on successful
     * <code>LogoutRequest</code>.
     * @throws SAML2Exception if error processing
     *          <code>LogoutRequest</code>.
     */
    public static LogoutResponse processLogoutRequest(
        LogoutRequest logoutReq,
        HttpServletRequest request,
        HttpServletResponse response,
        String binding,
        String relayState,
        String idpEntityID,
        String realm, boolean isVerified) throws SAML2Exception {

        Status status = null;
        String spEntity = logoutReq.getIssuer().getValue();
        Object session = null;
        String tmpStr = request.getParameter("isLBReq");
        boolean isLBReq = (tmpStr == null || !tmpStr.equals("false"));

        try {
            do {
                String requestId = logoutReq.getID();
                SAML2Utils.verifyRequestIssuer(
                         realm, idpEntityID, logoutReq.getIssuer(), requestId);

                List siList = logoutReq.getSessionIndex();
                if(siList == null) {
                    debug.error("IDPSingleLogout.processLogoutRequest: " +
                        "session index are null in logout request");
                    status =
                        SAML2Utils.generateStatus(SAML2Constants.REQUESTER, "");
                    break;
                }
                int numSI = siList.size();
                // TODO : handle list of session index
                Iterator siIter = siList.iterator();
                String sessionIndex = null;
                if (siIter.hasNext()) {
                    sessionIndex = (String)siIter.next();
                }

                if (debug.messageEnabled()) {
                    debug.message("IDPLogoutUtil.processLogoutRequest: " +
                        "idpEntityID=" + idpEntityID + ", sessionIndex="
                        + sessionIndex);
                }

                if (sessionIndex == null) {
                    // this case won't happen
                    // according to the spec: SP has to send at least
                    // one sessionIndex, could be multiple (TODO: need
                    // to handle that above; but when IDP sends out
                    // logout request, it could omit sessionIndex list,
                    // which means all sessions on SP side, so SP side
                    // needs to care about this case
                    debug.error("IDPLogoutUtil.processLogoutRequest: " +
                        "No session index in logout request");

                    status =
                        SAML2Utils.generateStatus(SAML2Constants.REQUESTER, "");
                    break;
                }

                String remoteServiceURL = null;
                if(isLBReq) {
                   // server id is the last two digit of the session index
                   String serverId =
                       sessionIndex.substring(sessionIndex.length() - 2);
                   if (debug.messageEnabled()) {
                       debug.message("IDPSingleLogout.processLogoutRequest: " +
                           "sessionIndex=" + sessionIndex +", id=" + serverId);
                   }
                   // find out remote serice URL based on server id
                   remoteServiceURL = SAML2Utils.getRemoteServiceURL(serverId);
                }

                IDPSession idpSession = IDPCache.idpSessionsByIndices.get(sessionIndex);

                if (idpSession == null && SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                    // Read from SAML2 Token Repository
                    IDPSessionCopy idpSessionCopy = null;
                    try {
                        idpSessionCopy = (IDPSessionCopy) SAML2FailoverUtils.retrieveSAML2Token(sessionIndex);
                    } catch (SAML2TokenRepositoryException se) {
                        debug.error("IDPSingleLogout.processLogoutRequest: Error while deleting token from " +
                                "SAML2 Token Repository for sessionIndex:" + sessionIndex, se);
                    }
                    // Copy back to IDPSession
                    if (idpSessionCopy != null) {
                        idpSession = new IDPSession(idpSessionCopy);
                    } else {
                        SAML2Utils.debug.error("IDPSessionCopy is NULL!!!");
                    }
                }
                if (idpSession == null) {
                    // If the IDP session does not find locally for a given
                    // session index and if the IDP is behind a lb with another
                    // peer then we have to route the request.
                    if (remoteServiceURL != null) {
                       boolean peerError = false;
                       String remoteLogoutURL = remoteServiceURL +
                           SAML2Utils.removeDeployUri(request.getRequestURI());
                       String queryString = request.getQueryString();
                       if(queryString == null) {
                          remoteLogoutURL = remoteLogoutURL + "?isLBReq=false";
                       } else {
                          remoteLogoutURL = remoteLogoutURL + "?" +
                              queryString + "&isLBReq=false";
                       }
                       LogoutResponse logoutRes =
                           LogoutUtil.forwardToRemoteServer(
                           logoutReq, remoteLogoutURL);
                       if ((logoutRes != null) &&
                           !isNameNotFound(logoutRes)) {
                           if ((isSuccess(logoutRes)) && (numSI > 0)) {
                              siList =
                                  LogoutUtil.getSessionIndex(logoutRes);
                              if(siList == null || siList.isEmpty()) {
                                 peerError = false;
                                 break;
                              }
                           }
                       } else {
                           peerError = true;
                       }
                       if (peerError ||
                           (siList != null && siList.size() > 0)) {
                           status = PARTIAL_LOGOUT_STATUS;
                           break;
                       } else {
                           status = SUCCESS_STATUS;
                           break;
                       }
                    } else {
                        debug.error("IDPLogoutUtil.processLogoutRequest: " +
                        "IDP no longer has this session index "+ sessionIndex);
                        status = SAML2Utils.generateStatus(
                            SAML2Constants.RESPONDER,
                            SAML2Utils.bundle.getString("invalidSessionIndex"));
                        break;
                    }
                } else {
                    // If the user session exists in this IDP then verify the
                    // signature.
                    if (!isVerified &&
                        !LogoutUtil.verifySLORequest(logoutReq, realm,
                        logoutReq.getIssuer().getValue(),
                        idpEntityID, SAML2Constants.IDP_ROLE)) {
                        throw new SAML2Exception(
                           SAML2Utils.bundle.getString("invalidSignInRequest"));
                    }
                }

                session = idpSession.getSession();
                // handle external application logout if configured
                BaseConfigType idpConfig = SAML2Utils.getSAML2MetaManager()
                    .getIDPSSOConfig(realm, idpEntityID);
                List appLogoutURL = (List) SAML2MetaUtils.getAttributes(
                    idpConfig).get(SAML2Constants.APP_LOGOUT_URL);
                if (debug.messageEnabled()) {
                    debug.message("IDPLogoutUtil.processLogoutRequest: " +
                        "external app logout URL= " + appLogoutURL);
                }
                if ((appLogoutURL != null) && (appLogoutURL.size() != 0)) {
                    SAML2Utils.postToAppLogout(request,
                        (String) appLogoutURL.get(0), session);
                }

                List<NameIDandSPpair> list = idpSession.getNameIDandSPpairs();
                int n = list.size();
                if (debug.messageEnabled()) {
                    debug.message("IDPLogoutUtil.processLogoutRequest: " +
                        "NameIDandSPpair for " + sessionIndex + " is " + list +
                        ", size=" + n);
                }


                NameIDandSPpair pair = null;
                // remove sending SP from the list
                String spIssuer = logoutReq.getIssuer().getValue();
                for (int i=0; i<n; i++) {
                    pair = list.get(i);
                    if (pair.getSPEntityID().equals(spIssuer)) {
                        list.remove(i);
                        removeTransientNameIDFromCache(pair.getNameID());
                        break;
                    }
                }
                List partners = idpSession.getSessionPartners();
                boolean cleanUp = true;
                if (partners != null && !partners.isEmpty()) {
                    cleanUp = false;
                }

                n = list.size();
                if (n == 0) {
                    // this is the case where there is no other
                    // session participant
                    status = destroyTokenAndGenerateStatus(
                        sessionIndex, idpSession.getSession(),
                        request, response, cleanUp);
                    if (cleanUp) {
                       IDPCache.idpSessionsByIndices.remove(sessionIndex);
                       if ((agent != null) &&
                           agent.isRunning() &&
                           (saml2Svc != null)) {
                           saml2Svc.setIdpSessionCount( (long)IDPCache.idpSessionsByIndices.size() );
                       }
                       if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                           try {
                                SAML2FailoverUtils.deleteSAML2Token(sessionIndex);
                           } catch (SAML2TokenRepositoryException se) {
                               debug.error("IDPSingleLogout.processLogoutRequest: Error while deleting token from " +
                                       "SAML2 Token Repository for sessionIndex:" + sessionIndex, se);
                           }
                       }
                       IDPCache.authnContextCache.remove(sessionIndex);
                    }
                    break;
                }

                //We should save the originally used request binding to make sure the response is sent back using the
                //correct binding.
                idpSession.setOriginatingLogoutRequestBinding(binding);
                // there are other SPs to be logged out
                if (binding.equals(SAML2Constants.HTTP_REDIRECT) ||
                    binding.equals(SAML2Constants.HTTP_POST)) {
                    idpSession.setOriginatingLogoutRequestID(logoutReq.getID());
                    idpSession.setOriginatingLogoutSPEntityID(
                                          logoutReq.getIssuer().getValue());
                }

                int soapFailCount = 0;
                for (int i = 0; i < n; i++) {
                    pair = list.remove(0);
                    removeTransientNameIDFromCache(pair.getNameID());

                    String spEntityID = pair.getSPEntityID();
                    if (debug.messageEnabled()) {
                        debug.message("IDPSingleLogout.processLogoutRequest: SP for " + sessionIndex + " is "
                                + spEntityID);
                    }
                    List<SingleLogoutServiceElement> slosList = getSPSLOServiceEndpoints(realm, spEntityID);

                    // get IDP entity config in case of SOAP,for basic auth info
                    SPSSOConfigElement spConfig = null;
                    spConfig = SAML2Utils.getSAML2MetaManager().getSPSSOConfig(realm, spEntityID);
                    String uri = request.getRequestURI();
                    String metaAlias = SAML2MetaUtils.getMetaAliasByUri(uri);
                    HashMap paramsMap = new HashMap();
                    paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);
                    StringBuffer requestID = null;
                    SingleLogoutServiceElement logoutEndpoint =
                            LogoutUtil.getMostAppropriateSLOServiceLocation(slosList,
                            idpSession.getOriginatingLogoutRequestBinding());
                    if (logoutEndpoint == null) {
                        continue;
                    }
                    try {
                        requestID = LogoutUtil.doLogout(metaAlias, spEntityID, null, logoutEndpoint, relayState,
                                sessionIndex, pair.getNameID(), request, response, paramsMap, spConfig);
                    } catch (SAML2Exception ex) {
                        if (logoutEndpoint.getBinding().equals(SAML2Constants.SOAP)) {
                            debug.error(
                                "IDPSingleLogout.initiateLogoutRequest:" , ex);
                            soapFailCount++;
                            continue;
                        } else {
                            throw ex;
                        }
                    }

                    String bindingUsed = logoutEndpoint.getBinding();
                    if (bindingUsed.equals(SAML2Constants.HTTP_REDIRECT) ||
                            bindingUsed.equals(SAML2Constants.HTTP_POST)) {
                        String requestIDStr = requestID.toString();
                        if (requestIDStr != null && requestIDStr.length() != 0) {
                            idpSession.setPendingLogoutRequestID(requestIDStr);
                        }
                        return null;
                    }
                }

                if (soapFailCount == n) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString("sloFailed"));
                } else if (soapFailCount > 0) {
                    throw new SAML2Exception(SAML2Utils.bundle.getString("partialLogout"));
                }
                spEntity = idpSession.getOriginatingLogoutSPEntityID();
                if (binding.equals(SAML2Constants.HTTP_REDIRECT) || binding.equals(SAML2Constants.HTTP_POST)) {
                    sendLastResponse(idpSession, null, request, response, sessionIndex, session, realm, idpEntityID,
                            relayState);
                    return null;
                } else {
                    // binding is SOAP, generate logout response
                    // and send to initiating SP
                    status = destroyTokenAndGenerateStatus(
                        sessionIndex, idpSession.getSession(),
                        request, response, true);
                    if (cleanUp) {
                        IDPCache.idpSessionsByIndices.remove(sessionIndex);
                        if ((agent != null) &&
                            agent.isRunning() &&
                            (saml2Svc != null))
                        {
                            saml2Svc.setIdpSessionCount(
                                (long)IDPCache.idpSessionsByIndices.
                                    size());
                        }
                        if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                            try {
                                SAML2FailoverUtils.deleteSAML2Token(sessionIndex);
                            } catch (SAML2TokenRepositoryException se) {
                                debug.error("IDPSingleLogout.processLogoutRequest: Error while deleting token from " +
                                        "SAML2 Token Repository for sessionIndex:" + sessionIndex, se);
                            }
                        }
                        IDPCache.authnContextCache.remove(sessionIndex);
                    }
                }
            } while (false);

        } catch (SessionException ssoe) {
            debug.error("IDPSingleLogout.processLogoutRequest: unable to get meta for ", ssoe);
            status = SAML2Utils.generateStatus(idpEntityID, ssoe.toString());
        } catch (SAML2Exception e) {
             // show throw exception
             e.printStackTrace();
             SAML2Utils.debug.error("DB ERROR!!!");
        }
        // process multi-federation protocol
        boolean isMultiProtocol = false;
        try {
            SessionProvider provider = SessionManager.getProvider();
            if ((session != null) && (provider.isValid(session)) &&
                MultiProtocolUtils.isMultipleProtocolSession(session,
                SingleLogoutManager.SAML2)) {
                isMultiProtocol = true;
            }
        } catch (SessionException ex) {
            //ignore
        }
        //here we are providing null for remote entity, because it's an unused variable in the method...
        LogoutResponse logRes = LogoutUtil.generateResponse(status, logoutReq.getID(),
                SAML2Utils.createIssuer(idpEntityID), realm, SAML2Constants.IDP_ROLE, null);
        if (!isMultiProtocol) {
            return logRes;
        } else {
            try {
                Set set = new HashSet();
                set.add(session);
                String sessUser =
                    SessionManager.getProvider().getPrincipalName(session);
                boolean isSOAPInitiated = binding.equals(SAML2Constants.SOAP);
                SingleLogoutServiceElement endpoint = getLogoutResponseEndpoint(realm, spEntity, binding);
                String location = getResponseLocation(endpoint);
                logRes.setDestination(XMLUtils.escapeSpecialCharacters(location));
                debug.message("IDPSingleLogout.processLogReq : call MP");
                int retStat = SingleLogoutManager.getInstance().doIDPSingleLogout(set, sessUser, request, response,
                    isSOAPInitiated, false, SingleLogoutManager.SAML2, realm, idpEntityID, spEntity, relayState,
                    logoutReq.toXMLString(true, true), logRes.toXMLString(true, true),
                    SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS);
                if (retStat != SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                    logRes = updateLogoutResponse(logRes, retStat);
                    return logRes;
                } else {
                    return null;
                }
            } catch (SessionException ex) {
                debug.error("IDPSingleLogout.ProcessLogoutRequest: SP " +
                    "initiated SOAP logout", ex);
                throw new SAML2Exception(ex.getMessage());
            } catch (Exception ex) {
                debug.error("IDPSingleLogout.ProcessLogoutRequest: SP " +
                    "initiated SOAP logout (MP)", ex);
                throw new SAML2Exception(ex.getMessage());
            }
        }
    }

    private static LogoutResponse updateLogoutResponse(LogoutResponse logRes,
        int retStat) throws SAML2Exception {
        if (debug.messageEnabled()) {
            debug.message("IDPSingleLogout.updateLogoutResponse: response=" +
                logRes.toXMLString() + "\nstatus = " + retStat);
        }
        if (retStat == SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS) {
            return logRes;
        } else {
            StatusCode code = logRes.getStatus().getStatusCode();
            if (code.getValue().equals(SAML2Constants.SUCCESS)) {
                code.setValue(SAML2Constants.RESPONDER);
            }
            return logRes;
        }
    }

    /**
     * Destroys the Single SignOn token and generates
     * the <code>Status</code>.
     *
     * @param sessionIndex IDP's session index.
     * @param session the Single Sign On session.
     *
     * @return <code>Status</code>.
     * @throws SAML2Exception if error generating
     *          <code>Status</code>.
     */
    private static Status destroyTokenAndGenerateStatus(
        String sessionIndex,
        Object session,
        HttpServletRequest request,
        HttpServletResponse response,
        boolean cleanUp) throws SAML2Exception {

        Status status = null;
        if (session != null) {
            try {
                if (cleanUp) {
                    MultiProtocolUtils.invalidateSession(session, request,
                        response, SingleLogoutManager.SAML2);
                }

                if (debug.messageEnabled()) {
                    debug.message("IDPLogoutUtil.destroyTAGR: "
                        + "Local session destroyed.");
                }
                status = SAML2Utils.generateStatus(SAML2Constants.SUCCESS, "");
            } catch (Exception e) {
                debug.error("IDPLogoutUtil.destroyTAGR: ", e);
                status = SAML2Utils.generateStatus(SAML2Constants.RESPONDER,"");
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("IDPLogoutUtil.destroyTAGR: " +
                    "No such session with index " + sessionIndex + " exists.");
            }
            // TODO : should this be success?
            status = SAML2Utils.generateStatus(SAML2Constants.SUCCESS, "");
        }
        return status;
    }

    private static void destroyAllTokenForUser(
        String  userToLogout, HttpServletRequest request,
        HttpServletResponse response) {

        Enumeration keys = IDPCache.idpSessionsByIndices.keys();
        String idpSessionIndex = null;
        IDPSession idpSession = null;
        Object idpToken = null;
        if (debug.messageEnabled()) {
                debug.message("IDPSingleLogout.destroyAllTokenForUser: " +
                    "User to logoutAll : " + userToLogout);
        }

        while (keys.hasMoreElements()) {
            idpSessionIndex = (String)keys.nextElement();
            idpSession = IDPCache.idpSessionsByIndices.get(idpSessionIndex);
            if (idpSession != null) {
                idpToken = idpSession.getSession();
                if (idpToken != null) {
                    try {
                        String userID = sessionProvider.getPrincipalName(idpToken);
                        if (userToLogout.equalsIgnoreCase(userID)) {
                            MultiProtocolUtils.invalidateSession(idpToken,
                                request, response, SingleLogoutManager.SAML2);
                            IDPCache.
                                idpSessionsByIndices.remove(idpSessionIndex);
                            IDPCache.authnContextCache.remove(idpSessionIndex);
                            if ((agent != null) &&
                                agent.isRunning() &&
                                (saml2Svc != null))
                            {
                                saml2Svc.setIdpSessionCount(
		                    (long)IDPCache.
					idpSessionsByIndices.size());
                            }
                        }
                    } catch (SessionException e) {
                        debug.error(
                            SAML2Utils.bundle.getString("invalidSSOToken"), e);
                        continue;
                    }
                }
            } else {
                IDPCache.idpSessionsByIndices.remove(idpSessionIndex);
                if ((agent != null) && agent.isRunning() && (saml2Svc != null)){
                    saml2Svc.setIdpSessionCount(
		        (long)IDPCache.idpSessionsByIndices.size());
                }
                try {
                    if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                        SAML2FailoverUtils.deleteSAML2Token(idpSessionIndex);
                    }
                } catch (SAML2TokenRepositoryException se) {
                    debug.error("IDPSingleLogout.destroyAllTokenForUser: Error while deleting token from " +
                            "SAML2 Token Repository for idpSessionIndex:" + idpSessionIndex, se);
                }
                IDPCache.authnContextCache.remove(idpSessionIndex);
            }
        }
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

   /**
     * Removes transient nameid from the cache.
     */
    private static void removeTransientNameIDFromCache(NameID nameID) {
        if(nameID == null) {
           return;
        }
        if(SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(
               nameID.getFormat())) {
           String nameIDValue = nameID.getValue();
           if(IDPCache.userIDByTransientNameIDValue.containsKey(
              nameIDValue)) {
              IDPCache.userIDByTransientNameIDValue.remove(
              nameIDValue);
           }
        }
    }

     /**
     * Checks if a SAML2 request has been misrouted, if so, send the
     * request to  the original server, gets the response and redirects it
     * or posts it back
     *
     * @param request the Servlet request
     * @param response the Servlet response
     * @param out the print writer for writing out presentation
     * @param session the Single Sign On session.
     *
     * @return true if the request was misrouted and it was forwarded to
     * the original server
     * @throws SAML2Exception, SessionException
     */
    private static boolean isMisroutedRequest(HttpServletRequest request,
            HttpServletResponse response, PrintWriter out, Object session)
            throws SAML2Exception, SessionException {

        String classMethod = "IDPSingleLogout.isMisroutedRequest : ";

        // Check that the request has not been missrouted
        String idpSessionIndex = IDPSSOUtil.getSessionIndex(session);
        if (idpSessionIndex == null) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "No SP session participant(s)");
            }
            MultiProtocolUtils.invalidateSession(session, request,
                response, SingleLogoutManager.SAML2);
            return true;
        }
        String serverId =
                idpSessionIndex.substring(idpSessionIndex.length() - 2);
        if (debug.messageEnabled()) {
            debug.message(classMethod + "idpSessionIndex=" + idpSessionIndex +
                    ", id=" + serverId);
        }

        // If misrouted, route it to the proper server
        if (!serverId.equals(SAML2Utils.getLocalServerID())) {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "SLO request is mis-routed, we are "
                        + SAML2Utils.getLocalServerID()
                        + " and request is owned by " + serverId);
            }
            String remoteServiceURL = SAML2Utils.getRemoteServiceURL(serverId);
            String remoteLogoutURL = remoteServiceURL
                    + SAML2Utils.removeDeployUri(request.getRequestURI());
            String queryString = request.getQueryString();
            if (queryString != null) {
                remoteLogoutURL = remoteLogoutURL + QUESTION_MARK + queryString;
            }
            HashMap remoteRequestData =
                    SAML2Utils.sendRequestToOrigServer(request, response, remoteLogoutURL);
            String redirect_url = null;
            String output_data = null;
            if (remoteRequestData != null && !remoteRequestData.isEmpty()) {
                redirect_url = (String) remoteRequestData.get(SAML2Constants.AM_REDIRECT_URL);
                output_data = (String) remoteRequestData.get(SAML2Constants.OUTPUT_DATA);
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod + "redirect_url : " + redirect_url);
                debug.message(classMethod + "output_data : " + output_data);
            }
            // if we have a redirect then let the JSP do the redirect
            if ((redirect_url != null) && !redirect_url.equals("")) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Redirecting the response, "
                            + "redirect actioned by the JSP");
                }
                try {
                    response.sendRedirect(redirect_url);
                } catch (IOException ex) {
                    debug.error(classMethod + "Error when redirecting", ex);
                }
                return true;
            }
            // no redirect, perhaps an error page, return the content
            if ((output_data != null) && (!output_data.equals(""))) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Printing the forwarded response");
                }
                response.setContentType("text/html; charset=UTF-8");
                out.println(output_data);
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a new Logout Response with Success Status saying that the user has already logged out.
     *
     * @param response The Servlet response.
     * @param logoutReq The SAML 2.0 Logout Request.
     * @param relayState The original relay state that came with the request.
     * @param realm The realm where the hosted entity has been defined.
     * @param idpEntityID The entity id of the hosted IdP.
     * @param spEntityID The entity id of the remote SP.
     * @param binding The binding that the IdP should reply with to the SP.
     *
     * @throws SAML2Exception If there was a problem while constructing/sending the Logout Response.
     */
    private static void sendAlreadyLogedOutResp(HttpServletResponse response, HttpServletRequest request,
            LogoutRequest logoutReq, String relayState,
            String realm, String idpEntityID, String spEntityID,
            String binding) throws SAML2Exception {

        String classMethod = "IDPSingleLogout.sendAlreadyLogedOutResp";
        debug.message(classMethod + "No session in the IdP. "
                + "We are already logged out. Generating success logout");
        LogoutResponse logRes = LogoutUtil.generateResponse(ALREADY_LOGGEDOUT,
                logoutReq.getID(), SAML2Utils.createIssuer(idpEntityID),
                realm, SAML2Constants.IDP_ROLE,
                logoutReq.getIssuer().getSPProvidedID());
        SingleLogoutServiceElement endpoint = getLogoutResponseEndpoint(realm, spEntityID, binding);
        binding = endpoint.getBinding();
        String location = getResponseLocation(endpoint);
        debug.message(classMethod + "Location found: " + location + " for binding " + binding);
        logRes.setDestination(XMLUtils.escapeSpecialCharacters(location));
        LogoutUtil.sendSLOResponse(response, request, logRes, location,
                relayState, realm, idpEntityID, SAML2Constants.IDP_ROLE,
                spEntityID, binding);
    }

    private static boolean sendLastResponse(IDPSession idpSession, LogoutResponse logoutRes, HttpServletRequest request,
            HttpServletResponse response, String idpSessionIndex, Object session, String realm, String idpEntityID,
            String relayState) throws SAML2Exception, SessionException, SAML2MetaException {
        String binding;
        //resetting the binding to the original value so the response is sent back with the correct binding
        binding = idpSession.getOriginatingLogoutRequestBinding();
        String originatingRequestID = idpSession.getOriginatingLogoutRequestID();
        String originatingLogoutSPEntityID = idpSession.getOriginatingLogoutSPEntityID();
        if (originatingRequestID == null) {
            // this is IDP initiated SLO
            if (idpSession.getLogoutAll()) {
                String userID = sessionProvider.getPrincipalName(idpSession.getSession());
                destroyAllTokenForUser(userID, request, response);
            } else {
                IDPCache.idpSessionsByIndices.remove(idpSessionIndex);
                if (agent != null && agent.isRunning() && saml2Svc != null) {
                    saml2Svc.setIdpSessionCount((long) IDPCache.idpSessionsByIndices.size());
                }
                try {
                    if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                        SAML2FailoverUtils.deleteSAML2Token(idpSessionIndex);
                    }
                } catch (SAML2TokenRepositoryException se) {
                    debug.error("IDPSingleLogout.sendLastResponse: Error while deleting token from " +
                            "SAML2 Token Repository for idpSessionIndex:" + idpSessionIndex, se);
                }
                IDPCache.authnContextCache.remove(idpSessionIndex);
                if (!MultiProtocolUtils.isMultipleProtocolSession(idpSession.getSession(), SingleLogoutManager.SAML2)) {
                    sessionProvider.invalidateSession(idpSession.getSession(), request, response);
                } else {
                    MultiProtocolUtils.removeFederationProtocol(idpSession.getSession(), SingleLogoutManager.SAML2);
                    // call Multi-Federation protocol SingleLogoutManager
                    SingleLogoutManager sloManager = SingleLogoutManager.getInstance();
                    Set<Object> set = new HashSet<Object>(1);
                    set.add(session);
                    SessionProvider provider = SessionManager.getProvider();
                    String uid = provider.getPrincipalName(session);
                    debug.message("IDPSingleLogout.sendLastResponse: MP/Http");
                    int retStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
                    try {
                        retStatus = sloManager.doIDPSingleLogout(set, uid, request, response, false, true,
                                SingleLogoutManager.SAML2, realm, idpEntityID, originatingLogoutSPEntityID, relayState,
                                null, null, getLogoutStatus(logoutRes));
                    } catch (SAML2Exception ex) {
                        throw ex;
                    } catch (Exception ex) {
                        debug.error("IDPSIngleLogout.sendLastResponse: MP/IDP initiated HTTP", ex);
                        throw new SAML2Exception(ex.getMessage());
                    }
                    if (retStatus == SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                        return true;
                    }
                }
            }
            debug.message("IDP initiated SLO Success");
            return false;
        }
        List<SingleLogoutServiceElement> slosList = getSPSLOServiceEndpoints(realm, originatingLogoutSPEntityID);
        String location = LogoutUtil.getSLOResponseServiceLocation(slosList, binding);
        if (location == null || location.isEmpty()) {
            location = LogoutUtil.getSLOServiceLocation(slosList, binding);
            if (location == null || location.length() == 0) {
                debug.error("Unable to find the IDP's single logout response service with the HTTP-Redirect binding");
                throw new SAML2Exception(SAML2Utils.bundle.getString("sloResponseServiceLocationNotfound"));
            } else {
                if (debug.messageEnabled()) {
                    debug.message("SP's single logout response service location = " + location);
                }
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("IDP's single logout response service location = " + location);
            }
        }
        Status status = destroyTokenAndGenerateStatus(idpSessionIndex, idpSession.getSession(), request, response,
                true);
        //here we are providing null for remote entity, because it's an unused variable in the method...
        logoutRes = LogoutUtil.generateResponse(status, originatingRequestID, SAML2Utils.createIssuer(idpEntityID),
                realm, SAML2Constants.IDP_ROLE, null);
        if (logoutRes != null) {
            logoutRes.setDestination(XMLUtils.escapeSpecialCharacters(location));
            IDPCache.idpSessionsByIndices.remove(idpSessionIndex);
            if (agent != null && agent.isRunning() && saml2Svc != null) {
                saml2Svc.setIdpSessionCount((long) IDPCache.idpSessionsByIndices.size());
            }
            try {
                if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                    SAML2FailoverUtils.deleteSAML2Token(idpSessionIndex);
                }
            } catch (SAML2TokenRepositoryException se) {
                debug.error("IDPSingleLogout.sendLastResponse: Error while deleting token from " +
                        "SAML2 Token Repository for idpSessionIndex:" + idpSessionIndex, se);
            }
            IDPCache.authnContextCache.remove(idpSessionIndex);

            // call multi-federation protocol processing
            // this is the SP initiated HTTP binding case
            boolean isMultiProtocolSession = false;
            int retStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
            try {
                SessionProvider provider = SessionManager.getProvider();
                session = idpSession.getSession();
                if (session != null && provider.isValid(session)
                        && MultiProtocolUtils.isMultipleProtocolSession(session, SingleLogoutManager.SAML2)) {
                    isMultiProtocolSession = true;
                    // call Multi-Federation protocol SingleLogoutManager
                    SingleLogoutManager sloManager = SingleLogoutManager.getInstance();
                    Set set = new HashSet();
                    set.add(session);
                    String uid = provider.getPrincipalName(session);
                    debug.message("IDPSingleLogout.sendLastResponse: MP/Http");
                    retStatus = sloManager.doIDPSingleLogout(set, uid, request, response, false, true,
                            SingleLogoutManager.SAML2, realm, idpEntityID, originatingLogoutSPEntityID, relayState,
                            null, logoutRes.toXMLString(), getLogoutStatus(logoutRes));
                }
            } catch (SessionException e) {
                // ignore as session might not be valid
                debug.message("IDPSingleLogout.sendLastResponse: session",e);
            } catch (Exception e) {
                debug.message("IDPSingleLogout.sendLastResponse: MP2",e);
                retStatus = SingleLogoutManager.LOGOUT_FAILED_STATUS;
            }

            if (!isMultiProtocolSession || (retStatus != SingleLogoutManager.LOGOUT_REDIRECTED_STATUS)) {
                logoutRes = updateLogoutResponse(logoutRes, retStatus);
                LogoutUtil.sendSLOResponse(response, request, logoutRes, location, relayState, realm, idpEntityID,
                        SAML2Constants.IDP_ROLE, originatingLogoutSPEntityID, binding);
                return true;
            } else {
                return false;
            }
        }
        IDPCache.idpSessionsByIndices.remove(idpSessionIndex);
        if (agent != null && agent.isRunning() && saml2Svc != null) {
            saml2Svc.setIdpSessionCount((long) IDPCache.idpSessionsByIndices.size());
        }
        try {
            if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                SAML2FailoverUtils.deleteSAML2Token(idpSessionIndex);
            }
        } catch (SAML2TokenRepositoryException se) {
            debug.error("IDPSingleLogout.sendLastResponse: Error while deleting token from " +
                    "SAML2 Token Repository for idpSessionIndex:" + idpSessionIndex, se);
        }
        IDPCache.authnContextCache.remove(idpSessionIndex);
        return false;
    }

    /**
     * Gets the single log out end points for the Service Provider.
     *
     * @param realm the realm that the service provider is configured within
     * @param spEntityID the id for the service provider configuration entity
     * @return a list of Single Logout Service elements
     * @throws SAML2Exception if there was a problem retrieving the SP SSO Descriptor Element
     */
    public static List<SingleLogoutServiceElement> getSPSLOServiceEndpoints(
            final String realm,
            final String spEntityID) throws SAML2Exception {
        // get SPSSODescriptor
        SPSSODescriptorElement spsso = sm.getSPSSODescriptor(realm, spEntityID);

        if (spsso == null) {
            String[] data = {spEntityID};
            LogUtil.error(Level.INFO, LogUtil.SP_METADATA_ERROR, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        return spsso.getSingleLogoutService();
    }
}
