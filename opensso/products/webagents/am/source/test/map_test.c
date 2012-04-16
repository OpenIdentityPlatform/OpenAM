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

#include <am_map.h>

#include "utilities.h"

int main(int argc, char **argv)
{
    am_status_t status;
    am_map_t map = NULL;

    status = am_map_create(&map);
    if (AM_SUCCESS == status) {
	const char *key;
	const char *value;

	if (argc > 1) {
	    key = argv[1];
	} else {
	    key = "foo";
	}
	status = am_map_insert(map, key, "bar", 1);
	if (AM_SUCCESS == status) {
	    message("successfully created map and inserted entry");
	    value = am_map_find_first_value(map, key);
	    if (value) {
		message("looking up %s, found: %s", key, value);
	    } else {
		error("unable to find value for inserted key: %s", key);
	    }
	} else {
	    fatal_with_status(status, "unable to insert into map");
	}
	am_map_destroy(map);
    } else {
	fatal("unable to create map");
    }

    return EXIT_SUCCESS;
}
