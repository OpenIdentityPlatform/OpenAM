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
 * $Id: LoginViewBean.java,v 1.37 2009/11/11 12:22:32 bhavnab Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock, Inc.
 */

package com.sun.identity.authentication.distUI;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.ImageField;
import com.iplanet.jato.view.html.StaticTextField;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.UI.ButtonTiledView;
import com.sun.identity.authentication.UI.CallBackTiledView;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.share.RedirectCallbackHandler;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.shared.locale.L10NMessageImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.openam.authentication.service.protocol.RemoteCookie;
import org.forgerock.openam.authentication.service.protocol.RemoteHttpServletRequest;
import org.forgerock.openam.authentication.service.protocol.RemoteHttpServletResponse;
import org.forgerock.openam.utils.ClientUtils;

/**
 * A default implementation of <code>LoginViewBean</code> auth Login UI.
 */
public class LoginViewBean
extends com.sun.identity.authentication.UI.AuthViewBeanBase {
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
            redirect_url = AuthClientUtils.encodeURL(redirect, request, ac);
            return new StaticTextField(this, name, redirect_url);
        } else if (name.equals(DEFAULT_LOGIN_URL)) {
            String default_login_url = AuthClientUtils.encodeURL(LOGINURL, request, ac);
            return new StaticTextField(this, name, default_login_url);
        } else if (name.equals(LOGIN_URL)) { // non-cookie support
            if ((loginURL==null)||(loginURL.length() == 0)) {
                loginURL = LOGINURL;
            }
            loginURL = AuthClientUtils.encodeURL(loginURL, request, ac);
            return new StaticTextField(this, name, loginURL);
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
            return new StaticTextField(this, HTML_TITLE_USERPROFILENOTFOUND,"");
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
        
        if (requestContext != null) {
            request = requestContext.getRequest();
            response = requestContext.getResponse();
            servletContext = requestContext.getServletContext();
            session = request.getSession();
        }
        
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        if (AuthClientUtils.isVersionHeaderEnabled()) {
            response.setHeader("X-DSAMEVersion", AuthClientUtils.getDSAMEVersion());
        }
        
        // get request ( GET ) parameters for 'login' process
        reqDataHash = AuthClientUtils.parseRequestParameters(request);
        gotoUrl = request.getParameter("goto");
        gotoOnFailUrl = request.getParameter("gotoOnFail");
        String encoded = request.getParameter("encoded");
        if (encoded != null && encoded.equals("true")) {
            gotoUrl = AuthClientUtils.getBase64DecodedValue(gotoUrl);
            gotoOnFailUrl = AuthClientUtils.getBase64DecodedValue(gotoOnFailUrl);
        }
        
        if (loginDebug.messageEnabled()) {
            //loginDebug.message("request data hash : " + reqDataHash);
            loginDebug.message("Request method is : " + request.getMethod());
            loginDebug.message("gotoUrl is : " + gotoUrl);
            loginDebug.message("gotoOnFailUrl is : " + gotoOnFailUrl);
        }
        
        if (request.getMethod().equalsIgnoreCase("POST")) {
            isPost = true;
        }
        
        try {
            manager = SSOTokenManager.getInstance();
            boolean newOrgExist = false;
            boolean isBackPost = false;
            // if the request is a GET then iPlanetAMDirectoryPro cookie
            // will be used to retrieve the session for session upgrade
            SessionID sessionID = AuthClientUtils.getSessionIDFromRequest(request);
            ssoToken = AuthClientUtils.getExistingValidSSOToken(sessionID);
	    orgName = AuthClientUtils.getDomainNameByRequest(request, reqDataHash);	 
            prepareLoginParams();
            //Check for session Timeout
            if((ssoToken == null) && (sessionID != null) &&
              (sessionID.toString().length()!= 0)){
                    if(AuthClientUtils.isTimedOut(sessionID)){
                        ISLocaleContext localeContext = new ISLocaleContext();	 
                        localeContext.setLocale(request);	 
                        locale = localeContext.getLocale();	 
                        rb =  rbCache.getResBundle(bundleName, locale);	 
 	 
                        timeout_jsp_page = AuthClientUtils.getFileName(	 
                             JSP_FILE_NAME,locale.toString(),orgName,request,servletContext,indexType,indexName);
                        clearCookie();
                        errorCode = AMAuthErrorCode.AUTH_TIMEOUT;
                        ErrorMessage = AuthClientUtils.getErrorVal(
                              AMAuthErrorCode.AUTH_TIMEOUT,
                              AuthClientUtils.ERROR_MESSAGE);
                        errorTemplate = AuthClientUtils.getErrorVal(
                              AMAuthErrorCode.AUTH_TIMEOUT,
                              AuthClientUtils.ERROR_TEMPLATE);

                        loginURL = AuthClientUtils.constructLoginURL(request);
                        session.setAttribute("LoginURL", loginURL);
                        super.forwardTo(requestContext);
                        return;
                  }
            }
            String authCookieValue = AuthClientUtils.getAuthCookieValue(request);
            loginURL = AuthClientUtils.constructLoginURL(request);
            //Commented out due to OPENAM-1215
            //session.setAttribute("LoginURL", loginURL);
            reqDataHash = AuthClientUtils.parseRequestParameters(request);

            if (ssoToken != null) {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("Existing valid ssoToken = " + ssoToken);
                }
                if (AuthClientUtils.newSessionArgExists(reqDataHash)) {
                    clearCookie();
                    manager.destroyToken(ssoToken);
                } else {
                    loginDebug.message("Old Session is Active.");
                    newOrgExist = checkNewOrg(ssoToken);
                    if (!newOrgExist && !dontLogIntoDiffOrg) {
                        if (isPost) {
                            isBackPost = canGetOrigCredentials(ssoToken);
                        }
                        sessionUpgrade = AuthClientUtils.checkSessionUpgrade(
                        ssoToken,reqDataHash);
                    }
                }
            }
            
            if (sessionUpgrade) {
               if (loginDebug.messageEnabled()) {
                    loginDebug.message("New AuthContext with " +
                         "existing valid SSOToken");
                }
                ac = new AuthContext(ssoToken);
                newRequest = true;
            } else if ((ssoToken == null && (session.isNew() || 
                    (authCookieValue == null) || 
                    (authCookieValue.length() == 0))) ||
                    (authCookieValue != null && 
                     authCookieValue.equalsIgnoreCase("LOGOUT")) ||
                    (authCookieValue != null && 
                     isOrgChanged()) ||                     
                    (!isOrgSame()) || 
                     AuthClientUtils.newSessionArgExists(reqDataHash)) {
            	if (loginDebug.messageEnabled()) {
                    loginDebug.message("New AuthContext with OrgName = "
                    + orgName);
                }
                ac = new AuthContext(orgName);
                newRequest = true;
            }
            
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(request);
            locale = localeContext.getLocale();

            if (newRequest) {
                loginDebug.message("New request / New AuthContext created");
                client_type = AuthClientUtils.getClientType(request);
                session.setAttribute("Client_Type", client_type);
                session.setAttribute("Locale", locale);
                session.setAttribute("LoginURL", loginURL);
                session.setAttribute("OrgName", orgName);
                session.setAttribute("AuthContext", ac);
                cookieSupported = AuthClientUtils.isCookieSupported(request);
                if (cookieSupported) {
                    if (AuthClientUtils.persistAMCookie(reqDataHash)) {
                        enableCookieTimeToLive();
                    }
                }
            } else if ( (authCookieValue != null) &&
                    (authCookieValue.length() != 0) &&
                    (!authCookieValue.equalsIgnoreCase("LOGOUT")) ) {
                loginDebug.message("Existing request");
                client_type = (String) session.getAttribute("Client_Type");
                loginURL = getLoginURL();
                ac = (AuthContext) session.getAttribute("AuthContext");
                orgName = (String) session.getAttribute("OrgName");
                if (AuthClientUtils.isCookieSet(request)) {
                    if (AuthClientUtils.checkForCookies(request)) {
                        loginDebug.message("Client support cookie");
                        cookieSupported = true;
                    } else {
                        loginDebug.message("Client do not support cookie");
                        cookieSupported = false;
                    }
                }
            }
            
            if (loginDebug.messageEnabled()) {
                loginDebug.message("Client Type is: " + client_type);
                loginDebug.message("JSPLocale = " + locale);
                loginDebug.message("loginURL : " + loginURL);
                loginDebug.message("Auth Context : " + ac);
                loginDebug.message("CookieSupported : " + cookieSupported);
                loginDebug.message("SessionUpgrade : " + sessionUpgrade);
            }
            
            fallbackLocale = locale;
            rb =  rbCache.getResBundle(bundleName, locale);
            if(ac != null){
                ac.setLocale(locale);
                ac.setClientHostName(ClientUtils.getClientIPAddress(request));
            }
            
            if ((errorTemplate==null)||(errorTemplate.length() == 0)) {            
                processLogin();
                if ((newRequest) && 
                        (AuthClientUtils.isCookieSupported(request))) {
                    setServerCookies();

                    if (ac != null &&
                            ac.getStatus() != AuthContext.Status.SUCCESS) {
                        setCookie();
                    }

                    setlbCookie();
                }
            }
            
        } catch (Exception e) {
            loginDebug.message("New Auth Context Error : " , e);
            
            setErrorMessage(e);
            jsp_page = errorTemplate;
            if (requestContext==null) {
                return;
            }
            super.forwardTo(requestContext);
            return;
        }
        
        // Set header for Misrouted server's usage
        response.setHeader("AM_CLIENT_TYPE", client_type);
        
        if (ac != null) {
            if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                response.setHeader("X-AuthErrorCode", "0");
                if (cookieSupported) {
                    setCookie();
                    clearCookieAndDestroySession();
                }
                try {
                    if (ssoToken != null &&
                            SystemProperties.getAsBoolean(Constants.DESTROY_SESSION_AFTER_UPGRADE)) {
                        loginDebug.message(
                            "Destroy existing/old valid session");
                        manager.destroyToken(ssoToken);
                    }
                } catch (Exception e) {
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message(
                            "destroyToken error : " + e.toString());
                    }
                }
            } else if (ac.getStatus() == AuthContext.Status.FAILED) {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("Destroy Session! for ac : " + ac);
                }
                clearCookieAndDestroySession();
                loginDebug.message("Login failure, current session destroyed!");
            }
        }
        
        if ((redirect_url != null) && (redirect_url.length() != 0)) {
            // forward check for liberty federation, if the redirect_url
            // is the federation post login servlet, use forward instead
            if (AuthClientUtils.isGenericHTMLClient(client_type)) {
                try {
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("Send Redirect to " + redirect_url);
                    }
                    
                    clearGlobals();
                    response.sendRedirect(redirect_url);
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
        if ((redirect_url != null) && (redirect_url.length() != 0) && (!redirect_url.equals("null"))) {
            jsp_page = "Redirect.jsp";
        } else if ((errorTemplate != null) && (errorTemplate.length() != 0) && (!errorTemplate.equals("null"))) {
            jsp_page = errorTemplate;
        } else if ((ErrorMessage != null) && (ErrorMessage.length() != 0) && (!ErrorMessage.equals("null"))) {
            jsp_page = "Message.jsp";
        } else if ((pageTemplate != null) && (pageTemplate.length() != 0) && (!pageTemplate.equals("null"))) {
            if (loginDebug.messageEnabled()) {
                loginDebug.message("Using module Template : " + pageTemplate);
            }
            jsp_page =  pageTemplate;
        } else {
            jsp_page =  "Login.jsp";
        }
        
        if(timeout_jsp_page != null) {
           jsp_page = timeout_jsp_page;	         
        } else {	 
           jsp_page = getFileName(jsp_page);	 
        }
        
        if ((param != null) && (param.length() != 0)) {
            if (jsp_page.indexOf("?") != -1) {
                jsp_page = jsp_page + "&amp;org=" + param;
            } else {
                jsp_page = jsp_page + "?org=" + param;
            }
        }
        return AuthClientUtils.encodeURL(jsp_page,request,ac);
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
            setDisplayFieldValue(
                HTML_TITLE_LOGIN, rb.getString("htmlTitle_Login"));
            setDisplayFieldValue(
                HTML_TITLE_MESSAGE, rb.getString("htmlTitle_Message"));
            setDisplayFieldValue(
                HTML_TITLE_REDIRECT, rb.getString("htmlTitle_Redirect"));
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
                setDisplayFieldValue(TXT_GOTO_LOGIN_AFTER_FAIL,
                    "Try again. Go To Login");
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
        
        try {
            if (ssoToken != null && !sessionUpgrade && !checkNewOrg &&
                   !AuthClientUtils.newSessionArgExists(reqDataHash)) {
                loginDebug.message("Session is Valid / already authenticated");
                bValidSession = true;

                /*
                 * redirect to 'goto' parameter or SPI hook or default
                 * redirect URL.
                 */
                redirect_url = gotoUrl;

                if ((gotoUrl == null) || (gotoUrl.length() == 0) ||
                    (gotoUrl.equalsIgnoreCase("null"))) {
                    redirect_url = ssoToken.getProperty("successURL");
                }

                if (redirect_url == null) {
                    ResultVal = rb.getString("authentication.already.login");
                }
                return;
            }
        } catch (Exception er){
            if (loginDebug.messageEnabled()) {
                loginDebug.message("SSOException : " + er.toString());
            }
            setErrorMessage(er);
            return;
        }
        
        if (isPost) {
            try {
                processLoginDisplay();
            } catch (Exception ep){
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("processLoginDisplay exception: ", ep);
                }
                clearCookieAndDestroySession();
                setErrorMessage(ep);
            }
        } else {
            try {                
                getLoginDisplay();
            } catch (Exception eg){
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("getLoginDisplay exception: ", eg);
                }
                clearCookieAndDestroySession();
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

    private void parseUserCredentials() {

        Enumeration keys = reqDataHash.keys();
    	
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            if (key.startsWith(TOKEN)) {
                if(credentials == null) {
                    tokenType = TOKEN;
                    credentials = new ArrayList();
                }
                try {
   	            credentials.add(new Integer(key.substring(TOKEN.length())));
                } catch (NumberFormatException nfe) {
                    if (loginDebug.messageEnabled()) {
                     loginDebug.message("Parsing error " +  nfe.getMessage());
                    }
                }
            }
        }
    	
        if (credentials == null) {
            keys = reqDataHash.keys();
            while (keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                if (key.startsWith(TOKEN_OLD)) {
                    if(credentials == null) {
                        tokenType = TOKEN_OLD;
                        credentials = new ArrayList();
                    }
                    try {
                        credentials.add(
                              new Integer(key.substring(TOKEN_OLD.length())));
                    } catch (NumberFormatException nfe) {
                        if (loginDebug.messageEnabled()) {
                           loginDebug.message("Parsing error " +  
                        		              nfe.getMessage());
                        }
                    }
                }
            }
        }
    	
        if (credentials != null) {
            Collections.sort(credentials);
        }
    }
    
    private void setOnePageLogin() {
        if (!bAuthLevel && SystemProperties.getAsBoolean(Constants.ZERO_PAGE_LOGIN_ENABLED)) {
            // Auth Level login will never do one page login.
            parseUserCredentials();

            if (credentials != null) {
                onePageLogin = true;
                userCredentials = new String[credentials.size()];
                
                for (int c = 0; c < userCredentials.length; c++) {
                    userCredentials[c] = XMLUtils.escapeSpecialCharacters(
							(String)reqDataHash.get(
                            tokenType + ((Integer)credentials.get(c)).toString()));
                }
            }
        }
    }
    
    protected void getLoginDisplay() throws Exception {
        loginDebug.message("In getLoginDisplay()");
        
        if (!bAuthLevel) {
            prepareLoginParams();
        }
        
        if (loginDebug.messageEnabled()) {
            loginDebug.message("Login Parameters : IndexType = " + indexType +
            " IndexName = " + indexName);
        }

        setOnePageLogin();
        
        try {
            if ( indexType != null ) {
                if (indexType.equals(AuthContext.IndexType.RESOURCE)) {
                     ac.login(indexType, indexName, userCredentials, envMap, request, response);
                } else {
                    ac.login(indexType, indexName, userCredentials,request,response);
                }
                session.setAttribute("IndexType", indexType.toString());
                session.setAttribute("IndexName", indexName);
            } else {
                ac.login(null,null,userCredentials,request,response);
            }
        } catch (AuthLoginException le) {
            loginDebug.message("AuthContext()::login error ", le);
            clearCookieAndDestroySession();
            LoginFail = true;
            setErrorMessage(le);
            ResultVal = ErrorMessage;
            // redirect to 'gotoOnFail' parameter or SPI hook
            // or default redirect URL.
            redirect_url = gotoOnFailUrl;
            if ((gotoOnFailUrl == null) || (gotoOnFailUrl.length() == 0) ||
                (gotoOnFailUrl.equalsIgnoreCase("null")) ) {
                redirect_url = ac.getFailureURL();
            }
            return;
        }
        
        try {
            // Get the information requested by the respective auth module
            if (ac.hasMoreRequirements(true)) {
                loginDebug.message("In getLoginDisplay, has More Requirements");
                callbacks = ac.getRequirements(true);
                session.setAttribute("LoginCallbacks", callbacks);
                addLoginCallbackMessage(callbacks);
                //AuthClientUtils.setCallbacksPerState(ac, pageState, callbacks);
            } else {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message(
                        "No more Requirements in getLoginDisplay");
                    loginDebug.message("Status is : " + ac.getStatus());
                }
                if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                    LoginSuccess = true;
                    ResultVal = rb.getString("authentication.successful");
                    
                    /*
                     * redirect to 'goto' parameter or SPI hook or default
                     * redirect URL.
                     */
                    redirect_url = gotoUrl;

                    if ((gotoUrl == null) || (gotoUrl.length() == 0) ||
                        (gotoUrl.equalsIgnoreCase("null"))
                    ) {
                        redirect_url = ac.getSuccessURL();
                    }

                    if (loginDebug.messageEnabled()) {
                        loginDebug.message(
                            "LoginSuccessURL in getLoginDisplay " +
                            "(in case of successful auth) : " + redirect_url);
                    }
                    ac.getSSOToken().setProperty
                        (ISAuthConstants.DISTAUTH_LOGINURL,loginURL);
                    session.invalidate();
                } else if (ac.getStatus() == AuthContext.Status.FAILED) {
                    LoginFail = true;
                    setErrorMessage(null);
                    ResultVal = ErrorMessage;
                    
                    /*
                     * redirect to 'goto' parameter or SPI hook or default
                     * redirect URL.
                     */
                    redirect_url = gotoOnFailUrl;
                    if ((gotoOnFailUrl == null) ||
                    (gotoOnFailUrl.length() == 0) ||
                    (gotoOnFailUrl.equalsIgnoreCase("null")) ) {
                        redirect_url = ac.getFailureURL();
                    }
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message(
                            "LoginFailedURL in getLoginDisplay : "
                        + redirect_url);
                    }
                    session.invalidate();
                } else if (ac.getStatus() == AuthContext.Status.RESET) {
                    LoginFail = true;
                    setErrorMessage(null);
                    ResultVal = ErrorMessage;

                    // AuthContext has been reset, send the client back to
                    // the login URL
                    if ((loginURL == null) ||
                        (loginURL.length() == 0) ||
                        (loginURL.equalsIgnoreCase("null")) ) {
                        redirect_url = getLoginURL();
                    } else {
                        redirect_url = loginURL;
                    }

                    if (loginDebug.messageEnabled()) {
                        loginDebug.message(
                            "LoginFailedURL in getLoginDisplay : "
                        + redirect_url);
                    }

                    session.invalidate();
                } else {
                    redirect_url = gotoOnFailUrl;
                    if ((gotoOnFailUrl == null) ||
                    (gotoOnFailUrl.length() == 0) ||
                    (gotoOnFailUrl.equalsIgnoreCase("null")) ) {
                        redirect_url = ac.getFailureURL();
                    }
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message(
                            "LoginFailedURL in getLoginDisplay : "
                        + redirect_url);
                    }
                    setErrorMessage(null);
                    ResultVal = "Unknown status: " + ac.getStatus() + "\n";
                    session.invalidate();
                }                
            }

            processRequestResponse(ac.getRemoteRequest(), ac.getRemoteResponse());

            if (loginDebug.messageEnabled()) {
                loginDebug.message("getLoginDisplay::getLoginDisplay=" + remoteRequestResponseProcessed);
            }
        } catch(Exception e){
            loginDebug.error("getLoginDisplay caught exception: ", e);
            setErrorMessage(e);
            throw new L10NMessageImpl(bundleName, "loginDisplay.get",
            new Object[]{e.getMessage()});
        }
    }
    
    protected void processLoginDisplay() throws Exception {
        loginDebug.message("In processLoginDisplay()");
        String tmp="";
        
        try {
            if (!onePageLogin){
                if (newRequest || session.isNew()) {
                    loginDebug.message(
                        "processLoginDisplay():New Session/Request");
                    getLoginDisplay();
                    return;
                }
                callbacks =
                (Callback[]) session.getAttribute("LoginCallbacks");
                indexType =
                AuthClientUtils.getIndexType((String) session.getAttribute("IndexType"));
                indexName = (String) session.getAttribute("IndexName");
            }
            /*
            String page_state = request.getParameter("page_state");
            if (loginDebug.messageEnabled()) {
                loginDebug.message("Submit with Page State : " + page_state);
            }
             
            if ((page_state != null) && (page_state.length() != 0)) {
                callbacks = AuthClientUtils.getCallbacksPerState(ac, page_state);
             
                //Get Callbacks in order to set the page state
                Callback[] callbacksForPageState = AuthClientUtils.getRecdCallback(ac);
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
                callbacks = AuthClientUtils.getRecdCallback(ac);
            }*/
            
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
                            loginDebug.message("No of tokens : "
                            + Integer.toString(cnt));
                        }
                        while (st.hasMoreTokens()) {
                            choice = st.nextToken();
                            if ((choice!=null) && (choice.length() != 0)) {
                                selected = Integer.parseInt(choice);
                                choice = choices[selected];
                                selectIndexs[j] = selected;
                                j++;
                                if (loginDebug.messageEnabled()) {
                                    loginDebug.message("selected  choice : "
                                        + choice + " & selected index : "
                                        + selected);
                                }
                            }
                        }
                        cc.setSelectedIndexes(selectIndexs);
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message("Selected indexes : "
                            + selectIndexs);
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
                    (ConfirmationCallback) callbacks[i];
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
                    PagePropertiesCallback ppc
                    = (PagePropertiesCallback) callbacks[i];
                } else if (callbacks[i] instanceof X509CertificateCallback) {
                    X509CertificateCallback xcc =
                        (X509CertificateCallback) callbacks[i];
                    X509Certificate[] allCerts = (X509Certificate[])
                        request.getAttribute
                            ("javax.servlet.request.X509Certificate");
                    if ((allCerts != null) && (allCerts.length > 0)) {
                        X509Certificate cert = (X509Certificate) allCerts[0];
                        xcc.setCertificate(cert);
                        if (loginDebug.messageEnabled()) {
                            try {
                                loginDebug.message(
                                    "X509CertificateCallback - " +
                                    "User Certificate : "
                                    + Base64.encode(cert.getEncoded()));
                            } catch (CertificateEncodingException e) {
                                loginDebug.message(
                                    "X509CertificateCallback - " +
                                    e.toString());
                            }
                        }
                    }
                } else if (callbacks[i] instanceof RedirectCallback) {
                    RedirectCallback rc = (RedirectCallback) callbacks[i];
                    String status =
                            request.getParameter(rc.getStatusParameter());
                    clearCookie(rc.getRedirectBackUrlCookieName());
                    loginDebug.message("Redirect callback : set status");
                    rc.setStatus(status);
                } else if (callbacks[i] instanceof HttpCallback) {
                    processHttpCallback((HttpCallback)callbacks[i]);
                }
            }
            
            // testing
            if (loginDebug.messageEnabled()) {
                loginDebug.message(
                    " length 0f callbacks : " + callbacks.length);
                loginDebug.message(" Index type : " + indexType
                + " Index name : " + indexName);
            }
            //testing
            
            if (((indexType == AuthContext.IndexType.LEVEL) ||
            (indexType == AuthContext.IndexType.COMPOSITE_ADVICE)) &&
            (choice.length() > 0)) {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("In processLoginDisplay(), Index type" +
                        " is Auth Level or Composite Advice and selected " 
                        + "module is : " + choice);
                }
                indexType = AuthContext.IndexType.MODULE_INSTANCE;
                indexName = choice;
                if (indexName == null) {
                    indexName="LDAP";
                } else {
                    indexName =
                    AuthClientUtils.getDataFromRealmQualifiedData(indexName);                	
                }
                
                bAuthLevel = true;
                if ( indexName != null ) {
                    if (indexName.equalsIgnoreCase("Application")) {
                        onePageLogin = true;
                    }
                }
                getLoginDisplay();
            } else {
                // Submit the information to auth module
                ac.submitRequirements(callbacks, request, response);
                
                // Check if more information is required
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("before hasMoreRequirements: Status is: "
                    + ac.getStatus());
                }
                if (ac.hasMoreRequirements(true)) {
                    loginDebug.message("Has more requirements after Submit ");
                    callbacks = ac.getRequirements(true);
                    session.setAttribute("LoginCallbacks", callbacks);
                    addLoginCallbackMessage(callbacks);
                    //AuthClientUtils.setCallbacksPerState(ac, pageState, callbacks);
                } else {
                    if (loginDebug.messageEnabled()) {
                        loginDebug.message("No more Requirements : Status is : "
                        + ac.getStatus());
                    }
                    if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                        LoginSuccess = true;
                        ResultVal = rb.getString("authentication.successful");
                        
                        // redirect to 'goto' parameter or SPI hook or default
                        // redirect URL.
                        redirect_url = gotoUrl;

                        SSOTokenManager.getInstance().refreshSession(ac.getSSOToken());
                        String successURLFromSession = ac.getSSOToken().getProperty(ISAuthConstants.POST_PROCESS_SUCCESS_URL);

                        if (successURLFromSession != null) {
                            redirect_url = successURLFromSession;
                        }

                        if ((gotoUrl == null) || (gotoUrl.length() == 0) ||
                        (gotoUrl.equalsIgnoreCase("null")) ) {
                            redirect_url = ac.getSuccessURL();
                        }
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message("LoginSuccessURL (in case of " +
                            " successful auth) : " + redirect_url);
                        }
                        ac.getSSOToken().setProperty
                            (ISAuthConstants.DISTAUTH_LOGINURL,loginURL);
			cookieTimeToLiveEnabled = isCookieTimeToLiveEnabled();
			cookieTimeToLive = getCookieTimeToLive();
                        session.invalidate();
                    } else if (ac.getStatus() == AuthContext.Status.FAILED) {
                        LoginFail = true;
                        setErrorMessage(null);
                        ResultVal = ErrorMessage;
                        
                        // redirect to 'goto' parameter or SPI hook or
                        // default redirect URL.
                        redirect_url = gotoOnFailUrl;
                        if ((gotoOnFailUrl == null) ||
                        (gotoOnFailUrl.length() == 0) ||
                        (gotoOnFailUrl.equalsIgnoreCase("null")) ) {
                            redirect_url = ac.getFailureURL();
                        }
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message("LoginFailedURL : "
                            + redirect_url);
                        }
                        session.invalidate();
                    } else {
                        redirect_url = gotoOnFailUrl;
                        if ((gotoOnFailUrl == null) ||
                        (gotoOnFailUrl.length() == 0) ||
                        (gotoOnFailUrl.equalsIgnoreCase("null")) ) {
                            redirect_url = ac.getFailureURL();
                        }
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message(
                                "LoginFailedURL in getLoginDisplay : "
                                + redirect_url);
                        }
                        ResultVal = "Unknown status: " + ac.getStatus() + "\n";
                        session.invalidate();
                    }                    
                }
            }

            if (loginDebug.messageEnabled()) {
                loginDebug.message("processLoginDisplay::processRequestResponse=" + remoteRequestResponseProcessed);
            }
            
            processRequestResponse(ac.getRemoteRequest(), ac.getRemoteResponse());
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
                    (PagePropertiesCallback) callbacks[i];
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

                if (callbacks.length == 1) {
                    onePageLogin = true;
                    processLoginDisplay();
                    break; 
                }
            } else if (callbacks[i] instanceof X509CertificateCallback) {
                onePageLogin = true;
                processLoginDisplay();
                break;
            } else if (callbacks[i] instanceof RedirectCallback) {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("addLoginCallbackMessage::processRequestResponse=" + remoteRequestResponseProcessed);
                }

                processRequestResponse(ac.getRemoteRequest(), ac.getRemoteResponse());
                processRedirectCallback((RedirectCallback)callbacks[i]);
            } else if (callbacks[i] instanceof HttpCallback) {
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("addLoginCallbackMessage::processRequestResponse=" + remoteRequestResponseProcessed);
                }

                processRequestResponse(ac.getRemoteRequest(), ac.getRemoteResponse());
                processHttpCallback((HttpCallback)callbacks[i]);
            }
        }
        
        return;
    }

    /**
     * Processes the request and response objects, primarily the response
     * object into the local HttpServletResponse
     *
     * @param req The incoming remote request
     * @param res The incoming remote response
     */
    protected void processRequestResponse(HttpServletRequest req, HttpServletResponse res) {
        if (remoteRequestResponseProcessed) {
            return;
        }

        RemoteHttpServletRequest remoteRequest = (RemoteHttpServletRequest) req;
        RemoteHttpServletResponse remoteResponse = (RemoteHttpServletResponse) res;

        loginDebug.message("req is " + remoteRequest);
        loginDebug.message("res is " + remoteResponse);
                
        if (remoteRequest != null) {
            // TODO should really worry the attributes
        }
        
        if (remoteResponse != null) {
            Set cookies = remoteResponse.getCookies();
            
            if (loginDebug.messageEnabled()) {
                loginDebug.message("cookies" + cookies);
            }
            
            Iterator it = cookies.iterator();
        
            while (it.hasNext()) {
                RemoteCookie remoteCookie = (RemoteCookie) it.next();
                response.addCookie(remoteCookie.getCookie());
                
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("Added cookie " + remoteCookie.getCookie().getName());
                }
            }
            
            Map headers = remoteResponse.getHeaders();
            Set keys = headers.keySet();
            
            it = keys.iterator();
            
            while (it.hasNext()) {
                String name = (String) it.next();
                Object obj = headers.get(name);
                
                if (obj instanceof String) {
                    response.addHeader(name, (String) obj);
                } else if (obj instanceof Integer) {
                    response.addIntHeader(name, ((Integer) obj).intValue());
                }
            }
            
            Map dateHeaders = remoteResponse.getDateHeaders();
            keys = dateHeaders.keySet();
            
            it = keys.iterator();
            
            while (it.hasNext()) {
                String name = (String) it.next();
                Long date = (Long) headers.get(name);
       
                response.addDateHeader(name, date.longValue());
            }

            // process only once per request
            remoteRequestResponseProcessed = true;
        }
    }

    /**
     * Returns tile Index.
     *
     * @return Tile Index;
     */
    public  String getTileIndex() {
        CallBackTiledView tView =
        (CallBackTiledView) getChild(TILED_CALLBACKS);
        String str =
        (String)tView.getDisplayFieldValue(CallBackTiledView.TXT_INDEX);
        return str;
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
                indexName = AuthClientUtils.getBase64DecodedValue(reqModule);
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
            indexName = URLEncDec.decode(indexName);            
        } else if (((reqDataHash.get(ISAuthConstants.IP_RESOURCE_ENV_PARAM)
            != null) && "true".equalsIgnoreCase((String) reqDataHash.get(
                 ISAuthConstants.IP_RESOURCE_ENV_PARAM)))||
            ((reqDataHash.get(ISAuthConstants.IP_RESOURCE_ENV_PARAM) == null) &&
              (reqDataHash.get(ISAuthConstants.RESOURCE_URL_PARAM) != null))) {
            indexType = AuthContext.IndexType.RESOURCE;
            indexName = AuthClientUtils.getResourceURL(request);
            envMap = AuthClientUtils.getEnvMap(request);
        }
    }

    // Process 'HttpCallback' initiated by Authentication module
    private void processHttpCallback(HttpCallback hc) throws Exception{
        String auth = null;
        if (hc.isForHTTPBasic()) {
            auth = request.getHeader(hc.getAuthorizationHeader());
        } else if (hc.isForWindowsDesktopSSO()) {
            auth = request.getHeader("Authorization");
            if ((auth != null) && auth.startsWith("Negotiate")) {
                auth = auth.substring("Negotiate".length()).trim();
            }
        }

        if (auth != null && auth.length() != 0) {
            if (loginDebug.messageEnabled()){
                loginDebug.message("Found authorization header.");
            }
            onePageLogin = true;
            if (hc.getAuthorization() == null) {
                hc.setAuthorization(auth);
                processLoginDisplay();
            }
        } else {
            if (loginDebug.messageEnabled()){
                loginDebug.message("Start authorization negotiation...");
                loginDebug.message("header: " + hc.getNegotiationHeaderName() +
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
        forward = false;
        redirectCallbackHandler.handleRedirectCallback(request, response, rc, loginURL);
    }

    // Method to check if this is Session Upgrade
    private boolean checkNewOrg(SSOToken ssoToken) {
        loginDebug.message("Check New Organization!");
        checkNewOrg = false;
        dontLogIntoDiffOrg = false;
        
        try {
            // always make sure the orgName is the same
            String orgName = ssoToken.getProperty("Organization");
            String orgParam = AuthClientUtils.getOrgParam(reqDataHash);
            String queryOrg = (orgParam != null)? orgParam : "/";
            String newOrgName = AuthClientUtils.getDomainNameByRequest(request,reqDataHash);
            if (loginDebug.messageEnabled()) {
                loginDebug.message("original org is : " + orgName);
                loginDebug.message("query org is : " + queryOrg);
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
                    java.util.Locale locale =
                    com.sun.identity.shared.locale.Locale.getLocale(
                        ssoToken.getProperty("Locale"));
                    rb =  rbCache.getResBundle(bundleName, locale);
                    
                    if (strButton.trim().equals(
                            rb.getString("Yes").trim())) {
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message("Submit with YES." +
                            "Destroy session.");
                        }
                        param = queryOrg;
                        clearCookie();
                        manager.destroyToken(ssoToken);
                    } else if ((strButton.trim().equals(
                            rb.getString("No").trim()))) {
                        if (loginDebug.messageEnabled()) {
                            loginDebug.message("Submit with NO." + 
                            "Don't destroy session.");
                        }
                        dontLogIntoDiffOrg = true;
                        return checkNewOrg;
                    }
                } else {
                    newOrg = true;
                    param = queryOrg;
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
        loginDebug.message("Set AM or AMAuth cookie");
        String cookieDomain = null;
        Set cookieDomainSet = AuthClientUtils.getCookieDomains();

        // No cookie domain specified in profile
        if (cookieDomainSet.isEmpty()) {
            try {
                cookie = AuthClientUtils.getCookieString(ac, null);
		if (cookieTimeToLiveEnabled) {
		    if (cookieTimeToLive > 0 && ac.getStatus() == AuthContext.Status.SUCCESS) {
			if (loginDebug.messageEnabled()) {
			    loginDebug.message("LoginViewBean.setCookie():"
				    + "set cookie maxAge=" + cookieTimeToLive);
			}
			cookie.setMaxAge(cookieTimeToLive);
		    }
		}
                CookieUtils.addCookieToResponse(response, cookie);
            } catch (Exception e) {
                loginDebug.message("Cound not set AM or AMAuth Cookie!");
            }
        } else {
            if (loginDebug.messageEnabled()) {
                if (cookieTimeToLiveEnabled && ac.getStatus() == AuthContext.Status.SUCCESS) {
                    if (cookieTimeToLive > 0) {
                        loginDebug.message("LoginViewBean.setCookie():"
                                + "would set cookie maxAge=" + cookieTimeToLive);
                    }
                }
            }
            Iterator iter = cookieDomainSet.iterator();
            while (iter.hasNext()) {
                cookieDomain = (String)iter.next();
                cookie = AuthClientUtils.getCookieString(ac, cookieDomain);
                if (cookieTimeToLiveEnabled && ac.getStatus() == AuthContext.Status.SUCCESS) {
                    if (cookieTimeToLive > 0) {
                        cookie.setMaxAge(cookieTimeToLive);
                    }
                }
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("cookie for new request : " + cookie);
                }
                CookieUtils.addCookieToResponse(response, cookie);
            }
        }
        if (ac.getStatus() == AuthContext.Status.SUCCESS) {
            AuthClientUtils.setHostUrlCookie(response);
        }
    }

    // Method to set DSAME cookie
    private void setServerCookies() {
        Map serverCookieMap = ac.getCookieTable();
        try {
            if (serverCookieMap != null && !serverCookieMap.isEmpty()) {
                for (Iterator it = serverCookieMap.values().iterator();
                    it.hasNext();){
                    Cookie cookie = (Cookie)it.next();
                    if (!cookie.getName().equals("JSESSIONID")){
                        AuthClientUtils.setServerCookie(cookie, request,
                                response);
                        String cookieName = cookie.getName();
                        if (!storeCookies.contains(cookieName)) {
                            storeCookies.add(cookieName);
                        }
                    }
                }
            }
         } catch (Exception exp) {
             loginDebug.message("could not set server Cookies");
         }
    }
  
    /** Method to clear AM Cookie
     */
    private void clearCookie() {
        if (cookieSupported) {
            clearCookie(AuthClientUtils.getCookieName());
            AuthClientUtils.clearHostUrlCookie(response);
            AuthClientUtils.clearlbCookie(request, response);
            if (storeCookies != null && !storeCookies.isEmpty()) {
                for (Iterator it = storeCookies.iterator();
                    it.hasNext();){
                    String cookieName = (String)it.next();
                    AuthClientUtils.clearServerCookie(cookieName, request,
                            response);
                }
            }
        }
    }
    
    /** Method to clear cookie based on the cookie
     *  name passed (Auth or AM Cookie)
     *  @param cookieName name of cookie to be cleared.
     */
    private void clearCookie(String cookieName) {
        String cookieDomain = null;
        Set cookieDomainSet = AuthClientUtils.getCookieDomains();

        // No cookie domain specified in profile
        if (cookieDomainSet.isEmpty()) {
            try {
                cookie = AuthClientUtils.createCookie(cookieName,LOGOUTCOOKIEVAULE, null);
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            } catch (Exception e) {
                loginDebug.message("Cound not set DSAME Cookie!");
            }
        } else {
            Iterator iter = cookieDomainSet.iterator();
            while (iter.hasNext()) {
                cookieDomain = (String)iter.next();
                cookie = AuthClientUtils.createCookie(cookieName,LOGOUTCOOKIEVAULE,
                cookieDomain);
                cookie.setMaxAge(0); // tell browser to expire DSAME Cookie
                response.addCookie(cookie);
            }
        }
    }
    
    private void setlbCookie(){
        try {
            AuthClientUtils.setlbCookie(request, response);
        } catch (Exception e) {
            loginDebug.message("Cound not set LB Cookie!");
        }
    }
    
    // get error template, message as well as error code.
    private void setErrorMessage(Exception e) {
        String authErrorCode = null;
        
        if ((e != null) && (e instanceof L10NMessage)) {
            L10NMessage l10nE = (L10NMessage) e;
            authErrorCode = l10nE.getErrorCode();
            // in case this AuthLoginException is only a wrapper of
            // LoginException which does not have errorCode.
            if (authErrorCode != null) {
                errorCode = authErrorCode;
                ErrorMessage = l10nE.getL10NMessage(locale);
            }
        }

        if (authErrorCode == null) {
            // if error code can not be got from exception,
            // then get error code and message from auth context
            if (ac != null) {
                ErrorMessage = ac.getErrorMessage();
                errorCode = ac.getErrorCode();
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
            ErrorMessage = AuthClientUtils.getErrorMessage(errorCode);
        }

        if (ac != null) {
            errorTemplate = ac.getErrorTemplate();
        } else {
            errorTemplate = AuthClientUtils.getErrorTemplate(errorCode);
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
            relativeFileName = AuthClientUtils.getFileName(
                fileName, locale.toString(), orgName, request, servletContext,
                indexType, indexName);
        } else {
            relativeFileName =
            AuthClientUtils.getDefaultFileName(request,fileName,locale,servletContext);
        }
        if (loginDebug.messageEnabled()) {
            loginDebug.message("fileName is : " + fileName);
            loginDebug.message("relativeFileName is : " + relativeFileName);
        }
        
        return relativeFileName;
    }
    
    // Get all the Original Auth credentials to start a new request
    private boolean canGetOrigCredentials(SSOToken ssoToken) {
        loginDebug.message("BACK re-submit with valid session");
        boolean gotOrigCredentials = false;
        try {
            String tmpLoginURL = ssoToken.getProperty("loginURL");

            if (tmpLoginURL != null) {
                loginURL = tmpLoginURL;
            }

            orgName = ssoToken.getProperty("Organization");
            indexType = AuthClientUtils.getIndexType(ssoToken.getProperty("IndexType"));
            indexName = AuthClientUtils.getIndexName(ssoToken,indexType);
            gotOrigCredentials = true;
        } catch (Exception e){
            loginDebug.message("Error in canGetOrigCredentials");
        }
        if (loginDebug.messageEnabled()) {
            loginDebug.message("canGetOrigCredentials : loginURL = "
            + loginURL);
            loginDebug.message("canGetOrigCredentials : orgName = "
            + orgName);
            loginDebug.message("canGetOrigCredentials : IndexType = "
            + indexType);
            loginDebug.message("canGetOrigCredentials : IndexName = "
            + indexName);
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
    
    /**
     * Clear cookie and destroy session
     */
    private void clearCookieAndDestroySession() {
        // clear cookie, destroy orignal invalid session
        if (cookieSupported) {
            clearCookie(AuthClientUtils.getAuthCookieName());
        }
    }
    
    // Checks whether the subsequent request invokation is for the same Org as 
    // the previous request
    private boolean isOrgSame() {
    	if (checkNewOrg && isPost) {
            return false;
        } else {
            return true;
        }
    }
    
    // Checks whether the subsequent request invokation is for the same Org as 
    // the previous request
    private boolean isOrgChanged() {    
        // this method will only be used when AuthCookie is not null
        String tmpOrgName = "";
        if (session != null) {
            tmpOrgName = (String) session.getAttribute("OrgName");
        }
        if (tmpOrgName != null && !tmpOrgName.equals(orgName) && !isPost) {
            return true;
        } else {
            return false;
        }
    }    
    
    // Returns the new LoginURL from the new request or 
    // from existing HttpSession 
    private String getLoginURL() {
        String tmpLoginURL = "";
        if (session != null) {
            tmpLoginURL = (String) session.getAttribute("LoginURL");
        }
        String qString = request.getQueryString();
        if ((qString != null) && (qString.length() != 0) && !isPost) {
            if (tmpLoginURL != null && !tmpLoginURL.equals(loginURL)) {
                tmpLoginURL = loginURL;
                session.setAttribute("LoginURL", tmpLoginURL);
            }
        }
        return tmpLoginURL;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Display cycle events:
    // If the fireDisplayEvents attribute in a display field tag is set to true,
    // then the begin/endDisplay events will fire for that display field.
    ////////////////////////////////////////////////////////////////////////////
    
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
        return (((ResultVal != null) && ( ResultVal.length() > 0)
        && LoginFail && !accountLocked)
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

   public void enableCookieTimeToLive() {
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
           cookieTimeToLiveEnabled = true;
           if (loginDebug.messageEnabled()) {
               loginDebug.message("LoginViewBean.enableCookieTimeToLive():"
                   + "cookieTimeToLive " + cookieTimeToLive + "s, enabled");
           }
           session.setAttribute("distAuth.cookieTimeToLive",
                   new Integer(cookieTimeToLive));
           session.setAttribute("distAuth.cookieTimeToLiveEnabled",
                   Boolean.TRUE);
       } else {
           if (loginDebug.messageEnabled()) {
               loginDebug.message("LoginViewBean.enableCookieTimeToLive():"
                   + "cookieTimeToLive not enabled");
           }
       }
   }
   
   public int getCookieTimeToLive() {
       return session.getAttribute("distAuth.cookieTimeToLive") != null ?
               ((Integer)(session.getAttribute(
                       "distAuth.cookieTimeToLive"))).intValue()
               : 0;
   }
   
   public boolean isCookieTimeToLiveEnabled() {
       return session.getAttribute("distAuth.cookieTimeToLiveEnabled") != null
           ? ((Boolean)(session.getAttribute(
                   "distAuth.cookieTimeToLiveEnabled"))).booleanValue()
           : false;
   }

    ////////////////////////////////////////////////////////////////////////////
    // Class variables
    ////////////////////////////////////////////////////////////////////////////
    boolean checkNewOrg = false;
    boolean dontLogIntoDiffOrg = false;
    /** Page name for login */
    public static final String PAGE_NAME="Login";
    /** Page name for session time out */
    public static final String JSP_FILE_NAME="session_timeout.jsp";
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
    private boolean cookieSupported = true;
    AuthContext ac = null;
    SSOToken ssoToken = null;
    boolean newRequest = false;
    private Hashtable reqDataHash = new Hashtable();
    private static String LOGINURL = "";
    private String loginURL = "";
    private static final String LOGOUTCOOKIEVAULE = "LOGOUT";
    private static final String bundleName = "amAuthUI";
    private boolean onePageLogin = false;
    private boolean sessionUpgrade = false;
    private boolean forward = true;
    private boolean newOrg = false;
    private boolean bHttpBasic = false;
    HttpServletRequest request;
    HttpServletResponse response;
    HttpSession session;
    ServletContext servletContext;
    Cookie cookie;
    static Debug loginDebug = Debug.getInstance("amLoginViewBean");
    private final RedirectCallbackHandler redirectCallbackHandler = new RedirectCallbackHandler();
    String client_type = "";
    String orgName = "";
    String orgQueryName = "";
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
    String timeout_jsp_page=null;
    String param=null;
    SSOTokenManager manager = null;
    java.util.Locale locale;
    /** Goto url */
    public String gotoUrl = "";
    /** Goto url for login failure */
    public String gotoOnFailUrl = "";
    ArrayList credentials = null;
    String tokenType = null;
    public String[] userCredentials = null; 
    private boolean cookieTimeToLiveEnabled = false;
    private int cookieTimeToLive = 0;
    private boolean remoteRequestResponseProcessed = false;
    
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
    /** Default parameter name for command new user */
    public static final String CMD_NEW_USER = "cmdNewUser";
    /** Default parameter name for label new user */
    public static final String LBL_NEW_USER = "lblNewUser";
    /** Default parameter name for label reset */
    public static final String LBL_RESET = "lblReset";
    
    /** Default parameter name for login html title */
    public static final String HTML_TITLE_LOGIN = "htmlTitle_Login";
    /** Default parameter name for login title message */
    public static final String HTML_TITLE_MESSAGE = "htmlTitle_Message";
    /** Default parameter name for redirect html title */
    public static final String HTML_TITLE_REDIRECT = "htmlTitle_Redirect";
    /** Default parameter name of html title for account expired */
    public static final String HTML_TITLE_ACCOUNTEXPIRED
    = "htmlTitle_AccountExpired";
    /** Default parameter name of html title for auth error */
    public static final String HTML_TITLE_AUTHERROR = "htmlTitle_AuthError";
    /** Default parameter name of html title for self registration error */
    public static final String HTML_TITLE_SELFREGERROR
    = "htmlTitle_SelfRegError";
    /** Default parameter name of html title for disclaimer */
    public static final String HTML_TITLE_DISCLAIMER = "htmlTitle_Disclaimer";
    /** Default parameter name of html title for invalid cookie id */
    public static final String HTML_TITLE_INVALIDPCOOKIEUID
    = "htmlTitle_InvalidPCookieUID";
    /** Default parameter name of html title for invalid password */
    public static final String HTML_TITLE_INVALIDPASSWORD
    = "htmlTitle_InvalidPassword";
    /** Default parameter name of html title for invalid domain */
    public static final String HTML_TITLE_INVALIDDOMAIN
    = "htmlTitle_InvalidDomain";
    /** Default parameter name of html title for user profile not found */
    public static final String HTML_TITLE_USERPROFILENOTFOUND
    = "htmlTitle_UserProfileNotFound";
    /** Default parameter name of html title for auth failure */
    public static final String HTML_TITLE_AUTHFAILED = "htmlTitle_AuthFailed";
    /** Default parameter name of html title for membership */
    public static final String HTML_TITLE_MEMBERSHIP = "htmlTitle_Membership";
    /** Default parameter name of html title for auth module denied */
    public static final String HTML_TITLE_AUTHMODULEDENIED
    = "htmlTitle_AuthModuleDenied";
    /** Default parameter name of html title for no config error */
    public static final String HTML_TITLE_NOCONFIGERROR
    = "htmlTitle_NoConfigError";
    /** Default parameter name of html title for org inactive */
    public static final String HTML_TITLE_ORGINACTIVE = "htmlTitle_OrgInactive";
    /** Default parameter name of html title for self module registration */
    public static final String HTML_TITLE_SELFREGMODULE
    = "htmlTitle_SelfRegModule";
    /** Default parameter name of html title for session timeout */
    public static final String HTML_TITLE_SESSIONTIMEOUT
    = "htmlTitle_SessionTimeOut";
    /** Default parameter name of html title for user not found */
    public static final String HTML_TITLE_USERNOTFOUND
    = "htmlTitle_UserNotFound";
    /** Default parameter name of html title for user inactive */
    public static final String HTML_TITLE_USERINACTIVE
    = "htmlTitle_UserInactive";
    /** Default parameter name of html title for new organization */
    public static final String HTML_TITLE_NEWORG = "htmlTitle_NewOrg";
    /** Default parameter name of html title for max session */
    public static final String HTML_TITLE_MAXSESSIONS = "htmlTitle_MaxSessions";
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance variables
    ////////////////////////////////////////////////////////////////////////////
    
    static {
        LOGINURL = serviceUri + "/UI/Login";
    }
}

