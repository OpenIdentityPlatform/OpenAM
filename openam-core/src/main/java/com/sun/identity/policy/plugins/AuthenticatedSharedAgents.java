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
 * $Id: AuthenticatedSharedAgents.java,v 1.3 2009/07/16 17:45:58 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceListener;

import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;

/**
 * This subject applies to all users/agents with valid <code>SSOToken</code>.
 */
public class AuthenticatedSharedAgents implements Subject {

    private static final String version = "1.0";

    private static final String agentserviceName = IdConstants.AGENT_SERVICE;

    private static ServiceConfigManager scm = null;

    private static Set sharedAgentsCache = new HashSet();

    private static Map realmCache = new HashMap(2);


    private static ValidValues validValues = 
        new ValidValues(ValidValues.SUCCESS, Collections.EMPTY_SET);

    static Debug debug = Debug.getInstance("AuthAgents");

    static {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        if (debug.messageEnabled()) {
            debug.message(": AuthenticatedSharedAgents adding Listener");
        }
        try {
            scm = new ServiceConfigManager(adminToken, agentserviceName,
                    version);
            scm.addListener(new ServiceListenerImpl());
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgents: "
                        + "Unable to init scm due to " + smse);
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgents: "
                        + "Unable to init scm due to " + ssoe);
            }
        }
    }

    /**
     * Default Constructor
     */
    public AuthenticatedSharedAgents() {
    }

    /**
     * Initialize the subject. No properties are required for this
     * subject.
     * @param configParams configurational information
     */
    public void initialize(Map configParams) {
        // do nothing
    }

    /**
     * Returns the syntax of the subject type.
     * @see com.sun.identity.policy.Syntax
     * @param token the <code>SSOToken</code>. Not used for this subject.
     * @return Syntax for this subject.
     */
    public Syntax getValueSyntax(SSOToken token) {
        return (Syntax.CONSTANT);
    }

    /**
     * Returns an empty list as possible values. 
     * @param token the <code>SSOToken</code>
     * @return <code>ValidValues</code> object with empty list.
     *
     */
    public ValidValues getValidValues(SSOToken token) {
        return validValues;
    }

    /**
     * Returns an empty list as possible values. 
     *
     * @param token the <code>SSOToken</code>
     * @param pattern the pattern to match in valid values. Ignored for this 
     * subject
     * @return <code>ValidValues</code> object with empty list.
     *
     */
    public ValidValues getValidValues(SSOToken token, String pattern) {
        return (validValues);
    }

    /**
     * This method does nothing as there are no values to display for this 
     * subject.
     *
     */
    public String getDisplayNameForValue(String value, Locale locale) {
        // does nothing
        return(value);
    }

    /**
     * Returns an empty collection as value.
     * @return an empty set 
     */
    public Set getValues() {
        return (Collections.EMPTY_SET);
    }

    /**
     * This method does nothing for this subject as there are no values to set
     * for this subject.
     */
    public void setValues(Set names) {
        // does nothing
    }

    
    /**
     * Determines if the agent belongs to  the
     * <code>AuthenticatedSharedAgents</code> object.
     * @param token SSOToken of the agent
     * @return <code>true</code> if the agent SSOToken is valid. 
     * <code>false</code> otherwise.
     * @exception SSOException if error occurs while validating the token.
     */

    public boolean isMember(SSOToken token) throws SSOException {

        boolean ismember = false;
        int errCode = 0;
        if ((token != null) && (SSOTokenManager.getInstance().
            isValidToken(token))) {
            try {
                String userDN = null;
                String userDNUnivId = null;
                AMIdentity amId = IdUtils.getIdentity(token);
                IdType idType = amId.getType();
                userDN = amId.getName();
                userDNUnivId = amId.getUniversalId();
                if (debug.messageEnabled()) {
                    debug.message("AuthenticatedSharedAgents:isMember:"+
                        "idType = " + idType + ", userDN = " + userDN);
                }
                if ((userDN != null) && (idType.equals(IdType.AGENT) || 
                    idType.equals(IdType.AGENTONLY))) {
                    String rlmName = amId.getRealm();
                    if (isSharedAgent(token, userDN, userDNUnivId, rlmName)) {
                        errCode = 1;
                        if (debug.messageEnabled()) {
                            debug.message("AuthenticatedSharedAgents:isMember:"+
                                "YES");
                        }
                    } else {
                        if (debug.messageEnabled()) {
                            debug.message("AuthenticatedSharedAgents:isMember:"+
                                "NO");
                        }
                    }
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("AuthenticatedSharedAgents.isMember():"
                            +"userDN is null or invalid IdType "+userDN +
                            "IdType :" + idType);
                        debug.message("AuthenticatedSharedAgents.isMember():"
                            +"returning false");
                    }
                    errCode = 0;
                }
            } catch (IdRepoException ire) {
                debug.error("AuthenticatedSharedAgents:isMember:" +
                    " IdRepoException:msg = " + ire.getMessage());
                errCode = 0;
            }
            if (errCode == 1) {
                ismember = true;
            }
        }
        return ismember;
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AuthenticatedSharedAgents theClone = null;
        try {
            theClone = (AuthenticatedSharedAgents) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        return theClone;
    }

    /**
    * Return a hash code for this <code>AuthenticatedSharedAgents</code>.
    * @return a hash code for this <code>AuthenticatedSharedAgents</code> 
    *         object.
    */
    public int hashCode() {
        return super.hashCode();
    }

    /**
    * Checks if distinguished user name is a shared user/agent 
    * if returns true if so.
    */
    protected boolean isSharedAgent(SSOToken token, String userName, 
        String userDNUnivId, String rlmName) {

        boolean isSharedAgent = false;
        try {
            if (debug.messageEnabled()) {
                debug.message("AuthenticatedSharedAgents:isSharedAgent:"+
                    "userName = " + userName + " Realm Name = " + rlmName);
            }
            if (userName != null) {
                String agentName = userName;
                if (DN.isDN(userName)) {
                    agentName = LDAPDN.explodeDN(userName, true)[0];
                }
                if (debug.messageEnabled()) {
                    debug.message("AuthenticatedSharedAgents:isSharedAgent:"+
                        "agentName = " + agentName);
                }
                // Check in cache
                if ((sharedAgentsCache != null) &&
                    (!sharedAgentsCache.isEmpty()) &&
                    (sharedAgentsCache.contains(userDNUnivId))) {
                    return (true);
                }
                SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                ServiceConfig oc = getOrgConfig(adminToken, rlmName);

                // Get the agent's schemaID from the config.
                ServiceConfig aCfg = oc.getSubConfig(agentName);
                if (aCfg != null) {
                    String agentType = aCfg.getSchemaID();
                    if ((oc.getSubConfigNames().contains(agentName)) && 
                        (agentType.equalsIgnoreCase("SharedAgent"))) {    
                        isSharedAgent = true;
                        updateCache(userDNUnivId);
                    }
                }
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgents.isSharedAgent(): "
                    + "SSOException: " + ssoe);
            }
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgents.isSharedAgent(): "
                    + "SMSException: " + smse);
            }
        }
        return isSharedAgent;
    }

    // Returns the organization configuration of the 'default' group
    // from AgentService.
    private ServiceConfig getOrgConfig(SSOToken token, String realmName) {

        if (debug.messageEnabled()) {
            debug.message("AuthenticatedSharedAgents.getOrgConfig() called. ");
        }
        ServiceConfig orgConfigCache = null;
        try {
            // Check in cache first
            if ((realmCache != null) && (!realmCache.isEmpty()) &&
                (realmCache.containsKey(realmName))) {
                orgConfigCache = (ServiceConfig) realmCache.get(realmName);
                if (orgConfigCache.isValid()) {
                    debug.message("AuthenticatedSharedAgents.getOrgConfig() found in cache.");
                    return (orgConfigCache);
                }
            }
            if (scm == null) {
                scm = new ServiceConfigManager(token, agentserviceName,
                    version);
            }
            orgConfigCache = scm.getOrganizationConfig(realmName, null);
            // Update the realm cache.
            updateRealmCache(realmName, orgConfigCache);
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgents.getOrgConfig(): "
                        + "Unable to get organization config due to " + smse);
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgents.getOrgConfig(): "
                        + "Unable to get organization config due to " + ssoe);
            }
        }
        return (orgConfigCache);
    }

    // Stores the shared agent name in Universal Id format 
    // as there may be same agent names in multiple sub realms.
    private static void updateCache(String userDNUnivId) {
        Set nset = new HashSet();
        nset.addAll(sharedAgentsCache);
        nset.add(userDNUnivId);
        sharedAgentsCache = nset;
    }

    // Clears the cache where the agent name is stored.
    static void clearCache() {
        sharedAgentsCache = new HashSet();
        realmCache = new HashMap(2);
    }

    // Cache to store the realm name and the organization config.
    private static void updateRealmCache(String realmName,
        ServiceConfig orgConfig) {
        if (debug.messageEnabled()) {
            debug.message("AuthenticatedSharedAgents.updateRealmCache: " + 
                "update cache for realm " + realmName);
        }
        Map rmap = new HashMap(2);
        rmap.putAll(realmCache);
        rmap.put(realmName, orgConfig);
        realmCache = rmap;
    }

    private static class ServiceListenerImpl implements ServiceListener {
        // The following three methods implement ServiceListener interface
        /*
         * (non-Javadoc)
         *
         * @see com.sun.identity.sm.ServiceListener#globalConfigChanged(
         *      java.lang.String,
         *      java.lang.String, java.lang.String, java.lang.String, int)
         */
        public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
            if (debug.messageEnabled()) {
                debug.message("AuthenticatedSharedAgents.globalConfigChanged..");
            }
            clearCache();
        }

        /*
         * (non-Javadoc)
         *
         * @see com.sun.identity.sm.ServiceListener#organizationConfigChanged(
         *      java.lang.String,
         *      java.lang.String, java.lang.String, java.lang.String,
         *      java.lang.String, int)
         */
        public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)

        {
            if (debug.messageEnabled()) {
                debug.message("AuthenticatedSharedAgents."+
                    "organizationConfigChanged..");
            }
            clearCache();
        }

        /*
         * (non-Javadoc)
         *
         * @see com.sun.identity.sm.ServiceListener#schemaChanged(java.lang.String,
         *      java.lang.String)
         */
        public void schemaChanged(String serviceName, String version) {
            if (debug.messageEnabled()) {
                debug.message("AuthenticatedSharedAgents.schemaChanged..");
            }
            clearCache();
        }
    }
}
