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
 * $Id: StateData.java,v 1.2 2008/06/25 05:51:24 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StateData implements Serializable, Cloneable, InstallConstants {

    public StateData(String instanceName, boolean isInstanceData,
            boolean isConfiguredInstance) {
        setIsInstanceDataFlag(isInstanceData);
        setIsConfiguredInstanceFlag(isConfiguredInstance);
        nameValueMap = new HashMap();
        if (isInstanceData) {
            this.instanceName = instanceName;
        } else {
            instanceName = STR_IS_GLOBAL_DATA_ID;
        }
    }

    public Object get(String key) {
        return nameValueMap.get(key);
    }

    public boolean contains(String key) {
        return nameValueMap.containsKey(key);
    }

    public void put(String key, Object data) {
        nameValueMap.put(key, data);
    }

    public void remove(String key) {
        nameValueMap.remove(key);
    }

    public void putAll(Map map) {
        nameValueMap.putAll(map);
    }

    private void setIsInstanceDataFlag(boolean flag) {
        isInstanceData = flag;
    }

    public boolean isInstanceData() {
        return isInstanceData;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public Map getNameValueMap() {
        return nameValueMap;
    }

    public void setInstanceAsConfigured(boolean isConfigured) {
        isConfiguredInstance = isConfigured;
    }

    public boolean isConfiguredInstance() {
        return isConfiguredInstance;
    }

    private void setIsConfiguredInstanceFlag(boolean flag) {
        isConfiguredInstance = flag;
    }

    public void clear() {
        nameValueMap.clear();
    }

    public boolean isEmpty() {
        return nameValueMap.isEmpty();
    }

    public void copyMissingData(Map data) {
        // Just make sure that we don't overwrite this objects nameValuePair
        // contents.
        Iterator iter = data.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            if (!getNameValueMap().containsKey(me.getKey())) {
                getNameValueMap().put(me.getKey(), me.getValue());
            }
        }
    }

    public Object clone() {
        StateData newStateData = new StateData(getInstanceName(),
                isInstanceData(), isConfiguredInstance());
        newStateData.getNameValueMap().putAll(getNameValueMap());
        return newStateData;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("    instanceName: ");
        sb.append(instanceName).append(LINE_SEP);
        sb.append("    isInstanceData: ");
        sb.append(isInstanceData).append(LINE_SEP);
        sb.append("    nameValueMap:").append(LINE_SEP);

        Iterator iter = nameValueMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            sb.append("      ");
            sb.append((String) me.getKey()).append(" = ");
            sb.append(me.getValue().toString()).append("\n");
        }

        return sb.toString();
    }

    private String instanceName;

    private boolean isInstanceData;

    private HashMap nameValueMap;

    // State to determine if the data was loaded from DataStore.
    private transient boolean isConfiguredInstance;

}
