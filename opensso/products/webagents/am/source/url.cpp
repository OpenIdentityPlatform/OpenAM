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
 * $Id: url.cpp,v 1.9 2009/12/01 21:52:54 subbae Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013-2014 ForgeRock AS
 */

#include <iterator>
#include <algorithm>
#include <stdlib.h>
#include "url.h"
#include "internal_exception.h"
#include "http.h"

USING_PRIVATE_NAMESPACE

const char * URL::protocolStr[] = { "http", "https", "ftp", "telnet",
				    "gopher", "ldap" };

const size_t URL::protocolLen[] = { sizeof("http")-1, 
				    sizeof("https")-1, 
				    sizeof("ftp")-1, 
				    sizeof("telnet")-1,
				    sizeof("gopher")-1,
				    sizeof("ldap")-1 
			          };

const std::size_t URL::defaultPort[]  = { 80, 443, 21, 23, 70, 389 };
const char * URL::defaultPortStr[]  = { "80", "443", "21", "23", "70", "389" };

#define MIN_PROTO_LEN	(sizeof("ftp")-1)
#define MAX_PROTO_LEN	(sizeof("telnet")-1)

/**
 * Throws InternalException if there is an error in the format 
 * of the URL, such as invalid protocol or port number.
 * The exception will contain an message for the error.
 */
URL::URL(const std::string &urlStr, std::size_t url_len,
	 bool ignore_case) 
    : 
      protocol(PROTOCOL_UNKNOWN),
      host(),
      portStr(),
      port(0),
      uri(),
      path_info(),
      qParams(),
      query(),
      icase(ignore_case) 
{
    parseURLStr(std::string(urlStr.c_str(), url_len), "");
}

/**
 * Throws InternalException if there is an error in the format 
 * of the URL, such as invalid protocol or port number.
 * The exception will contain an message for the error.
 */
URL::URL(const std::string &urlStr,
	 const std::string &pathInfo,
	 bool ignore_case) 
    : 
      protocol(PROTOCOL_UNKNOWN),
      host(),
      portStr(),
      port(0),
      uri(),
      path_info(pathInfo),
      qParams(),
      query(),
      icase(ignore_case) 
{
    parseURLStr(urlStr, pathInfo);
}

/**
 * Throws InternalException if there is an error in the format 
 * of the URL, such as invalid protocol or port number.
 * The exception will contain an message for the error.
 */
URL::URL(const std::string &urlStr,
	 bool ignore_case) 
    : 
      protocol(PROTOCOL_UNKNOWN),
      host(),
      portStr(),
      port(0),
      uri(),
      path_info(),
      qParams(),
      query(),
      icase(ignore_case) 
{
    parseURLStr(urlStr, "");
}


URL::URL(const URL &srcURL):
			    protocol(srcURL.protocol),
			    host(srcURL.host),
			    portStr(srcURL.portStr),
			    port(srcURL.port),
			    uri(srcURL.uri),
			    path_info(srcURL.path_info),
			    qParams(srcURL.qParams),
			    query(srcURL.query),
			    icase(srcURL.icase){
}

/**
 * Throws InternalException if there is an error in the format 
 * of the URL, such as invalid protocol or port number.
 * The exception will contain an message for the error.
 */
