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
 * $Id: PolicyCache.java,v 1.9 2010/01/10 01:19:35 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;
import com.sun.identity.policy.interfaces.PolicyListener;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.SMSException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.stats.Stats;
import com.sun.identity.shared.stats.StatsListener;
import java.util.*;

import com.sun.identity.shared.ldap.util.DN;

/**
 * The class <code>PolicyCache</code> manages policy cache 
 * for the policy framework.
 */
public class PolicyCache implements ServiceListener {

    public static final String POLICY_STATS = "amPolicyStats";

    static final Debug DEBUG = PolicyManager.debug;
    private static final String CACHE_KEY_DELIMITER = "/";
    private static final String POLICIES_COMPONENT = "/policies/";
    private static final String RESOURCES_COMPONENT = "/resources/";
    private static final String REALM_SUBJECTS_COMPONENT = "/realmsubjects";
    private static PolicyCache policyCache; //singleton instance

    /**
     *  orgName+policyName:Policy 
     */
    private Map policies = Collections.synchronizedMap(new HashMap());

    /**
     * orgName:PolicyManager 
     */
    private Map policyManagers = Collections.synchronizedMap(new HashMap());

    /**
     * serviceTypeName:<code>Set</code> of <code>PolicyListener</code>(s) 
     * Used to notify on policy changes
     * At present known implementations of interface of PolicyListener are
     * PolicyDecisionCacheListener and PolicyListenerRequest (from remote 
     * package)
     * @see com.sun.identity.policy.PolicyDecisionCacheListener
     * @see com.sun.identity.policy.remote.PolicyListenerRequest
     */
    private Map policyListenersMap = Collections.synchronizedMap(new HashMap());

    private ServiceConfigManager scm;
    private SSOToken token;

    /** Gets the singleton instance of PolicyCache
     *  @return the singleton instance of policy cache
     *  @throws PolicyException if error.
     */
    public synchronized static PolicyCache getInstance() throws PolicyException {
        if (policyCache == null) {
            if ( DEBUG.messageEnabled() ) {
                DEBUG.message("Creating singleton policy cache");
            }
            policyCache = new PolicyCache();
            try {
                policyCache.token = ServiceTypeManager.getSSOToken();
                policyCache.scm = new ServiceConfigManager(
                        PolicyManager.POLICY_SERVICE_NAME,
                        policyCache.token);
                if (!PolicyManager.isMigratedToEntitlementService()) {
                    policyCache.scm.addListener(policyCache);
                } else {
                    if ( DEBUG.messageEnabled() ) {
                        DEBUG.message("PolicyCache.getInstance():"
                                + " migrated to entilement service,  "
                                + " not registering notification listener with SMS");
                    }
                }
                
            } catch (SMSException smse) {
                DEBUG.error(ResBundleUtils.getString(
                    "can_not_create_policy_cache"), smse);
                throw new PolicyException(ResBundleUtils.rbName,
                        "can_not_create_policy_cache", null, smse);
            } catch (SSOException ssoe) {
                DEBUG.error(ResBundleUtils.getString(
                    "can_not_create_policy_cache"), ssoe);
                throw new PolicyException(ResBundleUtils.rbName,
                        "can_not_create_policy_cache", null, ssoe);
            }

            //Register policyStatsListener
            Stats policyStats = Stats.getInstance(POLICY_STATS);
            if (policyStats.isEnabled()) {
                StatsListener policyStatsListener 
                        = new PolicyStatsListener(policyStats);
                policyStats.addStatsListener(policyStatsListener);
                if ( DEBUG.messageEnabled() ) {
                    DEBUG.message("PolicyCache.getInstance():"
                            + " Registered PolicyStatsListener with "
                            + " Stats service");
                }
            }

        }
        return policyCache;
    }

	/**
         * Gets the singleton instance of PolicyCache.
	 * It takes into consideration of the SSOToken passed 
	 * to this method as a parameter
 	 */	


