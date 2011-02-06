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
 * $Id: am_web.cpp,v 1.58 2010/03/10 05:09:37 dknab Exp $
 *
 */

/*
 * "Portions Copyrighted [2010] [ForgeRock AS]"
 */

#include <ctype.h>
#include <stdio.h>
#if (!defined(WINNT) && !defined(_AMD64_))
#include <iconv.h>
#include <langinfo.h>
#endif
#include <string.h>
#include <string>
#include <set>
#include <errno.h>

#include <prlock.h>
#include <prnetdb.h>
#include <prmem.h>
#include <prtime.h>
#include <time.h>

#include "http.h"
#include "am_web.h"
#include "am_policy.h"
#include "am_log.h"
#include "key_value_map.h"
#include "p_cache.h"
#include "fqdn_handler.h"
#include "xml_tree.h"
#include "url.h"
#include "utils.h"
#include "agent_profile_service.h"
#include "service.h"

#include "policy_engine.h"
#include "auth_context.h"
#include "am_sso.h"
#include "am_utils.h"

#include <locale.h>

#if	(defined(WINNT) || defined(_AMD64_))
#if(!defined(_AMD64_))
#define _X86_
#endif
#include <windef.h>
#include <winbase.h>
#include <winuser.h>
#include <winnls.h>
#include <windows.h>
#if	!defined(strncasecmp)
#if defined(_AMD64_)
#define stricmp _stricmp
#define strnicmp _strnicmp
#endif
#define	strncasecmp	strnicmp
#define	strcasecmp	stricmp
#endif

#if     !defined(snprintf)
#define snprintf        _snprintf
#endif
#else /* WINNT */
#include <unistd.h>
#endif /* WINNT */

#if	!defined(FALSE)
#define FALSE	0
#endif
#if	!defined(TRUE)
#define TRUE	1
#endif

USING_PRIVATE_NAMESPACE

/*
 * Names of the various advices that we need to process.
 */
#define	AUTH_SCHEME_KEY			"AuthSchemeConditionAdvice"
#define	AUTH_LEVEL_KEY			"AuthLevelConditionAdvice"
#define	AUTH_REALM_KEY			"AuthenticateToRealmConditionAdvice"
#define	AUTH_SERVICE_KEY		"AuthenticateToServiceConditionAdvice"

#define COMPOSITE_ADVICE_KEY "sunamcompositeadvice"
#define SESSION_COND_KEY     "SessionConditionAdvice"

/*
   Liberty Alliance Protocol Related Strings.
 */
#define SAML_PROTOCOL_MAJOR_VERSION 	"1"
#define SAML_PROTOCOL_MINOR_VERSION	"0"
#define DEFAULT_ENCODING			"UTF-8"
#define LIB_PREFIX				"lib:"
#define PROTOCOL_PREFIX				"samlp:"
#define LIB_NAMESPACE_STRING			" xmlns:lib=\"http://projectliberty.org/schemas/core/2002/12\""
#define PROTOCOL_NAMESPACE_STRING		" xmlns:samlp=\"urn:oasis:names:tc:SAML:1.0:protocol\""
#define ELEMENT_ASSERTION			"Assertion"
#define ELEMENT_AUTHN_STATEMENT			"AuthenticationStatement"
#define ELEMENT_SUBJECT				"Subject"
#define ELEMENT_NAME_IDENTIFIER			"IDPProvidedNameIdentifier"

// notification response body.
#define NOTIFICATION_OK		       "OK\r\n"
unsigned long policy_clock_skew = 0;
int postDataPreserveKey = 0;
#define PORT_MIN_VAL 0
#define PORT_MAX_VAL 65536

/**
  * Constants to create the POST submit form
*/
static const char *sector_one   =  "<HTML>\n<BODY onLoad=\"document"
                                   ".postform.submit()\">\n<FORM NAME"
				   "=\"postform\" METHOD=\"POST\" ACTION=\"";
static const char *sector_two   =  "\">\n";
static const char *sector_three =  "<INPUT TYPE=hidden NAME=\"";
static const char *sector_four  =  "\" VALUE=\"";
static const char *sector_five  =  "</FORM>\n</BODY>\n</HTML>\n";

static const char * sunwErrCode = "sunwerrcode";

static int initialized = AM_FALSE;

#define URL_INFO_PTR_NULL		((Utils::url_info_t *) NULL)
#define URL_INFO_LIST_INITIALIZER	{ 0, URL_INFO_PTR_NULL }
#define URL_INFO_INITIALIZER {NULL, 0, NULL, 0, AM_FALSE }

#define COOKIE_INFO_PTR_NULL ((Utils::cookie_info_t *) NULL)
#define COOKIE_INFO_INITIALIZER {NULL, NULL, NULL, NULL, NULL, AM_FALSE}
#define COOKIE_INFO_LIST_INITIALIZER {0, COOKIE_INFO_PTR_NULL}

#define INSTANCE_NAME  "unused"
#define EMPTY_STRING  ""

static const char* requestIp = "requestIp";
static const char* requestDnsName = "requestDnsName";

#if defined(WINNT)
HINSTANCE hInstance;
#endif


extern "C" int decrypt_base64(const char *, char *, const char*);
extern "C" int decode_base64(const char *, char *);


static Utils::boot_info_t boot_info = {
    NULL,           // AGENT PROPERTIES LOCATION
    NULL,           // AGENT Password 
    NULL,           // AGENT name 
    NULL,           // AGENT config file name 
    (unsigned int) -1,      // policy handle
    AM_PROPERTIES_NULL,      // properties
    URL_INFO_LIST_INITIALIZER,	// login_url_list
    1024
};

AgentProfileService* agentProfileService;

/**
 *                    -------- helper functions --------
 */

/* Throws std::exception's from fqdn_handler functions */
inline am_bool_t is_valid_fqdn_access(const char *url, 
                                      void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    if(AM_TRUE == (*agentConfigPtr)->fqdn_check_enable) {
        return ((*agentConfigPtr)->fqdn_handler->isValidFqdnResource(url)?AM_TRUE:AM_FALSE);
    } else {
	return AM_TRUE;
    }
}

void populate_am_resource_traits(am_resource_traits_t &rsrcTraits,
                                 void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    rsrcTraits.cmp_func_ptr = &am_policy_compare_urls;
    rsrcTraits.has_patterns = &am_policy_resource_has_patterns;
    rsrcTraits.get_resource_root = &am_policy_get_url_resource_root;
    rsrcTraits.separator = '/';
    rsrcTraits.ignore_case =
	((*agentConfigPtr)->url_comparison_ignore_case) ? B_TRUE : B_FALSE;
    rsrcTraits.canonicalize = &am_policy_resource_canonicalize;
    rsrcTraits.str_free = &free;
    return;
}

void encode_url( const char *orig_url, char *dest_url)
{
    int i, ucnt;
    char p_enc = '%';
    char buffer[4];
    for(i=0; i < strlen(orig_url); i++) {
	ucnt = orig_url[i];
	if (( ucnt >  32) && ( ucnt < 127))  {
	   strncat(dest_url, &orig_url[i], 1);
	} else {
	   if ( ucnt < 0 ) ucnt += 256;
	      if (ucnt < 32)
		 sprintf(buffer, "%c0%X", p_enc, ucnt);
	      else
		 sprintf(buffer, "%c%X", p_enc, ucnt);
	}
    }
    strncat(dest_url, buffer, strlen(buffer));
}

void getFullQualifiedHostName(const am_map_t env_parameter_map,
			      PRNetAddr *address,
			      PRHostEnt *hostEntry)
{
    char* hostName;
    int i = 0;
    PRUint16 port = 0;
    char* alias;
    PRIntn hostIndex = PR_EnumerateHostEnt(0, hostEntry, port, address);

    if (hostIndex >= 0) {
	hostName = hostEntry->h_name;

	if (hostName) {
	    Log::log(boot_info.log_module, Log::LOG_DEBUG,
		"getFullQualifiedHostName: map_insert: "
		"hostname=%s", hostName);
	    am_map_insert(env_parameter_map, 
                requestDnsName,
		hostName, 
                AM_FALSE);
	}

	alias = hostEntry->h_aliases[i++];
	while (alias) {
	    Log::log(boot_info.log_module, Log::LOG_DEBUG,
		"getFullQualifiedHostName: map_insert: "
		"alias=%s", alias);
	    am_map_insert(env_parameter_map, 
                requestDnsName,
		alias, 
                AM_FALSE);
	    alias = hostEntry->h_aliases[i++];
	}
    }
}

#if (defined(WINNT) || defined(_AMD64_))
#include "resource.h"
void get_string(UINT key, char *buf, size_t buflen) {

    if (buf != NULL) {
	if (LoadString(hInstance, key, buf, buflen) == 0) {
	    buf[0] = '\0';
	    return;
	}
    }
    return;
}
/* On windows default strtok_r to strtok */
#define strtok_r(s1, s2, p) strtok(s1, s2);
#elif defined(HPUX) || defined(AIX)
void get_string(const char *key, char *buf, size_t buflen) {
  strncpy(buf, key, buflen);
  return;
}
#else
#include <libintl.h>
#define domainname "am_web_keys"
void get_string(const char *key, char *buf, size_t buflen) {
    if(buf != NULL) {
	strcpy(buf, (const char *)gettext(key));
    }
    return;
}
#endif

#if (defined(WINNT) || defined(_AMD64_))
void mbyte_to_wchar(const char * orig_str,char *new_str, int dest_len)
{
    int orgStrLen = strlen (orig_str);

    WCHAR * wszNew_Value = (WCHAR *) malloc ((orgStrLen+2)*sizeof (WCHAR) );
    if (wszNew_Value != NULL ){
	int uniSize = orgStrLen+2;
	/* Initialize this array to zero so that we can treat as NULL
	 * terminated String of wide char and use it in WideCharToMultiByte
         */
	for (int i=0; i<uniSize; i++) {
	    wszNew_Value[i]=0;
	}
	(void)MultiByteToWideChar(CP_UTF8,0,orig_str,-1,wszNew_Value,uniSize);
    }
    (void)WideCharToMultiByte(CP_ACP, 0, wszNew_Value, -1, new_str, dest_len,
			      NULL, NULL);
    free(wszNew_Value);
}
#else
void mbyte_to_wchar(const char * orig_str,char *dest_str,int dest_len)
{
#if defined(LINUX) || defined(HPUX) || defined(AIX)
    char *origstr = const_cast<char *>(orig_str);
#else
    const char *origstr = orig_str;
#endif
#if defined(HPUX) || defined(AIX) || defined(SOLARIS_64)
    unsigned long len = strlen(origstr);
    unsigned long size=0 ;
#else
    unsigned int len = strlen(origstr);
    unsigned int size=0 ;
#endif

    size = dest_len;
    memset(dest_str, 0, dest_len);
    char * native_encoding = nl_langinfo(CODESET);
    Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
	"i18n using native encoding %s.", native_encoding);
    iconv_t encoder = iconv_open(native_encoding,  "UTF-8" );
    if (encoder == (iconv_t)-1) {
	 /*
	  * iconv_open failed
	  */
	 Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
		  "iconv_open failed");
	 strcpy(dest_str, origstr);
     } else {
	/* Perform iconv conversion */
	Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
		 "i18n b4 convlen = %d  size = %d", len, size);
#if defined(LINUX_64) || defined(LINUX)
	int ret = iconv(encoder, &origstr, (size_t*)&len, &dest_str, (size_t*)&size);
#else
	int ret = iconv(encoder, &origstr, &len, &dest_str, &size);
#endif
	Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
		 "i18n len = %d  size = %d", len, size);
	if (ret < 0) {
	    Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
		     "iconv conversion failed" );
	    strcpy(dest_str, origstr);
	}
	iconv_close(encoder);
    }
}
#endif

/**
 * NOTE: this function may be called more than once so don't add
 * any code in here that could cause problems if called twice.
 */

static am_bool_t is_server_alive(const Utils::url_info_t *info_ptr, 
                                 void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    am_bool_t status = AM_FALSE;
    char	buffer[PR_NETDB_BUF_SIZE];
    PRNetAddr	address;
    PRHostEnt	hostEntry;
    PRIntn	hostIndex;
    PRStatus	prStatus;
    PRFileDesc *tcpSocket;
    unsigned timeout = 0;
    
    prStatus = PR_GetHostByName(info_ptr->host, buffer, sizeof(buffer),
				&hostEntry);
    if (PR_SUCCESS == prStatus) {
	hostIndex = PR_EnumerateHostEnt(0, &hostEntry, info_ptr->port,
					&address);
	if (hostIndex >= 0) {
		timeout = (unsigned) ((*agentConfigPtr)->connection_timeout);

		if (((PRFileDesc *) NULL) != tcpSocket) {
		Log::log(boot_info.log_module, Log::LOG_DEBUG,
			"is_server_alive(): Connection timeout set to %i", timeout);
	    }
		tcpSocket = PR_NewTCPSocket();
	    if (((PRFileDesc *) NULL) != tcpSocket) {
		prStatus = PR_Connect(tcpSocket, &address,
				      PR_SecondsToInterval(timeout));
		if (PR_SUCCESS == prStatus) {
		    status = AM_TRUE;
		}
	    	PR_Shutdown(tcpSocket, PR_SHUTDOWN_BOTH);
	    	prStatus = PR_Close(tcpSocket);
	    	if (prStatus != PR_SUCCESS) {
		    PRErrorCode error = PR_GetError();
		    Log::log(boot_info.log_module, Log::LOG_ERROR,
			     "is_server_alive(): NSPR Error while calling "
			     "PR_Close(): %d.", error);
	        }
	    }
	}
    }

    return status;
}

static Utils::url_info_t *find_active_login_server(void* agent_config) 
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    Utils::url_info_t *result = URL_INFO_PTR_NULL;
    unsigned int i = 0;
    Utils::url_info_list_t *url_list = NULL;

    if(initialized == AM_TRUE) {
	PR_Lock((*agentConfigPtr)->lock);

	if((*agentConfigPtr)->cdsso_enable) {
	    url_list = &(*agentConfigPtr)->cdsso_server_url_list;
	} else {
	    url_list = &(*agentConfigPtr)->login_url_list;
	}

	if ((*agentConfigPtr)->ignore_server_check == AM_FALSE) {
	    for (i = 0; i < url_list->size; ++i) {
		    am_web_log_max_debug("find_active_login_server(): "
		    "Trying server: %s", url_list->list[i].url);
		    if (is_server_alive(&url_list->list[i], agent_config)) {
			    result = &url_list->list[i];
			    break;
		    }
	    }
	} else {
	    result = &url_list->list[i];
	}

	PR_Unlock((*agentConfigPtr)->lock);
    } else {
	am_web_log_error("find_active_login_server(): "
			 "Library not initialized.");
    }

    return result;
}


static Utils::url_info_t *find_active_logout_server(void* agent_config) 
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    Utils::url_info_t *result = URL_INFO_PTR_NULL;
    unsigned int i = 0;
    Utils::url_info_list_t *url_list = NULL;

    if(initialized == AM_TRUE) {
	PR_Lock((*agentConfigPtr)->lock);

    url_list = &(*agentConfigPtr)->logout_url_list;

	if ((*agentConfigPtr)->ignore_server_check == AM_FALSE) {
	    for (i = 0; i < url_list->size; ++i) {
		    am_web_log_max_debug("find_active_logout_server(): "
		    "Trying server: %s", url_list->list[i].url);
		    if (is_server_alive(&url_list->list[i], agent_config)) {
			    result = &url_list->list[i];
			    break;
		    }
	    }
	} else {
	    result = &url_list->list[i];
	}

	PR_Unlock((*agentConfigPtr)->lock);
    } else {
	am_web_log_error("find_active_logout_server(): "
			 "Library not initialized.");
    }

    return result;
}

/** 
 * Loads bootstrap file into boot_info structure.
 * Throws std::exception's from url parsing routines. 
 */
static am_status_t
load_bootstrap_properties(Utils::boot_info_t *boot_ptr, 
                          const char *boot_file, 
                          const char *config_file, 
                          boolean_t initializeLog)
{
//    const char *thisfunc = "load_bootstrap_properties()";
    am_status_t status, keyStatus;
    const char *function_name = "am_properties_create";
    const char *parameter = "";
    const char *encrypt_passwd = NULL;
    char decrypt_passwd[100] = "";
    const char *decrypt_key = NULL;
    int decrypt_status;

    status = am_properties_create(&boot_ptr->properties);
    if (AM_SUCCESS == status) {
        function_name = "am_properties_load";
        parameter = boot_file;
        status = am_properties_load(boot_ptr->properties, boot_file);
    }
    // Set the log file pointer early enough
    if (initializeLog) {

        if (AM_SUCCESS == status) {
	// this will set log file and default levels from the properties.
	    status = am_log_init(boot_ptr->properties);
        }

        // Add agent log module, level 
        if (AM_SUCCESS == status) {
	    //boot_info.log_module = AM_LOG_ALL_MODULES;
	    boot_ptr->log_module = AM_LOG_ALL_MODULES;
        }
    }

    if (AM_SUCCESS == status) {
        parameter = AM_POLICY_PASSWORD_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &encrypt_passwd);
        keyStatus = am_properties_get(boot_ptr->properties, 
                AM_POLICY_KEY_PROPERTY, &decrypt_key);
        if (AM_SUCCESS == status && AM_SUCCESS == keyStatus) {
            if(encrypt_passwd != NULL && decrypt_key != NULL){
                decrypt_status = decrypt_base64(encrypt_passwd, decrypt_passwd, 
                        decrypt_key);
                if(decrypt_status == 0){
                    am_properties_set(boot_ptr->properties, parameter,
                                      decrypt_passwd);
		    status = am_properties_get(boot_ptr->properties, parameter,
                                   &boot_ptr->agent_passwd);
                }else {
                    status=static_cast<am_status_t>(decrypt_status);
                }
            }else {
                status=AM_FAILURE;
            }
        }
    }

    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_POLICY_USER_NAME_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
            &boot_ptr->agent_name);
    }

    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_AGENT_PROFILE_NAME_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
            &boot_ptr->shared_agent_profile_name);
    }

    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_POLICY_ORG_NAME_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
            &boot_ptr->realm_name);
    }

    // Get the naming URL.
    if (AM_SUCCESS == status) {
	const char *property_str;
	parameter = AM_COMMON_NAMING_URL_PROPERTY;
	status = am_properties_get(boot_ptr->properties, parameter,
				   &property_str);
	if (AM_SUCCESS == status) {
	    status = Utils::parse_url_list(property_str, ' ',
				    &boot_ptr->naming_url_list, AM_TRUE);
	}
    }
    boot_ptr->agent_config_file = strdup(config_file);

    return status;
}

#if defined(WINNT)
extern "C" AM_WEB_EXPORT DWORD
am_web_get_iis_filter_priority(void* agent_config) {

    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    DWORD priorityFlag =0; 
    const char *amAgentPriorityflag = (*agentConfigPtr)->filter_priority;
    if(strncasecmp(amAgentPriorityflag ,"DEFAULT",7) == 0)
        priorityFlag = 0x00020000;         
    else if(strncasecmp(amAgentPriorityflag ,"MEDIUM",6) == 0)
        priorityFlag = 0x00040000;
    else if(strncasecmp(amAgentPriorityflag,"HIGH",4) == 0)
        priorityFlag=0x00080000;
    else if(strncasecmp(amAgentPriorityflag,"LOW",3) == 0)
        priorityFlag=0x00020000;

    return priorityFlag;
}
#endif

extern "C" AM_WEB_EXPORT void
am_web_free_memory(void *data)
{
    free(data);
}

extern "C" AM_WEB_EXPORT char *
am_web_http_decode(const char *source, size_t len)
{
    char *result = NULL;

    if (source != NULL) {
	result = (char *)malloc(len + 1);
	if (result != NULL) {
	    char *ptr = result;
	    size_t i;

	    for (i = 0; i < len; ++i) {
		char curChar = source[i];

		if (curChar == '%') {
		    if (i + 2 < len && isxdigit(source[i + 1]) &&
			isxdigit(source[i + 2])) {
			unsigned int value;
			char data = source[++i];

			if (isdigit(data)) {
			    value = data - '0';
			} else if (isupper(data)) {
			    value = data - 'A' + 0xA;
			} else {
			    value = data - 'a' + 0xa;
			}

			data = source[++i];
			value *= 0x10;

			if (isdigit(data)) {
			    value += data - '0';
			} else if (isupper(data)) {
			    value += data - 'A' + 0xA;
			} else {
			    value += data - 'a' + 0xa;
			}
			*(ptr++) = (char) value;
		    } else {
			free(result);
			result = NULL;
			break;
		    }
		} else {
		  if(curChar == '+')
                           curChar = ' ';
		    *(ptr++) = curChar;
		}
	    }
	    if (result) {
		*ptr = '\0';
	    }
	}
    } else {
	result = NULL;
    }

    return result;
}


/**
 * Initializes agent during first request. 
 */
extern "C" AM_WEB_EXPORT am_status_t
am_agent_init(boolean_t* pAgentInitialized)
{
    const char *thisfunc = "am_agent_init";
    am_status_t status = AM_SUCCESS;
//    const Properties *properties = NULL;
    //change the below member to boolean
    int agentAuthenticated = AM_FALSE;
    SSOToken ssoToken;
    AgentConfigurationRefCntPtr* agentConfigPtr;
    void* agent_config;
    string userName(boot_info.agent_name);
    string passwd(boot_info.agent_passwd);
    const Properties& propPtr =
        *reinterpret_cast<Properties *>(boot_info.properties);
//    const char * agentConfigFile = boot_info.agent_config_file;
    // call this to ensure NSS is initialised in the child process
    status = Connection::initialize(propPtr);
    
    if (AM_SUCCESS == status) {
        if (agentProfileService == NULL) {
            agentProfileService = new AgentProfileService(propPtr, boot_info);

        }
        if (agentProfileService != NULL) {
            status = agentProfileService->agentLogin();
        }
    }
    
    if (AM_SUCCESS == status) {
        agentAuthenticated = AM_TRUE;
        agentProfileService->fetchAndUpdateAgentConfigCache();
        agent_config = am_web_get_agent_configuration();
        agentConfigPtr = 
             (AgentConfigurationRefCntPtr*) agent_config;
        if ((*agentConfigPtr) == NULL) {
            status = AM_FAILURE;
        }
    }

    if (AM_SUCCESS == status) {

        am_resource_traits_t rsrcTraits;
        #if !defined(WINNT) && !defined(HPUX) && !defined(AIX)
        /* Logging locale is set as per the value defined in the properties file
        However we need to set the rest of locale behaviour so that code
        conversion routines will work correctly. Setting LC_MESSAGE to a
        different value  can be useful to run the agent in non English locale
        and dump the log in English  */
        if((*agentConfigPtr)->locale != NULL) {
            setlocale(LC_MESSAGES,(*agentConfigPtr)->locale);
            setlocale(LC_CTYPE,"");
            setlocale(LC_NUMERIC,"");
            setlocale(LC_TIME,"");
            setlocale(LC_MONETARY,"");
            am_web_log_info("%s: Logging Locale: %s",
                            thisfunc, (*agentConfigPtr)->locale);
        } else {
            setlocale(LC_ALL,"");
            am_web_log_info("am_agent_init(): Logging Locale: OS locale");
        }
	textdomain(domainname);
	#endif
	populate_am_resource_traits(rsrcTraits, agent_config);

	status = am_policy_init((*agentConfigPtr)->properties);
	if (AM_SUCCESS == status) {
	    /* initialize policy */
	    status = am_policy_service_init("iPlanetAMWebAgentService",
						INSTANCE_NAME,
						rsrcTraits,
						(*agentConfigPtr)->properties,
						&boot_info.policy_handle);
	    if (AM_SUCCESS == status) {
		initialized = AM_TRUE;
	    } else {
		am_web_log_error("%s unable to "
				    "initialize the agent's policy object",
				    thisfunc);
	    }
	} else {
	    am_web_log_error("%s unable to initialize "
				"the AM SDK, status = %s (%d)",
				thisfunc,
				am_status_to_string(status), status);
	}

	status = am_log_add_module("RemoteLog", &(*agentConfigPtr)->remote_LogID);
	if (AM_SUCCESS != status) {
	    (*agentConfigPtr)->remote_LogID = AM_LOG_ALL_MODULES;
	    status = AM_SUCCESS;
	}

    }

    if (AM_SUCCESS == status) {
        *pAgentInitialized = B_TRUE;
    } else {
        if(agentAuthenticated == AM_TRUE) {
            agentProfileService->agentLogout(propPtr); 
        }
    }
    return status;
}

/**
 * Agent plugin init function calls to load bootstrap properties fiile.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_init(const char *agent_bootstrap_file,
                const char *agent_config_file)
{
    const char *thisfunc = "am_web_init";
    am_status_t status = AM_SUCCESS;
//    am_status_t authStatus = AM_FAILURE;
//    const Properties *properties = NULL;
//    am_properties_t tempprop ;

    if (! initialized) {
	// initialize log here so any error before properties file is
	// loaded will go to stderr. After it's loaded will go to log file.
	status = Log::initialize();
	if (AM_SUCCESS == status) {
	    try {
		status = load_bootstrap_properties(&boot_info, 
                                                   agent_bootstrap_file, 
                                                   agent_config_file, 
                                                   B_TRUE);
            } catch(InternalException& ex) {
                am_web_log_error("%s: Exception encountered while loading "
                    "agent bootstrap properties: %s: %s",
                    thisfunc, ex.what(), ex.getMessage());
                status = ex.getStatusCode();
            } catch(std::bad_alloc& exb) {
		status = AM_NO_MEMORY;
            } catch(std::exception& exs) {
                am_web_log_error("%s: Exception encountered while loading "
                     "agent bootstrap properties: %s",
                thisfunc, exs.what());
                status = AM_FAILURE;
            } catch(...) {
                am_web_log_error("%s: Unknown exception encountered "
                    "while loading bootstrap properties.", thisfunc);
                status = AM_FAILURE;
            }
        }
    }
    return status;
}

/**
 * Performs cleanup.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_cleanup()
{
    const char *thisfunc = "am_web_cleanup()";
    am_status_t status = AM_FAILURE;

    if (initialized) {
	am_web_log_debug("%s: cleanup sequence initiated.", thisfunc);

	    const Properties& propPtr = 
            *reinterpret_cast<Properties *>(boot_info.properties);
	    agentProfileService->agentLogout(propPtr); 
	    am_web_log_debug("%s: Agent logout done.", thisfunc);

	status = am_cleanup();

	initialized = AM_FALSE;
    } else {
	status = AM_SUCCESS;
    }
    return status;
}

// -- supporting methods for am_web_is_access_allowed

/* Throws std::exceptions's from URL methods */
am_bool_t in_not_enforced_list(URL &urlObj,
                               void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "in_not_enforced_list";
    std::string baseURL_str;
    const char *baseURL;
    std::string dummyNotEnforcedUrl_str;
    std::string url_str;
    const char *url = NULL;
    am_bool_t found = AM_FALSE;
    size_t i = 0;
    am_resource_traits_t rsrcTraits;
    populate_am_resource_traits(rsrcTraits, agent_config);

    am_bool_t access_denied_url_match_flag = AM_FALSE;
    am_bool_t dummy_post_url_match_flag = AM_FALSE;

    urlObj.getURLString(url_str);
    url = url_str.c_str();
    urlObj.getBaseURL(baseURL_str);
    baseURL = baseURL_str.c_str();
    
    /* First check is the access denied URL */
    if ((*agentConfigPtr)->access_denied_url != NULL) {
        /* We append the sunwerrorcode as a querystring to the 
	 * access denied url if the policy denies access to the resource. 
	 * This query parameter needs to be removed from the url if 
	 * present before comparison.
	 */
        std::string urlStr;
        const char* accessUrl = url;
        std::string accessDeniedUrlStr;
        const char* access_denied_url = NULL;

        URL accessDeniedUrlObj((*agentConfigPtr)->access_denied_url);
        accessDeniedUrlObj.getURLString(accessDeniedUrlStr);
        access_denied_url = accessDeniedUrlStr.c_str();

        if (urlObj.findQueryParameter(sunwErrCode)) {
            urlObj.removeQueryParameter(sunwErrCode);
            urlObj.getURLString(url_str);
            if (urlObj.findQueryParameter((*agentConfigPtr)->url_redirect_param)) {
             urlObj.removeQueryParameter((*agentConfigPtr)->url_redirect_param);
            }
            urlObj.getURLString(urlStr);
            accessUrl = urlStr.c_str();
        }

        am_resource_match_t access_denied_url_match;
        access_denied_url_match = am_policy_compare_urls(&rsrcTraits,
					access_denied_url, accessUrl, B_FALSE);
        if (AM_EXACT_MATCH == access_denied_url_match) {
            access_denied_url_match_flag = AM_TRUE;
            am_web_log_debug("%s: Matching %s with access_denied_url %s: TRUE",
                     thisfunc, accessUrl, (*agentConfigPtr)->access_denied_url);
        } else {
            am_web_log_debug("%s: Matching %s with access_denied_url %s: FALSE",
                     thisfunc,accessUrl, (*agentConfigPtr)->access_denied_url);
	}
    }

    // Check for dummy post url
    urlObj.getRootURL(dummyNotEnforcedUrl_str);
    dummyNotEnforcedUrl_str.append(DUMMY_NOTENFORCED);
        am_resource_match_t dummy_post_url_match;
        dummy_post_url_match = am_policy_compare_urls(&rsrcTraits,
                              dummyNotEnforcedUrl_str.c_str(),
                                        baseURL, B_TRUE);
        if ( (AM_EXACT_MATCH == dummy_post_url_match)  ||
	     ( AM_EXACT_PATTERN_MATCH == dummy_post_url_match )) {
            dummy_post_url_match_flag = AM_TRUE;
        }

    if (access_denied_url_match_flag == AM_TRUE) {
        am_web_log_debug("%s: The requested URL %s "
		 "is the access denied URL which is always not enforced.",
		 thisfunc, url);
        found = AM_TRUE;
    } else if (dummy_post_url_match_flag == AM_TRUE) {
        am_web_log_debug("%s: The requested URL %s, base URL %s "
		 "is the dummy post URL which is always not enforced.",
		  thisfunc, url, baseURL);
        found = AM_TRUE;
    } else {
        for (i = 0;
	    (i < (*agentConfigPtr)->not_enforced_list.size) 
	     && (AM_FALSE == found);
	     i++) {
	    am_resource_match_t match_status;

	    if ((*agentConfigPtr)->not_enforced_list.list[i].has_patterns
		 == AM_TRUE) {
            if (AM_TRUE ==
                 (*agentConfigPtr)->ignore_path_info_for_not_enforced_list)
            {
	        match_status = am_policy_compare_urls(
                    &rsrcTraits, 
                    (*agentConfigPtr)->not_enforced_list.list[i].url,
	            baseURL, B_TRUE);
	    } else {
	        match_status = am_policy_compare_urls(
                &rsrcTraits, (*agentConfigPtr)->not_enforced_list.list[i].url,
                            url, B_TRUE);
                }
            } else {
                match_status = am_policy_compare_urls(
                    &rsrcTraits, (*agentConfigPtr)->not_enforced_list.list[i].url,
                url, B_FALSE);
	    }

	    if (AM_EXACT_MATCH == match_status ||
	        AM_EXACT_PATTERN_MATCH == match_status) {
            am_web_log_debug("%s(%s): "
			 "matched '%s' entry in not-enforced list", thisfunc,
			 url, (*agentConfigPtr)->not_enforced_list.list[i].url);
	        found = AM_TRUE;
	    }
        }

        if ((*agentConfigPtr)->reverse_the_meaning_of_not_enforced_list 
	     == AM_TRUE) {
            am_web_log_debug("%s: not enforced list is reversed, "
		     "only matches will be enforced.", thisfunc);
            if (found == AM_TRUE) {
                found = AM_FALSE;
            } else {
                found = AM_TRUE;
            }
        }

        if (AM_TRUE == found) {
            am_web_log_debug("%s: Allowing access to %s ",
                              thisfunc, url);
        } else {
            am_web_log_debug("%s: Enforcing access control for %s ",
                             thisfunc, url);
        }
    }
    return found;
}

