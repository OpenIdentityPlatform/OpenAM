/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: IConstants.java,v 1.2 2008/11/28 12:36:22 saueree Exp $
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */
package com.sun.identity.agents.tools.tomcat.v6;

public interface IConstants {
    /** Field STR_TOMCAT_GROUP * */
    public static String STR_TOMCAT_GROUP = "tomcatv6Tools";

    /** Field STR_FORWARD_SLASH * */
    public static final String STR_FORWARD_SLASH = "/";
    public static final String FILE_SEP = STR_FORWARD_SLASH;
    public static final String STR_AGENT_REALM_CLASS_NAME =
    	"com.sun.identity.agents.tomcat.v6.AmTomcatRealm";
    public static final String FILTER_NAME = "Agent";
    public static final String FILTER_DISPLAY_NAME = FILTER_NAME;
    public static final String FILTER_DESCRIPTION =
    	"SJS Access Manager Tomcat Policy Agent Filter";
    public static final String FILTER_CLASS =
    	"com.sun.identity.agents.filter.AmAgentFilter";
    public static final String FILTER_URL_PATTERN = "/*";
    public static final String ELEMENT_SERVLET = "servlet";
    public static final String ELEMENT_SERVLET_MAPPING = "servlet-mapping";
    public static final String ELEMENT_MIME_MAPPING = "mime-mapping";
    public static final String ELEMENT_WELCOME_FILE_LIST = "welcome-file-list";
    public static final String ELEMENT_SEC_CONSTRAINT = "security-constraint";
    public static final String ELEMENT_SEC_ROLE = "security-role";
    public static final String ELEMENT_ENV_ENTRY = "env-entry";
    public static final String ELEMENT_EJB_REF = "ejb-ref";
    public static final String ELEMENT_EJB_LOCAL_REF = "ejb-local-ref";
    public static final String ELEMENT_AUTH_METHOD = "auth-method";
    public static final String ELEMENT_FILTER = "filter";
    public static final String ELEMENT_FILTER_MAPPING = "filter-mapping";
    public static final String ELEMENT_FILTER_NAME = "filter-name";
    public static final String ELEMENT_DISPLAY_NAME = "display-name";
    public static final String ELEMENT_DESCRIPTION = "description";
    public static final String ELEMENT_FILTER_CLASS = "filter-class";
    public static final String ELEMENT_URL_PATTERN = "url-pattern";
    public static final String ELEMENT_DISPATCHER = "dispatcher";
    public static final String ELEMENT_DISPATCHER_VALUE_REQUEST = "REQUEST";
    public static final String ELEMENT_DISPATCHER_VALUE_INCLUDE = "INCLUDE";
    public static final String ELEMENT_DISPATCHER_VALUE_FORWARD = "FORWARD";
    public static final String ELEMENT_DISPATCHER_VALUE_ERROR = "ERROR";
    public static final String ELEMENT_SERVICE = "Service";
    public static final String ELEMENT_ENGINE = "Engine";
    public static final String ELEMENT_LISTENER = "Listener";
    public static final String ELEMENT_REALM = "Realm";
    public static final String ELEMENT_SERVER = "Server";
    public static final String ELEMENT_HOST = "Host";
    public static final String ATTR_NAME_CLASSNAME = "className";
    public static final String ATTR_VALUE_SERVER_LIFECYCLE_CLASSNAME =
    	"org.apache.catalina.mbeans.ServerLifecycleListener";
    public static final String ATTR_VALUE_LIFECYCLE_CLASSNAME =
            "org.forgerock.agents.tomcat.v6.TomcatLifeCycleListener";
    public static final String ATTR_NAME_DESCRIPTORS = "descriptors";
    public static final String ATTR_VAL_AGENT_MBEAN_DESCRIPTOR =
    	"/com/sun/identity/agents/tomcat/v6/mbean-descriptor.xml";
    public static final String ATTR_NAME_AGENT_REALM_CLASSNAME = "className";
    public static final String ATTR_VAL_AGENT_REALM_CLASSNAME =
    	"com.sun.identity.agents.tomcat.v6.AmTomcatRealm";
    public static final String ATTR_NAME_AGENT_REALM_DEBUG = "debug";
    public static final String ATTR_VAL_AGENT_REALM_DEBUG = "99";
    public static final String STR_APP_BASE = "appBase";
    public static final String ELEMENT_REALM_NAME = "realm-name";
    public static final String VALUE_REALM_NAME = "Default";
    public static final String ELEMENT_LOGIN_CONFIG = "login-config";
    public static final String ELEMENT_FORM_LOGIN_CONFIG = "form-login-config";
    public static final String ELEMENT_FORM_LOGIN_PAGE = "form-login-page";
    public static final String VALUE_FORM_LOGIN_PAGE = "/AMLogin.html";
    public static final String ELEMENT_FORM_ERROR_PAGE = "form-error-page";
    public static final String VALUE_FORM_ERROR_PAGE = "/AMError.html";
    public static final String VALUE_AUTH_METHOD_FORM = "FORM";
    public static final String KEY_SERVER_LIFECYCLE_ELEM_EXISTS =
    	"SERVER_LIFECYCLE_ELEM_EXISTS";
    public static final String KEY_OLD_REALM = "OLD_REALM";
    public static final String KEY_OLD_REALM_ELEMENT = "OLD REALM ELEMENT";
    public static final String KEY_OLD_LOGIN_CONFIG = "OLD_LOGIN_CONFIG";
    public static final String KEY_OLD_LOGIN_CONFIG_ELEMENT =
    	"OLD_LOGIN_CONFIG_ELEMENT";
    public static final String STR_BIN_DIRECTORY = "bin";
    public static final String STR_CLASSPATH_LINE = "SET CLASSPATH";
    public static final String SET_CLASSPATH_SCRIPT_NAME =
    	"setAgentClassPath.sh";
    public static final String STR_JAVA_COMMAND = "java";
    public static final String STR_CLASSPATH = "-classpath";
    public static final String STR_CATALINA_JAR = "catalina.jar";
    public static final String STR_CATALINA_JAR_PATH = "CATALINA_JAR_PATH";
    public static final String STR_TOMCAT_VERSION_CLASS =
    	"org.apache.catalina.util.ServerInfo";
    public static final String STR_TOMCAT_VERSION =
    	"org.apache.catalina.util.ServerInfo";
    public static final String TOMCAT_VER_60 = "Tomcat 6.0.x";
    public static final String TOMCAT_VMWARE_7 = "VMware vFabric tc Runtime "
            + "2.7.1.RELEASE/7.0.29.A.RELEASE";
    public static final String STR_WEBAPP_DIR = "webapps";
    public static final String STR_AGENT_APP_WAR_FILE = "agentapp.war";
    public static final String INSTANCE_CONFIG_DIR_NAME = "config";
    public static final String STR_TOMCAT_SERVER_DIR = "server";
    public static final String STR_TOMCAT_SERVER_LIB = "lib";
    public static final String STR_TOMCAT_COMMON_LIB = "lib";
    public static final String STR_SET_CLASSPATH_FILE_WINDOWS =
    	"setclasspath.bat";
    public static final String STR_SET_CLASSPATH_FILE_UNIX = "setclasspath.sh";
    public static final String STR_SET_ENV_FILE_WINDOWS = "setenv.bat";
    public static final String STR_SET_ENV_FILE_UNIX = "setenv.sh";
    public static final String STR_SET_AGENT_CLASSPATH_FILE_WINDOWS =
    	"setAgentclasspath.bat";
    public static final String STR_SET_AGENT_CLASSPATH_FILE_UNIX =
    	"setAgentclasspath.sh";
    public static final String STR_APACHE_TOMCAT = "Apache Tomcat/";
    public static final String cmdTemplateSuffix = ".cmd.template";
    public static final String shTemplateSuffix = ".sh.template";
    public static final String AGENT_ENV_CMD_TEMPLATE = "setAgentEnv"
        + cmdTemplateSuffix;
    public static final String AGENT_ENV_SH_TEMPLATE = "setAgentEnv"
        + shTemplateSuffix;
    public static final String STR_OS_NAME_PROPERTY = "os.name";
    public static final String STR_WINDOWS = "windows";
    public static final String PRE_AGENT_BACKUP_SUFFIX = "-preAgent";
    public static final String AGENT_BACKUP_SUFFIX = "-Agent";
    public static final String STR_VALUE_TRUE = "true";
    public static final String STR_VALUE_FALSE = "false";
    public static final String DIR_NAME_WEBINF = "WEB-INF";
    public static final String FILE_NAME_WEB_XML = "web.xml";
    public static final String STR_CONFIG_DIR_LOOKUP_KEY =
    	"CONFIG_DIR_LOOKUP_KEY";
    public static final String STR_WEB_APPS_PATH = "WEB_APPS_PATH";
    public static final String STR_WEB_APP_CONTEXT_PATH_LIST =
    	"STR_WEB_APP_CONTEXT_PATH_LIST";
    public static final String AMAGENT_PROPS_FILE_PATH =
    	"config/OpenSSOAgentConfiguration.properties";
    public static final String KEY_FILTER_MODE_MAP_PROP =
    	"com.sun.identity.agents.config.filter.mode";
    public static final String KEY_LOGOUT_URI_MAP_PROP =
    	"com.sun.identity.agents.config.logout.uri";
    public static final String KEY_LOGIN_FORMLIST_PROP =
    	"com.sun.identity.agents.config.login.form";
    public static final String KEY_LOGIN_ERRORLIST_PROP =
    	"com.sun.identity.agents.config.login.error.uri";
    public static final String STRING_KEY_J2EE_MODE = "J2EE_POLICY";
    public static final String STR_ADMIN_APP_CONTEXT = "admin";
    public static final String STR_ADMIN_APP_CONTEXT_LOGOUT_URI =
    	"/admin/logOut.do";
    public static final String CLASSPATH_PREFIX_UNIX =
    	"CLASSPATH=\"$CLASSPATH\":";
    public static final String CLASSPATH_PREFIX_WINDOWS =
    	"set AGENT_CLASSPATH=%AGENT_CLASSPATH%;";
    public static final String TOMCAT_VERSION_SPECIFIC_SERVER_LIB_CLASSPATHS =
        "TOMCAT_VERSION_SPECIFIC_SERVER_LIB_CLASSPATHS";
    public static final String TOMCAT_VERSION_SPECIFIC_COMMON_LIB_CLASSPATHS =
        "TOMCAT_VERSION_SPECIFIC_COMMON_LIB_CLASSPATHS";
}
