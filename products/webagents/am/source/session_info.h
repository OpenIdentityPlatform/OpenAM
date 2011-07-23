/* The contents of this file are subject to the terms
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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 *
 *
 * Abstract:
 *
 * Container for the information returned by the DSAME Session Service.
 *
 */

#ifndef SESSION_INFO_H
#define SESSION_INFO_H

#include <string>

#include <prtime.h>

#include "internal_macros.h"
#include "properties.h"
#include "sso_token.h"
#include "xml_element.h"

BEGIN_PRIVATE_NAMESPACE

class SessionInfo {
public:
    enum State { INVALID, VALID, INACTIVE, DESTROYED };

    static const std::string HOST_IP;

    SessionInfo()
	: ssoToken(), sessionType(), id(), domain(), maxSessionTime(0),
	  maxIdleTime(0), maxCachingTime(0), remainingSessionTime(),
	  idleTime(0), state(INVALID), properties()
    {
    }

    SessionInfo(const SessionInfo& copy) :
	ssoToken(copy.ssoToken),
	sessionType(copy.sessionType),
	id(copy.id),
	domain(copy.domain),
	maxSessionTime(copy.maxSessionTime),
	maxIdleTime(copy.maxIdleTime),
	maxCachingTime(copy.maxCachingTime),
	remainingSessionTime(copy.remainingSessionTime),
	idleTime(copy.idleTime),
	state(copy.state),
	properties(copy.properties)
    { }

    const SSOToken& getSSOToken() const { 
        return ssoToken; 
    }
    SSOToken& getSSOToken() { 
        return ssoToken; 
    }
    const std::string& getSessionType() const { return sessionType; }
    const std::string& getId() const { return id; }
    const std::string& getDomain() const { return domain; }
    PRTime getMaxSessionTime() const { return maxSessionTime; }
    PRTime getMaxIdleTime() const { return maxIdleTime; }
    PRTime getMaxCachingTime() const { return maxCachingTime; }
    PRTime getRemainingSessionTime() const { return remainingSessionTime; }
    PRTime getIdleTime() const { return idleTime; }
    State getState() const { return state; }
    bool isValid() const { return state == VALID; }
    const Properties& getProperties() const { return properties; }

protected:
    void setState(SessionInfo::State newState) { state = newState; }

private:
    friend class SessionService;
    friend class SSOTokenService;
    friend class SSOTokenListenerThreadFunc;

    /* The following functions throw XMLTree::ParseException 
     * on XML parse error */
    void parseXML(XMLElement element);
    void parseAttributes(XMLElement element);

    SSOToken ssoToken;
    std::string sessionType;
    std::string id;
    std::string domain;
    PRTime maxSessionTime;
    PRTime maxIdleTime;
    PRTime maxCachingTime;
    PRTime remainingSessionTime;
    PRTime idleTime;
    State state;
    Properties properties;
};

END_PRIVATE_NAMESPACE

#endif	// not SESSION_INFO_H
