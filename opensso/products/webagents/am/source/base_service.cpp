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
 * $Id: base_service.cpp,v 1.7 2008/09/13 01:11:53 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */
#include <stdio.h>
#include <stdexcept>
#include <iostream>
#include <sstream>
#include <cassert>
#include "base_service.h"
#include "connection.h"
#include "http.h"
#include "log.h"
#include "nspr_exception.h"
#include "scope_lock.h"
#include "xml_tree.h"
#include <cstring>

#define AM_NAMING_LOCK ".am_naming_lock"
extern "C" char *read_naming_value(const char *key, int iid);
extern "C" unsigned long am_web_naming_validation_status();

USING_PRIVATE_NAMESPACE

        namespace {
    // A byte formatted as decimal digits will fit in at most 3 char's.
    const unsigned int DIGITS_PER_BYTE = 3;
    const char CONTENT_LENGTH_HDR[] = "Content-Length: ";

    const char HTTP_GET_PREFIX[] = "GET ";
    const char HTTP_POST_PREFIX[] = "POST ";
    const char HTTP_VERSION_SUFFIX[] = " HTTP/1.0\r\n";

    const char HTTP_GET_SUFFIX[] = {
        "Accept: text/plain\r\n"
        "\r\n"
    };

    const char HTTP_POST_SUFFIX[] = {
        "Accept: text/xml\r\n"
        "Content-Type: text/xml; charset=UTF-8\r\n"
        "\r\n"
    };

    const char HTTP_POST_FORM_SUFFIX[] = {
        "Accept: text/xml, text/plain\r\n"
        "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n"
        "\r\n"
    };

    const char RESPONSE_SET_VERSION[] = "1.0";
}

Mutex BaseService::classLock;
unsigned int BaseService::globalRequestId;

BaseService::Request::Request(BaseService& serviceArg,
        const BodyChunk& globalPrefixChunk,
        const BodyChunk& servicePrefixChunk,
        std::size_t numExtraElements,
        bool addServiceID)
: service(serviceArg), bodyChunkList(), extraData(NULL) {
    const std::size_t NUM_STD_ELEMENTS = 4;
    BodyChunk temp;
    int cnt = 0;

    try {
        bodyChunkList.reserve(NUM_STD_ELEMENTS + numExtraElements);
    } catch (const std::length_error& exc) {
        Log::log(serviceArg.logModule, Log::LOG_WARNING,
                "Request() caught length_error trying to size bodyChunkList "
                "to (%u + %u) elements, will grow incrementally: %s",
                NUM_STD_ELEMENTS, numExtraElements, exc.what());
    }

    bodyChunkList.push_back(globalPrefixChunk);
    cnt = snprintf(globalIdBuf, sizeof (globalIdBuf), "%u",
            service.getNextGlobalRequestId());

    assert(cnt > 0 && static_cast<std::size_t> (cnt) < sizeof (globalIdBuf));
    temp.data = globalIdBuf;
    bodyChunkList.push_back(temp);

    bodyChunkList.push_back(servicePrefixChunk);
    if (addServiceID) {
        getNextServiceRequestIdAsString(serviceIdBuf, sizeof (serviceIdBuf));
        temp.data = serviceIdBuf;
        bodyChunkList.push_back(temp);
    }
}

std::size_t
BaseService::Request::getNextServiceRequestIdAsString(char *buffer,
        std::size_t bufferLen) {
    int cnt = 0;

    cnt = snprintf(buffer, bufferLen, "%u",
            service.getNextServiceRequestId());
    assert(cnt > 0 && static_cast<std::size_t> (cnt) < bufferLen);

    return static_cast<std::size_t> (cnt);
}

BaseService::BaseService(const std::string& name,
        const Properties& props)
: logModule(Log::addModule(name)), objLock(), serviceRequestId(0),
poll_primary_server(props.get(AM_COMMON_POLL_PRIMARY_SERVER, "5")),
proxyHost(props.get(AM_COMMON_FORWARD_PROXY_HOST, "")),
proxyPort(atoi(props.get(AM_COMMON_FORWARD_PROXY_PORT, "0").c_str())),
proxyUser(props.get(AM_COMMON_FORWARD_PROXY_USER, "")),
proxyPassword(props.get(AM_COMMON_FORWARD_PROXY_PASSWORD, "")) {
    useProxy = proxyHost.size() > 0 ? true : false;
    useProxyAuth = proxyUser.size() > 0 ? true : false;
}

BaseService::~BaseService() {
}

BaseService::IdType BaseService::getNextGlobalRequestId() {
    ScopeLock myLock(classLock);

    return globalRequestId++;
}

