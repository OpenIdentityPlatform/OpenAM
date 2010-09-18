/* -*- Mode: C -*- */
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: am_web.h,v 1.4 2008/08/19 19:11:38 veiming Exp $
 */

#ifndef AM_WEB_H
#define AM_WEB_H

#if     defined(WINNT) && !(AM_STATIC_LIB)
#if     defined(AM_BUILDING_LIB)

#define AM_WEB_EXPORT    __declspec(dllexport)
#else /* if defined(AM_BUILDING_LIB) */
#define AM_WEB_EXPORT    __declspec(dllimport)
#endif
#else /* if defined(WINNT) */
#define AM_WEB_EXPORT
#endif

#if !defined(WINNT)
#define AM_WEB_ALLOW_USER_MSG            "User %s was allowed access to %s."
#define AM_WEB_DENIED_USER_MSG            "User %s was denied access to %s."
#endif

#include <stdlib.h>
#include <am.h>
#include "am_policy.h"
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
#define REQUEST_METHOD_TYPE     	"sunwMethod"
#define REFERER_SERVLET         	"refererservlet"
#define FORM_METHOD_POST        	"POST"
#define FORM_METHOD_GET         	"GET"

#define HTTP_PROTOCOL_STR       	"http://"
#define HTTP_PROTOCOL_STR_LEN   	(sizeof(HTTP_PROTOCOL_STR) - 1)
#define HTTPS_PROTOCOL_STR      	"https://"
#define HTTPS_PROTOCOL_STR_LEN  	(sizeof(HTTPS_PROTOCOL_STR) - 1)


#define AM_WEB_PROPERTY_PREFIX		"com.sun.am.policy.agents.config."
#define AM_WEB_PROPERTY_PREFIX_OLD	"com.sun.am.policy.agents."
#define AM_WEB_AGENTS_VERSION_OLD AM_WEB_PROPERTY_PREFIX_OLD "version"
#define AM_WEB_CHECK_CLIENT_IP_PROPERTY		AM_WEB_PROPERTY_PREFIX "client_ip_validation.enable"
#define AM_WEB_CHECK_CLIENT_IP_PROPERTY_OLD		AM_WEB_PROPERTY_PREFIX_OLD "client_ip_validation_enable"
#define AM_WEB_ACCESS_DENIED_URL_PROPERTY AM_WEB_PROPERTY_PREFIX "accessdenied.url"
#define AM_WEB_ACCESS_DENIED_URL_PROPERTY_OLD AM_WEB_PROPERTY_PREFIX_OLD "accessDeniedURL"
#define AM_WEB_ANONYMOUS_USER AM_WEB_PROPERTY_PREFIX "anonymous_user"
#define AM_WEB_ANONYMOUS_USER_OLD AM_WEB_PROPERTY_PREFIX_OLD "unauthenticatedUser"
#define AM_WEB_ANON_REMOTE_USER_ENABLE AM_WEB_PROPERTY_PREFIX "anonymous_user.enable"
#define AM_WEB_ANON_REMOTE_USER_ENABLE_OLD AM_WEB_PROPERTY_PREFIX_OLD "anonRemoteUserEnabled"
#define AM_WEB_URL_REDIRECT_PARAM AM_WEB_PROPERTY_PREFIX "url.redirect.param"
#define AM_WEB_URL_REDIRECT_PARAM_OLD AM_WEB_PROPERTY_PREFIX_OLD "urlRedirectParam"
#define AM_WEB_INSTANCE_NAME_PROPERTY	AM_WEB_PROPERTY_PREFIX "instance.name"
#define AM_WEB_INSTANCE_NAME_PROPERTY_OLD	AM_WEB_PROPERTY_PREFIX_OLD "instanceName"
#define AM_WEB_NOT_ENFORCED_LIST_PROPERTY AM_WEB_PROPERTY_PREFIX "notenforced_list"
#define AM_WEB_NOT_ENFORCED_LIST_PROPERTY_OLD AM_WEB_PROPERTY_PREFIX_OLD "notenforcedList"
#define AM_WEB_REVERSE_NOT_ENFORCED_LIST AM_WEB_PROPERTY_PREFIX "notenforced_list.invert"
#define AM_WEB_REVERSE_NOT_ENFORCED_LIST_OLD AM_WEB_PROPERTY_PREFIX_OLD "reverse_the_meaning_of_notenforcedList"
#define AM_WEB_NOT_ENFORCED_IPADDRESS AM_WEB_PROPERTY_PREFIX "notenforced_client_ip_list"
#define AM_WEB_NOT_ENFORCED_IPADDRESS_OLD AM_WEB_PROPERTY_PREFIX_OLD "notenforced_client_IP_address_list"
#define AM_WEB_IGNORE_POLICY_EVALUATION_IF_NOT_ENFORCED AM_WEB_PROPERTY_PREFIX "ignore_policy_evaluation_if_notenforced"
#define AM_WEB_DO_SSO_ONLY AM_WEB_PROPERTY_PREFIX "do_sso_only"
#define AM_WEB_DO_SSO_ONLY_OLD AM_WEB_PROPERTY_PREFIX_OLD "do_sso_only"
#define AM_WEB_CDSSO_ENABLED_PROPERTY AM_WEB_PROPERTY_PREFIX "cdsso.enable"
#define AM_WEB_CDSSO_ENABLED_PROPERTY_OLD AM_WEB_PROPERTY_PREFIX_OLD "cdsso-enabled"
#define AM_WEB_CDC_SERVLET_URL_PROPERTY AM_WEB_PROPERTY_PREFIX "cdcservlet.url"
#define AM_WEB_CDC_SERVLET_URL_PROPERTY_OLD AM_WEB_PROPERTY_PREFIX_OLD "cdcservletURL"

