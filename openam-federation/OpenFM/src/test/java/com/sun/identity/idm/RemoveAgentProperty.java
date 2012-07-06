/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RemoveAgentProperty.java,v 1.2 2009/12/22 18:00:25 veiming Exp $
 */

package com.sun.identity.idm;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RemoveAgentProperty {
    private static String realm = "/";
    private static String agentName = "TestAgent";
    private static String agentType = AgentConfiguration.AGENT_TYPE_J2EE;
    private static Map<String, Set<String>> attrValues =
        new HashMap<String, Set<String>>();
    private static String serverURL = SystemProperties.getServerInstanceName();
    private static String agentURL = SystemProperties.getServerInstanceName();

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

    @BeforeClass
    public void setup() throws Exception {
        AgentConfiguration.createAgent(adminToken, realm, agentName,
            agentType, attrValues, serverURL, agentURL);
    }

    @AfterClass
    public void cleanup() throws Exception {
        AMIdentity amid = new AMIdentity(adminToken, agentName,
            IdType.AGENTONLY, realm, null); 
        Set<AMIdentity> setDelete = new HashSet<AMIdentity>();
        setDelete.add(amid);
        AMIdentityRepository amir = new AMIdentityRepository(adminToken, realm);
        amir.deleteIdentities(setDelete);
    }

    @Test
    public void postiveTest()
        throws Exception {

        AMIdentity amid = new AMIdentity(adminToken, agentName,
            IdType.AGENTONLY, realm, null); 
        Map<String, Set<String>> attrMap =
            AgentConfiguration.getAgentAttributes(amid, false);

        Set<String> values = attrMap.get(
            "com.sun.identity.policy.client.clockSkew");

        // both new HashSet() or null should work
        attrMap = new HashMap<String, Set<String>>();
        attrMap.put("com.sun.identity.policy.client.clockSkew",
            new HashSet<String>());
//        attrMap.put("com.sun.identity.policy.client.clockSkew", null);
        amid.setAttributes(attrMap);
        amid.store();

        attrMap = AgentConfiguration.getAgentAttributes(amid, false);

        values = attrMap.get("com.sun.identity.policy.client.clockSkew");
        if ((values != null) && (!values.isEmpty())) {
            throw new Exception("Unable to remove agent property");
        }

    }
}
