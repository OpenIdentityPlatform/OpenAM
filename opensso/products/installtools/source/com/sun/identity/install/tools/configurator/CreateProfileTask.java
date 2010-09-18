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
 * $Id: CreateProfileTask.java,v 1.5 2008/08/19 19:13:02 veiming Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.RESTUtils;

/**
 * This class provides the feature of creating Agent Profile during installation
 * when the profile does not exist in OpenSSO.
 */
public class CreateProfileTask implements ITask, InstallConstants {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        
        boolean result = false;
        
        // check if skipping creating agent profile is needed.
        String createAgentProfile = (String)stateAccess.get(
                InstallConstants.STR_CREATE_AGENT_PROFILE_NAME);
        if (createAgentProfile == null ||
                createAgentProfile.equalsIgnoreCase("false")) {
            Debug.log("CreateProfileTask.execute() - " +
                    "Agent profile will not be created");
            return true;
        }
        Debug.log("CreateProfileTask.execute() - " +
                "Agent profile will be created");
        try {
            
            RESTUtils.RESTResponse response = createAgentProfile(stateAccess,
                    properties);
            if (response.getResponseCode() != HTTP_RESPONSE_OK) {
                Debug.log("CreateProfileTask.execute() - " +
                        "failed to create agent profile. response code = " +
                        response.getResponseCode());
                Debug.log("CreateProfileTask.execute() - " +
                        "failed to create agent profile. response message = " +
                        response.toString());
                return false;
                
            } else {
                String agentUserName = (String)stateAccess.get(
                        STR_AGENT_PROFILE_NAME);
                Debug.log("CreateProfileTask.execute() -" +
                        " Agent profile:" + agentUserName  +
                        " was created successfully!");
                result = true;
            }
        } catch (Exception ex) {
            Debug.log("CreateProfileTask.execute(): failed!", ex);
        }
        
