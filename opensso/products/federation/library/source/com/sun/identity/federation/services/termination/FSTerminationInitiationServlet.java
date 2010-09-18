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
 * $Id: FSTerminationInitiationServlet.java,v 1.7 2008/12/19 06:50:47 exu Exp $
 *
 */


package com.sun.identity.federation.services.termination;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.FSServiceManager;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import java.util.logging.Level;

/**
 * Used to initiate federation termination.
 */
public class FSTerminationInitiationServlet extends HttpServlet {
    private static IDFFMetaManager metaManager = null;  
    private String termDoneURL = null;
    private String commonErrorPage = null;    
    private HttpServletRequest request = null;
    /**
     * Initializes the servlet.
     * @param config the <code>ServletConfig</code> object that contains 
     *  configutation information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *  the servlet's normal operation.
     */
    public void init(ServletConfig config) 
        throws ServletException
    {
        super.init(config);        
        FSUtils.debug.message("Entered FSTerminationInitiationServlet Init");
        metaManager = FSUtils.getIDFFMetaManager();      
    }
    
    /**
     * Handles the HTTP GET request.
     * @param request <code>HttpServletRequest</code> object that contains the
     *  request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     * the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doGet(
        HttpServletRequest  request,
        HttpServletResponse response)
        throws ServletException, IOException 
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     * @param request <code>HttpServletRequest</code> object that contains the
     *  request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains
     *  the response
     * the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     * the servlet handles the POST request
     * @exception IOException if the request for the POST could not be handled
     */
    public void doPost(
        HttpServletRequest  request,
        HttpServletResponse response)
        throws ServletException, IOException 
    {
        doGetPost(request, response);
    }
    
