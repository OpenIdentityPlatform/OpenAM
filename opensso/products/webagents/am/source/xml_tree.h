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
 * $Id: xml_tree.h,v 1.3 2008/06/25 08:14:42 qcheng Exp $
 *
 * Abstract:
 *
 * Representation ofan XML parse tree.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef XML_TREE_H
#define XML_TREE_H

#include "internal_macros.h"
#include <exception>
#include <stdexcept>
#include "xml_utilities.h"
#include "xml_element.h"

BEGIN_PRIVATE_NAMESPACE

class XMLTree {
public:

    class ParseException : public std::exception {
    public:

        explicit ParseException(const std::string& msg)
        : std::exception(), message(msg) {
        }

        ~ParseException() throw () {
        }

        const char *what() const throw () {
            return "XMLTree::ParseException";
        }

        const std::string& getMessage() const {
            return message;
        }

    private:
        std::string message;
    };

    class Init {
    public:

        explicit Init()
#ifdef _MSC_VER
        : hr(CoInitializeEx(NULL, COINIT_MULTITHREADED))
#endif
        {
        }

        ~Init() {
#ifdef _MSC_VER
            if (SUCCEEDED(hr))
                CoUninitialize();
#endif
        }
    private:
#ifdef _MSC_VER
        HRESULT hr;
#endif
        Init(const Init&);
        Init& operator=(const Init&);
    };

    //
    // Constructs an XMLTree object based on the contents of the specified
    // string.  If the string cannot be parsed, then a ParseException is
    // thrown.
    //
    // Parameters:
    //   validate
    //		validate the parsed XML against the appropriate DTD
    //
    //   docBuffer
    //		buffer containing the XML to be parsed.  The data is
    //		expected to be UTF-8 encoded.
    //
    //   bufferLen
    //		length of the docBuffer.  If not specified, i.e. zero,
    //		then the docBuffer is assumed to be a NUL terminated string.
    //
    // Throws:
    //   std::bad_alloc
    //		if memory cannot be allocated to hold the parsed XML
    //
    //   std::invalid_argument
    //		if docBuffer is NULL or the empty string
    //
    //		if the length of the docBuffer is greater than INT_MAX
    //
    //   ParseException
    //		if the specified string cannot be parsed
    //
    XMLTree(bool validate, const char *docBuffer, std::size_t bufferLen = 0);

    //
    // Destroys a previous constructed tree.
    //
    ~XMLTree();

    //
    // Get the root node of the XML tree.
    //
    // Returns:
    //		A an XMLElement representing the root Element of the
    //		XML tree.
    //
    XMLElement getRootElement() const;

    //
    // Recursively logs the name and content of every element in the
    // tree to the specified logging module at the specified level.
    //
    // Parameters:
    //   logModule
    //		module id of the logging module to use
    //
    //   level
    //		logging level to associate with the generated messages
    //
    void log(Log::ModuleId logModule, Log::Level level) const;

    //
    // Initializes the XML parsing code and any internal data structures.
    //
    static void initialize();

    //
    // Cleans up the XML parsing code and any internal data structures.
    //
    static void shutdown();

private:
    XMLTree(const XMLTree& rhs); // not implemented
    XMLTree& operator=(const XMLTree& rhs); // not implemented

    static bool initialized;

#ifdef _MSC_VER
    MSXML2::IXMLDOMDocument2Ptr docPtr;
    MSXML2::IXMLDOMElementPtr rootPtr;
#else
    struct _xmlDoc *docPtr;
#endif

};

END_PRIVATE_NAMESPACE

#endif	// XML_TREE_H
