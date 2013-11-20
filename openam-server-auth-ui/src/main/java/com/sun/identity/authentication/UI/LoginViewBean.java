/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LoginViewBean.java,v 1.28 2009/11/25 11:58:53 manish_rustagi Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 * Portions Copyrighted 2012 Nomura Research Institute, Ltd
 */
package com.sun.identity.authentication.UI;

//import com.iplanet.am.util.AMURLEncDec;
import com.sun.identity.authentication.share.RedirectCallbackHandler;
import com.sun.identity.shared.encode.URLEncDec;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.ImageField;
import com.iplanet.jato.view.html.StaticTextField;
import com.sun.identity.shared.encode.CookieUtils;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.shared.locale.L10NMessageImpl;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is a default implementation of <code>LoginViewBean</code> 
 * auth Login UI.
 */
public class LoginViewBean extends AuthViewBeanBase {
    /**
     * Creates <code>LoginViewBean</code> object.
     */
    public LoginViewBean() {
        super(PAGE_NAME);
        loginDebug.message("LoginViewBean() constructor called");
        registerChildren();
    }
    
    /**
     * register child view
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(PAGE_STATE, StaticTextField.class);
        registerChild(LOGIN_URL, StaticTextField.class);
        registerChild(AM_ORIG_URL, StaticTextField.class);
        registerChild(DEFAULT_LOGIN_URL, StaticTextField.class);
        registerChild(REDIRECT_URL, StaticTextField.class);
        registerChild(TILED_CALLBACKS, CallBackTiledView.class);
        registerChild(TILED_BUTTONS, ButtonTiledView.class);
        registerChild(DEFAULT_BTN, StaticTextField.class);
        registerChild(TXT_GOTO_LOGIN_AFTER_FAIL, StaticTextField.class);
        registerChild(CMD_SUBMIT, StaticTextField.class);
        registerChild(LBL_SUBMIT, StaticTextField.class);
        registerChild(CMD_CONTINUE, StaticTextField.class);
        registerChild(LBL_CONTINUE, StaticTextField.class);
        registerChild(CMD_AGREE, StaticTextField.class);
        registerChild(LBL_AGREE, StaticTextField.class);
        registerChild(CMD_DISAGREE, StaticTextField.class);
        registerChild(LBL_DISAGREE, StaticTextField.class);
        registerChild(CMD_YES, StaticTextField.class);
        registerChild(LBL_YES, StaticTextField.class);
        registerChild(CMD_NO, StaticTextField.class);
        registerChild(LBL_NO, StaticTextField.class);
        registerChild(CMD_NEW_USER, StaticTextField.class);
        registerChild(LBL_NEW_USER, StaticTextField.class);
        registerChild(LBL_RESET, StaticTextField.class);
    }

    /**
     * creates child view/component
     *
     * @param name of view/component
     * @return view/component
     */
    protected View createChild(String name) {
        if (name.equals("StaticTextResult")) {
            return new StaticTextField(this, name, ResultVal);
        } else if (name.equals("StaticTextWarning")) {
            return new StaticTextField(this, name, lockWarning);
        } else if (name.equals("StaticTextMessage")) { // Error message
            return new StaticTextField(this, name, ErrorMessage);
        } else if (name.equals("StaticTextHeader")) {
            return new StaticTextField(this, name, TextHeaderVal);
        } else if (name.equals(REDIRECT_URL)) { // Redirect URL for wireless
            String redirect = redirect_url;
            redirect_url = AuthUtils.encodeURL(redirect, ac, response);
            return new StaticTextField(this, name, redirect_url);
        } else if (name.equals(DEFAULT_LOGIN_URL)) {
            String default_login_url = AuthUtils.encodeURL(LOGINURL, ac, response);
            return new StaticTextField(this, name, default_login_url);
        } else if (name.equals(LOGIN_URL)) { // non-cookie support
            if ((loginURL==null)||(loginURL.length() == 0)) {
                loginURL = LOGINURL;
            }
            loginURL = AuthUtils.encodeURL(loginURL, ac, response);
            return new StaticTextField(this, name, loginURL);
        } else if (name.equals(AM_ORIG_URL)) { //only used by new_org.jsp
            origLoginURL = AuthUtils.constructOrigURL(request);
            return new StaticTextField(this, name, origLoginURL);
        } else if (name.equals(PAGE_STATE)) {
            return new StaticTextField(this, name, pageState);
        } else if (name.equals("Image")) {
            return new ImageField(this, name, pageImage);
        } else if (name.equals(TILED_CALLBACKS)) {
            return new CallBackTiledView(this, TILED_CALLBACKS);
        } else if (name.equals(TILED_BUTTONS)) {
            return new ButtonTiledView(this, TILED_BUTTONS);
        } else if (name.equals(DEFAULT_BTN)) {
            return new StaticTextField(this, DEFAULT_BTN, "");
        } else if (name.equals(TXT_GOTO_LOGIN_AFTER_FAIL)) {
            return new StaticTextField(this, TXT_GOTO_LOGIN_AFTER_FAIL, "");
        } else if (name.equals(CMD_SUBMIT)) {
            return new StaticTextField(this, CMD_SUBMIT, "");
        } else if (name.equals(LBL_SUBMIT)) {
            return new StaticTextField(this, LBL_SUBMIT, "");
        } else if (name.equals(CMD_CONTINUE)) {
            return new StaticTextField(this, CMD_CONTINUE, "");
        } else if (name.equals(LBL_CONTINUE)) {
            return new StaticTextField(this, LBL_CONTINUE, "");
        } else if (name.equals(CMD_AGREE)) {
            return new StaticTextField(this, CMD_AGREE, "");
        } else if (name.equals(LBL_AGREE)) {
            return new StaticTextField(this, LBL_AGREE, "");
        } else if (name.equals(CMD_DISAGREE)) {
            return new StaticTextField(this, CMD_DISAGREE, "");
        } else if (name.equals(LBL_DISAGREE)) {
            return new StaticTextField(this, LBL_DISAGREE, "");
        } else if (name.equals(CMD_YES)) {
            return new StaticTextField(this, CMD_YES, "");
        } else if (name.equals(LBL_YES)) {
            return new StaticTextField(this, LBL_YES, "");
        } else if (name.equals(CMD_NO)) {
            return new StaticTextField(this, CMD_NO, "");
        } else if (name.equals(LBL_NO)) {
            return new StaticTextField(this, LBL_NO, "");
        } else if (name.equals(CMD_NEW_USER)) {
            return new StaticTextField(this, CMD_NEW_USER, "");
        } else if (name.equals(LBL_NEW_USER)) {
            return new StaticTextField(this, LBL_NEW_USER, "");
        } else if (name.equals(LBL_RESET)) {
            return new StaticTextField(this, LBL_RESET, "");
        } else if (name.equals(HTML_TITLE_LOGIN)) {
            return new StaticTextField(this, HTML_TITLE_LOGIN, "");
        } else if (name.equals(HTML_TITLE_MESSAGE)) {
            return new StaticTextField(this, HTML_TITLE_MESSAGE, "");
        } else if (name.equals(HTML_TITLE_REDIRECT)) {
            return new StaticTextField(this, HTML_TITLE_REDIRECT, "");
        } else if (name.equals(HTML_TITLE_ACCOUNTEXPIRED)) {
            return new StaticTextField(this, HTML_TITLE_ACCOUNTEXPIRED, "");
        } else if (name.equals(HTML_TITLE_AUTHERROR)) {
            return new StaticTextField(this, HTML_TITLE_AUTHERROR, "");
        } else if (name.equals(HTML_TITLE_SELFREGERROR)) {
            return new StaticTextField(this, HTML_TITLE_SELFREGERROR, "");
        } else if (name.equals(HTML_TITLE_DISCLAIMER)) {
            return new StaticTextField(this, HTML_TITLE_DISCLAIMER, "");
        } else if (name.equals(HTML_TITLE_INVALIDPCOOKIEUID)) {
            return new StaticTextField(this, HTML_TITLE_INVALIDPCOOKIEUID, "");
        } else if (name.equals(HTML_TITLE_INVALIDPASSWORD)) {
            return new StaticTextField(this, HTML_TITLE_INVALIDPASSWORD, "");
        } else if (name.equals(HTML_TITLE_INVALIDDOMAIN)) {
            return new StaticTextField(this, HTML_TITLE_INVALIDDOMAIN, "");
        } else if (name.equals(HTML_TITLE_USERPROFILENOTFOUND)) {
            return new StaticTextField(
                this, HTML_TITLE_USERPROFILENOTFOUND, "");
        } else if (name.equals(HTML_TITLE_AUTHFAILED)) {
            return new StaticTextField(this, HTML_TITLE_AUTHFAILED, "");
        } else if (name.equals(HTML_TITLE_MEMBERSHIP)) {
            return new StaticTextField(this, HTML_TITLE_MEMBERSHIP, "");
        } else if (name.equals(HTML_TITLE_AUTHMODULEDENIED)) {
            return new StaticTextField(this, HTML_TITLE_AUTHMODULEDENIED, "");
        } else if (name.equals(HTML_TITLE_NOCONFIGERROR)) {
            return new StaticTextField(this, HTML_TITLE_NOCONFIGERROR, "");
        } else if (name.equals(HTML_TITLE_ORGINACTIVE)) {
            return new StaticTextField(this, HTML_TITLE_ORGINACTIVE, "");
        } else if (name.equals(HTML_TITLE_SELFREGMODULE)) {
            return new StaticTextField(this, HTML_TITLE_SELFREGMODULE, "");
        } else if (name.equals(HTML_TITLE_SESSIONTIMEOUT)) {
            return new StaticTextField(this, HTML_TITLE_SESSIONTIMEOUT, "");
        } else if (name.equals(HTML_TITLE_USERNOTFOUND)) {
            return new StaticTextField(this, HTML_TITLE_USERNOTFOUND, "");
        } else if (name.equals(HTML_TITLE_USERINACTIVE)) {
            return new StaticTextField(this, HTML_TITLE_USERINACTIVE, "");
        } else if (name.equals(HTML_TITLE_NEWORG)) {
            return new StaticTextField(this, HTML_TITLE_NEWORG, "");
        } else if (name.equals(HTML_TITLE_MAXSESSIONS)) {
            return new StaticTextField(this, HTML_TITLE_MAXSESSIONS, "");
        } else {
            return super.createChild(name);
        }
    }
    
