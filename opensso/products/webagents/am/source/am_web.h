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
 * $Id: am_web.h,v 1.32 2010/03/10 05:09:37 dknab Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef AM_WEB_H
#define AM_WEB_H

#ifdef _MSC_VER
#if defined(AM_BUILDING_LIB)
#define AM_WEB_EXPORT __declspec(dllexport)
#else
#define AM_WEB_EXPORT __declspec(dllimport)
#endif
#else
#define AM_WEB_EXPORT
#endif

#define AM_WEB_ALLOW_USER_MSG            "User %s was allowed access to %s"
#define AM_WEB_DENIED_USER_MSG            "User %s was denied access to %s"

#include <stdlib.h>
#include <am.h>
#include "am_policy.h"
#include "am_log.h"
#define AM_RESERVED NULL

AM_BEGIN_EXTERN_C

/**
 *                     -------- constants --------
 */


#define AM_WEB_ACTION_ALLOWED		"allow"
#define AM_WEB_ACTION_DENIED		"deny"

/*
 * Constants used in looking up HTTP headers or request attributes.
 *
 * NOTE:  These constants must be all lowercase or the lookup will fail.
 */
#define CONTENT_LENGTH_HDR      	"content-length"
#define COOKIE_HDR              	"cookie"
#define HOST_HDR                	"host"
#define PATH_INFO               	"path-info"
#define REQUEST_IP_ADDR         	"ip"
#define REQUEST_METHOD          	"method"
#define REQUEST_QUERY           	"query"
#define REQUEST_URI             	"uri"
#define REQUEST_PROTOCOL        	"protocol"
#define REQUEST_CLF             	"clf-request"
#define AUTH_USER_VAR           	"auth-user"
#define AUTH_TYPE_VAR           	"auth-type"
#define GOTO_PARAMETER          	"goto"
#define REFERER_SERVLET         	"refererservlet"

/* for am_web_process_request and related functions */
#define REQUEST_METHOD_GET                      "GET"
#define REQUEST_METHOD_POST                     "POST"
#define REQUEST_METHOD_HEAD                     "HEAD"
#define REQUEST_METHOD_PUT                      "PUT"
#define REQUEST_METHOD_DELETE                   "DELETE"
#define REQUEST_METHOD_TRACE                    "TRACE"
#define REQUEST_METHOD_OPTIONS                  "OPTIONS"
#define REQUEST_METHOD_CONNECT                  "CONNECT"
#define REQUEST_METHOD_COPY                     "COPY"
#define REQUEST_METHOD_INVALID                  "INVALID"
#define REQUEST_METHOD_LOCK                     "LOCK"
#define REQUEST_METHOD_UNLOCK                   "UNLOCK"
#define REQUEST_METHOD_MKCOL                    "MKCOL"
#define REQUEST_METHOD_MOVE                     "MOVE"
#define REQUEST_METHOD_PATCH                    "PATCH"
#define REQUEST_METHOD_PROPFIND                 "PROPFIND"
#define REQUEST_METHOD_PROPPATCH                "PROPPATCH"
#define REQUEST_METHOD_VERSION_CONTROL          "VERSION_CONTROL"
#define REQUEST_METHOD_CHECKOUT                 "CHECKOUT"
#define REQUEST_METHOD_UNCHECKOUT               "UNCHECKOUT"
#define REQUEST_METHOD_CHECKIN                  "CHECKIN"
#define REQUEST_METHOD_UPDATE                   "UPDATE"
#define REQUEST_METHOD_LABEL                    "LABEL"
#define REQUEST_METHOD_REPORT                   "REPORT"
#define REQUEST_METHOD_MKWORKSPACE              "MKWORKSPACE"
#define REQUEST_METHOD_MKACTIVITY               "MKACTIVITY"
#define REQUEST_METHOD_BASELINE_CONTROL         "BASELINE_CONTROL"
#define REQUEST_METHOD_MERGE                    "MERGE"
#define REQUEST_METHOD_CONFIG                   "CONFIG"
#define REQUEST_METHOD_ENABLE_APP               "ENABLE-APP"
#define REQUEST_METHOD_DISABLE_APP              "DISABLE-APP"
#define REQUEST_METHOD_STOP_APP                 "STOP-APP"
#define REQUEST_METHOD_STOP_APP_RSP             "STOP-APP-RSP"
#define REQUEST_METHOD_REMOVE_APP               "REMOVE-APP"
#define REQUEST_METHOD_STATUS                   "STATUS"
#define REQUEST_METHOD_STATUS_RSP               "STATUS-RSP"
#define REQUEST_METHOD_INFO                     "INFO"
#define REQUEST_METHOD_INFO_RSP                 "INFO-RSP"
#define REQUEST_METHOD_DUMP                     "DUMP"
#define REQUEST_METHOD_DUMP_RSP                 "DUMP-RSP"
#define REQUEST_METHOD_PING                     "PING"
#define REQUEST_METHOD_PING_RSP                 "PING-RSP"
#define REQUEST_METHOD_UNKNOWN                  "UNKNOWN"


#define HTTP_PROTOCOL_STR       	"http://"
#define HTTP_PROTOCOL_STR_LEN   	(sizeof(HTTP_PROTOCOL_STR) - 1)
#define HTTPS_PROTOCOL_STR      	"https://"
#define HTTPS_PROTOCOL_STR_LEN  	(sizeof(HTTPS_PROTOCOL_STR) - 1)


#define AM_WEB_LOGIN_URL_PROPERTY    AM_COMMON_PROPERTY_PREFIX "login.url"
#define AM_WEB_CHECK_CLIENT_IP_PROPERTY		AM_COMMON_PROPERTY_PREFIX "client.ip.validation.enable"
#define AM_WEB_ACCESS_DENIED_URL_PROPERTY AM_COMMON_PROPERTY_PREFIX "access.denied.url"
#define AM_WEB_ANONYMOUS_USER AM_COMMON_PROPERTY_PREFIX "anonymous.user.id"
#define AM_WEB_ANON_REMOTE_USER_ENABLE AM_COMMON_PROPERTY_PREFIX "anonymous.user.enable"
#define AM_WEB_URL_REDIRECT_PARAM AM_COMMON_PROPERTY_PREFIX "redirect.param"
#define AM_WEB_NOT_ENFORCED_LIST_PROPERTY AM_COMMON_PROPERTY_PREFIX "notenforced.url"
#define AM_WEB_REVERSE_NOT_ENFORCED_LIST AM_COMMON_PROPERTY_PREFIX "notenforced.url.invert"
#define AM_WEB_NOT_ENFORCED_IPADDRESS AM_COMMON_PROPERTY_PREFIX "notenforced.ip"
#define AM_WEB_NOTENFORCED_URL_ATTRS_ENABLED_PROPERTY AM_COMMON_PROPERTY_PREFIX "notenforced.url.attributes.enable"
#define AM_WEB_DO_SSO_ONLY AM_COMMON_PROPERTY_PREFIX "sso.only"
#define AM_WEB_CDSSO_ENABLED_PROPERTY AM_COMMON_PROPERTY_PREFIX "cdsso.enable"
#define AM_WEB_CDC_SERVLET_URL_PROPERTY AM_COMMON_PROPERTY_PREFIX "cdsso.cdcservlet.url"

