/* -*- Mode: C -*-
 *
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
 * $Id: am_notify.h,v 1.2 2008/06/25 05:48:54 qcheng Exp $
 */


#ifndef AM_NOTIFY_H
#define AM_NOTIFY_H

#include <am_policy.h>

AM_BEGIN_EXTERN_C

/*
 * This function should be called by the service listening on the 
 * notification URL given in the properties file if notification is enabled.
 *
 * It parses the XML message and calls SSO Token listeners and policy 
 * notification handlers accordingly. 
 *
 * Parameters:
 *   xmlmsg
 *		XML message containing the notification message.
 * 
 *   policy_handle_t
 *              The policy handle created from am_policy_service_init().
 * 
 *              NULL if policy is not initialized or not used.
 *
 * Returns:
 *   AM_SUCCESS 
 *              if XML message was successfully parsed and processed.
 * 
 *   AM_INVALID_ARGUMENT
 *		if any input parameter is invalid.
 * 
 *   AM_ERROR_PARSING_XML
 *              if there was an error parsing the XML message.
 *
 *   AM_ERROR_DISPATCH_LISTENER
 *              if there was an error dispatching the listener(s).
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t 
am_notify(const char *xmlmsg, am_policy_t policy_handle);

AM_END_EXTERN_C

#endif