void set_host_ip_in_env_map(const char *client_ip,
			    const am_map_t env_parameter_map,
                            void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    PRStatus prStatus;
    PRNetAddr address;
    PRHostEnt hostEntry;
    char buffer[PR_NETDB_BUF_SIZE];

    Log::log(boot_info.log_module, Log::LOG_DEBUG,
	     "set_host_ip_in_env_map: map_insert: "
	     "client_ip=%s", client_ip);
    am_map_insert(env_parameter_map, requestIp, client_ip, AM_TRUE);

    if ((*agentConfigPtr)->getClientHostname) {
	prStatus = PR_StringToNetAddr(client_ip, &address);
	if (PR_SUCCESS == prStatus) {
	    prStatus = PR_GetHostByAddr(
		&address, buffer, sizeof(buffer), &hostEntry);

	    if (PR_SUCCESS == prStatus) {
		// this function will log info about the client's hostnames
		// so no need to do it here.
		getFullQualifiedHostName(
		    env_parameter_map, &address, &hostEntry);
	    }
	}
	else {
	    am_web_log_warning("set_host_ip_in_env_map: map_insert: "
			       "could not get client's hostname for policy. "
			       "Error %s.",
			       PR_ErrorToString(PR_GetError(),
					        PR_LANGUAGE_I_DEFAULT));
	}
    }
}

/*
 * Throws std::exception's from key value map methods.
 */
am_status_t eval_action_results_map(const KeyValueMap &action_results_map,
				    const char *url,
				    const char *action_name,
				    am_policy_result_t *result)
{
    am_status_t status = AM_ACCESS_DENIED;
    KeyValueMap::const_iterator iter = action_results_map.find(action_name);
    if(iter != action_results_map.end()) {
	const KeyValueMap::mapped_type& valueRef = iter->second;
	for(KeyValueMap::mapped_type::const_iterator v_iter= valueRef.begin();
	    v_iter != valueRef.end(); v_iter++) {
	    const char *value = (*v_iter).c_str();
	    if(strcmp(value, AM_WEB_ACTION_ALLOWED) == 0) {
		status = AM_SUCCESS;
	    } else {
		if(strcmp(value, AM_WEB_ACTION_DENIED) == 0) {
		    if (result->attr_profile_map != AM_MAP_NULL) {
			am_map_destroy(result->attr_profile_map);
			result->attr_profile_map = AM_MAP_NULL;
		    }
		    if (result->attr_session_map != AM_MAP_NULL) {
			am_map_destroy(result->attr_session_map);
			result->attr_session_map = AM_MAP_NULL;
		    }
		    if (result->attr_response_map != AM_MAP_NULL) {
			am_map_destroy(result->attr_response_map);
			result->attr_response_map = AM_MAP_NULL;
		    }
		} else {
		    am_web_log_error("eval_action_results_map("
				     "%s, %s) denying access: "
				     "unknown value '%s'", url,
				     action_name, value);
		}
		status = AM_ACCESS_DENIED;
		break;
	    }
	}
    } else {
	Log::log(boot_info.log_module, Log::LOG_DEBUG,
		 "eval_action_results_map(%s, %s) denying "
		 "access: no action information found",
		 url, action_name);
    }

    if(status == AM_SUCCESS) {
	am_map_destroy(result->advice_map);
	result->advice_map = AM_MAP_NULL;
    }

    return status;
}

extern "C" AM_WEB_EXPORT am_status_t
am_web_get_token_from_assertion(char * enc_assertion, 
                                char **token,
                                void* agent_config )
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "am_web_get_token_from_assertion()";
    am_status_t status = AM_FAILURE;
    char * str = NULL;
    char *tmp1 = NULL;
    char *tmp2 = NULL;
    char * dec_assertion = NULL;
    char buf[1024] = {'\0'};

    dec_assertion = am_web_http_decode(enc_assertion, strlen(enc_assertion));
    // Add null check to avoid crashes
    // - forward port fix in CRT (604)
    if(dec_assertion != NULL) {
        tmp1 = strchr(dec_assertion, '=');
    }

    if ((tmp1 != NULL)  &&
      !(strncmp(dec_assertion, LARES_PARAM, strlen(LARES_PARAM)))) {
        tmp2 = tmp1 + 1;
        decode_base64(tmp2, tmp1);
	am_web_log_debug("Received Authn Response = %s", tmp1);
	if (*tmp1 == NULL) {
	    am_web_log_error("Improper LARES param received");
	    status = AM_FAILURE;
	    return status;
	}
        str = tmp1;

        try {
	    std::string name;
            XMLTree tree(false, str, strlen(str));
            XMLElement rootElement = tree.getRootElement();
	    rootElement.getName(name);
	    am_web_log_debug("Root Element name=%s", name.c_str());
    	    std::string attrVal;
    	    std::string elemValue;
    	    XMLElement subElem;
	    if(rootElement.getSubElement(ELEMENT_ASSERTION, subElem)) {
		    am_web_log_debug("saml:Assertion found");
		if(subElem.getSubElement(ELEMENT_AUTHN_STATEMENT, subElem)) {
			am_web_log_debug("saml:AuthenticationStatement found");
		    if(subElem.getSubElement(ELEMENT_SUBJECT, subElem)) {
			    am_web_log_debug("saml:Subject found");
			if(subElem.getSubElement(
				    ELEMENT_NAME_IDENTIFIER, subElem)) {
				am_web_log_debug(
					"lib:IDPProvidedNameIdentifier found");
			    if(subElem.getValue(elemValue)) {
				    am_web_log_debug("Value found(elemVal)=%s",
						     elemValue.c_str());
                                    am_http_cookie_decode(elemValue.c_str(),
                                                          buf, 1024);
				    *token = strdup(buf);
				    if(*token == NULL) {
					status = AM_NO_MEMORY;
				    }
			    }else {
				    am_web_log_debug("Element value for "
					"IDPProvidedNameIdentifier not found");
			    }
			} else {
				am_web_log_debug("Element "
				    "lib:IDPProvidedNameIdentifier not found");
			}
		    } else {
			    am_web_log_debug("Element saml:Subject not found");
		    }
		} else {
			am_web_log_debug(
				"Element AuthenticationStatement not found");
		}
	    } else {
		    am_web_log_debug("Element saml:Assertion not found");
	    }
	} catch(XMLTree::ParseException &ex) {
	    am_web_log_error("Could not find %s cookie in the Liberty "
		             "AuthnResponse", (*agentConfigPtr)->cookie_name);
	    status = AM_NOT_FOUND;
        }
	catch (std::bad_alloc& exb) {
	    status = AM_NO_MEMORY;
	}
	catch (std::exception& exs) {
	    am_web_log_error("%s: exception caught: %s", thisfunc, exs.what());
	    status = AM_FAILURE;
	}
	catch (...) {
	    am_web_log_error("%s: unknown exception caught.", thisfunc);
	    status = AM_FAILURE;
	}
	if((*token != NULL) && (strlen(*token) > 0)) {
	    status = AM_SUCCESS;
	    am_web_log_debug("Token value found: \"%s\"", *token);
	}
    }
    if(dec_assertion!=NULL)
    {
        free(dec_assertion);
        dec_assertion = NULL;
	}
    return status;
}

/*
 * Creates a Set-Cookie HTTP Response header from cookie_info_t structure
 * The string returned should be freed when not needed anymore.
 *
 * Throws: std::exception's from string methods.
 */
static const char *
buildSetCookieHeader(Utils::cookie_info_t *cookie)
{
    char *reset_header = NULL;
    if (cookie != NULL && cookie->name != NULL) {
        char *name = cookie->name;
        char *value = cookie->value;
        char *domain = cookie->domain;
        char *max_age = cookie->max_age;
        char *path = cookie->path;
        std::string resetCookieVal;

        PUSH_BACK_CHAR(resetCookieVal, ' ');
        resetCookieVal.append(name);
        PUSH_BACK_CHAR(resetCookieVal, '=');
        if (NULL != value) {
            resetCookieVal.append(value);
        }
        if (NULL != domain && '\0' != domain[0]) {
            // if domain is an empty string it's better to not
            // set domain at all since netscape 4.79, IE 5.5
            // and mozilla < 1.4 ignores those cookie entries.
            resetCookieVal.append(";Domain=");
            resetCookieVal.append(domain);
        }
        if (NULL != max_age && '\0' != max_age[0]) {
            resetCookieVal.append(";Max-Age=");
            resetCookieVal.append(max_age);
        }
        if (NULL != path && '\0' != path[0]) {
            resetCookieVal.append(";Path=");
            resetCookieVal.append(path);
        }
        if (AM_TRUE == cookie->isSecure) {
            resetCookieVal.append(";Secure");            
        } 
        reset_header = strdup(resetCookieVal.c_str());

    } else  {
	am_web_log_error("buildSetCookieHeader() : Invalid Cookie");
    }
    return reset_header;
}


extern "C" AM_WEB_EXPORT am_status_t
am_web_remove_parameter_from_query(const char *inpString,
                                   const char *remove_str, char **outString )
{
    const char *thisfunc = "am_web_remove_parameter_from_query()";
    am_status_t status = AM_FAILURE;
    try {
	string inpStr = inpString;
	string remStr = remove_str;
	string outStr;
	std::size_t i = 0, j = 0 ;

	if((i = inpStr.find(remStr)) !=  std::string::npos){
	    if(i != 0){
		outStr.append(inpStr, 0 , i - 1 );
	    }
	    if((i = inpStr.find("&", i)) != string::npos) {
		if(outStr.length() > 0){
		    if((j = outStr.find("?")) != string::npos ||
			(j = outStr.find("=")) != string::npos) {
			outStr.append("&");
		    }else {
			outStr.append("?");
		    }
		    outStr.append(inpStr, i + 1, inpStr.size() - i);
		} else {
		    outStr.append(inpStr, i + 1, inpStr.size() - i);
		}
	    }
	} else {
	    outStr = inpStr;
	}
	am_web_log_debug("String before removal [%s]", inpStr.c_str());
	strcpy(*outString, outStr.c_str());
	am_web_log_debug("String after removal of %s [%s]",
                         remove_str, *outString);
	status = AM_SUCCESS;
    }
    catch (std::bad_alloc& exb) {
	status = AM_NO_MEMORY;
    }
    catch (std::exception& exs) {
	am_web_log_error("%s: Exception caught: %s", thisfunc, exs.what());
	status = AM_FAILURE;
    }
    catch (...) {
	status = AM_FAILURE;
    }
    return status;
}

extern "C" AM_WEB_EXPORT am_status_t
am_web_get_parameter_value(const char* inpString,
                           const char* param_name, char **param_value)
{
    const char *thisfunc = "am_web_get_parameter_value()";
    am_status_t status = AM_NOT_FOUND;
    string inpStr = inpString;
    string paramStr = param_name;
    string outStr;
#if defined(_AMD64_)
    DWORD64 i = 0, end_param = 0;
#else
    int i = 0, end_param = 0;
#endif

    *param_value = NULL;
    if (inpString == NULL || param_name == NULL && param_value == NULL) {
        status = AM_INVALID_ARGUMENT;
    }
    else {
	try {
	    if((i = inpStr.find(paramStr)) != -1) {
		i = inpStr.find("=", i);
		if (i >= 0) {
		    if((end_param = inpStr.find("&", i+1)) != -1) {
			outStr.append(inpStr, i+1, end_param - (i+1));
		    } else {
			outStr.append(inpStr, i+1, inpStr.length() - 1);
		    }
		    *param_value = strdup(outStr.c_str());
		    if(*param_value == NULL) {
			status = AM_NO_MEMORY;
		    }
		    else {
			status = AM_SUCCESS;
		    }
		}
	    }
        }
	catch (std::bad_alloc& exb) {
	    status = AM_NO_MEMORY;
	}
	catch (std::exception& exs) {
	    am_web_log_error("%s: Exception encountered: %s",
			     thisfunc, exs.what());
	    status = AM_FAILURE;
	}
	catch (...) {
	    am_web_log_error("%s: Unknown xception encountered", thisfunc);
	    status = AM_FAILURE;
	}
    }
    am_web_log_debug("%s: Param Name = %s, & Param Value = %s, status %s",
                     thisfunc, param_name,
		     *param_value==NULL?"NULL":*param_value,
                     am_status_to_string(status));
    return status;
}


/* Throws std::exception's from string methods */
string
create_authn_request_query_string(string requestID, string providerID,
                                  string issueInstant, bool isSigned) {
    string urlEncodedAuthnRequest;
    urlEncodedAuthnRequest.reserve(512);
    urlEncodedAuthnRequest.append("RequestID=").append(smi::Http::encode(requestID)).append("&");
    urlEncodedAuthnRequest.append("MajorVersion=").append(SAML_PROTOCOL_MAJOR_VERSION).append("&");
    urlEncodedAuthnRequest.append("MinorVersion=").append(SAML_PROTOCOL_MINOR_VERSION).append("&");
    urlEncodedAuthnRequest.append("ProviderID=").append(smi::Http::encode(providerID)).append("&");
    urlEncodedAuthnRequest.append("IssueInstant=").append(smi::Http::encode(issueInstant));

    return urlEncodedAuthnRequest;
}



/**
 * Overrides url with the agent uri prefix's protocol host or port
 * if configured in OpenSSOAgentConfiguration.properties.
 * Returns true if something in url was overridden, false otherwise.
 */
static bool
overrideProtoHostPort(URL& url, 
                      void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    bool override = false;
    if ((*agentConfigPtr)->override_protocol) {
	url.setProtocol(std::string((*agentConfigPtr)->agent_server_url.protocol));
	override = true;
    }

    if ((*agentConfigPtr)->override_host || url.getHost().size() == 0) {
	url.setHost((*agentConfigPtr)->agent_server_url.host);
	override = true;
    }

    if ((*agentConfigPtr)->override_port) {
	url.setPort((*agentConfigPtr)->agent_server_url.port);
	override = true;
    }
    return override;
}

am_status_t
log_access(am_status_t access_status,
	   const char *remote_user,
	   const char *user_sso_token,
	   const char *url,
           void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    am_status_t status = AM_SUCCESS;
    char fmtStr[MSG_MAX_LEN];
#if (defined(WINNT) || defined(_AMD64_))
    UINT key = 0;
#else
    const char *key = NULL;
#endif

    if ((*agentConfigPtr)->log_access_type != LOG_ACCESS_NONE) {
	switch(access_status) {
	case AM_SUCCESS:
	    if ((*agentConfigPtr)->log_access_type & LOG_ACCESS_ALLOW) {
		key = AM_WEB_ALLOW_USER_MSG;
	    }
	    break;
	case AM_ACCESS_DENIED:
	    if ((*agentConfigPtr)->log_access_type & LOG_ACCESS_DENY) {
		key = AM_WEB_DENIED_USER_MSG;
	    }
	    break;
	default:
	    break;
	}
	if (key != NULL) {
	    memset(fmtStr, 0, sizeof(fmtStr));
	    get_string(key, fmtStr, sizeof(fmtStr));
            status = Log::auditLog((*agentConfigPtr)->auditLogDisposition,
                    (*agentConfigPtr)->localAuditLogFileRotate,
                    (*agentConfigPtr)->localAuditLogFileSize,
                    (*agentConfigPtr)->remote_LogID,
                    AM_LOG_LEVEL_INFORMATION, 
                    user_sso_token,
                    fmtStr, 
                    remote_user, 
                    url);
	}
    }
    return status;
}

am_bool_t
is_url_not_enforced(const char *url, const char *client_ip, std::string pInfo, 
        std::string query, void* agent_config)
{
    const char *thisfunc = "is_url_not_enforced()";
    am_status_t status = AM_SUCCESS;
    am_bool_t foundInNotEnforcedList = AM_FALSE;
    am_bool_t inNotenforceIP = AM_FALSE;
    am_bool_t isNotEnforced = AM_FALSE;
    am_bool_t isLogoutURL = AM_FALSE;    
    std::string urlStr(url);
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    
    // Check all required arguments.
    if (url == NULL || *url == '\0' || 
            (AM_TRUE == (*agentConfigPtr)->check_client_ip &&
            NULL == client_ip || *client_ip == '\0')) {
        status = AM_INVALID_ARGUMENT;
    }

    if (status == AM_SUCCESS) {
        try {
            // See if it client ip is in the not enforced client ip list.
            if ((*agentConfigPtr)->not_enforce_IPAddr != NULL) {
                if ((*agentConfigPtr)->not_enforce_IPAddr->find(client_ip) !=
                         (*agentConfigPtr)->not_enforce_IPAddr->end()) {
                    inNotenforceIP = AM_TRUE;
                    am_web_log_debug("%s: client_ip %s is not enforced",
                                thisfunc, client_ip);
                } else {
                    am_web_log_debug("%s: client_ip %s not found in "
                           "client ip not enforced list",
                           thisfunc, client_ip);
                }
            }
            // Do the not enforced list check only if the client ip check
            // fails; no need otherwise 
            if (AM_FALSE == inNotenforceIP) {
                if ((AM_TRUE == (*agentConfigPtr)->ignore_path_info) && (!pInfo.empty())) {
                    // Add again the path info and query to the url as they
                    // were removed for the AM evaluation
                    am_web_log_debug("%s: Add path info (%s) and query (%s) that were "
                                     "removed for AM evaluation",
                                     thisfunc, pInfo.c_str(), query.c_str());
                    urlStr.append(pInfo).append(query);
                }
                URL url_again(urlStr, pInfo);
                foundInNotEnforcedList = in_not_enforced_list(url_again, agent_config);
            }
        } catch (std::bad_alloc& exb) {
            status = AM_NO_MEMORY;
        } catch (InternalException& exi) {
            status = exi.getStatusCode();
        } catch (std::exception& exs) {
            am_web_log_error("%s: Exception (%s) caught while checking "
                        "if client IP is in not enfourced list.",
                        thisfunc, exs.what());
            status = AM_FAILURE;
        } catch (...) {
            am_web_log_error("%s: Unknown Exception while checking if "
                        "client IP is in not enforced list.", thisfunc);
            status = AM_FAILURE;
        }
    }
    // check if it's the logout URL
    if (status == AM_SUCCESS) {
        if ((*agentConfigPtr)->logout_url_list.size > 0 &&
            am_web_is_logout_url(url, agent_config) == B_TRUE) {
            isLogoutURL = AM_TRUE;
        }
    }
    // If one of the above tests succeeded, the request is
    // not enforced -> setup a special flag
    if (status == AM_SUCCESS) {
        if ((AM_TRUE == inNotenforceIP) ||
                (AM_TRUE == foundInNotEnforcedList) ||
                (AM_TRUE == isLogoutURL)) {
            isNotEnforced = AM_TRUE;
            am_web_log_max_debug("%s: URL %s is not enforced.", thisfunc, url);
        } else {
            am_web_log_max_debug("%s: URL %s is enforced.", thisfunc, url);
        }
    }

    return isNotEnforced;
}

am_status_t
get_normalized_url(const char *url_str, 
                   const char *path_info,
                   std::string &normalizedURL, 
                   std::string &pInfo,
                   void* agent_config)
{
    const char *thisfunc = "get_normalized_url()";
    am_status_t status = AM_SUCCESS;
//    am_bool_t isNotEnforced = AM_FALSE;
    std::string new_url_str;
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    // Check argument
    if (url_str == NULL || *url_str == '\0') {
        status = AM_INVALID_ARGUMENT;
    }
    
    if (status == AM_SUCCESS) {
        // Parse & canonicalize URL
        am_web_log_max_debug("%s: Original url: %s", thisfunc, url_str);
        am_web_log_max_debug("%s: PathInfo: %s", thisfunc, path_info);

        try {
            if (AM_TRUE == (*agentConfigPtr)->ignore_path_info) {
                am_web_log_max_debug("%s: Ignoring path info for "
                                     "policy evaluation.", thisfunc);
                pInfo=path_info;
                URL urlObject(url_str, pInfo);
                (void)overrideProtoHostPort(urlObject, agent_config);
                urlObject.getBaseURL(normalizedURL);
                pInfo.erase();

            } else {
                am_web_log_max_debug("%s: Using Full URI for "
                                     "policy evaluation.", thisfunc);
                URL urlObject(url_str);
                (void)overrideProtoHostPort(urlObject, agent_config);
                urlObject.getURLString(normalizedURL);
            }
            am_web_log_max_debug("%s: Normalized url: %s",
                                 thisfunc, normalizedURL.c_str());

        } catch(InternalException &iex) {
            Log::log(boot_info.log_module, Log::LOG_ERROR, iex);
            status = iex.getStatusCode();
        } catch(std::exception &ex) {
            Log::log(boot_info.log_module, Log::LOG_ERROR, ex);
            status = AM_FAILURE;
        } catch(...) {
            Log::log(boot_info.log_module, Log::LOG_ERROR,
            "%s: Unknown exception during URL canonicalization.", thisfunc);
            status = AM_FAILURE;
        }
    }
    return status;
}

