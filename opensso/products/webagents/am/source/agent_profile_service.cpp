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
 * $Id: agent_profile_service.cpp,v 1.23 2009/08/07 21:08:24 subbae Exp $
 *
 */
/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

/*
* This service does the following functions:
* 1. Initial naming request to determine rest service url
* 2. Agent authentication.
* 3. Fetches agent profile properties using REST attribute service.
* 4. Agent logout
*/

#include <string>
#include "agent_profile_service.h"
#include "xml_tree.h"
#include "http.h"
#include "sso_token.h"
#include "auth_svc.h"
#include "am_properties.h"
#include "url.h"
#include "naming_info.h"
#include "naming_service.h"
#include "am_web.h"


USING_PRIVATE_NAMESPACE
#define EXCEPTION "exception"


#define DEFAULT_AGENT_AUTH_MODULE "Application"
#define DEFAULT_AGENT_ORG_NAME "/"
#define ATTRIB_URI "xml/read"
#define LOCAL_CONFIG "local"
#define CENTRAL_CONFIG "remote"

const std::string authURLAttribute("iplanet-am-naming-auth-url");
const std::string restURLAttribute("sun-naming-idsvcs-rest-url");
const std::string protocolPart("%protocol");
const std::string hostPart("%host");
const std::string portPart("%port");
const std::string uriPart("%uri");
const std::size_t MAX_PORT_LENGTH = 5;
const unsigned long DEFAULT_TIMEOUT = 3;
extern smi::SSOTokenService *get_sso_token_service();

AgentProfileService::AgentProfileService(const Properties& config, 
                                         Utils::boot_info_t boot_info_prop)
    : BaseService("Agent Profile Service", config),
      agentAuthnd(false),
      mNamingServiceURL(config.get(AM_COMMON_NAMING_URL_PROPERTY, "")),
      mNamingServiceInfo(mNamingServiceURL),
      mNamingService(config)
{
    /*
      * Instantiate AgentConfigCache 
      * Instantiate AgentConfigurationObj
      * Instantiate AgentConfigFetch;
      * Set Repo Type
      * first instance of AgentConfirutationObj object into the AgentConfigCache.
      */

    boot_info = boot_info_prop;
    if(isRESTServiceAvailable()==AM_REST_SERVICE_NOT_AVAILABLE){
        setRepoType(LOCAL_CONFIG);
    }
    else{
        setRepoType(CENTRAL_CONFIG);
    }
} 

AgentProfileService::~AgentProfileService()
{
}

/*
 * This function retrieves the latest AgentConfiguration instance from the
 * cache.
 */
AgentConfigurationRefCntPtr AgentProfileService::getAgentConfigInstance()
{
    AgentConfigurationRefCntPtr agentConfig = 
                                agentConfigCache.getLatestAgentConfigInstance();

    if (agentConfig == NULL) {
        // Need to refetch the agent configuration, because by the time 
        // we get the latestConfigKey and try to fetch the agent config object
        // associated with this key (before the find_cac() call, the ref_cnt
        // will be zero), the agent config cache cleanup might have
        // already removed this entry.   
        fetchAndUpdateAgentConfigCacheInternal(agentConfig);
    }     
    return agentConfig;
}

/*
 * This function reads the bootstrap properties and updates the properties 
 * object which already has agent configuration data in it.
 */
