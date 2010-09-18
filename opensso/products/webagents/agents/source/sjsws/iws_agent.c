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
 * $Id: iws_agent.c,v 1.25 2009/10/13 01:29:10 robertis Exp $
 *
 *
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
//#include <prlock.h>

#include <stdio.h>
#if     defined(WINNT)
#define snprintf        _snprintf
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

	/* Send the headers to the client*/
	protocol_start_response(sn, rq);

	/* Write the output using net_write*/
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
    if(ret == AM_SUCCESS && redirect_url != NULL) {
	char *advice_txt = NULL;
	if (policy_result->advice_string != NULL) {
	    ret = am_web_build_advice_response(policy_result, redirect_url,
					       &advice_txt);
	    am_web_log_debug("do_redirect(): policy status=%s, "
			     "response[%s]", am_status_to_string(status),
			      advice_txt);

	    if(ret == AM_SUCCESS) {
		retVal = send_data(advice_txt, sn, rq);
	    } else {
		am_web_log_error("do_redirect(): Error while building "
				 "adivce response body:%s",
				 am_status_to_string(ret));
		retVal = REQ_EXIT;
	    }
	} else {
	    am_web_log_debug("do_redirect() policy status = %s, "
			     "redirection URL is %s",
			     am_status_to_string(status), redirect_url);

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
    } else if(ret == AM_NO_MEMORY) {
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
  * Method to register POST data in agent cache
*/

static void register_post_data_orig(Session *sn, Request *rq,char *url,
			       const char *key, void* agent_config)
{
    int i = 0;
    char *body = NULL;
    int cl = 0;
    char *cl_str = NULL;
    am_web_postcache_data_t post_data;

    /**
    * content length and body
    *
    * note: memory allocated in here should be released by
    * other function such as: "policy_unregister_post"
    */

    request_header("content-length", &cl_str, sn, rq);
    cl = atoi(cl_str);
    if ((cl_str != NULL) && (cl > 0)){
	body =  system_malloc(cl + 1);
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
	am_web_log_error("Error Registering POST content body");
    }

    am_web_log_max_debug("Register POST content body : %s", body);

    post_data.value = body;
    post_data.url = url;

    am_web_log_debug("Register POST data key :%s",key);

    if(am_web_postcache_insert(key,&post_data, agent_config) == B_FALSE){
	am_web_log_warning("Register POST data insert into"
			  " hash table failed:%s",key);
    }

    system_free(body);

    /**
    * need to reset content length before redirect,
    * otherwise, web server will wait for serveral minutes
    * for non existant data
    */
    param_free(pblock_remove("content-length", rq->headers));
    pblock_nvinsert("content-length", "0", rq->headers);

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
    char *buffer_page = NULL;
    int nsapi_status;
    char msg_length[8];
    const char *lbCookieHeader = NULL;
    am_status_t status_tmp = AM_SUCCESS;

    buffer_page = am_web_create_post_page(key,postentry.value,postentry.url,
                                           agent_config);

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

    util_itoa(strlen(buffer_page), msg_length);
    pblock_nvinsert("content-length", msg_length, rq->srvhdrs);

    // If using the lb cookie, it needs to be reset to NULL there.
    status_tmp = am_web_get_postdata_preserve_lbcookie(
                 &lbCookieHeader, B_TRUE, agent_config);
    if (status_tmp == AM_SUCCESS) {
        if (lbCookieHeader != NULL) {
            am_web_log_debug("%s: Setting LB cookie for post data "
                              "preservation to  null", thisfunc);
            pblock_nvinsert("set-cookie", lbCookieHeader, rq->srvhdrs);
        }
        // Send the headers to the client
        protocol_start_response(sn, rq);
        // Repost the form
        if (net_write(sn->csd, buffer_page , strlen(buffer_page)) == IO_ERROR){
            nsapi_status = REQ_EXIT;
        } else {
            nsapi_status = REQ_PROCEED;
        }
    } else {
        am_web_log_error("%s: am_web_get_postdata_preserve_lbcookie() failed",
                          thisfunc);
        nsapi_status = REQ_EXIT;
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
    const char *post_data_query   = NULL;
    char *uri		    = NULL;
    am_web_postcache_data_t get_data = {NULL, NULL};
    const char *postdata_cache    = NULL;
    const char *actionurl	    = NULL;

    void* agent_config = NULL;
    uri = pblock_findval("uri", rq->reqpb);
    agent_config = am_web_get_agent_configuration();


    if (uri != NULL){
	post_data_query = uri + strlen(POST_PRESERVE_URI);
    }

    // If magic uri present and there is a corresponding value in hashtable
    if (post_data_query != NULL){

	am_web_log_debug("POST Magic Query Value  : %s",post_data_query);

	if (am_web_postcache_lookup(post_data_query,
				    &get_data, agent_config) == B_TRUE) {

	    // Now that the data is found, find the data and the URL to redirect
	    postdata_cache = get_data.value;
	    actionurl = get_data.url;

	    am_web_log_debug("POST hashtable actionurl : %s",actionurl);

	    // Create the buffer string that is to be written
	    return create_buffer_withpost(post_data_query,
					  get_data,sn, rq, agent_config);

	} else {
	    am_web_log_debug(" Found magic URI but entry not in POST"
			    " Hash table :%s",post_data_query);
	}

	return(REQ_ABORTED);

    }else {

	am_web_log_error("Magic URL value not found in POST cache");
	return(REQ_ABORTED);
    }
}

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
  * Processes both session and policy notifications coming from OpenSSO server.
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
            system_malloc(strlen(temp_buf) + sizeof(AGENT_BOOTSTRAP_FILE));
        agent_config_file = 
            system_malloc(strlen(temp_buf) + sizeof(AGENT_CONFIG_FILE));
	if (agent_bootstrap_file != NULL) {
	    strcpy(agent_bootstrap_file, temp_buf);
	    strcat(agent_bootstrap_file, AGENT_BOOTSTRAP_FILE);
	} else {
	    log_error(LOG_FAILURE, "URL Access Agent: ", sn, rq,
		      "web_agent_init() unable to allocate memory for bootstrap "
		      "file name", DSAME_CONF_DIR);
	    nsapi_status = REQ_ABORTED;
	}

	if (agent_config_file != NULL) {
	    strcpy(agent_config_file, temp_buf);
	    strcat(agent_config_file, AGENT_CONFIG_FILE);
	} else {
	    log_error(LOG_FAILURE, "URL Access Agent: ", sn, rq,
		      "web_agent_init() unable to allocate memory for local config "
		      "file name", DSAME_CONF_DIR);
	    nsapi_status = REQ_ABORTED;
	}

	status = am_properties_create(&agent_props.agent_bootstrap_props);
	if(status == AM_SUCCESS) {
	    status = am_properties_load(agent_props.agent_bootstrap_props, 
                                    agent_bootstrap_file);
	    if(status == AM_SUCCESS) {
                //this is where the agent config info is passed from filter code
                //to amsdk. Not sure why agent_props is required.
		status = am_web_init(agent_bootstrap_file, 
                                         agent_config_file);
		system_free(agent_bootstrap_file);
		system_free(agent_config_file);
		if (AM_SUCCESS != status) {
		    log_error(LOG_FAILURE, "URL Access Agent: ", sn, rq,
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
    } else {
	log_error(LOG_FAILURE, "URL Access Agent: ", sn, rq,
		  "web_agent_init() %s variable not defined in magnus.conf",
		  DSAME_CONF_DIR);
	nsapi_status = REQ_ABORTED;
    }

    if(nsapi_status == REQ_PROCEED) {
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


char * get_post_assertion_data(Session *sn, Request *rq, char *url)
{
    int i = 0;
    char *body = NULL;
    int cl = 0;
    char *cl_str = NULL;

    /**
    * content length and body
    *
    * note: memory allocated in here should be released by
    * other function such as: "policy_unregister_post"
    */

    request_header("content-length", &cl_str, sn, rq);
    if(cl_str == NULL)
	    cl_str = pblock_findval("content-length", rq->headers);
    if(cl_str == NULL)
	    return body;
    if(PR_sscanf(cl_str, "%ld", &cl) == 1) {
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
        am_web_log_error("Error reading POST content body");
    }

    am_web_log_max_debug("Read POST content body : %s", body);


    /**
    * need to reset content length before redirect,
    * otherwise, web server will wait for serveral minutes
    * for non existant data
    */
    param_free(pblock_remove("content-length", rq->headers));
    pblock_nvinsert("content-length", "0", rq->headers);
    return body;

}

char * get_post_data(Session *sn, Request *rq, char *url)
{
    const char *thisfunc = "get_post_data()";int i = 0;
    char *body = NULL;
    int cl = 0;
    char *cl_str = NULL;

    request_header("content-length", &cl_str, sn, rq);
    if(cl_str == NULL)
        cl_str = pblock_findval("content-length", rq->headers);
        if(cl_str == NULL) {
            return body;
        }
        if(PR_sscanf(cl_str, "%ld", &cl) == 1) {
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
    am_web_log_max_debug("%s: Read POST content body : %s", body, thisfunc);

    // Need to reset content length before redirect,
    // otherwise, web server will wait for several minutes
    // for non existant data
    param_free(pblock_remove("content-length", rq->headers));
    pblock_nvinsert("content-length", "0", rq->headers);

    return body;
}

int getISCookie(const char *cookie, char **dpro_cookie,
                void* agent_config) {
    char *loc = NULL;
    char *marker = NULL;
    int length = 0;
    char *search_cookie = NULL;


    if (cookie != NULL) {
	const char* cookieName = am_web_get_cookie_name(agent_config);
	if (cookieName != NULL && cookieName[0] != '\0') {
		length = 2+strlen(cookieName);
		search_cookie = (char *)malloc(length);
		if (search_cookie !=NULL) {
	    	search_cookie = strcpy(search_cookie,cookieName);
	    	search_cookie = strcat(search_cookie,"=");
	    	length = 0;
		} else {
			am_web_log_error("iws_agent::getISCookie "
			"unable to allocate, size = %u", length);
		    return REQ_ABORTED;
		}
    }

	loc = strstr(cookie, search_cookie);

	// look for last cookie
	while (loc) {
	    char *tmp = strstr(loc+1, search_cookie);
	    if (tmp) {
		loc = tmp;
	    } else {
		break;
	    }
	}

	if (search_cookie !=NULL) {
		free(search_cookie);
	}

	if (loc) {
	    loc = loc + am_web_get_cookie_name_len(agent_config) + 1;
	    while (*loc == ' ') {
		++loc;
	    }

	    // skip leading space
	    while (*loc == ' ') {
		++loc;
	    }

	    // look for end of cookie
	    marker = loc;
	    while ((*loc != '\0') && (*loc != ';')) {
		++loc;
	    }
	    length = loc - marker;

	    if (length > 0) {
		*dpro_cookie = malloc(length+1);
		if (*dpro_cookie == NULL) {
		    am_web_log_error("iws_agent::getISCookie "
			"unable to allocate, size = %u", length);
		    return REQ_ABORTED;
		}
		memcpy(*dpro_cookie, marker, length);
		(*dpro_cookie)[length] = '\0';
	    }

	} else {
	    am_web_log_warning("OpenSSO Server Cookie not found.");
	}
    }

    return REQ_PROCEED;
}


static void set_method(void ** args, char * orig_req){
    Request *rq = (Request *)args[0];
    if (rq != NULL) {
      pblock_nvinsert(REQUEST_METHOD, orig_req, rq->reqpb);
    }
}

am_status_t get_request_url(Session *sn, 
                           Request *rq, 
                           void* agent_config,
                           char **request_url,
                           char **orig_request_url) {
    am_status_t retVal = AM_SUCCESS;
    const char *protocol = "HTTP";
    const char *host_hdr = pblock_findval(HOST_HDR, rq->headers);
    const char *query = pblock_findval(REQUEST_QUERY, rq->reqpb);
    const char *uri = pblock_findval(REQUEST_URI, rq->reqpb);

    if (security_active) {
        protocol = "HTTPS";
    }
    retVal = am_web_get_all_request_urls(host_hdr, protocol, server_hostname,
                                    server_portnum, uri, query,
                                    agent_config, request_url, orig_request_url);
    if (retVal != AM_SUCCESS) {
        am_web_log_error("get_request_url(): Failed with error: %s.",
                         am_status_to_string(retVal));
    }
    return retVal;
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
    char *method = NULL;
    char *request_url = NULL;
    char *orig_request_url = NULL;
    char *orig_req = NULL ;
    char *path_info = NULL;
    char * response = NULL;
    char *clf_req =NULL;
    const char *query = pblock_findval(REQUEST_QUERY, rq->reqpb);
    const char *uri = pblock_findval(REQUEST_URI, rq->reqpb);
    const char *protocol = pblock_findval(REQUEST_PROTOCOL, rq->reqpb); 
    void* agent_config = NULL;

    char* logout_url = NULL;
    am_status_t cdStatus = AM_FAILURE; 
    char* cookie_name=NULL; 
    int cookie_header_len;
    char* cookie_header = NULL;
    // check if agent is initialized.
    // if not initialized, then call agent init function
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

    if (am_web_is_max_debug_on()) {
	/* Dump the entire set of request headers */
	char *header_str = pblock_pblock2str(rq->reqpb, NULL);

	am_web_log_max_debug("validate_session_policy() headers = %s",
			    header_str);
	system_free(header_str);
    }

    path_info = pblock_findval(PATH_INFO, rq->vars);
    am_web_log_debug("%s: path_info=%s", "validate_session_policy()", path_info);
    method = pblock_findval(REQUEST_METHOD, rq->reqpb);
    
    agent_config = am_web_get_agent_configuration();

    status = get_request_url(sn, 
                             rq, 
                             agent_config, 
                             &request_url, 
                             &orig_request_url);

    /* Check notification URL. orig_request_url is getting passed. */
    if (B_TRUE==am_web_is_notification(orig_request_url, agent_config)) {
        notifResult = process_new_notification(param, sn, rq, agent_config);
        am_web_free_memory(request_url);
        am_web_free_memory(orig_request_url);
        am_web_delete_agent_configuration(agent_config);
        return notifResult;
    }

    if (getISCookie(pblock_findval(COOKIE_HDR, rq->headers), &dpro_cookie, agent_config)
        == REQ_ABORTED) {
        am_web_free_memory(request_url);
        am_web_free_memory(orig_request_url);
        am_web_delete_agent_configuration(agent_config);
        return REQ_ABORTED;
    }

    if (dpro_cookie != NULL)
        am_web_log_debug("validate_session_policy(): Cookie Found: %s",
                         dpro_cookie);
    else
        am_web_log_debug("validate_session_policy(): Cookie Not Found in "
                         " request headers.");

    if (dpro_cookie == NULL && (am_web_is_cdsso_enabled(agent_config) == B_TRUE)
       && (am_web_is_url_enforced(request_url, path_info, 
               pblock_findval(REQUEST_IP_ADDR, sn->client), agent_config) == B_TRUE)){
        if (strcmp(method, REQUEST_METHOD_POST) == 0) {
            response = get_post_assertion_data(sn, rq, request_url);
            // Set the original request method to GET always
            orig_req = (char *)malloc(strlen(getMethod)+1);
            if (orig_req != NULL) {
                strcpy(orig_req,getMethod);
                status = am_web_check_cookie_in_post(args, &dpro_cookie,
                                               &request_url,
                                               &orig_req, method, response,
                                               B_FALSE, set_cookie, 
                                               set_method, agent_config);
                if (status == AM_SUCCESS) {
                    // Set back the original clf-request attribute
                    int clf_reqSize = 0;
                    if ((query != NULL) && (strlen(query) > 0)) {
                        clf_reqSize = strlen(orig_req) + strlen(uri) +
                                      strlen (query) + strlen(protocol) + 4;
                    } else {
                        clf_reqSize = strlen(orig_req) + strlen(uri) +
                                      strlen(protocol) + 3;
                    }
                    clf_req = malloc(clf_reqSize);
                    if (clf_req == NULL) {
                        am_web_log_error("validate_session_policy() "
                                      "Unable to allocate %i bytes for clf_req",
                        clf_reqSize);
                        status = AM_NO_MEMORY;
                    } else {
                        memset (clf_req,'\0',clf_reqSize);
                        strcpy(clf_req, orig_req);
                        strcat(clf_req, " ");
                        strcat(clf_req, uri);
                        if ((query != NULL) && (strlen(query) > 0)) {
                            strcat(clf_req, "?");
                            strcat(clf_req, query);
                        }
                        strcat(clf_req, " ");
                        strcat(clf_req, protocol);
                        am_web_log_debug("validate_session_policy(): "
                                         "clf-request set to %s", clf_req);
                    }
                    pblock_nvinsert(REQUEST_CLF, clf_req, rq->reqpb);
                }
            } else {
                am_web_log_error("validate_session_policy() : Unable to "
                                 "allocate memory for orig_req");
            }
	} 
    }

    if (dpro_cookie != NULL) {
	am_web_log_debug("validate_session_policy() cookie is %s", dpro_cookie);
    } else {
	am_web_log_debug("validate_session_policy() request has no cookie");
    }

    status = am_map_create(&env_parameter_map);
    if (status != AM_SUCCESS) {
        am_web_log_error("validate_session_policy() unable to create map, "
                         "status = %s (%d)", am_status_to_string(status),
                         status);
    }

    if (status == AM_SUCCESS) {
        // Check if client ip header property is set
        const char* client_ip_header_name = 
            am_web_get_client_ip_header_name(agent_config);

        // Check if client hostname header property is set
        const char* client_hostname_header_name = 
            am_web_get_client_hostname_header_name(agent_config);
        char* ip_header = NULL;
        char* hostname_header = NULL;
        char* client_ip_from_ip_header = NULL;
        char* client_hostname_from_hostname_header = NULL;

        // If client ip header property is set, then try to
        // retrieve header value.
        if(client_ip_header_name != NULL && client_ip_header_name[0] != '\0') {
            ip_header = pblock_findval(client_ip_header_name, rq->headers);

            // Usually client ip header value is: client, proxy1, proxy2....
            // Process client ip header value to get the correct value. 
            am_web_get_client_ip(ip_header,
                &client_ip_from_ip_header);
        }

        // If client hostname header property is set, then try to
        // retrieve header value.
        if(client_hostname_header_name != NULL && client_hostname_header_name[0] != '\0') {
            hostname_header = pblock_findval(client_hostname_header_name, rq->headers);

           // Usually client hostname header value is: client, proxy1, proxy2....
           // Process client hostname header value to get the correct value.
            am_web_get_client_hostname(hostname_header, 
                &client_hostname_from_hostname_header);
        }

        // If client IP value is present from above processing, then
        // set it to env_param_map. Else use from request structure.
        if(client_ip_from_ip_header != NULL && client_ip_from_ip_header[0] != '\0') {
            am_web_set_host_ip_in_env_map(client_ip_from_ip_header,
                                  client_hostname_from_hostname_header,
                                  env_parameter_map,
                                  agent_config);

            status = am_web_is_access_allowed(dpro_cookie, 
                         request_url,
                         path_info, 
                         method,
                         client_ip_from_ip_header,
                         env_parameter_map, 
                         &result, 
                         agent_config);
        } else {
            status = am_web_is_access_allowed(dpro_cookie, 
                         request_url,
                         path_info, 
                         method,
                         pblock_findval(REQUEST_IP_ADDR, sn->client),
                         env_parameter_map, 
                         &result, 
                         agent_config);
        }
        am_map_destroy(env_parameter_map);
        am_web_free_memory(client_ip_from_ip_header);
        am_web_free_memory(client_hostname_from_hostname_header);
    }

    switch (status) {
    case AM_SUCCESS:
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

            am_web_log_debug("validate_session_policy() access allowed to %s",
                             ruser);
	} else {
	    am_web_log_debug("validate_session_policy() remote user not set,"
	    " allowing access to the url as it is in not enforced list");
	}

        if (am_web_is_logout_url(request_url, agent_config) == B_TRUE) {
            (void)am_web_logout_cookies_reset(reset_cookie, args, agent_config);
        }

	// set LDAP user attributes to http header
	status = am_web_result_attr_map_set(&result, set_header,
                                               set_cookie_in_response,
                                               set_header_attr_as_cookie,
                                               get_cookie_sync, args, agent_config);
	if (status != AM_SUCCESS) {
	    am_web_log_error("am_web_result_attr_map_set failed, "
                            "status = %s (%d)",
                            am_status_to_string(status), status);
	}

	requestResult = REQ_PROCEED;
	break;

    case AM_ACCESS_DENIED:
	am_web_log_debug("validate_session_policy() access denied to %s",
			 result.remote_user ? result.remote_user :
			 "unknown user");
	requestResult = do_redirect(sn, rq, status, &result,
				    request_url, method, agent_config);
	break;

    case AM_INVALID_SESSION:
        //reset the cookie CDSSO. 
        am_web_log_info("%s: Invalid session.",thisfunc);
        if (am_web_is_cdsso_enabled(agent_config) == B_TRUE)
        {
            cdStatus = am_web_do_cookie_domain_set(set_cookie, args, EMPTY_STRING, agent_config);        
            if(cdStatus != AM_SUCCESS) {
                am_web_log_error("validate_session_policy : CDSSO reset cookie failed");
            }
        }


        am_web_do_cookies_reset(reset_cookie, args, agent_config);


        if (strcmp(method, REQUEST_METHOD_POST) == 0 &&
            B_TRUE==am_web_is_postpreserve_enabled(agent_config)) 
        {
                // Create the magic URI, actionurl
                post_urls_t *post_urls;
                post_urls = am_web_create_post_preserve_urls(request_url, 
                                                         agent_config);
                // In CDSSO mode, for a POST request, the post data have
                // already been saved in the response variable, so we need
                // to get them here only if response is NULL.
                if (response == NULL) {
                    response = get_post_data(sn, rq, request_url);
                }
                if (response != NULL && strlen(response) > 0) {
                    if (AM_SUCCESS == register_post_data(sn, rq,
                                         post_urls->action_url,
                                         post_urls->post_time_key, response, agent_config))
                    {
                        const char *lbCookieHeader = NULL;
                        // If using a LB in front of the agent, the LB cookie
                        // needs to be set there. The boolean argument allows
                        // to set the value of the cookie to the one defined in the
                        // properties file (B_FALSE) or to NULL (B_TRUE).
                        status_tmp = am_web_get_postdata_preserve_lbcookie(
                                                   &lbCookieHeader, B_FALSE, agent_config);
                        if (status_tmp == AM_SUCCESS) {
                            if (lbCookieHeader != NULL) {
                                am_web_log_debug("%s: Setting LB cookie for "
                                             "post data preservation (%s)",
                                             thisfunc, lbCookieHeader);
                                set_cookie(lbCookieHeader, args);
                            }
                            requestResult =  do_redirect(sn, rq, status, &result,
                                                 post_urls->dummy_url, method, agent_config);
                        } else {
                            am_web_log_error("%s: "
                              "am_web_get_postdata_preserve_lbcookie() "
                              "failed ", thisfunc);
                            requestResult = REQ_ABORTED;
                        }
                        if (lbCookieHeader != NULL) {
                            am_web_free_memory(lbCookieHeader);
                            lbCookieHeader = NULL;
                        }
                    } else {
                        requestResult = REQ_ABORTED;
                    }
                    // call cleanup routine
                    am_web_clean_post_urls(post_urls);
                } else {
                    am_web_log_debug("%s: AM_INVALID_SESSION. This is a POST "
                                     "request with no post data => redirecting "
                                     " as a GET request.", thisfunc);
                    requestResult = do_redirect(sn, rq, status, &result,
                               request_url, REQUEST_METHOD_GET, agent_config);
                }
            } else {
                am_web_log_debug("%s: AM_INVALID_SESSION in GET", thisfunc);
                requestResult = do_redirect(sn, rq, status, &result,
                           request_url, method, agent_config);
            }

	break;

    case AM_INVALID_FQDN_ACCESS:
        // Redirect to self with correct FQDN - no post preservation
        requestResult = do_redirect(sn, rq, status, &result,
                                    request_url, method, agent_config);
        break;

    case AM_INVALID_ARGUMENT:
    case AM_NO_MEMORY:

    case AM_REDIRECT_LOGOUT:
        status = am_web_get_logout_url(&logout_url, agent_config);
        if(status == AM_SUCCESS)
        {
            do_url_redirect(sn,rq,logout_url);
        }
        else
        {
            requestResult = REQ_ABORTED;
            am_web_log_debug("validate_session_policy(): "
				"am_web_get_logout_url failed. ");
        }
    break;
    default:
	am_web_log_error("validate_session_policy() status: %s (%d)",
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

    am_web_log_max_debug("validate_session_policy(): "
			 "Completed handling request with status: %s.",
			 am_status_to_string(status));

    if (orig_req != NULL) {
        free(orig_req);
        orig_req = NULL;
    }
    if (response != NULL) {
        free(response);
        response = NULL;
    }
    if (clf_req != NULL) {
        free(clf_req);
        clf_req = NULL;
    }
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
        log_error(LOG_FAILURE, "URL Access Agent: ", NULL, NULL,
            "Initialization of the agent failed: "
            "status = %s (%d)", am_status_to_string(status), status);
    } 
} 
