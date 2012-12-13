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
 * $Id: IDFFSingleLogoutHandler.java,v 1.6 2008/11/10 22:56:59 veiming Exp $
 *
 */

package com.sun.identity.multiprotocol;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSessionPartner;
import com.sun.identity.federation.services.logout.FSLogoutStatus;
import com.sun.identity.federation.services.logout.FSLogoutUtil;
import com.sun.identity.federation.services.logout.FSSingleLogoutHandler;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>IDFFSingleLogoutHander</code> class is an implementation of
 * the <code>SingleLogoutHandler</code> for the Liberty ID-FF protocol.
 */
public class IDFFSingleLogoutHandler implements SingleLogoutHandler {
    
    /** Creates a new instance of IDFFSingleLogoutHandler */
    public IDFFSingleLogoutHandler() {
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
     *          <code>SingleLogoutManager.WS_FED</code>
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
     *         <code>SingleLogoutManager.LOGOUT_NO_ACTION_STATUS</code>
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
        SingleLogoutManager.debug.message(
                "IDFFSingleLogoutHandler.doIDPSLO : start");
        String idpMetaAlias = findIDPMetaAlias(idpEntityID, spEntityID,
                realm, protocol);
        if (idpMetaAlias == null) {
            // no IDFF IDP found
            return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
        }
        if (SingleLogoutManager.debug.messageEnabled()) {
            SingleLogoutManager.debug.message("IDFFSingleLogoutHandler: " +
                "IDFF idp meta alias=" + idpMetaAlias +
                ", userID=" + userID + ", session=" + userSession + 
                ", isSOAInitiated=" + isSOAPInitiated + ", isIDPInitiated=" +
                isIDPInitiated + ", protocol=" + protocol + ", relam=" +
                realm + ", idpEntityID=" + idpEntityID + ", spEntityID=" +
                spEntityID + ", status=" + currentStatus +
                "\nlogout Request XML=" + singleLogoutRequestXML +
                "\nlogout response XML=" + singleLogoutResponseXML);
        }
        IDFFMetaManager idffManager = new IDFFMetaManager(null);
        String idpEntityId =  idffManager.getEntityIDByMetaAlias(idpMetaAlias);
        if (!FSLogoutUtil.liveConnectionsExist(userID, idpMetaAlias)) {
            // no session for this protocol
            return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
        }
        if (isSOAPInitiated) {
            return handleSOAPInitiatedSingleLogout(userSession, userID,
                request, response, realm, idpMetaAlias, 
                idpEntityId, relayState, idffManager);
        } else {
            SingleLogoutManager.debug.message(
                    "IDFFSingleLogoutHandler.doIDPSLO : HTTP initiated SLO");
            if (!MultiProtocolUtils.usedInProtocol(request,
                    SingleLogoutManager.IDFF)) {
                return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
            }
            String redirectURL = MultiProtocolUtils.geServerBaseURL(request) +
                    "/liberty-logout?" + IFSConstants.META_ALIAS + "=" +
                    idpMetaAlias + "&" + IFSConstants.RELAY_STATE + "=" +
                    URLEncoder.encode(relayState, "UTF-8");
            if (SingleLogoutManager.debug.messageEnabled()) {
                SingleLogoutManager.debug.message(
                    "IDFFSingleLogoutHandler.doIDPSLO : HTTP init, redirect to "
                    + redirectURL);
            }
            response.sendRedirect(redirectURL);
            return SingleLogoutManager.LOGOUT_REDIRECTED_STATUS;
        }
    }
    
