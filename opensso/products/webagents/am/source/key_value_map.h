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
 * $Id: key_value_map.h,v 1.3 2008/06/25 08:14:32 qcheng Exp $
 *
 *
 * Abstract:
 *
 * Key-value map for use in the DSAME Remote Client SDK.  The map is
 * from a string key to to one or more string values, which are
 * represented as an standard C++ vector.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef KEY_VALUE_MAP_H
#define KEY_VALUE_MAP_H

#include <map>
#include <string>
#include <vector>
#include "xml_element.h"
#include "internal_macros.h"
#include "ref_cnt_ptr.h"
#include "utils.h"
BEGIN_PRIVATE_NAMESPACE

typedef std::map<std::string, std::vector<std::string>,
    Utils::CmpFunc> KeyValueMapBaseClassType;

class KeyValueMap: public KeyValueMapBaseClassType, public RefCntObj
{
public:
    KeyValueMap(bool ignore_case=false):
	std::map<std::string,std::vector<std::string>,
		 Utils::CmpFunc>(Utils::CmpFunc(ignore_case)) {}

    KeyValueMap(const KeyValueMap &kvm):KeyValueMapBaseClassType(kvm){}

    //
    // Inserts the XML AttributeValuePair element referred by elem into
    // the map.
    //
    // Parameters:
    //   elem	the XMLElement from which to retrieve the data
    //
    // Throws:
    //   std::invalid_argument
    //		if elem does not refer to an XML AttributeValuePair element
    //		or if the element does not contain the appropriate
    //		sub-elements.
    //
    void insert(XMLElement elem); 

    void insert(const std::string &key, const std::string &value,
		bool replace_values=false);

    using KeyValueMapBaseClassType::insert;

    void merge(const KeyValueMap &);

    // Throws: std::invalid_argument, other std::exception's. 
    void parseKeyValuePairString(const std::string &, const char, const char,
				 bool sortValues=false,
				 bool sortValuesCaseIgnore=false); 

    am_status_t for_each(am_status_t (*func)(const char *,
					     const char *,
					     void **args),
			 void **args) const;

    virtual ~KeyValueMap() {};
};

typedef RefCntPtr<KeyValueMap> KVMRefCntPtr;
END_PRIVATE_NAMESPACE

#endif	// not KEY_VALUE_MAP_H
