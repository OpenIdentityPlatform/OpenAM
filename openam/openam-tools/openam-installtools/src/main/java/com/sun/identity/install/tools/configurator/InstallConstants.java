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
 * $Id: InstallConstants.java,v 1.12 2008/08/04 19:29:27 huacui Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.admin.ICommonToolsConstants;

public interface InstallConstants extends ICommonToolsConstants {
            
    /** Field INT_INSTALL_REGULAR **/
    public static final int INT_OPERATION_TYPE_REGULAR = 0;
    
    /** Field INT_INSTALL_USE_RESPONSE_FILE **/
    public static final int INT_OPERATION_TYPE_USE_RESPONSE = 1;
    
    /** Field INT_INSTALL_SAVE_RESPONSE_FILE **/
    public static final int INT_OPERATION_TYPE_SAVE_RESPONSE = 2;
    
    /** Field STR_INSTALL **/
    public static final String STR_INSTALL = "install";
    
    /** Field STR_UNINSTALL **/
    public static final String STR_UNINSTALL = "uninstall";
    
    /** Field STR_INSTALL **/
    public static final String STR_MIGRATE = "migrate";
    
    /** Field STR_CUSTOM **/
    public static final String STR_CUSTOM = "custom";
    
    /** Field STR_LOG_CONFIG_FILE_PATH **/
    public static final String STR_LOG_CONFIG_FILE_PATH = 
                    "LOG_CONFIG_FILE_PATH";
    
    /** Field STR_LOG_CONFIG_FILENAME **/
    public static final String STR_LOG_CONFIG_FILENAME =
                    "OpenSSOAgentLogConfig.properties";
    
    /** Field STR_VERSION_FILE_NAME **/
    public static final String STR_VERSION_FILE_NAME = "/.version";
    //////////////////////////////////////////////////////////////////////////
    // Constants for component : InstallState
    // Prefix                  : IS
    //////////////////////////////////////////////////////////////////////////
    /* Field STR_IS_GLOBAL_DATA_ID **/
    public static final String STR_IS_GLOBAL_DATA_ID = "GLOBAL";
        
    // LOCALIZED MESSAGE KEYS ////////////////////////////////////////////////
    /** Field LOC_IS_ERR_LOAD_INSTALL_STATE **/
    public static final String LOC_IS_ERR_LOAD_INSTALL_STATE = 
        "IS_ERR_LOAD_INSTALL_STATE";
    
    /** Field LOC_IS_ERR_SAVE_INSTALL_STATE **/
    public static final String LOC_IS_ERR_SAVE_INSTALL_STATE = 
        "IS_ERR_SAVE_INSTALL_STATE";     
            
    
    //////////////////////////////////////////////////////////////////////////
    // Constants for component : Driver
    // Prefix                  : DR
    //////////////////////////////////////////////////////////////////////////
    
    // LOCALIZED MESSAGE KEYS ////////////////////////////////////////////////
    /** Field LOC_DR_ERR_APP_SERVER_HOME_LOCATOR **/    
    public static final String LOC_DR_ERR_APP_SERVER_HOME_LOCATOR =        
        "DR_ERR_APP_SERVER_HOME_LOCATOR";        
          
    /** Field LOC_DR_ERR_PRODUCT_LOCATOR_READ **/
    public static final String LOC_DR_ERR_PRODUCT_LOCATOR_READ = 
        "DR_ERR_PRODUCT_LOCATOR_READ";
    
    /** Field DR_ERR_PRODUCT_LOCATOR_WRITE **/
    public static final String LOC_DR_ERR_PRODUCT_LOCATOR_WRITE = 
        "DR_ERR_PRODUCT_LOCATOR_WRITE";
    
    /** Field DR_ERR_PRODUCT_LOCATOR_BACKUP **/
    public static final String LOC_DR_ERR_PRODUCT_LOCATOR_BACKUP = 
        "DR_ERR_PRODUCT_LOCATOR_BACKUP";
    
    /** Field DR_ERR_PRODUCT_LOCATOR_BACKUP **/
    public static final String LOC_DR_ERR_PRODUCT_ALREADY_MIGRATED = 
        "DR_ERR_PRODUCT_ALREADY_MIGRATED";
    
