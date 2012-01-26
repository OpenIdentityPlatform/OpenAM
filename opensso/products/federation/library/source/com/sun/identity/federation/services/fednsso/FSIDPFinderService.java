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
 * $Id: FSIDPFinderService.java,v 1.4 2008/06/25 05:46:58 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.fednsso;

import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSRedirectException;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSAuthnDecisionHandler;
import com.sun.identity.federation.services.FSAuthContextResult;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.shared.encode.URLEncDec;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;


/**
 * This class <code>FSIDPFinderService</code> is used to find a preferred
 * identity provider by using a common domain cookie. If the common domain
 * is not found, this will pick one from the random list of preferred
 * identity providers from the list that it knows that it can proxy the
 * authentication request.
 */
public class FSIDPFinderService extends HttpServlet {

    /**
     * A static hash map that contains request IDs as keys and circle of
     * trusts are as values. This static table will be used to iterate
     * through the number of circle of trusts if the IDP is particapting
     * in more than one circle of trust.
     */ 
    private static Map requestCotSetMap = 
                 Collections.synchronizedMap(new HashMap());

    /**
     * Gets <code>IDP</code> from common domain and sends proxy authentication
     * request to the <code>IDP</code>.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException, IOException if error occurred.
     */
    public void doGet(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException
    {
        if (request == null || response == null) {
            FSUtils.debug.error("FSIDPFinderService.doGet:: Null Input");
            return;
        }
        FSUtils.debug.message("FSIDPFinderService.doGet::Init");
        String entityID = request.getParameter("ProviderID");
        String requestID = request.getParameter("RequestID");
        String realm = request.getParameter("Realm");
        if (entityID == null || requestID == null || realm == null) {
            FSUtils.debug.error("FSIDPFinderService.doGet:: Request is missing"+
                "either ProviderID or the RequestID"); 
            throw new ServletException("invalidRequest");
        }
        String idpID = null;
        try {
            idpID = getCommonDomainIDP(
                request, response, realm, entityID, requestID);
        } catch (FSRedirectException fe) {
            if (FSUtils.debug.messageEnabled()) {
               FSUtils.debug.message("FSIDPFinderService.doGet:Redirection" +
               " has happened");
            }
            return;
        }

        String hostMetaAlias = null;
        BaseConfigType hostConfig = null;
        IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
        try {
            if (metaManager != null ) {
                hostConfig = metaManager.getIDPDescriptorConfig(
                    realm, entityID);
                if (hostConfig != null) {
                    hostMetaAlias = hostConfig.getMetaAlias();
                }
            }
        } catch (IDFFMetaException ie) {
            FSUtils.debug.error("FSIDPFinderService.doGet:: Failure in " +
                "getting proxying hosted meta:", ie);
            return;
        }

        FSSessionManager sessionManager = 
            FSSessionManager.getInstance(hostMetaAlias);
        FSAuthnRequest authnReq = sessionManager.getAuthnRequest(requestID);

        // If the introduction cookie is not available or the provider
        // is same as the local provider then do a local login.
        if (idpID == null || idpID.equals(entityID)) {
            String loginURL = getLoginURL(authnReq, realm, entityID, request);
            if (loginURL == null) {
                FSUtils.debug.error("FSIDPFinderService.doGet : login url" +
                    " is null");
                return; 
            }

            response.setHeader("Location", loginURL);
            response.sendRedirect(loginURL);
        } else {
 
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSIDPFinderService.doGet:IDP to be proxied:" + idpID);
            }

            // Now proxy the authentication request to the preferred IDP.
            try {
                FSProxyHandler handler = new FSProxyHandler(request, response);
                handler.setHostedEntityId(entityID);
                IDPDescriptorType hostDesc = null;
                SPDescriptorType origSPDesc = null;
                if (metaManager != null ) {
                    hostDesc = metaManager.getIDPDescriptor(realm, entityID);
                    origSPDesc = metaManager.getSPDescriptor(
                        realm, authnReq.getProviderId());
                }
                handler.setSPDescriptor(origSPDesc);
                handler.setHostedDescriptor(hostDesc);
                handler.setHostedDescriptorConfig(hostConfig);
                handler.setMetaAlias(hostMetaAlias);
                handler.setRealm(realm);
                handler.sendProxyAuthnRequest(authnReq, idpID);
            } catch (IDFFMetaException ie) {
                FSUtils.debug.error("FSIDPFinderService.doGet:: Failure in " +
                    "getting proxying hosted meta:", ie);
            } catch (FSException fe) {
                FSUtils.debug.error("FSIDPFinderService.doGet:: Failure in " +
                    "sending the proxy authentication request.", fe);
            }
        }
 
    }

