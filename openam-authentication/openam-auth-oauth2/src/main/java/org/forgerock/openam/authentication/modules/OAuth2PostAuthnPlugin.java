/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.authentication.modules.oauth2;

import com.iplanet.sso.SSOToken;
import java.util.Map;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;

/**
 * The <code>OAuth2PostAuthnPlugin</code> implements
 * AMPostAuthProcessInterface interface for authentication
 * post processing. This class can only be used for the OAuth2 authentication
 * module.
 * 
 * The post processing class can be assigned per ORGANIZATION or SERVICE
 */
public class OAuth2PostAuthnPlugin implements AMPostAuthProcessInterface {

    private static String FB_API_KEY = "api_key";
    private static String FB_SESSION_KEY  ="session_key";
    private static String FB_NEXT = "next";
    
    /** Post processing on successful authentication.
     * @param requestParamsMap - map contains HttpServletRequest parameters
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @param ssoToken  authenticated user's ssoToken
     * @exception Authentication Exception when there is an error
     */
    public void onLoginSuccess(Map requestParamsMap,
            HttpServletRequest request,
            HttpServletResponse response,
            SSOToken ssoToken)
            throws AuthenticationException {

        OAuthUtil.debugMessage("OAuth2PostAuthnPlugin:onLoginSuccess called");

    }

    /** Post processing on failed authentication.
     * @param requestParamsMap - map contains HttpServletRequest parameters
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @exception AuthenticationException when there is an error
     */
    public void onLoginFailure(Map requestParamsMap,
            HttpServletRequest request,
            HttpServletResponse response)
            throws AuthenticationException {
        
        OAuthUtil.debugMessage("OAuth2PostAuthnPlugin:onLoginFailure called");

    }

    /** Post processing on Logout.
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @param ssoToken - user's session
     */
    public void onLogout(HttpServletRequest request,
            HttpServletResponse response,
            SSOToken ssoToken)
            throws AuthenticationException {
        
        OAuthUtil.debugMessage("OAuth2PostAuthnPlugin:onLogout called " + request.getRequestURL());
        String gotoParam = request.getParameter(PARAM_GOTO);
        String serviceURI = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR); 

        try {
            String loginURL = OAuthUtil.findCookie(request, COOKIE_PROXY_URL);
            String accessToken = ssoToken.getProperty(SESSION_OAUTH_TOKEN);

            OAuthUtil.debugMessage("OAuth2PostAuthnPlugin: OAUTH2 Token is: " + accessToken);
            String logoutBehaviour = ssoToken.getProperty(SESSION_LOGOUT_BEHAVIOUR);
            if (logoutBehaviour.equalsIgnoreCase("donotlogout")) {
                return;
            }
            
            if (accessToken != null && !accessToken.isEmpty()) {
                OAuthUtil.debugMessage("OAuth2PostAuthnPlugin: OAuth2 logout");

                String logoutURL =
                        OAuthUtil.findCookie(request, COOKIE_LOGOUT_URL);

                if (logoutURL.toLowerCase().contains("facebook")) {
                    OAuthUtil.debugMessage("OAuth2PostAuthnPlugin: facebook");
                    String origUrl = URLEncoder.encode(loginURL, "UTF-8");
                    String query = "";
                    if (accessToken.contains("\\|")) { 
                        // Non encrypted token
                        String[] tokenParts = accessToken.split("\\|");
                        String api_key = tokenParts[0];
                        String session_key = tokenParts[1];
                        query = FB_API_KEY +"=" + api_key + "&" + FB_SESSION_KEY + 
                                "=" + session_key + "&" + FB_NEXT + "=" + origUrl;
                    } else {      
                        // Encrypted token
                        query = FB_NEXT + "=" + origUrl + "&" + 
                                PARAM_ACCESS_TOKEN +"=" + accessToken;
                    }
                    logoutURL += "?" + query;
                }

                logoutURL = serviceURI + "/oauth2c/OAuthLogout.jsp?" + PARAM_LOGOUT_URL +
                        "=" + URLEncoder.encode(logoutURL, "UTF-8");;
                
                if (logoutBehaviour.equalsIgnoreCase("logout")) {
                    logoutURL += "&" + PARAM_LOGGEDOUT + "=logmeout";
                }
                
                if (gotoParam != null && !gotoParam.isEmpty()) {
                    logoutURL = logoutURL + "&" + PARAM_GOTO + "=" + 
                            URLEncoder.encode(gotoParam, "UTF-8");
                } 
                
                OAuthUtil.debugMessage("OAuth2PostAuthnPlugin: redirecting to: "
                        + logoutURL);

                request.setAttribute(AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL,
                        logoutURL);
            }
        } catch (Exception ex) {
            OAuthUtil.debugError("OAuth2PostAuthnPlugin: onLogout exception "
                    + "while setting the logout property :", ex);
        }

    }
}
