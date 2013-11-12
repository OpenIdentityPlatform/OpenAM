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
 * $Id: iws_agent.c,v 1.26 2010/03/10 05:08:54 dknab Exp $
 *
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

/*
 * iws_agent.c:
 *
 * This file implements functions required to do session validation and user's
 * URL access check. Some functions are implemented as a NSAPI SAF to be
 * called by NSAPI directives which are defined in obj.conf file. Other
 * supportive funtions need not be NSAPI SAFs.
 *
 * The Server Application Functions(SAF) in this file are PathCheck and Init
 * directive functions
 *
 */

/*
 * Header Files
 */

#include <string.h>
#include <nsapi.h>

#include "am_properties.h"
#include "am_web.h"

#include <stdio.h>
#ifdef _MSC_VER
#include <io.h>
#define R_OK            4
#ifndef access
#define access          _access
#endif
#define snprintf        _snprintf
#else
#include <unistd.h>
#endif

#define AGENT_BOOTSTRAP_FILE "/OpenSSOAgentBootstrap.properties"
#define AGENT_CONFIG_FILE "/OpenSSOAgentConfiguration.properties"
#define DSAME_CONF_DIR "dsameconfdir"

#define	MAGIC_STR		"sunpostpreserve"
#define	POST_PRESERVE_URI	"/dummypost/"MAGIC_STR
#define   EMPTY_STRING	""


typedef struct agent_props {
    am_properties_t agent_bootstrap_props;
    am_properties_t agent_config_props;
} agent_props_t;

static agent_props_t agent_props = {
    AM_PROPERTIES_NULL
};

boolean_t agentInitialized = B_FALSE;
static CRITICAL initLock;
const char getMethod[] = "GET";

void init_at_request();

am_status_t get_header_value(pblock *pb, const char *header_name,
                             boolean_t isRequired, char **header_value,
                             boolean_t needCopy, char **header_value_copy)
{
    const char *thisfunc = "get_header_value()";
    am_status_t status = AM_SUCCESS;
    // From NSAPI guide:
    // The pointer returned is a pointer into the pblock. 
    // Do not FREE it. If you want to modify it, do a STRDUP 
    // and modify the copy.
    *header_value = pblock_findval(header_name, pb);
    if ((*header_value != NULL) && (strlen(*header_value) > 0 )) {
        am_web_log_debug("%s: %s = %s", thisfunc, header_name, *header_value);
    } else {
        *header_value = NULL;
        if (isRequired == B_TRUE) {
            am_web_log_error("%s: Could not get a value for header %s.", 
                             thisfunc, header_name);
            status = AM_FAILURE;
        } else {
            am_web_log_debug("%s: %s =", thisfunc,
                             header_name);
        }
    }
    // In case the header needs to be modified later in the code,
    // copy it in a variable that can be modified. This one must
    // be freed.
    if ((status == AM_SUCCESS) && (needCopy == B_TRUE) &&
        (*header_value != NULL))
    {
        *header_value_copy = strdup(*header_value);
        if (*header_value_copy == NULL) {
            am_web_log_debug("%s: Not enough memory to make "
                             "a copy of the %s header.",
                             thisfunc, header_name);
            status = AM_NO_MEMORY;
        }
    }
    return status;
}

int send_data(const char *msg, Session *sn, Request *rq) {
    int len = msg != NULL?strlen(msg):0;
    int retVal = REQ_PROCEED;

    if(len > 0) {
        char buf[50];
        buf[0] = '\0';
        sprintf(buf, "%d", len);
        protocol_status(sn, rq, PROTOCOL_OK, NULL);
        param_free(pblock_remove("content-type", rq->srvhdrs));
        pblock_nvinsert("content-type", "text/html", rq->srvhdrs);
        pblock_nvinsert("content-length", buf, rq->srvhdrs);
        // Send the headers to the client
        protocol_start_response(sn, rq);
        // Write the output using net_write
        if (IO_ERROR == net_write(sn->csd, (char *)msg, len)) {
            retVal = REQ_EXIT;
        } else {
            retVal = net_flush(sn->csd);
        }
    }
    return retVal;
}

/*
 * this function redirects the user to the auth login url
 */

static int do_redirect(Session *sn, Request *rq, am_status_t status,
        am_policy_result_t *policy_result,
        const char *original_url, const char* method,
        void* agent_config) {
    int retVal = REQ_ABORTED;
    char *redirect_url = NULL;
    const am_map_t advice_map = policy_result->advice_map;
    am_status_t ret = AM_SUCCESS;

    ret = am_web_get_url_to_redirect(status, advice_map,
            original_url, method,
            AM_RESERVED, &redirect_url, agent_config);
    if (ret == AM_SUCCESS && redirect_url != NULL) {
        char *advice_txt = NULL;
        if (B_FALSE == am_web_use_redirect_for_advice(agent_config) && policy_result->advice_string != NULL) {
            // Composite advice is sent as a POST
            ret = am_web_build_advice_response(policy_result, redirect_url,
                    &advice_txt);
            am_web_log_debug("do_redirect(): policy status=%s, "
                    "response[%s]", am_status_to_string(status),
                    advice_txt);

            if (ret == AM_SUCCESS) {
                retVal = send_data(advice_txt, sn, rq);
            } else {
                am_web_log_error("do_redirect(): Error while building "
                        "adivce response body:%s",
                        am_status_to_string(ret));
                retVal = REQ_EXIT;
            }
        } else {
            // No composite advice or composite advice is redirected
            am_web_log_debug("do_redirect() policy status = %s, "
                    "redirection URL is %s",
                    am_status_to_string(status), redirect_url);

            // we need to modify the redirect_url with the policy advice
            if (B_TRUE == am_web_use_redirect_for_advice(agent_config) &&
                    policy_result->advice_string != NULL) {
                char *redirect_url_with_advice = NULL;
                ret = am_web_build_advice_redirect_url(policy_result,
                        redirect_url, &redirect_url_with_advice);
                if (ret == AM_SUCCESS) {
                    redirect_url = redirect_url_with_advice;
                    am_web_log_debug("do_redirect(): policy status=%s, "
                            "redirect url with advice [%s]",
                            am_status_to_string(status),
                            redirect_url);
                } else {
                    am_web_log_error("do_redirect(): Error while building "
                            "the redirect url with advice:%s",
                            am_status_to_string(ret));
                }
            }

            /* redirection is enabled by the PathCheck directive */
            /* Set the return code to 302 Redirect */
            protocol_status(sn, rq, PROTOCOL_REDIRECT, NULL);

            /* set the new URL to redirect */
            //pblock_nvinsert("url", redirect_url, rq->vars);
            pblock_nvinsert("escape", "no", rq->vars);

            param_free(pblock_remove("Location", rq->srvhdrs));
            pblock_nvinsert("Location", redirect_url, rq->srvhdrs);
            protocol_start_response(sn, rq);

            am_web_free_memory(redirect_url);
        }
    } else if (ret == AM_NO_MEMORY) {
        /* Set the return code 500 Internal Server Error. */
        protocol_status(sn, rq, PROTOCOL_SERVER_ERROR, NULL);
        am_web_log_error("do_redirect() Status code= %s.",
                am_status_to_string(status));
    } else {
        /* Set the return code 403 Forbidden */
        protocol_status(sn, rq, PROTOCOL_FORBIDDEN, NULL);
        am_web_log_info("do_redirect() Status code= %s.",
                am_status_to_string(status));
    }

    return retVal;
}

