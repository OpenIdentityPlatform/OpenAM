/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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

#include <apr.h>
#include <apr_strings.h>

#include "am_web.h"

#define  DSAME   "DSAME"
#define  OpenSSO   "OpenSSO"								

boolean_t agentInitialized = B_FALSE;

/* Mutex variable */
static apr_thread_mutex_t *init_mutex = NULL;

typedef struct {
    char *properties_file;
    char *bootstrap_file;
} agent_config_rec_t;

module AP_MODULE_DECLARE_DATA dsame_module;

static const char *set_properties_file(
        cmd_parms *, agent_config_rec_t *, const char *);

static const char *set_bootstrap_file(
        cmd_parms *, agent_config_rec_t *, const char *);

static agent_config_rec_t agent_config;

static const command_rec dsame_auth_cmds[] = {
    AP_INIT_TAKE1("Agent_Config_File", set_properties_file, NULL, RSRC_CONF,
    "Full path of the Agent configuration file"),
    // Tag directive Agent_Bootstrap_File should be set in the dsame.conf file
    // -- For this appropriate changes need to be done in the apache install
    // webagents/install/apache/source/com/sun/identity/agents/install/apache
    // files (java files) particularly ConfigureDsameFileTask.java file and
    // and any other related files.
    AP_INIT_TAKE1("Agent_Bootstrap_File", set_bootstrap_file, NULL, RSRC_CONF,
    "Full path of the Agent bootstrap file"), {
        NULL}
};

/*
 * The next group of routines are used to capture the path to the
 * OpenSSOAgentBootstrap.properties file and the directory where the shared libraries
 * needed by the DSAME agent are stored during module configuration.
 */
static const char *set_properties_file(cmd_parms *cmd,
        agent_config_rec_t *config_rec_ptr,
        const char *arg) {
    config_rec_ptr->properties_file = apr_pstrdup(cmd->pool, arg);
    return NULL;
}

static const char *set_bootstrap_file(cmd_parms *cmd,
        agent_config_rec_t *config_rec_ptr,
        const char *arg) {
    config_rec_ptr->bootstrap_file = apr_pstrdup(cmd->pool, arg);
    return NULL;
}

