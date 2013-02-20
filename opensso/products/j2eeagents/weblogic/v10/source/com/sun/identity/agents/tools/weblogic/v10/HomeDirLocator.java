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
 * $Id: HomeDirLocator.java,v 1.2 2008/06/25 05:52:21 qcheng Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

import java.io.File;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;

/**
 * Home directory locator for WL10 agent
 */
public class HomeDirLocator implements IServerHomeDirLocator,
        IConfigKeys {
    
    public String getServerDirectory(IStateAccess state)
    throws InstallException {
        String serverHomeDir = null;
        // Home dir
        String startupScr = (String)state.get(STR_KEY_WL_STARTUP_SCRIPT);
        String wlHomeDir = (new File(startupScr)).getParent();
        if ((wlHomeDir != null) && (wlHomeDir.length() > 0)) {
            serverHomeDir = wlHomeDir;
        }
        
        if(!FileUtils.isDirValid(serverHomeDir)) {
            Debug.log("HomeDirLocator: The server home " +
                    serverHomeDir + ", directory specified is invalid:");
            throw new InstallException(
                    LocalizedMessage.get(LOC_WL_ERR_IN_VALID_HOME_DIR,
                    STR_WL_GROUP));
        }
        
        Debug.log("HomeDirLocator : WebLogic server home " +
                "directory = " + serverHomeDir);
        return serverHomeDir;
    }
    
    /*
     * Localized constants
     */
    public static String LOC_WL_ERR_IN_VALID_HOME_DIR =
            "WL_ERR_IN_VALID_HOME_DIR";
    
}
