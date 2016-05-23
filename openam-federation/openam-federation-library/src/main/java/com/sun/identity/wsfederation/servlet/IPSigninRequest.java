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
 * $Id: IPSigninRequest.java,v 1.8 2009/10/28 23:59:00 exu Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.wsfederation.servlet;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.wsfederation.logging.LogUtil;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.profile.IDPSSOUtil;
import com.sun.identity.wsfederation.profile.RequestSecurityTokenResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import javax.servlet.ServletException;
import org.owasp.esapi.ESAPI;

/**
 * This class implements the sign-in request for the identity provider.
 */
public class IPSigninRequest extends WSFederationAction {
    private static Debug debug = WSFederationUtils.debug;
    
    String wtrealm;
    String whr;
    String wct;
    String wctx;
    String wreply;

    /**
     * Creates a new instance of RPSigninRequest
     * @param request HTTPServletRequest for this interaction
     * @param response HTTPServletResponse for this interaction
     * @param whr the whr parameter from the signin request
     * @param wtrealm the wtrealm parameter from the signin request
     * @param wct the wct parameter from the signin request
     * @param wctx the wctx parameter from the signin request
     * @param wreply the wreply parameter from the signin request
     */
    public IPSigninRequest(HttpServletRequest request,
        HttpServletResponse response, String whr, String wtrealm, String wct,
        String wctx, String wreply) {
        super(request,response);
        this.whr = whr;
        this.wtrealm = wtrealm;
        this.wct = wct;
        this.wctx = wctx;
        this.wreply = wreply;
    }
    
    /**
     * Processes the sign-in request, returning a response via the 
     * HttpServletResponse passed to the constructor.
     */
    public void process() throws IOException, WSFederationException
    {
        String classMethod = "IPSigninRequest.process: ";
        Object session = null;

        String idpMetaAlias = WSFederationMetaUtils.getMetaAliasByUri(
                                            request.getRequestURI());
        if ((idpMetaAlias == null) 
            || (idpMetaAlias.trim().length() == 0)) {
            debug.error(classMethod +
                "unable to get IDP meta alias from request.");
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("IDPMetaAliasNotFound"));
        }

        WSFederationMetaManager metaManager =
            WSFederationUtils.getMetaManager();
        // retrieve IDP entity id from meta alias            
        String idpEntityID = metaManager.getEntityByMetaAlias(idpMetaAlias);
        if ((idpEntityID == null) 
            || (idpEntityID.trim().length() == 0)) {
            debug.error(classMethod +
                "Unable to get IDP Entity ID from metaAlias");
            throw new WSFederationException(
               WSFederationUtils.bundle.getString("nullIDPEntityID"));
        }

        String realm = 
            WSFederationMetaUtils.getRealmByMetaAlias(idpMetaAlias);

        String spEntityID = 
            metaManager.getEntityByTokenIssuerName(realm,
            wtrealm);
        if ((spEntityID == null) 
            || (spEntityID.trim().length() == 0)) {
            debug.error(classMethod +
                "Unable to get SP Entity ID from wtrealm");
            throw new WSFederationException(
               WSFederationUtils.bundle.getString("nullIDPEntityID"));
        }
        
