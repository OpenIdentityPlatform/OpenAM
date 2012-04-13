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
 * $Id: InstFinderStore.java,v 1.2 2008/06/25 05:51:19 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator; 

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;

public class InstFinderStore extends OrderedPropertyStore {
        
    public static synchronized InstFinderStore getInstance() 
        throws InstallException 
    {
        if (instanceIdentifier == null) {            
            instanceIdentifier = new InstFinderStore();
            File file = new File(TRANSLATION_FILE);
            if (file.exists() && file.canRead()) {
                Debug.log("InstanceFinder : initializing instance by loading" +
                        " the properties");
                instanceIdentifier.load();
            } 
        }
        
        return instanceIdentifier;
    }
                   
    public String getInstanceName(Map map, ArrayList keysToUse) 
        throws InstallException 
    {
        String uniqueKey = createUniqueKey(map, keysToUse);        
        String instanceName = getProperty(uniqueKey);
        
        if (instanceName == null) {
            Debug.log("InstanceFinder.getInstanceName(): instanceName not " +
                "found in the properties. Returning null value");
        } else {
            Debug.log("InstanceFinder.getInstanceName(): instanceName " +
                "found in the properties.");
        }
        
        return instanceName;
    }
    
    public InstFinderData generateInstFinderData(Map map, 
        ArrayList keysToUse) throws InstallException 
    {
        String countStr = getProperty(STR_IS_LOOK_UP_COUNTER_KEY);
        int counter = 1; // Default - Starts from 1
        if (countStr != null) {
            try {
                counter = Integer.parseInt(countStr);
                counter++; // Next count
            } catch (NumberFormatException ne) {         
                LocalizedMessage message = LocalizedMessage.get(
                    LOC_IS_ERR_GENERATE_INST_NAME);
                throw new InstallException(message, ne);                
            }
        }     
        
        String nextCountStr = Integer.toString(counter);       
        String instanceName = generateInstanceName(nextCountStr);
        String key = createUniqueKey(map, keysToUse);
        
        return new InstFinderData(key, instanceName, nextCountStr); 
    }
            
    public void addInstFinderData(InstFinderData data) {
        // The Counter Key should be added first. That way it remains the 
        // first element in the properties.
        setProperty(STR_IS_LOOK_UP_COUNTER_KEY, data.nextCountStr());
        setProperty(data.getKey(), data.getInstanceName());        
    }
    
