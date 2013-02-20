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

#include <am.h>
#include <am_log.h>

typedef void (*am_log_logger_func_t)(const char *moduleName,
                                     am_log_level_t level,
                                     const char *msg);

extern am_log_set_logger(const am_log_logger_func_t logger_func,
	                                 am_log_logger_func_t *old_func);

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

void
logger_func(const char *module_name, am_log_level_t level, const char *msg)
{
    fprintf(stderr, "TEST LOGGER: %s %d \"%s\"\n", module_name, level, msg); 
}

void Usage(char **argv) {
    printf("Usage: %s"
           " [-f properties_file]"
           "\n",
           argv[0]);
}

int
main(int argc, char *argv[])
{
    const char* prop_file = "../../config/OpenSSOAgentBootstrap.properties";
    am_status_t status = AM_FAILURE;
    am_properties_t prop = AM_PROPERTIES_NULL;
    am_log_module_id_t module_id;
    int j;
    char c;
    int usage = 0;

    for (j=1; j < argc; j++) {
        if (*argv[j]=='-') {
            c = argv[j][1];
            switch (c) {
	    case 'f':
                prop_file = (j <= argc-1) ? argv[++j] : NULL;
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

    if (usage || prop_file == NULL) {
        Usage(argv);
        return EXIT_FAILURE;
    }

    // create properties
    status = am_properties_create(&prop);
    fail_on_error(status, "am_properties_create");

    // load properties
    status = am_properties_load( prop, prop_file );
    fail_on_error(status, "am_properties_load");

    // init log 
    status = am_log_init(prop);
    fail_on_error(status, "am_log_init");

    // add module 
    status = am_log_add_module("TestModule", &module_id);
    fail_on_error(status, "am_log_add_module");

    status = am_log_set_logger(logger_func, NULL);
    fail_on_error(status, "am_log_set_logger");

    am_log_log(module_id, AM_LOG_INFO, "See this %d %s.", 1, "log");
    am_log_log(module_id, AM_LOG_INFO, "See this log.");

    (void)am_cleanup();
    am_properties_destroy(prop);

    return EXIT_SUCCESS;
}  /* end of main procedure */

