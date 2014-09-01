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
 * $Id: OAuthServiceConstants.java,v 1.3 2010/01/20 17:51:37 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

/**
 * The Constants for OAuth Token Service
 *
 * @author Hua Cui <hua.cui@Sun.COM>
 */
public interface OAuthServiceConstants {

    // Entity types
    public static final String CONSUMER_TYPE = "oauth.consumer";
    public static final String REQUEST_TOKEN_TYPE = "oauth.requesttoken";
    public static final String ACCESS_TOKEN_TYPE = "oauth.accesstoken";

    // Consumer attributes
    public static final String ETAG = "etag";
    public static final String CONSUMER_NAME = "cons_name";
    public static final String CONSUMER_SECRET = "cons_secret";
    public static final String CONSUMER_RSA_KEY = "cons_rsakey";
    public static final String CONSUMER_KEY = "cons_key";
    public static final String CONSUMER_ID = "consumer_id";

    // Request Token attributes
    public static final String REQUEST_TOKEN_URI = "reqt_uri";
    public static final String REQUEST_TOKEN_VAL = "reqt_val";
    public static final String REQUEST_TOKEN_SECRET = "reqt_secret";
    public static final String REQUEST_TOKEN_PPAL_ID = "reqt_ppalid";
    public static final String REQUEST_TOKEN_LIFETIME = "reqt_lifetime";
    public static final String REQUEST_TOKEN_CALLBACK = "reqt_callback";
    public static final String REQUEST_TOKEN_VERIFIER = "reqt_verifier";

    // Access Token attributes
    public static final String ACCESS_TOKEN_URI = "acct_uri";
    public static final String ACCESS_TOKEN_VAL = "acct_val";
    public static final String ACCESS_TOKEN_SECRET = "acct_secret";
    public static final String ACCESS_TOKEN_PPAL_ID = "acct_ppalid";
    public static final String ACCESS_TOKEN_LIFETIME = "acct_lifetime";
    public static final String ACCESS_TOKEN_ONETIME = "acct_onetime";

    // OAuth parameters
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String REQUEST_TOKEN = "request_token";
    public static final String OAUTH_TOKEN = "oauth_token";
    public static final String OAUTH_CALLBACK = "oauth_callback";
    public static final String OAUTH_ID = "id";
    public static final String OAUTH_TOKEN_SECRET = "oauth_token_secret";
    public static final String OAUTH_SUBJECT = "subject";
    public static final String OAUTH_SHARED_SECRET = "shared_secret";
    public static final String OAUTH_OOB = "oob";
    public static final String OAUTH_CALLBACK_CONFIRMED = "oauth_callback_confirmed";
    public static final String OAUTH_VERIFIER = "oauth_verifier";

    public static final String C_NAME = "name";
    public static final String C_CERT = "certificate";
    public static final String C_SIGNATURE_METHOD = "signature_method";
    public static final String C_SECRET = "secret";
    public static final String C_KEY = "cons_key";
    public static final String C_ID = "cid";

    // HTTP status codes
    public static final int OK = 200;
    public static final int TOKEN_CREATED = 201;
    public static final int TOKEN_UPDATED = 204;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int PRECONDITION_FAILED = 412;

}
 