#define AM_WEB_POST_CACHE_ENTRY_LIFETIME AM_COMMON_PROPERTY_PREFIX "postcache.entry.lifetime"
#define AM_WEB_POST_CACHE_DATA_PRESERVE AM_COMMON_PROPERTY_PREFIX "postdata.preserve.enable"
#define AM_WEB_POST_CACHE_DATA_PRESERVE_STICKY_SESSION_MODE AM_COMMON_PROPERTY_PREFIX "postdata.preserve.stickysession.mode"
#define AM_WEB_POST_CACHE_DATA_PRESERVE_STICKY_SESSION_VALUE AM_COMMON_PROPERTY_PREFIX "postdata.preserve.stickysession.value"

#define AM_WEB_URI_PREFIX AM_COMMON_PROPERTY_PREFIX "agenturi.prefix"

#define AM_WEB_FQDN_MAP AM_COMMON_PROPERTY_PREFIX "fqdn.mapping"
#define AM_WEB_FQDN_DEFAULT AM_COMMON_PROPERTY_PREFIX "fqdn.default"
#define AM_WEB_FQDN_CHECK_ENABLE AM_COMMON_PROPERTY_PREFIX "fqdn.check.enable"

#define AM_WEB_COOKIE_RESET_ENABLED AM_COMMON_PROPERTY_PREFIX "cookie.reset.enable"
#define AM_WEB_COOKIE_RESET_LIST AM_COMMON_PROPERTY_PREFIX "cookie.reset"
#define AM_WEB_CDSSO_COOKIE_DOMAIN_LIST AM_COMMON_PROPERTY_PREFIX "cdsso.cookie.domain"
#define AM_WEB_LOGOUT_URL_PROPERTY AM_COMMON_PROPERTY_PREFIX "logout.url"
#define AM_WEB_AGENT_LOGOUT_URL_PROPERTY AM_COMMON_PROPERTY_PREFIX "agent.logout.url"
#define AM_WEB_LOGOUT_REDIRECT_URL_PROPERTY AM_COMMON_PROPERTY_PREFIX "logout.redirect.url"
#define AM_WEB_LOGOUT_COOKIE_RESET_PROPERTY AM_COMMON_PROPERTY_PREFIX "logout.cookie.reset"
#define AM_WEB_GET_CLIENT_HOSTNAME AM_COMMON_PROPERTY_PREFIX "get.client.host.name"

#define AM_WEB_CONVERT_MBYTE_ENABLE AM_COMMON_PROPERTY_PREFIX  "convert.mbyte.enable"
#define AM_WEB_ENCODE_URL_SPECIAL_CHARS AM_COMMON_PROPERTY_PREFIX  "encode.url.special.chars.enable"
#define AM_WEB_ENCODE_COOKIE_SPECIAL_CHARS AM_COMMON_PROPERTY_PREFIX  "encode.cookie.special.chars.enable"
#define AM_WEB_OVERRIDE_PROTOCOL AM_COMMON_PROPERTY_PREFIX  "override.protocol"
#define AM_WEB_OVERRIDE_HOST AM_COMMON_PROPERTY_PREFIX  "override.host"
#define AM_WEB_OVERRIDE_PORT AM_COMMON_PROPERTY_PREFIX  "override.port"
#define AM_WEB_OVERRIDE_NOTIFICATION_URL AM_COMMON_PROPERTY_PREFIX  "override.notification.url"

#define AM_COMMON_IGNORE_PATH_INFO    AM_COMMON_PROPERTY_PREFIX "ignore.path.info"
#define AM_COMMON_IGNORE_PATH_INFO_FOR_NOT_ENFORCED_LIST    AM_COMMON_PROPERTY_PREFIX "ignore.path.info.for.not.enforced.list"

/* Followings are for the attribute related properties */
#define AM_POLICY_PROFILE_ATTRS_MODE AM_COMMON_PROPERTY_PREFIX "profile.attribute.fetch.mode"
#define AM_POLICY_PROFILE_ATTRS_MAP   AM_COMMON_PROPERTY_PREFIX "profile.attribute.mapping"
#define AM_POLICY_PROFILE_ATTRS_COOKIE_PFX AM_COMMON_PROPERTY_PREFIX "profile.attribute.cookie.prefix"
#define AM_POLICY_PROFILE_ATTRS_COOKIE_MAX_AGE AM_COMMON_PROPERTY_PREFIX "profile.attribute.cookie.maxage"

#define AM_POLICY_SESSION_ATTRS_MODE	AM_COMMON_PROPERTY_PREFIX "session.attribute.fetch.mode"
#define AM_POLICY_SESSION_ATTRS_MAP   AM_COMMON_PROPERTY_PREFIX "session.attribute.mapping"
#define AM_POLICY_RESPONSE_ATTRS_MODE AM_COMMON_PROPERTY_PREFIX "response.attribute.fetch.mode"
#define AM_POLICY_RESPONSE_ATTRS_MAP   AM_COMMON_PROPERTY_PREFIX "response.attribute.mapping"
#define AM_POLICY_CLOCK_SKEW AM_COMMON_PROPERTY_PREFIX "policy.clock.skew"
#define AM_POLICY_ATTRS_MULTI_VALUE_SEPARATOR   AM_COMMON_PROPERTY_PREFIX "attribute.multi.value.separator"

/* Followings are for the Header attribute modes */
#define AM_POLICY_SET_ATTRS_AS_COOKIE "HTTP_COOKIE"
#define AM_POLICY_SET_ATTRS_AS_HEADER "HTTP_HEADER"
#define AM_POLICY_SET_ATTRS_AS_NONE "NONE"

#define AM_PROXY_PROPERTY_PREFIX        AM_COMMON_PROPERTY_PREFIX "proxy."
#define AM_DOMINO_PROPERTY_PREFIX       AM_COMMON_PROPERTY_PREFIX "domino."
#define AM_PROXY_OVERRIDE_HOST_PORT_PROPERTY  AM_PROXY_PROPERTY_PREFIX "override.host.port"

#define LTPA_DEFAULT_TOKEN_NAME "LtpaToken"
#define LTPA_DEFAULT_CONFIG_NAME "LtpaToken"
#define LTPA_DEFAULT_ORG_NAME NULL

#define AM_DOMINO_CHECK_NAME_DB_PROPERTY  AM_DOMINO_PROPERTY_PREFIX "check.name.database"
#define AM_DOMINO_LTPA_TOKEN_ENABLE_PROPERTY  AM_DOMINO_PROPERTY_PREFIX "ltpa.enable"
#define AM_DOMINO_LTPA_CONFIG_NAME_PROPERTY  AM_DOMINO_PROPERTY_PREFIX "ltpa.config.name"
#define AM_DOMINO_LTPA_ORG_NAME_PROPERTY  AM_DOMINO_PROPERTY_PREFIX "ltpa.org.name"
#define AM_DOMINO_LTPA_TOKEN_NAME_PROPERTY  AM_DOMINO_PROPERTY_PREFIX "ltpa.cookie.name"

