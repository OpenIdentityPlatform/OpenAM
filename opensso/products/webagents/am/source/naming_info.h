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
 * $Id: naming_info.h,v 1.5 2008/06/25 08:14:33 qcheng Exp $
 *
 *
 * Abstract:
 *
 * Container for the information returned by the AM Naming Service.
 *
 */ 

#ifndef NAMING_INFO_H
#define NAMING_INFO_H

#include <string>

#include "internal_macros.h"
#include "properties.h"
#include "service_info.h"

BEGIN_PRIVATE_NAMESPACE

class NamingInfo {
public:
    NamingInfo(bool valid = false)
	: infoValid(valid), loggingSvcInfo(), policySvcInfo(),
	  profileSvcInfo(), sessionSvcInfo(), restSvcInfo(), extraProperties()
    {
    }

    const ServiceInfo& getLoggingSvcInfo() const { return loggingSvcInfo; }
    const ServiceInfo& getPolicySvcInfo() const { return policySvcInfo; }
    const ServiceInfo& getProfileSvcInfo() const { return profileSvcInfo; }
    const ServiceInfo& getSessionSvcInfo() const { return sessionSvcInfo; }
    const ServiceInfo& getRESTSvcInfo() const { return restSvcInfo; }

    const Properties& getExtraProperties() const { return extraProperties; }
    const std::string getlbCookieStr() const { return lbCookieStr; }
    const std::string getlbCookieName() const { return lbCookieName; }
    const std::string getlbCookieValue() const { return lbCookieValue; }
        
    bool isValid() {
	return infoValid;
    }

    void setHostPort(const ServerInfo& serverInfo) {
	loggingSvcInfo.setHostPort(serverInfo);
	policySvcInfo.setHostPort(serverInfo);
	profileSvcInfo.setHostPort(serverInfo);
	sessionSvcInfo.setHostPort(serverInfo);
	restSvcInfo.setHostPort(serverInfo);
    }
    
private:
    bool infoValid;
    friend class NamingService;
    friend class AgentProfileService;

    ServiceInfo loggingSvcInfo;
    ServiceInfo policySvcInfo;
    ServiceInfo profileSvcInfo;
    ServiceInfo sessionSvcInfo;
    ServiceInfo restSvcInfo;
    Properties extraProperties;
    std::string lbCookieStr; 
    std::string lbCookieName;
    std::string lbCookieValue;
};

END_PRIVATE_NAMESPACE

#endif	// not NAMING_INFO_H
