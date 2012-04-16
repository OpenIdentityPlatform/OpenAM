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
 *
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import java.io.File;
import java.util.Collection;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class AgentFilesBase implements IConstants, IConfigKeys {
    protected String _catalinaHomeDir = null;
    protected String _agentLibPath;
    protected String _agentLocalePath;
    protected String _catalinaCommonLibDir;

    protected static final String[] localeExts = new String[] { "txt", "properties" };
    protected static final String[] libJarFiles = new String[] { "agent.jar", "openssoclientsdk.jar" };

    protected void getAgentConfigLocation(IStateAccess stateAccess) {
        _agentLibPath = ConfigUtil.getLibPath();
        _agentLocalePath = ConfigUtil.getLocaleDirPath();
        _catalinaHomeDir = (String) stateAccess.get(STR_KEY_CATALINA_HOME_DIR);
        _catalinaCommonLibDir = _catalinaHomeDir + STR_FORWARD_SLASH
                                                        + STR_TOMCAT_COMMON_LIB;
    }

    protected boolean removeAgentFiles(IStateAccess stateAccess) {
        boolean status = false;

        getAgentConfigLocation(stateAccess);
        status = removeAgentJarFiles();
        status = removeAgentLocaleFiles() && status;
        return status;
    }
    
    protected Collection<File> listAgentLocaleFiles() {
        return FileUtils.listFiles(new File(_agentLocalePath), localeExts, false);
    }

    protected boolean removeAgentJarFiles() {
        boolean status = false;

        for (int i = 0; i < libJarFiles.length; i++) {
            Debug.log("AgentFilesBase.removeAgentJarFiles(): " +
                      " Removing file " + libJarFiles[i]);

            File file = new File(_catalinaCommonLibDir + STR_FORWARD_SLASH + libJarFiles[i]);
            status = file.delete();
        }

        return status;
    }

    protected boolean removeAgentLocaleFiles() {
        boolean status = false;

        for (File localeFile : listAgentLocaleFiles()) {
            Debug.log("AgentFilesBase.removeAgentLocaleFiles(): " +
                      " Removing file " + _catalinaCommonLibDir + STR_FORWARD_SLASH +
                      localeFile.getName());
            File toDelete = new File(_catalinaCommonLibDir + STR_FORWARD_SLASH + localeFile.getName());
            status = toDelete.delete();
        }

        return status;
    }
}
