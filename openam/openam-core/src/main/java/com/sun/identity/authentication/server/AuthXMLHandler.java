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
 * $Id: AuthXMLHandler.java,v 1.20 2009/04/29 18:07:03 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock, Inc.
 */
package com.sun.identity.authentication.server;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.server.RequestHandler;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.comm.share.ResponseSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.L10NMessage;
import org.forgerock.openam.utils.ClientUtils;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * <code>AuthXMLHandler</code> class implements the <code>RequestHandler</code>.
 * It processes the authentication request from remote client which 
 * comes in as XML document
 */
public class AuthXMLHandler implements RequestHandler {
    private String localAuthServerProtocol ;
    private String localAuthServer;
    private String localAuthServerPort;
    private Locale locale;
    
    static Debug debug;
    private static String serviceURI;
    private static boolean messageEnabled = false;
    private boolean security = false;
    
    static {
        debug = com.sun.identity.shared.debug.Debug.getInstance("amXMLHandler");
        messageEnabled = debug.messageEnabled();
        serviceURI= SystemProperties.get(Constants.
            AM_SERVICES_DEPLOYMENT_DESCRIPTOR)+"/authservice";
    }
    
    /**
     * Creates <code>AuthXMLHandler</code> object
     */
    public AuthXMLHandler() {
        localAuthServerProtocol
            = SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
        localAuthServer = SystemProperties.get(Constants.AM_SERVER_HOST);
        localAuthServerPort = SystemProperties.get(Constants.AM_SERVER_PORT);
        
        AuthContext.localAuthServiceID = localAuthServerProtocol + "://"
            + localAuthServer + ":" + localAuthServerPort;
        locale = (new ISLocaleContext()).getLocale();
    }
    
    
    /**
     * process the request and return the response
     * @param requests Vector of
     *     <code>com.iplanet.services.comm.server.RequestHandler</code> objects.
     * @param servletRequest <code>HttpServletRequest</code>object for 
     *      this request.
     * @param servletResponse <code>HttpServletResponse</code> object for this
     *      request.
     * @param servletContext <code>servletContext</code> object for this request
     * @return <code>ResponseSet</code> object for the processed request.
     */
    public ResponseSet process(
        List<Request> requests,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse,
        ServletContext servletContext) {
        ResponseSet rset = new ResponseSet(AuthXMLTags.AUTH_SERVICE);
        for (Request req : requests) {
            Response res = processRequest(req,servletRequest, servletResponse);
            rset.addResponse(res);
        }
        return rset;
    }
    