#define AM_WEB_CONNECTION_TIMEOUT AM_COMMON_PROPERTY_PREFIX ".auth.connection.timeout"
#define AM_WEB_AUTHTYPE_IN_IIS6_AGENT AM_COMMON_PROPERTY_PREFIX  "iis.auth.type"
#define AM_COMMON_PROPERTY_PREFIX_IIS6_REPLAYPASSWD_KEY AM_COMMON_PROPERTY_PREFIX "replaypasswd.key"
#define AM_WEB_FILTER_PRIORITY AM_COMMON_PROPERTY_PREFIX  "iis.filter.priority"
#define AM_WEB_OWA_ENABLED AM_COMMON_PROPERTY_PREFIX  "iis.owa.enable"
#define AM_WEB_OWA_ENABLED_CHANGE_PROTOCOL AM_COMMON_PROPERTY_PREFIX  "iis.owa_enable.change.protocol"
#define AM_WEB_OWA_ENABLED_SESSION_TIMEOUT_URL AM_COMMON_PROPERTY_PREFIX  "iis.owa.enable.session.timeout.url"


#define AM_WEB_AGENT_REPOSITORY_LOCATION_PROPERTY           AM_COMMON_PROPERTY_PREFIX "repository.location"
#define AGENT_PROPERTIES_LOCAL "local"
#define AGENT_PROPERTIES_CENTRALIZED "centralized"
#define AM_WEB_AGENT_FREEFORM_PROPERTY           AM_COMMON_PROPERTY_PREFIX "freeformproperties"
#define AM_COMMON_AGENTS_CONFIG_POLLING_PROPERTY AM_COMMON_PROPERTY_PREFIX "polling.interval"
#define AM_COMMON_AGENTS_CONFIG_CLEANUP_PROPERTY AM_COMMON_PROPERTY_PREFIX "cleanup.interval"

#define AM_WEB_LOCALE_PROPERTY AM_COMMON_PROPERTY_PREFIX "locale"
#define AM_WEB_CLIENT_IP_HEADER_PROPERTY AM_COMMON_PROPERTY_PREFIX "client.ip.header"
#define AM_WEB_CLIENT_HOSTNAME_HEADER_PROPERTY AM_COMMON_PROPERTY_PREFIX "client.hostname.header"

#define AM_WEB_EMPTY_POST "EMPTY"
/*
 * Enough space to hold PRTime key in a string
 */
#define	AM_WEB_MAX_POST_KEY_LENGTH	64

/*
 * Enough space to hold a formatted TCP port number and terminating NULL.
 */
#define	AM_WEB_MAX_PORT_STR_LEN		6

/*
 * Return values of am_web_is_cookie_present
 */
#define AM_WEB_COOKIE_EXIST             2
#define AM_WEB_COOKIE_MODIFIED          1
#define AM_WEB_COOKIE_ABSENT            0
#define AM_WEB_COOKIE_ERROR            -1

/*
 * Auth-Type identifier for OpenAM authentication.
 */
#define	AM_WEB_AUTH_TYPE_VALUE		"DSAME"

/*
 * Liberty like Authn Response parameter
 */
#define	LARES_PARAM		"LARES"


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

#define IIS_FILTER_PRIORITY  "DEFAULT"

#define bool_to_am_bool_t(x) (x?AM_TRUE:AM_FALSE)
#define	HTTP_PREFIX	"http://"
#define	HTTP_PREFIX_LEN	(sizeof(HTTP_PREFIX) - 1)
#define	HTTP_DEF_PORT	80
#define	HTTPS_PREFIX	"https://"
#define	HTTPS_PREFIX_LEN (sizeof(HTTPS_PREFIX) - 1)
#define HTTPS_DEF_PORT	443
#define MSG_MAX_LEN 1024
#define AM_REVISION_LEN 10

#define COOKIE_ATTRIBUTE_PREFIX  "HTTP_"
#define COOKIE_ATTRIBUTE_MAX_AGE  "300"
#define POLICY_ATTRIBUTES_MODE_NONE "NONE"
#define ATTR_MULTI_VALUE_SEPARATOR "|"
#define POLICY_SESSION_ATTRIBUTES "SESSION_ATTRIBUTES"
#define POLICY_RESPONSE_ATTRIBUTES "RESPONSE_ATTRIBUTES"

typedef enum {
    SET_ATTRS_NONE,
    SET_ATTRS_AS_HEADER,
    SET_ATTRS_AS_COOKIE
} set_user_attrs_mode_t;

/*
 * POST preservation related strings
*/
#define MAGIC_STR		"sunpostpreserve"
#define DUMMY_NOTENFORCED	"/dummypost*"
#define DUMMY_REDIRECT		"/dummypost/"
#define POSTHASHTBL_INITIAL_SIZE 31

/*
 * How long to wait in attempting to connect to an OpenAM AUTH server.
 */

#define CONNECT_TIMEOUT	2

const char CDSSO_RESET_COOKIE_TEMPLATE[] = {
"%s=;Max-Age=300;Path=/"
};

/*
 * Auth-Type ACCESS status.
 */
typedef enum {
    AM_ACCESS_DENY = 0,
    AM_ACCESS_ALLOW
} am_web_access_t;

/*
 * For agents request processing
 */

typedef enum {
    AM_WEB_REQUEST_UNKNOWN,
    AM_WEB_REQUEST_GET,
    AM_WEB_REQUEST_POST,
    AM_WEB_REQUEST_HEAD,
    AM_WEB_REQUEST_PUT,
    AM_WEB_REQUEST_DELETE,
    AM_WEB_REQUEST_TRACE,
    AM_WEB_REQUEST_OPTIONS,
    AM_WEB_REQUEST_CONNECT,
    AM_WEB_REQUEST_COPY,
    AM_WEB_REQUEST_INVALID,
    AM_WEB_REQUEST_LOCK,
    AM_WEB_REQUEST_UNLOCK,
    AM_WEB_REQUEST_MOVE,
    AM_WEB_REQUEST_MKCOL,
    AM_WEB_REQUEST_PATCH,
    AM_WEB_REQUEST_PROPFIND,
    AM_WEB_REQUEST_PROPPATCH,
    AM_WEB_REQUEST_VERSION_CONTROL,
    AM_WEB_REQUEST_CHECKOUT,
    AM_WEB_REQUEST_UNCHECKOUT,
    AM_WEB_REQUEST_CHECKIN,
    AM_WEB_REQUEST_UPDATE,
    AM_WEB_REQUEST_LABEL,
    AM_WEB_REQUEST_REPORT,
    AM_WEB_REQUEST_MKWORKSPACE,
    AM_WEB_REQUEST_MKACTIVITY,
    AM_WEB_REQUEST_BASELINE_CONTROL,
    AM_WEB_REQUEST_MERGE,
    AM_WEB_REQUEST_CONFIG,
    AM_WEB_REQUEST_ENABLE_APP,
    AM_WEB_REQUEST_DISABLE_APP,
    AM_WEB_REQUEST_STOP_APP,
    AM_WEB_REQUEST_STOP_APP_RSP,        
    AM_WEB_REQUEST_REMOVE_APP,
    AM_WEB_REQUEST_STATUS,
    AM_WEB_REQUEST_STATUS_RSP,
    AM_WEB_REQUEST_INFO,
    AM_WEB_REQUEST_INFO_RSP,
    AM_WEB_REQUEST_DUMP,
    AM_WEB_REQUEST_DUMP_RSP,
    AM_WEB_REQUEST_PING,
    AM_WEB_REQUEST_PING_RSP
} am_web_req_method_t;

