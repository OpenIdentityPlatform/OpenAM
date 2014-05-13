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
 * $Id: CDCClientServlet.java,v 1.6 2009/01/12 18:57:12 madan_ranganath Exp $
 *
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 */

package com.iplanet.services.cdc.client;

import javax.servlet.*;
import javax.servlet.http.*;

import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionException;
import com.sun.identity.shared.encode.CookieUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.HttpURLConnectionManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Enumeration;
import com.sun.identity.shared.Constants;
import java.util.Set;

/**
 * The <code>CDCClientServlet</code> is the heart of the Cross Domain Single Signon mechanism of OpenAM in the DMZ
 * along with the distributed auth.
 * <br/>
 * The following is the algorithm used by the program:
 * <ol>
 *  <li>If request does not contain SSO related cookie or policy has generated some advices then redirect request to
 *  the distributed auth service.</li>
 *  <li>If request contains SSO related cookie and no advices.</li>
 *  <ul>
 *   <li>Tunnel the Request to OpenAM.</li>
 *   <li>Send the received AuthNResponse as Form POST to the original request requested using the goto parameter in
 *   the query string.</li>
 *  </ul>
 * </ol>
 */
public class CDCClientServlet extends HttpServlet {
    private static final ArrayList adviceParams = new ArrayList();
    private static final Set<String> INVALID_SET = new HashSet<String>();
    private static final Set<String> VALID_LOGIN_URIS = new HashSet<String>();
    private static final String LEFT_ANGLE              = "<";
    private static final String RIGHT_ANGLE             = ">";
    private static final String URLENC_RIGHT_ANGLE      = "%3e";
    private static final String URLENC_LEFT_ANGLE       = "%3c";
    private static final String URLENC_JAVASCRIPT       = "javascript%3a";
    private static final String JAVASCRIPT              = "javascript:";
    private static final String DELIM                   = ",";
    private static final String DEBUG_FILE_NAME         = "amCDC";
    private static final char	QUESTION_MARK = '?';
    private static final char	AMPERSAND = '&';
    private static final char	EQUAL_TO = '=';
    private static final char   SEMI_COLON = ';';
    private static final char   SPACE = ' ';
    private static final String GOTO_PARAMETER = "goto";
    private static final String TARGET_PARAMETER = "TARGET";
    private static final String CDCURI	= "/cdcservlet";
    private static final String LOGIN_URI = "loginURI";
    private static String cdcAuthURI;
    private static final String AUTHURI	= "/UI/Login";
    private static final String RESPONSE_HEADER_ALERT =
            "X-DSAME-Assertion-Form";
    private static final String RESPONSE_HEADER_ALERT_VALUE =
            "true";
    private static final String FORBIDDEN_STR_MATCH = "#403x";
    private static final String SERVER_ERROR_STR_MATCH = "#500x";
    static Debug debug = Debug.getInstance(DEBUG_FILE_NAME);

    static {
        initConfig();
    }

    private static SSOTokenManager tokenManager;
    private static String sessionServiceName = "iPlanetAMSessionService";
    private static String authURLCookieName;
    private static String authURLCookieDomain;
    private static String deployDescriptor;
    boolean serverMode = Boolean.valueOf(System.getProperty(
        Constants.SERVER_MODE, SystemProperties.get(Constants.SERVER_MODE, 
        "false"))).booleanValue();
    private static boolean cookieEncoding =
            SystemProperties.getAsBoolean(Constants.AM_COOKIE_ENCODE);
    
