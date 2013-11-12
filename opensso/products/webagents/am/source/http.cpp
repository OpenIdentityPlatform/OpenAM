/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights reserved
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
 * $Id: http.cpp,v 1.6 2009/12/19 00:05:46 subbae Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <cstdio>
#include <algorithm>
#include <new>
#include <stdexcept>

#include "http.h"
#include "nspr_exception.h"
#include <cstring>

USING_PRIVATE_NAMESPACE

namespace {
    // The following constants must be all lower case in order for the
    // string comparisons in hasPrefixIgnoringCase to work correctly.
    //const char CONTENT_LENGTH_HDR[] = "content-length:";
    
    enum {
	TOKEN = 0x1,    // a cookie "token" as defined in RFC 2965 informally 
			// a sequence of non special, non whitespace chars, 
			// i.e. any valid char for cookie name and value.
	CTL = 0x2,	    // control character 
	SEPARATOR = 0x4,    // any name value seperator in a cookie string
	DIGIT = 0x8,	
	HEX_DIGIT = 0x10,
	SPACE = 0x20,	    // encoded to a + in URLEncoder.java
	HT = 0x40,	    // Horizontal Tab
	ALPHA = 0x80,
	UPPER = 0x100,
	LOWER = 0x200,
	SPECIAL = 0x400,   // special characters not encoded by URLEncoder.java
	PLUS = 0x800,	   // decoded to a space by URLDecoder.java
	ALPHA_UPPER = (ALPHA|UPPER),
	ALPHA_LOWER = (ALPHA|LOWER)
    };

    int charType[256] = {
	// 0 - 31: control characters
	CTL, CTL, CTL, CTL, CTL, CTL, CTL, CTL,
	CTL, CTL|HT|SEPARATOR, CTL, CTL, CTL, CTL, CTL, CTL,
	CTL, CTL, CTL, CTL, CTL, CTL, CTL, CTL,
	CTL, CTL, CTL, CTL, CTL, CTL, CTL, CTL,

	// 32 - 47: miscellaneous symbols
	SPACE|SEPARATOR,    // space // encodes to + in URLEncoder.java 
	TOKEN,		    // !
	TOKEN,		    // "
	TOKEN,		    // #
	TOKEN,		    // $
	TOKEN,		    // %
	TOKEN,		    // &
	TOKEN,		    // '
	TOKEN,		    // (
	TOKEN,		    // )
	TOKEN|SPECIAL,	    // *  // not encoded in URLEncoder.java
	TOKEN|PLUS,	    // +  // decodes to space in URLEncoder.java
	SEPARATOR,	    // ,
	TOKEN|SPECIAL,	    // -  // not encoded in URLEncoder.java
	TOKEN|SPECIAL,	    // .  // not encoded in URLEncoder.java
	TOKEN,		    // /

	// 48 - 63: mostly numbers
	TOKEN|DIGIT, TOKEN|DIGIT, TOKEN|DIGIT, TOKEN|DIGIT,
	TOKEN|DIGIT, TOKEN|DIGIT, TOKEN|DIGIT, TOKEN|DIGIT,
	TOKEN|DIGIT, TOKEN|DIGIT, 
	TOKEN,		    // :
	SEPARATOR,	    // ;
	TOKEN,		    // <
	SEPARATOR,	    // =
	TOKEN,		    // >
	TOKEN,		    // ?

	// 64 - 95: mostly upper case letters
	TOKEN,		    // @
      	TOKEN|ALPHA_UPPER|HEX_DIGIT, TOKEN|ALPHA_UPPER|HEX_DIGIT, 
	TOKEN|ALPHA_UPPER|HEX_DIGIT, TOKEN|ALPHA_UPPER|HEX_DIGIT, 
	TOKEN|ALPHA_UPPER|HEX_DIGIT, TOKEN|ALPHA_UPPER|HEX_DIGIT, 
	TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER,
	TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, 
	TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER,
	TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, 
	TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER,
	TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, 
	TOKEN|ALPHA_UPPER, TOKEN|ALPHA_UPPER, 
	TOKEN,		    // [
	TOKEN,		    // '\' 
	TOKEN,		    // ]
	TOKEN,		    // ^      
	TOKEN|SPECIAL,	    // _   // not encoded in URLEncoder.java 

	// 96 - 127: mostly lower case letters
	TOKEN,		    // `
	TOKEN|ALPHA_LOWER|HEX_DIGIT, TOKEN|ALPHA_LOWER|HEX_DIGIT, 
	TOKEN|ALPHA_LOWER|HEX_DIGIT, TOKEN|ALPHA_LOWER|HEX_DIGIT, 
	TOKEN|ALPHA_LOWER|HEX_DIGIT, TOKEN|ALPHA_LOWER|HEX_DIGIT, 
	TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER,
	TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, 
	TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER,
	TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, 
	TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER,
	TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, 
	TOKEN|ALPHA_LOWER, TOKEN|ALPHA_LOWER, 
	TOKEN,		// {
	TOKEN,		// | 
	TOKEN,		// }
	TOKEN,		// ~
	CTL,

	// 128 - 255: Non-ASCII characters
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    inline bool isToken(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & TOKEN) != 0;
    }

