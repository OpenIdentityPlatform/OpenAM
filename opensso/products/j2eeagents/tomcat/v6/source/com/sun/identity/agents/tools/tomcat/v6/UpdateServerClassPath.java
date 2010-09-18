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
 * $Id: UpdateServerClassPath.java,v 1.2 2008/11/28 12:36:23 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.OSChecker;
import com.sun.identity.install.tools.util.ReplaceTokens;

public class UpdateServerClassPath extends UpdateServerClasspathBase implements
		ITask {

	public boolean execute(String name, IStateAccess stateAccess,
		Map properties) throws InstallException {

		boolean status = true;

		try {

			getAgentClasspathDirectories(stateAccess);
			super.getSetClasspathScriptFile(stateAccess);

			status = status && updateSetClasspathScript(stateAccess);
			status = status && copyAgentClassPathFile(stateAccess);
			//status = status && copyAgentAppWarFile(stateAccess);

		} catch (Exception ex) {
			status = false;
			Debug.log(
					"UpdateServerClasspath.execute() - encountered exception "
							+ ex.getMessage(), ex);
		}

		return status;
	}

	private boolean updateSetClasspathScript(IStateAccess stateAccess) {
		boolean status = true;
		int index = -1;

		String addLine = super.constructAddAgentClassPathString();

		if (FileUtils.getFirstOccurence(_setClassPathFile, addLine, true,
				false, true, 0) == -1) {

			Debug
					.log("UpdateServerClasspath.updateSetClasspathScript(): " +
							"writing " + addLine + " to " + _setClassPathFile);

			if (OSChecker.isWindows()) {

				if ((index = FileUtils.getFirstOccurence(_setClassPathFile,
						STR_CLASSPATH_LINE, true,
						false, true, 0)) != -1) {

					status = FileUtils.insertLineByNumber(_setClassPathFile,
							index + 1, addLine);

				} else {
					Debug
							.log("UpdateServerClasspath." +
								"updateSetClasspathScript(): "
								+ "could not find classpath location");
				}
			} else {
				status = FileUtils.appendLinesToFile(_setClassPathFile,
						new String[] { addLine });
			}
		} else {
			Debug
					.log("UpdateServerClasspath.updateSetClasspathScript(): " +
							"agent classpath already present");
		}

		return status;
	}

	private boolean copyAgentClassPathFile(IStateAccess stateAccess)
			throws Exception {

		boolean status = true;

		String srcFile = createTagSwapFile(stateAccess);
		String destFile = _setAgentClassPathFile;

		stateAccess.put(STR_KEY_TOMCAT_AGENT_ENV_FILE_PATH, destFile);

		Map tokens = new HashMap();

		tokens.put("AGENT_LOCALE_DIR", _agentLocaleDir);
		tokens.put("AGENT_CONFIG_DIR", _agentInstanceConfigDirPath);
		tokens.put("AGENT_LIB_DIR", _agentLibPath);
		tokens.put("TOMCAT_SERVER_LIB", _catalinaServerLibDir);
		tokens.put("TOMCAT_COMMON_LIB", _catalinaCommonLibDir);
		tokens.put("CATALINA_HOME", _catalinaHomeDir);

		ReplaceTokens filter = new ReplaceTokens(srcFile, destFile, tokens);
		filter.tagSwapAndCopyFile();

		new File(srcFile).delete();
		Debug.log("UpdateServerClasspath.copyAgentClassPathFile() - " + srcFile
				+ " : " + destFile);

		return status;
	}

	private String createTagSwapFile(IStateAccess stateAccess)
	throws Exception
	{

		Map tokens = new HashMap();
		String srcFile;

		String version = (String) stateAccess.get(STR_TOMCAT_VERSION);

		if (OSChecker.isWindows()) {
			srcFile = ConfigUtil.getEtcDirPath() + STR_FORWARD_SLASH
					+ AGENT_ENV_CMD_TEMPLATE;

		} else {
			srcFile = ConfigUtil.getEtcDirPath() + STR_FORWARD_SLASH
					+ AGENT_ENV_SH_TEMPLATE;

		}

		String destFile = ConfigUtil.getEtcDirPath() + STR_FORWARD_SLASH
				+ "tokens.txt";

		ReplaceTokens filter = new ReplaceTokens(srcFile, destFile, tokens);
		filter.tagSwapAndCopyFile();

		return destFile;
	}

	private boolean copyAgentAppWarFile(IStateAccess stateAccess) {
		boolean status = true;
		String srcDir = ConfigUtil.getEtcDirPath();
		String destDir = _catalinaHomeDir + STR_FORWARD_SLASH + STR_WEBAPP_DIR;

		try {
			FileUtils.copyJarFile(srcDir, destDir, STR_AGENT_APP_WAR_FILE);
			Debug.log("UpdateServerClasspath.copyAgentAppWarFile() - copy "
					+ STR_AGENT_APP_WAR_FILE + " from " + srcDir + " to "
					+ destDir);
		} catch (Exception e) {
			Debug
				.log("UpdateServerClasspath.copyAgentAppWarFile() - " +
						"Error occured while copying "
						+ STR_AGENT_APP_WAR_FILE
						+ " from "
						+ srcDir
						+ " to " + destDir);
			status = false;
		}
		return status;
	}

	private void getAgentClasspathDirectories(IStateAccess stateAccess) {
		String homeDir = ConfigUtil.getHomePath();
		_agentLibPath = ConfigUtil.getLibPath();
		_agentLocaleDir = ConfigUtil.getLocaleDirPath();
		_catalinaCommonLibDir = _catalinaHomeDir + STR_FORWARD_SLASH
								+ STR_TOMCAT_COMMON_LIB;
		_catalinaServerLibDir = _catalinaHomeDir + STR_FORWARD_SLASH
								+ STR_TOMCAT_SERVER_LIB;
		_catalinaJarPath = (String) stateAccess.get(STR_CATALINA_JAR_PATH);

		String instanceName = stateAccess.getInstanceName();
		StringBuffer sb = new StringBuffer();
		sb.append(homeDir).append(STR_FORWARD_SLASH);
		sb.append(instanceName).append(STR_FORWARD_SLASH);
		sb.append(INSTANCE_CONFIG_DIR_NAME);
		_agentInstanceConfigDirPath = sb.toString();
	}

	public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
			Map properties) {
		super.getSetClasspathScriptFile(stateAccess);
		Object[] args = { _setClassPathFile };
		LocalizedMessage message = LocalizedMessage.get(
				LOC_TSK_MSG_UPDATE_SET_CLASSPATH_SCRIPT_EXECUTE,
				STR_TOMCAT_GROUP, args);

		return message;
	}

	public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
			Map properties) {
		Object[] args = { _setClassPathFile };
		LocalizedMessage message = LocalizedMessage.get(
				LOC_TSK_MSG_UPDATE_SET_CLASSPATH_SCRIPT_ROLLBACK,
				STR_TOMCAT_GROUP, args);
		return message;
	}

	public boolean rollBack(String name, IStateAccess stateAccess,
			Map properties) throws InstallException {
		boolean status = false;
		status = super.unconfigureServerClassPath(stateAccess);
		return status;
	}

	private String getAgentLibPath() {
		return _agentLibPath;
	}

	private void setAgentLibPath(String agentLibPath) {
		_agentLibPath = agentLibPath;
	}

	private String getAgentLocaleDir() {
		return _agentLocaleDir;
	}

	private void setAgentLocaleDir(String agentLocaleDir) {
		_agentLocaleDir = agentLocaleDir;
	}

	private String getAgentInstanceConfigDirPath() {
		return _agentInstanceConfigDirPath;
	}

	private void setAgentInstanceConfigDirPathe(
			String agentInstanceConfigDirPath) {
		_agentInstanceConfigDirPath = agentInstanceConfigDirPath;
	}

	private String getCatalinaCommonLibDir() {
		return _catalinaCommonLibDir;
	}

	private void setCatalinaHomeDir(String catalinaCommonLibDir) {
		_catalinaCommonLibDir = catalinaCommonLibDir;
	}

	private String getCatalinaServerLibDir() {
		return _catalinaServerLibDir;
	}

	private void setCatalinaServerLibDir(String catalinaServerLibDir) {
		_catalinaServerLibDir = catalinaServerLibDir;
	}

	private String getCatalinaJarPath() {
		return _catalinaJarPath;
	}

	private void setCatalinaJarPath(String catalinaJarPath) {
		_catalinaJarPath = catalinaJarPath;
	}

	public static final String LOC_TSK_MSG_UPDATE_SET_CLASSPATH_SCRIPT_EXECUTE
			= "TSK_MSG_UPDATE_SET_CLASSPATH_SCRIPT_EXECUTE";

	public static final String LOC_TSK_MSG_UPDATE_SET_CLASSPATH_SCRIPT_ROLLBACK
			= "TSK_MSG_UPDATE_SET_CLASSPATH_SCRIPT_ROLLBACK";

	private String _agentLibPath;

	private String _agentLocaleDir;

	private String _agentInstanceConfigDirPath;

	private String _catalinaCommonLibDir;

	private String _catalinaServerLibDir;

	private String _catalinaJarPath;
}
