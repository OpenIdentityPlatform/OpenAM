/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2016 ForgeRock AS.
 * Copyright 2011 Cybernetica AS.
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

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import org.apache.commons.collections.CollectionUtils;
import org.forgerock.openam.authentication.modules.oauth2.service.DefaultServiceUrlProvider;
import org.forgerock.openam.authentication.modules.oauth2.service.ESIAServiceUrlProvider;
import org.forgerock.openam.authentication.modules.oauth2.service.ServiceUrlProvider;
import org.forgerock.openam.utils.MappingUtils;
import org.forgerock.openam.utils.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_ACCOUNT_MAPPER;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_ACCOUNT_MAPPER_CONFIG;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_ACCOUNT_PROVIDER;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_ANONYMOUS_USER;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_ATTRIBUTE_MAPPER;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_ATTRIBUTE_MAPPER_CONFIG;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_AUTH_LEVEL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_AUTH_SERVICE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_CLIENT_ID;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_CLIENT_SECRET;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_CREATE_ACCOUNT;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_CUSTOM_PROPERTIES;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_EMAIL_FROM;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_EMAIL_GWY_IMPL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_LOGOUT_BEHAVIOUR;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_LOGOUT_SERVICE_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_MAIL_ATTRIBUTE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_MAP_TO_ANONYMOUS_USER_FLAG;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_PROFILE_SERVICE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_PROFILE_SERVICE_PARAM;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_PROMPT_PASSWORD;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SAVE_ATTRIBUTES_TO_SESSION;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SCOPE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_HOSTNAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_PASSWORD;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_PORT;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_SSL_ENABLED;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_USERNAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SSO_PROXY_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_TOKEN_SERVICE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.OIDC_SCOPE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SCOPE_SEPARATOR;


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
    static final String ESIA_PREFIX = "esia";

    static final String ESIA_KEY_PATH = "[esia-key-path]";
    static final String ESIA_CERT_PATH = "[esia-cert-path]";
    
    private boolean openIDConnect;
    private String accountProvider;
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
    private Set<String> attributeMappers = null;
    private String createAccountFlag = null;
    private String promptPasswordFlag = null;
    private String useAnonymousUserFlag = null;
    private String anonymousUser = null;
    private Map<String, String> accountMapperConfig = null;
    private Map<String, String> attributeMapperConfig = null;
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
	private Map<String, String> customProperties = null;

    private ServiceUrlProvider serviceUrlProvider;

    OAuthConf() {
    }

    @SuppressWarnings("unchecked")
	OAuthConf(Map config) {
        clientId = CollectionHelper.getMapAttr(config, KEY_CLIENT_ID);
        clientSecret = CollectionHelper.getMapAttr(config, KEY_CLIENT_SECRET);
        scope = CollectionHelper.getMapAttr(config, KEY_SCOPE);
        if (StringUtils.isNotEmpty(scope)){
            openIDConnect = Arrays.asList(scope.split(SCOPE_SEPARATOR)).contains(OIDC_SCOPE);
        } else {
            openIDConnect = false;
        }
        authServiceUrl = CollectionHelper.getMapAttr(config, KEY_AUTH_SERVICE);
        tokenServiceUrl = CollectionHelper.getMapAttr(config, KEY_TOKEN_SERVICE);
        profileServiceUrl = CollectionHelper.getMapAttr(config, KEY_PROFILE_SERVICE);
        profileServiceParam = CollectionHelper.getMapAttr(config, KEY_PROFILE_SERVICE_PARAM, "access_token");
        // ssoLoginUrl = CollectionHelper.getMapAttr(config, KEY_SSO_LOGIN_URL);
        ssoProxyUrl = CollectionHelper.getMapAttr(config, KEY_SSO_PROXY_URL);
        accountProvider = CollectionHelper.getMapAttr(config, KEY_ACCOUNT_PROVIDER);
        accountMapper = CollectionHelper.getMapAttr(config, KEY_ACCOUNT_MAPPER);
        accountMapperConfig = MappingUtils.parseMappings((Set<String>) config.get(KEY_ACCOUNT_MAPPER_CONFIG));
        attributeMappers = (Set<String>) config.get(KEY_ATTRIBUTE_MAPPER);
        attributeMapperConfig = MappingUtils.parseMappings((Set<String>) config.get(KEY_ATTRIBUTE_MAPPER_CONFIG));
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
        
        customProperties  = CollectionUtils.isNotEmpty((Set<String>)config.get(KEY_CUSTOM_PROPERTIES))  
        		? MappingUtils.parseMappings((Set<String>) config.get(KEY_CUSTOM_PROPERTIES)) : Collections.EMPTY_MAP;

        if(this.authServiceUrl != null && this.authServiceUrl.contains(ESIA_PREFIX)) {
            final String keyPath = customProperties.get(ESIA_KEY_PATH);
            final String certPath = customProperties.get(ESIA_CERT_PATH);
            serviceUrlProvider = new ESIAServiceUrlProvider(keyPath, certPath);
        } else {
            serviceUrlProvider = new DefaultServiceUrlProvider();
        }
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
        Map<String, String> config = new HashMap<>();
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

    public String getAccountProvider() {

        return accountProvider;
    }

    public Set<String> getAttributeMappers() {

        return attributeMappers;
    }

    public Map<String, String> getAccountMapperConfig() {

        return accountMapperConfig;
    }

    public Map<String, String> getAttributeMapperConfig() {

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

    public String getScope() {
        return scope;
    }
    
    public String getAuthServiceUrl() {
    	return authServiceUrl;
    }

    public String getAuthServiceUrl(String originalUrl, String state) throws
            AuthLoginException {
    	return serviceUrlProvider.getServiceUri(this, originalUrl, state);

    }

    private void addParam(StringBuilder url, String key, String value) {
            url.append(url.toString().contains("?") ? "&" : "?")
                    .append(key).append("=").append(value);
    }

    public String getTokenServiceUrl(){
        return tokenServiceUrl;
    }
    
    public Map<String, String> getTokenServiceGETParameters(String code, String authServiceURL)
            throws AuthLoginException {
    	return serviceUrlProvider.getTokenServiceGETparameters(this, code, authServiceURL);
    }

    public Map<String, String> getTokenServicePOSTparameters(String code, String authServiceURL)
            throws AuthLoginException {
    	return serviceUrlProvider.getTokenServicePOSTparameters(this, code, authServiceURL);
    }

    public String getProfileServiceUrl() {
        return profileServiceUrl;
    }

    public Map<String, String> getProfileServiceGetParameters() {
        return Collections.emptyMap();
    }
    
    public void validateConfiguration() throws AuthLoginException {
        if (clientId == null || clientId.isEmpty()) {
            OAuthUtil.debugError("The Client Id can not be empty");
            throw new AuthLoginException("The Client Id can not be empty");
        }
        if (clientSecret == null || clientSecret.isEmpty()){
            OAuthUtil.debugError("The Client Secret can not be empty");
            throw new AuthLoginException("The Client Secret can not be empty");       
        }
        if (authServiceUrl==null || authServiceUrl.isEmpty() || 
                tokenServiceUrl == null || tokenServiceUrl.isEmpty() ||
                (!openIDConnect && (profileServiceUrl == null || profileServiceUrl.isEmpty()))) {
            OAuthUtil.debugError("One or more of the OAuth2 Provider endpoints "
                    + "is empty");
            throw new AuthLoginException("One or more of the OAuth2 Provider "
                    + "endpoints is empty");
        }
        if (accountMapper == null || accountMapper.isEmpty() ||
                attributeMappers == null || attributeMappers.isEmpty()) {
            OAuthUtil.debugError("One or more of the Mappers is empty");
            throw new AuthLoginException("One or more of the Mappers is empty");
        }
        if (getAccountMapperConfig().isEmpty()
                && !getUseAnonymousUserFlag()) {
            OAuthUtil.debugError("The account mapper configuration "
                    + "is empty and anonymous mapping was not enabled");
            throw new AuthLoginException("Aborting authentication, "
                    + "Account Mapper configuration is empty and "
                    + "anonymous mapping was not enabled!");
        }
        if (getUseAnonymousUserFlag()
                && getCreateAccountFlag()) {
            OAuthUtil.debugError("Map to anonymous user and "
                    + "Create Account if does not exist can not be"
                    + " selected at the same time");
            throw new AuthLoginException("Map to anonymous user and "
                    + "Create Account if does not exist can not be"
                    + " selected at the same time");
        }
    }

    public String getClientId() {
        return clientId;
    }
    
    public String getClientSecret() {
    	return clientSecret;
    }

    public boolean isOpenIDConnect() {
        return openIDConnect;
    }
    
    
    public Map<String, String> getCustomProperties() {
        return customProperties;
    }

    public ServiceUrlProvider getServiceUrlProvider() {
        return serviceUrlProvider;
    }

}
