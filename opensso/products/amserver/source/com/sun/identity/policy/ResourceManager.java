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
 * $Id: ResourceManager.java,v 1.7 2009/06/30 17:46:02 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.*;
import java.io.*;

import org.w3c.dom.*;

import com.sun.identity.policy.interfaces.Referral;
import com.sun.identity.policy.plugins.OrgReferral;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.util.DN;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.*;
import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.am.util.Cache;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;


/**
 * The class <code>ResourceManager</code> manages an index to the  
 * resources managed by policies in a specific organization/realm.
 *
 */
public class ResourceManager {

    private String org = "/";
    private SSOToken token = null;
    private ServiceConfigManager scm = null;
    private boolean canCreateNewRes = false;
    ServiceTypeManager stm = null;
    // resources service config
    private ServiceConfig rConfig = null;
    // key: service type name, value: ServiceType object 
    private Hashtable serviceTypeHash = new Hashtable();

    private static final String RESOURCES_XML = "xmlresources";
    private static final String RESOURCE_PREFIXES = "resourceprefixes";
    static final String EMPTY_RESOURCE_NAME = "---EMPTY---";

    static Debug debug = Debug.getInstance("amPolicy");


    //Constants to build XML representation
    static final String LTS = "<";
    static final String LTSS = "</";
    static final String GTS = ">";
    static final String SGTS = "/>";
    static final String SPACE = " ";
    static final String QUOTE = "\"";
    static final String EQUALS = "=";
    static final String NEW_LINE = "\n";
    static final String RESOURCE_PREFIXES_XML = "resourcePrefixesXml";
    static final String ATTRIBUTE_VALUE_PAIR = "AttributValuePair";
    static final String PREFIX = "Prefix";
    static final String NAME = "name";
    static final String COUNT = "count";

    // Cache to store the policy names
    Cache policyNames = new Cache(1000);

    /**
     * this constructor is called by PolicyManager
     */
    ResourceManager(String orgName)
        throws SSOException, SMSException {
            
        this.token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        org = orgName;
        this.scm = new ServiceConfigManager(PolicyManager.POLICY_SERVICE_NAME,
            token);
        DN orgDN = new DN(org);
        DN baseDN = new DN(ServiceManager.getBaseDN());
        stm = ServiceTypeManager.getServiceTypeManager();
        canCreateNewRes = orgDN.equals(baseDN);
    }

    /**
     * Returns a set of all managed resource names for all the
     * service types
     * 
     * @return names of the resources managed
     *
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     */
    public Set getManagedResourceNames()throws PolicyException {
        Set managedResources = null;
        ServiceConfig resources = getResourcesServiceConfig(false);
        if (resources == null) {
            managedResources = Collections.EMPTY_SET;
        } else {
            Set resourceTypes = null;
            try {
                resourceTypes = resources.getSubConfigNames();
            } catch (SMSException e1) {
                throw new PolicyException(e1);
            }
            if ( (resourceTypes == null) || (resourceTypes.isEmpty()) ) {
                managedResources = Collections.EMPTY_SET;
            } else {
                managedResources = new HashSet();
                Iterator rtIter = resourceTypes.iterator();
                while ( rtIter.hasNext() ) {
                    String resourceType = (String) rtIter.next();
                    managedResources.addAll(
                            getManagedResourceNames(resourceType));
                }
            }
        }
        return managedResources;
    }   
    
        
    /**
     * Returns a set of all managed resource names for the given 
     * service type.
     * 
     * @param serviceType the service type for which the resource
     * names should be returned.
     *
     * @return names of the resources.
     *
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     */
    public Set getManagedResourceNames(String serviceType)
        throws PolicyException {

        ServiceConfig resources = getResourcesServiceConfig(false);
        if (resources == null) {
            return Collections.EMPTY_SET;
        }

        ServiceConfig leafConfig = null;
        try {
            leafConfig = resources.getSubConfig(serviceType);
        } catch (SMSException e1) {
            throw new PolicyException(e1);
        } catch (SSOException e1) {
            throw (new PolicyException(ResBundleUtils.rbName,
                    "invalid_sso_token", null, null));
        }   

        if (leafConfig == null) {
            // no resource node for this service type
            return Collections.EMPTY_SET;
        }

        // else, see if the attribute is there and non-empty
        Map existingAttrs = null;
        existingAttrs = leafConfig.getAttributes();

        if ((existingAttrs == null) ||
            (!existingAttrs.containsKey(RESOURCE_PREFIXES))) {
            return Collections.EMPTY_SET;
        }

        // else, need to look into the attribute

        Set existingRes = (Set) existingAttrs.get(RESOURCE_PREFIXES);
        Set resourcePrefixes = existingRes;
        if ( (existingRes != null) && (!existingRes.isEmpty()) ) {
            String xmlPrefix = (String) (existingRes.iterator().next());
            resourcePrefixes 
                    = xmlToResourcePrefixes(xmlPrefix).keySet();
        }
        return resourcePrefixes;
    }
    
    /**
     * Determines that with the given organization (or, sub-organization, 
     * or container) name, if a new resource can be created or not.
     * Only root level organization/realm has the privilege to create 
     * any resource. 
     *
     * @param ServiceType the service type
     * 
     * @return  <code>true</code> if new resources can be created,
     * else <code>false</code>
     *
     * @exception PolicyException problem with configuration store
     */
    public boolean canCreateNewResource(String ServiceType)
        throws PolicyException {
            return canCreateNewRes;
    }

