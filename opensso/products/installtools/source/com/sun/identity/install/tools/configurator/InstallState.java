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
 * $Id: InstallState.java,v 1.3 2008/06/25 05:51:21 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.identity.install.tools.util.Debug;

/**
 * Class that encapuslates the state of an install instance. It provides
 * functionality to add/modify instance specific data or global data. It also
 * provides API's to save/delete the instance state.
 * 
 */
public class InstallState implements InstallConstants {

    /**
     * Returns the names of the instances already configured.
     * 
     * @return Set of instance names if any are configured otherwise an empty
     *         Set is returned.
     */
    public static Set getInstanceNames() throws InstallException {
        return getInstFinderStore().getInstanceNames();
    }

    /**
     * Returns the number of instances already configured.
     * 
     * @return a integer value representing the number of instances configured.
     */
    public static int getInstanceCount() throws InstallException {
        return getInstFinderStore().getInstancesCount();
    }

    public static boolean isFreshInstall() throws InstallException {
        return !getInstallDataStore().isExistingStore();
    }

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
    public InstallState(Map keyValuePairs, ArrayList keysToUse)
            throws InstallException {
        Debug.log("InstallState : initalizing the state");

        String instanceName = getInstFinderStore().getInstanceName(
                keyValuePairs, keysToUse);

        // If instance name was not found in store. Generate a new one.
        if (instanceName == null) {
            InstFinderData iFinderData = getInstFinderStore()
                    .generateInstFinderData(keyValuePairs, keysToUse);
            setInstFinderData(iFinderData);
            instanceName = iFinderData.getInstanceName();
        }

        if (!getInstallDataStore().isExistingStore()) {
            Debug.log("InstallState(): No existing data store was found. "
                    + "Creating state with Instance Finder data.");
            // New InstallStateOld
            initialize(instanceName, keyValuePairs);
        } else {
            Debug.log("InstallState(): Existing data store found. Creating "
                    + "state.");
            // Existing install state (global & may be instance state too)
            initializeFromStore(instanceName, keyValuePairs);
        }
    }

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

    /**
     * Returns true if the instance is already configured. Otherwise returns
     * false.
     * 
     * @return Returns true if the instance is already configured. Otherwise
     *         returns false.
     */
    public boolean isConfiguredInstance() {
        return getStateAccess().getInstanceData().isConfiguredInstance();
    }

    /**
     * Removes the instance. The method saveState() should be called to remove
     * it from the persistent store.
     */
    public void removeInstance() throws InstallException {
        getStateAccess().getInstanceData().clear();
        getInstallDataStore().removeInstance(getInstanceName());
        getInstFinderStore().removeInstance(getInstanceName());
    }

    public void saveState() throws InstallException {
        // TODO: Set the time stamps for the instance & global data's
        Debug.log("InstallState.saveState(): Saving state ..");

        getInstallDataStore().setGlobalData(getStateAccess().getGlobalData());
        // A new instance has been configured. So add it
        StateData instanceData = getStateAccess().getInstanceData();
        if (!instanceData.isEmpty()) {
            Debug.log("InstallState.saveState(): Instance Data present for "
                    + "instance " + getInstanceName() + ". It will be saved.");
            getInstallDataStore().addInstanceData(instanceData);
        }
        // Save it to the file
        getInstallDataStore().save();

        // Save the Translation Properties
        if (getInstFinderData() != null) {
            getInstFinderStore().addInstFinderData(getInstFinderData());
        }
        getInstFinderStore().save();
    }

    private void initialize(String instanceName, Map nameValuePair) {
        PersistentStateAccess pStateAccess = new PersistentStateAccess();
        StateData globalData = new StateData(STR_IS_GLOBAL_DATA_ID, false,
                false);
        StateData instanceData = new StateData(instanceName, true, false);
        pStateAccess.setGlobalData(globalData);
        pStateAccess.setInstanceData(instanceData);
        pStateAccess.setCompleteData(new HashMap());
        pStateAccess.getInstanceData().putAll(nameValuePair);
        pStateAccess.getCompleteData().putAll(nameValuePair);
        setStateAccess(pStateAccess);
        setInstanceName(instanceName);
    }

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
            Debug.log("InstallState : initializing. No instance data found "
                    + "for instance " + instanceName);
            // New Instance
            instanceData = new StateData(instanceName, true, false);
            pStateAccess.setInstanceData(instanceData);
            pStateAccess.getInstanceData().putAll(nameValuePair);
            pStateAccess.getCompleteData().putAll(nameValuePair);
        } else {
            // Already Configured Instance
            Debug.log("InstallState : initializing. Instance data found "
                    + "for instance " + instanceName);
            pStateAccess.setInstanceData(instanceData);
            pStateAccess.getInstanceData().setInstanceAsConfigured(true);
            pStateAccess.getCompleteData().putAll(
                    instanceData.getNameValueMap());
        }
        setStateAccess(pStateAccess);
        setInstanceName(instanceName);
    }

    private static InstFinderStore getInstFinderStore() throws InstallException
    {
        return InstFinderStore.getInstance();
    }

    private static InstallDataStore getInstallDataStore()
            throws InstallException {
        return InstallDataStore.getInstallDataStore();
    }

    private InstFinderData getInstFinderData() {
        return iFinderData;
    }

    protected void setStateAccess(PersistentStateAccess stateAccess) {
        pStateAccess = stateAccess;
    }

    private void setInstanceName(String name) {
        instanceName = name;
    }

    private void setInstFinderData(InstFinderData data) {
        iFinderData = data;
    }

    private InstFinderData iFinderData;

    private String instanceName;

    private PersistentStateAccess pStateAccess;
}