typedef enum {
    AM_WEB_RESULT_OK,               /* access check was OK */
    AM_WEB_RESULT_OK_DONE,          /* OK and handled (for ex. notification) */
    AM_WEB_RESULT_FORBIDDEN,        /* access forbidden */
    AM_WEB_RESULT_REDIRECT,         /* redirected */
    AM_WEB_RESULT_ERROR             /* internal error */
} am_web_result_t;

typedef struct {
    char *url;                      /* The full request URL */
    char *query;                    /* query string if any */
    am_web_req_method_t method;     /* request method */
    char *path_info;                /* path info if any */
    char *client_ip;                /* client IP if any */
    char *client_hostname;          /* client hostname if any */
    char *cookie_header_val;	    /* the cookie header value if any */
    void *reserved;		    /* reserved - do not set this */
} am_web_request_params_t;


/**
 * Following are functions that agents must define/write
 * for calling am_web_process_request for agents request processing.
 */

/**
 * Get post data.
 * The post data returned must be null terminated and will be freed by
 * calling the free_post_data function.
 * Should return AM_SUCCESS on success, any other error will result in
 * HTTP internal error result.
 */
typedef am_status_t (*am_web_get_post_data_func_t)(void **args, char **data);
typedef struct {
    am_web_get_post_data_func_t func;
    void **args;
} am_web_get_post_data_t;

/**
 * Free the post data in get_post_data. Can be null if not needed.
 * Should return AM_SUCCESS if successful. If not status will be
 * logged as warning but ignored.
 */
typedef am_status_t (*am_web_free_post_data_func_t)(void **args, char *data);
typedef struct {
    am_web_free_post_data_func_t func;
    void **args;
} am_web_free_post_data_t;

/**
 * Set (and check) user
 * Should return AM_SUCCESS on success, any other error will result in
 * HTTP forbidden result.
 */
typedef am_status_t (*am_web_set_user_func_t)(void **args, const char *user);
typedef struct {
    am_web_set_user_func_t func;
    void **args;
} am_web_set_user_t;

/**
 * Set the request method.
 * Required and used in CDSSO mode to set/modify the request method.
 * Should return AM_SUCCESS on success, any other error will result in
 * HTTP internal error result.
 *
 * Arguments:
 *
 * args - agent defined arguments to pass in.
 * method - the request method to set.
 *
 */
typedef am_status_t (*am_web_set_method_func_t)(
			void **args, am_web_req_method_t method);
typedef struct {
    am_web_set_method_func_t func;
    void **args;
} am_web_set_method_t;

/**
 * Render the http result, one of am_web_result_t.
 * For AM_WEB_RESULT_OK_DONE, agent should return a HTTP respons code 200 OK
 * and the body of the HTTP response should be set to the string in the
 * data argument.
 * For AM_WEB_RESSULT_REDIRECT, agent should return a HTTP response code 302,
 * and Location header should be set to the redirect url in the data argument.
 */
typedef am_status_t (*am_web_render_result_func_t)(void **args,
			am_web_result_t http_result, char *data);
typedef struct {
    am_web_render_result_func_t func;
    void **args;
} am_web_render_result_t;

/**
 * Set a header in the request.
 * If a header of the same name already exists it should be replaced
 * with another header of the same name.
 *
 * Arguments:
 *
 * args - agent defined arguments to pass in.
 * name - the header name
 * val - the header value
 *
 * Return:
 *
 * This function should return AM_SUCCESS if the header was successfully set
 * and appropriate am_status_t code otherwise.
 */
typedef am_status_t (*am_web_set_header_in_request_func_t)(
			void **args, const char *name, const char *val);
typedef struct {
    am_web_set_header_in_request_func_t func;
    void **args;
} am_web_set_header_in_request_t;

/**
 * Set(Add) a header in response.
 * The header should be added to the response even if another header
 * of the same name already exists.
 *
 * Arguments:
 *
 * args - agent defined arguments to pass in.
 * name - the header name
 * val - the header value
 *
 * Return:
 *
 * This function should return AM_SUCCESS if the header was successfully set
 * and appropriate am_status_t code otherwise.
 */
typedef am_status_t (*am_web_add_header_in_response_func_t)(
			void **args, const char *name, const char *val);
typedef struct {
    am_web_add_header_in_response_func_t func;
    void **args;
} am_web_add_header_in_response_t;

/**
 * Store post data, generated magic url key and
 * original action url in agent shared cache
 *
 * Arguments:
 *
 * args - agent defined arguments to pass in.
 * key - generated post data preservation key (part of Magic url)
 * acturl - action url (original request url)
 * value - actual post data
 * postcacheentry_life - shared pdp cache entry validity period (in minutes)
 *
 * Return:
 *
 * This function should return AM_SUCCESS if store was successfully 
 * and appropriate am_status_t code otherwise.
 */
typedef am_status_t (*am_web_register_postdata_func_t)(
			void **args, const char *key, const char *acturl, const char *value, const unsigned long postcacheentry_life);
typedef struct {
    am_web_register_postdata_func_t func;
    void **args;
} am_web_register_postdata_t;

/**
 * Retrieve post data from 
 * agent shared cache
 *
 * Arguments:
 *
 * args - agent defined arguments to pass in.
 * requestURL - magic url 
 * page - actual data stored in a shared cache (if found)
 *
 * Return:
 *
 * This function should return AM_SUCCESS if retrieve was successfully 
 * and appropriate am_status_t code otherwise.
 */
typedef am_status_t (*am_web_check_postdata_func_t)(
			void **args, const char *requestURL, char **page, const unsigned long postcacheentry_life);
typedef struct {
    am_web_check_postdata_func_t func;
    void **args;
} am_web_check_postdata_t;

typedef am_status_t(*am_web_add_notes_in_response_func_t)(
        void **args, const char *name, const char *val);

typedef struct {
    am_web_add_notes_in_response_func_t func;
    void **args;
} am_web_add_notes_in_response_t;
/**
 * structure for all the functions above
 */
