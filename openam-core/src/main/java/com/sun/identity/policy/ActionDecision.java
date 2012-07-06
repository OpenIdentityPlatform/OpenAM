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
 * $Id: ActionDecision.java,v 1.5 2008/06/25 05:43:43 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import org.w3c.dom.*;


/**
 * The <code>ActionDecision</code> class represents the action results of a 
 * policy evaluation. It has action values for a given <code>action</code> and 
 * <code>advice</code>. 
 *
 * @supported.api
 */
public class ActionDecision {

    static final String ACTION_DECISION = "ActionDecision";
    static final String ADVICES = "Advices";
    static final String VALUES = "Values";
    static final String TIME_TO_LIVE = "timeToLive";
    static Debug debug = Debug.getInstance("amPolicy");

    private String actionName;
    private Set values;
    private long timeToLive = Long.MAX_VALUE;
    private Map advices;

    /**
     *  Difference of system clock on the client machine compared to 
     * policy server machine. Valid life of policy decisions are extended 
     * by this skew on the client side.
     * Value for this is set by reading property 
     * com.sun.identity.policy.client.clockSkew
     * from SystemProperties
     * If the value is not defined in AMConfig.properties, 
     * this would default to 0.
     */
    private static long clientClockSkew = 0;

    /**
     * No argument constructor
     * @deprecated No replacement API provided. 
     * There should be no need to invoke this constructor.
     */
    public ActionDecision() {
    }

    /**
     * Constructor
     * @param actionName name of the action.
     * @param values a <code>Set></code> of <code>String</code> values for the 
     * action
     * @supported.api
     */
    public ActionDecision(String actionName, Set values) {
        this.actionName = actionName;
        this.values = values;
    }

    /**
     * Constructor
     * @param actionName action name
     * @param values a <code>Set</code> of <code>String</code> values for the 
     * action
     * @param advices <code>advices</code> associated with this action 
     *       decision. The advice name is the key to the Map. The
     *       value is a set of advice message Strings corresponding 
     *       to the  advice name.
     * @param timeToLive the GMT time in milliseconds since epoch 
     *       when this object is to  be treated as expired. 
     *       That is the action values would likely be different
     *       after that time.
     * @supported.api
     */
    public ActionDecision(String actionName, Set values, Map advices,
        long timeToLive
    ) {
        this.actionName = actionName;
        this.values = values;
        this.advices = advices;
        this.timeToLive = timeToLive;
    }

    /**
     * Gets the name of the action
     *
     * @return name of the action
     * @supported.api
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * Sets the action values for the action.  
     *
     * @param values a <code>Set</code> of String values
     * @supported.api
     */
    public void setValues(Set values) {
        this.values = values;
    }

    /**
     * Gets the action values for the action.  
     *
     * @return a <code>Set>/code> of String values
     * @supported.api
     */
    public Set getValues() {
        return values;
    }

    /**
     * Gets the GMT time in milliseconds since epoch when this object is to
     * be treated as expired. That is the action values would likely be 
     * different after that time.
     * This is computed as a result of <code>SimpleTimeCondition(s)</code>
     * specified in the Policy definition. 
     *
     * @return long represeting the time to live for this object.
     * @supported.api
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
     * Sets the GMT time in milliseconds since epoch when this object is to
     * be treated as expired. That is the action values would likely be 
     * different after that time.
     * This is computed as a result of <code>SimpleTimeCondition(s)</code> 
     * specified in the Policy definition. 
     *
     * @param timeToLive time to live
     * @supported.api
     */
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * Sets <code>advices</code> associated with this <code>ActionDecision
     * </code>.
     * The advice name is the key to the <code>Map</code>. The
     * value is a <code>Set</code> of advice message Strings corresponding to 
     * the advice name. The two  possible advices are authentication 
     * level(<code>AuthLevel</code>) and authentication modules
     * (<code>AuthSchemes</code>). The advice message Strings for
     * <code>AuthLevel</code> are integer valued.
     *
     * @param advices map of advices
     * @supported.api
     */
    public void setAdvices(Map advices) {
        this.advices = advices;
    }

    /**
     * Returns a <code>Map</code> of <code>advices</code> associated with this 
     * object. 
     * The advice name is the key to the <code>Map</code>. The
     * value is a <code>Set</code> of advice message Strings corresponding to 
     * the advice name. The two  possible advices are authentication 
     * level(<code>AuthLevel</code>) and authentication modules
     * (<code>AuthSchemes</code>). The advice message Strings for
     * <code>AuthLevel</code> are integer valued.
     *
     * @return advices associated with this <code>ActionDecision</code>.
     * @supported.api
     */
    public Map getAdvices() {
        return advices;
    }

