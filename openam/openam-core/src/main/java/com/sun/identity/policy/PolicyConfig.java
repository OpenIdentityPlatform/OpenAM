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
 * $Id: PolicyConfig.java,v 1.10 2009/01/28 05:35:01 ww203982 Exp $
 *
 */



package com.sun.identity.policy;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Iterator;

import com.sun.identity.sm.*;
import com.iplanet.sso.*;
import com.sun.identity.shared.datastruct.CollectionHelper;

import com.sun.identity.shared.ldap.util.DN;

/**
 * The <code>PolicyConfig</code> class manages policy configuration for 
 * an organization and resource comparator configuration for a <code>
 * serviceType</code>.
 * The policy organization configuration is defined in amPolicyConfiguration 
 * service. The policy configuration values need to be set for each 
 * organization. The <code>Subject</code> implementations get these 
 * configuration values as a <code>Map</code>. The keys to the map are defined
 * as constants in this class. Different Subject implementations need different
 * key values. For example, LDAP Group subject needs <code>
 * LDAP_GROUP_SEARCH_FILTER, LDAP_GROUP_SEARCH_SCOPE</code>. All subject 
 * plugins that do not use Identity repository API, will  require <code>
 * LDAP_SERVER, LDAP_BASE_DN, LDAP_BIND_DN, LDAP_BIND_PASSWORD</code>. 
 * <p>
 * The resource comparator configuration is a  <code>Map</code>. The keys
 * to this map are serviceType names. For example, "iplanetAMWebAgentService".
 * The value for these keys is also a <code>Map</code>. The value map contains
 * following keys. This map is passed to the ResourceComparator class while
 * instantiating a ResourceComparator class.
 * The map contains the following keys:
 * <li><code>RESOURCE_COMPARATOR_CLASS</code></li>
 * <li><code>RESOURCE_COMPARATOR_WILDCARD</code></li>
 * <li><code>RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD</code></li>
 * <li><code>RESOURCE_COMPARATOR_DELIMITER</code> </li>
 * <li><code>RESOURCE_COMPARATOR_CASE_SENSITIVE</code></li>
 */

public class PolicyConfig implements com.sun.identity.sm.ServiceListener {


    public static final String LDAP_SERVER =
        "iplanet-am-policy-config-ldap-server";

    public static final String LDAP_BASE_DN =
        "iplanet-am-policy-config-ldap-base-dn";

    public static final String LDAP_USERS_BASE_DN =
        "iplanet-am-policy-config-ldap-users-base-dn";

    public static final String LDAP_BIND_DN =
        "iplanet-am-policy-config-ldap-bind-dn";

    public static final String LDAP_BIND_PASSWORD =
        "iplanet-am-policy-config-ldap-bind-password";

    public static final String LDAP_ORG_SEARCH_FILTER =
        "iplanet-am-policy-config-ldap-organizations-search-filter";

    public static final String LDAP_ORG_SEARCH_SCOPE =
        "iplanet-am-policy-config-ldap-organizations-search-scope";

    public static final String LDAP_GROUP_SEARCH_FILTER =
        "iplanet-am-policy-config-ldap-groups-search-filter";

    public static final String LDAP_GROUP_SEARCH_SCOPE =
        "iplanet-am-policy-config-ldap-groups-search-scope";

    public static final String LDAP_USERS_SEARCH_FILTER =
        "iplanet-am-policy-config-ldap-users-search-filter";

    public static final String LDAP_USERS_SEARCH_SCOPE =
        "iplanet-am-policy-config-ldap-users-search-scope";

    public static final String LDAP_ROLES_SEARCH_FILTER =
        "iplanet-am-policy-config-ldap-roles-search-filter";

    public static final String LDAP_ROLES_SEARCH_SCOPE =
        "iplanet-am-policy-config-ldap-roles-search-scope";

    public static final String LDAP_ORG_SEARCH_ATTRIBUTE =
        "iplanet-am-policy-config-ldap-organizations-search-attribute";

    public static final String LDAP_GROUP_SEARCH_ATTRIBUTE =
        "iplanet-am-policy-config-ldap-groups-search-attribute";
    
    public static final String LDAP_USER_SEARCH_ATTRIBUTE =
        "iplanet-am-policy-config-ldap-users-search-attribute";

    public static final String LDAP_ROLES_SEARCH_ATTRIBUTE =
        "iplanet-am-policy-config-ldap-roles-search-attribute";

