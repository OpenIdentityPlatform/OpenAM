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
 * $Id: AuthContext.java,v 1.25 2009/11/21 01:12:59 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock AS
 */
package com.sun.identity.authentication;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.authentication.share.AuthXMLUtils;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.AMSecurityPropertiesException;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.KeyStore;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.openam.authentication.service.protocol.RemoteHttpServletRequest;
import org.forgerock.openam.authentication.service.protocol.RemoteHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>AuthContext</code> provides the implementation for
 * authenticating users.
 * <p>
 * A typical caller instantiates this class and starts the login process.
 * The caller then obtains an array of <code>Callback</code> objects,
 * which contains the information required by the authentication plug-in
 * module. The caller requests information from the user. On receiving
 * the information from the user, the caller submits the same to this class.
 * While more information is required, the above process continues until all
 * the information required by the plug-ins/authentication modules, has
 * been supplied. The caller then checks if the user has successfully
 * been authenticated. If successfully authenticated, the caller can
 * then get the <code>Subject</code> and <code>SSOToken</code> for the user;
 * if not successfully authenticated, the caller obtains the
 * <code>AuthLoginException</code>.
 * <p>
 * The implementation supports authenticating users either locally
 * i.e., in process with all authentication modules configured or remotely
 * to an authentication service/framework. (See documentation to configure
 * in either of the modes).
 *
 * @supported.api
 */
public class AuthContext extends Object implements java.io.Serializable {

    private java.util.Locale clientLocale = null;
    
    private String server_proto =
        SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
    private String server_host  =
        SystemProperties.get(Constants.AM_SERVER_HOST);
    private String server_port  =
        SystemProperties.get(Constants.AM_SERVER_PORT);
    private String server_uri  =
        SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    private boolean includeReqRes  =
        SystemProperties.getAsBoolean(Constants.REMOTEAUTH_INCLUDE_REQRES);

    private static final String amAuthContext = "amAuthContext";
    
    private static final String JSS_PASSWORD_UTIL =
        "com.sun.identity.authentication.util.JSSPasswordUtil";
    
    private static final String JSSE_PASSWORD_CALLBACK =
        "com.sun.identity.security.keystore.AMCallbackHandler";
    
    static String protHandlerPkg =
        System.getProperty(Constants.PROTOCOL_HANDLER, Constants.JSSE_HANDLER);
    
    static boolean usingJSSEHandler =
        protHandlerPkg.equals(Constants.JSSE_HANDLER);
    
    // Debug & I18N class
    protected static Debug authDebug = Debug.getInstance(amAuthContext);
    protected static ResourceBundle bundle =
            com.sun.identity.shared.locale.Locale.getInstallResourceBundle(amAuthContext);
    
    Status loginStatus = Status.IN_PROGRESS;
    String organizationName = "";
    Document receivedDocument;
    AuthLoginException loginException = null;

    String hostName = null;
    private boolean forceAuth = false;
    private boolean localSessionChecked = false;
    String nickName = null;
    private URL authURL = null;
    private URL authServiceURL = null;
    private SSOToken ssoToken = null;
    private String ssoTokenID = null;
    private static SSOToken appSSOToken = null;
    com.sun.identity.authentication.server.AuthContextLocal acLocal = null;
    private final static int DEFAULT_RETRY_COUNT = 1;
    private int retryRunLogin = DEFAULT_RETRY_COUNT;
    
    /**
     * Variables for checking auth service is running local
     */ 
    public boolean localFlag = false;
    /**
     * Variables for local AuthService identifier
     */ 
    public static String localAuthServiceID;

    // Variable to check if 6.3 style remote AuthN has to be performed
    static boolean useOldStyleRemoteAuthentication;
    static boolean useNewStyleRemoteAuthentication;
    
    // this cookieTable is used to keep all the cookies retrieved from the
    // the PLL layer and replay them in subsequent auth requests, mainly for
    // persistence purpose.
    private HashMap cookieTable = new HashMap();

    private HttpServletRequest remoteRequest = null;
    private HttpServletResponse remoteResponse = null;
    
    /**
     * Constructs an instance of <code>AuthContext</code> for a given
     * organization name or sub organization name. This organization or
     * sub-organization name must be either "/" separated
     * ( where it starts with "/" ) , DN , Domain name or DNS Alias Name.
     * <p>
     * Caller would then use <code>login</code> to start the
     * authentication process and use <code>getRequirements()</code> and
     * <code>submitRequirements()</code> to pass the credentials
     * needed for authentication by the plugin authentication modules.
     * The method <code>getStatus()</code> returns the
     * authentication status.
     *
     * @param orgName Name of the user's organization.
     * @throws AuthLoginException if <code>AuthContext</code> creation fails.
     *         This exception is kept for backward compatibility only.
     * 
     * @supported.api
     */
    public AuthContext(String orgName) throws AuthLoginException {
        organizationName = orgName;
    }
    
    /**
     * Constructs an instance of <code>AuthContext</code> for a given
     * organization name, or sub organization name and the OpenSSO
     * URL.
     * This organization or sub-organization name must be either "/" separated
     * ( where it starts with "/" ) , DN , Domain name or DNS Alias Name.
     * And the <code>url</code> should specify the OpenSSO protocol,
     * host name, port to talk to.
     * for example : <code>http://daye.red.iplanet.com:58080</code>
     *
     * Caller would then use <code>login</code> to start the
     * authentication process and use <code>getRequirements()</code> and
     * <code>submitRequirements()</code> to pass the credentials
     * needed for authentication by the plugin authentication modules.
     * The method <code>getStatus()</code> returns the
     * authentication status.
     *
     * @param orgName name of the user's organization
     * @param url URL of the OpenSSO to talk to
     * @throws AuthLoginException if <code>AuthContext</code> creation fails.
     *         This exception is kept for backward compatibility only.
     *
     * @supported.api
     */
    public AuthContext(String orgName, URL url) throws AuthLoginException {
        organizationName = orgName;
        authURL = url;
    }
    
    /**
     * Constructs an instance of <code>AuthContext</code> for a given
     * organization name, or sub organization name and a nick name
     * for the certificate to be used in SSL handshake if client authentication
     * is turn on in the server side.
     * This organization or sub-organization name must be either "/" separated
     * ( where it starts with "/" ) , DN , Domain name or DNS Alias Name.
     *
     * This constructor would be mainly used for the Certificate based
     * authentication. If the certificate database contains multiple matching
     * certificates for SSL, this constructor must be called in order for the
     * desired certificate to be used for the Certificate based authentication.
     *
     * Caller would then use <code>login</code> to start the
     * authentication process and use <code>getRequirements()</code> and
     * <code>submitRequirements()</code> to pass the credentials
     * needed for authentication by the plugin authentication modules.
     * The method <code>getStatus()</code> returns the
     * authentication status.
     *
     * @param orgName name of the user's organization
     * @param nickName nick name for the certificate to be used
     * @throws AuthLoginException if <code>AuthContext</code> creation fails.
     *         This exception is kept for backward compatibility only.
     *
     * @supported.api
     */
    public AuthContext(String orgName, String nickName)
            throws AuthLoginException {
        organizationName = orgName;
        this.nickName = nickName;
    }
    
    /**
     * Constructs an instance of <code>AuthContext</code> for a given
     * organization name, or sub organization name, a nick name
     * for the certificate to be used in SSL handshake if client authentication
     * is turn on in the server side and the OpenSSO URL.
     * This organization or sub-organization name must be either "/" separated
     * ( where it starts with "/" ) ,  DN , Domain name or a DNS Alias Name.
     * And the <code>url</code> should specify the OpenSSO protocol,
     * host name, port to talk to.
     * for example : <code>http://daye.red.iplanet.com:58080</code>
     * This constructor would be mainly used for the Certificate based
     * authentication. If the certificate database contains multiple matching
     * certificates for SSL, this constructor must be called in order for the
     * desired certificate to be used for the Certificate based authentication.
     *
     * Caller would then use <code>login</code> to start the
     * authentication process and use <code>getRequirements()</code> and
     * <code>submitRequirements()</code> to pass the credentials
     * needed for authentication by the plugin authentication modules.
     * The method <code>getStatus()</code> returns the
     * authentication status.
     *
     * @param orgName name of the user's organization
     * @param nickName nick name for the certificate to be used
     * @param url URL of the OpenSSO to talk to
     * @throws AuthLoginException if <code>AuthContext</code> creation fails.
     *         This exception is kept for backward compatibility only.
     *
     * @supported.api
     */
    public AuthContext(String orgName, String nickName, URL url)
            throws AuthLoginException {
        organizationName = orgName;
        this.nickName = nickName;
        authURL = url;
    }
    
    /**
     * Constructs an instance of <code>AuthContext</code> for a given
     * organization name, or sub organization name contained in the
     * single sign on token.
     *
     * This constructor should be called for re-authentication of an
     * authenticated user. single sign on token is the authenticated resource's
     * Single-Sign-On Token. If the session properties based on
     * the login method used matches those in the user's new
     * authenticated  session then session upgrade will be done.
     * A new session containing properties from both old single sign on token
     * and new session shall be returned and old session will be
     * destroyed if authentication  passes.
     *
     * Caller would then use <code>login</code> to start the
     * authentication process and use <code>getRequirements()</code> and
     * <code>submitRequirements()</code> to pass the credentials
     * needed for authentication by the plugin authentication modules.
     * The method <code>getStatus()</code> returns the
     * authentication status.
     *
     * @param ssoToken single sign on token representing the resource's previous
     *        authenticated session.
     * @throws AuthLoginException if <code>AuthContext</code> creation fails.
     *         This exception is kept for backward compatibility only.
     *
     * @supported.api
     */
    public AuthContext(SSOToken ssoToken) throws AuthLoginException {
        this.ssoToken = ssoToken;
    }
    