#define AM_WEB_POST_CACHE_CLEANPUP_INTERVAL AM_WEB_PROPERTY_PREFIX "postcache.cleanup.interval"
#define AM_WEB_POST_CACHE_CLEANPUP_INTERVAL_OLD AM_WEB_PROPERTY_PREFIX_OLD "postcachecleanupinterval"
#define AM_WEB_POST_CACHE_ENTRY_LIFETIME AM_WEB_PROPERTY_PREFIX "postcache.entry.lifetime"
#define AM_WEB_POST_CACHE_ENTRY_LIFETIME_OLD AM_WEB_PROPERTY_PREFIX_OLD "postcacheentrylifetime"
#define AM_WEB_POST_CACHE_DATA_PRESERVE AM_WEB_PROPERTY_PREFIX "postdata.preserve.enable"
#define AM_WEB_POST_CACHE_DATA_PRESERVE_OLD AM_WEB_PROPERTY_PREFIX_OLD "is_postdatapreserve_enabled"
#define AM_WEB_URI_PREFIX AM_WEB_PROPERTY_PREFIX "agenturi.prefix"
#define AM_WEB_URI_PREFIX_OLD AM_WEB_PROPERTY_PREFIX_OLD "agenturiprefix"
#define AM_LOG_ACCESS_TYPE_PROPERTY  AM_WEB_PROPERTY_PREFIX "audit.accesstype"
#define AM_LOG_ACCESS_TYPE_PROPERTY_OLD  AM_WEB_PROPERTY_PREFIX_OLD "logAccessType"

#define AM_WEB_FQDN_MAP AM_WEB_PROPERTY_PREFIX "fqdn.map"
#define AM_WEB_FQDN_MAP_OLD AM_WEB_PROPERTY_PREFIX_OLD "fqdnMap"
#define AM_WEB_FQDN_DEFAULT AM_WEB_PROPERTY_PREFIX "fqdn.default"
#define AM_WEB_FQDN_DEFAULT_OLD AM_WEB_PROPERTY_PREFIX_OLD "fqdnDefault"
#define AM_WEB_FQDN_CHECK_ENABLE AM_WEB_PROPERTY_PREFIX "fqdn.check.enable"

#define AM_WEB_COOKIE_RESET_ENABLED AM_WEB_PROPERTY_PREFIX "cookie.reset.enable"
#define AM_WEB_COOKIE_RESET_ENABLED_OLD AM_WEB_PROPERTY_PREFIX_OLD "cookie_reset_enabled"
#define AM_WEB_COOKIE_RESET_LIST AM_WEB_PROPERTY_PREFIX "cookie.reset.list"
#define AM_WEB_COOKIE_RESET_LIST_OLD AM_WEB_PROPERTY_PREFIX_OLD "cookie_reset_list"
#define AM_WEB_COOKIE_DOMAIN_LIST AM_WEB_PROPERTY_PREFIX "cookie.domain.list"
#define AM_WEB_COOKIE_DOMAIN_LIST_OLD AM_WEB_PROPERTY_PREFIX "cookieDomainList"
#define AM_WEB_LOGOUT_URL_PROPERTY AM_WEB_PROPERTY_PREFIX "logout.url"
#define AM_WEB_LOGOUT_URL_PROPERTY_OLD AM_WEB_PROPERTY_PREFIX_OLD "logout.url"
#define AM_WEB_LOGOUT_COOKIE_RESET_PROPERTY AM_WEB_PROPERTY_PREFIX "logout.cookie.reset.list"
#define AM_WEB_LOGOUT_COOKIE_RESET_PROPERTY_OLD AM_WEB_PROPERTY_PREFIX_OLD "logout.cookie_reset_list"
#define AM_WEB_GET_CLIENT_HOSTNAME AM_WEB_PROPERTY_PREFIX "get_client_host_name"
#define AM_WEB_GET_CLIENT_HOSTNAME_OLD AM_WEB_PROPERTY_PREFIX_OLD "getClientHostName"