    /**
     * Forwards the request to this view bean, displaying the page. This
     * method is the equivalent of <code>RequestDispatcher.forward()</code>,
     * meaning that the same semantics apply to the use of this method.
     * This method makes implicit use of the display URL returned
     * by the <code>getDisplayURL()</code> method.
     * @param requestContext servlet context for auth request
     */
    public void forwardTo(RequestContext requestContext) {
        
        loginDebug.message("In forwardTo()");
        SSOToken ssoToken = null;
        if (requestContext!=null) {
            request = requestContext.getRequest();
            response = requestContext.getResponse();
        }
        
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        if (AuthClientUtils.isVersionHeaderEnabled()) {
            response.setHeader("X-DSAMEVersion", AuthClientUtils.getDSAMEVersion());
        }
        
        // get request ( GET ) parameters for 'login' process
        reqDataHash = AuthUtils.parseRequestParameters(request);
        /*if (loginDebug.messageEnabled()) {
            loginDebug.message("request data hash : " + reqDataHash);
        }*/
        
        client_type = AuthUtils.getClientType(request);
        // Set header for Misrouted server's usage
        response.setHeader("AM_CLIENT_TYPE", client_type);
        
        if (loginDebug.messageEnabled()) {
            loginDebug.message("Client Type is: " + client_type);
            loginDebug.message("Request method is : " + request.getMethod());
        }
        if (request.getMethod().equalsIgnoreCase("POST")) {
            isPost = true;
        }
        
        SessionID sessionID = null;
        InternalSession intSession = null;
        try {
            boolean isBackPost = false;
            // if the request is a GET then iPlanetAMDirectoryPro cookie
            // will be used to retrieve the session for session upgrade
            sessionID = AuthUtils.getSessionIDFromRequest(request);
            ssoToken = AuthUtils.getExistingValidSSOToken(sessionID);

            //Check for session Timeout	 
            if((ssoToken == null) && (sessionID != null) &&	 
              (sessionID.toString().length()!= 0)){	 
                    if(AuthUtils.isTimedOut(sessionID)){
                        clearCookie(request);	 
                        errorCode = AMAuthErrorCode.AUTH_TIMEOUT;	 
                        ErrorMessage = AuthUtils.getErrorVal(	 
                              AMAuthErrorCode.AUTH_TIMEOUT,	 
                              AuthUtils.ERROR_MESSAGE);	 
                        errorTemplate = AuthUtils.getErrorVal(	 
                              AMAuthErrorCode.AUTH_TIMEOUT,	 
                              AuthUtils.ERROR_TEMPLATE);	 
	 
                        ISLocaleContext localeContext = new ISLocaleContext();	 
                        localeContext.setLocale(request);	 
                        java.util.Locale locale = localeContext.getLocale();	 
                        rb =  rbCache.getResBundle(bundleName, locale);	 
                        super.forwardTo(requestContext);	 
                        return;	 
                  }	 
            }

            forceAuth = AuthUtils.forceAuthFlagExists(reqDataHash);
            if (ssoToken != null) {
                if (AuthUtils.newSessionArgExists(reqDataHash)) {
                    SSOTokenManager.getInstance().destroyToken(ssoToken);
                } else {
                    loginDebug.message("Old Session is Active.");
                    newOrgExist = checkNewOrg(ssoToken);
                    if (logIntoDiffOrg) {
                        String origURL = request.getParameter(AM_ORIG_URL);
                        if (origURL != null) {
                            String tmp = new String(Base64.decode(origURL));
                            response.sendRedirect(tmp);
                        } else {
                            errorCode = "102";
                            setErrorMessage(null);
                            super.forwardTo(requestContext);
                        }
                        return;
                    }
                    if (!newOrgExist && !dontLogIntoDiffOrg) {
                        if (isPost) {
                            isBackPost = canGetOrigCredentials(ssoToken);
                        }
                        if  (forceAuth) {
                            sessionUpgrade = true;
                        } else {
                            sessionUpgrade = AuthUtils.checkSessionUpgrade(
                                ssoToken, reqDataHash);
                        }
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message(
                                "Session Upgrade = " + sessionUpgrade);
                        }                        
                    }
                }
            }
            
            if ((ssoToken != null) && !sessionUpgrade && !newOrgExist) {
                try {
                    loginDebug.message("Session is Valid / already "
                    + "authenticated");
                    bValidSession = true;
                    /*
                     * redirect to 'goto' parameter or SPI hook or default
                     * redirect URL.
                     */
                    if (request != null) {
	                redirect_url = AuthUtils.getValidGotoURL(request, ssoToken.getProperty("Organization"));
	                if ((redirect_url == null) || (redirect_url.length() 
                            == 0)){
                            redirect_url = ssoToken.getProperty(
                                ISAuthConstants.SUCCESS_URL);
                        }
                    }
                    if (redirect_url == null) {
                        ResultVal = rb.getString
                            ("authentication.already.login");
                    }
                    LoginSuccess = true;
                    boolean doForward = AuthUtils.forwardSuccessExists(request);
                    if (doForward) {  
                        if(loginDebug.messageEnabled()){
                            loginDebug.message(
                                "LoginViewBean.forwardRequest=true");
                            loginDebug.message("LoginViewBean.forwardTo():" +
                            "Forward URL before appending cookie is " + 
                            redirect_url); 
                        }
                        if(loginDebug.messageEnabled()){
                            loginDebug.message("LoginViewBean.forwardTo():" +
                            "Final Forward URL is " + redirect_url); 
                        }
                        
                        RequestDispatcher dispatcher =
                        request.getRequestDispatcher(redirect_url);
                        request.setAttribute(Constants.FORWARD_PARAM,
                            Constants.FORWARD_YES_VALUE);
                        dispatcher.forward(request, response);
                    } else {            
                        if (redirect_url.startsWith(SSO_REDIRECT)) {
                            if (loginDebug.messageEnabled()) {
                                loginDebug.message("LoginViewBean.forwardTo():" +
                                    "Redirect to: " + redirect_url);
                            }
                            response.sendRedirect(serviceUri + redirect_url);
                        } else {
                            response.sendRedirect(redirect_url);
                        }
                    }
                    return;
                }
                catch (Exception er){
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("Session getState exception: ", er);
                    }
                    setErrorMessage(er);
                }
            }

            ac = AuthUtils.getAuthContext(
                    request, response, sessionID, sessionUpgrade, isBackPost);
            if (sessionID != null) {
                intSession = AuthD.getSession(sessionID);
            }
            if ((intSession != null) && (intSession.isTimedOut())) { //Session Timeout
                // clear the cookie only if cookie supported
                loginDebug.message("Session timeout TRUE");
                if (sessionUpgrade) {
                    try {
                        redirect_url = getPrevSuccessURLAndSetCookie();
                        clearGlobals();
                        response.sendRedirect(redirect_url);
                        return;
                    } catch (Exception e) {
                        loginDebug.message("Error redirecting :" ,e);
                    }
                } else {
                    // clear AM Cookie if it exists.
                    if (CookieUtils.getCookieValueFromReq(request,
                    AuthUtils.getCookieName())!=null) {
                        clearCookie(AuthUtils.getCookieName());
                    }
                    // clear Auth Cookie if it exists.
                    if (CookieUtils.getCookieValueFromReq(request,
                    AuthUtils.getAuthCookieName())!=null) {
                        clearCookie(AuthUtils.getAuthCookieName());
                    }
                    loginURL = intSession.getProperty(ISAuthConstants.
                        FULL_LOGIN_URL);
                    errorTemplate = AuthUtils.getErrorVal(
                        AMAuthErrorCode.AUTH_TIMEOUT,AuthUtils.ERROR_TEMPLATE);
                    errorCode = AMAuthErrorCode.AUTH_TIMEOUT;
                    ErrorMessage = AuthUtils.getErrorVal(
		        AMAuthErrorCode.AUTH_TIMEOUT,AuthUtils.ERROR_MESSAGE);
                }
            }
            java.util.Locale locale =
                com.sun.identity.shared.locale.Locale.getLocale(
                    AuthUtils.getLocale(ac));
            fallbackLocale = locale;
            rb =  rbCache.getResBundle(bundleName, locale);
            if (loginDebug.messageEnabled()) {
                loginDebug.message("ac = " + ac);
                loginDebug.message("JSPLocale = " + locale);
            }
            if (sessionUpgrade) {
                ac.getLoginState().setForceAuth(forceAuth);
            }
            if (!AuthUtils.getInetDomainStatus(ac)) {//domain inactive
                if ((errorTemplate==null)||(errorTemplate.length() == 0)) {
                    setErrorMessage(null);
                }
            }
            // check session or new request
            // add cookie only if cookie is supported
            if (!isBackPost) {
                loginURL = AuthUtils.getLoginURL(ac);
            }
            /*if (loginDebug.messageEnabled()) {
                loginDebug.message("loginURL : " + loginURL);
            }*/
            
            // Check whether need to detect the cookie support in the browser
            String cookieless =
                    (String)request.getAttribute("displayCookieError");
            if (cookieless != null && cookieless.equals("true")) {
                ErrorMessage = rb.getString("nocookiesupport");
                errorTemplate = "Message.jsp";
            }