    synchronized static PolicyCache getInstance(SSOToken tok) throws PolicyException {
	if (policyCache == null) {
		if ( DEBUG.messageEnabled() ) {
			DEBUG.message("Creating singleton policy cache");
		}
		policyCache = new PolicyCache();
		try {
			policyCache.token = tok;
			policyCache.scm = new ServiceConfigManager(
				PolicyManager.POLICY_SERVICE_NAME,
				policyCache.token);
			policyCache.scm.addListener(policyCache);
		} catch (SMSException smse) {
			DEBUG.error(ResBundleUtils.getString(
				"can_not_create_policy_cache"), smse);
			throw new PolicyException(ResBundleUtils.rbName,
				"can_not_create_policy_cache", null, smse);
		} catch (SSOException ssoe) {
			DEBUG.error(ResBundleUtils.getString(
				"can_not_create_policy_cache"), ssoe);
			throw new PolicyException(ResBundleUtils.rbName,
				"can_not_create_policy_cache", null, ssoe);
		}

		//Register policyStatsListener
		Stats policyStats = Stats.getInstance(POLICY_STATS);
		if (policyStats.isEnabled()) {
			StatsListener policyStatsListener
				= new PolicyStatsListener(policyStats);
			policyStats.addStatsListener(policyStatsListener);
			if ( DEBUG.messageEnabled() ) {
				DEBUG.message("PolicyCache.getInstance():"
				+ " Registered PolicyStatsListener with "
				+ " Stats service");
			}
		}

	}
	return policyCache;
    }


    /** No argument constructor 
     */
    private PolicyCache() {
    }

    /** Gets a policy given organization name and policy name
     *  @param orgName name of the organization under which the 
     *         policy is defined
     *  @param policyName policy name
     *  @return policy with the given name under the given organization
     */
    Policy getPolicy(String orgName, String policyName) {
        String cacheKey = buildCacheKey(orgName, policyName);
        if ( DEBUG.messageEnabled() ) {
            StringBuilder sb = new StringBuilder(100);
            sb.append("at PolicyCache.getPolicy(orgName,policyName):");
            sb.append("orgName=").append(orgName).append(":")
                    .append("policyName=").append(policyName)
                    .append("cacheKey=").append(cacheKey);
            DEBUG.message(sb.toString());
        }
        return getPolicy(cacheKey);
    }

    /** Gets a policy from the cache for the given the cacheKey. 
     *  If the policy is not in the cache, it would be read from 
     *  the data store.
     *  @param cacheKey cache key
     *  @return policy cached with the given cache key
     */
    Policy getPolicy(String cacheKey) {
        Policy policy = null;
        if ( DEBUG.messageEnabled() ) {
            DEBUG.message("PolicyCache:cacheKeys in cache:" 
                    + policies.keySet());
        }
        if ( policies.containsKey(cacheKey) ) {
            policy = (Policy) policies.get(cacheKey);
            if ( DEBUG.messageEnabled() && (policy == null) ) {
                DEBUG.message("PolicyCache:returning null policy from cache "
                        + "for key:" 
                        + cacheKey);
            }
        } else {
            if ( DEBUG.messageEnabled() ) {
                DEBUG.message("PolicyCache:refreshing policy for cache key:" 
                        + cacheKey);
            }
            policy = refreshPolicy(cacheKey);
        }
        return policy;
    }

    /** Refreshes a policy into the cache for the given cacheKey. 
     *  The policy is read from the data store and put in the cache.
     *  @param cacheKey cache key
     *  @return the refreshed policy
     */
    Policy refreshPolicy(String cacheKey) {
        String[] cacheKeyTokens = tokenizeCacheKey(cacheKey);
        Policy policy = null;
        String orgName = cacheKeyTokens[5];
        String policyName = cacheKeyTokens[1];
        if ( DEBUG.messageEnabled() ) {
            StringBuilder sb = new StringBuilder(500);
            sb.append("at PolicyCache.refreshPolicy refreshing policy for - ")
                 .append("cacheKey=").append(cacheKey).append(":")
                 .append("policyName=").append(policyName).append(":")
                 .append("orgName=").append(orgName);

            DEBUG.message(sb.toString());
        }
        try {
            PolicyManager pm = getPolicyManager(orgName);
            policy = pm.getPolicy(policyName);
        } catch (PolicyException pe) {
            String[] objs = { cacheKey};
            DEBUG.error(ResBundleUtils.getString(
                "can_not_refresh_policy_for_cachekey", objs), pe);
        } catch (SSOException ssoe) {
            String[] objs = { cacheKey};
            DEBUG.error(ResBundleUtils.getString(
                "can_not_refresh_policy_for_cachekey", objs), ssoe);
        }
        if ( policy == null ) {
            DEBUG.error("refreshed policy is null for cache key : " + cacheKey);
        } else {
            policies.put(cacheKey, policy);
            policy.initializeEvaluationWeights();
        }
        return policy;
    }

