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
 * $Id: SessionAttributeNames.java,v 1.2 2009/06/17 21:45:42 goodearth Exp $
 */

package com.sun.identity.config;

public interface SessionAttributeNames {
    static final String CONFIG_VAR_ADMIN_PWD = "ADMIN_PWD";
    static final String CONFIG_VAR_AMLDAPUSERPASSWD = "AMLDAPUSERPASSWD";
    static final String CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM = "AMLDAPUSERPASSWD_CONFIRM";

    static final  String ENCRYPTION_KEY = "encryptionKey";
    static final  String ENCLDAPUSERPASSWD = "ENCLDAPUSERPASSWD";
    static final  String SERVER_URL = "serverURL";
    static final  String COOKIE_DOMAIN = "cookieDomain";
    static final  String PLATFORM_LOCALE = "platformLocale";
    static final  String CONFIG_DIR = "configDirectory";

    static final  String CONFIG_STORE_SSL = "configStoreSSL";
    static final  String CONFIG_STORE_HOST = "configStoreHost";
    static final  String CONFIG_STORE_PORT = "configStorePort";
    static final  String CONFIG_STORE_LOGIN_ID = "configStoreLoginId";
    static final  String CONFIG_STORE_ROOT_SUFFIX = "rootSuffix";
    static final  String CONFIG_STORE_PWD = "configStorePassword";

    static final  String CONFIG_VAR_DATA_STORE = "DATA_STORE";
    static final  String DS_EMB_REPL_FLAG = "DS_EMB_REPL_FLAG";
    static final  String EXISTING_HOST = "existingHost";
    static final  String EXISTING_STORE_HOST = "existingStoreHost";
    static final  String EXISTING_PORT = "existingPort";
    static final  String EXISTING_STORE_PORT = "existingStorePort";
    static final  String LOCAL_REPL_PORT = "localRepPort";
    static final  String EXISTING_REPL_PORT = "existingRepPort";
    static final  String EXISTING_SERVER_ID = "existingserverid";

    static final  String EXT_DATA_STORE = "EXT_DATA_STORE";
    static final  String USER_STORE_SSL = "userStoreSSL";
    static final  String USER_STORE_HOST = "userStoreHostName";
    static final  String USER_STORE_DOMAINNAME = "userStoreDomainName";
    static final  String USER_STORE_PORT = "userStorePort";
    static final  String USER_STORE_ROOT_SUFFIX = "userStoreRootSuffix";
    static final  String USER_STORE_LOGIN_ID = "userStoreLoginID";
    static final  String USER_STORE_LOGIN_PWD = "userStoreLoginPassword";
    static final  String USER_STORE_TYPE = "userStoreType";

    static final  String LB_SITE_NAME = "wizardLoadBalancerSiteName";
    static final  String LB_PRIMARY_URL = "wizardLoadBalancerURL";
    static final  String LB_SESSION_HA_SFO = "wizardLoadBalancerSessionHASFO";
}
