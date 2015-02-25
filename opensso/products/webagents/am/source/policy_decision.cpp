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
 * $Id: policy_decision.cpp,v 1.5 2008/06/25 08:14:34 qcheng Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <string>
#include <am_types.h>
#include <time.h>
#include <climits>
#include "policy_resource.h"
#include "xml_element.h"
#include "utils.h"
#include "policy_decision.h"
#include "action_decision.h"
#include "properties.h"


USING_PRIVATE_NAMESPACE
using std::string;
extern unsigned long policy_clock_skew;

const PDRefCntPtr PolicyDecision::INVALID_POLICY_DECISION;

PolicyDecision::~PolicyDecision() {
    std::map<std::string, ActionDecision *>::iterator iter = actionDecisions.begin();
    for(; iter != actionDecisions.end(); iter++) {
	delete (*iter).second;
    }
}

/**
 * Throws InternalException upon error.
 */
PDRefCntPtr
PolicyDecision::construct_policy_decision(const ResourceName &resName,
					  XMLElement &rr,
					  const KVMRefCntPtr env_param)
{
    XMLElement actnD;
    // Construct the policy decision object using the
    // resource name.
    PolicyDecision *retVal = new PolicyDecision(resName, env_param);

    for(XMLElement iter = rr.getFirstSubElement();
	iter.isValid(); iter.nextSibling()) {

	// Process Response Attributes
	if(iter.isNamed(RESPONSE_ATTRIBUTES)) {
	    XMLElement attrValuePair;
	    if(iter.getSubElement(ATTRIBUTE_VALUE_PAIR, attrValuePair)) {
		for(;attrValuePair.isValid();
		    attrValuePair.nextSibling(ATTRIBUTE_VALUE_PAIR)) {
		    XMLElement attr;
		    string attrName; 
		    if(attrValuePair.getSubElement(ATTRIBUTE, attr)) {
			if((attr.getAttributeValue(ATTRIBUTE_NAME,
						   attrName))) {
			    if(attrName.size() > 0) {
				KeyValueMap::mapped_type attrVals;

				XMLElement values;
				for(attrValuePair.getSubElement(VALUE, values);
				    values.isValid();
				    values.nextSibling(VALUE)) {
				    std::string value;
				    if(values.getValue(value)) {
					attrVals.push_back(value);
				    }
				}
				if(attrVals.size() > 0) {
				   retVal->responseAttributes[attrName] = 
							      attrVals;
				}
			    }
			}
		    }
		}
	    }
	}

	if(iter.isNamed(ACTION_DECISION)) {
	    std::string actName;

	    // Get the attribute value pairs
	    XMLElement avp, attrNode;
	    ActionDecision *ad = NULL;
	    if(iter.getSubElement(ATTRIBUTE_VALUE_PAIR, avp)) {
		if(avp.getSubElement(ATTRIBUTE, attrNode)) {
		    
		    if(attrNode.getAttributeValue(ATTRIBUTE_NAME, actName)) {
			// Create the ActionDecision with
			// the action name and TTL.
			ad = new ActionDecision(actName, 
                                       Utils::getTTL(iter,policy_clock_skew));
                                       
			// Add the action values.
			XMLElement values;
			if(avp.getSubElement(VALUE, values)) {
			    for(; values.isValid();
				values.nextSibling(VALUE)) {
				ad->addActionValues(values);
			    }
			}
			// No else for this if condition.
			// No value for actions is fine.

		    } else {
			// Log:ERROR
			std::string msg("No attribute name value node "
				   "in Attribute node for while "
				   "processing resource: ");
			msg.append(resName.getString());
			throw InternalException(
			    "PolicyDecision::construct_policy_decision",
			    msg, AM_POLICY_FAILURE);
		    }
		} else {
		    // Log:ERROR
		    std::string msg("No Attribute node found under"
			       "AttributeValuePair while resource: ");
		    msg.append(resName.getString());
		    throw InternalException(
			"PolicyDecision::construct_policy_decision",
			msg, AM_POLICY_FAILURE);
		}
	    } else {
		// Log:ERROR
		std::string msg("No AttributeValuePair node "
			   "found while resource: ");
		msg.append(resName.getString());
		throw InternalException(
		    "PolicyDecision::construct_policy_decision",
		    msg, AM_POLICY_FAILURE);
	    }

	    // Get the advices.
	    XMLElement advices_element;
	    if(iter.getSubElement(ADVICES, advices_element)) {
		XMLElement attrValPair;
		advices_element.getSubElement(ATTRIBUTE_VALUE_PAIR,
					      attrValPair);
		for(;attrValPair.isValid();
		    attrValPair.nextSibling(ATTRIBUTE_VALUE_PAIR)) {
		    ad->addAdvice(attrValPair);
		}
	    }

	    // Add it to the policy decision.
	    retVal->actionDecisions[actName]=ad;
	}

	// Process Response decisions.
	if(iter.isNamed(RESPONSE_DECISIONS)) {
	    XMLElement attrValuePair;
	    if(iter.getSubElement(ATTRIBUTE_VALUE_PAIR, attrValuePair)) {
		for(;attrValuePair.isValid();
		    attrValuePair.nextSibling(ATTRIBUTE_VALUE_PAIR)) {
		    XMLElement attr;
		    if(attrValuePair.getSubElement(ATTRIBUTE, attr)) {
			std::string ldapAttrName;
			if((attr.getAttributeValue(ATTRIBUTE_NAME,
						   ldapAttrName))) {
                            if (ldapAttrName.size() > 0) {
			        KeyValueMap::mapped_type attrVals;

			        XMLElement values;
			        for(attrValuePair.getSubElement(VALUE, values);
				    values.isValid();
				    values.nextSibling(VALUE)) {
				        std::string value;
				        if(values.getValue(value)) {
					    attrVals.push_back(value);
				        }
			         }
			         if(attrVals.size() > 0) {
				    retVal->responses[ldapAttrName] = attrVals;
			         }
                            }
			}
		    }
		}
	    }
	}
    }
    // Mark node as not stale.
    retVal->refetch = false;

    return PDRefCntPtr(retVal);
}

bool
PolicyDecision::isStale(const std::string &actionName) {
    bool retVal = isStale();
    ActionDecision *ad = actionDecisions[actionName];

    // If there is action decision, use it, but if there is not,
    // there is no point re-fetching the policy decision.
    if (retVal != true && ad != NULL) {
        time_t now = time(0);
        time_t expirationTime = ad->getTimeStamp();
        const char *resName = resourceName.getString().c_str();
        
        if (Log::isLevelEnabled(Log::ALL_MODULES, Log::LOG_DEBUG)) {
            char time_string[50];
            struct tm tm;
#ifdef _MSC_VER
            localtime_s(&tm, &expirationTime);
#else
            localtime_r(&expirationTime, &tm);
#endif
            strftime(time_string, sizeof (time_string), "%Y-%m-%d %H:%M:%S", &tm);
            
            Log::log(Log::ALL_MODULES, Log::LOG_DEBUG,
                    "Policy time stamp for resource %s is %s", resName,
                    time_string);
        }
        if (difftime(now, expirationTime) > 0) {
            Log::log(Log::ALL_MODULES, Log::LOG_INFO,
                    "Policy node %s marked stale due to time out.",
                    resName);

            markStale();
            retVal = true;
        }
    }
    return retVal;
}

bool
PolicyDecision::matchEnvParams(const KeyValueMap &ev) {
    return (*env)==ev;
}

