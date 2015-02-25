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
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import static org.forgerock.openam.agents.jboss.install.InstallerConstants.*;

/**
 * Performs the necessary steps to install/remove the agent's JBoss module.
 *
 * @author Peter Major
 */
public class ModuleBase {

    private static final String[] LOCALE_SUFFIXES = new String[]{"txt", "properties"};
    private static final String[] JAR_FILES = new String[]{"agent.jar", "openssoclientsdk.jar"};
    private static final String MODULE_PATH = "modules" + File.separator + "org" + File.separator + "forgerock"
            + File.separator + "openam" + File.separator + "agent" + File.separator + "main";

    /**
     * Creates the necessary directory structure for a custom JBoss module and copies the necessary agent components
     * along with the module's definition XML.
     *
     * @param state Agent installer state.
     * @return <code>true</code> if installing the agent module has been successful, <code>false</code> otherwise.
     * @throws InstallException If there was an error while trying to create the directory structure for the module.
     */
    public boolean performChanges(IStateAccess state) throws InstallException {
        File modulePath = getModulePath(state);
        File moduleLocaleDir = new File(modulePath.getPath() + File.separator + "locale");
        if (!modulePath.exists()) {
            if (!modulePath.mkdirs()) {
                throw new InstallException(LocalizedMessage.get(LOC_MODULE_MKDIR_FAIL));
            }
        }
        try {
            FileUtils.copyDirectory(new File(ConfigUtil.getLocaleDirPath()), moduleLocaleDir,
                    new SuffixFileFilter(LOCALE_SUFFIXES));
            Debug.log("Locale files has been copied to module directory");
            FileUtils.copyDirectory(new File(ConfigUtil.getLibPath()), modulePath, new NameFileFilter(JAR_FILES));
            Debug.log("Agent JAR files has been copied to module directory");
            FileUtils.copyFileToDirectory(new File(ConfigUtil.getConfigDirPath() + File.separator + "module.xml"),
                    modulePath);
        } catch (IOException ioe) {
            Debug.log("Unable to copy agent files to module directory", ioe);
            return false;
        }
        return true;
    }

    /**
     * Removes the agent's JBoss module from the JBoss installation by simply deleting the module's directory.
     *
     * @param state Agent installer state.
     * @return <code>true</code> if the module directory has been removed successfully, <code>false</code> otherwise.
     */
    protected boolean rollbackChanges(IStateAccess state) {
        try {
            FileUtils.deleteDirectory(getModulePath(state));
        } catch (IOException ioe) {
            Debug.log("Unable to delete agent module from JBoss", ioe);
            return false;
        }
        return true;
    }

    private File getModulePath(IStateAccess state) {
        String modulePath = (String) state.get(HOME_DIR);
        if (!modulePath.endsWith(File.separator)) {
            modulePath += File.separator;
        }
        modulePath += MODULE_PATH;
        return new File(modulePath);
    }
}
