/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

#include "xml_element.h"

USING_PRIVATE_NAMESPACE

XMLElement::Type XMLElement::getType() const {
    Type value = INVALID_NODE;
    MSXML2::DOMNodeType type;
    if (nodePtr) {
        if ((nodePtr->get_nodeType(&type)) == S_OK) {
            switch (type) {
                case NODE_ELEMENT:
                    value = ELEMENT_NODE;
                    break;
                case NODE_TEXT:
                    value = TEXT_NODE;
                    break;
                case NODE_CDATA_SECTION:
                    value = CDATA_NODE;
                    break;
                case NODE_COMMENT:
                    value = COMMENT_NODE;
                    break;
            }
        }
    }
    return value;
}

bool XMLElement::getName(std::string& name) const {
    bool valid = false;
    BSTR bstr;
    if (nodePtr) {
        if ((nodePtr->get_nodeName(&bstr)) == S_OK) {
            name = bstrToString(bstr).c_str();
            valid = true;
        }
    }
    return valid;
}

bool XMLElement::isNamed(const std::string& name) const {
    bool valid = false;
    BSTR bstr;
    if (nodePtr) {
        if ((nodePtr->get_nodeName(&bstr)) == S_OK) {
            valid = matchesXMLString(name, bstr);
        }
    }
    return valid;
}

void XMLElement::assertName(const std::string& name) const {
    if (!isNamed(name)) {
        std::string tmp;
        getName(tmp);
        throw std::invalid_argument("XMLElement::assertName() '" + name
                + "' != '"
                + tmp
                + "'");
    }
}

bool XMLElement::getValue(std::string& value) const {
    bool found = false;
    BSTR bstr;
    if (nodePtr) {
        if (nodePtr->GetnodeType() == NODE_CDATA_SECTION) {
            IXMLDOMCDATASectionPtr pCData = nodePtr;
            if ((pCData->get_xml(&bstr)) == S_OK) {
                value = bstrToString(bstr).c_str();
                found = true;
            }
        } else {
            if ((nodePtr->get_text(&bstr)) == S_OK) {
                value = bstrToString(bstr).c_str();
                found = true;
            }
        }
    }
    return found;
}

bool XMLElement::getAttributeValue(const char *attributeName,
        std::string& attributeValue) const {
    bool found = false;
    MSXML2::IXMLDOMNamedNodeMapPtr attrs;
    MSXML2::IXMLDOMNodePtr attr;
    BSTR bstr;
    if (nodePtr) {
        if ((nodePtr->get_attributes(&attrs)) == S_OK) {
            if ((attrs->raw_getNamedItem(_bstr_t(attributeName), &attr)) == S_OK) {
                if ((attr->get_text(&bstr)) == S_OK) {
                    attributeValue = bstrToString(bstr).c_str();
                    found = true;
                }
            }
        }
    }
    return found;
}

bool XMLElement::hasAttribute(const char *attributeName) const {
    std::string tmp;
    return getAttributeValue(attributeName, tmp);
}

bool XMLElement::hasAttributes() const {
    bool found = false;
    MSXML2::IXMLDOMNamedNodeMapPtr attrs;
    if (nodePtr) {
        if ((nodePtr->get_attributes(&attrs)) == S_OK) {
            if (attrs->Getlength() > 0) found = true;
        }
    }
    return found;
}

XMLAttribute XMLElement::getFirstAttribute() const {
    XMLAttribute attr;
    MSXML2::IXMLDOMNamedNodeMapPtr attrs;
    if (nodePtr) {
        if ((nodePtr->get_attributes(&attrs)) == S_OK) {
            if (attrs->Getlength() > 0) {
                attr.attrPtr = attrs->Getitem(0);
            }
        }
    }
    return attr;
}

bool XMLElement::findSubElement(const std::string& name,
        XMLElement& subElement) const {
    bool found = false;
    BSTR bstr;
    if (nodePtr) {
        if ((nodePtr->get_nodeName(&bstr)) == S_OK) {
            if (matchesXMLString(name, bstr)) {
                subElement = *this;
                found = true;
            }
        }
        if (found == false && nodePtr->hasChildNodes() == VARIANT_TRUE) {
            for (XMLElement subNode = getFirstSubElement();
                    subNode.isValid(); subNode.nextSibling()) {
                found = subNode.findSubElement(name, subElement);
                if (found) break;
            }
        }
    }
    return found;
}

bool XMLElement::getSubElement(const std::string& name,
        XMLElement& subElement) const {
    bool found = false;
    if (nodePtr && nodePtr->hasChildNodes() == VARIANT_TRUE) {
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
    return found;
}

XMLElement XMLElement::getFirstSubElement() const {
    XMLElement subElement;
    if (nodePtr) {
        subElement.nodePtr = nodePtr->GetfirstChild();
    }
    return subElement;
}

bool XMLElement::hasSubElements() const {
    bool found = false;
    if (nodePtr && nodePtr->hasChildNodes() == VARIANT_TRUE) {
        found = getFirstSubElement().isValid();
    }
    return found;
}

bool XMLElement::nextSibling() {
    if (nodePtr) {
        nodePtr = nodePtr->GetnextSibling();
    }
    return isValid();
}

bool XMLElement::nextSibling(const std::string &targetNodeName) {
    while (nextSibling() && !nameMatches(targetNodeName));
    return isValid();
}

void XMLElement::log(Log::ModuleId logModule, Log::Level level) const {
    if (Log::isLevelEnabled(logModule, level)) {
        if (isValid()) {
            Log::log(logModule, Log::LOG_MAX_DEBUG, "XMLElement::log() Calling walkTree().");
            walkTree(logModule, level, 0, *this);
        } else {
            Log::log(logModule, level, "XMLElement::log() invalid element");
        }
    }
}

void XMLElement::walkTree(Log::ModuleId logModule, Log::Level level,
        unsigned int depth, XMLElement node) {
    while (node.isValid()) {
        XMLElement::Type nodeType = node.getType();
        XMLAttribute attr;
        if (XMLElement::ELEMENT_NODE == nodeType) {
            std::string name;
            if (node.getName(name)) {
                Log::log(logModule, level, "%*sElement = %s", depth * 2, "",
                        name.c_str());
            } else {
                Log::log(logModule, level, "%*sElement, unamed",
                        depth * 2, "");
            }

            for (attr = node.getFirstAttribute(); attr.isValid(); attr.next()) {
                attr.log(logModule, level, depth);
            }

            walkTree(logModule, level, depth + 1, node.getFirstSubElement());

        } else if (XMLElement::CDATA_NODE == nodeType) {
            std::string value;
            if (node.getValue(value)) {
                Log::log(logModule, level, "%*sCDATA = '%s'", depth * 2, "",
                        value.c_str());
            } else {
                Log::log(logModule, level, "%*sCDATA unable to retrieve value",
                        depth * 2, "");
            }
        } else if (XMLElement::TEXT_NODE == nodeType) {
            std::string value;
            if (node.getValue(value)) {
                Log::log(logModule, level, "%*sTEXT = '%s'", depth * 2, "",
                        value.c_str());
            } else {
                Log::log(logModule, level, "%*sTEXT unable to retrieve value",
                        depth * 2, "");
            }
        } else {
            Log::log(logModule, level, "%*sType = %d, unknown node type",
                    depth * 2, "", nodeType);
        }
        node.nextSibling();
    }
}
