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
 * $Id: scope_lock.h,v 1.3 2008/06/25 08:14:35 qcheng Exp $
 *
 *
 * Abstract:
 *
 * Provides class to automate locking and unlocking of Mutex objects.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef SCOPE_LOCK_H
#define SCOPE_LOCK_H

#include "internal_macros.h"
#include "mutex.h"

BEGIN_PRIVATE_NAMESPACE

class ScopeLock {
public:

    explicit ScopeLock(Mutex& lockArg, bool lck = true) : mlock(lockArg), locked(false) {
        if (lck) {
            lock();
        }
    }

    ~ScopeLock() {
        if (locked) {
            mlock.unlock();
        }
    }

    void lock() {
        locked = true;
        mlock.lock();
    }

    void unlock() {
        locked = false;
        mlock.unlock();
    }

private:
    ScopeLock(const ScopeLock& rhs); // not implemented
    ScopeLock& operator=(const ScopeLock& rhs); // not implemented

    Mutex& mlock;
    bool locked;
};

END_PRIVATE_NAMESPACE

#endif	// not SCOPE_LOCK_H