    /* process the request */
    private Response processRequest(Request req,
        HttpServletRequest servletReq, HttpServletResponse servletRes) {
        
        // this call is to create a http session so that the JSESSIONID cookie
        // is created. The appserver(8.1) load balancer plugin relies on the
        // JSESSIONID cookie to set its JROUTE sticky cookie.
        debug.message("=======================Entering processRequest");
        servletReq.getSession(true);
        
        String content  = req.getContent();
        AuthXMLResponse authResponse = null;

        // Check for mis-routed requests
        String cookieURL = null;
        int index = content.indexOf(AuthXMLTags.AUTH_ID_HANDLE);
        if (index != -1) {
            // Check for mis-routed requests, get server URL for
            // AuthIdentifier
            int beginIndex = content.indexOf('"', index);
            int endIndex = content.indexOf('"', beginIndex+1);
            String authIdentifier = content.substring(beginIndex+1, endIndex);
            if (debug.messageEnabled()) {
                debug.message("authIdentifier = " + authIdentifier+
                "beginIndex = "+beginIndex+"endIndex ="+endIndex);
            }
            if (!authIdentifier.equals("0")) {
                try {
                    SessionID sessionID = new SessionID(authIdentifier);
                    URL sessionServerURL = Session.getSessionServiceURL(
                        sessionID);
                    StringBuilder srtBuff = new StringBuilder(100);
                    srtBuff.append(sessionServerURL.getProtocol()).append("://")
                        .append(sessionServerURL.getHost()).append(":")
                        .append(Integer.toString(sessionServerURL.getPort()))
                        .append(serviceURI);
                    cookieURL = srtBuff.toString();
                } catch (Exception exp) {
                    debug.error("Error in getting URL from session", exp);
                    cookieURL = null;
                }
            }
        }

        if ((cookieURL != null) && (cookieURL.trim().length() != 0) && 
            !(AuthUtils.isLocalServer(cookieURL,serviceURI))) {
            // Routing to the correct server, the looks like a mis-routed 
            // requested.
            HashMap cookieTable = new HashMap();
            Map headers = new HashMap();
            Enumeration headerNames = servletReq.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String)headerNames.nextElement();
                List headerValues = new ArrayList();
                Enumeration enum1 = servletReq.getHeaders(headerName);
                while (enum1.hasMoreElements()) {
                    headerValues.add(enum1.nextElement());
                }
                headers.put(headerName,headerValues);
            }
            if (debug.messageEnabled()) {
                debug.message("Headers: " + headers);
            }
            PLLClient.parseCookies(headers,cookieTable);
            if (debug.messageEnabled()) {
                debug.message("Cookies: " + cookieTable);
            }
            RequestSet set = new RequestSet(AuthXMLTags.AUTH_SERVICE);
            set.addRequest(req);
            try {
                Vector responses = PLLClient.send(new URL(cookieURL), set, 
                    cookieTable);
                if (!responses.isEmpty()) {
                    debug.message("=====================Returning redirected");
                    return ((Response) responses.elementAt(0));
                }
            } catch (Exception e) {
                debug.error("Error in misrouted ", e);
                // Attempt to contact server failed
                authResponse = new AuthXMLResponse(AuthXMLRequest.
                    NewAuthContext);
                setErrorCode(authResponse, e);
                return new Response(authResponse.toXMLString());
            }
        }

        // Either local request or new request, handle it locally
        try {
            AuthXMLRequest sreq = AuthXMLRequest.parseXML(content, servletReq);
            sreq.setHttpServletRequest(servletReq);
            authResponse = processAuthXMLRequest(content, sreq, servletReq, servletRes);
        } catch (AuthException e) {
            debug.error("Got Auth Exception", e);
            authResponse = new AuthXMLResponse(AuthXMLRequest.NewAuthContext);
            authResponse.setErrorCode(e.getErrorCode());
        } catch (Exception ex) {
            debug.error("Error while processing xml request",ex);
            authResponse = new AuthXMLResponse(AuthXMLRequest.NewAuthContext);
            setErrorCode(authResponse, ex);
        }
        debug.message("=======================Returning");
        return new Response(authResponse.toXMLString());
    }
    
    /*
     * Process the XMLRequest
     */
    private AuthXMLResponse processAuthXMLRequest(
        String xml,
        AuthXMLRequest authXMLRequest,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse) {
        if (messageEnabled) {
            debug.message("authXMLRequest is : " + authXMLRequest);
        }
        int requestType = authXMLRequest.getRequestType();
        String sessionID = authXMLRequest.getAuthIdentifier();
        String orgName = authXMLRequest.getOrgName();
        AuthContextLocal authContext = authXMLRequest.getAuthContext();
        LoginState loginState = AuthUtils.getLoginState(authContext);
        String params = authXMLRequest.getParams();
        List envList = authXMLRequest.getEnvironment();
        Map envMap = toEnvMap(envList);
        AuthXMLResponse authResponse = new AuthXMLResponse(requestType);
        authResponse.setAuthContext(authContext);
        authResponse.setAuthIdentifier(sessionID);
        if (messageEnabled) {
            debug.message("authContext is : " + authContext);
            debug.message("requestType : " + requestType);
        }
        if (authXMLRequest.getValidSessionNoUpgrade()) {
            authResponse.setAuthXMLRequest(authXMLRequest);
            authResponse.setValidSessionNoUpgrade(true);
            return authResponse;
        }
        String securityEnabled =  null;
        try {
            securityEnabled =  AuthUtils.getRemoteSecurityEnabled();
        } catch (AuthException auExp) {
            debug.error("Got Exception", auExp);
            setErrorCode(authResponse, auExp);
            return authResponse;
        }
        if (debug.messageEnabled()) {
            debug.message("Security Enabled = " + securityEnabled);
        }

        if (requestType != 0) {
            if ((securityEnabled != null) && (securityEnabled.equals("true"))) {
                security = true;
                String indexNameLoc =  authXMLRequest.getIndexName();
                AuthContext.IndexType indexTypeLoc =  authXMLRequest.getIndexType();
                if (indexTypeLoc == null) {
                    indexTypeLoc = AuthUtils.getIndexType(authContext);
                    indexNameLoc =   AuthUtils.getIndexName(authContext);
                }
                if (debug.messageEnabled()) {
                    debug.message("Index Name Local : " + indexNameLoc);
                    debug.message("Index Type Local : " + indexTypeLoc);
                }
                if (((indexTypeLoc == null) || (indexNameLoc == null)) ||
                    !((indexTypeLoc == AuthContext.IndexType.MODULE_INSTANCE) &&
                    indexNameLoc.equals("Application"))) {
                    try {
                        String ssoTokenID = authXMLRequest.getAppSSOTokenID();

                        if (debug.messageEnabled()) {
                            debug.message("Session ID = : " + ssoTokenID);
                        }

                        SSOTokenManager manager = SSOTokenManager.getInstance();
                        SSOToken appSSOToken = manager.createSSOToken(ssoTokenID);

                        // if the token isn't valid, let the client know so they
                        // retry
                        if (!manager.isValidToken(appSSOToken)) {
                            if (debug.messageEnabled()) {
                                debug.message("App SSOToken is not valid");
                            }

                            setErrorCode(authResponse, new AuthException(
                                AMAuthErrorCode.REMOTE_AUTH_INVALID_SSO_TOKEN, null));
                            return authResponse;
                        } else {
                            debug.message("App SSOToken is VALID");
                        }
                    } catch (SSOException ssoe) {
                        // token is unknown to OpenAM, let the client know so they
                        // can retry
                        if (debug.messageEnabled()) {
                            debug.message("App SSOToken is not valid: " + ssoe.getMessage());
                        }

                        setErrorCode(authResponse, new AuthException(
                            AMAuthErrorCode.REMOTE_AUTH_INVALID_SSO_TOKEN, null));
                        return authResponse;
                    } catch (Exception exp) {
                        debug.error("Got Exception", exp);
                        setErrorCode(authResponse, exp);
                        return authResponse;
                    }
                }
            }
        } else {
            security = false;
        }

        // if index type is level and choice callback has a
        // selected choice then start module based authentication.
        if ((AuthUtils.getIndexType(authContext) == AuthContext.IndexType.LEVEL) ||
            (AuthUtils.getIndexType(authContext) ==
                AuthContext.IndexType.COMPOSITE_ADVICE)
        ){
            Callback[] callbacks = authXMLRequest.getSubmittedCallbacks();
            if (messageEnabled) {
                debug.message("Callbacks are  : "+ callbacks);
            }
            if (callbacks != null) {
                if (messageEnabled) {
                    debug.message("Callback length is : " + callbacks.length);
                }
                
                if (callbacks[0] instanceof ChoiceCallback) {
                    ChoiceCallback cc = (ChoiceCallback) callbacks[0];
                    int[] selectedIndexes = cc.getSelectedIndexes();
                    int selected = selectedIndexes[0];
                    String[] choices = cc.getChoices();
                    String indexName = choices[selected];
                    if (messageEnabled) {
                        debug.message("Selected Index is : " + indexName);
                    }
                    authXMLRequest.setIndexType("moduleInstance");
                    authXMLRequest.setIndexName(indexName);
                    authXMLRequest.setRequestType(AuthXMLRequest.LoginIndex);
                    requestType = AuthXMLRequest.LoginIndex;
                }
            }
        }
        
        AuthContext.Status loginStatus = AuthContext.Status.IN_PROGRESS;
        HttpServletRequest clientRequest = authXMLRequest.getClientRequest();
        if (loginState != null) {
            loginState.setHttpServletRequest(clientRequest);
            loginState.setHttpServletResponse(authXMLRequest.getClientResponse());
            if (clientRequest != null) {
                loginState.setParamHash(AuthUtils.parseRequestParameters(clientRequest));
            }
        }
        switch (requestType) {
            case AuthXMLRequest.NewAuthContext:
                try {
                    processNewRequest(servletRequest, servletResponse,
                        authResponse, loginState, authContext);
                    postProcess(loginState, authResponse);
                } catch (Exception ex) {
                    debug.error("Error in NewAuthContext ", ex);
                    setErrorCode(authResponse, ex);
                }
                break;
            case AuthXMLRequest.Login:
                try {
                    if (sessionID != null && sessionID.equals("0")) {
                        processNewRequest(servletRequest, servletResponse,
                        authResponse, loginState, authContext);
                    }
                    String clientHost = null;
                    if (security) {
                        clientHost = authXMLRequest.getHostName();
                        if (messageEnabled) {
		            debug.message("Client Host from Request = " + 
                                clientHost);
                        }
                    }
                    if ((clientHost == null) && (servletRequest != null)) {
                        clientHost = ClientUtils.getClientIPAddress(servletRequest);
                    }
                    loginState.setClient(clientHost);
                    authContext.login();
                    //setServletRequest(servletRequest,authContext);
                    processRequirements(xml, authContext,authResponse, params,
                        servletRequest);
                    loginStatus = authContext.getStatus();
                    authResponse.setRemoteRequest(loginState.getHttpServletRequest());
                    authResponse.setRemoteResponse(loginState.getHttpServletResponse());

                    postProcess(loginState, authResponse);
                    checkACException(authResponse, authContext);
                } catch (Exception ex) {
                    debug.error("Error during login ", ex);
                    setErrorCode(authResponse, ex);
                    authResponse.setLoginStatus(authContext.getStatus());
                }
                break;
            case AuthXMLRequest.LoginIndex:
                try {
                    AuthContext.IndexType indexType
                    = authXMLRequest.getIndexType();
                    String indexName = authXMLRequest.getIndexName();
                    if (messageEnabled) {
                        debug.message("indexName is : " + indexName);
                        debug.message("indexType is : " + indexType);
                    }
                    if (sessionID != null && sessionID.equals("0")) {
                        processNewRequest(servletRequest, servletResponse,
                        authResponse, loginState, authContext);
                    }
                    String clientHost = null;
                    if (security) {
                        clientHost = authXMLRequest.getHostName();
                        if (messageEnabled) {
		                    debug.message("Client Host from Request = " +
                                clientHost);
                        }
                    }
                    if ((clientHost == null)  && (servletRequest != null)) {
                        clientHost = ClientUtils.getClientIPAddress(servletRequest);
                    }
                    loginState.setClient(clientHost);
                    String locale = authXMLRequest.getLocale();
                    if (locale != null && locale.length() > 0) {
                        if (debug.messageEnabled()) {
                            debug.message("locale is : " + locale);
                        }
                        authContext.login(indexType,indexName, false, 
                            envMap, locale);
                    } else {
                        authContext.login(indexType,indexName, false, 
                            envMap, null);
                    }
                    //setServletRequest(servletRequest,authContext);
                    processRequirements(xml, authContext,authResponse, params,
                        servletRequest);
                    loginStatus = authContext.getStatus();
                    authResponse.setRemoteRequest(loginState.getHttpServletRequest());
                    authResponse.setRemoteResponse(loginState.getHttpServletResponse());
                    postProcess(loginState, authResponse);
                    checkACException(authResponse, authContext);
                } catch (Exception ex) {
                    debug.error("Exception during LoginIndex", ex);
                    setErrorCode(authResponse, ex);
                }
                break;
            case AuthXMLRequest.LoginSubject:
                try {
                    Subject subject = authXMLRequest.getSubject();
                    authContext.login(subject);
                    //setServletRequest(servletRequest,authContext);
                    processRequirements(xml, authContext,authResponse, params,
                        servletRequest);
                    postProcess(loginState, authResponse);
                    loginStatus = authContext.getStatus();
                    checkACException(authResponse, authContext);
                } catch (AuthLoginException ale) {
                    debug.error("Exception during LoginSubject", ale);
                    setErrorCode(authResponse, ale);
                }
                break;
            case AuthXMLRequest.SubmitRequirements:
                try {
                    //setServletRequest(servletRequest,authContext);
                    Callback[] submittedCallbacks =
                    authXMLRequest.getSubmittedCallbacks();
                    authContext.submitRequirements(submittedCallbacks);
                    Callback[] reqdCallbacks = null;
                    if (authContext.hasMoreRequirements()) {
                        reqdCallbacks = authContext.getRequirements();
                        authResponse.setReqdCallbacks(reqdCallbacks);
                    }
                    authResponse.setRemoteRequest(loginState.getHttpServletRequest());
                    authResponse.setRemoteResponse(loginState.getHttpServletResponse());
                    postProcess(loginState, authResponse);
                    loginStatus = authContext.getStatus();
                    authResponse.setLoginStatus(loginStatus);
                    InternalSession oldSession =  loginState.
                        getOldSession();
                    authResponse.setOldSession(oldSession);
                    checkACException(authResponse, authContext);
                } catch (Exception ex) {
                    debug.error("Error during submit requirements ", ex);
                    setErrorCode(authResponse, ex);
                }
                break;
            case AuthXMLRequest.QueryInformation:
                try {
                    if (sessionID != null && sessionID.equals("0")) {
                        processNewRequest(servletRequest, servletResponse,
                        authResponse, loginState, authContext);
                    }
                    Set moduleNames = authContext.getModuleInstanceNames();
                    authResponse.setModuleNames(moduleNames);
                    authResponse.setAuthContext(authContext);
                    postProcess(loginState, authResponse);
                    checkACException(authResponse, authContext);
                } catch (Exception ex) {
                    debug.error("Error during Query Information", ex);
                    setErrorCode(authResponse, ex);
                }
                break;
            case AuthXMLRequest.Logout:
                //Object loginContext = null;
                //InternalSession intSess = null;
                //SSOToken token = null;
                //boolean logoutCalled = false;
                if (sessionID != null && !sessionID.equals("0")) {
                    /*intSess = AuthD.getSession(sessionID);
                    try {
                        token = SSOTokenManager.getInstance().
                            createSSOToken(sessionID);
                        if (debug.messageEnabled()) {
                            debug.message("AuthXMLHandler."
                                + "processAuthXMLRequest: Created token " 
                                + "during logout = "+token);
                        }
	            } catch (com.iplanet.sso.SSOException ssoExp) {
                       if (debug.messageEnabled()) {
		           debug.message("AuthXMLHandler.processAuthXMLRequest:"
                           + "SSOException checking validity of SSO Token");
                       }
	            }*/
                    try {
                        AuthUtils.logout(sessionID, servletRequest, servletResponse);
                    } catch (com.iplanet.sso.SSOException ssoExp) {
                        if (debug.messageEnabled()) {
                            debug.message("AuthXMLHandler.processAuthXMLRequest:"
                                + "SSOException checking validity of SSO Token");
                        }
                    }
                }
                
                /*if (intSess != null) {
                    loginContext = intSess.getObject(ISAuthConstants.
                        LOGIN_CONTEXT);
                }
                try {
                    if (loginContext != null) {
                        if (loginContext instanceof 
                            javax.security.auth.login.LoginContext) {
                            javax.security.auth.login.LoginContext lc = 
                                (javax.security.auth.login.LoginContext) 
                                 loginContext;
                            lc.logout();
                        } else {
                            com.sun.identity.authentication.jaas.LoginContext 
                                jlc = (com.sun.identity.authentication.jaas.
                                LoginContext) loginContext;
                            jlc.logout();
                        }
                        logoutCalled = true;
                    }
                } catch (javax.security.auth.login.LoginException loginExp) {
                    debug.error("AuthXMLHandler.processAuthXMLRequest: "
                        + "Cannot Execute module Logout", loginExp);
                }
                Set postAuthSet = null;
                if (intSess != null) {
                    postAuthSet = (Set) intSess.getObject(ISAuthConstants.
                        POSTPROCESS_INSTANCE_SET);
                }
                if ((postAuthSet != null) && !(postAuthSet.isEmpty())) {
                    AMPostAuthProcessInterface postLoginInstance=null;
                    for(Iterator iter = postAuthSet.iterator();
                    iter.hasNext();) {
                        try {
	                    postLoginInstance =
	 	                (AMPostAuthProcessInterface) iter.next();
                             postLoginInstance.onLogout(servletRequest, 
                                 servletResponse, token);
                        } catch (Exception exp) {
                           debug.error("AuthXMLHandler.processAuthXMLRequest: "
                               + "Failed in post logout.", exp);
                        }
	            }
                } else {
                    String plis = null;
                    if (intSess != null) {
                        plis = intSess.getProperty(
                            ISAuthConstants.POST_AUTH_PROCESS_INSTANCE);
                    }
                    if (plis != null && plis.length() > 0) {
                        StringTokenizer st = new StringTokenizer(plis, "|");
                        if (token != null) {
                            while (st.hasMoreTokens()) {
                                String pli = (String)st.nextToken();
                                try {
                                    AMPostAuthProcessInterface postProcess = 
                                            (AMPostAuthProcessInterface)
                                            Thread.currentThread().
                                            getContextClassLoader().
                                            loadClass(pli).newInstance();
                                    postProcess.onLogout(servletRequest, 
                                        servletResponse, token);
                                } catch (Exception e) {
                                    debug.error("AuthXMLHandler."
                                        + "processAuthXMLRequest:" + pli, e);
                                }
                            }
                        }
                    }
                }
                try {
                    boolean isTokenValid = SSOTokenManager.getInstance().
                        isValidToken(token);
                    if ((token != null) && isTokenValid) {
                        AuthD.getAuth().logLogout(token);
                        Session session = Session.getSession(
                            new SessionID(sessionID));
                        session.logout();
                        debug.message("logout successful.");
                    }
	        } catch (com.iplanet.dpro.session.SessionException 
                    sessExp) {
                    if (debug.messageEnabled()) {
                        debug.message("AuthXMLHandler."
                            + "processAuthXMLRequest: SessionException"
                            + " checking validity of SSO Token");
                    }
	        } catch (com.iplanet.sso.SSOException ssoExp) {
                    if (debug.messageEnabled()) {
                        debug.message("AuthXMLHandler."
                            + "processAuthXMLRequest: SSOException "
                            + "checking validity of SSO Token");
                    }
                }*/
                authResponse.setLoginStatus(AuthContext.Status.COMPLETED);
                break;
            case AuthXMLRequest.Abort:
                try {
                    authContext.abort();
                    loginStatus = authContext.getStatus();
                    authResponse.setLoginStatus(loginStatus);
                    checkACException(authResponse, authContext);
                } catch (AuthLoginException ale) {
                    debug.error("Error aborting ", ale);
                    setErrorCode(authResponse, ale);
                }
                break;
        }
        
        if (messageEnabled) {
            debug.message("loginStatus: " + loginStatus);

            if (authContext != null) {
                debug.message("error Code: " + authContext.getErrorCode());
                debug.message("error Template: " + authContext.getErrorTemplate());
            }
        }
        
        if (loginStatus == AuthContext.Status.FAILED) {
            if((authContext.getErrorMessage() != null) &&
                (authContext.getErrorMessage().equals(
                AMResourceBundleCache.getInstance().
                getResBundle("amAuthLDAP",
                com.sun.identity.shared.locale.Locale.getLocale(
                loginState.getLocale())).
                getString(ISAuthConstants.EXCEED_RETRY_LIMIT)))){
                    loginState.setErrorCode(AMAuthErrorCode.AUTH_LOGIN_FAILED);
            }
            if ((authContext.getErrorCode() != null) &&
            ((authContext.getErrorCode()).length() > 0 )) {
                authResponse.setErrorCode(authContext.getErrorCode());
            }
            checkACException(authResponse, authContext);
            if ((authContext.getErrorTemplate() != null) &&
            ((authContext.getErrorTemplate()).length() > 0 )) {
                authResponse.setErrorTemplate(authContext.getErrorTemplate());
            }
            //Account Lockout Warning Check
            if((authContext.getErrorCode() != null) &&
                (authContext.getErrorCode().equals(
                AMAuthErrorCode.AUTH_INVALID_PASSWORD))){
                String lockWarning = authContext.getLockoutMsg();
                if((lockWarning != null) && (lockWarning.length() > 0)){
                    authResponse.setErrorMessage(lockWarning);
                }
            }
        }
        
        return authResponse;
    }

    /*
     * Process the new http request
     */
    private void processNewRequest(
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse, 
        AuthXMLResponse authResponse, 
        LoginState loginState, 
        AuthContextLocal authContext
    ) throws AuthException {
        if ( authContext == null ) {
            throw new AuthException(
            AMAuthErrorCode.AUTH_INVALID_DOMAIN, null);
        }
        InternalSession oldSession =  loginState.getOldSession();
        authResponse.setOldSession(oldSession);
                    
        authResponse.setLoginStatus(AuthContext.Status.IN_PROGRESS);
        AuthUtils.setlbCookie(authContext, servletRequest, servletResponse);
    }

    /*
     * reset the auth identifier, in case a status change(auth succeeds)
     * will cause sid change from that of HttpSession to InternalSession.
     */
    private void postProcess(LoginState loginState, 
        AuthXMLResponse authResponse) {
        SessionID sid = loginState.getSid();
        String sidString = null;
        if (sid != null ) {
            sidString = sid.toString();
        }
        if (messageEnabled) {
            debug.message("sidString is.. : " + sidString);
        }
        authResponse.setAuthIdentifier(sidString);
    }

    /*
     * Gets the next http request parameter
     */
    private String getNextParam(StringTokenizer st) {
        String retStr = null;
        if (st != null) {
            if (st.hasMoreTokens()) {
                retStr = st.nextToken();
            }
        }
        return retStr;
    }

    
    /*
     * process callbacks
     */
    private void processRequirements(
        String xml,
        AuthContextLocal authContext, 
        AuthXMLResponse authResponse,
        String params,
        HttpServletRequest servletRequest) {
        String[] paramArray = null;
        StringTokenizer paramsSet = null;
        if (params != null) {
            paramsSet = new StringTokenizer(params,
                ISAuthConstants.PIPE_SEPARATOR);
        }  
        boolean allCallbacksAreSet = true;
        String param;

        while (authContext.hasMoreRequirements()) {
            Callback[] reqdCallbacks = authContext.getRequirements();

            for (int i = 0 ; i < reqdCallbacks.length ; i++) {
                if (reqdCallbacks[i] instanceof X509CertificateCallback) {
                    X509CertificateCallback certCallback =
                    (X509CertificateCallback) reqdCallbacks[i];
                    LoginState loginState = 
                            AuthUtils.getLoginState(authContext);
                    if (loginState != null) {
                        X509Certificate cert = 
                                loginState.getX509Certificate(servletRequest);
                        if (cert != null) {
                            certCallback.setCertificate(cert);
                            certCallback.setReqSignature(false);
                        } else {
                            allCallbacksAreSet = false;
                        }
                    }                    
                } else {
                    param = null;

                    if (reqdCallbacks[i] instanceof NameCallback) {
                        param = getNextParam(paramsSet);

                        if (param != null) {
                            NameCallback nc = (NameCallback)reqdCallbacks[i];
                            nc.setName(param);

                            if (messageEnabled) {
                                debug.message("Name callback set to " + param);
                            }
                        } else {
                            allCallbacksAreSet = false;
                            break;
                        }
                    } else if (reqdCallbacks[i] instanceof PasswordCallback) {
                        param = getNextParam(paramsSet);

                        if (param != null) {
                            PasswordCallback pc =
                                (PasswordCallback)reqdCallbacks[i];
                            pc.setPassword(param.toCharArray());
                            if (messageEnabled) {
                                debug.message("Password callback is set");
                            }
                        } else {
                            allCallbacksAreSet = false;
                            break;
                        }
                    } else {
                        if (params == null) {
                            allCallbacksAreSet = false;
                        } 
                    }
                    // add more callbacks if required
                }    
            }

            if (getNextParam(paramsSet) != null) {
                allCallbacksAreSet = false;
            }

            if (allCallbacksAreSet) {
                if (messageEnabled) {
                    debug.message("submit callbacks with passed in params");
                }
                authContext.submitRequirements(reqdCallbacks);
            } else {
                authResponse.setReqdCallbacks(reqdCallbacks);
                break;
            } 
        } 
        if (!authContext.hasMoreRequirements()) {
            AuthContext.Status loginStatus = authContext.getStatus();
            if (messageEnabled) {
                debug.message(" Status: " + loginStatus);
            }
            authResponse.setLoginStatus(loginStatus);
        }
    }
    
    /*
     * Check for the AuthContext Exceptions
     */
    private void checkACException(
        AuthXMLResponse authResponse,
        AuthContextLocal acl) {
        AuthLoginException ale = acl.getLoginException();
        if (ale == null) {
            return;
        }

        /*
         * this code does not allow client to remotely select locale.
         * but this is a problem comes with the AuthContext API, cannot
         * be simply solved here.
         */
        if ((ale.getL10NMessage(locale) != null) &&
            ((ale.getL10NMessage(locale)).length() > 0 )
        ) {
            authResponse.setErrorMessage(ale.getL10NMessage(locale));
        }
        authResponse.setIsException(true);
    }
    
    /*
     * Set the error code
     */
    private void setErrorCode(AuthXMLResponse authResponse, Exception e) {
        if (e == null) {
            return;
        }
        if (e instanceof L10NMessage) {
            authResponse.setErrorCode(getAuthErrorCode((L10NMessage)e));
        } else {
            authResponse.setErrorCode(e.getMessage());
        }
        authResponse.setIsException(true);
    }
    
    /*
     * Get the error code
     */
    private String getAuthErrorCode(L10NMessage le) {
        String errorCode = le.getErrorCode();
        if (errorCode == null) {
            errorCode = le.getMessage();
        }
        return errorCode;
    }

    private void setServletRequest(HttpServletRequest servletRequest, 
            AuthContextLocal authContext) {
        LoginState theLoginState = AuthUtils.getLoginState(authContext);
        theLoginState.setHttpServletRequest(servletRequest);
        if (debug.messageEnabled()) {
            debug.message("AuthXMLHandler.setServletRequest(): Setting servlet request.");
        }
    }
    
    // Returns environment Map based on input environment List values
    // each value takes following format:
    // env-name|value1|value2|....
    private Map toEnvMap(List envList) {
        if (envList == null) {
            return null;
        }
        Map map = new HashMap();
        int size = envList.size();
        for (int i = 0; i < size; i++) {
            String value = (String) envList.get(i);
            StringTokenizer tokens = new StringTokenizer(value, 
                ISAuthConstants.PIPE_SEPARATOR);
            String envName = null;
            if (tokens.hasMoreTokens()) {
                envName = (String) tokens.nextToken();
            }
            Set envValues = new HashSet();
            while (tokens.hasMoreTokens()) {
                envValues.add(
                    AuthClientUtils.unescapePipe(tokens.nextToken()));
            }
            if ((envName != null) && !envValues.isEmpty()) {
                map.put(envName, envValues);
            }
        }
        return map;
    }
} // end class
