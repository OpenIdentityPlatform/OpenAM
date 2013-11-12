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
 * $Id: am_types.h,v 1.12 2009/10/28 21:56:20 subbae Exp $
 *
 * Abstract:
 *
 * Common types and macros provided by the OpenAM 
 * Access Management SDK.
 *
 */
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */

#ifndef AM_TYPES_H
#define AM_TYPES_H

#if defined(_MSC_VER)
#if defined(AM_BUILDING_LIB)
#define	AM_EXPORT __declspec(dllexport)
#else
#if !defined(AM_STATIC_LIB)
#define	AM_EXPORT __declspec(dllimport)
#else
#if !defined(__cplusplus)
#define	AM_EXPORT extern
#endif
#endif
#endif
#else
#define	AM_EXPORT
#endif

#if	defined(__cplusplus)
#define	AM_BEGIN_EXTERN_C	extern "C" {
#define	AM_END_EXTERN_C		}
#else
#define	AM_BEGIN_EXTERN_C
#define	AM_END_EXTERN_C
#endif

AM_BEGIN_EXTERN_C

#if defined(_MSC_VER) || defined(LINUX) || defined(HPUX)
#include <sys/stat.h>     /* for time_t */
typedef enum { 
    B_FALSE = 0, 
    B_TRUE 
} boolean_t;
#else 
#include <sys/types.h>   /* for time_t and boolean_t */
#endif

#if defined(AIX)
typedef enum {
    B_FALSE = 0,
    B_TRUE
} booleant;
#endif

#define AM_NAMING_LOCK ".am_naming_lock"

typedef struct {
    unsigned long ping_interval;
    unsigned long ping_ok_count;
    unsigned long ping_fail_count;
    int instance_id;
    int default_set_size;
    int *default_set;
    int url_size;
    char **url_list;
    void (*log)(const char *, ...);
    void (*debug)(const char *, ...);
    int (*validate)(const char *, const char **, int *httpcode);
} naming_validator_t;

typedef enum {
    AM_FALSE = 0,
    AM_TRUE
} am_bool_t;

typedef enum {
    AM_SUCCESS = 0, /* error code 0 */
    AM_FAILURE, /* error code 1 */
    AM_INIT_FAILURE, /* error code 2 */
    AM_AUTH_FAILURE, /* error code 3 */
    AM_NAMING_FAILURE, /* error code 4 */
    AM_SESSION_FAILURE, /* error code 5 */
    AM_POLICY_FAILURE, /* error code 6 */
    AM_NO_POLICY, /* error code 7 */
    AM_INVALID_ARGUMENT, /* error code 8 */
    AM_INVALID_VALUE, /* error code 9 */
    AM_NOT_FOUND, /* error code 10 */
    AM_NO_MEMORY, /* error code 11 */
    AM_NSPR_ERROR, /* error code 12 */
    AM_END_OF_FILE, /* error code 13 */
    AM_BUFFER_TOO_SMALL, /* error code 14 */
    AM_NO_SUCH_SERVICE_TYPE, /* error code 15 */
    AM_SERVICE_NOT_AVAILABLE, /* error code 16 */
    AM_ERROR_PARSING_XML, /* error code 17 */
    AM_INVALID_SESSION, /* error code 18 */
    AM_INVALID_ACTION_TYPE, /* error code 19 */
    AM_ACCESS_DENIED, /* error code 20 */
    AM_HTTP_ERROR, /* error code 21 */
    AM_INVALID_FQDN_ACCESS, /* error code 22 */
    AM_FEATURE_UNSUPPORTED, /* error code 23 */
    AM_AUTH_CTX_INIT_FAILURE, /* error code 24 */
    AM_SERVICE_NOT_INITIALIZED, /* error code 25 */
    AM_INVALID_RESOURCE_FORMAT, /* error code 26 */
    AM_NOTIF_NOT_ENABLED, /* error code 27 */
    AM_ERROR_DISPATCH_LISTENER, /* error code 28 */
    AM_REMOTE_LOG_FAILURE, /* error code 29 */
    AM_LOG_FAILURE, /* error code 30 */
    AM_REMOTE_LOG_NOT_INITIALIZED, /* error code 31 */
    AM_REST_ATTRS_SERVICE_FAILURE, /* error code 32 */
    AM_REST_SERVICE_NOT_AVAILABLE, /* error code 33 */
    AM_REPOSITORY_TYPE_INVALID, /* error code 34, valid values: local, centralized */
    AM_INVALID_APP_SSOTOKEN,
    AM_REDIRECT_LOGOUT, /* error code 36, used for redirecting to logout page*/
    AM_AGENT_TIME_NOT_SYNC, /* error code 37, indicate time synchronization issue between AM and Agent m/cs*/
    AM_NUM_ERROR_CODES	/* This should always be the last. */
} am_status_t;

/*
 * Returns the message for the given status code.
 * For example, the message for AM_SUCCESS is "success".
 *
 * Parameters:
 *   status     the status code.
 *
 * Returns:
 *   Message for the status code as a const char *.
 */
AM_EXPORT const char *am_status_to_string(am_status_t status);

/*
 * Returns the name of the given status code as a string.
 * For example, the name of AM_SUCCESS is "AM_SUCCCESS".
 *
 * Parameters:
 *   status     the status code.
 *
 * Returns:
 *   Name of the status code as a const char *.
 */
AM_EXPORT const char *am_status_to_name(am_status_t status);

AM_EXPORT int am_instance_id(const char *);

AM_END_EXTERN_C

#endif	/* not AM_TYPES_H */
