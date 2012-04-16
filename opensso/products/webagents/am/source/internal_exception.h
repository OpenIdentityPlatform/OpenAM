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
 * $Id: internal_exception.h,v 1.3 2008/06/25 08:14:32 qcheng Exp $
 *
 * Abstract: 
 *
 * Base class of all exceptions thrown in am library.
 *
 *
 */

#ifndef __INTERNAL_EXCEPTION_H__
#define __INTERNAL_EXCEPTION_H__

#include <exception>
#include <string>
#include <am_types.h>
#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE

class InternalException: public std::exception {
public:
    InternalException(const std::string &method, const std::string &message,
		      am_status_t status) throw()
        : std::exception(), throwingMethod(method), mesg(message),
          statusCode(status)
    { }

    ~InternalException() throw() {}

    virtual const char *what() const throw() { return "InternalException"; }

    const char *getThrowingMethod() const { return throwingMethod.c_str(); }

    const char *getMessage() const { return mesg.c_str(); }

    am_status_t getStatusCode() const { return statusCode; }

private:
    std::string throwingMethod;
    std::string mesg;
    am_status_t statusCode;
};

END_PRIVATE_NAMESPACE

#endif	/* not __INTERNAL_EXCEPTION_H__ */
