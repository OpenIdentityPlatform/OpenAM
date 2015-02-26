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
    const char HTTP_REPLY_VERSION[] = "http/1.1";
    const char CONTENT_LENGTH_HDR[] = "content-length:";
    const char SET_COOKIE_HDR[] = "set-cookie:";

    class LineBuffer {
    public:
	LineBuffer(Connection& connArg)
	    : conn(connArg), data(), len(0), offset(0), eof(false)
	{
	    data[len] = '\0';
	}

	am_status_t getLine(const char *& linePtr);
	std::size_t getDataLen() const { return len - offset; }

	void extractRemainingData(char *dest)
	{
	    std::memcpy(dest, &data[offset], getDataLen());
	    offset = len;
	}

    private:
	am_status_t fill();
	am_status_t findEndOfLine(char *& eolPtr);

	Connection& conn;
	// This buffer is sized to be large enough to hold the least
	// allowable maximum size for a cookie: 4k.  This allows some
	// extra room for the domain and path parameters.  Enough?
	char data[5000];
	std::size_t len;
	std::size_t offset;
	bool eof;
    };

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

am_status_t LineBuffer::fill()
{
    am_status_t status;
    std::size_t readLen;

    if (0 < offset && offset < len) {
	len -= offset;
	std::memmove(data, &data[offset], len);
	offset = 0;
	data[len] = '\0';
    }

    if (! eof) {
	readLen = sizeof(data) - len - 1;
	if (readLen > 0) {
	    status = conn.receiveData(&data[len], readLen);
	    if (AM_SUCCESS == status) {
		if (readLen > 0) {
		    len += readLen;
		    data[len] = '\0';
		} else {
		    eof = true;
		}
	    } else {
		data[len] = '\0';
	    }
	} else {
	    status = AM_BUFFER_TOO_SMALL;
	}
    } else {
	status = AM_END_OF_FILE;
    }

    return status;
}

am_status_t LineBuffer::findEndOfLine(char *& eolPtr)
{
    am_status_t status = AM_SUCCESS;

    eolPtr = std::strchr(&data[offset], '\n');
    while (NULL == eolPtr) {
	status = fill();
	if (AM_SUCCESS == status) {
	    eolPtr = std::strchr(&data[offset], '\n');
	} else {
	    break;
	}
    }

    if (NULL == eolPtr) {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		 "LineBuffer::findEndOfLine(): %s", &data[offset]);

	status = AM_NOT_FOUND;
    }

    return status;
}

