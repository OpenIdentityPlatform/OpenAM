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
 * $Id: agent_configuration.cpp,v 1.22 2010/03/10 05:08:55 dknab Exp $
 *
 * Abstract:
 * AgentConfiguration: This class creates/delets the agent configuration 
 * object which contains all the bootstrap and configurable properties. 
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

#include <string>
#include "agent_configuration.h"
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
#define IIS_FILTER_PRIORITY  "DEFAULT"
#define	HTTP_PREFIX	"http://"
#define	HTTP_PREFIX_LEN (sizeof(HTTP_PREFIX) - 1)
#define	HTTP_DEF_PORT	80
#define	HTTPS_PREFIX	"https://"
#define	HTTPS_PREFIX_LEN (sizeof(HTTPS_PREFIX) - 1)
#define HTTPS_DEF_PORT	443
        
#define LOG_TYPE_NONE    "LOG_NONE"
#define LOG_TYPE_ALLOW   "LOG_ALLOW"
#define LOG_TYPE_DENY    "LOG_DENY"
#define LOG_TYPE_BOTH    "LOG_BOTH"
        
#define LOG_ACCESS_NONE    0x0
#define LOG_ACCESS_ALLOW   0x1
#define LOG_ACCESS_DENY    0x2
        
#define MAGIC_STR		"sunpostpreserve"
#define DUMMY_NOTENFORCED	"/dummypost*"
#define DUMMY_REDIRECT		"/dummypost/"
        
#define CONNECT_TIMEOUT	2
        
AgentConfiguration::AgentConfiguration(am_properties_t props) 
    : RefCntObj()
{
    initAgentConfiguration();
    initLoginURLList();    
    initLogoutURLList();    
    initAgentLogoutURLList();    
    initNotEnforcedURLList();    
    initCDSSOURLList();
    initCookieList();
    initLogoutCookieList();
    initAgentServerURLList();
    
    setProperties(props);
    error = populateAgentProperties();
    
} // constructor

AgentConfiguration::~AgentConfiguration() 
{
    // Need to invoke the properties cleanup functions
    cleanup_properties();
}

smap_t conditional_login_url(const char *pv) {
    smap_t cond_v_m;
    slist_t cond_login_url_l, url_l;
    std::string prms(pv);
    size_t found;
    /*tokenize multi-value parameter*/
    tokenize(prms, cond_login_url_l, " ", true);
    for (slist_t::const_iterator ur = cond_login_url_l.begin(); ur != cond_login_url_l.end(); ++ur) {
        std::string const& str = *ur;
        /* get configured request url (first) and associated login url value (second)
         * first - string value to match
         * second - comma separated login url value list
         */
        if ((found = str.find_last_of("|")) != std::string::npos) {
            /*create a list of login url values*/
            if (found > 0) tokenize(str.substr(found + 1), url_l, ",", true);
            if (!url_l.empty()) cond_v_m.insert(spair_t(str.substr(0, found), url_l));
        }
        url_l.clear();
        found = std::string::npos;
    }
    return cond_v_m;
}

