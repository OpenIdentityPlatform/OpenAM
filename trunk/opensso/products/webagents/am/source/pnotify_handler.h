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
 * $Id: pnotify_handler.h,v 1.3 2008/06/25 08:14:34 qcheng Exp $
 *
 * Abstract:
 *
 * Policy notification handler.  This class is a functor object
 * derived from HTFunction.  Each hashtable element will be called.
 *
 */

#ifndef __PNOTIFY_HANDLER_H__
#define __PNOTIFY_HANDLER_H__
#include <string>
#include "internal_macros.h"
#include "policy_entry.h"
#include "hash_table.h"
#include "log.h"

BEGIN_PRIVATE_NAMESPACE

class PolicyNotificationHandler :public HTFunction<PolicyEntryRefCntPtr> {
  private:
    std::string resName;
    NotificationType nType;
    Log::ModuleId logID;
  public:
    PolicyNotificationHandler(const std::string &rName,
			      NotificationType type):resName(rName),
						     nType(type) {
	logID = Log::addModule("Notification");


	/**
	 * Hack to overcome the problem in the server side when they send
	 * the policy notification.  The resource name contains a \n in the
	 * beginning.
	 * TO BE REMOVED after the Bug 4696623 gets fixed : Piras
	 */
	if(!isalpha(rName[0])) {
	    const char *str = rName.c_str();
	    while(!isalpha(*str++));
	    resName = --str;
	}

    }
    virtual void operator()(PolicyEntryRefCntPtr);
    virtual ~PolicyNotificationHandler() {
    }
};


END_PRIVATE_NAMESPACE
#endif	/* not __PNOTIFY_HANDLER_H__ */
