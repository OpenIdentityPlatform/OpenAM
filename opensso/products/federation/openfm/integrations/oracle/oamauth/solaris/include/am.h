/* -*- Mode: C -*-
 *
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
 * $Id: am.h,v 1.4 2008/08/19 19:11:35 veiming Exp $
 */

/*
 * Abstract:
 *
 * General utility routines provided by the OpenSSO 
 * Access Management library.
 *
 */

#ifndef AM_H
#define AM_H

#include <am_types.h>

AM_BEGIN_EXTERN_C

#define AM_COMMON_PROPERTY_PREFIX	"com.sun.am."
#define	AM_POLICY_PROPERTY_PREFIX	"com.sun.am.policy.am."
#define AM_AUTH_PROPERTY_PREFIX         "com.sun.am.auth."
#define AM_SSO_PROPERTY_PREFIX          "com.sun.am.sso."
#define AM_LOG_PROPERTY_PREFIX          "com.sun.am.log."

/* Common Properties */
#define	AM_COMMON_SSL_CERT_DIR_PROPERTY AM_COMMON_PROPERTY_PREFIX "sslcert.dir"
#define	AM_COMMON_SSL_CERT_DIR_PROPERTY_OLD AM_COMMON_PROPERTY_PREFIX "sslCertDir"
#define AM_COMMON_CERT_DB_PREFIX_PROPERTY AM_COMMON_PROPERTY_PREFIX "certdb.prefix"
#define AM_COMMON_CERT_DB_PREFIX_PROPERTY_OLD AM_COMMON_PROPERTY_PREFIX "certDbPrefix"

#define AM_COMMON_TRUST_SERVER_CERTS_PROPERTY AM_COMMON_PROPERTY_PREFIX "trust_server_certs"
#define AM_COMMON_TRUST_SERVER_CERTS_PROPERTY_OLD AM_COMMON_PROPERTY_PREFIX "trustServerCerts"
#define	AM_COMMON_COOKIE_NAME_PROPERTY	AM_COMMON_PROPERTY_PREFIX "cookie.name"
#define	AM_COMMON_COOKIE_NAME_PROPERTY_OLD	AM_COMMON_PROPERTY_PREFIX "cookieName"
#define	AM_COMMON_COOKIE_SECURE_PROPERTY	AM_COMMON_PROPERTY_PREFIX "cookie.secure"
#define AM_COMMON_CERT_DB_PASSWORD_PROPERTY AM_COMMON_PROPERTY_PREFIX "certdb.password"
#define AM_COMMON_CERT_DB_PASSWORD_PROPERTY_OLD AM_COMMON_PROPERTY_PREFIX "certDBPassword"
#define	AM_COMMON_NAMING_URL_PROPERTY	AM_COMMON_PROPERTY_PREFIX "naming.url"
#define	AM_COMMON_NAMING_URL_PROPERTY_OLD	AM_COMMON_PROPERTY_PREFIX "namingURL"
#define AM_COMMON_NOTIFICATION_ENABLE_PROPERTY AM_COMMON_PROPERTY_PREFIX "notification.enable"
#define AM_COMMON_NOTIFICATION_ENABLE_PROPERTY_OLD AM_COMMON_PROPERTY_PREFIX "notificationEnabled"
#define AM_COMMON_NOTIFICATION_URL_PROPERTY	AM_COMMON_PROPERTY_PREFIX "notification.url"
#define AM_COMMON_NOTIFICATION_URL_PROPERTY_OLD	AM_COMMON_PROPERTY_PREFIX "notificationURL"
#define AM_COMMON_LOADBALANCE_PROPERTY    AM_COMMON_PROPERTY_PREFIX "load_balancer.enable"
#define AM_COMMON_LOADBALANCE_PROPERTY_OLD    AM_COMMON_PROPERTY_PREFIX "loadBalancer_enable"
#define	AM_COMMON_IGNORE_NAMING_SERVICE_PROPERTY	AM_COMMON_PROPERTY_PREFIX "ignore.naming_service"
#define AM_COMMON_RECEIVE_TIMEOUT_PROPERTY AM_COMMON_PROPERTY_PREFIX "receive_timeout"
#define AM_COMMON_TCP_NODELAY_ENABLE_PROPERTY AM_COMMON_PROPERTY_PREFIX "tcp_nodelay.enable"
#define AM_COMMON_CONNECT_TIMEOUT_PROPERTY AM_COMMON_PROPERTY_PREFIX "connect_timeout"
#define AM_COMMON_IGNORE_SERVER_CHECK    AM_COMMON_PROPERTY_PREFIX "ignore_server_check"
#define AM_COMMON_POLL_PRIMARY_SERVER AM_COMMON_PROPERTY_PREFIX "poll_primary_server"