/**
 * This function is used for sending a 302 redirect in the browser.
 */
static void do_url_redirect(Session *sn, Request *rq, char* redirect_url)
{
    /* Set the return code to 302 Redirect */
    protocol_status(sn, rq, PROTOCOL_REDIRECT, NULL);

    /* set the new URL to redirect */
    pblock_nvinsert("escape", "no", rq->vars);

    param_free(pblock_remove("Location", rq->srvhdrs));
    pblock_nvinsert("Location", redirect_url, rq->srvhdrs);
    protocol_start_response(sn, rq);
}

static int do_deny(Session *sn, Request *rq, am_status_t status) {
    int retVal = REQ_ABORTED;
    /* Set the return code 403 Forbidden */
    protocol_status(sn, rq, PROTOCOL_FORBIDDEN, NULL);
    am_web_log_info("do_redirect() Status code= %s.",
        am_status_to_string(status));
    return retVal;
}

static am_status_t register_post_data(Session *sn, Request *rq, char *url,
                               const char *key, char* body, void* agent_config)
{
    const char *thisfunc = "register_post_data()";
    am_status_t status = AM_SUCCESS;
    am_web_postcache_data_t post_data;
    
    am_web_log_max_debug("%s: Register POST content body : %s",
                         thisfunc, body);
    post_data.value = body;
    post_data.url = url;
    am_web_log_debug("%s: Register POST data key :%s", thisfunc, key);
    if(am_web_postcache_insert(key,&post_data, agent_config) == B_FALSE){
        am_web_log_error("%s: Register POST data insert into"
                         " hash table failed:%s",thisfunc, key);
        status = AM_FAILURE;
    }
    return status;
}

/**
  * Create the html form with the javascript that does the post with the
  * invisible name value pairs
*/
static int create_buffer_withpost(const char *key, am_web_postcache_data_t
                                  postentry, Session *sn, Request *rq,
                                  void* agent_config)
{
    const char *thisfunc = "create_buffer_withpost()";
    am_status_t status = AM_SUCCESS;
    char *buffer_page = NULL;
    int nsapi_status;
    char msg_length[8];
    char *lbCookieHeader = NULL;
    am_status_t status_tmp = AM_SUCCESS;
    buffer_page = am_web_create_post_page(key, postentry.value, 
                                          postentry.url, agent_config);

    // Use the protocol_status function to set the status of the
    // response before calling protocol_start_response.
    protocol_status(sn, rq, PROTOCOL_OK, NULL);

    // Although we would expect the ObjectType stage to
    // set the content-type, set it here just to be
    // completely sure that it gets set to text/html.
    param_free(pblock_remove("content-type", rq->srvhdrs));
    pblock_nvinsert("content-type", "text/html", rq->srvhdrs);
    util_itoa(strlen(buffer_page), msg_length);
    pblock_nvinsert("content-length", msg_length, rq->srvhdrs);

    // If using a LB cookie, it needs to be set to NULL there.
    // If am_web_get_postdata_preserve_lbcookie() returns
    // AM_INVALID_ARGUMENT, it means that the sticky session
    // feature is disabled (ie no LB) or that the sticky
    // session mode is URL.
    status = am_web_get_postdata_preserve_lbcookie(&lbCookieHeader, 
                                                   B_TRUE, agent_config);
    if (status == AM_NO_MEMORY) {
        nsapi_status = REQ_EXIT;
    } else {
        if (status == AM_SUCCESS) {
            am_web_log_debug("%s: Setting LB cookie for post data "
                             "preservation to null.", thisfunc);
            pblock_nvinsert("set-cookie", lbCookieHeader, rq->srvhdrs);
        }
        // Send the headers to the client
        protocol_start_response(sn, rq);
        // Repost the form
        if (net_write(sn->csd, buffer_page , strlen(buffer_page)) == IO_ERROR){
            am_web_log_error("%s: Fail to send the form.", thisfunc);
            nsapi_status = REQ_EXIT;
        } else {
            nsapi_status = REQ_PROCEED;
        }
    }
    am_web_postcache_data_cleanup(&postentry);
    if(buffer_page != NULL){
        system_free(buffer_page);
    }
    if (lbCookieHeader != NULL) {
        am_web_free_memory(lbCookieHeader);
        lbCookieHeader = NULL;
    }
    return nsapi_status;
}

/**
  * Function Name: append_post_data
  * This method is called when the SAF finds a /dummypost.htm extension to the
  * the URL that the browser is trying to reach. If it finds "/dummypost.htm", 
  * the method needs to find the post data from cache and repost it
  *
  * Input:  As defined by a SAF
  * Output: As defined by a SAF
*/

