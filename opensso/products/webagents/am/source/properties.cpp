/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: properties.cpp,v 1.12 2008/09/13 01:11:53 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <cerrno>
#include <cstdlib>
#include <stdexcept>
#include "internal_macros.h"
#include "properties.h"
#include "am_web.h"
#include <cstring>
#include <sys/types.h>
#include <sys/stat.h>

#ifdef _MSC_VER
#define stat _stat
#endif

USING_PRIVATE_NAMESPACE

namespace {
    enum {
	COMMENT = 0x1,
	WHITESPACE = 0x2,
	SEPARATOR = 0x4,
	END_OF_LINE = 0x8,
	ESCAPE = 0x10
    };

    int charType[256] = {
	// 0 - 31: control characters
	SEPARATOR|END_OF_LINE, 0, 0, 0, 0, 0, 0, 0,
	0, WHITESPACE|SEPARATOR, WHITESPACE|SEPARATOR|END_OF_LINE, 0,
	0, WHITESPACE|SEPARATOR|END_OF_LINE, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0,

	// 32 - 47: miscellaneous symbols
	WHITESPACE|SEPARATOR, COMMENT, 0, COMMENT, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0,

	// 48 - 63: mostly numbers
	0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, SEPARATOR, 0, 0, SEPARATOR, 0, 0,

	// 64 - 95: mostly upper case letters
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ESCAPE, 0, 0, 0,

	// 96 - 127: mostly lower case letters
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

	// 128 - 255: Non-ASCII characters
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    inline bool isComment(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & COMMENT) != 0;
    }

    inline bool isWhitespace(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & WHITESPACE) != 0;
    }

    inline bool isSeparator(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & SEPARATOR) != 0;
    }

    inline bool isEndOfLine(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & END_OF_LINE) != 0;
    }

    inline bool isEscape(char data)
    {
	return (charType[static_cast<unsigned char>(data)] & ESCAPE) != 0;
    }

    inline char *skipWhitespace(char *data)
    {
	while (isWhitespace(*data)) {
	    data += 1;
	}

	return data;
    }

    inline char *findEndOfLine(char *data)
    {
	while (! isEndOfLine(*data)) {
	    data += 1;
	}

	return data;
    }

    inline char *findStartOfNewLine(char *data)
    {
	while (*data && isEndOfLine(*data)) {
	    data += 1;
	}

	return skipWhitespace(data);
    }

    inline char *findSeparator(char *data)
    {
	while (! isSeparator(*data)) {
	    data += 1;
	}

	return data;
    }

    inline char *skipComment(char *data)
    {
	if (isComment(*data)) {
	    data = findEndOfLine(data);
	    data = findStartOfNewLine(data);
	}

	return data;
    }

    inline char *skipWhitespaceAndComments(char *data)
    {
	do {
	    data = skipWhitespace(data);
	    data = skipComment(data);
	} while (isWhitespace(*data) || isComment(*data));

	return data;
    }

    //
    // Finds the end of the current line and replaces the first end-of-line
    // character with a NUL.
    //
    // Parameters:
    //   start	where to start searching for the end of the line
    //
    // Returns:
    //   A pointer to the first character non-whitespace character after
    // the end of the line or a pointer to the NUL terminating the string
    // if no such character exists.
    //
    char *terminateLine(char *start)
    {
	char *endOfLine = findEndOfLine(start);
	char *nextLine = findStartOfNewLine(endOfLine);
	bool done;

	done = ! *nextLine || endOfLine == start || ! isEscape(endOfLine[-1]);
	while (! done) {
	    char *endOfNextLine = findEndOfLine(nextLine);
	    std::size_t len = endOfNextLine - nextLine;
	    char *copyDest = endOfLine;

	    if (isEscape(endOfLine[-1])) {
		copyDest -= 1;
	    }
	    std::memcpy(copyDest, nextLine, len);
	    endOfLine = copyDest + len;
	    nextLine = findStartOfNewLine(endOfNextLine);
	    done = (! *nextLine || ! isEscape(endOfLine[-1]));
	}

	// Remove of any trailing newline escape at the end of the file.
	if (endOfLine > start && isEscape(endOfLine[-1])) {
	    endOfLine[-1] = '\0';
	} else {
	    *endOfLine = '\0';
	}

	return nextLine;
    }
}

