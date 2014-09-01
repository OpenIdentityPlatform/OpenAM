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
 * $Id: agent_module_factory.h,v 1.3 2009/08/18 22:50:19 robertis Exp $
 *
 *
 */
/*
 * Portions Copyrighted 2012 ForgeRock AS
 */

#ifndef __AGENT_MODULE_FACTORY_H__
#define __AGENT_MODULE_FACTORY_H__

#include "agent_module.h"

// This class creates instances of CAgentModule for each request.

class CAgentModuleFactory : public IHttpModuleFactory {
public:

    virtual HRESULT GetHttpModule(OUT CHttpModule **ppModule, IN IModuleAllocator *) {
        HRESULT hr = S_OK;
        CAgentModule * pModule = NULL;

        if (ppModule == NULL) {
            hr = HRESULT_FROM_WIN32(ERROR_INVALID_PARAMETER);
            goto Finished;
        }

        pModule = new CAgentModule();
        if (pModule == NULL) {
            hr = HRESULT_FROM_WIN32(ERROR_NOT_ENOUGH_MEMORY);
            goto Finished;
        }

        *ppModule = pModule;
        pModule = NULL;

Finished:

        if (pModule != NULL) {
            delete pModule;
            pModule = NULL;
        }

        return hr;
    }

    virtual void Terminate() {
        TerminateAgent();
        delete this;
    }
};

#endif
