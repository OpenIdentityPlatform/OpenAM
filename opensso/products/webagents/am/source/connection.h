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
 * $Id: connection.h,v 1.5 2010/03/10 05:09:38 dknab Exp $
 *
 *
 * Abstract:
 *
 * This class encapsulates a TCP socket connection.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef CONNECTION_H
#define CONNECTION_H

#include <cstdlib>
#include <string>
#include <map>

#ifdef _MSC_VER
#include <windows.h>
#include <winhttp.h>
#include <wincrypt.h>
#endif

#include <am_types.h>
#include "internal_macros.h"
#include "nspr_exception.h"
#include "log.h"
#include "properties.h"
#include "server_info.h"

#define NETWORK_TIMEOUT 4000 //msec

#if !defined(_MSC_VER) && !defined(__sun)
extern "C" void libiconv_close();
#endif

BEGIN_PRIVATE_NAMESPACE

#ifdef _MSC_VER

class Connection {
public:

    typedef std::map<std::string, std::string> ConnHeaderMap;
    typedef std::pair<std::string, std::string> ConnHeaderMapValue;

    typedef enum {
        GET = 0,
        POST,
        HEAD
    } REQUEST_TYPE;

    typedef enum {
        NO_AUTH = 0,
        CERT_AUTH
    } AUTH_TYPE;

    typedef struct {
        HINTERNET hRequest;
        DWORD dwSize;
        DWORD dwStatusCode;
    } REQUEST_CONTEXT_INT;

    typedef struct {
        HINTERNET hSession;
        HINTERNET hConnect;
        LPWSTR lpUrlPath;
        DWORD dwReqFlag;
        DWORD dwSecFlag;
        REQUEST_CONTEXT_INT *lpRequest;
        AUTH_TYPE tokenType;
        PCCERT_CONTEXT pCertContext;
        HCERTSTORE pfxStore;
    } REQUEST_CONTEXT;

    Connection(const ServerInfo& server);

    ~Connection();

    static am_status_t initialize(const Properties& properties);

    static am_status_t initialize_in_child_process(const Properties& properties);

    static am_status_t shutdown();

    static am_status_t shutdown_in_child_process();

    am_status_t sendRequest(const char *type, std::string& uri, ConnHeaderMap& hdrs, std::string& data);

    int httpContentLength() {
        if (context) {
            REQUEST_CONTEXT_INT *r = context->lpRequest;
            if (r) {
                return r->dwSize;
            }
        }
        return -1;
    }

    int httpStatusCode() {
        if (context) {
            REQUEST_CONTEXT_INT *r = context->lpRequest;
            if (r) {
                return r->dwStatusCode;
            }
        }
        return -1;
    }

    std::string& getBody() {
        return dataBuffer;
    }

    ConnHeaderMap::iterator begin() {
        return headers.begin();
    }

    ConnHeaderMap::iterator end() {
        return headers.end();
    }

    ConnHeaderMap::const_iterator begin() const {
        return headers.begin();
    }

    ConnHeaderMap::const_iterator end() const {
        return headers.end();
    }


private:

    Connection(const Connection&);
    Connection& operator=(const Connection&);

    static unsigned long timeout;
    REQUEST_CONTEXT *context;
    ConnHeaderMap headers;
    std::string dataBuffer;

    static std::string proxyHost;
    static std::string proxyUser;
    static std::string proxyPassword;
    static std::string proxyPort;
    static bool trustServerCerts;
    static std::string cipherList;
    static std::string keyName;

    void http_close();
    BOOL request(REQUEST_TYPE type, std::wstring& urlpath, ConnHeaderMap& hdrs, std::string& post);
    BOOL response(REQUEST_TYPE type);

    void log_error(const char *head, DWORD errCode) {
        LPSTR errString = NULL;
        DWORD size = 0;
        if ((size = FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER |
                FORMAT_MESSAGE_FROM_SYSTEM, 0, errCode, 0, (LPSTR) & errString, 0, 0)) == 0) {
            Log::log(Log::ALL_MODULES, Log::LOG_WARNING,
                    "%s Unknown error code (%X)", head, errCode);
        } else {
            char *p = strchr(errString, '\r');
            if (p != NULL) *p = '\0';
            if (errCode == 0) {
                Log::log(Log::ALL_MODULES, Log::LOG_DEBUG,
                        "%s %s", head, errString);
            } else {
                Log::log(Log::ALL_MODULES, Log::LOG_WARNING,
                        "%s %s (%X)", head, errString, errCode);
            }
            LocalFree(errString);
        }
    }
};

#else

class Connection {
public:

    typedef std::map<std::string, std::string> ConnHeaderMap;
    typedef std::pair<std::string, std::string> ConnHeaderMapValue;
    
    Connection(const ServerInfo& server);

    ~Connection();

    static am_status_t initialize(const Properties& properties);

    static am_status_t initialize_in_child_process(const Properties& properties);

    static am_status_t shutdown();

    static am_status_t shutdown_in_child_process();

    am_status_t sendRequest(const char *type, std::string& uri, ConnHeaderMap& hdrs, std::string& data);

    int httpContentLength() {
        return dataLength;
    }

    int httpStatusCode() {
        return statusCode;
    }

    std::string& getBody() {
        return dataBuffer;
    }

    ConnHeaderMap::iterator begin() {
        return headers.begin();
    }

    ConnHeaderMap::iterator end() {
        return headers.end();
    }

    ConnHeaderMap::const_iterator begin() const {
        return headers.begin();
    }

    ConnHeaderMap::const_iterator end() const {
        return headers.end();
    }


private:
    static unsigned long timeout;
    int sock;
    int statusCode;
    int dataLength;
    ConnHeaderMap headers;
    std::string dataBuffer;
    ServerInfo server;
    void *ssl;

    static std::string proxyHost;
    static std::string proxyUser;
    static std::string proxyPassword;
    static std::string proxyPort;
    static bool trustServerCerts;
    static std::string cipherList;
    static std::string keyFile;
    static std::string keyPassword;
    static std::string caFile;
    static std::string certFile;

    void http_close();

    ssize_t response(char **);
    ssize_t request(const char *buff, const size_t len);

    void net_error(int n) {
#ifdef __sun
        size_t size = 1024;
        char *s = (char *) malloc(size);
        if (s == NULL) return;
        while (strerror_r(n, s, size) == -1 && errno == ERANGE) {
            size *= 2;
            s = (char *) realloc(s, size);
            if (s == NULL) return;
        }
        if (s != NULL) {
            Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Connection(): %s", s);
            free(s);
        }
#else
        Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Connection(): code %d", n);
#endif
    }

};

#endif

END_PRIVATE_NAMESPACE

#endif	/* not CONNECTION_H */