    public static final String LDAP_SEARCH_TIME_OUT =
        "iplanet-am-policy-config-search-timeout";
    
    public static final String LDAP_SEARCH_LIMIT =
        "iplanet-am-policy-config-search-limit";

    public static final String LDAP_CONNECTION_POOL_MIN_SIZE =
        "iplanet-am-policy-config-connection_pool_min_size";

    public static final String LDAP_CONNECTION_POOL_MAX_SIZE =
        "iplanet-am-policy-config-connection_pool_max_size";

    public static final String LDAP_SSL_ENABLED =
        "iplanet-am-policy-config-ldap-ssl-enabled";

    public static final String IS_ROLES_BASE_DN =
        "iplanet-am-policy-config-is-roles-base-dn";

    public static final String IS_ROLES_SEARCH_SCOPE =
        "iplanet-am-policy-config-is-roles-search-scope";

    public static final String SELECTED_SUBJECTS =
        "iplanet-am-policy-selected-subjects";

    public static final String SELECTED_REFERRALS =
        "iplanet-am-policy-selected-referrals";

    public static final String SELECTED_CONDITIONS =
        "iplanet-am-policy-selected-conditions";

    public static final String SELECTED_RESPONSE_PROVIDERS =
        "sun-am-policy-selected-responseproviders";

    public static final String SELECTED_DYNAMIC_ATTRIBUTES =
        "sun-am-policy-dynamic-response-attributes";

    public static final String USER_ALIAS_ENABLED =
        "iplanet-am-policy-config-user-alias-enabled";

    public static final String RESOURCE_COMPARATOR =
        "iplanet-am-policy-config-resource-comparator";

    public static final String RESOURCE_COMPARATOR_TYPE = "serviceType";

    public static final String RESOURCE_COMPARATOR_CLASS = "class";

    public static final String RESOURCE_COMPARATOR_DELIMITER = "delimiter";

    public static final String RESOURCE_COMPARATOR_WILDCARD = "wildcard";

    public static final String RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD 
        = "oneLevelWildcard";

    public static final String RESOURCE_COMPARATOR_CASE_SENSITIVE =
        "caseSensitive";

    public static final String CONTINUE_EVALUATION_ON_DENY_DECISION 
            = "iplanet-am-policy-config-continue-evaluation-on-deny-decision";

    public static final String ORG_ALIAS_MAPPED_RESOURCES_ENABLED 
            = "sun-am-policy-config-org-alias-mapped-resources-enabled";

    public static final String ADVICES_HANDLEABLE_BY_AM 
            = "sun-am-policy-config-advices-handleable-by-am";

    public static final String ORG_DN = "orgDN";

    /** 
     * attribute to define value for Subjects result ttl 
     */
    public static final String SUBJECTS_RESULT_TTL 
            = "iplanet-am-policy-config-subjects-result-ttl";

    public static final String POLICY_CONFIG_SERVICE 
            = "iPlanetAMPolicyConfigService";

    /** 
     * OpenSSO directory host.
     */
    public static final String ISDS_HOST = PolicyUtils.getISDSHostName();

    private static ServiceConfigManager scm = null;
    private static ServiceSchemaManager ssm = null;
    private static Map attrMap = new HashMap();
    private static Map resourceCompMap = new HashMap();
    private static PolicyCache policyCache;

    static boolean continueEvaluationOnDenyDecisionFlag = false;
    static Set advicesHandleableByAM = null;
    static boolean orgAliasMappedResourcesEnabledFlag = false;
  
    private PolicyConfig() {
        // do nothing
    }

