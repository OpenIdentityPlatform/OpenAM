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
 * $Id: AuthContextLocal.java,v 1.12 2009/05/21 21:57:34 qcheng Exp $
 *
 */



package com.sun.identity.authentication.server;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AMLoginContext;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.service.LoginStatus;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.util.PolicyDecisionUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpSession;

/**
 * The <code>AuthContextLocal</code> provides the implementation for
 * authenticating users.
 * <p>
 * A typical caller instantiates this class and starts the login process.
 * The caller then obtains an array of <code>Callback</code> objects,
 * which contains the information required by the authentication plug-in
 * module. The caller requests information from the user. On receiving
 * the information from the user, the caller submits the same to this class.
 * If more information is required, the above process continues until all
 * the information required by the plug-ins/authentication modules, has
 * been supplied. The caller then checks if the user has successfully
 * been authenticated. If successfully authenticated, the caller can
 * then get the <code>Subject</code> and <code>SSOToken</code> for the user;
 * if not successfully authenticated, the caller obtains the AuthLoginException.
 * <p>
 * The implementation supports authenticating users either locally
 * i.e., in process with all authentication modules configured or remotely
 * to an authentication service/framework. (See documentation to configure
 * in either of the modes).
 * <p>
 * The <code>getRequirements()</code> and <code>submitRequirements()</code> 
 * are used to pass the user credentials for authentication by the plugin 
 * modules,<code>getStatus()</code> returns the authentication status.
 * <p>
 * It should be serializable as a requirement to be stored in HttpSession.
 *
 */
