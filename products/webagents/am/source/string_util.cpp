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
 * $Id: string_util.cpp,v 1.3 2008/06/25 08:14:38 qcheng Exp $
 *
 */ 

#include <new>
#include <stdexcept>
#include <string>

#include "string_util.h"

BEGIN_PRIVATE_NAMESPACE

char *CopyStringNoThrow(const char *src, std::size_t len)
{
    char *dest;

    if (static_cast<const char *>(NULL) != src) {
	dest = new (std::nothrow) char[len + 1];

	if (static_cast<char *>(NULL) != dest) {
	    std::memcpy(dest, src, len);
	    dest[len] = '\0';
	}
    } else {
	dest = static_cast<char *>(NULL);
    }

    return dest;
}

char *CopyStringNoThrow(const char *src)
{
    char *result;

    if (static_cast<const char *>(NULL) != src) {
	result = CopyStringNoThrow(src, strlen(src));
    } else {
        result = static_cast<char *>(NULL);
    }

    return result;
}

/* Throws std::invalid_argument if any argument is invalid */
char *CopyString(const char *src, std::size_t len)
{
    char *dest;

    if (static_cast<const char *>(NULL) != src) {
	dest = new char[len + 1];

	std::memcpy(dest, src, len);
	dest[len] = '\0';
    } else if (len > 0) {
	throw std::invalid_argument(std::string("src == NULL and len > 0"));
    } else {
	dest = static_cast<char *>(NULL);
    }

    return dest;
}

char *CopyString(const char *src)
{
    char *result;

    if (static_cast<const char *>(NULL) != src) {
	result = CopyString(src, strlen(src));
    } else {
        result = static_cast<char *>(NULL);
    }

    return result;
}

END_PRIVATE_NAMESPACE