BaseService::IdType BaseService::getNextServiceRequestId() {
    ScopeLock myLock(objLock);

    return serviceRequestId++;
}

am_status_t BaseService::sendRequest(Connection& conn,
        const BodyChunk& headerPrefix,
        const std::string& uri,
        const std::string& uriParameters,
        const Http::HeaderList& headerList,
        const Http::CookieList& cookieList,
        const BodyChunk& contentLine,
        const BodyChunk& headerSuffix,
        const BodyChunkList& bodyChunkList) const {

    Connection::ConnHeaderMap hdrs;
    size_t hListSize = headerList.size();
    Http::Cookie theCookie;
    for (size_t idx = 0; idx < hListSize; idx++) {
        std::string headerLine;
        theCookie = headerList[idx];
        headerLine.append(theCookie.name);
        headerLine.append(": ");
        headerLine.append(theCookie.value);
        hdrs.insert(Connection::ConnHeaderMapValue(headerLine, ""));
    }

    size_t listSize = cookieList.size();
    if (listSize > 0) {
        std::string cookieLine("Cookie: ");
        for (size_t iii = 0; iii < listSize; iii++) {
            theCookie = cookieList[iii];
            cookieLine.append(theCookie.name);
            cookieLine.append("=");
            cookieLine.append(theCookie.value);
            if (iii != listSize - 1) {
                cookieLine.append(";");
            }
        }
        hdrs.insert(Connection::ConnHeaderMapValue(cookieLine, ""));
    }

    std::string hsorig(headerSuffix.data);
    hsorig.erase(hsorig.size() - 2);
    std::stringstream hs(hsorig);
    std::string line;
    while (std::getline(hs, line)) {
        if (line != "")
            hdrs.insert(Connection::ConnHeaderMapValue(line, ""));
    }

    std::string body;
    for (std::size_t i = 0; i < bodyChunkList.size(); ++i) {
        body.append(bodyChunkList[i].data);
    }

    std::string u(uri + uriParameters);
    return conn.sendRequest(headerPrefix.data.c_str(),
            u, hdrs, body);
}