am_status_t
load_bootinfo_to_properties(Utils::boot_info_t *boot_ptr, am_properties_t properties)
{
//    const char *thisfunc = "load_bootinfo_to_properties()";
    const char *function_name = "am_properties_get";
    am_status_t status = AM_SUCCESS;
    const char *parameter = "";
//    const char *encrypt_passwd = NULL;
    const char *namingURL = NULL;
    const char *agentName = NULL;
    const char *agentPasswd = NULL;
    const char *certDir = NULL;
    const char *certDbPrefix = NULL;
    const char *trustServerCerts = NULL;
    const char *certDbPasswd = NULL;
    const char *certAlias = NULL;
    const char *connReceiveTimeout = NULL;
    const char *connTimeout = NULL;
    const char *connTcpDelay = NULL;
    const char *proxy_host = NULL;
    const char *proxy_port = NULL;
    const char *proxy_user = NULL;
    const char *proxy_password = NULL;
    const char *org_name = NULL;
    const char *debugFile = NULL;
    const char *localAuditLogFile = NULL;
    const char *sharedAgentProfileName = NULL;
    const char *realmName = NULL;
    
    if (AM_SUCCESS == status) {
        parameter = AM_POLICY_PASSWORD_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &agentPasswd);
        am_properties_set(properties, parameter,
                                      agentPasswd);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_POLICY_USER_NAME_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &agentName);
        am_properties_set(properties, parameter,
                                      agentName);
    }

    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_COMMON_NAMING_URL_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &namingURL);
        am_properties_set(properties, parameter,
                                      namingURL);
     }

    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_SSL_CERT_DIR_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &certDir);
        am_properties_set(properties, parameter,
                                      certDir);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_CERT_DB_PREFIX_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &certDbPrefix);
        am_properties_set(properties, parameter,
                                      certDbPrefix);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_TRUST_SERVER_CERTS_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &trustServerCerts);
        am_properties_set(properties, parameter,
                                      trustServerCerts);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_CERT_DB_PASSWORD_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &certDbPasswd);
        am_properties_set(properties, parameter,
                                      certDbPasswd);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_AUTH_CERT_ALIAS_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &certAlias);
        am_properties_set(properties, parameter,
                                      certAlias);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_RECEIVE_TIMEOUT_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &connReceiveTimeout);
        am_properties_set(properties, parameter,
                                      connReceiveTimeout);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_TCP_NODELAY_ENABLE_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &connTcpDelay);
        am_properties_set(properties, parameter,
                                      connTcpDelay);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_CONNECT_TIMEOUT_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &connTimeout);
        am_properties_set(properties, parameter,
                                      connTimeout);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_FORWARD_PROXY_HOST;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &proxy_host);
        am_properties_set(properties, parameter, proxy_host);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_FORWARD_PROXY_PORT;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &proxy_port);
        am_properties_set(properties, parameter, proxy_port);
    }
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_FORWARD_PROXY_USER;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &proxy_user);
        am_properties_set(properties, parameter, proxy_user);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_FORWARD_PROXY_PASSWORD;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &proxy_password);
        am_properties_set(properties, parameter, proxy_password);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_POLICY_ORG_NAME_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                                   &org_name);
        am_properties_set(properties, parameter, org_name);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_AGENT_DEBUG_FILE_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                &debugFile);
        am_properties_set(properties, parameter, debugFile);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_AUDIT_LOCAL_LOG_FILE_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                &localAuditLogFile);
        am_properties_set(properties, parameter, localAuditLogFile);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_AGENT_PROFILE_NAME_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                &sharedAgentProfileName);
        am_properties_set(properties, parameter, sharedAgentProfileName);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_POLICY_ORG_NAME_PROPERTY;
        status = am_properties_get(boot_ptr->properties, parameter,
                &realmName);
        am_properties_set(properties, parameter, realmName);
    }

    return status;
}

/*
 * This function fetches the agent configuration data from either the flat file
 * or OpenAM REST server and updates the AgentConfigCache with the latest object of
 * AgentConfiguration object. This function is invoked once by the constructor
 * of this class and periodically by the polling and notification thread.
 */
am_status_t AgentProfileService::fetchAndUpdateAgentConfigCache()
{
    AgentConfigurationRefCntPtr agentConfig;
    return fetchAndUpdateAgentConfigCacheInternal(agentConfig);
}

/*
 * This function does the actual job of retrieving the agent configuration data
 * if the repository type is local or issuing the REST call to Opensso server
 * to get the latest agent configuration data if the repository type is
 * centralized
 */