Properties::Properties(bool ignore_case)
    : std::map<std::string, std::string,
	       Utils::CmpFunc>(Utils::CmpFunc(ignore_case)),
    logID(Log::ALL_MODULES), icase(ignore_case) {
    logID = Log::ALL_MODULES;
}

Properties::Properties(const Properties &props, Log::ModuleId moduleID)
    : std::map<std::string, std::string, Utils::CmpFunc>(props),
    logID(props.logID), icase(props.icase)
{
    logID = moduleID;
}

Properties::Properties(const Properties &props)
    : std::map<std::string, std::string, Utils::CmpFunc>(props),
    logID(props.logID), icase(props.icase)
{
}

Properties::~Properties()
{
}

void Properties::setLogID(Log::ModuleId moduleID)
{
    logID = moduleID;
}

am_status_t Properties::parseBuffer(char *buffer)
{
    am_status_t status = AM_SUCCESS;
    char *nextLine;
    std::size_t len;

    try {
	for (buffer = skipWhitespaceAndComments(buffer);
	     *buffer;
	     buffer = skipWhitespaceAndComments(nextLine)) {

	    char *start = buffer;
	    nextLine = terminateLine(buffer);

	    // XXX - Should handle backslash escapes in the key
	    buffer = findSeparator(start);
	    if (start == buffer) {
		break;
	    }

	    std::string key(start, buffer - start);

	    buffer = skipWhitespace(buffer);
	    if (*buffer && isSeparator(*buffer)) {
		buffer += 1;
		buffer = skipWhitespace(buffer);
	    }

	    len = strlen(buffer) -1;
	    while ((len > 0) && (buffer[len] == ' ')) {
		buffer[len--] = '\0';
	    }
            
            // XXX - Should handle backslash escapes in the value
            set(key, buffer);
	}
	if (*buffer) {
	    status = AM_FAILURE;
	}
    } catch (const std::bad_alloc&) {
	status = AM_NO_MEMORY;
    }

    return status;
}

am_status_t Properties::load(const std::string& fileName) {
    am_status_t status = AM_SUCCESS;
    FILE *propFile;

    propFile = fopen(fileName.c_str(), "rb");
    if (NULL != propFile) {
        struct stat st;
        if (stat(fileName.c_str(), &st) == 0 && st.st_size > 0) {
            try {
                std::size_t readCount;
                char *buffer = new char[st.st_size + 1];

                readCount = fread(buffer, 1, st.st_size, propFile);
                if (readCount == st.st_size) {
                    buffer[readCount] = '\0';
                    status = parseBuffer(buffer);
                } else {
                    Log::log(Log::ALL_MODULES,
                            Log::LOG_ERROR,
                            "am_properties_load(): "
                            "Could not load properties file %s: "
                            "Number of bytes read (%d) "
                            "did not match expected file size (%d).",
                            fileName.c_str(), readCount, st.st_size);
                    status = AM_BUFFER_TOO_SMALL;
                }

                delete[] buffer;
            } catch (const std::bad_alloc&) {
                status = AM_NO_MEMORY;
            }

        } else {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "am_properties_load(): "
                    "Error getting info for properties file %s (unable to determine file size)",
                    fileName.c_str());
            status = AM_END_OF_FILE;
        }
        fclose(propFile);
    } else {
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                "am_properties_load(): "
                "Error opening properties file %s (unable to open file for reading)",
                fileName.c_str());
        status = AM_NOT_FOUND;
    }

    return status;
}

