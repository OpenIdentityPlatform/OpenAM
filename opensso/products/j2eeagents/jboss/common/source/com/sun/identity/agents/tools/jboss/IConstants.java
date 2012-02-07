/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IConstants.java,v 1.2 2009/01/25 05:57:48 naghaon Exp $
 *
 */

package com.sun.identity.agents.tools.jboss;

public interface IConstants {
    
    /**  JBoss string **/
    public static String STR_JBOSS = "JBoss";

    /** Field STR_JB_GROUP **/
    public static String STR_JB_GROUP = "jbossv40Tools";
    
    /** Field STR_FORWARD_SLASH **/
    public static final String STR_FORWARD_SLASH = "/";
    
    public static final String STR_LOGIN_MODULE_CLASS_NAME =
            "com.sun.identity.agents.jboss.v40.AmJBossLoginModule";
    
    public static final String STR_CONFIG_DIR_WINDOWS =
            "C:\\jboss-4.2.3\\server\\default\\conf";
    public static final String STR_CONFIG_DIR_SOLARIS =
            "/opt/jboss-4.2.3/server/default/conf";
    public static final String STR_CONFIG_DIR_LINUX	=
            "/opt/jboss-4.2.3/server/default/conf";
    
    public static final String STR_SERVICE_XML_FILE = "jboss-service.xml";
    public static final String STR_LOGIN_CONF_XML_FILE = "login-config.xml";
    public static final String STR_SERVER_POLICY_FILE = "server.policy";
    
    public static final String STR_AM_LOGIN_CONF_XML_FILE
            = "am-login-config.xml";
    public static final String STR_AM_LOGIN_CONF_SERVICE_XML_FILE
            = "am-login-config-service.xml";
    
    public static final String STR_SET_AGENT_CLASSPATH_FILE =
            "setAgentClasspath";
    public static final String STR_SET_AGENT_CLASSPATH_FILE_WIN_EXTN = ".bat";
    public static final String STR_SET_AGENT_CLASSPATH_FILE_UNIX_EXTN = ".sh";
    public static final String STR_SET_AGENT_CLASSPATH_FILE_WIN_SERVER_PARAM =
            "%CONFIG";
    public static final String STR_SET_AGENT_CLASSPATH_FILE_UNIX_SERVER_PARAM =
            "$CONFIG";
    public static final String STR_BIN_DIRECTORY = "bin";
    public static final String STR_RUN_FILE_WINDOWS = "run.bat";
    public static final String STR_RUN_FILE_UNIX = "run.sh";
    public static final String STR_CLASSPATH_LINE = "JBOSS_CLASSPATH";
    
    public static final String STR_CMD_TEMPLATE_SUFFIX = ".cmd.template";
    public static final String STR_SH_TEMPLATE_SUFFIX = ".sh.template";
    public static final String STR_AGENT_ENV_CMD_TEMPLATE = "setAgentEnv"
            + STR_CMD_TEMPLATE_SUFFIX;
    public static final String STR_AGENT_ENV_SH_TEMPLATE = "setAgentEnv"
            + STR_SH_TEMPLATE_SUFFIX;
    public static final String STR_INSTANCE_CONFIG_DIR_NAME = "config";
    public static final String STR_INSTANCE_LOGS_DIR_NAME = "logs";
    public static final String STR_JB_SERVER = "server";
    public static final String STR_JB_SERVER_BIN = "bin";
    public static final String STR_JB_INST_CONF = "conf";
    public static final String STR_JB_INST_DEPLOY = "deploy";
    public static final String STR_AGENT_APP_WAR_FILE = "agentapp.war";
    public static final String STR_SERVER_ELEMENT = "default";
    public static final String STR_ARCHIVES_ATTR = "archives";
    public static final String STR_AGENT_CLASSPATH= "classpath";
    
    public static final String STR_SERVER_CLASSPATH_SEP =
            "${path.separator}";
    
    public static final String STR_AGENT_JAR = "agent.jar";
    public static final String STR_AM_CLIENT_SDK_JAR = "openssoclientsdk.jar";
    
    public static final String STR_PRE_AGENT_DEFAULT_CLASSPATH =
            "PRE_AGENT_DEFAULT_CLASSPATH";
    public static final String STR_JAVA_COMMAND = "java";
    public static final String STR_CLASSPATH = "-classpath";
    public static final String STR_JB_RUN_JAR = "run.jar";
    public static final String STR_JB_MAIN_CLASS =
        "org.jboss.Main";
    public static final String STR_JB_VERSION_ARGUMENT =
        "--version";
    public static final int JB_VER_40_MAJOR = 4;
    public static final int JB_VER_40_MINOR = 0;
    public static final int JB_VER_42_MAJOR = 4;
    public static final int JB_VER_42_MINOR = 3;
    public static final int JB_VER_50_MAJOR = 5;
    public static final int JB_VER_50_MINOR = 0;
    public static final int JB_VER_32_MAJOR = 3;
    public static final int JB_VER_32_MINOR = 2;
    public static final int JB_VER_32_SECOND_MINOR = 5;
}


