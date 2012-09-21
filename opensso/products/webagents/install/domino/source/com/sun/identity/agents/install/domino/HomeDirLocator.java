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
 * $Id: HomeDirLocator.java,v 1.2 2009/12/01 22:06:46 leiming Exp $
 *
 */
package com.sun.identity.agents.install.domino;

import java.io.File;

import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;


/**
 * Determines IBM Lotus Domino server home directory based on user's specified
 * Domino server instance's data directory at the time of agent setup.
 */
public class HomeDirLocator implements IServerHomeDirLocator,
        IConfigKeys, IConstants, InstallConstants {

    private static String LOC_DOMINO_ERR_IN_VALID_HOME_DIR =
            "DOMINO_ERR_IN_VALID_HOME_DIR";

    public String getServerDirectory(IStateAccess state)
            throws InstallException {

        String dominoHomeDir = null;
        // Home dir
        String dominoConfigDir = (String) state.get(
                STR_KEY_DOMINO_INST_CONF_DIR);
        if ((dominoConfigDir != null) && (dominoConfigDir.length() > 0)) {
            String dominoNotesFile = dominoConfigDir + FILE_SEP +
                    STR_DOMINO_NOTES_INI_FILE;

            if (FileUtils.isFileValid(dominoNotesFile)) {
                File configDirFile = new File(dominoConfigDir);
                dominoHomeDir = configDirFile.getParent();
            }
        }

        if (!FileUtils.isDirValid(dominoHomeDir)) {
            Debug.log("HomeDirLocator: The Domino Home " +
                    dominoHomeDir + ", directory is invalid:");
            throw new InstallException(
                    LocalizedMessage.get(LOC_DOMINO_ERR_IN_VALID_HOME_DIR,
                    STR_DOMINO_GROUP));
        }

        Debug.log("HomeDirLocator : Domino Home " +
                "directory = " + dominoHomeDir);
        return dominoHomeDir;
    }
}

