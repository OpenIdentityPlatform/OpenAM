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
 * $Id: Referral.java,v 1.2 2008/06/25 05:43:47 qcheng Exp $
 *
 */



package com.sun.identity.policy.interfaces;

import com.sun.identity.policy.*;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import java.util.*;

/**
 * Interface to facilitate delegating policy evaluation 
 * There would be many implementations with different policy delegation 
 * mechanisms such as delegating to peer organizations only 
 * or delegating to sub organizations only. 
 * @supported.all.api
 */
public interface Referral {
    /**
     * Initializes the Referral with a <code>Map</code>
     * @param configurationMap a <code>Map</code> containing configuration 
     *        information. Each key of the <code>Map</code> is a configuration
     *        parameter. Each value of the key would be a <code>Set</code> of 
     *        values for the parameter. The <code>Map</code> is cloned and a 
     *        reference to the clone is stored in the referral
     */
    void initialize(Map configurationMap);

    /**
     * Sets the values of this referral 
     * @param values <code>Set</code> of values for this referral.
     *               Each element of the <code>Set</code> has to be a 
     *               <code>String</code>
     * @throws InvalidNameException if any value passed in values is 
     *         not valid
     */
    void setValues(Set values) throws InvalidNameException;

    /**
     * Gets the values of this referral 
     * @return the values of this referral
     *     Each element of the returned <code>Set</code> is a 
     *     <code>String</code>.
     */
    Set getValues();

    /**
     * 
     * Returns the display name for the value for the given locale.
     * For all the valid values obtained through the methods
     * <code>getValidValues</code> this method must be called
     * by web and command line interfaces to get the corresponding display name.
     * The <code>locale</code> variable could be used by the
     * plugin to customize
     * the display name for the given locale.
     * The <code>locale</code> variable
     * could be <code>null</code>, in which case the plugin must
     * use the default locale (most probably <code>en_US</code>).
     * This method returns only the display name and should not
     * be used for the method <code>setValues</code>.
     * Alternatively, if the plugin does not have to localize
     * the value, it can just return the <code>value</code> as is.
     *
     * @param value one of the valid value for the plugin
     * @param locale locale for which the display name must be customized
     * @return the display name for the value for the given locale.
     * @exception NameNotFoundException if the given <code>value</code>
     * is not one of the valid values for the plugin
     */
    public String getDisplayNameForValue(String value, Locale locale)
        throws NameNotFoundException;

    /**
     * Gets the valid values for this referral 
     * @param token <code>SSOToken</code>
     * @return <code>ValidValues</code> object
     * @throws SSOException, PolicyException
     */
    ValidValues getValidValues(SSOToken token) 
        throws SSOException, PolicyException;

    /**
     * Gets the valid values for this referral 
     * matching a pattern
     * @param token <code>SSOToken</code>
     * @param pattern a pattern to match against the value
     * @return <code>ValidValues</code> object
     * @throws SSOException, PolicyException
     */
    ValidValues getValidValues(SSOToken token, String pattern)
        throws SSOException, PolicyException;

    /**
     * Gets the syntax for the value 
     * @param token <code>SSOToken</code>
     * @see com.sun.identity.policy.Syntax
     */
    Syntax getValueSyntax(SSOToken token)
            throws SSOException, PolicyException;

    /**
     * Gets the name of the Referral Type 
     * @return name of the Referral Type representing this referral
     */
    String getReferralTypeName();

    /**
     * Gets policy results 
     * @param token SSOToken
     * @param resourceType resource Type
     * @param resourceName name of the resource
     * @param actionNames a set of action names
     * @param envParameters a map of enivronment parameters.
     *        Each key is an environment parameter name.
     *        Each value is a set of values for the parameter.
     * @return policy decision
     *
     * @throws PolicyException
     * @throws SSOException
     */
    PolicyDecision getPolicyDecision(SSOToken token, String resourceType,
        String resourceName, Set actionNames, Map envParameters
    ) throws SSOException, PolicyException;

    /**
     * Gets resource names that are exact matches, sub resources or 
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
     * @throws PolicyException
     * @throws SSOException
     *
     * @see com.sun.identity.policy.ResourceMatch#EXACT_MATCH
     * @see com.sun.identity.policy.ResourceMatch#SUB_RESOURCE_MATCH
     * @see com.sun.identity.policy.ResourceMatch#WILDCARD_MATCH
     *
     */
    Set getResourceNames(SSOToken token, String serviceTypeName, 
        String resourceName) throws PolicyException, SSOException;
}
