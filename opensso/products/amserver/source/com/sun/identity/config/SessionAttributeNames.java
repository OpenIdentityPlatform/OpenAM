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
    String CONFIG_VAR_ADMIN_PWD = "ADMIN_PWD";
    String CONFIG_VAR_AMLDAPUSERPASSWD = "AMLDAPUSERPASSWD";
    String CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM = "AMLDAPUSERPASSWD_CONFIRM";

    String ENCRYPTION_KEY = "encryptionKey";
    String ENCLDAPUSERPASSWD = "ENCLDAPUSERPASSWD";
    String SERVER_URL = "serverURL";
    String COOKIE_DOMAIN = "cookieDomain";
    String PLATFORM_LOCALE = "platformLocale";
    String CONFIG_DIR = "configDirectory";

    String CONFIG_STORE_SSL = "configStoreSSL";
    String CONFIG_STORE_HOST = "configStoreHost";
    String CONFIG_STORE_PORT = "configStorePort";
    String CONFIG_STORE_LOGIN_ID = "configStoreLoginId";
    String CONFIG_STORE_ROOT_SUFFIX = "rootSuffix";
    String CONFIG_STORE_PWD = "configStorePassword";

    String CONFIG_VAR_DATA_STORE = "DATA_STORE";
    String DS_EMB_REPL_FLAG = "DS_EMB_REPL_FLAG";
    String EXISTING_HOST = "existingHost";
    String EXISTING_STORE_HOST = "existingStoreHost";
    String EXISTING_PORT = "existingPort";
    String EXISTING_STORE_PORT = "existingStorePort";
    String LOCAL_REPL_PORT = "localRepPort";
    String EXISTING_REPL_PORT = "existingRepPort";
    String EXISTING_SERVER_ID = "existingserverid";

    String EXT_DATA_STORE = "EXT_DATA_STORE";
    String USER_STORE_SSL = "userStoreSSL";
    String USER_STORE_HOST = "userStoreHostName";
    String USER_STORE_DOMAINNAME = "userStoreDomainName";
    String USER_STORE_PORT = "userStorePort";
    String USER_STORE_ROOT_SUFFIX = "userStoreRootSuffix";
    String USER_STORE_LOGIN_ID = "userStoreLoginID";
    String USER_STORE_LOGIN_PWD = "userStoreLoginPassword";
    String USER_STORE_TYPE = "userStoreType";

    String LB_SITE_NAME = "wizardLoadBalancerSiteName";
    String LB_PRIMARY_URL = "wizardLoadBalancerURL";
}