    /**
     * Returns a set of valid service names that are applicable for
     * the organization. The result will depended if new resources
     * can be created for the organization and also if the organization
     * has managed resources.
     * 
     * @return set of service names that are valid for the organization
     *
     * @exception SSOException if the caller's single sign on token has expired
     * @exception PolicyException if not able to get list of services
     * defined for the organization
     */
    public Set getValidServiceNames() throws SSOException, PolicyException {
        Set answer = null;
        Iterator serviceNames = stm.getServiceTypeNames().iterator();
        while (serviceNames.hasNext()) {
            String serviceName = (String) serviceNames.next();
            if (canCreateNewResource(serviceName) ||
                    !getManagedResourceNames(serviceName).isEmpty()) {
                if (answer == null) {
                    answer = new HashSet();
                }
                answer.add(serviceName);
            }
        }
        return ((answer == null) ? Collections.EMPTY_SET : answer);
    }

    /**
     * Returns a set of names of all the policies for the given resource
     * of the given service.
     *
     * @param serviceType the service type which the resource is associated
     * with
     * @param resource the resource for which policies should be returned
     * @param includePoliciesForSuperResources indicating whether the
     * policies for all the super-resources in addition to the ultimate
     * (sub)resource should be returned
     *
     * @return set of names of the policies.
     *
     * @exception InvalidFormatException the retrieved resources
     * from the data store have been corrupted or do not have a
     * valid format.
     * @exception NoPermissionException the user does not have sufficient
     * privileges.
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     * @exception SSOException single-sign-on token invalid or expired
     */
    public Set getPolicyNames(String serviceType,
                              String resource,
                              boolean includePoliciesForSuperResources)
        throws InvalidFormatException, NoPermissionException,
                                  PolicyException, SSOException {

        // %%% Need to flush the cache when policy's are changed
        StringBuilder cacheNameBuffer = new StringBuilder();
        String cacheName = cacheNameBuffer.append(serviceType)
                .append(resource)
                .append(includePoliciesForSuperResources).toString();
        Set answer = null;
        if ((answer = (Set) policyNames.get(cacheName)) != null) {
            return (answer);
        }

        // This line may impact performance, try to optimize it later
        Node rootNode = getXMLRootNode(serviceType);
        if (rootNode == null) {
            return Collections.EMPTY_SET;
        }

        ServiceType st = getServiceType(serviceType);

        answer = getPolicyNames(rootNode, null, st, resource,
                              includePoliciesForSuperResources);

        // add it to the cache
        policyNames.put(cacheName, answer);
        return (answer);
    }


    private ServiceType getServiceType(String serviceType)
        throws SSOException, NameNotFoundException {
            
            ServiceType st = (ServiceType)serviceTypeHash.get(serviceType);
            if (st == null) {
                st = stm.getServiceType(serviceType);
                serviceTypeHash.put(serviceType, st);
            }
            return st;
    }

    /**
     * Adds the resource names of the policy to the resource tree.
     *
     * @param policy the policy to be added
     *
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     * @exception SSOException single-sign-on token invalid or expired
     */
    
    void addPolicyToResourceTree(Policy policy)
        throws PolicyException, SSOException {

        Set ruleNames = policy.getRuleNames();
        Iterator i = ruleNames.iterator();
        
        // iterating through each rule
        String ruleName = null;
        Rule rule = null;
        while (i.hasNext()) {
            ruleName = (String) i.next();
            rule = policy.getRule(ruleName);
            addRuleToResourceTree(policy.getName(), rule);
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
                                        String serviceTypeName 
                                            = rule1.getServiceTypeName();
                                        Set resourceNames = new HashSet();
                                        resourceNames.add(resourceName);
                                        rm.addResourcePrefixes(serviceTypeName,
                                            resourceNames);
                                    } 
                                }    
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Removes the resource names of the policy from the resource tree.
     *
     * @param policy the policy to be removed
     *
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     * @exception SSOException single-sign-on token invalid or expired
     */
    
