/* The contents of this file are subject to the terms
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
 * $Id: ConfigUnconfig.java
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.csdk;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.agents.CreateAgentProfile;
import com.sun.identity.qatest.common.CSDKCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * This class does the configuration for C SDK. Creates a webagent and modifies
 * the Opensso AgentBootStrap file
 * Policy service is configured based on the UMDatastore properties.
 * This class also starts and stops the notification server.
 */
public class ConfigUnconfig extends TestCommon {

    Map notificationMap;
    int listenerID;
    int eventID;
    SSOToken token;   
    AMIdentityRepository idrepo;
    String strID;
    private IDMCommon idmc;
    private CSDKCommon cc;
    ProcessBuilder pb;
    String configKey = "secret12";
    String encryptedKey;
    private SMSCommon smsc;
    private AuthenticationCommon ac;

    /**
     * Creates a new instance of ConfigUnconfig
     */
    public ConfigUnconfig() {
        super("ConfigUnconfig");
        cc = new CSDKCommon();
        idmc = new IDMCommon();
        ac = new AuthenticationCommon("csdk");
    }

    /**
     * Start the notification (jetty) server for getting notifications from the
     * server.
     */
    @BeforeSuite(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void startServer()
            throws Exception {
        entering("startServer", null);
        notificationMap = startNotificationServer();
        exiting("startServer");
    }

    /**
     * Stop the notification (jetty) server for getting notifications from the
     * server.
     */
    public void stopServer()
            throws Exception {
        entering("stopServer", null);
        stopNotificationServer(notificationMap);
        exiting("stopServer");
    }

    /**
     * Creates web agent and modifies the bootstrap file
     */
    @BeforeSuite(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
            throws Exception {
        entering("setup", null);
        try {
            ac.createAuthInstancesMap();
            CreateAgentProfile createAgent = new CreateAgentProfile();
            token = getToken(adminUser, adminPassword, realm);
            idrepo = new AMIdentityRepository(token, realm);
            strID = "webagentCSDK";
            File directory;
            String absFileName;
            Map map = new HashMap();
            Set set = new HashSet();
            set.add(strID);
            map.put("userpassword", set);
            set = new HashSet();
            set.add("Active");
            map.put("sunIdentityServerDeviceStatus", set);
            set = new HashSet();
            SMSCommon smsC = new SMSCommon(token);
            if ((smsC.isAMDIT())) {
                idmc.createIdentity(token, realm, IdType.AGENT, strID,
                        map);
            } else {
                log(Level.FINE, "setup", "This is FAM " +
                        "DIT, Agents are part of SM node");
                set.add("WebAgent");
                map.put("AgentType", set);

            }
            //Deleting agent profile if exists
            if (setValuesHasString(idmc.searchIdentities(token,
                    strID, IdType.AGENTONLY), strID)) {
                log(Level.FINEST, "createAgentProfile", "Agent :" + strID +
                        " exists. Deleting it.");
                idmc.deleteIdentity(token, realm, IdType.AGENTONLY,
                        strID);
            }
            //Creating agent profile
            idmc.createIdentity(token, realm, IdType.AGENTONLY,
                    strID, map);

            log(Level.FINEST, "createAgentProfile", "creating agent " + strID);
            createAgent.create(strID, "3.0WEB");
            Map configurationMap = cc.getLibraryPath();
            boolean isWindows = Boolean.parseBoolean(configurationMap.get
                    ("isWindows").toString());
            String cryptUtilPath = (String) configurationMap.get
                    ("cryptUtilPath");
            String results;
            String error;
            if (isWindows == true) {
                pb = new ProcessBuilder(cryptUtilPath + fileseparator +
                        "cryptit", strID, configKey);
            } else {
                pb = new ProcessBuilder(cryptUtilPath + fileseparator +
                        "crypt_util", strID, configKey);
            }
            pb.directory(new File(cryptUtilPath));
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader
                    (p.getInputStream()));
            while ((results = stdInput.readLine()) != null) {
                encryptedKey = results;
            }
            BufferedReader stdError = new BufferedReader(new InputStreamReader
                    (p.getErrorStream()));
            while ((error = stdError.readLine()) != null) {
                log(Level.FINEST, "setup", error);
            }
            String debugLogDir = getBaseDir() + fileseparator +
                    serverName + fileseparator + "debug";
            String auditFileName = "csdkLog";
            Map replaceVals = new HashMap();
            replaceVals.put("@AGENT_PROFILE_NAME@", strID);
            replaceVals.put("@AGENT_ENCRYPTED_PASSWORD@", encryptedKey);
            replaceVals.put("@AGENT_ENCRYPT_KEY@", configKey);
            replaceVals.put("@AM_SERVICES_PROTO@", protocol);
            replaceVals.put("@AM_SERVICES_HOST@", host);
            replaceVals.put("@AM_SERVICES_PORT@", port);
            replaceVals.put("@AM_SERVICES_DEPLOY_URI@", uri);
            replaceVals.put("@DEBUG_LOGS_DIR@", debugLogDir);
            replaceVals.put("@AUDIT_LOGS_DIR@", debugLogDir);
            replaceVals.put("@AUDIT_LOG_FILENAME@", auditFileName);
            absFileName = getBaseDir() + fileseparator + "resources" +
                    fileseparator + "csdk" + fileseparator +
                    "CSDKBootstrap.properties";
            String absFileNameDestination = getBaseDir() + fileseparator +
                    serverName + fileseparator + "built" + fileseparator +
                    "classes" + fileseparator + "csdk" + fileseparator +
                    "CSDKBootstrap.properties";
            directory = new File(absFileName);
            assert (directory.exists());
            log(Level.FINEST, "setup", "Replacing the file :" +
                    absFileName);
            replaceString(absFileName, replaceVals, absFileNameDestination);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            cleanup();
            throw e;
        } finally {
            destroyToken(token);
        }
        exiting("setup");
    }

    /**
     * This method deletes the webagent
     */
    @AfterSuite(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
            throws Exception {
        try {
            token = getToken(adminUser, adminPassword, basedn);
            smsc = new SMSCommon(token);
            if (smsc.isAMDIT()) {
                if (setValuesHasString(idmc.searchIdentities(token,
                        strID, IdType.AGENT), strID)) {
                    idmc.deleteIdentity(token, realm, IdType.AGENT,
                            strID);
                }
            } else {
                if (setValuesHasString(idmc.searchIdentities(token,
                        strID, IdType.AGENTONLY), strID)) {
                    idmc.deleteIdentity(token, realm, IdType.AGENTONLY,
                            strID);
                }
            }
        } catch (Exception e) {            
            log(Level.SEVERE, "cleanup", e.getMessage());
            throw e;
        } finally {
            destroyToken(token);
            stopServer();
        }
    }
}
