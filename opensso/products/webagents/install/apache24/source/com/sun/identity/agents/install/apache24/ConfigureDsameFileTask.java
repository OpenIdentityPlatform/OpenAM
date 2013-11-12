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
 * $Id: ConfigureDsameFileTask.java,v 1.4 2009/12/09 23:01:00 krishna_indigo Exp $
 *
 */
/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.install.apache24;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.OSChecker;
import com.sun.identity.install.tools.util.ReplaceTokens;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

/**
 * Copies dsame.conf to agent instance's config directory. 
 * dsame.conf's tags gets replaced with agent instance specific
 * information.
 */
public class ConfigureDsameFileTask 
    implements ITask, IConstants, InstallConstants, IConfigKeys {
    
    private static final String LOC_TSK_MSG_CONFIGURE_DSAME_FILE_EXECUTE
            = "TSK_MSG_CONFIGURE_DSAME_FILE_EXECUTE";
    
    private static final String LOC_TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK
            = "TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK";
    
    /**
     * 
     * @param name 
     * @param stateAccess 
     * @param properties 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return true or false status of the task
     */
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        
        return copyDsameFile(stateAccess, properties);
    }
    
    /**
     * 
     * @param stateAccess 
     * @param properties 
     * @return 
     */
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        Object[] args = {getDsameConfFile(stateAccess)};
        return LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_DSAME_FILE_EXECUTE,
                STR_APC24_GROUP, args);
    }
    
    /**
     * 
     * @param stateAccess 
     * @param properties 
     * @return 
     */
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        Object[] args = {getDsameConfFile(stateAccess)};
        return LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_DSAME_FILE_ROLLBACK,
                STR_APC24_GROUP, args);
    }
    
    /**
     * 
     * @param name 
     * @param stateAccess 
     * @param properties 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return 
     */
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        return true;
    }

    /**
     * Tag swaps etc/dsame.template and copies to agent instance's config
     * directory as dsame.conf. This file gets included in Apache's httpd.conf
     * file.
     * @param stateAccess 
     * @param properties 
     * @return 
     */
    protected boolean copyDsameFile(IStateAccess stateAccess,
            Map properties) {

        boolean status = false;
        String srcFile = null;
        String destFile = null;
        try {
            // gets dsame.template file from etc directory and creates 
            // a temporary file called tokens.txt
            srcFile = createTagSwapFile(stateAccess);
            destFile = getDsameConfFile(stateAccess);
            
            // Tags get swapped with agent library and agent instance's config
            // file.
            Map tokens = new HashMap();
            if (OSChecker.isWindows()) {
                tokens.put("MODULE", ConfigUtil.getBinDirPath());
            } else {
                tokens.put("MODULE", ConfigUtil.getLibPath());
            }
            tokens.put("AGENTBOOTSTRAP",
	   	stateAccess.get(STR_CONFIG_FILE_PATH_TAG));
            tokens.put("AGENTCONFIG", 
                stateAccess.get(STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG));

            tokens.put("EAPI", STR_APC24_TWO_LIB_SUFFIX);
            if (OSChecker.isWindows()) {
                tokens.put("EXTN", STR_APC24_WIN_LIB_EXTN);
	     } else if (OSChecker.isHPUX()) {
		  tokens.put("EXTN", STR_APC24_HPUX_LIB_EXTN);
            } else {
                tokens.put("EXTN", STR_APC24_UNIX_LIB_EXTN);
            }
            
            // Tag swapping is done here into a temporary file, tokens.txt
            ReplaceTokens filter = new ReplaceTokens(srcFile, destFile, tokens);
            filter.tagSwapAndCopyFile();
            // deletes the temporary file.
            status = new File(srcFile).delete();
            Debug.log("ConfigureDsameFileTask.copyDsameFile() - "
                + srcFile + " : " + destFile);
        } catch (Exception e) {
            Debug.log("ConfigureDsameFileTask." +
                "copyDsameFile() - Failed to copy  " + srcFile
                + " : " + destFile, e);

        }
        return status;
    }

    private String createTagSwapFile(IStateAccess stateAccess)
        throws Exception {

        Map tokens = new HashMap();
        String srcFile;

        srcFile = ConfigUtil.getEtcDirPath() + FILE_SEP
                    + STR_DSAME_FILE_TEMPLATE;

        String destFile = ConfigUtil.getEtcDirPath() + FILE_SEP
                + "tokens.txt";

        ReplaceTokens filter = new ReplaceTokens(srcFile, destFile, tokens);
        filter.tagSwapAndCopyFile();
        Debug.log("ConfigureDsameFileTask." +
            "createTagSwapFile() - after filter tag swap " + srcFile
            + " : " + destFile);

        return destFile;
    }

    private String getDsameConfFile(IStateAccess stateAccess) {
        // apache22_agent home directory
        String homeDir = ConfigUtil.getHomePath();
        // agent instance name
        String instanceName = stateAccess.getInstanceName();
        // agent instance's dsame.conf
        StringBuffer confSb = new StringBuffer();
        confSb.append(homeDir).append(FILE_SEP);
        confSb.append(instanceName).append(FILE_SEP);
        confSb.append(STR_INSTANCE_CONFIG_DIR_NAME);
        confSb.append(FILE_SEP);
        confSb.append(STR_DSAME_CONF_FILE);

        return confSb.toString();
    }
}
