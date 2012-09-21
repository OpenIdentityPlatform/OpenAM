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
 * $Id: IdRepoUtils.java,v 1.1 2009/11/12 18:37:36 veiming Exp $
 */

package com.sun.identity.entitlement.util;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.FQDNUrl;
import com.sun.identity.sm.SMSException;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dennis
 */
public class IdRepoUtils {
    private IdRepoUtils() {

    }

    public static void deleteIdentity(
        String realm,
        AMIdentity identity
    ) throws IdRepoException, SSOException {
        Set<AMIdentity> set = new HashSet<AMIdentity>();
        set.add(identity);
        deleteIdentities(realm, set);
    }

    public static void deleteIdentities(
        String realm,
        Set<AMIdentity> identities
    ) throws IdRepoException, SSOException {
        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, realm);
        amir.deleteIdentities(identities);
    }

    public static AMIdentity createUser(String realm, String id)
        throws SSOException, IdRepoException {
        return createUser(realm, id, null);
    }

    public static AMIdentity createUser(
        String realm,
        String id,
        Map<String, Set<String>> properties
    )
        throws SSOException, IdRepoException {
        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, realm);
        Map<String, Set<String>> attrValues =
            new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(id);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);

        if (properties != null) {
            attrValues.putAll(properties);
        }

        return amir.createIdentity(IdType.USER, id, attrValues);
    }

    public static AMIdentity createAgent(
        String realm,
        String id
    ) throws IdRepoException, SSOException, SMSException, 
        MalformedURLException, ConfigurationException {
        String agentType = "J2EEAgent";
        String serverURL = "http://www.example.com:8080/opensso";
        String agentURL = "http://www.example.com:9090/client";

        FQDNUrl fqdnServerURL = new FQDNUrl(serverURL);
        FQDNUrl fqdnAgentURL = new FQDNUrl(agentURL);

        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        Map map = AgentConfiguration.getDefaultValues(agentType, false);
        AgentConfiguration.tagswapAttributeValues(map, agentType,
            fqdnServerURL, fqdnAgentURL);
        Set set = new HashSet();
        set.add(id);
        map.put("userpassword", set);
        return AgentConfiguration.createAgent(adminToken, realm,
            id, agentType, map);
    }


    public static AMIdentity createGroup(String realm, String name)
        throws SSOException, IdRepoException {
        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, realm);
        Map<String, Set<String>> attrValues =new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(name);
        attrValues.put("cn", set);
        return amir.createIdentity(IdType.GROUP, name, attrValues);
    }
}