NSAPI_PUBLIC int append_post_data(pblock *param, Session *sn, Request *rq)
{
    const char *thisfunc = "append_post_data()";
    am_status_t status = AM_SUCCESS;
    am_status_t status_tmp = AM_SUCCESS;
    int requestResult = REQ_ABORTED;
    const char *post_data_query = NULL;
    char *uri = NULL;
    am_web_postcache_data_t get_data = {NULL, NULL};
    const char *postdata_cache = NULL;
    const char *actionurl = NULL;
    char *stickySessionValue = NULL;
    char *stickySessionPos = NULL;
    char *temp_uri = NULL;
    void* agent_config = NULL;
    
    uri = pblock_findval("uri", rq->reqpb);
    if(uri == NULL) {
        status = AM_INVALID_ARGUMENT;
    }

    agent_config = am_web_get_agent_configuration();

    // Check if magic URI is present in the URL
    if(status == AM_SUCCESS) {
        post_data_query = uri + strlen(POST_PRESERVE_URI);
        // Check if a query paramter for the  sticky session has been
        // added to the dummy URL. Remove it if it is the case.
        status_tmp = am_web_get_postdata_preserve_URL_parameter
                                   (&stickySessionValue, agent_config);
        if (status_tmp == AM_SUCCESS) {
            stickySessionPos = strstr(post_data_query, stickySessionValue);
            if (stickySessionPos != NULL) {
                size_t len = strlen(post_data_query) - 
                             strlen(stickySessionPos)-1;
                temp_uri = malloc(len+1);
                memset(temp_uri,0,len+1);
                strncpy(temp_uri, post_data_query, len);
                post_data_query = temp_uri;
            }
        }
    }
    // If magic uri present search for corresponding value in hashtable
    if((status == AM_SUCCESS) && (post_data_query != NULL) &&
       (strlen(post_data_query) > 0))
    {
        am_web_log_debug("%s: POST Magic Query Value: %s", 
                         thisfunc, post_data_query);
        if (am_web_postcache_lookup(post_data_query,
                    &get_data, agent_config) == B_TRUE)
        {
            // Now that the data is found, find the data and the URL to redirect
            postdata_cache = get_data.value;
            actionurl = get_data.url;
            am_web_log_debug("%s: POST hashtable actionurl : %s", 
                             thisfunc, actionurl);
            // Create the buffer string that is to be written
            requestResult = create_buffer_withpost(post_data_query,
                        get_data,sn, rq, agent_config);
        } else {
            am_web_log_error("%s: Found magic URI but entry not in POST"
                             " Hash table :%s", thisfunc, post_data_query);
            protocol_status(sn, rq, PROTOCOL_NOT_FOUND, NULL);
            param_free(pblock_remove("referer", rq->headers));
            requestResult = REQ_ABORTED;
        }
    } else {
        am_web_log_error("%s: Magic URL value not found in POST cache", thisfunc);
        protocol_status(sn, rq, PROTOCOL_NOT_FOUND, NULL);
        param_free(pblock_remove("referer", rq->headers));
        requestResult = REQ_ABORTED;
    }
    if (temp_uri != NULL) {
        free(temp_uri);
        temp_uri = NULL;
    }
    if (stickySessionValue != NULL) {
        am_web_free_memory(stickySessionValue);
        stickySessionValue = NULL;
    }
    return requestResult;}

/*
 * update the agent cache from the listener response.  Any response without a
 * valid state for the session would result on the cache being updated
 */

static int handle_notification(Session *sn, 
                               Request *rq,
                               void* agent_config)
{
    int result;
    char *content_length_header;
    size_t content_length;

    /* fixme GETPOST use new getRequestBody() routine here.... */
    result = request_header(CONTENT_LENGTH_HDR, &content_length_header, sn,rq);
    if (REQ_PROCEED == result && NULL != content_length_header &&
	sscanf(content_length_header, "%u", &content_length) == 1) {
	char ch;
	size_t data_length = 0;
	char *buf = NULL;

	buf = system_malloc(content_length);
	if (buf != NULL) {
	    for (data_length = 0; data_length < content_length; data_length++){
		ch = netbuf_getc(sn->inbuf);
		if (ch == IO_ERROR || ch == IO_EOF) {
		    break;
		}
		buf[data_length] = (char) ch;
	    }
	    am_web_handle_notification(buf, data_length, agent_config);
	    system_free(buf);
	} else {
	    am_web_log_error("handle_notification() unable to allocate memory "
			    "for notification data, size = %u",
			    content_length);
	}
	result = REQ_PROCEED;
    } else {
	am_web_log_error("handle_notification() %s content-length header",
			(REQ_PROCEED == result &&
			 NULL != content_length_header) ?
			"unparsable" : "missing");
    }

    return result;
}

/**
  * Function Name: process_notification
  *
  * Processes both session and policy notifications coming from OpenAM server.
  * Implemented as a NSAPI SAF. Works together with Service directive.
  *
  * Input:  As defined by a SAF
  * Output: As defined by a SAF
*/
NSAPI_PUBLIC int process_notification(pblock *param, Session *sn, Request *rq)
{
    return REQ_PROCEED;
}

static int process_new_notification(pblock *param, 
                                    Session *sn, 
                                    Request *rq,
                                    void* agent_config)
{
    handle_notification(sn, rq, agent_config);

    /* Use the protocol_status function to set the status of the
     * response before calling protocol_start_response.
     */
    protocol_status(sn, rq, PROTOCOL_OK, NULL);

    /* Although we would expect the ObjectType stage to
     * set the content-type, set it here just to be
     * completely sure that it gets set to text/html.
     */
    param_free(pblock_remove("content-type", rq->srvhdrs));
    pblock_nvinsert("content-type", "text/html", rq->srvhdrs);

    pblock_nvinsert("content-length", "2", rq->srvhdrs);

    /* Send the headers to the client*/
    protocol_start_response(sn, rq);

    /* Write the output using net_write*/
    if (IO_ERROR == net_write(sn->csd, "OK", 2)) {
        return REQ_EXIT;
    }
    net_flush(sn->csd);
    return REQ_PROCEED;
}

NSAPI_PUBLIC void agent_cleanup(void *args) {
    am_properties_destroy(agent_props.agent_bootstrap_props);
    am_web_cleanup();
    crit_terminate(initLock);
}

