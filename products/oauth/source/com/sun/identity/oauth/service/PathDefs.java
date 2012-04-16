/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PathDefs.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Hua Cui <hua.cui@Sun.COM>
 */
public class PathDefs {

    // Global paths
    static final String GENERIC_PATH = "/oauth";
    static final String VERSION_PATH = "/1";

    // OAuth protocol endpoints
    static final String REQUEST_TOKEN_REQUEST_PATH = VERSION_PATH + GENERIC_PATH  + "/get_request_token";
    static final String ACCESS_TOKEN_REQUEST_PATH = VERSION_PATH + GENERIC_PATH + "/get_access_token";

    // Token Service endpoints
    static final String REQUEST_TOKENS_PATH = VERSION_PATH + GENERIC_PATH + "/rtoken";
    static final String ACCESS_TOKENS_PATH = VERSION_PATH + GENERIC_PATH + "/atoken";
    static final String CONSUMERS_PATH = VERSION_PATH + GENERIC_PATH + "/consumer";
    static final String CONSUMER_REGISTRATION_PATH = VERSION_PATH + GENERIC_PATH + "/consumer_registration";

    // Other endpoints
    static final String NO_BROWSER_AUTHORIZATION_PATH = VERSION_PATH + GENERIC_PATH + "/NoBrowserAuthorization";
    static final String CREATE_AUTHORIZATION_PATH = VERSION_PATH + GENERIC_PATH + "/AuthorizationFactory";

    // OpenSSO server URL
    public static final String OPENSSO_SERVER_URL = "com.sun.identity.oauth.server.url";

    // OpenSSO server Login URI
    public static final String OPENSSO_SERVER_LOGIN_URI = "com.sun.identity.oauth.server.login.uri";

    // OpenSSO session cookie name
    public static final String OPENSSO_COOKIE_NAME = "com.sun.identity.oauth.server.cookie.name";

    // OpenSSO server authentication service endpoint
    public static final String OPENSSO_SERVER_AUTHENTICATION_ENDPOINT = "/identity/authenticate";

    // OpenSSO server attributes service endpoint
    public static final String OPENSSO_SERVER_ATTRIBUTES_ENDPOINT = "/identity/attributes";

    // OpenSSO server read attributes service endpoint
    public static final String OPENSSO_SERVER_READ_ATTRIBUTES_ENDPOINT = "/identity/read";

    // OpenSSO server token validation service endpoint
    public static final String OPENSSO_SERVER_TOKEN_VALIDATION_ENDPOINT = "/identity/isTokenValid";

    // OpenSSO server token validation service endpoint
    public static final String OPENSSO_CORE_TOKEN_SERVICE_ENDPOINT = "/ws/1/token";

    // Life time of tokens
    public static final String ACCESS_TOKEN_LIFETIME = "com.sun.identity.oauth.service.accesstoken.lifetime";
    public static final String REQUEST_TOKEN_LIFETIME = "com.sun.identity.oauth.service.requesttoken.lifetime";
    public static final String APP_USER_NAME = "com.sun.identity.oauth.service.appuser.name";
    public static final String APP_USER_PASSWORD = "com.sun.identity.oauth.service.appuser.password";
}
 
