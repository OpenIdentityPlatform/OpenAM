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
 * $Id: HomeDirLocator.java,v 1.2 2008/11/21 22:21:43 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;


/**
 * Home directory locator for WAS agent
 *
 */
public class HomeDirLocator implements IServerHomeDirLocator,
        IConfigKeys, IConstants {
    
    public String getServerDirectory(IStateAccess state)
    throws InstallException {
        
        String wasHomeDir = (String)state.get(STR_KEY_WAS_HOME_DIR);
        if(!FileUtils.isDirValid(wasHomeDir)) {
            Debug.log("HomeDirLocator: The server home " +
                    wasHomeDir + ", directory specified is invalid:");
            throw new InstallException(
                    LocalizedMessage.get(LOC_WAS_ERR_IN_VALID_HOME_DIR,
                    STR_WAS_GROUP));
        } else {
            String libExtDir = wasHomeDir + STR_FILE_SEP + "lib" +
                    STR_FILE_SEP + "ext";
            // Will validate later
            state.put(STR_KEY_WAS_LIB_EXT, libExtDir);
        }
        
        Debug.log("HomeDirLocator : WebSphere home " + "directory = " +
                wasHomeDir);
        return wasHomeDir;
    }
    
    /*
     * Localized constants
     */
    public static String LOC_WAS_ERR_IN_VALID_HOME_DIR =
            "WAS_ERR_IN_VALID_HOME_DIR";
    
}