            if (AuthUtils.isNewRequest(ac)) {
                loginDebug.message("New AuthContext created");
                if (AuthUtils.isCookieSupported(ac)) {
		    if (AuthUtils.persistAMCookie(reqDataHash)) {
			enableCookieTimeToLive();
		    }
                    if (!newOrgExist) {
                        setCookie();
                    }
                    setlbCookie();
                }
            } else {
                // check if client still have the cookie we set.
                if (AuthUtils.isCookieSet(ac)) {
                    if (AuthUtils.checkForCookies(request, ac)) {
                        loginDebug.message("Client support cookie");
                        AuthUtils.setCookieSupported(ac, true);
                    } else {
                        loginDebug.message("Client do not support cookie");
                        AuthUtils.setCookieSupported(ac, false);
                    }
                }
            }
        } catch (Exception e) {
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(request);
            fallbackLocale = localeContext.getLocale();
            rb =  rbCache.getResBundle(bundleName, fallbackLocale);
            if (loginDebug.messageEnabled()) {
                loginDebug.message("JSPLocale = " + fallbackLocale);
            }
            setErrorMessage(e);
            jsp_page = errorTemplate;
            if (requestContext==null) {
                return;
            }
            super.forwardTo(requestContext);
            return;
        }
        
        if ((errorTemplate==null)||(errorTemplate.length() == 0)) {
            processLogin();
            if (requestContext==null) { // solve the recursive case
                clearGlobals();
                return;
            }
        }
        
        if ((redirect_url != null) && (redirect_url.length() != 0)) {
            // forward check for liberty federation, if the redirect_url
            // is the federation post login servlet, use forward instead
            boolean doForward = AuthUtils.isForwardSuccess(ac,request);

            if (AuthUtils.isGenericHTMLClient(client_type) || doForward) {
                try {
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("Send Redirect to " + redirect_url);
                    }
                    
                    // destroy session if necessary.
                    InternalSession oldSession = AuthUtils.getOldSession(ac);
                    if (ac.getStatus() == AuthContext.Status.FAILED) {
                        loginDebug.message(
                            "forwardTo(): Auth failed - Destroy Session!");
                        if (AuthUtils.isSessionUpgrade(ac)) {
                            clearCookieAndDestroySession(ac);
                            loginDebug.message(
                                "forwardTo(): Session upgrade - " +
                                "Restoring original Session!");
                            if (oldSession != null) {
                                ac.getLoginState().setSession(oldSession);
                                ac.getLoginState().setSid(oldSession.getID());
                            }
                        } else {
                            clearCookieAndDestroySession(ac);
                            if (oldSession != null) {
                                loginDebug.message(
                                    "Destroy existing/old valid session");
                                AuthD authD = AuthD.getAuth();
                                authD.destroySession(oldSession.getID());
                            }
                        }
                        loginDebug.message(
                            "Login failure, current session destroyed!");
                    } else if (ac.getStatus()==AuthContext.Status.SUCCESS) {
                        response.setHeader("X-AuthErrorCode", "0");
                        if (ac.getLoginState().getForceFlag()) {
                            if (loginDebug.messageEnabled()) {
                                loginDebug.message("Forced Auth Succeed."
                                    + "Restoring updated session");
                            }
                            clearCookieAndDestroySession(ac);
                            ac.getLoginState().setSession(oldSession);
                            ac.getLoginState().setSid(oldSession.getID());
                        } else {
                            if (AuthUtils.isCookieSupported(ac)) {
                                setCookie();
                                clearCookie(AuthUtils.getAuthCookieName());
                            }
                            if (SystemProperties.getAsBoolean(Constants.DESTROY_SESSION_AFTER_UPGRADE) &&
                                    oldSession != null) {
                                loginDebug.message(
                                    "Destroy existing/old valid session");
                                AuthD authD = AuthD.getAuth();
                                authD.destroySession(oldSession.getID());
                            }
                        }
                    }
                    
                    Cookie appendCookie = AuthUtils.getCookieString(ac, null);
                    clearGlobals();
                    if (doForward) {
                        loginDebug.message("LoginViewBean.forwardRequest=true");
                        if(loginDebug.messageEnabled()){
                            loginDebug.message("LoginViewBean.forwardTo():" +
                            "Forward URL before appending cookie is " + 
                            redirect_url); 
                        }
                        //since this is a request FORWARD, we MUST add the session id to the URL, otherwise federation
                        //would not have any knowledge about the freshly created session ID - this can be especially
                        //a problem, when upgrading session: old session ID cookie is still present in the request
                        //but the new isn't.
                        if(redirect_url.indexOf("?") == -1){
                            redirect_url = redirect_url + "?" + 
                            appendCookie.getName() + "=" + 
                            URLEncDec.encode(appendCookie.getValue());
                        }else{
                            redirect_url = redirect_url + "&" + 
                            appendCookie.getName() + "=" + 
                            URLEncDec.encode(appendCookie.getValue());
                        }
                        if(loginDebug.messageEnabled()){
                            loginDebug.message("LoginViewBean.forwardTo():" +
                            "Final Forward URL is " + redirect_url); 
                        }

                        RequestDispatcher dispatcher =
                        request.getRequestDispatcher(redirect_url);
                        request.setAttribute(Constants.FORWARD_PARAM,
                            Constants.FORWARD_YES_VALUE);
                        dispatcher.forward(request, response);
                    } else {
                        if (redirect_url.startsWith(SSO_REDIRECT)) {
                            if (loginDebug.messageEnabled()) {
                                loginDebug.message("LoginViewBean.forwardTo():" +
                                    "Redirect to: " + redirect_url);
                            }
                            response.sendRedirect(serviceUri + redirect_url);
                        } else {
                            response.sendRedirect(redirect_url);
                        }
                    }
                    forward = false;
                    
                    return;
                } catch (Exception e) { // Servlet redirect error.
                    ResultVal = rb.getString("redirect.error");
                }
            }
        }
        if (forward) {
            forward = false;
            super.forwardTo(requestContext);
        }
        clearGlobals();
    }
    
    /**
     * Returns display url for auth auth Login UI
     * 
     * @return display url for auth auth Login  UI
     */
    public String getDisplayURL() {
        loginDebug.message("In getDisplayURL()");
        
        // redirect url gets a higher priority
        // if URLRedirection class is implemented
        // and customers want to use login failed url
        if ((redirect_url != null) && (redirect_url.length() != 0)) {
            jsp_page = "Redirect.jsp";
        } else if ((errorTemplate != null) && (errorTemplate.length() != 0)) {
            jsp_page = errorTemplate;
        } else if ((ErrorMessage != null) && (ErrorMessage.length() != 0)) {
            jsp_page = "Message.jsp";
        } else if ((pageTemplate != null) && (pageTemplate.length() != 0)) {
            if (loginDebug.messageEnabled()) {
                loginDebug.message("Using module Template : " + pageTemplate);
            }
            jsp_page =  pageTemplate;
        } else {
            jsp_page =  "Login.jsp";
        }
        
        jsp_page = getFileName(jsp_page);
        
        if (ac != null) {
            InternalSession oldSession = AuthUtils.getOldSession(ac);
            if (loginDebug.messageEnabled()) {
                loginDebug.message("Previous Session : " + oldSession);
            }
            if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                response.setHeader("X-AuthErrorCode", "0");
                if (ac.getLoginState().getForceFlag()) {
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("Forced Auth Succeed. "
                            + "Restoring updated session");
                    }
                    clearCookieAndDestroySession(ac);
                    if (oldSession != null) {
                        ac.getLoginState().setSession(oldSession);
                        ac.getLoginState().setSid(oldSession.getID());
                    }
                } else {
                    if (AuthUtils.isCookieSupported(ac)) {
                        setCookie();
                        clearCookie(AuthUtils.getAuthCookieName());
                    }
                    try {
                        if (oldSession != null) {
                            if (loginDebug.messageEnabled()) {
                                loginDebug.message("Destroy the " +
                                "original session Successful!");
                            }
                            AuthD authD = AuthD.getAuth();
                            authD.destroySession(oldSession.getID());
                        }
                    } catch (Exception e) {
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message("Destroy " +
                            "original session Failed! " + e.getMessage());
                        }
                    }
                }
            } else if (ac.getStatus() == AuthContext.Status.FAILED) {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("Destroy Session! for ac : " + ac);
                }
                if (AuthUtils.isSessionUpgrade(ac)) {
                    // clear cookie ,destroy failed session
                    clearCookieAndDestroySession(ac);
                    loginDebug.message(
                        "Session upgrade - Restoring original Session!");
                    if (oldSession != null) {
                        ac.getLoginState().setSession(oldSession);
                        ac.getLoginState().setSid(oldSession.getID());
                    }
                    loginDebug.message("Original session restored successful!");
                } else {
                    // clear cookie ,destroy failed session
                    clearCookieAndDestroySession(ac);
                    if (oldSession != null) {
                        loginDebug.message(
                            "Destroy existing/old valid session");
                        AuthD authD = AuthD.getAuth();
                        authD.destroySession(oldSession.getID());
                    }
                }
                loginDebug.message("Login failure, current session destroyed!");
            }
        }
        
        return AuthUtils.encodeURL(jsp_page,ac,response);
    }
    
    
    /**
     * Called as notification that the JSP has begun its display 
     * processing. In addition to performing the default behavior in the 
     * superclass's version, this method executes any auto-retrieving or auto-
     * executing models associated with this view unless auto-retrieval is
     * disabled.
     *
     * @param event Display Event.
     * @throws ModelControlException if manipulation of a model fails during
     *         display preparation or execution of auto-retrieving models.
     */
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        setPageEncoding(request,response);
        CallBackTiledView tView = (CallBackTiledView) getChild(TILED_CALLBACKS);
        tView.setCallBackArray(callbacks, requiredList, infoText);
        
        if ( rb != null ) {
            if (newOrg) {
                buttonOptions = new String[2];
                buttonOptions[0] = rb.getString("Yes");
                buttonOptions[1] = rb.getString("No");
            }
            
            setDisplayFieldValue(TXT_GOTO_LOGIN_AFTER_FAIL,
            rb.getString("gotoLoginAfterFail"));
            setDisplayFieldValue(CMD_SUBMIT, "Submit");
            
            if ((reqDataHash.get("authlevel") != null) ||
            (reqDataHash.get(Constants.COMPOSITE_ADVICE) != null)) {
                setDisplayFieldValue(LBL_SUBMIT, rb.getString("Submit"));
            } else {
                setDisplayFieldValue(LBL_SUBMIT, rb.getString("LogIn"));
            }
            setDisplayFieldValue(CMD_CONTINUE, "Continue");
            setDisplayFieldValue(LBL_CONTINUE, rb.getString("Continue"));
            setDisplayFieldValue(CMD_AGREE, "Agree");
            setDisplayFieldValue(LBL_AGREE, rb.getString("Agree"));
            setDisplayFieldValue(CMD_DISAGREE, "Disagree");
            setDisplayFieldValue(LBL_DISAGREE, rb.getString("Disagree"));
            setDisplayFieldValue(CMD_YES, "Yes");
            setDisplayFieldValue(LBL_YES, rb.getString("Yes"));
            setDisplayFieldValue(CMD_NO, "No");
            setDisplayFieldValue(LBL_NO, rb.getString("No"));
            setDisplayFieldValue(CMD_NEW_USER, "New User");
            setDisplayFieldValue(LBL_NEW_USER, rb.getString("NewUser"));
            setDisplayFieldValue(LBL_RESET, rb.getString("Reset"));
            setDisplayFieldValue(HTML_TITLE_LOGIN,
                rb.getString("htmlTitle_Login"));
            setDisplayFieldValue(HTML_TITLE_MESSAGE,
                rb.getString("htmlTitle_Message"));
            setDisplayFieldValue(HTML_TITLE_REDIRECT,
                rb.getString("htmlTitle_Redirect"));
            setDisplayFieldValue(HTML_TITLE_ACCOUNTEXPIRED,
                rb.getString("htmlTitle_AccountExpired"));
            setDisplayFieldValue(HTML_TITLE_AUTHERROR,
                rb.getString("htmlTitle_AuthError"));
            setDisplayFieldValue(HTML_TITLE_SELFREGERROR,
                rb.getString("htmlTitle_SelfRegError"));
            setDisplayFieldValue(HTML_TITLE_DISCLAIMER,
                rb.getString("htmlTitle_Disclaimer"));
            setDisplayFieldValue(HTML_TITLE_INVALIDPCOOKIEUID,
                rb.getString("htmlTitle_InvalidPCookieUID"));
            setDisplayFieldValue(HTML_TITLE_INVALIDPASSWORD,
                rb.getString("htmlTitle_InvalidPassword"));
            setDisplayFieldValue(HTML_TITLE_INVALIDDOMAIN,
                rb.getString("htmlTitle_InvalidDomain"));
            setDisplayFieldValue(HTML_TITLE_USERPROFILENOTFOUND,
                rb.getString("htmlTitle_UserProfileNotFound"));
            setDisplayFieldValue(HTML_TITLE_AUTHFAILED,
                rb.getString("htmlTitle_AuthFailed"));
            setDisplayFieldValue(HTML_TITLE_MEMBERSHIP,
                rb.getString("htmlTitle_Membership"));
            setDisplayFieldValue(HTML_TITLE_AUTHMODULEDENIED,
                rb.getString("htmlTitle_AuthModuleDenied"));
            setDisplayFieldValue(HTML_TITLE_NOCONFIGERROR,
                rb.getString("htmlTitle_NoConfigError"));
            setDisplayFieldValue(HTML_TITLE_ORGINACTIVE,
                rb.getString("htmlTitle_OrgInactive"));
            setDisplayFieldValue(HTML_TITLE_SELFREGMODULE,
                rb.getString("htmlTitle_SelfRegModule"));
            setDisplayFieldValue(HTML_TITLE_SESSIONTIMEOUT,
                rb.getString("htmlTitle_SessionTimeOut"));
            setDisplayFieldValue(HTML_TITLE_USERNOTFOUND,
                rb.getString("htmlTitle_UserNotFound"));
            setDisplayFieldValue(HTML_TITLE_USERINACTIVE,
                rb.getString("htmlTitle_UserInactive"));
            setDisplayFieldValue(HTML_TITLE_NEWORG,
                rb.getString("htmlTitle_NewOrg"));
            setDisplayFieldValue(HTML_TITLE_MAXSESSIONS,
                rb.getString("htmlTitle_MaxSessions"));
        } else {
            loginDebug.message("In beginDisplay ... rb is NULL");
            /*
             * setDisplayFieldValue(TXT_GOTO_LOGIN_AFTER_FAIL,
             *  "Try again. Go To Login");
             */
        }
        
        ButtonTiledView tBtnView = (ButtonTiledView) getChild(TILED_BUTTONS);
        tBtnView.setButtonArray(buttonOptions);
        
        if ((buttonOptions != null) && (buttonOptions.length > 0)) {
            setDisplayFieldValue(DEFAULT_BTN, buttonOptions[0]);
        }
        
        //Set Redirect URL for MAP usage
        setDisplayFieldValue(REDIRECT_URL, redirect_url);
    }
    
    private void processLogin() {
        loginDebug.message("In processLogin()");
        if (isPost) {
            try {
                processLoginDisplay();
            }
            catch (Exception ep){
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("processLoginDisplay exception: ", ep);
                }
                setErrorMessage(ep);
            }
        }
        else {
            try {
                getLoginDisplay();
            }
            catch (Exception eg){
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("getLoginDisplay exception: ", eg);
                }
                setErrorMessage(eg);
            }
        }
    }
    
    /**
     * Handles button login request
     * @param event request invocation event
     */
    public void handleButtonLoginRequest(RequestInvocationEvent event) {
        forwardTo();
    }
    
    /**
     * Handles href login request
     * @param event request invocation event.
     */
    public void handleHrefRequest(RequestInvocationEvent event) {
        forwardTo();
    }
    
    protected void getLoginDisplay() throws Exception {
        loginDebug.message("In getLoginDisplay()");
        
        if (!bAuthLevel) {
            prepareLoginParams();
        }
        
        // if pCookie exists and valid and not a session upgrade
        // case return.
        if (!AuthUtils.newSessionArgExists(reqDataHash) && isPersistentCookieValid()
        && (!AuthUtils.isSessionUpgrade(ac)))  {
            return;
        }
        
        if (loginDebug.messageEnabled()) {
            loginDebug.message("Login Parameters : IndexType = " + 
                indexType + " IndexName = " + indexName);
        }
        
        try {
            if ( indexType != null ) {
                if (indexType.equals(AuthContext.IndexType.RESOURCE)) {
                    ac.login(indexType, indexName, false, envMap, null);
                } else {
                    ac.login(indexType, indexName);
                }
            } else {
                ac.login();
            }
        } catch (AuthLoginException le) {
            loginDebug.message("AuthContext()::login error ", le);
            if ((ac.getStatus() == AuthContext.Status.RESET) ||
            (ac.getStatus() == AuthContext.Status.ORG_MISMATCH)) {
                loginDebug.message(
                    "getLoginDisplay(): Destroying current session!");
                InternalSession oldSession = AuthUtils.
                            getOldSession(ac);
                if (AuthUtils.isSessionUpgrade(ac)) {
                    clearCookieAndDestroySession(ac);
                    loginDebug.message("getLoginDisplay(): Session upgrade - " +
                    " Restoring original Session!");
                    if (oldSession != null) {
                        ac.getLoginState().setSession(oldSession);
                        ac.getLoginState().setSid(oldSession.getID());
                        String redirect_url = AuthUtils.getSuccessURL(request,ac);
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message(
                                "Session Upgrade - redirect_url : "
                                + redirect_url);
                        }
                        response.sendRedirect(redirect_url);
                    }
                    forward=false;
                } else {
                    clearCookieAndDestroySession(ac);
                    if (oldSession != null) {
                        loginDebug.message(
                            "Destroy existing/old valid session");
                        AuthD authD = AuthD.getAuth();
                           	authD.destroySession(oldSession.getID());
                    }
                    ac = null;
                    handleAuthLoginException(le);
                }
            } else {
                handleAuthLoginException(le);
            }
            return;
        }
        
        try {
            
            // Get the information requested by the respective auth module
            if (ac.hasMoreRequirements()) {
                loginDebug.message("In getLoginDisplay, has More Requirements");
                callbacks = ac.getRequirements();
                for (int i = 0; i < callbacks.length; i++) {
                    if (callbacks[i] instanceof HttpCallback) {
                        processHttpCallback((HttpCallback)callbacks[i]);
                        return;
                    } else if (callbacks[i] instanceof RedirectCallback) {
                        processRedirectCallback((RedirectCallback)callbacks[i]);
                        return;
                    } else if (!bAuthLevel && !newOrgExist) {
                        // Auth Level login will never do one page login.
                        if (callbacks[i] instanceof NameCallback) {
                            if (reqDataHash.get(TOKEN
                            + Integer.toString(i))!=null) {
                                onePageLogin = true;
                                break;
                            } else if (reqDataHash.get(TOKEN_OLD
                            + Integer.toString(i))!=null) {
                                onePageLogin = true;
                                break;
                            }
                        } else if (callbacks[i] instanceof PasswordCallback) {
                            if (reqDataHash.get(TOKEN
                            + Integer.toString(i))!=null) {
                                onePageLogin = true;
                                break;
                            } else if (reqDataHash.get(TOKEN_OLD
                            + Integer.toString(i))!=null) {
                                onePageLogin = true;
                                break;
                            }
                        } else if (callbacks[i] instanceof ChoiceCallback) {
                            if (reqDataHash.get(TOKEN
                            + Integer.toString(i))!=null) {
                                onePageLogin = true;
                                break;
                            } else if (reqDataHash.get(TOKEN_OLD
                            + Integer.toString(i))!=null) {
                                onePageLogin = true;
                                break;
                            }
                        } else if(callbacks[i] instanceof ConfirmationCallback){
                            if (reqDataHash.get(BUTTON)!=null) {
                                onePageLogin = true;
                                break;
                            } else if (reqDataHash.get(BUTTON_OLD)!=null) {
                                onePageLogin = true;
                                break;
                            }
                        }
                    }
                }
                
                if (onePageLogin && (isPost || AuthUtils.isZeroPageLoginEnabled(ac))) {
                    // user input login info in URL
                    loginDebug.message("User input login information in URL!");
                    processLoginDisplay();
                } else {
                    addLoginCallbackMessage(callbacks);
                    AuthUtils.setCallbacksPerState(ac, pageState, callbacks);
                }
            } else {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message(
                        "No more Requirements in getLoginDisplay");
                    loginDebug.message("Status is : " + ac.getStatus());
                }
                if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                    LoginSuccess = true;
                    ResultVal = rb.getString("authentication.successful");
                    
                    if (AuthUtils.getPersistentCookieMode(ac) &&
                                                // create new persistent cookie
                        AuthUtils.isPersistentCookieOn(ac) &&
                        AuthUtils.isCookieSupported(ac)
                    ) {
                        addPersistentCookie();
                    }
                    
                    /*
                     * redirect to 'goto' parameter or SPI hook or default
                     * redirect URL.
                     */
                    redirect_url = AuthUtils.getLoginSuccessURL(ac);
                    if ((redirect_url != null) && (redirect_url.length() != 0)){
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message(
                                "LoginSuccessURL in getLoginDisplay " +
                                "(in case of successful auth) : " +
                                redirect_url);
                        }
                    }
                } else if (ac.getStatus() == AuthContext.Status.FAILED) {
                    handleAuthLoginException(null);
                    
                    /*
                     * redirect to 'goto' parameter or SPI hook or default
                     * redirect URL.
                     */
                    redirect_url = AuthUtils.getLoginFailedURL(ac);
                    if ((redirect_url != null) && (redirect_url.length() != 0)){
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message(
                                "LoginFailedURL in getLoginDisplay : "
                                + redirect_url);
                        }
                    }
                } else {
                    /*
                     * redirect to 'goto' parameter or SPI hook or default
                     * redirect URL.
                     */
                    redirect_url = AuthUtils.getLoginFailedURL(ac);
                    if (loginDebug.warningEnabled()) {
                        loginDebug.warning("Login Status is " + ac.getStatus() +
                        " - redirect to loginFailedURL : " + redirect_url);
                    }
                    setErrorMessage(null);
                }
            }
        } catch(Exception e){
            setErrorMessage(e);
            throw new L10NMessageImpl(bundleName, "loginDisplay.get",
            new Object[]{e.getMessage()});
        }
    }

    // Process 'HttpCallback' initiated by Authentication module
    private void processHttpCallback(HttpCallback hc) throws Exception{
        String auth = request.getHeader(hc.getAuthorizationHeader());
        if (auth != null && auth.length() != 0) {
            loginDebug.message("Found authorization header.");
            onePageLogin = true;
            processLoginDisplay();
        } else {
            if (loginDebug.messageEnabled()){
                loginDebug.message("Start authorization negotiation...");
                loginDebug.message(
                "header: " + hc.getNegotiationHeaderName() +
                ", value: " + hc.getNegotiationHeaderValue() +
                ", code: " + hc.getNegotiationCode());
            }
            
            forward = false;
            response.setHeader(hc.getNegotiationHeaderName(),
            hc.getNegotiationHeaderValue());
            response.sendError(hc.getNegotiationCode());
        }
    }

    // Process 'RedirectCallback' initiated by Authentication module
    private void processRedirectCallback(RedirectCallback rc) throws Exception {                
        String status = request.getParameter(rc.getStatusParameter()); 
        clearCookie(rc.getRedirectBackUrlCookieName());
        if (status != null && status.length() != 0) {
            loginDebug.message("Found Status parameter."); 
            rc.setStatus(status);
            onePageLogin = true;
            processLoginDisplay();
        } else {
            forward = false;
            redirectCallbackHandler.handleRedirectCallback(request, response, rc, loginURL);
        }
    } 
    
    protected void processLoginDisplay() throws Exception {
        loginDebug.message("In processLoginDisplay()");
        String tmp="";
        
        try {
            
            if (!onePageLogin){
                if (AuthUtils.isNewRequest(ac)) {
                    loginDebug.message(
                        "In processLoginDisplay() : Session New ");
                    getLoginDisplay();
                    return;
                }
            }
            
            String page_state = request.getParameter("page_state");
            if (loginDebug.messageEnabled()) {
                loginDebug.message("Submit with Page State : " + page_state);
            }
            
            if ((page_state != null) && (page_state.length() != 0)) {
                callbacks = AuthUtils.getCallbacksPerState(ac, page_state);

                if(callbacks == null) {
                	errorCode = AMAuthErrorCode.AUTH_TIMEOUT;
                	ErrorMessage = AuthUtils.getErrorVal(
                              AMAuthErrorCode.AUTH_TIMEOUT,
                              AuthUtils.ERROR_MESSAGE);
                	errorTemplate = AuthUtils.getErrorVal(
                              AMAuthErrorCode.AUTH_TIMEOUT,
                              AuthUtils.ERROR_TEMPLATE);
                	return;
                }            
                
                //Get Callbacks in order to set the page state
                Callback[] callbacksForPageState = AuthUtils.getRecdCallback(ac);
                for (int i = 0; i < callbacksForPageState.length; i++) {
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message(
                            "In processLoginDisplay() callbacksForPageState : "
                            + callbacksForPageState[i]);
                    }
                    if (callbacksForPageState[i] instanceof
                        PagePropertiesCallback
                    ) {
                        PagePropertiesCallback ppc =
                        (PagePropertiesCallback) callbacksForPageState[i];
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message(
                                "setPageState in PPC to : " + page_state);
                        }
                        ppc.setPageState(page_state);
                        break;
                    }
                }
            } else {
                callbacks = AuthUtils.getRecdCallback(ac);
            }
            
            indexType = AuthUtils.getIndexType(ac);
            
            // Assign user specified values
            for (int i = 0; i < callbacks.length; i++) {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("In processLoginDisplay() callback : "
                    + callbacks[i]);
                }
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    tmp = (String)reqDataHash.get(TOKEN
                    + Integer.toString(i));
                    if (tmp == null) {
                        tmp = (String)reqDataHash.get(TOKEN_OLD
                        + Integer.toString(i));
                    }
                    if ((bAuthLevel) || (tmp==null)) {
                        tmp = "";
                    }
                    nc.setName(tmp.trim());
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    tmp = (String)reqDataHash.get(TOKEN
                    + Integer.toString(i));
                    if (tmp == null) {
                        tmp = (String)reqDataHash.get(TOKEN_OLD
                        + Integer.toString(i));
                    }
                    if (tmp==null) {
                        tmp = "";
                    }
                    pc.setPassword(tmp.toCharArray());
                } else if (callbacks[i] instanceof ChoiceCallback) {
                    ChoiceCallback cc = (ChoiceCallback) callbacks[i];
                    choice = (String)reqDataHash.get(TOKEN
                    + Integer.toString(i));
                    if (choice == null) {
                        choice = (String)reqDataHash.get(TOKEN_OLD
                        + Integer.toString(i));
                    }
                    if (choice==null) {
                        choice = "0";
                    }
                    
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("choice : " + choice);
                    }
                    
                    String[] choices = cc.getChoices();
                    int selected = 0;
                    
                    if (choice.indexOf("|") != -1) {
                        StringTokenizer st = new StringTokenizer(choice, "|");
                        int cnt = st.countTokens();
                        int[] selectIndexs = new int[cnt];
                        int j = 0;
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message(
                                "No of tokens : " + Integer.toString(cnt));
                        }
                        while (st.hasMoreTokens()) {
                            choice = st.nextToken();
                            if ((choice!=null) && (choice.length() != 0)) {
                                selected = Integer.parseInt(choice);
                                choice = choices[selected];
                                selectIndexs[j] = selected;
                                j++;
                                if (loginDebug.messageEnabled()) {
                                    loginDebug.message(
                                        "selected  choice : " + choice
                                        + " & selected index : " + selected);
                                }
                            }
                        }
                        cc.setSelectedIndexes(selectIndexs);
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message(
                                "Selected indexes : " + selectIndexs);
                        }
                    } else {
                        selected = Integer.parseInt(choice);
                        cc.setSelectedIndex(selected);
                        choice = choices[selected];
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message("selected ONE choice : " + choice
                            + " & selected ONE index : " + selected);
                        }
                    }
                } else if (callbacks[i] instanceof ConfirmationCallback) {
                    ConfirmationCallback conc =
                        (ConfirmationCallback)callbacks[i];
                    buttonOptions = conc.getOptions();
                    tmp = (String)reqDataHash.get(BUTTON);
                    if (tmp == null) {
                        tmp = (String)reqDataHash.get(BUTTON_OLD);
                    }
                    if (tmp == null) {
                        tmp = "";
                    }
                    
                    int selectedIndex = 0;
                    for (int j=0; j<buttonOptions.length; j++) {
                        if ( (buttonOptions[j].trim()).equals(tmp.trim()) ) {
                            selectedIndex = j;
                        }
                    }
                    conc.setSelectedIndex(selectedIndex);
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("selected  button : "
                        + buttonOptions[selectedIndex]
                        + " & selected button index : " + selectedIndex);
                    }
                } else if (callbacks[i] instanceof PagePropertiesCallback) {
                    PagePropertiesCallback ppc = (PagePropertiesCallback) callbacks[i];
                } else if (callbacks[i] instanceof RedirectCallback) {
                    RedirectCallback rc = (RedirectCallback) callbacks[i];
                    String status = 
                        request.getParameter(rc.getStatusParameter()); 
                    clearCookie(rc.getRedirectBackUrlCookieName());
                    loginDebug.message("Redirect callback : set status");                        
                    rc.setStatus(status);
                }
            }
            
            // testing
            if (loginDebug.messageEnabled()) {
                loginDebug.message(" length 0f callbacks : " +callbacks.length);
                loginDebug.message(" Index type : " + indexType
                + " Index name : " + indexName);
            }
            //testing
            
            if ((indexType == AuthContext.IndexType.LEVEL) ||
            (indexType == AuthContext.IndexType.COMPOSITE_ADVICE)) {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("In processLoginDisplay(), Index type" +
                    " is Auth Level or Composite Advice and selected Module " + 
                    "or Service is : " + choice);
                }
                indexName = AMAuthUtils.getDataFromRealmQualifiedData(choice);
                String qualifiedRealm = 
                    AMAuthUtils.getRealmFromRealmQualifiedData(choice);
                String orgDN = null;
                if ((qualifiedRealm != null) && (qualifiedRealm.length() != 0)) {
                    orgDN = DNMapper.orgNameToDN(qualifiedRealm);
                    ac.setOrgDN(orgDN);
                }

                int type = AuthUtils.getCompositeAdviceType(ac);
                
                if (type == AuthUtils.MODULE) {
                    indexType = AuthContext.IndexType.MODULE_INSTANCE;
                } else if (type == AuthUtils.SERVICE) {
                    indexType = AuthContext.IndexType.SERVICE;
                } else if (type == AuthUtils.REALM) {
                    indexType = AuthContext.IndexType.SERVICE;
                    orgDN = DNMapper.orgNameToDN(choice);
                    indexName = AuthUtils.getOrgConfiguredAuthenticationChain(orgDN);
                    ac.setOrgDN(orgDN);
                } else {
                    indexType = AuthContext.IndexType.MODULE_INSTANCE;
                }
                
                bAuthLevel = true;
                
                if ((indexName != null) && 
                    (indexType == AuthContext.IndexType.MODULE_INSTANCE)){
                    if (indexName.equalsIgnoreCase("Application")) {
                        onePageLogin = true;
                    }
                }
                
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("Index type : " + indexType);
                    loginDebug.message("Index name : " + indexName);
                    loginDebug.message("qualified orgDN : " + orgDN);
                }
                
                getLoginDisplay();
            } else {
                // Submit the information to auth module
                ac.submitRequirements(callbacks);
                
                // Check if more information is required
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("before hasMoreRequirements: Status is: "
                    + ac.getStatus());
                }
                if (ac.hasMoreRequirements()) {
                    loginDebug.message("Has more requirements after Submit ");
                    callbacks = ac.getRequirements();
                    
                    for (int i = 0; i < callbacks.length; i++) {
                        if (callbacks[i] instanceof HttpCallback) {
                            processHttpCallback((HttpCallback)callbacks[i]);
                            return;
                        } else if (callbacks[i] instanceof RedirectCallback) {
                            processRedirectCallback(
                                (RedirectCallback)callbacks[i]);
                            return;
                        }
                    }
                    
                    addLoginCallbackMessage(callbacks);
                    AuthUtils.setCallbacksPerState(ac, pageState, callbacks);
                } else {
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message(
                            "No more Requirements : Status is : "
                                + ac.getStatus());
                    }
                    if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                        LoginSuccess = true;
                        ResultVal = rb.getString("authentication.successful");
                        
                        // set persistant cookie
                        if (AuthUtils.isCookieSupported(ac) &&
                            AuthUtils.isPersistentCookieOn(ac) &&
                                                    //iPSPCookie value in URL
                            AuthUtils.getPersistentCookieMode(ac)
                                        //persistent cookie setting in profile
                        ) {
                            addPersistentCookie();
                        }
                        
                        /*
                         * redirect to 'goto' parameter or SPI hook or default
                         * redirect URL.
                         */
                        redirect_url = AuthUtils.getLoginSuccessURL(ac);
                        if ((redirect_url != null) &&
                            (redirect_url.length() != 0)
                        ) {
                            if (loginDebug.messageEnabled()) {
                                loginDebug.message(
                                    "LoginSuccessURL (in case of " +
                                    " successful auth) : " + redirect_url);
                            }
                        }
                    } else if (ac.getStatus() == AuthContext.Status.FAILED) {
                        handleAuthLoginException(null);
                        
                        /*
                         * redirect to 'goto' parameter or SPI hook or default
                         * redirect URL.
                         */
                        redirect_url = AuthUtils.getLoginFailedURL(ac);
                        if ((redirect_url != null) &&
                            (redirect_url.length() != 0)
                        ) {
                            if (loginDebug.messageEnabled()) {
                                loginDebug.message("LoginFailedURL : "
                                + redirect_url);
                            }
                        }
                    } else {
                        /*
                         * redirect to 'goto' parameter or SPI hook or default
                         * redirect URL.
                         */
                        redirect_url = AuthUtils.getLoginFailedURL(ac);
                        if (loginDebug.warningEnabled()) {
                            loginDebug.warning(
                                "Login Status is " + ac.getStatus() +
                                " - redirect to loginFailedURL : " +
                                redirect_url);
                        }
                        setErrorMessage(null);
                    }
                }
            }
        } catch (Exception e) {
            if (loginDebug.messageEnabled()) {
                loginDebug.message("Error in processing LoginDisplay : ", e);
            }
            setErrorMessage(e);
            throw new L10NMessageImpl(bundleName, "loginDisplay.process",
            new Object[]{e.getMessage()});
        }
    }
    
    
    // Method to generate HTML page from Callback objects
    protected void addLoginCallbackMessage(Callback[] callbacks)
    throws Exception {
        loginDebug.message("In addLoginCallbackMessage()");
        buttonOptions = null;
        pageState = null;
        
        for (int i = 0; i < callbacks.length; i++) {
            if (loginDebug.messageEnabled()) {
                loginDebug.message("In addLoginCallbackMessage() callback : "
                + callbacks[i]);
            }
            if (callbacks[i] instanceof ConfirmationCallback) {
                ConfirmationCallback conc = (ConfirmationCallback) callbacks[i];
                buttonOptions = conc.getOptions();
                defaultButtonIndex = conc.getDefaultOption();
                String defaultButton = buttonOptions[defaultButtonIndex];
                
            } else if (callbacks[i] instanceof PagePropertiesCallback) {
                PagePropertiesCallback ppc =
                    (PagePropertiesCallback)callbacks[i];
                TextHeaderVal = ppc.getHeader();
                pageTemplate = ppc.getTemplateName();
                pageImage = ppc.getImage();
                requiredList = ppc.getRequire();
                pageState = ppc.getPageState();
                infoText = ppc.getInfoText();
                
                int lsize = 0;
                
                if ((requiredList != null) && (!requiredList.isEmpty())) {
                    loginDebug.message("PPC - list not null & not empty");
                    lsize = requiredList.size();
                }
                
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("PagePropertiesCallback - header : "
                        + TextHeaderVal + " template : " + pageTemplate
                        + " image : " + pageImage + " Required list : "
                        + requiredList + " List size : " + lsize
                        + "Info Text : " + infoText 
                        + " Page State : " + pageState);
                }

                // empty callback processing
                if (callbacks.length == 1) {
                    onePageLogin = true;
                    processLoginDisplay();
                    break;
                }
            }
        }
    }
    
    
    /**
     * Returns tile Index.
     *
     * @return Tile Index;
     */
    public String getTileIndex() {
        CallBackTiledView tView = (CallBackTiledView) getChild(TILED_CALLBACKS);
        return (String)tView.getDisplayFieldValue(CallBackTiledView.TXT_INDEX);
    }
    
    // Method to prepare 'login' method parameters from request object data
    protected void prepareLoginParams() {
        loginDebug.message("begin prepareLoginParams");
        String reqModule = (String) reqDataHash.get("module");
        if ( reqDataHash.get("user") != null ) {
            indexType = AuthContext.IndexType.USER;
            indexName = (String)reqDataHash.get("user");
        } else if ( reqDataHash.get("role") != null ) {
            indexType = AuthContext.IndexType.ROLE;
            indexName = (String)reqDataHash.get("role");
	} else if ( reqDataHash.get("service") != null &&
		    reqDataHash.get(Constants.COMPOSITE_ADVICE) == null) {
            indexType = AuthContext.IndexType.SERVICE;
            indexName = (String)reqDataHash.get("service");
        } else if ( (reqModule != null) && 
            (reqModule.length() != 0) && 
            (!reqModule.equalsIgnoreCase("null")) ) {
            indexType = AuthContext.IndexType.MODULE_INSTANCE;
            String encoded = (String) reqDataHash.get("encoded");
            String new_org = (String) reqDataHash.get("new_org");
            if ((new_org != null && new_org.equals("true")) &&
                (encoded != null && encoded.equals("true"))){
                indexName = AuthUtils.getBase64DecodedValue(reqModule);
            } else {
                indexName = reqModule;
            }
            // Application auth is always 0 page login
            if ( indexName != null ) {
                if (indexName.equalsIgnoreCase("Application")) {
                    onePageLogin = true;
                }
            }
        } else if ( reqDataHash.get("authlevel") != null ) {
            indexType = AuthContext.IndexType.LEVEL;
            indexName = (String)reqDataHash.get("authlevel");
            
        } else if ( reqDataHash.get(Constants.COMPOSITE_ADVICE) != null ) {
            indexType = AuthContext.IndexType.COMPOSITE_ADVICE;
            indexName = (String)reqDataHash.get(Constants.COMPOSITE_ADVICE);
        } else if (((reqDataHash.get(ISAuthConstants.IP_RESOURCE_ENV_PARAM)
            != null) && "true".equalsIgnoreCase((String) reqDataHash.get(
                 ISAuthConstants.IP_RESOURCE_ENV_PARAM))) ||
            ((reqDataHash.get(ISAuthConstants.IP_RESOURCE_ENV_PARAM) == null) &&
              (reqDataHash.get(ISAuthConstants.RESOURCE_URL_PARAM) != null))) {
            indexType = AuthContext.IndexType.RESOURCE;
            indexName = AuthClientUtils.getResourceURL(request);
            envMap = AuthClientUtils.getEnvMap(request);
        } 
    }
    
    // Method to check if this is Session Upgrade
    private boolean checkNewOrg(SSOToken ssot) {
        loginDebug.message("Check New Organization!");
        boolean checkNewOrg = false;
        dontLogIntoDiffOrg = false;
        logIntoDiffOrg = false;

        try {
            // always make sure the orgName is the same
            String orgName = ssot.getProperty("Organization");
            String newOrgName = AuthUtils.getDomainNameByRequest(request, reqDataHash);
            if (loginDebug.messageEnabled()) {
                loginDebug.message("original org is : " + orgName);
                loginDebug.message("new org is : " + newOrgName);
            }
            
            //if new Org is not valid / does not exist
            if ((newOrgName == null) || (newOrgName.length() == 0)) {
                return checkNewOrg;
            }
            
            // if new Org is different from the old Org
            if (!(DNUtils.normalizeDN(newOrgName))
                .equals(DNUtils.normalizeDN(orgName))) {
                String strButton = (String)reqDataHash.get(BUTTON);
                if (strButton == null) {
                    strButton = (String)reqDataHash.get(BUTTON_OLD);
                }
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("Submit with button : " + strButton);
                }
                
                if ((strButton != null) && (strButton.length() != 0)) {
                    ISLocaleContext localeContext = new ISLocaleContext();
                    localeContext.setLocale(request);
                    fallbackLocale = localeContext.getLocale();
                    rb =  rbCache.getResBundle(bundleName, fallbackLocale);
                    
                    if (strButton.trim().equals(rb.getString("Yes").trim())) {
                        logIntoDiffOrg = true;
                        loginDebug.message("Submit with YES. Destroy session.");
                        clearCookie(AuthUtils.getCookieName());
                        AuthUtils.clearHostUrlCookie(response);
                        AuthUtils.clearlbCookie(request, response);
                        SSOTokenManager.getInstance().destroyToken(ssot);
                    } else if (strButton.trim().equals(rb.getString("No").trim())) {
                        loginDebug.message("Aborting different realm auth");
                        dontLogIntoDiffOrg = true;
                        return checkNewOrg;
                    }
                } else {
                    newOrg = true;
                    errorTemplate = "new_org.jsp";
                }
                checkNewOrg = true;
            }
        } catch (Exception e) {
            loginDebug.message("Exception in checkNewOrg : " + e);
        }
        
        if (loginDebug.messageEnabled()) {
            loginDebug.message("checkNewOrg : " + checkNewOrg);
        }
        return checkNewOrg;
    }
    
    // Method to set DSAME cookie
    private void setCookie() {
        loginDebug.message("Set Auth or AM cookie");
        String cookieDomain = null;
        Set cookieDomainSet = AuthClientUtils.getCookieDomainsForReq(request);
        if (cookieDomainSet.isEmpty()) { //No cookie domain specified in profile
            try {
                cookie = AuthUtils.getCookieString(ac, null);
		int cookieTimeToLive = 0;
		if (isCookieTimeToLiveEnabled()) {
		    cookieTimeToLive = getCookieTimeToLive();
		    if ((cookieTimeToLive > 0)
			    && ac.getStatus() == AuthContext.Status.SUCCESS) {
			if (loginDebug.messageEnabled()) {
			    loginDebug.message("LoginViewBean.setCookie():"
				    + "set cookie maxAge=" + cookieTimeToLive);
			}
			cookie.setMaxAge(cookieTimeToLive);
		    }
		}
                CookieUtils.addCookieToResponse(response, cookie);
                if ((cookie.getName()).equals(AuthUtils.getCookieName())) {
                    AuthUtils.setHostUrlCookie(response);
                }
            } catch (Exception e) {
                loginDebug.message("Cound not set Auth or AM Cookie!");
            }
        } else {
            Iterator iter = cookieDomainSet.iterator();
            int cookieTimeToLive = 0;
            if (isCookieTimeToLiveEnabled()) {
                cookieTimeToLive = getCookieTimeToLive();
                if (cookieTimeToLive > 0 && ac.getStatus() == AuthContext.Status.SUCCESS) {
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("LoginViewBean.setCookie(): would set cookie maxAge=" + cookieTimeToLive);
                    }
                }
            }
            while (iter.hasNext()) {
                cookieDomain = (String)iter.next();
                cookie = AuthUtils.getCookieString(ac, cookieDomain);
		if (isCookieTimeToLiveEnabled() && cookieTimeToLive > 0
			&& ac.getStatus() == AuthContext.Status.SUCCESS) {
		    cookie.setMaxAge(cookieTimeToLive);
		}
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("cookie for new request : "
                    + cookie.toString());
                }
                CookieUtils.addCookieToResponse(response, cookie);
                if ((cookie.getName()).equals(AuthUtils.getCookieName())) {
                    AuthUtils.setHostUrlCookie(response);
                }
            }
        }
    }

    private void setlbCookie(){
        try {
            AuthUtils.setlbCookie(ac, request, response);
        } catch (Exception e) {
            loginDebug.message("Cound not set LB Cookie!");
        }
    }

    /** Method to clear AM Cookie
     */
    private void clearCookie(AuthContextLocal ac) {
        if (AuthUtils.isCookieSupported(ac)) {
            clearCookie(AuthUtils.getCookieName());
            AuthUtils.clearHostUrlCookie(response);
            AuthUtils.clearlbCookie(request, response);
        }
    }
    
    /** 
     *  Method to clear cookie based on the cookie
     *  name passed (Auth or AM Cookie)
     *  @param cookieName  name of cookie to be cleared.
     */
    private void clearCookie(String cookieName) {
        String cookieDomain = null;
        Set cookieDomainSet = AuthClientUtils.getCookieDomainsForReq(request);
        if (cookieDomainSet.isEmpty()) {// No cookie domain specified in profile
            cookie = AuthUtils.createCookie(cookieName,LOGOUTCOOKIEVALUE, null);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        } else {
            Iterator iter = cookieDomainSet.iterator();
            while (iter.hasNext()) {
                cookieDomain = (String)iter.next();
                cookie = AuthUtils.createCookie(cookieName,LOGOUTCOOKIEVALUE,
                cookieDomain);
                cookie.setMaxAge(0); // tell browser to expire DSAME Cookie
                response.addCookie(cookie);
            }
        }        
    }    
   
    private void clearCookie(HttpServletRequest req) {	 
        if (AuthUtils.isCookieSupported(req)) {	 
            clearCookie(AuthUtils.getCookieName());	 
            AuthUtils.clearHostUrlCookie(response);	 
            AuthUtils.clearlbCookie(request, response);	 
            if (storeCookies != null && !storeCookies.isEmpty()) {	 
                for (Iterator it = storeCookies.iterator();	 
                    it.hasNext();){	 
                    String cookieName = (String)it.next();	 
                    AuthUtils.clearServerCookie(cookieName, request, response);	 
                }	 
            }	 
        }	 
    }

    // Method to check if Persistent exist and use it to login to DSAME
    private boolean isPersistentCookieValid() {
        if (loginDebug.messageEnabled()) {
            loginDebug.message("PCOOKIE setting in profile "
            + AuthUtils.getPersistentCookieMode(ac));
            loginDebug.message("PCOOKIE setting in URL "
            + AuthUtils.isPersistentCookieOn(ac));
        }

        // persistent cookie setting in profile
        if (AuthUtils.getPersistentCookieMode(ac)) {
            String userName = AuthUtils.searchPersistentCookie(ac);
            
            if  (userName!=null) {  // persistent cookie exist!
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("Username is " + userName);
                }
                // try login with the PCookie
                try {
                    ac.login(AuthContext.IndexType.USER, userName, true);
                    // if came here and session upgrade
                    // case then return
                    if (AuthUtils.isSessionUpgrade(ac)) {
                        return true;
                    }
                    if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                        LoginSuccess = true;
                        ResultVal = rb.getString("authentication.successful");
                        redirect_url = AuthUtils.getLoginSuccessURL(ac);
                        loginDebug.message(
                            "Session activate by persistent cookie!");
                        addPersistentCookie();
                        return true;
                    }
                } catch (Exception e) {  // clear the invalid PCookie
                    String cookieDomain = null;
                    Set cookieDomainSet =
                            AuthClientUtils.getCookieDomainsForReq(request);

                    // No cookie domain specified in profile
                    if (cookieDomainSet.isEmpty()) {
                        try {
                            cookie = AuthUtils.clearPersistentCookie(null, ac);
                            response.addCookie(cookie);
                        } catch (Exception ee) {
                            loginDebug.message("Could not set Persistent Cookie!");
                        }
                    } else {
                        Iterator iter = cookieDomainSet.iterator();
                        while (iter.hasNext()) {
                            cookieDomain = (String)iter.next();
                            Cookie cookie = AuthUtils.clearPersistentCookie(
                                cookieDomain, ac);
                            response.addCookie(cookie);
                        }
                    }
                    handleAuthLoginException(null);
                }
            }
        }
        return false;
    }
    
    // Method to add persistent cookie
    private void addPersistentCookie() {
        String cookieDomain = null;
        Set cookieDomainSet = AuthClientUtils.getCookieDomainsForReq(request);
        if (cookieDomainSet.isEmpty()) { //No cookie domain specified in profile
            try {
                cookie = AuthUtils.createPersistentCookie(ac, null);
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("cookie for new request : "
                    + cookie.toString());
                    loginDebug.message("Cookie domain is null.");
                }
                CookieUtils.addCookieToResponse(response, cookie);
            } catch (Exception e) {
                loginDebug.message("Could not set Persistent Cookie!");
            }
        } else {
            Iterator iter = cookieDomainSet.iterator();
            try {
                while (iter.hasNext()) {
                    cookieDomain = (String)iter.next();
                    cookie = AuthUtils.createPersistentCookie(ac, cookieDomain);
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("cookie for new request : "
                        + cookie.toString());
                    }
                    CookieUtils.addCookieToResponse(response, cookie);
                }
            } catch (Exception e) {
                loginDebug.message("Could not set Persistent Cookie!");
            }
        }
    }
    
    // get error template, message as well as error code.
    private void setErrorMessage(Exception e) {
        String authErrorCode = null;
        
        if ((e != null) && (e instanceof L10NMessage)) {
            // if exception is instance of L10NMessage,
            // then get error code from exception
            L10NMessage l10nE = (L10NMessage) e;
            authErrorCode = l10nE.getErrorCode();
            // in case this AuthLoginException is only a wrapper of
            // LoginException which does not have errorCode.
            if (authErrorCode != null) {
                errorCode = authErrorCode;
                ErrorMessage = l10nE.getL10NMessage(
                        com.sun.identity.shared.locale.Locale.getLocale(
                        AuthUtils.getLocale(ac)));
            }
        }
        if (authErrorCode == null) {
            // if error code can not be got from exception,
            // then get error code and message from auth context
            if (ac != null) {
                errorCode = ac.getErrorCode();
                ErrorMessage = ac.getErrorMessage();
            }
        }

        if (errorCode == null || errorCode.isEmpty()) {
            //if error code is still null, let's set it to AUTH_ERROR, so the
            //template lookup will succeed
            errorCode = AMAuthErrorCode.AUTH_ERROR;
        }
        if (ErrorMessage == null || ErrorMessage.isEmpty()) {
            // if error message is still null,
            // then get error message by using error code
            ErrorMessage = AuthUtils.getErrorMessage(errorCode);
        }
        
        if (ac != null) {
            errorTemplate = ac.getErrorTemplate();
        } else {
            errorTemplate = AuthUtils.getErrorTemplate(errorCode);
        }
        
        // handle InternalSession timeout
        if (loginURL != null && errorCode.equals("110") && loginURL.isEmpty()) {
            setDisplayFieldValue(LOGIN_URL, AuthUtils.constructLoginURL(request));
        }
        
        if (loginDebug.messageEnabled()) {
            loginDebug.message("Error Message = " + ErrorMessage);
            loginDebug.message("Error Template = " + errorTemplate);
            loginDebug.message("Error Code = " + errorCode);
        }

        response.setHeader("X-AuthErrorCode", "-1");
    }
    
    // Method to retrieve filename
    private String getFileName(String fileName) {
        String relativeFileName = null;
        if (ac != null) {
            relativeFileName = AuthUtils.getFileName(ac,fileName);
        } else {
            relativeFileName = AuthUtils.getDefaultFileName(request,fileName);
        }
        if (loginDebug.messageEnabled()) {
            loginDebug.message("fileName is : " + fileName);
            loginDebug.message("relativeFileName is : " + relativeFileName);
        }
        
        return relativeFileName;
    }
    
    /** Retrieves the original AuthContext and the session,
     *  sets the cookie and retrieves the original
     *  success login url.
     *  @return redirect_url, a String
     */
    String getPrevSuccessURLAndSetCookie() {
        loginDebug.message("Restoring original Session !");
        InternalSession oldSession = AuthUtils.getOldSession(ac);
        clearCookieAndDestroySession(ac);
        if (oldSession != null) {
            ac.getLoginState().setSession(oldSession);
            ac.getLoginState().setSid(oldSession.getID());
        }
        String redirect_url = oldSession.getProperty(ISAuthConstants.
            SUCCESS_URL);
        return redirect_url;
    }
    
    // Get all the Original Auth credentials to start a new request
    private boolean canGetOrigCredentials(SSOToken ssoToken) {
        loginDebug.message("BACK re-submit with valid session");
        boolean gotOrigCredentials = false;
        try {
            loginURL = ssoToken.getProperty("loginURL");
            indexType = AuthUtils.getIndexType(ssoToken.getProperty("IndexType"));
            indexName = AuthUtils.getIndexName(ssoToken,indexType);
            gotOrigCredentials = true;
        } catch (Exception e){
            loginDebug.message("Error in canGetOrigCredentials");
        }
        if (loginDebug.messageEnabled()) {
            loginDebug.message(
                "canGetOrigCredentials : IndexType = " + indexType);
            loginDebug.message(
                "canGetOrigCredentials : IndexName = " + indexName);
        }
        return gotOrigCredentials;
    }   
   
    /**
     * Clears all global variables.
     */
    private void clearGlobals() {
        ac = null;
        cookie = null;
    }
    
    /** Clear cookie and destroy session
     *  @param ac AuthContext for this request
     */
    private void clearCookieAndDestroySession(AuthContextLocal ac) {
        // clear cookie, destroy orignal invalid session
        if (AuthUtils.isCookieSupported(ac)) {
            clearCookie(AuthUtils.getAuthCookieName());
        }
        AuthUtils.destroySession(ac);
    }

    private void handleAuthLoginException(AuthLoginException ale) {
        LoginFail = true;
        setErrorMessage(ale);
        ResultVal = ErrorMessage;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Display cycle events:
    // If the fireDisplayEvents attribute in a display field tag is set to true,
    // then the begin/endDisplay events will fire for that display field.
    ////////////////////////////////////////////////////////////////////////////
    
    // StaticTextResult ( Result )
    /**
     * Returns if it begins static text result display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginStaticTextResultDisplay(ChildDisplayEvent event) {
        return true;
    }
    
    /**
     * Returns if it begins content static text result display
     * @param event child display event
     * @return <code>false</code> if result value if null or empty.
     */ 
    public boolean beginContentStaticTextResultDisplay(ChildDisplayEvent event){
        if (( ResultVal == null ) || (ResultVal.length() == 0)) {
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
     * Returns if it begins content static warning display
     * @param event child display event
     * @return <code>true</code> if lock warning is not null or not empty.
     */ 
    public boolean beginContentStaticWarningDisplay(ChildDisplayEvent event) {
        lockWarning = ac.getLockoutMsg();
        accountLocked = ac.isLockedOut();
        if (loginDebug.messageEnabled()) {
            loginDebug.message("lock warning message is : " + lockWarning);
        }
        return ((lockWarning != null) && (lockWarning.length() > 0));
    }
    
    /**
     * Returns if it begins static text message display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginStaticTextMessageDisplay(ChildDisplayEvent event) {
        return true;
    }
    
    /**
     * Returns if it begins content static text message display
     * @param event child display event
     * @return <code>true</code> if error message is not null or not empty.
     */ 
    public boolean beginContentStaticTextMessageDisplay(
        ChildDisplayEvent event
    ) {
        return ((ErrorMessage != null) && (ErrorMessage.length() > 0));
    }
    
    /**
     * Returns if it begins static text header display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginStaticTextHeaderDisplay(ChildDisplayEvent event) {
        return true;
    }
    
    /**
     * Returns if it begins content static text header display
     * @param event child display event
     * @return <code>true</code> if text header is not null or not empty.
     */ 
    public boolean beginContentStaticTextHeaderDisplay(ChildDisplayEvent event){
        return ((TextHeaderVal != null ) && (TextHeaderVal.length() > 0));
    }
    
    /**
     * Returns if it begins href display
     * @param event child display event
     * @return <code>true</code> by default.
     */ 
    public boolean beginHrefDisplay(ChildDisplayEvent event) {
        return true;
    }
    
    /**
     * Returns if it begins content href display
     * @param event child display event
     * @return <code>true</code> if result value is not null and account is not 
     *  locked or error template is not null and not empty.
     */ 
    public boolean beginContentHrefDisplay(ChildDisplayEvent event) {
        return (((ResultVal != null) &&
            ( ResultVal.length() > 0) && LoginFail && !accountLocked)
            || ((errorTemplate != null) && ( errorTemplate.length() > 0)));
    }
    
    /**
     * Returns if it begins content button login display
     * @param event child display event
     * @return <code>true</code> if login is not completed.
     */ 
    public boolean beginContentButtonLoginDisplay(ChildDisplayEvent event) {
        return (!LoginSuccess && !LoginFail);
    }
    
    /**
     * Returns if it begins content image display
     * @param event child display event
     * @return <code>true</code> if page image is not null or not empty.
     */ 
    public boolean beginContentImageDisplay(ChildDisplayEvent event) {
        return ((pageImage != null ) && (pageImage.length() > 0));
    }
    
    /**
     * gets display of valid content block
     *
     * @param event - child display event
     * @return true of bean is valid
     */
    public boolean beginValidContentDisplay(ChildDisplayEvent event) {
        return !LoginSuccess && !LoginFail && !bValidSession;
    }
    
    /**
     * begins display of has button content
     *
     * @param event - child display event
     * @return true if there is one or more buttons
     */
    public boolean beginHasButtonDisplay(ChildDisplayEvent event) {
        return (buttonOptions != null) && (buttonOptions.length > 0);
    }
    
    /**
     * begins display of has no button content
     *
     * @param event - child display event
     * @return true if there is no button
     */
    public boolean beginHasNoButtonDisplay(ChildDisplayEvent event) {
        return (buttonOptions == null) || (buttonOptions.length == 0);
    }
    
    /**
     * begins display of occupy full browser
     *
     * @param event - child display event
     * @return false if session is in progress
     */
    public boolean beginOccupyFullBrowserDisplay(ChildDisplayEvent event) {
        if (loginDebug.messageEnabled()) {
            loginDebug.message("Login Status : " + ac.getStatus());
        }
        
        if (sessionUpgrade)
            return false;
        return true;
    }

   /**
    * Enables AM session cookie time to live
    */
   public void enableCookieTimeToLive() {
       int cookieTimeToLive = 0;
       String cookieTimeToLiveString = SystemProperties.get(
            com.sun.identity.shared.Constants.AM_COOKIE_TIME_TO_LIVE);
     if ((cookieTimeToLiveString != null)
               && (cookieTimeToLiveString.length() != 0)) {
           try {
               cookieTimeToLive
                       = Integer.parseInt(cookieTimeToLiveString) * 60;
               if (loginDebug.messageEnabled()) {
                   loginDebug.message("LoginViewBean.enableCookieTimeToLive():"
                       + "cookieTimeToLive=" + cookieTimeToLive);
               }

           } catch (NumberFormatException nfe) {
               if (loginDebug.warningEnabled()) {
                   loginDebug.warning("LoginViewBean.enableCookieTimeToLive():"
                       + "not a valid number, leaving cookieTimeToLive as 0");
               }
           }
       }
       if (cookieTimeToLive > 0) {
           boolean cookieTimeToLiveEnabledFlag = true;
           if (loginDebug.messageEnabled()) {
               loginDebug.message("LoginViewBean.enableCookieTimeToLive():"
                   + "cookieTimeToLive " + cookieTimeToLive + "s, enabled");
           }
           ac.getLoginState().setCookieTimeToLive(cookieTimeToLive);
           ac.getLoginState().enableCookieTimeToLive(true);
       } else {
           if (loginDebug.messageEnabled()) {
               loginDebug.message("LoginViewBean.enableCookieTimeToLive():"
                   + "cookieTimeToLive not enabled");
           }
       }
   }

    /**
     * Checks whether AM session cookie time to live is enabled
     * @return <code>true</code> if AM session cookie time to live
     *         is enabled, otherwise returns <code>false</code>
     */
    public boolean isCookieTimeToLiveEnabled() {
        return ac.getLoginState().isCookieTimeToLiveEnabled();
    }
   
    /**
     * Returns AM session cookie time to live
     * @return AM session cookie time to live in seconds
     */
    public int getCookieTimeToLive() {
        return ac.getLoginState().getCookieTimeToLive();
    }

    /**
     * Returns the name of the current realm
     * @return Realm name for current context
     */
    public String getRealmName() {
        if (ac == null) {
            return null;
        }

        LoginState ls = ac.getLoginState();
        if (ls != null) {
            return ls.getOrgName();
        }
        return null;
    }

    /**
     * Returns the service (authentication chain name)
     * @return Applicable service for current context, or null if service based authentication is not being used.
     */
    public String getAuthChainName() {
        if (ac == null) {
            return null;
        }

        LoginState ls = ac.getLoginState();
        if (ls != null && ls.getIndexType() == AuthContext.IndexType.SERVICE) {
            return ls.getIndexName();
        }
        return null;
    }


    ////////////////////////////////////////////////////////////////////////////
    // Class variables
    ////////////////////////////////////////////////////////////////////////////
    /** Page name for login */
    public static final String PAGE_NAME="Login";
    /** Result value */
    public String ResultVal = "";
    /** Error message */
    public String ErrorMessage = "";
    /** Error template */
    public String errorTemplate = "";
    /** Error code */
    public String errorCode = "";
    /** Lock warning */
    public String lockWarning = "";
    /** Account lock */
    public boolean accountLocked=false;
    /** Text header value */
    public String TextHeaderVal = "";
    /** Redirect url */
    public String redirect_url = null;
    /** Page state */
    public String pageState = null;
    /** Choice */
    public String choice = "";
    /** Page template */
    public String pageTemplate = "";
    /** Page image */
    public String pageImage = "";
    /** Login failure */
    public boolean LoginFail = false;
    /** Login success */
    public boolean LoginSuccess = false;
    /** Auth level */
    public boolean bAuthLevel = false;
    /** Session is valid */
    public boolean bValidSession = false;
    /** Request is post */
    public boolean isPost = false;
    /** Required list */
    public List requiredList = null;
    public List<String> infoText = null;
    AuthContextLocal ac;
    private Hashtable reqDataHash = new Hashtable();
    private static String LOGINURL = "";
    private String loginURL = "";
    private String origLoginURL = "";
    private static final String LOGOUTCOOKIEVALUE = "LOGOUT";
    private static final String bundleName = "amAuthUI";
    private boolean onePageLogin = false;
    private boolean sessionUpgrade = false;
    private boolean forward = true;
    private boolean newOrg = false;
    private boolean bHttpBasic = false;
    private boolean newOrgExist = false;
    private boolean dontLogIntoDiffOrg = false;
    private boolean logIntoDiffOrg = false;
    HttpServletRequest request;
    HttpServletResponse response;
    Cookie cookie;
    static Debug loginDebug = Debug.getInstance("amLoginViewBean");
    private final RedirectCallbackHandler redirectCallbackHandler = new RedirectCallbackHandler();
    String client_type = "";
    String orgName = "";
    String indexName = "";
    AuthContext.IndexType indexType;
    Map envMap = null;
    /** List of callback */
    public Callback[] callbacks = null;
    /** List of button options */
    public String[] buttonOptions = null;
    /** Default button index */
    public int defaultButtonIndex = 0;
    String jsp_page=null;
    private boolean forceAuth;
    
    /** Default parameter name for old token */
    public static final String TOKEN_OLD = "Login.Token";
    /** Default parameter name for token */
    public static final String TOKEN = "IDToken";
    /** Default parameter name for old button */
    public static final String BUTTON_OLD = "Login.ButtonLogin";
    /** Default parameter name for id button */
    public static final String BUTTON = "IDButton";
    
    /** Default parameter name for page state  */
    public static final String PAGE_STATE = "PageState";
    /** Default parameter name for login url */
    public static final String LOGIN_URL = "LoginURL";
    /** Original login URL used on first access */
    public static final String AM_ORIG_URL = "AMOrigURL";
    /** Default parameter name for default login url */
    public static final String DEFAULT_LOGIN_URL = "DefaultLoginURL";
    /** Default parameter name for redirect url */
    public static final String REDIRECT_URL = "RedirectURL";
    /** Default parameter name for tiled callback */
    public static final String TILED_CALLBACKS = "tiledCallbacks";
    /** Default parameter name for tiled buttons */
    public static final String TILED_BUTTONS = "tiledButtons";
    /** Default parameter name for default buttons */
    public static final String DEFAULT_BTN = "defaultBtn";
    /** Default parameter name for text goto login after failure  */
    public static final String TXT_GOTO_LOGIN_AFTER_FAIL =
    "txtGotoLoginAfterFail";
    /** Default parameter name for submit command */
    public static final String CMD_SUBMIT = "cmdSubmit";
    /** Default parameter name for submit label */
    public static final String LBL_SUBMIT = "lblSubmit";
    /** Default parameter name for continue command */
    public static final String CMD_CONTINUE = "cmdContinue";
    /** Default parameter name for continue label */
    public static final String LBL_CONTINUE = "lblContinue";
    /** Default parameter name for agree command */
    public static final String CMD_AGREE = "cmdAgree";
    /** Default parameter name for agree label */
    public static final String LBL_AGREE = "lblAgree";
    /** Default parameter name for disagree command */
    public static final String CMD_DISAGREE = "cmdDisagree";
    /** Default parameter name for disagree label */
    public static final String LBL_DISAGREE = "lblDisagree";
    /** Default parameter name for command yes */
    public static final String CMD_YES = "cmdYes";
    /** Default parameter name for label yes */
    public static final String LBL_YES = "lblYes";
    /** Default parameter name for command no */
    public static final String CMD_NO = "cmdNo";
    /** Default parameter name for label no */
    public static final String LBL_NO = "lblNo";
    /** Default parameter name for new user command */
    public static final String CMD_NEW_USER = "cmdNewUser";
    /** Default parameter name for new user label */
    public static final String LBL_NEW_USER = "lblNewUser";
    /** Default parameter name for reset label */
    public static final String LBL_RESET = "lblReset";
    
    /** Default parameter name for login html title */
    public static final String HTML_TITLE_LOGIN = "htmlTitle_Login";
    /** Default parameter name for login title message */
    public static final String HTML_TITLE_MESSAGE = "htmlTitle_Message";
    /** Default parameter name for redirect html title */
    public static final String HTML_TITLE_REDIRECT = "htmlTitle_Redirect";
    /** Default parameter name of html title for account expired */
    public static final String HTML_TITLE_ACCOUNTEXPIRED =
        "htmlTitle_AccountExpired";
    /** Default parameter name of html title for auth error */
    public static final String HTML_TITLE_AUTHERROR = "htmlTitle_AuthError";
    /** Default parameter name of html title for self registration error */
    public static final String HTML_TITLE_SELFREGERROR =
        "htmlTitle_SelfRegError";
    /** Default parameter name of html title for disclaimer */
    public static final String HTML_TITLE_DISCLAIMER = "htmlTitle_Disclaimer";
    /** Default parameter name of html title for invalid cookie id */
    public static final String HTML_TITLE_INVALIDPCOOKIEUID =
        "htmlTitle_InvalidPCookieUID";
    /** Default parameter name of html title for invalid password */
    public static final String HTML_TITLE_INVALIDPASSWORD =
        "htmlTitle_InvalidPassword";
    /** Default parameter name of html title for invalid domain */
    public static final String HTML_TITLE_INVALIDDOMAIN =
        "htmlTitle_InvalidDomain";
    /** Default parameter name of html title for user profile not found */
    public static final String HTML_TITLE_USERPROFILENOTFOUND =
        "htmlTitle_UserProfileNotFound";
    /** Default parameter name of html title for auth failure */
    public static final String HTML_TITLE_AUTHFAILED = "htmlTitle_AuthFailed";
    /** Default parameter name of html title for membership */
    public static final String HTML_TITLE_MEMBERSHIP = "htmlTitle_Membership";
    /** Default parameter name of html title for auth module denied */
    public static final String HTML_TITLE_AUTHMODULEDENIED =
        "htmlTitle_AuthModuleDenied";
    /** Default parameter name of html title for no config error */
    public static final String HTML_TITLE_NOCONFIGERROR =
        "htmlTitle_NoConfigError";
    /** Default parameter name of html title for org inactive */
    public static final String HTML_TITLE_ORGINACTIVE =
        "htmlTitle_OrgInactive";
    /** Default parameter name of html title for self module registration */
    public static final String HTML_TITLE_SELFREGMODULE =
        "htmlTitle_SelfRegModule";
    /** Default parameter name of html title for session timeout */
    public static final String HTML_TITLE_SESSIONTIMEOUT =
        "htmlTitle_SessionTimeOut";
    /** Default parameter name of html title for user not found */
    public static final String HTML_TITLE_USERNOTFOUND =
        "htmlTitle_UserNotFound";
    /** Default parameter name of html title for user inactive */
    public static final String HTML_TITLE_USERINACTIVE =
        "htmlTitle_UserInactive";
    /** Default parameter name of html title for new organization */
    public static final String HTML_TITLE_NEWORG = "htmlTitle_NewOrg";
    /** Default parameter name of html title for max session */
    public static final String HTML_TITLE_MAXSESSIONS = "htmlTitle_MaxSessions";

    public static final String SSO_REDIRECT = "/SSORedirect";
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance variables
    ////////////////////////////////////////////////////////////////////////////
    
    static {
        LOGINURL = serviceUri + "/UI/Login";
    }
    
}

