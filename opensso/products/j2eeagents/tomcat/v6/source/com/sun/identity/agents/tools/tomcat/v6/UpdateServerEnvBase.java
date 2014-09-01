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

import com.sun.identity.agents.arch.IAgentConfigurationConstants;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.OSChecker;
import java.io.File;

/**
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class UpdateServerEnvBase implements IConstants, IConfigKeys {
    protected String _catalinaHomeDir = null;
    protected String _setenvFile;
    protected String _setJvmOption;
    protected String _agentInstanceConfigDirPath;
    protected String _agentLibPath;
    protected String _agentLocaleDir;
    protected String _catalinaCommonLibDir;

    public UpdateServerEnvBase() {
    }

    protected boolean unconfigureServerEnv(IStateAccess state) {
        boolean status = true;

        getSetenvScriptFile(state);
        getAgentConfigLocation(state);
        status = updateServerEnv();
        return status;
    }

    protected void getAgentConfigLocation(IStateAccess stateAccess) {
        String homeDir = ConfigUtil.getHomePath();
        _agentLibPath = ConfigUtil.getLibPath();
        _agentLocaleDir = ConfigUtil.getLocaleDirPath();
        _catalinaCommonLibDir = _catalinaHomeDir + STR_FORWARD_SLASH
                                                        + STR_TOMCAT_COMMON_LIB;
        String instanceName = stateAccess.getInstanceName();
        StringBuffer sb = new StringBuffer();
        sb.append(homeDir).append(STR_FORWARD_SLASH);
        sb.append(instanceName).append(STR_FORWARD_SLASH);
        sb.append(INSTANCE_CONFIG_DIR_NAME);
        _agentInstanceConfigDirPath = sb.toString();
    }

    protected boolean updateServerEnv() {
        boolean status = false;
        int index = -1;

        if ((index = FileUtils.getFirstOccurence(
                        _setenvFile,
                        constructAddJVMOptionString(),
                        true,
                        false,
                        true,
                        0)) != -1) {
            status = FileUtils.removeLinesByNum(
                    _setenvFile,
                    index,
                    1);
            
            // if setenv.sh is empty then we must of created it, remove it
            File setenvFile = new File(_setenvFile);

            if (setenvFile.length() == 0) {
                setenvFile.delete();
            }
        } else {
            Debug.log(
                "UpdateServerEnvBase.updateServerEnv(): " +
                "agent JVM option string not found");
        }

        return status;
    }

    protected String constructAddJVMOptionString() {
    	StringBuilder buff = new StringBuilder();

    	if (OSChecker.isWindows()) {
            buff.append("set JAVA_OPTS=%JAVA_OPTS% -D");
            buff.append(IAgentConfigurationConstants.CONFIG_JVM_OPTION_NAME);
            buff.append("=");
            buff.append(_agentInstanceConfigDirPath);
    	} else {
            buff.append("JAVA_OPTS=\"$JAVA_OPTS -D");
            buff.append(IAgentConfigurationConstants.CONFIG_JVM_OPTION_NAME);
            buff.append("=");
            buff.append(_agentInstanceConfigDirPath);
            buff.append("\"");
    	}

        Debug.log("UpdateServerEnvBase.constructAddJVMOptionString(): " +
                   buff.toString());

        return buff.toString();
    }

    protected void getSetenvScriptFile(IStateAccess stateAccess) {
        if (_catalinaHomeDir == null) {
            _catalinaHomeDir = (String) stateAccess.get(STR_KEY_CATALINA_BASE_DIR);

            String temp = _catalinaHomeDir + STR_FORWARD_SLASH
            				+ STR_BIN_DIRECTORY + STR_FORWARD_SLASH;

            if (OSChecker.isWindows()) {
                _setenvFile = temp + STR_SET_ENV_FILE_WINDOWS;
            } else {
                _setenvFile = temp + STR_SET_ENV_FILE_UNIX;
            }

            Debug.log("getSetenvScriptFile(): script name = " + _setenvFile);
        }

        return;
    }
}