    /**
     * Constructs an instance of <code>AuthContext</code> for a given
     * organization name, or sub organization name contained in the
     * single sign on token.
     *
     * This constructor should be called for re-authentication of an
     * authenticated user. single sign on token is the authenticated resource's
     * Single-Sign-On Token. If the session properties based on
     * the login method used matches those in the user's new
     * authenticated  session then session upgrade will be done.
     * If forceAuth flag is <code>true</code> then the existing session 
     * is used and no new session is created otherwise this constructor 
     * behaves same as the constructor with no forceAuth flag.
     *
     * Caller would then use <code>login</code> to start the
     * authentication process and use <code>getRequirements()</code> and
     * <code>submitRequirements()</code> to pass the credentials
     * needed for authentication by the plugin authentication modules.
     * The method <code>getStatus()</code> returns the
     * authentication status.
     *
     * @param ssoToken single sign on token representing the resource's 
     *        previous authenticated session.
     * @param forceAuth indicates that authentication preocess has to be 
     *        restarted and given single sign on token will be used and new 
     *        session will not be created.
     * @throws AuthLoginException if <code>AuthContext</code> creation fails.
     *         This exception is kept for backward compatibility only.
     *
     * @supported.api
     */
    public AuthContext(SSOToken ssoToken, boolean forceAuth) throws 
        AuthLoginException {
        this.ssoToken = ssoToken;
        this.forceAuth = forceAuth;
    }
    
    /**
     * Starts the login process for the given <code>AuthContext</code> object.
     *
     * @exception AuthLoginException if an error occurred during login.
     *
     * @supported.api
     */
    public void login() throws AuthLoginException {
        login(null, null, null, false, null, null, null);
    }

    /**
     * Starts the login process for the given <code>AuthContext</code> object.
     *
     * @param request The HttpServletRequest that was sent to start the authentication process.
     * @param response The corresponding HttpServletResponse for the HttpServletRequest.
     * @throws AuthLoginException If an error occurred during login.
     *
     * @supported.api
     */
    public void login(HttpServletRequest request, HttpServletResponse response) throws AuthLoginException {
        login(null, null, null, null, request, response);
    }
    
    /**
     * Starts the login process for the given <code>AuthContext</code> object
     * identified by the index type and index name. The <code>IndexType</code>
     * defines the possible kinds of "objects" or "resources" for which an
     * authentication can be performed. Currently supported index types are
     * users, roles, services (or application), levels, resources and
     * mechanism/authentication modules.
     *
     * @param type Authentication index type.
     * @param indexName Authentication index name.
     * @exception AuthLoginException if an error occurred during login.
     *
     * @supported.api
     */
    public void login(IndexType type, String indexName)
            throws AuthLoginException {
        login(type, indexName, null, false, null, null, null);
    }
    
    /**
     * Starts the login process for the given <code>AuthContext</code> object
     * identified by the index type and index name.
     * The <code>IndexType</code> defines the possible kinds of "objects"
     * or "resources" for which an authentication can
     * be performed. Currently supported index types are
     * users, roles, services (or application), levels, resources and mechanism.
     * The <code>pCookieMode</code> indicates that a persistent cookie exists
     * for this request.
     *
     * @param type authentication index type.
     * @param indexName authentication index name.
     * @param pCookieMode <code>true</code> if persistent Cookie exists.
     * @exception AuthLoginException if an error occurred during login
     */
    public void login(IndexType type, String indexName, boolean pCookieMode)
            throws AuthLoginException {
        login(type, indexName, null, pCookieMode, null, null, null);
    }
    
    /**
     * Starts the login process for the given <code>AuthContext</code> object
     * identified by the index type and index name.
     * The <code>IndexType</code> defines the possible kinds of "objects"
     * or "resources" for which an authentication can
     * be performed. Currently supported index types are
     * users, roles, services (or application), levels, resources and mechanism.
     * It allows the caller to pass in the desired locale for this request.
     *
     * @param type authentication index type
     * @param indexName authentication index name
     * @param locale locale setting
     *
     * @exception AuthLoginException if an error occurred during login
     */
    public void login(IndexType type, String indexName, String locale)
            throws AuthLoginException {
        login(type, indexName, null, false, null, locale);
    }

    /**
     * Starts the login process for the given <code>AuthContext</code> object
     * identified by the index type and index name and also completes
     * the login process by submitting the given User credentials
     * in the form of Callbacks.
     * The <code>IndexType</code> defines the possible kinds of "objects"
     * or "resources" for which an authentication can
     * be performed. Currently supported index types are
     * users, roles, services (or application), levels, resources and mechanism.
     * <p>
     * NOTE : This is a simplified wrapper method to eliminate multi-step calls
     * to 'login' and submit credentials. This method is useful and will work
     * only for those authentication modules which require only one set of
     * callbacks or one page. This method can not be used to authenticate to
     * authentication modules which require user interaction or multiple pages.
     *
     * @param type Authentication index type.
     * @param indexName Authentication index name.
     * @param userInfo User information/credentials in the form of array of
     *        <code>Callback</code> objects. The <code>Callback</code> objects
     *        array must be in the same order as defined in the authentication
     *        module properties file, otherwise authentication module code will
     *        not work.
     * @return single-sign-on token for the valid user after successful
     *         authentication.
     * @exception AuthLoginException if an error occurred during login.
     */
    public SSOToken login(IndexType type, String indexName, Callback[] userInfo)
            throws AuthLoginException {
        login(type, indexName, null, false, null, null, null);
        
        SSOToken ssoToken = null;
        Callback[] callbacks = null;
        
        while (hasMoreRequirements()) {
            callbacks = getRequirements();
            
            if (callbacks != null) {
                try {
                    submitRequirements(userInfo);
                } catch (Exception e) {
                    if (authDebug.messageEnabled()) {
                        authDebug.message(
                            "Error: submitRequirements with userInfo : "
                        + e.getMessage());
                    }
                    throw new AuthLoginException(e);
                }
            }
        }
        try {
            if (getStatus() == AuthContext.Status.SUCCESS) {
                ssoToken = getSSOToken();
            }
        } catch (Exception e) {
            if (authDebug.messageEnabled()) {
                authDebug.message("Error: getSSOToken : " + e.getMessage());
            }
            throw new AuthLoginException(e);
        }
        return ssoToken;
    }
    
    /**
     * Starts the login process for the given <code>AuthContext</code> object
     * identified by the index type and index name with default parameters.
     * The <code>IndexType</code> defines the possible kinds of "objects"
     * or "resources" for which an authentication can be performed. Currently
     * supported index types are users, roles, services (or application),
     * levels, resources and mechanism/authentication modules.
     *
     * @param indexType authentication index type.
     * @param indexName authentication index name.
     * @param params contains the default values for the callbacks. The order
     *        of this array matches the callbacks order for this login process.
     *        value for the <code>PasswordCallback</code> is also in String
     *        format, it will be converted to <code>char[]</code> when it is
     *        set to the callback. Internal processing for this string array
     *        uses <code>|</code> as separator. Hence <code>|</code> should not
     *        be used in these default values. Currently only
     *        <code>NameCallback</code> and <code>PasswordCallback</code> are
     *        supported.
     * @exception AuthLoginException if an error occurred during login.
     *
     * @supported.api
     */
    public void login(IndexType indexType, String indexName, String[] params)
            throws AuthLoginException {
        login(indexType, indexName, params, false, null, null, null);
    }

    public void login(IndexType indexType,
                      String indexName,
                      String[] params,
                      HttpServletRequest request,
                      HttpServletResponse response)
            throws AuthLoginException {
        login(indexType, indexName, params, false, null, request, response);
    }
    
    /**
     * Starts the login process for the given <code>AuthContext</code> object
     * identified by the index type and index name with certain parameters
     * and environment map.
     * The <code>IndexType</code> defines the possible kinds of "objects"
     * or "resources" for which an authentication can be performed. Currently
     * supported index types are users, roles, services (or application),
     * levels, modules and resources.
     *
     * @param indexType authentication index type.
     * @param indexName authentication index name.
     * @param params contains the default values for the callbacks. The order
     *        of this array matches the callbacks order for this login process.
     *        value for the <code>PasswordCallback</code> is also in String
     *        format, it will be converted to <code>char[]</code> when it is
     *        set to the callback. Internal processing for this string array
     *        uses <code>|</code> as separator. Hence <code>|</code> should not
     *        be used in these default values. Currently only
     *        <code>NameCallback</code> and <code>PasswordCallback</code> are
     *        supported.
     * @param envMap contains the environment key/value pairs. Key is a String
     *        object indicating the property name, value is a Set of String
     *        values for the property. Currenty this parameter only applicable
     *        when the indexTye is <code>AuthContext.IndexType.RESOURCE</code>.
     * @exception AuthLoginException if an error occurred during login.
     *
     * @supported.api
     */
    public void login(IndexType indexType, String indexName, 
        String[] params, Map envMap)
            throws AuthLoginException {
        login(indexType, indexName, params, false, envMap, null, null);
    }

    public void login(IndexType indexType,
                      String indexName,
                      String[] params,
                      Map envMap,
                      HttpServletRequest request,
                      HttpServletResponse response)
            throws AuthLoginException {
        login(indexType, indexName, params, false, envMap, request, response);
    }
    
