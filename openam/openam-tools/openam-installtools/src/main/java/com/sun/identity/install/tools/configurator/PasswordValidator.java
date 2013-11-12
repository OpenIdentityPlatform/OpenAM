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
 * $Id: PasswordValidator.java,v 1.6 2008/08/04 19:29:27 huacui Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */
package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.RESTUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;


public class PasswordValidator extends ValidatorBase {
    
    public PasswordValidator() throws InstallException {
        super();
    }
    
    public ValidationResult isPasswordValid(String passfileName, Map props,
            IStateAccess state) {
        
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        BufferedReader br = null;
        
        try {
            br = new BufferedReader(new FileReader(passfileName));
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                if (lineCount == 0) {
                    lineCount++;
                    int passLen = line.length();
                    String minSize = (String) props.get(STR_VAL_MIN_DIGITS);
                    String maxSize = (String) props.get(STR_IN_MAX_DIGITS);
                    
                    if ((minSize != null) && (minSize.length() > 0)
                    && (maxSize != null) && (maxSize.length() > 0)) {
                        
                        int minLen = Integer.parseInt(minSize);
                        int maxLen = Integer.parseInt(maxSize);
                        
                        Debug.log("PasswordValidator : Password : "
                                + " Min length = " + minLen + ", "
                                + "Max Length = " + maxLen);
                        if (maxLen > minLen) {
                            if ((passLen >= minLen) && (passLen <= maxLen)) {
                                validRes =
                                        ValidationResultStatus.STATUS_SUCCESS;
                                
                            } else {
                                Debug.log("PasswordValidator : Length of "
                                        + "password field is invalid");
                            }
                        }
                    } else {
                        // min and max not present; so if length of pass > 0
                        // it will be valid pass
                        validRes = ValidationResultStatus.STATUS_SUCCESS;
                        Debug.log("Password entry is valid");
                    }
                } else {
                    Debug.log("PasswordValidator : Invalid password file"
                            + " format, file had more than one line.");
                    validRes = ValidationResultStatus.STATUS_FAILED;
                    break;
                }
            }
        } catch (Exception ex) {
            Debug.log("PasswordValidator : Failed to read password with ex :",
                    ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    Debug.log("PasswordValidator : Failed to close "
                            + "password file :", ex);
                }
            }
        }
        
        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_PASS);
        }
        
        Debug.log("PasswordValidator : Is password valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }
    
    /*
     * Checks if Agent profile/User's name&password is valid.
     *
     * @param port @param props @param state
     *
     * @return ValidationResult
     */
    
    public ValidationResult isAgentLoginValid(String passfileName, Map props,
            IStateAccess state) throws InstallException {
        
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        String agentUserName = null;
        String serverURL = null;
        String restAuthURL = null;
        String str = null;
        
        String serverURLValid = (String)state.get("isServerURLValid");
        String createAgentProfileKey = (String)state.get(
                InstallConstants.STR_CREATE_AGENT_PROFILE_KEY);
        if (createAgentProfileKey == null) {
            setCreateAgentProfile(state, "false");
        }

        if (serverURLValid != null && serverURLValid.equals("true")) {
            
            if (isAgentAdmin(props, state)) {
                return isAgentAdminLoginValid(passfileName, props, state);
            }
            
            String agentUserNameKey = (String)props.get(
                    STR_AGENT_PROFILE_LOOKUP_KEY);
            if (agentUserNameKey != null) {
                agentUserName = (String) state.get(agentUserNameKey);
            }
            
            Map tokens = state.getData();
            if (tokens.containsKey("AM_SERVER_URL")) {
                serverURL = (String) tokens.get("AM_SERVER_URL");
            }
            
            String agentUserPasswd = readDataFromFile(passfileName);
            try {
                restAuthURL = serverURL +
                        "/identity/authenticate";
                String encodingType = "UTF-8";
                String encodedPostData = "username="
                        + URLEncoder.encode(agentUserName, encodingType)
                        + "&password="
                        + URLEncoder.encode(agentUserPasswd, encodingType)
                        + "&uri=" + URLEncoder.encode("realm=/&module=Application", encodingType);
                RESTUtils.RESTResponse response = RESTUtils.callServiceURL(
                        restAuthURL, encodedPostData);
                
                // Read response code
                int responseCode = response.getResponseCode();
                if (responseCode == HTTP_RESPONSE_OK) {
                    validRes = ValidationResultStatus.STATUS_SUCCESS;
                    setCreateAgentProfile(state, "false");
                    
                } else if (responseCode ==
                        HTTP_RESPONSE_AUTHENTICATION_FAILED) {
                    String responseStr = response.toString();
                    if (responseStr.indexOf(
                            STR_IDENTITY_INVALID_PASSWORD) > 0) {
                        // wrong password
                        returnMessage = LocalizedMessage.get(
                                LOC_VA_WRN_IN_VAL_AGENT_PASSWORD,
                                new Object[] {agentUserName});
                        
                    } else if (responseStr.indexOf(
                                STR_IDENTITY_INVALID_CREDENTIALS) > 0) {
                        // create agent profile only when agent profile 
                        // does not exist.
                        setCreateAgentProfile(state, "true");
                        returnMessage = LocalizedMessage.get(
                                LOC_VA_WRN_IN_VAL_AGENT_PROFILE_NOT_PRESENT,
                                new Object[] {agentUserName});
                        validRes = ValidationResultStatus.STATUS_WARNING;
                        
                    } else {
                        Debug.log("PasswordValidator.isAgentLoginValid() - " +
                            "Error from OpenSSO:" + response.toString());
                        returnMessage = LocalizedMessage.get(
                                LOC_VA_WRN_IN_VAL_OTHER_AGENT_AUTH_FAILURE,
                                new Object[] {Integer.valueOf(responseCode)});
                        validRes = ValidationResultStatus.STATUS_WARNING;
                    }
                    
                } else {
                    Debug.log("PasswordValidator.isAgentLoginValid() - " +
                            "Error from OpenSSO:" + response.toString());
                    returnMessage = LocalizedMessage.get(
                            LOC_VA_WRN_IN_VAL_AGENT_GENERIC_FAILURE,
                            new Object[] {Integer.valueOf(responseCode)});
                    validRes = ValidationResultStatus.STATUS_WARNING;
                }
                
            } catch (UnknownHostException uhe) {
                Debug.log("PasswordValidator.isAgentLoginValid() " +
                        "threw exception :",
                        uhe);
                returnMessage = LocalizedMessage.get(
                        LOC_VA_WRN_UN_REACHABLE_FAM_SERVER_URL,
                        new Object[] { restAuthURL });
                validRes = ValidationResultStatus.STATUS_WARNING;
            } catch (ConnectException ce) {
                Debug.log("PasswordValidator.isAgentLoginValid() " +
                        "threw exception :",
                        ce);
                returnMessage = LocalizedMessage.get(
                        LOC_VA_WRN_UN_REACHABLE_FAM_SERVER_URL,
                        new Object[] { restAuthURL });
                validRes = ValidationResultStatus.STATUS_WARNING;
            } catch (FileNotFoundException ex) {
                Debug.log("PasswordValidator.isAgentLoginValid() " +
                        "threw exception :",
                        ex);
                returnMessage = LocalizedMessage.get(
                        LOC_VA_WRN_NOT_FOUND_SERVER_URL,
                        new Object[] { restAuthURL });
                validRes = ValidationResultStatus.STATUS_WARNING;
            } catch (IOException ex) {
                Debug.log("PasswordValidator.isAgentLoginValid() " +
                        "threw exception :", ex);
            }
            return new ValidationResult(validRes, null, returnMessage);
            
        } else {
           
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_SERVER_URL_NOT_RUNNING,
                    new Object[] { serverURL });
            validRes = ValidationResultStatus.STATUS_WARNING;
            return new ValidationResult(validRes, null, returnMessage);
        }
    }

    /*
     * validate Agent Administrator's name and password.
     */
    private ValidationResult isAgentAdminLoginValid(String passfileName, 
            Map props, IStateAccess state) throws InstallException {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        String agentUserName = null;
        String serverURL = null;
        String restAuthURL = null;
        
        String agentUserNameKey = (String)
        props.get(STR_AGENT_PROFILE_LOOKUP_KEY);
        if (agentUserNameKey != null) {
            agentUserName = (String) state.get(agentUserNameKey);
        }
        
        Map tokens = state.getData();
        if (tokens.containsKey("AM_SERVER_URL")) {
            serverURL = (String) tokens.get("AM_SERVER_URL");
        }
        
        String agentUserPasswd = readDataFromFile(passfileName);
        try {
            restAuthURL = serverURL +
                    "/identity/authenticate";
            String encodingType = "UTF-8";
            String encodedPostData = "username="
                    + URLEncoder.encode(agentUserName, encodingType)
                    + "&password="
                    + URLEncoder.encode(agentUserPasswd, encodingType)
                    + "&uri=" + URLEncoder.encode("realm=/&module=Application", encodingType);
            RESTUtils.RESTResponse response = RESTUtils.callServiceURL(
                    restAuthURL, encodedPostData);
            
            // Read response code
            int responseCode = response.getResponseCode();
            if (responseCode == HTTP_RESPONSE_OK) {
                validRes = ValidationResultStatus.STATUS_SUCCESS;
                setCreateAgentProfile(state, "true");

            } else if (responseCode ==
                    HTTP_RESPONSE_AUTHENTICATION_FAILED) {
                
                String responseStr = response.toString();
                if (responseStr.indexOf(
                        STR_IDENTITY_INVALID_PASSWORD) > 0) {
                    // wrong password
                    returnMessage = LocalizedMessage.get(
                            LOC_VA_WRN_IN_VAL_AGENT_PASSWORD,
                            new Object[] {agentUserName});
                    
                } else if (responseStr.indexOf(
                        STR_IDENTITY_INVALID_CREDENTIALS) > 0) {
                    // user does not exist.
                    returnMessage = LocalizedMessage.get(
                            LOC_VA_ERR_IN_VAL_AGENT_PROFILE_NOT_PRESENT,
                            new Object[] {agentUserName});
                    
                } else {
                    Debug.log("PasswordValidator.isAgentAdminLoginValid() - " +
                            "Error from OpenSSO:" + response.toString());
                    returnMessage = LocalizedMessage.get(
                            LOC_VA_ERR_IN_VAL_OTHER_AGENT_AUTH_FAILURE,
                            new Object[] {Integer.valueOf(responseCode)});
                }
                
            } else {
                Debug.log("PasswordValidator.isAgentAdminLoginValid() - " +
                        "Error from OpenSSO:" + response.toString());
                returnMessage = LocalizedMessage.get(
                        LOC_VA_ERR_IN_VAL_AGENT_GENERIC_FAILURE,
                        new Object[] {Integer.valueOf(responseCode)});
            }
            
        } catch (UnknownHostException uhe) {
            Debug.log("PasswordValidator.isAgentAdminLoginValid() " +
                    "threw exception :",
                    uhe);
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_UN_REACHABLE_FAM_SERVER_URL,
                    new Object[] { restAuthURL });
        } catch (ConnectException ce) {
            Debug.log("PasswordValidator.isAgentAdminLoginValid() " +
                    "threw exception :",
                    ce);
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_UN_REACHABLE_FAM_SERVER_URL,
                    new Object[] { restAuthURL });
        } catch (FileNotFoundException ex) {
            Debug.log("PasswordValidator.isAgentAdminLoginValid() " +
                    "threw exception :",
                    ex);
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_NOT_FOUND_SERVER_URL,
                    new Object[] { restAuthURL });
        } catch (IOException ex) {
            Debug.log("PasswordValidator.isAgentAdminLoginValid() " +
                    "threw exception :", ex);
        }
        return new ValidationResult(validRes, null, returnMessage);
        
    }

    /*
     * check if the user to be validated is Agent admin or not.
     */
    private boolean isAgentAdmin(Map props, IStateAccess state) {
        String agentUserName = null;
        String agentUserNameKey = (String) props.get(
                STR_AGENT_PROFILE_LOOKUP_KEY);
        if (agentUserNameKey != null) {
            agentUserName = (String) state.get(agentUserNameKey);
        }
        
        String agentAdminName = (String)state.get(
                STR_AGENT_ADMIN_LOOKUP_KEY);
        if ((agentAdminName == null) ||
                !agentUserName.equalsIgnoreCase(agentAdminName)) {
            return false;
            
        } else {
            return true;
        }
    }
    
    /*
     * set flag indicating if agent profile will be created or not.
     */
    private void setCreateAgentProfile(IStateAccess state, String status) {
        
        state.put(InstallConstants.STR_CREATE_AGENT_PROFILE_KEY, status);
        state.put(InstallConstants.STR_CREATE_AGENT_PROFILE_NAME, status);
    }
    
    /*
     * read user's password from password file.
     */
    private String readDataFromFile(String fileName) throws InstallException {
        String firstLine = null;
        BufferedReader br = null;
        
        try {
            FileInputStream fis = new FileInputStream(fileName);
            InputStreamReader fir = new InputStreamReader(fis);
            br = new BufferedReader(fir);
            firstLine = br.readLine();
        } catch (Exception e) {
            Debug.log("PasswordValidator.readDataFromFile() - Error " +
                    "reading file - " + fileName, e);
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
    
    public void initializeValidatorMap() throws InstallException {
        
        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };
        
        try {
            getValidatorMap().put("VALID_PASSWORD",
                    this.getClass().getMethod("isPasswordValid", paramObjs));
            
            getValidatorMap().put("VALIDATE_AGENT_PASSWORD",
                    this.getClass().getMethod("isAgentLoginValid", paramObjs));
            
        } catch (NoSuchMethodException nsme) {
            Debug.log("PasswordValidator: "
                    + "NoSuchMethodException thrown while loading method :",
                    nsme);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("PasswordValidator: "
                    + "SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("PasswordValidator: "
                    + "Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
        }
        
    }
    
    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();
    
    /*
     * Localized messages
     */
    public static String LOC_VA_WRN_IN_VAL_PASS = "VA_WRN_IN_VAL_PASS";
    public static final String LOC_TK_ERR_PASSWD_FILE_READ =
            "TSK_ERR_PASSWD_FILE_READ";
    public static String LOC_VA_WRN_UN_REACHABLE_FAM_SERVER_URL =
            "VA_WRN_UN_REACHABLE_FAM_SERVER_URL";
    public static String LOC_VA_WRN_SERVER_URL_NOT_RUNNING =
            "VA_WRN_SERVER_URL_NOT_RUNNING";
    public static String LOC_VA_WRN_NOT_FOUND_SERVER_URL =
            "VA_WRN_NOT_FOUND_SERVER_URL";

    public static String LOC_VA_WRN_IN_VAL_AGENT_PASSWORD =
            "VA_WRN_IN_VAL_AGENT_PASSWORD";
    public static String LOC_VA_WRN_IN_VAL_AGENT_PROFILE_NOT_PRESENT =
            "VA_WRN_IN_VAL_AGENT_PROFILE_NOT_PRESENT";
    public static String LOC_VA_ERR_IN_VAL_AGENT_PROFILE_NOT_PRESENT =
            "VA_ERR_IN_VAL_AGENT_PROFILE_NOT_PRESENT";
    
    public static String LOC_VA_WRN_IN_VAL_OTHER_AGENT_AUTH_FAILURE =
            "VA_WRN_IN_VAL_OTHER_AGENT_AUTH_FAILURE";
    public static String LOC_VA_ERR_IN_VAL_OTHER_AGENT_AUTH_FAILURE =
            "VA_ERR_IN_VAL_OTHER_AGENT_AUTH_FAILURE";
    public static String LOC_VA_WRN_IN_VAL_AGENT_GENERIC_FAILURE =
            "VA_WRN_IN_VAL_AGENT_GENERIC_FAILURE";
    public static String LOC_VA_ERR_IN_VAL_AGENT_GENERIC_FAILURE =
            "VA_ERR_IN_VAL_AGENT_GENERIC_FAILURE";

    // lookup keys
    public static final String STR_AGENT_PROFILE_LOOKUP_KEY =
            "AGENT_PROFILE_LOOKUP_KEY";
    public static final String STR_AGENT_ADMIN_LOOKUP_KEY =
            "AGENT_ADMINISTRATOR_NAME";
    
    /*
     * String constants
     */
    public static String STR_VAL_MIN_DIGITS = "minLen";
    public static String STR_IN_MAX_DIGITS = "maxLen";
    
    public static final int HTTP_RESPONSE_OK = 200;
    public static final int HTTP_RESPONSE_GENERIC_FAILURE = 500;
    public static final int HTTP_RESPONSE_AUTHENTICATION_FAILED = 401;

    public static final String STR_IDENTITY_INVALID_PASSWORD =
            "InvalidPassword";
    public static final String STR_IDENTITY_INVALID_CREDENTIALS =
            "InvalidCredentials";
}