/* Log Properties */
#define AM_COMMON_LOG_LEVELS_PROPERTY	AM_COMMON_PROPERTY_PREFIX "log.level"
#define AM_COMMON_LOG_LEVELS_PROPERTY_OLD	AM_COMMON_PROPERTY_PREFIX "logLevels"
#define AM_COMMON_IGNORE_PATH_INFO_OLD    AM_COMMON_PROPERTY_PREFIX "ignore_path_info"
#define AM_COMMON_LOG_FILE_PROPERTY_OLD	AM_COMMON_PROPERTY_PREFIX "logFile"
#define AM_COMMON_SERVER_LOG_FILE_PROPERTY_OLD	AM_COMMON_PROPERTY_PREFIX "serverLogFile"

/* Authentication Properties */
#define AM_AUTH_ORGANIZATION_NAME_PROPERTY AM_AUTH_PROPERTY_PREFIX "org.name"
#define AM_AUTH_ORGANIZATION_NAME_PROPERTY_OLD AM_AUTH_PROPERTY_PREFIX "orgName"
#define AM_AUTH_CERT_ALIAS_PROPERTY AM_AUTH_PROPERTY_PREFIX "certificate.alias"
#define AM_AUTH_CERT_ALIAS_PROPERTY_OLD AM_AUTH_PROPERTY_PREFIX "certificateAlias"
#define AM_AUTH_SERVICE_URLS_PROPERTY AM_AUTH_PROPERTY_PREFIX "auth_service.url"
#define AM_AUTH_SERVICE_URLS_PROPERTY_OLD AM_AUTH_PROPERTY_PREFIX "authServiceURL"

/* Policy Properties */
#define AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY AM_POLICY_PROPERTY_PREFIX "url_comparison.case_ignore"
#define AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY_OLD AM_POLICY_PROPERTY_PREFIX "urlComparison.caseIgnore"
#define AM_POLICY_PROFILE_ATTRS_MODE_OLD AM_POLICY_PROPERTY_PREFIX "ldapattribute.mode"
#define AM_POLICY_PROFILE_ATTRS_MAP_OLD  AM_POLICY_PROPERTY_PREFIX  "headerAttributes"
#define AM_POLICY_PROFILE_ATTRS_COOKIE_PFX_OLD AM_POLICY_PROPERTY_PREFIX "ldapattribute.cookiePrefix"
#define AM_POLICY_PROFILE_ATTRS_COOKIE_MAX_AGE_OLD AM_POLICY_PROPERTY_PREFIX "ldapattribute.cookieMaxAge"

#define	AM_POLICY_HASH_BUCKET_SIZE_PROPERTY	AM_POLICY_PROPERTY_PREFIX "hash_bucket.size"
#define	AM_POLICY_HASH_BUCKET_SIZE_PROPERTY_OLD	AM_POLICY_PROPERTY_PREFIX "hashBucketSize"
#define AM_POLICY_HASH_TIMEOUT_MINS_PROPERTY AM_POLICY_PROPERTY_PREFIX "polling.interval"
#define AM_POLICY_HASH_TIMEOUT_MINS_PROPERTY_OLD AM_POLICY_PROPERTY_PREFIX "cacheEntryLifeTime"
#define	AM_POLICY_LOGIN_URL_PROPERTY	AM_POLICY_PROPERTY_PREFIX "login.url"
#define	AM_POLICY_LOGIN_URL_PROPERTY_OLD	AM_POLICY_PROPERTY_PREFIX "loginURL"
#define AM_POLICY_PASSWORD_PROPERTY		AM_POLICY_PROPERTY_PREFIX "password"
#define AM_POLICY_USER_NAME_PROPERTY	AM_POLICY_PROPERTY_PREFIX "username"
#define AM_POLICY_ORG_NAME_PROPERTY	AM_POLICY_PROPERTY_PREFIX "org.name"
#define AM_POLICY_MODULE_NAME_PROPERTY	AM_POLICY_PROPERTY_PREFIX "auth_module"
#define AM_POLICY_MAX_THREADS_PROPERTY      AM_POLICY_PROPERTY_PREFIX "max_threads"

