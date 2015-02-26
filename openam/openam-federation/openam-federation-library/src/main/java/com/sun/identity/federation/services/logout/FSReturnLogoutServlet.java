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
 * $Id: FSReturnLogoutServlet.java,v 1.6 2008/12/19 06:50:47 exu Exp $
 *
 */

package com.sun.identity.federation.services.logout;

import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.protocol.Status;
import java.util.logging.Level;

/**
 * Processes <code>ID-FF</code> single logout return (logout response).
 */
public class FSReturnLogoutServlet extends HttpServlet {
    IDFFMetaManager metaManager = null;
    private static String COMMON_ERROR_PAGE = "";   
    private String univId  = null;
    /**
     * Initiates the servlet.
     * @param config the <code>ServletConfig</code> object that contains 
     *              configutation information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *              the servlet's normal operation.
     */
    public void init(ServletConfig config)
    throws ServletException 
    {
        super.init(config);
        FSUtils.debug.message("FSReturnLogoutServlet Initializing...");
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Handles the HTTP GET request.
     *
     * @param request an <code>HttpServletRequest</code> object that contains 
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *  the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doGet(HttpServletRequest  request,
                    HttpServletResponse response)
        throws ServletException, IOException 
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     *
     * @param request an <code>HttpServletRequest</code> object that contains 
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the POST request
     * @exception IOException if the request for the POST could not be handled
     */
    public void doPost(HttpServletRequest request,
                    HttpServletResponse response)
        throws ServletException, IOException
    {
        doGetPost(request, response);
    }
    
    /**
     * Processes logout response.
     * @param request an <code>HttpServletRequest</code> object that contains 
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the request
     * @exception IOException if the request could not be handled
     */
    private void doGetPost(HttpServletRequest  request,
                        HttpServletResponse response)
        throws ServletException, IOException 
    {
        FSUtils.debug.message("FSReturnLogoutServlet doGetPost...");
        // Alias processing
        String providerAlias = request.getParameter(IFSConstants.META_ALIAS);
        if (providerAlias == null || providerAlias.length() < 1) {
            providerAlias = FSServiceUtils.getMetaAlias(request);
        }
        if (providerAlias == null || providerAlias.length() < 1) {
            FSUtils.debug.message("Unable to retrieve alias, Hosted" +
                " Provider. Cannot process request");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("aliasNotFound"));
            return;
        }
        
        Object ssoToken = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            ssoToken = sessionProvider.getSession(request);
            if ((ssoToken == null) || (!sessionProvider.isValid(ssoToken))) {
                FSUtils.debug.message(
                    "FSReturnLogoutRequest: Unable to get principal");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("nullSSOToken"));
                return;
            }
            univId = sessionProvider.getPrincipalName(ssoToken);
        } catch (SessionException ssoExp) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSReturnLogoutRequest: Unable to get principal", ssoExp);
            }
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("nullSSOToken"));
            return;
        }
               
        if (metaManager == null) {
            FSUtils.debug.error("Failed to get meta manager");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FEDERATION_FAILED_META_INSTANCE));
            return;
        }
        String realm = IDFFMetaUtils.getRealmByMetaAlias(providerAlias);
        String hostedRole = null;
        String hostedEntityId = null;
        BaseConfigType hostedConfig = null;
        
        try {
            hostedRole = metaManager.getProviderRoleByMetaAlias(providerAlias);
            hostedEntityId = metaManager.getEntityIDByMetaAlias(providerAlias);
            if (hostedRole != null) {
                if (hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                    hostedConfig = metaManager.getIDPDescriptorConfig(
                        realm, hostedEntityId);
                } else if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                    hostedConfig = metaManager.getSPDescriptorConfig(
                        realm, hostedEntityId);
                }
            }
            if (hostedConfig == null) {
                throw new IDFFMetaException((String) null);
            }
        } catch (IDFFMetaException e){
            FSUtils.debug.error("Failed to get Hosted Provider");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FAILED_HOSTED_DESCRIPTOR));
            return;
        }
        setLogoutURL(request, hostedConfig, providerAlias);
        // Here we will need to
        //   1. verify response signature
        //   2. verify response status
        //   3. retrieve registration request Id from Map
        //   4. if status success then do locally else not do locally and
        //   5. show status page or LRURL if found in MAP (eg intersiteTransfer)
        
        FSLogoutResponse logoutResponse = null;
        try {
            logoutResponse =
                FSLogoutResponse.parseURLEncodedRequest(request);
        } catch (FSMsgException e) {
            FSServiceUtils.showErrorPage(response,
                COMMON_ERROR_PAGE,
                IFSConstants.LOGOUT_REQUEST_IMPROPER,
                IFSConstants.LOGOUT_FAILED);
            return;
        } catch (SAMLException e) {
            FSServiceUtils.showErrorPage(response,
                COMMON_ERROR_PAGE,
                IFSConstants.LOGOUT_REQUEST_IMPROPER,
                IFSConstants.LOGOUT_FAILED);
            return;
        }
 
        String remoteEntityId = logoutResponse.getProviderId();

        ProviderDescriptorType remoteDesc = null;
        boolean isRemoteIDP = false;
        try {
            if (hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                remoteDesc = metaManager.getSPDescriptor(
                    realm, remoteEntityId);
            } else if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                remoteDesc = metaManager.getIDPDescriptor(
                    realm, remoteEntityId);
                isRemoteIDP = true;
            }
        } catch (IDFFMetaException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutReturnServlet.doGetPost:", e);
            }
        }
        if (remoteDesc == null) {
            FSServiceUtils.showErrorPage(response,
                COMMON_ERROR_PAGE,
                IFSConstants.LOGOUT_REQUEST_IMPROPER,
                IFSConstants.LOGOUT_FAILED);
            return;
        }
        boolean bVerify = true;
        if (FSServiceUtils.isSigningOn()) {
            try {
                bVerify = verifyResponseSignature(
                    request, remoteDesc, remoteEntityId, isRemoteIDP);
            } catch (SAMLException e){
                bVerify = false;
            } catch (FSException e){
                bVerify = false;
            }
        }

        Status status = logoutResponse.getStatus();
        String logoutStatus = status.getStatusCode().getValue();

        // remove session partner in case of logout success or this is IDP
        if (logoutStatus.equalsIgnoreCase(IFSConstants.SAML_SUCCESS) ||
            !isRemoteIDP)
        {
            FSLogoutUtil.removeCurrentSessionPartner(
                providerAlias, remoteEntityId, ssoToken, univId);
        }

        if (bVerify) {
            // check the status on response and update entry
            // in ReturnSessionManager only if it is failure
            if (!logoutStatus.equalsIgnoreCase(IFSConstants.SAML_SUCCESS)) {
                FSReturnSessionManager localManager =
                    FSReturnSessionManager.getInstance(providerAlias);
                if (localManager != null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "update status of logout to failure " +
                            " in session manager");
                    }
                    localManager.setLogoutStatus(logoutStatus,univId);
                } else {
                    FSUtils.debug.message("Cannot get FSReturnSessionManager");
                }
                FSUtils.debug.error(
                    "FSReturnLogoutServlet, failed logout response " + 
                    logoutStatus);
                String[] data = { univId };
                LogUtil.error(Level.INFO, LogUtil.LOGOUT_FAILED, data,ssoToken);
                FSLogoutUtil.sendErrorPage(request, response, providerAlias);
                return;
            }
        } else {
            FSUtils.debug.error(
                "FSReturnLogoutServlet " +
                "Signature on logout response is invalid" +
                "Cannot proceed logout");
            String[] data = { univId };
            LogUtil.error(Level.INFO,LogUtil.INVALID_SIGNATURE,data,ssoToken);
            FSServiceUtils.showErrorPage(
                response,
                COMMON_ERROR_PAGE,
                IFSConstants.LOGOUT_REQUEST_IMPROPER,
                IFSConstants.LOGOUT_FAILED);
            return;
        }
        
        StringBuffer processLogout = new StringBuffer();
        request.setAttribute("logoutSource", "remote");
        processLogout.append(IFSConstants.SLO_VALUE)
            .append("/")
            .append(IFSConstants.META_ALIAS)
            .append(providerAlias);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("About to get RequestDispatcher for " +
                processLogout.toString());
        }
        RequestDispatcher dispatcher =
            getServletConfig().getServletContext().getRequestDispatcher(
                processLogout.toString()) ;
        if ( dispatcher == null ) {
            FSUtils.debug.message("RequestDispatcher is null");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Unable to find " + processLogout +
                    "\ncalling sendErrorPage ");
            }
            FSLogoutUtil.sendErrorPage(
                request,
                response,
                providerAlias);
            return;
        }
        dispatcher.forward(request, response);
        return;
    }
    
    /** 
     * Verifies the logout response signature received from the remote end.
     * @param request <code>HttpServletRequest</code> containing the signed 
     *  logout response
     * @param remoteDescriptor remote provider descriptor
     * @param remoteEntityId remote provider's entity id
     * @param isRemoteIDP whether the remote provider is an IDP or not
     * @return <code>true</code> if the signature is verified; <code>null</code>
     *  otherwise.
     * @exception SAMLException, FSException
     */
    private boolean verifyResponseSignature(
        HttpServletRequest request,
        ProviderDescriptorType remoteDescriptor,
        String remoteEntityId,
        boolean isRemoteIDP
    ) throws SAMLException, FSException 
    {
        FSUtils.debug.message(
            "Entered FSReturnLogoutServlet::verifylogoutSignature");
                    
        // Verify the signature on the request
        X509Certificate cert = KeyUtil.getVerificationCert(
            remoteDescriptor, remoteEntityId, isRemoteIDP);
        if (cert == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSReturnLogoutServlet.verifyRegistrationSignature: " +
                    "couldn't obtain this site's cert.");
            }
            throw new SAMLResponderException(
                FSUtils.bundle.getString(IFSConstants.NO_CERT));
        }
        boolean isValidSign = 
            FSSignatureUtil.verifyRequestSignature(request, cert);        
        if (!isValidSign) {
            FSUtils.debug.error("Logout response is not properly signed");
            return false;
        } else {
            FSUtils.debug.message("Logout response is properly signed");
            return true;
        }        
    }
    
    protected void setLogoutURL(HttpServletRequest request, 
        BaseConfigType hostedConfig, String metaAlias) {
        COMMON_ERROR_PAGE = FSServiceUtils.getErrorPageURL(
            request, hostedConfig, metaAlias);
        if (FSUtils.debug.messageEnabled()) {            
            FSUtils.debug.message("COMMON_ERROR_PAGE : " + COMMON_ERROR_PAGE);
        }                        
    }
    
}   // FSReturnLogoutServlet
