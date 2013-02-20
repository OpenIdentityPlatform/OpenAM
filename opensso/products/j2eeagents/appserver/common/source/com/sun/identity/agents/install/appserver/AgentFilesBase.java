/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
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
 */
package com.sun.identity.agents.install.appserver;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import java.io.File;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.forgerock.openam.agents.install.appserver.VersionChecker;

/**
 *
 * @author Peter Major
 */
public class AgentFilesBase implements InstallConstants, IConfigKeys {

    protected String asDomainDir = null;
    protected String agentLibPath;
    protected String agentLocalePath;
    protected String asLibDir;
    protected String asClassesDir;
    protected static final String[] localeExts = new String[]{"txt", "properties"};
    protected static final String[] libJarFiles = new String[]{"agent.jar", "openssoclientsdk.jar"};

    protected void getAgentConfigLocation(IStateAccess stateAccess) {
        agentLibPath = ConfigUtil.getLibPath();
        agentLocalePath = ConfigUtil.getLocaleDirPath();
        File homeDir = new File((String) stateAccess.get(STR_KEY_AS_HOME_DIR));
        asDomainDir = homeDir.getParent();
        asLibDir = asDomainDir + "/lib";
        asClassesDir = asLibDir + "/classes";
    }

    protected boolean removeAgentFiles(IStateAccess stateAccess) {
        boolean status = false;

        getAgentConfigLocation(stateAccess);
        if (VersionChecker.isGlassFishv3(stateAccess)) {
            status = removeAgentJarFiles();
            status &= removeAgentLocaleFiles();
        } else {
            //In case of GlassFish v2, we don't need to delete anything.
            status = true;
        }
        return status;
    }

    protected Collection<File> listAgentLocaleFiles() {
        return FileUtils.listFiles(new File(agentLocalePath), localeExts, false);
    }

    protected boolean removeAgentJarFiles() {
        boolean status = false;

        for (int i = 0; i < libJarFiles.length; i++) {
            Debug.log("AgentFilesBase.removeAgentJarFiles(): "
                    + " Removing file " + libJarFiles[i]);

            File file = new File(asLibDir + "/" + libJarFiles[i]);
            status = file.delete();
        }

        return status;
    }

    protected boolean removeAgentLocaleFiles() {
        boolean status = false;

        for (File localeFile : listAgentLocaleFiles()) {
            Debug.log("AgentFilesBase.removeAgentLocaleFiles(): "
                    + " Removing file " + localeFile.getName());
            File toDelete = new File(asClassesDir + "/" + localeFile.getName());
            status = toDelete.delete();
        }

        return status;
    }
}