am_status_t LineBuffer::getLine(const char *& linePtr)
{
    am_status_t status;
    char *eolPtr = NULL;

    status = findEndOfLine(eolPtr);
    while (AM_SUCCESS == status && isWhitespace(eolPtr[1])) {
	// This line is folded onto the next line, so we need to replace
	// the [<CR>]<LF>1*(<SP>|<HT>) with <SP>, copy the rest of the
	// data (including the terminating NUL) forward, and then resume
	// searching for the end of the line.
	char *startOfLine = eolPtr + 2;

	while (isWhitespace(*startOfLine)) {
	    startOfLine += 1;
	}
	if (eolPtr > data && '\r' == eolPtr[-1]) {
	    eolPtr[-1] = ' ';
	} else {
	    *(eolPtr++) = ' ';
	}

	std::memmove(eolPtr, startOfLine, len - (startOfLine - data) + 1);
	len -= (startOfLine - eolPtr);
	status = findEndOfLine(eolPtr);
    }

    if (AM_SUCCESS == status) {
	// NUL terminate the line and move the offset to
	// point to just after the <CR><LF> pair.
	if (eolPtr > data && '\r' == eolPtr[-1]) {
	    eolPtr[-1] = '\0';
	} else {
	    *eolPtr = '\0';
	}
	linePtr = &data[offset];
	offset = (eolPtr - data) + 1;
    }

    return status;
}

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
Http::Response::Response(Log::ModuleId logModule, Connection& conn,
			 std::size_t initialBufferLen)
    : httpStatus(INVALID), cookieList(), extraHdrs(), bodyPtr(NULL), bodyLen(0)
{
    am_status_t status;

    status = readAndParse(logModule, conn, initialBufferLen);

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

am_status_t Http::Response::readAndParse(Log::ModuleId logModule,
					    Connection& conn,
					    std::size_t initialBufferLen)
{
    am_status_t status;
    LineBuffer buffer(conn);
    const char *linePtr;
    PRInt32 contentLength = 0;
    bool contentLengthHdrSeen = false;

    status = buffer.getLine(linePtr);
    if (AM_SUCCESS == status) {
	linePtr += sizeof(HTTP_REPLY_VERSION) - 1;

	linePtr = skipWhitespace(linePtr);

	if (isDigit(*linePtr) && isDigit(linePtr[1]) &&
	    isDigit(linePtr[2])) {
	    unsigned int value;

	    value = (*linePtr - '0') * 100;
	    value += (linePtr[1] - '0') * 10;
	    value += linePtr[2] - '0';
	    httpStatus = static_cast<Status>(value);
	    linePtr += 3;

	    linePtr = skipWhitespace(linePtr);

	    Log::log(logModule, Log::LOG_DEBUG,
		     "HTTP Status = %d (%s)", httpStatus, linePtr);
	} else {
	  Log::log(logModule, Log::LOG_ERROR, "Http::Response::readAndParse(): "
		   "Unable to retrieve status header from server.");
	    status = AM_FAILURE;
	}
    }

    Log::log(logModule, Log::LOG_MAX_DEBUG,
	     "Http::Response::readAndParse(): Reading headers.");

    if (AM_SUCCESS == status) {
	for (status = buffer.getLine(linePtr);
	     AM_SUCCESS == status;
	     status = buffer.getLine(linePtr)) {
	    std::size_t len = std::strlen(linePtr);

	    if (0 < len) {
		if (hasPrefixIgnoringCase(linePtr, CONTENT_LENGTH_HDR)) {
		    linePtr += sizeof(CONTENT_LENGTH_HDR) - 1;
		    linePtr = skipWhitespace(linePtr);
		    if (PR_sscanf(linePtr, "%u", &contentLength) == 1) {
			contentLengthHdrSeen = true;
		    }
		} else if (hasPrefixIgnoringCase(linePtr, SET_COOKIE_HDR)) {
		    linePtr += sizeof(SET_COOKIE_HDR) - 1;
		    linePtr = skipWhitespace(linePtr);

		    try {
			cookieList.push_back(Cookie(linePtr));
			Log::log(logModule, Log::LOG_MAX_DEBUG,
				 "%s %s", SET_COOKIE_HDR, linePtr);
		    } catch (...) {
			// Could not parse the header.  Log it and go on?
			Log::log(logModule, Log::LOG_INFO,
				 "Unparsable Set-Cookie header: %s", linePtr);
			continue;
		    }

		} else {
		    const char *colonPtr = std::strchr(linePtr, ':');

		    if (colonPtr) {
			std::string key(linePtr, colonPtr - linePtr);

			extraHdrs.set(key, skipWhitespace(colonPtr + 1));
			Log::log(logModule, Log::LOG_MAX_DEBUG, "%s", linePtr);
		    } else {
			// XXX - Replace this with proper processing
			Log::log(logModule, Log::LOG_WARNING,
				 "Unparsable header: %s", linePtr);
		    }
		}
	    } else {
		// End of HTTP response headers
		break;
	    }
	}
    }

    if(contentLengthHdrSeen == true) {
	Log::log(logModule, Log::LOG_MAX_DEBUG,
		 "Http::Response::readAndParse(): "
		 "Reading body content of length: %llu", contentLength);
    } else {
	Log::log(logModule, Log::LOG_DEBUG,
		 "Http::Response::readAndParse(): "
		 "No content length in response.");
    }

    if (AM_SUCCESS == status) {
	bodyLen = buffer.getDataLen();

	if (contentLength >= initialBufferLen) {
	    initialBufferLen = contentLength + 1;
	}
	if (bodyLen > initialBufferLen) {
	    initialBufferLen = bodyLen + 1;
	}

	bodyPtr = new (std::nothrow) char[initialBufferLen];
	if (NULL != bodyPtr) {
	    if (0 < bodyLen) {
		buffer.extractRemainingData(bodyPtr);
	    }

	    status = conn.waitForReply(bodyPtr, initialBufferLen, bodyLen,
				       bodyLen);
	    if (AM_SUCCESS != status) {
		delete[] bodyPtr;
		bodyPtr = NULL;
	    }
	} else {
	    status = AM_NO_MEMORY;
	}
    }

    Log::log(logModule, Log::LOG_MAX_DEBUG, "Http::Response::readAndParse(): "
	     "Completed processing the response with status: %s",
	     am_status_to_string(status));

    return status;
}

/**
 * Get the HTTP response and discard it
 */
void Http::Response::readAndIgnore(Log::ModuleId logModule,
                                   Connection& conn)
{
    LineBuffer buffer(conn);
    const char *linePtr;

    while (AM_SUCCESS == buffer.getLine(linePtr) && strlen(linePtr) > 0) {
        Log::log(logModule, Log::LOG_MAX_DEBUG,
                 "Http::Response::readAndIgnore(): %s",
                 linePtr);
    }
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