    /**
     * @param config the ServletConfig object that contains configutation
     *               information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *               the servlet's normal operation.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (debug.messageEnabled()) {
            debug.message("CDCClientServlet.init:CDCClientServlet "
                +"Initializing...");
        }
        try {
            tokenManager = SSOTokenManager.getInstance();
        } catch (SSOException ssoe) {
            debug.error("CDCClientServlet.init:unable to get SSOTokenManager", 
                ssoe);
        }
        authURLCookieName = SystemProperties.get(
            Constants.AUTH_UNIQUE_COOKIE_NAME, "sunIdentityServerAuthNServer");
        authURLCookieDomain = SystemProperties.get(
            Constants.AUTH_UNIQUE_COOKIE_DOMAIN, "");
        deployDescriptor = SystemProperties.get(
            Constants.AM_DISTAUTH_DEPLOYMENT_DESCRIPTOR, "/distauth");
    }
    
    /**
     * Handles the HTTP GET request.
     *
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    @Override
    public void doGet(HttpServletRequest  request, HttpServletResponse response)
    throws ServletException, IOException {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     *
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    @Override
    public void doPost(HttpServletRequest  request, HttpServletResponse 
        response) throws ServletException, IOException 
    {
        doGetPost(request, response);
    }
    
    /**
     * The method redirects the user to the authentication module if he is not
     * authenticated; else redirects him back to the original referrer.
     *
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    private void doGetPost(HttpServletRequest request, 
            HttpServletResponse response) throws ServletException, IOException {
        if (debug.messageEnabled()) {
            debug.message("CDCClientServlet.doGetPost:Query String received= "
                + request.getQueryString());
        }
        String gotoParameter = request.getParameter(GOTO_PARAMETER);
        String targetParameter = request.getParameter(TARGET_PARAMETER);
        if (targetParameter == null) {
            targetParameter =
                request.getParameter(TARGET_PARAMETER.toLowerCase());
        }
        // if check if goto ot target have invalid strings, to avoid
        // accepting invalid injected javascript.

        if ((gotoParameter != null ) || (targetParameter != null)) {
            if (debug.messageEnabled()) {
                debug.message("CDCClientServlet:doGetPost():validating goto: " + gotoParameter
                        + " and target: " + targetParameter);
            }
            for (String invalidStr : INVALID_SET) {
                if (gotoParameter != null && gotoParameter.toLowerCase().contains(invalidStr)) {
                    showError(response, SERVER_ERROR_STR_MATCH + "GOTO parameter has invalid characters");
                    return;
                }
                if (targetParameter != null  && targetParameter.toLowerCase().contains(invalidStr)) {
                    showError(response, SERVER_ERROR_STR_MATCH + "TARGET parameter has invalid characters");
                    return;
                }
            }
        }

        /* Steps to be done
         * 1. If no SSOToken or policy advice present , forward to 
         *    authentication.
         * 2. If SSOToken is valid tunnel request to the backend AM's 
         *    CDCServlet and Form POST the received response to the agent.
         */
        // Check for a valid SSOToken in the request. If SSOToken is not found 
        // or if the token is invalid, redirect the user for authentication.
        // Also re-direct if there are policy advices in the query string
        SSOToken token = getSSOToken(request, response);
        // collect advices in parsedRequestParams[0] String and rest of params
        // other than original goto url in parsedRequestParams[1] String.
        String[] parsedRequestParams = parseRequestParams(request);

