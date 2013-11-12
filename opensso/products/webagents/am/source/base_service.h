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
 * $Id: base_service.h,v 1.5 2008/06/25 08:14:31 qcheng Exp $
 *
 *
 * Abstract:
 *
 * Common base class for all of the service classes that communicate
 * with DSAME services.
 *
 */
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */

#ifndef BASE_SERVICE_H
#define BASE_SERVICE_H

#include <string>
#include <vector>

#include "am_types.h"
#include "connection.h"
#include "http.h"
#include "internal_macros.h"
#include "log.h"
#include "mutex.h"
#include "service_info.h"

BEGIN_PRIVATE_NAMESPACE

class BaseService {
protected:
    struct BodyChunk {
	BodyChunk() : data(), secure(false) {}
	BodyChunk(const char *str, std::size_t str_len,
		  bool hide = false): data(str, str_len), secure(hide)
	{
	}
	explicit BodyChunk(const std::string& rhs,
			   bool hide = false):data(rhs), secure(hide)
	{
	}

	BodyChunk& operator=(const std::string& rhs)
	{
	    data = rhs;
	    return *this;
	}

	BodyChunk(const BodyChunk &tbcopied):data(tbcopied.data),
					     secure(tbcopied.secure){}
	std::string data;
	bool secure;
    };
    typedef std::vector<BodyChunk> BodyChunkList;

    class Request;
    friend class Request;
    class Request {
    public:
	// Enough space hold an unsigned 32 bit number
	enum { ID_BUF_LEN = 12 };

	Request(BaseService& service, const BodyChunk& globalPrefixChunk,
		const BodyChunk& servicePrefixChunk,
		std::size_t numExtraElements, bool addServiceID = true);
	~Request() { delete[] extraData; }

	BodyChunkList& getBodyChunkList() { return bodyChunkList; }

	const char *getGlobalId() const { return globalIdBuf; }
	const char *getServiceId() const { return serviceIdBuf; }

	std::size_t getNextServiceRequestIdAsString(char *buffer,
						    std::size_t bufferLen);

	void setExtraData(char *data) { extraData = data; }

    private:
	BaseService& service;
	char globalIdBuf[ID_BUF_LEN];
	char serviceIdBuf[ID_BUF_LEN];
	BodyChunkList bodyChunkList;
	char *extraData;
    };

    BaseService(const std::string& name,
            const Properties& props);
    virtual ~BaseService() = 0;

    am_status_t doHttpGet(const ServiceInfo& service,
			  const std::string& uriParameters,
			  const Http::CookieList& cookieList,
			  Http::Response& response,
			  const ServerInfo** serverInfo = NULL) const;

    am_status_t doHttpPost(const ServiceInfo& service,
			   const std::string& uriParameters,
			   const Http::CookieList& cookieList,
			   const BodyChunkList& bodyChunks,
			   Http::Response& response,
			   bool doFormPost = false,
			   bool checkHTTPRetCode = true,
			   const ServerInfo** serverInfo = NULL) const;

    std::vector<std::string>
    parseGenericResponse(const Http::Response& response,
			 const char *expectedId) const;

    Log::ModuleId logModule;
    bool getUseProxy() { return useProxy; }
    bool getUseAuth() { return useProxyAuth; }
    const char * getProxyHost() { return proxyHost.c_str(); }
    unsigned short getProxyPort() { return proxyPort; }



private:
    typedef unsigned int IdType;

    static IdType getNextGlobalRequestId();
    IdType getNextServiceRequestId();
    
    am_status_t sendRequest(Connection& conn,
			    const BodyChunk& headerPrefix,
			    const std::string& uri,
			    const std::string& uriParameters,
			    const Http::HeaderList& headerList,
			    const Http::CookieList& cookieList,
			    const BodyChunk& contentLine,
			    const BodyChunk& headerSuffix,
			    const BodyChunkList& bodyChunkList) const;

    am_status_t doRequest(const ServiceInfo& service,
			  const BodyChunk& headerPrefix,
			  const std::string& uriParameters,
			  const Http::CookieList& cookieList,
			  const BodyChunk& headerSuffix,
			  const BodyChunkList& bodyChunkList,
			  Http::Response& response,
			  const ServerInfo** serverInfo) const;

    static Mutex classLock;
    static IdType globalRequestId;

    Mutex objLock;
    IdType serviceRequestId;
    std::string poll_primary_server;
    
    /* proxy parameters */
    bool useProxy;
    bool useProxyAuth;
    std::string proxyHost;
    unsigned short proxyPort;
    std::string proxyUser;
    std::string proxyPassword;


};

END_PRIVATE_NAMESPACE

#endif	/* not BASE_SERVICE_H */