typedef struct {

    // get post data
    // the post data returned must be null terminated.
    am_web_get_post_data_t get_post_data;

    // free the post data returned in get_post_data. Can be null if not needed
    am_web_free_post_data_t free_post_data;

    // set user in the web container.
    am_web_set_user_t set_user;

    // set method
    am_web_set_method_t set_method;

    // set a request header
    am_web_set_header_in_request_t set_header_in_request;

    // add a response header
    am_web_add_header_in_response_t add_header_in_response;

    // render http result
    am_web_render_result_t render_result;
    
    //PDP
    am_web_register_postdata_t reg_postdata;
    
    am_web_check_postdata_t check_postdata;
    
    am_web_add_notes_in_response_t set_notes_in_request;

} am_web_request_func_t;

/**
 * Method to return the latest instance of agent configuration 
 * from agent configuration cache. 
 * The returned instance gets used to serve requests.
 */
AM_WEB_EXPORT void* am_web_get_agent_configuration();

/**
 * Method to delete ref counted object associated with
 * the latest instance of agent configuration. 
 */
AM_WEB_EXPORT void am_web_delete_agent_configuration(void *);

/**
 * Agent plugin init function calls to load bootstrap properties file.
 */
AM_WEB_EXPORT am_status_t am_web_init(const char *agent_bootstrap_file, 
                                          const char *agent_config_file);

/**
 * Initializes agent during first request. Creates the agent profile object 
 * and performs agent authentication to receive the initial agent configuration
 * data either from the OpenAM server or from the local configuration file
 */
AM_WEB_EXPORT am_status_t am_agent_init(boolean_t *pAgentAuthenticated);

/**
 * Method to clean up the Agent Toolkit
 */
AM_WEB_EXPORT am_status_t am_web_cleanup();

/**
 * Method to retrieve Agent version information
 * 
 * Parameters:
 *  info       Pointer to array of four (constant) char pointers, where,
 *             first entry will point to agent version information,
 *             second to SVN revision number,
 *             third to build date,
 *             fourth to build machine information.
 * All entries are NULL terminated C strings. 
 * Pointers to entry values must not be released (free()) by caller.
 */
AM_WEB_EXPORT void am_agent_version(char **info);

/**
 *                   -------- policy methods --------
 */

/*
 * Evaluates the access control policies for a specified web-resource and
 * action.
 *
 * Parameters:
 *   sso_token	The sso_token from the OpenAM server cookie.  This
 *		parameter may be NULL if there is no cookie present.
 *
 *   url	The URL whose accessibility is being determined.
 *		This parameter may not be NULL.
 *
 *   action_name
 *		The action (GET, POST, etc.) being performed on the
 *		specified URL.  This parameter may not be NULL.
 *
 *   client_ip
 *		The IP address of the client attempting to access the
 *		specified URL.  If client IP validation is turned on,
 *		then this parameter may not be NULL.
 *
 *   env_parameter_map
 *		A map containing additional information about the user
 *		attempting to access the specified URL.  This parameter
 *		may not be NULL.
 *
 *   advices_map_ptr
 *		An output parameter where an am_map_t can be stored
 *		if the policy evaluation produces any advice information.
 *		This parameter may not be NULL.
 *
 *   agent_config
 *		Agent configuration instance returned by  
 *		am_web_get_agent_configuration(). 
 *		This parameter may not be NULL.
 *
 * Returns:
 *   AM_SUCCESS
 *		if the evaluation was performed successfully and access
 *		is to be allowed to the specified resource
 *
 *   AM_NO_MEMORY
 *		if the evaluation was not successfully completed due to
 *		insufficient memory being available
 *
 *   AM_INVALID_ARGUMENT
 *		if any of the url, action_name, env_parameter_map, or
 *		advices_map_ptr parameters is NULL or if client IP validation
 *		is enabled and the client_ip parameter is NULL.
 *
 *   AM_INVALID_SESSION
 *		if the specified sso_token does not refer to a currently
 *		valid session
 *
 *   AM_ACCESS_DENIED
 *		if the policy information indicates that the user does
 *		not have permission to access the specified resource or
 *		any error is detected other than the ones listed above
 */
AM_WEB_EXPORT am_status_t
am_web_is_access_allowed(const char *sso_token, const char *url,
			 const char *path_info, const char *action_name,
			 const char *client_ip, const am_map_t env_parameter_map,
			 am_policy_result_t *result, void* agent_config);

/*
 * Determines whether the request contains is an OpenAM server 
 * notification message intended for the policy SDK.
 */
AM_WEB_EXPORT boolean_t am_web_is_notification(const char *request_url, void* agent_config);

/*
 * Returns true if the URL being accessed by the user is in the not
 * enforced list.
 */
AM_WEB_EXPORT boolean_t am_web_is_in_not_enforced_list(const char *url,
						       const char *path_info, void* agent_config);

/*
 * Returns true if the given IP address is present in the list of
 * not enforced IP addresses.
 */
AM_WEB_EXPORT boolean_t am_web_is_in_not_enforced_ip_list(const char *ip, void* agent_config);

/*
 * Returns if the requested URL is a Valid FQDN resource.
 */
AM_WEB_EXPORT boolean_t am_web_is_valid_fqdn_url(const char *url, void* agent_config);


/*
 * Handles notification data received by an agent.  This code handles
 * generating logging messages for the event and any error that may
 * occur during the processing of the notification.
 */
AM_WEB_EXPORT void am_web_handle_notification(const char *data,
                                              size_t data_length,
                                              void* agent_config);

/*
 * This function returns a string representing the URL for redirection that
 * is appropriate to the provided status code and advice map returned by
 * the Policy SDK.  This may either redirect the user to the login URL or
 * the access denied URL.  If the redirection is to the login URL then the
 * URL will include any exsisting information specified in the URL from the
 * configuration file, like org value etc., followed by the specified goto
 * parameter value, which will be used by OpenAM server after the user has
 * successfully authenticated.
 *
 * The function am_web_get_redirect_url(), has been deprecated and
 * must not be used.  It is supported only for backward compatibility
 * reasons.
 * The last parameter reserved must be passed with NULL.
 *
 * Note: If the redirect_url returned is NOT NULL, the caller of this function
 * must call am_web_free_memory(void *) to free the pointer.
 */
AM_WEB_EXPORT am_status_t am_web_get_url_to_redirect(am_status_t status,
					 	     const am_map_t advice_map,
					  	     const char *goto_url,
						     const char* method,
						     void *reserved,
					  	     char ** redirect_url,
                                                     void* agent_config);

/*
 * This function sets the LDAP attributes in header.
 * The method will be called when ldapattribute.mode is set to "HEADER"
 *
 * Parameters:
 *   key	    key whose value to be set.
 *
 *   attrValues     The value string that will be set.
 *
 *   args           container specific argument containing request.
 *
 * Returns:
 *   AM_SUCCESS
 *		    if the setting is successful.
 *
 */
typedef am_status_t (*am_web_result_set_header_func_t)(
                         const char *key,
                         const char *attrValues,
                         void **args);

