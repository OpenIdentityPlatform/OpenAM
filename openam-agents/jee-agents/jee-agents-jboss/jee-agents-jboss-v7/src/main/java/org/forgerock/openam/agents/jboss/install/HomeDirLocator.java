/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.agents.jboss.install;

import com.sun.identity.install.tools.configurator.IServerHomeDirLocator;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import static org.forgerock.openam.agents.jboss.install.InstallerConstants.*;

/**
 * Based on the user's input returns the JBoss deployment's home directory.
 *
 * @author Peter Major
 */
public class HomeDirLocator implements IServerHomeDirLocator {

    /**
     * Returns the home directory based on the user input or throws an exception if the provided data was invalid.
     *
     * @param state Agent installer state.
     * @return The path to the JBoss home directory.
     * @throws InstallException If the provided home directory is not valid.
     */
    @Override
    public String getServerDirectory(IStateAccess state) throws InstallException {
        String homeDir = (String) state.get(HOME_DIR);

        if (!FileUtils.isDirValid(homeDir)) {
            Debug.log("HomeDirLocator: The JBoss home directory \"" + homeDir + "\" is invalid:");
            throw new InstallException(
                    LocalizedMessage.get(LOC_HOME_DIR_INVALID, BUNDLE_NAME, new String[]{homeDir}));
        }

        Debug.log("HomeDirLocator: JBoss home directory = " + homeDir);
        return homeDir;
    }
}
