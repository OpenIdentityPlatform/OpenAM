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
 * $Id: SSOTaskHandler.java,v 1.4 2008/06/25 05:51:48 qcheng Exp $
 *
 */
 /*
 * Portions Copyrighted [2010-2011] [ForgeRock AS]
 */

package com.sun.identity.agents.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.common.ISSOTokenValidator;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.common.ICookieResetHelper;
import org.forgerock.openam.agents.filter.PDPInitHelper;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * requests for Single Sign-On.
 * </p>
 */
public class SSOTaskHandler extends AmFilterTaskHandler 
implements ISSOTaskHandler {

    public SSOTaskHandler(Manager manager) {
        super(manager);
    }
    
    /**
     * Checks to see if the incoming request has valid SSO credentials or not
     * and suggests any action needed to handle such requests appropriately.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle notifications.
     * @throws AgentException in case the processing of this request results
     * in an unexpected error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        ISSOContext ssoContext = getSSOContext();
        ISSOTokenValidator tokenValidator = ssoContext.getSSOTokenValidator();
        SSOValidationResult ssoValidationResult = 
            tokenValidator.validate(ctx.getHttpServletRequest());

        if (!ssoValidationResult.isValid()) {
            //Check if it is a logout request
            try {
                String applicationLogoutHandlerImplClass =
                          AgentConfiguration.getServiceResolver().
                          getApplicationLogoutHandlerImpl();
                ApplicationLogoutHandler handler = (ApplicationLogoutHandler) 
                                ServiceFactory.getServiceInstance(getManager(),
                                applicationLogoutHandlerImplClass);
                handler.initialize(ssoContext, ctx.getFilterMode());
                result = handler.process(ctx);
            } catch (Exception ex) {
                logError("SSOTaskHandler: Error while " + 
                         " delegating to ApplicationLogoutHandler.", ex);
                result = null;
            }
            
            if (result == null) {
                //implementation of CR openam-307
                result = PDPInitHelper.initializePDP(this, ctx, ssoContext);
                //end of implementation of CR openam-307
                if (result == null) {
                    if(isLogMessageEnabled()) {
                        logMessage("SSOTaskHandler: SSO Validation failed for "
                                   + tokenValidator.getSSOTokenValue(
                                           ctx.getHttpServletRequest()));
                    }
                    doCookiesReset(ctx);
                    result = doSSOLogin(ctx);
                }
            }
        } else {
            if (ssoContext.isSSOCacheEnabled()) {
                cacheSSOToken(ssoValidationResult);
            }
            if(ssoContext.getLoginAttemptLimit() > 0) {
                int loginAttempt = ssoContext.getLoginAttemptValue(ctx);
                if (loginAttempt >= 0) {
                    ctx.expireCookie(ssoContext.getLoginCounterCookieName());
                }
            }
            String userDN = ssoValidationResult.getUserPrincipal();
            if (isLogMessageEnabled()) {
                logMessage("SSOTaskHandler: SSO Validation successful for "
                           + userDN);
            }

            // Invalidate the HTTP session if this session binding is enabled
            // and the user is different between requests.
            if (userDN != null && isSessionBindingEnabled()) {
                // Don't create a session if one doesn't already exist.
                HttpServletRequest request = ctx.getHttpServletRequest();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    // If the users are different between requests then
                    // invalidate the current session before storing the new
                    // userDN in a new session
                    String currentUserDN = (String)session.getAttribute(HTTPSESSION_BINDING_ATTRIBUTE);
                    // If null then we have not recorded the user in the session yet
                    if (currentUserDN == null) {
                        // Record the user for next request
                        session.setAttribute(HTTPSESSION_BINDING_ATTRIBUTE, userDN);
                        if (isLogMessageEnabled()) {
                            logMessage("SSOTaskHandler: "
                                    + "recording the user "
                                    + userDN + " in the HTTP Session");
                        }
                    } else if (!userDN.equals(currentUserDN)) {
                        if (isLogMessageEnabled()) {
                            logMessage("SSOTaskHandler: "
                                    + "invalidating HTTP Session because the "
                                    + "user has changed, new "
                                    + userDN + " old " + currentUserDN);
                        }
                        session.invalidate();
                        // Force the creation of a new session
                        session = request.getSession(true);
                        // Record the user for next request
                        session.setAttribute(HTTPSESSION_BINDING_ATTRIBUTE, userDN);
                    }
                }
            }

            ctx.setSSOValidationResult(ssoValidationResult);
        }

        return result;
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_SSO_TASK_HANDLER_NAME;
    }

    public boolean isActive() {
        return isModeSSOOnlyActive();        
    }
    
    private void doCookiesReset(AmFilterRequestContext ctx)
    {
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        ICookieResetHelper cookieResetHelper = getSSOContext().getCookieResetHelper();
        if (cookieResetHelper != null && cookieResetHelper.isActive()) {
            cookieResetHelper.doCookiesReset(request, response);
        }
    }

    private void cacheSSOToken(SSOValidationResult ssoValidationResult) {
        if (isLogMessageEnabled()) {
            logMessage("SSOTaskHandler: caching SSO Token for user "
                       + ssoValidationResult.getUserPrincipal());
        }
        try {
            IAmSSOCache amSSOCache = AmFilterManager.getAmSSOCache();
            amSSOCache.addSSOCacheEntry(ssoValidationResult);
        } catch (Exception ex) {
            logError("SSOTaskHandler: Exception caught while trying to cache "
                     + "sso for " + ssoValidationResult.getUserPrincipal()
                     + ", ssoToken: " + ssoValidationResult.getSSOTokenString(),
                     ex);
        }
    }

    protected AmFilterResult doSSOLogin(AmFilterRequestContext ctx)
        throws AgentException
    {

        AmFilterResult result = null;
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        
        /**
        * For new feature to allow session data to not be destroyed when a user 
        * authenticates to AM server and new session is created. So added a new check
        * to test if session binding is enabled before destroying.For RFE issue #763
        */
        if (isSessionBindingEnabled()) {
            //First destroy the local session if it exists
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
        if(getSSOContext().getLoginAttemptLimit() > 0) {
            int loginAttempt = getSSOContext().getLoginAttemptValue(ctx);
            if(loginAttempt >= 0) {
                if(loginAttempt < getSSOContext().getLoginAttemptLimit()) {

                    if(isLogWarningEnabled()) {
                        logWarning("SSOTaskHandler: Login attempt number "
                                   + loginAttempt
                                   + " failed for request URI: "
                                   + request.getRequestURI());
                    }

                    response.addCookie(
                        getSSOContext().getNextLoginAttemptCookie(
                        loginAttempt));
                    result = ctx.getAuthRedirectResult();
                } else {

                    if(isLogWarningEnabled()) {
                        logWarning(
                            "SSOTaskHandler: number of login attempts have "
                            + "exceeded the set limit. Access blocked for "
                            + "request URI: " + request.getRequestURI());
                    }

                    result = ctx.getBlockAccessResult();
                }
            } else {

                if(isLogWarningEnabled()) {
                    logWarning(
                        "SSOTaskHandler: Invalid value found for counter "
                        + "cookie. Denying access to request URI: "
                        + request.getRequestURI());
                }

                result = ctx.getBlockAccessResult();
            }
        } else {
            result = ctx.getAuthRedirectResult();
        }

        return result;
    }
}
