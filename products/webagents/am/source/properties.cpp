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
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 *
 */

#include <cerrno>
#include <cstdlib>
#include <stdexcept>

#include <prerror.h>
#include <prio.h>
#include <prprf.h>

#include "internal_macros.h"
#include "properties.h"
#include "am_web.h"

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
#if defined(_AMD64_)
    size_t len;
#else
    int len;
#endif
    char ver[10] = {'\0'};

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

            if (key == AM_WEB_AGENTS_VERSION_OLD) {
                strcpy(ver, buffer);
                if (!strncmp(ver, "2.1", 3)) {
                    create_old_to_new_attributes_map();
                }
            }
            
            if (!strncmp(ver, "2.1", 3)) {
                // First resolve backward compatibility issue
                const char *new_property_name = get_new_property_name(key);
                if (new_property_name != NULL) {
                    std::string newkey(new_property_name);
		    set(newkey, buffer);
                } else {
                    set(key,buffer);
                }
	    } else {
		// XXX - Should handle backslash escapes in the value
		set(key, buffer);
	    }
	}

	if (*buffer) {
	    status = AM_FAILURE;
	}
    } catch (const std::bad_alloc&) {
	status = AM_NO_MEMORY;
    }

    return status;
}

am_status_t Properties::load(const std::string& fileName)
{
    am_status_t status = AM_SUCCESS;
    PRFileDesc *propFile;

    PRErrorCode pr_errorCode;

    propFile = PR_Open(fileName.c_str(), PR_RDONLY, 0);
    if (NULL != propFile) {
	PRFileInfo fileInfo;

	if (PR_GetOpenFileInfo(propFile, &fileInfo) == PR_SUCCESS) {
	    try {
		PRInt32 readCount;
		char *buffer = new char[fileInfo.size + 1];

		readCount = PR_Read(propFile, buffer, fileInfo.size);
		if (readCount == fileInfo.size) {
		    buffer[readCount] = '\0';
		    status = parseBuffer(buffer);
		} else {
		    Log::log(Log::ALL_MODULES, 
			     Log::LOG_ERROR, 
			     "am_properties_load(): "
			     "Could not load properties file %s: "
			     "Number of bytes read (%d) "
			     "did not match expected file size (%d).\n", 
			     fileName.c_str(), readCount, fileInfo.size);
		    status = AM_NSPR_ERROR;
		}
               
                delete[] buffer;
	    } catch (const std::bad_alloc&) {
		status = AM_NO_MEMORY;
	    }

	} else {
	    pr_errorCode = PR_GetError();
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_properties_load(): "
		     "Error getting info for properties file %s: %s\n", 
		     fileName.c_str(), 
		     PR_ErrorToString(pr_errorCode, PR_LANGUAGE_I_DEFAULT));
	    status = AM_NSPR_ERROR;
	}
	PR_Close(propFile);
    } else {
	pr_errorCode = PR_GetError();

	if (PR_FILE_NOT_FOUND_ERROR == pr_errorCode) {
	    status = AM_NOT_FOUND;
	} else if (PR_NO_ACCESS_RIGHTS_ERROR == pr_errorCode) {
	    status = AM_ACCESS_DENIED;
	} else {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_properties_load(): "
		     "Error opening properties file '%s': %s\n", 
		     fileName.c_str(), 
		     PR_ErrorToString(pr_errorCode, PR_LANGUAGE_I_DEFAULT));
	    status = AM_NSPR_ERROR;
	}
    }

    return status;
}

