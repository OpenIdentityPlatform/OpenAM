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
 */ 
#ifndef NSPR_EXCEPTION_H
#define NSPR_EXCEPTION_H

#include <exception>

#include <prerror.h>

#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE

class NSPRException: public std::exception {
public:
    NSPRException(const char *method, const char *nsprOperation,
		  PRErrorCode errorCode = PR_GetError()) throw()
	: std::exception(), throwingMethod(method), nsprMethod(nsprOperation),
	  nsprError(errorCode)
    { }

    NSPRException(const NSPRException& rhs)
	: std::exception(rhs), throwingMethod(rhs.throwingMethod),
          nsprMethod(rhs.nsprMethod), nsprError(rhs.nsprError)
    { }

    ~NSPRException() throw() {}

    virtual const char *what() const throw() { return "NSPRException"; }

    const char *getThrowingMethod() const { return throwingMethod; }

    const char *getNsprMethod() const { return nsprMethod; }

    PRErrorCode getErrorCode() const { return nsprError; }

private:
    const char *throwingMethod;
    const char *nsprMethod;
    PRErrorCode nsprError;
};

END_PRIVATE_NAMESPACE

#endif	/* not NSPR_EXCEPTION_H */
