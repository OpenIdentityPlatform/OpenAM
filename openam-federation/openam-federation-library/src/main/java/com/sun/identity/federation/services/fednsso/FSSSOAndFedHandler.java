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
 * $Id: FSSSOAndFedHandler.java,v 1.12 2009/11/04 00:06:11 exu Exp $ 
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionPartner;
import com.sun.identity.federation.services.FSAssertionManager;
import com.sun.identity.federation.services.FSAuthnDecisionHandler;
import com.sun.identity.federation.services.FSAuthContextResult;
import com.sun.identity.federation.services.FSIDPProxy;
import com.sun.identity.federation.services.FSRealmIDPProxy;
import com.sun.identity.federation.services.logout.FSTokenListener;
import com.sun.identity.federation.services.util.FSNameIdentifierHelper;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSSignatureManager;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSResponse;
import com.sun.identity.federation.message.FSSAMLRequest;
import com.sun.identity.federation.message.FSScoping;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSRedirectException;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml2.xmlsig.SigManager;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
 * Base class for <code>IDP</code> side handler that handles single sign on 
 * and federation requests.
 */
public abstract class FSSSOAndFedHandler {
    private static FSIDPProxy proxyFinder = null; 
    private static FSRealmIDPProxy realmProxyFinder = null; 
    protected static IDFFMetaManager metaManager = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected SPDescriptorType spDescriptor = null;
    protected BaseConfigType spConfig = null;
    protected String spEntityId = null;
    protected String relayState = null;
    protected FSAuthnRequest authnRequest = null;
    protected Object ssoToken = null;
        
    protected String metaAlias = null;
    protected IDPDescriptorType hostedDesc = null;
    protected BaseConfigType hostedConfig = null;
    protected String realm = null;
    protected String hostedEntityId = null;
    protected Status noFedStatus = null;
    protected FSAccountManager accountManager = null;

    static {
        metaManager = FSUtils.getIDFFMetaManager();
        try {
            String proxyFinderClass = SystemConfigurationUtil.getProperty(
                "com.sun.identity.federation.proxyfinder");
            if ((proxyFinderClass != null) &&
                (proxyFinderClass.length() != 0))
            {
                Object proxyClass = 
                    Thread.currentThread().getContextClassLoader().loadClass(
                        proxyFinderClass).newInstance();
                if (proxyClass instanceof FSRealmIDPProxy) {
                    realmProxyFinder = (FSRealmIDPProxy)proxyClass;
                } else if (proxyClass instanceof FSIDPProxy) {
                    proxyFinder = (FSIDPProxy)proxyClass;
                }
            }
        } catch (Exception ex) {
            FSUtils.debug.error("FSSSOAndFedHandler:Static Init Failed", ex);
        }
    }
    
