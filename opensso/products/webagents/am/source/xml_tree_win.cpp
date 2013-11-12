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

#include "xml_tree.h"
#include <sstream>

USING_PRIVATE_NAMESPACE

bool XMLTree::initialized = false;

XMLTree::XMLTree(bool validate, const char *docBuffer, std::size_t bufferLen) : docPtr(NULL), rootPtr(NULL) {
    VARIANT_BOOL status = VARIANT_FALSE;

    if (static_cast<const char *> (NULL) == docBuffer) {
        throw ParseException("XMLTree() empty document (NULL data buffer)");
    }

    HRESULT hr = docPtr.CreateInstance(__uuidof(MSXML2::DOMDocument60));
    if (SUCCEEDED(hr)) {
        docPtr->async = VARIANT_FALSE;
        docPtr->validateOnParse = VARIANT_TRUE;
        docPtr->raw_loadXML(_bstr_t(docBuffer), &status);
        if (status == VARIANT_TRUE) {
            rootPtr = docPtr->GetdocumentElement();
        } else {
            if (!validate) {
                throw ParseException("XMLTree() empty document (XML validation disabled)");
            } else {
                long errc = 0, errl = 0, errp = 0;
                IXMLDOMParseErrorPtr errPtr = docPtr->GetparseError();
                if (errPtr) {
                    BSTR reason;
                    std::ostringstream ss;
                    errPtr->get_errorCode(&errc);
                    errPtr->get_line(&errl);
                    errPtr->get_linepos(&errp);
                    errPtr->get_reason(&reason);
                    ss << "XMLTree() docBuffer does not validate (source line: "
                            << errl << ", char: " << errp << ", error code: 0x"
                            << std::hex << std::uppercase << errc << "): " << bstrToString(reason);
                    throw ParseException(ss.str());
                } else {
                    throw ParseException("XMLTree() docBuffer does not validate");
                }
            }
        }
    } else {
        _com_error er(hr);
        std::ostringstream ss;
        ss << "XMLTree() unable to parse docBuffer: " << er.ErrorMessage();
        throw ParseException(ss.str());
    }

    if (static_cast<MSXML2::IXMLDOMElementPtr> (NULL) == rootPtr) {
        throw ParseException("XMLTree() empty document");
    }
}

XMLTree::~XMLTree() {
    docPtr = 0;
}

void XMLTree::initialize() {
}

void XMLTree::shutdown() {
}

XMLElement XMLTree::getRootElement() const {
    XMLElement a;
    if (rootPtr) {
        a.nodePtr = rootPtr;
    }
    return a;
}

void XMLTree::log(Log::ModuleId logModule, Log::Level level) const {
    if (Log::isLevelEnabled(logModule, level)) {
        getRootElement().log(logModule, level);
    }
}
