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

#include <limits.h>
#include <signal.h>
#include <errno.h>

#if defined(LINUX)
#include <dlfcn.h>
#endif
#include <httpd.h>
#include <http_config.h>
#include <http_core.h>
#include <http_protocol.h>
#include <http_request.h>
#include <http_main.h>
#include <http_log.h>

#if defined(WINNT)
#include <windows.h>
#include <winbase.h>
#endif
typedef void (*sighandler_t)(int);

#if defined(APACHE2)
#include <apr.h>
#include <apr_strings.h>
#include <apr_compat.h>
#endif
#include "am_web.h"

#define  DSAME   "DSAME"

typedef struct {
    char *properties_file;
} agent_config_rec_t;

#if defined(APACHE2)
module AP_MODULE_DECLARE_DATA dsame_module;
#else
extern module dsame_module;
#endif

static const char *set_properties_file(
     cmd_parms *, agent_config_rec_t *, const char *);

static agent_config_rec_t agent_config;

static const command_rec dsame_auth_cmds[]=
#if defined(APACHE2)
{
    AP_INIT_TAKE1("Agent_Config_File", set_properties_file, NULL, RSRC_CONF,
				  "Full path of the Agent configuration file"),
    {NULL}
};
#else
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
#endif

/*
 * The next group of routines are used to capture the path to the
 * AMAgent.properties file and the directory where the shared libraries
 * needed by the DSAME agent are stored during module configuration.
 */
static const char *set_properties_file(cmd_parms *cmd,
				       agent_config_rec_t *config_rec_ptr,
				       const char *arg)
{
    config_rec_ptr->properties_file = ap_pstrdup(cmd->pool, arg);
    return NULL;
}
#if defined(APACHE2)
static void *create_agent_config_rec(apr_pool_t *pool_ptr, char *dir_name)
#else
static void *create_agent_config_rec(pool *pool_ptr, char *dir_name)
#endif
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
#if defined(APACHE2)
static int init_dsame(apr_pool_t *pconf, apr_pool_t *plog,
		      apr_pool_t *ptemp, server_rec *server_ptr)
#else
static void init_dsame(server_rec *server_ptr, pool *pool_ptr)
#endif
{
	void *lib_handle;
#if defined(APACHE2)
    int ret = OK;
#endif
    am_status_t status = AM_SUCCESS;

#if defined(WINNT)
    LoadLibrary("libnspr4.dll");
#if defined(APACHE2)
    LoadLibrary("libamapc2.dll");
#endif
#endif

#if defined(LINUX) && defined(APACHE2)
    lib_handle = dlopen("libamapc2.so", RTLD_LAZY);
	if (!lib_handle) {
		fprintf(stderr, "Error during dlopen(): %s\n", dlerror());
		exit(1);
	}
#endif

    status = am_web_init(agent_config.properties_file);
    if(status == AM_SUCCESS) {
	am_web_log_debug("Process initialization result:%s",
			 am_status_to_string(status));
    } else {
	am_web_log_error("Process initialization failure:%s",
			 am_status_to_string(status));
#if defined(APACHE2)
        ap_log_error(__FILE__, __LINE__, APLOG_ALERT, 0, server_ptr,
                     "Policy web agent configuration failed: %s",
                     am_status_to_string(status));
#else
        ap_log_error(__FILE__, __LINE__, APLOG_ALERT, server_ptr,
                     "Policy web agent configuration failed: %s",
                     am_status_to_string(status));
#endif
    }

#if defined(APACHE2)
    if (status != AM_SUCCESS) {
        // anything besides OK or DECLINED is an error
        ret = HTTP_BAD_REQUEST;
    }
    return ret;

#endif
}


#if defined(APACHE2)
static apr_status_t dummy_cleanup_func(void *data)
{
    // this func intentionally left blank
    return OK;
}
#endif

// This is used to hold SIGTERM while agent is cleaning up
// and released when done.
static int sigterm_delivered = 0;
static void sigterm_handler(int sig)
{
    // remember that a SIGTERM was delivered so we can raise it later.
    sigterm_delivered = 1;
}

