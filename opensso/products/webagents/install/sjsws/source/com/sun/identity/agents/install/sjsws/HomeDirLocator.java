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
 * $Id: HomeDirLocator.java,v 1.5 2008/06/25 05:54:40 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.sjsws;

import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;

import java.io.File;

/**
 * Determines SWS server home directory based on user's specified 
 * SWS server instance's config directory at the time of agent setup. 
 */
public class HomeDirLocator implements IServerHomeDirLocator,
        IConfigKeys, IConstants, InstallConstants {
    
    private static String LOC_SWS_ERR_IN_VALID_HOME_DIR =
            "SWS_ERR_IN_VALID_HOME_DIR";
    
    public String getServerDirectory(IStateAccess state)
        throws InstallException {
        String sjswsHomeDir = null;
        String serverHomeDir = null;
        // Home dir
        String sjswsConfigDir = (String)state.get(STR_KEY_SWS_INST_CONF_DIR);
        if ((sjswsConfigDir != null) && (sjswsConfigDir.length() > 0)) {
	   String sjswsObjFile = sjswsConfigDir 
                                 + FILE_SEP 
                                 + STR_SWS_OBJ_FILE;
	   String sjswsMagnusFile = sjswsConfigDir 
                                 + FILE_SEP 
                                 + STR_SWS_MAGNUS_FILE;

            if (FileUtils.isFileValid(sjswsObjFile) &&
               FileUtils.isFileValid(sjswsMagnusFile)) {
                   sjswsHomeDir = (new File (sjswsObjFile))
                                            .getParentFile()
                                            .getParentFile()
                                            .getParent();
            }
        }
        if ((sjswsHomeDir != null) && (sjswsHomeDir.length() > 0)) {
            serverHomeDir = sjswsHomeDir;
        }
        
        if(!FileUtils.isDirValid(serverHomeDir)) {
            Debug.log("HomeDirLocator: The SWS Home " +
                    serverHomeDir + ", directory is invalid:");
            throw new InstallException(
                    LocalizedMessage.get(LOC_SWS_ERR_IN_VALID_HOME_DIR,
                    STR_SWS_GROUP));
        }
        
        Debug.log("HomeDirLocator : SWS Home " +
                "directory = " + serverHomeDir);
        return serverHomeDir;
    }
}
