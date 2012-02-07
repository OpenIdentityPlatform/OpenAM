/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MigrateServerLocatorHandler.java,v 1.2 2008/06/25 05:51:22 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.File;

import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.launch.IAdminTool;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;

/**
 * class to read/save product home saved in product locator file.
 *
 */
class MigrateServerLocatorHandler extends ServerLocatorHandler {
    
    protected MigrateServerLocatorHandler(String serverLocatorClass) {
        super(serverLocatorClass);
    }
    
    /**
     * Backup the old product home directory before migration.
     *
     * @throws InstallException
     */
    protected void backupProductHome()
    throws InstallException {
        
        String locatorFile = getProductLocatorFile();
        try {
            FileUtils.backupFile(locatorFile,
                    STR_BACK_UP_FILE_SUFFIX);
            saveProductHome();
            
        } catch (Exception e) {
            Debug.log("MigrateServerLocatorHandler - Error occurred "
                    + "while backup and deletion for file: '" + locatorFile
                    + "'.");
            Object[] args = { locatorFile };
            throw new InstallException(LocalizedMessage.get(
                    LOC_DR_ERR_PRODUCT_LOCATOR_BACKUP, args));
        }
    }
    
    /**
     * Returns the product home directory if the locator file is present or
     * returns null. If null => first time install
     *
     * @return the product home directory
     * @throws InstallException
     */
    protected String getProductHome(IStateAccess stateAccess)
    throws InstallException {
        String serverHomeDir = getServerHomeDir(stateAccess);
        setProductLocatorFile(serverHomeDir + FILE_SEP +
                STR_LOCATOR_FILE_NAME);
        
        String productHome = null;
        File file = new File(getProductLocatorFile());
        if (file.exists() && file.canRead()) {
            // Found old install
            productHome = readProductHome();
            validateProductHome(productHome, getProductLocatorFile());
            
            if (!productHome.equals(ConfigUtil.getHomePath())) {
                Debug.log("MigrateServerLocatorHandler.validateProductHome() : "
                        + "old Product home: "
                        + productHome + ", New Product Home: "
                        + ConfigUtil.getHomePath());
                
            } else {
                Debug.log("MigrateServerLocatorHandler - Error:"
                        + "This agent has been already migrated!");
                
                throw new InstallException(LocalizedMessage.get(
                        LOC_DR_ERR_PRODUCT_ALREADY_MIGRATED));
            }
            
        }
        
        return productHome;
    }
    
    private void validateProductHome(String productHome,
            String locatorFile) throws InstallException {
        if (productHome == null || productHome.trim().length() == 0) {
            Debug.log("MigrateServerLocatorHandler : Error invalid product home"
                    + " property '" + IAdminTool.PROP_PRODUCT_HOME + " = "
                    + productHome + "' in file " + locatorFile);
            throw new InstallException(LocalizedMessage
                    .get(LOC_DR_ERR_APP_SERVER_HOME_LOCATOR));
        }
    }
    
    public static final String STR_BACK_UP_FILE_SUFFIX = "-preAm"
            + ToolsConfiguration.getProductShortName();
    
}
