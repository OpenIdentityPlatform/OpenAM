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
 * $Id: am_utils.cpp,v 1.3 2008/06/25 08:14:29 qcheng Exp $
 *
 */ 

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

#include <stdexcept>
#include <am_utils.h>
#include "http.h"


USING_PRIVATE_NAMESPACE

extern "C"
am_status_t
am_http_cookie_encode(const char *cookie, char *buf, int len)
{
    am_status_t sts = AM_SUCCESS;
    if (NULL==cookie || NULL==buf || len <= 0) {
        sts = AM_INVALID_ARGUMENT;
    }
    else {
	try {
	    std::string encoded = Http::encode(std::string(cookie));
	    const char *encoded_str = encoded.c_str();
	    if ((int)strlen(encoded_str) >= len)
		sts = AM_BUFFER_TOO_SMALL;
	    else 
		strcpy(buf, encoded_str);
	}
        catch (std::invalid_argument& ex) {
	    sts = AM_INVALID_ARGUMENT;
        }
        catch (...) {
	    sts = AM_FAILURE;
        }
   }
   return sts;
}

extern "C"
am_status_t
am_http_cookie_decode(const char *cookie, char *buf, int len)
{
    am_status_t sts = AM_SUCCESS;
    if (NULL==cookie || NULL==buf || len <= 0) {
        sts = AM_INVALID_ARGUMENT;
    }
    else {
        try {
            std::string decoded = Http::decode(std::string(cookie));
            const char *decoded_str = decoded.c_str();
            if ((int)strlen(decoded_str) >= len)
                sts = AM_BUFFER_TOO_SMALL;
            else 
                strcpy(buf, decoded_str);
	}
        catch (std::invalid_argument& ex) {
	    sts = AM_INVALID_ARGUMENT;
        }
        catch (...) {
	    sts = AM_FAILURE;
        }
   }
   return sts;
}