am_status_t AgentProfileService::fetchAndUpdateAgentConfigCacheInternal
                           (AgentConfigurationRefCntPtr& agentConfig)
{
    //check for REMOTE/LOCAL, then fetch attributes 
    //set latestConfigkey to the current time and then update AgentConfigCache
    //with a new entry that has the latestConfigKey as the key and agentConfig
    //as the value.

    const char *thisfunc = "fetchAndUpdateAgentConfigCache()";
    am_properties_t properties;
    am_status_t status;
    std::string userName(boot_info.agent_name);
    std::string passwd(boot_info.agent_passwd);
    std::string sharedAgentProfileName(boot_info.shared_agent_profile_name);
    std::string realmName(boot_info.realm_name);
    const char* agentConfigFile = boot_info.agent_config_file;

    if (getRepoType() == LOCAL_CONFIG) {
        am_properties_create(&properties);
        status = am_properties_load(properties,agentConfigFile);
        if (status != AM_SUCCESS ) {
            am_web_log_error("%s: Identity REST Services Endpoint URL not"
                " available. Also failed to load local agent configuration file"
                ", %s. Please check for any configuration errors during "
                "installation.", thisfunc, agentConfigFile);
        }
    }
    else {
            am_properties_t tmpPropPtr;
            status = am_properties_create(&tmpPropPtr);
            
            if (status == AM_SUCCESS) {
                status = getAgentAttributes(agentSSOToken, 
                            sharedAgentProfileName,
                            realmName,
                            tmpPropPtr);               
                if (status == AM_SUCCESS) {
                    const char* repoType = NULL;
                    boolean_t repoKeyPresent;
                    repoKeyPresent = am_properties_is_set(tmpPropPtr,
                        AM_WEB_AGENT_REPOSITORY_LOCATION_PROPERTY);
                    
                    if( repoKeyPresent == B_TRUE) {
                        status = am_properties_get(tmpPropPtr,
                            AM_WEB_AGENT_REPOSITORY_LOCATION_PROPERTY,
                            &repoType );
                        if (status == AM_SUCCESS) {
                            if (strcasecmp(repoType, AGENT_PROPERTIES_LOCAL) == 0) {
                                //LOCAL repo type
                                am_web_log_info("%s:Repository type property is "
                                                "set to %s in agent profile, "
                                                "%s. ", thisfunc, repoType, 
                                                userName.c_str());
                                am_properties_create(&properties);
                                status = am_properties_load(properties,
                                                            agentConfigFile);
                                if (status != AM_SUCCESS) {
                                   am_web_log_error("%s: Repository type property "
                                                    "is set to %s in agent "
                                                    "profile, %s. But Agent "
                                                    "failed to load local %s "
                                                    "file", thisfunc, repoType, 
                                                    userName.c_str(), 
                                                    agentConfigFile);
                                }                               
                            }  else if (strcasecmp(repoType, 
                                       AGENT_PROPERTIES_CENTRALIZED) == 0) {
                                // REMOTE repo type
                                am_properties_create(&properties);
                                am_properties_copy(tmpPropPtr, &properties);
                            } else { //treat as misconfiguration
                                am_web_log_error("%s: Repository type property is "
                                    "present  in agent profile, %s, "
                                    "but invalid value is present, %s."
                                    "Sepcify valid value (local|centralized) "
                                    "to make agent function properly.",
                                    thisfunc, userName.c_str(), repoType);
                                status = AM_REPOSITORY_TYPE_INVALID;
                            }
                        } else { //treat as misconfiguration
                            am_web_log_error("%s:Repository type property is "
                                "present in agent profile, %s, "
                                "but invalid value is present, %s."
                                "Sepcify valid value (local|centralized) "
                                "to make agent function properly.",
                                thisfunc, userName.c_str(), repoType);
                            status = AM_REPOSITORY_TYPE_INVALID;
                        }
                        
                    } else { // repository property doesn't exist
                        am_web_log_warning("%s:Repository type property is not "
                                            "present in agent profile, %s. "
                                            "So Agent tries to load "
                                            "from local %s file", thisfunc,
                                            userName.c_str(), agentConfigFile);
                        am_properties_create(&properties);
                        status = am_properties_load(properties,agentConfigFile);
                        if (status != AM_SUCCESS) {
                            am_web_log_error("%s: Agent failed to load local"
                                             "file, %s", thisfunc, agentConfigFile);
                        }                        
                    }
                } else {
                    am_web_log_error("%s:There is an error while fetching"
                                     " attributes by user %s, using REST service. "
                                     "Status: %s ", thisfunc,
                                     userName.c_str(), 
                                     am_status_to_string(status));
                    status = AM_REST_ATTRS_SERVICE_FAILURE;
                }                
            }
            am_properties_destroy(tmpPropPtr);
    }
    //Insert the AMAgentConfiguration object in the hast table with the current
    //time stamp as its key.
    if (status == AM_SUCCESS ) {
        // AgentConfigurationRefCntPtr agentConfig;
        agentConfig = new AgentConfiguration(properties);
        status = load_bootinfo_to_properties(&boot_info, 
                agentConfig->getProperties());
        agentConfigCache.populateAgentConfigCacheTable(agentConfig);
    }
    return status;
}

/*
 * This function deletes any old agent config instances that are
 * stored in the agentConfigCacheTable
 */
void AgentProfileService::deleteOldAgentConfigInstances()
{
    agentConfigCache.deleteOldAgentConfigInstances();
}

/**
 * Fetches agent profile attributes using REST attribute service
 * Agent has to be authenticated before doing this.
 * If successful, properties object gets loaded with profile attributes
 */
