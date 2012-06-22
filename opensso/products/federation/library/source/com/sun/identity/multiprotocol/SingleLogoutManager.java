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
 * $Id: SingleLogoutManager.java,v 1.8 2008/11/10 22:57:00 veiming Exp $
 *
 */

package com.sun.identity.multiprotocol;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.profile.LogoutUtil;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.w3c.dom.Element;

/**
 * The <code>SingleLogoutManager</code> class provides methods to perform
 * single logout cross multiple federation protocols. This method
 * <code>doIDPSingleLogout</code> need to be invoked by identity providers
 * after finishing processing its protocol specific single logout logics and 
 * before destroying the local session(s).
 * 
 */
public class SingleLogoutManager {
    /**
     * Status code for logout success
     */
    public static final int LOGOUT_SUCCEEDED_STATUS = 0;

    /**
     * Status code for partial logout success
     */
    public static final int LOGOUT_PARTIAL_STATUS = 1;

    /**
     * Status code for logout failure 
     */
    public static final int LOGOUT_FAILED_STATUS = 2;

    /**
     * Status code for logout request being redirected 
     */
    public static final int LOGOUT_REDIRECTED_STATUS = 3;
    
    /**
     * Status code for no logout action performed. This is for the case that 
     * this protocol does not take part in the single logout process.
     */
    public static final int LOGOUT_NO_ACTION_STATUS = 4;

    /**
     * Constant for SAML2 protocol
     */
    public static final String SAML2 = "saml2";
    
    /**
     * Constant for ID-FF Protocol
     */
    public static final String IDFF = "idff";
    
    /**
     * Constant for WS-Federation protocol
     */
    public static final String WS_FED = "wsfed";
    
    private static final String KEY_PARAM = "key";
    private static final String CLASS_PARAM = "class";
    
    /**
     * Session Property to record all federation protocols which used this
     * session object.
     */
    static final String FEDERATION_PROTOCOLS = "federationprotocols";
    
    /**
     * Constant for servlet alias
     */
    static final String RELAY_SERVLET = "multiprotocolrelay";
    private static final String RELAY_SERVLET_URI = "/" + RELAY_SERVLET + "/";
    
    /**
     * Constant for logout status parameter name
     */
    public static final String STATUS_PARAM = "logoutStatus";
            
    private static String MULTI_PROTOCOL_CONFIG_NAME = "MULTI_PROTOCOL";
    private static String EMPTY_STRING = "";   
    private static String DELIMITOR = "|";
    private static String LOCAL_HOST_URL = "http://localhost/idp";
 
    /**
     * Map to hold the protocol to SingleLogouthandler impl class mapping
     */
    private static Map handlerMap = new HashMap();
    
    /** 
     * Map to hold the relay state to request information 
     * TODO : handle cleanup
     */
    private static Map relayStateMap = new HashMap();
    private static Map userSessionMap = new HashMap();
    private static Map userIDMap = new HashMap();
    private static Map isSOAPInitiatedMap = new HashMap();
    private static Map isIDPInitiatedMap = new HashMap();
    private static Map origProtocolMap = new HashMap();
    private static Map protocolListMap = new HashMap();
    private static Map realmMap = new HashMap();
    private static Map idpEntityIDMap = new HashMap();
    private static Map spEntityIDMap = new HashMap();
    private static Map sloRequestXMLMap = new HashMap();
    private static Map sloResponseXMLMap = new HashMap();
    private static Map currentStatusMap = new HashMap();
    /**
     * List to maintain the list of protocol in order
     */
    private static List protocolList = new ArrayList();
                
    static Debug debug = Debug.getInstance("libMultipleProtocol");
    
    /** 
     * single logout manager instance 
     */
    private static SingleLogoutManager manager = new SingleLogoutManager();
    

