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
 * $Id: Referrals.java,v 1.3 2008/06/25 05:43:45 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.*;

import org.w3c.dom.*;

import com.iplanet.sso.*;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.identity.policy.plugins.OrgReferral;

/**
 * The class <code>Referrals</code> provides methods to maintain
 * a collection of <code>Referral</code> objects that can be
 * applied to a policy. This class provides methods to add, replace
 * and remove <code>Referral</code> objects from this referrals collection.
 * The <code>Policy</code> object provides methods to set
 * <code>Referrals</code>, which identifies referrals to whom the
 * the policy applies.
 */
public class Referrals implements Cloneable {

    private String name;
    private String description;
    private Map referrals = new HashMap();

    /**
     * Constructor used by the <code>Policy</code> object
     * to get a default instance of the <code>Referrals</code>
     */
    protected Referrals() {
        this((String) null, (String) null);
    }

    /**
     * Constructor used by <code>Policy</code> to obtain
     * an instance of <code>Referrals</code> from the
     * XML document
     *
     * @param pm <code>PolicyManager</code> to initialize the
     * <code>Referrals</code> with
     *
     * @param referralsNode node that repersents the Referrals
     * @throws InvalidFormatException if the node passed in does not
     * conform to expected format
     * 
     * @throws InvalidNameException  if the name specified in the 
     * </code>Node</code> for <code>Referrals</code> is invalid
     *
     * @throws NameNotFoundException need to add the situations that would
     * cause this
     *         
     * @throws PolicyException if can not construct <code>Referrals</code>
     */
    protected Referrals(PolicyManager pm, Node referralsNode)
        throws InvalidFormatException, InvalidNameException,
        NameNotFoundException, PolicyException {
        // Check if the node name is PolicyManager.POLICY_REFERRALS_NODE
        if (!referralsNode.getNodeName().equalsIgnoreCase(
            PolicyManager.POLICY_REFERRALS_NODE)) {
            if (PolicyManager.debug.warningEnabled()) {
                PolicyManager.debug.warning(
                    "invalid referrals xml blob given to construct referrals");
            }
            throw (new InvalidFormatException(ResBundleUtils.rbName,
                "invalid_xml_referrals_root_node", null, "",
                PolicyException.REFERRAL_COLLECTION));
        }

        // Get the referrals name
        if ((name = XMLUtils.getNodeAttributeValue(referralsNode,
            PolicyManager.NAME_ATTRIBUTE)) == null) {
            name = "Referrals:" + ServiceTypeManager.generateRandomName();
        }

        // Get the description
        if ((description = XMLUtils.getNodeAttributeValue(referralsNode,
            PolicyManager.DESCRIPTION_ATTRIBUTE)) == null) {
            description = "";
        }

        // Get ReferralTypeManager
        ReferralTypeManager rtm = pm.getReferralTypeManager();

        // Get individual referrals
        Iterator referralNodes = XMLUtils.getChildNodes(
            referralsNode, PolicyManager.REFERRAL_POLICY).iterator();
        while (referralNodes.hasNext()) {
            Node referralNode = (Node) referralNodes.next();
            String referralType = XMLUtils.getNodeAttributeValue(
                referralNode, PolicyManager.TYPE_ATTRIBUTE);
            if (referralType == null) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning("referral type is null");
                }
                throw (new InvalidFormatException(
                    ResBundleUtils.rbName,
                    "invalid_xml_referrals_root_node", null, "",
                    PolicyException.REFERRAL_COLLECTION));
            }

            // Construct the referral object
            Referral referral = rtm.getReferral(referralType);

            // Get and set the values
            NodeList attrValuePairNodes = referralNode.getChildNodes();
            int numAttrValuePairNodes = attrValuePairNodes.getLength();
            for (int j = 0; j < numAttrValuePairNodes; j++) {
                Node attrValuePairNode = attrValuePairNodes.item(j);
                if (XMLUtils.getNamedChildNode(attrValuePairNode,
                    PolicyManager.ATTR_NODE, PolicyManager.NAME_ATTRIBUTE,
                    REFERRAL_VALUES_ATTR_NAME) != null) {
                    referral.setValues(XMLUtils.getAttributeValuePair(
                	attrValuePairNode));
                }
            }

            // Get the friendly name given to referral
            String referralName = XMLUtils.getNodeAttributeValue(
                referralNode, PolicyManager.NAME_ATTRIBUTE);

