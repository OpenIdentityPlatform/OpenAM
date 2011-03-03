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
 * $Id: PolicyDecision.java,v 1.3 2008/06/25 05:43:44 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchema;
import org.w3c.dom.*;


/**
 * The <code>PolicyDecision</code> class represents the result of a policy
 * evaluation. 
 *
 * @supported.api
 */ 
public class PolicyDecision {

    static final String POLICY_DECISION = "PolicyDecision";
    static final String RESPONSE_DECISIONS = "ResponseDecisions";
    
    //added in 7.0 for attributes provided by policy response providers
    static final String RESPONSE_ATTRIBUTES = "ResponseAttributes"; 

    Map actionDecisions = new HashMap();
    private Map responseDecisions;
    private Map responseAttributes;
    private long timeToLive = Long.MAX_VALUE;
    private boolean advicesAreSet = false;

    /**
     * Default constructor.
     * @supported.api
     */
    public PolicyDecision() {
    }

    /**
     * Gets the  <code>Map</code> of action decisions associated 
     * with this policy decision. The 
     * action name is the key to the Map. The value for each key is an 
     * <code>ActionDecision</code>.
     *
     * @return  the  <code>Map</code> of action decisions associated 
     *         with this policy decision. The 
     *         action name is the key to the Map. The value for each key is an 
     *         <code>ActionDecision</code>.
     * @supported.api
     */
    public Map getActionDecisions() {
        return actionDecisions;
    }

    /**
     * Add an <code>ActionDecision</code> to the <code>PolicyDecision</code>
     * These are the rules followed to add action decision:
     * If the action schema has boolean syntax, boolean false value
     * overrides boolean true value. The time to live of boolean false 
     * value overrides the time to live of boolean true value.
     * Otherwise, action values are simply  aggregated. Time to live
     * is set to the minimum of time to live(s) of all values of the
     * action.
     *
     * @param newActionDecision an <code>ActionDecision</code> to be added.
     * @param resourceType <code>ServiceType</code> representing the
     * service which provides the schema for the action.
     * 
     */

    void addActionDecision(ActionDecision newActionDecision, ServiceType
            resourceType) {
        String action = newActionDecision.getActionName();
        ActionDecision oldActionDecision 
                = (ActionDecision) actionDecisions.get(action);
        if ( oldActionDecision == null ) {
            addActionDecision(newActionDecision);
        } else {
            ActionSchema actionSchema = null;
            AttributeSchema.Syntax actionSyntax = null;
            try {
              actionSchema = resourceType.getActionSchema(action);
              actionSyntax = actionSchema.getSyntax();
            } catch(InvalidNameException e) {
              PolicyManager.debug.error(
                    "can not find action schmea for action = " 
                     + action, e );
            }
            if (!AttributeSchema.Syntax.BOOLEAN.equals(
                    actionSyntax)) {
              addActionDecision(newActionDecision);
            } else { //boolean valued action
              String falseValue = actionSchema.getFalseValue();
              String trueValue = actionSchema.getTrueValue();
              addActionDecision(newActionDecision, trueValue, falseValue);
            }
        }
    }

