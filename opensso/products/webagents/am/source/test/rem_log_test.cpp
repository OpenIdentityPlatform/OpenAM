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
#include <iostream>
#include <cstdlib>
#include <exception>
#include <am_auth.h>

#include "properties.h"
#include "log.h"
#include "service_info.h"
#include "sso_token.h"
#include "http.h"
#include "auth_svc.h"
#include "auth_context.h"
#include "log_service.h"

#define REM_LOG_NAME "amLog";
#define REM_LOG_URL "http://firefly.red.iplanet.com:58080/amserver/loggingservice";
#define SSO_TOKEN_ID "xxx";  // invalid sso token id.
#define LOCAL_LOG_FILE "./rem_log_test_log";
#define PROP_FILE "./rem_log_test.properties";
#define MESG "testing remote log \" \t testing remote log";

using std::cout;
using std::endl;
using std::string;

using smi::Log;
using smi::Properties;
using smi::SSOToken;
using smi::ServiceInfo;
using smi::Http::CookieList;
using smi::InternalException;
using smi::AuthService;
using smi::AuthContext;

void usage(char *progname)
{
    cout << "Usage: " << progname
         << " [-r <remote log service url>]"
         << " [-u <user>]"
         << " [-p <password>]"
         << " [-s <sso token id>]"
         << " [-n <remote log name>]"
         << " [-f <properties file>]"
         << " [-l <local log file>]"
         << " [-m <log message>]"
         << endl;
}

void
log_in(Properties& props, const char *user, const char *pw, const char **sso_token_id)
throw(InternalException)
{
    AuthService *authSvc = new AuthService(props);
    string org = string("dc=iplanet,dc=com");
    string certname = string("");
    string url = string("");

    AuthContext *authCtx = new AuthContext(org, certname, url);
    authSvc->create_auth_context(*authCtx);
    authSvc->login(*authCtx, AM_AUTH_INDEX_MODULE_INSTANCE, (char *)"LDAP");
    std::size_t n = authSvc->getNumRequirements(*authCtx);
    std::vector<am_auth_callback_t>& callbacks = authCtx->getRequirements();
    for (int i = 0; i < n; i++) {
	switch(callbacks[i].callback_type) {
	    case NameCallback:
		callbacks[i].callback_info.name_callback.response = user;
		break;
	    case PasswordCallback:
		callbacks[i].callback_info.password_callback.response = pw;
		break;
	    default:
		/* ignore */
		break;
	}
    }
    authSvc->submitRequirements(*authCtx);
    *sso_token_id = authCtx->getSSOToken().data();
    cout << "SSO Token ID '" << *sso_token_id << "'" << endl;
}

const char *
init(char *prop_file, char *user, char *pw, const char **sso_token_id) 
{
    const char *error = NULL;
    Properties props;
    try {
        am_status_t status = props.load(prop_file);
        if (status != AM_SUCCESS) {
	    error = am_status_to_string(status);
	}
	else {
	    Log::initialize(props);
            if (user != NULL && pw != NULL) {
                log_in(props, user, pw, sso_token_id);
            }
	}
    }
    catch (InternalException& iex) {
	error = iex.getMessage();
    }
    catch (std::exception& ex) {
	error = ex.what();
    }
    catch (...) {
	error = "Unknown exception.";
    }
    return error;
}

const char *
run_test(char *rem_log_url, const char *sso_token_id, char *rem_log_name, char *mesg)
{
    const char *error = NULL;
    
    try {
	smi::LogService *newLogSvc =
			new smi::LogService(ServiceInfo(rem_log_url),
					    SSOToken(sso_token_id, ""),
					    smi::Http::CookieList(),
					    rem_log_name,
					    Properties(),Properties().get(AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),
                                            Properties().get(AM_AUTH_CERT_ALIAS_PROPERTY,""),false);
        Log::setRemoteInfo(newLogSvc);

        Log::log(Log::REMOTE_MODULE, 
                 Log::LOG_AUTH_REMOTE, 
                 mesg);
    }
    catch (smi::InternalException& iex) {
	error = iex.getMessage();
	cout << "Internal Exception: " << error << endl;
    }
    catch (std::exception& ex) {
	error = ex.what();
    }
    catch (...) {
	error = "Unknown exception.";
    }
    return error;
}

int
main(int argc, char *argv[])
{
    int j;
    char c;
    char *rem_log_name = (char *)REM_LOG_NAME;
    char *rem_log_url = (char *)REM_LOG_URL;
    const char *sso_token_id = (char *)SSO_TOKEN_ID;
    char *prop_file = (char *)PROP_FILE;
    char *local_log_file = (char *)LOCAL_LOG_FILE;
    char *mesg = (char *)MESG;
    bool bad_opt = false;
    char *user = NULL;
    char *pw = NULL;
    

    for (j=1; j < argc; j++) {
        if (*argv[j]=='-') {
            c = argv[j][1];
            switch (c) {
	    case 'u':
                user = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'p':
                pw = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'r':
                rem_log_url = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 's':
                sso_token_id = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'n':
                rem_log_name = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'f':
                prop_file = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'l':
                local_log_file = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'm':
                mesg = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    default:
		bad_opt = true;
		break;
	    }
	    if (bad_opt)
		break;
        }
        else {
            bad_opt = true;
            break;
        }
    }

    if (bad_opt ||
        !rem_log_url || !sso_token_id || !rem_log_name ||
        !prop_file || !local_log_file || !mesg) {
        usage(argv[0]);
        return 1;
    }

    const char *error = init(prop_file, user, pw, &sso_token_id);
    if (error) {
        cout << "Failed initializing: " << error << endl;
        return 2;
    }

    error = run_test(rem_log_url, sso_token_id, rem_log_name, mesg);
    if (error) {
        cout << "Test failed: " << error << endl;
        return 4;
    }
    
    cout << "Test completed." << endl;
    return 0;
}


