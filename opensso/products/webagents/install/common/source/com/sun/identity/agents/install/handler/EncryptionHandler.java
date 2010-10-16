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
 * $Id: EncryptionHandler.java,v 1.3 2008/08/04 19:22:14 huacui Exp $
 *
 *
 * This is the Encryption handler for webagents.
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.agents.install.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import com.sun.identity.install.tools.admin.ICommonToolsConstants;
import com.sun.identity.install.tools.admin.IToolsOptionHandler;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

import com.sun.identity.install.tools.util.ExecuteCommand;
import com.sun.identity.install.tools.util.OSChecker;
import com.sun.identity.install.tools.util.ConfigUtil;
import java.util.List;

/**
 * This class provides password encryption 
 */

public class EncryptionHandler implements IToolsOptionHandler, ICommonToolsConstants {

    public boolean checkArguments(List arguments) {
        boolean result = true;
        if (arguments.size() == 2) {
            String agentId = (String) arguments.get(0);
            String passwordFile = (String) arguments.get(1);
            if (validateAgentIdentifier(agentId)) {
                String passwordText = getPasswordText(passwordFile);
                if (passwordText != null) {
                    Debug.log("EncryptionHandler.checkArguments: valid args");
                } else {
                    result = false;
                    Console.println();
                    Console.println(
                         LocalizedMessage.get(LOC_HR_ERR_ENCRYPT_PASSWORD));
                }           
            } else {
                result = false;
                Console.println();
                Console.println(
                        LocalizedMessage.get(LOC_HR_ERR_ENCRYPT_AGENT_ID));
            }
        } else {
            result = false;
            Console.println();
            Console.println(LocalizedMessage.get(LOC_HR_ERR_ENCRYPT_ARGS));
            Console.println();
        }
        
        return result;
    }

    public void handleRequest(List arguments) {
        boolean result = false;
        String agentId = (String) arguments.get(0);
        String passwordFile = (String) arguments.get(1);

        String configFilePath = getAgentConfigFilePath(agentId);
        String passwordText = getPasswordText(passwordFile);

        Properties properties = new Properties();
        InputStream instream = null;
        String encryptedText = null;
        Method method = null;
        
        try {
            instream = new FileInputStream(configFilePath);
            properties.load(instream);

            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                if (name != null && name.trim().length() > 0) {
                    String value = properties.getProperty(name);
                    System.setProperty(name, value);
                } else {
                    Debug.log("EncryptionHandler.handleRequest: "
                            + "found empty property key: skipping");
                }
            }

        // Try the AM 70 method, if failed try the AM 63 method
        String cryptUtil = null;
    	String cryptUtilError = null;
        String encryptionKey = System.getProperty(STR_ENCRYPTION_KEY_PROP_KEY);

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
            String[] commandArray = { cryptUtil, passwordText, encryptionKey };
            StringBuffer output = new StringBuffer();
            int sts = ExecuteCommand.executeCommand(
                commandArray,
                null,
                output);

            if (output != null) {
                Debug.log(
                    "EncryptTask.getEncryptedAppPassword() command "
                    + "returned =" + output);
                encryptedText = output.toString();
            }

    	} catch (Exception ex) {
            Debug.log("EncryptionHandler.getEncryptedAppPassword() - " +
                "failed to invoke method with exception :", ex);
            cryptUtilError = ex.toString();
    	}

