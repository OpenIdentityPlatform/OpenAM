/*
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
 * $Id: am.h,v 1.23 2010/01/26 00:54:46 dknab Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

/*
 * Abstract:
 *
 * General utility routines provided by the OpenAM 
 * Access Management library.
 *
 */

#ifndef AM_H
#define AM_H

#include <am_types.h>

AM_BEGIN_EXTERN_C

#define AM_COMMON_PROPERTY_PREFIX	"com.sun.identity.agents.config."
/*
#define AM_COMMON_PROPERTY_PREFIX	"com.sun.am."
#define	AM_POLICY_PROPERTY_PREFIX	"com.sun.am.policy."
#define AM_AUTH_PROPERTY_PREFIX         "com.sun.am.auth."
#define AM_SSO_PROPERTY_PREFIX          "com.sun.am.sso."
#define AM_LOG_PROPERTY_PREFIX          "com.sun.am.log."
*/

/* Common Properties */
#define	AM_COMMON_SSL_CERT_DIR_PROPERTY AM_COMMON_PROPERTY_PREFIX "sslcert.dir"
#define AM_COMMON_CERT_DB_PREFIX_PROPERTY AM_COMMON_PROPERTY_PREFIX "certdb.prefix"
#define AM_COMMON_CERT_DB_PASSWORD_PROPERTY AM_COMMON_PROPERTY_PREFIX "certdb.password"

#define AM_COMMON_TRUST_SERVER_CERTS_PROPERTY AM_COMMON_PROPERTY_PREFIX "trust.server.certs"
#define AM_COMMON_CERT_KEY_PROPERTY "com.forgerock.agents.config.cert.key"
#define AM_COMMON_CERT_KEY_PASSWORD_PROPERTY "com.forgerock.agents.config.cert.key.password"
#define AM_COMMON_CERT_FILE_PROPERTY "com.forgerock.agents.config.cert.file"
#define AM_COMMON_CERT_CA_FILE_PROPERTY "com.forgerock.agents.config.cert.ca.file"
#define AM_COMMON_CIPHERS_PROPERTY "com.forgerock.agents.config.ciphers"

#define	AM_COMMON_COOKIE_NAME_PROPERTY	AM_COMMON_PROPERTY_PREFIX "cookie.name"
#define	AM_COMMON_COOKIE_SECURE_PROPERTY	AM_COMMON_PROPERTY_PREFIX "cookie.secure"
#define	AM_COMMON_NAMING_URL_PROPERTY	AM_COMMON_PROPERTY_PREFIX "naming.url"
#define AM_COMMON_NOTIFICATION_ENABLE_PROPERTY AM_COMMON_PROPERTY_PREFIX "notification.enable"
#define AM_COMMON_NOTIFICATION_URL_PROPERTY	"com.sun.identity.client.notification.url"
#define AM_COMMON_LOADBALANCE_PROPERTY    AM_COMMON_PROPERTY_PREFIX "load.balancer.enable"
#define AM_COMMON_RECEIVE_TIMEOUT_PROPERTY AM_COMMON_PROPERTY_PREFIX "receive.timeout"
#define AM_COMMON_TCP_NODELAY_ENABLE_PROPERTY AM_COMMON_PROPERTY_PREFIX "tcp.nodelay.enable"
#define AM_COMMON_CONNECT_TIMEOUT_PROPERTY AM_COMMON_PROPERTY_PREFIX "connect.timeout"
#define AM_COMMON_IGNORE_SERVER_CHECK    AM_COMMON_PROPERTY_PREFIX "ignore.server.check"
#define AM_COMMON_POLL_PRIMARY_SERVER AM_COMMON_PROPERTY_PREFIX "poll.primary.server"

/* Authentication Properties */
#define AM_AUTH_CERT_ALIAS_PROPERTY AM_COMMON_PROPERTY_PREFIX "certificate.alias"


