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
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>

#include <am_web.h>

typedef struct {
    char user[50];
    am_web_req_method_t method;
    char request_hdrs[500];
    char response_hdrs[500];
    char data[100];
    am_web_result_t http_result;
} process_info_t;


void fail_on_status(am_status_t status, 
                    am_status_t expected_status, 
                    const char *method_name) 
{
    if (status != expected_status) {
	fprintf(stderr,"\n");
	fprintf(stderr,"ERROR: %s failed with status %s.\n",
		method_name, am_status_to_name(status));
	exit(EXIT_FAILURE);
    }
}

void fail_on_error(am_status_t status, const char *method_name) 
{
    fail_on_status(status, AM_SUCCESS, method_name);
}

void Usage(char **argv) {
    printf("Usage: %s"
           " -u url"
           " [-f properties_file (default: ./OpenSSOAgentBootstrap.properties)]"
	   " [-m method (default: GET)]"
	   " [-p path info]"
	   " [-q query]"
           " [-i client IP (default: 127.0.0.1)]"
           " [-s sso token id]"
           " [-c cookie header value]"
           " [-o file containing post data (default: stdin)]"
           "\n",
           argv[0]);
}


int
get_http_code(am_web_result_t web_result)
{
    int ret = -1;
    switch(web_result) {
    case AM_WEB_RESULT_OK:
	ret = 200;
	break;
    case AM_WEB_RESULT_OK_DONE:
	ret = 201;
	break;
    case AM_WEB_RESULT_REDIRECT:
	ret = 302;
	break;
    case AM_WEB_RESULT_FORBIDDEN:
	ret = 403;
	break;
    case AM_WEB_RESULT_ERROR:
	ret = 500;
	break;
    };
    return ret;
}

char *
get_http_code_str(am_web_result_t web_result)
{
    char *ret = "UNKNOWN";
    switch(web_result) {
    case AM_WEB_RESULT_OK:
	ret = "OK";
	break;
    case AM_WEB_RESULT_OK_DONE:
	ret = "OK-DONE";
	break;
    case AM_WEB_RESULT_REDIRECT:
	ret = "REDIRECT";
	break;
    case AM_WEB_RESULT_FORBIDDEN:
	ret = "FORBIDDEN";
	break;
    case AM_WEB_RESULT_ERROR:
	ret = "ERROR";
	break;
    };
    return ret;
}

am_status_t
render_result(void **args, am_web_result_t http_result, char *data)
{
    am_status_t sts = AM_SUCCESS;
    process_info_t *pinfo = NULL;

    if (args == NULL || 
	(pinfo = (process_info_t *)args[0]) == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	pinfo->http_result = http_result;

	printf("\n");
	printf("-----------------------------------------------------------\n");

	printf("HTTP Result:\t%d %s\n", 
		    get_http_code(pinfo->http_result),
		    get_http_code_str(pinfo->http_result));
	printf("User:\t'%s'\n", pinfo->user);
	printf("method:\t'%s'\n", am_web_method_num_to_str(pinfo->method));
	printf("request headers:\n\t{\n%s\t}\n", pinfo->request_hdrs);
	printf("response headers:\n\t{\n%s\t}\n", pinfo->response_hdrs);
	printf("data:\t'%s'\n", data == NULL ? "NULL" : data);
	printf("-----------------------------------------------------------\n");
	sts = AM_SUCCESS;
    }
    return sts;
}

am_status_t
set_user(void **args, const char *user)
{
    am_status_t sts = AM_SUCCESS;
    process_info_t *pinfo = NULL;
	
    if (args == NULL || 
	(pinfo = (process_info_t *)args[0]) == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	if (user != NULL)
	    strcpy(pinfo->user, user);
	sts = AM_SUCCESS;
    }
    return sts;
}

am_status_t
set_method(void **args, am_web_req_method_t method)
{
    am_status_t sts = AM_SUCCESS;
    process_info_t *pinfo = NULL;

    if (args == NULL || 
	(pinfo = (process_info_t *)args[0]) == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	pinfo->method = method;
	sts = AM_SUCCESS;
    }
    return sts;
}

am_status_t
set_header_in_request(void **args, const char *key, const char *val)
{
    am_status_t sts = AM_SUCCESS;
    process_info_t *pinfo = NULL;

    if (args == NULL || 
	(pinfo = (process_info_t *)args[0]) == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	strcat(pinfo->request_hdrs, "\t");
	strcat(pinfo->request_hdrs, key == NULL ? "NULL" : key);
	    strcat(pinfo->request_hdrs, ": ");
	strcat(pinfo->request_hdrs, val == NULL ? "NULL" : val);
	strcat(pinfo->request_hdrs, "\n");
	sts = AM_SUCCESS;
    }
    return sts;
}

