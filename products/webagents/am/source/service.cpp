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

#include <climits>
#include <ctime>
#include <string>
#include <list>
#include <am.h>
#include <am_web.h>
#include "internal_macros.h"
#include "xml_tree.h"
#include "scope_lock.h"
#include "ref_cnt_ptr.h"
#include "sso_token.h"
#include "service_info.h"
#include "policy_resource.h"
#include "policy_entry.h"
#include "action_decision.h"
#include "policy_engine.h"
#include "auth_svc.h"
#include "naming_service.h"
#include "sso_token_service.h"
#include "policy_service.h"
#include "log_service.h"
#include "service.h"

#define SERVICE_ENGINE "ServiceEngine"
#define AM_POLICY_FETCH_HEADER_ATTRS_PROPERTY AM_POLICY_PROPERTY_PREFIX "fetchHeaders"
USING_PRIVATE_NAMESPACE

using std::vector;
using std::invalid_argument;
using std::string;

namespace {
    const unsigned long DEFAULT_HASH_SIZE = 131;
    const unsigned long DEFAULT_TIMEOUT = 3;
    const PRUint32 DEFAULT_MAX_THREADS = 10;
    const char *DEFAULT_SESSION_USER_ID_PARAM = "UserToken";
    const char *DEFAULT_LDAP_USER_ID_PARAM = "entrydn";
}
extern smi::SSOTokenService *get_sso_token_service();
std::string attrMultiValueSeparator = "|";

string trimUriOrgEntry(const string& uriString) {
    string retVal(uriString);

    std::size_t t = retVal.find("&org=");
    if(t == string::npos) {
	t = retVal.find("?org=");
    }

    // no occurance of org.
    if(t == string::npos)
	return retVal;

    std::size_t x = t+1;
    while(x < retVal.size() && retVal.at(x) != '&') x++;

    if(x == retVal.size()) {
	retVal.erase(t);
    } else {
	/* deletes from "org=...&" */
	retVal.erase(t+1, x-t);
    }

    return retVal;
}

bool isValidAttrsFetchMode(const std::string &svcName) {
	return ((!strcasecmp(svcName.c_str(), AM_POLICY_SET_ATTRS_AS_HEADER)) ||
	    (!strcasecmp(svcName.c_str(), AM_POLICY_SET_ATTRS_AS_HEADER_OLD)) ||
	    (!strcasecmp(svcName.c_str(), AM_POLICY_SET_ATTRS_AS_COOKIE)) ||
	    (!strcasecmp(svcName.c_str(), AM_POLICY_SET_ATTRS_AS_COOKIE_OLD)));
}


/*
 * Throws:
 *	std::invalid_argument if an input argument is invalid.
 *	InternalException upon other errors
 */
Service::Service(const char *svcName,
		 const char *instName,
		 const am_resource_traits_t rTraits,
		 const Properties& initParams)
    : logID(Log::addModule(SERVICE_ENGINE)),
      svcParams(initParams, logID),
      initialized(false),
      threadPoolCreated(false),
      serviceName(), instanceName(),
      notificationEnabled(svcParams.getBool(
                              AM_COMMON_NOTIFICATION_ENABLE_PROPERTY, false)),
      do_sso_only(svcParams.getBool(AM_WEB_DO_SSO_ONLY, false)),
      policy_number_of_tries(svcParams.getUnsigned(
                              AM_POLICY_NUMBER_OF_TRIES, 0)),
      notificationURL(),
      policyTable(svcParams.getUnsigned(AM_POLICY_HASH_BUCKET_SIZE_PROPERTY,
				      DEFAULT_HASH_SIZE),
	       svcParams.getPositiveNumber(AM_POLICY_HASH_TIMEOUT_MINS_PROPERTY,
				      DEFAULT_TIMEOUT)),
      fetchProfileAttrs(false),
      fetchSessionAttrs(false),
      fetchResponseAttrs(false),
      rsrcTraits(rTraits),
	  tPool(NULL),
      htCleaner(NULL),
      lock(),
      namingSvcInfo(svcParams.get(AM_COMMON_NAMING_URL_PROPERTY)),
		    alwaysTrustServerCert(
                        svcParams.getBool(AM_COMMON_TRUST_SERVER_CERTS_PROPERTY,
					  false)),
      authSvc(svcParams),
      authCtx(),
      namingSvc(svcParams,
                svcParams.get(AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),
                svcParams.get(AM_AUTH_CERT_ALIAS_PROPERTY,""),
                alwaysTrustServerCert),
      policySvc(NULL),
      mFetchFromRootResource(
			     svcParams.getBool(AM_POLICY_FETCH_FROM_ROOT_RSRC_PROPERTY, true)),
      mOrdNum(svcParams.getUnsigned(AM_COMMON_ORDINAL_NUMBER, 0)),
      mUserIdParamType(USER_ID_PARAM_TYPE_SESSION),
      mUserIdParam(""),
      mLoadBalancerEnable(false),
      mSSOTokenSvc(*(get_sso_token_service())) {
    if(svcName == NULL)
	throw std::invalid_argument("Invalid service name during service initialization");

    try {
	// Fetch the profile mode
	fetchProfileAttrs = isValidAttrsFetchMode(svcParams.get(AM_POLICY_PROFILE_ATTRS_MODE, ""));

	// Fetch the session mode
	fetchSessionAttrs = isValidAttrsFetchMode(svcParams.get(AM_POLICY_SESSION_ATTRS_MODE, ""));

	// Fetch the response mode
	fetchResponseAttrs = isValidAttrsFetchMode(svcParams.get(AM_POLICY_RESPONSE_ATTRS_MODE, ""));
    }
    catch (std::invalid_argument& ex) {
	// for backwards compatibility, check if fetchHeaders property is
	// there.
	bool fetchHeaders = false;
	try {
	    fetchHeaders = svcParams.getBool(AM_POLICY_FETCH_HEADER_ATTRS_PROPERTY);
	    if (true == fetchHeaders)
		fetchProfileAttrs = true;
	}
	catch (invalid_argument& iex) {
	    fetchProfileAttrs = false;
	}
    }

    {
	std::string userIdParamType =
	    svcParams.get(AM_POLICY_USER_ID_PARAM_TYPE_PROPERTY, "Session");
	if (strcasecmp(userIdParamType.c_str(), "SESSION")==0) {
	    mUserIdParamType = USER_ID_PARAM_TYPE_SESSION;
	}
	else if (strcasecmp(userIdParamType.c_str(), "LDAP")==0) {
	    mUserIdParamType = USER_ID_PARAM_TYPE_LDAP;
	}
	else {
	    const char *msg = "Invalid value for property "
				        AM_POLICY_USER_ID_PARAM_TYPE_PROPERTY;
	    throw std::invalid_argument(msg);
	}
	if (mUserIdParamType == USER_ID_PARAM_TYPE_SESSION) {
	    mUserIdParam = svcParams.get(AM_POLICY_USER_ID_PARAM_PROPERTY,
					 DEFAULT_SESSION_USER_ID_PARAM);
	}
	else { // LDAP
	    mUserIdParam = svcParams.get(AM_POLICY_USER_ID_PARAM_PROPERTY,
					 DEFAULT_LDAP_USER_ID_PARAM);
	}
    }

    if(rsrcTraits.cmp_func_ptr == NULL ||
       rsrcTraits.get_resource_root == NULL ||
       rsrcTraits.canonicalize == NULL ||
       rsrcTraits.str_free == NULL) {
	throw std::invalid_argument("Invalid resource traits "
		"structure contents passed during service creation.");
    }

    string func("Service constructor");

    serviceName = svcName;
    instanceName = instName;
    if(fetchProfileAttrs) {
	profileAttributesMap.parsePropertyKeyValue(
		svcParams.get(AM_POLICY_PROFILE_ATTRS_MAP, ""),
		',', '|');
	Properties::iterator iter;
	for(iter = profileAttributesMap.begin(); 
		iter != profileAttributesMap.end(); iter++) {
	    string attr = (*iter).first;
	    Log::log(logID, Log::LOG_MAX_DEBUG,
		     "Service::Service() Profile Attribute=%s", attr.c_str());
	    attrList.push_back(attr);
	}
    }

    if(fetchSessionAttrs) {        
	sessionAttributesMap.parsePropertyKeyValue(
		svcParams.get(AM_POLICY_SESSION_ATTRS_MAP, ""),
		',', '|');
    }

    if(fetchResponseAttrs) {        
	responseAttributesMap.parsePropertyKeyValue(
		svcParams.get(AM_POLICY_RESPONSE_ATTRS_MAP, ""),
		',', '|');
    }

    attrMultiValueSeparator =
        svcParams.get(AM_POLICY_ATTRS_MULTI_VALUE_SEPARATOR, "|");

    // if user id parameter comes from an ldap attribute, add it also
    // to the list of ldap attributes to fetch and parse.
    if (mUserIdParamType == USER_ID_PARAM_TYPE_LDAP) {
	// Set parameter in the attributes to parse in the response.
	// if fetch headers was set to true and the parameter is already
	// there then no need to add it again; if not, add it.
	if (!profileAttributesMap.isSet(mUserIdParam)) {
	    profileAttributesMap.set(mUserIdParam, mUserIdParam);
	}
	// Now set the parameter in attributes to get in the request.
	attrList.push_back(mUserIdParam);
    }

    if (notificationEnabled) {
	notificationURL = svcParams.get(AM_COMMON_NOTIFICATION_URL_PROPERTY,"");
	Log::log(logID, Log::LOG_INFO,
		 "Service() notification enabled, URL = %s",
		 notificationURL.c_str());

	if(notificationURL.size() < 8) {
	    Log::log(logID, Log::LOG_WARNING,
		     "Service() Notification disabled. Notification URL is invalid.");
	    notificationEnabled = false;
	    notificationURL.resize(0);
	}
    } else {
	Log::log(logID, Log::LOG_INFO,
		 "Service() notification disabled");
    }
}

