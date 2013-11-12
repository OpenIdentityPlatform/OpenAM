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
 * $Id: FSSSOAndFedService.java,v 1.8 2009/06/19 02:45:50 bigfatrat Exp $
 *
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSServiceManager;
import com.sun.identity.federation.services.FSSOAPService;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.common.Extension;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonIDFFSvc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletConfig;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * <code>IDP</code> Single Sign On Service.
 */
public class FSSSOAndFedService  extends HttpServlet {

    private static FSSOAPService soapService = FSSOAPService.getInstance();
    private static MessageFactory msgFactory = null;
    private static IDFFMetaManager metaManager = null;

    private static FedMonAgent agent = MonitorManager.getAgent();
    private static FedMonIDFFSvc idffSvc =
	MonitorManager.getIDFFSvc();
    
    /**
     * Initializes the servlet.
     * @param config <code>ServletConfig</code> object
     * @exception ServletException if the initialization failed
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            msgFactory = MessageFactory.newInstance();
        } catch (SOAPException ex) {
            FSUtils.debug.error(
                "FSSSOAndFedService.init: create message factory failed.", ex);
            throw new ServletException(ex.getMessage());
        }
        metaManager = FSUtils.getIDFFMetaManager();
    }
   
    /**
     * Processes single sign on request.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException, IOException if an error occurred
     */
    public void doGet(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException 
    {
        FSUtils.debug.message("FSSSOAndFedService.doGet: Called");
        if ((request == null) ||(response == null)) {
            FSUtils.debug.error("FSSSOAndFedService: " 
                + FSUtils.bundle.getString("nullInputParameter"));
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("nullInputParameter"));
            return;
        }
        
        if (FSUtils.needSetLBCookieAndRedirect(request, response, true)) {
            return;
        }

        // check for post authn
        boolean bPostAuthn = false;
        boolean bLECP = false;
        String authnIndicator = 
            request.getParameter(IFSConstants.AUTHN_INDICATOR_PARAM);
        if (authnIndicator == null) {
            FSUtils.debug.message(
                "FSSSOAndFedService.doGet:Pre Authentication SSO");
            bPostAuthn = false;
        } else {
            FSUtils.debug.message(
                "FSSSOAndFedService.doGet:Post Authentication SSO");
            bPostAuthn = true;
            
            String lecpIndicator = 
                request.getParameter(IFSConstants.LECP_INDICATOR_PARAM);
            if (lecpIndicator == null) {
                FSUtils.debug.message(
                    "FSSSOAndFedService.doGet:non LECP request");
                bLECP = false;
            } else {
                FSUtils.debug.message(
                    "FSSSOAndFedService.doGet:post Authn LECP request");
                bLECP = true;
            }
            //Start Change
            
            String requestId =
                request.getParameter(IFSConstants.AUTH_REQUEST_ID);            
            String hostEntityId = 
                request.getParameter(IFSConstants.PROVIDER_ID_KEY);
            String authnContext = 
                request.getParameter(IFSConstants.AUTHN_CONTEXT);
            String realm = request.getParameter(IFSConstants.REALM);
            String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
            FSSessionManager sessionService = FSSessionManager.getInstance(
                metaAlias);

            FSAuthnRequest authnRequest = 
                sessionService.getAuthnRequest(requestId);
            if (authnRequest == null) {
                FSUtils.debug.message(
                    "FSSSOAndFedService.doGet: authnRequest is null");
            }
            if ((authnContext == null) || (authnContext.length() == 0)) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedService.doGet: authnContext is null. " +
                        "Using default password");
                }
                authnContext = IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD;
            }
            //End Change
            if (authnRequest != null &&
                realm != null &&
                realm.length() != 0 &&
                hostEntityId != null &&
                hostEntityId.length() != 0 &&
                authnContext != null &&
                authnContext.length() != 0)
            {
                handleAuthnRequest(request, 
                                    response,
                                    authnRequest, 
                                    realm,
                                    hostEntityId, 
                                    bLECP,
                                    authnContext);
                return;
            } else {
                FSUtils.debug.error("FSSSOAndFedService.doGet: "
                    + "AuthnRequest not found in FSSessionManager");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("nullInput"));
                return;
            }
        }
        // obtain AuthnRequest message
        //decode and create FSAuthnRequest object
        FSAuthnRequest authnRequest = null;
        try {
            authnRequest = FSAuthnRequest.parseURLEncodedRequest(request);
            if (authnRequest == null){
                FSUtils.debug.error("FSSSOAndFedService: "
                    + FSUtils.bundle.getString("invalidAuthnRequest"));
                String[] data = 
                    { FSUtils.bundle.getString("invalidAuthnRequest")};
                LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_REQUEST,data);
                response.sendError(response.SC_BAD_REQUEST,
                    FSUtils.bundle.getString("invalidAuthnRequest"));
                return;
            }
        } catch(FSException e){
            FSUtils.debug.error("FSSSOAndFedService: " +
                FSUtils.bundle.getString("invalidAuthnRequest") +
                ", queryString=" +
                request.getQueryString(), e);
            String[] data = 
                { FSUtils.bundle.getString("invalidAuthnRequest")};
            LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_REQUEST,data);
            response.sendError(response.SC_BAD_REQUEST,
                FSUtils.bundle.getString("invalidAuthnRequest"));
            return;
        }
        String metaAlias = null;
        String realm = null;
        String hostEntityId = null;
        IDPDescriptorType hostedDesc = null;
        BaseConfigType hostedConfig = null;
        try {
            metaAlias = FSServiceUtils.getMetaAlias(request);
            realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            hostEntityId = metaManager.getEntityIDByMetaAlias(metaAlias);
            hostedDesc = metaManager.getIDPDescriptor(realm, hostEntityId);
            hostedConfig = metaManager.getIDPDescriptorConfig(
                realm, hostEntityId);
        } catch (Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSSOAndFedService: couldn't obtain hosted entity id:",e);
            }
        }

        handleAuthnRequest(request, 
                            response,
                            authnRequest, 
                            bPostAuthn,
                            bLECP,
                            realm,
                            hostEntityId,
                            metaAlias,
                            hostedDesc,
                            hostedConfig);
        return;
    }
    
    /**
     * Processes single sign on POST request.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException, IOException if an error occurred
     */
    public void doPost(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException 
    {
        FSUtils.debug.message("FSSSOAndFedService.doPost: Called");
        if ((request == null) ||(response == null)) {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("nullInputParameter"));
            return;
        }
        
        if (FSUtils.needSetLBCookieAndRedirect(request, response, true)) {
            return;
        }
        // Check if it's an LECP request
        if (isLECPRequest(request)) {
            // TODO: assume auth framework will understand this param
            String useForward = 
                (String) request.getAttribute(Constants.FORWARD_PARAM);
            if (useForward != null &&
                useForward.equals(Constants.FORWARD_YES_VALUE)) {
                // this is a forward POST after authentication, need to
                // use GET instead of POST here
                FSUtils.debug.message(
                    "FSSSOAndFedService.doPost: LECP forward");
                this.doGet(request,response);
            } else {
               try {
                   MimeHeaders mimeHeaders = SAMLUtils.getMimeHeaders(request);
                   ServletInputStream sInputStream = request.getInputStream();
                   SOAPMessage soapMessage =
                       msgFactory.createMessage(mimeHeaders, sInputStream);
                   this.onMessage(request, response, soapMessage);
               } catch (SOAPException se) {
                   throw new ServletException(se);
               }
           }
           return;
        } 
        
        // obtain AuthnRequest message
        String enocodedAuthnRequest = 
            request.getParameter(IFSConstants.POST_AUTHN_REQUEST_PARAM);
        if (enocodedAuthnRequest == null){
            doGet(request, response);
            return;
        }
        
        enocodedAuthnRequest = enocodedAuthnRequest.replace(' ', '\n');
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSSOAndFedService.doPost: "
                + "BASE64 encoded AuthnRequest at the RECEIVER: " 
                + enocodedAuthnRequest);
        }
        //decode and create FSAuthnRequest object
        FSAuthnRequest authnRequest = null;
        try {
            authnRequest = 
                FSAuthnRequest.parseBASE64EncodedString(enocodedAuthnRequest);
            if (authnRequest == null){
                FSUtils.debug.error("FSSSOAndFedService: "
                    + FSUtils.bundle.getString("invalidAuthnRequest"));
                String[] data = 
                    { FSUtils.bundle.getString("invalidAuthnRequest") };
                LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_REQUEST,data);
                response.sendError(response.SC_BAD_REQUEST,
                    FSUtils.bundle.getString("invalidAuthnRequest"));
                return;
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOAndFedService: " +
                        "AuthnRequest received:" +
                        authnRequest.toXMLString());
                }
            }
        } catch(FSException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedService: "
                    + FSUtils.bundle.getString("invalidAuthnRequest"), e);
            }
            response.sendError(response.SC_BAD_REQUEST,
                FSUtils.bundle.getString("invalidAuthnRequest"));
            return;
        }
        
        String metaAlias = null;
        String realm = null;
        String hostEntityId = null;
        IDPDescriptorType hostedDesc = null;
        BaseConfigType hostedConfig = null;
        try {
            metaAlias = FSServiceUtils.getMetaAlias(request);
            realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            hostEntityId = metaManager.getEntityIDByMetaAlias(metaAlias);
            hostedDesc = metaManager.getIDPDescriptor(realm, hostEntityId);
            hostedConfig = metaManager.getIDPDescriptorConfig(
                realm, hostEntityId);
        } catch (Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSSOAndFedService: couldn't obtain hosted entity id:",e);
            }
        }

        handleAuthnRequest(request,
                            response, 
                            authnRequest, 
                            false,
                            false,
                            realm,
                            hostEntityId,
                            metaAlias,
                            hostedDesc,
                            hostedConfig);
        return;
    }

    /**
     * Check if this is an LECP authentication request.
     * 
     * @param request <code>HttpServletRequest</code> object
     * @return <code>true</code> if it's an LECP request; <code>false</code>
     *  otherwise.
     */
    private boolean isLECPRequest(HttpServletRequest request) {

        String lecpHeader = request.getHeader(IFSConstants.LECP_HEADER_NAME);
        if (lecpHeader != null) {
            FSUtils.debug.message("FSSSOAndFedService: is LECP request.");
            return true;
        }
        String contentType = request.getHeader("content-type");
        if (contentType != null && contentType.startsWith("text/xml")) {
            return true;
        }
        return false;
    }
    
    private void handleAuthnRequest(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FSAuthnRequest authnRequest, 
        String realm,
        String hostEntityId, 
        boolean bLECP,
        String authnContext
    ) {
        // post authn process
        FSUtils.debug.message("FSSSOAndFedService.handleAuthnRequest: Called");
        IDPDescriptorType hostedDesc = null;
        BaseConfigType hostedConfig = null;
        String metaAlias = null;
        try {
            hostedDesc = metaManager.getIDPDescriptor(realm, hostEntityId);
            hostedConfig = metaManager.getIDPDescriptorConfig(
                realm, hostEntityId);
            if (hostedConfig != null) {
                metaAlias = hostedConfig.getMetaAlias();
            }
        } catch (Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedService.handleAuthnRequest: "
                + "Couldn't obtain hosted meta:", e);
            }
        }
        /* Not really useful.
        String nameRegisDone = 
            request.getParameter(IFSConstants.NAMEREGIS_INDICATOR_PARAM);
        boolean doNameRegis = false;
        String doNameRegisStr = 
            IDFFMetaUtils.getFirstAttributeValueFromConfig(
                hostedConfig, IFSConstants.ENABLE_REGISTRATION_AFTER_SSO);
        if (doNameRegisStr != null && doNameRegisStr.equalsIgnoreCase("true")) {
            doNameRegis = true;
        }
        */
        Object ssoToken = null;
        String userID = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            ssoToken = sessionProvider.getSession(request);
            if (ssoToken == null) {
                FSUtils.debug.error("FSSSOAndFedService.handleAuthnRequest: "
                    + "session token is null.");
                return;
            } else if(!sessionProvider.isValid(ssoToken)) {
                FSUtils.debug.error("FSSSOAndFedService.handleAuthnRequest: "
                    + "session token is not valid.");
                return;
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedService.handleAuthnRequest: "
                        + "session token is valid.");
                }
            }
            
            FSSessionManager sessionManager = FSSessionManager.getInstance(
                metaAlias);
            FSSession session = sessionManager.getSession(ssoToken);
            userID = sessionProvider.getPrincipalName(ssoToken);
            if (session == null) {
                session = new FSSession(sessionProvider.getSessionID(ssoToken));
                session.setAuthnContext(authnContext);
                sessionManager.addSession(
                    userID, session);
            } else {
                session.setAuthnContext(authnContext);
            }
        } catch(SessionException se) {
            FSUtils.debug.error("FSSSOAndFedService.handleAuthnRequest: ", se);
            return;
        }
        try {
            if (userID == null) {
                LogUtil.error(Level.INFO,LogUtil.USER_NOT_FOUND,null, ssoToken);
                return;
            }
            String remoteEntityID = authnRequest.getProviderId();
            FSAccountManager acctMng = FSAccountManager.getInstance(
                metaAlias);
            acctMng.readAccountFedInfo(userID,remoteEntityID);
            /* Not useful at all.  Commented out for now.
            if (doNameRegis &&
                (nameRegisDone == null || 
                    !nameRegisDone.equals(IFSConstants.TRUE)) && 
                !authnRequest.getFederate()) 
            {
                // have to do nameregis now 
                Map queryMap = new HashMap();
                queryMap.put(IFSConstants.AUTH_REQUEST_ID,
                    authnRequest.getRequestID());
                queryMap.put(IFSConstants.PROVIDER_ID_KEY,hostEntityId);
                queryMap.put(IFSConstants.AUTHN_CONTEXT,authnContext);
                FSServiceManager instSManager = FSServiceManager.getInstance();
                if (instSManager != null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSSSOAndFedService.handleAuthnRequest:" +
                            "FSServiceManager Instance not null");
                    }
                    try {
                        FSNameRegistrationHandler handlerObj = 
                            instSManager.getNameRegistrationHandler(
                                realm, remoteEntityID, IFSConstants.SP); 
                        SPDescriptorType remoteProviderDesc = 
                            metaManager.getSPDescriptor(realm,remoteEntityID);
                        if (handlerObj != null) {
                            handlerObj.setHostedDescriptor(hostedDesc);
                            handlerObj.setHostedDescriptorConfig(hostedConfig);
                            handlerObj.setMetaAlias(metaAlias);
                            handlerObj.setHostedEntityId(hostEntityId);
                            handlerObj.handleNameRegistration(
                                request,
                                response, 
                                ssoToken,
                                (HashMap)queryMap);
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSSOAndFedService.handleAuthnRequest:" +
                                    "Control returned from name registration");
                            }
                            if (!FSServiceUtils.isRegisProfileSOAP(userID,
                                            remoteEntityID,
                                            remoteProviderDesc,
                                            metaAlias,
                                            hostedDesc))
                            {
                                return;
                            }
                        }
                    } catch (Exception ex){
                        FSUtils.debug.error(
                            "FSSSOAndFedService.handleAuthnRequest:Error in " +
                            "invoking Name registration. returning.", ex);
                        return;
                    }
                }
            }
            */
        } catch(FSAccountMgmtException exp) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSSOAndFedService:: handleAuthnRequest()" +
                    " No account information avialable for user. " +
                    "So no invocation " + " of name registration. ", exp);
            }
        }
        handleAuthnRequest(request, 
                            response, 
                            authnRequest, 
                            true, 
                            bLECP,
                            realm,
                            hostEntityId,
                            metaAlias,
                            hostedDesc,
                            hostedConfig);
    }
    
    private void handleAuthnRequest(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FSAuthnRequest authnRequest, 
        boolean bPostAuthn, 
        boolean bLECP,
        String realm,
        String hostEntityId,
        String metaAlias,
        IDPDescriptorType hostedDesc,
        BaseConfigType hostedConfig)
    {
        FSUtils.debug.message("FSSSOAndFedService.handleAuthnRequest: Called");
        Object session = null;
        try {
            SessionProvider provider = SessionManager.getProvider();
            session = provider.getSession(request);
            if ((session != null) && (provider.isValid(session))) {
                MultiProtocolUtils.addFederationProtocol(session, 
                    SingleLogoutManager.IDFF);
            }
        } catch (SessionException e) {
            FSUtils.debug.warning("FSSSOFedService.handleAuthnRequest: hub", e);
        }
        
        try {
            if (!bPostAuthn && !authnRequest.getIsPassive()){
                FSSessionManager sessionService = FSSessionManager.getInstance(
                    metaAlias);
                sessionService.setAuthnRequest(authnRequest.getRequestID(),
                    authnRequest);
            } else {
                // remove it from authn request map
                FSSessionManager sessionService = FSSessionManager.getInstance(
                    metaAlias);
                sessionService.removeAuthnRequest(authnRequest.getRequestID());
            }
            // handle sso
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedService.handleAuthnRequest: "
                    + "Trying to handle AuthnRequest message: " 
                    + authnRequest.toXMLString());

                List extensions = authnRequest.getExtensions();
                if ((extensions != null) && (!extensions.isEmpty())) {
                    FSUtils.debug.message(
                        "FSSSOAndFedService.handleAuthnRequest: " +
                        "AuthnRequest extensions: " +
                        ((Extension)extensions.get(0)).getAttributeMap());;
                }
            }
            FSServiceManager sm = FSServiceManager.getInstance();
            FSSSOAndFedHandler handler = null;
            if (!bLECP) {
                handler = 
                    sm.getSSOAndFedHandler(
                        request, response, authnRequest, realm);
            } else {
                handler = sm.getLECPProfileHandler(
                    request, response, authnRequest, realm);
            }
            if (handler == null){
                FSUtils.debug.error("FSSSOAndFedService.handleAuthnRequest: "
                    + "could not create SSOAndFedHandler");
                String[] data = {
                        FSUtils.bundle.getString("requestProcessingFailed") };
                LogUtil.error(
                    Level.INFO,LogUtil.AUTHN_REQUEST_PROCESSING_FAILED, data,
                    session);  
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("requestProcessingFailed"));
                return;
            }
            

            if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
                idffSvc.incIdAuthnRqt();
            }

            handler.setHostedEntityId(hostEntityId);
            handler.setMetaAlias(metaAlias);
            handler.setHostedDescriptor(hostedDesc);
            handler.setHostedDescriptorConfig(hostedConfig);
            handler.setRealm(realm);
            handler.processAuthnRequest(authnRequest, bPostAuthn);
            return;
        } catch(Exception se) {
            FSUtils.debug.error("FSSSOAndFedService: Exception occured:", se);
            try {
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    se.getMessage());
            } catch(IOException ex){
                FSUtils.debug.error("FSSSOAndFedService: Exception occured "
                    + ex.getMessage());
            }
            return;
        }
    }
    
    /**
     * Default constructor.
     */
    public FSSSOAndFedService() {
    }
    
    /**
     * SOAP JAXM Listener implementation for LECP AuthnRequest.
     *
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @param message SOAP message that contains LECP request
     */
    public void onMessage(HttpServletRequest request,
                          HttpServletResponse response,
                          SOAPMessage message) 
    {
        FSUtils.debug.message("FSSSOAndFedService.onMessage: Called");
        try {
            Element elt = soapService.parseSOAPMessage(message);
            if (elt == null){
                FSUtils.debug.error("FSSSOAndFedService.onMessage: "
                    + "Error in processing. Invalid SOAPMessage");
                response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                returnSOAPMessage(
                    soapService.formSOAPError(
                        "Server", "cannotProcessRequest", null),
                    response);
                return;
            }
            String eltTagName = (elt.getTagName().trim());
            String ns = elt.getNamespaceURI().trim();
            String nodeName = elt.getLocalName().trim();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedService.onMessage: "
                    + "tagName: " + eltTagName + " namespaceUri: " + ns
                    + " localName: " + nodeName);
            }
            if (nodeName.equalsIgnoreCase("AuthnRequest") &&
                (ns.equalsIgnoreCase(IFSConstants.libertyMessageNamespaceURI))
                || (ns.equalsIgnoreCase(IFSConstants.FF_12_XML_NS)))
            {
                SOAPMessage retMessage = null;
                try {
                    FSAuthnRequest authnRequest = new FSAuthnRequest(elt);
                    String metaAlias = FSServiceUtils.getMetaAlias(request);
                    IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
                    String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
                    String hostEntityId = metaManager.getEntityIDByMetaAlias(
                        metaAlias);
                    IDPDescriptorType hostedDesc =
                        metaManager.getIDPDescriptor(realm, hostEntityId);
                    BaseConfigType hostedConfig =
                        metaManager.getIDPDescriptorConfig(realm, hostEntityId);
                    FSSessionManager sessionService = 
                        FSSessionManager.getInstance(metaAlias);
                    sessionService.setAuthnRequest(
                        authnRequest.getRequestID(), authnRequest);
                    handleLECPRequest(request, response, authnRequest,
                        hostedDesc, hostedConfig, realm, hostEntityId,
                        metaAlias);
                    retMessage = null;
                } catch(Exception e){
                    FSUtils.debug.error("FSSSOAndFedService.onMessage: "
                        + "Error in processing lecp AuthnRequest:", e);
                    response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                    returnSOAPMessage(
                        soapService.formSOAPError(
                            "Server", "cannotProcessRequest", null),
                        response);
                    return;
                }
                returnSOAPMessage(retMessage, response);
                return;
             } else {
                FSUtils.debug.error(
                    "FSSSOAndFedService.onMessage: Invalid SOAP Request:" +
                    nodeName);
             }
        } catch(Exception e) {
            FSUtils.debug.error("FSSSOAndFedService.onMessage: "
                + "Error in processing Request: Exception occured: ", e);
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            java.io.ByteArrayOutputStream strm =
                new java.io.ByteArrayOutputStream();
            e.printStackTrace(new java.io.PrintStream(strm));
            FSUtils.debug.error(strm.toString());
            returnSOAPMessage(
                soapService.formSOAPError(
                    "Server", "cannotProcessRequest", null),
                response);
            return;
        }
        returnSOAPMessage(
            soapService.formSOAPError("Server", "cannotProcessRequest", null),
            response);
        return;
    }

    /**
     * Handles LECP AuthnRequest.
     *
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @param authnRequest <code>FSAuthnRequest</code> object
     * @param hostedDesc hosted identity provider's meta descriptor
     * @param hostedConfig hosted identity provider's extended meta
     * @param realm The realm under which the entity resides
     * @param hostEntityId hosted identity provider's entity id
     * @param metaAlias hosted identity provider's meta alias
     */
    private void handleLECPRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest,
        IDPDescriptorType hostedDesc,
        BaseConfigType hostedConfig,
        String realm,
        String hostEntityId,
        String metaAlias) 
    {
        FSUtils.debug.message("FSSSOAndFedService.handleLECPRequest:");
        try {
            // handle sso
            FSServiceManager sm = FSServiceManager.getInstance();
            FSSSOLECPProfileHandler handler = sm.getLECPProfileHandler(
                request, response, authnRequest, realm);
            handler.setHostedEntityId(hostEntityId);
            handler.setMetaAlias(metaAlias);
            handler.setHostedDescriptor(hostedDesc);
            handler.setHostedDescriptorConfig(hostedConfig);
            handler.setRealm(realm);
            handler.processLECPAuthnRequest(authnRequest);
        } catch(Exception se) {
            FSUtils.debug.error("FSSSOAndFedService.handleLECPRequest: " +
                "processing LECP request failed." + se);
            return;
        }
    }
   
    /**
     * Forms and Returns SOAP message to the requested client.
     *
     * @param msg <code>SOAPMessage</code> to be returned
     * @param response <code>HttpServletResponse</code> object
     */
    private void returnSOAPMessage(SOAPMessage msg, 
        HttpServletResponse response) 
    {
        try {
            if (msg != null) {
                SAMLUtils.setMimeHeaders(msg.getMimeHeaders(), response);
                ServletOutputStream servletoutputstream =
                    response.getOutputStream();
                msg.writeTo(servletoutputstream);
                servletoutputstream.flush();
                return;
            } else {
                response.flushBuffer();
                return;
            }
        } catch(Exception e) {
            FSUtils.debug.error("FSSSOAndFedService.returnSOAPMessage: "
                + "Exception::", e);
            return;
        }
    }

}