/*
 * Function Name: web_agent_init
 *
 * Initializes different things required by the web agent.  Implemented as
 * as a NSAPI SAF.  It initializes the Agent Toolkit, which initializes
 * the DSAME Remote SDK (AM) and any necessary support libraries.
 *
 * NOTE: Until am_web_init() returns successfully, the routine should use
 * log_error rather than the am_web_log_* routines.
 *
 * Input:  As defined by a SAF
 * Output: As defined by a SAF
 */

NSAPI_PUBLIC int web_agent_init(pblock *param, Session *sn, Request *rq)
{
    am_status_t status;
    int nsapi_status = REQ_PROCEED;
    char *temp_buf = NULL;
    char *agent_bootstrap_file = NULL;
    char *agent_config_file = NULL;

    initLock = crit_init();

    temp_buf = pblock_findval(DSAME_CONF_DIR, param);

    if (temp_buf != NULL) {
        agent_bootstrap_file =
                system_malloc(strlen(temp_buf) + sizeof (AGENT_BOOTSTRAP_FILE));
        agent_config_file =
                system_malloc(strlen(temp_buf) + sizeof (AGENT_CONFIG_FILE));
        if (agent_bootstrap_file != NULL) {
            strcpy(agent_bootstrap_file, temp_buf);
            strcat(agent_bootstrap_file, AGENT_BOOTSTRAP_FILE);
        } else {
            log_error(LOG_FAILURE, "Web Policy Agent: ", sn, rq,
                    "web_agent_init() unable to allocate memory for bootstrap "
                    "file name", DSAME_CONF_DIR);
            nsapi_status = REQ_ABORTED;
        }

        if (agent_config_file != NULL) {
            strcpy(agent_config_file, temp_buf);
            strcat(agent_config_file, AGENT_CONFIG_FILE);
        } else {
            log_error(LOG_FAILURE, "Web Policy Agent: ", sn, rq,
                    "web_agent_init() unable to allocate memory for local config "
                    "file name", DSAME_CONF_DIR);
            nsapi_status = REQ_ABORTED;
        }

        if (access(agent_bootstrap_file, R_OK) != 0 || access(agent_config_file, R_OK) != 0) {
            log_error(LOG_FAILURE, "Web Policy Agent: ", sn, rq,
                    "web_agent_init() unable to access bootstrap and/or local config file", DSAME_CONF_DIR);
            nsapi_status = REQ_ABORTED;
        } else {
            status = am_properties_create(&agent_props.agent_bootstrap_props);
            if (status == AM_SUCCESS) {
                status = am_properties_load(agent_props.agent_bootstrap_props,
                        agent_bootstrap_file);
                if (status == AM_SUCCESS) {
                    //this is where the agent config info is passed from filter code
                    //to amsdk. Not sure why agent_props is required.
                    status = am_web_init(agent_bootstrap_file,
                            agent_config_file);
                    system_free(agent_bootstrap_file);
                    system_free(agent_config_file);
                    if (AM_SUCCESS != status) {
                        log_error(LOG_FAILURE, "Web Policy Agent: ", sn, rq,
                                "Initialization of the agent failed: "
                                "status = %s (%d)", am_status_to_string(status),
                                status);
                        nsapi_status = REQ_ABORTED;
                    }
                } else {
                    log_error(LOG_FAILURE, "web_agent_init():", sn, rq,
                            "Error while creating properties object= %s",
                            am_status_to_string(status));
                    nsapi_status = REQ_ABORTED;
                }
            } else {
                log_error(LOG_FAILURE, "web_agent_init():", sn, rq,
                        "Error while creating properties object= %s",
                        am_status_to_string(status));
                nsapi_status = REQ_ABORTED;
            }
        }
    } else {
        log_error(LOG_FAILURE, "Web Policy Agent: ", sn, rq,
                "web_agent_init() %s variable not defined in magnus.conf",
                DSAME_CONF_DIR);
        nsapi_status = REQ_ABORTED;
    }

    if (nsapi_status == REQ_PROCEED) {
        daemon_atrestart(&agent_cleanup, NULL);
    }

    return nsapi_status;
}

/**
 * convert uppercase letters to lowercase
 */
static char *to_lower(char *string)
{
    int c, i, len;
    if (string == NULL) {
        return string;
    }
    len = strlen(string);
    for (i = 0; i < len; ++i) {
        c = string[i];
        if (isupper(c)) {
            string[i] = tolower(c);
        }
    }
    return string;
}

static am_status_t set_header(const char *key, const char *values,
			      void **args) {
    Request *rq = (Request *)args[0];
    // first remove all headers with this key
    char *thekey = to_lower((char *)key);
    char *val = NULL;
    val = pblock_findval(thekey, rq->headers);
    while (val != NULL) {
	param_free(pblock_remove(thekey, rq->headers));
	val = pblock_findval(thekey, rq->headers);
    }
    // set new header
    if (values != NULL) {
	pblock_nvinsert(thekey, values, rq->headers);
    }
    return AM_SUCCESS;
}

static am_status_t set_header_attr_as_cookie(const char *values, void **args)
{
    Request *rq = NULL;
    am_status_t sts = AM_SUCCESS;
    char *cookie_header = NULL;
    char *new_cookie_header = NULL;

    if (args == NULL || args[0] == NULL ||
	values == NULL || values[0] == '\0') {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
        rq = (Request *)args[0];
	cookie_header = pblock_findval("cookie", rq->headers);

	sts = am_web_set_cookie(cookie_header, values, &new_cookie_header);
	if (sts == AM_SUCCESS && new_cookie_header != NULL &&
		new_cookie_header != cookie_header) {
	    sts = set_header("cookie", new_cookie_header, args);
	    free(new_cookie_header);
	}
    }
    return sts;
}


static am_status_t get_cookie_sync(const char *cookieName,
                                   char** dpro_cookie,
                                   void **args)
{
    am_status_t ret = AM_SUCCESS;
    return ret;
}

static am_status_t reset_cookie(const char *header, void **args)
{
    am_status_t ret = AM_INVALID_ARGUMENT;
    if (header != NULL && args !=NULL) {
        Request *rq = (Request *)args[0];
        if (rq == NULL) {
           am_web_log_debug("in reset_cookie: Invalid Request structure");
        } else {
            pblock_nvinsert("set-cookie", header, rq->srvhdrs);
    	    ret = AM_SUCCESS;
	}
    }
    return ret;
}