/**
 * Method to evaluate boolean policies for a resource
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_is_access_allowed(const char *sso_token,
                const char *url_str,
                const char *path_info,
                const char *action_name,
                const char *client_ip,
                const am_map_t env_parameter_map,
                am_policy_result_t *result,
                void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    const char *thisfunc = "am_web_is_access_allowed()";
    am_status_t status = AM_SUCCESS;
    char fmtStr[MSG_MAX_LEN];
    const char *rmtUsr = NULL;
    std::string queryToken;
    am_bool_t isNotEnforced = AM_FALSE;
    am_bool_t ignorePolicyResult = AM_FALSE;
    const char *url = NULL;
    std::string normalizedURL;
    std::string pInfo;
    std::string test;
    am_bool_t isAgentLogoutURL = AM_FALSE;
    am_status_t log_status = AM_SUCCESS;
    char * encodedUrl = NULL;
#if defined(_AMD64_)
    DWORD64 encodedUrlSize = 0;
#else
    unsigned int encodedUrlSize = 0;
#endif

    std::string originalPathInfo("");
    std::string originalQuery("");
    // The following two variables gets used in cookieless mode
    char *urlSSOToken = NULL;    //sso_token present in the url
    char *modifiedURL = NULL;    //modified url after removal of sso_token

    memset(fmtStr, 0, sizeof(fmtStr));

    // Check arguments
    if (url_str == NULL || action_name == NULL ||
            *url_str == '\0' || action_name == '\0' ||
            env_parameter_map == NULL || result == NULL ||
            (AM_TRUE == (*agentConfigPtr)->check_client_ip &&
            NULL == client_ip || *client_ip == '\0')) {
        status = AM_INVALID_ARGUMENT;
    }

    // parse & canonicalize URL
    if (status == AM_SUCCESS) {
        const char *pQuery = strchr(url_str,'?');
        // The query and path info are saved here because get_normalized_url()
        // will set them to null if ignore_path_info is true
        if (pQuery != NULL) {
            originalQuery.append(pQuery);
        }
        if (path_info != NULL) {
            originalPathInfo.append(path_info);
        }
        status = get_normalized_url(url_str, path_info, 
                    normalizedURL, pInfo, agent_config);
    }
    if (status == AM_SUCCESS) {
        url = normalizedURL.c_str();
        if (url == NULL || *url == '\0') {
            status = AM_INVALID_ARGUMENT;
        } else {
            am_web_log_max_debug("%s: Processing url %s.", thisfunc, url);
        }
    }

    // check FQDN access
    if (status == AM_SUCCESS) {
        try {
            if (AM_FALSE == is_valid_fqdn_access(url, agent_config)) {
                status = AM_INVALID_FQDN_ACCESS;
            }
        } catch (std::exception& exs) {
            am_web_log_error("%s: Exception encountered while checking "
                     "valid fqdn access: %s", thisfunc, exs.what());
            status = AM_INVALID_FQDN_ACCESS;
        } catch (...) {
            am_web_log_error("%s: Unknown exception while checking "
                     "valid fqdn access", thisfunc);
            status = AM_INVALID_FQDN_ACCESS;
        }
    }

    if (status == AM_SUCCESS) {
        // Check if the url is enforced
        isNotEnforced = is_url_not_enforced(url, client_ip, originalPathInfo,
                                     originalQuery,agent_config);
        // Check if it's the logout URL
        if ((*agentConfigPtr)->agent_logout_url_list.size > 0 &&
            am_web_is_agent_logout_url(url, agent_config) == B_TRUE) {
            isAgentLogoutURL = AM_TRUE;
            am_web_log_max_debug("%s: url %s IS logout url.", thisfunc,url);
        }
    }

    //Check whether agent is operating in cookieless mode.
    //If yes, extract sso token from the url. The modified url after
    //removal of sso token needs to be sent for policy evaluations.
    if (sso_token == NULL || '\0' == sso_token[0]) {
        if(url != NULL && strlen(url) > 0) {
            am_web_get_parameter_value(url,
                                       am_web_get_cookie_name(agent_config),
                                       &urlSSOToken);

            if (urlSSOToken != NULL) {
                sso_token = urlSSOToken;
                am_web_log_debug("am_web_is_access_allowed(): "
                        "sso token from request url = \"%s\"", sso_token);
                modifiedURL = (char *) malloc(strlen(url));
                if(modifiedURL != NULL) {
                    am_web_remove_parameter_from_query(url,
                                                   am_web_get_cookie_name(agent_config),
                                                   &modifiedURL);
                    url = modifiedURL;
                    am_web_log_debug("am_web_is_access_allowed(): "
                        "Modified url = \"%s\"", url);
                } else {
                    am_web_log_error("am_web_is_access_allowed(): "
                            "malloc fail to allocate memory for modifiedURL");
                }
            }
        }
    }

    // No further processing if notenforced_url_attributes_enable
    // is set to false and URL is not enforced and 
    // also not logout url. 
    // If the accessed URL is logout url, then it requires processing.
    if (isNotEnforced == AM_TRUE &&
                (*agentConfigPtr)->notenforced_url_attributes_enable 
                 == AM_FALSE && isAgentLogoutURL == AM_FALSE ) {
        am_web_log_debug("%s: URL = %s is in not-enforced list and "
                         "notenforced.url.attributes.enable is set to "
                         "false", thisfunc, url);
    } else {
        if (status == AM_SUCCESS) {
            // check sso token. if URL is enforced, return invalid session if no
            // token is provided.
            if (sso_token == NULL || '\0' == sso_token[0]) {
                // no sso token
                if (AM_FALSE == isNotEnforced) {
                    am_web_log_debug("%s(%s,%s): no sso token, "
                            "setting status to invalid session.",
                            thisfunc, url, action_name);
                    status = AM_INVALID_SESSION;
                }
            }  else {
                // Then do am_policy_evaluate to get session info (userid, attributes)
                // whether or not url is enforced, whether or not do_sso_only is true.
                // but ignore result if url not enforced or do_sso_only.
                // Note: This will be replaced by a different policy call to just get
                // user ldap attributes or id so the policy does not get evaluated.

                KeyValueMap action_results_map;
                am_status_t eval_status = AM_SUCCESS;
                // the following should not throw exceptions 

                if (client_ip != NULL && *client_ip != '\0') {
                    // check whether requestIp is already set in
                    // environment map else set it.
                    const char* tmpRequestIP = am_map_find_first_value(env_parameter_map,
                                          requestIp);
                    if(tmpRequestIP != NULL) {
                        am_web_log_debug("%s(%s,%s): , "
                            "requestIp is already set in env_parameter_map.",
                            thisfunc, url, tmpRequestIP);
                    } else {
                        set_host_ip_in_env_map(client_ip, env_parameter_map,
                                     agent_config);
                    }
                }
                if ((isNotEnforced == AM_TRUE &&
                       ((*agentConfigPtr)->notenforced_url_attributes_enable 
                       == AM_FALSE) && 
                       (*agentConfigPtr)->setUserProfileAttrsMode
                       == SET_ATTRS_NONE)) {
                       am_web_log_debug("%s: URL = %s is in not-enforced "
                                   "list and ldap attribute mode is NONE",
                                    thisfunc, url);
                } else {
                    if ((*agentConfigPtr)->encode_url_special_chars 
                               == AM_TRUE ) {
                        encodedUrlSize = (strlen(url)+1)*4;
                        encodedUrl = (char *) malloc (encodedUrlSize);
                        // Check url for special chars
                        if (encodedUrl != NULL) {
                            bool url_spl_flag = false;
                            memset(encodedUrl, 0, encodedUrlSize);
                            for(int i = 0; i < strlen(url); i++) {
                                if (( url[i] <  32) || ( url[i] > 127))  {
                                    url_spl_flag = true;
                                }
                            }
                            if (url_spl_flag) {
                                encode_url(url, encodedUrl);
                                am_web_log_debug("%s: original URL = %s",
                                                 thisfunc, url);
                                url = encodedUrl;
                                am_web_log_debug("%s: encoded URL = %s", 
                                                  thisfunc, encodedUrl);
                            }
                        } else {
                            am_web_log_error("%s: failed to allocate"
                              "%d bytes for encodedUrl",encodedUrlSize);
                        }
                    } 
                    if (AM_TRUE == isNotEnforced || (isAgentLogoutURL == AM_TRUE) || 
                           AM_TRUE == (*agentConfigPtr)->do_sso_only) {
                        ignorePolicyResult = AM_TRUE;
                    }
                    // Use the policy clock skew if set
                    policy_clock_skew = (*agentConfigPtr)->policy_clock_skew;
                    eval_status = am_policy_evaluate_ignore_url_notenforced(
                                 boot_info.policy_handle,
                                 sso_token, url, action_name,
                                 env_parameter_map,
                                 reinterpret_cast<am_map_t>
                                 (&action_results_map),
                                 result,
                                 ignorePolicyResult,
                                 (*agentConfigPtr)->properties);
                    // if eval policy success, check policy decision in the result
                    // if it is enforced and not in do_sso_only mode.
                    if (eval_status == AM_SUCCESS) {
                        if (AM_FALSE == isNotEnforced &&
                            AM_FALSE == (*agentConfigPtr)->do_sso_only) {
                            try {
                                if (action_results_map.size() == 0) {
                                    am_web_log_debug("%s(%s, %s) denying access: "
                                          "NULL action_result_map",
                                          thisfunc, url, action_name);
                                    status = AM_ACCESS_DENIED;
                                } else {
                                    status = eval_action_results_map(
                                             action_results_map,
                                             url,
                                             action_name,
                                             result);
                                }
                            } catch (std::bad_alloc& exb) {
                                status = AM_NO_MEMORY;
                            } catch (std::exception& exs) {
                                am_web_log_error("%s(%s,%s): Exception "
                                          "encountered while checking policy "
                                          "results: %s", thisfunc, url, 
                                          action_name, exs.what());
                                status = AM_FAILURE;
                            } catch (...) {
                                am_web_log_error("%s(%s,%s): Unknown Exception "
                                             "while checking policy results: %s",
                                             thisfunc, url, action_name);
                                status = AM_FAILURE;
                            }
                        }
                    } else {
                        // if eval policy failure, ignore if in do_sso_only and
                        // failure is related to policy (and not invalid session).
                        // also ignore if url is not enforced.
                        // otherwise set status to the evaluation status.

                        // Note: This is a temporary workaround for
                        // determining if the session is valid. This should be
                        // replaced by an new and more appropriate function call
                        // which only evaluates if the current session
                        // is valid or not.
                        if (AM_TRUE == (*agentConfigPtr)->do_sso_only &&
                                  (eval_status == AM_NO_POLICY ||
                            eval_status == AM_INVALID_ACTION_TYPE)) {
                            am_web_log_debug("%s(%s, %s) do_sso_only - ignore "
                                   "policy eval result of no policy "
                                   "or invalid action",
                                   thisfunc, url, action_name);
                        } else if (AM_FALSE == isNotEnforced) {
                            status = eval_status;
                        }
                    }
                    // Access denied, no policy and invalid action type are three
                    // error types that are mapped to access denied.  All others
                    // are handled as they are.  INVALID session will redirect
                    // user to auth, most others throw 500 internal server error.
                    if (AM_SUCCESS != status) {
                        am_web_log_warning("%s(%s, %s) denying access: "
                                    "status = %s",
                                    thisfunc, url, action_name,
                                    am_status_to_string(status));
                        if(status == AM_NO_POLICY ||
                                status == AM_INVALID_ACTION_TYPE) {
                            status = AM_ACCESS_DENIED;
                        }
                    }

                    // Check if the ip address matches what is in sso token,
                    // if check client ip address is true and url is enforced.
                    if (AM_SUCCESS == status && AM_FALSE == isNotEnforced &&
                        AM_TRUE == (*agentConfigPtr)->check_client_ip &&
                        result->remote_IP != NULL) {
                        if (client_ip == NULL ||
                            strcmp(result->remote_IP, client_ip) != 0) {
                            status = AM_ACCESS_DENIED;
                            am_web_log_warning("%s: Client ip [%s] "
                                     "does not match sso token ip [%s]. "
                                     "Denying access.",
                                     thisfunc, result->remote_IP,
                                     client_ip);
                        } else {
                            am_web_log_debug("%s: Client ip [%s] matched "
                                                "sso token ip.", thisfunc,
                            result->remote_IP);
                        }
                    }
                }

                // Invalidate user's sso token if it's the agent logout URL,
                // ignore the invalidate status.
                // Note that this must be done *after* am_policy_evaluate()
                // so we can get the user's id and pass it to the web app.
                // Can't get the user's id after session's been invalidated.
                if (isAgentLogoutURL && sso_token != NULL && sso_token[0] != '\0'
                                && status != AM_INVALID_SESSION){
                    am_web_log_debug("invalidating session %s", sso_token);
                    am_status_t redirLogoutStatus = AM_FAILURE;
                    am_status_t logout_status =
                    am_policy_user_logout(boot_info.policy_handle,
                                 sso_token,(*agentConfigPtr)->properties);
                    if (AM_SUCCESS != logout_status) {
                        am_web_log_warning(
                                   "%s: Error %s invalidating session %s.",
                                   thisfunc, am_status_to_name(logout_status),
                                   sso_token);
                    } else {
                        am_web_log_debug("%s: Logged out session id %s.",
                                        thisfunc, sso_token);
                        redirLogoutStatus = AM_REDIRECT_LOGOUT;
                    }
                    return redirLogoutStatus;

                }
            }
        }
    }

    // Set user in the result.
    // result->remote_user gets freed in am_policy_result_destroy().
    if (AM_SUCCESS == status && result->remote_user == NULL) {
        if (AM_TRUE == (*agentConfigPtr)->anon_remote_user_enable &&
                   (*agentConfigPtr)->unauthenticated_user != NULL &&
                   *(*agentConfigPtr)->unauthenticated_user != '\0') {
            result->remote_user = strdup((*agentConfigPtr)->unauthenticated_user);
    } else {
        result->remote_user = NULL;
    }
    am_web_log_debug("%s: remote user set to unauthenticated user %s",
                     thisfunc, result->remote_user);
    }

    // log the final access allow/deny result in IS's audit log.
    rmtUsr = (result->remote_user == NULL) ? "unknown user" :
                result->remote_user;
    // We do not log the notenforced list accesses for allow,
    // because, if agent is installed on top of DSAME, the
    // access to loggingservice, which gets allowed by the
    // not enforced list would cause a recursion tailspin.
    if (AM_FALSE == isNotEnforced) {
        log_status = log_access(status, result->remote_user, 
                      sso_token, url, agent_config);
    }
    if(encodedUrl) {
        am_web_free_memory(encodedUrl);
    }
    am_web_free_memory(modifiedURL);
    am_web_log_info("%s(%s, %s) returning status: %s.",
              thisfunc, url, action_name, am_status_to_string(status));
    return status;
}


extern "C" AM_WEB_EXPORT boolean_t
am_web_is_notification(const char *request_url,
                       void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "am_web_is_notification()";
    boolean_t result = B_FALSE;
    std::string request_url_str;

    if (am_policy_is_notification_enabled(boot_info.policy_handle) ||
        (*agentConfigPtr)->configChangeNotificationEnable) {
	try {
	    // check override proto/host/port for notification url.
	    // this would be true if notifications url is coming from
	    // outside a firewall.
		URL url(request_url);

	    if ((*agentConfigPtr)->override_notification_url) {
			overrideProtoHostPort(url, agent_config);
	    }

            url.getURLString(request_url_str);
            request_url = request_url_str.c_str();

            if ((*agentConfigPtr)->notification_enable ||
                (*agentConfigPtr)->configChangeNotificationEnable) {
                if (!strcasecmp(request_url, 
                               (*agentConfigPtr)->notification_url)) {
                    result = B_TRUE;
                } else {
                    am_web_log_max_debug("%s: %s is not notification url %s.",
                                         thisfunc, request_url,
                                        (*agentConfigPtr)->notification_url);
                }
            }
	}
	catch (std::exception& ex) {
	    am_web_log_error("%s: Unexpected exception %s encountered "
			     "while checking url %s.",
			     thisfunc, ex.what(), request_url);
	}
	catch (...) {
	    am_web_log_error("%s: Unknown exception encountered "
			     "while checking url %s.",
			     thisfunc, request_url);
	}
    }
    return result;
}

extern "C" AM_WEB_EXPORT void
am_web_handle_notification(const char *data,
			   size_t data_length,
                           void* agent_config)
{
    am_status_t status;
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    Log::log(boot_info.log_module, Log::LOG_DEBUG,
	     "am_web_handle_notification() data is: %.*s", data_length,
	     data);

    if((*agentConfigPtr)->configChangeNotificationEnable) {
        status = am_policy_notify(boot_info.policy_handle,
                              data, 
                              data_length,
                              B_TRUE);
    } else {
        status = am_policy_notify(boot_info.policy_handle,
                              data, 
                              data_length,
                              B_FALSE);
    }
    if (AM_SUCCESS != status) {
	am_web_log_error("am_web_handle_notification() error processing "
			"notification: status = %s (%d), notification data = "
			"%.*s", am_status_to_string(status), status,
			data_length, data);
    }
}

/* Throws std::exception's from string methods */
am_status_t
getValid_FQDN_URL(const char *goto_url, 
                  string &result, 
                  void* agent_config) {

    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    am_status_t retVal = AM_SUCCESS;
    const std::string newURL(
		(*agentConfigPtr)->fqdn_handler->getValidFqdnResource(goto_url));
    if(!newURL.empty()) {
	result = newURL;
    } else {
	Log::log(boot_info.log_module, Log::LOG_ERROR,
		 "getValid_FQDN_URL(): Cannot find valid FQDN URL.");
	retVal = AM_FAILURE;
    }
    return retVal;
}

/**
 * When a user comes back from the CDSSO authentication, there are a
 * list of Liberty parameters that we added when the user was redirected
 * for authentication.  This function removes those extra parameters, so
 * that the request could be forwarded to the applications as it came
 * from the browser.
 *
 * Parameter:
 *    inpString: Request URL that was recieved after authentication
 *               containing Liberty attributes.
 *    outString: (OUT) The output string where all the Liberty
 *               attributes are cleaned up.
 *
 * Returns:
 *  am_status_t:
 *               AM_SUCCESS if operation was successful, appropriate
 *               error codes otherwise.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_remove_authnrequest(char *inpString, char **outString) {
	am_status_t status = AM_FAILURE;

	status = am_web_remove_parameter_from_query(inpString, "RequestID",
                                                                outString);
	status = am_web_remove_parameter_from_query(*outString, "MajorVersion",
                                                                outString);
	status = am_web_remove_parameter_from_query(*outString, "MinorVersion",
                                                                outString);
	status = am_web_remove_parameter_from_query(*outString, "ProviderID",
                                                                outString);
	status = am_web_remove_parameter_from_query(*outString, "IssueInstant",
                                                                outString);
	status = am_web_remove_parameter_from_query(*outString, "ForceAuthn",
                                                                outString);
	status = am_web_remove_parameter_from_query(*outString, "IsPassive",
                                                                outString);
	status = am_web_remove_parameter_from_query(*outString, "Federate",
                                                                outString);
	return status;
}

extern "C" AM_WEB_EXPORT am_status_t
am_web_get_request_url(const char *host_hdr, const char *protocol,
		       const char *hostname, size_t port, const char *uri,
		       const char *query, char **request_url,
                       void* agent_config) {
    am_status_t retVal = AM_SUCCESS;
    URL url;

    if(uri != NULL) url.setURI(uri);
    if(query != NULL) url.setQuery(query);
    if(protocol != NULL) url.setProtocol(protocol);

    if(host_hdr == NULL || strlen(host_hdr) == 0) {
	if(hostname != NULL) {
	    url.setHost(hostname);
	}

	if(port > PORT_MIN_VAL && port < PORT_MAX_VAL) {
	    url.setPort(port);
	}
    } else {
	std::string hostHdr(host_hdr);
	std::size_t pos = hostHdr.find(':');
	if(pos == std::string::npos) {
	    url.setHost(hostHdr);
	} else {
	    std::string host = hostHdr.substr(0, pos);
	    if(host.size() != 0) {
		url.setHost(host);
	    } else {
		url.setHost(hostname);
	    }

	    port = Utils::getNumber(hostHdr.substr(pos + 1));
	    if(port > PORT_MIN_VAL && port < PORT_MAX_VAL) url.setPort(port);
	}
    }
    overrideProtoHostPort(url, agent_config);
    std::string urlStr;
    url.getURLString(urlStr);

    *request_url = (char *)malloc(urlStr.size() + 1);
    if(*request_url == NULL) {
	am_web_log_error("am_web_get_request_url(): "
			 "Unable to allocate memory.");
	retVal = AM_NO_MEMORY;
    } else {
	strcpy(*request_url, urlStr.c_str());
    }
    return retVal;
}

/**
 * Sets original request url and request url.
 * request url gets overrideden if override properties are set.
 * Original request url gets used during notification request processing.
 * request url is used for rest of request processing.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_get_all_request_urls(const char *host_hdr, 
                       const char *protocol,
		       const char *hostname, 
                       size_t port, 
                       const char *uri,
		       const char *query, 
                       void* agent_config,
                       char **request_url,
                       char **orig_request_url) {
    am_status_t retVal = AM_SUCCESS;
    URL url;

    if(uri != NULL) url.setURI(uri);
    if(query != NULL) url.setQuery(query);
    if(protocol != NULL) url.setProtocol(protocol);

    if(host_hdr == NULL || strlen(host_hdr) == 0) {
	if(hostname != NULL) {
	    url.setHost(hostname);
	}

	if(port > PORT_MIN_VAL && port < PORT_MAX_VAL) {
	    url.setPort(port);
	}
    } else {
	std::string hostHdr(host_hdr);
	std::size_t pos = hostHdr.find(':');
	if(pos == std::string::npos) {
	    url.setHost(hostHdr);
	} else {
	    std::string host = hostHdr.substr(0, pos);
	    if(host.size() != 0) {
		url.setHost(host);
	    } else {
		url.setHost(hostname);
	    }

	    port = Utils::getNumber(hostHdr.substr(pos + 1));
	    if(port > 0 && port < 65536) url.setPort(port);
	}
    }

    // set original request url
    std::string origUrlStr;
    url.getURLString(origUrlStr);

    *orig_request_url = (char *)malloc(origUrlStr.size() + 1);
    if(*orig_request_url == NULL) {
	am_web_log_error("am_web_get_all_request_urls(): "
			 "Unable to allocate memory to orig_request_url.");
	retVal = AM_NO_MEMORY;
    } else {
	am_web_log_debug("am_web_get_all_request_urls(): "
			 "orig_request_url is %s",  origUrlStr.c_str());
	strcpy(*orig_request_url, origUrlStr.c_str());
    }

    // sets override request url, if override is set
    overrideProtoHostPort(url, agent_config);
    std::string urlStr;
    url.getURLString(urlStr);

    *request_url = (char *)malloc(urlStr.size() + 1);
    if(*request_url == NULL) {
	am_web_log_error("am_web_get_all_request_urls(): "
			 "Unable to allocate memory to request_url.");
	retVal = AM_NO_MEMORY;
    } else {
	am_web_log_debug("am_web_get_all_request_urls(): "
			 "request_url is %s",  urlStr.c_str());
	strcpy(*request_url, urlStr.c_str());
    }
    return retVal;
}

extern "C" AM_WEB_EXPORT am_status_t
am_web_check_cookie_in_post(void **args, char **dpro_cookie,
                char **request_url,
                char **orig_req, char *method,
                char *response,
                boolean_t responseIsCookie,
                am_status_t (*set_cookie)(const char*, void**),
                void (*set_method)(void **, char *), 
                void* agent_config) {
    const char *thisfunc = "am_web_check_cookie_in_post()";
    char *recv_token = NULL;
    am_status_t status = AM_SUCCESS;

    // Check arguments
    if(response == NULL || strlen(response) == 0) {
        am_web_log_error("%s: Response object is NULL or empty.", thisfunc);
        status = AM_INVALID_ARGUMENT;
    }
    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: AuthnResponse received = %s", 
                         thisfunc, response);
        // Check for Liberty response
        if(responseIsCookie) {
            recv_token = strdup(response);
            if(recv_token == NULL) {
                am_web_log_error("%s: Unable to allocate memory "
                                 "for recv_token.", thisfunc);
                status = AM_NO_MEMORY;
            } 
        } else {
            status = am_web_get_token_from_assertion(response, &recv_token,
                                                     agent_config);
            if (status != AM_SUCCESS) {
                am_web_log_error("%s: am_web_get_token_from_assertion() "
                                 "failed with error code: %s",
                                 thisfunc, am_status_to_string(status));
            }
        }
    }
    if(status == AM_SUCCESS) {
        am_web_log_debug("%s: recv_token : %s", thisfunc, recv_token);
        // Set cookie in browser for the foreign domain.
        am_web_do_cookie_domain_set(set_cookie, args, recv_token,
                                    agent_config);
        *dpro_cookie = strdup(recv_token);
        if(*dpro_cookie == NULL) {
            am_web_log_error("%s: Unable to allocate memory for dpro_cookie.",
                             thisfunc);
            status = AM_NO_MEMORY;
        } else {
            free(recv_token);	
            strcpy(method, *orig_req);
            set_method(args, *orig_req);
            status = AM_SUCCESS;
        }
    }

    return status;
}


extern "C" AM_WEB_EXPORT am_status_t
am_web_check_cookie_in_query(
		void **args, char **dpro_cookie,
		const char *query, char **request_url,
		char ** orig_req, char *method,
		am_status_t (*set_cookie)(const char*, void**),
		void (*set_method)(void **, char *),
                void* agent_config) 
{

    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    char *recv_token = NULL;
    am_status_t status = AM_FAILURE;

    if(query != NULL && strlen(query) > 0) {
        status = am_web_get_parameter_value(query,
			am_web_get_cookie_name(agent_config), &recv_token);
	if (status == AM_SUCCESS) {
	    am_web_log_debug("am_web_check_cookie_in_query(): "
			"Received Token from query = \"%s\"", recv_token);
	    am_web_do_cookie_domain_set(set_cookie, args, recv_token, agent_config);

	    *dpro_cookie = strdup(recv_token);
	    if(*dpro_cookie == NULL){
		am_web_log_error("am_web_check_cookie_in_query(): "
				"unable to allocate memory "
				"for dpro cookie , size = %u",
				strlen(recv_token));
		am_web_free_memory(recv_token);
        	status = AM_NO_MEMORY;
	    } else {
	        status = am_web_remove_parameter_from_query(*request_url,
						am_web_get_cookie_name(agent_config),
						request_url);
	        status = am_web_remove_parameter_from_query(*request_url,
						(*agentConfigPtr)->url_redirect_param,
						request_url);
	        status = am_web_remove_authnrequest(*request_url, request_url);
		set_method(args, *orig_req);
	        strcpy(method, *orig_req);
	        status = AM_SUCCESS;
	    }
	} else {
	    /* If there is no cookie in the query string, to keep the original
	     * request method from getting lost, retrieve it back from the
	     * goto url and add it again  to the query string
	     */
	    am_web_log_debug("am_web_check_cookie_in_query(): "
			"No token found in query string: status %s(%d)",
		    	am_status_to_string(status),status);

	    status = am_web_remove_parameter_from_query(*request_url,
						am_web_get_cookie_name(agent_config),
						request_url);
	    status = am_web_remove_parameter_from_query(*request_url,
						(*agentConfigPtr)->url_redirect_param,
						request_url);
	    status = am_web_remove_authnrequest(*request_url, request_url);
	    strcpy(method, *orig_req);
	}
    } else {
        strcpy(method, *orig_req);
	am_web_log_debug("am_web_check_cookie_in_query(): No cookie in query.");
    }
    return status;

}

/* now used by function add_cdsso_elements_to_redirect_url only */
static char *prtime_2_str(char *buffer, 
                          size_t buffer_len, 
                          void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    PRTime timestamp;
    PRExplodedTime exploded_time;
    int n_written = 0;

    PR_Lock((*agentConfigPtr)->lock);
    timestamp = PR_Now();
    PR_Unlock((*agentConfigPtr)->lock);

    PR_ExplodeTime(timestamp, PR_LocalTimeParameters, &exploded_time);
    n_written = snprintf(buffer, buffer_len, "%d-%02d-%02dT%02d:%02d:%02dZ",
                         exploded_time.tm_year, exploded_time.tm_month+1,
			 exploded_time.tm_mday, exploded_time.tm_hour,
			 exploded_time.tm_min, exploded_time.tm_sec);
    if (buffer_len > 0 && n_written < 1) {
        // This means the buffer wasn't big enough or an error occured.
        *buffer = '\0';
    }

    return buffer;
}

/* Throws std::exception's from string methods */
string add_cdsso_elements_to_redirect_url(void* agent_config) {

    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    string providerId = (*agentConfigPtr)->agent_server_url.url;

	const char* orgName = NULL;
	am_status_t status = am_properties_get( (*agentConfigPtr)->properties,
								AM_POLICY_ORG_NAME_PROPERTY,
								&orgName );

	if((status == AM_SUCCESS) && (orgName != NULL) && (orgName[0] != '\0')) {
		//do not pass realm name for agent accounts created under root org
		if (strcmp(orgName, "/")) {
			providerId.append("?Realm=").append(orgName);
		}
	}

    bool isSigned = false;

    // request id
    char id[10];
    srand((unsigned)time(NULL));
    sprintf(id, "%ld", (long int)rand());
    string requestID = id;

    // issue instant
    char *strtime = NULL;
    strtime = (char *) malloc ( AM_WEB_MAX_POST_KEY_LENGTH );
    prtime_2_str(strtime,AM_WEB_MAX_POST_KEY_LENGTH, agent_config);
    string strIssueInstant;
    if(strtime != NULL && strlen(strtime) > 0) {
            strIssueInstant = strtime;
    }

    string authnRequest = create_authn_request_query_string(requestID,
                                                            providerId,
                                                            strIssueInstant,
                                                            isSigned);
    if(strtime!=NULL)
    {
        free(strtime);
        strtime = NULL;
    }

    return authnRequest;
}



extern "C" AM_WEB_EXPORT am_status_t
am_web_get_redirect_url(am_status_t status,
			const am_map_t advice_map,
			const char *goto_url,
			char **redirect_url,
                        void* agent_config) {
    return am_web_get_url_to_redirect(status, advice_map, goto_url, NULL,
				      NULL, redirect_url, agent_config);
}

