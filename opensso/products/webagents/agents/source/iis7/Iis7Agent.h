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
/*
 * Portions Copyrighted 2012 - 2013 ForgeRock AS
 */

#ifndef __IIS7AGENT_H__
#define __IIS7AGENT_H__

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <stdio.h>
#include <httpserv.h>
#include <string>
#include "am_web.h"

#define TCP_PORT_ASCII_SIZE_MAX 5
#define URL_SIZE_MAX (20*1024)

typedef struct OphResources {
    PCSTR cookies;
    DWORD cbCookies;
    am_policy_result_t result;
} tOphResources;

BOOL RegisterAgentModule();

am_status_t get_request_url(IHttpContext* pHttpContext, std::string& requestURL,
        std::string& origRequestURL, std::string& pathInfo, void* agent_config);

am_status_t GetVariable(IHttpContext* pHttpContext, PCSTR varName,
        PCSTR* pVarVal, DWORD* pVarValSize, BOOL isRequired);

BOOL loadAgentPropertyFile(IHttpContext* pHttpContext);

BOOL GetEntity(IHttpContext* pHttpContext, std::string& data);

static am_status_t set_cookie(const char *header, void **args);

static void set_method(void ** args, char * orig_req);

static am_status_t reset_cookie(const char *header, void **args);

static am_status_t set_header(const char *key, const char *values, void **args);

static am_status_t set_cookie_in_response(const char *header, void **args);

static am_status_t set_header_attr_as_cookie(const char *header, void **args);

static am_status_t get_cookie_sync(const char *cookieName, char** dpro_cookie, void **args);

am_status_t set_request_headers(IHttpContext *pHttpContext, void** args);

REQUEST_NOTIFICATION_STATUS redirect_to_request_url(IHttpContext* pHttpContext,
        const char *redirect_url, const char *set_cookies_list);

static am_status_t do_redirect(IHttpContext* pHttpContext, am_status_t status,
        am_policy_result_t *policy_result, const char *original_url,
        const char *method, void** args, void* agent_config);

am_status_t remove_key_in_headers(char* key, char** httpHeaders);

am_status_t set_headers_in_context(IHttpContext *pHttpContext,
        std::string headersList, BOOL isRequest);

void ConstructReqCookieValue(std::string& completeString, std::string value);

void do_deny(IHttpContext* pHttpContext);

void OphResourcesFree(tOphResources* pOphResources);

void TerminateAgent();

void init_at_request();

#endif
