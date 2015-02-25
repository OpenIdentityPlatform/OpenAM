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
 * Portions Copyrighted 2013-2014 ForgeRock AS
 */

#include <stdexcept>
#include "xml_element.h"
#include "xml_utilities.h"

#ifndef PUGIXML
namespace {

    inline xmlNodePtr skipIgnoredNodes(xmlNodePtr nodePtr) {
        while (nodePtr && xmlIsBlankNode(nodePtr)) {
            nodePtr = nodePtr->next;
        }
        return nodePtr;
    }
}
#endif

USING_PRIVATE_NAMESPACE

XMLElement::Type XMLElement::getType() const {
    Type value;
#ifdef PUGIXML
    if (nodePtr) {
        if (nodePtr.type() == pugi::node_element) {
            value = ELEMENT_NODE;
        } else if (nodePtr.type() == pugi::node_pcdata) {
            value = TEXT_NODE;
        } else if (nodePtr.type() == pugi::node_cdata) {
            value = CDATA_NODE;
        } else if (nodePtr.type() == pugi::node_comment) {
            value = COMMENT_NODE;
        } else {
            value = INVALID_NODE;
        }
    } else {
        value = INVALID_NODE;
    }
#else
    if (nodePtr) {
        value = static_cast<Type> (static_cast<int> (nodePtr->type));
    } else {
        value = INVALID_NODE;
    }
#endif
    return value;
}

bool XMLElement::getName(std::string& name) const {
    bool valid = false;
#ifdef PUGIXML
    if (nodePtr) {
        name = nodePtr.name();
        valid = true;
    } else {
        valid = false;
    }
#else
    if (nodePtr && nodePtr->name) {
        name = reinterpret_cast<const char *> (nodePtr->name);
        valid = true;
    } else {
        valid = false;
    }
#endif
    return valid;
}

bool XMLElement::isNamed(const std::string& name) const {
    bool valid = false;
#ifdef PUGIXML
    if (nodePtr) {
        valid = matchesXMLString(name, nodePtr.name());
    }
#else
    if (nodePtr) {
        valid = matchesXMLString(name, nodePtr->name);
    }
#endif
    return valid;
}

/* Throws std::invalid_argument if argument is invalid */
void XMLElement::assertName(const std::string& name) const {
    if (!isNamed(name)) {
        throw std::invalid_argument("XMLElement::assertName() '" + name
                + "' != '"
#ifdef PUGIXML
                + nodePtr.name()
#else
                + reinterpret_cast<const char *> (nodePtr->name)
#endif
                + "'");
    }
}

