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
 * $Id: xml_tree.cpp,v 1.4 2008/09/13 01:11:53 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <stdio.h>
#include <climits>
#include <cstdarg>
#include <stdexcept>
#include <cstring>
#include "xml_tree.h"

USING_PRIVATE_NAMESPACE

namespace {

    struct XMLErrorData {
	XMLErrorData(const char *prefix)
	    : errorDetected(false), message(prefix)
	{
	}

	bool errorDetected;
	std::string message;
    };

    void XMLErrorHandler(XMLErrorData *data, const char *format, ...)
    {
	if (data) {
	    const char prefix[] = "XMLTree() ";
	    char buf[1024 + sizeof(prefix)];
	    va_list args;

	    std::memcpy(buf, prefix, sizeof(prefix) - 1);

	    va_start(args, format);
	    vsnprintf(buf, sizeof(buf) - sizeof(prefix) - 1, format, args);
	    va_end(args);

	    data->errorDetected = true;
	    try {
		data->message.append(buf);
	    } catch (const std::exception&) {
		// supress any exceptions that occur copying the message
	    }
	}
    }
}

bool XMLTree::initialized = false;

/* 
 * Throws:
 *	std::invalid_argument if any argument is invalid 
 *	XMLTree::ParseException upon XML parsing error
 */
XMLTree::XMLTree(bool validate, const char *docBuffer, std::size_t bufferLen)
    : docPtr(NULL)
{
    if (static_cast<const char *>(NULL) == docBuffer) {
	throw std::invalid_argument("XMLTree(docBuffer) is NULL");
    }

    if (0 == bufferLen) {
	bufferLen = std::strlen(docBuffer);
	if (0 == bufferLen) {
	    throw std::invalid_argument("XMLTree(docBuffer) is empty");
	}
    }

    if (INT_MAX < bufferLen) {
	throw std::invalid_argument("XMLTree(docBuffer) too long");
    }

    docPtr = xmlParseMemory(docBuffer, static_cast<int>(bufferLen));
    if (static_cast<xmlDocPtr>(NULL) == docPtr) {
	throw ParseException("XMLTree() unable to parse docBuffer");
    }

    if (validate) {
	//
	// Now, try to validate the document.
	//
        XMLErrorData errorData("XMLTree() ");
	xmlValidCtxt cvp;

	cvp.userData = &errorData;
	cvp.error = reinterpret_cast<xmlValidityErrorFunc>(XMLErrorHandler);
	cvp.warning = reinterpret_cast<xmlValidityErrorFunc>(XMLErrorHandler);

	if (!xmlValidateDocument(&cvp, docPtr)) {
	    xmlFreeDoc(docPtr);
	    if (errorData.errorDetected) {
		throw ParseException(errorData.message);
	    } else {
		throw ParseException("XMLTree() docBuffer does not validate");
	    }
	}
    }

    if (static_cast<xmlNodePtr>(NULL) == xmlDocGetRootElement(docPtr)) {
	xmlFreeDoc(docPtr);
	throw ParseException("XMLTree() empty document");
    }
}

XMLTree::~XMLTree()
{
    xmlFreeDoc(docPtr);
    docPtr = NULL;
}

void XMLTree::initialize()
{
    if (! initialized) {
	xmlInitParser();
	xmlKeepBlanksDefault(false);
	initialized = true;
    }
}

void XMLTree::shutdown()
{
    xmlCleanupParser();
}

XMLElement XMLTree::getRootElement() const
{
    return XMLElement(xmlDocGetRootElement(docPtr));
}

void XMLTree::log(Log::ModuleId logModule, Log::Level level) const
{
    if (Log::isLevelEnabled(logModule, level)) {
	getRootElement().log(logModule, level);
    }
}