am_status_t Properties::store(const std::string& fileName) const
{
    am_status_t status = AM_SUCCESS;
    FILE *propFile;

    propFile = fopen(fileName.c_str(), "w");
    if (NULL != propFile) {
	int rc = 0;

	for (const_iterator iter = begin(); iter != end(); ++iter) {
	    rc = fprintf(propFile, "%s=%s\n", iter->first.c_str(),
			    iter->second.c_str());
	    if (-1 == rc) {
		break;
	    }
	}
	if (0 != fclose(propFile) || -1 == rc) {
	    status = AM_NSPR_ERROR;
	}
    } else {
	status = AM_NSPR_ERROR;
    }

    return status;
}

void Properties::log(Log::ModuleId module, Log::Level level) const
{
    if (Log::isLevelEnabled(module, level)) {
	for (const_iterator iter = begin(); iter != end(); ++iter) {
	    Log::log(module, level, "%s=%s\n", iter->first.c_str(),
		     iter->second.c_str());
	}
    }
}

bool Properties::isSet(const std::string& key) const
{
    return find(key) != end();
}

/* Throws std::invalid_argument if a value for key is not found. */
const std::string& Properties::get(const std::string& key) const
{
    const_iterator iter = find(key);
    if (iter == end()) {
        throw std::invalid_argument(key + " not found");
    }
    return iter->second;
}

const std::string& Properties::get(const std::string& key,
				   const std::string& defaultValue,
				   bool terse) const
{
    const_iterator iter = find(key);
    if (iter == end()) {
        if (terse) {
	    Log::log(logID, Log::LOG_MAX_DEBUG, 
	        "No value specified for key %s, using default value %s.", 
		    key.c_str(), defaultValue.empty() ? "\"\"" : defaultValue.c_str());
        }
	return defaultValue;
    } else {
	try {
	    return iter->second;
        } catch (...) {
            if (terse) {
	        Log::log(logID, Log::LOG_MAX_DEBUG,
	         "Invalid value specified for key %s, using default value %s.", 
		    key.c_str(), defaultValue.empty() ? "\"\"" : defaultValue.c_str());
            }
	    return defaultValue;
        }
    }
}

/* Throws 
 *	std::range_error if value is too small or large.
 *	std::domain_error if valid is invalid otherwise.
 */
unsigned long Properties::parseUnsigned(const std::string& key,
					const std::string& value) const
{
    unsigned long result;
    char *endPtr;

    errno = 0;
    result = std::strtoul(value.c_str(), &endPtr, 0);
    if (0 != errno || *endPtr) {
	if (ERANGE == errno) {
	    throw std::range_error(key + " has too large a value: " + value);
	} else {
	    throw std::domain_error(key + " does not have a unsigned value: "
				    + value);
	}
    }

    return result;
}

/* Throws std::invalid_argument if a value for key is not found */
unsigned long Properties::getPositiveNumber(const std::string& key) const
{
    unsigned long result;
    result = getUnsigned(key);
    if(result == 0){
	throw std::invalid_argument(key + " Property value set to 0");
    }
   return result;
}

unsigned long Properties::getPositiveNumber(const std::string& key, 
					unsigned long defaultValue) const
{
    unsigned long result;
    result = getUnsigned(key,defaultValue);
    if(result == 0){
	Log::log(logID, Log::LOG_MAX_DEBUG,"Property [%s] value set to 0",key.c_str());
	result = defaultValue;
	Log::log(logID, Log::LOG_MAX_DEBUG,"Resetting Property [%s] value back to default : %d",key.c_str(),defaultValue);
    }
   return result;
}

/* Throws 
 *      std::invalid_argument if a value for key is not found
 *	std::range_error if value is too small or large.
 *	std::domain_error if valid is invalid otherwise.
 */
unsigned long Properties::getUnsigned(const std::string& key) const
{
    const_iterator iter = find(key);

    if (iter == end()) {
        throw std::invalid_argument(key + " not found");
    }
    return parseUnsigned(key, iter->second);
}

