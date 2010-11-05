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
 * $Id: IConfigKeys.java,v 1.2 2008/06/25 05:52:02 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.appserver;

/**
 *
 * Interface to isolate the app's server specific config keys
 * 
 */
public interface IConfigKeys {
    
    /*
     * Key for the interaction to lookup config dir
     */
    public static String STR_KEY_AS_INST_CONFIG_DIR = "CONFIG_DIR";
    
    /*
     * Key to store AS home dir
     */
    public static String STR_KEY_AS_HOME_DIR = "AS_HOME_DIR";
    
    /*
     * Key to domain.xml file
     */
    public static String STR_KEY_AS_DOMAIN_XML_FILE  = "AS_DOMAIN_XML_FILE";
    
    /*
     * Key to login.conf file
     */
    public static String STR_KEY_AS_LOGIN_CONF_FILE = "AS_LOGIN_CONF_FILE";
    
    /*
     * Key to server.policy file
     */
    public static String STR_KEY_AS_SERVER_POLICY_FILE = 
        "AS_SERVER_POLICY_FILE";
    
    /*
     * Key to domain administration server is remote field 
     */
    public static final String STR_DAS_HOST_IS_REMOTE_KEY = 
        "DAS_HOST_IS_REMOTE";

    /*
     * Key to instance on DAS host field
     */
     public static final String STR_REMOTE_INSTANCE_LOCAL_DAS_KEY = "REMOTE_INSTANCE_LOCAL_DAS";

    /*
     * Key to agent instance name
     */
     public static final String STR_AGENT_INSTANCE_NAME_KEY = "AGENT_INSTANCE_NAME";
    
     /*
     * Key to new agent instance name on the remote server instance.
     */
     public static final String STR_MIGRATE_AGENT_INSTANCE_NAME_KEY = 
             "MIGRATE_AGENT_INSTANCE_NAME";
     
    /*
     * Key to agent install directory on a remote instance 
     */
     public static final String STR_REMOTE_AGENT_INSTALL_DIR_KEY = "REMOTE_AGENT_INSTALL_DIR";
    
     /*
     * Key to new agent install directory on a remote instance 
     */
     public static final String STR_MIGRATE_REMOTE_AGENT_INSTALL_DIR_KEY = 
             "MIGRATE_REMOTE_AGENT_INSTALL_DIR";
    
    /*
     * Key to Agent and AM colocation field 
     */
    public static final String STR_AM_COEXIST_KEY = "AM_COEXIST";
}