#define AM_WEB_DENY_ON_LOG_FAILURE AM_WEB_PROPERTY_PREFIX "deny_on_log_failure"
#define AM_WEB_DENY_ON_LOG_FAILURE_OLD AM_WEB_PROPERTY_PREFIX_OLD "denyOnLogFailure"
#define AM_WEB_CONVERT_MBYTE_ENABLE AM_WEB_PROPERTY_PREFIX  "convert_mbyte.enable"
#define AM_WEB_ENCODE_URL_SPECIAL_CHARS AM_WEB_PROPERTY_PREFIX  "encode_url_special_chars.enable"
#define AM_WEB_CONVERT_MBYTE_ENABLE_OLD AM_WEB_PROPERTY_PREFIX_OLD  "convertMbyteEnabled"
#define AM_WEB_OVERRIDE_PROTOCOL AM_WEB_PROPERTY_PREFIX  "override_protocol"
#define AM_WEB_OVERRIDE_PROTOCOL_OLD AM_WEB_PROPERTY_PREFIX_OLD  "overrideProtocol"
#define AM_WEB_OVERRIDE_HOST AM_WEB_PROPERTY_PREFIX  "override_host"
#define AM_WEB_OVERRIDE_HOST_OLD AM_WEB_PROPERTY_PREFIX_OLD  "overrideHost"
#define AM_WEB_OVERRIDE_PORT AM_WEB_PROPERTY_PREFIX  "override_port"
#define AM_WEB_OVERRIDE_PORT_OLD AM_WEB_PROPERTY_PREFIX_OLD  "overridePort"
#define AM_WEB_OVERRIDE_NOTIFICATION_URL AM_WEB_PROPERTY_PREFIX  "override_notification.url"
#define AM_WEB_OVERRIDE_NOTIFICATION_URL_OLD AM_WEB_PROPERTY_PREFIX_OLD  "overrideNotificationUrl"

#define AM_COMMON_IGNORE_PATH_INFO    AM_WEB_PROPERTY_PREFIX "ignore_path_info"
#define AM_COMMON_CLIENT_IP_CHECK_ENABLED_PROPERTY   AM_WEB_PROPERTY_PREFIX "client.ip.check.enable"

/* Following are log related properties */
#define AM_COMMON_LOG_FILE_PROPERTY	AM_WEB_PROPERTY_PREFIX "local.log.file"
#define AM_COMMON_LOG_ROTATION		AM_WEB_PROPERTY_PREFIX "local.log.rotate"
#define AM_COMMON_LOG_FILE_SIZE		AM_WEB_PROPERTY_PREFIX "local.log.size"
#define AM_COMMON_LOG_REMOTE_BUFFER_SIZE_PROPERTY	AM_WEB_PROPERTY_PREFIX "remote.log.buffer.size"
#define AM_COMMON_SERVER_LOG_FILE_PROPERTY   AM_WEB_PROPERTY_PREFIX "remote.log"

/* Followings are for the attribute related properties */
#define AM_POLICY_PROFILE_ATTRS_MODE AM_WEB_PROPERTY_PREFIX "profile.attribute.fetch.mode"
#define AM_POLICY_PROFILE_ATTRS_MAP   AM_WEB_PROPERTY_PREFIX "profile.attribute.map"
#define AM_POLICY_PROFILE_ATTRS_COOKIE_PFX AM_WEB_PROPERTY_PREFIX "profile.attribute.cookie.prefix"
#define AM_POLICY_PROFILE_ATTRS_COOKIE_MAX_AGE AM_WEB_PROPERTY_PREFIX "profile.attribute.cookie.maxage"

#define AM_POLICY_SESSION_ATTRS_MODE	AM_WEB_PROPERTY_PREFIX "session.attribute.fetch.mode"
#define AM_POLICY_SESSION_ATTRS_MAP   AM_WEB_PROPERTY_PREFIX "session.attribute.map"
#define AM_POLICY_RESPONSE_ATTRS_MODE AM_WEB_PROPERTY_PREFIX "response.attribute.fetch.mode"
#define AM_POLICY_RESPONSE_ATTRS_MAP   AM_WEB_PROPERTY_PREFIX "response.attribute.map"
#define AM_POLICY_ATTRS_MULTI_VALUE_SEPARATOR AM_WEB_PROPERTY_PREFIX "attribute.multi_value_separator"

