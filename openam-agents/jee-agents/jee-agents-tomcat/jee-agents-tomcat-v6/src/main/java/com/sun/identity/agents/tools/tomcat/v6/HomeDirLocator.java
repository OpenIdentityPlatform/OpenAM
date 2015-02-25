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
 * $Id: HomeDirLocator.java,v 1.2 2008/11/28 12:36:22 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;

import java.io.File;


/**
 *
 * Home directory locator for Tomcat agent
 *
 *
 */
public class HomeDirLocator implements IServerHomeDirLocator, IConstants,
    IConfigKeys {
    /*
     * Localized constants
     */
    public static String LOC_TOMCAT_ERR_IN_VALID_CONF_DIR =
    	"TOMCAT_ERR_IN_VALID_HOME_DIR";

    public String getServerDirectory(IStateAccess state)
        throws InstallException {
        String serverHomeDir = null;

        if (state != null) {
            String tomcatHomeDir = (String) state.get(
                    STR_KEY_TOMCAT_SERVER_CONFIG_DIR);

            if ((tomcatHomeDir != null) && (tomcatHomeDir.length() > 0)) {
                serverHomeDir = (new File(tomcatHomeDir)).getParent();
            }
        }

        if (!FileUtils.isDirValid(serverHomeDir)) {
            Debug.log(
                "HomeDirLocator: The catalina config directory "
                + serverHomeDir + " specified is invalid:");
            throw new InstallException(
                LocalizedMessage.get(
                    LOC_TOMCAT_ERR_IN_VALID_CONF_DIR,
                    STR_TOMCAT_GROUP));
        }

        Debug.log(
            "HomeDirLocator : Tomcat server config " + "directory = "
            + serverHomeDir);

        return serverHomeDir;
    }
}
