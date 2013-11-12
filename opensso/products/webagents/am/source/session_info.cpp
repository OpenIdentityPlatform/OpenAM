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
 * $Id: session_info.cpp,v 1.3 2008/06/25 08:14:36 qcheng Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include "http.h"
#include "session_info.h"
#include "xml_tree.h"

USING_PRIVATE_NAMESPACE


const std::string SessionInfo::HOST_IP("Host");


/* Throws 
 *	XMLTree::ParseException on XML parse error 
 */
void SessionInfo::parseAttributes(XMLElement element)
{
    bool foundAll = true;
    std::string missingAttr;
    std::string value;

    if (! element.getAttributeValue("sid", value)) {
	foundAll = false;
	missingAttr = "sid";
    } else {
	ssoToken.token = value;
	ssoToken.encodedToken = Http::encode(value);
    }

    if (! element.getAttributeValue("stype", sessionType)) {
	foundAll = false;
	missingAttr = "stype";
    }
    if (! element.getAttributeValue("cid", id)) {
	foundAll = false;
	missingAttr = "cid";
    }
    if (! element.getAttributeValue("cdomain", domain)) {
	foundAll = false;
	missingAttr = "cdomain";
    }
    if (! element.getAttributeValue("maxtime", value) ||
	1 != sscanf(value.c_str(), "%lld", &maxSessionTime)) {
	foundAll = false;
	missingAttr = "maxtime";
    }
    if (! element.getAttributeValue("maxidle", value) ||
	1 != sscanf(value.c_str(), "%lld", &maxIdleTime)) {
	foundAll = false;
	missingAttr = "maxidle";
    }
    if (! element.getAttributeValue("maxcaching", value) ||
	1 != sscanf(value.c_str(), "%lld", &maxCachingTime)) {
	foundAll = false;
	missingAttr = "maxcaching";
    }
    if (! element.getAttributeValue("timeidle", value) ||
	1 != sscanf(value.c_str(), "%lld", &idleTime)) {
	foundAll = false;
	missingAttr = "timeidle";
    }
    if (! element.getAttributeValue("timeleft", value) ||
	1 != sscanf(value.c_str(), "%lld", &remainingSessionTime)) {
	foundAll = false;
	missingAttr = "timeleft";
    }
    if (element.getAttributeValue("state", value)) {
	if (value == "valid") {
	    state = SessionInfo::VALID;
	} else if (value == "invalid") {
	    state = SessionInfo::INVALID;
	} else if (value == "inactive") {
	    state = SessionInfo::INACTIVE;
	} else if (value == "destroyed") {
	    state = SessionInfo::DESTROYED;
	} else {
	    foundAll = false;
	    missingAttr = "state has invalid value";
	}
    } else {
	foundAll = false;
	missingAttr = "state";
    }

    if (! foundAll) {
	throw XMLTree::ParseException(std::string("Session attribute missing "
						  "or invalid: ") +
				      missingAttr);
    }
}

/* Throws 
 *	XMLTree::ParseException on XML parse error 
 */
void SessionInfo::parseXML(XMLElement element)
{
    std::string name;
    std::string value;

    parseAttributes(element);

    element = element.getFirstSubElement();
    while (element.isNamed("Property")) {
	if (element.getAttributeValue("name", name) &&
	    element.getAttributeValue("value", value)) {
	    properties.set(name, value);
	} else {
	    throw XMLTree::ParseException("Property missing name or value "
					  "attribute");
	}
	element.nextSibling();
    }

    if (element.isValid()) {
	element.getName(name);
	throw XMLTree::ParseException("unexpected element in Session:" + name);
    }
}