public final class AuthContextLocal extends Object
    implements java.io.Serializable {

    /*
     * Protected variables used locally
     */

    // Debug & I18N class
    private static final String amAuthContextLocal = "amAuthContextLocal";
    /**
     * Hold the debug instance
     */
    protected static Debug authDebug = Debug.getInstance(amAuthContextLocal);
    
    /**
     * Holds the locale-specific information 
     */
    protected static ResourceBundle bundle =
        Locale.getInstallResourceBundle(amAuthContextLocal);

    /**
     * Holds organizationName
     */
    protected String organizationName;
    /**
     * Holds the set of module instance names
     */
    protected Set moduleInstanceNames;
    /**
     * Holds the index type
     */
    protected AuthContext.IndexType indexType;
    /**
     * Holds the index name
     */
    protected String indexName;
    /**
     * Holds the login status
     */
    protected AuthContext.Status loginStatus;
    /**
     * Holds the host name
     */
    protected String hostName;
    /**
     * Holds the http session
     */
    protected HttpSession httpSession;
    /**
     * Holds Single Sign on Token
     */
    protected SSOToken ssoToken;
    /**
     * AuthLoginException
     */
    protected volatile AuthLoginException loginException = null;
    /**
     * Holds call back information
     */
    protected Callback[] informationRequired = null;
    /**
     * AuthLoginContext
     */
    public AMLoginContext amlc = null;
    /**
     * Holds LoginStatus
     */
    public LoginStatus ls;
    /**
     * Holds subject
     */
    protected Subject subject;
    /**
     * character array for password
     */
    protected char[] password;
    
    private LoginState loginState = null;

    private String orgDN = null;
    
    /**
     * Holds information about submittion of requirements
     */
    private boolean inSubmitRequirements = false;

    /**
     * Creates <code>AuthContextLocal</code> instance is obtained for a given
     * organization name, or sub organization name. <code>login</code> method is
     * then used to start the authentication process.
     *
     * @param orgName name of the user's organization.
     *
     * @supported.api
     */
    public AuthContextLocal(String orgName) {
        authDebug.message("AuthContextLocal() constructor called");
        organizationName = orgName;

        amlc = new AMLoginContext(this);
        if (authDebug.messageEnabled()) {
            authDebug.message("AMLoginContext object is... " + amlc);
        }
        reset();
    }

    /**
     * Returns authentication module/s instances(or) plugin(s) configured
     * for an organization, or sub-organization that was set during the
     * <code>AuthContext</code> constructor.
     *
     * @return authentication module/s instances (or plugins).
     * @throws UnsupportedOperationException if an error occurred.
     *
     * @supported.api
     */
    public Set getModuleInstanceNames() {
        moduleInstanceNames = amlc.getModuleInstanceNames();

        return (moduleInstanceNames);
    }

    /**
     * Starts the login process for the given <code>AuthContextLocal</code>
     * object.
     *
     * @exception AuthLoginException if an error occurred during login.
     * @supported.api
     */
    public void login() throws AuthLoginException {
        login(null);
    }
    
    /**
     * Starts the login process for the given <code>AuthContextLocal</code>s
     * object for the given <code>Principal</code> and the user's password.
     * This method should be called primarily
     * when the authenticator knows there would no other
     * credentials needed to complete the authentication process.
     *
     * @param principal <code>Principal</code> of the user to be authenticated.
     * @param password password for the user.
     * @throws AuthLoginException if an error occurred 
     *            during login.
     * @supported.api
     */
    public void login(Principal principal, char[] password) 
        throws AuthLoginException {

        // Make sure principal and password are not null
        if (principal == null)
            throw new AuthLoginException(amAuthContextLocal, 
                "invalid-username", null);
        if (password == null)
            throw new AuthLoginException(amAuthContextLocal, 
                "invalid-password", null);

        // Copy the password
        this.password = password;

        login(null, null, principal, password, null, false);
    }

    /**
     * Start the login process for the <code>AuthContextLocal</code> object
     * identified by the index type and index name.
     * The <code>IndexType</code> defines the possible kinds
     * of "objects" or "resources" for which an authentication can
     * be performed.  Currently supported index types are
     * users, roles, services (or application), levels and mechanism.
     *
     * @param type authentication index type.
     * @param indexName authentication index name.
     * @throws AuthLoginException if an error occurred 
     *            during login.
     * @supported.api
     */
    public void login(AuthContext.IndexType type, String indexName)
        throws AuthLoginException {
        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContextLocal::login() called " +
            "with IndexType : " + type + " & Indexname : " + indexName);
        }

        login(type, indexName, null, null, null, false);
    }

    /**
     * Starts the login process for the given <code>AuthContextLocal</code>
     * object for the given <code>Subject</code>.
     * Refer to JAAS for description on <code>Subject</code>.
     *
     * @param subject <code>Subject</code> of the user to be authenticated.
     * @throws AuthLoginException if an error occurred 
     *            during login.
     * @supported.api
     */
    public void login(Subject subject) throws AuthLoginException {
        login(null, null, null, null, subject, false);
    }

    /**
     * Starts the login process for the given <code>AuthContextLocal</code>
     * object identified by the index type and index name.
     * The <code>IndexType</code> defines the possible kinds
     * of "objects" or "resources" for which an authentication can
     * be performed.Currently supported index types are
     * users, roles, services (or application), levels and mechanism.
     * The pCookieMode indicates that a persistent cookie exists
     * for this request.
     *
     * @param type authentication index type.
     * @param indexName authentication index name.
     * @param locale locale setting.
     * @throws AuthLoginException if an error occurred during 
     *            login process.
     */
    public void login(
        AuthContext.IndexType type,
        String indexName,
        String locale
    ) throws AuthLoginException {
        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContextLocal::login() called " +
            "with IndexType : " + type + " & Indexname : " + indexName +
            " & locale : " + locale);
        }

        login(type, indexName, null, null, null, false, null, locale);
    }

    /**
     * Starts the login process for the given <code>AuthContextLocal</code>
     * object identified by the index type and index name.
     * The <code>IndexType</code> defines the possible kinds
     * of "objects" or "resources" for which an authentication can
     * be performed.Currently supported index types are
     * users, roles, services (or application), levels and mechanism.
     * The pCookieMode indicates that a persistent cookie exists
     * for this request.
     *
     * @param type authentication index type.
     * @param indexName authentication index name.
     * @param pCookieMode <code>true</code> if persistent Cookie exists,
     *        <code>false</code> otherwise
     * @throws AuthLoginException if an error occurred during 
     *            login process.
     */
    public void login(
        AuthContext.IndexType type,
        String indexName,
        boolean pCookieMode
    ) throws AuthLoginException {
        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContextLocal::login() called " +
            "with IndexType : " + type + " & Indexname : " + indexName +
            " & pCookieMode : " + pCookieMode);
        }

        login(type, indexName, null, null, null, pCookieMode);
    }

    /**
     * Starts the login process for the given <code>AuthContextLocal</code>
     * object identified by the index type and index name.
     * The <code>IndexType</code> defines the possible kinds
     * of "objects" or "resources" for which an authentication can
     * be performed.Currently supported index types are
     * users, roles, services (or application), levels and mechanism.
     * The pCookieMode indicates that a persistent cookie exists
     * for this request.
     * The locale specifies the user preferred locale setting.
     *
     * @param type authentication index type.
     * @param indexName authentication index name.
     * @param pCookieMode <code>true</code> if persistent Cookie exists,
     *        <code>false</code> otherwise
     * @param locale locale setting.
     * @throws AuthLoginException if an error occurred during 
     *            login process.
     */
    public void login(
        AuthContext.IndexType type,
        String indexName,
        boolean pCookieMode,
        String locale
    ) throws AuthLoginException {
        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContextLocal::login() called " +
            "with IndexType : " + type + " & Indexname : " + indexName +
            " & pCookieMode : " + pCookieMode + " & locale : " + locale);
        }
        login(type, indexName, null, null, null, pCookieMode, null, locale);
    }
    
    /**
     * Starts the login process for the given <code>AuthContextLocal</code>
     * object identified by the index type and index name.
     * The <code>IndexType</code> defines the possible kinds
     * of "objects" or "resources" for which an authentication can
     * be performed.Currently supported index types are
     * users, roles, services (or application), levels and mechanism.
     * The pCookieMode indicates that a persistent cookie exists
     * for this request.
     * The locale specifies the user preferred locale setting.
     *
     * @param type authentication index type.
     * @param indexName authentication index name.
     * @param pCookieMode <code>true</code> if persistent Cookie exists,
     *        <code>false</code> otherwise
     * @param envMap Environment Map, key is String, value is set of string.
     *        this is applicable only when the type is 
     *        <code>AuthContext.IndexType.RESOURCE</code>
     * @param locale locale setting.
     * @throws AuthLoginException if an error occurred during 
     *            login process.
     */
    public void login(
        AuthContext.IndexType type,
        String indexName,
        boolean pCookieMode, 
        Map envMap,
        String locale
    ) throws AuthLoginException {
        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContextLocal::login() called " +
            "with IndexType : " + type + " & Indexname : " + indexName +
            " & pCookieMode : " + pCookieMode + " & locale : " + locale +
            " & envMap : " + envMap);
        }
        login(type, indexName, null, null, null, pCookieMode, envMap, locale);
    }

    /**
     * Performs the Login for the given AuthContext
     * @param type authentication index type
     * @param indexName authentication index name
     * @param principal principal name of the user to be authenticated
     * @param password password for the user
     * @param subject authentication subject
     * @param pCookieMode <code>true</code>persistent Cookie exists,
     *        <code>false</code> otherwise
     * @throws AuthLoginException if error occurs during login
     */
    protected void login(AuthContext.IndexType type, String indexName, 
        Principal principal, char[] password, Subject subject, 
        boolean pCookieMode) throws AuthLoginException {
        login(type, indexName, principal, password, subject, pCookieMode, 
            null, null);
    }

    /**
     * Performs the Login for the given AuthContext
     * @param type authentication index type
     * @param indexName authentication index name
     * @param principal principal name of the user to be authenticated
     * @param password password for the user
     * @param subject authentication subject
     * @param pCookieMode <code>true</code>persistent Cookie exists,
     *        <code>false</code> otherwise
     * @param envMap Environment map, this is applicable only when the type
     *        is <code>AuthContext.IndexType.RESOURCE</code>
     * @param locale locale setting
     * @throws AuthLoginException if error occurs during login
     */
    protected void login(AuthContext.IndexType type, String indexName, 
        Principal principal, char[] password, Subject subject, 
        boolean pCookieMode, Map envMap, String locale) 
        throws AuthLoginException {
        try {
            /*if (!getStatus().equals(AuthContext.Status.NOT_STARTED)) {
                if (authDebug.messageEnabled()) {
                    authDebug.message("AuthContextLocal::login called " +
                    "when the current login status is : " + getStatus());
                }
                throw new AuthLoginException(amAuthContextLocal, 
                    "invalidMethod", new Object[]{getStatus()});
            }*/

            // switch the login status
            loginStatus = AuthContext.Status.IN_PROGRESS;

            String redirectUrl = null;
            // specially processing for resouce/IP/Environement based auth
            if ((type != null) && type.equals(AuthContext.IndexType.RESOURCE)) { 
                // this is resouce/IP/Env based authentication
                // call Policy Decision Util to find out the actual auth type 
                // required by policy
                List result = Collections.EMPTY_LIST;
                try {
                    result = PolicyDecisionUtils.doResourceIPEnvAuth(
                            indexName, organizationName, envMap);
                } catch (PolicyException pe) {
                    // ignore, continue to default realm based authentication
                    // may need to revisit this in the future
                    authDebug.warning("AuthContextLocal.login() policy error " +
                        "indexName=" + indexName, pe);
                    type = null;
                    indexName = null;
                }
                if (authDebug.messageEnabled()) {
                    authDebug.message("AuthContextLocal.login: policy decision="
                        + result);
                }
                if (result.size() == 2) {
                    type = (AuthContext.IndexType) result.get(0);
                    indexName = (String) result.get(1);
                } else if (result.size() == 1) {
                    // this is the redirection case (Policy Redirection Advice)
                    redirectUrl = (String) result.get(0);
                    // append goto parameter for federation case
                    Set tmp = (Set) envMap.get(ISAuthConstants.GOTO_PARAM);
                    if ((tmp != null) && !tmp.isEmpty()) {
                        String gotoParam = (String) tmp.iterator().next();
                        if ((gotoParam != null) && (gotoParam.length() != 0)) {
                            if ((redirectUrl != null) && 
                                (redirectUrl.indexOf("?") != -1)) {
                                redirectUrl = redirectUrl + "&" + 
                                    ISAuthConstants.GOTO_PARAM + "=" + 
                                    URLEncDec.encode(gotoParam);
                            } else {
                                redirectUrl = redirectUrl + "?" + 
                                    ISAuthConstants.GOTO_PARAM + "=" + 
                                    URLEncDec.encode(gotoParam);
                            }
                        }
                    }
                    type = null;
                    indexName = null;
                } else {
                    // no policy decision, use default realm login
                    type = null;
                    indexName = null;
                }

            }
            HashMap loginParamsMap = new HashMap();

            loginParamsMap.put(INDEX_TYPE, type);
            loginParamsMap.put(INDEX_NAME, indexName);
            loginParamsMap.put(PRINCIPAL, principal);
            loginParamsMap.put(PASSWORD, password);
            loginParamsMap.put(SUBJECT, subject);
            loginParamsMap.put(PCOOKIE, Boolean.valueOf(pCookieMode));
            loginParamsMap.put(LOCALE, locale);
            if (redirectUrl != null) {
                loginParamsMap.put(REDIRECT_URL, redirectUrl);
            }

            if (authDebug.messageEnabled()) {
                authDebug.message(
                    "loginParamsMap : " + loginParamsMap.toString());
            }

            authDebug.message("calling AMLoginContext::exceuteLogin : ");
            amlc.executeLogin(loginParamsMap);
            authDebug.message("after AMLoginContext::exceuteLogin : ");
            if (amlc.getStatus() == LoginStatus.AUTH_SUCCESS) {
                loginStatus = AuthContext.Status.SUCCESS;
            } else if (amlc.getStatus() == LoginStatus.AUTH_FAILED) {
                loginStatus = AuthContext.Status.FAILED;
            }
            if (authDebug.messageEnabled()) {
                authDebug.message(
                    "Status at the end of login() : " + loginStatus);
            }
        } catch (AuthLoginException e) {
            if (authDebug.messageEnabled()) {
                authDebug.message("Exception in ac.login : " + e.toString());
            }
            throw e;
        }
    }

    /**
     * Resets this instance of <code>AuthContextLocal</code>
     * object, so that a new login process can be initiated.
     * A new authentication process can started using any
     * one of the <code>login</code> methods.
     */
    public void reset() {
        authDebug.message("AuthContextLocal::reset() called");
        loginStatus = AuthContext.Status.NOT_STARTED;
        informationRequired = null;
        loginException = null;
    }

    /**
     * Returns the set of Principals the user has been authenticated as.
     * This should be invoked only after successful authentication.
     * If the authentication fails or the authentication is in process,
     * this will return <code>null</code>.
     *
     * @return The set of Principals the user has been authenticated as.
     * @supported.api
     */
    public Subject getSubject() {
        if (!loginStatus.equals(AuthContext.Status.SUCCESS)) {
            return (null);
        }
        if (subject == null) {
            subject = amlc.getSubject();
        }
        return (subject);
    }

    /**
     * Checks if the login process requires more information from the user to
     * complete the authentication.
     *
     * @return <code>true</code> if more credentials are required
     *         from the user.
     * @supported.api
     */
    public boolean hasMoreRequirements() {
        authDebug.message("AuthContextLocal::hasMoreRequirements()");

        if ((amlc.getStatus() == LoginStatus.AUTH_SUCCESS) ||
            (amlc.getStatus() == LoginStatus.AUTH_FAILED)
        ) {
            return false;
        } else {
            informationRequired = amlc.getRequiredInfo();
            return (informationRequired != null);
        }
    }

    /**
     * Checks if the login process requires more information from the user to
     * complete the authentication
     * @param noFilter falg to indicate if there is a Filter
     * @return <code>true</code> if more credentials are required
     *         from the user.
     */
    public boolean hasMoreRequirements(boolean noFilter) {
        authDebug.message("AuthContextLocal::hasMoreRequirements()");

        if ((amlc.getStatus() == LoginStatus.AUTH_SUCCESS) ||
            (amlc.getStatus() == LoginStatus.AUTH_FAILED)
        ) {
            return false;
        } else {
            informationRequired = amlc.getRequiredInfo();
            return (getCallbacks(informationRequired, noFilter) != null);
        }
    }

    /**
     * Returns an array of <code>Callback</code> objects that
     * must be populated by the user and returned back.
     * These objects are requested by the authentication plug-ins,
     * and these are usually displayed to the user. The user then provides
     * the requested information for it to be authenticated.
     *
     * @return an array of <code>Callback</code> objects requesting credentials
     *         from user.
     * @supported.api
     */
    public Callback[] getRequirements() {
        authDebug.message("AuthContextLocal::getRequirements()");

        if ((amlc.getStatus() == LoginStatus.AUTH_SUCCESS) ||
            (amlc.getStatus() == LoginStatus.AUTH_FAILED)
        ) {
            return null;
        } else {
            return (informationRequired);
        }
    }

    /**
     * Returns an array of <code>Callback</code> objects that
     * must be populated by the user and returned back.
     * These objects are requested by the authentication plug-ins,
     * and these are usually displayed to the user. The user then provides
     * the requested information for it to be authenticated.
     *
     * @param noFilter flag to indicate if there is a Filter
     * @return an array of <code>Callback</code> objects requesting credentials
     *         from user.
     * @supported.api
     */
    public Callback[] getRequirements(boolean noFilter) {
        authDebug.message("AuthContextLocal::getRequirements()");

        if ((amlc.getStatus() == LoginStatus.AUTH_SUCCESS) ||
            (amlc.getStatus() == LoginStatus.AUTH_FAILED)
        ) {
            return null;
        } else {
            return (getCallbacks(informationRequired, noFilter));
        }
    }

    /**
     * Submit the populated <code>Callback</code> objects
     * to the authentication plug-in modules. Called after
     * <code>getRequirements</code> method and obtaining
     * user's response to these requests.
     *
     * @param info array of <code>Callback</code> objects
     * @supported.api
     */
    public void submitRequirements(Callback[] info) {
        authDebug.message("AuthContextLocal::submitRequirements()");
	inSubmitRequirements = true;
	try{
	   informationRequired = null;
 	   amlc.submitRequiredInfo(info) ;
	   if (!amlc.isPureJAAS()) {
		amlc.runLogin();
	   }
  	   if (amlc.getStatus() == LoginStatus.AUTH_SUCCESS) {
		loginStatus = AuthContext.Status.SUCCESS;
	   } else if (amlc.getStatus() == LoginStatus.AUTH_FAILED) {
		loginStatus = AuthContext.Status.FAILED;
	   }
	   authDebug.message("AuthContextLocal::submitRequirements end");
	   if (authDebug.messageEnabled()) {
		authDebug.message("Status at the end of submitRequirements() : "
				+ loginStatus);
	   }
         } finally {
           inSubmitRequirements = false;
         }
    }

    /**
     * Logs out the user and also invalidates the <code>SSOToken</code>
     * associated with this <code>AuthContextLocal</code>.
     *
     * @throws AuthLoginException if an error occurred during logout
     * @supported.api
     */
    public void logout() throws AuthLoginException {
        authDebug.message("AuthContextLocal::logout()");

        try {
            amlc.logout();
        } catch (Exception e) {
            if (authDebug.messageEnabled()) {
                authDebug.message("Exception in AMLoginContext::logout() "
                    + e.getMessage());
            }
            throw new AuthLoginException(amAuthContextLocal, "logoutError",
                null, e);
        }

        authDebug.message("Called AMLoginContext::logout()");
        loginStatus = AuthContext.Status.COMPLETED;
    }

    /**
     * Returns login exception, if any, during
     * the authentication process. Typically set when the login
     * fails.
     *
     * @return login exception.
     * @supported.api
     */
    public AuthLoginException getLoginException() {
        authDebug.message("AuthContextLocal::getLoginException()");
        return (loginException);
    }

    /**
     * Sets the login exception that represents errors during the 
     * authentication process.
     *
     * @param exception AuthLoginException to be set.
     */
    public void setLoginException(AuthLoginException exception) {
        loginException = exception;
    }

    /**
     * Returns the current status of the authentication process.
     *
     * @return the current status of the authentication process.
     * @supported.api
     */
    public AuthContext.Status getStatus() {
        authDebug.message("AuthContextLocal::getStatus()");
        if (amlc.getStatus() == LoginStatus.AUTH_SUCCESS) {
            loginStatus = AuthContext.Status.SUCCESS;
        }
        else if (amlc.getStatus() == LoginStatus.AUTH_FAILED) {
            loginStatus = AuthContext.Status.FAILED;
        }
        else if (amlc.getStatus() == LoginStatus.AUTH_RESET) {
            loginStatus =  AuthContext.Status.RESET;
        }
        else if (amlc.getStatus() == LoginStatus.AUTH_ORG_MISMATCH) {
            loginStatus =  AuthContext.Status.ORG_MISMATCH;
        }
        else if (amlc.getStatus() == LoginStatus.AUTH_IN_PROGRESS) {
            loginStatus =  AuthContext.Status.IN_PROGRESS;
        }
        else if (amlc.getStatus() == LoginStatus.AUTH_COMPLETED) {
            loginStatus =  AuthContext.Status.COMPLETED;
        }


        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContextLocal:: Status : " + loginStatus);
        }

        return (loginStatus);
    }

    /**
     * Sets the login status. Used internally and
     * not visible outside this package.
     * @param status login status
     */
    protected void setLoginStatus(AuthContext.Status status) {
        authDebug.message("AuthContextLocal::setLoginStatus()");
        loginStatus = status;
    }

    /**
     * Returns the Single-Sign-On (SSO) Token for the authenticated
     * user.Single-Sign-On token can be used as the authenticated token.
     *
     * @return single-sign-on token
     * @supported.api
     */
    public SSOToken getSSOToken() {
        ssoToken = amlc.getSSOToken();
        return (ssoToken);
    }

    /**
     * Returns the Successful Login URL for the authenticated user.
     * 
     * @return the Successful Login URL for the authenticated user.
     */
    public String getSuccessURL() {        
        return amlc.getSuccessURL();        
    }
    
    /**
     * Returns the Failure Login URL for the authenticating user.
     * 
     * @return the Failure Login URL for the authenticating user.
     */
    public String getFailureURL() {        
        return amlc.getFailureURL();        
    }
    
    /**
     * Returns the the organization name that was set during the
     * <code>AuthContextLocal</code> constructor.
     *
     * @return Organization name.
     *
     * @supported.api
     */
    public String getOrganizationName() {
        return (amlc.getOrganizationName());
    }

    /**
     * Terminates an ongoing <code>login</code> call that has not yet completed.
     *
     * @throws AuthLoginException if an error occurred during abort.
     *
     * @supported.api
     */
    public void abort() throws AuthLoginException {
        authDebug.message("AuthContextLocal::abort()");

        try {
            amlc.abort();
        } catch (Exception e) {
            if (authDebug.messageEnabled()) {
                authDebug.message("Exception in AMLoginContext::abort() "
                + e.getMessage());
            }
            throw new AuthLoginException(amAuthContextLocal, "abortError", 
                null, e);
        }

        loginStatus = AuthContext.Status.COMPLETED;
    }

    /**
     * Returns the error template.
     *
     * @return the error template.
     */
    public String getErrorTemplate() {
        return amlc.getErrorTemplate();
    }

    /**
     * Returns the error message.
     *
     * @return the error message.
     */
    public String getErrorMessage() {
        return amlc.getErrorMessage();
    }

    /**
     * Returns the error code.
     *
     * @return error code.
     */
    public String getErrorCode() {
        return amlc.getErrorCode();
    }

    /**
     * Returns the current 'authIdentifier' of the authentication process as
     * String Session ID.
     *
     * @return <code>authIdentifier</code> of the authentication process
     */
    public String getAuthIdentifier() {        
        return amlc.getAuthIdentifier();
    }
    
    /**
     * Returns the account lockout message. This can be either a dynamic
     * message indicating the number of tries left or the the account
     * deactivated message.
     *
     * @return account lockout message.
     */
    public String getLockoutMsg() {

        String lockoutMsg = amlc.getLockoutMsg();

        if (authDebug.messageEnabled()) {
            authDebug.message("getLockoutMsg: lockoutMsg: " + lockoutMsg);
        }

        return lockoutMsg;
    }

    /**
     * Checks the account is locked out
     * @return <code>true</code> if the account is locked,
     *         <code>false</code> otherwise
     */
    public boolean isLockedOut() {
        boolean isLockedOut = amlc.isLockedOut();
        if (authDebug.messageEnabled()) {
            authDebug.message("isLockedOut : " + isLockedOut);
        }

        return isLockedOut;
    }

    /**
     * Sets the client's host name , this method is used in case of remote 
     * authentication,to set the client's hostname or IP address. 
     * This could be used by the policy component to restrict access 
     * to resources.
     *
     * @param hostname Host name.
     */
    public void setClientHostName(String hostname) {
        hostName = hostname;
    }

    /**
     * Returns the clients host name
     * @return hostname
     */
    protected String getClientHostName() {
        return (hostName);
    }

    public boolean submittedRequirements() {
        return inSubmitRequirements;
    }

    /**
     * Sets the <code>HttpSession</code> that will be used by
     * the SSO component to store the session information. In the
     * absence of <code>HttpSession</code> the information is stored
     * in <code>HashMap</code> and will have issues with fail-over.
     * With session fail-over turned on <code>HttpSession</code>
     * would be provide persistance storage mechanism for SSO.
     *
     * @param session HttpSession
     */
    public void setHttpSession(HttpSession session) {
        httpSession = session;
    }

    /**
     * Returns the <code>HTTPSession</code> associated with the current
     * authentication context
     * @return httpSession
     */
    protected HttpSession getHttpSession() {
        return (httpSession);
    }

    /**
     * Returns the array of <code>Callback</code> requirements objects
     * @param recdCallbacks callbacks requirements
     * @param noFilter boolean to indicate if filter exists
     * @return an array of <code>Callback</code> objects
     */
    protected static Callback[] getCallbacks(
        Callback[] recdCallbacks,
        boolean noFilter) {
        if (recdCallbacks == null) {
            return (null);
        } else if (noFilter) {
            return recdCallbacks;
        } else {
            Callback[] answer = new Callback[0];
            ArrayList callbackList= new ArrayList(); 

            for (int i = 0; i < recdCallbacks.length; i++) {
                if (authDebug.messageEnabled()) {
                    authDebug.message("In getCallbacks() callback : "
                      + recdCallbacks[i]);
                }
                if (!(recdCallbacks[i] instanceof PagePropertiesCallback)) {
                     callbackList.add(recdCallbacks[i]);
                } 
            }
            return (Callback[]) callbackList.toArray(answer);
        }
    }

    /**
     * Sets the Login State
     * @param state login state
     */
    public void setLoginState(LoginState state) {
        loginState = state;
    } 

    /**
     * Returns the login state
     * @return loginState
     */
    public LoginState getLoginState() {
        return loginState;
    }

    /**
     * Sets the Organization DN
     * @param orgDN Organization DN
     */
    public void setOrgDN(String orgDN) {
        this.orgDN = orgDN;
    }

    /**
     * Returns the Organization DN
     * @return the Organization DN 
     */
    public String getOrgDN() {
        return orgDN;
    }

    /**
     * Holds LDAP URL
     */
    public final static String LDAP_AUTH_URL = "ldap://";

    /**
     * Holds ersistent cookie mode
     */
    public final static String PCOOKIE = "pCookieMode";
    /**
     * Holds principal name to be authenticated
     */
    public final static String PRINCIPAL = "principal";
    /**
     * Holds Password for the user
     */
    public final static String PASSWORD = "password";
    /**
     * authentication subject
     */
    public final static String SUBJECT = "subject";
    /**
     * authentication index type
     */
    public final static String INDEX_TYPE = "indexType";
    
    /**
     * authentication index name
     */
    public final static String INDEX_NAME = "indexName";

    /**
     * locale setting
     */
    public final static String LOCALE = "locale";
    
    /**
     * Redirection URL
     */
    public static final String REDIRECT_URL = "redirectionURL";
}
