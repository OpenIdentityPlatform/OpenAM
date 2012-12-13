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
 * $Id: ConfigureUnconfigure.java,v 1.13 2009/01/26 23:45:48 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * Contains setup and cleanup methods required to be executed
 * before and after the execution of agents suite.
 */
public class ConfigureUnconfigure extends TestCommon {

    private SSOToken admintoken;
    private ResourceBundle rbg;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private String agentId;
    private String agentPassword;
    private String strGblRB = "agentsGlobal";
    private String agentType;
    private String agentConfigurationType;
    private Map notificationMap;

    /**
     * Class constructor. Instantiates the ResourceBundles and
     * creates common objects required by tests.
     */
    public ConfigureUnconfigure()
    throws Exception {
        super("ConfigureUnconfigure");
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        agentId = rbg.getString(strGblRB + ".agentId");
        agentPassword = rbg.getString(strGblRB + ".agentPassword");
        agentType = rbg.getString(strGblRB + ".agentType");
        agentConfigurationType = rbg.getString(strGblRB + 
                ".30agentConfigurationType");
        idmc = new IDMCommon();
    }

    /**
     * Creates agent profile for the agent and start the notification server.
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void createAgentProfile()
    throws Exception {
        entering("createAgentProfile", null);
        try {
            notificationMap = startNotificationServer();
            CreateAgentProfile create = new CreateAgentProfile();
            Map map = new HashMap();
            Set set = new HashSet();
            set.add(agentPassword);
            map.put("userpassword", set);
            set = new HashSet();
            set.add("Active");
            map.put("sunIdentityServerDeviceStatus", set);
            log(Level.FINEST, "createAgentProfile", "Create the agent " +
                    "with ID " + agentId);
            admintoken = getToken(adminUser, adminPassword, basedn);
            smsc = new SMSCommon(admintoken);
            if (smsc.isAMDIT()) {
                if (!setValuesHasString(idmc.searchIdentities(admintoken,
                        agentId, IdType.AGENT), agentId))
                    idmc.createIdentity(admintoken, realm, IdType.AGENT,
                            agentId,  map);
            } else {
                set = new HashSet();
                if (agentType.contains("2.2")) {
                    set.add("2.2_Agent");
                    map.put("AgentType",set);
                } else {
                    if (agentType.contains("3.0J2EE") || 
                            agentType.contains("3.0WEBLOGIC")) {
                        set.add("J2EEAgent");
                        map.put("AgentType",set);
                    } else if (agentType.contains("3.0WEB")) {
                        set.add("WebAgent");
                        map.put("AgentType",set);
                    }
                }
                //Deleting agent profile if exists
                if (setValuesHasString(idmc.searchIdentities(admintoken,
                        agentId, IdType.AGENTONLY), agentId)) {
                    log(Level.FINEST, "createAgentProfile", "Agent :" + agentId 
                            + " exists. Deleting it." );
                    idmc.deleteIdentity(admintoken, realm, IdType.AGENTONLY,
                            agentId);
                }
                //Creating agent profile
                idmc.createIdentity(admintoken, realm, IdType.AGENTONLY,
                        agentId, map);
                if (agentType.contains("3.0") && 
                        agentConfigurationType.equals("centralized")) { 
                    log(Level.FINEST, "createAgentProfile", "creating agent " 
                            + agentId);
                    create.create(agentId, agentType);
                }

            }
        } catch (Exception e) {
            stopNotificationServer(notificationMap);
            log(Level.SEVERE, "createAgentProfile", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(admintoken);
        }
        exiting("createAgentProfile");
    }

    /**
     * Deletes agent profile for the agent and stops the notification server.
     */
   // @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void deleteAgentProfile()
    throws Exception {
        entering("deleteAgentProfile", null);
        try {
            admintoken = getToken(adminUser, adminPassword, basedn);
            smsc = new SMSCommon(admintoken);
            if (smsc.isAMDIT()) {
                if (setValuesHasString(idmc.searchIdentities(admintoken,
                agentId, IdType.AGENT), agentId))
                    idmc.deleteIdentity(admintoken, realm, IdType.AGENT,
                            agentId);
            }
            else {
                if (setValuesHasString(idmc.searchIdentities(admintoken,
                agentId, IdType.AGENTONLY), agentId))
                    idmc.deleteIdentity(admintoken, realm, IdType.AGENTONLY,
                            agentId);
            }
            stopNotificationServer(notificationMap);
        } catch (Exception e) {
            stopNotificationServer(notificationMap);
            log(Level.SEVERE, "deleteAgentProfile", e.getMessage());
            throw e;
        } finally {
            destroyToken(admintoken);
        }
        exiting("deleteAgentProfile");
    }
}
