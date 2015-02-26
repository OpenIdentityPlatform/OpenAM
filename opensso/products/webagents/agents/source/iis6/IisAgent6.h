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
 *
 */
/*
 * Portions Copyrighted 2012 ForgeRock AS
 */

#include <windows.h>
#include <httpext.h>

//   Size from minimum maximum of cookies per host in section 5.3 of:
//   http://www.ietf.org/rfc/rfc2965.txt
#define COOKIES_SIZE_MAX (20*4096)

// Tcp port numbers are 16 bits or 5 ascii decimal digits.
// See http://www.ietf.org/rfc/rfc793.txt
#define TCP_PORT_ASCII_SIZE_MAX 5

void log_primitive(CHAR *);
am_status_t get_header_value(EXTENSION_CONTROL_BLOCK *, const char *, char **, BOOL, BOOL);
char* string_case_insensitive_search(char *, char *);
