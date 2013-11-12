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
 * $Id: server_info.cpp,v 1.5 2008/09/13 01:11:53 robertis Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <stdio.h>
#include <climits>
#include <cstring>
#include <stdexcept>

#include "server_info.h"
#include "url.h"

USING_PRIVATE_NAMESPACE

namespace {
    const std::string protocolSeparators("://");
    const std::string portSeparator(":");

    const std::size_t MAX_PORT_LENGTH = 5;
    const unsigned short HTTP_PORT = 80;
    const unsigned short HTTPS_PORT = 443;

    /* Throws std::invalid_argument with a message containing the invalid url */
    inline void invalidURL(const char *message, const char *url,
			   std::size_t len)
    {
	std::string msg("ServerInfo::parseURL() ");

	msg += message;
	msg += ": ";
	msg.append(url, len);

	throw std::invalid_argument(msg);
    }
}

const std::string ServerInfo::http("http");
const std::string ServerInfo::https("https");

/* Throws std::invalid_argument if url is invalid */
ServerInfo::ServerInfo(const char *theURL, std::size_t len)
    : host(), port(0), use_ssl(false), uri(), healthy(true), excludeTime(0), url()
{
    if (NULL == theURL) {
	throw std::invalid_argument("ServerInfo() url is NULL");
    }

    if (0 == len) {
	len = std::strlen(theURL);
    }

    parseURL(theURL, len);
}

ServerInfo::ServerInfo(const std::string& theURL)
    : host(), port(0), use_ssl(false), uri(), healthy(true), excludeTime(0), url()
{
    parseURL(theURL.c_str(), theURL.size());
}

/* Throws std::invalid_argument if url is invalid */
void ServerInfo::setFromString(const char *theURL, std::size_t len)
{
    if (NULL == theURL) {
	throw std::invalid_argument("ServerInfo.setFromString() url is NULL");
    }

    if (0 == len) {
	len = std::strlen(theURL);
    }

    parseURL(theURL, len);
}

void ServerInfo::setFromString(const std::string& theURL)
{
    parseURL(theURL.c_str(), theURL.size());
}

void ServerInfo::parseURL(const char *theURL, std::size_t len)
{
    std::size_t offset = 0;
    bool urlUsesSSL;

    // Check that the length of the infoString is at least as long as the
    // shortest permissible URL.  This check allows us to safely execute
    // all of the checks in the following if statement without worrying
    // about running off the end of the buffer.  The comparison string
    // is only significant for being a minimally valid URL.
    if (len - offset < MIN_URL_LEN) {
	invalidURL("URL too short", theURL, len);
    }

    if ((theURL[offset] == 'h' || theURL[offset] == 'H') &&
	(theURL[++offset] == 't' || theURL[offset] == 'T') &&
	(theURL[++offset] == 't' || theURL[offset] == 'T') &&
	(theURL[++offset] == 'p' || theURL[offset] == 'P')) {
	if (theURL[offset + 1] == 's' || theURL[offset + 1] == 'S') {
	    urlUsesSSL = true;
	    offset += 2;
	} else {
	    urlUsesSSL = false;
	    offset += 1;
	}

	if (theURL[offset] == ':' && theURL[offset + 1] == '/' &&
	    theURL[offset + 2] == '/') {
	    offset += 3;

	    std::size_t startOfHost = offset;
	    while (offset < len && ':' != theURL[offset] && '/' != theURL[offset]) {
		offset += 1;
	    }

	    std::size_t hostLen = offset - startOfHost;
	    if (hostLen > 0) {
		std::string newHost(&theURL[startOfHost], hostLen);
		unsigned short newPort = urlUsesSSL ? HTTPS_PORT : HTTP_PORT;
		std::string newURI("/");

		if (offset < len) {
		    if (':' == theURL[offset]) {
			offset += 1;

			std::size_t startOfPort = offset;
			while (offset < len && '0' <= theURL[offset] &&
			    '9' >= theURL[offset]) {
			    offset += 1;
			}

			// NOTE: RFC NNNN allows a URL to have the following
			// form: http://host:/, i.e. port separator present
			// but no port number specified.
			if (offset - startOfPort > 0) {
			    unsigned int value = 0;

			    for (std::size_t i = startOfPort; i < offset; ++i){
				value = (value * 10) + (theURL[i] - '0');
			    }
			    if (value <= USHRT_MAX) {
				newPort = static_cast<unsigned short>(value);
			    } else {
				invalidURL("invalid port number", theURL, len);
			    }
			}
		    }

		    if (offset < len) {
			if ('/' == theURL[offset++]) {
			    newURI.append(&theURL[offset], len - offset);
			} else {
			    invalidURL("invalid character in port number",
				       theURL, len);
			}
		    }
		}
		// We have successfully parsed the URL, so now we can
		// assign the bits and pieces to the member fields, without
		// worrying about corrupting the object, since none of
		// these operations will generate an exception.
		host.swap(newHost);
		port = newPort;
		use_ssl = urlUsesSSL;
		uri.swap(newURI);
		this->url.append(theURL, len);
	    } else {
		invalidURL("missing host name", theURL, len);
	    }
	} else {
	    invalidURL("unable to parse protocol terminator", theURL, len);
	}
    } else {
	invalidURL("unable to parse protocol", theURL, len);
    }
}

void ServerInfo::setURI(const std::string& newURI)
{
    if (newURI.size() == 0) {
	uri = "/";
    } else {
	uri = newURI;
    }
}

std::string ServerInfo::toString() const
{
    char portBuf[MAX_PORT_LENGTH + 1];
    std::string result;

    result.reserve(getProtocol().size() + protocolSeparators.size() +
		   host.size() + portSeparator.size() + MAX_PORT_LENGTH +
		   uri.size());

    result.append(getProtocol());
    result.append(protocolSeparators);
    result.append(host);
    result.append(portSeparator);
    snprintf(portBuf, sizeof(portBuf), "%u", port);
    result.append(portBuf);
    result.append(uri);

    return result;
}