    /**
     * Add an <code>ActionDecision</code> to the <code>PolicyDecision</code>
     * using the provided <code>trueValue</code> and <code>falseValue</code>
     * These are the rules followed to add action decision:
     * Boolean false value overrides boolean true value. The time to live 
     * of boolean false value overrides the time to live of boolean true value.
     * Otherwise, action values are simply  aggregated. Time to live
     * is set to the minimum of time to live(s) of all values of the
     * action.
     *
     * @param newActionDecision an <code>ActionDecision</code> to be added.
     * @param trueValue <code>String</code> representing the </code>true</code>
     * value in the action schema.
     * @param falseValue <code>String</code> representing the 
     * </code>false</code> value in the action schema.
     * 
     */
    public void addActionDecision(ActionDecision newActionDecision, 
            String trueValue, String falseValue) {
        String action = newActionDecision.getActionName();
        ActionDecision oldActionDecision 
                = (ActionDecision) actionDecisions.get(action);
        if ( (oldActionDecision == null) 
                || (trueValue == null) || (falseValue == null)) {
            addActionDecision(newActionDecision);
        } else { //boolean valued action
            long newTtl = newActionDecision.getTimeToLive();
            long oldTtl = oldActionDecision.getTimeToLive();
            Set oldActionValues = oldActionDecision.getValues();
            Set newActionValues = newActionDecision.getValues();
            Map advices = null;
            Map oldAdvices = oldActionDecision.getAdvices();
            Map newAdvices = newActionDecision.getAdvices();
            advices = PolicyUtils.addMapToMap(oldAdvices, newAdvices);
            if ( (oldActionValues != null) 
                    && (oldActionValues.contains(falseValue)) ) {
              if ( (newActionValues != null) 
                        && newActionValues.contains(falseValue) ) {
                  //both old and new values are false
                  //get the ttl to max of newTtl and oldTtl
                  oldActionDecision.setTimeToLive(Math.max(newTtl, oldTtl));
              }

              /* else block not required here since
                 oldActionDecision does not need to change as it is false
                 and newActionDecision is null or true
              */
            } else if ( (oldActionValues != null)
                    && oldActionValues.contains(trueValue) ) {
              if ( (newActionValues != null)
                        && newActionValues.contains(falseValue) ) {
                  actionDecisions.put(action, newActionDecision);
              } else if ( newActionDecision.getValues().contains(trueValue) ) {
                  //get the ttl to max of newTtl and oldTtl
                  oldActionDecision.setTimeToLive(Math.max(newTtl, oldTtl));
              }
            } else {
                  actionDecisions.put(action, newActionDecision);
            }
            ActionDecision ad = (ActionDecision) actionDecisions.get(action);
            ad.setAdvices(advices);
            setTimeToLive();
        }
    }

    /**
     * Adds an action decision to this object
     * if there is already an existing actionDecision associated with the
     * action name in the param <code>actionDecision</code>, merges
     * the values of the new decision with the existing one,
     * changing the time to live for the decision appropriately.
     *
     * @param actionDecision action decision to be added
     * @supported.api
     */
    public void addActionDecision(ActionDecision actionDecision) {
        ActionDecision oldDecision =
                (ActionDecision) actionDecisions.get(
                actionDecision.getActionName());
        if ( oldDecision == null ) {
            actionDecisions.put(actionDecision.getActionName(), 
                    actionDecision);
        } else {
            Set oldValues = oldDecision.getValues();
            if ( (oldValues == Collections.EMPTY_SET)
                    || ( oldValues == null) ) {
                    oldDecision.setValues(actionDecision.getValues());
            } else {
                oldValues.addAll(actionDecision.getValues());
            }
            if ( actionDecision.getTimeToLive() 
                    < oldDecision.getTimeToLive() ) {
                oldDecision.setTimeToLive(actionDecision.getTimeToLive());
            }
            PolicyUtils.appendMapToMap(actionDecision.getAdvices(),
                    oldDecision.getAdvices());
        }
        setTimeToLive();
    }

    /**
     * Gets a String representation of this <code>PolicyDecision</code>
     * @return a String representation of this <code>PolicyDecision</code>
     *
     * @supported.api
     */
     public String toString() {
        StringBuilder sb = new StringBuilder();
        if ((responseAttributes != null) && 
            (responseAttributes != Collections.EMPTY_MAP)) {
             Iterator attrNames = responseAttributes.keySet().iterator();
             while ( attrNames.hasNext() ) {
                 String attrName = (String) attrNames.next();
                 Set attrValues = (Set) responseAttributes.get(attrName);
                 sb.append(attrName).append("=").append(attrValues).append("\n");
             }
        }
        Iterator actionNames = actionDecisions.keySet().iterator();
        while ( actionNames.hasNext() ) {
            String actionName = (String) actionNames.next();
            ActionDecision actionDecision = 
                (ActionDecision) actionDecisions.get(actionName);
            Set actionValues = (Set) actionDecision.getValues();
            sb.append(actionName).append("=").append(actionValues).append("\n");
        }
        return sb.toString();
     }