void URL::parseURLStr(const std::string &urlString, const std::string &pathInfo) {
    std::string urlStr(urlString);
    std::string func("URL::parseURLStr");
    typedef std::string::const_iterator iterator_t;
    Utils::trim(urlStr);

    if (urlStr.size() < MIN_URL_LEN) {
        std::string msg("Invalid URL. URL Resource is smaller "
                "than the least possible URL: ");
        msg.append(MIN_URL);
        throw InternalException(func, msg, AM_INVALID_RESOURCE_FORMAT);
    }

    iterator_t uriStart = urlStr.begin();
    iterator_t uriEnd = urlStr.end();
    /* query start */
    iterator_t queryStart = std::find(uriStart, uriEnd, '?');
    /* protocol */
    iterator_t protocolStart = urlStr.begin();
    iterator_t protocolEnd = std::find(protocolStart, uriEnd, ':');
    if (protocolEnd != uriEnd) {
        std::string prot = &*(protocolEnd);
        if ((prot.length() > 3) && (prot.substr(0, 3) == "://")) {
            protocol = whichProtocol(std::string(protocolStart, protocolEnd));
            protocolEnd += 3;
        } else {
            protocolEnd = urlStr.begin();
        }
    } else {
        protocolEnd = urlStr.begin();
    }

    if (protocol == PROTOCOL_UNKNOWN) {
        std::string msg("Unsupported protocol in URL: ");
        msg.append(urlStr);
        throw InternalException(func, msg, AM_INVALID_RESOURCE_FORMAT);
    }

    /* host */
    iterator_t hostStart = protocolEnd;
    iterator_t pathStart = std::find(hostStart, uriEnd, '/'); /* path start */
    iterator_t hostEnd = std::find(protocolEnd,
            (pathStart != uriEnd) ? pathStart : queryStart,
            ':'); /* check for port */
    host = std::string(hostStart, hostEnd);
    Utils::trim(host);
    if (host.size() <= 0) {
        std::string msg("Invalid Host name in URL: ");
        msg.append(urlStr);
        throw InternalException(func, msg, AM_INVALID_RESOURCE_FORMAT);
    }

    /* parse port */
    if ((hostEnd != uriEnd) && ((&*(hostEnd))[0] == ':')) {
        hostEnd++;
        iterator_t portEnd = (pathStart != uriEnd) ? pathStart : queryStart;
        portStr = std::string(hostEnd, portEnd);
        port = strtol(portStr.c_str(), NULL, 10);
    } else {
        portStr = defaultPortStr[protocol];
        port = defaultPort[protocol];
    }

    /* parse uri */
    if (pathStart != uriEnd) {
        std::string uriTmp = std::string(pathStart, queryStart);
        const char *u = uriTmp.c_str();
        char last = 0;
        uri.reserve(uriTmp.size());
        while (*u != '\0') {
            // replace all consecutive '/' with a single '/'
            if (*u != '/' || (*u == '/' && last != '/')) {
                uri.push_back(*u);
            }
            last = *u;
            u++;
        }
        if (pathInfo.size() > 0) {
            std::string uriDec;
            std::size_t pPos = uri.rfind(pathInfo);
            try {
                uriDec = Http::decode(uri);
            } catch (...) {
                uriDec.erase();
            }
            // if path_info is indeed a substring of the URI,
            // then we do take path info into account.
            if (pPos != std::string::npos) {
                uri.erase(pPos);
                path_info = pathInfo;
            } else if ((pPos = uriDec.rfind(pathInfo)) != std::string::npos) {
                uri.erase(pPos);
                path_info = pathInfo;
            } else {
                std::string msg("Path info passed was not "
                        "found to be a part of the URL: ");
                msg.append(urlStr);
                msg.append(" :Path-info: ");
                msg.append(pathInfo);
                throw InternalException(func, msg, AM_INVALID_RESOURCE_FORMAT);
            }
        }
    }

    /* parse queryParameters */
    if (queryStart != uriEnd && (queryStart + 1) == uriEnd) {
        /* keep trailing question mark */
        query = "?";
    }

    if (queryStart != uriEnd && (queryStart + 1) != uriEnd) {
        query = std::string(queryStart, uriEnd);
        checkQueryFormat();
        splitQParams(query);
    }
}

void URL::getURLString(std::string& urlStr, size_t capacity) {
    size_t size = MIN_URL_LEN+
	       host.size()+portStr.size()+uri.size()+path_info.size()+100;
    if (capacity == 0) 
	capacity = size;
    getBaseURL(urlStr, capacity);
    urlStr.append(path_info);
    urlStr.append(get_query_parameter_string());
}

void URL::getCanonicalizedURLString(std::string& urlStr, size_t capacity) {
    size_t size = MIN_URL_LEN+
    host.size()+portStr.size()+uri.size()+path_info.size()+100;
    if (capacity == 0) {
        capacity = size;
    }
    getBaseURL(urlStr, capacity);
    urlStr.append(path_info);
    urlStr.append(get_canonicalized_query_parameter_string());
}
 