    private void login(
        IndexType indexType,
        String indexName,
        String[] params,
        boolean pCookie,
        Map envMap,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws AuthLoginException {
        if (clientLocale == null) {
            login(indexType, indexName, params, pCookie, envMap, null, request, response);
        } else {
            String localeStr = clientLocale.toString();
            login(indexType, indexName, params, pCookie, envMap, localeStr, request, response);
        }
    }

    private void login(
        IndexType indexType,
        String indexName,
        String[] params,
        boolean pCookie,
        Map envMap,
        String locale
    ) throws AuthLoginException {
        login(indexType, indexName, params, false, envMap, locale, null, null);
    }

    private void login(
        IndexType indexType,
        String indexName,
        String[] params,
        boolean pCookie,
        Map envMap,
        String locale,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws AuthLoginException {
        if (ssoToken != null) {
            try {
                organizationName = ssoToken.getProperty(
                    ISAuthConstants.ORGANIZATION);
                ssoTokenID = ssoToken.getTokenID().toString();
                authURL = Session.getSession(
                    new SessionID(ssoTokenID)).getSessionServiceURL();
            } catch (Exception e) {
                throw new AuthLoginException(e);
            }
        }
        
        if (authURL != null) {
            authServiceURL = getAuthServiceURL(authURL.getProtocol(),
                authURL.getHost(), Integer.toString(authURL.getPort()),
                authURL.getPath());
        }
        
        AuthLoginException authException = null;
        try {
            if (authServiceURL == null) {
                authServiceURL = getAuthServiceURL( server_proto,
                    server_host, server_port, server_uri);
            }
            if (authServiceURL != null) {
            	if (authDebug.messageEnabled()) {
            	    authDebug.message("AuthContext.login : runLogin against "
            		    + authServiceURL);
            	}
                runLogin(indexType, indexName, params, pCookie, envMap, locale,
                        request, response);
                return;
            }
        } catch (AuthLoginException e) {
            authException = e;
            authDebug.error("Failed to login to " + authServiceURL);
        } catch (Exception e) {
            authDebug.error("Failed to login to " + authServiceURL
                + ": " + e.getMessage(),e);
        }
        
        if (authURL == null) {
            // failover when authURL is not specified
            Vector serviceURLs = null;
            try {
                serviceURLs = WebtopNaming.getServiceAllURLs(
                AuthXMLTags.AUTH_SERVICE);
            } catch (Exception e) {
                throw new AuthLoginException(amAuthContext, "loginError",
                new Object[]{e.getMessage()});
            }
            
            if (authDebug.messageEnabled()) {
                authDebug.message("Org Name : " + organizationName);
                authDebug.message("ssoTokenID: " + ssoTokenID);
                authDebug.message("serviceURLs: " + serviceURLs);
            }
            
            if (serviceURLs != null) {
                serviceURLs.remove(authServiceURL);
                for (Enumeration e = serviceURLs.elements();
                e.hasMoreElements(); ) {
                    authServiceURL = (URL)e.nextElement();
                    try {
                        runLogin(indexType, indexName, params, pCookie, 
                            envMap, locale, request, response);
                        return;
                    } catch (AuthLoginException ex) {
                        authException = ex;
                        authDebug.error("Failed to login in failover with " +
                        authServiceURL + ": " + ex.getMessage());
                    }
                }
            }
        }
        authDebug.error("Authentication failed.");
        if (authException != null) {
            throw authException;
        } else {
            throw new AuthLoginException(amAuthContext, "loginError",null);
        }
    }
    
    
    private void runLogin(
        IndexType indexType,
        String indexName,
        String[] params,
        boolean pCookie,
        Map envMap,
        String locale,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws AuthLoginException {
        if (!localFlag) {
            setLocalFlag(authServiceURL);
        }

        if (appSSOToken == null) {
            if (!((indexType == IndexType.MODULE_INSTANCE) && 
                (indexName.equals("Application")))){
                appSSOToken = getAppSSOToken(false);
            }
        }
        
        if (localFlag) {
            try {
                if (ssoTokenID == null) {
                    acLocal = com.sun.identity.authentication.service.AuthUtils.
                        getAuthContext(organizationName);
                } else {
                    if (authDebug.messageEnabled()) {
                        authDebug.message("AuthContext.runLogin: "
                        + "ForceAuth = "+forceAuth);
                    }
                    acLocal = com.sun.identity.authentication.service.AuthUtils.
                        getAuthContext(organizationName, ssoTokenID, false, 
                            null, null, null, forceAuth);
                }
                LoginState loginState = acLocal.getLoginState();
                /*
                 * Set both the HttpRequest and HttpResponse on the login state so they are accessible by the Auth
                 * Modules.
                 */
                if (request != null) {
                    loginState.setHttpServletRequest(request);
                    Hashtable hashtable = AuthClientUtils.parseRequestParameters(request);
                    loginState.setParamHash(hashtable);
                }
                if (response != null) {
                    loginState.setHttpServletResponse(response);
                }
                if (hostName != null) {
                    acLocal.getLoginState().setClient(hostName);
                }
                acLocal.login(indexType, indexName, pCookie, envMap, locale);
            } catch (AuthException e) {
                throw new AuthLoginException(e);
            }
            if (acLocal.getStatus().equals(Status.SUCCESS)) {
                onSuccessLocal();
            }
            return;
        }
        
        // Check if 7.0 RR stype protocol needs to be used
        // This will setup NewAuthContext and authHandles
        if (useOldStyleRemoteAuthentication) {
            runRemoteOldAuthContext();
            if (loginException != null) {
                throw loginException;
            }
        }
        // Run Login
        runRemoteLogin(indexType, indexName, params, pCookie, envMap, locale,
                request, response);
        // reset the retry count
        retryRunLogin = DEFAULT_RETRY_COUNT;
        
        if (authDebug.messageEnabled()) {
            authDebug.message("useNewStyleRemoteAuthentication : " 
                + useNewStyleRemoteAuthentication);
            authDebug.message("useOldStyleRemoteAuthentication : " 
                + useOldStyleRemoteAuthentication);
            authDebug.message("receivedDocument : " + receivedDocument);
            authDebug.message("loginException : " + loginException);
        }

        // If "Login" fails and we have not set 6.3, 7.0 RR style protocol
        // the server could be either 6.3 or 7.0 RR. Hence try "NewAuthContext"
        // and then "Login"
        if (!useNewStyleRemoteAuthentication &&
            !useOldStyleRemoteAuthentication &&
            (receivedDocument == null || 
            (getAuthenticationHandle(receivedDocument)).equals("null")) && 
            loginException != null) {
            if (authDebug.messageEnabled()) {
                authDebug.message("AuthContext: trying 6.3 style remote " +
                    "AuthN and setting the flag to use 6.3 style");
            }
            useOldStyleRemoteAuthentication = true;
            // Server could be either 6.3 or 7.0 RR, try old style
            // Construct the Request XML with New AuthContext parameters
            loginException = null;  // Reset loginException
            runRemoteOldAuthContext();
            if (loginException != null) {
                throw loginException;
            }
            // Re-try login process with AuthIdentifier
            runRemoteLogin(indexType, indexName, params, pCookie, 
                envMap, locale, request, response);
            // reset the retry count
            retryRunLogin = DEFAULT_RETRY_COUNT;
        } else if (!useNewStyleRemoteAuthentication) {
            useNewStyleRemoteAuthentication = true;
        }
        if (loginException != null) {
            throw loginException;
        }
    }

    private void runRemoteLogin(IndexType indexType, String indexName,
        String[] params, boolean pCookie, Map envMap, String locale,
        HttpServletRequest req, HttpServletResponse res)
        throws AuthLoginException {
        try {
            String xmlString = null;
            // remote auth
            StringBuilder request = new StringBuilder(100);
            String[] authHandles = new String[1];
            authHandles[0] = getAuthHandle();
            if ((ssoTokenID != null) && (authHandles[0].equals("0"))) {
                if (authDebug.messageEnabled()) {
                    authDebug.message("AuthContext.runRemoteLogin: Found"
                        + " SSOTokenID " + ssoTokenID);
                }
                authHandles[0] = ssoTokenID;
            }

            request.append(MessageFormat.format(
                AuthXMLTags.XML_REQUEST_PREFIX, (Object[])authHandles));
            if (appSSOToken != null) {
                request.append(AuthXMLTags.APPSSOTOKEN_BEGIN);
                request.append(appSSOToken.getTokenID().toString()).
                    append(AuthXMLTags.APPSSOTOKEN_END);
            }
            request.append(AuthXMLTags.LOGIN_BEGIN);

            if (!useOldStyleRemoteAuthentication) {
                request.append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.ORG_NAME_ATTR)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append(XMLUtils.escapeSpecialCharacters(organizationName))
                    .append(AuthXMLTags.QUOTE);
                if (hostName != null) {
                    request.append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.HOST_NAME_ATTR)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append(XMLUtils.escapeSpecialCharacters(hostName))
                    .append(AuthXMLTags.QUOTE);
                }
                if ((locale != null) && (locale.length() > 0)) {
                        request.append(AuthXMLTags.SPACE)
                        .append(AuthXMLTags.LOCALE)
                        .append(AuthXMLTags.EQUAL)
                        .append(AuthXMLTags.QUOTE)
                        .append(XMLUtils.escapeSpecialCharacters(locale))
                        .append(AuthXMLTags.QUOTE);
                }
                if (forceAuth) {
                    request.append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.FORCE_AUTH_ATTR)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append("true")
                    .append(AuthXMLTags.QUOTE);
                }
            }
            request.append(AuthXMLTags.ELEMENT_END);

            if (indexType != null) {
                request.append(AuthXMLTags.INDEX_TYPE_PAIR_BEGIN)
                    .append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.INDEX_TYPE)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE);

                if (indexType == IndexType.USER) {
                    request.append(AuthXMLTags.INDEX_TYPE_USER_ATTR);
                } else if (indexType == IndexType.ROLE) {
                    request.append(AuthXMLTags.INDEX_TYPE_ROLE_ATTR);
                } else if (indexType == IndexType.SERVICE) {
                    request.append(AuthXMLTags.INDEX_TYPE_SVC_ATTR);
                } else if (indexType == IndexType.MODULE_INSTANCE) {
                    request.append(AuthXMLTags.INDEX_TYPE_MODULE_ATTR);
                } else if (indexType == IndexType.LEVEL) {
                    request.append(AuthXMLTags.INDEX_TYPE_LEVEL_ATTR);
                } else if (indexType == IndexType.COMPOSITE_ADVICE) {
                    request.append(
                        AuthXMLTags.INDEX_TYPE_COMPOSITE_ADVICE_ATTR);
                } else if (indexType == IndexType.RESOURCE) {
                    request.append(AuthXMLTags.INDEX_TYPE_RESOURCE);
                }
                request.append(AuthXMLTags.QUOTE)
                    .append(AuthXMLTags.ELEMENT_END)
                    .append(AuthXMLTags.INDEX_NAME_BEGIN)
                    .append(indexName)
                    .append(AuthXMLTags.INDEX_NAME_END)
                    .append(AuthXMLTags.INDEX_TYPE_PAIR_END);
            }

