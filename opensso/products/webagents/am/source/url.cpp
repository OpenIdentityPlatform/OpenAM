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
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <iterator>
#include "url.h"
#include "internal_exception.h"

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
	 bool ignore_case, bool useOld) 
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
    parseURLStr(std::string(urlStr.c_str(), url_len), "", useOld);
}

/**
 * Throws InternalException if there is an error in the format 
 * of the URL, such as invalid protocol or port number.
 * The exception will contain an message for the error.
 */
URL::URL(const std::string &urlStr,
	 const std::string &pathInfo,
	 bool ignore_case, bool useOld) 
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
    parseURLStr(urlStr, pathInfo, useOld);
}

/**
 * Throws InternalException if there is an error in the format 
 * of the URL, such as invalid protocol or port number.
 * The exception will contain an message for the error.
 */
URL::URL(const std::string &urlStr,
	 bool ignore_case, bool useOld) 
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
    parseURLStr(urlStr, "", useOld);
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
			    icase(srcURL.icase){}

void URL::parseURLStr(const std::string &urlString,
		      const std::string &pathInfo,
		      bool useOld)  
{
    if (useOld) {
	parseURLStrOld(urlString, pathInfo);
    }
    else {
	parseURLStrNew(urlString, pathInfo);
    }
}

void URL::parseURLStrNew(const std::string &urlString,
		         const std::string &pathInfo) 
{
    const char *func = "parseURLStrNew()";
    const char *url = urlString.c_str();
    const char *path_info_cstr = pathInfo.c_str();
    char *colon_ptr = NULL;
    size_t proto_len = 0;
    char *host_ptr = NULL;
    char *port_ptr = NULL;
    char *uri_ptr = NULL;
    char *query_ptr = NULL;
    char *path_info_ptr = NULL;
    char *end_uri = NULL;
    char *uri_path = NULL;
    char *next_uri_path = NULL;
    int i = 0;

    // check arguments
    if (url == NULL || *url == '\0') {
	throw InternalException(func, "URL is null or empty", 
			        AM_INVALID_RESOURCE_FORMAT);
    }

    // 1. parse protocol 
    
    if ((colon_ptr = (char *)strchr(url, ':')) == NULL) { 
	throw InternalException(func, "URL is missing expected ':'", 
			        AM_INVALID_RESOURCE_FORMAT);
    }
    else if ((proto_len = colon_ptr - url) < MIN_PROTO_LEN || 
	      proto_len > MAX_PROTO_LEN) {
	throw InternalException(func, "Unrecognized URL protocol", 
			        AM_INVALID_RESOURCE_FORMAT);
    }
    // note - if colon_ptr[1] is null, the 2nd compare below will not be done
    // so we are not accessing memory out of bounds. Same with 3rd compare.
    else if (colon_ptr[1] != '/' || colon_ptr[2] != '/' || 
	     colon_ptr[3] == '\0') {
	throw InternalException(func, 
			        "Invalid URL Format - missing :// or "
				"host name following ://", 
			        AM_INVALID_RESOURCE_FORMAT);
    }
    // we know the protocol length is equal to one of the protocol lengths
    // at this point.
    for (i = 0; i < PROTOCOL_UNKNOWN; ++i) {
	if (proto_len == protocolLen[i] &&
	    !strncasecmp(url, protocolStr[i], protocolLen[i])) {
	    // protocol found.
	    protocol = (Protocol)i;
	    break;
	}
    }
    if (i == PROTOCOL_UNKNOWN) {
	throw InternalException(func, "Unknown protocol", 
			        AM_INVALID_RESOURCE_FORMAT);
    }

    // 2. parse host, port
    
    host_ptr = &colon_ptr[3];	// this cannot be the empty string b/c we 
				// checked it above.
    port_ptr = strchr(host_ptr, ':');
    uri_ptr = strchr(host_ptr, '/');
    query_ptr = strchr(host_ptr, '?');

    // check that the special characters : / ? comes after the hostname, 
    // and in that order if at all.
    if (port_ptr == host_ptr || uri_ptr == host_ptr || query_ptr == host_ptr) {
	throw InternalException(func, "Missing hostname in URL",
			        AM_INVALID_RESOURCE_FORMAT);

    }
    if (port_ptr != NULL && 
	    (uri_ptr != NULL && uri_ptr <= port_ptr) || 
	    (query_ptr != NULL && query_ptr <= port_ptr)) {
        port_ptr = NULL; //as there is no port mentioned in URL.

    }
    

    // Now check that / and ? comes after the port, and that 
    // ? comes after the uri.
    if (port_ptr == NULL || port_ptr[1] == '\0' ||
	port_ptr[1] == '/' || port_ptr[1] == '?') {
	// set host
	if (port_ptr != NULL) {
	    host.append(host_ptr, port_ptr - host_ptr);
	}
	else if (uri_ptr != NULL) {
	    host.append(host_ptr, uri_ptr - host_ptr);
	} 
	else if (query_ptr != NULL) {
	    host.append(host_ptr, query_ptr - host_ptr);
	}
	else {
	    host.append(host_ptr);
	}
	// set port
	port = defaultPort[protocol];
	portStr = defaultPortStr[protocol];
    }
    else {
	// set host
	host.append(host_ptr, port_ptr - host_ptr);
	port_ptr = &port_ptr[1];
	// set port
	// check if port number is valid 
	port = 0;
	for (i = 0; port_ptr[i] != '\0' && 
		    port_ptr[i] != '/' && port_ptr[i] != '?'; ++i) {
	    if (port_ptr[i] != '*' && 
		(port_ptr[i] < '0' || port_ptr[i] > '9')) {
		break;
	    } else {
		if (port_ptr[i] == '*') {
		    port = (port*10);   // port is actually never used as a 
					// number, but keeping it for now.
		} else {
		    port = (port*10) + (port_ptr[i] - '0');
		}
	    }
	}
	if (port_ptr[i] != '/' && port_ptr[i] != '\0' && port_ptr[i] != '?') {
	    throw InternalException(func, "Invalid port number", 
				    AM_INVALID_RESOURCE_FORMAT);
	}
	portStr.append(port_ptr, i);
    }

    // 3. parse uri and path-info
    
    if (uri_ptr != NULL) {
	if (path_info_cstr[0] != '\0') {

            if (strcmp(path_info_cstr, "/") != 0) {
                path_info_ptr = strstr(uri_ptr, path_info_cstr);
            } else {
                // As there can be several "/" in the uri, if path info
                // equal "/" we need to point at the last one before the
                // query.
                for (size_t i=0 ; i<strlen(uri_ptr) ; i++) {
                    if (uri_ptr[i] == '?') {
                        break;
                    }
                    if (uri_ptr[i] == '/') {
                        path_info_ptr = uri_ptr + i;
                    }
                }
            }
            if (path_info_ptr == NULL) {
                throw InternalException(func, "Path Info not found in uri",
                      AM_INVALID_RESOURCE_FORMAT);
            } else {
                end_uri = path_info_ptr;
            }

	}
	else if (query_ptr != NULL) {
	    end_uri = query_ptr;
	}
	else {
	    end_uri = uri_ptr+strlen(uri_ptr);
	}
	// remove null paths in and set uri string.
	uri.reserve(end_uri - uri_ptr);
	uri_path = uri_ptr;
	while (uri_path < end_uri) {
	    uri.append("/");
	    ++uri_path;
	    // remove any consecutive '/' so uri_path will point to 
	    // the first non '/' char or end of uri.
	    while (uri_path < end_uri && *uri_path == '/') 
		++uri_path;  
	    if (uri_path >= end_uri) {
		break;
	    }
	    else {
		next_uri_path = uri_path;
		// look for the next '/' 
		while (next_uri_path < end_uri && *next_uri_path != '/')
		    ++next_uri_path;
		if (next_uri_path >= end_uri) {
		    uri.append(uri_path, end_uri - uri_path);
		    break;
		}
		else {
		    uri.append(uri_path, next_uri_path - uri_path);
		    uri_path = next_uri_path;
		}
	    }
	}
    }
    else {
	if (*path_info_cstr != '\0') {
	    throw InternalException(func, "Path Info not found in uri",
				    AM_INVALID_RESOURCE_FORMAT);
	}
    }

    // 4. parse query
    if (query_ptr != NULL && query_ptr[1] != '\0') {
        query.reserve(strlen(query_ptr));
        query.append(query_ptr);
        checkQueryFormat();
	splitQParams(&query_ptr[1]);
    }
}

