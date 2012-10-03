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
 * $Id: WSFederationSingleLogoutHandler.java,v 1.4 2009/10/28 23:58:57 exu Exp $
 *
 */

package com.sun.identity.multiprotocol;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>WSFederationSingleLogoutHandler</code> class is an implementation 
 * of the <code>SingleLogoutHandler</code> for the WS-Federation protocol.
 */
public class WSFederationSingleLogoutHandler implements SingleLogoutHandler {
    
    private Debug debug = null;
    
    /** Creates a new instance of WSFederationSingleLogoutHandler */
    public WSFederationSingleLogoutHandler() {
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
                "WSFederationSingleLogoutHandler.doIDPSingleLogout : start");
        if (!isSessionUsedInWSFed(userSession, userID)) {
            // no session for this protocol
            debug.message("WSFedSingleLogoutHandler.doIDPSLO : no action");
            return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
        }

        if (isSOAPInitiated) {
            debug.message("WSFedSLOHandler.doIDPSLO : SOAP initiated SLO");
            // WS-Federation does not support SOAP based profile
            return SingleLogoutManager.LOGOUT_FAILED_STATUS;
        } else {
            debug.message("WSFedSLOHandler.doIDPSLO : HTTP initiated SLO");
            String metaAlias = findIDPMetaAlias(idpEntityID, spEntityID,
                realm, protocol); 
            String redirectURL = MultiProtocolUtils.geServerBaseURL(request) +
                "/WSFederationServlet/metaAlias" + metaAlias + "?" +
                WSFederationConstants.WA + "=" +  
                WSFederationConstants.WSIGNOUT10 + "&" +
                WSFederationConstants.WREPLY + "=" +
                URLEncoder.encode(relayState, "UTF-8");
            if (debug.messageEnabled()) {
                debug.message("WSFedSLOHandler.doIDPSLO: HTTPinit, redirect to "
                    + redirectURL);
            }
            response.sendRedirect(redirectURL);
            return SingleLogoutManager.LOGOUT_REDIRECTED_STATUS;
        }
    }
    
    /**
     * Returns the WSFed IDP metaAlis which is in the same COT as the initiation
     * IDP and SP. Return null if such WSFed IDP does not exist or exception
     * occurs.
     */
    private String findIDPMetaAlias(String idpEntityID, String spEntityID,
            String realm, String protocol) {
        try {
            WSFederationMetaManager metaManager = 
                WSFederationUtils.getMetaManager();
            List hostedIdps = metaManager.
                getAllHostedIdentityProviderEntities(realm);
            if (debug.messageEnabled()) {
                debug.message("WSFedSingleLogoutHandler.findIDPMetaAlias: "
                        + " all hosted WS-Fed IDPs = " + hostedIdps);
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
                        debug.message("WSFedSLOHandler.findIDPMetaAlias: "
                        + " check COT = " + cotName);
                    }
                    // check if this cot contains the wsfed IDP to be checked 
                    Set providers = cotManager.listCircleOfTrustMember(realm,
                            cotName, SingleLogoutManager.WS_FED);
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
                            "WSFedSingleLogoutHandler.findIDPMetaAlias : " +
                            "found IDP " + idpId + " in COT " + cotName);
                    }
                    IDPSSOConfigElement config =
                        metaManager.getIDPSSOConfig(realm, idpId);
                    return config.getMetaAlias();
                }
            }
        } catch (Exception e) {
            SingleLogoutManager.debug.error("WSFederationSingleLogoutHandler." +
                    "findIDPMetaAlias", e);
        }
        return null;
    }
    
    /**
     * Returns true if the user session is used by WS-Fed protocol, false
     * otherwise
     */
    private boolean isSessionUsedInWSFed(Set userSession, String userId) {
        if ((userSession == null) || userSession.isEmpty()) {
            return false;
        } else {
            Object session = userSession.iterator().next();
            return MultiProtocolUtils.usedInProtocol(session,
                    SingleLogoutManager.WS_FED);
        }
    }
}