/* Policy Properties */
#define AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY AM_COMMON_PROPERTY_PREFIX "url.comparison.case.ignore"
#define AM_POLICY_HASH_TIMEOUT_MINS_PROPERTY AM_COMMON_PROPERTY_PREFIX "policy.cache.polling.interval"
#define AM_POLICY_PASSWORD_PROPERTY		AM_COMMON_PROPERTY_PREFIX "password"
#define AM_POLICY_USER_NAME_PROPERTY	AM_COMMON_PROPERTY_PREFIX "username"
#define AM_POLICY_KEY_PROPERTY	AM_COMMON_PROPERTY_PREFIX "key"
#define AM_POLICY_ORG_NAME_PROPERTY	AM_COMMON_PROPERTY_PREFIX "organization.name"
#define AM_POLICY_MODULE_NAME_PROPERTY	AM_COMMON_PROPERTY_PREFIX "auth.module"
#define AM_POLICY_USER_ID_PARAM_PROPERTY AM_COMMON_PROPERTY_PREFIX "userid.param"
#define AM_POLICY_USER_ID_PARAM_TYPE_PROPERTY AM_COMMON_PROPERTY_PREFIX "userid.param.type"
#define AM_POLICY_FETCH_FROM_ROOT_RSRC_PROPERTY      AM_COMMON_PROPERTY_PREFIX "fetch.from.root.resource"


#define AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY AM_COMMON_PROPERTY_PREFIX "sso.cache.polling.interval"

#define	AM_COMMON_IGNORE_PREFERRED_NAMING_URL_PROPERTY	AM_COMMON_PROPERTY_PREFIX "ignore.preferred.naming.url"

/* Proxy Properties */
#define AM_COMMON_FORWARD_PROXY_HOST AM_COMMON_PROPERTY_PREFIX "forward.proxy.host"
#define AM_COMMON_FORWARD_PROXY_PORT AM_COMMON_PROPERTY_PREFIX "forward.proxy.port"
#define AM_COMMON_FORWARD_PROXY_USER AM_COMMON_PROPERTY_PREFIX "forward.proxy.user"
#define AM_COMMON_FORWARD_PROXY_PASSWORD AM_COMMON_PROPERTY_PREFIX "forward.proxy.password"

#define AM_COMMON_CONFIG_CHANGE_NOTIFICATION_ENABLE_PROPERTY AM_COMMON_PROPERTY_PREFIX "change.notification.enable"

/* Following are audit log related properties */
#define AM_AUDIT_LOCAL_LOG_FILE_PROPERTY         AM_COMMON_PROPERTY_PREFIX "local.logfile"
#define AM_AUDIT_LOCAL_LOG_ROTATE_PROPERTY     AM_COMMON_PROPERTY_PREFIX "local.log.rotate"
#define AM_AUDIT_LOCAL_LOG_FILE_SIZE_PROPERTY    AM_COMMON_PROPERTY_PREFIX "local.log.size"
#define AM_AUDIT_SERVER_LOG_FILE_PROPERTY   AM_COMMON_PROPERTY_PREFIX "remote.logfile"
#define AM_AUDIT_SERVER_LOG_INTERVAL_PROPERTY   AM_COMMON_PROPERTY_PREFIX "remote.log.interval"
#define AM_AUDIT_ACCESS_TYPE_PROPERTY  AM_COMMON_PROPERTY_PREFIX "audit.accesstype"
#define AM_AUDIT_DISPOSITION_PROPERTY   AM_COMMON_PROPERTY_PREFIX "log.disposition"

/* Debug Properties */
#define AM_AGENT_DEBUG_LEVEL_PROPERTY	AM_COMMON_PROPERTY_PREFIX "debug.level"
#define AM_AGENT_DEBUG_FILE_PROPERTY	AM_COMMON_PROPERTY_PREFIX "debug.file"
#define AM_AGENT_DEBUG_FILE_ROTATE_PROPERTY	AM_COMMON_PROPERTY_PREFIX "debug.file.rotate"
#define AM_AGENT_DEBUG_FILE_SIZE_PROPERTY	AM_COMMON_PROPERTY_PREFIX "debug.file.size"

/* shared agent profile name */
#define AM_AGENT_PROFILE_NAME_PROPERTY	AM_COMMON_PROPERTY_PREFIX "profilename"

/*
* Agent debug File minimum size
*/
#define DEBUG_FILE_MIN_SIZE 1048576 /* 1 MB */
#define DEBUG_FILE_DEFAULT_SIZE 10485760 /* 10 MB */

/*
* Audit Disposition
*/
#define AUDIT_DISPOSITION_ALL "ALL"
#define AUDIT_DISPOSITION_LOCAL "LOCAL"
#define AUDIT_DISPOSITION_REMOTE "REMOTE"
#define LOCAL_AUDIT_FILE_DEFAULT_SIZE 52428800 /* 5 MB */

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

AM_EXPORT am_status_t
am_shutdown_nss(void); 

AM_END_EXTERN_C

#endif	/* not AM_H */