/*
 * Throws:
 *	std::invalid_argument if any argument is invalid
 *	XMLTree::ParseException upon XML parsing error
 *	NSPRException upon NSPR error
 *	InternalException upon other errors
 */
void
Service::initialize() {
    string func("Service::initialize()");
    ScopeLock myLock(lock);
    if(initialized == true) {
	return;
    }
    if (htCleaner == NULL) {
        try {
            htCleaner = new HTCleaner<PolicyEntry>(&policyTable,
                                      svcParams.getPositiveNumber(
                                           AM_POLICY_HASH_TIMEOUT_MINS_PROPERTY,
				           DEFAULT_TIMEOUT),
		                           "policy cache cleanup");
        } catch(std::bad_alloc &bae) {
            throw InternalException(func,
                                    "Memory allocation failure while "
                                    "creating hash table cleaner.",
                                    AM_NO_MEMORY);
        } catch(InternalException &ie) {
	    throw ie;
	}
    }

    if(threadPoolCreated == false) {
	if(tPool == NULL) {
		try {
		    tPool = new ThreadPool(1, 1 + notificationEnabled?
			    svcParams.getUnsigned(AM_POLICY_MAX_THREADS_PROPERTY,
						  DEFAULT_MAX_THREADS):0);
		} catch(std::bad_alloc &bae) {
		    throw InternalException(func,
					    "Memory allocation failure while"
					    " creating thread pool.",
					    AM_NO_MEMORY);
		}
	}

	/* Adding cleanup thread for the policyEntry hash table */
        /* thread pool will free htCleaner pointer when it is done
         * executing, when am_cleanup() is called. */
	if(tPool->dispatch(htCleaner) == false) {
	    string msg("Cleaner thread dispatch failed.");
	    Log::log(logID, Log::LOG_ERROR, msg.c_str());
	    throw InternalException(func, msg, AM_INIT_FAILURE);
	}
	threadPoolCreated = true;
    }

    Log::log(logID, Log::LOG_MAX_DEBUG,
	     "Service communication with server started.");
    am_status_t status;
    mPolicyEntry = new PolicyEntry(rsrcTraits, profileAttributesMap);
    construct_auth_svc(mPolicyEntry);

    // Do naming query
    if(AM_SUCCESS !=
       (status = namingSvc.getProfile(namingSvcInfo,
			      mPolicyEntry->getSSOToken().getString(),
			      mPolicyEntry->cookies,
			      mPolicyEntry->namingInfo))) {

	string msg("Naming query failed during service creation.");
	throw InternalException(func, msg, status);
    }
    
    // Do session query
    if((status = mSSOTokenSvc.getSessionInfo(
			    mPolicyEntry->namingInfo.getSessionSvcInfo(),
			    mPolicyEntry->getSSOToken().getString(),
                            mPolicyEntry->cookies,
			    true,
			    mAppSessionInfo, false, false)) != AM_SUCCESS) {
	string msg("Session query failed during service creation.");
	throw InternalException(func, msg, status);
    }

   if (do_sso_only && !fetchProfileAttrs && !fetchResponseAttrs) {
       Log::log(logID, Log::LOG_INFO,"do_sso_only is set to true, profile and response attributes fetch mode is set to NONE");
   } else {
    // Do policy init.
    string policydata;
    KeyValueMap env;
    policySvc = new PolicyService(
		    mPolicyEntry->getSSOToken(),
		    svcParams,
		    svcParams.get(AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),
                    svcParams.get(AM_AUTH_CERT_ALIAS_PROPERTY,""),
		    alwaysTrustServerCert);

	policySvc->sendNotificationMsg(false,
			mPolicyEntry->namingInfo.getPolicySvcInfo(),
			serviceName, mPolicyEntry->cookies,
			notificationURL);

    if(notificationURL.size() > 0) {
	policySvc->sendNotificationMsg(true,
				  mPolicyEntry->namingInfo.getPolicySvcInfo(),
				  serviceName,
				  mPolicyEntry->cookies,
				  notificationURL);
    }

    if(policySvc->getRevisionNumber() >= 30) {
	KeyValueMap advicesMap;
	policySvc->getAdvicesList(mPolicyEntry->namingInfo.getPolicySvcInfo(),
				  serviceName, mPolicyEntry->cookies,
				  advicesMap);
	KeyValueMap::const_iterator result = advicesMap.find(SERVER_HANDLED_ADVICES);
	const std::vector<std::string> adviceNames = (*result).second;
        if (!serverHandledAdvicesList.empty()) {
            	serverHandledAdvicesList.clear();
   		Log::log(logID, Log::LOG_MAX_DEBUG,"Server handled Advice list not empty, so cleared ");
        }
	serverHandledAdvicesList.insert(serverHandledAdvicesList.begin(),
				       adviceNames.begin(), adviceNames.end());
    }
   }

    string remoteLogName = svcParams.get(AM_COMMON_SERVER_LOG_FILE_PROPERTY,
					 "");

    LogService *newLogSvc =
	   new LogService(mPolicyEntry->namingInfo.getLoggingSvcInfo(),
			  mPolicyEntry->getSSOToken(),
			  mPolicyEntry->cookies, remoteLogName,
			  svcParams,
                          svcParams.get(AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),
                          svcParams.get(AM_AUTH_CERT_ALIAS_PROPERTY,""),
                          alwaysTrustServerCert);


    Log::setRemoteInfo(newLogSvc);

    initialized = true;
    Log::log(logID, Log::LOG_MAX_DEBUG,
	     "Service communication with finished successfully.");
    return;
}

