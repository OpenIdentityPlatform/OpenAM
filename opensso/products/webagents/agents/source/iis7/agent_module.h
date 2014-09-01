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
 * $Id: agent_module.h,v 1.2 2009/06/03 00:25:32 robertis Exp $
 *
 *
 */
/*
 * Portions Copyrighted 2012 ForgeRock AS
 */

#ifndef __AGENT_MODULE_H__
#define __AGENT_MODULE_H__

#include "Iis7Agent.h"

/*
 *This class has implementation for all the module functionality
 *for all the server events registered.
 **/

class CAgentModule : public CHttpModule {
public:

    REQUEST_NOTIFICATION_STATUS OnBeginRequest(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnAuthenticateRequest(IN IHttpContext *pHttpContext,
            IN OUT IAuthenticationProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnAuthorizeRequest(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnResolveRequestCache(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnAcquireRequestState(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnMapRequestHandler(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnPreExecuteRequestHandler(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnExecuteRequestHandler(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnReleaseRequestState(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnUpdateRequestCache(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnLogRequest(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnEndRequest(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);


private:

    PCSTR userName;
    PCWSTR userPassword;
    DWORD userPasswordSize;
    PCWSTR userPasswordCrypted;
    DWORD userPasswordCryptedSize;
    BOOL showPassword;
    BOOL doLogOn;

};

class OpenAMUser : public IHttpUser {
public:

    virtual PCWSTR GetRemoteUserName(VOID) {
        return userName;
    }

    virtual PCWSTR GetUserName(VOID) {
        return userName;
    }

    virtual PCWSTR GetAuthenticationType(VOID) {
        return L"OpenAM";
    }

    virtual PCWSTR GetPassword(VOID) {
        return showPassword ? userPassword : L"";
    }

    virtual HANDLE GetImpersonationToken(VOID) {
        return hToken;
    }

    VOID SetImpersonationToken(HANDLE tkn) {
        hToken = tkn;
    }

    virtual HANDLE GetPrimaryToken(VOID) {
        return NULL;
    }

    virtual VOID ReferenceUser(VOID) {
        InterlockedIncrement(&m_refs);
    }

    virtual VOID DereferenceUser(VOID) {
        if (0 == InterlockedDecrement(&m_refs)) {
            if (hToken) CloseHandle(hToken);
            delete this;
        }
    }

    virtual BOOL SupportsIsInRole(VOID) {
        return FALSE;
    }

    virtual HRESULT IsInRole(IN PCWSTR pszRoleName, OUT BOOL * pfInRole) {
        return E_NOTIMPL;
    }

    virtual PVOID GetUserVariable(IN PCSTR pszVariableName) {
        return NULL;
    }

    OpenAMUser(PCWSTR usrn, PCWSTR usrp, PCWSTR usrpcrypted,
            BOOL showpass, BOOL dologon) : userName(usrn), userPassword(usrpcrypted), showPassword(showpass), status(FALSE), error(0) {
        HANDLE tkn = NULL;
        m_refs = 1;
        if (dologon) {
            if (usrn != NULL && usrp != NULL) {
                status = LogonUserW(usrn, NULL, usrp,
                        LOGON32_LOGON_NETWORK, LOGON32_PROVIDER_DEFAULT, &tkn);
                error = GetLastError();
                if (status) {
                    SetImpersonationToken(tkn);
                }
            } else {
                error = ERROR_INVALID_DATA;
            }
        } else {
            SetImpersonationToken(tkn);
            status = TRUE;
        }
    }

    BOOL GetStatus() {
        return status;
    }

    DWORD GetError() {
        return error;
    }

private:

    LONG m_refs;
    PCWSTR userName;
    PCWSTR userPassword;
    HANDLE hToken;
    BOOL status;
    BOOL showPassword;
    DWORD error;

    virtual ~OpenAMUser() {
    }
};

#endif
