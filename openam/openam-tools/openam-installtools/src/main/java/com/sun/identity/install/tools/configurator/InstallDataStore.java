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
 * $Id: InstallDataStore.java,v 1.3 2008/06/25 05:51:20 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;

class InstallDataStore implements Serializable, InstallConstants {

    private static String getInstallStateFile() {
        return ConfigUtil.getDataDirPath() + FILE_SEP + STR_STATE_FILE_NAME;
    }

    public static synchronized InstallDataStore getInstallDataStore()
            throws InstallException {
        if (idStore == null) {
            String stateFileName = getInstallStateFile();
            File stateFile = new File(stateFileName);
            if (!stateFile.exists()) {
                Debug.log("InstallDataStore : creating a empty store.");
                idStore = new InstallDataStore(false);
            } else {
                Debug.log("InstallDataStore : loading an existing store.");
                FileInputStream fStream = null;
                ObjectInputStream oStream = null;
                try {
                    fStream = new FileInputStream(stateFile);
                    oStream = new ObjectInputStream(fStream);
                    idStore = (InstallDataStore) oStream.readObject();
                    idStore.setIsExistingStore(true);
                } catch (Exception e) {
                    Debug.log("InstallDataStore : Error loading State "
                            + "information ", e);
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

    private InstallDataStore(boolean isExistingStore) {
        setIsExistingStore(isExistingStore);
        setInstancesMap(new HashMap());
    }

    public int getConfiguredInstancesCount() {
        return getInstancesMap().size();

    }

    public StateData getGlobalDataCopy() {
        StateData globalDataCopy = null;
        StateData globalData = getGlobalData();

        if (globalData != null) {
            globalDataCopy = (StateData) globalData.clone();
        }

        return globalDataCopy;
    }

    public void setGlobalData(StateData globalData) {
        this.globalData = globalData;
    }

    public StateData getInstanceDataCopy(String instanceName) {
        StateData instanceDataCopy = null;
        StateData instanceData = getInstanceData(instanceName);

        if (getInstanceData(instanceName) != null) {
            instanceDataCopy = (StateData) instanceData.clone();
        }

        return instanceDataCopy;
    }

    public void addInstanceData(StateData instanceData) {
        getInstancesMap().put(instanceData.getInstanceName(), instanceData);
    }

    public void removeInstance(String instanceName) {
        getInstancesMap().remove(instanceName);
    }

    private StateData getGlobalData() {
        return globalData;
    }

    public boolean isExistingStore() {
        return isExistingStore;
    }

    private StateData getInstanceData(String instanceName) {
        return (StateData) getInstancesMap().get(instanceName);
    }

    public void setIsExistingStore(boolean isExistingStore) {
        this.isExistingStore = isExistingStore;
    }

    public void save() throws InstallException {
        String stateFileName = getInstallStateFile();

        if (getConfiguredInstancesCount() > 0) {
            Debug.log("InstallDataStore.save() - Number of Configured "
                    + "instances > 0. Saving the state file ..");
            FileOutputStream fStream = null;
            ObjectOutputStream oStream = null;
            try {
                fStream = new FileOutputStream(stateFileName);
                oStream = new ObjectOutputStream(fStream);
                oStream.writeObject(this);
            } catch (Exception e) {
                Debug.log("InstallDataStore : Error saving State "
                        + "information ", e);
                throw new InstallException(
                        LocalizedMessage.get(LOC_IS_ERR_SAVE_INSTALL_STATE), 
                        e);
            } finally {
                if (oStream != null) {
                    try {
                        oStream.flush();
                    } catch (IOException ie) {
                        // Ignore
                    }
                    try {
                        oStream.close();
                    } catch (IOException ie) {
                        // Ignore
                    }
                }
            }
        } else { // All the instances are deleted. So delete the file
            Debug.log("InstallDataStore.save() - Number of Configured "
                    + "instances = 0. Hence deleting the state file ..");
            File file = new File(stateFileName);
            file.delete();
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(LINE_SEP);
        sb.append("***BEGIN**************InstallDataStore*****************");
        sb.append(LINE_SEP);
        sb.append("  isExistingStore  : ").append(isExistingStore);
        sb.append(LINE_SEP);
        sb.append("  globalData       : ").append(globalData.toString());
        sb.append("  instances        : ").append(LINE_SEP);
        Iterator iter = getInstancesMap().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            sb.append("  ");
            sb.append((String) me.getKey()).append("  = ");
            sb.append(((StateData) me.getValue()).toString()).append("\n");
        }
        sb.append("***END***************InstallDataStore******************\n");

        return sb.toString();
    }

    private Map getInstancesMap() {
        return instances;
    }

    private void setInstancesMap(Map instances) {
        this.instances = instances;
    }

    private void setExistingStoreFlag(boolean flag) {
        isExistingStore = flag;
    }

    private StateData globalData = null;

    private Map instances = null;

    private static final long serialVersionUID = 1234989898989898989L;

    public static final String STR_STATE_FILE_NAME = ".am"
            + ToolsConfiguration.getProductShortName() + "State";

    private static transient InstallDataStore idStore = null;

    private transient boolean isExistingStore = false;
}
