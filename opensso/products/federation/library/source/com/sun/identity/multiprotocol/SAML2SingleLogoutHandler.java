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
 * $Id: SAML2SingleLogoutHandler.java,v 1.6 2008/11/10 22:57:00 veiming Exp $
 *
 */

package com.sun.identity.multiprotocol;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPSession;
import com.sun.identity.saml2.profile.LogoutUtil;
import com.sun.identity.saml2.profile.NameIDandSPpair;
import com.sun.identity.shared.debug.Debug;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SAML2SingleLogoutHander</code> class is an implementation of
 * the <code>SingleLogoutHandler</code> for the Liberty ID-FF protocol.
 */
public class SAML2SingleLogoutHandler implements SingleLogoutHandler {
    
    private Debug debug = null;
    
    /** Creates a new instance of SAML2SingleLogoutHandler */
    public SAML2SingleLogoutHandler() {
        debug = SingleLogoutManager.getInstance().debug;
    }
    
    /**
     * Performs single logout for a specific protocol. This method need to be
     * implemented by each federation protocol, and will be invoked by other
     * protocol to achieve cross federation protocol single logout. The local
     * session shall not be destroyed by the SPI implementation. In cases of
     * IDP proxying configuration, the implementation need to do single
     * logout for the entity acting as both SP and IDP.
     *
     * Normally, there are three types of single logout to be supported:
     * - logout single session (specified by userSession parameter)
     * - logout a list of session (specified by userSession parameter)
     * - logout all sessions for a specific user (specified by userID oarameter)
     *
     * As a single instance of the implementation class will be used internally
     * in the SingleLogoutManager class, implementation of the method shall
     * not maintain any states.
     *
     * @param userSession Set of user session objects (java.lang.Object) to be
     *     logout.
     * @param userID Universal identifier of the user to be logout.
     * @param request HTTP servlet request object of the request.
     * @param response HTTP servlet response object of the request.
     * @param isSOAPInitiated True means original single logout request is
     *     initiated using SOAP binding, false means the original single logout
     *     request is initiated using HTTP binding.
     * @param isIDPInitiated True means this is identity provider initiated
     *     single logout, false means this is service provider initiated single
     *     logout.
     * @param protocol The protocol of the original single logout.
     *     Possible values for this parameter:
     *          <code>SingleLogoutManager.SAML2</code>
     *              - single logout initiated using SAMLv2 protocol
     *          <code>SingleLogoutManager.IDFF</code>
     *              - single logout initiated using ID-FF protocol
     *          <code>SingleLogoutManager.WS-FED</code>
     *              - single logout initiated using WS-Federation protocol
     * @param realm Realm of the hosted entity.
     * @param idpEntityID <code>EntityID</code> of the hosted identity provider
     *      in the original Single Logout request.
     * @param spEntityID <code>EntityID</code> of the remote service provider
     *      in the original Single Logout request.
     * @param relayState A state information to be relayed back in response.
     * @param singleLogoutRequestXML Original single logout request in XML
     *      string.
     * @param singleLogoutResponseXML Logout response to be sent back to SP.
     *      This only apply to the case of SP initiated Single Logout, it will
     *      be null in case of IDP initiated single logout.
     * @param currentStatus Current logout status, this is the accumulative
     *      single logout status for all protocols processed so far.
     *      Possible values:
     *         <code>SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS</code>
     *         <code>SingleLogoutManager.LOGOUT_FAILED_STATUS</code>
     *         <code>SingleLogoutManager.LOGOUT_PARTIAL_STATUS</code>
     * @return the single logout status for this protocol, possible values:
     *         <code>SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS</code>
     *         <code>SingleLogoutManager.LOGOUT_FAILED_STATUS</code>
     *         <code>SingleLogoutManager.LOGOUT_PARTIAL_STATUS</code>
     *         <code>SingleLogoutManager.LOGOUT_REDIRECTED_STATUS</code>
     * @exception Exception if error occurs when processing the protocol.
     */
    public int doIDPSingleLogout(
            Set userSession,
            String userID,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean isSOAPInitiated,
            boolean isIDPInitiated,
            String protocol,
            String realm,
            String idpEntityID,
            String spEntityID,
            String relayState,
            String singleLogoutRequestXML,
            String singleLogoutResponseXML,
            int currentStatus
            ) throws Exception {
        SingleLogoutManager.getInstance().debug.message(
                "SAML2SingleLogoutHandler.doIDPSingleLogout : start");
        if (!isSessionUsedInSAML2(userSession, userID)) {
            // no session for this protocol
            debug.message("SAML2SingleLogoutHander.doIDPSLO : no action");
            return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
        }

        if (isSOAPInitiated) {
            SAML2MetaManager saml2Manager = new SAML2MetaManager();
            String idpMetaAlias = findIDPMetaAlias(idpEntityID, spEntityID,
                    realm, protocol, saml2Manager);
            if (idpMetaAlias == null) {
                // no SAML2 IDP found
                return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
            }
            if (debug.messageEnabled()) {
                debug.message("SAML2SingleLogoutHandler: " +
                    "userID=" + userID + ", session=" + userSession +
                    ", isSOAInited=" + isSOAPInitiated + ", isIDPInited=" +
                    isIDPInitiated + ", protocol=" + protocol + ", relam=" +
                    realm + ", idpEntityID=" + idpEntityID + ", spEntityID=" +
                    spEntityID + ", status=" + currentStatus +
                    "\nlogout Request XML=" + singleLogoutRequestXML +
                    "\nlogout response XML=" + singleLogoutResponseXML);
            }
            String idpEntityId = saml2Manager.getEntityByMetaAlias(idpMetaAlias);
            return handleSOAPInitiatedSingleLogout(userSession, userID,
                    request, response, realm, idpMetaAlias,
                    idpEntityId, relayState, saml2Manager);
        } else {
            debug.message(
                    "SAML2SingleLogoutHandler.doIDPSLO : HTTP initiated SLO");
            String redirectURL = MultiProtocolUtils.geServerBaseURL(request) +
                    "/IDPSloInit?" +  SAML2Constants.BINDING + "=" +
                    SAML2Constants.HTTP_REDIRECT + "&" +
                    SAML2Constants.RELAY_STATE + "=" +
                    URLEncoder.encode(relayState, "UTF-8");
            if (debug.messageEnabled()) {
                debug.message(
                    "SAML2SingleLogoutHandler.doIDPSLO: HTTP init, redirect to "
                    + redirectURL);
            }
            response.sendRedirect(redirectURL);
            return SingleLogoutManager.LOGOUT_REDIRECTED_STATUS;
        }
    }
    