    /**
     * Sets meta alias of the host identity provider.
     * @param metaAlias meta alias of the provider.
     */
    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
        try {
            accountManager = FSAccountManager.getInstance(metaAlias);
        } catch (FSAccountMgmtException e) {
            FSUtils.debug.error(
                "FSSSOAndFedHandler: couldn't obtain account manager:", e);
        }
    }
    
    /**
     * Sets host identity provider's entity ID.
     * @param hostedEntityId entity ID to be set
     * @see #getHostedEntityId()
     */
    public void setHostedEntityId(String hostedEntityId) {
        this.hostedEntityId = hostedEntityId;
    }

    /**
     * Sets host identity provider's meta descriptor.
     * @param hostedDesc hosted meta descriptor to be set
     */
    public void setHostedDescriptor(IDPDescriptorType hostedDesc) {
        this.hostedDesc = hostedDesc;
    }

    /**
     * Sets host identity provider's extended meta.
     * @param hostedConfig host identity provider's extended meta to be set
     */
    public void setHostedDescriptorConfig(BaseConfigType hostedConfig) {
        this.hostedConfig = hostedConfig;
    }
        
    /**
     * Gets hosted provider id.
     * @return hosted provider id.
     * @see #setHostedEntityId(String)
     */
    public String getHostedEntityId(){
        return hostedEntityId;
    }
    
    /**
     * Gets the realm under which the entity resides.
     * @return the realm under which the entity resides.
     * @see #setRealm(String)
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Sets the realm under which the entity resides.
     * @param realm The realm under which the entity resides.
     * @see #getRealm()
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Default constructor.
     */ 
    protected FSSSOAndFedHandler() {
    }
    
    /**
     * Constructor.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @param authnRequest authentication request
     * @param spDescriptor <code>SP</code>'s provider descriptor
     * @param spConfig <code>SP</code>'s provider extended meta
     * @param spEntityId <code>SP</code>'s entity id
     * @param relayState where to go after single sign on is done
     * @param ssoToken token of the user to be single sign-oned
     */
    public FSSSOAndFedHandler(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FSAuthnRequest authnRequest, 
        SPDescriptorType spDescriptor, 
        BaseConfigType spConfig,
        String spEntityId,
        String relayState, 
        Object ssoToken) 
    {
        this.request = request;
        this.response = response;
        this.relayState = relayState;
        this.authnRequest = authnRequest;
        this.spDescriptor = spDescriptor;
        this.spConfig = spConfig;
        this.spEntityId = spEntityId;
        this.ssoToken = ssoToken;
    }
    
    /**
     * Constructor.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @param authnRequest authentication request
     * @param spDescriptor <code>SP</code>'s provider descriptor
     * @param spConfig <code>SP</code>'s extended meta
     * @param spEntityId <code>SP</code>'s entity id
     * @param relayState where to go after single sign on is done
     */
    public FSSSOAndFedHandler(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest,
        SPDescriptorType spDescriptor,
        BaseConfigType spConfig,
        String spEntityId,
        String relayState) 
    {
        this.request = request;
        this.response = response;
        this.authnRequest = authnRequest;
        this.spDescriptor = spDescriptor;
        this.spConfig = spConfig;
        this.spEntityId = spEntityId;
        this.relayState = relayState;
    }
    
    /**
     * Constructor.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     */
    public FSSSOAndFedHandler(
        HttpServletRequest request, 
        HttpServletResponse response) 
    {
        this.request = request;
        this.response = response;
    }
    
   
    /**
     * Handles authentication request.
     * @param authnRequest <code>FSAuthnRequest</code> object
     * @return <code>true</code> if the request is handled successfully;
     *  <code>false</code> otherwise.
     */
    public boolean processPreAuthnSSO(FSAuthnRequest authnRequest) 
    {
        FSUtils.debug.message("FSSSOAndFedHandler.processPreAuthnSSO: Called");
        String loginURL = null;
        List authenticationContextClassRef = null;
        String currentAuthnContextRef = null;
        String authType = null;
        FSAuthContextResult authnResult = null;
        FSSessionManager sessionMgr = 
            FSSessionManager.getInstance(metaAlias);
        
        if (authnRequest.getAuthnContext() != null){
            authenticationContextClassRef = 
                authnRequest.getAuthnContext().getAuthnContextClassRefList();
            if (authenticationContextClassRef == null) {
                String authCtxRefDefault = 
                    IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD;
               authenticationContextClassRef = new ArrayList();
               authenticationContextClassRef.add(authCtxRefDefault);
            }
            authType = authnRequest.getAuthContextCompType();
            currentAuthnContextRef = null;
        }
        
        boolean authenticated = true;
        Object ssoToken = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            ssoToken = sessionProvider.getSession(request);
            if (ssoToken == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.processPreAuthnSSO: "
                        + "session is null. User is not authenticated.");
                }
                authenticated = false;
            } else if(!sessionProvider.isValid(ssoToken)) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.processPreAuthnSSO: "
                        + "session is not valid. User is not authenticated.");
                }
                authenticated = false;
            } else {
                FSSession ssoSession = sessionMgr.getSession(ssoToken);
                if (ssoSession != null) {
                    currentAuthnContextRef = ssoSession.getAuthnContext();
                    if (currentAuthnContextRef != null){
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSSSOAndFedHandler."
                                + "processPreAuthnSSO: User has an existing "
                                + "valid session with authnContext: " 
                                + currentAuthnContextRef);
                        }
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSSSOAndFedHandler."
                                + "processPreAuthnSSO: User's authentication"
                                + " context information not found using "
                                + "default authentication context");
                        }
                        currentAuthnContextRef = 
                            IDFFMetaUtils.getFirstAttributeValueFromConfig(
                                hostedConfig, 
                                IFSConstants.DEFAULT_AUTHNCONTEXT);
                    }
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSSSOAndFedHandler.process"
                            + "PreAuthnSSO: User's authenticated session "
                            + "information is not present in FSSessionManager. "
                            + "using default authentication context");
                    }
                    currentAuthnContextRef = 
                        IDFFMetaUtils.getFirstAttributeValueFromConfig(
                            hostedConfig, IFSConstants.DEFAULT_AUTHNCONTEXT);
                }
                authenticated = true;
            }
            if (authenticated) {
                // add a listener. TODO : more than one listeners could be
                // added in case of multiple SPs
                try {
                    sessionProvider.addListener(ssoToken,
                        new FSTokenListener(metaAlias));
                } catch (Exception e) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSSSOAndFedHandler.processPreAuthnSSO: " +
                            "Couldn't add listener to session:", e);
                    }
                }
            }
        } catch(SessionException se) {
            FSUtils.debug.message("FSSSOAndFedHandler.processPreAuthnSSO: "
                + "SSOException Occured: User does not have session " +
                se.getMessage());
            authenticated = false;
        }

        //Initiate proxying
        if (!authenticated) {
            try {
                boolean isProxy = isIDPProxyEnabled(authnRequest);
                if (isProxy && !authnRequest.getFederate()) {
                    String preferredIDP = getPreferredIDP(authnRequest);
                    if (preferredIDP != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSSSOAndFedHandler.process" +
                                "PreAuthnSSO:IDP to be proxied" + preferredIDP);
                        }
                        sendProxyAuthnRequest(authnRequest, preferredIDP);
                        return true;
                    }
                    //else continue for the local authentication.
                }
            } catch (FSRedirectException re) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandle.processPreAuthnSSO:"
                        + "Redirecting for the proxy handling.");
                }
                return true;
            } catch (Exception ex) {
                FSUtils.debug.error("FSSSOAndFedHandler.processPreAuthnSSO:" +
                    "Exception occured while processing for the proxy.", ex);
                return false;
            }
        }
        
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedHandler.processPreAuthnSSO: "
                    + "User's authentication status: " + authenticated);
            }
            FSAuthnDecisionHandler authnDecisionHandler = 
                new FSAuthnDecisionHandler(realm, hostedEntityId,request);
            List defAuthnCxtList = new ArrayList();
            defAuthnCxtList.add(IDFFMetaUtils.getFirstAttributeValueFromConfig(
                hostedConfig, IFSConstants.DEFAULT_AUTHNCONTEXT));

            if (authnRequest.getIsPassive()){
                if (authnRequest.getForceAuthn()){
                    if (FSUtils.debug.warningEnabled()) {
                        FSUtils.debug.warning("FSSSOAndFedHandler.PreAuthnSSO: "
                            + "IDP is passive can't force authentication.");
                    }
                    return false;
                } else {
                    if (authenticated){
                        if (authenticationContextClassRef != null){
                            authnResult = 
                                authnDecisionHandler.decideAuthnContext(
                                    authenticationContextClassRef, 
                                    currentAuthnContextRef,
                                    authType);
                        } else {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSSSOAndFedHandler."
                                    + "processPreAuthnSSO: User's "
                                    + "authentication context is default");
                            }
                            authnResult =
                                authnDecisionHandler.getURLForAuthnContext(
                                    defAuthnCxtList,
                                    authType);
                        }
                        if (authnResult == null) {
                            return false;
                        }
                        if (authnResult.getLoginURL() != null) {
                            // When it's not null.,
                            // we should show the login page
                            // may be it'asking for higher auth context.
                            loginURL = authnResult.getLoginURL();
                            loginURL = formatLoginURL(loginURL,
                                authnResult.getAuthContextRef());
                            FSUtils.forwardRequest(request, response, loginURL);
                            return true;
                        } else {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSSSOAndFedHandler."
                                    + "processPreAuthnSSO: User's "
                                    + "authentication "
                                    + "context is evaluated to be valid");
                            }
                            return processPostAuthnSSO(authnRequest);
                        }
                    } else {
                        if (FSUtils.debug.warningEnabled()) {
                            FSUtils.debug.warning(
                                "FSSSOAndFedHandler.processPreAuthnSSO: " +
                                "IDP is passive and user is not authenticated");
                        }
                        noFedStatus = new Status(
                            new StatusCode("samlp:Responder",
                                new StatusCode("lib:NoPassive", null)),
                            FSUtils.bundle.getString(
                                "AuthnRequestProcessingFailed"), 
                            null);
                        return false;
                    }
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOAndFedHandler."
                        + "processPreAuthnSSO: AuthnRequest is active");
                }
                if (authnRequest.getForceAuthn()){
                    if (authenticationContextClassRef != null){
                        authnResult = 
                            authnDecisionHandler.getURLForAuthnContext(
                                                authenticationContextClassRef,
                                                authType);
                    } else {
                        authnResult =
                            authnDecisionHandler.getURLForAuthnContext(
                                defAuthnCxtList);
                    }
                    if (authnResult == null || 
                        authnResult.getLoginURL() == null || 
                        authnResult.getLoginURL().length() == 0)
                    {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOAndFedHandler.processPreAuthnSSO:"
                                + "AuthnDecision engine failed to take a "
                                + "authn decision");
                        }
                        return false;
                    } else {
                        if (ssoToken != null) {
                            try {
                                SessionManager.getProvider().invalidateSession(
                                    ssoToken, request, response);
                            } catch (SessionException ssoe) {
                                FSUtils.debug.error(
                                    "FSSSOAndFedHandler.processPreAuthnSSO:" +
                                    "Unable to invalidate the sso session.");
                            }
                            ssoToken = null;
                        }

                        loginURL = authnResult.getLoginURL();
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOAndFedHandler.processPreAuthnSSO: "
                                + "AuthnDecision engine returned: " 
                                + loginURL);
                        }
                    }
                    loginURL = formatLoginURL(loginURL, 
                                            authnResult.getAuthContextRef());
                    FSUtils.forwardRequest(request, response, loginURL);
                    response.flushBuffer ();
                    return true;
                } else {
                    if (authenticated){
                        if (authenticationContextClassRef != null){
                            authnResult = 
                                authnDecisionHandler.decideAuthnContext(
                                    authenticationContextClassRef, 
                                    currentAuthnContextRef,
                                    authType);
                        } else {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSSSOAndFedHandler."
                                    + "processPreAuthnSSO: User's "
                                    + "authentication "
                                    + "context is default");
                            }
                            authnResult =
                                authnDecisionHandler.getURLForAuthnContext(
                                    defAuthnCxtList,
                                    authType);
                        }
                        if (authnResult == null){
                            return false;
                        } else if(authnResult.getLoginURL() == null){
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSSSOAndFedHandler."
                                    + "processPreAuthnSSO: User's "
                                    + "authentication "
                                    + "context is evaluated to be valid");
                            }
                            return processPostAuthnSSO(authnRequest);
                        } else if(authnResult.getLoginURL().length() == 0){
                            return false;
                        } else {
                            loginURL = authnResult.getLoginURL();
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSSOAndFedHandler.processPreAuthnSSO"
                                    + ": AuthnDecision engine returned: "
                                    + loginURL);
                            }
                        }
                        loginURL = formatLoginURL(loginURL, 
                                        authnResult.getAuthContextRef());
                        FSUtils.forwardRequest(request, response, loginURL);
                        return true;
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSSSOAndFedHandler."
                                + "processPreAuthnSSO: AuthnRequest is active");
                        }
                        //redirect for authentication authnContextRef
                        if (authenticationContextClassRef != null){
                            authnResult = 
                                authnDecisionHandler.getURLForAuthnContext(
                                    authenticationContextClassRef,
                                    authType);
                        } else {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSSSOAndFedHandler."
                                    + "processPreAuthnSSO: User's "
                                    + "authentication "
                                    + "context is default");
                            }
                            authnResult =
                                authnDecisionHandler.getURLForAuthnContext(
                                    defAuthnCxtList,
                                    authType);
                        }
                        if (authnResult == null || 
                            authnResult.getLoginURL() == null || 
                            authnResult.getLoginURL().length() == 0)
                        {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSSOAndFedHandler. processPreAuthnSSO: "
                                    + " AuthnDecision engine"
                                    + " failed to take a decision");
                            }
                            noFedStatus = new Status(
                                new StatusCode("samlp:Responder",
                                    new StatusCode("lib:NoAuthnContext", null)),
                                FSUtils.bundle.getString(
                                    "AuthnRequestProcessingFailed"),
                                null);
                            return false;
                        } else {
                            loginURL = authnResult.getLoginURL();
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSSOAndFedHandler.processPreAuthnSSO: "
                                    + "AuthnDecision engine returned: " 
                                    + loginURL);
                            }
                        }
                        loginURL = formatLoginURL(loginURL, 
                                        authnResult.getAuthContextRef());
                        FSUtils.forwardRequest(request, response, loginURL);
                        return true;
                    }
                }
            }
        } catch(Exception e){
            FSUtils.debug.error("FSSSOAndFedHandler.processPreAuthnSSO: "
                + "Exception occured");
            return processPostAuthnSSO(authnRequest);
        }
    }
    
    /**
     * Generates local login url.
     * @param loginUrl authentication base url
     * @param authnContext requested <code>AuthnContextRef</code>
     * @return local login url with appropriate parameters
     */
    public String formatLoginURL(
        String loginUrl, 
        String authnContext
    ) 
    {
        FSUtils.debug.message("FSSSOAndFedHandler.formatLoginURL: Called");
        
        try {
            if (loginUrl == null){
                FSUtils.debug.error("FSSSOAndFedHandler.formatLoginURL: ");
                return null;
            }
            
            //create return url
            String ssoUrl = hostedDesc.getSingleSignOnServiceURL();
            StringBuffer returnUrl = new StringBuffer(ssoUrl);
            if (ssoUrl.indexOf('?') == -1) {
                returnUrl.append("?");
            } else {
                returnUrl.append("&");
            }
            returnUrl.append(IFSConstants.AUTHN_INDICATOR_PARAM)
                .append("=").append(IFSConstants.AUTHN_INDICATOR_VALUE)
                .append("&").append(IFSConstants.AUTHN_CONTEXT)
                .append("=").append(URLEncDec.encode(authnContext))
                .append("&").append(IFSConstants.REALM)
                .append("=").append(URLEncDec.encode(realm))
                .append("&").append(IFSConstants.PROVIDER_ID_KEY)
                .append("=").append(URLEncDec.encode(hostedEntityId))
                .append("&").append(IFSConstants.META_ALIAS)
                .append("=").append(URLEncDec.encode(metaAlias))
                .append("&").append(IFSConstants.AUTH_REQUEST_ID)
                .append("=").append(URLEncDec.encode(
                    authnRequest.getRequestID()));
            
            //create goto url
            String postLoginUrl = FSServiceUtils.getBaseURL(request) 
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
            redirectUrl.append(loginUrl);
            if (loginUrl.indexOf('?') == -1) {
                redirectUrl.append("?");
            } else {
                redirectUrl.append("&");
            }
            redirectUrl.append(IFSConstants.GOTO_URL_PARAM).append("=")
                .append(URLEncDec.encode(gotoUrl.toString()));
            
            redirectUrl.append("&").append(IFSConstants.ORGKEY).append("=")
                    .append(URLEncDec.encode(realm));
            //will change
            //request.getSession(true)
             //      .setAttribute(IFSConstants.AUTHN_CONTEXT, authnContext);
            int len = redirectUrl.length() - 1;
            if (redirectUrl.charAt(len) == '&') {
                redirectUrl = redirectUrl.deleteCharAt(len);
            }
            return redirectUrl.toString();
        } catch(Exception e){
            FSUtils.debug.error(
                "FSSSOAndFedHandler.formatLoginURL: Exception: " ,e);
            return null;
        }
    }
    
    /**
     * Handles authentication request after local login.
     * @param authnRequest <code>FSAuthnRequest</code> object
     * @return <code>true</code> if the request is handled successfully;
     *  <code>false</code> otherwise.
     */
    public boolean processPostAuthnSSO(FSAuthnRequest authnRequest) {
        FSUtils.debug.message("FSSSOAndFedHandler.processPostAuthnSSO: Called");
        SessionProvider sessionProvider = null;
        try {
            sessionProvider = SessionManager.getProvider();
            if (ssoToken == null) {
                ssoToken = sessionProvider.getSession(request);
            }
            if ((ssoToken == null) || (!sessionProvider.isValid(ssoToken))) {
                FSUtils.debug.error("FSSSOAndFedHandler.processPostAuthnSSO: "
                    + "session is not valid.");
                return false;
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.processPostAuthnSSO: "
                        + "session is valid.");
                }
            }
        } catch(SessionException se) {
            FSUtils.debug.error("FSSSOAndFedHandler.processPostAuthnSSO: ", se);
            return false;
        }

        //save session
        String userID = null;
        String sessionID = null;
        try {
            userID = sessionProvider.getPrincipalName(ssoToken);
            sessionID = sessionProvider.getSessionID(ssoToken);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSSOAndFedHandler.processPostAuthnSSO: "
                    + "UserID of the principal in the session: " + userID
                    + "sessionID of the session: " + sessionID);
            }
        } catch(SessionException ex){
            FSUtils.debug.error("FSSSOAndFedHandler.processPostAuthnSSO: "
                + "SessionException occured. "
                + "Principal information not found in the session: ", ex);
            return false;
        }
        FSSessionManager sessionManager = 
            FSSessionManager.getInstance(metaAlias);
        FSSession session = sessionManager.getSession(userID, sessionID);
        
        if (session != null){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedHandler.processPostAuthnSSO: "
                    + "An existing SSO session found with ID:" 
                    + session.getSessionID());
            }
            session.addSessionPartner(
                new FSSessionPartner(spEntityId, false));
            sessionManager.addSession(userID, session);
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedHandler.processPostAuthnSSO: "
                    + "No existing SSO session found. "
                    + "Entering a new session to the session manager with ID: " 
                    + sessionID);
            }
            session = new FSSession(sessionID);
            String sessionIndex = SAMLUtils.generateID();
            session.setSessionIndex(sessionIndex);
            session.addSessionPartner(
                new FSSessionPartner(spEntityId, false));
            sessionManager.addSession(userID, session);
        }

        // check for federation
        String autoFedStr = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.ENABLE_AUTO_FEDERATION);
        if (authnRequest.getFederate() || 
            (autoFedStr != null && autoFedStr.equalsIgnoreCase("true")))
        {
            FSAccountFedInfo fedInfo = doAccountFederation(ssoToken,
                authnRequest, session);
            NameIdentifier spNI = null;
            NameIdentifier idpNI = null;
            if(fedInfo == null){
                FSUtils.debug.error("FSSSOAndFedHandler.processPostAuthnSSO: "
                    + "Accountfederation failed");
                return false;
            } else {
                spNI = fedInfo.getRemoteNameIdentifier();
                idpNI = fedInfo.getLocalNameIdentifier();
                if (idpNI == null){
                    idpNI = fedInfo.getRemoteNameIdentifier();
                    if (idpNI == null) {
                        FSUtils.debug.error("FSSSOAndFedHandler.processPost" +
                            "AuthnSSO: Opaque handle not found");
                        return false;
                    }
                }
                if (spNI == null){
                    spNI = idpNI;
                }
            }
            return doSingleSignOn(ssoToken, 
                                  authnRequest.getRequestID(), 
                                  spNI, 
                                  idpNI);
        } else {
            return doSingleSignOn(ssoToken, authnRequest.getRequestID());
        }
    }
    
    
    protected FSAuthnResponse createAuthnResponse(
        Object ssoToken,
        String inResponseTo,
        NameIdentifier userHandle,
        NameIdentifier idpHandle)
    {
        FSUtils.debug.message(
            "FSSSOAndFedHandler.createAuthnResponse:  Called");
        FSAuthnResponse authnResponse = null;
        try {
            String requestID = authnRequest.getRequestID();
            FSAssertionManager am = 
                FSAssertionManager.getInstance(metaAlias);
            FSAssertion assertion = null;
            SessionProvider sessionProvider = SessionManager.getProvider();
            assertion = am.createFSAssertion(
                sessionProvider.getSessionID(ssoToken),
                null,
                realm,
                spEntityId,
                userHandle,
                idpHandle,
                inResponseTo,
                authnRequest.getMinorVersion());
            StatusCode statusCode = new StatusCode(
                IFSConstants.STATUS_CODE_SUCCESS);
            Status status = new Status(statusCode);
            List contents = new ArrayList();
            contents.add(assertion);
            authnResponse = new FSAuthnResponse(null, 
                                    requestID, 
                                    status, 
                                    contents, 
                                    relayState);
            authnResponse.setMinorVersion(authnRequest.getMinorVersion());
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedHandler.createAuthnResponse: "
                    + "CHECK1: " + hostedEntityId);
            }
            authnResponse.setProviderId(hostedEntityId);
        } catch(FSException se) {
            FSUtils.debug.error(
                "FSSSOAndFedHandler.createAuthnResponse: FSException: ", se);
            return null;
        } catch(SAMLException se) {
            FSUtils.debug.error("FSSSOAndFedHandler.createAuthnResponse: "
                + "SAMLException: ", se);
            return null;
        } catch (SessionException se) {
            FSUtils.debug.error("FSSSOAndFedHandler.createAuthnResponse: "
                + "SessionException: ", se);
            return null;
        }
        // sign AuthnResponse
        return authnResponse;
    }
    
    
    protected boolean doSingleSignOn(
        Object ssoToken,
        String inResponseTo,
        NameIdentifier spHandle,
        NameIdentifier idpHandle)
    {
        return false;
    }
    
    protected boolean doSingleSignOn(
        Object ssoToken, 
        String inResponseTo) 
    {
        FSUtils.debug.message("FSSSOAndFedHandler.doSingleSignOn(2):  Called");
        try {
            String securityDomain = authnRequest.getProviderId();
            String affiliationID = authnRequest.getAffiliationID();
            if (affiliationID != null) {
                securityDomain = affiliationID;
            }
            SessionProvider sessionProvider = SessionManager.getProvider();
            String userID = sessionProvider.getPrincipalName(ssoToken);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedHandler.doSingleSignOn: "
                    + "Initiating SSO for user with ID: " + userID);
            }
            FSAccountFedInfo accountInfo = accountManager.readAccountFedInfo(
                userID, securityDomain);

            if (accountInfo == null) {
                FSUtils.debug.error(
                    "FSSSOAndFedHandler.doSingleSignOn: Account Federation "
                    + "Information not found for user with ID: "
                    + userID);
                noFedStatus = new Status(
                    new StatusCode("samlp:Responder",
                        new StatusCode("lib:FederationDoesNotExist", null)),
                    FSUtils.bundle.getString("AuthnRequestProcessingFailed"),
                    null);
                String[] data = { userID };
                LogUtil.error(Level.INFO,
                    LogUtil.USER_ACCOUNT_FEDERATION_INFO_NOT_FOUND, 
                    data, ssoToken);
                return false;
            }

            if (accountInfo != null &&
                accountInfo.isFedStatusActive() &&
                accountInfo.getLocalNameIdentifier() != null) 
            {
                // Check if this is 6.2
                NameIdentifier localNI = accountInfo.getLocalNameIdentifier();
                String qualifier = localNI.getNameQualifier(); 
                if (qualifier != null && qualifier.equals(hostedEntityId)) {
                    localNI = new NameIdentifier(
                        localNI.getName(), securityDomain);
                    NameIdentifier remoteNI = 
                        accountInfo.getRemoteNameIdentifier();
                    if (remoteNI != null) {
                        remoteNI = new NameIdentifier(remoteNI.getName(),
                            securityDomain);
                    }
                    FSAccountFedInfoKey newFedKey = new FSAccountFedInfoKey(
                        securityDomain, localNI.getName());
                    accountInfo = new FSAccountFedInfo(
                        securityDomain, localNI, remoteNI, false);
                    accountManager.writeAccountFedInfo(
                        userID, newFedKey, accountInfo);
                    FSAccountFedInfoKey oldFedKey = new FSAccountFedInfoKey(
                        hostedEntityId, localNI.getName());
                    accountManager.removeAccountFedInfoKey(userID, oldFedKey);
                }
            }
            NameIdentifier idpNI = accountInfo.getLocalNameIdentifier();
            if (idpNI == null){
                idpNI = accountInfo.getRemoteNameIdentifier(); 
                if (idpNI == null) {
                    FSUtils.debug.error("FSSSOAndFedHandler.doSingleSignOn: "
                        + "NameIdentifier not found");
                    return false;
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOAndFedHandler.doSingleSignOn: "
                        + "IDP generated opaque handle: " + idpNI.getName());
                }
            }
            
            NameIdentifier spNI = accountInfo.getRemoteNameIdentifier();
            if (spNI == null){
                spNI = idpNI;
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOAndFedHandler.doSingleSignOn: "
                        + "SP generated opaque handle: " + spNI.getName());
                }
            }
            return doSingleSignOn(ssoToken, inResponseTo, spNI, idpNI);
        } catch(Exception e){
            FSUtils.debug.error("FSSSOAndFedHandler.doSingleSignOn: "
                + "Exception during Single Sign-On:", e);
            return false;
        }
    }
    
    
    protected FSAccountFedInfo doAccountFederation(
        Object ssoToken, 
        FSAuthnRequest authnRequest,
        FSSession session)
    {
        FSUtils.debug.message("FSSSOAndFedHandler.doAccountFederation: Called");
        String nameIDPolicy = authnRequest.getNameIDPolicy();
        String affiliationID = authnRequest.getAffiliationID();
        boolean isAffiliationFed = false;
        if (affiliationID != null) {
            try {
                isAffiliationFed =  metaManager.isAffiliateMember(
                            realm, hostedEntityId, affiliationID);
            } catch (Exception e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOAndFedHandler.doAccount" +
                        "Federation:Error in checking for the affiliation:", e);
                }
            }
        }

        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            String userID = sessionProvider.getPrincipalName(ssoToken);
            FSAccountFedInfo existActInfo = null;
            if (isAffiliationFed) {
                existActInfo = accountManager.readAccountFedInfo(
                    userID, affiliationID);
                if (existActInfo != null && existActInfo.isFedStatusActive()) {
                    return existActInfo;
                }
            }

            // Check if there is an existing fed info
            String nameQualifier = authnRequest.getProviderId(); 
            existActInfo = accountManager.readAccountFedInfo(
                userID, nameQualifier);
            if (existActInfo != null && existActInfo.isFedStatusActive()) {
                return existActInfo;
            }

            FSNameIdentifierHelper nameHelper = 
                new FSNameIdentifierHelper(hostedConfig);
            String opaqueHandle = nameHelper.createNameIdentifier();
            if (opaqueHandle == null){
                FSUtils.debug.error("FSSSOAndFedHandler.doAccountFederation: "
                    + "Could not generate handle");
                return null;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOAndFedHandler.doAccountFederation: "
                    + "Generated handle: " + opaqueHandle);
            }
            if (isAffiliationFed) {
                nameQualifier = affiliationID;
            }
            NameIdentifier ni = new NameIdentifier(opaqueHandle, nameQualifier);
            if (authnRequest.getMinorVersion() == 
                IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) 
            {
                if (nameIDPolicy == null ||
                    !nameIDPolicy.equals(IFSConstants.NAME_ID_POLICY_ONETIME))
                {
                    ni.setFormat(IFSConstants.NI_FEDERATED_FORMAT_URI);
                } else {
                    ni.setFormat(IFSConstants.NI_ONETIME_FORMAT_URI);
                }
            }
            FSAccountFedInfo accountInfo = new FSAccountFedInfo(
                authnRequest.getProviderId(), ni, null, false);

            FSAccountFedInfoKey fedKey = null;
            if (isAffiliationFed) {
                fedKey = new FSAccountFedInfoKey(affiliationID, opaqueHandle);
                accountInfo.setAffiliation(true);
            } else {
                fedKey = new FSAccountFedInfoKey(
                    authnRequest.getProviderId(), opaqueHandle);
            }

            if (nameIDPolicy == null || !nameIDPolicy.equals("onetime")) {
                accountManager.writeAccountFedInfo(userID, fedKey, accountInfo);
            } else {
                session.setOneTime(true);
                session.setAccountFedInfo(accountInfo);
                session.setUserID(userID);
            }
            return accountInfo;
        } catch(Exception ex){
            FSUtils.debug.error("FSSSOAndFedHandler.doAccountFederation: "
                + "Exception when doing account federation", ex);
            return null;
        }
    }
    
    protected void returnErrorResponse() {
    }
    
    /**
     * Processes <code>SAML</code> request.
     * @param samlRequest <code>FSSAMLRequest</code> object
     * @return generated <code>FSResponse</code> object
     */
    public FSResponse processSAMLRequest(FSSAMLRequest samlRequest) {
        FSUtils.debug.error("FSSSOAndFedHandler.processSAMLRequest: "
            + "Call should not resolve here, abstract class.");
        return null;
    }
    
    /**
     * Processes authentication request.
     * @param authnRequest authentication request
     * @param bPostAuthn <code>true</code> indicates it's post authentication;
     *  <code>false</code> indicates it's pre authentication.
     */
    public void processAuthnRequest(
        FSAuthnRequest authnRequest,
        boolean bPostAuthn
    ) 
    {
        FSUtils.debug.message("FSSSOAndFedHandler.processAuthnRequest: Called");
        this.authnRequest = authnRequest;
        String message = null;
        String inResponseTo = authnRequest.getRequestID();
        Status status = null;
        FSAuthnResponse errResponse = null;
        spEntityId = authnRequest.getProviderId();

        try {
            spDescriptor = metaManager.getSPDescriptor(realm, spEntityId);
            spConfig = metaManager.getSPDescriptorConfig(realm, spEntityId);
            if (!metaManager.isTrustedProvider(
                realm, hostedEntityId, spEntityId)) 
            { 
                FSUtils.debug.error(
                    "FSSSOAndFedHandler.processAuthnRequest: "
                    + "RemoteProvider is not trusted");
                message = FSUtils.bundle.getString(
                    "AuthnRequestProcessingFailed");
                status = new Status(
                    new StatusCode("samlp:Responder"), message, null);
                errResponse = new FSAuthnResponse(null, 
                                  inResponseTo,
                                  status,
                                  null,
                                  relayState);
                errResponse.setMinorVersion(
                    authnRequest.getMinorVersion());
                sendAuthnResponse(errResponse);
                return;
            }
            if (bPostAuthn){
                if (processPostAuthnSSO(authnRequest)){
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSSSOAndFedHandler."
                            + "processAuthnRequest: AuthnRequest Processing "
                            + "successful");
                    }
                    return;
                } else {
                    if (FSUtils.debug.warningEnabled()) {
                        FSUtils.debug.warning(
                            "FSSSOAndFedHandler.processAuthnRequest: "
                            + "AuthnRequest Processing failed");
                    }
                    message = FSUtils.bundle.getString(
                        "AuthnRequestProcessingFailed");
                    if (noFedStatus != null) {
                        status = noFedStatus;
                    } else {
                        status = new Status(
                            new StatusCode("samlp:Responder"), message, null);
                    }
                    errResponse = new FSAuthnResponse(null, 
                                                    inResponseTo,
                                                    status,
                                                    null,
                                                    relayState);
                    errResponse.setMinorVersion(authnRequest.getMinorVersion());
                    sendAuthnResponse(errResponse);
                    return;
                }
            } else {
                boolean authnRequestSigned =
                    spDescriptor.isAuthnRequestsSigned();
                
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.processAuthnRequest: "
                        + "ProviderID : " + spEntityId
                        + " AuthnRequestSigned :this is for testing " 
                        + authnRequestSigned);
                }
                if (FSServiceUtils.isSigningOn()){
                    if (authnRequestSigned){
                        //verify request signature
                        if (!verifyRequestSignature(authnRequest)){
                            FSUtils.debug.error("FSSSOAndFedHandler."
                                + "processAuthnRequest: "
                                + "AuthnRequest Signature Verification Failed");
                            message = FSUtils.bundle.getString(
                                "signatureVerificationFailed");
                            String[] data = { message };
                            LogUtil.error(Level.INFO,
                                LogUtil.SIGNATURE_VERIFICATION_FAILED,
                                data, ssoToken);
                            status = new Status(
                                new StatusCode("samlp:Responder",
                                    new StatusCode(
                                        "lib:UnsignedAuthnRequest", null)),
                                message,
                                null);
                            errResponse = new FSAuthnResponse(null, 
                                            inResponseTo, 
                                            status,
                                            null,
                                            relayState);
                            errResponse.setMinorVersion(
                                authnRequest.getMinorVersion());
                            sendAuthnResponse(errResponse);
                            return;
                        } else {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSSOAndFedHandler. processAuthnRequest"
                                    + ": AuthnRequest Signature Verified");
                            }
                        }
                    }
                }
                if (processPreAuthnSSO(authnRequest)){
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSSSOAndFedHandler.processAuthnRequest: "
                            + "AuthnRequest Processing successful");
                    }
                    return;
                } else {
                    if (FSUtils.debug.warningEnabled()) {
                        FSUtils.debug.warning(
                            "FSSSOAndFedHandler.processAuthnRequest: "
                            + "AuthnRequest Processing failed");
                    }
                    String[] data = { FSUtils.bundle.getString(
                        "AuthnRequestProcessingFailed") };
                    LogUtil.error(Level.INFO,
                        LogUtil.AUTHN_REQUEST_PROCESSING_FAILED,
                        data, ssoToken);
                    message = FSUtils.bundle.getString(
                        "AuthnRequestProcessingFailed");
                    status = new Status(
                        new StatusCode("samlp:Responder"), message, null);
                    if (noFedStatus != null) {
                        status = noFedStatus;
                    }
                    errResponse = new FSAuthnResponse(null, 
                                inResponseTo,
                                status,
                                null,
                                relayState);
                    errResponse.setMinorVersion(authnRequest.getMinorVersion());
                    sendAuthnResponse(errResponse);
                    return;
                }
            }
        } catch(Exception e){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(out));
            FSUtils.debug.error("FSSSOAndFedHandler.processAuthnRequest: "
                + "Exception Occured: " + e.getMessage() 
                + "Stack trace is " + out.toString());
            message = FSUtils.bundle.getString("AuthnRequestProcessingFailed");
            try {
                status = new Status(
                    new StatusCode("samlp:Responder"), message, null);
                errResponse = new FSAuthnResponse(null, 
                                                inResponseTo,
                                                status,
                                                null, 
                                                relayState);
                errResponse.setMinorVersion(authnRequest.getMinorVersion());
                sendAuthnResponse(errResponse);
            } catch(Exception ex){
                if (FSUtils.debug.messageEnabled()){
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.processAuthnRequest: "
                        + "Exception Occured: ", ex);
                }
            }
        }
    }
    
    protected void sendAuthnResponse(
        FSAuthnResponse authnResponse
    )
    {
        FSUtils.debug.error("FSSSOAndFedHandler.sendAuthnResponse: "
            + "Call should not resolve here. error");
    }
    
    /**
     * Sets remote <code>SP</code> provider descriptor.
     * @param spDescriptor remote <code>SP</code> provider descriptor.
     * @see #getProvider()
     */
    public void setSPDescriptor(SPDescriptorType spDescriptor) {
        this.spDescriptor = spDescriptor;
    }
    
    /**
     * Returns remote <code>SP</code> provider descriptor.
     *
     * @return remote <code>SP</code> provider descriptor
     */
    public SPDescriptorType getProvider() {
        return spDescriptor;
    }
    
    protected boolean verifyRequestSignature(FSAuthnRequest authnRequest) {
        FSUtils.debug.message(
            "FSSSOAndFedHandler.verifyRequestSignature: Called");
        try {
            X509Certificate cert = KeyUtil.getVerificationCert(
                spDescriptor, spEntityId, false);
            
            if (cert == null) {
                if(FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.verifyRequestSignature: "
                        + "couldn't obtain this site's cert.");
                }
                throw new FSException(IFSConstants.NO_CERT, null);
            }
            
            if (request.getMethod().equals("GET")){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.verifyRequestSignature: "
                        + "Request is sent by GET" );
                }
                String sigAlg = request.getParameter("SigAlg");
                String encSig = request.getParameter("Signature");
                if (sigAlg == null || 
                    sigAlg.length() == 0 || 
                    encSig == null || 
                    encSig.length() == 0)
                {
                    return false;
                }
                String algoId = null;
                if (sigAlg.equals(IFSConstants.ALGO_ID_SIGNATURE_DSA)) {
                    algoId = IFSConstants.ALGO_ID_SIGNATURE_DSA_JCA;
                } else if (sigAlg.equals(IFSConstants.ALGO_ID_SIGNATURE_RSA)) {
                    algoId = IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA;
                } else {
                    FSUtils.debug.error(
                        "FSSSOAndFedHandler.signAndReturnQueryString: "
                        + "Invalid signature algorithim");
                    return false;
                }
                String queryString = request.getQueryString(); 
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOAndFedHandler.verifyRequest" +
                        "Signature: queryString:" + queryString);
                }
                int sigIndex = queryString.indexOf("&Signature");
                String newQueryString = queryString.substring(0, sigIndex);
                byte[] signature = null;
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.verifyRequestSignature: "
                        + "Signature: " + encSig + "Algorithm: " + algoId);
                }
                signature = Base64.decode(encSig);
                
                FSSignatureManager fsmanager = FSSignatureManager.getInstance();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.verifyRequestSignature: "
                        + "String to be verified: " + newQueryString);
                }
                return fsmanager.verifySignature(newQueryString, 
                                                signature, 
                                                algoId, 
                                                cert);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSSOAndFedHandler.verifyRequestSignature: "
                        + "Request is sent by POST ");
                }
                int minorVersion=authnRequest.getMinorVersion(); 
                if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                    return SigManager.getSigInstance().verify(
                        authnRequest.getSignedXMLString(),
                        IFSConstants.ID,
                        Collections.singleton(cert));
                } else if (minorVersion ==
                    IFSConstants.FF_12_PROTOCOL_MINOR_VERSION)
                {
                    return SigManager.getSigInstance().verify(
                        authnRequest.getSignedXMLString(),
                        IFSConstants.REQUEST_ID,
                        Collections.singleton(cert));
                } else { 
                    FSUtils.debug.message("invalid minor version.");
                    return false;          
                }
            }
        } catch(Exception e){
            FSUtils.debug.error("FSSSOAndFedHandler.verifyRequestSignature: "
                + "Exception occured while verifying SP's signature:", e);
            return false;
        }
    }
    
    /**
     * Removes meta alias from request parameters.
     * @param request <code>HttpServletRequest</code> object
     * @return parameter string which doesn't contain meta alias
     */
    public static String cleanMetaAlias(HttpServletRequest request) {
        FSUtils.debug.message("FSSSOAndFedHandler.cleanMetaAlias: Called");
        Enumeration paramEnum = request.getParameterNames();
        String returnString = new String();
        while (paramEnum.hasMoreElements()) {
            String paramKey = (String)paramEnum.nextElement();
            if (paramKey.equalsIgnoreCase(IFSConstants.META_ALIAS)) {
                FSUtils.debug.message(
                    "FSSSOAndFedHandler.cleanMetaAlias: found metaAlias");
            } else {
                String paramValue = request.getParameter(paramKey);
                if (returnString == null || returnString.length() < 1) {
                    returnString =  
                        paramKey + "=" + URLEncDec.encode(paramValue);
                } else {
                    returnString = returnString + "&"
                        +  paramKey + "=" + URLEncDec.encode(paramValue);
                }
            }
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSSOAndFedHandler.cleanMetaAlias: "
                + " returning with " + returnString);
        }
        return returnString;
    }

    /**
     * Sends a new AuthnRequest to the authenticating provider. 
     * @param authnRequest original AuthnRequest sent by the service provider.
     * @param preferredIDP IDP to be proxied.
     * @exception FSException for any federation failure.
     * @exception IOException if there is a failure in redirection.
     */
    protected void sendProxyAuthnRequest (
         FSAuthnRequest authnRequest, String preferredIDP
    ) throws FSException, IOException 
    {

         FSAuthnRequest newAuthnRequest = getNewAuthnRequest(authnRequest);
         if (FSUtils.debug.messageEnabled()) {
             FSUtils.debug.message("FSSSOAndFedHandler.sendProxyAuthnRequest:" +
                 "New Authentication request:" + newAuthnRequest.toXMLString());
         }

         FSSessionManager sessManager = FSSessionManager.getInstance(
             IDFFMetaUtils.getMetaAlias(
                 realm, hostedEntityId, IFSConstants.SP, null));
         String requestID = newAuthnRequest.getRequestID();
         sessManager.setAuthnRequest(requestID, newAuthnRequest);
         sessManager.setProxySPDescriptor(requestID, spDescriptor);
         sessManager.setProxySPAuthnRequest(requestID, authnRequest);
         sessManager.setIDPEntityID(requestID, preferredIDP);

         String targetURL = null;
         SPDescriptorType localDescriptor = null;
         BaseConfigType localDescriptorConfig = null;
         try {
             IDPDescriptorType idpDescriptor = 
                 metaManager.getIDPDescriptor(realm, preferredIDP);

             targetURL = idpDescriptor.getSingleSignOnServiceURL();
             if (targetURL == null) {
                 FSUtils.debug.error(
                     "FSSSOAndFedHandler.sendProxyAuthnRequest: Single " +
                     "Sign-on service is not found for the proxying IDP");
                 return;
             }

             localDescriptor = metaManager.getSPDescriptor(
                 realm, hostedEntityId);
             localDescriptorConfig =
                 metaManager.getSPDescriptorConfig(realm, hostedEntityId);
         } catch (Exception e) {
             FSUtils.debug.error(
                 "FSSSOAndFedHandler.sendProxyAuthnRequest:",e);
             return;
         }

         String queryString = newAuthnRequest.toURLEncodedQueryString();
         if (FSServiceUtils.isSigningOn()) {
             String certAlias = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                 localDescriptorConfig, IFSConstants.SIGNING_CERT_ALIAS);
             if (localDescriptor.isAuthnRequestsSigned()) {
                 queryString = FSSignatureUtil.signAndReturnQueryString(
                     queryString, certAlias);
             }
         }

         StringBuffer tmpURL = new StringBuffer(1000);
         if (targetURL.indexOf("?") != -1) {
             tmpURL.append(targetURL).append("&").append(queryString);
         } else {
             tmpURL.append(targetURL).append("?").append(queryString);
         }
         String redirectURL = tmpURL.toString();
         if (FSUtils.debug.messageEnabled()) {
             FSUtils.debug.message("FSSSOAndFedHandler.sendProxyAuthnRequest:" +
                 "SSO URL to be redirected" + redirectURL);
         }
         response.setStatus(response.SC_MOVED_TEMPORARILY);
         response.setHeader("Location", redirectURL);
         response.sendRedirect(redirectURL);
    }

    /**
     * Checks if the identity provider is configured for proxying the 
     * authentication requests for a requesting service provider.
     * @param authnRequest Authentication Request.
     * @return <code>true</code> if the IDP is configured for proxying.
     * @exception FSException for any failure.
     */
    protected boolean isIDPProxyEnabled(FSAuthnRequest authnRequest)
        throws FSException 
    {

        if (authnRequest.getMinorVersion() != 
            IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) 
        {
            return false;
        }
        FSScoping scoping = authnRequest.getScoping();
        if (scoping != null && scoping.getProxyCount() == 0) {
            return false;
        }
 
        String enabledString = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            spConfig, IFSConstants.ENABLE_IDP_PROXY);
        if (enabledString != null && enabledString.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the preferred IDP Id to be proxied. This method makes use of an
     * SPI to determine the preffered IDP. 
     * @param authnRequest original Authn Request.
     * @return String preferred IDP to be proxied.
     */
    private String getPreferredIDP(FSAuthnRequest authnRequest)
        throws FSRedirectException 
    {
        if (realmProxyFinder != null) {
            return realmProxyFinder.getPreferredIDP(
                authnRequest, realm, hostedEntityId, request, response);
        } else {  
            return proxyFinder.getPreferredIDP(
                authnRequest, hostedEntityId, request, response);
        }
    }


    /**
     * Constructs new authentication request by using the original request
     * that is sent by the service provider to the proxying IDP.
     * @param origRequest Original Authn Request
     * @return FSAuthnRequest new authn request.
     * @exception FSException for failure in creating new authn request.
     */
    private FSAuthnRequest getNewAuthnRequest(
        FSAuthnRequest origRequest
    ) throws FSException 
    {

        // New Authentication request should only be a single sign-on request.
        
        try {
            FSAuthnRequest newRequest = new FSAuthnRequest(null,
                        origRequest.getRespondWith(),
                        hostedEntityId,
                        origRequest.getForceAuthn(),
                        origRequest.getIsPassive(),
                        false,
                        origRequest.getNameIDPolicy(),
                        origRequest.getProtocolProfile(),
                        origRequest.getAuthnContext(),
                        origRequest.getRelayState(),
                        origRequest.getAuthContextCompType());
            newRequest.setMinorVersion(
                IFSConstants.FF_12_PROTOCOL_MINOR_VERSION);
            FSScoping scoping = origRequest.getScoping(); 
            if (scoping != null) {
                int proxyCount = scoping.getProxyCount();
                if (proxyCount > 0 ) {
                    FSScoping newScoping = new FSScoping();
                    newScoping.setProxyCount(proxyCount-1);
                    newScoping.setIDPList(scoping.getIDPList());
                    newRequest.setScoping(newScoping);
                }
            }
            return newRequest;
        } catch (Exception ex) {
            FSUtils.debug.error("FSSSOAndFedHandler.getNewAuthnRequest:" +
                "Error in creating new authn request.", ex);
            throw new FSException(ex);
        }
    }

}
