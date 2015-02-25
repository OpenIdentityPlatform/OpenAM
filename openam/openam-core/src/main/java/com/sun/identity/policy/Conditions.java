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
 * $Id: Conditions.java,v 1.4 2008/06/25 05:43:43 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.*;

import org.w3c.dom.*;

import com.sun.identity.policy.interfaces.Condition;
import com.iplanet.sso.*;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * The class <code>Conditions</code> provides methods to maintain
 * a collection of <code>Condition</code> objects that 
 * apply to a <code>Policy</code>. This class provides methods to add, replace
 * and remove <code>Condition</code> objects from this condition collection.
 * The <code>Policy</code> object provides methods to set
 * <code>Conditions</code>, which identifies conditions that apply
 * to the policy
 */
public class Conditions {

    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);
    private String name;
    private String description;
    private Map conditions = new HashMap();

    /**
     * No argument constructor
     */
    Conditions() {
    }

    /**
     * Constructor used by <code>Policy</code> to obtain
     * an instance of <code>Conditions</code> from W3C DOM
     * representation of the object.
     *
     * @param ctm <code>ConditionTypeManager</code>
     * providing methods to handle the  <code>Conditions</code>.
     *
     * @param conditionsNode node that represents the Conditions
     */
    protected Conditions(ConditionTypeManager ctm, Node conditionsNode)
        throws InvalidFormatException, InvalidNameException,
        NameNotFoundException, PolicyException {
        // Check if the node name is PolicyManager.POLICY_CONDITIONS_NODE
        if (!conditionsNode.getNodeName().equalsIgnoreCase(
            PolicyManager.POLICY_CONDITIONS_NODE)) {
            if (PolicyManager.debug.warningEnabled()) {
                PolicyManager.debug.warning(
                   "invalid conditions xml blob given to construct conditions");
            }
            throw (new InvalidFormatException(ResBundleUtils.rbName,
                "invalid_xml_conditions_root_node", null, "",
                PolicyException.CONDITION_COLLECTION));
        }

        // Get the conditions name
        if ((name = XMLUtils.getNodeAttributeValue(conditionsNode,
            PolicyManager.NAME_ATTRIBUTE)) == null) {
            name = "Conditions:" + ServiceTypeManager.generateRandomName();
        }

        // Get the description
        if ((description = XMLUtils.getNodeAttributeValue(conditionsNode,
            PolicyManager.DESCRIPTION_ATTRIBUTE)) == null) {
            description = "";
        }

        // Get individual conditions
        Iterator conditionNodes = XMLUtils.getChildNodes(
            conditionsNode, PolicyManager.CONDITION_POLICY).iterator();
        while (conditionNodes.hasNext()) {
            Node conditionNode = (Node) conditionNodes.next();
            String conditionType = XMLUtils.getNodeAttributeValue(
                conditionNode, PolicyManager.TYPE_ATTRIBUTE);
            if (conditionType == null) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning("condition type is null");
                }
                throw (new InvalidFormatException(
                    ResBundleUtils.rbName,
                    "invalid_xml_conditions_root_node", null, "",
                    PolicyException.CONDITION_COLLECTION));
            }

            // Construct the condition object
            Condition condition = ctm.getCondition(conditionType);

            // Get and set the properties
            Map properties = new HashMap();
            NodeList attrValuePairNodes = conditionNode.getChildNodes();
            int numAttrValuePairNodes = attrValuePairNodes.getLength();
            for (int j = 0; j < numAttrValuePairNodes; j++) {
                Node attrValuePairNode = attrValuePairNodes.item(j);
                Node attributeNode 
                        = XMLUtils.getChildNode(attrValuePairNode, 
                        PolicyManager.ATTR_NODE);
                if ( attributeNode != null ) {
                    String name = XMLUtils.getNodeAttributeValue(attributeNode,
                            PolicyManager.NAME_ATTRIBUTE);
                    Set values = XMLUtils.getAttributeValuePair(
                            attrValuePairNode);
                    if ( ( name != null ) && ( values != null ) ) {
                        properties.put(name, values);
                    }
                }
            }
            condition.setProperties(properties);

            // Get the friendly name given to condition
            String conditionName = XMLUtils.getNodeAttributeValue(
                conditionNode, PolicyManager.NAME_ATTRIBUTE);

            // Add the condition to conditions collection
            addCondition(conditionName, condition);
        }
    }

    /**
     * Constructor to obtain an instance of <code>Conditions</code>
     * to hold collection of conditions represented as
     * <code>Condition</code>
     *
     * @param name name for the collection of <code>Condition</code>
     * @param description user friendly description for
     * the collection of <code>Condition</code>  
     */
    public Conditions(String name, String description) {
        this.name = (name == null) ? 
            ("Conditions:" + ServiceTypeManager.generateRandomName()) : name;
        this.description = (description == null) ?
            "" : description;
    }

    /**
     * Returns the name for the collection of conditions
     * represented as <code>Condition</code>
     *
     * @return name of the collection of conditions
     */
    public String getName() {
        return (name);
    }

    /**
     * Returns the description for the collection of conditions
     * represented as <code>Condition</code>
     *
     * @return description for the collection of conditions
     */
    public String getDescription() {
        return (description);
    }

    /**
     * Sets the name for this instance of the
     * <code>Conditions<code> which contains a collection
     * of conditions respresented as <code>Condition</code>.
     *
     * @param name for the collection of conditions
     */
    public void setName(String name) {
        this.name = (name == null) ?
            ("Conditions:" + ServiceTypeManager.generateRandomName()) : name;
    }

    /**
     * Sets the description for this instance of the
     * <code>Conditions</code> which contains a collection
     * of conditions respresented as <code>Condition</code>.
     *
     * @param description description for the collection conditions
     */
    public void setDescription(String description) {
        this.description = (description == null) ?
            "" : description;
    }

    /**
     * Returns the names of <code>Condition</code> objects
     * contained in this object.
     *
     * @return names of <code>Condition</code> contained in
     * this object
     */
    public Set getConditionNames() {
        return (conditions.keySet());
    }

    /**
     * Returns the <code>Condition</code> object associated
     * with the given condition name.
     *
     * @param conditionName name of the condition object
     *
     * @return condition object corresponding to condition name
     *
     * @exception NameNotFoundException if a condition
     * with the given name is not present
     */
    public Condition getCondition(String conditionName)
        throws NameNotFoundException {
        Condition answer = (Condition) conditions.get(conditionName);
        if (answer == null) {
            String[] objs = { conditionName };
            throw (new NameNotFoundException(ResBundleUtils.rbName,
                "name_not_present", objs, 
                conditionName, PolicyException.CONDITION_COLLECTION));
        
        }
        return (answer);
    }

    /**
     * Adds a <code>Condition</code> object to the this instance
     * of condition collection. Since the name is not provided it
     * will be dynamically assigned such that it is unique within
     * this instance of the condition collection. However if a
     * condition entry with the same name already exists in the 
     * condition collection <code>NameAlreadyExistsException</code> 
     * will be thrown.
     *
     * @param condition instance of the condition object added to this
     * collection
     *
     * @exception NameAlreadyExistsException throw if a
     * condition object is present with the same name
     */
    public void addCondition(Condition condition)
        throws NameAlreadyExistsException {
        addCondition(null, condition);
    }

    /**
     * Adds a <code>Condition</code> object to the this instance
     * of conditions collection. If another condition with the same 
     * name already exists in the conditions collection
     * <code>NameAlreadyExistsException</code> will be thrown.
     *
     * @param conditionName name for the condition instance
     * @param condition instance of the condition object added to this
     * collection
     *
     * @exception NameAlreadyExistsException if a
     * condition object is present with the same name 
     */
    public void addCondition(String conditionName, Condition condition)
        throws NameAlreadyExistsException {
        if (conditionName == null) {
            conditionName = "Condition:" +
                ServiceTypeManager.generateRandomName();
        }
        if (conditions.containsKey(conditionName)) {
            String[] objs = { conditionName };
            throw (new NameAlreadyExistsException(ResBundleUtils.rbName,
                "name_already_present", objs,
                conditionName, PolicyException.CONDITION_COLLECTION));
        }
        conditions.put(conditionName, condition);
    }

    /**
     * Replaces an existing condition object having the same name
     * with the new one. If a <code>Condition</code> with the given
     * name does not exist, <code>NameNotFoundException</code>
     * will be thrown.
     *
     * @param conditionName name for the condition instance
     * @param condition instance of the condition object that will
     * replace another condition object having the given name
     *
     * @exception NameNotFoundException if a condition instance
     * with the given name is not present
     */
    public void replaceCondition(String conditionName, Condition condition)
        throws NameNotFoundException {
        if (!conditions.containsKey(conditionName)) {
            String[] objs = { conditionName };
            throw (new NameNotFoundException(ResBundleUtils.rbName,
                "name_not_present", objs,
                conditionName, PolicyException.CONDITION_COLLECTION));
        }
        conditions.put(conditionName, condition);
    }

    /**
     * Removes the <code>Condition</code> object identified by
     * the condition name. If a condition instance with the given
     * name does not exist, the method will return silently.
     *
     * @param conditionName name of the condition instance that
     * will be removed from the conditions collection
     * @return the condition that was just removed
     */
    public Condition removeCondition(String conditionName) {
        return (Condition)conditions.remove(conditionName);
    }
 
    /**
     * Removes the <code>Condition</code> object identified by
     * object's <code>equals</code> method. If a condition instance
     * does not exist, the method will return silently.
     *
     * @param condition condition object that
     * will be removed from the conditions collection
     * @return the condition that was just removed
     */
    public Condition removeCondition(Condition condition) {
        String conditionName = getConditionName(condition);
        if (conditionName != null) {
            return (Condition) removeCondition(conditionName);
        }
        return null;
    }

    /**
     * Returns the name associated with the given condition object.
     * It uses the <code>equals</code> method on the condition
     * to determine equality. If a condition instance that matches
     * the given condition object is not present, the method
     * returns <code>null</code>.
     *
     * @param condition condition object for which this method will
     * return its associated name
     *
     * @return user friendly name given to the condition object;
     * <code>null</code> if not present
     */
    public String getConditionName(Condition condition) {
        String answer = null;
        Iterator items = conditions.keySet().iterator();
        while (items.hasNext()) {
            String conditionName = (String) items.next();
            if (condition.equals(conditions.get(conditionName))) {
                answer = conditionName;
                break;
            }
        }
        return (answer);
    }

    /**
     * Checks if two <code>Conditions</code> are identical.
     * Two conditions (or conditions collections) are identical only
     * if both have the same set of <code>Condition</code> objects.
     *
     * @param o object againt which this conditions object
     * will be checked for equality
     *
     * @return <code>true</code> if all the conditions match;
     * <code>false</code> otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof Conditions) {
            Conditions s = (Conditions) o;
            Iterator iter = conditions.entrySet().iterator();
            while (iter.hasNext()) {
                Object ss = ((Map.Entry) iter.next()).getValue();
                if (!s.conditions.containsValue(ss)) {
                    return (false);
                }
            }
            return (true);
        }
        return (false);
    }

    /**
     * Returns a new copy of this object with the identical
     * set of conditions collections (conditions).
     *
     * @return a copy of this object with identical values
     */
    public Object clone() {
        Conditions answer = null;
        try {
            answer = (Conditions) super.clone();
        } catch (CloneNotSupportedException se) {
            answer = new Conditions();
        }
        answer.name = name;
        answer.description = description;
        answer.conditions = new HashMap();
        Iterator items = conditions.keySet().iterator();
        while (items.hasNext()) {
            Object item = items.next();
            Condition condition = (Condition) conditions.get(item);
            answer.conditions.put(item, condition.clone());
        }
        return (answer);
    }

    /**
     * Checks whether the effective result of conditions is an allow or deny.  
     * The effective result is an allow only if each condition type of this
     * contraint collection evaluates to allow, for the environment parameters
     * passed in env.  When there are multiple condition elements in the 
     * conditions collection, the condition evaluation logic does a 
     * logical or for the condition elements of the same type and does a 
     * logical and between sets of condition elements of different condition
     * types
     *
     * @param token single sign on token of the user
     * @param env a map of key/value pairs containing any information 
     *            that could be used by each contraint to evaluate
     *            the allow/deny result
     * @return <code>true</code> if the effective result is an allow.  
     *         Otherwise <code>false</code>.
     *
     * @throws PolicyException if an error occured 
     * @throws SSOException if the token is invalid
     */
    public boolean isAllowed(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        return getConditionDecision(token, env).isAllowed();
    }

    /**
     * Gets result of evalutating the conditions.  
     * The effective result is an allow only if each condition type of this
     * contraint collection evaluates to allow, for the environment parameters
     * passed in env.  When there are multiple condition elements in the 
     * conditions collection, the condition evaluation logic does a 
     * logical or for the condition elements of the same type and does a 
     * logical and between sets of condition elements of different condition
     * types
     *
     * @param token single sign on token of the user
     * @param env a map of key/value pairs containing any information 
     *            that could be used by each contraint to evaluate
     *            the allow/deny result
     * @return <code>result of evaluating the conditions</code>
     *
     * @throws PolicyException if an error occured 
     * @throws SSOException if the token is invalid
     */
    ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        boolean allowed = false;
        HashMap allowMap = new HashMap();
        HashMap advicesMap = new HashMap();
        long timeToLive = Long.MAX_VALUE;
        Iterator items = conditions.entrySet().iterator();
        while (items.hasNext()) {
            Condition condition = (Condition)
                ((Map.Entry) items.next()).getValue();
            String conditionType 
                    = ConditionTypeManager.conditionTypeName(condition);
            boolean previousAllowed = false;
            Boolean previousValue = (Boolean) allowMap.get(conditionType);
            if ( previousValue != null ) {
                previousAllowed = previousValue.booleanValue();
            }
            ConditionDecision cd = condition.getConditionDecision(token, env);
            boolean currentAllowed = cd.isAllowed();
            currentAllowed = currentAllowed || previousAllowed;
            allowMap.put(conditionType, Boolean.valueOf(currentAllowed));
            Map cdAdvices = cd.getAdvices();
            if ( (cdAdvices != null) && (!cdAdvices.isEmpty()) ) {
                Map advices = (Map) advicesMap.get(conditionType);
                if ( advices == null ) {
                    advices = new HashMap();
                    advicesMap.put(conditionType, advices);
                }
                PolicyUtils.appendMapToMap( 
                        cdAdvices, (Map)advicesMap.get(conditionType));
            }
            long ttl = cd.getTimeToLive();
            if ( ttl < timeToLive) {
                timeToLive = ttl;
            }
        }
        Map effectiveAdvices = new HashMap();
        if ( !allowMap.containsValue(Boolean.FALSE) ) {
            allowed = true;
        } else {
            Iterator conditionTypes = advicesMap.keySet().iterator();
            while ( conditionTypes.hasNext() ) {
                String conditionType = (String) conditionTypes.next();
                Boolean result = (Boolean) allowMap.get(conditionType);
                if ( result.equals(Boolean.FALSE) ) {
                    PolicyUtils.appendMapToMap( 
                            (Map)advicesMap.get(conditionType), 
                            effectiveAdvices);
                }
            }
        }
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At Conditions.getConditionDecision():allowed,"
            + "timeToLive, " + " advices=" + allowed +"," +  timeToLive 
            + "," + effectiveAdvices);
        }
        return new ConditionDecision(allowed, timeToLive, effectiveAdvices);
    }

    /**
     * Returns XML string representation of the condition
     * (conditions collection) object.
     *
     * @return xml string representation of this object
     */
    public String toString() {
        return (toXML());
    }

    /**
     * Returns XML string representation of the condition
     * (conditions collection) object.
     *
     * @return xml string representation of this object
     */
    protected String toXML() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("\n").append(CONDITIONS_ELEMENT_BEGIN)
            .append(XMLUtils.escapeSpecialCharacters(name))
            .append(CONDITIONS_DESCRIPTION)
            .append(XMLUtils.escapeSpecialCharacters(description))
            .append("\">");
        Iterator items = conditions.keySet().iterator();
        while (items.hasNext()) {
            String conditionName = (String) items.next();
            Condition condition = (Condition) conditions.get(conditionName);
            sb.append("\n").append(CONDITION_ELEMENT)
                .append(XMLUtils.escapeSpecialCharacters(conditionName))
                .append(CONDITION_TYPE)
                .append(XMLUtils.escapeSpecialCharacters(
                        ConditionTypeManager.conditionTypeName(condition)))
                .append("\">");
            // Add attribute values pairs
            Map properties = condition.getProperties();
            if (properties != null) {
                Set keySet = properties.keySet();
                Iterator keys = keySet.iterator();
                while ( keys.hasNext() ) {
                    sb.append("\n").append(ATTR_VALUE_PAIR_BEGIN);
                    String key = (String) keys.next();
                    sb.append(ATTR_NAME_BEGIN);
                    sb.append(quote(
                            XMLUtils.escapeSpecialCharacters(key)));
                    sb.append(ATTR_NAME_END);
                    Set valueSet = (Set) properties.get(key);
                    if ( (valueSet != null ) && (!valueSet.isEmpty()) ) {
                        Iterator values = valueSet.iterator();
                        while ( values.hasNext() ) {
                            String value = (String) values.next();
                            sb.append(VALUE_BEGIN);
                            sb.append(XMLUtils.escapeSpecialCharacters(
                                    value));
                            sb.append(VALUE_END);
                        }
                    }
                    sb.append("\n").append(ATTR_VALUE_PAIR_END);
                }
            }
            sb.append("\n").append(CONDITION_ELEMENT_END);
        }
        sb.append("\n").append(CONDITIONS_ELEMENT_END);
        return (sb.toString());                
    }

    /**
     * Places quotes around a string
     */
    private String quote(String s) {
        String str = null;
        if ( s == null ) {
            str = "\"\"";
        } else {
            str = "\"" + s + "\"";
        }
        return str;
    }

    /**
     * Returns the number of <code>Condition</code> elements in this
     * </code>Conditions</code> object
     *
     * @return the number of <code>Condition</code> elements in this
     *           </code>Conditions</code> object
     */
    int size() {
        return conditions.size();
    }

    // Private variables to construct the XML document
    private static String CONDITIONS_ELEMENT_BEGIN = "<Conditions name=\"";
    private static String CONDITIONS_DESCRIPTION = "\" description=\"";
    private static String CONDITIONS_ELEMENT_END = "</Conditions>";
    private static String CONDITION_ELEMENT = "<Condition name=\"";
    private static String CONDITION_TYPE = "\" type=\"";
    private static String CONDITION_ELEMENT_END = "</Condition>";
    private static String VALUE_BEGIN = "<Value>";
    private static String VALUE_END = "</Value>";
    private static String ATTR_VALUE_PAIR_BEGIN = "<AttributeValuePair>";
    private static String ATTR_NAME_BEGIN = "<Attribute name=";
    private static String ATTR_NAME_END = "/>";
    private static String ATTR_VALUE_PAIR_END = "</AttributeValuePair>";
}
