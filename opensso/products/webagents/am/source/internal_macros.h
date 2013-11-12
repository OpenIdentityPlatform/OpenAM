/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: internal_macros.h,v 1.6 2008/09/13 01:11:53 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef INTERNAL_MACROS_H
#define INTERNAL_MACROS_H
#include <cstddef>
#include <cstdarg>
#include <math.h>
#include <string.h>

#define	PRIVATE_NAMESPACE_NAME	smi
#define	BEGIN_PRIVATE_NAMESPACE	namespace PRIVATE_NAMESPACE_NAME {
#define END_PRIVATE_NAMESPACE	}
#define USING_PRIVATE_NAMESPACE	using namespace PRIVATE_NAMESPACE_NAME;

#ifndef NULL
#ifdef __cplusplus
#define NULL    0
#else
#define NULL    ((void *)0)
#endif
#endif

extern const char *POLICY_SERVICE;
extern const char *POLICY_RESPONSE;
extern const char *RESOURCE_RESULT;
extern const char *SESSION_NOTIFICATION;
extern const char *AGENT_CONFIG_CHANGE_NOTIFICATION;
extern const char *ADD_LISTENER_RESPONSE;
extern const char *REMOVE_LISTENER_RESPONSE;
extern const char *SESSION;
extern const char *POLICY_CHANGE_NOTIFICATION;
extern const char *SUBJECT_CHANGE_NOTIFICATION;
extern const char *POLICY_DECISION;
extern const char *RESPONSE_ATTRIBUTES;
extern const char *ACTION_DECISION;
extern const char *ATTRIBUTE_VALUE_PAIR;
extern const char *RESPONSE_DECISIONS;
extern const char *SESSION_ID_ATTRIBUTE;
extern const char *SESSION_STATE_ATTRIBUTE;
extern const char *SESSION_STATE_VALUE_VALID;
extern const char *SESSION_NOTIF_TYPE;
extern const char *SESSION_NOTIF_TIME;
extern const char *SERVICE_NAME;
extern const char *RESOURCE_NAME;
extern const char *ATTRIBUTE_NAME;
extern const char *NAME;
extern const char *ATTRIBUTE;
extern const char *VALUE;
extern const char *TIME_TO_LIVE;
extern const char *ADVICES;
extern const char *NOTIFICATION_SET;
extern const char *NOTIFICATION;
extern const char *NOTIFICATION_TYPE;
extern const char *NOTIF_TYPE_MODIFIED;
extern const char *NOTIF_TYPE_ADDED;
extern const char *NOTIF_TYPE_DELETED;
extern const char *RESPONSE_SET;
extern const char *RESPONSE;
extern const char *VERSION;
extern const char *VERSION_STR;
extern const char *REVISION_STR;
extern const char *ADVICE_LIST_RESPONSE;
extern const char *REQUEST_ID;
extern const char *REQUEST_ID_STR;
extern const char *NOTIFICATION_ID;
extern const char *NOTIFICATION_SET_VERSION;
extern const char *REQUEST_SET_VERSION;
extern const char *QUERY_RESULT;
extern const char *GET_REQUIREMENTS;
extern const char *LOGIN_STATUS;
extern const char *AUTH_IDENTIFIER;
extern const char *EXCEPTION;
extern const char *STATUS;
extern const char *IN_PROGRESS;
extern const char *COMPLETED;
extern const char *SUCCESS;
extern const char *FAILED;
extern const char *SUBJECT;
extern const char *MESSAGE;
extern const char *TOKEN_ID;
extern const char *ERROR_CODE;
extern const char *TEMPLATE_NAME;
extern const char *CALLBACKS;
extern const char *LENGTH;
extern const char *NAME_CALLBACK;
extern const char *PASSWORD_CALLBACK;
extern const char *CHOICE_CALLBACK;
extern const char *CONFIRMATION_CALLBACK;
extern const char *TEXT_INPUT_CALLBACK;
extern const char *TEXT_OUTPUT_CALLBACK;
extern const char *LANGUAGE_CALLBACK;
extern const char *PAGE_PROPERTIES_CALLBACK;
extern const char *CUSTOM_CALLBACK;
extern const char *PROMPT;
extern const char *ECHO_PASSWORD;
extern const char *SERVER_HANDLED_ADVICES;

#define AM_COMMON_ORDINAL_NUMBER     AM_COMMON_PROPERTY_PREFIX "ordinal"

#define PUSH_BACK_CHAR(str, c) str.push_back(c)
#define MIN_URL_SIZE 8

enum NotificationType {
    NOTIFICATION_ADD = 0,
    NOTIFICATION_DELETE,
    NOTIFICATION_MODIFY
};

#ifdef _MSC_VER
#define stricmp _stricmp
#define strnicmp _strnicmp
#define strcasecmp stricmp
#define strncasecmp strnicmp
#define snprintf _snprintf
#define strdup _strdup
#define strtok_r strtok_s
#endif

#define DEFINE_BASE_INIT  \
void base_init(const PRIVATE_NAMESPACE_NAME::Properties &, boolean_t initializeLog)
extern "C" void encode_base64(const char *, std::size_t, char *);


#endif	// not INTERNAL_MACROS_H
