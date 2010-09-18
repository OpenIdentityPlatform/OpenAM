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
 * $Id: SingleLogoutHandler.java,v 1.5 2008/11/10 22:57:00 veiming Exp $
 *
 */

package com.sun.identity.multiprotocol;

import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The interface <code>SingleLogoutHandler</code> is used to handle
 * Single Logout for a specific protocol. This interface need to be
 * implemented by ID-FF, SAMLv2 and WS-Federation protocol to enable
 * single logout cross multiple federation protocols.
 *
 */

public interface SingleLogoutHandler {

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
     * @param SPEntityID <code>EntityID</code> of the remote service provider
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
        String SPEntityID,
        String relayState,
        String singleLogoutRequestXML,
        String singleLogoutResponseXML,
        int currentStatus
    ) throws Exception;
}
