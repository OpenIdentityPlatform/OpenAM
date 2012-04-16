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
 * $Id: HomeDirLocator.java,v 1.1 2008/12/11 14:36:06 naghaon Exp $
 *
 */


package com.sun.identity.agents.tools.jboss;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;

public class HomeDirLocator implements IServerHomeDirLocator,
        IConfigKeys, IConstants {
    
    /*
     * Localized constants
     */
    public static String LOC_JB_ERR_IN_VALID_HOME_DIR =
            "JB_ERR_IN_VALID_HOME_DIR";
    
    public String getServerDirectory(IStateAccess state)
    throws InstallException {
        String serverHomeDir = null;
        // Home dir
      
        String jbHomeDir = (String)state.get(STR_KEY_JB_HOME_DIR);
	    if ((jbHomeDir != null) && (jbHomeDir.length() > 0)) {
            serverHomeDir = jbHomeDir;
        }
                    
        if(!FileUtils.isDirValid(serverHomeDir)) {
            Debug.log("HomeDirLocator: The JBoss Home " +
                    serverHomeDir + ", directory is invalid:");
            throw new InstallException(
                    LocalizedMessage.get(LOC_JB_ERR_IN_VALID_HOME_DIR,
                    STR_JB_GROUP));
        }
        
        Debug.log("HomeDirLocator : JBoss Home " +
                "directory = " + serverHomeDir);
        return serverHomeDir;
    }
    
}