static am_status_t set_cookie_in_response(const char *header, void **args)
{
    am_status_t ret = AM_INVALID_ARGUMENT;

    if (header != NULL && args !=NULL) {
        Request *rq = (Request *)args[0];
        if (rq == NULL) {
           am_web_log_error("in set_cookie(): Invalid Request structure");
           ret = AM_NO_MEMORY;
        } else {
	    pblock_nvinsert("set-cookie", header, rq->srvhdrs);
	    ret = AM_SUCCESS;
        }
    }
    return ret;
}

static am_status_t set_cookie(const char *header, void **args)
{
    am_status_t ret = AM_INVALID_ARGUMENT;
    if (header != NULL && args !=NULL) {
        Request *rq = (Request *)args[0];
        if (rq == NULL) {
           am_web_log_error("in set_cookie: Invalid Request structure");
        } else {
            pblock_nvinsert("set-cookie", header, rq->srvhdrs);
    	    ret = AM_SUCCESS;
	}
    }
    return ret;
}

char * get_post_data(Session *sn, Request *rq, char *url)
{
    const char *thisfunc = "get_post_data()";
    long cl = 0, i = 0;
    char *body = NULL;
    char *cl_str = NULL;

    request_header("content-length", &cl_str, sn, rq);
    if(cl_str == NULL)
        cl_str = pblock_findval("content-length", rq->headers);
        if(cl_str == NULL) {
            return body;
        }
        if((cl = strtol(cl_str, NULL, 10)) > 0 && errno != ERANGE) {
            body =  (char *)malloc(cl + 1);
            if(body != NULL){
                for (i = 0; i < cl; i++) {
                    int ch = netbuf_getc(sn->inbuf);
                    if (ch==IO_ERROR || ch == IO_EOF) {
                    break;
                }
                body[i] = ch;
            }
            body[i] = '\0';
        }
    } else {
        am_web_log_error("%s: Error reading POST content body", thisfunc);
    }
    am_web_log_max_debug("%s: Read POST content length: %ld, body: %s", thisfunc, cl, body);

    // Need to reset content length before redirect,
    // otherwise, web server will wait for several minutes
    // for non existant data
    param_free(pblock_remove("content-length", rq->headers));
    pblock_nvinsert("content-length", "0", rq->headers);

    return body;
}

static void set_method(void ** args, char * orig_req){
    Request *rq = (Request *)args[0];
    if (rq != NULL) {
      pblock_nvinsert(REQUEST_METHOD, orig_req, rq->reqpb);
    }
}

int process_request_with_post_data_preservation(Session *sn, Request *rq,
                                    am_status_t request_status,
                                    am_policy_result_t *policy_result,
                                    char *requestURL,
                                    void **args,
                                    char **resp,
                                    void* agent_config)
{
    const char *thisfunc = "process_request_with_post_data_preservation()";
    am_status_t status = AM_SUCCESS;
    int requestResult = REQ_PROCEED;
    post_urls_t *post_urls;
    char *response = NULL;
    int local_alloc = 1;

    if (*resp != NULL) {
        response = *resp;
        local_alloc = 0;
    }
    // Create the magic URI, actionurl
    status = am_web_create_post_preserve_urls(requestURL, &post_urls,
                                              agent_config);
    if (status != AM_SUCCESS) {
        requestResult = REQ_ABORTED;
    }
    // In CDSSO mode, for a POST request, the post data have
    // already been saved in the response variable, so we need
    // to get them here only if response is NULL.
    if (status == AM_SUCCESS) {
        if (response == NULL) {
            response = get_post_data(sn, rq, requestURL);
        }
    }
    if (status == AM_SUCCESS) {
        if (response == NULL || strlen(response) == 0) {
            // this is empty POST, make sure PDP handler preserves it and sets up empty html form for re-POST
            if ((response = realloc(response, strlen(AM_WEB_EMPTY_POST) + 1)) != NULL)
                strcpy(response, AM_WEB_EMPTY_POST);
        }
        if (response != NULL && strlen(response) > 0) {
            if (AM_SUCCESS == register_post_data(sn, rq,
                                post_urls->action_url,
                                post_urls->post_time_key,
                                response,
                                agent_config))
            {
                char *lbCookieHeader = NULL;
                // If using a LB in front of the agent and if the sticky 
                // session mode is COOKIE, the lb cookie needs to be set there.
                // If am_web_get_postdata_preserve_lbcookie()
                // returns AM_INVALID_ARGUMENT, it means that the 
                // sticky session feature is disabled (ie no LB) or
                // that the sticky session mode is set to URL.
                status = am_web_get_postdata_preserve_lbcookie(
                          &lbCookieHeader, B_FALSE, agent_config);
                if (status == AM_NO_MEMORY) {
                    requestResult = REQ_ABORTED;
                } else {
                    if (status == AM_SUCCESS) {
                        am_web_log_debug("%s: Setting LB cookie "
                                         "for post data preservation (%s).",
                                         thisfunc, lbCookieHeader);
                        set_cookie(lbCookieHeader, args);
                    }
                    requestResult =  do_redirect(sn, rq, request_status,
                                                 policy_result,
                                                 post_urls->dummy_url,
                                                 REQUEST_METHOD_POST,
                                                 agent_config);
                }
                if (lbCookieHeader != NULL) {
                    am_web_free_memory(lbCookieHeader);
                    lbCookieHeader = NULL;
                }
            } else {
                am_web_log_error("%s: register_post_data() "
                     "failed.", thisfunc);
                requestResult = REQ_ABORTED;
            }
        } else {
            am_web_log_debug("%s: This is a POST request with no post data. "
                             "Redirecting as a GET request.", thisfunc);
            requestResult = do_redirect(sn, rq, request_status,
                                      policy_result,
                                      requestURL, 
                                      REQUEST_METHOD_GET,
                                      agent_config);
        }
    } 
    if (post_urls != NULL) {
        am_web_clean_post_urls(post_urls);
        post_urls = NULL;
    }
    if (response != NULL && local_alloc == 1) {
        free(response);
        response = NULL;
    }
    
    return requestResult;
}

