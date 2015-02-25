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
 * $Id: FSIntersiteTransferService.java,v 1.6 2008/08/29 04:57:16 exu Exp $
 *
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.common.IDPEntries;
import com.sun.identity.federation.message.common.IDPEntry;
import com.sun.identity.federation.message.FSIDPList;
import com.sun.identity.federation.message.FSScoping;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSSignatureManager;
import com.sun.identity.federation.services.util.FSSignatureException;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServlet;

import java.util.logging.Level;

/**
 * Gets called to send <code>AuthnRequest</code> to <code>IDP</code>.
 */
public class FSIntersiteTransferService extends HttpServlet {
    
    String framedLoginPageURL = null;
    
    private void redirectToCommonDomain(
        HttpServletRequest request, 
        HttpServletResponse response, 
        String requestID)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSIntersiteTransferService."
                + "redirectToCommonDomain: Called");
        }
        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
        try {
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            HttpSession session = request.getSession(true);
            Set cotSet = 
                   (Set)session.getAttribute(IFSConstants.SESSION_COTSET_ATTR);
            CircleOfTrustManager cotManager = new CircleOfTrustManager();
            if(cotSet == null){
                cotSet = cotManager.getAllCirclesOfTrust(realm);
                if(cotSet != null){
                    session.setAttribute(IFSConstants.SESSION_COTSET_ATTR, 
                                         cotSet);
                }
                if(cotSet == null || cotSet.isEmpty()){
                    FSUtils.debug.error("FSIntersiteTransferService. redirect"
                        + "ToCommonDomain: No CommonDomain metadata found");
                    String[] data = 
                        { FSUtils.bundle.getString("noCommonDomainMetadata") };
                    LogUtil.error(Level.INFO,
                                  LogUtil.COMMON_DOMAIN_META_DATA_NOT_FOUND,
                                  data);
                    //response.sendRedirect(framedLoginPageURL);
                    FSUtils.forwardRequest(request, response,
                                           framedLoginPageURL);
                    return;
                }
            }
            if(cotSet.isEmpty()){
                FSUtils.debug.error("FSIntersiteTransferService."
                    + "redirectToCommonDomain: No more CommonDomain left");
                String[] data = 
                        { FSUtils.bundle.getString("noCommonDomainMetadata") };
                    LogUtil.error(Level.INFO,
                                  LogUtil.COMMON_DOMAIN_META_DATA_NOT_FOUND,
                                  data);
                //response.sendRedirect(framedLoginPageURL);
                FSUtils.forwardRequest(request, response, framedLoginPageURL);
                return;
            }
            Iterator iter = cotSet.iterator();
            String cotName =(String)iter.next();
            cotSet.remove(cotName);
            session.setAttribute(IFSConstants.SESSION_COTSET_ATTR, cotSet);
            String readerServiceURL = 
                cotManager.getCircleOfTrust(realm, cotName)
                    .getIDFFReaderServiceURL();
            if(readerServiceURL != null){
                StringBuffer redirectURL = new StringBuffer(300);
                StringBuffer returnURL = request.getRequestURL();
                returnURL.append("?")
                         .append(IFSConstants.AUTH_REQUEST_ID)
                         .append("=")
                         .append(URLEncDec.encode(requestID));
                returnURL.append("&")
                         .append(IFSConstants.META_ALIAS)
                         .append("=")
                         .append(URLEncDec.encode(metaAlias));
                redirectURL.append(readerServiceURL);
                redirectURL.append("?");
                redirectURL.append(IFSConstants.LRURL);
                redirectURL.append("=");
                redirectURL.append(URLEncDec.encode(
                    returnURL.toString()));
                String url = redirectURL.toString();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSIntersiteTransferService."
                        + "redirectToCommonDomain: "
                        + "Redirecting to check for PrefferedIDP @:" + url);
                }
                response.setHeader("Location", url);
                response.sendRedirect(url);
                return;
            }
        } catch(COTException e){
            FSUtils.debug.error("FSIntersiteTransferService."
                + "redirectToCommonDomain: "
                + "COTException occured while trying to "
                + "redirect to the CommonDomain: " , e);
            try {
                //response.sendRedirect(framedLoginPageURL);
                FSUtils.forwardRequest(request, response, framedLoginPageURL);
            } catch(Exception ex) {
                FSUtils.debug.error("FSIntersiteTransferService."
                    + "redirectToCommonDomain: IOException : " , ex);
            }
            return;
        } catch(IOException e){
            FSUtils.debug.error("FSIntersiteTransferService."
                + "redirectToCommonDomain: IOException"
                + " occured while trying to redirect to the CommonDomain: ", e);
            return;
        }
    }
    
    private String findRequestID(HttpServletRequest request) {
        FSUtils.debug.message(
            "FSIntersiteTransferService.findRequestID: Called");
        String requestID = request.getParameter(IFSConstants.AUTH_REQUEST_ID);
        if (requestID == null || requestID.length() == 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSIntersiteTransferService.findRequestID:"
                    + "No requestID in the query string");
            }
            return null;
        } else {
            return requestID;
        }
        
    }
    
    private String signAndReturnQueryString(
        String queryString, 
        String certAlias
    )
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSIntersiteTransferService."
                + "signAndReturnQueryString: Called");
        }
        
        if(queryString == null || queryString.length() == 0){
            FSUtils.debug.error("FSIntersiteTransferService."
                + "signAndReturnQueryString: " 
                + FSUtils.bundle.getString("nullInput"));
            return null;
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSIntersiteTransferService."
                + "signAndReturnQueryString: certAlias: " + certAlias);
            }
        }
        
        if(queryString == null || queryString.length() == 0){
            FSUtils.debug.error("FSIntersiteTransferService."
                + "signAndReturnQueryString: " 
                + FSUtils.bundle.getString("nullInput"));
            return null;
        }
        
        FSSignatureManager manager = FSSignatureManager.getInstance();
        String sigAlg = IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA;
        if(manager.getKeyProvider().getPrivateKey(certAlias).
            getAlgorithm().equals(IFSConstants.KEY_ALG_RSA))
        {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                "FSIntersiteTransferService.signAndReturnQueryString: "
                + "private key algorithm is: RSA");
            }
            sigAlg = IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA;                                    
        } else if(manager.getKeyProvider().getPrivateKey(certAlias).
            getAlgorithm().equals(IFSConstants.KEY_ALG_DSA))
        {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                "FSIntersiteTransferService.signAndReturnQueryString: "
                + "private key algorithm is: DSA");
            }
            sigAlg = IFSConstants.ALGO_ID_SIGNATURE_DSA_JCA; 
        } else {
            FSUtils.debug.error(
                "FSIntersiteTransferService.signAndReturnQueryString: "
                + "private key algorithm is not supported");
            return null;
        }
        
        byte[] signature = null;

        if(sigAlg == null || sigAlg.length() == 0){
            sigAlg = IFSConstants.DEF_SIG_ALGO_JCA;
        }
        if(queryString.charAt(queryString.length()-1) != '&'){
            queryString = queryString + "&";
        }
        String algoId = null;
        if(sigAlg.equals(IFSConstants.ALGO_ID_SIGNATURE_DSA_JCA)) {
            algoId = IFSConstants.ALGO_ID_SIGNATURE_DSA;
        } else if (sigAlg.equals(IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA)) {
            algoId = IFSConstants.ALGO_ID_SIGNATURE_RSA;
        } else {
            FSUtils.debug.error(
                "FSIntersiteTransferService.signAndReturnQueryString: "
                + "Invalid signature algorithim");
            return null;
        }
        queryString = 
            queryString + "SigAlg=" + URLEncDec.encode(algoId);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSIntersiteTransferService.signAndReturnQueryString: "
                + "Querystring to be signed: " + queryString);
        }
        try {
            signature = manager.signBuffer(queryString, certAlias, sigAlg);
        } catch(FSSignatureException se){
            FSUtils.debug.error("FSIntersiteTransferService."
                + "signAndReturnQueryString: FSSignatureException occured "
                + "while signing query string: " 
                + se.getMessage());
            return null;
        }
        if(signature == null){
            FSUtils.debug.error("FSIntersiteTransferService."
                + "signAndReturnQueryString: Signature generated is null");
            return null;
        }
        String encodedSig = Base64.encode(signature);
        queryString = queryString + "&" + "Signature=" 
                        + URLEncDec.encode(encodedSig);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSIntersiteTransferService."
                + "signAndReturnQueryString:Signed Querystring: " 
                + queryString);
        }
        return queryString;
    }
    
    /**
     * Generates <code>AuthnRequest</code> and sends it to <code>IDP</code>.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException,IOException if error occurred
     */
    public void doGet(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException 
    {
        FSUtils.debug.message("FSIntersiteTransferService.doGet: Called");
        /**
         * Check to see if there is a need to set lb cookie.
         * This is for the use case that AuthnRequest is not created by the 
         * preLogin process and lb cookie wasn't set there.
         */ 
        if (FSUtils.needSetLBCookieAndRedirect(request, response, false)) {
            return;
        }
        try {
            IDPDescriptorType idpDescriptor = null;
            
            String metaAlias =  request.getParameter(IFSConstants.META_ALIAS);
            if (metaAlias == null || metaAlias.length() == 0) {
                metaAlias = FSServiceUtils.getMetaAlias(request);
            }
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            String hostEntityId = metaManager.getEntityIDByMetaAlias(metaAlias);
            String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            if ((request == null) ||(response == null)) {
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("nullInputParameter"));
                return;
            }
            
            String qs = request.getQueryString();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSIntersiteTransferService.doGet: "
                    + "QueryString Received from CommonDomain: " + qs);
            }
            
            String requestID = findRequestID(request);
            
            if (requestID == null){
                //throw error page
                FSUtils.debug.error("FSIntersiteTransferService.doGet: "
                    + FSUtils.bundle.getString("nullInputParameter"));
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("nullInputParameter"));
                return;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSIntersiteTransferService.doGet: "
                    + "RequestID found: " + requestID);
            }
            FSSessionManager sessionMgr = FSSessionManager.getInstance(
                metaAlias);
            FSAuthnRequest authnRequest = sessionMgr.getAuthnRequest(requestID);
            
            if (authnRequest == null) {
                FSUtils.debug.error("FSIntersiteTransferService.doGet: "
                    + FSUtils.bundle.getString("invalidRequestId"));
                String[] data = { FSUtils.bundle.getString("invalidRequestId")};
                LogUtil.error(Level.INFO,"INVALID_AUTHN_REQUEST",data);
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("invalidRequestId"));
                return;
            }
            String resourceUrl = authnRequest.getRelayState();
            String baseURL = FSServiceUtils.getBaseURL(request);
            framedLoginPageURL = FSServiceUtils.getCommonLoginPageURL(
                metaAlias, resourceUrl, null, request, baseURL);        
            
           String idpID = FSUtils.findPreferredIDP(realm, request);
            if (idpID == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSIntersiteTransferService.doGet: "
                        + "No Preffered IDP found in this Common Domain. "
                        + "Try to find PrefferedIDP in other common domains");
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSIntersiteTransferService.doGet: "
                        + "RequestID :" + requestID);
                }
                redirectToCommonDomain(request, response, requestID);
                return;
            } else {
                idpDescriptor = metaManager.getIDPDescriptor(realm, idpID);
                if (idpDescriptor == null) {
                    FSUtils.debug.error("FSIntersiteTransferService.doGet: "
                        + FSUtils.bundle.getString("noTrust"));
                    String[] data = { idpID };
                    LogUtil.error(Level.INFO,"PROVIDER_NOT_TRUSTED",data);
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                        FSUtils.bundle.getString("noTrust"));
                    return;
                }
                HttpSession session = request.getSession(true);
                session.removeAttribute(IFSConstants.SESSION_COTSET_ATTR);
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSIntersiteTransferService.doGet: "
                    + "Preffered IDP found:" + idpID);           
            }
            
            
            sessionMgr.setIDPEntityID(requestID, idpID);
            // Set the authn request version here
            int minorVersion = FSServiceUtils.getMinorVersion(
                 idpDescriptor.getProtocolSupportEnumeration());
            authnRequest.setMinorVersion(minorVersion);
            authnRequest.getAuthnContext().setMinorVersion(minorVersion);
            SPDescriptorType hostDesc = 
                metaManager.getSPDescriptor(realm, hostEntityId);
            BaseConfigType hostConfig = 
                metaManager.getSPDescriptorConfig(realm, hostEntityId);
            if (IDFFMetaUtils.getBooleanAttributeValueFromConfig(
                    hostConfig, IFSConstants.ENABLE_AFFILIATION))
            {
                Set affiliations = 
                    metaManager.getAffiliateEntity(realm, idpID);
                if (affiliations != null && !affiliations.isEmpty()) {
                    AffiliationDescriptorType affiliateDescriptor =
                        (AffiliationDescriptorType)
                            affiliations.iterator().next();
                    authnRequest.setAffiliationID(
                        affiliateDescriptor.getAffiliationID());
                }
            }

            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION &&
                IDFFMetaUtils.getBooleanAttributeValueFromConfig(
                    hostConfig, IFSConstants.ENABLE_IDP_PROXY))
            {
                FSScoping scoping = new FSScoping();
                scoping.setProxyCount(Integer.parseInt(
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostConfig, IFSConstants.IDP_PROXY_COUNT)));
                List proxyIDPs = IDFFMetaUtils.getAttributeValueFromConfig(
                    hostConfig, IFSConstants.IDP_PROXY_LIST);
                if (proxyIDPs != null && !proxyIDPs.isEmpty()) {
                    Iterator iter = proxyIDPs.iterator();
                    ArrayList list = new ArrayList();
                    while(iter.hasNext()) {
                        IDPEntry entry = 
                            new IDPEntry((String)iter.next(),null, null);
                        list.add(entry);
                    }
                    IDPEntries entries = new IDPEntries(list); 
                    FSIDPList idpList = new FSIDPList(entries, null);
                    scoping.setIDPList(idpList);
                }
                authnRequest.setScoping(scoping);
            }

            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSIntersiteTransferService.doGet: "
                    + "AuthnRequest:" + authnRequest.toXMLString(true, true));
            }
            
            if (authnRequest.getProtocolProfile().equals(
                            IFSConstants.SSO_PROF_BROWSER_ART) ||
                authnRequest.getProtocolProfile().equals(
                            IFSConstants.SSO_PROF_BROWSER_POST)) 
            {
                handleBrowserArtifactPOSTIST(request, 
                                            response, 
                                            authnRequest, 
                                            idpDescriptor,
                                            hostDesc,
                                            hostConfig);
                return;
            } else if(authnRequest.getProtocolProfile().equals(
                                IFSConstants.SSO_PROF_WML_POST)) {
                handleWMLIST(request, response, authnRequest, idpDescriptor);
                return;
            }
            FSUtils.debug.error("FSIntersiteTransferService.doGet: "
                + "Unknown Protocol Profile");
            String[] data = { FSUtils.bundle.getString("invalidAuthnRequest") };
            LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_REQUEST,data);
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("invalidAuthnRequest"));
            return;
        } catch(Exception e){
            FSUtils.debug.error("FSIntersiteTransferService.doGet: ", e);
            try {
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("Exception"));
                return;
            } catch(IOException ioe){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSIntersiteTransferService.doGet: "
                    + FSUtils.bundle.getString("sendFailed")+ioe.getMessage());
                }
            }
        }
    }
    
    /**
     * Generates <code>AuthnRequest</code> and sends it to <code>IDP</code>.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException,IOException if error occurred
     */
    public void doPost(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws javax.servlet.ServletException, java.io.IOException 
    {
        doGet(request, response);
    }
    
    private void handleBrowserArtifactPOSTIST(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest,
        IDPDescriptorType idpDescriptor,
        SPDescriptorType hostDesc,
        BaseConfigType hostConfig
    )
    {
        FSUtils.debug.message(
            "FSIntersiteTransferService.handleBrowserArtifactPOSTIST: Called");
        try {
            if ((request == null) ||
                (response == null) ||
                (authnRequest == null) ||
                (idpDescriptor == null))
            {
                FSUtils.debug.error("FSIntersiteTransferService.doGet: "
                    + FSUtils.bundle.getString("nullInputParameter"));
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("nullInputParameter"));
                return;
            }
            String targetURL = idpDescriptor.getSingleSignOnServiceURL();
            if (targetURL == null) {
                return;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSIntersiteTransferService."
                    + "handleBrowserArtifactPOSTIST: "
                    + "Destination URL to send AuthnRequest: " 
                    + targetURL);
            }
            
            // Call SP adapter in case of browser GET
            FederationSPAdapter spAdapter = FSServiceUtils.getSPAdapter(
                authnRequest.getProviderId(), hostConfig);
            if (spAdapter != null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSIntersiteTransferService, " + 
                        "GET, call spAdapter.preSSOFederationRequest");
                }
                try {
                    spAdapter.preSSOFederationRequest(
                        authnRequest.getProviderId(),
                        idpDescriptor.getId(),
                        request, response, authnRequest);
                } catch (Exception e) {
                    // log run time exception in Adapter
                    // implementation, continue
                    FSUtils.debug.error("FSIntersiteTransferService,"
                        + "GET SPAdapter.preSSOFederationRequest:", e);
                }
            }
            StringBuffer tmp = new StringBuffer(1000);
            String queryString = authnRequest.toURLEncodedQueryString();
            if (queryString == null) {
                FSUtils.debug.error("FSIntersiteTransferService."
                    + "handleBrowserArtifactPOSTIST: "
                    + FSUtils.bundle.getString("invalidRequest"));
                String[] data = { FSUtils.bundle.getString("invalidRequest") };
                LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_REQUEST,data);
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("invalidRequest"));
                return;
            }
            
            //signAuthnRequest If specified
            String certAlias = 
                IDFFMetaUtils.getFirstAttributeValueFromConfig(
                    hostConfig, IFSConstants.SIGNING_CERT_ALIAS);
            boolean authnRequestSigned = hostDesc.isAuthnRequestsSigned();
            if (FSServiceUtils.isSigningOn()) {
                if (authnRequestSigned) {
                    queryString = signAndReturnQueryString(queryString,
                                  certAlias);
                    
                    if (queryString == null){
                        FSUtils.debug.error("FSIntersiteTransferService."
                            + "handleBrowserArtifactPOSTIST: "
                            + "AuthnRequest signing failed");
                        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                        FSUtils.bundle.getString("signFailed"));
                        return;
                    }
                }
            }
            
           if (targetURL.indexOf("?") != -1) {
                tmp.append(targetURL).append("&").append(queryString);
            } else {
                tmp.append(targetURL).append("?").append(queryString);
            }
            String[] data =
                { targetURL };
            LogUtil.access(Level.FINER, "REDIRECT_TO",data); 
            String redirecto = tmp.toString();
            if (redirecto.length() > IFSConstants.URL_MAX_LENGTH) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSIntersiteTransferService."
                        + "handleBrowserArtifactPOSTIST: Redirection URL"
                        + " length exceeding the URL MAX length restriction. "
                        + "Switching to form post");
                }
                
                if (authnRequestSigned) {
                    authnRequest.signXML(certAlias);
                }
                sendAuthnRequestPost(response, targetURL, authnRequest);
                return;
            }
            response.setStatus(response.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", redirecto);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSIntersiteTransferService."
                    + "handleBrowserArtifactPOSTIST: "
                    + "Sending AuthnRequest by http-redirect to: " 
                    + targetURL);
            }
            response.sendRedirect(redirecto);
        } catch(Exception ex){
            FSUtils.debug.error("FSIntersiteTransferService."
                + "handleBrowserArtifactPOSTIST:" , ex);
            try {
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("Exception"));
            } catch(IOException ioe){
                FSUtils.debug.error("FSIntersiteTransferService."
                    + "handleBrowserArtifactPOSTIST: "
                    + FSUtils.bundle.getString("sendFailed") , ioe);
            }
            return;
        }
    }
    
    private void handleWMLIST(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest,
        IDPDescriptorType idpDescriptor
    )
    {
        try {
            FSUtils.debug.message(
                "FSIntersiteTransferService.handleWMLIST: Called");
            if ((request == null) ||
                (response == null) ||
                (authnRequest == null) ||
                (idpDescriptor == null))
            {
                FSUtils.debug.error("FSIntersiteTransferService.handleWMLIST: "
                    + FSUtils.bundle.getString("nullInputParameter"));
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("nullInputParameter"));
                return;
            }
            String targetURL = idpDescriptor.getSingleSignOnServiceURL();
            if (targetURL == null) {
                return;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSIntersiteTransferService.handleWMLIST: "
                    + "Destination URL to send AuthnRequest: " + targetURL);
            }
            String[] data = { targetURL };
            LogUtil.access(Level.INFO,LogUtil.REDIRECT_TO, data);
            sendWMLB64Post(response, targetURL, authnRequest);
            return;
        } catch(Exception ex) {
            FSUtils.debug.error("FSIntersiteTransferService.handleWMLIST: "
                + FSUtils.bundle.getString("Exception"), ex);
            try {
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("Exception"));
            } catch(IOException ioe){
                FSUtils.debug.error("FSIntersiteTransferService.handleWMLIST: "
                    + FSUtils.bundle.getString("sendFailed"), ioe);
            }
            return;
        }
    }
    
    private boolean sendWMLB64Post(
        HttpServletResponse response, 
        String destination, 
        FSAuthnRequest authnRequest
    )
    {
        FSUtils.debug.message(
            "FSIntersiteTransferService:sendWMLB64Post: Called");
        try {
            response.setContentType("text/vnd.wap.wml");
            PrintWriter out = response.getWriter();
            out.println("<!DOCTYPE wml PUBLIC \"-//WAPFORUM//DTD WML 1.1//EN\""
                + " \"http://www.wapforum.org/DTD/wml_1.1.xml\">");
            out.println("<wml>");
            out.println("<card id=\"request\" title=\"SP Request\">");
            out.println("<onevent type=\"onenterforward\">");
            out.println("<go method=\"post\" href=\"" + destination + "\">");
            out.println("<postfield name=\"" 
                + IFSConstants.POST_AUTHN_REQUEST_PARAM 
                + "\" " + "value=\"" 
                + authnRequest.toBASE64EncodedString() + "\"/>");
            out.println("</go>");
            out.println("</onevent>");
            out.println("<onevent type=\"onenterbackward\">");
            out.println("<prev/>");
            out.println("</onevent>");
            out.println("<p>");
            out.println("Contacting IdP. Please wait....");
            out.println("</p>");
            out.println("</card>");
            out.println("</wml>");
            out.close();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSIntersiteTransferService:sendWMLB64Post: "
                    + "Base64 Encoded AuthnRequest at the Sender: " 
                    + authnRequest.toBASE64EncodedString());
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSIntersiteTransferService:sendWMLB64Post: "
                    + "AuthnRequest sent successfully to: " + destination);
            }
            return true;
        } catch(Exception ex){
            FSUtils.debug.error("FSIntersiteTransferService:sendWMLB64Post:",
                ex);
            return false;
        }
    }
    
    protected void sendAuthnRequestPost(
        HttpServletResponse response, 
        String destination, 
        FSAuthnRequest authnRequest
    ) 
    {
        FSUtils.debug.message(
            "FSIntersiteTransferService.sendAuthnRequestPost: Called");
        try {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<HTML>");
            out.println("<BODY Onload=\"document.Request.submit()\">");
            out.println("<FORM NAME=\"Request\" METHOD=\"POST\" ACTION=\"" 
                + destination + "\">");
            out.println("<INPUT TYPE=\"HIDDEN\" NAME=\"" 
                + IFSConstants.POST_AUTHN_REQUEST_PARAM 
                + "\" " + "VALUE=\"" 
                + authnRequest.toBASE64EncodedString() + "\"/>");
            out.println("</FORM>");
            out.println("</BODY></HTML>");
            out.close();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSIntersiteTransferService:sendAuthnRequestPost: "
                    + "Base64 Encoded AuthnRequest at the Sender: " 
                    + authnRequest.toBASE64EncodedString()
                    + "\nFSIntersiteTransferService:sendAuthnRequestPost: "
                    + "AuthnRequest sent successfully to: " 
                    + destination);
            }
            return;
        } catch(Exception ex){
            FSUtils.debug.error(
                "FSIntersiteTransferService:sendAuthnRequestPost:", ex);
            return;
        }
    }
}