am_status_t AgentProfileService::getAgentAttributes(
    const std::string appSSOToken, 
    const std::string sharedAgentProfileName, 
    const std::string realmName, 
    am_properties_t properties)
{
    am_status_t status = AM_FAILURE;
    Http::Response response;
    std::string::size_type pos;

    std::string encodedAgentToken = Http::encode(appSSOToken);
    std::string encodedSharedAgentProfileName = Http::encode(sharedAgentProfileName);
    std::string encodedRealmName = Http::encode(realmName);

    std::string urlParams = "?name=";
    urlParams.append(encodedSharedAgentProfileName);
    urlParams.append("&attributes_names=realm");
    urlParams.append("&attributes_values_realm=");
    urlParams.append(encodedRealmName); 
    urlParams.append("&attributes_names=objecttype");
    urlParams.append("&attributes_values_objecttype=Agent" );
    urlParams.append("&admin=" );
    urlParams.append(encodedAgentToken);

    Log::log(logModule, Log::LOG_DEBUG,
            "AgentProfileService::getAgentAttributes(): sending request to: %s%s",
            mRestURL.c_str(), urlParams.c_str());
    
    try {
        setRestSvcInfo(mRestURL);
    } catch (InternalException &iex) {
        status = AM_FAILURE;
    }
    
    status =  doHttpGet(mRestSvcInfo, urlParams, Http::CookieList(),
                        response);
    if(status == AM_SUCCESS) {
        std::string xmlResponse(response.getBodyPtr());
        pos = xmlResponse.find(EXCEPTION,0);
        if(pos != std::string::npos){
            status = AM_REST_ATTRS_SERVICE_FAILURE;
        } else {
            try {
                status = parseAgentResponse(xmlResponse, properties);
            } catch(...) {
                Log::log(logModule, Log::LOG_ERROR, 
                    "parseAgentResponse(): Attribute xml parsing error");
                status = AM_REST_ATTRS_SERVICE_FAILURE;
            }
        }
    } else {
        status = AM_REST_ATTRS_SERVICE_FAILURE;
    }
   return status;

}

/**
 * This function logs out the agent user. This function will be called
 * when the web container is stopped.
 */
am_status_t
AgentProfileService::agentLogout(const Properties &config)
{
    am_status_t status = AM_SUCCESS;
    AuthService authSvc(config);
    try {
        if (mAuthCtx.getSSOToken().size() >= 0) {
            authSvc.logout(mAuthCtx);
        }
    }
    catch (InternalException& iex) {
        status = iex.getStatusCode();
        Log::log(logModule, Log::LOG_WARNING, 
            "Service::do_agent_auth_logout(): "
            "Internal Exception in agent logout, "
            "status %s.",  am_status_to_name(status));
    }
    catch (std::exception& ex) {
        Log::log(logModule, Log::LOG_WARNING, 
            "Service::do_agent_auth_logout(): "
            "Exception in agent logout, msg [%s].", ex.what());
        status = AM_AUTH_FAILURE;
    }
    catch (...) {
        Log::log(logModule, Log::LOG_WARNING, 
            "Service::do_agent_auth_logout(): "
            "Unknown error in agent logout.");
        status = AM_AUTH_FAILURE;
    }
    return status;
}


/**
 * Parses agent profile REST attributes service response and sets the
 * properties in properties object.
 *
 * Throws XMLTree::ParseException 
 */
