/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: agent_module.cpp,v 1.2 2009/06/03 00:25:32 robertis Exp $
 *
 *
 */

#include <stdio.h> 
#include "precomp.h"
#include "Iis7Agent.h"
#include "agent_module.h"

/*
 * This is the function invoked by IIS during the first event
 * in the request processing pipeline. Here, we implement the 
 * agent functionality.
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnBeginRequest(IN IHttpContext* pHttpContext, 
            IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    status = ProcessRequest(pHttpContext, pProvider);
    return status;

}

/*
 * This function is invoked when IIS establishes the identity of the user
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnAuthenticateRequest(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}


/*
 * Invoked when IIS verifies user authorization
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnAuthorizeRequest(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}

/*
 * Invoked when IIS resolves a request in the cache.
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnResolveRequestCache(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}

/*
 * Invoked when IIS retrieves the state of the request.
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnAcquireRequestState(IN IHttpContext* pHttpContext, 
        IN OUT IHttpEventProvider * pProvider)
{
    HRESULT hr = S_OK;
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}


/*
 * Invoked when IIS maps request to an event handler.
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnMapRequestHandler(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}


/*
 * Invoked before IIS executes a request handler.
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnPreExecuteRequestHandler(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}


/*
 * Invoked when IIS executes a request handler.
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnExecuteRequestHandler(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}


/*
 * Invoked when the current state is released.
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnReleaseRequestState(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}



/*
 * Invoked when IIS stores the request in the cache.
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnUpdateRequestCache(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}



/*
 * Invoked when IIS prepares to log the current request.
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnLogRequest(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}


/*
 * This is the function invoked by IIS during the last event
 * in the request processing pipeline. 
 *
 * */
REQUEST_NOTIFICATION_STATUS
CAgentModule::OnEndRequest(IN IHttpContext* pHttpContext, 
                        IN OUT IHttpEventProvider * pProvider)
{
    REQUEST_NOTIFICATION_STATUS status = RQ_NOTIFICATION_CONTINUE;
    return status;

}

