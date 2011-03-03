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

#ifndef XML_UTILITIES_H
#define XML_UTILITIES_H

#include <string>

#if	defined(SOLARIS) || defined(LINUX)
#include <strings.h>
#elif	(defined(WINNT) || defined(_AMD64_))
#endif

#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE

inline bool matchesXMLString(const std::string& str1, const xmlChar *str2)
{
#if	defined(SOLARIS) || defined(LINUX) || defined(HPUX) || defined(AIX)
    return (0 == strcasecmp(str1.c_str(),
			    reinterpret_cast<const char *>(str2)));
#elif	(defined(WINNT) || defined(_AMD64_))
    return (0 == _stricmp(str1.c_str(), reinterpret_cast<const char *>(str2)));
#else
#error "don't know how to do case-insensitve comparison on this platform "
#endif
}

END_PRIVATE_NAMESPACE

#endif	/* not XML_UTILITIES_H */