#if defined(APACHE2)
static apr_status_t cleanup_dsame(void *data)
#else
static void cleanup_dsame(server_rec *server_ptr, pool *pool_ptr)
#endif
{
    /*
     * Apache calls the cleanup func then sends a SIGTERM before
     * the routine finishes. so hold SIGTERM to let destroy agent session
     * complete and release it after.
     * The signal() interface (ANSI C) is chosen to hold the signal instead of
     * sigaction() or other signal handling interfaces since it seems
     * to work best across platforms.
     */
    sighandler_t prev_handler = signal(SIGTERM, sigterm_handler);

    am_web_log_info("Cleaning up web agent..");
    (void)am_web_cleanup();

    // release SIGTERM
    (void)signal(SIGTERM, prev_handler);
    if (sigterm_delivered) {
	raise(SIGTERM);
    }
#if defined(APACHE2)
    return OK;
#endif
}


#if defined(APACHE2)
/*
 * Called in the child_init hook in apache 2, this function is needed to
 * register the cleanup_dsame routine to be called upon the child's exit.
 * Registration is using apr_pool_cleanup_register(), which replaces the
 * child_exit hook in apache 1.3.x.
 * Note that init_dsame in apache 2 is called in the post_config hook
 * instead of the child_init hook. See comments in register_hooks().
 */
static void apache2_child_init(apr_pool_t *pool_ptr, server_rec *server_ptr)
{
    /*
     * The first cleanup func, "plain_cleanup" as it is declared in
     * apache headers, is called when the pool is cleaned up, i.e.
     * when the child spawned by the initial parent exits.
     * This is where we really need to clean up agents.
     * The 2nd cleanup func, "child_cleanup" as it is declared, is
     * called when any child, including one forked from another child
     * to run cgi scripts, exits. We do not need or want to do anything
     * there. A dummy func still needs to be passed, however, or
     * apache crashes on the null pointer.
     */
    apr_pool_cleanup_register(pool_ptr, server_ptr,
			      cleanup_dsame, dummy_cleanup_func);
}
#endif

static am_status_t
render_result(void **args, am_web_result_t http_result, char *data)
{
    request_rec *r = NULL;
    const char *thisfunc = "render_result()";
    int *apache_ret = NULL;
    am_status_t sts = AM_SUCCESS;
    core_dir_config *conf;
	int len = 0;

    char *am_rev_number = am_web_get_am_revision_number();
    if (args == NULL || (r = (request_rec *)args[0]) == NULL,
	(apache_ret = (int *)args[1]) == NULL ||
	((http_result == AM_WEB_RESULT_OK_DONE ||
	    http_result == AM_WEB_RESULT_REDIRECT) &&
		(data == NULL || *data == '\0'))) {
	am_web_log_error("%s: invalid arguments received.", thisfunc);
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	// only redirect and OK-DONE need special handling.
	// ok, forbidden and internal error can just set in the result.
	switch (http_result) {
	case AM_WEB_RESULT_OK:
	    *apache_ret = OK;
	    break;
	case AM_WEB_RESULT_OK_DONE:
	    if ((am_rev_number != NULL) && (!strcmp(am_rev_number, "7.0")) && data && ((len = strlen(data)) > 0))
	    if (data && ((len = strlen(data)) > 0))
		{
#if defined(APACHE2)
                        ap_set_content_type(r, "text/html");
#else
                        r->content_type="text/html";
                        ap_send_http_header(r);
#endif
                        ap_set_content_length(r, len);
			ap_rwrite(data, len, r);
            ap_rflush(r);
            *apache_ret = HTTP_FORBIDDEN;
        } else {
            *apache_ret = OK;
        }
	    break;
	case AM_WEB_RESULT_REDIRECT:
	    // The following lines are added to work around the problem for
	    // Apache Bug 8334 for older apache versions (< 1.3.20).
	    // See http://bugs.apache.org/index.cgi/full/8334 for details
	    if ((conf = ap_get_module_config(
			    r->per_dir_config, &core_module)) != NULL) {
		conf->response_code_strings = NULL;
		ap_set_module_config(r->per_dir_config,
					&core_module, conf);
	    }
	    ap_custom_response(r, HTTP_MOVED_TEMPORARILY, data);
	    *apache_ret = HTTP_MOVED_TEMPORARILY;
	    break;
	case AM_WEB_RESULT_FORBIDDEN:
	    *apache_ret = HTTP_FORBIDDEN;
	    break;
	case AM_WEB_RESULT_ERROR:
	    *apache_ret = HTTP_INTERNAL_SERVER_ERROR;
	    break;
	default:
	    am_web_log_error("%s: Unrecognized process result %d.",
			     thisfunc, http_result);
	    *apache_ret = HTTP_INTERNAL_SERVER_ERROR;
	    break;
	}
	sts = AM_SUCCESS;
    }
    return sts;
}

