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
 * $Id: AuthenticateToServiceCondition.java,v 1.7 2009/05/05 18:29:01 mrudul_uchil Exp $
 *
 */




package com.sun.identity.policy.plugins;

import com.sun.identity.authentication.util.AMAuthUtils;
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
 * The class <code>AuthenticateToServiceCondition</code>  is a plugin 
 * implementation  of <code>Condition</code> that lets you specify 
 * the service to which user should authenticate for the policy to apply
 *
 */
public class AuthenticateToServiceCondition implements Condition {

    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private Map properties;
    private String authenticateToService = null;
    private boolean realmEmpty = false;

    private static List propertyNames = new ArrayList(1);

    static {
        propertyNames.add(AUTHENTICATE_TO_SERVICE);
    }

    /** No argument constructor 
     */
    public AuthenticateToServiceCondition() {
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
     *         define value for the key <code>AUTHENTICATE_TO_SERVICE</code>. 
     *         The value
     *         should be a Set with only one element. The element should be 
     *         a <code>String</code>, the realm name for which 
     *         the user should authenticate
     *         for the policy to apply. Please note that 
     *         properties is not cloned by the method. 
     *
     *  @throws PolicyException if properties is <code>null</code> 
     *          or does not contain
     *          value for the key <code>AUTHENTICATE_TO_SERVICE</code> or 
     *          the value of the key is not a Set with one 
     *         <code>String</code> element 
     *
     *  @see #REQUEST_AUTHENTICATED_TO_SERVICES
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
     *        <code>AuthenticateToServiceCondition</code> looks for value of key
     *        <code>REQUEST_AUTHENTICATED_TO_SERVICES</code> in the map.  
     *        The value should be a <code>Set</code> with <code>String</code> 
     *        elements. 
     *        If the <code>env</code> parameter is <code>null</code> or does not
     *        define the value for 
     *       <code>REQUEST_AUTHENTICATED_TO_SERVICES</code>,  value for
     *        <code>REQUEST_AUTHENTICATED_TO_SERVICES</code> is computed 
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
     *         successful call to <code>setProperties(Map)</code> and/or the
     *         value of <code>REQUEST_AUTHENTICATED_TO_SERVICES</code> 
     *         could not be determined.
     * @throws SSOException if the token is invalid
     *
     * @see #setProperties(Map)
     * @see #AUTHENTICATE_TO_SERVICE
     * @see #REQUEST_AUTHENTICATED_TO_SERVICES
     * @see com.sun.identity.policy.ConditionDecision
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        boolean allowed = false;
        Set requestAuthnServices = new HashSet();
        if ( (env != null) 
                    && (env.get(REQUEST_AUTHENTICATED_TO_SERVICES) != null) ) {
            try {
                requestAuthnServices = (Set) env.get(
                    REQUEST_AUTHENTICATED_TO_SERVICES);
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At AuthenticateToServiceCondition."
                            + "getConditionDecision(): "
                            + "requestAuthnServices from request = " 
                            + requestAuthnServices);
                }
            } catch (ClassCastException e) {
                String args[] = { REQUEST_AUTHENTICATED_TO_SERVICES };
                throw new PolicyException(
                        ResBundleUtils.rbName, "property_is_not_a_Set", 
                        args, e);
            }
        } else {

            if (token != null) {
                Set authenticatedServices 
                        = AMAuthUtils.getRealmQualifiedAuthenticatedServices(
                        token);
                if (authenticatedServices != null) {
                    requestAuthnServices.addAll(authenticatedServices);
                }
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At AuthenticateToServiceCondition."
                            + "getConditionDecision(): "
                            + "requestAuthnServices from ssoToken = " 
                            + requestAuthnServices);
                }
            }
        }

        Set adviceMessages = new HashSet(1);
        if (requestAuthnServices.contains(authenticateToService)) {
            allowed = true;
        } else if (realmEmpty){
            for (Iterator iter = requestAuthnServices.iterator();
                    iter.hasNext(); ) {
                String requestAuthnService = (String)iter.next();
                String service = AMAuthUtils.getDataFromRealmQualifiedData(
                        requestAuthnService);
                if (authenticateToService.equals(service)) {
                    allowed = true;
                    break;
                }
            }
        }

        if (!allowed) {
            adviceMessages.add(authenticateToService);
            if ( DEBUG.messageEnabled()) {
                DEBUG.message("At AuthenticateToServiceCondition."
                        + "getConditionDecision():"
                        + "authenticateToService not satisfied = "
                        + authenticateToService);
            }
        }

        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At AuthenticateToServiceCondition."
                +"getConditionDecision():authenticateToService = " 
                + authenticateToService + "," + " requestAuthnServices = " 
                + requestAuthnServices + ", " + " allowed = " + allowed);
        }
        Map advices = new HashMap();
        if (!allowed) {
            advices.put(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE, 
                adviceMessages);
        }
        return new ConditionDecision(allowed, advices);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AuthenticateToServiceCondition theClone = null;
        try {
            theClone = (AuthenticateToServiceCondition) super.clone();
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
            theClone.authenticateToService = authenticateToService;
            theClone.realmEmpty = realmEmpty;
        }
        return theClone;
    }

    /**
     * Checks the properties set using setProperties() method for
     * validity like, not null, presence of AUTHENTICATE_TO_SERVICE property,
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
        if ( !keySet.contains(AUTHENTICATE_TO_SERVICE) ) {
            String args[] = { AUTHENTICATE_TO_SERVICE };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_value_not_defined", 
                    args, null);
        }

        //Check if all the keys are valid 
        Iterator keys = keySet.iterator();
        while ( keys.hasNext()) {
            String key = (String) keys.next();
            if ( !AUTHENTICATE_TO_SERVICE.equals(key) ) {
                String args[] = {key};
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "attempt_to_set_invalid_property ",
                        args, null);
            }
        }

        //validate AUTHENTICATE_TO_SERVICE
        Set authnToServiceSet = null;
        try {
            authnToServiceSet = (Set) properties.get(AUTHENTICATE_TO_SERVICE);
        } catch (ClassCastException e) {
            String args[] = { REQUEST_AUTHENTICATED_TO_SERVICES };
            throw new PolicyException(
                    ResBundleUtils.rbName, "property_is_not_a_Set", 
                    args, e);
        }
        if ( authnToServiceSet != null ) {
            validateAuthnToServices(authnToServiceSet);
        }

        return true;

    }

    /**
     * Validates the module chain names provided to the setProperties()
     * call for the AUTHENTICATE_TO_SERVICE key. Checks for null and throws
     * Exception if null or not a String.
     */

    private boolean validateAuthnToServices(Set authnToServiceSet) 
            throws PolicyException {
        if ( authnToServiceSet.isEmpty() ) {
            String args[] = { AUTHENTICATE_TO_SERVICE };
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "property_does_not_allow_empty_values", 
                    args, null);
        }
        authenticateToService = null;
        Iterator authnToServiceSetIter = authnToServiceSet.iterator();
        try {
            authenticateToService = (String) authnToServiceSetIter.next();
        } catch (ClassCastException e) {
            String args[] = { AUTHENTICATE_TO_SERVICE };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_is_not_a_String", 
                    args, null);
        }
        if (authenticateToService != null) {
            String realm = AMAuthUtils.getRealmFromRealmQualifiedData(
                    authenticateToService);
            if ((realm == null) || (realm.length() == 0)) {
                realmEmpty = true;
            }
        }
        return true;
    }

}
