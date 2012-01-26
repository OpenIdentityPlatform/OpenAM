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
 * $Id: FSPostLogin.java,v 1.6 2008/07/31 00:55:33 exu Exp $
 *
 */

package com.sun.identity.federation.login;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;

import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;

import com.sun.identity.federation.services.FSLoginHelper;
import com.sun.identity.federation.services.FSLoginHelperException;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.logout.FSTokenListener;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;

import com.sun.identity.shared.encode.URLEncDec;

import com.sun.liberty.LibertyManager;

/**
 * This class defines methods which will be invoked post
 * Single Sign-On/Federation.
 */

public class FSPostLogin {
    
    private String federationPage = null;
    private String consentPage = null;
    private String errorPage = null;
    private static IDFFMetaManager metaManager = null;
    private boolean isIDP = false;
    private String providerRole = null;
    private String entityID = null;
    private String realm = null;
    
    static {
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Retreives and forwards request to URL after login.
     *
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     */
    public void doPostLogin(HttpServletRequest request,
        HttpServletResponse response) 
    {
        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        String sso = request.getParameter(IFSConstants.SSOKEY);
        String lrURL = request.getParameter(IFSConstants.LRURL);
        String showFederatePage =
            request.getParameter(IFSConstants.FEDERATEKEY);
        String returnURL = null;
        
        if (metaAlias == null) {
            metaAlias = FSServiceUtils.getMetaAlias(request);
            String rqst = (new StringBuffer())
                .append(request.getRequestURL().toString())
                .append(IFSConstants.QUESTION_MARK)
                .append(request.getQueryString()).toString();
            lrURL = getValueFromURL(rqst, IFSConstants.LRURL);
            sso = getValueFromURL(rqst, IFSConstants.SSOKEY);
            // this is for LECP, we need to map the random id back to
            // original URL stored in session manager
            FSSessionManager sessMgr = FSSessionManager.getInstance(metaAlias);
            String relayStateURL = sessMgr.getRelayState(lrURL);
            if (relayStateURL != null) {
                sessMgr.removeRelayState(lrURL);
                lrURL = relayStateURL;
            }
        }
        
        try {
            setMetaInfo(metaAlias,request);
        } catch (FSPostLoginException fsexp) {
            sendResponse(request, response,errorPage);
        }
        
        if (lrURL == null || lrURL.length() <= 0) {
            lrURL = LibertyManager.getHomeURL(realm, entityID, providerRole);
        }
        if ((sso != null  && sso.length() > 0
                && sso.equalsIgnoreCase(IFSConstants.SSOVALUE)) ||
             isIDP) 
        {
            // means in middle of SSO show consent to introduction page
            try {
                Set cotSet = LibertyManager.getListOfCOTs(
                    realm, entityID, providerRole);
                if (cotSet != null && !cotSet.isEmpty()) {
                    if(cotSet.size() <= 1) {
                        String cotSelected = (String)cotSet.iterator().next();
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSPostLogin::doPostLogin "
                                + "single cot present is " + cotSelected);
                        }
                        returnURL =
                            doConsentToIntro(metaAlias,lrURL,cotSelected);
                    } else {
                        returnURL = new StringBuffer().append(consentPage)
                            .append(IFSConstants.QUESTION_MARK)
                            .append(IFSConstants.META_ALIAS)
                            .append(IFSConstants.EQUAL_TO).append(metaAlias)
                            .append(IFSConstants.AMPERSAND)
                            .append(IFSConstants.LRURL)
                            .append(IFSConstants.EQUAL_TO)
                            .append(URLEncDec.encode(lrURL)).toString();
                    }
                } else {
                    if (FSUtils.debug.messageEnabled())  {
                        FSUtils.debug.message("FSPostLogin::doPostLogin: No "
                            + "COTS configured. redirecting to lrurl "
                            + lrURL);
                    }
                    returnURL = lrURL;
                }
            } catch (FSPostLoginException fsexp) {
                sendResponse(request, response, errorPage);
            }
        } else if (showFederatePage != null && 
                    !showFederatePage.equalsIgnoreCase(
                        IFSConstants.FEDERATEVALUE)) 
        {
            //show do u want to federate page with list of IDP's
            String providerID = LibertyManager.getEntityID(metaAlias);
            String univId = LibertyManager.getUser(request);
            if(univId == null) {
                String gotoUrl = new StringBuffer()
                    .append(request.getRequestURL())
                    .append(IFSConstants.QUESTION_MARK)
                    .append(IFSConstants.AMPERSAND)
                    .append(request.getQueryString()).toString();
                String preLoginURL = LibertyManager.getLoginURL(request);
                sendResponse(request, response, preLoginURL + "&goto=" +
                    URLEncDec.encode(gotoUrl));
                return;
            }
            Set providerSet = LibertyManager.getProvidersToFederate(
                realm, providerID, providerRole,univId);
            if (providerSet != null &&  providerSet.size() != 0 &&
                federationPage != null) 
            {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPostLogin::doPostLogin: Found "
                        + "provider(s) to federated with " + providerSet);
                }
                returnURL = new StringBuffer().append(federationPage).append
                        (IFSConstants.QUESTION_MARK)
                        .append(IFSConstants.META_ALIAS)
                        .append(IFSConstants.EQUAL_TO)
                        .append(metaAlias).append(IFSConstants.AMPERSAND)
                        .append(IFSConstants.LRURL)
                        .append(IFSConstants.EQUAL_TO)
                        .append(URLEncDec.encode(lrURL)).toString();
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPostLogin::doPostLogin: No "
                        + "provider(s) to federated with or "
                        + "federationPage null. Redirecting to LRURL "
                        + lrURL);
                }
                returnURL = lrURL;
            }
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSPostLogin::doPostLogin. No cotkey or "
                    + "Federatekey found");
            }
            returnURL = lrURL;
        }
        setTokenListenerAndSessionInfo(request, metaAlias);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPostLogin::doPostLogin return url "
                + returnURL);
        }
        sendResponse(request, response,returnURL);
    }
    
    /**
     * Sets the Session Listener and session information.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param metaAlias the provider alias string.
     */
    private void setTokenListenerAndSessionInfo(
        HttpServletRequest request, String metaAlias) 
    {
        Object ssoToken = null;
        String sessionID = null;
        String userID = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            ssoToken = sessionProvider.getSession(request);
            sessionID = sessionProvider.getSessionID(ssoToken);
            userID = sessionProvider.getPrincipalName(ssoToken);
            sessionProvider.addListener(
                ssoToken, new FSTokenListener(metaAlias));
        } catch(SessionException ssoExp) {
            FSUtils.debug.error("FSPostLogin::setTokenListenerAndSessionInfo "
                + "Failed during trying to add token Listener:", ssoExp);
            return;
        }
        FSSessionManager sessionManager =
            FSSessionManager.getInstance(metaAlias);
        FSSession session = sessionManager.getSession(userID, sessionID);
        if(session == null)  {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSPostLogin::setTokenListenerAndSessionInfo. "
                    + "No existing session found for user " + userID
                    + " And SessionID: " + sessionID
                    + " Creating a new Session");
            }
            FSSession newSession = new FSSession(sessionID);
            sessionManager.addSession(userID, newSession);
        }
    }
    
    /**
     * Forwards request to the Return URL.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param request the <code>HttpServletResponse</code> object.
     * @param returnURL the url to forward to.
     */
    private void sendResponse(HttpServletRequest request,
        HttpServletResponse response,
        String returnURL) 
    {
        try {
            FSUtils.forwardRequest(request, response, returnURL);
        } catch (Exception exp) {
            FSUtils.debug.error(
                "FSPreLogin:: sendError Error during sending error page");
        }
    }
    
    /**
     * Returns the introduction URL.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @return a String the Introduction URL.
     * @exception FSPostLoginException on error.
     */
    public String doConsentToIntro(HttpServletRequest request)
        throws FSPostLoginException 
    {
        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        String targetURL = request.getParameter(IFSConstants.LRURL);
        String cotSelected = request.getParameter(IFSConstants.COTKEY);
        return doConsentToIntro(metaAlias,targetURL,cotSelected);
    }
    
    /**
     * Retrieves the entityID of the provider.
     *
     * @param metaAlias the provider alias.
     * @param request the <code>HttpServletRequest</code> object.
     * @exception FSPostLoginException on error.
     */
    private void setMetaInfo(String metaAlias,HttpServletRequest request)
        throws FSPostLoginException 
    {
        if (metaManager != null) {
            BaseConfigType hostedConfig = null;
            try {
                providerRole = 
                    metaManager.getProviderRoleByMetaAlias(metaAlias);
                entityID = metaManager.getEntityIDByMetaAlias(
                    metaAlias);
                realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
                if (providerRole != null &&
                    providerRole.equals(IFSConstants.IDP))
                {
                    isIDP = true;
                    hostedConfig = metaManager.getIDPDescriptorConfig(
                        realm, entityID);
                } else if (providerRole != null &&
                    providerRole.equalsIgnoreCase(IFSConstants.SP))
                {
                    hostedConfig = metaManager.getSPDescriptorConfig(
                        realm, entityID);
                }
            } catch (IDFFMetaException ie) {
                FSUtils.debug.error("FSPostLogin::setMetaInfo: exception:",ie);
            }
            consentPage = FSServiceUtils.getConsentPageURL(
                request, hostedConfig);
            federationPage = FSServiceUtils.getDoFederatePageURL(
                request, hostedConfig);
            errorPage = FSServiceUtils.getErrorPageURL(
                request, hostedConfig, metaAlias);
        } else {
            FSUtils.debug.error("FSPostLogin::setMetaInfo "
                + "could not get alliane manager handle "
                + "Cannot proceed so throwing error page");
            throw new FSPostLoginException(
                "FSPostLogin:: could not get meta manager handle.");
        }
    }
    
    /**
     * Returns the Introduction Writer URL.
     *
     * @param metaAlias the provider alias.
     * @param targetURL the url the writer servlet will redirect to.
     * @param cotSelected the name of the Circle fo Trust.
     * @return the writer url.
     * @exception FSPostLoginException on error.
     */
    private String doConsentToIntro(String metaAlias,String targetURL,
        String cotSelected )
        throws FSPostLoginException 
    {
        String tldURL = null;
        try {
            if (entityID == null) {
                if (metaManager != null) {
                    entityID = metaManager.getEntityIDByMetaAlias(metaAlias);
                }
            }
            if (realm == null) {
                realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            }
            CircleOfTrustManager cotManager = new CircleOfTrustManager();
            CircleOfTrustDescriptor cotDesc = cotManager.getCircleOfTrust(
                realm, cotSelected);
            if (cotDesc != null &&
                (cotDesc.getCircleOfTrustStatus())
                    .equalsIgnoreCase(IFSConstants.ACTIVE)) 
            {
                tldURL = cotDesc.getIDFFWriterServiceURL();
            }
        } catch (IDFFMetaException metaExp) {
            FSUtils.debug.error(
                "FSPostLogin::doConsentToIntro in cot managment expextion:",
                metaExp);
            tldURL = null;
        } catch (COTException meta2Exp) {
            FSUtils.debug.error(
                "FSPostLogin::doConsentToIntro in cot managment expextion:",
                meta2Exp);
            tldURL = null;
        }
        
        String redirectURL = targetURL;
        if (tldURL != null && entityID != null) {
            redirectURL =  new StringBuffer().append(tldURL)
                .append(IFSConstants.QUESTION_MARK)
                .append(IFSConstants.LRURL).append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(targetURL))
                .append(IFSConstants.AMPERSAND)
                .append(IFSConstants.PROVIDER_ID_KEY)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(entityID)).toString();
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPostLogin::doConsentToIntro return url"
                + redirectURL);
        }
        return redirectURL;
    }

    /**
     * Returns the URL to which request should be redirected
     * for federation. This method reads the request parameters
     * and creates an Authentication Request to send to
     * initiate the Single Sign-On / Federation process.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @return the URL to redirect request to.
     * @exception FSPostLoginException on error.
     */
    public String doFederation(HttpServletRequest request,
        HttpServletResponse response )
    throws FSPostLoginException {

        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        String LRURL = request.getParameter(IFSConstants.LRURL);
        String selectedProvider = 
            request.getParameter(IFSConstants.SELECTEDPROVIDER);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPostLogin::doFederation metaAlias "
                + metaAlias);
            FSUtils.debug.message("FSPostLogin::doFederation lrurl "
                + LRURL);
            FSUtils.debug.message("FSPostLogin::doFederation selected provider"
                + selectedProvider);
        }
        Map headerMap = new HashMap();
        Enumeration headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement().toString();
            String hv = request.getHeader(hn);
            headerMap.put(hn, hv);
        }
        FSLoginHelper plh = new FSLoginHelper(request);
        Map retMap = new HashMap();
        String authLevel = null;
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            authLevel = (String) httpSession.getAttribute(
                IFSConstants.AUTH_LEVEL_KEY);
        }
        try {
            retMap = plh.createAuthnRequest(headerMap,
                    LRURL,
                    authLevel,
                    metaAlias,
                    selectedProvider,
                    true);
        } catch (FSLoginHelperException fsLoginExp) {
            FSUtils.debug.error("FSPostLogin::doFederate in exception ",
                fsLoginExp);
            throw new FSPostLoginException("FSPostLogin::doFederate exception "
                + fsLoginExp.getMessage());
        }
        Map retHeaderMap = (Map)retMap.get(IFSConstants.HEADER_KEY);
        Iterator hdrNames = retHeaderMap.keySet().iterator();
        while (hdrNames.hasNext()) {
            String name = hdrNames.next().toString();
            String value = (String)retHeaderMap.get(name);
            response.addHeader(name, value);
        }
        String urlKey = (String) retMap.get(IFSConstants.URL_KEY);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPostLogin::doFederation returning with "
                + urlKey);
        }
        return urlKey;
    }

    
    /**
     * Retrieves the value of a parameter from the URL. This is
     * an utility method.
     *
     * @param reqURLStr the url string.
     * @param name the value of the parameter to be retrieved.
     * @return value of the parameter
     */
    private static String getValueFromURL(String reqURLStr, String name) {
        String returnStr = null;
        int iIndex =0 ;
        if (reqURLStr != null &&
            ((iIndex = reqURLStr.lastIndexOf(name)) != -1)) 
        {
            iIndex = iIndex + name.length() + 1;
            String newStr = reqURLStr.substring(iIndex);
            byte strInBytes[] = newStr.getBytes();
            int endOfString;
            int len = newStr.length();
            for (endOfString = 0; endOfString < len; endOfString++) {
                if (strInBytes[endOfString] == '/' ||
                    strInBytes[endOfString] == '?') 
                {
                    break;
                }
            }
            returnStr = newStr.substring(0, endOfString);
        }
        return returnStr;
    }
}