        // check if the remote provider is valid
        if (!metaManager.isTrustedProvider(realm, idpEntityID, 
            spEntityID)) {
            debug.error(classMethod +
                "The remote provider is not valid.");
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("invalidReceiver"));
        }

        // get the user sso session from the request
        try {
            session = WSFederationUtils.sessionProvider.getSession(request);
        } catch (SessionException se) {
            if (debug.messageEnabled()) {
                debug.message(
                    classMethod + "Unable to retrieve user session.");
            }
            session = null;
        }

        if (session == null) {
            // the user has not logged in yet, redirect to auth
            redirectAuthentication(idpEntityID, realm);
            return;
        }

        String sessionRealm = getSessionRealm(session);
        // If we are in the same realm as the users existing session then we can continue processing
        if (realm.equalsIgnoreCase(sessionRealm)) {
            // set session property for multi-federation protocol hub
            MultiProtocolUtils.addFederationProtocol(session, SingleLogoutManager.WS_FED);
            sendResponse(session, idpEntityID, spEntityID, idpMetaAlias, realm);
        } else {
            // Trigger a re-auth to the new realm if the session realm value is different
            if (debug.messageEnabled()) {
                debug.message(classMethod + "The users realm: " + sessionRealm + " was different to the IDP's realm: "
                        + realm + ", will re-authenticate to IDP: " + idpEntityID);
            }
            redirectAuthentication(idpEntityID, realm);
        }
    }

    /**
     * Redirect to authenticate service
     */
    private void redirectAuthentication(String idpEntityID, String realm) 
        throws WSFederationException, IOException {
        String classMethod = "IDPSSOFederate.redirectAuthentication: ";
        
        // get the authentication service url 
        StringBuffer newURL = new StringBuffer(
            IDPSSOUtil.getAuthenticationServiceURL(realm, idpEntityID, 
            request));
        // find out the authentication method, e.g. module=LDAP, from
        // authn context mapping 
        /*
        IDPAuthnContextMapper idpAuthnContextMapper = 
            IDPSSOUtil.getIDPAuthnContextMapper(realm, idpEntityID);
        
        IDPAuthnContextInfo info = 
            idpAuthnContextMapper.getIDPAuthnContextInfo(
                authnReq, idpEntityID, realm);
        Set authnTypeAndValues = info.getAuthnTypeAndValues();
        if ((authnTypeAndValues != null) 
            && (!authnTypeAndValues.isEmpty())) { 
            Iterator iter = authnTypeAndValues.iterator();
            StringBuffer authSB = new StringBuffer((String)iter.next());
            while (iter.hasNext()) {
                authSB.append("&"); 
                authSB.append((String)iter.next());
            }
            if (newURL.indexOf("?") == -1) {
                newURL.append("?");
            } else {
                newURL.append("&");
            }
            newURL.append(authSB.toString());
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                    "authString=" + authSB.toString());
            }
        }
        */
        if (newURL.indexOf("?") == -1) {
            newURL.append("?goto=");
        } else {
            newURL.append("&goto=");
        }
        StringBuffer target = request.getRequestURL().
            append("?").
            append(request.getQueryString());
        if (debug.messageEnabled()) {
            debug.message(classMethod +
                "Target to get back here: " + target.toString());
        }
        newURL.append(URLEncDec.encode(target.toString()));
        if (debug.messageEnabled()) {
            debug.message(classMethod +
                "New URL for authentication: " + newURL.toString());
        }
        
        // We want authentication request from browser to come back to this 
        // instance
        WSFederationUtils.sessionProvider.setLoadBalancerCookie(request, response);
        
        // TODO: here we should check if the new URL is one
        //       the same web container, if yes, forward,
        //       if not, redirect
        response.sendRedirect(newURL.toString());
    }
    
    /**
     * Sends <code>RequestSecurityTokenResponse</code> containing an 
     * <code>Assertion</code> back to the requesting service provider
     */
    private void sendResponse(Object session, String idpEntityId, 
        String spEntityId, String idpMetaAlias, String realm) 
        throws WSFederationException, IOException {
        String classMethod = "IDPSSOFederate.sendResponse: " ;
        String acsURL = IDPSSOUtil.getACSurl(spEntityId, realm, wreply);

        if ((acsURL == null) || (acsURL.trim().length() == 0)) {
            debug.error(classMethod + "no ACS URL found.");
            String[] data = { realm, spEntityId, wreply };
            LogUtil.error(Level.INFO,
                LogUtil.NO_ACS_URL, data, null);
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("unableTofindACSURL"));
        }

        final SPSSOConfigElement spConfig = WSFederationUtils.getMetaManager().getSPSSOConfig(realm, spEntityId);
        if (spConfig == null) {
            debug.error("Cannot find configuration for SP " + spEntityId);
            throw new WSFederationException(WSFederationUtils.bundle.getString("unableToFindSPConfiguration"));
        }

        String authMethod;
        try {
            authMethod = WSFederationUtils.sessionProvider.getProperty(session, SessionProvider.AUTH_METHOD)[0];
        } catch (SessionException se) {
            throw new WSFederationException(se);
        }

        String strWantAssertionSigned = WSFederationMetaUtils.getAttribute(spConfig,
                WSFederationConstants.WANT_ASSERTION_SIGNED);
        // By default, we want to sign assertions
        boolean wantAssertionSigned = strWantAssertionSigned != null ? Boolean.parseBoolean(strWantAssertionSigned)
                : true;

        FederationElement sp = WSFederationUtils.getMetaManager().getEntityDescriptor(realm, spEntityId);
        String spTokenIssuerName = WSFederationUtils.getMetaManager().getTokenIssuerName(sp);

        // generate a response for the authn request
        RequestSecurityTokenResponse rstr = new RequestSecurityTokenResponse(
                WSFederationUtils.createSAML11Token(realm, idpEntityId, spEntityId, session, spTokenIssuerName,
                        authMethod, wantAssertionSigned), wtrealm);

        if (rstr == null) {
            debug.error(classMethod + "response is null");
            String errorMsg = 
                WSFederationUtils.bundle.getString("UnableToCreateAssertion");
            // TODO - check WS-Fed error handling
            /*
            res = IDPSSOUtil.getErrorResponse(authnReq, 
                SAML2Constants.RESPONDER, errorMsg, idpEntityID);
             */
            return;
        } else {
            try {
                String[] values = {idpMetaAlias};

                // Add SP to SP list in session
                String[] spList = WSFederationUtils.sessionProvider.
                    getProperty(session, WSFederationConstants.SESSION_SP_LIST);
                ArrayList<String> newSpList = ( spList != null ) ?
                    new ArrayList<String>(Arrays.asList(spList)) :
                    new ArrayList<String>();
                if ( ! newSpList.contains(spEntityId))
                {
                    newSpList.add(spEntityId);
                    WSFederationUtils.sessionProvider.setProperty(session, 
                        WSFederationConstants.SESSION_SP_LIST,
                        newSpList.toArray(new String[0]));
                }
            } catch (SessionException e) {
                debug.error(classMethod +
                    "error setting idpMetaAlias into the session: ", e);
            }
            
            try {
                postToTarget(rstr, acsURL);
            } catch (ServletException se) {
                throw new WSFederationException(se);
            }
        }
    }
    
    /**
     * This method posts the assertion response to the service provider using 
     * the HttpServletResponse object.
     *
     * @param rstr the <code>RequestSecurityTokenResponse</code> to send
     * @param targetURL the <code>URL</code> of the target location
     * 
     * @exception IOException if there is any network I/O problem
     */
    private void postToTarget(RequestSecurityTokenResponse rstr,
        String targetURL)
        throws IOException, ServletException
    {
        String classMethod = "IDPSSOUtil.postToTarget: ";
        
        String wresult = rstr.toString();
        
        if (debug.messageEnabled()) {
            debug.message(classMethod + "wresult before encoding: " + wresult);
        }
        
        request.setAttribute(WSFederationConstants.POST_ACTION, ESAPI.encoder().encodeForHTML(targetURL));
        request.setAttribute(WSFederationConstants.POST_WA, WSFederationConstants.WSIGNIN10);
        request.setAttribute(WSFederationConstants.POST_WCTX, ESAPI.encoder().encodeForHTML(wctx));
        request.setAttribute(WSFederationConstants.POST_WRESULT, ESAPI.encoder().encodeForHTML(wresult));
        request.getRequestDispatcher("/wsfederation/jsp/post.jsp").forward(request, response);
    }

    /**
     * Return the realm from the session if it can be read
     */
    private static String getSessionRealm(Object session) {

        String classMethod = "IPSigninRequest.getSessionRealm: ";
        String sessionRealm = null;

        try {
            sessionRealm = WSFederationUtils.sessionProvider.
                    getProperty(session, SAML2Constants.ORGANIZATION)[0];
        } catch (SessionException ex) {
            debug.error(classMethod + "Could not retrieve the session information", ex);
        }

        return sessionRealm;
    }
}