    /** 
     * Creates a new instance of SingleLogoutManager 
     */
    private SingleLogoutManager() {
        try {
            ConfigurationInstance configInst = 
                ConfigurationManager.getConfigurationInstance(
                    MULTI_PROTOCOL_CONFIG_NAME);
            Map attrs = configInst.getConfiguration(null, null);
            Set handlers = (Set) attrs.get("SingleLogoutHandlerList");
            if (debug.messageEnabled()) {
                debug.message("SingleLogoutManager.constructor: handlers set=" 
                    + handlers);
            }
            if ((handlers != null) && !handlers.isEmpty()) {
                Iterator it = handlers.iterator();
                while (it.hasNext()) {
                    String tmp = (String) it.next();
                    StringTokenizer tokens = 
                        new StringTokenizer(tmp, DELIMITOR);
                    if (tokens.countTokens() != 2) {
                        debug.error("SingleLogoutManager.constructor: wrong "
                            + "handler value " + tmp);
                        continue;
                    }
                    String[] params = new String[2];
                    params[0] = tokens.nextToken();
                    params[1] = tokens.nextToken();
                    String key = null;
                    String className = null;
                    for (int i = 0; i < 2; i++) {
                        int loc = params[i].indexOf("=");
                        if (i == -1) {
                            debug.error("SingleLogoutManager.constructor: "
                                    + "missing = in parameter " + params[i]);
                            break;
                        }
                        String first = params[i].substring(0, loc);
                        if (first.equalsIgnoreCase(KEY_PARAM)) {
                            key = params[i].substring(loc + 1);
                        } else if (first.equalsIgnoreCase(CLASS_PARAM)) {
                            className = params[i].substring(loc + 1);
                        } else {
                            debug.error("SingleLogoutManager.constructor: "
                                    + "wrong key in parameter " + params[i]);
                            break;
                        }
                    }
                    
                    if ((key == null) || (key.length() == 0) ||
                        (className == null) || (className.length() == 0)) {
                        debug.error("SingleLogoutManager.constructor: "
                            + "invalid value " + params[0] + "|" + params[1]);
                        continue;
                    }
                    
                    try {
                        if (key.equalsIgnoreCase(SAML2)) {
                            key = SAML2;
                        } else if (key.equalsIgnoreCase(IDFF)) {
                            key = IDFF;
                        } else if (key.equalsIgnoreCase(WS_FED)) {
                            key = WS_FED;
                        } else {
                            // this could be protocol extension, 
                            // but not allowed right now
                            debug.error("SingleLogoutManager.constructor: "
                                + "invalid protocol " + key);
                            continue;
                        }
                        SingleLogoutHandler handler = (SingleLogoutHandler) 
                            Class.forName(className).newInstance();
                        protocolList.add(key);
                        handlerMap.put(key, handler);
                    } catch (ClassNotFoundException c) {
                        debug.error("SingleLogoutManager.constructor: "
                            + "class not found " + className, c);
                    } catch (InstantiationException i) {
                        debug.error("SingleLogoutManager.constructor: "
                            + "instantiation exception " + className, i);
                    } catch (IllegalAccessException i) {
                        debug.error("SingleLogoutManager.constructor: "
                            + "illegal access exception " + className, i);
                    }
                }
            }
            if (debug.messageEnabled()) {
                debug.message("SingleLogoutManager.constructor: handlers map=" 
                    + handlerMap);
            }
        } catch (ConfigurationException e) {
            debug.error("Unable to initiate Single Loogut Manager", e);
        }
    }
    
    /**
     * Returns SingleLogoutManager singleton instance.
     * @return manager instance.
     */
    public static SingleLogoutManager getInstance() {
        return manager;
    }
    
