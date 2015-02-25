/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

#include <ws2tcpip.h>
#include <stdexcept>
#include "am.h"
#include "connection.h"
#include "log.h"
#include "string_util.h"
#include "version.h"

static std::wstring utf8_decode(const std::string& s) {
    int len = 0;
    int slength = (int) s.length() + 1;
    len = MultiByteToWideChar(CP_ACP, 0, s.c_str(), slength, 0, 0);
    std::wstring r(len, L'\0');
    MultiByteToWideChar(CP_ACP, 0, s.c_str(), slength, &r[0], len);
    return r;
}

static std::string utf8_encode(const std::wstring& s) {
    int len = 0;
    int slength = (int) s.length() + 1;
    len = WideCharToMultiByte(CP_ACP, 0, s.c_str(), slength, 0, 0, 0, 0);
    std::string r(len, '\0');
    WideCharToMultiByte(CP_ACP, 0, s.c_str(), slength, &r[0], len, 0, 0);
    return r;
}

USING_PRIVATE_NAMESPACE

        unsigned long Connection::timeout = NETWORK_TIMEOUT;
std::string Connection::proxyHost = "";
std::string Connection::proxyUser = "";
std::string Connection::proxyPassword = "";
std::string Connection::proxyPort = "";
std::string Connection::cipherList = "";
bool Connection::trustServerCerts = true;
std::string Connection::keyName = "";

Connection::Connection(const char *host, unsigned int port, bool usessl = false) : context(NULL), useSSL(usessl), dataBuffer(NULL) {

    char ccName[256];
    std::wstring whost(utf8_decode(std::string(host)));
    std::wstring agent(utf8_decode(std::string(Version::getAgentVersion())));

    agent = L"OpenAM Web Agent/" + agent;

    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
            "Connection::Connection(): connecting to: %s:%d", host, port);

    if ((context = (REQUEST_CONTEXT *) malloc(sizeof (REQUEST_CONTEXT))) != NULL) {
        ZeroMemory(context, sizeof (REQUEST_CONTEXT));
        context->tokenType = NO_AUTH;
        context->hSession = WinHttpOpen(agent.c_str(), WINHTTP_ACCESS_TYPE_DEFAULT_PROXY,
                WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
        if (context->hSession) {

            if (timeout > 0) {
                if (!WinHttpSetTimeouts(context->hSession, timeout, timeout, timeout, timeout)) {
                    log_error("Connection::Connection(): timeout set failed (%d)", GetLastError());
                }
            } else {
                if (!WinHttpSetTimeouts(context->hSession, NETWORK_TIMEOUT, NETWORK_TIMEOUT, NETWORK_TIMEOUT, NETWORK_TIMEOUT)) {
                    log_error("Connection::Connection(): default timeout set failed (%d)", GetLastError());
                }
            }

            if ((context->hConnect = WinHttpConnect(context->hSession, whost.c_str(), port, 0))) {
                context->dwReqFlag = usessl ? WINHTTP_FLAG_SECURE | WINHTTP_FLAG_REFRESH : WINHTTP_FLAG_REFRESH;
                context->dwSecFlag = (usessl && trustServerCerts) ?
                        SECURITY_FLAG_IGNORE_CERT_CN_INVALID | SECURITY_FLAG_IGNORE_CERT_DATE_INVALID
                        | SECURITY_FLAG_IGNORE_UNKNOWN_CA | SECURITY_FLAG_IGNORE_CERT_WRONG_USAGE : 0;

                if (context->dwSecFlag > 0) {
                    DWORD protocols = WINHTTP_FLAG_SECURE_PROTOCOL_SSL3 | WINHTTP_FLAG_SECURE_PROTOCOL_TLS1 |
                            WINHTTP_FLAG_SECURE_PROTOCOL_TLS1_1 | WINHTTP_FLAG_SECURE_PROTOCOL_TLS1_2;
                    WinHttpSetOption(context->hSession, WINHTTP_OPTION_SECURE_PROTOCOLS,
                            &protocols, sizeof (protocols));
                }

                if (context->dwSecFlag > 0 && keyName.length() > 0) {
                    context->pfxStore = CertOpenStore(CERT_STORE_PROV_SYSTEM,
                            X509_ASN_ENCODING | PKCS_7_ASN_ENCODING, 0, CERT_SYSTEM_STORE_LOCAL_MACHINE, L"My");
                    if (context->pfxStore) {
                        context->pCertContext = CertFindCertificateInStore(context->pfxStore, X509_ASN_ENCODING | PKCS_7_ASN_ENCODING,
                                0, CERT_FIND_SUBJECT_STR, keyName.c_str(), NULL);
                        if (context->pCertContext) {
                            if (CertGetNameStringA(context->pCertContext, CERT_NAME_SIMPLE_DISPLAY_TYPE, 0, NULL, ccName, 128)) {
                                Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                                        "Connection::Connection(): found certificate: \"%s\", nickname: %s", ccName, keyName.c_str());
                                context->tokenType = CERT_AUTH;
                            }
                        } else {
                            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                                    "Connection::Connection(): failed to locate certificate in local machine store (nickname: %s)", keyName.c_str());
                        }
                    }
                }
                return;
            } else {
                log_error("Connection::Connection(): http connect failed:", GetLastError());
            }
        } else {
            log_error("Connection::Connection(): http connection open failed:", GetLastError());
        }
    }
    http_close();
}

