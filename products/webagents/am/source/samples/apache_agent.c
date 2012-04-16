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
 *
 */ 
#include <limits.h>

#include <httpd.h>
#include <http_config.h>
#include <http_core.h> 
#include <http_log.h>
#include <http_main.h>
#include <http_protocol.h> 
#include <http_request.h>

#include "am_web.h"

#define  DSAME   "DSAME"

typedef struct {
    char *properties_file;
} agent_config_rec_t;

extern module dsame_module;

static const char *set_properties_file(
     cmd_parms *, agent_config_rec_t *, const char *);

static agent_config_rec_t agent_config;

static const command_rec dsame_auth_cmds[]=
{
    {
	"Agent_Config_File",
	set_properties_file, NULL, RSRC_CONF, TAKE1,
	"Full path of the Agent configuration file"
    },
    {
	NULL
    }
};


/*
 * The next group of routines are used to capture the path to the
 * OpenSSOAgentBootstrap.properties file and the directory where the shared libraries
 * needed by the DSAME agent are stored during module configuration.
 */
static const char *set_properties_file(cmd_parms *cmd,
				       agent_config_rec_t *config_rec_ptr,
				       const char *arg)
{
    config_rec_ptr->properties_file = ap_pstrdup(cmd->pool, arg);
    return NULL;
}

static void *create_agent_config_rec(pool *pool_ptr, char *dir_name)
{
    return &agent_config;
}

/*
 * This routine is called by the Apache server when the module is first
 * loaded.  It handles loading all of the shared libraries that are needed
 * to instantiate the DSAME Policy Agent.  If all of the libraries can
 * successfully be loaded, then the routine looks up the two entry points
 * in the actual policy agent: init_policy and dsame_check_access.  The
 * first routine is invoked directory and an error is logged if it returns
 * an error.  The second routine is inserted into the module interface
 * table for use by Apache during request processing.
 */
static void init_dsame(server_rec *server_ptr, pool *pool_ptr)
{
    am_web_init(agent_config.properties_file);

/* KWL: NOTE: apache calls init twice. the second call will fail.
so am_web_init_func either have to return a special error code
for already initialized or return success for already initialize.
it cannot return a generic failure because we still need to 
save the dsame_module.access_checker.
*/
}

static int do_redirect(request_rec *request, am_status_t status,
		       const am_map_t advice_map, const char *original_url)
{
    boolean_t allocated;
    char *redirect_url;
    int apache_status;

    redirect_url = am_web_get_redirect_url(status, advice_map, original_url,
                                          &allocated);
    if (redirect_url != NULL) {
	apache_status = HTTP_MOVED_TEMPORARILY;
	am_web_log_debug("do_redirect() policy status = %s, redirection URL "
			"is %s", am_status_to_string(status), redirect_url);

	ap_custom_response(request, apache_status, redirect_url);
	if (B_TRUE==allocated) {
	    am_web_free_memory(redirect_url);
	}
    } else {
	apache_status = HTTP_FORBIDDEN;
	am_web_log_debug("do_redirect() policy status = %s, returning %d",
			am_status_to_string(status), apache_status);
    }

    return apache_status;
}


/**
 * gets request URL
 */
static char *get_request_url(request_rec *r)
{
    const char *args = r->args;

    if ((args != NULL) && (args[0] != '\0')) {
	am_web_log_debug("query args :%s", args);
	return ap_psprintf(r->pool, "%s://%s:%u%s?%s", ap_http_method(r),
	    r->hostname, r->server->port, r->uri, args);
    } else {
	return ap_psprintf(r->pool, "%s://%s:%u%s", ap_http_method(r),
	    r->hostname, r->server->port, r->uri);
    }
}

/**
 *  get DSAME Cookie
 */
static char *get_cookie(request_rec *r, const char *name)
{
    static const char separators[] = { " ;\n\r\t\f" };
    char *cookie;
    char *part;
    char *marker = NULL;
    char *value = NULL;
    char *last_separator;

    if (!(cookie = (char *)ap_table_get(r->headers_in, "Cookie"))) {
	am_web_log_warning("in get_cookie: no cookie in ap_table");
	return NULL;
    }

    part = ap_pstrcat (r->pool, cookie, ";", NULL);
    am_web_log_debug("in get_cookie: part is  %s", part);

    // get the last IS cookie
    for (part = strtok_r(part, separators, &last_separator);
	part != NULL;
	part = strtok_r(NULL, separators, &last_separator))
    {
	while (part && !(marker = strchr(part, '='))) {
	    part = strtok_r(NULL, separators, &last_separator);
	}

	if (++marker) {
	    if (!strcasecmp(ap_getword(r->pool, (const char **)&part, '='),
		name))
	    {
		value = marker;
	    }
	}
    }

    if (value) {
	am_web_log_debug("in get_cookie: return cookie value of %s", value);
    }

    return value;
}

/**
 * gets content if this is notification.
 */
