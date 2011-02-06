/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PrivilegeCondition.java,v 1.1 2009/08/19 05:40:38 veiming Exp $
 */

package com.sun.identity.policy.plugins;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.ResBundleUtils;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;


/**
 * A generic condition mapper which map entitlement condition to OpenSSO
 * condition.
 */
public class PrivilegeCondition implements Condition, Cloneable {
    private static List propertyNames = new ArrayList(1);
    public static final String STATE = "privilegeConditionState";
    private String state;

    static {
        propertyNames.add(STATE);
    }

    /**
     * Returns a list of property names.
     *
     * @return a list of property names.
     */
    public List getPropertyNames() {
        return (new ArrayList(propertyNames));
    }

    /**
     * Return the syntax for displaying the property value.
     *
     * @param property Property name.
     * @return syntax for displaying the property value.
     */
    public Syntax getPropertySyntax(String property) {
        return Syntax.ANY;
    }

    /**
     * Returns the display name of a property.
     *
     * @param property Property name.
     * @param locale Locale.
     * @return the display name of a property.
     * @throws PolicyException if display name cannot be provided.
     */
    public String getDisplayName(String property, Locale locale)
        throws PolicyException {
        ResourceBundle rb = AMResourceBundleCache.getInstance().getResBundle(
            ResBundleUtils.rbName, locale);
        return com.sun.identity.shared.locale.Locale.getString(rb, property);
    }

    /**
     * Returns the valid values of a property.
     *
     * @param property Property Name.
     * @return valid values of a property.
     * @throws PolicyException if valid values cannot be provided.
     */
    public Set getValidValues(String property) throws PolicyException {
        return Collections.EMPTY_SET;
    }

    /**
     * Sets the property values to this object.
     *
     * @param properties Properties to be set.
     * @throws PolicyException if property cannot be set.
     */
    public void setProperties(Map properties) throws PolicyException {
        if ((properties != null) && !properties.isEmpty()) {
            String k = (String)properties.keySet().iterator().next();
            Set set = (Set)properties.get(k);
            String v = (String)set.iterator().next();
            state = k + "=" + v;
        }
    }

    /**
     * Returns the property values.
     *
     * @return Property values.
     */
    public Map getProperties() {
        Map map = new HashMap(2);
        if (state != null) {
            int idx = state.indexOf("=");
            Set set = new HashSet(2);
            set.add(state.substring(idx+1));
            map.put(state.substring(0, idx), set);
        }
        return map;
    }

    /**
     * Returns condition decision. Returing false.
     *
     * @param token Single sign on token of the subject.
     * @param env Environment map.
     * @return <code>false</code>.
     * @throws PolicyException if decision cannot be provided.
     * @throws com.iplanet.sso.SSOException if single sign on token is invalid
     *         or has expired.
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env)
        throws PolicyException, SSOException {
        return new ConditionDecision(false);
    }

    /**
     * Returns a clone of this object.
     * 
     * @return clone of this object.
     */
    @Override
    public Object clone() {
        PrivilegeCondition theClone = null;
        try {
            theClone = (PrivilegeCondition)super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        theClone.state = state;
        return theClone;
    }
}