unsigned long Properties::getUnsigned(const std::string& key,
				      unsigned long defaultValue,
				      bool terse) const
{
    unsigned long result;
    const_iterator iter = find(key);

    if (iter == end()) {
        if (terse) {
	    Log::log(logID, Log::LOG_MAX_DEBUG,
	         "No value specified for key %s, using default value %ld.", 
			key.c_str(), defaultValue);
        }
	result = defaultValue;
    } else {
	try {
	   result = parseUnsigned(key, iter->second);
        } catch (...) {
           if (terse) {
	       Log::log(logID, Log::LOG_MAX_DEBUG,
	         "Invalid value specified for key %s, using default value %ld.", 
	               key.c_str(), defaultValue);
           }
           result = defaultValue;
	}
    }
    return result;
}

/* Throws 
 *	std::range_error if value is too small or large.
 *	std::domain_error if valid is invalid otherwise.
 */
long Properties::parseSigned(const std::string& key,
			     const std::string& value) const
{
    long result;
    char *endPtr;

    errno = 0;
    result = std::strtol(value.c_str(), &endPtr, 0);
    if (0 != errno || *endPtr) {
	if (ERANGE == errno) {
	    throw std::range_error(key + " has too large a value: " + value);
	} else {
	    throw std::domain_error(key + " does not have a signed value: "
				    + value);
	}
    }

    return result;
}

/* Throws 
 *      std::invalid_argument if any argument is invalid.
 *	std::range_error if value is too small or large.
 *	std::domain_error if valid is invalid otherwise.
 */
long Properties::getSigned(const std::string& key) const
{
    const_iterator iter = find(key);
    if (iter == end()) {
        throw std::invalid_argument(key + " not found");
    }
    return parseSigned(key, iter->second);
}

long Properties::getSigned(const std::string& key, long defaultValue,
						   bool terse) const
{
    long result;  
    const_iterator iter = find(key);

    if (iter == end()) {
        if (terse) {
	    Log::log(logID, Log::LOG_MAX_DEBUG, 
	         "No value specified for key %s, using default value %ld.", 
			key.c_str(), defaultValue);
        }
	result =  defaultValue;
    } else {
	try {
	   result = parseSigned(key, iter->second);
        } catch (...) {
           if (terse) {
	       Log::log(logID, Log::LOG_MAX_DEBUG,
	         "Invalid value specified for key %s, using default value %ld.", 
		    key.c_str(), defaultValue);
           }
           result = defaultValue;
        }
    }

    return result;
}

/*
 *  This implementation is probably incomplete.
 *
 * Throws 
 *	std::domain_error if value does not have a boolean value of  
 *	    "true", "yes", "on", "false", "no", "off", all case insensitive.
 */
bool Properties::parseBool(const std::string& key,
			   const std::string& value) const
{
    bool result;
  
    const char *cvalue = value.c_str();
    if (strcasecmp(cvalue, "true") == 0 || strcasecmp(cvalue, "on") == 0 
        || strcasecmp(cvalue, "yes") == 0) {
        result = true;
    } else if (strcasecmp(cvalue, "false") == 0 || strcasecmp(cvalue, "off") == 0
        || strcasecmp(cvalue, "no") == 0) {
        result = false;
    } else {
	throw std::domain_error(
		    key + " does not have a boolean value: " + value);
    }

    return result;
}

/*
 * Throws 
 *	std::invalid_argument if a value for key is not found.
 *	std::domain_error if value does not have a boolean value of  
 *	    "true", "yes", "on", "false", "no", "off", all case insensitive.
 */
bool Properties::getBool(const std::string& key) const
{
    const_iterator iter = find(key);

    if (iter == end()) {
        throw std::invalid_argument(key + " not found");
    }
    return parseBool(key, iter->second);
}

bool Properties::getBool(const std::string& key, bool defaultValue,
						 bool terse) const
{
    bool result;  
    const_iterator iter = find(key);

    if (iter == end()) {
        if (terse) {
	    Log::log(logID, Log::LOG_MAX_DEBUG,
	         "No value specified for key %s, using default value %s.", 
			key.c_str(), defaultValue ? "TRUE" : "FALSE");
        }
	result = defaultValue;
    } else {
	try {
	    result = parseBool(key, iter->second);
        } catch (...) {
            if (terse) {
	        Log::log(logID, Log::LOG_MAX_DEBUG,
		  "Invalid value specified for key %s, using default value %s.", 
		      key.c_str(), defaultValue ? "TRUE" : "FALSE");
            }
            result = defaultValue;
	}
    }

    return result;
}

