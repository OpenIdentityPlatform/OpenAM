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
 * $Id: sso_token_listener_thrfunc.h,v 1.3 2008/06/25 08:14:38 qcheng Exp $
 *
 * Abstract:
 *
 * Abstract Functor object that needs to be derived and implemented.
 * The object will be called by the thread in the thread pool.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef __SSO_TOKEN_LISTENER_THRFUNC_H__
#define __SSO_TOKEN_LISTENER_THRFUNC_H__

#include "internal_macros.h"
#include "scope_lock.h"
#include "am_sso.h"

BEGIN_PRIVATE_NAMESPACE
class SSOTokenListenerThreadFunc : public ThreadFunction {
    friend class SSOTokenListenerEntry;
    friend class SSOTokenService;
private:
    SessionInfo mSessionInfo;
    am_sso_token_listener_func_t mListener;
    void *mArgs;
    am_sso_token_event_type_t mEventType;
    time_t mEventTime;
    bool mDispatchInSepThread;


public:

    SSOTokenListenerThreadFunc(am_sso_token_listener_func_t listener,
            void *args,
            bool dispatchInSepThread) : ThreadFunction("SSOTokenListenerThreadFunc"),
    mSessionInfo(),
    mListener(listener),
    mArgs(args),
    mEventType(AM_SSO_TOKEN_EVENT_TYPE_UNKNOWN),
    mEventTime(0),
    mDispatchInSepThread(dispatchInSepThread) {
    }

    SSOTokenListenerThreadFunc(const SSOTokenListenerThreadFunc& copy) :
    mSessionInfo(copy.mSessionInfo),
    mListener(copy.mListener),
    mArgs(copy.mArgs),
    mEventType(copy.mEventType),
    mEventTime(copy.mEventTime),
    mDispatchInSepThread(copy.mDispatchInSepThread) {
    }

    const void callListener() const {
        mListener((const am_sso_token_handle_t) &mSessionInfo,
                mEventType, mEventTime, mArgs);
    }

    void operator()(void) const {
        callListener();
    }

    ~SSOTokenListenerThreadFunc() {
    }

    void setEvent(am_sso_token_event_type_t eventType, time_t eventTime) {
        mEventType = eventType;
        mEventTime = eventTime;
        return;
    }

    void updateSessionInfo(XMLElement element) {
        mSessionInfo.parseAttributes(element);
        mSessionInfo.parseXML(element);
        return;
    }

    const am_sso_token_listener_func_t getListener() {
        return mListener;
    }

    bool doDispatchInSepThread() const {
        return mDispatchInSepThread;
    }

};

END_PRIVATE_NAMESPACE


#endif	
