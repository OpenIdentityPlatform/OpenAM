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
 * $Id: AuthenticatedAgents.java,v 1.3 2008/06/25 05:43:51 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.common.DNUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * This subject applies to all users with valid <code>SSOToken</code>.
 */
public class AuthenticatedAgents implements Subject {

    private static ValidValues validValues = 
        new ValidValues(ValidValues.SUCCESS, Collections.EMPTY_SET);
    private static String specialUser =
        SystemProperties.get(Constants.AUTHENTICATION_SPECIAL_USERS,"");
    static Debug debug = Debug.getInstance("AuthAgents");

    /**
     * Default Constructor
     */
    public void AuthenticatedAgents() {
        // do nothing
    }

    /**
     * Initialize the subject. No properties are required for this
     * subject.
     * @param configParams configurational information
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
     * @param token the <code>SSOToken</code>
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
     * @return <code>ValidValues</code> object with empty list.
     *
     */
    public ValidValues getValidValues(SSOToken token, String pattern) {
        return (validValues);
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
     * Determines if the agent belongs to  the
     * <code>AuthenticatedAgents</code> object.
     * @param token SSOToken of the agent
     * @return <code>true</code> if the agent SSOToken is valid. 
     * <code>false</code> otherwise.
     * @exception SSOException if error occurs while validating the token.
     */

    public boolean isMember(SSOToken token) throws SSOException {
        if (token == null) {
            return false;
        }
        if (!SSOTokenManager.getInstance().isValidToken(token)) {
            return false;
        }
        try {
            AMIdentity amId = IdUtils.getIdentity(token);
            IdType idType = amId.getType();
            if (debug.messageEnabled()) {
                debug.message("AuthenticatedAgents:isMember:idType = " +
                    idType + ", amId.getName() = " + amId.getName());
            }
            if (!idType.equals(IdType.AGENT)) {
                if (isSpecialUser(token.getPrincipal().getName())) {
                    return true;
                }
                return false;
            }
        } catch (IdRepoException ire) {
            debug.error("AuthenticatedAgents:isMember:IdRepoException:msg = " +
                ire.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AuthenticatedAgents theClone = null;
        try {
            theClone = (AuthenticatedAgents) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        return theClone;
    }

    /**
    * Return a hash code for this <code>AuthenticatedAgents</code>.
    * @return a hash code for this <code>AuthenticatedAgents</code> object.
    */
    public int hashCode() {
        return super.hashCode();
    }

    /**
    * checks if distinguished user name is a special user (the
    * url access agent, in particular).  returns true if so.
    */

    protected boolean isSpecialUser(String dn) {
        boolean isSpecialUser = false;
        StringTokenizer st = new StringTokenizer(specialUser, "|");
        if (debug.messageEnabled()) {
            debug.message("AuthAgents:isSpecial:dn = " + dn);
        }
        if ((dn != null) && (specialUser != null)) {
            String lcdn = DNUtils.normalizeDN(dn);
            while (st.hasMoreTokens()) {
                String specialAdminDN = (String)st.nextToken();
                if (specialAdminDN != null) {
                    String normSpecialAdmin =
                        DNUtils.normalizeDN(specialAdminDN);
                    if (debug.messageEnabled()) {
                        debug.message("AuthAgents:isSpecial:compare to " +
                            normSpecialAdmin);
                    }
                    if (lcdn.equals(normSpecialAdmin)) {
                        isSpecialUser = true;
                        break;
                    }
                }
            }
        }
        return isSpecialUser;
    }
}