am_status_t AgentConfiguration::populateAgentProperties() 
{
    
    const char *thisfunc = "populateAgentPropeties()";
    am_status_t status = AM_FAILURE;
    const char *function_name = "am_properties_create";
    const char *parameter = "";
    const char *agent_prefix_url = NULL;
    const char *url_redirect_default = "goto";
    const char *user_id_default = "UserToken";
    const char *authLogType_default = LOG_TYPE_NONE;
    bool urlstatssl = false;
    bool urlstatnonssl = false;
    const char *filterPriority_default=IIS_FILTER_PRIORITY;
    am_resource_traits_t rsrcTraits;

    function_name = "am_properties_get";

    /* Get the debug level */

    parameter = AM_AGENT_DEBUG_LEVEL_PROPERTY;
    const char* debugLevels;
    status = am_properties_get_with_default(this->properties,
                parameter,
                "all:3",
                &debugLevels);
    if (AM_SUCCESS == status) {
       status = am_log_set_levels_from_string(debugLevels);
    }

    // get debug file size
    if (AM_SUCCESS == status) {
        parameter = AM_AGENT_DEBUG_FILE_SIZE_PROPERTY;
        status = am_properties_get_positive_number(this->properties, parameter,
                DEBUG_FILE_DEFAULT_SIZE, &this->debugFileSize);
        am_log_set_debug_file_size(this->debugFileSize);
    }

    // get debug file rotate flag
    if (AM_SUCCESS == status) {
        parameter = AM_AGENT_DEBUG_FILE_ROTATE_PROPERTY;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_TRUE,
                &(this->debugFileRotate));
        am_log_set_debug_file_rotate(
            this->debugFileRotate ? B_TRUE : B_FALSE);
    }

    /* Get dpro cookie name.*/
    parameter = AM_COMMON_COOKIE_NAME_PROPERTY;
    status = am_properties_get(this->properties, parameter,
                &this->cookie_name);
    if (AM_SUCCESS == status) {
       this->cookie_name_len = strlen(this->cookie_name);
    }
    
    /* Get the is_cookie_secure flag */
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_COOKIE_SECURE_PROPERTY;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter,
                AM_FALSE, &this->is_cookie_secure);
    }
    /* Get the is_cookie_httponly flag */
    if (AM_SUCCESS == status) {
        status = am_properties_get_boolean_with_default(
                this->properties, "com.sun.identity.cookie.httponly",
                AM_FALSE, &this->is_cookie_httponly);
    }
    
    /* Get fqdn.check.enable */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_FQDN_CHECK_ENABLE;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_TRUE,
                &this->fqdn_check_enable);
    }
    
    
    /* Get fqdn_default value */
    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_WEB_FQDN_DEFAULT;
        status = am_properties_get(this->properties, parameter,
                &this->fqdn_default);
        if (AM_SUCCESS == status) {
            this->fqdn_default_len = strlen(this->fqdn_default);
            const char *temp = strchr(this->fqdn_default, '.');
            if (temp != NULL) {
                this->cookie_reset_default_domain = ++temp;
            }
        }
    }
    
    /* set fqdn.map */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_FQDN_MAP;
        status = am_properties_set_map(this->properties,
                parameter,
                "|",
                ",");
    }

    /* Get the cookie domain list. */
    if (AM_SUCCESS == status) {
        const char *cookie_domain_listptr;
        this->cookie_domain_list = NULL;
        parameter = AM_WEB_CDSSO_COOKIE_DOMAIN_LIST;

        status =am_properties_set_list(this->properties, parameter, " ");

        status = am_properties_get_with_default(this->properties, parameter,
                NULL, &cookie_domain_listptr);
        if(NULL != cookie_domain_listptr && '\0' != cookie_domain_listptr[0]) {
            am_web_log_info("calling parseCookieDomains(): "
                    "cookie_domain_listptr: %s",
                    cookie_domain_listptr);
            this->cookie_domain_list = new std::set<std::string>();
            if(this->cookie_domain_list == NULL) {
                status = AM_NO_MEMORY;
            }
            else {
                Utils::parseCookieDomains(cookie_domain_listptr,
                        *(this->cookie_domain_list));
            }
        }
    }
    
    /* Get the denied URL.*/
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_ACCESS_DENIED_URL_PROPERTY;
        status = am_properties_get_with_default(this->properties,
                parameter, NULL,
                &this->access_denied_url);
        
        if(this->access_denied_url != NULL) {
            urlstatnonssl = (strncasecmp(this->access_denied_url,
                    HTTP_PREFIX, HTTP_PREFIX_LEN) == 0);
            urlstatssl = (strncasecmp(this->access_denied_url,
                    HTTPS_PREFIX, HTTPS_PREFIX_LEN) == 0);
            
            if( (urlstatnonssl == false) && (urlstatssl == false) ){
                am_web_log_warning(
                        "Invalid URL (%s) for property (%s) specified",
                        this->access_denied_url == NULL ? "NULL" :
                            this->access_denied_url,
                                    AM_WEB_ACCESS_DENIED_URL_PROPERTY);
            }
            
        }
        
        if ((AM_SUCCESS == status &&
                this->access_denied_url != NULL &&
                '\0' == *this->access_denied_url) ||
                ((urlstatnonssl == false) && (urlstatssl == false))) {
            /*
             * Treat an empty property value as if the property was not
             * specified at all.
             */
            this->access_denied_url = NULL;
            am_web_log_warning("Setting access_denied_url to null");
        }
    }
    
    /* Get the logout redirect URL.*/
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_LOGOUT_REDIRECT_URL_PROPERTY;
        status = am_properties_get_with_default(this->properties,
                parameter, NULL,
                &this->logout_redirect_url);
        
        
        if ((AM_SUCCESS == status &&
                this->logout_redirect_url != NULL &&
                '\0' == *this->logout_redirect_url) ) {
            /*
             * Treat an empty property value as if the property was not
             * specified at all.
             */
            this->logout_redirect_url = NULL;
            am_web_log_warning("Setting logout_redirect_url to null");
        }
    }


    /* Get the UnAuthenticated User info */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_ANONYMOUS_USER;
        status = am_properties_get_with_default(this->properties,
                parameter, NULL,
                &this->unauthenticated_user);
        if (AM_SUCCESS == status &&
                this->unauthenticated_user != NULL &&
                '\0' == *this->unauthenticated_user) {
            
            /*
             * Treat an empty property value as if the property was not
             * specified at all.
             */
            this->unauthenticated_user = NULL;
        }
    }
    
    
    /* Get the Anonymous Remote User Enabled flag */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_ANON_REMOTE_USER_ENABLE;
        if (this->unauthenticated_user != NULL) {
            status = am_properties_get_boolean_with_default(
                    this->properties, parameter,
                    AM_FALSE, &this->anon_remote_user_enable);
        } else {
            am_web_log_warning("Invalid  Unauthenticated User : %s Disabled",
                    parameter);
            this->anon_remote_user_enable = AM_FALSE;
        }
    }
    
    /* Get the URL Redirect param  */
    if (AM_SUCCESS == status) {
	parameter = AM_WEB_URL_REDIRECT_PARAM;
	status = am_properties_get_with_default(this->properties, parameter,
			url_redirect_default, &this->url_redirect_param);
    }

    /* Get the User Id param  */
    if (AM_SUCCESS == status) {
        parameter = AM_POLICY_USER_ID_PARAM_PROPERTY;
        status = am_properties_get_with_default(this->properties, parameter,
                user_id_default, &this->user_id_param);
    }
    
    /* Get the auth log type param  */
    if (AM_SUCCESS == status) {
        parameter = AM_AUDIT_ACCESS_TYPE_PROPERTY;
        status = am_properties_get_with_default(this->properties,
                parameter, authLogType_default,
                &this->authLogType_param);
        this->log_access_type = 0;
        if (!strcasecmp(this->authLogType_param, LOG_TYPE_ALLOW)) {
            this->log_access_type |= LOG_ACCESS_ALLOW;
        }
        else if (!strcasecmp(this->authLogType_param, LOG_TYPE_DENY)) {
            this->log_access_type |= LOG_ACCESS_DENY;
        }
        else if (!strcasecmp(this->authLogType_param, LOG_TYPE_BOTH)) {
            this->log_access_type |= LOG_ACCESS_ALLOW|LOG_ACCESS_DENY;
        }
    }
    
    /* Get the CDSSO URL */
    if(AM_SUCCESS == status) {
        am_bool_t fetchCDSSOURL = AM_FALSE;
        const char *property_str = NULL;
        parameter = AM_WEB_CDSSO_ENABLED_PROPERTY;
        am_properties_get_boolean_with_default(this->properties,
                parameter, AM_FALSE,
                &fetchCDSSOURL);
        if(fetchCDSSOURL) {
            parameter = AM_WEB_CDC_SERVLET_URL_PROPERTY;
            status =am_properties_set_list(this->properties, parameter, " ");

            status = am_properties_get(this->properties,
                    parameter,
                    &property_str);
            if(AM_SUCCESS == status &&
                    property_str != NULL && property_str[0] != '\0') {
                status = Utils::parse_url_list(property_str, ' ',
                        &this->cdsso_server_url_list,
                        AM_TRUE);
            } else {
                if (status == AM_SUCCESS &&
                        (property_str == NULL || property_str[0] == '\0')) {
                    status = AM_NOT_FOUND;
                }
                
                am_web_log_error("am_web_init(): "
                        "CDSSO is enabled but could not get value "
                        "for property %s: %s.",
                        AM_WEB_CDSSO_ENABLED_PROPERTY,
                        am_status_to_string(status));
            }
        }
    }
    
    /* Get the agent server url */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_URI_PREFIX;
        status = am_properties_get(this->properties, parameter,
                &agent_prefix_url);
        
        if (AM_SUCCESS == status) {
            
            status = Utils::parse_url(agent_prefix_url, 
                    strlen(agent_prefix_url),
                    &this->agent_server_url,
                    AM_TRUE);
            if ( AM_SUCCESS == status) {
                am_web_log_info("%s: %s : Value => %s",
                        thisfunc, AM_WEB_URI_PREFIX,
                        this->agent_server_url.url);
            } else {
                am_web_log_warning(
                        "%s: Invalid URL for %s: Value = %s",
                        thisfunc, AM_WEB_URI_PREFIX, agent_prefix_url);
            }
            
        } else {
            am_web_log_error("%s: Invalid URL for %s : Value => %s",
                    thisfunc, AM_WEB_URI_PREFIX, agent_prefix_url);
        }
    }
    
    /* Get the login URL.*/
    if (AM_SUCCESS == status) {
        const char *property_str;
        parameter = AM_WEB_LOGIN_URL_PROPERTY;

        status =am_properties_set_list(this->properties, parameter, " ");

        status = am_properties_get(this->properties, parameter,
                &property_str);
        if (AM_SUCCESS == status) {
            status = Utils::parse_url_list(property_str, ' ',
                    &this->login_url_list, AM_TRUE);
        }
    }
    
    /* Get conditional login URL*/
    if (AM_SUCCESS == status) {
        const char *property_str;
        am_status_t cond_login_status = am_properties_set_list(this->properties, "com.forgerock.agents.conditional.login.url", " ");
        cond_login_status = am_properties_get(this->properties, "com.forgerock.agents.conditional.login.url", &property_str);
        if (AM_SUCCESS == cond_login_status && property_str != NULL) {
            this->cond_login_url = conditional_login_url(property_str);
        }
    }
    
    /* Get the notification.enable */
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_NOTIFICATION_ENABLE_PROPERTY;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter, AM_FALSE,
                &this->notification_enable);
    }

    /* Get the change.notification.enable */
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_CONFIG_CHANGE_NOTIFICATION_ENABLE_PROPERTY;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter, AM_FALSE,
                &this->configChangeNotificationEnable);
    }

    /* Get the notification URL.*/
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_NOTIFICATION_URL_PROPERTY;
        status = am_properties_get_with_default(this->properties,
                parameter, "",
                &this->notification_url);
        if (this->notification_url == NULL ||
                strlen(this->notification_url) == 0) {
            this->notification_enable = AM_FALSE;
            this->configChangeNotificationEnable = AM_FALSE;
        }
    }

    /* Get notenforced_IP handler mode*/
    if (AM_SUCCESS == status) {
        status = am_properties_get_with_default(this->properties, "com.forgerock.agents.config.notenforced.ip.handler", NULL, &(this->notenforcedIPmode));
        am_web_log_max_debug("Property [com.forgerock.agents.config.notenforced.ip.handler] value set to [%s]", 
                this->notenforcedIPmode != NULL ? this->notenforcedIPmode : "");
    }
    
    /* Get post-data preservation URL prefix value */
    if (AM_SUCCESS == status) {
        status = am_properties_get_with_default(this->properties, "com.forgerock.agents.config.pdpuri.prefix", NULL, &(this->dummyPostPrefixUri));
        /*dummyPostPrefixUri should be either set with non-empty value or NULL (not-enforced dummy_post_url_match processing requirement)*/
        if (this->dummyPostPrefixUri != NULL && strlen(this->dummyPostPrefixUri) == 0) {
            this->dummyPostPrefixUri = NULL;
        }
        am_web_log_max_debug("Property [com.forgerock.agents.config.pdpuri.prefix] value set to [%s]", 
                this->dummyPostPrefixUri != NULL ? this->dummyPostPrefixUri : "");
    }

    /* Get the redirect composite advice param */
    if (AM_SUCCESS == status) {
        parameter = "com.sun.am.use_redirect_for_advice";
        status = am_properties_get_boolean_with_default(this->properties,
                parameter, AM_FALSE,
                &this->use_redirect_for_advice);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->use_redirect_for_advice ? "TRUE" : "FALSE"));
    }
    
    /* Get encode cookie value extracted from LARES response param */
    if (AM_SUCCESS == status) {
        parameter = "com.forgerock.agents.cdsso.cookie.urlencode";
        status = am_properties_get_boolean_with_default(this->properties,
                parameter, AM_FALSE,
                &this->cdsso_cookie_urlencode);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->cdsso_cookie_urlencode ? "TRUE" : "FALSE"));
    }
    
    /* CDSSO: enable/disable extra-302-redirect on (after) LARES post */
    if (AM_SUCCESS == status) {
        parameter = "com.forgerock.agents.cdsso.disable.redirect.on_post";
        status = am_properties_get_boolean_with_default(this->properties,
                parameter, AM_FALSE,
                &this->cdsso_disable_redirect_on_post);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->cdsso_disable_redirect_on_post ? "TRUE" : "FALSE"));
    }

    if (AM_SUCCESS == status) {
        parameter = "com.forgerock.agents.cache_control_header.enable";
        status = am_properties_get_boolean_with_default(this->properties,
                parameter, AM_FALSE,
                &this->cache_control_header_enable);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->cache_control_header_enable ? "TRUE" : "FALSE"));
    }

    if (AM_SUCCESS == status) {
        parameter = "com.sun.identity.agents.config.iis.password.header";
        am_properties_get_boolean_with_default(this->properties, parameter, AM_FALSE,
                &this->password_header_enabled);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->password_header_enabled ? "TRUE" : "FALSE"));
    }

    if (AM_SUCCESS == status) {
        parameter = "com.sun.identity.agents.config.iis.logonuser";
        am_properties_get_boolean_with_default(this->properties, parameter, AM_FALSE,
                &this->iis_logonuser_enabled);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->iis_logonuser_enabled ? "TRUE" : "FALSE"));
    }

    if (AM_SUCCESS == status) {
        parameter = "com.sun.identity.agents.config.replaypasswd.key";
        am_properties_get_with_default(this->properties, parameter, NULL, &(this->password_encr_key));
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, this->password_encr_key != NULL ? this->password_encr_key : "");
    }
    
    if (AM_SUCCESS == status) {
        parameter = "com.forgerock.agents.remote_user_header.disable";
        status = am_properties_get_boolean_with_default(this->properties,
                parameter, AM_FALSE,
                &this->remote_user_header_disable);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->remote_user_header_disable ? "TRUE" : "FALSE"));
    }
    
    if (AM_SUCCESS == status) {
        parameter = "com.forgerock.agents.notenforced.url.regex.enable";
        am_properties_get_boolean_with_default(this->properties, parameter, AM_FALSE,
                &this->nfurl_regex_enabled);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->nfurl_regex_enabled ? "TRUE" : "FALSE"));
    }
    
    if (AM_SUCCESS == status) {
        parameter = "com.forgerock.agents.agent.logout.url.regex";
        am_properties_get_with_default(this->properties, parameter, NULL, &(this->alogout_regex));
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, this->alogout_regex != NULL ? this->alogout_regex : "");
    }
    
    if (AM_SUCCESS == status) {
        parameter = "com.forgerock.agents.config.logout.redirect.disable";
        am_properties_get_boolean_with_default(this->properties, parameter, AM_FALSE,
                &this->user_logout_redirect_disable);
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, (this->user_logout_redirect_disable ? "TRUE" : "FALSE"));
    }
    
    if (AM_SUCCESS == status) {
        parameter = "com.forgerock.agents.agent.invalid.url.regex";
        am_properties_get_with_default(this->properties, parameter, NULL, &(this->invalid_url_regex));
        am_web_log_max_debug("Property [%s] value set to [%s]", parameter, this->invalid_url_regex != NULL ? this->invalid_url_regex : "");
    }
    
    /* Get url string comparision case sensitivity values. */
    if (AM_SUCCESS == status) {
        status = am_properties_get_boolean_with_default(this->properties,
                AM_POLICY_URL_COMPARISON_CASE_IGNORE_PROPERTY, AM_FALSE,
                &this->url_comparison_ignore_case);
    }
    
    /* Get the POST data cache preserve status */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_POST_CACHE_DATA_PRESERVE;
        status =
                am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->postdatapreserve_enable);
    }
 
    /* Get the mode the LB uses to get the sticky session */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_POST_CACHE_DATA_PRESERVE_STICKY_SESSION_MODE;
        status = am_properties_get_with_default(this->properties,
                                    parameter, "", 
                                    &this->postdatapreserve_sticky_session_mode);
    }

    /* Get the value of the sticky session to use with post preservation.*/
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_POST_CACHE_DATA_PRESERVE_STICKY_SESSION_VALUE;
        status = am_properties_get_with_default(this->properties,
                                    parameter, "", 
                                    &this->postdatapreserve_sticky_session_value);
    }

    /* Get the POST cache entry lifetime */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_POST_CACHE_ENTRY_LIFETIME;
        status = am_properties_get_unsigned_with_default(this->properties,
                parameter, 3UL,
                &this->postcacheentry_life);
    }
    
    /* Get locale setting */
    if(AM_SUCCESS == status) {
        parameter = AM_WEB_LOCALE_PROPERTY;
        am_properties_get_with_default(this->properties, parameter,
                NULL, &this->locale);
    }
    
    /* Get client_ip_validation.enable.*/
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_CHECK_CLIENT_IP_PROPERTY;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->check_client_ip);
    }
    
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_CONVERT_MBYTE_ENABLE;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->convert_mbyte);
    }
    
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_ENCODE_URL_SPECIAL_CHARS;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->encode_url_special_chars);
    }
    
    /* Get notenforced_client_IP_address */
    if (AM_SUCCESS == status) {
        const char *not_enforced_ipstr;
        parameter = AM_WEB_NOT_ENFORCED_IPADDRESS;

        status =am_properties_set_list(this->properties, parameter, " ");

        status = am_properties_get_with_default(this->properties,
                parameter,
                NULL,
                &not_enforced_ipstr);
        if (not_enforced_ipstr != NULL)
            am_web_log_info("calling parseIPAddresses(): not_enforced_ipstr: %s",
                    not_enforced_ipstr);
        this->not_enforce_IPAddr = new std::set<std::string>();
        if(this->not_enforce_IPAddr == NULL) {
            status = AM_NO_MEMORY;
        }
        if (AM_SUCCESS == status && not_enforced_ipstr != NULL) {
            Utils::parseIPAddresses(not_enforced_ipstr,
                    *(this->not_enforce_IPAddr));
        }
    }
    
    
    /* Get the not enforced list.*/
    if (AM_SUCCESS == status) {
        const char *not_enforced_str;
        
        parameter = AM_WEB_NOT_ENFORCED_LIST_PROPERTY;

        status =am_properties_set_list(this->properties, parameter, " ");

        status = am_properties_get_with_default(this->properties,
                parameter,
                NULL,
                &not_enforced_str);
        
        if (AM_SUCCESS == status) {
            status = Utils::parse_url_list(not_enforced_str, ' ',
                    &this->not_enforced_list, AM_FALSE);
        }
    }
    
    /* Get reverse_the_meaning_of_notenforcedList */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_REVERSE_NOT_ENFORCED_LIST;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->reverse_the_meaning_of_not_enforced_list);
    }
    
    /* Get notenforced_url_attributes_enable */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_NOTENFORCED_URL_ATTRS_ENABLED_PROPERTY;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->notenforced_url_attributes_enable);
    }
    
    /* Get do_sso_only.*/
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_DO_SSO_ONLY;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->do_sso_only);
    }
    
    /* Get CDSSO Enabled/Disabled */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_CDSSO_ENABLED_PROPERTY;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->cdsso_enable);
    }
    
    /* Get Logout URLs if any */
    if (AM_SUCCESS == status) {
        this->logout_url_list.size = 0;
        parameter = AM_WEB_LOGOUT_URL_PROPERTY;
        status =am_properties_set_list(this->properties, parameter, " ");

        const char *logout_url_str;
        status = am_properties_get_with_default(this->properties,
                parameter,
                NULL,
                &logout_url_str);
        if (AM_SUCCESS == status && logout_url_str != NULL) {
            am_web_log_max_debug("am_web_init(): Logout URL is %s.",
                    logout_url_str);
            status = Utils::parse_url_list(logout_url_str, ' ',
                    &this->logout_url_list, AM_TRUE);
        }
    }
    
    /* Get Agent Logout URLs if any */
    if (AM_SUCCESS == status) {
        this->agent_logout_url_list.size = 0;
        parameter = AM_WEB_AGENT_LOGOUT_URL_PROPERTY;
        status =am_properties_set_list(this->properties, parameter, " ");

        const char *agent_logout_url_str;
        status = am_properties_get_with_default(this->properties,
                parameter,
                NULL,
                &agent_logout_url_str);
        if (AM_SUCCESS == status && agent_logout_url_str != NULL) {
            am_web_log_max_debug("am_web_init(): Agent Logout URL is %s.",
                    agent_logout_url_str);
            status = Utils::parse_url_list(agent_logout_url_str, ' ',
                    &this->agent_logout_url_list, AM_TRUE);

        }
    }

    /* Get Logout Cookie reset list if any */
    if (AM_SUCCESS == status) {
        this->logout_cookie_reset_list.size = 0;
        this->logout_cookie_reset_list.list = NULL;
        parameter = AM_WEB_LOGOUT_COOKIE_RESET_PROPERTY;
        const char *logout_cookie_reset_str = NULL;

        status =am_properties_set_list(this->properties, parameter, ",");

        status = am_properties_get_with_default(this->properties,
                parameter,
                NULL,
                &logout_cookie_reset_str);
        if (AM_SUCCESS == status) {
            if (NULL != logout_cookie_reset_str &&
                    '\0' != logout_cookie_reset_str[0]) {
                am_web_log_max_debug("logout cookie reset list is %s.",
                        logout_cookie_reset_str);
                status = Utils::parseCookieList(logout_cookie_reset_str, ',',
                        &this->logout_cookie_reset_list);
                if (AM_SUCCESS == status) {
                    status = Utils::initCookieResetList(
                            &this->logout_cookie_reset_list,
                            strlen(this->cookie_reset_default_domain),
                            this->cookie_reset_default_domain);
                }
            }
            else {
                am_web_log_max_debug("no cookies to be reset on logout.");
            }
        }
    }
    
    /* Get Reset Cookie Enabled/Disabled */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_COOKIE_RESET_ENABLED;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->cookie_reset_enable);
    }
    
    /* Get the List of Cookies to be Reset */
    
    if (AM_SUCCESS == status) {
        if (this->cookie_reset_enable == AM_TRUE) {
            const char *cookie_str = NULL;
            parameter = AM_WEB_COOKIE_RESET_LIST;

            status =am_properties_set_list(this->properties, parameter, ",");

            status = am_properties_get_with_default(this->properties,
                    parameter,
                    NULL,
                    &cookie_str);
            if (AM_SUCCESS == status &&
                    cookie_str != NULL && '\0' != cookie_str[0]) {
                am_web_log_max_debug("%s: cookies to be reset: %s",
                        thisfunc, cookie_str);
                status = Utils::parseCookieList(
                        cookie_str, ',', &this->cookie_list);
                if ( AM_SUCCESS == status) {
                    status = Utils::initCookieResetList(&this->cookie_list,
                            strlen(this->cookie_reset_default_domain),
                            this->cookie_reset_default_domain);
                }
            }
            else {
                this->cookie_reset_enable = AM_FALSE;
                am_web_log_max_debug("%s: no cookies to be reset.", thisfunc);
            }
        }
        else {
            am_web_log_max_debug(
                    "%s: cookie reset enabled property %s is false",
                    thisfunc, AM_WEB_COOKIE_RESET_ENABLED);
        }
    }
    
    /* Get whether client's host name should be looked up and given to policy */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_GET_CLIENT_HOSTNAME;
        status = am_properties_get_boolean_with_default(
                this->properties,
                parameter, AM_TRUE,
                &this->getClientHostname);
        if (status != AM_SUCCESS) {
            am_web_log_warning("%s: Error %s while getting %s property. "
                    "Defaulting to true.", thisfunc,
                    am_status_to_string(status),
                    parameter);
            this->getClientHostname = AM_TRUE;
            status = AM_SUCCESS;
        }
    }
    
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_OVERRIDE_PROTOCOL;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->override_protocol));
    }
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_OVERRIDE_HOST;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->override_host));
    }
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_OVERRIDE_PORT;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->override_port));
    }
    if (AM_SUCCESS == status && this->notification_enable) {
        parameter = AM_WEB_OVERRIDE_NOTIFICATION_URL;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->override_notification_url));
    }
    
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_IGNORE_PATH_INFO;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->ignore_path_info));
    }

    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_IGNORE_PATH_INFO_FOR_NOT_ENFORCED_LIST;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->ignore_path_info_for_not_enforced_list));
    }
    
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_CONNECTION_TIMEOUT;
        status = am_properties_get_positive_number(this->properties,
                parameter, CONNECT_TIMEOUT, &this->connection_timeout);
    }
    
    
    /* Policy clock skew */
    if (AM_SUCCESS == status) {
        parameter = AM_POLICY_CLOCK_SKEW;
        status = am_properties_get_positive_number(this->properties,
                                       parameter, 0, &this->policy_clock_skew);
    }

    /* To skip is_server_alive()? */
    if (AM_SUCCESS == status) {
        parameter = AM_COMMON_IGNORE_SERVER_CHECK;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->ignore_server_check);
    }

    /* Proxy host name if configured */
    if (AM_SUCCESS == status) {
        const char * proxyHost = NULL;
        parameter = AM_COMMON_FORWARD_PROXY_HOST;
	status = am_properties_get_with_default(this->properties, parameter, NULL, 
						&proxyHost); 
        if (AM_SUCCESS == status && proxyHost != NULL && proxyHost[0] != '\0') {
            /* is_server_alive always skipped when proxy is specified */
            /* no direct tcp/ip connection through proxy */ 
	    this->ignore_server_check = AM_TRUE;
        }
    }

    /* Get iis6 auth_type property */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_AUTHTYPE_IN_IIS6_AGENT;
        status = am_properties_get_with_default(this->properties,
                parameter, "dsame",
                &(this->authtype));
        
    }
    
    /* Get iis6 replay passwd key if defined */
    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_COMMON_PROPERTY_PREFIX_IIS6_REPLAYPASSWD_KEY;
        status = am_properties_get_with_default(this->properties, parameter,
                NULL, &this->iis6_replaypasswd_key);
    }
    
    /* Get the IIS5 filter priority param  */
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_FILTER_PRIORITY;
        status = am_properties_get_with_default(this->properties,
                parameter, filterPriority_default,
                &this->filter_priority);
        am_web_log_info("Default priority => %s : Actual priority  => %s",
                filterPriority_default, this->filter_priority);
        
    }
    
    
    // get owa.enable flag
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_OWA_ENABLED;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->owa_enable));
    }
    
    // get owa.enable.change.protocol flag
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_OWA_ENABLED_CHANGE_PROTOCOL;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->owa_enable_change_protocol));
    }
    
    // get owa_enable_session_timeout_url
    if (AM_SUCCESS == status) {
        function_name = "am_properties_get";
        parameter = AM_WEB_OWA_ENABLED_SESSION_TIMEOUT_URL;
        status = am_properties_get_with_default(this->properties, parameter,
                NULL, &this->owa_enable_session_timeout_url);
    }
    
    // get proxy's override_host_port
    if (AM_SUCCESS == status) {
        parameter = AM_PROXY_OVERRIDE_HOST_PORT_PROPERTY;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->override_host_port));
    }

    // get IBM Lotus domino's check_name_database
    if (AM_SUCCESS == status) {
        parameter = AM_DOMINO_CHECK_NAME_DB_PROPERTY;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->check_name_database));
    }

    // get IBM Lotus domino's ltpa_enable
    if (AM_SUCCESS == status) {
        parameter = AM_DOMINO_LTPA_TOKEN_ENABLE_PROPERTY;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->ltpa_enable));
    }

    // get IBM Lotus domino's ltpa_config_name
    if (AM_SUCCESS == status) {
        parameter = AM_DOMINO_LTPA_CONFIG_NAME_PROPERTY;
        status = am_properties_get_with_default(this->properties, parameter,
                     LTPA_DEFAULT_CONFIG_NAME, &(this->ltpa_config_name));
    }

    // get IBM Lotus domino's ltpa_org_name
    if (AM_SUCCESS == status) {
        parameter = AM_DOMINO_LTPA_ORG_NAME_PROPERTY;
        status = am_properties_get_with_default(this->properties, parameter,
                     LTPA_DEFAULT_ORG_NAME, &(this->ltpa_org_name));
    }

    // get IBM Lotus domino's ltpa_cookie_name
    if (AM_SUCCESS == status) {
        parameter = AM_DOMINO_LTPA_TOKEN_NAME_PROPERTY;
        status = am_properties_get_with_default(this->properties, parameter,
                     LTPA_DEFAULT_TOKEN_NAME, &(this->ltpa_cookie_name));
    }


    // get local audit log file rotate flag
    if (AM_SUCCESS == status) {
        parameter = AM_AUDIT_LOCAL_LOG_ROTATE_PROPERTY;
        status = am_properties_get_boolean_with_default(
                this->properties, parameter, AM_FALSE,
                &(this->localAuditLogFileRotate));
    }
    // get local audit log file size
    if (AM_SUCCESS == status) {
        parameter = AM_AUDIT_LOCAL_LOG_FILE_SIZE_PROPERTY;
        status = am_properties_get_positive_number(this->properties, parameter,
                LOCAL_AUDIT_FILE_DEFAULT_SIZE, &this->localAuditLogFileSize);
    }
    // get audit log disposition
    if (AM_SUCCESS == status) {
        parameter = AM_AUDIT_DISPOSITION_PROPERTY;
        status = am_properties_get_with_default(this->properties, parameter,
                AUDIT_DISPOSITION_REMOTE, &this->auditLogDisposition);
    }

    if (AM_SUCCESS == status) {
        // set whether doing remote logging ok or not
        if (((strcasecmp(this->authLogType_param, LOG_TYPE_ALLOW) == 0) ||
                (strcasecmp(this->authLogType_param, LOG_TYPE_DENY) == 0) ||
                (strcasecmp(this->authLogType_param, LOG_TYPE_BOTH) == 0)) &&
                ((strcasecmp(this->auditLogDisposition, AUDIT_DISPOSITION_REMOTE) == 0) ||
                (strcasecmp(this->auditLogDisposition, AUDIT_DISPOSITION_ALL) == 0))) {
            this->doRemoteLog = AM_TRUE;
        }
    }

    // get client ip header 
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_CLIENT_IP_HEADER_PROPERTY;
        status = am_properties_get_with_default(this->properties, parameter,
                     NULL, &this->clientIPHeader);
    }

    // get client hostname header 
    if (AM_SUCCESS == status) {
        parameter = AM_WEB_CLIENT_HOSTNAME_HEADER_PROPERTY;
        status = am_properties_get_with_default(this->properties, parameter,
                     NULL, &this->clientHostnameHeader);
    }

    if (AM_SUCCESS == status && this->notification_enable
            && this->notification_url != NULL
            && strlen(this->notification_url) > 0) {
        am_web_log_debug("%s: normalizing notification URL: %s", thisfunc, this->notification_url);
        this->nurl_local_alloc = 0;
        try {
            std::string notURL_str;
            const char* normURL = NULL;
            smi::URL url(this->notification_url);
            url.getURLString(notURL_str);
            normURL = notURL_str.c_str();
            if (normURL == NULL || *normURL == '\0') {
                status = AM_FAILURE;
            } else {
                this->notification_url = strdup(normURL);
                if (!this->notification_url) {
                    status = AM_NO_MEMORY;
                } else this->nurl_local_alloc = 1;
            }
        } catch (InternalException& exc) {
            am_web_log_error("%s: InternalException encountered while normalizing notification URL, status %s", thisfunc,
                    exc.getMessage(), am_status_to_name(exc.getStatusCode()));
            status = AM_FAILURE;
        } catch (...) {
            am_web_log_error("%s: Unknown exception encountered while normalizing notification URL", thisfunc);
            status = AM_FAILURE;
        }
        if (status != AM_SUCCESS) {
            /*on error allow agent to continue with notifications disabled*/
            status = AM_SUCCESS;
            this->notification_enable = AM_FALSE;
            if (this->nurl_local_alloc == 1 && this->notification_url) {
                free((void *) this->notification_url);
                this->notification_url = NULL;
            }
            am_web_log_error("%s: error normalizing notification URL, notifications are disabled", thisfunc);
        }
        am_web_log_debug("%s: notification URL normalization result: %s, enabled: %s", thisfunc,
                (this->notification_url != NULL ? this->notification_url : "empty"),
                (this->notification_enable ? "yes" : "no"));
    }


    /* Get attribute multi value separator property */
    if (AM_SUCCESS == status) {
        parameter = AM_POLICY_ATTRS_MULTI_VALUE_SEPARATOR;
        status = am_properties_get_with_default(this->properties,
                parameter, 
                ATTR_MULTI_VALUE_SEPARATOR,
                &this->attrMultiValueSeparator);
    }

    if (AM_SUCCESS == status) {

        status = am_properties_set_map(this->properties, 
                              AM_POLICY_PROFILE_ATTRS_MAP,
                              "|", ",");
        status = am_properties_set_map(this->properties, 
                              AM_POLICY_SESSION_ATTRS_MAP,
                              "|", ",");
        status = am_properties_set_map(this->properties, 
                              AM_POLICY_RESPONSE_ATTRS_MAP,
                              "|", ",");

        status = am_properties_get_with_default(this->properties,
                                               AM_POLICY_PROFILE_ATTRS_MODE,
                                               AM_POLICY_SET_ATTRS_AS_NONE,
                                               &this->profileMode);
        if (status == AM_SUCCESS &&
                this->profileMode != NULL && this->profileMode[0] != '\0') {
            if (!strcasecmp(this->profileMode, AM_POLICY_SET_ATTRS_AS_COOKIE)){
                this->setUserProfileAttrsMode = SET_ATTRS_AS_COOKIE;
            }
            else if (!strcasecmp(this->profileMode,
                                 AM_POLICY_SET_ATTRS_AS_HEADER)) {
                this->setUserProfileAttrsMode = SET_ATTRS_AS_HEADER;
            }
            else {
                // anything other than COOKIE or HEADER will be NONE
                this->setUserProfileAttrsMode = SET_ATTRS_NONE;
            }
        }
        else {
            this->setUserProfileAttrsMode = SET_ATTRS_NONE;
        }

        status = am_properties_get_with_default(this->properties,
                                               AM_POLICY_SESSION_ATTRS_MODE,
                                               AM_POLICY_SET_ATTRS_AS_NONE,
                                               &this->sessionMode);
        if (status == AM_SUCCESS &&
                this->sessionMode != NULL && this->sessionMode[0] != '\0') {
            if (!strcasecmp(this->sessionMode, AM_POLICY_SET_ATTRS_AS_COOKIE)) {
                this->setUserSessionAttrsMode = SET_ATTRS_AS_COOKIE;
            }
            else if (!strcasecmp(this->sessionMode,
                                 AM_POLICY_SET_ATTRS_AS_HEADER)) {
                this->setUserSessionAttrsMode = SET_ATTRS_AS_HEADER;
            }
            else {
                // anything other than COOKIE or HEADER will be NONE
                this->setUserSessionAttrsMode = SET_ATTRS_NONE;
            }
        }
        else {
            this->setUserSessionAttrsMode = SET_ATTRS_NONE;
        }

        status = am_properties_get_with_default(this->properties,
                                                 AM_POLICY_RESPONSE_ATTRS_MODE,
                                                 AM_POLICY_SET_ATTRS_AS_NONE,
                                                 &this->responseMode);
        if (status == AM_SUCCESS &&
                this->responseMode != NULL && this->responseMode[0] != '\0') {
         if (!strcasecmp(this->responseMode, AM_POLICY_SET_ATTRS_AS_COOKIE)) {
                this->setUserResponseAttrsMode = SET_ATTRS_AS_COOKIE;
            }
            else if (!strcasecmp(this->responseMode,
                                 AM_POLICY_SET_ATTRS_AS_HEADER)) {
                this->setUserResponseAttrsMode = SET_ATTRS_AS_HEADER;
            }
            else {
                // anything other than COOKIE or HEADER will be NONE
                this->setUserResponseAttrsMode = SET_ATTRS_NONE;
            }
        }
        else {
            this->setUserResponseAttrsMode = SET_ATTRS_NONE;
        }

        try {
            const Properties& propertiesRef =
                *reinterpret_cast<Properties *>(this->properties);

            if (this->setUserProfileAttrsMode == SET_ATTRS_AS_COOKIE || this->setUserProfileAttrsMode == SET_ATTRS_AS_HEADER) {
                Properties attributeMap;
                const std::string &headerAttrs =
                      propertiesRef.get(AM_POLICY_PROFILE_ATTRS_MAP, "");
                attributeMap.parsePropertyKeyValue(headerAttrs, ',','|');
                am_web_log_max_debug("Profile Attributes count=%u", attributeMap.size());
                Properties::iterator iter;
                for(iter = attributeMap.begin();
                      iter != attributeMap.end(); iter++) {
                    std::string attr = (*iter).second;
                    am_web_log_max_debug("Profile Attribute=%s", attr.c_str());
                    this->attrList.push_back(attr);
                }
            }

            if (this->setUserSessionAttrsMode == SET_ATTRS_AS_COOKIE || this->setUserSessionAttrsMode == SET_ATTRS_AS_HEADER) {
                  // Repeat the same for the session attribute map
                  Properties sessionAttributeMap;
                  const std::string &sessionAttrs =
                      propertiesRef.get(AM_POLICY_SESSION_ATTRS_MAP, "");
                  sessionAttributeMap.parsePropertyKeyValue(sessionAttrs,
                                                            ',','|');
                  am_web_log_max_debug("Session Attributes count=%u",
                           sessionAttributeMap.size());
                  Properties::iterator iter_sessionAttr;
                  for(iter_sessionAttr = sessionAttributeMap.begin();
                          iter_sessionAttr != sessionAttributeMap.end();
                           iter_sessionAttr++) {
                      std::string attr = (*iter_sessionAttr).second;
                               am_web_log_max_debug(
                               "Session Attribute=%s", attr.c_str());
                      this->attrList.push_back(attr);
                  }
              }
             if (this->setUserResponseAttrsMode == SET_ATTRS_AS_COOKIE || this->setUserResponseAttrsMode == SET_ATTRS_AS_HEADER) {
                  // Repeat the same for the response attribute map
                  Properties responseAttributeMap;
                  const std::string &responseAttrs =
                      propertiesRef.get(AM_POLICY_RESPONSE_ATTRS_MAP, "");
                  responseAttributeMap.parsePropertyKeyValue(responseAttrs,
                                                            ',','|');
                  am_web_log_max_debug("Response Attributes count=%u",
                           responseAttributeMap.size());
                  Properties::iterator iter_responseAttr;
                  for(iter_responseAttr = responseAttributeMap.begin();
                      iter_responseAttr != responseAttributeMap.end();
                      iter_responseAttr++) {
                      std::string attr = (*iter_responseAttr).second;
                      am_web_log_max_debug("Response Attribute=%s", attr.c_str());
                      this->attrList.push_back(attr);
                  }
             }
         } catch (InternalException& exi) {
              status = exi.getStatusCode();
         } catch (std::bad_alloc& exb) {
              am_web_log_error("%s: Bad alloc error encountered while "
                               "parsing LDAP attributes as cookie: %s",
                               thisfunc, exb.what());
                status = AM_NO_MEMORY;
         } catch (std::exception& exs) {
                am_web_log_error("%s: Exception encountered while "
                               "parsing LDAP attributes as cookie: %s",
                               thisfunc, exs.what());
                status = AM_FAILURE;
         } catch (...) {
                am_web_log_error("%s: Unknown exception encountered while "
                                 "parsing LDAP attributes as cookie.",
                                 thisfunc);
                status = AM_FAILURE;
        }
    }

    if (AM_SUCCESS == status) {
        status = am_properties_get_with_default(
                    this->properties,
                    AM_POLICY_PROFILE_ATTRS_COOKIE_PFX,
                    COOKIE_ATTRIBUTE_PREFIX,
                    &this->attrCookiePrefix);
        am_web_log_debug("%s: Using cookie prefix %s.",
                         thisfunc, attrCookiePrefix);
    }

    if (AM_SUCCESS == status) {
        status = am_properties_get_with_default(
                    this->properties,
                    AM_POLICY_PROFILE_ATTRS_COOKIE_MAX_AGE,
                    COOKIE_ATTRIBUTE_MAX_AGE,
                    &this->attrCookieMaxAge);
        am_web_log_debug("%s: Using cookie max-age %s.",
                         thisfunc, attrCookieMaxAge);
    }

    if (AM_SUCCESS == status) {
        parameter = AM_WEB_ENCODE_COOKIE_SPECIAL_CHARS;
        status = am_properties_get_boolean_with_default(this->properties,
                parameter,
                AM_FALSE,
                &this->encodeCookieSpecialChars);
    }

    if (AM_SUCCESS == status) {
        Properties *props = 
            reinterpret_cast<Properties *>(this->properties);
        if (this->postdatapreserve_enable == AM_TRUE) {
            try {
                this->postcache_handle = new PostCache(*props);
            }
            catch(InternalException& exi) {
                status = exi.getStatusCode();
            }
            catch(std::exception& exs) {
                am_web_log_error("%s: exception encountered while "
                                 "initializing post cache handle: %s.",
                                 thisfunc, exs.what());
                status = AM_FAILURE;
            }
            catch(...) {
                am_web_log_error("%s: Unknown exception encountered "
                                 "while initializing post cache handle.",
                                 thisfunc);
                status = AM_FAILURE;
            }
        }
    }

    populate_am_resource_traits(rsrcTraits);

    // initialize the FQDN handler
    if (AM_SUCCESS == status) {
        try {
              status = load_fqdn_handler(rsrcTraits.ignore_case?true:false);
        }
        catch(std::bad_alloc& exb) {
            status = AM_NO_MEMORY;
        }
        catch(std::exception& exs) {
            am_web_log_error("%s: exception encountered while "
                         "loading fqdn handler: %s",
                         thisfunc, exs.what());
            status = AM_FAILURE;
        }
        catch(...) {
            am_web_log_error("%s: Unknown exception encountered "
                         "while loading fqdn handler.",
                         thisfunc);
            status = AM_FAILURE;
        }
    }


    if (AM_SUCCESS == status) {
        this->lock = new Mutex();
        if (!this->lock) {
            status = AM_NO_MEMORY;
        }
    }


    if (AM_SUCCESS != status) {
        cleanup_properties();
        am_web_log_error("AgentConfiguration::populateAgentProperties():"
                " initialization error: %s(%s) failed, error = %s "
                      "(%d): exiting...", function_name, parameter,
                      am_status_to_string(status), status);
    }


    
    return status;
}

