/**
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

 * $Id: IFilterConfigurationConstants.java,v 1.15 2009/10/15 23:22:29 leiming Exp $

 *


 */
/**
 * Portions Copyrighted 2010-2012 ForgeRock Inc
 */


package com.sun.identity.agents.filter;



/**

 * The interface for defining agent filter configuration constants

 */

public interface IFilterConfigurationConstants {



    //Configuration keys

    public static final String CONFIG_FILTER_MODE = "filter.mode";



    public static final String CONFIG_ACCESS_DENIED_URI = "access.denied.uri";



    public static final String CONFIG_FORM_LOGIN_LIST = "login.form";



    public static final String CONFIG_REDIRECT_PARAM_NAME = "redirect.param";



    public static final String CONFIG_LOGIN_URL = "login.url";



    public static final String CONFIG_LOGIN_URL_PRIORITIZED =

        "login.url.prioritized";



    public static final String CONFIG_LOGIN_URL_PROBE_ENABLED =

        "login.url.probe.enabled";



    public static final String CONFIG_LOGIN_URL_PROBE_TIMEOUT =

        "login.url.probe.timeout";



    public static final String CONFIG_LOGOUT_URL = "logout.url";



    public static final String CONFIG_LOGOUT_URL_PRIORITIZED =

        "logout.url.prioritized";



    public static final String CONFIG_LOGOUT_URL_PROBE_ENABLED =

        "logout.url.probe.enabled";



    public static final String CONFIG_LOGOUT_URL_PROBE_TIMEOUT =

        "logout.url.probe.timeout";



    public static final String CONFIG_LOGIN_ATTEMPT_LIMIT =

        "login.attempt.limit";



    public static final String CONFIG_LOGIN_COUNTER_COOKIE_NAME =

        "login.counter.name";



    public static final String CONFIG_COOKIE_RESET_ENABLE = "cookie.reset.enable";



    public static final String CONFIG_COOKIE_RESET_LIST = "cookie.reset.name";



    public static final String CONFIG_COOKIE_RESET_DOMAINS = "cookie.reset.domain";



    public static final String CONFIG_COOKIE_RESET_PATHS = "cookie.reset.path";



    public static final String CONFIG_CDSSO_ENABLED = "cdsso.enable";



    public static final String CONFIG_CDSSO_REDIRECT_URI =

        "cdsso.redirect.uri";



    public static final String CONFIG_CDSSO_COOKIE_NAME =

        "cdsso.cookie.name";



    public static final String CONFIG_CDSSO_CDC_SERVLET_URL =

        "cdsso.cdcservlet.url";

    public static final String CONFIG_CONDITIONAL_LOGIN_URL =
            "conditional.login.url";

    public static final String CONFIG_CONDITIONAL_LOGOUT_URL =
            "conditional.logout.url";

    public static final String CONFIG_CDSSO_CLOCK_SKEW =

        "cdsso.clock.skew";

        

    public static final String CONFIG_CDSSO_SECURE_ENABLED =

        "cdsso.secure.enable";        



    public static final String CONFIG_CDSSO_DOMAIN =

        "cdsso.domain";



    public static final String CONFIG_CDSSO_TRUSTED_ID_PROVIDER =

        "cdsso.trusted.id.provider";



    public static final String CONFIG_AGENT_HOST = "agent.host";



    public static final String CONFIG_AGENT_PORT = "agent.port";



    public static final String CONFIG_AGENT_PROTOCOL = "agent.protocol";



    public static final String CONFIG_LOGOUT_INTROSPECT_ENABLE =

        "logout.introspect.enabled";



    public static final String CONFIG_LOGOUT_APPLICATION_HANDLER_MAP =

        "logout.application.handler";



    public static final String CONFIG_LOGOUT_REQUEST_PARAM_MAP =

        "logout.request.param";



    public static final String CONFIG_LOGOUT_URI_MAP =

        "logout.uri";



    public static final String CONFIG_LOGOUT_ENTRY_URI_MAP =

        "logout.entry.uri";



    public static final String CONFIG_FORM_ERROR_LIST = "login.error.uri";



    public static final String CONFIG_FORM_LOGIN_USE_INTERNAL_FLAG =

        "login.use.internal";



    public static final String CONFIG_FORM_LOGIN_CONTENT_FILENAME =

        "login.content.file";



    public static final String CONFIG_FQDN_ENABLE_FLAG =

        "fqdn.check.enable";



    public static final String CONFIG_DEFAULT_FQDN = "fqdn.default";



    public static final String CONFIG_FQDN_MAP = "fqdn.mapping";



    public static final String CONFIG_LEGACY_USER_AGENT_LIST =

        "legacy.user.agent";



    public static final String CONFIG_LEGACY_REDIRECT_URI =

        "legacy.redirect.uri";



    public static final String CONFIG_LEGACY_SUPPORT_FLAG =

        "legacy.support.enable";



    public static final String CONFIG_RESPONSE_HEADER_MAP =

