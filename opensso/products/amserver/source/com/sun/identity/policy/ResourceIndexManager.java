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
 * $Id: ResourceIndexManager.java,v 1.4 2009/02/28 04:14:58 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package  com.sun.identity.policy;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import org.w3c.dom.*;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.Cache;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.identity.policy.plugins.OrgReferral;

/**
 * Class to find set of policy names that are applicable given 
 * resource type and resource name.  This provides a subset of 
 * features provided by ResourceManager. ResourceIndexManager
 * uses the index that is maintained by the ResourceManager in 
 * the data store. This class maintains one resource index for 
 * each resource type.
 */
class ResourceIndexManager {

    static final Debug DEBUG = PolicyManager.debug;

    //Constants to build XML representation
    static final String LTS = "<";
    static final String LTSS = "</";
    static final String GTS = ">";
    static final String SGTS = "/>";
    static final String SPACE = " ";
    static final String QUOTE = "\"";
    static final String EQUALS = "=";
    static final String NEW_LINE = "\n";
    static final String POLICY_CROSS_REFERENCES = "PolicyCrossReferences";
    static final String NAME = "name";
    static final String TYPE = "type";
    static final String RESOURCES = "Resources";
    static final String REFERENCE = "Reference";
    static final String POLICY_NAME = "PolicyName";

    static final int POLICY_NAMES_CACHE_CAP = 10000;

    private Map resourceIndices = Collections.synchronizedMap(new HashMap());
    private ResourceManager resourceManager;