    inline bool isSpecial(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & (SPECIAL)) != 0;
    }

    inline bool isAlpha(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & (ALPHA_UPPER|ALPHA_LOWER)) != 0;
    }

    inline bool isDigit(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & DIGIT) != 0;
    }

    inline bool isHexDigit(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & (DIGIT|HEX_DIGIT)) != 0;
    }

    inline bool isWhitespace(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & (SPACE|HT)) != 0;
    }

    inline const char *skipWhitespace(const char *linePtr)
    {
	while (isWhitespace(*linePtr)) {
	    linePtr += 1;
	}
	return linePtr;
    }

    inline bool isUpper(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & UPPER) != 0;
    }

    inline char toLower(char data)
    {
	return isUpper(data) ? (data - 'A' + 'a') : data;
    }

    inline unsigned int convertHexDigit(char data)
    {
	unsigned int value;

	if (isDigit(data)) {
	    value = data - '0';
	} else if (isUpper(data)) {
	    value = data - 'A' + 0xA;
	} else {
	    value = data - 'a' + 0xa;
	}

	return value;
    }

    inline char convertToHexDigit(unsigned int value)
    {
	char data;

	if (value < 10) {
	    data = value + '0';
	} else {
	    data = value - 10 + 'A';
	}

	return data;
    }

    // Determines whether the first string begins with the second string
    // ignoring any differences in case.
    //
    // NOTE: The second string is expected be all lower case.
    inline bool hasPrefixIgnoringCase(const char *str, const char *prefix)
    {
	while (*prefix && (*prefix == toLower(*str))) {
	    prefix += 1;
	    str += 1;
	}

	return *prefix == '\0';
    }
}

struct CompareIgnoringCase {
    explicit CompareIgnoringCase(const std::string& key)
	: keyToFind()
    {
	std::size_t len = key.size();

	keyToFind.reserve(len);
	for (std::size_t i = 0; i < len; ++i) {
	    keyToFind += toLower(key[i]);
	}
    }

    bool operator()(const Http::HeaderMap::MapType::value_type& entry) const
    {
	bool match;

	if (entry.first.size() == keyToFind.size()) {
	    match = hasPrefixIgnoringCase(entry.first.c_str(),
					  keyToFind.c_str());
	} else {
	    match = false;
	}

	return match;
    }

    std::string keyToFind;
};

Http::HeaderMap::HeaderMap():headers(MapType()) {
    // The headers map std::map won't initialize correctly.
    // this is a gig so that we force it to initialize correctly.
    headers["SUNWxxx"] = "SUNWxxx";
    headers.erase("SUNWxxx");
}

Http::HeaderMap::HeaderMap(const HeaderMap &hMap):headers(hMap.headers) {
}

void Http::HeaderMap::erase(const std::string& key)
{
    MapType::iterator iter = std::find_if(headers.begin(), headers.end(),
					  CompareIgnoringCase(key));

    if (iter != headers.end()) {
	headers.erase(iter);
    }
}

Http::HeaderMap::MapType::const_iterator
Http::HeaderMap::get(const std::string& key) const
{
    return std::find_if(headers.begin(), headers.end(),
			CompareIgnoringCase(key));
}

Http::HeaderMap::MapType::iterator
Http::HeaderMap::get(const std::string& key)
{
    return std::find_if(headers.begin(), headers.end(),
			CompareIgnoringCase(key));
}

const std::string *Http::HeaderMap::getValue(const std::string& key) const
{
    Http::HeaderMap::MapType::const_iterator iter = get(key);

    return (iter != headers.end() ? &iter->second : NULL);
}

std::string *Http::HeaderMap::getValue(const std::string& key)
{
    Http::HeaderMap::MapType::iterator iter = get(key);

    return (iter != headers.end() ? &iter->second : NULL);
}