#define DEFAULT_AGENT_AUTH_MODULE "Application"
#define DEFAULT_AGENT_USERNAME "UrlAccessAgent"
#define DEFAULT_AGENT_ORG_NAME "/"

/* Throws
 *	std::invalid_argument if any argument is invalid
 *	InternalException upon other errors.
 */
void
Service::do_agent_auth_login(SSOToken& ssoToken)
{
    const char *thisfunc = "Service::do_agent_auth_login()";
    string orgName;
    string moduleName;
    string userName;
    string passwd;

    // Get all the parameters.
    // Right now we just handle username/password callback for LDAP module.
    // in the future we may want to handle different callbacks depending
    // on the module.
    orgName = svcParams.get(AM_POLICY_ORG_NAME_PROPERTY,
			    DEFAULT_AGENT_ORG_NAME);
    moduleName = svcParams.get(AM_POLICY_MODULE_NAME_PROPERTY,
                               DEFAULT_AGENT_AUTH_MODULE);
    userName = svcParams.get(AM_POLICY_USER_NAME_PROPERTY,
			     DEFAULT_AGENT_USERNAME);
    passwd = svcParams.get(AM_POLICY_PASSWORD_PROPERTY);

    // Create Auth context, do login.
    std::string certName, namingURLs;   // dummy strings for linux compile.
    AuthContext authC(orgName, certName, namingURLs);
    authSvc.create_auth_context(authC);
    authSvc.login(authC, AM_AUTH_INDEX_MODULE_INSTANCE, moduleName);


    std::vector<am_auth_callback_t>& callbacks = authC.getRequirements();
    int nCallbacks = callbacks.size();
    if (nCallbacks > 0) {
	bool userNameIsSet = false;
	bool passwdIsSet = false;
        for (int i = nCallbacks-1; i >= 0; --i) {
            am_auth_callback_t& cb = callbacks[i];
            switch(cb.callback_type) {
            case NameCallback:
                Log::log(logID, Log::LOG_DEBUG,
                         "%s: Setting name callback to '%s'.",
                         thisfunc, userName.c_str());
                cb.callback_info.name_callback.response = userName.c_str();
                userNameIsSet = true;
                break;
            case PasswordCallback:
                Log::log(logID, Log::LOG_DEBUG,
                         "%s: Setting password callback.", thisfunc);
                cb.callback_info.password_callback.response = passwd.c_str();
                passwdIsSet = true;
                break;
            default:
                // ignore unexpected callback.
                Log::log(logID, Log::LOG_WARNING,
                         "%s: Unexpected callback type %d ignored.",
			 thisfunc, cb.callback_type);
                continue;
                break;
            }
        }

        authSvc.submitRequirements(authC);

	am_auth_status_t auth_sts = authC.getStatus();
	switch(auth_sts) {
	case AM_AUTH_STATUS_SUCCESS:
	    break;
	case AM_AUTH_STATUS_FAILED:
	case AM_AUTH_STATUS_NOT_STARTED:
	case AM_AUTH_STATUS_IN_PROGRESS:
	case AM_AUTH_STATUS_COMPLETED:
	default:
	    Log::log(logID, Log::LOG_ERROR,
		     "Agent failed to login to IS, auth status %d.",
		     auth_sts);
	    throw InternalException(std::string(thisfunc),
				    "Unexpected auth status",
				    AM_AUTH_FAILURE);
	    break;
	}
    }
    else {
	Log::log(logID, Log::LOG_ERROR,
		 "%s: Agent cannot login to IS: Expected auth callbacks "
		 "for agent login not found.", thisfunc);
	throw InternalException(
		thisfunc, "Missing expected auth callbacks.", AM_AUTH_FAILURE);
    }

    // set the SSO Token.
    std::string ssoTokStr = authC.getSSOToken();
    ssoToken = SSOToken(ssoTokStr, Http::encode(ssoTokStr));

    // save the auth context for logging out when service is destroyed
    authCtx = authC;

    Log::log(logID, Log::LOG_DEBUG, "%s: Successfully logged in as %s.",
                                    thisfunc, userName.c_str());
}

// do application login.
/* Throws
 *	std::invalid_argument if any argument is invalid
 *	InternalException upon other errors.
 */
void
Service::construct_auth_svc(PolicyEntryRefCntPtr policyEntry)
{
    SSOToken& ssoToken = policyEntry->getSSOToken();
    do_agent_auth_login(ssoToken);

    Log::log(logID, Log::LOG_MAX_DEBUG, "Service::construct_auth_svc() done");
}

void
Service::sso_notify(const std::string &ssoToken) {
    if(initialized == false) {
	Log::log(logID, Log::LOG_WARNING, "Service::sso_notify() Service"
		 " not initalized.  Ignoring session notification.");
	return;
    }
    policyTable.remove(ssoToken);
    mSSOTokenSvc.removeSSOTokenTableEntry(ssoToken);
    return;
}

