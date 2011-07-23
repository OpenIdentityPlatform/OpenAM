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
 * These class together provide thread-safe, strongly typed, reference
 * counting pointers.
 */ 

#ifndef REF_CNT_PTR_H
#define REF_CNT_PTR_H

#include "internal_macros.h"
#include "mutex.h"
#include "nspr_exception.h"

BEGIN_PRIVATE_NAMESPACE

//
// This is the base class of all reference counted objects.  It provides
// the synchronized bookkeeping functionality needed by the pointer class.
//
// NOTE: The two constructors will throw NSPRExceptions if they are unable
// to create the required PRLock objects.
//
class RefCntObj {
public:
    RefCntObj();
    RefCntObj(const RefCntObj& rhs);

    virtual ~RefCntObj() = 0;

    RefCntObj& operator=(const RefCntObj& rhs);

    void addRef();
    void removeRef();

private:
    Mutex lock;
    PRInt32 refCnt;
};

inline RefCntObj::RefCntObj()
    : lock(), refCnt(0)
{
}

inline RefCntObj::RefCntObj(const RefCntObj&)
    : lock(), refCnt(0)
{
}

inline RefCntObj::~RefCntObj()
{
}

inline RefCntObj& RefCntObj::operator=(const RefCntObj&)
{
    return *this;
}


//
// This template is used to instantiate reference counted pointers to any
// data type.
//
// NOTE:  The class does not provide a conversion operator to the "dumb"
// version of the pointer to avoid a variety of situations that can result
// in corruption of the reference count.  As a result, users of the this
// class must use the '.' (dot) operator when dereference a smart pointer.
//
template<typename T> class RefCntPtr {
public:
    explicit RefCntPtr(T *realPtr = NULL);
    RefCntPtr(const RefCntPtr& rhs);
    ~RefCntPtr();

    RefCntPtr& operator=(const RefCntPtr& rhs);
    RefCntPtr& operator=(T *rhs);

    T& operator*() const { return *pointer; }
    T *operator->() const { return pointer; }

    struct NestedObj {}; // Empty nested class used for pointer conversion.

    //
    // The following conversion will allow you to test a RefCntPtr
    // against NULL.
    //
    operator const NestedObj *() const { return reinterpret_cast<NestedObj *>(pointer); }

private:
    T *pointer;
};

template<typename T>
RefCntPtr<T>::RefCntPtr(T* realPointer)
    : pointer(realPointer)
{
    if (static_cast<T *>(NULL) != pointer) {
	pointer->addRef();
    }
}

template<typename T>
RefCntPtr<T>::RefCntPtr(const RefCntPtr<T>& rhs)
    : pointer(rhs.pointer)
{
    if (static_cast<T *>(NULL) != pointer) {
	pointer->addRef();
    }
}

template<typename T>
RefCntPtr<T>::~RefCntPtr()
{
    if (static_cast<T *>(NULL) != pointer) {
	pointer->removeRef();
    }
}

template<typename T>
RefCntPtr<T>& RefCntPtr<T>::operator=(const RefCntPtr<T>& rhs)
{
    if (pointer != rhs.pointer) {
	//
	// The reference to the right-hand object must be incremented
	// before decrementing the reference to the left-hand object
	// in case the assignment being performed is of the form:
	//
	// lhs = lhs->next;
	//
	if (static_cast<T *>(NULL) != rhs.pointer) {
	    rhs.pointer->addRef();
	}
	if (static_cast<T *>(NULL) != pointer) {
	    pointer->removeRef();
	}
	pointer = rhs.pointer;
    }

    return *this;
}

template<typename T>
RefCntPtr<T>& RefCntPtr<T>::operator=(T *rhs)
{
    if (static_cast<T *>(NULL) != rhs) {
	rhs->addRef();
    }
    if (static_cast<T *>(NULL) != pointer) {
	pointer->removeRef();
    }
    pointer = rhs;

    return *this;
}

END_PRIVATE_NAMESPACE

#endif	/* not REF_CNT_PTR_H */
