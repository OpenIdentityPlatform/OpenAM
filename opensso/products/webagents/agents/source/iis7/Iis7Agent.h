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
 * $Id: Iis7Agent.h,v 1.3 2010/03/10 05:08:52 dknab Exp $
 *
 *
 */
/*
 * Portions Copyrighted 2012 - 2014 ForgeRock AS
 */

#ifndef IIS7AGENT_H
#define IIS7AGENT_H

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <io.h>
#include <stdio.h>
#include <httpserv.h>
#include <stdarg.h>
#include <string>
#include <vector>
#include "am_web.h"

/*
 * This class has implementation for all of the module functionality
 * for all of the server events registered.
 **/
class CAgentModule : public CHttpModule {
public:

    CAgentModule(IAppHostAdminManager *admin, HANDLE eventh) {
        InitializeCriticalSection(&initLock);
        eventLog = eventh;
        adminMgr = admin;
    }

    ~CAgentModule() {
        DeleteCriticalSection(&initLock);
    }

    REQUEST_NOTIFICATION_STATUS OnBeginRequest(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnAuthenticateRequest(IN IHttpContext *pHttpContext,
            IN OUT IAuthenticationProvider *pProvider);

    REQUEST_NOTIFICATION_STATUS OnEndRequest(IN IHttpContext *pHttpContext,
            IN OUT IHttpEventProvider *pProvider);

private:

    am_status_t loadAgentPropertyFile(IHttpContext * pHttpContext);

    void reportEvent(const char *format, va_list argList) {
        int count = _vscprintf(format, argList);
        if (count > 0) {
            std::vector<char> formattedStringBuff(count + 1);
            char *formattedString = &formattedStringBuff.front();
            vsprintf(formattedString, format, argList);
            if (eventLog != NULL) {
                ReportEvent(eventLog, EVENTLOG_INFORMATION_TYPE, 0, 0,
                        NULL, 1, 0, (LPCTSTR *) & formattedString, NULL);
            }
        }
    }

    void WriteEventViewerLog(const char *format, ...) {
        va_list args;
        va_start(args, format);
        reportEvent(format, args);
        va_end(args);
    }

    HANDLE eventLog;
    IAppHostAdminManager *adminMgr;
    PCSTR userName;
    PCWSTR userPassword;
    DWORD userPasswordSize;
    PCWSTR userPasswordCrypted;
    DWORD userPasswordCryptedSize;
    BOOL showPassword;
    BOOL doLogOn;
    CRITICAL_SECTION initLock;

};

/*
 * This class creates instances of the CHttpModule for each request.
 **/
class CAgentModuleFactory : public IHttpModuleFactory {
public:

    CAgentModuleFactory(IAppHostAdminManager *admin) {
        eventLog = RegisterEventSource(NULL, "IISADMIN");
        adminMgr = admin;
    }

    ~CAgentModuleFactory() {
        if (eventLog != NULL) {
            DeregisterEventSource(eventLog);
            eventLog = NULL;
        }
    }

    virtual HRESULT GetHttpModule(OUT CHttpModule **ppModule, IN IModuleAllocator *) {
        CAgentModule *pModule = NULL;

        if (ppModule == NULL) {
            return HRESULT_FROM_WIN32(ERROR_INVALID_PARAMETER);
        }

        pModule = new CAgentModule(adminMgr, eventLog);
        if (pModule == NULL) {
            return HRESULT_FROM_WIN32(ERROR_NOT_ENOUGH_MEMORY);
        }

        *ppModule = pModule;
        pModule = NULL;
        return S_OK;
    }

    virtual void Terminate() {
        am_agent_cleanup();
        am_web_cleanup();
        am_shutdown_nss();
        delete this;
    }

private:

    IAppHostAdminManager *adminMgr;
    HANDLE eventLog;
};

/*
 * This class provides request-specific information about a user.
 * This information includes data such as credentials and role-based authorization.
 **/
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
        if (InterlockedDecrement(&m_refs) <= 0) {
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
