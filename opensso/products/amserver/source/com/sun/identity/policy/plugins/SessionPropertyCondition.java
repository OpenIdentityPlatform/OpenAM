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
 * $Id: SessionPropertyCondition.java,v 1.4 2008/06/25 05:43:52 qcheng Exp $
 *
 */

package com.sun.identity.policy.plugins;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;


/**
 * The class <code>SessionPropertyCondition</code> is a plugin 
 * implementation of <code>Condition</code> interface.
 * This condition checks whether session properties contain at least 
 * one value of the each property listed in the <code>Condition</code>
 */

public class SessionPropertyCondition implements Condition {

    private static final boolean IGNORE_VALUE_CASE_DEFAULT = true;
    private static final String IGNORE_VALUE_CASE_FALSE_STRING = "false";
    private static final String DELIMITER = "|";
    
    private List propertyNames = Collections.EMPTY_LIST;
    private Map properties;

    private static final Debug debug 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private boolean ignoreValueCase = IGNORE_VALUE_CASE_DEFAULT;

    /** 
     * No argument constructor 
     */
    public SessionPropertyCondition() {
         propertyNames = new ArrayList();
    }

     /**
      * Returns a <code>List</code> of property names for the condition.
      *
      * @return <code>List</code> of property names
      */

     public List getPropertyNames()
     {
         return propertyNames;
     }
 
     /**
      * Returns the syntax for a property name
      * @see com.sun.identity.policy.Syntax
      *
      * @param property <code>String</code> representing property name
      *
      * @return <code>Syntax<code> for the property name
      */
     public Syntax getPropertySyntax(String property)
     {
         return (Syntax.ANY);
     }
      
     /**
      * Gets the display name for the property name.
      * The <code>locale</code> variable could be used by the
      * plugin to customize the display name for the given locale.
      * The <code>locale</code> variable could be <code>null</code>, in which 
      * case the plugin must use the default locale.
      *
      * @param property <code>String</code> representing property name
      * @param locale <code>Locale</code> for which the property name must be 
      * customized.
      * @return display name for the property name
      * @exception PolicyException if unable to get the display name.
      */
     public String getDisplayName(String property, Locale locale) 
       throws PolicyException
     {
         return property;
     }
 
     /**
      * Returns a set of valid values given the property name. This method
      * is called if the property Syntax is either the SINGLE_CHOICE or 
      * MULTIPLE_CHOICE.
      *
      * @param property <code>String</code> representing property name
      * @return <code>Set</code> of valid values for the property.
      * @exception PolicyException if unable to get  the <code>Set</code> of
      * valid values.
      */
     public Set getValidValues(String property) throws PolicyException
     {
         return (Collections.EMPTY_SET);
     }


    /** 
     *  Sets the properties of the condition.
     *  Evaluation of <code>ConditionDecision</code> is influenced by these 
     *  properties.
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The keys in properties should
     *         be <code>String</code> objects.   Value corresponding to each 
     *         key should be a Set of String(s). Please note that properties 
     *         is not cloned by the method.
     *
     *  @throws PolicyException if properties is null or empty
     */