am_status_t
add_header_in_response(void **args, const char *key, const char *val)
{
    am_status_t sts = AM_SUCCESS;
    process_info_t *pinfo = NULL;

    if (args == NULL || 
	(pinfo = (process_info_t *)args[0]) == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	strcat(pinfo->response_hdrs, "\t");
	strcat(pinfo->response_hdrs, key == NULL ? "NULL" : key);
	strcat(pinfo->response_hdrs, ": ");
	strcat(pinfo->response_hdrs, val == NULL ? "NULL" : val);
	strcat(pinfo->response_hdrs, "\n");
	sts = AM_SUCCESS;
    }
    return sts;
}

#define BUF_SIZE 2048

am_status_t
get_post_data(void **args, char **data) 
{
    am_status_t sts = AM_SUCCESS;
    char *postdata_file = NULL;
    FILE *postdata_fp = NULL;
    char *buf = NULL;
    int nread = 0;

    if (args == NULL || data == NULL) {
	sts = AM_INVALID_ARGUMENT;
    }
    else {
	if ((postdata_file = (char *)args[0]) == NULL) {
	    // read from stdin
	    postdata_fp = stdin;
	    fprintf(stdout, 
		    "\n>>> Enter Post data followed by a ^D:\n");
	}
	else {
	    postdata_fp = fopen(postdata_file, "r");
	    if (postdata_fp == NULL) {
		fprintf(stderr, 
			"Could open file %s containing POST data. "
			"Error %s.\n",
			postdata_file, strerror(errno));
		sts = AM_NOT_FOUND;
	    }
	}
	if (postdata_fp != NULL) {
	    buf = calloc(BUF_SIZE, 1);  // post data not expected to exceed 2K
	    if (buf == NULL) {
		fprintf(stderr, "Could not allocate memory for POST data: "
			"out of memory.\n");
		sts = AM_NO_MEMORY;
	    }
	    else {
		nread = fread(buf, 1, BUF_SIZE-1, postdata_fp);
		*data = buf;
		sts = AM_SUCCESS;
	    }
	}
    }
    return sts;
}

am_status_t
free_post_data(void **args, char *data) 
{
    am_status_t sts = AM_SUCCESS;
    if (data != NULL) {
	free((void *)data);
    }
    return sts;
}

int
main(int argc, char *argv[])
{
    const char* prop_file = "../../config/OpenSSOAgentBootstrap.properties";
    am_status_t status = AM_FAILURE;
    char *ssoTokenID = NULL;
    char *url = NULL;
    char *query = NULL;
    char *pathInfo = NULL;
    char *method = "GET";
    char *clientIP = "127.0.0.1";
    char *cookie_header = NULL;
    char *postdata_file = NULL;
    int j;
    char c;
    int usage = 0;

    for (j=1; j < argc; j++) {
        if (*argv[j]=='-') {
            c = argv[j][1];
            switch (c) {
	    case 'u':
                url = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'f':
                prop_file = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 's':
                ssoTokenID = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'p':
                pathInfo = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'q':
                query = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'm':
                method = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'i':
                clientIP = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'c':
                cookie_header = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'o':
                postdata_file = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    default:
		usage++;
		break;
	    }
	    if (usage)
		break;
        }
        else {
            usage++;
            break;
        }
    }

    if (usage || NULL==url) {
        Usage(argv);
        return EXIT_FAILURE;
    }

    // load properties
    // initialize web agent
    // status = am_web_init(prop_file);
    fail_on_error(status, "am_web_init");

    // do access check 
    {
	am_status_t sts = AM_SUCCESS;
	am_web_result_t result;
	am_web_request_params_t req_params;
	am_web_request_func_t req_func;
	process_info_t pinfo;
	void *args[1] = { (void *)&pinfo };	
	void *get_post_data_args[1] = { (void *)postdata_file };
	char buf[200];

	memset((void *)&req_params, 0, sizeof(req_params));
	memset((void *)&req_func, 0, sizeof(req_func));
	memset((void *)&pinfo, 0, sizeof(pinfo));
	memset((void *)buf, 0, sizeof(buf));

	req_params.url = url;
	req_params.query = query;
	req_params.method = am_web_method_str_to_num(method);
	req_params.path_info = pathInfo;
	req_params.client_ip = clientIP;

	// insert sso token into cookie header val if provided.
	if (ssoTokenID != NULL) {
	    //sprintf(buf, "%s=%s;%s",
	//	    am_web_get_cookie_name(), ssoTokenID, 
	//	    cookie_header == NULL ? "" : cookie_header);
	 //   req_params.cookie_header_val = buf;
	}
	else {
	    req_params.cookie_header_val = cookie_header;
	}

	req_func.get_post_data.func = get_post_data;
	req_func.get_post_data.args = get_post_data_args;
	req_func.free_post_data.func = free_post_data;
	req_func.free_post_data.args = NULL;
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

	//result = am_web_process_request(&req_params, &req_func, &sts);

	printf("am_web_process_request() returned status %s, result %s.\n",
		    am_status_to_name(sts), get_http_code_str(result));
    }

    printf("\nCleaning up...");
    (void)am_cleanup();
    printf("Done.\n\n");

    return EXIT_SUCCESS;
}  /* end of main procedure */

