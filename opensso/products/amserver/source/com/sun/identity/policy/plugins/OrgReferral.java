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
 * $Id: OrgReferral.java,v 1.4 2008/06/25 05:43:51 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;

import com.sun.identity.policy.*;
import com.sun.identity.policy.interfaces.Referral;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.Cache;
import com.sun.identity.shared.debug.Debug;
import java.util.*;

/** An abstract class to facilitate policy referrals based on 
 *  organization
 */
abstract public class OrgReferral implements Referral {

    protected static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);
    protected final static String CAN_NOT_GET_VALUES_FOR_REFERRAL 
        = "can_not_get_values_for_referral";

    private Set _values;
    protected Map _configurationMap;
    protected String _orgName;
    protected Cache cachedPolicyEvaluators = new Cache(50);

    /** No argument constructor */
    public OrgReferral() {
    }

    /**Initializes the referral with a map of Configuration parameters
     * @param configurationMap a map containing configuration 
     *        information. Each key of the map is a configuration
     *        parameter. Each value of the key would be a set of values
     *        for the parameter. The map is cloned and a reference to the 
     *        clone is stored in the referral
     */
    public void initialize(Map configurationMap) {
        _configurationMap = configurationMap;
    }

    /**Sets the values of this referral.
     * @param values a set of values for this referral
     *        Each element of the set has to be a String
     * @exception InvalidNameException if any value passed in the 
     * values is invalid
     */
    public void setValues(Set values) throws InvalidNameException {
        if ((values.isEmpty()) || (values.size() > 1)) {
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "invalid_organization", null,
                "", PolicyException.ORGANIZATION));
        }
        _values = values;
        Iterator items = values.iterator();
        _orgName = (String) items.next();
    }

    /**Gets the values of this referral 
     * @return the values of this referral
     *                Each element of the set would be a String
     */
    public Set getValues() {
        return _values;
    }

    /**
     * Returns the display name for the value for the given locale.
     * For all the valid values obtained through the methods
     * <code>getValidValues</code> this method must be called
     * by GUI and CLI to get the corresponding display name.
     * The <code>locale</code> variable could be used by the
     * plugin to customize
     * the display name for the given locale.
     * The <code>locale</code> variable
     * could be <code>null</code>, in which case the plugin must
     * use the default locale (most probabily en_US).
     * This method returns only the display name and should not
     * be used for the method <code>setValues</code>.
     * Alternatively, if the plugin does not have to localize
     * the value, it can just return the <code>value</code> as is.
     *
     * @param value one of the valid value for the plugin
     * @param locale locale for which the display name must be customized
     *
     * @exception NameNotFoundException if the given <code>value</code>
     * is not one of the valid values for the plugin
     */
    public String getDisplayNameForValue(String value, Locale locale)
        throws NameNotFoundException {
        String displayName = null;
        try {
            displayName = PolicyUtils.getDisplayName(value);
        } catch (Exception e) {
            String[] objs = { value };
            throw (new NameNotFoundException(ResBundleUtils.rbName,
                "role_name_not_present", objs, value,
                PolicyException.USER_COLLECTION));
        }
        return displayName;
    }

    /**
     * Gets the valid values for this referral 
     * @param token SSOToken
     * @return <code>ValidValues</code> object
     * @exception SSOException if <code>SSOToken></code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public abstract ValidValues getValidValues(SSOToken token) 
            throws SSOException, PolicyException;

    /**
     * Gets the valid values for this referral matching a pattern
     * @param token SSOToken
     * @param pattern a pattern to match against the value
     * @return </code>ValidValues</code> object
     * @exception SSOException if <code>SSOToken></code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public abstract ValidValues getValidValues(SSOToken token, String pattern)
            throws SSOException, PolicyException;

    /**
     * Returns the syntax of the values the <code>OrgReferral</code> 
     * @see com.sun.identity.policy.Syntax
     * @param token the <code>SSOToken</code> that will be used
     * to determine the syntax
     * @return set of of valid names for the referral.
     * @exception SSOException if <code>SSOToken></code> is not valid
     */
    public Syntax getValueSyntax(SSOToken token)
            throws SSOException, PolicyException {
        Syntax syntax = Syntax.NONE;
        ValidValues values = getValidValues(token);
        Set validValues = values.getSearchResults();
        if ( (validValues == null) || (validValues.isEmpty()) ) {
            syntax = Syntax.NONE;
        } else {
            syntax = Syntax.SINGLE_CHOICE;
        }
        return syntax;
    }

    /**Gets the name of the ReferralType 
     * @return name of the ReferralType representing this referral
     */
    abstract public String getReferralTypeName();

    /**Gets policy evaluation results 
     * @param token SSOToken
     * @param resourceType resource type
     * @param resourceName name of the resource 
     * @param actionNames a set of action names
     * @param envParameters a map of enivronment parameters.
     *        Each key is an environment parameter name.
     *        Each value is a set of values for the parameter.
     * @return policy decision
     * @throws SSOException
         * @throws PolicyException
     */
    public PolicyDecision getPolicyDecision(SSOToken token, 
            String resourceType, String resourceName, Set actionNames, 
            Map envParameters) throws SSOException, PolicyException 
    {
        /*
         Currently this method is not invoked by policy framework.
         OrgReferrals are treated specially by the framework to avoid
         circular referral problems. Policy frame work gets the org name 
         from the referral and creates policy evaluator and continues 
         evaluation with the policy evaluator.

         Obtain the policy evaluator, create if not present
         Also, since it is of single choice, there could be only one
         organization name.
         */
        PolicyEvaluator policyEvaluator = null;
        StringBuilder cacheNameBuffer = new StringBuilder();
        String cacheName = cacheNameBuffer.append(_orgName)
            .append(resourceType).toString();
        if ((policyEvaluator = (PolicyEvaluator) cachedPolicyEvaluators
                .get(cacheName)) == null) {
            policyEvaluator = new PolicyEvaluator(_orgName, resourceType);
            cachedPolicyEvaluators.put(cacheName, policyEvaluator);
        }
        return (policyEvaluator.getPolicyDecision(token, resourceName, 
            actionNames, envParameters));
    }


    /**Gets resource names that are exact matches, sub resources or 
     * wild card matches of argument resource name.
     * To determine whether to include a
     * resource name of a resource,  argument resource name and  policy 
     * resource name are compared treating wild characters in the policy 
     * resource name as wild. If the comparsion resulted in EXACT_MATCH,
     * WILD_CARD_MACTH or SUB_RESOURCE_MACTH, the resource result would be
     * included.
     *
     * @param token sso token
     * @param serviceTypeName service type name
     * @param resourceName resource name
     * @return names of sub resources for the given resourceName.
     *         The return value also includes the resourceName.
     *
     * @exception PolicyException if unable to get the Set of
     * resource names.
     * @exception SSOException is the token is invalid.
     *
     * @see com.sun.identity.policy.ResourceMatch#EXACT_MATCH
     * @see com.sun.identity.policy.ResourceMatch#SUB_RESOURCE_MATCH
     * @see com.sun.identity.policy.ResourceMatch#WILDCARD_MATCH
     *
     */
    public Set getResourceNames(SSOToken token, String serviceTypeName, 
            String resourceName) throws PolicyException, SSOException {
        Set resourceNames = null;
        PolicyEvaluator policyEvaluator = null;
        StringBuilder cacheNameBuffer = new StringBuilder();
        String cacheName = cacheNameBuffer.append(_orgName)
                .append(serviceTypeName).toString();
        if ((policyEvaluator = (PolicyEvaluator) cachedPolicyEvaluators
               .get(cacheName)) == null) {
            policyEvaluator = new PolicyEvaluator(_orgName, 
                        serviceTypeName);
            cachedPolicyEvaluators.put(cacheName, policyEvaluator);
        }
        resourceNames =  policyEvaluator.getResourceNames(token,  
                resourceName, true);
        return resourceNames;
    }

}
