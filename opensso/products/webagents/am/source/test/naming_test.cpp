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
 */ 
#include <stdexcept>

#include "am.h"

#include "naming_service.h"
#include "session_service.h"

#include "utilities.h"
#include "utilities_cpp.h"

USING_PRIVATE_NAMESPACE

int main(int argc, char **argv)
{
    am_status_t status;
    args_t *arg_ptr = init(argc, argv);
    const Properties *propPtr;

    propPtr = reinterpret_cast<Properties *>(arg_ptr->properties);

    try {
	bool trustCerts = propPtr->getBool(
                              AM_COMMON_TRUST_SERVER_CERTS_PROPERTY, true);
	NamingService namingSvc(propPtr,propPtr->get(AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),propPtr->get(AM_AUTH_CERT_ALIAS_PROPERTY,""),trustCerts);
	NamingInfo namingInfo;
	SessionService sessionSvc("SessionService",
                                  propPtr,
                                  propPtr->get(AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),
                                  propPtr->get(AM_AUTH_CERT_ALIAS_PROPERTY,""),
                                  trustCerts);

	std::string namingURL = propPtr->get(AM_COMMON_NAMING_URL_PROPERTY,
					     "http://piras.red.iplanet.com:8080/amserver/namingservice");

        Http::CookieList cookieList;
	status = namingSvc.getProfile(namingURL, arg_ptr->ssotoken,
				      cookieList, namingInfo);
	if (AM_SUCCESS == status) {
	    message("successfully retrieved profile, validating session");

	    Log::setModuleLevel(Log::addModule("SessionService"), Log::LOG_DEBUG);
	    status = sessionSvc.validateSession(namingInfo.getSessionSvcInfo(),
					SSOToken(arg_ptr->ssotoken, ""));
	    if (AM_SUCCESS == status) {
		message("successfully validated session");
	    } else {
		fatal_with_status(status, "unable to validate session");
	    }
	} else {
	    fatal_with_status(status, "failed to retrieve profile");
	}
    } catch (const std::exception& exc) {
	fatal("caught an exception", exc.what());
    }

    cleanup(arg_ptr);

    return EXIT_SUCCESS;
}