void 
AgentConfiguration::populate_am_resource_traits(am_resource_traits_t &rsrcTraits) {
    rsrcTraits.cmp_func_ptr = &am_policy_compare_urls;
    rsrcTraits.has_patterns = &am_policy_resource_has_patterns;
    rsrcTraits.get_resource_root = &am_policy_get_url_resource_root;
    rsrcTraits.separator = '/';
    rsrcTraits.ignore_case =
        (this->url_comparison_ignore_case) ? B_TRUE : B_FALSE;
    rsrcTraits.canonicalize = &am_policy_resource_canonicalize;
    rsrcTraits.str_free = &free;
    return;
}

/* Throws std::exception's from fqdn_handler functions */
am_status_t
AgentConfiguration::load_fqdn_handler(bool ignore_case)
{
    am_status_t result = AM_FAILURE;

    try {
        const Properties *props = reinterpret_cast<Properties *>(
                                                this->properties);
        this->fqdn_handler = new FqdnHandler(*props,
                                                  ignore_case,
                                                  this->log_module);
        result = AM_SUCCESS;
     } catch (...) {
        am_web_log_error("load_fqdn_handler() failed with exception");
     }

     return result;
}

am_status_t
AgentConfiguration::unload_fqdn_handler()
{
    am_status_t result = AM_FAILURE;

    try {
        if (this->fqdn_handler != NULL) {
            delete this->fqdn_handler;
        }
        result = AM_SUCCESS;
    } catch (...) {
        am_web_log_error("unload_fqdn_handler() failed with exception");
    }

    return result;
}