void Properties::set(const std::string& key, const std::string& value)
{
    (*this)[key] = value;
}

void
Properties::parsePropertyKeyValue(const std::string &property,
				  const char kvpsep,
				  const char kvsep) {
    size_t coma = 0, curPos = 0;
    std::string attrs(property);

    Utils::trim(attrs);
    size_t len = attrs.size();

    while(coma < len) {
	coma = attrs.find(kvpsep, curPos);
	std::string attrPair = attrs.substr(curPos, coma - curPos);

	while(coma < len && attrs.at(coma) == kvpsep) coma++;

	curPos = coma;

	Utils::trim(attrPair);
	if(attrPair.size() == 0)
	    continue;

	size_t pipe = attrPair.find(kvsep);
	if(pipe != std::string::npos) {
	    std::string key = attrPair.substr(0, pipe);
	    std::string value = attrPair.substr(pipe + 1);

	    Utils::trim(key);
	    Utils::trim(value);
	    set(key, value);
	}
    }
    return;
}


Properties::const_iterator
Properties::vfind(const Properties::mapped_type &v) const {
    Properties::const_iterator map_iter = begin();
    for(; map_iter != end(); map_iter++) {
	const Properties::mapped_type &value = map_iter->second;
	if(icase) {
	    if(strcasecmp(v.c_str(), value.c_str()) == 0) break;
	} else {
	    if(v == value) break;
	}
    }
    return map_iter;
}

/* Throws std::invalid_argument if a value for key is not found. */
void Properties::set_list(const std::string& key,
                          const std::string& valueSep) 
{
    if(!isSet(key)) {
        Properties::const_iterator map_iter = begin();
        const std::string tmpKey = key + "[";
        std::string tmpValue = "";
        for(; map_iter != end(); map_iter++) {
            size_t found=(*map_iter).first.find(tmpKey);
            if (found!=std::string::npos) {
                std::string firstStr = (*map_iter).first;
                size_t beginSB = firstStr.find('[');
                size_t endSB = firstStr.find(']');
                if(beginSB != std::string::npos &&
                   endSB != std::string::npos) {
                    std::string listIndex = firstStr.substr(beginSB+1, (endSB - beginSB)-1);
	            Utils::trim(listIndex);
                    if(listIndex.size() > 0) {
                        try {
                            std::size_t index = Utils::getNumber(listIndex);
                            std::string listValue = ((*map_iter).second);
        	            Utils::trim(listValue);
                            if(listValue.size() > 0) {
                                tmpValue += listValue + valueSep;
                            }
                        }
                        catch (...) {
                            // ignore invalid indexes and corresponding values
                        }
                    }
                }
            }
        }
	Utils::trim(tmpValue);
        if(tmpValue.size() > 0) {
	    set(key, tmpValue);
        }
    }
    return;
}

/* Throws std::invalid_argument if a value for key is not found. */
void Properties::set_map(const std::string& key,
                         const std::string& mapSep, 
                         const std::string& valueSep) 
{
    if(!isSet(key)) {
        Properties::const_iterator map_iter = begin();
        const std::string tmpKey = key + "[";
        std::string tmpValue = "";
        for(; map_iter != end(); map_iter++) {
            size_t found=(*map_iter).first.find(tmpKey);
            if (found!=std::string::npos) {
                std::string firstStr = (*map_iter).first;
                size_t beginSB = firstStr.find('[');
                size_t endSB = firstStr.find(']');
                if(beginSB != std::string::npos &&
                   endSB != std::string::npos) {
                    std::string map = firstStr.substr(beginSB+1, (endSB - beginSB)-1);
	            Utils::trim(map);
                    if(map.size() > 0) {
                        tmpValue += map + mapSep + ((*map_iter).second) + valueSep;
                    }
                }
            }
        }
	Utils::trim(tmpValue);
        if(tmpValue.size() > 0) {
	    set(key, tmpValue);
        }
    }
    return;
}