static int content_read(request_rec *r, char **rbuf)
{
    int rc;
    int rsize, len_read, rpos = 0;

    if ((rc = ap_setup_client_block(r, REQUEST_CHUNKED_ERROR)) != OK) {
	return -1;
    }

    if (ap_should_client_block(r)) {
	char argsbuffer[HUGE_STRING_LEN];
	long length = r->remaining;
	*rbuf = ap_pcalloc(r->pool, length+1);
	ap_hard_timeout("content_read", r);

	while ((len_read = ap_get_client_block(r, argsbuffer,
	    sizeof(argsbuffer))) > 0)
	{
	    ap_reset_timeout(r);

	    if ((rpos + len_read) > length) {
		rsize = length -rpos;
	    } else {
		rsize = len_read;
	    }

	    memcpy((char*) *rbuf + rpos, argsbuffer, rsize);
	    rpos = rpos + rsize;
	}

	ap_kill_timeout(r);
    }
    am_web_log_debug("in content_read: rpos=%d", rpos);
    return rpos;
}

static am_status_t set_header(char *key, char *values, void **args)
{
    request_rec *r = (request_rec *)args[0];
    ap_table_add(r->headers_in, key, values);
    return (AM_SUCCESS);
}

/**
 * determines to grant access
 */
int dsame_check_access(request_rec *r)
{
    char *dpro_cookie = get_cookie(r, am_web_get_cookie_name());
    char *url = get_request_url(r);
    char *content;
    am_status_t status = AM_FAILURE;
    int  ret = OK;
    const char *ruser = NULL;
    int bytesRead;
    am_map_t env_parameter_map = NULL;
    am_policy_result_t result = AM_POLICY_RESULT_INITIALIZER;
    void *args[1] = { (void *)r };

    am_web_log_debug("Apache Agent: %s request for URL %s", r->method, url);
    if (B_TRUE==am_web_is_notification(url)) {
        bytesRead = content_read(r, &content);

        am_web_log_max_debug("Apache Agent: Content is %s", content);
        am_web_handle_notification(content, bytesRead);
        ap_rputs("OK",r);
        return OK;
    }

    if (dpro_cookie != NULL){
        am_web_log_debug("Apache Agent: cookie value is: %s", dpro_cookie);
    } else {
        am_web_log_debug("Apache Agent: cookie is null");
    }

    status = am_map_create(&env_parameter_map);
    if( status != AM_SUCCESS) {
        am_web_log_error("Apache Agent: unable to create map, "
                        "status = %s (%d)", am_status_to_string(status),
                        status);
    } else {
        am_web_log_debug("Apache Agent: am_map_create returned status=%s",
                     am_status_to_string(status));
    }

    if(status == AM_SUCCESS) {
	status = am_web_is_access_allowed(dpro_cookie, url, r->method,
					  (char *)r->connection->remote_ip, 
					  env_parameter_map, &result);

	am_map_destroy(env_parameter_map);
    }

    am_web_log_debug("Apache Agent: am_web_is_access_allowed returned status=%s",
                     am_status_to_string(status));

    switch (status) {
    case AM_SUCCESS:
	ruser = result.remote_user;
	if (ruser != NULL) {
	    r->connection->user = ap_pstrdup(r->pool, ruser);
	    r->connection->ap_auth_type = ap_pstrdup(r->pool, DSAME);
	    am_web_log_debug("URL Access Agent: access allowed to %s", ruser);
	} else {
	    am_web_log_error("URL Access Agent: access allowed to unknown "
			    "user");
	}

	/* set LDAP user attributes to http header */
	status = am_web_do_result_attr_map_set(&result, set_header, args);
	if (status != AM_SUCCESS) {
	    am_web_log_error("URL Access Agent: am_web_do_result_attr_map_set "
			    "failed, status = %s (%d)",
			    am_status_to_string(status), status);
	}
	ret = OK;
	break;

    case AM_ACCESS_DENIED:
	am_web_log_always("URL Access Agent: access denied to %s",
			 result.remote_user ? result.remote_user :
			 "unknown user");
	ret = do_redirect(r, status, result.advice_map, url);
	break;

    case AM_INVALID_SESSION:
	/* XXX - Add POST Cache code here. */
	ret = do_redirect(r, status, result.advice_map, url);
	break;

    case AM_INVALID_FQDN_ACCESS:
        // Redirect to self with correct FQDN - no post preservation
        ret = do_redirect(r, status, result.advice_map, url);
        break;

    case AM_INVALID_ARGUMENT:
    case AM_NO_MEMORY:
    default:
	am_web_log_error("URL Access Agent: status: %s (%d)",
			am_status_to_string(status), status);
	ret = SERVER_ERROR;
	break;
    }

    am_policy_result_destroy(&result);

    return ret;
}



/*
 * Interface table used by Apache to interact with this module.
 */
module MODULE_VAR_EXPORT dsame_module =
{
    STANDARD_MODULE_STUFF,
    NULL,			/* initializer */
    create_agent_config_rec,	/* dir config creater */
    NULL,			/* dir merger */
    NULL,			/* server config */
    NULL,			/* merge server config */
    dsame_auth_cmds,		/* command table */
    NULL,			/* handlers */
    NULL,			/* filename translation */
    NULL,			/* check_user_id */
    NULL,			/* check auth */
    dsame_check_access,         /* check access */
    NULL,			/* type_checker */
    NULL,			/* fixups */
    NULL,			/* logger */
    NULL,			/* header parser */
    init_dsame,			/* child_init */
    NULL,			/* child_exit */
    NULL			/* post read-request */
};
