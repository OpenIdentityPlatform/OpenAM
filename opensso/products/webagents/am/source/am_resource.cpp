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
 * $Id: am_resource.cpp,v 1.3 2008/06/25 08:14:28 qcheng Exp $
 *
 */ 
/*
 * Portions Copyrighted 2014 ForgeRock AS
 */

#include <cctype>
#include "policy_resource.h"
#include "utils.h"

USING_PRIVATE_NAMESPACE

bool
ResourceName::getResourceRoot(const am_resource_traits_t &rsrcTraits,
			      std::string &str) 
{
    size_t maxlen = resourceStr.size();
    char *rootResName = new char[resourceStr.size() + 1];
    boolean_t result;
    bool retVal = false;

    if(rootResName == NULL)
	return retVal;

    try {
	result = rsrcTraits.get_resource_root(resourceStr.c_str(),
					    rootResName, maxlen);
	if(result == B_TRUE) {
	    str = rootResName;
	    retVal = true;
	}

	delete []rootResName;
    } catch(...) {
	delete []rootResName;
    }
    return retVal;
}