#define AM_POLICY_USER_ID_PARAM_PROPERTY AM_POLICY_PROPERTY_PREFIX "userid.param"
#define AM_POLICY_USER_ID_PARAM_PROPERTY_OLD AM_POLICY_PROPERTY_PREFIX "userIdParam"
#define AM_POLICY_USER_ID_PARAM_TYPE_PROPERTY AM_POLICY_PROPERTY_PREFIX "userid.param.type"
#define AM_POLICY_USER_ID_PARAM_TYPE_PROPERTY_OLD AM_POLICY_PROPERTY_PREFIX "userIdParamType"

#define AM_POLICY_FETCH_FROM_ROOT_RSRC_PROPERTY      AM_POLICY_PROPERTY_PREFIX "fetch_from_root_resource"
#define AM_POLICY_FETCH_FROM_ROOT_RSRC_PROPERTY_OLD      AM_POLICY_PROPERTY_PREFIX "fetchFromRootResource"

#define	AM_SSO_HASH_BUCKET_SIZE_PROPERTY	AM_SSO_PROPERTY_PREFIX "hash_bucket.size"
#define	AM_SSO_HASH_BUCKET_SIZE_PROPERTY_OLD	AM_SSO_PROPERTY_PREFIX "hashBucketSize"
#define AM_SSO_HASH_TIMEOUT_MINS_PROPERTY   AM_SSO_PROPERTY_PREFIX "cache_entry.lifetime"
#define AM_SSO_HASH_TIMEOUT_MINS_PROPERTY_OLD   AM_SSO_PROPERTY_PREFIX "cacheEntryLifeTime"
#define AM_SSO_MAX_THREADS_PROPERTY   AM_SSO_PROPERTY_PREFIX "max_threads"
#define AM_SSO_MAX_THREADS_PROPERTY_OLD   AM_SSO_PROPERTY_PREFIX "maxThreads"
#define AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY AM_SSO_PROPERTY_PREFIX "polling.period"
#define AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY_OLD AM_SSO_PROPERTY_PREFIX "checkCacheInterval"
#define AM_SSO_DEFAULT_SESSION_URL   AM_SSO_PROPERTY_PREFIX "default_session.url"
#define AM_SSO_DEFAULT_SESSION_URL_OLD   AM_SSO_PROPERTY_PREFIX "defaultSessionURL"

#define AM_COMMON_IGNORE_PREFERRED_NAMING_URL_PROPERTY  AM_COMMON_PROPERTY_PREFIX "ignore.preferred_naming_url"

/*
 * This function must be called at the end of the program to
 * release memory used by am_sso_init, am_auth_init, and/or am_policy_init().
 *
 * This should be called only once.
 *
 * Any properties input parameter given to the init functions am_sso_init()
 * am_auth_init() or am_policy_init() should be destroyed only after
 * am_cleanup is called.
 *
 * Parameters:
 *   xmlmsg
 *		XML message containing the notification message.
 *
 *   policy_handle_t
 *              The policy handle created from am_policy_service_init().
 *
 *              NULL if policy is not initialized or not used.
 *
 * Returns:
 *   AM_SUCCESS
 *              if XML message was successfully parsed and processed.
 *
 *   AM_INVALID_ARGUMENT
 *		if any input parameter is invalid.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_cleanup(void);

AM_END_EXTERN_C

#endif	/* not AM_H */