    /** Field LOC_DR_ERR_INVALID_INSTALL_HOME **/
    public static final String LOC_DR_ERR_INVALID_INSTALL_HOME =
        "DR_ERR_INVALID_INSTALL_HOME";
    
    /** Field LOC_DR_ERR_CORRUPT_PRODUCT_INSTALL **/
    public static final String LOC_DR_ERR_CORRUPT_PRODUCT_INSTALL = 
        "DR_ERR_CORRUPT_PRODUCT_INSTALL";   
        
    /** LOC_DR_MSG_PRODUCT_SUMM_INFO_BEGIN **/
    public static final String LOC_DR_MSG_PRODUCT_SUMM_INFO_BEGIN = 
        "DR_MSG_PRODUCT_SUMM_INFO_BEGIN";
    
    /** LOC_DR_MSG_PRODUCT_SUMM_INFO_BEGIN **/
    public static final String LOC_DR_MSG_PRODUCT_SUMM_INFO_BEGIN_LINE = 
        "DR_MSG_PRODUCT_SUMM_INFO_BEGIN_LINE";
    
    /** LOC_DR_MSG_PRODUCT_SUMM_INFO_END **/
    public static final String LOC_DR_MSG_PRODUCT_SUMM_INFO_END = 
        "DR_MSG_PRODUCT_SUMM_INFO_END";
    
    // SHARED CONSTATNS BETWEEN INFRASTRUCTURE TASKS //////////////////////////
    
    /** Field STR_CONFIG_DIR_PREFIX_TAG **/
    public static final String STR_CONFIG_DIR_PREFIX_TAG = 
        "PRODUCT_INSTANCE_CONFIG_DIR";
    
    /** Field STR_CONFIG_DIR_MIGRATE_PREFIX_TAG **/
    public static final String STR_CONFIG_DIR_PREFIX_MIGRATE_TAG = 
        "PRODUCT_INSTANCE_CONFIG_MIGRATE_DIR";
    
    /** Field STR_INSTANCE_NAME_MIGRATE_TAG **/
    public static final String STR_INSTANCE_NAME_MIGRATE_TAG = 
        "PRODUCT_INSTANCE_NAME_MIGRATE";
    
    /** Field STR_DEBUG_DIR_PREFIX **/
    public static final String STR_DEBUG_DIR_PREFIX_TAG = "DEBUG_LOGS_DIR";
     
    /** Field STR_AUDIT_DIR_PREFIX_TAG **/
    public static final String STR_AUDIT_DIR_PREFIX_TAG = "AUDIT_LOGS_DIR";
    
    /** Field STR_CONFIG_FILE_PATH_TAG **/
    public static final String STR_CONFIG_FILE_PATH_TAG = "CONFIG_FILE_PATH";

    /** Field STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG **/
    public static final String STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG = 
        "AGENT_CONFIG_FILE_PATH";

    /** Field STR_CONFIG_AGENT_MIGRATE_FILE_PATH_TAG **/
    public static final String STR_CONFIG_AGENT_MIGRATE_FILE_PATH_TAG = 
        "AGENT_MIGRATE_FILE_PATH";
    
   /**
    * Key to save new agent instance name on the remote server instance.
    */
    public static final String STR_SAVE_AGENT_INSTANCE_NAME_KEY = 
             "SAVE_AGENT_INSTANCE_NAME";
    
    /**
     * Key to save agent install directory on a remote instance before migration
     */
     public static final String STR_SAVE_REMOTE_AGENT_INSTALL_DIR_KEY = 
             "SAVE_REMOTE_AGENT_INSTALL_DIR";
     
     
    /**
     * Key to flag indicating if agent profile will be created
     */
     public static final String STR_CREATE_AGENT_PROFILE_KEY = 
             "CREATE_AGENT_PROFILE_KEY";
     
     public static final String STR_CREATE_AGENT_PROFILE_NAME =
            "CREATE_AGENT_PROFILE_NAME";
     
     
    // FORMATTING LINE FOR WELCOME AND EXIT MESSAGE, 80 chars
    public static final String STR_BEGIN_END_LINE_MARKER = "*************" +
                "***********************************************************";
}