    /**
     * Gets an XML representation of this object
     * @return an XML representation of this object
     *
     * @supported.api
     */
     public String toXML() {
        StringBuilder sb  = new StringBuilder(300);
        sb.append("<").append(POLICY_DECISION)
               /*
                .append(" ").append("timeToLive")
                .append("=\"").append(timeToLive).append("\"") 
                .append(" ").append("hasAdvices")
                .append("=\"").append(hasAdvices()).append("\"")  
                */
                .append(">").append(PolicyUtils.CRLF);
        if ((responseAttributes != null) && 
            (responseAttributes != Collections.EMPTY_MAP)) {
            sb.append("<").append(RESPONSE_ATTRIBUTES);
            sb.append(">").append(PolicyUtils.CRLF);
            sb.append(PolicyUtils.mapToXMLString(responseAttributes));
            sb.append("<").append("/").append(RESPONSE_ATTRIBUTES);
            sb.append(">").append(PolicyUtils.CRLF);
        }
        Iterator actionNames = actionDecisions.keySet().iterator();
        while ( actionNames.hasNext() ) {
            String actionName = (String) actionNames.next();
            ActionDecision actionDecision = (ActionDecision)
            actionDecisions.get(actionName);
            sb.append(actionDecision.toXML());
        }
        if (responseDecisions != null) {
            sb.append("<").append(RESPONSE_DECISIONS).append(">")
                    .append(PolicyUtils.CRLF);
            sb.append(PolicyUtils.mapToXMLString(responseDecisions));
            sb.append("</").append(RESPONSE_DECISIONS).append(">")
                    .append(PolicyUtils.CRLF);
        }
        sb.append("</").append(POLICY_DECISION).append(">");
        sb.append(PolicyUtils.CRLF);
        return sb.toString();
     }

    /**
     * Gets a PolicyDecision given corresponding XML node
     * @param policyDecisionNode XML node for the policy decision
     * @return policy decision based on the XML node
     */
     public static PolicyDecision parsePolicyDecision(Node policyDecisionNode) 
        throws PolicyException 
    {
        PolicyDecision policyDecision = new PolicyDecision();
        Set nodeSet = XMLUtils.getChildNodes(policyDecisionNode, 
                ActionDecision.ACTION_DECISION);
        if (nodeSet == null) {
            PolicyManager.debug.error("parsePolicyDecision: Required element "
                + "not found in policy decision node:"
                + ActionDecision.ACTION_DECISION);
            Object [] args = { ActionDecision.ACTION_DECISION };
            throw new PolicyException(ResBundleUtils.rbName,
                "missing_element", args, null);
        } else {
            Iterator nodes = nodeSet.iterator();
            while (nodes.hasNext()) {
                Node node = (Node)nodes.next();
                ActionDecision actionDecision = 
                    ActionDecision.parseActionDecision(node);
                policyDecision.addActionDecision(actionDecision);
            }
        }
        Set resposeAttrsSet = XMLUtils.getChildNodes(policyDecisionNode, 
                RESPONSE_ATTRIBUTES);
        if ( (resposeAttrsSet != null) && !resposeAttrsSet.isEmpty() ) {
            Node node = (Node) resposeAttrsSet.iterator().next();
            Map responseAttrsMap = PolicyUtils.parseAttributeValuePairs(node);
            policyDecision.setResponseAttributes(responseAttrsMap);
        }
        Set responseNodeSet = XMLUtils.getChildNodes(policyDecisionNode, 
                RESPONSE_DECISIONS);
        if ( (responseNodeSet != null) && !responseNodeSet.isEmpty() ) {
            Node node = (Node) responseNodeSet.iterator().next();
            Map responseMap = PolicyUtils.parseAttributeValuePairs(node);
            policyDecision.setResponseDecisions(responseMap);
        }
        return policyDecision;
     }

    /**
     * 
     * Gets response decisions associated with this policy decision
     * @return <code>Map</code> representing the response decisions associated 
     * with this policy decision
     *
     */
    public Map getResponseDecisions() {
        return responseDecisions;
    }

