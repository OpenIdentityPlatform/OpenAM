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
 * $Id: agent_main.cpp,v 1.2 2009/06/03 00:25:32 robertis Exp $
 *
 *
 */

#include "precomp.h"
#include "Iis7Agent.h"

IHttpServer *                       g_pHttpServer = NULL;

PVOID                               g_pModuleContext = NULL;

/*
 * IIS calls this function when the agent module DLL is loaded.
 * It registers for the server events.
 *
 * */
HRESULT
__stdcall
RegisterModule(
    DWORD                           dwServerVersion,
    IHttpModuleRegistrationInfo *   pModuleInfo,
    IHttpServer *                   pHttpServer
)
{
    HRESULT                             hr = S_OK;
    CAgentModuleFactory  *             pFactory = NULL;

    if ( pModuleInfo == NULL || pHttpServer == NULL )
    {
        hr = HRESULT_FROM_WIN32( ERROR_INVALID_PARAMETER );
        goto Finished;
    }

    g_pModuleContext = pModuleInfo->GetId();
    g_pHttpServer = pHttpServer;

    pFactory = new CAgentModuleFactory();
    if ( pFactory == NULL )
    {
        hr = HRESULT_FROM_WIN32( ERROR_NOT_ENOUGH_MEMORY );
        goto Finished;
    }

    hr = pModuleInfo->SetRequestNotifications( pFactory, 
            RQ_BEGIN_REQUEST 
            | RQ_AUTHORIZE_REQUEST 
            | RQ_RESOLVE_REQUEST_CACHE 
            | RQ_ACQUIRE_REQUEST_STATE 
            | RQ_PRE_EXECUTE_REQUEST_HANDLER 
            | RQ_EXECUTE_REQUEST_HANDLER 
            | RQ_RELEASE_REQUEST_STATE 
            | RQ_UPDATE_REQUEST_CACHE 
            | RQ_LOG_REQUEST 
            | RQ_END_REQUEST 
            , 0 );

    hr = pModuleInfo->SetPriorityForRequestNotification(RQ_BEGIN_REQUEST,
                                        PRIORITY_ALIAS_HIGH);

    if ( FAILED( hr ) )
    {
        goto Finished;
    }

    RegisterAgentModule();


    pFactory = NULL;

Finished:
    
    if ( pFactory != NULL )
    {
        delete pFactory;
        pFactory = NULL;
    }   

    return hr;
}