    /** Adds a policy listener. The policy listener would be notified
     *  whenever a policy is added/removed/changed.
     *  @param policyListener <code>PolicyListener</code> to add
     */
    void addPolicyListener(PolicyListener policyListener) {
        String listenerServiceName = policyListener.getServiceTypeName();
        Set newListeners = new HashSet();
        newListeners.add(policyListener);
        Set oldListeners = (Set) policyListenersMap.get(listenerServiceName);
        if ( oldListeners != null ) {
            newListeners.addAll(oldListeners);
        }
        policyListenersMap.put(listenerServiceName, newListeners);
    }

    /** Removes a policy listener
     *  @param policyListener policy listener to remove
     */
    void removePolicyListener(PolicyListener policyListener) {
        String listenerServiceName = policyListener.getServiceTypeName();
        Set oldListeners = (Set) policyListenersMap.get(listenerServiceName);
        if ( oldListeners != null ) {
            Set newListeners = new HashSet();
            newListeners.addAll(oldListeners);
            newListeners.remove(policyListener);
            policyListenersMap.put(listenerServiceName, newListeners);
        }
    }

    /**
     * This method will be invoked when a service's schema has been changed.
     *
     * @param serviceName name of the service
     * @param version version of the service
     */
    public void schemaChanged(String serviceName, String version) {
        //NO-OP
    }