    /**
     * Invoked when the user clicks on the termination link in the application.
     * @param request <code>HttpServletRequest</code> object that contains the
     *  request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException,IOException if the request could not be
     *  handled
     */
    private void doGetPost(
        HttpServletRequest  request,
        HttpServletResponse response)
        throws ServletException, IOException 
    {
        // Alias processing
        String providerAlias = request.getParameter(IFSConstants.META_ALIAS);                
        if (providerAlias == null || providerAlias.length() < 1) {
            FSUtils.debug.error("Unable to retrieve alias, Hosted Provider. "
                + "Cannot process request");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("aliasNotFound"));
            return;
        }        
        if (metaManager == null) {        
            FSUtils.debug.error("Cannot retrieve hosted descriptor. " +
                "Cannot process request");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FAILED_HOSTED_DESCRIPTOR));
            return;
        }
        ProviderDescriptorType hostedProviderDesc = null;
        BaseConfigType hostedConfig = null;
        String hostedRole = null;
        String hostedEntityId = null;
        String realm = IDFFMetaUtils.getRealmByMetaAlias(providerAlias);
        try {
            hostedRole = metaManager.getProviderRoleByMetaAlias(providerAlias);
            hostedEntityId = metaManager.getEntityIDByMetaAlias(providerAlias);
            if (hostedRole != null &&
                hostedRole.equalsIgnoreCase(IFSConstants.SP))
            {
                hostedProviderDesc =
                    metaManager.getSPDescriptor(realm, hostedEntityId);
                hostedConfig =
                    metaManager.getSPDescriptorConfig(realm, hostedEntityId);
            } else if (hostedRole != null &&
                hostedRole.equalsIgnoreCase(IFSConstants.IDP))
            {
                hostedProviderDesc =
                    metaManager.getIDPDescriptor(realm, hostedEntityId);
                hostedConfig =
                    metaManager.getIDPDescriptorConfig(realm, hostedEntityId);
            }
            if (hostedProviderDesc == null) {
                throw new IDFFMetaException((String) null);
            }
        } catch (IDFFMetaException eam) {
            FSUtils.debug.error(
                "Unable to find Hosted Provider. not process request", eam);
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FAILED_HOSTED_DESCRIPTOR));
            return;
        }
        boolean isIDP = false;
        if (hostedRole != null && hostedRole.equalsIgnoreCase(IFSConstants.IDP))
        {
            isIDP = true;
        }

        if (FSUtils.needSetLBCookieAndRedirect(request, response, isIDP)) {
            return;
        }

        this.request = request;
        setTerminationURL(hostedConfig, providerAlias);
        doTerminationInitiation(request, response, hostedProviderDesc, 
            hostedConfig, realm, hostedEntityId, hostedRole, providerAlias);
        return;
    }
    
    /**
     * Invoked to set some commonly used registration URLs based on hosted
     * provider.
     * @param hostedConfig hosted provider's extended meta.
     * @param metaAlias hosted provider's meta alias
     */
    protected void setTerminationURL (BaseConfigType hostedConfig,
        String metaAlias) {
        termDoneURL = FSServiceUtils.getTerminationDonePageURL(
            request, hostedConfig, metaAlias);
        commonErrorPage = FSServiceUtils.getErrorPageURL(
            request, hostedConfig, metaAlias);
    }

    /**
     * Retrieves session token from <code>HTTPServletRequest</code>
     * object.
     * @param request HTTP request object
     * @return the valid session token from the request object;
     *  <code>null</code> otherwise.
     */
    private Object getValidToken(HttpServletRequest request) 
    {
        FSUtils.debug.message(
            "Entered FSTerminationInitiationServlet::getValidToken");
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object ssoToken = sessionProvider.getSession(request);
            if ((ssoToken == null) || (!sessionProvider.isValid(ssoToken))) {
                FSUtils.debug.error("session token is not valid, " +
                    "redirecting for authentication");
                return null;
            }
            return ssoToken;
        } catch (SessionException e) {
            FSUtils.debug.error("SessionException caught: ", e);
            return null;
        }
    }    
        
    /**
     * Called when a Termination needs to be initiated with a remote provider.
     * @param request <code>HTTPServletRequest</code> object received via a
     *  HTTP Redirect
     * @param response <code>HTTPServletResponse</code> object to send the
     *  response back to user agent
     * @param hostedProviderDesc the provider where termination is initiated
     * @param hostedConfig hosted provider's extended meta
     * @param realm the realm under which the entity resides
     * @param hostedEntityId hosted provider's entity ID
     * @param hostedRole hosted provider's role
     * @param providerAlias hosted provider's meta alias
     */    
    private void doTerminationInitiation(
        HttpServletRequest request,
        HttpServletResponse response,
        ProviderDescriptorType hostedProviderDesc,
        BaseConfigType hostedConfig,
        String realm,
        String hostedEntityId,
        String hostedRole,
        String providerAlias) 
    {

        FSUtils.debug.message(
            "Entered FSTerminationInitiationServlet::doTerminationInitiation");
        try {
            Object ssoToken = getValidToken(request);
            if (ssoToken != null) {
                String remoteEntityId = 
                    request.getParameter(IFSConstants.TERMINATION_PROVIDER_ID);
                if (remoteEntityId == null || remoteEntityId.length() < 1) {
                    FSUtils.debug.error(
                        "Provider Id not found, display error page");
                    FSServiceUtils.showErrorPage(
                        response,
                        commonErrorPage,
                        IFSConstants.TERMINATION_NO_PROVIDER,
                        IFSConstants.CONTACT_ADMIN);
                    return;
                }                
                // session token is valid, ProviderId available
                FSServiceManager instSManager = FSServiceManager.getInstance();
                if (instSManager != null) {
                    FSUtils.debug.message("FSServiceManager Instance not null");
                    String remoteProviderRole = IFSConstants.SP;
                    FSAccountFedInfo fedinfo = null;
                    if (hostedRole.equalsIgnoreCase( IFSConstants.SP)) {
                       remoteProviderRole = IFSConstants.IDP;
                       FSSessionManager sessManager =
                           FSSessionManager.getInstance(providerAlias);
                       FSSession ssoSession = sessManager.getSession(ssoToken);
                       if (ssoSession != null) {
                           if (!ssoSession.getOneTime()) {
                               fedinfo = ssoSession.getAccountFedInfo();
                           }
                       }
                    }

                    FSFedTerminationHandler handlerObj =
                        instSManager.getFedTerminationHandler(
                            remoteEntityId,
                            remoteProviderRole,
                            SessionManager.getProvider().getPrincipalName(
                                ssoToken),
                            realm,
                            hostedEntityId,
                            providerAlias); 
                    

                    if (handlerObj != null) {
                        handlerObj.setHostedDescriptor(hostedProviderDesc);
                        handlerObj.setHostedDescriptorConfig(hostedConfig);
                        handlerObj.setHostedProviderRole(hostedRole);
                        handlerObj.setMetaAlias(providerAlias);
                        handlerObj.setRealm(realm);
                        handlerObj.setHostedEntityId(hostedEntityId);
                        if (fedinfo != null) {
                            handlerObj.setAccountInfo(fedinfo);
                        }
                        boolean bStatus = 
                            handlerObj.handleFederationTermination(request,
                                                                   response, 
                                                                   ssoToken);
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "handleFederationTermination status is : " + 
                                bStatus);
                        }
                        return;                        
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Unable to get termination " +
                                "handler. User account Not valid");
                        }
                    }
                } else{
                    FSUtils.debug.message("FSServiceManager Instance null");
                }
                FSServiceUtils.returnLocallyAfterOperation(
                    response, termDoneURL, false,
                    IFSConstants.TERMINATION_SUCCESS,
                    IFSConstants.TERMINATION_FAILURE);
                return;
            } else{
                FSServiceUtils.redirectForAuthentication(
                    request, response, providerAlias);
                return;
            }          
        } catch(IOException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("IOException in doTerminationInitiation",
                    e);
            }
        } catch(SessionException ex) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "SessionException in doTerminationInitiation", ex);
            }
        }
        FSServiceUtils.returnLocallyAfterOperation(
            response, termDoneURL, false,
            IFSConstants.TERMINATION_SUCCESS, IFSConstants.TERMINATION_FAILURE);
        return;
    }
    
}   // FSTerminationInitiationServlet
