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
 * $Id: xml_utilities.h,v 1.5 2009/08/27 21:41:30 subbae Exp $
 *
 * Abstract:
 *
 * Minor utilities used by the XML parsing classes.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef XML_UTILITIES_H
#define XML_UTILITIES_H

#include <string>
#include <assert.h>
#ifdef _MSC_VER
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <objbase.h>
#include <oleauto.h>
#import <msxml6.dll> named_guids
#include <msxml6.h>
#else
#include <strings.h>
#include <libxml/tree.h>
#endif

#include "internal_macros.h"
#include "log.h"

BEGIN_PRIVATE_NAMESPACE

#if defined(SOLARIS) || defined(LINUX) || defined(HPUX) || defined(AIX)
inline bool matchesXMLString(const std::string& str1, const xmlChar *str2) {
    return (0 == strcasecmp(str1.c_str(),
            reinterpret_cast<const char *> (str2)));
}
#elif defined(_MSC_VER)

inline std::string bstrToString(const BSTR bstr, int cp = CP_UTF8) {
    if (!bstr) return "";
    int len = WideCharToMultiByte(cp, 0, bstr, -1, NULL, 0, NULL, NULL);
    std::string r(len, '\0');
    WideCharToMultiByte(cp, 0, bstr, -1, &r[0], len, NULL, NULL);
    return r;
}

inline bool matchesXMLString(const std::string& str1, const BSTR bstr) {
    std::string tmp = bstrToString(bstr);
    /* skip namespace prefix if any */
    tmp.erase(0, tmp.find(":") + 1);
    return (0 == _stricmp(str1.c_str(), tmp.c_str()));
}

#else
#error "don't know how to do case-insensitive comparison on this platform "
#endif

END_PRIVATE_NAMESPACE

#endif	/* not XML_UTILITIES_H */
