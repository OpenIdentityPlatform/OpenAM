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
 * $Id: SharedState.java,v 1.1 2009/01/30 12:09:40 kalpanakm Exp $
 *
 */

package com.sun.opensso.agents.jsr115;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import com.sun.identity.agents.arch.AgentConfiguration;

/**
 * 
 * SharedState maintains the state of the PolicyConfiguration for various 
 * CONTEXT_IDs
 * 
 * @author kalpana
 */
public class SharedState {

    //lock on the shared configTable and linkTable
    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private static Lock rLock = rwLock.readLock();
    private static Lock wLock = rwLock.writeLock();
    private static HashMap<String, OpenSSOJACCPolicyConfiguration> configTable =
            new HashMap<String, OpenSSOJACCPolicyConfiguration>();
    private static HashMap<String, HashSet<String>> linkTable =
            new HashMap<String, HashSet<String>>();
    private static final Logger logger =
            Logger.getLogger(SharedState.class.getPackage().getName());
    private static HashSet<String> adminAppList = null;
    
    /*
     * JACC configuration is for an entire domain. So all the admin apps also 
     * pass through the custom JSR115 provider. Inorder to avoid this, a check
     * is made for the contextID. If its an adminapp, control is passed on to
     * default provider implementation
     *  
     */
    
    static {
        adminAppList = new HashSet();
        adminAppList.add("adminapp/adminapp");
        adminAppList.add("admingui/admingui");
        adminAppList.add("__default-web-module/__default-web-module");
        adminAppList.add("__ejb_container_timer_app/ejb_jar");
        adminAppList.add("__JWSappclients/sys_war");
        adminAppList.add("WSTXServices/WSTXServices");
        adminAppList.add("MEjbApp/mejb_jar");        
    }
    

    private SharedState() {
    }

    static Logger getLogger() {
        return logger;
    }
    
    static OpenSSOJACCPolicyConfiguration lookupConfig(String pcid) {

        OpenSSOJACCPolicyConfiguration pc = null;
        wLock.lock();
        try {
            String key = AgentConfiguration.getApplicationUser()+ pcid ;
            pc = configTable.get(key);
        } finally {
            wLock.unlock();
        }
        return pc;
    }

    static OpenSSOJACCPolicyConfiguration getConfig(String pcid, boolean remove) {
    
        OpenSSOJACCPolicyConfiguration pc = null;
        String key = null;
        wLock.lock();
        try {
            key = AgentConfiguration.getApplicationUser()+ pcid ;
            pc = configTable.get(key);
            if (pc == null) {
                // TODO - Check whether the policyfiles are present in LDAP and decide
                // Whether you need read that and create a config object ...
                boolean isPresent = OpenSSOJACCPolicyConfiguration.isPolicyPresent(pcid);
                if (isPresent){
                    pc = new OpenSSOJACCPolicyConfiguration(pcid,false);
                } else {                                 
                    pc = new OpenSSOJACCPolicyConfiguration(pcid);
                }
                SharedState.initLinks(key);
                configTable.put(key, pc);                
            } else if (remove) {
                SharedState.removeLinks(key);
            }
        } finally {
            wLock.unlock();
        }
        return pc;
    }
    
    static Collection getConfigCollection() {
        return configTable.values();
    }
    
    /**
     * Creates a relationship between this configuration and another
     * such that they share the same principal-to-role mappings.
     * PolicyConfigurations are linked to apply a common principal-to-role
     * mapping to multiple seperately manageable PolicyConfigurations,
     * as is required when an application is composed of multiple
     * modules.
     * <P>
     * Note that the policy statements which comprise a role, or comprise
     * the excluded or unchecked policy collections in a PolicyConfiguration
     * are unaffected by the configuration being linked to another.
     * <P>
     * The relationship formed by this method is symetric, transitive
     * and idempotent. 
     * @param id
     * @param otherId
     * @throws javax.security.jacc.PolicyContextException If otherID 
     * equals receiverID. no relationship is formed.
     */
    static void link(String id, String otherId)
            throws javax.security.jacc.PolicyContextException {

        wLock.lock();
        try {

            if (otherId.equals(id)) {
                String msg = "Operation attempted to link PolicyConfiguration to itself.";
                throw new IllegalArgumentException(msg);
            }

            // get the linkSet corresponding to this context
            String key = AgentConfiguration.getApplicationUser()+ id;
            String key1 = AgentConfiguration.getApplicationUser() + otherId;
            HashSet<String> linkSet = linkTable.get(key);

            // get the linkSet corresponding to the context being linked to this
            HashSet otherLinkSet = linkTable.get(key1);

            if (otherLinkSet == null) {
                String msg = "Linked policy configuration (" + otherId + ") does not exist";
                throw new RuntimeException(msg);
            }

            Iterator it = otherLinkSet.iterator();

            // for each context (id) linked to the context being linked to this
            while (it.hasNext()) {
                String nextid = (String) it.next();

                //add the id to this linkSet
                linkSet.add(nextid);

                //replace the linkset mapped to all the contexts being linked
                //to this context, with this linkset.
                linkTable.put(nextid, linkSet);
            }

        } finally {
            wLock.unlock();
        }
    }

    static void initLinks(String id) {
        // create a new linkSet with only this context id, and put in the table.
        HashSet linkSet = new HashSet();
        linkSet.add(id);
        linkTable.put(id, linkSet);
    }

    static void removeLinks(String id) {
        wLock.lock();
        String key = AgentConfiguration.getApplicationUser();
        try {        // get the linkSet corresponding to this context.
            HashSet linkSet = linkTable.get(key);
            // remove this context id from the linkSet (which may be shared
            // with other contexts), and unmap the linkSet from this context.
            if (linkSet != null) {
                linkSet.remove(key);
                linkTable.remove(key);
            }

            initLinks(key);
        } finally {
            wLock.unlock();
        }

    }
    
    
    public static HashSet getLink(String contextID){
        String key = AgentConfiguration.getApplicationUser() + contextID;
        return linkTable.get(contextID);        
    }
    
    public static boolean isAdminApp(String contextID){       
        return adminAppList.contains(contextID);        
    }

}