/* Throws
 *	std::invalid_argument if any argument is invalid
 *	InternalException upon other errors.
 */
void
Service::policy_notify(const string &resName,
		       NotificationType type)
{
    if(initialized == false) {
	Log::log(logID, Log::LOG_WARNING, "Service::policy_notify() Service"
		 " not initalized.  Ignoring policy notification.");
	return;
    }

    Log::log(logID, Log::LOG_INFO, "Service::policy_notify(resourceName=%s)",
	     resName.c_str());
    PolicyHandler *pH = new PolicyHandler(*this, resName, type);
    /* threadpool will free pointer when it has finished executing. */
    tPool->dispatch(pH);
    return;
}

/* Throws
 *	std::invalid_argument if any argument is invalid
 *	InternalException upon other errors.
 *	XMLTree::ParseException upon XML Parsing error.
 */
void
Service::process_policy_response(PolicyEntryRefCntPtr policyEntry,
				 const KeyValueMap &env,
				 const string &xmlData)
{
    XMLTree xmlObj(false, xmlData.data(), xmlData.size());
    XMLElement rootElem = xmlObj.getRootElement();
    KVMRefCntPtr lclKVM(new KeyValueMap(env));
    for(XMLElement elem = rootElem.getFirstSubElement();
	elem.isValid(); elem.nextSibling()) {

	// Handle policy responses
	if(elem.isNamed(POLICY_RESPONSE)) {
	    XMLElement rr;
	    for(elem.getSubElement(RESOURCE_RESULT, rr); rr.isValid();
		rr.nextSibling(RESOURCE_RESULT)) {
		policyEntry->add_policy(rr, lclKVM);
	    }
	}
    }
}


const am_status_t Service::destroyAppSSOToken() {
    am_status_t status = AM_NOT_FOUND;
    ServiceInfo svcInfo;
    status = mSSOTokenSvc.destroySession(svcInfo, mAppSessionInfo);
    if (status != AM_SUCCESS) {
	Log::log(logID, Log::LOG_DEBUG,
		 "Service::destroyAppSSOToken(): "
		 "Destroy agent sso token [%s] failed with %s.",
		 mAppSessionInfo.getSSOToken().getString().c_str(),
		 am_status_to_name(status));
    }
    return status;
}

Service::~Service() {
    ScopeLock mylock(lock);
    Log::log(logID, Log::LOG_MAX_DEBUG, "Service::~Service(): "
	    "Cleaning up %s service.",
	    serviceName.c_str());
    (void)do_agent_auth_logout();
    // Thread pool will free htCleaner pointer when it has stopped
    // executing.
    if (htCleaner != NULL) {
	htCleaner->stopCleaning();
	Log::log(logID, Log::LOG_MAX_DEBUG,
	     "Service::Service(): HTCleaner stopped.");
    } else {
	Log::log(logID, Log::LOG_DEBUG,
	     "Service::Service(): HTCleaner was not yet initialized.");
    }
    if (tPool != NULL) {
	delete(tPool);
    	Log::log(logID, Log::LOG_MAX_DEBUG,
	     "Service::Service(): Thread pool cleaned up.");
    } else {
    	Log::log(logID, Log::LOG_DEBUG,
	     "Service::Service(): Thread pool was not yet initialized.");
    }
    if (policySvc != NULL) {
        delete(policySvc);
    	Log::log(logID, Log::LOG_MAX_DEBUG,
	     "Service::Service(): Policy service destroyed.");
    } else {
    	Log::log(logID, Log::LOG_MAX_DEBUG,
	     "Service::Service(): Policy service was not yet initialized.");
    }
}

