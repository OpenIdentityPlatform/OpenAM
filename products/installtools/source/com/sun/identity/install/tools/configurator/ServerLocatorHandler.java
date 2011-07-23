/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServerLocatorHandler.java,v 1.3 2008/06/25 05:51:23 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.launch.IAdminTool;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;

class ServerLocatorHandler implements InstallConstants {

    protected ServerLocatorHandler(String serverLocatorClass) {
        setServerLocatorClass(serverLocatorClass);
    }

    protected void saveProductHome() throws InstallException {
        BufferedWriter bw = null;
        try {
            FileWriter fw = new FileWriter(getProductLocatorFile());
            bw = new BufferedWriter(fw);
            bw.write(ConfigUtil.getHomePath());
        } catch (Exception e) {
            Debug.log("ServerLocatorHome : Error saving Product Home to "
                    + " file - " + getProductLocatorFile(), e);
            throw new InstallException(
                    LocalizedMessage.get(LOC_DR_ERR_PRODUCT_LOCATOR_WRITE), e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException i) {
                    // Ignore
                }
            }
        }
    }

    protected void removeProductHome() throws InstallException {
        File locatorFile = new File(getProductLocatorFile());
        boolean successful = locatorFile.delete();
        Debug.log("ServerLocatorHandler: removing product locator file '"
                + getProductLocatorFile() + " successful: " + successful);
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
        if (file.exists() && file.canRead()) { // Not a fresh install
            // At this point, we have validateProductHome(..) which can 
            // throw an/ InstallException. So it can't be in the try catch 
            // block.
            productHome = readProductHome();
            validateProductHome(productHome, getProductLocatorFile());
        }

        return productHome;
    }

    protected String readProductHome() throws InstallException {
        String firstLine = null;
        BufferedReader br = null;
        try {
            FileReader fr = new FileReader(getProductLocatorFile());
            br = new BufferedReader(fr);
            firstLine = br.readLine();
        } catch (Exception e) {
            Debug.log("ServerLocatorHome : Error reading file - "
                    + getProductLocatorFile(), e);
            throw new InstallException(LocalizedMessage
                    .get(LOC_DR_ERR_PRODUCT_LOCATOR_READ), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException i) {
                    // Ignore
                }
            }
        }
        return firstLine;
    }

    private void validateProductHome(String productHome, String locatorFile)
            throws InstallException {
        if (productHome == null || productHome.trim().length() == 0) {
            Debug.log("ServerLocatorHome : Error invalid product home"
                    + " property '" + IAdminTool.PROP_PRODUCT_HOME + " = "
                    + productHome + "' in file " + locatorFile);
            throw new InstallException(LocalizedMessage
                    .get(LOC_DR_ERR_APP_SERVER_HOME_LOCATOR));
        } else if (!productHome.equals(ConfigUtil.getHomePath())) {
            Debug.log("ServerLocatorHome : Error Product home "
                    + "from Product locator file '" + locatorFile
                    + "' does not match with the product installation home '"
                    + ConfigUtil.getHomePath() + "'");
            Object[] args = { productHome };
            throw new InstallException(LocalizedMessage.get(
                    LOC_DR_ERR_INVALID_INSTALL_HOME, args));
        }
    }

    protected String getServerHomeDir(IStateAccess stateAccess)
            throws InstallException {
        IServerHomeDirLocator locator = null;
        try {
            locator = (IServerHomeDirLocator) Class.forName(
                    getServerLocatorClass()).newInstance();
        } catch (Exception e) {
            Debug.log("Driver : Error occurred while instantiating class: "
                    + getServerLocatorClass(), e);
            throw new InstallException(LocalizedMessage
                    .get(LOC_DR_ERR_APP_SERVER_HOME_LOCATOR), e);
        }

        return locator.getServerDirectory(stateAccess);
    }

    protected String getServerLocatorClass() {
        return serverLocatorClass;
    }

    protected String getProductLocatorFile() {
        return productInstallLocatorFile;
    }

    protected void setProductLocatorFile(String file) {
        productInstallLocatorFile = file;
    }

    private void setServerLocatorClass(String serverLocatorClass) {
        this.serverLocatorClass = serverLocatorClass;
    }

    private String productInstallLocatorFile;

    private String serverLocatorClass;

    public static final String STR_LOCATOR_FILE_NAME = ".am"
            + ToolsConfiguration.getProductShortName() + "Locator";

    public static final String LOC_DR_ERR_PRODUCT_LOCATOR_READ = 
        "DR_ERR_PRODUCT_LOCATOR_READ";

    public static final String LOC_DR_ERR_PRODUCT_LOCATOR_WRITE = 
        "DR_ERR_PRODUCT_LOCATOR_WRITE";
}