    /**
     * Returns the resource comparator configuration for the given 
     * service type
     * @param  service <code>ServiceType</code> name
     *
     * @return - Map containing data for <code>RESOURCE_COMPARATOR_CLASS</code>,
     * <code>RESOURCE_COMPARATOR_DELIMITER</code>, 
     * <code>RESOURCE_COMPARATOR_WILDCARD</code>, 
     * <code>RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD</code>, 
     * <code>RESOURCE_COMPARATOR_CASE_SENSITIVE</code> keys.
     *  Note that return value would be null if service name passed in is null 
     * or if there is no configuration available for service
     */
    public static Map getResourceCompareConfig(String service) 
                                                throws PolicyException {
        Map config = null;
        if (scm == null || ssm == null) {
            // following should happen only once, until scm is not null
            try {
                scm = new ServiceConfigManager(POLICY_CONFIG_SERVICE, 
                                        ServiceTypeManager.getSSOToken());
                ssm = new ServiceSchemaManager(POLICY_CONFIG_SERVICE, 
                                        ServiceTypeManager.getSSOToken());
                PolicyConfig pcm = new PolicyConfig();
                scm.addListener(pcm); // listener for org config changes
                ssm.addListener(pcm); // listener for global schema changes
            } catch (SMSException se) {
                PolicyManager.debug.error("getResourceCompareConfig: " +
                                "Unable to create ServiceConfigManager", se);
                throw (new PolicyException(se));
            } catch (SSOException se) {
                PolicyManager.debug.error("getResourceCompareConfig: " + 
                                "Unale to create ServiceConfigManager", se);
                throw (new PolicyException(se));
            }
        }
        ServiceSchema globalSchema = null;
        if ( (service == null) || !resourceCompMap.containsKey(service)) {
            try {
                globalSchema = ssm.getGlobalSchema();    
            } catch (SMSException se) {
                PolicyManager.debug.error("getResourceCompConfig: " +
                                "Unable to get ServiceConfig", se);
                throw (new PolicyException(se));
            }
            if (globalSchema != null) {
                Map attributeDefaults =  globalSchema.getAttributeDefaults();
                setContinueEvaluationOnDenyDecision(attributeDefaults);
                setOrgAliasMappedResourcesEnabled(attributeDefaults);
                setAdvicesHandleableByAM(attributeDefaults);
                processResourceMap(attributeDefaults);
            }
        }
        if (service != null)  {
            synchronized(resourceCompMap) {
                config = ((Map)resourceCompMap.get(service));
            }
        }
        return config;
    }

    /**
     * this method returns the policy configuration for the given organization.
     * @param org Organization name
     *
     * @return  Map of organization configuration attributes. The possible
     * keys in the map are defined in <code>PolicyConfig</code> 
     *
     * @throws  PolicyException if it is not able to get the policy 
     * configuration for the given organization.
     */
    public static Map getPolicyConfig(String org) throws PolicyException {

        org = new DN(org).toRFCString().toLowerCase();
        if (policyCache == null) {
            policyCache = PolicyCache.getInstance();
        }
        if (scm == null) {
            // following should happen only once, until scm is not null
            try {
                scm = new ServiceConfigManager(POLICY_CONFIG_SERVICE, 
                                        ServiceTypeManager.getSSOToken());
                ssm = new ServiceSchemaManager(POLICY_CONFIG_SERVICE, 
                                        ServiceTypeManager.getSSOToken());
                PolicyConfig pcm = new PolicyConfig();
                scm.addListener(pcm); // listener for org config changes
                ssm.addListener(pcm); // listener for global schema changes
            } catch (SMSException se) {
                PolicyManager.debug.error("getPolicyConfig: " +
                                "Unable to create ServiceConfigManager", se);
                throw (new PolicyException(se));
            } catch (SSOException se) {
                PolicyManager.debug.error("getPolicyConfig " + 
                                "Unable to create ServiceConfigManager", se);
                throw (new PolicyException(se));
            }
        }
        if (!attrMap.containsKey(org)) {
            ServiceConfig orgConfig = null;
            try {
                orgConfig = scm.getOrganizationConfig(org, null);
            } catch (SMSException se) {
                PolicyManager.debug.error("getPolicyConfig: " +
                                "Unable to get ServiceConfig", se);
                throw (new PolicyException(se));
            } catch (SSOException se) {
                PolicyManager.debug.error("getPolicyConfig: " + 
                                        "Unable to get ServiceConfig", se);
                throw (new PolicyException(se));
            }
            if (orgConfig != null) {
                Map orgAttrMap = processOrgAttrMap(orgConfig.getAttributes());

                //Add organizationDN to the map
                orgAttrMap.put(PolicyConfig.ORG_DN, org);
                synchronized (attrMap) {
                    attrMap.put(org, orgAttrMap);
                }
            }
        }
        synchronized(attrMap) {
            return((Map)attrMap.get(org));
        }
    }

    /**
     * This method will be invoked when a service's schema has been changed.
     *
     * @param serviceName name of the service
     * @param version version of the service
     */
    public void schemaChanged(String serviceName, String version) {
        ServiceSchema globalSchema = null;
        PolicyManager.debug.message("PolicyConfig.schemaChanged():entering");
        try {
            globalSchema = ssm.getGlobalSchema();
        } catch (SMSException se) {
            PolicyManager.debug.error("globalConfigChanged: " +
                                "Unable to get global config ", se);
            return;
        }
        if (globalSchema != null) {
            Map attributeDefaults =  globalSchema.getAttributeDefaults();
            setContinueEvaluationOnDenyDecision(attributeDefaults);
            setOrgAliasMappedResourcesEnabled(attributeDefaults);
            setAdvicesHandleableByAM(attributeDefaults);
            setOrgAliasMappedResourcesEnabled(attributeDefaults);
            processResourceMap(attributeDefaults);
        }
    }

