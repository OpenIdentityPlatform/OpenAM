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
 * $Id: PrivilegeSubject.java,v 1.1 2009/08/19 05:40:38 veiming Exp $
 */

package com.sun.identity.policy.plugins;

import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.Locale;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.policy.Syntax;
import java.util.HashSet;

/**
 * A generic subject mapper which map entitlement subject to OpenSSO
 * subject.
 */
public class PrivilegeSubject implements Subject, Cloneable {
    private String state;
    private static ValidValues validValues = 
                new ValidValues(ValidValues.SUCCESS, Collections.EMPTY_SET);

    /**
     * Default Constructor
     */
    public PrivilegeSubject() {
        // do nothing
    }

    /**
     * Initialize the subject. No properties are required for this
     * subject.
     * @param configParams configuration information
     */
    public void initialize(Map configParams) {
        // do nothing
    }

    /**
     * Return the syntax for displaying the property value.
     *
     * @param token SSO token
     * @return syntax for displaying the property value.
     */
    public Syntax getValueSyntax(SSOToken token) {
        return Syntax.ANY;
    }

    /**
     * Returns an empty list as possible values. 
     *
     * @param token the <code>SSOToken</code>
     *
     * @return <code>ValidValues</code> object with empty list.
     *
     */
    public ValidValues getValidValues(SSOToken token) {
        return validValues;
    }

    /**
     * Returns an empty list as possible values. 
     *
     * @param token the <code>SSOToken</code>
     * @param pattern the pattern to match in valid values. Ignored for this 
     * subject
     *
     * @return <code>ValidValues</code> object with empty list.
     *
     */
    public ValidValues getValidValues(SSOToken token, String pattern) {
        return new ValidValues(ValidValues.SUCCESS, Collections.EMPTY_SET);
    }

    /**
     * Returns the display name for property value.
     *
     * @param value Property value.
     * @param locale Locale.
     * @return the display name for property value.
     */
    public String getDisplayNameForValue(String value, Locale locale) {
        // does nothing
        return(value);
    }

    /**
     * Returns values
     *
     * @return values.
     */
    public Set getValues() {
        Set<String> values = new HashSet<String>();
        values.add(state);
        return values;
    }

    /**
     * Set values.
     *
     * @param values Values.
     */
    public void setValues(Set values) {
        state = ((values != null) && !values.isEmpty()) ?
            (String)values.iterator().next() : null;
    }

    
    public boolean isMember(SSOToken token) throws SSOException {
        return false;
    }


    /**
     * Indicates whether some other object is "equal to" this one.
     * @param o another object that will be compared with this one
     * @return <code>true</code> if equal; <code>false</code>
     * otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PrivilegeSubject)) {
            return false;
        }

        PrivilegeSubject sbj = (PrivilegeSubject)o;
        if ((state == null) && (sbj.state == null)) {
            return true;
        }
        if ((state == null) && (sbj.state != null)) {
            return false;
        }
        if ((state != null) && (sbj.state == null)) {
            return false;
        }

        return state.equals(sbj.state);
    }

    
    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    @Override
    public Object clone() {
        PrivilegeSubject cloned = null;
        try {
            cloned = (PrivilegeSubject)super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (state != null) {
            cloned.state = state;
        }
        return cloned;
    }

    /**
    * Return a hash code for this <code>AuthenticatedUsers</code>.
    *
    * @return a hash code for this <code>AuthenticatedUsers</code> object.
    */
    @Override
    public int hashCode() {
        return (state != null) ? state.hashCode() : super.hashCode();
    }
}
