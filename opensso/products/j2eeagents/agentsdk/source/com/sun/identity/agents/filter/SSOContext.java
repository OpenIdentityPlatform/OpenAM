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
 * $Id: SSOContext.java,v 1.6 2009/02/03 08:33:21 bakka Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.Cookie;

import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.ICookieResetHelper;
import com.sun.identity.agents.common.ISSOTokenValidator;
import com.sun.identity.agents.util.IUtilConstants;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.arch.ICrypt;

/**
 * A <code>SSOContext</code> encapsulates all the configuration and
 * intializations.
 */
public class SSOContext extends AgentBase
        implements IFilterConfigurationConstants, ISSOContext {

    public SSOContext(Manager manager) {
        super(manager);
    }

    public void initialize(AmFilterMode filterMode) throws AgentException {
        setCryptUtil();
        setSSOTokenName(AgentConfiguration.getSSOTokenName());
        setLoginAttemptLimit(getConfigurationInt(
                CONFIG_LOGIN_ATTEMPT_LIMIT, DEFAULT_LOGIN_ATTEMPT_LIMIT));
        setLoginCounterCookieName(getConfigurationString(
                CONFIG_LOGIN_COUNTER_COOKIE_NAME,
                DEFAULT_LOGIN_COUNTER_COOKIE_NAME));
        CommonFactory cf = new CommonFactory(getModule());
        setSSOTokenValidator(cf.newSSOTokenValidator());
        initCookieResetHelper(cf);

        initSSOCacheEnabledFlag(filterMode);
        if (isLogMessageEnabled()) {
            logMessage("SSOContext: initialized.");
        }
    }

    public int getLoginAttemptValue(AmFilterRequestContext ctx) {

        int result = 0;

        String value = ctx.getRequestCookieValue(getLoginCounterCookieName());

        if(value != null && value.trim().length() > 0) {
            try {
                String innerValue = getCryptUtil().decrypt(value);
                if((innerValue != null) && (innerValue.trim().length() > 0)) {
                    int suffixPosition = innerValue.indexOf(':');

                    if(suffixPosition != -1) {
                        result = Integer.parseInt(innerValue.substring(0,
                                suffixPosition));

                        if(isLogMessageEnabled()) {
                            logMessage("SSOTaskHandler: Login attempt number: "
                                    + result);
                        }
                    } else {
                        throw new AgentException(
                                "Invalid counter token value");
                    }
                }
            } catch(Exception ex) {
                if (!value.trim().equals(IUtilConstants.COOKIE_RESET_STRING)) {
                    if(isLogWarningEnabled()) {
                        logWarning(
                                "SSOTaskHandler: Error reading counter token "
                                + "value: " + value);
                    }
                    result = -1;
                } else {
                    if (isLogMessageEnabled()) {
                        logMessage(
                            "SSOTaskHandler: Ignoring reset value of "
                                + "counter cookie");
                    }
                }
            }
        } else {
            if(isLogMessageEnabled()) {
                logMessage("SSOTaskHandler: no counter token found in request");
            }
        }

        return result;
    }

    public Cookie getNextLoginAttemptCookie(int currentValue)
    throws AgentException {

        String counterCookienName = getLoginCounterCookieName();
        String value =
                getCryptUtil().encrypt(String.valueOf(currentValue + 1) + ":"
                + String.valueOf(System.currentTimeMillis()));
        Cookie nextLoginAttemptCookie = new Cookie(counterCookienName, value);
        nextLoginAttemptCookie.setPath(IUtilConstants.DEFAULT_COOKIE_PATH);
        return nextLoginAttemptCookie;
    }

    /**
     * creates and returns a new SSOToken cookie.
     * @param tokenValue - a URL decoded value.
     * @return a SSO Tokem Cookie
     */
    public Cookie[] createSSOTokenCookie(String tokenValue) {
        Cookie[] cookies;
        String[] domains = getConfigurationStrings(CONFIG_CDSSO_DOMAIN);
        boolean isSecure = getConfigurationBoolean(CONFIG_CDSSO_SECURE_ENABLED);
        
        int nbDomains = 0;
        if (domains != null && domains.length > 0) {
            nbDomains = domains.length;
        }
        
        if (tokenValue.indexOf("%") < 0){
            tokenValue = URLEncoder.encode(tokenValue);         
        }        
        
        if (nbDomains == 0) {
            cookies = new Cookie[1];
            cookies[0] = new Cookie(AgentConfiguration.getSSOTokenName(),
                tokenValue);
            cookies[0].setPath(IUtilConstants.DEFAULT_COOKIE_PATH);
            cookies[0].setSecure(isSecure);
        } else {
            cookies = new Cookie[nbDomains];
            for(int i = 0; i < nbDomains; i++) {
                cookies[i] = new Cookie(AgentConfiguration.getSSOTokenName(),
                    tokenValue);
                cookies[i].setPath(IUtilConstants.DEFAULT_COOKIE_PATH);
                cookies[i].setDomain(domains[i]);
                cookies[i].setSecure(isSecure);
            }
        }
        return cookies;
    }
    
    public Cookie getRemoveSSOTokenCookie() {
        Cookie cookie = new Cookie(AgentConfiguration.getSSOTokenName(),
                IUtilConstants.COOKIE_RESET_STRING);
        cookie.setMaxAge(0);
        cookie.setPath(IUtilConstants.DEFAULT_COOKIE_PATH);
        return cookie;
    }

    public boolean isSSOCacheEnabled() {
        return _ssoCacheEnabled;
    }

    public int getLoginAttemptLimit() {
        return _loginAttemptLimit;
    }

    private void initSSOCacheEnabledFlag(AmFilterMode filterMode) {
        boolean cacheEnabled = false;
        if (filterMode.equals(AmFilterMode.MODE_J2EE_POLICY)
        || filterMode.equals(AmFilterMode.MODE_ALL)) {
            boolean stat = getManager().getConfigurationBoolean(
                    CONFIG_AM_SSO_CACHE_ENABLE, DEFAULT_AM_SSO_CACHE_ENABLE);
            cacheEnabled = stat;
        }
        setSSOCacheEnabledFlag(cacheEnabled);
    }

    private void setSSOCacheEnabledFlag(boolean flag) {
        _ssoCacheEnabled = flag;
        if (isLogMessageEnabled()) {
            logMessage(
                    "SSOContext: active cache of sso is: " + _ssoCacheEnabled);
        }
    }

    private void initCookieResetHelper(CommonFactory cf) throws AgentException {
        CookieResetInitializer cookieResetInitializer =
                new CookieResetInitializer(getManager());
        setCookieResetHelper(cf.newCookieResetHelper(cookieResetInitializer));
    }

    private void setCookieResetHelper(ICookieResetHelper cookieHelper){
        _cookieResetHelper = cookieHelper;
    }

    public ICookieResetHelper getCookieResetHelper(){
        return _cookieResetHelper;
    }

    private void setSSOTokenValidator(ISSOTokenValidator validator) {
        _ssoTokenValidator = validator;
        if (isLogMessageEnabled()) {
            logMessage("SSOContext: sso validator set to: "
                    + _ssoTokenValidator);
        }
    }

    public ISSOTokenValidator getSSOTokenValidator() {
        return _ssoTokenValidator;
    }

    public String getLoginCounterCookieName() {
        return _loginCounterCookieName;
    }

    private void setLoginCounterCookieName(String cookieName) {
        _loginCounterCookieName = cookieName;
    }

    private void setLoginAttemptLimit(int limit) {
        if (limit > 0) {
            _loginAttemptLimit = limit;
        }
        if (isLogMessageEnabled()) {
            logMessage("SSOContext: Login attempt limit is set to: "
                    + _loginAttemptLimit);
        }
    }

    private void setSSOTokenName(String name) {
        _ssoTokenName = name;
        if (isLogMessageEnabled()) {
            logMessage("SSOContext: SSO Token name set to: " + _ssoTokenName);
        }
    }

    private String getSSOTokenName() {
        return _ssoTokenName;
    }

    protected ICrypt getCryptUtil() {
        return _crypt;
    }

    private void setCryptUtil() throws AgentException {
        _crypt = ServiceFactory.getCryptProvider();
    }


    private int _loginAttemptLimit;
    private String _loginCounterCookieName;
    private ISSOTokenValidator _ssoTokenValidator;
    private ICookieResetHelper _cookieResetHelper;
    private boolean _ssoCacheEnabled;
    private String _ssoTokenName;
    private ICrypt _crypt;

}
