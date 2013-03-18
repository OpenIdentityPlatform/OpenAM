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
 * $Id: IDPDiscoveryConstants.java,v 1.5 2009/11/03 00:50:34 madan_ranganath Exp $
 *
 */



package com.sun.identity.saml2.idpdiscovery;

/**
 * This interface represents a collection of common constants used by
 * the classes in saml2 idp discovery Service.  Classes implementing this
 * interface can use these constants.
 */

public interface IDPDiscoveryConstants {    
   
    public static final char   QUESTION_MARK = '?';
    public static final char   AMPERSAND = '&';
    public static final char   EQUAL_TO = '=';   
    public static final String LRURL = "RelayState";
    public static final String PREFERRED_COOKIE_SEPERATOR = " ";
    public static final String SESSION_COOKIE = "SESSION";
    public static final String PERSISTENT_COOKIE = "PERSISTENT";
    public static final int PERSISTENT_COOKIE_AGE = 31536000; // 365 days
    public static final int SESSION_COOKIE_AGE = -1;

    public static final String IDPDISCOVERY_COOKIE_TYPE =
                        "com.sun.identity.saml2.idpdiscovery.cookietype";
    public static final String IDPDISCOVERY_URL_SCHEME = 
                        "com.sun.identity.saml2.idpdiscovery.urlscheme";
    public static final String IDPDISCOVERY_COOKIE_DOMAIN = 
                        "com.sun.identity.saml2.idpdiscovery.cookiedomain";
    public static final String HTTPS = "https"; 
    public static final String DEBUG_LEVEL = 
                        "com.iplanet.services.debug.level";  
    public static final String DEBUG_DIR = 
                        "com.iplanet.services.debug.directory";
    public static final String NO_DIR = 
                        "com.sun.identity.services.debug.nodir";    
    public static final String AM_COOKIE_SECURE = 
                        "com.iplanet.am.cookie.secure";
    public static final String AM_COOKIE_ENCODE = 
                        "com.iplanet.am.cookie.encode";
    public static final String AM_COOKIE_HTTPONLY = 
                        "com.sun.identity.cookie.httponly";
    public static final String SAML2_WRITER_URI = "/saml2writer"; 
    public static final String IDFF_WRITER_URI = "/idffwriter";
    public static final String SAML2_READER_URI = "/saml2reader"; 
    public static final String IDFF_READER_URI = "/idffreader";
    public static final String SAML2_COOKIE_NAME = "_saml_idp";
    public static final String IDFF_COOKIE_NAME = "_liberty_idp";   
    public String ERROR_URL_PARAM_NAME = 
        "com.sun.identity.saml2.idpdiscovery.errorurl";
}

