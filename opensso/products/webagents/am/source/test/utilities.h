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
 * Miscellaneous utilities used by the test programs.
 *
 */

#ifndef UTILITIES_H
#define UTILITIES_H

#include <am_log.h>
#include <am_properties.h>

AM_BEGIN_EXTERN_C

typedef struct args {
    int argc;
    const char *program;
    am_properties_t properties;
    am_log_module_id_t log_module;
    const char *password;
    const char *ssotoken;
} args_t;

void fatal(const char *format, ...);
void fatal_with_status(am_status_t status, const char *format, ...);
void error(const char *format, ...);
void message(const char *format, ...);

args_t *init(int argc, char **argv);
void cleanup(args_t *arg_ptr);

AM_END_EXTERN_C

#endif	/* not UTILITIES_H */