void URL::getBaseURL(std::string& baseURL, size_t capacity) {
    size_t size = MIN_URL_LEN+
	       host.size()+portStr.size()+uri.size()+path_info.size()+10;
    if (capacity == 0) 
	capacity = size;
    getRootURL(baseURL, capacity);
    baseURL.append(uri);
}

void URL::getRootURL(std::string& rootURL, size_t capacity) {
    size_t size = MIN_URL_LEN + host.size() + portStr.size() +10;
    rootURL.erase();
    if (capacity == 0) {
	capacity = size;
    }
    if (rootURL.capacity() < capacity) 
	rootURL.reserve(capacity);
    rootURL.append(protocolStr[protocol]);
    rootURL.append("://");
    rootURL.append(host);
    rootURL.append(":");
    rootURL.append(portStr);
}

std::string URL::get_query_parameter_string() const 
{
    return query;
}

/**
  * For policy evaluation, the query string needs to be
  * canonicalized, that is the query parameters are put
  * in alphabetic order.
  */
std::string URL::get_canonicalized_query_parameter_string() const
{
    std::string retVal;
    if(qParams.size() > 0) {
        retVal.append("?");
        KeyValueMap::const_iterator iter = qParams.begin();
        for(; iter != qParams.end(); ++iter) {
            const KeyValueMap::key_type &key = iter->first;
            const KeyValueMap::mapped_type &values = iter->second;
            std::size_t val_size = values.size();
            for(std::size_t i = 0; i < val_size; ++i) {
                if (!key.empty()) {
                    if(i > 0 || retVal.size() > 1) {
                        retVal.append("&");
                    }
                    retVal.append(key);
                    if (!values[i].empty()) {
                        retVal.append("=");
                        retVal.append(values[i]);
                    }
                }
            }
        }
    } else if (query.size() == 1 && query == "?") {
        retVal.append("?");
    }
    return retVal;
}
 

/**
 * Throws InternalException if the query parameter has an invalid format.
 */
void URL::splitQParams(const std::string &qparam) {
    try {
        qParams.parseKeyValuePairString(qparam[0] != '?' ? qparam : qparam.substr(1),
                '&', '=', true, icase);
    } catch (...) {
        throw InternalException("URL::splitQParams",
                "Invalid key value pair",
                AM_INVALID_ARGUMENT);
    }
}

void URL::removeQueryParameter(const std::string &key) {
    size_t startPos = 0, endPos = 0;

    // Remove parameter from KeyValueMap
    if (qParams.size() > 0) {
        KeyValueMap::iterator iter = qParams.begin();
        for (; iter != qParams.end(); ++iter) {
            const KeyValueMap::key_type &k = iter->first;
            if (key == k) {
                qParams.erase(iter);
                break;
            }
        }
    }

    // Remove parameter from the query string
    startPos = query.find(key);
    if (startPos != std::string::npos) {
        endPos = query.find("&");
        if (endPos == std::string::npos) {
            endPos = query.size();
        }
        query.erase(startPos, endPos);
        if (query.compare("?") == 0) {
            query.clear();
        }
    }
}

void URL::checkQueryFormat() {
    // If there is a query, it must start with a question mark.
    if ((!query.empty()) && (query[0] != '?')) {
        query.insert(0,"?");
    }
}

bool URL::findQueryParameter(const std::string &key) {
    bool retValue = false;
    size_t pos;
    std::string tmpStr;

    tmpStr= "?";
    tmpStr.append(key).append("=");
    pos = query.find(tmpStr);
    if (pos == 0) {
        retValue= true;
    } else {
        tmpStr.replace(0,1,"&");
        pos = query.find(tmpStr);
        if (pos != std::string::npos) {
            retValue= true;
        }
    }
    return retValue;
}

const char * URL::getProtocolString() const {
    return protocolStr[protocol];
}