            if (locale != null && locale.length() > 0) {
                request.append(AuthXMLTags.LOCALE_BEGIN);
                request.append(locale);
                request.append(AuthXMLTags.LOCALE_END);
            }

            if (params != null) {
                StringBuilder paramString = new StringBuilder();
                for (int i = 0; i < params.length; i++) {
                    if (i != 0 ) {
                        paramString.append(ISAuthConstants.PIPE_SEPARATOR);
                    }
                    paramString.append(params[i]);
                }
                request.append(AuthXMLTags.PARAMS_BEGIN)
                    .append(paramString.toString())
                    .append(AuthXMLTags.PARAMS_END);
            }
            if ((envMap != null) && !envMap.isEmpty()) {
                StringBuilder envString = new StringBuilder();
                Iterator keys = envMap.keySet().iterator();
                while (keys.hasNext()) {
                    // convert Map to XMLString as follows:
                    // <EnvValue>keyname|value1|value2|...</EnvValue>
                    String keyName = (String) keys.next();
                    Set values = (Set) envMap.get(keyName);
                    if ((values != null) && !values.isEmpty()) {
                        envString.append(AuthXMLTags.ENV_AV_BEGIN).append(
                             AuthClientUtils.escapePipe(keyName));
                        Iterator iter = values.iterator();
                        while (iter.hasNext()) {
                            envString.append(ISAuthConstants.PIPE_SEPARATOR)
                                .append(AuthClientUtils.escapePipe(
                                    XMLUtils.escapeSpecialCharacters(
                                    (String) iter.next())));
                        }
                        envString.append(AuthXMLTags.ENV_AV_END);
                    }
                }
                request.append(AuthXMLTags.ENV_BEGIN)
                    .append(envString.toString())
                    .append(AuthXMLTags.ENV_END);
            }
            request.append(AuthXMLTags.LOGIN_END);

            if (includeReqRes) {
                request.append(AuthXMLTags.REMOTE_REQUEST_RESPONSE_START)
                .append(AuthXMLTags.HTTP_SERVLET_REQUEST_START);
                String encObj = "";

                if (req != null) {
                    try {
                        encObj = AuthXMLUtils.serializeToString(new RemoteHttpServletRequest(req));
                    } catch (IOException ioe) {
                        authDebug.error("AuthXMLUtils::runRemoteLogin Unable to serailize http request", ioe);
                    }

                    if (authDebug.messageEnabled()) {
                        authDebug.message("req=" + new RemoteHttpServletRequest(req).toString());
                    }

                    request.append(encObj);
                }

                request.append(AuthXMLTags.HTTP_SERVLET_REQUEST_END);
                request.append(AuthXMLTags.HTTP_SERVLET_RESPONSE_START);

                if (res != null) {
                    encObj = "";

                    try {
                        encObj = AuthXMLUtils.serializeToString(new RemoteHttpServletResponse(res));
                    } catch (IOException ioe) {
                        authDebug.error("AuthXMLUtils::runRemoteLogin Unable to serailize http response", ioe);
                    }

                    if (authDebug.messageEnabled()) {
                        authDebug.message("res=" + res);
                    }

                    request.append(encObj);
                }

                request.append(AuthXMLTags.HTTP_SERVLET_RESPONSE_END)
                .append(AuthXMLTags.REMOTE_REQUEST_RESPONSE_END);
            } else {
                if (authDebug.messageEnabled()) {
                    authDebug.message("Not including req/res " + includeReqRes);
                }
            }

            request.append(AuthXMLTags.XML_REQUEST_SUFFIX);
            xmlString = request.toString();

            // process the request, which will check for exceptions
            // and also get the authentication handle ID
            receivedDocument = processRequest(xmlString);

            // Check set the login status
            checkAndSetLoginStatus();

