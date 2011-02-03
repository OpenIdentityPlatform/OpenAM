/*
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
 * $Id: Iis7Agent.h,v 1.3 2010/03/10 05:08:52 dknab Exp $
 *
 *
 */

#ifndef __IIS7AGENT_H__
#define __IIS7AGENT_H__

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <stdio.h>
#include <httpserv.h>
#include <string>
#include "am_web.h"

using namespace std;

#define TCP_PORT_ASCII_SIZE_MAX 5
#define URL_SIZE_MAX (20*1024)

typedef struct OphResources {
    PCSTR cookies;
    DWORD cbCookies;
    am_policy_result_t result;
} tOphResources;

typedef struct AgentConfig {
    BOOL bAgentInitSuccess; // For disabling IIS if init fails.
} tAgentConfig;

BOOL RegisterAgentModule();
REQUEST_NOTIFICATION_STATUS ProcessRequest(IHttpContext* pHttpContext, 
                                    IHttpEventProvider* pProvider);

am_status_t get_request_url(IHttpContext* pHttpContext, string& requestURL,
                            string& origRequestURL, string& pathInfo,
                            void* agent_config);

am_status_t GetVariable(IHttpContext* pHttpContext, PCSTR varName, 
                        PCSTR* pVarVal, DWORD* pVarValSize, BOOL isRequired); 

BOOL loadAgentPropertyFile(IHttpContext* pHttpContext);

BOOL iisaPropertiesFilePathGet(CHAR** propertiesFileFullPath, 
                string instanceId, BOOL isBootStrapFile);

void GetEntity(IHttpContext* pHttpContext, string& data);

static am_status_t set_cookie(const char *header, void **args);

static void set_method(void ** args, char * orig_req);

static am_status_t reset_cookie(const char *header, void **args);

static am_status_t set_header(const char *key, const char *values, void **args);

static am_status_t set_cookie_in_response(const char *header, void **args);

static am_status_t set_header_attr_as_cookie(const char *header, void **args);

static am_status_t get_cookie_sync(const char *cookieName, char** dpro_cookie, 
                                                    void **args);

am_status_t set_request_headers(IHttpContext *pHttpContext, void** args);

REQUEST_NOTIFICATION_STATUS redirect_to_request_url(IHttpContext* pHttpContext, 
                const char *redirect_url, const char *set_cookies_list);

static am_status_t do_redirect(IHttpContext* pHttpContext, am_status_t status, 
        am_policy_result_t *policy_result, const char *original_url, 
        const char *method, void** args, void* agent_config);

am_status_t remove_key_in_headers(char* key, char** httpHeaders);

am_status_t set_headers_in_context(IHttpContext *pHttpContext, 
                        string headersList, BOOL isRequest);

void ConstructReqCookieValue(string& completeString,string value);

void do_deny(IHttpContext* pHttpContext);

void logPrimitive(CHAR *message);

void OphResourcesFree(tOphResources* pOphResources);

void TerminateAgent();

void init_at_request();

// Agent error codes to return to IIS on failure via SetLastError() 
// See WINERROR.H for format.

// Error | Customer code flag
#define IISA_ERROR_BASE (3 << 30 | 1 << 29)

#define IISA_ERROR_GET_EXTENSION_VERSION    (IISA_ERROR_BASE | 1 << 15)

#define IISA_ERROR_PROPERTIES_FILE_PATH_GET (IISA_ERROR_GET_EXTENSION_VERSION | 1)
#define IISA_ERROR_INIT_POLICY              (IISA_ERROR_GET_EXTENSION_VERSION | 2)
#define IISA_ERROR_SEE_DEBUG_LOG            (IISA_ERROR_GET_EXTENSION_VERSION | 3)


#endif