/*
 * This function sets the LDAP attributes in header through cookie in response.
 * The method will be called when ldapattribute.mode is set to "COOKIE"
 *
 * Parameters:
 *   cookieValues   The string containing cookie value that will be set.
 *
 *   args           container specific argument containing request.
 *
 * Returns:
 *   AM_SUCCESS
 *		    if the setting is successful.
 */
typedef am_status_t (*am_web_result_set_header_attr_in_response_func_t)(
                         const char *cookieValues,
                         void **args);

/*
 * This function sets the LDAP attributes in header through cookie in request.
 * The method will be called when ldapattribute.mode is set to "COOKIE"
 *
 * Parameters:
 *   cookieValues   The string containing cookie value that will be set.
 *
 *   args           container specific argument containing request.
 *
 * Returns:
 *   AM_SUCCESS
 *		    if the setting is successful.
 */
typedef am_status_t (*am_web_result_set_header_attr_in_request_func_t)(
                         const char *cookieValues,
                         void **args);

/*
 * This function at present is DUMMY. will be needed for cookie synchronization.
 * has been added to get the interface.
 *
 * Parameters:
 *   cookieName     Name of the cookie to be synchronized with.
 *
 *   dproCookie
 *
 *   args           container specific argument containing request.
 *
 * Returns:
 *   AM_SUCCESS
 *		    if the setting is successful.
 */
typedef am_status_t (*am_web_get_cookie_sync_func_t)(
                         const char *cookieName,
                         char **dproCookie,
                         void **args);

/*
 * NOTE - This function replaces am_web_do_result_attr_map_set() in the
 * previous version of the library. The old interface is still provided
 * in the library but deprecated. Users need to explicity declare it as
 * follows to use it.
 *         am_status_t am_web_do_result_attr_map_set(
 *                              am_policy_result *result,
 *                              am_status_t (*setFunc)(const char *,
 *                                                     const char *,
 *                                                     void **),
 *                              void **args);
 *
 * This function processes attr_response_map of am_policy_result_t
 * and performs the appropriate set action that is passed in.
 *
 */
AM_WEB_EXPORT am_status_t
am_web_result_attr_map_set(
              am_policy_result_t *result,
              am_web_result_set_header_func_t setHeaderFunc,
              am_web_result_set_header_attr_in_response_func_t setCookieRespFunc,
              am_web_result_set_header_attr_in_request_func_t  setCookieReqFunc,
              am_web_get_cookie_sync_func_t  getCookieSyncFunc,
              void **args, void* agent_config);

/*
 * This function the reset_cookie headers for the cookies
 * specified in the configuration file and invokes the  set action
 * that caller (i.e. the agent) passes in for each of them.
 */
AM_WEB_EXPORT am_status_t
am_web_do_cookies_reset(am_status_t (*setFunc)(const char *, void **),
			void **args, void* agent_config);


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
AM_WEB_EXPORT am_status_t
am_web_do_cookie_domain_set(am_status_t (*setFunc)(const char *, void **),
			    void **args, const char *cookie, void* agent_config);


/*
 * This function is used to get the cookie sent in the SAML assertion
 * from the OpenAM server 
 */

AM_WEB_EXPORT am_status_t
am_web_get_token_from_assertion(char *assertion, char **token, void* agent_config);

AM_WEB_EXPORT am_status_t
am_web_remove_parameter_from_query(const char* inpString, const char *remove_str, char **outString );

AM_WEB_EXPORT am_status_t
am_web_get_parameter_value(const char *inpQuery, const char *param_name, char **param_value);

AM_WEB_EXPORT boolean_t
am_web_is_cdsso_enabled(void* agent_config);

AM_WEB_EXPORT boolean_t
am_web_is_cache_control_enabled(void* agent_config);

AM_WEB_EXPORT boolean_t
am_web_is_iis_logonuser_enabled(void* agent_config);

AM_WEB_EXPORT boolean_t
am_web_is_password_header_enabled(void* agent_config);

AM_WEB_EXPORT boolean_t
am_web_is_remoteuser_header_disabled(void* agent_config);

AM_WEB_EXPORT const char *am_web_get_password_encryption_key(void* agent_config);

AM_WEB_EXPORT int am_web_validate_url(void* agent_config, const char *url);

AM_WEB_EXPORT am_status_t am_web_check_cookie_in_post(
		void ** args, char ** dpro_cookie,
		char ** request_url,
		char **orig_req, char *method,
		char *response,
		boolean_t responseIsCookie,
		am_status_t (*set_cookie)(const char *, void **),
		void (*set_method)(void **, char *),
                void* agent_config
		);
AM_WEB_EXPORT am_status_t am_web_check_cookie_in_query(
		void **args, char **dpro_cookie,
		const char *query, char **request_url,
		char ** orig_req, char *method,
		am_status_t (*set_cookie)(const char *, void **),
		void (*set_method)(void **, char *),
                void* agent_config
		);




/*
 * Free memory previously allocated by a am_web_* routine.
 */
AM_WEB_EXPORT void am_web_free_memory(void *memory);

/*
 * Method to retrieve the name of the OpenAM server cookie.
 */

AM_WEB_EXPORT const char *am_web_get_cookie_name(void* agent_config);

/*
 * Method to retrieve the value of the OpenAM server cookie.
 */

AM_WEB_EXPORT am_status_t am_web_get_cookie_value(const char *separator, const char *cookie_name, const char *cookie_header_val, char **value);

/*
 * Method to retrieve the name of the OpenAM server notification Url.
 */
AM_WEB_EXPORT const char *am_web_get_notification_url(void* agent_config);


/*
 * Method to retrieve the name of the Agent Server Host.
 */
AM_WEB_EXPORT const char *am_web_get_agent_server_host(void* agent_config);

/*
 * Method to retrieve the name of the Agent Server Port.
 */
AM_WEB_EXPORT int am_web_get_agent_server_port(void* agent_config);


/**
 *                        -------- logging --------
 */

AM_WEB_EXPORT boolean_t am_web_is_debug_on();
AM_WEB_EXPORT boolean_t am_web_is_max_debug_on();

AM_WEB_EXPORT void am_web_log_always(const char *fmt, ...);
AM_WEB_EXPORT boolean_t am_web_log_auth(am_web_access_t access_type, 
                                        void* agent_config, const char *fmt, ...);
AM_WEB_EXPORT void am_web_log_error(const char *fmt, ...);
AM_WEB_EXPORT void am_web_log_warning(const char *fmt, ...);
AM_WEB_EXPORT void am_web_log_info(const char *fmt, ...);
AM_WEB_EXPORT void am_web_log_debug(const char *fmt, ...);
AM_WEB_EXPORT void am_web_log_max_debug(const char *fmt, ...);

AM_WEB_EXPORT char *am_web_http_decode(const char *string, size_t len);


/*
 *		         --------- POST cache preservation -------
 */

