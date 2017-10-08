/*
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
 * $Id: RPSigninRequest.java,v 1.9 2009/11/03 00:48:54 madan_ranganath Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.wsfederation.servlet;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;

import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import java.io.IOException;
import java.util.Date;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class implements the sign-in request for the service provider.
 */
public class RPSigninRequest extends WSFederationAction {
    private static Debug debug = WSFederationUtils.debug;
    
    String whr;
    String wreply;
    String wctx;
    String wct;
            
    /**
     * Creates a new instance of RPSigninRequest
     * @param request HTTPServletRequest for this interaction
     * @param response HTTPServletResponse for this interaction
     * @param whr the whr parameter from the signin request
     * @param wct the wct parameter from the signin request
     * @param wctx the wctx parameter from the signin request
     * @param wreply the wreply parameter from the signin request
     */
    public RPSigninRequest(HttpServletRequest request,
        HttpServletResponse response, String whr, 
        String wct, String wctx, String wreply) {
        super(request,response);
        this.whr = whr;
        this.wct = wct;
        this.wctx = wctx;
        this.wreply = wreply;
    }
    
    /**
     * Processes the sign-in request, redirecting the browser to the identity
     * provider via the HttpServletResponse passed to the constructor.
     */
    public void process() throws WSFederationException, IOException
    {
        String classMethod = "RPSigninRequest.process: ";
        
        if (debug.messageEnabled()) {
            debug.message(classMethod+"entered method");
        }

        if (wctx == null || wctx.length() == 0){
            // Exchange reply URL for opaque identifier
            wctx = (wreply != null && (wreply.length() > 0)) ? 
                WSFederationUtils.putReplyURL(wreply) : null;
        }

        String spMetaAlias = WSFederationMetaUtils.getMetaAliasByUri(
                                            request.getRequestURI());

        if ( spMetaAlias==null || spMetaAlias.length()==0 ) {
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("MetaAliasNotFound"));
        }

        String spRealm = SAML2MetaUtils.getRealmByMetaAlias(spMetaAlias);
        