    /**
     * This method will be invoked when a service's global configuation
     * data has been changed. The parameter groupName denote the name
     * of the configuration grouping (e.g. default) and serviceComponent
     * denotes the service's sub-component that changed
     * (e.g. /NamedPolicy, /Templates).
     * 
     * @param serviceName name of the service
     * @param version version of the service
     * @param serviceComponent name of the service components that
     *                          changed
     */
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int changeType) {
        //NO-OP
    }

    /**
     * This method will be invoked when a service's organization
     * configuation data has been changed. The parameters orgName,
     * groupName and serviceComponent denotes the organization name,
     * configuration grouping name and
     * service's sub-component that are changed respectively.
     * 
     * @param serviceName name of the service
     * @param version version of the service
     * @param groupName
     * @param orgName organization name as DN
     * @param serviceComponent the name of the service components that
     *                          changed
     */
    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, 
            int changeType) {
        if ( DEBUG.messageEnabled() ) {
            StringBuilder sb = new StringBuilder(255);
            sb.append("Received SMS notification, orgConfigChanged");
            sb.append("serviceName, version, orgName, groupName, ");
            sb.append(" serviceComponent, changeType:");
            sb.append(serviceName).append(":").append(version).append(":");
            sb.append(orgName).append(":").append(groupName).append(":");
            sb.append(serviceComponent).append(":").append(changeType);
            DEBUG.message(sb.toString());
        }
        Policy oldPolicy = null;
        Policy newPolicy = null;
        if ( serviceComponent.startsWith(POLICIES_COMPONENT)) {
            String cacheKey = buildCacheKey(serviceName, version,
                    orgName, groupName, serviceComponent);
            if ( changeType == ServiceListener.ADDED) { // 1
                if ( DEBUG.messageEnabled() ) {
                    DEBUG.message ("SMS Notification- policy added -"
                            + "Refreshing policy for cacheKey=" 
                            + cacheKey);
                }
                newPolicy = refreshPolicy(cacheKey);
            } else if ( changeType == ServiceListener.REMOVED ) { // 2
                if ( DEBUG.messageEnabled() ) {
                    DEBUG.message ("SMS Notification- policy removed -"
                            + " cacheKey=" 
                            + cacheKey);
                }
                oldPolicy=  (Policy) policies.get(cacheKey);
                policies.put(cacheKey,  null);
            } else if ( changeType == ServiceListener.MODIFIED ) { // 4
                oldPolicy=  (Policy) policies.get(cacheKey);
                if ( DEBUG.messageEnabled() ) {
                    DEBUG.message ("SMS Notification- policy modified -"
                            + "Refreshing policy for cacheKey=" 
                            + cacheKey);
                }
                newPolicy = refreshPolicy(cacheKey);
            } else { //unsupported notification type 
                DEBUG.error ("SMS Notification- unsupported change type : "
                        + changeType);
            }

            sendPolicyChangeNotification(oldPolicy, newPolicy, changeType);
        }

        if ( serviceComponent.startsWith(RESOURCES_COMPONENT)) {
            try {
                PolicyManager pm = getPolicyManager(orgName);
                String resourceTypeName =
                        serviceComponent.substring("/resources".length() + 1);
                if ( DEBUG.messageEnabled() ) {
                    DEBUG.message ("SMS Notification- resource index modified-"
                            + "clearing index for resource type "
                            + resourceTypeName);
                }
                pm.getResourceIndexManager().
                    clearResourceIndex(resourceTypeName);
                // catch and log PolicyException, SSOException 
            } catch (PolicyException pe) {
                    DEBUG.error( "error while clearing resource index ", pe );
            } catch (SSOException ssoe) {
                    DEBUG.error(ResBundleUtils.getString("invalid_sso_token"), 
                            ssoe );
            }
        }

        if ( serviceComponent.startsWith(REALM_SUBJECTS_COMPONENT)) {
            if ( DEBUG.messageEnabled() ) {
                DEBUG.message ("SMS Notification- realm subjects modified "
                        + "- resetting realm subjects for orgName:"
                        + orgName);
            }
            realmSubjectsChanged(orgName);
        }
    }

    public void sendPolicyChangeNotification(
        Policy oldPolicy,
        Policy newPolicy,
        int changeType
    ) {
        for (Iterator i = policyListenersMap.keySet().iterator(); i.hasNext();
        ) {
            String listenerServiceName = (String) i.next();
            Set affectedResourceNames = new HashSet();
            try {
                if (oldPolicy != null) {
                    affectedResourceNames.addAll(oldPolicy.getResourceNames(
                        listenerServiceName));
                }
                if (newPolicy != null) {
                    affectedResourceNames.addAll(newPolicy.getResourceNames(
                        listenerServiceName));
                }
            } catch (SSOException ssoe) {
                DEBUG.error(ResBundleUtils.getString("invalid_sso_token"),
                    ssoe);
            } catch (NameNotFoundException nnfe) {
                String objs[] = {listenerServiceName};
                DEBUG.error(ResBundleUtils.getString(
                    "service_name_not_found", objs), nnfe);
            }
            if (!affectedResourceNames.isEmpty()) {
                firePolicyChanged(listenerServiceName,
                    affectedResourceNames, changeType);
            }
        }
    }

    /**
     * Creates a String representation of cache key used to index
     * policy cache using serviceName, version, orgName, groupName
     * and serviceComponent. 
     * The format is like:
     * serviceComponent/groupName/version/serviceName/orgName
     * where "/" is defined in key CACHE_KEY_DELIMITER
     * @see #CACHE_KEY_DELIMITER
     */

    private String buildCacheKey(String serviceName, String version,
            String orgName, String groupName, String serviceComponent) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(serviceComponent).append(CACHE_KEY_DELIMITER);
        sb.append(groupName).append(CACHE_KEY_DELIMITER);
        sb.append(version).append(CACHE_KEY_DELIMITER);
        sb.append(serviceName).append(CACHE_KEY_DELIMITER);
        sb.append(orgName);
        return sb.toString().toLowerCase();
    }

    /**
     * Creates a String representation of cache key used to index
     * policy cache orgName and  policyName
     * The format is like:
     * serviceComponent/groupName/version/serviceName/orgName
     * <p>where "/" is defined in key CACHE_KEY_DELIMITER</p>
     * <p>serviceComponent defaults to "/Policies/<code>policyName</code>"</p>
     * <p>serviceName defaults to "iPlanetAMPolicyService"</p>
     * <p>version defaults to "1.0"</p>
     * <p>groupName defaults to "default"</p>
     * @see #CACHE_KEY_DELIMITER
     */
    private String buildCacheKey(String orgName, String policyName) {
        String serviceComponent =   CACHE_KEY_DELIMITER 
                + PolicyManager.NAMED_POLICY 
                + CACHE_KEY_DELIMITER + policyName;
        return buildCacheKey(PolicyManager.POLICY_SERVICE_NAME, 
                PolicyManager.POLICY_VERSION, orgName, "default", 
                serviceComponent);
    }

    /**
     * Returns an Array of Strings representing the cache key
     */

    private String[] tokenizeCacheKey(String cacheKey) {
        String[] tokens = new String[6];
        StringTokenizer st = new StringTokenizer(cacheKey, 
                CACHE_KEY_DELIMITER);
        //ou=policy1,ou=policies
        tokens[0] = st.nextToken(); //policies
        tokens[1] = st.nextToken(); //policy1
        tokens[2] = st.nextToken(); //groupName(default)
        tokens[3] = st.nextToken(); //version
        tokens[4] = st.nextToken(); //serviceName
        tokens[5] = st.nextToken(); //orgName
        return tokens;
    }

    /**
     * Creates a policyEvent with the changed resource names
     * and then invokes all the registered PolicyListeners
     * to notify about the event.
     */

    private void firePolicyChanged(String serviceName, 
            Set affectedResourceNames, int changeType) {
        if( DEBUG.messageEnabled() ) {
            StringBuilder sb = new StringBuilder(255);
            sb.append( 
                   "at firePolicyChanged(serrviceName,affectedResourceNames):");
            sb.append(serviceName).append(":");
            sb.append(affectedResourceNames.toString());
            DEBUG.message(sb.toString());
        }
        PolicyEvent policyEvent = new PolicyEvent();
        policyEvent.setResourceNames(affectedResourceNames);
        policyEvent.setChangeType(changeType);
        Set pListeners = (Set) policyListenersMap.get(serviceName);
        if ( pListeners != null ) {
            Iterator listeners = pListeners.iterator();
            while ( listeners.hasNext() ) {
                PolicyListener policyListener = 
                    (PolicyListener) listeners.next();
                try {
                    policyListener.policyChanged(policyEvent);
                } catch (Exception e) {
                    DEBUG.error("policy change not handled properly", e);
                }
            }
        }

        //notify policy evaluator so it can clean up cached resource names
        PolicyEvaluator.policyChanged(serviceName, policyEvent);
    }

    /**
     * Creates a policyEvent with the changed resource names
     * and then invokes all the registered PolicyListeners
     * to notify about the event. This is triggered when
     * entitlement privilege is added, removed or modified.
     */

    public void firePrivilegeChanged(String serviceName, 
            Set affectedResourceNames, int changeType) {
        if( DEBUG.messageEnabled() ) {
            StringBuilder sb = new StringBuilder();
            sb.append( 
                   "at firePrivilegeChanged(serrviceName,affectedResourceNames):");
            sb.append(serviceName).append(":");
            sb.append(affectedResourceNames.toString());
            DEBUG.message(sb.toString());
        }
        PolicyEvent policyEvent = new PolicyEvent();
        policyEvent.setResourceNames(affectedResourceNames);
        policyEvent.setChangeType(changeType);
        Set pListeners = (Set) policyListenersMap.get(serviceName);
        if ( pListeners != null ) {
            Iterator listeners = pListeners.iterator();
            while ( listeners.hasNext() ) {
                PolicyListener policyListener = 
                    (PolicyListener) listeners.next();
                try {
                    policyListener.policyChanged(policyEvent);
                } catch (Exception e) {
                    DEBUG.error("policy change not handled properly", e);
                }
            }
        }

        //notify policy evaluator so it can clean up cached resource names
        PolicyEvaluator.policyChanged(serviceName, policyEvent);
    }

    /** Gets a policy manager for the given organization name
     *  PolicyCache maintains a cache of policy managers to improve 
     *  performance during policy evaluation. This methods returns 
     *  the policy mananger for the given organization name from the
     *  cache 
     *  @param orgName name of the organization for which to get the
     *                 policy manager
     *  @return policy manager for the organization
     *  @throws PolicyException
     *  @throws SSOException
     */
    PolicyManager getPolicyManager(String orgName) throws PolicyException, 
        SSOException 
    {
        orgName = new DN(orgName).toRFCString().toLowerCase();
        PolicyManager pm = (PolicyManager) policyManagers.get(orgName);
        if ( pm == null ) {
            pm = new PolicyManager(token, orgName);
            policyManagers.put(orgName, pm);
            if( DEBUG.messageEnabled() ) {
                StringBuilder sb = new StringBuilder(255);
                sb.append( 
                        "at PolicyCache.getPolicyManager():");
                sb.append("creating and caching pm for orgname ");
                sb.append(orgName);
                DEBUG.message(sb.toString());
            }
        }
        return pm;
    }

    /**
     * For the organization with name <code>orgName</code> 
     * finds all the cached policies and calls methods
     * on them to refresh policy config data.
     */

    void policyConfigChanged(String orgName) {
        String pattern = CACHE_KEY_DELIMITER + orgName;
        if( DEBUG.messageEnabled() ) {
            StringBuilder sb = new StringBuilder(255);
            sb.append( 
                    "at PolicyCache.policyConfigChanged():");
            sb.append("updating policy config for orgname ");
            sb.append(orgName);
            DEBUG.message(sb.toString());
        }
        try {
            PolicyManager pm = getPolicyManager(orgName);
            Map policyConfig = pm.getPolicyConfig();
            if ((policyConfig != null) && (!policyConfig.isEmpty())) {
                Set cacheKeys = policies.keySet();
                String[] clonedCacheKeys = {};
                synchronized(policies) {
                    clonedCacheKeys = new String[cacheKeys.size()];
                    int i = 0;
                    Iterator cacheIter = cacheKeys.iterator();
                    while ( cacheIter.hasNext() ) {
                        clonedCacheKeys[i] = (String)cacheIter.next();
                        i++;
                    }
                }
                int length = clonedCacheKeys.length;
                for( int i = 0; i < length; i++) {
                    String cacheKey = clonedCacheKeys[i];
                    if ( cacheKey.endsWith(pattern) ) {
                        Policy policy = 
                            (Policy) policyCache.getPolicy(cacheKey);
                        if ( policy!= null ) {
                            policy.getSubjects().setPolicyConfig(policyConfig);
                        }
                    }
                }
            }
        } catch (NameNotFoundException nnfe) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("Can not set policy config for orgname:" 
                        + orgName + ":" + nnfe.getMessage());
            }
        } catch (PolicyException pe) {
            DEBUG.error("Can not set policy config for orgname:" + orgName, pe);
        } catch (SSOException se) {
            DEBUG.error("Can not set policy config for orgname:" + orgName, se);
        }
    }

    void realmSubjectsChanged(String orgName) {
        DEBUG.message("resetting realm subjects");
        try {
            PolicyManager pm = getPolicyManager(orgName);
            SubjectTypeManager stm = pm.getSubjectTypeManager();
            synchronized(stm) {
                stm.resetRealmSubjects();
            }
        } catch (PolicyException pe) {
            DEBUG.error("Can not reset realmSubjects for orgname:" 
                + orgName, pe);
        } catch (SSOException se) {
            DEBUG.error("Can not reset realmSubjects for orgname:" 
                + orgName, se);
        }

    }

    /**
     * Prints the stats for policies policyManagers, policyListenerMap
     */

    static void printStats(Stats policyStats) {

        /* record stats for policies,  policyManagers, 
         * policyListenersMap
         */

        policyStats.record("PolicyCache: Number of policies in cache: " 
                + policyCache.policies.size());

        policyStats.record("PolicyCache: Number of policyManagers in cache:"
                + policyCache.policyManagers.size());

        policyStats.record("PolicyCache: Number of service names in "
                + " policyListeners cache: " 
                + policyCache.policyListenersMap.size());
    }

    /**
     * Clears the cached membership evaluation results corresponding
     * to the <code>tokenIdString</code>. This is triggered through
     * <code>PolicySSOTokenListener</code> when session property
     * of a logged in user is changed. This call delegates to each
     * cached Policy. Each Policy in turn clears the cached
     * membership evaluation results
     *
     * @param tokenIdString sessionId of the user whose session property changed
     */
    void clearSubjectResultCache(String tokenIdString) throws PolicyException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("PolicyCache.clearSubjectResultCache(tokenIdString): "
                    + " clearing cached subject evaluation result for "
                    + " tokenId XXXXX in each cached Policy");
        }
        Set policyNames = new HashSet();
        policyNames.addAll(policies.keySet());
        for (Iterator iter = policyNames.iterator(); iter.hasNext();) {
            Policy policy = (Policy)policies.get(iter.next());
            if (policy != null) {
                policy.clearSubjectResultCache(tokenIdString);
            }
        }
    }

}

