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
 * $Id: PolicyEvaluator.java,v 1.19 2010/01/14 23:18:35 dillidorai Exp $
 *
 * Portions copyright 2011-2013 ForgeRock, Inc.
 */
package com.sun.identity.policy;

import java.util.Set;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.util.Cache;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.stats.Stats;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.sso.SSOException;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.SsoServerPolicySvcImpl;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.PolicyListener;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.DNMapper;
import java.security.AccessController;
import java.security.Principal;
import java.util.List;
import javax.security.auth.Subject;

/**
 * The class <code>PolicyEvaluator</code> evaluates policies
 * and provides policy decisions.
 * @supported.api
 */
public class PolicyEvaluator {

    /**
     * Constant used to identity all the resources of a service type. 
     * The resources include the sub resources of all resource prefixes of 
     * resource type
     *
     * @supported.api
     */
    public static final String ALL_RESOURCES 
            = "---ALL_RESOURCES---";

    public static final String ADVICING_ORGANIZATION 
            = "AdvicingOrganization";

    /**
     * Constant used to identity empty resource
     *
     * @supported.api
     */
    public static final String EMPTY_RESOURCE_NAME = "";

    /**
     * Constant used for key to pass the requested resource name canonicalized
     * in the env map, so that Condition(s)/ResponseProvider(s) could use 
     * the requested resource name, if necessary
     */
    public static final String SUN_AM_REQUESTED_RESOURCE 
            = "sun.am.requestedResource";

    /**
     * Constant used for key to pass the requested resource name uncanonicalized
     * in the env map, so that Condition(s)/ResponseProvider(s) could use 
     * the requested resource name, if necessary
     */
    public static final String SUN_AM_ORIGINAL_REQUESTED_RESOURCE 
            = "sun.am.requestedOriginalResource";

    /**
     * Constant used for key to pass the requested actions names
     * in the env map, so that Condition(s)/ResponseProvider(s) could use 
     *  the requested actions names, if necessary
     */
    public static final String SUN_AM_REQUESTED_ACTIONS 
            = "sun.am.requestedActions";

    /**
     * Constant used for key to pass the PolicyConfig configuration map 
     * in the env map, so that Condition(s)  could use 
     * the <code>PolicyConfig</code> config map, if necessary.
     * <code>LDAPFilterCondition</code> needs to use PolicyConfig config map
     *
     *
     */
    public static final String SUN_AM_POLICY_CONFIG = "sun.am.policyConfig";

    public static final String RESULTS_CACHE_SESSION_CAP 
            = "com.sun.identity.policy.resultsCacheSessionCap";

    public static int DEFAULT_RESULTS_CACHE_SESSION_CAP = 1000;

    public static int resultsCacheSessionCap = DEFAULT_RESULTS_CACHE_SESSION_CAP;

    public static final String RESULTS_CACHE_RESOURCE_CAP 
            = "com.sun.identity.policy.resultsCacheResourceCap";

    public static int DEFAULT_RESULTS_CACHE_RESOURCE_CAP = 100;

    public static int resultsCacheResourceCap = DEFAULT_RESULTS_CACHE_RESOURCE_CAP;

    private static final Debug DEBUG = PolicyManager.debug;
    private static final boolean USE_POLICY_CACHE = true;
    private static final boolean INCLUDE_SUPER_RESOURCE_POLCIES = true;
    private static final long DEFAULT_USER_NSROLE_CACHE_TTL = 600000;

    private String orgName;
    private String realm;
    private String serviceTypeName;
    private ServiceType serviceType;
    private PolicyCache policyCache;
    private PolicyManager policyManager;
    private ResourceIndexManager resourceIndexManager;

    private HashMap booleanActionNameTrueValues; //cache
    private HashMap booleanActionNameFalseValues; //cache
    private Set actionNames; //all action names valid for the serviceType
    private Set orgNames = new HashSet(); // to pass org name in envParameters
    // used to pass service type name in envParameters
    private Set serviceTypeNames = new HashSet(); 
    // listener for policy decision cache
    private PolicyDecisionCacheListener listener = null; 

    /*
     * Cache to keep the policy evaluation results
     * Cache structure layout:
     * 
     * cache ----> Servicename1
     *       ----> servicename2
     *        ...  
     *       ----> servicenameN
     *
     * servicenameI ----> Resourcename1
     *              ----> Resourcename2
     *              ...
     *              ----> ResourcenameN
     *
     * resourcenameI ----> userssotokenidstring1
     *               ----> userssotokenidstring2
     *               ...
     *               ----> userssotokenidstringN
     *
     * userssotokenidstringI ----> requestscope1
     *                       ----> requestscope2
     *
     * requestscope1 ----> resourceresult1
     * requestscope2 ----> resourceresult2
     */
    static Map policyResultsCache = new HashMap();

    /*
     * The sso token listener registry for policy decision cache. 
     * To avoid adding multiple sso token listeners for the same 
     * token, we use this registry to make sure the listener is 
     * registered only once for each token. It will be unregistered 
     * if token is expired. 
     *
     * Key is tokenId and value is policySSOTokenListener
     * ssoTokenIDString : PolicySSOTokenListener
     *
     * Used to clean up cache on ssoToken notifications
     */
    public static Map ssoListenerRegistry =
              Collections.synchronizedMap(new HashMap());

    /*
     * The policy change listener registry for policy decision cache.
     * To avoid adding multiple listeners for the same service, we 
     * use this registry to make sure the listener is registered only 
     * once for each service. 
     *
     * Key is serviceTypeName and value is <code>PolicyDecisionCacheListener
     * </code>
     * serviceTypeName : PolicyDecisionCacheListener for service type
     *
     * Used to clean up the decision cache on policy change notification
     */
    private static Map policyListenerRegistry =
              Collections.synchronizedMap(new HashMap());

    /**
     * The user <code>nsRole</code> attribute cache.
     * AMSDK cache stops caching a user's nsRole attribute in 6.2
     * due to notification issue. Adding this cache in policy to 
     * avoid performance impact caused by the AMSDK change. This 
     * cache uses a user's token as the key to map to the user's
     * <code>nsRole</code> attribute values.
     *
     * Key is tokenId and value is set of role DN(s)
     * ssoTokenIDString : set of role DN(s)
     */
    static Map userNSRoleCache = 
              Collections.synchronizedMap(new HashMap());

    // TTL value for entries in the user's nsRole attribute values.
    private static long userNSRoleCacheTTL = 0;

    /**
     * listener object to be used in cleaning up the
     * userNSRoleCache, subjectEvaluationCache , user role
     * cache in LDAPRoles and policyResultsCache 
     * upon user token  expiration.
     */
    public static SSOTokenListener ssoListener = 
                                 new PolicySSOTokenListener(); 
               
    /*
     * Cache for sub resources keyed by resource name 
     * The structure is a Map of
     * serviceType(String) : resourceNamesCache(Cache)
     * Key for resourceNamesCache is a root resource name and value is
     * a <code>Set</code> of sub resource names for the root resource name
     *
     * serviceType: resourceName : resourceNames
     */
    private static Map resourceNamesMap = new HashMap(); 

    /**
     * Constant key for passing organization name in the environment map during
     * policy evaluation. The value for the key would be a <code>Set</code> 
     * with one element of type String. The string is the name of the 
     * organization the policy evaluator has been instantiated for.
     */
     static final String ORGANIZATION_NAME = "organizationName";

    /**
     * Constant key for passing service type name in the environment map during
     * policy evaluation. The value for the key would be a <code>Set</code> 
     * with one element of type String. The string is the name of the 
     * <code>ServiceType</code> the  policy evaluator has been instantiated for.
     */
     static final String SERVICE_TYPE_NAME = "serviceTypeName";

     static final Object lock = new Object();

