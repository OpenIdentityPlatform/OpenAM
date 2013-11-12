/* -*- Mode: C -*- */
/*
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
 * $Id: am_types.h,v 1.4 2008/08/19 19:11:38 veiming Exp $
 */

/*
 * Abstract:
 *
 * Common types and macros provided by the OpenSSO 
 * Access Management SDK.
 *
 */

#ifndef AM_TYPES_H
#define AM_TYPES_H

#if	defined(WINNT)
#if	defined(AM_BUILDING_LIB)
#define	AM_EXPORT	__declspec(dllexport)
#else
#if	!defined(AM_STATIC_LIB)
#define	AM_EXPORT	__declspec(dllimport)
#else
#if	!defined(__cplusplus)
#define	AM_EXPORT	extern
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

#if defined(WINNT) || defined(LINUX) || defined(HPUX)
#include <sys/stat.h>     /* for time_t */
typedef enum { 
    B_FALSE, 
    B_TRUE 
} boolean_t;
#else 
#include <sys/types.h>   /* for time_t and boolean_t */
#endif /* WINNT */

#if defined(AIX)
typedef enum {
    B_FALSE=0,
    B_TRUE
} booleant;
#endif


typedef enum {
    AM_FALSE = 0,
    AM_TRUE
} am_bool_t;

typedef enum {
    AM_SUCCESS = 0,
    AM_FAILURE,
    AM_INIT_FAILURE,
    AM_AUTH_FAILURE,
    AM_NAMING_FAILURE,
    AM_SESSION_FAILURE,
    AM_POLICY_FAILURE,
    AM_NO_POLICY,
    AM_INVALID_ARGUMENT,
    AM_INVALID_VALUE,
    AM_NOT_FOUND,
    AM_NO_MEMORY,
    AM_NSPR_ERROR,
    AM_END_OF_FILE,
    AM_BUFFER_TOO_SMALL,
    AM_NO_SUCH_SERVICE_TYPE,
    AM_SERVICE_NOT_AVAILABLE,
    AM_ERROR_PARSING_XML,
    AM_INVALID_SESSION,
    AM_INVALID_ACTION_TYPE,
    AM_ACCESS_DENIED,
    AM_HTTP_ERROR,
    AM_INVALID_FQDN_ACCESS,
    AM_FEATURE_UNSUPPORTED,
    AM_AUTH_CTX_INIT_FAILURE,
    AM_SERVICE_NOT_INITIALIZED,
    AM_INVALID_RESOURCE_FORMAT,
    AM_NOTIF_NOT_ENABLED,
    AM_ERROR_DISPATCH_LISTENER,
    AM_REMOTE_LOG_FAILURE,
    AM_LOG_FAILURE,
    AM_REMOTE_LOG_NOT_INITIALIZED,
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

AM_END_EXTERN_C

#endif	/* not AM_TYPES_H */