    /**
     * 
     * Sets response decisions associated with this policy decision
     * @param responseDecisions A <code>Map</code> representing response 
     * decisions associated with this policy decision
     */
    public void setResponseDecisions(Map responseDecisions) {
        this.responseDecisions = responseDecisions;
    }


    /**
     * 
     * Gets response attributes associated with this policy decision.
     * Response attributes are computed as an aggregation of the return
     * <code>Map</code>(s) of the <code>ResponseProvider</code> objects 
     * associated with the policy obtained via the getResponseDecision() call.
     * @return the <code>Map</code> of response attributes associated with 
     * this policy decision.
     *
     */
    public Map getResponseAttributes() {
        return responseAttributes;
    }

    /**
     * 
     * Sets response attributes associated with this policy decision
     * @param responseAttributes <code>Map</code> of attribute value pairs 
     * associated with this policy decision.
     */
    public void setResponseAttributes(Map responseAttributes) {
        this.responseAttributes = responseAttributes;
    }
    /**
     * Makes a copy of this object
     *
     * @return a copied instance
     */
    public Object clone() {
        PolicyDecision clone = new PolicyDecision();
        clone.actionDecisions = new HashMap(actionDecisions.size());
        Iterator actionDecisionIter = actionDecisions.keySet().iterator();
        while (actionDecisionIter.hasNext()) {
            String key = (String) actionDecisionIter.next();
            ActionDecision ad = (ActionDecision) actionDecisions.get(key);
            clone.addActionDecision((ActionDecision)ad.clone());
        }

        if (responseDecisions != null) {
            clone.responseDecisions = new HashMap(responseDecisions.size());
            Iterator responseDecisionsIter =
                responseDecisions.keySet().iterator();
            while (responseDecisionsIter.hasNext()) {
                String key = (String) responseDecisionsIter.next();
                clone.responseDecisions.put(key, responseDecisions.get(key));
            }
        }
        if (responseAttributes != null) {
            clone.responseAttributes = new HashMap(responseAttributes.size());
            Iterator responseAttributesIter =
                responseAttributes.keySet().iterator();
            while (responseAttributesIter.hasNext()) {
                String key = (String) responseAttributesIter.next();
                clone.responseAttributes.put(key, responseAttributes.get(key));
            }
        }
        return clone;
    }


    /**
     * Gets the GMT time in milliseconds since epoch when this object is to
     * be treated as expired. That is the policy decision would likely 
     * be different after that time.
     * This is computed as a result of <code>SimpleTimeCondition</code>s 
     * specified in the <code>Policy</code> definition. 
     *
     * @return time to live
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
     * Sets the <code>timeToLive</code> value of the policy decision to the
     * smallest of <code>timeToLive(s)<code> of contained
     * <code>ActionDecision(s)<code>. Also sets value of
     * <code>advicesAreSet</code>. This is set to <code>true</code>
     * if any of the contained action decision(s) has advice defined.
     */
    private void setTimeToLive() {
        timeToLive = Long.MAX_VALUE;
        advicesAreSet = false;
        Iterator actionDecisionIter = actionDecisions.keySet().iterator();
        while (actionDecisionIter.hasNext()) {
            String key = (String) actionDecisionIter.next();
            ActionDecision ad = (ActionDecision) actionDecisions.get(key);
            long actionTtl = ad.getTimeToLive();
            if ( actionTtl < timeToLive) {
                timeToLive = actionTtl;
            }
            advicesAreSet = advicesAreSet || 
                 ((ad.getAdvices()) != null) && (!(ad.getAdvices().isEmpty()));
        }
    }

    /**
     * Sets the timeToLive value of the policy decision.
     *
     * @param ttl timeToLive value to be set
     */
    void setTimeToLive(long ttl) {
        timeToLive = ttl;
    }

    /**
     * Checks wether advices are set in this object
     * @return <code>true</code>, if advices are set, else <code>false</code>
     */
    public boolean hasAdvices() {
        return advicesAreSet;
    }
}