    /**
     * Returns the SAML2 IDP metaAlis which is in the same COT as the initiation
     * IDP and SP. Return null if such SAML2 IDP does not exist or exception
     * occurs.
     */
    private String findIDPMetaAlias(String idpEntityID, String spEntityID,
            String realm, String protocol, SAML2MetaManager saml2Manager) {
        try {
            List hostedIdps =
                    saml2Manager.getAllHostedIdentityProviderEntities(realm);
            if (debug.messageEnabled()) {
                debug.message("SAML2SingleLogoutHandler.findIDPMetaAlias: "
                        + " all hosted SAML2 IDPs = " + hostedIdps);
            }
            if ((hostedIdps == null) || hostedIdps.isEmpty()) {
                return null;
            }
            CircleOfTrustManager cotManager = new CircleOfTrustManager();
            Set cots = cotManager.getAllActiveCirclesOfTrust(realm);
            int num = hostedIdps.size();
            for (int i = 0; i < num; i++) {
                String idpId = (String) hostedIdps.get(i);
                Iterator it = cots.iterator();
                while (it.hasNext()) {
                    String cotName = (String) it.next();
                    if (debug.messageEnabled()) {
                        debug.message("SAML2SLOHandler.findIDPMetaAlias: "
                        + " check COT = " + cotName);
                    }
                    // check if this cot contains all entities
                    Set providers = cotManager.listCircleOfTrustMember(realm,
                            cotName, SingleLogoutManager.SAML2);
                    if ((providers == null) || !providers.contains(idpId)) {
                        continue;
                    }
                    providers = cotManager.listCircleOfTrustMember(realm,
                            cotName, protocol);
                    if ((providers == null) || 
                        !providers.contains(idpEntityID)) {
                        continue;
                    }
                    if ((spEntityID != null) && (spEntityID.length() != 0) &&
                            !providers.contains(spEntityID)) {
                        continue;
                    }
                    // found the matching IDP, might want to find all in future
                    // but just stop here right now.
                    if (SingleLogoutManager.debug.messageEnabled()) {
                        SingleLogoutManager.debug.message(
                                "SAML2SingleLogoutHandler.findIDPMetaAlias : " +
                                "found IDP " + idpId + " in COT " + cotName);
                    }
                    IDPSSOConfigElement config =
                            saml2Manager.getIDPSSOConfig(realm, idpId);
                    return config.getMetaAlias();
                }
            }
        } catch (Exception e) {
            SingleLogoutManager.debug.error("SAML2SingleLogoutHandler." +
                    "findIDPMetaAlias", e);
        }
        return null;
    }
    