    private int handleSOAPInitiatedSingleLogout(Set userSession, String userID,
            HttpServletRequest request, HttpServletResponse response, 
            String realm, String idpMetaAlias, String idpEntityId,
            String relayState, IDFFMetaManager metaManager) throws Exception {
        
        // TODO : verify this works under LB

        Object ssoToken = null;
        if ((userSession != null) && !userSession.isEmpty()) {
            // TODO : handle multiple SSO token case
            ssoToken = (Object) userSession.iterator().next();
        } else {
            FSSessionManager manager = 
                FSSessionManager.getInstance(idpMetaAlias);
            List sessions = manager.getSessionList(userID);
            if ((sessions != null) && !sessions.isEmpty()) {
                // TODO : handle multiple SSO token case
                ssoToken = sessions.iterator().next();
            } else {
                return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
            }
            
        }
        // call Single Logout Handler
        FSUtils.debug.message("creating FSSingleLogoutHandler");
        
        HashMap providerMap = FSLogoutUtil.getCurrentProvider(
            userID, idpEntityId, ssoToken);
        if (providerMap != null) {
            FSSessionPartner currentSessionProvider =
                (FSSessionPartner)providerMap.get(IFSConstants.PARTNER_SESSION);
            String sessionIndex =
                 (String)providerMap.get(IFSConstants.SESSION_INDEX);
            if (currentSessionProvider != null) {
                ProviderDescriptorType hostedProviderDesc =
                    metaManager.getIDPDescriptor(realm, idpEntityId);
                BaseConfigType hostedConfig = 
                    metaManager.getIDPDescriptorConfig(realm, idpEntityId);
                FSSingleLogoutHandler handlerObj = new FSSingleLogoutHandler();
                handlerObj.setHostedDescriptor(hostedProviderDesc);
                handlerObj.setHostedDescriptorConfig(hostedConfig);
                handlerObj.setHostedEntityId(idpEntityId);
                handlerObj.setHostedProviderRole(IFSConstants.IDP);
                handlerObj.setMetaAlias(idpMetaAlias);
                handlerObj.setSingleLogoutProtocol(
                    IFSConstants.LOGOUT_IDP_SOAP_PROFILE);
                handlerObj.setRelayState(relayState);
                handlerObj.setRealm(realm);
                FSLogoutStatus logoutStatus = handlerObj.handleSingleLogout(
                    response, request, currentSessionProvider, userID,
                    sessionIndex, false, ssoToken);
                if (SingleLogoutManager.debug.messageEnabled()) {
                    SingleLogoutManager.debug.message("IDFFSLOHandler." +
                        "handleSOAPInitiatedSLO: logout status=" + 
                        logoutStatus.toString());
                }
                if (logoutStatus.getStatus().equalsIgnoreCase(
                    IFSConstants.SAML_SUCCESS)) {                   
                    return SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
                } else {
                    return SingleLogoutManager.LOGOUT_FAILED_STATUS;
                }
            }
        }

        return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
    }
    
    /**
     * Returns the IDFF IDP metaAlis which is in the same COT as the initiation
     * IDP and SP. Return null if such IDFF IDP does not exist or exception
     * occurs.
     */
    private String findIDPMetaAlias(String idpEntityID, String spEntityID,
            String realm, String protocol) {
        try {
            IDFFMetaManager idffManager = new IDFFMetaManager(null);
            List hostedIdps = idffManager.getAllHostedIdentityProviderIDs(
                realm);
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
                    // check if this cot contains all entities
                    Set providers = cotManager.listCircleOfTrustMember(realm,
                            cotName, SingleLogoutManager.IDFF);
                    if ((providers == null) || !providers.contains(idpId)) {
                        continue;
                    }
                    providers = cotManager.listCircleOfTrustMember(realm,
                            cotName, protocol);
                    if ((providers == null) || 
                        !providers.contains(idpEntityID)) {
                        continue;
                    }
                    if ((spEntityID != null) &&
                            !providers.contains(spEntityID)) {
                        continue;
                    }
                    // found the matching IDP, might want to find all in future
                    // but just stop here right now.
                    if (SingleLogoutManager.debug.messageEnabled()) {
                        SingleLogoutManager.debug.message(
                                "IDFFSingleLogoutHandler.findIDPMetaAlias : " +
                                "found IDP " + idpId + " in COT " + cotName);
                    }
                    IDPDescriptorConfigElement config =
                            idffManager.getIDPDescriptorConfig(realm, idpId);
                    return config.getMetaAlias();
                }
            }
        } catch (Exception e) {
            SingleLogoutManager.debug.error("IDFFSingleLogoutHandler." +
                    "findIDPMetaAlias", e);
        }
        return null;
    }
}
