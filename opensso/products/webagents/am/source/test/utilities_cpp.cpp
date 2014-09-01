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
 *
 *
 * Abstract:
 *
 * Miscellanous utilities used by the test programs.
 *
 */ 

#include <stdexcept>

#include "am.h"

#include "auth_svc.h"
#include "properties.h"
#include "sso_token.h"

#include "internal_macros.h"
#include "utilities.h"
#include "utilities_cpp.h"

USING_PRIVATE_NAMESPACE

SSOToken *doAnonymousLogin(args_t *arg_ptr)
{
    am_status_t status;
    const Properties *propPtr;
    SSOToken *ssoTokenPtr = new SSOToken();

    propPtr = reinterpret_cast<Properties *>(arg_ptr->properties);
    try {
	AuthService *authSvc = 
            new AuthService(propPtr->get(AM_COMMON_COOKIE_NAME_PROPERTY),
			    propPtr,
			    propPtr->get(AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),
			    propPtr->get(AM_AUTH_CERT_ALIAS_PROPERTY,""),
			    propPtr->getBool(
                            AM_COMMON_TRUST_SERVER_CERTS_PROPERTY, true));
	ServiceInfo service(propPtr->get(AM_POLICY_LOGIN_URL_PROPERTY));

	status = authSvc->doAnonymousLogin(service, *ssoTokenPtr);
        // delete(authSvc);  
	if (AM_SUCCESS != status) {
	    fatal_with_status(status, "anonymous login attempt failed");
	}
    } catch (const std::exception& exc) {
	fatal("caught an exception: %s", exc.what());
    }

    return ssoTokenPtr;
}

SSOToken *doApplicationLogin(args_t *arg_ptr)
{
    am_status_t status;
    const Properties *propPtr;
    SSOToken *ssoTokenPtr = new SSOToken();

    propPtr = reinterpret_cast<Properties *>(arg_ptr->properties);
    try {
	AuthService *authSvc =
            new AuthService(propPtr->get(AM_COMMON_COOKIE_NAME_PROPERTY),
			    propPtr,
			    propPtr->get(AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),
			    propPtr->get(AM_AUTH_CERT_ALIAS_PROPERTY,""),
			    propPtr->getBool(
                                AM_COMMON_TRUST_SERVER_CERTS_PROPERTY, true));
	ServiceInfo service(propPtr->get(AM_POLICY_LOGIN_URL_PROPERTY));

	status = authSvc->doApplicationLogin(service, "UrlAccessAgent",
					    arg_ptr->password, *ssoTokenPtr);
        // delete(authSvc);
	if (AM_SUCCESS != status) {
	    fatal_with_status(status, "application login attempt failed");
	}
    } catch (const std::exception& exc) {
	fatal("caught an exception: %s", exc.what());
    }

    return ssoTokenPtr;
}

extern "C" const char *doCAnonymousLogin(args_t *arg_ptr) {
    return doAnonymousLogin(arg_ptr)->getString().c_str();
}


extern "C" const char *doCApplicationLogin(args_t *arg_ptr) {
    return doApplicationLogin(arg_ptr)->getString().c_str();
}
