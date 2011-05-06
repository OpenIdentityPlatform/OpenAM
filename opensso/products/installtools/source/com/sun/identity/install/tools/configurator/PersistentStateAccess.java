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
 * $Id: PersistentStateAccess.java,v 1.2 2008/06/25 05:51:23 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PersistentStateAccess implements IStateAccess, InstallConstants {

    /**
     * Should be instantiated only from InstallState
     */
    public PersistentStateAccess() {
    }

    public String getInstanceName() {
        return getInstanceData().getInstanceName();
    }

    /**
     * Gets the data (value) corresponding to the specified key.
     * 
     * @param key
     *            a key whose value needs to be determined
     * @return an Object representing the value correponding to the specified
     *         key. If no matching key is found, a null value is returned.
     */
    public Object get(String key) {
        return getCompleteData().get(key);
    }

    public void put(String key, Object value) {
        getData().put(key, value);

        if (isCommonData()) { // Global Data
            getGlobalData().put(key, value);
        } else { // InstanceData
            getInstanceData().put(key, value);
        }
    }

    public void remove(String key) {
        getData().remove(key);
        if (isCommonData()) { // Global Data
            getGlobalData().remove(key);
        } else { // InstanceData
            getInstanceData().remove(key);
        }
    }

    public void removeKeys(Set keys) {
        Iterator cIter = keys.iterator();
        while (cIter.hasNext()) {
            String key = (String) cIter.next();
            remove(key);
        }
    }

    public Map getData() {
        return getCompleteData();
    }

    /**
     * Method to merge existing data with data provided in the HashMap
     * 
     */
    public void putData(Map data) {
        if (isCommonData()) {
            getGlobalData().putAll(data);
        } else {
            getInstanceData().putAll(data);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(LINE_SEP);
        sb.append("*** BEGIN PersistentStateAccess ************");
        sb.append(LINE_SEP);
        sb.append("GlobalData:").append(LINE_SEP);
        sb.append("      ").append(getGlobalData().toString());
        sb.append("InstanceData:").append(LINE_SEP);
        sb.append("      ").append(getInstanceData().toString());
        sb.append("CompleteData:").append(LINE_SEP);

        Iterator iter = getCompleteData().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            sb.append("      ");
            sb.append((String) me.getKey()).append(" = ");
            sb.append(me.getValue().toString()).append(LINE_SEP);
        }
        sb.append("*** END PersistentStateAccess **************");
        sb.append(LINE_SEP);
        return sb.toString();
    }

    /**
     * Method to add data from the specified PersistentStateAccess object
     * corresponding to keys which are missing in this instance.
     * 
     * @param pStateAccess
     *            the PersistentStateAccess object from which the missing data
     *            will be added.
     */
    public void copyMissingData(PersistentStateAccess pStateAccess) {
        StateData gData = pStateAccess.getGlobalData();
        getGlobalData().copyMissingData(gData.getNameValueMap());

        StateData iData = pStateAccess.getInstanceData();
        getInstanceData().copyMissingData(iData.getNameValueMap());

        Map cData = pStateAccess.getCompleteData();
        Iterator iter = cData.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            if (!getCompleteData().containsKey(me.getKey())) {
                getCompleteData().put(me.getKey(), me.getValue());
            }
        }
    }

    public void setCommonDataFlag(boolean isCommonData) {
        this.isCommonData = isCommonData;
    }

    private boolean isCommonData() {
        return isCommonData;
    }

    public StateData getGlobalData() {
        return globalData;
    }

    public void setGlobalData(StateData sData) {
        globalData = sData;
    }

    public StateData getInstanceData() {
        return instanceData;
    }

    public void setInstanceData(StateData iData) {
        instanceData = iData;
    }

    public HashMap getCompleteData() {
        return completeData;
    }

    public void setCompleteData(HashMap data) {
        completeData = data;
    }

    // isCommonData is true if the data being stored is common type; false
    // otherwise. Default is true
    private boolean isCommonData = true;

    private StateData globalData;

    private StateData instanceData;

    private HashMap completeData; // Global + instance data
}