        "response.header";



    public static final String CONFIG_AUTH_HANDLER_MAP =

        "auth.handler";



    public static final String CONFIG_REDIRECT_COUNTER_COOKIE_NAME =

        "redirect.cookie.name";



    public static final String CONFIG_REDIRECT_ATTTEMPT_LIMIT =

        "redirect.attempt.limit";



    public static final String CONFIG_COOKE_RESET_ENABLE_FLAG =

        "cookie.reset.enable";



    public static final String CONFIG_COOKE_RESET_COOKIE_LIST =

        "cookie.reset.name";



    /** Field CONFIG_COOKE_RESET_DOMAIN_MAP **/

    public static final String CONFIG_COOKE_RESET_DOMAIN_MAP =

        "cookie.reset.domain";



    /** Field CONFIG_COOKE_RESET_PATH_MAP **/

    public static final String CONFIG_COOKE_RESET_PATH_MAP =

        "cookie.reset.path";

   /**

     * For new feature to allow session data to not be destroyed when a user

     * authenticates to AM server and new session is created.

     * For RFE issue #763

     */

    public static final String CONFIG_HTTPSESSION_BINDING =

        "httpsession.binding";

    

    public static final boolean DEFAULT_CONFIG_HTTPSESSION_BINDING = true;





    public static final String CONFIG_LOGOUT_HANDLER_MAP = "logout.handler";



    public static final String CONFIG_PORT_CHECK_FILENAME =

        "port.check.file";



    public static final String CONFIG_PORT_CHECK_ENABLE_FLAG =

        "port.check.enable";



    public static final String CONFIG_PORT_CHECK_MAP = "port.check.setting";



    public static final String CONFIG_NOTENFORCED_LIST_CACHE_FLAG =

        "notenforced.uri.cache.enable";



    public static final String CONFIG_INVERT_NOTENFORCED_LIST_FLAG =

        "notenforced.uri.invert";



    public static final String CONFIG_NOTENFORCED_LIST_CACHE_SIZE =

        "notenforced.uri.cache.size";



    public static final String CONFIG_NOTENFORCED_LIST =

        "notenforced.uri";

    public static final String CONFIG_NOTENFORCED_REFRESH_SESSION_IDLETIME =

        "notenforced.refresh.session.idletime";


    public static final boolean
            DEFAULT_CONFIG_NOTENFORCED_REFRESH_SESSION_IDLETIME = false;

    public static final String CONFIG_NOTENFORCED_IP_CACHE_FLAG =

                "notenforced.ip.cache.enable";



    public static final String CONFIG_NOTENFORCED_IP_CACHE_SIZE =

                "notenforced.ip.cache.size";



    public static final String CONFIG_INVERT_NOTENFORCED_IP_FLAG =

                "notenforced.ip.invert";



    public static final String CONFIG_NOTENFORCED_IP_LIST =

                "notenforced.ip";



    public static final String CONFIG_PROFILE_ATTRIBUTE_FETCH_MODE =

        "profile.attribute.fetch.mode";



    public static final String CONFIG_PROFILE_ATTRIBUTE_MAP =

        "profile.attribute.mapping";



    public static final String CONFIG_SESSION_ATTRIBUTE_FETCH_MODE =

        "session.attribute.fetch.mode";



    public static final String CONFIG_SESSION_ATTRIBUTE_MAP =

        "session.attribute.mapping";



    public static final String CONFIG_ATTRIBUTE_DATE_FORMAT =

        "attribute.date.format";



    public static final String CONFIG_ATTRIBUTE_SEPARATOR =

        "attribute.cookie.separator";



    public static final String CONFIG_ATTRIBUTE_ENCODE =

        "attribute.cookie.encode";



    public static final String CONFIG_RESPONSE_ATTRIBUTE_FETCH_MODE =

        "response.attribute.fetch.mode";



    public static final String CONFIG_RESPONSE_ATTRIBUTE_MAP =

        "response.attribute.mapping";



    public static final String CONFIG_WEBSERVICE_ENABLE_FLAG =

        "webservice.enable";



    public static final String CONFIG_WEBSERVICE_END_POINT =

        "webservice.endpoint";



    public static final String CONFIG_WEBSERVICE_PROCESS_GET =

        "webservice.process.get.enable";



    public static final String CONFIG_WEBSERVICE_AUTHENTICATOR_IMPL =

        "webservice.authenticator";


    public static final String CONFIG_WEBSERVICE_RESPONSEPROCESSOR_IMPL =
        "webservice.responseprocessor";


    public static final String CONFIG_WEBSERVICE_INTERNAL_ERROR_FILE =

        "webservice.internalerror.content";



    public static final String CONFIG_WEBSERVICE_AUTH_ERROR_FILE =

        "webservice.autherror.content";



    public static final String CONFIG_AM_SSO_CACHE_ENABLE =

        "amsso.cache.enable";

    
    public static final String CONFIG_IGNORE_PATH_INFO =