extern "C" AM_WEB_EXPORT am_status_t
am_web_get_url_to_redirect(am_status_t status,
                 const am_map_t advice_map,
                 const char *goto_url,
                 const char* method,
                 void *reserved,
                 char **redirect_url,
                 void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    const char *thisfunc = "am_web_get_url_to_redirect";
    am_status_t ret = AM_SUCCESS;
    *redirect_url = NULL;

    if (goto_url == NULL || *goto_url == '\0') {
        ret = AM_INVALID_ARGUMENT;
    } else {
        URL gotoURL(goto_url);
        std::string orig_url;
        // override the goto URL to agent's protocol, host or port
        // if configured
        if (overrideProtoHostPort(gotoURL, agent_config)) {
            gotoURL.getURLString(orig_url);
            goto_url = orig_url.c_str();
        }
        am_web_log_max_debug("%s: goto URL is %s", thisfunc, goto_url);
        // if previous status was invalid fqdn access,
        // redirect to the valid fqdn url.
        if (status == AM_INVALID_FQDN_ACCESS) {
            try {
                std::string valid_fqdn;
                ret = getValid_FQDN_URL(goto_url, valid_fqdn, agent_config);
                if (ret != AM_SUCCESS) {
                    am_web_log_error("%s: getValid_FQDN_URL failed with error: "
                        "%s", thisfunc, am_status_to_string(ret));
                } else if (valid_fqdn.size() == 0) {
                    am_web_log_error("%s: getValid_FQDN_URL is empty string",
                                     thisfunc);
                    ret = AM_NOT_FOUND;
                } else {
                    *redirect_url = strdup(valid_fqdn.c_str());
                    if (*redirect_url == NULL) {
                        ret = AM_NO_MEMORY;
                    } else {
                        ret = AM_SUCCESS;
                    }
                }
            } catch (InternalException& exi) {
                ret = exi.getStatusCode();
            } catch (std::bad_alloc& exb) {
                ret = AM_NO_MEMORY;
            } catch (std::exception& exs) {
                am_web_log_error("%s: Exception while getting valid FQDN: %s",
                                 thisfunc, exs.what());
                ret = AM_FAILURE;
            } catch (...) {
                am_web_log_error("%s: Unknown exception while getting FQDN.",
                                 thisfunc);
                ret = AM_FAILURE;
            }
        } else {
            // if previous status was invalid session or if there was a policy
            // advice, redirect to the IS login page. If not, redirect to the
            // configured access denied url if any.
            
            // Check for advice.
            // If we have advice information, then we attempt to customize
            // the redirect URL based on the provided advice.  As of the
            // Alpha code-freeze for Identity Server 6.0, the URL may
            // contain only one of module, level, role, user, or service.
            // I (pds) have made the (semi-)arbitrary decision that if
            // both types of advice are available we will prefer to
            // specify the auth module (scheme), because the module seems
            // more specific than the level.
            const char *auth_advice_value = "";
            size_t auth_advice_value_len = 0;
            const char *session_adv_value = "";
            if (AM_MAP_NULL != advice_map) {
                auth_advice_value = am_map_find_first_value(advice_map,
                                    AUTH_SCHEME_KEY);
                if (NULL != auth_advice_value) {
                    auth_advice_value_len = strlen(auth_advice_value);
                } else {
                    auth_advice_value = am_map_find_first_value(advice_map,
                                        AUTH_LEVEL_KEY);
                    if (NULL != auth_advice_value) {
                        auth_advice_value_len = strlen(auth_advice_value);
                    } else {
                        auth_advice_value = am_map_find_first_value(advice_map,
                                                            AUTH_REALM_KEY);
                        if (NULL != auth_advice_value) {
                            auth_advice_value_len = strlen(auth_advice_value);
                        } else {
                            auth_advice_value = am_map_find_first_value(advice_map,
                                                            AUTH_SERVICE_KEY);
                            if (NULL != auth_advice_value) {
                                auth_advice_value_len = strlen(auth_advice_value);
                            } else {
                                auth_advice_value = "";
                                am_web_log_debug("%s: advice_map contains no "
                                             "Auth-related advices", thisfunc);
                            }
                        }
                    }
                    // Check for session advice
                    session_adv_value = am_map_find_first_value(advice_map,
                                                          SESSION_COND_KEY);
                    if (NULL != session_adv_value) {
                        am_web_log_debug("%s: session adv value: %s ", thisfunc,
                                             session_adv_value);
                    } else {
                        session_adv_value = "";
                        am_web_log_debug("%s: advice_map contains no "
                                         "session-related advices", thisfunc);
                    }
                }
            }
            // If session is invalid or we found a relevant advice, then
            // redirect the browser to the login page.  */
            if ((AM_INVALID_SESSION == status) ||
                      (auth_advice_value_len > 0) ||
                      (strcmp(session_adv_value, AM_WEB_ACTION_DENIED) == 0)) {
                try {
                    Utils::url_info_t *url_info_ptr;
                    std::string encoded_url;
                    std::string retVal;
                    url_info_ptr = find_active_login_server(agent_config);
                    if (NULL == url_info_ptr) {
                        am_web_log_warning("%s: unable to find active Access "
                                     "Manager Auth server.", thisfunc);
                    ret = AM_FAILURE;
                    } else {
                        retVal.append(url_info_ptr->url, url_info_ptr->url_len);
                        // In CDSSO mode, to have the user redirected to a static page that
                        // in turn will redirect to the resource, the goto parameter
                        // with the static page as value must be added to the cdcservlet 
                        // url and the url_redirect_param set to a value other than "goto".
                        // In such a case the "?" character must be added before 
                        // url_redirect_param
                        string temp_url = url_info_ptr->url;
                        string temp_redirect_param = 
                              (*agentConfigPtr)->url_redirect_param;
                        if (((*agentConfigPtr)->cdsso_enable == AM_TRUE) && 
                                (temp_url.find("goto=") != string::npos) && 
                                (temp_redirect_param.compare("goto") != 0)) {
                            retVal.append("?");
                        } else {
                            retVal.append(
                                  (url_info_ptr->has_parameters) ?"&":"?");
                        }
                        retVal.append((*agentConfigPtr)->url_redirect_param);
                        retVal.append("=");
                        if((*agentConfigPtr)->cdsso_enable == AM_TRUE &&
                                       method != NULL) {
                            temp_url = goto_url;   // removed redeclaration
                            encoded_url =
                               PRIVATE_NAMESPACE_NAME::Http::encode(temp_url);
                        } else {
                            encoded_url =
                            PRIVATE_NAMESPACE_NAME::Http::encode(goto_url);
                        }
                        retVal.append(encoded_url);
                        if((*agentConfigPtr)->cdsso_enable == AM_TRUE) {
                            am_web_log_debug("%s: The goto_url and url before "
                                        "appending cdsso elements: "
                                        "[%s] [%s]", thisfunc,
                                        goto_url, retVal.c_str());
                            retVal.append("&");
                            retVal.append(
                            add_cdsso_elements_to_redirect_url(agent_config));
                        }
                        *redirect_url = strdup(retVal.c_str());
                        if (*redirect_url == NULL) {
                            ret = AM_NO_MEMORY;
                        } else {
                            ret = AM_SUCCESS;
                        }
                    }
                } catch (std::bad_alloc& exb) {
                    ret = AM_NO_MEMORY;
                } catch (std::exception& exs) {
                    am_web_log_error("%s: Exception encountered: %s",
                                      thisfunc, exs.what());
                    ret = AM_FAILURE;
                } catch (...) {
                    am_web_log_error("%s: Unexpected exception encountered.",
                                 thisfunc);
                    ret = AM_FAILURE;
                }
            } else if ((*agentConfigPtr)->access_denied_url != NULL) {
                // redirect user to the access denied url if it was configured.
                char codeStr[10];
                std::string redirStr = (*agentConfigPtr)->access_denied_url;
                std::size_t t = redirStr.find('?');
                if(t == std::string::npos) {
                    PUSH_BACK_CHAR(redirStr,'?');
                } else {
                    PUSH_BACK_CHAR(redirStr, '&');
                }
                int x = status;
                redirStr.append("sunwerrcode=");
                snprintf(codeStr, sizeof(codeStr) - 1, "%d", x);
                redirStr += codeStr;
                if (goto_url != NULL) {
                    std::string encoded_url;
                    encoded_url = PRIVATE_NAMESPACE_NAME::Http::encode(goto_url);
                    redirStr.append("&");
                    redirStr.append((*agentConfigPtr)->url_redirect_param);
                    redirStr.append("=");
                    redirStr.append(encoded_url);
                }
                *redirect_url = strdup(redirStr.c_str());
                if (*redirect_url == NULL) {
                    ret = AM_NO_MEMORY;
                } else {
                    ret = AM_SUCCESS;
                }
            } else {
                // if no access denied url configured,
                //return null for redirect url
                *redirect_url = NULL;
                ret = AM_SUCCESS;
            }
        }
    }
    return ret;
}

/*
 * Not exported - resets a given cookies list.
 * Note that the reset (callback) is called for each cookie.
 * If any cookie reset function fails the last failed status is returned.
 */
am_status_t
am_web_reset_cookies_list(const Utils::cookie_info_list_t *cookies_list,
                          am_status_t (*setFunc)(const char *, void **),
                          void **args)
{
    const char *thisfunc = "am_web_reset_cookies_list()";
    am_status_t status = AM_SUCCESS;
    am_status_t tmpStatus = AM_SUCCESS;

    Utils::cookie_info_t *cookies = cookies_list->list;
    unsigned int size = cookies_list->size;
    if (cookies != NULL) {
	try {
	    for (std::size_t idx=0; idx < size; ++idx)
	    {
		char *reset_header =
		    const_cast<char*>(buildSetCookieHeader(&cookies[idx]));
		if (reset_header != NULL) {
		    am_web_log_max_debug("am_web_reset_cookies_list(): "
					 "resetting cookie: %s", reset_header);
		    tmpStatus =  setFunc(reset_header, args);
		    if (AM_SUCCESS != tmpStatus) {
			status = tmpStatus;
                    }
		    free(reset_header);
		    reset_header = NULL;
		}
	    }
	}
	catch (std::bad_alloc& exb) {
	    status = AM_NO_MEMORY;
	}
	catch (std::exception& exs) {
	    am_web_log_error("%s: Exception encountered: %s.",
			     thisfunc, exs.what());
	    status = AM_FAILURE;
	}
	catch (...) {
	    am_web_log_error("%s: Unknown Exception encountered.",thisfunc);
	    status = AM_FAILURE;
	}
    }
    return status;
}

/*
 * reset the ldap attributes cookies list.
 * Note that the reset (callback) is called for each cookie.
 * If any cookie reset function fails the last failed status is returned.
 */
am_status_t
am_web_reset_ldap_attribute_cookies(
     am_status_t (*setFunc)(const char *, void **), 
     void **args, 
     void* agent_config)
{

    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    const char *thisfunc = "am_web_reset_ldap_attribute_cookies()";
    am_status_t status = AM_SUCCESS;
    am_status_t tmpStatus = AM_SUCCESS;

    // Reset cookies from ldap Attributes
    if (((*agentConfigPtr)->setUserProfileAttrsMode == SET_ATTRS_AS_COOKIE) ||
       ((*agentConfigPtr)->setUserSessionAttrsMode == SET_ATTRS_AS_COOKIE)  ||
       ((*agentConfigPtr)->setUserResponseAttrsMode == SET_ATTRS_AS_COOKIE) &&
	    ((*agentConfigPtr)->attrList.size() > 0)) {
	Utils::cookie_info_t attr_cookie;
	attr_cookie.value = const_cast<char*>("");
	attr_cookie.domain = NULL;
        // This must be null to work with older browsers
	// netscape 4.79, IE 5.5, mozilla < 1.4.
	attr_cookie.max_age = const_cast<char*>("0"); // delete the cookie.
	attr_cookie.path = const_cast<char*>("/");

	try {
	    std::list<std::string>::const_iterator iter;
	    for(iter = (*agentConfigPtr)->attrList.begin(); 
                iter != (*agentConfigPtr)->attrList.end(); iter++) {

		std::string attr = (*iter);
		std::string cookie_name((*agentConfigPtr)->attrCookiePrefix);
		cookie_name.append(const_cast<char*>(attr.c_str()));
		attr_cookie.name = const_cast<char*>(cookie_name.c_str());
		char *cookie_header =
		    const_cast<char*>(buildSetCookieHeader(&attr_cookie));
		if (cookie_header != NULL) {
		    Log::log(boot_info.log_module, Log::LOG_DEBUG,
			     "RESET cookie_header=%s", cookie_header);
		    tmpStatus =  setFunc(cookie_header, args);
		    if (AM_SUCCESS != tmpStatus)
			status = tmpStatus;
		    free(cookie_header);
		}
	    }
	}
	catch (std::bad_alloc& exb) {
	    status = AM_NO_MEMORY;
	}
	catch (std::exception& exs) {
	    am_web_log_error("%s: Exception encountered: %s.",
			     thisfunc, exs.what());
	    status = AM_FAILURE;
	}
	catch (...) {
	    am_web_log_error("%s: Unknown exception encountered.",thisfunc);
	    status = AM_FAILURE;
	}
    }
    return status;
}


/*
 * This function resets cookies for the cookies
 * specified in the configuration file and the ldap attributes cookies if
 * configured, and invokes the set action that caller
 * (i.e. the agent) passes in for each of them.
 * The reset (callback) is called for each cookie.
 * If any cookie reset function fails the last failed status is returned.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_do_cookies_reset(am_status_t (*setFunc)(const char *, void **),
                        void **args, void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    am_status_t status = AM_SUCCESS;
    am_status_t tmpStatus = AM_SUCCESS;

    // Reset cookies from properties file.
    if ((*agentConfigPtr)->cookie_reset_enable == AM_TRUE) {
        tmpStatus  = am_web_reset_cookies_list(
                               &(*agentConfigPtr)->cookie_list,
                               setFunc, args);
        if (AM_SUCCESS != tmpStatus) {
            status = tmpStatus;
        }
    }
    tmpStatus = am_web_reset_ldap_attribute_cookies(setFunc,
                              args, agent_config);
    if (AM_SUCCESS != tmpStatus) {
        status = tmpStatus;
    }
    
    return status;
}

/*
 * This function resets the cookies to be reset on logout as specified
 * in the property.
 * The reset (callback) is reset for every cookie configured.
 * If any cookie reset function fails the last failed status is returned.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_logout_cookies_reset(am_status_t (*setFunc)(const char *, void **),
                            void **args, 
                            void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "am_web_logout_cookies_reset()";
    am_status_t status = AM_SUCCESS;
    if (NULL != (*agentConfigPtr)->logout_cookie_reset_list.list &&
        (*agentConfigPtr)->logout_cookie_reset_list.size >= 0) {
	am_web_log_debug("%s: Resetting logout cookies upon logout.",
			thisfunc);
        status = am_web_reset_cookies_list(
                     &(*agentConfigPtr)->logout_cookie_reset_list,
                     setFunc, args);
    }
    else {
	am_web_log_debug("%s: No cookies to be reset upon logout.",
			thisfunc);
    }
    return status;
}


extern "C" AM_WEB_EXPORT boolean_t
am_web_is_url_enforced(const char *url_str,
                       const char *path_info,
                       const char *client_ip,
                       void* agent_config)
{
    const char *thisfunc = "am_web_is_url_enforced()";
    am_status_t status = AM_SUCCESS;
    am_bool_t isNotEnforced = AM_FALSE;
    boolean_t isEnforced = B_TRUE;
    const char *url = NULL;
    std::string normalizedURL;
    std::string pInfo;
    
    // Normalized the url
    const char *pQuery = NULL;
    std::string originalPathInfo("");
    std::string originalQuery("");

    // The query and path info are saved here because
    // get_normalized_url() will set them to null if
    // ignore_path_info is true
    pQuery = strchr(url_str,'?');
    if (pQuery != NULL) {
        originalQuery.append(pQuery);
    }

    if (path_info != NULL) {
        originalPathInfo.append(path_info);
    }      

    status = get_normalized_url(url_str, path_info,
                                normalizedURL, pInfo, agent_config);
    if (status == AM_SUCCESS) {
        url = normalizedURL.c_str();
        if (url == NULL || *url == '\0') {
            am_web_log_warning("%s: Normalized url is null (original url=%s)",
                                thisfunc, url_str);
        } else {
            // Check if the url is enforced
            isNotEnforced = is_url_not_enforced(url, client_ip, originalPathInfo,
                            originalQuery, agent_config);
            if (isNotEnforced == AM_TRUE) {
                isEnforced = B_FALSE;
            }
        }
    } else {
        am_web_log_warning("%s: get_normalized_url() failed for url %s",
                            thisfunc, url_str);
    }
    return isEnforced;
}
/*
 * This function sets the iPlanetDirectoryPro cookie for each domain
 * configured in the com.sun.am.policy.agents.cookieDomainList property.
 * It builds the set-cookie header for each domain specified in the
 * property, and calls the callback function 'setFunc' in the first
 * argument to actually set the cookie.
 * This function is called by am_web_check_cookie_in_query() and
 * am_web_check_cookie_in_post() which are called in CDSSO mode
 * to set the iPlanetDirectoryPro cookie in the cdsso response.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_do_cookie_domain_set(am_status_t (*setFunc)(const char *, void **),
			    void **args, 
                            const char *cookie_val, 
                            void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "am_web_do_cookie_domain_set()";
    am_status_t status = AM_SUCCESS;
    am_status_t setStatus = AM_SUCCESS;

    Utils::cookie_info_t cookieInfo;
    cookieInfo.name = const_cast<char*>((*agentConfigPtr)->cookie_name);
    cookieInfo.value = const_cast<char*>(cookie_val);
    cookieInfo.domain = NULL;
    // This must be null to work with older browsers
    // netscape 4.79, IE 5.5, mozilla < 1.4.
    cookieInfo.max_age = NULL;
    cookieInfo.path = const_cast<char*>("/");
    cookieInfo.isSecure = (*agentConfigPtr)->is_cookie_secure;

    try {
	std::set<std::string> *cookie_domains = (*agentConfigPtr)->cookie_domain_list;
	if (NULL == cookie_domains || cookie_domains->size() <= 0) {
	    const char *cookie_header = buildSetCookieHeader(&cookieInfo);
	    am_web_log_debug("am_web_do_cookie_domain_set(): "
			     "setting cookie %s.", cookie_header);
	    status = setFunc(cookie_header, args);
	    free((void *)cookie_header);
	}
	else {
	    std::set<std::string>::iterator iter;
	    for (iter=cookie_domains->begin();
		 iter != cookie_domains->end(); iter++) {
		cookieInfo.domain = const_cast<char*>((*iter).c_str());
		const char *cookie_header = buildSetCookieHeader(&cookieInfo);
		am_web_log_debug("am_web_do_cookie_domain_set(): "
				 "setting cookie %s.", cookie_header);
		setStatus = setFunc(cookie_header, args);
		if (AM_SUCCESS != status) {
		    status = setStatus;
		}
		free((void *)cookie_header);
	    }
	}
    }
    catch (std::bad_alloc& exb) {
	status = AM_NO_MEMORY;
    }
    catch (std::exception& exs) {
	am_web_log_error("%s: Exception encountered: %s.", thisfunc,exs.what());
	status = AM_FAILURE;
    }
    catch (...) {
	am_web_log_error("%s: Unknown exception encountered.",thisfunc);
	status = AM_FAILURE;
    }
    return status;
}

/*
 * NOTE - This function is deprecated. It is replaced by
 * am_web_result_attr_map_set() below and is here only for backwards
 * compatibility.
 *
 * This function process attr_profile_map of am_policy_result_t
 * and perform appropriate action that caller (i.e. agent) pass in
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_do_result_attr_map_set(am_policy_result_t *result,
			      am_status_t (*setFunc)(const char *,
						     const char *, void **),
			      void **args, 
                              void* agent_config)
{
	return am_web_result_attr_map_set(result,
					  setFunc,
					  NULL,
					  NULL,
					  NULL,
					  args, agent_config);
}

/*
 * This function process attr_profile_map of am_policy_result_t
 * and perform appropriate action that caller (i.e. agent) pass in
 */

extern "C" AM_WEB_EXPORT am_status_t
am_web_result_attr_map_set(
	am_policy_result_t *result,
	am_status_t (*setFunc)(const char *, const char *, void **),
	am_status_t (*set_cookie_in_response)(const char *, void **),
	am_status_t (*set_header_attr_as_cookie)(const char *, void **),
	am_status_t (*getCookieFunc)(const char * ,char **, void **),
	void **args, 
        void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "am_web_result_attr_map_set()";
    am_status_t retVal = AM_SUCCESS;

    am_map_t attrMap = NULL;
    const char* mode  = "NONE";

    if(result == NULL || setFunc == NULL) {
       retVal = AM_INVALID_ARGUMENT;
    }
    else if ((result->attr_profile_map == AM_MAP_NULL) &&
             (result->attr_session_map == AM_MAP_NULL) &&
             (result->attr_response_map == AM_MAP_NULL)) {
                am_web_log_info("%s: No profile or session or response"
                                " attributes to be set as headers or cookies",
                                thisfunc);
    }
    else {
        for (int i=0; i<3; i++) {
            attrMap = NULL;         /* clear it,  since the switch does not*/
	      switch (i) {
		  case 0:
		       if (result->attr_profile_map != NULL) {
		           attrMap = result->attr_profile_map;
		           mode = (*agentConfigPtr)->profileMode;
                       }
		       break;
		  case 1:
		       if (result->attr_session_map != NULL) {
		           attrMap = result->attr_session_map;
		           mode = (*agentConfigPtr)->sessionMode;
                       }
		       break;
		  case 2:
		       if (result->attr_response_map != NULL) {
		           attrMap = result->attr_response_map;
		           mode = (*agentConfigPtr)->responseMode;
                       }
		       break;
                  default:
		       break;
              }

	if (attrMap != NULL) {
          try {
             // set attributes as headers
	     if (!strcasecmp(mode, AM_POLICY_SET_ATTRS_AS_HEADER)) {
                const KeyValueMap &headerAttrs =
                      *(reinterpret_cast<const KeyValueMap *>
                          (attrMap));
                am_web_log_max_debug("%s: Now setting %u header attributes.",
                          thisfunc, headerAttrs.size());
                KeyValueMap::const_iterator iter_header = headerAttrs.begin();
                for(;(iter_header != headerAttrs.end()) &&
                                (retVal == AM_SUCCESS); iter_header++) {
                  std::string values;
                  am_status_t set_sts = AM_SUCCESS;
                  const KeyValueMap::key_type &keyRef = iter_header->first;
                  char str[2048];
                  char * new_str = NULL;
                  unsigned int new_str_size = 0;
                  unsigned int new_str_free = 0;

                  Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
                    "%s: User attribute is %s.", thisfunc, keyRef.c_str());

                  // Clear the header
                  std::string hdr_or_cookie_name_s(keyRef.c_str());
                  const char * hdr_or_cookie_name =
                                             hdr_or_cookie_name_s.c_str();
                  if (setFunc != NULL) {
                     set_sts = setFunc(hdr_or_cookie_name, NULL, args);
                     if (set_sts != AM_SUCCESS) {
                         am_web_log_warning("%s: Error %s clearing header %s",
                                            thisfunc,
                                            am_status_to_string(set_sts),
                                            hdr_or_cookie_name);
                     }
                   }

                   const KeyValueMap::mapped_type &valueRef =
                                                      iter_header->second;
                   am_web_log_max_debug("%s: Iterating over %u values.",
                                        thisfunc, valueRef.size());

                   for(std::size_t i1 = 0; i1 < valueRef.size(); ++i1) {
                      values.append(valueRef[i1]);
                      PUSH_BACK_CHAR(values, ',');
                   }

                   /* we say > 1 below becoz, the last extra ',' is at
                    * least one char.  so we need more that that to
                    * set header.
                    */
                    if(values.size() > 1) {
                      std::size_t t = values.rfind(',');
                      values.erase(t);
                      Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
                               "%s: Calling container-specific header setter "
                               "function. ", thisfunc);
                      if (values.c_str() != NULL) {
                         new_str_size = (strlen(values.c_str())+1)*4;
                         if (new_str_size  > 2048) {
                            new_str_free = 1;
                            new_str = (char *) malloc (new_str_size);
                         } else {
                            new_str = str;
                         }
                       }
                       if ((*agentConfigPtr)->convert_mbyte == AM_TRUE ) {
                           Log::log(boot_info.log_module,
                                    Log::LOG_MAX_DEBUG,
                                    "i18n encoding in native ");
                           if (new_str != NULL) {
                               mbyte_to_wchar(values.c_str(), new_str,
                                              new_str_size);
                               retVal = setFunc(keyRef.c_str(), new_str, args);
                           } else {
                               am_web_log_error("%s: failed to allocate"
                                                "%d bytes for new_str",
                                                new_str_size);
                          }
                       } else {
                              Log::log(boot_info.log_module,
                                       Log::LOG_MAX_DEBUG,
                                       "i18n encoding in utf-8 ");
                              retVal = setFunc(keyRef.c_str(), values.c_str(),
                                               args);
                      }
                     }
                     if (new_str_free == 1) {
                        free(new_str);
                     }
                   }
                }

		// set attributes as cookies
	        if (!strcasecmp(mode, AM_POLICY_SET_ATTRS_AS_COOKIE)) {
                  const KeyValueMap &cookieAttrs =
                         *(reinterpret_cast<const KeyValueMap *>
                           (attrMap));

                  am_web_log_max_debug("%s: Now setting %u cookie attributes.",
                                        thisfunc, cookieAttrs.size());
                  KeyValueMap::const_iterator iter_cookie =
                                                    cookieAttrs.begin();
                  for(;(iter_cookie != cookieAttrs.end()) &&
                            (retVal == AM_SUCCESS); iter_cookie++) {
                    std::string values;
                    am_status_t set_sts = AM_SUCCESS;
                    const KeyValueMap::key_type &keyRef = iter_cookie->first;
                    Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
                             "%s: User attribute is %s.", thisfunc,
                             keyRef.c_str());
                    std::string hdr_or_cookie_name_s(keyRef.c_str());
                    const char * hdr_or_cookie_name =
                                 hdr_or_cookie_name_s.c_str();

                    // Clear that cookie
                    char *cookie_hdr = NULL;
                    char *cookie_header =  NULL;
                    Utils::cookie_info_t clear_cookie;
                    std::string c_name((*agentConfigPtr)->attrCookiePrefix);
                    Utils::cookie_info_t attr_cookie;
                    std::string cookie_name((*agentConfigPtr)->attrCookiePrefix);
                    c_name.append(hdr_or_cookie_name);
                    clear_cookie.name = const_cast<char*>(c_name.c_str());
                    clear_cookie.value = const_cast<char*>("");
                    clear_cookie.domain = NULL;
                    clear_cookie.max_age = (char *)"0";
                    clear_cookie.path = const_cast<char*>("/");
                    cookie_hdr = (char*)(buildSetCookieHeader(&clear_cookie));

                    if (set_cookie_in_response != NULL) {
                      set_sts =  set_cookie_in_response(cookie_hdr, args);
                      if (set_sts != AM_SUCCESS) {
                         am_web_log_warning("%s: Error %s clearing "
                                            "cookie %s in response header",
                                            thisfunc,
                                            am_status_to_string(set_sts),
                                            hdr_or_cookie_name);
                      }
                    }
                    if (set_header_attr_as_cookie != NULL) {
                       set_sts = set_header_attr_as_cookie(cookie_hdr,args);
                      if (set_sts != AM_SUCCESS) {
                         am_web_log_warning("%s: Error %s clearing "
                                            "cookie %s in request headers",
                                            thisfunc,
                                            am_status_to_string(set_sts),
                                            hdr_or_cookie_name);
                      }
                    }
                    if (cookie_hdr != NULL) {
                       free(cookie_hdr);
                    }

                    // Set the new value to the cookie
                    const KeyValueMap::mapped_type &valueRef =
                                                   iter_cookie->second;
                    am_web_log_max_debug("%s: Iterating over %u values.",
                                                  thisfunc, valueRef.size());
                    for(std::size_t i2 = 0; i2 < valueRef.size(); ++i2) {
                       values.append(valueRef[i2]);
                       PUSH_BACK_CHAR(values, ',');
                    }

                    /* we say > 1 below becoz, the last extra ',' is at least
                     * one char.  so we need more that that to set header.
                     */
                    if(values.size() > 1) {
                       std::size_t t = values.rfind(',');
                       values.erase(t);
                       Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
                               "%s: Calling container-specific cookie setter "
                               "function. %d", thisfunc);

                       cookie_name.append(const_cast<char*>(keyRef.c_str()));
                       attr_cookie.name =
                            const_cast<char*>(cookie_name.c_str());

                       std::string encoded_values;
                       if ((*agentConfigPtr)->encodeCookieSpecialChars == AM_TRUE ) {
                           encoded_values = Http::cookie_encode(values);
                           attr_cookie.value = (char*)encoded_values.c_str();
                       } else {
                           attr_cookie.value = (char*)values.c_str();
                       }

                       attr_cookie.domain = NULL;
                       attr_cookie.max_age =
                           const_cast<char*>((*agentConfigPtr)->attrCookieMaxAge);
                       attr_cookie.path = const_cast<char*>("/");
                       cookie_header =
                           (char*)(buildSetCookieHeader(&attr_cookie));
                       if (cookie_header != NULL) {
                          if (set_cookie_in_response != NULL) {
                             retVal =  set_cookie_in_response(cookie_header,
                                                              args);
                          } else {
                             Log::log(boot_info.log_module, Log::LOG_INFO,
                                      "%s: response header setting function "
                                      "is NULL", thisfunc);
                          }
                          if (set_header_attr_as_cookie != NULL) {
                             retVal = set_header_attr_as_cookie(cookie_header,
                                                                args);
                          } else {
                             Log::log(boot_info.log_module, Log::LOG_INFO,
                                      "%s: request header setting function "
                                      "is NULL", thisfunc);
                          }
                          free(cookie_header);
                        }
                       } else {
                          am_web_log_debug("%s:No values found for "
                                     "attribute:%s",thisfunc, keyRef.c_str());
                       }
                     }
                 }
             } catch (std::bad_alloc& exb) {
                  retVal = AM_NO_MEMORY;
             } catch (std::exception& exs) {
                  am_web_log_error("%s: Exception encountered: %s.", thisfunc,
                                   exs.what());
                  retVal = AM_FAILURE;
             } catch (...) {
                  am_web_log_error("%s: Unknown exception encountered.",
                                   thisfunc);
                  retVal = AM_FAILURE;
             }
          } else {
              am_web_log_debug("%s: Attribute map set is empty. "
                               "No attribute set as cookie or header",thisfunc);
	  }
       }

       if (retVal == AM_SUCCESS) {
           Log::log(boot_info.log_module, Log::LOG_DEBUG,
                    "%s: Successfully set all attributes.", thisfunc);
       } else {
           Log::log(boot_info.log_module, Log::LOG_ERROR,
                    "%s: Error while setting attributes: %s",
                    thisfunc, am_status_to_string(retVal));
       }
    
    }
    return retVal;
}

