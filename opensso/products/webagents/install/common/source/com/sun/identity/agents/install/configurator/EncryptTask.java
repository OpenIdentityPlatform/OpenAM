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
 * $Id: EncryptTask.java,v 1.5 2008/06/25 05:54:38 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.configurator;

import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ExecuteCommand;
import com.sun.identity.install.tools.util.OSChecker;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.EncryptionKeyGenerator;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * The <code>EncryptTask</code> provides the implementation for
 * encrypting user supplied password for agent profile 
 * during agent installation. This implementation uses crypt_util/cryptit 
 * external utilities, provided as part of agent installation bits,
 * to encrypt plain text password. 
 */
public class EncryptTask implements ITask, InstallConstants {
        
    // User provided password file's lookup key
    private static final String STR_DATA_FILE_LOOKUP_KEY = 
        "DATA_FILE_LOOKUP_KEY"; 
    // Encrypted password's lookup key
    private static final String STR_ENCRYPTED_DATA_LOOKUP_KEY = 
        "ENCRYPTED_VALUE_KEY_LOOKUP_KEY";
    
    public static final String STR_ENCRYPTION_KEY_LOOKUP_KEY =    
        "ENCRYPTION_KEY_LOOKUP_KEY";

    // Localiziation keys
    private static final String LOC_TK_ERR_PASSWD_FILE_READ = 
        "TSK_ERR_PASSWD_FILE_READ";
    private static final String LOC_TSK_MSG_ENCRYPT_DATA_EXECUTE = 
        "TSK_MSG_ENCRYPT_DATA_EXECUTE";
    private static final String LOC_TSK_MSG_ENCRYPT_DATA_ROLLBACK = 
        "TSK_MSG_ENCRYPT_DATA_ROLLBACK";
    private static final String LOC_TSK_ERR_INVALID_APP_SSO_PASSWORD = 
        "TSK_ERR_INVALID_APP_SSO_PASSWORD";
    private static final String LOC_TSK_ERR_CRYPT_UTIL = 
        "TSK_ERR_CRYPT_UTIL";

    // crypt_util binary for unix platforms
    private static final String STR_UNIX_CRYPT_UTIL = "crypt_util";
    // cryptit binary for windows platform
    private static final String STR_WIN_CRYPT_UTIL = "cryptit.exe";

    private String encryptionKey=null;

    public static final String STR_ENCRYPTION_KEY_PROP_KEY = 
        "com.sun.identity.agents.config.key";

    /**
     * Calls crypt utility, which is in the bin directory, 
     * to encrypt the password provided by the user.
     * @param name 
     * @param stateAccess 
     * @param properties 
     * @throws com.sun.identity.install.tools.configurator.InstallException 
     * @return true or false status of task
     */
    public boolean execute(String name, IStateAccess stateAccess, 
        Map properties) throws InstallException 
    {

        String encryptionKeyLookUpKey = (String) properties.get(
            STR_ENCRYPTION_KEY_LOOKUP_KEY);
        Debug.log("EncryptTask.execute() - Obtained encryption lookup key = " + 
            encryptionKeyLookUpKey);        
        encryptionKey = (String) stateAccess.get(encryptionKeyLookUpKey);
        if (encryptionKey == null) {
            encryptionKey = EncryptionKeyGenerator.generateRandomString();
            stateAccess.put("AGENT_ENCRYPT_KEY", encryptionKey);
        }
        // Set the encrypted value key
        String encryptedDataKey = (String) properties.get(
            STR_ENCRYPTED_DATA_LOOKUP_KEY);

        String dataFileKey = (String) properties.get(STR_DATA_FILE_LOOKUP_KEY);
        String dataFileName = (String) stateAccess.get(dataFileKey);
        
        Debug.log("EncryptTask.execute() - Encrypting data stored in file '" +
            dataFileName + "'");
        // read plain text password from the file
        String data = readDataFromFile(dataFileName);
        // encrypt the plain text password
        String encryptedData = getEncryptedAppPassword(data);
        // store the encrypted password into install state
        stateAccess.put(encryptedDataKey, encryptedData);
        
        // This task does not have anything that could set the return value to
        // false. The task will only fail with fatal exceptions if they occur 
        // & halt the system.
        return true;
    }

