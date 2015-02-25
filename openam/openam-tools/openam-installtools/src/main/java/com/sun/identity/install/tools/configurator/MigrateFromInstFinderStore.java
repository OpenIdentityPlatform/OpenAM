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
 * $Id: MigrateFromInstFinderStore.java,v 1.2 2008/06/25 05:51:22 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;


/**
 * This class reads the instance finder file, belonging to previous product.
 *
 */
public class MigrateFromInstFinderStore extends OrderedPropertyStore {
    
    /**
     * get the singleton of MigrateFromInstFinderStore.
     *
     * @return MigrateFromInstFinderStore
     * @throws InstallException thrown if reading instance finder file fails.
     */
    public static synchronized MigrateFromInstFinderStore getInstance()
    throws InstallException {
        
        if (instanceIdentifier == null) {
            instanceIdentifier = new MigrateFromInstFinderStore();
            File file = new File(getTranslateFile());
            if (file.exists() && file.canRead()) {
                Debug.log("MigrateFromInstFinderStore : " +
                        "initializing instance by loading" +
                        " the properties");
                instanceIdentifier.load();
                
            } else {
                Debug.log("MigrateFromInstFinderStore : " +
                        "Error - can not find old product finder store. " +
                        "File Name:" +
                        getTranslateFile());
                
                Object[] args = { getTranslateFile() };
                LocalizedMessage message = LocalizedMessage.get(
                        LOC_IS_ERR_IFINDER_FILE_NOT_EXIST, args);
                throw new InstallException(message);
            }
        }
        
        return instanceIdentifier;
    }
    
    /**
     * return the instance name to be migrated from.
     *
     * @param map
     * @param keysToUse
     * @return
     * @throws InstallException
     */
    public String getInstanceName(Map map, ArrayList keysToUse)
    throws InstallException {
        String uniqueKey = createUniqueKey(map, keysToUse);
        String instanceName = getProperty(uniqueKey);
        
        if (instanceName == null) {
            Debug.log("MigrateFromInstFinderStore.getInstanceName(): " +
                    "instanceName not " +
                    "found in the properties. Returning null value");
            
            Object[] args = { instanceName };
            LocalizedMessage message = LocalizedMessage.get(
                    LOC_IS_ERR_IFINDER_INSTANCE_NOT_FOUND, args);
            throw new InstallException(message);
            
        } else {
            Debug.log("MigrateFromInstFinderStore.getInstanceName(): " +
                    instanceName +
                    " found in the properties.");
        }
        
        return instanceName;
    }
    
    /**
     * get all instances' instance finder data.
     *
     * @param mapKeys
     * @return Map of all product's install details.
     * @throws InstallException
     */
    public Map getAllProductDetails(ArrayList mapKeys) throws InstallException {
        int count = size();
        Map productDetails = new TreeMap();
        for(int i=1; i<count; i++) { // Ignore the first one, it is counter
            String instanceName = getPropertyValue(i);
            String key = getPropertyKey(i);
            Map iFinderValues = extractInstanceFinderValues(key, instanceName,
                    mapKeys);
            productDetails.put(instanceName, iFinderValues);
        }
        return productDetails;
    }
    
    private Map extractInstanceFinderValues(String key, String instanceName,
            ArrayList mapKeys) throws InstallException {
        Map iFinderValues = new TreeMap();
        StringTokenizer st = new StringTokenizer(key,
                STR_IS_LOOK_UP_KEY_FIELD_SEP);
        
        int count = mapKeys.size();
        if (count != st.countTokens()) {
            Debug.log("MigrateFromInstFinderStore." +
                    "extractInstanceFinderValues(): " +
                    "Error - size of mapKeys and number of tokens " +
                    "are not same. key = " + key + ", mapKeys = " +
                    mapKeys.toString());
            Object[] args = { instanceName };
            LocalizedMessage message = LocalizedMessage.get(
                    LOC_IS_ERR_EXTRACT_IFINDER_DATA);
            throw new InstallException(message);
        }
        
        for (int i=0; i<count; i++) {
            String mapKey = (String) mapKeys.get(i);
            iFinderValues.put(mapKey, st.nextElement());
        }
        
        return iFinderValues;
    }
    
    private MigrateFromInstFinderStore() {
    }
    
