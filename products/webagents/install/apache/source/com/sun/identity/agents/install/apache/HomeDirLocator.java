/* The contents of this file are subject to the terms
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
 * $Id: HomeDirLocator.java,v 1.1 2006/10/06 18:27:33 subbae Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.agents.install.apache;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;

import java.io.File;

/**
 * Determines Apache server home directory based on user's specified 
 * Apache server instance's config directory at the time of agent setup. 
 */
public class HomeDirLocator implements IServerHomeDirLocator,
        IConfigKeys, IConstants {
    
    private static String LOC_APC_ERR_IN_VALID_HOME_DIR =
            "APC_ERR_IN_VALID_HOME_DIR";
    
    /**
     * Returns Apache's home directory.
     * @param state 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return 
     */
    public String getServerDirectory(IStateAccess state)
        throws InstallException {
        String serverHomeDir = null;
        // Apache home dir
        String apcHomeDir = (String)state.get(STR_KEY_APC_HOME_DIR);
        if ((apcHomeDir != null) && (apcHomeDir.length() > 0)) {
            serverHomeDir = apcHomeDir;
        }
        
        if(!FileUtils.isDirValid(serverHomeDir)) {
            Debug.log("HomeDirLocator: The Apache Home " +
                    serverHomeDir + ", directory is invalid:");
            throw new InstallException(
                    LocalizedMessage.get(LOC_APC_ERR_IN_VALID_HOME_DIR,
                    STR_APC_GROUP));
        }
        
        Debug.log("HomeDirLocator : Apache Home " +
                "directory = " + serverHomeDir);
        return serverHomeDir;
    }
}
