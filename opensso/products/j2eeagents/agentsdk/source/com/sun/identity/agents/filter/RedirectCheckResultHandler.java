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
 * $Id: RedirectCheckResultHandler.java,v 1.2 2008/06/25 05:51:48 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.util.IUtilConstants;
import com.sun.identity.agents.util.NameValuePair;

import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.arch.ICrypt;


/**
 * <p>
 * This result handler provides the necessary functionality to process incoming
 * requests that need correction for single-point infinite redirect loops.
 * </p>
 */
public class RedirectCheckResultHandler extends AmFilterResultHandler
        implements IRedirectCheckResultHandler {
    
    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     */
    public RedirectCheckResultHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode)
    throws AgentException {
        super.initialize(context, mode);
        setCryptUtil();
        initRedirectAttemptLimit();
        initRedirectCounterCookieName();
        
        //NOTE: This handler is active even in NONE mode of operation
    }
    
    /**
     * Checks to see if the given result is a potential single-point inifinite
     * looping point which needs to be controlled. If the result is identified
     * as a redirect loop, the result will be overriden to ensure that such
     * a loop can be stopped immediately.
     *
     * @param ctx the filter request context which provides access to the
     * underlying <code>HttpServletRequest</code>,
     * <code>HttpServletResponse</code> and other data that
     * may be needed by this handler for facilitating its processing.
     *
     * @param result the <code>AmFilterResult</code> obtained by the
     * <code>AmFilter</code> by processing the incoming request.
     *
     * @return <code>AmFilterResult</code> if the processing resulted in a
     * particular action to be taken for the incoming request. <b>If no 
     * processing is applicable to the given result instance, the same instance 
     * is returned by this method.</b>
     *
     * @throws AgentException if the processing resulted in an unrecoverable
     * error condition
     * an unexpected error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx,
            AmFilterResult result)
            throws AgentException {
        try {
            if (getRedirectAttemptLimit() > 0) {
                NameValuePair nvp = getRedirectAttemptCounterValue(ctx);
                if (result.getStatus().getIntValue() ==
                        AmFilterResultStatus.INT_STATUS_REDIRECT) {
                    String newURL = result.getRedirectURL();
                    if (nvp != null && nvp.getValue().equals(newURL)) {
                        int lastValue = Integer.parseInt(nvp.getName());
                        if (lastValue >= getRedirectAttemptLimit()) {
                            if (isLogWarningEnabled()) {
                                logWarning("RedirectCheckResultHandler: "
                                        + "redirect attempt limit reached for "
                                        + newURL + ", access will be denied");
                            }
                            // Check for Access denied URL
                            String accessDeniedURL = ctx.getAccessDeniedURL();
                            if (accessDeniedURL != null 
                                    && newURL.equals(accessDeniedURL)) {
                                logError("RedirectCheckResultHandler: Detected "
                                        + " redirects on access denied URL "
                                        + accessDeniedURL + ", using FORBIDDEN "
                                        + "code to block");
                                result = ctx.getBlockAccessResult(true);
                            } else {
                                result = ctx.getBlockAccessResult();
                            }
                        } else {
                            if (isLogWarningEnabled()) {
                                logWarning(
                                        "RedirectCheckResultHandler: " 
                                        + "redirect number "
                                        + lastValue + " for " + newURL
                                        + " resulted in same redirect");
                            }
                            setRedirectAttemptCounterValue(ctx, newURL,
                                    nvp.getName());
                        }
                    } else {
                        // Either previous cookie was null or its a new URL,
                        // need to reset the counter value
                        setRedirectAttemptCounterValue(ctx, newURL);
                    }
                } else {
                    if (nvp != null) {
                        if (isLogMessageEnabled()) {
                            logMessage("RedirectCheckResultHandler: removing "
                                    + "redirect counter cookie");
                        }
                        ctx.expireCookie(getRedirectCounterCookieName());
                    }
                }
            }
        } catch (Exception ex) {
            logError("RedirectCheckResultHandler: Unable to process filter "
                    + "result, denying access",
                    ex);
            result = ctx.getBlockAccessResult();
        }
        
        return result;
    }
    
    /**
     * Returns a boolean value indicating if this result handler is enabled 
     * or not.
     * @return true if the result handler is enabled, false otherwise
     */
    public boolean isActive() {
        return  isModeSSOOnlyActive() && (getRedirectAttemptLimit() > 0);
    }
    
    /**
     * Returns a String that can be used to identify this result handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_REDIRECT_CHECK_RESULT_HANDLER_NAME;
    }
    
    private void setRedirectAttemptCounterValue(AmFilterRequestContext ctx,
            String url)
            throws AgentException {
        setRedirectAttemptCounterValue(ctx, url, "0");
    }
    
    private void setRedirectAttemptCounterValue(AmFilterRequestContext ctx,
            String url, String lastCounter)
            throws AgentException {
        HttpServletResponse response = ctx.getHttpServletResponse();
        try {
            int value = Integer.parseInt(lastCounter);
            
            if (value < 0) {
                throw new AgentException("Invalid last counter value: " +
                        lastCounter);
            }
            
            Cookie cookie = new Cookie(getRedirectCounterCookieName(),
                    getRedirectCounterCookieValue(++value, url));
            cookie.setPath(IUtilConstants.DEFAULT_COOKIE_PATH);
            response.addCookie(cookie);
        } catch (Exception ex) {
            throw new AgentException(
                    "Failed to set redirect counter value", ex);
        }
    }
    
    private String getRedirectCounterCookieValue(int value, String url)
    throws AgentException {
        String result = null;
        try {
            String innerValue = String.valueOf(value) + " "
                    + url + " " + String.valueOf(System.currentTimeMillis());
            result = getCryptUtil().encrypt(innerValue);
        } catch (Exception ex) {
            throw new AgentException("Unable to encrypt redirect counter value",
                    ex);
        }
        return result;
    }
    
    private NameValuePair getRedirectAttemptCounterValue(
            AmFilterRequestContext ctx) throws AgentException {
        NameValuePair result      = null;
        String value = ctx.getRequestCookieValue(
                getRedirectCounterCookieName());
        
        if(value != null && value.trim().length() > 0) {
            try {
                String innerValue = getCryptUtil().decrypt(value);
                
                if((innerValue != null) && (innerValue.trim().length() > 0)) {
                    StringTokenizer stok = new StringTokenizer(innerValue);
                    String counterValue = null;
                    String redirectURL = null;
                    String timeSuffix = null;
                    if (stok.countTokens() == 3) {
                        counterValue = stok.nextToken();
                        redirectURL = stok.nextToken();
                        timeSuffix = stok.nextToken();
                    } else {
                        throw new AgentException(
                                "Invalid Redirect Counter Value: "
                                + innerValue);
                    }
                    if (counterValue == null || redirectURL == null 
                            || timeSuffix == null
                            || counterValue.trim().length() == 0
                            || redirectURL.trim().length() == 0
                            || timeSuffix.trim().length() == 0) {
                        throw new AgentException("Malformed Redirect Counter: "
                                + innerValue);
                    }
                    
                    result = new NameValuePair(counterValue, redirectURL);
                }
            } catch(Exception ex) {
                throw new AgentException("Error reading redirect counter value",
                        ex);
            }
        } else {
            if(isLogMessageEnabled()) {
                logMessage(
                        "RedirectCheckResultHandler: no redirect counter token "
                        + "found in request");
            }
        }
        
        return result;
    }
    
    private void initRedirectCounterCookieName() {
        setRedirectCounterCookieName(getManager().getConfigurationString(
                CONFIG_REDIRECT_COUNTER_COOKIE_NAME,
                DEFAULT_REDIRECT_COUNTER_COOKIE_NAME));
    }
    
    private void setRedirectCounterCookieName(String cookieName) {
        _redirectCounterCookieName = cookieName;
    }
    
    private String getRedirectCounterCookieName() {
        return _redirectCounterCookieName;
    }
    
    private int getRedirectAttemptLimit() {
        return _redirectAttemptLimit;
    }
    
    private void initRedirectAttemptLimit() {
        int limit = getManager().getConfigurationInt(
                CONFIG_REDIRECT_ATTTEMPT_LIMIT,
                DEFAULT_REDIRECT_ATTEMPT_LIMIT);
        if (limit <= 0) {
            if (isLogWarningEnabled()) {
                logWarning(
                    "RedirectCheckResultHandler: Redirect counter disabled: " 
                    + limit);
            }
            limit = 0;
        }
        setRedirectAttemptLimit(limit);
    }
    
    private void setRedirectAttemptLimit(int limit) {
        _redirectAttemptLimit = limit;
    }
    
    protected ICrypt getCryptUtil() {
        return _crypt;
    }
    
    private void setCryptUtil() throws AgentException {
        _crypt = ServiceFactory.getCryptProvider();
    }
    
    
    private int _redirectAttemptLimit;
    private String _redirectCounterCookieName;
    private ICrypt _crypt;
}
