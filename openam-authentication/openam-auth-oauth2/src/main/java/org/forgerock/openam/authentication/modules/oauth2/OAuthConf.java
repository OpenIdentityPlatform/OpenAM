/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2013 ForgeRock Inc. All rights reserved.
 * Copyright © 2011 Cybernetica AS.
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

/* 
 * Portions Copyrighted 2013 ForgeRock Inc
 */

package org.forgerock.openam.authentication.modules.oauth2;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;


/* 
 * The purpose of OAuthConf is to encapsulate module's configuration
 * and based on this configuration provide a common interface for getting
 * essential URLs, like: 
 * - authentication service URL;
 * - token service URL;
 * - profile service URL.
 */
public class OAuthConf {

    static final String CLIENT = "genericHTML";
   // private static Debug debug = Debug.getInstance("amAuth");
    private String clientId = null;
    private String clientSecret = null;
    private String scope = null;
    private String authServiceUrl = null;
    private String tokenServiceUrl = null;
    private String profileServiceUrl = null;
    private String profileServiceParam = null;
    private String ssoProxyUrl = null;
    private String accountMapper = null;
    private String attributeMapper = null;
    private String createAccountFlag = null;
    private String promptPasswordFlag = null;
    private String useAnonymousUserFlag = null;
    private String anonymousUser = null;
    private Set<String> accountMapperConfig = null;
    private Set<String> attributeMapperConfig = null;
    private String saveAttributesToSessionFlag = null;
    private String mailAttribute = null;
    private String logoutServiceUrl = null;
    private String logoutBehaviour = null;
    private String gatewayEmailImplClass = null;
    private String smtpHostName = null;
    private String smtpPort = null;
    private String smtpUserName = null;
    private String smtpUserPassword = null;
    private String smtpSSLEnabled = "false";
    private String emailFrom = null;
    private String authLevel = "0";

    OAuthConf() {
    }

    OAuthConf(Map config) {
        clientId = CollectionHelper.getMapAttr(config, KEY_CLIENT_ID);
        clientSecret = CollectionHelper.getMapAttr(config, KEY_CLIENT_SECRET);
        scope = CollectionHelper.getMapAttr(config, KEY_SCOPE);
        authServiceUrl = CollectionHelper.getMapAttr(config, KEY_AUTH_SERVICE);
        tokenServiceUrl = CollectionHelper.getMapAttr(config, KEY_TOKEN_SERVICE);
        profileServiceUrl = CollectionHelper.getMapAttr(config, KEY_PROFILE_SERVICE);
        profileServiceParam = CollectionHelper.getMapAttr(config, KEY_PROFILE_SERVICE_PARAM, "access_token");
        // ssoLoginUrl = CollectionHelper.getMapAttr(config, KEY_SSO_LOGIN_URL);
        ssoProxyUrl = CollectionHelper.getMapAttr(config, KEY_SSO_PROXY_URL);
        accountMapper = CollectionHelper.getMapAttr(config, KEY_ACCOUNT_MAPPER);
        accountMapperConfig = (Set) config.get(KEY_ACCOUNT_MAPPER_CONFIG);
        attributeMapper = CollectionHelper.getMapAttr(config, KEY_ATTRIBUTE_MAPPER);
        attributeMapperConfig = (Set) config.get(KEY_ATTRIBUTE_MAPPER_CONFIG);
        saveAttributesToSessionFlag = CollectionHelper.getMapAttr(config,
                KEY_SAVE_ATTRIBUTES_TO_SESSION);
        mailAttribute = CollectionHelper.getMapAttr(config, KEY_MAIL_ATTRIBUTE);
        createAccountFlag = CollectionHelper.getMapAttr(config, KEY_CREATE_ACCOUNT);
        promptPasswordFlag = CollectionHelper.getMapAttr(config, KEY_PROMPT_PASSWORD);
        useAnonymousUserFlag = CollectionHelper.getMapAttr(config,
                KEY_MAP_TO_ANONYMOUS_USER_FLAG);
        anonymousUser = CollectionHelper.getMapAttr(config, KEY_ANONYMOUS_USER);
        logoutServiceUrl = CollectionHelper.getMapAttr(config, KEY_LOGOUT_SERVICE_URL);
        logoutBehaviour = CollectionHelper.getMapAttr(config, KEY_LOGOUT_BEHAVIOUR);
        // Email parameters
        gatewayEmailImplClass = CollectionHelper.getMapAttr(config, KEY_EMAIL_GWY_IMPL);
        smtpHostName = CollectionHelper.getMapAttr(config, KEY_SMTP_HOSTNAME);
        smtpPort = CollectionHelper.getMapAttr(config, KEY_SMTP_PORT);
        smtpUserName = CollectionHelper.getMapAttr(config, KEY_SMTP_USERNAME);
        smtpUserPassword = CollectionHelper.getMapAttr(config, KEY_SMTP_PASSWORD);
        smtpSSLEnabled = CollectionHelper.getMapAttr(config, KEY_SMTP_SSL_ENABLED);
        emailFrom = CollectionHelper.getMapAttr(config, KEY_EMAIL_FROM);
        authLevel = CollectionHelper.getMapAttr(config, KEY_AUTH_LEVEL);
    }