    /**
     * Gets a String representation of this object
     *
     * @return a String representation of this object
     * @supported.api
     */
    public String toString() {
        return actionName + "=" + values;
    }

    /**
     * Gets an XML representation of this object
     *
     * @return XML representation of this object
     * @supported.api
     */
     public String toXML() {
        StringBuilder sb  = new StringBuilder(300);
        sb.append("<").append(ACTION_DECISION).append(" ");
        sb.append(TIME_TO_LIVE).append("=").append(
                PolicyUtils.quote(timeToLive)).append(">");
        sb.append(PolicyUtils.CRLF);
        sb.append(PolicyUtils.attributeValuePairToXMLString(getActionName(), 
            values));
        sb.append("<").append(ADVICES).append(">").append(PolicyUtils.CRLF);
        if (advices != null) {
            sb.append(PolicyUtils.mapToXMLString(advices));
        }
        sb.append("</").append(ADVICES).append(">").append(PolicyUtils.CRLF);
        sb.append("</").append(ACTION_DECISION).append(">").append(
            PolicyUtils.CRLF);
        return sb.toString();
     }

     /**
     * Creates an ActionDecisions object given a w3c DOM node
      *  @param actionDecisionNode w3c DOM node for action decision
      *
      *  @return ActionDecisions object created using the w3c DOM node
      *  @throws PolicyException if any error occurs during parsing.
      */
     public static ActionDecision parseActionDecision(Node actionDecisionNode) 
             throws PolicyException {
        ActionDecision actionDecision = null;
        //process action name and values
        Set nodeSet = XMLUtils.getChildNodes(actionDecisionNode, 
        PolicyUtils.ATTRIBUTE_VALUE_PAIR);
        if ( (nodeSet == null) ||  (nodeSet.isEmpty()) ) {
            debug.error("parseActionDecision: missing element " 
            + PolicyUtils.ATTRIBUTE_VALUE_PAIR);
            return null;
        }
        Iterator nodes = nodeSet.iterator();
        Node node = (Node)nodes.next();
        String actionName = PolicyUtils.getAttributeName(node);
        Set actionValues = PolicyUtils.getAttributeValues(node);
        actionDecision = new ActionDecision(actionName,
        actionValues);

        //process timeToLive
        long timeToLive = Long.MAX_VALUE;
        String ttlString = XMLUtils.getNodeAttributeValue(actionDecisionNode,
        ActionDecision.TIME_TO_LIVE) ;
        if ( ttlString != null ) {
            try {
                timeToLive = Long.parseLong(ttlString);
                if (timeToLive != Long.MAX_VALUE) {
                    timeToLive += clientClockSkew;
                }
            } catch (Exception e) {
                debug.error("Error while parsing timeToLive in "
                + " ActionDecision:" + ttlString);
                Object [] args = { new Long(timeToLive) };
                throw new PolicyException(ResBundleUtils.rbName,
                        "invalid_time_to_live",
                    args,e);
            }
        }
        actionDecision.setTimeToLive(timeToLive);
        
        //process advices
        Map advices = new HashMap();
        nodeSet = XMLUtils.getChildNodes(actionDecisionNode,
        ActionDecision.ADVICES);
        if (nodeSet != null) {
            nodes = nodeSet.iterator();
            node = (Node) nodes.next();
            nodeSet = XMLUtils.getChildNodes(node, 
                PolicyUtils.ATTRIBUTE_VALUE_PAIR);
            if ( nodeSet != null ) {
                nodes = nodeSet.iterator();
                while ( nodes.hasNext() ) {
                    node = (Node) nodes.next();
                    String adviceName = PolicyUtils.getAttributeName(node);
                    if ( adviceName != null ) {
                        Set adviceMessages = PolicyUtils.
                            getAttributeValues(node);
                        advices.put(adviceName, adviceMessages);
                    }
                }
            }
        }
        actionDecision.setAdvices(advices);

        return actionDecision;
     }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        ActionDecision clone = new ActionDecision();
        clone.actionName = actionName;
        clone.timeToLive = timeToLive;

        if (values != null) {
            Iterator valuesIter = values.iterator();
            clone.values = new HashSet(values.size());
            while (valuesIter.hasNext()) {
                clone.values.add(valuesIter.next());
            }
        }

        if (advices != null) {
            Iterator adviceIter = advices.keySet().iterator();
            clone.advices = new HashMap(advices.size());

            while (adviceIter.hasNext()) {
                String key = (String) adviceIter.next();
                clone.advices.put(key, advices.get(key));
            }
        }

        return clone;
    }

    /**
     * Sets the client clock skew 
     * @param skew the time skew in milliseconds, serverTime - clientTime
     */
    public static void setClientClockSkew(long skew) {
        clientClockSkew = skew;
    }

}
