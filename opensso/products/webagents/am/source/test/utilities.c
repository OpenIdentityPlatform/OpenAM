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
 * Abstract:
 *
 * Miscellanous utilities used by the test programs.
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <am.h>
#include <am_web.h>
#include <am_log.h>

#include "utilities.h"

static args_t parsed_args = {
    0,
    NULL,
    NULL,
    AM_LOG_ALL_MODULES,
    "iplanet333"
};

static void set_property(am_properties_t properties, const char *key,
			 const char *value)
{
    am_status_t status;

    status = am_properties_set(properties, key, value);
    if (AM_SUCCESS != status) {
	fatal_with_status(status, "unable to set property '%s' to '%s'", key,
			  value);
    }
}

static void outputMsg(FILE *output, const char *prefix, const char *format,
		      va_list arg_list, const char *suffix_format, ...)
{
    if (parsed_args.program) {
	fprintf(output, "%s: %s", parsed_args.program, prefix);
    } else {
	fprintf(output, prefix);
    }

    vfprintf(output, format, arg_list);

    va_start(arg_list, suffix_format);
    vfprintf(output, suffix_format, arg_list);
    va_end(arg_list);

    fprintf(output, "\n");
    fflush(output);
}

void message(const char *format, ...)
{
    va_list arg_list;

    va_start(arg_list, format);
    outputMsg(stdout, "", format, arg_list, "");
    va_end(arg_list);
}

void error(const char *format, ...)
{
    va_list arg_list;

    va_start(arg_list, format);
    outputMsg(stderr, "error: ", format, arg_list, "");
    va_end(arg_list);
}

void fatal(const char *format, ...)
{
    va_list arg_list;

    va_start(arg_list, format);
    outputMsg(stderr, "fatal: ", format, arg_list, "");
    va_end(arg_list);

    exit(EXIT_FAILURE);
}

void fatal_with_status(am_status_t status, const char *format, ...)
{
    va_list arg_list;

    va_start(arg_list, format);
    outputMsg(stderr, "fatal: ", format, arg_list, ": %d", status);
    va_end(arg_list);

    exit(EXIT_FAILURE);
}

args_t *init(int argc, char **argv)
{
    am_status_t status;
    const char *config_file = NULL;
    int i, j;

    parsed_args.program = strrchr(argv[0], '/');
    if (parsed_args.program) {
	parsed_args.program += 1;
    } else {
	parsed_args.program = argv[0];
    }

    for (i = 1; i < argc && argv[i][0] == '-'; ++i) {
	if (strcmp("--help", argv[i]) == 0) {
	    printf("usage: %s <options>\n", argv[0]);
	    printf("  --help                   print this message and exit\n");
	    printf("  --config <file>          file from which to load config info\n");
	    printf("  --def-log-level <num>    set default log level\n");
	    printf("  --log-level <mod>:<num>  set log level for module\n");
	    printf("  --password <string>      password to use for appl. login\n");
	    printf("  --ssotoken <string>      ssotoken to use for test\n");
	    printf("\n  --log-level can be repeated to set multiple modules\n");
	    exit(EXIT_SUCCESS);
	} else if (strcmp("--config", argv[i]) == 0) {
	    if (++i < argc) {
	        config_file = argv[i];
	    } else {
		fatal("missing value for %s switch", argv[i]);
	    }
	} else if (strcmp("--def-log-level", argv[i]) == 0) {
	    if (++i < argc) {
	        am_log_set_module_level(AM_LOG_ALL_MODULES,
					   strtoul(argv[i], NULL, 0));
	    } else {
		fatal("missing value for %s switch", argv[i]);
	    }
	} else if (strcmp("--log-level", argv[i]) == 0) {
	    if (++i < argc) {
		am_log_module_id_t moduleId;
		char *sepPtr = strchr(argv[i], ':');

		if (sepPtr) {
		    *sepPtr = '\0';
		}
		status = am_log_add_module(argv[i], &moduleId);
		if (AM_SUCCESS != status) {
		    fatal_with_status(status,
				      "unable to add logging module '%s'",
				      argv[i]);
		}
		if (sepPtr) {
		    am_log_set_module_level(moduleId,
					       strtoul(sepPtr+1, NULL, 0));
		} else {
		    am_log_set_module_level(moduleId, AM_LOG_MAX_DEBUG);
		}
	    } else {
		fatal("missing value for %s switch", argv[i]);
	    }
	} else if (strcmp("--password", argv[i]) == 0) {
	    if (++i < argc) {
	        parsed_args.password = argv[i];
	    } else {
		fatal("missing value for %s switch", argv[i]);
	    }
	} else if (strcmp("--ssotoken", argv[i]) == 0) {
	    if (++i < argc) {
	        parsed_args.ssotoken = argv[i];
	    } else {
		fatal("missing value for %s switch", argv[i]);
	    }
	} else {
	    fatal("unknown switch '%s'", argv[i]);
	}
    }

    j = 0;
    while (i <= argc) {
	argv[++j] = argv[i++];
    }
    parsed_args.argc = j;

    status = am_properties_create(&parsed_args.properties);
    if (AM_SUCCESS != status) {
	fatal_with_status(status, "unable to create properties object");
    }

    if (config_file != NULL) {
	status = am_properties_load(parsed_args.properties, config_file);
	if (AM_SUCCESS != status) {
	    fatal_with_status(status, "unable to load properties");
	}
    } else {
	set_property(parsed_args.properties, AM_WEB_LOGIN_URL_PROPERTY,
		     "http://piras.red.iplanet.com:8080/amserver/UI/Login");
	set_property(parsed_args.properties, AM_COMMON_COOKIE_NAME_PROPERTY,
		     "iPlanetDirectoryPro");
    }

    status = am_auth_init(parsed_args.properties);
    if (AM_SUCCESS != status) {
	fatal_with_status(status, "unable to initialize library");
    }
    status = am_policy_init(parsed_args.properties);
    if (AM_SUCCESS != status) {
	fatal_with_status(status, "unable to initialize library");
    }
    status = am_log_add_module(parsed_args.program,
				  &parsed_args.log_module);
    if (AM_SUCCESS != status) {
	fatal_with_status(status, "unable to add logging module");
    }

    return &parsed_args;
}

void cleanup(args_t *arg_ptr)
{
    am_status_t status;

    if (arg_ptr->properties) {
	am_properties_destroy(arg_ptr->properties);
	arg_ptr->properties = NULL;
    }

    status = am_cleanup();
    if (AM_SUCCESS != status) {
	fatal_with_status(status, "unable to cleanup library");
    }
}
