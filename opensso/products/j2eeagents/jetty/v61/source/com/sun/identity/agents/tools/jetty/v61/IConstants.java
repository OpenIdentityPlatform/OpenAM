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
 * $Id: IConstants.java,v 1.1 2009/01/21 18:43:56 kanduls Exp $
 */

package com.sun.identity.agents.tools.jetty.v61;

public interface IConstants {
    
    //Localized resource file name
    public static String STR_JETTY_GROUP = "jettyv61Tools";
    public static final String STR_CONFIG_DIR_WINDOWS = 
        "c:\\jetty-6.1.14\\etc";
    public static final String STR_CONFIG_DIR_SOLARIS = 
        "/opt/jetty-6.1.14/etc";
    public static final String STR_CONFIG_DIR_LINUX =
        "/opt/jetty-6.1.14/etc";
    
    public static final String JETTY_ERR_IN_VALID_HOME_DIR =
        "JETTY_ERR_IN_VALID_HOME_DIR";
    public static final String VA_MSG_JETTY_VAL_HOME =
        "VA_MSG_JETTY_VAL_HOME";
    public static final String VA_WRN_JETTY_IN_VAL_HOME =
        "VA_WRN_JETTY_IN_VAL_HOME";
    public static final String VA_MSG_JETTY_VAL_CONFIG_DIR = 
        "VA_MSG_JETTY_VAL_CONFIG_DIR";
    public static final String VA_WRN_JETTY_IN_VAL_CONFIG_DIR =
        "VA_WRN_JETTY_IN_VAL_CONFIG_DIR";
    public static final String VA_WRN_JETTY_IN_VAL_VERSION =
        "VA_WRN_JETTY_IN_VAL_VERSION";
    public static final String VA_MSG_JETTY_VAL_VERSION =
        "VA_MSG_JETTY_VAL_VERSION";
    public static final String VN_WRN_JAVA_HOME_NOT_FOUND =
        "VN_WRN_JAVA_HOME_NOT_FOUND";
    public static final String TSK_MSG_UPDATE_SET_CLASSPATH_EXECUTE =
        "TSK_MSG_UPDATE_SET_CLASSPATH_EXECUTE";
    public static final String TSK_MSG_UPDATE_SET_CLASSPATH_ROLLBACK =
        "TSK_MSG_UPDATE_SET_CLASSPATH_ROLLBACK";
    public static final String TSK_MSG_CONFIGURE_AGENT_APP_EXECUTE = 
        "TSK_MSG_CONFIGURE_AGENT_APP_EXECUTE";
    public static final String TSK_MSG_CONFIGURE_AGENT_APP_ROLLBACK = 
        "TSK_MSG_CONFIGURE_AGENT_APP_ROLLBACK";
    public static final String TSK_MSG_CONFIGURE_JETTY_LOGIN_CONF_FILE_EXECUTE =
        "TSK_MSG_CONFIGURE_JETTY_LOGIN_CONF_FILE_EXECUTE";
    public static final String 
        TSK_MSG_CONFIGURE_JETTY_LOGIN_CONF_FILE_ROLLBACK = 
        "TSK_MSG_CONFIGURE_JETTY_LOGIN_CONF_FILE_ROLLBACK";
    public static final String 
        TSK_MSG_UNCONFIGURE_JETTY_LOGIN_CONF_FILE_EXECUTE = 
        "TSK_MSG_UNCONFIGURE_JETTY_LOGIN_CONF_FILE_EXECUTE";
    public static final String TSK_MSG_UNCONFIGURE_AGENT_CLASSPATH_EXECUTE =
        "TSK_MSG_UNCONFIGURE_AGENT_CLASSPATH_EXECUTE";
    public static final String TSK_MSG_UNCONFIGURE_AGENT_APP_EXECUTE =
        "TSK_MSG_UNCONFIGURE_AGENT_APP_EXECUTE";
    
    public static final String STR_FORWARD_SLASH = "/";
    public static final String STR_SERVER_XML = "jetty.xml";
    public static final String JETTY_CLASSPATH_CONF_FILE = "start.conf";
    public static final String JETTY_START_JAR = "start.jar";
    public static final String JETTY_MAIN_CLASS = "org.mortbay.start.Main";
    public static final String JETTY_VERSION_ARGUMENT = "--version";
    public static final String STR_ETC_DIRECTORY = "etc";
    public static final String JETTY6_VERSION = "6.1";
    public static final String JETTY_WEB_APP_DIR = "webapps";
    public static final String STR_AGENT_APP_WAR_FILE = "agentapp.war";
    public static final String LOGIN_CONF_FILE = "jetty-login-conf.xml";
    public static final String AGENT_JAR = "agent.jar";
    public static final String OPENSSO_CLIENT_SDK_JAR = "openssoclientsdk.jar";
    public static final String STR_INSTANCE_CONFIG_DIR_NAME = "config";
    public static final int BUFF_SIZE = 2048;
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
}

