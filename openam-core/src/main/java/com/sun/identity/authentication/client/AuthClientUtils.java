/*
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
 * $Id: AuthClientUtils.java,v 1.40 2010/01/22 03:31:01 222713 Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2019 Open Source Solution Technology Corporation
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.sun.identity.authentication.client;

import static java.util.Arrays.asList;

import com.iplanet.am.util.AMClientDetector;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.iplanet.services.cdm.AuthClient;
import com.iplanet.services.cdm.Client;
import com.iplanet.services.cdm.ClientsManager;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.FqdnValidator;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.common.ResourceLookup;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.plugins.AuthSchemeCondition;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.security.whitelist.ValidGotoUrlExtractor;
import org.forgerock.openam.session.SessionServiceURLService;
import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.openam.utils.StringUtils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

public class AuthClientUtils {

    public static final String  DEFAULT_CLIENT_TYPE ="genericHTML";
    public static final String  COMPOSITE_ADVICE = "sunamcompositeadvice";
    private static final String  DEFAULT_CONTENT_TYPE="text/html";
    private static final String  DEFAULT_FILE_PATH = "html";
    private static final String DEFAULT_COOKIE_SUPPORT = "true";
    private static final String  DSAME_VERSION="7.0";
    public static final String ERROR_MESSAGE = "Error_Message";
    public static final String ERROR_TEMPLATE = "Error_Template";
    public static final String MSG_DELIMITER= "|";
    public static final String BUNDLE_NAME="amAuth";
    private static final String HTTP_REFERER = "Referer";

    private static boolean setRequestEncoding = false;

    private static AMClientDetector clientDetector;
    private static Client defaultClient;
    private static volatile ResourceBundle bundle;
    private static final boolean urlRewriteInPath =
        Boolean.valueOf(SystemProperties.get(
        Constants.REWRITE_AS_PATH,"")).booleanValue();
    public static final String templatePath =
        new StringBuilder().append(Constants.FILE_SEPARATOR)
    .append(ISAuthConstants.CONFIG_DIR)
    .append(Constants.FILE_SEPARATOR)
    .append(ISAuthConstants.AUTH_DIR).toString();
    private static final String rootSuffix = SMSEntry.getRootSuffix();
    protected static final RedirectUrlValidator<String> REDIRECT_URL_VALIDATOR =
            new RedirectUrlValidator<String>(ValidGotoUrlExtractor.getInstance());

    private static SessionServiceURLService sessionServiceURLService = SessionServiceURLService.getInstance();

    // dsame version
    private static String dsameVersion =
        SystemProperties.get(Constants.AM_VERSION, DSAME_VERSION);

    // If true, version header will be added to responses, default is false
    private static final boolean isVersionHeaderEnabled =
            SystemProperties.getAsBoolean(Constants.AM_VERSION_HEADER_ENABLED, false);

    /* Constants.AM_COOKIE_NAME is the AM Cookie which
     * gets set when the user has authenticated
     */
    private static String cookieName=
        SystemProperties.get(Constants.AM_COOKIE_NAME);

    /* Constants.AM_AUTH_COOKIE_NAME is the Auth Cookie which
     * gets set during the authentication process.
     */
    private static String authCookieName=
        SystemProperties.get(Constants.AM_AUTH_COOKIE_NAME,
        ISAuthConstants.AUTH_COOKIE_NAME);
    /* Constants.AM_DIST_AUTH_COOKIE_NAME is the Auth Cookie which
     * gets set during the authentication process.
     */
    private static String distAuthCookieName=
        SystemProperties.get(Constants.AM_DIST_AUTH_COOKIE_NAME,
        ISAuthConstants.DIST_AUTH_COOKIE_NAME);
    private static String serviceURI = getServiceURI() + "/UI/Login";

    private static String serverURL = null;
    static Debug utilDebug = Debug.getInstance("amAuthClientUtils");
    private static String[] ignoreList = {
        "IDtoken0", "IDtoken1", "IDtoken2", "IDButton", "AMAuthCookie", "encoded", "IDToken3"
    };
    private static boolean useCache = Boolean.getBoolean(SystemProperties.get(
        Constants.URL_CONNECTION_USE_CACHE, "false"));
    private static boolean isSessionHijackingEnabled =
        Boolean.valueOf(SystemProperties.get(
        Constants.IS_ENABLE_UNIQUE_COOKIE, "false")).booleanValue();
    private static String hostUrlCookieName =
        SystemProperties.get(Constants.AUTH_UNIQUE_COOKIE_NAME,
                "sunIdentityServerAuthNServer");
    private static String hostUrlCookieDomain =
        SystemProperties.get(Constants.AUTH_UNIQUE_COOKIE_DOMAIN);

    private static final String distAuthCluster =
        SystemProperties.get(Constants.DISTAUTH_CLUSTER, "");
    
    private static ArrayList distAuthClusterList = new ArrayList();     
    
    private static final String distAuthSites =
        SystemProperties.get(Constants.AM_DISTAUTH_SITES, "");
    
    private static Map<String, Set<String>> distAuthSitesMap = new HashMap();
    private static final List<String> RETAINED_HTTP_REQUEST_HEADERS = new ArrayList<String>();
    private static final List<String> RETAINED_HTTP_HEADERS = new ArrayList<String>();

    static {
        // Initialzing variables
        String installTime =
            SystemProperties.get(AdminTokenAction.AMADMIN_MODE, "false");
        if (installTime.equalsIgnoreCase("false")) {
            clientDetector = new AMClientDetector();
            if (isClientDetectionEnabled()) {
                defaultClient = ClientsManager.getDefaultInstance();
            }
        }
        bundle = Locale.getInstallResourceBundle(BUNDLE_NAME);
        String proto = SystemProperties.get(Constants.DISTAUTH_SERVER_PROTOCOL);
        String host = null;
        String port = null;
        if (proto != null && proto.length() != 0 ) {
            host = SystemProperties.get(Constants.DISTAUTH_SERVER_HOST);
            port = SystemProperties.get(Constants.DISTAUTH_SERVER_PORT);
        } else {
            proto = SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
            host = SystemProperties.get(Constants.AM_SERVER_HOST);
            port = SystemProperties.get(Constants.AM_SERVER_PORT);
        }
        serverURL = proto + "://" + host + ":" + port;

        if(distAuthCluster.length() != 0){
            try {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message(
                        "AuthClientUtils.static(): "
                        + "Cluster List is: " + distAuthCluster);
                }
                if (distAuthCluster.indexOf(",") != -1) {
                    StringTokenizer distAuthServersList = 
                        new StringTokenizer(distAuthCluster, ",");
                    while (distAuthServersList.hasMoreTokens()) {
                        String distAuthServer = 
                            distAuthServersList.nextToken().trim();
                        distAuthClusterList.add(distAuthServer);
                    }
                } else {
                    distAuthClusterList.add(distAuthCluster.trim());
                }
            } catch (Exception e) {
            	utilDebug.error("AuthClientUtils.static(): " + 
                    e.toString());
            }        	
        }
        
        if (distAuthSites.length() != 0) {
            try {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message(
                        "AuthClientUtils.static(): "
                        + "Dist Auth Site list is: " + distAuthSites);
                }
                if (distAuthSites.indexOf(",") != -1) {
                    StringTokenizer distAuthSitesList = 
                        new StringTokenizer(distAuthSites, ",");
                    
                    while (distAuthSitesList.hasMoreTokens()) {
                        String distAuthServer = 
                            distAuthSitesList.nextToken().trim();
                        
                        if (distAuthServer.indexOf("=") != -1) {
                            String distAuthServerName = 
                                    distAuthServer.substring(0, distAuthServer.indexOf("="));
                            String distAuthSiteName =
                                    distAuthServer.substring(distAuthServer.indexOf("=") + 1);
                            Set<String> distAuthSet = distAuthSitesMap.get(distAuthSiteName);
                            
                            if (distAuthSet == null) {
                                distAuthSet = new HashSet<String>();
                            }
                            
                            distAuthSet.add(distAuthServerName);
                            distAuthSitesMap.put(distAuthSiteName, distAuthSet);
                        } else {
                            if (utilDebug.messageEnabled()) {
                                utilDebug.message("AuthClientUtils.static(): " +
                                        "invalid dist auth server entry: " + distAuthServer);
                            }
                            continue;
                        }
                    }
                } else {
                    if (distAuthSites.indexOf("=") != -1) {
                        String distAuthServerName = 
                                distAuthSites.substring(0, distAuthSites.indexOf("="));
                        String distAuthSiteName =
                                distAuthSites.substring(distAuthSites.indexOf("=") + 1);
                        Set<String> distAuthSet = new HashSet<String>();
                        distAuthSet.add(distAuthServerName);
                        distAuthSitesMap.put(distAuthSiteName, distAuthSet);
                    } else {
                        if (utilDebug.messageEnabled()) {
                            utilDebug.message("AuthClientUtils.static(): " +
                                    "invalid dist auth server entry: " + distAuthSites);
                        }
                    }
                }
            } catch (Exception ex) {
                utilDebug.error("AuthClientUtils.static(): " + 
                    ex.toString());
            }
            
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthClientUtils.static(): " +
                        "dist auth server to site: " + distAuthSitesMap);
            }
        }

        RETAINED_HTTP_REQUEST_HEADERS.addAll(getHeaderNameListForProperty(
                Constants.RETAINED_HTTP_REQUEST_HEADERS_LIST));
        //configuration sanity check
        RETAINED_HTTP_REQUEST_HEADERS.removeAll(getHeaderNameListForProperty(
                Constants.FORBIDDEN_TO_COPY_REQUEST_HEADERS));

        RETAINED_HTTP_HEADERS.addAll(getHeaderNameListForProperty(
                Constants.RETAINED_HTTP_HEADERS_LIST));
        //configuration sanity check
        RETAINED_HTTP_HEADERS.removeAll(getHeaderNameListForProperty(
                Constants.FORBIDDEN_TO_COPY_HEADERS));
        //we need to ensure that set-cookie headers are always retained for the response.
        RETAINED_HTTP_HEADERS.add("set-cookie");
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Retained request headers: " + RETAINED_HTTP_REQUEST_HEADERS);
            utilDebug.message("Retained response headers: " + RETAINED_HTTP_HEADERS);
        }
    }

    /*
     * Protected constructor to prevent any instances being created
     * Needs to be protected to allow subclass AuthUtils
     */
    protected AuthClientUtils() {
    }        

    private static List<String> getHeaderNameListForProperty(String property) {
        String value = SystemProperties.get(property);
        if (value != null) {
            return asList(value.toLowerCase().split(","));
        }
        return Collections.EMPTY_LIST;
    }

    public static Hashtable parseRequestParameters(
        HttpServletRequest request) {

    	return (decodeHash(request));
    }

    private static Hashtable<String, String> decodeHash(HttpServletRequest request) {

        Hashtable<String, String> data = new Hashtable<>();
        String clientEncoding = request.getCharacterEncoding();
        String encoding = (clientEncoding != null) ? clientEncoding : "UTF-8";
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils::decodeHash: clientEncoding='{}', encoding='{}'", clientEncoding, encoding);
        }

        @SuppressWarnings("unchecked")
        Enumeration<String> names = request.getParameterNames();
        boolean base64Encoded = Boolean.parseBoolean(request.getParameter("encoded"));

        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = request.getParameter(name);
            if (value == null) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("AuthUtils::decodeHash parameter '{}' is null", name);
                }
                continue;
            }
            if (name.equalsIgnoreCase("SunQueryParamsString")) {
                // This will normally be the case when browser back button is
                // used and the form is posted again with the base64 encoded parameters
                if (!value.isEmpty()) {
                    String decodedValue = Base64.decodeAsUTF8String(value);
                    if (decodedValue == null) {
                        if (utilDebug.warningEnabled()) {
                            utilDebug.warning("As parameter 'encoded' is true, parameter ['{}']='{}' should be base64"
                                    + " encoded", name, value);
                        }
                        continue;
                    }
                    value = decodedValue;
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("AuthUtils::decodeHash base 64 decoded '{}'='{}'", name, value);
                    }
                    StringTokenizer st = new StringTokenizer(value, "&");
                    while (st.hasMoreTokens()) {
                        String str = st.nextToken();
                        if (str.indexOf("=") != -1) {
                            int index = str.indexOf("=");
                            String parameter = str.substring(0, index);
                            String parameterValue = str.substring(index + 1);
                            putDecodedValue(data, parameter, parameterValue, encoding);
                        }
                    }
                }
            } else if (name.equals(RedirectUrlValidator.GOTO) || name.equals(RedirectUrlValidator.GOTO_ON_FAIL)){
                // Again this will be the case when browser back
                // button is used and the form is posted with the
                // base64 encoded parameters including goto
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("AuthUtils::decodeHash '{}'='{}', encoded='{}'", name, value, base64Encoded);
                }

                if (base64Encoded) {
                    String decodedValue = Base64.decodeAsUTF8String(value);
                    if (decodedValue == null && utilDebug.warningEnabled()) {
                        utilDebug.warning("As parameter 'encoded' is true, parameter ['{}']='{}' should be base64" +
                                        " encoded", name, value);
                    }
                    value = decodedValue;
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("AuthUtils::decodeHash base 64 decoded '{}'='{}'", name, value);
                    }
                }
                putDecodedValue(data, name, value, encoding);
            } else{
                putDecodedValue(data, name, value, encoding);
            }
        }// while
        return (data);
    }           

    /**
     * Returns the Logout cookie.
     *
     * @param sid Session ID.
     * @param cookieDomain Cookie domain.
     * @return logout cookie string.
     */
    public static Cookie getLogoutCookie(SessionID sid, String cookieDomain) {
        String logoutCookieString = getLogoutCookieString(sid);
        Cookie logoutCookie = createCookie(logoutCookieString, cookieDomain);
        logoutCookie.setMaxAge(0);
        return (logoutCookie);
    }


    /**
     * Returns the encrpted Logout cookie string .
     * The format of this cookie is:
     * <code>LOGOUT@protocol@servername@serverport@sessiondomain</code>.
     *
     * @param sid the SessionID
     * @return encrypted logout cookie string.
     */
    public static String getLogoutCookieString(SessionID sid) {
        String logout_cookie = null;
        try {
            logout_cookie = (String) AccessController.doPrivileged(
                new EncodeAction(
                "LOGOUT" + "@" +
                sid.getSessionServerProtocol() + "@" +
                sid.getSessionServer() + "@" +
                sid.getSessionServerPort() + "@" +
                sid.getSessionDomain(), Crypt.getHardcodedKeyEncryptor()));
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Logout cookie : " + logout_cookie);
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error creating cookie : " + e.getMessage());
            }
        }
        return (logout_cookie );
    }

    /**
     * Returns Cookie to be set in the response.
     *
     * @param cookieValue value of cookie
     * @param cookieDomain domain for which cookie will be set.
     * @return Cookie object.
     */
    public static Cookie createCookie(String cookieValue, String cookieDomain) {
        String cookieName = getCookieName();
        if (utilDebug.messageEnabled()) {
            utilDebug.message("cookieName='{}', cookieValue='{}', cookieDomain='{}'", cookieName, cookieValue,
                    cookieDomain);
        }
        return (createCookie(cookieName,cookieValue,cookieDomain));
    }    

    public static String getQueryOrgName(HttpServletRequest request,
        String org) {
        String queryOrg = null;
        if ((org != null) && (org.length() != 0)) {
            queryOrg = org;
        } else {
            if (request != null) {
                queryOrg = request.getServerName();
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("queryOrg is :" + queryOrg);
        }
        return queryOrg;
    }   


    // print cookies in the request
    // use for debugging purposes

    public static void printCookies(HttpServletRequest req) {
        Cookie ck[] = req.getCookies();
        if (ck == null) {
            utilDebug.message("No Cookie in header");
            return;
        }
        for (int i = 0; i < ck.length; ++i) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Received Cookie: '{}'='{}'", ck[i].getName(), ck[i].getValue());
            }
        }
    }    

    public static void printHash(Hashtable reqParameters) {
        try {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthRequest: In printHash" + reqParameters);
            }
            if (reqParameters == null) {
                return;
            }
            Enumeration Edata = reqParameters.keys();
            while (Edata.hasMoreElements()) {
                Object key =  Edata.nextElement();
                Object value = reqParameters.get(key);
                utilDebug.message("printHash Key is : " + key);
                if (value instanceof String[]) {
                    String tmp[] = (String[])value;
                    for (int ii=0; ii < tmp.length; ii++) {
                        if (utilDebug.messageEnabled()) {
                            utilDebug.message("printHash : String[] keyname '{}'='{}'", key, tmp[ii]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.warning("Exception: printHash :" , e);
            }
        }
    }

    public static void setlbCookie(HttpServletRequest request,
            HttpServletResponse response) throws AuthException {
        String cookieName = getlbCookieName();
        if (cookieName != null && cookieName.length() != 0) {
            Set<String> domains = getCookieDomainsForRequest(request);
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createlbCookie(domain);
                    CookieUtils.addCookieToResponse(response, cookie);
                }
            } else {
                CookieUtils.addCookieToResponse(response, createlbCookie(null));
            }
        }
    }

    /**
     * Creates a Cookie with the <code>cookieName</code>,
     * <code>cookieValue</code> for the cookie domains specified.
     *
     * @param cookieName is the name of the cookie
     * @param cookieValue is the value fo the cookie
     * @param cookieDomain Domain for which the cookie is to be set.
     * @return the cookie object.
     */
    public static Cookie createCookie(String cookieName, String cookieValue, String cookieDomain) {

        if (utilDebug.messageEnabled()) {
            utilDebug.message("cookieName='{}', cookieValue='{}', cookieDomain='{}'", cookieName, cookieValue,
                    cookieDomain);
        }

        Cookie cookie = null;
        try {
            // hardcoded need to read from attribute and set cookie for all domains
            cookie = CookieUtils.newCookie(cookieName, cookieValue, "/", cookieDomain);
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error creating cookie. : " + e.getMessage());
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("createCookie Cookie is set : " + cookie);
        }
        return cookie;
    }

    public static void clearlbCookie(HttpServletRequest request,
            HttpServletResponse response) {
        String cookieName = getlbCookieName();
        if (cookieName != null && cookieName.length() != 0) {
            Set<String> domains = getCookieDomainsForRequest(request);
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createCookie(
                            cookieName, "LOGOUT", 0, domain);
                    response.addCookie(cookie);
                }
            } else {
                response.addCookie(
                        createCookie(cookieName, "LOGOUT", 0, null));
            }
        }
    }          

    /* return the the error message for the error code */
    public static String getErrorMessage(String errorCode) {
        return getErrorVal(errorCode, ERROR_MESSAGE);
    }

    /* return the the error template for the error code */
    public static String getErrorTemplate(String errorCode) {
        return getErrorVal(errorCode,ERROR_TEMPLATE);
    }

    public static boolean checkForCookies(HttpServletRequest req) {

        // came here if cookie not found , return false
        return CookieUtils.getCookieValueFromReq(req,getAuthCookieName()) != null
                || CookieUtils.getCookieValueFromReq(req,getCookieName()) != null;
    }       

    // Get Original Redirect URL for Auth to redirect the Login request
    public static String getOrigRedirectURL(HttpServletRequest request, SessionID sessID) {
        try {
            String sidString = null;
            if (sessID != null) {
                sidString = sessID.toString();
            }
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(sidString);
            if (manager.isValidToken(ssoToken)) {
                utilDebug.message("Valid SSOToken");

                return REDIRECT_URL_VALIDATOR.getRedirectUrl(ssoToken.getProperty(ISAuthConstants.ORGANIZATION),
                        REDIRECT_URL_VALIDATOR.getAndDecodeParameter(request, RedirectUrlValidator.GOTO),
                        ssoToken.getProperty("successURL"));
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getOrigRedirectURL:", e);
            }
            return null;
        }
        return null;
    }     

    /**
     * Adds Logout cookie to URL.
     *
     * @param url is the url to be rewritten with the logout cookie
     * @param logoutCookie is the logoutCookie String
     * @param isCookieSupported is a boolean which indicates whether
     *        cookie support is true or false
     * @return URL with the logout cookie appended to it.
     */
    public static String addLogoutCookieToURL(
        String url,
        String logoutCookie,
        boolean isCookieSupported) {
        String logoutURL = null;

        if ((logoutCookie == null) || (isCookieSupported)) {
            logoutURL = url;
        } else {
            StringBuilder cookieString = new StringBuilder();
            cookieString.append(URLEncDec.encode(getCookieName()))
            .append("=").append(URLEncDec.encode(logoutCookie));

            if (url.indexOf("?") != -1) {
                cookieString.insert(0,"&amp;");
            } else {
                cookieString.insert(0,"?");
            }

            cookieString.insert(0,url);
            logoutURL = cookieString.toString();

            if (utilDebug.messageEnabled()) {
                utilDebug.message("cookieString is : "+ cookieString);
            }
        }

        return logoutURL;
    }                 

    /**
     * Returns the Session ID for the request.
     * The cookie in the request for invalid sessions
     * is in authentication cookie, <code>com.iplanet.am.auth.cookie</code>,
     * and for active/inactive sessions in <code>com.iplanet.am.cookie</code>.
     *
     *  @param request HttpServletRequest object.
     *  @return session id for this request.
     */
    private static SessionID getSidFromCookie(HttpServletRequest request) {
        SessionID sessionID = null;
        //Let's check the URL first in case this is a forwarded request from Federation. URL should have precedence
        //over the actual cookie value, so this way a new federated auth can always start with a clear auth session.
        String sidValue = SessionEncodeURL.getSidFromURL(request, getAuthCookieName());
        if (sidValue == null) {
            sidValue = CookieUtils.getCookieValueFromReq(request, getAuthCookieName());
        }
        if (sidValue != null && !sidValue.isEmpty()) {
            sessionID = new SessionID(sidValue);
            utilDebug.message("sidValue from Auth Cookie");
        }
        return sessionID;
    }

    /**
     * Returns the Session ID for this request.  If Authetnication Cookie and
     * Valid AM Cookie are there and request method is GET then use Valid
     * AM Cookie else use Auth Cookie. The cookie in the request for invalid
     * sessions is in auth cookie, <code>com.iplanet.am.auth.cookie</code>,
     * and for active/inactive sessions in <code>com.iplanet.am.cookie</code>.
     *
     * @param request HTTP Servlet Request.
     * @return Session ID for this request.
     */
    public static SessionID getSessionIDFromRequest(HttpServletRequest request) {
        boolean isGetRequest= (request !=null &&
            request.getMethod().equalsIgnoreCase("GET"));
        SessionID amCookieSid = new SessionID(request);
        SessionID authCookieSid = getSidFromCookie(request);
        SessionID sessionID;
        if (authCookieSid == null) {
            sessionID = amCookieSid;
        } else {
            if (isGetRequest) {
                sessionID = amCookieSid;
            } else {
                sessionID = authCookieSid;
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils:returning sessionID:" + sessionID);
        }
        return sessionID;
    }    

    /**
     * Returns <code>true</code> if the request has the
     * <code>arg=newsession</code> query parameter.
     *
     * @param reqDataHash Request Data Hashtable.
     * returns <code>true</code> if this parameter is present.
     */
    public static boolean newSessionArgExists(Hashtable reqDataHash) {
        String arg = (String) reqDataHash.get("arg");
        boolean newSessionArgExists =
            (arg != null) && arg.equals("newsession");
        if (utilDebug.messageEnabled()) {
            utilDebug.message("newSessionArgExists : " + newSessionArgExists);
        }
        return newSessionArgExists;
    }

    // Get the AuthContext.IndexType given string index type value
    public static AuthContext.IndexType getIndexType(String strIndexType) {
        AuthContext.IndexType indexType = null;
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getIndexType : strIndexType = " + strIndexType);
        }
        if (strIndexType != null) {
            if (strIndexType.equalsIgnoreCase("user")) {
                indexType = AuthContext.IndexType.USER;
            } else if (strIndexType.equalsIgnoreCase("role")) {
                indexType = AuthContext.IndexType.ROLE;
            } else if (strIndexType.equalsIgnoreCase("service")) {
                indexType = AuthContext.IndexType.SERVICE;
            } else if (strIndexType.equalsIgnoreCase("module_instance")) {
                indexType = AuthContext.IndexType.MODULE_INSTANCE;
            } else if (strIndexType.equalsIgnoreCase("level")) {
                indexType = AuthContext.IndexType.LEVEL;
            } else if (strIndexType.equalsIgnoreCase("composite_advice")) {
                indexType = AuthContext.IndexType.COMPOSITE_ADVICE;
            }

        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getIndexType : IndexType = " + indexType);
        }
        return indexType;
    }

    // Get the index name given index type from the existing valid session
    public static String getIndexName(SSOToken ssoToken,
        AuthContext.IndexType indexType) {
        String indexName = "";
        try {
            if (indexType == AuthContext.IndexType.USER) {
                indexName = ssoToken.getProperty("UserToken");
            } else if (indexType == AuthContext.IndexType.ROLE) {
                indexName = ssoToken.getProperty("Role");
            } else if (indexType == AuthContext.IndexType.SERVICE) {
                indexName = ssoToken.getProperty("Service");
            } else if (indexType == AuthContext.IndexType.MODULE_INSTANCE) {
                indexName = 
                    getLatestIndexName(ssoToken.getProperty("AuthType"));
            } else if (indexType == AuthContext.IndexType.LEVEL) {
                indexName = ssoToken.getProperty("AuthLevel");
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getIndexName :"+ e.toString());
            }
            return indexName;
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getIndexName : IndexType='{}', IndexName='{}'", indexType, indexName);
        }
        return indexName;
    }

    // Get the first or latest index name from the string of index names
    // separated by "|".
    private static String getLatestIndexName(String indexName) {
        String firstIndexName = indexName;
        if (indexName != null) {
            StringTokenizer st = new StringTokenizer(indexName,"|");
            if (st.hasMoreTokens()) {
                firstIndexName = (String)st.nextToken();
            }
        }
        return firstIndexName;
    }

    // search valve in the String
    public static boolean isContain(String value, String key) {
        if (value == null) {
            return (false);
        }
        try {
            if (value.indexOf("|") != -1) {
                StringTokenizer st = new StringTokenizer(value, "|");
                while (st.hasMoreTokens()) {
                    if ((st.nextToken()).equals(key)) {
                        return true;
                    }
                }
            } else {
                if (value.trim().equals(key.trim())) {
                    return true;
                }
            }
        } catch (Exception e) {
            utilDebug.error("AuthClientUtils.isContain: error : ", e);
        }
        return false;
    }

    // Method to check if this is Session Upgrade
    public static boolean checkSessionUpgrade(
        SSOToken ssoToken,Hashtable reqDataHash) {
        utilDebug.message("Check Session upgrade!");
        String tmp = null;
        String value = null;
        boolean upgrade = false;
        try {
            if (reqDataHash.get("user")!=null) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("AuthClientUtils.checkSessionUpgrade: user");
            	}
                tmp = (String) reqDataHash.get("user");
                value = ssoToken.getProperty("UserToken");
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("user='{}', userToken ='{}'", tmp, value);
                }
                if (!tmp.equals(value)) {
                    upgrade = true;
                }
            } else if (reqDataHash.get("role")!=null) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("AuthClientUtils.checkSessionUpgrade: role");
            	}
                tmp = (String) reqDataHash.get("role");
                value = ssoToken.getProperty("Role");
                if (!isContain(value, tmp)) {
                    upgrade = true;
                }
	            } else if (reqDataHash.get("service")!=null &&
	                    reqDataHash.get(Constants.COMPOSITE_ADVICE) == null) {
                if(utilDebug.messageEnabled()) {
                    utilDebug.message("AuthClientUtils.checkSessionUpgrade:service");
                }
                tmp = (String) reqDataHash.get("service");
                value = ssoToken.getProperty("Service");
                if (!isContain(value, tmp)) {
                    upgrade = true;
                }
            } else if (reqDataHash.get("module")!=null) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("AuthClientUtils.checkSessionUpgrade:module");
                }
                tmp = (String) reqDataHash.get("module");
                value = ssoToken.getProperty("AuthType");
                if (!isContain(value, tmp)) {
                    upgrade = true;
                }
            } else if (reqDataHash.get("authlevel")!=null) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("checksessionUpgrade: authlevel");
                }
                int i = Integer.parseInt((String)reqDataHash.get("authlevel"));
                if (i>Integer.parseInt(ssoToken.getProperty("AuthLevel"))) {
                    upgrade = true;
                }
            } else if ( reqDataHash.get(Constants.COMPOSITE_ADVICE) != null ) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("checksessionUpgrade: composite advice");
                }
                upgrade = true;
            }
        } catch (Exception e) {
            utilDebug.message("Exception in checkSessionUpgrade : ", e);
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("Check session upgrade : " + upgrade);
        }
        return upgrade;
    }

    /**
     * Tells whether the incoming request corresponds to a session upgrade or ForceAuth.
     *
     * @param request The incoming HttpServletRequest.
     * @return <code>true</code> if the request corresponds to a session upgrade or ForceAuth, <code>false</code>
     * otherwise.
     */
    public static boolean isSessionUpgradeOrForceAuth(HttpServletRequest request) {
        Hashtable reqDataHash = parseRequestParameters(request);
        boolean isForceAuth = forceAuthFlagExists(reqDataHash);
        if (!isForceAuth) {
            try {
                SSOTokenManager tokenManager = SSOTokenManager.getInstance();
                SSOToken token = tokenManager.createSSOToken(request);
                return checkSessionUpgrade(token, reqDataHash);
            } catch (SSOException ssoe) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Unable to create sso token for isSessionUpgrade check: ", ssoe);
                }
            }
        }

        return isForceAuth;
    }

    public static String getCookieURLForSessionUpgrade(HttpServletRequest request) {
        String cookieURL = null;
        try {
            SSOTokenManager tokenManager = SSOTokenManager.getInstance();
            SSOToken token = tokenManager.createSSOToken(request);
            Hashtable reqDataHash = parseRequestParameters(request);
            if (tokenManager.isValidToken(token)) {
                cookieURL = getCookieURL(new SessionID(token.getTokenID().toString()));
                if (cookieURL != null && !isLocalServer(cookieURL, true)
                        && (forceAuthFlagExists(reqDataHash)
                        || checkSessionUpgrade(token, reqDataHash))) {
                    return cookieURL;
                }
            }
        } catch (SSOException ssoe) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("SSOException occurred while checking session upgrade case", ssoe);
            }
        }

        return null;
    }

    public static String getCookieURL(SessionID sessionID) {
        String cookieURL = null;
        try {
            URL sessionServerURL = sessionServiceURLService.getSessionServiceURL(sessionID);
            cookieURL = sessionServerURL.getProtocol()
                    + "://" + sessionServerURL.getHost() + ":"
                    + Integer.toString(sessionServerURL.getPort()) + serviceURI;
        } catch (SessionException se) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("LoginServlet error in Session : ", se);
            }
        }

        return cookieURL;
    }

    public static boolean isClientDetectionEnabled() {
        boolean clientDetectionEnabled = false;

        if (clientDetector != null) {
            clientDetectionEnabled = clientDetector.isDetectionEnabled();
        } else {
            utilDebug.message("getClientDetector,Service does not exist");
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("clientDetectionEnabled = " + clientDetectionEnabled);
        }
        return clientDetectionEnabled;
    }

    /**
     * Returns the client type. If client detection is enabled then
     * client type is determined by the <code>ClientDetector</code> class otherwise
     * <code>defaultClientType</code> set in
     * <code>iplanet-am-client-detection-default-client-type</code>
     * is assumed to be the client type.
     *
     * @param req HTTP Servlet Request.
     * @return client type.
     */
    public static String getClientType(HttpServletRequest req) {
        if (isClientDetectionEnabled() && (clientDetector != null)) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("clienttype = " + clientDetector.getClientType(req));
            }
            return (clientDetector.getClientType(req));
        }
        return (getDefaultClientType());
    }

    /**
     * Get default client
     */
    public static String getDefaultClientType() {
        String defaultClientType = DEFAULT_CLIENT_TYPE;
        if (defaultClient != null) {
            try {
                defaultClientType = defaultClient.getClientType();
                // add observer, so auth will be notified if the client changed
                // defClient.addObserver(this);
            } catch (Exception e) {
                utilDebug.error("getDefaultClientType Error : ", e);
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getDefaultClientType, ClientType = " + defaultClientType);
        }
        return defaultClientType;
    }

    /**
     * return the client Object associated with a clientType
     * default instance is returned if the instance could not be found
     */
    private static Client getClientInstance(String clientType) {
        if (!clientType.equals(getDefaultClientType())) {
            try {
                return AuthClient.getInstance(clientType, null);
            } catch (Exception ce) {
                utilDebug.warning("getClientInstance: clientType='{}'", clientType, ce);
            }
        }
        return defaultClient;
    }

    /**
     * Returns the requested property from clientData (example fileIdentifer).
     *
     * @param clientType
     * @param property
     * @return the requested property from clientData.
     */
    private static String getProperty(String clientType, String property) {
        if (clientDetector == null || !isClientDetectionEnabled()) {
            return null;
        }

        try {
            return (getClientInstance(clientType).getProperty(property));
        } catch (Exception ce) {
            // which means we did not get the client Property
            utilDebug.warning("Error retrieving Client Data : property='{}'", property, ce);
            // if this was not the default client type then lets
            // try to get the default client Property
            return getDefaultProperty(property);
        }
    }

    /**
     * return the requested property for default client
     */
    public static String getDefaultProperty(String property) {        
        try {
            return (defaultClient.getProperty(property));
        } catch (Exception ce) {
            utilDebug.warning("Could not get property='{}'", property, ce);
        }
        return (null);
    }

    /**
     * return the charset associated with the clientType
     */
    public static String getCharSet(String clientType,java.util.Locale locale) {
        String charset = Client.CDM_DEFAULT_CHARSET;
        if (isClientDetectionEnabled()) {
            try {
                charset = getClientInstance(clientType).getCharset(locale);
            } catch (Exception ce) {
                if (utilDebug.warningEnabled()) {
                    utilDebug.warning("AuthClientUtils.getCharSet:Client data was not found, setting charset to UTF-8.");
                }
                charset = Constants.CONSOLE_UI_DEFAULT_CHARSET;
            }
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthClientUtils.getCharSet: Charset from Client is charset='{}'", charset);
            }
        } else {
            charset = Constants.CONSOLE_UI_DEFAULT_CHARSET;
        }
        return (charset);
    }

    /**
     * return the filePath associated with a clientType
     */
    public static String getFilePath(String clientType) {
        String filePath = getProperty(clientType,"filePath");
        if (filePath == null) {
            return DEFAULT_FILE_PATH;
        }
        return filePath;
    }

    /**
     * return the contentType associated with a clientType
     * if no contentType found then return the default
     */
    public static String getContentType(String clientType) {

        String contentType = getProperty(clientType,"contentType");
        if (contentType == null) {
            return (DEFAULT_CONTENT_TYPE);
        }
        return contentType;
    }

    /**
     * for url rewriting with session id we need to know whether
     * cookies are supported
     * RFE 4412286
     */
    public static String getCookieSupport(String clientType) {
        String cookieSup = getProperty(clientType, "cookieSupport");

        if (cookieSup == null) {
            return (DEFAULT_COOKIE_SUPPORT);
        }

        return cookieSup;
    }

    /**
     * determine if this client is an html client
     */
    public static boolean isGenericHTMLClient(String clientType) {
        String type = getProperty(clientType,"genericHTML");
        return type == null || "true".equals(type);
    }    

    /* return true if cookiSupport is true or cookieDetection
     * mode has been detected .This is used to determine
     * whether cookie should be set in response or not.
     */
    public static boolean isSetCookie(String clientType) {
        boolean setCookie =  setCookieVal(clientType, "true");

        if (utilDebug.messageEnabled()) {
            utilDebug.message("setCookie : " + setCookie);
        }

        return setCookie;
    }

    /* checks the cookieDetect , cookieSupport values to
     * determine if cookie should be rewritten or set.
     */
    public static boolean setCookieVal(String clientType,String value) {

        String cookieSupport = getCookieSupport(clientType);
        boolean cookieDetect = getCookieDetect(cookieSupport);

        boolean cookieSup =  ((cookieSupport !=null) &&
            (cookieSupport.equalsIgnoreCase(value) ||
            cookieSupport.equalsIgnoreCase(
            ISAuthConstants.COOKIE_DETECT_PROPERTY)));
        boolean setCookie = (cookieSup || cookieDetect) ;

        if (utilDebug.messageEnabled()) {
            utilDebug.message("cookieSupport='{}', cookieDetect='{}', setCookie='{}'", cookieSupport, cookieDetect,
                    setCookie);
        }

        return setCookie;
    }

    /** Returns true if cookieDetect mode else false.
     *  @param cookieSupport , whether cookie is supported or not.
     *  @return true if cookieDetect mode else false
     */
    public static boolean getCookieDetect(String cookieSupport) {
        boolean cookieDetect
                = ((cookieSupport == null) ||
            (cookieSupport.equalsIgnoreCase(
            ISAuthConstants.COOKIE_DETECT_PROPERTY)));
        if (utilDebug.messageEnabled()) {
            utilDebug.message("CookieDetect : " + cookieDetect);
        }
        return cookieDetect;
    }

    /**
     * Extracts the client URL from the String passed
     * URL passed is in the format clientType | URL
     * @param urlString is a String , a URL
     * @param index is the position of delimiter "|"
     * @return Returns the client URL.
     */
    public static String getClientURLFromString(String urlString,int index,
        HttpServletRequest request) {
        String clientURL = null;
        if (urlString != null) {
            String clientTypeInUrl = urlString.substring(0,index);
            if ((clientTypeInUrl != null) &&
                (clientTypeInUrl.equals(getClientType(request)))) {
                if (urlString.length() > index) {
                    clientURL = urlString.substring(index+1);
                }
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Client URL is :" + clientURL);
        }
        return clientURL;
    }

    /* return true if cookieSupport is false and cookie Detect
     * mode (which is rewrite as well as set cookie the first
     * time). This determines whether url should be rewritten
     * or not.
     */
    public static boolean isUrlRewrite(String clientType) {

        boolean rewriteURL = setCookieVal(clientType,"false");
        if (utilDebug.messageEnabled()) {
            utilDebug.message("rewriteURL : " + rewriteURL);
        }

        return rewriteURL;
    }

    public static String getDSAMEVersion() {
        return dsameVersion;
    }

    public static boolean isVersionHeaderEnabled() {
        return isVersionHeaderEnabled;
    }

    /**Returns the Auth Cookie Name.
     *
     * @return authCookieName, a String,the auth cookie name.
     */
    public static String getAuthCookieName() {
        return authCookieName;
    }

    /**Returns the Dist Auth Cookie Name.
     *
     * @return authCookieName, a String, the dist auth cookie name.
     */
    public static String getDistAuthCookieName() {
        return distAuthCookieName;
    }

    public static String getCookieName() {
        return cookieName;
    }

    public static String getlbCookieName() {
        String loadBalanceCookieName = null;
        if (SystemProperties.isServerMode()) {
            loadBalanceCookieName = SystemProperties.get(
                    Constants.AM_LB_COOKIE_NAME,"amlbcookie");
        } else {
            loadBalanceCookieName = SystemProperties.get(
                    Constants.AM_DISTAUTH_LB_COOKIE_NAME);
        }

        if(utilDebug.messageEnabled()){
        	utilDebug.message("AuthClientUtils.getlbCookieName() loadBalanceCookieName is:"
                    + loadBalanceCookieName);
        }
        return loadBalanceCookieName;
    }

    public static String getlbCookieValue() {
        if (SystemProperties.isServerMode()) {
            try {
                return WebtopNaming.getLBCookieValue(WebtopNaming.getAMServerID());
            } catch (Exception e) {
                if(utilDebug.messageEnabled()){
                    utilDebug.message("AuthClientUtils.getlbCookieValue(). Can't get the lbCookie value.", e);
                }
                return null;
            }
        } else {
            return SystemProperties.get(Constants.AM_DISTAUTH_LB_COOKIE_VALUE);
        }
    }

    /**
     * Return the set of cookie domains configured in Platform settings. Whenever possible, use
     * {@link #getCookieDomainsForRequest(HttpServletRequest)} instead.
     *
     * @return The set of configured cookie domains. May contain null.
     */
    public static Set<String> getCookieDomains() {
        Set<String> cookieDomains = Collections.EMPTY_SET;
        try {
            SSOToken token = AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            try {
                ServiceSchemaManager scm  = new ServiceSchemaManager("iPlanetAMPlatformService",token);
                ServiceSchema psc = scm.getGlobalSchema();
                Map attrs = psc.getAttributeDefaults();
                cookieDomains = (Set)attrs.get(ISAuthConstants.PLATFORM_COOKIE_DOMAIN_ATTR);
            } catch (SMSException ex) {
                // Ignore the exception and leave cookieDomains empty;
                utilDebug.message("getCookieDomains - SMSException ");
            }
            if (cookieDomains == null) {
                cookieDomains = Collections.singleton(null);
            }
        } catch (SSOException ex) {
            // unable to get SSOToken
            utilDebug.message("getCookieDomains - SSOException ");
        }
        if (utilDebug.messageEnabled() && (!cookieDomains.isEmpty())) {
            StringBuilder message = new StringBuilder("CookieDomains : ");
            for (String cookieDomain : cookieDomains) {
                message.append("  '").append(cookieDomain).append("'");
            }
            utilDebug.message(message.toString());

        }
        return cookieDomains;
    }

    /**
     * Find the cookie domains from the cookie domain list based on the hostname of the incoming request.
     *
     * @param request HttpServletRequest request.
     * @return Set of the matching cookie domains. May contain null.
     */
    public static Set<String> getCookieDomainsForRequest(HttpServletRequest request) {
        Set<String> domains = getCookieDomains();
        if (request == null) {
            return domains;
        }

        domains = CookieUtils.getMatchingCookieDomains(request, domains);
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthClientUtils:getCookieDomainsForRequest returns " + domains);
        }

        return domains;
    }

    /* This method returns the organization DN.
     * The organization DN is deteremined based on
     * the query parameters "org" OR "domain" OR
     * the server host name. For backward compatibility
     * the orgname will be determined from requestURI
     * in the case where either query params OR server host
     * name are not valid and orgDN cannot be found.
     * The orgDN is determined based on and in order,by the SDK:
     * 1. OrgDN - organization dn.
     * 2. Domain - check if org is a domain by trying to get
     *    domain component
     * 3  Org path- check if the orgName passed is a path (eg."/suborg1")
     * 4. URL - check if the orgName passed is a DNS alias (URL).
     * 5. If no orgDN is found null is returned.
     * @param orgParam is the org or domain query param ,
     *        or the server host name
     * @param noQueryParam is a boolean indicating that the
     *        the request did not have query.
     * @param request is the HttpServletRequest object
     * @return A String which is the organization DN
     */
    public static String getOrganizationDN(String orgParam,boolean noQueryParam,
        HttpServletRequest request) {
        String orgName = null;

        // try to get the host name if org or domain Param is null
        try {
        	orgName=Realm.of(orgParam).asDN();
            if ((orgName != null) && (orgName.length() != 0)) {
                orgName = orgName.toLowerCase();
            }
        } catch (Exception oe) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Could not get orgName", oe);
            }
        }

        // if orgName is null then match the DNS Alias Name
        // to the full url ie. proto:/server/amserver/UI/Login
        // This is for backward compatibility

        if (((orgName == null) || orgName.length() == 0) && (noQueryParam)) {
            if (request != null) {
                String url = request.getRequestURL().toString();
                int index  = url.indexOf(";");
                if (index != -1) {
                    orgParam = stripPort(url.substring(0,index));
                } else {
                    orgParam = stripPort(url);
                }

                try {
                    orgName=Realm.of(orgParam).asDN();
                } catch (Exception e) {
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Could not get orgName='{}'", orgParam, e);
                    }
                }
            }
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("getOrganizationDN : orgParam.='{}', orgDN='{}'", orgParam, orgName);
        }
        return orgName;
    }

    /** This method determines the org parameter
     * and determines the organization DN based on
     * query parameters.
     * The organization DN is determined based on
     * the policy advice OR
     * the query parameters "org" OR "domain" OR
     * the server host name. For backward compatibility
     * the orgname will be determined from requestURI
     * in the case where either query params OR server host
     * name are not valid and orgDN cannot be found.
     * The orgDN is determined based on and in order,by the SDK:
     * 1. OrgDN - organization dn.
     * 2. Domain - check if org is a domain by trying to get
     *    domain component
     * 3  Org path- check if the orgName passed is a path (eg."/suborg1")
     * 4. URL - check if the orgName passed is a DNS alias (URL).
     * 5. Policy Advice will be checked for realm advice, or realm component in
     *    the advice
     * 6. If no orgDN is found null is returned.
     *
     * @param request HTTP Servlet Request object.
     * @param requestHash Query Hashtable.
     * @return Organization DN.
     */
    public static String getDomainNameByRequest(
        HttpServletRequest request,
        Map<String, String> requestHash) {

        boolean noQueryParam=false;
        String realm = getRealmFromPolicyAdvice(requestHash);
        if (realm == null) {
            realm = getRealmFromAttribute(request);
        }
        String orgParam = getOrgParam(requestHash);
        if (realm != null) {
            //Policy Advice has precedence over GET parameter
            orgParam = realm;
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgParam='{}'", orgParam);
        }

        // try to get the host name if org or domain Param is null
        if ((orgParam == null) || (orgParam.length() == 0)) {
            noQueryParam= true;
            orgParam = request.getServerName();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Hostname='{}'", orgParam);
            }
        }
        String orgDN = getOrganizationDN(orgParam,noQueryParam,request);

        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgDN='{}'", orgDN);
        }

        return orgDN;
    }

    private static String getRealmFromAttribute(HttpServletRequest request) {
        return (String) request.getAttribute(ISAuthConstants.REALM_PARAM);
    }

    /**
     * Returns the org or domain parameter passed as a query in the request.
     *
     * @param requestHash Hashtable containing the query parameters
     * @return organization name.
     */
    public static String getOrgParam(Map<String, String> requestHash) {
        String orgParam = null;
        if (requestHash != null && !requestHash.isEmpty()) {
            orgParam = requestHash.get(ISAuthConstants.DOMAIN_PARAM);
            if (orgParam == null || orgParam.length() == 0) {
                orgParam = requestHash.get(ISAuthConstants.ORG_PARAM);
            }
            if (orgParam == null || orgParam.length() == 0) {
                orgParam = requestHash.get(ISAuthConstants.REALM_PARAM);
            }
        }
        return orgParam;
    }

    static String stripPort(String in) {
        try {
            URL url = new URL(in);
            return url.getProtocol() + "://" + url.getHost()+ url.getFile();
        } catch (MalformedURLException ex) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("URL='{}' is mal formed", in, ex);
            }
            return in;
        }
    }

    /**
     * Returns <code>true</code> if the host name in the URL is valid.
     *
     * @param hostName Host name.
     * @return <code>true</code> if the host name in the URL is valid.
     */
    public static boolean isValidFQDNRequest(String hostName) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("hostName is : " + hostName);
        }

        boolean retVal = FqdnValidator.getInstance().isHostnameValid(hostName);
        if (utilDebug.messageEnabled()) {
            if (retVal) {
                utilDebug.message("hostname  and fqdnDefault match returning true");
            } else {
                utilDebug.message("hostname and fqdnDefault don't match");
            }
            utilDebug.message("retVal is : " + retVal);
        }
        return retVal;
    }

    /**
     * Returns the valid hostname from the fqdn map and constructs the correct
     * URL. The request will be forwarded to the new URL.
     *
     * @param partialHostName Partial host name.
     * @param servletRequest HTTP Servlet Request.
     */
    public static String getValidFQDNResource(String partialHostName, HttpServletRequest servletRequest) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Get mapping for " + partialHostName);
        }

        // get mapping from table
        String validHostName =
            FqdnValidator.getInstance().getFullyQualifiedHostName(partialHostName);

        if (validHostName == null) {
            validHostName = partialHostName;
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("fully qualified hostname :" + validHostName);
        }

        String requestURL = constructURL(validHostName,servletRequest);

        if (utilDebug.messageEnabled()) {
            utilDebug.message("Request URL :" + requestURL);
        }
        return requestURL;
    }

    /* get the host name from the servlet request's host header or
     * get it using servletRequest:getServerName() in the case
     * where host header is not found
     */
    public static String getHostName(HttpServletRequest servletRequest) {
        // get the host header
        String hostname = servletRequest.getHeader("host");
        if (hostname != null) {
            int i = hostname.indexOf(":");
            if (i != -1) {
                hostname = hostname.substring(0,i);
            }
        } else {
            hostname = servletRequest.getServerName();
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("Returning host name : " + hostname);
        }
        return hostname;

    }

    /* construct the url */
    static String constructURL(String validHostName,
        HttpServletRequest servletRequest) {
        String scheme =
            RequestUtils.getRedirectProtocol(
                servletRequest.getScheme(),validHostName);
        int port = servletRequest.getServerPort();
        String requestURI = servletRequest.getRequestURI();
        String queryString = servletRequest.getQueryString();

        StringBuilder urlBuffer = new StringBuilder();
        urlBuffer.append(scheme)
                .append("://")
                .append(validHostName)
                .append(":")
                .append(port)
                .append(requestURI);

        if (queryString != null) {
            urlBuffer.append("?").append(queryString);
        }

        String urlString = urlBuffer.toString();

        if (utilDebug.messageEnabled()) {
            utilDebug.message("returning new url : " + urlString);
        }

        return urlString;
    }

    private static boolean ignoreParameter(String parameter) {
        boolean ignore = false;
        for (int i = 0; i < ignoreList.length; i++) {
            if (parameter.equalsIgnoreCase(ignoreList[i])) {
                ignore = true;
                break;
            }
        }
       return ignore;
    }

    public static String constructLoginURL(HttpServletRequest request) {
        StringBuilder loginURL = new StringBuilder(serviceURI);
        StringBuilder queryString = new StringBuilder();
        String clientEncoding = request.getCharacterEncoding();
        String encoding = (clientEncoding != null) ? clientEncoding : "UTF-8";
        boolean encoded = Boolean.parseBoolean(request.getParameter("encoded"));

        if (request.getAttribute("jakarta.servlet.forward.servlet_path") != null) {
            //this is a forwarded request, we should only save the forwarded URL.
            queryString.append(request.getQueryString());
            if (queryString.length() > 0) {
                loginURL.append('?')
                        .append(queryString);
            }

            if (utilDebug.messageEnabled()) {
                utilDebug.message("constructLoginURL: Returning login url for forwarded request: " + loginURL);
            }
            return loginURL.toString();
        }
        Enumeration parameters = request.getParameterNames();
        for ( ; parameters.hasMoreElements() ;) {
            String parameter = (String)parameters.nextElement();
            if(utilDebug.messageEnabled()) {
                utilDebug.message("constructLoginURL:parameter: "+parameter);
            }
            if(!ignoreParameter(parameter)){
                // This will nornally be the case when browser back button is 
                // used and the form is posted again with the base64 encoded 
                // parameters
                if (parameter.equalsIgnoreCase("SunQueryParamsString")) {
                    String queryParams = request.getParameter(parameter);
                    if ((queryParams != null) && (queryParams.length()>0)){
                        String decodedQueryParams = Base64.decodeAsUTF8String(queryParams);
                        if (decodedQueryParams == null && utilDebug.warningEnabled()) {
                            utilDebug.warning("Parameter ['{}']='{}' should be base64 encoded", parameter, queryParams);
                        }
                        queryParams = decodedQueryParams;
                    }
                    if ((queryParams != null) &&
                         (queryParams.length()>0)) {
                        if(utilDebug.messageEnabled()) {
                            utilDebug.message("constructLoginURL: value: " + queryParams);
                        }
                        // This function will encode all the parameters in
                        // SunQueryParamsString 
                        queryParams = URLencodedSunQueryParamsString(queryParams,encoding);
                    }
                    queryString.append(queryParams);
                } else {
                    String value = request.getParameter(parameter);
                    if (StringUtils.isNotEmpty(value)) {
                       if ((RedirectUrlValidator.GOTO.equals(parameter) ||
                               RedirectUrlValidator.GOTO_ON_FAIL.equals(parameter)) && encoded) {
                    	   // Again this will be the case when browser back
                    	   // button is used and the form is posted with the
                    	   // base64 encoded parameters including goto
                           String decodedValue = Base64.decodeAsUTF8String(value);
                           if (decodedValue == null && utilDebug.warningEnabled()) {
                               utilDebug.warning("As parameter 'encoded' is true, parameter ['{}']='{}' should be base64" +
                                       " encoded", parameter, value);
                           }
                           value = decodedValue;
                           if(utilDebug.messageEnabled()) {
                               utilDebug.message("constructLoginURL: Base64 decoded "+parameter+"='{}'", value);
                           }
                       } 
                       queryString.append(URLEncDec.encode(parameter)).append("=")
                               .append(URLEncDec.encode(getCharDecodedField(value, encoding)));
                    }
                }
                if (parameters.hasMoreElements()) {
                    queryString.append("&");
                }
            }
        }

        if(queryString.length() > 0){
            loginURL.append("?")
                    .append(queryString);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthClientUtils.constructLoginURL()returning URLEncoded login url : " + loginURL);
        }
        return loginURL.toString();
    }
    
    /**
     * This method takes in a String representing query parameters, and 
     * URL encodes "sunamcompositeadvice" parameter out of it.
     */
     private static String URLencodedCompositeAdvice(String queryParams) {
         StringBuilder sb = new StringBuilder(400);
         StringTokenizer st = new StringTokenizer(queryParams, "&");
         String adviceString = null;
         while (st.hasMoreTokens()) {
             String str = st.nextToken();
             if (str.indexOf(COMPOSITE_ADVICE) != -1 ) {
                 adviceString = str;
             } else {
                sb.append(str).append("&");
             }
         }
         int index = adviceString.indexOf("=");
         String value = adviceString.substring(index+1);
         sb.append(COMPOSITE_ADVICE).append("=");
         sb.append(URLEncDec.encode(value));
         return sb.toString();
     }
     
     /**
     * This method takes in a String representing base64 decoded 
     * SunQueryParamsString and URL encodes all the parameters
     * included in its value
     */
      protected static String URLencodedSunQueryParamsString(String queryParams,
              String encoding){
          StringBuilder sb = new StringBuilder(400);
          StringTokenizer st = new StringTokenizer(queryParams, "&");
          while (st.hasMoreTokens()) {
              String str = st.nextToken();
              if (str.indexOf("=") != -1) {
                  int index = str.indexOf("=");
                  String parameter = str.substring(0,index);
                  String value = str.substring(index+1);
                  if(parameter.equalsIgnoreCase("realm")||
                     parameter.equalsIgnoreCase("org")||
                     parameter.equalsIgnoreCase("module")){
                     value = getCharDecodedField(value, encoding);
                  }
                  sb.append(URLEncDec.encode(parameter));
                  sb.append("=");
                  sb.append(URLEncDec.encode(value));
                  if(st.hasMoreTokens()){
                      sb.append("&");
                  }
              } 
          }          
          return sb.toString();
      }     

    // Get Original Redirect URL for Auth to redirect the Login request
    public static SSOToken getExistingValidSSOToken(SessionID sessID) {
        SSOToken ssoToken = null;
        try {
            if (sessID != null) {
                String sidString = sessID.toString();
                SSOTokenManager manager = SSOTokenManager.getInstance();
                SSOToken currentToken = manager.createSSOToken(sidString);
                if (manager.isValidToken(currentToken)) {
                    ssoToken = currentToken;
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getExistingValidSSOToken {} {}",sessID ,e.toString());
            }
            return ssoToken;
        }
        return ssoToken;
    }

    // Check for Session Timed Out
    // If Session is Timed Out Exception is thrown
    public static boolean isTimedOut(SessionID sessID) {
        boolean isTimedOut = false;
        try {
            if (sessID != null) {
                String sidString = sessID.toString();
                SSOTokenManager manager = SSOTokenManager.getInstance();
                SSOToken currentToken = manager.createSSOToken(sidString);
                if (manager.isValidToken(currentToken)) {
                      isTimedOut = false;
                }
            }
        } catch (Exception e) {
            if (e.getMessage().indexOf("Session timed out") != -1) {
                isTimedOut = true;
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Session Timed Out :" + isTimedOut);
        }
        return isTimedOut;
    }

    public static String getErrorVal(String errorCode,String type) {

        if (Locale.getDefaultLocale() != bundle.getLocale()) {
            bundle = Locale.getInstallResourceBundle(BUNDLE_NAME);
        }

        return getErrorVal(errorCode, type, bundle);
    }
    
    public static String getErrorVal(String errorCode,String type,
            ResourceBundle bundle) {

        ResourceBundle errMsgBundle = bundle;
        if (errMsgBundle == null) {
            errMsgBundle = Locale.getInstallResourceBundle(BUNDLE_NAME);
        }

        String errorMsg=null;
        String templateName=null;
        String resProperty = errMsgBundle.getString(errorCode);
        if (utilDebug.messageEnabled()) {
            utilDebug.message("errorCod='{}', resProperty='{}'", errorCode, resProperty);
        }
        if ((resProperty != null) && (resProperty.length() != 0)) {
            int commaIndex = resProperty.indexOf(MSG_DELIMITER);
            if (commaIndex != -1) {
                templateName = 
                    resProperty.substring(commaIndex+1,resProperty.length());
                errorMsg = resProperty.substring(0,commaIndex);
            } else {
                errorMsg = resProperty;
            }
        }

        if (ERROR_MESSAGE.equals(type)) {
            return errorMsg;
        } else if (ERROR_TEMPLATE.equals(type)) {
            return templateName;
        } else {
            return null;
        }
    }

    public static boolean isCookieSupported(HttpServletRequest req) {
        boolean cookieSupported = true;
        String cookieSupport = getCookieSupport(getClientType(req));
        if ((cookieSupport != null) && "false".equals(cookieSupport)) {
            cookieSupported = false;
        }
        return cookieSupported;
    }

    public static boolean isCookieSet(HttpServletRequest req) {
        boolean cookieSet = false;
        String cookieSupport = getCookieSupport(getClientType(req));
        boolean cookieDetect = getCookieDetect(cookieSupport);
        if (isClientDetectionEnabled() && cookieDetect) {
            cookieSet = true;
        }
        return cookieSet;
    }

    public static Cookie createCookie(String name, String value, int maxAge, String cookieDomain) {

        Cookie pCookie = CookieUtils.newCookie(name, value, "/", cookieDomain);
        if (maxAge >= 0) {
            pCookie.setMaxAge(maxAge);
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("pCookie='{}'", pCookie);
        }

        return pCookie;
    }

    public static Cookie createlbCookie(String cookieDomain) throws AuthException {
        Cookie lbCookie = null;
        try {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("cookieDomain : " + cookieDomain);
            }
            String cookieName = getlbCookieName();
            String cookieValue = getlbCookieValue();
            lbCookie =
                    createCookie(
                            cookieName, cookieValue, -1, cookieDomain);
            return (lbCookie);
        } catch (Exception e) {
            utilDebug.message("Unable to create Load Balance Cookie");
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
    }

    /**
     * Returns the Cookie object created based on the <code>cookieName</code>,
     * Session ID and <code>cookieDomain</code>.
     * If <code>AuthContext,/code> status is not <code>SUCCESS</code> then
     * cookie is created with authentication cookie Name, else AM Cookie Name
     * will be used to create cookie.
     *
     * @param ac the AuthContext object
     * @param cookieDomain the cookie domain for creating cookie.
     * @return Cookie object.
     */
    public static Cookie getCookieString(AuthContext ac, String cookieDomain) {
        Cookie cookie = null;
        String cookieName = getAuthCookieName();
        String cookieValue = serverURL + serviceURI;
        try {
            if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                cookieName = getCookieName();
                cookieValue = ac.getAuthIdentifier();
                utilDebug.message("Create AM cookie");
            }
            cookie = createCookie(cookieName,cookieValue,cookieDomain);
            if (CookieUtils.isCookieSecure()) {
                cookie.setSecure(true);
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error getCookieString : ", e);
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Cookie is : " + cookie);
        }
        return cookie;
    }

    /**
     ( Returns URL with the cookie value in the URL. The cookie in the
     * re-written URL will have the AM cookie if session is active/inactive
     * and authentication cookie if session is invalid.
     *
     * @param url URL to be encoded.
     * @param request HTTP Servlet Request.
     * @param ac Authentication Context.
     * @return the encoded URL.
     */
    public static String encodeURL(
        String url,
        HttpServletRequest request,
        AuthContext ac) {
        if (isCookieSupported(request)) {
            return (url);
        }

        String cookieName = getAuthCookieName();
        if (ac.getStatus() == AuthContext.Status.SUCCESS) {
            cookieName = getCookieName();
        }

        String encodedURL = url;
        if (urlRewriteInPath) {
            encodedURL = encodeURL(url,SessionUtils.SEMICOLON,false, cookieName,ac.getAuthIdentifier());
        } else {
            encodedURL = encodeURL(url,SessionUtils.QUERY,true, cookieName,ac.getAuthIdentifier());
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("encodeURL : URL='{}', \nRewritten URL='{}'", url, encodedURL);
        }
        return encodedURL;
    }

    private static String encodeURL(String url, short encodingScheme,boolean escape,
            String cookieName, String strSessionID) {
        String cookieStr = SessionEncodeURL.createCookieString(cookieName,strSessionID);
        return SessionEncodeURL.encodeURL(cookieStr,url, encodingScheme,escape);
    }

    /**
     * Returns the resource based on the default values.
     *
     * @param request HTTP Servlet Request.
     * @param fileName name of the file
     * @param locale Locale used for the search.
     * @param servletContext Servlet Context for server
     * @return Path to the resource.
     */
    public static String getDefaultFileName(
        HttpServletRequest request,
        String fileName,
        java.util.Locale locale, 
        ServletContext servletContext) {

        String strlocale = "";
        if (locale != null) {
            strlocale = locale.toString();
        }
        String filePath = getFilePath(getClientType(request));
        String fileRoot = getFileRoot();
        String orgDN;
        try {
            orgDN = getDomainNameByRequest(request, parseRequestParameters(request));
        } catch (Exception ex) {
            //in case we are unable to determine the realm from the incoming
            //requests, let's fallback to top level realm
            orgDN = getOrganizationDN("/", false, request);
        }
        String orgFilePath = getOrgFilePath(orgDN);

        String templateFile = null;
        try {
            templateFile = ResourceLookup.getFirstExisting(servletContext, fileRoot, strlocale, orgFilePath, filePath,
                    fileName, templatePath);
        } catch (Exception e) {
            templateFile = new StringBuilder()
                    .append(templatePath)
                    .append(fileRoot)
                    .append(Constants.FILE_SEPARATOR)
                    .append(fileName).toString();
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getDefaultFileName:templateFile is :" + templateFile);
        }
        return templateFile;
    }

    /* get the root suffix , eg. o= isp */
    public static String getRootSuffix() {
        // rootSuffix is already normalized in SMSEntry
        return rootSuffix;
    }

    /* get the root dir to start lookup from./<default org>
     * default is /default
     */
    protected static String getFileRoot() {
        String fileRoot = ISAuthConstants.DEFAULT_DIR;
        String rootOrgName = DNUtils.DNtoName(rootSuffix);
        if (utilDebug.messageEnabled()) {
            utilDebug.message("rootOrgName is : " + rootOrgName);
        }
        if (rootOrgName != null) {
            fileRoot = rootOrgName;
        }
        return (fileRoot);
    }

    /* insert chartset in the filename */
    private static String getCharsetFileName(String fileName) {
        ISLocaleContext localeContext = new ISLocaleContext();
        String charset = localeContext.getMIMECharset();
        if (fileName == null) {
            return (null);
        }

        int i = fileName.indexOf(".");
        String charsetFilename = null;
        if (i != -1) {
            charsetFilename = fileName.substring(0, i) + "_" + charset +
                fileName.substring(i);
        } else {
            charsetFilename = fileName + "_" + charset;
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("charsetFilename is : "+ charsetFilename);
        }
        return charsetFilename;
    }

    /* retrieve the resource (file) using resource lookup */
    public static String getResourceLocation(String fileRoot, String localeName,
        String orgFilePath,String filePath,String filename,String templatePath,
        ServletContext servletContext,HttpServletRequest request) {
        String resourceName = null;
        String clientType = getClientType(request);
        if ((clientType != null) &&
            (!clientType.equals(getDefaultClientType()))) {
            // non-HTML client
            String charsetFileName = getCharsetFileName(filename);
            resourceName =
                ResourceLookup.getFirstExisting(servletContext,fileRoot,
                localeName,orgFilePath,
                filePath,charsetFileName,
                templatePath);
        }
        if (resourceName == null) {
            resourceName = ResourceLookup.getFirstExisting(servletContext,
                fileRoot,localeName,
                orgFilePath,
                filePath,filename,
                templatePath);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("resourceName='{}'", resourceName);
        }
        return resourceName;
    }

    /* constructs the filePath parameter for FileLookUp
     * filePath = indexName (service name) + clientPath (eg. html).
     */
    public static String getFilePath(HttpServletRequest request, AuthContext.IndexType indexType, String indexName) {

        String filePath = getFilePath(getClientType(request));
        String serviceName = null;
        StringBuilder filePathBuffer = new StringBuilder();
        // only if index name is service type then need it
        // as part of the filePath since  service can have
        // have different auth template

        if (AuthContext.IndexType.SERVICE.equals(indexType)) {
            serviceName = indexName;
        }

        if (filePath == null && serviceName == null) {
            return null;
        }

        if (filePath != null && !filePath.isEmpty()) {
            filePathBuffer.append(Constants.FILE_SEPARATOR).append(filePath);
        }

        if (serviceName != null && !serviceName.isEmpty()) {
            // To avoid issues with case-sensitive filesystems, always use the lowercase version of the serviceName
            filePathBuffer.append(Constants.FILE_SEPARATOR).append(serviceName.toLowerCase());
        }

        String newFilePath = filePathBuffer.toString();
        if (utilDebug.messageEnabled()) {
            utilDebug.message("FilePath='{}'", newFilePath);
        }

        return newFilePath;
    }

    /* retrieves the org path to search resource
     * eg. if orgDN = o=org1,o=org11,o=org12,dc=iplanet,dc=com
     * then orgFilePath will be org12/org11/org1
     */
    public static String getOrgFilePath(String orgDN) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getOrgFilePath : orgDN is: " + orgDN);
        }
        String normOrgDN = DNUtils.normalizeDN(orgDN);
        String orgPath = null;

        if (normOrgDN != null) {
            StringBuilder orgFilePath = new StringBuilder();
            String remOrgDN = normOrgDN;
            String orgName = null;
            while ((remOrgDN != null) && (remOrgDN.length() != 0)
                && !remOrgDN.equals(getRootSuffix())) {
                orgName = DNUtils.DNtoName(remOrgDN);
                orgFilePath = orgFilePath.insert(0,
                    Constants.FILE_SEPARATOR+
                    orgName);
                int i = remOrgDN.indexOf(",");
                if (i != -1) {
                    remOrgDN = remOrgDN.substring(i+1);
                } else {
                    break;
                }
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("remOrgDN is : "+ remOrgDN);
                }
            }
            orgPath = orgFilePath.toString();
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("getOrgFilePath: orgPath is : " + orgPath);
        }
        return orgPath;
    }

    /**
     * Returns the File name based on the given input values.
     *
     * @param fileName Name of the file.
     * @param localeName Locale name.
     * @param orgDN Organization distinguished name.
     * @param servletRequest HTTP Servlet Request.
     * @param servletContext Servlet Context for server.
     * @param indexType AuthContext Index Type.
     * @param indexName index name associated with the index type.
     * @return File name of the resource.
     */
    public static String getFileName(
        String fileName,
        String localeName,
        String orgDN,
        HttpServletRequest servletRequest,
        ServletContext servletContext,
        AuthContext.IndexType indexType,
        String indexName
    ) {
        String fileRoot = getFileRoot();
        String templateFile = null;
        try {
            // get the filePath  Client filePath + serviceName
            String filePath = getFilePath(servletRequest,indexType,indexName);
            String orgFilePath = getOrgFilePath(orgDN);

            if (utilDebug.messageEnabled()) {
                utilDebug.message("Calling ResourceLookup: filename='{}', defaultOrg='{}'," +
                                " locale='{}', filePath='{}', orgPath='{}'",
                        fileName, fileRoot, localeName, filePath, orgFilePath);
            }

            templateFile = getResourceLocation(fileRoot,localeName,orgFilePath,
                filePath,fileName,templatePath,servletContext,servletRequest);
        } catch (Exception e) {
            utilDebug.message("Error getting File : ", e);
            templateFile = new StringBuilder()
                    .append(templatePath)
                    .append(Constants.FILE_SEPARATOR)
                    .append(ISAuthConstants.DEFAULT_DIR)
                    .append(Constants.FILE_SEPARATOR)
                    .append(fileName)
                    .toString();
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("File/Resource is : " + templateFile);
        }
        return (templateFile);
    }

    public static String getAuthCookieValue(HttpServletRequest request) {
        //Let's check the URL first in case this is a forwarded request from Federation. URL should have precedence
        //over the actual cookie value, so this way a new federated auth can always start with a clear auth session.
        String isForward = (String) request.getAttribute(Constants.FORWARD_PARAM);
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthClientUtils.getAuthCookieValue: is forward = " + isForward);
        }
        String ret = null;
        if (Constants.FORWARD_YES_VALUE.equals(isForward)) {
            ret = SessionEncodeURL.getSidFromURL(request, getAuthCookieName());
        }

        return ret == null ? CookieUtils.getCookieValueFromReq(request, getAuthCookieName()) : ret;
    }

    /**
     * @deprecated use {@link #getDomainNameByRequest(
     * jakarta.servlet.http.HttpServletRequest, java.util.Map<String, String>)} instead.
     */
    public static String getDomainNameByRequest(Map<String, String> requestHash) {
        String realm = getRealmFromPolicyAdvice(requestHash);
        String orgParam = getOrgParam(requestHash);
        if (realm != null) {
            //Policy Advice has precedence over GET parameter
            orgParam = realm;
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgParam='{}'", orgParam);
        }
        // try to get the host name if org or domain Param is null
        if ((orgParam == null) || (orgParam.length() == 0)) {
            orgParam = "/";
            if (utilDebug.messageEnabled()) {
                utilDebug.message("defaultOrg : " + orgParam);
            }
        }
        String orgDN = getOrganizationDN(orgParam,false,null);

        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgDN is " + orgDN);
        }
        return orgDN;
    }          

    /**
     * Parses the policy condition advice and checks for realm advices
     * @param requestHash Request parameters
     * @return realm defined in the policy advice, if defined - or nullđ
     * @throws IllegalArgumentException if more than one realm is defined within
     * the advice
     * @see com.sun.identity.authentication.util.AMAuthUtils
     */
    private static String getRealmFromPolicyAdvice(Map<String, String> requestHash) {
    		if (requestHash!=null) {
	        String advice = requestHash.get(COMPOSITE_ADVICE);
	        if (advice == null) {
	            return null;
	        }
	
	        try {
	            String decodedXml = URLDecoder.decode(advice, "UTF-8");
	            return getRealmFromPolicyAdvice(decodedXml);
	        } catch (UnsupportedEncodingException uee) {
	            utilDebug.error("Unable to URLdecode condition advice using UTF-8");
	        }
    		}
        return null;
    }

    /**
     * Parses the policy condition advice and checks for realm advices
     * @param advice The policy advice XML
     * @return realm defined in the policy advice, if defined - or nullđ
     * @throws IllegalArgumentException if more than one realm is defined within
     * the advice
     * @see com.sun.identity.authentication.util.AMAuthUtils
     */
    public static String getRealmFromPolicyAdvice(String advice) {
        String realm = null;
        try {
            Map<String, Set<String>> adviceMap = PolicyUtils.parseAdvicesXML(advice);
            if (adviceMap != null) {
                for (Map.Entry<String, Set<String>> entry : adviceMap.entrySet()) {
                    String key = entry.getKey();
                    Set<String> value = entry.getValue();
                    for (String adv : value) {
                        String tmpRealm = null;
                        if (key.equals(AuthSchemeCondition.AUTHENTICATE_TO_REALM_CONDITION_ADVICE)) {
                            tmpRealm = adv;
                        } else {
                            //AMAuthUtils is not present at DAS, so let's parse
                            //the advice manually
                            int idx = adv.indexOf(':');
                            if (idx != -1) {
                                tmpRealm = adv.substring(0, idx);
                            }
                        }
                        if (realm == null) {
                            realm = tmpRealm;
                        } else if (tmpRealm != null && !realm.equalsIgnoreCase(tmpRealm)) {
                            //NB: this method is also used when the engine wants
                            //to show the error page from the correct realm, hence
                            //this will fail twice, resulting in a generic error
                            //page
                            throw new IllegalArgumentException("More than one realm defined in the Policy Advice");
                        }
                    }
                }
            }
        } catch (PolicyException pe) {
            utilDebug.error("Unable to parse policy condition advices", pe);
        }

        return realm;
    }

    // Check whether the request is coming to the server who created the
    // original Auth request or session
    public static boolean isLocalServer(String cookieURL, boolean isServer) {
        boolean local = false;
        try {
            String urlStr   = serverURL + serviceURI;

            if (utilDebug.messageEnabled()) {
                utilDebug.message("This server URL='{}', Server URL from cookie='{}'", urlStr, cookieURL);
            }

            if ((urlStr != null) && (cookieURL != null) &&
                (cookieURL.equalsIgnoreCase(urlStr))) {
                local = true;
            }
            if (!local && isServer && (cookieURL != null)) {
                int uriIndex = cookieURL.indexOf(serviceURI);
                String tmpCookieURL = cookieURL;
                if (uriIndex != -1) {
                    tmpCookieURL = cookieURL.substring(0,uriIndex) + 
                        SystemProperties.get(Constants.
                        AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
                }
                Set<String> platformList = WebtopNaming.getPlatformServerList();
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("search CookieURL='{}', platform server List='{}' ", tmpCookieURL, platformList);
                }
                // if cookie URL is not in the Platform server list then
                // consider as new authentication for that local server
                if (!platformList.contains(tmpCookieURL)) {
                    local = true;
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error isLocalServer : " + e.getMessage());
            }
        }
        return local;
    }
    // Check whether the request is coming to the server who created the
    // original Auth request or session
    // This method needs to be merged with the one above.
    public static boolean isLocalServer(String cookieURL, String inputURI) {
        int uriIndex = cookieURL.indexOf(inputURI);
        String tmpCookieURL = cookieURL;
        if (uriIndex != -1) {
            tmpCookieURL = cookieURL.substring(0,uriIndex);
        }
        return isLocalServer(tmpCookieURL + serviceURI, true);
    }
    
    public static boolean isServerMemberOfLocalSite(String cookieURL) {
        boolean isSiteMember = false;
        
        try {
            if (!distAuthSitesMap.isEmpty()) {
                String localSiteID = WebtopNaming.getSiteID(WebtopNaming.getAMServerID());
                
                if (localSiteID == null) {
                    if (utilDebug.warningEnabled()) {
                        utilDebug.warning("AuthClientUtils::isServerMemberOfLocalSite:" +
                                "unable to determine local site id: " + WebtopNaming.getAMServerID());
                    }
                    
                    return false;
                }
                
                String localSiteName = WebtopNaming.getSiteNameById(localSiteID);
                
                if (localSiteName != null) {
                    Set distAuthForSite = distAuthSitesMap.get(localSiteName);
                    
                    if (distAuthForSite == null) {
                        if (utilDebug.warningEnabled()) {
                            utilDebug.warning("AuthClientUtils::isServerMemberOfLocalSite:" +
                                "unable to determine distAuthForSite: " + localSiteName);
                        }
                    
                        return false;   
                    }
                    
                    if (distAuthForSite.contains(cookieURL)) {
                        isSiteMember = true;

                        if (utilDebug.messageEnabled()) {
                            utilDebug.message("AuthClientUtils::isServerMemberOfLocalSite:" +
                                    "local URL " + cookieURL + " found in local site " +
                                    distAuthForSite);
                        }
                    }
                } else {
                    isSiteMember = true;
                }
            } else {
                isSiteMember = true;
            }
        } catch (Exception ex) {
            utilDebug.error("AuthClientUtils::isServerMemberOfLocalSite: ", ex);
        }
        
        return isSiteMember;
    }

    /**
     * Sends the request to the original Auth server and receives the result
     * data.
     *
     * @param request HttpServletRequest to be sent
     * @param response HttpServletResponse to be received
     * @param cookieURL URL of the original authentication server to be
     * connected
     *
     * @return HashMap of the result data from the original server's response
     *
     */
    public static Map<String, Object> sendAuthRequestToOrigServer(HttpServletRequest request,
        HttpServletResponse response, String cookieURL) {
        Map<String, Object> origRequestData = new HashMap<String, Object>();

        // Print request Headers
        if (utilDebug.messageEnabled()) {
            StringBuilder message = new StringBuilder();
            Enumeration<String> requestHeaders = request.getHeaderNames();
            while (requestHeaders.hasMoreElements()) {
                String name = requestHeaders.nextElement();
                Enumeration value = (Enumeration) request.getHeaders(name);
                message.append("Header name='").append(name).append("', Value='").append(value).append("'\n");
            }
            utilDebug.message(message.toString());
        }

        // Open URL connection
        HttpURLConnection conn = null;
        OutputStream  out = null;
        String strCookies = null;
        URL authURL = null;
        try {
            String queryString = request.getQueryString();

            if (queryString != null) {
                authURL = new URL(cookieURL + "?" + queryString);
            } else {
                authURL = new URL(cookieURL);
            }

            if (utilDebug.messageEnabled()) {
                utilDebug.message("Connecting to : " + authURL);
            }

            conn = HttpURLConnectionManager.getConnection(authURL);
            conn.setUseCaches(useCache);
            conn.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty(ISAuthConstants.ACCEPT_LANG_HEADER,
                    request.getHeader(ISAuthConstants.ACCEPT_LANG_HEADER));
            // We should preserve the original host, so the target server will also see the accessed URL
            // If we don't do this the server might going to deny the request because of invalid domain access.
            conn.setRequestProperty("Host", request.getHeader("host"));

            List<Cookie> cookies = removeLocalLoadBalancingCookie(asList(request.getCookies()));
            // replay cookies
            strCookies = getCookiesString(cookies);
            if (strCookies != null) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Sending cookies : " + strCookies);
                }
                conn.setRequestProperty("Cookie", strCookies);
            }

            // Sending Output to Original Auth server...
            utilDebug.message("SENDING DATA ... ");
            copyRequestHeaders(request, conn);

            if (request.getMethod().equals("GET")) {
                conn.connect();
            } else {
                //First we should find out what GET parameters do we have.
                Map<String, Set<String>> queryParams = new HashMap<String, Set<String>>();
                if (queryString != null) {
                    for (String param : queryString.split("&")) {
                        int idx = param.indexOf('=');
                        if (idx != -1) {
                            String paramName = param.substring(0, idx);
                            String paramValue = param.substring(idx + 1);
                            Set<String> values = queryParams.get(paramName);
                            if (values == null) {
                                values = new HashSet<String>();
                                queryParams.put(paramName, values);
                            }
                            values.add(paramValue);
                        }
                    }
                }

                conn.setRequestProperty(
                    "Content-Type", "application/x-www-form-urlencoded");
                // merged parameter list containing both GET and POST parameters
                Map<String, String[]> params = request.getParameterMap();
                Map<String, Set<String>> postParams = new HashMap<String, Set<String>>();

                for (Map.Entry<String, String[]> entry : params.entrySet()) {
                    if (queryParams.containsKey(entry.getKey())) {
                        // TODO: do we need to care about params that can be both in GET and POST?
                    } else {
                        Set<String> values = new HashSet<String>();
                        for (String value : entry.getValue()) {
                            values.add(getCharDecodedField(value, "UTF-8"));
                        }
                        postParams.put(entry.getKey(), values);
                    }
                }

                String postData = getFormData(postParams);
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Request data : " + postData);
                }
                if (postData.trim().length() > 0) {
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    out = conn.getOutputStream();
                    PrintWriter pw = new PrintWriter(out);
                    pw.print(postData); // here we "send" the request body
                    pw.flush();
                    pw.close();
                }
            }

            // Receiving input from Original Auth server...
            utilDebug.message("RECEIVING DATA ... ");
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Response Code='{}', Response Message='{}' ", conn.getResponseCode(),
                        conn.getResponseMessage());
            }

            // Check response code
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                // Input from Original servlet...
                StringBuilder in_buf = new StringBuilder();
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
                int len;
                char[] buf = new char[1024];
                while ((len = in.read(buf,0,buf.length)) != -1) {
                    in_buf.append(buf,0,len);
                }
                String in_string = in_buf.toString();
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Received response data : " + in_string);
                }
                origRequestData.put("OUTPUT_DATA",in_string);

            } else {
                utilDebug.warning("Response code for proxied auth is NOT OK");
            }

            String client_type = conn.getHeaderField("AM_CLIENT_TYPE");
            if (client_type != null) {
                origRequestData.put("AM_CLIENT_TYPE", client_type);
            }
            String redirect_url = conn.getHeaderField("Location");
            if (redirect_url != null) {
                try {
                    URL gotoURL = new URL(redirect_url);
                    if (isSameServer(authURL, gotoURL)) {
                        if (utilDebug.messageEnabled()) {
                            utilDebug.message("Relative redirect detected");
                        }
                        //relative redirect happened
                        String path = gotoURL.getPath();
                        String query = gotoURL.getQuery();
                        redirect_url = (path != null ? path : "") + (query != null ? "?" + gotoURL.getQuery() : "");
                    }
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("sendAuthRequestToOrigServer(): Setting redirect URL to: " + redirect_url);
                    }
                    origRequestData.put("AM_REDIRECT_URL", redirect_url);
                } catch (MalformedURLException murle) {
                    //fallback to original handling
                    origRequestData.put("AM_REDIRECT_URL", redirect_url);
                }
            }
            String content_type = conn.getHeaderField("Content-Type");
            if (content_type != null) {
                origRequestData.put("CONTENT_TYPE", content_type);
            }
            origRequestData.put("RESPONSE_CODE", conn.getResponseCode());

            //replay received headers to the original response
            copyResponseHeaders(conn.getHeaderFields(), response);
        } catch (IOException ioe) {
            //the catcher will log the exception
            origRequestData.put("EXCEPTION", ioe);
        } catch (Exception e) {
            if (utilDebug.warningEnabled()) {
                utilDebug.warning("send exception : " , e);
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("send IOException : ", ioe);
                    }
                }
            }
        }

        return origRequestData;
    }

    /**
     * Filter the load balancing cookie if it points to this server to avoid potential infinite redirect loop.
     */
    private static List<Cookie> removeLocalLoadBalancingCookie(final List<Cookie> cookies) {
        final String lblCookieName = getlbCookieName();
        final String lblCookieValue = getlbCookieValue();
        final List<Cookie> filteredCookies = new ArrayList<>();
        for (final Cookie cookie : cookies) {
            if (!Objects.equals(cookie.getName(), lblCookieName)
                    && !Objects.equals(cookie.getValue(), lblCookieValue)) {
                filteredCookies.add(cookie);
            }
        }
        return filteredCookies;
    }

    private static boolean isSameServer(URL url1, URL url2) {
        int port1 = url1.getPort() != -1 ? url1.getPort() : url1.getDefaultPort();
        int port2 = url2.getPort() != -1 ? url2.getPort() : url2.getDefaultPort();
        return url1.getProtocol().equals(url2.getProtocol())
                && url1.getHost().equalsIgnoreCase(url2.getHost())
                && port1 == port2;
    }

    private static void copyRequestHeaders(HttpServletRequest request, HttpURLConnection conn) {
        utilDebug.message("AuthClientUtils.copyRequestHeaders: starting to copy request headers");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName != null && RETAINED_HTTP_REQUEST_HEADERS.contains(headerName.toLowerCase())) {
                Enumeration<String> values = request.getHeaders(headerName);
                while (values.hasMoreElements()) {
                    String value = values.nextElement();
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Copying header for proxied request: " + headerName + ": " + value);
                    }
                    conn.addRequestProperty(headerName, value);
                }
            }
        }
    }

    private static void copyResponseHeaders(Map<String, List<String>> headers, HttpServletResponse response) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            if (headerName != null && RETAINED_HTTP_HEADERS.contains(headerName.toLowerCase())) {
                List<String> headerValues = entry.getValue();
                if (headerValues != null) {
                    for (String headerValue : headerValues) {
                        response.addHeader(headerName, headerValue);
                    }
                }
            }
        }
    }

    // Gets the request form data in the form of string
    private static String getFormData(Map<String, Set<String>> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : params.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                sb.append(URLEncDec.encode(key));
                sb.append('=');
                sb.append(URLEncDec.encode(value));
                sb.append('&');
            }
        }

        sb.deleteCharAt(sb.length() -1);

        return(sb.toString());
    }

    // Get cookies string from HTTP request object
    private static String getCookiesString(List<Cookie> cookies) {
        StringBuilder cookieStr = null;
        String strCookies = null;
        // Process Cookies
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Cookie name='{}', value='{}'", cookie.getName(), cookie.getValue());
                }
                if (cookieStr == null) {
                    cookieStr = new StringBuilder();
                } else {
                    cookieStr.append(";");
                }
                cookieStr.append(cookie.getName()).append("=").append(cookie.getValue());
            }
        }
        if (cookieStr != null) {
            strCookies = cookieStr.toString();
        }
        return (strCookies);
    }
    /**
    * Sets server cookie to <code>HttpServletResponse</code> object
    * @param aCookie auth context associated with lb cookie
    * @param response <code>true</code> if it is persistent
    * @throws AuthException if it fails to create pcookie
    */
    public static void setServerCookie(Cookie aCookie,
            HttpServletRequest request, HttpServletResponse response)
            throws AuthException {
        String cookieName = aCookie.getName();
        String cookieValue = aCookie.getValue();
        if (cookieName != null && cookieName.length() != 0) {
            Set<String> domains = getCookieDomainsForRequest(request);
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createCookie(cookieName, cookieValue,
                        domain);
                    CookieUtils.addCookieToResponse(response, cookie);
                }
            } else {
                CookieUtils.addCookieToResponse(response,
                        createCookie(cookieName,cookieValue,null));
            }
        }
    } 

    /**
     * Sets the redirectBackUrlCookie to be set as OpenAM
     * server URL when redirecting to external web site during authentication
     * process.
     * @param cookieName auth context associated with lb cookie
     * @param cookieValue auth context associated with lb cookie
     * @param response <code>true</code> if it is persistent
     * @throws AuthException if it fails to create this cookie
     */
    public static void setRedirectBackServerCookie(String cookieName, 
            String cookieValue, HttpServletRequest request,
            HttpServletResponse response) throws AuthException {

        if (cookieName != null && cookieName.length() != 0) {
            Set<String> domains = getCookieDomainsForRequest(request);
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createCookie(cookieName, cookieValue,
                        domain);
                    CookieUtils.addCookieToResponse(response, cookie);
                }
            } else {
                CookieUtils.addCookieToResponse(response,
                        createCookie(cookieName,cookieValue,null));
            }
        }
    }

    /**
     * Clears server cookie.
     *
     * @param cookieName Cookie Name.
     * @param response HTTP Servlet Response.
     */
    public static void clearServerCookie(String cookieName,
            HttpServletRequest request, HttpServletResponse response) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("In clear server Cookie = " +  cookieName);
        }
        if (cookieName != null && cookieName.length() != 0) {
            Set<String> domains = getCookieDomainsForRequest(request);
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie =
                            createCookie(cookieName, "LOGOUT", 0, domain);
                    response.addCookie(cookie);
                    utilDebug.message("In clear server Cookie added cookie");
                }
            } else {
                response.addCookie(
                        createCookie(cookieName, "LOGOUT", 0, null));
                utilDebug.message("In clear server added cookie no domain");
            }
        }
    }

    // Returns Query String from request parameters Map
    public static String getQueryStrFromParameters(Map paramMap) {
        StringBuilder buff = new StringBuilder();
        boolean first = true;

        if (paramMap != null && !paramMap.isEmpty()) {
            for (Iterator i = paramMap.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry me = (Map.Entry)i.next();
                String key = (String)me.getKey();
                String value = (String)me.getValue();

                if (first) {
                    buff.append("?");
                    first = false;
                } else {
                    buff.append("&");
                }

                buff.append(key).append("=").append(value);
            }
        }
        return (buff.toString());
    }

    /**
     * Checks whether OpenAM session cookie has to be made
     * persistent.
     *
     * Only if value of <code>true</code> is providued for HTTP query
     * parameter <code>Constants.PERSIST_AM_COOKIE</code> and this property is
     * enabled or if persistent cookies are set globally.
     *
     * If either of these are true, AM session cookie will be made persistent
     *
     * @param reqDataHash http request parameters and values
     * @return <code>true</code> if AM session cookie has to be made persistent,
     *        otherwise returns <code>false</code>
     */
     public static boolean persistAMCookie(Hashtable reqDataHash) {
        String globalPersistCookieString = SystemProperties.get(
            Constants.PERSIST_AM_COOKIE);
        boolean globalPersist =
            Boolean.valueOf(globalPersistCookieString).booleanValue();

        if (globalPersist) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils.persistAMCookie(): Set globally ");
            }

            return true;
        }

        boolean persistCookie = false;
        String persistCookieString
                = (String)reqDataHash.get(Constants.PERSIST_AM_COOKIE);
        String allowRequestPersistString = SystemProperties.get(
                Constants.ALLOW_PERSIST_AM_COOKIE);
        boolean allowRequestPersist =
                Boolean.valueOf(allowRequestPersistString).booleanValue();

         if (allowRequestPersist && (persistCookieString != null)) {
             persistCookie
                     = (Boolean.valueOf(persistCookieString)).booleanValue();
         }

         if (utilDebug.messageEnabled()) {
              utilDebug.message("AuthUtils.persistAMCookie(): " + persistCookie);
         }

         return persistCookie;
     }
    

    /**
     * Returns true if the request has the ForceAuth=<code>true</code>
     * query parameter or composite advise.
     *
     * @return true if this parameter is present otherwise false.
     */
    public static boolean forceAuthFlagExists(Hashtable reqDataHash) {
        String force = (String) reqDataHash.get("ForceAuth");
        boolean forceFlag = (Boolean.valueOf(force)).booleanValue();
        if (utilDebug.messageEnabled()) {
             utilDebug.message("AuthUtils.forceFlagExists : " + forceFlag);
        }
        if (forceFlag == false) {
            if ( reqDataHash.get(Constants.COMPOSITE_ADVICE) != null ) {
                String tmp = (String)reqDataHash.
                    get(Constants.COMPOSITE_ADVICE);
                forceFlag =
                checkForForcedAuth(tmp);
            }
        }
        return forceFlag;
    }

    /**
     * Returns true if the composite Advice has the ForceAuth element
     *
     * @return true if this parameter is present otherwise false.
     */
    public static boolean checkForForcedAuth(String xmlCompositeAdvice) {
        boolean returnForcedAuth = false;
        try {
            String decodedAdviceXML = URLDecoder.decode(xmlCompositeAdvice);
            Map adviceMap = PolicyUtils.parseAdvicesXML(decodedAdviceXML);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils.checkForForcedAuth : decoded XML "
                    + "= " + decodedAdviceXML);
                utilDebug.message("AuthUtils.checkForForcedAuth : result Map = "
                + adviceMap);
            }
            if (adviceMap != null) {
                if (adviceMap.containsKey(AuthSchemeCondition.
                    FORCE_AUTH_ADVICE)) {
                    returnForcedAuth = true;
                }
            }
        } catch  (com.sun.identity.policy.PolicyException polExp) {
            utilDebug.error("AuthUtils.checkForForcedAuth : Error in "
                + "Policy  XML parsing ",polExp );
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils.checkForForcedAuth: returnForcedAuth"+
                "= " + returnForcedAuth);
        }
        return returnForcedAuth;
    }
    
    /** 
     * Returns the service URI
     * @return a String the Service URI
     */
    public static String getServiceURI() {
        if (SystemProperties.isServerMode()) {
            return SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        } else {
            return SystemProperties.get(Constants.AM_DISTAUTH_DEPLOYMENT_DESCRIPTOR);
        }
    }

    public static void setHostUrlCookie(HttpServletResponse response) {
        if (isSessionHijackingEnabled) {
            String hostUrlCookieValue = null;
            try {
                String siteID = WebtopNaming.getSiteID(
                        WebtopNaming.getAMServerID());
                hostUrlCookieValue = WebtopNaming.getServerFromID(siteID);
                String uri = SystemProperties.get(
                        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
                hostUrlCookieValue = hostUrlCookieValue.substring(0, 
                        (hostUrlCookieValue.length() - uri.length()));
            } catch(ServerEntryNotFoundException e) {
                utilDebug.message("AuthClientUtils.setHostUrlCookie:", e);
            }

            if (hostUrlCookieValue == null ||
                    hostUrlCookieValue.length() == 0) {
                String authServerProtocol = SystemProperties.get(
                        Constants.AM_SERVER_PROTOCOL);
                String authServer = SystemProperties.get(
                        Constants.AM_SERVER_HOST);
                String authServerPort = SystemProperties.get(
                        Constants.AM_SERVER_PORT);
                hostUrlCookieValue   = authServerProtocol + "://" +
                        authServer + ":" + authServerPort;
            }
            
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthClientUtils.setHostUrlCookie: " +
                    "hostUrlCookieName = " + hostUrlCookieName +
                    ", hostUrlCookieDomain = " + hostUrlCookieDomain +
                    ", hostUrlCookieValue = " + hostUrlCookieValue);
            }
            
            // Create Cookie
            try {
                Cookie cookie = createCookie(hostUrlCookieName,
                hostUrlCookieValue, hostUrlCookieDomain);
                CookieUtils.addCookieToResponse(response, cookie);
            } catch (Exception e) {
                utilDebug.message("AuthClientUtils.setHostUrlCookie:", e);
            }
        }
    }

    public static void clearHostUrlCookie(HttpServletResponse response) {
        if (isSessionHijackingEnabled) {
            // Create Cookie
            try {
                Cookie cookie = createCookie(hostUrlCookieName,
                "LOGOUT", hostUrlCookieDomain);
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            } catch (Exception e) {
                utilDebug.message("AuthClientUtils.clearHostUrlCookie:", e);
            }
        }
    }
    
    public static boolean isDistAuthServerTrusted(String distAuthServerLoginURL){
        return distAuthClusterList.contains(distAuthServerLoginURL);
    }
    
    /**
     * Returns the resource URL. The method checks value for "resourceURL" 
     * parameter first, if not present, checks value for "goto" parameter. 
     * If none exists, returns null.
     * @param request HttpServletRequest object
     * @return resourceURL based on the query parameters, returns null if
     *      resource URL could not be found.
     */
    public static String getResourceURL(HttpServletRequest request) {
        String resourceUrl = request.getParameter(
            ISAuthConstants.RESOURCE_URL_PARAM);
        if (resourceUrl == null) {
            resourceUrl = 
                request.getParameter(ISAuthConstants.GOTO_PARAM);
        }
        return resourceUrl;
    }

    /**
     * Returns an environment map which contains all query parameters
     * and HTTP headers. Keys of the map are String, values of the map are
     * Sets of String.
     * @param request HttpServletRequest object.
     * @return environment Map whose key is String, and value is Set of String.
     */
    public static Map getEnvMap(HttpServletRequest request) {
        Map envParameters = new HashMap();
        // add all query parameters
        String strIP = ClientUtils.getClientIPAddress(request);
        if (strIP != null) {
            Set ipSet = new HashSet(1);
            ipSet.add((String) strIP);
            envParameters.put(ISAuthConstants.REQUEST_IP,ipSet);
        }
        Enumeration enum1 = request.getParameterNames();
        while (enum1.hasMoreElements()) {
            String paramName = (String) enum1.nextElement();
            String[] values = request.getParameterValues(paramName);
            if (values != null) {
                Set set = new HashSet();
                for (int i = 0; i < values.length; i++) {
                    set.add((String) values[i]);
                }
                if (!set.isEmpty()) {
                    envParameters.put(paramName, set);
                }
            }
        }

        // add all headers
        enum1 = request.getHeaderNames();
        if (enum1 != null) {
            while (enum1.hasMoreElements()) {
                String name = (String) enum1.nextElement();
                Enumeration enum2 = request.getHeaders(name);
                Set values = new HashSet();
                while (enum2.hasMoreElements()) {
                    values.add(enum2.nextElement());
                }
                if (!values.isEmpty()) {
                    envParameters.put(name, values);
                }
            }
        }
        return envParameters;
    }
   
    /**
     * Returns unescaped text. This method replaces "&#124;" with "|".
     *
     * @param text String to be unescaped.
     * @return unescape special character text.
     */
    public static String unescapePipe(String text) {
        return text.replaceAll("&#124;", "|");
    }

    /**
     * Replaces  <code>|</code> with "&#124;".
     *
     * @return String with the special "|" character replaced with "&#124;".
     */
    public static String escapePipe(String text) {
        // escape "|" as it will be used as separator
        int i = text.indexOf("|");
        if (i != -1) {
            StringBuilder sb = new StringBuilder();
            int len = 0;
            if (text != null) {
                len = text.length();
            }
            sb.append(text.substring(0, i));
            for (; i < len; i++) {
                if (text.charAt(i) == '|') {
                    sb.append("&#124;");
                } else {
                    sb.append(text.charAt(i));
                }
            }
            text = sb.toString();
        }
        return text;
    }
    
    /**
     * Returns the data from Realm qualified data. This could be authentication
     * scheme or authentication level or service.
     *
     * @param realmQualifedData Realm qualified data. This could be Realm
     * qualified authentication scheme or authentication level or service.
     * @return String representing data. This could be authentication
     * scheme or authentication level or service.
     */
    public static String getDataFromRealmQualifiedData(
    String realmQualifedData){
        String data = null;
        if (realmQualifedData != null && realmQualifedData.length() != 0) {
            int index = realmQualifedData.indexOf(ISAuthConstants.COLON);
            if (index != -1) {
                data = realmQualifedData.substring(index + 1).trim();
            } else {
                data = realmQualifedData;
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("realmQualifedData : " + realmQualifedData );
            utilDebug.message("DataFromRealmQualifiedData : " + data );
        }
        return data;
    }


    /**
     * Determines whether Zero Page Login (ZPL) should be allowed for this request. This includes checking whether
     * ZPL is enabled for this AuthContext and, if so, whether the HTTP Referer header on the request matches the
     * ZPL whitelist. POST requests are always enabled, but are still subject to the Referer whitelist.
     *
     * @param config the ZPL configuration.
     * @param request the HTTP request.
     * @return true if ZPL is allowed, otherwise false.
     */
    public static boolean isZeroPageLoginAllowed(ZeroPageLoginConfig config, HttpServletRequest request) {
        final boolean isPost = "POST".equalsIgnoreCase(request.getMethod());
        if (!isPost && !config.isEnabled()) {
            return false;
        }

        final String referer = request.getHeader(HTTP_REFERER);
        final Set<String> whitelist = config.getRefererWhitelist();

        if (referer == null) {
            return config.isAllowedWithoutReferer();
        }

        return whitelist.isEmpty() || whitelist.contains(referer);
    }

    /**
     * Decode the value
     * @param strIn
     * @param charset
     * @return an empty string if strIn is null. Use UTF-8 if the charset is empty or null. Return the original
     * string if the decoding failed.
     */
    private static String getCharDecodedField(String strIn, String charset) {

        if (strIn == null) {
            return "";
        }
        if (charset == null || charset.isEmpty()) {
            charset = "UTF-8";
        }
        try {
            // Translate the individual field values in the encoding value.
            // Do not use getBytes() instead convert unicode into bytes by
            // casting. Using getBytes() results in conversion into platform
            // encoding. It appears to work in C locale because default
            // encoding is 8859-1 but fails in other locales like Japanese,
            // Chinese.
            int len = strIn.length();
            byte buf[] = new byte[len];

            int i = 0;
            int offset = 0;
            char[] carr = strIn.toCharArray();
            while (i < len) {
                buf[offset++] = (byte) carr[i++];
            }
            return new String(buf, 0, offset, charset);

        } catch (Exception ex) {
            utilDebug.error("AuthClientUtils.getCharDecodedField():", ex);
            return strIn;
        }
    }

    /**
     * Put the value in the map. The value will be char decoded with the correct encoding.
     * If for any reason, the value is empty, this function won't add the value to the map. So no Null or empty value
     * will added to the map
     * @param data the map where you want to add the value
     * @param name the value key name
     * @param value the value you want to add in the map.
     * @param encoding the encoding charset. If null, UTF-8 will be used
     */
    private static void putDecodedValue(Map<String, String> data, String name, String value, String encoding) {

        if (value == null || value.isEmpty()) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils::putDecodedValue the '" + name + "' value is null or empty'");
            }
            return;
        }

        String decodedValue = getCharDecodedField(value, encoding);
        if (decodedValue.isEmpty()) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils::putDecodedValue decoding with encoding '" + encoding + "' is empty");
            }
            return;
        }

        data.put(name, decodedValue);
    }
}