        "ignore.path.info";

    public static final String CONFIG_POSTDATA_PRESERVE_ENABLE =
            "postdata.preserve.enable";
    public static final String CONFIG_POSTDATA_PRESERVE_TTL =
            "postdata.preserve.cache.entry.ttl";
    public static final String CONFIG_POSTDATA_PRESERVE_NOENTRY_URL =
            "postdata.preserve.cache.noentry.url";
    public static final String CONFIG_POSTDATA_PRESERVE_STICKYSESSION_MODE =
            "postdata.preserve.stickysession.mode";
    public static final String CONFIG_POSTDATA_PRESERVE_STICKYSESSION_VALUE =
            "postdata.preserve.stickysession.value";



    //Default Values

    public static final boolean DEFAULT_AM_SSO_CACHE_ENABLE = true;


    public static final String DEFAULT_REDIRECT_PARAM_NAME = "goto";



    public static final boolean DEFAULT_LOGIN_URL_PRIORITIZED = true;

    
    public static final boolean DEFAULT_LOGOUT_URL_PRIORITIZED = true;



    public static final String  DEFAULT_LOGIN_COUNTER_COOKIE_NAME =

        "amFilterParam";



    public static final String DEFAULT_USERID_PROP_NAME = "UserToken";



    public static final int DEFAULT_LOGIN_ATTEMPT_LIMIT = 0;



    public static final String DEFAULT_CDSSO_COOKIE_NAME =

        "amFilterCDSSORequest";



    public static final int DEFAULT_CDSSO_CLOCK_SKEW = 0;



    public static final boolean DEFAULT_LOGOUT_INTROSPECT_ENABLE = false;



    public static final boolean DEFAULT_FORM_LOGIN_USE_INTERNAL_FLAG = true;



    public static final String DEFAULT_FORM_LOGIN_CONTENT_FILENAME =

        "FormLoginContent.txt";



    public static final boolean DEFAULT_FQDN_ENABLE_FLAG = true;



    public static final boolean DEFAULT_LEGACY_SUPPORT_FLAG = false;



    public static final String DEFAULT_REDIRECT_COUNTER_COOKIE_NAME =

        "amFilterRDParam";



    public static final int DEFAULT_REDIRECT_ATTEMPT_LIMIT = 0;



    public static final boolean DEFAULT_COOKE_RESET_ENABLE_FLAG = false;



    public static final String DEFAULT_PORT_CHECK_FILENAME =

        "PortCheckContent.txt";



    public static final boolean DEFAULT_PORT_CHECK_ENABLE_FLAG = false;



    public static final boolean DEFAULT_NOTENFORCED_LIST_CACHE_FLAG = true;



    public static final boolean DEFAULT_INVERT_NOTENFORCED_LIST_FLAG = false;



    public static final int DEFAULT_NOTENFORCED_LIST_CACHE_SIZE = 1000;



    public static final boolean DEFAULT_NOTENFORCED_IP_CACHE_FLAG = true;



    public static final int DEFAULT_NOTENFORCED_IP_CACHE_SIZE = 1000;



    public static final boolean DEFAULT_INVERT_NOTENFORCED_IP_FLAG = false;



    public static final String DEFAULT_DATE_FORMAT_STRING =

        "EEE, d MMM yyyy hh:mm:ss z";



    public static final String DEFAULT_ATTRIBUTE_SEPARATOR = "|";



    public static final boolean DEFAULT_ATTRIBUTE_ENCODE = true;



    public static final boolean DEFAULT_WEBSERVICE_ENABLE_FLAG = false;



    public static final String DEFAULT_WEBSERVICE_INTERNAL_ERROR_FILE =

        "WSInternalErrorContent.txt";



    public static final String DEFAULT_WEBSERVICE_AUTH_ERROR_FILE =

        "WSAuthErrorContent.txt";



    public static final boolean DEFAULT_WEBSERVICE_PROCESS_GET = true;


    public static final boolean DEFAULT_IGNORE_PATH_INFO = false;

    public static final boolean DEFAULT_POSTDATA_PRESERVE_ENABLE = false;
    //postdata preservation default to 5 minutes
    public static final long DEFAULT_POSTDATA_PRESERVE_TTL = 5 * 1000 * 60;

    public static final String DEFAULT_POSTDATA_PRESERVE_STICKYSESSION_MODE =
            "URL";


    // Other supporting constants

    public static final String STR_HTTP = "http";

    public static final String STR_HTTPS = "https";

    public static final String STR_MODE_COOKIE = "HTTP_COOKIE";



    public static final int MAX_PORT = 65535;

    /**
     * Used when working in HTTP Session Binding mode to store
     * the value of the User DN in the HTTP Session to be
     * able to test if the user has changed between requests, i.e.
     * has logged out and the next request is from a different user.
     */
    public static final String HTTPSESSION_BINDING_ATTRIBUTE =
        "httpsession.binding.attribute";

}

