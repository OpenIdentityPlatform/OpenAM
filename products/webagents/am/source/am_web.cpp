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
#include "http.h"
#include "key_value_map.h"
#include "p_cache.h"
#include "fqdn_handler.h"
#include "xml_tree.h"
#include "url.h"
#include "utils.h"

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

#if defined _MSC_VER
#include <winsock.h>
#include <intrin.h>
#if !defined(strtok_r)
#define strtok_r strtok_s
#endif
typedef unsigned __int32 uint32_t;
#endif

#if	!defined(FALSE)
#define FALSE	0
#endif
#if	!defined(TRUE)
#define TRUE	1
#endif

USING_PRIVATE_NAMESPACE
#define bool_to_am_bool_t(x) (x?AM_TRUE:AM_FALSE)
#define	HTTP_PREFIX	"http://"
#define	HTTP_PREFIX_LEN	(sizeof(HTTP_PREFIX) - 1)
#define	HTTP_DEF_PORT	80
#define	HTTPS_PREFIX	"https://"
#define	HTTPS_PREFIX_LEN (sizeof(HTTPS_PREFIX) - 1)
#define HTTPS_DEF_PORT	443
#define MSG_MAX_LEN 1024
#define AM_REVISION_LEN 10

/*
 * Names of the various advices that we need to process.
 */
#define	AUTH_SCHEME_KEY			"AuthSchemeConditionAdvice"
#define	AUTH_SCHEME_URL_PREFIX		"&module="
#define	AUTH_SCHEME_URL_PREFIX_LEN	(sizeof(AUTH_SCHEME_URL_PREFIX) - 1)
#define	AUTH_LEVEL_KEY			"AuthLevelConditionAdvice"
#define	AUTH_LEVEL_URL_PREFIX		"&authlevel="
#define	AUTH_LEVEL_URL_PREFIX_LEN	(sizeof(AUTH_LEVEL_URL_PREFIX) - 1)
#define	AUTH_REALM_KEY			"AuthenticateToRealmConditionAdvice"
#define	AUTH_REALM_URL_PREFIX		"&realm="
#define	AUTH_REALM_URL_PREFIX_LEN	(sizeof(AUTH_REALM_URL_PREFIX) - 1)
#define	AUTH_SERVICE_KEY		"AuthenticateToServiceConditionAdvice"

/*
 * How long to wait in attempting to connect to an Access Manager AUTH server.
 */
#define CONNECT_TIMEOUT	2

/*
 * POST preservation related strings
*/
#define MAGIC_STR                 "sunpostpreserve"
#define DUMMY_NOTENFORCED         "/dummypost*"
#define DUMMY_REDIRECT            "/dummypost/"
#define POSTHASHTBL_INITIAL_SIZE  31

#define COMPOSITE_ADVICE_KEY    "sunamcompositeadvice"
#define SESSION_COND_KEY        "SessionConditionAdvice"

/*
   Liberty Alliance Protocol Related Strings.
 */
#define SAML_PROTOCOL_MAJOR_VERSION  "1"
#define SAML_PROTOCOL_MINOR_VERSION  "0"
#define DEFAULT_ENCODING             "UTF-8"
#define LIB_PREFIX                   "lib:"
#define PROTOCOL_PREFIX              "samlp:"
#define LIB_NAMESPACE_STRING         " xmlns:lib=\"http://projectliberty.org/schemas/core/2002/12\""
#define PROTOCOL_NAMESPACE_STRING    " xmlns:samlp=\"urn:oasis:names:tc:SAML:1.0:protocol\""
#define ELEMENT_ASSERTION            "Assertion"
#define ELEMENT_AUTHN_STATEMENT      "AuthenticationStatement"
#define ELEMENT_SUBJECT              "Subject"
#define ELEMENT_NAME_IDENTIFIER      "IDPProvidedNameIdentifier"

// notification response body.
#define NOTIFICATION_OK		       "OK\r\n"
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

static const char *am_70_revision_number = "7.0";
static const char *am_63_revision_number = "6.3";

static const char *requestIp = "requestIp";
static const char *requestDnsName = "requestDnsName";

typedef enum {
    SET_ATTRS_NONE,
    SET_ATTRS_AS_HEADER,
    SET_ATTRS_AS_COOKIE
} set_user_attrs_mode_t;

static const char * attrCookiePrefix = "HTTP_";
static const char * attrCookieMaxAge = "300";
static const char * profileMode = "NONE";
static const char * sessionMode = "NONE";
static const char * responseMode = "NONE";
static const char * sessionAttributes = "SESSION_ATTRIBUTES";
static const char * responseAttributes = "RESPONSE_ATTRIBUTES";
static set_user_attrs_mode_t setUserProfileAttrsMode = SET_ATTRS_NONE;
static set_user_attrs_mode_t setUserSessionAttrsMode = SET_ATTRS_NONE;
static set_user_attrs_mode_t setUserResponseAttrsMode = SET_ATTRS_NONE;
static std::list<std::string> attrList;
static const char * attrMultiValueSeparator = "|";
static const char * sunwErrCode = "sunwerrcode";

/*
 * For logging access to remote IS
 */
#define LOG_TYPE_NONE    "LOG_NONE"
#define LOG_TYPE_ALLOW   "LOG_ALLOW"
#define LOG_TYPE_DENY    "LOG_DENY"
#define LOG_TYPE_BOTH    "LOG_BOTH"

#define LOG_ACCESS_NONE    0x0
#define LOG_ACCESS_ALLOW   0x1
#define LOG_ACCESS_DENY    0x2

/**
 * instance
 */
static int initialized = AM_FALSE;

typedef struct url_info {
    char *url;
    size_t url_len;
    char *protocol;
    char *host;
    unsigned short port;
    am_bool_t has_parameters;
    am_bool_t has_patterns;
} url_info_t;

typedef struct url_info_list {
    unsigned int size;
    url_info_t *list;
} url_info_list_t;


/**
  * POST data structure to hold name value pair
*/
typedef struct name_value_pair {
    char *name;
    char *value;
} name_value_pair_t;


/**
  * POST data structure to hold an array of name value pairs
*/
typedef struct post_struct {
    char *buffer;
    name_value_pair_t *namevalue;
    int count;
} post_struct_t;


/**
  * Data Structure to hold Cookie information passed to set_cookie function.
  */
typedef struct {
    char *name;	    // name of cookie.
    char *value;    // value of cookie.
    char *domain;   // cookie domain, or NULL if no domain.
    char *path;    // cookie path, or NULL if no path.
    char *max_age;  // max age, or NULL if no max age.
    PRBool isSecure;  //if cookie is secure or not
} cookie_info_t;

typedef struct cookie_info_list {
    unsigned int size;
    cookie_info_t *list;
} cookie_info_list_t;

#define URL_INFO_PTR_NULL		((url_info_t *) NULL)
#define URL_INFO_LIST_INITIALIZER	{ 0, URL_INFO_PTR_NULL }
#define URL_INFO_INITIALIZER {NULL, 0, NULL, 0, AM_FALSE }

#define COOKIE_INFO_PTR_NULL ((cookie_info_t *) NULL)
#define COOKIE_INFO_INITIALIZER {NULL, NULL, NULL, NULL, NULL, AM_FALSE}
#define COOKIE_INFO_LIST_INITIALIZER {0, COOKIE_INFO_PTR_NULL}

#define IIS_FILTER_PRIORITY  "DEFAULT"

extern "C" int decrypt_base64(const char *, char *);
extern "C" int decode_base64(const char *, char *);

typedef std::set< std::vector< string > > ipAddrSet_t;

/**
 * Access Manager Policy agent
 */
typedef struct agent_info_t {
    am_log_module_id_t log_module;
    am_log_module_id_t remote_LogID;
    am_properties_t properties;
    url_info_list_t not_enforced_list;
    ipAddrSet_t *not_enforce_IPAddr;
    PRBool reverse_the_meaning_of_not_enforced_list;
    PRBool ignore_policy_evaluation_if_notenforced;
    PRBool do_sso_only;
    const char *instance_name;
    const char *cookie_name;
    size_t cookie_name_len;
    PRBool is_cookie_secure;
    const char *access_denied_url;
    const char *error_page_url;
    PRLock *lock;
    url_info_list_t login_url_list;
    url_info_list_t cdsso_server_url_list;
    PRBool notification_enabled;
    const char *notification_url;
    PRBool url_comparison_ignore_case;
    am_policy_t policy_handle;
    unsigned long postcacheentry_life;
    PostCache *postcache_handle;
    PRBool postdatapreserve_enabled;
    const char *postdatapreserve_sticky_session_mode;
    const char *postdatapreserve_sticky_session_value;
    const char *url_redirect_param;
    const char *user_id_param;
    const char *authLogType_param;
#if (defined(WINNT) || defined(_AMD64_))
    HINSTANCE hInst;
#endif
    const char *locale;
    const char *unauthenticated_user;
    PRBool anon_remote_user_enable;
    PRBool check_client_ip;

    PRBool fqdn_check_enable;
    FqdnHandler *fqdn_handler;
    const char *fqdn_default;
    size_t fqdn_default_len;

    PRBool cookie_reset_enabled;
    std::set<std::string> *cookie_domain_list;
    PRBool cdsso_enabled;
    PRBool useSunwMethod;
    cookie_info_list_t cookie_list;
    const char *cookie_reset_default_domain;
    url_info_t agent_server_url;
    url_info_list_t logout_url_list;
    cookie_info_list_t logout_cookie_reset_list;
    PRBool getClientHostname;
    unsigned log_access_type;
    PRBool denyOnLogFailure;
    PRBool convert_mbyte;
    PRBool encode_url_special_chars;
    PRBool encode_cookie_special_chars;
    PRBool override_protocol;	// whether to override protocol in request url
    PRBool override_host;	// whether to override host in request url
    PRBool override_port;	// whether to override port in request url
    PRBool override_notification_url;  // whether to override the notification
                                       // url the same way as other rq urls
    PRBool ignore_path_info;
    PRBool ignore_path_info_for_not_enforced_list;
    unsigned long connection_timeout;   //connection timeout in sec to check if active login server alive
    PRBool ignore_server_check;	// ignore server check before redirection
    PRBool use_redirect_for_advice;	// use redirect instead of POST for advice
    PRBool remove_sunwmethod;	// remove sunwMethod from query string in CDSSO
    const char *authtype;   //value of authtype in IIS6 agent
    char *am_revision_number;	// AM revision number    
    const char *iis6_replaypasswd_key; // IIS6 replay passwd key
    const char *filter_priority; //IIS 5 filter priority
    PRBool owa_enabled;	// OWA enabled in IIS6
    PRBool owa_enabled_change_protocol;	// OWA enabled change protocol in IIS6
    const char *owa_enabled_session_timeout_url; // OWA enabled session timeout url
    PRBool no_child_thread_activation_delay;
    unsigned long policy_clock_skew; // Policy Clock Skew
    const char *clientIPHeader;
    const char *clientHostnameHeader;
    const char *notenforcedIPmode;/*notenforced ip handling mode*/
    std::set<std::string> *not_enforce_IPAddr_set;
} agent_info_t;

static agent_info_t agent_info = {
    AM_LOG_ALL_MODULES,	    // log_module
    AM_LOG_ALL_MODULES,	    // remote_LogID
    AM_PROPERTIES_NULL,	    // properties
    URL_INFO_LIST_INITIALIZER,	// not_enforced_list
    NULL,		    // not_enforce_IPAddr
    AM_FALSE,		    // reverse_the_meaning_of_not_enforced_list
    AM_FALSE,		    // ignore_policy_evaluation_if_notenforced
    AM_FALSE,		    // do_sso_only
    NULL,		    // instance_name
    NULL,		    // cookie_name
    0,			    // cookie_name_len
    AM_FALSE,		    // is_cookie_secure
    NULL,		    // access_denied_url
    NULL,		    // error_page_url
    (PRLock *) NULL,	    // lock
    URL_INFO_LIST_INITIALIZER,	// login_url_list
    URL_INFO_LIST_INITIALIZER,	// cdsso_server_url_list
    AM_FALSE,		    // notification enabled
    NULL,		    // notification_url
    false,		    // url_comparison_ignore_case
    (unsigned int) -1,	    // policy_handle
    0,			    // postcacheentry_life
    NULL,		    // postcache_handle
    AM_TRUE,	    // postdatapreserve_enabled
    NULL,		    // postdatapreserve_sticky_session_value
    NULL,		    // postdatapreserve_sticky_session_mode
    NULL,		    // url_redirect_param
    NULL,		    // user_id_param
    NULL,		    // authLogType_param
#if (defined(WINNT) || defined(_AMD64_))
    NULL,		    // hInst
#endif
    NULL,		    // locale
    NULL,		    // unauthenticated_user
    AM_FALSE,		    // anon_remote_user_enable
    AM_FALSE,		    // check_client_ip
    AM_TRUE,		    // fqdn_check_enable
    NULL,		    // fqdn_handler
    NULL,		    // fqdn_default
    0,			    // fqdn_default_len
    AM_FALSE,		    // cookie_reset_enabled
    NULL,		    // cookie_domain_list
    AM_FALSE,		    // cdsso_enabled
    AM_FALSE,		    // useSunwMethod
    COOKIE_INFO_LIST_INITIALIZER,   // cookie_list
    NULL,		    // cookie_reset_default_domain
    URL_INFO_INITIALIZER,   // agent_server_url
    URL_INFO_LIST_INITIALIZER,	// logout_url_list
    COOKIE_INFO_LIST_INITIALIZER,   // logout_cookie_reset_list
    AM_FALSE,		    // getClientHostname
    (unsigned int)-1,	    // log_access_type
    AM_FALSE,		    // denyOnLogFailure
    AM_FALSE,		    // convert_mbyte
    AM_FALSE,		    // encode_url_special_chars
    AM_FALSE,		    // encode_cookie_special_chars
    AM_FALSE,		    // override_protocol
    AM_FALSE,		    // override_host
    AM_FALSE,		    // override_port
    AM_FALSE,		    // override_notification_url
    AM_FALSE,		   // ignore_path_info
    AM_TRUE,		   // ignore_path_info_for_not_enforced_list
    0,			    // connection_timeout
    AM_FALSE,		    // used by ignore_server_check
    AM_FALSE,		    // used by use_redirect_for_advice
    AM_FALSE,        //used by remove_sunwmethod
    NULL,                    // authtype in iis agent
    NULL,		    // AM revision number
    NULL,		    // IIS6 Replay passwd key
    IIS_FILTER_PRIORITY,     // IIS5 default priority
    AM_FALSE,			//owa enabled
    AM_FALSE,			//owa enabled change protocol
    NULL,			    // owa enabled session timeout url
    AM_FALSE,		    // no child thread activation delay
    0,				    // Policy Clock Skew
    NULL,               // Client IP header name
    NULL,                // Client host name header name
    NULL,//notenforced parser mode
    NULL//notenforced value set (new)
};

PRBool no_child_thread_activation_delay = AM_FALSE;
unsigned long policy_clock_skew = 0;
int postDataPreserveKey = 0;

/**
 *                    -------- helper functions --------
 */

#define IPCOMP(addr, n) ((addr >> (24 - 8 * n)) & 0xFF)

static inline int am_floor(const double x) {
    return x < 0 ? (int) x == x ? (int) x : (int) x - 1 : (int) x;
}

static const uint32_t CIDR2MASK[] = {0x00000000, 0x80000000,
    0xC0000000, 0xE0000000, 0xF0000000, 0xF8000000, 0xFC000000,
    0xFE000000, 0xFF000000, 0xFF800000, 0xFFC00000, 0xFFE00000,
    0xFFF00000, 0xFFF80000, 0xFFFC0000, 0xFFFE0000, 0xFFFF0000,
    0xFFFF8000, 0xFFFFC000, 0xFFFFE000, 0xFFFFF000, 0xFFFFF800,
    0xFFFFFC00, 0xFFFFFE00, 0xFFFFFF00, 0xFFFFFF80, 0xFFFFFFC0,
    0xFFFFFFE0, 0xFFFFFFF0, 0xFFFFFFF8, 0xFFFFFFFC, 0xFFFFFFFE,
    0xFFFFFFFF};

static inline uint32_t max_block(uint32_t w) {
    uint32_t res;
    res = 32;
    while (res > 0) {
        uint32_t mask = CIDR2MASK[ res - 1 ];
        uint32_t maskedBase = w & mask;
        if (maskedBase != w) {
            break;
        }
        res--;
    }
    return res;
}

static boolean_t notenforced_ip_cidr_match(const char *ip, std::set<std::string> *list);

static boolean_t notenforced_ip_range_cidr_match(const char *ip, std::string const& str) {
    uint32_t start, end;
    char *st = NULL, *en = NULL, *p = NULL, *p_buf = NULL;
    char buf[128];
    char tbf[19];
    std::set<std::string> all;
    memset(buf, 0, sizeof (buf));
    strncpy(buf, str.c_str(), sizeof (buf) - 1);
    if ((p = strtok_r(buf, "-", &p_buf)) == NULL) {
        return B_FALSE;
    }
    st = strdup(p);
    if ((start = inet_addr(p)) == -1) {
        free(st);
        return B_FALSE;
    }
    if ((p = strtok_r(NULL, "-", &p_buf)) != NULL) {
        en = strdup(p);
        if ((end = inet_addr(p)) == -1) {
            free(st);
            free(en);
            return B_FALSE;
        }
    } else {
        free(st);
        return B_FALSE;
    }
    start = ntohl(start);
    end = ntohl(end);
    while (end >= start) {
        int maxsize = max_block(start);
        double x = log(end - start + 1.0) / log(2.0);
        int maxdiff = (char) (32 - am_floor(x));
        if (maxsize < maxdiff) {
            maxsize = maxdiff;
        }
        memset(tbf, 0, sizeof (tbf));
        if (snprintf(tbf, sizeof (tbf), "%u.%u.%u.%u/%i", IPCOMP(start, 0), IPCOMP(start, 1), IPCOMP(start, 2), IPCOMP(start, 3), maxsize) != -1) {
            all.insert(std::string(tbf));
        }
        start += ((32 - maxsize) != 0) ? 2 << ((32 - maxsize) - 1) : 1;
    }
    free(st);
    free(en);
    if (am_web_is_max_debug_on()) {
        am_web_log_max_debug("notenforced_ip_range_cidr_match(): IP range %s is transformed to the following CIDR list:", str.c_str());
        for (std::set<std::string>::const_iterator iplv = all.begin(); iplv != all.end(); ++iplv) {
            std::string const& sv = *iplv;
            am_web_log_max_debug("[%s]", sv.c_str());
        }
    }
    if (notenforced_ip_cidr_match(ip, &all) == B_TRUE) {
        return B_TRUE;
    }
    return B_FALSE;
}

static boolean_t notenforced_ip_cidr_match(const char *ip, std::set<std::string> *list) {
    int mask;
    uint32_t t, s, e, ipt;
    uint32_t lx;
    char *p = NULL, *p_buf = NULL;
    char buf[64];
    /*check if CIDR value list is not empty or IP address sent in is valid IP address*/
    if (list == NULL || list->empty() || (ipt = inet_addr(ip)) == -1) {
        return B_FALSE;
    }
    for (std::set<std::string>::const_iterator iplv = list->begin(); iplv != list->end(); ++iplv) {
        std::string const& str = *iplv;
        if (strchr(str.c_str(), '-') != NULL && strchr(str.c_str(), '/') == NULL) {
            /* we support IP range only in a following notation
             *           192.168.1.1-192.168.2.3
             **/
            if (notenforced_ip_range_cidr_match(ip, str) == B_TRUE) {
                return B_TRUE;
            } else {
                continue;
            }
        }
        /*clean out buffer*/
        memset(buf, 0, sizeof (buf));
        /*copy CIDR value from set element to buffer*/
        strncpy(buf, str.c_str(), sizeof (buf) - 1);
        if ((p = strtok_r(buf, "/", &p_buf)) == NULL) {
            continue;
        }
        if ((lx = inet_addr(p)) == -1) {
            continue;
        }
        if ((p = strtok_r(NULL, "/", &p_buf)) != NULL) {
            mask = atoi(p);
            if (mask < 0 || mask > 32) {
                /* invalid mask */
                am_web_log_error("notenforced_ip_cidr_match(): invalid mask [%d] for range [%s]", mask, str.c_str());
                continue;
            }
        } else {
            /* single IP address*/
            mask = 32;
        }
        lx = htonl(lx);
        t = htonl(ipt);
        s = (lx & (~((1 << (32 - mask)) - 1) & 0xFFFFFFFF));
        e = (lx | (((1 << (32 - mask)) - 1) & 0xFFFFFFFF));
        if (t >= s && t <= e) {
            am_web_log_debug("notenforced_ip_cidr_match(): found ip [%s] in range [%s]", ip, str.c_str());
            return B_TRUE;
        } else {
            am_web_log_debug("notenforced_ip_cidr_match(): ip [%s] is not in range [%s]", ip, str.c_str());
        }
    }
    return B_FALSE;
}

/*
 *------------------------------- FQDN HANDLER FUNCTIONS ---------------------------
 */

static am_status_t
unload_fqdn_handler()
{
    am_status_t result = AM_FAILURE;

    try {
        if (agent_info.fqdn_handler != NULL) {
            delete agent_info.fqdn_handler;
        }
        result = AM_SUCCESS;
    } catch (...) {
        am_web_log_error("unload_fqdn_handler() failed with exception");
    }

    return result;
}

/* Throws std::exception's from fqdn_handler functions */
static am_status_t
load_fqdn_handler(bool ignore_case)
{
    am_status_t result = AM_FAILURE;

    try {
        const Properties *properties = reinterpret_cast<Properties *>(
						agent_info.properties);
        agent_info.fqdn_handler = new FqdnHandler(*properties,
						  ignore_case,
						  agent_info.log_module);
        result = AM_SUCCESS;
     } catch (...) {
        am_web_log_error("load_fqdn_handler() failed with exception");
     }

     return result;
}

/* Throws std::exception's from fqdn_handler functions */
inline am_bool_t is_valid_fqdn_access(const char *url) {
    if(AM_TRUE == agent_info.fqdn_check_enable) {
        return (agent_info.fqdn_handler->isValidFqdnResource(url)?AM_TRUE:AM_FALSE);
    } else {
	return AM_TRUE;
    }
}

/*
 *------------------------------- FQDN HANDLER FUNCTIONS OVER ----------------------
 */

static void cleanup_url_info_list(url_info_list_t *url_list)
{
    unsigned int i;

    if (url_list->list != NULL) {
	for (i = 0; i < url_list->size; i++) {
	    if (url_list->list[i].url != NULL) {
		free(url_list->list[i].url);
		url_list->list[i].url = NULL;
	    }
	    if (url_list->list[i].host != NULL) {
		free(url_list->list[i].host);
		url_list->list[i].host = NULL;
	    }
	}
	free(url_list->list);
	url_list->list = NULL;
    }

    url_list->size = 0;
}


void cleanup_cookie_info(cookie_info_t *cookie_data)
{
    if (cookie_data != NULL) {
	if (cookie_data->name != NULL) {
	    free(cookie_data->name);
	    cookie_data->name = NULL;
	}
	if (cookie_data->value != NULL) {
	    free(cookie_data->value);
	    cookie_data->value = NULL;
	}
	if (cookie_data->domain != NULL) {
	    free(cookie_data->domain);
	    cookie_data->domain = NULL;
	}
	if (cookie_data->max_age != NULL) {
	    free(cookie_data->max_age);
	    cookie_data->max_age = NULL;
	}
	if (cookie_data->path != NULL) {
	    free(cookie_data->path);
	    cookie_data->path = NULL;
	}
    }
}

