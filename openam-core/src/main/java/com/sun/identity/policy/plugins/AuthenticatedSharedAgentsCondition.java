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
 * $Id: AuthenticatedSharedAgentsCondition.java,v 1.7 2009/09/09 23:52:28 veiming Exp $
 *
 */



package com.sun.identity.policy.plugins;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.shared.debug.Debug;

/**
 * The class <code>AuthenticatedSharedAgentsCondition</code> checks
 * if the authenticated shared agent has the agent name it is trying to read.
 */

public class AuthenticatedSharedAgentsCondition implements Condition, 
    ServiceListener {

    // Instance variables
    private Map properties;

    private String delimiter = "/";

    // Configuration property names
    private static List propertyNames = Collections.EMPTY_LIST;

    private static ServiceConfigManager scm = null;

    private static final String version = "1.0";

    private static final String attributeToRead = "AgentsAllowedToRead";

    private static final String agentserviceName = IdConstants.AGENT_SERVICE;

    private static Map sharedAgentsCache = new HashMap(2);

    private static Map realmCache = new HashMap(2);

    // Debug file
    private static Debug debug = Debug.getInstance("AuthAgents");

    //  Constants for constructing resource names
    static final String RESOURCE_PREFIX = "sms://";
    static final String SVC_RESOURCE_NAME =
        "/sunIdentityRepositoryService/1.0/application/";

    /** No argument constructor 
     */
    public AuthenticatedSharedAgentsCondition() {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
               AdminTokenAction.getInstance());
        try {
            if (scm == null) {
                scm = new ServiceConfigManager(adminToken, agentserviceName,
                    version);
            }
            if (scm != null) {
                scm.addListener(this);
            }
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgentsCondition: "
                       + "Unable to init scm due to " + smse);
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgentsCondition: "
                       + "Unable to init scm due to " + ssoe);
            }
        }
    }

    /**
     * Returns a set of property names for the condition.
     *
     * @return set of property names
     */
    public List getPropertyNames() 
    {
        return (propertyNames);
    }
 
    /**
     * Returns the syntax for a property name
     * @see com.sun.identity.policy.Syntax
     *
     * @param property <code>String</code> representing property name
     *
     * @return <code>Syntax<code> for the property name
     */
     public Syntax getPropertySyntax(String property)
     {
         return (Syntax.ANY);
     }
      
    /**
     * Gets the display name for the property name.
     * The <code>locale</code> variable could be used by the plugin to
     * customize the display name for the given locale.
     * The <code>locale</code> variable could be <code>null</code>, in which
     * case the plugin must use the default locale.
     *
     * @param property property name.
     * @param locale locale for which the property name must be customized.
     * @return display name for the property name.
     * @throws PolicyException if unable to get display name
     */
     public String getDisplayName(String property, Locale locale) 
       throws PolicyException
     {
         return property;
     }
 
     /**
      * Returns a set of valid values given the property name. This method
      * is called if the property Syntax is either the SINGLE_CHOICE or 
      * MULTIPLE_CHOICE.
      *
      * @param property <code>String</code> representing property name
      * @return Set of valid values for the property.
      * @exception PolicyException if unable to get the Syntax.
      */
     public Set getValidValues(String property) throws PolicyException
     {
         return (Collections.EMPTY_SET);
     }


    /** 
     *  Sets the properties of the condition.
     *  Evaluation of ConditionDecision is influenced by these properties.
     *  @param properties of the condition that governs
     *         whether a policy applies. The only defined property
     *         is <code>attributes</code>
     */
    public void setProperties(Map properties) throws PolicyException {

        this.properties = properties;
    }

    /** Gets the properties of the condition.  
     *  @return  map view of properties that govern the 
     *           evaluation of  the condition decision
     *  @see #setProperties(Map)
     */
    public Map getProperties() {
       return (properties == null)
                ? null : Collections.unmodifiableMap(properties);
    }

    /**
     * Gets the decision computed by this condition object.
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment map of key/value pairs.
     *
     * @return the condition decision. The condition decision 
     *         encapsulates whether a policy applies for the request. 
     *
     * Policy framework continues evaluating a policy only if it 
     * applies to the request as indicated by the CondtionDecision. 
     * Otherwise, further evaluation of the policy is skipped. 
     *
     * @throws SSOException if the token is invalid
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        boolean allowed = false;
        if (debug.messageEnabled()) {
            debug.message("AuthenticatedSharedAgentsCondition."+
                "getConditionDecision: " +
                "called with Token: " + token.getPrincipal().getName() +
                ", requestedResourcename: "
                + env.get(PolicyEvaluator.SUN_AM_ORIGINAL_REQUESTED_RESOURCE));
        }
        String realmName = null;
        String sharedAgentName = null;
        String sharedAgentUnivId = null;
        try {
            AMIdentity id = IdUtils.getIdentity(token);
            realmName = id.getRealm();
            sharedAgentName = id.getName();
            sharedAgentUnivId = id.getUniversalId();
        } catch (SSOException ssoe) {
            // Debug it and throe error message.
            if (debug.messageEnabled()) {
                debug.message("AuthenticatedSharedAgentsCondition."
                    +"getConditionDecision: invalid sso token: " 
                    + ssoe.getMessage());
            }
            throw ssoe;
        } catch (IdRepoException ide) {
            // Debug it and throw converted policy exception.
            if (debug.messageEnabled()) {
                debug.message("AuthenticatedSharedAgentsCondition."
                    +"getConditionDecision IdRepo exception: ", ide);
            }
            throw new PolicyException(ide);
        }
        // Get the resource name from the env
        Object o = env.get(PolicyEvaluator.SUN_AM_ORIGINAL_REQUESTED_RESOURCE);
        if (debug.messageEnabled()) {
            debug.message("AuthenticatedSharedAgentsCondition."
                +"getConditionDecision:"
                +" name: " + sharedAgentName + " resource: " + o);
        }
        if (o != null) {
            String resourceName = null;
            if (o instanceof String ) {
                resourceName = (String) o;
            } else if (o instanceof Set) {
                if (!((Set) o).isEmpty()) {
                    resourceName = (String) ((Set) o).iterator().next();
                }
            } else if (debug.warningEnabled()) {
                resourceName = "";
                debug.warning("AuthenticatedSharedAgentsCondition."
                    +"getConditionDecision: Unable to get resource name");
            }

            // Get the agents from the shared list of the tokenID/shared
            // agent.
            // Iterate and compare the agents from the shared list with 
            // the ones under resource from env and if equal assign true.
            // resource from env might be a single element Set.

            try {
                Set agentsFromEnv = new HashSet();
                String agentTypeName = IdType.AGENT.getName();
                String agentOnlyTypeName = IdType.AGENTONLY.getName();
                SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                ServiceConfig orgConfig = getOrgConfig(adminToken, realmName);

                String[] retVal = split(resourceName);
                         
                if ((retVal[0].equalsIgnoreCase(agentTypeName) &&
                    retVal[1].equalsIgnoreCase(agentTypeName)) ||
                    (retVal[0].equalsIgnoreCase(agentOnlyTypeName) &&
                     retVal[1].equalsIgnoreCase(agentOnlyTypeName))) {
                    agentsFromEnv.add(retVal[0]);
                }

                if ((!retVal[0].equalsIgnoreCase(agentTypeName)) &&
                    (!retVal[0].equalsIgnoreCase(agentOnlyTypeName))) {

                    retVal[0] = getAgentNameFromEnv(resourceName);

                    if (retVal[0] == null) {
                        return new ConditionDecision(false);
                    }
                    if (retVal[0].equalsIgnoreCase(sharedAgentName)) {
                        Map envMap = getAttributes(orgConfig, retVal[0]);
                        agentsFromEnv = (Set) envMap.get(attributeToRead);
                    } else {
                        agentsFromEnv.add(retVal[0]);
                    }
                    if (debug.messageEnabled()) {
                        debug.message("AuthenticatedSharedAgentsCondition." +
                            "getConditionDecision: agentsFromEnv: " +
                            agentsFromEnv + "retVal[0] "+retVal[0]);
                    }
                }
                // Check in cache
                if ((sharedAgentsCache != null) &&
                    (sharedAgentsCache.containsKey(sharedAgentUnivId))) {
                    Set agentsfromCache = 
                        (Set) sharedAgentsCache.get(sharedAgentUnivId);

                    if (agentsfromCache != null && 
                        !agentsfromCache.isEmpty()) {
                        allowed = getPermission(agentsFromEnv, 
                            agentsfromCache); 
                    }
                    return new ConditionDecision(allowed);
                }

                // If not in cache.
                // Return the attributes for the given agent under
                // default group.
                Map agentsAttrMap = getAttributes(orgConfig, sharedAgentName);
                Set agentsToRead = (Set) agentsAttrMap.get(attributeToRead);
                if (debug.messageEnabled()) {
                    debug.message("AuthenticatedSharedAgentsCondition." +
                        "getConditionDecision: agentsToRead: " +
                            agentsToRead);
                }

                if (agentsToRead != null && !agentsToRead.isEmpty()) {
                    allowed = getPermission(agentsFromEnv, agentsToRead);
                }
                // Update the cache.
                updateCache(sharedAgentUnivId, agentsToRead);
            } catch (IdRepoException idpe) {
                debug.error("AuthenticatedSharedAgentsCondition."+
                    "getConditionDecision(): Unable to read agent"+
                    " attributes for " + sharedAgentName, idpe);
                throw new PolicyException(idpe);
            }
        }
        return new ConditionDecision(allowed);
    }

    private String getAgentNameFromEnv(String resName) {
        String agentName = null;
        /*
         * This check is for the resource name like this is constructed 
         * from the delegation service while getting the permission
         * requestedResourcename:
         * [sms://dc=openam,dc=openidentityplatform,dc=org/sunIdentityRepositoryService
         * /1.0/application/agentonly/http://quasar.red.iplanet.com:7001/
         * StockService/StockService]
         */
        String rn = resName.toLowerCase();
        // Get the index after the realm name
        int ndx = rn.indexOf(SVC_RESOURCE_NAME.toLowerCase());
        if (ndx != -1) {
            // Get the substring after the realm, server name, version and 
            // application.
            rn = rn.substring(ndx + SVC_RESOURCE_NAME.length());
            // Find the next index of "/" to bypass the agent type
            ndx = rn.indexOf('/');
            if (ndx != -1) {
                // Get the agent name
                agentName = rn.substring(ndx + 1);
            }
        }
        return (agentName);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AuthenticatedSharedAgentsCondition theClone = null;
        try {
            theClone = (AuthenticatedSharedAgentsCondition) super.clone();
            theClone.properties = Collections.unmodifiableMap(
                com.sun.identity.sm.SMSUtils.copyAttributes(properties));
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        return theClone;
    }

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
            debug.message("AuthenticatedSharedAgentsCondition."+
                "globalConfigChanged..");
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
            debug.message("AuthenticatedSharedAgentsCondition."+
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
            debug.message("AuthenticatedSharedAgentsCondition.schemaChanged..");
        }
        clearCache();
    }

    // Returns the attributes from the agent's profile.
    private Map getAttributes(ServiceConfig svcConfig, String agentName)
        throws IdRepoException, SSOException, PolicyException {

        if (debug.messageEnabled()) {
            debug.message("AuthenticatedSharedAgentsCondition."+
                "getAttributes() called:AgentName- " + agentName);
        }
        Map answer = new HashMap(2);
        try {
            // Get the agent's config and then it's attributes.
            ServiceConfig aCfg = svcConfig.getSubConfig(agentName);
            if (aCfg != null) {
                answer = aCfg.getAttributesForRead();
            }
        } catch (SMSException sme) {
            debug.error("AuthenticatedSharedAgentsCondition.getAttributes(): "
                + "Error occurred while getting " + agentName, sme);
            throw new PolicyException(sme.getMessage());
        }
        return (answer);
    }

    // Returns the organization configuration of the 'default' group
    // from AgentService.
    private ServiceConfig getOrgConfig(SSOToken token, String realmName) {

        if (debug.messageEnabled()) {
            debug.message("AuthenticatedSharedAgentsCondition."+
                "getOrgConfig() called. ");
        }
        ServiceConfig orgConfigCache = null;
        try {
            // Check in cache first
            if ((realmCache != null) && (!realmCache.isEmpty()) &&
                (realmCache.containsKey(realmName))) {
                orgConfigCache = (ServiceConfig) realmCache.get(realmName);
                if (orgConfigCache.isValid()) {
                    debug.message("AuthenticatedSharedAgentsCondition.getOrgConfig() found in cache.");
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
                debug.warning("AuthenticatedSharedAgentsCondition."+
                    "getOrgConfig(): Unable to get organization config "+
                     "due to " + smse);
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AuthenticatedSharedAgentsCondition."+
                    "getOrgConfig(): Unable to get organization config "+
                     "due to " + ssoe);
            }
        }
        return (orgConfigCache);
    }

    // Cache to store the agents configured for the "sharedAgentName".
    private static void updateCache(String sharedAgentUnivId, 
        Set agentsToRead) {
        Map nmap = new HashMap(2);
        nmap.putAll(sharedAgentsCache);
        nmap.put(sharedAgentUnivId, agentsToRead);
        sharedAgentsCache = nmap;
    }

    // Clears the cache where shared agent name and it's list of
    // agents to be read are stored.
    // Clears the cache where realm name and the service configuration
    // for the realm are stored.
    static void clearCache() {
        sharedAgentsCache = new HashMap(2);
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

    /*
     * Returns the permission for the shared agent to read the profile
     * other agents by comparing the agent from resource from env 
     * parameter and from the token's list.
     */
    private boolean getPermission(Set agentsFromEnv, Set agentsToRead) { 
       
        boolean allowed = false;

        if (agentsFromEnv != null && !agentsFromEnv.isEmpty()) {
            Set envValues = new CaseInsensitiveHashSet(agentsFromEnv);
            if (agentsToRead != null && !agentsToRead.isEmpty()) {
                for (Iterator itr = agentsToRead.iterator();itr.hasNext();) {
                    String avName = (String) itr.next();
                    if ((envValues != null) && (envValues.contains(avName))) {
                        allowed = true;
                        if (debug.messageEnabled()) {
                            debug.message("AuthenticatedSharedAgentsCondition."
                                + "getPermission(): returning true.");
                        }
                        break;
                    }
                }
            }
        }
        if (!allowed) {
            if (debug.messageEnabled()) {
                debug.message("AuthenticatedSharedAgentsCondition."
                    + "getPermission(): returning false.");
            }
        }
        return (allowed);
    }

    /* Splits the given resource name
     * @param res the resource name to be split
     * @return an array of (String) split resource names
     */
    public String[] split(String res) {
        StringTokenizer st = new StringTokenizer(res, delimiter);
        int n = st.countTokens();
        String[] retVal = new String[n];
        for (int i = 0; i < n; i++) {
            retVal[n-i-1] = st.nextToken();
        }
        return retVal;
    }
}