    /**
     * Constructor to create a <code>PolicyEvaluator</code> given the <code>
     * ServiceType</code> name.
     *
     * @param serviceTypeName the name of the <code>ServiceType</code> for 
     * which this evaluator can be used.
     * @throws SSOException if <code>SSOToken</code> used by
     *                      <code>PolicyEvaluator</code> is invalid
     * @throws NameNotFoundException if the service with name 
     *                      <code>serviceTypeName</code> is not found
     * @throws PolicyException for any other abnormal condition
     *
     * @supported.api
     */
    public PolicyEvaluator(String serviceTypeName)
        throws SSOException, NameNotFoundException, PolicyException {

        this("", serviceTypeName);

        /* Register a policy listener for updating policy decision 
         * cache if there is none already registered
         */
        synchronized (lock) {
            if (!(policyListenerRegistry.containsKey(serviceTypeName))) {
                listener = new PolicyDecisionCacheListener(serviceTypeName);
                try {
                    PolicyCache.getInstance().addPolicyListener(listener);
                } catch (PolicyException pe) {
                    DEBUG.error("PolicyEvaluator: registering policy decision "
                            + " cache listener failed");
                }
                policyListenerRegistry.put(serviceTypeName, listener);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("PolicyEvaluator:policy listener for service "
                            + serviceTypeName + " added");
                }
            } else {
                listener = (PolicyDecisionCacheListener)
                    policyListenerRegistry.get(serviceTypeName);
            }
        }
    }

    /**
     * Constructor to create a <code>PolicyEvaluator</code> given organization
     * name and the <code>ServiceType</code> name.
     *
     * @param orgName the name of the organization under which the evaluation
     * is being done
     * @param serviceTypeName the name of the <code>ServiceType</code> for 
     * which this evaluator can be used.
     */
    public PolicyEvaluator(String orgName, String serviceTypeName)
            throws SSOException, PolicyException, NameNotFoundException {
        
        if ( (orgName == null) || (orgName.equals("/")) 
                    || (orgName.length() == 0) ) {
            orgName = ServiceManager.getBaseDN();
        } else {
            orgName = com.sun.identity.sm.DNMapper.orgNameToDN(orgName);
        }
        this.orgName = orgName;

        this.realm = com.sun.identity.sm.DNMapper.orgNameToRealmName(orgName);
        this.serviceTypeName = serviceTypeName;

        this.policyCache = PolicyCache.getInstance();

        ServiceTypeManager stm = ServiceTypeManager.getServiceTypeManager();
        serviceType = stm.getServiceType(serviceTypeName);
        policyManager = policyCache.getPolicyManager(orgName);
        this.orgNames.add(policyManager.getOrganizationDN());
        this.serviceTypeNames.add(serviceTypeName);
        resourceIndexManager = policyManager.getResourceIndexManager();

        String resultsCacheSessionCapString 
                = SystemProperties.get(RESULTS_CACHE_SESSION_CAP);
        if (resultsCacheSessionCapString != null) {
            try {
                resultsCacheSessionCap 
                        = Integer.parseInt(resultsCacheSessionCapString);
            } catch (NumberFormatException nfe) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning("PolicyEvaluator:"
                            + "number format exception: "
                            + "defaulting resultsCacheSessionCap to "
                            + DEFAULT_RESULTS_CACHE_SESSION_CAP);
                }
                resultsCacheSessionCap = DEFAULT_RESULTS_CACHE_SESSION_CAP;
            }
        } else {
            if (PolicyManager.debug.warningEnabled()) {
                PolicyManager.debug.warning("PolicyEvaluator:"
                        + "resultsCacheSessionCap not specified, "
                        + "defaulting resultsCacheSessionCap to "
                        + DEFAULT_RESULTS_CACHE_SESSION_CAP);
            }
            resultsCacheSessionCap = DEFAULT_RESULTS_CACHE_SESSION_CAP;
        }
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message("PolicyEvaluator:"
                    + "resultsCacheSessionCap=" + resultsCacheSessionCap);
        }

        String resultsCacheResourceCapString 
                = SystemProperties.get(RESULTS_CACHE_RESOURCE_CAP);
        if (resultsCacheResourceCapString != null) {
            try {
                resultsCacheResourceCap 
                        = Integer.parseInt(resultsCacheResourceCapString);
            } catch (NumberFormatException nfe) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning("PolicyEvaluator:"
                            + "number format exception: "
                            + "defaulting resultsCacheResourceCap to "
                            + DEFAULT_RESULTS_CACHE_RESOURCE_CAP);
                }
                resultsCacheResourceCap = DEFAULT_RESULTS_CACHE_RESOURCE_CAP;
            }
        } else {
            if (PolicyManager.debug.warningEnabled()) {
                PolicyManager.debug.warning("PolicyEvaluator:"
                        + "resultsCacheResourceCap not specified, "
                        + "defaulting resultsCacheResourceCap to "
                        + DEFAULT_RESULTS_CACHE_RESOURCE_CAP);
            }
            resultsCacheResourceCap = DEFAULT_RESULTS_CACHE_RESOURCE_CAP;
        }
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message("PolicyEvaluator:"
                    + "resultsCacheResourceCap=" + resultsCacheResourceCap);
        }

    }        
    
    /**
     * Evaluates a simple privilege of boolean type. The privilege indicate
     * if the user can perform specified action on the specified resource.
     * Invoking this method would result in <code>PolicyException</code>,
     * if the syntax for the <code>actionName</code> is not declared to be
     * boolean, in the service schema.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     * @param actionName name of the action the user is trying to perform on
     * the resource
     *
     * @return the result of the evaluation as a boolean value
     *
     * @exception SSOException single-sign-on token invalid or expired
     * 
     */
    public boolean isAllowed(SSOToken token, String resourceName,
        String actionName) throws PolicyException, SSOException {
        return (isAllowed(token, resourceName, actionName, 
                new HashMap()));
    }

    /**
     * Evaluates simple privileges of boolean type. The privilege indicate
     * if the user can perform specified action on the specified resource.
     * The evaluation depends on user's application environment parameters.
     * Invoking this method would result in <code>PolicyException</code>,
     * if the syntax for the <code>actionName</code> is not declared to be
     * boolean, in the service schema.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     * @param actionName name of the action the user is trying to perform on
     * the resource
     * @param envParameters run-time environment parameters
     *
     * @return the result of the evaluation as a boolean value
     *
     * @throws SSOException single-sign-on token invalid or expired
     * @throws PolicyException for any other abnormal condition
     * 
     * @supported.api
     */
    public boolean isAllowed(SSOToken token, String resourceName,
        String actionName, Map envParameters) throws SSOException,
        PolicyException {
        if (PolicyManager.isMigratedToEntitlementService()) {
            return isAllowedE(token, resourceName, actionName, envParameters);
        }
        return isAllowedO(token, resourceName, actionName, envParameters);
    }

    public boolean isAllowedO(SSOToken token, String resourceName,
            String actionName, Map envParameters) throws SSOException,
            PolicyException {

        ActionSchema schema = serviceType.getActionSchema(actionName);

        // Cache the false values for the action names
        if (booleanActionNameFalseValues == null) {
            booleanActionNameFalseValues = new HashMap(10);
        }
        String falseValue = null;
        if ((falseValue = (String)
                booleanActionNameFalseValues.get(actionName)) == null) {
            falseValue = schema.getFalseValue();
            // Add it to the cache
            booleanActionNameFalseValues.put(actionName, falseValue);
        }


        // Cache the true values for the action names
        if (booleanActionNameTrueValues == null) {
            booleanActionNameTrueValues = new HashMap(10);
        }

        String trueValue = null;
        if ((trueValue = (String)
                booleanActionNameTrueValues.get(actionName)) == null) {
            trueValue = schema.getTrueValue();
            // Add it to the cache
            booleanActionNameTrueValues.put(actionName, trueValue);
        }

        if (!AttributeSchema.Syntax.BOOLEAN.equals(schema.getSyntax())) {
            String objs[] = {actionName};
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "action_does_not_have_boolean_syntax", objs, null);
        }

        boolean actionAllowed = false;
        HashSet actionNames = new HashSet(2);
        actionNames.add(actionName);
        PolicyDecision policyDecision = getPolicyDecision(token, resourceName,
                                   actionNames, envParameters);
        ActionDecision actionDecision =
                (ActionDecision) policyDecision.getActionDecisions()
                .get(actionName);

        if ( actionDecision != null ) {
            Set set = (Set) actionDecision.getValues();
            if ( (set != null) ) {
                if ( set.contains(falseValue) ) {
                    actionAllowed = false;
                } else if ( set.contains(trueValue) ) {
                    actionAllowed = true;
                }
            }
        }

        return actionAllowed;
    }

    private void padEnvParameters(SSOToken token, String resourceName,
        String actionName, Map envParameters) throws PolicyException, SSOException {
        if ((resourceName == null) || (resourceName.trim().length() == 0)) {
            resourceName = Rule.EMPTY_RESOURCE_NAME;
        }

        Set originalResourceNames = new HashSet(2);
        originalResourceNames.add(resourceName);

        String realmName = (DN.isDN(realm)) ?
            DNMapper.orgNameToRealmName(realm) : realm;
        try {
            Application appl = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject,
                realmName, serviceTypeName);
            resourceName = appl.getResourceComparator().canonicalize(
                resourceName);
        } catch (EntitlementException e) {
            throw new PolicyException(e);
        }
        //Add request resourceName and request actionNames to the envParameters
        //so that Condition(s)/ResponseProvider(s) can use them if necessary
        Set resourceNames = new HashSet(2);
        resourceNames.add(resourceName);

        Set actions = new HashSet();
        if (actionName != null) {
            actions.add(actionName);
        } else {
            Set actionNames = serviceType.getActionNames();
            if (actionNames != null) {
                actions.addAll(actionNames);
            }
        }

        envParameters.put(SUN_AM_REQUESTED_RESOURCE, resourceNames);
        envParameters.put(SUN_AM_ORIGINAL_REQUESTED_RESOURCE,
                originalResourceNames);
        envParameters.put(SUN_AM_REQUESTED_ACTIONS, actions);
        envParameters.put(SUN_AM_POLICY_CONFIG,
            policyManager.getPolicyConfig());
        
        // Fix for OPENAM-811
        String userid = null; 
        Principal principal = token.getPrincipal();
        if (principal != null) {
            userid = principal.getName();
        }
        if ((userid != null) && (userid.length() != 0)) {
           HashSet<String> set = new HashSet<String>();
           set.add(userid);
           // Required by the AMIdentityMembershipCondition
           envParameters.put(Condition.INVOCATOR_PRINCIPAL_UUID, set);
        } else {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("PolicyEvaluator.padEnvParameters() unable to get userid from token.");
            }
        }
    }

    private boolean isAllowedE(SSOToken token, String resourceName,
        String actionName, Map envParameters) throws SSOException,
        PolicyException {

        if ((envParameters == null) || envParameters.isEmpty()) {
            envParameters = new HashMap();
        }

        padEnvParameters(token, resourceName, actionName, envParameters);

        ActionSchema schema = serviceType.getActionSchema(actionName);
        
        if (!AttributeSchema.Syntax.BOOLEAN.equals(schema.getSyntax())) {
            String objs[] = {actionName};
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "action_does_not_have_boolean_syntax", objs, null);
        }

        HashSet actions = new HashSet(2);
        actions.add(actionName);
        SSOToken adminSSOToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        try {
            Subject adminSubject =  SubjectUtils.createSubject(token);

            Entitlement entitlement = new Entitlement(serviceTypeName, resourceName, actions);
            entitlement.canonicalizeResources(adminSubject, realm);

            Evaluator eval = new Evaluator(adminSubject, serviceTypeName);
            return eval.hasEntitlement(realm, SubjectUtils.createSubject(token), entitlement, envParameters);

        } catch (EntitlementException e) {
            throw new PolicyException(e);
        }
    }

    private String getActionFalseBooleanValue(String actionName)
        throws InvalidNameException {

        if (serviceType == null) {
            return Boolean.FALSE.toString();
        }

        ActionSchema schema = serviceType.getActionSchema(actionName);

        // Cache the false values for the action names
        if (booleanActionNameFalseValues == null) {
            booleanActionNameFalseValues = new HashMap(10);
        }
        String falseValue = null;
        if ((falseValue = (String)
                booleanActionNameFalseValues.get(actionName)) == null) {
            falseValue = schema.getFalseValue();
            // Add it to the cache
            booleanActionNameFalseValues.put(actionName, falseValue);
        }
        return falseValue;
    }

    private String getActionTrueBooleanValue(String actionName)
        throws InvalidNameException {

        if (serviceType == null) {
            return Boolean.TRUE.toString();
        }

        ActionSchema schema = serviceType.getActionSchema(actionName);

        // Cache the true values for the action names
        if (booleanActionNameTrueValues == null) {
            booleanActionNameTrueValues = new HashMap(10);
        }

        String trueValue = null;
        if ((trueValue = (String)
            booleanActionNameTrueValues.get(actionName)) == null) {
            trueValue = schema.getTrueValue();
            booleanActionNameTrueValues.put(actionName, trueValue);
        }

        return trueValue;
    }

    /**
     * Evaluates privileges of the user to perform the specified actions
     * on the specified resource.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     * @param actionNames a <code>Set</code> of <code>Sting</code> objects
     * representing names of the actions the user is trying to perform on
     * the resource
     *
     * @return policy decision
     *
     * @exception SSOException single-sign-on token invalid or expired
     * @exception PolicyException for any other abnormal condition.
     */
    public PolicyDecision getPolicyDecision(SSOToken token, String resourceName,
        Set actionNames) throws PolicyException, SSOException {
        return getPolicyDecision(token, resourceName, actionNames, null);
    }
    

    /**
     * Evaluates privileges of the user to perform the specified actions
     * on the specified resource. The evaluation depends on user's
     * application environment parameters.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     * @param actionNames <code>Set</code> of names(<code>String</code>) of 
     * the action the user is trying to perform on the resource
     * @param envParameters <code>Map</code> of run-time environment parameters
     *
     * @return policy decision
     *
     * @throws SSOException single-sign-on token invalid or expired
     * @throws PolicyException for any other abnormal condition
     * 
     * @supported.api
     */
    public PolicyDecision getPolicyDecision(
            SSOToken token, String resourceName, Set actionNames,
            Map envParameters)  throws SSOException, PolicyException {
        if ( (resourceName == null) || (resourceName.length() == 0) ) {
            resourceName = Rule.EMPTY_RESOURCE_NAME;
        }

        Set originalResourceNames = new HashSet(2);
        originalResourceNames.add(resourceName);

        resourceName = serviceType.canonicalize(resourceName);

        //Add request resourceName and request actionNames to the envParameters
        //so that Condition(s)/ResponseProvider(s) can use them if necessary
        Set resourceNames = new HashSet(2);
        resourceNames.add(resourceName);

        /* compute for all action names if passed in actionNames is
           null or empty */
        if ( (actionNames == null) || (actionNames.isEmpty()) ) {
            actionNames = serviceType.getActionNames();
        }

        Set actions = new HashSet();
        if (actionNames != null) {
            actions.addAll(actionNames);
        }

        /*
         * We create new HashMap in place of empty map since
         * Collections.EMPTY_MAP can not be modified
         */
        if ((envParameters == null) || envParameters.isEmpty()) {
            envParameters = new HashMap();
        }

        envParameters.put(SUN_AM_REQUESTED_RESOURCE, resourceNames);
        envParameters.put(SUN_AM_ORIGINAL_REQUESTED_RESOURCE, 
                originalResourceNames);
        envParameters.put(SUN_AM_REQUESTED_ACTIONS, actions);
        envParameters.put(SUN_AM_POLICY_CONFIG, 
                policyManager.getPolicyConfig());

        return getPolicyDecision(token, resourceName, actionNames,
                envParameters, new HashSet());
    }

    /**
     * Evaluates privileges of the user to perform the specified actions
     * on the specified resource. The evaluation depends on user's
     * application environment parameters.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     * @param actionNames <code>Set</code> of names(<code>String</code>) of the
     * action the user is trying to perform on the resource.
     * @param envParameters run-time environment parameters
     * @param visitedOrgs names of organizations that have been already visited
     *                    during policy evaluation for this request
     *
     * @return policy decision
     *
     * @exception SSOException single-sign-on token invalid or expired
     * @exception PolicyException if any policy evaluation error.
     */
    private PolicyDecision getPolicyDecision(
        SSOToken token, String resourceName, Set actionNames,
        Map envParameters, Set visitedOrgs)
        throws PolicyException, SSOException {
        if (MonitoringUtil.isRunning()) {
            SsoServerPolicySvcImpl sspsi =
                Agent.getPolicySvcMBean();
            sspsi.incPolicyEvalsIn();
        }

        try {
            return (PolicyManager.isMigratedToEntitlementService()) ? 
                getPolicyDecisionE(token, resourceName, actionNames,
                envParameters) : getPolicyDecisionO(token, resourceName,
                actionNames,
                envParameters, visitedOrgs);
        } finally {
            if (MonitoringUtil.isRunning()) {
                SsoServerPolicySvcImpl sspsi =
                        Agent.getPolicySvcMBean();
                sspsi.incPolicyEvalsOut();
            }
        }
    }


    /**
     * Evaluates privileges of the user to perform the specified actions
     * on the specified resource. The evaluation depends on user's
     * application environment parameters.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     * @param actionNames <code>Set</code> of names(<code>String</code>) of the
     * action the user is trying to perform on the resource.
     * @param envParameters run-time environment parameters
     * @return policy decision
     *
     * @exception SSOException single-sign-on token invalid or expired
     * @exception PolicyException if any policy evaluation error.
     */
    private PolicyDecision getPolicyDecisionE(
        SSOToken token, String resourceName, Set actionNames,
        Map envParameters)
        throws PolicyException, SSOException {

        if ( DEBUG.messageEnabled() ) {
            DEBUG.message("Evaluating policies at org " + orgName);
        }

        /* compute for all action names if passed in actionNames is
           null or empty */
        if ( (actionNames == null) || (actionNames.isEmpty()) ) {
            actionNames = serviceType.getActionNames();
        }

        SSOToken adminSSOToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        try {
            Evaluator eval = new Evaluator(
                SubjectUtils.createSubject(adminSSOToken), serviceTypeName);
            Subject sbj = (token != null) ? SubjectUtils.createSubject(token) :
                null;
            List<Entitlement> entitlements = eval.evaluate(
                orgName, sbj, resourceName, envParameters, false);
            if ((entitlements != null) && !entitlements.isEmpty()) {
                Entitlement e = entitlements.iterator().next();
                return (entitlementToPolicyDecision(e, actionNames));
            }
        } catch (EntitlementException e) {
            throw new PolicyException(e);
        }
        return (new PolicyDecision());
    }

    private PolicyDecision getPolicyDecisionO(
            SSOToken token, String resourceName, Set actionNames,
            Map envParameters, Set visitedOrgs)
            throws PolicyException, SSOException {

        if ( DEBUG.messageEnabled() ) {
            DEBUG.message("Evaluating policies at org " + orgName);
        }

        /* compute for all action names if passed in actionNames is
           null or empty */
        if ( (actionNames == null) || (actionNames.isEmpty()) ) {
            actionNames = serviceType.getActionNames();
        }

        Set actions = new HashSet();
        actions.addAll(actionNames);

        PolicyDecision mergedPolicyDecision = null;
        Set policyNameSet = null;
        Set toRemovePolicyNameSet = null;
        policyNameSet = resourceIndexManager.getPolicyNames(
                serviceType, resourceName, INCLUDE_SUPER_RESOURCE_POLCIES);
        if ( DEBUG.messageEnabled() ) {
            String tokenPrincipal =
                    (token != null) ? token.getPrincipal().getName()
                    : PolicyUtils.EMPTY_STRING;
            DEBUG.message(new StringBuffer("at PolicyEvaluator")
                .append(".getPolicyDecision()")
                .append(" principal, resource name, ")
                .append("action names, policy names,")
                .append(" orgName =")
                .append(tokenPrincipal) .append(",  ")
                .append(resourceName) .append(",  ")
                .append(actionNames) .append(",  ")
                .append(policyNameSet).append(",  ")
                .append(orgName).toString());
        }
        Iterator policyIter = policyNameSet.iterator();
        while ( policyIter.hasNext() ) {
            String policyName = (String) policyIter.next();
            Policy policy = policyManager.getPolicy(policyName,
                USE_POLICY_CACHE);
            if ( policy != null && policy.isActive()) {
                //policy might have been removed or inactivated
                PolicyDecision policyDecision = policy.getPolicyDecision(token,
                       serviceTypeName, resourceName, actions, envParameters);
                if (!policy.isReferralPolicy() && policyDecision.hasAdvices()) {
                    addAdvice(policyDecision, ADVICING_ORGANIZATION, orgName);
                }

                // Let us log all policy evaluation results
                if (PolicyUtils.logStatus && (token != null)) {
                    String decision = policyDecision.toString();
                    if (decision != null && decision.length() != 0) {
                        String[] objs = { policyName, orgName, serviceTypeName,
                                        resourceName, actionNames.toString(),
                                        decision };
                            PolicyUtils.logAccessMessage("POLICY_EVALUATION",
                                                objs, token, serviceTypeName);
                    }
                }
                if ( mergedPolicyDecision == null ) {
                    mergedPolicyDecision = policyDecision;
                } else {
                    mergePolicyDecisions(serviceType, policyDecision,
                           mergedPolicyDecision);
                }

                if (!PolicyConfig.continueEvaluationOnDenyDecision()) {
                    actions.removeAll(getFinalizedActions(serviceType,
                            mergedPolicyDecision));
                }

                if ( actions.isEmpty() ) {
                    break;
                }
            } else { // add policy names to toRemovePolicyNameSet
                if (toRemovePolicyNameSet == null) {
                    toRemovePolicyNameSet = new HashSet();
                }
                toRemovePolicyNameSet.add(policyName);
                if ( DEBUG.messageEnabled() ) {
                    DEBUG.message("PolicyEvaluator.getPolicyDecision():"
                        +policyName+ " is inactive or non-existent");
                }
            }
        }

        // remove inactive/missing policies from policyNameSet
        if (toRemovePolicyNameSet != null) {
            policyNameSet.removeAll(toRemovePolicyNameSet);
        }

        Set orgsToVisit = getOrgsToVisit(policyNameSet);

        if (PolicyConfig.orgAliasMappedResourcesEnabled()
                    && PolicyManager.WEB_AGENT_SERVICE.equalsIgnoreCase(
                    serviceTypeName)) {
            String orgAlias = policyManager.getOrgAliasWithResource(
                    resourceName);
            if (orgAlias != null) {
                String orgWithAlias = policyManager.getOrgNameWithAlias(
                        orgAlias);
                if (orgWithAlias != null) {
                    if ( DEBUG.messageEnabled() ) {
                        DEBUG.message("PolicyEvaluator.getPolicyDecision():"
                                + "adding orgWithAlias to orgsToVisit="
                                + orgWithAlias);
                    }
                    orgsToVisit.add(orgWithAlias);
                }
            }
        }

        if ( DEBUG.messageEnabled() ) {
            DEBUG.message(new StringBuffer("at PolicyEvaluator")
                .append(".getPolicyDecision()")
                .append(" orgsToVist=").append(orgsToVisit.toString())
                .toString());
        }
        orgsToVisit.removeAll(visitedOrgs);
        if ( DEBUG.messageEnabled() ) {
            DEBUG.message(new StringBuffer("at PolicyEvaluator")
                .append(".getPolicyDecision()")
                .append(" orgsToVist(after removing already visited orgs=")
                .append(orgsToVisit.toString())
                .toString() );
        }
        while ( !orgsToVisit.isEmpty() && !actions.isEmpty() ) {
            String orgToVisit = (String) orgsToVisit.iterator().next();
            orgsToVisit.remove(orgToVisit);
            visitedOrgs.add(orgToVisit);
            try {
                // need to use admin sso token here. Need all privileges to
                // check for the organzation
                policyManager.verifyOrgName(orgToVisit);
            } catch (NameNotFoundException nnfe) {
                if( DEBUG.warningEnabled()) {
                    DEBUG.warning("Organization does not exist - "
                            + "skipping referral to " + orgToVisit);
                }
                continue;
            }
            PolicyEvaluator pe = new PolicyEvaluator(orgToVisit,
                    serviceTypeName);
            /**
             * save policy config before passing control down to
             * sub realm
             */
            Map savedPolicyConfig =(Map)envParameters.get(SUN_AM_POLICY_CONFIG);
            // Update env to point to the realm policy config data.
            envParameters.put(SUN_AM_POLICY_CONFIG, PolicyConfig.
                getPolicyConfig(orgToVisit));
            PolicyDecision policyDecision
                    = pe.getPolicyDecision(token, resourceName, actionNames,
                    envParameters,visitedOrgs);
            // restore back the policy config data for the parent realm
            envParameters.put(SUN_AM_POLICY_CONFIG, savedPolicyConfig);
            if ( mergedPolicyDecision == null ) {
                mergedPolicyDecision = policyDecision;
            } else {
                mergePolicyDecisions(serviceType, policyDecision,
                       mergedPolicyDecision);
            }
            if (!PolicyConfig.continueEvaluationOnDenyDecision()) {
                actions.removeAll(getFinalizedActions(serviceType,
                        mergedPolicyDecision));
            }
        }

        if ( mergedPolicyDecision == null ) {
            mergedPolicyDecision = new PolicyDecision();
        }

        return mergedPolicyDecision;
    }

    /**
     * Gets protected resources for a user identified by single sign on token
     * Conditions defined  in the policies are ignored while 
     * computing protected resources. 
     * Only resources that are sub resources of the  given 
     * <code>rootResource</code> or equal to the given <code>rootResource</code>
     * would be returned. 
     * If all policies applicable to a resource are 
     * only referral policies, no <code>ProtectedResource</code> would be
     * returned for such a resource.
     *
     * @param token single sign on token of the user
     * @param rootResource  only resources that are sub resources of the  
     *                      given <code>rootResource</code> or equal to the
     *                      given <code>rootResource</code> would be returned
     *                      <code>rootResource</code> would be returned.
     *                      If <code>PolicyEvaluator.ALL_RESOURCES</code> is 
     *                      passed as <code>rootResource</code>, resources under
     *                      all root  resources of the service 
     *                      type are considered while computing protected 
     *                      resources.
     * @return <code>Set</code> of protected resources. The set 
     *         contains <code>ProtectedResource</code> objects. 
     *
     * @throws SSOException if single sign on token is invalid
     * @throws PolicyException for any other abnormal condition
     * @see ProtectedResource
     *
     * @supported.api
     *
     */
    public Set getProtectedResourcesIgnoreConditions(
        SSOToken token, String rootResource)  
        throws SSOException, PolicyException 
    {
        if ( (rootResource == null) || (rootResource.equals("")) ) {
            rootResource = EMPTY_RESOURCE_NAME;
        }
        Set protectedResources = new HashSet();
        Set topLevelResources = null;
        if (rootResource.equals(ALL_RESOURCES)) {
            topLevelResources 
                    = resourceIndexManager.getTopLevelResourceNames(
                    serviceType);
        } else {
            topLevelResources = new HashSet();
            topLevelResources.add(rootResource);
        }
        Iterator iter = topLevelResources.iterator();
        while (iter.hasNext()) {
            String topLevelResource = (String)iter.next();
            Set resourceNames 
                    = getResourceNames(token, topLevelResource, true);
            Iterator resourceIter = resourceNames.iterator();
            while (resourceIter.hasNext()) {
                String resourceName = (String)resourceIter.next();
                Set protectingPolicies 
                        = getProtectingPolicies(token, resourceName);
                if ((protectingPolicies != null) 
                            && (!protectingPolicies.isEmpty())) {
                    boolean allReferralPolicies = true;
                    Iterator iter1 = protectingPolicies.iterator();
                    while (iter1.hasNext()){
                        Policy policy = (Policy)iter1.next();
                        if (!policy.isReferralPolicy()) {
                            allReferralPolicies = false;
                            break;
                        }
                    }
                    if (!allReferralPolicies) {
                        protectedResources.add(
                                new ProtectedResource(resourceName, 
                                protectingPolicies));
                    }
                }
            }
        }
        return protectedResources;
    }

    /**
     * Gets policies applicable to user that are  protecting 
     * the specified resource.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     *
     * @return set of policies applicable to user that are protecting the 
     *         specified resource
     *
     * @throws PolicyException policy exception coming from policy framework
     * @throws SSOException single-sign-on token invalid or expired
     * 
     */
    Set getProtectingPolicies(
        SSOToken token, String resourceName)  
        throws PolicyException, SSOException 
    {
        return getProtectingPolicies(token, resourceName, new HashSet());
    }

    /**
     * Gets policies applicable to user that are  protecting 
     * the specified resource.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     *
     * @param visitedOrgs names of organizations that have been 
     *            already visited during evaluation for this request
     * @return set of policies applicable to user that are protecting the 
     *         specified resource
     *
     * @throws PolicyException policy exception coming from policy framework
     * @throws SSOException single-sign-on token invalid or expired
     * 
     */
    private Set getProtectingPolicies(
        SSOToken token, String resourceName, Set visitedOrgs)  
        throws PolicyException, SSOException 
    {

        Set protectingPolicies = new HashSet();


        // false - do not include super resource policies
        // includes EXACT_MATCH and WILD_CARD_MATCH
        Set policyNameSet = resourceIndexManager.getPolicyNames(
                serviceType, resourceName, false);
        Set toRemovePolicyNameSet = null;
        if ( DEBUG.messageEnabled() ) {
            String tokenPrincipal = 
                    (token != null) ? token.getPrincipal().getName()
                    : PolicyUtils.EMPTY_STRING;
            DEBUG.message(new StringBuffer(
                    "at PolicyEvaluator.getProtectingPolicies()")
                .append(" principal, resource name, policy names,")
                .append(" orgName =")
                .append(tokenPrincipal) .append(",  ")
                .append(resourceName) .append(",  ")
                .append(policyNameSet).append(",  ")
                .append(orgName).toString());
        }
        Iterator policyIter = policyNameSet.iterator();
        while ( policyIter.hasNext() ) {
            String policyName = (String) policyIter.next();
            Policy policy = policyManager.getPolicy(policyName);
            if ( policy != null && policy.isActive()) { 
                //policy might have been removed or inactivated
                if (!policy.isReferralPolicy()) {
                    if (policy.isApplicableToUser(token)) {
                        policy.setOrganizationName(orgName);
                        protectingPolicies.add(policy); 
                    }
                } else {
                    policy.setOrganizationName(orgName);
                    protectingPolicies.add(policy);
                }
            } else { // add policy names to toRemovePolicyNameSet
                if (toRemovePolicyNameSet == null) {
                    toRemovePolicyNameSet = new HashSet();
                }
                toRemovePolicyNameSet.add(policyName);
                if ( DEBUG.messageEnabled() ) {
                    DEBUG.message("PolicyEvaluator.getProtectingPolicies():"
                        +policyName+ " is inactive or non-existent");
                }
            }
        }

        // remove inactive/missing policies from policyNameSet
        if (toRemovePolicyNameSet != null) {
            policyNameSet.removeAll(toRemovePolicyNameSet);
        }

        //include super resource policies provided they are referral policies
        policyNameSet = resourceIndexManager.getSuperResourcePolicyNames(
                serviceType, resourceName);
        if (toRemovePolicyNameSet != null) {
            toRemovePolicyNameSet.clear();
        }
        policyIter = policyNameSet.iterator();
        while ( policyIter.hasNext() ) {
            String policyName = (String) policyIter.next();
            Policy policy = policyManager.getPolicy(policyName);
            if ( policy != null && policy.isActive()) { 
                //policy might have been removed or inactivated
                if (policy.isReferralPolicy()) {
                        policy.setOrganizationName(orgName);
                        protectingPolicies.add(policy);
                }
            } else { // add policy names to toRemovePolicyNameSet
                if (toRemovePolicyNameSet == null) {
                    toRemovePolicyNameSet = new HashSet();
                }
                toRemovePolicyNameSet.add(policyName);
                if ( DEBUG.messageEnabled() ) {
                    DEBUG.message("PolicyEvaluator.getProtectingPolicies():"
                        +policyName+ " is inactive or non-existent");
                }
            }
        }
        // remove inactive/missing policies from policyNameSet
        if (toRemovePolicyNameSet != null) {
            policyNameSet.removeAll(toRemovePolicyNameSet);
        }

        Set orgsToVisit = getOrgsToVisit(policyNameSet);
        if ( DEBUG.messageEnabled() ) {
            DEBUG.message(new StringBuffer(
                    "at PolicyEvaluator.getProtectingPolicies()")
                .append(" orgsToVist=").append(orgsToVisit.toString())
                .toString());
        }

        if (PolicyConfig.orgAliasMappedResourcesEnabled()
                    && PolicyManager.WEB_AGENT_SERVICE.equalsIgnoreCase(
                    serviceTypeName)) {
            String orgAlias = policyManager.getOrgAliasWithResource(
                    resourceName); 
            if (orgAlias != null) {
                String orgWithAlias = policyManager.getOrgNameWithAlias(
                        orgAlias);
                if (orgWithAlias != null) {
                    if ( DEBUG.messageEnabled() ) {
                        DEBUG.message("PolicyEvaluator.getProtectingPolicies():"
                                + "adding orgWithAlias to orgsToVisit="
                                + orgWithAlias);
                    }
                    orgsToVisit.add(orgWithAlias);
                }
            }
        }

        orgsToVisit.removeAll(visitedOrgs);
        if ( DEBUG.messageEnabled() ) {
            DEBUG.message(new StringBuffer(
                    "at PolicyEvaluator.getProtectingPolicies()")
                .append(" orgsToVist(after removing already visited orgs=")
                .append(orgsToVisit.toString())
                .toString() );
        }
        while (!orgsToVisit.isEmpty() ) {
            String orgToVisit = (String) orgsToVisit.iterator().next();
            orgsToVisit.remove(orgToVisit);
            visitedOrgs.add(orgToVisit);
            try {
                // need to use admin sso token here. Need all privileges to
                // check for the organzation
                policyManager.verifyOrgName(orgToVisit);
            } catch (NameNotFoundException nnfe) {
                if( DEBUG.warningEnabled()) {
                    DEBUG.warning("Organization does not exist - "
                            + "skipping referral to " + orgToVisit);
                }
                continue;
            }
            PolicyEvaluator pe 
                    = new PolicyEvaluator(orgToVisit, serviceTypeName);
            Set pp = pe.getProtectingPolicies(token, resourceName,
                    visitedOrgs); 
            protectingPolicies.addAll(pp);
        }

        String principalName =  (token != null) 
                ? token.getPrincipal().getName()
                : PolicyUtils.EMPTY_STRING;

        StringBuffer sb = null;
        String pp = null;
        if (PolicyManager.debug.messageEnabled() || PolicyUtils.logStatus) {
            sb = new StringBuffer();
            Iterator pIter = protectingPolicies.iterator();
            while (pIter.hasNext()) {
                Policy policy = (Policy)pIter.next();
                sb.append(policy.getOrganizationName()).append(":")
                        .append(policy.getName()) .append(",");
            }
            pp = sb.toString();
        }


        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message("Computed policies "
                    + " protecting resource "
                    + resourceName
                    + "for principal:" + principalName + " " + pp);
        }

        if (PolicyUtils.logStatus && (token != null)) {
            String[] objs = { principalName, 
                    resourceName, pp };
            PolicyUtils.logAccessMessage("PROTECTED_RESOURCES", objs, token,
                    serviceTypeName);
        }
        return protectingPolicies;
    }

    /**
     * Gets resource result objects given a resource name. The set
     * contains <code>ResourceResult</code> objects for all resources 
     * that would affect policy decisions for any resource associated with the 
     * argument resource name. To determine whether to include the
     * <code>ResourceResult</code> of a resource,  we compare argument resource
     * name and policy resource name, treating wild characters in the policy 
     * resource name as wild. If the comparison resulted in
     * <code>EXACT_MATCH</code>, <code>WILD_CARD_MACTH</code> or
     * <code>SUB_RESOURCE_MACTH</code>, the resource result would be
     * included.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource 
     * @param scope indicates whether to compute the resource result based on
     *              the policy decision for only the <code>resourceName</code>
     *              or all the resources associated with the resource name.
     *              The valid scope values are:
     *              <ul>
     *              <li><code>ResourceResult.SUBTREE_SCOPE</code>
     *              <li><code>ResourceResult.STRICT_SUBTREE_SCOPE</code>
     *              <li><code>ResourceResult.SELF_SCOPE</code>
     *              <ul>
     *              If the scope is <code>ResourceResult.SUBTREE_SCOPE</code>,
     *              the method will return a set of <code>ResourceResult</code>
     *              objects, one of them for the <code>resourceName</code> and
     *              its sub resources; the others are for resources that match
     *              the <code>resourceName</code> by wildcard. If the scope is
     *              <code>ResourceResult.STRICT_SUBTREE_SCOPE</code>, the 
     *              method will return a set object that contains one 
     *              <code>ResourceResult</code> object. The
     *              <code>ResourceResult</code> contains the policy decisions
     *              regarding the <code>resourceName</code> and its sub
     *              resources. If the scope is
     *              <code>ResourceResult.SELF_SCOPE</code>, the method will
     *              return a set object that contains one
     *              <code>ResourceResult</code> object.
     *              The <code>ResourceResult</code> contains the policy decision
     *              regarding the <code>resourceName</code> only.
     *
     * @param envParameters run-time environment parameters
     *
     * @return set of <code>ResourceResult</code> objects
     *
     * @throws SSOException if <code>token</code> is invalid
     * @throws PolicyException for any other abnormal condition
     *
     * @see ResourceMatch#EXACT_MATCH
     * @see ResourceMatch#SUB_RESOURCE_MATCH
     * @see ResourceMatch#WILDCARD_MATCH
     * @see ResourceResult#SUBTREE_SCOPE
     * @see ResourceResult#STRICT_SUBTREE_SCOPE
     * @see ResourceResult#SELF_SCOPE
     *
     * 
     * @supported.api
     */
    public Set getResourceResults(SSOToken token, 
            String resourceName, String scope, Map envParameters) 
            throws SSOException, PolicyException {
        return (PolicyManager.isMigratedToEntitlementService()) ?
            getResourceResultsE(token, resourceName, scope, envParameters) :
            getResourceResultsO(token, resourceName, scope, envParameters);
    }
    
    private Set getResourceResultsO(SSOToken token,
            String resourceName, String scope, Map envParameters)
            throws SSOException, PolicyException {
        Set resultsSet;

        if (ResourceResult.SUBTREE_SCOPE.equals(scope)) {
            resultsSet = getResourceResultTree(token, resourceName, scope,
                                         envParameters).getResourceResults();
        } else if (ResourceResult.STRICT_SUBTREE_SCOPE.equals(scope)
                   || ResourceResult.SELF_SCOPE.equals(scope)) {
            ResourceResult result = getResourceResultTree(token, resourceName,
                                         scope, envParameters);
            resultsSet = new HashSet();
            resultsSet.add(result);
        } else {
            DEBUG.error("PolicyEvaluator: invalid request scope: " + scope);
            String objs[] = {scope};
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_request_scope", objs, null);
        }

        return resultsSet;
    }

    private Set getResourceResultsE(SSOToken token,
            String resourceName, String scope, Map envParameters)
            throws SSOException, PolicyException {

        if ((envParameters == null) || envParameters.isEmpty()) {
            envParameters = new HashMap();
        }
        padEnvParameters(token, resourceName, null, envParameters);

        Set resultsSet;
        boolean subTreeSearch = false;

        if (ResourceResult.SUBTREE_SCOPE.equals(scope)) {
            subTreeSearch = true;
            //resultsSet = getResourceResultTree(token, resourceName, scope,
            //                            envParameters).getResourceResults();
        } else if (ResourceResult.STRICT_SUBTREE_SCOPE.equals(scope)
                   || ResourceResult.SELF_SCOPE.equals(scope)) {
            /*
            ResourceResult result = getResourceResultTree(token, resourceName,
                                         scope, envParameters);
            resultsSet = new HashSet();
            resultsSet.add(result);*/
        } else {
            DEBUG.error("PolicyEvaluator: invalid request scope: " + scope);
            String objs[] = {scope};
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_request_scope", objs, null);
        }

        SSOToken adminSSOToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        try {
            // Parse the resource name before proceeding.
            resourceName = serviceType.canonicalize(resourceName);

            Subject userSubject = SubjectUtils.createSubject(token);
            Evaluator eval = new Evaluator(
                SubjectUtils.createSubject(adminSSOToken), serviceTypeName);

            List<Entitlement> entitlements = eval.evaluate(
                realm, userSubject, resourceName,
                envParameters, subTreeSearch);
            resultsSet = new HashSet();

            if (!entitlements.isEmpty()) {
                if (!subTreeSearch) {
                    resultsSet.add(entitlementToResourceResult(
                        (Entitlement)entitlements.iterator().next()));
                } else {
                    ResourceResult virtualResourceResult =
                        new ResourceResult(ResourceResult.VIRTUAL_ROOT,
                            new PolicyDecision());
                    for (Entitlement ent : entitlements ) {
                        ResourceResult r = entitlementToResourceResult(ent);
                        virtualResourceResult.addResourceResult(r, serviceType);
                    }

                    resultsSet.addAll(
                        virtualResourceResult.getResourceResults());
                }
            }
        } catch (Exception e) {
            DEBUG.error("Error in getResourceResults", e);
            throw new PolicyException(e.getMessage()); //TOFIX
        }

        return resultsSet;
    }

   
    private ResourceResult entitlementToResourceResult(
        Entitlement entitlement
    ) throws PolicyException {
        return new ResourceResult(entitlement.getResourceName(),
            entitlementToPolicyDecision(entitlement, Collections.EMPTY_SET));
    }
    
    private PolicyDecision entitlementToPolicyDecision(
        Entitlement entitlement,
        Set<String> actionNames
    ) throws PolicyException {
        PolicyDecision pd = new PolicyDecision();
        Map actionValues = entitlement.getActionValues();

        if ((actionValues != null) && !actionValues.isEmpty()) {
            for (Iterator i = actionValues.keySet().iterator(); i.hasNext();) {
                String actionName = (String) i.next();
                Set set = new HashSet();
                boolean isBooleanAction = true;
                if (serviceType != null) {
                    ActionSchema as = null;
                    try {
                        as = serviceType.getActionSchema(actionName);
                    } catch (InvalidNameException inex) {
                        if (DEBUG.warningEnabled()) {
                            DEBUG.warning("PolicyEvaluator." +
                                "entitlementToPolicyDecision:", inex);
                        }
                    }
                    isBooleanAction = (as != null) &&
                        as.getSyntax().equals(AttributeSchema.Syntax.BOOLEAN);
                }

                if (isBooleanAction) {

                    Boolean values = (Boolean) actionValues.get(actionName);

                    if (values.booleanValue()) {
                        set.add(getActionTrueBooleanValue(actionName));
                    } else {
                        set.add(getActionFalseBooleanValue(actionName));
                    }
                } else {
                    // Parse the action name to get the value
                    int index = actionName.indexOf('_');
                    if (index != -1) {
                        set.add(actionName.substring(index+1));
                        actionName = actionName.substring(0, index);
                    } else {
                        set.add(actionName);
                    }
                }

                ActionDecision ad = new ActionDecision(actionName, set);
                ad.setAdvices(entitlement.getAdvices());
                ad.setTimeToLive(entitlement.getTTL());
                pd.addActionDecision(ad, serviceType);
            }
        } else {
            Map advices = entitlement.getAdvices();
            if ((advices != null) && (!advices.isEmpty()) &&
                ((actionNames == null) || actionNames.isEmpty())) {
                actionNames = serviceType.getActionNames();
            }
            for (String actionName : actionNames) {
                Set set = new HashSet();
                // Determinte if the serviceType have boolean action values
                ActionSchema as = null;
                if (serviceType != null) {
                    try {
                        as = serviceType.getActionSchema(actionName);
                    } catch (InvalidNameException inex) {
                        if (DEBUG.warningEnabled()) {
                            DEBUG.warning("PolicyEvaluator." +
                                "entitlementToPolicyDecision:", inex);
                        }
                    }
                }
                if ((as == null) ||
                    as.getSyntax().equals(AttributeSchema.Syntax.BOOLEAN)) {
                    set.add(getActionFalseBooleanValue(actionName));
                } else {
                    set.addAll(as.getDefaultValues());
                }
                ActionDecision ad = new ActionDecision(actionName, set);
                ad.setAdvices(entitlement.getAdvices());
                ad.setTimeToLive(entitlement.getTTL());
                pd.addActionDecision(ad, serviceType);
            }
        }

        pd.setTimeToLive(entitlement.getTTL());
        pd.setResponseAttributes(entitlement.getAttributes());
        return pd;
    }

    /**
     * Gets resource result given a resource name. <code>ResourceResult</code>
     * is a tree representation of policy decisions for all resources rooted 
     * at the resource name.
     * To determine whether a resource defined in the policy
     * is a sub resource of argument resource name, argument resource name 
     * and policy resource name are compared, treating wild characters as 
     * literals. If comparison resulted in <code>EXACT_MACTH</code> or
     * <code>SUB_RESOURCE_MACTH</code>, the resource would be included
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource 
     * @param scope indicates whether to compute the resource result based on
     *              the policy decision for only the <code>resourceName</code>
     *              or all the resources associated with the resource name.
     *              The valid scope values are:
     *              <ul>
     *              <li><code>ResourceResult.SUBTREE_SCOPE</code>
     *              <li><code>ResourceResult.STRICT_SUBTREE_SCOPE</code>
     *              <li><code>ResourceResult.SELF_SCOPE</code>
     *              </ul>
     *              If the scope is <code>ResourceResult.SUBTREE_SCOPE</code> or
     *              <code>ResourceResult.STRICT_SUBTREE_SCOPE</code>, the method
     *              will return a <code>ResourceResult</code> object that
     *              contains the policy decisions regarding the
     *              <code>resourceName</code> and its sub resources.
     *              If the scope is <code>ResourceResult.SELF_SCOPE</code>, the
     *              method will return a <code>ResourceResult</code> object that
     *              contains the policy decision regarding the
     *              <code>resourceName</code> only. Note, scope values
     *              <code>ResourceResult.SUBTREE_SCOPE</code> and
     *              <code>ResourceResult.STRICT_SUBTREE_SCOPE</code> are being
     *              treated as the same for backword compatibility reasons. This
     *              method is being deprecated. The method
     *              <code>getResourceResults()</code> should be used instead. 
     *
     * @param envParameters run-time environment parameters
     *
     * @return <code>ResourceResult</code>.
     *
     * @throws SSOException if <code>token</code> is invalid 
     * @throws PolicyException for any other abnormal condition
     *
     * @see ResourceMatch#EXACT_MATCH
     * @see ResourceMatch#SUB_RESOURCE_MATCH
     * @see ResourceMatch#WILDCARD_MATCH
     * @see ResourceResult#SUBTREE_SCOPE
     * @see ResourceResult#STRICT_SUBTREE_SCOPE
     * @see ResourceResult#SELF_SCOPE
     *
     * @deprecated Use <code>getResourceResults()</code>
     *
     * @supported.api
     *
     */
    public ResourceResult getResourceResult(SSOToken token, 
            String resourceName, String scope, Map envParameters) 
            throws SSOException, PolicyException {
        if (ResourceResult.SUBTREE_SCOPE.equals(scope)
                || ResourceResult.STRICT_SUBTREE_SCOPE.equals(scope) 
                || ResourceResult.SELF_SCOPE.equals(scope)) {
            if (ResourceResult.SUBTREE_SCOPE.equals(scope)) {
                scope = ResourceResult.STRICT_SUBTREE_SCOPE; 
            }
            return getResourceResultTree(token, resourceName, scope, 
                    envParameters); 
        } else {
            DEBUG.error("PolicyEvaluator: invalid request scope: " + scope);
            String objs[] = {scope};
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_request_scope", objs, null);
        }
    }

    /**
     * Gets resource result given a resource name. <code>ResourceResult</code>
     * is a tree representation of policy decisions for all resources 
     * that are sub resources of argument resource name. 
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource 
     * @param scope indicates whether to compute the resource result based on
     *              the policy decision for only the <code>resourceName</code>
     *              or all the resources associated with the resource name.
     * @param envParameters run-time environment parameters
     *
     * @return <code>ResourceResult</code>.
     *
     * @exception SSOException if <code>token</code> is invalid
     * @exception PolicyException for any other abnormal condition
     *
     * @see ResourceMatch#EXACT_MATCH
     * @see ResourceMatch#SUB_RESOURCE_MATCH
     * @see ResourceMatch#WILDCARD_MATCH
     *
     */
    private ResourceResult getResourceResultTree(SSOToken token, 
            String resourceName, String scope, Map envParameters)
            throws PolicyException, SSOException {

        String userSSOTokenIDStr =  (token != null) 
                ? token.getTokenID().toString() 
                : PolicyUtils.EMPTY_STRING;

        if (token == null) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("user sso token is null, forcing ResourceResult"
                        + " evaluation to self_scope");
            }
            scope = ResourceResult.SELF_SCOPE;

        }

        ResourceResult resourceResult = null;

        if ( (resourceName == null) || (resourceName.equals("")) ) {
            resourceName = Rule.EMPTY_RESOURCE_NAME;
        }
        resourceName = serviceType.canonicalize(resourceName);
        Map clientEnv = PolicyUtils.cloneMap(envParameters);


        // check if we already have the result in the cache
        // policyResultsCache: 
        // serviceType -> resource -> sessionId -> scope -> result
        synchronized(policyResultsCache) {
            // rscCACHE: resource -> sessionId -> scope -> result
            Map rscCache = (Map)policyResultsCache.get(serviceTypeName);
            if (rscCache != null) {
                // resultCACHE: sessionId -> scope -> resourceResult
                Map resultsCache = (Map)rscCache.get(resourceName);
                if (resultsCache != null) {
                    Map results = (Map)resultsCache.get(userSSOTokenIDStr);
                    if (results != null) {
                        resourceResult = (ResourceResult)results.get(scope);
                        if (resourceResult != null) {
                            long currentTime = System.currentTimeMillis();
                            long ttlMinimal = resourceResult.getTimeToLive();
                            if (ttlMinimal > currentTime) {

                                //check envMap equality of request and cache
                                Map cachedEnv = resourceResult.getEnvMap();
                                if ( ((clientEnv == null) 
                                            && (cachedEnv == null))
                                        || ((clientEnv != null)
                                            && clientEnv.equals(cachedEnv)) ) {
                                    if (DEBUG.messageEnabled()) {
                                        DEBUG.message("PolicyEvaluator."
                                        + " getResourceResult(): we get the "
                                        + "result from the cache.\n" 
                                        + resourceResult.toXML());
                                    }
                                    return resourceResult;
                                } else {
                                    if (PolicyManager.debug.messageEnabled()) {
                                        PolicyManager.debug.message(
                                        "PolicyEvaluator.getResourceesultTree()"
                                        + ":cached envMap does not equal "
                                        + "request envMap, request envMap = " 
                                        + clientEnv 
                                        + ", cachedEnv=" + cachedEnv
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        /* compute all action names if passed in actionNames is
           null or empty */
        if ( (actionNames == null) || (actionNames.isEmpty()) ) {
            actionNames = serviceType.getActionNames();
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("PolicyEvaluator:computing policy decisions "
                    + " for resource : " + resourceName);
        }
        PolicyDecision policyDecision = getPolicyDecision(token, resourceName,
                actionNames, envParameters);
        resourceResult =  new ResourceResult(resourceName, policyDecision);

        if (ResourceResult.SUBTREE_SCOPE.equals(scope)) {
            ResourceResult virtualResourceResult 
                    = new ResourceResult(ResourceResult.VIRTUAL_ROOT, 
                    new PolicyDecision());
            virtualResourceResult.addResourceResult(resourceResult, 
                    serviceType);
            resourceResult = virtualResourceResult;
        }

        if (ResourceResult.SUBTREE_SCOPE.equals(scope)
                || ResourceResult.STRICT_SUBTREE_SCOPE.equals(scope)) {
            Map resourceNamesCache    
                    = (Map)resourceNamesMap.get(serviceTypeName);
            if (resourceNamesCache == null) {
                resourceNamesCache = new Cache(resultsCacheResourceCap);
                resourceNamesMap.put(serviceTypeName, resourceNamesCache);
            }
            Set resourceNames = (Set)resourceNamesCache.get(resourceName);
            if (resourceNames == null) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Computing subresources for:  "
                            + resourceName);
                }
                // true indicates to follow referral
                resourceNames = getResourceNames(token, resourceName, true);
                resourceNames = removeDuplicateResourceNames(resourceNames,
                        serviceType);
                resourceNames = removeResourceName(resourceNames,
                        serviceType, resourceName);
                resourceNamesCache.put(resourceName, resourceNames);
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("PolicyEvaluator:computing policy decisions "
                        + " for subresources : " + resourceNames);
            }

            Iterator resourceNameIter = resourceNames.iterator();
            while (resourceNameIter.hasNext()) {
                String subResourceName = (String) resourceNameIter.next();
                if (ResourceResult.SUBTREE_SCOPE.equals(scope) ||
                        (serviceType.compare(resourceName,
                        subResourceName, false).equals(
                        ResourceMatch.SUB_RESOURCE_MATCH))) {
                    PolicyDecision pDecision = getPolicyDecision(token, 
                            subResourceName, actionNames, envParameters);
                    resourceResult.addResourceResult(
                            new ResourceResult(subResourceName, pDecision),
                            serviceType);
                }
            }
        }
  
        // Do not cache policy decision with advices
        if ( (resourceResult != null) 
                    && !resourceResult.hasAdvices()) {
            resourceResult.setEnvMap(clientEnv);
            // add the evaluation result to the result cache
            Map scopeElem = null;
            //cacheElem: sessionId -> scope -> resourceResult
            Map cacheElem = null;
            Map rscElem = null;
            // serviceType -> resourceName -> sessionId -> scope -> resourceResult
            synchronized(policyResultsCache) { 
                // rscElemCACHE: resourceName -> sessionId -> scope -> resourceResult
                rscElem = (Map)policyResultsCache.get(
                                                serviceTypeName);
                if (rscElem != null) { // serviceType has been seen earlier
                    //CACHEElem: sessionId -> scope -> resourceResult
                    cacheElem = (Map)rscElem.get(resourceName);
                    if (cacheElem != null) { // resource seen earlier
                        scopeElem = (Map)cacheElem.get(
                                                userSSOTokenIDStr);
                        if (scopeElem == null) { // seeing sessionId first time
                            scopeElem = new HashMap();
                        }
                    } else { // seeing the resource first time
                        if (PolicyManager.debug.messageEnabled()) {
                            PolicyManager.debug.message(
                                "PolicyEvaluator.getResourceResultTree()"
                                + " Create Cache for:" 
                                + ", resourceName=" + resourceName
                                + ", sessionId=" + userSSOTokenIDStr
                                + ", scope=" + scope);
                        }
                        cacheElem = new Cache(resultsCacheSessionCap); 
                        scopeElem = new HashMap();
                    }
                } else { // seeing service for first time
                    // rscElemCACHE: resourceName -> sessionId -> scope -> resourceResult
                    rscElem = new Cache(resultsCacheResourceCap);
                    //CACHEElem: sessionId -> scope -> resourceResult
                    if (PolicyManager.debug.messageEnabled()) {
                            PolicyManager.debug.message(
                                "PolicyEvaluator.getResourceResultTree()"
                                + " Create Cache for:" 
                                + ", resourceName=" + resourceName
                                + ", sessionId=" + userSSOTokenIDStr
                                + ", scope=" + scope
                                + ", serviceType=" + serviceTypeName);
                    }
                    cacheElem = new Cache(resultsCacheSessionCap);
                    scopeElem = new HashMap();
                }
                scopeElem.put(scope, resourceResult);
                cacheElem.put(userSSOTokenIDStr, scopeElem);
                if (PolicyManager.debug.messageEnabled()) {
                        PolicyManager.debug.message(
                            "PolicyEvaluator.getResourceResultTree()"
                            + " Create Cache for:" 
                            + ", resourceName=" + resourceName
                            + ", sessionId=" + userSSOTokenIDStr
                            + ", scope=" + scope
                            + ", cacheSize=" + cacheElem.size());
                }
                rscElem.put(resourceName, cacheElem);
                policyResultsCache.put(serviceTypeName, rscElem);
            } 

            if ( (token != null) 
                        && !(ssoListenerRegistry.containsKey(
                        userSSOTokenIDStr))) {
                try {
                    token.addSSOTokenListener(ssoListener);
                } catch (SSOException se) {
                    DEBUG.error("PolicyEvaluator:"
                            + "failed to add sso token listener");
                }
                ssoListenerRegistry.put(userSSOTokenIDStr, ssoListener);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("PolicyEvaluator.getResourceResultTree():"
                            + " sso listener added .\n");
                }
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("PolicyEvaluator: we added the evaluation "
                        + " result to the cache");
            }
        } 

        return resourceResult;
    }


    /**
     * Gets resource names that are exact matches, sub resources or 
     * wild card matches of argument resource name.
     * To determine whether to include a
     * resource name of a resource,  we compare argument resource name and 
     * policy resource name, treating wild characters in the policy 
     * resource name as wild. If the comparison resulted in
     * <code>EXACT_MATCH</code>, <code>WILD_CARD_MACTH</code> or
     * <code>SUB_RESOURCE_MACTH</code>, the resource result would be
     * included.
     *
     * @param token single sign on token
     *
     * @param resourceName resoure name
     * @param followReferral indicates whether to follow the referrals 
     *                       defined in policies to compute resource names
     * @return names of sub resources for the given <code>resourceName</code>.
     *         The return value would also include the
     *         <code>resourceName</code>.
     *
     * @exception SSOException if <code>token</code> is invalid
     * @exception PolicyException for any other abnormal condition
     *
     * @see ResourceMatch#EXACT_MATCH
     * @see ResourceMatch#SUB_RESOURCE_MATCH
     * @see ResourceMatch#WILDCARD_MATCH
     *
     */
     public Set getResourceNames(SSOToken token, String resourceName, 
             boolean followReferral) throws PolicyException, SSOException {
         Set visitedOrgs = new HashSet();
         visitedOrgs.add(policyManager.getOrganizationDN());
         return getResourceNames(token, resourceName, followReferral,
                 visitedOrgs);
     }

    /**Gets resource names that are exact matches, sub resources or 
     * wild card matches of argument resource name.
     * To determine whether to include a
     * resource name of a resource,  we compare argument resource name and 
     * policy resource name, treating wild characters in the policy 
     * resource name as wild. If the comparsion resulted in 
     * <code>EXACT_MATCH</code>, <code>WILD_CARD_MACTH</code> or 
     * <code>SUB_RESOURCE_MACTH</code>, the resource result would be
     * included.
     *
     * @param token single sign on token
     *
     * @param resourceName resoure name
     * @param followReferral indicates whether to follow the referrals 
     *                       defined in policies to compute resource names
     * @param visitedOrgs organizations that were already visited to 
     *                    compute resource names
     * @return names of sub resources for the given <code>resourceName</code>.
     *         The return value would also include the
     *         <code>resourceName</code>.
     *
     * @exception SSOException if <code>token</code> is invalid
     * @exception PolicyException for any other abnormal condition
     *
     * @see ResourceMatch#EXACT_MATCH
     * @see ResourceMatch#SUB_RESOURCE_MATCH
     * @see ResourceMatch#WILDCARD_MATCH
     *
     */
     public Set getResourceNames(SSOToken token, String resourceName, 
             boolean followReferral, Set visitedOrgs) 
             throws PolicyException, SSOException {
         DEBUG.message("PolicyEvaluator.getResourceNames():entering");
         Set resourceNames = new HashSet();
         Set policyNameSet = null;
         Set toRemovePolicyNameSet = null;
         Set orgsToVisit = new HashSet();
         policyNameSet = resourceIndexManager.getSubResourcePolicyNames(
                serviceType, resourceName);
         policyNameSet.addAll(
                resourceIndexManager.getPolicyNames(serviceType,
                resourceName, true)); //include policies of super resources
         policyNameSet.addAll(
                resourceIndexManager.getWildSubResourcePolicyNames(
                serviceType, resourceName));
         if ( (policyNameSet != null) && (!policyNameSet.isEmpty()) ) {
             Iterator policyIter = policyNameSet.iterator();
             while (policyIter.hasNext()) {
                 String policyName = (String) policyIter.next();
                 Policy policy = policyManager.getPolicy(policyName, 
                        USE_POLICY_CACHE);
                 // policy could have been deleted
                 if ( policy != null && policy.isActive()) {
                     // true inidicates to follow referrals
                     Set pResourceNames = policy.getResourceNames(token, 
                             serviceTypeName, resourceName, true);
                     if (pResourceNames != null) {
                         resourceNames.addAll(pResourceNames);
                     }
                 } else { // add policy names to toRemovePolicyNameSet
                    if (toRemovePolicyNameSet == null) {
                        toRemovePolicyNameSet = new HashSet();
                    }
                    toRemovePolicyNameSet.add(policyName);
                    if ( DEBUG.messageEnabled() ) {
                        DEBUG.message("PolicyEvaluator.getResourceNames():"
                            +policyName+ " is inactive or non-existent");
                    }
                 }
             }
             // remove inactive/missing policies from policyNameSet
             if (toRemovePolicyNameSet != null) {
                 policyNameSet.removeAll(toRemovePolicyNameSet);
             }

             orgsToVisit.addAll(getOrgsToVisit(policyNameSet));

             if ( DEBUG.messageEnabled() ) {
                 DEBUG.message("PolicyEvaluator.getResourceNames():"
                     + "realmAliasEnabled=" 
                     + PolicyConfig.orgAliasMappedResourcesEnabled()
                     + ", serviceTypeName=" + serviceTypeName);
             }
         }
         if (PolicyConfig.orgAliasMappedResourcesEnabled()
                && PolicyManager.WEB_AGENT_SERVICE.equalsIgnoreCase(
                serviceTypeName)) {
             String orgAlias = policyManager.getOrgAliasWithResource(
                    resourceName); 
             if (orgAlias != null) {
                 String orgWithAlias = policyManager.getOrgNameWithAlias(
                        orgAlias);
                 if (orgWithAlias != null) {
                     if ( DEBUG.messageEnabled() ) {
                         DEBUG.message("PolicyEvaluator."
                                 + "getgetResourceNames():"
                                 + "adding orgWithAlias to orgsToVisit="
                                 + orgWithAlias);
                     }
                     orgsToVisit.add(orgWithAlias);
                 } else {
                     if ( DEBUG.messageEnabled() ) {
                         DEBUG.message("PolicyEvaluator."
                                 + "getgetResourceNames():"
                                 + "no realm matched orgAlias:" + orgAlias);
                     }
                 }
             }
         }

         orgsToVisit.removeAll(visitedOrgs);
         while (!orgsToVisit.isEmpty() ) {
             String orgToVisit = (String) orgsToVisit.iterator().next();
             orgsToVisit.remove(orgToVisit);
             visitedOrgs.add(orgToVisit);
             //resourceNames.add(resourceName);
             try {
                 // need to use admin sso token here. Need all privileges to
                 // check for the organzation
                 policyManager.verifyOrgName(orgToVisit);
             } catch (NameNotFoundException nnfe) {
                 if( DEBUG.warningEnabled()) {
                     DEBUG.warning("PolicyEvaluator."
                            + "getgetResourceNames():"
                             + "Organization does not exist - "
                             + "skipping referral to " + orgToVisit);
                 }
                 continue;
             }
             PolicyEvaluator pe = new PolicyEvaluator(orgToVisit, 
                    serviceTypeName);
             resourceNames.addAll(pe.getResourceNames(token, 
                    resourceName, true,
                    visitedOrgs)); 
         }
         return resourceNames; 
     }

     /** Adds a policy listener that would be notified whenever a policy
      *  is added, removed or changed
      *
      *  @param policyListener the listener to be added
      * 
      * @supported.api
      */
      public void addPolicyListener(PolicyListener policyListener) {
          policyCache.addPolicyListener(policyListener);
      }

     /** Removes a policy listener that was previously registered 
      *  to receive notifications whenever a policy is added, removed
      *  or changed. It is not an error to attempt to remove a listener
      *  that was not registered. It would return silently.
      *
      *  @param policyListener the listener to be removed
      * 
      * @supported.api
      */
      public void removePolicyListener(PolicyListener policyListener) {
          policyCache.removePolicyListener(policyListener);
      }
     
      /** Merges two policy decisions.
       *  Merging policy decisions merges each action decision of the 
       *  policy with the corresponding action decision of the other 
       *  policy. This method also merges ResponseProviderDecision of one
       *  policy ( response attributes per policy) 
       *  with that of the other policy.
       *  These are the rules followed to merge each action decision:
       *  If the action schema has boolean syntax, boolean false value
       *  overrides boolean true value. The time to live of boolean false 
       *  value overrides the time to live of boolean true value.
       *  Otherwise, action values are simply  aggregated. Time to live
       *  is set to the minimum of time to live(s) of all values of the
       *  action.
       *  For response attributes, all response attributes are aggregated.
       *  In case of mutiple values for the same attribute 
       *  they appear as multi valued data for the attribute.
       *  @param serviceType service type that would be consulted to merge the
       *         policy decisions
       *  @param pd1 policy decision 1
       *  @param pd2 policy decision 2
       *  @return the merged policy decision. 
       *          Policy decisions pd1 and pd2 are merged into pd2 and 
       *          pd2 is returned.
       */
       static PolicyDecision mergePolicyDecisions(ServiceType
            serviceType, PolicyDecision pd1, PolicyDecision pd2) {
          Map actionDecisions1 = pd1.getActionDecisions();
          Set actions = new HashSet();
          actions.addAll(actionDecisions1.keySet());
          Iterator iter = actions.iterator();
          while ( iter.hasNext() ) {
              String action = (String) iter.next();
              ActionDecision ad1 = (ActionDecision) actionDecisions1.get(
                    action);
              pd2.addActionDecision(ad1, serviceType);
          }
          Map mergedReponseAttrsMap = new HashMap();
          PolicyUtils.appendMapToMap(pd1.getResponseAttributes(),
                mergedReponseAttrsMap);
          PolicyUtils.appendMapToMap(pd2.getResponseAttributes(), 
                mergedReponseAttrsMap);
          pd2.setResponseAttributes(mergedReponseAttrsMap);
          return pd2;
      }

      /** Gets a set of action names for which final values have been 
       *  determined. We assume the final values have been determined
       *  for an action if the action schema syntax is boolean and 
       *  the value is boolean false value
       *
       *  @param serviceType service type that would be consulted to decide
       *         the final values for actions
       *  @param pd policy decision 
       */
      static Set getFinalizedActions(ServiceType
            serviceType, PolicyDecision pd) {
        Set finalizedActions = new HashSet();
        Map actionDecisions = pd.getActionDecisions();
        Iterator actions = actionDecisions.keySet().iterator();
        while ( actions.hasNext() ) {
            String action = (String) actions.next();
            ActionDecision actionDecision 
                  = (ActionDecision) actionDecisions.get(action);
            Set values = actionDecision.getValues();
            if ( (values != null) && !values.isEmpty() ) {
                try {
                    ActionSchema schema 
                            = serviceType.getActionSchema(action);
                    if ((AttributeSchema.Syntax.BOOLEAN.equals(
                                schema.getSyntax()))
                                && values.contains(schema.getFalseValue()) ) {
                        finalizedActions.add(action);
                    }
                } catch(InvalidNameException e) {
                    DEBUG.error("can not find action schmea for action = " +
                            action, e );
                }

            }
              

        }
        return finalizedActions;
      }

    /**
     *  Gets names of organizations to visit for policy evaluation
     *  based on the give policy names.  This is used to follow 
     *  OrgReferral(s) defined in the policies
     *
     *  @return names of organization to visit
     *  @exception SSOException if <code>token</code> is invalid
     *  @exception PolicyException for any other abnormal condition
     */
    private Set getOrgsToVisit(Set policyNameSet) 
            throws PolicyException, SSOException {
        Set orgsToVisit = new HashSet();
        Iterator policyNames = policyNameSet.iterator();
        while ( policyNames.hasNext() ) {
            String policyName = (String) policyNames.next();
            Policy policy = policyManager.getPolicy(policyName, 
                USE_POLICY_CACHE);
            if (policy != null) {
                orgsToVisit.addAll(policy.getReferredToOrganizations());
            }
        }
        return orgsToVisit;
    }

    /**
     *   This would be a costly operation.
     *   Can be avoided if ResourceName has api for getting canonical name.
     *   When the policies are stored, resource names would be converted to and 
     *   stored as canonical name.
     */
    private static Set removeDuplicateResourceNames(Set resourceNames,
            ServiceType serviceType) {
        Set answer = resourceNames;
        if ( (resourceNames != null) && (serviceType != null) ) {
            answer = new HashSet(resourceNames.size());
            Iterator iter = resourceNames.iterator();
            while ( iter.hasNext() ) {
                String resourceName = (String) iter.next();
                Iterator answerIter = answer.iterator();
                boolean duplicate = false;
                while (answerIter.hasNext()) {
                    String answerResourceName = (String) answerIter.next();

                    if ( serviceType.compare(resourceName,
                            answerResourceName, false)
                            .equals(ResourceMatch.EXACT_MATCH) ) {
                        duplicate = true;
                        break;
                    } 
                } 
                if (!duplicate) {
                    answer.add(resourceName);
                }
            }

        }
        return answer;
    }

    /**
     * Removes the <code>resourceName</code> from the <code>Set</code>
     * of resource names matching on <code>serviceType</code> and
     * performing a <code>ResourceMatch.EXACT_MATCH</code>
     */

    private static Set removeResourceName(Set resourceNames, 
            ServiceType serviceType, String resourceName) {
        Set answer = resourceNames;
        if ( (resourceNames != null) && (serviceType != null) 
                    && (resourceName != null) ) {
            answer = new HashSet(resourceNames.size());
            answer.addAll(resourceNames);
            Iterator iter = resourceNames.iterator();
            while ( iter.hasNext() ) {
                String rName = (String) iter.next();
                if ( serviceType.compare(resourceName,
                        rName, false).equals(ResourceMatch.EXACT_MATCH) ) {
                    answer.remove(rName);
                }
            }

        }
        return answer;
    }


    /**
     * Handles policyChanged notifications - clears the cached resource
     * names for the service type name
     *
     * @param serviceTypeName service type name
     * @param pe policy event
     */
    static void policyChanged(String serviceTypeName, PolicyEvent pe) {

        if (DEBUG.messageEnabled()) {
            DEBUG.message("PolicyEvaulator.policyChanged():serviceTypeName="
                    + serviceTypeName);
        }
        resourceNamesMap.remove(serviceTypeName);

        Cache resourceNamesCache    
                = (Cache)resourceNamesMap.get(serviceTypeName);
        if ((resourceNamesCache == null) || (resourceNamesCache.isEmpty())) {
            return;
        }
        try {
            DEBUG.error("PolicyEvaluator.policyChanged: enterred try block");
            ServiceTypeManager stm = ServiceTypeManager.getServiceTypeManager();
            ServiceType serviceType = stm.getServiceType(serviceTypeName);
            Set resourceNamesToRemove = new HashSet();
            synchronized(resourceNamesCache) {
                Enumeration resourceNames = resourceNamesCache.keys();
                while (resourceNames.hasMoreElements()) {
                    String resourceName = (String)resourceNames.nextElement();
                    if (resourceNamesToRemove.contains(resourceName)) {
                        continue;
                    }
                    Set affectedResourceNames = pe.getResourceNames();
                    Iterator iter = affectedResourceNames.iterator();
                    while (iter.hasNext())  {
                        String affectedResourceName = (String)iter.next();
                        if (serviceType.compare(resourceName, 
                                    affectedResourceName)
                                    != ResourceMatch.NO_MATCH) {
                            resourceNamesToRemove.add(resourceName);
                        }
                    }
                }
                Iterator iter1 = resourceNamesToRemove.iterator();
                while (iter1.hasNext()) {
                    String resourceNameToRemove = (String) iter1.next();
                    resourceNamesCache.remove(resourceNameToRemove);
                }
            }
        } catch (SSOException e) {
            DEBUG.error("PolicyEvaluator.policyChanged:", e);
        } catch (PolicyException pex) {
            DEBUG.error("PolicyEvaluator.policyChanged:", pex);
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("PolicyEvaulator.policyChanged():serviceTypeName="
                    + serviceTypeName
                    + ", new cached resoruceNames=" 
                    + resourceNamesMap.get(serviceTypeName));
        }
    }

    /**
     * Add an advice to the policy decision.
     * @param pd <code>PolicyDecision</code> in which to add the advice.
     * @param adviceKey key to the condition generating the advice
     *        like SessionCondition.SESSION_CONDITION_ADVICE, 
     *        AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE
     * @param adviceValue advice message to be added to the advice
     */
    private static void addAdvice(PolicyDecision pd, String adviceKey, 
            String adviceValue) {
        if ((pd != null) 
                    && (pd.hasAdvices())) {
            Map actionDecisions = pd.getActionDecisions();
            Iterator actionDecisionIter = actionDecisions.keySet().iterator();
            while (actionDecisionIter.hasNext()) {
                String key = (String) actionDecisionIter.next();
                ActionDecision ad = (ActionDecision) actionDecisions.get(key);
                Map advices = ad.getAdvices();
                if ((advices != null) && !advices.isEmpty()) {
                    Set values = (Set)advices.get(adviceKey);
                    if (values == null) {
                        values = new HashSet();
                    }
                    values.add(adviceValue);
                    advices.put(adviceKey, values);
                }
            }
        }
    }

    /**
     * Get the policy decision for a resource ignoring the subject
     */

    PolicyDecision getPolicyDecisionIgnoreSubjects(String resourceName, 
            Set actionNames, Map env) throws PolicyException, SSOException  {

        Set originalResourceNames = new HashSet(2);
        originalResourceNames.add(resourceName);

        /*
         * Add request resourceName and request actionNames to the envParameters
         * so that Condition(s)/ResponseProvider(s) can use them if necessary
         */
        Set resourceNames = new HashSet(2);
        resourceNames.add(resourceName);

        /* compute for all action names if passed in actionNames is
           null or empty */
        if ( (actionNames == null) || (actionNames.isEmpty()) ) {
            actionNames = serviceType.getActionNames();
        }

        Set actions = new HashSet();
        if (actionNames != null) {
            actions.addAll(actionNames);
        }

        //We create new HashMap in place of empty map since
        //Collections.EMPTY_MAP can not be modified
        if ((env == null) || env.isEmpty()) {
            env = new HashMap();
        }

        env.put(SUN_AM_REQUESTED_RESOURCE, resourceNames);
        env.put(SUN_AM_ORIGINAL_REQUESTED_RESOURCE, 
                originalResourceNames);
        env.put(SUN_AM_REQUESTED_ACTIONS, actions);
        env.put(SUN_AM_POLICY_CONFIG, 
                policyManager.getPolicyConfig());

        return getPolicyDecision(null, resourceName, actionNames, env,
                new HashSet());
    }


    /**
     * Get the set of role DNs of a user. The role DNs are cached to 
     * improve the performance of IdentityServerRole subject membership
     * validation.
     *
     * @param token single sign on token of the user evaluating policies
     *
     * @return The set of user <code>nsRole</code> attribute values
     *
     * @exception SSOException single-sign-on token invalid or expired
     * @exception PolicyException if an error occured while getting the
     *            user's nsRole attribute value set
     */
     public static Set getUserNSRoleValues(SSOToken token)
        throws SSOException, PolicyException {
        if (userNSRoleCacheTTL == 0) {
            synchronized(userNSRoleCache) {
                String orgName = ServiceManager.getBaseDN();
                Map pConfigValues = PolicyConfig.getPolicyConfig(orgName);
                userNSRoleCacheTTL = 
                     PolicyConfig.getSubjectsResultTtl(pConfigValues);
                if (userNSRoleCacheTTL <= 0) {
                    userNSRoleCacheTTL = DEFAULT_USER_NSROLE_CACHE_TTL;
                    if (DEBUG.warningEnabled()) {
                        DEBUG.warning("Invalid TTL got from configuration."
                                    + " Set TTL to default:" 
                                    + userNSRoleCacheTTL);
                    }
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("userNSRoleCacheTTL=" 
                                   + userNSRoleCacheTTL);
                }
            }
        }
        if (token == null) {
            return null;
        }
        String tokenIDStr = token.getTokenID().toString();
        Object[] element = (Object[])userNSRoleCache.get(tokenIDStr);
        if (element != null) {
            Long timeStamp = (Long)element[0];
            long timeToLive = 0;
            if (timeStamp != null) {
                timeToLive = timeStamp.longValue();
            }
            long currentTime = System.currentTimeMillis();
            if (timeToLive > currentTime) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("PolicyEvaluator.getUserNSRoleValues():"
                              + " get the nsRole values from cache.\n");
                }
                return (HashSet)element[1];
            }
        }
        // add or update the cache entry.
        // we come here either the token is first registered with the
        // cache or the cache element is out of date. 
        try {
            AMStoreConnection am = new AMStoreConnection(token);
            AMUser user = am.getUser(token.getPrincipal().getName());
            if ((user == null) || !(user.isActivated())) {
                return null;
            }
            Set roleSet = new HashSet();
            Set roles = new HashSet();
            // get all the roles assigned to the user
            Set staticRoles = user.getRoleDNs();
            Set filteredRoles = user.getFilteredRoleDNs();
            if (staticRoles != null) {
                roles.addAll(staticRoles);
            }
            if (filteredRoles != null) {
                roles.addAll(filteredRoles);
            }
            if (!roles.isEmpty()) {
                Iterator iter = roles.iterator();
                while (iter.hasNext()) {
                    String role = (String) iter.next();
                    if (role != null) { 
                        roleSet.add((new DN(role)).toRFCString().toLowerCase());
                    }
                }
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("PolicyEvaluator.getUserNSRoleValues():"
                            + " added user nsRoles: " + roleSet);
            }
            Object[] elem = new Object[2];
            elem[0] = new Long(System.currentTimeMillis() 
                               + userNSRoleCacheTTL);
            elem[1] = roleSet;
            userNSRoleCache.put(tokenIDStr, elem);
            if (!ssoListenerRegistry.containsKey(tokenIDStr)) {
                token.addSSOTokenListener(ssoListener);
                ssoListenerRegistry.put(tokenIDStr, ssoListener);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("PolicyEvaluator.getUserNSRoleValues():"
                            + " sso listener added .\n");
                }
            }
            return roleSet;
        } catch (AMException e) {
            throw (new PolicyException(e));
        }
    }

    /**
     * record stats for policyResultsCache,  ssoListenerRegistry, 
     * policyListenerRegistry, userNSRoleCache, resouceNamesMap
     */
    static void printStats(Stats policyStats) {


        int resultsCacheSize = 0;
        synchronized (policyResultsCache) {
            resultsCacheSize = policyResultsCache.size();
        }
        policyStats.record("PolicyEvaluator: Number of services in "
                + " resultsCache: " + resultsCacheSize);

        policyStats.record("PolicyEvaluator: Number of token IDs in "
                + " sessionListernerRgistry:"
                + ssoListenerRegistry.size());

        policyStats.record("PolicyEvaluator: Number of serviceNames "
                + " in policyListenerRegistry: "
                + policyListenerRegistry.size());

        policyStats.record("PolicyEvaluator: Number of token IDs "
                + " in role cahce: " + userNSRoleCache.size());

        policyStats.record("PolicyEvaluator:Number of serviceNames in "
                + " resourceNames cache: " 
                + resourceNamesMap.size());
    }
}