    /**
     * This method will be invoked when a service's global configuation
     * data has been changed. The parameter groupName denote the name
     * of the configuration grouping (e.g. default) and serviceComponent
     * denotes the service's sub-component that changed
     *
     * @param serviceName name of the service
     * @param version version of the service
     * @param serviceComponent name of the service components that
     *                          changed
     */
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int changeType) {
        // NO-OP
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

        Map orgAttrMap = null;
        ServiceConfig orgConfig = null;
        try {
            orgConfig = scm.getOrganizationConfig(orgName, null);
        } catch (SMSException se) {
            PolicyManager.debug.error("orgConfigChanged: " +
                                "Unable to get org config: " + orgName, se);
            return;
        } catch (SSOException se) {
            PolicyManager.debug.error("orgConfigChanged: " +
                                "Unable to get org config: " + orgName, se);
            return;
        }

        if (orgConfig != null) {
            orgAttrMap = processOrgAttrMap(orgConfig.getAttributes());
        }
        synchronized (attrMap) {
            attrMap.put(orgName, orgAttrMap);
        }
        if (policyCache != null) {
            policyCache.policyConfigChanged(orgName);
        }
    }

    /**
     * This method converts the attributes map got from organization config
     * into a key-value map. The keys are specified as constants in this class.
     * The service management returns value for each key as a set. This method
     * converts that to a string for easy access since all the organization
     * policy configuration attribute values are string.
     */

    private static Map processOrgAttrMap(Map orgConfigMap) {

        /**
         * Its known that the attributes are single type and string value
         * Process the map to get the string value.
         * use the ServiceSchemaManager and ServiceSchema to get the
         * attribute type for processing.
         */

        Set attrKeys = orgConfigMap.keySet();
        Map orgAttrMap = new HashMap();
        if ((attrKeys != null) && (!attrKeys.isEmpty())) {
            Iterator keysIterator = attrKeys.iterator();
            while ( keysIterator.hasNext() ) {
                String attrName = (String) keysIterator.next();
                Set values = (Set) orgConfigMap.get(attrName);
                if (values == null || values.isEmpty()) {
                    continue;
                }
                if (attrName.equals(SELECTED_SUBJECTS) 
                        || attrName.equals(SELECTED_REFERRALS) ||
                            attrName.equals(SELECTED_RESPONSE_PROVIDERS) ||
                            attrName.equals(SELECTED_DYNAMIC_ATTRIBUTES) ||
                            attrName.equals(SELECTED_CONDITIONS)) {
                    orgAttrMap.put(attrName, values);
                    continue;
                }
                if (attrName.equals(LDAP_SERVER)) {
                    orgAttrMap.put(attrName, CollectionHelper.getServerMapAttr(
                        orgConfigMap, LDAP_SERVER));
                    continue;
                } 
                Iterator valIterator = values.iterator();
                while (valIterator.hasNext()) {
                    String attrValue = (String) valIterator.next();
                    if (attrName != null && attrValue != null) {
                        orgAttrMap.put(attrName, attrValue);
                       /**
                        * don't want to expose ldap bind passwd 
                        * in clear text
                          */
                        if (attrName.equals(LDAP_BIND_PASSWORD)) {
                            attrValue = PolicyUtils.encrypt(attrValue);
                            orgAttrMap.put(attrName, attrValue);
                        }
                        if (PolicyManager.debug.messageEnabled()) {
                            PolicyManager.debug.message("Attr Name = " 
                                   + attrName + ";  Attr Value = " + attrValue);
                        }
                    }
                }
            }
        }
        return(orgAttrMap);
    }

    /** This function process RESOURCE_COMPARATOR attribute. It processes each 
     * element in the set. It creates a Map for each entry in the values 
     * <code>Set</code>. The serviceType becomes the key for the maps.
     * For  ex: serviceType=url service|class=PrefixCompare|wildcard=*|case=true
     * becomes a map indexed by "url service". The value of the key is a map.
     * This map would contain values for class, wildcard one level wildcard 
     * and case keys.
     */
    private static void processResourceMap(Map attrs) {


        Set values = (Set) attrs.get(RESOURCE_COMPARATOR);
        if (values != null && !values.isEmpty()) {
            // values is a set. each element in the set is of the form
            // serviceType=1|class=com.sun.identity.policy.Class|wildcard=*|
            // caseSensitive=true|one_level_wildcard=-*-
            Iterator valIterator = values.iterator();
            while (valIterator.hasNext()) {
                String elemVal = (String) valIterator.next();
                if (elemVal != null) {
                    StringTokenizer st = new StringTokenizer(elemVal, "|");
                    String[] tokens = new String[6];
                    int count = 0;
                    while (st.hasMoreTokens()) {
                        tokens[count++] = st.nextToken();
                        if (count > 5) { // accept only first six tokens
                            break;
                        }
                    }
                    Map configMap = new HashMap();
                    String serviceType = null;
                    // right now we don't handle spaces within elements
                    // separated by "|". We can add it later.
                    for (int i = 0; i < count; i++) {
                        int equal = tokens[i].indexOf("=");
                        String name = tokens[i].substring(0, equal);        
                        String value = tokens[i].substring(equal + 1);        
                        if (name == null) {
                            PolicyManager.debug.error("Resource comapartaor: "
                                                + " name is null");
                            continue;
                        }
                        if (value == null) {
                            PolicyManager.debug.error("Resource comapartaor: "
                                                + " value is null");
                            continue;
                        }
                        if (PolicyManager.debug.messageEnabled()) {
                            PolicyManager.debug.message("Attr Name = " + 
                                                        name +
                                            " Attr Value = " + value);
                        }
                        if (name.equalsIgnoreCase(RESOURCE_COMPARATOR_TYPE)) {
                            serviceType = value;
                        } else if (name.equalsIgnoreCase(
                            RESOURCE_COMPARATOR_CLASS)) {
                            configMap.put(RESOURCE_COMPARATOR_CLASS, value);
                        } else if (name.equalsIgnoreCase(
                            RESOURCE_COMPARATOR_DELIMITER)) {
                            configMap.put(RESOURCE_COMPARATOR_DELIMITER, value);
                        } else if (name.equalsIgnoreCase(
                            RESOURCE_COMPARATOR_WILDCARD)) {
                            configMap.put(RESOURCE_COMPARATOR_WILDCARD, value);
                        } else if (name.equalsIgnoreCase(
                            RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD)) {
                            configMap.put(RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD
                                , value);
                        } else if (name.equalsIgnoreCase(
                            RESOURCE_COMPARATOR_CASE_SENSITIVE)) {
                            configMap.put(RESOURCE_COMPARATOR_CASE_SENSITIVE,
                                value);
                        }
                    }
                    if (PolicyManager.debug.messageEnabled()) {
                        PolicyManager.debug.message("PolicyConfig."+
                            "processResourceMap():configMap.toString()"+
                            configMap.toString());
                    }
                    synchronized(resourceCompMap) {
                        resourceCompMap.put(serviceType, configMap);
                    }
                }
            }
        }
    }


    /**
     * Gets subjectsResultTtl - time in milliseconds for which result of
     * subjects evaluation would be cached based, on the policyConfig map 
     * passed.
     *
     * @param policyConfig policy config map that is used to compute 
     *        subjectsResultTtl.  Value of key 
     *        PolicyConfig.SUBJECTS_RESULT_TTL in the map is assumed to be 
     *        value of subjectsResultTtl in minutes. If the value is not 
     *        defined in the map or it can not be parsed as int, the value 
     *        would default to <code>0</code>  
     *
     * @return subjectsResultTtl
     */
    public static long getSubjectsResultTtl(Map policyConfig) {
        String subjectsTtl = null;
        if (policyConfig != null) {
                subjectsTtl 
                = (String)policyConfig.get(PolicyConfig.SUBJECTS_RESULT_TTL);
        }
        long subjectsResultTtl = 0;
        if (subjectsTtl != null) {
            try {
                subjectsResultTtl 
                        = Integer.parseInt(subjectsTtl) * 60 * 1000;
            } catch (NumberFormatException nfe) {
                if ( PolicyManager.debug.warningEnabled() ) {
                        PolicyManager.debug.warning(
                        "NumberFormatException while parsing "
                        + " subjectsResultTtl defined in policyConfig "
                        + " service  using default " 
                        + PolicyManager.DEFAULT_SUBJECTS_RESULT_TTL);
                }
            }
        }
        return subjectsResultTtl;
    }


    /**
     * set the value for attribute CONTINUE_EVALUATION_ON_DENY_DECISION
     * getting it as one attribute in the <code>attributes</code> Map.
     */
    static void setContinueEvaluationOnDenyDecision(Map attributes) {
        if (attributes != null){
            Set codSet =
                    (Set)attributes.get(
                    CONTINUE_EVALUATION_ON_DENY_DECISION);
            
            if ((codSet != null) && !codSet.isEmpty()) {
                String codValue = (String)(codSet.iterator().next());
                if (codValue != null) {
                    continueEvaluationOnDenyDecisionFlag
                        = Boolean.valueOf(codValue).booleanValue();
                }
                if (PolicyManager.debug.messageEnabled()) {
                    PolicyManager.debug.message("PolicyConfig."
                            + "setContinueEvaluationOnDenyDecision():"
                            + "global attribute "
                            + " continueEvaluationOnDenyDecision="
                            + codValue);
                }
            }
        }
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message("PolicyConfig."
                    + "setContinueEvaluationOnDenyDecision():"
                    + "continueEvaluationOnDenyDecision="
                     + continueEvaluationOnDenyDecisionFlag);
        }
    }

    /**
     * set the value for attribute ORG_ALIAS_MAPPED_RESOURCES_ENABLED
     * getting it as one attribute in the <code>attributes</code> Map.
     */
    static void setOrgAliasMappedResourcesEnabled(Map attributes) {
        if (attributes != null){
            Set amreSet = (Set)attributes.get(
                    ORG_ALIAS_MAPPED_RESOURCES_ENABLED);
            
            if ((amreSet != null) && !amreSet.isEmpty()) {
                String amreValue = (String)(amreSet.iterator().next());
                if (amreValue != null) {
                    orgAliasMappedResourcesEnabledFlag
                        = Boolean.valueOf(amreValue).booleanValue();
                }
                if (PolicyManager.debug.messageEnabled()) {
                    PolicyManager.debug.message("PolicyConfig."
                            + "setOrgAliasMappedResourcesEnabled():"
                            + "global attribute "
                            + " orgAliasMappedResourcesEnabledFlag="
                            + amreValue);
                }
            }
        }
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message("PolicyConfig."
                    + "setOrgAliasMappedResourcesEnabled():"
                    + "orgAliasMappedResourcesEnabledFlag="
                     + orgAliasMappedResourcesEnabledFlag);
        }
    }

    /**
     * return boolean representing the value of 
     * CONTINUE_EVALUATION_ON_DENY_DECISION as boolean true/false
     */
    static boolean continueEvaluationOnDenyDecision() {
        return continueEvaluationOnDenyDecisionFlag;
    }

    /**
     * return boolean representing the value of 
     * ORG_ALIAS_MAPPED_RESOURCES_ENABLED as boolean true/false
     */
    static boolean orgAliasMappedResourcesEnabled() {
        return orgAliasMappedResourcesEnabledFlag;
    }

    /**
     * get the value for ADVICES_HANDLEABLE_BY_AM attribute 
     * from a map of attributes and
     * intialize <code>advicesHandleableByAM</code> with it.
     */
    private static void setAdvicesHandleableByAM(Map attributes) {
        if (attributes != null){
            Set advices = (Set)attributes.get(ADVICES_HANDLEABLE_BY_AM);
            
            if (advices != null) {
                advicesHandleableByAM = advices;
            }
        }

        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message("PolicyConfig."
                    + "setAdvicesHandleableByAM():"
                    + "global attribute advicesHandleableByAM="
                    + advicesHandleableByAM);
        }
        if (advicesHandleableByAM == null) { 
            advicesHandleableByAM = Collections.EMPTY_SET;
        }

    }

    /**
     * Returns names of policy advices that could be handled by OpenSSO
     * Enterprise if PEP redirects the user agent to OpenSSO.
     * @return <code>Set</code> representing names of policy advices 
     *         OpenSSO could handle.
     */
    public static Set getAdvicesHandleableByAM() throws PolicyException {
        if (advicesHandleableByAM == null) {
            getResourceCompareConfig(null); //to bootstrap schema
        }
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message("PolicyConfig."
                    + "getAdvicesHandleableByAM():"
                    + "returning global attribute advicesHandleableByAM="
                    + advicesHandleableByAM);
        }
        return advicesHandleableByAM;
    }
}