extern "C" AM_WEB_EXPORT const char *
am_web_get_user_id_param(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    return (*agentConfigPtr)->user_id_param;
}

extern "C" AM_WEB_EXPORT const char *
am_web_get_cookie_name(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    return (*agentConfigPtr)->cookie_name;
}

extern "C" AM_WEB_EXPORT size_t
am_web_get_cookie_name_len(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    return (*agentConfigPtr)->cookie_name_len;
}

extern "C" AM_WEB_EXPORT const char *
am_web_get_notification_url(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    return (*agentConfigPtr)->notification_url;
}

extern "C" AM_WEB_EXPORT const char *
am_web_get_agent_server_host(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    return (*agentConfigPtr)->agent_server_url.host;
}

extern "C" AM_WEB_EXPORT int
am_web_get_agent_server_port(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


   return (*agentConfigPtr)->agent_server_url.port;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_in_not_enforced_list(const char *url, 
                               const char *path_info,
                               void* agent_config) {
    boolean_t retVal = B_FALSE;
    try {
	std::string pInfo;
	if(path_info != NULL) pInfo = path_info;

	URL urlObj(url, pInfo);
	if(in_not_enforced_list(urlObj, agent_config) == AM_TRUE)
	    retVal = B_TRUE;
    } catch(InternalException &iex) {
	Log::log(boot_info.log_module, Log::LOG_ERROR, iex);
    } catch(std::exception &ex) {
	Log::log(boot_info.log_module, Log::LOG_ERROR, ex);
    } catch(...) {
	Log::log(boot_info.log_module, Log::LOG_ERROR,
		 "am_web_is_in_not_enforced_list(): "
		 "Unknown exception encountered.");
    }
    return retVal;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_in_not_enforced_ip_list(const char *ip, 
                                  void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    boolean_t retVal = B_FALSE;
    if(ip != NULL && (*agentConfigPtr)->not_enforce_IPAddr != NULL &&
       (*agentConfigPtr)->not_enforce_IPAddr->size() > 0) {
	retVal = ((*agentConfigPtr)->not_enforce_IPAddr->find(ip) !=
		  (*agentConfigPtr)->not_enforce_IPAddr->end())?B_TRUE:B_FALSE;
    }
    return retVal;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_valid_fqdn_url(const char *url,
                         void* agent_config)
{
    boolean_t ret = B_FALSE;
    try {
	ret = (AM_TRUE==is_valid_fqdn_access(url, agent_config)) ? B_TRUE : B_FALSE;
    }
    catch (...) {
	am_web_log_error("am_web_is_valid_fqdn_url(): "
			 "Unknown exception encountered.");
	ret = B_FALSE;
    }
    return ret;
}


/**
 *                   --------- logging --------
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_debug_on()
{
    return am_log_is_level_enabled(boot_info.log_module, AM_LOG_DEBUG);
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_max_debug_on()
{
    return am_log_is_level_enabled(boot_info.log_module,
				      AM_LOG_MAX_DEBUG);
}

extern "C" AM_WEB_EXPORT void
am_web_log_always(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(boot_info.log_module, AM_LOG_ALWAYS, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_log_auth(am_web_access_t accessType, 
                const char *fmt, 
                void* agent_config, ...)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    va_list args;

    va_start(args, fmt);
    switch(accessType) {
    case AM_ACCESS_DENY:
        if (strcasecmp((*agentConfigPtr)->authLogType_param, "LOG_DENY") == 0 ||
              strcasecmp((*agentConfigPtr)->authLogType_param, "LOG_BOTH") == 0) {
            am_log_vlog((*agentConfigPtr)->remote_LogID, AM_LOG_AUTH_REMOTE, fmt, args);
        }
        break;
    case AM_ACCESS_ALLOW:
        if (strcasecmp((*agentConfigPtr)->authLogType_param, "LOG_ALLOW") == 0 ||
              strcasecmp((*agentConfigPtr)->authLogType_param, "LOG_BOTH") == 0) {
            am_log_vlog((*agentConfigPtr)->remote_LogID, AM_LOG_AUTH_REMOTE, fmt, args);
        }
        break;
    }
    va_end(args);
    return B_TRUE;
}


extern "C" AM_WEB_EXPORT void
am_web_log_error(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(boot_info.log_module, AM_LOG_ERROR, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT void
am_web_log_warning(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(boot_info.log_module, AM_LOG_WARNING, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT void
am_web_log_info(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(boot_info.log_module, AM_LOG_INFO, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT void
am_web_log_debug(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(boot_info.log_module, AM_LOG_DEBUG, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT void
am_web_log_max_debug(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(boot_info.log_module, AM_LOG_MAX_DEBUG, fmt, args);
    va_end(args);
}

/*
 * ----------------------- POST -----------------------------------
*/

/**
 *  Method to find if POST preservation is enabled or not
*/

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_postpreserve_enabled(void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;



    return static_cast<bool>(
	    (*agentConfigPtr)->postdatapreserve_enable)==true?B_TRUE:B_FALSE;
}

/**
 *  Method to insert POST data in the POST hash table
*/

extern "C" AM_WEB_EXPORT boolean_t
am_web_postcache_insert(const char *key,
			const am_web_postcache_data_t *value,
                        void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    boolean_t ret = B_FALSE;
    PostCacheEntryRefCntPtr postEntry;
    if((*agentConfigPtr)->postcache_handle != NULL) {
	try {
	    postEntry = new PostCacheEntry(value->value, value->url);
	    ret = (*agentConfigPtr)->postcache_handle->post_hash_insert(key,postEntry)?
		   B_TRUE:B_FALSE;
	} catch (...) {
	    am_web_log_error("am_web_postcache_insert(): "
			     "Unknown exception encountered.");
	    ret = B_FALSE;
	}
    }
    return ret;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_postcache_lookup(const char *key,
		        am_web_postcache_data_t *postdata_entry,
                        void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "am_web_postcache_lookup()";
    PostCacheEntryRefCntPtr value;
    boolean_t ret = B_FALSE;

    if((*agentConfigPtr)->postcache_handle  != NULL) {
	try {
	    value =  (*agentConfigPtr)->postcache_handle->post_hash_get(key);
	    if(value != NULL) {
	        postdata_entry->value = strdup(value->getPostData());
	        postdata_entry->url = strdup(value->getDestUrl());
		ret = B_TRUE;
	    }
	}
	catch (std::bad_alloc& exb) {
	    am_web_log_error("%s: Bad Alloc exception encountered: %s.",
			     thisfunc, exb.what());
	    ret = B_FALSE;
	}
	catch (std::exception& exs) {
	    am_web_log_error("%s: Exception encountered: %s.",
			     thisfunc, exs.what());
	    ret = B_FALSE;
	}
	catch (...) {
	    am_web_log_error("%s: Unknown exception encountered.",thisfunc);
	    ret = B_FALSE;
	}
    }
    return ret;
}

/**
  * Method to remove POST data in the POST hash table
*/

extern "C" AM_WEB_EXPORT void
am_web_postcache_remove(const char *key, 
                        void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "am_web_postcache_remove()";
    try {
	 if((*agentConfigPtr)->postcache_handle != NULL) {
	     (*agentConfigPtr)->postcache_handle->post_hash_remove(key);
	 }
    }
    catch (std::bad_alloc& exb) {
	am_web_log_error("%s: Bad Alloc exception encountered: %s.",
			 thisfunc, exb.what());
    }
    catch (std::exception& exs) {
	am_web_log_error("%s: Exception encountered: %s.",
			 thisfunc, exs.what());
    }
    catch (...) {
	am_web_log_error("%s: Unknown exception encountered.",thisfunc);
    }
}

/**
  * Method to convert PRTime to string
*/

static char *prtime_to_string(char *buffer, 
                              size_t buffer_len, 
                              void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    PRTime timestamp;
    PRExplodedTime exploded_time;
    int n_written = 0;

    PR_Lock((*agentConfigPtr)->lock);
    timestamp = PR_Now();
    PR_Unlock((*agentConfigPtr)->lock);

    PR_ExplodeTime(timestamp, PR_LocalTimeParameters, &exploded_time);
    n_written = snprintf(buffer, buffer_len, "%d-%02d-%02d%02d:%02d:%02d.%03d",
                         exploded_time.tm_year, exploded_time.tm_month+1,
			 exploded_time.tm_mday, exploded_time.tm_hour,
			 exploded_time.tm_min, exploded_time.tm_sec,
			 exploded_time.tm_usec / 1000);
    if (buffer_len > 0 && n_written < 1) {
        // This means the buffer wasn't big enough or an error occured.
        *buffer = '\0';
    }

    return buffer;
}

/**
  * Method to cleanup internal data struct that has
  * destination url, dummy url, post key
*/

extern "C" AM_WEB_EXPORT void
am_web_clean_post_urls(post_urls_t *posturl_struct)
{
    if (posturl_struct->action_url != NULL) {
	free(posturl_struct->action_url);
	posturl_struct->action_url = NULL;
    }
    if (posturl_struct->dummy_url != NULL) {
	free(posturl_struct->dummy_url);
	posturl_struct->dummy_url = NULL;
    }
    if (posturl_struct->post_time_key != NULL) {
	free(posturl_struct->post_time_key);
	posturl_struct->post_time_key = NULL;
    }
    if (posturl_struct != NULL) {
	free(posturl_struct);
	posturl_struct = NULL;
    }
}

/**
  * Method to cleanup POST data structure
*/

extern "C" AM_WEB_EXPORT void
am_web_postcache_data_cleanup(am_web_postcache_data_t * const postentry_struct) {
    if(postentry_struct != NULL){
	if (postentry_struct->value != NULL) {
	    free(postentry_struct->value);
	    postentry_struct->value = NULL;
	}
	if (postentry_struct->url != NULL) {
	    free(postentry_struct->url);
	    postentry_struct->url = NULL;
	}
    }
}

/**
  * Once the POST call has been registered, the dummy URL with the magic
  * query string needs to be constructed for a goto="dummypost.htm"
  * This is a helper function and it will be shared with all agents
*/

extern "C" AM_WEB_EXPORT am_status_t
am_web_create_post_preserve_urls(const char *request_url, 
                                 post_urls_t **url_data,
                                 void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    const char *thisfunc = "am_web_create_post_preserve_urls()";
    am_status_t status = AM_SUCCESS;
    char *time_str = NULL;
    std::string key;
    post_urls_t *url_data_tmp = (post_urls_t *)malloc (sizeof(post_urls_t));
    const char *stickySessionValue = 
            (*agentConfigPtr)->postdatapreserve_sticky_session_value;

    if (request_url == NULL) {
        am_web_log_error("%s: request_url is NULL", thisfunc);
        status = AM_INVALID_ARGUMENT;
    }
    // Get the time stamp
    if (status == AM_SUCCESS) {
        time_str = (char *) malloc (AM_WEB_MAX_POST_KEY_LENGTH);
        if (time_str != NULL) {
            prtime_to_string(time_str,AM_WEB_MAX_POST_KEY_LENGTH, agent_config);
        } else {
            am_web_log_error("%s: Failed to allocate time_str.", thisfunc);
            status = AM_NO_MEMORY;
        }
    }
    // Build the key
    if (status == AM_SUCCESS) {
        std::string agentID, stickySessionValueStr;
        size_t equalPos = 0;
        char uniqueNumber[5];
        key.assign(time_str);
        // Add the agent id (if using a LB) to the key
        if ((stickySessionValue != NULL) && (strlen(stickySessionValue) > 0))
        {
            stickySessionValueStr.assign(stickySessionValue);
            equalPos=stickySessionValueStr.find('=');
            if (equalPos != std::string::npos) {
                agentID = stickySessionValueStr.substr(equalPos+1);
                if (!agentID.empty()) {
                    key.append(".").append(agentID);
                }
            }
        }
        // Add a number to the key to make sure it is unique.
        // To prevent this number to get too big,
        // reset it when it reaches 5000 (there should not be more
        // than 5000 requests in the same millisecond)
        postDataPreserveKey++;
        if (postDataPreserveKey == 5000) {
            postDataPreserveKey = 1;
        }
        sprintf(uniqueNumber, "%d", postDataPreserveKey);
        key.append(".").append(uniqueNumber);
        url_data_tmp->post_time_key = (char *)strdup(key.c_str());
        if (url_data_tmp->post_time_key == NULL) {
            am_web_log_error("%s: Failed to allocate url_data_tmp->post_time_key.",
                             thisfunc);
            status = AM_NO_MEMORY;
        }
        free(time_str);
        time_str = NULL;
    }
    
    // Build the dummy URL
    if (status == AM_SUCCESS) {
        std::string dummyURL;
        char *stickySessionFromUrl = NULL;
        URL urlObject(request_url);
        urlObject.getRootURL(dummyURL);
        dummyURL.append(DUMMY_REDIRECT).append(MAGIC_STR).append(key);
        // Add the sticky session parameter if a LB is used with sticky
        // session mode set to URL.
        if (am_web_get_postdata_preserve_URL_parameter(
                            &stickySessionFromUrl, agent_config)
                            == AM_SUCCESS)
        {
            dummyURL.append("?").append(stickySessionFromUrl);
        }
        url_data_tmp->dummy_url = (char *)strdup(dummyURL.c_str());
        if (url_data_tmp->dummy_url == NULL) {
            am_web_log_error("%s: Failed to allocate url_data_tmp->dummy_url.",
                             thisfunc);
            status = AM_NO_MEMORY;
        }
        if (stickySessionFromUrl != NULL) {
            free(stickySessionFromUrl);
            stickySessionFromUrl = NULL;
        }
    }
    // Set the action URL
    if (status == AM_SUCCESS) {
        url_data_tmp->action_url = (char *)strdup(request_url);
        if (url_data_tmp->action_url == NULL) {
            am_web_log_error("%s: Failed to allocate "
                             "url_data_tmp->action_url.", thisfunc);
            status = AM_NO_MEMORY;
        }
    }
    if (status == AM_SUCCESS) {
        *url_data = url_data_tmp;
        am_web_log_info("%s: url_data->post_time_key: %s", 
                        thisfunc, url_data_tmp->post_time_key);
        am_web_log_info("%s: url_data->dummy_url: %s", 
                        thisfunc, url_data_tmp->dummy_url);
        am_web_log_info("%s: url_data->action_url: %s", 
                        thisfunc, url_data_tmp->action_url);
    } else {
        am_web_log_error("%s: Failed to build url_data.", thisfunc);
    }
    return status;
}

static char* escapeQuotationMark(char*& ptr)
{
    std::string valueStr(ptr);

    if(ptr && strchr(ptr,'"')) {
#if defined(_AMD64_)
       size_t pos  = 0;
#else
       int pos  = 0;
#endif

       while((pos = valueStr.find('"',pos)) != std::string::npos) {
          valueStr.erase(pos,1);
          valueStr.insert(pos,"&quot;");
       }

       free(ptr);
       ptr = strdup(valueStr.c_str());
    }

    return ptr;
}

/**
  * Create a data structure for the encoded post data, helper function
  * to create the html page
*/
Utils::post_struct_t *
split_post_data(const char * test_string)
{
    const char *thisfunc = "split_post_data()";
    char * str = NULL;
    char * ptr = NULL;
    std::size_t i = 0;
    unsigned int num_sectors = 0;
    Utils::post_struct_t *post_data =
        (Utils::post_struct_t *) malloc(sizeof(Utils::post_struct_t) * 1);

    //Create the tokens with name value pair separated with "&"
    char *postValue = strdup(test_string);
    ptr = strchr(postValue, '&');
    num_sectors = 1;
    while (ptr != NULL) {
        num_sectors += 1;
        ptr = strchr(ptr + 1, '&');
    }
    if(post_data != NULL){
        post_data->namevalue = (Utils::name_value_pair_t *)
                malloc (sizeof(Utils::name_value_pair_t) * num_sectors);
        post_data->count = num_sectors;
        // Parse the name value pair in a structure in one pass
        if(post_data->namevalue != NULL) {
            ptr = postValue;
            for(i = 0; i < num_sectors-1; ++i){
                post_data->namevalue[i].name = ptr;
                ptr = strchr(ptr,'&');
                *ptr = '\0';
                ptr += 1;
                post_data->namevalue[i].value =
                       strchr(post_data->namevalue[i].name, '=');
                *(post_data->namevalue[i].value++) = '\0';
                post_data->namevalue[i].name = 
                       am_web_http_decode(post_data->namevalue[i].name,
                                          strlen(post_data->namevalue[i].name));
                post_data->namevalue[i].value = 
                        am_web_http_decode(post_data->namevalue[i].value,
                                           strlen(post_data->namevalue[i].value));
                escapeQuotationMark(post_data->namevalue[i].name);
                escapeQuotationMark(post_data->namevalue[i].value);
            }
            post_data->namevalue[i].name = ptr;
            post_data->namevalue[i].value = 
                      strchr(post_data->namevalue[i].name, '=');
            *(post_data->namevalue[i].value++) = '\0';
            post_data->namevalue[i].name = 
                    am_web_http_decode(post_data->namevalue[i].name,
                                       strlen(post_data->namevalue[i].name));
            post_data->namevalue[i].value = 
                    am_web_http_decode(post_data->namevalue[i].value,
                                       strlen(post_data->namevalue[i].value));
            escapeQuotationMark(post_data->namevalue[i].name);
            escapeQuotationMark(post_data->namevalue[i].value);
        }
        post_data->buffer = str;
        am_web_log_max_debug("%s: post value = %s", thisfunc, post_data->buffer);
    }
    return post_data;
}

/**
  * Create the html form with the javascript that does the post with the
  * invisible name value pairs
*/
extern "C" AM_WEB_EXPORT char *
am_web_create_post_page(const char *key, 
                        const char *postdata, 
                        const char *actionurl,
                        void* agent_config)
{
    const char *thisfunc = "am_web_create_post_page()";
    char *buffer_page = NULL;
    int num_sectors = 0;
    int i =0;
    size_t totalchars = 0;
    Utils::post_struct_t *post_data = split_post_data(postdata);
    num_sectors = post_data->count;

    // Find the total length required to construct the name value fragment
    // of the page
    for(i = 0; i < num_sectors; ++i){
        totalchars += strlen(post_data->namevalue[i].name);
        totalchars += strlen(post_data->namevalue[i].value);
        totalchars += strlen(sector_two) + strlen(sector_three)
                      + strlen(sector_four);
    }
    // Allocate the length of the buffer
    buffer_page = (char *)malloc(strlen(sector_one) + strlen(actionurl) +
                  strlen(sector_two) +
                  totalchars + strlen(sector_five) + 1);
    strcpy(buffer_page,sector_one);
    strcat(buffer_page,actionurl);
    strcat(buffer_page,sector_two);
    // Copy in the variable part, the name value pair..
    for(i = 0; i < num_sectors; i++){
        strcat(buffer_page,sector_three);
        strcat(buffer_page,post_data->namevalue[i].name);
        strcat(buffer_page,sector_four);
        strcat(buffer_page,post_data->namevalue[i].value);
        strcat(buffer_page,sector_two);
    }
    strcat(buffer_page, sector_five);
    // Now remove the entry from the hashtable
    if(key != NULL){
        am_web_postcache_remove(key, agent_config);
    }
    Log::log(boot_info.log_module, Log::LOG_DEBUG,
             "%s: HTML page for post %s :", thisfunc, buffer_page);
    if(post_data->namevalue != NULL){
        free(post_data->namevalue);
    }
    if(post_data->buffer != NULL){
        free(post_data->buffer);
    }
    if(post_data != NULL){
        free(post_data);
    }

    return buffer_page;
}


extern "C" AM_WEB_EXPORT int
am_web_is_cookie_present(const char *cookie, const char *value,
                               char **new_cookie)
{
    int ret = AM_WEB_COOKIE_ERROR;
    const char *thisfunc = "am_web_is_cookie_present()";
    try {
	std::string new_cookie_name,
		    new_cookie_val,
		    req_cookie_value = value;

	string::size_type eq_pos,
			  sm_pos;

	std::string original_cookie = cookie,
		    prsnt_cookie_val; // value of cookie present in request.

	string::size_type name_pos_in_cookie,
			  val_pos_in_cookie;

#if defined(_AMD64_)
	size_t _new_length = 0;
#else
	unsigned int _new_length = 0;
#endif

	char sep = '=',
	     ln_sep = '\n',
	     val_sep = ';';

	// getting the name and value from the new string
	eq_pos = req_cookie_value.find(sep);
	// check if seperator is present else error
	if (eq_pos == string::npos)
	{
	    return AM_WEB_COOKIE_ERROR;
	}

	// if last cookie value may not contain ";"
	sm_pos = req_cookie_value.find(val_sep,eq_pos);
	if (sm_pos == string::npos)
	{
	    sm_pos = req_cookie_value.find(val_sep,ln_sep);
	}

	// get the cookie name and value from the new string value
	new_cookie_name = req_cookie_value.substr(0,eq_pos+1);
	new_cookie_val  = req_cookie_value.substr(eq_pos+1,sm_pos-eq_pos-1);

	am_web_log_debug("%s: The new_cookie_name:%s, new_cookie_val:%s",
		     thisfunc, new_cookie_name.c_str(), new_cookie_val.c_str());

	// getting the name and value from the request
	name_pos_in_cookie = original_cookie.find(new_cookie_name);

	if (name_pos_in_cookie == string::npos)
	{
	    // cookie name not present append it and send new string;
	    name_pos_in_cookie = original_cookie.find(val_sep,
					  original_cookie.length() - 1);
	    if(name_pos_in_cookie == string::npos)
	    {
		original_cookie.append(";");
	    }
	    original_cookie.append(new_cookie_name);
	    original_cookie.append(new_cookie_val);

	    _new_length = original_cookie.length();
	    *new_cookie = (char *) malloc (_new_length + 1);
	    if (*new_cookie == NULL){
		am_web_log_error("%s: alloc fail %d", thisfunc, _new_length);
		ret = AM_WEB_COOKIE_ERROR;
	    }
	    strcpy(*new_cookie, original_cookie.c_str());
	    ret = AM_WEB_COOKIE_ABSENT;
	} else {

	    val_pos_in_cookie  = original_cookie.find(new_cookie_val,
							name_pos_in_cookie);
	    am_web_log_debug("%s: val_pos_in_cookie %d",
			     thisfunc, val_pos_in_cookie);
	    if (val_pos_in_cookie == string::npos)
	    {
		// logic for if the value is modified
		am_bool_t last_cookie = AM_FALSE;
		am_web_log_debug("%s: value modified", thisfunc);
		string::size_type _end_val = original_cookie.find(val_sep,
						      name_pos_in_cookie);
		if (_end_val == string::npos)
		{
		    _end_val = original_cookie.find(ln_sep,
					       name_pos_in_cookie);
		    last_cookie = AM_TRUE;
		    am_web_log_debug("%s: ln_sep _end_val %d",
					thisfunc, _end_val);
		}
		am_web_log_debug("%s: _end_val %d", thisfunc, _end_val);
		std::string _tmp_str = new_cookie_name;
		_tmp_str += new_cookie_val;

		if (!last_cookie)
		{
		    _tmp_str += ";";
		}
		original_cookie.replace(name_pos_in_cookie,
				     _end_val-name_pos_in_cookie+1, _tmp_str);
		_new_length = original_cookie.length();
		*new_cookie = (char *) malloc (_new_length + 1);
		if(*new_cookie == NULL){
		    am_web_log_error("%s: alloc fail %d", thisfunc,_new_length);
		     ret = AM_WEB_COOKIE_ERROR;
		}
		strcpy(*new_cookie, original_cookie.c_str());
		am_web_log_error("%s: new_cookie:%s", thisfunc, *new_cookie);
		ret = AM_WEB_COOKIE_MODIFIED;
	    } else {

		sm_pos = original_cookie.find(val_sep,val_pos_in_cookie);
		prsnt_cookie_val = original_cookie.substr(val_pos_in_cookie,
				sm_pos-val_pos_in_cookie) ;
		if (prsnt_cookie_val == new_cookie_val)
		{
		    // the cookie is already present.
		    // copy into new_cookie and return AM_WEB_COOKIE_EXIST.
		    _new_length = original_cookie.length();

		    am_web_log_debug("%s: cookie_exist %s",
					thisfunc, original_cookie.c_str());
		    *new_cookie = (char *) malloc(_new_length + 1);
		    if (*new_cookie == NULL){
			am_web_log_error("%s: malloc fail %d",
					  thisfunc, _new_length);
			ret = AM_WEB_COOKIE_ERROR;
		    }
		    strcpy(*new_cookie, original_cookie.c_str());
		    ret = AM_WEB_COOKIE_EXIST;
		} else {
		    // when new value is subset of present value
		    // logic for modification
		   am_bool_t last_cookie = AM_FALSE;
		   am_web_log_debug("%s: value modified", thisfunc);
		   string::size_type _end_val = original_cookie.find(val_sep,
						      name_pos_in_cookie);
		   if (_end_val == string::npos)
		   {
		       _end_val = original_cookie.find(ln_sep,
					       name_pos_in_cookie);
		       last_cookie = AM_TRUE;
		   }
		   am_web_log_debug("%s: _end_val %d", thisfunc, _end_val);
		   std::string _tmp_str = new_cookie_name;
		   _tmp_str += new_cookie_val;

		   if (!last_cookie)
		   {
		       _tmp_str += ";";
		   }
		   original_cookie.replace(name_pos_in_cookie,
				     _end_val-name_pos_in_cookie+1,
				     _tmp_str);
		   _new_length = original_cookie.length();
		   *new_cookie = (char *) malloc (_new_length + 1);
		   if(*new_cookie == NULL){
		       am_web_log_error("%s: malloc fail %d",
					thisfunc, _new_length);
		       ret = AM_WEB_COOKIE_ERROR;
		   }
		   strcpy(*new_cookie, original_cookie.c_str());
		   am_web_log_debug("%s: new_cookie:%s", thisfunc, *new_cookie);
		   ret = AM_WEB_COOKIE_MODIFIED;
		}
	    }
	}
    }
    catch (std::bad_alloc& exb) {
	am_web_log_error("%s: Bad alloc exception encountered: %s.",
			 thisfunc, exb.what());
	ret = AM_WEB_COOKIE_ERROR;
    }
    catch (std::exception& exs) {
	am_web_log_error("%s: Exception encountered: %s.",
			 thisfunc, exs.what());
	ret = AM_WEB_COOKIE_ERROR;
    }
    catch (...) {
	am_web_log_error("%s: Unknown exception encountered.",thisfunc);
	ret = AM_WEB_COOKIE_ERROR;
    }
    return ret;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_logout_url(const char *url,
                     void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    const char *thisfunc = "am_web_is_logout_url";
    boolean_t found = B_FALSE;
    if (NULL != url && '\0' != url[0]) {
	try {
	    // normalize the given url before comparison.
	    URL url_obj(url);
	    // override protocol/host/port if configured.
	    (void)overrideProtoHostPort(url_obj, agent_config);
	    std::string url_str;
	    url_obj.getURLString(url_str);
	    const char *norm_url = url_str.c_str();
	    Log::log(boot_info.log_module, Log::LOG_DEBUG,
		     "%s(%s): normalized URL %s.\n", thisfunc, url, norm_url);
	    unsigned int i;
	    for (i = 0;
		(i < (*agentConfigPtr)->logout_url_list.size) && (B_FALSE == found);
		 i++) {
		am_resource_traits_t rsrcTraits;
		populate_am_resource_traits(rsrcTraits, agent_config);
		am_resource_match_t match_status;
		boolean_t usePatterns =
		    (*agentConfigPtr)->logout_url_list.list[i].has_patterns==AM_TRUE? B_TRUE:B_FALSE;
		match_status = am_policy_compare_urls(
		    &rsrcTraits, (*agentConfigPtr)->logout_url_list.list[i].url, norm_url, usePatterns);
		if (match_status == AM_EXACT_MATCH ||
		    match_status == AM_EXACT_PATTERN_MATCH) {
		    Log::log(boot_info.log_module, Log::LOG_DEBUG,
			"%s(%s): matched '%s' entry in logout url list",
			thisfunc, url, (*agentConfigPtr)->logout_url_list.list[i].url);
		    found = B_TRUE;
		    break;
		}
	    }
	}
	catch (InternalException& exi) {
	    am_web_log_error("%s: Internal exception encountered: %s.",
			     thisfunc, exi.getMessage());
	    found = B_FALSE;
	}
	catch (std::bad_alloc& exb) {
	    am_web_log_error("%s: Bad Alloc exception encountered: %s.",
			     thisfunc, exb.what());
	    found = B_FALSE;
	}
	catch (std::exception& exs) {
	    am_web_log_error("%s: Exception encountered: %s.",
			     thisfunc, exs.what());
	    found = B_FALSE;
	}
	catch (...) {
	    am_web_log_error("%s: Unknown exception encountered.",thisfunc);
	    found = B_FALSE;
	}
    }
    return found;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_agent_logout_url(const char *url,
                     void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    const char *thisfunc = "am_web_is_agent_logout_url";
    boolean_t found = B_FALSE;
    if (NULL != url && '\0' != url[0]) {
	try {
	    // normalize the given url before comparison.
	    URL url_obj(url);
	    // override protocol/host/port if configured.
	    (void)overrideProtoHostPort(url_obj, agent_config);
	    std::string url_str;
	    url_obj.getURLString(url_str);
	    const char *norm_url = url_str.c_str();
	    Log::log(boot_info.log_module, Log::LOG_DEBUG,
		     "%s(%s): normalized URL %s.\n", thisfunc, url, norm_url);
	    unsigned int i;
	    for (i = 0;
		(i < (*agentConfigPtr)->agent_logout_url_list.size) && (B_FALSE == found);
		 i++) {
		am_resource_traits_t rsrcTraits;
		populate_am_resource_traits(rsrcTraits, agent_config);
		am_resource_match_t match_status;
		boolean_t usePatterns =
		    (*agentConfigPtr)->agent_logout_url_list.list[i].has_patterns==AM_TRUE? B_TRUE:B_FALSE;
		match_status = am_policy_compare_urls(
		    &rsrcTraits, (*agentConfigPtr)->agent_logout_url_list.list[i].url, norm_url, usePatterns);
		if (match_status == AM_EXACT_MATCH ||
		    match_status == AM_EXACT_PATTERN_MATCH) {
		    Log::log(boot_info.log_module, Log::LOG_DEBUG,
			"%s(%s): matched '%s' entry in logout url list",
			thisfunc, url, (*agentConfigPtr)->agent_logout_url_list.list[i].url);
		    found = B_TRUE;
		    break;
		}
	    }
	}
	catch (InternalException& exi) {
	    am_web_log_error("%s: Internal exception encountered: %s.",
			     thisfunc, exi.getMessage());
	    found = B_FALSE;
	}
	catch (std::bad_alloc& exb) {
	    am_web_log_error("%s: Bad Alloc exception encountered: %s.",
			     thisfunc, exb.what());
	    found = B_FALSE;
	}
	catch (std::exception& exs) {
	    am_web_log_error("%s: Exception encountered: %s.",
			     thisfunc, exs.what());
	    found = B_FALSE;
	}
	catch (...) {
	    am_web_log_error("%s: Unknown exception encountered.",thisfunc);
	    found = B_FALSE;
	}
    }
    return found;
}


extern "C" AM_WEB_EXPORT const char *
am_web_method_num_to_str(am_web_req_method_t method)
{
    const char *methodName;
    switch(method) {
    case AM_WEB_REQUEST_GET:
	methodName = REQUEST_METHOD_GET;
	break;
    case AM_WEB_REQUEST_POST:
	methodName = REQUEST_METHOD_POST;
	break;
    case AM_WEB_REQUEST_HEAD:
	methodName = REQUEST_METHOD_HEAD;
	break;
    case AM_WEB_REQUEST_PUT:
	methodName = REQUEST_METHOD_PUT;
	break;
    case AM_WEB_REQUEST_DELETE:
	methodName = REQUEST_METHOD_DELETE;
	break;
    case AM_WEB_REQUEST_TRACE:
	methodName = REQUEST_METHOD_TRACE;
	break;
    case AM_WEB_REQUEST_OPTIONS:
	methodName = REQUEST_METHOD_OPTIONS;
	break;
    case AM_WEB_REQUEST_CONNECT:
	methodName = REQUEST_METHOD_CONNECT;
	break;
    case AM_WEB_REQUEST_COPY:
	methodName = REQUEST_METHOD_COPY;
	break;
    case AM_WEB_REQUEST_INVALID:
	methodName = REQUEST_METHOD_INVALID;
	break;
    case AM_WEB_REQUEST_LOCK:
	methodName = REQUEST_METHOD_LOCK;
	break;
    case AM_WEB_REQUEST_UNLOCK:
	methodName = REQUEST_METHOD_UNLOCK;
	break;
    case AM_WEB_REQUEST_MKCOL:
	methodName = REQUEST_METHOD_MKCOL;
	break;
    case AM_WEB_REQUEST_MOVE:
	methodName = REQUEST_METHOD_MOVE;
	break;
    case AM_WEB_REQUEST_PATCH:
	methodName = REQUEST_METHOD_PATCH;
	break;
    case AM_WEB_REQUEST_PROPFIND:
	methodName = REQUEST_METHOD_PROPFIND;
	break;
    case AM_WEB_REQUEST_PROPPATCH:
	methodName = REQUEST_METHOD_PROPPATCH;
	break;
    case AM_WEB_REQUEST_VERSION_CONTROL:
        methodName = REQUEST_METHOD_VERSION_CONTROL;
        break;
    case AM_WEB_REQUEST_CHECKOUT:
        methodName = REQUEST_METHOD_CHECKOUT;
        break;
    case AM_WEB_REQUEST_UNCHECKOUT:
        methodName = REQUEST_METHOD_UNCHECKOUT;
        break;
    case AM_WEB_REQUEST_CHECKIN:
        methodName = REQUEST_METHOD_CHECKIN;
        break;
    case AM_WEB_REQUEST_UPDATE:
        methodName = REQUEST_METHOD_UPDATE;
        break;
    case AM_WEB_REQUEST_LABEL:
        methodName = REQUEST_METHOD_LABEL;
        break;
    case AM_WEB_REQUEST_REPORT:
        methodName = REQUEST_METHOD_REPORT;
        break;
    case AM_WEB_REQUEST_MKWORKSPACE:
        methodName = REQUEST_METHOD_MKWORKSPACE;
        break;
    case AM_WEB_REQUEST_MKACTIVITY:
        methodName = REQUEST_METHOD_MKACTIVITY;
        break;
    case AM_WEB_REQUEST_BASELINE_CONTROL:
        methodName = REQUEST_METHOD_BASELINE_CONTROL;
        break;
    case AM_WEB_REQUEST_MERGE:
        methodName = REQUEST_METHOD_MERGE;
        break;
    case AM_WEB_REQUEST_UNKNOWN:
    default:
	methodName = REQUEST_METHOD_UNKNOWN;
	break;
    }
    return methodName;
}

extern "C" AM_WEB_EXPORT am_web_req_method_t
am_web_method_str_to_num(const char *method_str)
{
    am_web_req_method_t method = AM_WEB_REQUEST_UNKNOWN;
    if (method_str != NULL) {
	if (!strcmp(method_str, REQUEST_METHOD_GET))
	    method = AM_WEB_REQUEST_GET;
	else if (!strcmp(method_str, REQUEST_METHOD_POST))
	    method = AM_WEB_REQUEST_POST;
	else if (!strcmp(method_str, REQUEST_METHOD_HEAD))
	    method = AM_WEB_REQUEST_HEAD;
	else if (!strcmp(method_str, REQUEST_METHOD_PUT))
	    method = AM_WEB_REQUEST_PUT;
	else if (!strcmp(method_str, REQUEST_METHOD_DELETE))
	    method = AM_WEB_REQUEST_DELETE;
	else if (!strcmp(method_str, REQUEST_METHOD_TRACE))
	    method = AM_WEB_REQUEST_TRACE;
	else if (!strcmp(method_str, REQUEST_METHOD_OPTIONS))
	    method = AM_WEB_REQUEST_OPTIONS;
	else if (!strcmp(method_str, REQUEST_METHOD_CONNECT))
	    method = AM_WEB_REQUEST_CONNECT;
	else if (!strcmp(method_str, REQUEST_METHOD_COPY))
	    method = AM_WEB_REQUEST_COPY;
	else if (!strcmp(method_str, REQUEST_METHOD_INVALID))
	    method = AM_WEB_REQUEST_INVALID;
	else if (!strcmp(method_str, REQUEST_METHOD_LOCK))
	    method = AM_WEB_REQUEST_LOCK;
	else if (!strcmp(method_str, REQUEST_METHOD_UNLOCK))
	    method = AM_WEB_REQUEST_UNLOCK;
	else if (!strcmp(method_str, REQUEST_METHOD_MKCOL))
	    method = AM_WEB_REQUEST_MKCOL;
	else if (!strcmp(method_str, REQUEST_METHOD_MOVE))
	    method = AM_WEB_REQUEST_MOVE;
	else if (!strcmp(method_str, REQUEST_METHOD_PATCH))
	    method = AM_WEB_REQUEST_PATCH;
	else if (!strcmp(method_str, REQUEST_METHOD_PROPFIND))
	    method = AM_WEB_REQUEST_PROPFIND;
	else if (!strcmp(method_str, REQUEST_METHOD_PROPPATCH))
	    method = AM_WEB_REQUEST_PROPPATCH;
	else if (!strcmp(method_str, REQUEST_METHOD_VERSION_CONTROL))
	    method = AM_WEB_REQUEST_VERSION_CONTROL;
	else if (!strcmp(method_str, REQUEST_METHOD_CHECKOUT))
	    method = AM_WEB_REQUEST_CHECKOUT;
	else if (!strcmp(method_str, REQUEST_METHOD_UNCHECKOUT))
	    method = AM_WEB_REQUEST_UNCHECKOUT;
	else if (!strcmp(method_str, REQUEST_METHOD_CHECKIN))
	    method = AM_WEB_REQUEST_CHECKIN;
	else if (!strcmp(method_str, REQUEST_METHOD_UPDATE))
	    method = AM_WEB_REQUEST_UPDATE;
	else if (!strcmp(method_str, REQUEST_METHOD_LABEL))
	    method = AM_WEB_REQUEST_LABEL;
	else if (!strcmp(method_str, REQUEST_METHOD_REPORT))
	    method = AM_WEB_REQUEST_REPORT;	   
	else if (!strcmp(method_str, REQUEST_METHOD_MKWORKSPACE))
	    method = AM_WEB_REQUEST_MKWORKSPACE;
	else if (!strcmp(method_str, REQUEST_METHOD_MKACTIVITY))
	    method = AM_WEB_REQUEST_MKACTIVITY;
	else if (!strcmp(method_str, REQUEST_METHOD_BASELINE_CONTROL))
	    method = AM_WEB_REQUEST_BASELINE_CONTROL;	    	    
	else if (!strcmp(method_str, REQUEST_METHOD_MERGE))
	    method = AM_WEB_REQUEST_MERGE;
	else
	    method = AM_WEB_REQUEST_UNKNOWN;
    }
    return method;
}

extern "C" AM_WEB_EXPORT const char *
am_web_result_num_to_str(am_web_result_t result)
{
    char *resultName = const_cast<char*>("UNKNOWN");
    switch(result) {
    case AM_WEB_RESULT_OK:
	resultName = const_cast<char *>("AM_WEB_RESULT_OK");
	break;
    case AM_WEB_RESULT_OK_DONE:
	resultName = const_cast<char *>("AM_WEB_RESULT_OK_DONE");
	break;
    case AM_WEB_RESULT_FORBIDDEN:
	resultName = const_cast<char *>("AM_WEB_RESULT_FORBIDDEN");
	break;
    case AM_WEB_RESULT_REDIRECT:
	resultName = const_cast<char *>("AM_WEB_RESULT_REDIRECT");
	break;
    case AM_WEB_RESULT_ERROR:
	resultName = const_cast<char *>("AM_WEB_RESULT_ERROR");
	break;
    default:
	resultName = const_cast<char *>("Unknown result code");
	break;
    }
    return resultName;
}

/**
 * Removes all instances of the given cookie name from the cookie
 * header value. cookie_header_val will be overwritten with
 * new cookie header value.
 * Note: only handles netscape style cookie headers.
 */
static am_status_t
remove_cookie(const char *cookie_name, char *cookie_header_val)
{
    am_status_t sts = AM_SUCCESS;
    char *last = NULL;
    char *tok = NULL;
    char *buf = NULL;
//    bool found = false;
    if (cookie_name == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else if (cookie_header_val == NULL) {
	sts = AM_SUCCESS;
    }
    else if (strstr(cookie_header_val, cookie_name) == NULL) {
	// simple check if cookie is in cookie header val.
	sts = AM_NOT_FOUND;
    }
    else if ((buf = (char *)calloc(1, strlen(cookie_header_val)+1)) == NULL) {
	sts = AM_NO_MEMORY;
    }
    else {
	// check if each cookie in cookie header
	tok = strtok_r(cookie_header_val, ";", &last);
	while (tok != NULL) {
#if defined(_AMD64_)
	    size_t cookie_name_len = strlen(cookie_name);
#else
	    unsigned int cookie_name_len = strlen(cookie_name);
#endif
	    bool match = false;
	    char *equal_sign = strchr(tok, '=');
	    // trim space before the cookie name in the cookie header.
	    while (isspace(*tok))
		tok++;
	    if (equal_sign != NULL && equal_sign != tok) {
		// trim white space after the cookie name in the cookie header.
		while ((--equal_sign) >= tok && isspace(*equal_sign))
		    ;
		equal_sign++;
		// now compare the cookie names.
		if (equal_sign != tok &&
		    (equal_sign - tok) == cookie_name_len &&
		    !strncmp(tok, cookie_name, cookie_name_len)) {
		    match = true;
		}
	    }
	    // put cookie in header only if didn't match cookie name.
	    if (!match) {
		if (*buf != '\0') {
		    strcat(buf, ";");
		}
		strcat(buf, tok);
	    }
	    tok = strtok_r(NULL, ";", &last);
	}
	strcpy(cookie_header_val, buf);
	free(buf);
	sts = AM_SUCCESS;
    }
    return sts;
}

/**
 * Find a cookie name in a cookie header value, return pointers to the
 * cookie's name, value etc. in the cookie header value.
 * Netscape style cookies is assumed.
 *
 * Arguments:
 * cookie_name - the cookie name
 * cookie_header_val - the cookie value.
 * name_ptr - will contain a pointer in cookie_header_val where cookie_name
 *	      is found.
 * val_ptr - will contain a pointer in cookie_header_val where the cookie
 *           value is begins or NULL if no value was set.
 * val_len - will contain the value length, or 0 if no value was set.
 * next_cookie_ptr - will contain pointer to the next cookie, including the ';'
 *                   seperator befor the next cookie.
 *
 * Returns:
 * AM_SUCCESS - if cookie name is found.
 * AM_INVALID_ARGUMENT - if any argument is invalid.
 * AM_NOT_FOUND - if cookie name is not found.
 */
static am_status_t
find_cookie(const char *cookie_name,
	    const char *cookie_header_val,
	    char **name_ptr,
	    char **val_ptr, size_t *val_len, char **next_cookie_ptr)
{
    const char *thisfunc = "find_cookie()";
    am_status_t sts = AM_NOT_FOUND;
    char *found = NULL;
    char *value = NULL;
    char c;
    char *search_cookie = NULL;

    if (cookie_name != NULL && cookie_name[0] != '\0') {
        search_cookie = (char *)malloc(2+strlen(cookie_name));
        if (search_cookie !=NULL) {
            search_cookie = strcpy(search_cookie,cookie_name);
            search_cookie = strcat(search_cookie,"=");
        }
    }

    if (cookie_name == NULL ||
	name_ptr == NULL || val_ptr == NULL || val_len == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else if (cookie_name[0] == '\0' ||
	     cookie_header_val == NULL || cookie_header_val[0] == '\0') {
	sts = AM_NOT_FOUND;
    } else if (search_cookie == NULL) {
        sts = AM_NO_MEMORY;
    }
    // find cookie_name in header value first.
    else if ((found = const_cast<char *>
			(strstr(cookie_header_val, search_cookie))) == NULL) {
	sts = AM_NOT_FOUND;
    }
    // if found, make sure it's not a substring of another cookie name i.e.
    // must have a ';' before and '=' after.
    else if (found != cookie_header_val &&
		(c = *(found-1)) != ';' && !isspace(c)) {
	sts = AM_NOT_FOUND;  // other chars besides ';' or space before name.
    }
    else if ((c = *(found + strlen(cookie_name))) != '=' &&
		!isspace(c) && c != ';' && c != '\0') {
	sts = AM_NOT_FOUND; // other chars besides space or '=' after name.
    }
    else if ((value = strchr(found, '=')) == NULL) {
	sts = AM_NOT_FOUND;  // invalid syntax: no '=' anywhere after name.
    }
    // name really found.
    else {
	// set name.
	*name_ptr = found;
	// get value -
	// skip white space after '=', stop at ';' or end of string.
	value++;
	while (isspace(*value))
	    value++;
	// value is null if we've reached the end
	if (*value == ';' || *value == '\0') {
	    *val_ptr = NULL;
	    *val_len = 0;
	    *next_cookie_ptr = NULL;
	}
	else {
	    *val_ptr = value;
	    found = strchr(value, ';');
	    if (found == NULL) {
		*val_len = strlen(value);  // no cookies following this one.
		*next_cookie_ptr = NULL;
	    }
	    else {
		*val_len = found - value;
		*next_cookie_ptr = found;
	    }
	}
	am_web_log_debug("%s: cookie found: header [%s] name [%s] val [%s] "
			 "val_len [%d] next_cookie [%s]", thisfunc,
			 cookie_header_val==NULL?"NULL":cookie_header_val,
			 *name_ptr==NULL?"NULL":*name_ptr,
			 *val_ptr==NULL?"NULL":*val_ptr,
			 *val_len,
			 *next_cookie_ptr==NULL?"NULL":*next_cookie_ptr);

	sts = AM_SUCCESS;
    }

    if(search_cookie !=NULL) {
        am_web_free_memory(search_cookie);
    }
    return sts;
}


/**
 * Get a cookie's value from the given cookie header.
 * Argument:
 * cookie_name - name of cookie to get
 * cookie_header_val - cookie header string
 * buf - will contain the cookie's value, or NULL if cookie was found but
 *       contained no (empty) value.
 *       If not null, the pointer must be freed by caller when done.
 * Returns:
 * AM_SUCCESS - on success.
 * AM_NOT_FOUND - if cookie was not found in cookie header.
 * AM_INVALID_ARGUMENT - if any arguments is invalid.
 */
static am_status_t
get_cookie_val(const char *cookie_name,
	       const char *cookie_header_val,
	       char **buf)
{
    am_status_t sts = AM_FAILURE;
    char *name = NULL;
    char *val = NULL;
    size_t val_len = 0;
    char *next_cookie = NULL;

    sts = find_cookie(cookie_name, cookie_header_val,
		      &name, &val, &val_len, &next_cookie);
    if (sts == AM_SUCCESS) {
	if (val == NULL) {  // cookie found but had no (empty) value.
	    *buf = NULL;
	    sts = AM_SUCCESS;
	}
	else {
	    *buf = (char *)malloc(1+val_len);
	    if (*buf == NULL) {
		sts = AM_NO_MEMORY;
	    }
	    else {
		strncpy(*buf, val, val_len);
		(*buf)[val_len] = '\0';
		sts = AM_SUCCESS;
	    }
	}
    }
    return sts;
}

static am_status_t
process_notification(
    char *url,
    am_web_req_method_t method,
    am_web_get_post_data_t get_post_data,
    am_web_free_post_data_t free_post_data)
{
    const char *thisfunc = "process_notification()";
    am_status_t sts = AM_SUCCESS;
    am_status_t free_sts = AM_SUCCESS;
    char *postdata = NULL;

    // Check arguments.
    if (get_post_data.func == NULL) {
	am_web_log_error("%s: input argument get_post_data is null.", thisfunc);
	sts = AM_INVALID_ARGUMENT;
    }
    // Check if it's a post.
    else if (method != AM_WEB_REQUEST_POST) {
	am_web_log_error("%s: Notification URL cannot be processed: "
			 "request method [%s] is not a POST.",
			 thisfunc, am_web_method_num_to_str(method));
	sts = AM_INVALID_ARGUMENT;
    }
    // get post data
    else if ((sts = get_post_data.func(
			get_post_data.args, &postdata)) != AM_SUCCESS ||
	     postdata == NULL || postdata[0] == '\0') {
	am_web_log_warning("%s: cannot process notification. "
			   "POST data is %s, status is %s.",
			   thisfunc, postdata == NULL ? "NULL":"empty",
			   am_status_to_string(sts));
	sts = AM_NOT_FOUND; // post data not found.
    }
    // process notification
    else {
        //FIX IT with 3.0 Apache agent
	//am_web_handle_notification(postdata, strlen(postdata));
	am_web_log_debug("%s: process notification from URL [%s] "
			   "returned.", thisfunc, url);
	if (free_post_data.func != NULL) {
	    free_sts = free_post_data.func(free_post_data.args, postdata);
	    if (free_sts != AM_SUCCESS) {
		am_web_log_warning("%s: free post data returned %s.",
				   thisfunc, am_status_to_string(free_sts));
	    }
	}
    }
    return sts;
}

// Get sso token from an assertion from either get or post.
// *sso_token will be not null and not empty if the function returns success.
static am_status_t
get_token_from_assertion(
	char *url,
	am_web_req_method_t method,
	am_web_get_post_data_t get_post_data,
	am_web_free_post_data_t free_post_data,
	char **sso_token,
        void* agent_config)
{
    const char *thisfunc = "get_token_from_assertion()";
    am_status_t sts = AM_SUCCESS;
    const char *cookieName = am_web_get_cookie_name(agent_config);
    char *postdata = NULL;

    // check method arg
    if (method != AM_WEB_REQUEST_GET &&
	method != AM_WEB_REQUEST_POST) {
	am_web_log_error("%s: invalid method %s.", thisfunc,
			 am_web_method_num_to_str(method));
	sts = AM_FEATURE_UNSUPPORTED;
    }
    // Get cookie from GET
    else if (method == AM_WEB_REQUEST_GET) {
	if (url == NULL || url[0] == '\0') {
	    am_web_log_error("%s: Error getting cookie from assertion: "
			     "URL is null or empty.", thisfunc);
	    sts = AM_NOT_FOUND;
	}
	else {
	    sts = am_web_get_parameter_value(url, cookieName, sso_token);
	    if (sts != AM_SUCCESS ||
		*sso_token == NULL || (*sso_token)[0] == '\0') {
		am_web_log_error("%s: Error getting cookie %s from assertion: "
				 "status %s, sso_token %s",
				 thisfunc, cookieName,
				 am_status_to_string(sts),
				 *sso_token == NULL ? "NULL": "empty");
		sts = AM_NOT_FOUND;
		if (*sso_token != NULL) {
		    free(*sso_token);
		    *sso_token = NULL;
		}
	    }
	}
    }
    // Get cookie from POST
    else {  // POST
	if (get_post_data.func == NULL) {
	    am_web_log_error("%s: request is a post but get_post_data is NULL",
			     thisfunc);
	    sts = AM_INVALID_ARGUMENT;
	}
	else if ((sts = get_post_data.func(get_post_data.args,
				      &postdata)) != AM_SUCCESS ||
		 postdata == NULL || postdata[0] == '\0') {
	    am_web_log_error("%s: Failed to get post data, status %s, "
			     "postdata %s",
			     thisfunc, am_status_to_string(sts),
			     postdata == NULL ? "NULL": "empty");
	    if (postdata != NULL) {
		if (free_post_data.func != NULL) {
		    free_post_data.func(free_post_data.args, postdata);
		}
		postdata = NULL;
	    }
	    sts = AM_NOT_FOUND;
	}
	else if ((sts = am_web_get_token_from_assertion(
			    postdata, sso_token, agent_config)) != AM_SUCCESS ||
		 *sso_token == NULL || (*sso_token)[0] == '\0') {
	    am_web_log_error("%s: Failed to get sso token from assertion, "
			     "status %s, sso_token %s",
			     thisfunc, am_status_to_string(sts),
			     *sso_token == NULL ? "NULL" : "empty");
	    sts = AM_NOT_FOUND;
	    if (*sso_token != NULL) {
		free(*sso_token);
		*sso_token = NULL;
	    }
	}
	else {
	    // all is ok - free post data.
	    if (postdata != NULL) {
		if (free_post_data.func != NULL) {
		    free_post_data.func(free_post_data.args, postdata);
		}
		postdata = NULL;
	    }
	}
    }
    return sts;
}

/**
 * If new_cookie_header_val_ptr contains NULL it means nothing was done,
 * i.e. if cookie was not found in header and it has empty value.
 * If new_cookie_header_val_ptr contains the original cookie header's
 * address then it means no cookie was added but cookie was removed.
 * Otherwise new_cookie_header_val_ptr contains pointer to a newly allocated
 * buffer containing the modified cookie header.
 * Caller is responsible for freeing the new cookie header value pointer.
 */
static am_status_t
set_cookie_in_cookie_header(char *cookie_header,
			    const char *cookie_name, const char *cookie_val,
			    char **new_cookie_header_ptr)
{
    const char *thisfunc = "set_cookie_in_cookie_header()";
    am_status_t sts = AM_SUCCESS;

    // remove any occurrences of the cookie in cookie header
    if (cookie_header != NULL &&
	(sts = remove_cookie(cookie_name, cookie_header)) != AM_SUCCESS &&
	sts != AM_NOT_FOUND) {
	am_web_log_error("%s: Error %s removing cookie %s from "
			 "cookie header val %s", thisfunc,
			 am_status_to_name(sts),
			 cookie_name, cookie_header);
    }
    else if (cookie_val==NULL || cookie_val[0]=='\0') {
	// no value in cookie so nothing to add to cookie header.
	if (sts == AM_NOT_FOUND) {
	    *new_cookie_header_ptr = NULL;  // cookie wasn't removed.
	} else {
	    *new_cookie_header_ptr = cookie_header;
	}
	sts = AM_SUCCESS;
    }
    else {
	// now set new value
	// if no new value given, we're done.
	char *new_cookie_header = NULL;
	if (cookie_header == NULL) {
	    new_cookie_header = (char *)
		calloc(1, strlen(cookie_name)+strlen(cookie_val)+2);
	} else {
	    new_cookie_header = (char *)
		calloc(1, strlen(cookie_header)+
			    strlen(cookie_name)+strlen(cookie_val)+3);
	    strcpy(new_cookie_header, cookie_header);
	    strcat(new_cookie_header, ";");
	}
	if (new_cookie_header == NULL) {
	    sts = AM_NO_MEMORY;
	}
	else {
	    strcat(new_cookie_header, cookie_name);
	    strcat(new_cookie_header, "=");
	    strcat(new_cookie_header, cookie_val);
	}
	*new_cookie_header_ptr = new_cookie_header;
	sts = AM_SUCCESS;
    }
    return sts;
}

/*
 * Set a cookie in the cookie request header.
 */
static am_status_t
set_cookie_in_request(Utils::cookie_info_t *cookie_info,
		      am_web_request_params_t *req_params,
		      am_web_request_func_t *req_func,
		      bool do_set)
{
    const char *thisfunc = "set_cookie_in_request()";
    am_status_t sts = AM_FAILURE;

    // check args
    if (cookie_info == NULL || cookie_info->name == NULL ||
	req_params == NULL || req_func == NULL) {
	am_web_log_error("%s: invalid arguments.", thisfunc);
	sts = AM_INVALID_ARGUMENT;
    }
    else if (req_func->set_header_in_request.func == NULL) {
	am_web_log_debug("%s: cannot set cookie; no function provided "
			 "to set header in request.", thisfunc);
	sts = AM_NOT_FOUND;
    }
    // insert the cookie into the cookie string, replacing
    // any existing cookies of the same name.
    else {
//	char *buf = NULL;
	char *cookie_header_val = NULL;
	char *new_cookie_header_val = NULL;

	// get previous cookie header val.
	if (req_params->reserved == NULL)
	    cookie_header_val = req_params->cookie_header_val;
	else
	    cookie_header_val = (char *)req_params->reserved;

	sts = set_cookie_in_cookie_header(
		    cookie_header_val, cookie_info->name,
		    cookie_info->value, &new_cookie_header_val);
	if (sts == AM_SUCCESS && new_cookie_header_val != NULL) {
	    if (new_cookie_header_val != cookie_header_val) {
		// free previous cookie header val if any, and save
		// this cookie header val in case more cookies need to be set.
		if (req_params->reserved != NULL) {
		    free(req_params->reserved);
		}
		req_params->reserved = new_cookie_header_val;
	    }
	    if (do_set) {
		sts = req_func->set_header_in_request.func(
			    req_func->set_header_in_request.args,
			    "Cookie", new_cookie_header_val);
		am_web_log_max_debug("%s: set cookie header %s "
				     "returned %s.", thisfunc,
				     new_cookie_header_val,
				     am_status_to_string(sts));
	    }
	}
    }
    return sts;
}

static am_status_t
add_cookie_in_response(const char *set_cookie_header_val, void **args)
{
    am_status_t sts = AM_SUCCESS;
    am_web_request_func_t *req_func = NULL;

    if (set_cookie_header_val == NULL || args == NULL ||
	(req_func = (am_web_request_func_t *)args[0]) == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else if (req_func->add_header_in_response.func == NULL) {
	sts = AM_NOT_FOUND;
    }
    else {
	sts = req_func->add_header_in_response.func(
		    req_func->add_header_in_response.args,
		    "Set-Cookie",
		    set_cookie_header_val);
    }
    return sts;
}

static am_status_t
set_cookie_in_request_and_response(Utils::cookie_info_t *cookie_info,
				   am_web_request_params_t *req_params,
				   am_web_request_func_t *req_func,
				   bool do_set)
{
    const char *thisfunc = "set_cookie_in_request_and_response()";
    am_status_t sts = AM_FAILURE;
    am_status_t set_sts = AM_FAILURE;

    if (cookie_info == NULL || req_params == NULL || req_func == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	// set cookie in request
	sts = set_cookie_in_request(cookie_info, req_params, req_func, do_set);
	am_web_log_debug("%s: set cookie %s in request returned %s.",
			 thisfunc, cookie_info->name, am_status_to_string(sts));

	// set cookie in response
	if (req_func->add_header_in_response.func == NULL) {
	    am_web_log_debug("%s: no function provided to add response header",
			     thisfunc);
	    sts = AM_NOT_FOUND;
	}
	else {
	    const char *set_cookie_header_val =
			    buildSetCookieHeader(cookie_info);
	    if (set_cookie_header_val == NULL) {
		am_web_log_error("%s: No more memory in setting cookie %s.",
				 thisfunc, cookie_info->name);
		if (sts == AM_SUCCESS) {
		    sts = AM_NO_MEMORY;
		}
	    }
	    else {
		set_sts = req_func->add_header_in_response.func(
				req_func->add_header_in_response.args,
				"Set-Cookie", set_cookie_header_val);
		am_web_log_debug("%s: set cookie %s in response returned %s",
				 thisfunc, cookie_info->name,
				 am_status_to_name(sts));
		if (AM_SUCCESS != sts) {
		    sts = set_sts;
		}
		free((void *)set_cookie_header_val);
	    }
	}
    }
    return sts;
}

// set the given sso token in the cookie request header and set-cookie
// response header.
static am_status_t
set_cookie_in_domains(const char *sso_token,
		      am_web_request_params_t *req_params,
		      am_web_request_func_t *req_func,
                      void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    const char *thisfunc = "set_cookie_in_domains()";
    am_status_t sts = AM_SUCCESS;
    am_status_t setSts = AM_SUCCESS;
    Utils::cookie_info_t cookieInfo;

    if (sso_token == NULL || sso_token[0] == '\0' || req_func == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	std::set<std::string> *cookie_domains = (*agentConfigPtr)->cookie_domain_list;
	cookieInfo.name = (char *)(*agentConfigPtr)->cookie_name;
	cookieInfo.value = (char *)sso_token;
	cookieInfo.domain = NULL;
	// This must be null (not empty string) for older browsers
	// netscape 4.79, IE 5.5, mozilla < 1.4.
	cookieInfo.max_age = NULL;
	cookieInfo.path = const_cast<char*>("/");

	if (NULL == cookie_domains || cookie_domains->size() <= 0) {
	    // if no domains configured, don't set domain,
	    // browser will default domain to the host.
	    sts = set_cookie_in_request_and_response(
			    &cookieInfo, req_params, req_func, true);
	    am_web_log_debug("%s: setting cookie %s in default domain "
			     "returned %s", thisfunc, (*agentConfigPtr)->cookie_name,
			     am_status_to_string(sts));
	}
	else {
	    std::set<std::string>::iterator iter;
	    for (iter = cookie_domains->begin();
		 iter != cookie_domains->end(); iter++) {
		cookieInfo.domain = (char *)((*iter).c_str());
		setSts = set_cookie_in_request_and_response(
			    &cookieInfo, req_params, req_func, true);
		am_web_log_debug("%s: setting cookie %s in domain %s"
				 "returned %s", thisfunc,
				 (*agentConfigPtr)->cookie_name,
				 cookieInfo.domain,
				 am_status_to_string(sts));
		if (setSts != AM_SUCCESS) {
		    sts = setSts;
		}
	    }
	}
    }
    return sts;
}

/*
 * Extract cookie from the POST assertion or GET parameter from the
 * CDC servlet, and set extracted cookie in agent domains and
 * set original method using container's set method routine.
 * Pointer for sso_token must be freed when done.
 */
static am_status_t
process_cdsso(
     am_web_request_params_t *req_params,
     am_web_request_func_t *req_func,
     am_web_req_method_t orig_method,
     char **sso_token,
     void* agent_config)
{
    const char *thisfunc = "process_cdsso()";
    am_status_t sts = AM_SUCCESS;
    am_status_t local_sts = AM_SUCCESS;
    am_web_req_method_t method = req_params->method;

    // Check args
    if (req_params->url == NULL ||
        sso_token == NULL || orig_method == NULL) {
        am_web_log_error("%s: one or more input arguments is not valid.",
                         thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if (AM_WEB_REQUEST_POST == method &&
               (NULL == req_func->get_post_data.func ||
               NULL == req_func->set_method.func)) {
        am_web_log_error("%s: get post data is not provided. "
                         "CDSSO with post cannot be supported.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if ((sts = get_token_from_assertion(req_params->url,
                               req_params->method,
                               req_func->get_post_data,
                               req_func->free_post_data,
                               sso_token,
                               agent_config)) != AM_SUCCESS) {
        // Get sso token from assertion.
        am_web_log_error("%s: Error getting token from assertion: %s",
                    thisfunc, am_status_to_string(sts));
    } else {
        // Set cookie in domain
        local_sts = set_cookie_in_domains(*sso_token, req_params, 
                           req_func, agent_config);
        if (local_sts != AM_SUCCESS) {
            // ignore error but give a warning
            am_web_log_warning("%s: cookie domain set of sso_token [%s] "
                             "failed with %s.",
                             thisfunc, sso_token,
                             am_status_to_string(local_sts));
        }
        // Set original method
        local_sts = req_func->set_method.func(
               req_func->set_method.args, orig_method);
        if (local_sts != AM_SUCCESS) {
            // ignore error but give a warning.
            am_web_log_warning("%s: set method to original method %s "
                     "returned error: %s.",
                     thisfunc, am_web_method_num_to_str(orig_method),
                     am_status_to_string(local_sts));
        }
    }
    return sts;
}

/*
 * this function does what am_web_result_attr_map_set() does
 * but uses the new function as arguments since am_web_result_attr_map_set
 * only takes one set of args for all functions.
 * If configured sets the user's profile, session and response attributes
 * as headers or cookies based on their respective modes.
 * Returns AM_SUCCESS if setting all attributes returned successfully.
 * If an error is encountered, other attributes are still set, and
 * the first (if any) failure status encountered is returned.
 */
static am_status_t
set_user_attributes(am_policy_result_t *result,
                    am_web_request_params_t *req_params,
                    am_web_request_func_t *req_func,
                    void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

     const char *thisfunc = "set_user_attributes()";
     am_status_t sts = AM_SUCCESS;
     am_status_t set_sts = AM_SUCCESS;
     am_map_t attrMap = NULL;
     const char* mode  = "NONE";

     // check arguments.
     if (result == NULL || req_params == NULL || req_func == NULL) {
         am_web_log_error("%s: invalid argument passed. ", thisfunc);
         sts = AM_INVALID_ARGUMENT;
     }
     // if attributes mode is none, we're done.
     else if ((SET_ATTRS_NONE == (*agentConfigPtr)->setUserProfileAttrsMode) &&
              (SET_ATTRS_NONE == (*agentConfigPtr)->setUserSessionAttrsMode) &&
              (SET_ATTRS_NONE == (*agentConfigPtr)->setUserResponseAttrsMode)) {
                am_web_log_debug("%s: set user attributes option set to none.",
                                  thisfunc);
                sts = AM_SUCCESS;
     }
     // if no attributes in result, we're done.
     else if ((result->attr_profile_map == AM_MAP_NULL) &&
             (result->attr_session_map == AM_MAP_NULL) &&
             (result->attr_response_map == AM_MAP_NULL)) {
               am_web_log_info("%s: All attributes maps are null. Nothing to set ",
			       thisfunc);
               sts = AM_SUCCESS;
    }
    // if set user LDAP attribute option is headers and set
    // request headers is null, return.
    else if (((SET_ATTRS_AS_HEADER == (*agentConfigPtr)->setUserProfileAttrsMode) ||
    		 (SET_ATTRS_AS_HEADER == (*agentConfigPtr)->setUserSessionAttrsMode) ||
    		 (SET_ATTRS_AS_HEADER == (*agentConfigPtr)->setUserResponseAttrsMode)) &&
                   NULL == req_func->set_header_in_request.func) {
              am_web_log_warning("%s: set user attributes option is "
                                 "HEADER but no set request header "
                                 "function is provided. ", thisfunc);
              sts = AM_NOT_FOUND;
    }
    // if set user LDAP attribute cookies option is "cookie" and
    // functions are not provided to set cookie, log a warning.
    else if (((SET_ATTRS_AS_COOKIE == (*agentConfigPtr)->setUserProfileAttrsMode) ||
              (SET_ATTRS_AS_COOKIE == (*agentConfigPtr)->setUserSessionAttrsMode) ||
              (SET_ATTRS_AS_COOKIE == (*agentConfigPtr)->setUserResponseAttrsMode)) &&
                (NULL == req_func->set_header_in_request.func ||
                 NULL == req_func->add_header_in_response.func)) {
              am_web_log_warning("%s: set user attributes option is "
                                 "COOKIE but no function provided "
                                 "to either set cookie in request "
                                 "response is provided. ", thisfunc);
              sts = AM_NOT_FOUND;
    }
    // now go do it.
    else {
      try {
          // clear headers/cookies first.
          char *cookie_header_val = NULL;
          if (req_params->reserved == NULL) {
             if (req_params->cookie_header_val != NULL) {
                cookie_header_val = strdup(req_params->cookie_header_val);
                req_params->reserved = cookie_header_val;
             }
         } else {
             cookie_header_val = (char *)req_params->reserved;
         }
         if (cookie_header_val != NULL) {
            std::list<std::string>::const_iterator attr_iter;
            std::list<std::string>::const_iterator attr_end = 
                (*agentConfigPtr)->attrList.end();
            for(attr_iter = (*agentConfigPtr)->attrList.begin(); attr_iter != attr_end;
                                              attr_iter++) {
               const char * header_name = (*attr_iter).c_str();
               if ((SET_ATTRS_AS_HEADER == (*agentConfigPtr)->setUserProfileAttrsMode) ||
                   (SET_ATTRS_AS_HEADER == (*agentConfigPtr)->setUserSessionAttrsMode) ||
                   (SET_ATTRS_AS_HEADER == (*agentConfigPtr)->setUserResponseAttrsMode)) {
                        am_web_log_warning("%s: Clearing header name %s.",
                                              thisfunc, header_name);
                        set_sts = req_func->set_header_in_request.func(
                                         req_func->set_header_in_request.args,
                                         header_name, NULL);
                        if (set_sts != AM_SUCCESS) {
                           am_web_log_warning("%s: Error encountered while "
                                              "clearing header name %s.",
                                              thisfunc, header_name);
                        }
                }
                else if ((SET_ATTRS_AS_COOKIE == (*agentConfigPtr)->setUserProfileAttrsMode) ||
                         (SET_ATTRS_AS_COOKIE == (*agentConfigPtr)->setUserSessionAttrsMode) ||
                         (SET_ATTRS_AS_COOKIE == (*agentConfigPtr)->setUserResponseAttrsMode)) {
                     // for cookie, remove all cookies of the same name
                     // in the cookie header.
                     std::string cookie_name((*agentConfigPtr)->attrCookiePrefix);
                     cookie_name.append(*attr_iter);
                     Utils::cookie_info_t cookie_info;
                     cookie_info.name = (char *)cookie_name.c_str();
                     cookie_info.value = NULL;
                     cookie_info.domain = NULL;
                     cookie_info.max_age = (char *)"0";
                     cookie_info.path = const_cast<char*>("/");
                     // set cookie in request and response.
                     set_sts = set_cookie_in_request_and_response(
                               &cookie_info, req_params, req_func, false);
                     am_web_log_debug("%s: clear cookie %s returned %s",
                                     thisfunc, cookie_info.name,
                                     am_status_to_name(sts));
                     if (set_sts != AM_SUCCESS && sts == AM_SUCCESS) {
                         sts = set_sts;
                     }
                 }
              }
          }

	// now set the cookie header.
        set_sts = req_func->set_header_in_request.func(
                       req_func->set_header_in_request.args,
                       "Cookie", (char *)req_params->reserved);
        am_web_log_debug("%s:set cookie header %s in request returned %s",
                        thisfunc, req_params->reserved,
                        am_status_to_string(set_sts));

        for (int i=0; i<3; i++) {
	      switch (i) {
		  case 0:
		       attrMap = result->attr_profile_map;
		       mode = (*agentConfigPtr)->profileMode;
		       break;
		  case 1:
		       attrMap = result->attr_session_map;
		       mode = (*agentConfigPtr)->sessionMode;
		       break;
		  case 2:
		       attrMap = result->attr_response_map;
		       mode = (*agentConfigPtr)->responseMode;
		       break;
                  default:
		       break;
             }
	     if (!strcasecmp(mode, AM_POLICY_SET_ATTRS_AS_HEADER) && (attrMap != AM_MAP_NULL)) {
               // set the new values.
               const KeyValueMap &headerAttrs =
		                         *(reinterpret_cast<const KeyValueMap *>
			                     (attrMap));
               // loop through all attributes from policy result.
               KeyValueMap::const_iterator iter = headerAttrs.begin();
               for (;(iter != headerAttrs.end()); iter++) {
                  std::size_t i = 0;
                  std::string values;
                  const KeyValueMap::key_type &keyRef = iter->first;
                  const KeyValueMap::mapped_type &valuesRef = iter->second;
                  std::size_t num_values = valuesRef.size();
                  const char *key = keyRef.c_str();
                  Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
			           "%s: For user attribute %s, iterating over %u values.",
			           thisfunc, key, num_values);

                  // put each value into "val1,val2,val3.." format
                  if (num_values > 0) {
                     values.append(valuesRef[i++]);
                     for(; i < num_values; ++i) {
                        PUSH_BACK_CHAR(values, ',');
                        values.append(valuesRef[i]);
                     }
                   }
                   set_sts = req_func->set_header_in_request.func(
				                       req_func->set_header_in_request.args,
                                       key, values.c_str());
                   am_web_log_debug("%s: set request header key %s, "
                                     "value %s, returned %s", thisfunc, key,
                                     values.c_str(), am_status_to_name(sts));
                  if (set_sts != AM_SUCCESS && sts == AM_SUCCESS) {
                      sts = set_sts;
                  }
                }
            }

            // set attributes as cookies
            if (!strcasecmp(mode, AM_POLICY_SET_ATTRS_AS_COOKIE)&& (attrMap != AM_MAP_NULL)) {
                // set the new values.
                const KeyValueMap &cookieAttrs =
                      *(reinterpret_cast<const KeyValueMap *>
                        (attrMap));
                KeyValueMap::const_iterator iter = cookieAttrs.begin();
                for(;(iter != cookieAttrs.end()); iter++) {
                   std::size_t i = 0;
                   std::string values;
                   const KeyValueMap::key_type &keyRef = iter->first;
                   const KeyValueMap::mapped_type &valuesRef = iter->second;
                   std::size_t num_values = valuesRef.size();
                   const char *key = keyRef.c_str();
                   Log::log(boot_info.log_module, Log::LOG_MAX_DEBUG,
                      "%s: For user attribute %s, iterating over %u values.",
                      thisfunc, key, num_values);

                   // put each value into "val1,val2,val3.." format
                   if (num_values > 0) {
                      values.append(valuesRef[i++]);
                      for(; i < num_values; ++i) {
                        PUSH_BACK_CHAR(values, ',');
                        values.append(valuesRef[i]);
                      }
                   }

                   Utils::cookie_info_t cookie_info;
                   std::string cookie_name((*agentConfigPtr)->attrCookiePrefix);
                   cookie_name.append(key);
                   cookie_info.name = (char *)cookie_name.c_str();

                   std::string encoded_values;
                   if ((*agentConfigPtr)->encodeCookieSpecialChars == AM_TRUE ) {
                       encoded_values = Http::cookie_encode(values);
                       cookie_info.value = (char*)encoded_values.c_str();
                   } else {
                       cookie_info.value = (char*)values.c_str();
                   }

                   cookie_info.domain = NULL;
                   cookie_info.max_age = (char *)(*agentConfigPtr)->attrCookieMaxAge;
                   cookie_info.path = const_cast<char*>("/");
                   // set cookie in request and response.
                   set_sts = set_cookie_in_request_and_response(
                             &cookie_info, req_params, req_func, false);
                   am_web_log_debug("%s: set cookie %s, value %s in header, "
                                    "returned %s", thisfunc, key,
                                    values.c_str(), am_status_to_name(sts));
                   if (set_sts != AM_SUCCESS && sts == AM_SUCCESS) {
                       sts = set_sts;
                   }
                 }
            }
        }
     }
          catch (std::bad_alloc& exb) {
             am_web_log_error("%s: Bad alloc exception encountered: %s.",
                              thisfunc, exb.what());
             sts = AM_NO_MEMORY;
          }
          catch (std::exception& exs) {
             am_web_log_error("%s: Exception encountered: %s.",
                              thisfunc, exs.what());
             sts = AM_FAILURE;
          }
          catch (...) {
            am_web_log_error("%s: Unknown exception encountered.",thisfunc);
            sts = AM_FAILURE;
          }
     }
     return sts;
}

static am_web_result_t
process_access_success(char *url,
                   am_policy_result_t policy_result,
                   am_web_request_params_t *req_params,
                   am_web_request_func_t *req_func,
                   void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    const char *thisfunc = "process_access_success()";
    am_web_result_t result = AM_WEB_RESULT_OK;
    am_status_t sts = AM_SUCCESS;
    PRBool setting_user = AM_TRUE;
    void *args[1];

    // set user - if fail, access is forbidden.
    // If remote_user is null there are two possibilities:
    //   1) if notenforced_url_attributes_enable is set
    //      to true, it is up to the agent/container
    //      to decide whether or not to allow null user.
    //   2) if notenforced_url_attributes_enable is set 
    //      to false, the remote user is not set to anything

    am_web_log_error("%s: Starting", thisfunc);
    if (((*agentConfigPtr)->notenforced_url_attributes_enable == AM_FALSE) &&
            (policy_result.remote_user == NULL)) {
       am_web_log_debug("%s: notenforced.url.attributes.enable is "
                        "set to false and remote user is NULL: "
                        "REMOTE_USER header will not be set.",
                         thisfunc);
       setting_user = AM_FALSE;
    }
    if (req_func->set_user.func == NULL) {
        am_web_log_error("%s: invalid input argument.", thisfunc);
        result = AM_WEB_RESULT_ERROR;
    } else if ((setting_user == AM_TRUE) &&
             ((sts = req_func->set_user.func(req_func->set_user.args,
               policy_result.remote_user)) != AM_SUCCESS)) {
        am_web_log_error("%s: access to %s allowed but "
                "error encountered setting user to %s: %s",
                thisfunc, url, policy_result.remote_user,
                am_status_to_string(sts));
        result = AM_WEB_RESULT_FORBIDDEN;
    }  else {
        // everything ok - now do things post access allowed.
        // if logout url, reset any logout cookies
        if (am_web_is_agent_logout_url(url, agent_config)) {
            args[0] = req_func;
            sts = am_web_logout_cookies_reset(add_cookie_in_response,
                                              args, agent_config);
            if (sts != AM_SUCCESS) {
                am_web_log_warning("%s: Resetting logout cookies after [%s], "
                        "returned %s.", thisfunc, url,
                        am_status_to_string(sts));
            }
        }
        // Set any profile,session or response attributes in 
        // the header or cookie.
        sts = set_user_attributes(&policy_result, req_params,
                                  req_func, agent_config);
        if (sts != AM_SUCCESS) {
            am_web_log_warning("%s: For url [%s], "
                       "set user LDAP attributes returned %s.",
                       thisfunc, url,
                       am_status_to_string(sts));
        }
        // CDSSO parameters should already be removed from the
        // url and query string so no need to remove it again here.
        result = AM_WEB_RESULT_OK;
    }
    am_web_log_debug("%s: returned %s.",
            thisfunc, am_web_result_num_to_str(result));
    return result;
}

static am_web_result_t
process_access_redirect(char *url,
                   am_web_req_method_t method,
                   am_status_t access_check_status,
                   am_policy_result_t policy_result,
                   am_web_request_func_t *req_func,
                   char **redirect_url,
                   char **advice_response,
                   void* agent_config)
{
    const char *thisfunc = "process_access_redirect()";
    am_status_t sts = AM_SUCCESS;
    am_web_result_t result = AM_WEB_RESULT_REDIRECT;

    // Get the redirect url.
    if(access_check_status == AM_REDIRECT_LOGOUT) {
        sts = am_web_get_logout_url(redirect_url, agent_config);
        if(sts == AM_SUCCESS) {
            result = AM_WEB_RESULT_REDIRECT;
        } else {
            result = AM_WEB_RESULT_ERROR;
            am_web_log_debug("process_access_redirect(): "
                                "am_web_get_logout_url failed. ");
        }
    } else if (result != AM_WEB_RESULT_ERROR) {
        sts = am_web_get_url_to_redirect(access_check_status,
                      policy_result.advice_map,
                      url,
                      am_web_method_num_to_str(method),
                      NULL,
                      redirect_url,
                      agent_config);
        am_web_log_debug("%s: get redirect url returned %s, redirect url [%s].",
                          thisfunc, am_status_to_name(sts),
                          *redirect_url == NULL ? "NULL" : *redirect_url);
        if ((policy_result.advice_string != NULL) && 
                    (advice_response != NULL)) {
            char* advice_res = (char *) malloc(2048 * sizeof(char));
            if (advice_res) {
                am_status_t ret = am_web_build_advice_response(
                                           &policy_result, 
                                           *redirect_url,
                                           &advice_res);
                am_web_log_debug("%s: policy status=%s, advice response[%s]", 
                                 thisfunc, am_status_to_string(ret),
                                 *advice_response);
                if(ret != AM_SUCCESS) {
                    am_web_log_error("%s: Error while building "
                               "advice response body:%s",
                               thisfunc, am_status_to_string(ret));
                } else {
                    result = AM_WEB_RESULT_OK_DONE;
                    *advice_response = advice_res;
                }
            } else {
                sts = AM_NO_MEMORY;
            }
        }
        switch (sts) {
            case AM_NO_MEMORY:
                result = AM_WEB_RESULT_ERROR;
                break;
            default:
                if ((!advice_response) || (!(*advice_response))) {
                    if (*redirect_url != NULL) {
                        result = AM_WEB_RESULT_REDIRECT;
                    } else {
                        result = AM_WEB_RESULT_FORBIDDEN;
                    }
                }
                break;
        }
    }
    am_web_log_debug("%s: returning web result %s.",
                thisfunc, am_web_result_num_to_str(result));
    return result;
}

/**
 * Get sso token from either cookie header or cdsso assertion.
 * Also gets the original method.
 * The sso token pointer must be freed when done.
 * Returns AM_SUCCESS upon success, AM_NOT_FOUND if sso token not found,
 * or AM_NO_MEMORY if memory could not be allocated for sso token.
 */
static am_status_t
get_sso_token(am_web_request_params_t *req_params,
              am_web_request_func_t *req_func,
              char **sso_token,
              am_web_req_method_t *orig_method,
              void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    const char *thisfunc = "get_sso_token()";
    am_status_t sts = AM_NOT_FOUND;
    am_web_req_method_t req_method = AM_WEB_REQUEST_UNKNOWN;

    // Get the sso token from cookie header
    sts = get_cookie_val(am_web_get_cookie_name(agent_config),
                            req_params->cookie_header_val, sso_token);
    if (sts != AM_SUCCESS && sts != AM_NOT_FOUND) {
        am_web_log_error("%s: Error while getting sso token from "
                         "cookie header: %s", thisfunc, 
                         am_status_to_string(sts));
    } else if (sts == AM_SUCCESS &&
             (*sso_token == NULL || (*sso_token)[0] == '\0')) {
        sts = AM_NOT_FOUND;
    }
    // If SSO token is not found and CDSSO mode is enabled
    // check for the request method.
    // If the method is POST, 
    //     1) set the request method to GET 
    //     2) try to get the SSO token from assertion
    if ((*agentConfigPtr)->cdsso_enable == AM_TRUE &&
           ((req_method = req_params->method) == AM_WEB_REQUEST_POST &&
           sts == AM_NOT_FOUND &&
           (am_web_is_url_enforced(req_params->url, req_params->path_info,
                    req_params->client_ip, agent_config) == B_TRUE))) {
        req_method = AM_WEB_REQUEST_GET;
        sts = process_cdsso(req_params, req_func, req_method, 
                            sso_token, agent_config);
        if (sts == AM_SUCCESS &&
             (*sso_token == NULL || (*sso_token)[0] == '\0')) {
            sts = AM_NOT_FOUND;
        }
        if (sts == AM_NOT_FOUND) {
            am_web_log_debug("%s: SSO token not found in "
                             "assertion. Redirecting to login page.",
                             thisfunc);
        } else if (sts == AM_SUCCESS) {
            am_web_log_debug("%s: SSO token found in assertion.",
                                  thisfunc);
        } else {
            am_web_log_error("%s: Error while getting sso token from "
                             "assertion: %s", thisfunc,
                             am_status_to_string(sts));
        }
    }
    return sts;
}

/**
 * do actual access check processing and get web result with redirect url
 * all input arguments should already be checked to be OK (not null, etc.)
 *
 * render_data is a buffer to store any data needed for rendering
 * HTTP response, such as a redirect url or a notification message response
 * to the OpenSSO server. It will be null terminated, so it will not
 * fill the buffer beyond render_data_size-1 bytes.
 * If the buffer is not big enough, an error message will be logged and
 * an internal error result will be returned.
 */
static am_web_result_t
process_request(am_web_request_params_t *req_params,
                am_web_request_func_t *req_func,
                char *data_buf, size_t data_buf_size, 
                void* agent_config)
{
    const char *thisfunc = "process_request()";
    am_status_t sts = AM_SUCCESS;
    am_web_result_t result = AM_WEB_RESULT_OK;
    char *sso_token = NULL;
    am_web_req_method_t orig_method = AM_WEB_REQUEST_UNKNOWN;
    am_status_t local_sts = AM_SUCCESS;
    am_map_t env_map = NULL;
    am_policy_result_t policy_result = AM_POLICY_RESULT_INITIALIZER;
    char *redirect_url = NULL;
    char *advice_response = NULL;
    void *args[1];
    boolean_t cdsso_enabled = am_web_is_cdsso_enabled(agent_config);
    // initialize reserved field to NULL
    req_params->reserved = NULL;

    // get sso token from either cookie header or assertion in cdsso mode.
    // OK if it's not found - am_web_is_access_allowed will check if
    // access is enforced.
    sts = get_sso_token(req_params, req_func, &sso_token, 
                        &orig_method, agent_config);
    if (sts != AM_SUCCESS && sts != AM_NOT_FOUND) {
        am_web_log_error("%s: Error while getting sso token from "
                         "cookie or cdsso assertion: %s",
                         thisfunc, am_status_to_string(sts));
        result = AM_WEB_RESULT_ERROR;
    } else if (am_map_create(&env_map) != AM_SUCCESS) {
        // Get ready to process access check - create map.
        am_web_log_error("%s: could not create map needed for "
                         "checking access.", thisfunc);
        result = AM_WEB_RESULT_ERROR;
    } else {
        // set orig_method to the method in the request, if
        // it has not been set to the value of the original method
        // query parameter from CDC servlet.
        if (orig_method == AM_WEB_REQUEST_UNKNOWN)
            orig_method = req_params->method;

        am_web_set_host_ip_in_env_map(req_params->client_ip,
                                      req_params->client_hostname,
                                      env_map,
                                      agent_config);

        // now check if access allowed
        sts = am_web_is_access_allowed(
                      sso_token, req_params->url, 
                      req_params->path_info,
                      am_web_method_num_to_str(orig_method),
                      req_params->client_ip, env_map, &policy_result,
                      agent_config);
        am_web_log_info("%s: Access check for URL %s returned %s.",
                        thisfunc, req_params->url, am_status_to_string(sts));
        // map access check result to web result - OK, FORBIDDEN, etc.
        switch(sts) {
            case AM_SUCCESS:  // Access to user allowed.
                // will be either OK or forbidden if sso token user failed
                // to be set in the web container.
                // Note - the method passed must be that of this request.
                result = process_access_success(req_params->url, 
                              policy_result, req_params, req_func,
                              agent_config);
                break;
            case AM_INVALID_SESSION:
                args[0] = req_func;
                // reset the CDSSO cookie first
                if (cdsso_enabled == B_TRUE) {
                    am_status_t cdStatus = am_web_do_cookie_domain_set(add_cookie_in_response, 
                                               args, 
                                               EMPTY_STRING, 
                                               agent_config);
                    if(cdStatus != AM_SUCCESS) {
                        am_web_log_error("process_request : CDSSO reset cookie failed");
                    }

                }
                // reset cookies on invalid session.
                local_sts = am_web_do_cookies_reset(add_cookie_in_response,
                                                    args, agent_config);
                if (local_sts != AM_SUCCESS) {
                    am_web_log_warning("%s: all_cookies_reset after "
                                "access to url [%s] returned "
                                "invalid session returned %s.",
                                thisfunc, req_params->url,
                                am_status_to_string(local_sts));
                }
                // will be either forbidden or redirect to auth
                result = process_access_redirect(req_params->url, orig_method,
                                sts, policy_result,
                                req_func,
                                &redirect_url,
                                NULL, agent_config);
                break;
            case AM_ACCESS_DENIED:
            case AM_INVALID_FQDN_ACCESS:
                result = process_access_redirect(req_params->url, orig_method,
                                sts, policy_result,
                                req_func,
                                &redirect_url,
                                &advice_response, agent_config);
                break;
            case AM_REDIRECT_LOGOUT:
                result = process_access_success(req_params->url, 
                              policy_result, req_params, req_func,
                              agent_config);
                result = process_access_redirect(req_params->url, 
                             orig_method,
                             sts, 
                             policy_result,
                             req_func,
                             &redirect_url,
                             NULL, 
                             agent_config);

                break;
            case AM_INVALID_ARGUMENT:
            case AM_NO_MEMORY:
            default:
                result = AM_WEB_RESULT_ERROR;
                break;
        }
        // clean up
        if (env_map != NULL) {
            am_map_destroy(env_map);
            env_map = NULL;
        }
        am_web_clear_attributes_map(&policy_result);
        am_policy_result_destroy(&policy_result);
        if (req_params->reserved != NULL) {
            free(req_params->reserved);
            req_params->reserved = NULL;
        }
        if (sso_token != NULL) {
            free(sso_token);
        }
        // copy redirect_url or advice response to the given buffer and free the pointer.
        // if size of buffer not big enough, return error.
        char* ptr = redirect_url;
        if (result == AM_WEB_RESULT_OK_DONE) {
            ptr = advice_response;
        }
        if (ptr != NULL) {
            if (data_buf_size < strlen(ptr)+1) {
                am_web_log_error("%s: size of render data buffer too small "
                "for pointer [%s]", thisfunc, ptr);
                result = AM_WEB_RESULT_ERROR;
            } else {
                strcpy(data_buf, ptr);
            }
        }
        if (redirect_url != NULL) {
            free(redirect_url);
        }
        if (advice_response != NULL) {
            free(advice_response);
        }
    }
    am_web_log_debug("%s: returning web result %s, data [%s]",
                  thisfunc, am_web_result_num_to_str(result), data_buf);
    return result;
}


extern "C" AM_WEB_EXPORT am_web_result_t
am_web_process_request(am_web_request_params_t *req_params,
                       am_web_request_func_t *req_func,
                       am_status_t *render_sts, 
                       void* agent_config)
{

    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;


    const char *thisfunc = "am_web_process_request()";
    am_status_t sts = AM_SUCCESS;
    am_web_result_t result = AM_WEB_RESULT_ERROR;
    // size of render_data_buf should be at least the length of
    // NOTIFICATION_OK string, and long enough to hold a redirect url.
    char data_buf[2048];

    memset(data_buf, 0, sizeof(data_buf));

    // Check arguments
    if (req_params == NULL || req_func == NULL || render_sts == NULL) {
	am_web_log_error("%s: one or more input arguments is NULL.",
			 thisfunc);
	result = AM_WEB_RESULT_ERROR;
    }
    // Check request parameters
    else if (req_params->url == NULL || req_params->url[0] == '\0') {
	am_web_log_error("%s: required request parameter url is %s.",
			thisfunc, req_params->url==NULL?"NULL":"empty");
	result = AM_WEB_RESULT_ERROR;
    }
    // Check request processing functions. The following are required and
    // the rest are optional.
    // a) in all cases:
    //	set_user
    //	render_result
    // b) in CDSSO mode - these will be checked in process_cdsso.
    //	get_post_data (if method is post)
    //	set_method (if method is post)
    //	one of set_header_in_request or set_cookie_in_request to set cookie
    //
    else if (req_func->set_user.func == NULL ||
             req_func->render_result.func == NULL) {
	am_web_log_error("%s: one of required request functions "
			 "set_user or render_result is null.",
			 thisfunc);
	result = AM_WEB_RESULT_ERROR;
    }
    // Process notification if it's a notification url
    else if (B_TRUE == am_web_is_notification(req_params->url,
                                              agent_config)) {
	if (NULL == req_func->get_post_data.func) {
	    am_web_log_warning("%s: Notification message received but "
			       "ignored because no get post data "
			       "function is found.", thisfunc);
	    result = AM_WEB_RESULT_OK_DONE;
	}
	else {
	    sts = process_notification(req_params->url,
				       req_params->method,
				       req_func->get_post_data,
				       req_func->free_post_data);
	    if (sts == AM_SUCCESS) {
		am_web_log_debug("%s: process notification url [%s] "
				 "completed successfully.",
				 thisfunc, req_params->url);
		// checked that size of data_buf is big enough for
		// the NOTIFICATION_OK message.
		strcpy(data_buf, NOTIFICATION_OK);
		result = AM_WEB_RESULT_OK_DONE;
	    } else {
		am_web_log_error("%s: process notification url [%s] "
				"completed with %s.",
				thisfunc, req_params->url,
				am_status_to_string(sts));
		result = AM_WEB_RESULT_ERROR;
	    }
	}
    }
    // Now process access check.
    else {
	result = process_request(req_params, req_func,
				 data_buf, sizeof(data_buf), agent_config);
    }

    // render web result
    am_web_log_debug("%s: Rendering web result %s",
		     thisfunc, am_web_result_num_to_str(result));
    sts = req_func->render_result.func(req_func->render_result.args,
				       result, data_buf);
    am_web_log_debug("%s: render result function returned %s.",
		     thisfunc, am_status_to_name(sts));
    if (sts != AM_SUCCESS) {
	am_web_log_error("%s: Error [%s] rendering web result, "
			 "resetting web result to ERROR.",
			 thisfunc, am_status_to_string(sts));
    }
    if (render_sts != NULL)
	*render_sts = sts;

    return result;
}

/**
 * cookie_header is the original cookie header.
 * set_cookie_value is the value of the "Set-Cookie".
 * new_cookie_header is a pointer to a buffer for the new cookie header.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_set_cookie(char *cookie_header, const char *set_cookie_value,
		  char **new_cookie_header)
{
    const char *thisfunc = "am_web_set_cookie()";
    am_status_t sts = AM_SUCCESS;
    char *equal_sign = NULL;
    char *semi_sign = NULL;
    if (set_cookie_value == NULL || new_cookie_header == NULL ||
	(equal_sign =
	    strchr(const_cast<char *>(set_cookie_value), '=')) == NULL ||
	(semi_sign = strchr(equal_sign+1, ';')) == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	while (isspace(*set_cookie_value))
	    set_cookie_value++;
	if (set_cookie_value == equal_sign) {
	    sts = AM_INVALID_ARGUMENT;
	}
	else {
#if defined(_AMD64_)
	    size_t cookie_name_len = equal_sign-set_cookie_value;
	    size_t cookie_val_len = semi_sign-equal_sign-1;
#else
	    unsigned int cookie_name_len = equal_sign-set_cookie_value;
	    unsigned int cookie_val_len = semi_sign-equal_sign-1;
#endif
	    char *cookie_name = (char *)malloc(cookie_name_len+1);
	    char *cookie_val = NULL;
	    if (cookie_name == NULL) {
		sts = AM_NO_MEMORY;
	    } else {
		strncpy(cookie_name, set_cookie_value, cookie_name_len);
		cookie_name[cookie_name_len] = '\0';
		if (cookie_val_len > 0) {
		    cookie_val = (char *)malloc(cookie_val_len+1);
		    if (cookie_val == NULL) {
			sts = AM_NO_MEMORY;
		    } else {
			strncpy(cookie_val, equal_sign+1, cookie_val_len);
			cookie_val[cookie_val_len]='\0';
		    }
		}
		if (sts == AM_SUCCESS) {
		    sts = set_cookie_in_cookie_header(cookie_header,
				cookie_name, cookie_val, new_cookie_header);
		    am_web_log_debug("%s: set cookie '%s' val '%s' in "
				     "header '%s' returned '%s'.",
				     thisfunc, cookie_name,
				     cookie_val==NULL?"(null)":cookie_val,
				     cookie_header,
				     *new_cookie_header==NULL?
					    "(null)":*new_cookie_header);
		}
		free(cookie_name);
		if (cookie_val != NULL)
		    free(cookie_val);
	    }
	}
    }
    return sts;
}
/*
 * Clear all the maps...
 */
extern "C" AM_WEB_EXPORT void
am_web_clear_attributes_map(am_policy_result_t *result)
{
    // Free up all the maps
    if (result->attr_profile_map != AM_MAP_NULL) {
        am_map_destroy(result->attr_profile_map);
        result->attr_profile_map = AM_MAP_NULL;
    }
    if (result->attr_session_map != AM_MAP_NULL) {
        am_map_destroy(result->attr_session_map);
        result->attr_session_map = AM_MAP_NULL;
    }
    if (result->attr_response_map != AM_MAP_NULL) {
        am_map_destroy(result->attr_response_map);
        result->attr_response_map = AM_MAP_NULL;
    }
}


extern "C" AM_WEB_EXPORT am_status_t
am_web_build_advice_response(const am_policy_result_t *policy_result,
			     const char *redirect_url,
			     char **advice_response) {
    am_status_t retVal = AM_SUCCESS;
    if(policy_result != NULL && advice_response != NULL &&
       redirect_url != NULL) {
	std::string msg = sector_one;
	msg.append(redirect_url);
	msg.append(sector_two);
	msg.append(sector_three);
	msg.append(COMPOSITE_ADVICE_KEY);
	msg.append(sector_four);
	std::string encoded_msg = Http::encode(policy_result->advice_string);
	msg.append(encoded_msg);
	msg.append(sector_two);
	msg.append(sector_five);
	*advice_response = (char *)malloc(msg.size() + 1);
	if(*advice_response != NULL) {
	    strcpy(*advice_response, msg.c_str());
	} else {
	    retVal = AM_NO_MEMORY;
	}
    } else {
	am_web_log_error("am_web_build_advice_response(): "
			 "Invalid parameters.");
	retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

/*authtype
 * Method to determine if the auth-type value "dsame"
 * in the IIS6 agent should be replaced by "Basic"
 */
extern "C" AM_WEB_EXPORT const char *
am_web_get_authType(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

	return (*agentConfigPtr)->authtype;
}

/*
 * Method to determine if the override_host_port is set
 * for the Proxy agent
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_proxy_override_host_port_set(void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    return (AM_TRUE==(*agentConfigPtr)->override_host_port) ? B_TRUE : B_FALSE;
}


/*
 * Method to get the IIS6 agent replay passwd key
 *
 */
extern "C" AM_WEB_EXPORT const char *
am_web_get_iis6_replaypasswd_key(void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    return (*agentConfigPtr)->iis6_replaypasswd_key;
}

/*
 * Method to check whether OWA is deployed on IIS6
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_owa_enabled(void* agent_config) {
    boolean_t status = B_FALSE;
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    if((*agentConfigPtr)->owa_enable == AM_TRUE) {
        status = B_TRUE;
    }
    return status;
}


/*
 * Method to convert http to https if OWA is deployed on IIS6
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_owa_enabled_change_protocol(void* agent_config) {
    boolean_t status = B_FALSE;
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    if((*agentConfigPtr)->owa_enable_change_protocol == AM_TRUE) {
        status = B_TRUE;
    }
    return status;
}


/*
 * Method to retrun the landing page for idle session 
 * timeouts when OWA is deployed on IIS6
 */
extern "C" AM_WEB_EXPORT const char *
am_web_is_owa_enabled_session_timeout_url(void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    return (*agentConfigPtr)->owa_enable_session_timeout_url;
}

/*
 * Method to check if IBM Lotus DOMINO Agent checks name database 
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_domino_check_name_database(void* agent_config) {
    boolean_t status = B_FALSE;
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    if((*agentConfigPtr)->check_name_database == AM_TRUE) {
        status = B_TRUE;
    }
    return status;
}


/*
 * Method to check if IBM Lotus DOMINO enables ltpa
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_domino_ltpa_enable(void* agent_config) {
    boolean_t status = B_FALSE;
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    if((*agentConfigPtr)->ltpa_enable == AM_TRUE) {
        status = B_TRUE;
    }
    return status;
}

/*
 * Method to return IBM Lotus DOMINO config name
 */
extern "C" AM_WEB_EXPORT const char *
am_web_domino_ltpa_config_name(void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    return (*agentConfigPtr)->ltpa_config_name;
}

/*
 * Method to return IBM Lotus DOMINO org name
 */
extern "C" AM_WEB_EXPORT const char *
am_web_domino_ltpa_org_name(void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    return (*agentConfigPtr)->ltpa_org_name;
}

/*
 * Method to return IBM Lotus DOMINO cookie name
 */
extern "C" AM_WEB_EXPORT const char *
am_web_domino_ltpa_token_name(void* agent_config) {
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    return (*agentConfigPtr)->ltpa_cookie_name;
}

/**
 * Method to return the latest instance of agent configuration 
 * from agent configuration cache.
 * The returned instance gets used to serve requests.
 */
extern "C" AM_WEB_EXPORT void*
am_web_get_agent_configuration() {
    void *agentC = NULL;
        if (agentProfileService != NULL) {
            agentC = new AgentConfigurationRefCntPtr(
                       agentProfileService->getAgentConfigInstance());
        }
    return agentC;
}

/**
 * Method to delete ref counted object associated with
 * the latest instance of agent configuration. 
 */
extern "C" AM_WEB_EXPORT void
am_web_delete_agent_configuration(void *agentC) {
    AgentConfigurationRefCntPtr* x = 
        (AgentConfigurationRefCntPtr*) agentC;
    delete(x);
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_cdsso_enabled(void* agent_config) {
    boolean_t status = B_FALSE;
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    if((*agentConfigPtr)->cdsso_enable == AM_TRUE) {
        status = B_TRUE;
    }

    return status;
}

extern "C" AM_WEB_EXPORT am_status_t
am_web_get_logout_url(char** logout_url, void* agent_config)
{
    const char *thisfunc = "am_web_get_logout_url()";
    am_status_t ret = AM_FAILURE;
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;

    Utils::url_info_t *url_info_ptr;
    std::string retVal;
    url_info_ptr = find_active_logout_server(agent_config);
    if (NULL == url_info_ptr) {
        am_web_log_warning("%s: unable to find active logout url"
                "Access Manager Auth server.", thisfunc);
        ret = AM_FAILURE;
    }
    else {
        retVal.append(url_info_ptr->url, url_info_ptr->url_len);

        //Append the logout redirect url
        if ((*agentConfigPtr)->logout_redirect_url != NULL )
        {
            if(retVal.find("?")!=string::npos){
                retVal.append("&");
            }
            else{
                retVal.append("?");
            }

            retVal.append("goto=");
            retVal.append((*agentConfigPtr)->logout_redirect_url);
        }

        am_web_log_debug("%s: active logout url= %s",thisfunc, retVal.c_str());
        ret = AM_SUCCESS;
    }

    *logout_url=strdup(retVal.c_str());

    return ret;
}

/**
 * Returns client.ip.header property value
 */
extern "C" AM_WEB_EXPORT const char *
am_web_get_client_ip_header_name(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    if (((*agentConfigPtr)->clientIPHeader != NULL) &&
        (strlen((*agentConfigPtr)->clientIPHeader) == 0))
    { 
        return NULL;
    } else {
        return (*agentConfigPtr)->clientIPHeader;
    }
}

/**
 * Returns client.hostname.header property value
 */
extern "C" AM_WEB_EXPORT const char *
am_web_get_client_hostname_header_name(void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    if (((*agentConfigPtr)->clientHostnameHeader != NULL) &&
        (strlen((*agentConfigPtr)->clientHostnameHeader) == 0))
    {
        return NULL;
    } else {
        return (*agentConfigPtr)->clientHostnameHeader;
    }
}

am_status_t getFirstValueOfList(const char *list, 
                                char separator,
                                char **firstValue)
{
    const char *thisfunc = "getFirstValueOfList()";
    am_status_t status = AM_SUCCESS;
    
    if ((list == NULL) || (strlen(list) == 0)) {
        am_web_log_error("%s: The list is null or empty.", thisfunc);
        status = AM_INVALID_ARGUMENT;
    }
    if (status == AM_SUCCESS) {
        std::string list_str(list);
        size_t separatorPos = list_str.find(separator);
        if (separatorPos == std::string::npos) {
            *firstValue = strdup(list_str.c_str());
        } else {
            *firstValue = strdup(list_str.substr(0,separatorPos).c_str());
        }
        if (*firstValue == NULL) {
            am_web_log_error("%s: Not enough memory to allocate firstValue.",
                             thisfunc);
            status = AM_NO_MEMORY;
        }
        if (strlen(*firstValue) == 0) {
            *firstValue = NULL;
        }
    }
    return status;
}

/*
 * Returns client IP and hostname value from client IP and hostname headers.
 * If the client IP header or client host name header contains comma 
 * separated values, then first value is taken into consideration.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_get_client_ip_host(const char *clientIPHeader, 
                          const char *clientHostHeader,
                          char **clientIP,
                          char **clientHost)
{
    const char *thisfunc = "am_web_get_client_ip_host()";
    am_status_t status = AM_SUCCESS;
    char *fullHostName = NULL;

     // If clientIPHeader contains a list of values, set clientIP
     // to the first value of the list
    if ((clientIPHeader != NULL) && (strlen(clientIPHeader) > 0)) {
        status = getFirstValueOfList(clientIPHeader, ',', &(*clientIP));
    }
    // If clientHostHeader contains a list of values, set the clientHost
    // to the first value of the list
    if (status == AM_SUCCESS) {
        if ((clientHostHeader != NULL) &&
            (strlen(clientHostHeader) > 0 )) 
        {
            status = getFirstValueOfList(clientHostHeader, ',',
                                         &fullHostName);
            // If fullHostName contains the port number, remove it
            if ((status == AM_SUCCESS) && (fullHostName != NULL)) {
                status = getFirstValueOfList(fullHostName, ':', &(*clientHost));
            }
        }
    }
    if (status == AM_SUCCESS) {
        if (*clientIP != NULL) {
            am_web_log_debug("%s: Processed client IP = %s",
                             thisfunc, *clientIP);
        } else {
            am_web_log_debug("%s: Processed client IP is NULL.",
                             thisfunc);
        }
        if (*clientHost != NULL) {
            am_web_log_debug("%s: Processed client hostname = %s", 
                             thisfunc, *clientHost);
        } else {
            am_web_log_debug("%s: Processed client hostname is NULL.", 
                             thisfunc);
        }
    }
    return status;
}

/**
 * Sets client ip (and client hostname) in environment map
 * which then sent as part of policy request.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_set_host_ip_in_env_map(const char *client_ip,
                              const char *client_hostname,
                              const am_map_t env_parameter_map,
                              void* agent_config)
{
    const char *thisfunc = "am_web_set_host_ip_in_env_map()";
    am_status_t status = AM_SUCCESS;
    AgentConfigurationRefCntPtr* agentConfigPtr =
          (AgentConfigurationRefCntPtr*) agent_config;
    PRStatus prStatus;
    PRNetAddr address;
    PRHostEnt hostEntry;
    char buffer[PR_NETDB_BUF_SIZE];

    if(client_ip != NULL && strlen(client_ip) > 0) {
        // Set the client IP in the environment map
        Log::log(boot_info.log_module, Log::LOG_DEBUG,
             "%s: map_insert: client_ip=%s", thisfunc, client_ip);
        am_map_insert(env_parameter_map, requestIp, client_ip, AM_TRUE);
        if(client_hostname != NULL && strlen(client_hostname) > 0) {
            // Set the hostname in the environment map if it is not null
            Log::log(boot_info.log_module, Log::LOG_DEBUG,
             "%s: map_insert: client_hostname=%s", thisfunc, client_hostname);
            status = am_map_insert(env_parameter_map, 
                                   requestDnsName,
                                   client_hostname, 
                                   AM_FALSE);
        } else if ((*agentConfigPtr)->getClientHostname) {
            prStatus = PR_StringToNetAddr(client_ip, &address);
            if (PR_SUCCESS == prStatus) {
                // Try to get the hostname through DNS reverse lookup
                prStatus = PR_GetHostByAddr(&address, buffer,
                                            sizeof(buffer), &hostEntry);
                if (PR_SUCCESS == prStatus) {
                    // this function will log info about the client's hostnames
                    // so no need to do it here.
                    getFullQualifiedHostName(env_parameter_map, 
                                             &address, &hostEntry);
                }
            } else {
                Log::log(boot_info.log_module, Log::LOG_DEBUG,
                         "%s: map_insert: could not get client's hostname for "
                         "policy. Error %s.", thisfunc,
                         PR_ErrorToString(PR_GetError(),
                                          PR_LANGUAGE_I_DEFAULT));
            }
        }
    }
    return status;
}

/*
 * Method to get the value of the set-cookie header for the lb cookie
 * when using a LB in front of the agent with post preservation
 * enabled
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_get_postdata_preserve_lbcookie(char **headerValue, 
                                      boolean_t isValueNull,
                                      void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    const char *thisfunc = "am_web_get_postdata_preserve_lbcookie()";
    am_status_t status = AM_SUCCESS;
    std::string header;
    std::string stickySessionValueStr;
    std::string cookieName, cookieValue;
    size_t equalPos = 0;
    const char *stickySessionMode = 
            (*agentConfigPtr)->postdatapreserve_sticky_session_mode;
    const char *stickySessionValue = 
           (*agentConfigPtr)->postdatapreserve_sticky_session_value;

    // If stickySessionMode or stickySessionValue is empty, 
    // then there is no LB in front of the agent.
    if ((stickySessionMode == NULL) || (strlen(stickySessionMode) == 0) ||
        (stickySessionValue == NULL) || (strlen(stickySessionValue) == 0))
    {
        status = AM_INVALID_ARGUMENT;
    } else if (strcmp(stickySessionMode, "COOKIE") != 0) {
        // Deals only with the case where the sticky session mode is COOKIE.
        status = AM_INVALID_ARGUMENT;
        if (strcmp(stickySessionMode, "URL") != 0) {
            am_web_log_warning("%s: %s is not a correct value for the property "
                             "config.postdata.preserve.stickysession.value.",
                             thisfunc, stickySessionMode);
        }
    }
    // Check if the sticky session value has a correct format ("param=value")
    if (status  == AM_SUCCESS) {
        stickySessionValueStr.assign(stickySessionValue);
        equalPos = stickySessionValueStr.find('=');
        if (equalPos != std::string::npos) {
            cookieName = stickySessionValueStr.substr(0, equalPos);
            cookieValue = stickySessionValueStr.substr(equalPos+1);
            if (cookieName.empty() || cookieValue.empty()) {
                am_web_log_warning("%s: The property "
                     "config.postdata.preserve.stickysession.value "
                     "(%s) does not a have correct format.",
                     thisfunc, stickySessionValueStr.c_str());
                status = AM_INVALID_ARGUMENT;
            }
        } else {
            am_web_log_warning("%s: The property "
                     "config.postdata.preserve.stickysession.value "
                     "(%s) does not have a correct format.",
                     thisfunc, stickySessionValueStr.c_str());
            status = AM_INVALID_ARGUMENT;
        }
    }
    if (status  == AM_SUCCESS) {
        if (isValueNull == B_TRUE) {
            cookieValue = "";
        }
        header = " ";
        header.append(cookieName).append("=").
               append(cookieValue).append(";Path=/");
        *headerValue = strdup(header.c_str());
        if (*headerValue == NULL) {
            am_web_log_error("%s: Not enough memory to allocate "
                             "the headerValue variable.", thisfunc);
             status = AM_NO_MEMORY;
        }
    }
    if (status == AM_SUCCESS) {
            am_web_log_debug("%s: Sticky session mode: %s", thisfunc, 
                       stickySessionMode);
            am_web_log_debug("%s: Sticky session value: %s", 
                             thisfunc, *headerValue);
    }
    return status;
}

/*
 * Method to get the query parameter that should be added to the
 * dummy url when using a LB in front of the agent with post 
 * preservation enabled and sticky session mode set to URL.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_get_postdata_preserve_URL_parameter(char **queryParameter,
                                           void* agent_config)
{
    AgentConfigurationRefCntPtr* agentConfigPtr =
        (AgentConfigurationRefCntPtr*) agent_config;
    const char *thisfunc = "am_web_get_postdata_preserve_URL_parameter()";
    am_status_t status = AM_SUCCESS;
    std::string stickySessionValueStr;
    std::string cookieName, cookieValue;
    size_t equalPos = 0;
    const char *stickySessionMode = 
            (*agentConfigPtr)->postdatapreserve_sticky_session_mode;
    const char *stickySessionValue = 
            (*agentConfigPtr)->postdatapreserve_sticky_session_value;
    
    // If stickySessionMode or stickySessionValue is empty, 
    // then there is no LB in front of the agent.
    if ((stickySessionMode == NULL) || (strlen(stickySessionMode) == 0) ||
        (stickySessionValue == NULL) || (strlen(stickySessionValue) == 0))
    {
        status = AM_INVALID_ARGUMENT;
    } else if (strcmp(stickySessionMode, "URL") != 0) {
        // Deals only with the case where the sticky session mode is URL.
        status = AM_INVALID_ARGUMENT;
        if (strcmp(stickySessionMode, "COOKIE") != 0) {
            am_web_log_warning("%s: %s is not a correct value for the property "
                             "config.postdata.preserve.stickysession.value.",
                             thisfunc, stickySessionMode);
        }
    }
    // Check if the sticky session value has a correct format ("param=value")
    if (status  == AM_SUCCESS) {
        stickySessionValueStr.assign(stickySessionValue);
        equalPos = stickySessionValueStr.find('=');
        if (equalPos != std::string::npos) {
            cookieName = stickySessionValueStr.substr(0, equalPos);
            cookieValue = stickySessionValueStr.substr(equalPos+1);
            if (cookieName.empty() || cookieValue.empty()) {
                am_web_log_warning("%s: The property "
                     "config.postdata.preserve.stickysession.value "
                     "(%s) does not a have correct format.",
                     thisfunc, stickySessionValueStr.c_str());
                status = AM_INVALID_ARGUMENT;
            }
        } else {
            am_web_log_warning("%s: The property "
                     "config.postdata.preserve.stickysession.value "
                     "(%s) does not have a correct format.",
                     thisfunc, stickySessionValueStr.c_str());
            status = AM_INVALID_ARGUMENT;
        }
    }
    if (status == AM_SUCCESS) {
        *queryParameter = strdup(stickySessionValueStr.c_str());
        if (*queryParameter == NULL) {
            am_web_log_error("%s: Not enough memory to allocate "
                             "the queryParameter variable.",
                             thisfunc);
            status = AM_NO_MEMORY;
        }
    }
    if (status == AM_SUCCESS) {
            am_web_log_debug("%s: Sticky session mode: %s", thisfunc, 
                             stickySessionMode);
            am_web_log_debug("%s: Sticky session value: %s", 
                             thisfunc, *queryParameter);
    }
    return status;
}

#if defined(WINNT)
AM_BEGIN_EXTERN_C
BOOL WINAPI DllMain(HINSTANCE hInst, DWORD fdwReason, LPVOID lpvReserved) {
        if(fdwReason == DLL_PROCESS_ATTACH) {
		hInstance = hInst;
        }
        return TRUE;
}
AM_END_EXTERN_C
#endif
