/* -*- Mode: C -*- */
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: am_string_set.h,v 1.4 2008/08/19 19:11:37 veiming Exp $
 */

/*
 * Abstract:
 *
 * Common types and macros provided by the OpenSSO 
 * Access Management SDK.
 *
 */

#ifndef AM_STRING_SET_H
#define AM_STRING_SET_H

#include <am_types.h>

typedef struct {
    int size;
    char **strings;
} am_string_set_t;

AM_BEGIN_EXTERN_C

/*
 * Allocate space for a am_string_set_t and space for size strings. 
 * also initializes size to the given size.
 *
 * Parameters:
 *     size
 *         size of set to allocate.
 *
 * Returns: 
 *     a pointer to allocated am_string_set_t, or NULL if size is < 0 (invalid).
 * 
 */
AM_EXPORT am_string_set_t * 
am_string_set_allocate(int size);

/*
 * Frees memory held by the parameter, by freeing each
 * string in the set of strings, followed by the strings pointer,
 * followed by the struct itself.
 *
 * Parameters: 
 *     string_set 
 *         the am_string_set_t pointer to be freed.
 *
 * Returns: 
 *     None
 *
 */
AM_EXPORT void 
am_string_set_destroy(am_string_set_t *string_set);

AM_END_EXTERN_C

#endif	/* not AM_STRING_SET_H */