void AgentConfiguration::cleanup_properties() {
    if (this->properties != AM_PROPERTIES_NULL) {
        am_properties_destroy(this->properties);
        this->properties = AM_PROPERTIES_NULL;
    }

    Utils::cleanup_url_info_list(&this->not_enforced_list);
    this->instance_name = NULL;
    this->cookie_name = NULL;
    this->cookie_name_len = 0;
    Utils::cleanup_url_info_list(&this->cdsso_server_url_list);
    this->access_denied_url = NULL;
    this->logout_redirect_url = NULL;

    delete this->lock;
    this->lock = NULL;

    delete this->not_enforce_IPAddr;
    this->not_enforce_IPAddr = NULL;

    Utils::cleanup_url_info_list(&this->login_url_list);
    this->unauthenticated_user = NULL;

    this->user_id_param = NULL;
    this->authLogType_param = NULL;
    this->fqdn_default = NULL;
    this->fqdn_default_len = 0;

    delete this->cookie_domain_list;
    this->cookie_domain_list = NULL;

    Utils::cleanup_cookie_info_list(&this->cookie_list);
    this->cookie_reset_default_domain = NULL;

    if (this->agent_server_url.url_len > 0) {
        if (this->agent_server_url.url != NULL) {
            free((void*) this->agent_server_url.url);
            this->agent_server_url.url = NULL;
        }
        if (this->agent_server_url.host != NULL) {
            free((void*) this->agent_server_url.host);
            this->agent_server_url.host = NULL;
        }
    }

    if (this->nurl_local_alloc == 1 && this->notification_url) {
        free((void *) this->notification_url);
        this->notification_url = NULL;
    }

    this->iis6_replaypasswd_key = NULL;
    this->owa_enable_session_timeout_url = NULL;

    this->clientIPHeader = NULL;
    this->clientHostnameHeader = NULL;

    if (this->postdatapreserve_enable) {
        delete this->postcache_handle;
        this->postcache_handle = NULL;
    }

    this->notenforcedIPmode = NULL;
    this->dummyPostPrefixUri = NULL;
    this->password_encr_key = NULL;
    this->alogout_regex = NULL;
    this->invalid_url_regex = NULL;

    this->cond_login_url.clear();

    try {
        unload_fqdn_handler();
    }    catch (std::exception& exs) {
        am_web_log_error("Exception caught while unloading "
                "fqdn handler: %s", exs.what());
    }    catch (...) {
        am_web_log_error("Unknown exception caught while unloading "
                "fqdn handler.");
    }
}