    /**
     * Gets <code>IDP</code> from common domain and sends proxy authentication
     * request to the <code>IDP</code>.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException, IOException if error occurred.
     */
    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException
    {
        doGet(request, response);
    }

    /**
     * Gets a preferred IDP from the common domain cookie.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param realm The realm under which the entity resides.
     * @param entityID Hosted entity ID.
     * @param requestID Original Authentication Request ID.
     * @exception FSRedirectException for the redirection.
     *            IOException for any redirection failure.
     */
    private String getCommonDomainIDP(
        HttpServletRequest request,
        HttpServletResponse response,
        String realm,
        String entityID,
        String requestID
    ) throws FSRedirectException, IOException 
    {

        String idpID = FSUtils.findPreferredIDP(realm, request);
        if (idpID != null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSIDPFinderService.getCommonDomainIDP:" +
                    "Preferred IDP found from the common domain." + idpID);
            }
            if (requestCotSetMap.containsKey(requestID)) {
                requestCotSetMap.remove(requestID);
            }
            return idpID;
        }
        Set tmpCotSet = (Set)requestCotSetMap.get(requestID);
        if (tmpCotSet == null) {
            try {
                IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
                List cotList = null;
                if (metaManager != null) {
                    BaseConfigType spConfig = 
                        metaManager.getSPDescriptorConfig(realm, entityID);
                    cotList = IDFFMetaUtils.getAttributeValueFromConfig(
                        spConfig, IFSConstants.COT_LIST);
                }
                if (cotList != null) {
                    tmpCotSet = new HashSet();
                    tmpCotSet.addAll(cotList);
                }
            } catch (IDFFMetaException ie) {
                FSUtils.debug.error(
                    "FSIDPFinderService.getCommonDomainIDP:cannot get meta:",
                    ie);
                return null;
            }
        }

        if (tmpCotSet == null || tmpCotSet.isEmpty()) {
            FSUtils.debug.message(
                "FSIDPFinderService.getCommonDomainIDP::No more Cots.");
            if (requestCotSetMap.containsKey(requestID)) {
                requestCotSetMap.remove(requestID);
            }
            return null; 
        }

        Iterator iter = tmpCotSet.iterator();
        while(iter.hasNext()) {
           String cotName = (String)iter.next();
           iter.remove();
           requestCotSetMap.put(requestID, tmpCotSet);
           if (FSUtils.debug.messageEnabled()) {
               FSUtils.debug.message(
                   "FSIDPFinderService.getCommonDomainIDP: Trying Cot: " +
                   cotName);
           }
           String readerServiceURL = null;
           try {
               CircleOfTrustManager cotManager = new CircleOfTrustManager();
               CircleOfTrustDescriptor cotDesc =
                   cotManager.getCircleOfTrust(realm, cotName);
               if (cotDesc != null &&
                   (cotDesc.getCircleOfTrustStatus())
                       .equalsIgnoreCase(IFSConstants.ACTIVE))
               {
                   readerServiceURL = cotDesc.getIDFFReaderServiceURL();
               }
           } catch (COTException fe) {
               FSUtils.debug.error("FSIDPFinderService.getCommonDomainIDP:" +
                   "Unable to retrieve reader service url.", fe);
           }
           if (readerServiceURL != null) {
               String baseURL = FSServiceUtils.getBaseURL(request);
               StringBuffer returnURL = new StringBuffer(300); 
               returnURL.append(baseURL).append(IFSConstants.IDP_FINDER_URL)
                   .append("?").append("RequestID")
                   .append("=").append(URLEncDec.encode(requestID))
                   .append("&").append("Realm=")
                   .append(URLEncDec.encode(realm))
                   .append("&").append("ProviderID=")
                   .append(URLEncDec.encode(entityID));
               StringBuffer redirectURL = new StringBuffer(300);
               redirectURL.append(readerServiceURL).append("?")
                   .append(IFSConstants.LRURL).append("=")
                   .append(URLEncDec.encode(returnURL.toString()));
               String url = redirectURL.toString();
               if (FSUtils.debug.messageEnabled()) {
                   FSUtils.debug.message(
                       "FSIDPFinderService.getCommonDomainIDP:Redirection URL:"
                       + url);
               }
               response.setHeader("Location", url);
               response.sendRedirect(url);
               throw new FSRedirectException(FSUtils.bundle.getString(
                   "Redirection_Happened"));
            }
        }
        return null;
    }

    private String getLoginURL(
        FSAuthnRequest authnRequest, 
        String realm,
        String hostProviderID,
        HttpServletRequest httpRequest) 
    {

        if (authnRequest == null) {
            FSUtils.debug.error(
                "FSIDPFinderServer.getLoginURL: null authnrequest");
            return null;
        }

        if (hostProviderID == null) {
            FSUtils.debug.error(
                "FSIDPFinderServer.getLoginURL: null hostProviderID");
            return null;
        }

        IDPDescriptorType idpDescriptor = null;
        BaseConfigType idpConfig = null;
        try {
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            idpDescriptor = metaManager.getIDPDescriptor(realm, hostProviderID);
            idpConfig = metaManager.getIDPDescriptorConfig(
                realm, hostProviderID);
        } catch (Exception e) {
            FSUtils.debug.error("FSIDPFinderServer.getLoginURL : exception "+
                "while retrieving meta config", e);
            return null;
        }

        String authType = authnRequest.getAuthContextCompType();

        FSAuthnDecisionHandler authnDecisionHandler =
            new FSAuthnDecisionHandler(realm, hostProviderID, httpRequest);
        List defAuthnCtxList = IDFFMetaUtils.getAttributeValueFromConfig(
                idpConfig, IFSConstants.DEFAULT_AUTHNCONTEXT);
        FSAuthContextResult authnResult = authnDecisionHandler.
            getURLForAuthnContext(defAuthnCtxList, authType);
        return formatLoginURL(
            authnResult.getLoginURL(), 
            authnResult.getAuthContextRef(),
            realm,
            hostProviderID, 
            idpDescriptor,
            idpConfig,
            authnRequest,
            httpRequest);
    }

    private String formatLoginURL(
        String loginURL, 
        String authnContext,
        String realm,
        String hostProviderID,
        IDPDescriptorType idpDescriptor,
        BaseConfigType idpConfig,
        FSAuthnRequest authnRequest,
        HttpServletRequest httpRequest) 
    {

        FSUtils.debug.message("FSIDPFinderService.formatLoginURL: Called");

        try {
            if (loginURL == null){
                FSUtils.debug.error("FSIDPFinderService.formatLoginURL: ");
                return null;
            }

            //create return url
            String metaAlias = idpConfig.getMetaAlias();
            String ssoUrl = idpDescriptor.getSingleSignOnServiceURL();
            StringBuffer returnUrl = new StringBuffer(ssoUrl);
            if (ssoUrl.indexOf('?') == -1) {
                returnUrl.append("?");
            } else {
                returnUrl.append("&");
            }
            returnUrl.append(IFSConstants.AUTHN_INDICATOR_PARAM)
                    .append("=").append(IFSConstants.AUTHN_INDICATOR_VALUE)
                    .append("&")
                    .append(IFSConstants.AUTHN_CONTEXT)
                    .append("=").append(URLEncDec.encode(authnContext))
                    .append("&")
                    .append(IFSConstants.REALM)
                    .append("=").append(URLEncDec.encode(realm))
                    .append("&")
                    .append(IFSConstants.PROVIDER_ID_KEY)
                    .append("=").append(URLEncDec.encode(hostProviderID))
                    .append("&")
                    .append(IFSConstants.META_ALIAS)
                    .append("=").append(URLEncDec.encode(metaAlias))
                    .append("&")
                    .append(IFSConstants.AUTH_REQUEST_ID)
                    .append("=").append(URLEncDec.encode(
                     authnRequest.getRequestID()));

            //create goto url
            String postLoginUrl = FSServiceUtils.getBaseURL(httpRequest)
                + IFSConstants.POST_LOGIN_PAGE;
            StringBuffer gotoUrl = new StringBuffer(postLoginUrl);
            if (postLoginUrl.indexOf('?') == -1) {
                gotoUrl.append("?");
            } else {
                gotoUrl.append("&");
            }
            gotoUrl.append(IFSConstants.LRURL).append("=")
                .append(URLEncDec.encode(returnUrl.toString()))
                .append("&").append(IFSConstants.SSOKEY).append("=")
                .append(IFSConstants.SSOVALUE).append("&")
                .append(IFSConstants.META_ALIAS).append("=").append(metaAlias);

            //create redirect url
            StringBuffer redirectUrl = new StringBuffer(100);
            redirectUrl.append(loginURL);
            if (loginURL.indexOf('?') == -1) {
                redirectUrl.append("?");
            } else {
                redirectUrl.append("&");
            }
            redirectUrl.append(IFSConstants.GOTO_URL_PARAM).append("=")
                .append(URLEncDec.encode(gotoUrl.toString()));

            if (realm != null && realm.length() != 0) {
                redirectUrl.append("&").append(IFSConstants.ORGKEY).append("=")
                    .append(URLEncDec.encode(realm));
            }
            int len = redirectUrl.length() - 1;
            if (redirectUrl.charAt(len) == '&') {
                redirectUrl = redirectUrl.deleteCharAt(len);
            }
            return redirectUrl.toString();
        } catch(Exception e){
            FSUtils.debug.error(
                "FSIDPFinderService.formatLoginURL: Exception: " ,e);
            return null;
        }
    }

}