    public int getAuthnLevel() {
        int authLevelInt = 0;
        if (authLevel != null) {
            try {
                authLevelInt = Integer.parseInt(authLevel);
            } catch (Exception e) {
                OAuthUtil.debugError("Unable to find a valid auth level " + authLevel
                        + ", defaulting to 0", e);
            }
        }
        return authLevelInt;
    }

    public String getGatewayImplClass()
            throws AuthLoginException {

        return gatewayEmailImplClass;
    }

    public Map<String, String> getSMTPConfig() {
        Map<String, String> config = new HashMap<String, String>();
        config.put(KEY_EMAIL_GWY_IMPL, gatewayEmailImplClass);
        config.put(KEY_SMTP_HOSTNAME, smtpHostName);
        config.put(KEY_SMTP_PORT, smtpPort);
        config.put(KEY_SMTP_USERNAME, smtpUserName);
        config.put(KEY_SMTP_PASSWORD, smtpUserPassword);
        config.put(KEY_SMTP_SSL_ENABLED, smtpSSLEnabled);
        return config;

    }

    public String getLogoutServiceUrl() {

        return logoutServiceUrl;
    }

    public String getLogoutBhaviour() {

        return logoutBehaviour;
    }

    public String getEmailFrom() {

        return emailFrom;
    }
    
    public String getAccountMapper() {

        return accountMapper;
    }

    public String getAttributeMapper() {

        return attributeMapper;
    }

    public Set<String> getAccountMapperConfig() {

        return accountMapperConfig;
    }

    public Set<String> getAttributeMapperConfig() {

        return attributeMapperConfig;
    }

    public boolean getSaveAttributesToSessionFlag() {

        return saveAttributesToSessionFlag.equalsIgnoreCase("true");
    }

    public String getMailAttribute() {

        return mailAttribute;
    }

    public boolean getCreateAccountFlag() {

        return createAccountFlag.equalsIgnoreCase("true");
    }

    public boolean getPromptPasswordFlag() {

        return promptPasswordFlag.equalsIgnoreCase("true");
    }

    public boolean getUseAnonymousUserFlag() {

        return useAnonymousUserFlag.equalsIgnoreCase("true");
    }

    public String getAnonymousUser() {

        return anonymousUser;
    }

    public String getProxyURL() {

        return ssoProxyUrl;
    }

    public String getAuthServiceUrl(String originalUrl) throws AuthLoginException {

        if (authServiceUrl.indexOf("?") == -1) {
            authServiceUrl = authServiceUrl + "?"
                    + PARAM_CLIENT_ID + "=" + clientId;
        } else {
            authServiceUrl = authServiceUrl + "&"
                    + PARAM_CLIENT_ID + "=" + clientId;
        }
        try {
            return authServiceUrl
                    + param(PARAM_SCOPE, OAuthUtil.oAuthEncode(scope))
                    + param(PARAM_REDIRECT_URI, OAuthUtil.oAuthEncode(originalUrl))
                    + param("response_type", "code");
        } catch (UnsupportedEncodingException ex) {
            OAuthUtil.debugError("OAuthConf.getAuthServiceUrl: problems while encoding "
                    + "the scope", ex);
            throw new AuthLoginException("Problem to build the Auth Service URL", ex);
        }
    }

    String getTokenServiceUrl(String code, String authServiceURL) 
            throws AuthLoginException {

        if (code == null) {
            OAuthUtil.debugError("process: code == null");
            throw new AuthLoginException(BUNDLE_NAME,
                    "authCode == null", null);
        }
        OAuthUtil.debugMessage("authentication code: " + code);
        if (tokenServiceUrl.indexOf("?") == -1) {
            tokenServiceUrl = tokenServiceUrl + "?"
                    + PARAM_CLIENT_ID + "=" + clientId;
        } else {
            tokenServiceUrl = tokenServiceUrl + "&"
                    + PARAM_CLIENT_ID + "=" + clientId;
        }

        try {
            return tokenServiceUrl
                    + param(PARAM_REDIRECT_URI, OAuthUtil.oAuthEncode(authServiceURL))
                    + param(PARAM_SCOPE, OAuthUtil.oAuthEncode(scope))
                    + param(PARAM_CLIENT_SECRET, clientSecret)
                    + param(PARAM_CODE, OAuthUtil.oAuthEncode(code))
                    + param("grant_type", "authorization_code");
        } catch (UnsupportedEncodingException ex) {
            OAuthUtil.debugError("OAuthConf.getTokenServiceUrl: problems while encoding "
                    + "and building the Token Service URL", ex);
            throw new AuthLoginException("Problem to build the Token Service URL", ex);
        }
    }

    String getProfileServiceUrl(String token) {

        if (profileServiceUrl.indexOf("?") == -1) {
            return profileServiceUrl + "?" + profileServiceParam
                    + "=" + token;
        } else {
            return profileServiceUrl + "&" + profileServiceParam
                    + "=" + token;
        }

    }

    private String param(String key, String value) {
        return "&" + key + "=" + value;
    }
}