void populate_am_resource_traits(am_resource_traits_t &rsrcTraits) {
    rsrcTraits.cmp_func_ptr = &am_policy_compare_urls;
    rsrcTraits.has_patterns = &am_policy_resource_has_patterns;
    rsrcTraits.get_resource_root = &am_policy_get_url_resource_root;
    rsrcTraits.separator = '/';
    rsrcTraits.ignore_case =
	(agent_info.url_comparison_ignore_case) ? B_TRUE : B_FALSE;
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

static void cleanup_cookie_info_list(cookie_info_list_t *cookie_list)
{
   unsigned int i;

   if (cookie_list != NULL) {
        for (i = 0; i < cookie_list->size; i++) {
            cleanup_cookie_info(&(cookie_list->list[i]));
        }
        free(cookie_list->list);
   }
   cookie_list->list = NULL;
   cookie_list->size = 0;
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
	    Log::log(agent_info.log_module, Log::LOG_DEBUG,
		"getFullQualifiedHostName: map_insert: "
		"hostname=%s", hostName);
	    am_map_insert(env_parameter_map, requestDnsName,
		hostName, AM_FALSE);
	}

	alias = hostEntry->h_aliases[i++];
	while (alias) {
	    Log::log(agent_info.log_module, Log::LOG_DEBUG,
		"getFullQualifiedHostName: map_insert: "
		"alias=%s", alias);
	    am_map_insert(env_parameter_map, requestDnsName,
		alias, AM_FALSE);
	    alias = hostEntry->h_aliases[i++];
	}
    }
}

#if (defined(WINNT) || defined(_AMD64_))
#include "resource.h"
void get_string(UINT key, char *buf, size_t buflen) {

    if (buf != NULL) {
	if (LoadString(agent_info.hInst, key, buf, buflen) == 0) {
	    buf[0] = '\0';
	    return;
	}
    }
    return;
}
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
    Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
	"i18n using native encoding %s.", native_encoding);
    iconv_t encoder = iconv_open(native_encoding,  "UTF-8" );
    if (encoder == (iconv_t)-1) {
	 /*
	  * iconv_open failed
	  */
	 Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
		  "iconv_open failed");
	 strcpy(dest_str, origstr);
     } else {
	/* Perform iconv conversion */
	Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
		 "i18n b4 convlen = %d  size = %d", len, size);
#if defined(LINUX_64) || defined(LINUX)
        int ret = iconv(encoder, &origstr, (size_t*)&len, &dest_str, (size_t*)&size);
#else
        int ret = iconv(encoder, &origstr, &len, &dest_str, &size);
#endif
	Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
		 "i18n len = %d  size = %d", len, size);
	if (ret < 0) {
	    Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
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
static void cleanup_properties(agent_info_t *info_ptr)
{
    if (info_ptr->properties != AM_PROPERTIES_NULL) {
	am_properties_destroy(info_ptr->properties);
	info_ptr->properties = AM_PROPERTIES_NULL;
    }

    cleanup_url_info_list(&info_ptr->not_enforced_list);
    info_ptr->instance_name = NULL;
    info_ptr->cookie_name = NULL;
    info_ptr->cookie_name_len = 0;
    cleanup_url_info_list(&info_ptr->cdsso_server_url_list);
    info_ptr->access_denied_url = NULL;
    if (((PRLock *) NULL) != info_ptr->lock) {
	PR_DestroyLock(info_ptr->lock);
	info_ptr->lock = (PRLock *) NULL;
    }
    if (info_ptr->not_enforce_IPAddr != NULL) {
	delete info_ptr->not_enforce_IPAddr;
	info_ptr->not_enforce_IPAddr = NULL;
    }
    cleanup_url_info_list(&info_ptr->login_url_list);
    info_ptr->notification_url = NULL;
    info_ptr->unauthenticated_user = NULL;

    info_ptr->url_redirect_param = NULL;
    info_ptr->user_id_param = NULL;
    info_ptr->authLogType_param = NULL;
    info_ptr->fqdn_default = NULL;
    info_ptr->fqdn_default_len = 0;

    if (info_ptr->cookie_domain_list != NULL) {
	delete info_ptr->cookie_domain_list;
	info_ptr->cookie_domain_list = NULL;
    }

    cleanup_cookie_info_list(&info_ptr->cookie_list);
    info_ptr->cookie_reset_default_domain = NULL;

    if (info_ptr->agent_server_url.url != NULL) {
	free(info_ptr->agent_server_url.url);
	info_ptr->agent_server_url.url = NULL;
    }
    if (info_ptr->agent_server_url.host != NULL) {
	free(info_ptr->agent_server_url.host);
	info_ptr->agent_server_url.host = NULL;
    }

	if (info_ptr->notification_url) {
		free((void *)info_ptr->notification_url);
		info_ptr->notification_url = NULL;
	}
	if (info_ptr->am_revision_number) {
		free((void *)info_ptr->am_revision_number);
		info_ptr->am_revision_number = NULL;
	}
    info_ptr->iis6_replaypasswd_key = NULL;
    info_ptr->owa_enabled_session_timeout_url = NULL;
    info_ptr->clientIPHeader = NULL;
    info_ptr->clientHostnameHeader = NULL;
    info_ptr->notenforcedIPmode = NULL;
    if (info_ptr->not_enforce_IPAddr_set != NULL) {
	delete info_ptr->not_enforce_IPAddr_set;
	info_ptr->not_enforce_IPAddr_set = NULL;
    }
}

static am_bool_t is_server_alive(const url_info_t *info_ptr)
{
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
		timeout = (unsigned) (agent_info.connection_timeout);

		if (((PRFileDesc *) NULL) != tcpSocket) {
		Log::log(agent_info.log_module, Log::LOG_DEBUG,
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
		    Log::log(agent_info.log_module, Log::LOG_ERROR,
			     "is_server_alive(): NSPR Error while calling "
			     "PR_Close(): %d.", error);
	        }
	    }
	}
    }

    return status;
}

static url_info_t *find_active_login_server(agent_info_t *info_ptr) {
    url_info_t *result = URL_INFO_PTR_NULL;
    unsigned int i = 0;
    url_info_list_t *url_list = NULL;

    if(initialized == AM_TRUE) {
	PR_Lock(info_ptr->lock);

	if(agent_info.cdsso_enabled) {
	    url_list = &info_ptr->cdsso_server_url_list;
	} else {
	    url_list = &info_ptr->login_url_list;
	}

	if (agent_info.ignore_server_check == AM_FALSE) {
	    for (i = 0; i < url_list->size; ++i) {
		    am_web_log_max_debug("find_active_login_server(): "
		    "Trying server: %s", url_list->list[i].url);
		    if (is_server_alive(&url_list->list[i])) {
			    result = &url_list->list[i];
			    break;
		    }
	    }
	} else {
	    result = &url_list->list[i];
	}

	PR_Unlock(info_ptr->lock);
    } else {
	am_web_log_error("find_active_login_server(): "
			 "Library not initialized.");
    }

    return result;
}


/* Throws std::exception's from string methods */
void parseIPAddresses(const std::string &property,
		      ipAddrSet_t &ipAddrSet )
{
    size_t space = 0, curPos = 0;
    std::string iplist(property);
    size_t size = iplist.size();

    while(space < size) {
        space = iplist.find(' ', curPos);
        std::string ipAddr;
        if (space == std::string::npos) {
            ipAddr = iplist.substr(curPos, size - curPos);
            space = size;
        } else {
            ipAddr = iplist.substr(curPos, space - curPos);
        }
        curPos = space+1;
        if(ipAddr.size() == 0)
            continue;
        size_t dot=0, curPos1=0;
        size_t ipAddrSize = ipAddr.size();
	std::vector<string> ipVector;

        while(dot < ipAddrSize) {
                std::string ipElement;
                dot = ipAddr.find('.', curPos1);
                if (dot == std::string::npos) {
                    ipElement = ipAddr.substr(curPos1, ipAddrSize - curPos1);
                    dot = ipAddrSize;
                } else {
                    ipElement = ipAddr.substr(curPos1, dot - curPos1);
                }
                curPos1 = dot +1;
                ipVector.push_back(ipElement);            
        }

        ipAddrSet.insert(ipVector);        
        am_web_log_info("parseIPAddresses(): add ipAddr: %s", ipAddr.c_str());
    }
    am_web_log_info("parseIPAddresses(): exit.");
}


void parseIPAddresses(const std::string &property,
        std::set<std::string> &ipAddrSet ) {
    size_t space = 0, curPos = 0;
    std::string iplist(property);
    size_t size = iplist.size();
    while(space < size) {
        space = iplist.find(' ', curPos);
        std::string ipAddr;
        if (space == std::string::npos) {
            ipAddr = iplist.substr(curPos, size - curPos);
            space = size;
        } else {
            ipAddr = iplist.substr(curPos, space - curPos);
        }
        curPos = space+1;
        if(ipAddr.size() == 0)
            continue;
        ipAddrSet.insert(ipAddr);
    }
}
/* Throws std::exception's from string methods */
void parseCookieDomains(const std::string &property,
			std::set<std::string> &CDListSet)
{
    size_t space = 0, curPos = 0;
    std::string cdlist(property);
    size_t size = cdlist.size();

    while(space < size) {
        space = cdlist.find(' ', curPos);
        std::string cookiedomain;
        if (space == std::string::npos) {
            cookiedomain = cdlist.substr(curPos, size - curPos);
            space = size;
        } else {
            cookiedomain = cdlist.substr(curPos, space - curPos);
        }
        curPos = space+1;
        if(cookiedomain.size() == 0)
            continue;
        CDListSet.insert(cookiedomain);
        am_web_log_info("parseCookieDomains(): add cookiedomain: %s",
			cookiedomain.c_str());
    }
    am_web_log_info("parseCookieDomains(): exit.");
}

/*
 * Parse a cookie string represenation of the form
 * name[=value][;Domain=value][;Max-Age=value][;Path=value]
 *
 * Throws std::exception's from string methods.
 */
am_status_t parseCookie(std::string cookie, cookie_info_t *cookie_data)
{
   char *holder = NULL;
   char* temp_str = const_cast<char*>(cookie.c_str());

   if ( cookie_data == NULL || temp_str == NULL) {
        am_web_log_error("parseCookie() : Invalid cookie => %s", cookie.c_str());
        return AM_INVALID_ARGUMENT;
   }

   cleanup_cookie_info(cookie_data);

   //Process name=value

   char *token = NULL;
   std::string tempstr;
   token = strtok_r(temp_str, ";", &holder);
   if (token == NULL) {
       am_web_log_error("parseCookie() : Invalid cookie Name => %s", token);
       return AM_INVALID_ARGUMENT;
   }
   tempstr = token;
   Utils::trim(tempstr);
   token = const_cast<char*>(tempstr.c_str());
#if defined(_AMD64_)
   DWORD64 len = strlen(token);
#else
   int len = strlen(token);
#endif
   char *loc = strchr(token, '=');
   if (loc == NULL) {
       cookie_data->name = (char *)malloc(len+1);
       if (cookie_data->name == NULL) {
	   am_web_log_error("parseCookie(): failed to allocate %u bytes",
                            len+1);
           return AM_NO_MEMORY;
       }
       strcpy(cookie_data->name, token);
   } else {
       len = len - strlen(loc);
       cookie_data->name = (char *)malloc(len+1);
       if (cookie_data->name == NULL) {
	   am_web_log_error("parseCookie(): failed to allocate %u bytes",
                            len+1);
           return AM_NO_MEMORY;
       }
       strncpy(cookie_data->name, token, len);
       cookie_data->name[len]='\0';
       cookie_data->value = (char *) malloc(strlen(loc));
       if (cookie_data->name == NULL) {
	   am_web_log_error("parseCookie(): failed to allocate %u bytes",
                            strlen(loc));
           cleanup_cookie_info(cookie_data);
           return AM_NO_MEMORY;
       }
       strcpy(cookie_data->value, loc+1);
   }

   token = NULL;
   token = strtok_r(NULL, ";", &holder);

   while (token != NULL)  {
      tempstr = token;
      Utils::trim(tempstr);
      token = const_cast<char *>(tempstr.c_str());
      len = strlen(token);
      loc = NULL;
      loc = strstr(token, "Domain=");
      if (loc != NULL) {
           loc = loc + strlen("Domain=");
           len = strlen(loc);
           cookie_data->domain = (char *)malloc(len+1);
           if (cookie_data->domain == NULL) {
	       am_web_log_error("parseCookie() :  "
				"failed to allocate %u bytes", len+1);
               cleanup_cookie_info(cookie_data);
               return AM_NO_MEMORY;
           }
           if (loc[0] == '.') {
              strcpy(cookie_data->domain, loc+1);
              cookie_data->domain[len-1] = '\0';
           } else {
              strcpy(cookie_data->domain, loc);
              cookie_data->domain[len] = '\0';
           }
      } else  {
           loc = strstr(token, "Max-Age=");
           if (loc != NULL) {
              loc = loc + strlen("Max-Age=");
              len = strlen(loc);
              cookie_data->max_age = (char *)malloc(len+1);
              if (cookie_data->max_age == NULL) {
	          am_web_log_error("parseCookie() : "
				   "failed to allocate %u bytes", len+1);
                  cleanup_cookie_info(cookie_data);
                  return AM_NO_MEMORY;
              }
              strcpy(cookie_data->max_age, loc);
           } else  {
      	      loc = strstr(token, "Path=");
              if (loc != NULL) {
                  loc = loc + strlen("Path=");
           	  len = strlen(loc);
           	  cookie_data->path = (char *)malloc(len+1);
           	  if (cookie_data->path == NULL) {
	              am_web_log_error("parseCookie() : "
				       "failed to allocate %u bytes", len+1);
               	      cleanup_cookie_info(cookie_data);
                      return AM_NO_MEMORY;
                  }
                  strcpy(cookie_data->path, loc);
              } else  {
  		  am_web_log_warning("Unprocessed token => %s", token);
              }
           }
      }
      token = strtok_r(NULL, ";", &holder);
   }

   return AM_SUCCESS;
}



am_status_t initCookieResetList(cookie_info_list_t *cookie_list)
{
   const char *DEFAULT_PATH = "/";

   if (cookie_list == NULL || cookie_list->list == NULL) {
	am_web_log_warning("initCookieResetList() : NULL cookie_list" );
        return AM_INVALID_ARGUMENT;
   }

   for (size_t i=0; i < cookie_list->size; ++i) {

       cookie_info_t *cookie_data = &cookie_list->list[i];
       if (cookie_data != NULL) {

           if ( cookie_data->domain == NULL ) {
#if defined(_AMD64_)
                DWORD64 domain_len = strlen(agent_info.cookie_reset_default_domain);
#else
                int domain_len = strlen(agent_info.cookie_reset_default_domain);
#endif
                cookie_data->domain = (char *) malloc(domain_len +1);
                if (cookie_data->domain == NULL) {
                    am_web_log_error("parseCookie(): "
                                     "failed to allocate %u bytes",
                                     domain_len + 1);
                    cleanup_cookie_info(cookie_data);
                    return AM_NO_MEMORY;
                }
                strcpy(cookie_data->domain,
                       agent_info.cookie_reset_default_domain);
           }

           if (cookie_data->max_age != NULL) {
               if (cookie_data->max_age[0] == '\0') {
                   free(cookie_data->max_age);
                   // max_age cannot be an empty string for with older browsers
	           // netscape 4.79, IE 5.5, mozilla < 1.4.
		   // If specified as an empty string in the config,
		   // don't set it at all in the cookie header.
		   cookie_data->max_age = NULL;
	       }
	   }
	   else {
	       // by default, delete cookie on reset.
	       cookie_data->max_age = const_cast<char*>("0");
	   }

	   if (cookie_data->path != NULL) {
               if (cookie_data->path[0] == '\0') {
                   free(cookie_data->path);
	           // path must be '/' for older browsers IE,
		   // netscape 4.79 to work
                   cookie_data->path = strdup(DEFAULT_PATH);
	       }
	   }
	   else {
	       cookie_data->path = strdup(DEFAULT_PATH);
	   }
       }
       am_web_log_debug("initCookieResetList(): "
                        "initialized cookie: "
                        "%s, domain %s, max_page %s, path %s",
                        cookie_data->name, cookie_data->domain,
                        cookie_data->max_age, cookie_data->path);
   }
   return AM_SUCCESS;
}


/* Throws std::exception's from string methods */
am_status_t parseCookieList(const char *property, char sep,
			    cookie_info_list_t *cookie_list)
{
    size_t num_cookies = 0;

    if ( property == NULL || cookie_list == NULL) {
       am_web_log_warning(
		"parseCookieList() : cookie_list or property is NULL");
       return AM_INVALID_ARGUMENT;
    }

    cleanup_cookie_info_list(cookie_list);

    const char *temp_ptr = property;

    // removing leading spaces and separators.
    while (*temp_ptr == ' ' || *temp_ptr == sep) {
        temp_ptr += 1;
    }

    if ( *temp_ptr == '\0') {
	cookie_list->size = 0;
        cookie_list->list = NULL;
        return AM_SUCCESS;
    }

    /* Calculate num elems */
    do {
        num_cookies += 1;

        while (*temp_ptr != '\0' && *temp_ptr != ' ' &&
               *temp_ptr != sep) {
            temp_ptr += 1;
        }
        while (*temp_ptr == ' ' || *temp_ptr == sep) {
            temp_ptr += 1;
        }
    } while (*temp_ptr != '\0');

    cookie_list->list = (cookie_info_t *) calloc(num_cookies,
						sizeof(cookie_info_t));
    if ( cookie_list->list == NULL) {
	am_web_log_error("parseCookieList() : failed to allocate %u bytes",
				num_cookies * sizeof(cookie_info_t));
	return AM_NO_MEMORY;
    }

    memset(cookie_list->list, 0, num_cookies * sizeof(cookie_info_t));

    size_t space = 0, curPos = 0;
#if defined(_AMD64_)
    unsigned int idx = 0;
#else
    size_t idx = 0;
#endif
    std::string cookies(property);
    Utils::trim(cookies);
    size_t size = cookies.size();

    while(space < size) {
	space = cookies.find(',', curPos);
        std::string cookie;
        if (space == std::string::npos) {
	    cookie = cookies.substr(curPos, size - curPos);
            space = size;
        } else {
            cookie = cookies.substr(curPos, space - curPos);
        }
        curPos = space+1;
        Utils::trim(cookie);
        if (cookie.size() == 0)
           continue;

        if ( AM_SUCCESS == parseCookie(cookie, &cookie_list->list[idx]) ) {
	     idx++;
	} else {
	     am_web_log_warning("Failed to Parse cookie: %s", cookie.c_str());
        }

    }
    cookie_list->size = idx;
    return AM_SUCCESS;
}


