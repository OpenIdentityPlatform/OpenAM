/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SampleCondition.java,v 1.2 2008/09/05 01:45:02 dillidorai Exp $
 *
 */

package com.sun.identity.samples.policy;

import java.util.*;

import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;


/**
 * The class <code>SampleCondition</code> is a plugin 
 * implementation of <code>Condition</code> interface.
 * This condition object provides the policy framework with the 
 * condition decision based on the length of the user's name.
 */

public class SampleCondition implements Condition {

    /** Key that is used to define the minimum of the user name length  
     *  for which the policy would apply.  The value should be
     *  a Set with only one element. The element should be a 
     *  String, parsable as an integer.
     */

    public static final String USER_NAME_LENGTH = "userNameLength";

    private List propertyNames;
    private Map properties;
    private int nameLength;

    /** No argument constructor 
     */
    public SampleCondition() {
         propertyNames = new ArrayList();
         propertyNames.add(USER_NAME_LENGTH);
    }

     /**
      * Returns a set of property names for the condition.
      *
      * @return set of property names
      */

     public List getPropertyNames()
     {
         return propertyNames;
     }
 
     /**
      * Returns the syntax for a property name
      * @see com.sun.identity.policy.Syntax
      *
      * @param String property name
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
      * @param String property name
      * @param Locale locale for which the property name must be customized
      * @return display name for the property name
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
      * @param String property name
      * @return Set of valid values for the property.
      * @exception PolicyException if unable to get the Syntax.
      */
     public Set getValidValues(String property) throws PolicyException
     {
         return (Collections.EMPTY_SET);
     }


    /** Sets the properties of the condition.
     *  Evaluation of ConditionDecision is influenced by these properties.
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The properties should
     *         define value for the key USER_NAME_LENGTH. The value should
     *         be a Set with only one element. The element should be
     *         a String, parsable as an integer. Please note that
     *         properties is not cloned by the method.
     *
     *  @throws PolicyException if properties is null or does not contain
     *          value for the key USER_NAME_LENGTH or the value of the key is
     *          not a Set with one String element that is parsable as
     *          an integer.
     */

    public void setProperties(Map properties) throws PolicyException {
        this.properties = (Map)((HashMap) properties);
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException("properties can not be null or empty");
        }

        //Check if the key is valid
        Set keySet = properties.keySet();
        Iterator keys = keySet.iterator();
        String key = (String) keys.next();
        if ( !USER_NAME_LENGTH.equals(key) ) {
            throw new PolicyException(
                "property " + USER_NAME_LENGTH + " is not defined");
        }

        // check if the value is valid
        Set nameLengthSet = (Set) properties.get(USER_NAME_LENGTH);
        if (( nameLengthSet == null ) || nameLengthSet.isEmpty() 
            || ( nameLengthSet.size() > 1 )) {
            throw new PolicyException(
                "property value is not defined or invalid");
        }

        Iterator nameLengths = nameLengthSet.iterator();
        String nameLengthString = null;
        nameLengthString = (String) nameLengths.next();
        try {
            nameLength = Integer.parseInt(nameLengthString);
        } catch (Exception e) {
            throw new PolicyException("name length value is not an integer");
        }
    }


    /** Get properties of this condition.
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
     *        SampleCondition doesn't use this parameter.
     *
     * @return the condition decision. The condition decision 
     *         encapsulates whether a policy applies for the request. 
     *
     * Policy framework continues evaluating a policy only if it 
     * applies to the request as indicated by the CondtionDecision. 
     * Otherwise, further evaluation of the policy is skipped. 
     *
     * @throws SSOException if the token is invalid
     */

    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
	boolean allowed = false;

        String userDN = token.getPrincipal().getName();
        // user DN is in the format like "uid=username,ou=people,dc=example,dc=com"
        int beginIndex = userDN.indexOf("=");
        int endIndex = userDN.indexOf(",");
        if (beginIndex >= endIndex) {
            throw (new PolicyException("invalid user DN"));
        }

        String userName = userDN.substring(beginIndex+1, endIndex);
        if (userName.length() >= nameLength) {
            allowed = true;
        }

	return new ConditionDecision(allowed);
    }


    public Object clone() {
	Object theClone = null;
	try {
	    theClone = super.clone();
	} catch (CloneNotSupportedException e) {
            throw new InternalError();
	}
	return theClone;
    }

}