am_status_t  AgentProfileService::parseAgentResponse(const std::string xmlResponse,
					       am_properties_t properties ) 
{
    am_status_t status = AM_SUCCESS;

    XMLTree::Init xt;
    XMLTree namingTree(false, xmlResponse.c_str(), xmlResponse.size());
    XMLElement element = namingTree.getRootElement();
    XMLElement element1; 

    // get the root element userdetails
    if (element.isNamed("identitydetails")) {

        // get the first attribute element
        element.getSubElement("attribute", element);
        if (element.isNamed("attribute")) {

            // iterate through all the attribute elements
            while (element.isValid()) {
                if(element.isNamed("attribute")) {
                    std::string propName;
                    std::string propValue;
                    bool propSet = false;

                    // get property name and assign it to propName
                    if (element.getAttributeValue("name", propName)) {

                        // get property's value(s) and assign it to propValue 
                        element1 = element.getFirstSubElement();
                        if (element1.isNamed("value")) {
                            int i = 0;

                            // if multiple values exist, 
                            //append them with a separator ' '
                             while (element1.isValid()) {
                                 if(element1.isNamed("value")) {
                                     std::string tmpValue;
                                     if (element1.getValue(tmpValue)) {

                                         // Process freeform properties
                                         if(strcasecmp(propName.c_str(), 
                                             AM_WEB_AGENT_FREEFORM_PROPERTY) == 0) {

                                             size_t equals = tmpValue.find("=");
                                             std::string ffPropName = tmpValue.substr(0, equals);
                                             std::string ffPropValue = tmpValue.substr(equals + 1);
                                             am_properties_set(properties, 
                                                 ffPropName.c_str(), 
                                                 ffPropValue.c_str());
                                             Log::log(logModule, Log::LOG_DEBUG, 
                                                 "agentAttributes() %s : %s  ",
                                                 ffPropName.c_str(), ffPropValue.c_str()); 
                                         }

                                         // Process list/map properties
                                         if(isListMapProperty(propName.c_str())) {
                                             size_t equals = tmpValue.find("=");
                                             //the following returns [] portion before =
                                             std::string lmPropNameSuffix = tmpValue.substr(0, equals);
                                             //the following returns value portion after =
                                             std::string lmPropValue = tmpValue.substr(equals + 1);
                                             std::string tmpPropName = propName;
                                             am_properties_set(properties, 
                                                 (tmpPropName.append(lmPropNameSuffix)).c_str(), 
                                                 lmPropValue.c_str());
                                             propSet = true;
                                             Log::log(logModule, Log::LOG_DEBUG, 
                                                 "agentAttributes() %s : %s  ",
                                                 propName.c_str(), lmPropValue.c_str()); 
                                         }

                                         // Process debug.level property
                                         if(strcasecmp(propName.c_str(), 
                                             AM_AGENT_DEBUG_LEVEL_PROPERTY) == 0) {
                                             std::string tmpDebugLevel;
                                             if(strcasecmp(tmpValue.c_str(),"Error") == 0)
                                                 tmpDebugLevel = "all:1";
                                             if(strcasecmp(tmpValue.c_str(),"Warning") == 0)
                                                 tmpDebugLevel = "all:2";
                                             if(strcasecmp(tmpValue.c_str(),"Info") == 0)
                                                 tmpDebugLevel = "all:3";
                                             if(strcasecmp(tmpValue.c_str(),"Message") == 0)
                                                 tmpDebugLevel = "all:4";
                                             if(strcasecmp(tmpValue.c_str(),"All") == 0)
                                                 tmpDebugLevel = "all:5";
                                                  
                                             if(!tmpDebugLevel.empty()) {
                                                  am_properties_set(properties, 
                                                      propName.c_str(), 
                                                      tmpDebugLevel.c_str());
                                                  propSet = true;
                                                 Log::log(logModule, Log::LOG_DEBUG, 
                                                     "agentAttributes() %s : %s  ",
                                                     propName.c_str(), tmpDebugLevel.c_str()); 
                                             }
                                         }
                                         if(i > 0)
                                             propValue.append(" ");
                                   
                                         propValue.append(tmpValue);
                                      } else {
                                          throw XMLTree::ParseException(
                                              "Attribute value missing ");
                                      }
                                  }
                                  element1.nextSibling();
                                  i++;
                              } // end of value element while loop 
                                    
                          } // end of if value check 

                          if(!propSet) {
                              // assign  property name and value in properties object.
                              if(!propValue.empty()) {
                                  am_properties_set(properties, 
                                      propName.c_str(), 
                                      propValue.c_str());
                              } else {
                                  // The following is required for validation of 
                                  // agent repository location value.
                                  if(strcasecmp(propName.c_str(), 
                                    AM_WEB_AGENT_REPOSITORY_LOCATION_PROPERTY) == 0) 
                                  {
                                      am_properties_set_null(properties, 
                                          propName.c_str(), "\0");
                                  }
                              }
                              Log::log(logModule, Log::LOG_DEBUG, 
                                  "agentAttributes() %s : %s", 
                                  propName.c_str(), propValue.c_str());
                          }

                    } else {
                        throw XMLTree::ParseException("Attribute name missing ");
                    }
                }
                element.nextSibling();
            }

            status = AM_SUCCESS;
        } else {
            throw XMLTree::ParseException("attribute element not present in "
                "Attributes Response xml");
        } 
    } else {
        throw XMLTree::ParseException("userdetails root element not present in "
            "Attributes Response xml");
    }

    Log::log(logModule, Log::LOG_DEBUG, "parseAgentResponse() "
        "returning with status %s.", am_status_to_string(status));
    return status;
}


/**
 * Sets mAuthSvcInfo member variable
 * Throws: InternalException upon error
 */
void
AgentProfileService::setAuthSvcInfo(std::string authURL)
{

    try {
        URL verifyURL(authURL);
    } catch (InternalException &iex) {
        throw InternalException("AuthService::setAuthSvcInfo()",
                                "Malformed URL.",
                                AM_AUTH_FAILURE);
    }

    mAuthSvcInfo.setFromString(authURL);
    Log::log(logModule, Log::LOG_MAX_DEBUG,
             "Number of servers in service:%d, '%s'.",
             mAuthSvcInfo.getNumberOfServers(), authURL.c_str());

    return;
}

/**
 * Sets mRestSvcInfo member variable
 * Throws: InternalException upon error
 */