        if ((token == null) || (parsedRequestParams[0] != null)) {
             // Redirect to authentication
             redirectForAuthentication(request, response, parsedRequestParams[0], parsedRequestParams[1]);
        } else {

            // tunnel request to AM
            // send the request to the CDCServlet of AM where the session 
            // was created.
            sendAuthnRequest(request, response, token);
        }
    }

    /**
     * This the main method of this servlet which takes in the request
     * opens a URLConnection to the CDCServlet endpoint in the
     * OpenAM, and tunnels the request content to it.
     * It parses the Response received and if the HTTP_STATUS is "HTTP_OK"
     * or "HTTP_MOVED_TEMP" POSTs the received Liberty Authn Response to the
     * goto URL specified in the original request.
     */
    private void sendAuthnRequest(HttpServletRequest request,
        HttpServletResponse response, SSOToken token) 
        throws ServletException, IOException 
    {
        SessionID sessid = new SessionID(request);
        URL CDCServletURL = null;
        URL sessionServiceURL = null;
        try {
            sessionServiceURL = Session.getSessionServiceURL(sessid);
        } catch (SessionException se) {
            debug.error("CDCClientServlet.sendAuthnRequest: Cannot locate"
                +" OpenAM instance to forward to.", se);
            showError(response,
                "Cannot locate OpenAM instance to forward to");
        }
        if (sessionServiceURL == null) {
            showError(response,
                "Cannot locate OpenAM instance to forward to");
        }
        // replace "sessionservice" by cdcservlet in obtained URL
        // we use naming so that we get the URL of the exact server
        // where the session is located and get the right deployment
        // descriptor.
        String sessionServiceURLString = sessionServiceURL.toString();
        int serviceNameIndex = sessionServiceURLString.lastIndexOf("/", 
            sessionServiceURLString.length() - 2); // avoiding trailing "/" 
                                                   // if any
        StringBuilder buffer = new StringBuilder(150);
        buffer.append(sessionServiceURLString.substring(0,serviceNameIndex))
           .append(CDCURI)
           .append(QUESTION_MARK)
           .append(request.getQueryString()); // add query string to 
                                              // CDCServletURL
        
        CDCServletURL = new URL (buffer.toString());
        
        // save the go to URL of the agent side to ultimately
        // POST to.
        try {
            HttpURLConnection connection = HttpURLConnectionManager.getConnection(CDCServletURL);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type",
                "text/html;charset=UTF-8");
            connection.setDoOutput( true );
            connection.setUseCaches( false );
            // replay cookies
            String strCookies = getCookiesFromRequest(request);
            if (strCookies != null) 
                {  if (debug.messageEnabled()) {
                    debug.message("CDCClientServlet.sendAuthnRequest:Setting "
                        +"cookies = " + strCookies);
                }
                connection.setRequestProperty("Cookie", strCookies);
            }
            // dont wish to follow redirect to agent, since
            // the response needs to go via the CDCClientServlet.
            HttpURLConnection.setFollowRedirects(false);

            // Receiving input from CDCServlet on the AM server instance
            if (debug.messageEnabled()) {
                debug.message("CDCClientServlet.sendAuthnRequest:Getting "
                    +"response back from  "+CDCServletURL);
                debug.message("CDCClientServlet.sendAuthnRequest:Response "
                    +"Code " + connection.getResponseCode());
                debug.message("CDCClientServlet.sendAuthnRequest:Response "
                    +"Message= " + connection.getResponseMessage());
            }

            // Check response code
            if ((connection.getResponseCode() == HttpURLConnection.HTTP_OK) || 
                (connection.getResponseCode() == 
                HttpURLConnection.HTTP_MOVED_TEMP))
            {
                /**
                 * Read the response back from CDCServlet, got a redirect
                 * since this response contains the "LARES" ( Liberty
                 * authn response, which needs to be posted back to the
                 * dest url (agent).
                 */
                StringBuilder inBuf  = new StringBuilder();
                BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"));
                int len;
                char[] buf = new char[1024];
                while((len = in.read(buf,0,buf.length)) != -1) {
                    inBuf.append(buf,0,len);
                }
                String inString = inBuf.toString();
                if (debug.messageEnabled()) {
                    debug.message("CDCClientServlet.sendAuthnRequest:"
                        +"Received response data = " + inString);
                }
                // put the received Liberty Auth Response 
                // in the servlet's response.
                sendAuthnResponse(request, response, inString);
            } else {
                debug.error("CDCClientServlet.sendAuthnRequest: Response "
                    +"code NOT OK/MOVED_TEMP ");
                showError(response, "ERROR: Received HTTP error code "
                    + connection.getResponseCode()+" from "
                    + CDCServletURL);
            }
        } catch (ConnectException ce) {
                // Debug the exception
            if (debug.warningEnabled()) {
                debug.warning("CDCClientServlet.sendAuthnRequest: "
                    +"Connection Exception to " + CDCServletURL, ce);
            }
            showError(response, "Could not connect to CDCServlet at "
                + CDCServletURL+":"+ce.getMessage());
        }
    }


    // Get cookies string from HTTP request object
    private String getCookiesFromRequest(HttpServletRequest request) {
        Cookie cookies[] = CookieUtils.getCookieArrayFromReq(request);
        // above call would return pure sid in iPlanetDirectoryPro cookie
        // independent of container encoding
        StringBuilder cookieStr = null;
        String strCookies = null;
        if (cookies != null) {
            for (int nCookie = 0; nCookie < cookies.length; nCookie++) {
                String cookieName = cookies[nCookie].getName();
                String cookieVal = cookies[nCookie].getValue();
                if (cookieName.equals(CookieUtils.getAmCookieName()) &&
                        cookieEncoding) {
                    cookieVal = URLEncDec.encode(cookieVal);
                }
                if (debug.messageEnabled()) {
                    debug.message("CDCClientServlet.getCookiesFromRequest:"
                        +"Cookie name = " + cookieName);
                    debug.message("CDCClientServlet.getCookiesFromRequest:"
                        +"Cookie val= "+ cookieVal);  
                }
                if (cookieStr == null) {
                    cookieStr = new StringBuilder();
                } else {
                    cookieStr.append(SEMI_COLON).append(SPACE);
                }
                cookieStr.append(cookieName).append(EQUAL_TO).append(cookieVal);
            }
        }
        if (cookieStr != null) {
            strCookies = cookieStr.toString();
        }
        return strCookies;
    }

    /**
     * Gathers the parameters in the request as a HTTP URL string.
     * to form request parameters and policy advice String array.
     * It collects all the parameters from the original request except
     * the original goto url and any advice parameters.
     * Note: All the paramters will be url decoded by default., we should
     * make sure that these values are encoded again
     * 
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @return An String array, index 0 is policy advice, index 1 is rest of the
     *                request parameters
     */
    private String[] parseRequestParams(HttpServletRequest request) {
	StringBuilder adviceList = null;
        StringBuilder parameterString = new StringBuilder(100);
	for (Enumeration e = request.getParameterNames(); e.hasMoreElements();){
             String paramName = (String)e.nextElement();
             if (adviceParams.contains(paramName.toLowerCase())) {
                 if (adviceList == null)  {
                     adviceList = new StringBuilder();
                 } else {
                     adviceList.append(AMPERSAND);
                 }
                 String[] values = request.getParameterValues(paramName);
                 for (int i = 0; values != null && i < values.length; i++) {
                     adviceList.append(paramName).append(EQUAL_TO) 
                         .append(values[i]);
                 }
             } else {
                 if (!paramName.equals(GOTO_PARAMETER)) {
                     String[] values = request.getParameterValues(paramName);
                     for (int i = 0; values != null && i < values.length; i++) {
                         parameterString.append(AMPERSAND).append(paramName)
                         .append(EQUAL_TO).append(URLEncDec.encode(values[i]));
                     }
                 }
             }
         }
         if (debug.messageEnabled()) {
                  debug.message("CDCClientServlet.parseRequestParams:"
                      +"Advice List is = " + adviceList);
                  debug.message("CDCClientServlet.parseRequestParams:"
                      +"Parameter String is = " +  parameterString.toString());
         }

         String policyAdviceList;
         String requestParams;

         if (adviceList == null) { 
            policyAdviceList= null;
         } else {
            policyAdviceList = adviceList.toString();
         }

	 if (parameterString.length() > 0 ) {
             requestParams=  (parameterString.deleteCharAt(0).toString());
	 } else {
             requestParams = parameterString.toString();
         }

         return new String[] { policyAdviceList, requestParams };
    }

    /**
     * Redirects the HTTP request to the Authentication module.
     * It gets the authentication url from <code>SystemProperties</code>.
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception IOException If an input or output exception occurs
     */
    private void redirectForAuthentication(HttpServletRequest  request,
        HttpServletResponse response, String policyAdviceList, String requestParams)
    throws IOException {
        if (debug.messageEnabled()) {
            debug.message("CDCClientServlet.redirectForAuthentication: "
                +"requestURL="+request.getRequestURL());
        }
        StringBuilder redirectURL = new StringBuilder(100);
        StringBuilder gotoURL = new StringBuilder(100);

        // Check if user has authenticated to another OpenAM
        // instance
        String authURL = null;
        Cookie authCookie = 
            CookieUtils.getCookieFromReq(request,authURLCookieName);
        if (authCookie != null) {
            authURL = CookieUtils.getCookieValue(authCookie);
            if (debug.messageEnabled()) {
                debug.message("CDCClientServlet.redirectForAuthentication: "
                    +"got an authenticated URL= " + authURL);
            }
        }
        try {
            if (authURL == null || authURL.length() == 0 ||
                !authURL.toLowerCase().startsWith("http") || 
                 policyAdviceList != null) {
                String finalURL = request.getParameter(GOTO_PARAMETER);
           
                if(finalURL == null || finalURL.equals("")) {
                   finalURL = request.getParameter(TARGET_PARAMETER);
                }

                if(finalURL == null || finalURL.equals("")) {
                   if (debug.messageEnabled()) {
                       debug.message("CDCClientServlet.redirectForAuthentication: "
                           +"goto or target parameter is missing in the request.");
                   }

                   showError(response, SERVER_ERROR_STR_MATCH);
                   return;
                }

                gotoURL.append(deployDescriptor).append(CDCURI)
                    .append(QUESTION_MARK).append(TARGET_PARAMETER)
                    .append(EQUAL_TO).append(URLEncDec.encode(finalURL))
                    .append(AMPERSAND).append(requestParams);
                    
                // Construct the login URL
                String loginURI = request.getParameter(LOGIN_URI);
                String cdcUri;

                if (loginURI != null && !loginURI.isEmpty() && isValidCDCURI(loginURI)) {
                    if (debug.messageEnabled()) {
                        debug.message("CDCClientServlet.redirectForAuthentication:found " + LOGIN_URI + "="
                                + loginURI);
                    }

                    cdcUri = loginURI;
                } else {
                    cdcUri = cdcAuthURI;
                }

                if (debug.messageEnabled()) {
                    debug.message("CDCClientServlet.redirectForAuthentication: Login URI is set to = " + cdcUri);
                }
                           
                if (cdcUri.indexOf(QUESTION_MARK) == -1) {
                    redirectURL.append(cdcUri).append(QUESTION_MARK);
                } else {
                    redirectURL.append(cdcUri).append(AMPERSAND);
                }

                if (policyAdviceList != null) {
                     redirectURL.append(policyAdviceList).append(AMPERSAND);
                }
                redirectURL.append(GOTO_PARAMETER)
                    .append(EQUAL_TO)
                    .append(URLEncDec.encode(gotoURL.toString()));

                if (debug.messageEnabled()) {
                    debug.message("CDCClientServlet.redirectForAuthentication"
                        +":redirectURL before dispatching is="+redirectURL);
                }
                RequestDispatcher dispatcher =
                    request.getRequestDispatcher(redirectURL.toString());
                dispatcher.forward(request, response);
            } else {
                // Redirect the user to the authenticated URL
                redirectURL.append(authURL).append(deployDescriptor)
                    .append(CDCURI).append(QUESTION_MARK)
                    .append(request.getQueryString());
                //Reset the cookie value to null, to avoid continuous loop
                // when a load balancer is used
                if (authCookie != null) {
	            authCookie.setValue("");
	            response.addCookie(authCookie);
                }
                response.sendRedirect(redirectURL.toString());
            }

            if (debug.messageEnabled()) {
                debug.message("CDCClientServlet.redirectForAuthentication:"
                    +"Forwarding for authentication to= " + redirectURL);
            }
        } catch (IOException ex) {
            debug.error("CDCClientServlet.redirectForAuthentication: Failed " 
                +"in forwarding to Authentication service. IOException", ex);
            showError(response, "Could for forward to authentication service:"
                +ex.getMessage());
        } catch (ServletException se) {
           debug.error("CDCClientServlet.redirectForAuthentication : Failed " +
               "in forwarding to Authentication service. ServletException", se);
            showError(response, "Could for forward to authentication service:"
                +se.getMessage());
        } catch (IllegalStateException ie) {
            debug.error("CDCClientServlet.redirectForAuthentication : Failed "
                +"in forwarding to Authentication service. Illegal state", ie);
            showError(response, "Could for forward to authentication service:"
                +ie.getMessage());
        }
    }

    /**
     * Shows Application Error message to the user.
     * @param response an HttpServletResponse object that contains the response
     *			the servlet sends to the client.
     * @param msg Message to be displayed.
     */
    private void showError(HttpServletResponse response, String msg)
    throws IOException {
        ServletOutputStream out = null;
        if (msg == null || msg.equals("") || msg.contains(SERVER_ERROR_STR_MATCH)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            return;
        }

        try {
            out = response.getOutputStream();
            out.println(msg);
            out.flush();
        } catch (IOException e) {
            debug.error("CDCClientServlet.showError::Could not show error "
                +"message to the user " + e);
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {}
        }
    }
    
    /**
     * Returns the SSOToken of the user. If user has not authenticated
     * re-directs the user to login page
     */
    private SSOToken getSSOToken(HttpServletRequest request, 
            HttpServletResponse response) throws IOException {
        SSOToken token = null;
        try {
            /* SSOTokenManager.createSSOToken() throws an SSOException if the
             * token is not valid, so for a invalid token manager.isValidToken()
             * will never get executed for an invalid token.
             */
            if (((token = tokenManager.createSSOToken(request)) == null) ||
                !tokenManager.isValidToken(token)) {
                if (debug.messageEnabled()) {
                    debug.message("CDCClientServlet.getSSOToken:SSOToken is "
                        +"either null or not valid: " + token +
                         "\nRedirecting for authentication");
                }
                token = null;
            }
        } catch (com.iplanet.sso.SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("CDCClientServlet.getSSOToken:SSOException "
                    +"caught= " + e);
            }
            token = null;
        }
        return (token);
    }

    private void sendAuthnResponse(HttpServletRequest request, 
        HttpServletResponse response, String authnResponse ) {
        if (debug.messageEnabled()) {
            debug.message("CDCClientServlet.sendAuthnResponse: Called");
        }
        try{
            if (debug.messageEnabled()) {
                debug.message("CDCClientServlet.sendAuthnResponse: " +
                    "AuthnResponse= " +  authnResponse);
            }
            response.setContentType("text/html");
            response.setHeader("Pragma", "no-cache");
            response.setHeader(RESPONSE_HEADER_ALERT, RESPONSE_HEADER_ALERT_VALUE);

            if (authnResponse.contains(FORBIDDEN_STR_MATCH)) {
                if (debug.messageEnabled()) {
                    debug.message("CDCClientServlet.sendAuthnResponse: " +
                                  "AuthnResponse showing 403 error page");
                }

                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (authnResponse.contains(SERVER_ERROR_STR_MATCH)) {
                if (debug.messageEnabled()) {
                    debug.error("CDCClientServlet.sendAuthnResponse: " +
                                "ERROR: An application error has occured.");
                }

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            PrintWriter out = response.getWriter();
            out.println(authnResponse);
            out.close();
            if (debug.messageEnabled()) {
                debug.message("CDCClientServlet.sendAuthnResponse: "
                + "AuthnResponse sent successfully");
            }
            return;
        } catch(IOException ioe){
            debug.error("CDCClientServlet.sendAuthnResponse:" + ioe.getMessage());
        }
    }

    /**
     * Return <code>true</code> if the passed URI is valid compared to the valid set loaded during initialization.
     *
     * @param cdcUri The URI to test.
     * @return <code>true</code> if the URI is considered valid, <code>false</code> otherwise.
     */
    private boolean isValidCDCURI(String cdcUri) {
        int questionMark = cdcUri.indexOf(QUESTION_MARK);

        // We are only interested in the URI part up to any parameters that may be included.
        if (questionMark != -1) {
            cdcUri = cdcUri.substring(0, questionMark);
        }

        // If there is not an exact match for the passed value then it cannot be considered valid
        boolean result = VALID_LOGIN_URIS.contains(cdcUri);

        if (debug.messageEnabled()) {
            debug.message("CDCClientServlet.isValidCDCURI: checking if " + cdcUri + " is in validLoginURISet: "
                    + VALID_LOGIN_URIS + " result:" + result);
        }

        return result;
    }

    private static void initConfig() {
        adviceParams.add("module");
        adviceParams.add("authlevel");
        adviceParams.add("role");
        adviceParams.add("service");
        adviceParams.add("user");
        adviceParams.add("realm");
        adviceParams.add("org");
        adviceParams.add("resource");
        adviceParams.add("sunamcompositeadvice");
        String invalidStrings = SystemPropertiesManager.get(Constants.INVALID_GOTO_STRINGS);
        if (INVALID_SET.isEmpty()) {
            debug.message("CDCClientServlet.initConfig: creating invalidSet");
            if (invalidStrings == null) {
                debug.message("CDCClientServlet.initConfig: invalidStrings is null");
                INVALID_SET.add(LEFT_ANGLE);
                INVALID_SET.add(RIGHT_ANGLE);
                INVALID_SET.add(URLENC_LEFT_ANGLE);
                INVALID_SET.add(URLENC_RIGHT_ANGLE);
                INVALID_SET.add(JAVASCRIPT);
                INVALID_SET.add(URLENC_JAVASCRIPT);
            } else {
                if (debug.messageEnabled()) {
                    debug.message("CDCClientServlet.initConfig: invalidStrings is: " + invalidStrings);
                }
                StringTokenizer st = new StringTokenizer(invalidStrings, DELIM);
                while (st.hasMoreTokens()) {
                    INVALID_SET.add(st.nextToken());
                }
            }
            debug.message("CDCClientServlet.initConfig: created invalidSet " + INVALID_SET);
        }

        String urlFromProps = SystemProperties.get(Constants.CDCSERVLET_LOGIN_URL);
        cdcAuthURI = (urlFromProps != null) ? urlFromProps : AUTHURI;

        String validLoginURIStrings = SystemPropertiesManager.get(Constants.VALID_LOGIN_URIS);
        debug.message("CDCClientServlet.initConfig: creating validLoginURISet");
        if (validLoginURIStrings == null) {
            debug.message("CDCClientServlet.initConfig: validLoginURIStrings is null, creating default set");
            VALID_LOGIN_URIS.add(cdcAuthURI);
        } else {
            if (debug.messageEnabled()) {
                debug.message("CDCClientServlet.initConfig: validLoginURIStrings is: " + validLoginURIStrings);
            }
            StringTokenizer st = new StringTokenizer(validLoginURIStrings, DELIM);
            while (st.hasMoreTokens()) {
                VALID_LOGIN_URIS.add(st.nextToken());
            }
        }
        debug.message("CDCClientServlet.initConfig: created validLoginURISet " + VALID_LOGIN_URIS);
    }
}
