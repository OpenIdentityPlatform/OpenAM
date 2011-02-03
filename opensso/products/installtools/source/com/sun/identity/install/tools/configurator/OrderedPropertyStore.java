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
 * $Id: OrderedPropertyStore.java,v 1.2 2008/06/25 05:51:23 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * An abstract class which encapulates the functionality to save/load 
 * properties to/from a file. Also the order of the properties are preserved.
 */
public abstract class OrderedPropertyStore implements InstallConstants {

    abstract public String getFile();

    abstract public String getFileHeader();

    abstract public LocalizedMessage getLoadErrorMessage();

    abstract public LocalizedMessage getSaveErrorMessage();

    abstract public LocalizedMessage getInvalidKeyErrorMessage(int lineNumber);

    abstract public LocalizedMessage getInvalidLineErrorMessage(
            int lineNumber);

    public OrderedPropertyStore() {
        setProperties(new OrderedProperties());
    }

    public void load() throws InstallException {

        BufferedReader br = null;

        Debug.log("OrderedPropertyStore: Loading the properties from file '"
                + getFile() + "'.");
        try {
            FileReader fr = new FileReader(getFile());
            br = new BufferedReader(fr);
            String lineData = null;
            int lineNumber = 1;
            while ((lineData = br.readLine()) != null) {
                // Ignore Empty lines or commented lines starting with '#'
                if (lineData.trim().length() > 0
                        && !lineData.startsWith(STR_KEY_COMMENT_MARKER)) {
                    int separatorIndex = lineData.indexOf(STR_KEY_VALUE_SEP);
                    if (separatorIndex != -1) {
                        String key = lineData.substring(0, separatorIndex);
                        verifyKeyString(key.trim(), lineNumber);

                        // Store the value even if it is empty String
                        String value = lineData.substring(separatorIndex + 1);
                        getProperties().put(key, value.trim());
                    } else { // NO Delimiter '=' found. So throw an exception
                        throw new InstallException(
                                getInvalidLineErrorMessage(lineNumber));
                    }
                }
                ++lineNumber;
            }
        } catch (Exception e) {
            Debug.log("OrderedPropertyStore: Error loading the properties", e);
            throw new InstallException(getLoadErrorMessage(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException i) {
                }
            }
        }
        Debug.log("OrderedPropertyStore.load() Contents: " + LINE_SEP
                + toString());
    }