am_status_t
BaseService::doRequest(const ServiceInfo& service,
        const BodyChunk& headerPrefix,
        const std::string& uriParameters,
        const Http::CookieList& cookieList,
        const BodyChunk& headerSuffix,
        const BodyChunkList& bodyChunkList,
        Http::Response& response,
        const ServerInfo** serverInfo) const {
    am_status_t status = AM_SERVICE_NOT_AVAILABLE;
    std::size_t dataLen = 0;
    // Create a temporary buffer for the Content-Line header
    // the extra '2' is for the <CR><LF> at the end.  The
    // sizeof the CONTENT_LENGTH_HDR includes space for the
    // terminating NUL.
    char contentLine[sizeof (CONTENT_LENGTH_HDR) +
            (sizeof (dataLen) * DIGITS_PER_BYTE) + 2];
    std::size_t contentLineLen;

    for (unsigned int i = 0; i < bodyChunkList.size(); ++i) {
        dataLen += bodyChunkList[i].data.size();
    }

    BodyChunk emptyContentLine;

    contentLineLen = snprintf(contentLine, sizeof (contentLine), "%s%d\r\n",
            CONTENT_LENGTH_HDR, dataLen);
    if (sizeof (contentLine) > contentLineLen) {
        BodyChunk contentLineChunk(contentLine, contentLineLen);
        ServiceInfo::const_iterator iter;
        ServiceInfo svc(service);
        int j = -1, current_index = -1;
        if (am_web_naming_validation_status() < 2 && svc.getNumberOfServers() > 1) {
            /* validation is enabled and number of servers is more than one */
            char *current_value = read_naming_value(AM_NAMING_LOCK, Log::getLockId());
            if (current_value != NULL) {
                current_index = strtol(current_value, NULL, 10);
                if (current_index < 0 || errno == ERANGE) {
                    current_index = 0;
                } else {
                    Log::log(logModule, Log::LOG_MAX_DEBUG,
                            "BaseService::doRequest(): will be using url index: %d",
                            current_index);
                }
                free(current_value);
            } else {
                current_index = 0;
            }
            if ((size_t) current_index >= svc.getNumberOfServers()) {
                Log::log(logModule, Log::LOG_WARNING,
                        "BaseService::doRequest(): invalid url index: %d (out of %d); validation results ignored.",
                        current_index, svc.getNumberOfServers());
                current_index = -1;
            }
        }

        for (iter = svc.begin(); iter != svc.end(); ++iter) {
            ServerInfo svrInfo = ServerInfo((const ServerInfo&) (*iter));

            if (current_index != -1) {
                if ((++j) != current_index) {
                    Log::log(logModule, Log::LOG_MAX_DEBUG,
                            "BaseService::doRequest(): skipping url(%d) %s",
                            j, svrInfo.getURL().c_str());
                    continue;
                } else {
                    Log::log(logModule, Log::LOG_MAX_DEBUG,
                            "BaseService::doRequest(): using url(%d) %s",
                            j, svrInfo.getURL().c_str());
                }
            }

            Http::HeaderList headerList, proxyHeaderList;
            Http::Cookie hostHeader("Host", svrInfo.getHost());
            headerList.push_back(hostHeader);

#ifndef _MSC_VER
            if (useProxy) {
                proxyHeaderList.push_back(hostHeader);
                // Override (temporarily) server credentials if using proxy
                svrInfo.setHost(proxyHost);
                svrInfo.setPort(proxyPort);
                // We don't use SSL for initial proxy connection
                svrInfo.setUseSSL(false);
                Log::log(logModule, Log::LOG_DEBUG,
                        "BaseService::doRequest(): Using proxy: %s:%d",
                        proxyHost.c_str(), proxyPort);
                // Add Proxy-Authorization header if user defined
                if (useProxyAuth) {
                    // allocate enough for a base64-encoded digest
                    size_t authSize = proxyUser.size() +
                            proxyPassword.size() + 1;
                    // 11 extra bytes for prefix and terminator
                    char * digest = (char *) malloc(authSize * 4 / 3 + 11);
                    strcpy(digest, "Basic ");
                    encode_base64((proxyUser + ":" + proxyPassword).c_str(),
                            authSize, (digest + 6));
                    Log::log(logModule, Log::LOG_MAX_DEBUG,
                            "BaseService::doRequest(): Using proxy auth as: %s",
                            proxyUser.c_str());
                    hostHeader = Http::Cookie("Proxy-Authorization", digest);
                    proxyHeaderList.push_back(hostHeader);
                    free(digest);
                }
            }
#endif

            // retry to connect to server before marking it as down.
            // making the number of attempts configurable may have a negative
            // side effect on performance, if the the value is a high number.
            int retryAttempts = 3;
            int retryCount = 0;
            while (retryCount < retryAttempts) {
                retryCount++;
                try {
                    Connection conn(svrInfo);
                    const char *operation = "sending to";
                    std::string requestString = iter->getURI();
                    /*
                     * In case the following request would go to a proxy
                     * we need to use full URL and special headers.
                     */

                    if (useProxy && !(iter->useSSL())) {
                        requestString = iter->getURL();
                        headerList = proxyHeaderList;
                    }
                    status = sendRequest(conn, headerPrefix, requestString,
                            uriParameters, headerList, cookieList,
                            dataLen > 0 ? contentLineChunk : emptyContentLine, headerSuffix,
                            bodyChunkList);
                    if (AM_SUCCESS == status) {
                        operation = "receiving from";
                        status = response.readAndParse(logModule, conn);
                        if (AM_SUCCESS == status) {
                            Log::log(logModule, Log::LOG_MAX_DEBUG, "%.*s",
                                    response.getBodyLen(), response.getBodyPtr());
                        }
                    }

                    if (AM_NSPR_ERROR == status) {
                        Log::log(logModule, Log::LOG_ALWAYS,
                                "BaseService::doRequest() NSPR failure while "
                                "%s %s", operation,
                                (*iter).toString().c_str());
                    }

                    if (AM_SUCCESS == status) {
                        if (serverInfo != NULL) *serverInfo = &(*iter);
                        break;
                    } else {
                        if (retryCount < retryAttempts) {
                            continue;
                        } else {
                            Log::log(logModule, Log::LOG_DEBUG,
                                    "BaseService::doRequest() Invoking markSeverDown");
                            svrInfo.markServerDown(poll_primary_server);
                        }
                    }
                } catch (const NSPRException& exc) {
                    Log::log(logModule, Log::LOG_ERROR,
                            "BaseService::doRequest() caught %s: %s called by %s", exc.what(), exc.getNsprMethod(),
                            exc.getThrowingMethod());

                    if (retryCount < retryAttempts) {
                        status = AM_NSPR_ERROR;
                        continue;
                    } else {
                        Log::log(logModule, Log::LOG_ERROR,
                                "BaseService::doRequest() Invoking markSeverDown");
                        svrInfo.markServerDown(poll_primary_server);
                        status = AM_NSPR_ERROR;
                    }
                }
            } //end of while

            if (AM_SUCCESS == status) {
                if (serverInfo != NULL) *serverInfo = &(*iter);
                break;
            }

        } // end of for
    } else {
        status = AM_BUFFER_TOO_SMALL;
    }

    return status;
}

