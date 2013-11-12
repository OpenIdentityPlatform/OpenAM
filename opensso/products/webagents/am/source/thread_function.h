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
 * $Id: thread_function.h,v 1.3 2008/06/25 08:14:39 qcheng Exp $
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

#ifndef __THREAD_FUNCTION_H__
#define __THREAD_FUNCTION_H__

#include "internal_macros.h"
#include <string>

BEGIN_PRIVATE_NAMESPACE
class ThreadFunction {
private:
    bool exitNow;
    std::string name;
public:

    ThreadFunction(std::string name = "ThreadFunction",
            bool exitNow = false) : name(name), exitNow(exitNow) {
    }
    virtual void operator()(void) const = 0;

    virtual ~ThreadFunction() {
    }

    const std::string& getName() const {
        return name;
    }

    bool exit() {
        return exitNow;
    }
};

class WorkerThreadExit : public ThreadFunction {
public:

    WorkerThreadExit() : ThreadFunction("WorkerThreadExit", true) {
    }

    void operator()() const {
    }
};

END_PRIVATE_NAMESPACE

#endif	/* not __THREAD_FUNCTION_H__ */
