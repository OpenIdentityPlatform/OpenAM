/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: am_auth.cpp,v 1.5 2009/12/09 23:58:50 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <vector>
#include <am_auth.h>
#include "internal_macros.h"
#include "auth_svc.h"
#include "am_string_set.h"

BEGIN_PRIVATE_NAMESPACE
static AuthService *authSvc = NULL;
DEFINE_BASE_INIT;
void auth_cleanup();
END_PRIVATE_NAMESPACE

USING_PRIVATE_NAMESPACE

#define AM_AUTH_MODULE "AuthService"

extern "C" am_status_t
am_auth_init(const am_properties_t auth_config_params) {
    am_status_t retVal = AM_FAILURE;

    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    if(auth_config_params != NULL) {
        try {
            const Properties& propertiesRef =
                *reinterpret_cast<Properties *>(auth_config_params);

            if(authSvc == NULL) {
                base_init(propertiesRef, B_TRUE);
                authSvc = new AuthService(propertiesRef);
                Log::log(amAuthLogID, Log::LOG_INFO,
                         "am_auth_init(): Authentication "
                         "service successfully initialized.");
            } else {
                Log::log(amAuthLogID, Log::LOG_WARNING,
                         "am_auth_init(): Authentication "
                         "service previously initialized.");
            }
            retVal = AM_SUCCESS;
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
            retVal = AM_AUTH_CTX_INIT_FAILURE;
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
            retVal = AM_INVALID_ARGUMENT;
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_init(): Unknown exception thrown.");
            retVal = AM_FAILURE;
        }
    } else {
        if(retVal != AM_SUCCESS) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_init(): Common Initialization failed: %s",
                     am_status_to_string(retVal));
        }
        if(auth_config_params == NULL) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_init(): Invalid argument "
                     "passed during invocation.");
            retVal = AM_INVALID_ARGUMENT;
        }
    }
    return retVal;
}

void PRIVATE_NAMESPACE_NAME::auth_cleanup() {
    delete authSvc;
    authSvc = NULL;
    return;
}

extern "C" am_status_t
am_auth_create_auth_context(am_auth_context_t *auth_ctx,
                            const char *org_name,
                            const char *cert_nick_name,
                            const char *url) {
    am_status_t retVal = AM_FAILURE;
    AuthContext *authContext = NULL;
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);

    try {
        *auth_ctx = NULL;
        std::string orgName("");
        std::string certNickName("");
        std::string namingURL("");

        if(org_name != NULL) {
            orgName = org_name;
        }
        if(cert_nick_name != NULL) {
            certNickName = cert_nick_name;
        }
        if(url != NULL) {
	    // from "http.../uri  http.../uri"
	    // to   "http.../uri/namingservice  http.../uri/namingservice"
	    const std::string space(" ");
	    const std::string namingservice_space("/namingservice ");
	    const std::string namingservice("/namingservice");
	    std::string urlStr(url);
	    std::size_t pos = 0;
	    pos = urlStr.find (space, pos);
	    while (pos != std::string::npos) {
		urlStr.replace (pos, space.size(), namingservice_space);
		pos += namingservice_space.size();
		pos = urlStr.find_first_not_of (space, pos);
		if (pos != std::string::npos) {
		    pos = urlStr.find (space, pos);
		}
	    }
	    namingURL = urlStr + namingservice;
        }

        authContext = new AuthContext(orgName, certNickName, namingURL);

        authSvc->create_auth_context(*authContext);

        *auth_ctx = reinterpret_cast<am_auth_context_t>(authContext);

        retVal = AM_SUCCESS;
    } catch(InternalException &iex) {
        Log::log(amAuthLogID, Log::LOG_ERROR, iex);
        retVal = AM_AUTH_CTX_INIT_FAILURE;
    } catch(std::invalid_argument &aex) {
        Log::log(amAuthLogID, Log::LOG_ERROR, aex);
        retVal = AM_INVALID_ARGUMENT;
    } catch(...) {
        Log::log(amAuthLogID, Log::LOG_ERROR,
        "am_auth_create_auth_context(): "
        "Unknown exception thrown.");
        retVal = AM_FAILURE;
    }
    if(retVal != AM_SUCCESS) {
        delete authContext;
    }

    return retVal;
}

