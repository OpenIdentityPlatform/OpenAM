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
 * $Id: AgentMigration71.java,v 1.5 2008/08/19 19:09:10 veiming Exp $
 *
 */

package com.sun.identity.idm;

import java.security.AccessController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.SMSEntry;

public class AgentMigration71 {

    public static void migrate22AgentsToFAM80() {
        try {
            // Assuming upgrade scripts imported the OpenSSO
            // AgentService.xml,
            // migrate agents from existing DIT (AM 6.x/AM 7.x to OpenSSO
            // Enterprise 8.0.

            SSOToken token = getSSOToken();
            // First get all the sub realms
            OrganizationConfigManager ocmGet = 
                new OrganizationConfigManager(token, "/");
            Set getSet = new HashSet(); 
            getSet.add(SMSEntry.getRootSuffix());
            Set orgSet = ocmGet.getSubOrganizationNames();
            if (!orgSet.isEmpty()) {
                getSet.addAll(orgSet);
            } 
            System.out.println(IdRepoBundle.getString("500"));
            Object [] args = { getSet.toString() };
            System.out.println(IdRepoBundle.getString("501", args));

            String p = IdConstants.AGENTREPO_PLUGIN;
            Class thisClass = Class.forName(p);
            IdRepo thisPlugin = (IdRepo) thisClass.newInstance();

            // Iterate through all subrealms and get/search for agent
            // identities from IdRepo node.
            for (Iterator items = getSet.iterator(); items.hasNext();) {
                String realm = (String) items.next();

                AMIdentityRepository idRepo = 
                    new AMIdentityRepository(token, realm);
                IdSearchResults results = idRepo.searchIdentities(
                    IdType.AGENT, "*",new IdSearchControl());

                Iterator it = results.getSearchResults().iterator();
                while (it.hasNext()){
                    AMIdentity iden = (AMIdentity) it.next();
                    String idName = iden.getName();
                    Object[] args1 = { idName };
                    System.out.println(IdRepoBundle.getString("502", args1));
                    Map attrs = iden.getAttributes();
                    attrs.remove("cn");
                    attrs.remove("dn");
                    attrs.remove("objectclass");
                    attrs.remove("sunidentityserverdevicetype");
                    attrs.remove("sunidentityserverdeviceversion");
                    attrs.remove("uid");
                    if (attrs.containsKey("sunidentityserverdevicestatus")) {
                        // To match the schema in OpenSSO's
                        // AgentService.xml
                        Set dSet = 
                            (Set)attrs.get("sunidentityserverdevicestatus");
                        attrs.remove("sunidentityserverdevicestatus");
                        attrs.put("sunIdentityServerDeviceStatus", dSet);
                    }
                    Object[] args2 = { attrs.toString() };
                    System.out.println(IdRepoBundle.getString("503", args2));
                    thisPlugin.create(token, IdType.AGENTONLY, idName, attrs);
                }
                // Now upgrade scripts should reset the revision number of 
                // idRepoService.xml from 20 to 30 to add the AgentRepo 
                // as IdRepo Plugin and to display these migrated agents
                // under 'Configuration/Agents' tab.
            }
            System.out.println(IdRepoBundle.getString("505"));
        } catch (Exception ex2) {
            System.out.println(IdRepoBundle.getString("504"));
            ex2.printStackTrace();
        }
    }

    private static SSOToken getSSOToken() throws SSOException{
        try {
            return ((SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance()));
        } catch (Exception e) {
            throw (new SSOException("AgentMigration71:getSSOToken(): FAILED "+
                "invalid admin user/password."));
        }

    }
}