void Connection::http_close() {
    if (context != NULL) {
        if (context->lpRequest != NULL) {
            REQUEST_CONTEXT_INT *r = context->lpRequest;
            if (r != NULL) {
                if (r->hRequest != NULL) {
                    WinHttpSetStatusCallback(r->hRequest, NULL, 0, (DWORD_PTR) NULL);
                    WinHttpCloseHandle(r->hRequest);
                    r->hRequest = NULL;
                }
                free(r);
                r = NULL;
            }
            context->lpRequest = NULL;
        }
        if (context->hConnect != NULL) {
            WinHttpCloseHandle(context->hConnect);
            context->hConnect = NULL;
        }
        if (context->hSession != NULL) {
            WinHttpCloseHandle(context->hSession);
            context->hSession = NULL;
        }
        if (context->pCertContext != NULL) {
            CertFreeCertificateContext(context->pCertContext);
            context->pCertContext = NULL;
        }
        if (context->pfxStore != NULL) {
            CertCloseStore(context->pfxStore, 0);
            context->pfxStore = NULL;
        }
        free(context);
        context = NULL;
    }
    if (dataBuffer != NULL) {
        free(dataBuffer);
    }
    dataBuffer = NULL;
}

BOOL Connection::request(REQUEST_TYPE type, std::wstring& urlpath, ConnHeaderMap& hdrs, std::string& post) {
    REQUEST_CONTEXT_INT *ctxi = NULL;
    DWORD len = (DWORD) post.length();
    LPWSTR method = NULL;

    switch (type) {
        case GET:method = L"GET";
            break;
        case POST:method = L"POST";
            break;
        case HEAD:method = L"HEAD";
            break;
        default:
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Connection::request(): unknown http request type (%d)", type);
            return FALSE;
    }

    if (context != NULL && (ctxi = (REQUEST_CONTEXT_INT *) malloc(sizeof (REQUEST_CONTEXT_INT))) != NULL) {
        ZeroMemory(ctxi, sizeof (REQUEST_CONTEXT_INT));
        context->lpRequest = ctxi;
        ctxi->hRequest = WinHttpOpenRequest(context->hConnect,
                method, (urlpath.empty() ? L"/" : urlpath.c_str()),
                NULL, WINHTTP_NO_REFERER, WINHTTP_DEFAULT_ACCEPT_TYPES,
                context->dwReqFlag);
        if (ctxi->hRequest) {

            /* setup proxy */
            if (!proxyHost.empty() && proxyPort.empty()) {
                WINHTTP_PROXY_INFO pi = {0};
                std::wstring proxy(utf8_decode(proxyHost + ":" + proxyPort));
                pi.dwAccessType = WINHTTP_ACCESS_TYPE_NAMED_PROXY;
                pi.lpszProxy = const_cast<LPWSTR> (proxy.c_str());
                WinHttpSetOption(ctxi->hRequest, WINHTTP_OPTION_PROXY, &pi, sizeof (pi));
                if (!proxyUser.empty()) {
                    std::wstring user(utf8_decode(proxyUser));
                    WinHttpSetOption(ctxi->hRequest, WINHTTP_OPTION_PROXY_USERNAME,
                            (LPVOID) user.c_str(), (DWORD) user.size() * sizeof (wchar_t));
                    if (!proxyPassword.empty()) {
                        std::wstring pass(utf8_decode(proxyPassword));
                        WinHttpSetOption(ctxi->hRequest, WINHTTP_OPTION_PROXY_PASSWORD,
                                (LPVOID) pass.c_str(), (DWORD) pass.size() * sizeof (wchar_t));
                    }
                }
            }

            /* setup ssl mutual cert auth */
            switch (context->tokenType) {
                case CERT_AUTH:
                    WinHttpSetOption(ctxi->hRequest, WINHTTP_OPTION_CLIENT_CERT_CONTEXT,
                            (LPVOID) context->pCertContext, sizeof (CERT_CONTEXT));
                    break;
            }

            /* add request headers */
            ConnHeaderMap::iterator it = hdrs.begin();
            ConnHeaderMap::iterator itEnd = hdrs.end();
            for (; it != itEnd; ++it) {
                std::wstring kv(utf8_decode((*it).first));
                Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                        "Connection::request(): Header: %s", ((*it).first).c_str());
                if (!WinHttpAddRequestHeaders(ctxi->hRequest, kv.c_str(), (DWORD) - 1,
                        WINHTTP_ADDREQ_FLAG_ADD | WINHTTP_ADDREQ_FLAG_REPLACE)) {
                    log_error("Connection::request(): http add request header failed:", GetLastError());
                }
            }

            switch (type) {
                case HEAD:
                case GET:
                    if (context->dwSecFlag > 0)
                        WinHttpSetOption(ctxi->hRequest, WINHTTP_OPTION_SECURITY_FLAGS, &(context->dwSecFlag), sizeof (DWORD));
                    if (WinHttpSendRequest(ctxi->hRequest, WINHTTP_NO_ADDITIONAL_HEADERS, 0,
                            WINHTTP_NO_REQUEST_DATA, 0, 0, (DWORD_PTR) 0)) {
                        return response(type);
                    } else {
                        log_error("Connection::request(): http get request failed:", GetLastError());
                    }
                    break;
                case POST:
                    if (context->dwSecFlag > 0)
                        WinHttpSetOption(ctxi->hRequest, WINHTTP_OPTION_SECURITY_FLAGS, &(context->dwSecFlag), sizeof (DWORD));
                    if (WinHttpSendRequest(ctxi->hRequest, WINHTTP_NO_ADDITIONAL_HEADERS, 0,
                            (LPVOID) post.c_str(), len, len, (DWORD_PTR) 0)) {
                        return response(type);
                    } else {
                        log_error("Connection::request(): http post request failed:", GetLastError());
                    }
                    break;
            }
        } else {
            log_error("Connection::request(): http open request failed:", GetLastError());
        }
    } else {
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                "Connection::request(): memory allocation failure");
    }
    return FALSE;
}