extern "C" am_status_t
am_auth_destroy_auth_context(am_auth_context_t auth_ctx) {
    am_status_t retVal = AM_FAILURE;
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    if(auth_ctx != NULL) {
        try {
            AuthContext *authContext =
                reinterpret_cast<AuthContext *>(auth_ctx);
            delete authContext;
            retVal = AM_SUCCESS;
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
            retVal = AM_AUTH_FAILURE;
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
            retVal = AM_INVALID_ARGUMENT;
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_destroy_auth_context(): "
                     "Unknown exception thrown.");
            retVal = AM_FAILURE;
        }
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t
am_auth_login(am_auth_context_t auth_ctx, am_auth_index_t auth_idx,
              const char *value) {
    am_status_t retVal = AM_FAILURE;
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    if(auth_ctx != NULL && value != NULL && authSvc != NULL) {
        if(auth_idx != AM_AUTH_INDEX_AUTH_LEVEL &&
        auth_idx != AM_AUTH_INDEX_ROLE &&
        auth_idx != AM_AUTH_INDEX_USER &&
        auth_idx != AM_AUTH_INDEX_MODULE_INSTANCE &&
        auth_idx != AM_AUTH_INDEX_SERVICE) {
            Log::log(amAuthLogID, Log::LOG_ERROR, "am_auth_login(): "
                     "Invalid index.");
            retVal = AM_FEATURE_UNSUPPORTED;
        } else {
            try {
                AuthContext &authContext =
                    reinterpret_cast<AuthContext &>(*auth_ctx);
                authSvc->login(authContext, auth_idx, value);
                retVal = AM_SUCCESS;
            } catch(InternalException &iex) {
                Log::log(amAuthLogID, Log::LOG_ERROR, iex);
                retVal = AM_AUTH_FAILURE;
            } catch(std::invalid_argument &aex) {
                Log::log(amAuthLogID, Log::LOG_ERROR, aex);
                retVal = AM_INVALID_ARGUMENT;
            } catch(...) {
                Log::log(amAuthLogID, Log::LOG_ERROR,
                         "am_auth_login(): "
                         "Unknown exception thrown.");
                retVal = AM_FAILURE;
            }
        }
    } else {
        retVal = (authSvc == NULL)?AM_AUTH_CTX_INIT_FAILURE:AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t
am_auth_logout(am_auth_context_t auth_ctx) {
    am_status_t retVal = AM_FAILURE;
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    if(auth_ctx != NULL && authSvc != NULL ) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);
            authSvc->logout(authContext);
            retVal = AM_SUCCESS;
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
            retVal = AM_AUTH_FAILURE;
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
            retVal = AM_INVALID_ARGUMENT;
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_logout(): "
                     "Unknown exception thrown.");
            retVal = AM_FAILURE;
        }
    } else {
        retVal = (authSvc == NULL)?AM_AUTH_CTX_INIT_FAILURE:AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t
am_auth_abort(am_auth_context_t auth_ctx) {
    am_status_t retVal = AM_FAILURE;
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    if(auth_ctx != NULL && authSvc != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);
            authSvc->abort(authContext);
            retVal = AM_SUCCESS;
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
            retVal = AM_AUTH_FAILURE;
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
            retVal = AM_INVALID_ARGUMENT;
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_abort(): "
                     "Unknown exception thrown.");
            retVal = AM_FAILURE;
        }
    } else {
        retVal = (authSvc == NULL)?AM_AUTH_CTX_INIT_FAILURE:AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t
am_auth_get_module_instance_names(am_auth_context_t auth_ctx,
                                  am_string_set_t** module_inst_names_ptr ) {
    am_status_t retVal = AM_FAILURE;
    Log::ModuleId authCtxModule = Log::addModule(AM_AUTH_MODULE);
    if(authSvc != NULL) {
        if(auth_ctx != NULL) {
            try {
                AuthContext &authContext =
                    reinterpret_cast<AuthContext &>(*auth_ctx);
                authSvc->getModuleInstanceNames(authContext,
						module_inst_names_ptr);
                retVal = AM_SUCCESS;
            } catch(InternalException &iex) {

                Log::log(authCtxModule, Log::LOG_ERROR, iex);
                retVal = AM_AUTH_FAILURE;
            } catch(std::invalid_argument &aex) {
                Log::log(authCtxModule, Log::LOG_ERROR, aex);
                retVal = AM_INVALID_ARGUMENT;
            } catch(...) {
                Log::log(authCtxModule, Log::LOG_ERROR,
                         "am_auth_get_module_instance_names() "
                         "Unknown exception thrown.");
            }
        } else {
                Log::log(authCtxModule, Log::LOG_ERROR,
                         "am_auth_get_module_instance_names() "
                         "Auth context not initialized.");
		retVal = AM_INVALID_ARGUMENT;
        }
    } else {
        Log::log(authCtxModule, Log::LOG_ERROR,
                 "am_auth_get_module_instance_names() ",
                 "Auth context not initialized.");
	retVal = AM_SERVICE_NOT_INITIALIZED;
    }
    return retVal;
}

extern "C" boolean_t
am_auth_has_more_requirements(am_auth_context_t auth_ctx) {
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    boolean_t retVal = B_FALSE;
    if(auth_ctx != NULL && authSvc != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);

            retVal = authContext.hasMoreRequirements() ? B_TRUE : B_FALSE;
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_has_more_requirements(): "
                     "Unknown exception thrown.");
        }
    } else {
        retVal = B_FALSE;
    }

    return retVal;
}

