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
 * Abstract:
 *
 * Represents an attribute of an element in an XML parse tree.
 *
 */

#ifndef XML_ATTRIBUTE_H
#define XML_ATTRIBUTE_H

#include <string>

#include "internal_macros.h"
#include "log.h"

BEGIN_PRIVATE_NAMESPACE

class XMLAttribute {
public:
    //
    // Construct a singular, i.e. in valid, XMLAttribute
    //
    XMLAttribute();

    //
    // Copy one XMLAttribute into another.
    //
    XMLAttribute(const XMLAttribute& rhs);

    //
    // Destroy an XMLAttribute
    //
    ~XMLAttribute();

    //
    // Assign the contents of one XMLAttribute to another.
    //
    XMLAttribute& operator=(const XMLAttribute& rhs);

    //
    // Get the name of this attribute.
    //
    // Parameters:
    //   name	where to store the name of the attribute
    //
    // Returns:
    //   true	if this attribute is valid
    //
    //   false	otherwise
    //
    // Throws:
    //   std::bad_alloc
    //		if unable to allocate space for the name
    //
    bool getName(std::string& name) const;


    //
    // Determines if the attribute has the specified name.
    //
    // Parameters:
    //   name	the expected name of the attribute
    //
    // Returns:
    //   true	if the attribute is valid and has the specified name
    //
    //   false	otherwise
    //
    bool isNamed(const std::string& name) const;

    //
    // Get the value of this attribute.
    //
    // Parameters:
    //   value	where to store the value of the attribute
    //
    // Returns:
    //   true	if this attribute is valid
    //
    //   false	otherwise
    //
    // Throws:
    //   std::bad_alloc
    //		if unable to allocate space for the value
    //
    bool getValue(std::string& value) const;

    //
    // Modifies the object to refer to the next attribute of
    // the XMLElement with which this XMLAttribute is associated.
    //
    // Returns:
    //   true	if the object is valid and there is such an attribute
    //
    //   false	otherwise
    //
    bool next();

    //
    // Determines whether the XMLAttribute is valid, i.e. references
    // some attribute in an XML parse tree.
    //
    // Returns:
    //   true	if the object is valid
    //
    //   false	otherwise
    //
    bool isValid() const;

    //
    // Logs the name and value of the attribute to the specified
    // logging module at the specified level.
    //
    // Parameters:
    //   logModule
    //		module id of the logging module to use
    //
    //   level
    //		logging level to associate with the generated messages
    //
    //   depth
    //		indentation level for printing the attribute information.
    //		This parameter is used by XMLElement::log() to indent
    //		attribute information appropriately.  In most other cases,
    //		the default value, zero, can is appropriate.
    //
    void log(Log::ModuleId logModule, Log::Level level,
	     unsigned int depth = 0) const;

private:
    friend class XMLElement;

    explicit XMLAttribute(struct _xmlAttr *pointer);

    struct _xmlAttr *attrPtr;
};

inline XMLAttribute::XMLAttribute()
    : attrPtr(NULL)
{
}

inline XMLAttribute::XMLAttribute(struct _xmlAttr *attrPtrArg)
    : attrPtr(attrPtrArg)
{
}

inline XMLAttribute::XMLAttribute(const XMLAttribute& rhs)
    : attrPtr(rhs.attrPtr)
{
}

inline XMLAttribute::~XMLAttribute()
{
    attrPtr = NULL;
}

inline XMLAttribute& XMLAttribute::operator=(const XMLAttribute& rhs)
{
    // Not worth checking for self-assignment, since this is safe to do anyway.
    attrPtr = rhs.attrPtr;

    return *this;
}

inline bool XMLAttribute::isValid() const
{
    return attrPtr != NULL;
}

END_PRIVATE_NAMESPACE

#endif	// not defined XML_ATTRIBUTE_H
