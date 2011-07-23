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
 * Abstract Functor object that needs to be derived and implemented.
 * The object will be called by the thread in the thread pool.
 *
 */

#ifndef __THREAD_FUNCTION_H__
#define __THREAD_FUNCTION_H__

#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE
class ThreadFunction {
public:
    virtual void operator()(void) const = 0;
    virtual ~ThreadFunction() { }
};
END_PRIVATE_NAMESPACE


#endif	/* not __THREAD_FUNCTION_H__ */