/*
 * Temporary structure to store post data before it is inserted in
 * POST hash table
 * Members :
 *     value
 *         String value for POST data
 *     url
 *         Destination URL for the POST request
 */

typedef struct am_web_postcache_data {
    char *value;
    char *url;
} am_web_postcache_data_t;

/*
 * Temporary structure to hold dummy url, action url
 * POST preservation key. All three of these variables
 * are required for POST preservation. Client passes
 * in the request URL for POST to public function
 * am_web_create_post_preserve_urls which creates
 * this structure and returns a pointer to the client
 *
 * Members :
 *     dummy_url
 *         Dummy URL for redirect for POST preservation
 *     action_url
 *         Destination URL for POST request
 *     post_time_key
 *         Unique key to tag POST data entry
 */

typedef struct post_urls {
    char *dummy_url;
    char *action_url;
    char *post_time_key;
} post_urls_t;


/*
 * Method to find out if POST data preservation is enabled
 * by clients through AMAgent.Properties
 * Returns :
 *    boolean_t
 *        True or False depending on whether POST
 *        preservation is switched on or off.
 *
 */
AM_WEB_EXPORT boolean_t am_web_is_postpreserve_enabled(void* agent_config);

/*
 * Method to get the value of the set-cookie header for the lb cookie
 * when using a LB in front of the agent with post preservation
 * enabled
 */
AM_WEB_EXPORT am_status_t
am_web_get_postdata_preserve_lbcookie(char **headerValue, 
                          boolean_t isValueNull, void* agent_config);

/*
 * Method to get the query parameter that should be added to the
 * dummy url when using a LB in front of the agent with post 
 * preservation enabled and sticky session mode set to URL.
 */
AM_WEB_EXPORT am_status_t 
am_web_get_postdata_preserve_URL_parameter(char **queryParameter,
                                           void* agent_config);

/*
 * Method to insert POST data entry in the POST cache
 * Parameters:
 *    key
 *        POST data preservation key for every entry
 *    value
 *        Structure to store POST data value and redirect URL
 *
 * Returns:
 *    boolean_t
 *        True or False depending on whether insertion was
 *        successful or a failure
 *
 */
AM_WEB_EXPORT boolean_t am_web_postcache_insert(const char *key,
						const am_web_postcache_data_t *value,
                                                void* agent_config);

/*
 * Method to lookup POST data in the POST cache
 *
 * Parameters:
 *     key
 *         Key to search POST data entry in POST data structure
 *
 * Returns:
 *     am_web_postcache_data_t
 *         Data structure containing POST data and redirect URL
 *
 */

AM_WEB_EXPORT boolean_t
am_web_postcache_lookup(const char *key,
			am_web_postcache_data_t *postdata_entry,
                        void* agent_config);

/*
 * Method to remove POST data from the POST cache
 *
 * Parameters:
 *     key
 *         Key to remove an entry from POST data structure
 *
 * Returns:
 *
 */
AM_WEB_EXPORT void am_web_postcache_remove(const char *key,
                                           void* agent_config);


/*
 * Method to construct dummy post url, action url and unique key
 *
 * Parameters:
 *    request_url
 *        The request URL for POST in the HTTP request
 *
 * Returns:
 *    post_urls_t
 *        Data structure that contains Dummy redirect URL, POST destination
 *        URL and POST preservation key. Dummy redirect URL is filtered by
 *        web server SAF to identify POST preservation redirects from general
 *        redirects. All three of these variables are required for POST
 *        preservation.
 *
 */
AM_WEB_EXPORT am_status_t am_web_create_post_preserve_urls(
                                  const char *request_url, 
                                  post_urls_t **url_data,
                                  void* agent_config);

/*
 * Method to clean up datastructure containing dummy post url, action url and
 * unique key
 *
 * Paramaters:
 *     posturl_struct
 *         Pointer to POST preservation URL data structure post_urls_t
 *
 * Returns
 *
 */
AM_WEB_EXPORT void am_web_clean_post_urls(post_urls_t *posturl_struct);

/*
 * Method to clean up data structure containing post string value,
 * redirect url
 *
 * Paramaters:
 *     const am_web_postcache_data_t
 *         Pointer to POST data entry
 *
 * Returns
 *
 */
AM_WEB_EXPORT void
am_web_postcache_data_cleanup(am_web_postcache_data_t * const postentry_struct);


/*
 * Create the html form with the javascript that submits the POST
 * with the invisible name value pairs
 *
 * Parameters:
 *     key
 *         Unique key to identify POST data entry. It is used to
 *         remove post data once the page is re-posted
 *     postdata
 *         POST data entry as a browser encoded string
 *     actionurl
 *         POST destination URL
 *
 * Returns
 *     char *
 *         POST form to be resubmitted
 *
*/
AM_WEB_EXPORT char * am_web_create_post_page(const char *key,
					     const char *postdata,
					     const char *actionurl,
                                             void* agent_config);

/*
 * Check whether a cookie is present.
 *
 * Parameters:
 *     cookie 
 *			Pointer to a cookie.
 *	   value 
 *			Pointer to a value.
 *     new_cookie
 *         Pointer to a pointer to the location of the new cookie.
 *     
 * Returns
 *     2,1,0 or -1 as defined above
 *
 */
AM_WEB_EXPORT int am_web_is_cookie_present(const char *cookie,
                                                 const char *value,
                                                 char **new_cookie);

/*
 * Resets the cookie configured to be reset on logout.
 * The reset function passed in is called for each cookie that is configured.
 * If the function failed for any cookie, the last failed status is returned.
 */
AM_WEB_EXPORT am_status_t
am_web_logout_cookies_reset(am_status_t (*setFunc)(const char *, void **),
                            void **args, void* agent_config);



/*
 * Returns true if url is a opensso logout url, false otherwise.
 * For example: http://amhost:amport/opensso/UI/Logout
 */
AM_WEB_EXPORT boolean_t am_web_is_logout_url(const char *url, void* agent_config);


/*
 * Returns true if url is a agent logout url, false otherwise.
 * When this URL is accessed, the user gets logged out of the OpenAM session.
 * For example : http://agenthost:agentport/logout.html
 */
AM_WEB_EXPORT boolean_t am_web_is_agent_logout_url(const char *url, void* agent_config);


/**
 * When a user comes back from the CDSSO authentication, there are a
 * list of Liberty parameters added when the user was redirected
 * for authentication.  This function removes those extra parameters, so
 * that the request could be forwarded to the applications as it came
 * from the browser.
 *
 * Parameter:
 *    inpString: Request URL that was recieved after authentication
 *               containing Liberty attributes.
 *    outString: Address of output string where all the Liberty
 *               attributes are cleaned up. Must be pre-allocated
 *               by caller with same size as inpString.
 *
 *
 * Returns:
 *  am_status_t:
 *               AM_SUCCESS if operation was successful, appropriate
 *               error codes otherwise.
 */
AM_WEB_EXPORT am_status_t
am_web_remove_authnrequest(char *inpString, char **outString);