        WSFederationMetaManager metaManager = 
            WSFederationUtils.getMetaManager();
        String spEntityId = 
            metaManager.getEntityByMetaAlias(spMetaAlias);        
        if ( spEntityId==null || spEntityId.length()==0 )
        {
            String[] args = {spMetaAlias, spRealm};
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME,
                "invalidMetaAlias", args);
        }

        SPSSOConfigElement spConfig = 
            metaManager.getSPSSOConfig(spRealm,spEntityId);
        if ( spConfig==null ) {
            String[] args = {spEntityId, spRealm};
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME,
                "badSPEntityID",args);
        }

        Map<String,List<String>> spConfigAttributes = 
            WSFederationMetaUtils.getAttributes(spConfig);

        String accountRealmSelection = 
                spConfigAttributes.get(
                com.sun.identity.wsfederation.common.WSFederationConstants.
                ACCOUNT_REALM_SELECTION).get(0);
        if ( accountRealmSelection == null )
        {
            accountRealmSelection = 
                WSFederationConstants.ACCOUNT_REALM_SELECTION_DEFAULT;
        }
        String accountRealmCookieName = 
            spConfigAttributes.get(WSFederationConstants.
            ACCOUNT_REALM_COOKIE_NAME).get(0);
        if ( accountRealmCookieName == null )
        {
            accountRealmCookieName = 
                WSFederationConstants.ACCOUNT_REALM_COOKIE_NAME_DEFAULT;
        }
        String homeRealmDiscoveryService = 
            spConfigAttributes.get(
            WSFederationConstants.HOME_REALM_DISCOVERY_SERVICE).get(0);

        if (debug.messageEnabled()) {
            debug.message(classMethod+"account realm selection method is " + 
                accountRealmSelection);
        }

        String idpIssuerName = null;
        if (whr != null && whr.length() > 0)
        {
            // whr parameter overrides other mechanisms...
            idpIssuerName = whr;

            if (accountRealmSelection.equals(WSFederationConstants.COOKIE)) 
            {
                // ...and overwrites cookie
                Cookie cookie = new Cookie(accountRealmCookieName,whr);
                // Set cookie to persist for a year
                cookie.setMaxAge(60*60*24*365);
		CookieUtils.addCookieToResponse(response, cookie);
            }
        }
        else
        {
            if (accountRealmSelection.equals(
                WSFederationConstants.USERAGENT)) {
                String uaHeader = 
                    request.getHeader(WSFederationConstants.USERAGENT);
                if (debug.messageEnabled()) {
                    debug.message(classMethod+"user-agent is :" + uaHeader);
                }
                idpIssuerName = 
                    WSFederationUtils.accountRealmFromUserAgent(uaHeader, 
                    accountRealmCookieName);
            } else if (accountRealmSelection.equals(
                WSFederationConstants.COOKIE)) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (int i = 0; i < cookies.length; i++) {
                        if (cookies[i].getName().equals(
                            accountRealmCookieName)) {
                            idpIssuerName = cookies[i].getValue();
                            break;
                        }
                    }
                }
            } else {
                debug.error(classMethod+"unexpected value for " + 
                    WSFederationConstants.ACCOUNT_REALM_SELECTION + " : " + 
                    accountRealmSelection);
                throw new WSFederationException(
                    WSFederationUtils.bundle.getString("badAccountRealm"));
            }
        }

        FederationElement sp = 
            metaManager.getEntityDescriptor(spRealm,spEntityId);
        String spIssuerName = 
            metaManager.getTokenIssuerName(sp);
        if (debug.messageEnabled()) {
            debug.message(classMethod+"SP issuer name:" + spIssuerName);
        }

        String idpEntityId = null;
        if (idpIssuerName != null && idpIssuerName.length() > 0)
        {
            // Got the issuer name from the cookie/UA string - let's see if 
            // we know the entity ID
            idpEntityId = 
                metaManager.getEntityByTokenIssuerName(null, 
                idpIssuerName);
        }

        if (idpEntityId == null) {
            // See if there is only one trusted IdP configured...
            List<String> allRemoteIdPs = 
                metaManager.getAllRemoteIdentityProviderEntities(spRealm);
            ArrayList<String> trustedRemoteIdPs = new ArrayList<String>();
            
            for ( String idp : allRemoteIdPs )
            {
                if ( metaManager.isTrustedProvider(spRealm, 
                    spEntityId, idp) ) {
                    trustedRemoteIdPs.add(idp);
                }
            }

            if ( trustedRemoteIdPs.size() == 0 )
            {
                // Misconfiguration!
                throw new WSFederationException(
                    WSFederationUtils.bundle.getString("noIDPConfigured"));
            }
            else if ( trustedRemoteIdPs.size() == 1 )
            {
                idpEntityId = trustedRemoteIdPs.get(0);
            }
        }

        FederationElement idp = null;
        if ( idpEntityId != null )
        {
            idp = metaManager.getEntityDescriptor(null,
                idpEntityId);
        }
        
        // Set LB cookie here so it's done regardless of which redirect happens
        // We want response to come back to this instance
        WSFederationUtils.sessionProvider.setLoadBalancerCookie(request, response);

        // If we still don't know the IdP, redirect to home realm discovery
        if (idp == null) {
            StringBuffer url = new StringBuffer(homeRealmDiscoveryService);
            url.append("?wreply=");
            url.append(URLEncDec.encode(request.getRequestURL().toString()));
            if (wctx != null) {
                url.append("&wctx=");
                url.append(URLEncDec.encode(wctx));
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod + 
                    "no account realm - redirecting to :" + url);
            }
            response.sendRedirect(url.toString());
            return;
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod+"account realm:" + idpEntityId);
        }

        String endpoint = 
            metaManager.getTokenIssuerEndpoint(idp);
        if (debug.messageEnabled()) {
            debug.message(classMethod+"endpoint:" + endpoint);
        }
        String replyURL = 
            metaManager.getTokenIssuerEndpoint(sp);
        if (debug.messageEnabled()) {
            debug.message(classMethod+"replyURL:" + replyURL);
        }
        StringBuffer url = new StringBuffer(endpoint);
        url.append("?wa=");
        url.append(URLEncDec.encode(WSFederationConstants.WSIGNIN10));
        if ( wctx != null )
        {
            url.append("&wctx=");
            url.append(URLEncDec.encode(wctx));
        }
        url.append("&wreply=");
        url.append(URLEncDec.encode(replyURL));
        url.append("&wct=");
        url.append(URLEncDec.encode(DateUtils.toUTCDateFormat(newDate())));
        url.append("&wtrealm=");
        url.append(URLEncDec.encode(spIssuerName));
        if (debug.messageEnabled()) {
            debug.message(classMethod+"Redirecting to:" + url);
        }
        response.sendRedirect(url.toString());
    }
}