void 
Service::setRemUserAndAttrs(am_policy_result_t *policy_res,
                       PolicyEntryRefCntPtr uPolicyEntry,
                       const SessionInfo sessionInfo,
                       std::string& resName,
                       const std::vector<PDRefCntPtr>& results) const
{
    const char *func = "Service::setRemUserAndAttrs()";
    am_status_t  status = AM_SUCCESS;

    policy_res->remote_user = NULL;
    policy_res->attr_profile_map = AM_MAP_NULL;
    policy_res->attr_session_map = AM_MAP_NULL;
    policy_res->attr_response_map = AM_MAP_NULL;

    // if remote user id param type was session, set the
    // remote from the session property.
    if (mUserIdParamType == USER_ID_PARAM_TYPE_SESSION) {
        const char *msg = "%s:User Id parameter %s not found in "
                          "user's session properties. "
                          "Setting user Id to null (unknown).";
        try {
            const std::string &remoteUser =
                    sessionInfo.getProperties().get(mUserIdParam);
            if(remoteUser.size() > 0) {
                policy_res->remote_user = strdup(remoteUser.c_str());
                if (policy_res->remote_user == NULL) {
                    throw InternalException(func,
                             "No more memory for setting remote user",
                             AM_NO_MEMORY);
                }
            } else {
                Log::log(logID, Log::LOG_WARNING, msg, func,
                   mUserIdParam.c_str());
            }
        } catch(std::invalid_argument &ex) {
            Log::log(logID, Log::LOG_WARNING, msg, func, mUserIdParam.c_str());
        }
    }

    // if remote user id param type was ldap or if fetch ldap attributes
    // is true, get the ldap attributes and set remote user and
    // the ldap attribute map in the policy result.
    // set the ldap attributes if any and set the remote user from
    // ldap attribute if user id param is an ldap attribute.

    // This is an egregious patch work for retriving ldap attributes.
    // Once the Node object in the tree gets replaced by the Policy
    // decision object. This must be done fetched by doing a getParent.
    // This fix is based on the assumption that we know we query for
    // root resource always and that the LDAP attributes are returned
    // as a part of the first policy decision, in our case is the root
    // resource.

    if(fetchProfileAttrs || fetchSessionAttrs || fetchResponseAttrs ||
         mUserIdParamType == USER_ID_PARAM_TYPE_LDAP) 
    {
        ResourceName resObj(resName);
        string rootRes;

        if(mFetchFromRootResource == false) {
            rootRes = resName;
        }

        if((mFetchFromRootResource == true) && 
            !resObj.getResourceRoot(rsrcTraits, rootRes))
        {
            Log::log(logID, Log::LOG_WARNING,
                "%s: Error getting root resource for %s while getting "
                "user Id from LDAP attribute %s. "
                "Setting to user to null (unknown).", func,
                resName.c_str(), mUserIdParam.c_str());
        } else {
            PDRefCntPtr rootPolicy = uPolicyEntry->getPolicyDecision(rootRes);
            if (rootPolicy == PolicyDecision::INVALID_POLICY_DECISION) {
                Log::log(logID, Log::LOG_ERROR,"%s: Root policy could "
                      "not be found for resource %s. "
                      "The attributes will not be set.", func, rootRes.c_str());
                return;
            }
            const KeyValueMap &attrResp = rootPolicy->getAttributeResponses();
            if (mUserIdParamType == USER_ID_PARAM_TYPE_LDAP) {
                KeyValueMap::const_iterator iter = attrResp.find(mUserIdParam);
                if (iter == attrResp.end() || iter->second.size() <= 0) {
                    Log::log(logID, Log::LOG_WARNING,
                           "%s: User Id parameter %s not found in user's LDAP "
                           "attributes. Setting user Id to null (unknown).",
                           func, mUserIdParam.c_str());
                } else {
                    policy_res->remote_user = strdup(iter->second[0].c_str());
                }
            }

            // Construct the profile attribute map
            if (fetchProfileAttrs) {
                KeyValueMap &profile_attributes_map = *(new KeyValueMap());
                policy_res->attr_profile_map =
                   reinterpret_cast<am_map_t>(&profile_attributes_map);

                Properties::const_iterator iter_profile_attr;
                for (iter_profile_attr = profileAttributesMap.begin(); 
                    iter_profile_attr != profileAttributesMap.end(); 
                    iter_profile_attr++) 
                {
                    std::string profileKey = (*iter_profile_attr).first;
                    std::string profileAttr = (*iter_profile_attr).second;
                    std::string profileValue; 
                    std::string tmpValue; 
                    if (profile_attributes_map.size() > 0) {
                        KeyValueMap::const_iterator iter = profile_attributes_map.find(profileAttr);
                        if (iter != profile_attributes_map.end() && 
                           iter->second.size() > 0) 
                        {
                            tmpValue = iter->second[0];
                            profile_attributes_map.erase(profileAttr);
                        }
                    }
                    KeyValueMap::const_iterator iter_profile = 
                                           attrResp.find(profileKey);
                    if (iter_profile != attrResp.end()) {
                        for(std::size_t i=0;i<iter_profile->second.size();++i) {
                            profileValue.append(iter_profile->second[i]);
                            if (i < (iter_profile->second.size()-1)) {
                                profileValue.append(attrMultiValueSeparator);
                            }
                        }
                    }
                    if (tmpValue.size() > 0) {
                        profileValue =  profileValue + attrMultiValueSeparator + tmpValue;
                    }
                    Log::log(logID, Log::LOG_MAX_DEBUG, 
                            "Attribute value for %s found in ldap = %s", 
                            profileKey.c_str(), profileValue.c_str());
                    if (!profileAttr.empty() ) {
                        profile_attributes_map.insert(profileAttr, 
                                                      profileValue);
                    }
                }
            }

            // Set the session attribute map
            if (fetchSessionAttrs) {
                KeyValueMap &session_attributes_map = *(new KeyValueMap());
                time_t retVal = (time_t)-1;
                policy_res->attr_session_map =
                          reinterpret_cast<am_map_t>(&session_attributes_map);
                // Next construct the session attribute map
                Properties::const_iterator iter_session_attr;
                for (iter_session_attr = sessionAttributesMap.begin(); 
                    iter_session_attr != sessionAttributesMap.end(); 
                    iter_session_attr++)
                {
                    std::string sessionKey = (*iter_session_attr).first;
                    std::string sessionAttr = (*iter_session_attr).second;
                    std::string sessionValue; 
                    std::string tmpValue; 
                    if (session_attributes_map.size() > 0) {
                        KeyValueMap::const_iterator iter = 
                                 session_attributes_map.find(sessionAttr);
                        if (iter != session_attributes_map.end() && 
                             iter->second.size() > 0)
                        {
                            tmpValue = iter->second[0];
                            session_attributes_map.erase(sessionAttr);
                        }
                    }
                    try {
                        char dataStr[50];
                        if (!strcmp(sessionKey.c_str(),"maxtime")) {
                            retVal = (time_t)(sessionInfo.getMaxSessionTime());
                            PR_snprintf(dataStr, 50, "%ld", retVal);
                            std::string tmpStr(dataStr);
                            sessionValue = tmpStr;
                        } else if (!strcmp(sessionKey.c_str(),"maxidle")) {
                            retVal = (time_t)(sessionInfo.getMaxIdleTime());
                            PR_snprintf(dataStr, 50, "%ld", retVal);
                            std::string tmpStr(dataStr);
                            sessionValue = tmpStr;
                        } else {
                            sessionValue = 
                               sessionInfo.getProperties().get(sessionKey);
                            if (tmpValue.size() > 0) {
                                sessionValue = sessionValue + 
                                            attrMultiValueSeparator + tmpValue;
                            }
                            Log::log(logID, Log::LOG_MAX_DEBUG, "Attribute "
                                  "value for %s found in session = %s", 
                                  sessionKey.c_str(), sessionValue.c_str());
                        }
                    } catch(std::invalid_argument &ex) {
                        Log::log(logID, Log::LOG_MAX_DEBUG, "Attribute "
                                " value for %s not found in Session", 
                                sessionKey.c_str());
                    }
                    if (!sessionAttr.empty()) {
                        session_attributes_map.insert(sessionAttr, 
                                                       sessionValue);
                    }
                }
            }

            // Set the response attribute map
            if (fetchResponseAttrs) {
                KeyValueMap &response_attributes_map = *(new KeyValueMap());
                policy_res->attr_response_map =
                          reinterpret_cast<am_map_t>(&response_attributes_map);

                PDRefCntPtr responsePolicy = 
                                    uPolicyEntry->getPolicyDecision(resName);
                if (responsePolicy != NULL) {
                    const KeyValueMap &responseAttrs = 
                                     responsePolicy->getResponseAttributes();
                    KeyValueMap::const_iterator iter_response =
                                               responseAttrs.begin();
                    for(;(iter_response!=responseAttrs.end());iter_response++) {
                        const KeyValueMap::key_type &keyRef = iter_response->first;
                        std::string tmpResponseKey(keyRef.c_str());
                        std::string responseKey; 
                        std::string responseValue; 
                        std::string tmpValue; 
                     
                        if (responseAttributesMap.size() > 0) {
                            try {
                                responseKey = responseAttributesMap.get(tmpResponseKey);
                            } catch (invalid_argument& iex) {
                                responseKey = tmpResponseKey;
                            }
                        } else {
                            responseKey = tmpResponseKey;
                        }
                        if (response_attributes_map.size() > 0) {
                            KeyValueMap::const_iterator iter = 
                                response_attributes_map.find(responseKey);
                             if (iter != response_attributes_map.end() && 
                                iter->second.size() > 0)
                            {
                                tmpValue = iter->second[0];
                                response_attributes_map.erase(responseKey);
                            }
                        }
                        const KeyValueMap::mapped_type &valueRef = 
                                                   iter_response->second;
                        for(std::size_t i = 0; i < valueRef.size(); ++i) {
                            responseValue.append(valueRef[i]);
                            if (i < (valueRef.size()-1)) {
                                responseValue.append(attrMultiValueSeparator);
                            }
                        }
                        if (tmpValue.size() > 0) {
                            responseValue = responseValue + 
                                         attrMultiValueSeparator + tmpValue;
                        }
                        if (!responseKey.empty() && !responseValue.empty()) {
                            Log::log(logID, Log::LOG_MAX_DEBUG, "Attribute value "
                                "for %s found in response = %s", 
                                responseKey.c_str(), responseValue.c_str());
                            response_attributes_map.insert(responseKey, 
                                                      responseValue);
                        }
                    }
                }
            }
        }
    }
}

