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
 * $Id: am_utils.h,v 1.2 2008/06/25 05:48:55 qcheng Exp $
 */

#ifndef __AM_UTILS_H__
#define __AM_UTILS_H__

#include <am.h>

AM_BEGIN_EXTERN_C

/*
 * URL encodes a HTTP cookie.
 *
 * Parameters:
 *   cookie
 *	the cookie to be URL encoded.
 *   buf
 *      the buffer to put the encoded cookie
 *   len
 *      the size of the buffer 
 *
 * Returns:
 *   AM_SUCCESS
 *      if the cookie was successfully encoded and copied into buf.
 *
 *   AM_INVALID_ARGUMENT
 *      if the cookie or buffer was NULL.
 *
 *   AM_BUFFER_TOO_SMALL
 *      if len was smaller than the size of the encoded value.
 *
 *   AM_FAILURE
 *      other error ocurred while encoding cookie.
 */
AM_EXPORT am_status_t
am_http_cookie_encode(const char *cookie, char *buf, int len);

/*
 * URL decodes a HTTP cookie.
 *
 * Parameters:
 *   cookie
 *	the cookie to be URL decoded.
 *   buf
 *      the buffer to put the decoded cookie
 *   len
 *      the size of the buffer 
 *
 * Returns:
 *   AM_SUCCESS
 *      if the cookie was successfully decoded and copied into buf.
 *
 *   AM_INVALID_ARGUMENT
 *      if the cookie or buffer was NULL.
 *
 *   AM_BUFFER_TOO_SMALL
 *      if len was smaller than the size of the decoded value.
 *
 *   AM_FAILURE
 *      other error ocurred while decoding cookie.
 */
AM_EXPORT am_status_t
am_http_cookie_decode(const char *cookie, char *buf, int len);

AM_END_EXTERN_C

#endif /*__AM_UTILS_H__*/