    /**
     * Returns true if handler is one of the keys in the relayState map.
     */
    boolean containKey(String handler) {
        if ((handler == null) || (handler.length() == 0)) {
            return false;
        }
        if (relayStateMap.containsKey(handler)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Performs single logout cross multiple federation protocols. This method
     * will invoke single logout processing for all the federation protocols. 
     *
     * Normally, there are three types of single logout to be supported:
     * - logout single session (specified by userSession parameter)
     * - logout a list of session (specified by userSession parameter)
     * - logout all sessions for a specific user (specified by userID parameter)
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
     *          SAML2  - single logout initiated using SAMLv2 protocol
     *          IDFF   - single logout initiated using ID-FF protocol
     *          WS_FED - single logout initiated using WS-Federation protocol
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
     * @param currentStatus Current logout status, this is the single logout 
     *      status for the federation protocol just processed.
     *      Possible values:
     *         <code>LOGOUT_SUCCEEDED_STATUS</code> - single logout succeeded.
     *         <code>LOGOUT_FAILED_STATUS</code>    - single logout failed.
     *         <code>LOGOUT_PARTIAL_STATUS</code>   - single logout partially 
     *                                                succeeded.
     * @return accumulative status of single logout for all protocols 
     *      processed so far, or status indicating the logout request has been
     *      redirected for processing. Possible values:
     *         <code>LOGOUT_SUCCEEDED_STATUS</code> - single logout succeeded.
     *         <code>LOGOUT_FAILED_STATUS</code>    - single logout failed.
     *         <code>LOGOUT_PARTIAL_STATUS</code>   - single logout partially 
     *                                                succeeded.
     *         <code>LOGOUT_REDIRECTED_STATUS</code> - single logout request 
     *                                                redirected.
     *         <code>LOGOUT_NO_ACTION_STATUS</code>  - single loglout not
     *                                                 performed.
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
        if (relayState == null){
            relayState = EMPTY_STRING;
        }
        if ((protocolList.isEmpty())) {
            // no handler configured, just return
            debug.message("SingleLogoutManager.doIDPSingleLogour : no handler");
            return LOGOUT_NO_ACTION_STATUS;
        }
        // the imcoming relayState could be the original relayState or the
        // generated relayStateString in this format: 
        // <proto>://<host>:<port>/<uri>/multiprotocolrelay/<40-byte-hex-string>
        // or just <40-byte-hex-string>
        String tmpRelayState = relayState;
        if (!relayStateMap.containsKey(relayState)) {
            tmpRelayState = getShortRelayState(relayState);
            if ((tmpRelayState != null) && !relayStateMap.containsKey(
                tmpRelayState)) {
                tmpRelayState = null;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("SingleLogoutManager.doIDPSLO: userID=" + userID +
                ", protocol=" + protocol + ", relay=" + relayState + 
                ", hex relay=" + tmpRelayState);
        }
        if (tmpRelayState == null) {
            // this is the first time the doIDPSingleLogout called, save params
            tmpRelayState = saveParameters(userSession, userID, isSOAPInitiated, 
                isIDPInitiated, protocol, realm, idpEntityID, spEntityID, 
                relayState, singleLogoutRequestXML, singleLogoutResponseXML,
                currentStatus);
            // replace relaystate with multi-protocol relay state servlet
            relayState = getRelayStateURL(request, tmpRelayState);
            if (debug.messageEnabled()) {
                debug.message("SingleLogoutManager.doIDPSingleLogout : save "
                    + tmpRelayState + ", new relayState=" + relayState);
            }
        } else {
            // update existing entry status
            updateStatus(tmpRelayState, currentStatus);
            if (tmpRelayState.equals(relayState)) {
                relayState = getRelayStateURL(request, tmpRelayState);
            }
            if (debug.messageEnabled()) {
                debug.message("SingleLogoutManager.doIDPSingleLogout : read "
                    + tmpRelayState + ", nu relayState=" + relayState);
            }
        }
        List list = (List) protocolListMap.get(tmpRelayState);
        if ((list == null) || list.isEmpty()) {
            return ((Integer)currentStatusMap.get(tmpRelayState)).intValue();
        } else {
            while (!list.isEmpty()) {
                String proto = (String) list.remove(0);
                SingleLogoutHandler handler = (SingleLogoutHandler)
                    handlerMap.get(proto);
                if (handler == null) {
                    debug.error("SingleLogoutManager.doIDPSingleLogout: "
                        + "no handler for protocol " + proto);
                    continue;
                }
                if (debug.messageEnabled()) {
                    debug.message("SingleLogoutManager.doIDPSingleLogout: "
                        + " handle protocol "  + proto);
                }
                userSession = (Set) userSessionMap.get(tmpRelayState);
                userID = (String) userIDMap.get(tmpRelayState);
                isSOAPInitiated = ((Boolean) isSOAPInitiatedMap.get(
                    tmpRelayState)).booleanValue(); 
                isIDPInitiated = ((Boolean) isIDPInitiatedMap.get(
                    tmpRelayState)).booleanValue(); 
                protocol = (String) origProtocolMap.get(tmpRelayState);
                realm = (String) realmMap.get(tmpRelayState);
                idpEntityID = (String) idpEntityIDMap.get(tmpRelayState);
                spEntityID = (String) spEntityIDMap.get(tmpRelayState);
                singleLogoutRequestXML = 
                    (String) sloRequestXMLMap.get(tmpRelayState);
                currentStatus = ((Integer) currentStatusMap.get(
                    tmpRelayState)).intValue();
                int status = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
                try {
                    status = handler.doIDPSingleLogout(userSession, userID, 
                        request, response, isSOAPInitiated, isIDPInitiated, 
                        protocol, realm, idpEntityID, spEntityID, relayState, 
                        singleLogoutRequestXML, singleLogoutResponseXML, 
                        currentStatus);
                    if (debug.messageEnabled()) {
                        debug.message("SingleLogoutManager.doIDPSingleLogout: "
                            + " logout status = " + status + " for " + proto);
                    }
                } catch (Exception ex) {
                    debug.error("SingleLogoutManager.doIDPSingleLogout: error"
                        + " for protocol " + proto, ex);
                    status = SingleLogoutManager.LOGOUT_FAILED_STATUS;
                }
                if (status == LOGOUT_REDIRECTED_STATUS) {
                    return status;
                } else {
                    updateStatus(tmpRelayState, status);
                }
            }
            
            int retVal = 
                ((Integer) currentStatusMap.get(tmpRelayState)).intValue();
            if (isSOAPInitiated) {
                cleanupParameters(tmpRelayState);
            }
            return retVal;
        }
    }
    
    private String getShortRelayState(String relayState) {
        if (relayState.length() == 0) {
            return null;
        } else {
            int loc = relayState.indexOf(RELAY_SERVLET_URI);
            if (loc == -1) {
                return null;
            } else {
                return relayState.substring(loc + RELAY_SERVLET_URI.length());
            }
        }
    }
    /**
     * Store incoming prameters to internal Map
     */
    private String saveParameters(
        Set userSession,
        String userID,
        boolean isSOAPInitiated,
        boolean isIDPInitiated,
        String protocol,
        String realm,
        String idpEntityID,
        String spEntityID,
        String relayState,
        String singleLogoutRequestXML,
        String singleLogoutResponseXML,
        int currentStatus) {

        String tmpRelayState = SAML2Utils.generateIDWithServerID();
        relayStateMap.put(tmpRelayState, relayState);
        if (debug.messageEnabled()) {
            debug.message("SingleLogoutManager.saveParameters: " +
                "userID=" + userID + ", session=" + userSession +
                ", orig relayState=" + relayState + ", new=" + tmpRelayState +
                ", isSOAInitiated=" + isSOAPInitiated + ", isIDPInitiated=" +
                isIDPInitiated + ", protocol=" + protocol + ", relam=" +
                realm + ", idpEntityID=" + idpEntityID + ", spEntityID=" +
                spEntityID + ", status=" + currentStatus +
                "\nlogout Request XML=" + singleLogoutRequestXML +
                "\nlogout response XML=" + singleLogoutResponseXML);
        }
        if (userSession != null) {
            userSessionMap.put(tmpRelayState, userSession);
        }
        if (userID != null) {
            userIDMap.put(tmpRelayState, userID);
        }
        if (isSOAPInitiated) {
            isSOAPInitiatedMap.put(tmpRelayState, Boolean.TRUE);
        } else {
            isSOAPInitiatedMap.put(tmpRelayState, Boolean.FALSE);
        }
        if (isIDPInitiated) {
            isIDPInitiatedMap.put(tmpRelayState, Boolean.TRUE);
        } else {
            isIDPInitiatedMap.put(tmpRelayState, Boolean.FALSE);
        }
        if (protocol != null) {
            origProtocolMap.put(tmpRelayState, protocol);
            
            // create a ArrayList without this protocol
            int listSize = protocolList.size();
            List list = new ArrayList(listSize - 1);
            for (int i = 0; i < listSize; i++) {
                String proto = (String) protocolList.get(i);
                if (!proto.equals(protocol)) {
                    list.add(proto);
                }
            }
            protocolListMap.put(tmpRelayState, list);
        }
        if (realm != null) {
            realmMap.put(tmpRelayState, realm);
        }
        if (idpEntityID != null) {
            idpEntityIDMap.put(tmpRelayState, idpEntityID);
        }
        if (spEntityID != null) {
            spEntityIDMap.put(tmpRelayState, spEntityID);
        }
        if (singleLogoutRequestXML != null) {
            sloRequestXMLMap.put(tmpRelayState, singleLogoutRequestXML);
        }
        if (singleLogoutResponseXML != null) {
            sloResponseXMLMap.put(tmpRelayState, singleLogoutResponseXML);
        }
        currentStatusMap.put(tmpRelayState, new Integer(currentStatus));
        return tmpRelayState;
    }   

    /**
     * Returns true if the specified relay state is one of the keys in
     * relayStateMap, false otherwise.
     */
    boolean containRelayState(String relayState) {
        return relayStateMap.containsKey(relayState);
    }
    
    /**
     * removes saved parameters from internal cache map
     */
    void cleanupParameters(String relayState) {
        if (debug.messageEnabled()) {
            debug.message("SingleLogoutManager.cleanupParameters:" +
                " new relayState=" + relayState);
        }
        relayStateMap.remove(relayState);
        userSessionMap.remove(relayState);
        userIDMap.remove(relayState);
        isSOAPInitiatedMap.remove(relayState);
        isSOAPInitiatedMap.remove(relayState);
        isIDPInitiatedMap.remove(relayState);
        origProtocolMap.remove(relayState);
        protocolListMap.remove(relayState);
        realmMap.remove(relayState);
        idpEntityIDMap.remove(relayState);
        spEntityIDMap.remove(relayState);
        sloRequestXMLMap.remove(relayState);
        sloResponseXMLMap.remove(relayState);
        currentStatusMap.remove(relayState);
    }

    /**
     * Sends logout response, this is for the case of HTTP binding
     * There are two cases here:
     * 1. IDP initiated HTTP Logout, just redirect user browser to original
     *    relaystate.
     * 2. SP initiated HTTP logout, need to send LogoutResponse back to SP.
     */ 
    void sendLogoutResponse(HttpServletRequest request,
            HttpServletResponse response, String relayState)
        throws IOException {
        if (debug.messageEnabled()) {
            debug.message("SingleLogoutManager.sendLogoutResponse: relaystate="
                + relayState);
        }
        String logoutResponseXML = (String) sloResponseXMLMap.get(relayState);
        if (logoutResponseXML == null) {
            // first case, just redirect to original relayState
            String origRelayState = (String) relayStateMap.get(relayState);
            int logoutStatus = 
                ((Integer) currentStatusMap.get(relayState)).intValue();
            String statusString = 
                MultiProtocolUtils.getLogoutStatus(logoutStatus);
            if ((origRelayState == null) || (origRelayState.length() == 0)) {
                // TODO : get default single logout URL for each protocol
                response.getWriter().print("Logout DONE. Status = " 
                    + statusString);
            } else {
                // include logout status
                if (origRelayState.indexOf("?") == -1) {
                    response.sendRedirect(origRelayState + "?" + 
                        SingleLogoutManager.STATUS_PARAM + "=" + statusString);
                } else {
                    response.sendRedirect(origRelayState + "&" + 
                        SingleLogoutManager.STATUS_PARAM + "=" + statusString);
                }
            }
        } else {
            String protocol = (String) origProtocolMap.get(relayState);
            String spEntityID = (String) spEntityIDMap.get(relayState);
            String origRelayState = (String) relayStateMap.get(relayState);
            String realm = (String) realmMap.get(relayState);
            String idpEntityID = (String) idpEntityIDMap.get(relayState);
            int currentStatus = 
                ((Integer) currentStatusMap.get(relayState)).intValue();
            if (protocol.equals(SingleLogoutManager.SAML2)) {
                try {
                    LogoutResponse logResp = ProtocolFactory.getInstance().
                        createLogoutResponse(logoutResponseXML);
                    String location = logResp.getDestination();
                    String statusVal = 
                        logResp.getStatus().getStatusCode().getValue();
                    String newVal = getNewStatusCode(currentStatus, statusVal);
                    if (!statusVal.equals(newVal)) {
                        logResp.getStatus().getStatusCode().setValue(statusVal);
                    }
                    if (debug.messageEnabled()) {
                       debug.message("SingleLogoutManager.sendLogoutRes:" +
                           "(SAML2) location=" + location + 
                           " orig status=" + statusVal + 
                           ", new status=" + newVal +
                           ", orig relay=" + origRelayState +
                           ", realm=" + realm +
                           ", idpEntityID=" + idpEntityID +
                           ", spEntityID=" + spEntityID);
                    }
                    LogoutUtil.sendSLOResponse(response, logResp, location, 
                        origRelayState, realm, idpEntityID, 
                        SAML2Constants.IDP_ROLE, spEntityID);
                } catch (SAML2Exception ex) {
                    debug.error("SingleLogoutManager.sendLogoutResponse:saml2",
                        ex);
                    throw new IOException(ex.getMessage());
                }             
            } else if (protocol.equals(SingleLogoutManager.IDFF)) {
                boolean failed = false;
                String logoutDoneURL = null;
                try {
                    debug.message("SingleLogoutManager.sendLogoutResp: IDFF");
                    IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
                    ProviderDescriptorType descriptor =
                        metaManager.getSPDescriptor(realm, spEntityID);
                    String retURL = descriptor.getSingleLogoutServiceReturnURL();
                    Element elem = XMLUtils.toDOMDocument(logoutResponseXML,
                        SingleLogoutManager.debug).getDocumentElement();
                    FSLogoutResponse responseLogout = new FSLogoutResponse(elem);
                    BaseConfigType hostedConfig =
                        metaManager.getIDPDescriptorConfig(realm, idpEntityID);
                    logoutDoneURL = FSServiceUtils.getLogoutDonePageURL(request,
                        hostedConfig, null);
                    Status status = responseLogout.getStatus();
                    String statusVal = status.getStatusCode().getValue();
                    String newVal = getNewStatusCode(currentStatus, statusVal);
                    if (!statusVal.equals(newVal)) {
                        com.sun.identity.saml.protocol.StatusCode statCode =
                            new com.sun.identity.saml.protocol.StatusCode(newVal);
                        com.sun.identity.saml.protocol.Status stat =
                            new com.sun.identity.saml.protocol.Status(statCode);
                        responseLogout.setStatus(stat);
                    }
                    if (debug.messageEnabled()) {
                        debug.message("SingleLogoutManager.sendLogoutRes:" +
                            "(IDFF) orig status=" + statusVal +
                            ", new status=" + newVal +
                            ", orig relay=" + origRelayState +
                            ", logout done URL=" + logoutDoneURL +
                            ", realm=" + realm +
                            ", idpEntityID=" + idpEntityID +
                            ", spEntityID=" + spEntityID);
                    }
                    String urlEncodedResponse =
                        responseLogout.toURLEncodedQueryString();
                    // Sign the request querystring
                    if (FSServiceUtils.isSigningOn()) {
                        String certAlias =
                                IDFFMetaUtils.getFirstAttributeValueFromConfig(
                                hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                        if (certAlias == null || certAlias.length() == 0) {
                            if (debug.messageEnabled()) {
                                debug.message("SingleLogoutManager.sendLogoutRes:"
                                        + "signSAMLRequest couldn't obtain cert alias.");
                            }
                            throw new SAMLResponderException(
                                FSUtils.bundle.getString(IFSConstants.NO_CERT_ALIAS));
                        } else {
                            urlEncodedResponse = FSSignatureUtil.signAndReturnQueryString(
                                    urlEncodedResponse, certAlias);
                        }
                    }
                    StringBuffer redirectURL = new StringBuffer();
                    redirectURL.append(retURL);
                    if (retURL.indexOf(IFSConstants.QUESTION_MARK) == -1) {
                        redirectURL.append(IFSConstants.QUESTION_MARK);
                    } else {
                        redirectURL.append(IFSConstants.AMPERSAND);
                    }
                    redirectURL.append(urlEncodedResponse);
                    if (debug.messageEnabled()) {
                        debug.message("SingleLogoutManager.sendResponse "
                                + "for IDFF, url = " + redirectURL.toString());
                    }
                    response.sendRedirect(redirectURL.toString());
                } catch (FSMsgException ex) {
                    debug.error("SingleLogoutManager.sendLogoutRes", ex);
                    failed = true;
                } catch (SAMLException ex) {
                    debug.error("SingleLogoutManager.sendLogoutRes", ex);
                    failed = true;;
                } catch (IDFFMetaException ex) {
                    debug.error("SingleLogoutManager.sendLogoutRes", ex);
                    failed = true;
                } catch (IOException ex) {
                    debug.error("SingleLogoutManager.sendLogoutRes", ex);
                    failed = true;
                }
                if (failed) {
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, logoutDoneURL, false,
                        IFSConstants.LOGOUT_SUCCESS, 
                        IFSConstants.LOGOUT_FAILURE);
                }
            } else if (protocol.equals(SingleLogoutManager.WS_FED)) {
                debug.message("SingleLogoutManager.sendLogoutResponse: WSFED");
                if (origRelayState != null) {
                    response.sendRedirect(origRelayState);
                } else {
                    response.getWriter().print("Logout DONE."); 
                }
            } else {
                // should never come here
                debug.error("SingleLogoutManager.sendLogoutResponse: invalid"
                    + " protocol : " + protocol);
            }
        }
        cleanupParameters(relayState);
        return;
    }
 
    /**
     * updates logout status based on current status and what stored in the
     * internal map.
     */
    private void updateStatus(String relayState, int currentStatus) {
        Integer tmp = (Integer) currentStatusMap.get(relayState);
        if (tmp != null) {
            int previousStatus = tmp.intValue();
            switch (previousStatus) {
                case LOGOUT_SUCCEEDED_STATUS:
                    if (currentStatus > previousStatus) {
                        currentStatusMap.put(relayState, 
                            new Integer(currentStatus));
                    }
                    break;
                case LOGOUT_FAILED_STATUS:
                    if (currentStatus < LOGOUT_FAILED_STATUS) {
                        currentStatusMap.put(relayState, 
                            new Integer(LOGOUT_PARTIAL_STATUS));
                    }
                    break;
                default:
                    break;   
            }
        } else {
            currentStatusMap.put(relayState, new Integer(currentStatus));
        }
    }
  
    /**
     * Returns new logout status code based on current status and old status
     * code.
     */
    private String getNewStatusCode(int currentStatus, String statusCode) {
        switch (currentStatus) {
            case LOGOUT_SUCCEEDED_STATUS:
                return statusCode;
            case LOGOUT_FAILED_STATUS:
            case LOGOUT_PARTIAL_STATUS:
                return IFSConstants.SAML_RESPONDER;
            default:
                return statusCode;
        }
    }
    
    /**
     * Returns relay state
     */
    private String getRelayStateURL(HttpServletRequest request, 
            String handler) {
        if (request == null) {
            // this is SOAP case
            return LOCAL_HOST_URL + RELAY_SERVLET_URI + handler;
        }
        String baseURL = MultiProtocolUtils.geServerBaseURL(request);
        return baseURL + RELAY_SERVLET_URI + handler;
    }
}