    public void setProperties(Map properties) throws PolicyException {
        this.properties = (Map)((HashMap) properties);
        if ( (properties == null) ||  properties.isEmpty() ) {
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "properties_can_not_be_null_or_empty", null, null);
        }
        if (debug.messageEnabled()) {
            debug.message("SessionPropertyCondition."
                    + "setProperties():"
                    + "properties=" + properties);  
        }
        this.properties = properties;
        resetIgnoreValueCase();
    }


    /** 
     * Get properties of this condition.
     * @return unmodifiable <code>Map</code> view of the properties that govern 
     *         the evaluation of the condition.
     *         Please note that properties is not cloned before returning
     */
    public Map getProperties() {
        return Collections.unmodifiableMap(properties);
    } 


    /**
     * Gets the decision computed by this condition object.
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment <code>Map</code> of key/value 
     *            pairs. Not used by this Condition implementation.
     *
     * @return the condition decision. The condition decision 
     *         encapsulates whether a policy applies for the request. 
     *         The condition decision would imply <code>true</code>, if 
     *         the  session properties contain at least 
     *         one value of the each property listed in the Condition. 
     *         Otherwise, it would imply <code>false</code>
     *
     * Policy framework continues evaluating a <code>Policy</code> only if it 
     * applies to the request as indicated by the CondtionDecision. 
     * Otherwise, further evaluation of the policy is skipped. 
     *
     * @throws SSOException if the token is invalid
     * @throws PolicyException in unable to get the condition decision..
     */

    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        boolean allowed = true;
        if (debug.messageEnabled()) {
            debug.message("SessionPropertyCondition.getConditionDecision():"
                    + "entering, ignoreValueCase= " + ignoreValueCase);
        }
        if ((properties != null) && !properties.isEmpty()) {
            Set names = properties.keySet();
            namesIterLoop:
            for (Iterator namesIter = names.iterator(); 
                    namesIter.hasNext() && allowed;) {
                String name = (String)namesIter.next();
                Set values = (Set)properties.get(name);

                if (debug.messageEnabled()) {
                    debug.message("SessionPropertyCondition."
                            + "getConditionDecision():"
                            + "propertyName = " + name
                            + ",conditionValues = " + values);
                }

                if (name.equals(VALUE_CASE_INSENSITIVE)
                        || (values == null) || values.isEmpty()) {
                    continue namesIterLoop;
                }

                String sessionValue = token.getProperty(name);
                Set sessionValues = null;
                if ((sessionValue != null) 
                        && (sessionValue.indexOf(DELIMITER) != -1)) {
                    sessionValues = PolicyUtils.delimStringToSet(sessionValue, 
                            DELIMITER);
                }
                
                if (debug.messageEnabled()) {
                    debug.message("SessionPropertyCondition."
                            + "getConditionDecision():"
                            + ",sessionValue = " + sessionValue
                            + ",sessionValues = " + sessionValues);
                }

                if (sessionValue == null) {
                    allowed = false;
                    continue namesIterLoop;
                }

                if (sessionValues != null) { //session, multivalued
                    if (!ignoreValueCase) { //caseExact match
                        Iterator sessionValueIter = sessionValues.iterator();
                        while (sessionValueIter.hasNext()) {
                            String splitSessionValue 
                                    = (String)sessionValueIter.next();
                            if (values.contains(splitSessionValue)) {
                                continue namesIterLoop;
                            }
                        }
                    } else { //caseIgnore match
                        Iterator sessionValueIter = sessionValues.iterator();
                        sessionValueIterLoop:
                        while (sessionValueIter.hasNext()) {
                            String splitSessionValue 
                                    = (String)sessionValueIter.next();
                            for (Iterator valueIter = values.iterator(); 
                                    valueIter.hasNext();) {
                                String value = (String)valueIter.next();
                                if (splitSessionValue.equalsIgnoreCase(value)) {
                                    continue namesIterLoop;
                                }
                            }
                        }
                    } 
                } else if (!ignoreValueCase) { //single session value, caseExact
                    if (values.contains(sessionValue)) {
                        continue namesIterLoop;
                    }
                } else { //single session value, caseIgnore match
                    for (Iterator valueIter = values.iterator(); 
                            valueIter.hasNext();) {
                        String value = (String)valueIter.next();
                        if (sessionValue.equalsIgnoreCase(value)) {
                            continue namesIterLoop;
                        }
                    }
                } 
                allowed = false;

            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("SessionPropertyCondition."
                        + "getConditionDecision():"
                        + "no  parameter defined, "
                        + "defaulting allow = true ");
            }
            allowed = true;
        }

        if (debug.messageEnabled()) {
            debug.message("SessionPropertyCondition.getConditionDecision():"
                    + "allowed= " + allowed);
        }

        return new ConditionDecision(allowed);
    }


    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        SessionPropertyCondition theClone = null;
        try {
            theClone = (SessionPropertyCondition)super.clone();
        } catch (CloneNotSupportedException e) {
            //this should never happen
            throw new InternalError();
        }

        if (properties != null) {
            theClone.properties = new HashMap();
            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                Object o = it.next();
                Set values = new HashSet();
                values.addAll((Set) properties.get(o));
                theClone.properties.put(o, values);
            }
        }
        return theClone;
    }

    /**
     * Resets the value of property VALUE_CASE_INSENSITIVE in <code>properties
     * </code>  Map
     */
    private void resetIgnoreValueCase() {
        if (properties != null) {
            Set values = (Set)properties.get(VALUE_CASE_INSENSITIVE);
            if ((values != null) && !values.isEmpty()){
                for (Iterator iter = values.iterator(); iter.hasNext();) {
                    String value = (String)iter.next();
                    if (IGNORE_VALUE_CASE_FALSE_STRING.equalsIgnoreCase(
                                value.trim())) {
                        ignoreValueCase = false;
                    } else {
                        ignoreValueCase = IGNORE_VALUE_CASE_DEFAULT;
                    }
                }
            } else {
                ignoreValueCase = IGNORE_VALUE_CASE_DEFAULT;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("SessionPropertyCondition.resetIgnoreValueCase():"
                    + "ignoreValueCase= " + ignoreValueCase);
        }
    }
}
