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
#include <string.h>

#include <am_properties.h>

#include "utilities.h"

#define	SUFFIX	".new"

int main(int argc, char **argv)
{
    int exit_status = EXIT_SUCCESS;
    am_status_t status;
    am_properties_t properties;
    args_t *arg_ptr;

    arg_ptr = init(argc, argv);

    status = am_properties_create(&properties);
    if (AM_SUCCESS == status) {
	if (arg_ptr->argc > 1) {
	    status = am_properties_load(properties, argv[1]);
	    if (AM_SUCCESS == status) {
		size_t len = strlen(argv[1]) + sizeof(SUFFIX);
		char *new_file_name = malloc(len);
		strcpy(new_file_name, argv[1]);
		strcat(new_file_name, SUFFIX);

		status = am_properties_store(properties, new_file_name);
		if (AM_SUCCESS != status) {
		    fatal_with_status(status,
				      "unable to store properties in '%s'",
				      new_file_name);
		}
	    } else {
		fatal_with_status(status,
				  "unable to load properties from '%s'",
				  argv[1]);
	    }
	} else {
	    const char *key = "foo";

	    status = am_properties_set(properties, key, "bar");
	    if (AM_SUCCESS == status) {
		message("successfully created properties and inserted entry");
	    } else {
		fatal_with_status(status, "unable to insert into properties");
	    }
	}
	am_properties_destroy(properties);
    } else {
	fatal_with_status(status, "unable to create properties");
    }

    cleanup(arg_ptr);

    return exit_status;
}