            // Add the referral to referrals collection
            addReferral(referralName, referral);
        }
    }

    /**
     * Constructor to obtain an instance of <code>Referrals</code>
     * to hold collection of referrals represented as
     * <code>Referral</code>
     *
     * @param name name for the collection of <code>Referral</code>
     * @param description user friendly description for
     * the collection of <code>Referral</code>  
     */
    public Referrals(String name, String description) {
        this.name = (name == null) ? 
            ("Referrals:" + ServiceTypeManager.generateRandomName()) : name;
        this.description = (description == null) ?
            "" : description;
    }

    /**
     * Returns the name for the collection of referrals
     * represented as <code>Referral</code>
     *
     * @return name of the collection of referrals
     */
    public String getName() {
        return (name);
    }

    /**
     * Returns the description for the collection of referrals
     * represented as <code>Referral</code>
     *
     * @return description for the collection of referrals
     */
    public String getDescription() {
        return (description);
    }

    /**
     * Sets the name for this instance of the
     * <code>Referrals<code> which contains a collection
     * of referrals respresented as <code>Referral</code>.
     *
     * @param name for the collection of referrals
     */
    public void setName(String name) {
        this.name = (name == null) ?
            ("Referrals:" + ServiceTypeManager.generateRandomName()) : name;
    }

    /**
     * Sets the description for this instance of the
     * <code>Referrals<code> which contains a collection
     * of referrals respresented as <code>Referral</code>.
     *
     * @param description description for the collection referrals
     */
    public void setDescription(String description) {
        this.description = (description == null) ?
            "" : description;
    }

    /**
     * Returns the names of <code>Referral</code> objects
     * contained in this object.
     *
     * @return names of <code>Referral</code> contained in
     * this object
     */
    public Set getReferralNames() {
        return (referrals.keySet());
    }

    /**
     * Returns the <code>Referral</code> object associated
     * with the given referral name.
     *
     * @param referralName name of the referral object
     *
     * @return <code>Referral</code> object corresponding to referral name
     *
     * @exception NameNotFoundException if a referral
     * with the given name is not present
     */
    public Referral getReferral(String referralName)
        throws NameNotFoundException {
        Referral answer = (Referral) referrals.get(referralName);
        if (answer == null) {
            String[] objs = { referralName };
            throw (new NameNotFoundException(ResBundleUtils.rbName,
                "name_not_present", objs,
                referralName, PolicyException.REFERRAL_COLLECTION));
        
        }
        return (answer);
    }

    /**
     * Adds a <code>Referral</code> object to the this instance
     * of user collection. Since the name is not provided it
     * will be dynamically assigned such that it is unique within
     * this instance of the user collection. However if a referral 
     * entry with the same name already exists in the user collection
     * <code>NameAlreadyExistsException</code> will be thrown.
     *
     * @param referral instance of the referral object added to this
     * collection
     *
     * @exception NameAlreadyExistsException if a 
     * referral object is present with the same name 
     */
    public void addReferral(Referral referral)
        throws NameAlreadyExistsException {
        addReferral(null, referral);
    }

    /**
     * Adds a <code>Referral</code> object to the this instance
     * of user collection. If another referral with the same name
     * already exists in the user collection
     * <code>NameAlreadyExistsException</code> will be thrown.
     *
     * @param referralName name for the referral instance
     * @param referral instance of the referral object added to this
     * collection
     *
     * @exception NameAlreadyExistsException if a 
     * referral object is present with the same name 
     */
    public void addReferral(String referralName, Referral referral)
        throws NameAlreadyExistsException {
        if (referralName == null) {
            referralName = "Referral:" +
                ServiceTypeManager.generateRandomName();
        }
        if (referrals.containsKey(referralName)) {
            String[] objs = { referralName };
            throw (new NameAlreadyExistsException(ResBundleUtils.rbName,
                "name_already_present", objs,
                referralName, PolicyException.REFERRAL_COLLECTION));
        }
        referrals.put(referralName, referral);
    }

    /**
     * Replaces an existing referral object having the same name
     * with the new one. If a <code>Referral</code> with the given
     * name does not exist, <code>NameNotFoundException</code>
     * will be thrown.
     *
     * @param referralName name for the referral instance
     * @param referral instance of the referral object that will
     * replace another referral object having the given name
     *
     * @exception NameNotFoundException if a referral instance
     * with the given name is not present
     */
    public void replaceReferral(String referralName, Referral referral)
        throws NameNotFoundException {
        if (!referrals.containsKey(referralName)) {
            String[] objs = { referralName };
            throw (new NameNotFoundException(ResBundleUtils.rbName,
                "name_not_present", objs,
                referralName, PolicyException.REFERRAL_COLLECTION));
        }
        referrals.put(referralName, referral);
    }

    /**
     * Removes the <code>Referral</code> object identified by
     * the referral name. If a referral instance with the given
     * name does not exist, the method will return silently.
     *
     * @param referralName name of the referral instance that
     * will be removed from the user collection
     * @return the referral that was just removed
     */
    public Referral removeReferral(String referralName) {
        return (Referral)referrals.remove(referralName);
    }
 
    /**
     * Removes the <code>Referral</code> object identified by
     * object's <code>equals</code> method. If a referral instance
     * does not exist, the method will return silently.
     *
     * @param referral referral object that
     * will be removed from the user collection
     * @return the referral that was just removed
     */
    public Referral removeReferral(Referral referral) {
        String referralName = getReferralName(referral);
        if (referralName != null) {
            return removeReferral(referralName);
        }
        return null;
    }

    /**
     * Returns the name associated with the given referral object.
     * It uses the <code>equals</code> method on the referral
     * to determine equality. If a referral instance that matches
     * the given referral object is not present, the method
     * returns <code>null</code>.
     *
     * @param referral referral object for which this method will
     * return its associated name
     *
     * @return user friendly name given to the referral object;
     * <code>null</code> if not present
     */
    public String getReferralName(Referral referral) {
        String answer = null;
        Iterator items = referrals.keySet().iterator();
        while (items.hasNext()) {
            String referralName = (String) items.next();
            if (referral.equals(referrals.get(referralName))) {
                answer = referralName;
                break;
            }
        }
        return (answer);
    }

    /**
     * Checks if two <code>Referrals</code> are identical.
     * Two referrals (or user collections) are identical only
     * if both have the same set of <code>Referral</code> objects.
     *
     * @param o object againt which this referrals object
     * will be checked for equality
     *
     * @return <code>true</code> if all the referrals match;
     * <code>false</code> otherwise
     */
    public boolean equals(Object o) {
        Iterator iter = null;
        if (o instanceof Referrals) {
            Referrals s = (Referrals) o;
            iter = referrals.entrySet().iterator();
            while (iter.hasNext()) {
                Object ss = ((Map.Entry) iter.next()).getValue();
                if (!s.referrals.containsValue(ss)) {
                    return (false);
                }
            }
            return (true);
        }
        return (false);
    }

    /**
     * Returns a new copy of this object with the identical
     * set of user collections (referrals).
     *
     * @return a copy of this object with identical values
     */
    public Object clone() {
        Referrals answer = null;
        try{
            answer = (Referrals) super.clone();
        }catch (CloneNotSupportedException cnse) {
            PolicyManager.debug.error("Referrals: clone failed", cnse); 
        }
        answer.name = name;
        answer.description = description;
        answer.referrals = new HashMap();
        Iterator items = referrals.keySet().iterator();
        while (items.hasNext()) {
            Object item = items.next();
            answer.referrals.put(item, referrals.get(item));
        }
        return (answer);
    }


    /**
     * Returns XML string representation of the referral
     * (user collection) object.
     *
     * @return xml string representation of this object
     */
    public String toString() {
        return (toXML());
    }

    /**
     * Returns an XML representaion of this <code>Referrals</code> object
     */
    protected String toXML() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("\n").append(REFERRALS_ELEMENT_BEGIN)
            .append(XMLUtils.escapeSpecialCharacters(name))
            .append(REFERRALS_DESCRIPTION)
            .append(XMLUtils.escapeSpecialCharacters(description))
            .append("\">");
        Iterator items = referrals.keySet().iterator();
        while (items.hasNext()) {
            String referralName = (String) items.next();
            Referral referral = (Referral) referrals.get(referralName);
            sb.append("\n").append(REFERRAL_ELEMENT)
                .append(XMLUtils.escapeSpecialCharacters(referralName))
                .append(REFERRAL_TYPE)
                .append(XMLUtils.escapeSpecialCharacters(
                        ReferralTypeManager.referralTypeName(referral))) 
                .append("\">");
            // Add attribute values pairs
            Set v = referral.getValues();
            if ((v != null) && !v.isEmpty()) {
                sb.append("\n").append(ATTR_VALUE_BEGIN);
                Iterator values = v.iterator();
                while (values.hasNext()) {
                    sb.append("\n").append(VALUE_BEGIN)
                	.append(XMLUtils.escapeSpecialCharacters(
                                (String) values.next()))
                	.append(VALUE_END);
                }
                sb.append("\n").append(ATTR_VALUE_END);
            }
            sb.append("\n").append(REFERRAL_ELEMENT_END);
        }
        sb.append("\n").append(REFERRALS_ELEMENT_END);
        return (sb.toString());        	
    }

    /** 
     * Returns policy deicision 
     * @param token sso token identifying the user for who the Referrals has to 
     *        be evaluated.
     * @param resourceType resourceType 
     * @param resourceName resourceName
     * @param actionNames a set of action names for which policy results
     *        are to be evaluated. Each element of the set should be a
     *        String
     * @param envParameters a map of environment parameters
     *        Each key of the map is a String valued parameter name
     *        Each value of the map is a set of String values
     * @return policy decision
     * @throws NameNotFoundException if the action name or resource name
     *         is not found
     * @throws SSOException if token is invalid
     * @throws PolicyException for any other exception condition
     */
    PolicyDecision getPolicyDecision(SSOToken token, String resourceType,
            String resourceName, Set actionNames, Map envParameters) 
	    throws SSOException, NameNotFoundException, PolicyException {
	PolicyDecision mergedPolicyDecision = null;
        ServiceType serviceType =
                ServiceTypeManager.getServiceTypeManager()
                .getServiceType(resourceType);
	Set referralNames = getReferralNames();
	Iterator referralIter = referralNames.iterator();
	while (referralIter.hasNext()) {
	    String referralName = (String) referralIter.next();
	    Referral referral = getReferral(referralName);
            if ( referral instanceof OrgReferral) {
                //specially evaluated by PolicyEvaluator
                continue;
            }
            PolicyDecision policyDecision = referral.getPolicyDecision(token, 
                   resourceType, resourceName, actionNames, envParameters);
            if ( mergedPolicyDecision == null ) {
                mergedPolicyDecision = policyDecision;
            } else {
                PolicyEvaluator.mergePolicyDecisions(serviceType, 
                        policyDecision, mergedPolicyDecision);
            }
            actionNames.removeAll(
                    PolicyEvaluator.getFinalizedActions(serviceType, 
                    mergedPolicyDecision));
            if ( actionNames.isEmpty() ) {
                break;
            }
	}
	return (mergedPolicyDecision);
    }

    /**
     * Checks if there is any <code>Referral</code> in this
     * <code>Referrals</code> object.
     * @return <code>true</code> if there is no <code>Referral</code>
     * contained in this object. Else, <code>false</code>
     */
    boolean isEmpty() {
	return (referrals.isEmpty());
    }


    /** 
     * Returns resource names rooted at the given resource name for the given
     *  serviceType that could be governed by this referral set
     * @param token sso token
     * @param serviceTypeName service type name
     * @param resourceName resource name
     * @return names of sub resources for the given resourceName.
     *         The return value also includes the resourceName.
     *
     * @throws PolicyException
     * @throws SSOException
     */
    Set getResourceNames(SSOToken token, String serviceTypeName, 
	    String resourceName) throws PolicyException, SSOException {
	Set resourceNames = new HashSet();
	Set referralNames = getReferralNames();
	Iterator referralIter = referralNames.iterator();
	while (referralIter.hasNext()) {
	    String referralName = (String) referralIter.next();
	    Referral referral = getReferral(referralName);
            if ( referral instanceof OrgReferral) {
                //specially evaluated by PolicyEvaluator
                continue;
            }
            Set rResourceNames = referral.getResourceNames(token, 
                    serviceTypeName, resourceName);
            resourceNames.addAll(rResourceNames);
	}
	return resourceNames;
    }

    // Private variables to construct the XML document
    private static String REFERRALS_ELEMENT_BEGIN = "<Referrals name=\"";
    private static String REFERRALS_DESCRIPTION = "\" description=\"";
    private static String REFERRALS_ELEMENT_END = "</Referrals>";
    private static String REFERRAL_ELEMENT = "<Referral name=\"";
    private static String REFERRAL_TYPE = "\" type=\"";
    private static String REFERRAL_ELEMENT_END = "</Referral>";
    private static String ATTR_VALUE_BEGIN =
        "<AttributeValuePair><Attribute name=\"Values\"/>";
    private static String VALUE_BEGIN = "<Value>";
    private static String VALUE_END = "</Value>";
    private static String ATTR_VALUE_END = "</AttributeValuePair>";
    private static String REFERRAL_VALUES_ATTR_NAME = "Values";
}

