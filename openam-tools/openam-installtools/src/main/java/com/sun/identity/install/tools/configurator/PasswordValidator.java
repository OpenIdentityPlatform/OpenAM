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
 * Portions Copyrighted 2010-2016 ForgeRock Inc
 */
package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.RESTEndpoint;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ConnectException;
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

        try (BufferedReader br = new BufferedReader(new FileReader(passfileName))) {
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
                                validRes = ValidationResultStatus.STATUS_SUCCESS;

                            } else {
                                Debug.log("PasswordValidator : Length of password field is invalid");
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
            Debug.log("PasswordValidator : Failed to read password with ex :", ex);
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED) {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_PASS);
        }
        Debug.log("PasswordValidator : Is password valid ? " + validRes.isSuccessful());

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
        String serverURLValid = (String)state.get("isServerURLValid");
        String createAgentProfileKey = (String)state.get(InstallConstants.STR_CREATE_AGENT_PROFILE_KEY);

        if (createAgentProfileKey == null) {
            setCreateAgentProfile(state, "false");
        }

        Map tokens = state.getData();
        if (tokens.containsKey("AM_SERVER_URL")) {
            serverURL = (String) tokens.get("AM_SERVER_URL");
        }

        // bail if server URL is not valid
        //
        if (serverURLValid == null || !serverURLValid.equals("true")) {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_SERVER_URL_NOT_RUNNING,
                    new Object[] { (String) tokens.get("AM_SERVER_URL") } );
            validRes = ValidationResultStatus.STATUS_WARNING;
            return new ValidationResult(validRes, null, returnMessage);
        }

        if (isAgentAdmin(props, state)) {
            return isAgentAdminLoginValid(passfileName, props, state);
        }

        String agentUserNameKey = (String)props.get(STR_AGENT_PROFILE_LOOKUP_KEY);
        if (agentUserNameKey != null) {
            agentUserName = (String) state.get(agentUserNameKey);
        }

        String agentUserPasswd = readDataFromFile(passfileName);
        RESTEndpoint restEndpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(serverURL)
                .path(RESTEndpoint.AUTHENTICATION_URI)
                .post()
                .parameter("noSession", "true")
                .addModuleParameters()
                .headers("Content-Type", "application/json")
                .headers("X-OpenAM-Username", agentUserName)
                .headers("X-OpenAM-Password", agentUserPasswd)
                .apiVersion(RESTEndpoint.AUTHENTICATION_URI_API_VERSION)
                .build();

        int responseCode;
        try {
            Debug.log("About to validate agent username: " + agentUserName);
            Debug.log("Details of REST call: " + restEndpoint.toString());

            RESTEndpoint.RESTResponse response = restEndpoint.call();

            // Read response code
            responseCode = response.getResponseCode();
            if (responseCode == HTTP_RESPONSE_OK) {
                validRes = ValidationResultStatus.STATUS_SUCCESS;
                setCreateAgentProfile(state, "false");
                Debug.log("Auth success with " + agentUserName + ": switching create agent profile to false");

            } else if (responseCode == HTTP_RESPONSE_AUTHENTICATION_FAILED) {
                // the new JSON endpoint doesn't leak as much information as the old /identify/authenticate one -
                // leaked information that was useful at this point.  All we can do is try to go ahead and create
                // the agent profile.  This may fail, but we'll have to report that when it happens.
                //
                Debug.log("Auth failed with " + agentUserName + ": switching create agent profile to true");
                validRes = ValidationResultStatus.STATUS_WARNING;
                setCreateAgentProfile(state, "true");

            } else {
                Debug.log("PasswordValidator.isAgentLoginValid() "
                        + "- Unexpected response code from JSON authenticate endpoint: "
                        + responseCode
                        + ": message" + response.toString());
                returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_AGENT_GENERIC_FAILURE,
                        new Object[] {Integer.valueOf(responseCode)});
                validRes = ValidationResultStatus.STATUS_WARNING;
            }

        } catch (UnknownHostException|ConnectException uhe) {
            Debug.log("PasswordValidator.isAgentLoginValid() threw exception :", uhe);
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_UN_REACHABLE_FAM_SERVER_URL,
                    new Object[] { restEndpoint.getPath() });
            validRes = ValidationResultStatus.STATUS_WARNING;
        } catch (FileNotFoundException ex) {
            Debug.log("PasswordValidator.isAgentLoginValid() threw exception :", ex);
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_NOT_FOUND_SERVER_URL,
                    new Object[] { restEndpoint.getPath() });
            validRes = ValidationResultStatus.STATUS_WARNING;
        } catch (IOException ex) {
            Debug.log("PasswordValidator.isAgentLoginValid() threw exception :", ex);
        }
        return new ValidationResult(validRes, null, returnMessage);
    }

    /*
     * Much of this code seems to have been duplicated from above, the only difference being the key used to index
     * the properties, i.e. props.get(STR_AGENT_PROFILE_LOOKUP_KEY)
     *
     * TODO Refactor to remove duplication
     *
     * validate Agent Administrator's name and password.
     */
    private ValidationResult isAgentAdminLoginValid(String passfileName,
            Map props, IStateAccess state) throws InstallException {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        String agentAdminUserName = null;
        String serverURL = null;

        String agentUserNameKey = (String) props.get(STR_AGENT_PROFILE_LOOKUP_KEY);
        if (agentUserNameKey != null) {
            agentAdminUserName = (String) state.get(agentUserNameKey);
        }

        Map tokens = state.getData();
        if (tokens.containsKey("AM_SERVER_URL")) {
            serverURL = (String) tokens.get("AM_SERVER_URL");
        }

        String agentAdminUserPasswd = readDataFromFile(passfileName);

        RESTEndpoint restEndpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(serverURL)
                .path(RESTEndpoint.AUTHENTICATION_URI)
                .post()
                .parameter("noSession", "true")
                .addModuleParameters()
                .headers("Content-Type", "application/json")
                .headers("X-OpenAM-Username", agentAdminUserName)
                .headers("X-OpenAM-Password", agentAdminUserPasswd)
                .apiVersion(RESTEndpoint.AUTHENTICATION_URI_API_VERSION)
                .build();

        try {
            Debug.log("About to validate username agent admin: " + agentAdminUserName);
            Debug.log("Details of REST call: " + restEndpoint.toString());

            RESTEndpoint.RESTResponse response = restEndpoint.call();

            // Read response code
            int responseCode = response.getResponseCode();
            if (responseCode == HTTP_RESPONSE_OK) {
                validRes = ValidationResultStatus.STATUS_SUCCESS;
                // All is good.  If we want to create the agent profile, we can go ahead.
                // It may be, of course, that we don't because the profile already exists.
                // We will have determined that in the function above.
                Debug.log("Auth success for " + agentAdminUserName);

            } else if (responseCode == HTTP_RESPONSE_AUTHENTICATION_FAILED) {

                Debug.log("Auth FAILED for " + agentAdminUserName + ": cannot create agent profile");
                setCreateAgentProfile(state, "false");
                validRes = ValidationResultStatus.STATUS_WARNING;

                // user does not exist or password incorrect
                returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_AGENT_CREDENTIALS,
                        new Object[] {agentAdminUserName});

            } else {
                Debug.log("PasswordValidator.isAgentAdminLoginValid() - Error from OpenSSO:" + response.toString());
                returnMessage = LocalizedMessage.get(LOC_VA_ERR_IN_VAL_AGENT_GENERIC_FAILURE,
                        new Object[] {Integer.valueOf(responseCode)});
            }

        } catch (UnknownHostException uhe) {
            Debug.log("PasswordValidator.isAgentAdminLoginValid() threw exception :", uhe);
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_UN_REACHABLE_FAM_SERVER_URL,
                    new Object[] { restEndpoint.getPath() });
        } catch (FileNotFoundException ex) {
            Debug.log("PasswordValidator.isAgentAdminLoginValid() threw exception :", ex);
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_NOT_FOUND_SERVER_URL,
                    new Object[] { restEndpoint.getPath() });
        } catch (IOException ex) {
            Debug.log("PasswordValidator.isAgentAdminLoginValid() threw exception :", ex);
        }
        return new ValidationResult(validRes, null, returnMessage);

    }

    /*
     * check if the user to be validated is Agent admin or not.
     */
    private boolean isAgentAdmin(Map props, IStateAccess state) {
        String agentUserName = null;
        String agentUserNameKey = (String) props.get(STR_AGENT_PROFILE_LOOKUP_KEY);

        if (agentUserNameKey != null) {
            agentUserName = (String) state.get(agentUserNameKey);
        }

        String agentAdminName = (String)state.get(STR_AGENT_ADMIN_LOOKUP_KEY);
        if (agentAdminName == null || !agentUserName.equalsIgnoreCase(agentAdminName)) {
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
            Debug.log("PasswordValidator.readDataFromFile() - Error reading file - " + fileName, e);
            throw new InstallException(LocalizedMessage.get(LOC_TK_ERR_PASSWD_FILE_READ), e);
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
            Debug.log("PasswordValidator: NoSuchMethodException thrown while loading method :", nsme);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("PasswordValidator: SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("PasswordValidator: Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
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

    public static String LOC_VA_WRN_IN_VAL_AGENT_GENERIC_FAILURE =
            "VA_WRN_IN_VAL_AGENT_GENERIC_FAILURE";
    public static String LOC_VA_ERR_IN_VAL_AGENT_GENERIC_FAILURE =
            "VA_ERR_IN_VAL_AGENT_GENERIC_FAILURE";
    public static String LOC_VA_WRN_IN_VAL_AGENT_CREDENTIALS =
            "VA_WRN_IN_VAL_AGENT_CREDENTIALS";

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