/**
 * gets request URL
 */
static char *
get_request_url(request_rec *r)
{
    const char *thisfunc = "get_request_url()";
    const char *args = r->args;
    char *server_name = NULL;
    char *retVal = NULL;
    unsigned int port_num = 0;
    const char *host = (const char *)ap_table_get(r->headers_in, "Host");
    char *http_method = NULL;
    char port_num_str[40];
    char *args_sep_str = NULL;

    // get host
    if(host != NULL) {
	size_t server_name_len = 0;
	char *colon_ptr = strchr(host, ':');
	am_web_log_max_debug("%s: Host: %s", thisfunc, host);
	if(colon_ptr != NULL) {
	    sscanf(colon_ptr + 1, "%u", &port_num);
	    server_name_len = colon_ptr - host;
	} else {
	    server_name_len = strlen(host);
	}
	server_name = ap_pcalloc(r->pool, server_name_len + 1);
	memcpy(server_name, host, server_name_len);
	server_name[server_name_len] = '\0';
    } else {
	server_name = (char *)r->hostname;
    }

    // In case of virtual servers with only a
    // IP address, use hostname defined in server_req
    // for the request hostname value
    if (server_name == NULL) {
	server_name = (char *)r->server->server_hostname;
	am_web_log_debug("%s: Host set to server hostname %s.", thisfunc, server_name);
    }

    // get port
    if(port_num == 0) {
    	port_num = r->server->port;
    }
    // Virtual servers set the port to 0 when listening on the default port.
    // This creates problems, so set it back to default port
    if(port_num == 0) {
    	port_num = ap_default_port(r);
    	am_web_log_debug("%s: Port is 0. Set to default port %u.",
    	                  thisfunc, ap_default_port(r));
    }    
    am_web_log_max_debug("%s: Port is %u.", thisfunc, port_num);
    sprintf(port_num_str, ":%u", port_num);

    // get protocol
    http_method = (char *)ap_http_method(r);

    // get query args
    if (NULL == args || '\0' == args[0]) {
	args_sep_str = "";
	args = "";
    }
    else {
	args_sep_str = "?";
    }

    // <method>:<host><:port or nothing><uri><? or nothing><args or nothing>
    retVal = ap_psprintf(r->pool, "%s://%s%s%s%s%s",
	                 http_method,
			 server_name,
			 port_num_str,
			 r->uri,
			 args_sep_str,
			 args);

    am_web_log_debug("%s: Returning request URL %s.", thisfunc, retVal);
    return retVal;
}

/**
 * gets content if this is notification.
 */
