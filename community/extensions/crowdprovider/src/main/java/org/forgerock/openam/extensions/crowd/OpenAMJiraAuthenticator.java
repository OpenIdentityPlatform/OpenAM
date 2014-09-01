/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
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
 * 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 */
package org.forgerock.openam.extensions.crowd;

import com.atlassian.jira.security.login.JiraSeraphAuthenticator;
import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.seraph.config.SecurityConfigFactory;
import com.atlassian.seraph.util.RedirectUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author steve
 */
public class OpenAMJiraAuthenticator extends JiraSeraphAuthenticator {
    private static final Debug debug = Debug.getInstance("OpenAMJiraAuthenticator");

    @Override
    public Principal getUserFromBasicAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Principal p = super.getUserFromBasicAuthentication(request, response);
        debug.error("ba: " + p);
        return p;
    }
    
    @Override
    public Principal getUserFromCookie(HttpServletRequest request, HttpServletResponse response) {
        Principal p = super.getUserFromCookie(request, response);
        debug.error("fc: " + p);
        
        return p;
    }
    
    @Override
    public Principal getUserFromSession(HttpServletRequest request) {
        Principal p = super.getUserFromSession(request);
        debug.error("fs: " + p);
        
        return p;
    }
    
    @Override
    public boolean authenticate(Principal user, String password)
    throws AuthenticatorException { 
        try {
            openAMAuthenticate(user.getName(), password);
            return true;
        } catch (Exception ex) {
            debug.error("Unable to authenticate user", ex);
            return false;
        }
    }
    
    @Override
    public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
        Principal user = null;

        try {
            request.getSession(true);
            
            if (debug.messageEnabled()) {
                debug.message("Trying seamless Single Sign-on...");
            }
            
            String username = obtainUsername(request);
            
            if (debug.messageEnabled()) {
                debug.message("Got username = " + username);
            }
            
            if (username != null) {
                if (request.getSession() != null && request.getSession().getAttribute(LOGGED_IN_KEY) != null) {
                    if (debug.messageEnabled()) {
                        debug.message("Session found; user already logged in");
                    }
                    
                    user = (Principal) request.getSession().getAttribute(LOGGED_IN_KEY);
                } else {
                    user = getUser(username);
                    
                    if (debug.messageEnabled()) {
                        debug.message("Logged in via SSO, with User " + user);
                    }
                    
                    request.getSession().setAttribute(LOGGED_IN_KEY, user);
                    request.getSession().setAttribute(LOGGED_OUT_KEY, null);
                }
            } else {
                String redirectUrl = RedirectUtils.getLoginUrl(request);
                
                if (debug.messageEnabled()) {
                    debug.message("Username is null; redirecting to " + redirectUrl);
                }
                
                return null;
            }
        } catch (Exception ex) {
            debug.error("Exception when getting user", ex);
        }
        
        return user;

    }
    
    private void openAMAuthenticate(String userName, String password) {
        debug.error("found user=" + userName + " and password=" + password);
    }

    private SSOToken getToken(HttpServletRequest request) {
        SSOToken token = null;
        
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            token = manager.createSSOToken(request);
        } catch (SSOException ssoe) {
            debug.warning("Error creating SSOToken", ssoe);
        } catch (Exception ex) {
            debug.error("Error creating SSOToken", ex);
        }
        
        return token;
    }

    private boolean isTokenValid(SSOToken token) {
        boolean result = false;
        
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            result = manager.isValidToken(token);
        } catch (Exception ex) {
            debug.error("Error validating SSOToken", ex);
        }
        
        return result;
    }
    
    private String obtainUsername(HttpServletRequest request) {
        String result = null;
        SSOToken token = getToken(request);
        
        if (token != null && isTokenValid(token)) {
            try {
                result = token.getProperty("UserId");
            } catch (SSOException ssoe) {
                debug.error("Error getting UserId from SSOToken", ssoe);
            }
        }
        
        return result;
    }

    @Override
    public boolean logout(HttpServletRequest request, HttpServletResponse response)
    throws AuthenticatorException {
        boolean result = false;
        try {
            result = doLogout(request, response);
        } catch (Exception ex) {
            debug.error("Exception during logout" , ex);
        }
        
        return result;
    }

    private boolean doLogout(HttpServletRequest request, HttpServletResponse response)
    throws AuthenticatorException, IOException {
        if (super.logout(request, response)) {
            logoutOfOpenAM(response);
            return true;
        }
        
        return false;
    }

    private void logoutOfOpenAM(HttpServletResponse response)
    throws IOException {
        String logoutURL = SecurityConfigFactory.getInstance().getLogoutURL();
        
        if (logoutURL != null) {
            response.sendRedirect(logoutURL);
        }
    }
}