/*
 * Throws:
 *	std::invalid_argument if any argument is invalid
 *	XMLTree::ParseException upon XML parsing error
 *	NSPRException upon NSPR error
 *	InternalException upon other errors
 */
void
Service::getPolicyResult(const char *userSSOToken,
               const char *rName,
               const char *actionName,
               const KeyValueMap &env,
               am_map_t responsePtr,
               am_policy_result_t *policy_res,
               am_bool_t ignorePolicyResult,
               char **am_revision_number)
{
    const char *am_70_revision_number = "7.0";
    const char *am_63_revision_number = "6.3";

    if(initialized == false) {
        initialize();
    }

    assert(userSSOToken != NULL);
    assert(rName != NULL);
    assert(actionName != NULL);

    const char *func = "Service::getPolicyResult()";
    std::string resName;
    char *c_res = NULL;
    bool justUpdated = false;
    const ActionDecision *ad = static_cast<ActionDecision *>(NULL);
    bool cookieEncoded=strchr(userSSOToken,'%')!=NULL;
    const SSOToken ssoToken(
            cookieEncoded?Http::decode(userSSOToken):userSSOToken, 
            cookieEncoded?userSSOToken:Http::encode(userSSOToken));
    SessionInfo uSessionInfo;
    PolicyEntryRefCntPtr uPolicyEntry;

    rsrcTraits.canonicalize(rName, &c_res);
    if(c_res == NULL) {
        Log::log(logID, Log::LOG_ERROR,
            "%s:rsrcTraits.canonicalize(...) did not suceeed.", func);
        throw InternalException(func,
            "rsrcTraits.canonicalize(...) did not suceeed.", AM_FAILURE);
    } else {
        resName = c_res;
        rsrcTraits.str_free(c_res);
        c_res = NULL;
    }

    // Log:INFO Calling update_policy to get policy info.
    uPolicyEntry = policyTable.find(ssoToken.getString());
    update_policy(ssoToken, resName, actionName, env, uSessionInfo,
                  mFetchFromRootResource==true?SCOPE_SUBTREE:SCOPE_SELF,
                  false, uPolicyEntry);

    policy_res->remote_user = NULL;
    policy_res->remote_user_passwd = NULL;
    policy_res->remote_IP = NULL;

    vector<PDRefCntPtr> results;
    vector<std::string> resources;
    do {
        justUpdated = false;
        resources.resize(0);
        results.resize(0);

        // Assign the user's passwd to the remote user passwd field
        if(policy_res->remote_user_passwd == NULL) {
            try {
                const std::string &remoteUserPasswd =
                    uSessionInfo.getProperties().get("sunIdentityUserPassword");
                if(remoteUserPasswd.size() > 0) {
                    policy_res->remote_user_passwd = strdup(remoteUserPasswd.c_str());
                }
            } catch(std::invalid_argument &ex) {
                Log::log(logID, Log::LOG_WARNING,
                       "%s:No passwd value in session response.", func);
            }
        }

        // Assign the user's IP as known to dsame to to the remote IP variable.
        if (policy_res->remote_IP == NULL) {
            try {
                const std::string &remoteIP  =
                       uSessionInfo.getProperties().get(SessionInfo::HOST_IP);
                if(remoteIP.size() > 0) {
                    policy_res->remote_IP = strdup(remoteIP.c_str());
                }
            } catch (std::invalid_argument &ex) {
                Log::log(logID, Log::LOG_WARNING,
                     "%s:Remote IP (%s) parameter does not have a "
                     "value in session response.", func,
                     SessionInfo::HOST_IP.c_str());
            }
        }

        // Get the action decision for the resource and action name.
        uPolicyEntry->getAllPolicyDecisions(resName, results);
        //If results not found, get the resource root
        if((results.size() == 0)) {
            ResourceName resObj(resName);
            std::string rootRes;
            if(resObj.getResourceRoot(rsrcTraits, rootRes)) {
                Log::log(logID, Log::LOG_WARNING,
                         "%s: No policy decisions found for resource %s. "
                         " Updating policy entry for root resource.", 
                         func, results.size(),resName.c_str());
                if (uPolicyEntry->getTree(rootRes,false) == NULL) {
                    update_policy(ssoToken, resName, actionName,
                           env, uSessionInfo,
                           mFetchFromRootResource==true?SCOPE_SUBTREE:SCOPE_SELF,
                           true, uPolicyEntry);
                    uPolicyEntry->getAllPolicyDecisions(resName, results);
                }
            }
        }

        // For each policy decision, if it is stale,
        // get the new one.
        std::vector<PDRefCntPtr>::iterator iter;
        if(results.size() > 0) {
            bool needsUpdate = false;
            // Iterate through the policies and find if
            // there is any policy that needs update.
            for(iter = results.begin(); iter != results.end(); iter++) {
                PDRefCntPtr policy = *iter;
                if(policy->isStale(actionName)) {
                    needsUpdate = true;
                    // create a list of resource names for which
                    // polices need to be updated.
                    resources.push_back(policy->getName().getString());
                }
            }
            if(needsUpdate) {
                std::vector<std::string>::const_iterator iter;
                // Remove nodes to be updated so, if the nodes were to actually
                // deleted on the server, it also gets removed from the local
                // cache.
                for(iter=resources.begin(); iter != resources.end(); ++iter) {
                    uPolicyEntry->removePolicy(*iter);
                }
                update_policy_list(ssoToken, resources, actionName, env, uPolicyEntry);
                justUpdated = true;
                continue;
            } else {
                break;
            }
        }
    } while(justUpdated);
    
    if (!do_sso_only && (policy_number_of_tries > 0)) {
        int counter = 0;
        while ((results.size() == 0) && 
              (++counter <= policy_number_of_tries)) {
            // Server is in the process of updating the policy cache. 
            // Wait till the cache is populated
            Log::log(logID, Log::LOG_WARNING,
                        "%s: Result size is 0. "
                        "Trying again to get policy decisions (%i/%i)",
                        func, counter, policy_number_of_tries);
            PR_Sleep(PR_TicksPerSecond());
            uPolicyEntry->getAllPolicyDecisions(resName, results);
        }
    }

    // Set remote user and ldap attributes if any.
    setRemUserAndAttrs(policy_res, uPolicyEntry, uSessionInfo,
                       resName, results);
    if(results.size() > 0) {
        std::vector<PDRefCntPtr>::iterator iter;
        KeyValueMap &result_map =
                 *(reinterpret_cast<KeyValueMap *>(responsePtr));
        vector<string> vals;
        KeyValueMap *advices = new KeyValueMap();
        // set action decisions and advices if any.
        for(iter = results.begin(); iter != results.end(); iter++) {
            PDRefCntPtr policy = *iter;
            ad = policy->getActionDecision(actionName);
            if(ad != NULL) {
                const std::list<string> actVals = ad->getActionValues();
                std::list<string>::const_iterator act_iter = actVals.begin();
                for(; act_iter != actVals.end(); act_iter++) {
                    vals.push_back(*act_iter);
                }
                advices->merge(ad->getAdvices());
            }
        }
        result_map[actionName] = vals;
        policy_res->advice_map = reinterpret_cast<am_map_t>(advices);

        // if advices returned by server contains any advice
        // that requires redirect to server, then construct
        // the advice XML string and set it in
        // am_policy_result_t->advice_string
        std::string adviceStr;
        construct_advice_string(*advices, adviceStr);
        if(adviceStr.size() > 0) {
            policy_res->advice_string = strdup(adviceStr.c_str());
            Log::log(logID, Log::LOG_MAX_DEBUG,
                     "Service::getPolicyResult(): "
                     "Advice string constructed: [%s]", adviceStr.c_str());
            // No need to cache the policy decision when it has an advice
            // Remove the entry from the policy cache
            Log::log(logID, Log::LOG_MAX_DEBUG,
                     "Service::getPolicyResult(): "
                     "Removing the policy decision which has advice "
                     "from the policy cache");
            policyTable.remove(ssoToken.getString());
        } else {
            Log::log(logID, Log::LOG_DEBUG,
                     "Service::getPolicyResult(): "
                     "No advice string created.");
        }
    } else {
        if (do_sso_only && !fetchProfileAttrs && !fetchResponseAttrs) {
            return;
        } else {
            if (!ignorePolicyResult) {
                string msg("No Policy or Action decisions found "
                           "found for resource: ");
                msg.append(resName);
                throw InternalException(func, msg, AM_NO_POLICY);
            } else {
                return;
            }
        }
    }

    // Policy decision 'revision' tag will have a value of 30 if
    // agent is interacting with AM 7.0 else will have no value assigned
    if (am_revision_number != NULL) {
        if(policySvc->getRevisionNumber() >= 30) {
            strcpy(*am_revision_number,am_70_revision_number);
        } else {
            strcpy(*am_revision_number,am_63_revision_number);
        }
    }
    return;
}

