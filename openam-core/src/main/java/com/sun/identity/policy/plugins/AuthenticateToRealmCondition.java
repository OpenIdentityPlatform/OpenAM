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
 * $Id: AuthenticateToRealmCondition.java,v 1.6 2009/06/19 22:53:42 mrudul_uchil Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */

package com.sun.identity.policy.plugins;

import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Collections;

/**
 * The class <code>AuthenticateToRealmCondition</code>  is a plugin 
 * implementation  of <code>Condition</code> that lets you specify 
 * the realm to which user should authenticate for the policy to apply
 *
 */
public class AuthenticateToRealmCondition implements Condition {

    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private static final boolean debugMessageEnabled 
            = DEBUG.messageEnabled(); 

    private Map properties;
    private String authenticateToRealm = null;

    private static List propertyNames = new ArrayList(1);

    static {
        propertyNames.add(AUTHENTICATE_TO_REALM);
    }

    /** No argument constructor 
     */
    public AuthenticateToRealmCondition() {
    }

    /**
     * Returns a list of property names for the condition.
     *
     * @return list of property names
     */
    public List getPropertyNames() {
        return (new ArrayList(propertyNames));
    }

    /**
     * Returns the syntax for a property name
     * @see com.sun.identity.policy.Syntax
     *
     * @param property property name
     *
     * @return <code>Syntax</code> for the property name
     */
    public Syntax getPropertySyntax(String property) {
        return Syntax.NONE; 
    }

    /**
     * Returns the display name for the property name.
     * The <code>locale</code> variable could be used by the plugin to
     * customize the display name for the given locale.
     * The <code>locale</code> variable could be <code>null</code>, in which
     * case the plugin must use the default locale.
     *
     * @param property property name
     * @param locale locale for which the property name must be customized
     * @return display name for the property name
     * @throws PolicyException  if unable to get display name
     */
    public String getDisplayName(String property, Locale locale)
       throws PolicyException {
        return property;
    }

    /**
     * Returns a set of valid values given the property name. This method
     * is called if the property Syntax is either the SINGLE_CHOICE or
     * MULTIPLE_CHOICE.
     *
     * @param property property name
     * @return Set of valid values for the property.
     * @throws PolicyException if unable to get valid values
     */
    public Set getValidValues(String property) throws PolicyException {
        return Collections.EMPTY_SET;
    }

    /** Sets the properties of the condition.  
     *  Evaluation of <code>ConditionDecision</code> is influenced by these
     *  properties.
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The properties should 
     *         define value for the key <code>AUTHENTICATE_TO_REALM</code>. 
     *         The value
     *         should be a Set with only one element. The element should be 
     *         a <code>String</code>, the realm name for which 
     *         the user should authenticate
     *         for the policy to apply. Please note that 
     *         properties is not cloned by the method. 
     *
     *  @throws PolicyException if properties is <code>null</code> 
     *          or does not contain
     *          value for the key <code>AUTHENTICATE_TO_REALM</code> or 
     *          the value of the key is not a Set with one 
     *         <code>String</code> element 
     *
     *  @see #REQUEST_AUTHENTICATED_TO_REALMS
     */
    public void setProperties(Map properties) throws PolicyException {
        this.properties = properties;
        validateProperties();
    }

    /** Returns the properties of the condition.  
     *  @return  unmodifiable map view of properties that govern the 
     *           evaluation of  the condition.
     *           Please note that properties is  not cloned before returning
     *  @see #setProperties(Map)
     */
    public Map getProperties() {
        return (properties == null)
                ? null : Collections.unmodifiableMap(properties);
    }

