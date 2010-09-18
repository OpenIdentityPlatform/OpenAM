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
 * $Id: MigrateFromInstallState.java,v 1.2 2008/06/25 05:51:22 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;

/**
 * Class that encapuslates the state of an install instance. It provides
 * functionality to read instance specific data or global data.
 *
 */
public class MigrateFromInstallState implements InstallConstants {
    
    /**
     * Creates a new InstallState object. One of the following cases may
     * apply:-
     * If no instances are configured corresponding to the given keyValuePairs,
     * then an InstallState object containing the input instance
     * (keyValuePairs) data (supplied map) will be returned. If a configured
     * instance is found associated with keyValuePairs supplied, then an
     * InstallState object containing the corresponding instance data and
     * global data is returned. If none of the configured instances correspond
     * to the given keyValuePairs then an InstallState object with just the
     * global data is returned.
     *
     * @param keyValuePairs
     *            a Map containing key value pairs that should be used to look
     *            up for the associated instance.
     * @param keysToUse
     *            A set of keys that should be only used to form a unique key.
     */
    public MigrateFromInstallState(Map keyValuePairs, ArrayList keysToUse)
            throws InstallException {
        
        Debug.log("MigrateFromInstallState : initalizing the state");
        
        String instanceName = getInstFinderStore().getInstanceName(
                keyValuePairs, keysToUse);
        
        // Load old product's install data.
        getInstallDataStore();
        Debug.log("MigrateFromInstallState() - loaded Install state: " +
                getInstallDataStore());
        
        if (!MigrateFromInstallDataStore.isExistingStore()) {
            Debug.log("MigrateFromInstallState(): Error - " +
                    "No existing data store was found. " +
                    "Creating state with Instance Finder data.");
        } else {
            Debug.log("MigrateFromInstallState(): Existing data store found. " +
                    "Creating state.");
            // Existing install state (global & may be instance state too)
            initializeFromStore(instanceName, keyValuePairs);
        }
    }
    
    /**
     * get StateAccess saved.
     * @return PersistentStateAccess saved
     */
    public PersistentStateAccess getStateAccess() {
        return pStateAccess;
    }
    
    /**
     * Returns the name of the instance associated with this InstallState
     *
     * @return the instance name
     */
    public String getInstanceName() {
        return instanceName;
    }
    
    
    /*
     * initialized InstallState from install data saved in file.
     */
    private void initializeFromStore(String instanceName, Map nameValuePair)
            throws InstallException {
        PersistentStateAccess pStateAccess = new PersistentStateAccess();
        
        // Retrieve Global data copy (not reference)
        StateData globalData = getInstallDataStore().getGlobalDataCopy();
        pStateAccess.setGlobalData(globalData);
        
        HashMap completeData = new HashMap(globalData.getNameValueMap());
        pStateAccess.setCompleteData(completeData);
        
        // Retrieve copy of instance data (not reference)
        StateData instanceData = getInstallDataStore().getInstanceDataCopy(
                instanceName);
        if (instanceData == null) {
            Debug.log("MigrateFromInstallState : initializing. " +
                    "No instance data found for instance " + instanceName);
            // New Instance
            instanceData = new StateData(instanceName, true, false);
            pStateAccess.setInstanceData(instanceData);
            pStateAccess.getInstanceData().putAll(nameValuePair);
            pStateAccess.getCompleteData().putAll(nameValuePair);
        } else {
            // Already Configured Instance
            Debug.log("MigrateFromInstallState : initializing. " +
                    "Instance data found for instance: " + instanceName);
            pStateAccess.setInstanceData(instanceData);
            pStateAccess.getInstanceData().setInstanceAsConfigured(true);
            pStateAccess.getCompleteData().putAll(
                    instanceData.getNameValueMap());
        }
        setStateAccess(pStateAccess);
        setInstanceName(instanceName);
    }
    
    private static MigrateFromInstFinderStore getInstFinderStore() throws
            InstallException {
        return MigrateFromInstFinderStore.getInstance();
    }
    
    private static InstallDataStore getInstallDataStore() 
    throws InstallException {
        return MigrateFromInstallDataStore.getInstallDataStore();
    }
    
    private void setStateAccess(PersistentStateAccess stateAccess) {
        pStateAccess = stateAccess;
    }
    
    private void setInstanceName(String name) {
        instanceName = name;
    }
    
    private String instanceName;
    
    private PersistentStateAccess pStateAccess;
}