static am_status_t parse_url(const char *url_str, size_t len,
				url_info_t *entry_ptr,
				am_bool_t validateURLs)
{
    const char *url = url_str;
    size_t url_len = len;
    std::string normalizedURL;
    am_status_t status = AM_SUCCESS;
    size_t host_offset = 0;
    const char *protocol;

    if (NULL != url) {
	/**
	 * FIX_NEXT_RELEASE
	 * This is a hack that I've put here.  The next release,
	 * we should be doing away with anything to do with URLs that's
	 * not in URL class.
	 *
	 * For now, compare wether it is a URL we are talking about or
	 * some regular expression like *.gif.
	 * If it is a URL, then, normalize it.
	 */
	if(strncasecmp(url, HTTP_PREFIX, HTTP_PREFIX_LEN) == 0 ||
	   strncasecmp(url, HTTPS_PREFIX, HTTPS_PREFIX_LEN) == 0) {
	    try {
		URL urlObject(url, len);
		urlObject.getURLString(normalizedURL);
		url = normalizedURL.c_str();
		url_len = normalizedURL.size();
		protocol = urlObject.getProtocolString();
	        Log::log(agent_info.log_module, Log::LOG_DEBUG,
		         "parse_url(%s): Normalized URL: %s",
			 url_str, url);
	    } catch(InternalException &iex) {
		Log::log(agent_info.log_module, Log::LOG_ERROR,
			 "parse_url(%s) failed with error: %s",
			 url_str, iex.getMessage());
		status = AM_INVALID_ARGUMENT;
	    } catch(std::exception &ex) {
		Log::log(agent_info.log_module, Log::LOG_ERROR,
			 "parse_url(%s) failed with error: %s",
			 url_str, ex.what());
		status = AM_INVALID_ARGUMENT;
	    } catch(...) {
		Log::log(agent_info.log_module, Log::LOG_ERROR,
			 "parse_url(%s) failed with unknown exception.",
			 url_str);
		status = AM_INVALID_ARGUMENT;
	    }
	}

	if(validateURLs == AM_TRUE && status == AM_SUCCESS) {
	    if(url_len >= MIN_URL_LEN) {
		if (strncasecmp(url, HTTPS_PREFIX, HTTPS_PREFIX_LEN) == 0) {
		    entry_ptr->port = HTTPS_DEF_PORT;
		    host_offset = HTTPS_PREFIX_LEN;
		} else if (strncasecmp(url, HTTP_PREFIX,
				       HTTP_PREFIX_LEN) == 0){
		    entry_ptr->port = HTTP_DEF_PORT;
		    host_offset = HTTP_PREFIX_LEN;
		} else {
		    status = AM_INVALID_ARGUMENT;
		}
	    } else {
		status = AM_INVALID_ARGUMENT;
	    }
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    if (AM_SUCCESS == status) {
	entry_ptr->url = (char *)malloc(url_len + 1);
	entry_ptr->host = (char *)malloc(url_len - host_offset + 1);
	entry_ptr->protocol = const_cast<char *>(protocol);
	if (NULL != entry_ptr->url && NULL != entry_ptr->host) {
	    char *temp_ptr;

	    memcpy(entry_ptr->url, url, url_len);
	    entry_ptr->url[url_len] = '\0';
	    entry_ptr->url_len = url_len;
	    if (strchr(entry_ptr->url, '?') != NULL) {
		entry_ptr->has_parameters = AM_TRUE;
	    } else {
		entry_ptr->has_parameters = AM_FALSE;
	    }

	    if (am_policy_resource_has_patterns(entry_ptr->url)==B_TRUE) {
		entry_ptr->has_patterns = AM_TRUE;
	    } else {
		entry_ptr->has_patterns = AM_FALSE;
	    }

	    url_len -= host_offset;
	    url += host_offset;
	    if (url_len > 0) {
		memcpy(entry_ptr->host, url, url_len);
	    }
	    entry_ptr->host[url_len] = '\0';

	    temp_ptr = strchr(entry_ptr->host, '/');
	    if (temp_ptr != NULL) {
		*temp_ptr = '\0';
	    }

	    temp_ptr = strchr(entry_ptr->host, ':');
	    if (NULL != temp_ptr) {
		*(temp_ptr++) = '\0';

		entry_ptr->port = 0;
		while (isdigit(*temp_ptr)) {
		    entry_ptr->port = (entry_ptr->port * 10) + *temp_ptr - '0';
		    temp_ptr += 1;
		}
	    }
	} else {
	    if (NULL == entry_ptr->url) {
		am_web_log_error("parse_url() failed to allocate %u bytes for "
				"URL.", url_len + 1);
	    } else {
		free(entry_ptr->url);
		entry_ptr->url = NULL;
	    }
	    if (NULL == entry_ptr->host) {
		am_web_log_error("parse_url() failed to allocate %u bytes for "
				"host name.", url_len + 1);
	    } else {
		free(entry_ptr->host);
		entry_ptr->host = NULL;
	    }
	    status = AM_NO_MEMORY;
	}
    }

    return status;
}

static am_status_t parse_url_list(const char *url_list_str, char sep,
				  url_info_list_t *list_ptr,
				  am_bool_t validateURLs)
{
    am_status_t status = AM_SUCCESS;
    int num_elements = 0;

    cleanup_url_info_list(list_ptr);

    if (url_list_str != NULL) {
	const char *temp_ptr = url_list_str;

	// removing leading spaces and separators.
	while (*temp_ptr == ' ' || *temp_ptr == sep) {
	    temp_ptr += 1;
	}

	if (*temp_ptr != '\0') {
	    url_list_str = temp_ptr;

	    /* Calculate num elems */
	    do {
		num_elements += 1;

		while (*temp_ptr != '\0' && *temp_ptr != ' ' &&
		       *temp_ptr != sep) {
		    temp_ptr += 1;
		}
		while (*temp_ptr == ' ' || *temp_ptr == sep) {
		    temp_ptr += 1;
		}
	    } while (*temp_ptr != '\0');

	    list_ptr->list = (url_info_t *) calloc(num_elements,
						   sizeof(list_ptr->list[0]));
	    if (NULL != list_ptr->list) {
		temp_ptr = url_list_str;
		do {
		    size_t len = 0;

		    while (temp_ptr[len] != '\0' && temp_ptr[len] != ' ' &&
			   temp_ptr[len] != sep) {
			len += 1;
		    }

		    status = parse_url(temp_ptr, len,
				       &list_ptr->list[list_ptr->size],
				       validateURLs);

		    if (AM_SUCCESS == status) {
			temp_ptr += len;
			list_ptr->size += 1;
		    } else {
			break;
		    }

		    while (*temp_ptr == ' ' || *temp_ptr == sep) {
			temp_ptr += 1;
		    }
		} while (*temp_ptr != '\0');
	    } else {
		am_web_log_error("parse_url_list() failed to allocate %u bytes "
				"for URL list",
				num_elements * sizeof(url_info_t));
		status = AM_NO_MEMORY;
	    }
	}
    }

    return status;
}

/* Throws std::exception's from url parsing routines. */
static am_status_t
load_agent_properties(agent_info_t *info_ptr, const char *file_name, boolean_t initializeLog)
{
    const char *thisfunc = "load_agent_properties()";
    am_status_t status;
    const char *function_name = "am_properties_create";
    const char *parameter = "";
    const char *agent_prefix_url = NULL;
    char *dummy_url = NULL;
#if defined(_AMD64_)
    DWORD64 tempurl_len = 0;
#else
    int tempurl_len = 0;
#endif
    const char *encrypt_passwd = NULL;
    char decrypt_passwd[100] = "";
    int decrypt_status;
    const char *url_redirect_default = "goto";
    const char *user_id_default = "UserToken";
    const char *authLogType_default = LOG_TYPE_NONE;
    bool urlstatssl = false;
    bool urlstatnonssl = false;
    const char *filterPriority_default=IIS_FILTER_PRIORITY;

    cleanup_properties(info_ptr);

    status = am_properties_create(&info_ptr->properties);
    if (AM_SUCCESS == status) {
	function_name = "am_properties_load";
	parameter = file_name;
	status = am_properties_load(info_ptr->properties, file_name);
    }

    if (AM_SUCCESS == status) {
	int do_sleep = 0;

	parameter = AM_WEB_PROPERTY_PREFIX "stopInInit";
	status = am_properties_get_boolean_with_default(
			info_ptr->properties, parameter, 0, &do_sleep);
	if (AM_SUCCESS != status) {
	    do_sleep = 0;
	    status = AM_SUCCESS;
	}

#if (defined(WINNT) || defined(_AMD64_))
	if (do_sleep) {
	    DebugBreak();
	}
#else
	while (do_sleep) {
	    sleep(1);
	}
#endif
    }

    /* Set the log file pointer early enough */
    if (initializeLog) {

    	if (AM_SUCCESS == status) {
			// this will set log file and default levels from the properties.
			status = am_log_init(info_ptr->properties);
    	}

    	/* Add agent log module, level */
    	if (AM_SUCCESS == status) {
			status = am_log_add_module("PolicyAgent",
					   &agent_info.log_module);
			if (AM_SUCCESS != status) {
		    agent_info.log_module = AM_LOG_ALL_MODULES;
			}
    	}
	}

    /* Get dpro cookie name.*/
    if (AM_SUCCESS == status) {
	function_name = "am_properties_get";
	parameter = AM_COMMON_COOKIE_NAME_PROPERTY;
	status = am_properties_get(info_ptr->properties, parameter,
				      &info_ptr->cookie_name);
	if (AM_SUCCESS == status) {
	    info_ptr->cookie_name_len = strlen(info_ptr->cookie_name);
	}
    }
    
    /* Get the is_cookie_secure flag */
    if (AM_SUCCESS == status) {
      parameter = AM_COMMON_COOKIE_SECURE_PROPERTY;
      status = am_properties_get_boolean_with_default(
            info_ptr->properties, parameter,
            AM_FALSE, &info_ptr->is_cookie_secure);
      if (info_ptr->is_cookie_secure == AM_TRUE) {
         am_web_log_info("%s : Property com.sun.am.cookie.secure is set to true.",thisfunc);
      } else {
         am_web_log_info("%s : Property com.sun.am.cookie.secure is set to false.",thisfunc);
      }
    }

     /* Get fqdn.check.enable */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_FQDN_CHECK_ENABLE;
        status = am_properties_get_boolean_with_default(info_ptr->properties,
                                                           parameter,
                                                           AM_TRUE,
                                                           &info_ptr->fqdn_check_enable);
     }


    /* Get fqdn_default value */
    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_WEB_FQDN_DEFAULT;
        status = am_properties_get(info_ptr->properties, parameter,
                                   &info_ptr->fqdn_default);
        if (AM_SUCCESS == status) {
            info_ptr->fqdn_default_len = strlen(info_ptr->fqdn_default);
            const char *temp = strchr(info_ptr->fqdn_default, '.');
            if (temp != NULL) {
                 info_ptr->cookie_reset_default_domain = ++temp;
            }
        }
    }

    /* Get the cookie domain list. */
    if (AM_SUCCESS == status) {
        const char *cookie_domain_listptr;
        info_ptr->cookie_domain_list = NULL;
        parameter = AM_WEB_COOKIE_DOMAIN_LIST;
        status = am_properties_get_with_default(info_ptr->properties, parameter,
				                NULL, &cookie_domain_listptr);
        if(NULL != cookie_domain_listptr && '\0' != cookie_domain_listptr[0]) {
	    am_web_log_info("calling parseCookieDomains(): "
                            "cookie_domain_listptr: %s",
                            cookie_domain_listptr);
            info_ptr->cookie_domain_list = new std::set<std::string>();
            if(info_ptr->cookie_domain_list == NULL) {
	        status = AM_NO_MEMORY;
            }
            else {
	        parseCookieDomains(cookie_domain_listptr,
                                   *(info_ptr->cookie_domain_list));
            }
        }
    }

    /* Get the denied URL.*/
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_ACCESS_DENIED_URL_PROPERTY;
	status = am_properties_get_with_default(info_ptr->properties,
						parameter, NULL,
						&info_ptr->access_denied_url);

	if(info_ptr->access_denied_url != NULL) {
	    urlstatnonssl = (strncasecmp(info_ptr->access_denied_url,
                                         HTTP_PREFIX, HTTP_PREFIX_LEN) == 0);
	    urlstatssl = (strncasecmp(info_ptr->access_denied_url,
                                      HTTPS_PREFIX, HTTPS_PREFIX_LEN) == 0);

	    if( (urlstatnonssl == false) && (urlstatssl == false) ){
		am_web_log_warning(
			"Invalid URL (%s) for property (%s) specified",
                        info_ptr->access_denied_url == NULL ? "NULL" :
                            info_ptr->access_denied_url,
			AM_WEB_ACCESS_DENIED_URL_PROPERTY);
	    }

	}

	if ((AM_SUCCESS == status &&
		info_ptr->access_denied_url != NULL &&
		'\0' == *info_ptr->access_denied_url) ||
	    ((urlstatnonssl == false) && (urlstatssl == false))) {
	    /*
	     * Treat an empty property value as if the property was not
	     * specified at all.
	     */
	    info_ptr->access_denied_url = NULL;
	    am_web_log_warning("Setting access_denied_url to null");
	}
    }
    
    /* Get the error page URL.*/
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_ERROR_PAGE_URL_PROPERTY;
	status = am_properties_get_with_default(info_ptr->properties,
						parameter, NULL,
						&info_ptr->error_page_url);

	if(info_ptr->error_page_url != NULL) {
	    urlstatnonssl = (strncasecmp(info_ptr->error_page_url,
                                         HTTP_PREFIX, HTTP_PREFIX_LEN) == 0);
	    urlstatssl = (strncasecmp(info_ptr->error_page_url,
                                      HTTPS_PREFIX, HTTPS_PREFIX_LEN) == 0);

	    if( (urlstatnonssl == false) && (urlstatssl == false) ){
		am_web_log_warning(
			"Invalid URL (%s) for property (%s) specified",
                        info_ptr->error_page_url == NULL ? "NULL" :
                            info_ptr->error_page_url,
			AM_WEB_ERROR_PAGE_URL_PROPERTY);
	    }

	}
	if ((AM_SUCCESS == status &&
		info_ptr->error_page_url != NULL &&
		'\0' == *info_ptr->error_page_url) ||
	    ((urlstatnonssl == false) && (urlstatssl == false))) {
	    /*
	     * Treat an empty property value as if the property was not
	     * specified at all.
	     */
	    info_ptr->error_page_url = NULL;
	    am_web_log_warning("Setting error_page_url to null");
	}
    }

    /* Get the UnAuthenticated User info */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_ANONYMOUS_USER;
	status = am_properties_get_with_default(info_ptr->properties,
						   parameter, NULL,
						   &info_ptr->unauthenticated_user);
	if (AM_SUCCESS == status &&
            info_ptr->unauthenticated_user != NULL &&
            '\0' == *info_ptr->unauthenticated_user) {

	    /*
	     * Treat an empty property value as if the property was not
	     * specified at all.
	     */
	    info_ptr->unauthenticated_user = NULL;
	}
    }


    /* Get the Anonymous Remote User Enabled flag */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_ANON_REMOTE_USER_ENABLE;
	if (info_ptr->unauthenticated_user != NULL) {
	    status = am_properties_get_boolean_with_default(
			  info_ptr->properties, parameter,
			  AM_FALSE, &info_ptr->anon_remote_user_enable);
        } else {
	    am_web_log_warning("Invalid  Unauthenticated User : %s Disabled",
				parameter);
	    info_ptr->anon_remote_user_enable = AM_FALSE;
        }
    }

    /* Get the URL Redirect param  */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_URL_REDIRECT_PARAM;
	status = am_properties_get_with_default(info_ptr->properties, parameter,
			url_redirect_default, &info_ptr->url_redirect_param);
    }

    /* Get the User Id param  */
    // The user_id_param of agent_info is actually not used.
    // but keep it so size of agent_info remains the same for backwards compat.
    // the actual parameter that's used to get user id is done in service.cpp.
    if (AM_SUCCESS == status) {
	parameter = AM_POLICY_USER_ID_PARAM_PROPERTY;
	status = am_properties_get_with_default(info_ptr->properties, parameter,
			user_id_default, &info_ptr->user_id_param);
    }

    /* Get the auth log type param  */
    if (AM_SUCCESS == status) {
	parameter = AM_LOG_ACCESS_TYPE_PROPERTY;
	status = am_properties_get_with_default(info_ptr->properties,
			    parameter, authLogType_default,
			    &info_ptr->authLogType_param);
	info_ptr->log_access_type = 0;
	if (!strcasecmp(info_ptr->authLogType_param, LOG_TYPE_ALLOW)) {
	    info_ptr->log_access_type |= LOG_ACCESS_ALLOW;
	}
	else if (!strcasecmp(info_ptr->authLogType_param, LOG_TYPE_DENY)) {
	    info_ptr->log_access_type |= LOG_ACCESS_DENY;
	}
	else if (!strcasecmp(info_ptr->authLogType_param, LOG_TYPE_BOTH)) {
	    info_ptr->log_access_type |= LOG_ACCESS_ALLOW|LOG_ACCESS_DENY;
	}
    }

    /* Get the deny on log failure param */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_DENY_ON_LOG_FAILURE;
	am_properties_get_boolean_with_default(info_ptr->properties,
					       parameter, AM_TRUE,
					       &info_ptr->denyOnLogFailure);
    }

    /* Get the CDSSO URL */
    if(AM_SUCCESS == status) {
	int fetchCDSSOURL = AM_FALSE;
	const char *property_str = NULL;
	parameter = AM_WEB_CDSSO_ENABLED_PROPERTY;
	am_properties_get_boolean_with_default(info_ptr->properties,
						  parameter, AM_FALSE,
						  &fetchCDSSOURL);
	if(fetchCDSSOURL) {
	    parameter = AM_WEB_CDC_SERVLET_URL_PROPERTY;
	    status = am_properties_get(info_ptr->properties,
				       parameter,
				       &property_str);
	    if(AM_SUCCESS == status &&
		    property_str != NULL && property_str[0] != '\0') {
		status = parse_url_list(property_str, ' ',
					&info_ptr->cdsso_server_url_list,
					AM_TRUE);
	    } else {
		if (status == AM_SUCCESS &&
		    (property_str == NULL || property_str[0] == '\0')) {
		    status = AM_NOT_FOUND;
		}

		am_web_log_error("am_web_init(): "
				 "CDSSO is enabled but could not get value "
				 "for property %s: %s.",
				 AM_WEB_CDSSO_ENABLED_PROPERTY,
				 am_status_to_string(status));
	    }
	}
    }

    /* Get the agent server url */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_URI_PREFIX;
        status = am_properties_get(info_ptr->properties, parameter,
                                   &agent_prefix_url);
        if (AM_SUCCESS == status) {
            status = parse_url(agent_prefix_url, strlen(agent_prefix_url),
                               &info_ptr->agent_server_url,
                               AM_TRUE);
            if ( AM_SUCCESS == status) {
                am_web_log_info("%s: %s : Value => %s",
                                thisfunc, AM_WEB_URI_PREFIX,
                                info_ptr->agent_server_url.url);
            } else {
                am_web_log_warning(
                         "%s: Invalid URL for %s: Value = %s",
                         thisfunc, AM_WEB_URI_PREFIX, agent_prefix_url);
            }
        } else {
            am_web_log_error("%s: Invalid URL for %s : Value => %s",
                  thisfunc, AM_WEB_URI_PREFIX, agent_prefix_url);
        }
    }

    /* Get the login URL.*/
    if (AM_SUCCESS == status) {
	const char *property_str;
	parameter = AM_POLICY_LOGIN_URL_PROPERTY;
	status = am_properties_get(info_ptr->properties, parameter,
				   &property_str);
	if (AM_SUCCESS == status) {
	    status = parse_url_list(property_str, ' ',
				    &info_ptr->login_url_list, AM_TRUE);
	}
    }

    /* Get the notification URL.*/
    if (AM_SUCCESS == status) {
	parameter = AM_COMMON_NOTIFICATION_ENABLE_PROPERTY;
	status = am_properties_get_boolean_with_default(info_ptr->properties,
						parameter, B_FALSE,
	    reinterpret_cast<int *>(&info_ptr->notification_enabled));
	if (info_ptr->notification_enabled) {
	    parameter = AM_COMMON_NOTIFICATION_URL_PROPERTY;
	    status = am_properties_get_with_default(info_ptr->properties,
						   parameter, "",
						   &info_ptr->notification_url);
 	    if (info_ptr->notification_url == NULL ||
		strlen(info_ptr->notification_url) == 0) {
		    info_ptr->notification_enabled = AM_FALSE;
	    }
	}
    }
    
    /* Get notenforced_IP handler mode*/
    if (AM_SUCCESS == status) {
        status = am_properties_get_with_default(info_ptr->properties, "com.forgerock.agents.config.notenforced.ip.handler", NULL, &info_ptr->notenforcedIPmode);
        am_web_log_max_debug("Property [com.forgerock.agents.config.notenforced.ip.handler] value set to [%s]", info_ptr->notenforcedIPmode);
    }

     /* Get url string comparision case sensitivity values. */
     if (AM_SUCCESS == status) {
	status = am_properties_get_boolean_with_default(info_ptr->properties,
	    AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY, AM_FALSE,
	    reinterpret_cast<int *>(&info_ptr->url_comparison_ignore_case));
    }

    /* Get the instance name of the server.*/
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_INSTANCE_NAME_PROPERTY;
	status = am_properties_get(info_ptr->properties, parameter,
				      &info_ptr->instance_name);
    }


    /* Get the POST data cache preserve status */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_POST_CACHE_DATA_PRESERVE;
	status =
	    am_properties_get_boolean_with_default(info_ptr->properties,
						   parameter,
						   AM_FALSE,
						   reinterpret_cast<int *>
						   (&info_ptr->postdatapreserve_enabled));
    }

    // Get the mode the LB uses to get the sticky session
    // that is used with post preservation (COOKIE or URL)
    if (AM_SUCCESS == status) {
        const char *property_str;
        parameter = AM_WEB_POST_CACHE_DATA_PRESERVE_STICKY_SESSION_MODE;
        status = am_properties_get_with_default(info_ptr->properties,
                                    parameter, "", 
                                    &info_ptr->postdatapreserve_sticky_session_mode);
    }

    // Get the value of the sticky session to use with post preservation.
    if (AM_SUCCESS == status) {
        const char *property_str;
        parameter = AM_WEB_POST_CACHE_DATA_PRESERVE_STICKY_SESSION_VALUE;
        status = am_properties_get_with_default(info_ptr->properties,
                                    parameter, "", 
                                    &info_ptr->postdatapreserve_sticky_session_value);
    }

    /* Get the POST cache entry lifetime */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_POST_CACHE_ENTRY_LIFETIME;
	status = am_properties_get_unsigned_with_default(info_ptr->properties,
							    parameter, 3UL,
							    &info_ptr->postcacheentry_life);
    }

    /* Get locale setting */
    if(AM_SUCCESS == status) {
	parameter = AM_WEB_PROPERTY_PREFIX "locale";
	am_properties_get_with_default(info_ptr->properties, parameter,
					  NULL, &info_ptr->locale);
    }

     /* Get client_ip_validation.enable.*/
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_CHECK_CLIENT_IP_PROPERTY;
	status = am_properties_get_boolean_with_default(info_ptr->properties,
							   parameter,
							   AM_FALSE,
							   &info_ptr->check_client_ip);
     }

	if (AM_SUCCESS == status) {
	parameter = AM_WEB_CONVERT_MBYTE_ENABLE;
	status = am_properties_get_boolean_with_default(info_ptr->properties,
							   parameter,
							   AM_FALSE,
							   &info_ptr->convert_mbyte);
     }

     if (AM_SUCCESS == status) {
	parameter = AM_WEB_ENCODE_URL_SPECIAL_CHARS;
	status = am_properties_get_boolean_with_default(info_ptr->properties,
							   parameter,
							   AM_FALSE,
							   &info_ptr->encode_url_special_chars);
     }
     if (AM_SUCCESS == status) {
	parameter = AM_WEB_ENCODE_COOKIE_SPECIAL_CHARS;
	status = am_properties_get_boolean_with_default(info_ptr->properties,
							   parameter,
							   AM_FALSE,
							   &info_ptr->encode_cookie_special_chars);
     }

     /* Get notenforced_client_IP_address */
    if (AM_SUCCESS == status) {
        const char *not_enforced_ipstr;
        parameter = AM_WEB_NOT_ENFORCED_IPADDRESS;
        status = am_properties_get_with_default(info_ptr->properties,
                parameter,
                NULL,
                &not_enforced_ipstr);
        if (not_enforced_ipstr != NULL)
            am_web_log_info("calling parseIPAddresses(): not_enforced_ipstr: %s",
                not_enforced_ipstr);
        info_ptr->not_enforce_IPAddr = new ipAddrSet_t;
        if (info_ptr->not_enforce_IPAddr == NULL) {
            status = AM_NO_MEMORY;
        }
        if (AM_SUCCESS == status && not_enforced_ipstr != NULL) {
            info_ptr->not_enforce_IPAddr_set = new std::set<std::string > ();
            if (info_ptr->not_enforce_IPAddr_set == NULL) {
                status = AM_NO_MEMORY;
            }
            if (AM_SUCCESS == status) {
                parseIPAddresses(not_enforced_ipstr,
                        *(info_ptr->not_enforce_IPAddr_set));
            }
        }
        if (AM_SUCCESS == status && not_enforced_ipstr != NULL && (info_ptr->notenforcedIPmode == NULL || strncasecmp(agent_info.notenforcedIPmode, "cidr", 4) != 0)) {
            parseIPAddresses(not_enforced_ipstr, *(info_ptr->not_enforce_IPAddr));
        }
    }


    /* Get the not enforced list.*/
    if (AM_SUCCESS == status) {
	const char *not_enforced_str;

	parameter = AM_WEB_NOT_ENFORCED_LIST_PROPERTY;
	status = am_properties_get_with_default(info_ptr->properties,
						   parameter,
						   NULL,
						   &not_enforced_str);

	if (AM_SUCCESS == status) {
	    status = parse_url_list(not_enforced_str, ' ',
				    &info_ptr->not_enforced_list, AM_FALSE);
	}
    }

    /* Get reverse_the_meaning_of_notenforcedList */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_REVERSE_NOT_ENFORCED_LIST;
	status = am_properties_get_boolean_with_default(info_ptr->properties,
                                                        parameter,
                                                        AM_FALSE,
                                                        &info_ptr->reverse_the_meaning_of_not_enforced_list);
     }

    /* Get ignore_policy_evaluation_if_notenforced */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_IGNORE_POLICY_EVALUATION_IF_NOT_ENFORCED;
	status = am_properties_get_boolean_with_default(info_ptr->properties,
                                                        parameter,
                                                        AM_FALSE,
                                                        &info_ptr->ignore_policy_evaluation_if_notenforced);
     }

     /* Get do_sso_only.*/
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_DO_SSO_ONLY;
	status = am_properties_get_boolean_with_default(info_ptr->properties,
                                                        parameter,
                                                        AM_FALSE,
                                                        &info_ptr->do_sso_only);
     }

    /* Get CDSSO Enabled/Disabled */
    if (AM_SUCCESS == status) {
       parameter = AM_WEB_CDSSO_ENABLED_PROPERTY;
       status = am_properties_get_boolean_with_default(info_ptr->properties,
                                                       parameter,
                                                       AM_FALSE,
                                                       &info_ptr->cdsso_enabled);
    }
    
    /* Get useSunwMethod Enabled/Disabled */
    if (AM_SUCCESS == status) {
       parameter = AM_WEB_USE_SUNWMETHOD_PROPERTY;
       status = am_properties_get_boolean_with_default(info_ptr->properties,
                                                       parameter,
                                                       AM_FALSE,
                                                       &info_ptr->useSunwMethod);
    }

    /* Get Logout URLs if any */
    if (AM_SUCCESS == status) {
        info_ptr->logout_url_list.size = 0;
        parameter = AM_WEB_LOGOUT_URL_PROPERTY;
        const char *logout_url_str;
        status = am_properties_get_with_default(info_ptr->properties,
                                                parameter,
                                                NULL,
                                                &logout_url_str);
        if (AM_SUCCESS == status && logout_url_str != NULL) {
            am_web_log_max_debug("am_web_init(): Logout URL is %s.",
                                 logout_url_str);
	    status = parse_url_list(logout_url_str, ' ',
				    &info_ptr->logout_url_list, AM_TRUE);
        }
    }

    /* Get Logout Cookie reset list if any */
    if (AM_SUCCESS == status) {
        info_ptr->logout_cookie_reset_list.size = 0;
        info_ptr->logout_cookie_reset_list.list = NULL;
        parameter = AM_WEB_LOGOUT_COOKIE_RESET_PROPERTY;
        const char *logout_cookie_reset_str = NULL;
        status = am_properties_get_with_default(info_ptr->properties,
                                               parameter,
                                               NULL,
                                               &logout_cookie_reset_str);
        if (AM_SUCCESS == status) {
            if (NULL != logout_cookie_reset_str &&
                '\0' != logout_cookie_reset_str[0]) {
                am_web_log_max_debug("logout cookie reset list is %s.",
                                     logout_cookie_reset_str);
	        status = parseCookieList(logout_cookie_reset_str, ',',
                                         &info_ptr->logout_cookie_reset_list);
                if (AM_SUCCESS == status) {
                    status = initCookieResetList(
                                  &info_ptr->logout_cookie_reset_list);
                }
            }
            else {
	        am_web_log_max_debug("no cookies to be reset on logout.");
            }
        }
    }

    /* Get Reset Cookie Enabled/Disabled */
    if (AM_SUCCESS == status) {
       parameter = AM_WEB_COOKIE_RESET_ENABLED;
       status = am_properties_get_boolean_with_default(info_ptr->properties,
                                                       parameter,
                                                       AM_FALSE,
                                                       &info_ptr->cookie_reset_enabled);
    }

    /* Get the List of Cookies to be Reset */

    if (AM_SUCCESS == status) {
        if (info_ptr->cookie_reset_enabled == AM_TRUE) {
            const char *cookie_str = NULL;
            parameter = AM_WEB_COOKIE_RESET_LIST;
            status = am_properties_get_with_default(info_ptr->properties,
                                                    parameter,
                                                    NULL,
                                                    &cookie_str);
	    if (AM_SUCCESS == status &&
                cookie_str != NULL && '\0' != cookie_str[0]) {
                am_web_log_max_debug("%s: cookies to be reset: %s",
				     thisfunc, cookie_str);
	            status = parseCookieList(
	 			cookie_str, ',', &info_ptr->cookie_list);
                    if ( AM_SUCCESS == status) {
 	 		status = initCookieResetList(&info_ptr->cookie_list);
 	 	   }
            }
            else {
	        info_ptr->cookie_reset_enabled = AM_FALSE;
                am_web_log_max_debug("%s: no cookies to be reset.", thisfunc);
            }
        }
        else {
	    am_web_log_max_debug(
		    "%s: cookie reset enabled property %s is false",
		    thisfunc, AM_WEB_COOKIE_RESET_ENABLED);
        }
    }

    /* Get whether client's host name should be looked up and given to policy */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_GET_CLIENT_HOSTNAME;
	status = am_properties_get_boolean_with_default(
				agent_info.properties,
				parameter, (int)true,
				&agent_info.getClientHostname);
	if (status != AM_SUCCESS) {
	    am_web_log_warning("%s: Error %s while getting %s property. "
			       "Defaulting to true.", thisfunc,
			       am_status_to_string(status),
			       parameter);
	    agent_info.getClientHostname = true;
	    status = AM_SUCCESS;
	}
    }

    if (AM_SUCCESS == status) {
	parameter = AM_WEB_OVERRIDE_PROTOCOL;
	status = am_properties_get_boolean_with_default(
			info_ptr->properties, parameter, 0,
			&(info_ptr->override_protocol));
    }
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_OVERRIDE_HOST;
	status = am_properties_get_boolean_with_default(
			info_ptr->properties, parameter, 0,
			&(info_ptr->override_host));
    }
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_OVERRIDE_PORT;
	status = am_properties_get_boolean_with_default(
			info_ptr->properties, parameter, 0,
			&(info_ptr->override_port));
    }
    if (AM_SUCCESS == status && info_ptr->notification_enabled) {
	parameter = AM_WEB_OVERRIDE_NOTIFICATION_URL;
	status = am_properties_get_boolean_with_default(
			info_ptr->properties, parameter, 0,
			&(info_ptr->override_notification_url));
    }

    /* Reset the shared secret to the decrypted password */
    if (AM_SUCCESS == status) {
	parameter = AM_POLICY_PASSWORD_PROPERTY;
	status = am_properties_get(info_ptr->properties, parameter,
				   &encrypt_passwd);
	if (AM_SUCCESS == status) {
	    if(encrypt_passwd != NULL){
		decrypt_status = decrypt_base64(encrypt_passwd, decrypt_passwd);
		if(decrypt_status == 0){
		    am_properties_set(info_ptr->properties, parameter,
				      decrypt_passwd);
		}else {
		    status=static_cast<am_status_t>(decrypt_status);
		}
	    }else {
		status=AM_FAILURE;
	    }
	}
    }

	if (AM_SUCCESS == status) {
	parameter = AM_COMMON_IGNORE_PATH_INFO;
	status = am_properties_get_boolean_with_default(
			info_ptr->properties, parameter, 0,
			&(info_ptr->ignore_path_info));
    }

    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_IGNORE_PATH_INFO_FOR_NOT_ENFORCED_LIST;
        status = am_properties_get_boolean_with_default(
            info_ptr->properties, parameter, 0,
            &(info_ptr->ignore_path_info_for_not_enforced_list));
    }

	if (AM_SUCCESS == status) {
	parameter = AM_WEB_CONNECTION_TIMEOUT;
	status = am_properties_get_positive_number(info_ptr->properties,
				parameter, CONNECT_TIMEOUT, &info_ptr->connection_timeout);
    }

	if (AM_SUCCESS == status) {
	    parameter = AM_POLICY_CLOCK_SKEW;
	    status = am_properties_get_positive_number(info_ptr->properties,
	                 parameter, 0, &info_ptr->policy_clock_skew);
	    policy_clock_skew = info_ptr->policy_clock_skew;
	}

	/* To skip is_server_alive()? */
	if (AM_SUCCESS == status) {
	  parameter = AM_COMMON_IGNORE_SERVER_CHECK;
	  status = am_properties_get_boolean_with_default(info_ptr->properties,
	                                                  parameter,
	                                                  AM_FALSE,
	                                            &info_ptr->ignore_server_check);
	}

	/* Get iis6 auth_type property */
	if (AM_SUCCESS == status) {
		parameter = AM_WEB_AUTHTYPE_IN_IIS6_AGENT;
         	status = am_properties_get_with_default(info_ptr->properties, 
                                                 parameter, "dsame", 
                                                 &(info_ptr->authtype));

     }

    /* Get the redirect composite advice param */
    if (AM_SUCCESS == status) {
			parameter = AM_COMMON_USE_REDIRECT_FOR_ADVICE;
			status = am_properties_get_boolean_with_default(info_ptr->properties,
													parameter, AM_FALSE,
													&info_ptr->use_redirect_for_advice);
	}

    /*Get the sunwMethod removal flag*/
    if (AM_SUCCESS == status) {
			parameter = AM_COMMON_REMOVE_SUNWMETHOD;
			status = am_properties_get_boolean_with_default(info_ptr->properties,
													parameter, AM_FALSE,
													&info_ptr->remove_sunwmethod);
	}

    /* Get iis6 replay passwd key if defined */
    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_COMMON_PROPERTY_PREFIX_IIS6_REPLAYPASSWD_KEY;
        status = am_properties_get_with_default(info_ptr->properties, parameter,
                                   NULL, &info_ptr->iis6_replaypasswd_key);
    }

   /* Get the IIS5 filter priority param  */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_FILTER_PRIORITY;
	status = am_properties_get_with_default(info_ptr->properties,
			    parameter, filterPriority_default,
			    &info_ptr->filter_priority);
	am_web_log_info("Default priority => %s : Actual priority  => %s",
			    filterPriority_default,info_ptr->filter_priority);

    }


	// get owa_enabled flag
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_OWA_ENABLED;
	status = am_properties_get_boolean_with_default(
			info_ptr->properties, parameter, 0,
			&(info_ptr->owa_enabled));
    }

	// get owa_enabled_change_protocol flag
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_OWA_ENABLED_CHANGE_PROTOCOL;
	status = am_properties_get_boolean_with_default(
			info_ptr->properties, parameter, 0,
			&(info_ptr->owa_enabled_change_protocol));
    }

	// get owa_enabled_session_timeout_url
    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_WEB_OWA_ENABLED_SESSION_TIMEOUT_URL;
        status = am_properties_get_with_default(info_ptr->properties, parameter,
                              NULL, &info_ptr->owa_enabled_session_timeout_url);
    }

    if (AM_SUCCESS == status) {
       parameter = AM_WEB_NO_CHILD_THREAD_ACTIVATION_DELAY;
       status = am_properties_get_boolean_with_default(info_ptr->properties,
                                    parameter,
                                    AM_FALSE,
                                    &info_ptr->no_child_thread_activation_delay);
       no_child_thread_activation_delay = info_ptr->no_child_thread_activation_delay;
    }
    
    // get client ip header
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_CLIENT_IP_HEADER_PROPERTY;
        status = am_properties_get_with_default(info_ptr->properties, parameter,
                     NULL, &info_ptr->clientIPHeader);
    }

    // get client hostname header
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_CLIENT_HOSTNAME_HEADER_PROPERTY;
        status = am_properties_get_with_default(info_ptr->properties, parameter,
                     NULL, &info_ptr->clientHostnameHeader);
    }
     
	std::string notURL_str;
	const char* normURL = NULL;

	if (agent_info.notification_url != NULL &&
	    strlen(agent_info.notification_url) > 0) {
	    URL url(agent_info.notification_url);
	    url.getURLString(notURL_str);
	    normURL = notURL_str.c_str();

	    if (normURL == NULL || *normURL == '\0') {
		status = AM_FAILURE;
	    } else {
		agent_info.notification_url = strdup(normURL);

		if (!agent_info.notification_url) {
			status = AM_NO_MEMORY;
		}
	    }
	}

    if (AM_SUCCESS == status) {
	info_ptr->lock = PR_NewLock();
	if (NULL == info_ptr->lock) {
	    status = AM_NSPR_ERROR;
	}
    }

    if (AM_SUCCESS != status) {
	cleanup_properties(info_ptr);
        am_web_log_error("initialization error: %s(%s) failed, error = %s "
			"(%d): exiting...", function_name, parameter,
			am_status_to_string(status), status);
    }

    return status;
}

