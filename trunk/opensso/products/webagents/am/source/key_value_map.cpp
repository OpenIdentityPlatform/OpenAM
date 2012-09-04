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
 * $Id: key_value_map.cpp,v 1.4 2009/10/13 01:38:15 robertis Exp $
 *
 */ 

#include <stdexcept>

#include "key_value_map.h"
using std::string;
USING_PRIVATE_NAMESPACE


/**
 * Throws std::invalid_argument, and other std::exception's from XMLTree.
 */
void KeyValueMap::insert(XMLElement elem)
{
    elem.assertName(ATTRIBUTE_VALUE_PAIR);

    XMLElement attributeElement;

    if (elem.getSubElement(ATTRIBUTE, attributeElement)) {
	std::string attrName;

	if (! attributeElement.getAttributeValue("name", attrName)) {
	    throw std::invalid_argument("KeyValueMap::insert() no attribute name");
	}
	KeyValueMap::mapped_type& values = (*this)[attrName];

	for (elem = elem.getFirstSubElement(); elem.isValid();
	     elem.nextSibling()) {
	    if (elem.isNamed(VALUE)) {
		std::string value;

		if (elem.getValue(value)) {
		    values.push_back(value);
		} else {
		    // XXX - this is unexpected
		}
	    } else if (elem.isNamed(ATTRIBUTE)) {
		continue;
	    } else {
		// XXX - What should be done here: unexpected element?
	    }
	}
    } else {
	// XXX - What should be done here?
    }
}

void
KeyValueMap::insert(const std::string &key, const std::string &value,
		    bool replace_values) {
    KeyValueMap::iterator iter = find(key);
    if (iter != end()) {
	if (replace_values) {
	    KeyValueMap::mapped_type newEntry;
	    newEntry.push_back(value);
	    newEntry.swap(iter->second);
	} else {
	    iter->second.push_back(value);
	}
    } else {
	KeyValueMap::mapped_type newEntry;
	newEntry.push_back(value);
	newEntry.swap((*this)[key]);
    }
}

void KeyValueMap::merge(const KeyValueMap &kvm) {
    KeyValueMap::const_iterator iter;
    for(iter = kvm.begin(); iter != kvm.end(); iter++) {
	std::string key = (*iter).first;
	KeyValueMap::mapped_type urValue = (*iter).second;

	iterator myiter = find(key);
	if(myiter != end()) {
	    KeyValueMap::mapped_type myValue = (*myiter).second;
	    myValue.insert(myValue.end(), urValue.begin(), urValue.end());
	} else {
	    (*this)[key] = urValue;
	}
    }
    return;
}

/**
 * Throws std::invalid_argument and other std::exception's
 */
void KeyValueMap::parseKeyValuePairString(const std::string &keyValStr,
					  const char kvpsep,
					  const char kvsep,
					  bool sortValues,
					  bool sortValueCaseIgnore) 
{
    std::size_t startPos = 0, tmpPos = 0;
    std::string kvpsepStr;
    std::string kvsepStr;
    PUSH_BACK_CHAR(kvsepStr, kvsep);
    PUSH_BACK_CHAR(kvpsepStr, kvpsep);

    do {
      tmpPos = keyValStr.find(kvpsepStr, startPos);
	std::string nvpair = keyValStr.substr(startPos, tmpPos - startPos);
	if(nvpair.size() > 0) {
	    std::string key;
	    std::size_t eqPos = nvpair.find(kvsepStr);

        std::string value;
        if (eqPos != string::npos) {
            key = nvpair.substr(0, eqPos);
            value = nvpair.substr(eqPos + 1, tmpPos - eqPos);
        } else {
            key.assign(nvpair);
            value = "";
        }

	    iterator iter = find(key);
	    if(iter != end()) {
		mapped_type &values = iter->second;
		values.push_back(value);
	    } else {
		mapped_type values;
		values.push_back(value);
		KeyValueMapBaseClassType::insert(KeyValueMap::value_type(key,
									 values));
	    }
	}
	startPos = tmpPos + 1;
    } while(tmpPos != std::string::npos);

    if(sortValues) {
	for(iterator iter = begin(); iter != end(); ++iter) {
	    KeyValueMap::mapped_type &values = iter->second;
	    std::stable_sort(values.begin(), values.end(),
			     Utils::CmpFunc(sortValueCaseIgnore));
	}
    }
    return;
}


/**
 * Throws std::invalid_argument, and other std::exception's
 */
am_status_t
KeyValueMap::for_each(am_status_t (*func)(const char *,
					  const char *,
					  void **args),
		      void **args) const {
    am_status_t retVal = AM_SUCCESS;
    if(size() > 0) {
	KeyValueMap::const_iterator iter = begin();
	for(; iter != end(); iter++) {
	    const KeyValueMap::key_type &key = iter->first;
	    const KeyValueMap::mapped_type &values = iter->second;
	    std::size_t numElems = values.size();
	    if(numElems) {
		for(size_t i = 0; i < numElems; ++i) {
		    if((retVal = func(key.c_str(),
				      values[i].c_str(), args)) != AM_SUCCESS)
			break;
		}
	    }
	    if(retVal != AM_SUCCESS) break;
	}
    }
    return retVal;
}
