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
 * $Id: ResourceResult.java,v 1.5 2009/10/12 17:53:05 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.*;

import org.w3c.dom.*;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.interfaces.ResourceName;


/**
 * Class that encapsulates a tree of resource names, with each node 
 *  having an associated policy decision. 
 * @supported.api
 */
public class ResourceResult {

    /**
     * Constant to indicate subtree level scope for ResourceResult evaluation
     *
     * @supported.api
     */
    public static final String SUBTREE_SCOPE = "subtree";

    /**
     * Constant to indicate strict subtree level scope for 
     * <code>ResourceResult</code> evaluation
     *
     * @supported.api
     */
    public static final String STRICT_SUBTREE_SCOPE = "strict-subtree";

    /**
     * Constant to indicate base (self) level scope for 
     * <code>ResourceResult</code> evaluation
     *
     * @supported.api
     */
    public static final String SELF_SCOPE = "self";

    /**
     * Constant used internally as a place holder for all encompassing root
     * resoure name.  Any resource name is considered to be sub resource of 
     * this resource name.
     */
    static final public String VIRTUAL_ROOT = "-__viRTuAl-rOot--_";

    static final String RESOURCE_RESULT = "ResourceResult";
    static final String RESOURCE_NAME = "name";
    static final String POLICY_DEBUG_NAME = "amPolicy";
    static final Debug DEBUG = Debug.getInstance(POLICY_DEBUG_NAME);

    private String resourceName = null;
    private PolicyDecision policyDecision = null;
    private Set resourceResults = new HashSet();
    private long timeToLive = Long.MAX_VALUE;
    private boolean advicesAreSet = false;
    private String stringForm = null;
    private String xmlForm = null;

    //tracks the envMap used to compute ResourceResult
    private Map envMap = null; 

    /**
     * Used in remote api for result caching
    */
    private boolean stale = false;
  
    /**
     * 
     * No argument constructor
     */
    ResourceResult() {
    }

    /**
     * Constructs a resource result given the resource name and policy decison
     * @param resourceName resource name for this resource result
     * @param policyDecision policy decision associated with the resource name
     */
    public ResourceResult(String resourceName, PolicyDecision 
            policyDecision ) {
        this.resourceName = resourceName;
        setPolicyDecision(policyDecision);
    }

    /**
     * Returns the resource name of this resource result
     * @return resource name of this resource result
     * @supported.api
     */
    public String getResourceName() {
        return resourceName;
    }


    /**
     * Sets the resource name of this resource result
     * @param resourceName resource name for this resource result
     */
    void setResourceName(String resourceName) {
        this.resourceName = resourceName;
        this.stringForm = null;
        this.xmlForm= null;
    }


    /**
     * Returns the policy decision associated with this resource result
     * @return policy decision associated with this resource result
     * @supported.api
     */
    public PolicyDecision getPolicyDecision() {
        return policyDecision;
    }


    /**
     * Sets the policy decision for this resource result
     * @param policyDecision policy decision for this resource result
     */
    public void setPolicyDecision(PolicyDecision policyDecision) {
        this.policyDecision = policyDecision;
        long pdTtl = policyDecision.getTimeToLive();
        if ( pdTtl < timeToLive ) {
            timeToLive = pdTtl;
        }
        advicesAreSet = advicesAreSet || policyDecision.hasAdvices();
        this.stringForm = null;
        this.xmlForm= null;
    }


    /**
     * Returns the child resource results of this resource result
     * @return child resource results of this resource result
     * @supported.api
     */
    public Set getResourceResults() {
        return resourceResults;
    }


    /**
     * Sets the child resource results of this resource result
     * @param resourceResults child resource results of this resource result
     */
    void setResourceResults(Set resourceResults) {
        if (resourceResults == null) {
            this.resourceResults.clear();
        } else {
            this.resourceResults = resourceResults;
            if (policyDecision != null) {
                timeToLive = policyDecision.getTimeToLive();
            }
            Iterator iter = resourceResults.iterator();
            while (iter.hasNext()) {
                ResourceResult rr = (ResourceResult)iter.next();
                long ttl = rr.getTimeToLive();
                if ( ttl < timeToLive) {
                    timeToLive = ttl;
                }
                advicesAreSet = advicesAreSet || rr.hasAdvices();
            }
        }

        this.stringForm = null;
        this.xmlForm= null;
    }