/*
 * Throws:
 *	std::invalid_argument if any argument is invalid
 *	XMLTree::ParseException upon XML parsing error
 *	InternalException upon other errors
 */
void
Service::update_policy(const SSOToken &ssoTok, const string &resName,
		       const string &actionName,
		       const KeyValueMap &env,
                       SessionInfo &sessionInfo,
		       policy_fetch_scope_t scope,
		       bool refetchPolicy,
                       PolicyEntryRefCntPtr &policyEntry)
{
    am_status_t status;
    bool policyUpdated = false;
    string func("Service::update_policy");
    bool isNewEntry = false;
    Log::log(logID, Log::LOG_MAX_DEBUG,
	     "Executing update_policy(%s, %s, %s, %d)",
	     ssoTok.getString().c_str(), resName.c_str(),
	     actionName.c_str(), scope);

  //      policyEntry = policyTable.find(ssoTok.getString());
   
    if (policyEntry == NULL) {
        policyEntry = new PolicyEntry(ssoTok, env, profileAttributesMap, rsrcTraits);
        isNewEntry = true;
    }

	    
    // Do naming query, if notification is not enabled or
    // valid naming information is not present in the policyEntry.
    if(!policyEntry->namingInfo.isValid()) {
        if(AM_SUCCESS != (status = namingSvc.getProfile(namingSvcInfo,
				   ssoTok.getString(),
				   policyEntry->cookies,
				   policyEntry->namingInfo))) {
           throw InternalException(func, "Naming query failed.",
				    status);
	}
    } 
            
    status =  mSSOTokenSvc.getSessionInfo(policyEntry->namingInfo.getSessionSvcInfo(), 
				  ssoTok.getString(), 
                                  policyEntry->cookies, true, sessionInfo, 
                                  false, false);
    
    if (status != AM_SUCCESS) {
        // if agent could not contact session service to validate
        // user, and get any NSPR error, it is considered equivalent
	// to the session being invalid.
        if (AM_NSPR_ERROR == status) {
                 status = AM_INVALID_SESSION;
        }
	throw InternalException(func, "Session query failed.", status);
    }

    if (refetchPolicy) {       
        
        policyUpdated = do_update_policy(ssoTok, resName, actionName, env,
                                             sessionInfo, scope, policyEntry);

        /* if server is in the process of initializing the
         * appssotoken the do_update_policy will return false
         * retry do_update_policy 
         */

        if(!policyUpdated) {
            policyUpdated = do_update_policy(ssoTok, resName, actionName, env,
                                                  sessionInfo, scope, policyEntry);
	    if(!policyUpdated) {
	      /*throw error message policy not updated even after refresh */
            }
        }
    }

    if (isNewEntry) {
        policyTable.insert(policyEntry->getSSOToken().getString(), policyEntry);
    }
    return;
}