    /**
     * Returns true if the user session is used by SAML2 protocol, false
     * otherwise
     */
    private boolean isSessionUsedInSAML2(Set userSession, String userId) {
        // TODO : handle single logout of sessions for a user, this case is not
        //        supported yet in current SAML2 implementation. Need to have
        //        IDPCache to provide a Map for user and session mapping.
        if ((userSession == null) || userSession.isEmpty()) {
            return false;
        } else {
            // TODO : handle multiple user session cases
            Object session = userSession.iterator().next();
            return MultiProtocolUtils.usedInProtocol(session,
                    SingleLogoutManager.SAML2);
        }
    }
    
    private int handleSOAPInitiatedSingleLogout(Set userSession, String userID,
          HttpServletRequest request, HttpServletResponse response,
            String realm, String idpMetaAlias, String idpEntityId,
            String relayState, SAML2MetaManager saml2Manager) 
            throws SAML2Exception, SessionException {
        
        debug.message("SAML2SingleLogoutHanlder: handleSOAPInitiatedSLO");
        // TODO : verify this works under LB        
        Object session = null;
        SessionProvider provider = SessionManager.getProvider();
        if ((userSession != null) && !userSession.isEmpty()) {
            // TODO : handle multiple SSO token case
            session = (Object) userSession.iterator().next();
            if (!provider.isValid(session)) {
                return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
            }
        } else {
            return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
        }
        
        if (debug.messageEnabled()) {
            debug.message("SAML2SLOHandler.handleSOAPSLO: " +
                    "handler session " + session + " for user " + userID);
        }
        
        // get IDP session index from session
        String[] sessIndex = provider.getProperty(session,
                SAML2Constants.IDP_SESSION_INDEX);
        if (debug.messageEnabled()) {
            debug.message("SAML2SLOHandler.handleSOAPSLO: " +
                    "session index = " + sessIndex);
        }
        if ((sessIndex == null) || (sessIndex.length == 0)) {
            if (debug.warningEnabled()) {
                debug.warning("SAML2SLOHandler.handleSOAPSLO: " +
                        "Null session index for " + session);
            }
            return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
        }
        
        IDPSession idpSession = (IDPSession)
        IDPCache.idpSessionsByIndices.get(sessIndex[0]);
        if (idpSession == null) {
            debug.error("SAML2SLOHanlder.handleSOAPSLO: " +
                    "IDP no longer has this session index " + sessIndex[0]);
            return SingleLogoutManager.LOGOUT_FAILED_STATUS;
        }
        
        List list = (List)idpSession.getNameIDandSPpairs();
        int n = list.size();
        if (debug.messageEnabled()) {
            debug.message("SAML2SLOHanlder.handleSOAPSLO: " +
                "NameIDandSPpair for " + sessIndex[0] + " is " + list +
                ", size=" + n);
        }
        NameIDandSPpair pair = null;
        
        int soapFailCount = 0;
        for (int i = 0; i < n; i++) {
            pair = (NameIDandSPpair) list.get(i);
            
            String spEntityID = pair.getSPEntityID();
            if (debug.messageEnabled()) {
                debug.message("SAML2SLOHanlder.handleSOAPSLO: "
                    + "SP for " + sessIndex[0] + " is " + spEntityID);
            }
            SPSSODescriptorElement sp = null;
            sp = SAML2Utils.getSAML2MetaManager().
                    getSPSSODescriptor(realm, spEntityID);
            List slosList = sp.getSingleLogoutService();
            
            // get IDP entity config for basic auth info
            SPSSOConfigElement spConfig = SAML2Utils.
                    getSAML2MetaManager().getSPSSOConfig(realm, spEntityID);
            HashMap paramsMap = new HashMap();
            paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);
            try {
                LogoutUtil.doLogout(idpMetaAlias,
                    spEntityID, slosList, null, SAML2Constants.SOAP,
                    relayState, sessIndex[0], pair.getNameID(), request,
                    response, paramsMap, spConfig);
            } catch (SAML2Exception ex) {
                debug.error("SAML2SLOHandler:handleSOAPSLO.doLogout" , ex);
                soapFailCount++;
                continue;
            }
        }
        
        int retStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
        if (soapFailCount == n) {
            retStatus = SingleLogoutManager.LOGOUT_FAILED_STATUS;
        } else if (soapFailCount > 0) {
            retStatus = SingleLogoutManager.LOGOUT_PARTIAL_STATUS;
        }
        //  invaidate session
        MultiProtocolUtils.invalidateSession(session, request, response,
            SingleLogoutManager.SAML2);
        IDPCache.idpSessionsByIndices.remove(sessIndex[0]);
        IDPCache.authnContextCache.remove(sessIndex[0]);
        if (debug.messageEnabled()) {
            debug.message("SAML2SLOHandler.doSOAPSLO: return status for "  +
                session + " is " + retStatus);
        }
        return retStatus;
    }
}
