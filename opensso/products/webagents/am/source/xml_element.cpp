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
 * $Id: xml_element.cpp,v 1.3 2008/06/25 08:14:41 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <stdexcept>
#include "xml_element.h"
#include "xml_utilities.h"

namespace {
    inline xmlNodePtr skipIgnoredNodes(xmlNodePtr nodePtr)
    {
	while (nodePtr && xmlIsBlankNode(nodePtr)) {
	    nodePtr = nodePtr->next;
	}

	return nodePtr;
    }
}

USING_PRIVATE_NAMESPACE

XMLElement::Type XMLElement::getType() const
{
    Type value;

    if (nodePtr) {
	value = static_cast<Type>(static_cast<int>(nodePtr->type));
    } else {
	value = INVALID_NODE;
    }

    return value;
}

bool XMLElement::getName(std::string& name) const
{
    bool valid;

    if (nodePtr && nodePtr->name) {
	name = reinterpret_cast<const char *>(nodePtr->name);
	valid = true;
    } else {
	valid = false;
    }

    return valid;
}

bool XMLElement::isNamed(const std::string& name) const
{
    bool valid = false;

    if (nodePtr) {
	valid = matchesXMLString(name, nodePtr->name);
    }

    return valid;
}

/* Throws std::invalid_argument if argument is invalid */
void XMLElement::assertName(const std::string& name) const
{
    if (! isNamed(name)) {
	throw std::invalid_argument("XMLElement::assertName() '" + name
				    + "' != '"
				    + reinterpret_cast<const char *>(nodePtr->name)
				    + "'");
    }
}

bool XMLElement::getValue(std::string& value) const
{
    bool found;

    if (nodePtr) {
	xmlChar *xmlValue = xmlNodeGetContent(nodePtr);

	if (xmlValue) {
	    // This is in a try-catch block in case the allocation by
	    // the string object (value) throws an exception.
	    try {
		value = reinterpret_cast<const char *>(xmlValue);
	    } catch (...) {
		xmlFree(xmlValue);
		throw;
	    }

	    xmlFree(xmlValue);
	    found = true;
	} else {
	    found = false;
	}
    } else {
	found = false;
    }

    return found;
}

bool XMLElement::getAttributeValue(const char *attributeName,
				   std::string& attributeValue) const
{
    bool found;
    const xmlChar *attrName = reinterpret_cast<const xmlChar *>(attributeName);
    xmlChar *attrValue = xmlGetProp(nodePtr, attrName);

    if (attrValue) {
	// This is in a try-catch block in case the allocation by
	// the string object (value) throws an exception.
	try {
	    attributeValue = reinterpret_cast<const char *>(attrValue);
	} catch (...) {
	    xmlFree(attrValue);
	    throw;
	}

	xmlFree(attrValue);
	found = true;
    } else {
	found = false;
    }

    return found;
}

bool XMLElement::hasAttribute(const char *attributeName) const
{
    // XXX
    return false;
}

bool XMLElement::hasAttributes() const
{
    return (nodePtr && nodePtr->properties);
}

XMLAttribute XMLElement::getFirstAttribute() const
{
    XMLAttribute attr;

    if (nodePtr) {
	attr.attrPtr = nodePtr->properties;
    }

    return attr;
}

bool XMLElement::findSubElement(const std::string& name,
				XMLElement& subElement) const
{
    bool found = false;

    if (matchesXMLString(name, nodePtr->name)) {
	subElement = *this;
	found = true;
    } else if (nodePtr->children) {
	// This is not the correct node so do a depth-first search
	// of all of the descendant nodes.
	for (XMLElement subNode = getFirstSubElement();
	     subNode.isValid();
	     subNode.nextSibling()) {
	    found = subNode.findSubElement(name, subElement);
	    if (found) {
		break;
	    }
	}
    }

    return found;
}

bool XMLElement::getSubElement(const std::string& name,
			       XMLElement& subElement) const
{
    bool found = false;

    if (nodePtr && nodePtr->children) {
	// Do a depth-first search of all of the descendant nodes.
	for (XMLElement subNode = getFirstSubElement();
	     subNode.isValid();
	     subNode.nextSibling()) {
	    found = subNode.findSubElement(name, subElement);
	    if (found) {
		break;
	    }
	}
    }

    if(!found)
	subElement.nodePtr = NULL;

    return found;
}

XMLElement XMLElement::getFirstSubElement() const
{
    XMLElement subElement;

    if (nodePtr) {
	subElement.nodePtr = skipIgnoredNodes(nodePtr->children);
    }

    return subElement;
}

bool XMLElement::hasSubElements() const
{
    return getFirstSubElement().isValid();
}

bool XMLElement::nextSibling()
{
    if (nodePtr) {
	nodePtr = skipIgnoredNodes(nodePtr->next);
    }

    return isValid();
}

bool XMLElement::nextSibling(const std::string &targetNodeName)
{
    while (nextSibling() &&
	   ! matchesXMLString(targetNodeName, nodePtr->name)) {
	// Empty loop, nextSibling() does the iteration.
    }

    return isValid();
}

void XMLElement::log(Log::ModuleId logModule, Log::Level level) const
{
    if (Log::isLevelEnabled(logModule, level)) {
	if (isValid()) {
	    Log::log(logModule, Log::LOG_MAX_DEBUG,
		     "XMLElement::log() Calling walkTree().");
	    walkTree(logModule, level, 0, *this);
	} else {
	    Log::log(logModule, level, "XMLElement::log() invalid element");
	}
    }
}

void XMLElement::walkTree(Log::ModuleId logModule, Log::Level level,
			  unsigned int depth, XMLElement node)
{
    while (node.isValid()) {
	XMLElement::Type nodeType = node.getType();
	XMLAttribute attr;
	xmlChar *value;

	if (XMLElement::ELEMENT_NODE == nodeType) {
	    if (node.nodePtr->name) {
		Log::log(logModule, level, "%*sElement = %s", depth * 2, "",
			 node.nodePtr->name);
	    } else {
		Log::log(logModule, level, "%*sElement, unamed",
			 depth * 2, "");
	    }

	    for (attr = node.getFirstAttribute(); attr.isValid();attr.next()) {
		attr.log(logModule, level, depth);
	    }

	    walkTree(logModule, level, depth + 1, node.getFirstSubElement());
	} else if (XMLElement::CDATA_NODE == nodeType) {
	    value = xmlNodeGetContent(node.nodePtr);
	    if (value) {
		Log::log(logModule, level, "%*sCDATA = '%s'", depth * 2, "",
			 value);
		xmlFree(value);
	    } else {
		Log::log(logModule, level, "%*sCDATA unable to retrieve value",
			 depth * 2, "");
	    }
	} else if (XMLElement::TEXT_NODE == nodeType) {
	    value = xmlNodeGetContent(node.nodePtr);
	    if (value) {
		Log::log(logModule, level, "%*sTEXT = '%s'", depth * 2, "",
			 value);
		xmlFree(value);
	    } else {
		Log::log(logModule, level, "%*sTEXT unable to retrieve value",
			 depth * 2, "");
	    }
	} else {
	    value = xmlNodeGetContent(node.nodePtr);
	    if (value) {
		Log::log(logModule, level, "%*sType = %d, value = %s",
			 depth * 2, "", nodeType, value);
		xmlFree(value);
	    } else {
		Log::log(logModule, level,
			 "%*sType = %d, unable to retrieve value",
			 depth * 2, "", nodeType);
	    }
	}

	node.nextSibling();
    }
}