    void removePolicyFromResourceTree(Policy policy)
        throws PolicyException, SSOException {

        Set ruleNames = policy.getRuleNames();
        Iterator i = ruleNames.iterator();
        
        // iterating through each rule
        String ruleName = null;
        Rule rule = null;
        while (i.hasNext()) {
            ruleName = (String) i.next();
            rule = policy.getRule(ruleName);
            removeRuleFromResourceTree(policy.getName(),
                                       rule.getResourceName(),
                                       rule.getServiceTypeName(),
                                       rule.getServiceType());
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
                                String value = (String) valueIter.next();
                                PolicyManager pm 
                                    = new PolicyManager(token, value);
                                ResourceManager rm = pm.getResourceManager();
                                Iterator ruleIter = 
                                policy.getRuleNames().iterator();    
                                while ( ruleIter.hasNext() ) {
                                    String ruleName1 
                                        = (String) ruleIter.next();
                                    Rule rule1 = policy.getRule(ruleName);
                                    String resourceName = 
                                            rule1.getResourceName();
                                    if ( resourceName != null ) {
                                        String serviceTypeName 
                                            = rule1.getServiceTypeName();
                                        Set resourceNames = new HashSet();
                                        resourceNames.add(resourceName);
                                        rm.removeResourcePrefixes(
                                            serviceTypeName, resourceNames);
                                    }
                                }    
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Replaces resource names of a  policy in the resource tree.
     *
     * @param oldPolicy the policy to be replaced
     * @param newPolicy the policy to replace the existins policy with
     *
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     * @exception SSOException single-sign-on token invalid or expired
     */
    void replacePolicyInResourceTree(Policy oldPolicy, Policy newPolicy)
        throws PolicyException, SSOException {

        removePolicyFromResourceTree(oldPolicy);
        addPolicyToResourceTree(newPolicy);
    }

    private ServiceConfig getResourcesServiceConfig(boolean create)
        throws PolicyException {
        if (rConfig == null) {
            try {
                if (create) {
                    rConfig = PolicyManager.createOrGetPolicyConfig(
                        PolicyManager.RESOURCES_POLICY,
                        PolicyManager.RESOURCES_POLICY, scm, org);
                } else {
                    //rConfig = scm.getOrganizationConfig(org, null);
                    ServiceConfig oConfig = scm.getOrganizationConfig(org,
                        null);
                    rConfig = (oConfig == null) ? null :
                        oConfig.getSubConfig(PolicyManager.RESOURCES_POLICY);
                }
            } catch (SMSException e) {
                throw new PolicyException(e);
            } catch (SSOException e) {
                throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_sso_token", null, null));
            }
        }
        if (!rConfig.isValid()) {
            if (debug.messageEnabled()) {
                debug.message("ResourceManager.getResourcesServiceConfig():"
                        + "rConfig is not valid");
            }
            try {
                scm = new ServiceConfigManager(
                    PolicyManager.POLICY_SERVICE_NAME, token);
                ServiceConfig oConfig = scm.getOrganizationConfig(org, null);
                rConfig = (oConfig == null) ? null :
                oConfig.getSubConfig(PolicyManager.RESOURCES_POLICY);
            } catch (SMSException e) {
                throw new PolicyException(e);
            } catch (SSOException e) {
                throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_sso_token", null, null));
            }
        }
        return rConfig;
    }
    
    private void addRuleToResourceTree(String policyName, Rule rule)
        throws PolicyException, SSOException {
            // to do: investigate this

        String resourceName = rule.getResourceName();
        String serviceTypeName = rule.getServiceTypeName();
        ServiceType st = rule.getServiceType();
            
        if (resourceName == null || resourceName.length() == 0) {
            resourceName = EMPTY_RESOURCE_NAME;
        }
        
        ServiceConfig resources = getResourcesServiceConfig(true);
        if (resources == null) {
            return;
        }

        ServiceConfig leafConfig = null;
        try {
            leafConfig = resources.getSubConfig(serviceTypeName);
        } catch (SMSException e1) {
            throw new PolicyException(e1);
        }   

        if (leafConfig == null) {
            // no resource node for this service type
            try {
                String newResourcesXml = rule.toResourcesXml(policyName);
                Map newAttrs = new HashMap();
                Set newSet = new HashSet();
                newSet.add(newResourcesXml);
                newAttrs.put(RESOURCES_XML, newSet);
        
                resources.addSubConfig(
                    serviceTypeName,
                    PolicyManager.RESOURCES_POLICY_ID,
                    0,
                    newAttrs);
            } catch (SMSException e2) {
                throw new PolicyException(e2);
            }
            return;
        }

        // else, see if the attribute is there and non-empty
        Map existingAttrs = null;
        existingAttrs = leafConfig.getAttributes();

        if ((existingAttrs == null) ||
                (!existingAttrs.containsKey(RESOURCES_XML))) {
            try {
                String newResourcesXml = rule.toResourcesXml(policyName);
                Set newSet = new HashSet();
                newSet.add(newResourcesXml);

                leafConfig.addAttribute(RESOURCES_XML, newSet);
            } catch (SMSException e4) {
                throw new PolicyException(e4);
            }
            return;
        }

        // else, need to look into the attribute
        Set existingRes = (Set) existingAttrs.get(RESOURCES_XML);
        if (existingRes.isEmpty()) {
            try {
                String newResourcesXml = rule.toResourcesXml(policyName);
                Map newAttrs = new HashMap();
                Set newSet = new HashSet();
                newSet.add(newResourcesXml);
                newAttrs.put(RESOURCES_XML, newSet);

                leafConfig.setAttributes(newAttrs);
            } catch (SMSException e5) {
                throw new PolicyException(e5);
            }                    
            return;
        }
        
        // else, the attribute really contains something

        Object[] retVal = getXMLRootNode(existingRes);
        Node rootNode = (Node)retVal[0];
        Document doc = (Document)retVal[1];

        boolean modified =
            matchAndAddReferenceNode(
                doc, rootNode, resourceName, policyName, st);
        
        if (!modified) {
            return;
        }
        
        // finally reset the modified xml content
        String modifiedResourcesXml =
            SMSSchema.nodeToString(rootNode);
        
        Map modifiedAttrs = new HashMap();
        Set modifiedSet = new HashSet();
        modifiedSet.add(modifiedResourcesXml);
        modifiedAttrs.put(RESOURCES_XML, modifiedSet);
        
        try {
            leafConfig.setAttributes(modifiedAttrs);
        } catch (SMSException e6) {
            throw new PolicyException(e6);
        }
    }
    
    private void removeRuleFromResourceTree(
            String policyName, String resourceName,
            String serviceTypeName, ServiceType st)
            throws PolicyException, SSOException {

        if (resourceName == null || resourceName.length() == 0) {
            resourceName = EMPTY_RESOURCE_NAME;
        }
        
        ServiceConfig resources = getResourcesServiceConfig(false);
        if (resources == null) {
            return;
        }

        ServiceConfig leafConfig = null;
        try {
            leafConfig =
                resources.getSubConfig(serviceTypeName);
        } catch (SMSException e1) {
            throw new PolicyException(e1);
        }   

        if (leafConfig == null) {
            // no resource node for this service type
            return;
        }

        // else, see if the attribute is there and non-empty
        Map existingAttrs = null;
        existingAttrs = leafConfig.getAttributes();

        if ((existingAttrs == null) ||
                (!existingAttrs.containsKey(RESOURCES_XML))) {
            return;
        }
        

        // else, need to look into the attribute
        int n = existingAttrs.size();
        Set existingRes = (Set) existingAttrs.get(RESOURCES_XML);
        if (existingRes.isEmpty()) {
            return;
        }
        
        // else, the attribute really contains something

        Object[] retVal = getXMLRootNode(existingRes);
        Node rootNode = (Node)retVal[0];

        boolean modified =
            matchAndRemoveReferenceNode(
                rootNode, resourceName, policyName,
                st, new Stack());
        
        if (!modified) {
            return;
        }

        if (!rootNode.hasChildNodes()) {
            try {
                leafConfig.removeAttribute(RESOURCES_XML);
                if (n == 1) {
                    resources.removeSubConfig(serviceTypeName);
                }
                return;
            } catch (SMSException e3) {
                throw new PolicyException(e3);
            }
        }
        
        // finally reset the modified xml content
        String modifiedResourcesXml =
                SMSSchema.nodeToString(rootNode);
        
        Map modifiedAttrs = new HashMap();
        Set modifiedSet = new HashSet();
        modifiedSet.add(modifiedResourcesXml);
        modifiedAttrs.put(RESOURCES_XML, modifiedSet);
        
        try {
            leafConfig.setAttributes(modifiedAttrs);
        } catch (SMSException e4) {
            throw new PolicyException(e4);
        }
    }

    private boolean matchAndAddReferenceNode(
            Document doc, Node node, String resource,
            String policyName, ServiceType st)
            throws PolicyException {

        boolean modified = true;
        
        Set referenceNodes = XMLUtils.getChildNodes(
            node, PolicyManager.POLICY_INDEX_REFERENCE_NODE);

        if (referenceNodes == null ||
            referenceNodes.isEmpty()) {
            addReferenceNodes(
                doc, node, resource, policyName, st);
            return modified;
        }
        
        Iterator items = referenceNodes.iterator();
        Node referenceNode = null;
        String referenceName = null;
        ResourceMatch matchResult = null;
        boolean hasMatch = false;
        
        // iterating through each reference node
        while (items.hasNext()) {
            referenceNode = (Node) items.next();
            referenceName =
                XMLUtils.getNodeAttributeValue(
                    referenceNode,
                    PolicyManager.POLICY_INDEX_REFERENCE_NODE_NAME_ATTR);
                
            matchResult = st.compare(resource, referenceName);
                
            if (matchResult.equals(ResourceMatch.EXACT_MATCH)) {
                hasMatch = true;
                Set pNames = getPolicyNames(referenceNode);
                if (pNames.contains(policyName)) {
                    modified = false;
                    break;
                }
                // else
                addPolicyNameNode(doc, referenceNode, policyName);
                break;
            }
            if (matchResult.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {
                hasMatch = true;
                String subResource = st.getSubResource(resource, referenceName);
                modified = matchAndAddReferenceNode(doc, referenceNode, 
                        subResource, policyName, st);        
                break;
            }                
        }
        if (!hasMatch) {
            // didn't find any match, need to add (a) reference node(s)
            addReferenceNodes(doc, node, resource, policyName, st);
        }
        return modified;
    }
    
    private boolean matchAndRemoveReferenceNode(
        Node node, String resource,
        String policyName, ServiceType st, Stack stack)
        throws PolicyException {
            
        Set referenceNodes = XMLUtils.getChildNodes(
            node, PolicyManager.POLICY_INDEX_REFERENCE_NODE);

        if (referenceNodes == null || referenceNodes.isEmpty()) {
            return false;
        }
        
        Iterator items = referenceNodes.iterator();
        Node referenceNode = null;
        String referenceName = null;
        ResourceMatch matchResult = null;
        
        // iterating through each reference node
        while (items.hasNext()) {
            referenceNode = (Node) items.next();
            referenceName =
                XMLUtils.getNodeAttributeValue(
                    referenceNode,
                    PolicyManager.POLICY_INDEX_REFERENCE_NODE_NAME_ATTR);
            
            matchResult = st.compare(resource, referenceName);
            
            if (matchResult.equals(ResourceMatch.EXACT_MATCH)) {
                stack.push(node);
                return removePolicyNameNode(referenceNode, policyName,
                                            stack);
            }
            if (matchResult.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {
                String subResource = st.getSubResource(resource, referenceName);
                stack.push(node);
                return matchAndRemoveReferenceNode(
                        referenceNode, subResource,
                        policyName, st, stack);        
            }                
        }
        return false;
    }
    
    private void addPolicyNameNode(Document doc,
                                   Node referenceNode,
                                   String policyName)
        throws PolicyException {

        Element element =
            doc.createElement(
                PolicyManager.POLICY_INDEX_POLICYNAME_NODE);
        element.setAttribute(
            PolicyManager.POLICY_INDEX_POLICYNAME_NODE_NAME_ATTR,
            policyName);
        referenceNode.appendChild(element);
    }
    
    private boolean removePolicyNameNode(Node referenceNode,
                                         String policyName,
                                         Stack stack)
    throws PolicyException {
    
    Set policyNameNodes =
            XMLUtils.getChildNodes(
                referenceNode,
                PolicyManager.POLICY_INDEX_POLICYNAME_NODE);
        Iterator items = policyNameNodes.iterator();
        Node policyNameNode = null;
        String policyNameAttr = null;
        while (items.hasNext()) {
            policyNameNode = (Node) items.next();
            policyNameAttr =
                XMLUtils.getNodeAttributeValue(
                    policyNameNode,
                    PolicyManager.POLICY_INDEX_POLICYNAME_NODE_NAME_ATTR);
            if (policyNameAttr.equals(policyName)) {
                referenceNode.removeChild(policyNameNode);
                removeReferenceNodes(referenceNode, stack);
                return true;
            }   
        }
        return false;
    }
    
    private void addReferenceNodes(Document doc,
                                   Node parentNode,
                                   String resourceName,
                                   String policyName,
                                   ServiceType st)
        throws PolicyException {
            
        String[] resources = st.split(resourceName);

        int n = resources.length;

        if ( n < 1 ) { 
            return;
        }
        
        Element[] nodes = new Element[n];

        Element policyNameNode =
            doc.createElement(
                PolicyManager.POLICY_INDEX_POLICYNAME_NODE);
        policyNameNode.setAttribute(
            PolicyManager.POLICY_INDEX_POLICYNAME_NODE_NAME_ATTR,
            policyName);

        nodes[n-1] =
            doc.createElement(
                PolicyManager.POLICY_INDEX_REFERENCE_NODE);
        nodes[n-1].setAttribute(
            PolicyManager.POLICY_INDEX_REFERENCE_NODE_NAME_ATTR,
            resources[n-1]);
        nodes[n-1].appendChild(policyNameNode);
        
        for (int i=n-2; i>=0; i--) {
            nodes[i] =
                doc.createElement(
                    PolicyManager.POLICY_INDEX_REFERENCE_NODE);
            nodes[i].setAttribute(
                PolicyManager.POLICY_INDEX_REFERENCE_NODE_NAME_ATTR,
                resources[i]);
            nodes[i].appendChild(nodes[i+1]);
        }

        parentNode.appendChild(nodes[0]);
    }

    private void removeReferenceNodes(Node referenceNode,
                                      Stack stack) {
        if (!referenceNode.hasChildNodes() && !stack.empty()) {
            Node parentRefNode = (Node) stack.pop();
            parentRefNode.removeChild(referenceNode);
            removeReferenceNodes(parentRefNode, stack);
        }
    }
    
    /**
     * Returns the xml root node for the service type's resources xml blob
     *
     * @param serviceType the service type which the resources xml blob is
     * associated with
     *
     * @return root node for the resources xml content.
     *
     * @exception InvalidFormatException the retrieved resources
     * from the data store have been corrupted or do not have a
     * valid format.
     * @exception NoPermissionException the user does not have sufficient
     * privileges.
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     */
    Node getXMLRootNode(String serviceType)
        throws InvalidFormatException, NoPermissionException,
            PolicyException {
        
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message(
                "searching for resources of the service type: " +
                serviceType + " in organization: " + org);
        }
        try {
            ServiceConfig policyResources = getResourcesServiceConfig(false);
        
            if (policyResources == null) {
                if (PolicyManager.debug.messageEnabled()) {
                    PolicyManager.debug.message(
                            "Resources branch is non-existent" +
                            " in organization: " + org);
                }
                return null;
            }
            ServiceConfig serviceResources =
                policyResources.getSubConfig(serviceType);
            if (serviceResources == null) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning(
                        serviceType + " branch under Resources is null" +
                        " in organization: " + org);
                }
                return null;
            }
            
            // Obtain the attributes
            Map attrs = serviceResources.getAttributes();
	    Set res = null;
	    if (attrs != null) {
                res = (Set) attrs.get(RESOURCES_XML);
	    }	
            if (res == null) {
	        if (PolicyManager.debug.warningEnabled()){
                    PolicyManager.debug.warning(
                        "Unable to find resources attribute for the service: "+
                        serviceType + " under Resources in organization: "+
                        org);
		}
                return null;
            }
        
            // Get the XML blob
            if (res.isEmpty()) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning(
                        "Unable to find resources attribute value for " +
                        "the service: " + serviceType + " in organization: " +
                        org);
                }
                return null;
            }
            Object[] retVal = getXMLRootNode(res);
            return ((Node)retVal[0]);
            
        } catch (SMSException se) {
            PolicyManager.debug.error(
                    "Unable to get resources of the service type: " +
                    serviceType + " in organization" + org);
            String objs[] = { serviceType, org };
            if (se.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                throw new NoPermissionException(ResBundleUtils.rbName,
                    "unable_to_get_resources_for_service", objs);
            } else {
                throw new PolicyException(se);
            }
        } catch (SSOException ssoe) {
            throw new PolicyException(
               ResBundleUtils.rbName,"invalid_sso_token", null, null);
        }  
    }      

    private Object[] getXMLRootNode(Set xmlBlob)
        throws PolicyException {
        
        Iterator it = xmlBlob.iterator();
        String resourcesXml = (String) it.next();

        Document doc = null;
        try {
            doc = XMLUtils.getXMLDocument(
                    new ByteArrayInputStream(resourcesXml.getBytes("UTF8")));
            
        } catch (Exception xmle) {
            debug.error("XML parsing error for resourcesXml");
            throw (new PolicyException(xmle));
        }
            
        
        Node rootNode = XMLUtils.getRootNode(
            doc, PolicyManager.POLICY_INDEX_ROOT_NODE);
        if (rootNode == null) {
            PolicyManager.debug.error(
                    "invalid (no root node) xml resources blob: " +
                    resourcesXml);
            throw new InvalidFormatException(
                ResBundleUtils.rbName, "invalid_resources_blob_no_root",
                null, "",
                PolicyException.SERVICE);
        }

        /*****
        if (!rootNode.getNodeName().equalsIgnoreCase(
                PolicyManager.POLICY_INDEX_ROOT_NODE))
            throw (new InvalidFormatException());
        ******/
        
        String referenceType = XMLUtils.getNodeAttributeValue(
            rootNode,
            PolicyManager.POLICY_INDEX_ROOT_NODE_TYPE_ATTR);
        
        if (!referenceType.equals(
            PolicyManager.POLICY_INDEX_ROOT_NODE_TYPE_ATTR_RESOURCES_VALUE)) {
            PolicyManager.debug.error(
                    "invalid (no type attr for PolicyCrossReference element) "+
                    "xml resources blob: " + resourcesXml);                
            throw new InvalidFormatException(
                ResBundleUtils.rbName, "invalid_resources_blob_no_type",
                null, "", PolicyException.SERVICE);
        }
        if (PolicyManager.debug.messageEnabled())
            PolicyManager.debug.message("returning XML root node");

        Object[] retVal = new Object[2];
        retVal[0] = rootNode;
        retVal[1] = doc;
        
        return retVal;
    }   

    /**
     * this method recursively finds the names of the policies corresponding
     * to the resource. Depending on the boolean input parameter, it would
     * either returns all the policies including those for super resources,
     * or, just returns the policies at the final level with exact match or
     * the closest match
     */
    private Set getPolicyNames(Node node,
                               String superRes,
                               ServiceType st,
                               String resource,
                               boolean includePoliciesForSuperResources) {

        Set referenceNodes =
            XMLUtils.getChildNodes(
                node,
                PolicyManager.POLICY_INDEX_REFERENCE_NODE);
        Iterator items = referenceNodes.iterator();
        
        Node referenceNode = null;
        String referenceName = null;
        String combinedName = null;
        ResourceMatch matchResult = null;
        
        while (items.hasNext()) {
            referenceNode = (Node) items.next();
            referenceName =
                XMLUtils.getNodeAttributeValue(
                    referenceNode,
                    PolicyManager.POLICY_INDEX_REFERENCE_NODE_NAME_ATTR);

            if (superRes == null) {
                combinedName = referenceName;
            } else {
                combinedName = st.append(superRes, referenceName);
            }
            
            matchResult = st.compare(resource, combinedName);
            
            if (matchResult.equals(ResourceMatch.EXACT_MATCH)) {
                return getPolicyNames(referenceNode);
            }

            if (matchResult.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {

                if (!includePoliciesForSuperResources) {
                     return getPolicyNames(referenceNode, combinedName,
                                           st, resource, false);
                }

                Set policyNamesForTheReferenceNode =
                    getPolicyNames(referenceNode);
                Set policyNamesForChildrenNodes =
                    getPolicyNames(referenceNode, combinedName,
                                   st, resource, true);

                if (policyNamesForChildrenNodes.isEmpty()) {
                    return policyNamesForTheReferenceNode;
                }
                if (policyNamesForTheReferenceNode.isEmpty()) {
                    return policyNamesForChildrenNodes;
                }
                policyNamesForTheReferenceNode.addAll(
                    policyNamesForChildrenNodes);
                return policyNamesForTheReferenceNode;
            }    
        }
        // didn't find exact match, return policies for the last
        // super-resource match
        if ( !includePoliciesForSuperResources && superRes!=null ) {
            return getPolicyNames(referenceNode);
        }
            
        return Collections.EMPTY_SET;
    }

    /**
     * this method finds the names of policies in the first
     * level of the node,
     */
    private Set getPolicyNames(Node referenceNode) {

        if ( referenceNode == null ) {
            return Collections.EMPTY_SET;
        }

        Set policyNameNodes =
            XMLUtils.getChildNodes(
                referenceNode,
                PolicyManager.POLICY_INDEX_POLICYNAME_NODE);
        Iterator items = policyNameNodes.iterator();
        Node policyNameNode = null;
        String policyName = null;
        Set retVal = new HashSet();
        while (items.hasNext()) {
            policyNameNode = (Node) items.next();
            policyName =
                XMLUtils.getNodeAttributeValue(
                    policyNameNode,
                    PolicyManager.POLICY_INDEX_POLICYNAME_NODE_NAME_ATTR);
            retVal.add(policyName);
        }
        return retVal;
    }

    /**
     * Adds specified resource prefixes for a certain service type 
     *
     * @param serviceTypeName the service type name the resource prefixes are
     * associated with
     * @param resourcePrefixes the prefixes to be added
     * 
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     */    
    void addResourcePrefixes(String serviceTypeName, Set resourcePrefixes)
            throws PolicyException {
        ServiceConfig resources = getResourcesServiceConfig(true);
        if (resources == null) {
            return;
        }

        ServiceConfig leafConfig = null;
        try {
            leafConfig = resources.getSubConfig(serviceTypeName);
        } catch (SMSException e1) {
            throw new PolicyException(e1);
        } catch (SSOException e1) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_sso_token", null, null));
        }   

        if (leafConfig == null) {
            // no resource node for this service type
            try {
                Map newAttrs = new HashMap();
                Set newSet = new HashSet();

                Map prefixMap 
                        = addResourcePrefixes(resourcePrefixes, new HashMap());
                newSet.clear();
                newSet.add(resourcePrefixesToXml(prefixMap));
                //newSet.addAll(resourcePrefixes);
                newAttrs.put(RESOURCE_PREFIXES, newSet);
                resources.addSubConfig(
                    serviceTypeName,
                    PolicyManager.RESOURCES_POLICY_ID,
                    0,
                    newAttrs);
            } catch (SMSException e2) {
                throw new PolicyException(e2);
            } catch (SSOException e) {
                throw (new PolicyException(ResBundleUtils.rbName,
                    "invalid_sso_token", null, null));
            }
            return;
        }

        // else, see if the attribute is there and non-empty
        Map existingAttrs = null;
        existingAttrs = leafConfig.getAttributes();

        if ((existingAttrs == null) ||
            (!existingAttrs.containsKey(RESOURCE_PREFIXES))) {
            try {
                Set newSet = new HashSet();
                Map prefixMap 
                        = addResourcePrefixes(resourcePrefixes, new HashMap());
                newSet.clear();
                newSet.add(resourcePrefixesToXml(prefixMap));
                //newSet.addAll(resourcePrefixes);
                leafConfig.addAttribute(RESOURCE_PREFIXES, newSet);
            } catch (SMSException e4) {
                throw new PolicyException(e4);
            } catch (SSOException e) {
                throw (new PolicyException(ResBundleUtils.rbName,
                    "invalid_sso_token", null, null));
            }
            return;
        }

        // else, need to look into the attribute
        Set existingRes = (Set) existingAttrs.get(RESOURCE_PREFIXES);
        try {
            Map newAttrs = new HashMap();
            //existingRes.addAll(resourcePrefixes);

            Map prefixMap = null;
            if ( (existingRes != null) && (!existingRes.isEmpty()) ) {
                String prefixXml = (String) (existingRes.iterator().next());
                prefixMap = xmlToResourcePrefixes(prefixXml);
            } else {
                prefixMap = new HashMap();
            }
            prefixMap = addResourcePrefixes(resourcePrefixes, prefixMap);
            Set newSet = new HashSet(1);
            newSet.add(resourcePrefixesToXml(prefixMap));
            newAttrs.put(RESOURCE_PREFIXES, newSet);
            //newAttrs.put(RESOURCE_PREFIXES, existingRes);
            leafConfig.setAttributes(newAttrs);
        } catch (SMSException e5) {
            throw new PolicyException(e5);
        } catch (SSOException e) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_sso_token", null, null));
        }                    
    }
    
    /**
     * Removed specified resource prefixes for a certain service type 
     *
     * @param serviceTypeName the service type name the resource prefixes are
     * associated with
     * @param resourcePrefixes the prefixes to be removed
     * 
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     */
    void removeResourcePrefixes(String serviceTypeName, Set resourcePrefixes)
            throws PolicyException {
        ServiceConfig resources = getResourcesServiceConfig(false);
        if (resources == null) {
            return;
        }

        ServiceConfig leafConfig = null;
        try {
            leafConfig = resources.getSubConfig(serviceTypeName);
        } catch (SMSException e1) {
            throw new PolicyException(e1);
        } catch (SSOException e) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_sso_token", null, null));
        }   

        if (leafConfig == null) {
            // no resource node for this service type
            return;
        }

        // else, see if the attribute is there and non-empty
        Map existingAttrs = null;
        existingAttrs = leafConfig.getAttributes();

        if ((existingAttrs == null) ||
            (!existingAttrs.containsKey(RESOURCE_PREFIXES))) {
            return;
        }

        // else, need to look into the attribute
        int n = existingAttrs.size();
        Set existingSet = (Set) existingAttrs.get(RESOURCE_PREFIXES);

        Map prefixMap = null;
        if ( (existingSet != null) && (!existingSet.isEmpty()) ) {
            String prefixXml = (String) (existingSet.iterator().next());
            prefixMap = xmlToResourcePrefixes(prefixXml);
        } else {
            prefixMap = new HashMap();
        }
        Map newAttrs = new HashMap();
        prefixMap = removeResourcePrefixes(resourcePrefixes, prefixMap);
        Set newSet = new HashSet(1);
        newSet.add(resourcePrefixesToXml(prefixMap));
        newAttrs.put(RESOURCE_PREFIXES, newSet);

        try {
            /*
            existingSet.removeAll(resourcePrefixes);
            if (existingSet.isEmpty()) {
                leafConfig.removeAttribute(RESOURCE_PREFIXES);
                if (n == 1)
                    resources.removeSubConfig(serviceTypeName);
            } else {
                newAttrs.put(RESOURCE_PREFIXES, existingSet);
                leafConfig.setAttributes(newAttrs);
            }
            */

            leafConfig.setAttributes(newAttrs);
        } catch (SMSException e5) {
            throw new PolicyException(e5);
        } catch (SSOException e) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_sso_token", null, null));
        }                    
    }
    
    /**
     * Returns the resource prefix (super-resource) and the rest of the
     * resource name (sub-resource)
     *
     * @param serviceTypeName the service type which the resource is
     * associated with
     * @param resourceName the resource name to be split
     *
     * @return array of two strings, the first being the super-resource
     * the second being the sub-resource
     *
     * @exception PolicyException if unable to get the policy services,
     * and will contain the exception thrown by SMS.
     * @exception NameNotFoundException service for the given <code>
     * serviceTypeName</code> does not exist
     * @exception SSOException single-sign-on token invalid or expired
     */
    public String[] splitResourceName(
        String serviceTypeName, String resourceName
    ) throws NameNotFoundException, SSOException, PolicyException {

        ServiceType st = getServiceType(serviceTypeName);

        Set prefixes = getManagedResourceNames(serviceTypeName);

        String[] retVal = new String[2];
        
        if (prefixes.isEmpty()) {
            retVal[0] = "";
            retVal[1] = resourceName;
            return retVal;
        }
        Iterator iter = prefixes.iterator();
        String tmp =  null;
        ResourceMatch matchResult = null;

        boolean foundSuperMatch = false;
        boolean foundExactMatch = false;
        
        while (iter.hasNext()) {
            tmp = (String) iter.next();
            matchResult = st.compare(resourceName, tmp);
            if (matchResult.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {
                foundSuperMatch = true;
                break;
            }
            if (matchResult.equals(ResourceMatch.EXACT_MATCH)) {
                foundExactMatch = true;
                break;
            }
        }

        if (foundSuperMatch) {
            retVal[0] = tmp;
            retVal[1] = st.getSubResource(resourceName, tmp);
            return retVal;
        }
        
        if (foundExactMatch) {
            retVal[0] = tmp;
            retVal[1] = "";
            return retVal;
        }

        retVal[0] = "";
        retVal[1] = resourceName;
        return retVal;
    }
    
    /**
     * Saves the resource index to data store
     * @param resourceType resource type
     * @param indexXML xml representation of index ( index to 
     *        policies keyed by resource name, in a tree structure)
     * @throws PolicyException
     * @throws SSOException
     */
    void saveResourceIndex(String resourceType, String indexXML) 
            throws PolicyException ,SSOException {
        Map newAttrs = new HashMap();
        Set newSet = new HashSet();
        newSet.add(indexXML);
        newAttrs.put(RESOURCES_XML, newSet);
        ServiceConfig resources = getResourcesServiceConfig(true);
        if (resources != null) {
            ServiceConfig leafConfig = null;
            try {
                leafConfig = resources.getSubConfig(resourceType);
                if (leafConfig == null) {
                // no resource node for this service type
                resources.addSubConfig(
                        resourceType,
                        PolicyManager.RESOURCES_POLICY_ID,
                        0,
                        newAttrs);
                } else {
                    leafConfig.setAttributes(newAttrs);
                } 
            } catch (SMSException e1) {
                throw new PolicyException(e1);
            }   
        }
    }

    /**
     * Converts xml representation of resource prefixes
     * to a map representation
     *         Key in the map is the prefix and the value is 
     *         a count of how many times the prefix has been
     *         effectively added. The count is incremented whenever
     *         the prefix is added and decremented whenever
     *         the prefix is removed. The count is not decremented
     *         below 0.
     * @param xmlResourcePrefixes xml representation of resource
     *        prefixes.  This is how it is stored in datastore.
     * @return map representation of resource prefixes.
     * 
     */
    private Map xmlToResourcePrefixes(String xmlResourcePrefixes) {
        Map resourcePrefixes = new HashMap();
        try {
            Document document = XMLUtils.getXMLDocument(
                new ByteArrayInputStream(xmlResourcePrefixes.getBytes("UTF8")));
            if ( (document != null) ) {
                Node rootNode 
                        = XMLUtils.getRootNode(document, RESOURCE_PREFIXES);
                if ( rootNode != null ) {
                    Set nodeSet 
                        = XMLUtils.getChildNodes(rootNode, PREFIX);
                    if ( nodeSet != null ) {
                        Iterator nodes = nodeSet.iterator();
                        while (nodes.hasNext()) {
                            Node node = (Node)nodes.next();
                            String prefix 
                                    = XMLUtils.getNodeAttributeValue(node,
                                    NAME);
                            String count
                                    = XMLUtils.getNodeAttributeValue(node,
                                    COUNT);
                            if ( (prefix != null) && (count != null) ) {
                                resourcePrefixes.put(prefix, count);
                            }
                        }
                    }
                }
            } 
        } catch (Exception xmle) {
            PolicyManager.debug.error("XML parsing error for resource prefixes "
                    + " in organization: " + org);
        }
        return resourcePrefixes;
    }

    /**
     * Converts map representation of resource prefixes
     * to an xml representation
     * @param resourcePrefixes map representation of resource
     *        prefixes
     * @return xml representation of resource prefixes
     * 
     */
    private static String resourcePrefixesToXml(Map resourcePrefixes) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(LTS).append(RESOURCE_PREFIXES).append(GTS)
                .append(NEW_LINE);
        Iterator prefixes = resourcePrefixes.keySet().iterator();
        while ( prefixes.hasNext() ) {
            String prefix = (String) prefixes.next();
            String value = (String) resourcePrefixes.get(prefix);
            sb.append(LTS).append(PREFIX).append(SPACE)
                    .append(NAME).append(EQUALS)
                    .append(QUOTE).append(
                     XMLUtils.escapeSpecialCharacters(prefix))
                    .append(QUOTE).append(SPACE)
                    .append(COUNT).append(EQUALS)
                    .append(QUOTE).append(value).append(QUOTE)
                    .append(SGTS).append(NEW_LINE);
        }
        sb.append(LTSS).append(RESOURCE_PREFIXES).append(GTS)
                .append(NEW_LINE);
        return sb.toString();
    }

    /**
     * Adds a set of resource prefixes to a map of resource prefixes.
     * Adding a prefix increments the the count for the prefix 
     * in map value.
     * @param prefixes a set of resource prefixes to add
     * @param prefixMap a map of resource prefixes to which to
     *         add the prefixes.
     * @return prefixMap modified accounting for the addition 
     *                   of prefixes
     * 
     */
    private Map addResourcePrefixes(Set prefixes, Map prefixMap) {
        Iterator iter = prefixes.iterator();
        while ( iter.hasNext() ) {
            String prefix = (String) iter.next();
            int intValue = 0;
            String count = (String) prefixMap.get(prefix);
            if ( count != null ) {
                try {
                    intValue = Integer.parseInt(count);
                } catch (Exception e) {
                    PolicyManager.debug.error(
                      "ResourceManager.addResourcePrefixes:", e);
                }
            }
            intValue++;
            prefixMap.put(prefix, Integer.toString(intValue));
        }
        return prefixMap;
    }

    /**
     * Removes a set of resource prefixes from a map of resource prefixes.
     * Removing a prefix decrements the count for the prefix in the 
     * map value.  Count value is not decremented below 0.
     * @param prefixes a set of resource prefixes to remove
     * @param prefixMap a map of resource prefixes from which to
     *         remove the prefixes.
     * @return prefixMap modified accounting for the removal 
     *         of prefixes.  
     */
    private Map removeResourcePrefixes(Set prefixes, Map prefixMap) {
        Iterator iter = prefixes.iterator();
        while ( iter.hasNext() ) {
            String prefix = (String) iter.next();
            int intValue = 0;
            String count = (String) prefixMap.get(prefix);
            if ( count != null ) {
                try {
                    intValue = Integer.parseInt(count);
                } catch (Exception e) {
                    PolicyManager.debug.error(
                      "ResourceManager.removeResourcePrefixes:", e);
                }
            }
            intValue--;
            if ( intValue > 0 ) {
                prefixMap.put(prefix, Integer.toString(intValue));
            } else {
                prefixMap.remove(prefix);
            }
        }
        return prefixMap;
    }
}