static am_status_t
content_read(void **args, char **rbuf)
{
    const char *thisfunc = "content_read()";
    request_rec *r = NULL;
    int rc = 0;
    int rsize = 0, len_read = 0, rpos = 0;
    int sts = AM_FAILURE;
    const char *new_clen_val = NULL;

    if (args == NULL || (r = (request_rec *)args[0]) == NULL || rbuf == NULL) {
	am_web_log_error("%s: invalid arguments passed.", thisfunc);
	sts = AM_INVALID_ARGUMENT;
    }
    else if ((rc = ap_setup_client_block(r, REQUEST_CHUNKED_ERROR)) != OK) {
	am_web_log_error("%s: error setup client block: %d", thisfunc, rc);
	sts = AM_FAILURE;
    }
    else if (ap_should_client_block(r)) {
	char argsbuffer[HUGE_STRING_LEN];
	long length = r->remaining;
	*rbuf = ap_pcalloc(r->pool, length+1);
#if !defined(APACHE2)
	ap_hard_timeout("content_read", r);
#endif
	while ((len_read = ap_get_client_block(r, argsbuffer,
	    sizeof(argsbuffer))) > 0)
	{
#if !defined(APACHE2)
	    ap_reset_timeout(r);
#endif
	    if ((rpos + len_read) > length) {
		rsize = length -rpos;
	    } else {
		rsize = len_read;
	    }
	    memcpy((char*) *rbuf + rpos, argsbuffer, rsize);
	    rpos = rpos + rsize;
	}
	am_web_log_debug("%s: Read %d bytes", thisfunc, rpos);
	sts = AM_SUCCESS;
#if !defined(APACHE2)
	ap_kill_timeout(r);
#endif
    }

    // Remove the content length since the body has been read.
    // If the content length is not reset, servlet containers think
    // the request is a POST.
    if(sts == AM_SUCCESS) {
	r->clength = 0;
	ap_table_unset(r->headers_in, "Content-Length");
	new_clen_val = ap_table_get(r->headers_in, "Content-Length");
	am_web_log_max_debug("content_read(): New value "
			     "of content length after reset: %s",
			     new_clen_val?"(NULL)":new_clen_val);
    }
    return sts;
}

