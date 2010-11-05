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
 * $Id: service_info.cpp,v 1.4 2008/06/25 08:14:36 qcheng Exp $
 *
 */ 
#include <cstring>
#include <stdexcept>

#include "utils.h"
#include "service_info.h"

USING_PRIVATE_NAMESPACE

ServiceInfo::ServiceInfo()
    : serverList()
{
}

/* Throws std::invalid_argument if the input argument is invalid */
ServiceInfo::ServiceInfo(const char *serverString)
    : serverList(ServerListType())
{
    if (NULL == serverString) {
	throw std::invalid_argument("ServiceInfo() serviceList is NULL");
    }

    parseServerList(serverString);
}

ServiceInfo::ServiceInfo(const std::string& serverString)
    : serverList(ServerListType())
{
    parseServerList(serverString.c_str());
}

/* Throws std::invalid_argument if the input argument is invalid */
void ServiceInfo::setFromString(const char *serverString)
{
    if (NULL == serverString) {
	throw std::invalid_argument("ServiceInfo() serviceList is NULL");
    }

    parseServerList(serverString);
}

void ServiceInfo::setFromString(const std::string& serverString)
{
    parseServerList(serverString.c_str());
}

void ServiceInfo::parseServerList(const char *server_string)
{
    std::vector<ServerInfo> newServerList;
    newServerList.clear();
    std::string serverStr(server_string);
    Utils::trim(serverStr);
    const char *serverString = serverStr.c_str();
    
    while (*serverString) {
	const char *endOfServer = serverString;
	while(*endOfServer && isspace(*endOfServer)) endOfServer++;

	serverString = endOfServer;

	while(*endOfServer && !isspace(*endOfServer)) endOfServer++;

	std::size_t len;

	if (endOfServer) {
	    len = endOfServer - serverString;
	} else {
	    len = std::strlen(serverString);
	}

	if (len > 0) {
	    newServerList.push_back(ServerInfo(serverString, len));
	}

	serverString = endOfServer;
    }

    // Now that we have successfully parsed the provided string,
    // update the member field.
    serverList = newServerList;
}

void ServiceInfo::addServer(const ServerInfo& server)
{
    serverList.push_back(server);
}

/* Throws std::invalid_argument if any argument is invalid */
void ServiceInfo::addServer(const char *serverInfoString, std::size_t len)
{
    if (NULL == serverInfoString) {
	throw std::invalid_argument(
			"ServiceInfo::addServer() serverInfoString is NULL");
    }

    if (0 == len) {
	len = std::strlen(serverInfoString);
    }

    addServer(ServerInfo(serverInfoString, len));
}

void ServiceInfo::addServer(const std::string& serverInfoString)
{
    addServer(ServerInfo(serverInfoString.c_str(), serverInfoString.size()));
}

// sets the given host and port in all the servers in the list.
void ServiceInfo::setHostPort(const ServerInfo& serverInfo) 
{
    const std::string& host = serverInfo.getHost();
    unsigned short port = serverInfo.getPort();
    bool use_ssl = serverInfo.useSSL();
    ServerListType::iterator iter;
    for (iter = serverList.begin(); iter != serverList.end(); ++iter) {
	(*iter).setHost(host);
	(*iter).setPort(port);
	(*iter).setUseSSL(use_ssl);
    }
}
