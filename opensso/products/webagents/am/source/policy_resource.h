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
 * $Id: policy_resource.h,v 1.3 2008/06/25 08:14:34 qcheng Exp $
 *
 */ 

#ifndef __RESOURCE_H__
#define __RESOURCE_H__

#include <cstdlib>
#include <list>
#include <string>
#include <am_policy.h>

#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE

class ResourceName {
 private:
    std::string resourceStr;
 public:

    ResourceName(const ResourceName &resName) {
	resourceStr = resName.resourceStr;
    }

    ResourceName& operator=(const std::string &resName) {
	resourceStr = resName;
	return *this;
    }

    ResourceName(const std::string &str): resourceStr(str) {
    }

    bool getResourceRoot(const am_resource_traits_t &, std::string &);

    const std::string &getString() const { return resourceStr; }
};

END_PRIVATE_NAMESPACE

#endif
