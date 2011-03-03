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
 * $Id: fqdn_handler.cpp,v 1.3 2008/06/25 08:14:31 qcheng Exp $
 *
 */ 

#include "fqdn_handler.h"
#include "url.h"
#include <am_web.h>

USING_PRIVATE_NAMESPACE

using std::string;

const std::string
FqdnHandler::getValidFqdnResource(const char *resName) {
    string retVal;
    
    if(resName != NULL) {
	try {
	    URL url(resName);
	    Properties::const_iterator iter = fqdnMap.find(url.getHost());
	    if(iter != fqdnMap.end()) {
		url.setHost(iter->second);
	        Log::log(logID, Log::LOG_DEBUG, 
			 "setHost(%s)", (iter->second).c_str());
	    }
	    else{
		url.setHost(fqdnDefault);
		Log::log(logID, Log::LOG_DEBUG, 
			 "setHost(%s)", fqdnDefault.c_str());
	    }
	    url.getURLString(retVal);
	} 
	catch (std::exception& exs) {
	    Log::log(logID, Log::LOG_WARNING,
		     "FqdnHandler::getValidFqdnResource() "
		     "Exception caught while processing "
		     "resource => %s : %s ", resName, exs.what());
	}
	catch (...) {
	    Log::log(logID, Log::LOG_WARNING,
		     "FqdnHandler::getValidFqdnResource() "
		     "Exception caught while processing "
		     "resource => %s", resName);
	}
    }
    
    if (Log::isLevelEnabled(logID, Log::LOG_MAX_DEBUG))
    {
        Log::log(logID, Log::LOG_MAX_DEBUG,
                 "FqdnHandler::getValidFqdnResource() "
                 "Resource => %s, result => %s",
                 resName, retVal.c_str());
    }

    return retVal;
}

bool
FqdnHandler::isValidFqdnResource(const char *resName)
{
    bool result = false;
    if(resName != NULL) {
	URL url(resName);
	const string hostName = url.getHost();
	const char *host_name = hostName.c_str();
	const char *fqdn_default = fqdnDefault.c_str();

	// check if it's the default fqdn
	if(ignore_case) {
	    result = (strcasecmp(host_name, fqdn_default)==0);
	} else {
	    result = (strcmp(host_name, fqdn_default)==0);
	}
	// if not, check if it's another valid fqdn
	if (!result && validFqdns.size() > 0) {
	    std::vector<std::string>::const_iterator iter = validFqdns.begin();
	    std::vector<std::string>::const_iterator end = validFqdns.end();

	    for ( ; iter != end; iter++) {
		const char *valid_host_name = (*iter).c_str();
		Log::log(logID, Log::LOG_MAX_DEBUG,
			 "FqdnHandler::isValidFqdnResource() "
			 "comparing valid host name [%s] with [%s]", 
			 valid_host_name==NULL?"(null)":valid_host_name, 
			 host_name==NULL?"(null)":host_name);
		if (ignore_case) { 
		    result = (strcasecmp(valid_host_name, host_name)==0);
		} else {
		    result = (strcmp(valid_host_name, host_name)==0);
		}
		if (result)
		    break;
	    }
	}
	// if not, check if it's in a fqdn map.
	if (!result) {
	    result = (fqdnMap.vfind(hostName) != fqdnMap.end());
	}
    }

    if (Log::isLevelEnabled(logID, Log::LOG_MAX_DEBUG)) {
        Log::log(logID, Log::LOG_MAX_DEBUG,
                 "FqdnHandler::isValidFqdnResource() "
                 "Resource => %s, is valid => %s",
                 resName, (result)?"true":"false");
    }

    return result;
}

void
FqdnHandler::parsePropertyKeyValue(const std::string &property,
				   const char kvpsep,
				   const char kvsep) 
{
    size_t coma = 0, curPos = 0;
    std::string attrs(property);

    Utils::trim(attrs);
    size_t len = attrs.size();
    std::string valid("valid");

    while(coma < len) {
	coma = attrs.find(kvpsep, curPos);
	std::string attrPair = attrs.substr(curPos, coma - curPos);

	while(coma < len && attrs.at(coma) == kvpsep) coma++;

	curPos = coma;

	Utils::trim(attrPair);
	if(attrPair.size() == 0)
	    continue;

	size_t pipe = attrPair.find(kvsep);
	if(pipe != std::string::npos) {
	    std::string key = attrPair.substr(0, pipe);
	    std::string value = attrPair.substr(pipe + 1);

	    Utils::trim(key);
	    Utils::trim(value);
	    if (key.compare(valid) == 0) {
	        validFqdns.push_back(value);
	    } else {
	        fqdnMap.set(key, value);
	    }
	}
    }
    return;
}


FqdnHandler::FqdnHandler(const Properties& properties,
			 bool icase,
                         Log::ModuleId module_id): logID(module_id),
						   ignore_case(icase),
						   fqdnMap(icase) {
    fqdnDefault = properties.get(AM_WEB_FQDN_DEFAULT);

    parsePropertyKeyValue(properties.get(AM_WEB_FQDN_MAP, ""), ',', '|');

    if (Log::isLevelEnabled(logID, Log::LOG_MAX_DEBUG))
    {
        Log::log(logID, Log::LOG_MAX_DEBUG,
                 "FqdnHandler::FqdnHandler() fqdnMap size => %d",
                 fqdnMap.size());
    }

    if (fqdnMap.empty()) {
        Log::log(logID, Log::LOG_WARNING,
                 "FqdnHandler::FqdnHandler() No value specified for "
                 "fqdnMap.");
    }

    // Log all the settings that have been read so far
    if (Log::isLevelEnabled(logID, Log::LOG_DEBUG))
    {
        Log::log(logID, Log::LOG_MAX_DEBUG,
                 "FqdnHandler::FqdnHandler() Fqdn Default => %s",
                 fqdnDefault.c_str());
	if (validFqdns.size() > 0) {
	    std::string msg = "FqdnHandler::FqdnHandler() Valid Fqdns:";
	    std::vector<std::string>::const_iterator iter = validFqdns.begin();
	    std::vector<std::string>::const_iterator end = validFqdns.end();
	    for ( ; iter != end; iter++) {
		msg.append(" ");
		msg.append(*iter);
	    }
            Log::log(logID, Log::LOG_DEBUG, msg.c_str());
	}

	Properties::const_iterator iter = fqdnMap.begin();
        for(; iter != fqdnMap.end(); iter ++) {
	    const Properties::key_type &k_iter = iter->first;
	    const Properties::mapped_type &v_iter = iter->second;

            Log::log(logID, Log::LOG_DEBUG,
                     "FqdnHandler::FqdnHandler() "
                     "[%s] => {%s}", k_iter.c_str(),
		     v_iter.c_str());
        }

    }
}
