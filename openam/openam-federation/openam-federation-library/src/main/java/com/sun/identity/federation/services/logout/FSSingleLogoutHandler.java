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
 * $Id: FSSingleLogoutHandler.java,v 1.15 2009/11/04 00:06:11 exu Exp $
 *
 * Portions Copyrighted 2013 ForgeRock AS
 *
 */
package com.sun.identity.federation.services.logout;

import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.plugin.session.SessionException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionPartner;
import com.sun.identity.federation.services.FSSOAPService;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * Work class that handles <code>ID-FF</code> single logout.
 */
public class FSSingleLogoutHandler {

    private static final String LOGOUT_JSP = "/saml2/jsp/autologout.jsp";
    private static final String WML_LOGOUT_JSP = "/saml2/jsp/autologoutwml.jsp";

    private HttpServletResponse response = null;
    private HttpServletRequest request = null;
    private String locale = null;
    private String userID = null;
    private String sessionIndex = "";
    private boolean isWMLAgent = false;
    private boolean isCurrentProviderIDPRole;
    private IDFFMetaManager metaManager = null;
    private ProviderDescriptorType remoteDescriptor = null;
    private ProviderDescriptorType hostedDescriptor = null;
    private BaseConfigType hostedConfig = null;
    private static final char QUESTION_MARK = '?';
    private static final char AMPERSAND = '&';
    private static String LOGOUT_DONE_URL = null;
    private static String COMMON_ERROR_URL = null;
    private String remoteEntityId = "";
    private String realm = null;
    private String hostedEntityId = "";
    private String hostedRole = null;
    private String metaAlias = null;
    private String relayState = null;

    private boolean logoutStatus = true;
    private boolean isHttpRedirect = false;
    private Object ssoToken = null;
    private FSLogoutResponse respObj = null;    
    private FSLogoutNotification requestLogout = null;
    private String singleLogoutProtocol = null;


   /*
    * Constructor.
    */
    public FSSingleLogoutHandler() {
        FSUtils.debug.message("FSSingleLogoutHandler::Constructor");
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Sets some commonly used URLs based on hosted provider.
     */
    protected void setLogoutURL() {
        LOGOUT_DONE_URL = FSServiceUtils.getLogoutDonePageURL(
            request, hostedConfig, metaAlias);
        COMMON_ERROR_URL = FSServiceUtils.getErrorPageURL(
            request, hostedConfig, metaAlias);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("LOGOUT_DONE_URL : " + LOGOUT_DONE_URL +
                "\nCOMMON_ERROR_URL : " + COMMON_ERROR_URL);
        }
    }

