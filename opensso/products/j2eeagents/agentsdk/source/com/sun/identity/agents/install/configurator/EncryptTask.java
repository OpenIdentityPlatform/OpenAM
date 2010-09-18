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
 * $Id: EncryptTask.java,v 1.3 2008/06/25 05:51:52 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.configurator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.lang.reflect.Method;

import com.iplanet.services.util.Crypt;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.EncryptionKeyGenerator;
import com.sun.identity.install.tools.util.LocalizedMessage;


/**
 * This class performs password encryption
 */
public class EncryptTask implements ITask, InstallConstants {
        
    public boolean execute(String name, IStateAccess stateAccess, 
        Map properties) throws InstallException 
    {
        Debug.log("EncryptTask.execute() - Setting up System encrypt " +
            "properties.");

        // Set the debug directory !!
        String debugLogsDirPath = (String) stateAccess.get(
            STR_DEBUG_DIR_PREFIX_TAG);
        System.setProperty(STR_DEBUG_DIR_PROPERTY, debugLogsDirPath);

        String debugLevel = (String) stateAccess.get(STR_DEBUG_LEVEL_TAG);
        System.setProperty(STR_DEBUG_LEVEL_PROPERTY, debugLevel);

        // Set the encryption key
        String encryptionKeyLookUpKey = (String) properties.get(
            STR_ENCRYPTION_KEY_LOOKUP_KEY);
        Debug.log("EncryptTask.execute() - Obtained encryption lookup key = " + 
            encryptionKeyLookUpKey);        
        String encryptionKey = (String) stateAccess.get(encryptionKeyLookUpKey);
	if (encryptionKey == null) {
	    encryptionKey = EncryptionKeyGenerator.generateRandomString();
            stateAccess.put("AGENT_ENCRYPT_KEY", encryptionKey);
	}
        Debug.log("EncryptTask.execute() - Obtained encryption key = " + 
            encryptionKey);
        System.setProperty(STR_ENCRYPTION_KEY_PROP_KEY, encryptionKey);
        
        // Set the Encryption modules
        System.setProperty(STR_SECURITY_ENCRYPTOR_PROP_KEY, 
            STR_SECURITY_ENCRYPTOR_PROP_VALUE);

        // Set the encrypted value key        
        String encryptedDataKey = (String) properties.get(
            STR_ENCRYPTED_DATA_LOOKUP_KEY);
        
        String dataFileKey = (String) properties.get(STR_DATA_FILE_LOOKUP_KEY);
        String dataFileName = (String) stateAccess.get(dataFileKey);
        
        Debug.log("EncryptTask.execute() - Encrypting data stored in file '" +
            dataFileName + "'");
        String data = readDataFromFile(dataFileName);       
        String encryptedData = getEncryptedAppPassword(data);
        stateAccess.put(encryptedDataKey, encryptedData);
        
        // This task does not have anything that could set the return value to
        // false. The task will only fail with fatal exceptions if they occur 
        // & halt the system.
        return true;
    }
    
    private String getEncryptedAppPassword(String data) throws InstallException {
    	String applicationPassword = null;
    	Method method = null;
    	
    	try {
    		method = Crypt.class.getMethod(STR_ENCRYPT_LOCAL_FUNCTION,
						new Class[]{String.class});    
    	} catch (Exception ex) {
    		if (method == null) {
    			Debug.log("EncryptTask.getEncryptedAppPassword() - failed to get " +
    				"method from SDK with exception : ",ex);
    			Debug.log("EncryptionHandler.getEncryptedAppPassword() - making " +
                    "second attempt to load method");
    				try {
	    		        method = Crypt.class.getMethod(STR_ENCRYPT_FUNCTION,
	    							new Class[]{String.class});
    				} catch (Exception e) {
    					Debug.log("EncryptionHandler.getEncryptedAppPassword() - "
    		                    + "failed to load method with exception : ", e);
    				}
    			if (method == null) {
    			    throw new InstallException(
    				LocalizedMessage.get(LOC_TSK_ERR_ENCRYPT_PASSWORD_INVOKE_METHOD));	
    			}
    		 }	
        }
    	
    	try {
	    	if (method != null) {
	    		 applicationPassword =  
	    		 	(String)method.invoke(Crypt.class,new Object[]{data});
	        }
    	} catch (Exception ex) {
    		   Debug.log("EncryptionHandler.getEncryptedAppPassword() - "
                    + "failed to invoke method with exception :", ex);
    	}

        if (applicationPassword == null || 
        		applicationPassword.trim().length() == 0) {
        	throw new InstallException(
        			LocalizedMessage.get(LOC_TSK_ERR_INVALID_APP_SSO_PASSWORD));
        }
        
        return applicationPassword;
       
    }
       
    
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
    
    // Lookup keys
    public static final String STR_DATA_FILE_LOOKUP_KEY = 
        "DATA_FILE_LOOKUP_KEY"; 
    public static final String STR_ENCRYPTED_DATA_LOOKUP_KEY = 
        "ENCRYPTED_VALUE_KEY_LOOKUP_KEY";
    public static final String STR_ENCRYPTION_KEY_LOOKUP_KEY =    
        "ENCRYPTION_KEY_LOOKUP_KEY";
    
    // Localiziation keys
    public static final String LOC_TK_ERR_PASSWD_FILE_READ = 
        "TSK_ERR_PASSWD_FILE_READ";
    public static final String LOC_TSK_MSG_ENCRYPT_DATA_EXECUTE = 
        "TSK_MSG_ENCRYPT_DATA_EXECUTE";
    public static final String LOC_TSK_MSG_ENCRYPT_DATA_ROLLBACK = 
        "TSK_MSG_ENCRYPT_DATA_ROLLBACK";
    public static final String LOC_TSK_ERR_ENCRYPT_PASSWORD_INVOKE_METHOD = 
        "TSK_ERR_ENCRYPT_PASSWORD_INVOKE_METHOD";
    public static final String LOC_TSK_ERR_INVALID_APP_SSO_PASSWORD = 
        "TSK_ERR_INVALID_APP_SSO_PASSWORD";
    
    public static final String STR_ENCRYPTION_KEY_PROP_KEY = 
        "am.encryption.pwd";
    public static final String STR_SECURITY_ENCRYPTOR_PROP_KEY = 
        "com.iplanet.security.encryptor";    
    public static final String STR_SECURITY_ENCRYPTOR_PROP_VALUE = 
        "com.iplanet.services.util.JCEEncryption";
    public static final String STR_ENCRYPT_LOCAL_FUNCTION = "encryptLocal";
    public static final String STR_ENCRYPT_FUNCTION = "encrypt";

    public static final String STR_DEBUG_DIR_PROPERTY =
        "com.iplanet.services.debug.directory";
    public static final String STR_DEBUG_LEVEL_PROPERTY =
        "com.iplanet.services.debug.level";

    public static final String STR_DEBUG_LEVEL_TAG = "DEBUG_LEVEL";
    
}
