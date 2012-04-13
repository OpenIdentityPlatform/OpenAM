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
 * $Id: UpdateServerClasspathBase.java,v 1.2 2008/11/28 12:36:23 saueree Exp $
 */


package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.OSChecker;

import java.io.File;

import java.util.ArrayList;


public class UpdateServerClasspathBase implements IConstants, IConfigKeys {
    protected String _catalinaHomeDir = null;
    protected String _setClassPathFile;
    protected String _setAgentClassPathFile;

    public UpdateServerClasspathBase() {
    }

    protected boolean unconfigureServerClassPath(IStateAccess state) {
        boolean status = true;

        getSetClasspathScriptFile(state);

        status = updateServerClassPath();
        status = removeAgentClassPathFile() && status;
        //status = removeAgentAppWar() && status;

        return status;
    }

    protected boolean updateServerClassPath() {
        boolean status = false;
        int index = -1;

        if ((index = FileUtils.getFirstOccurence(
                        _setClassPathFile,
                        constructAddAgentClassPathString(),
                        true,
                        false,
                        true,
                        0)) != -1) {
            status = FileUtils.removeLinesByNum(
                    _setClassPathFile,
                    index,
                    1);
        } else {
            Debug.log(
                "UpdateServerClasspathBase.updateServerClassPath(): " +
                "agent classpath string not found");
        }

        return status;
    }

    protected String constructAddAgentClassPathString() {
    	StringBuffer buff = new StringBuffer();

    	if (OSChecker.isWindows()) {
    		buff.append("call ");
    	} else {
    		buff.append(". ");
    	}

    	buff.append("\"")
    		.append(_setAgentClassPathFile)
    		.append("\"");

        Debug.log(
            "UpdateServerClasspathBase.constructAddAgentClassPathString(): "
            + buff.toString());

        return buff.toString();
    }

    protected boolean removeAgentClassPathFile() {
        boolean status = false;

        Debug.log(
            "UpdateServerClassPathBase.removeAgentClassPathFile(): "
            + " Removing file " + _setAgentClassPathFile);

        File file = new File(_setAgentClassPathFile);
        status = file.delete();

        return status;
    }

    protected boolean removeAgentAppWar() {
        boolean status = false;

        StringBuffer destDirBuff = new StringBuffer(_catalinaHomeDir);
        destDirBuff.append(STR_FORWARD_SLASH);
        destDirBuff.append(STR_WEBAPP_DIR);
        destDirBuff.append(STR_FORWARD_SLASH);
        destDirBuff.append(STR_AGENT_APP_WAR_FILE);

        String agentAppWar = destDirBuff.toString();

        Debug.log(
            "UpdateServerClassPathBase.removeAgentAppWar(): "
            + " Removing file " + agentAppWar);

        File file = new File(agentAppWar);
        status = file.delete();

        return status;
    }

    protected void getSetClasspathScriptFile(IStateAccess stateAccess) {
        if (_catalinaHomeDir == null) {
            _catalinaHomeDir = (String) stateAccess.get(
                    STR_KEY_CATALINA_HOME_DIR);

            String temp = _catalinaHomeDir + STR_FORWARD_SLASH
            				+ STR_BIN_DIRECTORY + STR_FORWARD_SLASH;

            if (OSChecker.isWindows()) {
                _setClassPathFile = temp + STR_SET_CLASSPATH_FILE_WINDOWS;
                _setAgentClassPathFile = temp
                    + STR_SET_AGENT_CLASSPATH_FILE_WINDOWS;
            } else {
                _setClassPathFile = temp + STR_SET_CLASSPATH_FILE_UNIX;
                _setAgentClassPathFile = temp
                    + STR_SET_AGENT_CLASSPATH_FILE_UNIX;
            }

            Debug.log(
                "getSetClasspathScriptFile(): script name = "
                + _setClassPathFile + " , agent script name = "
                + _setAgentClassPathFile);
        }

        return;
    }

    private String getCatalinaHomeDir() {
        return _catalinaHomeDir;
    }

    private void setCatalinaHomeDir(String catalinaHomeDir) {
        _catalinaHomeDir = catalinaHomeDir;
    }

    private String getSetClassPathFile() {
        return _setClassPathFile;
    }

    private void setSetClassPathFile(String setClassPathFile) {
        _setClassPathFile = setClassPathFile;
    }

    private String getAgentClassPathFile() {
        return _setAgentClassPathFile;
    }

    private void setAgentClassPathFile(String setAgentClassPathFile) {
        _setAgentClassPathFile = setAgentClassPathFile;
    }
}