/**
 * Throws InternalException if there is an error in the format 
 * of the URL, such as invalid protocol or port number.
 * The exception will contain an message for the error.
 */
void URL::parseURLStrOld(const std::string &urlString,
		         const std::string &pathInfo) 
{
    std::string urlStr(urlString);
    std::size_t startPos = 0, tmpPos = 0, endPos = 0;
    std::string func("URL::parseURLStr");
    Utils::trim(urlStr);
    
    if(urlStr.size() < MIN_URL_LEN) {
	std::string msg("Invalid URL. URL Resource is smaller "
			"than the least possible URL: ");
	msg.append(MIN_URL);
	throw InternalException(func, msg, AM_INVALID_RESOURCE_FORMAT);
    }

    /* parse protocol */
    startPos = urlStr.find(":");
    if(startPos == std::string::npos) {
	std::string msg("Invalid protocol in URL :");
	msg.append(urlStr);
	throw InternalException(func, msg, AM_INVALID_RESOURCE_FORMAT);
    } else {
	std::string proto = urlStr.substr(0, startPos);
	protocol = whichProtocol(proto);
	if(protocol == PROTOCOL_UNKNOWN) {
	    std::string msg("Unsupported protocol in URL:");
	    msg.append(urlStr);
	    throw InternalException(func, msg, AM_INVALID_RESOURCE_FORMAT);
	}
    }

    /* parse host */

    // move position after //.
    startPos += 3;
    tmpPos = urlStr.find(":", startPos);

    if(tmpPos == std::string::npos)
	tmpPos = urlStr.find("/", startPos);

    endPos = (tmpPos == std::string::npos)?tmpPos:tmpPos - startPos;

    host = urlStr.substr(startPos, endPos);
    Utils::trim(host);
    if(host.size() <= 0) {
	std::string msg("Invalid Host name in URL:");
	msg.append(urlStr);
	throw InternalException(func, msg, AM_INVALID_RESOURCE_FORMAT);
    }

    /* parse port */
    // The options are
    // ftp://xyz.sun.com
    // ftp://fxy.blah.com:/
    // ftp://ftp.sun.com:80
    // ftp://ftp.sun.com:80/...

    // e.g. ftp://xyz.com
    if(endPos == std::string::npos) {
	port = defaultPort[protocol];
	portStr = defaultPortStr[protocol];
    } else {
	startPos = tmpPos;
	if(urlStr.at(tmpPos) == '/') {
	    // e.g. http://xyz.com/blah
	    port = defaultPort[protocol];
	    portStr = defaultPortStr[protocol];
	} else {
	    // e.g. http://xyz.com:<port>/blah
	    startPos = tmpPos + 1;
	    tmpPos = urlStr.find("/", startPos);
	    if(tmpPos == std::string::npos) {
		endPos = tmpPos;
	    } else {
		endPos = tmpPos - startPos;
	    }

	    portStr = urlStr.substr(startPos, endPos);
	    Utils::trim(portStr);
	    if(portStr.size() == 0) {
		port = defaultPort[protocol];
		portStr = defaultPortStr[protocol];
	    } else {
	        size_t indx = portStr.find('*');
	  	if (indx < 0) {
		    try {
			port = Utils::getNumber(portStr);
		    }
		    catch (...) {
			throw InternalException(func, "Invalid Port Number",
					        AM_INVALID_ARGUMENT);
		    }
		    if (0 == port) {
                        port = defaultPort[protocol];
                    }
		    if(!validatePort()) {
		        std::string msg("Invalid port value specified in URL:");
		        msg.append(urlStr);
		        throw InternalException(func, msg,
					    AM_INVALID_RESOURCE_FORMAT);
		    }
		} 
	    }
	}
    }
    /* parse URI */
    // The options are:
    // http://xyz.sun.com[:port]
    // http://xyz.sun.com[:port]/uri
    // http://xyz.sun.com[:port]/uri[query params]
    if(endPos != std::string::npos) {
	// e.g. http://xyz.sun.com[:<port>]/<uri>[query params]
	startPos = tmpPos;
	tmpPos = urlStr.find("?", startPos);
	if(tmpPos == std::string::npos) {
	    endPos = tmpPos;
	} else {
	    endPos = tmpPos - startPos;
	}
	uri = urlStr.substr(startPos, endPos);
	if(pathInfo.size() > 0) {
	    std::size_t pPos = uri.rfind(pathInfo);
	    // if path_info is indeed a substring of the URI,
	    // then we do take path info into account.
	    if(pPos != std::string::npos) {
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
    if(endPos != std::string::npos) {
	startPos = tmpPos + 1;
	query = urlStr.substr(startPos);
        checkQueryFormat();
	splitQParams(query);
    }

    return;
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
    }
    return retVal;
}
 


/**
 * Throws InternalException if the query parameter has an invalid format.
 */
void URL::splitQParams(const std::string &qparam) 
{
    try {
	qParams.parseKeyValuePairString(qparam, '&', '=', icase);
    }
    catch (...) {
	throw InternalException("URL::splitQParams", 
			        "Invalid key value pair",
				AM_INVALID_ARGUMENT);
    }
}

void URL::removeQueryParameter(const std::string &key) {
    KeyValueMap::iterator iter = qParams.find(key);
    size_t startPos = 0; size_t endPos = 0;
    
    // Remove parameter from KeyValueMap
    qParams.erase(iter);
    
    // Remove parameter from the query string
    startPos=query.find(key);
    if (startPos != std::string::npos) {
        endPos=query.find("&");
        if (endPos == std::string::npos) {
            endPos = query.size();
        }
        query.erase (startPos, endPos);
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