    /**
     * Constructs a ResourceIndexManager
     * @param  resourceManager resource manager that would be used
     *         by this resource index manager
     */
    ResourceIndexManager (ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Returns the set of policy names applicable to the given resource name
     * and resource type.
     *
     * @param resourceType resource type
     * @param resourceName resource name
     * @param includeSrPolicies if <code>true</code> names of policies 
     *            applicable to super resources of the resource name 
     *            also are included in the return value
     * @return  set of policy names applicable to the given resource name
     *          and resource type
     * @throws PolicyException 
     */
    Set getPolicyNames (ServiceType resourceType, String resourceName, boolean
            includeSrPolicies) throws PolicyException {
        Set policyNames = null;
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        policyNames = resourceIndex.getPolicyNames(resourceName, 
                includeSrPolicies);
        if ( DEBUG.messageEnabled() ) {
            DEBUG.message("ResourceIndexManager.getPolicyNames "
                    + "- resourceName, policyNames=" 
                    + resourceName + ":" + policyNames);
        }
        return  policyNames;
    }

    /**
     * Returns the set of policy names applicable to the super resources
     * of given resource name of the given resource type.
     *
     * @param resourceType resource type
     * @param resourceName resource name
     * @return   set of policy names applicable to the super resources
     *           of given resource name of the given resource type.
     * @throws PolicyException 
     */
    Set getSuperResourcePolicyNames(ServiceType resourceType, 
        String resourceName) throws PolicyException 
    {
        Set policyNames = null;
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        policyNames = resourceIndex.getSuperResourcePolicyNames(
                resourceName); //include super resource policies
        if ( DEBUG.messageEnabled() ) {
            DEBUG.message("ResourceIndexManager.getPolicyNames "
                    + "- resourceName, policyNames=" 
                    + resourceName + ":" + policyNames);
        }
        return  policyNames;
    }

    /**
     * Returns the set of top level resource names
     * of the given resource type.
     *
     * @param resourceType resource type
     * @return  the set of top level resource names
     *          of the given resource type.
     * @throws PolicyException 
     */
    Set getTopLevelResourceNames(ServiceType resourceType) 
        throws PolicyException 
    {
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        return resourceIndex.getTopLevelResourceNames();

    }

    /**
     * Returns the set of policy names applicable to the given resource name
     * and its sub resources treating wild character 
     * in the policy resource name  as literal
     *
     * @param resourceType resource type
     * @param resourceName resource name
     * @return  set of policy names applicable to the given resource name
     *          and its sub resources
     * @throws PolicyException 
     */
    Set getSubResourcePolicyNames (ServiceType resourceType, 
            String resourceName) throws PolicyException {
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        return  resourceIndex.getSubResourcePolicyNames(resourceName);
    }

    /**
     * Returns the set of policy names applicable to the given resource name
     * and its sub resources treating wild character in the policy resource
     * name as wild
     *
     * @param resourceType resource type
     * @param resourceName resource name
     * @return  set of policy names applicable to the given resource name
     *          and its sub resources
     * @throws PolicyException 
     */
    Set getWildSubResourcePolicyNames (ServiceType resourceType, 
            String resourceName) throws PolicyException {
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        return  resourceIndex.getWildSubResourcePolicyNames(resourceType,
                resourceName);
    }

    /**
     * Clears resourceIndex of the given resource type name from 
     * the local cache. If an attempt is made to use this resource
     * index subsequently, it would be refreshed from the datastore.
     * 
     * @param resourceTypeName resource type name
     */
    void clearResourceIndex(String resourceTypeName) {

        Set resourceTypes = new HashSet();
        resourceTypes.addAll(resourceIndices.keySet());
        Iterator iter = resourceTypes.iterator();
        while ( iter.hasNext() ) {
            String resourceType = (String) iter.next();
            /*
              We compare ingoring the case since we get the resourceTypeName 
              from SMS notification. SMS converts resourceTypeName to lowercase
             */
            if ( resourceType.equalsIgnoreCase(resourceTypeName) ) {
                resourceIndices.remove(resourceType);
                break;
            }
        }
    }

    /**
     * Returns the resource index given the resource type. 
     * This would read from the data store, if the index 
     * was not aleady read from the datastore.
     *
     * @param resourceType resouce type
     * @return resource index read from the datastore
     * @throws PolicyException
     */
    ResourceIndex getResourceIndex(ServiceType resourceType) 
            throws PolicyException {
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        return resourceIndex;
    }

    /**
     * Returns the resource index from data store for the given 
     * resource type
     *
     * @param resourceType resouce type
     * @return resource index read from the datastore
     * @throws PolicyException
     */
    //private ResourceIndex getResourceIndexFromDataStore (ServiceType
    ResourceIndex getResourceIndexFromDataStore (ServiceType
            resourceType) throws PolicyException {
        ResourceIndex resourceIndex = new ResourceIndex(resourceType,
                resourceManager);
        resourceIndex.refreshFromDataStore();
        return  resourceIndex;
    }

    /**
     * Refreshes the resource index in the local cache reading 
     * from the data store
     *
     * @param resourceType resouce type
     * @return resource index read from the datastore
     * @throws PolicyException
     */
    private ResourceIndex refreshResourceIndexFromDataStore (ServiceType
            resourceType) throws PolicyException {
        ResourceIndex resourceIndex =
                getResourceIndexFromDataStore(resourceType);
        resourceIndices.put(resourceType.getName(), resourceIndex);
        return  resourceIndex;
    }

    /**
     * Adds a new index entry or updates an existing index entry
     * @param resourceType resource type
     * @param resourceName resource name
     * @param policyName policy name
     * @return  <code>true</code> if an index entry was added or 
     *          updated. <code>false</code> otherwise.
     * @throws PolicyException
     */
    private boolean addIndexEntry (ServiceType resourceType, String
            resourceName, String policyName) throws PolicyException {
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        return  resourceIndex.addIndexEntry(resourceName, policyName);
    }

    /**
     * Removes or updates an index entry 
     * @param resourceType resource type
     * @param resourceName resource name
     * @param policyName policy name
     * @return  <code>true</code> if an index entry was removed or 
     *          updated. <code>false</code> otherwise.
     * @throws PolicyException
     */
    private boolean removeIndexEntry (ServiceType resourceType, String
            resourceName, String policyName) throws PolicyException {
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        return  resourceIndex.removeIndexEntry(resourceName, policyName);
    }

    /** 
     * Returns the closest match and its children or all descendents of 
     *  the resource name with the given resource type.
     *
     *  @param resourceType resource type
     *  @param resourceName resource name
     *  @param followChild if <code>true</code> gets all the descendents,
     *         includindg direct children. Else, gets only direct children.
     *  @return set of names of child resources or all descendent resources
     * @throws PolicyException
     */
    Set getChildResourceNames(ServiceType resourceType, String resourceName, 
            boolean followChild) throws PolicyException {
        Set resourceNames = null;
        ResourceIndex resourceIndex =
                (ResourceIndex)resourceIndices.get(resourceType.getName());
        if (resourceIndex == null) {
            resourceIndex = refreshResourceIndexFromDataStore(resourceType);
        }
        resourceNames = resourceIndex.getChildResourceNames(resourceName, 
                followChild);
        return  resourceNames;
    }

    /** 
     * Saves the resource index in data store
     *  @param resourceType resource type
     *  @throws PolicyException
     *  @throws SSOException
     */
     void saveResourceIndex(String resourceType) 
            throws PolicyException, SSOException {
         ResourceIndex resourceIndex = 
                (ResourceIndex) resourceIndices.get(resourceType);
         if ( resourceIndex != null ) {
             String resourceIndexXML = resourceIndex.toXML();
             resourceManager.saveResourceIndex(resourceType, resourceIndexXML);
         }
     }

    /**
     *  Adds a policy's relevant content to the resource tree.
     *
     *  @param svtm service type manager
     *  @param token sso token
     *  @param policy the policy to be added
     *
     *  @exception PolicyException if unable to get the policy services,
     *  and will contain the exception thrown by SMS.
     *  @exception SSOException single-sign-on token invalid or expired
     */
    void addPolicyToResourceTree(ServiceTypeManager svtm, SSOToken token, 
            Policy policy) throws PolicyException, SSOException {

        Set ruleNames = policy.getRuleNames();
        Iterator iter = ruleNames.iterator();
        Set serviceNames = new HashSet();
        while (iter.hasNext()) {
            String ruleName = (String) iter.next();
            Rule rule = policy.getRule(ruleName);
            String serviceName = rule.getServiceTypeName();
            serviceNames.add(serviceName);
            ServiceType resourceType = svtm.getServiceType(serviceName);
            addIndexEntry(resourceType, rule.getResourceName(), 
                    policy.getName());
        }
        iter = serviceNames.iterator();
        while ( iter.hasNext() ) {
            String serviceName = (String) iter.next();
            saveResourceIndex(serviceName);
        }


        //Process Referrals
        Referrals referrals = policy.getReferrals();
        if ( referrals != null ) {
            Set referralNames = referrals.getReferralNames();
            if ( (referralNames != null) && (!referralNames.isEmpty()) ) {
                Iterator referralIter = referralNames.iterator();
                while ( referralIter.hasNext() ) {
                    String referralName= (String) referralIter.next();
                    Referral referral = referrals.getReferral(referralName);
                    if ( referral instanceof OrgReferral ) {
                        Set values = referral.getValues();
                        if ( (values != null) && (!values.isEmpty()) ) {
                            Iterator valueIter = values.iterator();
                            while ( valueIter.hasNext() ) {
                                Map servicePrefixMap = new HashMap();
                                String value = (String) valueIter.next();
                                PolicyManager pm 
                                    = new PolicyManager(token, value);
                                ResourceManager rm = pm.getResourceManager();
                                Set ruleNames1 = policy.getRuleNames();
                                Iterator ruleIter = ruleNames1.iterator();    
                                while ( ruleIter.hasNext() ) {
                                    String ruleName1 
                                        = (String) ruleIter.next();
                                    Rule rule1 = policy.getRule(ruleName1);
                                    String resourceName = 
                                            rule1.getResourceName();
                                    if ( resourceName != null ) {
                                        String service
                                            = rule1.getServiceTypeName();
                                        Set resourceNames 
                                            = (Set)servicePrefixMap.get(
                                                    service);
                                        if (resourceNames == null) {
                                            resourceNames = new HashSet();
                                            servicePrefixMap.put(service,
                                                    resourceNames);
                                        }
                                        resourceNames.add(resourceName);
                                    } 
                                }    
                                Iterator serviceIter =
                                        servicePrefixMap.keySet().iterator();
                                while (serviceIter.hasNext()) {
                                    String service = (String)serviceIter.next();
                                    Set resourceNames 
                                        = (Set)servicePrefixMap.get(service);
                                    rm.addResourcePrefixes(service,
                                            resourceNames);
                                }
                            } //processed a referral value
                        }
                    }
                }
            }
        }

    }

    /**
     *  Removes a policy's relevant content from the resource tree.
     *
     *  @param svtm service type manager
     *  @param token sso token
     *  @param policy the policy to be removed
     *
     *  @exception PolicyException if unable to get the policy services,
     *  and will contain the exception thrown by SMS.
     *  @exception SSOException single-sign-on token invalid or expired
     */
    void removePolicyFromResourceTree(ServiceTypeManager svtm, SSOToken token, 
            Policy policy) throws PolicyException, SSOException {

        Set ruleNames = policy.getRuleNames();
        Iterator iter = ruleNames.iterator();
        
        // iterating through each rule
        String ruleName = null;
        Rule rule = null;
        Set serviceNames = new HashSet();
        while (iter.hasNext()) {
            ruleName = (String) iter.next();
            rule = policy.getRule(ruleName);
            String serviceName = rule.getServiceTypeName();
            serviceNames.add(serviceName);
            ServiceType resourceType = svtm.getServiceType(serviceName);
            removeIndexEntry(resourceType, rule.getResourceName(), 
                    policy.getName());
        }
        iter = serviceNames.iterator();
        while ( iter.hasNext() ) {
            String serviceName = (String) iter.next();
            saveResourceIndex(serviceName);
        }

        //Process Referrals
        Referrals referrals = policy.getReferrals();
        if ( referrals != null ) {
            Set referralNames = referrals.getReferralNames();
            if ( (referralNames != null) && (!referralNames.isEmpty()) ) {
                Iterator referralIter = referralNames.iterator();
                while ( referralIter.hasNext() ) {
                    String referralName = (String) referralIter.next();
                    Referral referral = referrals.getReferral(referralName);
                    if ( referral instanceof OrgReferral ) {
                        Set values = referral.getValues();
                        if ( (values != null) && (!values.isEmpty()) ) {
                            Iterator valueIter = values.iterator();
                            while ( valueIter.hasNext() ) {
                                Map servicePrefixMap = new HashMap();
                                String value = (String) valueIter.next();
                                try {
                                    PolicyManager pm 
                                        = new PolicyManager(token, value);
                                    ResourceManager rm 
                                        = pm.getResourceManager();
                                    Iterator ruleIter = 
                                            policy.getRuleNames().iterator();
                                    while ( ruleIter.hasNext() ) {
                                        String ruleName1 
                                            = (String) ruleIter.next();
                                        Rule rule1 = policy.getRule(ruleName1);
                                        String resourceName = 
                                                rule1.getResourceName();
                                        if ( resourceName != null ) {
                                            String service
                                                = rule1.getServiceTypeName();
                                            Set resourceNames 
                                                    = (Set)servicePrefixMap
                                                    .get(service);
                                            if (resourceNames == null) {
                                                resourceNames = new HashSet();
                                                servicePrefixMap.put(service,
                                                        resourceNames);
                                            }
                                            resourceNames.add(resourceName);
                                        }
                                    }    
                                    Iterator serviceIter =
                                        servicePrefixMap.keySet().iterator();
                                    while (serviceIter.hasNext()) {
                                        String service 
                                            = (String)serviceIter.next();
                                        Set resourceNames 
                                            = (Set)servicePrefixMap.get(
                                            service);
                                        rm.removeResourcePrefixes(service,
                                                resourceNames);
                                    }
                                } catch (PolicyException e) {
                                    if (DEBUG.warningEnabled()) {
                                    DEBUG.warning("Could not clean up resource "
                                             + " prefixes in referrred to org :"
                                             + value + "-" + e.getMessage());
                                    }
                                }
                            } // processed referral value
                        }
                    }
                }
            }
        }

    }

    /**
     *  Replaces a policy's relevant content in the resource tree.
     *
     *  @param svtm service type manager
     *  @param token sso token
     *  @param oldPolicy the policy to be replaced
     *  @param newPolicy the policy to replace the existins policy with
     *
     *  @exception PolicyException if unable to get the policy services,
     *  and will contain the exception thrown by SMS.
     *  @exception SSOException single-sign-on token invalid or expired
     */
    void replacePolicyInResourceTree(ServiceTypeManager svtm, SSOToken token, 
            Policy oldPolicy, Policy newPolicy) 
            throws PolicyException, SSOException {

        removePolicyFromResourceTree(svtm, token, oldPolicy);
        addPolicyToResourceTree(svtm, token, newPolicy);
    }

    /** 
     * Class that holds the index of policy names by resource names
     *  for a resource type
     */
    //private static class ResourceIndex {
    static class ResourceIndex {
        private ServiceType resourceType;
        private ResourceManager resourceManager;
        private Set topLevelEntries = new HashSet();
        private Map policyNamesCache 
                = Collections.synchronizedMap(new Cache(POLICY_NAMES_CACHE_CAP));
        private Map policyNamesCacheFp 
                = Collections.synchronizedMap(new Cache(POLICY_NAMES_CACHE_CAP));

        /**
         * Constructs ResourceIndex
         * @param   resourceType resource type
         * @param   resourceManager resource manager
         */
        ResourceIndex (ServiceType resourceType, 
            ResourceManager resourceManager) {
            this.resourceType = resourceType;
            this.resourceManager = resourceManager;
        }

        /**
         * Returns a set of policy names applicable to a resource
         * @param resourceName resource name
         * @param includeSrPolicies if <code>true</code> names of policies 
         *        applicable to super resources of the resource name 
         *        also are included in the return value
         * @return set of policy names applicable to resource name
         */
        Set getPolicyNames(String resourceName, boolean includeSrPolicies) {
            Set policyNames = null;
            if (includeSrPolicies) {
                policyNames = (Set)policyNamesCacheFp.get(resourceName);
                if (policyNames == null) {
                    policyNames = new HashSet();
                    Iterator iter = topLevelEntries.iterator();
                    while (iter.hasNext()) {
                        ResourceIndexEntry resourceIndexEntry 
                                = (ResourceIndexEntry)iter.next();
                        policyNames.addAll(resourceIndexEntry.getPolicyNames(
                                resourceName, includeSrPolicies, resourceType));
                    } 
                    policyNamesCacheFp.put(resourceName, policyNames);
                }
            } 
            else {
                policyNames = (Set)policyNamesCache.get(resourceName);
                if (policyNames == null) {
                    policyNames = new HashSet();
                    Iterator iter = topLevelEntries.iterator();
                    while (iter.hasNext()) {
                        ResourceIndexEntry resourceIndexEntry 
                                = (ResourceIndexEntry)iter.next();
                        policyNames.addAll( resourceIndexEntry.getPolicyNames(
                                resourceName, includeSrPolicies, resourceType));
                    } 
                }
                policyNamesCache.put(resourceName, policyNames);
            }
            if ( DEBUG.messageEnabled() ) {
                DEBUG.message("ResourceIndex.getPolicyNames "
                        + "- resourceName, policyNames=" 
                        + resourceName + ":" + policyNames);
            }
            return  policyNames;
        }

        Set getSuperResourcePolicyNames(String resourceName) {
            Set policyNames = Collections.EMPTY_SET;
            ResourceIndexEntry resourceIndexEntry 
                    = findClosestMatch(resourceName);

            //true - include super resources
            if (resourceIndexEntry != null) {
                policyNames = resourceIndexEntry.getPolicyNames(true); 
            }
            return policyNames;
        }

        /**
         * Refreshes this resource index reading from data store
         * @throws PolicyException
         */
        void refreshFromDataStore () throws PolicyException {
            Node xmlRootNode = null;
            try {
                xmlRootNode = resourceManager.getXMLRootNode(
                        resourceType.getName());
            } catch (Exception e) {
                DEBUG.error("Error reading resource index from data store ", e);
                throw  new PolicyException(ResBundleUtils.rbName,
                        "error_reading_resource_index_from_data_store", 
                        null, e);
            }
            if (xmlRootNode != null) {
                Set topIndexEntryNodeSet = XMLUtils.getChildNodes(xmlRootNode, 
                        PolicyManager.POLICY_INDEX_REFERENCE_NODE);
                Iterator topIndexEntryNodes = topIndexEntryNodeSet.iterator();
                while (topIndexEntryNodes.hasNext()) {
                    Node topIndexEntryNode = (Node)topIndexEntryNodes.next();
                    String resourceName = XMLUtils.getNodeAttributeValue(
                           topIndexEntryNode, 
                           PolicyManager.POLICY_INDEX_REFERENCE_NODE_NAME_ATTR);
                    Set policyNames = getPolicyNames(topIndexEntryNode);
                    ResourceIndexEntry rie = new ResourceIndexEntry(
                            resourceName, policyNames);
                    topLevelEntries.add(rie);
                    Set indexEntryNodeSet = XMLUtils.getChildNodes(
                            topIndexEntryNode, 
                            PolicyManager.POLICY_INDEX_REFERENCE_NODE);
                    Iterator indexEntryNodes = indexEntryNodeSet.iterator();
                    while (indexEntryNodes.hasNext()) {
                        Node indexEntryNode = (Node)indexEntryNodes.next();
                        processIndexEntryNode(rie, indexEntryNode);
                    }
                }
            }
        }

        /**
         * Adds or updates an index entry
         * @param resourceName resource name
         * @param policyName policy name
         * @return  <code>true</code> if an index entry was added or 
         *           updated. <code>false</code> otherwise.
         */
        boolean addIndexEntry (String resourceName, String policyName) {
            /* return value of true indicates whether an index entry was 
             either added or updated*/
            Iterator iter = topLevelEntries.iterator();
            boolean processed = false;
            while (!processed && (iter.hasNext())) {
                ResourceIndexEntry resourceIndexEntry 
                        = (ResourceIndexEntry)iter.next();
                if (resourceIndexEntry.addIndexEntry(resourceType, 
                            resourceName, policyName)) {
                    processed = true;
                }
            }
            if (!processed) {
                // top level entry
                // may have to reparent other top level entries
                ResourceIndexEntry resourceIndexEntry 
                        = new ResourceIndexEntry(resourceName, policyName);
                Set currentEntries = new HashSet();
                currentEntries.addAll(topLevelEntries);
                Iterator iter1 = currentEntries.iterator();
                while (iter1.hasNext()) {
                    ResourceIndexEntry rie = (ResourceIndexEntry)iter1.next();
                    ResourceMatch rm = resourceType.compare(resourceName, 
                            rie.getResourceName(), false);
                    if (rm.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                        rie.setParent(resourceIndexEntry);
                        topLevelEntries.remove(rie);
                    }
                }
                topLevelEntries.add(resourceIndexEntry);
                processed = true;
            }
            return  processed;
        }

        /**
         * Removes an index entry
         * @param resourceName resource name
         * @param policyName policy name
         * @return  <code>true</code> if an index entry was added or 
         *           updated. <code>false</code> otherwise.
         */
        boolean removeIndexEntry (String resourceName, String policyName) {
            Iterator iter = topLevelEntries.iterator();
            boolean processed = false;
            while (!processed && (iter.hasNext())) {
                ResourceIndexEntry resourceIndexEntry 
                        = (ResourceIndexEntry)iter.next();
                if (resourceIndexEntry.removeIndexEntry(resourceType, 
                            resourceName, policyName)) {
                    processed = true;
                }
            }
            return  processed;
        }

        /**
         * Finds the index entry with closest matching resource name 
         * for a given resource name
         * Known problem : if the resourceName happens to be a super resource 
         *         of more than one top level entry, one top level entry is
         *         returned. Which top level entry is returned is indeterminate
         *
         * @param resourceName resource name
         * @return the index entry with closest matching resource name 
         *         for the given resource name
         */
        private ResourceIndexEntry findClosestMatch (String resourceName) {
            ResourceIndexEntry resourceIndexEntry = null;
            Iterator iter = topLevelEntries.iterator();
            boolean processed = false;
            while ((!processed) && (iter.hasNext())) {
                ResourceIndexEntry tle = (ResourceIndexEntry)iter.next();
                resourceIndexEntry = tle.findClosestMatch(resourceType, 
                        resourceName);
                if (resourceIndexEntry != null) {
                    processed = true;
                }
            }
            return  resourceIndexEntry;
        }

        /**
         * Returns the set of policy names applicable to the given resource name
         * and its sub resources treating wild character 
         * in the policy resource  name as literal
         *
         * @param resourceName resource name
         * @return the set of policy names that are applicable to a resource 
         *         and its sub resources
         */
        private Set getSubResourcePolicyNames (String resourceName) {
            Set policyNames = new HashSet();
            Iterator iter = topLevelEntries.iterator();
            while ( iter.hasNext() ) {
                ResourceIndexEntry tle = (ResourceIndexEntry)iter.next();
                ResourceIndexEntry resourceIndexEntry 
                        = tle.findClosestMatch(resourceType, resourceName);
                if (resourceIndexEntry != null) {
                    policyNames.addAll(
                            resourceIndexEntry.getSubResourcePolicyNames());
                }
            }
            return  policyNames;
        }

        /**
         * Returns the set of policy names applicable to the given resource name
         * and its sub resources treating wild character 
         * in the policy resource  name as wild
         *
         * @param resourceType resource type 
         * @param resourceName resource name
         * @return the set of policy names that are applicable to a resource 
         *         and its sub resources
         */
        private Set getWildSubResourcePolicyNames (ServiceType resourceType,
                String resourceName) {
            Set policyNames = new HashSet();
            Iterator iter = topLevelEntries.iterator();
            while ( iter.hasNext() ) {
                ResourceIndexEntry tle = (ResourceIndexEntry)iter.next();
                policyNames.addAll(
                        tle.getWildSubResourcePolicyNames(resourceType, 
                        resourceName));
            }
            return  policyNames;
        }

        /**
         * Converts a dom node to index entry and adds it to 
         * a parent index entry
         *
         * @param rie parent index entry
         * @param indexNode dom node that needs to be converted to 
         *        an index entry added to the parent index entry
         */
        private void processIndexEntryNode (ResourceIndexEntry rie, 
                Node indexNode) {
            String resourceName 
                    = XMLUtils.getNodeAttributeValue(indexNode, 
                    PolicyManager.POLICY_INDEX_REFERENCE_NODE_NAME_ATTR);
            Set policyNames = getPolicyNames(indexNode);
            ResourceIndexEntry ie 
                    = new ResourceIndexEntry(resourceName, policyNames);
                    //= new ResourceIndexEntry(resourceType.append(
                    //        rie.resourceName, resourceName), policyNames);
            rie.childEntries.add(ie);
            ie.parent = rie;
            Set indexEntryNodeSet = XMLUtils.getChildNodes(indexNode, 
                    PolicyManager.POLICY_INDEX_REFERENCE_NODE);
            Iterator indexEntryNodes = indexEntryNodeSet.iterator();
            while (indexEntryNodes.hasNext()) {
                Node indexEntryNode = (Node)indexEntryNodes.next();
                processIndexEntryNode(ie, indexEntryNode);
            }
        }

        /**
         * Returns a set of policy names that are referenced in a dom 
         * reference node
         *
         * @param referenceNode dom reference node
         * @return  set of policy names
         */
        private Set getPolicyNames (Node referenceNode) {
            Set policyNameNodes = XMLUtils.getChildNodes(referenceNode, 
                    PolicyManager.POLICY_INDEX_POLICYNAME_NODE);
            Iterator items = policyNameNodes.iterator();
            Node policyNameNode = null;
            String policyName = null;
            Set retVal = new HashSet();
            while (items.hasNext()) {
                policyNameNode = (Node)items.next();
                policyName = XMLUtils.getNodeAttributeValue(policyNameNode, 
                        PolicyManager.POLICY_INDEX_POLICYNAME_NODE_NAME_ATTR);
                retVal.add(policyName);
            }
            return  retVal;
        }   

        /** Returns the closest match and its children or all descendents of 
         *  the resource name with  the given resource type.
         *
         *  @param resourceName resource name
         *  @param followChild if <code>true</code> gets all the descendents,
         *         includindg direct children. Else, gets only direct children.
         *  @return set of names of child resources or all descendent resources
         */
        Set getChildResourceNames( String resourceName, boolean followChild) {
            Set resourceNames = null;
            ResourceIndexEntry rie = findClosestMatch(resourceName);
            if ( rie != null ) {
                resourceNames 
                        = rie.getChildResourceNames(followChild);
            } else {
                resourceNames = Collections.EMPTY_SET;
            }
            return  resourceNames;
        }

    /**
     * Returns the set of top level resource names
     *
     * @return  the set of top level resource names
     */
        Set getTopLevelResourceNames() {
            Set tlr = new HashSet();
            Iterator iter = topLevelEntries.iterator();
            while (iter.hasNext()) {
                ResourceIndexEntry resourceIndexEntry 
                        = (ResourceIndexEntry)iter.next();
                tlr.add(resourceIndexEntry.getResourceName());
            }
            return tlr;
        }

        /** Returns the XML representation of this resource index
         */
         String toXML() {
             StringBuilder sb = new StringBuilder(256);
             sb.append(ResourceIndexManager.LTS)
                    .append(ResourceIndexManager.POLICY_CROSS_REFERENCES)
                    .append(ResourceIndexManager.SPACE)
                    .append(ResourceIndexManager.NAME)
                    .append(ResourceIndexManager.EQUALS)
                    .append(ResourceIndexManager.QUOTE)
                    .append(XMLUtils.escapeSpecialCharacters(
                            resourceType.getName()))
                    .append(ResourceIndexManager.QUOTE)
                    .append(ResourceIndexManager.SPACE)
                    .append(ResourceIndexManager.TYPE)
                    .append(ResourceIndexManager.EQUALS)
                    .append(ResourceIndexManager.QUOTE)
                    .append(ResourceIndexManager.RESOURCES)
                    .append(ResourceIndexManager.QUOTE)
                    .append(ResourceIndexManager.GTS)
                    .append(ResourceIndexManager.NEW_LINE);
             Iterator iter = topLevelEntries.iterator();
             while ( iter.hasNext() ) {
                 ResourceIndexEntry rie = (ResourceIndexEntry) iter.next();
                 sb.append(rie.toXML());
             }
             sb.append(ResourceIndexManager.LTSS)
                    .append(ResourceIndexManager.POLICY_CROSS_REFERENCES)
                    .append(GTS)
                    .append(ResourceIndexManager.NEW_LINE);
             return sb.toString();
         }

    }

    /** class to represent each entry in the resource index */
    private static class ResourceIndexEntry {

        private String resourceName;
        private Set policyNames = new HashSet();
        private Set childEntries = new HashSet();
        private ResourceIndexEntry parent;

        /**
         * Constructs a resource index entry
         * @param resourceName resource name
         * @param policyName policy name
         */
        private ResourceIndexEntry (String resourceName, String policyName) {
            this.resourceName = resourceName;
            policyNames.add(policyName);
        }

        /**
         * Constructs a resource index entry
         * @param resourceName resource name
         * @param policyNames set of policy names
         */
       ResourceIndexEntry (String resourceName, Set policyNames) {
            this.resourceName = resourceName;
            if (policyNames != null) {
                this.policyNames = policyNames;
            }
        }

        /**
         * Returns the resource name of this index entry
         * @return resource name of this index entry
         */
        String getResourceName () {
            return  resourceName;
        }

        /**
         * Sets the parent index entry of this index entry
         * @param parent parent index entry of this index entry
         */
        void setParent (ResourceIndexEntry parent) {
            if (this.parent != null) {
                this.parent.childEntries.remove(this);
            }
            this.parent = parent;
            parent.childEntries.add(this);
        }

        /**
         * Returns the parent index entry of this index entry
         */
        ResourceIndexEntry getParent () {
            return  parent;
        }

        /**
         * Adds a new index entry or updates an existing index entry
         * @param resourceType resource type
         * @param resourceName resource name
         * @param policyName policy name
         * @return  <code>true</code> if an index entry was added or 
         *          updated. <code>false</code> otherwise.
         * @throws PolicyException
         */
        boolean addIndexEntry (ServiceType resourceType, String resourceName, 
                String policyName) {
            boolean processed = false;
            ResourceMatch resourceMatch 
                    = resourceType.compare(this.resourceName, resourceName,
                            false);
            if (resourceMatch.equals(ResourceMatch.EXACT_MATCH)) {
                policyNames.add(policyName);
                processed = true;
            } 
            else if (resourceMatch.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                Iterator iter = childEntries.iterator();
                while (!processed && (iter.hasNext())) {
                    ResourceIndexEntry resourceIndexEntry 
                            = (ResourceIndexEntry)iter.next();
                    if (resourceIndexEntry.addIndexEntry(resourceType, 
                                resourceName, policyName)) {
                        processed = true;
                    }
                }
                if (!processed) {
                    // first level child, may have to reparent other first level
                    // children
                    ResourceIndexEntry resourceIndexEntry 
                            = new ResourceIndexEntry(resourceName, policyName);
                    Set children = new HashSet();
                    children.addAll(childEntries);
                    Iterator iter1 = children.iterator();
                    while (iter1.hasNext()) {
                        ResourceIndexEntry rie 
                                = (ResourceIndexEntry)iter1.next();
                        ResourceMatch rm = resourceType.compare(resourceName, 
                                rie.resourceName, false);
                        if (rm.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                            rie.setParent(resourceIndexEntry);
                        }
                    }
                    childEntries.add(resourceIndexEntry);
                    processed = true;
                }
            }
            return  processed;
        }

        /**
         * Removes or updates an index entry 
         * @param resourceType resource type
         * @param resourceName resource name
         * @param policyName policy name
         * @return  <code>true</code> if an index entry was removed or 
         *          updated. <code>false</code> otherwise.
         */
        boolean removeIndexEntry (ServiceType resourceType, 
                String resourceName, String policyName) {
            boolean processed = false;
            ResourceMatch resourceMatch 
                    = resourceType.compare(this.resourceName, resourceName,
                            false);
            if (resourceMatch.equals(ResourceMatch.EXACT_MATCH)) {
                policyNames.remove(policyName);
                processed = true;
                if ( childEntries.isEmpty() && policyNames.isEmpty() 
                            && (parent != null) ) {
                    parent.childEntries.remove(this);
                }
            } 
            else if (resourceMatch.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                Iterator iter = childEntries.iterator();
                while (!processed && (iter.hasNext())) {
                    ResourceIndexEntry resourceIndexEntry 
                            = (ResourceIndexEntry)iter.next();
                    if (resourceIndexEntry.removeIndexEntry(resourceType, 
                                resourceName, policyName)) {
                        processed = true;
                    }
                }
                processed = true;
            }
            return  processed;
        }

         /**
         * Finds the index entry with closest matching resource name 
         * for a given resource name
         *
         * @param resourceType resource type 
         * @param resourceName resource name
         * @return the index entry with closest matching resource name 
         *         for the given resource name
         */
        ResourceIndexEntry findClosestMatch (ServiceType resourceType, 
                String resourceName) {
            ResourceIndexEntry resourceIndexEntry = null;
            ResourceMatch rm = resourceType.compare(resourceName, 
                    this.resourceName, false); //TODO should it be true
            if ( rm.equals(ResourceMatch.EXACT_MATCH)
                        || rm.equals(ResourceMatch.SUB_RESOURCE_MATCH) ) {
                resourceIndexEntry = this;
            } 
            else if (rm.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {
                Iterator iter = childEntries.iterator();
                boolean processed = false;
                while ((!processed) && (iter.hasNext())) {
                    ResourceIndexEntry rie = (ResourceIndexEntry)iter.next();
                    resourceIndexEntry 
                            = rie.findClosestMatch(resourceType, resourceName);
                    if (resourceIndexEntry != null) {
                        processed = true;
                    }
                }
                if (resourceIndexEntry == null) {
                    resourceIndexEntry = this;
                }
            }
            return  resourceIndexEntry;
        }

        /**
         * Returns a set of policy names that are referenced in this
         * index entry and optionally its ancestors
         * @param includeSrPolicies if <code>true</code> names of policies 
         *        applicable to super resources of the resource name 
         *        also are included in the return value
         * @return set of policy names referenced in this index entry
         */
        Set getPolicyNames (boolean includeSrPolicies) {
            Set pNames = new HashSet();
            if (includeSrPolicies) {
                ResourceIndexEntry current = this;
                while (current != null) {
                    pNames.addAll(current.policyNames);
                    current = current.getParent();
                }
            } 
            else {
                pNames.addAll(policyNames);
            }
            return  pNames;
        }

    /**
     * Returns the set of policy names referred in this index entry
     * and its descendent entries
     *
     * @return  set of policy names referred in this index entry
     *          and its descendent entries
     * @throws PolicyException 
     */
        Set getSubResourcePolicyNames () {
            Set pNames = new HashSet();
            pNames.addAll(policyNames);
            Iterator children = childEntries.iterator();
            while ( children.hasNext() ) {
                ResourceIndexEntry child = (ResourceIndexEntry) children.next();
                pNames.addAll(child.getSubResourcePolicyNames());
            } 
            return  pNames;
        }

        /**
         * Returns the set of policy names that have resources matching the 
         * argument resource name as exact match, wild card match, or sub 
         * resource match. Wild character in policy resource name is 
         * treated as wild
         *
         * @param resourceType resource type 
         * @param resourceName resource name
         * @return  set of policy names referred in this index entry
         *          and its descendent entries
         * @throws PolicyException 
         */
        Set getWildSubResourcePolicyNames(ServiceType resourceType, 
                String resourceName) {
            Set pNames = new HashSet();
            ResourceMatch rm = resourceType.compare(resourceName, 
                    this.resourceName, true); //interpret wild card
            if (rm != ResourceMatch.NO_MATCH) {
                pNames.addAll(getSubResourcePolicyNames());
            }

            Set mismatches = new HashSet();
            Iterator iter = pNames.iterator();
            while (iter.hasNext()) {
                String candidateResource = (String)iter.next();
                rm = resourceType.compare(resourceName, 
                        this.resourceName, true); //interpret wild card
                if (rm == ResourceMatch.NO_MATCH) {
                    mismatches.add(candidateResource);
                }
            }
            pNames.removeAll(mismatches);

            return  pNames;
        }

        /**
         * Returns a set of policy names that are referenced in this
         * index entry and its descendents and applicable to the 
         * given resource name
         *
         * @param resourceName resource name
         * @param includeSrPolicies if <code>true</code> names of policies 
         *            applicable to super resources of the resource name 
         *            also are included in the return value
         * @param resourceType resource type
         *
         * @return set of policy names referenced in this index entry and
         *         its descendents and applicable to the given
         *         resource name
         */
        Set getPolicyNames (String resourceName, boolean includeSrPolicies, 
                ServiceType resourceType) {
            Set pNames = new HashSet();
            ResourceMatch rm = resourceType.compare(resourceName, 
                    this.resourceName);
            if ( rm.equals(ResourceMatch.EXACT_MATCH) 
                    || rm.equals(ResourceMatch.WILDCARD_MATCH) ) {
                pNames.addAll(this.policyNames);
                Iterator iter = childEntries.iterator();
                while (iter.hasNext()) {
                    ResourceIndexEntry rie = (ResourceIndexEntry)iter.next();
                    pNames.addAll(rie.getPolicyNames(resourceName, 
                            includeSrPolicies, resourceType));
                }
            } else if( rm.equals(ResourceMatch.SUPER_RESOURCE_MATCH) ) {
                if ( includeSrPolicies ) {
                    pNames.addAll(this.policyNames);
                }
                Iterator iter = childEntries.iterator();
                while (iter.hasNext()) {
                    ResourceIndexEntry rie = (ResourceIndexEntry)iter.next();
                    pNames.addAll(rie.getPolicyNames(resourceName, 
                            includeSrPolicies, resourceType));
                }
            }
            if ( DEBUG.messageEnabled() ) {
                DEBUG.message("ResourceIndexEntry.getPolicyNames "
                        + "- resourceName, this.resourceName, resourceMach, "
                        + " policyNames=" 
                        + resourceName + ":" + this.resourceName 
                        + ":" + rm + ":" + policyNames);
            }
            return  pNames;
        }

        /**
         * Returns a string representation of this index entry
         * @return  a string representation of this index entry
         */
        public String toString () {
            StringBuilder sb = new StringBuilder(120);
            sb.append("ResourceName:").append(resourceName).append(";");
            sb.append("PolicyNames:").append(policyNames.toString());
            return  sb.toString();
        }

        /** 
         * Returns the resource names of this index entry and its child entries.
         *
         *  @param followChild if <code>true</code> gets resource names of
         *         all the descendents includindg direct children and self.
         *         Else, gets resource names of only self and direct children.
         *  @return set of names of child resources or all descendent resources
         */
        Set getChildResourceNames(boolean followChild) {
            Set resourceNames = new HashSet();
            resourceNames.add(getResourceName());
            if ( followChild ) {
                Iterator children = childEntries.iterator();
                while ( children.hasNext() ) {
                    ResourceIndexEntry indexEntry 
                            = (ResourceIndexEntry) children.next();
                    resourceNames.addAll(
                            indexEntry.getChildResourceNames(followChild));
                }
            }
            return  resourceNames;
        }


        /** 
         * Returns XML representation of this index entry
         */
         String toXML() {
             String xmlString = null;
             if ( policyNames.isEmpty() && childEntries.isEmpty() ) {
                 xmlString = "";
             } else {
                 StringBuilder sb = new StringBuilder(256);
                 sb.append(ResourceIndexManager.LTS)
                        .append(ResourceIndexManager.REFERENCE)
                        .append(ResourceIndexManager.SPACE)
                        .append(ResourceIndexManager.NAME)
                        .append(ResourceIndexManager.EQUALS)
                        .append(ResourceIndexManager.QUOTE)
                        .append(XMLUtils.escapeSpecialCharacters(
                                getResourceName()))
                        .append(ResourceIndexManager.QUOTE)
                        .append(ResourceIndexManager.GTS)
                        .append(ResourceIndexManager.NEW_LINE);
                 Iterator iter = policyNames.iterator();
                 while ( iter.hasNext() ) {
                     String policyName = (String) iter.next();
                     sb.append(ResourceIndexManager.LTS)
                            .append(ResourceIndexManager.POLICY_NAME)
                            .append(ResourceIndexManager.SPACE)
                            .append(ResourceIndexManager.NAME)
                            .append(ResourceIndexManager.EQUALS)
                            .append(ResourceIndexManager.QUOTE)
                            .append(XMLUtils.escapeSpecialCharacters(
                                    policyName))
                            .append(ResourceIndexManager.QUOTE)
                            .append(ResourceIndexManager.SGTS)
                            .append(ResourceIndexManager.NEW_LINE);
                 }
                 iter = childEntries.iterator();
                 while ( iter.hasNext() ) {
                     ResourceIndexEntry rie = (ResourceIndexEntry) iter.next();
                     sb.append(rie.toXML());
                 }
                 sb.append(ResourceIndexManager.LTSS)
                        .append(ResourceIndexManager.REFERENCE)
                        .append(ResourceIndexManager.GTS)
                        .append(ResourceIndexManager.NEW_LINE);
                 xmlString =  sb.toString();
             }
             return xmlString;
         }

    }

}