BOOL Connection::response(REQUEST_TYPE rtype) {
    REQUEST_CONTEXT_INT *ctx = NULL;
    DWORD readCount = 0, downloadSize = 0;
    if (context && (ctx = context->lpRequest)) {
        if (WinHttpReceiveResponse(ctx->hRequest, NULL) == TRUE) {

            DWORD len = 0;
            WinHttpQueryHeaders(ctx->hRequest, WINHTTP_QUERY_RAW_HEADERS, WINHTTP_HEADER_NAME_BY_INDEX,
                    WINHTTP_NO_OUTPUT_BUFFER, &len, WINHTTP_NO_HEADER_INDEX);
            std::wstring headerData;
            headerData.resize(len / sizeof (std::wstring::value_type) + 1);
            WinHttpQueryHeaders(ctx->hRequest, WINHTTP_QUERY_RAW_HEADERS, WINHTTP_HEADER_NAME_BY_INDEX,
                    &headerData[0], &len, WINHTTP_NO_HEADER_INDEX);

            ctx->dwStatusCode = wcstol(&headerData[headerData.find(L' ') + 1], NULL, 10);
            headerData.erase(0, headerData.find(L'\0') + 1);

            Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                    "Connection::response(): HTTP %d", ctx->dwStatusCode);

            while (headerData[0]) {
                std::wstring entry(&headerData[0]);
                headerData.erase(0, entry.size() + 1);
                std::wstring::size_type n = entry.find_first_of(L':');
                if (n == std::wstring::npos) continue;
                std::string h(utf8_encode(entry.substr(0, n)));
                std::string v(utf8_encode(entry.substr(n + 2)));
                headers.insert(ConnHeaderMapValue(h, v));

                Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                        "Connection::response(): Header: %s: %s", h.c_str(), v.c_str());
            }

            std::auto_ptr<char> outBuffer;
            bool error = false;
            std::string dataBufferTmp;

            do {
                downloadSize = 0;
                if (!WinHttpQueryDataAvailable(ctx->hRequest, &downloadSize)) {
                    error = true;
                    break;
                }

                if (downloadSize == 0)
                    break;

                outBuffer.reset(new char[downloadSize + 1]);
                if (!outBuffer.get()) {
                    error = true;
                    break;
                } else {
                    ZeroMemory(outBuffer.get(), downloadSize + 1);
                    if (WinHttpReadData(ctx->hRequest, outBuffer.get(), downloadSize, &readCount)) {
                        dataBufferTmp.append(outBuffer.get(), readCount);
                        ctx->dwSize += readCount;
                    }

                }
            } while (downloadSize > 0);

            DWORD err = GetLastError();

            if (!error) {
                if (dataBufferTmp.size() > 0) {
                    dataBuffer = (char *) malloc(dataBufferTmp.size() + 1);
                    if (dataBuffer) {
                        memcpy(dataBuffer, dataBufferTmp.c_str(), dataBufferTmp.size());
                        dataBuffer[dataBufferTmp.size()] = 0;
                    }
                    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                            "Connection::response():\n%s", dataBufferTmp.c_str());
                    log_error("Connection::response():", err);
                    return TRUE;
                } else {
                    ctx->dwSize = 0;
                }
            }

            log_error("Connection::response():", err);

            if (!error && rtype == HEAD) {
                return TRUE;
            }

        } else {
            log_error("Connection::response(): http receive request failed:", GetLastError());
        }
    }
    return FALSE;
}