    /**
     * Converts an XML representation of resource result to ResourceResult
     * @param resourceResultNode XML DOM node representing resource result
     * @return <code>ResourceResult</code> object representation of resource 
     * result
     * @throws PolicyException if the conversion fails
     */
    public static ResourceResult parseResourceResult(Node resourceResultNode)
            throws PolicyException {
        ResourceResult resourceResult = new ResourceResult();
        String resourceName = XMLUtils.getNodeAttributeValue(resourceResultNode,
                RESOURCE_NAME);
        if (resourceName == null) {
            DEBUG.error("ResourceResult: missing attribute " + RESOURCE_NAME);
	    Object[] objs = {RESOURCE_NAME};
            throw new PolicyException(ResBundleUtils.rbName,
		"missing_attribute_in_resourceresult", objs, null);
        }
        resourceResult.setResourceName(resourceName);

        Node node = XMLUtils.getChildNode(resourceResultNode, 
                PolicyDecision.POLICY_DECISION);
        if (node == null) {
            DEBUG.error("ResourceResult: missing element " + 
                    PolicyDecision.POLICY_DECISION);
	    Object[] objs = {PolicyDecision.POLICY_DECISION};
            throw new PolicyException(ResBundleUtils.rbName,
		"missing_attribute_in_resourceresult", objs, null);
        } else {
            resourceResult.setPolicyDecision(
                    PolicyDecision.parsePolicyDecision(node)); 
        }
            
        Set nodeSet = XMLUtils.getChildNodes(
                resourceResultNode, RESOURCE_RESULT);
        if (nodeSet != null) {
            Iterator nodes = nodeSet.iterator();
            while (nodes.hasNext()) {
                node = (Node)nodes.next();
                ResourceResult rRes = ResourceResult.parseResourceResult(node);
                resourceResult.resourceResults.add(rRes);
            }
        }

        return resourceResult;
    }


    /**
     * Returns a string representation of this resource result 
     * @return a string representation of this resource result
     * @supported.api
     */
    public String toString() {
        if (stringForm == null) {
            StringBuilder sb = new StringBuilder(200);
            sb.append("Resource Result for resourceName : ")
                    .append(resourceName)
                    .append(PolicyUtils.CRLF)
                    .append("PolicyDecision : ")
                    .append(policyDecision)
                    .append("Nested ResourceResults : ")
                    .append(resourceResults);
            stringForm = sb.toString();
        }
        return stringForm;
    }
     
    /**
     * Returns an XML representation of this resource result 
     * @return an XML representation of this resource result
     * @supported.api
     */
    public String toXML() {
        if (xmlForm == null) {
            StringBuilder xmlsb = new StringBuilder(1000);

            xmlsb.append("<")
                    .append(RESOURCE_RESULT)
                    .append(" ").append(RESOURCE_NAME)
                    .append("=\"")
                    .append(XMLUtils.escapeSpecialCharacters(resourceName)) 
                    /*
                    .append("\" ").append("timeToLive")
                    .append("=\"").append(timeToLive).append("\"") 
                    .append( " ").append("hasAdvices")
                    .append("=\"").append(hasAdvices())
                    */
                    .append("\">")
                    .append(PolicyUtils.CRLF);
            if (policyDecision != null) {
                xmlsb.append(policyDecision.toXML());
            }
            
            Iterator rrIter = resourceResults.iterator();
            while ( rrIter.hasNext() ) {
               ResourceResult rr = (ResourceResult) rrIter.next();
               xmlsb.append(rr.toXML());
            }

            xmlsb.append("</")
                    .append(RESOURCE_RESULT)
                    .append( ">")
                    .append(PolicyUtils.CRLF); 

            xmlForm = xmlsb.toString();                
        }
        return xmlForm;
    }

    /**
     * Adds a resource result to the resource result sub tree rooted at
     * this ResourceResult
     * @param resourceResult resource result to be added
     * @param serviceType service type of the resource result being added
     * @throws PolicyException if the resourceResult could not be added
     */
    public void addResourceResult( ResourceResult resourceResult,
            ServiceType serviceType) throws PolicyException {
	addResourceResult(resourceResult,
	    serviceType.getResourceNameComparator());
    }

