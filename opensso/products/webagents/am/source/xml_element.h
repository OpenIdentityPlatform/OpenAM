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
 * $Id: xml_element.h,v 1.3 2008/06/25 08:14:41 qcheng Exp $
 *
 * Abstract:
 *
 * Represents an element in an XML parse tree.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef XML_ELEMENT_H
#define XML_ELEMENT_H

#include "internal_macros.h"
#include "xml_utilities.h"
#include "xml_attribute.h"

BEGIN_PRIVATE_NAMESPACE

class XMLElement {
public:
    // The different types of XML nodes.

    enum Type {
        INVALID_NODE = 0,
        ELEMENT_NODE = 1,
        TEXT_NODE = 3,
        CDATA_NODE = 4,
        COMMENT_NODE = 8
    };

    //
    // Construct a singular, i.e. invalid, XMLElement
    //
    XMLElement();

    //
    // Copy one XMLElement into another.
    //
    XMLElement(const XMLElement& rhs);

    //
    // Destroy an XMLElement
    //
    ~XMLElement();

    //
    // Assign the contents of one XMLElement to another.
    //
    XMLElement& operator=(const XMLElement& rhs);

    //
    // Get the type of this element
    //
    // Returns:
    //   INVALID_NODE	if the element is not valid
    //
    //   type of node	otherwise
    //
    Type getType() const;

    //
    // Get the name of this element.
    //
    // Parameters:
    //   name	where to store the name of the element
    //
    // Returns:
    //   true	if this element is valid and has a name
    //
    //   false	otherwise
    //
    // Throws:
    //   std::bad_alloc
    //		if unable to allocate space for the name
    //
    bool getName(std::string& name) const;

    //
    // Determines if the element has the specified name.
    //
    // Parameters:
    //   name	the expected name of the element
    //
    // Returns:
    //   true	if the element is valid and has the specified name
    //
    //   false	otherwise
    //
    bool isNamed(const std::string& name) const;

    //
    // Asserts that the element has the specified name.  If the element
    // does not match the specified name, then an exception is thrown.
    //
    // Parameters:
    //   name	the expected name of the element
    //
    // Throws:
    //   std::invalid_argument
    //		if the element is not valid or does not have the specified
    //		name
    //
    void assertName(const std::string& name) const;

    //
    // Get the value of this element.
    //
    // Parameters:
    //   value	where to store the value (data) of the element
    //
    // Returns:
    //   true	if this element is valid
    //
    //   false	otherwise
    //
    // Throws:
    //   std::bad_alloc
    //		if unable to allocate space for the value
    //
    bool getValue(std::string& value) const;

    //
    // Get the value of the specified attribute.
    //
    // Parameters:
    //   attributeName
    //		name of the attribute to retrieve
    //
    //   attributeValue
    //		where to store the value of the specified attribute
    //
    // Returns:
    //   false	if attributeName is NULL or is not associated with this node
    //
    //   true	otherwise
    //
    bool getAttributeValue(const char *attributeName,
            std::string& attributeValue) const;

    //
    // Determines if this node has a value for the specified attribute.
    //
    // Parameters:
    //   attributeName
    //		name of the attribute to check
    //
    // Returns:
    //   false	if attributeName is NULL or is not associated with this node
    //
    //   true	otherwise
    //
    bool hasAttribute(const char *attributeName) const;

    //
    // Returns true if and only if this node has any attributes
    //
    bool hasAttributes() const;

    //
    // Get the XMLAttribute for the first attribute of this element.
    //
    // Returns:
    //   a valid XMLAttribute
    //		if the element is valid and has an attribute
    //
    //   an invalid XMLAttribute
    //		otherwise
    //
    XMLAttribute getFirstAttribute() const;

    //
    // Returns true if and only if this element is valid and has any
    // non-blank sub-elements.
    //
    bool hasSubElements() const;

    //
    // Get the XMLElement for the first non-blank sub-element of this element.
    //
    // Returns:
    //   a valid XMLElement
    //		if the element is valid and has a non-blank sub-element
    //
    //   an invalid XMLElement
    //		otherwise
    //
    XMLElement getFirstSubElement() const;

    //
    // Modifies the XMLElement to refer to its next sibling in the
    // parse tree.
    //
    // Returns:
    //   true	if there is a sibling node
    //
    //   false	otherwise
    //
    bool nextSibling();

    //
    // Modifies the XMLElement to refer to its next sibling in the
    // parse tree that has the specified name.
    //
    // Parameters:
    //   name	name of the element to locate
    //
    // Returns:
    //   true	if there is a sibling node with the specified name
    //
    //   false	otherwise
    //
    bool nextSibling(const std::string& name);

    //
    // Finds an element under this node with the given name.
    //
    // Parameters:
    //   name	name of the element to locate
    //
    //   subElement
    //		the XMLElement to hold the desired XML node pointer
    //
    // Returns:
    //   true	if the node is found.  subElement is set to the appropriate
    //          xmlNodePtr.
    //
    //   false	otherwise and subElement is set to an invalid value
    //
    bool getSubElement(const std::string& name, XMLElement& subElement) const;

    //
    // Determines whether the XMLElement is valid, i.e. references
    // some node in an XML parse tree.
    //
    // Returns:
    //   true	if the object is valid
    //
    //   false	otherwise
    //
    bool isValid() const;

    //
    // Recursively logs the name and content of the element to the
    // specified logging module at the specified level.
    //
    // Parameters:
    //   logModule
    //		module id of the logging module to use
    //
    //   level
    //		logging level to associate with the generated messages
    //
    void log(Log::ModuleId logModule, Log::Level level) const;

private:
    friend class XMLTree;

#ifdef _MSC_VER
    explicit XMLElement(MSXML2::IXMLDOMNodePtr pointer);

    bool nameMatches(const std::string &targetNodeName) const {
        bool found = false;
        BSTR bstr;
        if (nodePtr) {
            if ((nodePtr->get_nodeName(&bstr)) == S_OK) {
                found = matchesXMLString(targetNodeName, bstr);
            }
        }
        return found;
    }

#else
    explicit XMLElement(struct _xmlNode *pointer);
#endif

    static void walkTree(Log::ModuleId logModule, Log::Level level,
            unsigned int depth, XMLElement node);

    bool findSubElement(const std::string& name, XMLElement& subElement) const;

#ifdef _MSC_VER
    MSXML2::IXMLDOMNodePtr nodePtr;
#else
    struct _xmlNode *nodePtr;
#endif
};

inline XMLElement::XMLElement()
: nodePtr(NULL) {
}

#ifdef _MSC_VER

inline XMLElement::XMLElement(MSXML2::IXMLDOMNodePtr nodePtrArg)
: nodePtr(nodePtrArg) {
}
#else

inline XMLElement::XMLElement(struct _xmlNode *nodePtrArg)
: nodePtr(nodePtrArg) {
}
#endif

inline XMLElement::XMLElement(const XMLElement& rhs)
: nodePtr(rhs.nodePtr) {
}

inline XMLElement::~XMLElement() {
    nodePtr = NULL;
}

inline XMLElement& XMLElement::operator=(const XMLElement& rhs) {
    // Not worth checking for self-assignment, since this is safe to do anyway.
    nodePtr = rhs.nodePtr;

    return *this;
}

inline bool XMLElement::isValid() const {
    return nodePtr != NULL;
}

END_PRIVATE_NAMESPACE

#endif	// not defined XML_ELEMENT_H