static void *create_agent_config_rec(apr_pool_t *pool_ptr, char *dir_name) {
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
static int init_dsame(apr_pool_t *pconf, apr_pool_t *plog,
        apr_pool_t *ptemp, server_rec *server_ptr) {
    void *lib_handle;
    int requestResult = HTTP_FORBIDDEN;
    int ret = OK;
    am_status_t status = AM_SUCCESS;

#if defined(WINNT)
    LoadLibrary("libnspr4.dll");
    LoadLibrary("libamapc22.dll");
#endif

#if defined(LINUX) 					
    lib_handle = dlopen("libamapc22.so", RTLD_LAZY);
    if (!lib_handle) {
        fprintf(stderr, "Error during dlopen(): %s\n", dlerror());
        exit(1);
    }
#endif
    status = am_web_init(agent_config.bootstrap_file, agent_config.properties_file);

    if (status == AM_SUCCESS) {
        am_web_log_debug("Process initialization result:%s",
                am_status_to_string(status));


        if ((apr_thread_mutex_create(&init_mutex, APR_THREAD_MUTEX_UNNESTED,
                pconf)) != APR_SUCCESS) {
            ap_log_error(__FILE__, __LINE__, APLOG_ALERT, 0, server_ptr,
                    "Policy web agent configuration failed: %s",
                    am_status_to_string(status));
            ret = HTTP_BAD_REQUEST;
        }
    }
    return ret;
}

static apr_status_t dummy_cleanup_func(void *data) {
    // this func intentionally left blank
    return OK;
}

// This is used to hold SIGTERM while agent is cleaning up
// and released when done.
static int sigterm_delivered = 0;

static void sigterm_handler(int sig) {
    // remember that a SIGTERM was delivered so we can raise it later.
    sigterm_delivered = 1;
}

static apr_status_t cleanup_dsame(void *data) {
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
    if (init_mutex) {
        apr_thread_mutex_destroy(init_mutex);
        am_web_log_info("Destroyed mutex...");
        init_mutex = NULL;
    }

    (void) am_web_cleanup();

    // release SIGTERM
    (void) signal(SIGTERM, prev_handler);
    if (sigterm_delivered) {
        raise(SIGTERM);
    }
    return OK;
}

/*
 * Called in the child_init hook in apache 2, this function is needed to
 * register the cleanup_dsame routine to be called upon the child's exit.
 * Registration is using apr_pool_cleanup_register(), which replaces the
 * child_exit hook in apache 1.3.x.
 * Note that init_dsame in apache 2 is called in the post_config hook
 * instead of the child_init hook. See comments in register_hooks().
 */
static void apache2_child_init(apr_pool_t *pool_ptr, server_rec *server_ptr) {
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

static am_status_t
render_result(void **args, am_web_result_t http_result, char *data) {
    request_rec *r = NULL;
    const char *thisfunc = "render_result()";
    int *apache_ret = NULL;
    am_status_t sts = AM_SUCCESS;
    core_dir_config *conf;
    int len = 0;

    if (args == NULL || (r = (request_rec *) args[0]) == NULL,
            (apache_ret = (int *) args[1]) == NULL ||
            ((http_result == AM_WEB_RESULT_OK_DONE ||
            http_result == AM_WEB_RESULT_REDIRECT) &&
            (data == NULL || *data == '\0'))) {
        am_web_log_error("%s: invalid arguments received.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        // only redirect and OK-DONE need special handling.
        // ok, forbidden and internal error can just set in the result.
        switch (http_result) {
            case AM_WEB_RESULT_OK:
                *apache_ret = OK;
                break;
            case AM_WEB_RESULT_OK_DONE:
                if (data && ((len = strlen(data)) > 0)) {
                    ap_set_content_type(r, "text/html");
                    ap_set_content_length(r, len);
                    ap_rwrite(data, len, r);
                    ap_rflush(r);
                    *apache_ret = DONE;
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
static am_status_t
get_request_url(request_rec *r, char **requestURL)
{
    const char *thisfunc = "get_request_url()";
    am_status_t status = AM_SUCCESS;
    const char *args = r->args;
    char *server_name = NULL;
    unsigned int port_num = 0;
    const char *host = NULL;
    char *http_method = NULL;
    char port_num_str[40];
    char *args_sep_str = NULL;

    // Get the host name
    if (host != NULL) {
        size_t server_name_len = 0;
        char *colon_ptr = strchr(host, ':');
        am_web_log_max_debug("%s: Host: %s", thisfunc, host);
        if (colon_ptr != NULL) {
            sscanf(colon_ptr + 1, "%u", &port_num);
            server_name_len = colon_ptr - host;
        } else {
            server_name_len = strlen(host);
        }
        server_name = apr_pcalloc(r->pool, server_name_len + 1);
        memcpy(server_name, host, server_name_len);
        server_name[server_name_len] = '\0';
    } else {
        server_name = (char *) r->hostname;
    }

    // In case of virtual servers with only a
    // IP address, use hostname defined in server_req
    // for the request hostname value
    if (server_name == NULL) {
        server_name = (char *) r->server->server_hostname;
        am_web_log_debug("%s: Host set to server hostname %s.",
                         thisfunc, server_name);
    }
    if (server_name == NULL || strlen(server_name) == 0) {
        am_web_log_error("%s: Could not get the hostname.", thisfunc);
        status = AM_FAILURE;
    } else {
        am_web_log_debug("%s: hostname = %s", thisfunc, server_name);
    }
    if (status == AM_SUCCESS) {
        // Get the port
        if (port_num == 0) {
            port_num = r->server->port;
        }
        // Virtual servers set the port to 0 when listening on the default port.
        // This creates problems, so set it back to default port
        if (port_num == 0) {
            port_num = ap_default_port(r);
            am_web_log_debug("%s: Port is 0. Set to default port %u.",
                thisfunc, ap_default_port(r));
        }
    }
    am_web_log_debug("%s: port = %u", thisfunc, port_num);
    sprintf(port_num_str, ":%u", port_num);
    // Get the protocol
    http_method = (char *) ap_http_scheme(r);
    // Get the query
    if (NULL == args || '\0' == args[0]) {
        args_sep_str = "";
        args = "";
    } else {
        args_sep_str = "?";
    }
    am_web_log_debug("%s: query = %s", thisfunc, args);

    // Construct the url
    // <method>:<host><:port or nothing><uri><? or nothing><args or nothing>
    *requestURL = apr_psprintf(r->pool, "%s://%s%s%s%s%s",
                               http_method,
                               server_name,
                               port_num_str,
                               r->uri,
                               args_sep_str,
                               args);

    am_web_log_debug("%s: Returning request URL = %s.", thisfunc, *requestURL);
    return status;
}

/**
 * gets content if this is notification.
 */
static am_status_t
content_read(void **args, char **rbuf) {
    const char *thisfunc = "content_read()";
    request_rec *r = NULL;
    int rc = 0;
    int rsize = 0, len_read = 0, rpos = 0;
    int sts = AM_FAILURE;
    const char *new_clen_val = NULL;

    if (args == NULL || (r = (request_rec *) args[0]) == NULL || rbuf == NULL) {
        am_web_log_error("%s: invalid arguments passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if ((rc = ap_setup_client_block(r, REQUEST_CHUNKED_ERROR)) != OK) {
        am_web_log_error("%s: error setup client block: %d", thisfunc, rc);
        sts = AM_FAILURE;
    } else if (ap_should_client_block(r)) {
        char argsbuffer[HUGE_STRING_LEN];
        long length = r->remaining;
        *rbuf = apr_pcalloc(r->pool, length + 1);
        while ((len_read = ap_get_client_block(r, argsbuffer,
                sizeof (argsbuffer))) > 0) {
            if ((rpos + len_read) > length) {
                rsize = length - rpos;
            } else {
                rsize = len_read;
            }
            memcpy((char*) * rbuf + rpos, argsbuffer, rsize);
            rpos = rpos + rsize;
        }
        am_web_log_debug("%s: Read %d bytes", thisfunc, rpos);
        sts = AM_SUCCESS;
    }

    // Remove the content length since the body has been read.
    // If the content length is not reset, servlet containers think
    // the request is a POST.
    if (sts == AM_SUCCESS) {
        r->clength = 0;
        apr_table_unset(r->headers_in, "Content-Length");
        new_clen_val = apr_table_get(r->headers_in, "Content-Length");
        am_web_log_max_debug("content_read(): New value "
                "of content length after reset: %s",
                new_clen_val ? "(NULL)" : new_clen_val);
    }
    return sts;
}

static am_status_t
set_header_in_request(void **args, const char *key, const char *values) {
    const char *thisfunc = "set_header_in_request()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;
    if (args == NULL || (r = (request_rec *) args[0]) == NULL ||
            key == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        // remove all instances of the header first.
        apr_table_unset(r->headers_in, key);
        if (values != NULL && *values != '\0') {
            apr_table_set(r->headers_in, key, values);
        }
        sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t
add_header_in_response(void **args, const char *key, const char *values) {
    const char *thisfunc = "add_header_in_response()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;
    if (args == NULL || (r = (request_rec *) args[0]) == NULL ||
            key == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        apr_table_add(r->headers_out, key, values);
        apr_table_add(r->err_headers_out, key, values);
        sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t
set_method(void **args, am_web_req_method_t method) {
    const char *thisfunc = "set_method()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;

    if (args == NULL || (r = (request_rec *) args[0]) == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        switch (method) {
            case AM_WEB_REQUEST_GET:
                r->method_number = M_GET;
                r->method = REQUEST_METHOD_GET;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_POST:
                r->method_number = M_POST;
                r->method = REQUEST_METHOD_POST;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_PUT:
                r->method_number = M_PUT;
                r->method = REQUEST_METHOD_PUT;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_DELETE:
                r->method_number = M_DELETE;
                r->method = REQUEST_METHOD_DELETE;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_OPTIONS:
                r->method_number = M_OPTIONS;
                r->method = REQUEST_METHOD_OPTIONS;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_CONNECT:
                r->method_number = M_CONNECT;
                r->method = REQUEST_METHOD_CONNECT;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_COPY:
                r->method_number = M_COPY;
                r->method = REQUEST_METHOD_COPY;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_INVALID:
                r->method_number = M_INVALID;
                r->method = REQUEST_METHOD_INVALID;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_LOCK:
                r->method_number = M_LOCK;
                r->method = REQUEST_METHOD_LOCK;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_UNLOCK:
                r->method_number = M_UNLOCK;
                r->method = REQUEST_METHOD_UNLOCK;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_MOVE:
                r->method_number = M_MOVE;
                r->method = REQUEST_METHOD_MOVE;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_PATCH:
                r->method_number = M_PATCH;
                r->method = REQUEST_METHOD_PATCH;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_PROPFIND:
                r->method_number = M_PROPFIND;
                r->method = REQUEST_METHOD_PROPFIND;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_PROPPATCH:
                r->method_number = M_PROPPATCH;
                r->method = REQUEST_METHOD_PROPPATCH;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_VERSION_CONTROL:
                r->method_number = M_VERSION_CONTROL;
                r->method = REQUEST_METHOD_VERSION_CONTROL;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_CHECKOUT:
                r->method_number = M_CHECKOUT;
                r->method = REQUEST_METHOD_CHECKOUT;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_UNCHECKOUT:
                r->method_number = M_UNCHECKOUT;
                r->method = REQUEST_METHOD_UNCHECKOUT;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_CHECKIN:
                r->method_number = M_CHECKIN;
                r->method = REQUEST_METHOD_CHECKIN;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_UPDATE:
                r->method_number = M_UPDATE;
                r->method = REQUEST_METHOD_UPDATE;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_LABEL:
                r->method_number = M_LABEL;
                r->method = REQUEST_METHOD_LABEL;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_REPORT:
                r->method_number = M_REPORT;
                r->method = REQUEST_METHOD_REPORT;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_MKWORKSPACE:
                r->method_number = M_MKWORKSPACE;
                r->method = REQUEST_METHOD_MKWORKSPACE;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_MKACTIVITY:
                r->method_number = M_MKACTIVITY;
                r->method = REQUEST_METHOD_MKACTIVITY;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_BASELINE_CONTROL:
                r->method_number = M_BASELINE_CONTROL;
                r->method = REQUEST_METHOD_BASELINE_CONTROL;
                sts = AM_SUCCESS;
                break;
            case AM_WEB_REQUEST_MERGE:
                r->method_number = M_MERGE;
                r->method = REQUEST_METHOD_MERGE;
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
set_user(void **args, const char *user) {
    const char *thisfunc = "set_user()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;

    if (args == NULL || (r = (request_rec *) args[0]) == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        if (user == NULL) {
            user = "";
        }
        r->user = apr_pstrdup(r->pool, user);
        r->ap_auth_type = apr_pstrdup(r->pool, OpenSSO);
        am_web_log_debug("%s: user set to %s", thisfunc, user);
        sts = AM_SUCCESS;
    }
    return sts;
}

static int
get_apache_method_num(am_web_req_method_t am_num) {
    int apache_num = -1;
    switch (am_num) {
        case AM_WEB_REQUEST_GET:
            apache_num = M_GET;
            break;
        case AM_WEB_REQUEST_POST:
            apache_num = M_POST;
            break;
        case AM_WEB_REQUEST_PUT:
            apache_num = M_PUT;
            break;
        case AM_WEB_REQUEST_DELETE:
            apache_num = M_DELETE;
            break;
        case AM_WEB_REQUEST_TRACE:
            apache_num = M_TRACE;
            break;
        case AM_WEB_REQUEST_OPTIONS:
            apache_num = M_OPTIONS;
            break;
        case AM_WEB_REQUEST_CONNECT:
            apache_num = M_CONNECT;
            break;
        case AM_WEB_REQUEST_COPY:
            apache_num = M_COPY;
            break;
        case AM_WEB_REQUEST_INVALID:
            apache_num = M_INVALID;
            break;
        case AM_WEB_REQUEST_LOCK:
            apache_num = M_LOCK;
            break;
        case AM_WEB_REQUEST_UNLOCK:
            apache_num = M_UNLOCK;
            break;
        case AM_WEB_REQUEST_MOVE:
            apache_num = M_MOVE;
            break;
        case AM_WEB_REQUEST_MKCOL:
            apache_num = M_MKCOL;
            break;
        case AM_WEB_REQUEST_PATCH:
            apache_num = M_PATCH;
            break;
        case AM_WEB_REQUEST_PROPFIND:
            apache_num = M_PROPFIND;
            break;
        case AM_WEB_REQUEST_PROPPATCH:
            apache_num = M_PROPPATCH;
            break;
        case AM_WEB_REQUEST_VERSION_CONTROL:
            apache_num = M_VERSION_CONTROL;
            break;
        case AM_WEB_REQUEST_CHECKOUT:
            apache_num = M_CHECKOUT;
            break;
        case AM_WEB_REQUEST_UNCHECKOUT:
            apache_num = M_UNCHECKOUT;
            break;
        case AM_WEB_REQUEST_CHECKIN:
            apache_num = M_CHECKIN;
            break;
        case AM_WEB_REQUEST_UPDATE:
            apache_num = M_UPDATE;
            break;
        case AM_WEB_REQUEST_LABEL:
            apache_num = M_LABEL;
            break;
        case AM_WEB_REQUEST_REPORT:
            apache_num = M_REPORT;
            break;
        case AM_WEB_REQUEST_MKWORKSPACE:
            apache_num = M_MKWORKSPACE;
            break;
        case AM_WEB_REQUEST_MKACTIVITY:
            apache_num = M_MKACTIVITY;
            break;
        case AM_WEB_REQUEST_BASELINE_CONTROL:
            apache_num = M_BASELINE_CONTROL;
            break;
        case AM_WEB_REQUEST_MERGE:
            apache_num = M_MERGE;
            break;
    }
    return apache_num;
}

static am_web_req_method_t
get_method_num(request_rec *r) {
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
        case M_VERSION_CONTROL:
            method_num = AM_WEB_REQUEST_VERSION_CONTROL;
            break;
        case M_CHECKOUT:
            method_num = AM_WEB_REQUEST_CHECKOUT;
            break;
        case M_UNCHECKOUT:
            method_num = AM_WEB_REQUEST_UNCHECKOUT;
            break;
        case M_CHECKIN:
            method_num = AM_WEB_REQUEST_CHECKIN;
            break;
        case M_UPDATE:
            method_num = AM_WEB_REQUEST_UPDATE;
            break;
        case M_LABEL:
            method_num = AM_WEB_REQUEST_LABEL;
            break;
        case M_REPORT:
            method_num = AM_WEB_REQUEST_REPORT;
            break;
        case M_MKWORKSPACE:
            method_num = AM_WEB_REQUEST_MKWORKSPACE;
            break;
        case M_MKACTIVITY:
            method_num = AM_WEB_REQUEST_MKACTIVITY;
            break;
        case M_BASELINE_CONTROL:
            method_num = AM_WEB_REQUEST_BASELINE_CONTROL;
            break;
        case M_MERGE:
            method_num = AM_WEB_REQUEST_MERGE;
            break;
        default:
            method_num = AM_WEB_REQUEST_UNKNOWN;
            break;
    }
    am_web_log_debug("%s: Method string is %s", thisfunc, r->method);
    am_web_log_debug("%s: Apache method number corresponds to %s method",
            thisfunc, am_web_method_num_to_str(method_num));

    // Check if method number and method string correspond
    if (method_num == AM_WEB_REQUEST_UNKNOWN) {
        // If method string is not null, set the correct method number
        if (r->method != NULL && *(r->method) != '\0') {
            method_num = am_web_method_str_to_num(r->method);
            r->method_number = get_apache_method_num(method_num);
            am_web_log_debug("%s: Set method number to correspond to %s method",
                    thisfunc, r->method);
        }
    } else if (strcasecmp(r->method, am_web_method_num_to_str(method_num))
            && (method_num != AM_WEB_REQUEST_INVALID)) {
        // If the method number and the method string do not match,
        // correct the method string. But if the method number is invalid
        // the method string needs to be preserved in case Apache is
        // used as a proxy (in front of Exchange Server for instance)
        r->method = am_web_method_num_to_str(method_num);
        am_web_log_debug("%s: Set method string to %s", thisfunc, r->method);
    }
    return method_num;
}

/**
 * This function is invoked to initialize the agent
 * during the first request.
 */
void init_at_request() {
    am_status_t status;
    status = am_agent_init(&agentInitialized);
    if (status != AM_SUCCESS) {
        am_web_log_debug("Initialization of the agent failed: "
                "status = %s (%d)", am_status_to_string(status), status);
    }
}


/**
 * Deny the access in case the agent is found uninitialized
 */
static int do_deny(request_rec *r, am_status_t status) {
    int retVal = HTTP_FORBIDDEN;
    /* Set the return code 403 Forbidden */
    r->content_type = "text/plain";
    ap_custom_response(r, HTTP_FORBIDDEN,
            "Access denied as Agent profile not"
            " found in Access Manager.");
    am_web_log_info("do_deny() Status code= %s.",
            am_status_to_string(status));
    return retVal;
}

/**
 * Grant access depending on policy and session evaluation
 */
int dsame_check_access(request_rec *r) {
    const char *thisfunc = "dsame_check_access()";
    am_status_t status = AM_SUCCESS;
    int ret = OK;
    char *url = NULL;
    void *args[] = {(void *) r, (void *) & ret};
    const char *clientIP_hdr_name = NULL;
    char *clientIP_hdr = NULL;
    char *clientIP = NULL;
    const char *clientHostname_hdr_name = NULL;
    char *clientHostname_hdr = NULL;
    char *clientHostname = NULL;
    am_web_req_method_t method;
    am_web_request_params_t req_params;
    am_web_request_func_t req_func;
    void* agent_config = NULL;

    memset((void *) & req_params, 0, sizeof (req_params));
    memset((void *) & req_func, 0, sizeof (req_func));

    /**
     * Initialize the agent during first request
     * Should not be repeated during subsequest requests.
     */
    if (agentInitialized != B_TRUE) {
        apr_thread_mutex_lock(init_mutex);
        am_web_log_info("%s: Locked initialization section.", thisfunc);
        if (agentInitialized != B_TRUE) {
            (void) init_at_request();
            if (agentInitialized != B_TRUE) {
                ret = do_deny(r, status);
                status = AM_FAILURE;
            } 
        }
    }    
    if (status == AM_SUCCESS) {
        apr_thread_mutex_unlock(init_mutex);
        am_web_log_info("%s: Unlocked initialization section.", thisfunc);
        // Get the agent config
        agent_config = am_web_get_agent_configuration();
        // Check request
        if (r == NULL) {
            am_web_log_error("%s: Request to http server is NULL!", thisfunc);
            status = AM_FAILURE;
        }
    }
    // Check arguments
    if (r == NULL) {
        am_web_log_error("%s: Request to http server is NULL.", thisfunc);
        status = AM_FAILURE;
    }
    if (status == AM_SUCCESS) {
        if (r->connection == NULL) {
            am_web_log_error("%s: Request connection is NULL.", thisfunc);
            status = AM_FAILURE;
        }
    }
    // Get the request URL
    if (status == AM_SUCCESS) {
        status = get_request_url(r, &url);
    }
    // Get the request method
    if (status == AM_SUCCESS) {
        method = get_method_num(r);
        if (method == AM_WEB_REQUEST_UNKNOWN) {
            am_web_log_error("%s: Request method is unknown.", thisfunc);
            status = AM_FAILURE;
        } 
    }
     
    // If there is a proxy in front of the agent, the user can set in the
    // properties file the name of the headers that the proxy uses to set
    // the real client IP and host name. In that case the agent needs
    // to use the value of these headers to process the request
    if (status == AM_SUCCESS) {
        // Get the client IP address header set by the proxy, if there is one
        clientIP_hdr_name = am_web_get_client_ip_header_name(agent_config);
        if (clientIP_hdr_name != NULL) {
            clientIP_hdr = (char *)apr_table_get(r->headers_in, 
                                                 clientIP_hdr_name);
        }
        // Get the client host name header set by the proxy, if there is one
        clientHostname_hdr_name = 
                 am_web_get_client_hostname_header_name(agent_config);
        if (clientHostname_hdr_name != NULL) {
            clientHostname_hdr = (char *)apr_table_get(r->headers_in, 
                                                 clientHostname_hdr_name);
        }
        // If the client IP and host name headers contain more than one
        // value, take the first value.
        if ((clientIP_hdr != NULL && strlen(clientIP_hdr) > 0) ||
            (clientHostname_hdr != NULL && strlen(clientHostname_hdr) > 0))
        {
            status = am_web_get_client_ip_host(clientIP_hdr,
                                               clientHostname_hdr,
                                               &clientIP, &clientHostname);
        }
    }
    // Set the client ip in the request parameters structure
    if (status == AM_SUCCESS) {
        if (clientIP == NULL) {
            req_params.client_ip = (char *)r->connection->remote_ip;
        } else {
            req_params.client_ip = clientIP;
        }
        if ((req_params.client_ip == NULL) ||
            (strlen(req_params.client_ip) == 0))
        {
            am_web_log_error("%s: Could not get the remote IP.", thisfunc);
            status = AM_FAILURE;
        }
    }

    // Process the request
    if (status == AM_SUCCESS) {
        req_params.client_hostname = clientHostname;
        req_params.url = url;
        req_params.query = r->args;
        req_params.method = method;
        req_params.path_info = r->path_info;
        req_params.cookie_header_val =
                (char *) apr_table_get(r->headers_in, "Cookie");
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

        (void) am_web_process_request(&req_params, &req_func,
                                      &status, agent_config);
        if (status != AM_SUCCESS) {
            am_web_log_error("%s: Error encountered rendering result %d.",
                    thisfunc, ret);
        }
    }
    // Cleaning
    if(clientIP != NULL) {
        am_web_free_memory(clientIP);
    }
    if(clientHostname != NULL) {
        am_web_free_memory(clientHostname);
    }
    am_web_delete_agent_configuration(agent_config);
    // Failure handling
    if (status == AM_FAILURE) {
        if (ret == OK) {
            ret = HTTP_INTERNAL_SERVER_ERROR;
        }
    }
    return ret;
}

static apr_status_t shutdownNSS(void *data)
{    
    am_status_t status = am_shutdown_nss();
    if (status != AM_SUCCESS) {
       am_web_log_error("shutdownNSS(): Failed to shutdown NSS.");
    }
    return OK;
}

static void register_hooks(apr_pool_t *p) {
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

    // NSS needs to be shutdown when a child process exits
    apr_pool_cleanup_register(p, NULL, shutdownNSS, dummy_cleanup_func);

    // register hook for agent cleanup.
    // A hook for child_init is still needed to register cleanup_dsame
    // to be called upon the child's exit.
    // This takes the place of the child_exit hook in apache 1.3.x.
    ap_hook_child_init(apache2_child_init, NULL, NULL, APR_HOOK_LAST);
}

/*
 * Interface table used by Apache 2.x to interact with this module.
 */
module AP_MODULE_DECLARE_DATA dsame_module ={
    STANDARD20_MODULE_STUFF,
    create_agent_config_rec, /* create per-directory config structures */
    NULL, /* create per-server config structures */
    NULL, /* merge per-server config structures */
    NULL, /* command handlers */
    dsame_auth_cmds, /* handlers */
    register_hooks /* register hooks */
};
