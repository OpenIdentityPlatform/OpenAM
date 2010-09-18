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
 * $Id: properties.h,v 1.7 2008/06/25 08:14:35 qcheng Exp $
 *
 */
#ifndef PROPERTIES_H
#define PROPERTIES_H

#include <map>
#include <string>
#include <am_types.h>

#include "internal_macros.h"
#include "log.h"
#include "utils.h"
#include "key_value_map.h"

BEGIN_PRIVATE_NAMESPACE

class Properties: public std::map<std::string, std::string, Utils::CmpFunc> {
public:
#if	defined(BROKEN_MSVC)
    typedef referent_type mapped_type;
#endif
    //
    // Creates an empty properties object.
    //
    Properties(bool ignore_case = false);

    // 
    // Creates a properties object as a copy of the suppled properties
    // and takes a Log::ModuleId for logging purposes.
    //
    Properties(const Properties &props, Log::ModuleId moduleID);

    // 
    // Creates a properties object as a copy of the suppled properties
    //
    Properties(const Properties &props);

    //
    // Destroys the properties object.
    //
    // Throws:	Nothing
    //
    ~Properties();

    // 
    // Sets the logID to be used when logging
    //
    void setLogID(Log::ModuleId moduleID);

    //
    // Loads property information from the specified file.  The file
    // is expected to use the standard Java Properties file syntax.
    //
    // Parameters:
    //   fileName
    //		name of the file from which to load the property information
    //
    // Returns:
    //   AM_SUCCESS
    //		if no error is detected
    //
    //   AM_NOT_FOUND
    //		if the specified file does not exist
    //
    //   AM_NSPR_ERROR
    //		if there is a problem accessing the file
    //
    //   AM_INVALID_ARGUMENT
    //		if fileName is an empty string
    //
    // Throws:
    //   std::bad_alloc
    //		if unable to allocate memory to store the property information
    //
    am_status_t load(const std::string& fileName);

    //
    // Take a property string that has key-value pairs and stores it
    // in this property.
    //
    //
    // Parameters:
    //   propertyStr
    //		String object containing the name value pairs with the
    //		expected format.
    //
    //	 kvpsep
    //		The seperation character used to seperate the key-value-pairs.
    //
    //	 kvsep
    //		The seperation character used to seperate the key and value.
    //
    // Returns:
    //   AM_SUCCESS
    //		if no error is detected
    //
    //
    // Throws:
    //   std::bad_alloc
    //		if unable to allocate memory to store the property information
    //
    void parsePropertyKeyValue(const std::string &propertyStr,
			       const char kvpsep, const char kvsep);


    //
    // Stores the property information in the specified file.
    //
    // Parameters:
    //   fileName
    //		name of the file in which to store the property information
    //
    // Returns:
    //   AM_SUCCESS
    //		if no error is detected
    //
    //   AM_NSPR_ERROR
    //		if there is a problem writing the properties to the file
    //
    //   AM_INVALID_ARGUMENT
    //		if fileName is an empty string
    //
    am_status_t store(const std::string& fileName) const;

    //
    // Log all of the entries in the properties object.
    //
    // Parameters:
    //   module
    //		logging module to use for the log messages
    //
    //   level	logging level to use for the log messages
    //
    void log(Log::ModuleId module, Log::Level level) const;

    //
    // Determine whether the object contains property with the specified name.
    //
    // Parameters:
    //   key	name of the property to look up
    //
    // Returns:
    //   true	if the property has a value
    //   false	otherwise
    //
    bool isSet(const std::string& key) const;

    //
    // The next eight methods retrieve values from the properties map.
    // The following parameters and exceptions are common to the
    // collection of methods.  The return values are specified with
    // the each related pair of methods.
    //
    // Parameters:
    //   key	name of the property to look up
    //
    //   defaultValue
    //		default value to use if there is no value associated
    //		with the specified key.
    //
    //   terse	enables logging of calls that result in the use of
    //		defaultValue when the specified key does not have a
    //		a valid value supplied in the properties.	
    //		
    //
    // Throws:
    //   std::invalid_argument
    //		if the specified key has no associated value and a
    //		default value is not provided.
    //
    //   std::domain_error
    //		if the value associated with the specified key cannot be
    //		parsed as required by the particular accessor method
    //
    //
    // Returns:
    //		the (unparsed) string form of the value associated with
    //		the specified key
    //
    const std::string& get(const std::string& key) const;
    const std::string& get(const std::string& key,
			   const std::string& defaultValue,
			   bool terse = true) const;

    void create_old_to_new_attributes_map();

    //
    // Returns:
    //   true	if the value associated with the specified key is one
    //		of: true, on, or yes.
    //
    //   false	if the value associated with the specified key is one
    //		of: false, off, or no.
    //
    // NOTE: If the associated value does not match any of the
    // recognized boolean values, then an std::domain_error exception
    // will be thrown.
    //
    bool getBool(const std::string& key) const;
    bool getBool(const std::string& key, bool defaultValue,
					 bool terse = true) const;

    //
    // Returns:
    //		the signed integer value associated with the specified key
    //
    // NOTE: If the associated value cannot be parsed as an integer or
    // cannot be represented in the range LONG_MIN to LONG_MAX, then an
    // std::range_error exception will be thrown.
    //
    long getSigned(const std::string& key) const;
    long getSigned(const std::string& key, long defaultValue,
					   bool terse = true) const;

    //
    // Returns:
    //		the unsigned integer value associated with the specified key
    //
    // NOTE: If the associated value cannot be parsed as an integer or
    // cannot be represented in the range 0 to ULONG_MAX, then an
    // std::range_error exception will be thrown.
    //
    unsigned long getUnsigned(const std::string& key) const;
    unsigned long getUnsigned(const std::string& key,
			      unsigned long defaultValue,
			      bool terse = true) const;
    //
    // Returns:
    //    the unsigned positive integer value associated with the specified key
    //
    unsigned long getPositiveNumber(const std::string& key) const;
    unsigned long getPositiveNumber(const std::string& key,
		             unsigned long defaultValue) const;

    //
    // Sets the value associated with the specified key.  The specified
    // value will replace any previously existing value.
    //
    // Parameters:
    //   key	the key to modify
    //
    //   value	the value to associate with the specified key
    //
    // Returns:
    //   Nothing
    //
    // Throws:
    //   std::bad_alloc
    //		if unable to allocate memory to store the new key/value.
    //
    void set(const std::string& key, const std::string& value);
    void set_list(const std::string& key,
                  const std::string& valueSep);
    void set_map(const std::string& key,
                         const std::string& mapSep,
                         const std::string& valueSep);


    const_iterator vfind(const Properties::mapped_type &value) const;
private:
    bool parseBool(const std::string& key, const std::string& value) const;

    long parseSigned(const std::string& key, const std::string& value) const;

    unsigned long parseUnsigned(const std::string& key,
				const std::string& value) const;

    am_status_t parseBuffer(char *buffer);
    const char* get_new_property_name(const std::string& key) const;
    const char* get_old_property_name(const std::string& key) const;


    Log::ModuleId logID;
    bool icase;
    KeyValueMap *newAttributesMap;
};


END_PRIVATE_NAMESPACE

#endif	// not PROPERTIES_H
