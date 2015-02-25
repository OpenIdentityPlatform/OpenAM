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
 * $Id: url.h,v 1.6 2009/10/13 01:40:42 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2014 ForgeRock AS
 */

#ifndef __URL_H__
#define __URL_H__
#include <string>
#include "internal_macros.h"
#include "key_value_map.h"

#ifdef __sun
#include <strings.h>
#endif

#define MIN_URL "ftp://a"
#define MIN_URL_LEN	(sizeof(MIN_URL) - 1)

BEGIN_PRIVATE_NAMESPACE
class URL {
 public:
    typedef enum {
	PROTOCOL_HTTP = 0,
	PROTOCOL_HTTPS,
	PROTOCOL_FTP,
	PROTOCOL_TELNET,
	PROTOCOL_GOPHER,
	PROTOCOL_LDAP,
	PROTOCOL_UNKNOWN
    } Protocol;

    /* constructors */
    /* The constructors throw InternalException if there is an error 
     * in the format of the URL, such as invalid protocol or port number.
     * The exception will contain an message for the error.
     */
    URL(const std::string &urlStr,
	bool ignore_case=true); 

    URL(const std::string &urlStr,
	const std::string &path_info,
	bool ignore_case=true); 

    URL(const std::string &urlStr,
	std::size_t url_len,
	bool ignore_case=true); 

    URL(const URL &);

    URL() {
	protocol = PROTOCOL_HTTP;
	port = defaultPort[protocol];
	portStr = defaultPortStr[protocol];
    }

    inline void setHost(const std::string &newHost) {
	host = newHost;
    }

    inline void setPort(std::size_t newPort) {
	if (newPort <= 0 || newPort > 65535) {
	    port = defaultPort[protocol];
	    portStr = defaultPortStr[protocol];
	} else {
	    port = newPort;
	    portStr = Utils::toString(port);
	}
    }

    inline void setProtocol(const std::string& newProtocol) {
        URL::Protocol p = whichProtocol(newProtocol);
	protocol = p;
	port = defaultPort[p];
	portStr = defaultPortStr[p];
    }

    inline void setURI(const std::string& newURI) {
	uri = newURI;
    }

    inline void setQuery(const std::string& newQuery) {
	splitQParams(newQuery);
	query = newQuery;
        checkQueryFormat();
    }

    void getBaseURL(std::string& baseURL, size_t capacity = 0) ;
    
    void getRootURL(std::string& rootURL, size_t capacity = 0) ;

    /* Get functions */
    URL::Protocol getProtocol() const {
	return protocol;
    }

    const char *getProtocolString() const;


    inline const std::string &getHost() const {
	return host;
    }

    inline std::size_t getPort() const {
	return port;
    }

    inline const std::string &getPortStr() const {
	return portStr;
    }

    inline const std::string &getURI() const {
	return uri;
    }

    void getURLString(std::string& urlString, size_t capacity = 0);
    void getCanonicalizedURLString(std::string& urlString, size_t capacity = 0); 

    void removeQueryParameter(const std::string &key);
    bool findQueryParameter(const std::string &key);

 private:
    static const char * protocolStr[];
    static const size_t protocolLen[];

    static const std::size_t defaultPort[];
    static const char *defaultPortStr[];

    /* Throws InternalException if there is an error in the format 
     * of the URL, such as invalid protocol or port number.
     * The exception will contain an message for the error.  */
    void parseURLStr(const std::string &urlStr,
		     const std::string &pathInfo=std::string(""));

    inline bool validatePort() {
	return (port > 65535) ? false : true;
    }

    URL::Protocol whichProtocol(const std::string &proto) {
	std::size_t p;
	for(p = 0; p < PROTOCOL_UNKNOWN; ++p) {
	    if(strcasecmp(proto.c_str(), protocolStr[p]) == 0) {
		break;
	    }
	}
	return (URL::Protocol)(p);
    }

    /* Throws InternalException if the query parameter has invalid format */
    void splitQParams(const std::string &qparam); 

    std::string construct_query_parameter_string() const;
    void checkQueryFormat();
    std::string get_query_parameter_string() const;
    std::string get_canonicalized_query_parameter_string() const; 

    Protocol protocol;
    std::string host;
    std::string portStr;
    std::size_t port;
    std::string uri;
    std::string path_info;
    KeyValueMap qParams;
    std::string query;
    bool icase;
};

END_PRIVATE_NAMESPACE
#endif