    private String createUniqueKey(Map map, ArrayList keysToUse) {
        String uniqueKey = null;
        if (map != null && !map.isEmpty()) {
            // The keysToUse is an ArrayList which contains the keys to use in
            // the order (as defined in config file).
            StringBuffer sb = new StringBuffer();
            
            int count = keysToUse.size();
            for (int i = 0; i < count; i++) {
                String key = (String) keysToUse.get(i);
                // The value should never be null. All instance finder
                // interactions need a value.
                String value = (String) map.get(key);
                String newKey = replaceChars(value, CHR_CHARS_TO_REPLACE,
                        CHR_REPLACE_WITH_CHAR);
                sb.append(newKey);
                sb.append(STR_IS_LOOK_UP_KEY_FIELD_SEP);
            }
            
            uniqueKey = sb.toString();
        }
        Debug.log("MigrateFromInstFinderStore." +
                "createUniqueKey() - key: " + uniqueKey);
        
        return uniqueKey;
    }
    
    private String replaceChars(String replaceStr, char[] oldChars,
            char newChar) {
        String returnStr = replaceStr;
        if (replaceStr != null && oldChars != null) {
            char[] replaceStrChar = replaceStr.toCharArray();
            int l1 = replaceStrChar.length;
            int l2 = oldChars.length;
            for (int i=0; i<l1; i++) {
                for (int j=0; j<l2; j++) {
                    if (replaceStrChar[i] == oldChars[j]) {
                        replaceStrChar[i] = newChar;
                    }
                }
            }
            returnStr = new String(replaceStrChar);
        }
        return returnStr;
    }
    
    public String toString() {
        String displayStr = "*** BEGIN MigrateFromInstFinderStore Data " +
                "*******" +
                LINE_SEP + super.toString() + LINE_SEP +
                "*** END MigrateFromInstFinderStore Data " +
                "**********" +
                LINE_SEP;
        return displayStr;
    }
    
    /**
     * get product home of previous product.
     * @return 
     */
    public static String getProductHome() {
        return productHome;
    }
    
    /**
     * set product home of previous product.
     * @oldProductHome product home of previous product.
     */
    public static void setProductHome(String  oldProductHome) {
        productHome = oldProductHome;
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Overridden abstract methods of PropertyStore
    //////////////////////////////////////////////////////////////////////////
    public String getFile() {
        return getTranslateFile();
    }
    
    public static String getTranslateFile() {
        return getProductHome() + TRANSLATION_FILE;
    }
    
    public static String getRelativeTranslateFile() {
        return TRANSLATION_FILE;
    }
    public String getFileHeader() {
        return STR_IS_LOOK_UP_FILE_HEADER;
    }
    
    public LocalizedMessage getLoadErrorMessage() {
        return LocalizedMessage.get(LOC_IS_ERR_LOAD_INSTALL_STATE);
    }
    
    public LocalizedMessage getSaveErrorMessage() {
        return LocalizedMessage.get(LOC_IS_ERR_SAVE_INSTALL_STATE);
    }
    
    public LocalizedMessage getInvalidLineErrorMessage(int lineNumber) {
        Debug.log("MigrateFromInstFinderStore: An error occcurred while " +
                "reading " + "state file " + getFile() +
                ". Invalid data found at line " +
                lineNumber + "Missing delimiter: " + STR_KEY_VALUE_SEP);
        return getLoadErrorMessage(); // Generic message is sufficient
    }
    
    public LocalizedMessage getInvalidKeyErrorMessage(int lineNumber) {
        Debug.log("MigrateFromInstFinderStore: An error occcurred while " +
                "reading " + "state file " +
                getFile() + ". Invalid key found at line " +
                lineNumber);
        return getLoadErrorMessage(); // Generic message is sufficient
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    private static MigrateFromInstFinderStore instanceIdentifier = null;
    private static final char[] CHR_CHARS_TO_REPLACE = { '=', ' ' };
    private static final char CHR_REPLACE_WITH_CHAR = '-';
    
    private static String productHome = null;
    
    /** Field STR_IS_LOOK_UP_KEY_FIELD_SEP **/
    private static final String STR_IS_LOOK_UP_KEY_FIELD_SEP = "|";
    
    /** Field STR_IS_LOOK_UP_FILE_HEADER **/
    private static final String STR_IS_LOOK_UP_FILE_HEADER =
            "# Product Instances Translation Lookup File"; // # is necessary
    
    private static String TRANSLATION_FILE =
            FILE_SEP + "data" + FILE_SEP +
            ".am" + ToolsConfiguration.getProductShortName() +
            "Lookup";
    
    
    private static final String LOC_IS_ERR_EXTRACT_IFINDER_DATA =
            "IS_ERR_EXTRACT_IFINDER_DATA";
    
    private static final String LOC_IS_ERR_IFINDER_FILE_NOT_EXIST =
            "IS_ERR_IFINDER_FILE_NOT_EXIST";
    
    private static final String LOC_IS_ERR_IFINDER_INSTANCE_NOT_FOUND =
            "IS_ERR_IFINDER_INSTANCE_NOT_FOUND";
    
}