#if (defined(WINNT) || defined(_AMD64_))
extern "C" AM_WEB_EXPORT DWORD
am_web_get_iis_filter_priority() {

    DWORD priorityFlag =0; 
    const char *amAgentPriorityflag =agent_info.filter_priority;
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
 * initialize agent instance
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_init(const char *config_file)
{
    const char *thisfunc = "am_web_init";
    am_status_t status = AM_SUCCESS;
    const Properties *properties = NULL;


    if (! initialized) {
	// initialize log here so any error before properties file is
	// loaded will go to stderr. After it's loaded will go to log file.
	status = Log::initialize();

	if (AM_SUCCESS == status) {
	    try {
		status = load_agent_properties(&agent_info, config_file, B_TRUE);
	    }
	    catch(InternalException& ex) {
		am_web_log_error("%s: Exception encountered while loading "
				 "agent properties: %s: %s",
				 thisfunc, ex.what(), ex.getMessage());
		status = ex.getStatusCode();
	    }
	    catch(std::bad_alloc& exb) {
		status = AM_NO_MEMORY;
	    }
	    catch(std::exception& exs) {
		am_web_log_error("%s: Exception encountered while loading "
				 "agent properties: %s",
				 thisfunc, exs.what());
		status = AM_FAILURE;
	    }
	    catch(...) {
		am_web_log_error("%s: Unknown exception encountered "
				 "while loading agent properties.", thisfunc);
		status = AM_FAILURE;
	    }
	}

	if (AM_SUCCESS == status) {
	    am_resource_traits_t rsrcTraits;
#if !defined(WINNT) && !defined(_AMD64_) && !defined(HPUX) && !defined(AIX)
/* Logging locale is set as per the value defined in the properties file
   However we need to set the rest of locale behaviour so that code
   conversion routines will work correctly. Setting LC_MESSAGE to a
   different value  can be useful to run the agent in non English locale
   and dump the log in English  */
	    if(agent_info.locale != NULL) {
		setlocale(LC_MESSAGES,agent_info.locale);
		setlocale(LC_CTYPE,"");
		setlocale(LC_NUMERIC,"");
		setlocale(LC_TIME,"");
		setlocale(LC_MONETARY,"");
		am_web_log_info("%s: Logging Locale: %s",
			        thisfunc, agent_info.locale);
	    } else {
		setlocale(LC_ALL,"");
		am_web_log_info("am_web_init(): Logging Locale: OS locale");
	    }
	    textdomain(domainname);
#endif
	    populate_am_resource_traits(rsrcTraits);
	    status = am_policy_init(agent_info.properties);
	    if (AM_SUCCESS == status) {
		/* initialize policy */
		status = am_policy_service_init("iPlanetAMWebAgentService",
						agent_info.instance_name,
						rsrcTraits,
						agent_info.properties,
						&agent_info.policy_handle);
		if (AM_SUCCESS == status) {
		    initialized = TRUE;
		} else {
		    am_web_log_error("%s(%s) unable to "
				    "initialize the agent's policy object",
				    thisfunc, config_file);
		}
	    } else {
		am_web_log_error("%s(%s) unable to initialize "
				"the AM SDK, status = %s (%d)",
				thisfunc, config_file,
				am_status_to_string(status), status);
	    }
	    status = am_log_add_module("RemoteLog",
				       &agent_info.remote_LogID);
	    if (AM_SUCCESS != status) {
	      agent_info.remote_LogID = AM_LOG_ALL_MODULES;
	      status = AM_SUCCESS;
	    }

	    if (AM_SUCCESS == status) {
	      properties =
		    reinterpret_cast<Properties *>(agent_info.properties);
	      if (agent_info.postdatapreserve_enabled == AM_TRUE) {
		try {
		    agent_info.postcache_handle = new PostCache(*properties);
		}
		catch(InternalException& exi) {
		    status = exi.getStatusCode();
		}
		catch(std::exception& exs) {
		    am_web_log_error("%s: exception encountered while "
				     "initializing post cache handle: %s.",
				     thisfunc, exs.what());
		    status = AM_FAILURE;
		}
		catch(...) {
		    am_web_log_error("%s: Unknown exception encountered "
				     "while initializing post cache handle.",
				     thisfunc);
		    status = AM_FAILURE;
		}
	      }
	    }

	    // initialize the FQDN handler
	    if (AM_SUCCESS == status) {
	        try {
		  status = load_fqdn_handler(rsrcTraits.ignore_case?true:false);
		}
		catch(std::bad_alloc& exb) {
		    status = AM_NO_MEMORY;
		}
		catch(std::exception& exs) {
		    am_web_log_error("%s: exception encountered while "
				     "loading fqdn handler: %s",
				     thisfunc, exs.what());
		    status = AM_FAILURE;
		}
		catch(...) {
		    am_web_log_error("%s: Unknown exception encountered "
				     "while loading fqdn handler.",
				     thisfunc);
		    status = AM_FAILURE;
		}
	    }

	}

	if (AM_SUCCESS == status) {
	    const Properties& propertiesRef =
		*reinterpret_cast<Properties *>(agent_info.properties);

	    status = am_properties_get_with_default(agent_info.properties,
			      	                   AM_POLICY_PROFILE_ATTRS_MODE,
			      	                   AM_POLICY_SET_ATTRS_AS_NONE,
			      			   &profileMode);
	    if (status == AM_SUCCESS &&
		    profileMode != NULL && profileMode[0] != '\0') {
		if ((!strcasecmp(profileMode, AM_POLICY_SET_ATTRS_AS_COOKIE)) ||
		(!strcasecmp(profileMode, AM_POLICY_SET_ATTRS_AS_COOKIE_OLD))) {
		    setUserProfileAttrsMode = SET_ATTRS_AS_COOKIE;
		}
		else if ((!strcasecmp(profileMode,
			    	     AM_POLICY_SET_ATTRS_AS_HEADER)) ||
				 (!strcasecmp(profileMode,
			    	     AM_POLICY_SET_ATTRS_AS_HEADER_OLD))) {
		    setUserProfileAttrsMode = SET_ATTRS_AS_HEADER;
	        }
		else {
		    // anything other than COOKIE or HEADER will be NONE
		    setUserProfileAttrsMode = SET_ATTRS_NONE;
		}
	    }
	    else {
		setUserProfileAttrsMode = SET_ATTRS_NONE;
	    }

	    status = am_properties_get_with_default(agent_info.properties,
			      	                   AM_POLICY_SESSION_ATTRS_MODE,
			      	                   AM_POLICY_SET_ATTRS_AS_NONE,
			      			   &sessionMode);
	    if (status == AM_SUCCESS &&
		    sessionMode != NULL && sessionMode[0] != '\0') {
		if ((!strcasecmp(sessionMode, AM_POLICY_SET_ATTRS_AS_COOKIE)) ||
		(!strcasecmp(sessionMode, AM_POLICY_SET_ATTRS_AS_COOKIE_OLD))) {
		    setUserSessionAttrsMode = SET_ATTRS_AS_COOKIE;
		}
		else if ((!strcasecmp(sessionMode,
			    	     AM_POLICY_SET_ATTRS_AS_HEADER)) ||
			(!strcasecmp(sessionMode,
			    	     AM_POLICY_SET_ATTRS_AS_HEADER_OLD))) {
		    setUserSessionAttrsMode = SET_ATTRS_AS_HEADER;
	        }
		else {
		    // anything other than COOKIE or HEADER will be NONE
		    setUserSessionAttrsMode = SET_ATTRS_NONE;
		}
	    }
	    else {
		setUserSessionAttrsMode = SET_ATTRS_NONE;
	    }

	    status = am_properties_get_with_default(agent_info.properties,
			      	                 AM_POLICY_RESPONSE_ATTRS_MODE,
			      	                 AM_POLICY_SET_ATTRS_AS_NONE,
			      			 &responseMode);
	    if (status == AM_SUCCESS &&
		    responseMode != NULL && responseMode[0] != '\0') {
	     if ((!strcasecmp(responseMode, AM_POLICY_SET_ATTRS_AS_COOKIE)) ||
	     (!strcasecmp(responseMode, AM_POLICY_SET_ATTRS_AS_COOKIE_OLD))) {
		    setUserResponseAttrsMode = SET_ATTRS_AS_COOKIE;
		}
		else if ((!strcasecmp(responseMode,
			    	     AM_POLICY_SET_ATTRS_AS_HEADER)) ||
			(!strcasecmp(responseMode,
			    	     AM_POLICY_SET_ATTRS_AS_HEADER_OLD))) {
		    setUserResponseAttrsMode = SET_ATTRS_AS_HEADER;
	        }
		else {
		    // anything other than COOKIE or HEADER will be NONE
		    setUserResponseAttrsMode = SET_ATTRS_NONE;
		}
	    }
	    else {
		setUserResponseAttrsMode = SET_ATTRS_NONE;
	    }

	    if (AM_SUCCESS == status) {
	        status = am_properties_get_with_default(
				 agent_info.properties,
		    	         AM_POLICY_ATTRS_MULTI_VALUE_SEPARATOR,
			         "|",
			         &attrMultiValueSeparator);
	    }

	    try {
	      if (setUserProfileAttrsMode == SET_ATTRS_AS_COOKIE) {
		  Properties attributeMap;
		  const std::string &headerAttrs =
			propertiesRef.get(AM_POLICY_PROFILE_ATTRS_MAP, "");
		  attributeMap.parsePropertyKeyValue(headerAttrs, ',','|');
		  Log::log(agent_info.log_module, Log::LOG_DEBUG,
			   "Profile Attributes count=%u", attributeMap.size());
		  Properties::iterator iter;
		  for(iter = attributeMap.begin();
		        iter != attributeMap.end(); iter++) {
                     string attr = (*iter).second;
		     Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
				 "Profile Attribute=%s", attr.c_str());
			attrList.push_back(attr);
		  }
	      }

	      if (setUserSessionAttrsMode == SET_ATTRS_AS_COOKIE) {
		    // Repeat the same for the session attribute map
		    Properties sessionAttributeMap;
		    const std::string &sessionAttrs =
			propertiesRef.get(AM_POLICY_SESSION_ATTRS_MAP, "");
		    sessionAttributeMap.parsePropertyKeyValue(sessionAttrs,
							      ',','|');
		    Log::log(agent_info.log_module, Log::LOG_DEBUG,
			    "Session Attributes count=%u",
			     sessionAttributeMap.size());
		    Properties::iterator iter_sessionAttr;
		    for(iter_sessionAttr = sessionAttributeMap.begin();
			    iter_sessionAttr != sessionAttributeMap.end();
			       iter_sessionAttr++) {
			string attr = (*iter_sessionAttr).second;
			Log::log(agent_info.log_module,
				 Log::LOG_MAX_DEBUG,
				 "Session Attribute=%s", attr.c_str());
			attrList.push_back(attr);
		    }
                }

	       if (setUserResponseAttrsMode == SET_ATTRS_AS_COOKIE) {
		    // Repeat the same for the response attribute map
		    Properties responseAttributeMap;
		    const std::string &responseAttrs =
			propertiesRef.get(AM_POLICY_RESPONSE_ATTRS_MAP, "");
		    responseAttributeMap.parsePropertyKeyValue(responseAttrs,
							      ',','|');
		    Log::log(agent_info.log_module, Log::LOG_DEBUG,
			    "Response Attributes count=%u",
			     responseAttributeMap.size());
		    Properties::iterator iter_responseAttr;
		    for(iter_responseAttr = responseAttributeMap.begin();
			iter_responseAttr != responseAttributeMap.end();
			iter_responseAttr++) {
			string attr = (*iter_responseAttr).second;
			Log::log(agent_info.log_module,
				 Log::LOG_MAX_DEBUG,
				 "Response Attribute=%s", attr.c_str());
			attrList.push_back(attr);
		    }
               }
	     } catch (InternalException& exi) {
		    status = exi.getStatusCode();
	     } catch (std::bad_alloc& exb) {
		    am_web_log_error("%s: Bad alloc error encountered while "
				     "parsing LDAP attributes as cookie: %s",
				     thisfunc, exb.what());
		    status = AM_NO_MEMORY;
	     } catch (std::exception& exs) {
		    am_web_log_error("%s: Exception encountered while "
				     "parsing LDAP attributes as cookie: %s",
				     thisfunc, exs.what());
		    status = AM_FAILURE;
	     } catch (...) {
		    am_web_log_error("%s: Unknown exception encountered while "
				     "parsing LDAP attributes as cookie.",
				     thisfunc);
		    status = AM_FAILURE;
	    }
	}

	if (AM_SUCCESS == status) {
	    status = am_properties_get_with_default(
		    	agent_info.properties,
		    	AM_POLICY_PROFILE_ATTRS_COOKIE_PFX,
			"HTTP_",
			&attrCookiePrefix);
	    am_web_log_debug("%s: Using cookie prefix %s.",
			     thisfunc, attrCookiePrefix);
	}

	if (AM_SUCCESS == status) {
	    status = am_properties_get_with_default(
		    	agent_info.properties,
		    	AM_POLICY_PROFILE_ATTRS_COOKIE_MAX_AGE,
			"300",
			&attrCookieMaxAge);
	    am_web_log_debug("%s: Using cookie max-age %s.",
			     thisfunc, attrCookieMaxAge);
	}

	if (AM_SUCCESS != status) {
	    if (agent_info.fqdn_handler != NULL) {
                delete agent_info.fqdn_handler;
            }
	    cleanup_properties(&agent_info);
	}
    }

    return status;
}