am_status_t Properties::store(const std::string& fileName) const
{
    am_status_t status = AM_SUCCESS;
    PRFileDesc *propFile;

    propFile = PR_Open(fileName.c_str(),
		       PR_WRONLY|PR_CREATE_FILE|PR_TRUNCATE, 0644);
    if (NULL != propFile) {
	PRUint32 rc = 0;

	for (const_iterator iter = begin(); iter != end(); ++iter) {
	    rc = PR_fprintf(propFile, "%s=%s\n", iter->first.c_str(),
			    iter->second.c_str());
	    if (static_cast<PRUint32>(-1) == rc) {
		break;
	    }
	}
	if (PR_SUCCESS != PR_Close(propFile) ||
	    static_cast<PRUint32>(-1) == rc) {
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

// Create a hash map of old-to-new properties
void Properties::create_old_to_new_attributes_map()
{
	newAttributesMap = reinterpret_cast<KeyValueMap *>(new KeyValueMap());
	if (newAttributesMap != NULL) {
	    newAttributesMap->insert(AM_COMMON_COOKIE_NAME_PROPERTY_OLD,AM_COMMON_COOKIE_NAME_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_NAMING_URL_PROPERTY_OLD,AM_COMMON_NAMING_URL_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_COOKIE_NAME_PROPERTY_OLD,AM_COMMON_COOKIE_NAME_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_NAMING_URL_PROPERTY_OLD,AM_COMMON_NAMING_URL_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_LOG_LEVELS_PROPERTY_OLD,AM_COMMON_LOG_LEVELS_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_SSL_CERT_DIR_PROPERTY_OLD,AM_COMMON_SSL_CERT_DIR_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_CERT_DB_PREFIX_PROPERTY_OLD,AM_COMMON_CERT_DB_PREFIX_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_TRUST_SERVER_CERTS_PROPERTY_OLD,AM_COMMON_TRUST_SERVER_CERTS_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_NOTIFICATION_ENABLE_PROPERTY_OLD,AM_COMMON_NOTIFICATION_ENABLE_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_NOTIFICATION_URL_PROPERTY_OLD,AM_COMMON_NOTIFICATION_URL_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_LOADBALANCE_PROPERTY_OLD,AM_COMMON_LOADBALANCE_PROPERTY,false);
	    newAttributesMap->insert(AM_POLICY_LOGIN_URL_PROPERTY_OLD,AM_POLICY_LOGIN_URL_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_LOG_FILE_PROPERTY_OLD,AM_COMMON_LOG_FILE_PROPERTY,false);
	    newAttributesMap->insert(AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY_OLD,AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY,false);
	    newAttributesMap->insert(AM_POLICY_HASH_TIMEOUT_MINS_PROPERTY_OLD,AM_POLICY_HASH_TIMEOUT_MINS_PROPERTY,false);
	    newAttributesMap->insert(AM_POLICY_USER_ID_PARAM_PROPERTY_OLD,AM_POLICY_USER_ID_PARAM_PROPERTY,false);
	    newAttributesMap->insert(AM_POLICY_USER_ID_PARAM_TYPE_PROPERTY_OLD,AM_POLICY_USER_ID_PARAM_TYPE_PROPERTY,false);
	    newAttributesMap->insert(AM_COMMON_SERVER_LOG_FILE_PROPERTY_OLD,AM_COMMON_SERVER_LOG_FILE_PROPERTY,false);
	    newAttributesMap->insert(AM_POLICY_PROFILE_ATTRS_MODE_OLD,AM_POLICY_PROFILE_ATTRS_MODE,false);
	    newAttributesMap->insert(AM_POLICY_PROFILE_ATTRS_MAP_OLD,AM_POLICY_PROFILE_ATTRS_MAP,false);
	    newAttributesMap->insert(AM_POLICY_PROFILE_ATTRS_COOKIE_PFX_OLD,AM_POLICY_PROFILE_ATTRS_COOKIE_PFX,false);
	    newAttributesMap->insert(AM_POLICY_PROFILE_ATTRS_COOKIE_MAX_AGE_OLD,AM_POLICY_PROFILE_ATTRS_COOKIE_MAX_AGE,false);
	    newAttributesMap->insert(AM_LOG_ACCESS_TYPE_PROPERTY_OLD,AM_LOG_ACCESS_TYPE_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_URI_PREFIX_OLD,AM_WEB_URI_PREFIX,false);
	    newAttributesMap->insert(AM_WEB_INSTANCE_NAME_PROPERTY_OLD,AM_WEB_INSTANCE_NAME_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_DO_SSO_ONLY_OLD,AM_WEB_DO_SSO_ONLY,false);
	    newAttributesMap->insert(AM_WEB_ACCESS_DENIED_URL_PROPERTY_OLD,AM_WEB_ACCESS_DENIED_URL_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_URL_REDIRECT_PARAM_OLD,AM_WEB_URL_REDIRECT_PARAM,false);
	    newAttributesMap->insert(AM_WEB_FQDN_DEFAULT_OLD,AM_WEB_FQDN_DEFAULT,false);
	    newAttributesMap->insert(AM_WEB_FQDN_MAP_OLD,AM_WEB_FQDN_MAP,false);
	    newAttributesMap->insert(AM_WEB_COOKIE_RESET_ENABLED_OLD,AM_WEB_COOKIE_RESET_ENABLED,false);
	    newAttributesMap->insert(AM_WEB_COOKIE_RESET_LIST_OLD,AM_WEB_COOKIE_RESET_LIST,false);
	    newAttributesMap->insert(AM_WEB_COOKIE_DOMAIN_LIST_OLD,AM_WEB_COOKIE_DOMAIN_LIST,false);
	    newAttributesMap->insert(AM_WEB_ANONYMOUS_USER_OLD,AM_WEB_ANONYMOUS_USER,false);
	    newAttributesMap->insert(AM_WEB_ANON_REMOTE_USER_ENABLE_OLD,AM_WEB_ANON_REMOTE_USER_ENABLE,false);
	    newAttributesMap->insert(AM_WEB_NOT_ENFORCED_LIST_PROPERTY_OLD,AM_WEB_NOT_ENFORCED_LIST_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_REVERSE_NOT_ENFORCED_LIST_OLD,AM_WEB_REVERSE_NOT_ENFORCED_LIST,false);
	    newAttributesMap->insert(AM_WEB_NOT_ENFORCED_IPADDRESS_OLD,AM_WEB_NOT_ENFORCED_IPADDRESS,false);
	    newAttributesMap->insert(AM_WEB_POST_CACHE_DATA_PRESERVE_OLD,AM_WEB_POST_CACHE_DATA_PRESERVE,false);
	    newAttributesMap->insert(AM_WEB_POST_CACHE_ENTRY_LIFETIME_OLD,AM_WEB_POST_CACHE_ENTRY_LIFETIME,false);
	    newAttributesMap->insert(AM_WEB_POST_CACHE_CLEANPUP_INTERVAL_OLD,AM_WEB_POST_CACHE_CLEANPUP_INTERVAL,false);
	    newAttributesMap->insert(AM_WEB_CDSSO_ENABLED_PROPERTY_OLD,AM_WEB_CDSSO_ENABLED_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_CDC_SERVLET_URL_PROPERTY_OLD,AM_WEB_CDC_SERVLET_URL_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_CHECK_CLIENT_IP_PROPERTY_OLD,AM_WEB_CHECK_CLIENT_IP_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_LOGOUT_URL_PROPERTY_OLD,AM_WEB_LOGOUT_URL_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_LOGOUT_COOKIE_RESET_PROPERTY_OLD,AM_WEB_LOGOUT_COOKIE_RESET_PROPERTY,false);
	    newAttributesMap->insert(AM_WEB_GET_CLIENT_HOSTNAME_OLD,AM_WEB_GET_CLIENT_HOSTNAME,false);
	    newAttributesMap->insert(AM_WEB_CONVERT_MBYTE_ENABLE_OLD,AM_WEB_CONVERT_MBYTE_ENABLE,false);
	    newAttributesMap->insert(AM_COMMON_IGNORE_PATH_INFO_OLD,AM_COMMON_IGNORE_PATH_INFO,false);
	    newAttributesMap->insert(AM_WEB_OVERRIDE_HOST_OLD,AM_WEB_OVERRIDE_HOST,false);
	    newAttributesMap->insert(AM_WEB_OVERRIDE_PORT_OLD,AM_WEB_OVERRIDE_PORT,false);
	    newAttributesMap->insert(AM_WEB_OVERRIDE_NOTIFICATION_URL_OLD,AM_WEB_OVERRIDE_NOTIFICATION_URL,false);
	    newAttributesMap->insert(AM_AUTH_ORGANIZATION_NAME_PROPERTY_OLD,AM_AUTH_ORGANIZATION_NAME_PROPERTY,false);
	    newAttributesMap->insert(AM_AUTH_CERT_ALIAS_PROPERTY_OLD,AM_AUTH_CERT_ALIAS_PROPERTY,false);
	    newAttributesMap->insert(AM_AUTH_SERVICE_URLS_PROPERTY_OLD,AM_AUTH_SERVICE_URLS_PROPERTY,false);
	    newAttributesMap->insert(AM_POLICY_HASH_BUCKET_SIZE_PROPERTY_OLD,AM_POLICY_HASH_BUCKET_SIZE_PROPERTY,false);
	    newAttributesMap->insert(AM_POLICY_FETCH_FROM_ROOT_RSRC_PROPERTY_OLD,AM_POLICY_FETCH_FROM_ROOT_RSRC_PROPERTY,false);
	    newAttributesMap->insert(AM_SSO_HASH_BUCKET_SIZE_PROPERTY_OLD,AM_SSO_HASH_BUCKET_SIZE_PROPERTY,false);
	    newAttributesMap->insert(AM_SSO_HASH_TIMEOUT_MINS_PROPERTY_OLD,AM_SSO_HASH_TIMEOUT_MINS_PROPERTY,false);
	    newAttributesMap->insert(AM_SSO_MAX_THREADS_PROPERTY_OLD,AM_SSO_MAX_THREADS_PROPERTY,false);
	    newAttributesMap->insert(AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY_OLD,AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY,false);
	    newAttributesMap->insert(AM_SSO_DEFAULT_SESSION_URL_OLD,AM_SSO_DEFAULT_SESSION_URL,false);
	    newAttributesMap->insert(AM_WEB_DENY_ON_LOG_FAILURE_OLD,AM_WEB_DENY_ON_LOG_FAILURE,false);
	    newAttributesMap->insert(AM_DOMINO_CHECK_NAME_DB_PROPERTY_OLD,AM_DOMINO_CHECK_NAME_DB_PROPERTY,false);
	}
}

// Create a hash map of new-to-old properties
const char* Properties::get_old_property_name(const std::string& key) const
{
        if (key == AM_COMMON_NAMING_URL_PROPERTY)
	   return AM_COMMON_NAMING_URL_PROPERTY_OLD;
        if (key == AM_COMMON_NOTIFICATION_ENABLE_PROPERTY)
	   return AM_COMMON_NOTIFICATION_ENABLE_PROPERTY_OLD;
        if (key == AM_COMMON_NOTIFICATION_URL_PROPERTY)
	    return AM_COMMON_NOTIFICATION_URL_PROPERTY_OLD;
        if (key == AM_COMMON_TRUST_SERVER_CERTS_PROPERTY)
	    return AM_COMMON_TRUST_SERVER_CERTS_PROPERTY_OLD;
        if (key == AM_COMMON_CERT_DB_PASSWORD_PROPERTY)
	   return AM_COMMON_CERT_DB_PASSWORD_PROPERTY_OLD;
        if (key == AM_AUTH_CERT_ALIAS_PROPERTY)
	   return AM_AUTH_CERT_ALIAS_PROPERTY_OLD;
        if (key == AM_COMMON_LOG_LEVELS_PROPERTY)
	   return AM_COMMON_LOG_LEVELS_PROPERTY_OLD;
        if (key == AM_COMMON_LOG_FILE_PROPERTY)
	   return AM_COMMON_LOG_FILE_PROPERTY_OLD;
        if (key == AM_COMMON_SSL_CERT_DIR_PROPERTY)
	    return AM_COMMON_SSL_CERT_DIR_PROPERTY_OLD;
        if (key == AM_COMMON_CERT_DB_PREFIX_PROPERTY)
	    return AM_COMMON_CERT_DB_PREFIX_PROPERTY_OLD;
        if (key == AM_COMMON_COOKIE_NAME_PROPERTY)
	   return AM_COMMON_COOKIE_NAME_PROPERTY_OLD;
        if (key == AM_COMMON_LOADBALANCE_PROPERTY)
	   return AM_COMMON_LOADBALANCE_PROPERTY_OLD;
        if (key == AM_AUTH_SERVICE_URLS_PROPERTY)
	   return AM_AUTH_SERVICE_URLS_PROPERTY_OLD;
        if (key == AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY)
	   return AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY_OLD;
        if (key == AM_SSO_HASH_BUCKET_SIZE_PROPERTY)
	   return AM_SSO_HASH_BUCKET_SIZE_PROPERTY_OLD;
        if (key == AM_SSO_HASH_TIMEOUT_MINS_PROPERTY)
	   return AM_SSO_HASH_TIMEOUT_MINS_PROPERTY_OLD;
        if (key == AM_SSO_MAX_THREADS_PROPERTY)
	   return AM_SSO_MAX_THREADS_PROPERTY_OLD;
        if (key == AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY)
	   return AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY_OLD;
        if (key == AM_SSO_DEFAULT_SESSION_URL)
	   return AM_SSO_DEFAULT_SESSION_URL_OLD;
        if (key == AM_COMMON_LOADBALANCE_PROPERTY)
	   return AM_COMMON_LOADBALANCE_PROPERTY_OLD;
        if (key == AM_COMMON_IGNORE_PATH_INFO)
	   return AM_COMMON_IGNORE_PATH_INFO_OLD;
        if (key == AM_AUTH_ORGANIZATION_NAME_PROPERTY)
	   return AM_AUTH_ORGANIZATION_NAME_PROPERTY_OLD;
        if (key == AM_AUTH_SERVICE_URLS_PROPERTY)
	   return AM_AUTH_SERVICE_URLS_PROPERTY_OLD;
        
        return NULL;
}

const char* Properties::get_new_property_name(const std::string& key) const
{
    if (newAttributesMap != NULL && newAttributesMap->size() > 0) {
       KeyValueMap::const_iterator iter = newAttributesMap->find(key);
       if (iter != newAttributesMap->end() && 
           iter->second.size() > 0) {
            return iter->second[0].c_str();
       }
    }
    return NULL;
}

/* Throws std::invalid_argument if a value for key is not found. */
const std::string& Properties::get(const std::string& key) const
{
    char tmpProp[100] = {'\0'};
    const_iterator iter = find(key);

    if (iter == end()) {
	const char* prop_name = get_old_property_name(key);
	if (prop_name != NULL) {
	    strcpy(tmpProp,prop_name);
	    const std::string old_property_name(tmpProp);	
    	    iter = find(old_property_name);
	    if (iter == end()) {
		throw std::invalid_argument(key + " not found");
	    }
	} else {
	    throw std::invalid_argument(key + " not found");
	}
    }

    return iter->second;
}

const std::string& Properties::get(const std::string& key,
				   const std::string& defaultValue,
				   bool terse) const
{
    char tmpProp[100] = {'\0'};
    const_iterator iter = find(key);

    if (iter == end()) {
	const char* prop_name = get_old_property_name(key);
	if (prop_name != NULL) {
	    strcpy(tmpProp, prop_name);
	    const std::string old_property_name(tmpProp);
    	    iter = find(old_property_name);
	} 
    }

    if (iter == end()) {
        if (terse) {
	    Log::log(logID, Log::LOG_MAX_DEBUG, 
	        "No value specified for key %s, using default value %s.", 
		    key.c_str(), defaultValue.c_str());
        }
	return defaultValue;
    } else {
	try {
	    return iter->second;
        } catch (...) {
            if (terse) {
	        Log::log(logID, Log::LOG_MAX_DEBUG,
	         "Invalid value specified for key %s, using default value %s.", 
		    key.c_str(), defaultValue.c_str());
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
    char tmpProp[100] = {'\0'};
    const_iterator iter = find(key);

    if (iter == end()) {
	const char* prop_name = get_old_property_name(key);
	if (prop_name != NULL) {
	    strcpy(tmpProp,prop_name);
	    const std::string old_property_name(tmpProp);	
    	    iter = find(old_property_name);
	    if (iter == end()) {
		throw std::invalid_argument(key + " not found");
	    }
	} else {
	    throw std::invalid_argument(key + " not found");
	}
    }

    return parseUnsigned(key, iter->second);
}

unsigned long Properties::getUnsigned(const std::string& key,
				      unsigned long defaultValue,
				      bool terse) const
{
    unsigned long result;
    char tmpProp[100] = {'\0'};
    const_iterator iter = find(key);

    if (iter == end()) {
	const char* prop_name = get_old_property_name(key);
	if (prop_name != NULL) {
	    strcpy(tmpProp,prop_name);
	    const std::string old_property_name(tmpProp);
    	    iter = find(old_property_name);
	} 
    }

    if (iter == end()) {
        if (terse) {
	    Log::log(logID, Log::LOG_MAX_DEBUG,
	         "No value specified for key %s, using default value.", 
			key.c_str());
        }
	result = defaultValue;
    } else {
	try {
	   result = parseUnsigned(key, iter->second);
        } catch (...) {
           if (terse) {
	       Log::log(logID, Log::LOG_MAX_DEBUG,
	         "Invalid value specified for key %s, using default value.", 
	               key.c_str());
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
    char tmpProp[100] = {'\0'};
    const_iterator iter = find(key);

    if (iter == end()) {
	const char* prop_name = get_old_property_name(key);
	if (prop_name != NULL) {
	    strcpy(tmpProp,prop_name);
	    const std::string old_property_name(tmpProp);	
    	    iter = find(old_property_name);
	    if (iter == end()) {
		throw std::invalid_argument(key + " not found");
	    }
	} else {
	    throw std::invalid_argument(key + " not found");
	}
    }

    return parseSigned(key, iter->second);
}

long Properties::getSigned(const std::string& key, long defaultValue,
						   bool terse) const
{
    long result;
    char tmpProp[100] = {'\0'};
    const_iterator iter = find(key);

    if (iter == end()) {
	const char* prop_name = get_old_property_name(key);
	if (prop_name != NULL) {
	    strcpy(tmpProp, prop_name);
	    const std::string old_property_name(tmpProp);
    	    iter = find(old_property_name);
	} 
    }

    if (iter == end()) {
        if (terse) {
	    Log::log(logID, Log::LOG_MAX_DEBUG, 
	         "No value specified for key %s, using default value.", 
			key.c_str());
        }
	result =  defaultValue;
    } else {
	try {
	   result = parseSigned(key, iter->second);
        } catch (...) {
           if (terse) {
	       Log::log(logID, Log::LOG_MAX_DEBUG,
	         "Invalid value specified for key %s, using default value.", 
		    key.c_str());
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
    char tmpProp[100] = {'\0'};
    const_iterator iter = find(key);

    if (iter == end()) {
	const char* prop_name = get_old_property_name(key);
	if (prop_name != NULL) {
	    strcpy(tmpProp,prop_name);
	    const std::string old_property_name(tmpProp);	
    	    iter = find(old_property_name);
	    if (iter == end()) {
		throw std::invalid_argument(key + " not found");
	    }
	} else {
	    throw std::invalid_argument(key + " not found");
	}
    }

    return parseBool(key, iter->second);
}

bool Properties::getBool(const std::string& key, bool defaultValue,
						 bool terse) const
{
    bool result;
    char tmpProp[100] = {'\0'};
    const_iterator iter = find(key);

    if (iter == end()) {
	const char* prop_name = get_old_property_name(key);
	if (prop_name != NULL) {
	    strcpy(tmpProp,prop_name);
	    const std::string old_property_name(tmpProp);
    	    iter = find(old_property_name);
	} 
    }

    if (iter == end()) {
        if (terse) {
	    Log::log(logID, Log::LOG_MAX_DEBUG,
	         "No value specified for key %s, using default value.", 
			key.c_str());
        }
	result = defaultValue;
    } else {
	try {
	    result = parseBool(key, iter->second);
        } catch (...) {
            if (terse) {
	        Log::log(logID, Log::LOG_MAX_DEBUG,
		  "Invalid value specified for key %s, using default value.", 
		      key.c_str());
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