    /**
     * Returns the decision computed by this condition object, based on the 
     * map of environment parameters 
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment map of key/value pairs
     *        <code>AuthenticateToRealmCondition</code> looks for value of key
     *        <code>REQUEST_AUTHENTICATED_TO_REALMS</code> in the map.  
     *        The value should be a <code>Set</code> with <code>String</code> 
     *        elements. 
     *        If the <code>env</code> parameter is <code>null</code> or does not
     *        define the value for 
     *       <code>REQUEST_AUTHENTICATED_TO_REALMS</code>,  value for
     *        <code>REQUEST_AUTHENTICATED_TO_REALMS</code> is computed 
     *        from sso token.
     *
     * @return the condition decision. The condition decision encapsulates
     *         whether a policy applies for the request and advice messages
     *         generated by the condition.  
     *
     * Policy framework continues evaluating a  policy only if it applies 
     * to the request  as indicated by the <code>ConditionDecision</code>. 
     * Otherwise, further evaluation of the policy is skipped. 
     * However, the advice messages encapsulated in the 
     * <code>ConditionDecision</code> are aggregated and passed up, encapsulated
     * in the policy  decision.
     *
     * @throws PolicyException if the condition has not been initialized with a
     *        successful call to <code>setProperties(Map)</code> and/or the
     *        value of <code>REQUEST_AUTHENTICATED_TO_REALMS</code> could not be
     *        determined.
     * @throws SSOException if the token is invalid
     *
     * @see #setProperties(Map)
     * @see #AUTHENTICATE_TO_REALM
     * @see #REQUEST_AUTHENTICATED_TO_REALMS
     * @see com.sun.identity.policy.ConditionDecision
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {

        // We don't care about case of the realm when doing the comparison so use a CaseInsensitiveHashSet
        Set requestAuthnRealms = new CaseInsensitiveHashSet();
        if ( (env != null) 
                    && (env.get(REQUEST_AUTHENTICATED_TO_REALMS) != null) ) {
            try {
                requestAuthnRealms.addAll((Set) env.get(REQUEST_AUTHENTICATED_TO_REALMS));
                if ( debugMessageEnabled) {
                    DEBUG.message("At AuthenticateToRealmCondition."
                            + "getConditionDecision(): "
                            + "requestAuthnRealms, from request = " 
                            + requestAuthnRealms);
                }
            } catch (ClassCastException e) {
                String args[] = { REQUEST_AUTHENTICATED_TO_REALMS };
                throw new PolicyException(
                        ResBundleUtils.rbName, "property_is_not_a_Set", 
                        args, e);
            }
        } else {

            if (token != null) {
                Set authenticatedRealms 
                        = AMAuthUtils.getAuthenticatedRealms(token);
                if (authenticatedRealms != null) {
                    requestAuthnRealms.addAll(authenticatedRealms);
                }
                if ( debugMessageEnabled) {
                    DEBUG.message("At AuthenticateToRealmCondition."
                            + "getConditionDecision(): "
                            + "requestAuthnRealms, from ssoToken = " 
                            + requestAuthnRealms);
                }
            }
        }

        boolean allowed = true;
        Set adviceMessages = new HashSet(1);
        if (!requestAuthnRealms.contains(authenticateToRealm)) {
            allowed = false;
            adviceMessages.add(authenticateToRealm);
            if ( debugMessageEnabled) {
                DEBUG.message("At AuthenticateToRealmCondition."
                        + "getConditionDecision():"
                        + "authenticateToRealm not satisfied = "
                        + authenticateToRealm);
            }
        }

        if ( debugMessageEnabled) {
            DEBUG.message("At AuthenticateToRealmCondition."
                +"getConditionDecision():authenticateToRealm = " 
                + authenticateToRealm + "," +"requestAuthnRealms = " 
                + requestAuthnRealms + ", " + " allowed = " + allowed);
        }
        Map advices = new HashMap();
        if (!allowed) {
            advices.put(AUTHENTICATE_TO_REALM_CONDITION_ADVICE, adviceMessages);
        }
        return new ConditionDecision(allowed, advices);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AuthenticateToRealmCondition theClone = null;
        try {
            theClone = (AuthenticateToRealmCondition) super.clone();
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
            theClone.authenticateToRealm = authenticateToRealm;
        }
        return theClone;
    }

    /**
     * Checks the properties set using setProperties() method for
     * validity like, not null, presence of AUTHENTICATE_TO_REALM property,
     * and no other invalid property.
     */

    private boolean validateProperties() throws PolicyException {
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "properties_can_not_be_null_or_empty", null, null);
        }

        Set keySet = properties.keySet();
        //Check if the required key(s) are defined
        if ( !keySet.contains(AUTHENTICATE_TO_REALM) ) {
            String args[] = { AUTHENTICATE_TO_REALM };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_value_not_defined", 
                    args, null);
        }

        //Check if all the keys are valid 
        Iterator keys = keySet.iterator();
        while ( keys.hasNext()) {
            String key = (String) keys.next();
            if ( !AUTHENTICATE_TO_REALM.equals(key) ) {
                String args[] = {key};
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "attempt_to_set_invalid_property ",
                        args, null);
            }
        }

        //validate AUTHENTICATE_TO_REALM
        Set authnToRealmSet = null;
        try {
            authnToRealmSet = (Set) properties.get(AUTHENTICATE_TO_REALM);
        } catch (ClassCastException e) {
            String args[] = { REQUEST_AUTHENTICATED_TO_REALMS };
            throw new PolicyException(
                    ResBundleUtils.rbName, "property_is_not_a_Set", 
                    args, e);
        }
        if ( authnToRealmSet != null ) {
            validateAuthnToRealms(authnToRealmSet);
        }

        return true;

    }

    /**
     * Validates the realm names provided to the setProperties()
     * call for the AUTHENTICATE_TO_REALM key. Checks for null and throws
     * Exception if null or not a String.
     */

    private boolean validateAuthnToRealms(Set authnToRealmSet) 
            throws PolicyException {
        if ( authnToRealmSet.isEmpty() ) {
            String args[] = { AUTHENTICATE_TO_REALM };
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "property_does_not_allow_empty_values", 
                    args, null);
        }
        authenticateToRealm = null;
        Iterator authnToRealmSetIter = authnToRealmSet.iterator();
        try {
            authenticateToRealm = (String) authnToRealmSetIter.next();
        } catch (ClassCastException e) {
            String args[] = { AUTHENTICATE_TO_REALM };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_is_not_a_String", 
                    args, null);
        }
        return true;
    }

}