/**
 * Processes a request access check and returns a HTTP result to be
 * rendered by the agent. Result can be OK, OK-done,
 * forbidden, error, or redirect with a redirect URL.
 * The render status is returned in the render_sts argument.
 * See am_web_request_func_t for description of each function.
 */
AM_WEB_EXPORT am_web_result_t
am_web_process_request(am_web_request_params_t *req_params,
		       am_web_request_func_t *req_func,
		       am_status_t *render_sts, void* agent_config);


/**
 * Converts a am_web_req_method_t number to a string as defined in RFC 2068.
 * If the method number passed is unrecognized, "UNKNOWN" is returned.
 */
AM_WEB_EXPORT const char *
am_web_method_num_to_str(am_web_req_method_t method);


/**
 * Converts a method string as defined in RFC 2068 to a am_web_req_method_t
 * number. If the string is unrecognized AM_WEB_REQUEST_UNKNOWN is returned.
 */
AM_WEB_EXPORT am_web_req_method_t
am_web_method_str_to_num(const char *method_str);

/**
 * Returns the name of a am_web_result_t as a string.
 * For example, AM_WEB_RESULT_OK returns "AM_WEB_RESULT_OK".
 * If the result code passed is unrecognized, "Unknown result code"
 * is returned.
 */
AM_WEB_EXPORT const char *
am_web_result_num_to_str(am_web_result_t result);

/**
 * Sets the given cookie in the cookie header in the request.
 * Arguments:
 * cookie header - the cookie header in the request
 * set_cookie_value - the cookie name and value in set-cookie response
 *		      header form. this should be the same argument as
 *		      the cookieValues argument of the
 *		      am_web_result_set_header_attr_in_request_func_t function.
 * new_cookie_header - contains either null, or the original cookie_header, or
 *		       a new point containing the new cookie header value which
 *		       needs to be freed by the caller.
 */
AM_WEB_EXPORT am_status_t
am_web_set_cookie(char *cookie_header, const char *set_cookie_value,
		  char **new_cookie_header);


AM_WEB_EXPORT am_status_t
am_web_build_advice_response(const am_policy_result_t *policy_result,
                             const char *redirect_url,
                             char **advice_response);

/*
 * Method to determine if the auth-type value "dsame"
 * in the IIS6 agent should be replaced by "Basic"
 */
AM_WEB_EXPORT const char *am_web_get_authType(void* agent_config);

/**
 * Method will take the host header, server host name
 * port, protocol, query parameter, URI and return the
 * URL to be used for agent purposes.
 * Parameters:
 *  host_hdr
 *	value of host header string as sent by browser
 *  protocol
 *	Protocol the container is servicing
 *  hostname
 *	Host name as known to the container
 *  port
 *	port number as known to the container
 *  uri
 *	URI of the request
 *  query
 *	query parameters sent with the request
 *  req_url
 *	OUT parameter which will be populated with the
 *	value of URL string to be used by Agent
 * Returns:
 *  am_status_t
 *	the status of operation.
 */
AM_WEB_EXPORT am_status_t
am_web_get_request_url(const char *host_hdr, const char *protocol,
		       const char *hostname, size_t port,
		       const char *uri, const char *query,
		       char **req_url, void* agent_config);

/**
 * Sets original request url and request url.
 * request url gets overrideden if override properties are set.
 * Original request url gets used during notification request processing.
 * request url is used for rest of request processing.
 */
AM_WEB_EXPORT am_status_t
am_web_get_all_request_urls(const char *host_hdr,
                       const char *protocol,
                       const char *hostname,
                       size_t port,
                       const char *uri,
                       const char *query,
                       void* agent_config,
                       char **request_url,
                       char **orig_request_url);

/*
 * Method to determine if the composite advice should be
 * redirect rather than POST
 */
AM_WEB_EXPORT boolean_t am_web_use_redirect_for_advice(void* agent_config);

AM_WEB_EXPORT am_status_t
am_web_build_advice_redirect_url(const am_policy_result_t *policy_result,
                             const char *redirect_url,
                             char **redirect_url_with_advice);

/*
 * Method to determine if the override_host_port is set
 * for the Proxy agent
 */
AM_WEB_EXPORT boolean_t am_web_is_proxy_override_host_port_set(void* agent_config);

/*
 * Method to if ltpa_enable is set for IBM Lotus Domino agent
 */
AM_WEB_EXPORT boolean_t am_web_is_domino_check_name_database(void* agent_config);

/*
 * Method to if ltpa_enable is set for IBM Lotus Domino agent
 */
AM_WEB_EXPORT boolean_t am_web_is_domino_ltpa_enable(void* agent_config);

/*
 * Method to get ltpa_config_name
 */
AM_WEB_EXPORT const char *am_web_domino_ltpa_config_name(void* agent_config);

/*
 * Method to get ltpa_org_name
 */
AM_WEB_EXPORT const char *am_web_domino_ltpa_org_name(void* agent_config);

/* 
 * Method to get ltpa_token_name
 */
AM_WEB_EXPORT const char *am_web_domino_ltpa_token_name(void* agent_config);

/*
 * Method to determine if a url is enforced
 */
AM_WEB_EXPORT boolean_t
am_web_is_url_enforced(const char *url_str,const char *path_info,
                       const char *client_ip, void* agent_config);

/*
 * Method to get the value of user id param
 */
AM_WEB_EXPORT const char * am_web_get_user_id_param(void* agent_config);
AM_WEB_EXPORT void am_web_clear_attributes_map(am_policy_result_t *result);

AM_WEB_EXPORT boolean_t am_web_is_owa_enabled(void* agent_config);
AM_WEB_EXPORT 
    boolean_t am_web_is_owa_enabled_change_protocol(void* agent_config);
AM_WEB_EXPORT 
    const char * am_web_is_owa_enabled_session_timeout_url(void* agent_config);
AM_WEB_EXPORT am_status_t am_web_get_logout_url(char** logout_url,
                                                void* agent_config);

/**
 * Returns client.ip.header property value
 */
AM_WEB_EXPORT const char *
    am_web_get_client_ip_header_name(void* agent_config);

/**
 * Returns client.hostname.header property value
 */
AM_WEB_EXPORT const char *
    am_web_get_client_hostname_header_name(void* agent_config);

/*
 * Returns client IP and hostname value from client IP and hostname headers.
 * If the client IP header or client host name header contains comma 
 * separated values, then first value is taken into consideration.
 */
AM_WEB_EXPORT am_status_t
    am_web_get_client_ip_host(const char *clientIPHeader,
                              const char *clientHostHeader,
                              char **clientIP,
                              char **clientHost);

/**
 * Sets client ip (and client hostname) in environment map
 * which then sent as part of policy request.
 */
AM_WEB_EXPORT am_status_t
    am_web_set_host_ip_in_env_map(const char *client_ip,
                              const char *client_hostname,
                              const am_map_t env_parameter_map,
                              void* agent_config);


AM_END_EXTERN_C

#endif	/* not AM_WEB_H */
