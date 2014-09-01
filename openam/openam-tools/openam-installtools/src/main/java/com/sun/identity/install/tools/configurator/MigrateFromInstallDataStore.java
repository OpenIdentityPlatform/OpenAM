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
 * $Id: MigrateFromInstallDataStore.java,v 1.2 2008/06/25 05:51:22 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * This class reads the install state file, belonging to previous product.
 *
 */
class MigrateFromInstallDataStore implements Serializable, InstallConstants {
    
    /**
     * get install state store, reading state file if install state store
     * is null.
     *
     * @return InstallDataStore
     * @throws InstallException thrown when reading state file fails.
     */
    public static synchronized InstallDataStore getInstallDataStore()
    throws InstallException {
        
        if (idStore == null) {
            String stateFileName = getInstallStateFile();
            File stateFile = new File(stateFileName);
            
            if (!stateFile.exists()) {
                Debug.log(
                        "MigrateFromInstallDataStore.getInstallDataStore() -" +
                        " Error - state file:" + stateFileName +
                        " does not exist");
            } else {
                Debug.log(
                        "MigrateFromInstallDataStore.getInstallDataStore() -" +
                        "loading an existing store. File: " +
                        getInstallStateFile());
                FileInputStream fStream = null;
                ObjectInputStream oStream = null;
                
                try {
                    fStream = new FileInputStream(stateFile);
                    oStream = new ObjectInputStream(fStream);
                    idStore = (InstallDataStore) oStream.readObject();
                    setIsExistingStore(true);
                    
                } catch (Exception e) {
                    Debug.log(
                        "MigrateFromInstallDataStore.getInstallDataStore() - " +
                        "Error loading State information ", e);
                    throw new InstallException(LocalizedMessage
                            .get(LOC_IS_ERR_LOAD_INSTALL_STATE), e);
                } finally {
                    if (oStream != null) {
                        try {
                            oStream.close();
                        } catch (IOException ioe) {
                            // Ignore
                        }
                    }
                }
            }
        }
        
        return idStore;
    }
    
    /**
     * check if install state store exists.
     *
     * @return true if install state store exists, false if it does not.
     */
    public static boolean isExistingStore() {
        return isExistingStore;
    }
    
    /**
     * set if install state store exists.
     *
     * @param existingStore
     */
    private static void setIsExistingStore(boolean existingStore) {
        isExistingStore = existingStore;
    }
    
    /**
     * return product's home.
     *
     * @return the product home to migrate from
     */
    private static String getProductHome() {
        return productHome;
    }
    
    /**
     * set product's home.
     *
     * @param oldProductHome the product home to migrate from
     */
    public static void setProductHome(String oldProductHome) {
        productHome = oldProductHome;
    }
    
    private static String getInstallStateFile() {
        return getProductHome() + STR_STATE_FILE_NAME;
    }
    
    private static final String STR_STATE_FILE_NAME =
            FILE_SEP + "data" + FILE_SEP +
            ".am" + ToolsConfiguration.getProductShortName() + "State";
    
    private static transient InstallDataStore idStore = null;
    
    private static transient boolean isExistingStore = false;
    
    private static String productHome = null;
}
