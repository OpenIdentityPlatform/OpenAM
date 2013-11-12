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

#include "xml_attribute.h"

USING_PRIVATE_NAMESPACE

bool XMLAttribute::getName(std::string& name) const {
    bool valid = false;
    BSTR bstr;
    if (attrPtr) {
        if ((attrPtr->get_nodeName(&bstr)) == S_OK) {
            name = bstrToString(bstr).c_str();
            valid = true;
        }
    }
    return valid;
}

bool XMLAttribute::isNamed(const std::string& name) const {
    bool valid = false;
    BSTR bstr;
    if (attrPtr) {
        if ((attrPtr->get_nodeName(&bstr)) == S_OK) {
            valid = matchesXMLString(name, bstr);
        }
    }
    return valid;
}

bool XMLAttribute::getValue(std::string& value) const {
    bool found = false;
    BSTR bstr;
    if (attrPtr) {
        if ((attrPtr->get_text(&bstr)) == S_OK) {
            value = bstrToString(bstr).c_str();
            found = true;
        }
    }
    return found;
}

bool XMLAttribute::next() {
    if (attrPtr) {
        attrPtr = attrPtr->GetnextSibling();
    }
    return isValid();
}

void XMLAttribute::log(Log::ModuleId logModule, Log::Level level,
        unsigned int depth) const {
    if (Log::isLevelEnabled(logModule, level)) {
        if (attrPtr) {
            BSTR bname, bval;
            std::string name, value;
            if ((attrPtr->get_nodeName(&bname)) == S_OK) {
                name = bstrToString(bname);
            }
            if ((attrPtr->get_text(&bval)) == S_OK) {
                value = bstrToString(bval);
            }
            Log::log(logModule, level,
			 "%*sAttribute = %s, value = %s",
			 depth * 2 + 1, "", name.c_str(), value.c_str());
        } else {
            Log::log(logModule, level, "XMLAttribute::log() invalid attribute");
        }
    }
}