    /**
     * Adds a resource result to the resource result sub tree rooted at
     * this ResourceResult
     * @param resourceResult resource result to be added
     * @param resourceComparator resource name comparator
     * @throws PolicyException if the resourceResult could not be added
     */
    public void addResourceResult( ResourceResult resourceResult,
            ResourceName resourceComparator) throws PolicyException {
        if (!this.isSuperResourceResultOf(resourceResult, 
                resourceComparator)) { 
	    String[] objs = {this.resourceName, resourceResult.resourceName};
            throw new PolicyException(ResBundleUtils.rbName,
		    "invalid_sub_resourceresult", objs, null);
        } else {
            Iterator resourceResultIter = resourceResults.iterator();
            boolean directChild = true;
            while (resourceResultIter.hasNext()) {
                ResourceResult rResult = 
                        (ResourceResult) resourceResultIter.next();
                if (rResult.isSuperResourceResultOf(resourceResult,
			resourceComparator)) {
                    rResult.addResourceResult(resourceResult,
			resourceComparator);
                    directChild = false;
                    break;
                }
            }
            if (directChild) {
                Set childrenToBeMoved = new HashSet();
                Iterator rrIter = resourceResults.iterator();
                while (rrIter.hasNext()) {
                    ResourceResult rResult = 
                            (ResourceResult) rrIter.next();
                    if (resourceResult.isSuperResourceResultOf(rResult,
			    resourceComparator)) {
                        childrenToBeMoved.add(rResult);
                    }
                }
                resourceResults.removeAll(childrenToBeMoved);
                resourceResult.resourceResults.addAll(childrenToBeMoved);
                resourceResults.add(resourceResult);
            }
        }
	long rrTtl =  resourceResult.getTimeToLive();
	if ( rrTtl < timeToLive ) {
	    timeToLive = rrTtl;
	}
	advicesAreSet = advicesAreSet || resourceResult.hasAdvices();
        this.stringForm = null;
        this.xmlForm= null;
    }

    /**
     * Marks result as stale
     */
    public void markStale() {
	stale = true;
    }

    /**
     * Determines if result is stale
     *
     * @return true if result is stale
     */
    public boolean isStale() {
	return stale;
    }

    /**
     * Checks if this resource result is a super resource result of
     * the argument resource result
     * @param resourceResult resource result for which we want to check
     *        whether this resource result is a super resource result
     * @param resourceComparator - resource comparator
     * @return <code>true</code> if this resource result is a super 
     *         resource result of resourceResult, else returns
     *         <code>false</code>
     * @throws PolicyException if there is any error while comparing the 
     *         resourceResult
     */
    private boolean isSuperResourceResultOf(ResourceResult resourceResult,
            ResourceName resourceComparator) throws PolicyException {
        boolean isSuperResource = false;
        if (VIRTUAL_ROOT.equals(resourceName)) {
            isSuperResource = true;
        } else if (resourceComparator != null) {
            boolean interpretWildCard = false;
            ResourceMatch resourceMatch =
		resourceComparator.compare(resourceName,
                    resourceResult.resourceName, interpretWildCard); 
            if (resourceMatch.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                isSuperResource = true;
            }
            // Results will contain both incoming URL as well as matched policy URL with decision. 
            // The problem is we don't know which order results will come in.
            // Incoming URL will be missing policyDecision so we need to check resourceResult contains '*' 
            // and if it does, check if resourceResult is parent of incoming URL and use parent's
            // policyDecision for this object.  
            else if (resourceResult.resourceName.indexOf('*') != -1 ) {
            	String resResultResName = resourceResult.resourceName;
            	String substrResultResName = resResultResName.substring(0, resResultResName.indexOf('*'));
            	if (resourceName.startsWith(substrResultResName)) {
            	    //check if policyDecision is null
            	    //if null, then copy policyDecision from parents
            	    if (policyDecision==null || policyDecision.getActionDecisions().isEmpty()) {
            		    policyDecision = resourceResult.policyDecision;
            	    }
            	}
            }
        } else {
            isSuperResource =
                    resourceResult.resourceName.startsWith(resourceName);
        }
        return isSuperResource;
    }

    /**
     * Returns the GMT time in milliseconds since epoch when this object is to
     * be treated as expired. That is the resource result would 
     * likely be different after that time.
     * This is computed as a result of time conditions specified in the Policy
     * definitions. 
     *
     * @return time to live
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
     * Checks wether advices are set in this object
     * @return <code>true</code>, if advices are set, else <code>false</code>
     */
    public boolean hasAdvices() {
        return advicesAreSet;
    }

    /**
     * Sets the environment map that was used while computing the 
     * resource result
     * @param envMap the environment map that was used while computing the 
     * resource result
     */
    void setEnvMap(Map envMap) {
        this.envMap = envMap;
        
    }

    /**
     * Returns the environment map that was used while computing the 
     * resource result
     * @param the environment map that was used while computing the 
     * resource result
     */
    Map getEnvMap() {
        return envMap;
    }

} 