/* Followings are for the Header attribute modes */
#define AM_POLICY_SET_ATTRS_AS_COOKIE "HTTP_COOKIE"
#define AM_POLICY_SET_ATTRS_AS_COOKIE_OLD "COOKIE"
#define AM_POLICY_SET_ATTRS_AS_HEADER "HTTP_HEADER"
#define AM_POLICY_SET_ATTRS_AS_HEADER_OLD "HEADER"
#define AM_POLICY_SET_ATTRS_AS_NONE "NONE"

#define AM_PROXY_PROPERTY_PREFIX        AM_WEB_PROPERTY_PREFIX "proxy."
#define AM_DOMINO_PROPERTY_PREFIX       AM_WEB_PROPERTY_PREFIX "domino."
#define AM_PROXY_OVERRIDE_HOST_PORT_PROPERTY  AM_PROXY_PROPERTY_PREFIX "override_host_port"
#define AM_DOMINO_CHECK_NAME_DB_PROPERTY  AM_DOMINO_PROPERTY_PREFIX "check_name_database"
#define AM_DOMINO_CHECK_NAME_DB_PROPERTY_OLD  AM_DOMINO_PROPERTY_PREFIX "checkNameDatabase"
#define AM_WEB_CONNECTION_TIMEOUT AM_WEB_PROPERTY_PREFIX "connection_timeout"
#define AM_WEB_AUTHTYPE_IN_IIS6_AGENT AM_WEB_PROPERTY_PREFIX  "iis.auth_type"
#define AM_COMMON_PROPERTY_PREFIX_IIS6_REPLAYPASSWD_KEY AM_COMMON_PROPERTY_PREFIX "replaypasswd.key"
#define AM_WEB_FILTER_PRIORITY AM_WEB_PROPERTY_PREFIX  "iis.filter_priority"
#define AM_WEB_OWA_ENABLED AM_WEB_PROPERTY_PREFIX  "iis.owa_enabled"
#define AM_WEB_OWA_ENABLED_CHANGE_PROTOCOL AM_WEB_PROPERTY_PREFIX  "iis.owa_enabled_change_protocol"
#define AM_WEB_OWA_ENABLED_SESSION_TIMEOUT_URL AM_WEB_PROPERTY_PREFIX  "iis.owa_enabled_session_timeout_url"
#define AM_WEB_NO_CHILD_THREAD_ACTIVATION_DELAY AM_WEB_PROPERTY_PREFIX  "no_child_thread_activation_delay"

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
 * Auth-Type identifier for OpenSSO authentication.
 */
#define	AM_WEB_AUTH_TYPE_VALUE		"DSAME"

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
    AM_WEB_REQUEST_PROPPATCH
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

} am_web_request_func_t;


/**
 *                   -------- policy methods --------
 */

/**
 * Method to initialize the Agent Toolkit.
 */
AM_WEB_EXPORT am_status_t am_web_init(const char *config_file);

/**
 * Method to clean up the Agent Toolkit
 */
AM_WEB_EXPORT am_status_t am_web_cleanup();

/*
 * Evaluates the access control policies for a specified web-resource and
 * action.
 *
 * Parameters:
 *   sso_token	The sso_token from the OpenSSO cookie.  This
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
			 am_policy_result_t *result);

/*
 * Determines whether the request contains is an OpenSSO
 * notification message intended for the policy SDK.
 */
AM_WEB_EXPORT boolean_t am_web_is_notification(const char *request_url);

/*
 * Returns true if the URL being accessed by the user is in the not
 * enforced list.
 */
AM_WEB_EXPORT boolean_t am_web_is_in_not_enforced_list(const char *url,
						       const char *path_info);

/*
 * Returns true if the given IP address is present in the list of
 * not enforced IP addresses.
 */
AM_WEB_EXPORT boolean_t am_web_is_in_not_enforced_ip_list(const char *ip);

/*
 * Returns if the requested URL is a Valid FQDN resource.
 */
AM_WEB_EXPORT boolean_t am_web_is_valid_fqdn_url(const char *url);


/*
 * Handles notification data received by an agent.  This code handles
 * generating logging messages for the event and any error that may
 * occur during the processing of the notification.
 */
AM_WEB_EXPORT void am_web_handle_notification(const char *data,
					    size_t data_length);

