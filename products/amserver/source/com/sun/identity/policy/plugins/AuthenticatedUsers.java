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
 * $Id: AuthenticatedUsers.java,v 1.2 2008/06/25 05:43:51 qcheng Exp $
 *
 */




package com.sun.identity.policy.plugins;

import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.Locale;

import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.policy.Syntax;

/**
 * This subject applies to all users with valid <code>SSOToken</code>.
 */
public class AuthenticatedUsers implements Subject {

    private static ValidValues validValues = 
                new ValidValues(ValidValues.SUCCESS, Collections.EMPTY_SET);
    /**
     * Default Constructor
     */
    public void AuthenticatedUser() {
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
     * Returns the syntax of the subject type.
     * @see com.sun.identity.policy.Syntax
     * @param token the <code>SSOToken</code>. Not used for this subject.
     * @return Syntax for this subject.
     */
    public Syntax getValueSyntax(SSOToken token) {
        return (Syntax.CONSTANT);
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
        return (new ValidValues(ValidValues.SUCCESS, 
                                        Collections.EMPTY_SET));
    }

    
    /**
     * This method does nothing as there are no values to display for this 
     * subject.
     *
     */
    public String getDisplayNameForValue(String value, Locale locale) {
        // does nothing
        return(value);
    }

    /**
     * Returns an empty collection as value.
     *
     * @return an empty set 
     */
    public Set getValues() {
        return (Collections.EMPTY_SET);
    }

    /**
     * This method does nothing for this subject as there are no values to set
     * for this subject.
     */
    public void setValues(Set names) {
        // does nothing
    }

    
    /**
     * Determines if the user belongs to  the
     * <code>AuthenticatedUsers</code> object.
     *
     * @param token SSOToken of the user
     *
     * @return <code>true</code> if the user SSOToken is valid. 
     * <code>false</code> otherwise.
     * @exception SSOException if error occurs while validating the token.
     */

    public boolean isMember(SSOToken token) throws SSOException {
        return (SSOTokenManager.getInstance().isValidToken(token));
    }


    /**
     * Indicates whether some other object is "equal to" this one.
     * @param o another object that will be compared with this one
     * @return <code>true</code> if equal; <code>false</code>
     * otherwise.
     */
    public boolean equals(Object o) {
        if (o instanceof AuthenticatedUsers) {
            return (true);
        }
        return (false);
    }

    
    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */

    public Object clone() {
        AuthenticatedUsers theClone = null;
        try {
            theClone = (AuthenticatedUsers) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        return theClone;
    }

    /**
    * Return a hash code for this <code>AuthenticatedUsers</code>.
    *
    * @return a hash code for this <code>AuthenticatedUsers</code> object.
    */

    public int hashCode() {
        return super.hashCode();
    }
}
