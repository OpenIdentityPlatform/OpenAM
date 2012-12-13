/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HomeDirLocator.java,v 1.1 2009/01/12 09:25:26 ranajitgh Exp $
 *
 */

package com.sun.identity.agents.install.proxy40;

import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;

import java.io.File;

/**
 * Determines Proxy server home directory based on user's specified 
 * Proxy server instance's config directory at the time of agent setup. 
 */
public class HomeDirLocator implements IServerHomeDirLocator,
        IConfigKeys, IConstants, InstallConstants {
    
    private static String LOC_PROXY4_ERR_IN_VALID_HOME_DIR =
            "PROXY4_ERR_IN_VALID_HOME_DIR";
    
    public String getServerDirectory(IStateAccess state)
        throws InstallException {
        String proxy40HomeDir = null;
        String serverHomeDir = null;
        // Home dir
        String proxy40ConfigDir = (String)state.get(STR_KEY_PROXY4_INST_CONF_DIR);
        if ((proxy40ConfigDir != null) && (proxy40ConfigDir.length() > 0)) {
	   String proxy40ObjFile = proxy40ConfigDir 
                                 + FILE_SEP 
                                 + STR_PROXY40_OBJ_FILE;

            if (FileUtils.isFileValid(proxy40ObjFile)) {
                   proxy40HomeDir = (new File (proxy40ObjFile))
                                            .getParentFile()
                                            .getParentFile()
                                            .getParent();
            }
        }
        if ((proxy40HomeDir != null) && (proxy40HomeDir.length() > 0)) {
            serverHomeDir = proxy40HomeDir;
        }
        
        if(!FileUtils.isDirValid(serverHomeDir)) {
            Debug.log("HomeDirLocator: The PROXY4 Home " +
                    serverHomeDir + ", directory is invalid:");
            throw new InstallException(
                    LocalizedMessage.get(LOC_PROXY4_ERR_IN_VALID_HOME_DIR,
                    STR_PROXY40_GROUP));
        }
        
        Debug.log("HomeDirLocator : SPS Home " +
                "directory = " + serverHomeDir);
        return serverHomeDir;
    }
}
