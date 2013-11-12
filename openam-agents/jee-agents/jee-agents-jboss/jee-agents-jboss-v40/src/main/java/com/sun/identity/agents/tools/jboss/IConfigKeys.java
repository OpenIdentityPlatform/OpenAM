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
 * $Id: IConfigKeys.java,v 1.1 2008/12/11 14:36:06 naghaon Exp $
 *
 */

package com.sun.identity.agents.tools.jboss;

/**
 * @author sevani
 *
 * Interface to isolate the JBoss's server specific config keys
 *
 */
public interface IConfigKeys {
    
    /*
     * Key for the interaction to lookup config dir
     */
    public static String STR_KEY_JB_INST_CONF_DIR = "CONFIG_DIR";
    
    /*
     * Key for the interaction to lookup config dir
     */
    public static String STR_KEY_JB_INST_DEPLOY_DIR = "DEPLOY_DIR";
    
    /*
     * Key to store JBoss home dir
     */
    public static String STR_KEY_JB_HOME_DIR = "JB_HOME_DIR";
    
    /*
     * Key to store JBoss server instance name
     */
    public static String STR_KEY_JB_RUN_SCRIPT = "JB_RUN_SCRIPT";
    
    /*
     * Key to store JBoss server instance name
     */
    public static String STR_KEY_JB_INST_NAME = "INSTANCE_NAME";
    
    /*
     * Key to jboss-service.xml file
     */
    public static String STR_KEY_JB_SERVICE_XML_FILE  = "JB_SERVICE_XML_FILE";
    
    /*
     * Key to login-config.conf file
     */
    public static String STR_KEY_JB_LOGIN_CONF_XML_FILE = "JB_LOGIN_CONF_FILE";
    
    /*
     * Key to server.policy file
     */
    public static String STR_KEY_JB_SERVER_POLICY_FILE =
            "SERVER_POLICY_FILE";
    
    /*
     * Key to indicate modification of server.policy file true/false
     */
    public static String STR_KEY_JB_MODIFY_SERVER_POLICY_FILE =
            "MODIFY_SERVER_POLICY_FILE";

    /*
     * Key to agent instance name
     */
    public static final String STR_KEY_AGENT_INSTANCE_NAME =
            "AGENT_INSTANCE_NAME";
    
    public static final String STR_KEY_JB_AGENT_ENV_FILE_PATH =
            "JB_AGENT_ENV_FILE_PATH";

    /*
     * JBOSS_HOME/bin/run.jar 
     */
    public static final String STR_KEY_JB_RUN_JAR_FILE = "JB_RUN_JAR_FILE";
   
}