static am_status_t
set_header_in_request(void **args, const char *key, const char *values)
{
    const char *thisfunc = "set_header_in_request()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;
    if (args == NULL || (r = (request_rec *)args[0]) == NULL ||
	key == NULL) {
	am_web_log_error("%s: invalid argument passed.", thisfunc);
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	// remove all instances of the header first.
	ap_table_unset(r->headers_in, key);
	if (values != NULL) {
	    ap_table_set(r->headers_in, key, values);
	}
	sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t
add_header_in_response(void **args, const char *key, const char *values)
{
    const char *thisfunc = "add_header_in_response()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;
    if (args == NULL || (r = (request_rec *)args[0]) == NULL ||
	key == NULL) {
	am_web_log_error("%s: invalid argument passed.", thisfunc);
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	ap_table_add(r->headers_out, key, values);
	ap_table_add(r->err_headers_out, key, values);
	sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t
set_method(void **args, am_web_req_method_t method)
{
    const char *thisfunc = "set_method()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;

    if (args == NULL || (r = (request_rec *)args[0]) == NULL) {
	am_web_log_error("%s: invalid argument passed.", thisfunc);
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	switch (method) {
	case AM_WEB_REQUEST_GET:
	    r->method_number = M_GET;
	    r->method = REQUEST_METHOD_GET;
	    sts = AM_SUCCESS;
	    break;
	case AM_WEB_REQUEST_POST:
	    r->method_number = M_POST;
	    r->method = AM_WEB_REQUEST_POST;
	    sts = AM_SUCCESS;
	    break;
	default:
	    sts = AM_INVALID_ARGUMENT;
	    am_web_log_error("%s: invalid method [%s] passed.",
			     thisfunc, am_web_method_num_to_str(method));
	    break;
	}
    }
    return sts;
}

static am_status_t
set_user(void **args, const char *user)
{
    const char *thisfunc = "set_user()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;

    if (args == NULL || (r = (request_rec *)args[0]) == NULL) {
	am_web_log_error("%s: invalid argument passed.", thisfunc);
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	if (user == NULL) {
	    user = "";
	}
#if defined(APACHE2)
	r->user = ap_pstrdup(r->pool, user);
	r->ap_auth_type = ap_pstrdup(r->pool, DSAME);
#else
	r->connection->user = ap_pstrdup(r->pool, user);
	r->connection->ap_auth_type = ap_pstrdup(r->pool, DSAME);
#endif
	am_web_log_debug("%s: user set to %s", thisfunc, user);
	sts = AM_SUCCESS;
    }
    return sts;
}

static am_web_req_method_t
get_method_num(request_rec *r)
{
    const char *thisfunc = "get_method_num()";
    am_web_req_method_t method_num = AM_WEB_REQUEST_UNKNOWN;
    // get request method from method number first cuz it's
    // faster. if not a recognized method, get it from the method string.
    switch (r->method_number) {
    case M_GET:
	method_num = AM_WEB_REQUEST_GET;
	break;
    case M_POST:
	method_num = AM_WEB_REQUEST_POST;
	break;
    case M_PUT:
	method_num = AM_WEB_REQUEST_PUT;
	break;
    case M_DELETE:
	method_num = AM_WEB_REQUEST_DELETE;
	break;
    case M_TRACE:
	method_num = AM_WEB_REQUEST_TRACE;
	break;
    case M_OPTIONS:
	method_num = AM_WEB_REQUEST_OPTIONS;
	break;
    case M_CONNECT:
	method_num = AM_WEB_REQUEST_CONNECT;
	break;
    case M_COPY:
	method_num = AM_WEB_REQUEST_COPY;
	break;
    case M_INVALID:
	method_num = AM_WEB_REQUEST_INVALID;
	break;
    case M_LOCK:
	method_num = AM_WEB_REQUEST_LOCK;
	break;
    case M_UNLOCK:
	method_num = AM_WEB_REQUEST_UNLOCK;
	break;
    case M_MOVE:
	method_num = AM_WEB_REQUEST_MOVE;
	break;
    case M_MKCOL:
	method_num = AM_WEB_REQUEST_MKCOL;
	break;
    case M_PATCH:
	method_num = AM_WEB_REQUEST_PATCH;
	break;
    case M_PROPFIND:
	method_num = AM_WEB_REQUEST_PROPFIND;
	break;
    case M_PROPPATCH:
	method_num = AM_WEB_REQUEST_PROPPATCH;
	break;
    default:
	method_num = AM_WEB_REQUEST_UNKNOWN;
	break;
    }
    if (r->method_number == AM_WEB_REQUEST_UNKNOWN) {
	if (r->method != NULL && *(r->method) != '\0') {
	    if (!strcmp(r->method, REQUEST_METHOD_GET)) {
		method_num = AM_WEB_REQUEST_GET;
		r->method_number = M_GET;	// fix it so it's consistent.
		am_web_log_warning("%s: Apache request method number did not "
				   "match method string. Setting "
				   "method number to match method string %s.",
				    thisfunc, AM_WEB_REQUEST_GET);
	    }
	    else if (!strcmp(r->method, AM_WEB_REQUEST_POST)) {
		method_num = AM_WEB_REQUEST_POST;
		r->method_number = M_POST;	// fix it so it's consistent.
		am_web_log_warning("%s: Apache request method number did not "
				   "match method string. Setting "
				   "method number to match method string %s.",
				    thisfunc, AM_WEB_REQUEST_POST);
	    }
	}
    }
    // fix it and warn if the apache method number and string didn't match.
    else if (r->method_number == AM_WEB_REQUEST_GET &&
	     strcasecmp(r->method, AM_WEB_REQUEST_GET)) {
	r->method = AM_WEB_REQUEST_GET;
	am_web_log_warning("%s: Apache request method number did not match "
			   "method string. Setting method string to match "
			   "method number %s.", thisfunc, AM_WEB_REQUEST_GET);
    }
    else if (r->method_number == AM_WEB_REQUEST_POST &&
	     strcasecmp(r->method, AM_WEB_REQUEST_POST)) {
	r->method = AM_WEB_REQUEST_POST;
	am_web_log_warning("%s: Apache request method number did not match "
			   "method string. Setting method string to match "
			   "method number %s.", thisfunc, AM_WEB_REQUEST_GET);
    }
    return method_num;
}


/**
 * determines to grant access
 */
int dsame_check_access(request_rec *r)
{
    const char *thisfunc = "dsame_check_access()";
    int  ret = OK;
    char *url = get_request_url(r);
    am_web_req_method_t method = get_method_num(r);
    void *args[] = { (void *)r, (void *)&ret };

    am_web_request_params_t req_params;
    am_web_request_func_t req_func;
    am_status_t render_sts = AM_FAILURE;

    memset((void *)&req_params, 0, sizeof(req_params));
    memset((void *)&req_func, 0, sizeof(req_func));

    if (r == NULL) {
	am_web_log_error("%s: Request to http server is NULL!", thisfunc);
	ret = HTTP_INTERNAL_SERVER_ERROR;
    }
    else if (url == NULL || *url == '\0' || method == AM_WEB_REQUEST_UNKNOWN ||
	     r->connection == NULL || r->connection->remote_ip == NULL ||
	     *(r->connection->remote_ip) == '\0') {
	am_web_log_error("%s: Request to http server had invalid url, "
			 "request method, or client IP.", thisfunc);
	ret = HTTP_INTERNAL_SERVER_ERROR;
    }
    else {
	req_params.url = url;
	req_params.query = r->args;
	req_params.method = method;
	req_params.path_info = r->path_info;
	req_params.client_ip = (char *)r->connection->remote_ip;
	req_params.cookie_header_val =
		    (char *)ap_table_get(r->headers_in, "Cookie");

	req_func.get_post_data.func = content_read;
	req_func.get_post_data.args = args;
	// no free_post_data
	req_func.set_user.func = set_user;
	req_func.set_user.args = args;
	req_func.set_method.func = set_method;
	req_func.set_method.args = args;
	req_func.set_header_in_request.func = set_header_in_request;
	req_func.set_header_in_request.args = args;
	req_func.add_header_in_response.func = add_header_in_response;
	req_func.add_header_in_response.args = args;
	req_func.render_result.func = render_result;
	req_func.render_result.args = args;

	(void)am_web_process_request(&req_params, &req_func, &render_sts);
	if (render_sts != AM_SUCCESS) {
	    am_web_log_error("%s: Error encountered rendering result %d.",
			     thisfunc, ret);
	    ret = HTTP_INTERNAL_SERVER_ERROR;
	}
    }
    return ret;
}

#if defined(APACHE2)
static void register_hooks(apr_pool_t *p)
{
    ap_hook_access_checker(dsame_check_access, NULL, NULL, APR_HOOK_MIDDLE);

    // register hook for agent initialization.
    // In apache 2 the post_config hook is used in place of child_init
    // for initialization.  The post_config hook is called by the
    // apache parent process before any child process is created.
    // The child process then inherits the agent_info initialized by
    // am_web_init() in init_dsame(), and creates its own Service objects
    // with connections to the IS on the first request handled by the child.
    // This saves each child from having to read from the properties file
    // when it is first started.
    ap_hook_post_config(init_dsame, NULL, NULL, APR_HOOK_LAST);

    // register hook for agent cleanup.
    // A hook for child_init is still needed to register cleanup_dsame
    // to be called upon the child's exit.
    // This takes the place of the child_exit hook in apache 1.3.x.
    ap_hook_child_init(apache2_child_init, NULL, NULL, APR_HOOK_LAST);
}
#endif

#if defined(APACHE2)
 /*
 * Interface table used by Apache 2.0 to interact with this module.
  */
module AP_MODULE_DECLARE_DATA dsame_module =
 {
    STANDARD20_MODULE_STUFF,
    create_agent_config_rec,	/* create per-directory config structures */
    NULL,			/* create per-server config structures */
    NULL,			/* merge per-server config structures */
    NULL, 			/* command handlers */
    dsame_auth_cmds,		/* handlers */
    register_hooks		/* register hooks */
 };

#else
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
    cleanup_dsame,		/* child_exit */
    NULL			/* post read-request */
};
#endif