Connection::~Connection() {
    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
            "Connection::http_close(): cleaning up");
    http_close();
}

am_status_t Connection::sendRequest(const char *type, std::string& uri, ConnHeaderMap& hdrs, std::string& data) {
    BOOL status = FALSE;
    if (type != NULL) {
        std::wstring urlpath(uri.empty() ? L"" : utf8_decode(uri));
        if (strstr(type, "POST") != NULL) {
            Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::sendRequest(): sending POST request: "
                    "uri: %s, data length: %d", uri.c_str(), data.length());
            if (!data.empty()) {
                Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::sendRequest(): POST data:\n%s", data.c_str());
            }
            status = request(POST, urlpath, hdrs, data);
        } else if (strstr(type, "GET") != NULL) {
            Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::sendRequest(): sending GET request: uri: %s",
                    uri.c_str());
            status = request(GET, urlpath, hdrs, data);
        } else if (strstr(type, "HEAD") != NULL) {
            Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::sendRequest(): sending HEAD request");
            status = request(HEAD, urlpath, hdrs, data);
        }
    }
    return status ? AM_SUCCESS : AM_HTTP_ERROR;
}

am_status_t Connection::initialize(const Properties& properties) {
    WSADATA wsad;
    WSAStartup(MAKEWORD(2, 2), &wsad);
    timeout = properties.getUnsigned(AM_COMMON_CONNECT_TIMEOUT_PROPERTY, NETWORK_TIMEOUT);
    proxyPort = properties.get(AM_COMMON_FORWARD_PROXY_PORT, "");
    proxyHost = properties.get(AM_COMMON_FORWARD_PROXY_HOST, "");
    proxyUser = properties.get(AM_COMMON_FORWARD_PROXY_USER, "");
    proxyPassword = properties.get(AM_COMMON_FORWARD_PROXY_PASSWORD, "");
    keyName = properties.get(AM_COMMON_CERT_KEY_PROPERTY, "");
    return AM_SUCCESS;
}

am_status_t Connection::initialize_in_child_process(const Properties& properties) {
    WSADATA wsad;
    WSAStartup(MAKEWORD(2, 2), &wsad);
    timeout = properties.getUnsigned(AM_COMMON_CONNECT_TIMEOUT_PROPERTY, NETWORK_TIMEOUT);
    proxyPort = properties.get(AM_COMMON_FORWARD_PROXY_PORT, "");
    proxyHost = properties.get(AM_COMMON_FORWARD_PROXY_HOST, "");
    proxyUser = properties.get(AM_COMMON_FORWARD_PROXY_USER, "");
    proxyPassword = properties.get(AM_COMMON_FORWARD_PROXY_PASSWORD, "");
    keyName = properties.get(AM_COMMON_CERT_KEY_PROPERTY, "");
    return AM_SUCCESS;
}

am_status_t Connection::shutdown() {
    WSACleanup();
    return AM_SUCCESS;
}

am_status_t Connection::shutdown_in_child_process() {
    WSACleanup();
    return AM_SUCCESS;
}