    /**
     * Sets the value of <code>RelayState</code> attribute.
     *
     * @param relayState the value of <code>RelayState</code> attribute.
     */
    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }

    /**
     * Sets the realm in which the provider resides.
     *
     * @param realm the realm in which the provider resides
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

   /**
    * Sets the single logout protocol to be used.
    * @param protocol Single Logout Protocol to be set
    */
    public void setSingleLogoutProtocol(String protocol) {
        this.singleLogoutProtocol = protocol;
    }
   /*
    * Initiates the logout operation.
    * @param response HTTP response
    * @param request HTTP request
    * @param currentSessionProvider initial provider with whom to broadcast
    * @param userID who is presently logging out
    * @param sessionIndex to be sent as part of logout message
    * @param isWMLAgent determines if response to be sent to WML agent
    * @param ssoToken session token of the user
    * @return status of the logout initiation operation.
    */
    public FSLogoutStatus handleSingleLogout(
        HttpServletResponse response,
        HttpServletRequest request,
        FSSessionPartner currentSessionProvider,
        String userID,
        String sessionIndex,
        boolean isWMLAgent,
        Object ssoToken) 
    {
        FSUtils.debug.message(
            "Entered FSSingleLogoutHandler::handleSingleLogout");
        // set all varaibles properly
        this.response = response;
        this.request = request;
        locale = FSServiceUtils.getLocale(request);
        setLogoutURL();
        this.userID = userID;
        this.sessionIndex = sessionIndex;
        this.isWMLAgent = isWMLAgent;
        if (currentSessionProvider != null) {
            isCurrentProviderIDPRole = currentSessionProvider.getIsRoleIDP();
            remoteEntityId = currentSessionProvider.getPartner();
            setRemoteDescriptor(getRemoteDescriptor(remoteEntityId));
        }
        this.ssoToken = ssoToken;
        String strProfile = getProfileToCommunicateLogout();
        singleLogoutProtocol = strProfile;
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Communicating logout with provider " +
                remoteEntityId + " using profile " + strProfile);
        }
        
        FSUtils.debug.message("FSSingleLogoutHandler, in case 1");
        FSLogoutStatus bLogoutStatus = null;
        
        if (strProfile.equals(IFSConstants.LOGOUT_SP_REDIRECT_PROFILE) ||
            strProfile.equals(IFSConstants.LOGOUT_IDP_REDIRECT_PROFILE)) 
        {
            FSUtils.debug.message("In redirect profile");
            try {
                String[] values = new String[]{"false"};
                SessionManager.getProvider().setProperty(ssoToken, 
                    IFSConstants.IS_SOAP_PROFILE, values);
            } catch (UnsupportedOperationException ex) {
                // ignore
            } catch (SessionException ex) {
                // ignore
            }
            bLogoutStatus = doHttpRedirect(remoteEntityId);
        } else if (strProfile.equals(IFSConstants.LOGOUT_IDP_SOAP_PROFILE) ||
            strProfile.equals(IFSConstants.LOGOUT_SP_SOAP_PROFILE)) 
        {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("In SOAP profile, current partner IDP? "
                    + isCurrentProviderIDPRole);
            }
            try {
                String[] values = new String[]{"true"};
                SessionManager.getProvider().setProperty(ssoToken, 
                    IFSConstants.IS_SOAP_PROFILE, values);
            } catch (UnsupportedOperationException ex) {
                // ignore
            } catch (SessionException ex) {
                // ignore
            }
            // This func should take care of initiating next
            // provider also as it has control
            bLogoutStatus = doIDPSoapProfile(remoteEntityId);
        } else if (strProfile.equals(IFSConstants.LOGOUT_IDP_GET_PROFILE) &&
            !isCurrentProviderIDPRole) 
        {
            FSUtils.debug.message("In GET profile");
            // HTTP GET is for IDP only, so always remove session partner
            FSLogoutUtil.removeCurrentSessionPartner(metaAlias,
                remoteEntityId, ssoToken, userID);
            bLogoutStatus = doHttpGet(remoteEntityId);
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Single Logout Profile cannot" +
                    " be processed. Verify profile in metadata");
            }
            String[] data = { strProfile };
            LogUtil.error(Level.INFO,LogUtil.LOGOUT_PROFILE_NOT_SUPPORTED,data,
                ssoToken);
            FSServiceUtils.returnLocallyAfterOperation(
                response, LOGOUT_DONE_URL,false,
                IFSConstants.LOGOUT_SUCCESS, IFSConstants.LOGOUT_FAILURE);
            return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Logout completed first round with status : "
                + bLogoutStatus);
        }
        // control will come here with error and without going
        // elsewhere in case of exception
        if (!bLogoutStatus.getStatus().equalsIgnoreCase(
            IFSConstants.SAML_SUCCESS))
        {
            FSServiceUtils.returnLocallyAfterOperation(
                response, LOGOUT_DONE_URL, false,
                IFSConstants.LOGOUT_SUCCESS, IFSConstants.LOGOUT_FAILURE);
        }
        return bLogoutStatus;
    }
    
    /**
     * Invoked in the case of Single Logout using SOAP profile.
     * Only in the case of SOAP do we have control to initiate logout for the
     * next-in-line provider. In the case of HTTP GET/Redirect we send the
     * message to one provider and lose control. Here in SOAP profile 
     * <code>continueLogout</code> continues the logout process.
     * @param isSuccess if true, means logout preformed successfully so far;
     *     if false, means logout failed in one or more providers.
     */
    private void continueLogout(boolean isSuccess) {
        FSUtils.debug.message(
            "Entered FSSingleLogoutHandler::continueLogout");
        if (FSLogoutUtil.liveConnectionsExist(userID, metaAlias)) 
        {
            FSUtils.debug.message("More liveConnectionsExist");
            HashMap providerMap = FSLogoutUtil.getCurrentProvider(
                userID, metaAlias, ssoToken);
            if (providerMap != null) {
                FSSessionPartner currentSessionProvider =
                    (FSSessionPartner)providerMap.get(
                        IFSConstants.PARTNER_SESSION);
                this.sessionIndex = (String)providerMap.get(
                    IFSConstants.SESSION_INDEX);
                if (currentSessionProvider != null) {
                    String currentEntityId = 
                        currentSessionProvider.getPartner();
                    isCurrentProviderIDPRole =
                        currentSessionProvider.getIsRoleIDP();
                    ProviderDescriptorType currentDesc = null;
                    try {
                        if (isCurrentProviderIDPRole) {
                            currentDesc = metaManager.getIDPDescriptor(
                                realm, currentEntityId);
                        } else {
                            currentDesc = metaManager.getSPDescriptor(
                                realm, currentEntityId);
                        }
                    } catch (Exception e) {
                        FSUtils.debug.error(
                            "FSSingleLogoutHandler:cannot get meta:", e);
                    }
                    setRemoteDescriptor(currentDesc);

                    // Clean session Map
                    FSSessionManager sessionManager = 
                        FSSessionManager.getInstance(metaAlias);
                    FSSession session = sessionManager.getSession(
                        sessionManager.getSessionList(userID), sessionIndex); 

                    if (!supportSOAPProfile(remoteDescriptor)) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "Single Logout Profile cannot" +
                                " be processed. Verify profile in metadata");
                        }
                        String[] data = { IFSConstants.LOGOUT_IDP_SOAP_PROFILE};
                        LogUtil.error(Level.INFO,
                            LogUtil.LOGOUT_PROFILE_NOT_SUPPORTED,data,ssoToken);
                        return;
                    }
                    FSUtils.debug.message("FSSLOHandler, SOAP in case 2");
                    // This func should take care of initiating next
                    // provider also as it has control
                    // remove session partner if status is success or 
                    // this is IDP
                    if ((doIDPSoapProfile(currentEntityId)).getStatus().
                            equalsIgnoreCase(IFSConstants.SAML_SUCCESS) ||
                        !isCurrentProviderIDPRole)
                    {
                        FSLogoutUtil.removeCurrentSessionPartner(
                            metaAlias, currentEntityId,
                            ssoToken, userID);
                        FSUtils.debug.message("SOAP partner removed, case 3");
                    }
                    return;
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("Reached else part " +
                            " currentSessionProvider "+
                            "is null. nothing more to broadcast" +
                            "\nNo more providers, destroy user" +
                            "session call destroyPrincipalSession");
                    }
                    FSLogoutUtil.destroyPrincipalSession(
                        userID, 
                        metaAlias,
                        sessionIndex,
                        request,
                        response);
                    if (response != null) {
                        returnAfterCompletion();
                    }
                    return;
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "GetCurrentProvider returns null HashMap" +
                        " Clean session and return" +
                        "\nNo live connections, destroy user" +
                        "  session call destroyPrincipalSession");
                }
                FSLogoutUtil.destroyPrincipalSession(
                    userID, metaAlias, sessionIndex, request, response);
                if (response != null) {
                    returnAfterCompletion();
                }
                return;
            }
        } else {
            FSUtils.debug.message("Reached else part in continuelogout");
            // destroy session when there is no failed logout or this is IDP
            // for SP does not logout local session in case IDP logout failed.
            if (isSuccess || !isCurrentProviderIDPRole) {
                FSUtils.debug.message("No live connections, destroy session");
                FSLogoutUtil.destroyPrincipalSession(
                    userID, metaAlias, sessionIndex, request, response);
            }
            // Call SP Adapter postSingleLogoutSuccess for SP/SOAP
            callPostSingleLogoutSuccess(
                respObj, IFSConstants.LOGOUT_SP_SOAP_PROFILE);
            if (response != null) {
                returnAfterCompletion();
            }
            return;
        }
    }
    
    /**
     * Performs the logout notification in the case of HTTP Redirect profile.
     * @param entityId the remote provider to whom logout message needs to
     *  be sent
     * @return logout status
     */
    private FSLogoutStatus doHttpRedirect(String entityId) {
        try {
            FSUtils.debug.message("In HTTP Redirect profile");
            isHttpRedirect = true;

            FSSessionManager sMgr = 
                FSSessionManager.getInstance(metaAlias);
            if (ssoToken == null) {
                try {
                    //this is HTTP based protocol, get from HTTP servlet request
                    ssoToken = SessionManager.getProvider().getSession(request);
                } catch (SessionException ex) {
                    FSUtils.debug.error(
                        "FSSLOHandler.doHttpRedirect: null ssoToken:", ex);
                }
            }
            FSSession session = sMgr.getSession(ssoToken);

            FSAccountFedInfo acctObj = null;
            if (session!=null) {
                acctObj = session.getAccountFedInfo();
            }
            if (acctObj == null && session != null && !session.getOneTime()) {
                acctObj = FSLogoutUtil.getCurrentWorkingAccount(
                    userID, entityId, metaAlias);
            }

            if (acctObj == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSingleLogoutHandler.doHttp" +
                        "Redirect: Account might have been terminated.");
                }
                return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
            }

            FSLogoutNotification reqLogout =
                createSingleLogoutRequest(acctObj, sessionIndex);
            if (this.relayState != null) {
                reqLogout.setRelayState(this.relayState);
            } 
            if (reqLogout == null) {
                FSUtils.debug.message("Logout Request is null");
                return new FSLogoutStatus(IFSConstants.SAML_REQUESTER);
            }
            reqLogout.setMinorVersion(getMinorVersion(remoteDescriptor));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSingleLogoutHandler::doHttpRedirect " +
                    remoteDescriptor.getSingleLogoutServiceURL() +
                    "\nLogout request: " + reqLogout.toXMLString());
            }
            String urlEncodedRequest = reqLogout.toURLEncodedQueryString();
            // Sign the request querystring
            if (FSServiceUtils.isSigningOn()) {
                String certAlias = 
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "Retrieving self certalias  : " + certAlias);
                }
                if (certAlias == null || certAlias.length() == 0) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSSingleLogoutHandler::" +
                            " doHttpRedirect: couldn't obtain " +
                            "this site's cert alias.");
                    }
                    return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
                }
                urlEncodedRequest =
                    FSSignatureUtil.signAndReturnQueryString(
                        urlEncodedRequest, certAlias);
            }
            StringBuffer redirectURL = new StringBuffer();
            String retURL = remoteDescriptor.getSingleLogoutServiceURL();
            FSUtils.debug.message("Encoded Redirect URL " + urlEncodedRequest);
            redirectURL.append(retURL);
            if (retURL.indexOf(QUESTION_MARK) == -1) {
                redirectURL.append(QUESTION_MARK);
            } else {
                redirectURL.append(AMPERSAND);
            }
            redirectURL.append(urlEncodedRequest);
            
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSingleLogoutHandler::doHttpRedirect" +
                    " URL is " + redirectURL.toString());
            }
            response.sendRedirect(redirectURL.toString());
            return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
        }catch(FSMsgException e){
            FSUtils.debug.error("FSSingleLogoutHandler::" +
            " doHttpRedirect FSMsgException:", e);
        }catch(IOException e){
            FSUtils.debug.error("FSSingleLogoutHandler::" +
            "doHttpRedirect IOException:", e);
        }
        return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
    }
    
    /**
     * Invoked to either send back control to remote provider if logout message
     * was received from one or
     * to show the local logout status page to the user.
     */
    protected void returnAfterCompletion() {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered FSSingleLogoutHandler::returnAC: "
                + "PROTOCOL=" + this.singleLogoutProtocol 
                + ", relayState=" + this.relayState);
        }
        try {            
            String returnProviderId = "";
            String relayState = "";
            String logoutStatusString = "";
            String inResponseTo = "";
            FSReturnSessionManager mngInst =
                FSReturnSessionManager.getInstance(metaAlias);
            HashMap providerMap = new HashMap();
            if (mngInst != null) {
                providerMap = mngInst.getUserProviderInfo(userID);
            }
            if (providerMap != null) {
                returnProviderId =
                    (String) providerMap.get(IFSConstants.PROVIDER);
                relayState =
                    (String) providerMap.get(IFSConstants.LOGOUT_RELAY_STATE);
                logoutStatusString =
                    (String) providerMap.get(IFSConstants.LOGOUT_STATUS);
                if (logoutStatusString == null || 
                    logoutStatusString.length() == 0) {
                    logoutStatusString = IFSConstants.SAML_SUCCESS;
                }
                inResponseTo =
                    (String) providerMap.get(IFSConstants.RESPONSE_TO);
                mngInst.removeUserProviderInfo(userID);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Deleted " + returnProviderId +
                        " from return list");
                }
                ProviderDescriptorType descriptor = null;
                if (hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                    descriptor = metaManager.getSPDescriptor(
                        realm, returnProviderId);
                } else {
                    descriptor = metaManager.getIDPDescriptor(
                        realm, returnProviderId);
                }
                String retURL = descriptor.getSingleLogoutServiceReturnURL();
                if (retURL != null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "Getting provider " +
                            returnProviderId + " IDP Return URL = " +
                            retURL);
                    }
                    FSLogoutResponse responseLogout = new FSLogoutResponse();
                    responseLogout.setResponseTo(inResponseTo);
                    responseLogout.setRelayState(relayState);
                    responseLogout.setProviderId(hostedEntityId);
                    responseLogout.setStatus(logoutStatusString);
                    responseLogout.setID(IFSConstants.LOGOUTID);
                    responseLogout.setMinorVersion(
                        getMinorVersion(descriptor));
                    responseLogout.setResponseID(FSUtils.generateID());
                    
                    // Call SP Adapter postSingleLogoutSuccess for SP/HTTP
                    callPostSingleLogoutSuccess(responseLogout,
                        IFSConstants.LOGOUT_IDP_REDIRECT_PROFILE);
                    
                    // call multi-federation protocol processing
                    if (MultiProtocolUtils.isMultipleProtocolSession(request,
                            SingleLogoutManager.IDFF) &&
                        hostedRole.equalsIgnoreCase(IFSConstants.IDP) &&
                        !MultiProtocolUtils.isMultiProtocolRelayState(
                            relayState)) {
                        int retStatus = handleMultiProtocolLogout(false, 
                            responseLogout.toXMLString(true, true), 
                            returnProviderId);
                        if (retStatus == 
                                SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                            return;
                        } else {
                            if ((retStatus == 
                                    SingleLogoutManager.LOGOUT_FAILED_STATUS) ||
                                (retStatus == 
                                    SingleLogoutManager.LOGOUT_PARTIAL_STATUS)){
                                responseLogout.setStatus(
                                    IFSConstants.SAML_RESPONDER);
                            }
                        }
                    }

                    String urlEncodedResponse =
                        responseLogout.toURLEncodedQueryString();
                    // Sign the request querystring
                    if (FSServiceUtils.isSigningOn()) {
                        String certAlias = 
                            IDFFMetaUtils.getFirstAttributeValueFromConfig(
                                hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                        if (certAlias == null || certAlias.length() == 0) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSBrowserArtifactConsumerHandler:: " +
                                    "signSAMLRequest:" +
                                    "couldn't obtain this site's cert alias.");
                            }
                            throw new SAMLResponderException(
                                FSUtils.bundle.getString(
                                    IFSConstants.NO_CERT_ALIAS));
                        }
                        urlEncodedResponse =
                            FSSignatureUtil.signAndReturnQueryString(
                                urlEncodedResponse, certAlias);
                    }
                    StringBuffer redirectURL = new StringBuffer();
                    redirectURL.append(retURL);
                    if (retURL.indexOf(IFSConstants.QUESTION_MARK) == -1) {
                        redirectURL.append(IFSConstants.QUESTION_MARK);
                    } else {
                        redirectURL.append(IFSConstants.AMPERSAND);
                    }
                    redirectURL.append(urlEncodedResponse);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("Response to be sent : " +
                        redirectURL.toString());
                    }
                    String[] data = { userID };
                    LogUtil.access(Level.INFO,LogUtil.LOGOUT_SUCCESS,data);
                    response.sendRedirect(redirectURL.toString());
                    return;
                }
            } else {
                FSUtils.debug.message(
                    "no source provider. return to local status page");
                
                // handle muliple federation protocol for IDP initiated SOAP
                // binding case, no need to redirect to default URL,
                // just return so the LogoutResponse is send back to
                // Multiple protocol single logout handler
                if ((this.singleLogoutProtocol != null) &&
                    this.singleLogoutProtocol.equals(
                    IFSConstants.LOGOUT_IDP_SOAP_PROFILE) && 
                    (this.relayState != null) && 
                    MultiProtocolUtils.isMultiProtocolRelayState(
                    this.relayState)) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSSingleLogoutHandler::returnAC:"
                            + " this is multiProto for IDP initiated SOAP");
                    }
                    return;
                }

                // call multi-federation protocol processing
                if (MultiProtocolUtils.isMultipleProtocolSession(request,
                    SingleLogoutManager.IDFF) &&
                    hostedRole.equalsIgnoreCase(IFSConstants.IDP) &&
                    !MultiProtocolUtils.isMultiProtocolRelayState(relayState)) {
                    boolean isSOAPInitiated = false;
                    if ((singleLogoutProtocol.equals(
                            IFSConstants.LOGOUT_IDP_SOAP_PROFILE)) ||
                        (singleLogoutProtocol.equals(
                            IFSConstants.LOGOUT_SP_SOAP_PROFILE))) {
                        isSOAPInitiated = true;
                    }
                    int retStatus = handleMultiProtocolLogout(isSOAPInitiated, 
                            null, remoteEntityId);
                    if (retStatus == 
                        SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                        return;
                    } else {
                        if ((retStatus == 
                                SingleLogoutManager.LOGOUT_FAILED_STATUS) ||
                            (retStatus ==
                                SingleLogoutManager.LOGOUT_PARTIAL_STATUS)) {
                             logoutStatus = false;           
                        }
                    }
                }
                if (logoutStatus) {
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, LOGOUT_DONE_URL, true,
                        IFSConstants.LOGOUT_SUCCESS, 
                        IFSConstants.LOGOUT_FAILURE);
                }
                return;
            }
        } catch (IDFFMetaException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Unable to get LRURL. " +
                    "No location to redirect. processing completed");
            }
            String[] data =
              { FSUtils.bundle.getString(IFSConstants.LOGOUT_REDIRECT_FAILED) };
            LogUtil.error(Level.INFO,LogUtil.LOGOUT_REDIRECT_FAILED,data,
                ssoToken);
        } catch (Exception ex){
            String[] data =
              { FSUtils.bundle.getString(IFSConstants.LOGOUT_REDIRECT_FAILED) };
            LogUtil.error(Level.INFO,LogUtil.LOGOUT_REDIRECT_FAILED,data,
                ssoToken);
        }
    }
    
    
    /**
     * Invoked when logout needs to done using the HTTP GET profile.
     * @param providerId the first provider whose preferred profile is HTTP GET
     * @return <code>FSLogoutStatus</code>
     */
    private FSLogoutStatus doHttpGet(String providerId) {
        FSUtils.debug.message("doHttpGet - Entered");
        if (isWMLAgent) {
            return doWMLGet(providerId);
        } else {
            return doHTMLGet(providerId);
        }
    }
    
    /**
     * Performs the HTTP GET related operations when the user agent is
     * WML based.
     * @param providerId the first provider whose preferred profile is HTTP GET
     */
    private FSLogoutStatus doWMLGet(String providerId) {
        FSUtils.debug.message("In WML based response");
        StringBuffer destination = new StringBuffer();
        destination.append(hostedDescriptor.getSingleLogoutServiceURL());
        if ((destination.toString()).indexOf(QUESTION_MARK) == -1) {
            destination.append(QUESTION_MARK);
        } else {
            destination.append(AMPERSAND);
        }
        destination.append("logoutSource=logoutGet");
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "Submit action : " + destination.toString());
        }

        // DO WML response
        FSUtils.debug.message("Calling getLogoutGETProviders");
        HashMap providerMap = FSLogoutUtil.getLogoutGETProviders(
                userID,
                providerId,
                sessionIndex,
                realm,
                metaAlias);
        Vector providerGetList = (Vector)providerMap.get("Provider");
        FSUtils.debug.message("Calling cleanSessionMapProviders");
        FSLogoutUtil.cleanSessionMapProviders(userID,
            providerGetList, metaAlias);

        FSUtils.debug.message("Calling getMultiLogoutRequest");
        String multiLogoutRequest = getMultiLogoutRequest(providerMap);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "Image Statements : " + multiLogoutRequest);
        }

        request.setAttribute("DESTINATION_URL", destination.toString());
        request.setAttribute("MULTI_LOGOUT_REQUEST", multiLogoutRequest);

        try {
            request.getRequestDispatcher(WML_LOGOUT_JSP).forward(request, response);

        } catch (ServletException sE) {
            FSUtils.debug.error("Error in performing HTTP GET for WML agent:", sE);
            return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
        } catch (IOException ioE) {
            FSUtils.debug.error("Error in performing HTTP GET for WML agent:", ioE);
            return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
        }

        return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
    }
    
    /**
     * Performs the HTTP GET related operations when the
     * user agent is non WML based.
     * @param providerId the first provider whose preferred profile is HTTP GET
     */
    private FSLogoutStatus doHTMLGet(String providerId) {
        // DO Normal HTML response
        FSUtils.debug.message("In HTML based response");
        StringBuffer destination = new StringBuffer();
        destination.append(hostedDescriptor.getSingleLogoutServiceURL());
        if ((destination.toString()).indexOf(QUESTION_MARK) == -1) {
            destination.append(QUESTION_MARK);
        } else {
            destination.append(AMPERSAND);
        }
        destination.append("logoutSource=logoutGet");

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Submit action : " +
                destination.toString());
        }
        FSUtils.debug.message("Calling getLogoutGETProviders");
        HashMap providerMap = FSLogoutUtil.getLogoutGETProviders(
            userID,
            providerId,
            sessionIndex,
            realm,
            metaAlias);
        Vector providerGetList =
            (Vector)providerMap.get(IFSConstants.PROVIDER);
        FSUtils.debug.message("Calling cleanSessionMapProviders");

        FSLogoutUtil.cleanSessionMapProviders(
            userID,
            providerGetList,
            metaAlias);
        FSUtils.debug.message("Calling getMultiLogoutRequest");
        String multiLogoutRequest = getMultiLogoutRequest(providerMap);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Image Statements : " +
                multiLogoutRequest);
        }

        request.setAttribute("DESTINATION_URL", destination.toString());
        request.setAttribute("MULTI_LOGOUT_REQUEST", multiLogoutRequest);

        try {
            request.getRequestDispatcher(LOGOUT_JSP).forward(request, response);

        } catch (ServletException sE) {
            FSUtils.debug.error("Error in performing HTTP GET for regular agent", sE);
            return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
        } catch (IOException ioE) {
            FSUtils.debug.error("Error in performing HTTP GET for regular agent", ioE);
            return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
        }

        return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
    }
    
    /**
     * Prepares the IMG tags that correspond to Single logout requests that
     * will all be shown in a single page when HTTP GET profile is used.
     * @param providerMap contains information about all the providers
     *  for whom GET is the logout profile
     * @return String that has the IMG tags for each provider to be notified
     */
    private String getMultiLogoutRequest(HashMap providerMap) {
        try {
            Vector providerList =
                (Vector)providerMap.get(IFSConstants.PROVIDER);
            HashMap sessionList = 
                (HashMap)providerMap.get(IFSConstants.SESSION_INDEX);
            StringBuffer imgString = new StringBuffer();
            if (providerList != null) {
                for (int i = 0; i < providerList.size(); i++) {
                    String providerId = (String)providerList.elementAt(i);
                    FSAccountFedInfo currentAccount =
                        FSLogoutUtil.getCurrentWorkingAccount(
                            userID, providerId, metaAlias);
                    FSLogoutNotification reqLogout =
                        createSingleLogoutRequest(currentAccount,
                            (String) sessionList.get(providerId));
                    ProviderDescriptorType descriptor =
                        metaManager.getSPDescriptor(realm, providerId);
                    reqLogout.setMinorVersion(getMinorVersion(descriptor));
                    String urlEncodedRequest = 
                        reqLogout.toURLEncodedQueryString();
                    // Sign the request querystring
                    String certAlias = 
                        IDFFMetaUtils.getFirstAttributeValueFromConfig(
                            hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("certalias  : " + certAlias);
                    }
                    if (certAlias == null || certAlias.length() == 0) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSSingleLogoutHandler" +
                                " getMultiLogoutRequest: couldn't obtain "
                                + "this site's cert alias.");
                        }
                        continue;
                    }
                    urlEncodedRequest =
                        FSSignatureUtil.signAndReturnQueryString(
                            urlEncodedRequest,
                            certAlias);
                    StringBuffer redirectURL = new StringBuffer();
                    String retURL = descriptor.getSingleLogoutServiceURL();
                    redirectURL.append(retURL);
                    if (retURL.indexOf(QUESTION_MARK) == -1) {
                        redirectURL.append(QUESTION_MARK);
                    } else {
                        redirectURL.append(AMPERSAND);
                    }
                    redirectURL.append(urlEncodedRequest);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSSingleLogoutHandler::" +
                            "doHttpRedirect URL is " + redirectURL.toString());
                    }
                    imgString.append("<IMG SRC=\"")
                        .append(redirectURL.toString()).append("\" />");
                }
                return imgString.toString();
            }
        } catch(FSMsgException e){
            FSUtils.debug.error("FSSingleLogoutHandler::getMultiLogoutRequest" +
                " FSMsgException", e);
        } catch (IDFFMetaException e){
            FSUtils.debug.error("FSSingleLogoutHandler::getMultiLogoutRequest" +
            "  IDFFMetaException", e);
        }
        FSUtils.debug.error("Returning null from getMultiLogoutRequest");
        return null;
    }
    
    /**
     * Initiates SOAP profile logout. It iterates through all the providers in
     * a loop.
     * @param providerId the first provider with SOAP as logout profile.
     */
    private FSLogoutStatus doIDPSoapProfile(String providerId) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSLOHandler.doIDPSoapProfile : providerId="
                + providerId);
        }
        FSLogoutStatus bSoapStatus = doSoapProfile(providerId);
        if (bSoapStatus.getStatus().equalsIgnoreCase(IFSConstants.SAML_SUCCESS))
        {
            FSUtils.debug.message(
                "SOAP first round went fine. Calling continue logout");
            // remove current session partner in case of success
            FSLogoutUtil.removeCurrentSessionPartner(metaAlias,
                providerId, ssoToken, userID);
            FSUtils.debug.message("SOAP partner removed in case of success");
        } else {
            FSUtils.debug.message("SOAP first round false. No continue logout");
            // remove session partner if this is IDP
            if (!isCurrentProviderIDPRole) {
                FSLogoutUtil.removeCurrentSessionPartner(metaAlias,
                    providerId, ssoToken, userID);
            }
            logoutStatus = false;
        }
        if (!isHttpRedirect && (logoutStatus || !isCurrentProviderIDPRole)) {
            continueLogout(logoutStatus);
        }
        
        if (!isHttpRedirect) {
            FSUtils.debug.message("FSSLOHandler.doIDPSoapProfile: call MP/SOAP");
            try {
                // call Multi-Federation protocol single logout
                if ((SessionManager.getProvider().isValid(ssoToken)) &&
                    (MultiProtocolUtils.isMultipleProtocolSession(ssoToken, 
                        SingleLogoutManager.IDFF))) {
                    int retStatus = handleMultiProtocolLogout(true, null, 
                        remoteEntityId);
                    logoutStatus = updateLogoutStatus(logoutStatus, retStatus);
                }
            } catch (SessionException ex) {
                //ignore;
                FSUtils.debug.message("FSSLOHandler.doIDPSoapProfile2", ex);
            }
        }
        if (!logoutStatus) {
            return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
        } else {
            // redirect in case of SOAP and successful logout
            if (response != null && !isHttpRedirect) {
                returnAfterCompletion();
            }
            return bSoapStatus;
        }
    }
    
    private boolean updateLogoutStatus(boolean logoutStatus, int retStatus) {
        boolean status = logoutStatus;
        switch (retStatus) {
            case SingleLogoutManager.LOGOUT_FAILED_STATUS:
                status = false;
                break;
            case SingleLogoutManager.LOGOUT_PARTIAL_STATUS:
                status = false;
                break;
            default:
                break;
        }
        return status;
    }
    
    /**
     * Initiates SOAP proifle logout.
     * @param providerId the first provider with SOAP as logout profile
     */
    private FSLogoutStatus doSoapProfile(String providerId) {
        FSUtils.debug.message("Entered IDP's doSoapProfile");
        try{
           FSSessionManager sMgr = FSSessionManager.getInstance(metaAlias);
           FSSession session = sMgr.getSession(ssoToken);

            FSAccountFedInfo currentAccount = null;
            if (session!=null) {
                currentAccount = session.getAccountFedInfo();
            }
            if (currentAccount == null && !session.getOneTime()) {
                currentAccount = FSLogoutUtil.getCurrentWorkingAccount(
                     userID, providerId, metaAlias);
            }            
            
            if(currentAccount == null) {
               if(FSUtils.debug.messageEnabled()) {
                  FSUtils.debug.message("FSSingleLogoutHandler. User's " +
                      "account may have been terminated."); 
               }
               return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
            }

            FSLogoutNotification reqLogout =
                createSingleLogoutRequest(currentAccount, sessionIndex);
            reqLogout.setMinorVersion(getMinorVersion(remoteDescriptor));
            if (reqLogout != null) {
                FSSOAPService instSOAP = FSSOAPService.getInstance();
                if (instSOAP != null){
                    FSUtils.debug.message(
                        "Signing suceeded. To call bindLogoutRequest");
                    
                    reqLogout.setID(IFSConstants.LOGOUTID);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("logout request before sign: "
                            + reqLogout.toXMLString(true,true));
                    }
                    SOAPMessage msgLogout = instSOAP.bind(
                        reqLogout.toXMLString(true, true));
                    if (msgLogout != null) {
                        SOAPMessage retSOAPMessage = null;
                        try{
                            if(FSServiceUtils.isSigningOn()) {
                                int minorVersion = reqLogout.getMinorVersion();
                                switch (minorVersion) {
                                case IFSConstants.FF_11_PROTOCOL_MINOR_VERSION:
                                    msgLogout =
                                        signLogoutRequest(
                                        msgLogout, IFSConstants.ID,
                                        reqLogout.getID());
                                    break;
                                case IFSConstants.FF_12_PROTOCOL_MINOR_VERSION:
                                    msgLogout = signLogoutRequest(
                                            msgLogout,
                                            IFSConstants.REQUEST_ID,
                                            reqLogout.getRequestID());
                                    break;
                                default:
                                    FSUtils.debug.message(
                                        "invalid minor version.");
                                    break;
                                }
                            }
                            retSOAPMessage = instSOAP.sendMessage(
                                msgLogout,
                                remoteDescriptor.getSoapEndpoint());
                        } catch(Exception e){
                            FSUtils.debug.error(
                                "FSSOAPException in doSOAPProfile" +
                                " Cannot send request", e);
                            return new FSLogoutStatus(
                                IFSConstants.SAML_RESPONDER);
                        }
                        if(retSOAPMessage != null) {
                            Element elt =
                                instSOAP.parseSOAPMessage(retSOAPMessage);
                            if (FSServiceUtils.isSigningOn()) {
                                if (!verifyResponseSignature(retSOAPMessage)){
                                    if (FSUtils.debug.messageEnabled()) {
                                        FSUtils.debug.message("Response " +
                                            "signature verification failed");
                                    }
                                    FSServiceUtils.returnLocallyAfterOperation(
                                        response, LOGOUT_DONE_URL, false,
                                        IFSConstants.LOGOUT_SUCCESS,
                                        IFSConstants.LOGOUT_FAILURE);
                                    return new FSLogoutStatus(
                                        IFSConstants.SAML_REQUESTER);
                                }
                            }
                            this.requestLogout = reqLogout;
                            respObj = new FSLogoutResponse(elt);
                            // Call SP Adapter preSingleLogout for SP/SOAP
                            if (hostedRole != null &&
                                hostedRole.equalsIgnoreCase(IFSConstants.SP))
                            {
                                FederationSPAdapter spAdapter =
                                    FSServiceUtils.getSPAdapter(
                                        hostedEntityId, hostedConfig);
                                if (spAdapter != null) {
                                    if (FSUtils.debug.messageEnabled()) {
                                        FSUtils.debug.message("FSSLOHandler." +
                                        "preSingleLogoutProcess, SP/SOAP");
                                    }
                                    try {
                                        spAdapter.preSingleLogoutProcess(
                                            hostedEntityId,
                                             request, response, userID,
                                             reqLogout, respObj,
                                             IFSConstants.
                                                 LOGOUT_SP_SOAP_PROFILE);
                                    } catch (Exception e) {
                                        // ignore adapter error
                                        FSUtils.debug.error("spAdapter." +
                                        "preSingleLogoutProcess, SP/SOAP:", e);
                                    }
                                }
                            }
                            
                            Status status = respObj.getStatus();
                            StatusCode statusCode = status.getStatusCode();
                            StatusCode secondLevelStatus = 
                                statusCode.getStatusCode();
                            String statusString = statusCode.getValue();
                            if (statusString.equalsIgnoreCase(
                                IFSConstants.SAML_SUCCESS)) 
                            {
                                if (FSUtils.debug.messageEnabled()) {
                                    FSUtils.debug.message(
                                        "FSSingleLogoutHandler: " +
                                        " doSoapProfile returning success");
                                }
                                return new FSLogoutStatus(
                                    IFSConstants.SAML_SUCCESS);
                            } else {
                                if (FSUtils.debug.messageEnabled()) {
                                    FSUtils.debug.message(
                                        "FSSingleLogoutHandler: " +
                                        "SOAP Profile failure " + statusString);
                                }
                                return new FSLogoutStatus(statusString);
                            }
                        }
                    }
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Unable to bindLogoutRequest." +
                        "Current Provider cannot be processed");
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Unable to create logout request" +
                        " Current Provider cannot be processed");
                }
            }
        } catch (Exception e){
            FSUtils.debug.error("In IOException of doSOAPProfile : " ,e);
        }
        return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
    }
    
    
    public FSLogoutStatus doIDPProxySoapProfile(
        HttpServletRequest request,
        HttpServletResponse response,
        FSSessionPartner currentSessionProvider,
        String userID,
        String sessionIndex,
        Object ssoToken) 
    {
        this.request = request;
        this.response = response;
        this.userID = userID;
        this.ssoToken = ssoToken;
        this.sessionIndex = sessionIndex;
        isCurrentProviderIDPRole = true;
        this.remoteEntityId = currentSessionProvider.getPartner();
        setRemoteDescriptor(getRemoteDescriptor(remoteEntityId));

        FSLogoutStatus retStatus = doSoapProfile(remoteEntityId);
        if (retStatus.getStatus().equalsIgnoreCase(IFSConstants.SAML_SUCCESS)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSingleLogoutHandler.doIDPProxySoapProfile: " + 
                    "single logout from " + remoteEntityId);
            }
            // remove current session partner
            FSLogoutUtil.removeCurrentSessionPartner(
                metaAlias, remoteEntityId, ssoToken, userID);
            callPostSingleLogoutSuccess(respObj,
                IFSConstants.LOGOUT_IDP_SOAP_PROFILE);
        }
        return retStatus;
    }

    /**
     * Creates the logoutNotification message for a provider.
     * @param acctInfo the curerent user-provider information
     * @param sessionIndex to be sent as part of lgout request
     * @return the logout request
     */
    private FSLogoutNotification createSingleLogoutRequest(
        FSAccountFedInfo acctInfo,
        String sessionIndex) 
    {
        FSUtils.debug.message(
            "Entered FSSingleLogoutHandler::createSingleLogoutRequest");
        FSLogoutNotification reqName = new FSLogoutNotification();
        if (reqName != null){
            NameIdentifier nameIdentifier = acctInfo.getRemoteNameIdentifier();
            if (nameIdentifier == null) {
                nameIdentifier = acctInfo.getLocalNameIdentifier();
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Hosted Provider Id : " +
                    hostedEntityId);
            }
            reqName.setProviderId(hostedEntityId);
            reqName.setNameIdentifier(nameIdentifier);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Session index is " + sessionIndex);
            }
            if (sessionIndex != null) {
                reqName.setSessionIndex(sessionIndex);
            }
            return reqName;
        }
        return null;
    }
    
    /**
     * Sets the hosted provider details.
     * @param hostedProviderDesc the descriptor of the hosted provider handling
     * logout
     */
    public void setHostedDescriptor(
         ProviderDescriptorType hostedProviderDesc) 
    {
        this.hostedDescriptor = hostedProviderDesc;
    }

    /**
     * Sets the hosted provider's extended meta config.
     * @param hostedConfig hosted provider's extended meta config
     */
    public void setHostedDescriptorConfig(BaseConfigType hostedConfig) {
        this.hostedConfig = hostedConfig;
    }

    /**
     * Sets hosted provider's entity id.
     * @param hostedEntityId hosted provider's entity id.
     */
    public void setHostedEntityId(String hostedEntityId) {
        this.hostedEntityId = hostedEntityId;
    }
 
    /**
     * Sets hosted provider's role.
     * @param hostedRole hosted provider's role
     */ 
    public void setHostedProviderRole(String hostedRole) {
        this.hostedRole = hostedRole;
    }

    /**
     * Sets hosted provider's meta alias.
     * @param metaAlias hosted provider's meta alias.
     */
    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
    }

    /**
     * Sets the remote provider descriptor.
     * @param remoteDesc Remote Provider Descriptor.
     */
    public void setRemoteDescriptor(ProviderDescriptorType remoteDesc) {
        this.remoteDescriptor = remoteDesc;
    }

    /**
     * Gets the remote provider descriptor.
     * @return remote provider descriptor
     */
    protected ProviderDescriptorType getRemoteDescriptor(String remoteEntityId){
        if (remoteEntityId == null || remoteEntityId.length() == 0 ||
            metaManager == null) 
        {
            return null;
        }
        
        FSUtils.debug.message(
            "FSSingleLogoutHandler :: getRemoteDescriptor...");
        ProviderDescriptorType providerDesc = null;
        try {
            if (isCurrentProviderIDPRole) {
                providerDesc = metaManager.getIDPDescriptor(
                    realm, remoteEntityId);
            } else {
                providerDesc = metaManager.getSPDescriptor(
                    realm, remoteEntityId);
            }
        } catch(IDFFMetaException e){
            FSUtils.debug.error("FSSingleLogoutHandler::" +
                " getRemoteDescriptor failed:", e);
        }
        return providerDesc;
    }

    /**
     * Determines the profile to be used to communicate logout.
     * @return String the liberty defined logout profile
     */
    protected String getProfileToCommunicateLogout() {
        FSUtils.debug.message(
            "FSSingleLogoutHandler :: getProfileToCommunicateLogout...");
        if (singleLogoutProtocol != null) {
            return singleLogoutProtocol;
        }
        String retProfileType = "";
        if (metaManager != null) {
            ProviderDescriptorType descriptor = remoteDescriptor;
            if (isCurrentProviderIDPRole) {
                FSUtils.debug.message("Local provider is SP");
                descriptor = hostedDescriptor;
            } else {
                FSUtils.debug.message("Local provider is IDP");
            }
            List profiles = descriptor.getSingleLogoutProtocolProfile();
            if (profiles != null && !profiles.isEmpty()) {
                retProfileType = (String) profiles.iterator().next();
            }
        }
        return retProfileType;
    }

    /**
     * Processes the logout request received through http.
     * @param response the HttpServletResponse object
     * @param reqLogout the logout request
     * @param currentSessionProvider initial provider with whom to broadcast
     * @param userID who is presently logging out
     * @param ssoToken user session
     * @param sourceEntityId source provider's entity id
     * @param sessionIndex to be sent as part of logout message
     * @param isWMLAgent determines if response to be sent to AML agent
     * @param relayState received with the logout request
     * @param isSourceIDP whether source provider is an IDP or not
     * @return logout status
     */
    public FSLogoutStatus processHttpSingleLogoutRequest(
        HttpServletResponse response,
        HttpServletRequest request,
        FSLogoutNotification reqLogout,
        FSSessionPartner currentSessionProvider,
        String userID,
        Object ssoToken,
        String sourceEntityId,
        String sessionIndex,
        boolean isWMLAgent,
        String relayState,
        String isSourceIDP)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered FSSingleLogoutHandler::" +
                " processHttpSingleLogoutRequest - HTTP");
        }

        this.response = response;
        this.request = request;
        this.requestLogout = reqLogout;
        locale = FSServiceUtils.getLocale(request);
        setLogoutURL();
        if (currentSessionProvider != null) {
           isCurrentProviderIDPRole = currentSessionProvider.getIsRoleIDP();
           remoteEntityId = currentSessionProvider.getPartner();
           setRemoteDescriptor(getRemoteDescriptor(remoteEntityId));
        }
        this.userID = userID;
        this.ssoToken = ssoToken;
        this.sessionIndex = sessionIndex;
        this.isWMLAgent = isWMLAgent;

        if (reqLogout != null) {
            FSUtils.debug.message("FSLogoutNotification formed really well");
            FSReturnSessionManager localManager =
                FSReturnSessionManager.getInstance(metaAlias);
            if (localManager != null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Added " + sourceEntityId +
                        " top return list");
                }
                localManager.setUserProviderInfo(
                    userID, 
                    sourceEntityId,
                    isSourceIDP, 
                    relayState,
                    reqLogout.getRequestID());
            } else {
                FSUtils.debug.message("Cannot get FSReturnSessionManager");
            }

            FSSessionManager sessionManager =  
                   FSSessionManager.getInstance(metaAlias);

            FSSession session = sessionManager.getSession(
                   sessionManager.getSessionList(userID),
                   sessionIndex);

            if (currentSessionProvider == null) {
                FSUtils.debug.message(
                    "currentSessionProvider is null. destroy and return");
              
                FSLogoutUtil.destroyPrincipalSession(
                    userID, 
                    metaAlias,
                    reqLogout.getSessionIndex(),
                    request,
                    response);
                returnAfterCompletion();
                return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
            } else {
                String currentEntityId =
                    currentSessionProvider.getPartner();
                isCurrentProviderIDPRole =
                    currentSessionProvider.getIsRoleIDP();

                FSUtils.debug.message("FSSLOHandler, in case 3");
                FSLogoutUtil.cleanSessionMapPartnerList(
                    userID, currentEntityId, metaAlias, session);
                FSLogoutStatus bLogoutStatus = null;
            
                List profiles = 
                    remoteDescriptor.getSingleLogoutProtocolProfile();
                if (profiles != null &&
                    (profiles.contains(IFSConstants.LOGOUT_SP_REDIRECT_PROFILE)
                        ||
                    profiles.contains(
                        IFSConstants.LOGOUT_IDP_REDIRECT_PROFILE)))
                {
                    FSUtils.debug.message("In redirect profile");
                    bLogoutStatus = doHttpRedirect(
                        currentEntityId);
                } else if (profiles != null &&
                    profiles.contains(IFSConstants.LOGOUT_IDP_GET_PROFILE) &&
                    !isCurrentProviderIDPRole) 
                {
                    FSUtils.debug.message("In GET profile");
                    bLogoutStatus = doHttpGet(currentEntityId);
                } else {
                    FSUtils.debug.error("Provider " + currentEntityId +
                        "doesn't support HTTP profile.");
                    returnAfterCompletion();
                    bLogoutStatus = new FSLogoutStatus(
                        IFSConstants.SAML_RESPONDER);
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Logout completed first round" +
                        " with status : " + bLogoutStatus);
                }
                return bLogoutStatus;
            }
        } else {
            String[] data = { userID };
            LogUtil.error(Level.INFO,LogUtil.LOGOUT_FAILED_REQUEST_IMPROPER,
                          data, ssoToken);
            FSUtils.debug.message(
                "Request not proper. Cannot proceed with single logout");
            returnAfterCompletion();
            return new FSLogoutStatus(IFSConstants.SAML_REQUESTER);
        }
    }
    
    /**
     * Processes the logout request received from soap profile.
     * @param reqLogout the logout request
     * @param currentSessionProvider initial provider with whom to broadcast
     * @param userID who is presently logging out
     * @param sourceEntityId remote provider id
     * @param sessionIndex to be sent as part of logout message
     * @param isWMLAgent determines if response to be sent to AML agent
     * @param isSourceIDP determines the role of the provider
     * @return logout status
     */
    protected FSLogoutStatus processSingleLogoutRequest(
        FSLogoutNotification reqLogout,
        FSSessionPartner currentSessionProvider,
        String userID,
        String sourceEntityId,
        String sessionIndex,
        boolean isWMLAgent,
        String isSourceIDP) 
    {
        FSUtils.debug.message(
            "Entered FSSingleLogoutHandler::processSingleLogoutRequest - SOAP");
        
        if (currentSessionProvider != null) {
            isCurrentProviderIDPRole = currentSessionProvider.getIsRoleIDP();
            remoteEntityId = currentSessionProvider.getPartner();
            setRemoteDescriptor(getRemoteDescriptor(remoteEntityId));
        }
        this.requestLogout = reqLogout;
        this.userID = userID;
        this.sessionIndex = sessionIndex;
        this.isWMLAgent = isWMLAgent;
        if (reqLogout != null) {
            FSUtils.debug.message("FSLogoutNotification formed really well");
            if (currentSessionProvider == null) {
                FSUtils.debug.message(
                    "currentSessionProvider is null. destroy and return");           
                // get ssoToken corresponding to the session index
                Vector sessionObjList = FSLogoutUtil.getSessionObjectList(
                    userID, metaAlias, sessionIndex);
                if ((sessionObjList != null) && !sessionObjList.isEmpty()) {
                    String sessid = 
                        ((FSSession) sessionObjList.get(0)).getSessionID();
                    try {
                        ssoToken = 
                            SessionManager.getProvider().getSession(sessid);
                    } catch (SessionException ex) {
                        // ignore;
                    }
                }

                // handle idp proxy case
                FSLogoutStatus proxyStatus = 
                    handleIDPProxyLogout(sourceEntityId);
                if (proxyStatus != null && 
                    !proxyStatus.getStatus().equalsIgnoreCase(
                        IFSConstants.SAML_SUCCESS))
                {
                    logoutStatus = false;
                }

                FSLogoutUtil.destroyPrincipalSession(
                    userID, 
                    metaAlias,
                    reqLogout.getSessionIndex(),
                    request,
                    response);

                // call multi-federation protocol processing
                int retStatus = handleMultiProtocolLogout(true,null,
                    sourceEntityId);
                if ((retStatus == SingleLogoutManager.LOGOUT_FAILED_STATUS) ||
                    (retStatus == SingleLogoutManager.LOGOUT_PARTIAL_STATUS)) {
                    return new FSLogoutStatus(IFSConstants.LOGOUT_FAILURE);
                } else {
                    return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
                }
            } else {
                // get ssoToken corresponding to the session index
                Vector sessionObjList = FSLogoutUtil.getSessionObjectList(
                    userID, metaAlias, sessionIndex);
                if ((sessionObjList != null) && !sessionObjList.isEmpty()) {
                    String sessid = 
                        ((FSSession) sessionObjList.get(0)).getSessionID();
                    try {
                        ssoToken = 
                            SessionManager.getProvider().getSession(sessid);
                    } catch (SessionException ex) {
                        // ignore;
                    }
                }
                // handle idp proxy case.
                FSLogoutStatus proxyStatus = 
                    handleIDPProxyLogout(sourceEntityId);

                // Check if any of the connections use HTTP GET/Redirect
                String currentEntityId = currentSessionProvider.getPartner();
                isCurrentProviderIDPRole = 
                    currentSessionProvider.getIsRoleIDP();
                if (!supportSOAPProfile(remoteDescriptor)) {
                    return new FSLogoutStatus(IFSConstants.SAML_UNSUPPORTED);
                }
                FSSessionManager sessionManager =
                      FSSessionManager.getInstance(metaAlias);
                FSSession session = sessionManager.getSession(
                sessionManager.getSessionList(userID), 
                    sessionIndex);

                FSUtils.debug.message("FSSLOHandler, process logout case 4");
                FSLogoutUtil.cleanSessionMapPartnerList(
                    userID, currentEntityId, metaAlias, session);

                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Communicate with provider " +
                        currentEntityId + " using soap profile.");
                }
                // In the middle of a SOAP call you can only use
                // SOAP profile
                FSUtils.debug.message("In SOAP profile");
                // This func should take care of initiating
                // next provider also as it has control
                FSLogoutStatus bLogoutStatus = 
                    doIDPSoapProfile(currentEntityId);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Logout completed first round " +
                        "with status : " + bLogoutStatus);
                }

                if (bLogoutStatus.getStatus().equalsIgnoreCase(
                    IFSConstants.SAML_SUCCESS) &&
                    (proxyStatus != null) && 
                    !proxyStatus.getStatus().equalsIgnoreCase(
                        IFSConstants.SAML_SUCCESS))
                {
                    bLogoutStatus = proxyStatus;
                }
                return bLogoutStatus;
            }
        } else {
            String[] data = { userID };
            LogUtil.error(Level.INFO,LogUtil.LOGOUT_FAILED_REQUEST_IMPROPER,
                          data);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Request not proper " +
                "Cannot proceed federation termination");
            }
            return new FSLogoutStatus(IFSConstants.SAML_REQUESTER);
        }
    }
    
    /**
     * Determines if any of the providers with whom we have
     * liveConnections uses either HTTP GET or Redirect profiles.
     * @return <code>true</code> if at least one provider uses http redirect
     *  or get profile; <code>false</code> otherwise.
     */
    private boolean supportSOAPProfile(ProviderDescriptorType currentDesc) 
    {
        FSUtils.debug.message(
            "Entered FSSingleLogoutHandler::supportSOAPProfile");
        if (currentDesc == null) {
            return false;
        }
        List profiles = currentDesc.getSingleLogoutProtocolProfile();
        if (profiles != null &&
            (profiles.contains(IFSConstants.LOGOUT_IDP_SOAP_PROFILE) ||
            profiles.contains(IFSConstants.LOGOUT_SP_SOAP_PROFILE)))
        {
            return true;
        }
        return false;
    }
    
    /**
     * Signs the logout request before sending it to the remote provider.
     * @param msg logout request message to be sent to remote provider
     * @param idAttrName id attribute name
     * @param id id attribute value
     * @return the signed logout request
     * @exception SAMLException, FSMsgException if an error occurred during
     *  the process
     */
    private SOAPMessage signLogoutRequest(
        SOAPMessage msg, String idAttrName, String id)
        throws SAMLException, FSMsgException 
    {
        FSUtils.debug.message(
            "Entered FSSingleLogoutHandler::signLogoutRequest");
        String certAlias = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
        if (certAlias == null || certAlias.length() == 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSingleLogoutHandler::" +
                    " signLogoutRequest: couldn't obtain "
                    + "this site's cert alias.");
            }
            throw new SAMLResponderException(
                FSUtils.bundle.getString(IFSConstants.NO_CERT_ALIAS));
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSingleLogoutHandler::signLogoutRequest" +
                " Provider's certAlias is found: " + certAlias);
        }
        XMLSignatureManager manager = XMLSignatureManager.getInstance();
        Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
                String xpath = "//*[local-name()=\'ProviderID\']";
        manager.signXML(doc,
                certAlias,
                SystemConfigurationUtil.getProperty(
                    SAMLConstants.XMLSIG_ALGORITHM),
                idAttrName,
                id,
                false,
                xpath);
        return FSServiceUtils.convertDOMToSOAP(doc);
    }
    
    private boolean verifyResponseSignature(SOAPMessage msg){
        FSUtils.debug.message(
            "Entered FSLogoutResponse::verifyResponseSignature");
        try {
            X509Certificate cert = KeyUtil.getVerificationCert(
                remoteDescriptor, remoteEntityId, 
                !hostedRole.equalsIgnoreCase(IFSConstants.IDP));
            if (cert == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Logout.verifyResponseSignature" +
                        "couldn't obtain this site's cert.");
                }
                throw new SAMLResponderException(
                    FSUtils.bundle.getString(IFSConstants.NO_CERT));
            }
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
            return manager.verifyXMLSignature(doc, cert);
        } catch (SAMLException e){
            FSUtils.debug.error("Error in verifying response:", e);
            return false;
        }
    }

    private int getMinorVersion(ProviderDescriptorType descriptor) {
        try {
            if (descriptor != null) {
                return FSServiceUtils.getMinorVersion(
                    descriptor.getProtocolSupportEnumeration());
            }
        } catch (Exception e) {
            FSUtils.debug.error("FSSingleLogoutHandler.getMinorVersion:" +
                "Error in getting in minor ver.", e);
        } 
        return IFSConstants.FF_11_PROTOCOL_MINOR_VERSION;
    }

    private void callPostSingleLogoutSuccess(FSLogoutResponse responseLogout,
        String sloProfile)
    {
        // Call SP Adapter postSingleLogoutSuccess
        if (hostedRole != null &&
            hostedRole.equalsIgnoreCase(IFSConstants.SP))
        {
            FederationSPAdapter spAdapter =
                FSServiceUtils.getSPAdapter(hostedEntityId, hostedConfig);
            if (spAdapter != null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSingleLogoutHandler, call postSingleLogoutSuccess");
                }
                try {
                    spAdapter.postSingleLogoutSuccess(
                        hostedEntityId, request, response, userID,
                        requestLogout, responseLogout, sloProfile);
                } catch (Exception e) {
                    // ignore adapter exception
                    FSUtils.debug.error("postSingleLogoutSuccess." + 
                        sloProfile, e);
                }
            }
        }
    }
    
    private FSLogoutStatus handleIDPProxyLogout(String sourceEntityId)
    {
        FSLogoutStatus retStatus = null;
        FSUtils.debug.message("FSSingleLogoutHandler.handleIDPProxyLogout.");
        
        // get sp metaAlias if any
        String proxySPAlias = null;
        boolean isProxy = false;
        BaseConfigType proxySPConfig = null;
        ProviderDescriptorType proxySPDescriptor = null;
        if (hostedRole == IFSConstants.IDP) {
            // see if there is a hosted SP with the same hostedEntityId
            proxySPAlias = IDFFMetaUtils.getMetaAlias(
                realm, hostedEntityId, IFSConstants.SP, null);
            if (proxySPAlias != null) {
                // check to see if original SP is idp proxy enabled
                if (metaManager != null) {
                    try {
                        BaseConfigType sourceSPConfig = 
                            metaManager.getSPDescriptorConfig(
                                realm, sourceEntityId);
                        String enabledString = 
                            IDFFMetaUtils.getFirstAttributeValueFromConfig(
                                sourceSPConfig, IFSConstants.ENABLE_IDP_PROXY);
                        if (enabledString != null && 
                            enabledString.equalsIgnoreCase("true")) 
                        {
                            isProxy = true; 
                        }
                    } catch (IDFFMetaException ie) {
                        // Shouldn't be here
                        isProxy = false;
                    }
                }
            }
        }
        if (isProxy) {
            FSUtils.debug.message(
                "FSSingleLogoutHandler.handleIDPProxyLogout:isProxy is true.");
            // see if there is any session with that proxySPAlias
            try {
                FSSessionManager sessionMgr = 
                    FSSessionManager.getInstance(proxySPAlias);
                FSSession session = sessionMgr.getSession(ssoToken);
                if (session != null) {
                    List partners = session.getSessionPartners();
                    if (partners != null && !partners.isEmpty()) {
                        FSSingleLogoutHandler handler =
                            new FSSingleLogoutHandler();
                        proxySPConfig = metaManager.getSPDescriptorConfig(
                            realm, hostedEntityId);
                        proxySPDescriptor = metaManager.getSPDescriptor(
                            realm, hostedEntityId);
                        handler.setHostedDescriptor(proxySPDescriptor);
                        handler.setHostedDescriptorConfig(proxySPConfig);
                        handler.setRealm(realm);
                        handler.setHostedEntityId(hostedEntityId);
                        handler.setHostedProviderRole(IFSConstants.SP);
                        handler.setMetaAlias(proxySPAlias);
                        Iterator iter = partners.iterator();
                        retStatus = new FSLogoutStatus(
                            IFSConstants.SAML_SUCCESS);
                        // most of the time it will have only one idp partner
                        while (iter.hasNext()) {
                            FSSessionPartner sessionPartner =
                                (FSSessionPartner)iter.next();
                            String curEntityId = sessionPartner.getPartner();
                            if (curEntityId.equals(sourceEntityId) ||
                                !sessionPartner.getIsRoleIDP())
                            {
                                continue;
                            }
                            FSLogoutStatus curStatus = 
                                handler.doIDPProxySoapProfile(request, response,
                                    sessionPartner, userID, 
                                    session.getSessionIndex(), ssoToken);
                            if (!curStatus.getStatus().equalsIgnoreCase(
                                IFSConstants.SAML_SUCCESS))
                            {
                               retStatus = curStatus;
                            }
                        }
                    }
                }
            } catch(Exception e) {
                FSUtils.debug.error("FSSingleLogoutHandler.handleIDPProxy:",e);
                retStatus = new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
            }
        }
        
        return retStatus;
        
    }

    private int handleMultiProtocolLogout(boolean isSOAPInited, 
        String responseXML, String remoteSPId) {
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSLOHandler.handleMultiProtocolLogout: "
                + "isSOAP initiated = " + isSOAPInited + ", response XML="
                + responseXML);
        }
        if (ssoToken == null) {
            try {
                // this is HTTP based protocol, get from HTTP servlet request
                ssoToken = SessionManager.getProvider().getSession(request);
            } catch (SessionException ex) {
                FSUtils.debug.message("FSSLOHandler.handleMPLogout: null", ex);
                return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
            }
        }
        try {
            if (!SessionManager.getProvider().isValid(ssoToken)) {
                return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
            }
        } catch (SessionException ex) {
            FSUtils.debug.message("FSSLOHandler.handleMPLogout: invalid",  ex);
                return SingleLogoutManager.LOGOUT_NO_ACTION_STATUS;
        }
        Set set = new HashSet();
        set.add(ssoToken);
        int currentStatus = (logoutStatus) ?
            SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS :
            SingleLogoutManager.LOGOUT_FAILED_STATUS;
        int retStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
        try {
            String requestXML = (requestLogout == null) ?
                null : requestLogout.toXMLString(true, true);
            String finalRelayState = relayState;
            if ((finalRelayState == null) || (finalRelayState.length() == 0)) {
                finalRelayState = LOGOUT_DONE_URL;
            }
            retStatus = SingleLogoutManager.getInstance().
                doIDPSingleLogout(set, userID, request, response, isSOAPInited,
                    FSLogoutUtil.isIDPInitiatedProfile(singleLogoutProtocol),
                    SingleLogoutManager.IDFF, realm, hostedEntityId,
                    remoteSPId, finalRelayState, requestXML, responseXML, 
                    currentStatus);
        } catch (Exception e) {
            FSUtils.debug.error("FSSLOHandler.doIDPProfile: MP/SOAP", e);
            retStatus = SingleLogoutManager.LOGOUT_FAILED_STATUS;
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSLOHandler.handleMultiProtocolLogout: "
                    + "return status = " + retStatus);
        }
        return retStatus;
    }
}