am_status_t BaseService::doHttpGet(const ServiceInfo& service,
        const std::string& uriParameters,
        const Http::CookieList& cookieList,
        Http::Response& response,
        const ServerInfo** serverInfo) const {
    am_status_t status;

    status = doRequest(service, BodyChunk(std::string(HTTP_GET_PREFIX)), uriParameters, cookieList,
            BodyChunk(std::string(HTTP_GET_SUFFIX)), BodyChunkList(), response, serverInfo);
    //
    // NOTE: The omission of a check of the HTTP status here, as compared
    // with doHttpPost, is intentional.  The AuthService code, which is
    // currently the only code that does a GET, performs its own analysis
    // of the HTTP status and various headers to determine the appropriate
    // return code.  Doing part of the analysis here and part in the
    // AuthService code would make things harder to implement and
    // understand.
    //

    return status;
}

am_status_t BaseService::doHttpPost(const ServiceInfo& service,
        const std::string& uriParameters,
        const Http::CookieList& cookieList,
        const BodyChunkList& bodyChunkList,
        Http::Response& response,
        bool doFormPost,
        bool checkHTTPRetCode,
        const ServerInfo **serverInfo) const {
    am_status_t status;

    if (doFormPost) {
        status = doRequest(service, BodyChunk(std::string(HTTP_POST_PREFIX)), uriParameters,
                cookieList, BodyChunk(std::string(HTTP_POST_FORM_SUFFIX)), bodyChunkList,
                response, serverInfo);
    } else {
        status = doRequest(service, BodyChunk(std::string(HTTP_POST_PREFIX)), uriParameters,
                cookieList, BodyChunk(std::string(HTTP_POST_SUFFIX)), bodyChunkList,
                response, serverInfo);
    }


    if (checkHTTPRetCode &&
            (AM_SUCCESS == status && Http::OK != response.getStatus())) {
        Http::Status httpStatus = response.getStatus();

        if (Http::NOT_FOUND == httpStatus) {
            status = AM_NOT_FOUND;
        } else if (Http::FORBIDDEN == httpStatus) {
            status = AM_ACCESS_DENIED;
        } else {
            Log::log(logModule, Log::LOG_WARNING,
                    "BaseService::doHttpPost() failed, HTTP error = %d",
                    httpStatus);
            status = AM_HTTP_ERROR;
        }
    }
    return status;
}

/* 
 * Throws XMLTree::PraseException and other 
 * std::exception's from string methods 
 */
std::vector<std::string>
BaseService::parseGenericResponse(const Http::Response& response,
        const char *expectedId) const {
    XMLTree::Init xt;
    XMLTree responseTree(false, response.getBodyPtr(), response.getBodyLen());
    XMLElement element = responseTree.getRootElement();
    std::vector<std::string> responses;

    if (element.isNamed(RESPONSE_SET)) {
        std::string version;
        std::string requestId;

        if (element.getAttributeValue(VERSION, version) &&
                std::strcmp(version.c_str(), RESPONSE_SET_VERSION) == 0 &&
                element.getAttributeValue(REQUEST_ID, requestId) &&
                std::strcmp(requestId.c_str(), expectedId) == 0) {
            std::string responseData;

            for (element = element.getFirstSubElement();
                    element.isNamed(RESPONSE);
                    element.nextSibling()) {
                if (element.getValue(responseData)) {
                    responses.push_back(responseData);
                } else {
                    element.log(logModule, Log::LOG_ERROR);
                    throw XMLTree::ParseException("unable to get Response "
                            "data");
                }
            }
            if (element.isValid()) {
                element.log(logModule, Log::LOG_ERROR);
                throw XMLTree::ParseException("unexpected element in "
                        "ResponseSet");
            }
        } else if (std::strcmp(version.c_str(), RESPONSE_SET_VERSION) == 0) {
            element.log(logModule, Log::LOG_ERROR);
            throw XMLTree::ParseException(std::string("missing or mismatched "
                    "request id in "
                    "ResponseSet: ") +
                    requestId);
        } else {
            element.log(logModule, Log::LOG_ERROR);
            throw XMLTree::ParseException(std::string("missing or unsupported "
                    "version in "
                    "ResponseSet: ") +
                    version);
        }
    } else {
        element.log(logModule, Log::LOG_ERROR);
        throw XMLTree::ParseException("unexpected element in response");
    }

    return responses;
}