/*
 * Function Name: validate_session_policy
 * This is the NSAPI directive funtion which gets called for each request
 * It does session validation and policy check for each request.
 * Input: As defined by a SAF
 * Output: As defined by a SAF
 */
NSAPI_PUBLIC int
validate_session_policy(pblock *param, Session *sn, Request *rq) 
{
    const char *thisfunc = "validate_session_policy()";
    char *dpro_cookie = NULL;
    am_status_t status = AM_FAILURE;
    am_status_t status_tmp = AM_SUCCESS;
    int  requestResult = REQ_ABORTED;
    int  notifResult = REQ_ABORTED;
    const char *ruser = NULL;
    am_map_t env_parameter_map = NULL;
    am_policy_result_t result = AM_POLICY_RESULT_INITIALIZER;
    void *args[] = { (void *)rq };
    char *request_url = NULL;    
    char *orig_req = NULL ;
    char *response = NULL;
    char *clf_req =NULL;
    char *protocol_hdr = NULL;
    char *uri_hdr = NULL;
    char *pathInfo_hdr = NULL;
    char *method_hdr = NULL;
    char *method = NULL;
    char *host_hdr = NULL;
    char *query_hdr = NULL;
    char *query = NULL;
    char *protocol = "HTTP";
    const char *clientIP_hdr_name = NULL;
    char *clientIP_hdr = NULL;
    char *clientIP = NULL;
    const char *clientHostname_hdr_name = NULL;
    char *clientHostname_hdr = NULL;
    char *clientHostname = NULL;
    char *orig_request_url = NULL;
    void* agent_config = NULL;
    am_status_t cdStatus = AM_FAILURE;
    char* logout_url = NULL;

    // Check if agent is initialized.
    // If not initialized, then call agent init function
    // This needs to be synchronized as only one time agent
    // initialization needs to be done.
    if(agentInitialized != B_TRUE){
        //Start critical section
        crit_enter(initLock);
        if(agentInitialized != B_TRUE){
            am_web_log_debug("validate_session_policy : "
                "Will call init");
            init_at_request(); 
            if(agentInitialized != B_TRUE){
                am_web_log_error("validate_session_policy : "
                   " Agent is still not intialized");
                //deny the access
                requestResult =  do_deny(sn, rq, status);
                return requestResult;
            }  else {
                am_web_log_debug("validate_session_policy : "
                    "Agent intialized");
            }
        }
        //end critical section
        crit_exit(initLock);
    }

    agent_config = am_web_get_agent_configuration();

    // Dump the entire set of request headers 
    if (am_web_is_max_debug_on()) {
        char *header_str = pblock_pblock2str(rq->reqpb, NULL);
        am_web_log_max_debug("%s: Headers = %s", thisfunc, header_str);
        system_free(header_str);
    }
    
    // Get header values.
    // Note: the variables ending by "_hdr" should not be modified or free
    status  = get_header_value(rq->reqpb, REQUEST_PROTOCOL,
                               B_TRUE, &protocol_hdr, B_FALSE, NULL);
    if (status == AM_SUCCESS) {
        status = get_header_value(rq->reqpb, REQUEST_URI,
                               B_TRUE, &uri_hdr, B_FALSE, NULL);
    }
    if (status == AM_SUCCESS) {
        status = get_header_value(rq->vars, PATH_INFO,
                               B_FALSE, &pathInfo_hdr, B_FALSE, NULL);
    }
    if (status == AM_SUCCESS) {
        status = get_header_value(rq->reqpb, REQUEST_METHOD,
                               B_TRUE, &method_hdr, B_TRUE, &method);
    }
    if (status == AM_SUCCESS) {
        status = get_header_value(rq->headers, HOST_HDR,
                               B_FALSE, &host_hdr, B_FALSE, NULL);
    }
    if (status == AM_SUCCESS) {
        status = get_header_value(rq->reqpb, REQUEST_QUERY,
                               B_FALSE, &query_hdr, B_TRUE, &query);
    }
    // Get the request URL
    if (status == AM_SUCCESS) {
        if (security_active) {
            protocol = "HTTPS";
        }
        status = am_web_get_all_request_urls(host_hdr, protocol,
                                             server_hostname,
                                             server_portnum, uri_hdr, query,
                                             agent_config, &request_url,
                                             &orig_request_url);
        if(status == AM_SUCCESS) {
            am_web_log_debug("%s: Request_url: %s", thisfunc, request_url);
        } else {
            am_web_log_error("%s: Could not get the request URL. "
                             "Failed with error: %s.",
                             thisfunc, am_status_to_string(status));
        }
    }
    // Check notification URL. request_url is getting passed.
    if (status == AM_SUCCESS) {
        if (B_TRUE == am_web_is_notification(orig_request_url, agent_config)) {
            notifResult = process_new_notification(param, sn, rq, agent_config);
            if(query != NULL) {
                free(query);
                query = NULL;
            }
            if(method != NULL) {
                free(method);
                method = NULL;
            }
            am_web_free_memory(request_url);
            am_web_free_memory(orig_request_url);
            am_web_delete_agent_configuration(agent_config);
            return notifResult;
        }
    }
    
    if (status == AM_SUCCESS) {
        int vs = am_web_validate_url(agent_config, request_url);
        if (vs != -1) {
            if (vs == 1) {
                am_web_log_debug("%s: Request URL validation succeeded", thisfunc);
                status = AM_SUCCESS;
            } else {
                am_web_log_error("%s: Request URL validation failed. Returning Access Denied error (HTTP403)", thisfunc);
                status = AM_FAILURE;
                if (query != NULL) {
                    free(query);
                    query = NULL;
                }
                if (method != NULL) {
                    free(method);
                    method = NULL;
                }
                am_web_free_memory(request_url);
                am_web_free_memory(orig_request_url);
                am_web_delete_agent_configuration(agent_config);
                return do_deny(sn, rq, status);
            }
        }
    }
    
    // Check if the SSO token is in the cookie header
    if (status == AM_SUCCESS) {
        status_tmp = am_web_get_cookie_value(";", am_web_get_cookie_name(agent_config),
                pblock_findval(COOKIE_HDR, rq->headers), &dpro_cookie);
        am_web_log_debug("%s: sso token %s, status - %s", thisfunc, dpro_cookie, am_status_to_string(status_tmp));
        if (status_tmp == AM_INVALID_ARGUMENT || status_tmp == AM_NO_MEMORY) {
            status = AM_FAILURE;
        }
    }
    // Create the environment map
    if( status == AM_SUCCESS) {
        status = am_map_create(&env_parameter_map);
        if( status != AM_SUCCESS) {
            am_web_log_error("%s: Unable to create map, status = %s (%d)",
                   thisfunc, am_status_to_string(status), status);
        }
    }
    // If there is a proxy in front of the agent, the user can set in the
    // properties file the name of the headers that the proxy uses to set
    // the real client IP and host name. In that case the agent needs
    // to use the value of these headers to process the request
    //
    // Get the client IP address header set by the proxy, if there is one
    if (status == AM_SUCCESS) {
        clientIP_hdr_name = am_web_get_client_ip_header_name(agent_config);
        if (clientIP_hdr_name != NULL) {
            status = get_header_value(rq->headers, clientIP_hdr_name,
                                    B_FALSE, &clientIP_hdr,
                                    B_FALSE, NULL);
        }
    }
    // Get the client host name header set by the proxy, if there is one
    if (status == AM_SUCCESS) {
        clientHostname_hdr_name = am_web_get_client_hostname_header_name
                                  (agent_config);
        if (clientHostname_hdr_name != NULL) {
            status = get_header_value(rq->headers, clientHostname_hdr_name,
                                    B_FALSE, &clientHostname_hdr,
                                    B_FALSE, NULL);
        }
    }
    // If the client IP and host name headers contain more than one
    // value, take the first value.
    if (status == AM_SUCCESS) {
        if ((clientIP_hdr != NULL) || (clientHostname_hdr != NULL)) {
            status = am_web_get_client_ip_host(clientIP_hdr,
                                               clientHostname_hdr,
                                               &clientIP, &clientHostname);
        }
    }
    // Set the IP address and host name in the environment map
    if ((status == AM_SUCCESS) && (clientIP != NULL)) {
        status = am_web_set_host_ip_in_env_map(clientIP, clientHostname,
                                      env_parameter_map, agent_config);
    }
    // If the client IP was not obtained previously,
    // get it from the REMOTE_ADDR header.
    if ((status == AM_SUCCESS) && (clientIP == NULL)) {
        status = get_header_value(sn->client, REQUEST_IP_ADDR,
                               B_FALSE, &clientIP_hdr, B_TRUE, &clientIP);
    }
    // In CDSSO mode, check if the sso token is in the post data
    if (status == AM_SUCCESS) {
        if ((dpro_cookie == NULL) &&
            (am_web_is_cdsso_enabled(agent_config) == B_TRUE) &&
            (am_web_is_url_enforced(request_url, pathInfo_hdr, 
                   clientIP, agent_config) == B_TRUE))
        {
            if (strcmp(method, REQUEST_METHOD_POST) == 0) {
                //Set original method to GET
                orig_req = strdup(REQUEST_METHOD_GET);
                if (orig_req != NULL) {
                    am_web_log_debug("%s: Request method set to GET.",
                                     thisfunc);
                    response = get_post_data(sn, rq, request_url);
                    status = am_web_check_cookie_in_post(args, &dpro_cookie,
                                                   &request_url,
                                                   &orig_req, method, response,
                                                   B_FALSE, set_cookie, 
                                                   set_method, agent_config);
                    // Set back the original clf-request attribute
                    if (status == AM_SUCCESS) {
                        int clf_reqSize = 0;
                        if ((query != NULL) && (strlen(query) > 0)) {
                            clf_reqSize = strlen(orig_req) + strlen(uri_hdr) +
                                          strlen (query) + strlen(protocol) + 4;
                        } else {
                            clf_reqSize = strlen(orig_req) + strlen(uri_hdr) +
                                          strlen(protocol) + 3;
                        }
                        clf_req = malloc(clf_reqSize);
                        if (clf_req == NULL) {
                            am_web_log_error("%s: Unable to allocate %i "
                                             "bytes for clf_req", thisfunc,
                                             clf_reqSize);
                            status = AM_NO_MEMORY;
                        } else {
                            memset (clf_req,'\0',clf_reqSize);
                            strcpy(clf_req, orig_req);
                            strcat(clf_req, " ");
                            strcat(clf_req, uri_hdr);
                            if ((query != NULL) && (strlen(query) > 0)) {
                                strcat(clf_req, "?");
                                strcat(clf_req, query);
                            }
                            strcat(clf_req, " ");
                            strcat(clf_req, protocol);
                            am_web_log_debug("%s: clf-request set to %s", 
                                             thisfunc, clf_req);
                        }
                        pblock_nvinsert(REQUEST_CLF, clf_req, rq->reqpb);
                    } else {
                        am_web_log_debug("%s: SSO token not found in "
                                       "assertion. Redirecting to login page.",
                                       thisfunc);
                        status = AM_INVALID_SESSION;
                    }
                } else {
                    am_web_log_error("%s: Not enough memory to ",
                                "allocate orig_req.", thisfunc);
                    status = AM_NO_MEMORY;
                }
            }
        } 
    }

    // Check if access is allowed.
    if( status == AM_SUCCESS) {
        if (dpro_cookie != NULL) {
            am_web_log_debug("%s: SSO token = %s", thisfunc, dpro_cookie);
        } else {
            am_web_log_debug("%s: SSO token not found.", thisfunc);
        }
        status = am_web_is_access_allowed(dpro_cookie, request_url,
                              pathInfo_hdr, method,
                              clientIP, env_parameter_map, &result,
                              agent_config);
        am_web_log_debug("%s: Status after "
                "am_web_is_access_allowed = %s (%d)",thisfunc,
                am_status_to_string(status), status);
        am_map_destroy(env_parameter_map);
    }
    
    /* avoid caching of any unauthenticated response */
    if (am_web_is_cache_control_enabled(agent_config) == B_TRUE && status != AM_SUCCESS) {
        set_header("Cache-Control", "no-store, no-cache", args);
        set_header("Pragma", "no-cache", args);
        set_header("Expires", "0", args);
    }
    
    switch (status) {
        case AM_SUCCESS:
            // Set remote user and authentication type
            ruser = result.remote_user;
            if (ruser != NULL) {
                pb_param *pbuser = pblock_remove(AUTH_USER_VAR, rq->vars);
                pb_param *pbauth = pblock_remove(AUTH_TYPE_VAR, rq->vars);
                if (pbuser != NULL) {
                    param_free(pbuser);
                }
                pblock_nvinsert(AUTH_USER_VAR, ruser, rq->vars);
                if (pbauth != NULL) {
                    param_free(pbauth);
                }
                pblock_nvinsert(AUTH_TYPE_VAR, AM_WEB_AUTH_TYPE_VALUE, rq->vars);
                am_web_log_debug("%s: Remote user set to %s", thisfunc, ruser);
            } else {
                am_web_log_debug("%s: Remote user not set, allowing access "
                        "to the url as it is in not enforced list", thisfunc);
            }
            // set LDAP user attributes to http header
            status = am_web_result_attr_map_set(&result, set_header,
                                               set_cookie_in_response,
                                               set_header_attr_as_cookie,
                                               get_cookie_sync, args, agent_config);
            if (status != AM_SUCCESS) {
                am_web_log_error("%s: am_web_result_attr_map_set failed, "
                                 "status = %s (%d)", thisfunc,
                                 am_status_to_string(status), status);
            }
            requestResult = REQ_PROCEED;
            break;

        case AM_ACCESS_DENIED:
            am_web_log_debug("%s: Access denied to %s", thisfunc,
                      result.remote_user ? result.remote_user :
                      "unknown user");
            if(am_web_is_cdsso_enabled(agent_config) == B_TRUE) {
                am_web_log_debug("Resetting cookie to avoid double assertion post");
                am_web_do_cookies_reset(reset_cookie, args, agent_config);
            }
            // If the post data preservation feature is enabled
            // save the post data in the cache for post requests.
            // This needs to be done when the access has been denied 
            // in case there is a composite advice.
            if ((strcmp(method, REQUEST_METHOD_POST) == 0) &&
                  (B_TRUE == am_web_is_postpreserve_enabled(agent_config)))
            {
                requestResult = process_request_with_post_data_preservation(
                                    sn, rq, status, &result, request_url,
                                    args, &response, agent_config);
            } else {
                requestResult = do_redirect(sn, rq, status, &result,
                                            request_url, method, agent_config);
            }
            break;

        case AM_INVALID_SESSION:
            am_web_log_info("%s: Invalid session.",thisfunc);
            // Reset the cookie CDSSO. 
            if (am_web_is_cdsso_enabled(agent_config) == B_TRUE) {
                cdStatus = am_web_do_cookie_domain_set(set_cookie, args, 
                                                       EMPTY_STRING, 
                                                       agent_config);
                if(cdStatus != AM_SUCCESS) {
                    am_web_log_error("%s : CDSSO reset cookie failed",
                                     thisfunc);
                }
             }
            am_web_do_cookies_reset(reset_cookie, args, agent_config);
            if ((strcmp(method, REQUEST_METHOD_POST) == 0) &&
                  (B_TRUE == am_web_is_postpreserve_enabled(agent_config)))
            {
                requestResult = process_request_with_post_data_preservation(
                                    sn, rq, status, &result, request_url,
                                    args, &response, agent_config);
            } else {
                requestResult = do_redirect(sn, rq, status, &result,
                                            request_url, method, agent_config);
            }
            break;

        case AM_INVALID_FQDN_ACCESS:
            // Redirect to self with correct FQDN - no post preservation
            requestResult = do_redirect(sn, rq, status, &result,
                                    request_url, method, agent_config);
            break;

        case AM_REDIRECT_LOGOUT:
            if (am_web_is_agent_logout_url(request_url, agent_config) == B_TRUE) {
                (void)am_web_logout_cookies_reset(reset_cookie, args,
                                                  agent_config);
            }
            status = am_web_get_logout_url(&logout_url, agent_config);
            if(status == AM_SUCCESS) {
                do_url_redirect(sn, rq, logout_url);
            } else {
                requestResult = REQ_ABORTED;
                am_web_log_debug("%s: am_web_get_logout_url failed.",
                                 thisfunc);
            }
            break;

        case AM_INVALID_ARGUMENT:
        case AM_NO_MEMORY:
        default:
            am_web_log_error("%s: Status: %s (%d)", thisfunc,
                   am_status_to_string(status), status);
            requestResult = REQ_ABORTED;
            break;
    }

    am_web_clear_attributes_map(&result);
    am_policy_result_destroy(&result);
    am_web_free_memory(dpro_cookie);
    am_web_free_memory(request_url);
    am_web_free_memory(orig_request_url);
    am_web_free_memory(logout_url);
    am_web_delete_agent_configuration(agent_config);
    if(response != NULL) {
        free(response);
        response = NULL;
    }    
    if(orig_req != NULL) {
        free(orig_req);
        orig_req = NULL;
    }
    if(clf_req != NULL) {
        free(clf_req);
        clf_req = NULL;
    }
    if(query != NULL) {
        free(query);
        query = NULL;
    }
    if(method != NULL) {
        free(method);
        method = NULL;
    }
    if(clientIP != NULL) {
        am_web_free_memory(clientIP);
    }
    if(clientHostname != NULL) {
        am_web_free_memory(clientHostname);
    }
    
    am_web_log_max_debug("%s: Completed handling request with status: %s.",
                         thisfunc, am_status_to_string(status));
    return requestResult;
}


/**
* This function is invoked to initialize the agent 
* during the first request.  
*/

void init_at_request()
{
    am_status_t status;
    status = am_agent_init(&agentInitialized);
    if (status != AM_SUCCESS) {
        log_error(LOG_FAILURE, "Web Policy Agent: ", NULL, NULL,
            "Initialization of the agent failed: "
            "status = %s (%d)", am_status_to_string(status), status);
    } 
} 
