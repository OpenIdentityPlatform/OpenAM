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
 * $Id: AuthRoleCondition.java,v 1.4 2008/06/25 05:43:50 qcheng Exp $
 *
 */



package com.sun.identity.policy.plugins;

import java.util.*;

import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

/**
 * The class <code>AuthRoleCondition</code> is a plugin 
 * implementation of <code>Condition</code> interface.
 * This condition object provides the policy framework with the 
 * condition decision based on the user's authenticated role name.
 *
 */
public class AuthRoleCondition implements Condition {


    private static final Debug DEBUG
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    /** Key that is used to define the authenticated role name  
     *  for which the policy would apply.  The value should be
     *  a <code>Set</code> with only one element. The element should be a 
     *  String, the full DN of the role.
     */
    public static final String ROLE_NAME = "authRoleName";

    private Map properties;
    private String authRoleName;

    private static List propertyNames = new ArrayList(1);

    static {
        propertyNames.add(ROLE_NAME);
    }

    /** No argument constructor 
     */
    public AuthRoleCondition() {
    }

     /**
      * Returns a set of property names for the condition.
      *
      * @return set of property names
      */
     public List getPropertyNames()
     {
         return (new ArrayList(propertyNames));
     }
 
     /**
      * Returns the syntax for a property name
      * @see com.sun.identity.policy.Syntax
      *
      * @param property property name
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
      * @param property property name.
      * @param locale locale for which the property name must be customized.
      * @return display name for the property name.
      * @throws PolicyException
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
      * @param property property name
      * @return Set of valid values for the property.
      * @exception PolicyException if unable to get the valid values.
      */
     public Set getValidValues(String property) throws PolicyException
     {
         return (Collections.EMPTY_SET);
     }


    /**
     * Sets the properties of the condition.
     *  Evaluation of <code>ConditionDecision</code> is influenced by these
     *  properties.
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The properties should
     *         define value for the key ROLE_NAME. The value should
     *         be a Set with only one element. The element should be
     *         a String, the full DN of the role. Please note that
     *         properties is not cloned by the method.
     *
     *  @throws PolicyException if properties is null or does not contain
     *          value for the key ROLE_NAME or the value of the key is
     *          not a Set with one String element.
     */
    public void setProperties(Map properties) throws PolicyException {
        this.properties = (Map)((HashMap) properties);
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(
                ResBundleUtils.rbName, "properties_can_not_be_null_or_empty",
                null, null);
        }

        //Check if the key is valid
        Set keySet = properties.keySet();
        Iterator keys = keySet.iterator();
        String key = (String) keys.next();
        if ( !ROLE_NAME.equals(key) ) {
            String args[] = { ROLE_NAME };
            throw new PolicyException(
                ResBundleUtils.rbName, "attempt_to_set_invalid_property", 
                args, null);
        }

        // check if the value is valid
        Set roleNameSet = (Set) properties.get(ROLE_NAME);
        if (( roleNameSet == null ) || roleNameSet.isEmpty() 
            || ( roleNameSet.size() > 1 )) {
            String args[] = { ROLE_NAME };
            throw new PolicyException(
                ResBundleUtils.rbName, 
                "property_does_not_allow_empty_or_multiple_values", args, null);
        }

        Iterator iter = roleNameSet.iterator();
        authRoleName = (String)iter.next();
    }


    /** 
     * Returns properties of this condition.
     *
     * @return properties of this condition.
     */
    public Map getProperties() {
        return properties;
    } 


    /**
     * Gets the decision computed by this condition object.
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment map of key/value pairs.
     *        <code>AuthRoleCondition</code> does not use this parameter.
     *
     * @return the condition decision. The condition decision 
     *         encapsulates whether a policy applies for the request. 
     * User's roles are fetched from the SSOToken and if the role 
     * ROLE_NAME is one of the user roles in the token this condition returns 
     * true, false otherwise.
     *
     * Policy framework continues evaluating a policy only if it applies
     * to the request as indicated by the <code>ConditionDecision</code>. 
     * Otherwise, further evaluation of the policy is skipped. 
     *
     * @throws SSOException if the token is invalid
     * @throws PolicyException for any other abnormal condition
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws SSOException, PolicyException {

        if (token != null) {
            String userAuthRoleNames = token.getProperty("Role");
            if ( DEBUG.messageEnabled()) {
                DEBUG.message("At AuthRoleCondition.getConditionDecision(): "
                    +"userAuthRoleNames=" + userAuthRoleNames);
            }
     
            if (userAuthRoleNames == null) {
                return new ConditionDecision(false);
            }
    
            String userAuthRoleName = null;
            StringTokenizer st = new StringTokenizer(userAuthRoleNames, "|"); 
            while (st.hasMoreElements()) {
                userAuthRoleName = (String)st.nextElement(); 
                if (userAuthRoleName.equals(authRoleName)) {
                    return new ConditionDecision(true);
                }  
            }
        }
        return new ConditionDecision(false);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AuthRoleCondition theClone = null;
        try {
            theClone = (AuthRoleCondition) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
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

}