void
AgentProfileService::setRestSvcInfo(std::string restURL)
{

    try {
        URL verifyURL(restURL);
    } catch (InternalException &iex) {
    Log::log(logModule, Log::LOG_ERROR,
             "Error in rest url :'%s' ", restURL.c_str());
        throw InternalException("AuthService::setAuthSvcInfo()",
                                "Malformed URL.",
                                AM_AUTH_FAILURE);
    }

    mRestSvcInfo.setFromString(restURL);
    Log::log(logModule, Log::LOG_MAX_DEBUG,
             "Number of servers in service:%d, '%s'.",
             mRestSvcInfo.getNumberOfServers(), restURL.c_str());

    return;
}


/**
 * This function performs Agent authentication to retrieve the agent
 * agent configuration data from the OpenAM server
 */
am_status_t AgentProfileService::agentLogin()
{
    std::string userName(boot_info.agent_name);
    std::string passwd(boot_info.agent_passwd);
    const Properties& config =
        *reinterpret_cast<Properties *>(boot_info.properties);
    am_status_t status = AM_SUCCESS;

    Log::log(logModule, Log::LOG_DEBUG, 
         "AgentProfileService()::agentLogin() :%s", userName.c_str());
    AuthService authSvc(config);

    const char *thisfunc = "AgentProfileService::agentLogin()";
    std::string orgName;
    std::string moduleName;

    // Get all the parameters.
    // Right now we just handle username/password callback for LDAP module.
    // in the future we may want to handle different callbacks depending
    // on the module.
    orgName = config.get(AM_POLICY_ORG_NAME_PROPERTY,
                            DEFAULT_AGENT_ORG_NAME);
    moduleName = config.get(AM_POLICY_MODULE_NAME_PROPERTY,
                               DEFAULT_AGENT_AUTH_MODULE);

    // Create Auth context, do login.
    try {
        // The following is necessary check, which resolves this case:
        // opensso/am server not running and agent running. When server is up,
        // then agent will be able to authenticate itself and initializes ok.
        if(mAuthURL.empty()) {
            if(isRESTServiceAvailable()==AM_REST_SERVICE_NOT_AVAILABLE){
                setRepoType(LOCAL_CONFIG);
            } else{
                setRepoType(CENTRAL_CONFIG);
            }
        }
        std::string certName, namingURLs;   // dummy strings for linux compile.
        AuthContext authC(orgName, certName, namingURLs);
        Log::log(logModule, Log::LOG_DEBUG, 
            "AgentProfileService()::agentLogin() :%s", mAuthURL.c_str());
        authC.authSvcInfo.setFromString(mAuthURL);

        authSvc.create_auth_context_cac(authC);
        Log::log(logModule, Log::LOG_DEBUG, 
            "AgentProfileService()::agentLogin() :%s", mAuthURL.c_str());
        authSvc.login(authC, AM_AUTH_INDEX_MODULE_INSTANCE, moduleName);

        std::vector<am_auth_callback_t>& callbacks = authC.getRequirements();
        int nCallbacks = (int) callbacks.size();
        if (nCallbacks > 0) {
            bool userNameIsSet = false;
            bool passwdIsSet = false;
            for (int i = nCallbacks-1; i >= 0; --i) {
                am_auth_callback_t& cb = callbacks[i];
                switch(cb.callback_type) {
                    case NameCallback:
                        Log::log(logModule, Log::LOG_DEBUG,
                         "%s: Setting name callback to '%s'.",
                         thisfunc, userName.c_str());
                        cb.callback_info.name_callback.response = 
                            userName.c_str();
                        userNameIsSet = true;
                        break;
                    case PasswordCallback:
                        Log::log(logModule, Log::LOG_DEBUG,
                         "%s: Setting password callback.", thisfunc);
                        cb.callback_info.password_callback.response = 
                            passwd.c_str();
                        passwdIsSet = true;
                        break;
                    default:
                        // ignore unexpected callback.
                        Log::log(logModule, Log::LOG_WARNING,
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
                    status =  AM_AUTH_FAILURE;
                    Log::log(logModule, Log::LOG_ERROR,
                     "Agent failed to login to OpenAM server, auth status %d.",
                     auth_sts);
                    break;
            }
        }
        else {
            Log::log(logModule, Log::LOG_ERROR,
                 "%s: Agent cannot login to OpenAM server: Expected auth callbacks "
                 "for agent login not found.", thisfunc);
            status =  AM_AUTH_FAILURE;
        }

        // set the agent SSO Token.
        agentSSOToken = authC.getSSOToken();
        setEncodedAgentSSOToken(agentSSOToken);

        // save the auth context for logging out when service is destroyed
        mAuthCtx = authC;

        Log::log(logModule, Log::LOG_DEBUG, 
             "%s: Successfully logged in as %s.",
             thisfunc, userName.c_str());
    } catch(...) {
        Log::log(logModule, Log::LOG_DEBUG, 
             "%s: login failed for agent profile:  %s.",
             thisfunc, userName.c_str());
        status =  AM_AUTH_FAILURE;
    }

    return status;

}

/**
 * This fnunction checks whether the incoming naming response has the 
 * REST service url in it. If it has then the agent determines that it is
 * interacting with the OpenAM server. If not present then the agent is interacting 
 * with the old AM server
 */
am_status_t AgentProfileService::isRESTServiceAvailable()
{
    am_status_t status = AM_SUCCESS;

    Http::CookieList cookieList;
    NamingInfo namingInfo;
    const char *thisFunc = "isRESTServiceAvailable()";

    try {
        status = mNamingService.doNamingRequest(mNamingServiceInfo,
                                               cookieList,
                                               namingInfo);
        if (status == AM_SUCCESS) {
            std::string authURL = 
                namingInfo.extraProperties.get(authURLAttribute, "", true);
            parseURL(authURL, false, mAuthURL);

            std::string restURL = 
                namingInfo.extraProperties.get(restURLAttribute, "", true);
            if(!restURL.empty()) {
               Log::log(logModule, Log::LOG_DEBUG,
                     ": Using rest service info from naming response %s:", 
                     restURL.c_str());
               parseURL(restURL, true, mRestURL);
            
            } else {
                status = AM_REST_SERVICE_NOT_AVAILABLE;
                Log::log(logModule, Log::LOG_DEBUG,
                     "%s: No REST service available. "
                     "So local agent configuration file gets used.", 
                      thisFunc);
            }

        } else {
                Log::log(logModule, Log::LOG_ERROR,
                     "%s: An error occured while doing naming request. %s", 
                      thisFunc, am_status_to_string(status));
        }    
    } catch(...) {
        Log::log(logModule, Log::LOG_ERROR,
             "%s: An error occured while doing naming request. %s", 
              thisFunc, am_status_to_string(status));
    }

    return status;
}


/**
 * Parses naming response's service url value.
 *
 * The AM Naming Service returns Naming Service Table
 * URLs without replacing %protocol://%host:%port
 * when the request does not have an SSO token.
 * So all those % values have to replaced using
 * naming.url property value
*/
void AgentProfileService::parseURL(std::string serviceURL, 
                                   bool isRestURL, 
                                   std::string& parsedServiceURL)
{
    am_status_t status = AM_SUCCESS;
    char portBuf[MAX_PORT_LENGTH + 1];
    ServiceInfo::const_iterator iter;

    for (iter = mNamingServiceInfo.begin(); 
         iter != mNamingServiceInfo.end() ; ++iter) {

        std::string protocol = (*iter).getProtocol();
        if(!protocol.empty()) {
        std::string tmpURL = serviceURL;
        std::string hostname = (*iter).getHost();
        unsigned short portnumber = (*iter).getPort();
        snprintf(portBuf, sizeof(portBuf), "%u", portnumber);
        std::string uri = (*iter).getURI();
        char *c_uri = NULL;
        char *uriTok = NULL;

        if (status == AM_SUCCESS) {
            std::size_t pos = 0;

            pos = tmpURL.find (protocolPart, pos);
            while (pos != std::string::npos) {
                tmpURL.replace (pos, protocolPart.size(), protocol);
                pos = tmpURL.find (protocolPart, pos + 1);
            }
            pos = 0;
            pos = tmpURL.find (hostPart, pos);
            while (pos != std::string::npos) {
                tmpURL.replace (pos, hostPart.size(), hostname);
                pos = tmpURL.find (hostPart, pos + 1);
            }
            pos = 0;
            pos = tmpURL.find (portPart, pos);
            while (pos != std::string::npos) {
                tmpURL.replace (pos, portPart.size(), portBuf);
                pos = tmpURL.find (portPart, pos + 1);
            }
            // Need to use strtok as the uri contains eg:opensso/namingservice
            // we want only /opensso
            if (!uri.empty()) {
                c_uri = (char *) malloc(strlen(uri.c_str())+1);
                strcpy(c_uri,uri.c_str());
                uriTok = strtok(c_uri, "/");
                if (uriTok != NULL) {
                    std::string tmpUri(uriTok);
                    std::string newUri("/");
                    newUri.append(tmpUri);
                    
                    pos = 0;
                    pos = tmpURL.find (uriPart, pos);
                    while (pos != std::string::npos) {
                        tmpURL.replace (pos, uriPart.size(), newUri);
                        pos = tmpURL.find (uriPart, pos + 1);
                    }
                }
                free(c_uri);
            }
        }
        // for rest service url, xml/attributes needs to be appended
        if(isRestURL) {
            tmpURL.append(ATTRIB_URI);
        }
        // if multiple urls are present in naming url property, 
        // then those many urls need to be present in 
        // auth service and rest service urls also. 
        tmpURL.append(" ");
        parsedServiceURL.append(tmpURL);
        }
    }
    Log::log(logModule, Log::LOG_MAX_DEBUG,
             "Number of servers in service: '%s'.", 
             parsedServiceURL.c_str());
    return;
} 

bool AgentProfileService::isListMapProperty(const char* propName) {
    bool retVal = false;
    if(strcasecmp(propName,
        AM_WEB_NOT_ENFORCED_LIST_PROPERTY) == 0 ||
       strcasecmp(propName,
        AM_WEB_NOT_ENFORCED_IPADDRESS) == 0 ||
       strcasecmp(propName,
        AM_WEB_FQDN_MAP) == 0 ||
       strcasecmp(propName,
        AM_WEB_COOKIE_RESET_LIST) == 0 ||
       strcasecmp(propName,
        AM_WEB_CDSSO_COOKIE_DOMAIN_LIST) == 0 ||
       strcasecmp(propName,
        AM_WEB_LOGOUT_COOKIE_RESET_PROPERTY) == 0 ||
       strcasecmp(propName,
        AM_POLICY_PROFILE_ATTRS_MAP) == 0 ||
       strcasecmp(propName,
        AM_POLICY_SESSION_ATTRS_MAP) == 0 ||
       strcasecmp(propName,
        AM_POLICY_RESPONSE_ATTRS_MAP) == 0 ||
       strcasecmp(propName,
        AM_WEB_LOGIN_URL_PROPERTY) == 0 || 
       strcasecmp(propName,
        AM_WEB_LOGOUT_URL_PROPERTY) == 0 ||
       strcasecmp(propName,
        AM_WEB_AGENT_LOGOUT_URL_PROPERTY) == 0 ||
       strcasecmp(propName,
        AM_WEB_CDC_SERVLET_URL_PROPERTY) == 0 )
    {
        retVal = true;
    }

    return retVal;
}

/*
* Encoded App SSOToken needs to be sent in
* requester attribute in SessionRequest.
*/
void AgentProfileService::setEncodedAgentSSOToken(std::string appSSOToken) {
    char* encodedAppSSOTokenString = NULL;
    std::string tmpTokenString = "token:";
    tmpTokenString.append(appSSOToken);
    encodedAppSSOTokenString = (char *)
        malloc(((tmpTokenString.size() * 4/3 + 1)/4 + 1)*4 + 4);

    if(encodedAppSSOTokenString != NULL) {
        encode_base64(tmpTokenString.c_str(),
                  tmpTokenString.size(),
                  encodedAppSSOTokenString);
        encodedAgentSSOToken = encodedAppSSOTokenString;
    } else {
        encodedAgentSSOToken = "";
    }

    if(encodedAppSSOTokenString != NULL) {
       free(encodedAppSSOTokenString);
    }
}


/*
* Validate App SSOToken
*/
am_status_t AgentProfileService::validateAgentSSOToken() {
    am_status_t sts = AM_FAILURE;
    const char *thisfunc = "AgentProfileService::validateAgentSSOToken()";
    SessionInfo *sessionInfo = NULL;
    try {
        sessionInfo = new SessionInfo();
        Http::CookieList cookieList;
        SSOTokenService *ssoTokenSvc = get_sso_token_service();
        sts = ssoTokenSvc->getSessionInfo(ServiceInfo(),
                                          agentSSOToken,
                                          cookieList,
                                          false,
                                          *sessionInfo,
                                          true);
        if (sts == AM_SUCCESS) {
            sts = sessionInfo->isValid() ? AM_SUCCESS : AM_INVALID_SESSION;
        }
        else {
            Log::log(logModule, Log::LOG_ERROR,
                 "%s: Invalid Agent(app) SSO token %s.",
                 thisfunc, agentSSOToken.c_str());
        }
    }
    catch (InternalException& ex) {
        Log::log(logModule, Log::LOG_ERROR, ex);
        sts = AM_FAILURE;
    }
    catch (std::bad_alloc& exb) {
        Log::log(logModule, Log::LOG_ERROR, exb);
        sts = AM_NO_MEMORY;
    }
    catch (std::exception& exs) {
        Log::log(logModule, Log::LOG_ERROR, exs);
        sts = AM_FAILURE;
    }
    catch (...) {
        Log::log(logModule, Log::LOG_ERROR,
                 "%s: Unknown exception encountered.");
        sts = AM_FAILURE;
    }

    delete sessionInfo;
    sessionInfo = NULL;
    
    return sts;
}