/*
 * This function returns a string representing the URL for redirection that
 * is appropriate to the provided status code and advice map returned by
 * the Policy SDK.  This may either redirect the user to the login URL or
 * the access denied URL.  If the redirection is to the login URL then the
 * URL will include any exsisting information specified in the URL from the
 * configuration file, like org value etc., followed by the specified goto
 * parameter value, which will be used by OpenSSO after the user has
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
					  	     char ** redirect_url);

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
              void **args);

/*
 * This function the reset_cookie headers for the cookies
 * specified in the configuration file and invokes the  set action
 * that caller (i.e. the agent) passes in for each of them.
 */
AM_WEB_EXPORT am_status_t
am_web_do_cookies_reset(am_status_t (*setFunc)(const char *, void **),
			void **args);


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
			    void **args, const char *cookie);


/*
 * This function is used to get the cookie sent in the SAML assertion
 * from the OpenSSO
 */

AM_WEB_EXPORT am_status_t
am_web_get_token_from_assertion(char *assertion, char **token);

AM_WEB_EXPORT am_status_t
am_web_remove_parameter_from_query(const char* inpString, const char *remove_str, char **outString );

AM_WEB_EXPORT am_status_t
am_web_get_parameter_value(const char *inpQuery, const char *param_name, char **param_value);

AM_WEB_EXPORT boolean_t
am_web_is_cdsso_enabled();

AM_WEB_EXPORT am_status_t am_web_check_cookie_in_post(
		void ** args, char ** dpro_cookie,
		char ** request_url,
		char **orig_req, char *method,
		char *response,
		boolean_t responseIsCookie,
		am_status_t (*set_cookie)(const char *, void **),
		void (*set_method)(void **, char *)
		);
AM_WEB_EXPORT am_status_t am_web_check_cookie_in_query(
		void **args, char **dpro_cookie,
		const char *query, char **request_url,
		char ** orig_req, char *method,
		am_status_t (*set_cookie)(const char *, void **),
		void (*set_method)(void **, char *)
		);




/*
 * Free memory previously allocated by a am_web_* routine.
 */
AM_WEB_EXPORT void am_web_free_memory(void *memory);

/*
 * Method to retrieve the name of the OpenSSO cookie.
 */

AM_WEB_EXPORT const char *am_web_get_cookie_name();

/*
 * Method to retrieve the name of the OpenSSO notification Url.
 */
AM_WEB_EXPORT const char *am_web_get_notification_url();


/*
 * Method to retrieve the name of the Agent Server Host.
 */
AM_WEB_EXPORT const char *am_web_get_agent_server_host();

/*
 * Method to retrieve the name of the Agent Server Port.
 */
AM_WEB_EXPORT int am_web_get_agent_server_port();


/**
 *                        -------- logging --------
 */

AM_WEB_EXPORT boolean_t am_web_is_debug_on();
AM_WEB_EXPORT boolean_t am_web_is_max_debug_on();

AM_WEB_EXPORT void am_web_log_always(const char *fmt, ...);
AM_WEB_EXPORT boolean_t am_web_log_auth(am_web_access_t access_type, const char *fmt, ...);
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
AM_WEB_EXPORT boolean_t am_web_is_postpreserve_enabled();

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
						const am_web_postcache_data_t *value);

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
			am_web_postcache_data_t *postdata_entry);

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
AM_WEB_EXPORT void am_web_postcache_remove(const char *key);


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
AM_WEB_EXPORT post_urls_t * am_web_create_post_preserve_urls(const
							     char *request_url);

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
					     const char *actionurl);

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
                            void **args);



/*
 * Returns true if url is a logout url, false otherwise.
 */
AM_WEB_EXPORT boolean_t am_web_is_logout_url(const char *url);


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
		       am_status_t *render_sts);


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
AM_WEB_EXPORT const char *am_web_get_authType();

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
		       char **req_url);

/*
 * Method to determine if the override_host_port is set
 * for the Proxy agent
 */
AM_WEB_EXPORT boolean_t am_web_is_proxy_override_host_port_set();

/*
 * Method to determine the version number of AM with which the agent is
 * interacting
 */
AM_WEB_EXPORT char * am_web_get_am_revision_number();

/*
 * Method to get the value of user id param
 */
AM_WEB_EXPORT const char * am_web_get_user_id_param();
AM_WEB_EXPORT void am_web_clear_attributes_map(am_policy_result_t *result);

AM_WEB_EXPORT boolean_t am_web_is_owa_enabled();
AM_WEB_EXPORT boolean_t am_web_is_owa_enabled_change_protocol();
AM_WEB_EXPORT const char * am_web_is_owa_enabled_session_timeout_url();

AM_END_EXTERN_C

#endif	/* not AM_WEB_H */
