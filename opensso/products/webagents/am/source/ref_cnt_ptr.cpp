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
 * $Id: ref_cnt_ptr.cpp,v 1.3 2008/06/25 08:14:35 qcheng Exp $
 *
 */ 
#include "ref_cnt_ptr.h"
#include "scope_lock.h"

USING_PRIVATE_NAMESPACE

void RefCntObj::addRef()
{
    ScopeLock myLock(lock);

    refCnt += 1;
}

void RefCntObj::removeRef()
{
    bool shouldDelete;

    {
	//
	// The nested scope here is used to ensure that we unlock the lock
	// before we delete the object containing it.
	//
	ScopeLock myLock(lock);

	shouldDelete = (--refCnt == 0);
    }

    if (shouldDelete) delete this;
}