void Http::HeaderMap::set(const std::string& key, const std::string& value)
{
    MapType::iterator iter = std::find_if(headers.begin(), headers.end(),
					  CompareIgnoringCase(key));

    if (iter != headers.end()) {
	PUSH_BACK_CHAR(iter->second, ',');
	iter->second.append(value);
    } else {
	headers[key] = value;
    }
}

/**
 * Throws: std::invalid_argument upon invalid argument
 * and other std::exceptions's from string methods. 
 */
Http::Cookie::Cookie(const char *cookieStr) 
    : name(), value()
{
    const char *ptr;
    bool parsed = false;

    ptr = cookieStr;
    while (isToken(*ptr)) {
	ptr += 1;
    }

    if (ptr > cookieStr) {
	name.append(cookieStr, ptr - cookieStr);
	ptr = skipWhitespace(ptr);
	if ('=' == *ptr) {
	    const char *startPtr;

	    ptr = skipWhitespace(ptr + 1);
	    startPtr = ptr;
	    while (*ptr && ';' != *ptr) { 
		ptr += 1;
	    }
	    if (ptr > startPtr) {
		value.append(startPtr, ptr - startPtr);
		parsed = true;
	    }
	}
    }

    if (! parsed) {
	throw std::invalid_argument(cookieStr);
    }
}

Http::Response::~Response()
{
    delete[] bodyPtr;
    bodyPtr = NULL;
}

Http::Response::Response()
    : httpStatus(INVALID), cookieList(), extraHdrs(), bodyPtr(NULL), bodyLen(0)
{
}

/**
 * Throws: 
 *	std::bad_alloc if no memory. 
 *	NSPRException if NSPR error. 
 *	ParseException upon parse error.
 */
Http::Response::Response(Log::ModuleId logModule, Connection& conn)
    : httpStatus(INVALID), cookieList(), extraHdrs(), bodyPtr(NULL), bodyLen(0)
{
    am_status_t status;

    status = readAndParse(logModule, conn);

    if (AM_SUCCESS != status) {
	if (AM_NO_MEMORY == status) {
	    throw std::bad_alloc();
	} else if (AM_NSPR_ERROR == status) {
	    throw NSPRException("Http::Response()", "Connection::receiveData");
	} else {
	    throw ParseException(status);
	}
    }
}

static bool http_to_lower(char l, char r) {
    return (tolower(l) == tolower(r));
}

am_status_t Http::Response::readAndParse(Log::ModuleId logModule, Connection& conn) {
    am_status_t status = AM_FAILURE;
    bool contentLengthHdrSeen = false;

    int htsts = conn.httpStatusCode();

    Log::log(logModule, Log::LOG_DEBUG,
            "HTTP Status = %d", htsts);

    if (htsts != -1) {
        status = AM_SUCCESS;
        httpStatus = static_cast<Status> (htsts);
    }

    if (htsts != 200) {
        status = AM_HTTP_ERROR;
    }

    if (status == AM_SUCCESS) {
        int contentLength = conn.httpContentLength();

        if (contentLength != -1) {
            contentLengthHdrSeen = true;
        }

        std::string search("set-cookie");
        Connection::ConnHeaderMap::iterator it = conn.begin();
        Connection::ConnHeaderMap::iterator itEnd = conn.end();
        for (; it != itEnd; ++it) {
            std::string k = (*it).first;
            std::string v = (*it).second;
            std::string::iterator fpos = std::search(k.begin(), k.end(), search.begin(), search.end(), http_to_lower);
            if (fpos != k.end()) {
                cookieList.push_back(Cookie(v.c_str()));
                //Log::log(logModule, Log::LOG_MAX_DEBUG, "Set-Cookie: %s", v.c_str());
            } else {
                extraHdrs.set(k, v);
                //Log::log(logModule, Log::LOG_MAX_DEBUG, "Header: %s: %s", k.c_str(), v.c_str());
            }
        }

        if (contentLengthHdrSeen == true) {
            Log::log(logModule, Log::LOG_DEBUG,
                    "Http::Response::readAndParse(): "
                    "Reading body content of length: %d", contentLength);

            std::string body = conn.getBody();
            if (body.length() == 0) {
                status = AM_END_OF_FILE;
                bodyPtr = NULL;
            } else {
                bodyPtr = new (std::nothrow) char[body.length() + 1];
                bodyLen = body.length();
                strcpy(bodyPtr, body.c_str());
            }

        } else {
            Log::log(logModule, Log::LOG_DEBUG,
                    "Http::Response::readAndParse(): "
                    "No content length in response.");
        }
    }

    Log::log(logModule, Log::LOG_MAX_DEBUG, "Http::Response::readAndParse(): "
            "Completed processing the response with status: %s",
            am_status_to_string(status));

    return status;
}

