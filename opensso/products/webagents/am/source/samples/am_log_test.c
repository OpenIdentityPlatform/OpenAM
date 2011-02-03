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
#include <stdio.h>
#include <stdlib.h>
#include <am.h>
#include <am_log.h>
#include <string.h>

void fail_on_status(am_status_t status, 
                    am_status_t expected_status, 
                    const char *method_name) 
{
    if (status != expected_status) {
	fprintf(stderr,"\n");
	fprintf(stderr,"ERROR: %s failed with status %s.\n",
		method_name, am_status_to_name(status));
	exit(1);
    }
}

void fail_on_error(am_status_t status, const char *method_name) {
    fail_on_status(status, AM_SUCCESS, method_name);
}

void Usage(char **argv) {
    printf("Usage: %s"
    " -u <user_token_id>"
    " -n <log_name>"
    " -l <logged_by_token_id>"
    " -m <log_message>"
    " [-d <log_module>]"
    " [-f <bootstrap_properties_file>]\n",
    argv[0]);
}

void do_remote_logging(const char *loggingUrl, am_properties_t prop, const char *user_token_id, const char *log_name, const char *log_message, const char *logged_by_token_id, const char *log_module);

int main(int argc, char *argv[])
{

    const char* prop_file = "../../config/OpenSSOAgentBootstrap.properties";
    am_status_t status = AM_FAILURE;
    am_properties_t prop = AM_PROPERTIES_NULL;

    const char *user_token_id = NULL;
    const char *log_name = NULL;
    const char *log_message = NULL;
    const char *logged_by_token_id = NULL;
    const char *log_module = "TestModule";
    const char *loggingservice = "loggingservice";
    int j;
    char c;
    char *serverUrl = NULL;
    const char *loggingUrl = NULL;
    int usage = 0;

    for (j=1; j < argc; j++) {
        if (*argv[j]=='-') {
            c = argv[j][1];

            switch (c) {

                case 'f':
                    prop_file = (j <= argc-1) ? argv[++j] : NULL;
                    break;

                case 'u':
                    user_token_id = (j <= argc-1) ? argv[++j] : NULL;
                    break;

                case 'n':
                    log_name = (j <= argc-1) ? argv[++j] : NULL;
                    break;

                case 'l':
                    logged_by_token_id = (j <= argc-1) ? argv[++j] : NULL;
                    break;

                case 'm':
                    log_message = (j <= argc-1) ? argv[++j] : NULL;
                    break;

                case 'd':
                    log_module = (j <= argc-1) ? argv[++j] : NULL;
                    break;

                default:
                    usage++;
                    break;

            }
            if (usage) {
                break;
            }
        } // end if
    } //end for

    if (usage || 
        prop_file == NULL || user_token_id == NULL || 
	log_name == NULL || logged_by_token_id == NULL || 
	log_message == NULL || log_module == NULL) 
    {
        Usage(argv);
        exit(0);
    }

    status = am_properties_create(&prop);
    fail_on_error(status, "am_properties_create");

    status = am_properties_load(prop, prop_file);
    fail_on_error(status, "am_properties_load");

    status = am_log_init(prop);
    fail_on_error(status, "am_log_init");

    serverUrl = (char *) malloc(512);
    memset(serverUrl,0,512);

    do_remote_logging(serverUrl, prop, user_token_id, log_name, log_message, logged_by_token_id, log_module);

    if (serverUrl != NULL) {
        free(serverUrl);
    }

    exit(0);
}

void do_remote_logging(const char *loggingUrl, am_properties_t prop, const char *user_token_id, const char *log_name, const char *log_message, const char *logged_by_token_id, const char *log_module)
{
    char *tmpUrl = NULL;
    am_status_t status = AM_FAILURE;
    boolean_t log_result = B_FALSE;
    am_log_record_t log_record = NULL;

    if (loggingUrl != NULL) {
	status = am_properties_get(prop, 
		   "com.sun.identity.agents.config.naming.url", &loggingUrl);
        if (strlen(loggingUrl) == 0) {
	    // The c-sdk is 2.2, the naming url property is different
	    status = am_properties_get(prop, "com.sun.am.naming.url", 
                                       &loggingUrl);
        }
	if ((status == AM_SUCCESS) && (loggingUrl != NULL)) {

	   tmpUrl = strstr(loggingUrl, "namingservice");
	   if (tmpUrl != NULL) {
              strcpy(tmpUrl,"loggingservice");

              /* log a message with fixed SSO Token ID */
              status = am_log_set_remote_info(loggingUrl,logged_by_token_id,
                                              log_name, prop);
              fail_on_error(status, "am_log_set_remote_info");

              log_result = am_log_log(AM_LOG_REMOTE_MODULE, AM_LOG_AUTH_REMOTE, 
                                      log_message);
           }
        }
    }
    if (log_result != B_TRUE) {
	printf("Log message failed.\n");
	exit(1);
    }

    /* get log record */
    status = am_log_record_create(&log_record, 
				  AM_LOG_LEVEL_INFORMATION, log_message);
    fail_on_error(status, "am_log_record_create");

    if (user_token_id != NULL) {
        status = am_log_record_populate(log_record, user_token_id);
        fail_on_error(status, "am_log_record_populate");
    }

    status = am_log_record_add_loginfo(log_record, MODULE_NAME, log_module);
    fail_on_error(status, "am_log_record_add_loginfo");

    /* log a log record */
    status = am_log_log_record(log_record, log_name, logged_by_token_id);
    fail_on_error(status, "am_log_log_record");

    /* flush log to remote IS */
    status = am_log_flush_remote_log();
    fail_on_error(status, "am_log_flush_remote_log");

    /* log again and wait for shutdown to call flush */
    status = am_log_log_record(log_record, log_name, logged_by_token_id);
    fail_on_error(status, "am_log_log_record");

    status = am_log_record_destroy(log_record);
    fail_on_error(status, "am_log_record_destroy");

    status = am_cleanup();
    fail_on_error(status, "am_cleanup");

    if (status == AM_SUCCESS) {
        printf("Logging Completed!\n");
    }

}