        return result;
    }
    
    /*
     * create agent profile.
     * @param stateAccess
     */
    private RESTUtils.RESTResponse createAgentProfile(IStateAccess stateAccess,
            Map properties) throws Exception {
        
        String agentUserName = (String)stateAccess.get(STR_AGENT_PROFILE_NAME);
        String agentUserPasswordFile = (String)stateAccess.get(
                STR_AGENT_PASSWORD_FILE);
        String agentUserPassword = null;
        
        if (agentUserName == null || agentUserPasswordFile == null) {
            Debug.log("CreateProfileTask.createAgentProfile() - " +
                    "Agent profile's name or password file is null");
            return null;
        }
        
        String agentAdminName = (String)stateAccess.get(
                STR_AGENT_ADMINISTRATOR_NAME);
        String agentAdminPasswordFile = (String)stateAccess.get(
                STR_AGENT_ADMINISTRATOR_PASSWORD_FILE);
        String agentAdminPassword = null;
        String agentProfileType = (String)properties.get(
                STR_AGENT_PROFILE_TYPE);
        String ssoToken = null;
        String serverURL = (String) stateAccess.get(STR_AM_SERVER_URL);
        String agentURL = (String) stateAccess.get(STR_AGENT_URL);
        
        if (agentAdminName == null || agentAdminPasswordFile == null ||
                agentProfileType == null) {
            Debug.log("CreateProfileTask.createAgentProfile() - " +
                    "Agent Administrator's " +
                    "name, password file or Agent Profile's type is null");
            return null;
        }
        
        agentUserPassword = readDataFromFile(agentUserPasswordFile);
        agentAdminPassword = readDataFromFile(agentAdminPasswordFile);
        if (agentUserPassword == null || agentAdminPassword == null) {
            Debug.log("CreateProfileTask.createAgentProfile() - " +
                    "Agent Administrator's password or Agent profile's " +
                    "password is null");
            return null;
        }
        
        // get Agent Administrator's sso token
        String authURL = serverURL + AUTHENTICATION_URI;
        ssoToken = getSSOToken(authURL, agentAdminName, agentAdminPassword);
        if (ssoToken == null) {
            Debug.log("CreateProfileTask.createAgentProfile() - " +
                    "can not create Agent Administrator's sso token.");
            return null;
        }
        
        // create the post data for creating agent profile.
        StringBuffer buffer = new StringBuffer();
        buffer.append("identity_name=" +
                URLEncoder.encode(agentUserName, ENCODING_TYPE));
        buffer.append("&identity_attribute_names=userpassword");
        buffer.append("&identity_attribute_values_userpassword=" +
                URLEncoder.encode(agentUserPassword, ENCODING_TYPE));
        buffer.append("&identity_type=AgentOnly");
        buffer.append("&identity_attribute_names=AgentType");
        buffer.append("&identity_attribute_values_AgentType=" +
                agentProfileType);
        buffer.append("&identity_attribute_names=AGENTURL");
        buffer.append("&identity_attribute_values_AGENTURL=" +
                URLEncoder.encode(agentURL, ENCODING_TYPE));
        buffer.append("&identity_attribute_names=SERVERURL");
        buffer.append("&identity_attribute_values_SERVERURL=" +
                URLEncoder.encode(serverURL, ENCODING_TYPE));
        buffer.append("&admin=" + URLEncoder.encode(ssoToken, ENCODING_TYPE));
        
        // call profile serivce to create agent profile.
        String profileURL = serverURL + CREATE_PROFILE_URI;
        Debug.log("CreateProfileTask.createAgentProfile() - REST URL: " +
                profileURL + " postData: " + buffer.toString());
        
        RESTUtils.RESTResponse response = RESTUtils.callServiceURL(
                profileURL,
                buffer.toString());
        
        return response;
        
    }
    
    /*
     * create one user's sso token.
     * @param serviceURL the URL to be called for authentication
     * @param usreName the name of the user whose sso token to be created
     * @param password the password of the user whose sso token to be created
     *
     * @return the String of sso token
     */
    private String getSSOToken(String serviceURL,
            String userName, String password) throws Exception {
        
        String encodedPostData = "username="  +
                URLEncoder.encode(userName, ENCODING_TYPE) +
                "&password=" +
                URLEncoder.encode(password, ENCODING_TYPE);
        RESTUtils.RESTResponse response = RESTUtils.callServiceURL(serviceURL,
                encodedPostData);
        Debug.log("CreateProfileTask.getSSOToken() - " +
                "Response code=" + response.getResponseCode());
        
        String ssoToken = null;
        if (response.getResponseCode() == HTTP_RESPONSE_OK) {
            ssoToken = (String)response.getContent().get(0);
            int index = ssoToken.indexOf("=");
            if (index >=0) {
                ssoToken = ssoToken.substring(index+1);
            }
        }
        Debug.log("CreateProfileTask.getSSOToken() - " +
                "SSO Token =" + ssoToken);
        
        return ssoToken;
    }
    
    /*
     * read password from the file.
     * @fileName the file name to be read from
     */
    private String readDataFromFile(String fileName) throws InstallException {
        String firstLine = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            firstLine = br.readLine();
            
        } catch (Exception ex) {
            Debug.log("CreateProfileTask.readDataFromFile() - Error " +
                    "reading file - " + fileName, ex);
            throw new InstallException(LocalizedMessage.get(
                    LOC_TK_ERR_PASSWD_FILE_READ), ex);
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
            Map properties) {
        String agentUserName = (String)stateAccess.get(STR_AGENT_PROFILE_NAME);
        Object[] args = { agentUserName };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CREATE_AGENT_PROFILE_EXECUTE, args);
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        // There is no roll back for uninstall tasks
        return null;
    }
    
    public boolean rollBack(String name, IStateAccess state, Map properties)
    throws InstallException {
        
        // Nothing to roll back during uninstall
        return true;
    }
    
    public static final String LOC_TSK_MSG_CREATE_AGENT_PROFILE_EXECUTE =
            "TSK_MSG_CREATE_AGENT_PROFILE_EXECUTE";
    
    public static final String STR_AM_SERVER_URL = "AM_SERVER_URL";
    
    public static final String STR_AGENT_URL = "AGENT_URL";
    
    public static final String STR_AGENT_PROFILE_TYPE = "AGENT_PROFILE_TYPE";
    
    public static final String STR_AGENT_ADMINISTRATOR_NAME =
            "AGENT_ADMINISTRATOR_NAME";
    
    public static final String STR_AGENT_ADMINISTRATOR_PASSWORD_FILE =
            "AGENT_ADMINISTRATOR_PASSWORD_FILE";
    
    public static final String STR_AGENT_PROFILE_NAME =
            "AGENT_PROFILE_NAME";
    
    public static final String STR_AGENT_PASSWORD_FILE =
            "AGENT_PASSWORD_FILE";
    
    public static final String LOC_TK_ERR_PASSWD_FILE_READ =
            "TSK_ERR_PASSWD_FILE_READ";
    
    public static final String AUTHENTICATION_URI = "/identity/authenticate";
    
    public static final String CREATE_PROFILE_URI = "/identity/create";
    
    public static final String ENCODING_TYPE = "UTF-8";
    
    public static final int HTTP_RESPONSE_OK = 200;
}
