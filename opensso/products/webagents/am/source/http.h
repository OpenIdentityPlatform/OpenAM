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
 * $Id: http.h,v 1.6 2009/12/19 00:05:46 subbae Exp $
 *
 *
 * Abstract:
 *
 * Collection of helper classes for handling HTTP requests and responses.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef HTTP_H
#define HTTP_H

#include <exception>
#include <map>
#include <string>
#include <vector>

#include "am_types.h"
#include "connection.h"
#include "internal_macros.h"
#include "log.h"

BEGIN_PRIVATE_NAMESPACE

namespace Http {
    enum Status {
	INVALID = 0,

	CONTINUE = 100,
	SWITCHING_PROTOCOLS,

	OK = 200,
	CREATED,
	ACCEPTED,
	NON_AUTH_INFO,
	NO_CONTENT,
	RESET_CONTENT,
	PARTIAL_CONTENT,

	MULTIPLE_CHOICES = 300,
	MOVED_PERMANENTLY,
	FOUND,
	SEE_OTHER,
	NOT_MODIFIED,
	USE_PROXY,
	// 306 is Unused
	TEMP_REDIRECT = 307,

	BAD_REQUEST = 400,
	UNAUTHORIZED,
	PAYMENT_REQUIRED,
	FORBIDDEN,
	NOT_FOUND,
	METHOD_NOT_ALLOWED,
	NOT_ACCEPTABLE,
	PROXY_AUTH_REQUIRED,
	REQUEST_TIMEOUT,
	CONFLICT,
	GONE,
	LENGTH_REQUIRED,
	PRE_CONDITION_FAILED,
	REQUEST_ENTITY_TOO_LONG,
	REQUEST_URI_TOO_LONG,
	UNSUPPORTED_MEDIA,
	REQUEST_RANGE_NOT_SATISFIABLE,
	EXPECTATION_FAILED,

	INTERNAL_SERVER_ERROR = 500,
	NOT_IMPLEMENTED,
	BAD_GATEWAY,
#if defined(AIX)
	SERVICE_UNAVAILABLE_AIX,
#else
	SERVICE_UNAVAILABLE,
#endif

	GATEWAY_TIMEOUT,
	HTTP_VERSION_NOT_SUPPORTED
    };

    class ParseException: public std::exception {
    public:
	ParseException(am_status_t statusArg) : status(statusArg) {}
	~ParseException() throw() {}

	virtual const char *what() const throw() 
	{
	    return "Http::ParseException";
	}

	am_status_t getStatus() const { return status; }

    private:
	am_status_t status;
    };

    struct Cookie {
	Cookie(): name(), value(),wholestring() {}
	Cookie(const std::string &n, const std::string &v): name(n),
							    value(v) {}
	/* throws std::exception's */
	explicit Cookie(const char *cookieString); 

	std::string name;
	std::string value;
	std::string wholestring;
    };
    typedef std::vector<Cookie> CookieList;
    typedef std::vector<Cookie> HeaderList;
    typedef CookieList::iterator CookieListIter;

    class HeaderMap {
    public:
	typedef	std::map<std::string, std::string> MapType;

	HeaderMap();
	HeaderMap(const HeaderMap &hMap);
	void erase(const std::string& key);
	MapType::const_iterator get(const std::string& key) const;
	MapType::iterator get(const std::string& key);
	const std::string *getValue(const std::string& key) const;
	std::string *getValue(const std::string& key);
	void set(const std::string& key, const std::string& value);

	MapType::iterator begin() { return headers.begin(); }
	MapType::iterator end() { return headers.end(); }

	MapType::const_iterator begin() const { return headers.begin(); }
	MapType::const_iterator end() const { return headers.end(); }

    private:
	MapType headers;
    };

    class Response {
    public:
	Response();
	Response(Log::ModuleId logModule, Connection& conn);
	~Response();

	am_status_t readAndParse(Log::ModuleId logModule, Connection& conn);
	Status getStatus() const { return httpStatus; }
	const CookieList& getCookieList() const { return cookieList; }
	const HeaderMap& getExtraHdrs() const { return extraHdrs; }
	const std::string *getHeaderValue(const std::string& key) const
	{
	    return extraHdrs.getValue(key);
	}
	const char *getBodyPtr() const { return bodyPtr; };
	std::size_t getBodyLen() const { return bodyLen; }

	inline char *getBody(std::size_t& bodyLen);

    private:
	Response(const Response& rhs); // not implemented
	Response& operator=(const Response& rhs); // not implemented

	Status httpStatus;
	CookieList cookieList;
	HeaderMap extraHdrs;
	char *bodyPtr;
	std::size_t bodyLen;
    };

    inline char *Response::getBody(std::size_t& bodyLenRef)
    {
	char *resultPtr = bodyPtr;

	bodyLenRef = bodyLen;
	bodyPtr = NULL;

	return resultPtr;
    }

    std::string decode(const std::string& encodedString);
    std::string encode(const std::string& rawString);
    std::string cookie_encode(const std::string& rawString);

}

END_PRIVATE_NAMESPACE

#endif	/* not HTTP_H */