        if (cryptUtilError != null) {  
       	    throw new InstallException(
                LocalizedMessage.get(LOC_TSK_ERR_CRYPT_UTIL));
        } else if (encryptedText == null || 
        		encryptedText.trim().length() == 0) {
       	    throw new InstallException(
                LocalizedMessage.get(LOC_TSK_ERR_INVALID_APP_SSO_PASSWORD));
        }

            
            if (encryptedText != null && encryptedText.trim().length() > 0) {
                Console.println();
                Console.println(
                        LocalizedMessage.get(LOC_HR_MSG_ENCRYPT_RESULT),
                        new Object[] { encryptedText });
                result = true;
                
            }
        } catch (Exception ex) {
            Debug.log("EncryptionHandler.handleRequest: "
                    + "failed with exception", ex);
        } finally {
            if (instream != null) {
                try {
                    instream.close();
                } catch (Exception ex) {
                    Debug.log("EncryptionHandler.handleRequest: "
                            + "Failed to close file inputstream", ex);
                }
            }
        }            
        
        if (!result) {
            Console.println();
            Console.println(LocalizedMessage.get(LOC_HR_ERR_ENCRYPT_FAILED));
        }
    }

    public void displayHelp() {
        Console.println();        
        Console.println(LocalizedMessage.get(LOC_HR_MSG_ENCRYPT_USAGE_DESC));        
        Console.println();
        Console.println(LocalizedMessage.get(LOC_HR_MSG_ENCRYPT_USAGE_HELP));
        Console.println();
    }
    
    private String getPasswordText(String filePath) {
        String result = null;
        if (isReadableFile(filePath)) {
            BufferedReader reader = null;
            ArrayList lines = new ArrayList();
            try {
                String nextLine = null;
                reader = new BufferedReader(new FileReader(filePath));
                while ((nextLine = reader.readLine()) != null) {
                    if (nextLine.trim().length() > 0) {
                        lines.add(nextLine);
                    }
                }
                
                if (lines.size() == 1) {
                    result = (String) lines.get(0);
                } else {
                    Debug.log("EncryptionHandler.getPasswordText: "
                            + "Invalid number of text lines in the file: "
                            + filePath);
                }
            } catch (Exception ex) {
                Debug.log("EncryptionHandler.getPasswordText: "
                        + "Validation failed with exception", ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ex) {
                        Debug.log("EncryptionHandler.getPasswordText: "
                                + "Failed to close file reader", ex);
                    }
                }
            }
        } else {
            Debug.log("EncryptionHandler.getPasswordText: "
                    + "file not readable");
        }        
        
        return result;
    }
    
    private boolean validateAgentIdentifier(String id) {
        boolean result = true;
        Debug.log("EncryptionHandler.validateAgentIdenfier: id = " + id);
        if (!isReadableFile(getAgentConfigFilePath(id))) 
        {
            result = false;
            Debug.log("EncryptionHandler.validateAgentIdenfier: "
                    + "Invalid agent identifier specified");
        }
        
        return result;
    }
    
    private boolean isReadableFile(String filePath) {
        boolean result = true;
        File file = new File(filePath);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            result = false;            
        }
        
        Debug.log("EncryptionHandler.isReadableFile(" 
                + filePath + ") : readable = " + result);
        
        return result;
    }
    
    
    private String getAgentConfigFilePath(String agentId) {
        String path =  ConfigUtil.getHomePath() 
			+ FILE_SEP + agentId + FILE_SEP + INSTANCE_CONFIG_DIR_NAME 
			+ FILE_SEP + AGENT_CONFIG_FILE_NAME;
        
        Debug.log("EncryptionHandler.getAgentConfigFilePath: path = " + path);
        return path;
    }

    public static final String LOC_HR_MSG_ENCRYPT_USAGE_DESC= 
        "HR_MSG_ENCRYPT_USAGE_DESC";
 
    public static final String LOC_HR_MSG_ENCRYPT_USAGE_HELP= 
        "HR_MSG_ENCRYPT_USAGE_HELP";
    
    public static final String LOC_HR_ERR_ENCRYPT_ARGS =
        "HR_ERR_ENCRYPT_ARGS";
    
    public static final String LOC_HR_ERR_ENCRYPT_AGENT_ID =
        "HR_ERR_ENCRYPT_AGENT_ID";
    
    public static final String LOC_HR_ERR_ENCRYPT_PASSWORD = 
        "HR_ERR_ENCRYPT_PASSWORD";
    
    public static final String LOC_HR_MSG_ENCRYPT_RESULT =
        "HR_MSG_ENCRYPT_RESULT";
    
    public static final String LOC_HR_ERR_ENCRYPT_FAILED =
        "HR_ERR_ENCRYPT_FAILED";
    
    public static final String LOC_TSK_ERR_ENCRYPT_PASSWORD_INVOKE_METHOD = 
        "TSK_ERR_ENCRYPT_PASSWORD_INVOKE_METHOD";
    
    public static final String STR_ENCRYPT_LOCAL_FUNCTION = "encryptLocal";
    public static final String STR_ENCRYPT_FUNCTION = "encrypt";
    public static final String AGENT_CONFIG_FILE_NAME = "OpenSSOAgentBootstrap.properties";
    public static final String STR_ENCRYPTION_KEY_PROP_KEY = 
        "com.sun.identity.agents.config.key";
    
    // crypt_util binary for unix platforms
    private static final String STR_UNIX_CRYPT_UTIL = "crypt_util";

    // cryptit binary for windows platform
    private static final String STR_WIN_CRYPT_UTIL = "cryptit.exe";

    private static final String LOC_TSK_ERR_INVALID_APP_SSO_PASSWORD = 
        "TSK_ERR_INVALID_APP_SSO_PASSWORD";

    private static final String LOC_TSK_ERR_CRYPT_UTIL = 
        "TSK_ERR_CRYPT_UTIL";

}
