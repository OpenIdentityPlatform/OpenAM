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

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.OSChecker;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import org.forgerock.agents.tools.jboss.CommonConstants;
import static org.forgerock.openam.agents.jboss.install.InstallerConstants.*;

/**
 * Validates input provided from the user during installation.
 *
 * @author Peter Major
 */
public class JBossValidator extends ValidatorBase {

    public JBossValidator() throws InstallException {
    }

    /**
     * Checks whether the provided JBoss home directory is valid or not.
     *
     * @param homeDir The JBoss home directory that needs to be validated.
     * @param params Extra parameters for the validator, not used.
     * @param state Agent installer state.
     * @return Success result if the provided path exists and is a directory.
     */
    public ValidationResult isHomeDirValid(String homeDir, Map params, IStateAccess state) {
        ValidationResultStatus status = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage message = LocalizedMessage.get(LOC_HOME_DIR_INVALID, new Object[]{homeDir});

        if (FileUtils.isDirValid(homeDir)) {
            status = ValidationResultStatus.STATUS_SUCCESS;
            message = LocalizedMessage.get(LOC_HOME_DIR_VALID, new Object[]{homeDir});
        }

        return new ValidationResult(status, null, message);
    }

    /**
     * Checks whether the JBoss installation's version contains the "AS 7" literal. The version is being detected by
     * executing:
     * <code>bin/&lt;instancename&gt;.(bat|sh) --version</code>
     *
     * @param instanceName The JBoss instance name, either <code>standalone</code> or <code>domain</code>
     * @param params Extra parameters for the validator, not used.
     * @param state Agent installer state.
     * @return Success result if the JBoss version string contains "AS 7".
     */
    public ValidationResult isVersionValid(String instanceName, Map params, IStateAccess state) {
        String homeDir = (String) state.get(HOME_DIR);
        ValidationResultStatus status = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage message = LocalizedMessage.get(LOC_VERSION_INVALID, BUNDLE_NAME, new Object[]{homeDir});

        StringBuilder command = new StringBuilder(100);
        command.append(homeDir).append(File.separator).append("bin").append(File.separator).append(instanceName);
        if (OSChecker.isWindows()) {
            command.append(".bat");
        } else {
            command.append(".sh");
        }
        //saving the CONFIG_FILE here, so we can create a backup using the BACKUP_FILE task later on
        state.put(CONFIG_FILE, homeDir + File.separator + instanceName + File.separator + "configuration"
                + File.separator + instanceName + ".xml");
        state.put(CommonConstants.STR_KEY_JB_INST_DEPLOY_DIR,
                homeDir + File.separator + instanceName + File.separator + "deployments");

        InputStream is = null;
        try {
            Debug.log("Executing command to verify JBoss version: " + command.toString() + " --version");
            Process process = Runtime.getRuntime().exec(new String[]{command.toString(), "--version"});
            is = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            Debug.log("Command output:");
            while ((line = br.readLine()) != null) {
                Debug.log(line);
                if (line.contains("AS 7") || line.contains("Application Server 7")) {
                    status = ValidationResultStatus.STATUS_SUCCESS;
                    message = LocalizedMessage.get(LOC_VERSION_VALID, BUNDLE_NAME);
                    break;
                }
            }
        } catch (IOException ioe) {
            Debug.log("An IOException occurred while verifying JBoss version", ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                }
            }
        }

        return new ValidationResult(status, null, message);
    }

    @Override
    protected void initializeValidatorMap() throws InstallException {
        Class[] paramObjs = {String.class, Map.class, IStateAccess.class};

        try {
            getValidatorMap().put("VALID_HOME_DIR", getClass().getMethod("isHomeDirValid", paramObjs));
            getValidatorMap().put("VALID_VERSION", getClass().getMethod("isVersionValid", paramObjs));
        } catch (NoSuchMethodException nsme) {
            Debug.log("JBossValidator: NoSuchMethodException thrown while loading method :", nsme);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("JBossValidator: SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("JBossValidator: Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
        }
    }
}