extern "C" std::size_t
am_auth_num_callbacks(am_auth_context_t auth_ctx) {
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    std::size_t retVal = 0;
    if(auth_ctx != NULL && authSvc != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);

            retVal = authSvc->getNumRequirements(authContext);
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_num_callbacks(): "
                     "Unknown exception thrown.");
        }
    } else {
        retVal = (authSvc == NULL)?AM_AUTH_CTX_INIT_FAILURE:AM_INVALID_ARGUMENT;
    }

    return retVal;
}

extern "C" const char *
am_auth_get_organization_name(am_auth_context_t auth_ctx) {
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    const char *retVal = NULL;
    if(auth_ctx != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);

            retVal = authContext.getOrganizationName().c_str();
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_get_organization_name(): "
                     "Unknown exception thrown.");
        }
    }

    return retVal;
}

extern "C" const char *
am_auth_get_sso_token_id(am_auth_context_t auth_ctx) {
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    const char *retVal = NULL;
    if(auth_ctx != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);
            if(authContext.getStatus() == AM_AUTH_STATUS_SUCCESS) {
                retVal = authContext.getSSOToken().c_str();
            }
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_get_sso_token_id(): "
                     "Unknown exception thrown.");
        }
    }

    return retVal;
}

extern "C" am_auth_callback_t *
am_auth_get_callback(am_auth_context_t auth_ctx, std::size_t idx) {
    am_auth_callback_t *retVal = NULL;


    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    if(auth_ctx != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);
            if(authContext.getStatus() == AM_AUTH_STATUS_IN_PROGRESS) {
                std::vector<am_auth_callback_t> &callbacks =
                    authContext.getRequirements();
                retVal = &(callbacks[idx]);
            }
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_get_callback(): "
                     "Unknown exception thrown.");
        }
    }
    return retVal;
}

extern "C" am_status_t
am_auth_submit_requirements(am_auth_context_t auth_ctx) {
    am_status_t retVal = AM_FAILURE;

    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    if(auth_ctx != NULL && authSvc != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);
            if(authContext.getStatus() == AM_AUTH_STATUS_IN_PROGRESS) {
                authSvc->submitRequirements(authContext);
                retVal = AM_SUCCESS;
            } else {
                retVal = AM_AUTH_FAILURE;
            }
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
            retVal = AM_AUTH_FAILURE;
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
            retVal = AM_INVALID_ARGUMENT;
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_submit_requirements(): "
                     "Unknown exception thrown.");
            retVal = AM_FAILURE;
        }
    } else {
        retVal = (authSvc == NULL)?AM_AUTH_CTX_INIT_FAILURE:AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t
am_auth_submit_requirements_and_update_authctx(am_auth_context_t *p_auth_ctx) {
    am_status_t retVal = AM_FAILURE;

    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    if(*p_auth_ctx != NULL && authSvc != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(**p_auth_ctx);
            if(authContext.getStatus() == AM_AUTH_STATUS_IN_PROGRESS) {
                authSvc->submitRequirements(authContext);
                retVal = AM_SUCCESS;
                *p_auth_ctx = reinterpret_cast<am_auth_context_t>(&authContext);
            } else {
                retVal = AM_AUTH_FAILURE;
            }
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
            retVal = AM_AUTH_FAILURE;
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
            retVal = AM_INVALID_ARGUMENT;
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_submit_requirements_and_update_authctx(): "
                     "Unknown exception thrown.");
            retVal = AM_FAILURE;
        }
    } else {
        retVal = (authSvc == NULL)?AM_AUTH_CTX_INIT_FAILURE:AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_auth_status_t
am_auth_get_status(am_auth_context_t auth_ctx) {
    Log::ModuleId amAuthLogID = Log::addModule(AM_AUTH_MODULE);
    am_auth_status_t retVal = AM_AUTH_STATUS_NOT_STARTED;
    if(auth_ctx != NULL) {
        try {
            AuthContext &authContext =
                reinterpret_cast<AuthContext &>(*auth_ctx);

            retVal = authContext.getStatus();
        } catch(InternalException &iex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, iex);
        } catch(std::invalid_argument &aex) {
            Log::log(amAuthLogID, Log::LOG_ERROR, aex);
        } catch(...) {
            Log::log(amAuthLogID, Log::LOG_ERROR,
                     "am_auth_get_status(): "
                     "Unknown exception thrown.");
        }
    }

    return retVal;
}

