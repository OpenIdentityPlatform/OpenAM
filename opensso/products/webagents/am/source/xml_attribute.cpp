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
 * $Id: xml_attribute.cpp,v 1.3 2008/06/25 08:14:41 qcheng Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include "xml_attribute.h"
#include "xml_utilities.h"

USING_PRIVATE_NAMESPACE

bool XMLAttribute::getName(std::string& name) const
{
    bool valid;

    if (attrPtr) {
	name = reinterpret_cast<const char *>(attrPtr->name);
	valid = true;
    } else {
	valid = false;
    }

    return valid;
}

bool XMLAttribute::isNamed(const std::string& name) const
{
    bool valid = false;

    if (attrPtr) {
	valid = matchesXMLString(name, attrPtr->name);
    }

    return valid;
}

bool XMLAttribute::getValue(std::string& value) const
{
    bool found;

    if (attrPtr) {
	xmlNodePtr nodePtr = reinterpret_cast<xmlNodePtr>(attrPtr);
	xmlChar *attrValue = xmlNodeGetContent(nodePtr);

	if (attrValue) {
	    // This is in a try-catch block in case the allocation by
	    // the string object (value) throws an exception.
	    try {
		value = reinterpret_cast<const char *>(attrValue);
	    } catch (...) {
		xmlFree(attrValue);
		throw;
	    }

	    xmlFree(attrValue);
	    found = true;
	} else {
	    found = false;
	}
    } else {
	found = false;
    }

    return found;
}

bool XMLAttribute::next()
{
    if (attrPtr) {
	attrPtr = attrPtr->next;
    }

    return isValid();
}

void XMLAttribute::log(Log::ModuleId logModule, Log::Level level,
		       unsigned int depth) const
{
    if (Log::isLevelEnabled(logModule, level)) {
	if (isValid()) {
	    xmlNodePtr nodePtr = reinterpret_cast<xmlNodePtr>(attrPtr);
	    xmlChar *value = xmlNodeGetContent(nodePtr);

	    if (value) {
		Log::log(logModule, level,
			 "%*sAttribute = %s, value = %s",
			 depth * 2 + 1, "", attrPtr->name, value);
		xmlFree(value);
	    } else {
		Log::log(logModule, level,
			 "%*sAttribute = %s, unable to get value",
			 depth * 2 + 1, "", attrPtr->name);
	    }
	} else {
	    Log::log(logModule, level,"XMLAttribute::log() invalid attribute");
	}
    }
}
