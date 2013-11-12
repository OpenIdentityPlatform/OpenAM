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
 * $Id: ref_cnt_ptr.h,v 1.3 2008/06/25 08:14:35 qcheng Exp $
 *
 * Abstract:
 *
 * These class together provide thread-safe, strongly typed, reference
 * counting pointers.
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef REF_CNT_PTR_H
#define REF_CNT_PTR_H

#include "internal_macros.h"
#include "mutex.h"

BEGIN_PRIVATE_NAMESPACE

//
// This is the base class of all reference counted objects.  It provides
// the synchronized bookkeeping functionality needed by the pointer class.
//
class RefCntObj {
public:
    RefCntObj();
    virtual ~RefCntObj() = 0;

    RefCntObj& operator=(const RefCntObj& rhs);

    void addRef();
    void removeRef();

private:
    Mutex lock;
    unsigned int refCnt;
};

inline RefCntObj::RefCntObj() : lock(), refCnt(0) {
}

inline RefCntObj::~RefCntObj() {
}

inline RefCntObj& RefCntObj::operator=(const RefCntObj&) {
    return *this;
}


//
// This template is used to instantiate reference counted pointers to any
// data type. Follows an implementation of the "safe bool idiom".
//

template<typename T> class RefCntPtr {
private:
    T *pointer;
    typedef T* RefCntPtr::*unspecified_bool_type;

public:
    explicit RefCntPtr(T *realPtr = 0);
    RefCntPtr(const RefCntPtr& rhs);
    ~RefCntPtr();

    RefCntPtr& operator=(const RefCntPtr& rhs);
    RefCntPtr& operator=(T *rhs);

    T& operator*() const {
        return *pointer;
    }

    T *operator->() const {
        return pointer;
    }

    operator unspecified_bool_type() const {
        return pointer != 0 ? &RefCntPtr::pointer : 0;
    }
};

template<typename T>
RefCntPtr<T>::RefCntPtr(T* realPointer) : pointer(realPointer) {
    if (pointer) {
        pointer->addRef();
    }
}

template<typename T>
RefCntPtr<T>::RefCntPtr(const RefCntPtr<T>& rhs) : pointer(rhs.pointer) {
    if (pointer) {
        pointer->addRef();
    }
}

template<typename T>
RefCntPtr<T>::~RefCntPtr() {
    if (pointer) {
        pointer->removeRef();
    }
    pointer = 0;
}

template<typename T>
RefCntPtr<T>& RefCntPtr<T>::operator=(const RefCntPtr<T>& rhs) {
    if (pointer == rhs.pointer) {
        return *this;
    }

    T* tmp_ptr = pointer;
    pointer = rhs.pointer;
    if (pointer) {
        pointer->addRef();
    }
    // removeRef second to prevent any deletion of any object which might
    // be referenced by the other object. i.e RefCntPtr is child of the
    // original pointer.
    if (tmp_ptr) {
        tmp_ptr->removeRef();
    }
    return *this;
}

template<typename T>
RefCntPtr<T>& RefCntPtr<T>::operator=(T *rhs) {
    if (pointer == rhs) {
        return *this;
    }

    T* tmp_ptr = pointer;
    pointer = rhs;
    if (pointer) {
        pointer->addRef();
    }
    if (tmp_ptr) {
        tmp_ptr->removeRef();
    }
    return *this;
}

END_PRIVATE_NAMESPACE

#endif	/* not REF_CNT_PTR_H */
