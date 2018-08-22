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
 */

package org.forgerock.openam.authentication.modules.oauth2;

public class OAuthParam {
    
    static final String MODULE_NAME = "OAuth";
    public static final String BUNDLE_NAME = "amAuthOAuth";

    static final String KEY_CLIENT_ID = "iplanet-am-auth-oauth-client-id";
    static final String KEY_CLIENT_SECRET = "iplanet-am-auth-oauth-client-secret";
    static final String KEY_AUTH_SERVICE = "iplanet-am-auth-oauth-auth-service";
    static final String KEY_TOKEN_SERVICE = "iplanet-am-auth-oauth-token-service";
    static final String KEY_PROFILE_SERVICE = "iplanet-am-auth-oauth-user-profile-service";
    static final String KEY_PROFILE_SERVICE_PARAM = "iplanet-am-auth-oauth-user-profile-param";
    static final String KEY_SCOPE = "iplanet-am-auth-oauth-scope";
    static final String KEY_SSO_PROXY_URL = "iplanet-am-auth-oauth-sso-proxy-url";
    static final String KEY_AUTH_LEVEL = "iplanet-am-auth-oauth-auth-level";
    static final String KEY_ACCOUNT_PROVIDER = "org-forgerock-auth-oauth-account-provider";
    static final String KEY_ACCOUNT_MAPPER = "org-forgerock-auth-oauth-account-mapper";
    static final String KEY_ACCOUNT_MAPPER_CONFIG =
            "org-forgerock-auth-oauth-account-mapper-configuration";
    static final String KEY_ATTRIBUTE_MAPPER = "org-forgerock-auth-oauth-attribute-mapper";
    static final String KEY_ATTRIBUTE_MAPPER_CONFIG =
            "org-forgerock-auth-oauth-attribute-mapper-configuration";
    static final String KEY_SAVE_ATTRIBUTES_TO_SESSION = 
            "org-forgerock-auth-oauth-save-attributes-to-session-flag";
    static final String KEY_MAIL_ATTRIBUTE = "org-forgerock-auth-oauth-mail-attribute";
    static final String KEY_CREATE_ACCOUNT = "org-forgerock-auth-oauth-createaccount-flag";
    static final String KEY_PROMPT_PASSWORD = "org-forgerock-auth-oauth-prompt-password-flag";
    static final String KEY_LOGOUT_SERVICE_URL = "org-forgerock-auth-oauth-logout-service-url";
    static final String KEY_LOGOUT_BEHAVIOUR = "org-forgerock-auth-oauth-logout-behaviour";
    static final String KEY_MAP_TO_ANONYMOUS_USER_FLAG = "org-forgerock-auth-oauth-map-to-anonymous-flag";
    static final String KEY_ANONYMOUS_USER = "org-forgerock-auth-oauth-anonymous-user";
    static final String KEY_EMAIL_GWY_IMPL = "org-forgerock-auth-oauth-email-gwy-impl";
    static final String KEY_SMTP_HOSTNAME = "org-forgerock-auth-oauth-smtp-hostname";
    static final String KEY_SMTP_PORT = "org-forgerock-auth-oauth-smtp-port";
    static final String KEY_SMTP_USERNAME = "org-forgerock-auth-oauth-smtp-username";
    static final String KEY_SMTP_PASSWORD = "org-forgerock-auth-oauth-smtp-password";
    static final String KEY_SMTP_SSL_ENABLED = "org-forgerock-auth-oauth-smtp-ssl_enabled";
    static final String KEY_EMAIL_FROM = "org-forgerock-auth-oauth-smtp-email-from";
    static final String KEY_CUSTOM_PROPERTIES = "openam-auth-oauth2-custom-properties";
    
    public final static String CODE_CHALLENGE_METHOD = "org-forgerock-auth-oauth-code-challenge-method-algorithm";
        

    // openam parameters
    public final static String PARAM_GOTO = "goto";
    public final static String PARAM_REALM = "realm";

    public final static String PARAM_MODULE = "module";
    // OAuth 2.0 parameters
    public final static String PARAM_CODE = "code";
    public final static String PARAM_REDIRECT_URI = "redirect_uri";
    public final static String PARAM_SCOPE = "scope";
    public final static String PARAM_CLIENT_SECRET = "client_secret";
    public final static String PARAM_CLIENT_ID = "client_id";
    public final static String PARAM_ACCESS_TOKEN =  "access_token";
    public final static String PARAM_REFRESH_TOKEN = "refresh_token";
    public final static String PARAM_GRANT_TYPE = "grant_type";

    // oauthproxy parameters
    public final static String PARAM_ACTIVATION = "activation";
    public final static String PARAM_TOKEN1 = "token1";
    public final static String PARAM_TOKEN2 = "token2";


    // Session parameters set by the module
    public final static String SESSION_OAUTH_TOKEN = "OAuthToken";
    public final static String SESSION_LOGOUT_BEHAVIOUR = "OAuth2logoutBehaviour";
    public final static String SESSION_OAUTH_SCOPE = "OAuthScope";
    
    // Cookies used by the module
    public final static String COOKIE_ORIG_URL = "ORIG_URL";
    public final static String COOKIE_PROXY_URL = "PROXY_URL";
    public final static String COOKIE_LOGOUT_URL = "OAUTH_LOGOUT_URL";
    public final static String NONCE_TOKEN_ID = "NTID";
    
    // Login states
    public final static int LOGIN_START = 1;
    public final static int GET_OAUTH_TOKEN_STATE = 2;
    public final static int SET_PASSWORD_STATE = 3;
    public final static int CREATE_USER_STATE = 4;
    
    // Email 
    public final static String MESSAGE_FROM = "messageFrom";
    public final static String MESSAGE_SUBJECT = "messageSubject";
    public final static String MESSAGE_BODY = "messageBody";
    
    // Logout
    public final static String PARAM_LOGGEDOUT = "loggedout";
    public final static String PARAM_LOGOUT_URL = "logoutURL"; 
    public final static String LOGOUT_FORM = "logoutForm";

    //OpenID Connect
    public final static String ID_TOKEN = "id_token";
    public final static String OIDC_SCOPE = "openid";
    public final static String SCOPE_SEPARATOR = " ";
    
}