/**
 * Method to close an initialized policy evaluator
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_cleanup()
{
    const char *thisfunc = "am_web_cleanup()";
    am_status_t status = AM_FAILURE;

    if (initialized) {
	 am_web_log_debug("%s: cleanup sequence initiated.", thisfunc);
	cleanup_properties(&agent_info);

	if (agent_info.postdatapreserve_enabled) {
	    if(agent_info.postcache_handle != NULL){
		delete agent_info.postcache_handle;
		Log::log(agent_info.log_module, Log::LOG_DEBUG,
			 "POST cache hashtable shutdown.");
	    } else {
		am_web_log_warning("%s: POST cache hashtable was enabled but "
				   "no post cache handle was found.", thisfunc);
		status = AM_FAILURE;
	    }
	}

        if (AM_SUCCESS == status) {
	    try {
		status = unload_fqdn_handler();
	    }
	    catch (std::exception& exs) {
		am_web_log_error("%s: Exception caught while unloading "
				 "fqdn handler: %s", thisfunc, exs.what());
		status = AM_FAILURE;
	    }
	    catch (...) {
		am_web_log_error("%s: Unknown exception caught while unloading "
				 "fqdn handler.", thisfunc);
		status = AM_FAILURE;
	    }
        }
	status = am_cleanup();

	initialized = AM_FALSE;
    } else {
	status = AM_SUCCESS;
    }

    return status;
}

/* Throws std::exceptions's from URL methods */
am_bool_t in_not_enforced_list(URL &urlObj)
{
    const char *thisfunc = "in_not_enforced_list";
    std::string baseURL_str;
    const char *baseURL;
    std::string dummyNotEnforcedUrl_str;
    std::string url_str;
    const char *url = NULL;
    am_bool_t found = AM_FALSE;
    size_t i = 0;
    am_resource_traits_t rsrcTraits;
    populate_am_resource_traits(rsrcTraits);

    am_bool_t access_denied_url_match_flag = AM_FALSE;
    am_bool_t dummy_post_url_match_flag = AM_FALSE;

    urlObj.getURLString(url_str);
    url = url_str.c_str();
    urlObj.getBaseURL(baseURL_str);
    baseURL = baseURL_str.c_str();

    // First check is the access denied URL 
    if (agent_info.access_denied_url != NULL) {
        // We append the sunwerrorcode as a querystring to the access denied url
        // if the policy denies access to the resouce. This query parameter
        // needs to be removed from the url if present before comparison
        std::string urlStr;
        const char* accessUrl = url;
        std::string accessDeniedUrlStr;
        const char* access_denied_url = NULL;

        URL accessDeniedUrlObj(agent_info.access_denied_url);
        accessDeniedUrlObj.getURLString(accessDeniedUrlStr);
        access_denied_url = accessDeniedUrlStr.c_str();

        if (urlObj.findQueryParameter(sunwErrCode)) {
            urlObj.removeQueryParameter(sunwErrCode);
            urlObj.getURLString(url_str);
            if (urlObj.findQueryParameter(agent_info.url_redirect_param)) {
                urlObj.removeQueryParameter(agent_info.url_redirect_param);
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
                  thisfunc, accessUrl, agent_info.access_denied_url);
        } else {
            am_web_log_debug("%s: Matching %s with access_denied_url %s: FALSE",
                  thisfunc, accessUrl, agent_info.access_denied_url);
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
             ( AM_EXACT_PATTERN_MATCH == dummy_post_url_match ))
    {
        dummy_post_url_match_flag = AM_TRUE;
    }

    if (access_denied_url_match_flag == AM_TRUE) {
        am_web_log_debug("%s: The requested URL %s "
                    "is the access denied URL which is always not enforced.",
                    thisfunc, url);
        found = AM_TRUE;
    } else if (dummy_post_url_match_flag == AM_TRUE) {
        am_web_log_debug("%s: The requested URL %s "
                    "is the dummy post URL which is always not enforced.",
                    thisfunc, url, baseURL);
        found = AM_TRUE;
    } else {
        for (i = 0;
             (i < agent_info.not_enforced_list.size) && (AM_FALSE == found);
              i++) {
            am_resource_match_t match_status;

            if (agent_info.not_enforced_list.list[i].has_patterns==AM_TRUE) {
                if (AM_TRUE == 
                     agent_info.ignore_path_info_for_not_enforced_list) 
                {
                    match_status = am_policy_compare_urls(
                            &rsrcTraits, agent_info.not_enforced_list.list[i].url,
                            baseURL, B_TRUE);
                } else {
                    match_status = am_policy_compare_urls(
                            &rsrcTraits, agent_info.not_enforced_list.list[i].url,
                            url, B_TRUE);
                }
            } else {
                match_status = am_policy_compare_urls(
                    &rsrcTraits, agent_info.not_enforced_list.list[i].url,
                    url, B_FALSE);
            }

            if (AM_EXACT_MATCH == match_status ||
                AM_EXACT_PATTERN_MATCH == match_status) {
                am_web_log_debug("%s(%s): "
                    "matched '%s' entry in not-enforced list", thisfunc,
                    url, agent_info.not_enforced_list.list[i].url);
                found = AM_TRUE;
            }
        }

        if (agent_info.reverse_the_meaning_of_not_enforced_list == AM_TRUE) {
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

/* Check if the current client ip address is in the list of not enforced client ip list.
 * if it matches, then return true or Otherwise false
 */
am_bool_t in_not_enforced_ip_list(const char *client_ip) {
    const char *thisfunc = "in_not_enforced_ip_list";
    am_bool_t found = AM_FALSE;

    if (agent_info.notenforcedIPmode != NULL && strncasecmp(agent_info.notenforcedIPmode, "cidr", 4) == 0) {
        /*cidr match is enabled*/
        found = notenforced_ip_cidr_match(client_ip, agent_info.not_enforce_IPAddr_set) ? AM_TRUE : AM_FALSE;
    } else {
        /*fall back to plain string match */
        std::vector<string> ipVector;
        std::string ipAddr(client_ip);
        size_t dot = 0, curPos1 = 0;
        size_t ipAddrSize = ipAddr.size();
        while (dot < ipAddrSize) {
            std::string ipElement;
            dot = ipAddr.find('.', curPos1);
            if (dot == std::string::npos) {
                ipElement = ipAddr.substr(curPos1, ipAddrSize - curPos1);
                dot = ipAddrSize;
            } else {
                ipElement = ipAddr.substr(curPos1, dot - curPos1);
            }
            curPos1 = dot + 1;
            ipVector.push_back(ipElement);
        }

        ipAddrSet_t::iterator iter;
        for (iter = agent_info.not_enforce_IPAddr->begin();
                iter != agent_info.not_enforce_IPAddr->end();
                iter++) {
            std::vector<string> not_enforced_ip = (std::vector<std::string>) * iter;
            am_web_log_debug("Comparing %s.%s.%s.%s and %s", not_enforced_ip[0].c_str(), not_enforced_ip[1].c_str(),
                    not_enforced_ip[2].c_str(), not_enforced_ip[3].c_str(), client_ip);
            for (int i = 0; i < 4; i++) {
                if ((strcmp(not_enforced_ip[i].c_str(), "*") == 0) ||
                        (strcmp(not_enforced_ip[i].c_str(), ipVector[i].c_str()) == 0)) {
                    if (i == 3) {
                        am_web_log_debug("%s(%s): "
                                "matched '%s.%s.%s.%s' entry in not-enforced list", thisfunc, client_ip,
                                not_enforced_ip[0].c_str(), not_enforced_ip[1].c_str(),
                                not_enforced_ip[2].c_str(), not_enforced_ip[3].c_str());
                        found = AM_TRUE;
                    }
                } else {
                    break;
                }
            }
            if (found == AM_TRUE)
                break;
        }
    }
    if (agent_info.reverse_the_meaning_of_not_enforced_list == AM_TRUE) {
        am_web_log_debug("%s: not enforced list is reversed, "
                "only matches will be enforced.", thisfunc);
        found = (AM_TRUE == found) ? AM_FALSE : AM_TRUE;
    }
    if (AM_TRUE == found) {
        am_web_log_debug("%s: Allowing access to %s ", thisfunc, client_ip);
    } else {
        am_web_log_debug("%s: Enforcing access control for %s ",
                thisfunc, client_ip);
    }
    return found;
}

void set_host_ip_in_env_map(const char *client_ip,
			    const am_map_t env_parameter_map)
{
    PRStatus prStatus;
    PRNetAddr address;
    PRHostEnt hostEntry;
    char buffer[PR_NETDB_BUF_SIZE];

    Log::log(agent_info.log_module, Log::LOG_DEBUG,
	     "set_host_ip_in_env_map: map_insert: "
	     "client_ip=%s", client_ip);
    am_map_insert(env_parameter_map, requestIp, client_ip, AM_TRUE);

    if (agent_info.getClientHostname) {
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
	Log::log(agent_info.log_module, Log::LOG_DEBUG,
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
am_web_get_token_from_assertion(char * enc_assertion, char **token )
{
    const char *thisfunc = "am_web_get_token_from_assertion()";
    am_status_t status = AM_FAILURE;
    char * str = NULL;
    char *tmp1 = NULL;
    char *tmp2 = NULL;
    char * dec_assertion = NULL;

    dec_assertion = am_web_http_decode(enc_assertion, strlen(enc_assertion));
    if(dec_assertion != NULL)
    tmp1 = strchr(dec_assertion, '=');
    if((tmp1 != NULL)  &&
      !(strncmp(dec_assertion, LARES_PARAM, strlen(LARES_PARAM)))){
        tmp2 = tmp1 + 1;
        decode_base64(tmp2, tmp1);
	am_web_log_debug("Received Authn Response = %s", tmp1);
        str = tmp1;
         if(*tmp1 == NULL)
                {
                        am_web_log_error("Improper LARES param received");
                        status= AM_FAILURE;
                        return status;
                }
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
				    *token = strdup(elemValue.c_str());
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
		             "AuthnResponse", agent_info.cookie_name);
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
buildSetCookieHeader(cookie_info_t *cookie)
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
 * if configured in AMAgent.properties.
 * Returns true if something in url was overridden, false otherwise.
 */
static bool
overrideProtoHostPort(URL& url)
{
    bool override = false;
    if (agent_info.override_protocol) {
	url.setProtocol(std::string(agent_info.agent_server_url.protocol));
	override = true;
    }

    if (agent_info.override_host || url.getHost().size() == 0) {
	url.setHost(agent_info.agent_server_url.host);
	override = true;
    }

    if (agent_info.override_port) {
	url.setPort(agent_info.agent_server_url.port);
	override = true;
    }
    return override;
}

am_status_t
log_access(am_status_t access_status,
	   const char *remote_user,
	   const char *user_sso_token,
	   const char *url)
{
    am_status_t status = AM_SUCCESS;
    char fmtStr[MSG_MAX_LEN];
#if (defined(WINNT) || defined(_AMD64_))
    UINT key = 0;
#else
    const char *key = NULL;
#endif

    if (agent_info.log_access_type != LOG_ACCESS_NONE) {
	switch(access_status) {
	case AM_SUCCESS:
	    if (agent_info.log_access_type & LOG_ACCESS_ALLOW) {
		key = AM_WEB_ALLOW_USER_MSG;
	    }
	    break;
	case AM_ACCESS_DENIED:
	    if (agent_info.log_access_type & LOG_ACCESS_DENY) {
		key = AM_WEB_DENIED_USER_MSG;
	    }
	    break;
	default:
	    break;
	}
	if (key != NULL) {
	    memset(fmtStr, 0, sizeof(fmtStr));
	    get_string(key, fmtStr, sizeof(fmtStr));
	    Log::log(agent_info.remote_LogID, Log::LOG_INFO,
		     fmtStr, remote_user, url);
	    status = Log::rlog(agent_info.remote_LogID,
			       AM_LOG_LEVEL_INFORMATION, user_sso_token,
			       fmtStr, remote_user, url);
	}
    }
    return status;
}

am_bool_t
is_url_not_enforced(const char *url, const char *client_ip,
                    std::string pInfo, std::string query)
{
    const char *thisfunc = "is_url_enforced()";
    am_status_t status = AM_SUCCESS;
    am_bool_t foundInNotEnforcedList = AM_FALSE;
    am_bool_t inNotenforceIP = AM_FALSE;
    am_bool_t isNotEnforced = AM_FALSE;
    am_bool_t isLogoutURL = AM_FALSE;
    std::string urlStr(url);
    
    // Check all required arguments.
    if (url == NULL || *url == '\0' || 
            (AM_TRUE == agent_info.check_client_ip &&
            NULL == client_ip || *client_ip == '\0')) {
        status = AM_INVALID_ARGUMENT;
    }

    if (status == AM_SUCCESS) {
        try {
            // See if it client ip is in the not enforced client ip list.
            if (agent_info.not_enforce_IPAddr != NULL) {
                inNotenforceIP = in_not_enforced_ip_list(client_ip);
		if (AM_TRUE == inNotenforceIP)  {
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
                if ((AM_TRUE == agent_info.ignore_path_info) && (!pInfo.empty())) {
                    // Add again the path info and query to the url as they
                    // were removed for the AM evaluation
                    am_web_log_debug("%s: Add path info (%s) and query (%s) that were "
                                     "removed for AM evaluation", 
                                     thisfunc, pInfo.c_str(), query.c_str());
                    urlStr.append(pInfo).append(query);
                }
                URL url_again(urlStr, pInfo);
                foundInNotEnforcedList = in_not_enforced_list(url_again);
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
        if (agent_info.logout_url_list.size > 0 &&
            am_web_is_logout_url(url) == B_TRUE) {
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
                   std::string &pInfo)
{
    const char *thisfunc = "get_normalized_url()";
    am_status_t status = AM_SUCCESS;
    am_bool_t isNotEnforced = AM_FALSE;
    std::string new_url_str;

    // Check argument
    if (url_str == NULL || *url_str == '\0') {
        status = AM_INVALID_ARGUMENT;
    }
    if (status == AM_SUCCESS) {
        // Parse & canonicalize URL
        am_web_log_max_debug("%s: Original url: %s", thisfunc, url_str);
        try {
            if(path_info != NULL && strlen(path_info) > 0) {
                pInfo=path_info;
                URL urlObject(url_str, pInfo);
                (void)overrideProtoHostPort(urlObject);
                am_web_log_max_debug("%s: Path info: %s", thisfunc, path_info);
                if (AM_TRUE == agent_info.ignore_path_info) {
                    am_web_log_max_debug("%s: Ignoring path info for "
                                         "policy evaluation.", thisfunc);
                    urlObject.getBaseURL(normalizedURL);
                    pInfo.erase();
                } else {
                    urlObject.getURLString(normalizedURL);
                }
            } else {
                URL urlObject(url_str);
                (void)overrideProtoHostPort(urlObject);
                urlObject.getURLString(normalizedURL);
            }
            am_web_log_max_debug("%s: Normalized url: %s", 
                                 thisfunc, normalizedURL.c_str());
        } catch(InternalException &iex) {
            Log::log(agent_info.log_module, Log::LOG_ERROR, iex);
            status = iex.getStatusCode();
        } catch(std::exception &ex) {
            Log::log(agent_info.log_module, Log::LOG_ERROR, ex);
            status = AM_FAILURE;
        } catch(...) {
            Log::log(agent_info.log_module, Log::LOG_ERROR,
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
             am_policy_result_t *result)
{
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
    am_bool_t isLogoutURL = AM_FALSE;
    am_status_t log_status = AM_SUCCESS;
    char *encodedUrl = NULL;
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

    // check all required arguments.
    if (url_str == NULL || action_name == NULL ||
            *url_str == '\0' || action_name == '\0' ||
            env_parameter_map == NULL || result == NULL ||
            (AM_TRUE == agent_info.check_client_ip &&
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
        status = get_normalized_url(url_str, path_info, normalizedURL, pInfo);
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
            if (AM_FALSE == is_valid_fqdn_access(url)) {
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
                                            originalQuery);

        // check if it's the logout URL
        if (agent_info.logout_url_list.size > 0 &&
            am_web_is_logout_url(url) == B_TRUE) {
            isLogoutURL = AM_TRUE;
            am_web_log_max_debug("%s: url %s IS logout url.", thisfunc,url);
        }
    }

    //Check whether agent is operating in cookieless mode.
    //If yes, extract sso token from the url. The modified url after
    //removal of sso token needs to be sent for policy evaluations.
    if (sso_token == NULL || '\0' == sso_token[0]) {
        if(url != NULL && strlen(url) > 0) {
            am_web_get_parameter_value(url,
                                       am_web_get_cookie_name(),
                                       &urlSSOToken);

            if (urlSSOToken != NULL) {
                sso_token = urlSSOToken;
                am_web_log_debug("am_web_is_access_allowed(): "
                        "sso token from request url = \"%s\"", sso_token);
                modifiedURL = (char *) malloc(strlen(url));
                if(modifiedURL != NULL) {
                    am_web_remove_parameter_from_query(url,
                                                   am_web_get_cookie_name(),
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

    // No further processing if ignore_policy_evaluation_if_notenforced
    // is set to true and URL is not enforced and
    // also not logout url.
    // If the accessed URL is logout url, then it requires processing.
    if (isNotEnforced == AM_TRUE &&
        agent_info.ignore_policy_evaluation_if_notenforced &&
        isLogoutURL == AM_FALSE ) {
        am_web_log_debug("URL = %s is in notenforced list and  "
                        "ignore_policy_evaluation_if_notenforced is set to "
                        "TRUE", url);
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
            // Then do am_policy_evaluate to get session info (userid, attributes)
            // whether or not url is enforced, whether or not do_sso_only is true.
            // but ignore result if url not enforced or do_sso_only.
            // Note: This will be replaced by a different policy call to just get
            // user ldap attributes or id so the policy does not get evaluated.
            } else {
                KeyValueMap action_results_map;
                am_status_t eval_status = AM_SUCCESS;
                /* the following should not throw exceptions */
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
                        set_host_ip_in_env_map(client_ip, env_parameter_map);
                    }
                }
                if (isNotEnforced == AM_TRUE &&
                    setUserProfileAttrsMode == SET_ATTRS_NONE)  {
                    am_web_log_debug("%s: URL = %s is in notenforced list and ldap "
                                   "attribute mode is NONE", thisfunc, url);
                } else {
                    if (agent_info.encode_url_special_chars == AM_TRUE ) {
                        encodedUrlSize = (strlen(url)+1)*4;
                        encodedUrl = (char *) malloc (encodedUrlSize);
                        // Check url for special chars
                        if (encodedUrl != NULL) {
                            bool url_spl_flag = false;
                            memset(encodedUrl, 0, encodedUrlSize);
                            for(int i = 0; i < strlen(url); i++) {
                                if (( url[i] <  32) || ( url[i] > 127)) {
                                    url_spl_flag = true;
                                }
                            }
                            if (url_spl_flag != NULL) {
                                encode_url(url, encodedUrl);
                                am_web_log_debug("%s: Original URL = %s", thisfunc, url);
                                url = encodedUrl;
                                am_web_log_debug("%s: Encoded URL = %s", thisfunc, encodedUrl);
                            }
                        } else {
                            am_web_log_error("%s: Failed to allocate"
                                 "%d bytes for encodedUrl",thisfunc, encodedUrlSize);
                        }
                    }

                    if (AM_TRUE == isNotEnforced ||
                            AM_TRUE == agent_info.do_sso_only) {
                        ignorePolicyResult = AM_TRUE;
                    }
                    if(agent_info.am_revision_number == NULL) {
                        agent_info.am_revision_number = (char *)malloc(AM_REVISION_LEN);
                    }
                    eval_status = am_policy_evaluate_ignore_url_notenforced(
                                agent_info.policy_handle,
                                sso_token, url, action_name,
                                env_parameter_map,
                                reinterpret_cast<am_map_t>
                                (&action_results_map),
                                result,
                                ignorePolicyResult,
                                &agent_info.am_revision_number);

                    // if eval policy success, check policy decision in the result
                    // if it is enforced and not in do_sso_only mode.
                    if (eval_status == AM_SUCCESS) {
                        if (AM_FALSE == isNotEnforced &&
                            AM_FALSE == agent_info.do_sso_only) {
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
                                am_web_log_error("%s(%s,%s): "
                                       "Exception encountered "
                                       "while checking policy results: %s",
                                       thisfunc, url, action_name, exs.what());
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

                        /* Note: This is a temporary workaround for
                        * determining if the session is valid. This should be
                        * replaced by an new and more appropriate function call
                        * which only evaluates if the current session
                        * is valid or not.  */
                        if (AM_TRUE == agent_info.do_sso_only &&
                                   (eval_status == AM_NO_POLICY ||
                                   eval_status == AM_INVALID_ACTION_TYPE)) {
                                   am_web_log_debug("%s(%s, %s) do_sso_only "
                                   "- ignore policy eval result of no policy or "
                                   "invalid action",
                                   thisfunc, url, action_name);
                        } else if (AM_FALSE == isNotEnforced) {
                            status = eval_status;
                        }
                    }

                    // access denied, no policy and invalid action type are three
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

                    // now check if the ip address matches what is in sso token,
                    // if check client ip address is true and url is enforced.
                    if (AM_SUCCESS == status && AM_FALSE == isNotEnforced &&
                        AM_TRUE == agent_info.check_client_ip &&
                        result->remote_IP != NULL) {
                        if (client_ip == NULL ||
                            strcmp(result->remote_IP, client_ip) != 0) {
                            status = AM_ACCESS_DENIED;
                            am_web_log_warning("%s: Client ip [%s] does not match "
                                       "sso token ip [%s]. Denying access.",
                                       thisfunc, result->remote_IP,
                                       client_ip);
                        } else {
                            am_web_log_debug("%s: Client ip [%s] matched "
                                "sso token ip.", thisfunc,
                                 result->remote_IP);
                        }
                    }
                }

                // invalidate user's sso token if it's the logout URL,
                // ignore the invalidate status.
                // Note that this must be done *after* am_policy_evaluate()
                // so we can get the user's id and pass it to the web app.
                // Can't get the user's id after session's been invalidated.
                if (isLogoutURL && sso_token != NULL && sso_token[0] != '\0') {
                    am_web_log_debug("invalidating session %s", sso_token);
                    am_status_t logout_status =
                    am_policy_invalidate_session(agent_info.policy_handle,
                                           sso_token);
                    if (AM_SUCCESS != logout_status) {
                        am_web_log_warning(
                             "%s: Error %s invalidating session %s.",
                             thisfunc, am_status_to_name(logout_status),
                             sso_token);
                    } else {
                        am_web_log_debug("%s: Logged out session id %s.",
                             thisfunc, sso_token);
                    }
                }
            }
        }
    }

    // now set user in the result.
    // result->remote_user gets freed in am_policy_result_destroy().
    if (AM_SUCCESS == status && result->remote_user == NULL) {
        if (AM_TRUE == agent_info.anon_remote_user_enable &&
                    agent_info.unauthenticated_user != NULL &&
                    *agent_info.unauthenticated_user != '\0') {
            result->remote_user = strdup(agent_info.unauthenticated_user);
        } else {
            result->remote_user = NULL;
        }
        am_web_log_debug("%s: remote user set to unauthenticated user %s",
                    thisfunc, result->remote_user);
    }

    // log the final access allow/deny result in IS's audit log.
    rmtUsr = (result->remote_user == NULL) ? "unknown user" :
                result->remote_user;
    /**
     * We do not log the notenforced list accesses for allow,
     * because, if agent is installed on top of DSAME, the
     * access to loggingservice, which gets allowed by the
     * not enforced list would cause a recursion tailspin.
     */
    if (AM_FALSE == isNotEnforced) {
        log_status = log_access(status, result->remote_user, sso_token, url);
        if (log_status == AM_SUCCESS) {
            am_web_log_debug("%s: Successfully logged to remote server "
                        "for %s action by user %s to resource %s.",
                        thisfunc, action_name, rmtUsr, url);
        } else {
            am_web_log_error("%s: Error '%s' encountered while "
                    "logging to remote server for "
                    "%s action by user %s to resource %s.",
                    thisfunc, am_status_to_string(log_status),
                    action_name, rmtUsr, url);
            if (log_status == AM_NSPR_ERROR) {
                status = AM_INVALID_SESSION;
            } else if (agent_info.denyOnLogFailure) {
                status = AM_ACCESS_DENIED;
            }
        }
    }

    if(encodedUrl != NULL) {
        am_web_free_memory(encodedUrl);
    }
    am_web_free_memory(modifiedURL);
    am_web_log_info("%s(%s, %s) returning status: %s.",
                    thisfunc, url, action_name, am_status_to_string(status));

    return status;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_notification(const char *request_url)
{
    const char *thisfunc = "am_web_is_notification()";
    boolean_t result = B_FALSE;
    std::string request_url_str;

    if (am_policy_is_notification_enabled(agent_info.policy_handle)) {
	try {
	    // check override proto/host/port for notification url.
	    // this would be true if notifications url is coming from
	    // outside a firewall.
		URL url(request_url);

	    if (agent_info.override_notification_url) {
			overrideProtoHostPort(url);
	    }

		url.getURLString(request_url_str);
		request_url = request_url_str.c_str();

		if (!strcasecmp(request_url, agent_info.notification_url)) {
			result = B_TRUE;
		} else {
			am_web_log_max_debug("%s, %s is not notification url %s.",
					 thisfunc, request_url,
					 agent_info.notification_url);
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
			   size_t data_length)
{
    am_status_t status;

    Log::log(agent_info.log_module, Log::LOG_DEBUG,
	     "am_web_handle_notification() data is: %.*s", data_length,
	     data);

    status = am_policy_notify(agent_info.policy_handle,
				 data, data_length);
    if (AM_SUCCESS != status) {
	am_web_log_error("am_web_handle_notification() error processing "
			"notification: status = %s (%d), notification data = "
			"%.*s", am_status_to_string(status), status,
			data_length, data);
    }
}

/* Throws std::exception's from string methods */
am_status_t
getValid_FQDN_URL(const char *goto_url, string &result) {
    am_status_t retVal = AM_SUCCESS;
    const std::string newURL(
		agent_info.fqdn_handler->getValidFqdnResource(goto_url));
    if(!newURL.empty()) {
	result = newURL;
    } else {
	Log::log(agent_info.log_module, Log::LOG_ERROR,
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
                       const char *query, char **request_url)
{
    const char *thisfunc = "am_web_get_request_url()";
    am_status_t retVal = AM_SUCCESS;
    URL url;

    if(uri != NULL) {
        url.setURI(uri);
    }
    if(query != NULL) {
        url.setQuery(query);
    }
    if(protocol != NULL) {
        url.setProtocol(protocol);
    }
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
           if(port > PORT_MIN_VAL && port < PORT_MAX_VAL) {
               url.setPort(port);
           }
       }
    }
    // sets override request url, if override is set.
    std::string urlStrTmp;
    url.getURLString(urlStrTmp);
    if (agent_info.override_notification_url || am_web_is_notification(urlStrTmp.c_str()) == B_FALSE) {
	(void)overrideProtoHostPort(url);
    }
    // Save the url in request_url
    std::string urlStr;
    url.getURLString(urlStr);
    *request_url = (char *)malloc(urlStr.size() + 1);
    if(*request_url == NULL) {
        am_web_log_error("%s: Unable to allocate memory for request_url.",
                         thisfunc);
        retVal = AM_NO_MEMORY;
    } else {
        strcpy(*request_url, urlStr.c_str());
        am_web_log_debug("%s: Request url: %s", thisfunc, *request_url);
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
                 void (*set_method)(void **, char *))
{
    const char *thisfunc = "am_web_check_cookie_in_post";
    char *recv_token = NULL;
    am_status_t status = AM_SUCCESS;

    // Check arguments
    if(response == NULL || strlen(response) == 0) {
        am_web_log_error("%s: Response object is NULL or empty.", thisfunc);
        status = AM_INVALID_ARGUMENT;
    }
    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: Post data received = %s", thisfunc, response);
        // Check for Liberty response
        if(responseIsCookie) {
            recv_token = strdup(response);
            if (recv_token == NULL) {
                am_web_log_error("%s: Unable to allocate memory.", thisfunc);
                status = AM_NO_MEMORY;
            }
        } else {
            status = am_web_get_token_from_assertion(response, &recv_token);
        }
    }
    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: recv_token : %s", thisfunc, recv_token);
        // Set cookie in browser for the foreign domain.
        status = am_web_do_cookie_domain_set(set_cookie, args, recv_token);
    }
    if (status == AM_SUCCESS) {
        *dpro_cookie = strdup(recv_token);
        if(*dpro_cookie == NULL) {
            am_web_log_error("%s: Unable to allocate memory.", thisfunc);
            status = AM_NO_MEMORY;
        } else {
            free(recv_token);
            if (agent_info.useSunwMethod == AM_TRUE) {
                status = am_web_remove_parameter_from_query(*request_url,
                               REQUEST_METHOD_TYPE, request_url);
            }
            strcpy(method, *orig_req);
            set_method(args, *orig_req);
        }
    }
    return status;
}

/* now used by function add_cdsso_elements_to_redirect_url only */
static char *prtime_2_str(char *buffer, size_t buffer_len)
{
    PRTime timestamp;
    PRExplodedTime exploded_time;
    int n_written = 0;

    PR_Lock(agent_info.lock);
    timestamp = PR_Now();
    PR_Unlock(agent_info.lock);

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
string add_cdsso_elements_to_redirect_url() {
    string providerId = agent_info.agent_server_url.url;

	const char* orgName = NULL;
	am_status_t status = am_properties_get( agent_info.properties,
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
    prtime_2_str(strtime,AM_WEB_MAX_POST_KEY_LENGTH);
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
			char **redirect_url) {
    return am_web_get_url_to_redirect(status, advice_map, goto_url, NULL,
				      NULL, redirect_url);
}

extern "C" AM_WEB_EXPORT am_status_t
am_web_get_url_to_redirect(am_status_t status,
               const am_map_t advice_map,
               const char *goto_url,
               const char* method,
               void *reserved,
               char **redirect_url)
{
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
        if (overrideProtoHostPort(gotoURL)) {
            gotoURL.getURLString(orig_url);
            goto_url = orig_url.c_str();
        }
        am_web_log_max_debug("%s: goto URL is %s", thisfunc, goto_url);

        // if previous status was invalid fqdn access,
        // redirect to the valid fqdn url.
        if (status == AM_INVALID_FQDN_ACCESS) {
            try {
                std::string valid_fqdn;
                if ((ret =
                  getValid_FQDN_URL(goto_url, valid_fqdn)) != AM_SUCCESS) {
                    am_web_log_error("%s: getValid_FQDN_URL failed with "
                         "error: %s", thisfunc, am_status_to_string(ret));
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

        // if previous status was invalid session or if there was a policy
        // advice, redirect to the IS login page. If not, redirect to the
        // configured access denied url if any.
        } else {
            // Check for advice.
            // If we have advice information, then we attempt to customize
            // the redirect URL based on the provided advice.  As of the
            // Alpha code-freeze for Identity Server 6.0, the URL may
            // contain only one of module, level, role, user, or service.
            // I (pds) have made the (semi-)arbitrary decision that if
            // both types of advice are available we will prefer to
            // specify the auth module (scheme), because the module seems
            // more specific than the level.
            
            const char *auth_advice_url_prefix = "";
            size_t auth_advice_url_prefix_len = 0;
            const char *auth_advice_value = "";
            size_t auth_advice_value_len = 0;
            const char *session_adv_value = "";

            if (AM_MAP_NULL != advice_map) {
                auth_advice_value = am_map_find_first_value(advice_map,
                      AUTH_SCHEME_KEY);
                if (NULL != auth_advice_value) {
                    auth_advice_url_prefix = AUTH_SCHEME_URL_PREFIX;
                    auth_advice_url_prefix_len = AUTH_SCHEME_URL_PREFIX_LEN;
                    auth_advice_value_len = strlen(auth_advice_value);
                } else {
                    auth_advice_value = am_map_find_first_value(advice_map,
                                        AUTH_LEVEL_KEY);
                    if (NULL != auth_advice_value) {
                        auth_advice_url_prefix = AUTH_LEVEL_URL_PREFIX;
                        auth_advice_url_prefix_len = AUTH_LEVEL_URL_PREFIX_LEN;
                        auth_advice_value_len = strlen(auth_advice_value);
                    } else {
                        auth_advice_value = am_map_find_first_value(advice_map,
                                                            AUTH_REALM_KEY);
                        if (NULL != auth_advice_value) {
                            auth_advice_url_prefix = AUTH_REALM_URL_PREFIX;
                            auth_advice_url_prefix_len = AUTH_REALM_URL_PREFIX_LEN;
                            auth_advice_value_len = strlen(auth_advice_value);
                        } else {                            
                            auth_advice_value = am_map_find_first_value(
                                              advice_map, AUTH_SERVICE_KEY);
                            if (NULL != auth_advice_value) {
                                auth_advice_value_len = strlen(auth_advice_value);
                                // This advice exists only starting with AM 7.1.
                                // Advice prefix is not needed with AM 7.1
                                auth_advice_url_prefix_len = 0;
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
            // redirect the browser to the login page. 
            if ((AM_INVALID_SESSION == status) ||
                  (auth_advice_value_len > 0) ||
                  (strcmp(session_adv_value, AM_WEB_ACTION_DENIED) == 0)) {
                try {
                    url_info_t *url_info_ptr;
                    std::string encoded_url;
                    std::string retVal;

                    url_info_ptr = find_active_login_server(&agent_info);
                    if (NULL == url_info_ptr) {
                        am_web_log_warning("%s: unable to find active Access "
                              "Manager Auth server.", thisfunc);
                        ret = AM_FAILURE;
                    } else {
                        retVal.append(url_info_ptr->url, url_info_ptr->url_len);
                        // In CDSSO mode, to have the user redirected to a 
                        // static page that in turn will redirect to the 
                        // resource, the goto parameter with the static page
                        // as value must be added to the cdcservlet url and 
                        // the url_redirect_param set to a value other than
                        // "goto". In such a case the "?" character must be 
                        // added before url_redirect_param
                        string temp_url = url_info_ptr->url;
                        string temp_redirect_param = 
                                      agent_info.url_redirect_param;
                        if ((agent_info.cdsso_enabled == AM_TRUE) && 
                             (temp_url.find("goto=") != string::npos) && 
                             (temp_redirect_param.compare("goto") != 0)) {
                            retVal.append("?");
                        } else {
                            retVal.append((url_info_ptr->has_parameters) ?"&":"?");
                        }
                        retVal.append(agent_info.url_redirect_param);
                        retVal.append("=");
                        if((agent_info.cdsso_enabled == AM_TRUE) &&
                                  (agent_info.useSunwMethod == AM_TRUE) &&
                                  (method != NULL)) {
                            temp_url = goto_url;
                            temp_url.append(
                                (temp_url.find("?") == string::npos)?"?":"&");
                            temp_url.append(REQUEST_METHOD_TYPE);
                            temp_url.append("=");
                            temp_url.append(method);
                            encoded_url =
                            PRIVATE_NAMESPACE_NAME::Http::encode(temp_url);
                        } else {
                            encoded_url =
                                PRIVATE_NAMESPACE_NAME::Http::encode(goto_url);
                        }
                        retVal.append(encoded_url);
                        char *am_rev_number = am_web_get_am_revision_number();
                        if ((am_rev_number != NULL) && 
                            (!strcmp(am_rev_number,am_63_revision_number)) &&
                            (auth_advice_url_prefix_len > 0)) {
                            retVal.append(auth_advice_url_prefix);
                            retVal.append(auth_advice_value);
                        }

                        if(agent_info.cdsso_enabled == AM_TRUE) {
                            am_web_log_debug("%s: The goto_url and url before "
                                   "appending cdsso elements: "
                                   "[%s] [%s]", thisfunc,
                            goto_url, retVal.c_str());
                            retVal.append("&");
                            retVal.append(add_cdsso_elements_to_redirect_url());
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
            // redirect user to the access denied url if it was configured.
            } else if (agent_info.access_denied_url != NULL) {
                char codeStr[10];
                std::string redirStr = agent_info.access_denied_url;
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
                    redirStr.append(agent_info.url_redirect_param);
                    redirStr.append("=");
                    redirStr.append(encoded_url);
                }
                *redirect_url = strdup(redirStr.c_str());
                if (*redirect_url == NULL) {
                    ret = AM_NO_MEMORY;
                } else {
                    ret = AM_SUCCESS;
                }
            // If no access denied url configured,
            // return null for redirect url 
            } else {
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
am_web_reset_cookies_list(const cookie_info_list_t *cookies_list,
                          am_status_t (*setFunc)(const char *, void **),
                          void **args)
{
    const char *thisfunc = "am_web_reset_cookies_list()";
    am_status_t status = AM_SUCCESS;
    am_status_t tmpStatus = AM_SUCCESS;

    cookie_info_t *cookies = cookies_list->list;
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
		    if (AM_SUCCESS != tmpStatus)
			status = tmpStatus;
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
     am_status_t (*setFunc)(const char *, void **), void **args)
{
    const char *thisfunc = "am_web_reset_ldap_attribute_cookies()";
    am_status_t status = AM_SUCCESS;
    am_status_t tmpStatus = AM_SUCCESS;

    // Reset cookies from ldap Attributes
    if ((setUserProfileAttrsMode == SET_ATTRS_AS_COOKIE) ||
        (setUserSessionAttrsMode == SET_ATTRS_AS_COOKIE)  ||
        (setUserResponseAttrsMode == SET_ATTRS_AS_COOKIE) &&
        (attrList.size() > 0))
    {
        cookie_info_t attr_cookie;
        attr_cookie.value = const_cast<char*>("");
        attr_cookie.domain = NULL;
        // This must be null to work with older browsers
        // netscape 4.79, IE 5.5, mozilla < 1.4.
        attr_cookie.max_age = const_cast<char*>("0"); // delete the cookie.
        attr_cookie.path = const_cast<char*>("/");
        try {
            std::list<std::string>::const_iterator iter;
            for(iter = attrList.begin(); iter != attrList.end(); iter++) {
                std::string attr = (*iter);
                std::string cookie_name(attrCookiePrefix);
                cookie_name.append(const_cast<char*>(attr.c_str()));
                attr_cookie.name = const_cast<char*>(cookie_name.c_str());
                char *cookie_header =
                const_cast<char*>(buildSetCookieHeader(&attr_cookie));
                if (cookie_header != NULL) {
                    am_web_log_debug("%s: Set cookie %s", 
                                     thisfunc, cookie_header);
                    tmpStatus =  setFunc(cookie_header, args);
                    if (AM_SUCCESS != tmpStatus) {
                        status = tmpStatus;
                    }
                    free(cookie_header);
                }
            }
        } catch (std::bad_alloc& exb) {
            status = AM_NO_MEMORY;
        } catch (std::exception& exs) {
            am_web_log_error("%s: Exception encountered: %s.",
                                thisfunc, exs.what());
            status = AM_FAILURE;
        } catch (...) {
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
			void **args)
{
    am_status_t status = AM_SUCCESS;
    am_status_t tmpStatus = AM_SUCCESS;
    
    // Reset cookies from properties file.
    if (agent_info.cookie_reset_enabled == AM_TRUE) {
        tmpStatus  = am_web_reset_cookies_list(&agent_info.cookie_list,
                     setFunc, args);
        if (AM_SUCCESS != tmpStatus) {
            status = tmpStatus;
        }
    }

    tmpStatus = am_web_reset_ldap_attribute_cookies(setFunc, args);
    if (AM_SUCCESS != tmpStatus) {
        status = tmpStatus;
    }
    
    // Reset iPlanetDirectoryPro cookie if not using SUNWmethod in CDSSO mode
    if ((am_web_is_cdsso_enabled() == B_TRUE) && 
            (agent_info.useSunwMethod == AM_FALSE)) {
        status = am_web_do_cookie_domain_set(setFunc, args, NULL);
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
                            void **args)
{
    const char *thisfunc = "am_web_logout_cookies_reset()";
    am_status_t status = AM_SUCCESS;
    if (NULL != agent_info.logout_cookie_reset_list.list &&
        agent_info.logout_cookie_reset_list.size >= 0) {
	am_web_log_debug("%s: Resetting logout cookies upon logout.",
			thisfunc);
        status = am_web_reset_cookies_list(
                     &agent_info.logout_cookie_reset_list,
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
                       const char *client_ip)
{
    const char *thisfunc = "am_web_is_url_enforced()";
    am_status_t status = AM_SUCCESS;
    am_bool_t isNotEnforced = AM_FALSE;
    boolean_t isEnforced = B_TRUE;
    const char *url = NULL;
    std::string normalizedURL;
    std::string pInfo;
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
                                normalizedURL, pInfo);
    if (status == AM_SUCCESS) {
        url = normalizedURL.c_str();
        if (url == NULL || *url == '\0') {
            am_web_log_warning("%s: Normalized url is null (original url=%s)",
                                thisfunc, url_str);
        } else {
            // Check if the url is enforced
            isNotEnforced = is_url_not_enforced(url, client_ip, 
                            originalPathInfo, originalQuery);
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

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_cdsso_enabled() {
	boolean_t status = B_FALSE;
	if(agent_info.cdsso_enabled == AM_TRUE) {
	    status = B_TRUE;
	}
	return status;
}

/*
 * This function sets the iPlanetDirectoryPro cookie for each domain
 * configured in the com.sun.am.policy.agents.cookieDomainList property.
 * It builds the set-cookie header for each domain specified in the
 * property, and calls the callback function 'setFunc' in the first
 * argument to actually set the cookie.
 * This function is called am_web_check_cookie_in_post() which is
 * called in CDSSO mode to set the iPlanetDirectoryPro cookie in the 
 * cdsso response.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_do_cookie_domain_set(am_status_t (*setFunc)(const char *, void **),
                void **args, const char *cookie_val)
{
    const char *thisfunc = "am_web_do_cookie_domain_set()";
    am_status_t status = AM_SUCCESS;
    am_status_t setStatus = AM_SUCCESS;

    cookie_info_t cookieInfo;
    cookieInfo.name = const_cast<char*>(agent_info.cookie_name);
    cookieInfo.value = const_cast<char*>(cookie_val);
    cookieInfo.domain = NULL;
    // This must be null to work with older browsers
    // netscape 4.79, IE 5.5, mozilla < 1.4.
    cookieInfo.max_age = NULL;
    cookieInfo.path = const_cast<char*>("/");
    cookieInfo.isSecure = agent_info.is_cookie_secure;

    try {
	std::set<std::string> *cookie_domains = agent_info.cookie_domain_list;
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
			      void **args)
{
	return am_web_result_attr_map_set(result,
					  setFunc,
					  NULL,
					  NULL,
					  NULL,
					  args);
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
    void **args)
{
    const char *thisfunc = "am_web_result_attr_map_set()";
    am_status_t retVal = AM_SUCCESS;

    am_map_t attrMap = NULL;
    const char* mode  = "NONE";

    if(result == NULL || setFunc == NULL) {
       retVal = AM_INVALID_ARGUMENT;
    } else if ((result->attr_profile_map == AM_MAP_NULL) &&
             (result->attr_session_map == AM_MAP_NULL) &&
             (result->attr_response_map == AM_MAP_NULL)) {
        am_web_log_info("%s: No profile or session or response"
                        " attributes to be set as headers or cookies",
                       thisfunc);
    } else {
        for (int i=0; i<3; i++) {
            switch (i) {
                case 0:
                    attrMap = result->attr_profile_map;
                    mode = profileMode;
                    break;
                case 1:
                    attrMap = result->attr_session_map;
                    mode = sessionMode;
                    break;
                case 2:
                    attrMap = result->attr_response_map;
                    mode = responseMode;
                    break;
                default:
                    break;
            }

            try {
                // set attributes as headers
                if (!strcasecmp(mode, AM_POLICY_SET_ATTRS_AS_HEADER)) {
                    const KeyValueMap &headerAttrs =
                          *(reinterpret_cast<const KeyValueMap *>
                           (attrMap));
                    am_web_log_max_debug("%s: Now setting %u "
                                         "header attributes.", 
                                         thisfunc, headerAttrs.size());
                    KeyValueMap::const_iterator iter_header = 
                                         headerAttrs.begin();
                    for(;(iter_header != headerAttrs.end()) &&
                                (retVal == AM_SUCCESS); iter_header++) {
                        std::string values;
                        am_status_t set_sts = AM_SUCCESS;
                        const KeyValueMap::key_type &keyRef = 
                                              iter_header->first;
                        char str[2048];
                        char * new_str = NULL;
                        unsigned int new_str_size = 0;
                        unsigned int new_str_free = 0;
                        Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
                                 "%s: User attribute is %s.",
                                 thisfunc, keyRef.c_str());
                        // Clear the header
                        std::string hdr_or_cookie_name_s(keyRef.c_str());
                        const char * hdr_or_cookie_name =
                                             hdr_or_cookie_name_s.c_str();
                        /*if (setFunc != NULL) {
                            set_sts = setFunc(hdr_or_cookie_name, NULL, args);
                            if (set_sts != AM_SUCCESS) {
                                am_web_log_warning("%s: Error %s "
                                            "clearing header %s",
                                            thisfunc,
                                            am_status_to_string(set_sts),
                                            hdr_or_cookie_name);
                            }
                        }*/
                        const KeyValueMap::mapped_type &valueRef =
                                                      iter_header->second;
                        am_web_log_max_debug("%s: Iterating over %u values.",
                                        thisfunc, valueRef.size());
                        for(std::size_t i = 0; i < valueRef.size(); ++i) {
                            values.append(valueRef[i]);
                            PUSH_BACK_CHAR(values, ',');
                        }
                        //Allow empty values to be send so agent can check if 
                        //there is no malicious header
                        if(values.size() >= 0) {
                            std::size_t t = values.rfind(',');
                            values.erase(t);
                            Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
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
                            if (agent_info.convert_mbyte == AM_TRUE ) {
                                Log::log(agent_info.log_module,
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
                                Log::log(agent_info.log_module,
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
                        Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
                             "%s: User attribute is %s.", thisfunc,
                             keyRef.c_str());
                        std::string hdr_or_cookie_name_s(keyRef.c_str());
                        const char * hdr_or_cookie_name =
                                 hdr_or_cookie_name_s.c_str();

                        // Clear that cookie
                        char *cookie_hdr = NULL;
                        char *cookie_header =  NULL;
                        cookie_info_t clear_cookie;
                        std::string c_name(attrCookiePrefix);
                        cookie_info_t attr_cookie;
                        std::string cookie_name(attrCookiePrefix);
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
                        for(std::size_t i = 0; i < valueRef.size(); ++i) {
                            values.append(valueRef[i]);
                            PUSH_BACK_CHAR(values, ',');
                        }

                        /* we say > 1 below becoz, the last extra ',' is at least
                        * one char.  so we need more that that to set header.
                        */
                        if(values.size() > 1) {
                            std::size_t t = values.rfind(',');
                            values.erase(t);
                            Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
                               "%s: Calling container-specific cookie setter "
                               "function. %d", thisfunc);

                            cookie_name.append(const_cast<char*>(keyRef.c_str()));
                            attr_cookie.name =
                            const_cast<char*>(cookie_name.c_str());
                            attr_cookie.value = const_cast<char*>(values.c_str());
                            attr_cookie.domain = NULL;
                            attr_cookie.max_age =
                            const_cast<char*>(attrCookieMaxAge);
                            attr_cookie.path = const_cast<char*>("/");
                            cookie_header =
                                (char*)(buildSetCookieHeader(&attr_cookie));
                            if (cookie_header != NULL) {
                                if (set_cookie_in_response != NULL) {
                                    retVal =  set_cookie_in_response(cookie_header,
                                                              args);
                                } else {
                                    Log::log(agent_info.log_module, Log::LOG_INFO,
                                      "%s: response header setting function "
                                      "is NULL", thisfunc);
                                }
                                if (set_header_attr_as_cookie != NULL) {
                                    retVal = set_header_attr_as_cookie(cookie_header,
                                                                args);
                                } else {
                                    Log::log(agent_info.log_module, Log::LOG_INFO,
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
        }

        if (retVal == AM_SUCCESS) {
            Log::log(agent_info.log_module, Log::LOG_DEBUG,
                    "%s: Successfully set all attributes.", thisfunc);
        } else {
            Log::log(agent_info.log_module, Log::LOG_ERROR,
                    "%s: Error while setting attributes: %s",
                    thisfunc, am_status_to_string(retVal));
        }
    }
    
    return retVal;
}

extern "C" AM_WEB_EXPORT const char *
am_web_get_user_id_param()
{
    return agent_info.user_id_param;
}

extern "C" AM_WEB_EXPORT const char *
am_web_get_cookie_name()
{
    return agent_info.cookie_name;
}

extern "C" AM_WEB_EXPORT size_t
am_web_get_cookie_name_len()
{
    return agent_info.cookie_name_len;
}

extern "C" AM_WEB_EXPORT const char *
am_web_get_notification_url()
{
    return agent_info.notification_url;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_in_not_enforced_list(const char *url, const char *path_info) {
    boolean_t retVal = B_FALSE;
    try {
	std::string pInfo;
	if(path_info != NULL) pInfo = path_info;

	URL urlObj(url, pInfo);
	if(in_not_enforced_list(urlObj) == AM_TRUE)
	    retVal = B_TRUE;
    } catch(InternalException &iex) {
	Log::log(agent_info.log_module, Log::LOG_ERROR, iex);
    } catch(std::exception &ex) {
	Log::log(agent_info.log_module, Log::LOG_ERROR, ex);
    } catch(...) {
	Log::log(agent_info.log_module, Log::LOG_ERROR,
		 "am_web_is_in_not_enforced_list(): "
		 "Unknown exception encountered.");
    }
    return retVal;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_in_not_enforced_ip_list(const char *ip) {
    boolean_t retVal = B_FALSE;
    if(ip != NULL && agent_info.not_enforce_IPAddr != NULL &&
       agent_info.not_enforce_IPAddr->size() > 0) {
	retVal = ((in_not_enforced_ip_list(ip))==AM_TRUE)?B_TRUE:B_FALSE;
	}
    return retVal;
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_valid_fqdn_url(const char *url)
{
    boolean_t ret = B_FALSE;
    try {
	ret = (AM_TRUE==is_valid_fqdn_access(url)) ? B_TRUE : B_FALSE;
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
    return am_log_is_level_enabled(agent_info.log_module, AM_LOG_DEBUG);
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_max_debug_on()
{
    return am_log_is_level_enabled(agent_info.log_module,
				      AM_LOG_MAX_DEBUG);
}

extern "C" AM_WEB_EXPORT void
am_web_log_always(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(agent_info.log_module, AM_LOG_ALWAYS, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT boolean_t
am_web_log_auth(am_web_access_t accessType, const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    switch(accessType) {
    case AM_ACCESS_DENY:
        if (strcasecmp(agent_info.authLogType_param, "LOG_DENY") == 0 ||
              strcasecmp(agent_info.authLogType_param, "LOG_BOTH") == 0) {
            am_log_vlog(agent_info.remote_LogID, AM_LOG_AUTH_REMOTE, fmt, args);
        }
        break;
    case AM_ACCESS_ALLOW:
        if (strcasecmp(agent_info.authLogType_param, "LOG_ALLOW") == 0 ||
              strcasecmp(agent_info.authLogType_param, "LOG_BOTH") == 0) {
            am_log_vlog(agent_info.remote_LogID, AM_LOG_AUTH_REMOTE, fmt, args);
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
    am_log_vlog(agent_info.log_module, AM_LOG_ERROR, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT void
am_web_log_warning(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(agent_info.log_module, AM_LOG_WARNING, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT void
am_web_log_info(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(agent_info.log_module, AM_LOG_INFO, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT void
am_web_log_debug(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(agent_info.log_module, AM_LOG_DEBUG, fmt, args);
    va_end(args);
}

extern "C" AM_WEB_EXPORT void
am_web_log_max_debug(const char *fmt, ...)
{
    va_list args;

    va_start(args, fmt);
    am_log_vlog(agent_info.log_module, AM_LOG_MAX_DEBUG, fmt, args);
    va_end(args);
}

/*
 * ----------------------- POST -----------------------------------
*/

/**
 *  Method to find if POST preservation is enabled or not
*/

extern "C" AM_WEB_EXPORT boolean_t
am_web_is_postpreserve_enabled() {

    return static_cast<bool>(
	    agent_info.postdatapreserve_enabled)==true?B_TRUE:B_FALSE;
}

/**
 *  Method to insert POST data in the POST hash table
*/

extern "C" AM_WEB_EXPORT boolean_t
am_web_postcache_insert(const char *key,
			const am_web_postcache_data_t *value)
{
    boolean_t ret = B_FALSE;
    PostCacheEntryRefCntPtr postEntry;
    if(agent_info.postcache_handle != NULL) {
	try {
	    postEntry = new PostCacheEntry(value->value, value->url);
	    ret = agent_info.postcache_handle->post_hash_insert(key,postEntry)?
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
		        am_web_postcache_data_t *postdata_entry)
{
    const char *thisfunc = "am_web_postcache_lookup()";
    PostCacheEntryRefCntPtr value;
    boolean_t ret = B_FALSE;

    if(agent_info.postcache_handle  != NULL) {
	try {
	    value =  agent_info.postcache_handle->post_hash_get(key);
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
am_web_postcache_remove(const char *key)
{
    const char *thisfunc = "am_web_postcache_remove()";
    try {
	 if(agent_info.postcache_handle != NULL) {
	     agent_info.postcache_handle->post_hash_remove(key);
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

static char *prtime_to_string(char *buffer, size_t buffer_len)
{
    PRTime timestamp;
    PRExplodedTime exploded_time;
    int n_written = 0;

    PR_Lock(agent_info.lock);
    timestamp = PR_Now();
    PR_Unlock(agent_info.lock);

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
                                 post_urls_t **url_data)
{
    const char *thisfunc = "am_web_create_post_preserve_urls()";
    am_status_t status = AM_SUCCESS;
    char *time_str = NULL;
    std::string key;
    std::string request_url_str;
    URL urlObject;
    post_urls_t *url_data_tmp = (post_urls_t *)malloc (sizeof(post_urls_t));

    if (request_url == NULL) {
        am_web_log_error("%s: request_url is NULL", thisfunc);
        status = AM_INVALID_ARGUMENT;
    }
    // Override the url if required
    if (status == AM_SUCCESS) {
        urlObject = URL(request_url);
        overrideProtoHostPort(urlObject);
        urlObject.getURLString(request_url_str);
    }
    // Get the time stamp
    if (status == AM_SUCCESS) {
        time_str = (char *) malloc (AM_WEB_MAX_POST_KEY_LENGTH);
        if (time_str != NULL) {
            prtime_to_string(time_str,AM_WEB_MAX_POST_KEY_LENGTH);
        } else {
            am_web_log_error("%s: Failed to allocate time_str.", thisfunc);
            status = AM_NO_MEMORY;
        }
    }
    // Build the key
    if (status == AM_SUCCESS) {
        std::string agentID, stickySessionValue;
        size_t equalPos = 0;
        char uniqueNumber[5];
        key.assign(time_str);
        // Add the agent id (if using a LB) to the key
        if ((agent_info.postdatapreserve_sticky_session_value != NULL) &&
              (strlen(agent_info.postdatapreserve_sticky_session_value) > 0))
        {
            stickySessionValue.assign
                       (agent_info.postdatapreserve_sticky_session_value);
            equalPos=stickySessionValue.find('=');
            if (equalPos != std::string::npos) {
                agentID = stickySessionValue.substr(equalPos+1);
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
        char *stickySessionValue = NULL;
        urlObject.getRootURL(dummyURL);
        dummyURL.append(DUMMY_REDIRECT).append(MAGIC_STR).append(key);
        // Add the sticky session parameter if a LB is used with sticky
        // session mode set to URL.
        if (am_web_get_postdata_preserve_URL_parameter(&stickySessionValue)
             == AM_SUCCESS)
        {
            dummyURL.append("?").append(stickySessionValue);
        }
        url_data_tmp->dummy_url = (char *)strdup(dummyURL.c_str());
        if (url_data_tmp->dummy_url == NULL) {
            am_web_log_error("%s: Failed to allocate url_data_tmp->dummy_url.",
                             thisfunc);
            status = AM_NO_MEMORY;
        }
        if (stickySessionValue != NULL) {
            free(stickySessionValue);
            stickySessionValue = NULL;
        }
    }
    // Set the action URL
    if (status == AM_SUCCESS) {
        url_data_tmp->action_url = (char *)strdup(request_url_str.c_str());
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
post_struct_t *
split_post_data(const char * test_string)
{
    const char *thisfunc = "split_post_data()";
    char * str = NULL;
    char * ptr = NULL;
    std::size_t i = 0;
    unsigned int num_sectors = 0;
    post_struct_t *post_data = 
             (post_struct_t *) malloc(sizeof(post_struct_t) * 1);

    //Create the tokens with name value pair separated with "&"
    char *postValue = strdup(test_string);
    ptr = strchr(postValue, '&');
    num_sectors = 1;
    while (ptr != NULL) {
        num_sectors += 1;
        ptr = strchr(ptr + 1, '&');
    }
    if(post_data != NULL){
        post_data->namevalue = (name_value_pair_t *)
                malloc (sizeof(name_value_pair_t) * num_sectors);
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
                post_data->namevalue[i].name = am_web_http_decode(
                                  post_data->namevalue[i].name,
                                  strlen(post_data->namevalue[i].name));
                post_data->namevalue[i].value= am_web_http_decode(
                                  post_data->namevalue[i].value,
                                  strlen(post_data->namevalue[i].value));
                escapeQuotationMark(post_data->namevalue[i].name);
                escapeQuotationMark(post_data->namevalue[i].value);
            }
            post_data->namevalue[i].name = ptr;
            post_data->namevalue[i].value = 
                    strchr(post_data->namevalue[i].name, '=');
            *(post_data->namevalue[i].value++) = '\0';
            post_data->namevalue[i].name = am_web_http_decode(
                               post_data->namevalue[i].name,
                               strlen(post_data->namevalue[i].name));
            post_data->namevalue[i].value= am_web_http_decode(
                               post_data->namevalue[i].value,strlen(
                               post_data->namevalue[i].value));
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
am_web_create_post_page(const char *key, const char *postdata, const char *actionurl)
{
    const char *thisfunc = "am_web_create_post_page()";
    char *buffer_page = NULL;
    int num_sectors = 0;
    int i =0;
#if defined(_AMD64_)
    size_t totalchars = 0;
#else
    int totalchars = 0;
#endif
    post_struct_t *post_data = split_post_data(postdata);
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
    // Remove the entry from the hashtable
    if(key != NULL){
        am_web_postcache_remove(key);
    }
    am_web_log_debug("%s: HTML page for post:\n%s", thisfunc, buffer_page);
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
am_web_is_logout_url(const char *url)
{
    const char *thisfunc = "am_web_is_logout_url";
    boolean_t found = B_FALSE;
    if (NULL != url && '\0' != url[0]) {
	try {
	    // normalize the given url before comparison.
	    URL url_obj(url);
	    // override protocol/host/port if configured.
	    (void)overrideProtoHostPort(url_obj);
	    std::string url_str;
	    url_obj.getURLString(url_str);
	    const char *norm_url = url_str.c_str();
	    Log::log(agent_info.log_module, Log::LOG_DEBUG,
		     "%s(%s): normalized URL %s.\n", thisfunc, url, norm_url);
	    unsigned int i;
	    for (i = 0;
		(i < agent_info.logout_url_list.size) && (B_FALSE == found);
		 i++) {
		am_resource_traits_t rsrcTraits;
		populate_am_resource_traits(rsrcTraits);
		am_resource_match_t match_status;
		boolean_t usePatterns =
		    agent_info.logout_url_list.list[i].has_patterns==AM_TRUE? B_TRUE:B_FALSE;
		match_status = am_policy_compare_urls(
		    &rsrcTraits, agent_info.logout_url_list.list[i].url, norm_url, usePatterns);
		if (match_status == AM_EXACT_MATCH ||
		    match_status == AM_EXACT_PATTERN_MATCH) {
		    Log::log(agent_info.log_module, Log::LOG_DEBUG,
			"%s(%s): matched '%s' entry in logout url list",
			thisfunc, url, agent_info.logout_url_list.list[i].url);
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
    bool found = false;
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
	am_web_handle_notification(postdata, strlen(postdata));
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
        char **sso_token)
{
    const char *thisfunc = "get_token_from_assertion()";
    am_status_t sts = AM_SUCCESS;
    const char *cookieName = am_web_get_cookie_name();
    char *postdata = NULL;

    // check method arg
    if (method != AM_WEB_REQUEST_GET &&
           method != AM_WEB_REQUEST_POST) {
        am_web_log_error("%s: invalid method %s.", thisfunc,
                   am_web_method_num_to_str(method));
        sts = AM_FEATURE_UNSUPPORTED;
    // Get cookie from GET
    } else if (method == AM_WEB_REQUEST_GET) {
        if (url == NULL || url[0] == '\0') {
            am_web_log_error("%s: Error getting cookie from assertion: "
                        "URL is null or empty.", thisfunc);
            sts = AM_NOT_FOUND;
        } else {
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
    // Get cookie from POST
    } else {  // POST
        if (get_post_data.func == NULL) {
            am_web_log_error("%s: request is a post but get_post_data is NULL",
                                thisfunc);
                sts = AM_INVALID_ARGUMENT;
        } else if ((sts = get_post_data.func(get_post_data.args,
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
        } else if ((sts = am_web_get_token_from_assertion(
                   postdata, sso_token)) != AM_SUCCESS ||
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
        } else {
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
set_cookie_in_request(cookie_info_t *cookie_info,
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
	char *buf = NULL;
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
set_cookie_in_request_and_response(cookie_info_t *cookie_info,
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
		      am_web_request_func_t *req_func)
{
    const char *thisfunc = "set_cookie_in_domains()";
    am_status_t sts = AM_SUCCESS;
    am_status_t setSts = AM_SUCCESS;
    cookie_info_t cookieInfo;

    if (sso_token == NULL || sso_token[0] == '\0' || req_func == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	std::set<std::string> *cookie_domains = agent_info.cookie_domain_list;
	cookieInfo.name = (char *)agent_info.cookie_name;
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
			     "returned %s", thisfunc, agent_info.cookie_name,
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
				 agent_info.cookie_name,
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

static am_status_t
get_original_method(am_web_request_params_t *req_params,
            am_web_req_method_t *orig_method)
{
    const char *thisfunc = "get_original_method()";
    am_status_t status = AM_NOT_FOUND;
    char *orig_method_str = NULL;

    *orig_method = AM_WEB_REQUEST_UNKNOWN;

    status = am_web_get_parameter_value(req_params->url,
            REQUEST_METHOD_TYPE, &orig_method_str);
    if (status == AM_SUCCESS) {
        if (orig_method_str == NULL || orig_method_str[0] == '\0') {
            status = AM_NOT_FOUND;
        } else {
            am_web_log_debug("%s: Got original method %s from "
                             "query parameter %s.",
                             thisfunc, orig_method_str,
                             REQUEST_METHOD_TYPE);
            *orig_method = am_web_method_str_to_num(orig_method_str);
            if (*orig_method == AM_WEB_REQUEST_UNKNOWN) {
                am_web_log_warning("%s: Unrecognized original method "
                      "%s received.", thisfunc, orig_method_str);
            }
        }
    }
    if (orig_method_str != NULL) {
        free(orig_method_str);
        orig_method_str = NULL;
    }
    return status;
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
        char **sso_token)
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
    // Get sso token from assertion.
    } else if ((sts = get_token_from_assertion(req_params->url,
                                        req_params->method,
                                        req_func->get_post_data,
                                        req_func->free_post_data,
                                        sso_token)) != AM_SUCCESS) {
        am_web_log_error("%s: Error getting token from assertion: %s",
                        thisfunc, am_status_to_string(sts));
    } else {
        // Set cookie in domain
        local_sts = set_cookie_in_domains(*sso_token, req_params, req_func);
        if (local_sts != AM_SUCCESS) {
            // Ignore error but give a warning
            am_web_log_warning("%s: cookie domain set of sso_token [%s] "
                                "failed with %s.",
                                thisfunc, sso_token,
                                am_status_to_string(local_sts));
        }
        // Set original method
        local_sts = req_func->set_method.func(
                req_func->set_method.args, orig_method);
        if (local_sts != AM_SUCCESS) {
            // Ignore error but give a warning.
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
			 am_web_request_func_t *req_func)
{
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
     else if ((SET_ATTRS_NONE == setUserProfileAttrsMode) &&
              (SET_ATTRS_NONE == setUserSessionAttrsMode) &&
              (SET_ATTRS_NONE == setUserResponseAttrsMode)) {
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
    else if (((SET_ATTRS_AS_HEADER == setUserProfileAttrsMode) ||
    		 (SET_ATTRS_AS_HEADER == setUserSessionAttrsMode) ||
    		 (SET_ATTRS_AS_HEADER == setUserResponseAttrsMode)) &&
                   NULL == req_func->set_header_in_request.func) {
              am_web_log_warning("%s: set user attributes option is "
                                 "HEADER but no set request header "
                                 "function is provided. ", thisfunc);
              sts = AM_NOT_FOUND;
    }
    // if set user LDAP attribute cookies option is "cookie" and
    // functions are not provided to set cookie, log a warning.
    else if (((SET_ATTRS_AS_COOKIE == setUserProfileAttrsMode) ||
              (SET_ATTRS_AS_COOKIE == setUserSessionAttrsMode) ||
              (SET_ATTRS_AS_COOKIE == setUserResponseAttrsMode)) &&
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
            std::list<std::string>::const_iterator attr_end=attrList.end();
            for(attr_iter = attrList.begin(); attr_iter != attr_end;
                                              attr_iter++) {
               const char * header_name = (*attr_iter).c_str();
               if ((SET_ATTRS_AS_HEADER == setUserProfileAttrsMode) ||
                   (SET_ATTRS_AS_HEADER == setUserSessionAttrsMode) ||
                   (SET_ATTRS_AS_HEADER == setUserResponseAttrsMode)) {
                        set_sts = req_func->set_header_in_request.func(
                                         req_func->set_header_in_request.args,
                                         header_name, NULL);
                        if (set_sts != AM_SUCCESS) {
                           am_web_log_warning("%s: Error %s encountered while "
                                              "clearing header name %s.",
                                              thisfunc, header_name);
                        }
                }
                else if ((SET_ATTRS_AS_COOKIE == setUserProfileAttrsMode) ||
                         (SET_ATTRS_AS_COOKIE == setUserSessionAttrsMode) ||
                         (SET_ATTRS_AS_COOKIE == setUserResponseAttrsMode)) {
                     // for cookie, remove all cookies of the same name
                     // in the cookie header.
                     std::string cookie_name(attrCookiePrefix);
                     cookie_name.append(*attr_iter);
                     cookie_info_t cookie_info;
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

        for (int i=0; i<3; i++) {
	      switch (i) {
		  case 0:
		       attrMap = result->attr_profile_map;
		       mode = profileMode;
		       break;
		  case 1:
		       attrMap = result->attr_session_map;
		       mode = sessionMode;
		       break;
		  case 2:
		       attrMap = result->attr_response_map;
		       mode = responseMode;
		       break;
                  default:
		       break;
             }
	     if (!strcasecmp(mode, AM_POLICY_SET_ATTRS_AS_HEADER)) { 
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
                  Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
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
            if (!strcasecmp(mode, AM_POLICY_SET_ATTRS_AS_COOKIE)) { 
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
                   Log::log(agent_info.log_module, Log::LOG_MAX_DEBUG,
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

                   cookie_info_t cookie_info;
                   std::string cookie_name(attrCookiePrefix);
                   cookie_name.append(key);
                   cookie_info.name = (char *)cookie_name.c_str();

		   std::string encoded_values;
                   if (agent_info.encode_cookie_special_chars == AM_TRUE ) {
	   		encoded_values = Http::cookie_encode(values);		
			cookie_info.value = (char*)encoded_values.c_str();
		   }else {
                   	   cookie_info.value = (char*)values.c_str();
		   	 }

                   cookie_info.domain = NULL;
                   cookie_info.max_age = (char *)attrCookieMaxAge;
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
             // now set the cookie header.
             set_sts = req_func->set_header_in_request.func(
                            req_func->set_header_in_request.args,
                            "Cookie", (char *)req_params->reserved);
             am_web_log_debug("%s:set cookie header %s in request returned %s",
                             thisfunc, req_params->reserved,
                             am_status_to_string(set_sts));
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
               am_web_request_func_t *req_func)
{
    const char *thisfunc = "process_access_success()";
    am_web_result_t result = AM_WEB_RESULT_OK;
    am_status_t sts = AM_SUCCESS;
    PRBool setting_user = AM_TRUE;
    void *args[1];

    // set user - if fail, access is forbidden.
    // If remote_user is null there are two possibilities:
    //   1) if ignore_policy_evaluation_if_notenforced is set
    //      to false (default) it is up to the agent/container
    //      to decide whether or not to allow null user.
    //   2) if ignore_policy_evaluation_if_notenforced is set
    //      to true, the remote user is not set to anything
    if ((agent_info.ignore_policy_evaluation_if_notenforced) &&
            (policy_result.remote_user == NULL)) {
       am_web_log_debug("%s: ignore_policy_evaluation_if_notenforced is "
                        "set to true and remote user is NULL: "
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
    } else {
        // If logout url, reset any logout cookies
        if (am_web_is_logout_url(url)) {
            args[0] = req_func;
            sts = am_web_logout_cookies_reset(add_cookie_in_response, args);
            if (sts != AM_SUCCESS) {
                am_web_log_warning("%s: Resetting logout cookies after [%s], "
                                "returned %s.", thisfunc, url,
                                am_status_to_string(sts));
            }
        }
        // Set any profile,session or response attributes in
        // the header or cookie.
        sts = set_user_attributes(&policy_result, req_params, req_func);
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
            char **advice_response)
{
    const char *thisfunc = "process_access_redirect()";
    am_status_t sts = AM_SUCCESS;
    am_web_result_t result = AM_WEB_RESULT_REDIRECT;

    // now get the redirect url.
    if (result != AM_WEB_RESULT_ERROR) {
        sts = am_web_get_url_to_redirect(access_check_status,
                        policy_result.advice_map,
                        url,
                        am_web_method_num_to_str(method),
                        NULL,
                        redirect_url);
        am_web_log_debug("%s: get redirect url returned %s, redirect url [%s].",
                        thisfunc, am_status_to_name(sts),
                        *redirect_url == NULL ? "NULL" : *redirect_url);

        char *am_rev_number = am_web_get_am_revision_number();
        if ((am_rev_number != NULL) &&
                 (!strcmp(am_rev_number, am_70_revision_number)) &&
                 (policy_result.advice_string != NULL)) {
            
            if ((B_FALSE == am_web_use_redirect_for_advice()) &&
                       (advice_response != NULL)) {
                // Composite advice is sent as a POST
                char* advice_res = (char *) malloc(2048 * sizeof(char));
                if (advice_res) {
                    sts = am_web_build_advice_response(&policy_result,
                                  *redirect_url, &advice_res);
                    am_web_log_debug("%s: policy status=%s, "
                                 "advice response[%s]", thisfunc, am_status_to_string(sts),
                                 *advice_response);
                    if(sts != AM_SUCCESS) {
                        am_web_log_error("process_access_redirect(): Error while building "
                                     "advice response body:%s",
                                     am_status_to_string(sts));
                    } else {
                        result = AM_WEB_RESULT_OK_DONE;
                        *advice_response = advice_res;
                    }
                } else {
                    sts = AM_NO_MEMORY;
                }
            } else if (B_TRUE == am_web_use_redirect_for_advice()){
                // Composite advice is redirected
                am_web_log_debug("%s: policy status = %s, redirection URL is %s",
                              thisfunc, am_status_to_string(sts), *redirect_url);
                char *redirect_url_with_advice = NULL;
                sts = am_web_build_advice_redirect_url(&policy_result,
                                 *redirect_url, &redirect_url_with_advice);
                if(sts == AM_SUCCESS) {
                    *redirect_url = redirect_url_with_advice;
                    am_web_log_debug("%s: policy status=%s, "
                                "redirect url with advice [%s]",
                                thisfunc, am_status_to_string(sts),
                                *redirect_url);
                } else {
                    am_web_log_error("%s: Error while building "
                                "the redirect url with advice:%s",
                                thisfunc, am_status_to_string(sts));
                }
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
              am_web_req_method_t *orig_method)
{
    const char *thisfunc = "get_sso_token()";
    am_status_t sts = AM_NOT_FOUND;
    bool foundSunwMethod = false;
    bool foundSSOToken = false;
    *orig_method = AM_WEB_REQUEST_UNKNOWN;
    
    //Get original method request
    if ((agent_info.cdsso_enabled == AM_TRUE) &&
              (agent_info.useSunwMethod == AM_TRUE) &&
              (req_params->method == AM_WEB_REQUEST_POST)) {
        sts = get_original_method(req_params, orig_method);
        if (sts == AM_SUCCESS) {
            foundSunwMethod = true;
        }
    }
    //Check if the SSO token is in the cookie header
    sts = get_cookie_val(am_web_get_cookie_name(),
            req_params->cookie_header_val, sso_token);
    if ((sts == AM_SUCCESS) && 
           (*sso_token != NULL) && ((*sso_token)[0] != '\0')) {
        foundSSOToken = true;
        am_web_log_debug("%s: SSO token found in cookie header.", thisfunc);
    } else if (sts != AM_SUCCESS && sts != AM_NOT_FOUND) {
        am_web_log_error("%s: Error while getting sso token from "
                         "cookie header: %s",
                         thisfunc, am_status_to_string(sts));
    }
    //Check if the sso token is in the assertion
    if ((agent_info.cdsso_enabled == AM_TRUE) &&
                (req_params->method == AM_WEB_REQUEST_POST)) {
        if (((agent_info.useSunwMethod == AM_FALSE) &&
             (foundSSOToken == false) &&
             (am_web_is_url_enforced(req_params->url, req_params->path_info, req_params->client_ip) == B_TRUE)) ||
            ((agent_info.useSunwMethod == AM_TRUE) &&
             (foundSunwMethod == true))) {
            if (agent_info.useSunwMethod == AM_FALSE) {
                //When the sunwMethod parameter is not used,
                //the request method is set to GET
                *orig_method = AM_WEB_REQUEST_GET;
                am_web_log_debug("%s: Request method set to GET.", thisfunc);
            }
            sts = process_cdsso(req_params, req_func, *orig_method, sso_token);
            if (sts == AM_SUCCESS) {
                am_web_log_debug("%s: SSO token found in assertion.",
                                  thisfunc);
            } else if (agent_info.useSunwMethod == AM_FALSE) {
                am_web_log_debug("%s: SSO token not found in "
                                 "assertion. Redirecting to login page.",
                                   thisfunc);
                sts = AM_NOT_FOUND;
            } else if (sts != AM_NOT_FOUND) {
                am_web_log_error("%s: Error while getting sso token from "
                                 "assertion : %s",
                                 thisfunc, am_status_to_string(sts));
            }
        }
    }
    if ((sts == AM_SUCCESS) && 
       (*sso_token != NULL) && ((*sso_token)[0] != '\0')) {
        am_web_log_debug("%s: SSO token = %s",
                          thisfunc, *sso_token);
    } else if (sts == AM_NOT_FOUND) {
        //As there is no token the request will be redirected to the login page
        //This will be handled by am_web_is_access_allowed
        sts = AM_SUCCESS;
    }
    return sts;
}

/**
 * do actual access check processing and get web result with redirect url
 * all input arguments should already be checked to be OK (not null, etc.)
 *
 * render_data is a buffer to store any data needed for rendering
 * HTTP response, such as a redirect url or a notification message response
 * to the Access Manager. It will be null terminated, so it will not
 * fill the buffer beyond render_data_size-1 bytes.
 * If the buffer is not big enough, an error message will be logged and
 * an internal error result will be returned.
 */
static am_web_result_t
process_request(am_web_request_params_t *req_params,
                am_web_request_func_t *req_func,
                char *data_buf, size_t data_buf_size)
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
    boolean_t cdsso_enabled = am_web_is_cdsso_enabled();
    PRBool access_is_denied = AM_FALSE;

    // initialize reserved field to NULL
    req_params->reserved = NULL;

    // Get sso token
    sts = get_sso_token(req_params, req_func, &sso_token, &orig_method);

    if (sts == AM_SUCCESS) {
        // Create map
        sts = am_map_create(&env_map);
    }
    if (sts != AM_SUCCESS) {
        am_web_log_error("%s: Could not create map needed for "
                "checking access.", thisfunc);
        result = AM_WEB_RESULT_ERROR;
    } 
    if (sts == AM_SUCCESS) {
        // Set orig_method to the method in the request, if
        // it has not been set to the value of the original method
        // query parameter from CDC servlet.
        if (orig_method == AM_WEB_REQUEST_UNKNOWN) {
            orig_method = req_params->method;
        }
        am_web_set_host_ip_in_env_map(req_params->client_ip,
                                      req_params->client_hostname,
                                      env_map);
        // Check if access allowed
        sts = am_web_is_access_allowed(
                sso_token, req_params->url, req_params->path_info,
                am_web_method_num_to_str(orig_method),
                req_params->client_ip, env_map, &policy_result);
        am_web_log_info("%s: Access check for URL %s returned %s.",
            thisfunc, req_params->url, am_status_to_string(sts));

        // Map access check result to web result 
        switch(sts) {
            case AM_SUCCESS: 
                result = process_access_success(req_params->url,
                           policy_result, req_params, req_func);
                break;
            case AM_INVALID_SESSION:
                // Reset cookies on invalid session.
                args[0] = req_func;
                local_sts = am_web_do_cookies_reset(add_cookie_in_response,
                            args);
                if (local_sts != AM_SUCCESS) {
                    am_web_log_warning("%s: all_cookies_reset after "
                                  "access to url [%s] returned "
                                  "invalid session returned %s.",
                                  thisfunc, req_params->url,
                                  am_status_to_string(local_sts));
                }
                // Will be either forbidden or redirect to auth
                result = process_access_redirect(req_params->url,
                                                 orig_method,
                                                 sts, policy_result,
                                                 req_func,
                                                 &redirect_url,
                                                 NULL);
                break;
            case AM_ACCESS_DENIED:
                access_is_denied = AM_TRUE;
            case AM_INVALID_FQDN_ACCESS:
                result = process_access_redirect(req_params->url,
                                                 orig_method,
                                                 sts, policy_result,
                                                 req_func,
                                                 &redirect_url,
                                                 &advice_response);
                break;
            case AM_INVALID_ARGUMENT:
            case AM_NO_MEMORY:
            default:
                result = AM_WEB_RESULT_ERROR;
                break;
        }
    }

    // Redirect all errors to error_page_url,
    // if this property has been defined
    if ((AM_FALSE == access_is_denied) &&
             (agent_info.error_page_url != NULL && 
              strlen(agent_info.error_page_url) > 0) &&
             (AM_WEB_RESULT_FORBIDDEN == result || 
              AM_WEB_RESULT_ERROR == result)) {
        if (redirect_url !=NULL) {
            free(redirect_url);
        }
        redirect_url = strdup(agent_info.error_page_url);
        am_web_log_error("%s: Result was %s, redirecting to "
                         "errorpage.url=%s", thisfunc,
                          am_web_result_num_to_str(result), redirect_url);
        result = AM_WEB_RESULT_REDIRECT;
    }
    
    // Clean up
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

    // Copy redirect_url or advice response to the given buffer and 
    // free the pointer. If size of buffer not big enough, return error.
    char* ptr = redirect_url;
    if (result == AM_WEB_RESULT_OK_DONE) {
        ptr = advice_response;
    }
    if (ptr != NULL) {
        if (data_buf_size < strlen(ptr)+1) {
            am_web_log_error("%s: size of render data buffer too small for "
                             "pointer [%s]", thisfunc, ptr);
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

    am_web_log_debug("%s: returning web result %s, data [%s]",
                    thisfunc, am_web_result_num_to_str(result), data_buf);
    return result;
}


extern "C" AM_WEB_EXPORT am_web_result_t
am_web_process_request(am_web_request_params_t *req_params,
                       am_web_request_func_t *req_func,
                       am_status_t *render_sts)
{
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
     // Check request parameters
    } else if (req_params->url == NULL || req_params->url[0] == '\0') {
        am_web_log_error("%s: required request parameter url is %s.",
                        thisfunc, req_params->url==NULL?"NULL":"empty");
        result = AM_WEB_RESULT_ERROR;
    // Check request processing functions. The following are required and
    // the rest are optional.
    // a) in all cases:
    //    - set_user
    //    - render_result
    // b) in CDSSO mode - these will be checked in process_cdsso.
    //    - get_post_data (if method is post)
    //    - set_method (if method is post)
    //    - one of set_header_in_request or set_cookie_in_request to set cookie
    //
    } else if (req_func->set_user.func == NULL ||
             req_func->render_result.func == NULL) {
        am_web_log_error("%s: one of required request functions "
                    "set_user or render_result is null.",
                    thisfunc);
        result = AM_WEB_RESULT_ERROR;
    // Process notification if it's a notification url
    } else if (B_TRUE == am_web_is_notification(req_params->url)) {
        if (NULL == req_func->get_post_data.func) {
            am_web_log_warning("%s: Notification message received but "
                               "ignored because no get post data "
                               "function is found.", thisfunc);
            result = AM_WEB_RESULT_OK_DONE;
        } else {
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
    // Process access check.
    } else {
        result = process_request(req_params, req_func,
                            data_buf, sizeof(data_buf));
    }

    // Render web result
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
    if (render_sts != NULL) {
        *render_sts = sts;
    }

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

extern "C" AM_WEB_EXPORT am_status_t
am_web_build_advice_redirect_url(const am_policy_result_t *policy_result,
            const char *redirect_url, char **redirect_url_with_advice) {
    const char *thisfunc = "am_web_build_advice_redirect_url()";
    am_status_t retVal = AM_SUCCESS;
    
    if (policy_result != NULL && redirect_url != NULL && 
        redirect_url_with_advice != NULL) {
        std::string msg(redirect_url);
        msg.append("&");
        msg.append(COMPOSITE_ADVICE_KEY);
        msg.append("=");
        std::string encoded_msg = Http::encode(Http::encode(policy_result->advice_string));
        msg.append(encoded_msg);
        *redirect_url_with_advice = (char *)malloc(msg.size() + 1);
        if (*redirect_url_with_advice != NULL) {
            strcpy(*redirect_url_with_advice, msg.c_str());
        } else {
            am_web_log_error("%s: Not enough memory",thisfunc);
            retVal = AM_NO_MEMORY;
        }
    } else {
        am_web_log_error("%s: Invalid Parameters",thisfunc);
        retVal = AM_INVALID_ARGUMENT;
    }

    return retVal;
}

/*authtype
 * Method to determine if the auth-type value "dsame"
 * in the IIS6 agent should be replaced by "Basic"
 */
extern "C" AM_WEB_EXPORT const char *
am_web_get_authType()
{
	return agent_info.authtype;
}

/*
 * Method to determine if the composite advice should be
 * redirect rather than POST
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_use_redirect_for_advice() {
    return (AM_TRUE==agent_info.use_redirect_for_advice) ? B_TRUE : B_FALSE;
}

/*
 * Method to determine if the sunwMethod query parameter should be  
 * removed in CDSSO
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_remove_sunwmethod() {
    return (AM_TRUE==agent_info.remove_sunwmethod) ? B_TRUE : B_FALSE;
}

extern "C" AM_WEB_EXPORT char *
am_web_get_am_revision_number() {
    return agent_info.am_revision_number;
}

extern "C" AM_WEB_EXPORT const char *
am_web_get_accessdenied_url() {
    return agent_info.access_denied_url;
}

/*
 * Method to get the IIS6 agent replay passwd key
 *
 */
extern "C" AM_WEB_EXPORT const char *
am_web_get_iis6_replaypasswd_key() {
    return agent_info.iis6_replaypasswd_key;
}

/*
 * Method to check whether OWA is deployed on IIS6
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_owa_enabled() {
	boolean_t status = B_FALSE;
	if(agent_info.owa_enabled == AM_TRUE) {
	    status = B_TRUE;
	}
	return status;
}

/*
 * Method to convert http to https if OWA is deployed on IIS6
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_owa_enabled_change_protocol() {
	boolean_t status = B_FALSE;
	if(agent_info.owa_enabled_change_protocol == AM_TRUE) {
	    status = B_TRUE;
	}
	return status;
}

/*
 * Method to convert http to https if OWA is deployed on IIS6
 */
extern "C" AM_WEB_EXPORT const char *
am_web_is_owa_enabled_session_timeout_url() {
    return agent_info.owa_enabled_session_timeout_url;
}

/*
 * Method to check whether the sunwMethod parameter 
 * should be used in CDSSO mode
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_use_sunwmethod() {
    boolean_t status = B_FALSE;
    if(agent_info.useSunwMethod == AM_TRUE) {
        status = B_TRUE;
    }
    return status;
}

/*
 * Method to check whether an url is
 * the dummy post url
 */
extern "C" AM_WEB_EXPORT boolean_t
am_web_is_dummypost_url(const char *url, const char *path_info) {
    boolean_t status = B_FALSE;
    std::string baseURL_str;
    std::string dummyNotEnforcedUrl_str;
    am_resource_traits_t rsrcTraits;
    populate_am_resource_traits(rsrcTraits);
    
    // Get the request base url
    URL urlObj(url, path_info);
    urlObj.getBaseURL(baseURL_str);
    // Built the dummy url
    urlObj.getRootURL(dummyNotEnforcedUrl_str);
    dummyNotEnforcedUrl_str.append(DUMMY_NOTENFORCED);
    am_resource_match_t dummy_post_url_match;
    dummy_post_url_match = am_policy_compare_urls(&rsrcTraits,
                              dummyNotEnforcedUrl_str.c_str(),
                              baseURL_str.c_str(), B_TRUE);
    if ( (AM_EXACT_MATCH == dummy_post_url_match)  ||
             ( AM_EXACT_PATTERN_MATCH == dummy_post_url_match ))
    {
        status = B_TRUE;
    }
    
    return status;
}

/*
 * Method to get the set-cookie header for the lb cookie
 * when using a LB in front of the agent with post preservation
 * enabled and sticky session mode set to COOKIE.
 */
extern "C" AM_WEB_EXPORT am_status_t
am_web_get_postdata_preserve_lbcookie(char **headerValue, 
                                      boolean_t isValueNull)
{
    const char *thisfunc = "am_web_get_postdata_preserve_lbcookie()";
    am_status_t status = AM_SUCCESS;
    std::string header;
    std::string stickySessionValue;
    std::string cookieName, cookieValue;
    size_t equalPos = 0;

    // If stickySessionMode or stickySessionValue is empty, 
    // then there is no LB in front of the agent.
    if ((agent_info.postdatapreserve_sticky_session_mode == NULL) ||
        (strlen(agent_info.postdatapreserve_sticky_session_mode) == 0) ||
        (agent_info.postdatapreserve_sticky_session_value == NULL) ||
        (strlen(agent_info.postdatapreserve_sticky_session_value) == 0))
    {
        status = AM_INVALID_ARGUMENT;
    } else if (strcmp(agent_info.postdatapreserve_sticky_session_mode, "COOKIE") != 0) {
        // Deals only with the case where the sticky session mode is COOKIE.
        status = AM_INVALID_ARGUMENT;
        if (strcmp(agent_info.postdatapreserve_sticky_session_mode, "URL") != 0) {
            am_web_log_warning("%s: %s is not a correct value for the property "
                             "config.postdata.preserve.stickysession.value.",
                             thisfunc, 
                             agent_info.postdatapreserve_sticky_session_mode);
        }
    }
    // Check if the sticky session value has a correct format ("param=value")
    if (status  == AM_SUCCESS) {
        stickySessionValue.assign(agent_info.postdatapreserve_sticky_session_value);
        equalPos = stickySessionValue.find('=');
        if (equalPos != std::string::npos) {
            cookieName = stickySessionValue.substr(0, equalPos);
            cookieValue = stickySessionValue.substr(equalPos+1);
            if (cookieName.empty() || cookieValue.empty()) {
                am_web_log_warning("%s: The property "
                     "config.postdata.preserve.stickysession.value "
                     "(%s) does not a have correct format.",
                     thisfunc, stickySessionValue.c_str());
                status = AM_INVALID_ARGUMENT;
            }
        } else {
            am_web_log_warning("%s: The property "
                     "config.postdata.preserve.stickysession.value "
                     "(%s) does not have a correct format.",
                     thisfunc, stickySessionValue.c_str());
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
                       agent_info.postdatapreserve_sticky_session_mode);
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
am_web_get_postdata_preserve_URL_parameter(char **queryParameter)
{
    const char *thisfunc = "am_web_get_postdata_preserve_URL_parameter()";
    am_status_t status = AM_SUCCESS;
    std::string stickySessionValue;
    std::string cookieName, cookieValue;
    size_t equalPos = 0;
    
    // If stickySessionMode or stickySessionValue is empty, 
    // then there is no LB in front of the agent.
    if ((agent_info.postdatapreserve_sticky_session_mode == NULL) ||
        (strlen(agent_info.postdatapreserve_sticky_session_mode) == 0) ||
        (agent_info.postdatapreserve_sticky_session_value == NULL) ||
        (strlen(agent_info.postdatapreserve_sticky_session_value) == 0))
    {
        status = AM_INVALID_ARGUMENT;
    } else if (strcmp(agent_info.postdatapreserve_sticky_session_mode, "URL") != 0) {
        // Deals only with the case where the sticky session mode is URL.
        status = AM_INVALID_ARGUMENT;
        if (strcmp(agent_info.postdatapreserve_sticky_session_mode, "COOKIE") != 0) {
            am_web_log_warning("%s: %s is not a correct value for the property "
                             "config.postdata.preserve.stickysession.value.",
                             thisfunc, 
                             agent_info.postdatapreserve_sticky_session_mode);
        }
    }
    // Check if the sticky session value has a correct format ("param=value")
    if (status  == AM_SUCCESS) {
        stickySessionValue.assign(agent_info.postdatapreserve_sticky_session_value);
        equalPos = stickySessionValue.find('=');
        if (equalPos != std::string::npos) {
            cookieName = stickySessionValue.substr(0, equalPos);
            cookieValue = stickySessionValue.substr(equalPos+1);
            if (cookieName.empty() || cookieValue.empty()) {
                am_web_log_warning("%s: The property "
                     "config.postdata.preserve.stickysession.value "
                     "(%s) does not a have correct format.",
                     thisfunc, stickySessionValue.c_str());
                status = AM_INVALID_ARGUMENT;
            }
        } else {
            am_web_log_warning("%s: The property "
                     "config.postdata.preserve.stickysession.value "
                     "(%s) does not have a correct format.",
                     thisfunc, stickySessionValue.c_str());
            status = AM_INVALID_ARGUMENT;
        }
    }
    if (status == AM_SUCCESS) {
        *queryParameter = strdup(stickySessionValue.c_str());
        if (*queryParameter == NULL) {
            am_web_log_error("%s: Not enough memory to allocate "
                             "the queryParameter variable.",
                             thisfunc);
            status = AM_NO_MEMORY;
        }
    }
    if (status == AM_SUCCESS) {
            am_web_log_debug("%s: Sticky session mode: %s", thisfunc, 
                       agent_info.postdatapreserve_sticky_session_mode);
            am_web_log_debug("%s: Sticky session value: %s", 
                             thisfunc, *queryParameter);
    }
    return status;
}

/**
 * Returns client.ip.header property value
 */
extern "C" AM_WEB_EXPORT const char *
am_web_get_client_ip_header_name()
{
    if ((agent_info.clientIPHeader != NULL) &&
        (strlen(agent_info.clientIPHeader) == 0))
    {
        return NULL;
    } else {
        return agent_info.clientIPHeader;
    }
}

/**
 * Returns client.hostname.header property value
 */
extern "C" AM_WEB_EXPORT const char *
am_web_get_client_hostname_header_name()
{
    if ((agent_info.clientHostnameHeader != NULL) &&
        (strlen(agent_info.clientHostnameHeader) == 0))
    {
        return NULL;
    } else {
        return agent_info.clientHostnameHeader;
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
                              const am_map_t env_parameter_map)
{
    const char *thisfunc = "am_web_set_host_ip_in_env_map()";
    am_status_t status = AM_SUCCESS;
    PRStatus prStatus;
    PRNetAddr address;
    PRHostEnt hostEntry;
    char buffer[PR_NETDB_BUF_SIZE];

    if((client_ip != NULL) && (strlen(client_ip) > 0)) {
        // Set the client IP in the environment map
        am_web_log_debug("%s: map_insert: client_ip=%s", thisfunc, client_ip);
        am_map_insert(env_parameter_map, requestIp, client_ip, AM_TRUE);
        if( client_hostname != NULL && strlen(client_hostname) > 0) {
            // Set the hostname in the environment map if it is not null
            am_web_log_debug("%s: map_insert: client_hostname=%s", 
                             thisfunc, client_hostname);
            status = am_map_insert(env_parameter_map,
                                   requestDnsName,
                                   client_hostname,
                                   AM_FALSE);
        } else if (agent_info.getClientHostname) {
            // Try to get the hostname through DNS reverse lookup
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
            } else {
                am_web_log_warning("%s: map_insert: could not get client's "
                                   "hostname for policy. Error %s.",
                                   thisfunc, PR_ErrorToString(PR_GetError(),
                                   PR_LANGUAGE_I_DEFAULT));
            }
        }
    }
    return status;
}

bool areHeadersEqual(string s1, string s2) {
   bool isEqual = true;
   if (s1.size() == s2.size()) {
       for (int i=0 ; i<s1.size() ; i++) {
           // In headers any "-" will become "_", therefore 
           // replace all "-" by "_".
           if (s1[i] == '-') {
               s1[i] = '_';
           }
           if (s2[i] == '-') {
               s2[i] = '_';
           }
           // Header comparison should be case insensitve
           if (toupper(s1[i]) != toupper(s2[i])) {
               isEqual = false;
               break;
           }
       }
   } else {
       isEqual = false;
   }
   return isEqual;
}

void remove_attribute_from_headers(string attribute_name,
                                   char** header_list_char)
{
    const char *thisfunc = "remove_attribute_from_headers()";
    am_status_t status = AM_SUCCESS;
    size_t startPos = 0;
    size_t endPos = 0;
    size_t colonPos = 0;
    string header_list, header, header_name;
    
    if ((header_list_char == NULL) || (strlen(*header_list_char) == 0)) {
        status = AM_INVALID_ARGUMENT;
    }
    if (status == AM_SUCCESS) {
        header_list.assign(*header_list_char);
        while (startPos < header_list.size()) {
            // Get the header name/value pair
            endPos = header_list.find("\r\n", startPos);
            if (endPos != string::npos) {
                header = header_list.substr(startPos, endPos-startPos);
                // Get the header name
                colonPos = header.find(":");
                if (colonPos != string::npos) {
                    header_name = header.substr(0, colonPos);
                    if (!header_name.empty()) {
                        if (areHeadersEqual(attribute_name, header_name)) {
                            header_list.erase(startPos, endPos-startPos + 2);
                            am_web_log_debug("%s: Header \"%s\" has been removed.",
                                              thisfunc, header.c_str());
                        }
                    }
                }
            } else {
                break;
            }
            startPos = endPos;
            while ((startPos <= header_list.size()) &&
                   ((header_list[startPos] == '\r') ||
                    (header_list[startPos] == '\n')))
            {
                startPos++;
            }
        }
        // Save the new header list
        memset(*header_list_char, 0, strlen(*header_list_char));
        strncpy(*header_list_char, header_list.c_str(), header_list.length());
    }
}

/**
 * Remove all the attributes listed in attributes_list_char
 * from header_list. 
 */
extern "C" AM_WEB_EXPORT void
am_web_remove_attributes_from_headers(char* attributes_list_char, char** header_list)
{
    const char *thisfunc = "am_web_remove_attributes_from_headers()";
    am_status_t status = AM_SUCCESS;
    string attribute_list, attribute, attribute_name;
    size_t startPos = 0;
    size_t endPos = 0;
    size_t colonPos = 0;
    
    if ((attributes_list_char == NULL) || 
        (strlen(attributes_list_char) == 0)) 
    {
        am_web_log_warning("%s: attributes_list_char is NULL.", thisfunc);
        status = AM_INVALID_ARGUMENT;
    }
    if ((*header_list == NULL) || (strlen(*header_list) == 0)) {
        am_web_log_warning("%s: header_list is NULL.", thisfunc);
        status = AM_INVALID_ARGUMENT;
    }
    if (status == AM_SUCCESS) {
        attribute_list.assign(attributes_list_char);
        while (startPos < attribute_list.size()) {
            // Get the attribute name/value pair
            endPos = attribute_list.find("\r\n", startPos);
            if (endPos != string::npos) {
                attribute = attribute_list.substr(startPos, endPos-startPos);
                // Get the attribute name
                colonPos = attribute.find(":");
                if (colonPos != string::npos) {
                    attribute_name = attribute.substr(0, colonPos);
                    // Remove the attribute from the header list
                    if (!attribute_name.empty()) {
                        remove_attribute_from_headers(attribute_name,
                                                      &(*header_list));
                    }
                }
            } else {
                break;
            }
            startPos = endPos;
            while ((startPos < attribute_list.size()) &&
                   ((attribute_list[startPos] == '\r') ||
                    (attribute_list[startPos] == '\n')))
            {
                startPos++;
            }
        }
    }
}

#if (defined(WINNT) || defined(_AMD64_))
AM_BEGIN_EXTERN_C
BOOL WINAPI DllMain(HINSTANCE hInst, DWORD fdwReason, LPVOID lpvReserved) {
        if(fdwReason == DLL_PROCESS_ATTACH) {
                agent_info.hInst = hInst;
        }
        return TRUE;
}
AM_END_EXTERN_C
#endif
