/* -*- Mode: C++ -*-
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
 * $Id: am_string_set.cpp,v 1.3 2008/06/25 08:14:29 qcheng Exp $
 *
 */ 
#include <stdlib.h>
#include <am_string_set.h>

extern "C" 
am_string_set_t * 
am_string_set_allocate(int size)
{
    am_string_set_t *ret = NULL;
    if (size >= 0) {
        ret = (am_string_set_t *)malloc(sizeof(am_string_set_t));
        ret->size = size;
        if (size > 0) 
            ret->strings = (char **)malloc(size*sizeof(char *));
        else
            ret->strings = NULL;
    }
    return ret;
}

extern "C"
void 
am_string_set_destroy(am_string_set_t *string_set) 
{
    int i;
    if (string_set != NULL) {
	if (string_set->strings) {
	    for (i=0; i < string_set->size; i++) {
		if (string_set->strings[i])
		    free((void *)string_set->strings[i]);
	    }
	    free((void *)string_set->strings);
	}
	free((void *)string_set);
    }
    return;
}

