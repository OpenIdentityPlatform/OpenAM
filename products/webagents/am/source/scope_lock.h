/* The contents of this file are subject to the terms
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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 *
 *
 * Abstract:
 *
 * Provides class to automate locking and unlocking of PRLock objects.
 *
 */

#ifndef SCOPE_LOCK_H
#define SCOPE_LOCK_H

#include "internal_macros.h"
#include "mutex.h"

BEGIN_PRIVATE_NAMESPACE

class ScopeLock {
public:
    explicit ScopeLock(Mutex& lockArg);
    ~ScopeLock();

private:
    ScopeLock(const ScopeLock& rhs); // not implemented
    ScopeLock& operator=(const ScopeLock& rhs);	// not implemented

    Mutex& lock;
};

inline ScopeLock::ScopeLock(Mutex& lockArg)
    : lock(lockArg)
{
    lock.lock();
}

inline ScopeLock::~ScopeLock()
{
    lock.unlock();
}

END_PRIVATE_NAMESPACE

#endif	// not SCOPE_LOCK_H
