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
 * Portions Copyrighted 2016 ForgeRock AS.
 */
package com.sun.identity.install.tools.configurator;

import static org.forgerock.json.JsonValue.*;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.RESTEndpoint;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.IOUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;

/**
 * This class provides the feature of creating Agent Profile during installation
 * when the profile does not exist in OpenSSO.
 */
public class CreateProfileTask implements ITask, InstallConstants {

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

    public static final String ENCODING_TYPE = "UTF-8";

    public static final int HTTP_RESPONSE_OK = 200;
    public static final int HTTP_RESPONSE_CREATED = 201;

    public boolean execute(String name, IStateAccess stateAccess, Map properties) throws InstallException {

        boolean result = false;

        // check if skipping creating agent profile is needed.
        String createAgentProfile = (String)stateAccess.get(InstallConstants.STR_CREATE_AGENT_PROFILE_NAME);
        if (createAgentProfile == null || createAgentProfile.equalsIgnoreCase("false")) {
            Debug.log("CreateProfileTask.execute() - Agent profile will not be created");
            return true;
        }
        Debug.log("CreateProfileTask.execute() - Agent profile will be created");
        try {
            RESTEndpoint.RESTResponse response = createAgentProfile(stateAccess, properties);
            int code = response.getResponseCode();
            if (code != HTTP_RESPONSE_OK && code != HTTP_RESPONSE_CREATED) {
                Debug.log("CreateProfileTask.execute() - FAILED to create agent profile. response code = " +
                        response.getResponseCode());
                Debug.log("CreateProfileTask.execute() - FAILED to create agent profile. response message = " +
                        response.toString());
                return false;

            } else {
                String agentUserName = (String)stateAccess.get(STR_AGENT_PROFILE_NAME);
                Debug.log("CreateProfileTask.execute() - Agent profile:"
                        + agentUserName  +
                        " was created successfully!");
                Debug.log("CreateProfileTask.execute() - In case you're interested, the response message = " +
                        response.toString());
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
    private RESTEndpoint.RESTResponse createAgentProfile(IStateAccess stateAccess, Map properties) throws Exception {

        String agentUserName = (String)stateAccess.get(STR_AGENT_PROFILE_NAME);
        String agentUserPasswordFile = (String)stateAccess.get(STR_AGENT_PASSWORD_FILE);
        String agentUserPassword = null;

        if (agentUserName == null || agentUserPasswordFile == null) {
            Debug.log("CreateProfileTask.createAgentProfile() - Agent profile's name or password file is null");
            return null;
        }

        String agentAdminName = (String)stateAccess.get(STR_AGENT_ADMINISTRATOR_NAME);
        String agentAdminPasswordFile = (String)stateAccess.get(STR_AGENT_ADMINISTRATOR_PASSWORD_FILE);
        String agentAdminPassword = null;
        String agentProfileType = (String)properties.get(STR_AGENT_PROFILE_TYPE);
        String ssoToken;
        String serverURL = (String) stateAccess.get(STR_AM_SERVER_URL);
        String agentURL = (String) stateAccess.get(STR_AGENT_URL);

        if (agentAdminName == null || agentAdminPasswordFile == null || agentProfileType == null) {
            Debug.log("CreateProfileTask.createAgentProfile() - Agent Administrator's " +
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

        String cookieName = getCookieName(serverURL);

        // get Agent Administrator's sso token
        ssoToken = getSSOToken(serverURL, RESTEndpoint.AUTHENTICATION_URI, agentAdminName, agentAdminPassword);
        if (ssoToken == null) {
            Debug.log("CreateProfileTask.createAgentProfile() - cannot create Agent Administrator's sso token.");
            return null;
        }

        JsonValue jsonPayload = json(object(
                field("username", agentUserName),
                field("userpassword", array(agentUserPassword)),
                field("agenttype", array(agentProfileType)),
                field("serverurl", array(serverURL)),
                field("agenturl", array(agentURL))
        ));

        RESTEndpoint restEndpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(serverURL)
                .path(RESTEndpoint.CREATE_PROFILE_URI)
                .post()
                .parameter("_action", RESTEndpoint.CREATE_PROFILE_URI_ACTION_VALUE)
                .addModuleParameters()
                .postData(jsonPayload.toString())
                .headers("Content-Type", "application/json")
                .headers(cookieName, ssoToken)
                .apiVersion(RESTEndpoint.CREATE_PROFILE_URI_API_VERSION)
                .build();

        Debug.log("CreateProfileTask.createAgentProfile() via endpoint " + restEndpoint.toString());

        return restEndpoint.call();
    }

    /*
     * Create a user SSO token by using the authenticate endpoint.
     * @param serviceURL the URL to be called for authentication
     * @param userName the name of the user
     * @param password the password of the user
     *
     * @return the SSO token as a string
     */
    private String getSSOToken(String serviceURL, String authURL, String userName, String password) throws Exception {

        RESTEndpoint restEndpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(serviceURL)
                .path(authURL)
                .post()
                .addModuleParameters()
                .headers("Content-Type", "application/json")
                .headers("X-OpenAM-Username", userName)
                .headers("X-OpenAM-Password", password)
                .apiVersion(RESTEndpoint.AUTHENTICATION_URI_API_VERSION)
                .build();

        Debug.log("about to login user " + userName + " with REST call " + restEndpoint.toString());

        RESTEndpoint.RESTResponse response = restEndpoint.call();

        String ssoToken = null;
        if (response.getResponseCode() == HTTP_RESPONSE_OK) {
            Debug.log("Call to " + serviceURL + " succeeded with response " + response.toString());
            JSONObject jsonObject = new JSONObject(response.toString());
            ssoToken = jsonObject.getString("tokenId");
        } else {
            Debug.log("Call to " + serviceURL + " FAILED with response code " + response.getResponseCode());
        }
        Debug.log("CreateProfileTask.getSSOToken() - SSO Token =" + ssoToken);

        return ssoToken;
    }

    /**
     * Get the iPlanetDirectoryPro cookie name from the serverinfo endpoint, using the agent admin user name and
     * password
     *
     * @param serviceURL The base OpenAM URL
     * @return The correct name of the iPlanetDirectoryPro cookie, which may not be iPlanetDirectoryPro
     * @throws Exception variously throws IOException from the rest service call and JSONException from JSON decoding
     */
    private String getCookieName(String serviceURL)
            throws Exception {

        String result = "iPlanetDirectoryPro"; // jump the gun with a suitable default

        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(serviceURL)
                .path(RESTEndpoint.SERVER_INFO_URI)
                .get()
                .headers("Content-Type", "application/json")
                .apiVersion(RESTEndpoint.SERVER_INFO_URI_API_VERSION)
                .build();

        Debug.log("About to determine cookie name, details of REST call: " + endpoint.toString());

        RESTEndpoint.RESTResponse response = endpoint.call();

        if (response.getResponseCode() == HTTP_RESPONSE_OK) {
            Debug.log("Call succeeded with response " + response.toString());
            JSONObject jsonObject = new JSONObject(response.toString());
            result = jsonObject.getString("cookieName");
        } else {
            Debug.log("Call FAILED with response code " + response.getResponseCode());
            Debug.log("Here is the response\n" + response.toString());
        }
        Debug.log("CreateProfileTask.getCookieName() - cookie name = " + result);

        return result;
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
            Debug.log("CreateProfileTask.readDataFromFile() - Error  reading file - " + fileName, ex);
            throw new InstallException(LocalizedMessage.get(LOC_TK_ERR_PASSWD_FILE_READ), ex);
        } finally {
            IOUtils.closeIfNotNull(br);
        }
        return firstLine;
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String agentUserName = (String)stateAccess.get(STR_AGENT_PROFILE_NAME);
        Object[] args = { agentUserName };
        LocalizedMessage message = LocalizedMessage.get(LOC_TSK_MSG_CREATE_AGENT_PROFILE_EXECUTE, args);
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
}