bool XMLElement::getValue(std::string& value) const {
    bool found = false;
#ifdef PUGIXML
    if (nodePtr) {
        pugi::xml_node cn = nodePtr.first_child();
        if (cn && (cn.type() == pugi::node_cdata || cn.type() == pugi::node_pcdata)) {
            value = cn.text().get();
        } else {
            value = nodePtr.value();
        }
        found = true;
    }
#else
    if (nodePtr) {
        xmlChar *xmlValue = xmlNodeGetContent(nodePtr);
        if (xmlValue) {
            // This is in a try-catch block in case the allocation by
            // the string object (value) throws an exception.
            try {
                value = reinterpret_cast<const char *> (xmlValue);
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
#endif
    return found;
}

bool XMLElement::getAttributeValue(const char *attributeName,
        std::string& attributeValue) const {
    bool found = false;
#ifdef PUGIXML
    pugi::xml_attribute attr;
    attr = nodePtr.attribute(attributeName);
    if (attr) {
        attributeValue = attr.value();
        found = true;
    }
#else
    const xmlChar *attrName = reinterpret_cast<const xmlChar *> (attributeName);
    xmlChar *attrValue = xmlGetProp(nodePtr, attrName);
    if (attrValue) {
        // This is in a try-catch block in case the allocation by
        // the string object (value) throws an exception.
        try {
            attributeValue = reinterpret_cast<const char *> (attrValue);
        } catch (...) {
            xmlFree(attrValue);
            throw;
        }

        xmlFree(attrValue);
        found = true;
    } else {
        found = false;
    }
#endif
    return found;
}

bool XMLElement::hasAttribute(const char *attributeName) const {
#ifdef PUGIXML
    return nodePtr.attribute(attributeName) ? true : false;
#else
    return false;
#endif
}

bool XMLElement::hasAttributes() const {
#ifdef PUGIXML
    return (!nodePtr.empty() && nodePtr.first_attribute());
#else
    return (nodePtr && nodePtr->properties);
#endif
}

XMLAttribute XMLElement::getFirstAttribute() const {
    XMLAttribute attr;
    if (nodePtr) {
        attr.attrPtr =
#ifdef PUGIXML
                nodePtr.first_attribute()
#else
                nodePtr->properties
#endif
                ;
    }

    return attr;
}

bool XMLElement::findSubElement(const std::string& name,
        XMLElement& subElement) const {
    bool found = false;
#ifdef PUGIXML
    if (matchesXMLString(name, nodePtr.name())) {
        subElement = *this;
        found = true;
    } else {
        /*for (pugi::xml_node_iterator it = nodePtr.begin(); it != nodePtr.end(); ++it) {
            if (matchesXMLString(name, it->name())) {
                subElement = XMLElement(*it);
                found = true;
                break;
            }
        }*/
        for (XMLElement subNode = getFirstSubElement();
                subNode.isValid();
                subNode.nextSibling()) {
            found = subNode.findSubElement(name, subElement);
            if (found) {
                break;
            }
        }
    }
#else
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
#endif
    return found;
}

bool XMLElement::getSubElement(const std::string& name,
        XMLElement& subElement) const {
    bool found = false;
#ifdef PUGIXML
    for (XMLElement subNode = getFirstSubElement();
            subNode.isValid();
            subNode.nextSibling()) {
        found = subNode.findSubElement(name, subElement);
        if (found) {
            break;
        }
    }
#else
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
    if (!found)
        subElement.nodePtr = NULL;
#endif
    return found;
}

XMLElement XMLElement::getFirstSubElement() const {
    XMLElement subElement;
#ifdef PUGIXML
    if (nodePtr) {
        subElement.nodePtr = nodePtr.first_child();
    }
#else
    if (nodePtr) {
        subElement.nodePtr = skipIgnoredNodes(nodePtr->children);
    }
#endif
    return subElement;
}

bool XMLElement::hasSubElements() const {
    return getFirstSubElement().isValid();
}

bool XMLElement::nextSibling() {
    if (nodePtr) {
#ifdef PUGIXML
        nodePtr = nodePtr.next_sibling();
#else
        nodePtr = skipIgnoredNodes(nodePtr->next);
#endif
    }
    return isValid();
}

bool XMLElement::nextSibling(const std::string &targetNodeName) {
    while (nextSibling() &&
            !matchesXMLString(targetNodeName,
#ifdef PUGIXML
            nodePtr.name()
#else
            nodePtr->name
#endif
            )) {
        // Empty loop, nextSibling() does the iteration.
    }
    return isValid();
}

void XMLElement::log(Log::ModuleId logModule, Log::Level level) const {
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

#ifdef PUGIXML

const char* node_types[] = {
    "null", "document", "element", "pcdata", "cdata", "comment", "pi", "declaration"
};

struct xml_tree_walker : pugi::xml_tree_walker {
    Log::ModuleId logModule;
    Log::Level level;

    xml_tree_walker(Log::ModuleId logModulep, Log::Level levelp) : logModule(logModulep), level(levelp) {
    }

    virtual bool for_each(pugi::xml_node& node) {

        Log::log(logModule, level, "XML node type = [%s], name = [%s], value = [%s]",
                node_types[node.type()], node.name(), 
                node.type() == pugi::node_cdata || node.type() == pugi::node_pcdata ? node.text().get() : node.value());
        for (pugi::xml_attribute_iterator ait = node.attributes_begin(); ait != node.attributes_end(); ++ait) {
            Log::log(logModule, level, "  attribute name = [%s], value = [%s]",
                    ait->name(), ait->value());
        }
        return true;
    }
};
#endif

void XMLElement::walkTree(Log::ModuleId logModule, Log::Level level,
        unsigned int depth, XMLElement node) {
#ifdef PUGIXML
    xml_tree_walker walker(logModule, level);
    node.nodePtr.traverse(walker);
#else
    while (node.isValid()) {
        XMLElement::Type nodeType = node.getType();
        XMLAttribute attr;
        xmlChar *value;

        if (XMLElement::ELEMENT_NODE == nodeType) {
            if (node.nodePtr->name) {
                Log::log(logModule, level, "Element = %s",
                        node.nodePtr->name);
            } else {
                Log::log(logModule, level, "Element, unnamed");
            }

            for (attr = node.getFirstAttribute(); attr.isValid(); attr.next()) {
                attr.log(logModule, level, depth);
            }

            walkTree(logModule, level, depth + 1, node.getFirstSubElement());
        } else if (XMLElement::CDATA_NODE == nodeType) {
            value = xmlNodeGetContent(node.nodePtr);
            if (value) {
                Log::log(logModule, level, "CDATA = '%s'",
                        value);
                xmlFree(value);
            } else {
                Log::log(logModule, level, "CDATA unable to retrieve value");
            }
        } else if (XMLElement::TEXT_NODE == nodeType) {
            value = xmlNodeGetContent(node.nodePtr);
            if (value) {
                Log::log(logModule, level, "TEXT = '%s'", value);
                xmlFree(value);
            } else {
                Log::log(logModule, level, "TEXT unable to retrieve value");
            }
        } else {
            value = xmlNodeGetContent(node.nodePtr);
            if (value) {
                Log::log(logModule, level, "Type = %d, value = %s",
                        nodeType, value);
                xmlFree(value);
            } else {
                Log::log(logModule, level,
                        "Type = %d, unable to retrieve value",
                        nodeType);
            }
        }

        node.nextSibling();
    }
#endif
}
