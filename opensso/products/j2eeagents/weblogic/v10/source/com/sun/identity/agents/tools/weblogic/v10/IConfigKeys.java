/**
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
 * $Id: IConfigKeys.java,v 1.9 2008/08/04 20:01:25 huacui Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

/**
 * Interface to isolate the app's server specific config keys
 *
 */
public interface IConfigKeys {
    
    /**
     * Key for the interaction to lookup config dir
     */
    public static String STR_KEY_WL_STARTUP_SCRIPT = "STARTUP_SCRIPT";
    
    /**
     * Key to agent instance name lookup key
     */
    public static final String STR_KEY_SERVER_NAME = "SERVER_NAME";
    
    /**
     * Key to store WL home dir lookup key
     */
    public static String STR_KEY_STARTUP_SCRIPT_DIR = "STARTUP_SCRIPT_DIR";
    
    /**
     * Key to store Weblogic home directory lookup key
     */
    public static String STR_KEY_WEBLOGIC_HOME_DIR = "WEBLOGIC_HOME_DIR";
    
    /**
     * Key to store mbeantypes directory derived from Weblogic home
     * directory lookup key
     */
    public static String STR_KEY_WEBLOGIC_MBEANS_DIR = "WEBLOGIC_MBEANS_DIR";
    
    /**
     * Key to store config.xml file location
     */
    public static String STR_KEY_CONFIG_XML = "CONFIG_XML";
    
    /**
     * Key to store AM coexist lookup key
     */
    public static final String STR_AM_COEXIST_KEY = "AM_COEXIST";
    
    /**
     * Key to store AM agent environment variable file path
     */
    public static final String STR_KEY_AGENT_ENV_FILE_PATH =
            "AGENT_ENV_FILE_PATH";
    
    /** Field STR_AS_GROUP **/
    public static String STR_WL_GROUP = "wl10Tools";
    
    /** Field STR_MBEANS_JAR_FILE **/
    public static String STR_MBEANS_JAR_FILE = "amauthprovider.jar";
    
    /** key for config directory */
    public static final String STR_CONFIG = "config";
    
    /** key for weblogic config.xml */
    public static final String STR_CONFIG_XML = "config.xml";
    
    /** key for WL domain **/
    public static final String STR_KEY_WL_DOMAIN = "WL_DOMAIN";
    
    /** Key for portal context URI **/
    public static final String STR_PORTAL_CONTEXT_URI = "PORTAL_CONTEXT_URI";
    
    /** Field server leaf **/
    public static final String STR_SERVER_LEAF = "server";
    
    /** Field lib leaf **/
    public static final String STR_LIB_LEAF = "lib";
    
    /** Field weblogic.jar **/
    public static final String STR_WEBLOGIC_JAR = "weblogic.jar";
    
    /** Field java command **/
    public static final String STR_JAVA_COMMAND = "java";
    
    /** Field STR_TRUE_VALUE **/
    public static final String STR_TRUE_VALUE = "true";
    
    /** Field server domain type **/
    public static final String STR_SERVER_DOMAIN_TYPE = "server";
    
    /** Field portal domain type **/
    public static final String STR_PORTAL_DOMAIN_TYPE = "portal";
    
    /** Field weblogic.version **/
    public static final String STR_WEBLOGIC_VERSION = "weblogic.version";
    
    /** Field classpath leaf **/
    public static final String STR_CLASSPATH = "-classpath";
    
    /** Field STR_J2EE_WL_PORTAL_AUTH_HANDLER **/
    public static final String STR_J2EE_WL_PORTAL_HANDLER
        = "com.sun.identity.agents.weblogic.v10.AmWLPortalJ2EEAuthHandler";
    
    /** Field STR_J2EE_WL_PORTAL_LOGOUT_HANDLER **/
    public static final String STR_J2EE_WL_PORTAL_LOGOUT_HANDLER
        = "com.sun.identity.agents.weblogic.v10.AmWLPortalLocalLogoutHandler";
    
    /** Field STR_J2EE_WL_PORTAL_VER_HANDLER **/
    public static final String STR_J2EE_WL_PORTAL_VER_HANDLER
        = "com.sun.identity.agents.weblogic.v10.AmWLPortalVerificationHandler";
    
    /** Field OpenSSOAgentBootstrap file name **/
    public static final String STR_AMAGENT_FILE = "OpenSSOAgentBootstrap.properties";
    
    /** Field auth handler **/
    public static final String STR_AUTH_HANDLER =
            "com.sun.identity.agents.config.auth.handler";
    
    /** Field logout handler **/
    public static final String STR_LOGOUT_HANDLER =
            "com.sun.identity.agents.config.logout.application.handler";
    
    /** Field verification handler **/
    public static final String STR_VERIFICATION_HANDLER =
            "com.sun.identity.agents.config.verification.handler";
    
    /** Enviroment config file for weblogic startup script (UNIX) */
    public static final String AGENT_ENV_SH_TEMPLATE =
            "setAgentEnv.sh.template";
    
    /** Enviroment config file for weblogic startup script (Windows) */
    public static final String AGENT_ENV_CMD_TEMPLATE =
            "setAgentEnv.cmd.template";

    /** Prefix of Enviroment config file for weblogic startup script */
    public static final String AGENT_ENV = "setAgentEnv";
    
    /** Agent jar file **/
    public static final String STR_AGENT_JAR = "agent.jar";
    
    /** openssoclientsdk jar file */
    public static final String STR_FAM_CLIENT_SDK_JAR = "openssoclientsdk.jar";
    
    /** JVM options */
    public static final String STR_LOG_CONFIG_FILE_OPTION_PREFIX =
            "-Djava.util.logging.config.file=";
    
    /** option for log compability */
    public static final String STR_LOG_COMPATMODE_OPTION =
            "-DLOG_COMPATMODE=Off";
    
    /** Field STR_FORWARD_SLASH **/
    public static final String STR_FORWARD_SLASH = "/";
    
    /** key to lookup OS type */
    public static final String STR_OS_NAME_PROPERTY = "os.name";
    
    /** OS type of Windows */
    public static final String STR_WINDOWS = "windows";
    
}
