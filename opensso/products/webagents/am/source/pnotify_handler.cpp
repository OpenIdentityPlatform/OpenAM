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
 * $Id: pnotify_handler.cpp,v 1.3 2008/06/25 08:14:34 qcheng Exp $
 *
 * Abstract:
 *
 * Policy notification handler.  This class is a functor object
 * derived from HTFunction.  Each hashtable element will be called.
 *
 */
#include "internal_macros.h"
#include "pnotify_handler.h"
#include "scope_lock.h"

USING_PRIVATE_NAMESPACE

void
PolicyNotificationHandler::operator()(PolicyEntryRefCntPtr elem) {
    ScopeLock myLock(elem->lock);
    const SSOToken &ssoToken = elem->getSSOToken();
    const std::string &ssoStr = ssoToken.getString();
    const char *notifType = NULL;
    bool foundOneTree = false;
    std::list<Tree *>::const_iterator iter = elem->forest.begin();

    for(;iter != elem->forest.end(); iter++) {
	Tree *tree = *iter;


	if(tree != NULL && tree->isInTree(resName)) {
	    foundOneTree = true;
	    // Process notification message.
	    switch(nType) {
	    case NOTIFICATION_ADD:
		{
		    PDRefCntPtr pol(new PolicyDecision(resName));
		    tree->insert(pol);
		    notifType = NOTIF_TYPE_ADDED;
		    break;
		}
	    case NOTIFICATION_DELETE:
		tree->outdatePolicyDecisions(resName);
		notifType = NOTIF_TYPE_DELETED;
		break;
	    case NOTIFICATION_MODIFY:
		{
		    if (!tree->markStale(resName, true)) {
			// insert a node and mark if as stale
			// if we cannot find it.
			PDRefCntPtr pol(new PolicyDecision(resName));
			tree->insert(pol);
			tree->markStale(resName, true);
		    }
		    notifType = NOTIF_TYPE_MODIFIED;
		    break;
		}
	    default:
		notifType = "UNKNOWN";
		break;
	    }
	}
    }

    if(foundOneTree == false) {
	Log::log(logID, Log::LOG_DEBUG,
		 "PolicyNotificationHandler::operator(): No tree "
		 "exists for resource '%s'.", resName.c_str());
	return;
    }

    Log::log(logID, Log::LOG_MAX_DEBUG,
	     "PolicyNotificationHandler::operator(): "
	     "Resource: %s : NotifType: %s PolicyEntry: %s.",
	     resName.c_str(),
	     notifType, ssoStr.c_str());

    return;
}