    public Map getAllProductDetails(ArrayList mapKeys) throws InstallException 
    {
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
    
    public Map getProductDetails(String instanceName, ArrayList mapKeys) 
        throws InstallException 
    {       
        String key = null;
        int count = size();
        for (int i=1; i<count; i++) { // Ignore the first one, it is counter
            if (getPropertyValue(i).equals(instanceName)) {
                key = getPropertyKey(i);
                break;
            }
        }
        
        Map iFinderValues = null;
        if (key != null) {
            iFinderValues = extractInstanceFinderValues(key, instanceName, 
                mapKeys);
        }
        // Returns null if the instanceName is not found
        return iFinderValues;
    }
    
    private Map extractInstanceFinderValues(String key, String instanceName, 
        ArrayList mapKeys) throws InstallException 
    {
        Map iFinderValues = new TreeMap();
        StringTokenizer st = new StringTokenizer(key, 
            STR_IS_LOOK_UP_KEY_FIELD_SEP);
        
        int count = mapKeys.size();
        if (count != st.countTokens()) {
            Debug.log("InstFinderStore.extractInstanceFinderValues(): " +
                "Error - size of mapKeys and number of tokens are not same. " +
                "key = " + key + ", mapKeys = " + mapKeys.toString());
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
    
    private InstFinderStore() {               
    }
      
    public Set getInstanceNames() {
        Set instanceNames = getPropertyValues();
        Debug.log("InstFinderStore.getInstanceNames() - Names are: " + 
                instanceNames);
        return instanceNames;        
    }
       
    public int getInstancesCount() {
        // One Entry is the instance counter
        return size() - 1; 
    }
               
    public void removeInstance(String instanceName) {
        Debug.log("InstFinderStore.remove(instanceName) removing " +
            "instance " + instanceName);        
        int count = size();
        for (int i=0; i<count; i++) {
            String value = getPropertyValue(i);
            if (value.equals(instanceName)) {
                removeProperty(i);
                break;
            }
        }        
    }
                
    private String generateInstanceName(String countStr) {
        // Generate String of format product_001, product_002 etc.,
        StringBuffer sb = new StringBuffer();
        sb.append(STR_IS_INSTANCE_NAME_PREFIX);
        sb.append(STR_IS_INSTANCE_NAME_SUFFIX);
        int sLength = sb.length();
        int cLength = countStr.length();        
        // Replace the prefix i.e., '000' with appropriate the counter number
        sb.replace(sLength - cLength, sLength, countStr);
        
        String instanceName = sb.toString();
        Debug.log("InstFinderStore.generateInstanceName(countStr): " +
                "generated instance name: " + instanceName);
        
        return instanceName;
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
        Debug.log("InstanceFinder.createUniqueKey() - key: " + uniqueKey);
        
        return uniqueKey;
    }
    
    private String replaceChars(String replaceStr, char[] oldChars, 
        char newChar)
    {
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
        String displayStr = "*** BEGIN InstFinderStore Data *******" +
                            LINE_SEP + super.toString() + LINE_SEP +
                            "*** END InstFinderStore Data **********" +
                            LINE_SEP;
        return displayStr;
    }
        
    //////////////////////////////////////////////////////////////////////////
    // Overridden abstract methods of PropertyStore
    //////////////////////////////////////////////////////////////////////////
    public String getFile() {
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
        Debug.log("InstFinderStore: An error occcurred while reading " +
            "state file " + getFile() + ". Invalid data found at line " + 
            lineNumber + "Missing delimiter: " + STR_KEY_VALUE_SEP);        
        return getLoadErrorMessage(); // Generic message is sufficient
    }
    
    public LocalizedMessage getInvalidKeyErrorMessage(int lineNumber) {
        Debug.log("InstFinderStore: An error occcurred while reading " +
            "state file " + getFile() + ". Invalid key found at line " + 
            lineNumber); 
        return getLoadErrorMessage(); // Generic message is sufficient    
    }
        
    //////////////////////////////////////////////////////////////////////////

    private static InstFinderStore instanceIdentifier = null;       
    private static final char[] CHR_CHARS_TO_REPLACE = { '=', ' ' };
    private static final char CHR_REPLACE_WITH_CHAR = '-';
    
      
   /** Field STR_IS_INSTANCE_NAME_PREFIX **/
    private static final String STR_IS_INSTANCE_NAME_PREFIX = 
        ToolsConfiguration.getProductShortName() + "_";

    /** Field STR_IS_LOOK_UP_KEY_FIELD_SEP **/
    private static final String STR_IS_LOOK_UP_KEY_FIELD_SEP = "|";
    
    /** Field STR_IS_LOOK_UP_FILE_HEADER **/
    private static final String STR_IS_LOOK_UP_FILE_HEADER = 
        "# Product Instances Translation Lookup File"; // # is necessary
        
    /** Field STR_IS_LOOK_UP_COUNTER_KEY **/
    private static final String STR_IS_LOOK_UP_COUNTER_KEY = 
        "Product_Instance_Count";               
    
    
    /** Field STR_IS_INSTANCE_NAME_SUFFIX **/
    private static final String STR_IS_INSTANCE_NAME_SUFFIX = "000";    
    
    private static final String TRANSLATION_FILE = 
        ConfigUtil.getDataDirPath() + FILE_SEP + 
        ".am" + ToolsConfiguration.getProductShortName() + 
        "Lookup";
    
    /** Field LOC_IS_ERR_LOAD_INSTALL_STATE **/
    private static final String LOC_IS_ERR_GENERATE_INST_NAME = 
        "IS_ERR_GENERATE_INST_NAME";
    
    private static final String LOC_IS_ERR_EXTRACT_IFINDER_DATA = 
        "IS_ERR_EXTRACT_IFINDER_DATA";
 
}