    /**
     * Calls crypt utility, which is in the bin directory, 
     * to encrypt the password provided by the user.
     */
    private String getEncryptedAppPassword(String data) 
        throws InstallException {
    	
    	String applicationPassword = null;
        String cryptUtil = null;
    	String cryptUtilError = null;
    	
    	try {
            if (OSChecker.isWindows()) {
                //crypt util is crypt.exe
                cryptUtil = ConfigUtil.getBinDirPath() + 
                                FILE_SEP + STR_WIN_CRYPT_UTIL;
            } else {
                //crypt util is crypt_util
                cryptUtil = ConfigUtil.getBinDirPath() + 
                                FILE_SEP + STR_UNIX_CRYPT_UTIL;
            }
            // execute crypt utility to encrypt the password
            String[] commandArray = { cryptUtil, data, encryptionKey };
            StringBuffer output = new StringBuffer();
            int sts = ExecuteCommand.executeCommand(
                commandArray,
                null,
                output);

            if (output != null) {
                Debug.log(
                    "EncryptTask.getEncryptedAppPassword() command "
                    + "returned =" + output);
                applicationPassword = output.toString();
            }

    	} catch (Exception ex) {
            Debug.log("EncryptionHandler.getEncryptedAppPassword() - " +
                "failed to invoke method with exception :", ex);
            cryptUtilError = ex.toString();
    	}

        if (cryptUtilError != null) {  
       	    throw new InstallException(
                LocalizedMessage.get(LOC_TSK_ERR_CRYPT_UTIL));
        } else if (applicationPassword == null || 
        		applicationPassword.trim().length() == 0) {
       	    throw new InstallException(
                LocalizedMessage.get(LOC_TSK_ERR_INVALID_APP_SSO_PASSWORD));
        }
        
        return applicationPassword;
    }
    /**
     * Reads plain text password from the user provided file.
     */
    private String readDataFromFile(String fileName) 
        throws InstallException 
    {
        Debug.log("EncryptTask.readDataFromFile() - Reading data stored in" +
                " file '" + fileName + "'");
        String firstLine = null;
        BufferedReader br = null;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            InputStreamReader fir = new InputStreamReader(fis);            
            br = new BufferedReader(fir);
            firstLine = br.readLine();
        } catch (Exception e) {
            Debug.log("EncryptTask.readPasswordFromFile() - Error reading " +
                "file - " + fileName, e);
            throw new InstallException(LocalizedMessage.get(
                LOC_TK_ERR_PASSWD_FILE_READ), e);                  
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
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess, 
        Map properties) 
    {
        String dataFileKey = (String) properties.get(STR_DATA_FILE_LOOKUP_KEY);
        String dataFileName = (String) stateAccess.get(dataFileKey);
        Object[] args = { dataFileName };
        LocalizedMessage message = LocalizedMessage.get(
            LOC_TSK_MSG_ENCRYPT_DATA_EXECUTE , args);
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess, 
        Map properties) 
    {
        String dataFileKey = (String) properties.get(STR_DATA_FILE_LOOKUP_KEY);
        String dataFileName = (String) stateAccess.get(dataFileKey);
        Object[] args = { dataFileName };
        LocalizedMessage message = LocalizedMessage.get(
            LOC_TSK_MSG_ENCRYPT_DATA_ROLLBACK , args);        
        return message;
    }

    public boolean rollBack(String name, IStateAccess stateAccess, 
        Map properties) throws InstallException 
    {
        // Remove the encrypted data.        
        String encryptedDataKey = (String) properties.get(
            STR_ENCRYPTED_DATA_LOOKUP_KEY);        
        stateAccess.remove(encryptedDataKey);        
        return true;
    }
}
