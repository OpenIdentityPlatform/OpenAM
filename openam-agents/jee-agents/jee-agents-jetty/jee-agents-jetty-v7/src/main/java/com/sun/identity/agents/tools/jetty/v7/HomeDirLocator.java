/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: HomeDirLocator.java,v 1.1 2009/01/21 18:43:54 kanduls Exp $
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS.
 */

package com.sun.identity.agents.tools.jetty.v7;

import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;

import java.io.File;


/**
 *
 * Home directory locator for Jetty agent
 *
 *
 */
public class HomeDirLocator implements IServerHomeDirLocator, IConstants, IConfigKeys {
    
    /**
     * Computes the jetty home directory from the config directory
     * @param state State object where are the 
     * @return Home directory.
     * @throws InstallException
     */
    public String getServerDirectory(IStateAccess state) throws InstallException {

        String jettyHomeDir = null;
        String jettyConfigDir = null;
        Debug.log("HomeDirLocator: Finding home directory from config dir.");
        if (state != null) {
            jettyConfigDir = (String) state.get(STR_KEY_JETTY_SERVER_CONFIG_DIR);
            if ((jettyConfigDir != null) && (jettyConfigDir.length() > 0)) {
                jettyHomeDir = (new File(jettyConfigDir)).getParent();
            }
        }
        if (!FileUtils.isDirValid(jettyHomeDir)) {
            Debug.log("HomeDirLocator: The Jetty home directory " + jettyHomeDir + " specified is invalid:");
            throw new InstallException(LocalizedMessage.get(JETTY_ERR_IN_VALID_HOME_DIR, STR_JETTY_GROUP));
        } 
        Debug.log("HomeDirLocator : Jetty server home " + "directory = " + jettyHomeDir);

        return jettyHomeDir;
    }
}