    public void save() throws InstallException {
        Debug.log("OrderedPropertyStore: Saving the properties to file '"
                + getFile() + "'.");
        Debug.log("OrderedPropertyStore.load() Saving Contents: " + LINE_SEP
                + toString());
        BufferedWriter bw = null;
        try {
            if (!getProperties().isEmpty()) { // Data to save is present
                FileWriter fw = new FileWriter(getFile());
                bw = new BufferedWriter(fw);
                int count = getProperties().size();
                // Write the header
                bw.write(getFileHeader() + LINE_SEP);
                for (int i = 0; i < count; i++) {
                    bw.write(getProperties().getKeyValueString(i) + LINE_SEP);
                }
            } else { // Nothing to save
                Debug.log("OrderedPropertyStore: No data present in "
                        + "KeyValueMap. Nothing to Save!!");
            }
        } catch (Exception e) {
            Debug.log("OrderedPropertyStore: Error saving the properties "
                    + "to file '" + getFile() + "'.", e);
            throw new InstallException(getSaveErrorMessage(), e);
        } finally {
            if (bw != null) {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException i) {
                }
            }
        }
    }

    public String getPropertyKey(int index) {
        return getProperties().getKey(index);
    }

    public String getPropertyValue(int index) {
        return getProperties().getValue(index);
    }

    public void removeProperty(int index) {
        getProperties().remove(index);
    }

    public void removeProperty(String key) {
        getProperties().remove(key);
    }

    public Set getPropertyValues() {
        return getProperties().getValues();
    }

    public Set getPropertyKeys() {
        return getProperties().getKeys();
    }

    public String getProperty(String key) {
        return getProperties().get(key);
    }

    public void setProperty(String key, String value) {
        getProperties().put(key, value);
    }

    public int size() {
        return getProperties().size();
    }

    public boolean isEmpty() {
        return getProperties().isEmpty();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        int count = size();
        for (int i = 0; i < count; i++) {
            sb.append("     ");
            sb.append(getProperties().getKeyValueString(i));
            sb.append(LINE_SEP);
        }
        return sb.toString();
    }

    private OrderedProperties getProperties() {
        return orderedProperties;
    }

    private void setProperties(OrderedProperties properties) {
        orderedProperties = properties;
    }

    private void verifyKeyString(String key, int lineNumber)
            throws InstallException {
        // Key can't be null as the substring method only returns an empty
        // String. The key is already trimmed before calling.
        if (key == null || key.length() == 0) {
            throw new InstallException(getInvalidKeyErrorMessage(lineNumber));
        }
    }

    private OrderedProperties orderedProperties;

    public static final String STR_KEY_VALUE_SEP = "=";

    public static final String STR_KEY_COMMENT_MARKER = "#";

    public static final String STR_KEY_VALUE_SEP_AND_SPACE = STR_KEY_VALUE_SEP
            + " ";

    /*
     * Inner Class that provides Ordered Map functionality. This is probably 
     * not a best way of doing it, but the main class OrderedPropertyStore 
     * being abstract, the implementation can be changed any time.
     */
    private class OrderedProperties {
        OrderedProperties() {
            keyValueMap = new HashMap();
            keyValueList = new ArrayList(); // Preserves ordering
        }

        void put(String key, String value) {
            keyValueMap.put(key, value);
            int index = removeFromList(key); // If already present remove it.
            if (index != -1) { // maintain the previous order even if updated
                keyValueList.add(index, new KeyValue(key, value));
            } else { // not present so add it to the end of the list
                keyValueList.add(new KeyValue(key, value));
            }
        }

        void remove(int index) {
            String key = getKey(index);
            keyValueMap.remove(key);
            keyValueList.remove(index);
        }

        void remove(String key) {
            keyValueMap.remove(key);
            removeFromList(key);
        }

        int removeFromList(String key) {
            int count = keyValueList.size();
            for (int i = 0; i < count; i++) {
                KeyValue keyValue = (KeyValue) keyValueList.get(i);
                if (keyValue.getKey().equals(key)) {
                    keyValueList.remove(i);
                    return i;
                }
            }
            return -1;
        }

        String getKey(int index) {
            return ((KeyValue) keyValueList.get(index)).getKey();
        }

        String getValue(int index) {
            return ((KeyValue) keyValueList.get(index)).getValue();
        }

        String get(String key) {
            return (String) keyValueMap.get(key);
        }

        Set getKeys() {
            Set keys = new HashSet();
            int count = size();
            for (int i = 0; i < count; i++) {
                keys.add(getKey(i));
            }
            return keys;
        }

        Set getValues() {
            Set values = new HashSet();
            int count = size();
            for (int i = 0; i < count; i++) {
                values.add(getValue(i));
            }
            return values;
        }

        String getKeyValueString(int index) {
            return ((KeyValue) keyValueList.get(index)).getKeyValueString();
        }

        boolean isEmpty() {
            return keyValueMap.isEmpty();
        }

        int size() {
            return keyValueMap.size();
        }

        String getDataString() {
            StringBuffer sb = new StringBuffer();
            int count = size();
            for (int i = 0; i < count; i++) {
                sb.append(getKeyValueString(i));
                sb.append(LINE_SEP);
            }
            return sb.toString();
        }

        private Map keyValueMap;

        private List keyValueList;
    }

    private class KeyValue {

        KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String getKey() {
            return key;
        }

        String getValue() {
            return value;
        }

        String getKeyValueString() {
            return key + STR_KEY_VALUE_SEP_AND_SPACE + value;
        }

        private String key;

        private String value;
    }

}