/*
 * URL decode a string, in the same way as URLDecoder.java in j2se, 
 * to be consistent with how IS does encoding/decoding.
 *
 * Throws std::invalid_argument and other std::exception's from std::string.
 */
std::string Http::decode(const std::string& encodedString)
{
    std::size_t encodedLen = encodedString.size();
    const char *encStr = encodedString.c_str();
    std::string decodedString;
    const char *tmpStr = NULL;
    std::size_t cnt = 0;

    // Reserve enough space for the worst case.
    decodedString.reserve(encodedLen);

    // Run down the length of the encoded string, examining each
    // character.  If it's a %, we discard it, read in the next two
    // characters, convert their hex value to a char, and write
    // that to the decoded string.  Anything else, we just copy over.
    for (std::size_t i = 0; i < encodedLen; ++i) {
	char curChar = encStr[i];

	if ('+' == curChar) {
	    if(tmpStr != NULL) {
		decodedString.append(tmpStr, cnt);
		tmpStr = NULL;
		cnt = 0;
	    }
	    PUSH_BACK_CHAR(decodedString, ' ');
	} else if ('%' == curChar) {
	    if(tmpStr != NULL) {
		decodedString.append(tmpStr, cnt);
		tmpStr = NULL;
		cnt = 0;
	    }
	    if (i + 2 < encodedLen && isHexDigit(encStr[i + 1]) &&
		isHexDigit(encStr[i + 2])) {
		unsigned int value;

		value = convertHexDigit(encStr[++i]);
		value = (value * 0x10) + convertHexDigit(encStr[++i]);
		PUSH_BACK_CHAR(decodedString, static_cast<char>(value));
	    } else {
		throw std::invalid_argument(
			"Http::decode() invalid %-escapes in " +
			encodedString);
	    }
	} else {
	    if(cnt == 0)
		tmpStr = encStr + i;
	    ++cnt;
	}
    }
    if(tmpStr != NULL) {
	decodedString.append(tmpStr, cnt);
	cnt = 0;
	tmpStr = NULL;
    }

    return decodedString;
}

/*
 * URL encode a string, in the same way as URLEncoder.java in j2se, 
 * to be consistent with how IS does encoding/decoding.
 */
std::string Http::encode(const std::string& rawString)
{
    std::size_t rawLen = rawString.size();
    std::string encodedString;
    char encodingBuffer[4] = { '%', '\0', '\0', '\0' };

    encodedString.reserve(rawLen);

    for (std::size_t i = 0; i < rawLen; ++i) {
	char curChar = rawString[i];

	if (curChar == ' ') {
	    encodedString += '+';	
	} 
	else if (isAlpha(curChar) || isDigit(curChar) || 
		    isSpecial(curChar)) {
	    encodedString += curChar;
	} 
	else {
	    unsigned int temp = static_cast<unsigned int>(curChar);

	    encodingBuffer[1] = convertToHexDigit(temp / 0x10);
	    encodingBuffer[2] = convertToHexDigit(temp % 0x10);
	    encodedString += encodingBuffer;
	}
    }

    return encodedString;
}

/*
 * Eencoding of the cookies which has special characters. 
 * Useful when profile, session and response  attributes contain 
 * special chars and attributes fetch mode is set to HTTP_COOKIE.
 */
std::string Http::cookie_encode(const std::string& rawString)
{
    std::size_t rawLen = rawString.size();
    std::string encodedString;
    char encodingBuffer[4] = { '%', '\0', '\0', '\0' };

    encodedString.reserve(rawLen);

    for (std::size_t i = 0; i < rawLen; ++i) {
        char curChar = rawString[i];

        if (curChar == ' ') {
            encodedString += '+';
        }
        else if (isAlpha(curChar) || isDigit(curChar) ||
                    isSpecial(curChar)) {
            encodedString += curChar;
        }
        else {
            unsigned int temp = static_cast<unsigned int>(curChar);

            encodingBuffer[1] = convertToHexDigit((temp>>4)& 0x0f);
            encodingBuffer[2] = convertToHexDigit(temp % 0x10);
            encodedString += encodingBuffer;
        }
    }

    return encodedString;
}
