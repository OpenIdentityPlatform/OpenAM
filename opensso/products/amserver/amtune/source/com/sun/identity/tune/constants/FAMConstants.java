/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: FAMConstants.java,v 1.7 2009/12/09 00:30:48 ykwon Exp $
 */

package com.sun.identity.tune.constants;

/**
 * OpenSSO Constants.
 * 
 */
public interface FAMConstants extends AMTuneConstants {
    static String OPENSSO_SERVER_BACKUP_DIR = "conf-opensso-backup";
    static String SSOADM_LOCATION = "SSOADM_LOCATION";
    static String OPENSSOSERVER_URL = "OPENSSOSERVER_URL";
    static String OPENSSOADMIN_USER = "OPENSSOADMIN_USER";
    static String REALM_NAME = "REALM_NAME";
    static String SDK_CACHE_MAXSIZE = "com.iplanet.am.sdk.cache.maxSize";
    static String NOTIFICATION_THREADPOOL_SIZE = 
            "com.iplanet.am.notification.threadpool.size";
    static String NOTIFICATION_THREADPOOL_THRESHOLD = 
            "com.iplanet.am.notification.threadpool.threshold";
    static String MAX_SESSIONS = "com.iplanet.am.session.maxSessions";

    /**
      * HTTP_SESSION_ENABLED parameter is not used in OpenSSO: so
      * it is meaningless to change its value.
      *
      */   
    //static String HTTP_SESSION_ENABLED = 
    //        "com.iplanet.am.session.httpSession.enabled";
    static String SESSION_PURGE_DELAY = "com.iplanet.am.session.purgedelay";
    static String INVALID_SESSION_MAX_TIME = 
            "com.iplanet.am.session.invalidsessionmaxtime";
    static String SERVER_NAME_OPT = "--servername";
    static String UPDATE_SERVER_SUB_CMD = "update-server-cfg";
    static String GET_SVRCFG_XML_SUB_CMD = "get-svrcfg-xml";
    static String SET_SVRCFG_XML_SUB_CMD = "set-svrcfg-xml";
    static String GET_ATTR_DEFS_SUB_CMD = "get-attr-defs";
    static String SET_ATTR_DEFS_SUB_CMD = "set-attr-defs";
    static String SHOW_DATASTORE_SUB_CMD = "show-datastore";
    static String UPDATE_DATASTORE_SUB_CMD = "update-datastore";
    static String LIST_DATA_STORES_SUB_CMD = "list-datastores";
    static String LIST_SERVER_CFG_SUB_CMD = "list-server-cfg";
    static String SHOW_REALM_SVC_SUB_CMD = "show-realm-svcs";
    static String GET_REALM_SVC_ATTRS_SUB_CMD = "get-realm-svc-attrs";
    static String SET_REALM_SVC_ATTRS_SUB_CMD = "set-svc-attrs";
    static String SERVICE_NAME_OPT = "--servicename";
    static String ATTR_NAMES_OPT = "--attributenames";
    static String OUTFILE_OPT = "--outfile";
    static String XML_FILE_OPT = "--xmlfile";
    static String SCHEMA_TYPE_OPT = "--schematype";
    static String ATTR_VALUES_OPT = "--attributevalues";
    static String REALM_OPT = "--realm";
    static String DATAFILE_OPT = "--datafile";
    static String MIN_CONN_POOL = "minConnPool";
    static String SMS_ELEMENT = "name=\"sms\"";
    static String DEFAULT_SERVER_ELEMENT = " name=\"default\"";
    static String MAX_CONN_POOL = "maxConnPool";
    static String AUTH_SVC = "iPlanetAMAuthService";
    static String AUTH_LDAP_SVC = "iPlanetAMAuthLDAPService";
    static String LOGGING_SVC = "iPlanetAMLoggingService";
    static String GLOBAL_SCHEMA = "global";
    static String DYNAMIC_SCHEMA = "dynamic";
    static String ORG_SCHEMA = "organization";
    static String LDAP_CONNECTION_POOL_SIZE = 
            "iplanet-am-auth-ldap-connection-pool-default-size";
    static String LOGGING_BUFFER_SIZE = "iplanet-am-logging-buffer-size";
    static String LDAP_CONN_POOL_MIN = 
            "sun-idrepo-ldapv3-config-connection_pool_min_size";
    static String LDAP_CONN_POOL_MAX =
            "sun-idrepo-ldapv3-config-connection_pool_max_size";
    static String LDAP_CONFIG_CACHE_SIZE = 
            "sun-idrepo-ldapv3-config-cache-size";
}