bool
Service::do_update_policy(const SSOToken &ssoTok, const string &resName,
                          const string &actionName,
                          const KeyValueMap &env,
                          SessionInfo &sessionInfo,
                          policy_fetch_scope_t scope,
                          PolicyEntryRefCntPtr& policyEntry)
{
    am_status_t status;
    string func("Service::do_update_policy");

    if (do_sso_only && !fetchProfileAttrs && !fetchResponseAttrs) {
        Log::log(logID, Log::LOG_INFO,"do_sso_only is set to true, profile and "
                "response attributes fetch mode is set to NONE");
        return true;
    } 

    // get the resource root.
    std::string rootRes;
    if(scope == SCOPE_SUBTREE) {
        ResourceName resObj(resName);
        if(!resObj.getResourceRoot(rsrcTraits, rootRes)) {
            throw InternalException(func,
                "ResourceName::getResourceRoot() failed.",
            AM_POLICY_FAILURE);
        }
    } else {
        rootRes = resName;
    }

    policy_fetch_scope_t policy_scope = scope;
    if (do_sso_only && fetchProfileAttrs && !fetchResponseAttrs) {
        policy_scope = SCOPE_RESPONSE_ATTRIBUTE_ONLY;
    }
    // Need to retrieve the lbcookie for app sso token and add it to
    // policyEntry->cookies
     
    // do policy
    string xmlData;
    if((status = policySvc->getPolicyDecisions(policyEntry->namingInfo.getPolicySvcInfo(),
                            policyEntry->getSSOToken(),
                            policyEntry->cookies,
                            sessionInfo,
                            serviceName,
                            rootRes.c_str(),
                            policy_scope,
                            env,
                            attrList,
                            xmlData)) != AM_SUCCESS)
    {
        if(status == AM_INIT_FAILURE) {
            Log::log(logID, Log::LOG_WARNING, "Agent SSOToken reset.");
            bool gotPermToCleanup = false;
            {
                ScopeLock myLock(lock);
                if(initialized == true) {
                    Log::log(logID, Log::LOG_WARNING,
                         "This thread got permission to reset");
                    initialized = false;
                    gotPermToCleanup = true;
                }
            }
            // only one thread get's permission to cleanup.
            // others wait till cleanup happens.
            if(gotPermToCleanup) {
                // If we need to update the agent SSOToken
                // we cleanup the entire cache.
                policyTable.cleanup();
                Log::log(logID, Log::LOG_WARNING, "Invoking initialize().");
                initialize();
            } else {
                int counter = 0;
                // Just a sleep factor.  The init must happen
                // in approxiamtely in 6 seconds.
                Log::log(logID, Log::LOG_WARNING,
                            "This thread waiting for initialization "
                            "to complete.");
                while(initialized == false && ++counter < 6) {
                    PR_Sleep(PR_TicksPerSecond());
                }
            }
            if(initialized == true) {
                Log::log(logID, Log::LOG_WARNING,
                        "Thread wokeup after successful initialization.");
                // This method is returning false because we have re-initialized   
                // the Agent but we haven't updated the orignal policy
                return false;
            } else {
                throw InternalException(func, "Agent reinitialization failed",
                                status);
            }
        } else {
            // Log:ERROR
            if (AM_NSPR_ERROR == status) {
                status = AM_INVALID_SESSION;
            }
            throw InternalException(func, "Policy query failed.", status);
        }
    } else {
        // Log:INFO
        process_policy_response(policyEntry, env, xmlData);
    }

    Log::log(logID, Log::LOG_INFO, "Successful return from do_update_policy().");
    return true;
}

/*
 * Throws:
 *	std::invalid_argument if any argument is invalid
 *	XMLTree::ParseException upon XML parsing error
 *	InternalException upon other errors
 */
void
Service::update_policy_list(const SSOToken &ssoTok,
			    const vector<string> &resList,
			    const string &actionName,
			    const KeyValueMap &env,
                            PolicyEntryRefCntPtr &policyEntry)
{
    std::vector<string>::const_iterator iter;
    policy_fetch_scope_t scope = SCOPE_SELF;

    if(mFetchFromRootResource == true) {
    	scope = (mOrdNum > 0)?SCOPE_STRICT_SUBTREE:SCOPE_SUBTREE;
    }
    
    SessionInfo sessionInfo;
    for(iter = resList.begin(); iter != resList.end(); iter++) {
	update_policy(ssoTok, *iter, actionName, env, sessionInfo, scope,
                      true, policyEntry);
    }
    return;
}

am_status_t
Service::do_agent_auth_logout()
{
    am_status_t status = AM_SUCCESS;
    try {
        if (authCtx.getSSOToken().size() >= 0) {
            authSvc.logout(authCtx);
        }
    }
    catch (InternalException& iex) {
        status = iex.getStatusCode();
        Log::log(logID, Log::LOG_WARNING, "Service::do_agent_auth_logout(): "
                                          "Internal Exception in agent logout, "
                                          "status %s.",
                                          am_status_to_name(status));
    }
    catch (std::exception& ex) {
        Log::log(logID, Log::LOG_WARNING, "Service::do_agent_auth_logout(): "
                                          "Exception in agent logout, "
                                          "msg [%s].",
                                          ex.what());
        status = AM_AUTH_FAILURE;
    }
    catch (...) {
        Log::log(logID, Log::LOG_WARNING, "Service::do_agent_auth_logout(): "
                                          "Unknown error in agent logout.");
        status = AM_AUTH_FAILURE;
    }
    return status;
}


am_status_t
Service::invalidate_session(const char *ssoTokenId) {
    am_status_t status = AM_FAILURE;
    ServiceInfo svcInfo;
    bool cookieEncoded=strchr(ssoTokenId,'%')!=NULL;
    const SSOToken ssoToken(cookieEncoded?Http::decode(ssoTokenId):ssoTokenId,
			    cookieEncoded?ssoTokenId:Http::encode(ssoTokenId));

	am_web_log_debug("Service::invalidate_session(): Calling LogoutSession");
    status = mSSOTokenSvc.logoutSession(svcInfo, ssoToken.getString());

    PolicyEntryRefCntPtr uPolicyEntry = policyTable.find(ssoToken.getString());
    if (uPolicyEntry) {
        // remove sso token entry from table whether or not logout Session
        // was successful.
        policyTable.remove(ssoToken.getString());
	Log::log(logID, Log::LOG_DEBUG,
	         "Service::invalidate_session(): "
                 "sso token %s removed from policy table.",
		 ssoTokenId);
    }
    return status;
}

void
Service::construct_advice_string(const KeyValueMap &advices,
				 std::string &adviceStr) const {
	
	if (advices.size() > 0) {
		adviceStr.append("<Advices>\n");
		KeyValueMap::const_iterator iter = advices.begin();
		for(; iter != advices.end(); ++iter) {
		const std::string &key = (*iter).first;
		std::vector<std::string>::const_iterator k_iter =
					serverHandledAdvicesList.begin();

		for(; k_iter != serverHandledAdvicesList.end(); ++k_iter) {
			const char *advStr = (*k_iter).c_str();
			if(strcmp(key.c_str(), advStr) == 0) {
			add_attribute_value_pair_xml(iter, adviceStr);
			}
		}
		}
		adviceStr.append("</Advices>\n");
	}
}

void
Service::add_attribute_value_pair_xml(const KeyValueMap::const_iterator &entry,
				      std::string &adviceStr) const {
    adviceStr.append("<AttributeValuePair>\n<Attribute name=\"");
    adviceStr.append((*entry).first);
    adviceStr.append("\"/>\n");

    const std::vector<std::string> values = (*entry).second;
    std::vector<std::string>::const_iterator iter = values.begin();
    for(;iter != values.end(); ++iter) {
	adviceStr.append("<Value>");
	adviceStr.append(*iter);
	adviceStr.append("</Value>\n");
    }
    adviceStr.append("</AttributeValuePair>\n");
    return;
}

