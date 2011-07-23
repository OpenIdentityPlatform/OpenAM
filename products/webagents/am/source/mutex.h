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
 * Abstract:
 *
 * Mutual exclusion synchronization object.
 *
 */ 

#ifndef MUTEX_H
#define MUTEX_H

#if	defined(DEBUG)
#include <cassert>
#endif

#include <prlock.h>

#include "nspr_exception.h"
#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE

class Mutex {
public:
    //
    // Creates a new mutex object.
    //
    // Throws:
    //   NSPRException
    //		if the underlying lock object cannot be created.
    //
    Mutex();

    //
    // Destroys the object.
    //
    ~Mutex();

    //
    // Locks the mutex object.  If the object is currently locked then,
    // the caller will block until the lock becomes available.
    //
    // NOTE: These locks are not recursive.  If a thread holds a lock
    // and attempts to lock the same lock again, the thread will deadlock.
    //
    void lock();

    //
    // Unlocks the mutex object.  The behavior is undefined if the
    // current thread does not hold the lock.
    //
    void unlock();

private:
    Mutex(const Mutex& rhs);	// not implemented
    Mutex& operator=(const Mutex& rhs);	// not implemented

    PRLock *lockPtr;
};

inline Mutex::Mutex()
    : lockPtr(PR_NewLock())
{
    if (NULL == lockPtr) {
	throw NSPRException("Mutex()", "PR_NewLock");
    }
}

inline Mutex::~Mutex()
{
    PR_DestroyLock(lockPtr);
}

inline void Mutex::lock()
{
    PR_Lock(lockPtr);
}

inline void Mutex::unlock()
{
#if	defined(DEBUG)
    assert(PR_SUCCESS == PR_Unlock(lockPtr));
#else
    PR_Unlock(lockPtr);
#endif
}

END_PRIVATE_NAMESPACE

#endif	/* not MUTEX_H */