            // if the app token was refreshed, retry remote login
            if (loginException != null &&
                loginException.getErrorCode().equals(AMAuthErrorCode.REMOTE_AUTH_INVALID_SSO_TOKEN) &&
                retryRunLogin > 0) {
                retryRunLogin--;

                if (authDebug.messageEnabled()) {
                    authDebug.message("Run remote login failed due to expired app token, retying");
                }

                // reset as we are starting again
                loginStatus = Status.IN_PROGRESS;
                runRemoteLogin(indexType, indexName, params, pCookie, envMap, locale, req,  res);
            }
        } catch (AuthLoginException le) {
            // Login has failed
            loginStatus = Status.FAILED;
            loginException = le;
        }
    }

    private void runRemoteOldAuthContext() throws AuthLoginException {
        try {
            StringBuilder request = new StringBuilder(100);
            String[] objs = { "0" };
            if (ssoTokenID != null) {
                objs[0] = ssoTokenID;
            }
            request.append(MessageFormat.format(
                AuthXMLTags.XML_REQUEST_PREFIX, (Object[])objs))
                .append(AuthXMLTags.NEW_AUTHCONTEXT_BEGIN)
                .append(AuthXMLTags.SPACE)
                .append(AuthXMLTags.ORG_NAME_ATTR)
                .append(AuthXMLTags.EQUAL)
                .append(AuthXMLTags.QUOTE)
                .append(XMLUtils.escapeSpecialCharacters(organizationName))
                .append(AuthXMLTags.QUOTE)
                .append(AuthXMLTags.ELEMENT_END)
                .append(AuthXMLTags.NEW_AUTHCONTEXT_END)
                .append(AuthXMLTags.XML_REQUEST_SUFFIX);
            // process the request, which will check for exceptions
            // and also get the authentication handle ID
            receivedDocument = processRequest(request.toString());

            // Check set the login status
            checkAndSetLoginStatus();
        } catch (AuthLoginException le) {
            // Login has failed
            loginStatus = Status.FAILED;
            loginException = le;
        }
    }
    
    /**
     * Returns the set of Principals or Subject the user has been
     * authenticated as.
     * This should be invoked only after successful authentication.
     *
     * @return <code>Subject</code> for the authenticated User.
     *         If the authentication fails or the authentication is in process,
     *         this will return <code>null</code>.
     *
     * @supported.api
     */
    public Subject getSubject() {
        if (localFlag) {
            if (!acLocal.getStatus().equals(Status.SUCCESS)) {
                return (null);
            }
            return (acLocal.getSubject());
        } else {
            if (!loginStatus.equals(Status.SUCCESS)) {
                return (null);
            }
            return (getSubject(receivedDocument));
        }
    }

   /**
    * Returns a <code>Map</code> object that
    * that contains cookies set by AM server
    *
    * @return a <code>Map</code> of cookie name and
    * <code>Cookie</code> object.
    */
    public Map getCookieTable() {
        return cookieTable;
    }
    
    /**
     * Returns <code>true</code> if the login process requires more
     * information from the user to complete the authentication.
     * <p>
     * NOTE: This method has to be called as a condition of a
     * <code>while</code> loop in order to complete the authentication process
     * and get the correct <code>Status</code> after submitting the
     * requirements.
     *
     * @return <code>true</code> if more credentials are required from the user.
     *
     * @supported.api
     */
    public boolean hasMoreRequirements() {
        if (localFlag) {
            return (acLocal.hasMoreRequirements(false));
        } else {
            if ((!loginStatus.equals(Status.IN_PROGRESS)) ||
            ((getCallbacks(receivedDocument, false)) == null)) {
                return (false);
            }
            return (true);
        }
    }
    
    /**
     * Returns <code>true</code> if the login process requires more information
     * from the user to complete the authentication.
     *
     * NOTE: This method has to be called as a condition of a <ode>while</code>
     * loop in order to complete the authentication process and get the correct
     * <code>Status</code> after submitting the requirements.
     *
     * @param noFilter flag indicates whether to filter
     *        <code>PagePropertiesCallback</code> or not. Value
     *        <code>true</code> will not filter
     *        <code>PagePropertiesCallback</code>.
     * @return <code>true</code> if more credentials are required from the user.
     *
     * @supported.api
     */
    public boolean hasMoreRequirements(boolean noFilter) {
        if (localFlag) {
            return (acLocal.hasMoreRequirements(noFilter));
        } else {
            if ((!loginStatus.equals(Status.IN_PROGRESS)) ||
            ((getCallbacks(receivedDocument, noFilter)) == null)) {
                return (false);
            }
            return (true);
        }
    }
    
    /**
     * Returns an array of <code>Callback</code> objects that must be populated
     * by the user and returned back. These objects are requested by the
     * authentication plug-ins, and these are usually displayed to the user.
     * The user then provides the requested information for it to be
     * authenticated.
     *
     * @return an array of <code>Callback</code> objects requesting credentials
     *         from user
     *
     * @supported.api
     */
    public Callback[] getRequirements() {
        if (localFlag) {
            if (!acLocal.getStatus().equals(Status.IN_PROGRESS)) {
                return (null);
            }
            return (acLocal.getRequirements(false));
        } else {
            if (!loginStatus.equals(Status.IN_PROGRESS)) {
                return (null);
            }
            return (getCallbacks(receivedDocument, false));
        }
    }
    
    /**
     * Returns an array of <code>Callback</code> objects that
     * must be populated by the user and returned back.
     * These objects are requested by the authentication plug-ins,
     * and these are usually displayed to the user. The user then provides
     * the requested information for it to be authenticated.
     *
     * @param noFilter boolean flag indicating whether to filter
     * <code>PagePropertiesCallback</code> or not. Value <code>true</code> will
     * not filter <code>PagePropertiesCallback</code>.
     *
     * @return an array of <code>Callback</code> objects requesting credentials
     * from user
     *
     * @supported.api
     */
    public Callback[] getRequirements(boolean noFilter) {
        if (localFlag) {
            if (!acLocal.getStatus().equals(Status.IN_PROGRESS)) {
                return (null);
            }
            return (acLocal.getRequirements(noFilter));
        } else {
            if (!loginStatus.equals(Status.IN_PROGRESS)) {
                return (null);
            }
            return (getCallbacks(receivedDocument, noFilter));
        }
    }

    /**
     * Fetches the remote request from the context
     *
     * @return The Http Servlet Request
     */
    public HttpServletRequest getRemoteRequest() {
        return remoteRequest;
    }

    /**
     * Fetches the remote response from the context
     *
     * @return The Http Servlet Response
     */
    public HttpServletResponse getRemoteResponse() {
        return remoteResponse;
    }
    
    /**
     * Submits the populated <code>Callback</code> objects to the
     * authentication plug-in modules. Called after <code>getRequirements</code>
     * method and obtaining user's response to these requests.
     *
     * @param info Array of <code>Callback</code> objects.
     *
     * @supported.api
     */
    public void submitRequirements(Callback[] info) {
        submitRequirements(info, null, null);
    }

    public void submitRequirements(Callback[] info, HttpServletRequest request,
            HttpServletResponse response) {
        if (authDebug.messageEnabled()) {
            authDebug.message("submitRequirements with Callbacks : " + info);
        }
        
        if (localFlag) {
            // Check if we are still in login session
            if (!acLocal.getStatus().equals(Status.IN_PROGRESS)) {
                return;
            }
            acLocal.submitRequirements(info);
            if (acLocal.getStatus().equals(Status.SUCCESS)) {
                onSuccessLocal();
            }
            return;
        } else {
            // Check if we are still in login session
            if (!loginStatus.equals(Status.IN_PROGRESS)) {
                return;
            }
            
            // Construct the XML
            try {
                StringBuilder xml = new StringBuilder(100);
                String[] authHandles = new String[1];
                authHandles[0] = getAuthenticationHandle(receivedDocument);
                xml.append(MessageFormat.format(
                    AuthXMLTags.XML_REQUEST_PREFIX,(Object[])authHandles));
                if (appSSOToken != null) {
                    xml.append(AuthXMLTags.APPSSOTOKEN_BEGIN);
                    xml.append(appSSOToken.getTokenID().toString()).
                        append(AuthXMLTags.APPSSOTOKEN_END);
                }
                xml.append(AuthXMLTags.SUBMIT_REQS_BEGIN)
                .append(AuthXMLUtils.getXMLForCallbacks(info));

                if (clientLocale != null) {
                    String localeStr = clientLocale.toString();
                    if ((localeStr != null) && (localeStr.length() > 0)) {
                        xml.append(AuthXMLTags.LOCALE_BEGIN)
                        .append(XMLUtils.escapeSpecialCharacters(localeStr))
                        .append(AuthXMLTags.LOCALE_END);
                    }
                }

                xml.append(AuthXMLTags.SUBMIT_REQS_END);

                if (includeReqRes) {
                    // serialized request and response objects
                    xml.append(AuthXMLTags.REMOTE_REQUEST_RESPONSE_START)
                    .append(AuthXMLTags.HTTP_SERVLET_REQUEST_START);
                    String encObj = "";

                    if (request != null) {
                        try {
                            encObj = AuthXMLUtils.serializeToString(new RemoteHttpServletRequest(request));
                        } catch (IOException ioe) {
                            authDebug.error("AuthXMLUtils::runRemoteLogin Unable to serailize http request", ioe);
                        }

                        if (authDebug.messageEnabled()) {
                            authDebug.message("req=" + request);
                        }

                        xml.append(encObj);
                    }

                    xml.append(AuthXMLTags.HTTP_SERVLET_REQUEST_END);
                    xml.append(AuthXMLTags.HTTP_SERVLET_RESPONSE_START);

                    if (response != null) {
                        encObj = "";

                        try {
                            encObj = AuthXMLUtils.serializeToString(new RemoteHttpServletResponse(response));
                        } catch (IOException ioe) {
                            authDebug.error("AuthXMLUtils::runRemoteLogin Unable to serailize http response", ioe);
                        }

                        if (authDebug.messageEnabled()) {
                            authDebug.message("res=" + response);
                        }

                        xml.append(encObj);
                    }

                    xml.append(AuthXMLTags.HTTP_SERVLET_RESPONSE_END)
                    .append(AuthXMLTags.REMOTE_REQUEST_RESPONSE_END);
                }
                xml.append(AuthXMLTags.XML_REQUEST_SUFFIX);
                
                // Send the request to be processes
                receivedDocument = processRequest(xml.toString());
                
                // Check set the login status
                checkAndSetLoginStatus();
            } catch (AuthLoginException le) {
                // Login has failed
                loginStatus = Status.FAILED;
                loginException = le;
            }
        }
    }
    
    /**
     * Logs out the user and also invalidates the single sign on token
     * associated with this <code>AuthContext</code>.
     *
     * @throws AuthLoginException if an error occurred during logout.
     *
     * @supported.api
     */
    public void logout() throws AuthLoginException {
        if (localFlag) {
            acLocal.logout();
            return;
        }

        // Construct the XML
        try {
            StringBuilder xml = new StringBuilder(100);
            String[] authHandles = new String[1];
            authHandles[0] = getAuthenticationHandle(receivedDocument);
            xml.append(MessageFormat.format(AuthXMLTags.XML_REQUEST_PREFIX,
            (Object[])authHandles));
            if (appSSOToken != null) {
                xml.append(AuthXMLTags.APPSSOTOKEN_BEGIN);
                xml.append(appSSOToken.getTokenID().toString()).
                    append(AuthXMLTags.APPSSOTOKEN_END);
            }
            xml.append(AuthXMLTags.LOGOUT_BEGIN)
               .append(AuthXMLTags.LOGOUT_END)
               .append(AuthXMLTags.XML_REQUEST_SUFFIX);
            
            // Send the request to be processes
            receivedDocument = processRequest(xml.toString());
            
            // Check set the login status
            checkAndSetLoginStatus();
        } catch (AuthLoginException le) {
            // Login has failed
            loginStatus = Status.FAILED;
            loginException = le;
        }
    }

    /**
     * Logs out the user and also invalidates the single sign on token
     * associated with this <code>AuthContext</code>.
	 *
	 * This method causes the logout to happen on the server and the 
	 * correct SPI hooks to be called.
     *
     * @throws AuthLoginException if an error occurred during logout.
     *
     * @supported.api
     */
    public void logoutUsingTokenID()
    throws AuthLoginException {
        if (localFlag) {
            return;
        }

        if (ssoToken != null) {
            try {
                organizationName = ssoToken.getProperty(
                    ISAuthConstants.ORGANIZATION);
                ssoTokenID = ssoToken.getTokenID().toString();
                authURL = Session.getSession(
                    new SessionID(ssoTokenID)).getSessionServiceURL();
            } catch (Exception e) {
                throw new AuthLoginException(e);
            }
        }

        if (authURL != null) {
            authServiceURL = getAuthServiceURL(authURL.getProtocol(),
                authURL.getHost(), Integer.toString(authURL.getPort()),
                authURL.getPath());
        }


        // Construct the XML
        try {
            StringBuilder xml = new StringBuilder(100);
            String[] authHandles = new String[1];
            authHandles[0] = ssoToken.getTokenID().toString();
            xml.append(MessageFormat.format(AuthXMLTags.XML_REQUEST_PREFIX,
            (Object[]) authHandles));
            if (appSSOToken != null) {
                xml.append(AuthXMLTags.APPSSOTOKEN_BEGIN);
                xml.append(appSSOToken.getTokenID().toString()).
                    append(AuthXMLTags.APPSSOTOKEN_END);
            }
            xml.append(AuthXMLTags.LOGOUT_BEGIN)
            .append(AuthXMLTags.LOGOUT_END)
            .append(AuthXMLTags.XML_REQUEST_SUFFIX);

            // Send the request to be processes
            receivedDocument = processRequest(xml.toString());

            // Check set the login status
            checkAndSetLoginStatus();
        } catch (AuthLoginException le) {
            // Login has failed
            loginStatus = Status.FAILED;
            loginException = le;
        }
    }
    
    /**
     * Returns login exception, if any, during the authentication process.
     * Typically set when the login fails.
     *
     * @return login exception.
     * @supported.api
     */
    public AuthLoginException getLoginException() {
        if (localFlag) {
            return (acLocal.getLoginException());
        } else {
            return (loginException);
        }
    }
    
    /**
     * Returns the Single-Sign-On (SSO) Token for the authenticated
     * user. If the user has not successfully authenticated
     * <code>Exception</code> will be thrown.
     * <p>
     * Single sign token can be used as the authenticated token.
     *
     * @return Single-Sign-On token for the valid user after successful
     *         authentication.
     * @throws L10NMessageImpl if the user is not authenticated or an error is
     *         encountered in retrieving the user's single sign on token.
     * @supported.api
     */
    public SSOToken getSSOToken() throws L10NMessageImpl {
        if (localFlag) {
            if (!acLocal.getStatus().equals(Status.SUCCESS)) {
                throw new L10NMessageImpl(
                    amAuthContext, "statusNotSuccess", null);
            }
            return (acLocal.getSSOToken());
        } else {
            // Get the loginStatus node
            if (!loginStatus.equals(Status.SUCCESS)) {
                throw new L10NMessageImpl(
                    amAuthContext, "statusNotSuccess", null);
            }
            Node loginStatusNode = XMLUtils.getRootNode(receivedDocument,
            AuthXMLTags.LOGIN_STATUS);
            if (loginStatusNode == null) {
                throw new L10NMessageImpl(amAuthContext, "noStatusNode", null);
            }
            
            String ssoTokenIDTmp = XMLUtils.getNodeAttributeValue(loginStatusNode,
                AuthXMLTags.SSOTOKEN);
            try {
                return new com.iplanet.sso.providers.dpro.SSOProviderImpl().
                    createSSOToken(ssoTokenIDTmp, true);
            } catch (SSOException ssoe) {
                throw new L10NMessageImpl(
                    amAuthContext, "createSSOTokenError", null);
            }
        }
    }
    
    /**
     * Returns the current status of the authentication process as
     * <code>AuthContext.Status</code>.
     *
     * @return <code>Status</code> of the authentication process.
     *
     * @supported.api
     */
    public Status getStatus() {
        if (localFlag) {
            return (acLocal.getStatus());
        } else {
            return (loginStatus);
        }
    }
    
    /**
     * Returns the current Auth Identifier of the authentication
     * process as String Session ID.
     *
     * @return Auth Identifier of the authentication process.
     */
    public String getAuthIdentifier() {
        if (localFlag) {
            return (acLocal.getAuthIdentifier());
        } else {
            return (getAuthHandle());
        }
    }
    
    /**
     * Returns the Successful Login URL for the authenticated user.
     *
     * @return the Successful Login URL for the authenticated user.
     * @throws Exception if it fails to get url for auth success
     */
    public String getSuccessURL() throws Exception {
        if (localFlag) {
            if (!acLocal.getStatus().equals(Status.SUCCESS)) {
                throw new
                L10NMessageImpl(amAuthContext, "statusNotSuccess", null);
            }
            return (acLocal.getSuccessURL());
        } else {
            // Get the loginStatus node
            if (!loginStatus.equals(Status.SUCCESS)) {
                throw new
                L10NMessageImpl(amAuthContext, "statusNotSuccess", null);
            }
            Node loginStatusNode = XMLUtils.getRootNode(receivedDocument,
            AuthXMLTags.LOGIN_STATUS);
            if (loginStatusNode == null) {
                throw new L10NMessageImpl(amAuthContext, "noStatusNode", null);
            }
            return (XMLUtils.getNodeAttributeValue(loginStatusNode,
            AuthXMLTags.SUCCESS_URL));
        }
    }
    
    /**
     * Returns the Failure Login URL for the authenticating user.
     *
     * @return the Failure Login URL for the authenticating user
     * @throws Exception if it fails to get url for auth failure
     */
    public String getFailureURL() throws Exception {
        if (localFlag) {
            return (acLocal.getFailureURL());
        } else {
            // Get the loginStatus node
            Node loginStatusNode = XMLUtils.getRootNode(receivedDocument,
            AuthXMLTags.LOGIN_STATUS);
            if (loginStatusNode == null) {
                throw new L10NMessageImpl(amAuthContext, "noStatusNode", null);
            }
            return (XMLUtils.getNodeAttributeValue(loginStatusNode,
            AuthXMLTags.FAILURE_URL));
        }
    }
    
    /**
     * Resets this instance of <code>AuthContext</code> object, so that a new
     * login process can be initiated. A new authentication process can started
     * using any one of the <code>login</code> methods.
     */
    public void reset() {
        loginStatus = Status.NOT_STARTED;
        //organizationName = null;
        //receivedDocument = null;
        //loginException = null;
    }
    
    /**
     * Returns the the organization name that was set during the
     * <code>AuthContext</code> constructor.
     *
     * @return Organization name in the <code>AuthContext</code>.
     *
     * @supported.api
     */
    public String getOrganizationName() {
        return (this.organizationName);
    }
    
    /**
     *
     * Returns authentication module/s instances (or plugins) configured
     * for a organization, or sub-organization name that was set during the
     * <code>AuthContext</code> constructor.
     *
     * @return Set of Module instance names.
     *
     * @supported.api
     */
    public Set getModuleInstanceNames() {
        if (authURL != null) {
            authServiceURL = getAuthServiceURL(
                authURL.getProtocol(),
                authURL.getHost(), 
                Integer.toString(authURL.getPort()),
                authURL.getPath());
        }
        if (!localFlag) {
            setLocalFlag(authServiceURL);
        }
        if (localFlag) {
            return (acLocal.getModuleInstanceNames());
        } else {
            if (authServiceURL == null) {
                try {
                    authServiceURL = getAuthServiceURL(server_proto,
                        server_host, server_port, server_uri);
                } catch (Exception e) {
                    return Collections.EMPTY_SET;
                }
            }
            sendQueryInformation(AuthXMLTags.MODULE_INSTANCE);
            
            //Receive data
            Node queryResultNode = XMLUtils.getRootNode(receivedDocument,
            AuthXMLTags.QUERY_RESULT);
            if (queryResultNode == null) {
                return (null);
            }
            
            // Iteratate through moduleInstanceNames
            HashSet moduleInstanceNames = new HashSet();
            NodeList childNodes = queryResultNode.getChildNodes();
            if ( childNodes != null ) {
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    String moduleName = XMLUtils.getValueOfValueNode(childNode);
                    moduleInstanceNames.add(moduleName);
                }
            }
            return (moduleInstanceNames);
        }
    }
    
    /**
     * Terminates an ongoing <code>login</code> call that has not yet completed.
     *
     * @exception AuthLoginException if an error occurred during abort.
     *
     * @supported.api
     */
    public void abort() throws AuthLoginException {
        if (localFlag) {
            acLocal.abort();
            return;
        }
        
        // Construct the XML
        try {
            StringBuilder xml = new StringBuilder(100);
            String[] authHandles = new String[1];
            authHandles[0] = getAuthenticationHandle(receivedDocument);
            xml.append(MessageFormat.format(AuthXMLTags.XML_REQUEST_PREFIX,
            (Object[])authHandles));
            if (appSSOToken != null) {
                xml.append(AuthXMLTags.APPSSOTOKEN_BEGIN);
                xml.append(appSSOToken.getTokenID().toString()).
                    append(AuthXMLTags.APPSSOTOKEN_END);
            }
            xml.append(AuthXMLTags.ABORT_BEGIN)
            .append(AuthXMLTags.ABORT_END)
            .append(AuthXMLTags.XML_REQUEST_SUFFIX);
            
            // Send the request to be processes
            receivedDocument = processRequest(xml.toString());
            
            // Check set the login status
            checkAndSetLoginStatus();
        } catch (AuthLoginException le) {
            // Login has failed
            loginStatus = Status.FAILED;
            loginException = le;
        }
    }
    
    /**
     * Sets the password for the certificate database.
     * It is required to call only once to initialize certificate database if
     * the password is not set in the password file (specified as
     * the value for <code>com.iplanet.am.admin.cli.certdb.passfile</code>
     * in <code>AMConfig.properties</code>). If both are set, this method will
     * overwrite the value in certificate password file.
     *
     * @param password Password for the certificate database.
     *
     * @supported.api
     */
    public static void setCertDBPassword(String password) {
        try {
            if (usingJSSEHandler) {
                Class pcbClass = (Class) Class.forName(JSSE_PASSWORD_CALLBACK);
                Object passwdCallback = (Object) pcbClass.newInstance();
                Method method =
                pcbClass.getMethod("setPassword", new Class[] { String.class });
                KeyStore keystore = (KeyStore)method.invoke(
                    passwdCallback, new Object[] { password });
            } else {
                Class initializer = Class.forName(JSS_PASSWORD_UTIL);
                Constructor initializerConstructor = initializer.getConstructor(
                    new Class[] { String.class });
                initializerConstructor.newInstance(new Object[] { password });
            }
        } catch (Exception e) {
            e.printStackTrace();
            authDebug.message("Error in setCertDBPassword : " + e.getMessage());
        }
    }
    
    
    /**
     * Returns the error template.
     *
     * @return error template.
     */
    public String getErrorTemplate() {
        if (localFlag) {
            return (acLocal.getErrorTemplate());
        } else {
            if (receivedDocument == null) {
                //something went terribly wrong, let's return with internal error template
                return AuthClientUtils.getErrorTemplate(AMAuthErrorCode.AUTH_ERROR);
            }
            String errTemplate = "";
            Node exceptionNode = XMLUtils.getRootNode(receivedDocument,
            AuthXMLTags.EXCEPTION);
            if (exceptionNode != null) {
                errTemplate = XMLUtils.getNodeAttributeValue(exceptionNode,
                AuthXMLTags.TEMPLATE_NAME);
            }
            return errTemplate;
        }
    }
    
    /**
     * Returns the error message.
     *
     * @return error message.
     */
    public String getErrorMessage() {
        if (localFlag) {
            return (acLocal.getErrorMessage());
        } else {
            if (receivedDocument == null) {
                //something went terribly wrong, let's return with internal error message
                return AuthClientUtils.getErrorMessage(AMAuthErrorCode.AUTH_ERROR);
            }
            String errMessage = null;
            Node exceptionNode = XMLUtils.getRootNode(receivedDocument,
            AuthXMLTags.EXCEPTION);
            if (exceptionNode != null) {
                errMessage = XMLUtils.getNodeAttributeValue(exceptionNode,
                AuthXMLTags.MESSAGE);
            }
            return errMessage;
        }
    }
    
    /**
     * Returns error code.
     *
     * @return error code with white space trimmed
     */
    public String getErrorCode() {
        if (localFlag) {
            return (acLocal.getErrorCode());
        } else {
            if (receivedDocument == null) {
                //something went terribly wrong
                return AMAuthErrorCode.AUTH_ERROR;
            }
            String errCode = "";
            Node exceptionNode = XMLUtils.getRootNode(receivedDocument,
            AuthXMLTags.EXCEPTION);

            if (exceptionNode != null) {
                errCode = XMLUtils.getNodeAttributeValue(exceptionNode,
                AuthXMLTags.ERROR_CODE);
            }

            if (errCode != null) {
                return errCode.trim();
            } else {
                return errCode;
            }
        }
    }
    
    /**
     * Sets the client's hostname or IP address.This could be used
     * by the policy component to restrict access to resources.
     * This method is ineffective if the "Remote Auth Security" option under 
     * the global configuration of Core Authentication Service is not enabled.
     * This method must be called before calling <code>login</code> method.
     * If it is called after calling <code>login</code> then 
     * it is ineffective.
     *
     * @param hostname hostname or ip address
     *
     * @supported.api
     */
    public void setClientHostName(String hostname) {
        this.hostName = hostname;
    }

    /**
     * Returns the client's hostname or IP address as set by 
     * setClientHostName
     * 
     * @return hostname/IP address
     *
     * @supported.api
     */
    public String getClientHostName() {
        return (hostName);
    }

    /**
     * Sets locale based on user locale preferemce.
     *
     * @param loc locale preference of user
     */
    public void setLocale (java.util.Locale loc) {
        clientLocale = loc;
    }

    /**
     * Returns locale preference set in AuthConext
     * @return - user prefered locale.
     */

    public java.util.Locale getLocale () {
        return clientLocale;
    }
    
    private AuthLoginException checkException(){
        AuthLoginException exception = null;
        String error = getErrorCode();

        // if the app token is invalid, refresh the token
        if (error != null && error.equals(AMAuthErrorCode.REMOTE_AUTH_INVALID_SSO_TOKEN)) {
            appSSOToken = getAppSSOToken(true);
        }

        if (error != null && error.length() != 0){
            exception = new AuthLoginException("amAuth", error, null);
        } else {
            error = getErrorMessage();
            if (error != null && error.length() != 0) {
                exception = new AuthLoginException(error);
            }
        }
        return exception;
    }
    
    protected void checkAndSetLoginStatus(){
        
        Node loginStatusNode = XMLUtils.getRootNode(
        receivedDocument, AuthXMLTags.LOGIN_STATUS);
        if (loginStatusNode == null) {
            loginException = checkException();
            
            if (includeReqRes) {
                remoteRequest = AuthXMLUtils.getRemoteRequest(
                    XMLUtils.getRootNode(receivedDocument, AuthXMLTags.REMOTE_REQUEST_RESPONSE));
                remoteResponse = AuthXMLUtils.getRemoteResponse(
                    XMLUtils.getRootNode(receivedDocument, AuthXMLTags.REMOTE_REQUEST_RESPONSE));
            }
        } else {
            // Get the status attribute
            String status = XMLUtils.getNodeAttributeValue(
            loginStatusNode, AuthXMLTags.STATUS);
            if (status != null) {
                if (status.equals(Status.SUCCESS.toString())) {
                    loginStatus = Status.SUCCESS;
                } else if (status.equals(Status.FAILED.toString())) {
                    loginStatus = Status.FAILED;
                    loginException = checkException();
                } else if (status.equals(Status.COMPLETED.toString())) {
                    loginStatus = Status.COMPLETED;
                } else if (status.equals(Status.IN_PROGRESS.toString())) {
                    loginStatus = Status.IN_PROGRESS;
                } else if (status.equals(Status.RESET.toString())) {
                    loginStatus = Status.RESET;
                }
            }

            if (includeReqRes) {
                remoteRequest = AuthXMLUtils.getRemoteRequest(
                    XMLUtils.getRootNode(receivedDocument, AuthXMLTags.REMOTE_REQUEST_RESPONSE));
                remoteResponse = AuthXMLUtils.getRemoteResponse(
                    XMLUtils.getRootNode(receivedDocument, AuthXMLTags.REMOTE_REQUEST_RESPONSE));
            }

            if (authDebug.messageEnabled()) {
                authDebug.message("LoginStatus : " + loginStatus);
            }
        }
    }
    
    protected void sendQueryInformation(String reqInfo) {
        // Construct the XML
        try {
            StringBuilder xml = new StringBuilder(100);
            String[] authHandles = new String[1];
            authHandles[0] = getAuthHandle();
            
            xml.append(MessageFormat.format(AuthXMLTags.XML_REQUEST_PREFIX,
            (Object[])authHandles));
            if (appSSOToken != null) {
                xml.append(AuthXMLTags.APPSSOTOKEN_BEGIN);
                xml.append(appSSOToken.getTokenID().toString()).
                    append(AuthXMLTags.APPSSOTOKEN_END);
            }
            xml.append(AuthXMLTags.QUERY_INFO_BEGIN)
               .append(AuthXMLTags.SPACE)
               .append(AuthXMLTags.REQUESTED_INFO)
               .append(AuthXMLTags.EQUAL)
               .append(AuthXMLTags.QUOTE)
               .append(reqInfo)
               .append(AuthXMLTags.QUOTE);

            if (authHandles[0].equals("0")) {
                xml.append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.ORG_NAME_ATTR)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append(XMLUtils.escapeSpecialCharacters(organizationName))
                    .append(AuthXMLTags.QUOTE);
            }

            xml.append(AuthXMLTags.ELEMENT_END)
                .append(AuthXMLTags.QUERY_INFO_END)
                .append(AuthXMLTags.XML_REQUEST_SUFFIX);
            
            // Send the request to be processes
            receivedDocument = processRequest(xml.toString());
            
            // Check set the login status
            checkAndSetLoginStatus();
        } catch (AuthLoginException le) {
            // Login has failed
            loginStatus = Status.FAILED;
            loginException = le;
        }
    }
    
    private void setLocalFlag(URL url) {
        try {
            String urlStr = url.getProtocol() + "://" + url.getHost() + ":"
                + Integer.toString(url.getPort());
            
            if (authDebug.messageEnabled()) {
                authDebug.message("in setLocalFlag(), url : " + urlStr);
                authDebug.message("AuthContext.localAuthServiceID : " +
                    localAuthServiceID);
            }
            
            if ((localAuthServiceID != null) &&
                (urlStr.equalsIgnoreCase(localAuthServiceID))
            ) {
                localFlag = true;
            }
        } catch (Exception e) {
            authDebug.error("AuthContext::setLocalFlag:: " + e);
        }
    }
    
    protected Document processRequest(String xmlRequest)
            throws AuthLoginException {
        Document doc = null;

        try {
            Request request = new Request(xmlRequest);
            RequestSet set = new RequestSet(AuthXMLTags.AUTH_SERVICE);
            set.addRequest(request);
            
            URL url = authServiceURL;
            
            if (url.getProtocol().equals("https") && (nickName != null)) {
                Class[] paramtype = {String.class};
                Object[] param = {nickName};
                String protHandler = protHandlerPkg + ".https.Handler";
                Constructor construct =
                    Class.forName(protHandler).getConstructor(paramtype);
                URLStreamHandler handler =
                    (URLStreamHandler)construct.newInstance(param);
                url = new URL(url.getProtocol(), url.getHost(), url.getPort(),
                url.getFile(), handler);
            }
            
            if (authDebug.messageEnabled()) {
                authDebug.message("Service URL : " + url.toString());
            }

            Vector responses = PLLClient.send(url, set, cookieTable);
            
            if ((responses.isEmpty()) || (responses.size() != 1)) {
                throw new L10NMessageImpl(amAuthContext, "responseError", null);
            }
            
            Response res = (Response) responses.elementAt(0);
            String responseStr = (String)res.getContent();
            
            doc = XMLUtils.getXMLDocument(
                new ByteArrayInputStream(responseStr.getBytes("UTF-8")));
        } catch (Exception e) {
            authDebug.message("error in getting service url", e);
            throw new AuthLoginException(amAuthContext, "xmlProcessError",
                null, e);
        }
        return (doc);
    }
    
    protected static void checkForException(Document document)
            throws AuthLoginException {
        Node exceptionNode = XMLUtils.getRootNode(
            document, AuthXMLTags.EXCEPTION);

        if (exceptionNode != null) {
            throw (new AuthLoginException(XMLUtils.getNodeAttributeValue(
                exceptionNode, AuthXMLTags.MESSAGE)));
        }
    }
    
    protected String getAuthenticationHandle(Document document)
            throws AuthLoginException {
        Node responseNode = XMLUtils.getRootNode(
            document, AuthXMLTags.RESPONSE);
        if (responseNode == null) {
            throw new AuthLoginException(amAuthContext, "responseError", null);
        }
        
        String authID = XMLUtils.getNodeAttributeValue(
            responseNode, AuthXMLTags.AUTH_ID_HANDLE);
        return (authID);
    }
    
    protected static Callback[] getCallbacks(
        Document document,
        boolean noFilter) {
        return (AuthXMLUtils.getCallbacks(XMLUtils.getRootNode(document,
            AuthXMLTags.CALLBACKS), noFilter));
    }
    
    protected static Subject getSubject(Document document) {
        Node loginStatusNode = XMLUtils.getRootNode(document,
            AuthXMLTags.LOGIN_STATUS);

        if (loginStatusNode == null) {
            return (null);
        }
        
        Node subjectNode = XMLUtils.getChildNode(loginStatusNode,
        AuthXMLTags.SUBJECT);
        
        if (subjectNode == null) {
            return (null);
        }
        
        String subject = XMLUtils.getValueOfValueNode(subjectNode);
        try {
            Subject sSubject = AuthXMLUtils.getDeSerializedSubject(subject);
            
            if (authDebug.messageEnabled()) {
                authDebug.message("Deserialized subject : "
                    + sSubject.toString());
            }
            return sSubject;
        } catch (Exception e) {
            authDebug.message("get Deserialized subject error : " , e);
            return null;
        }
        
    }
    
    protected static String getXMLforSubject(Subject subject) {
        if (subject == null) {
            return ("");
        }
        StringBuilder request = new StringBuilder(100);
        request.append(AuthXMLTags.SUBJECT_BEGIN);
        String serializeSubject = AuthXMLUtils.getSerializedSubject(subject);
        request.append(serializeSubject);
        request.append(AuthXMLTags.SUBJECT_END);
        return (request.toString());
    }
    
    /**
     * Returns the account lockout message. This can be either a dynamic
     * message indicating the number of tries left or the the account
     * deactivated message.
     *
     * @return account lockout message.
     */
    public String getLockoutMsg() {
        String lockoutMsg = null;
        if (localFlag) {
            lockoutMsg = acLocal.getLockoutMsg();
        } else {
            // Account Lockout Warning Check by scanning the error
            // message in the exception thrown by the server
            lockoutMsg = getErrorMessage();
            if((lockoutMsg == null) ||
                (lockoutMsg.indexOf("Account lockout") == -1)){
                lockoutMsg = "";
            }
        }
        return lockoutMsg;
    }
    
    /**
     * Returns <code>true</code> if account is lock out.
     *
     * @return <code>true</code> if account is lock out.
     */
    public boolean isLockedOut() {
        boolean isLockedOut = false;
        if (localFlag) {
            isLockedOut = acLocal.isLockedOut();
        } else {
            // TBD
        }
        
        return isLockedOut;
    }
    
    /**
     * The class <code>Status</code> defines the possible
     * authentication states during the login process.
     *
     * @supported.all.api
     */
    public static class Status extends Object {
        
        private String status;
        
        /**
         * The <code>NOT_STARTED</code> status indicates that the login process
         * has not yet started. Basically, it means that the method
         * <code>login</code> has not been called.
         */
        public static final Status NOT_STARTED = new Status("not_started");
        
        /**
         * The <code>IN_PROGRESS</code> status indicates that the login process
         * is in progress. Basically, it means that the <code>login</code>
         * method has been called and that this object is waiting for the user
         * to send authentication information.
         */
        public static final Status IN_PROGRESS = new Status("in_progress");
        
        /**
         *
         * The <code>SUCCESS</code> indicates that the login process has
         * succeeded.
         */
        public static final Status SUCCESS = new Status("success");
        
        /**
         * The <code>FAILED</code> indicates that the login process has failed.
         */
        public static final Status FAILED = new Status("failed");
        
        /**
         *
         * The <code>COMPLETED</code> indicates that the user has been
         * successfully logged out.
         */
        public static final Status COMPLETED = new Status("completed");
        
        /**
         * The <code>RESET</code> indicates that the login process has been
         * reset or re-initialized.
         */
        public static final Status RESET = new Status("reset");
        
        /**
         * The <code>ORG_MISMATCH</code> indicates that the framework
         * <code>org</code> and the <code>org</code> required by the user do
         * not match.
         */
        public static final Status ORG_MISMATCH = new Status("org_mismatch");
        
        
        private Status() {
            // do nothing
        }
        
        private Status(String s) {
            status = s;
        }
        
        /**
         * Returns the string representation of the authentication status.
         *
         * @return String representation of authentication status.
         */
        public String toString() {
            return (status);
        }
        
        /**
         * Checks if two authentication status objects are equal.
         *
         * @param authStatus Reference object with which to compare.
         * @return <code>true</code> if the objects are same.
         */
        public boolean equals(Object authStatus) {
            if (authStatus instanceof Status) {
                Status s = (Status) authStatus;
                return (s.status.equalsIgnoreCase(status));
            }
            return (false);
        }
    }
    
    /**
     * The class <code>IndexType</code> defines the possible kinds of "objects"
     * or "resources" for which an authentication can be performed.
     *
     * @supported.all.api
     */
    public static class IndexType extends Object {
        
        private String index;
        
        /**
         * The <code>USER</code> index type indicates that the index name given
         * corresponds to a user.
         */
        public static final IndexType USER = new IndexType("user");
        
        /**
         * The <code>ROLE</code> index type indicates that the index name given
         * corresponds to a role.
         */
        public static final IndexType ROLE = new IndexType("role");
        
        /**
         *
         * The <code>SERVICE</code> index type indicates that the index name
         * given corresponds to a service (or application).
         */
        public static final IndexType SERVICE = new IndexType("service");
        
        /**
         * The <code>LEVEL</code> index type indicates that the index name
         * given corresponds to a given authentication level.
         */
        public static final IndexType LEVEL = new IndexType("level");
        
        /**
         * The <code>MODULE_INSTANCE</code> index type indicates that the index
         * name given corresponds to one of the authentication modules.
         */
        public static final IndexType MODULE_INSTANCE =
            new IndexType("module_instance");
        
        /**
         * The <code>RESOURCE</code> index type indicates that the index
         * name given corresponds to a given policy protected resource URL.
         */
        public static final IndexType RESOURCE =
            new IndexType("resource");
        
        /**
         * The <code>COMPOSITE_ADVICE</code> index type indicates that the
         * index name given corresponds to string in the form of XML
         * representing different Policy Authentication conditions, example
         * <code>AuthSchemeCondition</code>, <code>AuthLevelCondition</code>,
         * etc.
         */
        public static final IndexType COMPOSITE_ADVICE =
            new IndexType("composite_advice");
        
        private IndexType() {
            // do nothing
        }
        
        private IndexType(String s) {
            index = s;
        }
        
        /**
         * Returns the string representation of the index type.
         *
         * @return String representation of index type.
         */
        public String toString() {
            return (index);
        }
        
        /**
         * Checks if two index type objects are equal.
         *
         * @param indexType Reference object with which to compare.
         *
         * @return <code>true</code> if the objects are same.
         */
        public boolean equals(Object indexType) {
            if (indexType instanceof IndexType) {
                IndexType s = (IndexType) indexType;
                return (s.index.equalsIgnoreCase(index));
            }
            return (false);
        }
    }
    
    private String getAuthHandle() {
        String handle = null;
        
        if (receivedDocument != null) {
            try {
                handle = getAuthenticationHandle(receivedDocument);
            } catch (Exception e) {
                // do nothing
            }
        }
        if ( handle == null ) {
            handle = "0";
        }
        return handle;
    }
    
    private static URL getAuthServiceURL(
        String protocol,
        String host,
        String port,
        String uri
    ) {
        URL authservice = null;
        try {
            authservice = WebtopNaming.getServiceURL(AuthXMLTags.AUTH_SERVICE,
                protocol, host, port, uri);
        } catch (Exception e) {
            authDebug.error("Failed to obtain auth service url from server: " +
            protocol + "://" + host + ":" + port);
        }
        return authservice;
    }
    
    private void onSuccessLocal() {
        if (localSessionChecked) {
            return;
        }
        SSOToken currToken = acLocal.getSSOToken();
        com.iplanet.dpro.session.service.InternalSession oldSess
            = acLocal.getLoginState().getOldSession();
        if (oldSess != null) {
            if (forceAuth) {
                try {
                    SSOTokenManager.getInstance().
                        destroyToken(currToken);
                } catch (SSOException ssoExp) {
                    authDebug.error("AuthContext.onSuccessLocal: ",
                        ssoExp);
    	
                }
                acLocal.getLoginState().setSession(oldSess);
                acLocal.getLoginState().setSid(oldSess
                    .getID());
                acLocal.getLoginState().setForceAuth(false);
                ssoToken = acLocal.getSSOToken();
                ssoTokenID = ssoToken.getTokenID().toString();
                
            } else {
                com.iplanet.dpro.session.service.SessionService.
                   getSessionService().destroyInternalSession
                   (oldSess.getID());
            }
        }
        localSessionChecked = true;
    }

    /**
     * Returns the application sso token. Can perform a check to ensure that
     * the app token is still valid (requires a session refresh call to OpenAM)
     *
     * @param refresh true if we should check with OpenAM if the app token is valid
     * @return a valid application's sso token.
     */
    private SSOToken getAppSSOToken(boolean refresh) {
        SSOToken appToken = null;

        try {
            appToken = (SSOToken) AccessController.doPrivileged(
                            AdminTokenAction.getInstance());
        } catch (AMSecurityPropertiesException aspe) {
            if (authDebug.messageEnabled()) {
                authDebug.message("AuthContext::getAppSSOToken: " +
                                  "unable to get app ssotoken " + aspe.getMessage());
            }
        }

        if (refresh) {
            // ensure the token is valid
            try {
                SSOTokenManager ssoTokenManager = SSOTokenManager.getInstance();
                ssoTokenManager.refreshSession(appToken);

                if (!ssoTokenManager.isValidToken(appToken)) {
                    if (authDebug.messageEnabled()) {
                        authDebug.message("AuthContext.getAppSSOToken(): " +
                                          "App SSOToken is invalid, retrying");
                    }

                    try {
                        appToken = (SSOToken) AccessController.doPrivileged(
                                                AdminTokenAction.getInstance());
                    } catch (AMSecurityPropertiesException aspe) {
                        if (authDebug.messageEnabled()) {
                            authDebug.message("AuthContext::getAppSSOToken: " +
                                              "unable to get app ssotoken " + aspe.getMessage());
                        }
                    }
                }
            } catch (SSOException ssoe) {
                if (authDebug.messageEnabled()) {
                    authDebug.message("AuthContext.getAppSSOToken(): " +
                                      "unable to refresh app token: " + ssoe.getL10NMessage());
                }

                try {
                    appToken = (SSOToken) AccessController.doPrivileged(
                                            AdminTokenAction.getInstance());
                } catch (AMSecurityPropertiesException aspe) {
                    if (authDebug.errorEnabled()) {
                        authDebug.error("AuthContext::getAppSSOToken: " +
                                          "unable to get app ssotoken " + aspe.getMessage());
                    }
                }
            }
        }

        if (authDebug.messageEnabled()) {
            if (appToken == null) {
                authDebug.message("Null App SSO Token");
            } else {
                authDebug.message("Obtained App Token= " + appToken.getTokenID().toString());
            }
        }

        return appToken;
    }

    public AuthContextLocal getAuthContextLocal() {
        return acLocal;
    }
}
