/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMAuthErrorCode.java,v 1.5 2009/03/05 00:03:56 manish_rustagi Exp $
 *
 */


package com.sun.identity.authentication.service;


/**
 * Class is representing error code for different error states
 */ 
public class AMAuthErrorCode {
    /**
     *  profile not found
     */ 
    public static final String AUTH_PROFILE_ERROR="100";

    /**
     *  User account is expired
     */ 
    public static final String AUTH_ACCOUNT_EXPIRED="101";

    /**
     *  Auth error happens
     */ 
    public static final String AUTH_ERROR = "102";

    /**
     *  Password is invalid
     */ 
    public static final String AUTH_INVALID_PASSWORD = "103";

    /**
     *  User profile is inactive
     */ 
    public static final String AUTH_USER_INACTIVE = "104";

    /**
     *  Any config not found for the auth module
     */ 
    public static final String AUTH_CONFIG_NOT_FOUND = "105";

    /**
     *  Persistent cookie is invalid
     */ 
    public static final String AUTH_INVALID_PCOOKIE = "106";

    /**
     *  Auth login failed - module failure
     */ 
    public static final String AUTH_LOGIN_FAILED = "107";

    /**
     *  Domain name is invalid
     */ 
    public static final String AUTH_INVALID_DOMAIN = "108";

    /**
     *  Organization is inactive
     */ 
    public static final String AUTH_ORG_INACTIVE = "109";

    /**
     *  Login timed out
     */ 
    public static final String AUTH_TIMEOUT = "110";

    /**
     *  Module denied
     */ 
    public static final String AUTH_MODULE_DENIED = "111";

    /**
     *  User locked
     */ 
    public static final String AUTH_USER_LOCKED="112";

    /**
     *  User role not found
     */ 
    public static final String AUTH_USER_NOT_FOUND="113";

    /**
     *  Authentication type denied
     */ 
    public static final String AUTH_TYPE_DENIED="114";

    /**
     *  Max sessions reached
     */ 
    public static final String AUTH_MAX_SESSION_REACHED="115";

    /**
     *  Profile cannot be created
     */ 
    public static final String AUTH_PROFILE_CREATE="116";

    /**
     *  Http negotiation handshaking error
     */ 
    public static final String HTTP_NEGO = "117";
        
    /**
     *  Session creation error
     */ 
    public static final String AUTH_SESSION_CREATE_ERROR = "118";

    /**
     *  Invalid auth level
     */ 
    public static final String INVALID_AUTH_LEVEL = "119";

    /**
     *  Module based authentication is not allowed
     */ 
    public static final String MODULE_BASED_AUTH_NOT_ALLOWED = "120";

    /**
     *  Too many auth attempts
     */ 
    public static final String AUTH_TOO_MANY_ATTEMPTS = "121";

    /*
     *  Invalid SSO Token presented by remote Auth
     */
    public static final String REMOTE_AUTH_INVALID_SSO_TOKEN = "122";

    /* locked at DS level - Constraint Violation */
    public static final String AUTH_USER_LOCKED_IN_DS = "123";
    
    /**
     *  Session upgrade failed
     */
    public static final String SESSION_UPGRADE_FAILED = "124";
    
}
